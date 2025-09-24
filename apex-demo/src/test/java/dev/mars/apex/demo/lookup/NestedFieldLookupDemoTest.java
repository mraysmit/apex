package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;

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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for NestedFieldLookupDemo functionality.
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * Count enrichments in YAML - 4 enrichments expected (nested-counterparty-country-extraction, country-settlement-lookup, nested-instrument-pricing-extraction, nested-trade-value-calculation)
 * Verify log shows "Processed: 4 out of 4" - Must be 100% execution rate
 * Check EVERY enrichment condition - Test data triggers ALL 4 conditions
 * Validate EVERY business calculation - Test actual nested field navigation and calculations
 * Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * BUSINESS LOGIC VALIDATION:
 * - Real nested field navigation: trade.counterparty.countryCode
 * - Real nested field navigation: trade.instrument.pricing.price
 * - Country-specific settlement information lookup using extracted country code
 * - Mathematical calculations using nested field values
 */
public class NestedFieldLookupDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(NestedFieldLookupDemoTest.class);

    /**
     * Tests comprehensive nested field lookup functionality with all 4 enrichments.
     *
     * This test demonstrates APEX's ability to:
     * 1. Navigate nested object structures (trade.counterparty.countryCode)
     * 2. Extract values from deeply nested fields (trade.instrument.pricing.price)
     * 3. Use extracted values for lookup operations (country -> settlement system)
     * 4. Perform mathematical calculations using nested field values
     *
     * Test Data Structure:
     * - trade.counterparty.countryCode: "US"
     * - trade.instrument.pricing.price: "150.75"
     * - trade.quantity: "1000"
     *
     * Expected Processing:
     * - Enrichment 1: Extract "US" from nested counterparty structure
     * - Enrichment 2: Lookup settlement info for "US" -> "DTC", "United States"
     * - Enrichment 3: Extract price 150.75 from nested pricing structure
     * - Enrichment 4: Calculate trade value: 1000 * 150.75 = 150750.0
     */
    @Test
    void testComprehensiveNestedFieldLookupFunctionality() {
        logger.info("=== Testing Comprehensive Nested Field Lookup Functionality ===");

        // Load YAML configuration containing 4 enrichments for nested field processing
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Create nested trade data structure with 3-level hierarchy:
        // trade -> counterparty -> countryCode (Level 2)
        // trade -> instrument -> pricing -> price (Level 3)
        Map<String, Object> testData = createNestedTradeData("US", "150.75", "1000");

        // Execute APEX enrichment processing - should trigger all 4 enrichments
        Object result = enrichmentService.enrichObject(config, testData);

        // Validate that enrichment processing completed successfully
        assertNotNull(result, "Nested field lookup enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate that ALL 4 enrichments processed successfully
        // Each enrichment should have generated its expected result field
        assertNotNull(enrichedData.get("extractedCountryCode"), "Enrichment 1: Country code extraction from nested structure");
        assertNotNull(enrichedData.get("settlementCountryName"), "Enrichment 2: Country name lookup based on extracted code");
        assertNotNull(enrichedData.get("settlementSystem"), "Enrichment 2: Settlement system lookup based on extracted code");
        assertNotNull(enrichedData.get("extractedPrice"), "Enrichment 3: Price extraction from nested pricing structure");
        assertNotNull(enrichedData.get("calculatedTradeValue"), "Enrichment 4: Mathematical calculation using nested values");

        // Validate the actual business logic results from nested field navigation
        // Enrichment 1: Nested field extraction - trade.counterparty.countryCode
        assertEquals("US", enrichedData.get("extractedCountryCode"),
            "Should extract 'US' from trade.counterparty.countryCode using SpEL expression");

        // Enrichment 2: Lookup enrichment using extracted country code
        assertEquals("United States", enrichedData.get("settlementCountryName"),
            "Should lookup 'United States' for country code 'US' from inline dataset");
        assertEquals("DTC", enrichedData.get("settlementSystem"),
            "Should lookup 'DTC' settlement system for US from inline dataset");

        // Enrichment 3: Nested field extraction - trade.instrument.pricing.price
        assertEquals(150.75, enrichedData.get("extractedPrice"),
            "Should extract 150.75 from trade.instrument.pricing.price using SpEL expression");

        // Enrichment 4: Mathematical calculation using extracted nested values
        assertEquals(150750.0, enrichedData.get("calculatedTradeValue"),
            "Should calculate trade value: quantity (1000) * extracted price (150.75) = 150750.0");

            logger.info("Comprehensive nested field lookup functionality test completed successfully");
            logger.info("   - Extracted country code: {}", enrichedData.get("extractedCountryCode"));
            logger.info("   - Settlement system: {}", enrichedData.get("settlementSystem"));
            logger.info("   - Extracted price: {}", enrichedData.get("extractedPrice"));
            logger.info("   - Calculated trade value: {}", enrichedData.get("calculatedTradeValue"));
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    /**
     * Tests nested field navigation across multiple countries and settlement systems.
     *
     * This test validates that APEX can consistently:
     * - Extract country codes from nested trade structures (trade.counterparty.countryCode)
     * - Perform lookup operations using extracted values as keys
     * - Handle different countries with their respective settlement systems
     *
     * Test Coverage:
     * - US -> DTC (Depository Trust Company)
     * - GB -> CREST (UK settlement system)
     * - DE -> CBF (Clearstream Banking Frankfurt)
     * - JP -> JASDEC (Japan Securities Depository Center)
     *
     * This demonstrates real-world financial settlement system mapping
     * based on counterparty country codes extracted from nested trade data.
     */
    @Test
    void testNestedFieldNavigationProcessing() {
        logger.info("=== Testing Nested Field Navigation Processing ===");

        // Load YAML configuration containing nested field navigation rules
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Test data: Country codes and their corresponding settlement infrastructure
        // This represents real financial market settlement systems used globally
        String[] countryCodes = {"US", "GB", "DE", "JP"};
        String[] expectedCountryNames = {"United States", "United Kingdom", "Germany", "Japan"};
        String[] expectedSettlementSystems = {"DTC", "CREST", "CBF", "JASDEC"};

        // Process each country to validate consistent nested field navigation
        for (int i = 0; i < countryCodes.length; i++) {
            String countryCode = countryCodes[i];
            String expectedCountryName = expectedCountryNames[i];
            String expectedSettlementSystem = expectedSettlementSystems[i];

            logger.info("Testing nested field navigation for country: {}", countryCode);

            // Create nested trade data structure with specific country code
            // Structure: trade.counterparty.countryCode = countryCode
            Map<String, Object> testData = createNestedTradeData(countryCode, "100.50", "500");

            // Execute APEX enrichment processing with nested field navigation
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Nested field navigation result should not be null for country: " + countryCode);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate nested field extraction: trade.counterparty.countryCode -> extractedCountryCode
            assertEquals(countryCode, enrichedData.get("extractedCountryCode"),
                "Should extract '" + countryCode + "' from nested counterparty structure");

            // Validate lookup operations using extracted nested field value as lookup key
            assertEquals(expectedCountryName, enrichedData.get("settlementCountryName"),
                "Should lookup '" + expectedCountryName + "' for country code '" + countryCode + "'");
            assertEquals(expectedSettlementSystem, enrichedData.get("settlementSystem"),
                "Should lookup '" + expectedSettlementSystem + "' settlement system for '" + countryCode + "'");

            logger.info("Country {} processed successfully - extracted: {}, settlement: {}",
                countryCode, enrichedData.get("extractedCountryCode"), enrichedData.get("settlementSystem"));
        }
        
            logger.info("Nested field navigation processing test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    /**
     * Tests nested field extraction and mathematical calculations using extracted values.
     *
     * This test validates APEX's ability to:
     * - Extract price values from deeply nested structures (trade.instrument.pricing.price)
     * - Use extracted nested values in mathematical calculations
     * - Perform real-time trade value calculations: quantity * extracted_price
     *
     * Test Scenarios:
     * - US: 1000 shares * $150.75 = $150,750.00
     * - GB: 500 shares * $200.50 = $100,250.00
     * - DE: 2000 shares * $75.25 = $150,500.00
     * - JP: 100 shares * $300.00 = $30,000.00
     *
     * This demonstrates real-world financial calculations where trade values
     * are computed using pricing data extracted from nested instrument structures.
     */
    @Test
    void testNestedPricingCalculations() {
        logger.info("=== Testing Nested Pricing Calculations ===");

        // Load YAML configuration containing nested field extraction and calculation rules
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Test cases with different price/quantity combinations for mathematical validation
        // Each test case: [countryCode, price, quantity] -> expected trade value calculation
        String[][] testCases = {
            {"US", "150.75", "1000"},  // Expected: 1000 * 150.75 = 150750.0
            {"GB", "200.50", "500"},   // Expected: 500 * 200.50 = 100250.0
            {"DE", "75.25", "2000"},   // Expected: 2000 * 75.25 = 150500.0
            {"JP", "300.00", "100"}    // Expected: 100 * 300.00 = 30000.0
        };

        // Process each test case to validate nested field extraction and calculations
        for (String[] testCase : testCases) {
            String countryCode = testCase[0];
            String price = testCase[1];
            String quantity = testCase[2];
            // Calculate expected result for validation
            double expectedTradeValue = Double.parseDouble(price) * Double.parseDouble(quantity);

            logger.info("Testing nested calculations for country: {}, price: {}, quantity: {}", countryCode, price, quantity);

            // Create nested trade data with specific pricing information
            // Structure: trade.instrument.pricing.price = price, trade.quantity = quantity
            Map<String, Object> testData = createNestedTradeData(countryCode, price, quantity);

            // Execute APEX enrichment processing with nested field calculations
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Nested pricing calculation result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate nested field extraction: trade.instrument.pricing.price -> extractedPrice
            assertEquals(Double.parseDouble(price), enrichedData.get("extractedPrice"),
                "Should extract price " + price + " from nested instrument.pricing.price structure");

            // Validate mathematical calculation using extracted nested field values
            // Formula: trade.quantity * extracted_price = calculated_trade_value
            assertEquals(expectedTradeValue, enrichedData.get("calculatedTradeValue"),
                "Should calculate trade value: " + quantity + " * " + price + " = " + expectedTradeValue);

            logger.info("Calculations completed - extracted price: {}, calculated value: {}",
                enrichedData.get("extractedPrice"), enrichedData.get("calculatedTradeValue"));
        }

            logger.info("Nested pricing calculations test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    /**
     * Tests the complete nested field workflow with all 4 enrichments processing together.
     *
     * This comprehensive test validates the entire APEX nested field processing pipeline:
     * 1. Nested Field Extraction: trade.counterparty.countryCode -> "US"
     * 2. Lookup Enrichment: "US" -> {"countryName": "United States", "settlementSystem": "DTC"}
     * 3. Nested Price Extraction: trade.instrument.pricing.price -> 125.50
     * 4. Mathematical Calculation: quantity (800) * extracted_price (125.50) = 100400.0
     *
     * This test demonstrates a complete real-world scenario where:
     * - Trade data arrives in nested structures (typical of financial systems)
     * - Country codes are extracted from counterparty information
     * - Settlement systems are determined based on extracted country codes
     * - Pricing information is extracted from nested instrument data
     * - Trade values are calculated using extracted nested field values
     *
     * Expected Result: All 4 enrichments process successfully with correct business logic.
     */
    @Test
    void testCompleteNestedFieldWorkflow() {
        logger.info("=== Testing Complete Nested Field Workflow ===");

        // Load YAML configuration containing all 4 nested field enrichments
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

        // Create comprehensive test data that triggers all 4 enrichments
        // This represents a complete trade record with nested counterparty and instrument data
        Map<String, Object> testData = createNestedTradeData("US", "125.50", "800");

        // Execute complete APEX enrichment workflow - all 4 enrichments should process
        Object result = enrichmentService.enrichObject(config, testData);
        assertNotNull(result, "Complete nested field workflow result should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate that ALL 4 enrichments processed successfully (non-null results)
        assertNotNull(enrichedData.get("extractedCountryCode"), "Enrichment 1: Nested country code extraction should succeed");
        assertNotNull(enrichedData.get("settlementCountryName"), "Enrichment 2: Country name lookup should succeed");
        assertNotNull(enrichedData.get("extractedPrice"), "Enrichment 3: Nested price extraction should succeed");
        assertNotNull(enrichedData.get("calculatedTradeValue"), "Enrichment 4: Mathematical trade value calculation should succeed");

        // Validate the complete end-to-end workflow with specific business logic results
        // Enrichment 1: Extract country code from nested counterparty structure
        assertEquals("US", enrichedData.get("extractedCountryCode"),
            "Should extract 'US' from trade.counterparty.countryCode nested path");

        // Enrichment 2: Lookup settlement information using extracted country code
        assertEquals("United States", enrichedData.get("settlementCountryName"),
            "Should lookup 'United States' country name for extracted code 'US'");
        assertEquals("DTC", enrichedData.get("settlementSystem"),
            "Should lookup 'DTC' settlement system for extracted code 'US'");

        // Enrichment 3: Extract price from nested instrument pricing structure
        assertEquals(125.50, enrichedData.get("extractedPrice"),
            "Should extract 125.50 from trade.instrument.pricing.price nested path");

        // Enrichment 4: Calculate trade value using extracted price and original quantity
        assertEquals(100400.0, enrichedData.get("calculatedTradeValue"),
            "Should calculate trade value: quantity (800) * extracted_price (125.50) = 100400.0");

        logger.info("Complete workflow validated:");
        logger.info("   - Nested field extraction: {} -> {}", "trade.counterparty.countryCode", enrichedData.get("extractedCountryCode"));
        logger.info("   - Settlement lookup: {} -> {}", enrichedData.get("extractedCountryCode"), enrichedData.get("settlementSystem"));
        logger.info("   - Price extraction: {} -> {}", "trade.instrument.pricing.price", enrichedData.get("extractedPrice"));
        logger.info("   - Trade calculation: {} * {} = {}", 800, enrichedData.get("extractedPrice"), enrichedData.get("calculatedTradeValue"));

            logger.info("Complete nested field workflow test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    /**
     * Creates a realistic nested trade data structure for testing APEX nested field navigation.
     *
     * This helper method constructs a 3-level nested HashMap structure that mimics
     * real-world financial trade data with nested counterparty and instrument information.
     *
     * Data Structure Created:
     * - Level 1: trade (root level)
     *   - Level 2: trade.counterparty (nested counterparty info)
     *     - countryCode: Used for settlement system lookup
     *   - Level 2: trade.instrument (nested instrument info)
     *     - Level 3: trade.instrument.pricing (deeply nested pricing info)
     *       - price: Used for trade value calculations
     *
     * APEX Navigation Paths Tested:
     * - trade['counterparty']['countryCode'] -> SpEL expression for country extraction
     * - trade['instrument']['pricing']['price'] -> SpEL expression for price extraction
     *
     * @param countryCode The country code to embed in counterparty structure (e.g., "US", "GB")
     * @param price The price to embed in nested pricing structure (e.g., "150.75")
     * @param quantity The trade quantity for calculations (e.g., "1000")
     * @return Nested HashMap structure ready for APEX enrichment processing
     */
    private Map<String, Object> createNestedTradeData(String countryCode, String price, String quantity) {
        logger.info("Creating nested trade data with countryCode: {}, price: {}, quantity: {}", countryCode, price, quantity);

        // Root data structure - represents the complete trade record
        Map<String, Object> testData = new HashMap<>();

        // Level 1: Main trade object containing all trade information
        Map<String, Object> trade = new HashMap<>();
        testData.put("trade", trade);

        // Level 1: Basic trade attributes (non-nested data)
        trade.put("tradeId", "TRD001");
        trade.put("quantity", quantity);  // Used in mathematical calculations
        trade.put("tradeDate", "2025-09-22");

        // Level 2: Nested counterparty structure - represents trading partner information
        Map<String, Object> counterparty = new HashMap<>();
        counterparty.put("counterpartyId", "CP001");
        counterparty.put("counterpartyName", "Goldman Sachs");
        counterparty.put("countryCode", countryCode);  // TARGET: Extracted via trade['counterparty']['countryCode']
        trade.put("counterparty", counterparty);

        // Level 2: Nested instrument structure - represents financial instrument details
        Map<String, Object> instrument = new HashMap<>();
        instrument.put("instrumentId", "AAPL");
        instrument.put("instrumentType", "EQUITY");

        // Level 3: Deeply nested pricing structure within instrument
        Map<String, Object> pricing = new HashMap<>();
        pricing.put("price", price);  // TARGET: Extracted via trade['instrument']['pricing']['price']
        pricing.put("currency", "USD");
        instrument.put("pricing", pricing);  // Attach Level 3 pricing to Level 2 instrument
        trade.put("instrument", instrument);  // Attach Level 2 instrument to Level 1 trade

        // Log the created nested structure for debugging and validation
        logger.info("Created nested trade structure:");
        logger.info("  - trade.counterparty.countryCode: {}", countryCode);
        logger.info("  - trade.instrument.pricing.price: {}", price);
        logger.info("  - trade.quantity: {}", quantity);

        return testData;
    }
}
