# POC Deployment Reference

**Project:** Corporate Learning Progress, Intervention & Compliance Tracking System  
**Document version:** 1.0  
**Date:** June 2, 2026  
**Audience:** Judges, evaluators, and team members running the live demo

This is a quick-reference guide to build, run, verify, and demo the working POC. For full POC narrative (features, limitations, team contributions), see [`Corporate_Learning_System_POC_Documentation.md`](Corporate_Learning_System_POC_Documentation.md).

---

## 1. What You Are Running

| Item | Detail |
|------|--------|
| Stack | Java 11, Spring Boot 2.7, Flyway, H2 (local) or PostgreSQL (Docker) |
| Architecture | Modular monolith — data plane, rules plane, reporting plane |
| Entry point | `backend/` — Spring Boot JAR |
| UI | Static L&D dashboard at http://localhost:8080 |
| API docs | Swagger UI at http://localhost:8080/swagger-ui.html |

---

## 2. Prerequisites

| Mode | Required software |
|------|-------------------|
| **Local (recommended for demo)** | Java 11+, Maven 3.8+ |
| **Docker** | Docker Desktop (includes Docker Compose) |

Verify Java and Maven:

```powershell
java -version
mvn -version
```

Expected: Java 11.x and Maven 3.8+.

---

## 3. Option A — Local Deployment (Fastest for Judges)

### 3.1 Build and start

**Windows (PowerShell):**

```powershell
cd backend
mvn clean package -DskipTests
java -jar target/corporate-learning-tracker-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

**Linux / macOS:**

```bash
cd backend
mvn clean package -DskipTests
java -jar target/corporate-learning-tracker-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Wait until the log shows: `Started CorporateLearningApplication`.

### 3.2 Access URLs

| URL | Purpose |
|-----|---------|
| http://localhost:8080 | L&D dashboard (main demo UI) |
| http://localhost:8080/swagger-ui.html | Interactive API documentation |
| http://localhost:8080/actuator/health | Health check (`{"status":"UP"}`) |
| http://localhost:8080/h2-console | H2 database console (local profile only) |

**H2 console login (optional):**

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:corplearning` |
| Username | `sa` |
| Password | *(leave blank)* |

> **Important:** Open the dashboard via `http://localhost:8080`, not as a local file (`file://`). The UI requires the running server for API calls.

### 3.3 Stop the application

Press `Ctrl+C` in the terminal, or on Windows:

```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
```

---

## 4. Option B — Docker Deployment (Persistent PostgreSQL)

### 4.1 Build and start

**Windows (PowerShell):**

```powershell
cd propeller-architect
copy .env.example .env
docker compose up --build
```

**Linux / macOS:**

```bash
cd propeller-architect
cp .env.example .env
docker compose up --build
```

First build may take 2–5 minutes (Maven dependency download inside Docker).

### 4.2 Services

| Service | Port | Description |
|---------|------|-------------|
| App | 8080 | Spring Boot application |
| PostgreSQL | 5432 | Persistent database (`corplearning`) |

### 4.3 Stop Docker stack

```powershell
docker compose down
```

To reset database volume: `docker compose down -v`

---

## 5. Environment Variables

Copy `.env.example` to `.env` for Docker. Local profile uses defaults unless overridden.

| Variable | Default | Description |
|----------|---------|-------------|
| `INGEST_API_KEY` | `demo-ingest-key` | Required `X-API-Key` header for `/api/v1/ingest/*` endpoints |
| `DB_HOST` | `postgres` | PostgreSQL host (Docker profile) |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `corplearning` | Database name |
| `DB_USER` | `corpuser` | Database user |
| `DB_PASSWORD` | `corppass` | Database password |

---

## 6. Seeded Demo Data (First Startup)

On first startup (empty database), `DataSeeder` automatically loads:

