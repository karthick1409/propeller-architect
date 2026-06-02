package com.corplearning.common;

import com.corplearning.common.ComplianceReportRun;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard/lnd")
    public Map<String, Object> dashboard() {
        return reportingService.dashboardLnd();
    }

    @PostMapping("/reports/compliance")
    public ComplianceReportRun generate(@RequestBody Map<String, String> body) {
        LocalDate start = LocalDate.parse(body.get("periodStart"));
        LocalDate end = LocalDate.parse(body.get("periodEnd"));
        String dept = body.get("department");
        return reportingService.generateReport(start, end, dept);
    }

    @GetMapping("/reports/{id}/export")
    public ResponseEntity<String> export(@PathVariable Long id, @RequestParam(defaultValue = "csv") String format) {
        String csv = reportingService.exportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compliance-report-" + id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}

