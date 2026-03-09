package dev.mars.apex.demo.codes;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
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
 * TradeValidationCodesDemo - Real-world demonstration of success/error codes and field mapping
 * in OTC options trade validation and processing.
 * 
 * BUSINESS SCENARIO: Middle Office Trade Processing
 * - Validates incoming OTC option trades for completeness and correctness
 * - Enriches trades with market data (volatility, pricing, Greeks)
 * - Applies business rules with detailed status codes for audit trail
 * - Maps validation results to specific fields for downstream processing
 * - Determines trade processing actions based on validation outcomes
 * 
 * DEMONSTRATES:
 * - success-code: Constant and SpEL expression codes for enrichments and rules
 * - error-code: Error codes when validations fail or data is missing
 * - map-to-field: Mapping codes and derived values to audit/status fields
 * - Real-world business logic: Trade validation, risk assessment, approval workflows
 * 
 * FOLLOWS APEX PRINCIPLES:
 * - All business logic in YAML configuration
 * - Tests actual functionality with real APEX services
 * - Validates business outcomes, not just YAML syntax
 * - Uses realistic financial domain data (OTC options)
 * 
 * @author APEX Demo
 * @since 1.0.0
 */
@DisplayName("Trade Validation with Success/Error Codes Demo")
public class TradeValidationCodesDemo extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(TradeValidationCodesDemo.class);
    private static final String CONFIG_FILE = "src/test/java/dev/mars/apex/demo/codes/TradeValidationCodesDemo.yaml";

    private YamlRuleConfiguration config;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        try {
            // Load YAML configuration
            ConfigurationLoader loader = new ConfigurationLoader();
            config = loader.loadFromFile(CONFIG_FILE);
            assertNotNull(config, "Configuration should not be null");

            logger.info("[OK] Configuration loaded: {} enrichments, {} rules",
                       config.getEnrichments() != null ? config.getEnrichments().size() : 0,
                       config.getRules() != null ? config.getRules().size() : 0);
        } catch (Exception e) {
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Test 1: Valid SPX Call Option - Demonstrates enrichments with success/error codes
     */
    @Test
    @DisplayName("Test 1: Valid SPX Call Option - Enrichment Success Codes Demo")
    public void testValidSpxCallOption() {
        logger.info("=== Test 1: Valid SPX Call Option - Enrichment Success Codes Demo ===");
        logger.info("Scenario: Standard SPX call option with normal volatility and notional");
        logger.info("Expected: Enrichments populate status fields via success codes and map-to-field");

        try {
            // Create valid SPX call option trade
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("tradeId", "TRD-SPX-001");
            tradeData.put("optionType", "CALL");
            tradeData.put("underlyingSymbol", "SPX");
            tradeData.put("strikePrice", 4600.00);
            tradeData.put("quantity", 100);
            tradeData.put("expiryDate", "2024-12-20");

            logger.info("Input trade data: {}", tradeData);

            // Execute APEX processing
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, tradeData);

        assertNotNull(result, "RuleResult should not be null");
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");

        logger.info("\n=== ENRICHMENT RESULTS ===");
        
        // Validate pricing enrichment
        assertEquals(18.5, enrichedData.get("impliedVolatility"), "Implied volatility should be enriched");
        assertEquals(4500.00, enrichedData.get("underlyingPrice"), "Underlying price should be enriched");
        assertEquals("PRICING_ENRICHED_NORMAL_VOL", enrichedData.get("pricingEnrichmentStatus"),
                    "Pricing enrichment status should indicate normal volatility");
        logger.info("[OK] Pricing enriched: IV=18.5%, Underlying=$4500, Status=PRICING_ENRICHED_NORMAL_VOL");

        // Validate Greeks calculation
        assertNotNull(enrichedData.get("optionDelta"), "Option delta should be calculated");
        assertEquals("GREEKS_CALCULATED_OTM", enrichedData.get("greeksCalculationStatus"),
                    "Greeks calculation status should indicate OTM");
        assertEquals("LOW_DELTA", enrichedData.get("riskCategory"),
                    "Risk category should be LOW_DELTA for OTM option");
        logger.info("[OK] Greeks calculated: Delta={}, Status=GREEKS_CALCULATED_OTM, Risk=LOW_DELTA",
                   enrichedData.get("optionDelta"));

        // Validate notional calculation
        assertEquals(460000.0, enrichedData.get("tradeNotional"), "Notional should be calculated correctly");
        assertEquals("NOTIONAL_CALCULATED_NORMAL", enrichedData.get("notionalCalculationStatus"),
                    "Notional calculation status should indicate normal size");
        assertEquals("TIER_2_STANDARD", enrichedData.get("notionalTier"),
                    "Notional tier should be TIER_2_STANDARD");
        logger.info("[OK] Notional calculated: $460,000, Status=NOTIONAL_CALCULATED_NORMAL, Tier=TIER_2_STANDARD");

        logger.info("\n=== RULE VALIDATION RESULTS ===");

        // Validate required fields (first matching rule will execute)
        assertEquals("REQUIRED_FIELDS_VALID", enrichedData.get("requiredFieldsValidation"),
                    "Required fields validation should pass");
        logger.info("[OK] Required fields: REQUIRED_FIELDS_VALID");

        logger.info("\n=== SUMMARY ===");
        logger.info("This demo shows success/error codes working for:");
        logger.info("  - 3 enrichments with dynamic codes and field mappings");
        logger.info("  - 1 rule with success code and field mapping");
        logger.info("All status fields were successfully populated via map-to-field directives");

        logger.info("\nTest 1 PASSED: Valid SPX call option ready for auto-booking");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Test 2: Large TSLA Put Option - Demonstrates high volatility and large notional codes
     */
    @Test
    @DisplayName("Test 2: Large TSLA Put Option - High Volatility Demo")
    public void testLargeTeslaPutOption() {
        logger.info("=== Test 2: Large TSLA Put Option - High Volatility Demo ===");
        logger.info("Scenario: Large TSLA put option with high volatility and large notional");
        logger.info("Expected: Enrichments populate different status codes based on thresholds");

        try {
            // Create large TSLA put option trade
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("tradeId", "TRD-TSLA-002");
            tradeData.put("optionType", "PUT");
            tradeData.put("underlyingSymbol", "TSLA");
            tradeData.put("strikePrice", 250.00);
            tradeData.put("quantity", 50000);  // Large quantity
            tradeData.put("expiryDate", "2024-12-20");

            logger.info("Input trade data: {}", tradeData);

            // Execute APEX processing
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, tradeData);

        assertNotNull(result, "RuleResult should not be null");
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");

        logger.info("\n=== ENRICHMENT RESULTS ===");

        // Validate pricing enrichment - TSLA has high volatility
        assertEquals(45.8, enrichedData.get("impliedVolatility"), "Implied volatility should be enriched");
        assertEquals("PRICING_ENRICHED_HIGH_VOL", enrichedData.get("pricingEnrichmentStatus"),
                    "Pricing enrichment status should indicate high volatility");
        logger.info("[OK] Pricing enriched: IV=45.8%, Status=PRICING_ENRICHED_HIGH_VOL (HIGH VOLATILITY)");

        // Validate notional calculation - Large notional
        assertEquals(12500000.0, enrichedData.get("tradeNotional"), "Notional should be calculated correctly");
        assertEquals("NOTIONAL_CALCULATED_LARGE", enrichedData.get("notionalCalculationStatus"),
                    "Notional calculation status should indicate large size");
        assertEquals("TIER_1_LARGE", enrichedData.get("notionalTier"),
                    "Notional tier should be TIER_1_LARGE");
        logger.info("[OK] Notional calculated: $12,500,000, Status=NOTIONAL_CALCULATED_LARGE, Tier=TIER_1_LARGE");

        logger.info("\n=== SUMMARY ===");
        logger.info("This test demonstrates dynamic success codes based on data thresholds:");
        logger.info("  - High volatility (45.8%) triggers PRICING_ENRICHED_HIGH_VOL");
        logger.info("  - Large notional ($12.5M) triggers NOTIONAL_CALCULATED_LARGE and TIER_1_LARGE");
        logger.info("Success codes adapt to business conditions automatically");

        logger.info("\nTest 2 PASSED: High volatility codes demonstrated");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Test 3: Invalid Trade - Demonstrates error codes
     */
    @Test
    @DisplayName("Test 3: Invalid Trade - Error Code Demo")
    public void testInvalidTradeMissingFields() {
        logger.info("=== Test 3: Invalid Trade - Error Code Demo ===");
        logger.info("Scenario: Trade with missing required fields");
        logger.info("Expected: Rule evaluation produces error code instead of success code");

        try {
            // Create invalid trade with missing fields
            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("tradeId", "TRD-INVALID-003");
            tradeData.put("optionType", "CALL");
            // Missing: underlyingSymbol, strikePrice, quantity, expiryDate

            logger.info("Input trade data (incomplete): {}", tradeData);

            // Execute APEX processing
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(config, tradeData);

        assertNotNull(result, "RuleResult should not be null");
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");

        logger.info("\n=== RULE VALIDATION RESULTS ===");

        // Validate required fields - SHOULD FAIL (error code instead of success code)
        assertEquals("REQUIRED_FIELDS_MISSING", enrichedData.get("requiredFieldsValidation"),
                    "Required fields validation should fail with error code");
        logger.info("[OK] Required fields: REQUIRED_FIELDS_MISSING (ERROR CODE)");

        logger.info("\n=== SUMMARY ===");
        logger.info("This test demonstrates error codes:");
        logger.info("  - When rule condition fails, error-code is evaluated instead of success-code");
        logger.info("  - map-to-field uses #error_code variable to populate validation status");
        logger.info("  - Result: requiredFieldsValidation = 'REQUIRED_FIELDS_MISSING'");

        logger.info("\nTest 3 PASSED: Error code correctly populated");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}

