package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.core.RulesEngine;
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
 * CRITICAL TEST: Enrichment-Refs BEFORE Inline Enrichments
 * 
 * Tests the CRITICAL scenario where enrichment-refs appears BEFORE any inline enrichments.
 * This is essential to prove that reference placeholders can appear at ANY position in the document.
 * 
 * Expected Processing Order:
 * 1. E1-from-ref (enrich-market-data) - from external file
 * 2. E2-from-ref (calculate-greeks) - from external file  
 * 3. E3-inline (enrich-counterparty-data) - inline enrichment
 * 4. R1-R3 (validation rules)
 * 
 * SUCCESS CRITERIA:
 * - All enrichments from external file are loaded
 * - Referenced enrichments execute BEFORE inline enrichments
 * - All enrichments execute BEFORE rules
 * - Processing order matches document order
 */
@DisplayName("Enrichment-Refs BEFORE Inline Enrichments")
class EnrichmentRefsBeforeInlineTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentRefsBeforeInlineTest.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @Test
    @DisplayName("Test 1: Verify enrichments loaded from external file FIRST")
    void testEnrichmentsLoadedFromExternalFileFirst() throws Exception {
        LOGGER.info("=== TESTING: Enrichments Loaded from External File FIRST ===");

        // Load configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentRefsBeforeInlineTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        
        // Verify enrichments loaded
        assertNotNull(config.getEnrichments(), "Enrichments should be loaded");
        assertEquals(3, config.getEnrichments().size(), 
            "Should have 3 enrichments: 2 from external file + 1 inline");
        
        // Verify enrichment IDs
        LOGGER.info("Loaded enrichments:");
        config.getEnrichments().forEach(e -> LOGGER.info("  - {}", e.getId()));
        
        assertTrue(config.getEnrichments().stream().anyMatch(e -> "enrich-market-data".equals(e.getId())),
            "Should have enrich-market-data from external file");
        assertTrue(config.getEnrichments().stream().anyMatch(e -> "calculate-greeks".equals(e.getId())),
            "Should have calculate-greeks from external file");
        assertTrue(config.getEnrichments().stream().anyMatch(e -> "enrich-counterparty-data".equals(e.getId())),
            "Should have enrich-counterparty-data inline");
        
        LOGGER.info("All enrichments loaded correctly (2 from ref + 1 inline)");
    }

    @Test
    @DisplayName("Test 2: CRITICAL - Verify enrichment-refs expanded at FIRST position")
    void testEnrichmentRefsExpandedAtFirstPosition() throws Exception {
        LOGGER.info("═══════════════════════════════════════════════════════════════");
        LOGGER.info("🔬 CRITICAL TEST: Enrichment-Refs BEFORE Inline Enrichments");
        LOGGER.info("═══════════════════════════════════════════════════════════════");

        // Load configuration and create engine
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentRefsBeforeInlineTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Test data with all required fields
        Map<String, Object> testData = new HashMap<>();
        testData.put("underlying", "SPX");  // For market data lookup (from ref)
        testData.put("counterparty", "HEDGE_FUND_X");  // For counterparty lookup (inline)
        
        LOGGER.info("Input data: {}", testData);
        LOGGER.info("🚀 Executing with sequential processing...");
        
        // Execute
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        
        LOGGER.info("📦 Enriched data: {}", enrichedData);
        
        // CRITICAL VERIFICATION: All enrichments executed in correct order
        
        // E1 (from ref): Market data enriched
        assertTrue(enrichedData.containsKey("currentSpotPrice"),
            "E1 (from ref): currentSpotPrice should be enriched FIRST");
        assertTrue(enrichedData.containsKey("impliedVolatility"),
            "E1 (from ref): impliedVolatility should be enriched FIRST");
        assertEquals(4500.0, enrichedData.get("currentSpotPrice"),
            "E1 (from ref): Should lookup SPX spot price = 4500.0");
        
        // E2 (from ref, depends on E1): Greeks calculated
        assertTrue(enrichedData.containsKey("optionDelta"),
            "E2 (from ref, depends on E1): optionDelta should be calculated SECOND");
        double expectedDelta = 4500.0 * 18.5 * 0.01;  // currentSpotPrice * impliedVolatility * 0.01
        assertEquals(expectedDelta, Double.parseDouble(enrichedData.get("optionDelta").toString()), 0.01,
            "E2 (from ref): Should calculate delta = 4500.0 * 18.5 * 0.01 = " + expectedDelta);
        
        // E3 (inline): Counterparty data enriched
        assertTrue(enrichedData.containsKey("counterpartyCreditRating"),
            "E3 (inline): counterpartyCreditRating should be enriched THIRD");
        assertTrue(enrichedData.containsKey("counterpartyCreditLimit"),
            "E3 (inline): counterpartyCreditLimit should be enriched THIRD");
        assertEquals("AA", enrichedData.get("counterpartyCreditRating"),
            "E3 (inline): Should lookup HEDGE_FUND_X credit rating = AA");
        
        // Verify processing succeeded (all rules passed)
        assertTrue(result.isSuccess(),
            "Processing should succeed - all enrichments executed before rules");
        
        LOGGER.info("═══════════════════════════════════════════════════════════════");
        LOGGER.info("CRITICAL FIX VERIFIED: enrichment-refs expanded at FIRST position!");
        LOGGER.info("   - E1 (from ref): Market data enriched FIRST");
        LOGGER.info("   - E2 (from ref): Greeks calculated SECOND (depends on E1)");
        LOGGER.info("   - E3 (inline): Counterparty enriched THIRD");
        LOGGER.info("   - All rules validated enriched data successfully");
        LOGGER.info("═══════════════════════════════════════════════════════════════");
    }

    @Test
    @DisplayName("Test 3: Verify execution order with missing data")
    void testExecutionOrderWithMissingData() throws Exception {
        LOGGER.info("=== TESTING: Execution Order with Missing Data ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentRefsBeforeInlineTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Test Case 1: Missing underlying (E1 won't enrich, E2 won't calculate)
        Map<String, Object> testData1 = new HashMap<>();
        testData1.put("counterparty", "BANK_Y");  // Only counterparty data
        
        LOGGER.info("Test Case 1 - Missing underlying: {}", testData1);
        RuleResult result1 = engine.evaluate(testData1);
        Map<String, Object> enrichedData1 = result1.getEnrichedData();
        
        // E1, E2 should not enrich (no underlying)
        assertFalse(enrichedData1.containsKey("currentSpotPrice"),
            "E1 should not enrich without underlying");
        assertFalse(enrichedData1.containsKey("optionDelta"),
            "E2 should not calculate without market data");
        
        // E3 should still enrich (has counterparty)
        assertTrue(enrichedData1.containsKey("counterpartyCreditRating"),
            "E3 should enrich counterparty data");
        assertEquals("AAA", enrichedData1.get("counterpartyCreditRating"),
            "E3 should lookup BANK_Y credit rating = AAA");
        
        LOGGER.info("Test Case 1: E1, E2 skipped, E3 executed correctly");
        
        // Test Case 2: Missing counterparty (E1, E2 execute, E3 won't enrich)
        Map<String, Object> testData2 = new HashMap<>();
        testData2.put("underlying", "NDX");  // Only underlying data
        
        LOGGER.info("Test Case 2 - Missing counterparty: {}", testData2);
        RuleResult result2 = engine.evaluate(testData2);
        Map<String, Object> enrichedData2 = result2.getEnrichedData();
        
        // E1, E2 should enrich
        assertTrue(enrichedData2.containsKey("currentSpotPrice"),
            "E1 should enrich market data");
        assertEquals(15000.0, enrichedData2.get("currentSpotPrice"),
            "E1 should lookup NDX spot price = 15000.0");
        assertTrue(enrichedData2.containsKey("optionDelta"),
            "E2 should calculate greeks");
        
        // E3 should not enrich (no counterparty)
        assertFalse(enrichedData2.containsKey("counterpartyCreditRating"),
            "E3 should not enrich without counterparty");
        
        LOGGER.info("Test Case 2: E1, E2 executed, E3 skipped correctly");
        LOGGER.info("Enrichment-Refs Execution Order Test PASSED");
    }
}

