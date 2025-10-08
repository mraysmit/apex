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
package dev.mars.apex.demo.datasources.inline;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Inline Data Source Demo
 *
 * This is the SIMPLEST possible example of using inline data sources in APEX.
 * 
 * What this demonstrates:
 * - Inline data embedded directly in YAML configuration
 * - Simple lookup by currency code
 * - Basic field mapping from lookup results
 * 
 * When to use inline data sources:
 * - Small, static reference data (< 100 records)
 * - Data that rarely changes
 * - Simple lookup tables
 * - Prototyping and testing
 * 
 * For larger or dynamic data, see:
 * - database/ examples for database sources
 * - filesystem/ examples for file-based sources
 * - restapi/ examples for API sources
 */
@DisplayName("Simple Inline Data Source Demo")
public class SimpleInlineDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleInlineDataSourceTest.class);

    @Test
    @DisplayName("Should enrich currency code with inline data")
    void testSimpleInlineDataSource() {
        logger.info("Testing simple inline data source...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/inline/SimpleInlineDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with currency code
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "USD");
            testData.put("amount", 1000.0);

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify enrichment worked
            assertEquals("USD", enrichedData.get("currencyCode"));
            assertEquals("US Dollar", enrichedData.get("currencyName"));
            assertEquals("$", enrichedData.get("currencySymbol"));
            assertEquals(1000.0, enrichedData.get("amount"));

            logger.info("✓ Inline data source enrichment successful");
        } catch (Exception e) {
            fail("Inline data source test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing currency code gracefully")
    void testMissingCurrencyCode() {
        logger.info("Testing missing currency code...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/inline/SimpleInlineDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data without currency code
            Map<String, Object> testData = new HashMap<>();
            testData.put("amount", 1000.0);

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (condition not met)
            assertEquals(1000.0, enrichedData.get("amount"));
            assertNull(enrichedData.get("currencyName"));
            assertNull(enrichedData.get("currencySymbol"));

            logger.info("✓ Missing currency code handled correctly");
        } catch (Exception e) {
            fail("Missing currency code test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle unknown currency code")
    void testUnknownCurrencyCode() {
        logger.info("Testing unknown currency code...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/inline/SimpleInlineDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with unknown currency
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "XYZ");
            testData.put("amount", 1000.0);

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (no match found)
            assertEquals("XYZ", enrichedData.get("currencyCode"));
            assertEquals(1000.0, enrichedData.get("amount"));
            assertNull(enrichedData.get("currencyName"));
            assertNull(enrichedData.get("currencySymbol"));

            logger.info("✓ Unknown currency code handled correctly");
        } catch (Exception e) {
            fail("Unknown currency code test failed: " + e.getMessage());
        }
    }
}
