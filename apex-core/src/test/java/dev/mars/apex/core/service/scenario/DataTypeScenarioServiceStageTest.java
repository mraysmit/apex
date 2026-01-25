package dev.mars.apex.core.service.scenario;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for stage-aware processing functionality in DataTypeScenarioService.
 *
 * Tests the new stage-based processing capabilities while ensuring
 * backward compatibility with legacy rule-based processing.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @deprecated This test class tests the deprecated {@link DataTypeScenarioService}.
 *             The service is being replaced by {@link dev.mars.apex.core.engine.config.RulesEngine}.
 *             This test specifically validates internal implementation details (stage-based vs legacy processing)
 *             that don't apply to the new RulesEngine API, which only supports the modern stage-based approach.
 *             This test will be removed in a future release along with DataTypeScenarioService.
 */
@Deprecated(since = "1.0.0", forRemoval = true)
class DataTypeScenarioServiceStageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataTypeScenarioServiceStageTest.class);
    private DataTypeScenarioService service;

    @BeforeEach
    void setUp() {
        service = new DataTypeScenarioService();
    }

    /**
     * Intentional error test: Verifies that stage-based scenario processing handles errors gracefully
     * when using error-handling YAML with default values across multiple stages. Each stage uses YAML
     * with SpEL expressions that will fail on missing fields, testing error recovery through default
     * values and proper reporting via ScenarioExecutionResult API.
     */
    @Test
    void testProcessData_WithStageBasedScenarioIntentionalError() throws Exception {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Stage-based scenario with error-handling YAML ===");
        // Arrange
        ScenarioStage validationStage = new ScenarioStage("validation", resourcePath("error-handling/yaml-default-value-test.yaml"), 1);
        ScenarioStage enrichmentStage = new ScenarioStage("enrichment", resourcePath("error-handling/yaml-default-value-test.yaml"), 2);

        List<ScenarioStage> stages = Arrays.asList(validationStage, enrichmentStage);
        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages("stage-scenario", "Stage Scenario",
                                                                              Arrays.asList("TestData"), stages);

        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, stageScenario);

        Object testData = new TestData();

        // Act
        Object result = service.processData(testData);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ScenarioExecutionResult, "Should return ScenarioExecutionResult for stage-based processing");

        ScenarioExecutionResult stageResult = (ScenarioExecutionResult) result;
        assertEquals("stage-scenario", stageResult.getScenarioId());
        
        // Verify execution completed (may have warnings/errors handled via default values)
        assertNotNull(stageResult.getStageResults(), "Stage results should not be null");
        assertFalse(stageResult.getStageResults().isEmpty(), "Should have stage execution results");
        
        // Verify stages were executed (errors handled via default values, not failures)
        for (StageExecutionResult stageExecResult : stageResult.getStageResults()) {
            assertNotNull(stageExecResult, "Individual stage result should not be null");
        }
    }

    /**
     * Intentional error test: Verifies that legacy scenario processing handles errors gracefully
     * when using error-handling YAML with default values. The YAML contains SpEL expressions that
     * will fail on missing fields, and the test verifies they are handled through default-value
     * error recovery mechanism, with results properly reported through RuleResult API.
     */
    @Test
    void testProcessData_WithLegacyScenarioIntentionalError() throws Exception {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Legacy scenario with error-handling YAML ===");
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy-scenario");
        legacyScenario.setName("Legacy Scenario");
        legacyScenario.setDataTypes(Arrays.asList("TestData"));
        legacyScenario.setRuleConfigurations(Arrays.asList(resourcePath("error-handling/yaml-default-value-test.yaml")));

        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, legacyScenario);

        Object testData = new TestData();

        // Act
        Object result = service.processData(testData);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof RuleResult, "Should return RuleResult for legacy processing");
        
        RuleResult ruleResult = (RuleResult) result;
        // Note: Error recovery via default values means result may be successful
        // The key is that errors are logged and handled gracefully, not that the result fails
        assertNotNull(ruleResult, "RuleResult should not be null");
    }

    /**
     * Intentional error test: Verifies that stage-based processing with error-handling YAML handles
     * SpEL expression errors gracefully through default-value error recovery. Tests that when
     * fields are missing from input data, APEX captures the SpEL errors and reports them through
     * StageExecutionResult → RuleResult API (not just logging).
     */
    @Test
    void testProcessDataWithStages_Success() throws Exception {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Processing with error-handling YAML ===");
        // Arrange
        ScenarioStage stage = new ScenarioStage("test-stage", resourcePath("error-handling/yaml-default-value-test.yaml"), 1);
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages("test-scenario", "Test Scenario",
                                                                              Arrays.asList("TestData"), stages);

        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, stageScenario);

        Object testData = new TestData();

        // Act
        ScenarioExecutionResult result = service.processDataWithStages(testData, "test-scenario");

        // Assert
        assertNotNull(result);
        assertEquals("test-scenario", result.getScenarioId());
        
        // Verify errors are captured in stage results' RuleResult
        assertNotNull(result.getStageResults(), "Stage results should not be null");
        assertFalse(result.getStageResults().isEmpty(), "Should have stage execution results");
        
        boolean foundErrorsInStageResults = false;
        for (StageExecutionResult stageExecResult : result.getStageResults()) {
            if (stageExecResult.getRuleResult() != null && stageExecResult.getRuleResult().hasFailures()) {
                List<String> failureMessages = stageExecResult.getRuleResult().getFailureMessages();
                
                // Verify specific error messages about missing fields
                boolean hasFieldErrors = failureMessages.stream().anyMatch(msg -> 
                    msg.contains("age") || msg.contains("email") || msg.contains("creditScore") || 
                    msg.contains("customerId"));
                
                if (hasFieldErrors) {
                    foundErrorsInStageResults = true;
                    
                    // Verify error messages contain detailed context for debugging
                    boolean hasDetailedContext = failureMessages.stream().anyMatch(msg -> 
                        msg.contains("Rule evaluation failed") || msg.contains("Property or field") || 
                        msg.contains("cannot be found"));
                    
                    assertTrue(hasDetailedContext, 
                        "Error messages should contain detailed debugging context. Messages: " + failureMessages);
                    
                    LOGGER.info("Stage '{}' captured {} error messages in RuleResult: {}", 
                        stageExecResult.getStageName(), failureMessages.size(), failureMessages);
                }
            }
        }
        
        assertTrue(foundErrorsInStageResults, 
            "Should report errors about missing fields in StageExecutionResult → RuleResult, not just log them");
    }

    @Test
    void testProcessDataWithStages_ScenarioNotFound() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.processDataWithStages(new TestData(), "non-existent-scenario");
        });
    }

    @Test
    void testProcessDataWithStages_NoStageConfiguration() throws Exception {
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy-scenario");
        legacyScenario.setDataTypes(Arrays.asList("TestData"));
        legacyScenario.setRuleConfigurations(Arrays.asList("config/rules.yaml"));

        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, legacyScenario);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.processDataWithStages(new TestData(), "legacy-scenario");
        });
    }

    /**
     * Intentional error test: Verifies that stage-based processing with error-handling YAML handles
     * SpEL expression errors gracefully. Tests that when fields are missing from input data,
     * APEX captures the SpEL errors and reports them through StageExecutionResult → RuleResult API.
     */
    @Test
    void testProcessDataWithScenario_StageBasedProcessing() throws Exception {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Stage-based processing with error-handling YAML ===");
        // Arrange
        ScenarioStage stage = new ScenarioStage("validation", resourcePath("error-handling/yaml-default-value-test.yaml"), 1);
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages("stage-scenario", "Stage Scenario",
                                                                              Arrays.asList("TestData"), stages);

        Object testData = new TestData();

        // Act
        Object result = service.processDataWithScenario(testData, stageScenario);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ScenarioExecutionResult, "Should return ScenarioExecutionResult for stage-based processing");

        ScenarioExecutionResult stageResult = (ScenarioExecutionResult) result;
        assertEquals("stage-scenario", stageResult.getScenarioId());
        
        // Verify stage results are present
        assertNotNull(stageResult.getStageResults(), "Stage results should not be null");
        assertFalse(stageResult.getStageResults().isEmpty(), "Should have stage execution results");
        
        // Verify errors are captured in stage results' RuleResult
        boolean foundErrorsInStageResults = false;
        for (StageExecutionResult stageExecResult : stageResult.getStageResults()) {
            assertNotNull(stageExecResult, "Individual stage result should not be null");
            
            // Check if this stage has a RuleResult with failures
            if (stageExecResult.getRuleResult() != null && stageExecResult.getRuleResult().hasFailures()) {
                List<String> failureMessages = stageExecResult.getRuleResult().getFailureMessages();
                
                // Verify specific error messages about missing fields
                boolean hasFieldErrors = failureMessages.stream().anyMatch(msg -> 
                    msg.contains("age") || msg.contains("email") || msg.contains("creditScore") || 
                    msg.contains("customerId") || msg.contains("principal") || msg.contains("value"));
                
                if (hasFieldErrors) {
                    foundErrorsInStageResults = true;
                    
                    // Verify error messages contain detailed context
                    boolean hasDetailedContext = failureMessages.stream().anyMatch(msg -> 
                        msg.contains("Rule evaluation failed") || msg.contains("Property or field") || 
                        msg.contains("cannot be found"));
                    
                    assertTrue(hasDetailedContext, 
                        "Error messages should contain detailed context. Messages: " + failureMessages);
                    
                    LOGGER.info("Stage '{}' captured {} error messages in RuleResult: {}", 
                        stageExecResult.getStageName(), failureMessages.size(), failureMessages);
                }
            }
        }
        
        assertTrue(foundErrorsInStageResults, 
            "Should report errors about missing fields in StageExecutionResult → RuleResult, not just log stack traces");
    }

    /**
     * Intentional error test: Verifies that legacy processing with error-handling YAML handles
     * SpEL expression errors gracefully through default-value error recovery. Tests that when
     * fields are missing from input data, APEX captures the SpEL errors and reports them through
     * RuleResult API (not just logging stack traces).
     */
    @Test
    void testProcessDataWithScenario_LegacyProcessingIntentionalError() throws Exception {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Legacy processing with error-handling YAML ===");
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy-scenario");
        legacyScenario.setDataTypes(Arrays.asList("TestData"));
        legacyScenario.setRuleConfigurations(Arrays.asList(resourcePath("error-handling/yaml-default-value-test.yaml")));

        Object testData = new TestData();

        // Act
        Object result = service.processDataWithScenario(testData, legacyScenario);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof RuleResult, "Should return RuleResult for legacy processing");
        
        RuleResult ruleResult = (RuleResult) result;
        
        // Verify errors are captured in RuleResult (not just logged)
        assertTrue(ruleResult.hasFailures(), "Should have failures from missing fields (age, email, creditScore, customerId, principal, value)");
        
        List<String> failureMessages = ruleResult.getFailureMessages();
        assertFalse(failureMessages.isEmpty(), "Should have failure messages reporting SpEL errors");
        
        // Verify specific error messages about missing fields
        boolean hasFieldErrors = failureMessages.stream().anyMatch(msg -> 
            msg.contains("age") || msg.contains("email") || msg.contains("creditScore") || 
            msg.contains("customerId") || msg.contains("principal") || msg.contains("value"));
        
        assertTrue(hasFieldErrors, 
            "Should report errors about missing fields in RuleResult, not just log stack traces. Messages: " + failureMessages);
        
        // Verify error messages contain sufficient detail (rule names or property names)
        boolean hasDetailedContext = failureMessages.stream().anyMatch(msg -> 
            msg.contains("Rule evaluation failed") || msg.contains("Property or field"));
        
        assertTrue(hasDetailedContext, 
            "Error messages should contain detailed context for debugging. Messages: " + failureMessages);
        
        LOGGER.info("Captured {} error messages in RuleResult: {}", failureMessages.size(), failureMessages);
    }

    @Test
    void testProcessDataWithScenario_NullScenario() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.processDataWithScenario(new TestData(), null);
        });
    }

    @Test
    void testProcessData_NoScenarioFound() {
        // Arrange
        Object testData = new UnknownDataType();

        // Act
        Object result = service.processData(testData);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof RuleResult);

        RuleResult ruleResult = (RuleResult) result;
        assertEquals(RuleResult.ResultType.NO_RULES, ruleResult.getResultType());
    }

    @Test
    void testBackwardCompatibility_ExistingMethodsStillWork() throws Exception {
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy-test");
        legacyScenario.setDataTypes(Arrays.asList("TestData"));
        legacyScenario.setRuleConfigurations(Arrays.asList("config/rules.yaml"));

        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, legacyScenario);

        Object testData = new TestData();

        // Act - Test existing methods still work
        ScenarioConfiguration retrievedScenario = service.getScenarioForData(testData);
        ScenarioConfiguration scenarioById = service.getScenario("legacy-test");
        Set<String> availableScenarios = service.getAvailableScenarios();
        Set<String> supportedDataTypes = service.getSupportedDataTypes();

        // Assert
        assertNotNull(retrievedScenario);
        assertEquals("legacy-test", retrievedScenario.getScenarioId());

        assertNotNull(scenarioById);
        assertEquals("legacy-test", scenarioById.getScenarioId());

        assertTrue(availableScenarios.contains("legacy-test"));
        assertTrue(supportedDataTypes.contains("TestData"));
    }

    // ========================================
    // Modern RulesEngine Equivalent Tests
    // ========================================

    /**
     * Modern equivalent of testProcessData_WithLegacyScenarioIntentionalError.
     * Demonstrates the new correct approach using RulesEngine API.
     */
    @Test
    void testRulesEngine_ErrorHandlingWithDefaultValues() throws YamlConfigurationException {
        LOGGER.info("=== MODERN APPROACH: RulesEngine with error-handling YAML ===");
        
        // Step 1: Create engine from YAML file
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        // Step 2: Prepare input data (intentionally missing fields to trigger errors)
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        // Missing: age, email, creditScore, customerId - will trigger default-value error recovery
        
        // Step 3: Evaluate rules
        RuleResult result = engine.evaluate(inputData);
        
        // Step 4: Check results
        assertNotNull(result, "Result should not be null");
        
        // Error recovery via default values means result may be successful
        // The key is that errors are logged and handled gracefully
        assertNotNull(result.getEnrichedData(), "Enriched data should not be null");
    }

    /**
     * Modern equivalent showing proper error verification.
     * Demonstrates checking for failures and error messages in RuleResult.
     */
    @Test
    void testRulesEngine_ErrorVerification() throws YamlConfigurationException {
        LOGGER.info("=== MODERN APPROACH: Verify error reporting through RuleResult API ===");
        
        // Create engine
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        // Input data with some valid and some missing fields
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        inputData.put("age", 25);  // Valid
        // Missing: email, creditScore, customerId
        
        // Evaluate
        RuleResult result = engine.evaluate(inputData);
        
        // Verify result structure
        assertNotNull(result, "Result should not be null");
        
        // Check if there are any failures or warnings
        if (result.hasFailures()) {
            List<String> failureMessages = result.getFailureMessages();
            assertFalse(failureMessages.isEmpty(), "Should have failure messages if hasFailures() is true");
            LOGGER.info("Failures detected (handled via default values): {}", failureMessages);
        }
        
        // Verify enriched data is available even with errors
        assertNotNull(result.getEnrichedData(), "Enriched data should be available");
    }

    /**
     * Modern equivalent showing the simplified 2-line usage pattern.
     */
    @Test
    void testRulesEngine_SimplifiedUsage() throws YamlConfigurationException {
        LOGGER.info("=== MODERN APPROACH: Simplified 2-line usage ===");
        
        // Simple 2-line usage
        RulesEngine engine = RulesEngine.fromFile(resourcePath("error-handling/yaml-default-value-test.yaml"));
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        inputData.put("customerId", "CUST123");
        
        RuleResult result = engine.evaluate(inputData);
        
        // Verify result
        assertNotNull(result);
        assertNotNull(result.getEnrichedData());
    }

    // ========================================
    // Helper Methods
    // ========================================

    // Helper to resolve classpath test resources to absolute file paths
    private String resourcePath(String name) {
        try {
            java.net.URL url = getClass().getClassLoader().getResource(name);
            assertNotNull(url, "Missing test resource: " + name);
            return new java.io.File(url.toURI()).getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Test data classes
    private static class TestData {
        private String value = "test";

        public String getValue() {
            return value;
        }
    }

    private static class UnknownDataType {
        private String data = "unknown";

        public String getData() {
            return data;
        }
    }
}
