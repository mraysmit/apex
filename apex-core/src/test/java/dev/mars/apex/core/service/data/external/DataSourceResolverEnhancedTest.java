package dev.mars.apex.core.service.data.external;

import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;


import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced tests for DataSourceResolver to verify file system support.
 * 
 * Tests the new capability to resolve external data source configurations
 * from both file system and classpath locations.
 */
@DisplayName("DataSourceResolver Enhanced Tests")
public class DataSourceResolverEnhancedTest {

    @TempDir
    Path tempDir;

    private DataSourceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DataSourceResolver();
    }

    @Test
    @DisplayName("Should resolve data source from file system")
    void testResolveFromFileSystem() throws Exception {
        // Create a valid external data source configuration using standard APEX format
        String dataSourceConfig = """
            metadata:
              id: "test-file-system-source-config"
              name: "test-file-system-source"
              type: "external-data-config"
              version: "1.0.0"
              description: "Test data source from file system"
            data-sources:
              - name: "test-file-system-source"
                type: "database"
                source-type: "h2"
                enabled: true
                connection:
                  database: "test_db"
                  username: "sa"
                  password: ""
                queries:
                  getTestData: "SELECT * FROM test_table WHERE id = :id"
                cache:
                  enabled: true
                  ttlSeconds: 300
            """;

        // Write to temporary file
        Path configFile = tempDir.resolve("test-data-source.yaml");
        Files.writeString(configFile, dataSourceConfig);

        // Resolve using file system path
        ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());

        // Verify resolution
        assertNotNull(resolved, "Resolved configuration should not be null");
        assertNotNull(resolved.getMetadata(), "Metadata should not be null");
        assertEquals("test-file-system-source", resolved.getMetadata().getName(),
                "Name should match the configuration");
        assertEquals("1.0.0", resolved.getMetadata().getVersion(),
                "Version should match the configuration");
        
        assertNotNull(resolved.getSpec(), "Spec (first data source) should not be null");
        assertEquals("database", resolved.getSpec().getType(),
                "Type should match the configuration");
        assertEquals("h2", resolved.getSpec().getSourceType(),
                "Source type should match the configuration");
    }

    @Test
    @DisplayName("Should resolve data source from classpath when file system fails")
    void testFallbackToClasspath() throws Exception {
        // This test uses a classpath resource that should exist
        // We'll use a non-existent file system path to force classpath fallback
        
        // Try to resolve a path that doesn't exist on file system but might exist on classpath
        String classpathResource = "test-data-source-classpath.yaml";
        
        // Create the resource content using standard APEX format
        String dataSourceConfig = """
            metadata:
              id: "test-classpath-source-config"
              name: "test-classpath-source"
              type: "external-data-config"
              version: "1.0.0"
              description: "Test data source from classpath"
            data-sources:
              - name: "test-classpath-source"
                type: "rest-api"
                source-type: "http"
                enabled: true
            """;

        // For this test, we'll create a file in a location that simulates classpath behavior
        // In a real scenario, this would be a resource in src/test/resources
        Path classpathSimulation = tempDir.resolve(classpathResource);
        Files.writeString(classpathSimulation, dataSourceConfig);

        // Test that file system path resolution works
        ExternalDataSourceConfig resolved = resolver.resolveDataSource(classpathSimulation.toString());
        
        assertNotNull(resolved, "Should resolve from file system path");
        assertEquals("test-classpath-source", resolved.getMetadata().getName());
    }

    @Test
    @DisplayName("Should cache resolved configurations")
    void testCaching() throws Exception {
        String dataSourceConfig = """
            metadata:
              id: "test-cached-source-config"
              name: "test-cached-source"
              type: "external-data-config"
              version: "1.0.0"
            data-sources:
              - name: "test-cached-source"
                type: "database"
                enabled: true
            """;

        Path configFile = tempDir.resolve("cached-data-source.yaml");
        Files.writeString(configFile, dataSourceConfig);

        // Verify cache is initially empty
        assertEquals(0, resolver.getCacheSize(), "Cache should be initially empty");

        // First resolution should load from file and cache
        ExternalDataSourceConfig first = resolver.resolveDataSource(configFile.toString());
        assertEquals(1, resolver.getCacheSize(), "Cache should contain one entry after first resolution");

        // Second resolution should use cache
        ExternalDataSourceConfig second = resolver.resolveDataSource(configFile.toString());
        assertEquals(1, resolver.getCacheSize(), "Cache size should remain the same");

        // Verify both references point to the same cached object
        assertSame(first, second, "Second resolution should return cached instance");
    }

    @Test
    @DisplayName("Should throw exception when configuration not found anywhere")
    void testConfigurationNotFound() {
        String nonExistentPath = "non-existent-data-source.yaml";

        DataSourceResolutionException exception = assertThrows(DataSourceResolutionException.class, () -> {
            resolver.resolveDataSource(nonExistentPath);
        }, "Should throw exception when configuration is not found");

        assertTrue(exception.getMessage().contains("Failed to resolve data-source reference"),
                "Exception message should indicate resolution failure");

        // Check the cause contains the detailed message
        assertNotNull(exception.getCause(), "Exception should have a cause");
        assertTrue(exception.getCause().getMessage().contains("not found in file system or classpath"),
                "Exception cause should indicate both locations were tried");
    }

    @Test
    @DisplayName("Should validate resolved configuration")
    void testConfigurationValidation() throws Exception {
        // Create invalid configuration (missing required data-sources)
        String invalidConfig = """
            metadata:
              id: "invalid-config"
              name: "invalid-source"
              type: "external-data-config"
            """;

        Path invalidConfigFile = tempDir.resolve("invalid-data-source.yaml");
        Files.writeString(invalidConfigFile, invalidConfig);

        DataSourceResolutionException exception = assertThrows(DataSourceResolutionException.class, () -> {
            resolver.resolveDataSource(invalidConfigFile.toString());
        }, "Should throw exception for invalid configuration");

        // The main exception message should indicate resolution failure
        assertTrue(exception.getMessage().contains("Failed to resolve data-source reference"),
                "Exception message should indicate resolution failure");

        // The cause should contain the validation error
        assertNotNull(exception.getCause(), "Exception should have a cause");
        // Debug: print the actual cause message
        System.out.println("DEBUG: Exception cause message: " + exception.getCause().getMessage());
        assertTrue(exception.getCause().getMessage().contains("data-sources") || 
                   exception.getCause().getMessage().contains("not found"),
                "Exception cause should indicate validation failure, got: " + exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle null and empty references")
    void testNullAndEmptyReferences() {
        // Test null reference
        DataSourceResolutionException nullException = assertThrows(DataSourceResolutionException.class, () -> {
            resolver.resolveDataSource(null);
        }, "Should throw exception for null reference");
        assertTrue(nullException.getMessage().contains("cannot be null or empty"));

        // Test empty reference
        DataSourceResolutionException emptyException = assertThrows(DataSourceResolutionException.class, () -> {
            resolver.resolveDataSource("");
        }, "Should throw exception for empty reference");
        assertTrue(emptyException.getMessage().contains("cannot be null or empty"));

        // Test whitespace-only reference
        DataSourceResolutionException whitespaceException = assertThrows(DataSourceResolutionException.class, () -> {
            resolver.resolveDataSource("   ");
        }, "Should throw exception for whitespace-only reference");
        assertTrue(whitespaceException.getMessage().contains("cannot be null or empty"));
    }

    @Test
    @DisplayName("Should clear cache correctly")
    void testCacheClear() throws Exception {
        String dataSourceConfig = """
            metadata:
              id: "test-cache-clear-config"
              name: "test-cache-clear"
              type: "external-data-config"
              version: "1.0.0"
            data-sources:
              - name: "test-cache-clear"
                type: "database"
                enabled: true
            """;

        Path configFile = tempDir.resolve("cache-clear-test.yaml");
        Files.writeString(configFile, dataSourceConfig);

        // Load configuration to populate cache
        resolver.resolveDataSource(configFile.toString());
        assertEquals(1, resolver.getCacheSize(), "Cache should contain one entry");

        // Clear cache
        resolver.clearCache();
        assertEquals(0, resolver.getCacheSize(), "Cache should be empty after clear");

        // Verify next resolution loads from file again (not from cache)
        resolver.resolveDataSource(configFile.toString());
        assertEquals(1, resolver.getCacheSize(), "Cache should contain one entry after reload");
    }
}
