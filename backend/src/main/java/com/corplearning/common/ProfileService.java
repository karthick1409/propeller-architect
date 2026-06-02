package com.corplearning.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final EmployeeRepository employeeRepository;
    private final TrainingAttendanceRepository attendanceRepository;
    private final AssessmentScoreRepository assessmentRepository;
    private final CompetencyMilestoneRepository milestoneRepository;
    private final RoleCompetencyRequirementRepository requirementRepository;
    private final CompetencyRepository competencyRepository;
    private final EmployeeLearningProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public ProfileService(EmployeeRepository employeeRepository,
                          TrainingAttendanceRepository attendanceRepository,
                          AssessmentScoreRepository assessmentRepository,
                          CompetencyMilestoneRepository milestoneRepository,
                          RoleCompetencyRequirementRepository requirementRepository,
                          CompetencyRepository competencyRepository,
                          EmployeeLearningProfileRepository profileRepository,
                          ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.assessmentRepository = assessmentRepository;
        this.milestoneRepository = milestoneRepository;
        this.requirementRepository = requirementRepository;
        this.competencyRepository = competencyRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EmployeeLearningProfile refreshProfile(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        LocalDate since = LocalDate.now().minusDays(30);
        List<TrainingAttendance> sessions = attendanceRepository.findByEmployeeIdAndSessionDateAfter(employeeId, since);

        BigDecimal attendancePct = pctPresent(sessions, false);
        BigDecimal mandatoryPct = pctPresent(sessions, true);

        List<AssessmentScore> scores = assessmentRepository.findByEmployeeIdOrderByAssessedAtDesc(employeeId);
        BigDecimal latestPct = scores.isEmpty() ? null
                : BigDecimal.valueOf(scores.get(0).percentage()).setScale(2, RoundingMode.HALF_UP);
        int consecutiveLow = countConsecutiveLow(scores, 60.0);

        Map<String, Object> competencyStatus = buildCompetencyStatus(employee, employeeId);

        EmployeeLearningProfile profile = profileRepository.findById(employeeId)
                .orElseGet(EmployeeLearningProfile::new);
        profile.setEmployeeId(employeeId);
        profile.setAttendancePct30d(attendancePct);
        profile.setAttendancePctMandatory(mandatoryPct);
        profile.setLatestAssessmentPct(latestPct);
        profile.setConsecutiveLowScores(consecutiveLow);
        try {
            profile.setCompetencyStatusJson(objectMapper.writeValueAsString(competencyStatus));
        } catch (Exception e) {
            profile.setCompetencyStatusJson("{}");
        }
        profile.setCalculatedAt(Instant.now());
        return profileRepository.save(profile);
    }

    public Map<String, Object> getProfileView(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        EmployeeLearningProfile profile = profileRepository.findById(employeeId).orElse(null);
        AtRiskClassification atRisk = null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", employee.getId());
        result.put("employeeNumber", employee.getEmployeeNumber());
        result.put("fullName", employee.getFullName());
        result.put("department", employee.getDepartment());
        result.put("roleId", employee.getRoleId());
        result.put("status", employee.getStatus().name());
        if (profile != null) {
            result.put("attendancePct30d", profile.getAttendancePct30d());
            result.put("attendancePctMandatory", profile.getAttendancePctMandatory());
            result.put("latestAssessmentPct", profile.getLatestAssessmentPct());
            result.put("consecutiveLowScores", profile.getConsecutiveLowScores());
            result.put("competencyStatus", parseJson(profile.getCompetencyStatusJson()));
            result.put("calculatedAt", profile.getCalculatedAt());
        }
        return result;
    }

    public ProfileMetrics metricsFor(Long employeeId) {
        EmployeeLearningProfile p = profileRepository.findById(employeeId).orElse(null);
        if (p == null) {
            p = refreshProfile(employeeId);
        }
        return new ProfileMetrics(
                p.getAttendancePct30d() != null ? p.getAttendancePct30d().doubleValue() : 100.0,
                p.getAttendancePctMandatory() != null ? p.getAttendancePctMandatory().doubleValue() : 100.0,
                p.getLatestAssessmentPct() != null ? p.getLatestAssessmentPct().doubleValue() : 100.0,
                p.getConsecutiveLowScores() != null ? p.getConsecutiveLowScores() : 0,
                hasMilestoneGap(employeeId)
        );
    }

    private boolean hasMilestoneGap(Long employeeId) {
        Employee e = employeeRepository.findById(employeeId).orElse(null);
        if (e == null) return false;
        List<RoleCompetencyRequirement> reqs = requirementRepository.findByRoleId(e.getRoleId());
        for (RoleCompetencyRequirement req : reqs) {
            if (req.getTargetDate() != null && LocalDate.now().isAfter(req.getTargetDate())) {
                int achieved = latestLevel(employeeId, req.getCompetencyId());
                if (achieved < req.getRequiredLevel()) return true;
            }
        }
        return false;
    }

    private int latestLevel(Long employeeId, Long competencyId) {
        return milestoneRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId).stream()
                .filter(m -> m.getCompetencyId().equals(competencyId))
                .mapToInt(CompetencyMilestone::getAchievedLevel)
                .findFirst().orElse(0);
    }

    private Map<String, Object> buildCompetencyStatus(Employee employee, Long employeeId) {
        Map<String, Object> status = new LinkedHashMap<>();
        for (RoleCompetencyRequirement req : requirementRepository.findByRoleId(employee.getRoleId())) {
            competencyRepository.findById(req.getCompetencyId()).ifPresent(c -> {
                int achieved = latestLevel(employeeId, c.getId());
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("competencyCode", c.getCode());
                entry.put("requiredLevel", req.getRequiredLevel());
                entry.put("achievedLevel", achieved);
                entry.put("targetDate", req.getTargetDate());
                entry.put("gap", Math.max(0, req.getRequiredLevel() - achieved));
                status.put(c.getCode(), entry);
            });
        }
        return status;
    }

    private BigDecimal pctPresent(List<TrainingAttendance> sessions, boolean mandatoryOnly) {
        List<TrainingAttendance> filtered = mandatoryOnly
                ? sessions.stream().filter(TrainingAttendance::isMandatory).collect(Collectors.toList())
                : sessions;
        if (filtered.isEmpty()) return BigDecimal.valueOf(100);
        long present = filtered.stream().filter(s -> "PRESENT".equalsIgnoreCase(s.getStatus())).count();
        return BigDecimal.valueOf(present * 100.0 / filtered.size()).setScale(2, RoundingMode.HALF_UP);
    }

    private int countConsecutiveLow(List<AssessmentScore> scores, double threshold) {
        int count = 0;
        for (AssessmentScore s : scores) {
            if (s.percentage() < threshold) count++;
            else break;
        }
        return count;
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static final class ProfileMetrics {
        private final double attendancePct30d;
        private final double attendancePctMandatory;
        private final double latestAssessmentPct;
        private final int consecutiveLowScores;
        private final boolean milestoneGap;

        public ProfileMetrics(double attendancePct30d, double attendancePctMandatory,
                              double latestAssessmentPct, int consecutiveLowScores, boolean milestoneGap) {
            this.attendancePct30d = attendancePct30d;
            this.attendancePctMandatory = attendancePctMandatory;
            this.latestAssessmentPct = latestAssessmentPct;
            this.consecutiveLowScores = consecutiveLowScores;
            this.milestoneGap = milestoneGap;
        }

        public double getAttendancePct30d() { return attendancePct30d; }
        public double getAttendancePctMandatory() { return attendancePctMandatory; }
        public double getLatestAssessmentPct() { return latestAssessmentPct; }
        public int getConsecutiveLowScores() { return consecutiveLowScores; }
        public boolean isMilestoneGap() { return milestoneGap; }
    }
}

