# Deployment

The application is one container plus a PostgreSQL database plus a folder of static files. That is
deliberately the whole shape of it — there is nothing to orchestrate.

---

## What you need

| Piece | What it is | Free-tier options |
|---|---|---|
| Database | Managed PostgreSQL 14+ | Neon, Supabase, Aiven, Render PostgreSQL |
| Backend | The container from `backend/Dockerfile` | Render, Railway, Fly.io, Koyeb |
| Frontend | Static files from `npm run build` | Netlify, Vercel, Cloudflare Pages, GitHub Pages |

Free tiers change constantly, so pick what is actually on offer when you deploy. Nothing in the
project is tied to a provider: the backend is a plain container configured entirely through
environment variables, and the frontend is plain static output.

Two things to know about free tiers before you pick one:

- **Containers idle out.** A free backend instance is often suspended after ~15 minutes of
  inactivity and takes 30–60 seconds to wake. That is survivable for a demo but not for a real
  examination — a student clicking "Start test" should not wait a minute. Use a paid instance for
  anything real, or keep it warm.
- **The expiry sweep needs the app to be running.** If the container is asleep, attempts whose time
  has run out are not finalised until it wakes. They are finalised correctly when it does, and no
  marks are lost, but a student's result may be delayed.

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
