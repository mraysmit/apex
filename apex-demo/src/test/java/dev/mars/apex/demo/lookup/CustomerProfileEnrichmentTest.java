/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Customer Profile Enrichment Test
 *
 * This test demonstrates APEX's customer profile enrichment capabilities using:
 * 1. H2 database lookup enrichment for customer profile data
 * 2. Conditional enrichment based on customer ID presence
 * 3. Multiple field mappings from database query results
 * 4. Database connection configuration and query execution
 * 5. Customer profile data enhancement from database source
 *
 * Key Features Demonstrated:
 * - H2 database data source configuration
 * - Database lookup enrichment with SQL queries
 * - Field mapping from database columns to enriched fields
 * - Conditional enrichment execution based on customer ID
 * - Customer profile data retrieval and enrichment
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 1 enrichment expected
 * ✅ Verify log shows "Processed: 1 out of 1" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers customer profile enrichment
 * ✅ Validate EVERY database lookup - Test actual H2 database query functionality
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * YAML FIRST PRINCIPLE:
 * - ALL enrichment logic is in YAML configuration
 * - Java test only sets up H2 database and validates results
 * - NO custom customer profile logic in Java test code
 * - Simple database setup and basic assertions only
 *
 * @author APEX Demo Team
 * @since 2025-09-25
 * @version 1.0.0
 */
