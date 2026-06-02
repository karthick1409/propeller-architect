package com.corplearning.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingest")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/attendance")
    public ResponseEntity<Map<String, Object>> attendance(@RequestBody List<Map<String, Object>> records) {
        return ResponseEntity.accepted().body(ingestionService.ingestAttendance(records));
    }

    @PostMapping("/assessments")
    public ResponseEntity<Map<String, Object>> assessments(@RequestBody List<Map<String, Object>> records) {
        return ResponseEntity.accepted().body(ingestionService.ingestAssessments(records));
    }

    @PostMapping("/milestones")
    public ResponseEntity<Map<String, Object>> milestones(@RequestBody List<Map<String, Object>> records) {
        return ResponseEntity.accepted().body(ingestionService.ingestMilestones(records));
    }

    @PostMapping("/employees/sync")
    public ResponseEntity<Map<String, Object>> syncEmployees(@RequestBody List<Map<String, Object>> records) {
        return ResponseEntity.ok(ingestionService.syncEmployees(records));
    }
}

