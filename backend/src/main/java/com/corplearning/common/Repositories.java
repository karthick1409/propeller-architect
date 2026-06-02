package com.corplearning.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeNumber(String employeeNumber);
    List<Employee> findByStatus(EmployeeStatus status);
}

@Repository
interface CompetencyRepository extends JpaRepository<Competency, Long> {
    Optional<Competency> findByCode(String code);
}

@Repository
interface RoleCompetencyRequirementRepository extends JpaRepository<RoleCompetencyRequirement, Long> {
    List<RoleCompetencyRequirement> findByRoleId(String roleId);
}

@Repository
interface TrainingAttendanceRepository extends JpaRepository<TrainingAttendance, Long> {
    List<TrainingAttendance> findByEmployeeIdAndSessionDateAfter(Long employeeId, LocalDate after);
    List<TrainingAttendance> findByEmployeeId(Long employeeId);
}

@Repository
interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, Long> {
    List<AssessmentScore> findByEmployeeIdOrderByAssessedAtDesc(Long employeeId);
}

@Repository
interface CompetencyMilestoneRepository extends JpaRepository<CompetencyMilestone, Long> {
    List<CompetencyMilestone> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
}

@Repository
interface EmployeeLearningProfileRepository extends JpaRepository<EmployeeLearningProfile, Long> {}

@Repository
interface RiskRuleRepository extends JpaRepository<RiskRuleEntity, Long> {
    List<RiskRuleEntity> findByStatus(String status);
    Optional<RiskRuleEntity> findTopByRuleIdOrderByVersionDesc(String ruleId);
}

@Repository
interface RiskAssessmentRepository extends JpaRepository<RiskAssessmentEntity, Long> {
    List<RiskAssessmentEntity> findByEmployeeIdOrderByEvaluatedAtDesc(Long employeeId);
}

@Repository
interface AtRiskClassificationRepository extends JpaRepository<AtRiskClassification, Long> {
    List<AtRiskClassification> findByRiskLevelNot(String riskLevel);
}

@Repository
interface InterventionRepository extends JpaRepository<InterventionEntity, Long> {
    List<InterventionEntity> findByEmployeeId(Long employeeId);
}

@Repository
interface InterventionOutcomeRepository extends JpaRepository<InterventionOutcomeEntity, Long> {}

@Repository
interface ComplianceReportRunRepository extends JpaRepository<ComplianceReportRun, Long> {}

@Repository
interface IngestionErrorRepository extends JpaRepository<IngestionError, Long> {
    long countByResolvedFalse();
}
