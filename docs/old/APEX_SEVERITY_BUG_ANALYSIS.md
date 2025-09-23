# APEX Severity Implementation Design

## Core Model Implementations

### 1. YamlRule.java - YAML Parsing Support
```java
@JsonProperty("severity")
private String severity;

public String getSeverity() {
    return severity;
}

public void setSeverity(String severity) {
    this.severity = severity;
}
```

### 2. Rule.java - Core Model with Severity
```java
private final String severity;

// Backward-compatible constructor
public Rule(String name, String condition, String message) {
    this(name, condition, message, "INFO");
}

// New constructor with severity
public Rule(String name, String condition, String message, String severity) {
    this.name = name;
    this.condition = condition;
    this.message = message;
    this.severity = severity != null ? severity : "INFO";
}

public String getSeverity() {
    return severity;
}
```

### 3. RuleResult.java - Result Model with Severity
```java
private final String severity;

// Updated factory methods
public static RuleResult match(String ruleName, String message, String severity) {
    return new RuleResult(ruleName, message, true, ResultType.MATCH, severity);
}

public static RuleResult noMatch(String ruleName, String message, String severity) {
    return new RuleResult(ruleName, message, false, ResultType.NO_MATCH, severity);
}

public static RuleResult error(String ruleName, String errorMessage, String severity) {
    return new RuleResult(ruleName, errorMessage, false, ResultType.ERROR, severity);
}

public String getSeverity() {
    return severity;
}
```

### 4. RuleBuilder.java - Programmatic Rule Creation
```java
private String severity = "INFO";

public RuleBuilder withSeverity(String severity) {
    this.severity = severity;
    return this;
}

public Rule build() {
    return new Rule(name, condition, message, severity);
}
```

## Processing Logic Implementations

### 5. YamlRuleFactory.java - YAML to Rule Conversion
```java
public Rule createRule(YamlRule yamlRule) {
    return new Rule(
        yamlRule.getName(),
        yamlRule.getCondition(),
        yamlRule.getMessage(),
        yamlRule.getSeverity() // Pass severity from YAML
    );
}
```

### 6. RulesEngine.java - Rule Execution with Severity
```java
public RuleResult executeRule(Rule rule, Object data) {
    try {
        boolean result = evaluateCondition(rule.getCondition(), data);
        String message = processMessage(rule.getMessage(), data);

        if (result) {
            return RuleResult.match(rule.getName(), message, rule.getSeverity());
        } else {
            return RuleResult.noMatch(rule.getName(), message, rule.getSeverity());
        }
    } catch (Exception e) {
        return RuleResult.error(rule.getName(), e.getMessage(), rule.getSeverity());
    }
}
```

### 7. RuleGroupSeverityAggregator.java - Group Severity Logic
```java
@Component
public class RuleGroupSeverityAggregator {
    private static final Map<String, Integer> SEVERITY_PRIORITY = Map.of(
        "ERROR", 3, "WARNING", 2, "INFO", 1
    );

    public String aggregateSeverity(List<RuleResult> results, boolean isAndOperator) {
        if (results.isEmpty()) return "INFO";

        return isAndOperator ?
            aggregateAndGroupSeverity(results) :
            aggregateOrGroupSeverity(results);
    }

    private String aggregateAndGroupSeverity(List<RuleResult> results) {
        List<RuleResult> failedRules = results.stream()
            .filter(r -> !r.isTriggered()).collect(Collectors.toList());

        return failedRules.isEmpty() ?
            getHighestSeverity(results) :
            getHighestSeverity(failedRules);
    }

    private String aggregateOrGroupSeverity(List<RuleResult> results) {
        return results.stream()
            .filter(RuleResult::isTriggered)
            .findFirst()
            .map(RuleResult::getSeverity)
            .orElse(getHighestSeverity(results));
    }

    private String getHighestSeverity(List<RuleResult> results) {
        return results.stream()
            .map(RuleResult::getSeverity)
            .max((s1, s2) -> Integer.compare(
                SEVERITY_PRIORITY.get(s1), SEVERITY_PRIORITY.get(s2)))
            .orElse("INFO");
    }
}
```

