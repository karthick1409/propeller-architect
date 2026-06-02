package com.corplearning.common;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
class RiskEvaluationService {

    private final EmployeeRepository employeeRepository;
    private final RiskRuleRepository ruleRepository;
    private final RiskAssessmentRepository assessmentRepository;
    private final AtRiskClassificationRepository classificationRepository;
    private final ProfileService profileService;
    private final RuleEvaluator ruleEvaluator;
    private final ObjectMapper objectMapper;

    public RiskEvaluationService(EmployeeRepository employeeRepository,
                                 RiskRuleRepository ruleRepository,
                                 RiskAssessmentRepository assessmentRepository,
                                 AtRiskClassificationRepository classificationRepository,
                                 ProfileService profileService,
                                 RuleEvaluator ruleEvaluator,
                                 ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.ruleRepository = ruleRepository;
        this.assessmentRepository = assessmentRepository;
        this.classificationRepository = classificationRepository;
        this.profileService = profileService;
        this.ruleEvaluator = ruleEvaluator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> evaluateAll() {
        List<RiskRuleEntity> activeRules = ruleRepository.findByStatus("ACTIVE");
        int assessed = 0;
        int flagged = 0;
        for (Employee employee : employeeRepository.findByStatus(EmployeeStatus.ACTIVE)) {
            evaluateEmployee(employee.getId(), activeRules, true);
            assessed++;
            if (classificationRepository.findById(employee.getId())
                    .map(c -> !"NONE".equals(c.getRiskLevel()))
                    .orElse(false)) {
                flagged++;
            }
        }
        return Map.of("employeesEvaluated", assessed, "atRiskCount", flagged, "rulesApplied", activeRules.size());
    }

    @Transactional
    public List<Map<String, Object>> dryRun(String ruleDefinitionJson, List<Long> employeeIds) {
        RiskRuleEntity temp = new RiskRuleEntity();
        temp.setRuleId("DRY-RUN");
        temp.setVersion(0);
        temp.setSeverity(extractSeverity(ruleDefinitionJson));
        temp.setDefinitionJson(ruleDefinitionJson);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Long id : employeeIds) {
            ProfileService.ProfileMetrics metrics = profileService.metricsFor(id);
            Optional<Map<String, Object>> fire = ruleEvaluator.evaluate(temp, metrics);
            fire.ifPresent(evidence -> results.add(Map.of(
                    "employeeId", id,
                    "wouldFire", true,
                    "evidence", evidence
            )));
        }
        return results;
    }

    private void evaluateEmployee(Long employeeId, List<RiskRuleEntity> rules, boolean persist) {
        ProfileService.ProfileMetrics metrics = profileService.metricsFor(employeeId);
        List<String> firedRules = new ArrayList<>();
        RiskLevel maxLevel = RiskLevel.NONE;

        assessmentRepository.findByEmployeeIdOrderByEvaluatedAtDesc(employeeId);

        for (RiskRuleEntity rule : rules) {
            Optional<Map<String, Object>> evidence = ruleEvaluator.evaluate(rule, metrics);
            if (evidence.isPresent()) {
                firedRules.add(rule.getRuleId());
                RiskLevel level = RiskLevel.fromSeverity(rule.getSeverity());
                maxLevel = RiskLevel.max(maxLevel, level);
                if (persist) {
                    RiskAssessmentEntity ra = new RiskAssessmentEntity();
                    ra.setEmployeeId(employeeId);
                    ra.setRuleId(rule.getRuleId());
                    ra.setRuleVersion(rule.getVersion());
                    ra.setSeverity(rule.getSeverity());
                    try {
                        ra.setEvidenceJson(objectMapper.writeValueAsString(evidence.get()));
                    } catch (Exception ignored) {}
                    ra.setEvaluatedAt(Instant.now());
                    assessmentRepository.save(ra);
                }
            }
        }

        if (persist) {
            AtRiskClassification c = classificationRepository.findById(employeeId)
                    .orElseGet(AtRiskClassification::new);
            c.setEmployeeId(employeeId);
            c.setRiskLevel(maxLevel.name());
            try {
                c.setRuleIdsJson(objectMapper.writeValueAsString(firedRules));
            } catch (Exception e) {
                c.setRuleIdsJson("[]");
            }
            c.setEvaluatedAt(Instant.now());
            classificationRepository.save(c);
        }
    }

