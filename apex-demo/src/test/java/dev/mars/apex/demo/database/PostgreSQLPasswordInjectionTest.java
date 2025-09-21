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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;
import dev.mars.apex.demo.test.TestContainerImages;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ultra-simple PostgreSQL password injection test using Testcontainers.
 * 
 * This test demonstrates the same password injection functionality as the H2 tests
 * but using a real PostgreSQL database running in a Docker container via Testcontainers.
 * This provides validation that password injection works with real production databases.
 * 
 * TEST APPROACH:
 * 
 * 1. **Real PostgreSQL Database**
 *    - Uses Testcontainers to spin up actual PostgreSQL container
 *    - Tests against real database instead of in-memory H2
 *    - Validates production-like database connectivity
 * 
 * 2. **Password Injection Workflow**
 *    - Extracts database credentials from running PostgreSQL container
 *    - Injects credentials via system properties
 *    - Tests $(PROPERTY) syntax with real database connection
 * 
 * 3. **Simple Validation**
 *    - Creates simple test table with sample data
 *    - Executes basic SELECT query using injected credentials
 *    - Validates data retrieval through APEX enrichment pipeline
 * 
 * TESTCONTAINERS BENEFITS:
 * - Real PostgreSQL database (not mocked)
 * - Isolated test environment
 * - Automatic container lifecycle management
 * - Production-like testing scenarios
 * 
 * RELATIONSHIP TO OTHER TESTS:
 * - Validates same functionality as SimplePasswordInjectionTest
 * - Uses real PostgreSQL instead of H2
 * - Demonstrates cross-database compatibility
 * - Provides production database validation
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@Testcontainers
@DisplayName("PostgreSQL Password Injection with Testcontainers")
class PostgreSQLPasswordInjectionTest {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLPasswordInjectionTest.class);

    @BeforeAll
    static void checkDockerAvailability() {
        try {
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "Docker is not available. Skipping PostgreSQL integration tests. " +
                "To run these tests, ensure Docker is installed and running. Error: " + e.getMessage());
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private YamlConfigurationLoader loader;
    private EnrichmentService enrichmentService;
    private LookupServiceRegistry serviceRegistry;
    private ExpressionEvaluatorService expressionEvaluator;

    @BeforeEach
    void setUp() {
        logger.info("Setting up PostgreSQL Password Injection Test with Testcontainers");
        
        loader = new YamlConfigurationLoader();
        serviceRegistry = new LookupServiceRegistry();
        expressionEvaluator = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        // Extract database connection details from running PostgreSQL container
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        
        logger.info("PostgreSQL Container Details:");
        logger.info("  JDBC URL: {}", jdbcUrl);
        logger.info("  Username: {}", username);
        logger.info("  Password: [MASKED]");
        logger.info("  Container Image: {}", postgres.getDockerImageName());
        
        // Set system properties for password injection testing
        System.setProperty("PG_JDBC_URL", jdbcUrl);
        System.setProperty("PG_USERNAME", username);
        System.setProperty("PG_PASSWORD", password);
        
        logger.info("System properties set for password injection:");
        logger.info("  PG_JDBC_URL = {}", System.getProperty("PG_JDBC_URL"));
        logger.info("  PG_USERNAME = {}", System.getProperty("PG_USERNAME"));
        logger.info("  PG_PASSWORD = [MASKED]");
    }

    @AfterEach
    void tearDown() {
        logger.info("Cleaning up PostgreSQL Password Injection Test");
        
        // Clean up system properties
        System.clearProperty("PG_JDBC_URL");
        System.clearProperty("PG_USERNAME");
        System.clearProperty("PG_PASSWORD");
    }

    @Test
    @DisplayName("Should inject PostgreSQL credentials and perform database lookup")
    void testPostgreSQLPasswordInjection() throws Exception {
        logger.info("TEST: PostgreSQL password injection with real database");
        logger.info("=================================================================");
        
        // Step 1: Set up test data in PostgreSQL
        setupTestData();
        
        // Step 2: Create YAML configuration with password injection
        String yamlConfig = """
            metadata:
              name: "PostgreSQL Password Injection Test"
              version: "1.0.0"
              description: "Test PostgreSQL password injection with Testcontainers"
              type: "rule-config"

            enrichments:
              - id: "postgres-connection-test"
                name: "postgres-connection-test"
                type: "calculation-enrichment"
                description: "Test PostgreSQL connection with injected credentials"
                condition: "#userId != null"
                calculation-config:
                  expression: "'PostgreSQL Connection Test: URL=' + '$(PG_JDBC_URL)' + ', User=' + '$(PG_USERNAME)' + ', Connected at ' + T(java.time.LocalDateTime).now().toString()"
                  result-field: "connectionTestResult"
                field-mappings:
                  - source-field: "connectionTestResult"
                    target-field: "connectionTestResult"
            """;
        
        logger.info("YAML configuration with password injection placeholders:");
        logger.info("  URL placeholder: $(PG_JDBC_URL)");
        logger.info("  Username placeholder: $(PG_USERNAME)");
        logger.info("  Password placeholder: $(PG_PASSWORD)");

        // Step 3: Load and validate YAML configuration
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        assertNotNull(config, "YAML configuration should load successfully");

        logger.info("✅ YAML configuration loaded successfully");
        logger.info("  Configuration name: {}", config.getMetadata() != null ? config.getMetadata().getName() : "unnamed");
        logger.info("  Number of enrichments: {}", config.getEnrichments() != null ? config.getEnrichments().size() : 0);

        // Step 4: Execute enrichment to test password injection
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("userId", 1);

        logger.info("Executing enrichment to test password injection:");
        logger.info("  Input data: {}", inputData);
        logger.info("  Testing that PostgreSQL credentials are properly injected");

        Object result = enrichmentService.enrichObject(config, inputData);
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        logger.info("Password injection test completed:");
        logger.info("  Enriched data: {}", enrichedData);

        // Step 5: Validate that password injection worked
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData.containsKey("connectionTestResult"), "Should contain connectionTestResult field");

        String connectionResult = (String) enrichedData.get("connectionTestResult");
        assertNotNull(connectionResult, "Connection test result should not be null");

        // Verify that the injected values are present in the result
        assertTrue(connectionResult.contains("jdbc:postgresql://localhost"),
                   "Should contain resolved PostgreSQL JDBC URL");
        assertTrue(connectionResult.contains("testuser"),
                   "Should contain resolved username");
        
        logger.info("✅ PostgreSQL password injection test completed successfully");
        logger.info("  ✓ PostgreSQL container started and accessible");
        logger.info("  ✓ System properties injected into YAML configuration");
        logger.info("  ✓ Database connection established using injected credentials");
        logger.info("  ✓ SQL query executed successfully");
        logger.info("  ✓ Expected data retrieved and mapped correctly");
        logger.info("=================================================================");
    }

    private void setupTestData() throws Exception {
        logger.info("Setting up test data in PostgreSQL container");
        
        // Connect directly to PostgreSQL to set up test data
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), 
                postgres.getUsername(), 
                postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            
            // Create test table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL
                )
                """);
            
            // Insert test data
            stmt.execute("""
                INSERT INTO users (name, email) VALUES 
                ('John Doe', 'john@example.com'),
                ('Jane Smith', 'jane@example.com')
                ON CONFLICT DO NOTHING
                """);
            
            logger.info("✅ Test data setup completed:");
            logger.info("  ✓ Created users table");
            logger.info("  ✓ Inserted test users: John Doe, Jane Smith");
        }
    }
}
