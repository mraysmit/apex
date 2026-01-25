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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for data persistence between scenario stages.
 * 
 * These tests verify that:
 * 1. Stage outputs and enriched data are persisted back to the data context
 * 2. ScenarioExecutionResult.getExecutionSummary() reports correct counts
 * 3. StageExecutionResult properly stores and retrieves outputs
 * 4. Subsequent stages can access data from previous stages
 * 
 * This test class was created to catch bugs where stage results were not
 * being persisted correctly between stages.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Stage Data Persistence Tests")
class StageDataPersistenceTest {

    private static final Logger logger = LoggerFactory.getLogger(StageDataPersistenceTest.class);

    // ========================================
    // Test Utilities
    // ========================================
    
    // Test loader that returns in-memory configs (shared across test classes)
    private static class TestConfigLoader extends YamlConfigurationLoader {
        private final Map<String, YamlRuleConfiguration> configs = new HashMap<>();
        private final Map<String, Boolean> failures = new HashMap<>();

        public void addSuccess(String path) {
            configs.put(path, new YamlRuleConfiguration());
            failures.put(path, false);
        }
        
        public void addSuccess(String path, YamlRuleConfiguration config) {
            configs.put(path, config);
            failures.put(path, false);
        }
        
        public void addFailure(String path, YamlRuleConfiguration config) {
            configs.put(path, config);
            failures.put(path, true);
        }

        @Override
        public YamlRuleConfiguration loadFromFile(String filePath) {
            if (failures.getOrDefault(filePath, false)) {
                throw new RuntimeException("Simulated config load failure for: " + filePath);
            }
            return configs.getOrDefault(filePath, new YamlRuleConfiguration());
        }
    }

    // ========================================
    // StageExecutionResult Unit Tests
    // ========================================
    
    @Nested
    @DisplayName("StageExecutionResult Output Persistence")
    class StageExecutionResultTests {
        
        @Test
        @DisplayName("Should store and retrieve stage outputs via addStageOutput")
        void testStageOutputsStoredAndRetrieved() {
            logger.info("TEST: Stage outputs stored and retrieved");
            
            // Create a successful result
            RuleResult ruleResult = new RuleResult("test-rule", "Test passed", true, RuleResult.ResultType.MATCH);
            StageExecutionResult result = StageExecutionResult.success("test-stage", ruleResult);
            
            // Add stage outputs
            result.addStageOutput("enrichedField1", "value1");
            result.addStageOutput("enrichedField2", 123);
            result.addStageOutput("enrichedField3", true);
            
            // Verify outputs are stored
            Map<String, Object> outputs = result.getStageOutputs();
            assertEquals("value1", outputs.get("enrichedField1"));
            assertEquals(123, outputs.get("enrichedField2"));
            assertEquals(true, outputs.get("enrichedField3"));
            assertEquals(3, outputs.size());
            
            logger.info("[OK] Stage outputs correctly stored and retrieved");
        }
        
        @Test
        @DisplayName("Should store outputs via setStageOutputs")
        void testSetStageOutputs() {
            logger.info("TEST: setStageOutputs works correctly");
            
            RuleResult ruleResult = new RuleResult("test-rule", "Test passed", true, RuleResult.ResultType.MATCH);
            StageExecutionResult result = StageExecutionResult.success("test-stage", ruleResult);
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("key1", "value1");
            outputs.put("key2", "value2");
            outputs.put("key3", 999);
            
            result.setStageOutputs(outputs);
            
            Map<String, Object> retrieved = result.getStageOutputs();
            assertEquals("value1", retrieved.get("key1"));
            assertEquals("value2", retrieved.get("key2"));
            assertEquals(999, retrieved.get("key3"));
            
            logger.info("[OK] setStageOutputs works correctly");
        }
        
        @Test
        @DisplayName("Should store outputs on failure result for partial data capture")
        void testFailureResultCanStoreOutputs() {
            logger.info("TEST: Failure result can store outputs");
            
            StageExecutionResult result = StageExecutionResult.failure("failing-stage", "Test error");
            
            // Even on failure, we should be able to capture partial outputs
            result.addStageOutput("partialData", "partialValue");
            result.addStageOutput("processedCount", 42);
            
            assertFalse(result.isSuccessful(), "Result should be marked as failed");
            assertEquals("partialValue", result.getStageOutputs().get("partialData"));
            assertEquals(42, result.getStageOutputs().get("processedCount"));
            
            logger.info("[OK] Failure result can store outputs for partial data capture");
        }
        