## API Layer Implementations

### 8. RuleEvaluationResponse.java - API Response with Severity
```java
public class RuleEvaluationResponse {
    private String severity;

    public RuleEvaluationResponse(String ruleName, String message, boolean triggered, String severity) {
        this.ruleName = ruleName;
        this.message = message;
        this.triggered = triggered;
        this.severity = severity;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
```

### 9. RuleEvaluationRequest.java - API Request with Severity
```java
public class RuleEvaluationRequest {
    private String severity;

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
```

### 10. RulesController.java - REST Endpoints with Severity
```java
@PostMapping("/evaluate")
public ResponseEntity<RuleEvaluationResponse> evaluateRule(@RequestBody RuleEvaluationRequest request) {
    RuleResult result = rulesEngine.executeRule(request.toRule(), request.getData());

    RuleEvaluationResponse response = new RuleEvaluationResponse(
        result.getRuleName(),
        result.getMessage(),
        result.isTriggered(),
        result.getSeverity()
    );

    return ResponseEntity.ok(response);
}
```

## Validation Implementations

### 11. SeverityConstants.java - Centralized Constants
```java
public final class SeverityConstants {
    public static final String ERROR = "ERROR";
    public static final String WARNING = "WARNING";
    public static final String INFO = "INFO";

    public static final Set<String> VALID_SEVERITIES = Set.of(ERROR, WARNING, INFO);
    public static final String DEFAULT_SEVERITY = INFO;

    public static final Map<String, Integer> SEVERITY_PRIORITY = Map.of(
        ERROR, 3, WARNING, 2, INFO, 1
    );

    private SeverityConstants() {
        // Utility class - prevent instantiation
    }
}
```

### 12. SeverityValidator.java - Validation Logic
```java
public final class SeverityValidator {
    private static final Logger logger = LoggerFactory.getLogger(SeverityValidator.class);

    public static void validateSeverity(String severity, String contextId) throws YamlConfigurationException {
        if (severity == null) {
            return; // Null is valid, defaults to INFO
        }

        String normalized = severity.trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new YamlConfigurationException(
                "Component '" + contextId + "' has empty severity. Must be ERROR, WARNING, or INFO");
        }

        if (!SeverityConstants.VALID_SEVERITIES.contains(normalized)) {
            throw new YamlConfigurationException(
                "Component '" + contextId + "' has invalid severity '" + severity +
                "'. Must be ERROR, WARNING, or INFO");
        }
    }

    public static String normalizeSeverity(String severity) {
        return severity == null ? SeverityConstants.DEFAULT_SEVERITY : severity.trim().toUpperCase();
    }

    private SeverityValidator() {
        // Utility class - prevent instantiation
    }
}
```

### 13. YamlConfigurationLoader.java - YAML Validation
```java
private void validateRules(YamlRuleConfiguration config) throws YamlConfigurationException {
    // Existing validation logic...

    // Add severity validation for rules
    if (config.getRules() != null) {
        for (YamlRule rule : config.getRules()) {
            SeverityValidator.validateSeverity(rule.getSeverity(), "Rule '" + rule.getId() + "'");
        }
    }

    // Add severity validation for enrichments
    if (config.getEnrichments() != null) {
        for (YamlEnrichment enrichment : config.getEnrichments()) {
            SeverityValidator.validateSeverity(enrichment.getSeverity(),
                "Enrichment '" + enrichment.getId() + "'");
        }
    }
}
```

## Test Implementations

