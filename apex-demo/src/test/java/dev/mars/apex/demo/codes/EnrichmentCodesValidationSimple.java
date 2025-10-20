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
 * Simplified test for enrichment with success-code and map-to-field.
 * Demonstrates a single enrichment that maps the success code to a field in the dataset.
 * Enrichment is loaded from external YAML configuration file.
 */
public class EnrichmentCodesValidationSimple extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentCodesValidationSimple.class);
    private YamlRuleConfiguration config;

    @BeforeEach
    public void setUpEnrichmentTest() throws Exception {
        super.setUp();  // Call parent setUp to initialize APEX services
        config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/codes/EnrichmentCodesValidationSimple-enrichments.yaml");
        assertNotNull(config, "Configuration should be loaded");
        logger.info("✓ Configuration loaded: {} enrichments", config.getEnrichments().size());
    }

    /**
     * SUCCESS CASE: Enrichment with success code on match
     */
    @Test
    @DisplayName("SUCCESS: Enrichment with success code on match")
    public void testEnrichmentSuccessCodeMappedToField() {
        logger.info("=== Testing Enrichment Success Code on Match ===");
        logger.info("Scenario: Enrichment condition matches, success code is set");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 150);
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 100 (should MATCH)");
        logger.info("Success code: AMOUNT_VALID");

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
        logger.info("  - success-code: AMOUNT_VALID");
    }

    /**
     * ERROR CASE: Enrichment with error code on no match
     */
    @Test
    @DisplayName("ERROR: Enrichment with error code on no match")
    public void testEnrichmentErrorCodeMappedToField() {
        logger.info("=== Testing Enrichment Error Code on No Match ===");
        logger.info("Scenario: Enrichment condition fails, error code is set");

        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 50);  // Below threshold
        logger.info("Input data: amount={}", testData.get("amount"));
        logger.info("Enrichment condition: #amount > 100 (should NOT MATCH)");
        logger.info("Error code: AMOUNT_INVALID");

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
        logger.info("  - error-code: AMOUNT_INVALID");
    }
}

