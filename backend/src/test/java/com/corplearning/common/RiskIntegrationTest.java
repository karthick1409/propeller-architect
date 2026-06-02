package com.corplearning.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RiskIntegrationTest {

    @Autowired
    private RiskRuleRepository ruleRepository;
    @Autowired
    private RuleEvaluator ruleEvaluator;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RiskEvaluationService riskEvaluationService;

    @Test
    void seededRulesFireForAtRiskEmployees() {
        Employee e2 = employeeRepository.findByEmployeeNumber("EMP-RISK-ATT-01").orElseThrow();
        ProfileService.ProfileMetrics metrics = profileService.metricsFor(e2.getId());
        assertTrue(metrics.getAttendancePctMandatory() < 75);

        List<RiskRuleEntity> rules = ruleRepository.findByStatus("ACTIVE");
        assertFalse(rules.isEmpty());

        long fireCount = rules.stream()
                .filter(r -> ruleEvaluator.evaluate(r, metrics).isPresent())
                .count();
        assertTrue(fireCount > 0, "Expected at least one DB rule to fire for low attendance employee");
    }

    @Test
    void evaluateAllPopulatesAtRiskQueue() {
        var result = riskEvaluationService.evaluateAll();
        assertTrue((Integer) result.get("atRiskCount") > 0);
        assertFalse(riskEvaluationService.listAtRisk().isEmpty());
    }
}
