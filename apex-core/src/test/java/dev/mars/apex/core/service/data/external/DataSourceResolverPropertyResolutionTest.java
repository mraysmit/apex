package dev.mars.apex.core.service.data.external;

import org.junit.jupiter.api.AfterEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for property resolution in DataSourceResolver.
 * 
 * Validates that environment variable placeholders (e.g., ${DB_PASSWORD}) 
 * are correctly resolved when loading external data-source configurations
 * via loadFromFileSystem or loadFromClasspath methods.
 * 
 * @author Mark A Ray-Smith Cityline Ltd
 * @since 2025-01-19
 */
@DisplayName("DataSourceResolver Property Resolution Tests")
public class DataSourceResolverPropertyResolutionTest {

    @TempDir
    Path tempDir;

    private DataSourceResolver resolver;

    // Track system properties we set so we can clean them up
    private static final String TEST_DB_HOST = "TEST_APEX_DB_HOST";
    private static final String TEST_DB_PORT = "TEST_APEX_DB_PORT";
    private static final String TEST_DB_USERNAME = "TEST_APEX_DB_USERNAME";
    private static final String TEST_DB_PASSWORD = "TEST_APEX_DB_PASSWORD";

    @BeforeEach
    void setUp() {
        resolver = new DataSourceResolver();
        resolver.clearCache();
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty(TEST_DB_HOST);
        System.clearProperty(TEST_DB_PORT);
        System.clearProperty(TEST_DB_USERNAME);
        System.clearProperty(TEST_DB_PASSWORD);
    }
    
    // Helper method to get connection property as String
    private String getConnectionProperty(ExternalDataSourceConfig config, String key) {
        Map<String, Object> connection = config.getSpec().getConnection();
        if (connection == null) return null;
        Object value = connection.get(key);
        return value != null ? value.toString() : null;
    }

    @Nested
    @DisplayName("File System Property Resolution")
    class FileSystemPropertyResolution {

        @Test
        @DisplayName("Should resolve ${VAR} placeholders from system properties")
        void shouldResolveDollarBracePlaceholdersFromSystemProperties() throws Exception {
            // Set up system properties
            System.setProperty(TEST_DB_HOST, "localhost");
            System.setProperty(TEST_DB_PORT, "5432");
            System.setProperty(TEST_DB_USERNAME, "testuser");
            System.setProperty(TEST_DB_PASSWORD, "secret123");

            String configWithPlaceholders = """
                metadata:
                  id: "property-resolution-test"
                  name: "property-resolution-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test property resolution"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "postgresql"
                    enabled: true
                    connection:
                      host: "${TEST_APEX_DB_HOST}"
                      port: "${TEST_APEX_DB_PORT}"
                      database: "testdb"
                      username: "${TEST_APEX_DB_USERNAME}"
                      password: "${TEST_APEX_DB_PASSWORD}"
                """;

            Path configFile = tempDir.resolve("config-with-placeholders.yaml");
            Files.writeString(configFile, configWithPlaceholders);

            ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());

            assertNotNull(resolved, "Resolved configuration should not be null");
            assertNotNull(resolved.getSpec(), "Spec should not be null");
            assertNotNull(resolved.getSpec().getConnection(), "Connection should not be null");

            // Verify placeholders were resolved
            assertEquals("localhost", getConnectionProperty(resolved, "host"),
                    "Host placeholder should be resolved");
            assertEquals("5432", getConnectionProperty(resolved, "port"),
                    "Port placeholder should be resolved");
            assertEquals("testuser", getConnectionProperty(resolved, "username"),
                    "Username placeholder should be resolved");
            assertEquals("secret123", getConnectionProperty(resolved, "password"),
                    "Password placeholder should be resolved");
        }

