# Migrating the backend from Render to Northflank

The API and its database currently run on Render's free tier. This describes moving both to
Northflank, why the move is worth making, and the one thing to check before committing to it.

[`DEPLOYMENT.md`](DEPLOYMENT.md) remains the general, provider-neutral guide. This document is
about one specific move.

---

## Why move

Two separate problems, one of which is urgent.

**The database has a deletion date.** Free Render PostgreSQL instances expire 30 days after
creation. After expiry there is a 14-day grace period to upgrade to a paid plan, and after that
Render deletes the instance and its data. The free tier supports no backup mechanism of any kind.
Every student account, every attempt and every result sits on one instance with an expiry date and
no safety net. This is the reason to move now rather than eventually.

**The web service sleeps.** Render suspends a free service after roughly 15 minutes without
inbound traffic. Northflank's free tier is always-on, which removes the problem rather than
working around it.

It is worth being precise about how bad the sleeping actually is, because the repository has
carried two different wrong numbers for it:

| Observation | Measured |
|---|---|
| Warm sign-in | ~1.4 s |
| Sign-in after 17 minutes idle | 0.49 s preflight + 7.97 s login |
| Two keep-alive runs, 21:52Z and 05:59Z | no response at all within 120 s |

The 8-second figure is a controlled measurement: production was left untouched for 17 minutes,
past the documented 15-minute threshold, with no keep-alive run inside the window. The 120-second
failures are real but were not reproduced on demand, so the honest position is that the wake cost
is usually seconds and occasionally catastrophic, and the mechanism for the bad case is not
established. A preflight answering in 0.49 s while the login beside it took 7.97 s points at the
connection pool rather than a JVM boot, since the preflight never touches the database.

The keep-warm GitHub Action is not a fix. It is scheduled `*/10` and has fired four times in
21 hours against an expected 126 — GitHub deprioritises high-frequency schedules on shared
runners, and no amount of tuning the cron changes that.

---

## Check this first

**Northflank does not publish the vCPU and RAM allowance of its free Sandbox plan.** That number
decides whether this migration is possible at all, and it is the one fact this document cannot
supply.

The application currently runs inside Render's 512 MB free instance. `backend/Dockerfile` is
already sized for a small container:

```
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC ..."
```

`MaxRAMPercentage` means the JVM sizes its heap from the cgroup limit rather than the host's RAM,
and `UseSerialGC` avoids the thread overhead a parallel collector brings to a small container. So
512 MB is known to work. Third-party summaries put the free Sandbox in the region of 0.1–1 vCPU
and around 512 MB, and the smallest *paid* preset, `nf-compute-10`, is 0.1 vCPU / 256 MB.

**If the free plan turns out to be 256 MB, stop.** A Spring Boot application with Hibernate and
Flyway will either fail to start or spend its life collecting garbage. Confirm the figure in the
Northflank dashboard before migrating anything.

Two smaller unknowns, worth confirming at the same time:

- Whether the free database addon includes storage or bills it at the published $0.30/GB-month.
- Northflank requires a card at signup. Their pricing page states you are not charged unless you
  upgrade, but it is a card either way.

---

## What you get on the free plan

2 services, 1 database addon, 2 cron jobs, always-on compute. This project needs one service and
one addon, so it fits with a service spare. The frontend stays on Netlify.

---

## Migration

Nothing here is destructive to the running system until step 6. Up to that point Render keeps
serving traffic and the Northflank stack is being built alongside it.

### 1. Back up the Render database

Do this first, before anything else, and keep the file somewhere other than this machine.

```bash
scripts/backup-db.sh "<external connection string from the Render dashboard>"
```

Use the **external** connection string. The internal one resolves only inside Render's network.

There is a version trap here. Production runs PostgreSQL 18.6; a `pg_dump` older than 18 refuses
to dump it and does so partway through rather than up front. `scripts/backup-db.sh` checks the
server version against the client version and stops with an explanation rather than leaving you
holding an unusable file. If it stops, install PostgreSQL 18 client tools or run the dump through
a `postgres:18` container image.

The script verifies the dump with `pg_restore --list` before reporting success, because a backup
that has not been read back is only a belief that you have a backup.

Dumps land in `backups/`, which is git-ignored — they contain names, phone numbers and password
hashes and must never be committed.

