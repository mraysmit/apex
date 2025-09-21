/*
 * Copyright (c) 2025 Augment Code Ltd.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of Augment Code Ltd.
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Augment Code Ltd.
 */
package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Integration Test
 *
 * This test class demonstrates the two fundamental APEX patterns:
 * 1. Currency Code Format Validation - Simple field validation using APEX validation rules
 * 2. Customer Name Enrichment - Data enrichment using REST API lookup
 *
 * Key Features:
 * - Uses TestableRestApiServer for realistic REST API integration
 * - Follows existing APEX patterns from demo codebase
 * - Comprehensive validation following APEX principles
 * - Separate YAML configurations for each test pattern
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 1.0.0
 */
public class RestApiIntegrationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RestApiIntegrationTest.class);

    private TestableRestApiServer testServer;
    private String baseUrl;
    private int serverPort;

    @BeforeEach
    void setupRestApiServer() throws IOException {
        logger.info("🌐 Setting up TestableRestApiServer for REST API integration tests...");
        
        // Create and start the reusable test server
        testServer = new TestableRestApiServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();
        serverPort = extractPortFromUrl(baseUrl);
        
        logger.info("✅ TestableRestApiServer ready at: {}", baseUrl);
        logger.info("  Currency Endpoint: {}/api/currency/{{currencyCode}}", baseUrl);
        logger.info("  Customer Endpoint: {}/api/customers/{{customerId}}", baseUrl);
    }

    @AfterEach
    void teardownRestApiServer() {
        if (testServer != null) {
            testServer.stop();
            logger.info("🛑 TestableRestApiServer stopped");
        }
    }

    @Test
    @DisplayName("Test 1: Currency Code Format Validation - Simple APEX Validation")
    void testCurrencyCodeFormatValidation() throws Exception {
        logger.info("=== Test 1: Currency Code Format Validation ===");
        
        // Load YAML configuration with dynamic port substitution
        String tempYamlPath = updateYamlWithServerPort("src/test/java/dev/mars/apex/demo/lookup/CurrencyCodeValidationTest.yaml");
        var config = yamlLoader.loadFromFile(tempYamlPath);
        assertNotNull(config, "YAML configuration should not be null");
        
        // Test 1a: Valid currency code format
        logger.info("Testing valid currency code format...");
        Map<String, Object> validData = Map.of(
            "transactionId", "TXN-001",
            "currencyCode", "EUR",  // Valid format
            "amount", 1000.00
        );
        
        // Process through APEX - validation only
        Object result = enrichmentService.enrichObject(config, validData);
        assertNotNull(result, "APEX processing should return result");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> validatedData = (Map<String, Object>) result;
        
        // Validate Every Operation - Original data preserved
        assertEquals("TXN-001", validatedData.get("transactionId"), "Original transaction ID preserved");
        assertEquals("EUR", validatedData.get("currencyCode"), "Original currency code preserved");
        assertEquals(1000.00, validatedData.get("amount"), "Original amount preserved");
        
        // Validate Actual Results - No validation errors for valid format
        assertNull(validatedData.get("validationErrors"), "Should have no validation errors for valid EUR format");
        
        // Test 1b: Invalid currency code format
        logger.info("Testing invalid currency code format...");
        Map<String, Object> invalidData = Map.of(
            "transactionId", "TXN-002",
            "currencyCode", "euro",  // Invalid format (lowercase)
            "amount", 500.00
        );
        
        Object invalidResult = enrichmentService.enrichObject(config, invalidData);
        @SuppressWarnings("unchecked")
        Map<String, Object> invalidValidated = (Map<String, Object>) invalidResult;
        
        // Should have validation errors for invalid format
        assertNotNull(invalidValidated.get("validationErrors"), "Should have validation errors for 'euro'");
        assertTrue(invalidValidated.get("validationErrors").toString().contains("3 uppercase letters"), 
            "Should contain format validation error message");
        
        // Test 1c: Multiple validation errors
        logger.info("Testing multiple validation errors...");
        Map<String, Object> multiErrorData = Map.of(
            "transactionId", "INVALID",  // Invalid format
            "currencyCode", "us",        // Invalid format
            "amount", -100.00            // Invalid amount
        );
        
        Object multiErrorResult = enrichmentService.enrichObject(config, multiErrorData);
        @SuppressWarnings("unchecked")
        Map<String, Object> multiErrorValidated = (Map<String, Object>) multiErrorResult;
        
        // Should have multiple validation errors
        assertNotNull(multiErrorValidated.get("validationErrors"), "Should have multiple validation errors");
        String errors = multiErrorValidated.get("validationErrors").toString();
        assertTrue(errors.contains("TXN-"), "Should contain transaction ID format error");
        assertTrue(errors.contains("3 uppercase"), "Should contain currency format error");
        assertTrue(errors.contains("positive"), "Should contain amount validation error");
        
        logger.info("✅ Currency Code Format Validation test completed successfully");
        
        // Cleanup temp file
        Files.deleteIfExists(Paths.get(tempYamlPath));
    }

    @Test
    @DisplayName("Test 2: Customer Name Enrichment - APEX Lookup Enrichment")
    void testCustomerNameEnrichment() throws Exception {
        logger.info("=== Test 2: Customer Name Enrichment ===");
        
        // Load YAML configuration with dynamic port substitution
        String tempYamlPath = updateYamlWithServerPort("src/test/java/dev/mars/apex/demo/lookup/CustomerNameEnrichmentTest.yaml");
        var config = yamlLoader.loadFromFile(tempYamlPath);
        assertNotNull(config, "YAML configuration should not be null");
        
        // Test 2a: Enrich blank customer name with known customer ID
        logger.info("Testing customer name enrichment for known customer...");
        Map<String, Object> enrichmentData = Map.of(
            "orderId", "ORD-001",
            "customerId", "CUST1",
            "customerName", "",  // Blank - will be enriched
            "orderAmount", 2500.00
        );
        
        // Process through APEX - enrichment only
        Object result = enrichmentService.enrichObject(config, enrichmentData);
        assertNotNull(result, "APEX processing should return enriched result");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate Every Operation - Original data preserved
        assertEquals("ORD-001", enrichedData.get("orderId"), "Original order ID preserved");
        assertEquals("CUST1", enrichedData.get("customerId"), "Original customer ID preserved");
        assertEquals(2500.00, enrichedData.get("orderAmount"), "Original order amount preserved");
        
        // Validate Actual Results - Customer data enriched from REST API
        assertEquals("Acme Corporation", enrichedData.get("customerName"), 
            "Should enrich blank customer name with 'Acme Corporation' from REST API lookup");
        assertEquals("CORPORATE", enrichedData.get("customerType"), 
            "Should enrich with customer type from REST API");
        assertEquals("AAA", enrichedData.get("creditRating"), 
            "Should enrich with credit rating from REST API");
        
        // Validate calculation enrichment
        assertNotNull(enrichedData.get("orderSummary"), "Should have order summary calculation");
        String orderSummary = (String) enrichedData.get("orderSummary");
        assertTrue(orderSummary.contains("ORD-001"), "Order summary should contain order ID");
        assertTrue(orderSummary.contains("Acme Corporation"), "Order summary should contain enriched customer name");
        assertTrue(orderSummary.contains("2500"), "Order summary should contain order amount");
        
        // Test 2b: Test with unknown customer ID
        logger.info("Testing customer name enrichment for unknown customer...");
        Map<String, Object> unknownCustomerData = Map.of(
            "orderId", "ORD-002",
            "customerId", "UNKN1",  // Unknown customer
            "customerName", "",
            "orderAmount", 1000.00
        );

        Object unknownResult = enrichmentService.enrichObject(config, unknownCustomerData);
        @SuppressWarnings("unchecked")
        Map<String, Object> unknownEnriched = (Map<String, Object>) unknownResult;

        // Should handle unknown customer gracefully
        assertEquals("Unknown Customer", unknownEnriched.get("customerName"),
            "Should use default customer name for unknown customer ID");
        assertEquals("UNKNOWN", unknownEnriched.get("customerType"),
            "Should use default customer type for unknown customer ID");

        // Test 2c: Test with existing customer name (should not be overwritten)
        logger.info("Testing customer name enrichment with existing name...");
        Map<String, Object> existingNameData = Map.of(
            "orderId", "ORD-003",
            "customerId", "CUST2",
            "customerName", "Existing Customer Name",  // Not blank - should not be enriched
            "orderAmount", 750.00
        );

        Object existingResult = enrichmentService.enrichObject(config, existingNameData);
        @SuppressWarnings("unchecked")
        Map<String, Object> existingEnriched = (Map<String, Object>) existingResult;

        // Should preserve existing customer name
        assertEquals("Existing Customer Name", existingEnriched.get("customerName"),
            "Should preserve existing customer name and not enrich");

        logger.info("✅ Customer Name Enrichment test completed successfully");

        // Cleanup temp file
        Files.deleteIfExists(Paths.get(tempYamlPath));
    }

    // Helper methods
    private int extractPortFromUrl(String url) {
        // Extract port from URL like "http://localhost:12345"
        String[] parts = url.split(":");
        return Integer.parseInt(parts[2]);
    }

    private String updateYamlWithServerPort(String originalYamlPath) throws IOException {
        // Read original YAML content
        Path originalPath = Paths.get(originalYamlPath);
        String yamlContent = Files.readString(originalPath);
        
        // Replace ${PORT} placeholder with actual server port
        String updatedContent = yamlContent.replace("${PORT}", String.valueOf(serverPort));
        
        // Write to temporary file
        Path tempPath = Paths.get(originalYamlPath.replace(".yaml", "-temp-" + System.currentTimeMillis() + ".yaml"));
        Files.writeString(tempPath, updatedContent);
        
        return tempPath.toString();
    }
}
