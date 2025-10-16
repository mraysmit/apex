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

import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 */
class DataTypeScenarioServiceStageTest {

    private DataTypeScenarioService service;

    @BeforeEach
    void setUp() {
        service = new DataTypeScenarioService();
    }

    @Test
    void testProcessData_WithStageBasedScenario() throws Exception {
        // Arrange
        ScenarioStage validationStage = new ScenarioStage("validation", resourcePath("yaml-default-value-test.yaml"), 1);
        ScenarioStage enrichmentStage = new ScenarioStage("enrichment", resourcePath("yaml-default-value-test.yaml"), 2);

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
    }

    @Test
    void testProcessData_WithLegacyScenario() throws Exception {
        // Arrange
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration();
        legacyScenario.setScenarioId("legacy-scenario");
        legacyScenario.setName("Legacy Scenario");
        legacyScenario.setDataTypes(Arrays.asList("TestData"));
        legacyScenario.setRuleConfigurations(Arrays.asList(resourcePath("yaml-default-value-test.yaml")));

        // Register the scenario
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).setAccessible(true);
        service.getClass().getDeclaredMethod("registerScenario", ScenarioConfiguration.class).invoke(service, legacyScenario);

        Object testData = new TestData();

        // Act
        Object result = service.processData(testData);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof RuleResult, "Should return RuleResult for legacy processing");
    }

    @Test
    void testProcessDataWithStages_Success() throws Exception {
        // Arrange
        ScenarioStage stage = new ScenarioStage("test-stage", resourcePath("yaml-default-value-test.yaml"), 1);
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
        ScenarioStage stage = new ScenarioStage("validation", resourcePath("yaml-default-value-test.yaml"), 1);
        List<ScenarioStage> stages = Arrays.asList(stage);
        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages("stage-scenario", "Stage Scenario",
                                                                              Arrays.asList("TestData"), stages);

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
        legacyScenario.setRuleConfigurations(Arrays.asList(resourcePath("yaml-default-value-test.yaml")));

        Object testData = new TestData();

        // Act
        Object result = service.processDataWithScenario(testData, legacyScenario);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof RuleResult);
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
