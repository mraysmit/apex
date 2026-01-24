package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
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
 * PRIORITY 2 TEST: Enrichment-Refs with Groups Placeholder Expansion in Sequential Mode
 *
 * This test verifies enrichment-refs placeholder expansion when loading enrichment groups:
 * - enrichment-refs placeholder is inserted at correct position during YAML parsing
 * - Placeholder is expanded to actual enrichment groups AFTER reference processing
 * - Referenced enrichment groups execute at the correct position in document order
 * 
 * Test Scenario: OTC Options Trade Processing
 * 
 * Expected Processing Order:
 * 1. E1-inline: enrich-counterparty-data (inline enrichment)
 * 2. EG1-from-ref: market-data-enrichment-group (from external file)
 *    - Contains: enrich-market-data-group, calculate-greeks-group
 * 3. EG2-from-ref: risk-metrics-enrichment-group (from external file)
 *    - Contains: calculate-var-group, calculate-exposure-group
 * 4. R1: validate-all-data-enriched
 * 
 * WITHOUT the fix:
 * - Referenced enrichment groups would be appended to END
 * - Order would be: E1 → R1 → EG1 → EG2 (WRONG!)
 * - R1 would fail because EG1 and EG2 haven't executed yet
 * 
 * WITH the fix:
 * - enrichment-group-refs placeholder is expanded at correct position
 * - Order is: E1 → EG1 → EG2 → R1 (CORRECT!)
 * - R1 passes because all enrichments executed first
 */
public class EnrichmentGroupRefsSequentialOrderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentGroupRefsSequentialOrderTest.class);
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        yamlLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("enrichment-refs placeholder expands enrichment groups at correct position in document order")
    public void testEnrichmentGroupRefsPlaceholderExpansion() throws Exception {
        LOGGER.info("=== TESTING: Enrichment-Refs (Groups) Placeholder Expansion ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentGroupRefsSequentialOrderTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Verify configuration loaded correctly
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
        assertNotNull(config.getRules(), "Rules should not be null");

        // Verify enrichment groups from external file were loaded
        assertEquals(2, config.getEnrichmentGroups().size(),
            "Should have 2 enrichment groups from external file");

        LOGGER.info("* Enrichment Groups loaded: {}", config.getEnrichmentGroups().size());
        config.getEnrichmentGroups().forEach(eg -> LOGGER.info("  - {}", eg.getId()));

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

        // Verify EG1 (market-data-enrichment-group from external file) executed
        assertTrue(enrichedData.containsKey("currentSpotPrice"),
            "EG1 (from ref): currentSpotPrice should be enriched");
        assertTrue(enrichedData.containsKey("impliedVolatility"),
            "EG1 (from ref): impliedVolatility should be enriched");
        assertTrue(enrichedData.containsKey("optionDelta"),
            "EG1 (from ref): optionDelta should be calculated");
        assertEquals(4500.0, enrichedData.get("currentSpotPrice"),
            "EG1: Spot price should be 4500.0 for SPX");
        assertEquals(18.5, enrichedData.get("impliedVolatility"),
            "EG1: Volatility should be 18.5 for SPX");

        // Verify EG2 (risk-metrics-enrichment-group from external file) executed
        assertTrue(enrichedData.containsKey("valueAtRisk"),
            "EG2 (from ref): valueAtRisk should be calculated");
        assertTrue(enrichedData.containsKey("netExposure"),
            "EG2 (from ref): netExposure should be calculated");

        // Verify all rules passed (they depend on enrichments)
        assertTrue(result.isTriggered(), "Rules should trigger");

        LOGGER.info("Enrichment-Group-Refs Placeholder Expansion Test PASSED");
        LOGGER.info("   Processing Order Verified:");
        LOGGER.info("   1. E1 (inline): enrich-counterparty-data [OK]");
        LOGGER.info("   2. EG1 (from ref): market-data-enrichment-group [OK]");
        LOGGER.info("   3. EG2 (from ref): risk-metrics-enrichment-group [OK]");
        LOGGER.info("   4. R1: validate-all-data-enriched [OK]");
        LOGGER.info("");
        LOGGER.info("   🎯 CRITICAL FIX VERIFIED: enrichment-group-refs expanded at correct position!");
    }

    @Test
    @DisplayName("enrichment-group-refs: verify all enrichment groups are loaded from external file")
    public void testEnrichmentGroupsLoadedFromExternalFile() throws Exception {
        LOGGER.info("=== TESTING: Enrichment Groups Loaded from External File ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentGroupRefsSequentialOrderTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Verify all enrichment groups are present
        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
        assertEquals(2, config.getEnrichmentGroups().size(), "Should have 2 enrichment groups total");

        // Verify external enrichment groups are present
        boolean hasMarketDataGroup = config.getEnrichmentGroups().stream()
            .anyMatch(eg -> "market-data-enrichment-group".equals(eg.getId()));
        assertTrue(hasMarketDataGroup, "Should have external enrichment group: market-data-enrichment-group");

        boolean hasRiskMetricsGroup = config.getEnrichmentGroups().stream()
            .anyMatch(eg -> "risk-metrics-enrichment-group".equals(eg.getId()));
        assertTrue(hasRiskMetricsGroup, "Should have external enrichment group: risk-metrics-enrichment-group");

        LOGGER.info("All enrichment groups loaded correctly from external file");
    }

    @Test
    @DisplayName("enrichment-group-refs: verify execution order with group dependencies")
    public void testEnrichmentGroupRefsExecutionOrderWithDependencies() throws Exception {
        LOGGER.info("=== TESTING: Enrichment-Group-Refs Execution Order with Dependencies ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/EnrichmentGroupRefsSequentialOrderTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Test Case 1: All data present - all enrichments and rules should execute
        Map<String, Object> completeData = new HashMap<>();
        completeData.put("counterparty", "BANK_Y");
        completeData.put("underlying", "NDX");
        completeData.put("optionType", "PUT");
        completeData.put("strike", 15500.0);
        completeData.put("notionalAmount", 75000000.0);

        RuleResult result1 = engine.evaluate(completeData);
        assertTrue(result1.isSuccess(), "Should succeed with complete data");
        
        Map<String, Object> enriched1 = result1.getEnrichedData();
        assertEquals("AAA", enriched1.get("counterpartyCreditRating"), "BANK_Y should have AAA rating");
        assertEquals(15000.0, enriched1.get("currentSpotPrice"), "NDX spot should be 15000.0");
        assertNotNull(enriched1.get("optionDelta"), "Delta should be calculated");
        assertNotNull(enriched1.get("valueAtRisk"), "VaR should be calculated");
        assertNotNull(enriched1.get("netExposure"), "Net exposure should be calculated");

        LOGGER.info("[OK] Test Case 1: Complete data - all enrichment groups executed in order");

        // Test Case 2: Missing underlying - market data group should fail, risk metrics should not calculate
        Map<String, Object> missingUnderlyingData = new HashMap<>();
        missingUnderlyingData.put("counterparty", "CORP_Z");
        missingUnderlyingData.put("notionalAmount", 10000000.0);
        // No underlying field

        RuleResult result2 = engine.evaluate(missingUnderlyingData);
        
        Map<String, Object> enriched2 = result2.getEnrichedData();
        assertEquals("BBB", enriched2.get("counterpartyCreditRating"), "CORP_Z should have BBB rating");
        // Market data and dependent calculations should not be enriched due to missing underlying
        assertNull(enriched2.get("currentSpotPrice"), "Spot price should be null (no underlying)");
        assertNull(enriched2.get("optionDelta"), "Delta should be null (depends on market data)");
        assertNull(enriched2.get("netExposure"), "Net exposure should be null (depends on delta)");

        LOGGER.info("[OK] Test Case 2: Missing underlying - dependency chain correctly handled");

        LOGGER.info("Enrichment-Group-Refs Execution Order with Dependencies Test PASSED");
    }
}

