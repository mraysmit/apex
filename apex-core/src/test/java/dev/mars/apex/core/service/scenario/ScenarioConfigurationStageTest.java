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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScenarioConfiguration stage-based functionality.
 * 
 * Tests cover:
 * - Stage-based configuration management
 * - Backward compatibility with legacy rule-configurations
 * - Stage retrieval and sorting
 * - Configuration type detection
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("ScenarioConfiguration Stage Tests")
class ScenarioConfigurationStageTest {

    private ScenarioConfiguration scenario;

    @BeforeEach
    void setUp() {
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("test-scenario");
        scenario.setName("Test Scenario");
        scenario.setDataTypes(Arrays.asList("TestData"));
    }

    // ========================================
    // Stage Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should manage processing stages")
    void testProcessingStagesManagement() {
        // Initially no stages
        assertFalse(scenario.hasStageConfiguration());
        assertTrue(scenario.getProcessingStages().isEmpty());
        
        // Add stages
        ScenarioStage validationStage = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage enrichmentStage = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        
        scenario.addProcessingStage(validationStage);
        scenario.addProcessingStage(enrichmentStage);
        
        assertTrue(scenario.hasStageConfiguration());
        assertEquals(2, scenario.getProcessingStages().size());
    }

    @Test
    @DisplayName("Should set processing stages list")
    void testSetProcessingStages() {
        ScenarioStage stage1 = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage stage2 = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        List<ScenarioStage> stages = Arrays.asList(stage1, stage2);
        
        scenario.setProcessingStages(stages);
        
        assertTrue(scenario.hasStageConfiguration());
        assertEquals(2, scenario.getProcessingStages().size());
        
        // Verify it's a copy, not the same list - create mutable list to test
        List<ScenarioStage> mutableStages = new ArrayList<>(stages);
        scenario.setProcessingStages(mutableStages);
        mutableStages.add(new ScenarioStage("compliance", "config/compliance.yaml", 3));
        assertEquals(2, scenario.getProcessingStages().size()); // Should still be 2
    }

    @Test
    @DisplayName("Should get stages by execution order")
    void testGetStagesByExecutionOrder() {
        // Add stages in random order
        ScenarioStage stage3 = new ScenarioStage("compliance", "config/compliance.yaml", 3);
        ScenarioStage stage1 = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage stage2 = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        
        scenario.addProcessingStage(stage3);
        scenario.addProcessingStage(stage1);
        scenario.addProcessingStage(stage2);
        
        List<ScenarioStage> sortedStages = scenario.getStagesByExecutionOrder();
        
        assertEquals(3, sortedStages.size());
        assertEquals("validation", sortedStages.get(0).getStageName());
        assertEquals("enrichment", sortedStages.get(1).getStageName());
        assertEquals("compliance", sortedStages.get(2).getStageName());
    }

    @Test
    @DisplayName("Should get stage by name")
    void testGetStageByName() {
        ScenarioStage validationStage = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage enrichmentStage = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        
        scenario.addProcessingStage(validationStage);
        scenario.addProcessingStage(enrichmentStage);
        
        ScenarioStage foundStage = scenario.getStageByName("validation");
        assertNotNull(foundStage);
        assertEquals("validation", foundStage.getStageName());
        
        ScenarioStage notFoundStage = scenario.getStageByName("nonexistent");
        assertNull(notFoundStage);
        
        ScenarioStage nullStage = scenario.getStageByName(null);
        assertNull(nullStage);
    }

    // ========================================
    // Backward Compatibility Tests
    // ========================================

    @Test
    @DisplayName("Should maintain backward compatibility with legacy rule configurations")
    void testBackwardCompatibility() {
        // Set legacy rule configurations
        List<String> legacyRules = Arrays.asList(
            "config/validation.yaml",
            "config/enrichment.yaml",
            "config/compliance.yaml"
        );
        scenario.setRuleConfigurations(legacyRules);
        
        // Should be detected as legacy configuration
        assertTrue(scenario.isLegacyConfiguration());
        assertFalse(scenario.hasStageConfiguration());
        
        // getRuleConfigurations should return legacy rules
        List<String> retrievedRules = scenario.getRuleConfigurations();
        assertEquals(legacyRules, retrievedRules);
    }

    @Test
    @DisplayName("Should return stage config files when stages are configured")
    void testRuleConfigurationsFromStages() {
        // Add stages with different execution orders
        ScenarioStage stage2 = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        ScenarioStage stage1 = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage stage3 = new ScenarioStage("compliance", "config/compliance.yaml", 3);
        
        scenario.addProcessingStage(stage2);
        scenario.addProcessingStage(stage1);
        scenario.addProcessingStage(stage3);
        
        // getRuleConfigurations should return config files sorted by execution order
        List<String> configFiles = scenario.getRuleConfigurations();
        
        assertEquals(3, configFiles.size());
        assertEquals("config/validation.yaml", configFiles.get(0));
        assertEquals("config/enrichment.yaml", configFiles.get(1));
        assertEquals("config/compliance.yaml", configFiles.get(2));
    }

