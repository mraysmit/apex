package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for property resolution feature.
 * 
 * This test demonstrates the complete property resolution functionality
 * working with a real YAML configuration file.
 */
public class PropertyResolutionEndToEndTest {

    private static final Logger LOGGER = Logger.getLogger(PropertyResolutionEndToEndTest.class.getName());
    
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        LOGGER.info("Setting up PropertyResolutionEndToEndTest");
        
        loader = new YamlConfigurationLoader();
        
        // Set up test environment variables and system properties
        System.setProperty("DB_HOST", "production-db.example.com");
        System.setProperty("DB_PORT", "5432");
        System.setProperty("DB_NAME", "production_users");
        System.setProperty("DB_USER", "prod_user");
        System.setProperty("DB_PASSWORD", "super_secret_password");
        System.setProperty("TEST_DB_PATH", "./target/test/custom_test_db");
        System.setProperty("TEST_DB_USER", "test_admin");
    }

    @AfterEach
    void tearDown() {
        LOGGER.info("Cleaning up PropertyResolutionEndToEndTest");
        
        // Clean up test properties
        System.clearProperty("DB_HOST");
        System.clearProperty("DB_PORT");
        System.clearProperty("DB_NAME");
        System.clearProperty("DB_USER");
        System.clearProperty("DB_PASSWORD");
        System.clearProperty("TEST_DB_PATH");
        System.clearProperty("TEST_DB_USER");
    }

    @Test
    @DisplayName("Should resolve all properties in complete YAML configuration")
    void testCompletePropertyResolution() throws Exception {
        LOGGER.info("TEST: Complete property resolution end-to-end");
        
        // Load the test configuration file with property placeholders
        YamlRuleConfiguration config = loader.loadFromClasspath("test-config-with-properties.yaml");
        
        // Verify configuration loaded successfully
        assertNotNull(config);
        assertNotNull(config.getMetadata());
        assertEquals("Test Configuration with Property Resolution", config.getMetadata().getName());
        
        // Verify data sources were loaded and properties resolved
        assertNotNull(config.getDataSources());
        assertEquals(2, config.getDataSources().size());
        
        // Test PostgreSQL database with resolved properties
        YamlDataSource userDatabase = config.getDataSources().stream()
            .filter(ds -> "user-database".equals(ds.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(userDatabase, "User database should be found");
        assertEquals("database", userDatabase.getType());
        assertEquals("postgresql", userDatabase.getSourceType());
        
        DataSourceConfiguration userDbConfig = userDatabase.toDataSourceConfiguration();
        assertNotNull(userDbConfig.getConnection());
        
        // Verify all properties were resolved correctly
        assertEquals("production-db.example.com", userDbConfig.getConnection().getHost());
        assertEquals(Integer.valueOf(5432), userDbConfig.getConnection().getPort());
        assertEquals("production_users", userDbConfig.getConnection().getDatabase());
        assertEquals("prod_user", userDbConfig.getConnection().getUsername());
        assertEquals("super_secret_password", userDbConfig.getConnection().getPassword());
        assertEquals("public", userDbConfig.getConnection().getSchema());
        
        LOGGER.info("✓ PostgreSQL database properties resolved correctly");
        
        // Test H2 database with mixed property resolution (some with defaults)
        YamlDataSource testDatabase = config.getDataSources().stream()
            .filter(ds -> "test-database".equals(ds.getName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(testDatabase, "Test database should be found");
        assertEquals("database", testDatabase.getType());
        assertEquals("h2", testDatabase.getSourceType());
        
        DataSourceConfiguration testDbConfig = testDatabase.toDataSourceConfiguration();
        assertNotNull(testDbConfig.getConnection());
        
        // Verify properties with defaults were resolved correctly
        assertEquals("./target/test/custom_test_db", testDbConfig.getConnection().getDatabase());
        assertEquals("test_admin", testDbConfig.getConnection().getUsername());
        assertEquals("", testDbConfig.getConnection().getPassword()); // Default empty password
        
        LOGGER.info("✓ H2 database properties with defaults resolved correctly");
        
        // Verify enrichments were loaded correctly
        assertNotNull(config.getEnrichments());
        assertEquals(1, config.getEnrichments().size());
        
        YamlEnrichment userLookup = config.getEnrichments().get(0);
        assertEquals("user-lookup", userLookup.getId());
        assertEquals("User Lookup Enrichment", userLookup.getName());
        
        LOGGER.info("✓ Enrichments loaded correctly");
        
        // Verify rules were loaded correctly
        assertNotNull(config.getRules());
        assertEquals(1, config.getRules().size());
        
        YamlRule userValidation = config.getRules().get(0);
        assertEquals("user-validation", userValidation.getId());
        assertEquals("User Validation Rule", userValidation.getName());
        
        LOGGER.info("✓ Rules loaded correctly");
        
        LOGGER.info("✓ Complete end-to-end property resolution test passed");
    }

    @Test
    @DisplayName("Should handle missing properties with defaults gracefully")
    void testDefaultValueHandling() throws Exception {
        LOGGER.info("TEST: Default value handling");
        
        // Clear some properties to test defaults
        System.clearProperty("DB_HOST");
        System.clearProperty("DB_PORT");
        System.clearProperty("TEST_DB_PATH");
        System.clearProperty("TEST_DB_USER");
        
        YamlRuleConfiguration config = loader.loadFromClasspath("test-config-with-properties.yaml");
        
        // Verify PostgreSQL database uses defaults where properties are missing
        YamlDataSource userDatabase = config.getDataSources().stream()
            .filter(ds -> "user-database".equals(ds.getName()))
            .findFirst()
            .orElse(null);
        
        DataSourceConfiguration userDbConfig = userDatabase.toDataSourceConfiguration();
        
        // These should use default values
        assertEquals("localhost", userDbConfig.getConnection().getHost()); // Default from ${DB_HOST:localhost}
        assertEquals(Integer.valueOf(5432), userDbConfig.getConnection().getPort()); // Default from ${DB_PORT:5432}
        
        // These should still be resolved from system properties
        assertEquals("production_users", userDbConfig.getConnection().getDatabase());
        assertEquals("prod_user", userDbConfig.getConnection().getUsername());
        assertEquals("super_secret_password", userDbConfig.getConnection().getPassword());
        
        // Test H2 database defaults
        YamlDataSource testDatabase = config.getDataSources().stream()
            .filter(ds -> "test-database".equals(ds.getName()))
            .findFirst()
            .orElse(null);
        
        DataSourceConfiguration testDbConfig = testDatabase.toDataSourceConfiguration();
        
        // These should use default values
        assertEquals("./target/test/testdb", testDbConfig.getConnection().getDatabase()); // Default
        assertEquals("sa", testDbConfig.getConnection().getUsername()); // Default
        
        LOGGER.info("✓ Default value handling works correctly");
    }

    @Test
    @DisplayName("Should fail gracefully for missing required properties")
    void testMissingRequiredProperties() throws Exception {
        LOGGER.info("TEST: Missing required properties handling");
        
        // Clear required properties (those without defaults)
        System.clearProperty("DB_NAME");
        System.clearProperty("DB_USER");
        System.clearProperty("DB_PASSWORD");
        
        // This should throw an exception because required properties are missing
        Exception exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromClasspath("test-config-with-properties.yaml");
        });
        
        // Verify the exception message indicates which property is missing
        String message = exception.getMessage();
        assertTrue(message.contains("Property not found"), 
            "Exception should indicate missing property: " + message);
        
        LOGGER.info("✓ Missing required properties handled correctly: " + message);
    }
}
