# APEX Scenario Stage Configuration - Detailed Implementation Plan

## Overview

This document provides a comprehensive, phased implementation plan for adding explicit stage configuration to APEX scenarios. The enhancement addresses the need for sophisticated middle and back office trade processing workflows with dependency management, failure policies, and sequential processing stages.

## Business Context

Current APEX scenario processing executes rule configurations sequentially without explicit stage management. For financial trade processing, we need:

- **Validation Stage**: Data integrity, completeness, format validation (must complete before enrichment)
- **Enrichment Stage**: Data augmentation, calculations, market data lookup (depends on validation)
- **Compliance Stage**: Regulatory compliance, risk management (depends on validation and enrichment)

## Architecture Goals

1. **Explicit Stage Configuration** - Clear definition of processing stages with metadata
2. **Dependency Management** - Stages can depend on successful completion of other stages
3. **Failure Policy Control** - Different failure handling strategies per stage
4. **Backward Compatibility** - Existing scenarios continue to work unchanged
5. **Performance Optimization** - Minimal overhead over current processing

## Phase 1: Core Data Model Enhancement
*Duration: 3-5 days*

### 1.1 Create ScenarioStage Configuration Class

**New Class**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStage.java`

```java
/**
 * Configuration class representing a single processing stage within a scenario.
 * 
 * Each stage defines a specific phase of trade processing (validation, enrichment, compliance)
 * with explicit execution order, failure policies, and dependency management.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ScenarioStage {
    private String stageName;
    private String configFile;
    private int executionOrder;
    private String failurePolicy; // "terminate", "continue-with-warnings", "flag-for-review"
    private List<String> dependsOn;
    private boolean required;
    private Map<String, Object> stageMetadata;
    
    // Standard getters/setters following existing patterns
    // Utility methods for dependency checking
    // Validation methods for configuration integrity
}
```

### 1.2 Enhance ScenarioConfiguration Class

**Modify**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioConfiguration.java`

```java
public class ScenarioConfiguration {
    // Existing fields...
    private List<ScenarioStage> processingStages; // NEW: Replace simple rule-configurations
    
    // NEW: Backward compatibility method
    @Deprecated
    public List<String> getRuleConfigurations() {
        if (processingStages != null) {
            return processingStages.stream()
                .sorted(Comparator.comparingInt(ScenarioStage::getExecutionOrder))
                .map(ScenarioStage::getConfigFile)
                .collect(Collectors.toList());
        }
        return ruleConfigurations; // Fallback to legacy
    }
    
    // NEW: Stage-aware methods
    public List<ScenarioStage> getProcessingStages() { return processingStages; }
    public List<ScenarioStage> getStagesByExecutionOrder() { /* sorted stages */ }
    public ScenarioStage getStageByName(String stageName) { /* find by name */ }
    public boolean hasStageConfiguration() { return processingStages != null && !processingStages.isEmpty(); }
}
```

### 1.3 Enhanced YAML Configuration Structure

**New YAML Structure** (backward compatible):

```yaml
scenario:
  scenario-id: "otc-options-standard"
  name: "OTC Options Processing"
  description: "Complete processing pipeline for OTC options"
  
  # Data types this scenario applies to
  data-types:
    - "OtcOption"
    - "dev.mars.apex.demo.model.OtcOption"
  
  # NEW: Explicit stage configuration
  processing-stages:
    - stage-name: "validation"
      config-file: "config/otc-options-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
      required: true
      stage-metadata:
        description: "Data integrity and format validation"
        sla-ms: 500
        
    - stage-name: "enrichment"
      config-file: "config/financial-enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
      depends-on: ["validation"]
      stage-metadata:
        description: "Market data and calculation enrichment"
        sla-ms: 2000
        
    - stage-name: "compliance"
      config-file: "config/derivatives-compliance-rules.yaml"
      execution-order: 3
      failure-policy: "flag-for-review"
      depends-on: ["validation", "enrichment"]
      stage-metadata:
        description: "Regulatory compliance and risk checks"
        sla-ms: 1000
  
  # LEGACY: Still supported for backward compatibility
  rule-configurations:
    - "config/otc-options-validation-rules.yaml"
    - "config/financial-enrichment-rules.yaml"
    - "config/derivatives-compliance-rules.yaml"
```

### 1.4 Failure Policy Definitions

- **terminate**: Stop processing immediately if stage fails
- **continue-with-warnings**: Log warnings but continue to next stage
- **flag-for-review**: Mark for manual review but continue processing

## Phase 2: Processing Engine Enhancement
*Duration: 4-6 days*

### 2.1 Create ScenarioStageExecutor

**New Class**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/ScenarioStageExecutor.java`

Following patterns from `ComplexWorkflowExecutor` and `SequentialDependencyExecutor`:

```java
/**
 * Executor for processing scenario stages with dependency management and failure policies.
 * 
 * Follows the existing pattern from ComplexWorkflowExecutor and SequentialDependencyExecutor
 * but specialized for financial trade processing workflows.
 */
public class ScenarioStageExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioStageExecutor.class);
    private final YamlConfigurationLoader configLoader;
    private final RulesEngine rulesEngine;
    
    /**
     * Execute scenario stages in dependency order with failure policy enforcement.
     */
    public ScenarioExecutionResult executeStages(ScenarioConfiguration scenario, Object data) {
        List<ScenarioStage> stages = scenario.getStagesByExecutionOrder();
        ScenarioExecutionResult result = new ScenarioExecutionResult(scenario.getScenarioId());
        
        logger.info("Executing {} stages for scenario '{}'", stages.size(), scenario.getScenarioId());
        
        for (ScenarioStage stage : stages) {
            if (!shouldExecuteStage(stage, result)) {
                logger.info("Skipping stage '{}' due to dependencies or previous failures", stage.getStageName());
                result.addSkippedStage(stage.getStageName(), "Dependencies not satisfied");
                continue;
            }
            
            long stageStartTime = System.currentTimeMillis();
            StageExecutionResult stageResult = executeStage(stage, data, result);
            stageResult.setExecutionTimeMs(System.currentTimeMillis() - stageStartTime);
            
            result.addStageResult(stageResult);
            
            if (!handleStageResult(stage, stageResult, result)) {
                logger.warn("Terminating scenario execution due to stage '{}' failure policy", stage.getStageName());
                break; // Terminate processing based on failure policy
            }
        }
        
        return result;
    }
    
    private boolean shouldExecuteStage(ScenarioStage stage, ScenarioExecutionResult result) {
        // Check if all dependencies are satisfied
        if (stage.getDependsOn() != null) {
            for (String dependency : stage.getDependsOn()) {
                if (!result.isStageSuccessful(dependency)) {
                    logger.debug("Stage '{}' dependency '{}' not satisfied", stage.getStageName(), dependency);
                    return false;
                }
            }
        }
        return true;
    }
    
    private StageExecutionResult executeStage(ScenarioStage stage, Object data, ScenarioExecutionResult context) {
        logger.info("Executing stage '{}' with config: {}", stage.getStageName(), stage.getConfigFile());
        
        try {
            // Load stage configuration
            YamlRuleConfiguration stageConfig = configLoader.loadFromFile(stage.getConfigFile());
            
            // Create rules engine for this stage
            RulesEngine stageEngine = new RulesEngine(ruleFactory.createRulesEngineConfiguration(stageConfig));
            
            // Execute stage rules
            RuleResult ruleResult = stageEngine.execute(createFactsMap(data, context));
            
            return StageExecutionResult.success(stage.getStageName(), ruleResult);
            
        } catch (Exception e) {
            logger.error("Error executing stage '{}': {}", stage.getStageName(), e.getMessage(), e);
            return StageExecutionResult.failure(stage.getStageName(), e.getMessage());
        }
    }
    
    private boolean handleStageResult(ScenarioStage stage, StageExecutionResult stageResult, ScenarioExecutionResult context) {
        if (stageResult.isSuccessful()) {
            logger.info("Stage '{}' completed successfully", stage.getStageName());
            return true;
        }
        
        // Apply failure policy
        switch (stage.getFailurePolicy()) {
            case "terminate":
                logger.error("Stage '{}' failed - terminating scenario execution", stage.getStageName());
                context.setTerminated(true);
                return false;
                
            case "continue-with-warnings":
                logger.warn("Stage '{}' failed - continuing with warnings", stage.getStageName());
                context.addWarning("Stage '" + stage.getStageName() + "' failed but processing continued");
                return true;
                
            case "flag-for-review":
                logger.warn("Stage '{}' failed - flagging for manual review", stage.getStageName());
                context.setRequiresReview(true);
                context.addReviewFlag("Stage '" + stage.getStageName() + "' requires manual review");
                return true;
                
            default:
                logger.warn("Unknown failure policy '{}' for stage '{}' - treating as terminate", 
                           stage.getFailurePolicy(), stage.getStageName());
                return false;
        }
    }
    
    private Map<String, Object> createFactsMap(Object data, ScenarioExecutionResult context) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        facts.put("scenarioContext", context);
        facts.put("previousStageResults", context.getStageResults());
        return facts;
    }
}
```

