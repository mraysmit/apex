# Engine Executor Layer Testing Methodology

## Overview

This document outlines the comprehensive testing approach developed for the Engine Executor Layer in the Apex Rules Engine. The executor layer represents the most complex component of the system, responsible for orchestrating rule execution across six different patterns. This methodology ensures robust, maintainable, and comprehensive test coverage.

## Testing Philosophy

### Integration-First Approach

Rather than relying heavily on mocking, we adopted an **integration-first testing strategy** that:

- Uses real service instances (`RuleEngineService`, `ExpressionEvaluatorService`)
- Tests actual behavior rather than implementation details
- Focuses on end-to-end functionality within the executor layer
- Provides confidence in real-world usage scenarios

### Rationale for Integration Testing

```java
@BeforeEach
void setUp() {
    // Create real service instances for integration testing
    evaluatorService = new ExpressionEvaluatorService();
    ruleEngineService = new RuleEngineService(evaluatorService);
    
    // Create all executor instances with real dependencies
    ruleChainExecutor = new RuleChainExecutor(ruleEngineService, evaluatorService);
    conditionalExecutor = new ConditionalChainingExecutor(ruleEngineService, evaluatorService);
    // ... other executors
}
```

**Benefits:**
- **Real behavior validation**: Tests actual service interactions
- **Reduced brittleness**: Less dependent on internal implementation changes
- **Simplified maintenance**: Fewer mocks to maintain and update
- **Higher confidence**: Tests reflect actual runtime behavior

## Pattern-Specific Testing Strategies

### 1. RuleChainExecutor - Pattern Routing and Delegation

**Testing Focus**: Ensures correct routing to pattern-specific executors and proper error handling.

```java
@Test
@DisplayName("Should return all supported patterns")
void testRuleChainExecutorSupportedPatterns() {
    String[] patterns = ruleChainExecutor.getSupportedPatterns();
    
    assertNotNull(patterns, "Patterns array should not be null");
    assertEquals(6, patterns.length, "Should support exactly 6 patterns");
    
    // Verify all expected patterns are present
    List<String> patternList = Arrays.asList(patterns);
    assertTrue(patternList.contains("conditional-chaining"));
    assertTrue(patternList.contains("sequential-dependency"));
    assertTrue(patternList.contains("result-based-routing"));
    assertTrue(patternList.contains("accumulative-chaining"));
    assertTrue(patternList.contains("complex-workflow"));
    assertTrue(patternList.contains("fluent-builder"));
}
```

**Key Test Scenarios:**
- Pattern enumeration and validation
- Routing to correct pattern-specific executors
- Error handling for unsupported patterns
- Null safety and edge cases

### 2. ConditionalChainingExecutor - Conditional Execution Paths

**Testing Focus**: Validates trigger-based conditional logic and branching execution paths.

```java
private Map<String, Object> createConditionalChainingConfig() {
    Map<String, Object> config = new HashMap<>();
    
    // Trigger rule configuration
    Map<String, Object> triggerRule = new HashMap<>();
    triggerRule.put("id", "high-value-check");
    triggerRule.put("condition", "#amount > 100000");
    triggerRule.put("message", "High value transaction detected");
    config.put("trigger-rule", triggerRule);
    
    // Conditional rules for different execution paths
    Map<String, Object> conditionalRules = new HashMap<>();
    
    List<Map<String, Object>> onTriggerRules = new ArrayList<>();
    Map<String, Object> onTriggerRule = new HashMap<>();
    onTriggerRule.put("id", "enhanced-check");
    onTriggerRule.put("condition", "true");
    onTriggerRule.put("message", "Enhanced validation required");
    onTriggerRules.add(onTriggerRule);
    conditionalRules.put("on-trigger", onTriggerRules);
    
    config.put("conditional-rules", conditionalRules);
    return config;
}
```

**Key Test Scenarios:**
- Trigger rule evaluation and execution
- On-trigger path execution when conditions are met
- On-no-trigger path execution when conditions fail
- Configuration validation and error handling

### 3. SequentialDependencyExecutor - Stage Dependencies and Pipeline Processing

**Testing Focus**: Ensures proper sequential execution with dependency management.

