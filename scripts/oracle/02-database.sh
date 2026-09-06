#!/usr/bin/env bash
#
# Creates iota's PostgreSQL role and database on the VM and restores the production dump
# into it, then proves the restore actually landed.
#
#   scp backups/iota-<stamp>.dump ubuntu@<vm-ip>:/tmp/
#   ssh ubuntu@<vm-ip>
#   read -rs -p 'New DB password: ' DB_PASSWORD; export DB_PASSWORD; echo
#   bash ~/02-database.sh /tmp/iota-<stamp>.dump
#
# The password is read into the environment rather than passed as an argument so it does not
# land in the shell history or in the process list, where any user on the box could read it.
#
# REFUSES to run against a database that already contains tables. Restoring over live data is
# the one action here that cannot be undone, so it is not something a mistyped re-run can do.
#
set -euo pipefail

DB_NAME=mathstrokes
DB_USER=mathstrokes
DUMP="${1:-}"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[1;31m%s\033[0m\n' "$*" >&2; exit 1; }

[ -n "$DUMP" ]  || die "Usage: 02-database.sh <path-to-dump>"
[ -f "$DUMP" ]  || die "No such dump: $DUMP"
[ -n "${DB_PASSWORD:-}" ] || die "Set DB_PASSWORD first (read -rs -p 'password: ' DB_PASSWORD; export DB_PASSWORD)"

log "Checking the dump is readable before touching the database"
# A dump that pg_restore cannot list cannot be restored. Finding that out now costs seconds;
# finding out after dropping something costs the data.
pg_restore --list "$DUMP" >/dev/null 2>&1 || die "pg_restore cannot read $DUMP. Do not proceed."
expected_tables=$(pg_restore --list "$DUMP" | grep -c 'TABLE DATA' || true)
echo "  $(wc -c < "$DUMP") bytes, $expected_tables tables with data"

log "Role and database"
if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1; then
    # Password interpolated through a here-doc rather than the command line, for the same
    # reason as above: psql arguments are visible in ps output.
    sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
CREATE ROLE $DB_USER LOGIN PASSWORD '$DB_PASSWORD';
SQL
    echo "  role $DB_USER created"
else
    sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
ALTER ROLE $DB_USER WITH PASSWORD '$DB_PASSWORD';
SQL
    echo "  role $DB_USER already existed - password reset to the one supplied"
fi

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1; then
    sudo -u postgres createdb --owner="$DB_USER" "$DB_NAME"
    echo "  database $DB_NAME created"
else
    echo "  database $DB_NAME already existed"
fi

log "Refusing to overwrite existing data"
existing=$(sudo -u postgres psql -tAd "$DB_NAME" -c \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'")
if [ "$existing" -ne 0 ]; then
    die "$DB_NAME already has $existing tables in public.

This script will not restore over them. If you genuinely want to replace this database,
drop it deliberately first:

  sudo -u postgres dropdb $DB_NAME

and re-run. That is destructive and irreversible - be certain the dump you hold is good."
fi
echo "  public schema is empty - safe to restore"

log "Restoring"
# --no-owner / --no-privileges because the dump's ownership refers to Render's role names,
# which do not exist here. Everything lands owned by the role doing the restore instead.
sudo -u postgres pg_restore --no-owner --no-privileges --dbname="$DB_NAME" "$DUMP"

# Flyway's own bookkeeping table travels in the dump. The schema is therefore already at the
# version production was at, and Flyway will validate rather than re-run migrations - which is
# what we want, because re-running them over restored data would fail on the seed inserts.
sudo -u postgres psql -v ON_ERROR_STOP=1 -d "$DB_NAME" <<SQL
ALTER SCHEMA public OWNER TO $DB_USER;
GRANT ALL ON ALL TABLES    IN SCHEMA public TO $DB_USER;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO $DB_USER;
SQL

log "Verifying the restore"
sudo -u postgres psql -d "$DB_NAME" -c "
SELECT relname AS table, n_live_tup AS rows
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC, relname;"

echo
echo "Flyway history:"
sudo -u postgres psql -d "$DB_NAME" -tAc \
    "SELECT version || '  ' || description || '  ' || CASE WHEN success THEN 'ok' ELSE 'FAILED' END
     FROM flyway_schema_history ORDER BY installed_rank;" | sed 's/^/  /'

log "Local connectivity as the application will connect"
PGPASSWORD="$DB_PASSWORD" psql -h 127.0.0.1 -U "$DB_USER" -d "$DB_NAME" -tAc "SELECT 'connected as ' || current_user" \
    | sed 's/^/  /'

cat <<DONE

Restore complete and verified.

Compare the row counts above against production before trusting this. In particular the
tables that cannot be regenerated: users, questions, tests, attempts, attempt answers.

Next: 03 puts the application on the box - build the jar, drop it in /opt/iota/app,
write the environment file, and start it under systemd.
DONE
