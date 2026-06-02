package com.corplearning.common;

import com.corplearning.common.RiskRuleEntity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator(new com.fasterxml.jackson.databind.ObjectMapper());

    @Test
    void attendanceRuleFiresBelowThreshold() {
        RiskRuleEntity rule = new RiskRuleEntity();
        rule.setRuleId("R-ATT-01");
        rule.setDefinitionJson("{\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"attendance_percentage\",\"scope\":\"mandatory_courses\",\"operator\":\"less_than\",\"value\":75}]}}");
        ProfileService.ProfileMetrics metrics = new ProfileService.ProfileMetrics(80, 74, 90, 0, false);
        assertTrue(evaluator.evaluate(rule, metrics).isPresent());
    }

    @Test
    void attendanceBoundaryDoesNotFire() {
        RiskRuleEntity rule = new RiskRuleEntity();
        rule.setRuleId("R-ATT-01");
        rule.setDefinitionJson("{\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"attendance_percentage\",\"scope\":\"mandatory_courses\",\"operator\":\"less_than\",\"value\":75}]}}");
        ProfileService.ProfileMetrics metrics = new ProfileService.ProfileMetrics(80, 75, 90, 0, false);
        assertTrue(evaluator.evaluate(rule, metrics).isEmpty());
    }

    @Test
    void consecutiveLowScoreRuleFires() {
        RiskRuleEntity rule = new RiskRuleEntity();
        rule.setDefinitionJson("{\"conditions\":{\"operator\":\"AND\",\"criteria\":[{\"metric\":\"consecutive_low_assessments\",\"operator\":\"greater_than\",\"value\":1}]}}");
        ProfileService.ProfileMetrics metrics = new ProfileService.ProfileMetrics(100, 100, 55, 2, false);
        Optional<java.util.Map<String, Object>> result = evaluator.evaluate(rule, metrics);
        assertTrue(result.isPresent());
    }
}
