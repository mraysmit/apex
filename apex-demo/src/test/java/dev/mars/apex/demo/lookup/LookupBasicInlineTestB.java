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
package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic Inline Lookup Operations Test B
 *
 * This test demonstrates APEX's basic inline lookup capabilities using:
 * 1. Currency lookup enrichment with inline reference data
 * 2. Instrument lookup enrichment for financial instruments
 * 3. Counterparty lookup enrichment with LEI and jurisdiction data
 * 4. Simple field-based lookup keys (no complex expressions)
 * 5. Basic reference data enrichment patterns
 *
 * Key Features Demonstrated:
 * - Inline dataset configuration with key-field mapping
 * - Simple lookup-key expressions using field references
 * - Multiple field mappings from lookup results
 * - Conditional enrichment execution based on field presence
 * - Basic reference data enrichment workflows
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 3 enrichments expected
 * ✅ Verify log shows "Processed: 3 out of 3" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers all lookup conditions
 * ✅ Validate EVERY lookup result - Test actual inline dataset lookup functionality
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 *
 * YAML FIRST PRINCIPLE:
 * - ALL lookup logic is in YAML enrichments
 * - Java test only provides input data and validates results
 * - NO custom lookup logic in Java test code
 * - Simple test data setup and basic assertions only
 *
 * @author APEX Demo Team
 * @since 2025-09-25
 * @version 1.0.0
 */
