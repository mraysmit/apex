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

import dev.mars.apex.demo.infrastructure.DemoTestBase;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlDataSourceLoader;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
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
import java.util.List;
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
 * - Uses RestApiTestableServer for realistic REST API integration
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

    private RestApiTestableServer testServer;
    private String baseUrl;
    private int serverPort;
    private YamlRulesEngineService rulesEngineService;
    private YamlConfigurationLoader yamlLoader;
    private YamlDataSourceLoader dataSourceLoader;
    private EnrichmentService enrichmentService;

    @BeforeEach
    void setupRestApiServer() throws IOException {
        logger.info("🌐 Setting up RestApiTestableServer for REST API integration tests...");

        // Initialize APEX services directly
        yamlLoader = new YamlConfigurationLoader();
        dataSourceLoader = new YamlDataSourceLoader();
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        // Initialize YamlRulesEngineService
        rulesEngineService = new YamlRulesEngineService();

        // Create and start the reusable test server
        testServer = new RestApiTestableServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();
        serverPort = extractPortFromUrl(baseUrl);

        logger.info(" RestApiTestableServer ready at: {}", baseUrl);
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
    @DisplayName("Test 1: Currency Code Format Validation - Using APEX Rules")
    void testCurrencyCodeFormatValidation() throws Exception {
        logger.info("=== Test 1: Currency Code Format Validation ===");

        // Load YAML configuration with dynamic port substitution
        String tempYamlPath = updateYamlWithServerPort("src/test/java/dev/mars/apex/demo/lookup/CurrencyCodeValidationTest.yaml");
        var config = yamlLoader.loadFromFile(tempYamlPath);
        assertNotNull(config, "YAML configuration should not be null");

        // Create RulesEngine from configuration
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(engine, "RulesEngine should be created");

        // Test 1a: Valid currency code format
        logger.info("Testing valid currency code format...");
        Map<String, Object> validData = new HashMap<>();
        validData.put("transactionId", "TXN-001");
        validData.put("currencyCode", "EUR");  // Valid format (3 uppercase letters)
        validData.put("amount", 1000.00);

        // Execute currency code validation rule
        Rule currencyRule = engine.getConfiguration().getRuleById("currency-code-format-validation");
        assertNotNull(currencyRule, "Currency validation rule should be found");

        RuleResult validResult = engine.executeRulesList(List.of(currencyRule), validData);
        assertNotNull(validResult, "Rule result should not be null");
        assertTrue(validResult.isTriggered(), "Currency code validation should pass for 'EUR'");

        // Test 1b: Invalid currency code format
        logger.info("Testing invalid currency code format...");
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("transactionId", "TXN-002");
        invalidData.put("currencyCode", "euro");  // Invalid format (lowercase)
        invalidData.put("amount", 500.00);

        RuleResult invalidResult = engine.executeRulesList(List.of(currencyRule), invalidData);
        assertNotNull(invalidResult, "Rule result should not be null");
        assertFalse(invalidResult.isTriggered(), "Currency code validation should fail for 'euro'");

        // Test 1c: Test transaction amount validation
        logger.info("Testing transaction amount validation...");
        Rule amountRule = engine.getConfiguration().getRuleById("transaction-amount-validation");
        assertNotNull(amountRule, "Amount validation rule should be found");

        Map<String, Object> negativeAmountData = new HashMap<>();
        negativeAmountData.put("transactionId", "TXN-003");
        negativeAmountData.put("currencyCode", "USD");
        negativeAmountData.put("amount", -100.00);  // Invalid negative amount

        RuleResult amountResult = engine.executeRulesList(List.of(amountRule), negativeAmountData);
        assertNotNull(amountResult, "Rule result should not be null");
        assertFalse(amountResult.isTriggered(), "Amount validation should fail for negative amount");

        // Test 1d: Test transaction ID validation
        logger.info("Testing transaction ID validation...");
        Rule transactionRule = engine.getConfiguration().getRuleById("transaction-id-validation");
        assertNotNull(transactionRule, "Transaction ID validation rule should be found");

        Map<String, Object> invalidTransactionData = new HashMap<>();
        invalidTransactionData.put("transactionId", "INVALID");  // Invalid format
        invalidTransactionData.put("currencyCode", "USD");
        invalidTransactionData.put("amount", 100.00);

        RuleResult transactionResult = engine.executeRulesList(List.of(transactionRule), invalidTransactionData);
        assertNotNull(transactionResult, "Rule result should not be null");
        assertFalse(transactionResult.isTriggered(), "Transaction ID validation should fail for 'INVALID'");

        logger.info(" Currency Code Format Validation test completed successfully");

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

        // Initialize data sources from YAML configuration
        try {
            dataSourceLoader.loadDataSources(config);
            logger.info("Successfully loaded data sources from YAML configuration");
        } catch (Exception e) {
            logger.error("Failed to load data sources from YAML configuration", e);
            throw new RuntimeException("Failed to initialize data sources", e);
        }
        
        // Test 2a: Enrich blank customer name with known customer ID
        logger.info("Testing customer name enrichment for known customer...");
        Map<String, Object> enrichmentData = new HashMap<>();
        enrichmentData.put("orderId", "ORD-001");
        enrichmentData.put("customerId", "CUST1");
        enrichmentData.put("customerName", "");  // Blank - will be enriched
        enrichmentData.put("orderAmount", 2500.00);
        
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
        Map<String, Object> unknownCustomerData = new HashMap<>();
        unknownCustomerData.put("orderId", "ORD-002");
        unknownCustomerData.put("customerId", "UNKN1");  // Unknown customer
        unknownCustomerData.put("customerName", "");
        unknownCustomerData.put("orderAmount", 1000.00);

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
        Map<String, Object> existingNameData = new HashMap<>();
        existingNameData.put("orderId", "ORD-003");
        existingNameData.put("customerId", "CUST2");
        existingNameData.put("customerName", "Existing Customer Name");  // Not blank - should not be enriched
        existingNameData.put("orderAmount", 750.00);

        Object existingResult = enrichmentService.enrichObject(config, existingNameData);
        @SuppressWarnings("unchecked")
        Map<String, Object> existingEnriched = (Map<String, Object>) existingResult;

        // Should preserve existing customer name
        assertEquals("Existing Customer Name", existingEnriched.get("customerName"),
            "Should preserve existing customer name and not enrich");

        logger.info(" Customer Name Enrichment test completed successfully");

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
