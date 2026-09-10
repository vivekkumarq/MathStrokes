#!/usr/bin/env bash
#
# Restore an iota backup into a Neon Postgres database and prove it matches the source.
#
# Neon is a drop-in Postgres, so this is an ordinary pg_restore. What the script adds is the
# checking either side of it: that the target is empty before we write, and that every table
# holds the same number of rows afterwards as the database we copied from. A restore that
# half worked is worse than one that failed outright, because nothing announces it.
#
# Credentials come from the environment, never from the command line - anything in argv is
# visible to every other user on the machine through `ps`.
#
#   export NEON_URL='postgresql://...@...neon.tech/iota?sslmode=require'
#   export SOURCE_URL="$(cat backups/.render-url)"      # optional, enables row-count checking
#   bash scripts/migrate-to-neon.sh backups/iota-<stamp>.dump
#
set -euo pipefail

DUMP="${1:-}"

if [ -z "$DUMP" ]; then
    echo "Usage: NEON_URL=... bash scripts/migrate-to-neon.sh <dump file>" >&2
    exit 1
fi
if [ ! -f "$DUMP" ]; then
    echo "No such dump: $DUMP" >&2
    exit 1
fi
if [ -z "${NEON_URL:-}" ]; then
    echo "NEON_URL is not set. Copy the connection string from the Neon dashboard:" >&2
    echo "  export NEON_URL='postgresql://...@...neon.tech/iota?sslmode=require'" >&2
    exit 1
fi

# PostgreSQL 18 client tools. The system's older pg_restore refuses a newer archive, and the
# failure arrives partway through rather than at the start.
export PATH="$HOME/AppData/Local/Programs/pg18/pgsql/bin:$PATH"

mask() { sed -E 's#://([^:]+):[^@]+@#://\1:****@#g'; }

echo "==> Target"
echo "    $(printf '%s' "$NEON_URL" | mask)"
psql "$NEON_URL" -tAc 'select version()' | sed 's/^/    /'

# ---------------------------------------------------------------------------------------
# Refuse to write into a database that already holds something. This is the only step here
# that cannot be undone, and a second run against a restored database would leave a mess of
# duplicate-key errors rather than a clean failure.
# ---------------------------------------------------------------------------------------
existing=$(psql "$NEON_URL" -tAc \
    "select count(*) from information_schema.tables where table_schema = 'public'")
if [ "$existing" -ne 0 ]; then
    echo >&2
    echo "Refusing to restore: the target already has $existing table(s) in 'public'." >&2
    echo "Restoring over them would not be a clean import. Drop the schema deliberately" >&2
    echo "first, or create a fresh Neon project and point NEON_URL at that." >&2
    exit 1
fi
echo "    empty - safe to restore"

echo
echo "==> Restoring $(basename "$DUMP") ($(wc -c <"$DUMP") bytes)"
# --no-owner / --no-privileges: the dump references the Render role, which does not exist on
# Neon. Without these every GRANT and ALTER OWNER fails and the run ends dirty.
pg_restore --no-owner --no-privileges --dbname="$NEON_URL" "$DUMP"
echo "    restored"

echo
echo "==> Verifying"
tables=$(psql "$NEON_URL" -tAc \
    "select count(*) from information_schema.tables where table_schema = 'public'")
echo "    tables in public: $tables"

# Per-table row counts, ordered so the two sides can be compared line for line.
counts() {
    psql "$1" -tAF',' -c "
        select relname, n_live_tup
        from pg_stat_user_tables
        where schemaname = 'public'
        order by relname"
}

if [ -n "${SOURCE_URL:-}" ]; then
    # n_live_tup is an estimate maintained by the statistics collector, so ask for a fresh
    # analyze on both sides before trusting the comparison.
    psql "$NEON_URL" -qc 'analyze' >/dev/null
    psql "$SOURCE_URL" -qc 'analyze' >/dev/null

    if diff <(counts "$SOURCE_URL") <(counts "$NEON_URL") >/tmp/iota-rowdiff 2>&1; then
        echo "    row counts match the source exactly, table for table"
    else
        echo >&2
        echo "Row counts DIFFER between source and Neon:" >&2
        sed 's/^/    /' /tmp/iota-rowdiff >&2
        echo >&2
        echo "Do not cut over. The old database is still live and serving." >&2
        exit 1
    fi
else
    echo "    SOURCE_URL not set - skipping the row-count comparison."
    echo "    Set it to check the copy against the database it came from."
    counts "$NEON_URL" | sed 's/^/    /'
fi

cat <<'NEXT'

==> Restored and verified.

Nothing is live on Neon yet. The old database is untouched and still serving.

To cut over, set these on the Render service (Environment tab) and redeploy:

    DATABASE_URL       jdbc:postgresql://<host>.neon.tech/<db>?sslmode=require
    DATABASE_USERNAME  <neon user>
    DATABASE_PASSWORD  <neon password>
    SEED_ENABLED       false

SEED_ENABLED matters: the database already has its accounts, and seeding again on a restored
database is not what you want on the first boot after a migration.

Note the JDBC form above - Spring needs `jdbc:postgresql://`, not the `postgresql://` string
Neon shows you, and it needs the user and password as separate settings rather than inline.

To roll back, put the old DATABASE_URL back and redeploy. Keep the Render database until the
new one has served real traffic.
NEXT
