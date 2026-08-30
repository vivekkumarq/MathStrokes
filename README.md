# iota

**An online Mathematics examination platform for JEE aspirants.**

> **Live:** https://mathstrokes.netlify.app
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
| Frontend | **Live** | https://mathstrokes.netlify.app (Netlify) |
| Backend API | Not yet hosted | Container from `backend/Dockerfile`; `render.yaml` ready |
| Database | Not yet hosted | Managed PostgreSQL; Flyway migrates on first boot |

Netlify serves static files and **cannot run a JVM**, so the API needs its own host. Until it
exists, `netlify.toml` proxies `/api/*` to a placeholder and anything behind a sign-in fails.
Everything works locally — see [Local setup](#local-setup).

The Netlify hostname still reads `mathstrokes` because the site was created before the rename.
That is cosmetic; renaming it changes the public URL.

---

## Contents

1. [Deployment status](#deployment-status)
2. [Repository layout](#repository-layout)
3. [Features](#features)
2. [Architecture](#architecture)
3. [Technology stack](#technology-stack)
4. [Prerequisites](#prerequisites)
5. [Local setup](#local-setup)
6. [Environment variables](#environment-variables)
7. [Database and migrations](#database-and-migrations)
8. [Running the backend](#running-the-backend)
9. [Running the frontend](#running-the-frontend)
10. [Seed data](#seed-data)
11. [API documentation](#api-documentation)
12. [Authentication](#authentication)
13. [Taking a test](#taking-a-test)
14. [Writing questions in LaTeX](#writing-questions-in-latex)
15. [The marking engine](#the-marking-engine)
16. [Ranking](#ranking)
17. [Testing](#testing)
18. [Deployment](#deployment)
19. [Future work](#future-work)

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

Migrations are never edited once applied — add a new `V4__...` instead.

To start over locally:

```sql
DROP DATABASE mathstrokes;
CREATE DATABASE mathstrokes OWNER mathstrokes;
```

---

## Seed data

On a fresh database the application creates:

- **One admin**, from the `SEED_ADMIN_*` environment variables.
- **Mathematics** and its 17 chapters.
- **Four marking schemes**, one per (exam pattern, question type) pair.
- **66 published questions** with LaTeX stems, options and worked solutions — covering quadratic
  equations, complex numbers, integrals, limits, matrices and determinants.
- **Two published tests**, each 25 questions over 60 minutes: one JEE Main (single correct) and
  one JEE Advanced (multiple correct with partial marking).

The sample questions were generated from known integer roots rather than typed by hand, so every
answer key agrees with its own worked solution.

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
[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md). In outline:

- **Backend** — a container built from `backend/Dockerfile`, deployable to any free-tier host that
  runs containers. Configure it entirely through environment variables.
- **Database** — any managed PostgreSQL. Flyway migrates on first boot.
- **Frontend** — `npm run build` produces static files for any static host or CDN.

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
