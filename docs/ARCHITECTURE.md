# Architecture decisions

Why the system is built the way it is. Each entry states the problem, the decision, and what the
decision costs — because the cost is usually the part that gets forgotten.

---

## 1. A modular monolith, not microservices

**Problem.** The brief calls for nine functional modules and an eventual second and third subject.

**Decision.** One deployable Spring Boot application, internally divided into modules that each own
their `controller / service / repository / entity / dto / mapper` layers. No Kafka, no Redis, no
Kubernetes.

**Why.** The parts of this system that talk to each other talk *constantly* — evaluation reads the
attempt snapshot, ranking reads evaluation output, analytics reads both. Splitting them into
services would replace ordinary method calls and one database transaction with network hops and
distributed-transaction problems, in exchange for scaling properties nobody needs at this size. The
module boundaries are the seams; if one ever genuinely needs extracting, the boundary is there.

**Cost.** Nothing stops a lazy import from reaching across a boundary. The discipline is
convention, not compiler-enforced. ArchUnit rules would fix that and are worth adding if the team
grows.

---

## 2. Attempts carry an immutable snapshot

**This is the most important decision in the system.**

**Problem.** A teacher edits a question — fixes a typo, or notices the answer key is wrong — after
fifty students have already been scored on it. What happens to those fifty results?

**Rejected: read the live question at evaluation time.** Correcting a typo would silently rescore
history, and fixing a wrong key would retroactively change who passed.

**Rejected: forbid editing a question once used.** The bank ossifies and teachers work around it by
creating near-duplicates, which is worse.

**Rejected: version questions and have attempts reference a version.** Correct, but it makes every
question row immutable-on-write and pushes a version-resolution join into every read path.

**Decision.** When an attempt starts, copy everything it will ever need onto the attempt itself.
`attempt_questions` holds the stem, the solution, the type, the difficulty and the marking
configuration; `attempt_question_options` holds each option's text **and whether it was correct**.

Evaluation reads only these tables. The live `questions` table is never consulted after the
snapshot is taken. The foreign key back to `questions` exists solely so analytics can group by
source question.

**Consequences.**

- A result computed today would come out identically if recomputed in ten years, whatever has
  happened to the question in between.
- Teachers can edit, re-key and archive freely.
- `questions.version` is bumped on every write and recorded on the snapshot, so a result can always
  be traced to the exact revision the student saw.

**Verified.** A test rewrites a question's stem, flips which option is correct, and archives it,
all after a student was scored — then asserts the student's score, counts, per-question marks, stem
and answer key are unchanged.

**Cost.** Storage: twenty-five questions with four options each is fifty extra rows per attempt. At
examination volumes this is nothing, and it buys correctness no amount of care elsewhere could.

---

## 3. The server owns the clock

**Problem.** A countdown in a browser is a suggestion. The clock can be wrong, paused by a
backgrounded tab, or edited in devtools.

**Decision.** `test_attempts.expires_at` is written once, when the attempt starts, and is never
extended. Every answer write compares against it using the *server's* clock. The browser countdown
renders a number the server supplied.

Every attempt-bearing response carries `serverTime`, `expiresAt` and a precomputed
`remainingSeconds`, so the client can measure its own offset and stay accurate between calls rather
than trusting its local clock.

**And it does not depend on the browser submitting.** A scheduled sweep finalises attempts whose
time has run out, scoring the answers already saved. A student who closes the tab, loses power or
walks away is still evaluated on their work.

Expiry is enforced *synchronously* on every write; the sweep is a safety net for cleanup, not the
mechanism that stops late answers.

**Cost.** A student on a bad connection can lose the last few seconds of work. The alternative —
accepting a late write on trust — is strictly worse in an examination.

---

## 4. Ranking compares only identical papers

**Problem.** "Rank the student" is meaningless unless it is clear who they are ranked against.

**Decision.** A test is either `FIXED_SET`, whose 25 questions are drawn once at publication so
every student sits the identical paper, or `RANDOM_PER_ATTEMPT`, which draws per attempt.
**Only `FIXED_SET` tests are ranked.** A randomly drawn paper differs per student, so placing two
such attempts on one board would compare different examinations.

**Tie policy: RANK semantics — 1, 2, 2, 4.** Ties break on score, then more correct, then fewer
incorrect, then faster completion, then earlier submission.

**Percentile** uses the NTA definition and is computed on raw score alone, so a tie-break on time
cannot move it:

```
percentile = 100 x (candidates scoring at or below you) / total
```

**One position per student** — a student's best attempt is the one placed, so sitting a paper twice
cannot occupy two positions.

**Recalculation policy.** The whole cohort is recomputed whenever an attempt is evaluated, in one
window-function pass. Freezing at close would leave the first finisher with no rank for hours. The
consequence is that a rank shown at submission is a snapshot and can move as later candidates
finish. That is not a bug; it is what the question means while the test is open.

**Cost.** O(cohort) work per submission — one indexed pass at this scale. Very large cohorts would
want a queue.

---

## 5. Marking is configuration plus a strategy, not code

**Problem.** JEE Main is +4/-1. JEE Advanced multiple-correct is +4 exact, +1 per correct option
capped at +3, and -2 for any wrong selection. Both must change without a deployment.

**Decision.** A `marking_schemes` row holds a JSONB configuration, read through a typed
`MarkingConfig` record so no other code touches raw JSON. One `EvaluationStrategy` bean per
question type, resolved from a registry built from whatever strategies exist.

