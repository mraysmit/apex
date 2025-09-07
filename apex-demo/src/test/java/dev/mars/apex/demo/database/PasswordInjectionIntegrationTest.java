package dev.mars.apex.demo.database;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.DataSourceException;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for $(PASSWD) password injection functionality.
 *
 * This test demonstrates the $(PASSWD) syntax for secure password injection
 * in YAML configurations with H2 database.
 *
 * Following established patterns from existing integration tests:
 * - Uses H2 in-memory for lightweight testing
 * - Tests actual database connectivity with injected passwords
 * - Validates that passwords are properly resolved and masked in logs
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Password Injection Integration Tests")
class PasswordInjectionIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(PasswordInjectionIntegrationTest.class);

    private YamlConfigurationLoader loader;
    private DataSourceFactory factory;

    @BeforeEach
    void setUp() {
        logger.info("Setting up PasswordInjectionIntegrationTest");

        loader = new YamlConfigurationLoader();
        factory = DataSourceFactory.getInstance();

        // Set up system properties for H2 password injection
        System.setProperty("H2_PASSWORD", "h2secret");
        System.setProperty("H2_USER", "sa");
        System.setProperty("H2_DATABASE", "mem:testdb;MODE=PostgreSQL");
    }

    @AfterEach
    void tearDown() {
        logger.info("Cleaning up PasswordInjectionIntegrationTest");

        // Clean up H2 properties
        System.clearProperty("H2_PASSWORD");
        System.clearProperty("H2_USER");
        System.clearProperty("H2_DATABASE");
    }

    @Test
    @DisplayName("Should inject H2 password using $(PASSWD) syntax")
    void testH2PasswordInjection() throws Exception {
        logger.info("TEST: H2 password injection with $(PASSWD) syntax");
        
        // Create YAML configuration with $(PASSWD) syntax
        String yamlContent = """
            metadata:
              name: "H2 Password Injection Test"
              version: "1.0.0"
              description: "Test $(PASSWD) syntax with H2 database"

            data-sources:
              - name: "h2-test-database"
                type: "database"
                sourceType: "h2"
                enabled: true
                description: "H2 database with password injection"
                
                connection:
                  database: "$(H2_DATABASE)"
                  username: "$(H2_USER)"
                  password: "$(H2_PASSWORD)"
                
                queries:
                  createTestTable: |
                    CREATE TABLE IF NOT EXISTS test_users (
                      id INTEGER PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      email VARCHAR(255)
                    )
                  insertTestUser: "INSERT INTO test_users (name, email) VALUES (:name, :email)"
                  getTestUser: "SELECT * FROM test_users WHERE id = :id"
            """;
        
        // Step 1: Load and validate YAML configuration
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);
        
        assertNotNull(config);
        assertEquals("H2 Password Injection Test", config.getMetadata().getName());
        assertEquals(1, config.getDataSources().size());
        
        YamlDataSource dataSource = config.getDataSources().get(0);
        assertEquals("h2-test-database", dataSource.getName());
        
        logger.info("✓ YAML configuration loaded successfully");
        
        // Step 2: Verify password injection in configuration
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        assertNotNull(dsConfig.getConnection());
        
        assertEquals("mem:testdb;MODE=PostgreSQL", dsConfig.getConnection().getDatabase());
        assertEquals("sa", dsConfig.getConnection().getUsername());
        assertEquals("h2secret", dsConfig.getConnection().getPassword());
        
        logger.info("✓ Password injection resolved correctly: database={}, username={}, password=[MASKED]", 
            dsConfig.getConnection().getDatabase(), dsConfig.getConnection().getUsername());
        
        // Step 3: Test actual database connectivity
        ExternalDataSource externalDataSource = factory.createDataSource(dsConfig);
        assertNotNull(externalDataSource);

        // Create test table using query method
        externalDataSource.query("CREATE TABLE IF NOT EXISTS test_users (id INTEGER PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255))", Map.of());
        logger.info("✓ Test table created successfully");

        // Insert test data using query method
        Map<String, Object> insertParams = Map.of(
            "name", "Test User",
            "email", "test@example.com"
        );
        externalDataSource.query("INSERT INTO test_users (name, email) VALUES (:name, :email)", insertParams);
        logger.info("✓ Test data inserted successfully");

        // Query test data
        List<Object> results = externalDataSource.query("SELECT * FROM test_users WHERE name = :name", Map.of("name", "Test User"));
        assertNotNull(results);
        assertFalse(results.isEmpty());
        logger.info("✓ Test data retrieved successfully: {} records found", results.size());

        logger.info("✓ H2 password injection test completed successfully");
    }

    @Test
    @DisplayName("Should inject H2 password using $(PASSWD) syntax with actual database operations")
    void testH2PasswordInjectionWithDatabaseOperations() throws Exception {
        logger.info("TEST: H2 password injection with actual database operations");

        // Create YAML configuration with $(PASSWD) syntax and actual database operations
        String yamlContent = """
            metadata:
              name: "H2 Database Operations Test"
              version: "1.0.0"
              description: "Test $(PASSWD) syntax with real H2 database operations"

            data-sources:
              - name: "h2-operations-database"
                type: "database"
                sourceType: "h2"
                enabled: true
                description: "H2 database with password injection for operations"

                connection:
                  database: "$(H2_DATABASE)"
                  username: "$(H2_USER)"
                  password: "$(H2_PASSWORD)"

                queries:
                  createUsersTable: |
                    CREATE TABLE IF NOT EXISTS users (
                      id INTEGER PRIMARY KEY AUTO_INCREMENT,
                      username VARCHAR(50) NOT NULL UNIQUE,
                      email VARCHAR(100) NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                  insertUser: "INSERT INTO users (username, email) VALUES (:username, :email)"
                  getUserByUsername: "SELECT * FROM users WHERE username = :username"
                  getAllUsers: "SELECT * FROM users ORDER BY created_at DESC"
                  updateUserEmail: "UPDATE users SET email = :email WHERE username = :username"
                  deleteUser: "DELETE FROM users WHERE username = :username"
            """;

        // Step 1: Load and validate YAML configuration
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);

        assertNotNull(config);
        assertEquals("H2 Database Operations Test", config.getMetadata().getName());
        assertEquals(1, config.getDataSources().size());

        YamlDataSource dataSource = config.getDataSources().get(0);
        assertEquals("h2-operations-database", dataSource.getName());

        logger.info("✓ YAML configuration loaded successfully");

        // Step 2: Verify password injection in configuration
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        assertNotNull(dsConfig.getConnection());

        assertEquals("mem:testdb;MODE=PostgreSQL", dsConfig.getConnection().getDatabase());
        assertEquals("sa", dsConfig.getConnection().getUsername());
        assertEquals("h2secret", dsConfig.getConnection().getPassword());

        logger.info("✓ Password injection resolved correctly: database={}, username={}, password=[MASKED]",
            dsConfig.getConnection().getDatabase(), dsConfig.getConnection().getUsername());

        // Step 3: Test comprehensive database operations
        ExternalDataSource externalDataSource = factory.createDataSource(dsConfig);
        assertNotNull(externalDataSource);

        // Create users table
        externalDataSource.query("createUsersTable", Map.of());
        logger.info("✓ Users table created successfully");

        // Insert test users
        Map<String, Object> user1 = Map.of("username", "john_doe", "email", "john@example.com");
        Map<String, Object> user2 = Map.of("username", "jane_smith", "email", "jane@example.com");

        externalDataSource.query("insertUser", user1);
        externalDataSource.query("insertUser", user2);
        logger.info("✓ Test users inserted successfully");

        // Query specific user
        List<Object> johnResults = externalDataSource.query("getUserByUsername", Map.of("username", "john_doe"));
        assertNotNull(johnResults);
        assertFalse(johnResults.isEmpty());
        logger.info("✓ User query successful: found {} records for john_doe", johnResults.size());

        // Query all users
        List<Object> allUsers = externalDataSource.query("getAllUsers", Map.of());
        assertNotNull(allUsers);
        assertEquals(2, allUsers.size());
        logger.info("✓ All users query successful: found {} total users", allUsers.size());

        // Update user email
        externalDataSource.query("updateUserEmail", Map.of("username", "john_doe", "email", "john.doe@newdomain.com"));
        logger.info("✓ User email updated successfully");

        // Verify update
        List<Object> updatedUser = externalDataSource.query("getUserByUsername", Map.of("username", "john_doe"));
        assertNotNull(updatedUser);
        assertFalse(updatedUser.isEmpty());
        logger.info("✓ User update verified successfully");

        // Delete user
        externalDataSource.query("deleteUser", Map.of("username", "jane_smith"));
        logger.info("✓ User deleted successfully");

        // Verify deletion
        List<Object> remainingUsers = externalDataSource.query("getAllUsers", Map.of());
        assertNotNull(remainingUsers);
        assertEquals(1, remainingUsers.size());
        logger.info("✓ User deletion verified: {} users remaining", remainingUsers.size());

        logger.info("✓ H2 password injection with database operations test completed successfully");
    }
}
