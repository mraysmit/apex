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

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LookupBasicInlineTest - APEX Inline Dataset Lookup Operations Demo
 * 
 * PURPOSE: Showcase APEX's inline dataset lookup capabilities through:
 * - APEX inline dataset configuration with embedded reference data
 * - APEX multi-field lookup enrichments with field mappings
 * - APEX financial instrument lookups (Treasury Notes, Government Bonds, Gilts)
 * - APEX currency lookups with symbols and country information
 * - APEX counterparty lookups with LEI codes and jurisdictions
 * 
 * INLINE DATASET FEATURES DEMONSTRATED:
 * - type: "inline" - Embedded datasets within YAML configuration
 * - key-field: "code" - Lookup key field specification
 * - data: [...] - Inline reference data arrays
 * - field-mappings - Source to target field transformations
 * - lookup-key: "#currencyCode" - SpEL expressions for lookup keys
 * 
 * CRITICAL LOOKUP PROCESSING CHECKLIST APPLIED:
 *  Verify 3 lookup enrichments process successfully
 *  Validate inline dataset lookups work correctly
 *  Check field mappings transform data properly
 *  Assert financial instrument data accuracy
 *  Confirm currency and counterparty enrichments
 * 
 * ALL LOOKUP LOGIC IS IN APEX YAML FILES - NO CUSTOM JAVA LOGIC
 * Tests validate APEX inline dataset capabilities using established patterns
 * 
 * Following established patterns from CalculationMathematicalTest
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Inline Dataset Lookup Tests")
public class LookupBasicInlineTest {
    