### 2.2 Create Result Classes

**New Classes**:

1. **`ScenarioExecutionResult.java`** - Overall scenario execution result
2. **`StageExecutionResult.java`** - Individual stage execution result

Following existing patterns from `RuleChainResult` and `YamlPipelineExecutionResult`.

```java
public class ScenarioExecutionResult {
    private String scenarioId;
    private boolean successful;
    private boolean terminated;
    private boolean requiresReview;
    private List<StageExecutionResult> stageResults;
    private List<String> warnings;
    private List<String> reviewFlags;
    private List<String> skippedStages;
    private long totalExecutionTimeMs;
    
    // Methods for stage result management
    public boolean isStageSuccessful(String stageName) { /* check stage success */ }
    public void addStageResult(StageExecutionResult result) { /* add result */ }
    public void addWarning(String warning) { /* add warning */ }
    public void addReviewFlag(String flag) { /* add review flag */ }
}

public class StageExecutionResult {
    private String stageName;
    private boolean successful;
    private String errorMessage;
    private RuleResult ruleResult;
    private long executionTimeMs;
    private Map<String, Object> stageOutputs;
    
    // Factory methods
    public static StageExecutionResult success(String stageName, RuleResult result) { /* create success */ }
    public static StageExecutionResult failure(String stageName, String error) { /* create failure */ }
    public static StageExecutionResult configurationError(String stageName, String error) { /* config error */ }
}
```

### 2.3 Enhance DataTypeScenarioService

**Modify**: `apex-core/src/main/java/dev/mars/apex/core/service/scenario/DataTypeScenarioService.java`

```java
public class DataTypeScenarioService {
    private ScenarioStageExecutor stageExecutor; // NEW
    
    public DataTypeScenarioService() {
        this.configLoader = new YamlConfigurationLoader();
        this.stageExecutor = new ScenarioStageExecutor(); // NEW
        this.scenarioCache = new ConcurrentHashMap<>();
        this.dataTypeToScenarios = new ConcurrentHashMap<>();
    }
    
    /**
     * NEW: Execute scenario with stage-aware processing
     */
    public ScenarioExecutionResult executeScenario(Object data) throws Exception {
        ScenarioConfiguration scenario = getScenarioForData(data);
        if (scenario == null) {
            throw new RuntimeException("No scenario found for data type: " + determineDataType(data));
        }
        
        logger.info("Executing scenario '{}' for data type '{}'", 
                   scenario.getScenarioId(), determineDataType(data));
        
        // Use stage executor if stages are defined, otherwise fall back to legacy processing
        if (scenario.hasStageConfiguration()) {
            logger.debug("Using stage-aware processing for scenario '{}'", scenario.getScenarioId());
            return stageExecutor.executeStages(scenario, data);
        } else {
            logger.debug("Using legacy processing for scenario '{}'", scenario.getScenarioId());
            return executeLegacyScenario(scenario, data); // Backward compatibility
        }
    }
    
    /**
     * Legacy scenario execution for backward compatibility
     */
    private ScenarioExecutionResult executeLegacyScenario(ScenarioConfiguration scenario, Object data) {
        // Convert legacy rule-configurations to simple stage execution
        ScenarioExecutionResult result = new ScenarioExecutionResult(scenario.getScenarioId());
        
        List<String> ruleFiles = scenario.getRuleConfigurations();
        for (int i = 0; i < ruleFiles.size(); i++) {
            String ruleFile = ruleFiles.get(i);
            try {
                // Execute rule file as a simple stage
                YamlRuleConfiguration config = configLoader.loadFromFile(ruleFile);
                RulesEngine engine = new RulesEngine(ruleFactory.createRulesEngineConfiguration(config));
                RuleResult ruleResult = engine.execute(Map.of("data", data));
                
                StageExecutionResult stageResult = StageExecutionResult.success("legacy-stage-" + i, ruleResult);
                result.addStageResult(stageResult);
                
            } catch (Exception e) {
                logger.error("Error executing legacy rule file '{}': {}", ruleFile, e.getMessage());
                StageExecutionResult stageResult = StageExecutionResult.failure("legacy-stage-" + i, e.getMessage());
                result.addStageResult(stageResult);
                break; // Terminate on first failure for legacy behavior
            }
        }
        
        return result;
    }
}
```

## Implementation Principles Applied

### From prompts.txt Coding Principles:

1. **Investigation Before Implementation** ✅
   - Thoroughly analyzed existing scenario processing patterns
   - Studied ComplexWorkflowExecutor and SequentialDependencyExecutor patterns
   - Examined existing YAML validation infrastructure

2. **Learn From Existing Patterns** ✅
   - Following RuleChainExecutor pattern for stage execution
   - Using YamlMetadataValidator pattern for validation
   - Adopting existing test patterns from DataTypeScenarioServiceTest

3. **Verify Assumptions** ✅
   - Each phase includes comprehensive testing
   - Integration tests verify real-world scenarios
   - Backward compatibility tests ensure no breaking changes

4. **Honest Error Handling** ✅
   - Configuration errors handled gracefully with warnings
   - Clear error messages for troubleshooting
   - Fail-safe defaults where appropriate

## Phase 3: YAML Validation Enhancement
*Duration: 2-3 days*

### 3.1 Extend YamlMetadataValidator

**Modify**: `apex-core/src/main/java/dev/mars/apex/core/util/YamlMetadataValidator.java`

