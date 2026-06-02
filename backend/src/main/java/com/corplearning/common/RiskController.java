package com.corplearning.common;

import com.corplearning.common.RiskRuleEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class RiskController {

    private final RiskEvaluationService evaluationService;
    private final RiskRuleService ruleService;

    public RiskController(RiskEvaluationService evaluationService, RiskRuleService ruleService) {
        this.evaluationService = evaluationService;
        this.ruleService = ruleService;
    }

    @GetMapping("/rules")
    public List<RiskRuleEntity> listRules() {
        return ruleService.listAll();
    }

    @PostMapping("/rules")
    public RiskRuleEntity createRule(@RequestBody Map<String, String> body) {
        return ruleService.saveDraft(
                body.get("ruleId"),
                body.get("name"),
                body.getOrDefault("severity", "MEDIUM"),
                body.get("definitionJson")
        );
    }

    @PostMapping("/rules/{id}/activate")
    public RiskRuleEntity activate(@PathVariable Long id) {
        return ruleService.activate(id);
    }

    @PostMapping("/rules/dry-run")
    public List<Map<String, Object>> dryRun(@RequestBody Map<String, Object> body) {
        String def = (String) body.get("definitionJson");
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("employeeIds");
        List<Long> employeeIds = ids.stream().map(Number::longValue).collect(Collectors.toList());
        return evaluationService.dryRun(def, employeeIds);
    }

    @PostMapping("/risk/evaluate")
    public Map<String, Object> evaluate() {
        return evaluationService.evaluateAll();
    }

    @GetMapping("/risk/at-risk")
    public List<Map<String, Object>> atRisk() {
        return evaluationService.listAtRisk();
    }
}

