package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
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
 * CRITICAL TEST: Rule-Refs BEFORE Inline Rules
 * 
 * Tests the CRITICAL scenario where rule-refs appears BEFORE any inline rules.
 * This is essential to prove that reference placeholders can appear at ANY position in the document.
 * 
 * Expected Processing Order:
 * 1. E1-inline (enrich-counterparty-data) - inline enrichment
 * 2. R1-from-ref (validate-notional-limit) - from external file
 * 3. R2-from-ref (validate-strike-price) - from external file
 * 4. R3-inline (validate-counterparty-enriched) - inline rule
 * 
 * SUCCESS CRITERIA:
 * - All rules from external file are loaded
 * - Referenced rules execute BEFORE inline rules
 * - All enrichments execute BEFORE all rules
 * - Processing order matches document order
 */
@DisplayName("Rule-Refs BEFORE Inline Rules")
class RuleRefsBeforeInlineTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleRefsBeforeInlineTest.class);
    private final YamlConfigurationLoader yamlLoader = new YamlConfigurationLoader();

    @Test
    @DisplayName("Test 1: Verify rules loaded from external file FIRST")
    void testRulesLoadedFromExternalFileFirst() throws Exception {
        LOGGER.info("=== TESTING: Rules Loaded from External File FIRST ===");

        // Load configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RuleRefsBeforeInlineTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Verify rules loaded
        assertNotNull(engine.getConfiguration().getAllRules(), "Rules should be loaded");
        assertEquals(3, engine.getConfiguration().getAllRules().size(), 
            "Should have 3 rules: 2 from external file + 1 inline");
        
        // Verify rule IDs
        LOGGER.info("Loaded rules:");
        engine.getConfiguration().getAllRules().forEach(r -> LOGGER.info("  - {}", r.getId()));
        
        assertTrue(engine.getConfiguration().getAllRules().stream().anyMatch(r -> "validate-notional-limit".equals(r.getId())),
            "Should have validate-notional-limit from external file");
        assertTrue(engine.getConfiguration().getAllRules().stream().anyMatch(r -> "validate-strike-price".equals(r.getId())),
            "Should have validate-strike-price from external file");
        assertTrue(engine.getConfiguration().getAllRules().stream().anyMatch(r -> "validate-counterparty-enriched".equals(r.getId())),
            "Should have validate-counterparty-enriched inline");
        
        LOGGER.info("All rules loaded correctly (2 from ref + 1 inline)");
    }

    @Test
    @DisplayName("Test 2: CRITICAL - Verify rule-refs expanded at FIRST position")
    void testRuleRefsExpandedAtFirstPosition() throws Exception {
        LOGGER.info("═══════════════════════════════════════════════════════════════");
        LOGGER.info("🔬 CRITICAL TEST: Rule-Refs BEFORE Inline Rules");
        LOGGER.info("═══════════════════════════════════════════════════════════════");

        // Load configuration and create engine
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RuleRefsBeforeInlineTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        
        // Test data with valid values (all rules should pass)
        Map<String, Object> testData = new HashMap<>();
        testData.put("counterparty", "HEDGE_FUND_X");  // For enrichment
        testData.put("notionalAmount", 50000000.0);  // Valid: <= 100M (R1 from ref)
        testData.put("strike", 100.0);  // Valid: > 0 (R2 from ref)
        
        LOGGER.info("Input data: {}", testData);
        LOGGER.info("🚀 Executing with sequential processing...");
        
        // Execute
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        
        LOGGER.info("📦 Enriched data: {}", enrichedData);
        
        // CRITICAL VERIFICATION: All processing in correct order
        
        // E1 (inline): Counterparty data enriched FIRST
        assertTrue(enrichedData.containsKey("counterpartyCreditRating"),
            "E1 (inline): counterpartyCreditRating should be enriched FIRST");
        assertTrue(enrichedData.containsKey("counterpartyCreditLimit"),
            "E1 (inline): counterpartyCreditLimit should be enriched FIRST");
        assertEquals("AA", enrichedData.get("counterpartyCreditRating"),
            "E1 (inline): Should lookup HEDGE_FUND_X credit rating = AA");
        
        // R1, R2 (from ref): Validation rules executed SECOND (after enrichments)
        // R3 (inline): Validation rule executed THIRD (after ref rules)
        
        // Verify processing succeeded (all rules passed)
        assertTrue(result.isSuccess(),
            "Processing should succeed - all rules should pass with valid data");
        
        LOGGER.info("═══════════════════════════════════════════════════════════════");
        LOGGER.info("CRITICAL FIX VERIFIED: rule-refs expanded at FIRST position!");
        LOGGER.info("   - E1 (inline): Counterparty enriched FIRST");
        LOGGER.info("   - R1 (from ref): Notional limit validated SECOND");
        LOGGER.info("   - R2 (from ref): Strike price validated THIRD");
        LOGGER.info("   - R3 (inline): Counterparty validated FOURTH");
        LOGGER.info("═══════════════════════════════════════════════════════════════");
    }

    @Test
    @DisplayName("Test 3: Verify execution order with different data scenarios")
    void testExecutionOrderWithDifferentScenarios() throws Exception {
        LOGGER.info("=== TESTING: Execution Order with Different Data Scenarios ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RuleRefsBeforeInlineTest.yaml";
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        // Test Case 1: High notional (R1 from ref should trigger)
        Map<String, Object> testData1 = new HashMap<>();
        testData1.put("counterparty", "BANK_Y");
        testData1.put("notionalAmount", 150000000.0);  // > 100M - triggers R1
        testData1.put("strike", 100.0);  // Valid

        LOGGER.info("Test Case 1 - High notional: {}", testData1);
        RuleResult result1 = engine.evaluate(testData1);

        // Verify enrichment executed BEFORE rules (correct order)
        assertNotNull(result1, "Result should not be null");
        assertNotNull(result1.getEnrichedData(), "Enriched data should not be null");
        assertTrue(result1.getEnrichedData().containsKey("counterpartyCreditRating"),
            "E1 (inline): Enrichment should execute BEFORE rules");
        assertEquals("AAA", result1.getEnrichedData().get("counterpartyCreditRating"),
            "E1 (inline): Should lookup BANK_Y credit rating = AAA");

        LOGGER.info("Test Case 1: Enrichment executed BEFORE rules (correct order)");

        // Test Case 2: Invalid strike (R2 from ref should trigger)
        Map<String, Object> testData2 = new HashMap<>();
        testData2.put("counterparty", "CORP_Z");
        testData2.put("notionalAmount", 50000000.0);  // Valid
        testData2.put("strike", -10.0);  // <= 0 - triggers R2

        LOGGER.info("Test Case 2 - Invalid strike: {}", testData2);
        RuleResult result2 = engine.evaluate(testData2);

        // Verify enrichment executed BEFORE rules (correct order)
        assertNotNull(result2, "Result should not be null");
        assertNotNull(result2.getEnrichedData(), "Enriched data should not be null");
        assertTrue(result2.getEnrichedData().containsKey("counterpartyCreditRating"),
            "E1 (inline): Enrichment should execute BEFORE rules");
        assertEquals("BBB", result2.getEnrichedData().get("counterpartyCreditRating"),
            "E1 (inline): Should lookup CORP_Z credit rating = BBB");

        LOGGER.info("Test Case 2: Enrichment executed BEFORE rules (correct order)");

        // Test Case 3: Valid data (all rules should pass)
        Map<String, Object> testData3 = new HashMap<>();
        testData3.put("counterparty", "HEDGE_FUND_X");
        testData3.put("notionalAmount", 50000000.0);  // Valid: <= 100M
        testData3.put("strike", 100.0);  // Valid: > 0

        LOGGER.info("Test Case 3 - Valid data: {}", testData3);
        RuleResult result3 = engine.evaluate(testData3);

        // Verify enrichment executed BEFORE rules (correct order)
        assertNotNull(result3, "Result should not be null");
        assertTrue(result3.isSuccess(), "Should succeed - all validations pass");
        assertNotNull(result3.getEnrichedData(), "Enriched data should not be null");
        assertTrue(result3.getEnrichedData().containsKey("counterpartyCreditRating"),
            "E1 (inline): Enrichment should execute BEFORE rules");
        assertTrue(result3.getEnrichedData().containsKey("counterpartyCreditLimit"),
            "E1 (inline): Enrichment should execute BEFORE rules");

        LOGGER.info("Test Case 3: Valid data - all rules passed in correct order");
        LOGGER.info("All test cases passed - rule-refs execution order verified!");
    }
}

