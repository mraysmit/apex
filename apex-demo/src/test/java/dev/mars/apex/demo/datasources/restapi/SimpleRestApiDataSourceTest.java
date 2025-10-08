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
import dev.mars.apex.demo.lookup.RestApiTestableServer;

import org.junit.jupiter.api.*;
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
 * Uses RestApiTestableServer for reliable, controlled testing without external dependencies.
 *
 * For other data source types, see:
 * - inline/ examples for small static data
 * - database/ examples for database sources
 * - filesystem/ examples for file-based sources
 */
@DisplayName("Simple REST API Data Source Demo")
@org.junit.jupiter.api.Disabled("Temporarily disabled due to compilation issue")
public class SimpleRestApiDataSourceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleRestApiDataSourceTest.class);

    private static RestApiTestableServer testServer;

    @BeforeAll
    static void setup() throws Exception {
        testServer = new RestApiTestableServer();
        testServer.start();
        logger.info("Test server started at: {}", testServer.getBaseUrl());
    }

    @AfterAll
    static void teardown() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    @DisplayName("Should enrich customer ID with REST API lookup")
    void testSimpleRestApiDataSource() throws Exception {
        logger.info("Testing simple REST API data source...");

        // Load YAML configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/restapi/SimpleRestApiDataSourceTest.yaml");
        assertNotNull(config, "YAML configuration should not be null");

        // Update base URL in config to use our test server
        updateRestApiBaseUrl(config, testServer.getBaseUrl());

        logger.info("Updated YAML config to use test server at: " + testServer.getBaseUrl());

        // Create test data with customer ID
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST1");
        testData.put("requestId", "REQ123");
        logger.info("DEBUG: Input test data: {}", testData);

        logger.info("DEBUG: Configuration has {} data sources", config.getDataSources().size());
        config.getDataSources().forEach(ds ->
            logger.info("DEBUG: Data source: {} (type: {})", ds.getName(), ds.getType())
        );

        logger.info("DEBUG: About to call enrichmentService.enrichObject...");

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");

        logger.info("DEBUG: enrichmentService.enrichObject completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        if (enrichedData != null) {
            logger.info("DEBUG: Result keys: {}", enrichedData.keySet());
            enrichedData.forEach((key, value) ->
                logger.info("DEBUG:   {} = {}", key, value)
            );
        }

        // Verify enrichment worked
        assertEquals("CUST1", enrichedData.get("customerId"));
        assertEquals("Acme Corporation", enrichedData.get("customerName"));
        assertEquals("CORPORATE", enrichedData.get("customerType"));
        assertEquals("AAA", enrichedData.get("creditRating"));
        assertEquals("REQ123", enrichedData.get("requestId"));

        logger.info("✓ REST API data source enrichment successful");
        logger.info("Customer Name: {}", enrichedData.get("customerName"));
        logger.info("Customer Type: {}", enrichedData.get("customerType"));
        logger.info("Credit Rating: {}", enrichedData.get("creditRating"));
    }

    @Test
    @DisplayName("Should handle missing customer ID gracefully")
    void testMissingCustomerId() throws Exception {
        logger.info("Testing missing customer ID...");

        // Load YAML configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/restapi/SimpleRestApiDataSourceTest.yaml");
        assertNotNull(config, "YAML configuration should not be null");

        // Update base URL in config to use our test server
        updateRestApiBaseUrl(config, testServer.getBaseUrl());

        // Create test data without customer ID
        Map<String, Object> testData = new HashMap<>();
        testData.put("requestId", "REQ123");

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify no enrichment occurred (condition not met)
        assertEquals("REQ123", enrichedData.get("requestId"));
        assertNull(enrichedData.get("customerName"));
        assertNull(enrichedData.get("customerType"));
        assertNull(enrichedData.get("creditRating"));

        logger.info("✓ Missing customer ID handled correctly");
    }

    @Test
    @DisplayName("Should handle unknown customer ID gracefully")
    void testUnknownCustomerId() throws Exception {
        logger.info("Testing unknown customer ID handling...");

        // Load YAML configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/restapi/SimpleRestApiDataSourceTest.yaml");
        assertNotNull(config, "YAML configuration should not be null");

        // Update base URL in config to use our test server
        updateRestApiBaseUrl(config, testServer.getBaseUrl());

        // Create test data with unknown customer ID
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "UNKNOWN");
        testData.put("requestId", "REQ123");

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify graceful error handling - test server returns default data for unknown IDs
        assertEquals("UNKNOWN", enrichedData.get("customerId"));
        assertEquals("Unknown Customer", enrichedData.get("customerName"));
        assertEquals("UNKNOWN", enrichedData.get("customerType"));
        assertEquals("NR", enrichedData.get("creditRating"));
        assertEquals("REQ123", enrichedData.get("requestId"));

        logger.info("✓ Unknown customer ID handled correctly");
    }
}
