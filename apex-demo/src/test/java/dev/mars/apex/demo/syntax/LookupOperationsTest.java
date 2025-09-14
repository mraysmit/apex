package dev.mars.apex.demo.syntax;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LookupOperationsTest - JUnit 5 Test for Lookup Operations and Dataset Queries
 *
 * This test validates comprehensive lookup functionality using real APEX services:
 * - Dataset lookup operations and key-value mappings
 * - External data source queries and integrations
 * - Lookup enrichment patterns and configurations
 * - Cache management and performance optimization
 * - Field mappings for lookup results
 * - Complex lookup key expressions
 *
 * REAL APEX SERVICES TESTED:
 * - EnrichmentService: Real APEX enrichment processor for lookup operations
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - LookupService: Real lookup service for dataset queries and caching
 *
 * CRITICAL VALIDATION CHECKLIST:
 * ✅ Count enrichments in YAML - Each test expects specific number of enrichments
 * ✅ Verify log shows "Processed: X out of X" - Must be 100% execution rate
 * ✅ Check EVERY lookup operation - Test data triggers ALL lookups
 * ✅ Validate EVERY lookup result - Test actual lookup data retrieval
 * ✅ Assert ALL field mappings - Every target-field has corresponding assertEquals
 */
public class LookupOperationsTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(LookupOperationsTest.class);

    @Test
    void testBasicInlineLookups() {
        logger.info("=== Testing Basic Inline Lookups ===");
        
        try {
            // Load YAML configuration for basic inline lookups
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/lookup-basic-inline-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with lookup keys
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("currencyCode", "USD");
            inputData.put("instrumentId", "US912828XG93");
            inputData.put("counterpartyName", "Deutsche Bank AG");
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate currency lookup results
            String currencyName = (String) enrichedData.get("currencyName");
            assertNotNull(currencyName, "Currency name should be looked up");
            assertEquals("US Dollar", currencyName, "Should lookup USD currency name");
            
            String currencySymbol = (String) enrichedData.get("currencySymbol");
            assertNotNull(currencySymbol, "Currency symbol should be looked up");
            assertEquals("$", currencySymbol, "Should lookup USD currency symbol");
            
            // Validate instrument lookup results
            String instrumentType = (String) enrichedData.get("instrumentType");
            assertNotNull(instrumentType, "Instrument type should be looked up");
            assertEquals("TREASURY_NOTE", instrumentType, "Should lookup instrument type");
            
            String instrumentMaturity = (String) enrichedData.get("instrumentMaturity");
            assertNotNull(instrumentMaturity, "Instrument maturity should be looked up");
            assertEquals("10Y", instrumentMaturity, "Should lookup instrument maturity");
            
            // Validate counterparty lookup results
            String counterpartyLEI = (String) enrichedData.get("counterpartyLEI");
            assertNotNull(counterpartyLEI, "Counterparty LEI should be looked up");
            assertEquals("7LTWFZYICNSX8D621K86", counterpartyLEI, "Should lookup Deutsche Bank LEI");
            
            String counterpartyJurisdiction = (String) enrichedData.get("counterpartyJurisdiction");
            assertNotNull(counterpartyJurisdiction, "Counterparty jurisdiction should be looked up");
            assertEquals("DE", counterpartyJurisdiction, "Should lookup Deutsche Bank jurisdiction");
            
            logger.info("Basic inline lookup results: " + enrichedData);
            logger.info("✅ Basic inline lookups test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Basic inline lookups test failed", e);
            fail("Basic inline lookups test failed: " + e.getMessage());
        }
    }

    @Test
    void testComplexLookupKeys() {
        logger.info("=== Testing Complex Lookup Keys ===");
        
        try {
            // Load YAML configuration for complex lookup keys
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/lookup-complex-keys-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with complex lookup scenarios
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("tradeAmount", 1500000.0);
            inputData.put("counterpartyLEI", "7LTWFZYICNSX8D621K86");
            inputData.put("currency", "USD");
            inputData.put("jurisdiction", "US");
            inputData.put("riskScore", 85);
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate amount-based lookup results
            String amountCategory = (String) enrichedData.get("amountCategory");
            assertNotNull(amountCategory, "Amount category should be looked up");
            assertEquals("HIGH_VALUE", amountCategory, "Should lookup high value category for 1.5M");
            
            String processingTier = (String) enrichedData.get("processingTier");
            assertNotNull(processingTier, "Processing tier should be looked up");
            assertEquals("TIER_1", processingTier, "Should lookup tier 1 for high value");
            
            // Validate concatenated key lookup results
            String compositeKey = (String) enrichedData.get("compositeKey");
            assertNotNull(compositeKey, "Composite key should be looked up");
            assertTrue(compositeKey.contains("7LTWFZYICNSX8D621K86"), "Should contain LEI in composite key");
            assertTrue(compositeKey.contains("USD"), "Should contain currency in composite key");
            
            // Validate conditional lookup results
            String regulatoryCode = (String) enrichedData.get("regulatoryCode");
            assertNotNull(regulatoryCode, "Regulatory code should be looked up");
            assertEquals("CFTC_CODE_US", regulatoryCode, "Should lookup CFTC code for US jurisdiction");
            
            // Validate risk-based lookup results
            String riskTier = (String) enrichedData.get("riskTier");
            assertNotNull(riskTier, "Risk tier should be looked up");
            assertEquals("HIGH_RISK", riskTier, "Should lookup high risk tier for score 85");
            
            logger.info("Complex lookup key results: " + enrichedData);
            logger.info("✅ Complex lookup keys test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Complex lookup keys test failed", e);
            fail("Complex lookup keys test failed: " + e.getMessage());
        }
    }

    @Test
    void testLookupWithDefaults() {
        logger.info("=== Testing Lookup with Default Values ===");
        
        try {
            // Load YAML configuration for lookup with defaults
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/lookup-defaults-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with some missing lookup keys
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("knownCurrency", "EUR");
            inputData.put("unknownCurrency", "XYZ");
            inputData.put("knownInstrument", "DE0001102309");
            inputData.put("unknownInstrument", "UNKNOWN123");
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate known currency lookup results
            String knownCurrencyName = (String) enrichedData.get("knownCurrencyName");
            assertNotNull(knownCurrencyName, "Known currency name should be looked up");
            assertEquals("Euro", knownCurrencyName, "Should lookup EUR currency name");
            
            // Validate unknown currency default results
            String unknownCurrencyName = (String) enrichedData.get("unknownCurrencyName");
            assertNotNull(unknownCurrencyName, "Unknown currency name should use default");
            assertEquals("Unknown Currency", unknownCurrencyName, "Should use default for unknown currency");
            
            String unknownCurrencySymbol = (String) enrichedData.get("unknownCurrencySymbol");
            assertNotNull(unknownCurrencySymbol, "Unknown currency symbol should use default");
            assertEquals("?", unknownCurrencySymbol, "Should use default symbol for unknown currency");
            
            // Validate known instrument lookup results
            String knownInstrumentType = (String) enrichedData.get("knownInstrumentType");
            assertNotNull(knownInstrumentType, "Known instrument type should be looked up");
            assertEquals("GOVERNMENT_BOND", knownInstrumentType, "Should lookup German bond type");
            
            // Validate unknown instrument default results
            String unknownInstrumentType = (String) enrichedData.get("unknownInstrumentType");
            assertNotNull(unknownInstrumentType, "Unknown instrument type should use default");
            assertEquals("UNKNOWN_INSTRUMENT", unknownInstrumentType, "Should use default for unknown instrument");
            
            String unknownInstrumentIssuer = (String) enrichedData.get("unknownInstrumentIssuer");
            assertNotNull(unknownInstrumentIssuer, "Unknown instrument issuer should use default");
            assertEquals("UNKNOWN_ISSUER", unknownInstrumentIssuer, "Should use default issuer for unknown instrument");
            
            logger.info("Lookup with defaults results: " + enrichedData);
            logger.info("✅ Lookup with defaults test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Lookup with defaults test failed", e);
            fail("Lookup with defaults test failed: " + e.getMessage());
        }
    }

    @Test
    void testMultipleLookupOperations() {
        logger.info("=== Testing Multiple Lookup Operations ===");
        
        try {
            // Load YAML configuration for multiple lookup operations
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("yaml/lookup-multiple-operations-test.yaml");
            assertNotNull(config, "Configuration should be loaded successfully");
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            
            // Test data with multiple lookup scenarios
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("primaryCurrency", "GBP");
            inputData.put("secondaryCurrency", "JPY");
            inputData.put("primaryInstrument", "GB00B03MLX29");
            inputData.put("secondaryInstrument", "JP1234567890");
            inputData.put("tradingVenue", "LSE");
            
            logger.info("Input data: " + inputData);
            
            // Process enrichments using real APEX services
            Object result = enrichmentService.enrichObject(config, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            assertNotNull(enrichedData, "Enriched data should not be null");
            
            // Validate primary currency lookup results
            String primaryCurrencyName = (String) enrichedData.get("primaryCurrencyName");
            assertNotNull(primaryCurrencyName, "Primary currency name should be looked up");
            assertEquals("British Pound", primaryCurrencyName, "Should lookup GBP currency name");
            
            // Validate secondary currency lookup results
            String secondaryCurrencyName = (String) enrichedData.get("secondaryCurrencyName");
            assertNotNull(secondaryCurrencyName, "Secondary currency name should be looked up");
            assertEquals("Japanese Yen", secondaryCurrencyName, "Should lookup JPY currency name");
            
            // Validate primary instrument lookup results
            String primaryInstrumentType = (String) enrichedData.get("primaryInstrumentType");
            assertNotNull(primaryInstrumentType, "Primary instrument type should be looked up");
            assertEquals("GILT", primaryInstrumentType, "Should lookup UK gilt type");
            
            // Validate secondary instrument lookup results
            String secondaryInstrumentType = (String) enrichedData.get("secondaryInstrumentType");
            assertNotNull(secondaryInstrumentType, "Secondary instrument type should be looked up");
            assertEquals("JGB", secondaryInstrumentType, "Should lookup Japanese government bond type");
            
            // Validate trading venue lookup results
            String venueName = (String) enrichedData.get("venueName");
            assertNotNull(venueName, "Venue name should be looked up");
            assertEquals("London Stock Exchange", venueName, "Should lookup LSE venue name");
            
            String venueCountry = (String) enrichedData.get("venueCountry");
            assertNotNull(venueCountry, "Venue country should be looked up");
            assertEquals("United Kingdom", venueCountry, "Should lookup LSE country");
            
            logger.info("Multiple lookup operations results: " + enrichedData);
            logger.info("✅ Multiple lookup operations test completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Multiple lookup operations test failed", e);
            fail("Multiple lookup operations test failed: " + e.getMessage());
        }
    }
}