        @Test
        @DisplayName("Should resolve ${VAR:default} placeholders with default values")
        void shouldResolvePlaceholdersWithDefaultValues() throws Exception {
            // Don't set any system properties - should use defaults

            String configWithDefaults = """
                metadata:
                  id: "default-values-test"
                  name: "default-values-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test default value resolution"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "h2"
                    enabled: true
                    connection:
                      host: "${NONEXISTENT_HOST:default-host}"
                      port: "${NONEXISTENT_PORT:3306}"
                      database: "testdb"
                      username: "${NONEXISTENT_USER:defaultuser}"
                      password: "${NONEXISTENT_PASS:defaultpass}"
                """;

            Path configFile = tempDir.resolve("config-with-defaults.yaml");
            Files.writeString(configFile, configWithDefaults);

            ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());

            assertNotNull(resolved, "Resolved configuration should not be null");
            assertNotNull(resolved.getSpec().getConnection(), "Connection should not be null");

            // Verify default values were used
            assertEquals("default-host", getConnectionProperty(resolved, "host"),
                    "Default host should be used when property not found");
            assertEquals("3306", getConnectionProperty(resolved, "port"),
                    "Default port should be used when property not found");
            assertEquals("defaultuser", getConnectionProperty(resolved, "username"),
                    "Default username should be used when property not found");
            assertEquals("defaultpass", getConnectionProperty(resolved, "password"),
                    "Default password should be used when property not found");
        }

        @Test
        @DisplayName("Should resolve $(VAR) parenthesis-style placeholders")
        void shouldResolveParenthesisStylePlaceholders() throws Exception {
            System.setProperty(TEST_DB_HOST, "paren-host");
            System.setProperty(TEST_DB_PORT, "9999");

            String configWithParenPlaceholders = """
                metadata:
                  id: "paren-placeholder-test"
                  name: "paren-placeholder-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test parenthesis-style placeholder resolution"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "postgresql"
                    enabled: true
                    connection:
                      host: "$(TEST_APEX_DB_HOST)"
                      port: "$(TEST_APEX_DB_PORT)"
                      database: "testdb"
                      username: "$(NONEXISTENT_USER:parenuser)"
                      password: ""
                """;

            Path configFile = tempDir.resolve("config-with-paren-placeholders.yaml");
            Files.writeString(configFile, configWithParenPlaceholders);

            ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());

            assertNotNull(resolved, "Resolved configuration should not be null");
            assertNotNull(resolved.getSpec().getConnection(), "Connection should not be null");

            assertEquals("paren-host", getConnectionProperty(resolved, "host"),
                    "Parenthesis-style host placeholder should be resolved");
            assertEquals("9999", getConnectionProperty(resolved, "port"),
                    "Parenthesis-style port placeholder should be resolved");
            assertEquals("parenuser", getConnectionProperty(resolved, "username"),
                    "Parenthesis-style default should be used");
        }

        @Test
        @DisplayName("Should throw exception for unresolved required placeholders")
        void shouldThrowExceptionForUnresolvedPlaceholders() throws Exception {
            // Don't set any system properties - placeholder should remain unresolved

            String configWithUnresolvedPlaceholder = """
                metadata:
                  id: "unresolved-test"
                  name: "unresolved-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test unresolved placeholder exception"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "postgresql"
                    enabled: true
                    connection:
                      host: "${REQUIRED_BUT_MISSING_PROPERTY}"
                      port: "5432"
                      database: "testdb"
                      username: "user"
                      password: ""
                """;

            Path configFile = tempDir.resolve("config-with-unresolved.yaml");
            Files.writeString(configFile, configWithUnresolvedPlaceholder);

            DataSourceResolutionException exception = assertThrows(
                    DataSourceResolutionException.class,
                    () -> resolver.resolveDataSource(configFile.toString()),
                    "Should throw exception for unresolved placeholders"
            );

            // Check the exception message or its cause chain for the property name
            String fullMessage = getFullExceptionMessage(exception);
            assertTrue(fullMessage.contains("Property not found") ||
                       fullMessage.contains("REQUIRED_BUT_MISSING_PROPERTY"),
                    "Exception message should indicate missing property: " + fullMessage);
        }
        
        private String getFullExceptionMessage(Throwable t) {
            StringBuilder sb = new StringBuilder();
            while (t != null) {
                if (t.getMessage() != null) {
                    sb.append(t.getMessage()).append(" | ");
                }
                t = t.getCause();
            }
            return sb.toString();
        }

        @Test
        @DisplayName("Should resolve mixed placeholders in same configuration")
        void shouldResolveMixedPlaceholders() throws Exception {
            System.setProperty(TEST_DB_HOST, "mixed-host");

            String configWithMixedPlaceholders = """
                metadata:
                  id: "mixed-placeholder-test"
                  name: "mixed-placeholder-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test mixed placeholder styles"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "postgresql"
                    enabled: true
                    connection:
                      host: "${TEST_APEX_DB_HOST}"
                      port: "$(MIXED_PORT:8080)"
                      database: "${MIXED_DB:mixeddb}"
                      username: "user"
                      password: ""
                """;

            Path configFile = tempDir.resolve("config-with-mixed.yaml");
            Files.writeString(configFile, configWithMixedPlaceholders);

            ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());

            assertNotNull(resolved, "Resolved configuration should not be null");
            assertEquals("mixed-host", getConnectionProperty(resolved, "host"),
                    "Curly brace placeholder should be resolved");
            assertEquals("8080", getConnectionProperty(resolved, "port"),
                    "Parenthesis placeholder with default should be resolved");
            assertEquals("mixeddb", getConnectionProperty(resolved, "database"),
                    "Curly brace placeholder with default should be resolved");
        }

        @Test
        @DisplayName("Should handle configuration without any placeholders")
        void shouldHandleConfigurationWithoutPlaceholders() throws Exception {
            String configWithoutPlaceholders = """
                metadata:
                  id: "no-placeholder-test"
                  name: "no-placeholder-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test configuration without placeholders"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "h2"
                    enabled: true
                    connection:
                      host: "localhost"
                      port: "9092"
                      database: "testdb"
                      username: "sa"
                      password: ""
                """;

            Path configFile = tempDir.resolve("config-without-placeholders.yaml");
            Files.writeString(configFile, configWithoutPlaceholders);

            ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());

            assertNotNull(resolved, "Resolved configuration should not be null");
            assertEquals("localhost", getConnectionProperty(resolved, "host"));
            assertEquals("9092", getConnectionProperty(resolved, "port"));
            assertEquals("sa", getConnectionProperty(resolved, "username"));
        }

        @Test
        @DisplayName("Should resolve placeholders in queries section")
        void shouldResolvePlaceholdersInQueries() throws Exception {
            System.setProperty("TEST_SCHEMA", "public");
            System.setProperty("TEST_TABLE", "customers");

            String configWithQueryPlaceholders = """
                metadata:
                  id: "query-placeholder-test"
                  name: "query-placeholder-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test placeholder resolution in queries"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "postgresql"
                    enabled: true
                    connection:
                      host: "localhost"
                      port: "5432"
                      database: "testdb"
                      username: "user"
                      password: ""
                    queries:
                      getCustomer: "SELECT * FROM ${TEST_SCHEMA}.${TEST_TABLE} WHERE id = :id"
                """;

            Path configFile = tempDir.resolve("config-with-query-placeholders.yaml");
            Files.writeString(configFile, configWithQueryPlaceholders);

            ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());

            assertNotNull(resolved, "Resolved configuration should not be null");
            assertNotNull(resolved.getSpec().getQueries(), "Queries should not be null");
            
            String resolvedQuery = resolved.getSpec().getQueries().get("getCustomer");
            assertNotNull(resolvedQuery, "getCustomer query should exist");
            assertTrue(resolvedQuery.contains("public.customers"),
                    "Query placeholders should be resolved: " + resolvedQuery);
            
            // Clean up
            System.clearProperty("TEST_SCHEMA");
            System.clearProperty("TEST_TABLE");
        }
    }

    @Nested
    @DisplayName("Caching Behavior with Property Resolution")
    class CachingBehavior {

        @Test
        @DisplayName("Should cache resolved configuration (not re-resolve on second call)")
        void shouldCacheResolvedConfiguration() throws Exception {
            System.setProperty(TEST_DB_HOST, "cached-host");

            String config = """
                metadata:
                  id: "caching-test"
                  name: "caching-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test caching behavior"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "h2"
                    enabled: true
                    connection:
                      host: "${TEST_APEX_DB_HOST}"
                      port: "5432"
                      database: "testdb"
                      username: "user"
                      password: ""
                """;

            Path configFile = tempDir.resolve("caching-test.yaml");
            Files.writeString(configFile, config);

            // First resolution
            ExternalDataSourceConfig first = resolver.resolveDataSource(configFile.toString());
            assertEquals("cached-host", getConnectionProperty(first, "host"));

            // Change the system property
            System.setProperty(TEST_DB_HOST, "new-host-value");

            // Second resolution should return cached value
            ExternalDataSourceConfig second = resolver.resolveDataSource(configFile.toString());
            assertEquals("cached-host", getConnectionProperty(second, "host"),
                    "Cached value should be returned, not re-resolved");

            // Same object should be returned
            assertSame(first, second, "Same cached object should be returned");
        }

        @Test
        @DisplayName("Should re-resolve after cache clear")
        void shouldReResolveAfterCacheClear() throws Exception {
            System.setProperty(TEST_DB_HOST, "initial-host");

            String config = """
                metadata:
                  id: "cache-clear-test"
                  name: "cache-clear-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test cache clear behavior"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "h2"
                    enabled: true
                    connection:
                      host: "${TEST_APEX_DB_HOST}"
                      port: "5432"
                      database: "testdb"
                      username: "user"
                      password: ""
                """;

            Path configFile = tempDir.resolve("cache-clear-test.yaml");
            Files.writeString(configFile, config);

            // First resolution
            ExternalDataSourceConfig first = resolver.resolveDataSource(configFile.toString());
            assertEquals("initial-host", getConnectionProperty(first, "host"));

            // Clear cache and change property
            resolver.clearCache();
            System.setProperty(TEST_DB_HOST, "updated-host");

            // Second resolution should get new value
            ExternalDataSourceConfig second = resolver.resolveDataSource(configFile.toString());
            assertEquals("updated-host", getConnectionProperty(second, "host"),
                    "New value should be resolved after cache clear");
        }
    }

    @Nested
    @DisplayName("System Property vs Environment Variable Priority")
    class PropertyPriority {

        @Test
        @DisplayName("System property should take precedence over environment variable")
        void systemPropertyShouldTakePrecedence() throws Exception {
            // Note: We can't easily set environment variables in Java tests,
            // but we can verify system property resolution works
            System.setProperty(TEST_DB_HOST, "system-property-host");

            String config = """
                metadata:
                  id: "priority-test"
                  name: "priority-test"
                  type: "external-data-config"
                  version: "1.0.0"
                  description: "Test property priority"
                data-sources:
                  - name: "test-database"
                    type: "database"
                    source-type: "h2"
                    enabled: true
                    connection:
                      host: "${TEST_APEX_DB_HOST}"
                      port: "5432"
                      database: "testdb"
                      username: "user"
                      password: ""
                """;

            Path configFile = tempDir.resolve("priority-test.yaml");
            Files.writeString(configFile, config);

            ExternalDataSourceConfig resolved = resolver.resolveDataSource(configFile.toString());
            assertEquals("system-property-host", getConnectionProperty(resolved, "host"),
                    "System property value should be used");
        }
    }
}
