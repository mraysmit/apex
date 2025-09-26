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
 * Tests for stage-aware processing functionality in DataTypeScenarioService.
 * 
 * Tests the new stage-based processing capabilities while ensuring
 * backward compatibility with legacy rule-based processing.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataTypeScenarioServiceStageTest {
    
    @Mock
    private YamlConfigurationLoader mockConfigLoader;
    
    @Mock
    private YamlRuleFactory mockRuleFactory;
    
    @Mock
    private YamlRuleConfiguration mockYamlConfig;
    
    private DataTypeScenarioService service;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DataTypeScenarioService(mockConfigLoader, mockRuleFactory);
    }
    
    @Test
    void testProcessData_WithStageBasedScenario() throws Exception {
        // Arrange
        ScenarioStage validationStage = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage enrichmentStage = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        
        List<ScenarioStage> stages = Arrays.asList(validationStage, enrichmentStage);
        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages("stage-scenario", "Stage Scenario",
                                                                              Arrays.asList("TestData"), stages);
        
        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, stageScenario);
        
        when(mockConfigLoader.loadFromFile(anyString())).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        Object testData = new TestData();
        
        // Act
        Object result = service.processData(testData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ScenarioExecutionResult, "Should return ScenarioExecutionResult for stage-based processing");
        
        ScenarioExecutionResult stageResult = (ScenarioExecutionResult) result;
        assertEquals("stage-scenario", stageResult.getScenarioId());
    }
    
    @Test
    void testProcessData_WithLegacyScenario() throws Exception {
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy-scenario");
        legacyScenario.setName("Legacy Scenario");
        legacyScenario.setDataTypes(Arrays.asList("TestData"));
        legacyScenario.setRuleConfigurations(Arrays.asList("config/legacy-rules.yaml"));
        
        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, legacyScenario);
        
        when(mockConfigLoader.loadFromFile("config/legacy-rules.yaml")).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        Object testData = new TestData();
        
        // Act
        Object result = service.processData(testData);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof RuleResult, "Should return RuleResult for legacy processing");
        
        verify(mockConfigLoader).loadFromFile("config/legacy-rules.yaml");
    }
    
    @Test
    void testProcessDataWithStages_Success() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("test-stage", "config/test.yaml", 1);
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages("test-scenario", "Test Scenario",
                                                                              Arrays.asList("TestData"), stages);
        
        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, stageScenario);
        
        when(mockConfigLoader.loadFromFile("config/test.yaml")).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        Object testData = new TestData();
        
        // Act
        ScenarioExecutionResult result = service.processDataWithStages(testData, "test-scenario");
        
        // Assert
        assertNotNull(result);
        assertEquals("test-scenario", result.getScenarioId());
        
        verify(mockConfigLoader).loadFromFile("config/test.yaml");
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
    
    @Test
    void testProcessDataWithScenario_StageBasedProcessing() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("validation", "config/validation.yaml", 1);
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages("stage-scenario", "Stage Scenario",
                                                                              Arrays.asList("TestData"), stages);
        
        when(mockConfigLoader.loadFromFile("config/validation.yaml")).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        Object testData = new TestData();
        
        // Act
        Object result = service.processDataWithScenario(testData, stageScenario);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ScenarioExecutionResult);
        
        ScenarioExecutionResult stageResult = (ScenarioExecutionResult) result;
        assertEquals("stage-scenario", stageResult.getScenarioId());
    }
    
    @Test
    void testProcessDataWithScenario_LegacyProcessing() throws Exception {
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy-scenario");
        legacyScenario.setDataTypes(Arrays.asList("TestData"));
        legacyScenario.setRuleConfigurations(Arrays.asList("config/legacy.yaml"));
        
        when(mockConfigLoader.loadFromFile("config/legacy.yaml")).thenReturn(mockYamlConfig);
        when(mockRuleFactory.createRulesEngineConfiguration(mockYamlConfig)).thenReturn(mock(dev.mars.apex.core.engine.config.RulesEngineConfiguration.class));
        
        Object testData = new TestData();
        
        // Act
        Object result = service.processDataWithScenario(testData, legacyScenario);
        
        // Assert
        assertNotNull(result);
        assertTrue(result instanceof RuleResult);
        
        verify(mockConfigLoader).loadFromFile("config/legacy.yaml");
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
