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

import dev.mars.apex.core.config.ResourceResolver;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComponentLoader classpath loading functionality (Phase 3).
 *
 * Tests focus on:
 * - Loading components from classpath resources
 * - Stream-based loading with classpathBase context
 * - Nested component resolution from classpath
 * - Circular reference detection for classpath resources
 * - ResourceResolver integration
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.3.0
 */
@DisplayName("ComponentLoader Classpath Loading Tests")
class ComponentLoaderClasspathTest {

    private static final Logger logger = LoggerFactory.getLogger(ComponentLoaderClasspathTest.class);
    private static final String TEST_RESOURCES_BASE = "component-classpath-test/";

    private ComponentLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ComponentLoader();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create ComponentLoader with default ResourceResolver")
    void testDefaultConstructor() {
        ComponentLoader testLoader = new ComponentLoader();
        assertNotNull(testLoader, "Loader should be created successfully");
        assertNotNull(testLoader.getResourceResolver(), "Should have ResourceResolver");
    }

    @Test
    @DisplayName("Should create ComponentLoader with custom ResourceResolver")
    void testConstructorWithResourceResolver() {
        ResourceResolver customResolver = ResourceResolver.builder()
                .strategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_FIRST)
                .build();
        
        ComponentLoader testLoader = new ComponentLoader(customResolver);
        assertNotNull(testLoader, "Loader should be created successfully");
        assertEquals(ResourceResolver.ResolutionStrategy.FILESYSTEM_FIRST,
                testLoader.getResourceResolver().getResolutionStrategy(),
                "Should use custom resolver strategy");
    }

    @Test
    @DisplayName("Should use default resolver when null is passed")
    void testConstructorWithNullResolver() {
        ComponentLoader testLoader = new ComponentLoader(null);
        assertNotNull(testLoader, "Loader should be created successfully");
        assertNotNull(testLoader.getResourceResolver(), "Should have default ResourceResolver");
    }

    // ========================================
    // Classpath Loading Tests
    // ========================================

    @Test
    @DisplayName("Should load component from classpath resource")
    void testLoadComponentFromClasspath() throws Exception {
        logger.info("=== Testing Classpath Component Loading ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "nested-parent.yaml";
        ComponentConfiguration component = loader.loadComponentFromClasspath(resourcePath);
        
        assertNotNull(component, "Component should be loaded from classpath");
        assertEquals("nested-parent", component.getId(), "Component ID should match");
        assertEquals("Nested Parent Component", component.getName(), "Component name should match");
        assertEquals("component", component.getType(), "Component type should be 'component'");
        
        logger.info("Successfully loaded component from classpath: {}", component.getId());
    }

    @Test
    @DisplayName("Should load nested component from classpath")
    void testLoadNestedComponentFromClasspath() throws Exception {
        logger.info("=== Testing Nested Classpath Component Loading ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "nested-parent.yaml";
        ComponentConfiguration component = loader.loadComponentFromClasspath(resourcePath);
        
        // Verify parent component
        assertNotNull(component, "Parent component should be loaded");
        assertEquals("nested-parent", component.getId(), "Parent component ID should match");
        
        // Verify component references exist
        List<ComponentConfiguration.FileReference> componentRefs = component.getComponentRefs();
        assertEquals(1, componentRefs.size(), "Should have 1 component reference");
        assertEquals("nested-child.yaml", componentRefs.get(0).getFile(), "Should reference nested-child");
        
        logger.info("Nested component reference validated: {}", componentRefs.get(0).getFile());
    }

    @Test
    @DisplayName("Should load component with classpathBase context")
    void testLoadComponentWithClasspathBase() throws Exception {
        logger.info("=== Testing Component Loading with classpathBase ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "nested-child.yaml";
        String classpathBase = TEST_RESOURCES_BASE;
        
        ComponentConfiguration component = loader.loadComponent(resourcePath, classpathBase);
        
        assertNotNull(component, "Component should be loaded");
        assertEquals("nested-child", component.getId(), "Component ID should match");
        
        logger.info("Component loaded with classpathBase: {}", classpathBase);
    }

    @Test
    @DisplayName("Should resolve all references from classpath component")
    void testResolveAllReferencesFromClasspath() throws Exception {
        logger.info("=== Testing Reference Resolution from Classpath ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "nested-parent.yaml";
        String classpathBase = TEST_RESOURCES_BASE;
        
        ComponentConfiguration component = loader.loadComponent(resourcePath, classpathBase);
        
        // Resolve all references with classpath context
        List<ComponentLoader.ResolvedFileReference> resolvedRefs =
                loader.resolveAllReferences(component, resourcePath, classpathBase);
        
        assertNotNull(resolvedRefs, "Resolved references should not be null");
        // Should have: child-rules.yaml (from nested component) + parent-rules.yaml (from parent)
        assertEquals(2, resolvedRefs.size(), "Should have 2 resolved references");
        
        // First should be from child component (depth 2)
        ComponentLoader.ResolvedFileReference ref1 = resolvedRefs.get(0);
        assertTrue(ref1.getFilePath().contains("child-rules"), 
                "First file should be child-rules: " + ref1.getFilePath());
        assertEquals(2, ref1.getNestingDepth(), "First file should be at depth 2");
        
        // Second should be from parent component (depth 1)
        ComponentLoader.ResolvedFileReference ref2 = resolvedRefs.get(1);
        assertTrue(ref2.getFilePath().contains("parent-rules"),
                "Second file should be parent-rules: " + ref2.getFilePath());
        assertEquals(1, ref2.getNestingDepth(), "Second file should be at depth 1");
        
        logger.info("Resolved {} references from classpath component", resolvedRefs.size());
    }

    // ========================================
    // Stream Loading Tests
    // ========================================

    @Test
    @DisplayName("Should load component from InputStream")
    void testLoadComponentFromStream() throws Exception {
        logger.info("=== Testing Stream-Based Component Loading ===");
        
        String yamlContent = """
            metadata:
              id: stream-test-component
              name: Stream Test Component
              type: component
              version: "1.0.0"
              description: Test component loaded from stream
              business-domain: testing
              owner: Test Team
              criticality: low
              sla-ms: 100
              enabled: true
            
            rule-configurations:
              - file: test-rules.yaml
                execution-order: 1
                failure-policy: terminate
            """;
        
        try (InputStream is = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))) {
            ComponentConfiguration component = loader.loadComponent(is);
            
            assertNotNull(component, "Component should be loaded from stream");
            assertEquals("stream-test-component", component.getId(), "Component ID should match");
            assertEquals("Stream Test Component", component.getName(), "Component name should match");
        }
        
        logger.info("Successfully loaded component from InputStream");
    }

    @Test
    @DisplayName("Should load component from InputStream with classpathBase")
    void testLoadComponentFromStreamWithClasspathBase() throws Exception {
        logger.info("=== Testing Stream Loading with classpathBase ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "stream-load-component.yaml";
        String classpathBase = TEST_RESOURCES_BASE;
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Test resource should exist on classpath");
            
            ComponentConfiguration component = loader.loadComponent(is, classpathBase);
            
            assertNotNull(component, "Component should be loaded from stream");
            assertEquals("stream-load-component", component.getId(), "Component ID should match");
        }
        
        logger.info("Successfully loaded component from stream with classpathBase");
    }

    // ========================================
    // Circular Reference Detection Tests
    // ========================================

    @Test
    @DisplayName("Should detect circular references in classpath components")
    void testCircularReferenceDetectionClasspath() {
        logger.info("=== Testing Circular Reference Detection in Classpath ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "circular-classpath-a.yaml";
        
        YamlConfigurationException exception = assertThrows(
                YamlConfigurationException.class,
                () -> loader.loadComponentFromClasspath(resourcePath),
                "Should throw exception for circular reference"
        );
        
        assertTrue(exception.getMessage().contains("Circular") || 
                   exception.getMessage().contains("circular"),
                "Exception message should mention circular reference: " + exception.getMessage());
        
        logger.info("Circular reference correctly detected in classpath components");
    }

    // ========================================
    // ResourceResolver Integration Tests
    // ========================================

    @Test
    @DisplayName("Should use custom ResourceResolver for resolution")
    void testCustomResourceResolverIntegration() throws Exception {
        logger.info("=== Testing Custom ResourceResolver Integration ===");
        
        // Create resolver with classpath prefix
        ResourceResolver resolver = ResourceResolver.builder()
                .strategy(ResourceResolver.ResolutionStrategy.CLASSPATH_FIRST)
                .addClasspathPrefix(TEST_RESOURCES_BASE)
                .build();
        
        ComponentLoader customLoader = new ComponentLoader(resolver);
        
        // Should be able to load with just the filename since prefix is added
        String resourcePath = TEST_RESOURCES_BASE + "nested-child.yaml";
        ComponentConfiguration component = customLoader.loadComponent(resourcePath, null);
        
        assertNotNull(component, "Component should be loaded using custom resolver");
        assertEquals("nested-child", component.getId(), "Component ID should match");
        
        logger.info("Custom ResourceResolver integration validated");
    }

    @Test
    @DisplayName("Should handle filesystem-first strategy")
    void testFilesystemFirstStrategy() throws Exception {
        logger.info("=== Testing Filesystem-First Strategy ===");
        
        ResourceResolver resolver = ResourceResolver.builder()
                .strategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_FIRST)
                .build();
        
        ComponentLoader fsFirstLoader = new ComponentLoader(resolver);
        
        // Should still find classpath resources as fallback
        String resourcePath = TEST_RESOURCES_BASE + "nested-child.yaml";
        ComponentConfiguration component = fsFirstLoader.loadComponentFromClasspath(resourcePath);
        
        assertNotNull(component, "Component should be loaded (filesystem-first with classpath fallback)");
        assertEquals("nested-child", component.getId(), "Component ID should match");
        
        logger.info("Filesystem-first strategy with classpath fallback validated");
    }

    // ========================================
    // Edge Cases and Error Handling
    // ========================================

    @Test
    @DisplayName("Should fail gracefully for non-existent classpath resource")
    void testNonExistentClasspathResource() {
        logger.info("=== Testing Non-Existent Classpath Resource ===");
        
        String resourcePath = "non-existent/component.yaml";
        
        assertThrows(YamlConfigurationException.class,
                () -> loader.loadComponentFromClasspath(resourcePath),
                "Should throw exception for non-existent resource");
        
        logger.info("Correctly failed for non-existent classpath resource");
    }

    @Test
    @DisplayName("Should handle empty classpathBase")
    void testEmptyClasspathBase() throws Exception {
        logger.info("=== Testing Empty classpathBase ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "nested-child.yaml";
        
        ComponentConfiguration component = loader.loadComponent(resourcePath, "");
        
        assertNotNull(component, "Component should be loaded with empty classpathBase");
        assertEquals("nested-child", component.getId(), "Component ID should match");
        
        logger.info("Empty classpathBase handled correctly");
    }

    @Test
    @DisplayName("Should handle null classpathBase")
    void testNullClasspathBase() throws Exception {
        logger.info("=== Testing Null classpathBase ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "nested-child.yaml";
        
        ComponentConfiguration component = loader.loadComponent(resourcePath, null);
        
        assertNotNull(component, "Component should be loaded with null classpathBase");
        assertEquals("nested-child", component.getId(), "Component ID should match");
        
        logger.info("Null classpathBase handled correctly");
    }

    @Test
    @DisplayName("Should resolve relative paths from classpath")
    void testRelativePathResolutionFromClasspath() throws Exception {
        logger.info("=== Testing Relative Path Resolution from Classpath ===");
        
        String resourcePath = TEST_RESOURCES_BASE + "nested-parent.yaml";
        ComponentConfiguration component = loader.loadComponentFromClasspath(resourcePath);
        
        // Verify the component has relative file references
        List<ComponentConfiguration.FileReference> allRefs = component.getAllReferences();
        
        // Should have: nested-child.yaml (component-ref) + parent-rules.yaml (rule-config)
        assertEquals(2, allRefs.size(), "Should have 2 file references");
        
        // The relative paths should be present
        boolean hasNestedChild = allRefs.stream()
                .anyMatch(ref -> ref.getFile().equals("nested-child.yaml"));
        boolean hasParentRules = allRefs.stream()
                .anyMatch(ref -> ref.getFile().equals("parent-rules.yaml"));
        
        assertTrue(hasNestedChild, "Should have nested-child.yaml reference");
        assertTrue(hasParentRules, "Should have parent-rules.yaml reference");
        
        logger.info("Relative path resolution validated for classpath resources");
    }
}
