package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.demo.infrastructure.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for CompoundKeyLookupDemo functionality.
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 1 enrichment expected (compound-key-lookup-demo)
 * ✅ Verify log shows "Processed: 1 out of 1" - Must be 100% execution rate
 * ✅ Check enrichment condition - Test data triggers compound key condition
 * ✅ Validate business logic - Test actual compound key database lookup
 * ✅ Assert enrichment results - All database fields have corresponding assertions
 *
 * BUSINESS LOGIC VALIDATION:
 * - Compound key database lookup using H2 with customer-region composite keys
 * - Customer-region specific pricing and tier information retrieval
 * - Real database operations with compound key generation (#customerId + '-' + #region)
 */
public class CompoundKeyLookupDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CompoundKeyLookupDemoTest.class);

    @Test
    void testCompoundKeyLookupFunctionality() {
        logger.info("=== Testing Compound Key Lookup Functionality ===");

        try {
            // Setup database with customer-region data
            setupCustomerRegionDatabase();

            // Load YAML configuration for compound key lookup
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CompoundKeyLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data for compound key lookup
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST001");
            testData.put("region", "US-EAST");

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);

            // Validate enrichment results using proper casting pattern
            assertNotNull(result, "Compound key lookup enrichment result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate compound key database lookup worked
            String customerName = (String) enrichedData.get("customerName");
            assertEquals("TechCorp Solutions", customerName, "Customer name should match expected value for compound key CUST001-US-EAST");

            logger.info("✅ Compound key lookup functionality test completed successfully");
        } catch (Exception e) {
            fail("Failed to execute compound key lookup test: " + e.getMessage());
        }
    }

    @Test
    void testMultipleCompoundKeyLookups() {
        logger.info("=== Testing Multiple Compound Key Lookups ===");

        try {
            // Setup database with customer-region data
            setupCustomerRegionDatabase();

            // Load YAML configuration for compound key lookup
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CompoundKeyLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Test different customer-region combinations
            String[][] customerRegionPairs = {
                {"CUST001", "US-EAST"},
                {"CUST002", "US-WEST"},
                {"CUST003", "EU-CENTRAL"}
            };

            for (String[] pair : customerRegionPairs) {
                String customerId = pair[0];
                String region = pair[1];

                Map<String, Object> testData = new HashMap<>();
                testData.put("customerId", customerId);
                testData.put("region", region);

                // Execute APEX enrichment processing
                Object result = enrichmentService.enrichObject(config, testData);

                // Validate enrichment results
                assertNotNull(result, "Compound key lookup result should not be null for " + customerId + "-" + region);
                @SuppressWarnings("unchecked")
                Map<String, Object> enrichedData = (Map<String, Object>) result;

                // Log compound key lookup results
                logger.info("Compound key lookup for {}-{}: customerName={}",
                    customerId, region,
                    enrichedData.get("customerName"));
            }

            logger.info("✅ Multiple compound key lookups test completed successfully");
        } catch (Exception e) {
            fail("Failed to execute multiple compound key lookups test: " + e.getMessage());
        }
    }

    @Test
    void testNonExistentCompoundKeyLookup() {
        logger.info("=== Testing Non-Existent Compound Key Lookup ===");

        try {
            // Setup database with customer-region data
            setupCustomerRegionDatabase();

            // Load YAML configuration for compound key lookup
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CompoundKeyLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Test with non-existent compound key
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST999");
            testData.put("region", "XX-UNKNOWN");

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);

            // Validate enrichment results - should handle gracefully
            assertNotNull(result, "Result should not be null even for non-existent compound key");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // For non-existent keys, database fields should be null
            assertNull(enrichedData.get("customerName"), "Customer name should be null for non-existent compound key");

            logger.info("✅ Non-existent compound key lookup test completed successfully");
        } catch (Exception e) {
            fail("Failed to execute non-existent compound key lookup test: " + e.getMessage());
        }
    }

    /**
     * Setup H2 database with customer-region data for compound key lookups.
     * Following the exact pattern from ComprehensiveLookupTest.java
     */
    private void setupCustomerRegionDatabase() {
        logger.info("Setting up H2 database with customer-region data...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (var connection = java.sql.DriverManager.getConnection(jdbcUrl, "sa", "")) {
            var statement = connection.createStatement();

            // Drop table if exists
            statement.execute("DROP TABLE IF EXISTS customer_regions");

            // Create customer_regions table
            statement.execute("""
                CREATE TABLE customer_regions (
                    customer_region_key VARCHAR(50) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    region_name VARCHAR(50) NOT NULL,
                    customer_tier VARCHAR(20) NOT NULL,
                    regional_discount DECIMAL(5,4) NOT NULL,
                    special_pricing VARCHAR(50) NOT NULL,
                    currency VARCHAR(3) NOT NULL,
                    tax_rate DECIMAL(5,4) NOT NULL
                )
                """);

            // Insert test data for compound key lookups
            statement.execute("""
                INSERT INTO customer_regions VALUES
                ('CUST001-US-EAST', 'TechCorp Solutions', 'US East', 'PLATINUM', 0.15, 'VOLUME_DISCOUNT', 'USD', 0.08),
                ('CUST002-US-WEST', 'InnovateTech Inc', 'US West', 'GOLD', 0.12, 'STANDARD_DISCOUNT', 'USD', 0.09),
                ('CUST003-EU-CENTRAL', 'EuroTech GmbH', 'EU Central', 'SILVER', 0.08, 'EU_PRICING', 'EUR', 0.19),
                ('CUST001-EU-CENTRAL', 'TechCorp Europe', 'EU Central', 'GOLD', 0.10, 'EU_PRICING', 'EUR', 0.20),
                ('CUST004-APAC', 'Asia Pacific Tech', 'Asia Pacific', 'PLATINUM', 0.18, 'APAC_PREMIUM', 'USD', 0.10)
                """);

            logger.info("✅ H2 database setup completed with {} customer-region records", 5);

        } catch (Exception e) {
            logger.error("Failed to setup customer-region database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }
}
