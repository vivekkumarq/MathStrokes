# MathStrokes

An online Mathematics examination platform for JEE aspirants.

A teacher authors mathematics questions in LaTeX and publishes timed tests; a student sits a
25-question, 60-minute paper one question at a time, with a server-authoritative countdown and
autosaved answers, then gets a scored, ranked result with per-topic analytics.

Built as a modular monolith: Angular in front, Spring Boot and PostgreSQL behind.

## Status

Under active development. See `docs/ARCHITECTURE.md` for the design decisions and
`docs/API.md` for the endpoint reference.

## Stack

| Layer      | Choice                                          |
|------------|-------------------------------------------------|
| Frontend   | Angular 22 (standalone, zoneless, signals), SCSS |
| Maths      | KaTeX                                            |
| Charts     | Chart.js                                         |
| Backend    | Java 21, Spring Boot 3.5                         |
| Data       | PostgreSQL 17, Spring Data JPA, Flyway           |
| Auth       | Spring Security, JWT access + rotating refresh   |
| Build      | Maven wrapper, Angular CLI                       |

## Quick start

Full setup instructions live further down; the short version:

```bash
# 1. database
createdb mathstrokes

# 2. backend
cd backend
cp ../.env.example ../.env      # then edit it
./mvnw spring-boot:run

# 3. frontend
cd frontend
npm ci
npm start
```

The API serves on `http://localhost:8080/api`, the app on `http://localhost:4200`.