```java
private Map<String, Object> createSequentialDependencyConfig() {
    Map<String, Object> config = new HashMap<>();
    List<Object> stages = new ArrayList<>();
    
    // Stage 1: Base Discount Calculation
    Map<String, Object> stage1 = new HashMap<>();
    stage1.put("stage", 1);
    stage1.put("name", "Base Discount Calculation");
    
    Map<String, Object> rule1 = new HashMap<>();
    rule1.put("id", "base-discount");
    rule1.put("condition", "#customerTier == 'GOLD' ? 0.15 : 0.10");
    rule1.put("message", "Base discount calculated");
    stage1.put("rule", rule1);
    stage1.put("output-variable", "baseDiscount");
    stages.add(stage1);
    
    // Stage 2: Regional Multiplier (depends on stage 1)
    Map<String, Object> stage2 = new HashMap<>();
    stage2.put("stage", 2);
    stage2.put("name", "Regional Multiplier");
    stage2.put("depends-on", Arrays.asList("baseDiscount"));
    
    Map<String, Object> rule2 = new HashMap<>();
    rule2.put("id", "regional-multiplier");
    rule2.put("condition", "#baseDiscount * #regionalFactor");
    rule2.put("message", "Regional adjustment applied");
    stage2.put("rule", rule2);
    stage2.put("output-variable", "adjustedDiscount");
    stages.add(stage2);
    
    config.put("stages", stages);
    return config;
}
```

**Key Test Scenarios:**
- Sequential stage execution in correct order
- Dependency validation and enforcement
- Output variable propagation between stages
- Pipeline failure handling and recovery

### 4. ResultBasedRoutingExecutor - Route Determination and Execution

**Testing Focus**: Validates dynamic routing based on rule evaluation results.

```java
private Map<String, Object> createResultBasedRoutingConfig() {
    Map<String, Object> config = new HashMap<>();
    
    // Router rule that determines the execution path
    Map<String, Object> routerRule = new HashMap<>();
    routerRule.put("id", "customer-tier-router");
    routerRule.put("condition", "#customerTier == 'PREMIUM' ? 'premium' : 'standard'");
    routerRule.put("message", "Customer tier routing decision");
    config.put("router-rule", routerRule);
    
    // Define routes for different outcomes
    Map<String, Object> routes = new HashMap<>();
    
    // Premium customer route
    List<Map<String, Object>> premiumRules = new ArrayList<>();
    Map<String, Object> premiumRule = new HashMap<>();
    premiumRule.put("id", "premium-processing");
    premiumRule.put("condition", "#amount > 50000 ? 'manual-review' : 'auto-approve'");
    premiumRule.put("message", "Premium customer processing");
    premiumRules.add(premiumRule);
    routes.put("premium", premiumRules);
    
    // Standard customer route
    List<Map<String, Object>> standardRules = new ArrayList<>();
    Map<String, Object> standardRule = new HashMap<>();
    standardRule.put("id", "standard-processing");
    standardRule.put("condition", "#amount > 10000 ? 'review-required' : 'auto-approve'");
    standardRule.put("message", "Standard customer processing");
    standardRules.add(standardRule);
    routes.put("standard", standardRules);
    
    config.put("routes", routes);
    return config;
}
```

**Key Test Scenarios:**
- Router rule evaluation and result interpretation
- Dynamic route selection based on evaluation results
- Route-specific rule execution
- Default route handling for unmatched results

### 5. AccumulativeChainingExecutor - Score Accumulation and Weighted Calculations

**Testing Focus**: Ensures proper accumulation logic with weighted scoring.

```java
private Map<String, Object> createAccumulativeChainingConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put("accumulator-variable", "riskScore");
    config.put("initial-value", 0);
    
    List<Map<String, Object>> accumulationRules = new ArrayList<>();
    
    // Credit score contribution
    Map<String, Object> creditRule = new HashMap<>();
    creditRule.put("id", "credit-score-rule");
    creditRule.put("condition", "#creditScore > 700 ? 15 : (#creditScore > 600 ? 10 : 5)");
    creditRule.put("message", "Credit score risk assessment");
    creditRule.put("weight", 2.0);
    creditRule.put("accumulation-expression", "#riskScore + (#ruleResult * #weight)");
    accumulationRules.add(creditRule);
    
    // Income stability contribution
    Map<String, Object> incomeRule = new HashMap<>();
    incomeRule.put("id", "income-stability-rule");
    incomeRule.put("condition", "#employmentYears > 5 ? 20 : (#employmentYears > 2 ? 15 : 10)");
    incomeRule.put("message", "Income stability assessment");
    incomeRule.put("weight", 1.5);
    incomeRule.put("accumulation-expression", "#riskScore + (#ruleResult * #weight)");
    accumulationRules.add(incomeRule);
    
    config.put("accumulation-rules", accumulationRules);
    return config;
}
```

