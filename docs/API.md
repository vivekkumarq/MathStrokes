# API reference

Base URL: `http://localhost:8080/api` (context path `/api`).

The generated OpenAPI document is at `/api/v3/api-docs` and Swagger UI at `/api/swagger-ui.html`,
both on the `dev` profile only. This file records the things a generated client will not tell you.

---

## Conventions

**Authentication.** Every route except `/auth/**`, `/actuator/health` and the docs requires
`Authorization: Bearer <accessToken>`.

**Nulls are omitted.** The application serialises with non-null inclusion, so a field that is null
is absent from the JSON rather than sent as `null`. Model optional fields as `field?: T`.

Fields that are commonly absent:

| Field | Absent when |
|---|---|
| `activeAttemptId` | the student has no attempt in flight |
| `unavailableReason` | `canStart` is true |
| `score`, `rankPosition`, `totalCandidates`, `percentile`, `correctCount`, `accuracy`, … | the attempt is not yet evaluated |
| `markingSchemeId` | the question uses the default scheme for its pattern and type |
| `fieldErrors` | the error is not a validation failure |

**Required fields.** The OpenAPI document currently marks *nothing* as required, because Java
records carry no nullability metadata. Do not infer optionality from the spec; use the table above.

**Errors.** Every failure returns the same envelope:

```json
{
  "timestamp": "2026-08-30T00:30:43.285Z",
  "status": 409,
  "error": "ATTEMPT_EXPIRED",
  "message": "Time is up for this test. Your saved answers are being submitted.",
  "path": "/api/attempts/3/answers",
  "fieldErrors": []
}
```

Branch on `error`, never on `message`.

| `error` | Status | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Bad request data; see `fieldErrors` |
| `AUTHENTICATION_FAILED` | 401 | Missing or bad credentials |
| `TOKEN_EXPIRED` | 401 | Access token expired — refresh and retry |
| `TOKEN_INVALID` | 401 | Token unusable — sign in again |
| `ACCESS_DENIED` | 403 | Authenticated, but not allowed |
| `RESOURCE_NOT_FOUND` | 404 | No such resource |
| `DUPLICATE_RESOURCE` | 409 | Conflicts with existing data |
| `BUSINESS_RULE_VIOLATION` | 409 | Rule refused the operation |
| `ATTEMPT_EXPIRED` | 409 | Answer write after the deadline |
| `ATTEMPT_ALREADY_FINALISED` | 409 | Answer write to a submitted attempt |
| `NOT_ENOUGH_QUESTIONS` | 409 | The bank cannot satisfy the blueprint |
| `RATE_LIMITED` | 429 | Too many attempts; wait and retry |
| `INTERNAL_ERROR` | 500 | Unexpected; nothing leaks |

`fieldErrors[].field` is the **JSON property name of the request body**, so a form can bind a
message straight onto the input that caused it. `rejectedValue` is withheld (null) for `password`,
`confirmPassword`, `newPassword`, `currentPassword`, `securityAnswer`, `resetToken`,
`refreshToken` and `accessToken`.

**Paging.** Paginated endpoints accept `page`, `size` and `sort`, and return:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0,
  "totalPages": 0, "first": true, "last": true }
```

---

## Authentication

| Method | Path | Notes |
|---|---|---|
| `GET` | `/auth/security-questions` | `[{ id, text }]`. Submit the **text** at registration, not the id. |
| `POST` | `/auth/student/register` | 201. Always creates a student; the role cannot be chosen. |
| `POST` | `/auth/login` | `{ phoneNumber, password }` |
| `POST` | `/auth/refresh` | `{ refreshToken }` — **rotates both tokens** |
| `POST` | `/auth/logout` | `{ refreshToken }`. Idempotent. |
| `POST` | `/auth/forgot-password/initiate` | `{ phoneNumber }` -> the security question only |
| `POST` | `/auth/forgot-password/verify` | `{ phoneNumber, securityAnswer }` -> single-use reset token |
| `POST` | `/auth/reset-password` | `{ resetToken, newPassword, confirmPassword }` |

`AuthResponse`, returned by register, login and refresh:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "user": { "id": 3, "fullName": "Ananya Sharma", "phoneNumber": "9812345670",
            "roles": ["ROLE_STUDENT"], "enabled": true, "createdAt": "2026-08-30T00:32:42Z" }
}
```

