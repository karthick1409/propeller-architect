# Corporate Learning System — Architect Prompts Log

**Project:** Tehnothon — Corporate Learning Progress, Intervention & Compliance Tracking System  
**Role:** Software Architect  
**Purpose:** Record every prompt used with AI assistants (Cursor, etc.) and provide reusable prompt templates aligned with judge criteria and deliverables.

---

## Context references

| Document | Path | Notes |
|----------|------|--------|
| Problem statement | `Corporate_Learning_System_problem_statement.md` | Corporate LMS scope; API ingestion; rules; interventions; compliance |
| Architecture review process | `Corporate_Learning_Architecture_Review_Process.md` | Post-implementation review; 10 architect roles; scoring rubrics (school template — adapt to corporate/L&D) |
| Implementation guide | `Corporate_learning_System_Implementation_Guide.md` | Phased build; data model; rules JSON; dashboards (school template — adapt terminology) |
| Judge evaluation | `Corporate_Learning_Judge_evaluation.ods` | Referenced; not in workspace at log creation — criteria mirrored in `Expectations.txt` |
| Expectations / judge criteria | `Expectations.txt` | Presentation scoring rubric (max marks per criterion) |
| Architect capabilities | `SOFTWARE ARCHITECT CAPABILITIES AND ROLES.txt` | Role expectations for architect |

**Domain adaptation note:** Review and implementation guides use school/student language. All downstream prompts should map: *student → employee*, *teacher → trainer/L&D*, *counselor → HR/learning coach*, *FERPA → GDPR/enterprise PII*, *grade → competency level*, *LMS fragmentation → corporate learning sources*.

---

## How to use this log

