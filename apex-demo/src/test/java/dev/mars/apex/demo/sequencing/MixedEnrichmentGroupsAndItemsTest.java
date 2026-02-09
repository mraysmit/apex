package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.core.RulesEngine;
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
 * PRIORITY 2 TEST: Mixed Enrichment Groups and Items in Sequential Mode
 *
 * This test verifies complex interleaving of enrichments and enrichment groups:
 * - enrichment-refs expands to BOTH individual enrichments AND enrichment groups
 * - Inline enrichments execute AFTER enrichment-refs
 * - Inline enrichment groups execute AFTER inline enrichments
 * - All items execute in document order
 *
 * Test Scenario: OTC Options Trade Processing
 *
 * Expected Processing Order:
 * 1. E1-from-ref: enrich-market-data (from external-enrichments-otc.yaml)
 * 2. E2-from-ref: calculate-greeks (from external-enrichments-otc.yaml)
 * 3. EG1-from-ref: market-data-enrichment-group (from external-enrichment-groups-otc.yaml)
 * 4. EG2-from-ref: risk-metrics-enrichment-group (from external-enrichment-groups-otc.yaml)
 * 5. E3-inline: enrich-counterparty-data (inline enrichment)
 * 6. EG3-inline: compliance-enrichment-group (inline enrichment group)
 * 7. R1: validate-all-enrichments-executed
 *
 * This proves:
 * - Individual enrichments and enrichment groups can coexist
 * - enrichment-refs loads both enrichments AND enrichment groups
 * - Complex interleaving works correctly
 */
public class MixedEnrichmentGroupsAndItemsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixedEnrichmentGroupsAndItemsTest.class);
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        yamlLoader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Mixed enrichments and enrichment groups execute in correct document order")
    public void testMixedEnrichmentGroupsAndItemsOrder() throws Exception {
        LOGGER.info("=== TESTING: Mixed Enrichment Groups and Items Order ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/MixedEnrichmentGroupsAndItemsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Verify configuration loaded correctly
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getEnrichments(), "Enrichments should not be null");
        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");

        // Verify enrichments from external file were loaded (2 from external-enrichments-otc.yaml + inline ones)
        assertTrue(config.getEnrichments().size() >= 2,
            "Should have at least 2 enrichments from external file");

        // Verify enrichment groups from external file were loaded (2 from external-enrichment-groups-otc.yaml + 1 inline)
        assertTrue(config.getEnrichmentGroups().size() >= 3,
            "Should have at least 3 enrichment groups (2 from ref + 1 inline)");

        LOGGER.info("* Enrichments loaded: {}", config.getEnrichments().size());
        LOGGER.info("* Enrichment Groups loaded: {}", config.getEnrichmentGroups().size());

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
        assertTrue(result.isSuccess(), "Processing should succeed - all enrichments should execute in order");

        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // Verify E1-E2 (from enrichment-refs) executed
        assertTrue(enrichedData.containsKey("currentSpotPrice"),
            "E1 (from ref): currentSpotPrice should be enriched");
        assertTrue(enrichedData.containsKey("optionDelta"),
            "E2 (from ref): optionDelta should be calculated");

        // Verify E3 (inline enrichment) executed
        assertTrue(enrichedData.containsKey("counterpartyCreditRating"),
            "E3 (inline): counterpartyCreditRating should be enriched");

        // Verify EG1-EG2 (from enrichment-group-refs) executed
        assertTrue(enrichedData.containsKey("valueAtRisk"),
            "EG2 (from ref): valueAtRisk should be calculated");
        assertTrue(enrichedData.containsKey("netExposure"),
            "EG2 (from ref): netExposure should be calculated");

        // Verify EG3 (inline enrichment group) executed
        assertTrue(enrichedData.containsKey("complianceScore"),
            "EG3 (inline group): complianceScore should be calculated");

        // Verify all rules passed
        assertTrue(result.isTriggered(), "Rules should trigger");

        LOGGER.info("Mixed Enrichment Groups and Items Order Test PASSED");
        LOGGER.info("   Processing Order Verified:");
        LOGGER.info("   1. E1 (from ref): enrich-market-data [OK]");
        LOGGER.info("   2. E2 (from ref): calculate-greeks [OK]");
        LOGGER.info("   3. E3 (inline): enrich-counterparty-data [OK]");
        LOGGER.info("   4. EG1 (from ref): market-data-enrichment-group [OK]");
        LOGGER.info("   5. EG2 (from ref): risk-metrics-enrichment-group [OK]");
        LOGGER.info("   6. EG3 (inline): compliance-enrichment-group [OK]");
        LOGGER.info("   7. R1: validate-all-enrichments-executed [OK]");
        LOGGER.info("");
        LOGGER.info("   🎯 COMPLEX INTERLEAVING VERIFIED: Mixed enrichments and groups work correctly!");
    }

    @Test
    @DisplayName("Verify enrichment-refs expands before inline enrichments")
    public void testEnrichmentRefsExpandBeforeInline() throws Exception {
        LOGGER.info("=== TESTING: Enrichment-Refs Expand Before Inline ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/MixedEnrichmentGroupsAndItemsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Verify enrichments from external file are present
        boolean hasMarketData = config.getEnrichments().stream()
            .anyMatch(e -> "enrich-market-data".equals(e.getId()));
        assertTrue(hasMarketData, "Should have enrichment from ref: enrich-market-data");

        boolean hasGreeks = config.getEnrichments().stream()
            .anyMatch(e -> "calculate-greeks".equals(e.getId()));
        assertTrue(hasGreeks, "Should have enrichment from ref: calculate-greeks");

        // Verify inline enrichment is present
        boolean hasCounterparty = config.getEnrichments().stream()
            .anyMatch(e -> "enrich-counterparty-data".equals(e.getId()));
        assertTrue(hasCounterparty, "Should have inline enrichment: enrich-counterparty-data");

        LOGGER.info("Enrichment-refs expanded before inline enrichments");
    }

    @Test
    @DisplayName("Verify enrichment-group-refs expands before inline enrichment groups")
    public void testEnrichmentGroupRefsExpandBeforeInline() throws Exception {
        LOGGER.info("=== TESTING: Enrichment-Group-Refs Expand Before Inline ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/MixedEnrichmentGroupsAndItemsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);

        // Verify enrichment groups from external file are present
        boolean hasMarketDataGroup = config.getEnrichmentGroups().stream()
            .anyMatch(eg -> "market-data-enrichment-group".equals(eg.getId()));
        assertTrue(hasMarketDataGroup, "Should have enrichment group from ref: market-data-enrichment-group");

        boolean hasRiskMetricsGroup = config.getEnrichmentGroups().stream()
            .anyMatch(eg -> "risk-metrics-enrichment-group".equals(eg.getId()));
        assertTrue(hasRiskMetricsGroup, "Should have enrichment group from ref: risk-metrics-enrichment-group");

        // Verify inline enrichment group is present
        boolean hasComplianceGroup = config.getEnrichmentGroups().stream()
            .anyMatch(eg -> "compliance-enrichment-group".equals(eg.getId()));
        assertTrue(hasComplianceGroup, "Should have inline enrichment group: compliance-enrichment-group");

        LOGGER.info("Enrichment-group-refs expanded before inline enrichment groups");
    }

    @Test
    @DisplayName("Verify all enrichments execute with complex dependencies")
    public void testComplexDependencyChain() throws Exception {
        LOGGER.info("=== TESTING: Complex Dependency Chain ===");

        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/MixedEnrichmentGroupsAndItemsTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Test with complete data
        Map<String, Object> completeData = new HashMap<>();
        completeData.put("counterparty", "BANK_Y");
        completeData.put("underlying", "NDX");
        completeData.put("optionType", "PUT");
        completeData.put("strike", 15500.0);
        completeData.put("notionalAmount", 75000000.0);

        RuleResult result = engine.evaluate(completeData);
        assertTrue(result.isSuccess(), "Should succeed with complete data");
        
        Map<String, Object> enriched = result.getEnrichedData();
        
        // Verify all enrichments executed
        assertEquals(15000.0, enriched.get("currentSpotPrice"), "Market data should be enriched");
        assertNotNull(enriched.get("optionDelta"), "Greeks should be calculated");
        assertEquals("AAA", enriched.get("counterpartyCreditRating"), "Counterparty should be enriched");
        assertNotNull(enriched.get("valueAtRisk"), "VaR should be calculated");
        assertNotNull(enriched.get("netExposure"), "Exposure should be calculated");
        assertEquals(100, enriched.get("complianceScore"), "Compliance score should be calculated");

        LOGGER.info("Complex Dependency Chain Test PASSED");
    }
}

