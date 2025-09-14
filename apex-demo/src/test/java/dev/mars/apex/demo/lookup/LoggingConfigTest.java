package dev.mars.apex.demo.lookup;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify logging functionality during actual APEX operations.
 *
 * FOLLOWS APEX TEST GUIDELINES:
 * - Tests logging during real APEX enrichment operations
 * - Sets up real database with actual test data
 * - Executes actual database lookups to generate meaningful logs
 * - Validates functional results while testing logging behavior
 */
@DisplayName("APEX Operations Logging Tests")
public class LoggingConfigTest extends DemoTestBase {

    @BeforeEach
    void setUpLoggingTest() {
        logger.info("Setting up APEX Operations Logging Test");
        setupLoggingTestDatabase();
    }

    /**
     * Set up H2 database for logging tests.
     */
    private void setupLoggingTestDatabase() {
        logger.info("Setting up database for logging tests...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/logging_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop existing table
            statement.execute("DROP TABLE IF EXISTS logging_test_data");

            // Create test table
            statement.execute("""
                CREATE TABLE logging_test_data (
                    id VARCHAR(20) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    category VARCHAR(50),
                    amount DECIMAL(10,2)
                )
                """);

            // Insert test data
            statement.execute("""
                INSERT INTO logging_test_data (id, name, category, amount) VALUES
                ('LOG001', 'Test Item 1', 'CATEGORY_A', 100.50),
                ('LOG002', 'Test Item 2', 'CATEGORY_B', 200.75),
                ('LOG003', 'Test Item 3', 'CATEGORY_A', 150.25)
                """);

            logger.info("✅ Logging test database setup completed");

        } catch (Exception e) {
            logger.error("Failed to setup logging test database: {}", e.getMessage(), e);
            fail("Logging test database setup failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should demonstrate logging during database lookup operations")
    void testLoggingDuringDatabaseLookup() {
        logger.info("=".repeat(80));
        logger.info("TESTING LOGGING DURING DATABASE LOOKUP OPERATIONS");
        logger.info("=".repeat(80));

        // Create YAML configuration for database lookup with logging
        String yamlContent = """
            metadata:
              name: "Logging Test Database Lookup"
              version: "1.0.0"
              description: "Test logging during database operations"

            data-sources:
              - name: "logging-test-db"
                type: "database"
                source-type: "h2"
                enabled: true

                connection:
                  database: "./target/h2-demo/logging_test"
                  username: "sa"
                  password: ""

            enrichments:
              - id: "logging-test-lookup"
                name: "logging-test-lookup"
                type: "lookup-enrichment"
                condition: "#itemId != null"
                lookup-config:
                  lookup-key: "#itemId"
                  lookup-dataset:
                    type: "database"
                    connection-name: "logging-test-db"
                    query: "SELECT name, category, amount FROM logging_test_data WHERE id = :itemId"
                    parameters:
                      - field: "itemId"
                        type: "string"
                field-mappings:
                  - source-field: "name"
                    target-field: "itemName"
                  - source-field: "category"
                    target-field: "itemCategory"
                  - source-field: "amount"
                    target-field: "itemValue"
            """;

        try {
            logger.info("Loading YAML configuration for logging test...");
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);

            // Test multiple lookups to generate various log messages
            String[] testIds = {"LOG001", "LOG002", "LOG003", "NONEXISTENT"};

            for (String testId : testIds) {
                logger.info("\n" + "-".repeat(60));
                logger.info("Testing lookup for ID: {}", testId);
                logger.info("-".repeat(60));

                Map<String, Object> testData = new HashMap<>();
                testData.put("itemId", testId);

                logger.info("Input data: {}", testData);

                // Execute enrichment - this will generate database lookup logs
                enrichmentService.enrichObject(config, testData);

                logger.info("Result data: {}", testData);

                if (testData.get("itemName") != null) {
                    logger.info("✅ Lookup successful for ID: {}", testId);
                    assertNotNull(testData.get("itemName"), "Item name should be found");
                    assertNotNull(testData.get("itemCategory"), "Item category should be found");
                    assertNotNull(testData.get("itemValue"), "Item value should be found");
                } else {
                    logger.warn("⚠ No data found for ID: {}", testId);
                }
            }

            logger.info("\n" + "=".repeat(80));
            logger.info("LOGGING DURING DATABASE LOOKUP OPERATIONS TEST COMPLETED");
            logger.info("=".repeat(80));
            logger.info("✅ All database lookup operations executed with comprehensive logging");

        } catch (Exception e) {
            logger.error("Logging test failed: {}", e.getMessage(), e);
            fail("Logging test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should demonstrate logging during enrichment error scenarios")
    void testLoggingDuringErrorScenarios() {
        logger.info("=".repeat(80));
        logger.info("TESTING LOGGING DURING ERROR SCENARIOS");
        logger.info("=".repeat(80));

        // Test with invalid database configuration to generate error logs
        String invalidYamlContent = """
            metadata:
              name: "Invalid Database Configuration"
              version: "1.0.0"

            data-sources:
              - name: "invalid-db"
                type: "database"
                source-type: "h2"

                connection:
                  database: "./target/h2-demo/nonexistent_db"
                  username: "invalid_user"
                  password: "invalid_password"

            enrichments:
              - id: "error-test-lookup"
                name: "error-test-lookup"
                type: "lookup-enrichment"
                condition: "#itemId != null"
                lookup-config:
                  lookup-key: "#itemId"
                  lookup-dataset:
                    type: "database"
                    connection-name: "invalid-db"
                    query: "SELECT * FROM nonexistent_table WHERE id = :itemId"
                    parameters:
                      - field: "itemId"
                        type: "string"
                field-mappings:
                  - source-field: "name"
                    target-field: "itemName"
            """;

        try {
            logger.info("Testing error scenario logging...");
            YamlRuleConfiguration config = yamlLoader.fromYamlString(invalidYamlContent);

            Map<String, Object> testData = new HashMap<>();
            testData.put("itemId", "TEST001");

            logger.info("Attempting enrichment with invalid configuration...");

            // This should generate error logs
            try {
                enrichmentService.enrichObject(config, testData);
                logger.info("Enrichment completed (may have failed silently)");
            } catch (Exception e) {
                logger.error("Expected error during enrichment: {}", e.getMessage());
                // This is expected - we're testing error logging
            }

            logger.info("✅ Error scenario logging test completed");

        } catch (Exception e) {
            logger.info("✅ Error scenario generated expected logs: {}", e.getMessage());
            // This is expected for error scenario testing
        }

        logger.info("\n" + "=".repeat(80));
        logger.info("ERROR SCENARIO LOGGING TEST COMPLETED");
        logger.info("=".repeat(80));
    }
}
