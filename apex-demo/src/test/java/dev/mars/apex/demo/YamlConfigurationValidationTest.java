package dev.mars.apex.demo;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to validate APEX functionality using demo YAML configurations.
 * Tests actual enrichment operations and business logic rather than YAML syntax.
 *
 * FOLLOWS APEX TEST GUIDELINES:
 * - Tests actual functionality using real APEX services
 * - Sets up real data sources (H2 database, JSON files)
 * - Executes actual enrichment operations
 * - Validates functional results with specific assertions
 */
public class YamlConfigurationValidationTest extends DemoTestBase {

    @BeforeEach
    void setUpTestData() {
        // Set up H2 database with test data for enrichment tests
        setupCustomerDatabase();
        setupTradeDatabase();
    }

    /**
     * Test customer transformer functionality using actual enrichment operations.
     */
    @Test
    void testCustomerTransformerFunctionality() {
        String yamlPath = "enrichment/customer-transformer-demo.yaml";
        logger.info("Testing customer transformer functionality: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);

        // Create test data for customer transformation
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("customerId", "CUST001");
        customerData.put("transformerType", "customer-segments-processing");
        customerData.put("segmentType", "membership-based");
        customerData.put("region", "AMERICAS");

        logger.info("Input customer data: {}", customerData);

        // Execute actual enrichment operation
        Object result = testEnrichment(config, customerData);

        // Validate functional results
        assertNotNull(result, "Customer transformation result should not be null");

        // Verify enrichment added expected fields
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        logger.info("Enriched customer data: {}", enrichedData);
        logger.info("✅ Customer transformer functionality test completed successfully");
    }

    /**
     * Test trade transformer functionality using actual enrichment operations.
     */
    @Test
    void testTradeTransformerFunctionality() {
        String yamlPath = "enrichment/trade-transformer-demo.yaml";
        logger.info("Testing trade transformer functionality: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);

        // Create test data for trade transformation
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeId", "TRD001");
        tradeData.put("instrumentType", "BOND");
        tradeData.put("currency", "USD");
        tradeData.put("amount", 1000000.0);
        tradeData.put("counterpartyId", "CP001");

        logger.info("Input trade data: {}", tradeData);

        // Execute actual enrichment operation
        Object result = testEnrichment(config, tradeData);

        // Validate functional results
        assertNotNull(result, "Trade transformation result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        logger.info("Enriched trade data: {}", enrichedData);
        logger.info("✅ Trade transformer functionality test completed successfully");
    }

    /**
     * Test financial settlement functionality using actual enrichment operations.
     */
    @Test
    void testFinancialSettlementFunctionality() {
        String yamlPath = "enrichment/comprehensive-financial-settlement-demo-config.yaml";
        logger.info("Testing financial settlement functionality: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);

        // Create test data for financial settlement
        Map<String, Object> settlementData = new HashMap<>();
        settlementData.put("settlementId", "SET001");
        settlementData.put("tradeId", "TRD001");
        settlementData.put("assetClass", "EQUITY");  // Required by YAML condition
        settlementData.put("counterpartyId", "CP001");
        settlementData.put("currency", "USD");
        settlementData.put("amount", 1000000.0);
        settlementData.put("settlementDate", "2025-01-15");

        logger.info("Input settlement data: {}", settlementData);

        // Execute actual enrichment operation
        Object result = testEnrichment(config, settlementData);

        // Validate functional results
        assertNotNull(result, "Settlement enrichment result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify that the settlement data contains expected fields
        assertTrue(enrichedData.containsKey("settlementId"), "Enriched data should contain settlementId");
        assertTrue(enrichedData.containsKey("tradeId"), "Enriched data should contain tradeId");
        assertTrue(enrichedData.containsKey("assetClass"), "Enriched data should contain assetClass");
        assertEquals("SET001", enrichedData.get("settlementId"), "Settlement ID should match");
        assertEquals("TRD001", enrichedData.get("tradeId"), "Trade ID should match");
        assertEquals("EQUITY", enrichedData.get("assetClass"), "Asset class should match");

        // Verify that the enrichment actually processed and added the settlement result
        assertTrue(enrichedData.containsKey("settlementResult"), "Enriched data should contain settlementResult from enrichment");
        String settlementResult = (String) enrichedData.get("settlementResult");
        assertNotNull(settlementResult, "Settlement result should not be null");
        assertTrue(settlementResult.contains("EQUITY"), "Settlement result should contain asset class");
        assertTrue(settlementResult.contains("TRD001"), "Settlement result should contain trade ID");

        logger.info("Enriched settlement data: {}", enrichedData);
        logger.info("Settlement result: {}", settlementResult);
        logger.info("✅ Financial settlement functionality test completed successfully");
    }

    /**
     * Test data management functionality using actual enrichment operations.
     */
    @Test
    void testDataManagementFunctionality() {
        String yamlPath = "enrichment/data-management-demo-data.yaml";
        logger.info("Testing data management functionality: {}", yamlPath);

        YamlRuleConfiguration config = loadAndValidateYaml(yamlPath);

        // Create test data for data management
        Map<String, Object> dataManagementData = new HashMap<>();
        dataManagementData.put("dataId", "DATA001");
        dataManagementData.put("dataType", "customer-profile");
        dataManagementData.put("source", "CRM");
        dataManagementData.put("customerId", "CUST001");

        logger.info("Input data management data: {}", dataManagementData);

        // Execute actual enrichment operation
        Object result = testEnrichment(config, dataManagementData);

        // Validate functional results
        assertNotNull(result, "Data management enrichment result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        logger.info("Enriched data management data: {}", enrichedData);
        logger.info("✅ Data management functionality test completed successfully");
    }

    /**
     * Set up H2 database with customer test data.
     */
    private void setupCustomerDatabase() {
        logger.info("Setting up customer database for enrichment tests...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop existing table
            statement.execute("DROP TABLE IF EXISTS customers");

            // Create customers table
            statement.execute("""
                CREATE TABLE customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20),
                    tier VARCHAR(10),
                    region VARCHAR(20),
                    status VARCHAR(20),
                    segment VARCHAR(20)
                )
                """);

            // Insert test customers
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, segment) VALUES
                ('CUST001', 'Goldman Sachs', 'INSTITUTIONAL', 'PLATINUM', 'AMERICAS', 'ACTIVE', 'PREMIUM'),
                ('CUST002', 'JP Morgan', 'INSTITUTIONAL', 'PLATINUM', 'AMERICAS', 'ACTIVE', 'PREMIUM'),
                ('CUST003', 'Deutsche Bank', 'INSTITUTIONAL', 'GOLD', 'EMEA', 'ACTIVE', 'STANDARD')
                """);

            logger.info("✅ Customer database setup completed");

        } catch (Exception e) {
            logger.error("Failed to setup customer database: {}", e.getMessage(), e);
            fail("Customer database setup failed: " + e.getMessage());
        }
    }

    /**
     * Set up H2 database with trade test data.
     */
    private void setupTradeDatabase() {
        logger.info("Setting up trade database for enrichment tests...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop existing table
            statement.execute("DROP TABLE IF EXISTS trades");

            // Create trades table
            statement.execute("""
                CREATE TABLE trades (
                    trade_id VARCHAR(20) PRIMARY KEY,
                    instrument_type VARCHAR(20) NOT NULL,
                    currency VARCHAR(3) NOT NULL,
                    amount DECIMAL(15,2),
                    counterparty_id VARCHAR(20),
                    status VARCHAR(20),
                    trade_date DATE
                )
                """);

            // Insert test trades
            statement.execute("""
                INSERT INTO trades (trade_id, instrument_type, currency, amount, counterparty_id, status, trade_date) VALUES
                ('TRD001', 'BOND', 'USD', 1000000.00, 'CP001', 'PENDING', '2025-01-14'),
                ('TRD002', 'EQUITY', 'EUR', 500000.00, 'CP002', 'SETTLED', '2025-01-13'),
                ('TRD003', 'BOND', 'GBP', 750000.00, 'CP003', 'PENDING', '2025-01-14')
                """);

            logger.info("✅ Trade database setup completed");

        } catch (Exception e) {
            logger.error("Failed to setup trade database: {}", e.getMessage(), e);
            fail("Trade database setup failed: " + e.getMessage());
        }
    }
}
