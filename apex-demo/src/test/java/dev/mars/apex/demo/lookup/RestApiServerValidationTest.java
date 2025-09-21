/*
 * Copyright (c) 2025 APEX Rules Engine Contributors
 * Licensed under the Apache License, Version 2.0
 * Author: APEX Demo Team
 */
package dev.mars.apex.demo.lookup;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;

/**
 * REST API Server Validation Test
 *
 * This test demonstrates how to use the reusable RestApiTestableServer
 * for HTTP server validation. The server is now decoupled from the test
 * and can be reused by other test classes.
 *
 * Key improvements:
 * - Separation of concerns: Server logic separated from test logic
 * - Reusability: RestApiTestableServer can be used by multiple test classes
 * - Maintainability: Server implementation is centralized
 * - Testability: Easier to test server functionality independently
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 2.0.0 (Renamed with RestApi prefix for consistency)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestApiServerValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(RestApiServerValidationTest.class);

    private static RestApiTestableServer testServer;
    private static String baseUrl;

    @BeforeAll
    static void setupTestSuite() throws Exception {
        logger.info("================================================================================");
        logger.info("REST API SERVER VALIDATION TEST SUITE");
        logger.info("================================================================================");

        // Create and start the reusable test server
        testServer = new RestApiTestableServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        logger.info("✅ REST API server validation test suite setup completed successfully");
        logger.info("  Using RestApiTestableServer at: {}", baseUrl);
    }

    @AfterAll
    static void teardownTestSuite() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Server Infrastructure Validation")
    void testServerInfrastructureValidation() throws Exception {
        logger.info("=== Server Infrastructure Validation ===");

        // Validate server is running
        org.junit.jupiter.api.Assertions.assertTrue(testServer.isRunning(), "Server should be running");
        org.junit.jupiter.api.Assertions.assertNotNull(baseUrl, "Base URL should not be null");
        org.junit.jupiter.api.Assertions.assertTrue(baseUrl.startsWith("http://localhost:"), "Base URL should be localhost");

        logger.info("✅ Server infrastructure validation completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Currency Endpoint Validation")
    void testCurrencyEndpointValidation() throws Exception {
        logger.info("=== Currency Endpoint Validation ===");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Test USD currency endpoint
        String usdUrl = baseUrl + "/api/currency/USD";
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(usdUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode(), "Currency endpoint should return 200");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("USD"), "Response should contain USD");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("US Dollar"), "Response should contain currency name");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("rate"), "Response should contain rate");

        logger.info("📊 USD Response: {}", response.body());
        logger.info("✅ Currency endpoint validation completed successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Currency Conversion Endpoint Validation")
    void testCurrencyConversionEndpointValidation() throws Exception {
        logger.info("=== Currency Conversion Endpoint Validation ===");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Test currency conversion endpoint
        String conversionUrl = baseUrl + "/api/convert?from=USD&to=EUR&amount=100";
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(conversionUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode(), "Conversion endpoint should return 200");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("fromCurrency"), "Response should contain fromCurrency");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("toCurrency"), "Response should contain toCurrency");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("convertedAmount"), "Response should contain convertedAmount");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("exchangeRate"), "Response should contain exchangeRate");

        logger.info("📊 Conversion Response: {}", response.body());
        logger.info("✅ Currency conversion endpoint validation completed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("Customer Endpoint Validation")
    void testCustomerEndpointValidation() throws Exception {
        logger.info("=== Customer Endpoint Validation ===");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Test individual customer endpoint
        String customerUrl = baseUrl + "/api/customers/CUST1";
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(customerUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode(), "Customer endpoint should return 200");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("CUST1"), "Response should contain customer ID");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("customerName"), "Response should contain customer name");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("customerType"), "Response should contain customer type");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("creditRating"), "Response should contain credit rating");

        logger.info("📊 Customer Response: {}", response.body());

        // Test all customers endpoint
        String allCustomersUrl = baseUrl + "/api/customers";
        java.net.http.HttpRequest allCustomersRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(allCustomersUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> allCustomersResponse = httpClient.send(allCustomersRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response
        org.junit.jupiter.api.Assertions.assertEquals(200, allCustomersResponse.statusCode(), "All customers endpoint should return 200");
        org.junit.jupiter.api.Assertions.assertTrue(allCustomersResponse.body().contains("customers"), "Response should contain customers array");
        org.junit.jupiter.api.Assertions.assertTrue(allCustomersResponse.body().contains("totalCount"), "Response should contain total count");

        logger.info("📊 All Customers Response: {}", allCustomersResponse.body());
        logger.info("✅ Customer endpoint validation completed successfully");
    }

    @Test
    @Order(5)
    @DisplayName("Health Check Endpoint Validation")
    void testHealthCheckEndpointValidation() throws Exception {
        logger.info("=== Health Check Endpoint Validation ===");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Test health check endpoint
        String healthUrl = baseUrl + "/api/health";
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(healthUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode(), "Health check endpoint should return 200");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("status"), "Response should contain status");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("UP"), "Response should contain UP status");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("timestamp"), "Response should contain timestamp");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("service"), "Response should contain service name");

        logger.info("📊 Health Check Response: {}", response.body());
        logger.info("✅ Health check endpoint validation completed successfully");
    }

    @Test
    @Order(6)
    @DisplayName("Error Handling Validation")
    void testErrorHandlingValidation() throws Exception {
        logger.info("=== Error Handling Validation ===");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Test invalid currency endpoint
        String invalidUrl = baseUrl + "/api/currency/INVALID";
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(invalidUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response (should still return 200 with unknown currency data)
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode(), "Invalid currency should return 200 with unknown data");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("INVALID"), "Response should contain requested currency code");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("Unknown Currency"), "Response should contain unknown currency name");

        logger.info("📊 Invalid Currency Response: {}", response.body());

        // Test invalid customer endpoint
        String invalidCustomerUrl = baseUrl + "/api/customers/INVALID";
        java.net.http.HttpRequest invalidCustomerRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(invalidCustomerUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> invalidCustomerResponse = httpClient.send(invalidCustomerRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response (should still return 200 with unknown customer data)
        org.junit.jupiter.api.Assertions.assertEquals(200, invalidCustomerResponse.statusCode(), "Invalid customer should return 200 with unknown data");
        org.junit.jupiter.api.Assertions.assertTrue(invalidCustomerResponse.body().contains("INVALID"), "Response should contain requested customer ID");
        org.junit.jupiter.api.Assertions.assertTrue(invalidCustomerResponse.body().contains("Unknown Customer"), "Response should contain unknown customer name");

        logger.info("📊 Invalid Customer Response: {}", invalidCustomerResponse.body());
        logger.info("✅ Error handling validation completed successfully");
    }

    @Test
    @Order(7)
    @DisplayName("Performance and Load Validation")
    void testPerformanceAndLoadValidation() throws Exception {
        logger.info("=== Performance and Load Validation ===");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Perform multiple requests to test performance
        long startTime = System.currentTimeMillis();
        int requestCount = 10;

        for (int i = 0; i < requestCount; i++) {
            String url = baseUrl + "/api/currency/USD";
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode(), "Request " + (i + 1) + " should succeed");
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageTime = (double) totalTime / requestCount;

        logger.info("📊 Performance Metrics:");
        logger.info("  Total Requests: {}", requestCount);
        logger.info("  Total Time: {} ms", totalTime);
        logger.info("  Average Time per Request: {:.2f} ms", averageTime);

        // Validate performance (should be reasonable for test server)
        org.junit.jupiter.api.Assertions.assertTrue(averageTime < 1000, "Average response time should be less than 1 second");

        logger.info("✅ Performance and load validation completed successfully");
    }
}
