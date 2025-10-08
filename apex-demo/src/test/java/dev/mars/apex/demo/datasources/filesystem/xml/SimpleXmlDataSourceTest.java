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
package dev.mars.apex.demo.datasources.filesystem.xml;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple XML Data Source Demo
 *
 * This is the SIMPLEST possible example of using XML file data sources in APEX.
 * 
 * What this demonstrates:
 * - XML file reading with element structure
 * - Simple lookup by department ID
 * - Basic field mapping from XML elements
 * 
 * When to use XML data sources:
 * - Legacy system data exports
 * - Configuration files
 * - Structured documents
 * - Data with hierarchical relationships
 * 
 * For other data source types, see:
 * - inline/ examples for small static data
 * - database/ examples for database sources
 * - csv/ examples for CSV files
 * - json/ examples for JSON files
 * - restapi/ examples for API sources
 */
@DisplayName("Simple XML Data Source Demo")
public class SimpleXmlDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleXmlDataSourceTest.class);

    @Test
    @DisplayName("Should enrich department ID with XML file lookup")
    void testSimpleXmlDataSource() {
        logger.info("Testing simple XML data source...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/xml/SimpleXmlDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with product ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("productId", "PROD001");
            testData.put("employeeId", "E123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify enrichment worked
            assertEquals("PROD001", enrichedData.get("productId"));
            assertEquals("US Treasury Bond", enrichedData.get("productName"));
            assertEquals(1200.0, enrichedData.get("productPrice")); // XML parser returns Double, not String
            assertEquals("FixedIncome", enrichedData.get("productCategory"));
            assertEquals("E123", enrichedData.get("employeeId"));

            logger.info("✓ XML data source enrichment successful");
        } catch (Exception e) {
            fail("XML data source test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing department ID gracefully")
    void testMissingDepartmentId() {
        logger.info("Testing missing department ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/xml/SimpleXmlDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data without department ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("employeeId", "E123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (condition not met)
            assertEquals("E123", enrichedData.get("employeeId"));
            assertNull(enrichedData.get("departmentName"));
            assertNull(enrichedData.get("departmentManager"));
            assertNull(enrichedData.get("departmentBudget"));

            logger.info("✓ Missing department ID handled correctly");
        } catch (Exception e) {
            fail("Missing department ID test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle unknown department ID")
    void testUnknownDepartmentId() {
        logger.info("Testing unknown department ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/xml/SimpleXmlDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with unknown department
            Map<String, Object> testData = new HashMap<>();
            testData.put("departmentId", "D999");
            testData.put("employeeId", "E123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (no match found)
            assertEquals("D999", enrichedData.get("departmentId"));
            assertEquals("E123", enrichedData.get("employeeId"));
            assertNull(enrichedData.get("departmentName"));
            assertNull(enrichedData.get("departmentManager"));
            assertNull(enrichedData.get("departmentBudget"));

            logger.info("✓ Unknown department ID handled correctly");
        } catch (Exception e) {
            fail("Unknown department ID test failed: " + e.getMessage());
        }
    }
}
