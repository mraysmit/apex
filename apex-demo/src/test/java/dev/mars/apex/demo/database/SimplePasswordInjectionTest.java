package dev.mars.apex.demo.database;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to debug $(PASSWD) password injection functionality.
 * 
 * This test focuses on verifying that the $(PASSWD) syntax is properly
 * resolved during YAML configuration loading, without database connectivity.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Simple Password Injection Tests")
class SimplePasswordInjectionTest {

    private static final Logger logger = LoggerFactory.getLogger(SimplePasswordInjectionTest.class);
    
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        logger.info("Setting up SimplePasswordInjectionTest");
        
        loader = new YamlConfigurationLoader();
        
        // Set up system properties for password injection
        System.setProperty("TEST_PASSWORD", "secret123");
        System.setProperty("TEST_USER", "testuser");
        System.setProperty("TEST_DATABASE", "testdb");
        System.setProperty("PASSWD", "secret123");  // Add missing PASSWD property
    }

    @AfterEach
    void tearDown() {
        logger.info("Cleaning up SimplePasswordInjectionTest");
        
        // Clean up properties
        System.clearProperty("TEST_PASSWORD");
        System.clearProperty("TEST_USER");
        System.clearProperty("TEST_DATABASE");
        System.clearProperty("PASSWD");
    }

    @Test
    @DisplayName("Should resolve $(PASSWD) syntax in YAML configuration")
    void testPasswordInjectionInYaml() throws Exception {
        logger.info("TEST: $(PASSWD) syntax resolution in YAML");
        
        // Create YAML configuration with $(PASSWD) syntax
        String yamlContent = """
            metadata:
              name: "Password Injection Test"
              version: "1.0.0"
              description: "Test $(PASSWD) syntax resolution"

            data-sources:
              - name: "test-database"
                type: "database"
                sourceType: "h2"
                enabled: true
                description: "Test database with password injection"
                
                connection:
                  database: "$(TEST_DATABASE)"
                  username: "$(TEST_USER)"
                  password: "$(TEST_PASSWORD)"
            """;
        
        logger.info("Input YAML contains: $(TEST_DATABASE), $(TEST_USER), $(TEST_PASSWORD)");
        
        // Step 1: Load YAML configuration
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);
        
        assertNotNull(config);
        assertEquals("Password Injection Test", config.getMetadata().getName());
        assertEquals(1, config.getDataSources().size());
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        assertEquals("test-database", dataSource.getName());
        
        logger.info("✓ YAML configuration loaded successfully");
        
        // Step 2: Verify password injection in configuration
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        assertNotNull(dsConfig.getConnection());
        
        String actualDatabase = dsConfig.getConnection().getDatabase();
        String actualUsername = dsConfig.getConnection().getUsername();
        String actualPassword = dsConfig.getConnection().getPassword();
        
        logger.info("Resolved values:");
        logger.info("  Database: {} (expected: testdb)", actualDatabase);
        logger.info("  Username: {} (expected: testuser)", actualUsername);
        logger.info("  Password: [MASKED] (expected: secret123)");
        
        // Verify property resolution worked
        assertEquals("testdb", actualDatabase, "Database property should be resolved");
        assertEquals("testuser", actualUsername, "Username property should be resolved");
        assertEquals("secret123", actualPassword, "Password property should be resolved");
        
        logger.info("✓ $(PASSWD) syntax resolution test completed successfully");
    }

    @Test
    @DisplayName("Should resolve mixed ${} and $() syntax in YAML configuration")
    void testMixedSyntaxInYaml() throws Exception {
        logger.info("TEST: Mixed ${} and $() syntax resolution in YAML");
        
        // Set up additional properties for mixed syntax test
        System.setProperty("CURLY_PROP", "curly_value");
        System.setProperty("PAREN_PROP", "paren_value");
        
        try {
            String yamlContent = """
                metadata:
                  name: "Mixed Syntax Test"
                  version: "1.0.0"

                data-sources:
                  - name: "mixed-database"
                    type: "database"
                    sourceType: "h2"
                    connection:
                      database: "${CURLY_PROP}"
                      username: "$(PAREN_PROP)"
                      password: "$(TEST_PASSWORD)"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yamlContent);
            
            assertNotNull(config);
            YamlDataSource dataSource = config.getDataSources().get(0);
            DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
            
            assertEquals("curly_value", dsConfig.getConnection().getDatabase());
            assertEquals("paren_value", dsConfig.getConnection().getUsername());
            assertEquals("secret123", dsConfig.getConnection().getPassword());
            
            logger.info("✓ Mixed syntax resolution test completed successfully");
            
        } finally {
            System.clearProperty("CURLY_PROP");
            System.clearProperty("PAREN_PROP");
        }
    }

    @Test
    @DisplayName("Should handle $(PASSWD) with default values")
    void testPasswordInjectionWithDefaults() throws Exception {
        logger.info("TEST: $(PASSWD) syntax with default values");
        
        String yamlContent = """
            metadata:
              name: "Default Values Test"
              version: "1.0.0"

            data-sources:
              - name: "default-database"
                type: "database"
                sourceType: "h2"
                connection:
                  database: "$(MISSING_DB:default_db)"
                  username: "$(TEST_USER)"
                  password: "$(MISSING_PASSWORD:default_pass)"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);
        
        assertNotNull(config);
        YamlDataSource dataSource = config.getDataSources().get(0);
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        
        assertEquals("default_db", dsConfig.getConnection().getDatabase());
        assertEquals("testuser", dsConfig.getConnection().getUsername());
        assertEquals("default_pass", dsConfig.getConnection().getPassword());
        
        logger.info("✓ Default values test completed successfully");
    }
}
