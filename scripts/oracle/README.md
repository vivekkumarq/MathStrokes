# Oracle Cloud deployment

Moves iota's backend and database off Render's free tier onto an Oracle Cloud Always Free VM,
without touching the Render or Netlify deployments until the new one is proven.

## Why

Render's free tier gives 0.1 vCPU and 512 MB, spins the service down after 15 minutes idle,
and — the part with a date on it — deletes a free PostgreSQL instance 30 days after creation
plus a 14-day grace period. A measured load test failed 10 of 25 concurrent logins and took
the service down for 141 seconds at 50. A class of 50 students does not fit.

An Always Free Ampere A1 shape gives up to 4 OCPU and 24 GB, stays up, and has no expiry
clock on its database. The cost is that we become the sysadmin: patching, backups, TLS
renewal and monitoring are ours now.

## Order

Run these in sequence. Each one stops rather than continuing past a failed check.

| | Script | Does | Needs |
|---|---|---|---|
| 0 | `00-provision.sh` | VCN, subnet, gateway, security list, and the VM itself | OCI CLI configured |
| | `watch-capacity.cmd` | double-click wrapper for the above, for the long wait | the same |
| 1 | `01-bootstrap.sh` | Java 21, PostgreSQL 18, Nginx, firewall, `iota` user | a fresh Ubuntu 24.04 VM |
| 2 | `02-database.sh` | role, database, restore, verify | a verified dump |
| 3 | `03-deploy.sh` | build, install jar, systemd | `config/iota.env` written by hand |
| 4 | `04-nginx-tls.sh` | reverse proxy, Let's Encrypt | a hostname with an A record |

Between 3 and 4 the application answers only on `127.0.0.1:8080`. That is intentional — it is
a working backend nobody can reach yet, which is the safest thing to have while testing.

## Shape

Ask for **VM.Standard.A1.Flex**, **1 OCPU / 6 GB**, **Ubuntu 24.04 LTS (aarch64)**.

Not the full 4 OCPU / 24 GB the free tier allows, even though it is free either way. Capacity
is fragmented, and a small request fits into gaps a large one cannot; asking for the maximum
as one block is the slowest way to get anything at all. The tradeoff barely exists at this
scale - against the 0.1 vCPU and 512 MB the application runs on today, 1 OCPU and 6 GB is ten
times the CPU and twelve times the memory, and it does not spin down. The remaining allowance
stays available to grow into once something is actually serving students.

Every component is native arm64 — the JDK, PostgreSQL, Nginx — so there is no emulation and
no compatibility work. `01-bootstrap.sh` prints the architecture it found so this is checked
rather than assumed.

A1 capacity is frequently exhausted, and India South (Hyderabad) is among the worst regions
for it. "Out of host capacity" is the normal first answer, not a mistake in the request.
`00-provision.sh` retries in a loop and walks the fault domains on each pass, because capacity
frees unevenly across them - leave it running rather than clicking.

Do **not** fall back to the x86 `E2.1.Micro` shape:
1 GB of RAM will not hold Spring Boot, PostgreSQL and a Maven build at once, and it is a
worse machine than the Render instance we are leaving.

## The environment file

`03-deploy.sh` refuses to run without `/opt/iota/config/iota.env`. It is written on the box
by hand and never comes from this repository, because it holds the database password and the
JWT secret. `.env.example` in the repository root documents every variable.

```bash
sudo -u iota tee /opt/iota/config/iota.env >/dev/null <<'ENV'
DATABASE_URL=jdbc:postgresql://localhost:5432/mathstrokes
DATABASE_USERNAME=mathstrokes
DATABASE_PASSWORD=CHANGE_ME
JWT_SECRET=CHANGE_ME_AT_LEAST_32_CHARS
JWT_ACCESS_TOKEN_EXPIRATION=PT15M
JWT_REFRESH_TOKEN_EXPIRATION=P7D
JWT_PASSWORD_RESET_TOKEN_EXPIRATION=PT10M
CORS_ALLOWED_ORIGINS=https://iota-jee.netlify.app
SPRING_PROFILES_ACTIVE=prod
PORT=8080
SEED_ENABLED=false
RATE_LIMIT_ENABLED=true
RATE_LIMIT_MAX_ATTEMPTS=10
RATE_LIMIT_WINDOW_SECONDS=300
SWAGGER_ENABLED=false
ENV
sudo chmod 0640 /opt/iota/config/iota.env
```

`SEED_ENABLED=false` matters. The database is being restored from production, so the admin
account already exists; seeding would either be a no-op or create a second admin with a
password written in a file.

Generate the JWT secret with `openssl rand -base64 48`. Reusing Render's is fine and means
existing refresh tokens survive the move; a new one silently logs everybody out.

## HTTPS needs a hostname

Netlify serves the frontend over HTTPS, so the browser refuses to call an `http://` backend
at all — blocked as mixed content before the request leaves. Let's Encrypt will not issue for
a bare IP. So a hostname is required; there is no free workaround that keeps TLS honest.

Either a domain you own (~₹100–1000/year), or a free DuckDNS subdomain pointed at the VM's
public IP. Both give real, valid TLS.

## Cutover

Only after the new backend is proven from a browser:

1. `netlify.toml` — point the `/api/*` rule at the new host.
2. `frontend/src/environments/environment.prod.ts` — it hardcodes the Render URL
   **absolutely**, so changing only `netlify.toml` leaves the app still calling Render.

Both, or neither. Leave Render running until students have used the new one.

## Backups

`scripts/backup-db.sh` still applies, pointed at the new database. A VM is not a backup —
the dump has to leave the machine. Copy it down with `scp` on a schedule you actually keep.
