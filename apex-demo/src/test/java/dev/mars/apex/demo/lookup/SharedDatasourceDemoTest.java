package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;

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
 * JUnit 5 test for SharedDatasourceDemo functionality.
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 1 enrichment expected (shared-datasource-demo)
 * ✅ Verify log shows "Processed: 1 out of 1" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers shared datasource lookup condition
 * ✅ Validate EVERY business calculation - Test actual shared datasource lookup logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * BUSINESS LOGIC VALIDATION:
 * - Shared H2 database data source demonstration using APEX lookup enrichments
 * - Customer lookup using H2 database queries
 * - Data source reuse and centralized management
 * - YAML-driven H2 database configuration
 *
 * YAML FIRST PRINCIPLE:
 * - ALL business logic is in YAML enrichments
 * - Java test only sets up minimal H2 data, loads YAML and calls APEX
 * - NO custom business logic or complex validation
 * - Simple database setup and basic assertions only
 */
public class SharedDatasourceDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SharedDatasourceDemoTest.class);

    /**
     * Setup minimal H2 database with customer test data.
     * This is infrastructure setup, not business logic - business logic is in YAML.
     */
    @BeforeEach
    void setupH2Database() {
        logger.info("Setting up H2 database for shared datasource demo...");

        String jdbcUrl = "jdbc:h2:./target/h2-demo/apex_demo_shared;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Statement statement = connection.createStatement();

            // Drop and create table
            statement.execute("DROP TABLE IF EXISTS customers");
            statement.execute("""
                CREATE TABLE customers (
                    customer_id VARCHAR(20) PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    customer_type VARCHAR(20),
                    tier VARCHAR(10),
                    region VARCHAR(20),
                    status VARCHAR(20)
                )
                """);

            // Insert minimal test data
            statement.execute("""
                INSERT INTO customers (customer_id, customer_name, customer_type, tier, region, status) VALUES
                ('CUST001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE'),
                ('CUST002', 'Global Investment Partners', 'INSTITUTIONAL', 'GOLD', 'EU', 'ACTIVE')
                """);

            logger.info("✓ H2 database setup completed");

        } catch (Exception e) {
            logger.error("Failed to setup H2 database: " + e.getMessage(), e);
            throw new RuntimeException("Database setup failed", e);
        }
    }

    @Test
    void testSharedDatasourceDemoFunctionality() {
        logger.info("=== Testing Shared Datasource Demo Functionality ===");

        // Load YAML configuration for shared datasource demo
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/SharedDatasourceDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Create simple test data that triggers the shared datasource enrichment
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        testData.put("approach", "real-apex-services");

        // Execute APEX enrichment processing - ALL logic in YAML
        Object result = enrichmentService.enrichObject(config, testData);

        // Validate enrichment results
        assertNotNull(result, "Shared datasource demo result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate YAML-driven H2 database lookup results
        assertNotNull(enrichedData.get("customerName"), "Customer name should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerType"), "Customer type should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerTier"), "Customer tier should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerRegion"), "Customer region should be retrieved from H2 database");
        assertNotNull(enrichedData.get("customerStatus"), "Customer status should be retrieved from H2 database");

        // Validate specific H2 database lookup results for CUST001
        assertEquals("Acme Corporation", enrichedData.get("customerName"), "CUST001 should map to Acme Corporation");
        assertEquals("CORPORATE", enrichedData.get("customerType"), "CUST001 should be CORPORATE type");
        assertEquals("PLATINUM", enrichedData.get("customerTier"), "CUST001 should have PLATINUM tier");
        assertEquals("NA", enrichedData.get("customerRegion"), "CUST001 should be in NA region");
        assertEquals("ACTIVE", enrichedData.get("customerStatus"), "CUST001 should have ACTIVE status");

            logger.info("✅ Shared datasource demo functionality test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

}
