package dev.mars.apex.core.config.component;

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
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComponentLoader.
 * 
 * Tests focus on:
 * - Loading component YAML files
 * - Execution order sorting (explicit + document order)
 * - Validation (required fields, failure policies)
 * - Nesting depth validation (warnings at depth 3-5, errors at depth 6+)
 * - Circular reference detection
 * - File reference resolution
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 */
@DisplayName("ComponentLoader Tests")
class ComponentLoaderTest {

    private static final Logger logger = LoggerFactory.getLogger(ComponentLoaderTest.class);
    private ComponentLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ComponentLoader();
    }

    // ========================================
    // Constructor and Basic Loading Tests
    // ========================================

    @Test
    @DisplayName("Should create ComponentLoader successfully")
    void testConstructor() {
        ComponentLoader testLoader = new ComponentLoader();
        assertNotNull(testLoader, "Loader should be created successfully");
    }

    @Test
    @DisplayName("Should load basic validation component from classpath")
    void testLoadBasicComponent() throws Exception {
        logger.info("=== Testing Basic Component Loading ===");
        
        String componentPath = "scenario/basic-validation-component.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);
        
        assertNotNull(component, "Component should be loaded");
        assertEquals("basic-validation-component", component.getId(), "Component ID should match");
        assertEquals("Basic Validation Component", component.getName(), "Component name should match");
        assertEquals("component", component.getType(), "Component type should be 'component'");
        assertEquals("1.0.0", component.getVersion(), "Version should match");
        assertEquals("validation", component.getBusinessDomain(), "Business domain should match");
        assertEquals("APEX Demo Team", component.getOwner(), "Owner should match");
        assertEquals("medium", component.getCriticality(), "Criticality should match");
        assertEquals(100, component.getSlaMs(), "SLA should match");
        
        // Verify file references
        List<ComponentConfiguration.FileReference> allRefs = component.getAllReferences();
        assertNotNull(allRefs, "File references should not be null");
        assertEquals(1, allRefs.size(), "Should have 1 file reference");
        
        ComponentConfiguration.FileReference ref = allRefs.get(0);
        assertEquals("BasicStageConfigurationTest-validation-rules.yaml", ref.getFile(), "File path should match");
        assertEquals(1, ref.getExecutionOrder(), "Execution order should be 1");
        assertEquals("terminate", ref.getFailurePolicy(), "Failure policy should be 'terminate'");
        
        logger.info("Basic component loaded successfully: {}", component);
    }

    @Test
    @DisplayName("Should load multi-stage component with enrichment and validation")
    void testLoadMultiStageComponent() throws Exception {
        logger.info("=== Testing Multi-Stage Component Loading ===");
        
        String componentPath = "scenario/multi-stage-component.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);
        
        assertNotNull(component, "Component should be loaded");
        assertEquals("multi-stage-component", component.getId(), "Component ID should match");
        assertEquals("high", component.getCriticality(), "Criticality should be high");
        
        // Verify file references are sorted by execution order
        List<ComponentConfiguration.FileReference> allRefs = component.getAllReferences();
        assertEquals(2, allRefs.size(), "Should have 2 file references");
        
        // First file (execution-order: 1)
        ComponentConfiguration.FileReference ref1 = allRefs.get(0);
        assertEquals("BasicStageConfigurationTest-enrichment-rules.yaml", ref1.getFile());
        assertEquals(1, ref1.getExecutionOrder());
        assertEquals("continue-with-warnings", ref1.getFailurePolicy());
        
        // Second file (execution-order: 2)
        ComponentConfiguration.FileReference ref2 = allRefs.get(1);
        assertEquals("BasicStageConfigurationTest-validation-rules.yaml", ref2.getFile());
        assertEquals(2, ref2.getExecutionOrder());
        assertEquals("terminate", ref2.getFailurePolicy());
        
        logger.info("Multi-stage component loaded successfully with {} files", allRefs.size());
    }

    @Test
    @DisplayName("Should handle mixed execution order (explicit + document order)")
    void testMixedExecutionOrder() throws Exception {
        logger.info("=== Testing Mixed Execution Order ===");
        
        String componentPath = "scenario/mixed-order-component.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);
        
        assertNotNull(component, "Component should be loaded");
        
        // Verify execution order: explicit order first (10, 20), then document order
        List<ComponentConfiguration.FileReference> allRefs = component.getAllReferences();
        assertEquals(4, allRefs.size(), "Should have 4 file references");
        
        // Files with explicit execution order come first
        assertTrue(allRefs.get(0).hasExplicitExecutionOrder(), "First file should have explicit order");
        assertEquals(10, allRefs.get(0).getExecutionOrder(), "First file should have order 10");
        
        assertTrue(allRefs.get(1).hasExplicitExecutionOrder(), "Second file should have explicit order");
        assertEquals(20, allRefs.get(1).getExecutionOrder(), "Second file should have order 20");
        
        // Files without explicit order come after, in document order
        assertFalse(allRefs.get(2).hasExplicitExecutionOrder(), "Third file should not have explicit order");
        assertFalse(allRefs.get(3).hasExplicitExecutionOrder(), "Fourth file should not have explicit order");
        
        logger.info("Mixed execution order validated successfully");
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    @DisplayName("Should fail validation when component ID is missing")
    void testValidationMissingId() {
        logger.info("=== Testing Validation: Missing ID ===");

        String componentPath = "scenario/invalid-component-no-id.yaml";

        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadComponent(componentPath),
            "Should throw exception for missing ID"
        );

        assertTrue(exception.getMessage().contains("id"),
            "Exception message should mention 'id'");
        logger.info("Validation correctly failed for missing ID");
    }

    @Test
    @DisplayName("Should fail validation when component type is wrong")
    void testValidationWrongType() {
        logger.info("=== Testing Validation: Wrong Type ===");

        String componentPath = "scenario/invalid-component-wrong-type.yaml";

        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadComponent(componentPath),
            "Should throw exception for wrong type"
        );

        assertTrue(exception.getMessage().contains("type") || exception.getMessage().contains("component"),
            "Exception message should mention 'type' or 'component'");
        logger.info("Validation correctly failed for wrong type");
    }

    @Test
    @DisplayName("Should fail validation when component has no file references")
    void testValidationNoFiles() {
        logger.info("=== Testing Validation: No File References ===");

        String componentPath = "scenario/invalid-component-no-files.yaml";

        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadComponent(componentPath),
            "Should throw exception for no file references"
        );

        logger.info("Exception message: " + exception.getMessage());
        String fullMessage = exception.getMessage();
        if (exception.getCause() != null) {
            fullMessage += " " + exception.getCause().getMessage();
        }
        logger.info("Full exception message: " + fullMessage);
        assertTrue(fullMessage.contains("file reference") || fullMessage.contains("at least one"),
            "Exception message should mention file references. Actual message: " + fullMessage);
        logger.info("Validation correctly failed for no file references");
    }

    // ========================================
    // Nesting Tests
    // ========================================

    @Test
    @DisplayName("Should load nested component (2 levels)")
    void testNestedComponent() throws Exception {
        logger.info("=== Testing Nested Component (2 levels) ===");

        String componentPath = "scenario/nested-component-level1.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);

        assertNotNull(component, "Component should be loaded");
        assertEquals("nested-component-level1", component.getId(), "Component ID should match");

        // Verify component references
        List<ComponentConfiguration.FileReference> componentRefs = component.getComponentRefs();
        assertEquals(1, componentRefs.size(), "Should have 1 component reference");
        assertEquals("nested-component-level2.yaml", componentRefs.get(0).getFile(),
            "Should reference level 2 component");

        logger.info("Nested component loaded successfully");
    }

    @Test
    @DisplayName("Should resolve all references from nested component")
    void testResolveNestedReferences() throws Exception {
        logger.info("=== Testing Nested Reference Resolution ===");

        String componentPath = "scenario/nested-component-level1.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);

        // Resolve all references (should expand nested component)
        List<ComponentLoader.ResolvedFileReference> resolvedRefs =
            loader.resolveAllReferences(component, componentPath);

        assertNotNull(resolvedRefs, "Resolved references should not be null");
        // Level 1 has 1 component ref (which expands to 1 enrichment file) + 1 direct rule file = 2 total
        assertEquals(2, resolvedRefs.size(), "Should have 2 resolved file references");

        // First resolved file should be from nested component (depth 2)
        ComponentLoader.ResolvedFileReference ref1 = resolvedRefs.get(0);
        assertTrue(ref1.getFilePath().contains("enrichment"), "First file should be enrichment");
        assertEquals(2, ref1.getNestingDepth(), "First file should be at nesting depth 2");

        // Second resolved file should be direct reference (depth 1)
        ComponentLoader.ResolvedFileReference ref2 = resolvedRefs.get(1);
        assertTrue(ref2.getFilePath().contains("validation"), "Second file should be validation");
        assertEquals(1, ref2.getNestingDepth(), "Second file should be at nesting depth 1");

        logger.info("Nested references resolved successfully: {} files", resolvedRefs.size());
    }

    // ========================================
    // Circular Reference Detection Tests
    // ========================================

    @Test
    @DisplayName("Should detect circular component references")
    void testCircularReferenceDetection() {
        logger.info("=== Testing Circular Reference Detection ===");

        String componentPath = "scenario/circular-component-a.yaml";

        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadComponent(componentPath),
            "Should throw exception for circular reference"
        );

        assertTrue(exception.getMessage().contains("Circular") || exception.getMessage().contains("circular"),
            "Exception message should mention circular reference");
        logger.info("Circular reference correctly detected and prevented");
    }

    @Test
    @DisplayName("Should handle component with no circular references")
    void testNoCircularReferences() throws Exception {
        logger.info("=== Testing No Circular References ===");

        String componentPath = "scenario/nested-component-level1.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);

        // Should not throw exception - circular reference check passes
        assertDoesNotThrow(() -> loader.detectCircularReferences(component, componentPath),
            "Should not throw exception for valid nested structure");

        logger.info("No circular references detected (as expected)");
    }

    @Test
    @DisplayName("Should load component with only component-refs and config-files sections")
    void testPartialSectionsComponent() throws Exception {
        logger.info("=== Testing Partial Sections Component (only component-refs and config-files) ===");

        String componentPath = "scenario/partial-sections-component.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);

        // Verify component loaded successfully
        assertNotNull(component, "Component should be loaded");
        assertEquals("partial-sections-component", component.getId(), "Component ID should match");
        assertEquals("Partial Sections Component", component.getName(), "Component name should match");
        assertEquals("component", component.getType(), "Component type should be 'component'");

        // Verify that rule-configurations, enrichment-refs, and component-refs are empty (not used)
        assertTrue(component.getRuleConfigurations().isEmpty(),
            "Rule configurations should be empty");
        assertTrue(component.getEnrichmentRefs().isEmpty(),
            "Enrichment refs should be empty");
        assertTrue(component.getComponentRefs().isEmpty(),
            "Component refs should be empty");

        // Verify that only config-files is populated
        assertFalse(component.getConfigFiles().isEmpty(),
            "Config files should not be empty");

        assertEquals(2, component.getConfigFiles().size(),
            "Should have 2 config file references");

        // Verify getAllReferences() returns all references in correct order
        List<ComponentConfiguration.FileReference> allRefs = component.getAllReferences();
        assertEquals(2, allRefs.size(),
            "Should have 2 total references (2 config-files)");

        // Verify execution order: config-file (order 1), config-file (order 2)
        assertTrue(allRefs.get(0).getFile().contains("BasicStageConfigurationTest-validation-rules.yaml"),
            "First should be validation-rules config-file with execution-order 1");
        assertEquals(1, allRefs.get(0).getExecutionOrder(),
            "First reference should have execution-order 1");

        assertTrue(allRefs.get(1).getFile().contains("BasicStageConfigurationTest-enrichment-rules.yaml"),
            "Second should be enrichment-rules config-file with execution-order 2");
        assertEquals(2, allRefs.get(1).getExecutionOrder(),
            "Second reference should have execution-order 2");

        // Verify failure policies
        assertEquals("terminate", allRefs.get(0).getFailurePolicy(),
            "First config-file should have terminate policy");
        assertEquals("continue-with-warnings", allRefs.get(1).getFailurePolicy(),
            "Second config-file should have continue-with-warnings policy");

        logger.info("Partial sections component loaded successfully with {} total references", allRefs.size());
        logger.info("Component demonstrates flexibility: only config-files section used (no component-refs, rule-configurations, or enrichment-refs)");
    }

    // ========================================
    // Component Enabled Field Tests
    // ========================================

    @Test
    @DisplayName("Should default to enabled=true when not specified")
    void testComponentEnabledDefaultTrue() {
        logger.info("=== Testing Component Enabled Default ===");

        ComponentConfiguration component = new ComponentConfiguration();
        ComponentConfiguration.Metadata metadata = new ComponentConfiguration.Metadata();
        metadata.setId("test-component");
        metadata.setName("Test Component");
        metadata.setType("component");
        component.setMetadata(metadata);

        assertTrue(metadata.isEnabled(), "Component should be enabled by default");
        assertTrue(loader.isComponentEnabled(component), "ComponentLoader should report component as enabled");
    }

    @Test
    @DisplayName("Should respect enabled=false in metadata")
    void testComponentEnabledFalse() {
        logger.info("=== Testing Component Enabled=false ===");

        ComponentConfiguration component = new ComponentConfiguration();
        ComponentConfiguration.Metadata metadata = new ComponentConfiguration.Metadata();
        metadata.setId("disabled-component");
        metadata.setName("Disabled Component");
        metadata.setType("component");
        metadata.setEnabled(false);
        component.setMetadata(metadata);

        assertFalse(metadata.isEnabled(), "Component should be disabled when enabled=false");
        assertFalse(loader.isComponentEnabled(component), "ComponentLoader should report component as disabled");
    }

    @Test
    @DisplayName("Should respect enabled=true in metadata")
    void testComponentEnabledTrue() {
        logger.info("=== Testing Component Enabled=true ===");

        ComponentConfiguration component = new ComponentConfiguration();
        ComponentConfiguration.Metadata metadata = new ComponentConfiguration.Metadata();
        metadata.setId("enabled-component");
        metadata.setName("Enabled Component");
        metadata.setType("component");
        metadata.setEnabled(true);
        component.setMetadata(metadata);

        assertTrue(metadata.isEnabled(), "Component should be enabled when enabled=true");
        assertTrue(loader.isComponentEnabled(component), "ComponentLoader should report component as enabled");
    }

    @Test
    @DisplayName("Should handle null metadata gracefully")
    void testComponentEnabledNullMetadata() {
        logger.info("=== Testing Component Enabled with Null Metadata ===");

        ComponentConfiguration component = new ComponentConfiguration();
        // Don't set metadata

        assertTrue(loader.isComponentEnabled(component),
            "Component with null metadata should default to enabled");
    }
}
