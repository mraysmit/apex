package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for property resolution in YAML configuration loading.
 * 
 * This test verifies that property resolution works correctly when loading
 * YAML configurations through all the different loading methods.
 */
public class PropertyResolutionIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(PropertyResolutionIntegrationTest.class.getName());
    
    private YamlConfigurationLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        LOGGER.info("Setting up PropertyResolutionIntegrationTest");
        
        loader = new YamlConfigurationLoader();
        
        // Set up test environment variables and system properties
        System.setProperty("TEST_DB_HOST", "test-host");
        System.setProperty("TEST_DB_PORT", "5432");
        System.setProperty("TEST_DB_NAME", "test_database");
        System.setProperty("TEST_DB_USER", "test_user");
        System.setProperty("TEST_DB_PASSWORD", "secret123");
    }

    @AfterEach
    void tearDown() {
        LOGGER.info("Cleaning up PropertyResolutionIntegrationTest");
        
        // Clean up test properties
        System.clearProperty("TEST_DB_HOST");
        System.clearProperty("TEST_DB_PORT");
        System.clearProperty("TEST_DB_NAME");
        System.clearProperty("TEST_DB_USER");
        System.clearProperty("TEST_DB_PASSWORD");
    }

    @Test
    @DisplayName("Should resolve properties when loading from file")
    void testPropertyResolutionFromFile() throws Exception {
        LOGGER.info("TEST: Property resolution from file");
        
        // Create test YAML file with property placeholders
        String yamlContent = """
            metadata:
              name: "Test Configuration with Properties"
              version: "1.0.0"
              description: "Test configuration with property placeholders"

            data-sources:
              - name: "test-database"
                type: "database"
                sourceType: "postgresql"
                enabled: true
                description: "Test database with resolved properties"
                
                connection:
                  host: "${TEST_DB_HOST}"
                  port: ${TEST_DB_PORT}
                  database: "${TEST_DB_NAME}"
                  username: "${TEST_DB_USER}"
                  password: "${TEST_DB_PASSWORD}"
                  schema: "public"
            """;
        
        Path testFile = tempDir.resolve("test-config.yaml");
        Files.writeString(testFile, yamlContent);
        
        // Load configuration and verify properties are resolved
        YamlRuleConfiguration config = loader.loadFromFile(testFile.toString());
        
        assertNotNull(config);
        assertNotNull(config.getMetadata());
        assertEquals("Test Configuration with Properties", config.getMetadata().getName());
        
        assertNotNull(config.getDataSources());
        assertEquals(1, config.getDataSources().size());
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        assertEquals("test-database", dataSource.getName());
        
        // Verify properties were resolved in connection configuration
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        assertNotNull(dsConfig.getConnection());
        
        assertEquals("test-host", dsConfig.getConnection().getHost());
        assertEquals(Integer.valueOf(5432), dsConfig.getConnection().getPort());
        assertEquals("test_database", dsConfig.getConnection().getDatabase());
        assertEquals("test_user", dsConfig.getConnection().getUsername());
        assertEquals("secret123", dsConfig.getConnection().getPassword());
        
        LOGGER.info("✓ Property resolution from file works correctly");
    }

    @Test
    @DisplayName("Should resolve properties when loading from stream")
    void testPropertyResolutionFromStream() throws Exception {
        LOGGER.info("TEST: Property resolution from stream");
        
        String yamlContent = """
            metadata:
              name: "Stream Test Configuration"
              version: "1.0.0"

            data-sources:
              - name: "stream-database"
                type: "database"
                sourceType: "postgresql"
                connection:
                  host: "${TEST_DB_HOST}"
                  database: "${TEST_DB_NAME:default_db}"
                  username: "${TEST_DB_USER}"
            """;
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
        
        YamlRuleConfiguration config = loader.loadFromStream(inputStream);
        
        assertNotNull(config);
        assertEquals("Stream Test Configuration", config.getMetadata().getName());
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        
        assertEquals("test-host", dsConfig.getConnection().getHost());
        assertEquals("test_database", dsConfig.getConnection().getDatabase());
        assertEquals("test_user", dsConfig.getConnection().getUsername());
        
        LOGGER.info("✓ Property resolution from stream works correctly");
    }

    @Test
    @DisplayName("Should resolve properties when parsing YAML string")
    void testPropertyResolutionFromString() throws Exception {
        LOGGER.info("TEST: Property resolution from YAML string");
        
        String yamlString = """
            metadata:
              name: "String Test Configuration"
              version: "1.0.0"

            data-sources:
              - name: "string-database"
                type: "database"
                sourceType: "h2"
                connection:
                  database: "${TEST_DB_NAME}"
                  username: "${TEST_DB_USER}"
                  password: "${MISSING_PASSWORD:default_password}"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlString);
        
        assertNotNull(config);
        assertEquals("String Test Configuration", config.getMetadata().getName());
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        
        assertEquals("test_database", dsConfig.getConnection().getDatabase());
        assertEquals("test_user", dsConfig.getConnection().getUsername());
        assertEquals("default_password", dsConfig.getConnection().getPassword()); // Default value used
        
        LOGGER.info("✓ Property resolution from YAML string works correctly");
    }

    @Test
    @DisplayName("Should handle YAML without property placeholders")
    void testYamlWithoutPlaceholders() throws Exception {
        LOGGER.info("TEST: YAML without property placeholders");
        
        String yamlContent = """
            metadata:
              name: "No Placeholders Configuration"
              version: "1.0.0"

            data-sources:
              - name: "static-database"
                type: "database"
                sourceType: "h2"
                connection:
                  database: "static_db"
                  username: "static_user"
                  password: "static_password"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);
        
        assertNotNull(config);
        assertEquals("No Placeholders Configuration", config.getMetadata().getName());
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        
        assertEquals("static_db", dsConfig.getConnection().getDatabase());
        assertEquals("static_user", dsConfig.getConnection().getUsername());
        assertEquals("static_password", dsConfig.getConnection().getPassword());
        
        LOGGER.info("✓ YAML without placeholders works correctly");
    }

    @Test
    @DisplayName("Should throw exception for missing required properties")
    void testMissingRequiredProperties() throws Exception {
        LOGGER.info("TEST: Missing required properties");
        
        String yamlContent = """
            metadata:
              name: "Missing Property Test"
              version: "1.0.0"

            data-sources:
              - name: "missing-prop-database"
                type: "database"
                connection:
                  host: "${DEFINITELY_MISSING_PROPERTY}"
            """;
        
        Exception exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.fromYamlString(yamlContent);
        });
        
        assertTrue(exception.getMessage().contains("Property not found: DEFINITELY_MISSING_PROPERTY"));
        
        LOGGER.info("✓ Missing required properties throw correct exception: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should resolve complex property patterns")
    void testComplexPropertyPatterns() throws Exception {
        LOGGER.info("TEST: Complex property patterns");
        
        String yamlContent = """
            metadata:
              name: "Complex Properties Test"
              version: "1.0.0"

            data-sources:
              - name: "complex-database"
                type: "database"
                connection:
                  # Multiple properties in one value
                  host: "${TEST_DB_HOST}:${TEST_DB_PORT}"
                  # Property with default
                  database: "${MISSING_DB:fallback_database}"
                  # Nested in longer string
                  username: "user_${TEST_DB_USER}_suffix"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        
        assertEquals("test-host:5432", dsConfig.getConnection().getHost());
        assertEquals("fallback_database", dsConfig.getConnection().getDatabase());
        assertEquals("user_test_user_suffix", dsConfig.getConnection().getUsername());
        
        LOGGER.info("✓ Complex property patterns work correctly");
    }
}
