# How to Run

Simple steps to run the Incident Intake Service on your machine.

## What you need

- **Java 17** or higher (`java -version`)
- **Maven 3.9+** (`mvn -version`)

## 1. Clone the repo

```bash
git clone https://github.com/verma-12/incident-intake-service.git
cd incident-intake-service
```

## 2. Start the application

```bash
mvn spring-boot:run
```

Wait until you see `Started IncidentIntakeApplication`. The API runs at:

**http://localhost:8080**

## 3. Test it works (Postman or curl)

**Create an incident** — `POST http://localhost:8080/incidents`

Headers: `Content-Type: application/json`

Body:

```json
{
  "title": "Facility outage - Building 7",
  "severity": "HIGH",
  "reportedBy": "facility.ops@example.com"
}
```

Copy the `id` from the response.

**Get that incident** — `GET http://localhost:8080/incidents/{id}`

Replace `{id}` with the UUID from the create response.

**List incidents** — `GET http://localhost:8080/incidents`

Optional query params: `?severity=HIGH` or `?status=REPORTED`

**Update status** — `PATCH http://localhost:8080/incidents/{id}/status`

```json
{
  "status": "ACKNOWLEDGED",
  "actor": "dispatcher@example.com",
  "note": "Assigned to on-call"
}
```

## 4. Run tests

```bash
mvn test
```

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `java: command not found` | Install Java 17+ and add it to PATH |
| `mvn: command not found` | Install Maven |
| Port 8080 already in use | Stop the other app, or set `server.port=8081` in `src/main/resources/application.yml` |
| Build fails first time | Run `mvn clean install` once (downloads dependencies) |

## More details

See [README.md](./README.md) for full API docs, error format, and design notes.
