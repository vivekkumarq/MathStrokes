#!/usr/bin/env bash
#
# Builds iota from source on the VM, installs the jar, and runs it under systemd.
#
#   bash ~/03-deploy.sh                 # build from the checked-out repo and restart
#
# Building on the box rather than shipping a jar from a laptop: an Ampere A1 shape has more
# memory than most laptops, the toolchain is already there, and it removes the risk of an
# architecture or JDK mismatch between build and run. On a 1 GB x86 micro shape this would
# have to change - Maven will not fit.
#
# The previous jar is kept as .jar.prev on every deploy. A bad build is then one command away
# from being undone, which matters when the alternative is a rebuild while students wait.
#
set -euo pipefail

APP_USER=iota
APP_HOME="/opt/$APP_USER"
REPO="$APP_HOME/src"
REPO_URL="https://github.com/vivekkumarq/MathStrokes.git"
BRANCH="${BRANCH:-main}"
ENV_FILE="$APP_HOME/config/iota.env"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[1;31m%s\033[0m\n' "$*" >&2; exit 1; }

[ -f "$ENV_FILE" ] || die "$ENV_FILE does not exist yet.

Create it first - it holds the database password and the JWT secret, so it is written by
hand on the box and never comes from the repository. See 03-deploy.README in scripts/oracle."

log "Fetching source ($BRANCH)"
if [ -d "$REPO/.git" ]; then
    sudo -u "$APP_USER" git -C "$REPO" fetch --quiet origin "$BRANCH"
    sudo -u "$APP_USER" git -C "$REPO" reset --hard --quiet "origin/$BRANCH"
else
    sudo -u "$APP_USER" git clone --quiet --branch "$BRANCH" "$REPO_URL" "$REPO"
fi
echo "  $(sudo -u "$APP_USER" git -C "$REPO" log -1 --format='%h %s')"

log "Building"
# Tests are skipped here on purpose: this step exists to produce a runnable artefact from a
# commit that already passed CI. Running them on the VM would test the VM, slowly.
sudo -u "$APP_USER" bash -c "cd '$REPO/backend' && ./mvnw -q -B -DskipTests package"

jar=$(sudo -u "$APP_USER" bash -c "ls -t '$REPO/backend/target/'*.jar | grep -v sources | head -1")
[ -n "$jar" ] || die "Build produced no jar."
echo "  $(basename "$jar")  $(stat -c%s "$jar") bytes"

log "Installing"
if [ -f "$APP_HOME/app/iota.jar" ]; then
    sudo -u "$APP_USER" cp "$APP_HOME/app/iota.jar" "$APP_HOME/app/iota.jar.prev"
    echo "  previous jar kept as iota.jar.prev"
fi
sudo -u "$APP_USER" cp "$jar" "$APP_HOME/app/iota.jar"

log "systemd unit"
sudo tee /etc/systemd/system/iota.service >/dev/null <<UNIT
[Unit]
Description=iota examination platform
# Postgres is on this same host, so the app must not start before it or the first
# connection attempt fails and Flyway aborts the boot.
After=network-online.target postgresql.service
Wants=network-online.target
Requires=postgresql.service

[Service]
Type=simple
User=$APP_USER
Group=$APP_USER
WorkingDirectory=$APP_HOME/app

# Secrets live here, not in the unit file: systemd unit files are world-readable by design,
# and this one is checked into nothing. The env file is 0640 and owned by $APP_USER.
EnvironmentFile=$ENV_FILE

# -Xmx512m leaves the rest of the box to PostgreSQL and the page cache. The JVM would
# otherwise size its heap from total RAM and, on a 24 GB shape, reserve far more than an
# application this size will ever use - memory Postgres could be caching with.
# MaxRAMPercentage is deliberately not used: an absolute cap is easier to reason about when
# two memory-hungry processes share one host.
ExecStart=/usr/bin/java -Xms256m -Xmx512m -XX:+UseSerialGC -jar $APP_HOME/app/iota.jar

# Restart on any exit, including a clean one - a Spring Boot process that exits cleanly in
# production has still stopped serving students. 10s rather than the 100ms default so a
# crash loop backs off instead of hammering the database.
Restart=always
RestartSec=10

StandardOutput=append:/var/log/$APP_USER/iota.log
StandardError=append:/var/log/$APP_USER/iota.log

# Hardening. The process needs its own directories and nothing else.
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/$APP_USER $APP_HOME

[Install]
WantedBy=multi-user.target
UNIT

sudo systemctl daemon-reload
sudo systemctl enable iota
sudo systemctl restart iota

log "Waiting for the application to answer"
port=$(grep -E '^PORT=' "$ENV_FILE" | cut -d= -f2 | tr -d '"' || echo 8080)
port=${port:-8080}
for i in $(seq 1 60); do
    if curl -sf "http://127.0.0.1:$port/api/actuator/health" >/dev/null 2>&1 \
    || curl -sf "http://127.0.0.1:$port/api/auth/login" -o /dev/null 2>&1 \
    || [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$port/api/tests")" != "000" ]; then
        echo "  up after ${i}s"
        break
    fi
    [ "$i" -eq 60 ] && { sudo journalctl -u iota -n 40 --no-pager; die "Did not come up in 60s. Log above."; }
    sleep 1
done

sudo systemctl status iota --no-pager | head -12

cat <<DONE

Deployed.

  systemctl status iota          state
  journalctl -u iota -f          live log
  tail -f /var/log/iota/iota.log application log
  systemctl restart iota         restart

Rolling back to the previous jar:
  sudo -u $APP_USER cp $APP_HOME/app/iota.jar.prev $APP_HOME/app/iota.jar
  sudo systemctl restart iota

The application is on 127.0.0.1:$port and is not reachable from outside. Nginx comes next.
DONE