The rule order in the multiple-correct strategy carries real weight: **a wrong selection is checked
before partial credit**. Reverse those and a student selecting two correct options and one wrong one
would be rewarded instead of penalised.

**Validation on write.** A configuration whose partial award could match or beat a perfect answer
is rejected, as is a positive penalty. A broken scheme can never reach an attempt snapshot.

**Cost.** JSONB is not type-checked by the database. `MarkingConfig.validate()` is the guard and
must stay thorough.

---

## 6. Money-like arithmetic uses BigDecimal

Marks are `NUMERIC(8,2)` in the database and `BigDecimal` in Java, never `double`. Partial marking
adds many small awards together, and binary floating point would make a total depend on the order
questions were scored in. Percentages use explicit scale and `HALF_UP` rounding, and every division
is guarded — a student who attempted nothing gets `0.00` accuracy, not a crash.

---

## 7. Refresh tokens are rotated and stored hashed

The access token is short-lived and stateless. The refresh token is persisted, and using it revokes
it and issues a replacement.

Presenting an already-rotated token means either a stolen credential is being replayed or the
legitimate client is retrying. Both are handled the same way: **revoke every session for that
account**. Being logged out is a survivable inconvenience; a live stolen session is not.

Refresh tokens are stored as a **SHA-256 digest, not BCrypt**. A refresh token is a signed JWT
carrying a random UUID — it already has far more entropy than any password, so BCrypt's work factor
buys nothing, and BCrypt refuses inputs over 72 bytes, which a JWT exceeds. This was found by
running the code: registration returned a 500 until it was fixed. The property that matters — a
database leak yields nothing replayable — holds either way.

---

## 8. Ownership is derived, never accepted

No endpoint takes a student id. The caller's identity comes from the security context, and every
attempt is loaded through a single method that proves ownership before returning it.

Answer writes are checked four ways: the attempt belongs to the caller, it is still `ACTIVE`, it has
not expired, and the question belongs to *that* attempt. The last check is easy to forget and is
what stops a student answering a question from somebody else's paper.

Role checks (`ROLE_ADMIN` on `/admin/**`) are a coarse first filter. Object-level ownership is
enforced in the service layer, because a role check alone would let one student read another's
attempt.

---

## 9. The answer key is structurally absent

A live attempt's option DTO has no correctness field **and no solution field**. Not filtered out —
absent from the type. The review payload uses a different type that has them.

This is deliberately stronger than filtering. A filter can be forgotten in a refactor; a field that
does not exist on the type cannot be serialised by accident. A future change that "simplifies" these
into one type with a nullable `isCorrect` would be a security regression.

---

## 10. Idempotent submission, and how it deadlocked

Submitting an already-finalised attempt returns the result rather than an error. This matters most
for the auto-submit race: the sweep finalises an attempt while the student's submit is in flight,
and the student should see their marks, not a conflict screen.

**The bug this exposed.** Ranking was originally invoked from inside the evaluation transaction with
`REQUIRES_NEW`. Ranking updates `test_attempts` — the table whose row the outer transaction had just
locked. The inner transaction waited for a lock only its own caller could release, and submission
hung until the connection pool timed out. `pg_blocking_pids` showed the ranking statement blocked by
the evaluation statement, in the same request.

**The fix.** The two now commit in sequence. `AttemptFinalisationService` closes and scores in one
transaction; ranking runs afterwards in its own. `SubmissionService` is deliberately *not*
transactional — it is an orchestrator whose whole job is to sequence those commits.

A pleasant side effect: a ranking failure now leaves a committed, correct result behind rather than
rolling back a student's marks. Ranking failures are logged and swallowed, and the next submission
on that test repairs the board.

---

## 11. Flyway owns the schema

Hibernate runs with `ddl-auto: validate` and refuses to start if the mapping and the schema
disagree. Every constraint that protects correctness lives in the database, not only in Java:

- A partial unique index allows **one active attempt per student per test**, so two concurrent
  "start test" clicks cannot both create an attempt. The service catches the resulting violation and
  returns whichever attempt won.
- A unique constraint on `question_attempt_results.attempt_question_id` makes double evaluation
  impossible even if the application logic were wrong.
- Check constraints pin every enum column to its known values.
- Partial indexes support the two hot paths — the expiry sweep and the ranking window query — while
  staying small.

**A bug this area caught.** Editing a question's options originally replaced the collection
wholesale. Hibernate orders all inserts before all deletes within a flush, so the new rows collided
with the old ones on the `(question_id, option_key)` unique index and every option edit failed.
Options are now merged in place by label, with an explicit flush after removals.

---

## 12. Known limitations

Recorded honestly rather than left to be discovered.

- **The OpenAPI document marks nothing as required.** Java records give springdoc no nullability
  information, so a generated client would make every field optional. The fix is
  `@Schema(requiredMode = REQUIRED)` across the response DTOs. Until then the omit-null rule in
  `docs/API.md` is the contract.
- **Rate limiting is per-instance and in-process.** Honest for a single-node deployment; it needs a
  shared store behind a load balancer.
- **The admin dashboard issues one count query per figure.** Fine at this size, and every count is
  indexed, but it would want caching or a summary table under real load.
- **`ORDER BY random()` drives question selection.** Correct and simple; the candidate set is one
  chapter of one exam pattern. Worth revisiting past tens of thousands of questions in one chapter.
- **No soft delete for accounts.** Students are disabled, never deleted, because results reference
  them. A genuine erasure requirement would need an anonymisation path.
- **Analytics reads are unbounded in time.** There is no date filter on question-quality figures
  yet, so they cover all history rather than a chosen window.