### 14. SeverityValidationTest.java - Core Validation Tests
```java
@DisplayName("Severity Validation Tests")
class SeverityValidationTest extends DemoTestBase {

    @Test
    @DisplayName("Valid severity values should pass validation")
    void testValidSeverityValues() {
        assertDoesNotThrow(() -> SeverityValidator.validateSeverity("ERROR", "test-rule"));
        assertDoesNotThrow(() -> SeverityValidator.validateSeverity("WARNING", "test-rule"));
        assertDoesNotThrow(() -> SeverityValidator.validateSeverity("INFO", "test-rule"));
        assertDoesNotThrow(() -> SeverityValidator.validateSeverity(null, "test-rule"));
    }

    @Test
    @DisplayName("Invalid severity values should throw exception")
    void testInvalidSeverityValues() {
        assertThrows(YamlConfigurationException.class,
            () -> SeverityValidator.validateSeverity("CRITICAL", "test-rule"));
        assertThrows(YamlConfigurationException.class,
            () -> SeverityValidator.validateSeverity("", "test-rule"));
        assertThrows(YamlConfigurationException.class,
            () -> SeverityValidator.validateSeverity("   ", "test-rule"));
    }

    @Test
    @DisplayName("Severity normalization should work correctly")
    void testSeverityNormalization() {
        assertEquals("ERROR", SeverityValidator.normalizeSeverity("error"));
        assertEquals("WARNING", SeverityValidator.normalizeSeverity(" warning "));
        assertEquals("INFO", SeverityValidator.normalizeSeverity(null));
    }
}
```

### 15. SeverityAggregationTest.java - Rule Group Tests
```java
@DisplayName("Rule Group Severity Aggregation Tests")
class SeverityAggregationTest extends DemoTestBase {

    @Test
    @DisplayName("AND group with mixed severities should use highest severity of failed rules")
    void testAndGroupMixedSeveritiesFailedRules() {
        // Create rules with different severities
        Rule errorRule = new Rule("error-rule", "false", "Error message", "ERROR");
        Rule warningRule = new Rule("warning-rule", "true", "Warning message", "WARNING");
        Rule infoRule = new Rule("info-rule", "true", "Info message", "INFO");

        // Execute rule group with AND logic
        RuleGroup group = new RuleGroup("test-group", List.of(errorRule, warningRule, infoRule), true);
        RuleGroupEvaluationResult result = group.evaluateWithDetails(createTestContext());

        // Verify highest severity of failed rules is used
        assertEquals("ERROR", result.getAggregatedSeverity());
        assertFalse(result.getGroupResult());
    }

    @Test
    @DisplayName("OR group should use severity of first matching rule")
    void testOrGroupFirstMatchSeverity() {
        Rule warningRule = new Rule("warning-rule", "true", "Warning message", "WARNING");
        Rule errorRule = new Rule("error-rule", "true", "Error message", "ERROR");

        RuleGroup group = new RuleGroup("test-group", List.of(warningRule, errorRule), false);
        RuleGroupEvaluationResult result = group.evaluateWithDetails(createTestContext());

        assertEquals("WARNING", result.getAggregatedSeverity());
        assertTrue(result.getGroupResult());
    }

    @Test
    @DisplayName("Empty rule group should default to INFO severity")
    void testEmptyRuleGroupDefaultSeverity() {
        RuleGroup group = new RuleGroup("empty-group", List.of(), true);
        RuleGroupEvaluationResult result = group.evaluateWithDetails(createTestContext());

        assertEquals("INFO", result.getAggregatedSeverity());
    }
}
```

