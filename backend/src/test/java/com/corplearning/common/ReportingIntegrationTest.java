package com.corplearning.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReportingIntegrationTest {

    @Autowired
    private ReportingService reportingService;

    @Test
    void generateCorporateReportIncludesTraceabilityMetadata() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        ComplianceReportRun run = reportingService.generateReport(start, end, null);
        assertNotNull(run.getId());

        Map<String, Object> summary = reportingService.getReport(run.getId());
        assertEquals(run.getId(), summary.get("runId"));
        assertEquals("CORPORATE_COMPLIANCE", summary.get("reportType"));
        assertNotNull(summary.get("generatedAt"));
        assertNotNull(summary.get("summary"));
        assertNotNull(summary.get("interventionSummary"));
        assertNotNull(summary.get("ruleVersions"));

        @SuppressWarnings("unchecked")
        Map<String, Object> reportSummary = (Map<String, Object>) summary.get("summary");
        assertTrue(((Number) reportSummary.get("totalEmployees")).intValue() > 0);

        String csv = reportingService.exportCsv(run.getId());
        assertTrue(csv.contains("runId"));
        assertTrue(csv.contains("interventionId"));

        byte[] pdf = reportingService.exportPdf(run.getId());
        assertTrue(pdf.length > 100);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
    }

    @Test
    void auditTrailReturnsRiskDecisions() {
        reportingService.auditRiskDecisions(null, LocalDate.of(2020, 1, 1), LocalDate.now());
        List<Map<String, Object>> audit = reportingService.auditRiskDecisions(null, null, null);
        assertNotNull(audit);
    }
}
