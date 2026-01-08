# Engine Executor Testing Quick Reference

## Testing Checklist for New Executor Patterns

When implementing tests for a new executor pattern, ensure you cover these essential areas:

### ✅ **Basic Functionality Tests**
- [ ] Constructor with valid dependencies
- [ ] Constructor with null dependencies (error handling)
- [ ] Basic execution with minimal valid configuration
- [ ] Execution result validation (not null, correct pattern)

### ✅ **Configuration Validation Tests**
- [ ] Missing required configuration elements
- [ ] Invalid configuration structure (wrong types)
- [ ] Empty/null configuration handling
- [ ] Complex valid configuration scenarios

### ✅ **Business Logic Tests**
- [ ] Core pattern-specific functionality
- [ ] Multiple execution paths/branches
- [ ] Edge cases and boundary conditions
- [ ] Integration with context and variables

### ✅ **Error Handling Tests**
- [ ] Null rule chain handling
- [ ] Null context handling
- [ ] Configuration validation failures
- [ ] Runtime execution exceptions
- [ ] Proper error logging with "TEST: Triggering intentional error" prefix

### ✅ **Integration Tests**
- [ ] Real service dependencies (not mocked)
- [ ] Context state management
- [ ] Variable propagation
- [ ] Result aggregation

## Configuration Templates by Pattern

### Minimal Configurations (for basic testing)

```java
// Conditional Chaining - Minimal
Map<String, Object> minimalConditional = Map.of(
    "trigger-rule", Map.of(
        "id", "simple-trigger",
        "condition", "true",
        "message", "Always trigger"
    )
);

// Sequential Dependency - Minimal
Map<String, Object> minimalSequential = Map.of(
    "stages", List.of()
);

// Result-Based Routing - Minimal
Map<String, Object> minimalRouting = Map.of(
    "router-rule", Map.of(
        "id", "simple-router",
        "condition", "'default'",
        "message", "Default route"
    ),
    "routes", Map.of()
);

// Accumulative Chaining - Minimal
Map<String, Object> minimalAccumulative = Map.of(
    "accumulator-variable", "score",
    "initial-value", 0,
    "accumulation-rules", List.of()
);

// Complex Workflow - Minimal
Map<String, Object> minimalWorkflow = Map.of(
    "stages", List.of()
);

// Fluent Builder - Minimal
Map<String, Object> minimalFluent = Map.of(
    "root-rule", Map.of(
        "id", "root",
        "condition", "true",
        "message", "Root rule"
    )
);
```

## Standard Test Structure Template

```java
class NewExecutorTest {
    
    private NewExecutor executor;
    private ChainedEvaluationContext context;
    private ExpressionEvaluatorService evaluatorService;
    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        // Use real services for integration testing
        evaluatorService = new ExpressionEvaluatorService();
        ruleEngineService = new RuleEngineService(evaluatorService);
        executor = new NewExecutor(ruleEngineService, evaluatorService);
        context = new ChainedEvaluationContext();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create executor with valid dependencies")
    void testConstructor() {
        assertNotNull(executor, "Executor should be created successfully");
    }

    // ========================================
    // Configuration Validation Tests
    // ========================================

    @Test
    @DisplayName("Should handle missing configuration gracefully")
    void testMissingConfiguration() {
        YamlRuleChain ruleChain = createTestRuleChain("test", "new-pattern");
        Map<String, Object> emptyConfig = new HashMap<>();
        
        assertDoesNotThrow(() -> {
            RuleChainResult result = executor.execute(ruleChain, emptyConfig, context);
            assertNotNull(result, "Result should not be null");
        });
    }

    // ========================================
    // Core Functionality Tests
    // ========================================

    @Test
    @DisplayName("Should execute with valid configuration")
    void testValidExecution() {
        YamlRuleChain ruleChain = createTestRuleChain("test", "new-pattern");
        Map<String, Object> config = createValidConfiguration();
        
        RuleChainResult result = executor.execute(ruleChain, config, context);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccessful(), "Execution should succeed");
        assertEquals("new-pattern", result.getPattern(), "Should have correct pattern");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle null rule chain appropriately")
    void testNullRuleChain() {
        System.out.println("TEST: Triggering intentional error - testing null rule chain");
        
        Map<String, Object> config = new HashMap<>();
        
        assertThrows(NullPointerException.class, () -> {
            executor.execute(null, config, context);
        }, "Should throw NPE for null rule chain");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private YamlRuleChain createTestRuleChain(String id, String pattern) {
        YamlRuleChain ruleChain = new YamlRuleChain();
        ruleChain.setId(id);
        ruleChain.setName("Test Rule Chain");
        ruleChain.setPattern(pattern);
        ruleChain.setEnabled(true);
        return ruleChain;
    }

    private Map<String, Object> createValidConfiguration() {
        // Return pattern-specific minimal valid configuration
        return createMinimalConfigurationForPattern();
    }
}
```

## Common Test Patterns

### 1. **Context Setup Pattern**

```java
// Set up realistic business context
private void setupLoanApplicationContext(ChainedEvaluationContext ctx) {
    ctx.setVariable("applicantAge", 35);
    ctx.setVariable("annualIncome", 75000);
    ctx.setVariable("creditScore", 720);
    ctx.setVariable("requestedAmount", 250000);
    ctx.setVariable("employmentYears", 8);
}

// Set up transaction context
private void setupTransactionContext(ChainedEvaluationContext ctx, double amount, String tier) {
    ctx.setVariable("transactionAmount", amount);
    ctx.setVariable("customerTier", tier);
    ctx.setVariable("transactionDate", LocalDateTime.now());
    ctx.setVariable("merchantCategory", "RETAIL");
}
```

### 2. **Configuration Builder Pattern**

