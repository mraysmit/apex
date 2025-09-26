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
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    
    @Mock
    private YamlConfigurationLoader mockConfigLoader;
    
    @Mock
    private YamlRuleFactory mockRuleFactory;
    
    @Mock
    private YamlRuleConfiguration mockYamlConfig;
    
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new ScenarioStageExecutor(mockConfigLoader, mockRuleFactory);
    }
    
    @Test
    void testExecuteStages_SingleStageSuccess() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("validation", "config/validation.yaml", 1);
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("test-scenario", "Test Scenario",
                                                                          Arrays.asList("TestData"), stages);
        
        when(mockConfigLoader.loadFromFile("config/validation.yaml")).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        // Create a successful rule result
        RuleResult successResult = RuleResult.match("test-rule", "Rule passed", "INFO");
        
        // Mock the rules engine evaluation - this is tricky because RulesEngine is created internally
        // For now, we'll test the overall flow and verify the result structure
        
        Object testData = new HashMap<String, Object>() {{
            put("testField", "testValue");
        }};
        
        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);
        
        // Assert
        assertNotNull(result);
        assertEquals("test-scenario", result.getScenarioId());
        assertFalse(result.isTerminated());
        assertFalse(result.requiresReview());
        
        // Verify configuration loading was called
        verify(mockConfigLoader).loadFromFile("config/validation.yaml");
        verify(mockRuleFactory).createRulesEngineConfiguration(mockYamlConfig);
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
        
        when(mockConfigLoader.loadFromFile(anyString())).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        Object testData = new HashMap<String, Object>() {{
            put("testField", "testValue");
        }};
        
        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);
        
        // Assert
        assertNotNull(result);
        assertEquals("multi-stage-scenario", result.getScenarioId());
        
        // Verify both configurations were loaded
        verify(mockConfigLoader).loadFromFile("config/validation.yaml");
        verify(mockConfigLoader).loadFromFile("config/enrichment.yaml");
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
        
        // Mock a configuration error to trigger failure
        when(mockConfigLoader.loadFromFile("config/critical.yaml")).thenThrow(new RuntimeException("Config not found"));
        
        Object testData = new HashMap<String, Object>() {{
            put("testField", "testValue");
        }};
        
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
        verify(mockConfigLoader, never()).loadFromFile("config/next.yaml");
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
        
        // Mock first stage to fail, second to succeed
        when(mockConfigLoader.loadFromFile("config/warning.yaml")).thenThrow(new RuntimeException("Warning failure"));
        when(mockConfigLoader.loadFromFile("config/next.yaml")).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        Object testData = new HashMap<String, Object>() {{
            put("testField", "testValue");
        }};
        
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
        
        // Both configurations should have been attempted
        verify(mockConfigLoader).loadFromFile("config/warning.yaml");
        verify(mockConfigLoader).loadFromFile("config/next.yaml");
    }
    
    @Test
    void testExecuteStages_FailurePolicyFlagForReview() throws Exception {
        // Arrange
        ScenarioStage reviewStage = new ScenarioStage("review-stage", "config/review.yaml", 1);
        reviewStage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_FLAG_FOR_REVIEW);
        
        List<ScenarioStage> stages = Arrays.asList(reviewStage);
        ScenarioConfiguration scenario = ScenarioConfiguration.withStages("review-test", "Review Test",
                                                                          Arrays.asList("TestData"), stages);
        
        // Mock stage to fail
        when(mockConfigLoader.loadFromFile("config/review.yaml")).thenThrow(new RuntimeException("Review needed"));
        
        Object testData = new HashMap<String, Object>() {{
            put("testField", "testValue");
        }};
        
        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isTerminated());
        assertTrue(result.requiresReview());
        assertTrue(result.hasReviewFlags());
        
        // Should have review flags
        assertFalse(result.getReviewFlags().isEmpty());
        
        verify(mockConfigLoader).loadFromFile("config/review.yaml");
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
        
        // Mock first stage to fail
        when(mockConfigLoader.loadFromFile("config/first.yaml")).thenThrow(new RuntimeException("First stage failed"));
        
        Object testData = new HashMap<String, Object>() {{
            put("testField", "testValue");
        }};
        
        // Act
        ScenarioExecutionResult result = executor.executeStages(scenario, testData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isTerminated());
        
        // Should have one executed stage and one skipped stage
        assertEquals(1, result.getStageResults().size());
        assertEquals(1, result.getSkippedStages().size());
        
        assertTrue(result.getSkippedStages().containsKey("dependent"));
        
        // Only first configuration should have been loaded
        verify(mockConfigLoader).loadFromFile("config/first.yaml");
        verify(mockConfigLoader, never()).loadFromFile("config/dependent.yaml");
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