**Key Test Scenarios:**
- Score accumulation across multiple rules
- Weighted calculation validation
- Accumulator variable management and updates
- Final score computation and result aggregation

## Error Handling and Edge Case Testing

### Comprehensive Error Scenarios

```java
@Test
@DisplayName("All executors should handle null rule chain appropriately")
void testExecutorsWithNullRuleChain() {
    System.out.println("TEST: Triggering intentional error - testing executors with null rule chain");
    
    Map<String, Object> config = new HashMap<>();
    
    // Test that executors handle null inputs predictably
    assertThrows(NullPointerException.class, () -> {
        conditionalExecutor.execute(null, config, context);
    }, "ConditionalChainingExecutor should throw NPE for null rule chain");
    
    // Similar tests for all other executors...
}
```

### Intentional Error Logging

Following the established pattern for test error documentation:

```java
@Test
@DisplayName("Should handle configuration validation failure")
void testConfigurationValidationFailure() {
    System.out.println("TEST: Triggering intentional error - testing configuration validation failure");
    
    YamlRuleChain ruleChain = createTestRuleChain("test-chain", "conditional-chaining");
    Map<String, Object> invalidConfig = new HashMap<>();
    // Intentionally missing required configuration
    
    assertDoesNotThrow(() -> {
        RuleChainResult result = conditionalExecutor.execute(ruleChain, invalidConfig, context);
        assertNotNull(result, "Result should not be null even with invalid config");
    }, "Should handle configuration validation gracefully");
}
```

## Configuration Testing Patterns

### Minimal Valid Configurations

Each executor requires specific configuration structures. We provide minimal valid configurations for testing:

```java
// Conditional Chaining - Minimal Configuration
private Map<String, Object> createMinimalConditionalConfig() {
    Map<String, Object> config = new HashMap<>();
    Map<String, Object> triggerRule = new HashMap<>();
    triggerRule.put("id", "simple-trigger");
    triggerRule.put("condition", "true");
    triggerRule.put("message", "Always trigger");
    config.put("trigger-rule", triggerRule);
    return config;
}

// Sequential Dependency - Minimal Configuration
private Map<String, Object> createMinimalSequentialConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put("stages", new ArrayList<>()); // Empty stages list
    return config;
}
```

### Configuration Validation Testing

```java
@Test
@DisplayName("Should validate required configuration elements")
void testConfigurationValidation() {
    YamlRuleChain ruleChain = createTestRuleChain("validation-test", "conditional-chaining");
    
    // Test missing trigger-rule
    Map<String, Object> missingTrigger = new HashMap<>();
    assertDoesNotThrow(() -> {
        RuleChainResult result = conditionalExecutor.execute(ruleChain, missingTrigger, context);
        assertNotNull(result, "Should handle missing configuration gracefully");
    });
    
    // Test invalid configuration structure
    Map<String, Object> invalidStructure = new HashMap<>();
    invalidStructure.put("trigger-rule", "invalid-string-instead-of-map");
    assertDoesNotThrow(() -> {
        RuleChainResult result = conditionalExecutor.execute(ruleChain, invalidStructure, context);
        assertNotNull(result, "Should handle invalid structure gracefully");
    });
}
```

## Test Organization and Maintainability

### Centralized Test Utilities

```java
/**
 * Helper method to create test rule chains with consistent structure
 */
private YamlRuleChain createTestRuleChain(String id, String pattern) {
    YamlRuleChain ruleChain = new YamlRuleChain();
    ruleChain.setId(id);
    ruleChain.setName("Test Rule Chain - " + pattern);
    ruleChain.setPattern(pattern);
    ruleChain.setEnabled(true);
    return ruleChain;
}

/**
 * Configuration factory methods for each pattern
 */
private Map<String, Object> createConfigurationForPattern(String pattern) {
    switch (pattern) {
        case "conditional-chaining":
            return createConditionalChainingConfig();
        case "sequential-dependency":
            return createSequentialDependencyConfig();
        case "result-based-routing":
            return createResultBasedRoutingConfig();
        case "accumulative-chaining":
            return createAccumulativeChainingConfig();
        case "complex-workflow":
            return createComplexWorkflowConfig();
        case "fluent-builder":
            return createFluentBuilderConfig();
        default:
            return new HashMap<>();
    }
}
```

