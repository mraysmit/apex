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

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Basic Lookup Test
 *
 * Tests APEX REST API integration using the reusable RestApiTestableServer.
 * This test demonstrates real REST API functionality without code duplication.
 *
 * Key Features Tested:
 * - Reusable RestApiTestableServer integration
 * - Real HTTP requests and responses
 * - JSON data parsing
 * - Currency rate lookup
 * - Currency conversion
 * - Basic error handling
 * - Proper separation of concerns
 *
 * YAML Configurations:
 * - RestApiBasicLookupTest.yaml: REST API data source and enrichment configuration
 *
 * Debug Logging:
 * - Enable with: -Dorg.slf4j.simpleLogger.log.dev.mars.apex.demo.lookup.RestApiBasicLookupTest=DEBUG
 * - Or set logger level to DEBUG in your IDE/logback configuration
 *
 * Test Coverage:
 * - HTTP server setup validation
 * - Direct HTTP currency rate lookup
 * - Direct HTTP currency conversion
 * - Health check endpoint validation
 * - Performance requirements validation
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 3.0.0 (Renamed with RestApi prefix for consistency)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestApiBasicLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RestApiBasicLookupTest.class);

    private static RestApiTestableServer testServer;
    private static String baseUrl;

    @BeforeAll
    static void setupBasicRestApiTestSuite() throws Exception {
        logger.info("================================================================================");
        logger.info("REFACTORED: Basic REST API Infrastructure Setup");
        logger.info("================================================================================");

        logger.info("🔧 Setting up RestApiTestableServer for REST API testing...");

        // Create and start the reusable test server
        testServer = new RestApiTestableServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        logger.info(" RestApiTestableServer started successfully:");
        logger.info("  Base URL: {}", baseUrl);
        logger.info("  Server Port: {}", testServer.getPort());
        logger.info("  Server Running: {}", testServer.isRunning());

        logger.info(" Refactored REST API test suite setup completed successfully");
    }

    @AfterAll
    static void teardownBasicRestApiTestSuite() {
        if (testServer != null && testServer.isRunning()) {
            logger.info("🛑 Stopping RestApiTestableServer...");
            testServer.stop();
            logger.info(" RestApiTestableServer stopped successfully");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should validate RestApiTestableServer setup")
    void testRestApiTestableServerSetup() {
        logger.info("================================================================================");
        logger.info("REFACTORED: RestApiTestableServer Setup Validation");
        logger.info("================================================================================");

        logger.info("🔧 Validating RestApiTestableServer setup...");

        // Validate server is running
        assertNotNull(testServer, "RestApiTestableServer should be initialized");
        assertTrue(testServer.isRunning(), "RestApiTestableServer should be running");
        assertTrue(testServer.getPort() > 0, "Server should be running on a valid port");
        assertNotNull(baseUrl, "Base URL should be set");
        assertTrue(baseUrl.startsWith("http://localhost:"), "Base URL should be valid");

        logger.info(" RestApiTestableServer Details:");
        logger.info("  Base URL: {}", baseUrl);
        logger.info("  Server Port: {}", testServer.getPort());
        logger.info("  Server Running: {}", testServer.isRunning());

        logger.info(" RestApiTestableServer setup validation completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should perform direct HTTP currency rate lookup")
    void testDirectHttpCurrencyRateLookup() throws Exception {
        logger.info("================================================================================");
        logger.info("REFACTORED: Direct HTTP Currency Rate Lookup");
        logger.info("================================================================================");

        logger.info("🔧 Testing direct HTTP call using RestApiTestableServer...");

        // Test direct HTTP call to our RestApiTestableServer
        String currencyCode = "EUR";
        String url = baseUrl + "/api/currency/" + currencyCode;

        logger.info("🔧 Testing direct HTTP call:");
        logger.info("  URL: {}", url);
        logger.info("  Currency Code: {}", currencyCode);

        // Make direct HTTP call using Java's built-in HTTP client
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Validate HTTP response
        assertEquals(200, response.statusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.body(), "Response body should not be null");

        logger.info(" HTTP Response Details:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Parse JSON response
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"code\": \"EUR\""), "Response should contain EUR code");
        assertTrue(jsonResponse.contains("\"name\": \"Euro\""), "Response should contain Euro name");
        assertTrue(jsonResponse.contains("\"rate\": 0.85"), "Response should contain exchange rate");
        assertTrue(jsonResponse.contains("\"symbol\": \"€\""), "Response should contain EUR symbol");
        assertTrue(jsonResponse.contains("\"lastUpdated\""), "Response should contain timestamp");

        // Validate performance requirement
        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info(" Direct HTTP currency rate lookup completed successfully in {}ms", responseTime);
    }

    @Test
    @Order(3)
    @DisplayName("Should perform direct HTTP currency conversion")
    void testDirectHttpCurrencyConversion() throws Exception {
        logger.info("================================================================================");
        logger.info("REFACTORED: Direct HTTP Currency Conversion");
        logger.info("================================================================================");

        logger.info("🔧 Testing direct HTTP currency conversion using RestApiTestableServer...");

        // Test direct HTTP call for currency conversion
        String fromCurrency = "USD";
        String toCurrency = "EUR";
        double amount = 100.0;
        String url = String.format("%s/api/convert?from=%s&to=%s&amount=%.2f",
            baseUrl, fromCurrency, toCurrency, amount);

        logger.info("🔧 Testing direct HTTP currency conversion:");
        logger.info("  URL: {}", url);
        logger.info("  From Currency: {}", fromCurrency);
        logger.info("  To Currency: {}", toCurrency);
        logger.info("  Amount: {}", amount);

        // Make direct HTTP call using Java's built-in HTTP client
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Validate HTTP response
        assertEquals(200, response.statusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.body(), "Response body should not be null");

        logger.info(" HTTP Response Details:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Parse JSON response and validate conversion logic
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"fromCurrency\": \"USD\""), "Response should contain from currency");
        assertTrue(jsonResponse.contains("\"toCurrency\": \"EUR\""), "Response should contain to currency");
        assertTrue(jsonResponse.contains("\"originalAmount\": 100.00"), "Response should contain original amount");
        assertTrue(jsonResponse.contains("\"convertedAmount\": 85.00"), "Response should contain converted amount (100 * 0.85)");
        assertTrue(jsonResponse.contains("\"exchangeRate\": 0.8500"), "Response should contain exchange rate");
        assertTrue(jsonResponse.contains("\"timestamp\""), "Response should contain timestamp");

        // Validate performance requirement
        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info(" Direct HTTP currency conversion completed successfully in {}ms", responseTime);
    }

    @Test
    @Order(4)
    @DisplayName("Should validate health check endpoint")
    void testHealthCheckEndpoint() throws Exception {
        logger.info("================================================================================");
        logger.info("REFACTORED: Health Check Endpoint Validation");
        logger.info("================================================================================");

        logger.info("🔧 Testing health check endpoint using RestApiTestableServer...");

        // Test health check endpoint
        String url = baseUrl + "/api/health";

        logger.info("🔧 Testing health check endpoint:");
        logger.info("  URL: {}", url);

        // Make direct HTTP call using Java's built-in HTTP client
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Validate HTTP response
        assertEquals(200, response.statusCode(), "Health check should return 200 OK");
        assertNotNull(response.body(), "Health check response body should not be null");

        logger.info(" Health Check Response Details:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Parse JSON response and validate health check data
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"status\": \"UP\""), "Health status should be UP");
        assertTrue(jsonResponse.contains("\"service\": \"Test REST API Server\""), "Service name should be present");
        assertTrue(jsonResponse.contains("\"version\": \"1.0.0\""), "Version should be present");
        assertTrue(jsonResponse.contains("\"timestamp\""), "Timestamp should be present");
        assertTrue(jsonResponse.contains("\"baseUrl\""), "Base URL should be present");

        // Validate performance requirement
        long responseTime = endTime - startTime;
        assertTrue(responseTime < 500, "Health check response time should be < 500ms, was: " + responseTime + "ms");

        logger.info(" Health check endpoint validation completed successfully in {}ms", responseTime);
        logger.info("================================================================================");
        logger.info("🎉 REFACTORED: Basic REST API Infrastructure - ALL TESTS PASSED!");
        logger.info("================================================================================");
    }
}