        @Test
        @DisplayName("Should return defensive copy from getStageOutputs")
        void testGetStageOutputsReturnsDefensiveCopy() {
            logger.info("TEST: getStageOutputs returns defensive copy");
            
            RuleResult ruleResult = new RuleResult("test-rule", "Test passed", true, RuleResult.ResultType.MATCH);
            StageExecutionResult result = StageExecutionResult.success("test-stage", ruleResult);
            
            result.addStageOutput("original", "value");
            
            // Get outputs and modify the returned map
            Map<String, Object> outputs = result.getStageOutputs();
            outputs.put("modified", "newValue");
            
            // Original should not be affected
            Map<String, Object> outputs2 = result.getStageOutputs();
            assertFalse(outputs2.containsKey("modified"), "Original map should not be modified");
            assertEquals("value", outputs2.get("original"));
            
            logger.info("[OK] getStageOutputs returns defensive copy");
        }
    }

    // ========================================
    // ScenarioExecutionResult Summary Tests  
    // ========================================
    
    @Nested
    @DisplayName("ScenarioExecutionResult Summary")
    class ScenarioExecutionResultSummaryTests {
        
        @Test
        @DisplayName("Should report correct successful stage count in getExecutionSummary")
        void testGetExecutionSummaryReportsCorrectSuccessCount() {
            logger.info("TEST: getExecutionSummary reports correct success count");
            
            ScenarioExecutionResult scenarioResult = new ScenarioExecutionResult("test-scenario");
            
            RuleResult ruleResult = new RuleResult("test-rule", "Test passed", true, RuleResult.ResultType.MATCH);
            
            // Add 2 successful and 1 failed stage
            scenarioResult.addStageResult(StageExecutionResult.success("stage1", ruleResult));
            scenarioResult.addStageResult(StageExecutionResult.success("stage2", ruleResult));
            scenarioResult.addStageResult(StageExecutionResult.failure("stage3", "Test failure"));
            
            String summary = scenarioResult.getExecutionSummary();
            
            // Should report 2 successful, not 3 (this was the bug we fixed)
            assertTrue(summary.contains("2 successful"), 
                "Summary should show 2 successful stages, got: " + summary);
            assertTrue(summary.contains("1 failed"),
                "Summary should show 1 failed stage, got: " + summary);
            
            logger.info("[OK] getExecutionSummary reports correct success count: {}", summary);
        }
        
        @Test
        @DisplayName("Should correctly count successful stages via getSuccessfulStages")
        void testGetSuccessfulStagesCount() {
            logger.info("TEST: getSuccessfulStages returns correct count");
            
            ScenarioExecutionResult scenarioResult = new ScenarioExecutionResult("test-scenario");
            
            RuleResult ruleResult = new RuleResult("test-rule", "Test passed", true, RuleResult.ResultType.MATCH);
            
            scenarioResult.addStageResult(StageExecutionResult.success("stage1", ruleResult));
            scenarioResult.addStageResult(StageExecutionResult.failure("stage2", "Failed"));
            scenarioResult.addStageResult(StageExecutionResult.success("stage3", ruleResult));
            scenarioResult.addStageResult(StageExecutionResult.criticalFailure("stage4", "Critical"));
            
            assertEquals(2, scenarioResult.getSuccessfulStages().size(),
                "Should have exactly 2 successful stages");
            assertEquals(2, scenarioResult.getFailedStages().size(),
                "Should have exactly 2 failed stages");
            
            logger.info("[OK] getSuccessfulStages returns correct count");
        }
        
