package com.corplearning.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private final ComplianceReportRunRepository reportRepository;
    private final EmployeeRepository employeeRepository;
    private final AtRiskClassificationRepository classificationRepository;
    private final InterventionRepository interventionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskRuleRepository ruleRepository;
    private final IngestionErrorRepository errorRepository;
    private final ObjectMapper objectMapper;

    public ReportingService(ComplianceReportRunRepository reportRepository,
                            EmployeeRepository employeeRepository,
                            AtRiskClassificationRepository classificationRepository,
                            InterventionRepository interventionRepository,
                            RiskAssessmentRepository riskAssessmentRepository,
                            RiskRuleRepository ruleRepository,
                            IngestionErrorRepository errorRepository,
                            ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.employeeRepository = employeeRepository;
        this.classificationRepository = classificationRepository;
        this.interventionRepository = interventionRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.ruleRepository = ruleRepository;
        this.errorRepository = errorRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> dashboardLnd() {
        Map<String, Object> d = new LinkedHashMap<>();
        long atRisk = classificationRepository.findByRiskLevelNot("NONE").size();
        long activeEmployees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE).size();
        d.put("activeEmployees", activeEmployees);
        d.put("atRiskCount", atRisk);
        d.put("ingestionErrors", errorRepository.countByResolvedFalse());
        d.put("openInterventions", interventionRepository.findAll().stream()
                .filter(i -> !"COMPLETED".equals(i.getStatus()) && !"CANCELLED".equals(i.getStatus())).count());
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (AtRiskClassification c : classificationRepository.findByRiskLevelNot("NONE")) {
            byLevel.merge(c.getRiskLevel(), 1L, Long::sum);
        }
        d.put("atRiskByLevel", byLevel);
        d.put("activeRules", ruleRepository.findByStatus("ACTIVE").size());
        return d;
    }

    public List<Map<String, Object>> listIngestionErrors() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (IngestionError err : errorRepository.findByResolvedFalseOrderByIdDesc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", err.getId());
            row.put("sourceType", err.getSourceType());
            row.put("errorMessage", err.getErrorMessage());
            row.put("createdAt", err.getCreatedAt());
            row.put("resolved", err.isResolved());
            rows.add(row);
        }
        return rows;
    }

    public ComplianceReportRun generateReport(LocalDate start, LocalDate end, String department) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("periodStart", start);
        report.put("periodEnd", end);
        report.put("department", department != null ? department : "ALL");
        report.put("generatedAt", Instant.now());
        report.put("ruleVersions", ruleRepository.findByStatus("ACTIVE").stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ruleId", r.getRuleId());
                    m.put("version", r.getVersion());
                    return m;
                }).collect(Collectors.toList()));
        List<Map<String, Object>> employees = new ArrayList<>();
        for (Employee e : employeeRepository.findByStatus(EmployeeStatus.ACTIVE)) {
            if (department != null && !department.isBlank() && !department.equals(e.getDepartment())) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeNumber", e.getEmployeeNumber());
            row.put("fullName", e.getFullName());
            classificationRepository.findById(e.getId()).ifPresent(c -> row.put("riskLevel", c.getRiskLevel()));
            row.put("interventions", interventionRepository.findByEmployeeId(e.getId()).size());
            row.put("riskEvents", riskAssessmentRepository.findByEmployeeIdOrderByEvaluatedAtDesc(e.getId()).size());
            employees.add(row);
        }
        report.put("employees", employees);
        report.put("summary", summaryMap(employees));

        ComplianceReportRun run = new ComplianceReportRun();
        run.setPeriodStart(start);
        run.setPeriodEnd(end);
        run.setDepartment(department);
        run.setGeneratedAt(Instant.now());
        try {
            run.setReportJson(objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            run.setReportJson("{}");
        }
        return reportRepository.save(run);
    }

    public String exportCsv(Long reportId) {
        ComplianceReportRun run = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        StringBuilder sb = new StringBuilder("employeeNumber,fullName,riskLevel,interventions,riskEvents\n");
        try {
            Map<?, ?> report = objectMapper.readValue(run.getReportJson(), Map.class);
            List<?> employees = (List<?>) report.get("employees");
            for (Object o : employees) {
                Map<?, ?> row = (Map<?, ?>) o;
                sb.append(row.get("employeeNumber")).append(',')
                        .append(row.get("fullName")).append(',')
                        .append(row.containsKey("riskLevel") ? row.get("riskLevel") : "NONE").append(',')
                        .append(row.get("interventions")).append(',')
                        .append(row.get("riskEvents")).append('\n');
            }
        } catch (Exception e) {
            sb.append("error,").append(e.getMessage()).append('\n');
        }
        return sb.toString();
    }

    private Map<String, Object> summaryMap(List<Map<String, Object>> employees) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEmployees", employees.size());
        long atRisk = 0;
        for (Map<String, Object> r : employees) {
            if (r.containsKey("riskLevel") && !"NONE".equals(String.valueOf(r.get("riskLevel")))) {
                atRisk++;
            }
        }
        summary.put("atRisk", atRisk);
        return summary;
    }
}

