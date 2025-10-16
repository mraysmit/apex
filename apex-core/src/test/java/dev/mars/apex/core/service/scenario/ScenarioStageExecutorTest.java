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
 * Comprehensive tests for ScenarioStageExecutor.
 *
 * Tests stage execution, dependency management, failure policies,
 * and integration with the rules engine.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ScenarioStageExecutorTest {

    private TestConfigLoader configLoader;
    // Test loader that returns in-memory configs without touching the filesystem
    private static class TestConfigLoader extends YamlConfigurationLoader {
        private final Map<String, YamlRuleConfiguration> configs = new HashMap<>();
        private final Set<String> throwOnLoad = new HashSet<>();

        public void addSuccess(String path, YamlRuleConfiguration cfg) {
            configs.put(path, cfg);
        }
        public void addSuccess(String path) {
            configs.put(path, new YamlRuleConfiguration()); // empty config => evaluation success
        }
        public void addFailure(String path) {
            throwOnLoad.add(path);
        }
        @Override
        public YamlRuleConfiguration loadFromFile(String filePath) {
            if (throwOnLoad.contains(filePath)) {
                throw new RuntimeException("TEST: Simulated load failure for " + filePath);
            }
            YamlRuleConfiguration cfg = configs.get(filePath);
            if (cfg == null) {
                // default to empty config (success) if not explicitly configured
                return new YamlRuleConfiguration();
            }
            return cfg;
        }
    }

    // Helper to create a config that induces evaluation failure via enrichment error
    private static YamlRuleConfiguration failingConfig() {
        YamlRuleConfiguration cfg = new YamlRuleConfiguration();
        // Minimal config that will not create runtime errors; stage failures are simulated via addFailure()
        return cfg;
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
    void testExecuteStages_SingleStageSuccess() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("validation", "config/validation.yaml", 1);
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("test-scenario", "Test Scenario",
                                                                          Arrays.asList("TestData"), stages);
        configLoader.addSuccess("config/validation.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertNotNull(result);
        assertEquals("test-scenario", result.getScenarioId());
        assertFalse(result.isTerminated());
        assertFalse(result.requiresReview());
        assertEquals(1, result.getStageResults().size());
        StageExecutionResult stageResult = result.getStageResults().get(0);
        assertEquals("validation", stageResult.getStageName());
        assertTrue(stageResult.isSuccessful());
    }

    @Test
    void testExecuteStages_MultipleStagesWithDependencies() throws Exception {
        // Arrange
        ScenarioStage validationStage = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage enrichmentStage = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        enrichmentStage.addDependency("validation");

        List<ScenarioStage> stages = Arrays.asList(validationStage, enrichmentStage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("multi-stage-scenario", "Multi Stage Test",
                                                                          Arrays.asList("TestData"), stages);

        configLoader.addSuccess("config/validation.yaml");
        configLoader.addSuccess("config/enrichment.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertNotNull(result);
        assertEquals("multi-stage-scenario", result.getScenarioId());

    }

    @Test
    void testExecuteStages_FailurePolicyTerminate() throws Exception {
        // Arrange
        ScenarioStage criticalStage = new ScenarioStage("critical-validation", "config/critical.yaml", 1);
        criticalStage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);
        criticalStage.setRequired(true);

        ScenarioStage nextStage = new ScenarioStage("next-stage", "config/next.yaml", 2);

        List<ScenarioStage> stages = Arrays.asList(criticalStage, nextStage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("terminate-test", "Terminate Test",
                                                                          Arrays.asList("TestData"), stages);

        // Simulate a configuration error to trigger failure
        configLoader.addFailure("config/critical.yaml");
        // Ensure next stage would succeed if executed
        configLoader.addSuccess("config/next.yaml");


        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTerminated());
        assertFalse(result.isSuccessful());

        // Should have only one stage result (the failed one)
        assertEquals(1, result.getStageResults().size());
        StageExecutionResult stageResult = result.getStageResults().get(0);
        assertEquals("critical-validation", stageResult.getStageName());
        assertFalse(stageResult.isSuccessful());

        // Next stage should not have been executed
        assertTrue(result.getSkippedStages().containsKey("next-stage"));
    }

    @Test
    void testExecuteStages_FailurePolicyContinueWithWarnings() throws Exception {
        // Arrange
        ScenarioStage warningStage = new ScenarioStage("warning-stage", "config/warning.yaml", 1);
        warningStage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_CONTINUE_WITH_WARNINGS);
        warningStage.setRequired(false);

        ScenarioStage nextStage = new ScenarioStage("next-stage", "config/next.yaml", 2);

        List<ScenarioStage> stages = Arrays.asList(warningStage, nextStage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("warning-test", "Warning Test",
                                                                          Arrays.asList("TestData"), stages);

        // Simulate first stage failure and second stage success
        configLoader.addFailure("config/warning.yaml");
        configLoader.addSuccess("config/next.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertNotNull(result);
        assertFalse(result.isTerminated());
        assertTrue(result.hasWarnings());

        // Should have executed both stages
        assertEquals(2, result.getStageResults().size());

        // First stage should have failed
        StageExecutionResult firstStage = result.getStageResults().get(0);
        assertEquals("warning-stage", firstStage.getStageName());
        assertFalse(firstStage.isSuccessful());

        // Should have warnings
        assertFalse(result.getWarnings().isEmpty());

    }

    @Test
    void testExecuteStages_FailurePolicyFlagForReview() throws Exception {
        // Arrange
        ScenarioStage reviewStage = new ScenarioStage("review-stage", "config/review.yaml", 1);
        reviewStage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_FLAG_FOR_REVIEW);

        List<ScenarioStage> stages = Arrays.asList(reviewStage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("review-test", "Review Test",
                                                                          Arrays.asList("TestData"), stages);

        // Simulate stage failure that should flag for review
        configLoader.addFailure("config/review.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertNotNull(result);
        assertFalse(result.isTerminated());
        assertTrue(result.requiresReview());
        assertTrue(result.hasReviewFlags());

        // Should have review flags
        assertFalse(result.getReviewFlags().isEmpty());

    }

    @Test
    void testExecuteStages_SkippedDueToDependencies() throws Exception {
        // Arrange
        ScenarioStage firstStage = new ScenarioStage("first", "config/first.yaml", 1);
        firstStage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);

        ScenarioStage dependentStage = new ScenarioStage("dependent", "config/dependent.yaml", 2);
        dependentStage.addDependency("first");

        List<ScenarioStage> stages = Arrays.asList(firstStage, dependentStage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("dependency-test", "Dependency Test",
                                                                          Arrays.asList("TestData"), stages);

        // Simulate first stage failure
        configLoader.addFailure("config/first.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");

        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);

        // Assert
        assertNotNull(result);
        assertTrue(result.isTerminated());

        // Should have one executed stage and one skipped stage
        assertEquals(1, result.getStageResults().size());
        assertEquals(1, result.getSkippedStages().size());

        assertTrue(result.getSkippedStages().containsKey("dependent"));

    }

    @Test
    void testExecuteStages_NullScenario() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            executor.executeStages(null, new Object());
        });
    }

    @Test
    void testExecuteStages_NoStageConfiguration() {
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy");
        legacyScenario.setRuleConfigurations(Arrays.asList("config/rules.yaml"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            executor.executeStages(legacyScenario, new Object());
        });
    }
}