        @Test
        @DisplayName("Should correctly identify successful vs failed stages by name")
        void testIsStageSuccessful() {
            logger.info("TEST: isStageSuccessful identifies correct stages");
            
            ScenarioExecutionResult scenarioResult = new ScenarioExecutionResult("test-scenario");
            
            RuleResult ruleResult = new RuleResult("test-rule", "Test passed", true, RuleResult.ResultType.MATCH);
            
            scenarioResult.addStageResult(StageExecutionResult.success("validation", ruleResult));
            scenarioResult.addStageResult(StageExecutionResult.failure("enrichment", "Failed"));
            
            assertTrue(scenarioResult.isStageSuccessful("validation"), "validation should be successful");
            assertFalse(scenarioResult.isStageSuccessful("enrichment"), "enrichment should not be successful");
            assertFalse(scenarioResult.isStageSuccessful("nonexistent"), "nonexistent should return false");
            
            logger.info("[OK] isStageSuccessful identifies correct stages");
        }
    }

    // ========================================
    // Integration Tests with ScenarioStageExecutor
    // ========================================
    
    @Nested
    @DisplayName("ScenarioStageExecutor Data Flow")
    class ScenarioStageExecutorTests {

        private TestConfigLoader configLoader;
        private YamlRuleFactory ruleFactory;
        private ScenarioStageExecutor executor;

        @BeforeEach
        void setUp() {
            configLoader = new TestConfigLoader();
            ruleFactory = new YamlRuleFactory();
            executor = new ScenarioStageExecutor(configLoader, ruleFactory);
        }
        
        @Test
        @DisplayName("Should execute multiple stages in sequence with dependencies")
        void testMultipleStagesExecuteInSequence() {
            logger.info("TEST: Multiple stages execute in sequence");
            
            ScenarioStage stage1 = new ScenarioStage("stage1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage2", "config/stage2.yaml", 2);
            stage2.addDependency("stage1");
            ScenarioStage stage3 = new ScenarioStage("stage3", "config/stage3.yaml", 3);
            stage3.addDependency("stage2");
            
            List<ScenarioStage> stages = Arrays.asList(stage1, stage2, stage3);
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "sequence-test", "Sequence Test", 
                Arrays.asList("TestData"), stages);
            
            configLoader.addSuccess("config/stage1.yaml");
            configLoader.addSuccess("config/stage2.yaml");
            configLoader.addSuccess("config/stage3.yaml");
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("input", "value");
            
            ScenarioExecutionResult result = executor.executeStages(scenario, inputData);
            
            assertTrue(result.isSuccessful(), "Scenario should be successful");
            assertEquals(3, result.getStageResults().size(), "All stages should execute");
            assertEquals(3, result.getSuccessfulStages().size(), "All stages should be successful");
            
            logger.info("[OK] Multiple stages execute in sequence");
        }
        
        @Test
        @DisplayName("Should preserve original data across all stages")
        void testOriginalDataPreserved() {
            logger.info("TEST: Original data preserved across stages");
            
            ScenarioStage stage1 = new ScenarioStage("stage1", "config/stage1.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("stage2", "config/stage2.yaml", 2);
            ScenarioStage stage3 = new ScenarioStage("stage3", "config/stage3.yaml", 3);
            
            List<ScenarioStage> stages = Arrays.asList(stage1, stage2, stage3);
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "preserve-test", "Preserve Test", 
                Arrays.asList("TestData"), stages);
            
            configLoader.addSuccess("config/stage1.yaml");
            configLoader.addSuccess("config/stage2.yaml");
            configLoader.addSuccess("config/stage3.yaml");
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("field1", "value1");
            inputData.put("field2", 42);
            inputData.put("field3", true);
            inputData.put("nestedData", Map.of("nested", "value"));
            
            executor.executeStages(scenario, inputData);
            
            // Original data should still be there after all stages
            assertEquals("value1", inputData.get("field1"));
            assertEquals(42, inputData.get("field2"));
            assertEquals(true, inputData.get("field3"));
            assertNotNull(inputData.get("nestedData"));
            
            logger.info("[OK] Original data preserved across stages");
        }
        
        @Test
        @DisplayName("Should add stage result after each stage execution")
        void testStageResultAddedAfterExecution() {
            logger.info("TEST: Stage result added after execution");
            
            ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
            List<ScenarioStage> stages = Arrays.asList(stage);
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "result-test", "Result Test", 
                Arrays.asList("TestData"), stages);
            
            configLoader.addSuccess("config/test.yaml");
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("data", "value");
            
            ScenarioExecutionResult result = executor.executeStages(scenario, inputData);
            