| Data | Count / detail |
|------|----------------|
| Employees | 5 (healthy, low-attendance, low-score, milestone-gap, boundary cases) |
| Competencies | Forklift Safety, Cybersecurity Awareness |
| Active risk rules | 5 (attendance, consecutive scores, milestone slip, composite, moderate attendance) |
| At-risk classifications | All 5 employees flagged (mix of CRITICAL / HIGH) |
| Ingestion errors | 2 unresolved (unknown employee, missing field) |
| Interventions | 4 (remedial training, coaching, in-progress, completed mentoring) |

**Note:** Local H2 is in-memory — data resets when the app stops. Docker uses persistent PostgreSQL; seed runs only once on an empty database.

---

## 7. Dashboard Demo Walkthrough (3 Minutes)

1. Open http://localhost:8080 — confirm KPI cards load (employees, at-risk, errors, rules).
2. Click **At-Risk Learners** — scrolls to at-risk queue table.
3. Click **Active Employees** — scrolls to employee profiles with attendance and scores.
4. Click **Ingestion Errors** — opens **Ingestion Error Queue** with 2 sample errors.
5. Click **Active Rules** — shows 5 active JSON-configurable rules.
6. Scroll to **Interventions** — shows 4 seeded interventions with assignees and statuses.
7. Click **Run Risk Evaluation** — re-runs batch risk engine; status bar confirms counts.
8. Click **Refresh** — reloads all dashboard data.

---

## 8. API Smoke Tests

Run these after startup to verify the backend independently of the UI.

### 8.1 Health and dashboard metrics

**PowerShell:**

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/api/v1/dashboard/lnd
Invoke-RestMethod http://localhost:8080/api/v1/risk/at-risk
Invoke-RestMethod http://localhost:8080/api/v1/dashboard/ingestion-errors
Invoke-RestMethod http://localhost:8080/api/v1/interventions
```

**curl:**

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/api/v1/dashboard/lnd
curl -s http://localhost:8080/api/v1/risk/at-risk
curl -s http://localhost:8080/api/v1/dashboard/ingestion-errors
curl -s http://localhost:8080/api/v1/interventions
```

**Expected dashboard metrics (approximate):**

| Metric | Expected |
|--------|----------|
| `activeEmployees` | 5 |
| `atRiskCount` | 5 |
| `ingestionErrors` | 2 |
| `activeRules` | 5 |

### 8.2 Run risk evaluation

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/api/v1/risk/evaluate
```

```bash
curl -X POST http://localhost:8080/api/v1/risk/evaluate
```

### 8.3 Ingest sample attendance (requires API key)

**PowerShell:**

```powershell
$headers = @{
  "Content-Type" = "application/json"
  "X-API-Key"    = "demo-ingest-key"
}
$body = '[{"employeeNumber":"EMP-ACTIVE-01","sessionId":"S-DEMO-1","courseId":"MAND-101","sessionDate":"2026-06-02","status":"PRESENT","mandatory":true}]'
Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/v1/ingest/attendance -Headers $headers -Body $body
```

**curl:**

```bash
curl -X POST http://localhost:8080/api/v1/ingest/attendance \
  -H "Content-Type: application/json" \
  -H "X-API-Key: demo-ingest-key" \
  -d '[{"employeeNumber":"EMP-ACTIVE-01","sessionId":"S-DEMO-1","courseId":"MAND-101","sessionDate":"2026-06-02","status":"PRESENT","mandatory":true}]'
```

### 8.4 Generate compliance report and export CSV

**PowerShell:**

```powershell
$report = Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/v1/reports/compliance `
  -ContentType "application/json" `
  -Body '{"periodStart":"2026-01-01","periodEnd":"2026-06-30","department":""}'
