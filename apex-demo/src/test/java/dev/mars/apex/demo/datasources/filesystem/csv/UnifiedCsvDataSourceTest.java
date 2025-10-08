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
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unified CSV Data Source Test
 *
 * Demonstrates the unified file-system approach for CSV files.
 * This shows how CSV files can be handled using type: "file-system"
 * with format-config, providing consistency with JSON and XML files.
 *
 * @author APEX Demo Team
 * @since 2025-10-08
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UnifiedCsvDataSourceTest extends DemoTestBase {

    @Test
    @Order(1)
    @DisplayName("Test unified CSV data source with file-system type")
    void testUnifiedCsvDataSource() throws Exception {
        logger.info("Testing unified CSV data source...");

        // Load configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/csv/UnifiedCsvDataSourceTest.yaml");

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "1");
        testData.put("employeeId", "E123");

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Cast result to Map for assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify enrichment worked
        assertEquals("1", enrichedData.get("customerId"));
        assertEquals("John Doe", enrichedData.get("customerName"));
        assertEquals("john@example.com", enrichedData.get("customerEmail"));
        assertEquals("ACTIVE", enrichedData.get("customerStatus"));
        assertEquals("E123", enrichedData.get("employeeId"));

        logger.info("✓ Unified CSV data source enrichment successful");
    }

    @Test
    @Order(2)
    @DisplayName("Test unknown customer ID with unified approach")
    void testUnknownCustomerId() throws Exception {
        logger.info("Testing unknown customer ID with unified approach...");

        // Load configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/csv/UnifiedCsvDataSourceTest.yaml");

        // Test data with unknown customer ID
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "999");
        testData.put("employeeId", "E456");

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Cast result to Map for assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify original data is preserved
        assertEquals("999", enrichedData.get("customerId"));
        assertEquals("E456", enrichedData.get("employeeId"));

        // Verify no enrichment occurred (fields should not exist)
        assertNull(enrichedData.get("customerName"));
        assertNull(enrichedData.get("customerEmail"));
        assertNull(enrichedData.get("customerStatus"));

        logger.info("✓ Unknown customer ID handled correctly with unified approach");
    }

    @Test
    @Order(3)
    @DisplayName("Test missing customer ID with unified approach")
    void testMissingCustomerId() throws Exception {
        logger.info("Testing missing customer ID with unified approach...");

        // Load configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/csv/UnifiedCsvDataSourceTest.yaml");

        // Test data without customer ID
        Map<String, Object> testData = new HashMap<>();
        testData.put("employeeId", "E789");

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Cast result to Map for assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify original data is preserved
        assertEquals("E789", enrichedData.get("employeeId"));

        // Verify no enrichment occurred (condition not met)
        assertNull(enrichedData.get("customerName"));
        assertNull(enrichedData.get("customerEmail"));
        assertNull(enrichedData.get("customerStatus"));

        logger.info("✓ Missing customer ID handled correctly with unified approach");
    }
}
