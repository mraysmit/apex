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
package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.demo.ColoredTestOutputExtension;

import org.springframework.expression.spel.standard.SpelExpressionParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Update Stage FX Transaction - Simplified Version
 *
 * This test validates the core FX transaction processing logic from the original
 * Update-Stage-FX-Transaction.yaml file, but with simplified inline data instead
 * of external database dependencies and rule references.
 *
 * Key Features Tested:
 * - Currency ranking enrichment with inline datasets
 * - NDF (Non-Deliverable Forward) processing logic
 * - SWIFT vs REUTERS system handling
 * - Conditional field mapping and transformations
 * - Risk assessment based on currency rankings
 * - Validation and audit trail generation
 *
 * Following APEX Principles:
 * - Tests actual functionality, not YAML syntax
 * - Uses real APEX RulesEngine operations
 * - Validates business logic outcomes with RuleResult API
 * - Self-contained with no external dependencies
 * - Follows BasicYamlRuleGroupProcessingATest patterns
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Update Stage FX Transaction - Simplified Test")
public class UpdateStageFxTransactionSimplifiedTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStageFxTransactionSimplifiedTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;
    private EnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for FX transaction processing tests...");
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();

        // Create enrichment service following the pattern from RulesEngineEvaluateTest
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        logger.info("✅ APEX services initialized successfully");
    }

    /**
     * Test SWIFT system with valid NDF values (0/1).
     * This test validates the core SWIFT NDF processing logic using the new RulesEngine approach.
     */
    @Test
    @DisplayName("Should process SWIFT system with valid NDF values (0/1)")
    void testSwiftValidNdfProcessing() {
        logger.info("=== Testing SWIFT Valid NDF Processing ===");

        try {
            // Load configuration and create RulesEngine with EnrichmentService
            YamlRuleConfiguration config = loadConfiguration();
            RulesEngine engine = createRulesEngineWithEnrichmentService(config);

            // Test data - SWIFT system with valid NDF value
            Map<String, Object> testData = createSwiftTestData("1", "USD", "EUR");
            logger.info("Testing SWIFT valid NDF with data: {}", testData);

            // Execute enrichments using RulesEngine evaluate method
            RuleResult result = engine.evaluate(config, testData);

            // Validate RuleResult API
            validateSuccessfulProcessing(result);

            // Get enriched data for business logic validation
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Validate currency ranking enrichment
            validateCurrencyRanking(enrichedData, "USD", 1, "Americas", "EUR", 2, "Europe");

            // Validate NDF processing
            validateNdfProcessing(enrichedData, "1", "SWIFT_PROCESSED", false, "VALIDATED");

            // Validate risk assessment
            validateRiskAssessment(enrichedData, "LOW", true);

            // Validate processing summary
            assertTrue(enrichedData.get("PROCESSING_SUMMARY").toString().contains("SWIFT"),
                "Should include SWIFT in processing summary");

            // Log comprehensive result details
            logRuleResultDetails("SWIFT Valid NDF Processing", result);

            logger.info("✅ SWIFT valid NDF test passed");

        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process configuration: {}", e.getMessage());
            fail("Failed to load or process configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process SWIFT system with Y/N flag conversion")
    void testSwiftYNFlagConversion() {
        logger.info("=== Testing SWIFT Y/N Flag Conversion ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionApexTest_main.yaml");
            
            // Test data - SWIFT system with Y flag
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "Y");
            testData.put("BUY_CURRENCY", "GBP");
            testData.put("SELL_CURRENCY", "JPY");
            
            logger.info("Testing SWIFT Y flag conversion with data: {}", testData);
            
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate Y -> 1 conversion
            assertEquals("1", enrichedData.get("PROCESSED_NDF"), 
                "Should convert Y flag to 1");
            assertEquals(false, enrichedData.get("REQUIRES_TRANSLATION"), 
                "Should not require translation for Y flag");
            assertEquals("VALIDATED", enrichedData.get("VALIDATION_STATUS"), 
                "Should validate Y flag conversion");
            
            // Test N flag conversion
            testData.put("IS_NDF", "N");
            result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedDataN = (Map<String, Object>) result;
            
            assertEquals("0", enrichedDataN.get("PROCESSED_NDF"), 
                "Should convert N flag to 0");
            
            logger.info("✅ SWIFT Y/N flag conversion test passed");
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process REUTERS system with TRUE/FALSE values")
    void testReutersSystemProcessing() {
        logger.info("=== Testing REUTERS System Processing ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionApexTest_main.yaml");
            
            // Test data - REUTERS system with TRUE value
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "REUTERS");
            testData.put("IS_NDF", "TRUE");
            testData.put("BUY_CURRENCY", "CHF");
            testData.put("SELL_CURRENCY", "USD");
            
            logger.info("Testing REUTERS system with data: {}", testData);
            
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate REUTERS processing
            assertEquals("1", enrichedData.get("PROCESSED_NDF"), 
                "Should convert TRUE to 1");
            assertEquals("REUTERS_PROCESSED", enrichedData.get("PROCESSING_STATUS"), 
                "Should indicate REUTERS processing");
            assertEquals(false, enrichedData.get("REQUIRES_TRANSLATION"), 
                "Should not require translation for TRUE/FALSE");
            
            // Test FALSE value
            testData.put("IS_NDF", "FALSE");
            result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedDataFalse = (Map<String, Object>) result;
            
            assertEquals("0", enrichedDataFalse.get("PROCESSED_NDF"), 
                "Should convert FALSE to 0");
            
            logger.info("✅ REUTERS system processing test passed");
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle complex NDF codes requiring translation")
    void testComplexNdfRequiringTranslation() {
        logger.info("=== Testing Complex NDF Requiring Translation ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionApexTest_main.yaml");
            
            // Test data - SWIFT system with complex NDF code
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "COMPLEX_CODE_123");
            testData.put("BUY_CURRENCY", "USD");
            testData.put("SELL_CURRENCY", "EUR");
            
            logger.info("Testing complex NDF code with data: {}", testData);
            
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate translation requirement
            assertEquals("REQUIRES_TRANSLATION", enrichedData.get("PROCESSED_NDF"), 
                "Should identify complex code as requiring translation");
            assertEquals(true, enrichedData.get("REQUIRES_TRANSLATION"), 
                "Should flag as requiring translation");
            assertEquals("NEEDS_TRANSLATION", enrichedData.get("VALIDATION_STATUS"), 
                "Should indicate translation needed");
            
            logger.info("✅ Complex NDF translation test passed: {}", enrichedData);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle unsupported system codes")
    void testUnsupportedSystemCodes() {
        logger.info("=== Testing Unsupported System Codes ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionApexTest_main.yaml");
            
            // Test data - Unsupported system
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "BLOOMBERG");
            testData.put("IS_NDF", "1");
            testData.put("BUY_CURRENCY", "USD");
            testData.put("SELL_CURRENCY", "EUR");
            
            logger.info("Testing unsupported system with data: {}", testData);
            
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate unsupported system handling
            assertEquals("UNSUPPORTED_SYSTEM", enrichedData.get("PROCESSED_NDF"), 
                "Should identify unsupported system");
            assertEquals("UNKNOWN_SYSTEM", enrichedData.get("PROCESSING_STATUS"), 
                "Should indicate unknown system");
            assertEquals(true, enrichedData.get("REQUIRES_TRANSLATION"), 
                "Should flag unsupported system as requiring translation");
            assertEquals("SYSTEM_ERROR", enrichedData.get("VALIDATION_STATUS"), 
                "Should indicate system error");
            
            logger.info("✅ Unsupported system test passed: {}", enrichedData);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should assess risk levels based on currency rankings")
    void testRiskAssessment() {
        logger.info("=== Testing Risk Assessment ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionApexTest_main.yaml");
            
            // Test HIGH risk scenario (lower ranked currencies)
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "1");
            testData.put("BUY_CURRENCY", "JPY");  // Rank 4
            testData.put("SELL_CURRENCY", "CHF");  // Rank 5
            
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            assertEquals("HIGH", enrichedData.get("RISK_LEVEL"),
                "Should assess JPY/CHF as high risk");
            assertEquals(true, enrichedData.get("CROSS_REGION"),
                "Should identify cross-region transaction (Asia vs Europe)");
            
            logger.info("✅ Risk assessment test passed: {}", enrichedData);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    // ========================================================================
    // HELPER METHODS - Following BasicYamlRuleGroupProcessingATest Pattern
    // ========================================================================

    /**
     * Load the simplified FX transaction configuration.
     * Centralized configuration loading following APEX patterns.
     */
    private YamlRuleConfiguration loadConfiguration() throws YamlConfigurationException {
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/conditional/UpdateStageFxTransactionApexTest_main.yaml");

        assertNotNull(config, "Configuration should be loaded");
        assertEquals("Update Stage FX Transaction - Simplified", config.getMetadata().getName());

        logger.info("✅ Configuration loaded: {} enrichments",
            config.getEnrichments() != null ? config.getEnrichments().size() : 0);

        return config;
    }

    /**
     * Create RulesEngine with EnrichmentService for processing enrichments.
     * Following the pattern from RulesEngineEvaluateTest.
     */
    private RulesEngine createRulesEngineWithEnrichmentService(YamlRuleConfiguration config) throws YamlConfigurationException {
        // Create basic configuration from YAML using the standard method
        RulesEngine baseEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        RulesEngineConfiguration rulesConfig = baseEngine.getConfiguration();

        // Create RulesEngine with EnrichmentService
        RulesEngine engine = new RulesEngine(rulesConfig, new SpelExpressionParser(),
                                           new ErrorRecoveryService(), new RulePerformanceMonitor(), enrichmentService);

        assertNotNull(engine, "RulesEngine should be created");
        logger.info("✅ RulesEngine created with EnrichmentService");

        return engine;
    }

    /**
     * Create test data for SWIFT system scenarios.
     */
    private Map<String, Object> createSwiftTestData(String ndfValue, String buyCurrency, String sellCurrency) {
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "SWIFT");
        testData.put("IS_NDF", ndfValue);
        testData.put("BUY_CURRENCY", buyCurrency);
        testData.put("SELL_CURRENCY", sellCurrency);
        return testData;
    }

    /**
     * Create test data for REUTERS system scenarios.
     */
    private Map<String, Object> createReutersTestData(String ndfValue, String buyCurrency, String sellCurrency) {
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "REUTERS");
        testData.put("IS_NDF", ndfValue);
        testData.put("BUY_CURRENCY", buyCurrency);
        testData.put("SELL_CURRENCY", sellCurrency);
        return testData;
    }

    /**
     * Validate successful processing using RuleResult API.
     * Enhanced with comprehensive RuleResult API validation.
     */
    private void validateSuccessfulProcessing(RuleResult result) {
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "RuleResult.isSuccess() should return true for successful processing");
        assertFalse(result.hasFailures(), "RuleResult.hasFailures() should return false for successful processing");
        assertTrue(result.getFailureMessages().isEmpty(), "RuleResult.getFailureMessages() should be empty for success");
        assertNotNull(result.getEnrichedData(), "RuleResult.getEnrichedData() should not be null");
    }

    /**
     * Validate currency ranking enrichment results.
     */
    private void validateCurrencyRanking(Map<String, Object> enrichedData,
                                       String buyCurrency, int buyRank, String buyRegion,
                                       String sellCurrency, int sellRank, String sellRegion) {
        assertEquals(buyRank, enrichedData.get("BUY_CURRENCY_RANK"),
            "Should enrich " + buyCurrency + " with rank " + buyRank);
        assertEquals(sellRank, enrichedData.get("SELL_CURRENCY_RANK"),
            "Should enrich " + sellCurrency + " with rank " + sellRank);
        assertEquals(buyRegion, enrichedData.get("BUY_CURRENCY_REGION"),
            "Should enrich " + buyCurrency + " with " + buyRegion + " region");
        assertEquals(sellRegion, enrichedData.get("SELL_CURRENCY_REGION"),
            "Should enrich " + sellCurrency + " with " + sellRegion + " region");
    }

    /**
     * Validate NDF processing results.
     */
    private void validateNdfProcessing(Map<String, Object> enrichedData,
                                     String expectedProcessedNdf, String expectedStatus,
                                     boolean expectedRequiresTranslation, String expectedValidationStatus) {
        assertEquals(expectedProcessedNdf, enrichedData.get("PROCESSED_NDF"),
            "Should process NDF value correctly");
        assertEquals(expectedStatus, enrichedData.get("PROCESSING_STATUS"),
            "Should indicate correct processing status");
        assertEquals(expectedRequiresTranslation, enrichedData.get("REQUIRES_TRANSLATION"),
            "Should set translation requirement correctly");
        assertEquals(expectedValidationStatus, enrichedData.get("VALIDATION_STATUS"),
            "Should set validation status correctly");
    }

    /**
     * Validate risk assessment results.
     */
    private void validateRiskAssessment(Map<String, Object> enrichedData, String expectedRiskLevel, boolean expectedCrossRegion) {
        assertEquals(expectedRiskLevel, enrichedData.get("RISK_LEVEL"),
            "Should assess risk level correctly");
        assertEquals(expectedCrossRegion, enrichedData.get("CROSS_REGION"),
            "Should identify cross-region transaction correctly");
    }

    /**
     * Log comprehensive RuleResult details following BasicYamlRuleGroupProcessingATest pattern.
     */
    private void logRuleResultDetails(String testName, RuleResult result) {
        logger.info("=== {} - RuleResult API Details ===", testName);
        logger.info("result.isSuccess(): {}", result.isSuccess());
        logger.info("result.hasFailures(): {}", result.hasFailures());
        logger.info("result.getFailureMessages(): {}", result.getFailureMessages());
        logger.info("result.getEnrichedData(): {}", result.getEnrichedData());
        logger.info("result.getRuleName(): {}", result.getRuleName());
        logger.info("result.getMessage(): {}", result.getMessage() != null ? result.getMessage() : "No message");
        logger.info("result.getResultType(): {}", result.getResultType());
        logger.info("result.getTimestamp(): {}", result.getTimestamp());
    }
}
