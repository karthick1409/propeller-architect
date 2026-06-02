# Corporate Learning System — POC Documentation

**Document version:** 1.0  
**Date:** June 2, 2026  
**Purpose:** Fifth deliverable per evaluation rubric — POC (20 marks)

---

## 1. Working Prototype Summary

The POC is a **Spring Boot modular monolith** that demonstrates all must-have features from the problem statement:

| Feature | POC evidence |
|---------|--------------|
| API ingestion (attendance, assessments, milestones) | `IngestionController` + `X-API-Key` auth |
| Employee learning profile aggregation | `ProfileService` + `GET /api/v1/profiles` |
| Configurable JSON risk rules (5 seeded) | `RiskRuleService` + `GET /api/v1/rules` |
| At-risk learner classification | `RiskEvaluationService` + `GET /api/v1/risk/at-risk` |
| Intervention tracking | `InterventionController` CRUD + outcomes |
| Compliance reporting | `POST /api/v1/reports/compliance` + CSV export |
| L&D dashboard | `static/index.html` + `GET /api/v1/dashboard/lnd` |

**Automated tests:** 3 unit/integration tests including rules golden-file style checks (`RuleEvaluatorTest`, `RiskIntegrationTest`).

---

## 2. Code Repository

| Item | Location |
|------|----------|
| Backend source | `backend/src/main/java/com/corplearning/` |
| DB migrations | `backend/src/main/resources/db/migration/V1__schema.sql` |
| Dashboard UI | `backend/src/main/resources/static/index.html` |
| Docker | `docker-compose.yml`, `backend/Dockerfile` |
| README | `README.md` |

**Repository URL:** _[Add your GitHub/GitLab URL after push]_

---

## 3. Deployment Instructions

### 3.1 Local (Windows / Linux)

```bash
# Prerequisites: Java 11+, Maven 3.8+
cd backend
mvn test package
java -jar target/corporate-learning-tracker-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Verify:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/risk/at-risk
```

Open http://localhost:8080 for the dashboard.

### 3.2 Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

| Service | Port |
|---------|------|
| App | 8080 |
| PostgreSQL | 5432 |

### 3.3 Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `INGEST_API_KEY` | `demo-ingest-key` | Required header for ingestion APIs |
| `DB_HOST` | `postgres` | PostgreSQL host (docker profile) |
| `DB_USER` / `DB_PASSWORD` | `corpuser` / `corppass` | Database credentials |

### 3.4 Demo data

On first startup, `DataSeeder` creates:

- 5 employees (including low-attendance, low-score, milestone-gap scenarios)
- Competency catalog (Forklift Safety, Cybersecurity)
- 5 active risk rules (R-ATT-01, R-SCR-01, R-MS-01, R-CMP-01, R-ATT-02)
- Initial risk evaluation run

### 3.5 Troubleshooting

| Issue | Fix |
|-------|-----|
| Port 8080 in use | Stop other Java processes or change `server.port` |
| Maven Java 17 error | Use Java 11 (POC targets Java 11 / Spring Boot 2.7) |
| Empty at-risk list | Click **Run Risk Evaluation** on dashboard or `POST /api/v1/risk/evaluate` |
| Ingestion 401 | Set header `X-API-Key: demo-ingest-key` |
| Docker build slow | First build downloads Maven dependencies (~2–5 min) |

---

## 4. Known Issues and Limitations

| # | Issue | Impact | Workaround |
|---|-------|--------|------------|
| 1 | H2 in-memory DB in local profile | Data lost on restart | Use Docker/PostgreSQL for persistence |
| 2 | No full RBAC / SSO | Security is POC-level | API key on ingest only; human APIs open |
| 3 | PDF export not implemented | CSV only | Use CSV export for compliance demo |
| 4 | Single-node deployment | No HA | Acceptable for POC |
| 5 | Batch risk only (no real-time stream) | Near-real-time not demonstrated | Manual `POST /risk/evaluate` or scheduled job (future) |
| 6 | Trainer cohort scoping simplified | All trainers see all interventions | Future: cohort filter by assignment |
| 7 | Architecture doc specifies Java 17 / Boot 3 | POC uses Java 11 / Boot 2.7 for environment compatibility | Upgrade path documented in HLD |
| 8 | Duplicate attendance on re-ingest same key | Unique constraint may error | Idempotent replay to be hardened |

---

## 5. Future Improvements

| Priority | Improvement |
|----------|-------------|
| High | Full RBAC (L&D, Trainer, Compliance) per use case matrix |
| High | PostgreSQL as default local profile; Testcontainers in CI |
| High | Rule simulation UI (dry-run screen for L&D) |
| Medium | Email/Teams notifications on new at-risk flags |
| Medium | PDF compliance report with audit watermark |
| Medium | Trend-based rules (declining scores over time) |
| Medium | Upgrade to Spring Boot 3 / Java 17 per HLD |
| Low | React SPA replacing static dashboard |
| Low | ML-assisted risk layered on rule engine |

---

## 6. Team Contributions

_Update names and roles before presentation._

| Team member | Role | Contributions | Artifacts |
|-------------|------|---------------|-----------|
| _[Name 1]_ | Software Architect | Problem analysis, use cases, test plan, HLD, rules format, POC scope | `Corporate_Learning_System_*.md`, architecture diagrams |
| _[Name 2]_ | Backend Developer | Spring Boot APIs, rules engine, profile aggregation | `backend/` |
| _[Name 3]_ | Frontend / Full-stack | Dashboard UI, API integration | `static/index.html` |
| _[Name 4]_ | QA | Test cases, smoke scripts, integration tests | `Corporate_Learning_System_Test_Case_Documentation.md`, `*Test.java` |
| _[Name 5]_ | DevOps | Docker Compose, deployment docs | `docker-compose.yml`, `README.md` |

### Architect highlights (presentation talking points)

- Clean **three-plane separation**: data / rules / reporting  
- **JSON rule format** with attendance, score, milestone, and composite rules  
- **Competency progression** via milestone history + role requirements  
- Traceability from problem statement → use cases → test cases → architecture → POC  

---

## 7. Demo Script (3 minutes)

1. **Dashboard** — Show at-risk counts, employee profiles, active rules (http://localhost:8080).  
2. **At-risk queue** — Highlight employees flagged for attendance, scores, milestone gap.  
3. **Profile drill-down** — `GET /api/v1/profiles/2` — unified attendance + competency view.  
4. **Intervention** — Assign remedial session via API or future UI; record outcome.  
5. **Compliance** — Generate report + CSV export.  
6. **Ingestion** — POST sample attendance with API key (optional live demo).

---

## 8. Rubric Self-Check (POC — 20 marks)

| Criterion | Max | Evidence |
|-----------|-----|----------|
| Working prototype | 4 | Runnable app; all must-have features |
| Code repository and documentation | 4 | `backend/`, README, this document |
| Deployment instructions | 3 | Section 3; README quick start |
| Known issues and improvements | 3 | Sections 4–5 |
| Team contribution and presentation | 3 | Section 6–7 (complete before judging) |
| Overall innovation and quality | 3 | JSON rules engine, three-plane architecture, seeded realistic scenarios |

---

## 9. Next Step

**Presentation (Step 6)** — Problem statement, team roles, solution/architecture overview, live POC demo, future scope, Q&A.

---

*End of document*
