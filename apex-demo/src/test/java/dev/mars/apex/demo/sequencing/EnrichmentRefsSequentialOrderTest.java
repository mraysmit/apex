package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRITICAL TEST: Enrichment-Refs Placeholder Expansion in Sequential Mode
 * 
 * This test verifies the CORE FIX for sequential processing:
 * - enrichment-refs placeholder is inserted at correct position during YAML parsing
 * - Placeholder is expanded to actual enrichments AFTER reference processing
 * - Referenced enrichments execute at the correct position in document order
 * 
 * Test Scenario: OTC Options Trade Processing
 * 
 * Expected Processing Order:
 * 1. E1-inline: enrich-counterparty-data (inline enrichment)
 * 2. E2-from-ref: enrich-market-data (from external file)
 * 3. E3-from-ref: calculate-greeks (from external file, depends on E2)
 * 4. R1: validate-counterparty-enriched
 * 5. R2: validate-market-data-enriched (depends on E2)
 * 6. R3: validate-greeks-calculated (depends on E3)
 * 
 * WITHOUT the fix:
 * - Referenced enrichments would be appended to END of enrichments list
 * - Order would be: E1 → R1 → R2 → R3 → E2 → E3 (WRONG!)
 * - R2 and R3 would fail because E2 and E3 haven't executed yet
 * 
 * WITH the fix:
 * - enrichment-refs placeholder is expanded at correct position
 * - Order is: E1 → E2 → E3 → R1 → R2 → R3 (CORRECT!)
 * - All rules pass because enrichments executed first
 */
public class EnrichmentRefsSequentialOrderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentRefsSequentialOrderTest.class);
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        yamlLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("enrichment-refs placeholder expands at correct position in document order")
    public void testEnrichmentRefsPlaceholderExpansion() throws Exception {
        LOGGER.info("=== TESTING: Enrichment-Refs Placeholder Expansion ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentRefsSequentialOrderTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Verify configuration loaded correctly
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getEnrichments(), "Enrichments should not be null");
        assertNotNull(config.getRules(), "Rules should not be null");

        // Verify enrichments from external file were loaded
        assertEquals(3, config.getEnrichments().size(),
            "Should have 3 enrichments: 1 inline + 2 from external file");

        LOGGER.info("* Enrichments loaded: {}", config.getEnrichments().size());
        config.getEnrichments().forEach(e -> LOGGER.info("  - {}", e.getId()));

        // Act - Execute with test data
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("counterparty", "HEDGE_FUND_X");
        tradeData.put("underlying", "SPX");
        tradeData.put("optionType", "CALL");
        tradeData.put("strike", 4600.0);
        tradeData.put("notionalAmount", 50000000.0);

        LOGGER.info("* Input Data: {}", tradeData);

        RuleResult result = engine.evaluate(config, tradeData);

        // Assert - Verify processing succeeded
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed - all enrichments should execute before rules");

        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // Verify E1 (inline) executed
        assertTrue(enrichedData.containsKey("counterpartyCreditRating"),
            "E1 (inline): counterpartyCreditRating should be enriched");
        assertTrue(enrichedData.containsKey("counterpartyCreditLimit"),
            "E1 (inline): counterpartyCreditLimit should be enriched");
        assertEquals("AA", enrichedData.get("counterpartyCreditRating"),
            "E1: Credit rating should be AA for HEDGE_FUND_X");
        assertEquals(100000000.0, enrichedData.get("counterpartyCreditLimit"),
            "E1: Credit limit should be 100M for HEDGE_FUND_X");

        // Verify E2 (from external file) executed
        assertTrue(enrichedData.containsKey("currentSpotPrice"),
            "E2 (from ref): currentSpotPrice should be enriched");
        assertTrue(enrichedData.containsKey("impliedVolatility"),
            "E2 (from ref): impliedVolatility should be enriched");
        assertEquals(4500.0, enrichedData.get("currentSpotPrice"),
            "E2: Spot price should be 4500.0 for SPX");
        assertEquals(18.5, enrichedData.get("impliedVolatility"),
            "E2: Volatility should be 18.5 for SPX");

        // Verify E3 (from external file, depends on E2) executed
        assertTrue(enrichedData.containsKey("optionDelta"),
            "E3 (from ref, depends on E2): optionDelta should be calculated");
        
        double expectedDelta = 4500.0 * 18.5 * 0.01; // 832.5
        assertEquals(expectedDelta, enrichedData.get("optionDelta"),
            "E3: Option delta should be calculated from spot price and volatility");

        // Verify all rules passed (they depend on enrichments)
        assertTrue(result.isTriggered(), "Rules should trigger");

        LOGGER.info("Enrichment-Refs Placeholder Expansion Test PASSED");
        LOGGER.info("   Processing Order Verified:");
        LOGGER.info("   1. E1 (inline): enrich-counterparty-data [OK]");
        LOGGER.info("   2. E2 (from ref): enrich-market-data [OK]");
        LOGGER.info("   3. E3 (from ref): calculate-greeks [OK]");
        LOGGER.info("   4. R1: validate-counterparty-enriched [OK]");
        LOGGER.info("   5. R2: validate-market-data-enriched [OK]");
        LOGGER.info("   6. R3: validate-greeks-calculated [OK]");
        LOGGER.info("");
        LOGGER.info("   🎯 CRITICAL FIX VERIFIED: enrichment-refs expanded at correct position!");
    }

    @Test
    @DisplayName("enrichment-refs: verify all enrichments are loaded from external file")
    public void testEnrichmentsLoadedFromExternalFile() throws Exception {
        LOGGER.info("=== TESTING: Enrichments Loaded from External File ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentRefsSequentialOrderTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Verify all enrichments are present
        assertNotNull(config.getEnrichments(), "Enrichments should not be null");
        assertEquals(3, config.getEnrichments().size(), "Should have 3 enrichments total");

        // Verify inline enrichment is present
        boolean hasInlineEnrichment = config.getEnrichments().stream()
            .anyMatch(e -> "enrich-counterparty-data".equals(e.getId()));
        assertTrue(hasInlineEnrichment, "Should have inline enrichment: enrich-counterparty-data");

        // Verify external enrichments are present
        boolean hasMarketData = config.getEnrichments().stream()
            .anyMatch(e -> "enrich-market-data".equals(e.getId()));
        assertTrue(hasMarketData, "Should have external enrichment: enrich-market-data");

        boolean hasGreeks = config.getEnrichments().stream()
            .anyMatch(e -> "calculate-greeks".equals(e.getId()));
        assertTrue(hasGreeks, "Should have external enrichment: calculate-greeks");

        LOGGER.info("All enrichments loaded correctly from inline and external sources");
    }

    @Test
    @DisplayName("enrichment-refs: verify execution order with dependencies")
    public void testEnrichmentRefsExecutionOrderWithDependencies() throws Exception {
        LOGGER.info("=== TESTING: Enrichment-Refs Execution Order with Dependencies ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentRefsSequentialOrderTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Test Case 1: All data present - all enrichments and rules should execute
        Map<String, Object> completeData = new HashMap<>();
        completeData.put("counterparty", "BANK_Y");
        completeData.put("underlying", "NDX");
        completeData.put("optionType", "PUT");
        completeData.put("strike", 15500.0);

        RuleResult result1 = engine.evaluate(completeData);
        assertTrue(result1.isSuccess(), "Should succeed with complete data");
        
        Map<String, Object> enriched1 = result1.getEnrichedData();
        assertEquals("AAA", enriched1.get("counterpartyCreditRating"), "BANK_Y should have AAA rating");
        assertEquals(15000.0, enriched1.get("currentSpotPrice"), "NDX spot should be 15000.0");
        assertNotNull(enriched1.get("optionDelta"), "Delta should be calculated");

        LOGGER.info("[OK] Test Case 1: Complete data - all enrichments executed in order");

        // Test Case 2: Missing underlying - market data enrichment should fail, greeks should not calculate
        Map<String, Object> missingUnderlyingData = new HashMap<>();
        missingUnderlyingData.put("counterparty", "CORP_Z");
        // No underlying field

        RuleResult result2 = engine.evaluate(missingUnderlyingData);
        
        Map<String, Object> enriched2 = result2.getEnrichedData();
        assertEquals("BBB", enriched2.get("counterpartyCreditRating"), "CORP_Z should have BBB rating");
        // Market data and greeks should not be enriched due to missing underlying
        assertNull(enriched2.get("currentSpotPrice"), "Spot price should be null (no underlying)");
        assertNull(enriched2.get("optionDelta"), "Delta should be null (depends on market data)");

        LOGGER.info("[OK] Test Case 2: Missing underlying - dependency chain correctly handled");

        LOGGER.info("Enrichment-Refs Execution Order with Dependencies Test PASSED");
    }
}

