# iota

**An online Mathematics examination platform for JEE aspirants.**

> **Live:** https://iota-jee.netlify.app
> The frontend is deployed. The API is not yet hosted, so sign-in does not work on the public
> site — see [Deployment status](#deployment-status).

*iota* is the Greek letter iota, the standard name in Indian mathematics teaching for the
imaginary unit $i = \sqrt{-1}$, and also the word for the smallest quantity.

A teacher writes mathematics questions in LaTeX and publishes timed tests. A student sits a
25-question, 60-minute paper one question at a time, with a server-authoritative countdown and
autosaved answers, then gets a scored, ranked result with analytics.

Built as a modular monolith: Angular in front, Spring Boot and PostgreSQL behind.

**Full reference:** [`docs/iota-handbook.pdf`](docs/iota-handbook.pdf) — 52 pages covering the
data model, every endpoint, the admin question-authoring flow and deployment.

---

## Deployment status

| Piece | Status | Where |
|---|---|---|
| Frontend | **Live** | https://iota-jee.netlify.app (Netlify) |
| Backend API | **Live** | `iota-api-jjai.onrender.com/api` — container from `backend/Dockerfile` |
| Database | **Live** | Render PostgreSQL 18.6, private network only |

Netlify serves static files and **cannot run a JVM**, so the API has its own host. The browser
calls it directly rather than through the Netlify proxy — [Where it lives](#where-it-lives)
explains why.

> **The database is on a 30-day timer.** Free Render PostgreSQL instances expire 30 days after
> creation, allow a 14-day grace period, and are then deleted along with their data. The free tier
> supports no backup mechanism at all. A move to Northflank, whose free tier is always-on and
> permits scheduled backups, is written up in
> [`docs/MIGRATING-TO-NORTHFLANK.md`](docs/MIGRATING-TO-NORTHFLANK.md). Until that lands, take a
> copy with `scripts/backup-db.sh` and keep it somewhere other than this machine.

---

## Contents

1. [Deployment status](#deployment-status)
2. [Repository layout](#repository-layout)
3. [Features](#features)
4. [Architecture](#architecture)
5. [Technology stack](#technology-stack)
6. [Prerequisites](#prerequisites)
7. [Local setup](#local-setup)
8. [Environment variables](#environment-variables)
9. [Database and migrations](#database-and-migrations)
10. [The data layer](#the-data-layer)
11. [Seed data](#seed-data)
12. [API documentation](#api-documentation)
13. [Authentication](#authentication)
14. [Taking a test](#taking-a-test)
15. [Writing questions in LaTeX](#writing-questions-in-latex)
16. [The marking engine](#the-marking-engine)
17. [Ranking](#ranking)
18. [Testing](#testing)
19. [Deployment](#deployment)
20. [Future work](#future-work)

---

## Repository layout

```
.
├── backend/                    Spring Boot API (Java 21)
│   ├── src/main/java/com/mathstrokes/
│   │   ├── auth/               registration, login, rotating refresh tokens, recovery
│   │   ├── user/               accounts, roles, student roster
│   │   ├── catalog/            subjects and chapters
│   │   ├── question/           the LaTeX question bank
│   │   ├── marking/            marking schemes and evaluation strategies
│   │   ├── exam/               tests, publication, question selection
│   │   ├── attempt/            sitting a paper: snapshot, autosave, submission, evaluation
│   │   ├── ranking/            leaderboards and percentiles
│   │   ├── analytics/          dashboards and question quality
│   │   ├── security/           JWT, filters, authorisation
│   │   └── common/             error envelope, enums, shared DTOs
│   ├── src/main/resources/db/migration/    V1..V5 Flyway migrations
│   ├── src/test/java/                      63 unit tests
│   └── Dockerfile
├── frontend/                   Angular 22 (standalone, zoneless, signals)
│   └── src/app/
│       ├── core/               auth store, HTTP layer, guards, models
│       ├── shared/             KaTeX renderer, logo, form helpers
│       └── features/           auth, student, exam, results, admin
├── docs/
│   ├── iota-handbook.pdf       52-page reference
│   ├── API.md                  endpoint reference and error contract
│   ├── ARCHITECTURE.md         12 decision records
│   └── DEPLOYMENT.md           hosting walkthrough
├── e2e/                        end-to-end checks (105 assertions)
├── netlify.toml                frontend build, API proxy, SPA fallback, headers
└── render.yaml                 backend service and database blueprint
```

The backend package names are `com.mathstrokes.*` and the database is `mathstrokes`. Those are
internal identifiers and were deliberately left alone at the rename: changing them touches every
file and is invisible to a user.

---

## Features

### Teacher (ROLE_ADMIN)

- Manage subjects and chapters; the student app reads these from the API rather than hardcoding them.
- Write questions in LaTeX with a live KaTeX preview of the stem, every option and the solution.
- Classify by exam pattern (JEE Main / JEE Advanced), difficulty (Easy / Medium / Hard) and
  question type (single correct / multiple correct) — three independent axes.
- Draft, publish, revert and archive questions; search and filter the bank with pagination.
- Configure marking schemes, including JEE Advanced partial credit, without touching code.
- Create tests, publish them, close them, and watch attempts and results come in.
- See which questions students actually find hard, measured rather than assumed.

### Student (ROLE_STUDENT)

- Register with name, phone number, password and a security question.
- Recover a forgotten password by answering that question — knowing a phone number is not enough.
- Browse published tests by chapter and exam pattern.
- Sit a 25-question, 60-minute paper: one question at a time, palette navigation, mark for review,
  answers autosaved as they go.
- Refresh, crash or close the tab and come back to the same paper with the clock still running.
- Get a scored result with rank, percentile, accuracy, attempt rate and negative marks, plus a
  question-by-question review with worked solutions.
- Track performance over time and by chapter.

---

## Architecture

```
Angular 22 (standalone, zoneless, signals)
        |
        |  HTTPS / REST, JWT bearer
        v
Spring Boot 3.5  (modular monolith)
        |
        +-- auth        registration, login, rotating refresh tokens, recovery
        +-- user        accounts, roles, student roster
        +-- catalog     subjects and chapters
        +-- question    the LaTeX question bank
        +-- marking     marking schemes and the evaluation strategies
        +-- exam        tests, publication, question selection
        +-- attempt     sitting a paper: snapshot, autosave, submission, evaluation
        +-- ranking     leaderboards and percentiles
        +-- analytics   dashboards and question quality
        |
        v
PostgreSQL 17
```

Each module owns its `controller / service / repository / entity / dto / mapper` layers.
Controllers carry no business logic and no entity is ever serialised to a client.

The module boundaries are real: they are the seams along which services could be extracted later
if that ever becomes necessary. It is not necessary now, and the project deliberately avoids
microservices, Kafka, Redis and Kubernetes for a first release that has to be cheap to run and
easy to reason about.

Three decisions do most of the work, and they are explained in full in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md):

- **Attempts carry an immutable snapshot of every question**, its options, the answer key and the
  marking rules. Editing a question later cannot move a historical mark.
- **The server owns the clock.** `expires_at` is written once when an attempt starts and is never
  extended. The browser countdown is a display of the server's number, not a source of truth.
- **Evaluation and ranking happen only on the server**, from data the client cannot see.

---

## Technology stack

| Layer            | Choice                                                        |
|------------------|---------------------------------------------------------------|
| Frontend         | Angular 22 — standalone components, zoneless, signals, SCSS   |
| Maths rendering  | KaTeX                                                          |
| Charts           | Chart.js                                                       |
| Backend          | Java 21 (LTS), Spring Boot 3.5                                 |
| Security         | Spring Security, JJWT — access token + rotating refresh token  |
| Persistence      | Spring Data JPA / Hibernate 6, PostgreSQL 17                   |
| Migrations       | Flyway                                                         |
| API docs         | springdoc-openapi (OpenAPI 3)                                  |
| Build            | Maven wrapper, Angular CLI                                     |
| Tests            | JUnit 5, Mockito, AssertJ                                      |

---

## Prerequisites

- **JDK 21** — `java -version` should report 21.x
- **Node.js 20+** and npm
- **PostgreSQL 17** running locally (or any reachable PostgreSQL 14+)
- **Git**

Maven itself is not required; the repository ships the Maven wrapper (`./mvnw`).

---

## Local setup

```bash
git clone https://github.com/vivekkumarq/MathStrokes.git
cd MathStrokes
```

### 1. PostgreSQL

Create a role and a database. As the `postgres` superuser:

```sql
CREATE ROLE mathstrokes LOGIN PASSWORD 'choose-a-password';
CREATE DATABASE mathstrokes OWNER mathstrokes;
```

On Windows, `psql` lives at `C:\Program Files\PostgreSQL\17\bin\psql.exe`.

### 2. Environment

```bash
cp .env.example .env
```

Edit `.env` and set at minimum `DATABASE_PASSWORD`, a strong `JWT_SECRET`, and the three
`SEED_ADMIN_*` values. `.env` is gitignored and must never be committed.

Generate a secret with:

```bash
openssl rand -base64 48
```

### 3. Backend

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Flyway creates the schema and loads the reference data and sample questions on first start.

### 4. Frontend

```bash
cd frontend
npm ci
npm start
```

Open <http://localhost:4200>. The API is on <http://localhost:8080/api>.

> The backend reads configuration from the environment, not from `.env` directly. Export it first:
> ```bash
> set -a; . ../.env; set +a       # bash / git-bash
> ```
> ```powershell
> Get-Content ..\.env | Where-Object { $_ -match '^[A-Z]' } | ForEach-Object {
>   $k,$v = $_ -split '=',2; [Environment]::SetEnvironmentVariable($k, $v.Trim('"'))
> }
> ```

---

## Environment variables

| Variable | Required | Default | Notes |
|---|---|---|---|
| `DATABASE_URL` | yes | `jdbc:postgresql://localhost:5432/mathstrokes` | JDBC URL |
| `DATABASE_USERNAME` | yes | `mathstrokes` | |
| `DATABASE_PASSWORD` | yes | — | |
| `JWT_SECRET` | **yes** | — | At least 32 characters. The app refuses to start without it. |
| `JWT_ACCESS_TOKEN_EXPIRATION` | no | `PT15M` | ISO-8601 duration |
| `JWT_REFRESH_TOKEN_EXPIRATION` | no | `P7D` | |
| `JWT_PASSWORD_RESET_TOKEN_EXPIRATION` | no | `PT10M` | |
| `CORS_ALLOWED_ORIGINS` | yes in prod | `http://localhost:4200` | Comma separated |
| `SPRING_PROFILES_ACTIVE` | no | `dev` | `dev` or `prod` |
| `PORT` | no | `8080` | |
| `SEED_ENABLED` | no | `true` | Set `false` in production once seeded |
| `SEED_ADMIN_PHONE` / `SEED_ADMIN_PASSWORD` / `SEED_ADMIN_SECURITY_ANSWER` | first run | — | All three needed to create the first admin |
| `RATE_LIMIT_ENABLED` | no | `true` | |
| `SWAGGER_ENABLED` | no | `false` in prod | |

There are no credentials anywhere in the repository. The seeder refuses to invent a default admin
password: if the three `SEED_ADMIN_*` values are missing it logs a warning and creates nothing,
because a known default password on a deployed instance is an open door.

---

## Database and migrations

Flyway owns the schema; Hibernate is set to `validate` and will refuse to start if the mapping and
the schema disagree. Migrations live in `backend/src/main/resources/db/migration`:

| Migration | Contents |
|---|---|
| `V1__initial_schema.sql` | 17 tables, constraints and indexes |
| `V2__seed_reference_data.sql` | Roles, Mathematics, 17 chapters, 4 marking schemes |
| `V3__seed_sample_questions.sql` | 66 published LaTeX questions |
| `V4__seed_question_bank.sql` | 11 further chapters and 1,456 questions |
| `V5__seed_chapter_tests.sql` | A chapter test per chapter and exam pattern |
| `V6__full_syllabus_tests.sql` | Makes `chapter_id` nullable; 2 full-syllabus papers |

Migrations are never edited once applied — add a new `V7__...` instead. This is not a style
preference. Flyway records a checksum for every migration it has run, and editing an applied file
makes that checksum disagree on the next boot, which stops the whole application rather than just
the migration. A comment in `V5` is inaccurate for exactly this reason: it was cheaper to carry the
correction forward in `V6` than to touch a file that had already run.

`V4` embeds LaTeX-heavy JSON, so it uses dollar-quoted PL/pgSQL (`$seedjson$`) rather than string
literals — otherwise every backslash in the LaTeX has to be escaped twice. Flyway's placeholder
replacement is switched off in configuration for the same reason: without that, a JSON object
opening `${` is read as a Flyway placeholder and the migration fails to parse.

To start over locally:

```sql
DROP DATABASE mathstrokes;
CREATE DATABASE mathstrokes OWNER mathstrokes;
```

---

## The data layer

Everything about where the data sits, how it is shaped, what it weighs and what will run out
first. The figures below were measured with `pg_column_size`, `pg_total_relation_size` and
`pg_stat_user_tables` against a **local database carrying the full seeded bank plus 15 real
attempts** — they are not read from the deployed instance, which has no public endpoint. A
production database that has been running longer will show more attempt rows and a larger total;
the per-row costs and the ratios are what carry over.

### Where it lives

| Layer | Host | Address | Notes |
|---|---|---|---|
| Frontend | Netlify CDN | `iota-jee.netlify.app` | Static Angular bundle. No data at rest. |
| API | Render, Docker, Singapore | `iota-api-jjai.onrender.com/api` | Free plan |
| Database | Render PostgreSQL 18.6 | `iota-db`, private network only | No public ingress |

Development runs PostgreSQL 17 and production is on 18.6, which is worth knowing because Flyway
11.7.2 logs a warning on every production boot: its latest tested version is 17. Nothing has
misbehaved, and the migrations use no version-specific syntax, but the combination is untested
upstream rather than blessed.

The browser calls the API directly rather than through the Netlify proxy. The proxy is still
configured, but its gateway gives up at about 29 seconds — verified in production as three
consecutive 504s at 28.9 s, 29.8 s and 28.5 s, all of them the gateway's ceiling rather than the
backend failing — and a wake can exceed that. Routing through it turned the first request of the
day into a 504. Going direct costs a CORS preflight of a few milliseconds and removes the ceiling,
since the browser's own timeout is measured in minutes. CORS is configured for the Netlify origin
instead.

The database has no public endpoint, which is the right default but has a practical consequence:
it cannot be opened from a laptop with pgAdmin or `psql`. Everything about production has to be
reached through the API.

It also has an expiry date. Free Render PostgreSQL instances are deleted 30 days after creation
plus a 14-day grace period, and the free tier offers no backups, so the only durable copy is one
taken deliberately with `scripts/backup-db.sh`. That script refuses to run when the local
`pg_dump` is older than the server — development is on 17 and production on 18.6, so the client
that works locally cannot back up production, and finding that out during an emergency would be
the wrong time. The planned move to an always-on host with scheduled backups is in
[`docs/MIGRATING-TO-NORTHFLANK.md`](docs/MIGRATING-TO-NORTHFLANK.md).

### How the connection is wired

Render injects `DATABASE_URL` through `fromDatabase` in `render.yaml`, so the password is generated
by the platform and never written into the repository. It arrives in libpq form, which JDBC cannot
parse:

```
Render provides   postgresql://user:pass@host/db
JDBC needs        jdbc:postgresql://host/db  + user and password supplied separately
```

`DatabaseUrlEnvironmentPostProcessor` rewrites it. It is registered in `META-INF/spring.factories`
as an `EnvironmentPostProcessor` rather than declared as a bean, because by the time beans are
constructed the datasource has already tried to start and failed.

### Seventeen tables in four layers

The schema divides by how often each layer changes. Reference data is written once by a migration,
the question bank is written by the teacher, papers are assembled from the bank, and attempts are
written by students and then frozen. Nothing in a lower layer is ever mutated by a higher one.

| Table | Rows | Holds |
|---|---:|---|
| **Reference** — seeded once, effectively constant | | |
| `roles` | 2 | `ROLE_ADMIN`, `ROLE_STUDENT` |
| `subjects` | 1 | Mathematics |
| `chapters` | 28 | Syllabus units the bank and papers hang off |
| `marking_schemes` | 4 | JEE Main and Advanced rules, stored as JSONB |
| **Question bank** — authored, versioned, archivable | | |
| `questions` | 1,522 | LaTeX stem, explanation, difficulty, status, pattern |
| `question_options` | 6,088 | Four per question; holds the answer key |
| **Papers** — which questions form which test | | |
| `tests` | 61 | 58 on a fresh database; see [Seed data](#seed-data) for why this one has 61 |
| `test_questions` | 1,525 | Ordered membership, one row per question per paper |
| **Attempts** — append-mostly, frozen on submission | | |
| `test_attempts` | 15 | One per sitting; owns `expires_at` and the final score |
| `attempt_questions` | 375 | Snapshot of each question as it was at start |
| `attempt_question_options` | 1,500 | Snapshot of the options and the key |
| `student_answers` | 169 | What was selected, plus mark-for-review state |
| `student_answer_options` | 269 | Selected labels; several rows for multiple-correct |
| `question_attempt_results` | 250 | Marks awarded per question, written at evaluation |
| **Identity** | | |
| `users` | 36 | Phone, BCrypt hash, security question and answer hash |
| `user_roles` | 36 | Join table |
| `refresh_tokens` | 99 | SHA-256 digests, family, revocation state |

### The snapshot, and what it costs

This is the decision the whole data model is built around, and it is why four of the seventeen
tables exist.

When a student starts a test, the platform does not store references to questions. It **copies**
the stem, every option, the answer key and the marking configuration onto the attempt. From that
point the attempt is self-contained and never reads the question bank again.

The requirement is that later admin edits must not alter old results, and it is far easier to
satisfy up front than to retrofit. Without the copy, fixing a typo in a stem months later silently
rewrites what a student saw, and correcting a wrong answer key retroactively changes scores that
have already been published. With the copy, both are impossible by construction rather than by
remembering to be careful.

This was checked rather than assumed: a question was rewritten, its answer key flipped and the
question archived, then the old attempt's stored result was re-read. It came back byte-identical.

The cost is paid in one burst when the attempt starts:

| Table | Rows written |
|---|---:|
| `test_attempts` | 1 |
| `attempt_questions` | 25 |
| `attempt_question_options` | 100 |
| **Total before the first question renders** | **126** |

126 inserts in a single transaction is the heaviest thing the application does. It is a fixed cost
per sitting rather than one that grows, but it lands on a click the student is watching, which is
why starting a test feels slower than answering one.

### What it weighs

| Table | Total | Heap | Indexes |
|---|---:|---:|---:|
| `question_options` | 1160 kB | 552 kB | 568 kB |
| `questions` | 712 kB | 464 kB | 208 kB |
| `test_questions` | 448 kB | 120 kB | 296 kB |
| `attempt_questions` | 432 kB | 272 kB | 128 kB |
| `attempt_question_options` | 400 kB | 160 kB | 200 kB |
| `question_attempt_results` | 120 kB | 32 kB | 64 kB |
| `refresh_tokens` | 112 kB | 24 kB | 64 kB |
| `tests` | 104 kB | 24 kB | 48 kB |
| Everything else | under 100 kB each | | |

Whole database: **12 MB**.

Two things are worth noticing. The question bank dominates — `questions` and `question_options`
together are about 1.9 MB — and that is with 1,522 questions of LaTeX. And the attempt tables are
already comparable in size to the bank after only 15 attempts. That crossover is the entire
capacity story.

### Are the indexes earning their place

The busiest, by scan count:

| Index | Scans | Serving |
|---|---:|---|
| `chapters_pkey` | 6,470 | Chapter lookup on nearly every screen |
| `idx_attempt_options_question` | 4,831 | Rendering options during a live attempt |
| `questions_pkey` | 2,397 | Bank reads and snapshot copying |
| `idx_chapters_subject_active` | 1,468 | The chapter list students browse |
| `idx_question_options_question` | 595 | The admin question editor |

Fifteen indexes report zero scans, the largest being `uq_question_options_key` at 208 kB. They
should not be dropped. Every one is a **unique constraint**, and a unique constraint does its work
on write rather than on read — `idx_scan` counts query planning, so a constraint that has silently
rejected every duplicate ever inserted still reports zero. They are the reason the data is
consistent, not dead weight.

The one real finding is `questions`: 6,352 sequential scans reading 4.67 million tuples between
them. Almost all of that is migration seeding and the random selection that assembles a
full-syllabus paper, both of which are inherently full-table operations. Not a problem now; the
first thing to revisit if the bank grows ten-fold.

### How much fits

Measured against real rows rather than estimated from column types. One completed 25-question
attempt costs:

| Table | Rows × size | Bytes |
|---|---|---:|
| `attempt_questions` | 25 × 620 B | 15,500 |
| `attempt_question_options` | 100 × 101 B | 10,100 |
| `question_attempt_results` | 25 × 105 B | 2,625 |
| `student_answers` | 25 × 96 B | 2,400 |
| `student_answer_options` | ~40 × 90 B | ~3,600 |
| `test_attempts` | 1 × 168 B | 168 |
| **Row payload** | | **~34 kB** |
| Index entries, ~45% observed | | ~16 kB |
| **All-in, per attempt** | | **~50 kB** |

Against a 1 GB allowance that is roughly **20,000 attempts**, or about 800 students sitting 25
tests each. Storage is not the constraint: the deployment is using about 1.2% of it, and the bank —
the part that feels large because it was authored by hand — is a rounding error against what
attempts will eventually occupy. Adding a tenth chapter of questions costs less than two students
sitting one test.

The 1 GB figure is Render's documented free-tier allowance, and it is fixed rather than a soft
quota. The open question beside it — whether the free instance carries an expiry date — has since
been answered, and badly: it expires 30 days after creation, allows 14 days' grace, and is then
deleted with its data, with no backup mechanism on that tier. That matters more than anything
below, because it ends with the data gone rather than merely slow. The creation date of `iota-db`
is therefore the single most important number about this deployment, and it is only visible on the
Render dashboard.

### What runs out first

Ranked by what a student would actually notice, except the first, which nobody notices until it
has already happened. The database's *size* is not near the top; its *expiry* is at the very top.

| Limit | Severity | Detail |
|---|---|---|
| Free database expiry | **Worst** | Free Render PostgreSQL expires 30 days after creation, allows 14 days' grace, then is deleted with its data — and the free tier supports no backups. This is the only limit here that destroys work rather than delaying it. `scripts/backup-db.sh` is the interim answer; [migrating off](docs/MIGRATING-TO-NORTHFLANK.md) is the real one. |
| Cold start | Bad, and poorly understood | The free service sleeps after ~15 minutes idle. Measured after a deliberate 17-minute idle: a 0.49 s preflight and a 7.97 s sign-in, against 1.4 s warm. But two keep-alive runs got no response at all within 120 s, and that has not been reproduced on demand, so the wake cost is usually seconds and occasionally far worse for reasons not yet established. A preflight answering in 0.49 s beside a 7.97 s login points at the connection pool, not a JVM boot — the preflight never touches the database. |
| The keep-warm Action | Ineffective | Scheduled `*/10`, it fired **four times in 21 hours** against an expected 126. GitHub deprioritises high-frequency schedules on shared runners; no cron tuning fixes that. An external pinger or an always-on host is the answer. |
| 126 inserts on start | By design | The price of historical integrity, paid where the student is watching. Fixed per attempt, but thirty students starting together is 3,780 inserts arriving at once, and that shape has not been load-tested. |
| Connection pool | Watch | Local PostgreSQL allows 100 connections; free hosted tiers usually allow considerably fewer, and the pool is sized for the generous case. The failure mode is connection exhaustion surfacing as timeouts rather than as a clear error. Worth pinning the pool size once the real limit is known. |
| Sequential scans on `questions` | Latent | Fine at 1,522 questions. The first thing to revisit at ten times that. |
| Storage | Not a concern | 12 MB of roughly 1 GB. Every other limit here arrives first. |

### Four things the schema will not let you do wrong

Correctness is enforced by the schema rather than by application code remembering to check — the
distinction that matters when the code is later changed by someone who has forgotten why.

- **Answer keys cannot leak before submission.** The key lives on the snapshot rows, and
  serialisation omits nulls rather than sending them, so an unsubmitted attempt does not send a
  null key whose shape a client could notice. Absence is the signal.
- **The clock belongs to the server.** `expires_at` is written once at start and never
  recalculated. Every attempt response carries `serverTime` and `remainingSeconds`, so changing the
  clock on the student's machine changes nothing. A sweep finalises attempts whose time ran out
  while they were disconnected.
- **Refresh tokens are single-use and detect replay.** Stored as SHA-256 digests and rotated on
  every use. Presenting an already-used token revokes the whole family, which is the right response
  to a stolen token because there is no way to tell the thief from the victim.
- **Migrations are validated, not trusted.** `ddl-auto: validate` makes Hibernate refuse to start
  if the entities and the schema disagree, so a mapping change without a migration fails at boot
  rather than at runtime.

---

## Seed data

On a fresh database the application creates:

- **One admin**, from the `SEED_ADMIN_*` environment variables.
- **Mathematics** and its 28 chapters.
- **Four marking schemes**, one per (exam pattern, question type) pair.
- **1,522 published questions** with LaTeX stems, options and worked solutions, across the whole
  syllabus — quadratic equations, complex numbers, integrals, limits, matrices, determinants,
  vectors, conic sections, probability and the rest.
- **58 published tests**, each 25 questions over 60 minutes: 56 chapter tests — one per chapter per
  exam pattern, 28 × 2 — and 2 full-syllabus papers that draw one question per chapter.

Each chapter test exists in a JEE Main form (single correct, +4 / −1) and a JEE Advanced form
(multiple correct with partial marking), so both marking engines are exercised by real data on a
fresh database.

There is also a Java `TestSeeder` that creates two sample tests, but on a fresh database it never
fires. Flyway runs before the application seeders, so `V5` has already inserted its 56 tests by the
time the seeder looks; the seeder skips when any test exists, and does nothing. It only produces
tests on a database that was migrated before `V5` was written — which is why a long-lived
development database can show a higher count than a freshly migrated one, with a chapter appearing
twice in the same exam pattern. That never happens on a database created from empty.

The questions were generated from known integer roots rather than typed by hand, so every answer
key agrees with its own worked solution.

Both seeders skip silently if an admin or any test already exists, so restarting the application
never overwrites real data.

---

## API documentation

With the backend running:

- OpenAPI document — <http://localhost:8080/api/v3/api-docs>
- Swagger UI — <http://localhost:8080/api/swagger-ui.html>

Both are enabled on the `dev` profile and **disabled on `prod`** unless `SWAGGER_ENABLED=true`.

A hand-written reference, including the exact error envelope and the shape of every response the
frontend depends on, is in [`docs/API.md`](docs/API.md).

Every failure returns the same envelope:

```json
{
  "timestamp": "2026-08-30T00:30:43.285Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/auth/student/register",
  "fieldErrors": [
    { "field": "phoneNumber", "message": "Phone number must be 10 to 15 digits, with no spaces or symbols" }
  ]
}
```

`error` is a stable enum name — branch on it, never on `message`. `fieldErrors[].field` is the JSON
property name of the request body, so a form can bind a server message straight onto the input that
caused it. `rejectedValue` is withheld for password, security-answer and token fields, so a
submitted password is never written into a response body or a network log.

Null fields are omitted from responses rather than sent as `null`. Model optional fields as
`field?: T`.

---

## Authentication

Phone number and password. There is no email anywhere in the system.

```
POST /api/auth/student/register     name, phone, password, security question and answer
POST /api/auth/login                phone + password
POST /api/auth/refresh              exchange a refresh token for a new pair
POST /api/auth/logout               revoke a refresh token
GET  /api/auth/security-questions   the canonical list shown at registration
POST /api/auth/forgot-password/initiate   returns only the security question
POST /api/auth/forgot-password/verify     answer it, receive a single-use reset token
POST /api/auth/reset-password             set the new password
```

- Passwords and security answers are hashed with **BCrypt at strength 12**. Neither is mapped into
  any DTO, so neither can be serialised into a response by accident.
- The **access token** is a short-lived JWT (15 minutes by default) sent as `Authorization: Bearer`.
- The **refresh token** is persisted, stored as a SHA-256 digest, and **rotated on every use**:
  presenting it revokes it and issues a replacement. Presenting an already-rotated token is treated
  as a stolen credential being replayed and revokes every session for that account.
- Every token carries a type claim, so an access token cannot be replayed as a refresh token or as
  a password-reset authorisation.
- Login is deliberately unable to distinguish a wrong password from an unknown account, so the
  endpoint cannot be used to discover which phone numbers are registered.
- Login, the security-answer challenge and recovery initiation are rate limited per identifier.

Recovery cannot be short-circuited: knowing a phone number returns nothing but the question. Only a
correct answer mints a reset token, that token authorises exactly one password change, and applying
it revokes every existing session for the account.

---

## Taking a test

1. The student picks a published test. `GET /api/tests` says whether they can start it and whether
   one is already in flight.
2. `POST /api/attempts` creates the attempt. The server draws the paper, **snapshots** every
   question onto the attempt, writes `started_at` and `expires_at`, and returns the whole
   25-question paper in one response.
3. Answers autosave. Each write is checked four ways: the attempt belongs to the caller, it is
   still `ACTIVE`, the server clock says it has not expired, and the question belongs to that
   attempt.
4. Submission is manual, or automatic when the clock runs out.
5. The server evaluates from the snapshot, stores per-question results, recomputes the leaderboard,
   and only then releases the answer key.

**Refreshing changes nothing.** Calling `POST /api/attempts` again while an attempt is live resumes
it: same questions, same order, same deadline. Verified end to end.

**The clock is the server's.** Every attempt response carries
`timing: { serverTime, startedAt, expiresAt, remainingSeconds, expired }`. `remainingSeconds` is
the server's own arithmetic; `serverTime` lets the client correct for clock drift instead of
trusting the browser. Nothing extends `expires_at` once written.

**Abandoning a test still gets it marked.** A scheduled sweep finalises attempts whose time has
run out, scoring the answers already saved. A student who closes the tab, loses power or walks
away is still evaluated on their work.

**Nothing is lost to a bad connection.** Each save carries a monotonic `clientSequence`; a write
arriving with an older sequence is a late packet and is discarded rather than allowed to overwrite
newer work. The acknowledgement returns the state the server actually holds, so the client
reconciles instead of guessing.

**The answer key is absent, not hidden.** The DTO a live attempt returns has no correctness field
at all, and no solution text — they are structurally different types from the review payload, so
the key cannot leak through a forgotten filter.

---

## Writing questions in LaTeX

The teacher types LaTeX; the browser renders it with KaTeX; **the source is what gets stored**.
Rendered HTML is never persisted, so the same content can be re-rendered by a different renderer
later without a migration.

```
Admin types LaTeX  ->  KaTeX live preview  ->  saved as source in PostgreSQL
                                                        |
                          KaTeX renders  <-  student fetches the source
```

Delimiters, and only these two:

| Form | Use | Example |
|---|---|---|
| `$ ... $` | inline | `If $\alpha$ and $\beta$ are the roots...` |
| `$$ ... $$` | displayed | `$$x^2 + 5x + 6 = 0$$` |

Stems, every option and the solution all support LaTeX. Content is multi-line and blank lines are
meaningful paragraph breaks — they are preserved verbatim.

```
Solve for $x$:

$$x^2 - 5x + 6 = 0$$
```

```
$$\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}$$
```

```
$$\int_0^1 x^2 \, dx$$
```

**Content is LaTeX and plain text, never HTML.** The client escapes everything outside the maths
delimiters and runs KaTeX with HTML trust disabled. As defence in depth the server also refuses to
save content containing `<script`, `<iframe`, `<object`, `<embed`, `javascript:` or an inline event
handler. The check is deliberately narrow: `$a < b$` is ordinary mathematics and is accepted.

---

## The marking engine

Marks are computed on the server, from a configuration stored as JSON, by one strategy per question
type. Nothing about scoring is hardcoded in a controller, and nothing is computed in the browser.

```java
public interface EvaluationStrategy {
    QuestionType supportedType();
    AnswerEvaluation evaluate(Set<Long> correctOptionIds, Set<Long> selectedOptionIds,
                              MarkingConfig config);
}
```

Implementations are discovered as Spring beans, so supporting a new question type means adding one
class and one marking scheme row — no change to the evaluation pipeline.

**Single correct** — the key scores `fullCorrectMarks`, anything else scores `wrongMarks`, no
selection scores `unansweredMarks`.

**Multiple correct**, in order:

| Situation | Result |
|---|---|
| Nothing selected | `unansweredMarks`, `UNANSWERED` |
| Any option outside the key selected | `wrongMarks`, `INCORRECT` |
| The key selected exactly | `fullCorrectMarks`, `CORRECT` |
| A strict subset of the key, nothing wrong | partial credit, `PARTIALLY_CORRECT` |

The wrong-selection check comes **before** partial credit, which is what makes the JEE Advanced
scheme come out right.

Seeded schemes:

| Pattern / type | Correct | Partial | Wrong | Blank |
|---|---|---|---|---|
| JEE Main, single correct | +4 | — | -1 | 0 |
| JEE Main, multiple correct | +4 exact | none | -1 | 0 |
| JEE Advanced, single correct | +3 | — | -1 | 0 |
| JEE Advanced, multiple correct | +4 exact | +1 per correct option, capped at +3 | -2 | 0 |

Configurations are validated on write — partial credit that could match or beat a perfect answer
is rejected, as is a positive penalty.

Marks are `NUMERIC`/`BigDecimal` throughout, never floating point, so partial-marking arithmetic is
exact.

Reported metrics:

```
accuracy    = correct / attempted   x 100      (0 when nothing was attempted)
attemptRate = attempted / total     x 100
```

A partially correct answer counts as attempted but not as correct, so accuracy reads as the share
of attempts that were fully right.

---

## Ranking

**Cohort.** Students are compared only when they sat the *same* paper. A test whose questions are
drawn per attempt is not ranked at all, because two such attempts are different examinations.
Only `FIXED_SET` tests — whose 25 questions are materialised once at publication — carry
`rankingEnabled: true`.

**One position per student.** If a test allows several attempts, only a student's best attempt is
placed, so sitting a paper twice cannot occupy two positions.

**Tie policy — RANK semantics: 1, 2, 2, 4.** Genuinely tied students share a position and the next
rank skips accordingly. Ties break on, in order: total score, then more correct answers, then fewer
incorrect answers, then faster completion, then earlier submission.

**Percentile** follows the NTA definition, computed on raw score alone so a tie-break on time does
not move it:

```
percentile = 100 x (candidates scoring at or below you) / total candidates
```

The topper is at 100.00.

**Recalculation.** The whole cohort is recomputed whenever an attempt is evaluated, in one
PostgreSQL window-function pass. A student who finishes first therefore sees a real position
immediately, and that position can move as later candidates finish — which is what "rank among
everyone who sat this paper" has to mean while the paper is still open. Once a test is `CLOSED` no
new attempts can start and the board settles.

Because a rank shown at submission is a snapshot, a client should re-fetch the result rather than
cache the rank indefinitely.

---

## Testing

```bash
cd backend
./mvnw test            # unit tests, no database required
```

56 tests covering the parts where being wrong is expensive:

| Area | What is asserted |
|---|---|
| Single-correct scoring | correct, wrong, unanswered, and a multi-selection scored as wrong rather than correct |
| Multiple-correct scoring | exact match, partial credit, the cap, a wrong selection overriding partial credit, superset of the key |
| Marking configuration | a partial award that could match a perfect answer is rejected; a positive penalty is rejected |
| Score aggregation | totals, counts, negative marks, accuracy with nothing attempted, a negative total, time capped at the duration |
| Idempotent evaluation | an evaluated attempt is never rescored; existing results are never rewritten |
| Attempt lifecycle | an evaluated attempt is terminal and cannot be reopened |
| Palette state | all five states, including answered-and-marked-for-review |
| Publish rules | exactly one key for single correct, at least one but not all for multiple correct, no duplicate labels |
| Content safety | `$a < b$` accepted, `<script>` / event handlers / `javascript:` refused |
| Security answers | case- and whitespace-insensitive matching |

The following were verified end to end against a running instance and PostgreSQL, and are
documented here because they are the behaviours the platform is judged on:

- A 25-question paper scored 55/100 for 15 correct, 5 wrong and 5 blank, with accuracy 75.00 and
  attempt rate 80.00 — matching an independently computed expectation.
- JEE Advanced partial marking scored 35 for 10 exact, 5 partial and 5 wrong.
- Refreshing resumed the same attempt with identical questions in identical order and a clock that
  did not reset.
- A live attempt's payload contained no correctness field and no solution text anywhere.
- A stale autosave carrying an older sequence was rejected and did not overwrite newer work.
- Another student was refused read, write and submit on the attempt with 403.
- Submitting three times returned the same score every time.
- Expiry rejected further answers, the sweep auto-submitted and scored the saved work, and a late
  manual submit returned the result rather than a conflict.
- A teacher rewrote a question's stem, flipped its answer key and archived it; the student's
  existing score, counts, per-question marks, stem and key were byte-identical afterwards.
- Four students on one test produced ranks 1, 2, 3, 4 with percentiles 100.00, 75.00, 75.00, 25.00,
  and a student who was rank 1 of 1 correctly became rank 2 of 4 as others finished.

---

## Deployment

Detailed instructions, including free-tier hosting, are in
[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md). The move from Render to Northflank — why, and the
step-by-step — is in [`docs/MIGRATING-TO-NORTHFLANK.md`](docs/MIGRATING-TO-NORTHFLANK.md). In
outline:

- **Backend** — a container built from `backend/Dockerfile`, deployable to any free-tier host that
  runs containers. Configure it entirely through environment variables.
- **Database** — any managed PostgreSQL. Flyway migrates on first boot.
- **Frontend** — `npm run build` produces static files for any static host or CDN.

Back the database up before anything else, and keep the copy off the deploying machine:

```bash
scripts/backup-db.sh "<external connection string>"
```

It verifies the dump with `pg_restore --list` before reporting success, and refuses to run when
the local `pg_dump` is older than the server rather than producing a file that only turns out to
be unusable when it is needed.

Production checklist:

- `SPRING_PROFILES_ACTIVE=prod`
- A strong `JWT_SECRET` from a secret store, never from source control
- `CORS_ALLOWED_ORIGINS` set to the real frontend origin
- `SEED_ENABLED=false` once the first admin exists
- `SWAGGER_ENABLED` left off
- The admin's seeded password changed

---

## Future work

Deliberately out of scope for this release, in rough priority order:

- **More question types.** `NUMERICAL`, `INTEGER`, `MATCHING`, `ASSERTION_REASON` and
  `COMPREHENSION`. The extension point is real: add the enum constant, one `EvaluationStrategy`
  bean and a marking scheme row. Only implemented types are declared, so the enum never advertises
  something that would fail at runtime.
- **More subjects.** Physics and Chemistry need no schema change — `subjects` and `chapters` are
  already generic and the client reads them from the API.
- **OTP recovery.** The recovery flow is three separate steps precisely so an OTP challenge can
  replace the security answer without touching the reset step.
- **Required-field metadata in the OpenAPI document.** Java records give springdoc no nullability
  information, so every field currently generates as optional. Fixing it means
  `@Schema(requiredMode = REQUIRED)` across the response DTOs. Until then, generated clients should
  be checked against the documented omit-null rule.
- **Distributed rate limiting.** The current throttle is per-instance and in-process, which is
  honest for a single-node deployment but would need a shared store behind a load balancer.
- **Question versioning as first-class history.** Attempts already snapshot everything they need,
  so results are safe today; a teacher-facing revision history would be additive.
- **Sub-chapters**, bulk question import, and scheduled test windows.

---

## Licence

Proprietary. All rights reserved.
