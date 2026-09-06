#!/usr/bin/env bash
#
# Prepares a fresh Oracle Cloud Always Free VM to host iota: Java, PostgreSQL, Nginx, a
# firewall and a deployment user. Run once, as the default `ubuntu` user, over SSH.
#
#   scp scripts/oracle/01-bootstrap.sh ubuntu@<vm-ip>:~
#   ssh ubuntu@<vm-ip> 'bash ~/01-bootstrap.sh'
#
# Idempotent: every step checks before it acts, so a re-run after a failure resumes rather
# than duplicating. Nothing here touches Render or Netlify - the existing deployment keeps
# serving students until this one is proven.
#
# Deliberately NOT done here: restoring the database, installing the application, issuing
# certificates. Those come later, once this box is known good.
#
set -euo pipefail

# PostgreSQL 18 specifically, not Ubuntu's default 16. Production runs 18.6, and pg_restore
# will not load a dump taken from a server newer than the target. Matching the major version
# removes a whole class of restore failure.
PG_MAJOR=18

# The application never runs as root and never as a login user. A dedicated system account
# with no shell limits what a compromised process can reach.
APP_USER=iota

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }

[ "$(id -u)" -eq 0 ] && { echo "Run as ubuntu, not root - the script sudos where it needs to." >&2; exit 1; }

log "Architecture and release"
arch=$(dpkg --print-architecture)
echo "  arch    : $arch"
echo "  release : $(lsb_release -ds 2>/dev/null || echo unknown)"
echo "  memory  : $(free -h | awk '/^Mem:/{print $2}')"
echo "  cpus    : $(nproc)"
# Every component below - the JDK, PostgreSQL, Nginx - ships native arm64 builds, so an
# Ampere A1 shape needs no emulation and no special casing. Recorded here so a future reader
# can see the check was actually made rather than assumed.

log "Updating package lists"
sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y -qq

log "Base packages"
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    curl ca-certificates gnupg lsb-release ufw nginx unzip htop

log "Java 21"
# Ubuntu 24.04 carries OpenJDK 21 in main, native on arm64. Matching the project's
# <java.version>21 exactly; a newer JDK would run the jar but changes GC and TLS defaults
# for no benefit.
if ! java -version 2>&1 | grep -q '"21'; then
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jdk-headless
fi
java -version

log "PostgreSQL $PG_MAJOR from the PGDG repository"
if ! [ -d "/usr/lib/postgresql/$PG_MAJOR" ]; then
    sudo install -d /usr/share/postgresql-common/pgdg
    sudo curl -fsSL -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
        https://www.postgresql.org/media/keys/ACCC4CF8.asc
    echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] \
https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" \
        | sudo tee /etc/apt/sources.list.d/pgdg.list >/dev/null
    sudo apt-get update -qq
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
        "postgresql-$PG_MAJOR" "postgresql-client-$PG_MAJOR"
fi
psql --version

log "Confirming PostgreSQL listens only on localhost"
# The default is already localhost-only, but this is the difference between a private
# database and one the whole internet can attempt to log into, so it is verified rather
# than trusted. The firewall below is a second layer, not the only one.
sudo -u postgres psql -tAc "SHOW listen_addresses;" | sed 's/^/  listen_addresses = /'

log "Application user and directory layout"
if ! id "$APP_USER" >/dev/null 2>&1; then
    sudo useradd --system --create-home --home-dir "/opt/$APP_USER" --shell /usr/sbin/nologin "$APP_USER"
fi
sudo install -d -o "$APP_USER" -g "$APP_USER" -m 0755 "/opt/$APP_USER/app"
# 0750 on config: it will hold the database password and the JWT secret. World-readable
# would put both in reach of any account on the box.
sudo install -d -o "$APP_USER" -g "$APP_USER" -m 0750 "/opt/$APP_USER/config"
sudo install -d -o "$APP_USER" -g "$APP_USER" -m 0755 "/var/log/$APP_USER"
sudo install -d -o "$APP_USER" -g "$APP_USER" -m 0700 "/opt/$APP_USER/backups"

log "Firewall"
# Order matters and is not cosmetic: enabling ufw before SSH is allowed drops the connection
# this script is running over, and the VM is then reachable only through Oracle's serial
# console. The SSH rule is added first and its presence is verified before enabling.
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw status | grep -qi 'OpenSSH\|22' || { echo "SSH rule missing - refusing to enable ufw." >&2; exit 1; }
sudo ufw --force enable
sudo ufw status verbose

log "Timezone"
# The application fixes Asia/Kolkata in TestSchedule for display, and stores every instant in
# UTC. The host clock stays UTC so logs and timestamps agree with the database.
sudo timedatectl set-timezone UTC
timedatectl | head -3

log "Nginx placeholder"
sudo systemctl enable --now nginx
sudo systemctl is-active nginx

cat <<DONE

Bootstrap complete.

  Java        $(java -version 2>&1 | head -1)
  PostgreSQL  $(psql --version)
  Nginx       $(nginx -v 2>&1)
  App user    $APP_USER  (no shell, owns /opt/$APP_USER)
  Firewall    22, 80, 443 open; 5432 and 8080 are not

Postgres port 5432 and the application port 8080 are reachable only from the machine itself.
Verify from your laptop that they are closed before going further:

  nc -zv <vm-ip> 5432    # should fail
  nc -zv <vm-ip> 8080    # should fail
  nc -zv <vm-ip> 22      # should succeed

Next: 02-database.sh creates the role and database and restores the production dump.
DONE