### 2. Create the Northflank project and database

Create a project, then add a PostgreSQL addon. Match the major version to production — **18** —
so the restore does not have to cross a version boundary as well as a provider.

### 3. Restore into it

```bash
pg_restore --no-owner --no-privileges --dbname="<northflank connection string>" backups/iota-<stamp>.dump
```

`--no-owner` and `--no-privileges` matter: the Render dump references Render's role names, which
do not exist on Northflank, and without these flags the restore fails on every `ALTER ... OWNER`.

Then confirm the Flyway history came across intact:

```sql
SELECT version, description, checksum, success FROM flyway_schema_history ORDER BY installed_rank;
```

Six rows, all `success = true`. If that table did not survive, Flyway will try to re-run every
migration against a populated schema and fail — and this project has already lost two deploys to
a Flyway checksum mismatch, so it is worth looking rather than assuming.

### 4. Create the service

Point Northflank at this repository as a combined service — it builds and deploys the latest
commit. Build from `backend/Dockerfile` with `backend` as the build context, and expose port
**8080** publicly over HTTP.

### 5. Environment variables

Everything the container needs, and nothing else. These mirror `render.yaml`.

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | injected from the addon |
| `JWT_SECRET` | a fresh 32+ character secret. Rotating it signs everyone out, which is correct for a host move |
| `CORS_ALLOWED_ORIGINS` | `https://iota-jee.netlify.app` |
| `SEED_ENABLED` | `false` — the admin already exists in the restored data |
| `SWAGGER_ENABLED` | `false` |
| `DB_POOL_MAX` | `5` |
| `BCRYPT_STRENGTH` | `10` |

`SEED_ENABLED=false` is deliberate. The restored database already contains the admin account, and
`AdminAccountSeeder` skips when one exists — but setting it false removes the question entirely.

`DATABASE_URL` arrives in libpq form (`postgresql://user:pass@host/db`), which JDBC cannot parse.
`DatabaseUrlEnvironmentPostProcessor` already rewrites it, and its tests cover Render, Neon,
Heroku and Railway URL shapes, so Northflank's should need no code change. Confirm it boots rather
than assuming — that class is the single point of failure for this step.

### 6. Cut over

Only now does anything change for students.

1. Copy the service's public domain from Northflank.
2. Update `apiBaseUrl` in `frontend/src/environments/environment.prod.ts`.
3. Update the `/api/*` proxy target in `netlify.toml`. The proxy is currently unused — the bundle
   calls the API host directly, because Netlify's proxy gives up at ~29 s — but leaving a stale
   hostname in there is a trap for whoever reads it next.
4. Rebuild and deploy the frontend.

Verify before announcing anything: sign in, list tests, start an attempt, submit it, read the
result. The count should be **58** tests with 2 full-syllabus papers.

### 7. Afterwards

Once Northflank has served real traffic for a few days:

- Delete `.github/workflows/keep-api-awake.yml`. An always-on service does not need waking, and
  leaving it pinging a dead host produces noise that looks like a fault.
- Remove the "Waking the server" notice on the login screen. It becomes untrue.
- Keep `render.yaml` or delete it deliberately. It is the only record of how the Render deployment
  was configured.
- Set up scheduled backups on the Northflank addon, which the free Render tier could not do.
  `scripts/backup-db.sh` still works and is worth keeping as the off-platform copy.

Do **not** delete the Render database until the Northflank one has been serving writes for long
enough that you would notice a problem. It is the only rollback.

---

## Rollback

Until step 6, rollback is doing nothing — Render is still live and still serving.

After step 6, point `apiBaseUrl` back at the Render hostname and redeploy the frontend. This works
only while the Render instance and its database still exist, which is the reason for keeping them,
and only until the free database's expiry date passes.

Writes made against Northflank after cutover will not exist on Render. A rollback therefore loses
whatever students did in between, so the window for a comfortable rollback is short. Cut over at a
quiet hour.

---

## Honest limits of this document

Written from Northflank's published documentation and pricing, not from a completed migration. The
steps follow their documented model, but the free-plan resource allowance is unpublished and the
exact console wording will drift.

Anything measured about the *current* deployment — the 8-second wake, the 120-second failures, the
keep-alive drop rate, the 58 tests, the PostgreSQL 18.6 server version — was measured against
production and is reliable.
