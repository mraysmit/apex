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

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Delay Test
 *
 * This test class specifically tests the configurable delay functionality
 * of the RestApiTestableServer. It validates that:
 * - Default delay is 0 seconds (no delay)
 * - Configurable delay works correctly
 * - All endpoints respect the delay setting
 * - Timing assertions validate actual delay behavior
 *
 * Key Features Tested:
 * - Default constructor (0 second delay)
 * - Parameterized constructor with delay
 * - Timing validation for all endpoints
 * - Health check endpoint includes delay information
 *
 * @author APEX Demo Team
 * @since 2025-09-21
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestApiDelayTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RestApiDelayTest.class);

    private RestApiTestableServer testServer;
    private String baseUrl;
    private HttpClient httpClient;

    @BeforeEach
    void setupDelayTest() throws Exception {
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void teardownDelayTest() {
        if (testServer != null && testServer.isRunning()) {
            testServer.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should have no delay with default constructor")
    void testDefaultConstructorNoDelay() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Default Constructor (No Delay)");
        logger.info("================================================================================");

        // Create server with default constructor (no delay)
        testServer = new RestApiTestableServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        // Validate delay configuration
        assertEquals(0, testServer.getResponseDelaySeconds(), "Default delay should be 0 seconds");

        // Test currency endpoint with timing
        String url = baseUrl + "/api/currency/USD";
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Validate response
        assertEquals(200, response.statusCode(), "Should return 200 OK");
        assertTrue(response.body().contains("\"code\": \"USD\""), "Should return USD currency data");

        // Validate timing - should be very fast (< 200ms for no delay, allowing for system overhead)
        assertTrue(responseTime < 200, "Response time should be < 200ms with no delay, was: " + responseTime + "ms");

        logger.info(" Default constructor test completed - Response time: {}ms", responseTime);
    }

    @Test
    @Order(2)
    @DisplayName("Should apply 2-second delay to currency endpoint")
    void testCurrencyEndpointWithDelay() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Currency Endpoint with 2-Second Delay");
        logger.info("================================================================================");

        // Create server with 2-second delay
        testServer = new RestApiTestableServer(2);
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        // Validate delay configuration
        assertEquals(2, testServer.getResponseDelaySeconds(), "Delay should be 2 seconds");

        // Test currency endpoint with timing
        String url = baseUrl + "/api/currency/EUR";
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Validate response
        assertEquals(200, response.statusCode(), "Should return 200 OK");
        assertTrue(response.body().contains("\"code\": \"EUR\""), "Should return EUR currency data");

        // Validate timing - should be at least 2000ms (2 seconds)
        assertTrue(responseTime >= 2000, "Response time should be >= 2000ms with 2-second delay, was: " + responseTime + "ms");
        assertTrue(responseTime < 2500, "Response time should be < 2500ms (allowing for processing overhead), was: " + responseTime + "ms");

        logger.info(" Currency endpoint delay test completed - Response time: {}ms", responseTime);
    }

    @Test
    @Order(3)
    @DisplayName("Should apply 1-second delay to conversion endpoint")
    void testConversionEndpointWithDelay() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Conversion Endpoint with 1-Second Delay");
        logger.info("================================================================================");

        // Create server with 1-second delay
        testServer = new RestApiTestableServer(1);
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        // Validate delay configuration
        assertEquals(1, testServer.getResponseDelaySeconds(), "Delay should be 1 second");

        // Test conversion endpoint with timing
        String url = baseUrl + "/api/convert?from=USD&to=EUR&amount=100";
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Validate response
        assertEquals(200, response.statusCode(), "Should return 200 OK");
        assertTrue(response.body().contains("\"fromCurrency\": \"USD\""), "Should return conversion data");

        // Validate timing - should be at least 1000ms (1 second)
        assertTrue(responseTime >= 1000, "Response time should be >= 1000ms with 1-second delay, was: " + responseTime + "ms");
        assertTrue(responseTime < 1500, "Response time should be < 1500ms (allowing for processing overhead), was: " + responseTime + "ms");

        logger.info(" Conversion endpoint delay test completed - Response time: {}ms", responseTime);
    }

    @Test
    @Order(4)
    @DisplayName("Should apply delay to customer endpoint")
    void testCustomerEndpointWithDelay() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Customer Endpoint with 1-Second Delay");
        logger.info("================================================================================");

        // Reuse server from previous test (1-second delay)
        if (testServer == null || !testServer.isRunning()) {
            testServer = new RestApiTestableServer(1);
            testServer.start();
            baseUrl = testServer.getBaseUrl();
        }

        // Test customer endpoint with timing
        String url = baseUrl + "/api/customers/CUST1";
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Validate response
        assertEquals(200, response.statusCode(), "Should return 200 OK");
        assertTrue(response.body().contains("\"customerId\": \"CUST1\""), "Should return customer data");

        // Validate timing - should be at least 1000ms (1 second)
        assertTrue(responseTime >= 1000, "Response time should be >= 1000ms with 1-second delay, was: " + responseTime + "ms");
        assertTrue(responseTime < 1500, "Response time should be < 1500ms (allowing for processing overhead), was: " + responseTime + "ms");

        logger.info(" Customer endpoint delay test completed - Response time: {}ms", responseTime);
    }

    @Test
    @Order(5)
    @DisplayName("Should apply delay to health check and include delay info")
    void testHealthCheckWithDelayInfo() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Health Check with Delay Information");
        logger.info("================================================================================");

        // Create server with 1-second delay
        testServer = new RestApiTestableServer(1);
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        // Test health check endpoint with timing
        String url = baseUrl + "/api/health";
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Validate response
        assertEquals(200, response.statusCode(), "Should return 200 OK");
        String responseBody = response.body();
        assertTrue(responseBody.contains("\"status\": \"UP\""), "Should return UP status");
        assertTrue(responseBody.contains("\"responseDelaySeconds\": 1"), "Should include delay configuration");

        // Validate timing - should be at least 1000ms (1 second)
        assertTrue(responseTime >= 1000, "Response time should be >= 1000ms with 1-second delay, was: " + responseTime + "ms");
        assertTrue(responseTime < 1500, "Response time should be < 1500ms (allowing for processing overhead), was: " + responseTime + "ms");

        logger.info(" Health check delay test completed - Response time: {}ms", responseTime);
        logger.info(" Health check response includes delay info: {}", responseBody.contains("\"responseDelaySeconds\": 1"));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle negative delay values gracefully")
    void testNegativeDelayHandling() throws Exception {
        logger.info("================================================================================");
        logger.info("Testing Negative Delay Value Handling");
        logger.info("================================================================================");

        // Create server with negative delay (should be treated as 0)
        testServer = new RestApiTestableServer(-5);
        testServer.start();
        baseUrl = testServer.getBaseUrl();

        // Validate delay configuration - negative values should be treated as 0
        assertEquals(0, testServer.getResponseDelaySeconds(), "Negative delay should be treated as 0 seconds");

        // Test endpoint with timing
        String url = baseUrl + "/api/health";
        long startTime = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Validate response
        assertEquals(200, response.statusCode(), "Should return 200 OK");
        assertTrue(response.body().contains("\"responseDelaySeconds\": 0"), "Should show 0 delay in response");

        // Validate timing - should be fast (< 100ms for no delay)
        assertTrue(responseTime < 100, "Response time should be < 100ms with negative delay treated as 0, was: " + responseTime + "ms");

        logger.info(" Negative delay handling test completed - Response time: {}ms", responseTime);
        logger.info("================================================================================");
        logger.info("REST API Delay Feature - ALL TESTS PASSED!");
        logger.info("================================================================================");
    }
}