```java
private void validateScenarioContent(Map<String, Object> yamlContent, YamlValidationResult result) {
    // Existing validation...

    // NEW: Validate processing stages if present
    Object scenarioObj = yamlContent.get("scenario");
    if (scenarioObj instanceof Map) {
        Map<String, Object> scenario = (Map<String, Object>) scenarioObj;

        if (scenario.containsKey("processing-stages")) {
            validateProcessingStages(scenario.get("processing-stages"), result);
        }

        // Validate that either processing-stages OR rule-configurations is present
        if (!scenario.containsKey("processing-stages") && !scenario.containsKey("rule-configurations")) {
            result.addError("Scenario must have either 'processing-stages' or 'rule-configurations'");
        }
    }
}

private void validateProcessingStages(Object stagesObj, YamlValidationResult result) {
    if (!(stagesObj instanceof List)) {
        result.addError("processing-stages must be a list");
        return;
    }

    List<?> stages = (List<?>) stagesObj;
    Set<String> stageNames = new HashSet<>();
    Set<Integer> executionOrders = new HashSet<>();
    Map<String, Set<String>> stageDependencies = new HashMap<>();

    for (Object stageObj : stages) {
        if (!(stageObj instanceof Map)) {
            result.addError("Each processing stage must be a map");
            continue;
        }

        Map<String, Object> stage = (Map<String, Object>) stageObj;

        // Validate required fields
        validateStageRequiredFields(stage, result);

        // Validate unique stage names and execution orders
        validateStageUniqueness(stage, stageNames, executionOrders, result);

        // Validate failure policies
        validateFailurePolicy(stage, result);

        // Collect dependencies for circular dependency check
        collectStageDependencies(stage, stageDependencies);
    }

    // Validate no circular dependencies
    validateNoCircularDependencies(stageDependencies, result);
}

private void validateStageRequiredFields(Map<String, Object> stage, YamlValidationResult result) {
    for (String requiredField : REQUIRED_STAGE_FIELDS) {
        if (!stage.containsKey(requiredField) || stage.get(requiredField) == null) {
            result.addError("Missing required stage field: " + requiredField);
        }
    }

    // Validate execution-order is a positive integer
    Object executionOrder = stage.get("execution-order");
    if (executionOrder != null) {
        if (!(executionOrder instanceof Integer) || (Integer) executionOrder < 1) {
            result.addError("execution-order must be a positive integer");
        }
    }
}

private void validateStageUniqueness(Map<String, Object> stage, Set<String> stageNames,
                                   Set<Integer> executionOrders, YamlValidationResult result) {
    String stageName = (String) stage.get("stage-name");
    if (stageName != null) {
        if (stageNames.contains(stageName)) {
            result.addError("Duplicate stage name: " + stageName);
        } else {
            stageNames.add(stageName);
        }
    }

    Integer executionOrder = (Integer) stage.get("execution-order");
    if (executionOrder != null) {
        if (executionOrders.contains(executionOrder)) {
            result.addError("Duplicate execution order: " + executionOrder);
        } else {
            executionOrders.add(executionOrder);
        }
    }
}

private void validateFailurePolicy(Map<String, Object> stage, YamlValidationResult result) {
    String failurePolicy = (String) stage.get("failure-policy");
    if (failurePolicy != null && !VALID_FAILURE_POLICIES.contains(failurePolicy)) {
        result.addError("Invalid failure policy: " + failurePolicy +
                       ". Valid policies: " + VALID_FAILURE_POLICIES);
    }
}

private void collectStageDependencies(Map<String, Object> stage, Map<String, Set<String>> stageDependencies) {
    String stageName = (String) stage.get("stage-name");
    Object dependsOnObj = stage.get("depends-on");

    if (stageName != null && dependsOnObj instanceof List) {
        List<?> dependsOn = (List<?>) dependsOnObj;
        Set<String> dependencies = new HashSet<>();
        for (Object dep : dependsOn) {
            if (dep instanceof String) {
                dependencies.add((String) dep);
            }
        }
        stageDependencies.put(stageName, dependencies);
    }
}

private void validateNoCircularDependencies(Map<String, Set<String>> stageDependencies, YamlValidationResult result) {
    // Simple cycle detection using DFS
    Set<String> visiting = new HashSet<>();
    Set<String> visited = new HashSet<>();

    for (String stage : stageDependencies.keySet()) {
        if (!visited.contains(stage)) {
            if (hasCycle(stage, stageDependencies, visiting, visited)) {
                result.addError("Circular dependency detected involving stage: " + stage);
                return;
            }
        }
    }
}

private boolean hasCycle(String stage, Map<String, Set<String>> dependencies,
                        Set<String> visiting, Set<String> visited) {
    if (visiting.contains(stage)) {
        return true; // Cycle detected
    }
    if (visited.contains(stage)) {
        return false; // Already processed
    }

    visiting.add(stage);
    Set<String> deps = dependencies.get(stage);
    if (deps != null) {
        for (String dep : deps) {
            if (hasCycle(dep, dependencies, visiting, visited)) {
                return true;
            }
        }
    }
    visiting.remove(stage);
    visited.add(stage);
    return false;
}
```

### 3.2 Add Stage-Specific Validation Constants

```java
private static final Set<String> VALID_FAILURE_POLICIES = Set.of(
    "terminate", "continue-with-warnings", "flag-for-review"
);

private static final Set<String> REQUIRED_STAGE_FIELDS = Set.of(
    "stage-name", "config-file", "execution-order"
);

private static final Set<String> OPTIONAL_STAGE_FIELDS = Set.of(
    "failure-policy", "depends-on", "required", "stage-metadata"
);
```

## Phase 4: Comprehensive Testing
*Duration: 3-4 days*

### 4.1 Unit Tests

**New Test Classes**:

1. **`ScenarioStageTest.java`** - Test stage configuration parsing and validation
2. **`ScenarioStageExecutorTest.java`** - Test stage execution logic with mocked dependencies
3. **`ScenarioExecutionResultTest.java`** - Test result aggregation and state management

**Enhanced Test Classes**:

1. **`DataTypeScenarioServiceTest.java`** - Add stage-aware processing tests
2. **`YamlMetadataValidatorTest.java`** - Add comprehensive stage validation tests

### 4.2 Integration Tests

**New Test Class**: `ScenarioStageIntegrationTest.java`

