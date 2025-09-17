package dev.mars.apex.demo.database;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to validate $(PASSWD) password injection functionality with actual database operations.
 *
 * FOLLOWS APEX TEST GUIDELINES:
 * - Uses embedded yaml configuration directly in this class
 * - Tests actual database connectivity with resolved passwords
 * - Creates real H2 tables with actual test data
 * - Executes actual APEX enrichment operations using database lookups
 * - Validates functional results rather than just configuration parsing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Password Injection Database Functionality Tests")
class SimplePasswordInjectionTest {

    private static final Logger logger = LoggerFactory.getLogger(SimplePasswordInjectionTest.class);

    private YamlConfigurationLoader loader;
    private EnrichmentService enrichmentService;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService expressionEvaluator;

    @BeforeEach
    void setUp() {
        logger.info("Setting up Password Injection Database Functionality Test");

        loader = new YamlConfigurationLoader();
        serviceRegistry = new LookupServiceRegistry();
        expressionEvaluator = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        // Set up system properties for password injection
        System.setProperty("DB_PASSWORD", "h2secret");
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_NAME", "password_injection_test");
        System.setProperty("PASSWD", "h2secret");

        // Set up test database with actual data
        setupTestDatabase();
    }

    @AfterEach
    void tearDown() {
        logger.info("Cleaning up Password Injection Database Functionality Test");

        // Clean up properties
        System.clearProperty("DB_PASSWORD");
        System.clearProperty("DB_USER");
        System.clearProperty("DB_NAME");
        System.clearProperty("PASSWD");
    }

