package com.corplearning.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private static final String REPORT_TYPE = "CORPORATE_COMPLIANCE";

    private final ComplianceReportRunRepository reportRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLearningProfileRepository profileRepository;
    private final AtRiskClassificationRepository classificationRepository;
    private final InterventionRepository interventionRepository;
    private final InterventionOutcomeRepository outcomeRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskRuleRepository ruleRepository;
    private final IngestionErrorRepository errorRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public ReportingService(ComplianceReportRunRepository reportRepository,
                            EmployeeRepository employeeRepository,
                            EmployeeLearningProfileRepository profileRepository,
                            AtRiskClassificationRepository classificationRepository,
                            InterventionRepository interventionRepository,
                            InterventionOutcomeRepository outcomeRepository,
                            RiskAssessmentRepository riskAssessmentRepository,
                            RiskRuleRepository ruleRepository,
                            IngestionErrorRepository errorRepository,
                            AuditLogRepository auditLogRepository,
                            ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.employeeRepository = employeeRepository;
        this.profileRepository = profileRepository;
        this.classificationRepository = classificationRepository;
        this.interventionRepository = interventionRepository;
        this.outcomeRepository = outcomeRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.ruleRepository = ruleRepository;
        this.errorRepository = errorRepository;
        this.auditLogRepository = auditLogRepository;
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
        d.put("reportsGenerated", reportRepository.count());
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

    public List<Map<String, Object>> listReports() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ComplianceReportRun run : reportRepository.findAllByOrderByGeneratedAtDesc()) {
            rows.add(reportSummary(run, false));
        }
        return rows;
    }

    public Map<String, Object> getReport(Long id) {
        ComplianceReportRun run = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        return reportSummary(run, true);
    }

    @Transactional
    public ComplianceReportRun generateReport(LocalDate start, LocalDate end, String department) {
        Instant periodStart = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant periodEnd = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", REPORT_TYPE);
        report.put("periodStart", start);
        report.put("periodEnd", end);
        report.put("department", department != null && !department.isBlank() ? department : "ALL");
        report.put("generatedAt", Instant.now());
        report.put("generatedBy", actorLabel());
        report.put("ruleVersions", ruleRepository.findByStatus("ACTIVE").stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ruleId", r.getRuleId());
                    m.put("version", r.getVersion());
                    m.put("name", r.getName());
                    m.put("severity", r.getSeverity());
                    return m;
                }).collect(Collectors.toList()));

        List<Map<String, Object>> employees = new ArrayList<>();
        for (Employee e : employeeRepository.findByStatus(EmployeeStatus.ACTIVE)) {
            if (department != null && !department.isBlank() && !department.equals(e.getDepartment())) {
                continue;
            }
            employees.add(buildEmployeeRow(e, periodStart, periodEnd));
        }
        report.put("employees", employees);
        report.put("summary", summaryMap(employees));
        report.put("interventionSummary", interventionSummary());

        ComplianceReportRun run = new ComplianceReportRun();
        run.setPeriodStart(start);
        run.setPeriodEnd(end);
        run.setDepartment(department);
        run.setGeneratedAt(Instant.now());
        run.setReportJson("{}");
        run = reportRepository.save(run);

        report.put("runId", run.getId());
        try {
            run.setReportJson(objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            run.setReportJson("{}");
        }
        run = reportRepository.save(run);
        logAudit("REPORT_GENERATE", "COMPLIANCE_REPORT", String.valueOf(run.getId()),
                Map.of("runId", run.getId(), "periodStart", start, "periodEnd", end,
                        "department", report.get("department"), "generatedBy", actorLabel()));
        return run;
    }

    public String exportCsv(Long reportId) {
        ComplianceReportRun run = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        StringBuilder sb = new StringBuilder();
        sb.append("runId,periodStart,periodEnd,department,generatedAt\n");
        sb.append(run.getId()).append(',')
                .append(run.getPeriodStart()).append(',')
                .append(run.getPeriodEnd()).append(',')
                .append(run.getDepartment() != null ? run.getDepartment() : "ALL").append(',')
                .append(run.getGeneratedAt()).append('\n');
        sb.append("\nemployeeNumber,fullName,department,riskLevel,attendancePct,latestScorePct,interventions,riskEvents\n");
        try {
            Map<?, ?> report = objectMapper.readValue(run.getReportJson(), Map.class);
            List<?> employees = (List<?>) report.get("employees");
            if (employees != null) {
                for (Object o : employees) {
                    Map<?, ?> row = (Map<?, ?>) o;
                    sb.append(row.get("employeeNumber")).append(',')
                            .append(csvEscape(String.valueOf(row.get("fullName")))).append(',')
                            .append(row.get("department")).append(',')
                            .append(row.containsKey("riskLevel") ? row.get("riskLevel") : "NONE").append(',')
                            .append(row.get("attendancePctMandatory")).append(',')
                            .append(row.get("latestAssessmentPct")).append(',')
                            .append(row.get("interventions")).append(',')
                            .append(row.get("riskEvents")).append('\n');
                }
            }
            sb.append("\ninterventionId,employeeNumber,type,scheduledDate,status,outcome,effectiveness\n");
            if (employees != null) {
                for (Object o : employees) {
                    Map<?, ?> row = (Map<?, ?>) o;
                    List<?> trail = (List<?>) row.get("interventionTrail");
                    if (trail == null) continue;
                    for (Object t : trail) {
                        Map<?, ?> iv = (Map<?, ?>) t;
                        sb.append(iv.get("interventionId")).append(',')
                                .append(row.get("employeeNumber")).append(',')
                                .append(iv.get("type")).append(',')
                                .append(iv.get("scheduledDate")).append(',')
                                .append(iv.get("status")).append(',')
                                .append(iv.get("outcome")).append(',')
                                .append(iv.get("effectiveness")).append('\n');
                    }
                }
            }
        } catch (Exception e) {
            sb.append("error,").append(csvEscape(e.getMessage())).append('\n');
        }
        logExport(reportId, "csv");
        return sb.toString();
    }

    public byte[] exportPdf(Long reportId) {
        ComplianceReportRun run = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            document.add(new Paragraph("Corporate Learning Compliance Report", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Run ID: " + run.getId()));
            document.add(new Paragraph("Period: " + run.getPeriodStart() + " to " + run.getPeriodEnd()));
            document.add(new Paragraph("Department: " + (run.getDepartment() != null ? run.getDepartment() : "ALL")));
            document.add(new Paragraph("Generated: " + run.getGeneratedAt()));
            document.add(new Paragraph("Generated by: " + actorLabel()));
            document.add(new Paragraph(" "));

            Map<?, ?> report = objectMapper.readValue(run.getReportJson(), Map.class);
            Map<?, ?> summary = (Map<?, ?>) report.get("summary");
            if (summary != null) {
                document.add(new Paragraph("Summary", headerFont));
                document.add(new Paragraph("Total employees: " + summary.get("totalEmployees")));
                document.add(new Paragraph("At-risk: " + summary.get("atRisk")));
                if (summary.get("avgAttendancePctMandatory") != null) {
                    document.add(new Paragraph("Avg mandatory attendance %: " + summary.get("avgAttendancePctMandatory")));
                }
                document.add(new Paragraph(" "));
            }

            document.add(new Paragraph("Employee Detail", headerFont));
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.addCell(cell("Employee", headerFont));
            table.addCell(cell("Dept", headerFont));
            table.addCell(cell("Risk", headerFont));
            table.addCell(cell("Att %", headerFont));
            table.addCell(cell("Score %", headerFont));
            table.addCell(cell("Interventions", headerFont));

            List<?> employees = (List<?>) report.get("employees");
            if (employees != null) {
                for (Object o : employees) {
                    Map<?, ?> row = (Map<?, ?>) o;
                    table.addCell(cell(String.valueOf(row.get("fullName")), null));
                    table.addCell(cell(String.valueOf(row.get("department")), null));
                    table.addCell(cell(row.containsKey("riskLevel") ? String.valueOf(row.get("riskLevel")) : "NONE", null));
                    table.addCell(cell(String.valueOf(row.get("attendancePctMandatory")), null));
                    table.addCell(cell(String.valueOf(row.get("latestAssessmentPct")), null));
                    table.addCell(cell(String.valueOf(row.get("interventions")), null));
                }
            }
            document.add(table);
            document.close();
            logExport(reportId, "pdf");
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF export failed: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> auditRiskDecisions(Long employeeId, LocalDate from, LocalDate to) {
        Instant fromInstant = from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : Instant.EPOCH;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : Instant.now();

        List<Map<String, Object>> rows = new ArrayList<>();
        List<RiskAssessmentEntity> assessments = employeeId != null
                ? riskAssessmentRepository.findByEmployeeIdOrderByEvaluatedAtDesc(employeeId)
                : riskAssessmentRepository.findAll().stream()
                .sorted(Comparator.comparing(RiskAssessmentEntity::getEvaluatedAt).reversed())
                .collect(Collectors.toList());

        for (RiskAssessmentEntity ra : assessments) {
            if (ra.getEvaluatedAt() == null) continue;
            if (ra.getEvaluatedAt().isBefore(fromInstant) || !ra.getEvaluatedAt().isBefore(toInstant)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("assessmentId", ra.getId());
            row.put("employeeId", ra.getEmployeeId());
            employeeRepository.findById(ra.getEmployeeId()).ifPresent(e -> {
                row.put("employeeNumber", e.getEmployeeNumber());
                row.put("fullName", e.getFullName());
            });
            row.put("ruleId", ra.getRuleId());
            row.put("ruleVersion", ra.getRuleVersion());
            row.put("severity", ra.getSeverity());
            row.put("evidence", ra.getEvidenceJson());
            row.put("evaluatedAt", ra.getEvaluatedAt());
            classificationRepository.findById(ra.getEmployeeId())
                    .ifPresent(c -> row.put("currentRiskLevel", c.getRiskLevel()));
            profileRepository.findById(ra.getEmployeeId()).ifPresent(p -> {
                row.put("attendancePctMandatory", p.getAttendancePctMandatory());
                row.put("latestAssessmentPct", p.getLatestAssessmentPct());
                row.put("consecutiveLowScores", p.getConsecutiveLowScores());
            });
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> buildEmployeeRow(Employee e, Instant periodStart, Instant periodEnd) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("employeeNumber", e.getEmployeeNumber());
        row.put("fullName", e.getFullName());
        row.put("department", e.getDepartment());
        classificationRepository.findById(e.getId()).ifPresent(c -> row.put("riskLevel", c.getRiskLevel()));
        profileRepository.findById(e.getId()).ifPresent(p -> {
            row.put("attendancePctMandatory", p.getAttendancePctMandatory());
            row.put("latestAssessmentPct", p.getLatestAssessmentPct());
            row.put("consecutiveLowScores", p.getConsecutiveLowScores());
        });

        List<InterventionEntity> interventions = interventionRepository.findByEmployeeId(e.getId());
        row.put("interventions", interventions.size());

        List<RiskAssessmentEntity> riskInPeriod = riskAssessmentRepository
                .findByEmployeeIdOrderByEvaluatedAtDesc(e.getId()).stream()
                .filter(ra -> ra.getEvaluatedAt() != null
                        && !ra.getEvaluatedAt().isBefore(periodStart)
                        && ra.getEvaluatedAt().isBefore(periodEnd))
                .collect(Collectors.toList());
        row.put("riskEvents", riskInPeriod.size());
        row.put("recentRiskEvents", riskInPeriod.stream().limit(5).map(ra -> {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("assessmentId", ra.getId());
            ev.put("ruleId", ra.getRuleId());
            ev.put("ruleVersion", ra.getRuleVersion());
            ev.put("severity", ra.getSeverity());
            ev.put("evaluatedAt", ra.getEvaluatedAt());
            return ev;
        }).collect(Collectors.toList()));

        row.put("interventionTrail", interventions.stream().map(i -> {
            Map<String, Object> iv = new LinkedHashMap<>();
            iv.put("interventionId", i.getId());
            iv.put("type", i.getType());
            iv.put("scheduledDate", i.getScheduledDate());
            iv.put("status", i.getStatus());
            iv.put("assignee", i.getAssignee());
            iv.put("riskAssessmentId", i.getRiskAssessmentId());
            outcomeRepository.findByInterventionId(i.getId()).ifPresent(o -> {
                iv.put("outcome", o.getOutcome());
                iv.put("effectiveness", o.getEffectiveness());
            });
            return iv;
        }).collect(Collectors.toList()));
        return row;
    }

    private Map<String, Object> interventionSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<InterventionEntity> all = interventionRepository.findAll();
        summary.put("total", all.size());
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long withOutcomes = 0;
        for (InterventionEntity i : all) {
            byStatus.merge(i.getStatus(), 1L, Long::sum);
            if (outcomeRepository.findByInterventionId(i.getId()).isPresent()) {
                withOutcomes++;
            }
        }
        summary.put("byStatus", byStatus);
        summary.put("withOutcomes", withOutcomes);
        return summary;
    }

    private Map<String, Object> reportSummary(ComplianceReportRun run, boolean includeDetail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("runId", run.getId());
        m.put("periodStart", run.getPeriodStart());
        m.put("periodEnd", run.getPeriodEnd());
        m.put("department", run.getDepartment() != null ? run.getDepartment() : "ALL");
        m.put("generatedAt", run.getGeneratedAt());
        try {
            Map<?, ?> report = objectMapper.readValue(run.getReportJson(), Map.class);
            m.put("reportType", report.get("reportType"));
            m.put("generatedBy", report.get("generatedBy"));
            m.put("summary", report.get("summary"));
            m.put("interventionSummary", report.get("interventionSummary"));
            m.put("ruleVersions", report.get("ruleVersions"));
            if (includeDetail) {
                m.put("employees", report.get("employees"));
            }
        } catch (Exception ignored) {
            m.put("parseError", true);
        }
        return m;
    }

    private Map<String, Object> summaryMap(List<Map<String, Object>> employees) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEmployees", employees.size());
        long atRisk = 0;
        Map<String, Long> riskByLevel = new LinkedHashMap<>();
        double attSum = 0;
        int attCount = 0;
        for (Map<String, Object> r : employees) {
            if (r.containsKey("riskLevel") && !"NONE".equals(String.valueOf(r.get("riskLevel")))) {
                atRisk++;
                riskByLevel.merge(String.valueOf(r.get("riskLevel")), 1L, Long::sum);
            }
            if (r.get("attendancePctMandatory") instanceof Number) {
                attSum += ((Number) r.get("attendancePctMandatory")).doubleValue();
                attCount++;
            }
        }
        summary.put("atRisk", atRisk);
        summary.put("riskByLevel", riskByLevel);
        if (attCount > 0) {
            summary.put("avgAttendancePctMandatory", Math.round(attSum / attCount * 10.0) / 10.0);
        }
        return summary;
    }

    private void logExport(Long reportId, String format) {
        logAudit("REPORT_EXPORT", "COMPLIANCE_REPORT", String.valueOf(reportId),
                Map.of("runId", reportId, "format", format, "exportedBy", actorLabel()));
    }

    private static String actorLabel() {
        return RoleContext.currentRole().name();
    }

    private static PdfPCell cell(String text, Font font) {
        Font f = font != null ? font : FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", f));
        cell.setPadding(4f);
        return cell;
    }

    private void logAudit(String action, String entityType, String entityId, Map<String, ?> details) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        try {
            entry.setDetailsJson(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            entry.setDetailsJson("{}");
        }
        auditLogRepository.save(entry);
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