### 16. SeverityApiIntegrationTest.java - API Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeverityApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRuleEvaluationRequestWithSeverity() {
        RuleEvaluationRequest request = new RuleEvaluationRequest();
        request.setSeverity("ERROR");
        request.setCondition("true");
        request.setMessage("Test message");

        ResponseEntity<RuleEvaluationResponse> response = restTemplate.postForEntity(
            "/api/rules/evaluate", request, RuleEvaluationResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ERROR", response.getBody().getSeverity());
    }

    @Test
    void testInvalidSeverityInApiRequest() {
        RuleEvaluationRequest request = new RuleEvaluationRequest();
        request.setSeverity("CRITICAL");

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/rules/evaluate", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
```

## YAML Configuration Examples

### 17. severity-comprehensive-test.yaml - All Severity Levels
```yaml
metadata:
  name: "Comprehensive Severity Test"
  version: "1.0"

rules:
  - id: "error-rule"
    name: "Critical Error Rule"
    condition: "#amount > 1000"
    message: "Amount {{#amount}} exceeds critical threshold"
    severity: "ERROR"
    priority: 1

  - id: "warning-rule"
    name: "Warning Rule"
    condition: "#amount > 500"
    message: "Amount {{#amount}} exceeds warning threshold"
    severity: "WARNING"
    priority: 2

  - id: "info-rule"
    name: "Information Rule"
    condition: "#amount > 100"
    message: "Amount {{#amount}} is above normal range"
    severity: "INFO"
    priority: 3

rule-groups:
  - id: "mixed-severity-group"
    name: "Mixed Severity Group"
    operator: "AND"
    rules:
      - "error-rule"
      - "warning-rule"
      - "info-rule"
```

### 18. severity-aggregation-and-group.yaml - AND Group Testing
```yaml
metadata:
  name: "AND Group Severity Aggregation"
  version: "1.0"

rules:
  - id: "failing-error-rule"
    name: "Failing Error Rule"
    condition: "false"
    message: "This rule always fails"
    severity: "ERROR"

  - id: "passing-warning-rule"
    name: "Passing Warning Rule"
    condition: "true"
    message: "This rule always passes"
    severity: "WARNING"

rule-groups:
  - id: "and-group-test"
    name: "AND Group Test"
    operator: "AND"
    rules:
      - "failing-error-rule"
      - "passing-warning-rule"
```

### 19. severity-aggregation-or-group.yaml - OR Group Testing
```yaml
metadata:
  name: "OR Group Severity Aggregation"
  version: "1.0"

rules:
  - id: "first-warning-rule"
    name: "First Warning Rule"
    condition: "true"
    message: "First matching rule"
    severity: "WARNING"

  - id: "second-error-rule"
    name: "Second Error Rule"
    condition: "true"
    message: "Second matching rule"
    severity: "ERROR"

rule-groups:
  - id: "or-group-test"
    name: "OR Group Test"
    operator: "OR"
    rules:
      - "first-warning-rule"
      - "second-error-rule"
```

## Integration Patterns

### 20. RuleGroupEvaluationResult.java - Enhanced Evaluation Model
```java
public class RuleGroupEvaluationResult {
    private final boolean groupResult;
    private final List<RuleResult> individualResults;
    private final String aggregatedSeverity;
    private final RulePerformanceMetrics performanceMetrics;

    public RuleGroupEvaluationResult(boolean groupResult,
                                   List<RuleResult> individualResults,
                                   String aggregatedSeverity,
                                   RulePerformanceMetrics performanceMetrics) {
        this.groupResult = groupResult;
        this.individualResults = new ArrayList<>(individualResults);
        this.aggregatedSeverity = aggregatedSeverity;
        this.performanceMetrics = performanceMetrics;
    }

    public boolean getGroupResult() {
        return groupResult;
    }

    public List<RuleResult> getIndividualResults() {
        return new ArrayList<>(individualResults);
    }

    public String getAggregatedSeverity() {
        return aggregatedSeverity;
    }

    public RulePerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
}
```

### 21. Enhanced RuleGroup.java - Detailed Evaluation
```java
public class RuleGroup {
    private final List<RuleResult> individualRuleResults = new ArrayList<>();
    private final RuleGroupSeverityAggregator severityAggregator;

    public RuleGroupEvaluationResult evaluateWithDetails(StandardEvaluationContext context) {
        long startTime = System.nanoTime();
        individualRuleResults.clear();

        boolean result = isAndOperator ?
            evaluateSequentialWithDetails(context) :
            evaluateParallelWithDetails(context);

        String aggregatedSeverity = severityAggregator.aggregateSeverity(
            individualRuleResults, isAndOperator);

        long endTime = System.nanoTime();
        RulePerformanceMetrics metrics = new RulePerformanceMetrics(
            endTime - startTime, individualRuleResults.size());

        return new RuleGroupEvaluationResult(result, individualRuleResults,
                                           aggregatedSeverity, metrics);
    }

    private boolean evaluateSequentialWithDetails(StandardEvaluationContext context) {
        for (Rule rule : rules) {
            RuleResult ruleResult = executeRuleWithSeverity(rule, context);
            individualRuleResults.add(ruleResult);

            if (isAndOperator && !ruleResult.isTriggered()) {
                return false; // Short-circuit for AND
            }
            if (!isAndOperator && ruleResult.isTriggered()) {
                return true; // Short-circuit for OR
            }
        }
        return isAndOperator; // AND: all passed, OR: none passed
    }

    private boolean evaluateParallelWithDetails(StandardEvaluationContext context) {
        List<CompletableFuture<RuleResult>> futures = rules.stream()
            .map(rule -> CompletableFuture.supplyAsync(() ->
                executeRuleWithSeverity(rule, context)))
            .collect(Collectors.toList());

        List<RuleResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        individualRuleResults.addAll(results);

        return isAndOperator ?
            results.stream().allMatch(RuleResult::isTriggered) :
            results.stream().anyMatch(RuleResult::isTriggered);
    }

    private RuleResult executeRuleWithSeverity(Rule rule, StandardEvaluationContext context) {
        try {
            boolean result = evaluateCondition(rule.getCondition(), context);
            String message = processMessage(rule.getMessage(), context);

            if (result) {
                return RuleResult.match(rule.getName(), message, rule.getSeverity());
            } else {
                return RuleResult.noMatch(rule.getName(), message, rule.getSeverity());
            }
        } catch (Exception e) {
            return RuleResult.error(rule.getName(), e.getMessage(), rule.getSeverity());
        }
    }

    public List<RuleResult> getIndividualRuleResults() {
        return new ArrayList<>(individualRuleResults);
    }
}
```

## Performance Monitoring

### 22. RulePerformanceMetrics.java - Performance Tracking
```java
public class RulePerformanceMetrics {
    private final long executionTimeNanos;
    private final int rulesEvaluated;
    private final long memoryUsedBytes;
    private final Instant timestamp;

    public RulePerformanceMetrics(long executionTimeNanos, int rulesEvaluated) {
        this.executionTimeNanos = executionTimeNanos;
        this.rulesEvaluated = rulesEvaluated;
        this.memoryUsedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        this.timestamp = Instant.now();
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public double getExecutionTimeMillis() {
        return executionTimeNanos / 1_000_000.0;
    }

    public int getRulesEvaluated() {
        return rulesEvaluated;
    }

    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getAverageRuleExecutionTime() {
        return rulesEvaluated > 0 ? getExecutionTimeMillis() / rulesEvaluated : 0.0;
    }
}
```

### 23. SeverityPerformanceTest.java - Performance Validation
```java
@DisplayName("Severity Performance Tests")
class SeverityPerformanceTest extends DemoTestBase {

    @Test
    @DisplayName("Severity processing should have minimal performance impact")
    void testSeverityPerformanceImpact() {
        // Benchmark without severity
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            Rule rule = new Rule("perf-test-" + i, "true", "Test message");
            RuleResult result = rulesEngine.executeRule(rule, createTestData());
            assertNotNull(result);
        }
        long withoutSeverityTime = System.nanoTime() - startTime;

        // Benchmark with severity
        startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            Rule rule = new Rule("perf-test-" + i, "true", "Test message", "ERROR");
            RuleResult result = rulesEngine.executeRule(rule, createTestData());
            assertNotNull(result.getSeverity());
        }
        long withSeverityTime = System.nanoTime() - startTime;

        // Verify performance impact is less than 5%
        double performanceImpact = ((double) withSeverityTime - withoutSeverityTime) / withoutSeverityTime;
        assertTrue(performanceImpact < 0.05,
            "Severity processing impact should be less than 5%, but was: " + (performanceImpact * 100) + "%");
    }

    @Test
    @DisplayName("Large scale rule group evaluation with mixed severities")
    void testLargeScaleRuleGroupPerformance() {
        // Create 100 rules with mixed severities
        List<Rule> rules = IntStream.range(0, 100)
            .mapToObj(i -> new Rule("rule-" + i, "true", "Message " + i,
                getSeverityForIndex(i)))
            .collect(Collectors.toList());

        RuleGroup largeGroup = new RuleGroup("large-group", rules, true);

        long startTime = System.nanoTime();
        RuleGroupEvaluationResult result = largeGroup.evaluateWithDetails(createTestContext());
        long executionTime = System.nanoTime() - startTime;

        assertNotNull(result.getAggregatedSeverity());
        assertEquals(100, result.getIndividualResults().size());

        // Verify execution time is reasonable (less than 1 second for 100 rules)
        assertTrue(executionTime < 1_000_000_000L,
            "Large rule group execution should complete in less than 1 second");
    }

    private String getSeverityForIndex(int index) {
        return switch (index % 3) {
            case 0 -> "ERROR";
            case 1 -> "WARNING";
            default -> "INFO";
        };
    }
}
```

## Error Handling Implementations

### 24. severity-invalid-test.yaml - Invalid Severity Testing
```yaml
metadata:
  name: "Invalid Severity Test"
  version: "1.0"

