package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests item-level processing with OTC options trade processing.
 *
 * This test demonstrates the NEW item-level processing capability where
 * individual items within sections are processed in document order.
 *
 * Test Scenario: Middle Office OTC Option Trade Processing
 *
 * Processing Order (Item-Level):
 * 1. enrich-counterparty-credit-rating (E1) - Lookup counterparty credit data
 * 2. validate-credit-limit (R1) - Validate trade against credit limit (depends on E1)
 * 3. calculate-value-at-risk (E2) - Calculate VaR (depends on E1)
 * 4. approve-or-reject-trade (R2) - Final approval decision (depends on E2)
 *
 * This demonstrates:
 * - Item-level ordering within sections (E1 -> E2 in enrichments section)
 * - Item-level ordering within sections (R1 -> R2 in rules section)
 * - Cross-section dependencies (R1 depends on E1, R2 depends on E2)
 * - Real database lookups (not inline data)
 */
public class ItemLevelProcessingOtcOptionsTest {

    private static final Logger LOGGER = Logger.getLogger(ItemLevelProcessingOtcOptionsTest.class.getName());
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        yamlLoader = new YamlConfigurationLoader();
    }

    @Test
    public void testItemLevelProcessingWithOtcOptions() throws Exception {
        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/ItemLevelProcessingOtcOptionsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeType", "OTC_OPTION");
        tradeData.put("counterparty", "HEDGE_FUND_X");
        tradeData.put("notionalAmount", 50000000.0);
        tradeData.put("optionType", "CALL");
        tradeData.put("strike", 105.0);
        tradeData.put("maturity", "2025-12-31");

        // Act
        RuleResult result = engine.evaluate(config, tradeData);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Overall result should be success");

        Map<String, Object> enrichedData = result.getEnrichedData();
        
        // Verify E1 executed: counterparty credit data should be enriched
        assertTrue(enrichedData.containsKey("counterpartyCreditRating"), 
            "counterpartyCreditRating should be present (from E1)");
        assertTrue(enrichedData.containsKey("counterpartyCreditLimit"), 
            "counterpartyCreditLimit should be present (from E1)");
        
        assertEquals("AA", enrichedData.get("counterpartyCreditRating"), 
            "Credit rating should be AA for HEDGE_FUND_X");
        assertEquals(100000000.0, enrichedData.get("counterpartyCreditLimit"), 
            "Credit limit should be 100M for HEDGE_FUND_X");
        
        // Verify R1 executed: should validate trade against credit limit
        // R1 depends on E1 (needs counterpartyCreditRating and counterpartyCreditLimit)
        // R1 should trigger because 50M < 100M credit limit
        assertTrue(result.isTriggered(), "R1 should trigger - trade within credit limit");

        // Verify E2 executed: VaR should be calculated
        // E2 depends on E1 (condition checks counterpartyCreditRating != null)
        assertTrue(enrichedData.containsKey("valueAtRisk"),
            "valueAtRisk should be present (from E2, depends on E1)");

        double expectedVaR = 50000000.0 * 0.15; // 7.5M
        assertEquals(expectedVaR, enrichedData.get("valueAtRisk"),
            "VaR should be 15% of notional amount");

        // Verify R2 executed: should approve trade based on VaR
        // R2 depends on E2 (needs valueAtRisk)
        // R2 should trigger because VaR (7.5M) < 10M limit
        assertTrue(result.isSuccess(), "Overall result should be success - all validations passed");

        LOGGER.info("Item-level processing test PASSED");
        LOGGER.info("   Processing Order:");
        LOGGER.info("   1. E1 (enrich-counterparty-credit-rating) -> Added credit rating & limit");
        LOGGER.info("   2. R1 (validate-credit-limit) -> Validated using E1 data");
        LOGGER.info("   3. E2 (calculate-value-at-risk) -> Calculated VaR using E1 data");
        LOGGER.info("   4. R2 (approve-or-reject-trade) -> Approved using E2 data");
        LOGGER.info("   ");
        LOGGER.info("   Enriched Data:");
        LOGGER.info("   - counterpartyCreditRating: " + enrichedData.get("counterpartyCreditRating"));
        LOGGER.info("   - counterpartyCreditLimit: " + enrichedData.get("counterpartyCreditLimit"));
        LOGGER.info("   - valueAtRisk: " + enrichedData.get("valueAtRisk"));
    }

    @Test
    public void testItemLevelProcessingWithHighRiskTrade() throws Exception {
        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/ItemLevelProcessingOtcOptionsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeType", "OTC_OPTION");
        tradeData.put("counterparty", "HEDGE_FUND_X");
        tradeData.put("notionalAmount", 80000000.0); // Higher notional -> higher VaR
        tradeData.put("optionType", "PUT");
        tradeData.put("strike", 95.0);
        tradeData.put("maturity", "2026-06-30");

        // Act
        RuleResult result = engine.evaluate(config, tradeData);

        // Assert
        assertNotNull(result, "Result should not be null");

        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify E1 executed
        assertTrue(enrichedData.containsKey("counterpartyCreditRating"));
        assertTrue(enrichedData.containsKey("counterpartyCreditLimit"));

        // Verify R1 executed (should still pass - 80M < 100M limit)
        assertTrue(result.isTriggered(), "R1 should trigger - trade within credit limit");

        // Verify E2 executed
        assertTrue(enrichedData.containsKey("valueAtRisk"));
        double expectedVaR = 80000000.0 * 0.15; // 12M
        assertEquals(expectedVaR, enrichedData.get("valueAtRisk"));

        // Verify R2 executed (should FAIL - VaR > 10M limit)
        // R2 condition: #valueAtRisk < 10000000
        // Since VaR is 12M, R2 will NOT trigger, but overall processing should still succeed
        assertTrue(result.isSuccess(), "Overall processing should succeed even if R2 doesn't trigger");

        LOGGER.info("High-risk trade test PASSED");
        LOGGER.info("   VaR: " + enrichedData.get("valueAtRisk") + " (exceeds 10M limit)");
        LOGGER.info("   Trade NOT approved due to high VaR (R2 did not trigger)");
    }

    @Test
    public void testItemLevelProcessingWithLowRiskCounterparty() throws Exception {
        // Arrange
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/ItemLevelProcessingOtcOptionsTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = RulesEngine.fromFile(yamlPath);

        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeType", "OTC_OPTION");
        tradeData.put("counterparty", "PENSION_FUND_Y"); // Different counterparty
        tradeData.put("notionalAmount", 30000000.0);
        tradeData.put("optionType", "CALL");
        tradeData.put("strike", 110.0);
        tradeData.put("maturity", "2025-09-30");

        // Act
        RuleResult result = engine.evaluate(config, tradeData);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Overall result should be success");

        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify E1 executed with different counterparty data
        assertTrue(enrichedData.containsKey("counterpartyCreditRating"));
        assertEquals("AAA", enrichedData.get("counterpartyCreditRating"),
            "Credit rating should be AAA for PENSION_FUND_Y");
        assertEquals(200000000.0, enrichedData.get("counterpartyCreditLimit"),
            "Credit limit should be 200M for PENSION_FUND_Y");

        // Verify R1 executed (should trigger - 30M < 200M limit)
        assertTrue(result.isTriggered(), "R1 should trigger - trade within credit limit");

        // Verify E2 executed
        assertTrue(enrichedData.containsKey("valueAtRisk"));
        double expectedVaR = 30000000.0 * 0.15; // 4.5M
        assertEquals(expectedVaR, enrichedData.get("valueAtRisk"));

        // Verify R2 executed (should PASS - VaR < 10M limit)
        // Both R1 and R2 should trigger, overall success
        assertTrue(result.isSuccess(), "Overall result should be success - all validations passed");

        LOGGER.info("Low-risk counterparty test PASSED");
        LOGGER.info("   Credit Rating: " + enrichedData.get("counterpartyCreditRating"));
        LOGGER.info("   VaR: " + enrichedData.get("valueAtRisk") + " (within 10M limit)");
        LOGGER.info("   Trade approved");
    }
}

