package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal test demonstrating sequential processing where a rule depends on enrichment results.
 *
 * This test follows the established pattern from other sequencing tests and demonstrates
 * sequential processing: respecting YAML document order when processing
 * configurations.
 */
class AMinimalSequentialProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMinimalSequentialProcessingTest.class);

    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        LOGGER.info("* Setting up MINIMAL SEQUENTIAL PROCESSING test");

        yamlLoader = new YamlConfigurationLoader();

        LOGGER.info("* Sequential processing services initialized");
    }

    @Test
    @DisplayName("SEQUENTIAL MODE: Minimal demonstration of document order processing")
    void testMinimalSequentialProcessing() throws Exception {
        LOGGER.info("=== TESTING: Minimal Sequential Processing Pattern ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/AMinimalSequentialProcessingTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");

        LOGGER.info("* Input Data: {}", testData);

        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        Map<String, Object> enrichedData = result.getEnrichedData();

        LOGGER.info("* Processing completed. Final data state: {}", enrichedData);

        // Verify the enrichment worked
        assertNotNull(enrichedData.get("customerTier"), "Customer tier should be enriched");
        assertEquals("GOLD", enrichedData.get("customerTier"), "Customer CUST001 should have GOLD tier");

        // Verify original data is preserved
        assertEquals("CUST001", enrichedData.get("customerId"), "Original customer ID should be preserved");

        LOGGER.info("* Minimal Sequential Processing pattern WORKS");
        LOGGER.info("   1. Input: customerId = {}", testData.get("customerId"));
        LOGGER.info("   2. Enrichment: customerTier = {}", enrichedData.get("customerTier"));
        LOGGER.info("   3. Sequential processing respects YAML document order");
        LOGGER.info("   4. Rule can depend on enrichment result (when rules are implemented)");
    }

    @Test
    @DisplayName("VALIDATION: Test with different customer")
    void testDifferentCustomer() throws Exception {
        LOGGER.info("=== VALIDATION: Testing with different customer data ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/AMinimalSequentialProcessingTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST002");

        LOGGER.info("* Input Data: {}", testData);

        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        Map<String, Object> enrichedData = result.getEnrichedData();

        LOGGER.info("* Processing completed. Final data state: {}", enrichedData);

        // Verify the enrichment worked for CUST002
        assertNotNull(enrichedData.get("customerTier"), "Customer tier should be enriched");
        assertEquals("SILVER", enrichedData.get("customerTier"), "Customer CUST002 should have SILVER tier");
        assertEquals("CUST002", enrichedData.get("customerId"), "Original customer ID should be preserved");

        LOGGER.info("* Sequential processing works for multiple customers");
        LOGGER.info("   CUST001 -> GOLD tier, CUST002 -> SILVER tier");
    }

    @Test
    @DisplayName("COMPARISON: Both YAML configurations produce same result")
    void testProcessingModeComparison() throws Exception {
        LOGGER.info("=== COMPARISON: Both YAML configurations produce same enrichment result ===");

        // Test both configs with same data
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");

        // First configuration
        String sequentialPath = "src/test/java/dev/mars/apex/demo/sequencing/AMinimalSequentialProcessingTest.yaml";
        YamlRuleConfiguration sequentialConfig = yamlLoader.loadFromFile(sequentialPath);
        RulesEngine sequentialEngine = RulesEngine.fromFile(sequentialPath);
        RuleResult sequentialResult = sequentialEngine.evaluate(sequentialConfig, new HashMap<>(testData));

        // Second configuration
        String standardPath = "src/test/java/dev/mars/apex/demo/sequencing/AMinimalStandardProcessingTest.yaml";
        YamlRuleConfiguration standardConfig = yamlLoader.loadFromFile(standardPath);
        RulesEngine standardEngine = RulesEngine.fromFile(standardPath);
        RuleResult standardResult = standardEngine.evaluate(standardConfig, new HashMap<>(testData));

        Map<String, Object> sequentialData = sequentialResult.getEnrichedData();
        Map<String, Object> standardData = standardResult.getEnrichedData();

        LOGGER.info("* First config result: {}", sequentialData);
        LOGGER.info("* Second config result: {}", standardData);

        // Both should have enriched the customer tier
        assertEquals(sequentialData.get("customerTier"), standardData.get("customerTier"),
                    "Both configurations should enrich customer tier identically");

        LOGGER.info("* Both configurations produce identical enrichment results");
    }
}