    @Test
    @DisplayName("Should prioritize stage configuration over legacy configuration")
    void testStagePriorityOverLegacy() {
        // Set both legacy and stage configurations
        scenario.setRuleConfigurations(Arrays.asList("legacy/config.yaml"));
        
        ScenarioStage stage = new ScenarioStage("validation", "stage/config.yaml", 1);
        scenario.addProcessingStage(stage);
        
        // Stage configuration should take priority
        assertTrue(scenario.hasStageConfiguration());
        assertFalse(scenario.isLegacyConfiguration());
        
        List<String> configFiles = scenario.getRuleConfigurations();
        assertEquals(1, configFiles.size());
        assertEquals("stage/config.yaml", configFiles.get(0));
    }

    // ========================================
    // Configuration Type Detection Tests
    // ========================================

    @Test
    @DisplayName("Should detect configuration types correctly")
    void testConfigurationTypeDetection() {
        // Initially no configuration
        assertFalse(scenario.hasStageConfiguration());
        assertFalse(scenario.isLegacyConfiguration());
        
        // Add legacy configuration
        scenario.setRuleConfigurations(Arrays.asList("config/legacy.yaml"));
        assertFalse(scenario.hasStageConfiguration());
        assertTrue(scenario.isLegacyConfiguration());
        
        // Add stage configuration
        scenario.addProcessingStage(new ScenarioStage("validation", "config/validation.yaml", 1));
        assertTrue(scenario.hasStageConfiguration());
        assertFalse(scenario.isLegacyConfiguration()); // Stage takes priority
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create scenario with stage-based factory method")
    void testStageBasedFactoryMethod() {
        ScenarioStage stage1 = new ScenarioStage("validation", "config/validation.yaml", 1);
        ScenarioStage stage2 = new ScenarioStage("enrichment", "config/enrichment.yaml", 2);
        List<ScenarioStage> stages = Arrays.asList(stage1, stage2);

        ScenarioConfiguration stageScenario = ScenarioConfiguration.withStages(
            "stage-scenario",
            "Stage Scenario",
            Arrays.asList("TestData"),
            stages
        );

        assertEquals("stage-scenario", stageScenario.getScenarioId());
        assertEquals("Stage Scenario", stageScenario.getName());
        assertTrue(stageScenario.hasStageConfiguration());
        assertEquals(2, stageScenario.getProcessingStages().size());
    }

    @Test
    @DisplayName("Should create scenario with legacy constructor")
    void testLegacyConstructor() {
        List<String> legacyRules = Arrays.asList("config/legacy1.yaml", "config/legacy2.yaml");
        
        ScenarioConfiguration legacyScenario = new ScenarioConfiguration(
            "legacy-scenario", 
            "Legacy Scenario", 
            Arrays.asList("TestData"), 
            legacyRules
        );
        
        assertEquals("legacy-scenario", legacyScenario.getScenarioId());
        assertEquals("Legacy Scenario", legacyScenario.getName());
        assertTrue(legacyScenario.isLegacyConfiguration());
        assertEquals(legacyRules, legacyScenario.getRuleConfigurations());
    }

    // ========================================
    // ToString Tests
    // ========================================

    @Test
    @DisplayName("Should include stage information in toString for stage-based configuration")
    void testToStringWithStages() {
        scenario.addProcessingStage(new ScenarioStage("validation", "config/validation.yaml", 1));
        scenario.addProcessingStage(new ScenarioStage("enrichment", "config/enrichment.yaml", 2));
        
        String toString = scenario.toString();
        
        assertTrue(toString.contains("2 stages"));
        assertTrue(toString.contains("validation"));
        assertTrue(toString.contains("enrichment"));
        assertTrue(toString.contains("configType=stage-based"));
    }

    @Test
    @DisplayName("Should include legacy information in toString for legacy configuration")
    void testToStringWithLegacy() {
        scenario.setRuleConfigurations(Arrays.asList("config/legacy.yaml"));
        
        String toString = scenario.toString();
        
        assertTrue(toString.contains("config/legacy.yaml"));
        assertTrue(toString.contains("configType=legacy"));
    }

    // ========================================
    // Edge Case Tests
    // ========================================

    @Test
    @DisplayName("Should handle null stage lists gracefully")
    void testNullStageList() {
        scenario.setProcessingStages(null);
        
        assertFalse(scenario.hasStageConfiguration());
        assertTrue(scenario.getProcessingStages().isEmpty());
        assertTrue(scenario.getStagesByExecutionOrder().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty stage lists")
    void testEmptyStageList() {
        scenario.setProcessingStages(Arrays.asList());
        
        assertFalse(scenario.hasStageConfiguration());
        assertTrue(scenario.getProcessingStages().isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when no configuration exists")
    void testNoConfiguration() {
        // No legacy rules, no stages
        List<String> configFiles = scenario.getRuleConfigurations();
        assertNull(configFiles); // Should return null for legacy rules when none set
        
        assertTrue(scenario.getProcessingStages().isEmpty());
        assertTrue(scenario.getStagesByExecutionOrder().isEmpty());
    }
}
