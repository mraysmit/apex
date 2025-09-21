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
 * Business Logic Integration Test
 *
 * This test demonstrates real-world business scenarios using the reusable
 * TestableRestApiServer. It focuses on business logic validation and integration
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
 * @version 1.0.0
 */
class TestableRestApiServerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(TestableRestApiServerIntegrationTest.class);

    private TestableRestApiServer testServer;
    private String baseUrl;

    @BeforeEach
    void setupTest() throws Exception {
        logger.info("🌐 Setting up TestableRestApiServer for business logic testing...");
        
        // Create and start the reusable test server
        testServer = new TestableRestApiServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();
        
        logger.info("✅ TestableRestApiServer ready at: {}", baseUrl);
    }

    @AfterEach
    void teardownTest() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    @DisplayName("Should validate customer credit rating for high-value transactions")
    void testHighValueTransactionCreditValidation() throws Exception {
        logger.info("================================================================================");
        logger.info("BUSINESS SCENARIO: High-Value Transaction Credit Validation");
        logger.info("================================================================================");

        // Business scenario: Validate customer eligibility for high-value transactions
        // based on credit rating and customer type

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

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

        // Business rule validation
        assertEquals(200, response.statusCode(), "Customer lookup should succeed");
        String customerJson = response.body();
        
        // Validate high-value transaction eligibility criteria
        assertTrue(customerJson.contains("\"creditRating\": \"AAA\""), 
            "Customer must have AAA credit rating for high-value transactions");
        assertTrue(customerJson.contains("\"customerType\": \"CORPORATE\""), 
            "Corporate customers are preferred for high-value transactions");
        assertTrue(customerJson.contains("\"customerName\": \"Acme Corporation\""), 
            "Customer name should be properly retrieved");

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

        assertEquals(200, hedgeFundResponse.statusCode(), "Hedge fund customer lookup should succeed");
        String hedgeFundJson = hedgeFundResponse.body();
        
        // Validate hedge fund specific criteria
        assertTrue(hedgeFundJson.contains("\"creditRating\": \"A\""), 
            "Hedge fund customer has A rating (requires additional validation)");
        assertTrue(hedgeFundJson.contains("\"customerType\": \"HEDGE_FUND\""), 
            "Hedge fund customers have different risk profiles");

        logger.info("✅ Business Rule: CUST3 requires additional validation (A/HEDGE_FUND)");
        logger.info("✅ High-value transaction credit validation completed successfully");
        logger.info("================================================================================");
    }

    @Test
    @DisplayName("Should validate international trade currency conversion workflow")
    void testInternationalTradeCurrencyWorkflow() throws Exception {
        logger.info("================================================================================");
        logger.info("BUSINESS SCENARIO: International Trade Currency Conversion");
        logger.info("================================================================================");

        // Business scenario: Multi-currency trade settlement workflow
        // 1. Validate customer eligibility
        // 2. Get current exchange rates
        // 3. Calculate conversion amounts
        // 4. Verify system readiness

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

        // Step 1: Validate international customer
        logger.info("🔧 Step 1: Validating international customer (Global Trading Ltd)...");
        
        String customerUrl = baseUrl + "/api/customers/CUST2";
        java.net.http.HttpRequest customerRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(customerUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> customerResponse = httpClient.send(customerRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, customerResponse.statusCode(), "International customer lookup should succeed");
        String customerJson = customerResponse.body();
        assertTrue(customerJson.contains("\"customerName\": \"Global Trading Ltd\""), 
            "Should be international trading customer");
        assertTrue(customerJson.contains("\"customerType\": \"INSTITUTIONAL\""), 
            "Institutional customers handle international trades");

        logger.info("✅ Step 1: International customer validated");

        // Step 2: Get current EUR exchange rate
        logger.info("🔧 Step 2: Retrieving EUR exchange rate for trade settlement...");
        
        String currencyUrl = baseUrl + "/api/currency/EUR";
        java.net.http.HttpRequest currencyRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(currencyUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> currencyResponse = httpClient.send(currencyRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, currencyResponse.statusCode(), "EUR rate lookup should succeed");
        String currencyJson = currencyResponse.body();
        assertTrue(currencyJson.contains("\"rate\": 0.85"), "EUR rate should be current");
        assertTrue(currencyJson.contains("\"symbol\": \"€\""), "EUR symbol should be present");

        logger.info("✅ Step 2: EUR exchange rate retrieved (0.85)");

        // Step 3: Calculate trade conversion (50,000 USD to EUR)
        logger.info("🔧 Step 3: Calculating trade conversion (50,000 USD → EUR)...");
        
        String conversionUrl = baseUrl + "/api/convert?from=USD&to=EUR&amount=50000.00";
        java.net.http.HttpRequest conversionRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(conversionUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> conversionResponse = httpClient.send(conversionRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, conversionResponse.statusCode(), "Trade conversion should succeed");
        String conversionJson = conversionResponse.body();
        assertTrue(conversionJson.contains("\"convertedAmount\": 42500.00"), 
            "50,000 USD should convert to 42,500 EUR");
        assertTrue(conversionJson.contains("\"exchangeRate\": 0.8500"), 
            "Exchange rate should be properly applied");

        logger.info("✅ Step 3: Trade conversion calculated (50,000 USD → 42,500 EUR)");

        // Step 4: Verify system health for trade execution
        logger.info("🔧 Step 4: Verifying system health for trade execution...");
        
        String healthUrl = baseUrl + "/api/health";
        java.net.http.HttpRequest healthRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(healthUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> healthResponse = httpClient.send(healthRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, healthResponse.statusCode(), "System health check should succeed");
        assertTrue(healthResponse.body().contains("\"status\": \"UP\""), "System should be ready for trades");

        logger.info("✅ Step 4: System health confirmed for trade execution");

        // Business workflow validation
        logger.info("✅ International Trade Workflow Summary:");
        logger.info("  - Customer: Global Trading Ltd (INSTITUTIONAL, AA+)");
        logger.info("  - Trade Amount: 50,000 USD → 42,500 EUR");
        logger.info("  - Exchange Rate: 0.8500 (EUR/USD)");
        logger.info("  - System Status: UP and ready for execution");

        logger.info("✅ International trade currency workflow completed successfully");
        logger.info("================================================================================");
    }

    @Test
    @DisplayName("Should validate multi-step customer onboarding integration")
    void testCustomerOnboardingIntegration() throws Exception {
        logger.info("================================================================================");
        logger.info("BUSINESS SCENARIO: Customer Onboarding Integration Process");
        logger.info("================================================================================");

        // Business scenario: New customer onboarding with multiple validation steps
        // This demonstrates how multiple REST endpoints work together in a business process

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

        // Onboarding Step 1: Verify customer record exists and is complete
        logger.info("🔧 Onboarding Step 1: Customer record verification...");
        
        String customerUrl = baseUrl + "/api/customers/CUST5";
        java.net.http.HttpRequest customerRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(customerUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> customerResponse = httpClient.send(customerRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, customerResponse.statusCode(), "Customer record should exist");
        String customerJson = customerResponse.body();
        
        // Validate all required customer fields are present
        assertTrue(customerJson.contains("\"customerId\": \"CUST5\""), "Customer ID required");
        assertTrue(customerJson.contains("\"customerName\": \"Pension Fund Alliance\""), "Customer name required");
        assertTrue(customerJson.contains("\"customerType\": \"PENSION_FUND\""), "Customer type required");
        assertTrue(customerJson.contains("\"creditRating\": \"AAA\""), "Credit rating required");
        assertTrue(customerJson.contains("\"registrationDate\""), "Registration date required");

        logger.info("✅ Onboarding Step 1: Customer record complete (Pension Fund Alliance, AAA)");

        // Onboarding Step 2: Validate base currency support
        logger.info("🔧 Onboarding Step 2: Base currency support validation...");
        
        String currencyUrl = baseUrl + "/api/currency/USD";
        java.net.http.HttpRequest currencyRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(currencyUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> currencyResponse = httpClient.send(currencyRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, currencyResponse.statusCode(), "Base currency should be supported");
        assertTrue(currencyResponse.body().contains("\"code\": \"USD\""), "USD should be base currency");

        logger.info("✅ Onboarding Step 2: USD base currency supported");

        // Onboarding Step 3: System readiness check
        logger.info("🔧 Onboarding Step 3: System readiness verification...");
        
        String healthUrl = baseUrl + "/api/health";
        java.net.http.HttpRequest healthRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(healthUrl))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> healthResponse = httpClient.send(healthRequest,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, healthResponse.statusCode(), "System should be ready for onboarding");
        assertTrue(healthResponse.body().contains("\"status\": \"UP\""), "System should be operational");

        logger.info("✅ Onboarding Step 3: System ready for new customer operations");

        // Integration validation: Complete onboarding workflow
        logger.info("✅ Customer Onboarding Integration Summary:");
        logger.info("  - Customer: Pension Fund Alliance (CUST5)");
        logger.info("  - Type: PENSION_FUND with AAA credit rating");
        logger.info("  - Base Currency: USD supported");
        logger.info("  - System Status: UP and ready for operations");
        logger.info("  - Onboarding Status: APPROVED for activation");

        logger.info("✅ Multi-step customer onboarding integration completed successfully");
        logger.info("================================================================================");
    }
}