```java
@DisplayName("Scenario Stage Processing Integration Tests")
class ScenarioStageIntegrationTest extends DemoTestBase {

    private DataTypeScenarioService scenarioService;
    private Path tempConfigDir;

    @BeforeEach
    void setUp() throws Exception {
        scenarioService = new DataTypeScenarioService();
        tempConfigDir = createTempDirectory("stage-integration-test");
    }

    @Test
    @DisplayName("Should execute stages in dependency order")
    void testStageExecutionOrder() throws Exception {
        // Create test scenario with stages
        createStageBasedScenario();

        // Create test data
        TestOtcOption option = new TestOtcOption("CALL", "AAPL", 150.0);

        // Execute scenario
        ScenarioExecutionResult result = scenarioService.executeScenario(option);

        // Verify execution order
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "Scenario should execute successfully");

        List<StageExecutionResult> stageResults = result.getStageResults();
        assertEquals(3, stageResults.size(), "Should have 3 stage results");

        // Verify stages executed in correct order
        assertEquals("validation", stageResults.get(0).getStageName());
        assertEquals("enrichment", stageResults.get(1).getStageName());
        assertEquals("compliance", stageResults.get(2).getStageName());

        // Verify all stages were successful
        assertTrue(stageResults.stream().allMatch(StageExecutionResult::isSuccessful),
                  "All stages should be successful");
    }

    @Test
    @DisplayName("Should handle stage failure policies correctly")
    void testFailurePolicyHandling() throws Exception {
        // Create scenario with failing validation stage (terminate policy)
        createFailingValidationScenario();

        TestOtcOption invalidOption = new TestOtcOption(null, null, -100.0); // Invalid data

        ScenarioExecutionResult result = scenarioService.executeScenario(invalidOption);

        // Verify termination behavior
        assertFalse(result.isSuccessful(), "Scenario should fail");
        assertTrue(result.isTerminated(), "Scenario should be terminated");

        List<StageExecutionResult> stageResults = result.getStageResults();
        assertEquals(1, stageResults.size(), "Should only have validation stage result");
        assertEquals("validation", stageResults.get(0).getStageName());
        assertFalse(stageResults.get(0).isSuccessful(), "Validation stage should fail");
    }

    @Test
    @DisplayName("Should maintain backward compatibility with legacy configurations")
    void testBackwardCompatibility() throws Exception {
        // Create legacy scenario (rule-configurations only)
        createLegacyScenario();

        TestOtcOption option = new TestOtcOption("CALL", "AAPL", 150.0);

        ScenarioExecutionResult result = scenarioService.executeScenario(option);

        // Verify legacy processing works
        assertNotNull(result, "Legacy execution should work");
        assertTrue(result.isSuccessful(), "Legacy scenario should execute successfully");

        // Verify legacy stages were created
        List<StageExecutionResult> stageResults = result.getStageResults();
        assertFalse(stageResults.isEmpty(), "Should have stage results from legacy processing");

        // Verify stage names follow legacy pattern
        assertTrue(stageResults.get(0).getStageName().startsWith("legacy-stage-"),
                  "Legacy stages should have legacy naming pattern");
    }

    @Test
    @DisplayName("Should skip stages when dependencies are not satisfied")
    void testDependencySkipping() throws Exception {
        // Create scenario where validation fails but enrichment continues with warnings
        createContinueWithWarningsScenario();

        TestOtcOption problematicOption = new TestOtcOption("INVALID", "AAPL", 150.0);

        ScenarioExecutionResult result = scenarioService.executeScenario(problematicOption);

        // Verify warning behavior
        assertFalse(result.isSuccessful(), "Overall scenario should have issues");
        assertFalse(result.isTerminated(), "Scenario should not be terminated");
        assertTrue(result.hasWarnings(), "Should have warnings");

        // Verify enrichment stage was skipped due to validation failure
        assertTrue(result.getSkippedStages().contains("compliance"),
                  "Compliance stage should be skipped due to failed dependencies");
    }

    private void createStageBasedScenario() throws Exception {
        // Create scenario configuration with proper stages
        String scenarioYaml = """
            metadata:
              id: "stage-test-scenario"
              name: "Stage Test Scenario"
              version: "1.0.0"
              description: "Test scenario for stage processing"
              type: "scenario"
              business-domain: "Testing"
              owner: "test@example.com"

            scenario:
              scenario-id: "stage-test-scenario"
              name: "Stage Test Processing"
              description: "Test scenario with explicit stages"

              data-types:
                - "TestOtcOption"

              processing-stages:
                - stage-name: "validation"
                  config-file: "test-validation-rules.yaml"
                  execution-order: 1
                  failure-policy: "terminate"
                  required: true

                - stage-name: "enrichment"
                  config-file: "test-enrichment-rules.yaml"
                  execution-order: 2
                  failure-policy: "continue-with-warnings"
                  depends-on: ["validation"]

                - stage-name: "compliance"
                  config-file: "test-compliance-rules.yaml"
                  execution-order: 3
                  failure-policy: "flag-for-review"
                  depends-on: ["validation", "enrichment"]
            """;

        // Create rule configuration files
        createTestRuleFiles();

        // Save scenario and load it
        Path scenarioFile = tempConfigDir.resolve("stage-test-scenario.yaml");
        Files.writeString(scenarioFile, scenarioYaml);

        scenarioService.loadScenarios(scenarioFile.toString());
    }

    private void createTestRuleFiles() throws Exception {
        // Create simple test rule files that will pass
        String validationRules = """
            metadata:
              id: "test-validation-rules"
              name: "Test Validation Rules"
              version: "1.0.0"
              description: "Simple validation rules for testing"
              type: "rule-config"
              author: "test@example.com"

            rules:
              - id: "option-type-valid"
                name: "Option Type Valid"
                condition: "#data.optionType != null"
                message: "Option type must not be null"
            """;

        String enrichmentRules = """
            metadata:
              id: "test-enrichment-rules"
              name: "Test Enrichment Rules"
              version: "1.0.0"
              description: "Simple enrichment rules for testing"
              type: "rule-config"
              author: "test@example.com"

            enrichments:
              - id: "add-timestamp"
                name: "Add Processing Timestamp"
                condition: "true"
                action: "#data.processingTimestamp = new java.util.Date()"
            """;

        String complianceRules = """
            metadata:
              id: "test-compliance-rules"
              name: "Test Compliance Rules"
              version: "1.0.0"
              description: "Simple compliance rules for testing"
              type: "rule-config"
              author: "test@example.com"

            rules:
              - id: "strike-price-positive"
                name: "Strike Price Positive"
                condition: "#data.strikePrice > 0"
                message: "Strike price must be positive"
            """;

        Files.writeString(tempConfigDir.resolve("test-validation-rules.yaml"), validationRules);
        Files.writeString(tempConfigDir.resolve("test-enrichment-rules.yaml"), enrichmentRules);
        Files.writeString(tempConfigDir.resolve("test-compliance-rules.yaml"), complianceRules);
    }
}
```

### 4.3 Real-World Trade Processing Tests

**New Test Class**: `TradeProcessingWorkflowTest.java`

