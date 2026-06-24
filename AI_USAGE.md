# AI Usage

This document describes how I used an AI coding assistant (Cursor) while building this take-home. The goal was not to auto-generate a finished product, but to move faster on boilerplate and spend more time on design choices the brief left open.

## Tool

- **Cursor** — used alongside my own edits, `mvn test`, and manual curl checks

## How AI helped

### Understanding the problem

Before writing code, I used AI to talk through ambiguous parts of the brief:

- What does *“handle gracefully”* mean for a duplicate `externalReferenceId` — error vs idempotent return?
- What counts as enough history to answer *“what happened, and when?”*
- Which list filter would be most useful for an on-call responder

Those conversations helped me narrow options; the final calls below are mine.

### Project structure

AI helped scaffold the initial layout so I did not spend time on wiring:

- Maven `pom.xml` with Spring Web, JPA, Validation, H2, and test dependencies
- Package layout: `controller` → `service` → `repository` → `model`
- Starter files for entities, repository, and a basic REST controller

I then trimmed and reorganized that scaffold — see *What I changed after AI suggestions*.

### Documentation

AI drafted a first pass of `README.md` (run/test commands, curl examples, API table). I reviewed it, corrected details after running the app, and kept only what matched the actual implementation.

This file (`AI_USAGE.md`) was also started from an AI outline and rewritten to reflect what I actually did.

## What I implemented and decided myself

These were not copied from AI output — I chose them, coded them, and tested them:

| Topic | My decision |
|-------|-------------|
| **Idempotency** | Same `externalReferenceId` + same payload → `200 OK` with existing incident. Conflicting replay → `409`. Safer for upstream retries than failing every duplicate. |
| **Lifecycle** | Fixed status enum with guarded transitions (not a free-form state machine). Invalid jumps return `422 INVALID_STATUS_TRANSITION`. |
| **History** | Append-only `IncidentEvent` timeline, not just `updatedAt`. |
| **List filters** | `severity` and `status` — the two I’d actually filter on during an incident. |
| **Error shape** | Single `ErrorResponse` JSON for all failure paths; handled in a small `ApiErrorHandler`, not mixed into the controller. |
| **Simplicity** | Kept ~8 production classes in plain MVC. Merged extra DTO/exception/mapper files the first scaffold suggested. |

## What I changed after AI suggestions

The first AI pass tended toward “enterprise Spring” — many small classes for every DTO and exception. I simplified:

- One `IncidentApi` class for request/response/error POJOs instead of separate files per type
- Nested enums on entities (`Incident.Severity`, `Incident.Status`) instead of standalone enum files
- Repository `@Query` instead of a separate specifications helper
- Thin `IncidentController` (endpoints only) + package-private `ApiErrorHandler`

I rejected a few AI defaults outright:

- `409` on every duplicate external ID → replaced with idempotent `200` for identical retries
- Generic `500` for bad status transitions → replaced with `422` and a clear error code
- PostgreSQL required for local run → H2 in-memory for zero-setup `mvn spring-boot:run`

## Example prompts I used

These are representative, not a full transcript:

1. *“Break down this take-home requirement — what’s ambiguous and what needs an explicit design decision?”*
2. *“Draft README run/test instructions and curl examples for these endpoints.”*
3. *“Same externalReferenceId submitted twice with identical payload — what’s the REST-friendly behavior?”*

After each answer I read the code, ran tests, and adjusted.

## How I verified correctness

```bash
cd incident-intake-service
mvn test
```

I focused tests where mistakes would hurt most:

1. Idempotency — same vs conflicting `externalReferenceId`
2. Lifecycle rules — e.g. `REPORTED → RESOLVED` must fail
3. Error JSON shape — validation, not-found, conflict
4. End-to-end — create → transition → get with timeline

Manual smoke test:

```bash
mvn spring-boot:run

curl -X POST http://localhost:8080/incidents -H 'Content-Type: application/json' \
  -d '{"title":"Test","severity":"LOW","reportedBy":"me@example.com","externalReferenceId":"T-1"}'
```

## If this went to production

Things I would add next (not in scope for the exercise):

- PostgreSQL + Flyway migrations
- Auth on `reportedBy` / `actor` (today caller-supplied strings)
- Race-safe idempotency (unique constraint + transactional handling under concurrent POSTs)
- Pagination on list, API versioning, OpenTelemetry metrics/traces

## Summary

AI was useful for **structure**, **documentation drafts**, and **thinking through open requirements**. The domain model, API behavior, simplification of the codebase, test choices, and final code review were done by me. I treated AI output as a starting point — not something to merge blindly.
