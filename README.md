# Incident Intake Service

Spring Boot backend for field incident reporting and lifecycle tracking.

> **Quick start:** see [HOW_TO_RUN.md](./HOW_TO_RUN.md) for simple clone → run → test steps.

## Quick run

```bash
git clone https://github.com/verma-12/incident-intake-service.git
cd incident-intake-service
mvn spring-boot:run
```

Open **http://localhost:8080** — then use Postman or curl (examples below).

```bash
mvn test          # run all tests
```

## Prerequisites

- Java 17+
- Maven 3.9+

## Run locally

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

## Test

```bash
mvn test
```

## API overview

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/incidents` | Intake a new incident |
| `GET` | `/incidents/{id}` | Get one incident with full timeline |
| `GET` | `/incidents` | List incidents (filter by `severity` and/or `status`) |
| `PATCH` | `/incidents/{id}/status` | Move an incident through its lifecycle |

### Create incident

```bash
curl -s -X POST http://localhost:8080/incidents \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Facility outage - Building 7",
    "severity": "HIGH",
    "reportedBy": "facility.ops@example.com",
    "externalReferenceId": "FAC-2026-001",
    "description": "Main breaker tripped"
  }'
```

Required fields: `title`, `severity` (`LOW` | `MEDIUM` | `HIGH` | `CRITICAL`), `reportedBy`.

Optional: `externalReferenceId`, `description`.

### External reference idempotency

If `externalReferenceId` is provided:

- **Same reference + same core payload** (`title`, `severity`, `reportedBy`) → `200 OK` with the existing incident (safe retry for upstream systems).
- **Same reference + different payload** → `409 Conflict` with code `DUPLICATE_EXTERNAL_REFERENCE_CONFLICT`.

This avoids duplicate incidents from network retries while still surfacing conflicting replays.

### Lifecycle transition

```bash
curl -s -X PATCH http://localhost:8080/incidents/{id}/status \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "ACKNOWLEDGED",
    "actor": "dispatcher@example.com",
    "note": "Assigned to on-call"
  }'
```

Allowed transitions:

```
REPORTED → ACKNOWLEDGED | CLOSED
ACKNOWLEDGED → IN_PROGRESS | CLOSED
IN_PROGRESS → RESOLVED | CLOSED
RESOLVED → CLOSED
```

Each change appends to the incident **timeline** so you can answer: *what happened, and when?*

### List with filters

```bash
curl -s 'http://localhost:8080/incidents?severity=HIGH&status=REPORTED'
```

### Error format

All failures return a consistent JSON body:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "timestamp": "2026-06-24T10:15:30Z",
  "path": "/incidents",
  "fieldErrors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

## Design notes

- **Layout**: classic MVC — `controller` → `service` → `repository` → `model`
- **POJOs**: `IncidentApi` holds request/response/error types; entities live in `Incident` / `IncidentEvent`
- **Errors**: `ApiErrorHandler` (small `@RestControllerAdvice`) — keeps the controller to endpoints only; required for the spec's consistent error JSON
- **Persistence**: in-memory H2 for local dev; swap datasource config for PostgreSQL in production.
- **Timeline**: `IncidentEvent` records creation, status changes, and notes with actor and timestamp.
- **Logging**: JSON logs via Logstash encoder with MDC fields (`incidentId`, `operation`, `severity`, `status`) for log aggregation.

See [AI_USAGE.md](./AI_USAGE.md) for how AI was used (structure, docs, requirement clarification) and what was decided independently.
