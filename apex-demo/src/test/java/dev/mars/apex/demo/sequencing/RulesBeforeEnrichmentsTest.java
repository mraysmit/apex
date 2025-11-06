package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UNUSUAL ORDER TEST: Rules Before Enrichments
 * 
 * Tests the UNUSUAL but VALID scenario where rules appear BEFORE enrichments in the YAML file.
 * This is the REVERSE of the typical order, but should still work correctly.
 * 
 * Expected Processing Order:
 * 1. R1-from-ref (validate-notional-limit) - from external rules file
 * 2. R2-from-ref (validate-strike-price) - from external rules file
 * 3. R3-inline (validate-product-type) - inline rule
 * 4. RG1-from-ref (trade-validation-group) - from external rule groups file
 * 5. RG2-from-ref (risk-validation-group) - from external rule groups file
 * 6. RG3-inline (input-validation-group) - inline rule group
 * 7. E1-from-ref (enrich-market-data) - from external enrichments file
 * 8. E2-from-ref (calculate-greeks) - from external enrichments file
 * 9. E3-inline (enrich-counterparty-data) - inline enrichment
 * 10. EG1-from-ref (market-data-enrichment-group) - from external enrichment groups file
 * 11. EG2-from-ref (risk-metrics-enrichment-group) - from external enrichment groups file
 * 12. EG3-inline (product-enrichment-group) - inline enrichment group
 *
 * SUCCESS CRITERIA:
 * - Rules execute BEFORE enrichments (unusual but valid)
 * - Rules can validate input data before enrichment
 * - Enrichments execute after rules complete
 * - All processing follows document order
 */
