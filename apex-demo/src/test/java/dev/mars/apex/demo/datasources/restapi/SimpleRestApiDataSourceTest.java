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
package dev.mars.apex.demo.datasources.restapi;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple REST API Data Source Demo
 *
 * This is the SIMPLEST possible example of using REST API data sources in APEX.
 * 
 * What this demonstrates:
 * - REST API connection configuration
 * - Simple HTTP GET request with parameter
 * - Basic field mapping from JSON response
 * 
 * When to use REST API data sources:
 * - Real-time data from external services
 * - Microservices integration
 * - Third-party API integration
 * - Dynamic data that changes frequently
 * 
 * NOTE: These tests are disabled by default because they require internet connectivity.
 * Remove @Disabled annotation to run tests with live API calls.
 * 
 * For other data source types, see:
 * - inline/ examples for small static data
 * - database/ examples for database sources
 * - filesystem/ examples for file-based sources
 */
@DisplayName("Simple REST API Data Source Demo")
public class SimpleRestApiDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleRestApiDataSourceTest.class);

    @Test
    @DisplayName("Should enrich user ID with REST API lookup")
    @Disabled("Requires internet connectivity - enable for live testing")
    void testSimpleRestApiDataSource() {
        logger.info("Testing simple REST API data source...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/restapi/SimpleRestApiDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with user ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("userId", "1");
            testData.put("requestId", "REQ123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify enrichment worked
            assertEquals("1", enrichedData.get("userId"));
            assertNotNull(enrichedData.get("userName"));
            assertNotNull(enrichedData.get("userEmail"));
            assertEquals("REQ123", enrichedData.get("requestId"));

            logger.info("✓ REST API data source enrichment successful");
            logger.info("User Name: {}", enrichedData.get("userName"));
            logger.info("User Email: {}", enrichedData.get("userEmail"));
        } catch (Exception e) {
            fail("REST API data source test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle missing user ID gracefully")
    void testMissingUserId() {
        logger.info("Testing missing user ID...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/restapi/SimpleRestApiDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data without user ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("requestId", "REQ123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify no enrichment occurred (condition not met)
            assertEquals("REQ123", enrichedData.get("requestId"));
            assertNull(enrichedData.get("userName"));
            assertNull(enrichedData.get("userEmail"));
            assertNull(enrichedData.get("userPhone"));

            logger.info("✓ Missing user ID handled correctly");
        } catch (Exception e) {
            fail("Missing user ID test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    @Disabled("Requires internet connectivity - enable for live testing")
    void testApiError() {
        logger.info("Testing API error handling...");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/restapi/SimpleRestApiDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Create test data with invalid user ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("userId", "999999");
            testData.put("requestId", "REQ123");

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Verify graceful error handling
            assertEquals("999999", enrichedData.get("userId"));
            assertEquals("REQ123", enrichedData.get("requestId"));
            // API may return null or empty response for invalid ID

            logger.info("✓ API error handled correctly");
        } catch (Exception e) {
            fail("API error test failed: " + e.getMessage());
        }
    }
}