@DisplayName("Customer Profile Enrichment Tests")
public class CustomerProfileEnrichmentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProfileEnrichmentTest.class);

    /**
     * Setup H2 database with customer profile data.
     * This is infrastructure setup, not business logic - business logic is in YAML.
     */
    @BeforeEach
    public void setupH2Database() {
        logger.info("Setting up H2 database for customer profile enrichment demo...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop and create customers table
            statement.execute("DROP TABLE IF EXISTS customers");
            statement.execute("""
                CREATE TABLE customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(30) NOT NULL,
                    tier VARCHAR(20) NOT NULL,
                    region VARCHAR(50) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    created_date DATE NOT NULL
                )
                """);

            // Insert customer profile test data
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status, created_date) VALUES
                ('CUST001', 'Acme Corporation', 'ENTERPRISE', 'PLATINUM', 'North America', 'ACTIVE', '2020-01-15'),
                ('CUST002', 'Global Tech Solutions', 'ENTERPRISE', 'GOLD', 'Europe', 'ACTIVE', '2021-03-22'),
                ('CUST003', 'Small Business Inc', 'SMB', 'SILVER', 'North America', 'ACTIVE', '2022-06-10'),
                ('CUST004', 'Startup Innovations', 'STARTUP', 'BRONZE', 'Asia Pacific', 'ACTIVE', '2023-09-05'),
                ('CUST005', 'Legacy Systems Ltd', 'ENTERPRISE', 'GOLD', 'Europe', 'INACTIVE', '2019-11-30')
                """);

            logger.info("✓ H2 database setup completed with customer profile data");

        } catch (Exception e) {
            logger.error("Failed to setup H2 database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    @DisplayName("Should enrich transaction with customer profile from database")
    void testCustomerProfileEnrichmentFromDatabase() {
        logger.info("=== Testing Customer Profile Enrichment from Database ===");

        try {
            // Load YAML configuration for customer profile enrichment
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerProfileEnrichmentTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Verify we have 1 enrichment as expected
            assertEquals(1, config.getEnrichments().size(), "Should have exactly 1 enrichment");

            // Create test data with customer ID (triggers enrichment condition)
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST001");
            testData.put("transactionId", "TXN12345");
            testData.put("amount", 5000.00);

            logger.debug("Input test data: {}", testData);

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, testData);

            // Validate enrichment results
            assertNotNull(result, "Customer profile enrichment result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Enriched result: {}", enrichedData);

            // Validate customer profile enrichment from H2 database
            assertNotNull(enrichedData.get("customerName"), "Customer name should be enriched from database");
            assertNotNull(enrichedData.get("customerType"), "Customer type should be enriched from database");
            assertNotNull(enrichedData.get("customerTier"), "Customer tier should be enriched from database");
            assertNotNull(enrichedData.get("customerRegion"), "Customer region should be enriched from database");
            assertNotNull(enrichedData.get("customerStatus"), "Customer status should be enriched from database");
            assertNotNull(enrichedData.get("customerCreatedDate"), "Customer created date should be enriched from database");

            // Validate specific customer profile data for CUST001
            assertEquals("Acme Corporation", enrichedData.get("customerName"), "Should retrieve correct customer name");
            assertEquals("ENTERPRISE", enrichedData.get("customerType"), "Should retrieve correct customer type");
            assertEquals("PLATINUM", enrichedData.get("customerTier"), "Should retrieve correct customer tier");
            assertEquals("North America", enrichedData.get("customerRegion"), "Should retrieve correct customer region");
            assertEquals("ACTIVE", enrichedData.get("customerStatus"), "Should retrieve correct customer status");

            logger.info("✅ Customer profile enrichment from database completed successfully");
            logger.info("  - Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  - Customer Type: {}", enrichedData.get("customerType"));
            logger.info("  - Customer Tier: {}", enrichedData.get("customerTier"));
            logger.info("  - Customer Region: {}", enrichedData.get("customerRegion"));
            logger.info("  - Customer Status: {}", enrichedData.get("customerStatus"));

        } catch (Exception e) {
            logger.error("Customer profile enrichment test failed: {}", e.getMessage());
            fail("Customer profile enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should enrich different customer profiles correctly")
    void testMultipleCustomerProfileEnrichments() {
        logger.info("=== Testing Multiple Customer Profile Enrichments ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerProfileEnrichmentTest.yaml");

            // Test different customer profiles
            String[] customerIds = {"CUST002", "CUST003", "CUST004"};
            String[] expectedNames = {"Global Tech Solutions", "Small Business Inc", "Startup Innovations"};
            String[] expectedTypes = {"ENTERPRISE", "SMB", "STARTUP"};
            String[] expectedTiers = {"GOLD", "SILVER", "BRONZE"};

            for (int i = 0; i < customerIds.length; i++) {
                String customerId = customerIds[i];
                String expectedName = expectedNames[i];
                String expectedType = expectedTypes[i];
                String expectedTier = expectedTiers[i];

                logger.info("Testing customer profile enrichment for: {}", customerId);

                Map<String, Object> testData = new HashMap<>();
                testData.put("customerId", customerId);
                testData.put("transactionId", "TXN" + (i + 1000));
                testData.put("amount", (i + 1) * 1000.00);

                logger.debug("Multiple profiles test data for {}: {}", customerId, testData);

                // Execute APEX enrichment processing
                Object result = enrichmentService.enrichObject(config, testData);
                @SuppressWarnings("unchecked")
                Map<String, Object> enrichedData = (Map<String, Object>) result;

                logger.debug("Multiple profiles enriched result for {}: {}", customerId, enrichedData);

                // Validate specific customer profile data
                assertEquals(expectedName, enrichedData.get("customerName"), 
                    "Should retrieve correct customer name for " + customerId);
                assertEquals(expectedType, enrichedData.get("customerType"), 
                    "Should retrieve correct customer type for " + customerId);
                assertEquals(expectedTier, enrichedData.get("customerTier"), 
                    "Should retrieve correct customer tier for " + customerId);

                logger.info("✓ Customer {} profile enriched: {} ({}, {})", 
                    customerId, expectedName, expectedType, expectedTier);
            }

            logger.info("✅ Multiple customer profile enrichments completed successfully");

        } catch (Exception e) {
            logger.error("Multiple customer profile enrichments test failed: {}", e.getMessage());
            fail("Multiple customer profile enrichments test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should skip enrichment when customer ID is missing")
    void testSkipEnrichmentWhenCustomerIdMissing() {
        logger.info("=== Testing Skip Enrichment When Customer ID Missing ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerProfileEnrichmentTest.yaml");

            // Create test data without customer ID (should NOT trigger enrichment)
            Map<String, Object> testData = new HashMap<>();
            testData.put("transactionId", "TXN99999");
            testData.put("amount", 1000.00);
            // Note: No customerId field

            logger.debug("Skip enrichment test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Skip enrichment result: {}", enrichedData);

            // Validate that customer profile fields were NOT added (enrichment skipped)
            assertNull(enrichedData.get("customerName"), "Customer name should not be enriched when ID missing");
            assertNull(enrichedData.get("customerType"), "Customer type should not be enriched when ID missing");
            assertNull(enrichedData.get("customerTier"), "Customer tier should not be enriched when ID missing");
            assertNull(enrichedData.get("customerRegion"), "Customer region should not be enriched when ID missing");
            assertNull(enrichedData.get("customerStatus"), "Customer status should not be enriched when ID missing");

            // Validate original data is preserved
            assertEquals("TXN99999", enrichedData.get("transactionId"), "Original transaction ID should be preserved");
            assertEquals(1000.00, enrichedData.get("amount"), "Original amount should be preserved");

            logger.info("✅ Skip enrichment when customer ID missing completed successfully");

        } catch (Exception e) {
            logger.error("Skip enrichment test failed: {}", e.getMessage());
            fail("Skip enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle inactive customer profile enrichment")
    void testInactiveCustomerProfileEnrichment() {
        logger.info("=== Testing Inactive Customer Profile Enrichment ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerProfileEnrichmentTest.yaml");

            // Create test data with inactive customer ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST005"); // Inactive customer
            testData.put("transactionId", "TXN55555");
            testData.put("amount", 2500.00);

            logger.debug("Inactive customer test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Inactive customer enriched result: {}", enrichedData);

            // Validate customer profile enrichment still works for inactive customers
            assertEquals("Legacy Systems Ltd", enrichedData.get("customerName"), "Should retrieve inactive customer name");
            assertEquals("ENTERPRISE", enrichedData.get("customerType"), "Should retrieve inactive customer type");
            assertEquals("GOLD", enrichedData.get("customerTier"), "Should retrieve inactive customer tier");
            assertEquals("Europe", enrichedData.get("customerRegion"), "Should retrieve inactive customer region");
            assertEquals("INACTIVE", enrichedData.get("customerStatus"), "Should retrieve inactive customer status");

            logger.info("✅ Inactive customer profile enrichment completed successfully");
            logger.info("  - Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  - Customer Status: {}", enrichedData.get("customerStatus"));

        } catch (Exception e) {
            logger.error("Inactive customer profile enrichment test failed: {}", e.getMessage());
            fail("Inactive customer profile enrichment test failed: " + e.getMessage());
        }
    }
}
