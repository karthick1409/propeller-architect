# Corporate Learning Progress, Intervention & Compliance Tracking System

Tehnothon Propeller Architect — working POC for employee learning risk detection, intervention tracking, and compliance reporting.

## Repository structure

```
propeller-architect/
├── backend/                          # Spring Boot modular monolith (Java 11)
├── docker-compose.yml                # PostgreSQL + app
├── .env.example                      # Environment template
├── Corporate_Learning_System_*.md    # Architect deliverables (analysis → HLD)
├── Corporate_Learning_System_POC_Documentation.md
└── ARCHITECT_PROMPTS_LOG.md
```

## Quick start (local — no Docker)

**Prerequisites:** Java 11+, Maven 3.8+

```powershell
cd backend
mvn test package
java -jar target/corporate-learning-tracker-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Open:

| URL | Purpose |
|-----|---------|
| http://localhost:8080 | L&D dashboard (POC UI) — use the **Demo role** bar to switch to **Compliance Officer** (no login) |
| http://localhost:8080/swagger-ui.html | API documentation |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/h2-console | H2 DB console (JDBC: `jdbc:h2:mem:corplearning`) |

Demo data (5 employees, 5 active risk rules, sample at-risk classifications) loads automatically on first startup.

### External configuration

By default the app also loads settings from **`C:/Users/Administrator/propeller-architect-main`** (override with env var `CORP_CONFIG_HOME`):

| Location | Purpose |
|----------|---------|
| `{CORP_CONFIG_HOME}/config/` | Local overrides (highest priority) |
| `{CORP_CONFIG_HOME}/backend/src/main/resources/` | Shared `application.yml`, `application-local.yml`, etc. |

```powershell
$env:CORP_CONFIG_HOME = "C:/Users/Administrator/propeller-architect-main"
java -jar target/corporate-learning-tracker-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

## Quick start (Docker)

**Prerequisites:** Docker Desktop

```powershell
cd propeller-architect
copy .env.example .env
docker compose up --build
```

App: http://localhost:8080

## API overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/ingest/attendance` | Ingest attendance (header `X-API-Key: demo-ingest-key`) |
| POST | `/api/v1/ingest/assessments` | Ingest assessment scores |
| POST | `/api/v1/ingest/milestones` | Ingest competency milestones |
| POST | `/api/v1/ingest/employees/sync` | Sync employee reference data |
| GET | `/api/v1/profiles` | List aggregated learning profiles |
| GET | `/api/v1/rules` | List risk rules |
| POST | `/api/v1/risk/evaluate` | Run risk engine batch |
| GET | `/api/v1/risk/at-risk` | At-risk learner queue |
| POST | `/api/v1/interventions` | Assign intervention |
| GET | `/api/v1/dashboard/lnd` | L&D dashboard metrics |
| POST | `/api/v1/reports/corporate` | Generate corporate compliance report |
| POST | `/api/v1/reports/compliance` | Alias for corporate report |
| GET | `/api/v1/reports` | List generated reports |
| GET | `/api/v1/reports/{id}` | Report summary with traceability metadata |
| GET | `/api/v1/reports/{id}/export?format=csv` | Export report CSV (audit-logged) |
| GET | `/api/v1/reports/{id}/export?format=pdf` | Export report PDF (Compliance Officer only) |
| GET | `/api/v1/audit/risk-decisions` | Risk decision audit trail |
| GET | `/api/v1/auth/me` | Current role and permissions (send header `X-User-Role`) |

**RBAC (POC):** Send `X-User-Role` header: `LND_ADMIN`, `COMPLIANCE_OFFICER`, `TRAINER`, `EMPLOYEE`, `LINE_MANAGER`, `INTEGRATOR`. Dashboard role selector sets this automatically. Report generate/export requires `COMPLIANCE_OFFICER`; rule configuration requires `LND_ADMIN`.

## Smoke test (curl)

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/dashboard/lnd
curl http://localhost:8080/api/v1/risk/at-risk
curl -X POST http://localhost:8080/api/v1/risk/evaluate
```

Ingest example:

```bash
curl -X POST http://localhost:8080/api/v1/ingest/attendance \
  -H "Content-Type: application/json" \
  -H "X-API-Key: demo-ingest-key" \
  -d '[{"employeeNumber":"EMP-ACTIVE-01","sessionId":"S-NEW-1","courseId":"MAND-101","sessionDate":"2026-06-02","status":"PRESENT","mandatory":true}]'
```

## Architecture alignment

The POC implements the [High Level Architecture](Corporate_Learning_System_High_Level_Architecture.md) modular monolith:

- **Data plane:** ingestion, validation, employee learning profiles  
- **Rules plane:** JSON-configurable rules, batch evaluator, at-risk classification  
- **Reporting plane:** L&D dashboard, compliance report + CSV export  

## Documentation index

| Step | Document |
|------|----------|
| 1 | `Corporate_Learning_System_Problem_Statement_Analysis.md` |
| 2 | `Corporate_Learning_System_Use_Case_Documentation.md` |
| 3 | `Corporate_Learning_System_Test_Case_Documentation.md` |
| 4 | `Corporate_Learning_System_High_Level_Architecture.md` |
| 5 | `Corporate_Learning_System_POC_Documentation.md` |
| — | `POC_DEPLOYMENT_REFERENCE.md` (judges: build, run, verify, demo) |
| 6 | `Corporate_Learning_System_Presentation.md` |

## Code repository

Local path: `propeller-architect/`  
Push to your team Git remote and submit that URL for evaluation.

## Team

Update the contribution tables in `Corporate_Learning_System_POC_Documentation.md` and `Corporate_Learning_System_Presentation.md` with your team member names before presenting.
