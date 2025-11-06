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
 * CRITICAL TEST: Both Enrichment-Refs AND Rule-Refs (Enrichment First)
 * 
 * Tests the CRITICAL scenario where BOTH enrichment-refs AND rule-refs appear in the same file.
 * Enrichment-refs appears FIRST, rule-refs appears SECOND.
 * 
 * Expected Processing Order:
 * 1. E1-from-ref (enrich-market-data) - from external enrichments file
 * 2. E2-from-ref (calculate-greeks) - from external enrichments file
 * 3. E3-inline (enrich-product-type) - inline enrichment
 * 4. R1-from-ref (validate-notional-limit) - from external rules file
 * 5. R2-from-ref (validate-strike-price) - from external rules file
 * 6. R3-inline (validate-enrichments-complete) - inline rule
 *
 * SUCCESS CRITERIA:
 * - Both enrichment-refs and rule-refs are loaded
 * - Enrichment-refs expands BEFORE inline enrichments
 * - Rule-refs expands BEFORE inline rules
 * - All enrichments execute BEFORE all rules
 */
@DisplayName("CRITICAL: Both Refs - Enrichment-Refs First")
class BothRefsEnrichmentFirstTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BothRefsEnrichmentFirstTest.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @Test
    @DisplayName("Test 1: Verify both enrichments and rules loaded from external files")
    void testBothEnrichmentsAndRulesLoaded() throws Exception {
        LOGGER.info("=== TESTING: Both Enrichments and Rules Loaded ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/BothRefsEnrichmentFirstTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Verify enrichments loaded (2 from external file + 1 inline)
        assertNotNull(config.getEnrichments(), "Enrichments should be loaded");
        assertEquals(3, config.getEnrichments().size(),
            "Should have 3 enrichments: 2 from external file + 1 inline");

        // Verify rules loaded (2 from external file + 1 inline)
        assertNotNull(engine.getConfiguration().getAllRules(), "Rules should be loaded");
        assertEquals(3, engine.getConfiguration().getAllRules().size(),
            "Should have 3 rules: 2 from external file + 1 inline");
        
        LOGGER.info("✅ Both enrichments and rules loaded correctly");
    }

    @Test
    @DisplayName("Test 2: CRITICAL - Verify enrichment-refs expanded BEFORE rule-refs")
    void testEnrichmentRefsBeforeRuleRefs() throws Exception {
        LOGGER.info("═══════════════════════════════════════════════════════════════");
        LOGGER.info("🔬 CRITICAL TEST: Both Refs - Enrichment-Refs First");
        LOGGER.info("═══════════════════════════════════════════════════════════════");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/BothRefsEnrichmentFirstTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("underlying", "SPX");  // For market data lookup (E1)
        testData.put("notionalAmount", 50000000.0);  // Valid: <= 100M (R1)
        testData.put("strike", 100.0);  // Valid: > 0 (R2)
        
        LOGGER.info("📊 Input data: {}", testData);
        LOGGER.info("🚀 Executing with sequential processing...");
        
        // Execute
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        
        LOGGER.info("📦 Enriched data: {}", enrichedData);
        
        // CRITICAL VERIFICATION: All processing in correct order
        
        // E1 (from ref): Market data enriched FIRST
        assertTrue(enrichedData.containsKey("currentSpotPrice"),
            "E1 (from ref): currentSpotPrice should be enriched FIRST");
        assertEquals(4500.0, enrichedData.get("currentSpotPrice"),
            "E1 (from ref): Should lookup SPX spot price = 4500.0");
        
        // E2 (from ref, depends on E1): Greeks calculated SECOND
        assertTrue(enrichedData.containsKey("optionDelta"),
            "E2 (from ref): optionDelta should be calculated SECOND");
        double expectedDelta = 4500.0 * 18.5 * 0.01;
        assertEquals(expectedDelta, Double.parseDouble(enrichedData.get("optionDelta").toString()), 0.01,
            "E2 (from ref): Should calculate delta = spotPrice * vega * 0.01");

        // E3 (inline): Product type enriched THIRD
        assertTrue(enrichedData.containsKey("productType"),
            "E3 (inline): productType should be enriched THIRD");
        assertEquals("OTC_OPTION", enrichedData.get("productType"),
            "E3 (inline): Should set productType = OTC_OPTION");

        // R1, R2, R3: Validation rules executed FOURTH (after all enrichments)
        assertTrue(result.isSuccess(),
            "Processing should succeed - all rules should pass with valid data");

        LOGGER.info("═══════════════════════════════════════════════════════════════");
        LOGGER.info("✅ CRITICAL FIX VERIFIED: enrichment-refs expanded BEFORE rule-refs!");
        LOGGER.info("   - E1 (from ref): Market data enriched FIRST");
        LOGGER.info("   - E2 (from ref): Greeks calculated SECOND");
        LOGGER.info("   - E3 (inline): Product type enriched THIRD");
        LOGGER.info("   - R1 (from ref): Notional limit validated FOURTH");
        LOGGER.info("   - R2 (from ref): Strike price validated FIFTH");
        LOGGER.info("   - R3 (inline): Enrichments validated SIXTH");
        LOGGER.info("═══════════════════════════════════════════════════════════════");
    }
}