```java
@DisplayName("Trade Processing Workflow Tests")
class TradeProcessingWorkflowTest extends DemoTestBase {

    @Test
    @DisplayName("Should process OTC Option through complete workflow")
    void testOtcOptionCompleteWorkflow() throws Exception {
        // Create realistic OTC Option data
        OtcOption option = createRealisticOtcOption();

        // Load real scenario configuration
        DataTypeScenarioService scenarioService = new DataTypeScenarioService();
        scenarioService.loadScenarios("config/otc-options-scenarios.yaml");

        // Execute through validation -> enrichment -> compliance stages
        ScenarioExecutionResult result = scenarioService.executeScenario(option);

        // Verify each stage produces expected results
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "OTC Option processing should succeed");

        List<StageExecutionResult> stageResults = result.getStageResults();
        assertEquals(3, stageResults.size(), "Should have validation, enrichment, and compliance stages");

        // Verify validation stage
        StageExecutionResult validationResult = findStageResult(stageResults, "validation");
        assertNotNull(validationResult, "Validation stage should be executed");
        assertTrue(validationResult.isSuccessful(), "Validation should pass for valid option");

        // Verify enrichment stage
        StageExecutionResult enrichmentResult = findStageResult(stageResults, "enrichment");
        assertNotNull(enrichmentResult, "Enrichment stage should be executed");
        assertTrue(enrichmentResult.isSuccessful(), "Enrichment should succeed");

        // Verify compliance stage
        StageExecutionResult complianceResult = findStageResult(stageResults, "compliance");
        assertNotNull(complianceResult, "Compliance stage should be executed");
        assertTrue(complianceResult.isSuccessful(), "Compliance should pass");

        // Verify failure in validation prevents enrichment
        OtcOption invalidOption = createInvalidOtcOption();
        ScenarioExecutionResult failedResult = scenarioService.executeScenario(invalidOption);

        assertFalse(failedResult.isSuccessful(), "Invalid option should fail processing");
        assertTrue(failedResult.isTerminated(), "Processing should terminate on validation failure");

        List<StageExecutionResult> failedStageResults = failedResult.getStageResults();
        assertEquals(1, failedStageResults.size(), "Only validation stage should execute");
        assertEquals("validation", failedStageResults.get(0).getStageName());
        assertFalse(failedStageResults.get(0).isSuccessful(), "Validation should fail");
    }

    @Test
    @DisplayName("Should handle settlement instruction auto-repair workflow")
    void testSettlementAutoRepairWorkflow() throws Exception {
        // Test Asian markets settlement processing
        SettlementInstruction instruction = createAsianMarketSettlement();

        DataTypeScenarioService scenarioService = new DataTypeScenarioService();
        scenarioService.loadScenarios("config/settlement-scenarios.yaml");

        ScenarioExecutionResult result = scenarioService.executeScenario(instruction);

        // Verify stage dependencies and failure handling
        assertNotNull(result, "Settlement processing should complete");

        // For settlement auto-repair, we expect continue-with-warnings behavior
        if (!result.isSuccessful()) {
            assertTrue(result.hasWarnings(), "Failed settlement should have warnings");
            assertFalse(result.isTerminated(), "Settlement processing should continue with warnings");
        }
    }

    private OtcOption createRealisticOtcOption() {
        OtcOption option = new OtcOption();
        option.setOptionType("CALL");
        option.setUnderlying("AAPL");
        option.setStrikePrice(150.0);
        option.setExpirationDate(LocalDate.now().plusMonths(3));
        option.setNotionalAmount(1000000.0);
        option.setCounterparty("GOLDMAN_SACHS");
        return option;
    }

    private OtcOption createInvalidOtcOption() {
        OtcOption option = new OtcOption();
        option.setOptionType(null); // Invalid - missing option type
        option.setUnderlying("AAPL");
        option.setStrikePrice(-150.0); // Invalid - negative strike price
        return option;
    }

    private StageExecutionResult findStageResult(List<StageExecutionResult> results, String stageName) {
        return results.stream()
            .filter(r -> stageName.equals(r.getStageName()))
            .findFirst()
            .orElse(null);
    }
}
```

## Phase 5: Documentation and Migration
*Duration: 2-3 days*

### 5.1 Update Documentation

1. **Update `APEX_SCENARIO.md`** with stage configuration examples
2. **Update `APEX_YAML_REFERENCE.md`** with new YAML structure
3. **Create `SCENARIO_STAGE_MIGRATION_GUIDE.md`** for existing users

### 5.2 Enhanced APEX_SCENARIO.md Examples

```yaml
# Example: Complete Stage-Based Scenario Configuration
metadata:
  id: "otc-options-complete-processing"
  name: "OTC Options Complete Processing Pipeline"
  version: "2.0.0"
  description: "Complete multi-stage processing for OTC Options"
  type: "scenario"
  business-domain: "Derivatives Trading"
  owner: "derivatives.team@company.com"

scenario:
  scenario-id: "otc-options-complete-processing"
  name: "OTC Options Complete Processing"
  description: "Multi-stage validation, enrichment, and compliance pipeline"

  data-types:
    - "OtcOption"
    - "dev.mars.apex.demo.model.OtcOption"

  processing-stages:
    # Stage 1: Data Validation (Critical - must pass)
    - stage-name: "validation"
      config-file: "config/otc-options-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
      required: true
      stage-metadata:
        description: "Validates option data integrity and completeness"
        sla-ms: 500
        critical: true

    # Stage 2: Market Data Enrichment (Important - warn if fails)
    - stage-name: "market-data-enrichment"
      config-file: "config/market-data-enrichment-rules.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
      depends-on: ["validation"]
      stage-metadata:
        description: "Enriches option with current market data"
        sla-ms: 2000
        data-sources: ["bloomberg", "reuters"]

    # Stage 3: Financial Calculations (Important - warn if fails)
    - stage-name: "financial-calculations"
      config-file: "config/financial-calculation-rules.yaml"
      execution-order: 3
      failure-policy: "continue-with-warnings"
      depends-on: ["validation", "market-data-enrichment"]
      stage-metadata:
        description: "Calculates Greeks, PV, and risk metrics"
        sla-ms: 1500

    # Stage 4: Regulatory Compliance (Critical for reporting)
    - stage-name: "regulatory-compliance"
      config-file: "config/derivatives-compliance-rules.yaml"
      execution-order: 4
      failure-policy: "flag-for-review"
      depends-on: ["validation"]
      stage-metadata:
        description: "Validates regulatory compliance requirements"
        sla-ms: 1000
        regulations: ["EMIR", "Dodd-Frank", "MiFID II"]

    # Stage 5: Risk Assessment (Review if fails)
    - stage-name: "risk-assessment"
      config-file: "config/risk-assessment-rules.yaml"
      execution-order: 5
      failure-policy: "flag-for-review"
      depends-on: ["validation", "financial-calculations"]
      stage-metadata:
        description: "Assesses counterparty and market risk"
        sla-ms: 800
        risk-types: ["credit", "market", "operational"]
```

### 5.3 Migration Utilities

**New Utility**: `ScenarioConfigurationMigrator.java`

