#!/usr/bin/env bash
#
# Takes a compressed, restorable snapshot of a PostgreSQL database and proves it is readable
# before reporting success.
#
# This exists because the hosted database has no backups of its own. Free Render PostgreSQL
# instances expire 30 days after creation, allow a 14-day grace period, and are then deleted
# with their data; the free tier supports no backup mechanism at all. Everything students have
# done lives on one instance with a deletion date, so the only copy that is certain to exist is
# one taken deliberately.
#
# Usage:
#   scripts/backup-db.sh "postgresql://user:pass@host:5432/dbname"
#   scripts/backup-db.sh                       # falls back to $DATABASE_URL
#
# Output lands in backups/ as a timestamped custom-format dump. Custom format rather than plain
# SQL because it restores selectively, restores in parallel, and compresses without a second tool.
#
set -euo pipefail

DB_URL="${1:-${DATABASE_URL:-}}"

if [ -z "$DB_URL" ]; then
    cat >&2 <<'USAGE'
No connection string given.

  scripts/backup-db.sh "postgresql://user:pass@host:5432/dbname"

For the hosted database, copy the EXTERNAL connection string from your provider's dashboard.
The internal one resolves only from inside the provider's own network and will not connect
from a laptop.
USAGE
    exit 2
fi

# Never let the URL reach the terminal or a log: it carries the password.
redacted() { printf '%s' "$1" | sed -E 's#(://[^:]+:)[^@]+(@)#\1****\2#'; }

command -v pg_dump >/dev/null 2>&1 || {
    echo "pg_dump not found on PATH." >&2
    echo "Windows: add C:\\Program Files\\PostgreSQL\\<version>\\bin" >&2
    exit 3
}

echo "Source : $(redacted "$DB_URL")"

# --- version gate --------------------------------------------------------------------------
# pg_dump refuses to dump from a server newer than itself, and it fails midway rather than up
# front. Checking here turns a confusing abort into a sentence saying what to install.
#
# This bites on this project specifically: development runs PostgreSQL 17 and production runs
# 18.6, so the client that works locally cannot back up production.
server_version=$(psql "$DB_URL" -Atc "SHOW server_version;" 2>/dev/null | head -1 || true)

if [ -z "$server_version" ]; then
    echo "Could not connect to the database to read its version." >&2
    echo "Check the connection string, and that the provider allows external connections." >&2
    exit 4
fi

server_major=${server_version%%.*}
dump_major=$(pg_dump --version | grep -oE '[0-9]+' | head -1)

echo "Server : PostgreSQL $server_version"
echo "Client : pg_dump $dump_major"

if [ "$dump_major" -lt "$server_major" ]; then
    cat >&2 <<EOF

pg_dump $dump_major cannot dump a PostgreSQL $server_major server. It will refuse rather than
produce a partial file, so this stops here instead of leaving you with a dump you would
only discover was unusable when you needed it.

Install PostgreSQL $server_major client tools and put their bin/ ahead on PATH, or run the
dump through a matching container image:

  docker run --rm postgres:$server_major pg_dump "<connection string>" -Fc > backup.dump

EOF
    exit 5
fi

# --- dump ----------------------------------------------------------------------------------
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
out_dir="$repo_root/backups"
mkdir -p "$out_dir"

stamp=$(date -u +%Y%m%dT%H%M%SZ)
out_file="$out_dir/iota-$stamp.dump"

echo "Writing: $out_file"
pg_dump "$DB_URL" --format=custom --no-owner --no-privileges --file="$out_file"

# --- verify --------------------------------------------------------------------------------
# A dump that cannot be listed cannot be restored. Reporting success without checking would be
# the same failure as having no backup, discovered later.
if ! pg_restore --list "$out_file" >/dev/null 2>&1; then
    echo "The dump was written but pg_restore cannot read it. Treat it as unusable." >&2
    exit 6
fi

size=$(wc -c < "$out_file" | tr -d ' ')
tables=$(pg_restore --list "$out_file" | grep -c 'TABLE DATA' || true)

echo
echo "Verified. $size bytes, $tables tables with data."
echo
echo "Restore into an empty database with:"
echo "  pg_restore --no-owner --no-privileges --dbname=\"<target connection string>\" \"$out_file\""