Invoke-RestMethod "http://localhost:8080/api/v1/reports/$($report.id)/export?format=csv"
```

---

## 9. API Endpoint Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/ingest/attendance` | `X-API-Key` | Ingest attendance records |
| POST | `/api/v1/ingest/assessments` | `X-API-Key` | Ingest assessment scores |
| POST | `/api/v1/ingest/milestones` | `X-API-Key` | Ingest competency milestones |
| POST | `/api/v1/ingest/employees/sync` | `X-API-Key` | Sync employee reference data |
| GET | `/api/v1/profiles` | — | List aggregated learning profiles |
| GET | `/api/v1/profiles/{id}` | — | Single employee profile |
| GET | `/api/v1/rules` | — | List risk rules |
| POST | `/api/v1/risk/evaluate` | — | Run batch risk evaluation |
| GET | `/api/v1/risk/at-risk` | — | At-risk learner queue |
| GET | `/api/v1/interventions` | — | List all interventions |
| POST | `/api/v1/interventions` | — | Assign new intervention |
| GET | `/api/v1/dashboard/lnd` | — | L&D dashboard KPI metrics |
| GET | `/api/v1/dashboard/ingestion-errors` | — | Unresolved ingestion error queue |
| POST | `/api/v1/reports/compliance` | — | Generate compliance report |
| GET | `/api/v1/reports/{id}/export?format=csv` | — | Export report as CSV |

Full request/response schemas: http://localhost:8080/swagger-ui.html

---

## 10. Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| Port 8080 already in use | Another Java process or service | Stop other process or change `server.port` in `application.yml` |
| Dashboard shows "Loading…" or errors | App not running, or opened as `file://` | Start JAR; open http://localhost:8080 |
| Maven build fails (JAR locked) | App still running | Stop Java process, then `mvn clean package` |
| Ingestion returns 401 | Missing or wrong API key | Set header `X-API-Key: demo-ingest-key` |
| Empty at-risk list | Risk not evaluated | Click **Run Risk Evaluation** or `POST /api/v1/risk/evaluate` |
| Empty interventions / errors | Old database without seed | Restart with fresh DB (local: restart app; Docker: `docker compose down -v` then up) |
| Docker build slow | First-time Maven download | Wait 2–5 min; subsequent builds are faster |
| Java version error | Java 17+ required by mistake | Use Java 11 (POC targets Java 11 / Spring Boot 2.7) |

---

## 11. Known POC Limitations

| Limitation | Notes |
|------------|-------|
| No full RBAC / SSO | Ingest APIs require API key; dashboard APIs are open (POC only) |
| H2 data not persistent | Local profile loses data on restart |
| CSV export only | PDF compliance export not implemented |
| Batch risk evaluation | No real-time streaming; manual or scheduled trigger |
| Intervention resolve UI | Outcomes via API; no "mark resolved" button on ingestion errors panel |

See [`Corporate_Learning_System_POC_Documentation.md`](Corporate_Learning_System_POC_Documentation.md) Sections 4–5 for full limitations and future improvements.

---

## 12. Repository Layout

```
propeller-architect/
├── backend/                                    # Spring Boot source and JAR build
│   ├── src/main/java/com/corplearning/common/  # Controllers, services, entities
│   ├── src/main/resources/static/index.html    # L&D dashboard UI
│   └── src/main/resources/db/migration/        # Flyway schema (V1__schema.sql)
├── docker-compose.yml                          # PostgreSQL + app
├── .env.example                                # Environment template
├── POC_DEPLOYMENT_REFERENCE.md                 # This document
├── Corporate_Learning_System_POC_Documentation.md
└── README.md                                   # Project overview
```

**Repository URL:** _[Add your Git remote URL before submission]_

---

## 13. Related Documents

| Document | Purpose |
|----------|---------|
| [`README.md`](README.md) | Project overview and quick start |
| [`Corporate_Learning_System_POC_Documentation.md`](Corporate_Learning_System_POC_Documentation.md) | Full POC deliverable (Step 5) |
| [`Corporate_Learning_System_Presentation.md`](Corporate_Learning_System_Presentation.md) | Presentation guide and demo script (Step 6) |
| [`Corporate_Learning_System_High_Level_Architecture.md`](Corporate_Learning_System_High_Level_Architecture.md) | Architecture design (Step 4) |

---

*End of deployment reference*
