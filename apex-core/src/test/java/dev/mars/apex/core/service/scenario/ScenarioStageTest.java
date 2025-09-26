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
 * Comprehensive unit tests for ScenarioStage.
 * 
 * Tests cover:
 * - Stage configuration and validation
 * - Dependency management
 * - Failure policy handling
 * - Metadata operations
 * - Utility methods
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("ScenarioStage Tests")
class ScenarioStageTest {

    private ScenarioStage stage;

    @BeforeEach
    void setUp() {
        stage = new ScenarioStage();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create stage with default constructor")
    void testDefaultConstructor() {
        ScenarioStage defaultStage = new ScenarioStage();
        
        assertNull(defaultStage.getStageName());
        assertNull(defaultStage.getConfigFile());
        assertEquals(0, defaultStage.getExecutionOrder());
        assertEquals(ScenarioStage.FAILURE_POLICY_CONTINUE_WITH_WARNINGS, defaultStage.getFailurePolicy());
        assertFalse(defaultStage.isRequired());
        assertNotNull(defaultStage.getDependsOn());
        assertTrue(defaultStage.getDependsOn().isEmpty());
        assertNotNull(defaultStage.getStageMetadata());
        assertTrue(defaultStage.getStageMetadata().isEmpty());
    }

    @Test
    @DisplayName("Should create stage with parameterized constructor")
    void testParameterizedConstructor() {
        ScenarioStage paramStage = new ScenarioStage("validation", "config/validation.yaml", 1);
        
        assertEquals("validation", paramStage.getStageName());
        assertEquals("config/validation.yaml", paramStage.getConfigFile());
        assertEquals(1, paramStage.getExecutionOrder());
        assertEquals(ScenarioStage.FAILURE_POLICY_CONTINUE_WITH_WARNINGS, paramStage.getFailurePolicy());
        assertFalse(paramStage.isRequired());
    }

    @Test
    @DisplayName("Should create stage with failure policy constructor")
    void testFailurePolicyConstructor() {
        ScenarioStage policyStage = new ScenarioStage("validation", "config/validation.yaml", 1, 
                                                     ScenarioStage.FAILURE_POLICY_TERMINATE);
        
        assertEquals("validation", policyStage.getStageName());
        assertEquals("config/validation.yaml", policyStage.getConfigFile());
        assertEquals(1, policyStage.getExecutionOrder());
        assertEquals(ScenarioStage.FAILURE_POLICY_TERMINATE, policyStage.getFailurePolicy());
    }

    // ========================================
    // Basic Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get stage name")
    void testStageNameProperty() {
        stage.setStageName("enrichment");
        assertEquals("enrichment", stage.getStageName());
    }

    @Test
    @DisplayName("Should set and get config file")
    void testConfigFileProperty() {
        stage.setConfigFile("config/enrichment-rules.yaml");
        assertEquals("config/enrichment-rules.yaml", stage.getConfigFile());
    }

    @Test
    @DisplayName("Should set and get execution order")
    void testExecutionOrderProperty() {
        stage.setExecutionOrder(2);
        assertEquals(2, stage.getExecutionOrder());
    }

    @Test
    @DisplayName("Should set and get failure policy")
    void testFailurePolicyProperty() {
        stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_FLAG_FOR_REVIEW);
        assertEquals(ScenarioStage.FAILURE_POLICY_FLAG_FOR_REVIEW, stage.getFailurePolicy());
    }

    @Test
    @DisplayName("Should set and get required flag")
    void testRequiredProperty() {
        stage.setRequired(true);
        assertTrue(stage.isRequired());
        
        stage.setRequired(false);
        assertFalse(stage.isRequired());
    }

    // ========================================
    // Dependency Management Tests
    // ========================================