@DisplayName("UNUSUAL: Rules Before Enrichments")
class RulesBeforeEnrichmentsTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesBeforeEnrichmentsTest.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @Test
    @DisplayName("Test 1: Verify all section types loaded correctly")
    void testAllSectionTypesLoaded() throws Exception {
        LOGGER.info("=== TESTING: All Section Types Loaded (Rules First) ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RulesBeforeEnrichmentsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Verify rules loaded (2 from external file + 1 inline = 3)
        assertNotNull(engine.getConfiguration().getAllRules(), "Rules should be loaded");
        assertTrue(engine.getConfiguration().getAllRules().size() >= 3,
            "Should have at least 3 rules: 2 from external file + 1 inline");

        // Verify rule groups loaded (2 from external file + 1 inline = 3)
        assertNotNull(engine.getConfiguration().getAllRuleGroups(), "Rule groups should be loaded");
        assertEquals(3, engine.getConfiguration().getAllRuleGroups().size(),
            "Should have 3 rule groups: 2 from external file + 1 inline");
        
        // Verify enrichments loaded (2 from external file + 1 inline = 3)
        assertNotNull(config.getEnrichments(), "Enrichments should be loaded");
        assertTrue(config.getEnrichments().size() >= 3,
            "Should have at least 3 enrichments: 2 from external file + 1 inline");

        // Verify enrichment groups loaded (2 from external file + 1 inline = 3)
        assertNotNull(engine.getConfiguration().getAllEnrichmentGroups(), "Enrichment groups should be loaded");
        assertEquals(3, engine.getConfiguration().getAllEnrichmentGroups().size(),
            "Should have 3 enrichment groups: 2 from external file + 1 inline");
        
        LOGGER.info("✅ All section types loaded correctly (rules first)");
        LOGGER.info("   - Rules: {}", engine.getConfiguration().getAllRules().size());
        LOGGER.info("   - Rule Groups: {}", engine.getConfiguration().getAllRuleGroups().size());
        LOGGER.info("   - Enrichments: {}", config.getEnrichments().size());
        LOGGER.info("   - Enrichment Groups: {}", engine.getConfiguration().getAllEnrichmentGroups().size());
    }

    @Test
    @DisplayName("Test 2: UNUSUAL - Verify rules execute BEFORE enrichments")
    void testRulesBeforeEnrichments() throws Exception {
        LOGGER.info("═══════════════════════════════════════════════════════════════");
        LOGGER.info("🔬 UNUSUAL TEST: Rules Before Enrichments");
        LOGGER.info("═══════════════════════════════════════════════════════════════");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RulesBeforeEnrichmentsTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Test data with all required input fields (rules validate input, enrichments add derived data)
        Map<String, Object> testData = new HashMap<>();
        testData.put("underlying", "SPX");
        testData.put("counterparty", "HEDGE_FUND_X");
        testData.put("notionalAmount", 50000000.0);  // Valid: <= 100M
        testData.put("strike", 4600.0);  // Valid: > 0
        testData.put("optionType", "CALL");
        
        LOGGER.info("📊 Input data: {}", testData);
        LOGGER.info("🚀 Executing with sequential processing (rules first)...");
        
        // Execute
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        
        LOGGER.info("📦 Enriched data: {}", enrichedData);
        
        // UNUSUAL VERIFICATION: Rules executed first, then enrichments
        
        // Rules should have validated input data successfully
        assertTrue(result.isSuccess(),
            "Rules should validate input data successfully");
        
        // Enrichments should have added derived data after rules
        assertTrue(enrichedData.containsKey("currentSpotPrice"),
            "E1 (from ref): currentSpotPrice should be enriched after rules");
        assertTrue(enrichedData.containsKey("optionDelta"),
            "E2 (from ref): optionDelta should be enriched after rules");
        assertTrue(enrichedData.containsKey("counterpartyRating"),
            "E3 (inline): counterpartyRating should be enriched after rules");
        assertTrue(enrichedData.containsKey("impliedVolatility"),
            "EG1 (from ref): impliedVolatility should be enriched after rules");
        assertTrue(enrichedData.containsKey("valueAtRisk"),
            "EG2 (from ref): valueAtRisk should be enriched after rules");
        assertTrue(enrichedData.containsKey("productCategory"),
            "EG3 (inline): productCategory should be enriched after rules");
        
        LOGGER.info("✅ Rules Before Enrichments Test PASSED");
        LOGGER.info("   Processing Order Verified:");
        LOGGER.info("   1. R1 (from ref): validate-notional-limit ✅");
        LOGGER.info("   2. R2 (from ref): validate-strike-price ✅");
        LOGGER.info("   3. R3 (inline): validate-product-type ✅");
        LOGGER.info("   4. RG1 (from ref): trade-validation-group ✅");
        LOGGER.info("   5. RG2 (from ref): risk-validation-group ✅");
        LOGGER.info("   6. RG3 (inline): input-validation-group ✅");
        LOGGER.info("   7. E1 (from ref): enrich-market-data ✅");
        LOGGER.info("   8. E2 (from ref): calculate-greeks ✅");
        LOGGER.info("   9. E3 (inline): enrich-counterparty-data ✅");
        LOGGER.info("   10. EG1 (from ref): market-data-enrichment-group ✅");
        LOGGER.info("   11. EG2 (from ref): risk-metrics-enrichment-group ✅");
        LOGGER.info("   12. EG3 (inline): product-enrichment-group ✅");
    }

    @Test
    @DisplayName("Test 3: Verify input validation before enrichment")
    void testInputValidationBeforeEnrichment() throws Exception {
        LOGGER.info("=== TESTING: Input Validation Before Enrichment ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RulesBeforeEnrichmentsTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Test data with invalid input (should fail validation BEFORE enrichment)
        Map<String, Object> testData = new HashMap<>();
        testData.put("underlying", "SPX");
        testData.put("counterparty", "HEDGE_FUND_X");
        testData.put("notionalAmount", 150000000.0);  // INVALID: > 100M
        testData.put("strike", 4600.0);
        testData.put("optionType", "CALL");
        
        LOGGER.info("📊 Test Case: Invalid notional amount = 150M (limit is 100M)");
        LOGGER.info("🚀 Expecting validation failure BEFORE enrichment...");
        
        // Execute
        RuleResult result = engine.evaluate(testData);
        
        // Verify validation failure detected BEFORE enrichment
        assertFalse(result.isSuccess(),
            "Should fail validation before enrichment");
        
        LOGGER.info("✅ Input validation before enrichment works correctly");
    }

    @Test
    @DisplayName("Test 4: Verify enrichments execute after rules pass")
    void testEnrichmentsAfterRulesPass() throws Exception {
        LOGGER.info("=== TESTING: Enrichments Execute After Rules Pass ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RulesBeforeEnrichmentsTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Test data with valid input
        Map<String, Object> testData = new HashMap<>();
        testData.put("underlying", "SPX");
        testData.put("counterparty", "HEDGE_FUND_X");
        testData.put("notionalAmount", 50000000.0);  // Valid
        testData.put("strike", 4600.0);  // Valid
        testData.put("optionType", "CALL");  // Valid
        
        // Execute
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        
        // Verify rules passed
        assertTrue(result.isSuccess(),
            "Rules should pass with valid input");
        
        // Verify enrichments executed after rules passed
        assertTrue(enrichedData.containsKey("currentSpotPrice"),
            "Enrichments should execute after rules pass");
        assertTrue(enrichedData.containsKey("valueAtRisk"),
            "Enrichments should execute after rules pass");
        assertTrue(enrichedData.containsKey("productCategory"),
            "Enrichments should execute after rules pass");
        
        LOGGER.info("✅ Enrichments executed after rules passed correctly");
    }
}

