package dev.mars.apex.demo.support.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for ResourcePathResolver to validate resource path migration
 * during the apex-demo reorganization.
 * 
 * These tests ensure that:
 * 1. Old paths are correctly mapped to new paths
 * 2. Unmapped paths are returned unchanged
 * 3. All new paths exist in the classpath
 * 4. Migration mappings are complete and accurate
 * 
 * @author apex-demo reorganization team
 * @version 1.0
 * @since 2025-08-24
 */
class ResourcePathResolverTest {

    @Test
    @DisplayName("Should resolve bootstrap paths correctly")
    void shouldResolveBootstrapPaths() {
        // Test bootstrap configuration file
        String oldPath = "bootstrap/custody-auto-repair-bootstrap.yaml";
        String expectedNewPath = "demos/bootstrap/custody-auto-repair/bootstrap-config.yaml";
        String actualNewPath = ResourcePathResolver.resolvePath(oldPath);
        
        assertEquals(expectedNewPath, actualNewPath, 
            "Bootstrap config path should be resolved correctly");
        
        // Test bootstrap dataset file
        String oldDatasetPath = "bootstrap/datasets/market-data.yaml";
        String expectedDatasetPath = "demos/bootstrap/custody-auto-repair/datasets/market-data.yaml";
        String actualDatasetPath = ResourcePathResolver.resolvePath(oldDatasetPath);
        
        assertEquals(expectedDatasetPath, actualDatasetPath,
            "Bootstrap dataset path should be resolved correctly");
    }

    @Test
    @DisplayName("Should resolve lookup example paths correctly")
    void shouldResolveLookupExamplePaths() {
        // Test simple field lookup
        String oldPath = "examples/lookups/simple-field-lookup.yaml";
        String expectedNewPath = "demos/patterns/lookups/simple-field-lookup.yaml";
        String actualNewPath = ResourcePathResolver.resolvePath(oldPath);
        
        assertEquals(expectedNewPath, actualNewPath,
            "Simple field lookup path should be resolved correctly");
        
        // Test conditional expression lookup
        String oldConditionalPath = "examples/lookups/conditional-expression-lookup.yaml";
        String expectedConditionalPath = "demos/patterns/lookups/conditional-expression-lookup.yaml";
        String actualConditionalPath = ResourcePathResolver.resolvePath(oldConditionalPath);
        
        assertEquals(expectedConditionalPath, actualConditionalPath,
            "Conditional expression lookup path should be resolved correctly");
        
        // Test nested field lookup
        String oldNestedPath = "examples/lookups/nested-field-lookup.yaml";
        String expectedNestedPath = "demos/patterns/lookups/nested-field-lookup.yaml";
        String actualNestedPath = ResourcePathResolver.resolvePath(oldNestedPath);
        
        assertEquals(expectedNestedPath, actualNestedPath,
            "Nested field lookup path should be resolved correctly");
        
        // Test compound key lookup
        String oldCompoundPath = "examples/lookups/compound-key-lookup.yaml";
        String expectedCompoundPath = "demos/patterns/lookups/compound-key-lookup.yaml";
        String actualCompoundPath = ResourcePathResolver.resolvePath(oldCompoundPath);
        
        assertEquals(expectedCompoundPath, actualCompoundPath,
            "Compound key lookup path should be resolved correctly");
    }

    @Test
    @DisplayName("Should resolve financial settlement paths correctly")
    void shouldResolveFinancialSettlementPaths() {
        String oldPath = "financial-settlement/comprehensive-settlement-enrichment.yaml";
        String expectedNewPath = "demos/industry/financial-services/settlement/comprehensive-settlement-enrichment.yaml";
        String actualNewPath = ResourcePathResolver.resolvePath(oldPath);
        
        assertEquals(expectedNewPath, actualNewPath,
            "Financial settlement path should be resolved correctly");
    }

    @Test
    @DisplayName("Should resolve configuration paths correctly")
    void shouldResolveConfigurationPaths() {
        String oldPath = "config/financial-validation-rules.yaml";
        String expectedNewPath = "demos/fundamentals/rules/financial-validation-rules.yaml";
        String actualNewPath = ResourcePathResolver.resolvePath(oldPath);
        
        assertEquals(expectedNewPath, actualNewPath,
            "Configuration path should be resolved correctly");
    }