rules:
  - id: "invalid-severity-rule"
    name: "Invalid Severity Rule"
    condition: "true"
    message: "Test message"
    severity: "CRITICAL"  # Invalid - should cause error

  - id: "empty-severity-rule"
    name: "Empty Severity Rule"
    condition: "true"
    message: "Test message"
    severity: ""  # Invalid - empty string

  - id: "null-severity-rule"
    name: "Null Severity Rule"
    condition: "true"
    message: "Test message"
    # severity: null - should default to INFO
```

### 25. SeverityErrorHandlingTest.java - Error Validation
```java
@DisplayName("Severity Error Handling Tests")
class SeverityErrorHandlingTest extends DemoTestBase {

    @Test
    @DisplayName("Invalid severity in YAML should throw configuration exception")
    void testInvalidSeverityInYaml() {
        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> yamlLoader.loadFromFile("test-configs/severity-invalid-test.yaml")
        );

        assertTrue(exception.getMessage().contains("invalid severity 'CRITICAL'"));
        assertTrue(exception.getMessage().contains("Must be ERROR, WARNING, or INFO"));
    }

    @Test
    @DisplayName("Empty severity should throw configuration exception")
    void testEmptySeverityHandling() {
        assertThrows(YamlConfigurationException.class, () -> {
            SeverityValidator.validateSeverity("", "test-rule");
        });

        assertThrows(YamlConfigurationException.class, () -> {
            SeverityValidator.validateSeverity("   ", "test-rule");
        });
    }

    @Test
    @DisplayName("Null severity should default to INFO")
    void testNullSeverityHandling() {
        Rule rule = new Rule("test", "true", "message", null);
        assertEquals("INFO", rule.getSeverity());

        // Null severity validation should not throw exception
        assertDoesNotThrow(() -> {
            SeverityValidator.validateSeverity(null, "test-rule");
        });
    }

    @Test
    @DisplayName("Case insensitive severity normalization")
    void testCaseInsensitiveSeverity() {
        assertEquals("ERROR", SeverityValidator.normalizeSeverity("error"));
        assertEquals("WARNING", SeverityValidator.normalizeSeverity("Warning"));
        assertEquals("INFO", SeverityValidator.normalizeSeverity("info"));
        assertEquals("ERROR", SeverityValidator.normalizeSeverity(" ERROR "));
    }
}
```

## Summary

This document provides detailed design implementations for APEX severity support including:

1. **Core Models** - YamlRule, Rule, RuleResult with severity fields
2. **Processing Logic** - YamlRuleFactory, RulesEngine with severity flow
3. **API Layer** - REST endpoints and DTOs with severity support
4. **Validation** - SeverityValidator and SeverityConstants for centralized validation
5. **Testing** - Comprehensive test suites for all severity scenarios
6. **YAML Configurations** - Test files with valid and invalid severity examples
7. **Rule Group Aggregation** - Advanced severity aggregation logic for AND/OR groups
8. **Performance Monitoring** - Performance metrics and benchmarking
9. **Error Handling** - Robust validation and error handling for invalid severities

All implementations follow SOLID principles, maintain backward compatibility, and include comprehensive error handling and testing.




















