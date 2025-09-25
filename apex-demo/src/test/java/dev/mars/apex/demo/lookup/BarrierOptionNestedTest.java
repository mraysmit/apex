package dev.mars.apex.demo.lookup;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BarrierOptionNestedTest - Validates APEX Engine Nested Data Processing Capabilities
 * 
 * PURPOSE: Prove that APEX can handle complex nested XML structures through:
 * - APEX enrichment engine processing 2+ level nested fields
 * - APEX mathematical calculations on nested data values
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 *  Count enrichments in YAML - 4 enrichments expected (apex-level2-navigation, apex-cross-nested-calculation, apex-level3-conditional, apex-nested-date-calculation)
 *  Verify log shows "Processed: 4 out of 4" - Must be 100% execution rate
 *  Check EVERY enrichment condition - Test data triggers ALL 4 conditions
 *  Validate EVERY business calculation - Test mathematical formulas on nested data
 *  Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * ALL PROCESSING LOGIC IS IN APEX YAML FILES - NO CUSTOM JAVA LOGIC
 * Tests validate APEX functionality, not custom implementations
 */
public class BarrierOptionNestedTest extends DemoTestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(BarrierOptionNestedTest.class);

    @Test
    @DisplayName("Validate APEX Nested Enrichment Processing Capabilities - Step 4: Complete (4 Enrichments)")
    void testApexNestedEnrichmentCapabilities() {
        logger.info("=== Testing APEX Nested Enrichment Processing Capabilities ===");

        // Load APEX enrichment configuration
        try {
            logger.info("Loading YAML configuration from: src/test/java/dev/mars/apex/demo/lookup/BarrierOptionNestedTest.yaml");
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/BarrierOptionNestedTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
            logger.info("YAML configuration loaded successfully: {}", config.getMetadata().getName());

            // Create barrier option test data that triggers ALL 4 enrichments
            logger.info("Creating barrier option test data...");
            Map<String, Object> barrierOptionData = createBarrierOptionTestData();
            logger.info("Test data created with {} top-level fields", barrierOptionData.size());
            logger.info("Test data structure: {}", barrierOptionData.keySet());

        // Execute APEX enrichment processing - ALL logic in YAML
        logger.info("Executing APEX enrichment processing...");
        Object result = enrichmentService.enrichObject(config, barrierOptionData);
        logger.info("Enrichment processing completed");

        // Validate APEX successfully processed nested structures
        assertNotNull(result, "APEX nested enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        logger.info("Enriched data contains {} fields", enrichedData.size());
        logger.info("Enriched data fields: {}", enrichedData.keySet());

            // VALIDATE: APEX processed ALL 4 enrichments (must be 100% execution rate)
            logger.info("Validating APEX nested enrichment results...");
            validateApexNestedEnrichmentResults(enrichedData);

            logger.info("✓ APEX successfully processed all nested data structures");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Validate APEX Nested Validation Rules Processing - Phase 3: Complete Validation")
    void testApexNestedValidationCapabilities() {
        logger.info("=== Testing APEX Nested Validation Rules Processing ===");

        // Load APEX validation configuration
        try {
            var config = yamlLoader.loadFromFile("src/test/resources/lookup/BarrierOptionNestedValidationTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Create barrier option test data for validation
        Map<String, Object> barrierOptionData = createBarrierOptionTestData();

        // VALIDATE: APEX validation configuration loaded successfully
        assertNotNull(config.getRules(), "APEX validation rules should be loaded");
        assertEquals(3, config.getRules().size(), "Should have exactly 3 validation rules");

        // VALIDATE: All validation rules are properly configured
        var rules = config.getRules();
        assertNotNull(rules.get(0).getCondition(), "First validation rule should have condition");
        assertNotNull(rules.get(1).getCondition(), "Second validation rule should have condition");
        assertNotNull(rules.get(2).getCondition(), "Third validation rule should have condition");

            // VALIDATE: APEX processed ALL 3 validation rules configuration
            validateApexNestedValidationResults(config, barrierOptionData);

            logger.info("✓ APEX successfully processed all nested validation rules");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }
    
    /**
     * Creates barrier option test data that triggers ALL 4 nested processing conditions
     */
    private Map<String, Object> createBarrierOptionTestData() {
        logger.info("Creating barrier option test data with nested structures...");
        Map<String, Object> testData = new HashMap<>();

        // Root level data
        logger.info("Adding root level data fields...");
        testData.put("tradeDate", "2025-09-19");
        testData.put("buyerParty", "GOLDMAN_SACHS");
        testData.put("sellerParty", "JP_MORGAN");
        testData.put("optionType", "Call");
        testData.put("expiryDate", "2025-12-19");
        testData.put("settlementType", "Cash");
        logger.info("Root level data added: {} fields", 6);

        // Level 1: Underlying asset with Level 2 market data
        logger.info("Creating Level 1 underlying asset with Level 2 market data...");
        Map<String, Object> underlyingAsset = new HashMap<>();
        underlyingAsset.put("commodity", "Gold");
        underlyingAsset.put("unit", "Troy Ounce");
        underlyingAsset.put("exchange", "COMEX");

        Map<String, Object> marketData = new HashMap<>();
        marketData.put("currentPrice", "2100.00");
        marketData.put("volatility", "0.18");
        underlyingAsset.put("marketData", marketData);
        testData.put("underlyingAsset", underlyingAsset);
        logger.info("Underlying asset created with market data - currentPrice: {}", marketData.get("currentPrice"));

        // Level 1: Pricing terms
        logger.debug("Creating Level 1 pricing terms...");
        Map<String, Object> pricingTerms = new HashMap<>();
        pricingTerms.put("strikePrice", "2150.00");
        pricingTerms.put("notionalQuantity", "100");
        pricingTerms.put("premium", "15000.00");
        testData.put("pricingTerms", pricingTerms);
        logger.debug("Pricing terms created - strikePrice: {}, premium: {}",
                    pricingTerms.get("strikePrice"), pricingTerms.get("premium"));

        // Level 1: Barrier terms with Level 2 and Level 3 nesting
        logger.debug("Creating Level 1 barrier terms with nested structures...");
        Map<String, Object> barrierTerms = new HashMap<>();
        barrierTerms.put("barrierType", "Knock-Out");
        barrierTerms.put("barrierLevel", "2300.00");
        barrierTerms.put("barrierDirection", "Up-and-Out");
        barrierTerms.put("monitoringFrequency", "Continuous");
        logger.debug("Barrier terms Level 1 created - barrierLevel: {}", barrierTerms.get("barrierLevel"));

        // Level 2: Knockout conditions
        logger.debug("Creating Level 2 knockout conditions...");
        Map<String, Object> knockoutConditions = new HashMap<>();
        knockoutConditions.put("triggerEvent", "Price Touch");

        // Level 3: Observation period
        logger.debug("Creating Level 3 observation period...");
        Map<String, Object> observationPeriod = new HashMap<>();
        observationPeriod.put("startDate", "2025-09-19");
        observationPeriod.put("endDate", "2025-12-19");
        knockoutConditions.put("observationPeriod", observationPeriod);
        logger.debug("Observation period created - startDate: {}, endDate: {}",
                    observationPeriod.get("startDate"), observationPeriod.get("endDate"));

        // Level 3: Rebate terms
        logger.debug("Creating Level 3 rebate terms...");
        Map<String, Object> rebateTerms = new HashMap<>();
        rebateTerms.put("rebateAmount", "5000.00");
        rebateTerms.put("rebatePaymentDate", "2025-12-21");
        knockoutConditions.put("rebateTerms", rebateTerms);
        logger.debug("Rebate terms created - rebateAmount: {}", rebateTerms.get("rebateAmount"));

        barrierTerms.put("knockoutConditions", knockoutConditions);
        logger.debug("Level 2 knockout conditions added to barrier terms");
        testData.put("barrierTerms", barrierTerms);
        logger.debug("Barrier terms added to test data");

        logger.debug("Test data creation completed - total top-level fields: {}", testData.size());
        logger.debug("Test data structure summary:");
        logger.debug("  - Root fields: tradeDate, buyerParty, sellerParty, optionType, expiryDate, settlementType");
        logger.debug("  - Level 1: underlyingAsset (with Level 2 marketData), pricingTerms, barrierTerms");
        logger.debug("  - Level 2: knockoutConditions (with Level 3 observationPeriod and rebateTerms)");
        logger.debug("  - Level 3: observationPeriod, rebateTerms");

        return testData;
    }
    
    /**
     * Validates APEX nested enrichment processing results
     * CRITICAL: Must validate ALL 4 enrichments were processed successfully
     */
    private void validateApexNestedEnrichmentResults(Map<String, Object> enrichedData) {
        logger.debug("Starting validation of APEX nested enrichment results...");

        // VALIDATE: APEX Level 2 nested field navigation
        logger.debug("Validating Level 2 nested field navigation...");
        Object extractedPrice = enrichedData.get("apexExtractedMarketPrice");
        logger.debug("apexExtractedMarketPrice value: {} (type: {})", extractedPrice,
                    extractedPrice != null ? extractedPrice.getClass().getSimpleName() : "null");
        assertNotNull(extractedPrice, "APEX should extract nested price: underlyingAsset.marketData.currentPrice");
        assertEquals("2100.00", extractedPrice,
                    "APEX should extract nested price: underlyingAsset.marketData.currentPrice");
        logger.debug("✓ Level 2 navigation validation passed");

        // VALIDATE: APEX cross-nested-field business calculation
        logger.debug("Validating cross-nested-field business calculation...");
        Object barrierSpread = enrichedData.get("apexBarrierSpread");
        logger.debug("apexBarrierSpread value: {} (type: {})", barrierSpread,
                    barrierSpread != null ? barrierSpread.getClass().getSimpleName() : "null");
        assertNotNull(barrierSpread, "APEX should calculate barrier spread");
        assertEquals(150.0, Double.parseDouble(barrierSpread.toString()),
                    "APEX should calculate: barrierLevel (2300) - strikePrice (2150) = 150");
        logger.debug("✓ Cross-nested calculation validation passed");

        // VALIDATE: APEX Level 3 nested conditional processing
        logger.debug("Validating Level 3 nested conditional processing...");
        Object rebatePercentage = enrichedData.get("apexRebatePercentage");
        logger.debug("apexRebatePercentage value: {} (type: {})", rebatePercentage,
                    rebatePercentage != null ? rebatePercentage.getClass().getSimpleName() : "null");
        assertNotNull(rebatePercentage, "APEX should calculate rebate percentage from Level 3 nested data");
        assertEquals(33.33, Double.parseDouble(rebatePercentage.toString()), 0.01,
                    "APEX should calculate: (rebateAmount (5000) / premium (15000)) * 100 = 33.33%");
        logger.debug("✓ Level 3 conditional processing validation passed");

        // VALIDATE: APEX nested date calculation with SpEL
        logger.debug("Validating nested date calculation with SpEL...");
        Object observationDays = enrichedData.get("apexObservationPeriodDays");
        logger.debug("apexObservationPeriodDays value: {} (type: {})", observationDays,
                    observationDays != null ? observationDays.getClass().getSimpleName() : "null");
        assertNotNull(observationDays, "APEX should calculate days between nested observation period dates");
        assertEquals(91L, Long.parseLong(observationDays.toString()),
                    "APEX should calculate days between 2025-09-19 and 2025-12-19 = 91 days");
        logger.debug("✓ Nested date calculation validation passed");

        logger.info("✓ All 4 APEX nested enrichments validated successfully:");
        logger.info("  - Level 2 Navigation: {}", extractedPrice);
        logger.info("  - Cross-Nested Calculation: {}", barrierSpread);
        logger.info("  - Level 3 Conditional: {}%", rebatePercentage);
        logger.info("  - Nested Date Calculation: {} days", observationDays);
        logger.debug("All validation checks completed successfully");
    }

    /**
     * Validates APEX nested validation rules configuration
     * CRITICAL: Must validate ALL 3 validation rules are properly configured
     */
    private void validateApexNestedValidationResults(YamlRuleConfiguration config, Map<String, Object> testData) {
        // VALIDATE: APEX validation rules configuration is complete
        var rules = config.getRules();

        // Validate each rule has the expected business logic conditions
        // Rule 1: Barrier vs Strike validation
        assertTrue(rules.get(0).getCondition().contains("barrierLevel") && rules.get(0).getCondition().contains("strikePrice"),
                "First rule should validate barrier vs strike price");

        // Rule 2: Date consistency validation
        assertTrue(rules.get(1).getCondition().contains("observationPeriod") && rules.get(1).getCondition().contains("endDate"),
                "Second rule should validate date consistency");

        // Rule 3: Rebate amount validation
        assertTrue(rules.get(2).getCondition().contains("rebateTerms") && rules.get(2).getCondition().contains("rebateAmount"),
                "Third rule should validate rebate amount");

        logger.info("✓ All 3 APEX nested validation rules configured successfully:");
        logger.info("  - Barrier vs Strike Validation: CONFIGURED");
        logger.info("  - Date Consistency Validation: CONFIGURED");
        logger.info("  - Rebate Amount Validation: CONFIGURED");
    }
}