```java
/**
 * Utility to migrate legacy scenario configurations to stage-based configurations.
 *
 * Provides automated migration from simple rule-configurations to explicit
 * processing-stages with sensible defaults for stage metadata.
 */
public class ScenarioConfigurationMigrator {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioConfigurationMigrator.class);

    // Default stage configurations based on common patterns
    private static final Map<String, StageDefaults> STAGE_DEFAULTS = Map.of(
        "validation", new StageDefaults(1, "terminate", true, "Data validation and integrity checks"),
        "enrichment", new StageDefaults(2, "continue-with-warnings", false, "Data enrichment and augmentation"),
        "compliance", new StageDefaults(3, "flag-for-review", false, "Regulatory compliance validation")
    );

    public void migrateLegacyScenario(String scenarioFile) throws Exception {
        logger.info("Migrating legacy scenario file: {}", scenarioFile);

        // Read existing scenario
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        Map<String, Object> yamlContent = loader.loadYamlAsMap(scenarioFile);

        // Check if already migrated
        if (hasStageConfiguration(yamlContent)) {
            logger.info("Scenario already has stage configuration, skipping migration");
            return;
        }

        // Extract legacy rule-configurations
        List<String> ruleConfigurations = extractRuleConfigurations(yamlContent);
        if (ruleConfigurations.isEmpty()) {
            logger.warn("No rule-configurations found in scenario, nothing to migrate");
            return;
        }

        // Convert to processing stages
        List<Map<String, Object>> processingStages = convertToStages(ruleConfigurations);

        // Update YAML content
        updateYamlWithStages(yamlContent, processingStages);

        // Create backup
        createBackup(scenarioFile);

        // Write updated configuration
        writeUpdatedScenario(scenarioFile, yamlContent);

        logger.info("Successfully migrated scenario to stage-based configuration");
    }

    private boolean hasStageConfiguration(Map<String, Object> yamlContent) {
        Object scenarioObj = yamlContent.get("scenario");
        if (scenarioObj instanceof Map) {
            Map<String, Object> scenario = (Map<String, Object>) scenarioObj;
            return scenario.containsKey("processing-stages");
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRuleConfigurations(Map<String, Object> yamlContent) {
        Object scenarioObj = yamlContent.get("scenario");
        if (scenarioObj instanceof Map) {
            Map<String, Object> scenario = (Map<String, Object>) scenarioObj;
            Object ruleConfigsObj = scenario.get("rule-configurations");
            if (ruleConfigsObj instanceof List) {
                return (List<String>) ruleConfigsObj;
            }
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> convertToStages(List<String> ruleConfigurations) {
        List<Map<String, Object>> stages = new ArrayList<>();

        for (int i = 0; i < ruleConfigurations.size(); i++) {
            String ruleFile = ruleConfigurations.get(i);
            String stageName = inferStageName(ruleFile, i);
            StageDefaults defaults = STAGE_DEFAULTS.getOrDefault(stageName,
                new StageDefaults(i + 1, "continue-with-warnings", false, "Migrated stage"));

            Map<String, Object> stage = new HashMap<>();
            stage.put("stage-name", stageName);
            stage.put("config-file", ruleFile);
            stage.put("execution-order", defaults.executionOrder + i);
            stage.put("failure-policy", defaults.failurePolicy);
            stage.put("required", defaults.required);

            // Add dependencies for stages after the first
            if (i > 0) {
                stage.put("depends-on", List.of(inferStageName(ruleConfigurations.get(0), 0)));
            }

            // Add stage metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("description", defaults.description);
            metadata.put("migrated", true);
            metadata.put("original-order", i);
            stage.put("stage-metadata", metadata);

            stages.add(stage);
        }

        return stages;
    }

    private String inferStageName(String ruleFile, int index) {
        String fileName = Paths.get(ruleFile).getFileName().toString().toLowerCase();

        if (fileName.contains("validation") || fileName.contains("validate")) {
            return "validation";
        } else if (fileName.contains("enrichment") || fileName.contains("enrich")) {
            return "enrichment";
        } else if (fileName.contains("compliance") || fileName.contains("regulatory")) {
            return "compliance";
        } else if (fileName.contains("risk")) {
            return "risk-assessment";
        } else {
            return "stage-" + (index + 1);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateYamlWithStages(Map<String, Object> yamlContent, List<Map<String, Object>> processingStages) {
        Object scenarioObj = yamlContent.get("scenario");
        if (scenarioObj instanceof Map) {
            Map<String, Object> scenario = (Map<String, Object>) scenarioObj;

            // Add processing stages
            scenario.put("processing-stages", processingStages);

            // Keep legacy rule-configurations as comment for reference
            scenario.put("legacy-rule-configurations", scenario.get("rule-configurations"));
            scenario.remove("rule-configurations");
        }
    }

    private void createBackup(String scenarioFile) throws IOException {
        Path originalPath = Paths.get(scenarioFile);
        Path backupPath = originalPath.resolveSibling(originalPath.getFileName() + ".backup");
        Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Created backup: {}", backupPath);
    }

    private void writeUpdatedScenario(String scenarioFile, Map<String, Object> yamlContent) throws IOException {
        // Use YAML writer to maintain formatting
        Yaml yaml = new Yaml();
        try (FileWriter writer = new FileWriter(scenarioFile)) {
            yaml.dump(yamlContent, writer);
        }
    }

    private static class StageDefaults {
        final int executionOrder;
        final String failurePolicy;
        final boolean required;
        final String description;

        StageDefaults(int executionOrder, String failurePolicy, boolean required, String description) {
            this.executionOrder = executionOrder;
            this.failurePolicy = failurePolicy;
            this.required = required;
            this.description = description;
        }
    }
}
```

### 5.4 Migration Guide Document

**New Document**: `SCENARIO_STAGE_MIGRATION_GUIDE.md`

```markdown
# APEX Scenario Stage Migration Guide

## Overview

This guide helps you migrate existing APEX scenarios from simple `rule-configurations`
to explicit `processing-stages` configuration for better control over trade processing workflows.

## Benefits of Stage-Based Configuration

- **Explicit Dependencies**: Define which stages must complete before others
- **Failure Policy Control**: Different handling for validation vs. enrichment failures
- **Better Monitoring**: Track performance and success rates per stage
- **Workflow Clarity**: Clear understanding of processing pipeline

## Migration Process

### Automated Migration

Use the `ScenarioConfigurationMigrator` utility:

```java
ScenarioConfigurationMigrator migrator = new ScenarioConfigurationMigrator();
migrator.migrateLegacyScenario("config/my-scenario.yaml");
```

### Manual Migration

1. **Identify Current Rule Files**: List your existing `rule-configurations`
2. **Categorize by Purpose**: Group into validation, enrichment, compliance stages
3. **Define Dependencies**: Determine which stages depend on others
4. **Set Failure Policies**: Choose appropriate failure handling per stage
5. **Add Stage Metadata**: Include descriptions and SLA information

## Example Migration

### Before (Legacy Configuration)
```yaml
scenario:
  scenario-id: "otc-options-processing"
  rule-configurations:
    - "config/otc-validation-rules.yaml"
    - "config/market-data-enrichment.yaml"
    - "config/compliance-checks.yaml"
```

### After (Stage-Based Configuration)
```yaml
scenario:
  scenario-id: "otc-options-processing"
  processing-stages:
    - stage-name: "validation"
      config-file: "config/otc-validation-rules.yaml"
      execution-order: 1
      failure-policy: "terminate"
      required: true

    - stage-name: "enrichment"
      config-file: "config/market-data-enrichment.yaml"
      execution-order: 2
      failure-policy: "continue-with-warnings"
      depends-on: ["validation"]

    - stage-name: "compliance"
      config-file: "config/compliance-checks.yaml"
      execution-order: 3
      failure-policy: "flag-for-review"
      depends-on: ["validation"]
```

## Best Practices

1. **Start with Critical Stages**: Migrate validation stages first
2. **Test Incrementally**: Migrate one scenario at a time
3. **Monitor Performance**: Compare execution times before/after migration
4. **Document Dependencies**: Clearly document why stages depend on each other
5. **Use Meaningful Names**: Choose descriptive stage names that reflect business purpose

## Rollback Strategy

- Automatic backups are created during migration
- Legacy `rule-configurations` are preserved as comments
- Can revert by restoring backup files

## Validation

After migration, validate your scenarios:

```bash
# Run YAML validation
java -cp apex-core.jar dev.mars.apex.core.util.YamlMetadataValidator config/

# Run integration tests
mvn test -Dtest=ScenarioStageIntegrationTest
```

## Phase 6: Performance and Production Readiness
*Duration: 2-3 days*

### 6.1 Performance Testing

**New Test Class**: `ScenarioStagePerformanceTest.java`

```java
@DisplayName("Scenario Stage Performance Tests")
class ScenarioStagePerformanceTest extends DemoTestBase {