Refresh **rotates both tokens** and revokes the presented one. Replace both values wholesale; never
merge. Reusing a rotated refresh token revokes every session for that account.

Login cannot distinguish an unknown account from a wrong password — both return
`AUTHENTICATION_FAILED`. That is deliberate anti-enumeration, so render one neutral message.

`/auth/forgot-password/initiate` returns `RESOURCE_NOT_FOUND` for an unregistered number. The
client should render the same neutral wording it uses for a wrong answer; enumeration is handled by
rate limiting rather than by lying to a student who mistyped their own number.

---

## Catalogue and profile

| Method | Path | Role | Notes |
|---|---|---|---|
| `GET` | `/catalog/subjects` | any | Active subjects |
| `GET` | `/catalog/subjects/{id}/chapters` | any | Active chapters. Do not hardcode these. |
| `GET` | `/profile` | any | The signed-in account, resolved from the token |

---

## Sitting a test (student)

| Method | Path | Notes |
|---|---|---|
| `GET` | `/tests` | `?subjectId=&chapterId=&examPattern=`. No question data. |
| `POST` | `/attempts` | `{ testId }` — starts, **or resumes** an attempt in flight |
| `GET` | `/attempts/active` | The attempt in flight, or **204** if there is none |
| `GET` | `/attempts/{id}` | Reload an attempt in progress |
| `PUT` | `/attempts/{id}/answers` | Autosave |
| `PUT` | `/attempts/{id}/questions/{qid}/visited` | Record that a question was seen |
| `POST` | `/attempts/{id}/submit` | Idempotent |
| `GET` | `/attempts/{id}/result` | 409 while the attempt is live |
| `GET` | `/attempts/{id}/review` | **A bare array**, not an envelope. 409 while live. |
| `GET` | `/attempts/history` | Paginated |
| `GET` | `/me/performance` | Dashboard summary |
| `GET` | `/rankings/tests/{testId}` | Leaderboard |

### POST /attempts

Returns the **whole paper in one response**. Do not fetch per question, and repaint entirely from
this payload on reload.

```json
{
  "attemptId": 1, "testId": 1, "testTitle": "...", "subjectName": "Mathematics",
  "chapterName": "Quadratic Equations", "examPattern": "JEE_MAIN",
  "status": "ACTIVE", "totalQuestions": 25, "durationMinutes": 60,
  "timing": {
    "serverTime": "2026-08-30T00:33:11Z", "startedAt": "2026-08-30T00:33:11Z",
    "expiresAt": "2026-08-30T01:33:11Z", "remainingSeconds": 3599, "expired": false
  },
  "clientSequence": 0,
  "questions": [
    {
      "attemptQuestionId": 1, "questionOrder": 1,
      "questionType": "SINGLE_CORRECT", "difficulty": "MEDIUM",
      "questionContent": "If $\alpha$ and $\beta$ are the roots of\n\n$$3x^2 + 7x + 2 = 0$$\n\nfind the sum of the roots.",
      "options": [ { "id": 1, "optionKey": "A", "content": "$-\frac{7}{3}$", "displayOrder": 0 } ],
      "selectedOptionIds": [], "answerStatus": "NOT_VISITED",
      "markedForReview": false, "visited": false
    }
  ]
}
```

Note what is **not** there: no correctness field on any option, and no solution text. Those exist
only on the review type.

Calling this again while an attempt is live **resumes** it — same questions, same order, same
deadline. It never redraws the paper or restarts the clock.

`questionType` decides radio versus checkbox. **Not** `examPattern` — a JEE Advanced paper may
contain single-correct questions.

### PUT /attempts/{id}/answers

```json
{ "attemptQuestionId": 1, "selectedOptionIds": [3], "markedForReview": false,
  "visited": true, "clientSequence": 12 }
```

Send the **complete selection**, never a delta. An empty array clears the answer. This makes a
retry safe.

