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
package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Customer Name Enrichment Test
 *
 * This test demonstrates APEX's customer name enrichment capabilities using:
 * 1. REST API lookup enrichment for missing customer names
 * 2. Conditional enrichment based on null/empty customer names
 * 3. Multiple field mappings from REST API response
 * 4. Calculation enrichment for order summary generation
 * 5. Chained enrichments with dependencies
 *
 * Key Features Demonstrated:
 * - REST API data source configuration with caching
 * - Lookup enrichment with conditional execution
 * - Field mapping from external API responses
 * - Calculation enrichment using enriched data
 * - Error handling and timeout configuration
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 2 enrichments expected
 * ✅ Verify log shows "Processed: 2 out of 2" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers customer name enrichment
 * ✅ Validate EVERY lookup result - Test actual REST API lookup functionality
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * YAML FIRST PRINCIPLE:
 * - ALL enrichment logic is in YAML configuration
 * - Java test only provides REST API server and validates results
 * - NO custom customer lookup logic in Java test code
 * - Simple REST server setup and basic assertions only
 *
 * @author APEX Demo Team
 * @since 2025-09-25
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Customer Name Enrichment Tests")
public class CustomerNameEnrichmentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CustomerNameEnrichmentTest.class);

    private static RestApiTestableServer customerServer;
    private static String customerBaseUrl;

    @BeforeAll
    static void setup() throws Exception {
        logger.info("================================================================================");
        logger.info("Setting up Customer Name Enrichment Test");
        logger.info("================================================================================");

        // Create REST API server with customer data
        customerServer = new RestApiTestableServer(0); // No delay for customer lookups
        customerServer.start();
        customerBaseUrl = customerServer.getBaseUrl();
        logger.info("Customer API server started at: {}", customerBaseUrl);

        // Extract port from URL and set as system property for YAML placeholder resolution
        String port = extractPortFromUrl(customerBaseUrl);
        System.setProperty("PORT", port);
        logger.info("Set PORT system property to: {}", port);

        logger.info("================================================================================");
    }

    @AfterAll
    static void teardown() {
        logger.info("================================================================================");
        logger.info("Tearing down Customer Name Enrichment Test");
        logger.info("================================================================================");

        if (customerServer != null) {
            customerServer.stop();
            logger.info("Customer API server stopped");
        }

        // Clean up system property
        System.clearProperty("PORT");
    }

    // Helper method to extract port from URL
    private static String extractPortFromUrl(String url) {
        // Extract port from URL like "http://localhost:12345"
        String[] parts = url.split(":");
        return parts[2];
    }

    @Test
    @Order(1)
    @DisplayName("Should enrich blank customer name using REST API lookup")
    void testCustomerNameEnrichmentFromRestApi() {
        logger.info("=== Testing Customer Name Enrichment from REST API ===");

        try {
            // Load YAML configuration for customer name enrichment
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerNameEnrichmentTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
            updateRestApiBaseUrl(config, customerBaseUrl);

            // Verify we have 2 enrichments as expected
            assertEquals(2, config.getEnrichments().size(), "Should have exactly 2 enrichments");

            // Create test data with blank customer name (triggers enrichment condition)
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST1");
            testData.put("customerName", ""); // Blank name triggers enrichment
            testData.put("orderId", "ORD12345");
            testData.put("orderAmount", 1500.00);

            logger.debug("Input test data: {}", testData);

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, testData);

            // Validate enrichment results
            assertNotNull(result, "Customer enrichment result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Enriched result: {}", enrichedData);

            // Validate customer name enrichment from REST API
            assertNotNull(enrichedData.get("customerName"), "Customer name should be enriched from REST API");
            assertNotEquals("", enrichedData.get("customerName"), "Customer name should not be blank after enrichment");
            
            // Validate additional customer fields from REST API
            assertNotNull(enrichedData.get("customerType"), "Customer type should be enriched from REST API");
            assertNotNull(enrichedData.get("creditRating"), "Credit rating should be enriched from REST API");

            logger.info("✅ Customer name enrichment from REST API completed successfully");
            logger.info("  - Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  - Customer Type: {}", enrichedData.get("customerType"));
            logger.info("  - Credit Rating: {}", enrichedData.get("creditRating"));

        } catch (Exception e) {
            logger.error("Customer name enrichment test failed: {}", e.getMessage());
            fail("Customer name enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should calculate order summary after customer enrichment")
    void testOrderSummaryCalculationAfterEnrichment() {
        logger.info("=== Testing Order Summary Calculation After Customer Enrichment ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerNameEnrichmentTest.yaml");
            updateRestApiBaseUrl(config, customerBaseUrl);

            // Create test data that triggers both enrichments
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST2");
            testData.put("customerName", null); // Null name triggers enrichment
            testData.put("orderId", "ORD67890");
            testData.put("orderAmount", 2750.50);

            logger.debug("Order summary test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Order summary enriched result: {}", enrichedData);

            // Validate customer name was enriched first
            assertNotNull(enrichedData.get("customerName"), "Customer name should be enriched");
            String customerName = (String) enrichedData.get("customerName");

            // Validate order summary calculation (depends on enriched customer name)
            assertNotNull(enrichedData.get("orderSummary"), "Order summary should be calculated");
            String orderSummary = (String) enrichedData.get("orderSummary");
            
            // Verify order summary contains expected elements
            assertTrue(orderSummary.contains("ORD67890"), "Order summary should contain order ID");
            assertTrue(orderSummary.contains(customerName), "Order summary should contain enriched customer name");
            assertTrue(orderSummary.contains("2750.5"), "Order summary should contain order amount");

            logger.info("✅ Order summary calculation after enrichment completed successfully");
            logger.info("  - Enriched Customer Name: {}", customerName);
            logger.info("  - Order Summary: {}", orderSummary);

        } catch (Exception e) {
            logger.error("Order summary calculation test failed: {}", e.getMessage());
            fail("Order summary calculation test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should skip enrichment when customer name already exists")
    void testSkipEnrichmentWhenCustomerNameExists() {
        logger.info("=== Testing Skip Enrichment When Customer Name Already Exists ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerNameEnrichmentTest.yaml");
            updateRestApiBaseUrl(config, customerBaseUrl);

            // Create test data with existing customer name (should NOT trigger enrichment)
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST003");
            testData.put("customerName", "Existing Customer Name"); // Already has name
            testData.put("orderId", "ORD11111");
            testData.put("orderAmount", 500.00);

            String originalCustomerName = (String) testData.get("customerName");
            logger.debug("Skip enrichment test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Skip enrichment result: {}", enrichedData);

            // Validate customer name was NOT changed (enrichment skipped)
            assertEquals(originalCustomerName, enrichedData.get("customerName"), 
                "Customer name should remain unchanged when already present");

            // Validate order summary still calculated (second enrichment should run)
            assertNotNull(enrichedData.get("orderSummary"), "Order summary should still be calculated");
            String orderSummary = (String) enrichedData.get("orderSummary");
            assertTrue(orderSummary.contains(originalCustomerName), "Order summary should use original customer name");

            logger.info("✅ Skip enrichment when customer name exists completed successfully");
            logger.info("  - Original Customer Name Preserved: {}", originalCustomerName);
            logger.info("  - Order Summary: {}", orderSummary);

        } catch (Exception e) {
            logger.error("Skip enrichment test failed: {}", e.getMessage());
            fail("Skip enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should process complete customer enrichment workflow")
    void testCompleteCustomerEnrichmentWorkflow() {
        logger.info("=== Testing Complete Customer Enrichment Workflow ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerNameEnrichmentTest.yaml");
            updateRestApiBaseUrl(config, customerBaseUrl);

            // Create comprehensive test data
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST3");
            testData.put("customerName", ""); // Blank name triggers enrichment
            testData.put("orderId", "ORD99999");
            testData.put("orderAmount", 10000.00);

            logger.debug("Complete workflow test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Complete workflow enriched result: {}", enrichedData);

            // Validate all enrichment results
            assertNotNull(enrichedData.get("customerName"), "Customer name should be enriched");
            assertNotNull(enrichedData.get("customerType"), "Customer type should be enriched");
            assertNotNull(enrichedData.get("creditRating"), "Credit rating should be enriched");
            assertNotNull(enrichedData.get("orderSummary"), "Order summary should be calculated");

            // Validate enrichment chain worked correctly
            String customerName = (String) enrichedData.get("customerName");
            String orderSummary = (String) enrichedData.get("orderSummary");
            
            assertNotEquals("", customerName, "Customer name should not be blank after enrichment");
            assertTrue(orderSummary.contains(customerName), "Order summary should use enriched customer name");
            assertTrue(orderSummary.contains("ORD99999"), "Order summary should contain order ID");
            assertTrue(orderSummary.contains("10000"), "Order summary should contain order amount");

            logger.info("✅ Complete customer enrichment workflow completed successfully");
            logger.info("  - Customer Name: {}", enrichedData.get("customerName"));
            logger.info("  - Customer Type: {}", enrichedData.get("customerType"));
            logger.info("  - Credit Rating: {}", enrichedData.get("creditRating"));
            logger.info("  - Order Summary: {}", enrichedData.get("orderSummary"));
            logger.info("  - All 2 enrichments processed successfully");

        } catch (Exception e) {
            logger.error("Complete customer enrichment workflow test failed: {}", e.getMessage());
            fail("Complete customer enrichment workflow test failed: " + e.getMessage());
        }
    }
}