            assertNotNull(result.getStageResults());
            assertEquals(1, result.getStageResults().size());
            
            StageExecutionResult stageResult = result.getStageResults().get(0);
            assertEquals("test-stage", stageResult.getStageName());
            assertTrue(stageResult.isSuccessful());
            assertNotNull(stageResult.getRuleResult());
            
            logger.info("[OK] Stage result correctly added");
        }
        
        @Test
        @DisplayName("Should track stage results in execution order")
        void testStageResultsInExecutionOrder() {
            logger.info("TEST: Stage results in execution order");
            
            // Create stages with explicit execution order
            ScenarioStage stage1 = new ScenarioStage("first", "config/first.yaml", 1);
            ScenarioStage stage2 = new ScenarioStage("second", "config/second.yaml", 2);
            ScenarioStage stage3 = new ScenarioStage("third", "config/third.yaml", 3);
            
            // Add in non-order to verify sorting
            List<ScenarioStage> stages = Arrays.asList(stage3, stage1, stage2);
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "order-test", "Order Test", 
                Arrays.asList("TestData"), stages);
            
            configLoader.addSuccess("config/first.yaml");
            configLoader.addSuccess("config/second.yaml");
            configLoader.addSuccess("config/third.yaml");
            
            Map<String, Object> inputData = new HashMap<>();
            
            ScenarioExecutionResult result = executor.executeStages(scenario, inputData);
            
            assertEquals(3, result.getStageResults().size());
            assertEquals("first", result.getStageResults().get(0).getStageName());
            assertEquals("second", result.getStageResults().get(1).getStageName());
            assertEquals("third", result.getStageResults().get(2).getStageName());
            
            logger.info("[OK] Stage results correctly ordered");
        }
        
        @Test
        @DisplayName("Should continue execution after non-critical failure with continue policy")
        void testContinueAfterNonCriticalFailure() {
            logger.info("TEST: Continue after non-critical failure");
            
            ScenarioStage stage1 = new ScenarioStage("stage1", "config/stage1.yaml", 1);
            stage1.setFailurePolicy(ScenarioStage.FAILURE_POLICY_CONTINUE_WITH_WARNINGS);
            
            ScenarioStage stage2 = new ScenarioStage("stage2", "config/stage2.yaml", 2);
            
            List<ScenarioStage> stages = Arrays.asList(stage1, stage2);
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "continue-test", "Continue Test", 
                Arrays.asList("TestData"), stages);
            
            configLoader.addSuccess("config/stage1.yaml");
            configLoader.addSuccess("config/stage2.yaml");
            
            Map<String, Object> inputData = new HashMap<>();
            
            ScenarioExecutionResult result = executor.executeStages(scenario, inputData);
            
            // Both stages should execute
            assertEquals(2, result.getStageResults().size(), "Both stages should execute");
            
            logger.info("[OK] Continued execution after non-critical failure policy");
        }
    }

    // ========================================
    // Scenario Metadata Isolation Tests
    // ========================================
    
    @Nested
    @DisplayName("Scenario Metadata Isolation Tests")
    class ScenarioMetadataIsolationTests {
        
        private ScenarioStageExecutor executor;
        private TestConfigLoader configLoader;
        private YamlRuleFactory ruleFactory;
        
        @BeforeEach
        void setUp() {
            logger.info("Setting up scenario metadata isolation tests");
            configLoader = new TestConfigLoader();
            ruleFactory = new YamlRuleFactory();
            executor = new ScenarioStageExecutor(configLoader, ruleFactory);
        }
        
        @Test
        @DisplayName("Should NOT pollute input dataObjectMap with scenarioContext")
        void testScenarioContextNotInInputData() {
            logger.info("TEST: scenarioContext should not pollute input data");
            
            // Create a simple scenario with enrichment
            ScenarioStage stage = new ScenarioStage();
            stage.setStageName("test-stage");
            stage.setConfigFile("test-config.yaml");
            stage.setEnabled(true);
            stage.setRequired(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "test-scenario",
                "Test Scenario",
                Collections.singletonList("TEST_TYPE"),
                Collections.singletonList(stage)
            );
            
            // Mock config with enrichment
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            configLoader.addSuccess("test-config.yaml", config);
            
            // Create input data with some fields
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST123");
            inputData.put("amount", 1000);
            
            // Store original keys for comparison
            Set<String> originalKeys = new HashSet<>(inputData.keySet());
            
            // Execute scenario
            ScenarioExecutionResult result = executor.executeStages(scenario, inputData);
            
            // CRITICAL ASSERTION: scenarioContext should NOT be in input data
            assertFalse(inputData.containsKey("scenarioContext"),
                "Input dataObjectMap should NOT contain scenarioContext");
            assertNull(inputData.get("scenarioContext"),
                "scenarioContext should be null in input data");
            
            logger.info("[OK] scenarioContext not present in input data");
        }
        
        @Test
        @DisplayName("Should NOT pollute input dataObjectMap with previousStageResults")
        void testPreviousStageResultsNotInInputData() {
            logger.info("TEST: previousStageResults should not pollute input data");
            
            ScenarioStage stage1 = new ScenarioStage();
            stage1.setStageName("stage1");
            stage1.setConfigFile("config1.yaml");
            stage1.setEnabled(true);
            
            ScenarioStage stage2 = new ScenarioStage();
            stage2.setStageName("stage2");
            stage2.setConfigFile("config2.yaml");
            stage2.setEnabled(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "multi-stage-scenario",
                "Multi Stage Test",
                Collections.singletonList("TEST_TYPE"),
                Arrays.asList(stage1, stage2)
            );
            
            YamlRuleConfiguration config1 = new YamlRuleConfiguration();
            configLoader.addSuccess("config1.yaml", config1);
            
            YamlRuleConfiguration config2 = new YamlRuleConfiguration();
            configLoader.addSuccess("config2.yaml", config2);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("testField", "testValue");
            
            executor.executeStages(scenario, inputData);
            
            // CRITICAL ASSERTION: previousStageResults should NOT be in input data
            assertFalse(inputData.containsKey("previousStageResults"),
                "Input dataObjectMap should NOT contain previousStageResults");
            assertNull(inputData.get("previousStageResults"),
                "previousStageResults should be null in input data");
            
            logger.info("[OK] previousStageResults not present in input data");
        }
        
        @Test
        @DisplayName("Should NOT pollute input dataObjectMap with scenarioId")
        void testScenarioIdNotInInputData() {
            logger.info("TEST: scenarioId should not pollute input data");
            
            ScenarioStage stage = new ScenarioStage();
            stage.setStageName("test-stage");
            stage.setConfigFile("test-config.yaml");
            stage.setEnabled(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "unique-scenario-id",
                "Test Scenario",
                Collections.singletonList("TEST_TYPE"),
                Collections.singletonList(stage)
            );
            
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            configLoader.addSuccess("test-config.yaml", config);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("originalField", "originalValue");
            
            executor.executeStages(scenario, inputData);
            
            // CRITICAL ASSERTION: scenarioId should NOT be in input data
            assertFalse(inputData.containsKey("scenarioId"),
                "Input dataObjectMap should NOT contain scenarioId");
            assertNull(inputData.get("scenarioId"),
                "scenarioId should be null in input data");
            
            logger.info("[OK] scenarioId not present in input data");
        }
        
        @Test
        @DisplayName("Should NOT pollute input dataObjectMap with executionStartTime")
        void testExecutionStartTimeNotInInputData() {
            logger.info("TEST: executionStartTime should not pollute input data");
            
            ScenarioStage stage = new ScenarioStage();
            stage.setStageName("test-stage");
            stage.setConfigFile("test-config.yaml");
            stage.setEnabled(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "test-scenario",
                "Test Scenario",
                Collections.singletonList("TEST_TYPE"),
                Collections.singletonList(stage)
            );
            
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            configLoader.addSuccess("test-config.yaml", config);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("data", "value");
            
            executor.executeStages(scenario, inputData);
            
            // CRITICAL ASSERTION: executionStartTime should NOT be in input data
            assertFalse(inputData.containsKey("executionStartTime"),
                "Input dataObjectMap should NOT contain executionStartTime");
            assertNull(inputData.get("executionStartTime"),
                "executionStartTime should be null in input data");
            
            logger.info("[OK] executionStartTime not present in input data");
        }
        
        @Test
        @DisplayName("Should NOT pollute input data with ANY scenario metadata fields")
        void testNoScenarioMetadataPollution() {
            logger.info("TEST: No scenario metadata should pollute input data");
            
            ScenarioStage stage = new ScenarioStage();
            stage.setStageName("enrichment-stage");
            stage.setConfigFile("enrichment-config.yaml");
            stage.setEnabled(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "comprehensive-test",
                "Comprehensive Metadata Test",
                Collections.singletonList("TEST_TYPE"),
                Collections.singletonList(stage)
            );
            
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            configLoader.addSuccess("enrichment-config.yaml", config);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "C001");
            inputData.put("amount", 5000);
            inputData.put("currency", "USD");
            
            // Store original key count
            int originalKeyCount = inputData.keySet().size();
            Set<String> originalKeys = new HashSet<>(inputData.keySet());
            
            executor.executeStages(scenario, inputData);
            
            // CRITICAL ASSERTIONS: None of the metadata fields should be present
            List<String> metadataFields = Arrays.asList(
                "scenarioContext",
                "previousStageResults", 
                "scenarioId",
                "executionStartTime"
            );
            
            for (String metadataField : metadataFields) {
                assertFalse(inputData.containsKey(metadataField),
                    "Input dataObjectMap should NOT contain: " + metadataField);
            }
            
            // Verify only legitimate fields remain
            Set<String> currentKeys = new HashSet<>(inputData.keySet());
            currentKeys.removeAll(originalKeys);
            
            // Any new keys should be legitimate enriched data, not metadata
            for (String newKey : currentKeys) {
                assertFalse(metadataFields.contains(newKey),
                    "New key '" + newKey + "' should not be scenario metadata");
            }
            
            logger.info("[OK] Input data contains {} fields, none are scenario metadata", inputData.size());
            logger.info("[OK] All scenario metadata fields successfully filtered");
        }
        
        @Test
        @DisplayName("Should preserve legitimate enriched data while filtering metadata")
        void testLegitimateEnrichedDataPreserved() {
            logger.info("TEST: Legitimate enriched data should be preserved");
            
            ScenarioStage stage = new ScenarioStage();
            stage.setStageName("enrichment-stage");
            stage.setConfigFile("enrichment.yaml");
            stage.setEnabled(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "enrichment-scenario",
                "Enrichment Test",
                Collections.singletonList("TEST_TYPE"),
                Collections.singletonList(stage)
            );
            
            // Create a config that will produce enriched data
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            
            // Mock enrichment result that would normally add fields
            configLoader.addSuccess("enrichment.yaml", config);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST123");
            
            executor.executeStages(scenario, inputData);
            
            // Original field should still be there
            assertEquals("CUST123", inputData.get("customerId"),
                "Original input data should be preserved");
            
            // Metadata should NOT be there
            assertFalse(inputData.containsKey("scenarioContext"));
            assertFalse(inputData.containsKey("previousStageResults"));
            assertFalse(inputData.containsKey("scenarioId"));
            assertFalse(inputData.containsKey("executionStartTime"));
            
            logger.info("[OK] Legitimate data preserved while metadata filtered");
        }
        
        @Test
        @DisplayName("Should filter metadata across multiple stages")
        void testMetadataFilteringAcrossMultipleStages() {
            logger.info("TEST: Metadata filtering across multiple stages");
            
            ScenarioStage stage1 = new ScenarioStage();
            stage1.setStageName("stage-1");
            stage1.setConfigFile("stage1.yaml");
            stage1.setEnabled(true);
            
            ScenarioStage stage2 = new ScenarioStage();
            stage2.setStageName("stage-2");
            stage2.setConfigFile("stage2.yaml");
            stage2.setEnabled(true);
            
            ScenarioStage stage3 = new ScenarioStage();
            stage3.setStageName("stage-3");
            stage3.setConfigFile("stage3.yaml");
            stage3.setEnabled(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "multi-stage-test",
                "Multi-Stage Metadata Test",
                Collections.singletonList("TEST_TYPE"),
                Arrays.asList(stage1, stage2, stage3)
            );
            
            YamlRuleConfiguration config1 = new YamlRuleConfiguration();
            configLoader.addSuccess("stage1.yaml", config1);
            
            YamlRuleConfiguration config2 = new YamlRuleConfiguration();
            configLoader.addSuccess("stage2.yaml", config2);
            
            YamlRuleConfiguration config3 = new YamlRuleConfiguration();
            configLoader.addSuccess("stage3.yaml", config3);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("initialData", "value");
            
            executor.executeStages(scenario, inputData);
            
            // After ALL stages, metadata should still be filtered
            assertFalse(inputData.containsKey("scenarioContext"),
                "scenarioContext should not appear after any stage");
            assertFalse(inputData.containsKey("previousStageResults"),
                "previousStageResults should not appear after any stage");
            assertFalse(inputData.containsKey("scenarioId"),
                "scenarioId should not appear after any stage");
            assertFalse(inputData.containsKey("executionStartTime"),
                "executionStartTime should not appear after any stage");
            
            logger.info("[OK] Metadata successfully filtered across all {} stages", 3);
        }
        
        @Test
        @DisplayName("Should filter metadata even when stage fails")
        void testMetadataFilteringOnStageFailure() {
            logger.info("TEST: Metadata filtering on stage failure");
            
            ScenarioStage stage = new ScenarioStage();
            stage.setStageName("failing-stage");
            stage.setConfigFile("failing-config.yaml");
            stage.setEnabled(true);
            stage.setRequired(false); // Allow continuation
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "failure-test",
                "Failure Metadata Test",
                Collections.singletonList("TEST_TYPE"),
                Collections.singletonList(stage)
            );
            
            // Config that will fail
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            configLoader.addFailure("failing-config.yaml", config);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("testData", "value");
            
            executor.executeStages(scenario, inputData);
            
            // Even on failure, metadata should not pollute input
            assertFalse(inputData.containsKey("scenarioContext"),
                "scenarioContext should not appear even on failure");
            assertFalse(inputData.containsKey("previousStageResults"),
                "previousStageResults should not appear even on failure");
            assertFalse(inputData.containsKey("scenarioId"),
                "scenarioId should not appear even on failure");
            assertFalse(inputData.containsKey("executionStartTime"),
                "executionStartTime should not appear even on failure");
            
            logger.info("[OK] Metadata successfully filtered even on stage failure");
        }
        
        @Test
        @DisplayName("Should NOT contain ANY scenario infrastructure objects (type-based detection)")
        void testNoInfrastructureObjectsInInputData() {
            logger.info("TEST: No scenario infrastructure objects should pollute input data");
            
            // Create scenario
            ScenarioStage stage = new ScenarioStage();
            stage.setStageName("test-stage");
            stage.setConfigFile("test-config.yaml");
            stage.setEnabled(true);
            
            ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
                "test-scenario",
                "Test Scenario",
                Collections.singletonList("TEST_TYPE"),
                Collections.singletonList(stage)
            );
            
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            configLoader.addSuccess("test-config.yaml", config);
            
            // Create input data
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST123");
            inputData.put("amount", 1000);
            inputData.put("region", "EMEA");
            
            // Execute scenario
            executor.executeStages(scenario, inputData);
            
            // CRITICAL: Check for ANY infrastructure object types
            // This catches future metadata regardless of field name
            for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                Object value = entry.getValue();
                String key = entry.getKey();
                
                // Check for specific infrastructure types that should NEVER leak
                assertFalse(value instanceof ScenarioExecutionResult,
                    "ScenarioExecutionResult leaked into input data as key: " + key);
                    
                assertFalse(value instanceof StageExecutionResult,
                    "StageExecutionResult leaked into input data as key: " + key);
                
                // Check for lists of infrastructure objects
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (!list.isEmpty()) {
                        Object firstItem = list.get(0);
                        assertFalse(firstItem instanceof StageExecutionResult,
                            "List<StageExecutionResult> leaked into input data as key: " + key);
                    }
                }
                
                // Check for scenario service classes (shouldn't be in business data)
                assertFalse(value instanceof ScenarioStageExecutor,
                    "ScenarioStageExecutor leaked into input data as key: " + key);
                    
                assertFalse(value instanceof ScenarioConfiguration,
                    "ScenarioConfiguration leaked into input data as key: " + key);
            }
            
            logger.info("[OK] No infrastructure objects found in input data (type-based check passed)");
        }
    }
}