1. **Append** each new user/assistant session prompt under [Session prompts (chronological)](#session-prompts-chronological) with the next `PROMPT-###` id, date, tool, and outcome/deliverable.
2. **Copy** templates from [Reusable architect prompt library](#reusable-architect-prompt-library) into Cursor or your team channel; replace `{placeholders}`.
3. **Trace** deliverables to judge rows in `Expectations.txt` using the criterion tags in each template.

---

## Session prompts (chronological)

### PROMPT-001 — 2026-06-02 — Context setup (Cursor)

**Source:** User (Architect)  
**Tool:** Cursor Agent  
**Intent:** Establish Tehnothon context; create this prompts log file.

**Full prompt:**

```
I am participating in Tehnothon challenge and my role is Architect. Here is the detailed description of the problem statement , The end goal is to implement working  Corporate Learning system. @Corporate_Learning_System_problem_statement.md  this file contains problem statement, @Corporate_Learning_Architecture_Review_Process.md  this file contains Architecture Review process, @Corporate_learning_System_Implementation_Guide.md this contains System Implementation guide, @Corporate_Learning_Judge_evaluation.ods this file contains Judge Evaluation. @Expectations.txt this file contains the expected output.(This is to set the context of problem statement)  please create a file and record all the prompts
```

**Outcome:** Created `ARCHITECT_PROMPTS_LOG.md` with PROMPT-001 recorded and reusable architect prompt library below.

---

## Reusable architect prompt library

Use these as copy-paste prompts. Tag format: `[JUDGE: <section> — <criterion>]`.

---

### A. Problem statement analysis (10 marks total)

#### PROMPT-TPL-A1 — Problem clarity and scope boundaries

```
[JUDGE: Problem Statement Analysis — Clarity / Expectation and scope / Out of scope]

Read Corporate_Learning_System_problem_statement.md and produce an architect-facing analysis:

1. Problem statement in 3–5 sentences (accountability, fragmented data, late detection, reactive interventions).
2. In-scope vs out-of-scope (explicitly: NOT a full corporate LMS).
3. Must-have features mapped to business capabilities.
4. Evaluation parameters from the problem statement and how architecture will satisfy each.
5. Future scope (ML risk prediction, deeper LMS integrations) — clearly labeled out of MVP.

Output: markdown section suitable for architecture deck slide 1–2.
```

#### PROMPT-TPL-A2 — Depth of analysis and documentation quality

```
[JUDGE: Problem Statement Analysis — Depth / Documentation quality]

As Principal Architect for a Corporate Learning Progress, Intervention & Compliance system:

- Stakeholders: L&D admin, trainers, compliance officer, HR, employee (learner), integration owner.
- Pain points with current fragmented LMS + assessments + trainer notes.
- Success metrics: early at-risk detection, intervention effectiveness, compliance report accuracy.
- Risks and assumptions (data quality, API availability, rule false positives).

Deliver a 2-page executive summary with a RACI for architect deliverables vs implementation team.
```

---

### B. Use case documentation (10 marks total)

#### PROMPT-TPL-B1 — Actors, flows, restrictions

```
[JUDGE: Use Case Documentation — User flows and actors / Restrictions and rights]

Define use cases for the Corporate Learning system (not a full LMS):

Actors: Employee, Trainer, L&D Administrator, Compliance Officer, System Integrator, API (external).

For each use case provide: ID, name, primary actor, preconditions, main flow, alternate flows, postconditions.

Must cover: API ingestion (attendance, assessments, milestones), profile aggregation, configurable risk rules, at-risk classification, intervention assign/track, compliance reporting, dashboard views.

Include a permissions matrix: which actor can view PII, configure rules, approve interventions, export compliance reports.
```

#### PROMPT-TPL-B2 — Sequence diagrams and completeness

```
[JUDGE: Use Case Documentation — Sequence diagrams / Completeness]

Create Mermaid sequence diagrams for:

1. Daily batch: ingest attendance + assessment + milestone APIs → validate → normalize → update employee learning profile.
2. Risk engine run: load profile → evaluate rule set (JSON/YAML) → persist risk assessment → notify L&D.
3. Intervention lifecycle: recommend → approve → execute remedial session/coaching → capture outcomes → measure effectiveness.

List any missing use cases and add them. Ensure documentation is professional (consistent naming, version header).
```

---

### C. Test case documentation (10 marks total)

#### PROMPT-TPL-C1 — Test plan and traceability

```
[JUDGE: Test Case Documentation — Test plan / Traceability matrix]

Given the must-have features in Corporate_Learning_System_problem_statement.md:

1. Write a test plan (unit, integration, API contract, rules engine, reporting).
2. Minimum 25 test cases with ID, description, preconditions, steps, expected result, priority.
3. Build a requirements traceability matrix: Feature → Use Case → Test Case ID.

Include edge cases: competency-level progression, boundary thresholds (attendance %, score), inactive employees, duplicate API records, rule version change mid-quarter.
```

#### PROMPT-TPL-C2 — Functional requirements and use-case tests

```
[JUDGE: Test Case Documentation — Use case related tests / Functional requirements]

Map test cases to use cases UC-INGEST-*, UC-PROFILE-*, UC-RISK-*, UC-INTERVENTION-*, UC-REPORT-*.

For the rules engine, define:
- Positive tests (rule fires correctly)
- Negative tests (should not fire)
- Performance test: 1000 employee profiles in <2 minutes

Output in markdown tables suitable for QA handoff.
```

---

### D. High-level architecture (10 marks total)

#### PROMPT-TPL-D1 — Tech stack and rationale

```
[JUDGE: High Level Architecture — Tech stack / Alternatives and rationale]

Propose a high-level architecture for the Corporate Learning Progress system:

Constraints: API ingestion, rule-based risk (configurable JSON/YAML), intervention tracking, compliance reports, simple dashboards; clean separation of data, rules, reporting.

Deliver:
1. C4 Context + Container diagram (Mermaid or PlantUML).
2. Recommended stack (backend, DB, cache, queue, frontend) with justification.
3. Two alternatives compared (e.g., monolith vs modular services; PostgreSQL vs other).
4. Non-functional requirements: security, scalability, observability.

Align with evaluation: realism of competency rules, competency progression, trainer/L&D usefulness, separation of concerns.
```

#### PROMPT-TPL-D2 — Dev, test, deploy, CI/CD

```
[JUDGE: High Level Architecture — Dev/Test/Deploy / CI/CD/CT]

Document:

1. Environments: dev, test, staging, prod.
2. CI/CD pipeline stages (build, test, security scan, deploy).
3. Continuous testing strategy (contract tests for ingestion APIs, rules golden files).
4. Deployment: containers, health checks, rollback.
5. Configuration management for risk rules (versioned, audited).

Include an architecture diagram for deployment on cloud or on-prem (team choice).
```

---

### E. Data architecture (Architect Role 2 / Technical 30%)

#### PROMPT-TPL-E1 — Corporate data model

```
[ARCHITECT ROLE: Data Architect — Evidence: ERD, data dictionary]

Design the corporate learning data model (replace school/student schema from implementation guide):

Entities: Employee, Department, TrainingAttendance, AssessmentScore, CompetencyMilestone, EmployeeLearningProfile (aggregated), RiskRule, RiskAssessment, Intervention, InterventionOutcome, ComplianceReportRun, AuditLog.

Deliver:
- ERD (Mermaid)
- PostgreSQL DDL sketch
- Data dictionary
- How competency-level progression is stored and queried
- Historical retention and lineage from source APIs
```

#### PROMPT-TPL-E2 — Data flow and integration

```
[ARCHITECT ROLE: Integration Architect]

Document data flow from:
[LMS/HRIS Attendance API] → Ingestion → Validation → Profile Aggregation → Risk Engine → Dashboards/Reports → Intervention Module.

Specify REST API contracts (OpenAPI outline) for the three ingestion types in the problem statement. Include error handling, idempotency, retry, reconciliation.
```

---

### F. Rules engine architecture (Architect Role 3)

#### PROMPT-TPL-F1 — Rule definition format

```
[ARCHITECT ROLE: Rules Engine Architect — Rule definition format + 15+ sample rules]

Define the corporate learning risk rule schema (JSON or YAML) supporting:
- Attendance % thresholds over configurable periods
- Assessment score thresholds and consecutive failures
- Competency milestone delays
- Composite AND/OR rules
- Severity, alerts (roles), recommended intervention types
- Versioning and audit trail

Provide 15+ sample rules realistic for workplace compliance training (safety, cybersecurity, leadership competencies).

Include rule validation approach and a dry-run/testing framework description.
```

#### PROMPT-TPL-F2 — Rule engine execution design

```
[ARCHITECT ROLE: Rules Engine Architect — Performance]

Design rule execution:
- Batch vs event-driven
- Priority and short-circuit evaluation
- Target: 1000 employees in <2 minutes
- Separation: rules stored in DB, engine stateless, reporting reads materialized risk snapshots

Document how L&D can change rules without code deploy (admin UI or config API).
```

---

### G. Security, privacy, performance (Roles 5–6)

#### PROMPT-TPL-G1 — Security and access control

```
[ARCHITECT ROLE: Privacy & Security Architect]

For corporate employee learning data (PII, performance):

- RBAC matrix (Employee self-view, Trainer, L&D Admin, Compliance, Integrator)
- Authentication approach (SSO/OAuth2)
- Encryption in transit/at rest
- Audit logging for compliance exports and rule changes
- GDPR/enterprise privacy checklist (adapt FERPA concepts from review doc)

Deliver security architecture summary + access control matrix.
```

#### PROMPT-TPL-G2 — Performance and scalability

```
[ARCHITECT ROLE: Performance & Scalability Architect]

Set targets from architecture review doc (adapted):
- API P95 <200ms
- Profile aggregation <500ms per employee
- Dashboard load <2s
- Report generation <30s

Provide load test scenarios, caching strategy (Redis), DB indexing plan, horizontal scaling notes for multi-department enterprises.
```

---

### H. Intervention workflow and reporting (Roles 7–8)

#### PROMPT-TPL-H1 — Intervention workflow

```
[ARCHITECT ROLE: Workflow & Automation Architect]

Design workflows for:
- Remedial training sessions
- Coaching/mentoring assignments

Include: states (Planned, Active, Completed), approvals, notifications, outcome metrics (pre/post attendance and scores), effectiveness measurement.

Mermaid state machine + notification list (email/in-app).
```

#### PROMPT-TPL-H2 — Compliance reporting

```
[ARCHITECT ROLE: Reporting & Compliance Architect]

Define compliance-ready reports for regulatory/industry standards:
- At-risk employee list
- Training attendance compliance
- Competency achievement summary
- Intervention effectiveness

Report template approach, audit trail, scheduled generation, export formats (PDF/CSV). Validate fields against problem statement scope.
```

---

### I. UX and technical leadership (Roles 9–10)

#### PROMPT-TPL-I1 — Dashboards by role

```
[ARCHITECT ROLE: User Experience Architect]

Design wireframe-level specs (markdown + ASCII or Mermaid) for:
- L&D Administrator dashboard (org-wide risk, interventions)
- Trainer dashboard (cohort attendance, scores)
- Compliance Officer dashboard (reports, deadlines)

Requirements: simple, drill-down, mobile-responsive, WCAG 2.1 AA considerations.
```

#### PROMPT-TPL-I2 — Documentation and team enablement

```
[ARCHITECT ROLE: Technical Leader & Educator]

Create an architecture documentation index:
- ADR template and first 5 ADRs (stack, rules engine, ingestion, security, deployment)
- README for developers
- Runbook for operations
- Training outline for L&D admins (configuring rules, reading dashboards)

Ensure implementation team can maintain the system independently.
```

---

### J. POC and implementation (20 marks POC + build)

#### PROMPT-TPL-J1 — POC scope and repository structure

```
[JUDGE: POC — Working prototype / Code repository]

Scaffold a minimal working Corporate Learning POC:

Modules: ingestion API stubs, employee profile aggregation, 5+ working risk rules, at-risk list API, 1 intervention CRUD flow, 1 compliance report (JSON/PDF), simple dashboard page.

Monorepo structure, README, docker-compose for local run. List known issues and v2 improvements.
```

#### PROMPT-TPL-J2 — Deployment instructions

```
[JUDGE: POC — Deployment instructions]

Write step-by-step deployment:
- Prerequisites
- Environment variables
- Database migrate/seed sample data
- Start services
- Smoke test commands (curl examples)
- Troubleshooting

Must be reproducible on Windows and Linux.
```

#### PROMPT-TPL-J3 — Team contribution slide content

```
[JUDGE: POC — Team contribution / Presentation]

Generate a table: Team Member | Role | Contributions | Artifacts produced.

Highlight Architect contributions: HLD, data model, rules format, integration contracts, security matrix, review of implementation against architecture.
```

---

### K. Presentation and Q&A (10 marks)

#### PROMPT-TPL-K1 — Presentation narrative

```
[JUDGE: Presentation — All criteria]

Build a 15-slide presentation outline:

1. Problem and scope (2 min)
2. Team roles and work distribution (1 min)
3. Solution overview and HLD (3 min)
4. Data + rules + integration deep dive (3 min)
5. POC demo script (3 min)
6. Future scope and innovation (2 min)
7. Anticipated Q&A with answers (compliance, false positives, scalability, why not full LMS)

Tone: confident, judge-aligned with Expectations.txt rubric.
```

#### PROMPT-TPL-K2 — Architecture review dry run

```
[JUDGE: Presentation — Q&A / Architecture Review Process]

Simulate the Architecture Review Panel from Corporate_Learning_Architecture_Review_Process.md (adapt questions to corporate learning).

For each of the 10 architect roles, generate 3 tough questions and model answers based on our documented architecture.

Identify gaps scoring below 4/5 and remediation actions.
```

---

### L. Cross-cutting prompts

#### PROMPT-TPL-L1 — Adapt school docs to corporate domain

```
The implementation guide and architecture review use school/student terminology. Produce a mapping document:

| School term | Corporate term |
|-------------|----------------|
| Student | Employee |
| ... | ... |

Then rewrite the executive summary of both docs in corporate language while preserving technical structure (phases, rubrics, KPIs).
```

#### PROMPT-TPL-L2 — Judge rubric self-assessment

```
Using Expectations.txt, score our current artifacts 0–2 per row with evidence links (file paths) and judge-style suggestions for gaps.

Format as a table: Step | Criteria | Max | Self Score | Evidence | Improvement Action.
```

#### PROMPT-TPL-L3 — Incremental prompt append

```
Append the following to ARCHITECT_PROMPTS_LOG.md as PROMPT-{next_id}:

[Paste your new prompt here]

Include: date, tool, intent, full prompt text, outcome/deliverable paths.
```

---

## Judge criteria quick reference (from Expectations.txt)

| Step | Criteria | Max Marks |
|------|----------|-----------|
| Problem Statement Analysis | Clarity of problem statement | 2 |
| Problem Statement Analysis | Expectation and scope | 2 |
| Problem Statement Analysis | Out of scope and future scope | 2 |
| Problem Statement Analysis | Depth of analysis | 2 |
| Problem Statement Analysis | Documentation quality | 2 |
| Use Case Documentation | User flows and actors | 2 |
| Use Case Documentation | Sequence diagrams | 2 |
| Use Case Documentation | User restrictions and rights | 2 |
| Use Case Documentation | Completeness | 2 |
| Use Case Documentation | Documentation quality | 2 |
| Test Case Documentation | Test plan and cases | 2 |
| Test Case Documentation | Use case related tests | 2 |
| Test Case Documentation | Functional requirements | 2 |
| Test Case Documentation | Traceability matrix | 2 |
| Test Case Documentation | Documentation quality | 2 |
| High Level Architecture | Tech stack choice | 2 |
| High Level Architecture | Alternatives and rationale | 2 |
| High Level Architecture | Architecture diagrams | 2 |
| High Level Architecture | Dev/Test/Deploy strategy | 2 |
| High Level Architecture | CI/CD/CT strategy | 2 |
| POC | Working prototype | 4 |
| POC | Code repository and documentation | 4 |
| POC | Deployment instructions | 3 |
| POC | Known issues and improvements | 3 |
| POC | Team contribution and presentation | 3 |
| POC | Overall innovation and quality | 3 |
| Presentation | Problem statement and work distribution | 2 |
| Presentation | Solution and architecture overview | 2 |
| Presentation | POC demo | 2 |
| Presentation | Future scope | 2 |
| Presentation | Q&A | 2 |

---

## Appendix — Problem statement must-haves (prompt grounding)

From `Corporate_Learning_System_problem_statement.md`:

- **Ingest via API:** training attendance, periodic assessment scores, competency-level milestones  
- **Rule-based risk identification** with defined rule format  
- **Interventions:** remedial training, coaching/mentoring  
- **Compliance-ready reporting**  
- **Must-haves:** employee learning profile aggregation; configurable risk rules; at-risk classification; intervention history/outcomes; simple dashboards/reports  
- **Evaluation focus:** realistic competency rules; competency progression; usefulness for trainers/L&D; clean separation of data, rules, reporting  

---

## Changelog

| Date | Change |
|------|--------|
| 2026-06-02 | Initial log: PROMPT-001 + reusable library (TPL A–L) |

---

*To record a new session prompt, copy PROMPT-TPL-L3 or add a new `### PROMPT-###` block under Session prompts.*
