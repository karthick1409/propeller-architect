package com.corplearning.common;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "competencies")
class Competency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(name = "max_level") private int maxLevel = 5;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@Entity @Table(name = "role_competency_requirements")
class RoleCompetencyRequirement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "role_id", nullable = false) private String roleId;
    @Column(name = "competency_id", nullable = false) private Long competencyId;
    @Column(name = "required_level", nullable = false) private int requiredLevel;
    @Column(name = "target_date") private LocalDate targetDate;
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public Long getCompetencyId() { return competencyId; }
    public void setCompetencyId(Long competencyId) { this.competencyId = competencyId; }
    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
}

@Entity @Table(name = "training_attendance")
class TrainingAttendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "employee_id", nullable = false) private Long employeeId;
    @Column(name = "session_id", nullable = false) private String sessionId;
    @Column(name = "course_id") private String courseId;
    @Column(name = "session_date", nullable = false) private LocalDate sessionDate;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private boolean mandatory = true;
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
}

@Entity @Table(name = "assessment_scores")
class AssessmentScore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "employee_id", nullable = false) private Long employeeId;
    @Column(name = "assessment_id", nullable = false) private String assessmentId;
    @Column(nullable = false) private BigDecimal score;
    @Column(name = "max_score", nullable = false) private BigDecimal maxScore;
    @Column(name = "competency_id") private Long competencyId;
    @Column(name = "assessed_at", nullable = false) private LocalDate assessedAt;
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getAssessmentId() { return assessmentId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
    public LocalDate getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDate assessedAt) { this.assessedAt = assessedAt; }
    public Long getCompetencyId() { return competencyId; }
    public void setCompetencyId(Long competencyId) { this.competencyId = competencyId; }
    public double percentage() {
        return score.doubleValue() * 100.0 / maxScore.doubleValue();
    }
}

@Entity @Table(name = "competency_milestones")
class CompetencyMilestone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "employee_id", nullable = false) private Long employeeId;
    @Column(name = "competency_id", nullable = false) private Long competencyId;
    @Column(name = "achieved_level", nullable = false) private int achievedLevel;
    @Column(name = "effective_date", nullable = false) private LocalDate effectiveDate;
    @Column(nullable = false) private String status;
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getCompetencyId() { return competencyId; }
    public void setCompetencyId(Long competencyId) { this.competencyId = competencyId; }
    public int getAchievedLevel() { return achievedLevel; }
    public void setAchievedLevel(int achievedLevel) { this.achievedLevel = achievedLevel; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

@Entity @Table(name = "employee_learning_profiles")
class EmployeeLearningProfile {
    @Id @Column(name = "employee_id") private Long employeeId;
    @Column(name = "attendance_pct_30d") private BigDecimal attendancePct30d;
    @Column(name = "attendance_pct_mandatory") private BigDecimal attendancePctMandatory;
    @Column(name = "latest_assessment_pct") private BigDecimal latestAssessmentPct;
    @Column(name = "consecutive_low_scores") private Integer consecutiveLowScores;
    @Column(name = "competency_status_json") private String competencyStatusJson;
    @Column(name = "calculated_at", nullable = false) private Instant calculatedAt;
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public BigDecimal getAttendancePct30d() { return attendancePct30d; }
    public void setAttendancePct30d(BigDecimal v) { this.attendancePct30d = v; }
    public BigDecimal getAttendancePctMandatory() { return attendancePctMandatory; }
    public void setAttendancePctMandatory(BigDecimal v) { this.attendancePctMandatory = v; }
    public BigDecimal getLatestAssessmentPct() { return latestAssessmentPct; }
    public void setLatestAssessmentPct(BigDecimal v) { this.latestAssessmentPct = v; }
    public Integer getConsecutiveLowScores() { return consecutiveLowScores; }
    public void setConsecutiveLowScores(Integer v) { this.consecutiveLowScores = v; }
    public String getCompetencyStatusJson() { return competencyStatusJson; }
    public void setCompetencyStatusJson(String v) { this.competencyStatusJson = v; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant v) { this.calculatedAt = v; }
}

@Entity @Table(name = "risk_rules")
class RiskRuleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "rule_id", nullable = false) private String ruleId;
    @Column(nullable = false) private int version;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String severity;
    @Column(nullable = false) private String status;
    @Column(name = "definition_json", nullable = false) private String definitionJson;
    @Column(name = "activated_at") private Instant activatedAt;
    public Long getId() { return id; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDefinitionJson() { return definitionJson; }
    public void setDefinitionJson(String definitionJson) { this.definitionJson = definitionJson; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
}

@Entity @Table(name = "risk_assessments")
class RiskAssessmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "employee_id", nullable = false) private Long employeeId;
    @Column(name = "rule_id", nullable = false) private String ruleId;
    @Column(name = "rule_version", nullable = false) private int ruleVersion;
    @Column(nullable = false) private String severity;
    @Column(name = "evidence_json") private String evidenceJson;
    @Column(name = "evaluated_at") private Instant evaluatedAt;
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public int getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(int ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}

@Entity @Table(name = "at_risk_classifications")
class AtRiskClassification {
    @Id @Column(name = "employee_id") private Long employeeId;
    @Column(name = "risk_level", nullable = false) private String riskLevel;
    @Column(name = "rule_ids_json") private String ruleIdsJson;
    @Column(name = "evaluated_at", nullable = false) private Instant evaluatedAt;
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRuleIdsJson() { return ruleIdsJson; }
    public void setRuleIdsJson(String ruleIdsJson) { this.ruleIdsJson = ruleIdsJson; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}

@Entity @Table(name = "interventions")
class InterventionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "employee_id", nullable = false) private Long employeeId;
    @Column(name = "risk_assessment_id") private Long riskAssessmentId;
    @Column(nullable = false) private String type;
    private String assignee;
    @Column(name = "scheduled_date") private LocalDate scheduledDate;
    @Column(nullable = false) private String status;
    private String notes;
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Long getRiskAssessmentId() { return riskAssessmentId; }
    public void setRiskAssessmentId(Long riskAssessmentId) { this.riskAssessmentId = riskAssessmentId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

@Entity @Table(name = "intervention_outcomes")
class InterventionOutcomeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "intervention_id", nullable = false, unique = true) private Long interventionId;
    @Column(nullable = false) private String outcome;
    private String summary;
    private String effectiveness;
    public Long getInterventionId() { return interventionId; }
    public void setInterventionId(Long interventionId) { this.interventionId = interventionId; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getEffectiveness() { return effectiveness; }
    public void setEffectiveness(String effectiveness) { this.effectiveness = effectiveness; }
}

@Entity @Table(name = "compliance_report_runs")
class ComplianceReportRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    private String department;
    @Column(name = "report_json", nullable = false) private String reportJson;
    @Column(name = "generated_at") private Instant generatedAt;
    public Long getId() { return id; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getReportJson() { return reportJson; }
    public void setReportJson(String reportJson) { this.reportJson = reportJson; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}

@Entity @Table(name = "ingestion_errors")
class IngestionError {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "source_type", nullable = false) private String sourceType;
    @Column(name = "error_message", nullable = false) private String errorMessage;
    @Column(nullable = false) private boolean resolved;
    public Long getId() { return id; }
    public String getSourceType() { return sourceType; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
