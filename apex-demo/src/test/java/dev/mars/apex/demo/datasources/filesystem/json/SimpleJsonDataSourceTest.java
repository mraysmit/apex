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
package dev.mars.apex.demo.datasources.filesystem.json;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple JSON Data Source Demo
 *
 * This is the SIMPLEST possible example of using JSON file data sources in APEX.
 * 
 * What this demonstrates:
 * - JSON file reading with array of objects
 * - Simple lookup by user ID
 * - Basic field mapping from JSON properties
 * 
 * When to use JSON data sources:
 * - Medium-sized datasets (100-10,000 records)
 * - Data from REST APIs saved to files
 * - Configuration data
 * - Structured reference data
 * 
 * For other data source types, see:
 * - inline/ examples for small static data
 * - database/ examples for database sources
 * - csv/ examples for CSV files
 * - xml/ examples for XML files
 * - restapi/ examples for API sources
 */
@DisplayName("Simple JSON Data Source Demo")
public class SimpleJsonDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleJsonDataSourceTest.class);

    @Test
    @DisplayName("Should enrich user ID with JSON file lookup")
    void testSimpleJsonDataSource() {
        logger.info("Testing simple JSON data source...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/json/SimpleJsonDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with product ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("productId", "PROD001");
            testData.put("taskId", "T123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify enrichment worked
            assertEquals("PROD001", enrichedData.get("productId"));
            assertEquals("US Treasury Bond", enrichedData.get("productName"));
            assertEquals(1200.0, enrichedData.get("productPrice"));
            assertEquals("FixedIncome", enrichedData.get("productCategory"));
            assertEquals("T123", enrichedData.get("taskId"));

            logger.info("✓ JSON data source enrichment successful");
        } catch (Exception e) {
            fail("JSON data source test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing user ID gracefully")
    void testMissingUserId() {
        logger.info("Testing missing user ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/json/SimpleJsonDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data without user ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("taskId", "T123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (condition not met)
            assertEquals("T123", enrichedData.get("taskId"));
            assertNull(enrichedData.get("userName"));
            assertNull(enrichedData.get("userEmail"));
            assertNull(enrichedData.get("userRole"));

            logger.info("✓ Missing user ID handled correctly");
        } catch (Exception e) {
            fail("Missing user ID test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle unknown user ID")
    void testUnknownUserId() {
        logger.info("Testing unknown user ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/json/SimpleJsonDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with unknown user
            Map<String, Object> testData = new HashMap<>();
            testData.put("userId", "U999");
            testData.put("taskId", "T123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (no match found)
            assertEquals("U999", enrichedData.get("userId"));
            assertEquals("T123", enrichedData.get("taskId"));
            assertNull(enrichedData.get("userName"));
            assertNull(enrichedData.get("userEmail"));
            assertNull(enrichedData.get("userRole"));

            logger.info("✓ Unknown user ID handled correctly");
        } catch (Exception e) {
            fail("Unknown user ID test failed: " + e.getMessage());
        }
    }
}
