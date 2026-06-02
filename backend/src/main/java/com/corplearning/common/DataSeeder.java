package com.corplearning.common;

import com.corplearning.common.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(EmployeeRepository employees,
                           CompetencyRepository competencies,
                           RoleCompetencyRequirementRepository requirements,
                           TrainingAttendanceRepository attendance,
                           AssessmentScoreRepository assessments,
                           CompetencyMilestoneRepository milestones,
                           RiskRuleService ruleService,
                           ProfileService profileService,
                           RiskEvaluationService riskEvaluation) {
        return args -> {
            if (employees.count() > 0) return;

            Competency fork = comp(competencies, "FORKLIFT", "Forklift Safety");
            Competency cyber = comp(competencies, "CYBER", "Cybersecurity Awareness");
            req(requirements, "WAREHOUSE_OPERATOR", fork.getId(), 3, LocalDate.now().minusDays(5));
            req(requirements, "OFFICE_STAFF", cyber.getId(), 2, LocalDate.now().plusDays(60));

            Employee e1 = emp(employees, "EMP-ACTIVE-01", "Alex Morgan", "Operations", "WAREHOUSE_OPERATOR");
            Employee e2 = emp(employees, "EMP-RISK-ATT-01", "Jamie Brooks", "Operations", "WAREHOUSE_OPERATOR");
            Employee e3 = emp(employees, "EMP-RISK-SCR-01", "Sam Rivera", "IT", "OFFICE_STAFF");
            Employee e4 = emp(employees, "EMP-RISK-MS-01", "Taylor Chen", "Operations", "WAREHOUSE_OPERATOR");
            Employee e5 = emp(employees, "EMP-BOUNDARY-01", "Jordan Lee", "Operations", "WAREHOUSE_OPERATOR");

            seedAttendance(attendance, e1, 9, 10);
            seedAttendance(attendance, e2, 7, 10);
            seedAttendancePct(attendance, e5, 75, 100);
            seedAttendance(attendance, e4, 8, 10);

            assess(assessments, e1, "A1", 88, 100);
            assess(assessments, e3, "A2", 55, 100, LocalDate.now().minusDays(14));
            assess(assessments, e3, "A3", 58, 100, LocalDate.now().minusDays(7));
            assess(assessments, e5, "A4", 85, 100);

            mile(milestones, e1, fork.getId(), 2);
            mile(milestones, e4, fork.getId(), 2);
            mile(milestones, e5, fork.getId(), 2);

            ruleService.saveDraft("R-ATT-01", "Low Mandatory Attendance", "HIGH",
                    "{\"ruleId\":\"R-ATT-01\",\"severity\":\"HIGH\",\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"attendance_percentage\",\"scope\":\"mandatory_courses\",\"period\":\"30_days\",\"operator\":\"less_than\",\"value\":75}]}}");
            ruleService.saveDraft("R-SCR-01", "Consecutive Low Scores", "HIGH",
                    "{\"ruleId\":\"R-SCR-01\",\"severity\":\"HIGH\",\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"consecutive_low_assessments\",\"operator\":\"greater_than\",\"value\":1}]}}");
            ruleService.saveDraft("R-MS-01", "Milestone Slip", "CRITICAL",
                    "{\"ruleId\":\"R-MS-01\",\"severity\":\"CRITICAL\",\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"competency_milestone_gap\",\"operator\":\"equals\",\"value\":1}]}}");
            ruleService.saveDraft("R-CMP-01", "Composite Risk", "MEDIUM",
                    "{\"ruleId\":\"R-CMP-01\",\"severity\":\"MEDIUM\",\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"attendance_percentage\",\"scope\":\"mandatory_courses\",\"operator\":\"less_than\",\"value\":80},{\"metric\":\"latest_assessment_percentage\",\"operator\":\"less_than\",\"value\":70}]}}");
            ruleService.saveDraft("R-ATT-02", "Moderate Attendance", "LOW",
                    "{\"ruleId\":\"R-ATT-02\",\"severity\":\"LOW\",\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"attendance_percentage\",\"scope\":\"mandatory_courses\",\"operator\":\"less_than\",\"value\":90}]}}");

            ruleService.listAll().forEach(r -> ruleService.activate(r.getId()));

            Arrays.asList(e1, e2, e3, e4, e5).forEach(e -> profileService.refreshProfile(e.getId()));
            riskEvaluation.evaluateAll();
        };
    }

    private static Competency comp(CompetencyRepository repo, String code, String name) {
        Competency c = new Competency();
        c.setCode(code);
        c.setName(name);
        return repo.save(c);
    }

    private static void req(RoleCompetencyRequirementRepository repo, String role, Long compId, int level, LocalDate target) {
        RoleCompetencyRequirement r = new RoleCompetencyRequirement();
        r.setRoleId(role);
        r.setCompetencyId(compId);
        r.setRequiredLevel(level);
        r.setTargetDate(target);
        repo.save(r);
    }

    private static Employee emp(EmployeeRepository repo, String num, String name, String dept, String role) {
        Employee e = new Employee();
        e.setEmployeeNumber(num);
        e.setFullName(name);
        e.setDepartment(dept);
        e.setRoleId(role);
        e.setStatus(EmployeeStatus.ACTIVE);
        return repo.save(e);
    }

    private static void seedAttendance(TrainingAttendanceRepository repo, Employee e, int present, int total) {
        for (int i = 0; i < total; i++) {
            TrainingAttendance a = new TrainingAttendance();
            a.setEmployeeId(e.getId());
            a.setSessionId("S-" + e.getEmployeeNumber() + "-" + i);
            a.setCourseId("MAND-101");
            a.setSessionDate(LocalDate.now().minusDays(i));
            a.setStatus(i < present ? "PRESENT" : "ABSENT");
            a.setMandatory(true);
            repo.save(a);
        }
    }

    private static void seedAttendancePct(TrainingAttendanceRepository repo, Employee e, int presentPct, int totalPct) {
        int total = 4;
        int present = (int) Math.round(total * presentPct / 100.0);
        seedAttendance(repo, e, present, total);
    }

    private static void assess(AssessmentScoreRepository repo, Employee e, String id, double score, double max) {
        assess(repo, e, id, score, max, LocalDate.now());
    }

    private static void assess(AssessmentScoreRepository repo, Employee e, String id, double score, double max, LocalDate date) {
        AssessmentScore a = new AssessmentScore();
        a.setEmployeeId(e.getId());
        a.setAssessmentId(id);
        a.setScore(BigDecimal.valueOf(score));
        a.setMaxScore(BigDecimal.valueOf(max));
        a.setAssessedAt(date);
        repo.save(a);
    }

    private static void mile(CompetencyMilestoneRepository repo, Employee e, Long compId, int level) {
        CompetencyMilestone m = new CompetencyMilestone();
        m.setEmployeeId(e.getId());
        m.setCompetencyId(compId);
        m.setAchievedLevel(level);
        m.setEffectiveDate(LocalDate.now().minusDays(10));
        m.setStatus("ACHIEVED");
        repo.save(m);
    }
}