    private static final int PERFORMANCE_TEST_ITERATIONS = 1000;
    private static final long ACCEPTABLE_OVERHEAD_MS = 50; // Max 50ms overhead per scenario

    @Test
    @DisplayName("Should have minimal performance overhead compared to legacy processing")
    void testStageProcessingPerformance() throws Exception {
        // Setup identical scenarios - one legacy, one stage-based
        DataTypeScenarioService legacyService = createLegacyScenarioService();
        DataTypeScenarioService stageService = createStageBasedScenarioService();

        List<TestOtcOption> testData = createPerformanceTestData(PERFORMANCE_TEST_ITERATIONS);

        // Warm up JVM
        warmUpServices(legacyService, stageService, testData.subList(0, 100));

        // Measure legacy processing time
        long legacyStartTime = System.currentTimeMillis();
        for (TestOtcOption option : testData) {
            legacyService.executeScenario(option);
        }
        long legacyTotalTime = System.currentTimeMillis() - legacyStartTime;

        // Measure stage-based processing time
        long stageStartTime = System.currentTimeMillis();
        for (TestOtcOption option : testData) {
            stageService.executeScenario(option);
        }
        long stageTotalTime = System.currentTimeMillis() - stageStartTime;

        // Calculate overhead
        long overhead = stageTotalTime - legacyTotalTime;
        double overheadPerScenario = (double) overhead / PERFORMANCE_TEST_ITERATIONS;

        logger.info("Performance Test Results:");
        logger.info("  Legacy processing: {}ms total, {}ms per scenario",
                   legacyTotalTime, (double) legacyTotalTime / PERFORMANCE_TEST_ITERATIONS);
        logger.info("  Stage processing: {}ms total, {}ms per scenario",
                   stageTotalTime, (double) stageTotalTime / PERFORMANCE_TEST_ITERATIONS);
        logger.info("  Overhead: {}ms total, {}ms per scenario", overhead, overheadPerScenario);

        // Assert acceptable performance
        assertTrue(overheadPerScenario <= ACCEPTABLE_OVERHEAD_MS,
                  String.format("Stage processing overhead (%.2fms) exceeds acceptable limit (%dms)",
                               overheadPerScenario, ACCEPTABLE_OVERHEAD_MS));
    }

    @Test
    @DisplayName("Should handle complex dependency graphs efficiently")
    void testComplexDependencyPerformance() throws Exception {
        // Create scenario with complex dependency graph (5 stages, multiple dependencies)
        DataTypeScenarioService scenarioService = createComplexDependencyScenario();

        List<TestOtcOption> testData = createPerformanceTestData(500);

        long startTime = System.currentTimeMillis();
        for (TestOtcOption option : testData) {
            ScenarioExecutionResult result = scenarioService.executeScenario(option);
            assertTrue(result.isSuccessful(), "Complex dependency scenario should succeed");
        }
        long totalTime = System.currentTimeMillis() - startTime;

        double avgTimePerScenario = (double) totalTime / testData.size();

        logger.info("Complex Dependency Performance: {}ms total, {}ms per scenario",
                   totalTime, avgTimePerScenario);

        // Should complete within reasonable time even with complex dependencies
        assertTrue(avgTimePerScenario <= 100,
                  "Complex dependency processing should complete within 100ms per scenario");
    }

    @Test
    @DisplayName("Should not have memory leaks in stage result aggregation")
    void testMemoryUsage() throws Exception {
        DataTypeScenarioService scenarioService = createStageBasedScenarioService();

        // Force garbage collection and measure initial memory
        System.gc();
        Thread.sleep(100);
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Process many scenarios
        List<TestOtcOption> testData = createPerformanceTestData(2000);
        for (TestOtcOption option : testData) {
            ScenarioExecutionResult result = scenarioService.executeScenario(option);
            // Don't hold references to results to allow GC
        }

        // Force garbage collection and measure final memory
        System.gc();
        Thread.sleep(100);
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long memoryIncrease = finalMemory - initialMemory;
        double memoryIncreasePerScenario = (double) memoryIncrease / testData.size();

        logger.info("Memory Usage Test:");
        logger.info("  Initial memory: {}MB", initialMemory / (1024 * 1024));
        logger.info("  Final memory: {}MB", finalMemory / (1024 * 1024));
        logger.info("  Memory increase: {}MB, {}KB per scenario",
                   memoryIncrease / (1024 * 1024), memoryIncreasePerScenario / 1024);

        // Memory increase should be minimal (less than 1KB per scenario)
        assertTrue(memoryIncreasePerScenario <= 1024,
                  "Memory usage should not increase significantly with stage processing");
    }
}
```

### 6.2 Error Handling Enhancement

Following the **CRITICAL ERROR HANDLING PRINCIPLE** from prompts.txt:

```java
/**
 * Enhanced error handling for stage configuration and execution issues.
 *
 * Follows APEX principle: Configuration errors should be handled gracefully
 * with clear logging, not cause system failures.
 */
public class ScenarioStageErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioStageErrorHandler.class);

    /**
     * Handles stage configuration errors gracefully.
     */
    public StageExecutionResult handleConfigurationError(ScenarioStage stage, Exception error) {
        String errorMessage = String.format("Configuration error in stage '%s': %s",
                                           stage.getStageName(), error.getMessage());

        logger.warn("{}. Applying default behavior.", errorMessage);

        // Log detailed error for troubleshooting
        if (logger.isDebugEnabled()) {
            logger.debug("Stage configuration details: {}", stage, error);
        }

        // Return error result but don't throw exception
        return StageExecutionResult.configurationError(stage.getStageName(), errorMessage);
    }

    /**
     * Handles stage execution errors with appropriate recovery strategies.
     */
    public StageExecutionResult handleExecutionError(ScenarioStage stage, Object data, Exception error) {
        String errorMessage = String.format("Execution error in stage '%s': %s",
                                           stage.getStageName(), error.getMessage());

        logger.error("{}. Data type: {}", errorMessage, data.getClass().getSimpleName(), error);

        // Apply recovery strategy based on stage importance
        if (stage.isRequired()) {
            logger.error("Required stage '{}' failed - scenario cannot continue", stage.getStageName());
            return StageExecutionResult.criticalFailure(stage.getStageName(), errorMessage);
        } else {
            logger.warn("Optional stage '{}' failed - scenario will continue", stage.getStageName());
            return StageExecutionResult.nonCriticalFailure(stage.getStageName(), errorMessage);
        }
    }

    /**
     * Handles dependency resolution errors.
     */
    public void handleDependencyError(ScenarioStage stage, List<String> unsatisfiedDependencies) {
        String errorMessage = String.format("Stage '%s' has unsatisfied dependencies: %s",
                                           stage.getStageName(), unsatisfiedDependencies);

        logger.warn("{}. Stage will be skipped.", errorMessage);

        // Log suggestions for fixing dependency issues
        if (logger.isInfoEnabled()) {
            logger.info("To fix dependency issues for stage '{}': ", stage.getStageName());
            logger.info("  1. Check that dependent stages are defined in the scenario");
            logger.info("  2. Verify execution-order values are correct");
            logger.info("  3. Ensure dependent stages completed successfully");
        }
    }

    /**
     * Handles circular dependency detection.
     */
    public void handleCircularDependency(List<String> dependencyCycle) {
        String errorMessage = String.format("Circular dependency detected: %s",
                                           String.join(" -> ", dependencyCycle));

        logger.error("{}. This is a configuration error that must be fixed.", errorMessage);

        // Provide specific guidance for fixing circular dependencies
        logger.error("To fix circular dependencies:");
        logger.error("  1. Review the 'depends-on' configuration for stages: {}", dependencyCycle);
        logger.error("  2. Remove unnecessary dependencies");
        logger.error("  3. Consider splitting stages if they have mutual dependencies");
        logger.error("  4. Use execution-order to define sequence without circular references");

        throw new ScenarioConfigurationException(errorMessage);
    }
}
```

### 6.3 Production Monitoring and Metrics

```java
/**
 * Metrics collection for scenario stage processing.
 *
 * Provides detailed metrics for monitoring stage performance,
 * success rates, and failure patterns in production.
 */
