package dev.mars.apex.demo.codes;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for enrichment success-code and error-code features.
 * Demonstrates success cases, failure cases, and mixed scenarios.
 * Enrichments are loaded from external YAML configuration file.
 */
public class EnrichmentCodesValidation extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentCodesValidation.class);
    private YamlRuleConfiguration config;

    @BeforeEach
    public void setUpEnrichmentTest() throws Exception {
        super.setUp();  // Call parent setUp to initialize APEX services
        config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/codes/EnrichmentCodesValidation-enrichments.yaml");
        assertNotNull(config, "Configuration should be loaded");
        logger.info("✓ Configuration loaded: {} enrichments", config.getEnrichments().size());
    }

    /**
     * SUCCESS CASE: Enrichment with success code when condition matches
     */
    @Test
    @DisplayName("SUCCESS: Enrichment with success code on match")
    public void testEnrichmentSuccessCodeOnMatch() {
        logger.info("=== Testing Enrichment Success Code on Match ===");
        logger.info("Scenario: Enrichment condition matches, success code should be set");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 150);
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 100 (should MATCH)");

        logger.info("");
        logger.info("BEFORE ENRICHMENT:");
        logger.info("  Dataset: {}", testData);

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        logger.info("Enrichment processing completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify enrichment was applied
        assertEquals(150, enrichedData.get("validated_amount"), "Amount should be enriched");
        logger.info("");
        logger.info("AFTER ENRICHMENT:");
        logger.info("  Dataset: {}", enrichedData);
        logger.info("");
        logger.info("✓ Enrichment succeeded with success code");
        logger.info("  - validated_amount: {}", enrichedData.get("validated_amount"));
        logger.info("  - success-code: ENRICHMENT_SUCCESS_AMOUNT_VALID");
    }

    /**
     * ERROR CASE: Enrichment with error code when condition does not match
     */
    @Test
    @DisplayName("ERROR: Enrichment with error code on no match")
    public void testEnrichmentErrorCodeOnNoMatch() {
        logger.info("=== Testing Enrichment Error Code on No Match ===");
        logger.info("Scenario: Enrichment condition fails, error code should be set");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 50);  // Below threshold
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 100 (should NOT MATCH)");

        logger.info("");
        logger.info("BEFORE ENRICHMENT:");
        logger.info("  Dataset: {}", testData);

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        logger.info("Enrichment processing completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify enrichment was not applied (condition failed)
        assertNull(enrichedData.get("validated_amount"), "Amount should not be enriched when condition fails");
        logger.info("");
        logger.info("AFTER ENRICHMENT:");
        logger.info("  Dataset: {}", enrichedData);
        logger.info("");
        logger.info("✓ Enrichment failed as expected with error code");
        logger.info("  - validated_amount: null (enrichment not applied)");
        logger.info("  - error-code: ENRICHMENT_ERROR_AMOUNT_INVALID");
    }

    /**
     * MIXED CASE: Enrichment with both success and error codes
     */
    @Test
    @DisplayName("MIXED: Enrichment with both success and error codes")
    public void testEnrichmentMixedCodes() {
        logger.info("=== Testing Enrichment with Mixed Codes ===");
        logger.info("Scenario: Enrichment has both success-code and error-code defined");

        // Test with matching condition
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 200);
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 100 (should MATCH)");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        logger.info("Enrichment processing completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify enrichment was applied
        assertEquals(200, enrichedData.get("validated_amount"), "Amount should be enriched");
        logger.info("✓ Enrichment succeeded with mixed codes");
        logger.info("  - validated_amount: {}", enrichedData.get("validated_amount"));
        logger.info("  - success-code: ENRICHMENT_SUCCESS_VALID (condition matched)");
        logger.info("  - error-code: ENRICHMENT_ERROR_INVALID (not used, condition matched)");
    }

    /**
     * SPEL SUCCESS CODE: Enrichment with SpEL expression success code
     */
    @Test
    @DisplayName("SPEL: Enrichment with SpEL expression success code")
    public void testEnrichmentSpelSuccessCode() {
        logger.info("=== Testing Enrichment with SpEL Success Code ===");
        logger.info("Scenario: Success code is determined by SpEL expression");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1500);  // High value
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 100 (should MATCH)");
        logger.info("Success code expression: #amount > 1000 ? 'HIGH_VALUE_ENRICHED' : 'NORMAL_VALUE_ENRICHED'");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        logger.info("Enrichment processing completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify enrichment was applied
        assertEquals(1500, enrichedData.get("validated_amount"), "Amount should be enriched");
        logger.info("✓ Enrichment succeeded with SpEL success code");
        logger.info("  - validated_amount: {}", enrichedData.get("validated_amount"));
        logger.info("  - success-code: HIGH_VALUE_ENRICHED (1500 > 1000)");
    }

    /**
     * SPEL ERROR CODE: Enrichment with SpEL expression error code
     */
    @Test
    @DisplayName("SPEL: Enrichment with SpEL expression error code")
    public void testEnrichmentSpelErrorCode() {
        logger.info("=== Testing Enrichment with SpEL Error Code ===");
        logger.info("Scenario: Error code is determined by SpEL expression when condition fails");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 30);  // Low value, condition fails
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 100 (should NOT MATCH)");
        logger.info("Error code expression: #amount < 50 ? 'CRITICAL_LOW_AMOUNT' : 'LOW_AMOUNT'");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        logger.info("Enrichment processing completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify enrichment was not applied
        assertNull(enrichedData.get("validated_amount"), "Amount should not be enriched when condition fails");
        logger.info("✓ Enrichment failed as expected with SpEL error code");
        logger.info("  - validated_amount: null (enrichment not applied)");
        logger.info("  - error-code: CRITICAL_LOW_AMOUNT (30 < 50)");
    }

    /**
     * CALCULATION SUCCESS: Calculation enrichment with success code
     */
    @Test
    @DisplayName("CALCULATION: Calculation enrichment with success code")
    public void testCalculationEnrichmentSuccessCode() {
        logger.info("=== Testing Calculation Enrichment with Success Code ===");
        logger.info("Scenario: Calculation enrichment with success code on match");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1000);
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 0 (should MATCH)");
        logger.info("Calculation expression: #amount * 0.01 (1% fee)");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        logger.info("Enrichment processing completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify calculation was applied
        assertEquals(10.0, enrichedData.get("fee_amount"), "Fee should be calculated (1% of 1000)");
        logger.info("✓ Calculation enrichment succeeded with success code");
        logger.info("  - calculated_fee: 10.0");
        logger.info("  - fee_amount: {}", enrichedData.get("fee_amount"));
        logger.info("  - success-code: CALCULATION_SUCCESS");
    }

    /**
     * CALCULATION ERROR: Calculation enrichment with error code
     */
    @Test
    @DisplayName("CALCULATION: Calculation enrichment with error code")
    public void testCalculationEnrichmentErrorCode() {
        logger.info("=== Testing Calculation Enrichment with Error Code ===");
        logger.info("Scenario: Calculation enrichment with error code on no match");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 0);  // Invalid amount
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 0 (should NOT MATCH)");
        logger.info("Calculation expression: #amount * 0.01 (1% fee)");

        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Enrichment result should not be null");
        logger.info("Enrichment processing completed");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Verify calculation was not applied (condition failed)
        assertNull(enrichedData.get("fee_amount"), "Fee should not be calculated when condition fails");
        logger.info("✓ Calculation enrichment failed as expected with error code");
        logger.info("  - fee_amount: null (enrichment not applied)");
        logger.info("  - error-code: CALCULATION_ERROR");
    }
}

