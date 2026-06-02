CREATE TABLE employees (
    id              BIGSERIAL PRIMARY KEY,
    employee_number VARCHAR(50) NOT NULL UNIQUE,
    full_name       VARCHAR(200) NOT NULL,
    department      VARCHAR(100),
    role_id         VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE competencies (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    max_level   INT NOT NULL DEFAULT 5
);

CREATE TABLE role_competency_requirements (
    id              BIGSERIAL PRIMARY KEY,
    role_id         VARCHAR(50) NOT NULL,
    competency_id   BIGINT NOT NULL REFERENCES competencies(id),
    required_level  INT NOT NULL,
    target_date     DATE,
    UNIQUE (role_id, competency_id)
);

CREATE TABLE training_attendance (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    session_id      VARCHAR(100) NOT NULL,
    course_id       VARCHAR(100),
    session_date    DATE NOT NULL,
    status          VARCHAR(20) NOT NULL,
    mandatory       BOOLEAN NOT NULL DEFAULT TRUE,
    source_ts       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employee_id, session_id, session_date)
);

CREATE TABLE assessment_scores (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    assessment_id   VARCHAR(100) NOT NULL,
    score           DECIMAL(8,2) NOT NULL,
    max_score       DECIMAL(8,2) NOT NULL,
    assessment_type VARCHAR(50),
    competency_id   BIGINT REFERENCES competencies(id),
    assessed_at     DATE NOT NULL,
    UNIQUE (employee_id, assessment_id, assessed_at)
);

CREATE TABLE competency_milestones (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    competency_id   BIGINT NOT NULL REFERENCES competencies(id),
    achieved_level  INT NOT NULL,
    target_level    INT,
    effective_date  DATE NOT NULL,
    status          VARCHAR(30) NOT NULL,
    recorded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_learning_profiles (
    employee_id             BIGINT PRIMARY KEY REFERENCES employees(id),
    attendance_pct_30d      DECIMAL(5,2),
    attendance_pct_mandatory DECIMAL(5,2),
    latest_assessment_pct   DECIMAL(5,2),
    consecutive_low_scores  INT DEFAULT 0,
    competency_status_json  TEXT,
    calculated_at           TIMESTAMP NOT NULL
);

CREATE TABLE risk_rules (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         VARCHAR(50) NOT NULL,
    version         INT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    definition_json TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at    TIMESTAMP,
    UNIQUE (rule_id, version)
);

CREATE TABLE risk_assessments (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees(id),
    rule_id         VARCHAR(50) NOT NULL,
    rule_version    INT NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    evidence_json   TEXT,
    evaluated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE at_risk_classifications (
    employee_id     BIGINT PRIMARY KEY REFERENCES employees(id),
    risk_level      VARCHAR(20) NOT NULL,
    rule_ids_json   TEXT,
    evaluated_at    TIMESTAMP NOT NULL
);

CREATE TABLE interventions (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES employees(id),
    risk_assessment_id  BIGINT REFERENCES risk_assessments(id),
    type                VARCHAR(50) NOT NULL,
    assignee            VARCHAR(200),
    scheduled_date      DATE,
    status              VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE intervention_outcomes (
    id                  BIGSERIAL PRIMARY KEY,
    intervention_id     BIGINT NOT NULL UNIQUE REFERENCES interventions(id),
    outcome             VARCHAR(30) NOT NULL,
    summary             TEXT,
    effectiveness       VARCHAR(30),
    recorded_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE compliance_report_runs (
    id              BIGSERIAL PRIMARY KEY,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    department      VARCHAR(100),
    generated_by    VARCHAR(100),
    report_json     TEXT NOT NULL,
    generated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ingestion_errors (
    id              BIGSERIAL PRIMARY KEY,
    source_type     VARCHAR(50) NOT NULL,
    payload_json    TEXT,
    error_message   TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       VARCHAR(100),
    details_json    TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attendance_employee_date ON training_attendance(employee_id, session_date);
CREATE INDEX idx_assessment_employee_date ON assessment_scores(employee_id, assessed_at);
CREATE INDEX idx_risk_assessment_employee ON risk_assessments(employee_id, evaluated_at);
CREATE INDEX idx_intervention_employee ON interventions(employee_id);
