# Deployment

The application is one container plus a PostgreSQL database plus a folder of static files. That is
deliberately the whole shape of it — there is nothing to orchestrate.

---

## What you need

| Piece | What it is | Free-tier options |
|---|---|---|
| Database | Managed PostgreSQL 14+ | Neon, Supabase, Aiven, Northflank, Render PostgreSQL |
| Backend | The container from `backend/Dockerfile` | Northflank, Render, Railway, Fly.io, Koyeb |
| Frontend | Static files from `npm run build` | Netlify, Vercel, Cloudflare Pages, GitHub Pages |

Free tiers change constantly, so pick what is actually on offer when you deploy. Nothing in the
project is tied to a provider: the backend is a plain container configured entirely through
environment variables, and the frontend is plain static output.

Three things to know about free tiers before you pick one:

- **Free databases can expire.** Render deletes a free PostgreSQL instance 30 days after creation
  plus 14 days' grace, and offers no backups on that tier. Check the retention terms before you
  put anything you care about on one, and take your own copies regardless —
  `scripts/backup-db.sh` does this and verifies the result is restorable.
- **Containers idle out.** A free backend instance is often suspended after ~15 minutes of
  inactivity. On this project's Render deployment, a sign-in after a deliberate 17-minute idle
  measured 7.97 s against 1.4 s warm — but two monitoring requests separately got no response at
  all within 120 s, and that was not reproducible on demand. Treat the wake cost as usually
  seconds and occasionally much worse. Use a paid instance for anything real, or an always-on
  free tier such as Northflank's — see
  [`MIGRATING-TO-NORTHFLANK.md`](MIGRATING-TO-NORTHFLANK.md).
- **The expiry sweep needs the app to be running.** If the container is asleep, attempts whose time
  has run out are not finalised until it wakes. They are finalised correctly when it does, and no
  marks are lost, but a student's result may be delayed.

---

## The shape of a Netlify deployment

Netlify serves static files. It **cannot run the Spring Boot backend** — there is no JVM on a
Netlify build, and an Angular bundle is not a server. So a deployment is three pieces:

```
  Netlify            netlify.toml           Render / Railway / Fly
  Angular app  ──────  /api/*  proxy  ─────▶  Spring Boot container
                                                      │
                                                      ▼
                                              managed PostgreSQL
```

Two config files in this repository do most of the work:

| File | What it does |
|---|---|
| `netlify.toml` | Build command, publish directory, the `/api/*` proxy, SPA fallback, cache and security headers |
| `render.yaml` | The backend service and database as a Render blueprint |

**Why proxy `/api/*` through Netlify rather than calling the backend directly.** It makes the API
same-origin from the browser's point of view, so there is no CORS preflight on every request and
no cross-origin configuration to keep in step between two hosts. It also means the backend's URL
appears in exactly one place — `netlify.toml` — instead of being baked into the JavaScript bundle.

Order matters in `netlify.toml`: the `/api/*` rule is declared **before** the SPA catch-all,
because Netlify applies rules in order and `/*  →  /index.html` would otherwise swallow every
API call and hand the browser an HTML page where it expected JSON.

### Step by step

**1. Database.** Create a PostgreSQL instance — Neon and Supabase both have a free tier that does
not expire; Render's own free database currently expires after 90 days, which matters if this is
meant to stay up. Convert the connection string to JDBC form as shown below.

**2. Backend.** Render → New → Blueprint → point it at this repository. It reads `render.yaml`,
builds `backend/Dockerfile` and starts the service. Set the variables marked `sync: false`:
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `CORS_ALLOWED_ORIGINS`, and the five
`SEED_ADMIN_*` values for the first boot. `JWT_SECRET` is generated for you.

Flyway builds the schema and seeds the full question bank on first start. Wait for
`/api/actuator/health` to return `{"status":"UP"}` before continuing, and note the service URL.

**3. Frontend.** Edit `netlify.toml` and replace `BACKEND_ORIGIN` with that hostname — no scheme,
no trailing slash:

```toml
to = "https://mathstrokes-api.onrender.com/api/:splat"
```

Then Netlify → Add new site → Import an existing project → pick this repository. It reads
`netlify.toml`, so the build command, publish directory and redirects need no configuration in the
dashboard.

**4. Point the app at the proxy.** The Angular client currently targets
`http://localhost:8080/api`. For production it must call `/api` on its own origin so the proxy
picks it up. That is a one-line change in the frontend's API configuration and must be done before
the production build, or the deployed site will try to reach localhost from the visitor's machine.

**5. Afterwards.** Sign in as the seeded admin, change the password, then set `SEED_ENABLED=false`
and redeploy the backend.

### Free-tier realities worth knowing before you rely on this

- **The backend sleeps.** A free Render service is suspended after about 15 minutes idle and takes
  30–60 seconds to wake. A student clicking "Start test" should not wait a minute, so this is fine
  for a demo and not for a real examination.