public class ScenarioStageMetrics {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioStageMetrics.class);

    // Metrics storage (in production, would integrate with monitoring system)
    private final Map<String, StageMetrics> stageMetrics = new ConcurrentHashMap<>();
    private final Map<String, ScenarioMetrics> scenarioMetrics = new ConcurrentHashMap<>();

    /**
     * Records stage execution metrics.
     */
    public void recordStageExecution(String scenarioId, StageExecutionResult stageResult) {
        String stageKey = scenarioId + ":" + stageResult.getStageName();

        stageMetrics.computeIfAbsent(stageKey, k -> new StageMetrics())
                   .recordExecution(stageResult);

        scenarioMetrics.computeIfAbsent(scenarioId, k -> new ScenarioMetrics())
                      .recordStageExecution(stageResult);
    }

    /**
     * Records overall scenario execution metrics.
     */
    public void recordScenarioExecution(ScenarioExecutionResult scenarioResult) {
        scenarioMetrics.computeIfAbsent(scenarioResult.getScenarioId(), k -> new ScenarioMetrics())
                      .recordScenarioExecution(scenarioResult);
    }

    /**
     * Gets performance summary for monitoring dashboards.
     */
    public Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Overall statistics
        summary.put("totalScenarios", scenarioMetrics.size());
        summary.put("totalStages", stageMetrics.size());

        // Success rates
        double avgScenarioSuccessRate = scenarioMetrics.values().stream()
            .mapToDouble(ScenarioMetrics::getSuccessRate)
            .average()
            .orElse(0.0);
        summary.put("avgScenarioSuccessRate", avgScenarioSuccessRate);

        // Performance metrics
        double avgExecutionTime = scenarioMetrics.values().stream()
            .mapToDouble(ScenarioMetrics::getAvgExecutionTime)
            .average()
            .orElse(0.0);
        summary.put("avgExecutionTimeMs", avgExecutionTime);

        // Top failing stages
        List<String> topFailingStages = stageMetrics.entrySet().stream()
            .filter(entry -> entry.getValue().getFailureRate() > 0.1) // > 10% failure rate
            .sorted((e1, e2) -> Double.compare(e2.getValue().getFailureRate(), e1.getValue().getFailureRate()))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        summary.put("topFailingStages", topFailingStages);

        return summary;
    }

    /**
     * Logs performance summary for monitoring.
     */
    public void logPerformanceSummary() {
        Map<String, Object> summary = getPerformanceSummary();

        logger.info("=== Scenario Stage Performance Summary ===");
        logger.info("Total Scenarios: {}", summary.get("totalScenarios"));
        logger.info("Total Stages: {}", summary.get("totalStages"));
        logger.info("Average Success Rate: {:.2f}%", (Double) summary.get("avgScenarioSuccessRate") * 100);
        logger.info("Average Execution Time: {:.2f}ms", summary.get("avgExecutionTimeMs"));

        @SuppressWarnings("unchecked")
        List<String> failingStages = (List<String>) summary.get("topFailingStages");
        if (!failingStages.isEmpty()) {
            logger.warn("Top Failing Stages: {}", failingStages);
        }

        logger.info("==========================================");
    }

    private static class StageMetrics {
        private long executionCount = 0;
        private long successCount = 0;
        private long totalExecutionTime = 0;

        synchronized void recordExecution(StageExecutionResult result) {
            executionCount++;
            if (result.isSuccessful()) {
                successCount++;
            }
            totalExecutionTime += result.getExecutionTimeMs();
        }

        double getSuccessRate() {
            return executionCount > 0 ? (double) successCount / executionCount : 0.0;
        }

        double getFailureRate() {
            return 1.0 - getSuccessRate();
        }

        double getAvgExecutionTime() {
            return executionCount > 0 ? (double) totalExecutionTime / executionCount : 0.0;
        }
    }

    private static class ScenarioMetrics {
        private long executionCount = 0;
        private long successCount = 0;
        private long totalExecutionTime = 0;
        private final Map<String, Long> stageFailureCounts = new ConcurrentHashMap<>();

        synchronized void recordScenarioExecution(ScenarioExecutionResult result) {
            executionCount++;
            if (result.isSuccessful()) {
                successCount++;
            }
            totalExecutionTime += result.getTotalExecutionTimeMs();
        }

        synchronized void recordStageExecution(StageExecutionResult stageResult) {
            if (!stageResult.isSuccessful()) {
                stageFailureCounts.merge(stageResult.getStageName(), 1L, Long::sum);
            }
        }

        double getSuccessRate() {
            return executionCount > 0 ? (double) successCount / executionCount : 0.0;
        }

        double getAvgExecutionTime() {
            return executionCount > 0 ? (double) totalExecutionTime / executionCount : 0.0;
        }
    }
}
```

## Risk Mitigation Summary

### 1. Backward Compatibility
- **Legacy Support**: Existing `rule-configurations` continue to work unchanged
- **Gradual Migration**: Users can adopt stage configuration incrementally
- **Automatic Fallback**: System automatically detects and handles legacy configurations

### 2. Performance Assurance
- **Minimal Overhead**: Stage processing adds less than 50ms overhead per scenario
- **Memory Efficiency**: No memory leaks in stage result aggregation
- **Scalability**: Complex dependency graphs handled efficiently

### 3. Error Resilience
- **Graceful Degradation**: Configuration errors don't cause system failures
- **Clear Diagnostics**: Detailed error messages for troubleshooting
- **Recovery Strategies**: Different failure policies for different stage types

### 4. Production Readiness
- **Comprehensive Monitoring**: Detailed metrics for stage performance and success rates
- **Automated Testing**: Full test coverage including integration and performance tests
- **Migration Tools**: Automated migration utilities with backup and rollback capabilities

## Implementation Timeline Summary

- **Phase 1** (3-5 days): Core data model and YAML structure
- **Phase 2** (4-6 days): Processing engine and execution logic
- **Phase 3** (2-3 days): YAML validation enhancement
- **Phase 4** (3-4 days): Comprehensive testing suite
- **Phase 5** (2-3 days): Documentation and migration tools
- **Phase 6** (2-3 days): Performance optimization and production readiness

**Total Estimated Duration: 16-24 days**

This implementation plan provides a robust, production-ready solution for explicit stage configuration in APEX scenarios while maintaining full backward compatibility and following established APEX architectural patterns.
```
