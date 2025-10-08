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
package dev.mars.apex.demo.datasources.database;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Database Data Source Demo
 *
 * This is the SIMPLEST possible example of using database data sources in APEX.
 *
 * What this demonstrates:
 * - H2 database connection
 * - Simple SQL query with parameter
 * - Basic field mapping from database columns
 *
 * When to use database data sources:
 * - Large datasets (> 1000 records)
 * - Real-time data updates
 * - Complex queries with joins
 * - Transactional data
 *
 * For other data source types, see:
 * - inline/ examples for small static data
 * - filesystem/ examples for file-based sources
 * - restapi/ examples for API sources
 */
@DisplayName("Simple Database Data Source Demo")
public class SimpleDatabaseDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDatabaseDataSourceTest.class);

    @BeforeEach
    void setupDatabase() {
        try {
            logger.info("Setting up H2 database for testing...");

            // Create H2 database connection
            Connection conn = DriverManager.getConnection(
                "jdbc:h2:./target/h2-demo/datasource_demo", "sa", "");

            Statement stmt = conn.createStatement();

            // Drop table if exists
            stmt.execute("DROP TABLE IF EXISTS customers");

            // Create customers table
            stmt.execute("""
                CREATE TABLE customers (
                    id VARCHAR(10) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL
                )
            """);

            // Insert test data
            stmt.execute("INSERT INTO customers VALUES ('C001', 'John Smith', 'john.smith@example.com')");
            stmt.execute("INSERT INTO customers VALUES ('C002', 'Jane Doe', 'jane.doe@example.com')");
            stmt.execute("INSERT INTO customers VALUES ('C003', 'Bob Wilson', 'bob.wilson@example.com')");

            stmt.close();
            conn.close();

            logger.info("✓ H2 database setup complete");
        } catch (Exception e) {
            fail("Database setup failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should enrich customer ID with database lookup")
    void testSimpleDatabaseDataSource() {
        logger.info("Testing simple database data source...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/database/SimpleDatabaseDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with customer ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "C001");
            testData.put("orderId", "ORD123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify enrichment worked
            assertEquals("C001", enrichedData.get("customerId"));
            assertEquals("John Smith", enrichedData.get("customerName"));
            assertEquals("john.smith@example.com", enrichedData.get("customerEmail"));
            assertEquals("ORD123", enrichedData.get("orderId"));

            logger.info("✓ Database data source enrichment successful");
        } catch (Exception e) {
            fail("Database data source test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing customer ID gracefully")
    void testMissingCustomerId() {
        logger.info("Testing missing customer ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/database/SimpleDatabaseDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data without customer ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("orderId", "ORD123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (condition not met)
            assertEquals("ORD123", enrichedData.get("orderId"));
            assertNull(enrichedData.get("customerName"));
            assertNull(enrichedData.get("customerEmail"));

            logger.info("✓ Missing customer ID handled correctly");
        } catch (Exception e) {
            fail("Missing customer ID test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle unknown customer ID")
    void testUnknownCustomerId() {
        logger.info("Testing unknown customer ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/database/SimpleDatabaseDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with unknown customer
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "C999");
            testData.put("orderId", "ORD123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (no match found)
            assertEquals("C999", enrichedData.get("customerId"));
            assertEquals("ORD123", enrichedData.get("orderId"));
            assertNull(enrichedData.get("customerName"));
            assertNull(enrichedData.get("customerEmail"));

            logger.info("✓ Unknown customer ID handled correctly");
        } catch (Exception e) {
            fail("Unknown customer ID test failed: " + e.getMessage());
        }
    }
}