    private static final Logger logger = LoggerFactory.getLogger(LookupBasicInlineTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;
    private EnrichmentService enrichmentService;
    private YamlRuleConfiguration config;

    @BeforeEach
    void setUp() {
        // Initialize APEX services following established patterns
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();

        // Create enrichment service with required dependencies
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);
        
        try {
            // Load inline lookup configuration
            config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/LookupBasicInlineTestB.yaml");
            
            logger.info("✅ APEX services initialized for inline lookup testing");
            logger.info("  - Configuration loaded: {}", config.getMetadata().getName());
            logger.info("  - Lookup enrichments: {}", config.getEnrichments().size());
            
        } catch (YamlConfigurationException e) {
            logger.error("X Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Create RulesEngine with EnrichmentService for lookup processing
     * Following the established pattern from previous tests
     */
    private RulesEngine createRulesEngineWithEnrichmentService(YamlRuleConfiguration config) throws YamlConfigurationException {
        // Create basic configuration from YAML using the standard method
        RulesEngine baseEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        RulesEngineConfiguration rulesConfig = baseEngine.getConfiguration();

        // Create RulesEngine with EnrichmentService
        RulesEngine engine = new RulesEngine(rulesConfig, new SpelExpressionParser(),
                                           new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);

        assertNotNull(engine, "RulesEngine should be created");
        logger.info("✅ RulesEngine created with EnrichmentService for lookup operations");

        return engine;
    }

    @Test
    @DisplayName("Test Currency Lookup Operations")
    void testCurrencyLookupOperations() {
        logger.info("=== Testing Currency Lookup Operations ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test USD currency lookup
            logger.info("Testing USD currency lookup...");
            Map<String, Object> usdData = new HashMap<>();
            usdData.put("currencyCode", "USD");

            RuleResult usdResult = engine.evaluate(config, usdData);
            assertNotNull(usdResult, "USD lookup result should not be null");
            
            Map<String, Object> usdEnriched = usdResult.getEnrichedData();
            logger.info("✓ USD Currency lookup successful:");
            logger.info("  - Currency Name: {}", usdEnriched.get("currencyName"));
            logger.info("  - Currency Symbol: {}", usdEnriched.get("currencySymbol"));
            logger.info("  - Currency Country: {}", usdEnriched.get("currencyCountry"));

            assertEquals("US Dollar", usdEnriched.get("currencyName"), "USD name should be US Dollar");
            assertEquals("$", usdEnriched.get("currencySymbol"), "USD symbol should be $");
            assertEquals("United States", usdEnriched.get("currencyCountry"), "USD country should be United States");

            // Test EUR currency lookup
            logger.info("Testing EUR currency lookup...");
            Map<String, Object> eurData = new HashMap<>();
            eurData.put("currencyCode", "EUR");

            RuleResult eurResult = engine.evaluate(config, eurData);
            assertNotNull(eurResult, "EUR lookup result should not be null");
            
            Map<String, Object> eurEnriched = eurResult.getEnrichedData();
            logger.info("✓ EUR Currency lookup successful:");
            logger.info("  - Currency Name: {}", eurEnriched.get("currencyName"));
            logger.info("  - Currency Symbol: {}", eurEnriched.get("currencySymbol"));
            logger.info("  - Currency Country: {}", eurEnriched.get("currencyCountry"));

            assertEquals("Euro", eurEnriched.get("currencyName"), "EUR name should be Euro");
            assertEquals("€", eurEnriched.get("currencySymbol"), "EUR symbol should be €");
            assertEquals("European Union", eurEnriched.get("currencyCountry"), "EUR country should be European Union");

        } catch (Exception e) {
            logger.error("X Currency lookup operations failed: {}", e.getMessage());
            fail("Currency lookup operations failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Financial Instrument Lookup Operations")
    void testFinancialInstrumentLookupOperations() {
        logger.info("=== Testing Financial Instrument Lookup Operations ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test US Treasury Note lookup
            logger.info("Testing US Treasury Note lookup...");
            Map<String, Object> treasuryData = new HashMap<>();
            treasuryData.put("instrumentId", "US912828XG93");

            RuleResult treasuryResult = engine.evaluate(config, treasuryData);
            assertNotNull(treasuryResult, "Treasury lookup result should not be null");
            
            Map<String, Object> treasuryEnriched = treasuryResult.getEnrichedData();
            logger.info("✓ US Treasury Note lookup successful:");
            logger.info("  - Instrument Type: {}", treasuryEnriched.get("instrumentType"));
            logger.info("  - Instrument Maturity: {}", treasuryEnriched.get("instrumentMaturity"));
            logger.info("  - Instrument Issuer: {}", treasuryEnriched.get("instrumentIssuer"));

            assertEquals("TREASURY_NOTE", treasuryEnriched.get("instrumentType"), "Should be Treasury Note");
            assertEquals("10Y", treasuryEnriched.get("instrumentMaturity"), "Should be 10 year maturity");
            assertEquals("US_TREASURY", treasuryEnriched.get("instrumentIssuer"), "Should be US Treasury");

            // Test German Government Bond lookup
            logger.info("Testing German Government Bond lookup...");
            Map<String, Object> bondData = new HashMap<>();
            bondData.put("instrumentId", "DE0001102309");

            RuleResult bondResult = engine.evaluate(config, bondData);
            assertNotNull(bondResult, "Bond lookup result should not be null");
            
            Map<String, Object> bondEnriched = bondResult.getEnrichedData();
            logger.info("✓ German Government Bond lookup successful:");
            logger.info("  - Instrument Type: {}", bondEnriched.get("instrumentType"));
            logger.info("  - Instrument Maturity: {}", bondEnriched.get("instrumentMaturity"));
            logger.info("  - Instrument Issuer: {}", bondEnriched.get("instrumentIssuer"));

            assertEquals("GOVERNMENT_BOND", bondEnriched.get("instrumentType"), "Should be Government Bond");
            assertEquals("30Y", bondEnriched.get("instrumentMaturity"), "Should be 30 year maturity");
            assertEquals("GERMAN_GOVERNMENT", bondEnriched.get("instrumentIssuer"), "Should be German Government");

        } catch (Exception e) {
            logger.error("X Financial instrument lookup operations failed: {}", e.getMessage());
            fail("Financial instrument lookup operations failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Counterparty Lookup Operations")
    void testCounterpartyLookupOperations() {
        logger.info("=== Testing Counterparty Lookup Operations ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test Deutsche Bank lookup
            logger.info("Testing Deutsche Bank counterparty lookup...");
            Map<String, Object> dbData = new HashMap<>();
            dbData.put("counterpartyName", "Deutsche Bank AG");

            RuleResult dbResult = engine.evaluate(config, dbData);
            assertNotNull(dbResult, "Deutsche Bank lookup result should not be null");
            
            Map<String, Object> dbEnriched = dbResult.getEnrichedData();
            logger.info("✓ Deutsche Bank counterparty lookup successful:");
            logger.info("  - Counterparty LEI: {}", dbEnriched.get("counterpartyLEI"));
            logger.info("  - Counterparty Jurisdiction: {}", dbEnriched.get("counterpartyJurisdiction"));
            logger.info("  - Counterparty Entity Type: {}", dbEnriched.get("counterpartyEntityType"));

            assertEquals("7LTWFZYICNSX8D621K86", dbEnriched.get("counterpartyLEI"), "Deutsche Bank LEI should match");
            assertEquals("DE", dbEnriched.get("counterpartyJurisdiction"), "Deutsche Bank jurisdiction should be DE");
            assertEquals("BANK", dbEnriched.get("counterpartyEntityType"), "Deutsche Bank should be BANK entity type");

            // Test JPMorgan Chase lookup
            logger.info("Testing JPMorgan Chase counterparty lookup...");
            Map<String, Object> jpData = new HashMap<>();
            jpData.put("counterpartyName", "JPMorgan Chase");

            RuleResult jpResult = engine.evaluate(config, jpData);
            assertNotNull(jpResult, "JPMorgan lookup result should not be null");
            
            Map<String, Object> jpEnriched = jpResult.getEnrichedData();
            logger.info("✓ JPMorgan Chase counterparty lookup successful:");
            logger.info("  - Counterparty LEI: {}", jpEnriched.get("counterpartyLEI"));
            logger.info("  - Counterparty Jurisdiction: {}", jpEnriched.get("counterpartyJurisdiction"));
            logger.info("  - Counterparty Entity Type: {}", jpEnriched.get("counterpartyEntityType"));

            assertEquals("8EE8DF3643E15DBFDA05", jpEnriched.get("counterpartyLEI"), "JPMorgan LEI should match");
            assertEquals("US", jpEnriched.get("counterpartyJurisdiction"), "JPMorgan jurisdiction should be US");
            assertEquals("BANK", jpEnriched.get("counterpartyEntityType"), "JPMorgan should be BANK entity type");

        } catch (Exception e) {
            logger.error("X Counterparty lookup operations failed: {}", e.getMessage());
            fail("Counterparty lookup operations failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Complete Multi-Lookup Workflow")
    void testCompleteMultiLookupWorkflow() {
        logger.info("=== Testing Complete Multi-Lookup Workflow ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test complete workflow with all lookup types
            logger.info("Testing complete multi-lookup workflow...");
            Map<String, Object> completeData = new HashMap<>();
            completeData.put("currencyCode", "GBP");
            completeData.put("instrumentId", "GB00B03MLX29");
            completeData.put("counterpartyName", "Barclays Bank PLC");

            RuleResult completeResult = engine.evaluate(config, completeData);
            assertNotNull(completeResult, "Complete workflow result should not be null");
            
            Map<String, Object> enrichedData = completeResult.getEnrichedData();
            
            logger.info("✓ Complete multi-lookup workflow processed successfully");
            logger.info("  - Currency: {} ({}) from {}", 
                enrichedData.get("currencyName"), 
                enrichedData.get("currencySymbol"), 
                enrichedData.get("currencyCountry"));
            logger.info("  - Instrument: {} {} issued by {}", 
                enrichedData.get("instrumentType"), 
                enrichedData.get("instrumentMaturity"), 
                enrichedData.get("instrumentIssuer"));
            logger.info("  - Counterparty: {} ({}) in {}", 
                enrichedData.get("counterpartyEntityType"), 
                enrichedData.get("counterpartyLEI"), 
                enrichedData.get("counterpartyJurisdiction"));

            // Validate all lookups worked correctly
            assertEquals("British Pound", enrichedData.get("currencyName"), "GBP name should be British Pound");
            assertEquals("£", enrichedData.get("currencySymbol"), "GBP symbol should be £");
            assertEquals("United Kingdom", enrichedData.get("currencyCountry"), "GBP country should be United Kingdom");
            
            assertEquals("GILT", enrichedData.get("instrumentType"), "Should be UK Gilt");
            assertEquals("5Y", enrichedData.get("instrumentMaturity"), "Should be 5 year maturity");
            assertEquals("UK_TREASURY", enrichedData.get("instrumentIssuer"), "Should be UK Treasury");
            
            assertEquals("G5GSEF7VJP5I7OUK5573", enrichedData.get("counterpartyLEI"), "Barclays LEI should match");
            assertEquals("GB", enrichedData.get("counterpartyJurisdiction"), "Barclays jurisdiction should be GB");
            assertEquals("BANK", enrichedData.get("counterpartyEntityType"), "Barclays should be BANK entity type");

        } catch (Exception e) {
            logger.error("X Complete multi-lookup workflow failed: {}", e.getMessage());
            fail("Complete multi-lookup workflow failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Lookup with Missing Data")
    void testLookupWithMissingData() {
        logger.info("=== Testing Lookup with Missing Data ===");

        try {
            // Create RulesEngine with EnrichmentService
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test with non-existent currency code
            logger.info("Testing lookup with non-existent currency code...");
            Map<String, Object> missingData = new HashMap<>();
            missingData.put("currencyCode", "XYZ"); // Non-existent currency

            RuleResult missingResult = engine.evaluate(config, missingData);
            assertNotNull(missingResult, "Missing data result should not be null");
            
            Map<String, Object> enrichedData = missingResult.getEnrichedData();
            logger.info("✓ Lookup with missing data handled gracefully");
            logger.info("  - Enriched data size: {}", enrichedData.size());

            // Should still have original data but no enriched lookup fields
            assertEquals("XYZ", enrichedData.get("currencyCode"), "Original currency code should be preserved");
            assertNull(enrichedData.get("currencyName"), "Currency name should be null for non-existent code");

        } catch (Exception e) {
            logger.error("X Lookup with missing data failed: {}", e.getMessage());
            fail("Lookup with missing data failed: " + e.getMessage());
        }
    }
}