    public List<Map<String, Object>> listAtRisk() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AtRiskClassification c : classificationRepository.findByRiskLevelNot("NONE")) {
            employeeRepository.findById(c.getEmployeeId()).ifPresent(e -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("employeeId", e.getId());
                row.put("employeeNumber", e.getEmployeeNumber());
                row.put("fullName", e.getFullName());
                row.put("department", e.getDepartment());
                row.put("riskLevel", c.getRiskLevel());
                row.put("ruleIds", c.getRuleIdsJson());
                row.put("evaluatedAt", c.getEvaluatedAt());
                list.add(row);
            });
        }
        list.sort((a, b) -> Integer.compare(rank((String) b.get("riskLevel")), rank((String) a.get("riskLevel"))));
        return list;
    }

    private int rank(String level) {
        if ("CRITICAL".equals(level)) return 4;
        if ("HIGH".equals(level)) return 3;
        if ("MEDIUM".equals(level)) return 2;
        if ("LOW".equals(level)) return 1;
        return 0;
    }

    private String extractSeverity(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.has("severity") ? node.get("severity").asText("MEDIUM") : "MEDIUM";
        } catch (Exception e) {
            return "MEDIUM";
        }
    }
}

@Service
class RuleEvaluator {

    private final ObjectMapper objectMapper;

    RuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Optional<Map<String, Object>> evaluate(RiskRuleEntity rule, ProfileService.ProfileMetrics metrics) {
        try {
            JsonNode root = objectMapper.readTree(rule.getDefinitionJson());
            JsonNode conditions = root.has("conditions") ? root.get("conditions") : root;
            boolean result = evaluateNode(conditions, metrics);
            if (result) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("ruleId", rule.getRuleId());
                evidence.put("attendancePct30d", metrics.getAttendancePct30d());
                evidence.put("attendancePctMandatory", metrics.getAttendancePctMandatory());
                evidence.put("latestAssessmentPct", metrics.getLatestAssessmentPct());
                evidence.put("consecutiveLowScores", metrics.getConsecutiveLowScores());
                evidence.put("milestoneGap", metrics.isMilestoneGap());
                return Optional.of(evidence);
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private boolean evaluateNode(JsonNode node, ProfileService.ProfileMetrics metrics) {
        if (node.has("operator") && node.has("criteria")) {
            String op = node.get("operator").asText("AND");
            boolean and = "AND".equalsIgnoreCase(op);
            boolean result = and;
            for (JsonNode criterion : node.get("criteria")) {
                boolean r = evaluateCriterion(criterion, metrics);
                if (and) result = result && r;
                else result = result || r;
            }
            return result;
        }
        return evaluateCriterion(node, metrics);
    }

    private boolean evaluateCriterion(JsonNode c, ProfileService.ProfileMetrics metrics) {
        String metric = c.get("metric").asText();
        String operator = c.get("operator").asText();
        double value = c.get("value").asDouble();
        double actual;
        switch (metric) {
            case "attendance_percentage":
                actual = "mandatory_courses".equals(c.path("scope").asText())
                        ? metrics.getAttendancePctMandatory() : metrics.getAttendancePct30d();
                break;
            case "latest_assessment_percentage":
                actual = metrics.getLatestAssessmentPct();
                break;
            case "consecutive_low_assessments":
                actual = metrics.getConsecutiveLowScores();
                break;
            case "competency_milestone_gap":
                actual = metrics.isMilestoneGap() ? 1 : 0;
                break;
            default:
                actual = 0;
        }
        switch (operator) {
            case "less_than": return actual < value;
            case "less_or_equal": return actual <= value;
            case "greater_than": return actual > value;
            case "equals": return actual == value;
            default: return false;
        }
    }
}

@Service
class RiskRuleService {

    private final RiskRuleRepository ruleRepository;

    RiskRuleService(RiskRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public RiskRuleEntity saveDraft(String ruleId, String name, String severity, String definitionJson) {
        int version = ruleRepository.findTopByRuleIdOrderByVersionDesc(ruleId)
                .map(r -> r.getVersion() + 1).orElse(1);
        RiskRuleEntity rule = new RiskRuleEntity();
        rule.setRuleId(ruleId);
        rule.setVersion(version);
        rule.setName(name);
        rule.setSeverity(severity);
        rule.setStatus("DRAFT");
        rule.setDefinitionJson(definitionJson);
        return ruleRepository.save(rule);
    }

    @Transactional
    public RiskRuleEntity activate(Long id) {
        RiskRuleEntity rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        ruleRepository.findByStatus("ACTIVE").stream()
                .filter(r -> r.getRuleId().equals(rule.getRuleId()))
                .forEach(r -> { r.setStatus("ARCHIVED"); ruleRepository.save(r); });
        rule.setStatus("ACTIVE");
        rule.setActivatedAt(Instant.now());
        return ruleRepository.save(rule);
    }

    public List<RiskRuleEntity> listAll() {
        return ruleRepository.findAll();
    }
}