    @Test
    @DisplayName("Should manage dependencies correctly")
    void testDependencyManagement() {
        // Initially no dependencies
        assertFalse(stage.hasDependencies());
        assertTrue(stage.getDependsOn().isEmpty());
        
        // Add dependency
        stage.addDependency("validation");
        assertTrue(stage.hasDependencies());
        assertTrue(stage.dependsOnStage("validation"));
        assertEquals(1, stage.getDependsOn().size());
        
        // Add another dependency
        stage.addDependency("enrichment");
        assertEquals(2, stage.getDependsOn().size());
        assertTrue(stage.dependsOnStage("enrichment"));
        
        // Remove dependency
        stage.removeDependency("validation");
        assertEquals(1, stage.getDependsOn().size());
        assertFalse(stage.dependsOnStage("validation"));
        assertTrue(stage.dependsOnStage("enrichment"));
    }

    @Test
    @DisplayName("Should not add duplicate dependencies")
    void testNoDuplicateDependencies() {
        stage.addDependency("validation");
        stage.addDependency("validation"); // Duplicate
        
        assertEquals(1, stage.getDependsOn().size());
        assertTrue(stage.dependsOnStage("validation"));
    }

    @Test
    @DisplayName("Should handle null dependency gracefully")
    void testNullDependency() {
        stage.addDependency(null);
        
        assertFalse(stage.hasDependencies());
        assertTrue(stage.getDependsOn().isEmpty());
    }

    @Test
    @DisplayName("Should set dependencies list")
    void testSetDependenciesList() {
        List<String> dependencies = Arrays.asList("validation", "enrichment");
        stage.setDependsOn(dependencies);

        assertEquals(2, stage.getDependsOn().size());
        assertTrue(stage.dependsOnStage("validation"));
        assertTrue(stage.dependsOnStage("enrichment"));

        // Verify it's a copy, not the same list - create a mutable list to test
        List<String> mutableDependencies = new ArrayList<>(Arrays.asList("validation", "enrichment"));
        stage.setDependsOn(mutableDependencies);
        mutableDependencies.add("compliance");
        assertEquals(2, stage.getDependsOn().size()); // Should still be 2
    }

    // ========================================
    // Metadata Tests
    // ========================================

    @Test
    @DisplayName("Should manage stage metadata")
    void testStageMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("description", "Validation stage");
        metadata.put("sla-ms", 1000);
        metadata.put("critical", true);
        
        stage.setStageMetadata(metadata);
        
        Map<String, Object> retrievedMetadata = stage.getStageMetadata();
        assertEquals("Validation stage", retrievedMetadata.get("description"));
        assertEquals(1000, retrievedMetadata.get("sla-ms"));
        assertEquals(true, retrievedMetadata.get("critical"));
        
