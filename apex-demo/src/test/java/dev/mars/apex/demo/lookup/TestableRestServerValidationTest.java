/*
 * Copyright (c) 2025 APEX Rules Engine Contributors
 * Licensed under the Apache License, Version 2.0
 * Author: APEX Demo Team
 */
package dev.mars.apex.demo.lookup;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Refactored JDK HTTP Server Validation Test
 *
 * This test demonstrates how to use the reusable TestableRestApiServer
 * for HTTP server validation. The server is now decoupled from the test
 * and can be reused by other test classes.
 *
 * Key improvements:
 * - Separation of concerns: Server logic separated from test logic
 * - Reusability: TestableRestApiServer can be used by multiple test classes
 * - Maintainability: Server implementation is centralized
 * - Testability: Easier to test server functionality independently
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestableRestServerValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(TestableRestServerValidationTest.class);

    private static TestableRestApiServer testServer;
    private static String baseUrl;

    @BeforeAll
    static void setupTestSuite() throws Exception {
        logger.info("================================================================================");
        logger.info("REFACTORED JDK HTTP SERVER VALIDATION TEST SUITE");
        logger.info("================================================================================");

        // Create and start the reusable test server
        testServer = new TestableRestApiServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        logger.info("✅ Refactored test suite setup completed successfully");
        logger.info("  Using TestableRestApiServer at: {}", baseUrl);
    }

    @AfterAll
    static void teardownTestSuite() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should validate reusable server setup")
    void testReusableServerSetup() {
        logger.info("================================================================================");
        logger.info("TEST 1: Reusable Server Setup Validation");
        logger.info("================================================================================");

        // Validate server is running
        assertNotNull(testServer, "TestableRestApiServer should be initialized");
        assertTrue(testServer.isRunning(), "Server should be running");
        assertTrue(testServer.getPort() > 0, "Server should be running on a valid port");
        assertNotNull(baseUrl, "Base URL should be configured");
        assertTrue(baseUrl.startsWith("http://localhost:"), "Base URL should be localhost HTTP");

        logger.info("✅ Reusable Server Configuration:");
        logger.info("  Server Port: {}", testServer.getPort());
        logger.info("  Base URL: {}", baseUrl);
        logger.info("  Server Running: {}", testServer.isRunning());

        logger.info("✅ Reusable server setup validation completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should validate currency endpoints using reusable server")
    void testCurrencyEndpointsWithReusableServer() throws Exception {
        logger.info("================================================================================");
        logger.info("TEST 2: Currency Endpoints Validation (Reusable Server)");
        logger.info("================================================================================");

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

        // Test currency rate lookup
        String currencyUrl = baseUrl + "/api/currency/EUR";
        logger.info("🔧 Testing currency rate lookup: {}", currencyUrl);

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(currencyUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Validate response
        assertEquals(200, response.statusCode(), "Currency lookup should return 200 OK");
        assertNotNull(response.body(), "Response body should not be null");

        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"code\": \"EUR\""), "Response should contain EUR code");
        assertTrue(jsonResponse.contains("\"name\": \"Euro\""), "Response should contain Euro name");
        assertTrue(jsonResponse.contains("\"rate\": 0.85"), "Response should contain exchange rate");

        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Currency endpoint validated successfully in {}ms", responseTime);

        // Test currency conversion
        String conversionUrl = baseUrl + "/api/convert?from=USD&to=EUR&amount=100.00";
        logger.info("🔧 Testing currency conversion: {}", conversionUrl);

        java.net.http.HttpRequest conversionRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(conversionUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> conversionResponse = httpClient.send(conversionRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        endTime = System.currentTimeMillis();

        assertEquals(200, conversionResponse.statusCode(), "Currency conversion should return 200 OK");
        String conversionJson = conversionResponse.body();
        assertTrue(conversionJson.contains("\"fromCurrency\": \"USD\""), "Response should contain from currency");
        assertTrue(conversionJson.contains("\"toCurrency\": \"EUR\""), "Response should contain to currency");
        assertTrue(conversionJson.contains("\"convertedAmount\": 85.00"), "Response should contain converted amount");

        responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Conversion response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Currency conversion validated successfully in {}ms", responseTime);
        logger.info("✅ Currency endpoints validation completed successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Should validate customer endpoints using reusable server")
    void testCustomerEndpointsWithReusableServer() throws Exception {
        logger.info("================================================================================");
        logger.info("TEST 3: Customer Endpoints Validation (Reusable Server)");
        logger.info("================================================================================");

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

        // Test all customers endpoint
        String allCustomersUrl = baseUrl + "/api/customers";
        logger.info("🔧 Testing all customers endpoint: {}", allCustomersUrl);

        java.net.http.HttpRequest allCustomersRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(allCustomersUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> allCustomersResponse = httpClient.send(allCustomersRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        assertEquals(200, allCustomersResponse.statusCode(), "All customers should return 200 OK");
        String allCustomersJson = allCustomersResponse.body();
        assertTrue(allCustomersJson.contains("\"customers\""), "Response should contain customers array");
        assertTrue(allCustomersJson.contains("\"totalCount\": 5"), "Response should contain total count");

        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "All customers response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ All customers endpoint validated successfully in {}ms", responseTime);

        // Test individual customer lookup
        String customerUrl = baseUrl + "/api/customers/CUST1";
        logger.info("🔧 Testing individual customer lookup: {}", customerUrl);

        java.net.http.HttpRequest customerRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(customerUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> customerResponse = httpClient.send(customerRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        endTime = System.currentTimeMillis();

        assertEquals(200, customerResponse.statusCode(), "Customer lookup should return 200 OK");
        String customerJson = customerResponse.body();
        assertTrue(customerJson.contains("\"customerId\": \"CUST1\""), "Response should contain customer ID");
        assertTrue(customerJson.contains("\"customerName\": \"Acme Corporation\""), "Response should contain customer name");
        assertTrue(customerJson.contains("\"customerType\": \"CORPORATE\""), "Response should contain customer type");
        assertTrue(customerJson.contains("\"creditRating\": \"AAA\""), "Response should contain credit rating");

        responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Customer response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Individual customer lookup validated successfully in {}ms", responseTime);
        logger.info("✅ Customer endpoints validation completed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("Should validate health check using reusable server")
    void testHealthCheckWithReusableServer() throws Exception {
        logger.info("================================================================================");
        logger.info("TEST 4: Health Check Validation (Reusable Server)");
        logger.info("================================================================================");

        String healthUrl = baseUrl + "/api/health";
        logger.info("🔧 Testing health check endpoint: {}", healthUrl);

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(healthUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        assertEquals(200, response.statusCode(), "Health check should return 200 OK");
        String healthJson = response.body();
        assertTrue(healthJson.contains("\"status\": \"UP\""), "Health status should be UP");
        assertTrue(healthJson.contains("\"service\": \"Test REST API Server\""), "Service name should be present");
        assertTrue(healthJson.contains("\"baseUrl\": \"" + baseUrl + "\""), "Base URL should be present");

        long responseTime = endTime - startTime;
        assertTrue(responseTime < 500, "Health check response time should be < 500ms, was: " + responseTime + "ms");

        logger.info("✅ Health check validated successfully in {}ms", responseTime);
        logger.info("================================================================================");
        logger.info("🎉 REFACTORED JDK HTTP SERVER VALIDATION - ALL TESTS PASSED!");
        logger.info("================================================================================");
    }
}