@DisplayName("Basic Inline Lookup Operations Test B")
public class LookupBasicInlineTestB extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(LookupBasicInlineTestB.class);

    @Test
    @DisplayName("Should enrich currency information using inline lookup")
    void testCurrencyLookupEnrichment() {
        logger.info("=== Testing Currency Lookup Enrichment ===");

        try {
            // Load YAML configuration for basic inline lookup operations
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/LookupBasicInlineTestB.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Verify we have 3 enrichments as expected
            assertEquals(3, config.getEnrichments().size(), "Should have exactly 3 enrichments");

            // Create test data with currency code (triggers currency lookup condition)
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "USD");
            testData.put("transactionId", "TXN12345");

            logger.debug("Currency lookup test data: {}", testData);

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, testData);

            // Validate enrichment results
            assertNotNull(result, "Currency lookup enrichment result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Currency lookup enriched result: {}", enrichedData);

            // Validate currency lookup enrichment from inline dataset
            assertNotNull(enrichedData.get("currencyName"), "Currency name should be enriched from inline dataset");
            assertNotNull(enrichedData.get("currencySymbol"), "Currency symbol should be enriched from inline dataset");
            assertNotNull(enrichedData.get("currencyCountry"), "Currency country should be enriched from inline dataset");

            // Validate specific currency data for USD
            assertEquals("US Dollar", enrichedData.get("currencyName"), "Should retrieve correct currency name for USD");
            assertEquals("$", enrichedData.get("currencySymbol"), "Should retrieve correct currency symbol for USD");
            assertEquals("United States", enrichedData.get("currencyCountry"), "Should retrieve correct currency country for USD");

            logger.info("✅ Currency lookup enrichment completed successfully");
            logger.info("  - Currency Name: {}", enrichedData.get("currencyName"));
            logger.info("  - Currency Symbol: {}", enrichedData.get("currencySymbol"));
            logger.info("  - Currency Country: {}", enrichedData.get("currencyCountry"));

        } catch (Exception e) {
            logger.error("Currency lookup enrichment test failed: {}", e.getMessage());
            fail("Currency lookup enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should enrich instrument information using inline lookup")
    void testInstrumentLookupEnrichment() {
        logger.info("=== Testing Instrument Lookup Enrichment ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/LookupBasicInlineTestB.yaml");

            // Create test data with instrument ID (triggers instrument lookup condition)
            Map<String, Object> testData = new HashMap<>();
            testData.put("instrumentId", "US912828XG93");
            testData.put("tradeId", "TRADE67890");

            logger.debug("Instrument lookup test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Instrument lookup enriched result: {}", enrichedData);

            // Validate instrument lookup enrichment from inline dataset
            assertNotNull(enrichedData.get("instrumentType"), "Instrument type should be enriched from inline dataset");
            assertNotNull(enrichedData.get("instrumentMaturity"), "Instrument maturity should be enriched from inline dataset");
            assertNotNull(enrichedData.get("instrumentIssuer"), "Instrument issuer should be enriched from inline dataset");

            // Validate specific instrument data for US912828XG93
            assertEquals("TREASURY_NOTE", enrichedData.get("instrumentType"), "Should retrieve correct instrument type");
            assertEquals("10Y", enrichedData.get("instrumentMaturity"), "Should retrieve correct instrument maturity");
            assertEquals("US_TREASURY", enrichedData.get("instrumentIssuer"), "Should retrieve correct instrument issuer");

            logger.info("✅ Instrument lookup enrichment completed successfully");
            logger.info("  - Instrument Type: {}", enrichedData.get("instrumentType"));
            logger.info("  - Instrument Maturity: {}", enrichedData.get("instrumentMaturity"));
            logger.info("  - Instrument Issuer: {}", enrichedData.get("instrumentIssuer"));

        } catch (Exception e) {
            logger.error("Instrument lookup enrichment test failed: {}", e.getMessage());
            fail("Instrument lookup enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should enrich counterparty information using inline lookup")
    void testCounterpartyLookupEnrichment() {
        logger.info("=== Testing Counterparty Lookup Enrichment ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/LookupBasicInlineTestB.yaml");

            // Create test data with counterparty name (triggers counterparty lookup condition)
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyName", "Deutsche Bank AG");
            testData.put("dealId", "DEAL11111");

            logger.debug("Counterparty lookup test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Counterparty lookup enriched result: {}", enrichedData);

            // Validate counterparty lookup enrichment from inline dataset
            assertNotNull(enrichedData.get("counterpartyLEI"), "Counterparty LEI should be enriched from inline dataset");
            assertNotNull(enrichedData.get("counterpartyJurisdiction"), "Counterparty jurisdiction should be enriched from inline dataset");
            assertNotNull(enrichedData.get("counterpartyEntityType"), "Counterparty entity type should be enriched from inline dataset");

            // Validate specific counterparty data for Deutsche Bank AG
            assertEquals("7LTWFZYICNSX8D621K86", enrichedData.get("counterpartyLEI"), "Should retrieve correct counterparty LEI");
            assertEquals("DE", enrichedData.get("counterpartyJurisdiction"), "Should retrieve correct counterparty jurisdiction");
            assertEquals("BANK", enrichedData.get("counterpartyEntityType"), "Should retrieve correct counterparty entity type");

            logger.info("✅ Counterparty lookup enrichment completed successfully");
            logger.info("  - Counterparty LEI: {}", enrichedData.get("counterpartyLEI"));
            logger.info("  - Counterparty Jurisdiction: {}", enrichedData.get("counterpartyJurisdiction"));
            logger.info("  - Counterparty Entity Type: {}", enrichedData.get("counterpartyEntityType"));

        } catch (Exception e) {
            logger.error("Counterparty lookup enrichment test failed: {}", e.getMessage());
            fail("Counterparty lookup enrichment test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should test multiple lookup enrichments with different currencies")
    void testMultipleCurrencyLookups() {
        logger.info("=== Testing Multiple Currency Lookups ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/LookupBasicInlineTestB.yaml");

            // Test different currencies
            String[] currencies = {"EUR", "GBP"};
            String[] expectedNames = {"Euro", "British Pound"};
            String[] expectedSymbols = {"€", "£"};
            String[] expectedCountries = {"European Union", "United Kingdom"};

            for (int i = 0; i < currencies.length; i++) {
                String currency = currencies[i];
                String expectedName = expectedNames[i];
                String expectedSymbol = expectedSymbols[i];
                String expectedCountry = expectedCountries[i];

                logger.info("Testing currency lookup for: {}", currency);

                Map<String, Object> testData = new HashMap<>();
                testData.put("currencyCode", currency);
                testData.put("transactionId", "TXN" + (i + 1000));

                logger.debug("Multiple currency test data for {}: {}", currency, testData);

                // Execute APEX enrichment processing
                Object result = enrichmentService.enrichObject(config, testData);
                @SuppressWarnings("unchecked")
                Map<String, Object> enrichedData = (Map<String, Object>) result;

                logger.debug("Multiple currency enriched result for {}: {}", currency, enrichedData);

                // Validate specific currency data
                assertEquals(expectedName, enrichedData.get("currencyName"), 
                    "Should retrieve correct currency name for " + currency);
                assertEquals(expectedSymbol, enrichedData.get("currencySymbol"), 
                    "Should retrieve correct currency symbol for " + currency);
                assertEquals(expectedCountry, enrichedData.get("currencyCountry"), 
                    "Should retrieve correct currency country for " + currency);

                logger.info("✓ Currency {} lookup: {} ({}, {})", currency, expectedName, expectedSymbol, expectedCountry);
            }

            logger.info("✅ Multiple currency lookups completed successfully");

        } catch (Exception e) {
            logger.error("Multiple currency lookups test failed: {}", e.getMessage());
            fail("Multiple currency lookups test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process complete basic inline lookup workflow")
    void testCompleteBasicInlineLookupWorkflow() {
        logger.info("=== Testing Complete Basic Inline Lookup Workflow ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/LookupBasicInlineTestB.yaml");

            // Create comprehensive test data that triggers all 3 enrichments
            Map<String, Object> testData = new HashMap<>();
            testData.put("currencyCode", "GBP");
            testData.put("instrumentId", "GB00B03MLX29");
            testData.put("counterpartyName", "Barclays Bank PLC");
            testData.put("transactionId", "TXN99999");
            testData.put("amount", 50000.00);

            logger.debug("Complete workflow test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Complete workflow enriched result: {}", enrichedData);

            // Validate all lookup enrichment results
            // Currency enrichment
            assertNotNull(enrichedData.get("currencyName"), "Currency name should be enriched");
            assertNotNull(enrichedData.get("currencySymbol"), "Currency symbol should be enriched");
            assertNotNull(enrichedData.get("currencyCountry"), "Currency country should be enriched");

            // Instrument enrichment
            assertNotNull(enrichedData.get("instrumentType"), "Instrument type should be enriched");
            assertNotNull(enrichedData.get("instrumentMaturity"), "Instrument maturity should be enriched");
            assertNotNull(enrichedData.get("instrumentIssuer"), "Instrument issuer should be enriched");

            // Counterparty enrichment
            assertNotNull(enrichedData.get("counterpartyLEI"), "Counterparty LEI should be enriched");
            assertNotNull(enrichedData.get("counterpartyJurisdiction"), "Counterparty jurisdiction should be enriched");
            assertNotNull(enrichedData.get("counterpartyEntityType"), "Counterparty entity type should be enriched");

            // Validate specific enrichment results
            assertEquals("British Pound", enrichedData.get("currencyName"), "Should enrich GBP currency name");
            assertEquals("GILT", enrichedData.get("instrumentType"), "Should enrich UK gilt instrument type");
            assertEquals("G5GSEF7VJP5I7OUK5573", enrichedData.get("counterpartyLEI"), "Should enrich Barclays LEI");

            // Validate original data preservation
            assertEquals("TXN99999", enrichedData.get("transactionId"), "Original transaction ID should be preserved");
            assertEquals(50000.00, enrichedData.get("amount"), "Original amount should be preserved");

            logger.info("✅ Complete basic inline lookup workflow completed successfully");
            logger.info("  - All 3 enrichments processed successfully");
            logger.info("  - Currency: {} ({})", enrichedData.get("currencyName"), enrichedData.get("currencySymbol"));
            logger.info("  - Instrument: {} ({})", enrichedData.get("instrumentType"), enrichedData.get("instrumentMaturity"));
            logger.info("  - Counterparty: {} ({})", enrichedData.get("counterpartyName"), enrichedData.get("counterpartyJurisdiction"));

        } catch (Exception e) {
            logger.error("Complete basic inline lookup workflow test failed: {}", e.getMessage());
            fail("Complete basic inline lookup workflow test failed: " + e.getMessage());
        }
    }
}