        // Verify it's a copy, not the same map
        metadata.put("new-key", "new-value");
        assertFalse(retrievedMetadata.containsKey("new-key"));
    }

    @Test
    @DisplayName("Should handle description metadata")
    void testDescriptionMetadata() {
        assertNull(stage.getDescription());
        
        stage.setDescription("Data validation stage");
        assertEquals("Data validation stage", stage.getDescription());
        
        // Verify it's stored in metadata
        assertEquals("Data validation stage", stage.getStageMetadata().get("description"));
    }

    @Test
    @DisplayName("Should handle SLA metadata")
    void testSlaMetadata() {
        assertNull(stage.getSlaMs());
        
        stage.setSlaMs(2000);
        assertEquals(Integer.valueOf(2000), stage.getSlaMs());
        
        // Verify it's stored in metadata
        assertEquals(2000, stage.getStageMetadata().get("sla-ms"));
    }

    // ========================================
    // Utility Method Tests
    // ========================================

    @Test
    @DisplayName("Should identify critical stages correctly")
    void testIsCritical() {
        // Not critical by default
        assertFalse(stage.isCritical());
        
        // Required but not terminate policy
        stage.setRequired(true);
        stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_CONTINUE_WITH_WARNINGS);
        assertFalse(stage.isCritical());
        
        // Terminate policy but not required
        stage.setRequired(false);
        stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);
        assertFalse(stage.isCritical());
        
        // Both required and terminate policy
        stage.setRequired(true);
        stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);
        assertTrue(stage.isCritical());
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    @DisplayName("Should validate complete stage configuration")
    void testValidCompleteStage() {
        stage.setStageName("validation");
        stage.setConfigFile("config/validation.yaml");
        stage.setExecutionOrder(1);
        stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);
        
        assertTrue(stage.isValid());
        assertTrue(stage.validate().isEmpty());
    }

    @Test
    @DisplayName("Should detect missing stage name")
    void testMissingStageName() {
        stage.setConfigFile("config/validation.yaml");
        stage.setExecutionOrder(1);
        
        assertFalse(stage.isValid());
        List<String> errors = stage.validate();
        assertTrue(errors.stream().anyMatch(error -> error.contains("Stage name is required")));
    }

    @Test
    @DisplayName("Should detect missing config file")
    void testMissingConfigFile() {
        stage.setStageName("validation");
        stage.setExecutionOrder(1);
        
        assertFalse(stage.isValid());
        List<String> errors = stage.validate();
        assertTrue(errors.stream().anyMatch(error -> error.contains("Config file is required")));
    }

    @Test
    @DisplayName("Should detect invalid execution order")
    void testInvalidExecutionOrder() {
        stage.setStageName("validation");
        stage.setConfigFile("config/validation.yaml");
        stage.setExecutionOrder(0); // Invalid - must be positive
        
        assertFalse(stage.isValid());
        List<String> errors = stage.validate();
        assertTrue(errors.stream().anyMatch(error -> error.contains("Execution order must be a positive integer")));
    }

    @Test
    @DisplayName("Should detect invalid failure policy")
    void testInvalidFailurePolicy() {
        stage.setStageName("validation");
        stage.setConfigFile("config/validation.yaml");
        stage.setExecutionOrder(1);
        stage.setFailurePolicy("invalid-policy");
        
        assertFalse(stage.isValid());
        List<String> errors = stage.validate();
        assertTrue(errors.stream().anyMatch(error -> error.contains("Invalid failure policy")));
    }

    @Test
    @DisplayName("Should detect self-dependency")
    void testSelfDependency() {
        stage.setStageName("validation");
        stage.setConfigFile("config/validation.yaml");
        stage.setExecutionOrder(1);
        stage.addDependency("validation"); // Self-dependency
        
        assertFalse(stage.isValid());
        List<String> errors = stage.validate();
        assertTrue(errors.stream().anyMatch(error -> error.contains("Stage cannot depend on itself")));
    }

    // ========================================
    // Equals and HashCode Tests
    // ========================================

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void testEqualsAndHashCode() {
        ScenarioStage stage1 = new ScenarioStage("validation", "config/validation.yaml", 1);
        stage1.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);
        stage1.setRequired(true);
        stage1.addDependency("pre-validation");
        
        ScenarioStage stage2 = new ScenarioStage("validation", "config/validation.yaml", 1);
        stage2.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);
        stage2.setRequired(true);
        stage2.addDependency("pre-validation");
        
        assertEquals(stage1, stage2);
        assertEquals(stage1.hashCode(), stage2.hashCode());
        
        // Change one property
        stage2.setRequired(false);
        assertNotEquals(stage1, stage2);
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void testToString() {
        stage.setStageName("validation");
        stage.setConfigFile("config/validation.yaml");
        stage.setExecutionOrder(1);
        stage.setFailurePolicy(ScenarioStage.FAILURE_POLICY_TERMINATE);
        stage.setRequired(true);
        stage.addDependency("pre-validation");
        stage.setDescription("Data validation stage");
        
        String toString = stage.toString();
        
        assertTrue(toString.contains("validation"));
        assertTrue(toString.contains("config/validation.yaml"));
        assertTrue(toString.contains("executionOrder=1"));
        assertTrue(toString.contains("terminate"));
        assertTrue(toString.contains("required=true"));
        assertTrue(toString.contains("pre-validation"));
        assertTrue(toString.contains("Data validation stage"));
    }
}