    @Test
    @DisplayName("Should resolve demo rules paths correctly")
    void shouldResolveDemoRulesPaths() {
        // Test custody auto-repair rules
        String oldPath = "demo-rules/custody-auto-repair-rules.yaml";
        String expectedNewPath = "demos/industry/financial-services/custody/custody-auto-repair-rules.yaml";
        String actualNewPath = ResourcePathResolver.resolvePath(oldPath);
        
        assertEquals(expectedNewPath, actualNewPath,
            "Custody auto-repair rules path should be resolved correctly");
        
        // Test quick start rules
        String oldQuickStartPath = "demo-rules/quick-start.yaml";
        String expectedQuickStartPath = "demos/quickstart/quick-start.yaml";
        String actualQuickStartPath = ResourcePathResolver.resolvePath(oldQuickStartPath);
        
        assertEquals(expectedQuickStartPath, actualQuickStartPath,
            "Quick start rules path should be resolved correctly");
    }

    @Test
    @DisplayName("Should resolve scenario paths correctly")
    void shouldResolveScenarioPaths() {
        String oldPath = "scenarios/otc-options-scenario.yaml";
        String expectedNewPath = "demos/advanced/complex-scenarios/otc-options-scenario.yaml";
        String actualNewPath = ResourcePathResolver.resolvePath(oldPath);
        
        assertEquals(expectedNewPath, actualNewPath,
            "OTC options scenario path should be resolved correctly");
    }

    @Test
    @DisplayName("Should return unchanged path for unmapped paths")
    void shouldReturnUnchangedPathForUnmappedPaths() {
        String unmappedPath = "some/unknown/path.yaml";
        String actualPath = ResourcePathResolver.resolvePath(unmappedPath);
        
        assertEquals(unmappedPath, actualPath,
            "Unmapped paths should be returned unchanged");
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void shouldHandleNullInputGracefully() {
        String result = ResourcePathResolver.resolvePath(null);
        assertNull(result, "Null input should return null");
    }

    @Test
    @DisplayName("Should correctly identify paths with migrations")
    void shouldCorrectlyIdentifyPathsWithMigrations() {
        // Test path with migration
        assertTrue(ResourcePathResolver.hasMigration("bootstrap/custody-auto-repair-bootstrap.yaml"),
            "Should identify paths with migrations");
        
        // Test path without migration
        assertFalse(ResourcePathResolver.hasMigration("some/unknown/path.yaml"),
            "Should identify paths without migrations");
        
        // Test null input
        assertFalse(ResourcePathResolver.hasMigration(null),
            "Should handle null input gracefully");
    }

    @Test
    @DisplayName("Should provide access to all migrations")
    void shouldProvideAccessToAllMigrations() {
        Map<String, String> migrations = ResourcePathResolver.getAllMigrations();
        
        assertNotNull(migrations, "Migrations map should not be null");
        assertFalse(migrations.isEmpty(), "Migrations map should not be empty");
        
        // Verify some key migrations exist
        assertTrue(migrations.containsKey("bootstrap/custody-auto-repair-bootstrap.yaml"),
            "Should contain bootstrap migration");
        assertTrue(migrations.containsKey("examples/lookups/simple-field-lookup.yaml"),
            "Should contain lookup migration");
        assertTrue(migrations.containsKey("financial-settlement/comprehensive-settlement-enrichment.yaml"),
            "Should contain financial settlement migration");
        
        // Verify the map is unmodifiable
        assertThrows(UnsupportedOperationException.class, 
            () -> migrations.put("test", "test"),
            "Migrations map should be unmodifiable");
    }

    @Test
    @DisplayName("Should validate that key new paths exist in classpath")
    void shouldValidateKeyNewPathsExistInClasspath() {
        // Test that some key new paths exist
        ClassLoader classLoader = getClass().getClassLoader();
        
        // Check that our newly created files exist
        assertNotNull(classLoader.getResource("demos/patterns/lookups/simple-field-lookup.yaml"),
            "Simple field lookup should exist in new location");
        assertNotNull(classLoader.getResource("demos/patterns/lookups/conditional-expression-lookup.yaml"),
            "Conditional expression lookup should exist in new location");
        assertNotNull(classLoader.getResource("demos/quickstart/quick-start.yaml"),
            "Quick start should exist in new location");
        assertNotNull(classLoader.getResource("demos/fundamentals/rules/financial-validation-rules.yaml"),
            "Financial validation rules should exist in new location");
    }
}