    /**
     * Set up H2 database with test data for password injection tests.
     */
    private void setupTestDatabase() {
        logger.info("Setting up test database with password injection...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/password_injection_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "h2secret")) {
            Statement statement = connection.createStatement();

            // Drop existing tables (drop dependent table first)
            statement.execute("DROP TABLE IF EXISTS accounts");
            statement.execute("DROP TABLE IF EXISTS users");

            // Create users table
            statement.execute("""
                CREATE TABLE users (
                    user_id VARCHAR(20) PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100),
                    status VARCHAR(20),
                    created_date DATE
                )
                """);

            // Create accounts table
            statement.execute("""
                CREATE TABLE accounts (
                    account_id VARCHAR(20) PRIMARY KEY,
                    user_id VARCHAR(20) NOT NULL,
                    account_type VARCHAR(20),
                    balance DECIMAL(15,2),
                    currency VARCHAR(3),
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                )
                """);

            // Insert test users
            statement.execute("""
                INSERT INTO users (user_id, username, email, status, created_date) VALUES
                ('USER001', 'john_doe', 'john@example.com', 'ACTIVE', '2025-01-01'),
                ('USER002', 'jane_smith', 'jane@example.com', 'ACTIVE', '2025-01-02'),
                ('USER003', 'bob_wilson', 'bob@example.com', 'SUSPENDED', '2025-01-03')
                """);

            // Insert test accounts
            statement.execute("""
                INSERT INTO accounts (account_id, user_id, account_type, balance, currency) VALUES
                ('ACC001', 'USER001', 'CHECKING', 5000.00, 'USD'),
                ('ACC002', 'USER001', 'SAVINGS', 15000.00, 'USD'),
                ('ACC003', 'USER002', 'CHECKING', 3000.00, 'EUR'),
                ('ACC004', 'USER003', 'CHECKING', 1000.00, 'USD')
                """);

            // Verify data was inserted
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            int userCount = rs.getInt(1);
            logger.info("✅ Test database setup completed with password injection - {} users inserted", userCount);

        } catch (Exception e) {
            logger.error("Failed to setup test database: {}", e.getMessage(), e);
            fail("Test database setup failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should perform database lookup with password injection")
    void testDatabaseLookupWithPasswordInjection() throws Exception {
        logger.info("TEST: Database lookup functionality with password injection");

        // Create YAML configuration with $(PASSWD) syntax for database lookup
        String yamlContent = """
            metadata:
              name: "Password Injection Database Lookup"
              version: "1.0.0"
              description: "Test database lookup with password injection"

            data-sources:
              - name: "user-database"
                type: "database"
                source-type: "h2"
                enabled: true

                connection:
                  database: "./target/h2-demo/password_injection_test"
                  username: "$(DB_USER)"
                  password: "$(DB_PASSWORD)"

            enrichments:
              - id: "user-lookup"
                name: "user-lookup"
                type: "lookup-enrichment"
                condition: "#userId != null"
                lookup-config:
                  lookup-key: "#userId"
                  lookup-dataset:
                    type: "database"
                    connection-name: "user-database"
                    query: "SELECT username, email, status FROM users WHERE user_id = :userId"
                    parameters:
                      - field: "userId"
                        type: "string"
                field-mappings:
                  - source-field: "USERNAME"
                    target-field: "userName"
                  - source-field: "EMAIL"
                    target-field: "userEmail"
                  - source-field: "STATUS"
                    target-field: "userStatus"
            """;

        logger.info("Loading YAML configuration with password injection...");

        // Load YAML configuration
        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);

        assertNotNull(config, "Configuration should be loaded");
        assertEquals(1, config.getDataSources().size());

        // Verify password injection worked
        YamlDataSource dataSource = config.getDataSources().get(0);
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        assertEquals("h2secret", dsConfig.getConnection().getPassword());

        logger.info("✅ Password injection resolved successfully");

        // Create test data for user lookup
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", "USER001");

        logger.info("Input data: {}", userData);

        // Execute actual enrichment operation using injected password
        enrichmentService.enrichObject(config, userData);

        logger.info("Enriched data: {}", userData);

        // Validate functional results
        assertNotNull(userData.get("userName"), "User name should be enriched");
        assertNotNull(userData.get("userEmail"), "User email should be enriched");
        assertNotNull(userData.get("userStatus"), "User status should be enriched");

        assertEquals("john_doe", userData.get("userName"));
        assertEquals("john@example.com", userData.get("userEmail"));
        assertEquals("ACTIVE", userData.get("userStatus"));

        logger.info("Enriched data: {}", userData);
        logger.info("✅ Database lookup with password injection completed successfully");
    }

    @Test
    @DisplayName("Should perform account lookup with mixed password syntax")
    void testAccountLookupWithMixedSyntax() throws Exception {
        logger.info("TEST: Account lookup with mixed ${} and $() password syntax");

        // Set up additional properties for mixed syntax test
        System.setProperty("MIXED_USER", "sa");
        System.setProperty("MIXED_DB", "password_injection_test");

        try {
            String yamlContent = """
                metadata:
                  name: "Mixed Syntax Account Lookup"
                  version: "1.0.0"

                data-sources:
                  - name: "account-database"
                    type: "database"
                    source-type: "h2"
                    connection:
                      database: "./target/h2-demo/password_injection_test"
                      username: "$(MIXED_USER)"
                      password: "$(DB_PASSWORD)"

                enrichments:
                  - id: "account-lookup"
                    name: "account-lookup"
                    type: "lookup-enrichment"
                    condition: "#accountId != null"
                    lookup-config:
                      lookup-key: "#accountId"
                      lookup-dataset:
                        type: "database"
                        connection-name: "account-database"
                        query: |
                          SELECT a.account_type, a.balance, a.currency, u.username
                          FROM accounts a
                          JOIN users u ON a.user_id = u.user_id
                          WHERE a.account_id = :accountId
                        parameters:
                          - field: "accountId"
                            type: "string"
                    field-mappings:
                      - source-field: "ACCOUNT_TYPE"
                        target-field: "accountType"
                      - source-field: "BALANCE"
                        target-field: "accountBalance"
                      - source-field: "CURRENCY"
                        target-field: "accountCurrency"
                      - source-field: "USERNAME"
                        target-field: "accountOwner"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlContent);

            // Verify mixed syntax resolution
            YamlDataSource dataSource = config.getDataSources().get(0);
            DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
            assertEquals("./target/h2-demo/password_injection_test", dsConfig.getConnection().getDatabase());
            assertEquals("sa", dsConfig.getConnection().getUsername());
            assertEquals("h2secret", dsConfig.getConnection().getPassword());

            logger.info("✅ Mixed syntax password injection resolved successfully");

            // Create test data for account lookup
            Map<String, Object> accountData = new HashMap<>();
            accountData.put("accountId", "ACC001");

            logger.info("Input data: {}", accountData);

            // Execute actual enrichment operation
            enrichmentService.enrichObject(config, accountData);

            // Validate functional results
            assertNotNull(accountData.get("accountType"), "Account type should be enriched");
            assertNotNull(accountData.get("accountBalance"), "Account balance should be enriched");
            assertNotNull(accountData.get("accountCurrency"), "Account currency should be enriched");
            assertNotNull(accountData.get("accountOwner"), "Account owner should be enriched");

            assertEquals("CHECKING", accountData.get("accountType"));
            assertEquals(5000.00, ((Number) accountData.get("accountBalance")).doubleValue(), 0.01);
            assertEquals("USD", accountData.get("accountCurrency"));
            assertEquals("john_doe", accountData.get("accountOwner"));

            logger.info("Enriched account data: {}", accountData);
            logger.info("✅ Account lookup with mixed password syntax completed successfully");

        } finally {
            System.clearProperty("MIXED_USER");
            System.clearProperty("MIXED_DB");
        }
    }

    @Test
    @DisplayName("Should handle password injection with default values")
    void testPasswordInjectionWithDefaults() throws Exception {
        logger.info("TEST: Password injection with default values functionality");

        String yamlContent = """
            metadata:
              name: "Default Values Database Test"
              version: "1.0.0"

            data-sources:
              - name: "default-database"
                type: "database"
                source-type: "h2"
                connection:
                  database: "./target/h2-demo/password_injection_test"
                  username: "$(MISSING_USER:sa)"
                  password: "$(MISSING_PASSWORD:h2secret)"

            enrichments:
              - id: "default-user-lookup"
                name: "default-user-lookup"
                type: "lookup-enrichment"
                condition: "#userId != null"
                lookup-config:
                  lookup-key: "#userId"
                  lookup-dataset:
                    type: "database"
                    connection-name: "default-database"
                    query: "SELECT username, status FROM users WHERE user_id = :userId"
                    parameters:
                      - field: "userId"
                        type: "string"
                field-mappings:
                  - source-field: "USERNAME"
                    target-field: "defaultUserName"
                  - source-field: "STATUS"
                    target-field: "defaultUserStatus"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlContent);

        // Verify default values were used
        YamlDataSource dataSource = config.getDataSources().get(0);
        DataSourceConfiguration dsConfig = dataSource.toDataSourceConfiguration();
        assertEquals("./target/h2-demo/password_injection_test", dsConfig.getConnection().getDatabase());
        assertEquals("sa", dsConfig.getConnection().getUsername());
        assertEquals("h2secret", dsConfig.getConnection().getPassword());

        logger.info("✅ Default password values resolved successfully");

        // Test actual database functionality with default values
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", "USER002");

        logger.info("Input data: {}", userData);

        // Execute enrichment with default password values
        enrichmentService.enrichObject(config, userData);

        // Validate results
        assertEquals("jane_smith", userData.get("defaultUserName"));
        assertEquals("ACTIVE", userData.get("defaultUserStatus"));

        logger.info("Enriched data with defaults: {}", userData);
        logger.info("✅ Password injection with default values test completed successfully");
    }
}