### Test Documentation Standards

Each test follows a consistent documentation pattern:

```java
/**
 * Tests the core functionality of [Component Name].
 * 
 * Validates:
 * - [Primary behavior 1]
 * - [Primary behavior 2]
 * - [Error handling scenario]
 * 
 * Configuration: [Description of test configuration]
 * Expected Result: [Description of expected outcome]
 */
@Test
@DisplayName("Clear description of what is being tested")
void testMethodName() {
    // Test implementation
}
```

## Benefits of This Testing Approach

### 1. **Comprehensive Coverage**
- All six executor patterns fully tested
- Configuration validation for each pattern
- Error handling and edge cases covered
- Integration scenarios validated

### 2. **Maintainability**
- Simple, focused tests without complex mocking
- Clear configuration patterns for each executor type
- Consistent error handling and logging
- Well-documented test intentions

### 3. **Reliability**
- Tests actual runtime behavior
- Validates real service interactions
- Provides confidence in production scenarios
- Catches integration issues early

### 4. **Developer Experience**
- Clear examples of how to configure each executor
- Documented patterns for extending tests
- Consistent approach across all executors
- Easy to understand and modify

## Future Considerations

### Extending the Test Suite

When adding new executor patterns or modifying existing ones:

1. **Follow the established configuration pattern**
2. **Create minimal and comprehensive configuration examples**
3. **Include error handling tests with proper logging**
4. **Document the test approach and expected behaviors**
5. **Maintain consistency with existing test structure**

### Performance Testing

Consider adding performance benchmarks for:
- Large rule chain execution
- Complex workflow orchestration
- High-volume accumulation scenarios
- Memory usage patterns

### 6. ComplexWorkflowExecutor - Multi-Stage Workflow Orchestration

**Testing Focus**: Validates complex business workflows with conditional stages and dependencies.

```java
private Map<String, Object> createComplexWorkflowConfig() {
    Map<String, Object> config = new HashMap<>();
    List<Object> stages = new ArrayList<>();

    // Stage 1: Risk Assessment (always executed)
    Map<String, Object> riskStage = new HashMap<>();
    riskStage.put("id", "risk-assessment");
    riskStage.put("name", "Risk Assessment");
    riskStage.put("order", 1);

    Map<String, Object> riskRule = new HashMap<>();
    riskRule.put("id", "risk-evaluation");
    riskRule.put("condition", "#amount > 100000 ? 'HIGH' : (#amount > 50000 ? 'MEDIUM' : 'LOW')");
    riskRule.put("message", "Risk level determined");
    riskStage.put("rule", riskRule);
    riskStage.put("output-variable", "riskLevel");
    stages.add(riskStage);

    // Stage 2: Enhanced Validation (conditional on high risk)
    Map<String, Object> validationStage = new HashMap<>();
    validationStage.put("id", "enhanced-validation");
    validationStage.put("name", "Enhanced Validation");
    validationStage.put("order", 2);
    validationStage.put("condition", "#riskLevel == 'HIGH'");
    validationStage.put("depends-on", Arrays.asList("risk-assessment"));

    Map<String, Object> validationRule = new HashMap<>();
    validationRule.put("id", "enhanced-checks");
    validationRule.put("condition", "#documentVerification && #identityConfirmed");
    validationRule.put("message", "Enhanced validation completed");
    validationStage.put("rule", validationRule);
    validationStage.put("output-variable", "validationPassed");
    stages.add(validationStage);

    // Stage 3: Approval Decision (depends on previous stages)
    Map<String, Object> approvalStage = new HashMap<>();
    approvalStage.put("id", "approval-decision");
    approvalStage.put("name", "Approval Decision");
    approvalStage.put("order", 3);
    approvalStage.put("depends-on", Arrays.asList("risk-assessment"));

    Map<String, Object> approvalRule = new HashMap<>();
    approvalRule.put("id", "final-approval");
    approvalRule.put("condition", "#riskLevel == 'LOW' ? 'AUTO_APPROVED' : " +
                                 "(#riskLevel == 'MEDIUM' ? 'MANUAL_REVIEW' : " +
                                 "(#validationPassed ? 'CONDITIONAL_APPROVAL' : 'REJECTED'))");
    approvalRule.put("message", "Final approval decision made");
    approvalStage.put("rule", approvalRule);
    approvalStage.put("output-variable", "approvalDecision");
    stages.add(approvalStage);

    config.put("stages", stages);
    return config;
}
```

