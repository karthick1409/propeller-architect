package com.corplearning.common;

import com.corplearning.common.InterventionEntity;
import com.corplearning.common.InterventionOutcomeEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/interventions")
public class InterventionController {

    private final InterventionService interventionService;

    public InterventionController(InterventionService interventionService) {
        this.interventionService = interventionService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return interventionService.listAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Map<String, Object>> byEmployee(@PathVariable Long employeeId) {
        return interventionService.listByEmployee(employeeId);
    }

    @PostMapping
    public InterventionEntity assign(@RequestBody Map<String, Object> body) {
        return interventionService.assign(
                Long.valueOf(body.get("employeeId").toString()),
                body.get("type").toString(),
                String.valueOf(body.getOrDefault("assignee", "")),
                body.containsKey("scheduledDate") ? LocalDate.parse(body.get("scheduledDate").toString()) : null,
                body.containsKey("riskAssessmentId") ? Long.valueOf(body.get("riskAssessmentId").toString()) : null,
                String.valueOf(body.getOrDefault("notes", ""))
        );
    }

    @PatchMapping("/{id}/status")
    public InterventionEntity updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return interventionService.updateStatus(id, body.get("status"), body.get("notes"));
    }

    @PostMapping("/{id}/outcome")
    public InterventionOutcomeEntity outcome(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return interventionService.recordOutcome(id, body.get("outcome"), body.get("summary"));
    }
}

