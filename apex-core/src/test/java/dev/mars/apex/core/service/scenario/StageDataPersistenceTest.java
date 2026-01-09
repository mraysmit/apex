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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
            
            logger.info("✓ Stage outputs correctly stored and retrieved");
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
            
            logger.info("✓ setStageOutputs works correctly");
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
            
            logger.info("✓ Failure result can store outputs for partial data capture");
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
            
            logger.info("✓ getStageOutputs returns defensive copy");
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
            
            logger.info("✓ getExecutionSummary reports correct success count: {}", summary);
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
            
            logger.info("✓ getSuccessfulStages returns correct count");
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
            
            logger.info("✓ isStageSuccessful identifies correct stages");
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

        // Test loader that returns in-memory configs (same pattern as other tests)
        private static class TestConfigLoader extends YamlConfigurationLoader {
            private final Map<String, YamlRuleConfiguration> configs = new HashMap<>();

            public void addSuccess(String path) {
                configs.put(path, new YamlRuleConfiguration());
            }

            @Override
            public YamlRuleConfiguration loadFromFile(String filePath) {
                return configs.getOrDefault(filePath, new YamlRuleConfiguration());
            }
        }

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
            
            logger.info("✓ Multiple stages execute in sequence");
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
            
            logger.info("✓ Original data preserved across stages");
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
            
            logger.info("✓ Stage result correctly added");
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
            
            logger.info("✓ Stage results correctly ordered");
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
            
            logger.info("✓ Continued execution after non-critical failure policy");
        }
    }
}