**Key Test Scenarios:**
- Multi-stage workflow execution with conditional branching
- Stage dependency validation and enforcement
- Complex business logic orchestration
- Workflow state management and result aggregation

### 7. FluentBuilderExecutor - Rule Tree Execution and Fluent API

**Testing Focus**: Ensures proper rule tree navigation and fluent API composition.

```java
private Map<String, Object> createFluentBuilderConfig() {
    Map<String, Object> config = new HashMap<>();

    // Root rule that starts the fluent chain
    Map<String, Object> rootRule = new HashMap<>();
    rootRule.put("id", "transaction-validation");
    rootRule.put("condition", "#amount > 0 && #currency != null");
    rootRule.put("message", "Transaction validation");

    // Success path - amount validation
    Map<String, Object> amountValidation = new HashMap<>();
    amountValidation.put("id", "amount-validation");
    amountValidation.put("condition", "#amount <= #dailyLimit");
    amountValidation.put("message", "Amount within daily limit");

    // Success path from amount validation - final approval
    Map<String, Object> finalApproval = new HashMap<>();
    finalApproval.put("id", "final-approval");
    finalApproval.put("condition", "true");
    finalApproval.put("message", "Transaction approved");
    amountValidation.put("on-success", finalApproval);

    // Failure path from amount validation - manual review
    Map<String, Object> manualReview = new HashMap<>();
    manualReview.put("id", "manual-review");
    manualReview.put("condition", "true");
    manualReview.put("message", "Manual review required");
    amountValidation.put("on-failure", manualReview);

    rootRule.put("on-success", amountValidation);

    // Failure path from root - validation failed
    Map<String, Object> validationFailed = new HashMap<>();
    validationFailed.put("id", "validation-failed");
    validationFailed.put("condition", "true");
    validationFailed.put("message", "Transaction validation failed");
    rootRule.put("on-failure", validationFailed);

    config.put("root-rule", rootRule);
    return config;
}
```

**Key Test Scenarios:**
- Rule tree traversal and navigation
- Conditional path execution (on-success/on-failure)
- Fluent API composition and chaining
- Complex branching logic validation

## Advanced Testing Scenarios

### Performance and Load Testing

```java
@Test
@DisplayName("Should handle large rule chains efficiently")
void testLargeRuleChainPerformance() {
    // Create a complex rule chain with multiple stages
    YamlRuleChain largeChain = createLargeRuleChain(100); // 100 stages
    Map<String, Object> config = createPerformanceTestConfig();

    long startTime = System.currentTimeMillis();

    RuleChainResult result = sequentialExecutor.execute(largeChain, config, context);

    long executionTime = System.currentTimeMillis() - startTime;

    assertNotNull(result, "Result should not be null");
    assertTrue(executionTime < 5000, "Execution should complete within 5 seconds");

    System.out.println("Large rule chain execution time: " + executionTime + "ms");
}
```

### Concurrent Execution Testing

```java
@Test
@DisplayName("Should handle concurrent executor usage")
void testConcurrentExecution() throws InterruptedException {
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<RuleChainResult> results = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        new Thread(() -> {
            try {
                YamlRuleChain ruleChain = createTestRuleChain("concurrent-test-" + threadId, "conditional-chaining");
                Map<String, Object> config = createConditionalChainingConfig();
                ChainedEvaluationContext threadContext = new ChainedEvaluationContext();

                RuleChainResult result = conditionalExecutor.execute(ruleChain, config, threadContext);
                results.add(result);
            } finally {
                latch.countDown();
            }
        }).start();
    }

    assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");
    assertEquals(threadCount, results.size(), "Should have results from all threads");

    // Verify all results are valid
    for (RuleChainResult result : results) {
        assertNotNull(result, "Each result should not be null");
    }
}
```