- **The expiry sweep only runs while the service is awake.** Attempts whose time ran out are
  finalised when it next wakes. No marks are lost — submission is idempotent and scores come from
  answers already saved — but a result can be delayed.
- **Netlify's free build minutes are limited.** Not an issue at this rate of change.

---

## 1. Database

Create the database and copy the connection details. If the provider hands you a URL like
`postgres://user:pass@host/db`, convert it to JDBC form:

```
DATABASE_URL=jdbc:postgresql://host:5432/db?sslmode=require
DATABASE_USERNAME=user
DATABASE_PASSWORD=pass
```

Most managed providers require `sslmode=require`.

Flyway creates the schema on first boot. There is no manual migration step.

---

## 2. Backend

Build and run the container:

```bash
docker build -t mathstrokes-api ./backend
docker run -p 8080:8080 --env-file .env mathstrokes-api
```

Most hosts will build from the repository instead — point them at `backend/Dockerfile`.

### Required environment

```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
JWT_SECRET=<48+ random bytes, base64>
CORS_ALLOWED_ORIGINS=https://your-frontend-domain
```

Generate the secret with `openssl rand -base64 48`. Store it in the host's secret manager, never in
the repository. Changing it signs everybody out, which is the correct behaviour if it leaks.

### First run only

```
SEED_ENABLED=true
SEED_ADMIN_NAME=...
SEED_ADMIN_PHONE=...
SEED_ADMIN_PASSWORD=<a real password>
SEED_ADMIN_SECURITY_QUESTION=...
SEED_ADMIN_SECURITY_ANSWER=...
```

Set `SEED_ENABLED=false` afterwards. The seeder will not overwrite an existing admin, but leaving
the credentials in the environment is pointless once the account exists.

The seeder deliberately refuses to invent a default password: if these values are missing it logs a
warning and creates nothing. A known default admin password on a public instance is an open door.

### Sizing

The container runs comfortably in 512 MB. `MaxRAMPercentage=75` and the serial collector are set
for small instances; drop `-XX:+UseSerialGC` if you give it more than about a gigabyte.

`DB_POOL_MAX` defaults to 10 — lower it to 5 or so if your database plan caps connections, which
free tiers usually do.

---

## 3. Frontend

```bash
cd frontend
npm ci
npm run build
```

Deploy `dist/frontend/browser` (Angular 22 emits a `browser` subdirectory).

Point the app at the deployed API by setting the API base URL in the frontend environment
configuration before building, then make sure that origin is in `CORS_ALLOWED_ORIGINS` on the
backend.

**Configure SPA fallback.** The router uses real paths, so a host that does not rewrite unknown
paths to `index.html` will 404 on a refresh of any deep link.

| Host | How |
|---|---|
| Netlify | `_redirects` containing `/* /index.html 200` |
| Vercel | `vercel.json` with a rewrite of `/(.*)` to `/index.html` |
| Cloudflare Pages | automatic for SPAs |
| nginx | `try_files $uri $uri/ /index.html;` |

---

## 4. After deploying

Check, in order:

```bash
curl https://your-api/api/actuator/health          # {"status":"UP"}
curl https://your-api/api/auth/security-questions  # the canonical list
```

Then sign in as the seeded admin, change the password, and confirm the chapters and sample
questions are present.

If the frontend loads but every request fails, it is almost always CORS: `CORS_ALLOWED_ORIGINS`
must contain the frontend's exact origin, scheme included, with no trailing slash.

---

## Production checklist

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET` from a secret manager, at least 32 characters, never committed
- [ ] `CORS_ALLOWED_ORIGINS` set to the real frontend origin only
- [ ] `SEED_ENABLED=false` once the admin exists
- [ ] The seeded admin password changed
- [ ] `SWAGGER_ENABLED` unset or false
- [ ] Database SSL required
- [ ] `DB_POOL_MAX` within the database plan's connection limit
- [ ] HTTPS everywhere — tokens travel in the `Authorization` header
- [ ] Backups configured on the database

What the `prod` profile already does for you: disables Swagger, disables `flyway.clean`, suppresses
stack traces and error messages in responses, and drops logging to `WARN` for everything outside
the application's own packages.

---

## Operational notes

**Migrations.** Flyway runs on boot and is transactional per migration. Never edit an applied
migration; add a new one. A failed migration stops startup rather than leaving a half-applied
schema.

**Zero-downtime deploys** are safe as long as a migration is backward-compatible with the version
still running. Add columns as nullable, backfill, then tighten in a later release.

**Scaling out** is possible but two things are currently per-instance: the rate limiter, which is
in-process, and the expiry sweep, which every instance would run. The sweep is safe to duplicate —
finalisation is idempotent and each attempt is handled in its own transaction — but it is wasted
work. Both are noted in `ARCHITECTURE.md` under known limitations.

**What to watch.** `/api/actuator/health` for liveness, and the log line
`Finalising N attempt(s) whose time has expired` — a steadily growing N means the sweep is falling
behind.
