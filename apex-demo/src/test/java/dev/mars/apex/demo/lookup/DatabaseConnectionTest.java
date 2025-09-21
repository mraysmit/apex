package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.infrastructure.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Connection Test - YAML First Approach
 *
 * DEMONSTRATES:
 * - Database connectivity testing using APEX lookup enrichments
 * - H2 database integration with connection validation
 * - YAML-driven database access patterns
 *
 * BUSINESS LOGIC VALIDATION:
 * - Database connection isolation testing using H2 database
 * - Customer data access verification through APEX enrichments
 * - YAML-driven database connectivity validation
 *
 * YAML FIRST PRINCIPLE:
 * - ALL business logic is in YAML enrichments
 * - Java test only sets up minimal H2 data, loads YAML and calls APEX
 * - NO direct JDBC business logic or complex database operations
 * - Simple database setup and basic assertions only
 *
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 2.0.0 - Converted to YAML First approach
 */
public class DatabaseConnectionTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionTest.class);

    /**
     * Setup minimal H2 database with customer data for connection testing.
     * This is infrastructure setup, not business logic - business logic is in YAML.
     */
    @BeforeEach
    void setupH2Database() {
        logger.info("Setting up H2 database for connection testing demo...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_connection_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop and create table
            statement.execute("DROP TABLE IF EXISTS customers");
            statement.execute("""
                CREATE TABLE customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(10) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL
                )
                """);

            // Insert test customer data for connection testing
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
                ('CUST000002', 'Global Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20'),
                ('CUST000003', 'Tech Solutions Ltd', 'CORPORATE', 'SILVER', 'APAC', 'PENDING', '2023-03-10')
                """);

            logger.info("✓ H2 database setup completed for connection testing");

        } catch (Exception e) {
            logger.error("Failed to setup H2 database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    void testDatabaseConnectionFunctionality() {
        logger.info("=== Testing Database Connection Functionality ===");

        // Load YAML configuration for database connection test
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/DatabaseConnectionTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Test data - customer ID to lookup
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST000001");

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, inputData);

            // Validate enrichment results
            assertNotNull(result, "Database connection test result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate YAML-driven H2 database connection and lookup results
        assertNotNull(enrichedData.get("customerName"), "Customer name should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerType"), "Customer type should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerTier"), "Customer tier should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerRegion"), "Customer region should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerStatus"), "Customer status should be retrieved from H2 database");
        assertNotNull(enrichedData.get("createdDate"), "Created date should be retrieved from H2 database");

        // Validate specific H2 database lookup results for CUST000001
        assertEquals("Acme Corporation", enrichedData.get("customerName"), "Should retrieve correct customer name");
        assertEquals("CORPORATE", enrichedData.get("customerType"), "Should retrieve correct customer type");
        assertEquals("PLATINUM", enrichedData.get("customerTier"), "Should retrieve correct customer tier");
        assertEquals("NA", enrichedData.get("customerRegion"), "Should retrieve correct customer region");
        assertEquals("ACTIVE", enrichedData.get("customerStatus"), "Should retrieve correct customer status");

            logger.info("✓ Database connection functionality test completed successfully");

        } catch (Exception e) {
            logger.error("Database connection functionality test failed: " + e.getMessage(), e);
            fail("Database connection functionality test failed: " + e.getMessage());
        }
    }

    @Test
    void testDatabaseConnectionIsolation() {
        logger.info("=== Testing Database Connection Isolation ===");

        // Load YAML configuration for database connection isolation test
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/DatabaseConnectionTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Test data - different customer ID to test isolation
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST000002");

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, inputData);

            // Validate enrichment results
            assertNotNull(result, "Database connection isolation test result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate YAML-driven H2 database connection isolation
        assertNotNull(enrichedData.get("customerName"), "Customer name should be retrieved from isolated H2 database");
        assertNotNull(enrichedData.get("customerType"), "Customer type should be retrieved from isolated H2 database");
        assertNotNull(enrichedData.get("customerTier"), "Customer tier should be retrieved from isolated H2 database");

        // Validate specific H2 database lookup results for CUST000002 (different customer)
        assertEquals("Global Industries", enrichedData.get("customerName"), "Should retrieve correct customer name for CUST000002");
        assertEquals("CORPORATE", enrichedData.get("customerType"), "Should retrieve correct customer type for CUST000002");
        assertEquals("GOLD", enrichedData.get("customerTier"), "Should retrieve correct customer tier for CUST000002");
        assertEquals("EU", enrichedData.get("customerRegion"), "Should retrieve correct customer region for CUST000002");
        assertEquals("ACTIVE", enrichedData.get("customerStatus"), "Should retrieve correct customer status for CUST000002");

            logger.info("✓ Database connection isolation test completed successfully");

        } catch (Exception e) {
            logger.error("Database connection isolation test failed: " + e.getMessage(), e);
            fail("Database connection isolation test failed: " + e.getMessage());
        }
    }

    @Test
    void testDatabaseConnectionValidation() {
        logger.info("=== Testing Database Connection Validation ===");

        // Load YAML configuration for database connection validation test
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/DatabaseConnectionTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Test data - third customer to test validation
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("customerId", "CUST000003");

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, inputData);

            // Validate enrichment results
            assertNotNull(result, "Database connection validation test result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate YAML-driven H2 database connection validation
        assertNotNull(enrichedData.get("customerName"), "Customer name should be retrieved for validation test");
        assertNotNull(enrichedData.get("customerStatus"), "Customer status should be retrieved for validation test");

        // Validate specific H2 database lookup results for CUST000003 (pending status)
        assertEquals("Tech Solutions Ltd", enrichedData.get("customerName"), "Should retrieve correct customer name for CUST000003");
        assertEquals("SILVER", enrichedData.get("customerTier"), "Should retrieve correct customer tier for CUST000003");
        assertEquals("APAC", enrichedData.get("customerRegion"), "Should retrieve correct customer region for CUST000003");
        assertEquals("PENDING", enrichedData.get("customerStatus"), "Should retrieve correct customer status for CUST000003");

            logger.info("✓ Database connection validation test completed successfully");

        } catch (Exception e) {
            logger.error("Database connection validation test failed: " + e.getMessage(), e);
            fail("Database connection validation test failed: " + e.getMessage());
        }
    }
}