### Memory Usage Testing

```java
@Test
@DisplayName("Should manage memory efficiently during execution")
void testMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();

    // Force garbage collection and measure baseline
    System.gc();
    long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

    // Execute multiple rule chains
    for (int i = 0; i < 1000; i++) {
        YamlRuleChain ruleChain = createTestRuleChain("memory-test-" + i, "accumulative-chaining");
        Map<String, Object> config = createAccumulativeChainingConfig();
        ChainedEvaluationContext testContext = new ChainedEvaluationContext();

        RuleChainResult result = accumulativeExecutor.execute(ruleChain, config, testContext);
        assertNotNull(result, "Result should not be null");
    }

    // Force garbage collection and measure final memory
    System.gc();
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = finalMemory - baselineMemory;

    // Memory increase should be reasonable (less than 100MB for 1000 executions)
    assertTrue(memoryIncrease < 100 * 1024 * 1024,
              "Memory increase should be reasonable: " + (memoryIncrease / 1024 / 1024) + "MB");

    System.out.println("Memory increase after 1000 executions: " + (memoryIncrease / 1024 / 1024) + "MB");
}
```

## Test Data Management

### Configuration Templates

```java
/**
 * Template configurations for different complexity levels
 */
public class ExecutorConfigurationTemplates {

    public static Map<String, Object> createSimpleConditionalConfig() {
        // Minimal configuration for basic testing
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> triggerRule = new HashMap<>();
        triggerRule.put("id", "simple-trigger");
        triggerRule.put("condition", "true");
        triggerRule.put("message", "Simple trigger");
        config.put("trigger-rule", triggerRule);
        return config;
    }

    public static Map<String, Object> createComplexConditionalConfig() {
        // Complex configuration with multiple conditional paths
        Map<String, Object> config = createSimpleConditionalConfig();

        Map<String, Object> conditionalRules = new HashMap<>();

        // Multiple on-trigger rules
        List<Map<String, Object>> onTriggerRules = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("id", "on-trigger-rule-" + i);
            rule.put("condition", "#value > " + (i * 10));
            rule.put("message", "On-trigger rule " + i);
            onTriggerRules.add(rule);
        }
        conditionalRules.put("on-trigger", onTriggerRules);

        // Multiple on-no-trigger rules
        List<Map<String, Object>> onNoTriggerRules = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("id", "on-no-trigger-rule-" + i);
            rule.put("condition", "true");
            rule.put("message", "On-no-trigger rule " + i);
            onNoTriggerRules.add(rule);
        }
        conditionalRules.put("on-no-trigger", onNoTriggerRules);

        config.put("conditional-rules", conditionalRules);
        return config;
    }
}
```

### Test Data Builders

```java
/**
 * Builder pattern for creating test rule chains
 */
public class RuleChainTestBuilder {
    private String id;
    private String name;
    private String pattern;
    private boolean enabled = true;
    private Map<String, Object> configuration = new HashMap<>();

    public static RuleChainTestBuilder create() {
        return new RuleChainTestBuilder();
    }

    public RuleChainTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RuleChainTestBuilder withPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public RuleChainTestBuilder withConfiguration(Map<String, Object> config) {
        this.configuration = config;
        return this;
    }

    public RuleChainTestBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public YamlRuleChain build() {
        YamlRuleChain ruleChain = new YamlRuleChain();
        ruleChain.setId(id != null ? id : "test-chain-" + System.currentTimeMillis());
        ruleChain.setName(name != null ? name : "Test Rule Chain");
        ruleChain.setPattern(pattern);
        ruleChain.setEnabled(enabled);
        ruleChain.setConfiguration(configuration);
        return ruleChain;
    }
}

// Usage example:
YamlRuleChain testChain = RuleChainTestBuilder.create()
    .withId("complex-test")
    .withPattern("conditional-chaining")
    .withConfiguration(ExecutorConfigurationTemplates.createComplexConditionalConfig())
    .build();
```

This comprehensive testing methodology provides a solid foundation for maintaining and extending the Engine Executor Layer while ensuring robust, reliable rule execution across all supported patterns.
