# End-to-end checks

These scripts exercise the behaviours the platform is judged on, against a **running** backend and
a real PostgreSQL database. They are the checks that caught the three bugs recorded in
`docs/ARCHITECTURE.md`, and they assert outcomes a unit test cannot: that a refresh really does
resume the same paper, that another student really is refused, that a rewritten question really
does leave an old result untouched.

The unit suite (`cd backend && ./mvnw test`) needs no database and covers the scoring logic. These
cover the system.

## Running them

Start the backend against a **development** database, then:

```bash
python e2e/01_start_and_refresh.py
python e2e/02_autosave_and_access.py
python e2e/03_submit_and_score.py
python e2e/04_partial_marking_and_ranking.py
python e2e/05_historical_integrity.py
python e2e/06_expiry_and_autosubmit.py
```

Run them in order — 03 continues the attempt that 02 leaves behind.

Requirements: Python 3.8+ (standard library only) and `psql` on the path. The scripts read the
answer key straight from the database, which is how they can assert an exact expected score rather
than merely a self-consistent one.

They assume the seeded data: admin `9000000001` / `Admin@2026`, student `9812345670` /
`Student@2026`, and the two seeded 25-question tests.

> **They write data.** They register students, sit tests, and 05 deliberately rewrites and archives
> a question. Point them at a development database, never at production.

## What each one asserts

| Script | Covers |
|---|---|
| `01_start_and_refresh` | 25 questions, 60 minutes, server clock present, **no answer key or solution anywhere in a live attempt**, and that a refresh resumes the same paper in the same order with the clock still running |
| `02_autosave_and_access` | Palette states, a stale `clientSequence` rejected without clobbering newer work, clearing an answer, an option from another question refused, two selections on a single-correct question refused, and another student refused read, write and submit |
| `03_submit_and_score` | Result and review withheld while live; an exact expected score (15 correct, 5 wrong, 5 blank -> 55/100, accuracy 75.00, attempt rate 80.00); triple submit idempotency; answers frozen after submission; the key released only in review; history, dashboard and leaderboard |
| `04_partial_marking_and_ranking` | JEE Advanced partial marking arithmetic (10 exact, 5 partial, 5 wrong -> 35), and a four-student cohort producing ranks 1, 2, 3, 4 with percentiles 100.00, 75.00, 75.00, 25.00 |
| `05_historical_integrity` | A rank correctly moving from 1-of-1 to 2-of-4 as others finish, and a question whose stem is rewritten, whose answer key is flipped, and which is then archived leaving an existing result byte-identical |
| `06_expiry_and_autosubmit` | Answers refused past the deadline, `timing.expired`, the scheduled sweep finalising an abandoned attempt and scoring the saved work, time capped at the paper duration, and a late manual submit returning the result rather than a conflict |