```java
public class ExecutorConfigBuilder {
    private Map<String, Object> config = new HashMap<>();
    
    public static ExecutorConfigBuilder create() {
        return new ExecutorConfigBuilder();
    }
    
    public ExecutorConfigBuilder withTriggerRule(String id, String condition, String message) {
        Map<String, Object> triggerRule = new HashMap<>();
        triggerRule.put("id", id);
        triggerRule.put("condition", condition);
        triggerRule.put("message", message);
        config.put("trigger-rule", triggerRule);
        return this;
    }
    
    public ExecutorConfigBuilder withStage(int order, String id, String condition, String outputVar) {
        List<Object> stages = (List<Object>) config.computeIfAbsent("stages", k -> new ArrayList<>());
        
        Map<String, Object> stage = new HashMap<>();
        stage.put("stage", order);
        stage.put("id", id);
        
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", id + "-rule");
        rule.put("condition", condition);
        rule.put("message", "Stage " + order + " execution");
        stage.put("rule", rule);
        stage.put("output-variable", outputVar);
        
        stages.add(stage);
        return this;
    }
    
    public Map<String, Object> build() {
        return new HashMap<>(config);
    }
}

// Usage:
Map<String, Object> config = ExecutorConfigBuilder.create()
    .withTriggerRule("high-value", "#amount > 100000", "High value detected")
    .withStage(1, "validation", "true", "validationResult")
    .withStage(2, "processing", "#validationResult == true", "processResult")
    .build();
```

### 3. **Assertion Helper Pattern**

```java
public class ExecutorTestAssertions {
    
    public static void assertSuccessfulExecution(RuleChainResult result, String expectedPattern) {
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccessful(), "Execution should succeed");
        assertEquals(expectedPattern, result.getPattern(), "Should have correct pattern");
        assertNotNull(result.getFinalOutcome(), "Should have final outcome");
    }
    
    public static void assertContextVariable(ChainedEvaluationContext context, 
                                           String variableName, Object expectedValue) {
        assertTrue(context.hasVariable(variableName), 
                  "Context should contain variable: " + variableName);
        assertEquals(expectedValue, context.getVariable(variableName), 
                    "Variable " + variableName + " should have expected value");
    }
    
    public static void assertStageExecution(ChainedEvaluationContext context, String... stageNames) {
        for (String stageName : stageNames) {
            assertTrue(context.hasVariable(stageName), 
                      "Stage " + stageName + " should have been executed");
        }
    }
}

// Usage:
ExecutorTestAssertions.assertSuccessfulExecution(result, "conditional-chaining");
ExecutorTestAssertions.assertContextVariable(context, "riskScore", 85);
ExecutorTestAssertions.assertStageExecution(context, "validation", "processing", "approval");
```

## Performance Testing Template

```java
@Test
@DisplayName("Should handle performance requirements")
void testPerformanceRequirements() {
    YamlRuleChain ruleChain = createComplexRuleChain();
    Map<String, Object> config = createComplexConfiguration();
    
    // Warmup
    for (int i = 0; i < 10; i++) {
        executor.execute(ruleChain, config, new ChainedEvaluationContext());
    }
    
    // Measure execution time
    long startTime = System.nanoTime();
    
    for (int i = 0; i < 100; i++) {
        ChainedEvaluationContext testContext = new ChainedEvaluationContext();
        setupTestContext(testContext);
        
        RuleChainResult result = executor.execute(ruleChain, config, testContext);
        assertNotNull(result, "Result should not be null");
    }
    
    long endTime = System.nanoTime();
    long avgExecutionTime = (endTime - startTime) / 100 / 1_000_000; // Convert to milliseconds
    
    assertTrue(avgExecutionTime < 50, 
              "Average execution time should be under 50ms, was: " + avgExecutionTime + "ms");
    
    System.out.println("Average execution time: " + avgExecutionTime + "ms");
}
```

## Memory Testing Template

```java
@Test
@DisplayName("Should manage memory efficiently")
void testMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    
    // Force GC and measure baseline
    System.gc();
    Thread.sleep(100); // Allow GC to complete
    long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // Execute many iterations
    for (int i = 0; i < 1000; i++) {
        YamlRuleChain ruleChain = createTestRuleChain("memory-test-" + i, "test-pattern");
        Map<String, Object> config = createValidConfiguration();
        ChainedEvaluationContext testContext = new ChainedEvaluationContext();
        
        RuleChainResult result = executor.execute(ruleChain, config, testContext);
        assertNotNull(result, "Result should not be null");
        
        // Clear references
        ruleChain = null;
        config = null;
        testContext = null;
        result = null;
    }
    
    // Force GC and measure final memory
    System.gc();
    Thread.sleep(100);
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = finalMemory - baselineMemory;
    
    // Memory increase should be reasonable (less than 50MB for 1000 executions)
    assertTrue(memoryIncrease < 50 * 1024 * 1024, 
              "Memory increase should be reasonable: " + (memoryIncrease / 1024 / 1024) + "MB");
}
```

## Documentation Standards

### Test Method Naming Convention
- `test[Functionality][Scenario]` - e.g., `testConditionalExecutionWithHighValueTrigger`
- Use `@DisplayName` for human-readable descriptions
- Group related tests with section comments

### Error Test Documentation
Always prefix intentional error tests with logging:
```java
System.out.println("TEST: Triggering intentional error - [description of what error is being tested]");
```

### Configuration Documentation
Document complex configurations with inline comments:
```java
// Trigger rule: Detects high-value transactions requiring enhanced validation
Map<String, Object> triggerRule = new HashMap<>();
triggerRule.put("condition", "#amount > 100000 && #customerTier == 'PREMIUM'");
```

This quick reference provides the essential patterns and templates needed to implement comprehensive tests for any executor pattern in the Apex Rules Engine.
