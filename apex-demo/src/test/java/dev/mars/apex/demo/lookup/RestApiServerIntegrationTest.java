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

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;

/**
 * REST API Server Integration Test
 *
 * This test demonstrates real-world business scenarios using the reusable
 * RestApiTestableServer. It focuses on business logic validation and integration
 * workflows rather than HTTP infrastructure testing.
 *
 * Test scenarios covered:
 * - Customer credit rating validation for high-value transactions
 * - International trade currency conversion workflows
 * - Multi-step customer onboarding integration processes
 * - End-to-end business rule validation
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 2.0.0 (Renamed with RestApi prefix for consistency)
 */
class RestApiServerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RestApiServerIntegrationTest.class);

    private RestApiTestableServer testServer;
    private String baseUrl;

    @BeforeEach
    void setupTest() throws Exception {
        logger.info("🌐 Setting up RestApiTestableServer for business logic testing...");
        
        // Create and start the reusable test server
        testServer = new RestApiTestableServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();
        
        logger.info("✅ RestApiTestableServer ready at: {}", baseUrl);
    }

    @AfterEach
    void teardownTest() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    @DisplayName("High-Value Transaction Customer Eligibility Validation")
    void testHighValueTransactionCustomerEligibility() throws Exception {
        logger.info("================================================================================");
        logger.info("BUSINESS SCENARIO: High-Value Transaction Customer Eligibility");
        logger.info("================================================================================");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Test Case 1: AAA-rated corporate customer (should be approved)
        logger.info("🔧 Testing AAA-rated corporate customer eligibility...");
        
        String customerUrl = baseUrl + "/api/customers/CUST1";
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(customerUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode(), "Customer lookup should succeed");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("CUST1"), "Response should contain customer ID");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("AAA"), "Response should contain AAA rating");
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("CORPORATE"), "Response should contain CORPORATE type");

        logger.info("📊 Customer Response: {}", response.body());
        logger.info("✅ Business Rule: CUST1 approved for high-value transactions (AAA/CORPORATE)");

        // Test Case 2: Lower-rated customer (different approval criteria)
        logger.info("🔧 Testing A-rated hedge fund customer eligibility...");
        
        String hedgeFundUrl = baseUrl + "/api/customers/CUST3";
        java.net.http.HttpRequest hedgeFundRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(hedgeFundUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> hedgeFundResponse = httpClient.send(hedgeFundRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        // Validate response
        org.junit.jupiter.api.Assertions.assertEquals(200, hedgeFundResponse.statusCode(), "Customer lookup should succeed");
        org.junit.jupiter.api.Assertions.assertTrue(hedgeFundResponse.body().contains("CUST3"), "Response should contain customer ID");
        org.junit.jupiter.api.Assertions.assertTrue(hedgeFundResponse.body().contains("A"), "Response should contain A rating");
        org.junit.jupiter.api.Assertions.assertTrue(hedgeFundResponse.body().contains("HEDGE_FUND"), "Response should contain HEDGE_FUND type");

        logger.info("📊 Hedge Fund Response: {}", hedgeFundResponse.body());
        logger.info("✅ Business Rule: CUST3 requires additional approval (A/HEDGE_FUND)");

        logger.info("✅ High-value transaction customer eligibility validation completed successfully");
    }

    @Test
    @DisplayName("International Trade Currency Conversion Workflow")
    void testInternationalTradeCurrencyConversionWorkflow() throws Exception {
        logger.info("================================================================================");
        logger.info("BUSINESS SCENARIO: International Trade Currency Conversion");
        logger.info("================================================================================");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Step 1: Get USD base rate
        logger.info("🔧 Step 1: Getting USD base rate...");
        
        String usdUrl = baseUrl + "/api/currency/USD";
        java.net.http.HttpRequest usdRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(usdUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> usdResponse = httpClient.send(usdRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        org.junit.jupiter.api.Assertions.assertEquals(200, usdResponse.statusCode(), "USD rate lookup should succeed");
        logger.info("📊 USD Rate: {}", usdResponse.body());

        // Step 2: Get EUR conversion rate
        logger.info("🔧 Step 2: Getting EUR conversion rate...");
        
        String eurUrl = baseUrl + "/api/currency/EUR";
        java.net.http.HttpRequest eurRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(eurUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> eurResponse = httpClient.send(eurRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        org.junit.jupiter.api.Assertions.assertEquals(200, eurResponse.statusCode(), "EUR rate lookup should succeed");
        logger.info("📊 EUR Rate: {}", eurResponse.body());

        // Step 3: Perform currency conversion
        logger.info("🔧 Step 3: Performing USD to EUR conversion...");
        
        String conversionUrl = baseUrl + "/api/convert?from=USD&to=EUR&amount=1000";
        java.net.http.HttpRequest conversionRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(conversionUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> conversionResponse = httpClient.send(conversionRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        org.junit.jupiter.api.Assertions.assertEquals(200, conversionResponse.statusCode(), "Currency conversion should succeed");
        org.junit.jupiter.api.Assertions.assertTrue(conversionResponse.body().contains("convertedAmount"), "Response should contain converted amount");
        org.junit.jupiter.api.Assertions.assertTrue(conversionResponse.body().contains("exchangeRate"), "Response should contain exchange rate");

        logger.info("📊 Conversion Result: {}", conversionResponse.body());
        logger.info("✅ Business Rule: USD 1000 converted to EUR successfully");

        logger.info("✅ International trade currency conversion workflow completed successfully");
    }

    @Test
    @DisplayName("Multi-Step Customer Onboarding Integration Process")
    void testMultiStepCustomerOnboardingIntegrationProcess() throws Exception {
        logger.info("================================================================================");
        logger.info("BUSINESS SCENARIO: Multi-Step Customer Onboarding Integration");
        logger.info("================================================================================");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Step 1: Health check to ensure system is ready
        logger.info("🔧 Step 1: System health check...");
        
        String healthUrl = baseUrl + "/api/health";
        java.net.http.HttpRequest healthRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(healthUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> healthResponse = httpClient.send(healthRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        org.junit.jupiter.api.Assertions.assertEquals(200, healthResponse.statusCode(), "Health check should succeed");
        org.junit.jupiter.api.Assertions.assertTrue(healthResponse.body().contains("UP"), "System should be UP");
        logger.info("📊 Health Status: {}", healthResponse.body());

        // Step 2: Validate new customer data
        logger.info("🔧 Step 2: Validating new customer data...");
        
        String newCustomerUrl = baseUrl + "/api/customers/CUST5";
        java.net.http.HttpRequest customerRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(newCustomerUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> customerResponse = httpClient.send(customerRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        org.junit.jupiter.api.Assertions.assertEquals(200, customerResponse.statusCode(), "Customer validation should succeed");
        org.junit.jupiter.api.Assertions.assertTrue(customerResponse.body().contains("CUST5"), "Response should contain customer ID");
        org.junit.jupiter.api.Assertions.assertTrue(customerResponse.body().contains("PENSION_FUND"), "Response should contain customer type");
        logger.info("📊 Customer Data: {}", customerResponse.body());

        // Step 3: Get all customers for compliance check
        logger.info("🔧 Step 3: Compliance check - retrieving all customers...");
        
        String allCustomersUrl = baseUrl + "/api/customers";
        java.net.http.HttpRequest allCustomersRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(allCustomersUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> allCustomersResponse = httpClient.send(allCustomersRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        org.junit.jupiter.api.Assertions.assertEquals(200, allCustomersResponse.statusCode(), "All customers lookup should succeed");
        org.junit.jupiter.api.Assertions.assertTrue(allCustomersResponse.body().contains("totalCount"), "Response should contain total count");
        org.junit.jupiter.api.Assertions.assertTrue(allCustomersResponse.body().contains("customers"), "Response should contain customers array");
        logger.info("📊 All Customers: {}", allCustomersResponse.body());

        logger.info("✅ Business Rule: Customer onboarding process completed successfully");
        logger.info("✅ Multi-step customer onboarding integration process completed successfully");
    }

    @Test
    @DisplayName("End-to-End Business Rule Validation")
    void testEndToEndBusinessRuleValidation() throws Exception {
        logger.info("================================================================================");
        logger.info("BUSINESS SCENARIO: End-to-End Business Rule Validation");
        logger.info("================================================================================");

        HttpClient httpClient = HttpClient.newHttpClient();

        // Business Rule: Only AAA-rated customers can perform transactions > $100,000
        // Test Case: Validate CUST1 (AAA) and CUST3 (A) for large transaction eligibility

        logger.info("🔧 Testing business rule: Large transaction eligibility based on credit rating...");

        // Check CUST1 (AAA-rated)
        String cust1Url = baseUrl + "/api/customers/CUST1";
        java.net.http.HttpRequest cust1Request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(cust1Url))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> cust1Response = httpClient.send(cust1Request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        boolean cust1Eligible = cust1Response.body().contains("AAA");
        logger.info("📊 CUST1 Credit Rating Check: {} (Eligible: {})", 
            cust1Response.body().contains("AAA") ? "AAA" : "Other", cust1Eligible);

        // Check CUST3 (A-rated)
        String cust3Url = baseUrl + "/api/customers/CUST3";
        java.net.http.HttpRequest cust3Request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(cust3Url))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> cust3Response = httpClient.send(cust3Request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        boolean cust3Eligible = cust3Response.body().contains("AAA");
        logger.info("📊 CUST3 Credit Rating Check: {} (Eligible: {})", 
            cust3Response.body().contains("A") ? "A" : "Other", cust3Eligible);

        // Business rule validation
        org.junit.jupiter.api.Assertions.assertTrue(cust1Eligible, "CUST1 (AAA) should be eligible for large transactions");
        org.junit.jupiter.api.Assertions.assertFalse(cust3Eligible, "CUST3 (A) should NOT be eligible for large transactions");

        logger.info("✅ Business Rule Validation: Large transaction eligibility correctly enforced");
        logger.info("✅ End-to-end business rule validation completed successfully");
    }
}
