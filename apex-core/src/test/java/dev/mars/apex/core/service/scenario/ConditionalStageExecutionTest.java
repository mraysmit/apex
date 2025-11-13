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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for conditional stage execution in ScenarioStageExecutor.
 *
 * Tests SpEL condition evaluation on processing-stages including:
 * - Condition evaluates to true (stage executes)
 * - Condition evaluates to false (stage skipped)
 * - No condition specified (stage executes - backward compatible)
 * - Condition evaluation error (stage skipped safely)
 * - Complex SpEL expressions with data access
 * - Conditions combined with dependencies
 * - Multiple stages with different conditions
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ConditionalStageExecutionTest {

    private TestConfigLoader configLoader;
    
    // Test loader that returns in-memory configs without touching the filesystem
    private static class TestConfigLoader extends YamlConfigurationLoader {
        private final Map<String, YamlRuleConfiguration> configs = new HashMap<>();
        
        public void addSuccess(String path) {
            configs.put(path, new YamlRuleConfiguration()); // empty config => evaluation success
        }
        
        @Override
        public YamlRuleConfiguration loadFromFile(String filePath) {
            YamlRuleConfiguration cfg = configs.get(filePath);
            if (cfg == null) {
                return new YamlRuleConfiguration(); // default to empty config (success)
            }
            return cfg;
        }
    }

    private YamlRuleFactory ruleFactory;
    private ScenarioStageExecutor executor;

    @BeforeEach
    void setUp() {
        configLoader = new TestConfigLoader();
        ruleFactory = new YamlRuleFactory();
        executor = new ScenarioStageExecutor(configLoader, ruleFactory);
    }

    @Test
    void testStageExecutesWhenConditionTrue() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("validation", "config/validation.yaml", 1);
        stage.setCondition("#data['region'] == 'US'");  // Condition will be true
        
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);
        
        configLoader.addSuccess("config/validation.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "US");  // Condition will evaluate to true

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed when condition is true");
        assertEquals(1, result.getSuccessfulStages().size(), "Stage should execute when condition is true");
        assertEquals(0, result.getSkippedStages().size(), "No stages should be skipped");
        assertTrue(result.isStageSuccessful("validation"), "Validation stage should be successful");
    }

    @Test
    void testStageSkippedWhenConditionFalse() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("us-compliance", "config/us-compliance.yaml", 1);
        stage.setCondition("#data['region'] == 'US'");  // Condition will be false

        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/us-compliance.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "EMEA");  // Condition will evaluate to false

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed even when stage is skipped");
        assertEquals(0, result.getSuccessfulStages().size(), "No stages should execute when condition is false");
        assertEquals(1, result.getSkippedStages().size(), "Stage should be skipped when condition is false");
        assertFalse(result.isStageSuccessful("us-compliance"), "Stage should not be successful");

        // Verify skip reason contains condition information
        String skipReason = result.getSkippedStages().get("us-compliance");
        assertNotNull(skipReason, "Skip reason should be provided");
        assertTrue(skipReason.contains("Condition not met"), "Skip reason should mention condition");
    }

    @Test
    void testStageExecutesWhenNoCondition() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("validation", "config/validation.yaml", 1);
        // No condition set - should execute (backward compatibility)

        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/validation.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "US");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed when no condition is specified");
        assertEquals(1, result.getSuccessfulStages().size(), "Stage should execute when no condition is specified");
        assertEquals(0, result.getSkippedStages().size(), "No stages should be skipped");
    }

    @Test
    void testStageSkippedWhenConditionEvaluationFails() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("validation", "config/validation.yaml", 1);
        stage.setCondition("#data['nonExistentField'].someMethod()");  // Will cause evaluation error

        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/validation.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "US");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed even when condition evaluation fails");
        assertEquals(0, result.getSuccessfulStages().size(), "Stage should not execute when condition evaluation fails");
        assertEquals(1, result.getSkippedStages().size(), "Stage should be skipped when condition evaluation fails");

        // Verify skip reason contains error information
        String skipReason = result.getSkippedStages().get("validation");
        assertNotNull(skipReason, "Skip reason should be provided");
        assertTrue(skipReason.contains("Condition evaluation failed") || skipReason.contains("Condition not met"),
            "Skip reason should mention condition failure");
    }

    @Test
    void testComplexSpelCondition() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("high-value-check", "config/high-value.yaml", 1);
        stage.setCondition("#data['region'] == 'US' && #data['amount'] > 10000");  // Complex condition

        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/high-value.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "US");
        testData.put("amount", 15000);  // Both conditions true

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed when complex condition is true");
        assertEquals(1, result.getSuccessfulStages().size(), "Stage should execute when complex condition is true");
        assertEquals(0, result.getSkippedStages().size(), "No stages should be skipped");
    }

    @Test
    void testComplexSpelConditionFalse() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("high-value-check", "config/high-value.yaml", 1);
        stage.setCondition("#data['region'] == 'US' && #data['amount'] > 10000");  // Complex condition

        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/high-value.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "US");
        testData.put("amount", 5000);  // Amount condition false

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed even when complex condition is false");
        assertEquals(0, result.getSuccessfulStages().size(), "Stage should not execute when complex condition is false");
        assertEquals(1, result.getSkippedStages().size(), "Stage should be skipped when complex condition is false");
    }

    @Test
    void testConditionWithDependencies() throws Exception {
        // Arrange
        ScenarioStage stage1 = new ScenarioStage("validation", "config/validation.yaml", 1);
        // No condition on stage1

        ScenarioStage stage2 = new ScenarioStage("us-compliance", "config/us-compliance.yaml", 2);
        stage2.setCondition("#data['region'] == 'US'");  // Condition on stage2
        stage2.addDependency("validation");  // Depends on stage1

        List<ScenarioStage> stages = Arrays.asList(stage1, stage2);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/validation.yaml");
        configLoader.addSuccess("config/us-compliance.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "US");  // Condition will be true

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed when both stages execute");
        assertEquals(2, result.getSuccessfulStages().size(), "Both stages should execute");
        assertEquals(0, result.getSkippedStages().size(), "No stages should be skipped");
        assertTrue(result.isStageSuccessful("validation"), "Validation stage should be successful");
        assertTrue(result.isStageSuccessful("us-compliance"), "US compliance stage should be successful");
    }

    @Test
    void testConditionWithDependenciesFalse() throws Exception {
        // Arrange
        ScenarioStage stage1 = new ScenarioStage("validation", "config/validation.yaml", 1);
        // No condition on stage1

        ScenarioStage stage2 = new ScenarioStage("us-compliance", "config/us-compliance.yaml", 2);
        stage2.setCondition("#data['region'] == 'US'");  // Condition on stage2
        stage2.addDependency("validation");  // Depends on stage1

        List<ScenarioStage> stages = Arrays.asList(stage1, stage2);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/validation.yaml");
        configLoader.addSuccess("config/us-compliance.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "EMEA");  // Condition will be false

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed even when second stage is skipped");
        assertEquals(1, result.getSuccessfulStages().size(), "Only first stage should execute");
        assertEquals(1, result.getSkippedStages().size(), "Second stage should be skipped");
        assertTrue(result.isStageSuccessful("validation"), "Validation stage should be successful");
        assertFalse(result.isStageSuccessful("us-compliance"), "US compliance stage should not be successful");
    }

    @Test
    void testMultipleStagesWithDifferentConditions() throws Exception {
        // Arrange
        ScenarioStage stage1 = new ScenarioStage("us-compliance", "config/us-compliance.yaml", 1);
        stage1.setCondition("#data['region'] == 'US'");

        ScenarioStage stage2 = new ScenarioStage("emea-compliance", "config/emea-compliance.yaml", 2);
        stage2.setCondition("#data['region'] == 'EMEA'");

        ScenarioStage stage3 = new ScenarioStage("apac-compliance", "config/apac-compliance.yaml", 3);
        stage3.setCondition("#data['region'] == 'APAC'");

        List<ScenarioStage> stages = Arrays.asList(stage1, stage2, stage3);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages(
            "test-scenario", "Test Scenario", Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/us-compliance.yaml");
        configLoader.addSuccess("config/emea-compliance.yaml");
        configLoader.addSuccess("config/apac-compliance.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("region", "EMEA");  // Only EMEA condition will be true

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertTrue(result.isSuccessful(), "Scenario should succeed");
        assertEquals(1, result.getSuccessfulStages().size(), "Only EMEA stage should execute");
        assertEquals(2, result.getSkippedStages().size(), "US and APAC stages should be skipped");
        assertFalse(result.isStageSuccessful("us-compliance"), "US compliance should not execute");
        assertTrue(result.isStageSuccessful("emea-compliance"), "EMEA compliance should execute");
        assertFalse(result.isStageSuccessful("apac-compliance"), "APAC compliance should not execute");
    }
}