`clientSequence` is a counter you increment **once per attempt**, not per question. A write whose
sequence is older than the stored one is a late packet from a flaky connection: the server discards
it, returns `accepted: false`, and the response body carries the *newer* stored state so you can
reconcile.

```json
{
  "accepted": true, "attemptQuestionId": 1, "selectedOptionIds": [3],
  "answerStatus": "ANSWERED", "markedForReview": false, "clientSequence": 12,
  "timing": { "...": "..." },
  "palette": [ { "attemptQuestionId": 1, "questionOrder": 1, "answerStatus": "ANSWERED" } ]
}
```

The refreshed 25-entry palette and the server clock come back with every save, so the navigator and
the countdown update from the same response — no second call.

`answerStatus` is one of `NOT_VISITED`, `NOT_ANSWERED`, `ANSWERED`, `MARKED_FOR_REVIEW`,
`ANSWERED_AND_MARKED_FOR_REVIEW`.

Rejections: `ATTEMPT_EXPIRED` past the deadline, `ATTEMPT_ALREADY_FINALISED` after submission,
`ACCESS_DENIED` for another student's attempt, `BUSINESS_RULE_VIOLATION` for an option that does
not belong to the question or a second selection on a single-correct question.

### POST /attempts/{id}/submit

Idempotent. An attempt already submitted, or auto-submitted by the expiry sweep, returns its result
with 200 rather than an error — so the auto-submit race never needs a conflict screen.

### GET /attempts/{id}/review

**A bare JSON array** of 25 objects, with no wrapper. The only endpoint that returns the answer
key and the worked solution, and only for the caller's own finished attempt.

```json
[
  {
    "attemptQuestionId": 1, "questionOrder": 1, "questionType": "SINGLE_CORRECT",
    "difficulty": "MEDIUM", "questionContent": "...", "solutionContent": "...",
    "options": [ { "id": 1, "optionKey": "A", "content": "...", "displayOrder": 0,
                   "isCorrect": true, "selected": true } ],
    "selectedOptionIds": [1], "correctOptionIds": [1],
    "resultStatus": "CORRECT", "marksAwarded": 4.00, "maxMarks": 4.00
  }
]
```

This renders the paper **as it was sat**, not as the question stands today. A later edit to the
question cannot change it.

---

## Admin

All under `/admin/**` and requiring `ROLE_ADMIN`.

| Method | Path | Notes |
|---|---|---|
| `GET/POST/PUT` | `/admin/catalog/subjects` | |
| `GET/POST/PUT` | `/admin/catalog/chapters` | `PATCH /{id}/active` to deactivate |
| `GET` | `/admin/questions` | Paginated; filters for subject, chapter, pattern, difficulty, type, status, free text |
| `GET/POST/PUT` | `/admin/questions`, `/admin/questions/{id}` | |
| `POST` | `/admin/questions/{id}/publish` \| `/draft` \| `/archive` | |
| `GET` | `/admin/marking-schemes` | `POST`, `PUT /{id}`, `PATCH /{id}/active` |
| `GET/POST/PUT` | `/admin/tests`, `/admin/tests/{id}` | Only drafts are editable |
| `POST` | `/admin/tests/{id}/publish` \| `/close` \| `/archive` | Publishing a fixed-set test pins its paper |
| `GET` | `/admin/students` | `?search=`; `PATCH /{id}/enabled` |
| `GET` | `/admin/analytics/dashboard` | Headline counters |
| `GET` | `/admin/analytics/questions/hardest` | Lowest measured accuracy first |
| `GET` | `/admin/analytics/chapters` | Average score and accuracy by chapter |

Publishing a question requires exactly one correct option for `SINGLE_CORRECT`, and at least one
but not all for `MULTIPLE_CORRECT`. An active marking scheme must exist for its
(pattern, type) pair — publishing fails loudly here rather than at a student's first click.

Question content is LaTeX and plain text. Content containing `<script`, `<iframe`, `<object`,
`<embed`, `javascript:` or an inline event handler is rejected. `$a < b$` is ordinary mathematics
and is accepted.
