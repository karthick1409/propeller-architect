package com.corplearning.common;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class IngestionService {

    private final EmployeeRepository employeeRepository;
    private final TrainingAttendanceRepository attendanceRepository;
    private final AssessmentScoreRepository assessmentRepository;
    private final CompetencyMilestoneRepository milestoneRepository;
    private final CompetencyRepository competencyRepository;
    private final IngestionErrorRepository errorRepository;
    private final ProfileService profileService;

    public IngestionService(EmployeeRepository employeeRepository,
                            TrainingAttendanceRepository attendanceRepository,
                            AssessmentScoreRepository assessmentRepository,
                            CompetencyMilestoneRepository milestoneRepository,
                            CompetencyRepository competencyRepository,
                            IngestionErrorRepository errorRepository,
                            ProfileService profileService) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.assessmentRepository = assessmentRepository;
        this.milestoneRepository = milestoneRepository;
        this.competencyRepository = competencyRepository;
        this.errorRepository = errorRepository;
        this.profileService = profileService;
    }

    @Transactional
    public Map<String, Object> ingestAttendance(List<Map<String, Object>> records) {
        int accepted = 0, rejected = 0;
        Set<Long> refresh = new HashSet<>();
        for (Map<String, Object> rec : records) {
            try {
                Employee emp = resolveEmployee(rec.get("employeeNumber").toString());
                TrainingAttendance a = new TrainingAttendance();
                a.setEmployeeId(emp.getId());
                a.setSessionId(rec.get("sessionId").toString());
                a.setCourseId(String.valueOf(rec.getOrDefault("courseId", "")));
                a.setSessionDate(LocalDate.parse(rec.get("sessionDate").toString()));
                a.setStatus(rec.get("status").toString().toUpperCase());
                a.setMandatory(Boolean.parseBoolean(String.valueOf(rec.getOrDefault("mandatory", true))));
                attendanceRepository.save(a);
                refresh.add(emp.getId());
                accepted++;
            } catch (Exception e) {
                logError("attendance", e.getMessage());
                rejected++;
            }
        }
        refresh.forEach(profileService::refreshProfile);
        return Map.of("accepted", accepted, "rejected", rejected);
    }

    @Transactional
    public Map<String, Object> ingestAssessments(List<Map<String, Object>> records) {
        int accepted = 0, rejected = 0;
        for (Map<String, Object> rec : records) {
            try {
                Employee emp = resolveEmployee(rec.get("employeeNumber").toString());
                BigDecimal score = new BigDecimal(rec.get("score").toString());
                BigDecimal max = new BigDecimal(rec.get("maxScore").toString());
                if (score.compareTo(max) > 0) throw new IllegalArgumentException("Score exceeds max");
                AssessmentScore a = new AssessmentScore();
                a.setEmployeeId(emp.getId());
                a.setAssessmentId(rec.get("assessmentId").toString());
                a.setScore(score);
                a.setMaxScore(max);
                a.setAssessedAt(LocalDate.parse(rec.get("assessedAt").toString()));
                if (rec.containsKey("competencyCode")) {
                    competencyRepository.findByCode(rec.get("competencyCode").toString())
                            .ifPresent(c -> a.setCompetencyId(c.getId()));
                }
                assessmentRepository.save(a);
                profileService.refreshProfile(emp.getId());
                accepted++;
            } catch (Exception e) {
                logError("assessment", e.getMessage());
                rejected++;
            }
        }
        return Map.of("accepted", accepted, "rejected", rejected);
    }

    @Transactional
    public Map<String, Object> ingestMilestones(List<Map<String, Object>> records) {
        int accepted = 0, rejected = 0;
        for (Map<String, Object> rec : records) {
            try {
                Employee emp = resolveEmployee(rec.get("employeeNumber").toString());
                Competency comp = competencyRepository.findByCode(rec.get("competencyCode").toString())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown competency"));
                CompetencyMilestone m = new CompetencyMilestone();
                m.setEmployeeId(emp.getId());
                m.setCompetencyId(comp.getId());
                m.setAchievedLevel(Integer.parseInt(rec.get("achievedLevel").toString()));
                m.setEffectiveDate(LocalDate.parse(rec.get("effectiveDate").toString()));
                m.setStatus(rec.getOrDefault("status", "ACHIEVED").toString());
                milestoneRepository.save(m);
                profileService.refreshProfile(emp.getId());
                accepted++;
            } catch (Exception e) {
                logError("milestone", e.getMessage());
                rejected++;
            }
        }
        return Map.of("accepted", accepted, "rejected", rejected);
    }

    @Transactional
    public Map<String, Object> syncEmployees(List<Map<String, Object>> records) {
        int upserted = 0;
        for (Map<String, Object> rec : records) {
            String num = rec.get("employeeNumber").toString();
            Employee emp = employeeRepository.findByEmployeeNumber(num).orElse(new Employee());
            emp.setEmployeeNumber(num);
            emp.setFullName(rec.get("fullName").toString());
            emp.setDepartment(String.valueOf(rec.getOrDefault("department", "")));
            emp.setRoleId(String.valueOf(rec.getOrDefault("roleId", "GENERAL")));
            if (rec.containsKey("status")) {
                emp.setStatus(EmployeeStatus.valueOf(rec.get("status").toString()));
            }
            employeeRepository.save(emp);
            upserted++;
        }
        return Map.of("upserted", upserted);
    }

    public long unresolvedErrorCount() {
        return errorRepository.countByResolvedFalse();
    }

    private Employee resolveEmployee(String employeeNumber) {
        return employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new IllegalArgumentException("Unknown employee: " + employeeNumber));
    }

    private void logError(String source, String message) {
        IngestionError err = new IngestionError();
        err.setSourceType(source);
        err.setErrorMessage(message);
        err.setResolved(false);
        errorRepository.save(err);
    }
}

