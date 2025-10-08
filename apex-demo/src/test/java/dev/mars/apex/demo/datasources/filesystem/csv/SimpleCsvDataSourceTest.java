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
package dev.mars.apex.demo.datasources.filesystem.csv;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple CSV Data Source Demo
 *
 * This is the SIMPLEST possible example of using CSV file data sources in APEX.
 * 
 * What this demonstrates:
 * - CSV file reading with header row
 * - Simple lookup by product ID
 * - Basic field mapping from CSV columns
 * 
 * When to use CSV data sources:
 * - Spreadsheet exports (100-10,000 records)
 * - Batch data updates
 * - Simple tabular data
 * - Data from Excel or Google Sheets
 * 
 * For other data source types, see:
 * - inline/ examples for small static data
 * - database/ examples for database sources
 * - json/ examples for JSON files
 * - xml/ examples for XML files
 * - restapi/ examples for API sources
 */
@DisplayName("Simple CSV Data Source Demo")
public class SimpleCsvDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleCsvDataSourceTest.class);

    @Test
    @DisplayName("Should enrich product ID with CSV file lookup")
    void testSimpleCsvDataSource() {
        logger.info("Testing simple CSV data source...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/csv/SimpleCsvDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with customer ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "1");
            testData.put("orderId", "ORD123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify enrichment worked
            assertEquals("1", enrichedData.get("customerId"));
            assertEquals("John Doe", enrichedData.get("customerName"));
            assertEquals("john@example.com", enrichedData.get("customerEmail"));
            assertEquals("ACTIVE", enrichedData.get("customerStatus"));
            assertEquals("ORD123", enrichedData.get("orderId"));

            logger.info("✓ CSV data source enrichment successful");
        } catch (Exception e) {
            fail("CSV data source test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing product ID gracefully")
    void testMissingProductId() {
        logger.info("Testing missing product ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/csv/SimpleCsvDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data without product ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("orderId", "ORD123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (condition not met)
            assertEquals("ORD123", enrichedData.get("orderId"));
            assertNull(enrichedData.get("productName"));
            assertNull(enrichedData.get("productPrice"));
            assertNull(enrichedData.get("productCategory"));

            logger.info("✓ Missing product ID handled correctly");
        } catch (Exception e) {
            fail("Missing product ID test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle unknown product ID")
    void testUnknownProductId() {
        logger.info("Testing unknown product ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/csv/SimpleCsvDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with unknown product
            Map<String, Object> testData = new HashMap<>();
            testData.put("productId", "P999");
            testData.put("orderId", "ORD123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (no match found)
            assertEquals("P999", enrichedData.get("productId"));
            assertEquals("ORD123", enrichedData.get("orderId"));
            assertNull(enrichedData.get("productName"));
            assertNull(enrichedData.get("productPrice"));
            assertNull(enrichedData.get("productCategory"));

            logger.info("✓ Unknown product ID handled correctly");
        } catch (Exception e) {
            fail("Unknown product ID test failed: " + e.getMessage());
        }
    }
}
