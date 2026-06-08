package com.corplearning.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/auth/me")
    public Map<String, Object> currentUser() {
        UserRole role = RoleContext.currentRole();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", role.name());
        body.put("canConfigureRules", role.has(Permission.RULE_CONFIGURE));
        body.put("canGenerateReports", role.has(Permission.REPORT_GENERATE));
        body.put("canExportReports", role.has(Permission.REPORT_EXPORT));
        body.put("canViewAuditTrail", role.has(Permission.AUDIT_VIEW));
        return body;
    }

    @GetMapping("/dashboard/lnd")
    public Map<String, Object> dashboard() {
        return reportingService.dashboardLnd();
    }

    @GetMapping("/dashboard/ingestion-errors")
    public List<Map<String, Object>> ingestionErrors() {
        return reportingService.listIngestionErrors();
    }

    @GetMapping("/reports")
    public List<Map<String, Object>> listReports() {
        return reportingService.listReports();
    }

    @GetMapping("/reports/{id}")
    public Map<String, Object> getReport(@PathVariable Long id) {
        return reportingService.getReport(id);
    }

    @PostMapping("/reports/compliance")
    public Map<String, Object> generateCompliance(@RequestBody Map<String, String> body) {
        return reportingService.getReport(generate(body).getId());
    }

    @PostMapping("/reports/corporate")
    public Map<String, Object> generateCorporate(@RequestBody Map<String, String> body) {
        return reportingService.getReport(generate(body).getId());
    }

    @GetMapping("/reports/{id}/export")
    public ResponseEntity<?> export(@PathVariable Long id, @RequestParam(defaultValue = "csv") String format) {
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = reportingService.exportPdf(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=corporate-learning-report-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        }
        String csv = reportingService.exportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=corporate-learning-report-" + id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/audit/risk-decisions")
    public List<Map<String, Object>> auditRiskDecisions(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return reportingService.auditRiskDecisions(employeeId, from, to);
    }

    private ComplianceReportRun generate(Map<String, String> body) {
        LocalDate start = LocalDate.parse(body.get("periodStart"));
        LocalDate end = LocalDate.parse(body.get("periodEnd"));
        String dept = body.get("department");
        return reportingService.generateReport(start, end, dept);
    }
}
