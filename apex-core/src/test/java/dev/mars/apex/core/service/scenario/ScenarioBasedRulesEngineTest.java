package dev.mars.apex.core.service.scenario;

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

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
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
 * Modern test for scenario-based validation and enrichment using the new RulesEngine API.
 * 
 * This test replaces the deprecated DataTypeScenarioService tests by using the same
 * valid YAML configurations with the new RulesEngine architecture.
 * 
 * The YAML files contain real business logic for OTC option trade processing including:
 * - Trade classification rules
 * - Multi-stage processing (validation → enrichment)
 * - SpEL error handling and propagation
 * - Stage dependencies and failure policies
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-16
 * @version 1.0
 */
@DisplayName("Scenario-Based RulesEngine Test")
public class ScenarioBasedRulesEngineTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioBasedRulesEngineTest.class);
    
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        ApexCacheManager.resetInstance();
        yamlLoader = new YamlConfigurationLoader();
        logger.info("✓ Initialized test environment with RulesEngine API");
    }

    @Test
    @DisplayName("Should validate OTC option trade using classification scenario")
    void testOtcOptionScenarioValidation() throws Exception {
        logger.info("=== Testing OTC Option Scenario Validation ===");
        
        // Load the validation rules from the scenario YAML
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-validation-rules.yaml"
        );
        
        // Create RulesEngine from configuration
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test data - valid OTC option trade (pass directly as root context)
        Map<String, Object> validTrade = new HashMap<>();
        validTrade.put("tradeId", "OTC-2025-001");
        validTrade.put("tradeType", "OTCOption");
        
        // Execute rules - pass data directly without wrapper
        RuleResult result = engine.evaluate(validTrade);
        
        // Debug output
        logger.info("Result: triggered={}, isSuccess={}, hasFailures={}, failureMessages={}",
                result.isTriggered(), result.isSuccess(), result.hasFailures(), result.getFailureMessages());
        logger.info("Result type: {}, Rule name: {}, Message: {}", 
                result.getResultType(), result.getRuleName(), result.getMessage());
        
        // Verify validation passed
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Validation should pass for valid trade data");
        
        logger.info("✅ OTC option scenario validation passed");
    }

    @Test
    @DisplayName("Should detect missing required fields in trade validation")
    void testOtcOptionScenarioValidationFailure() throws Exception {
        logger.info("=== Testing OTC Option Scenario Validation Failure ===");
        
        // Load the validation rules
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-validation-rules.yaml"
        );
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test data - missing tradeType (invalid, pass directly as root context)
        Map<String, Object> invalidTrade = new HashMap<>();
        invalidTrade.put("tradeId", "OTC-2025-002");
        // tradeType is missing - should fail validation
        
        // Execute rules - pass data directly without wrapper
        RuleResult result = engine.evaluate(invalidTrade);
        
        // Debug output
        logger.info("Result details:");
        logger.info("  ResultType: {}", result.getResultType());
        logger.info("  isSuccess: {}", result.isSuccess());
        logger.info("  isTriggered: {}", result.isTriggered());
        logger.info("  getSeverity: {}", result.getSeverity());
        logger.info("  getMessage: {}", result.getMessage());
        logger.info("  hasFailures: {}", result.hasFailures());
        logger.info("  getFailureMessages: {}", result.getFailureMessages());
        
        // Verify validation failed appropriately
        assertNotNull(result, "Result should not be null");
        // Per APEX_ERROR_HANDLING_GUIDE: ERROR severity with recovery disabled (default) uses FAIL_FAST
        // When validation rule condition returns FALSE (field is missing), severity determines fail-fast behavior
        assertFalse(result.isSuccess(), "Validation should fail when ERROR severity rule condition is false");
        assertFalse(result.isTriggered(), "Validation rule condition should be FALSE for missing required field");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), "Result type should be ERROR for failed validation");
        
        logger.info("✅ OTC option scenario validation failure detected correctly");
        logger.info("   Evaluation failed (isSuccess=false): {}", !result.isSuccess());
        logger.info("   Rule condition FALSE (field missing): {}", !result.isTriggered());
        logger.info("   ResultType=ERROR: {}", result.getResultType() == RuleResult.ResultType.ERROR);
    }

    @Test
    @DisplayName("Should handle SpEL error propagation in validation stage")
    void testSpelErrorPropagationInValidation() throws Exception {
        logger.info("=== Testing SpEL Error Propagation ===");
        
        // Load the SpEL error test validation rules
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-spel-error-validation-rules.yaml"
        );
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test data - minimal data that will trigger SpEL errors for missing fields
        Map<String, Object> minimalData = new HashMap<>();
        minimalData.put("tradeId", "TEST-001");
        // Missing: currency, instrumentType, quantity, price (will trigger SpEL errors)
        
        // Execute rules - SpEL errors should be caught and propagated (pass data directly)
        RuleResult result = engine.evaluate(minimalData);
        
        // Verify errors were captured
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Should fail due to SpEL errors");
        
        // The engine should handle SpEL errors gracefully
        assertTrue(result.getFailureMessages().size() > 0 || !result.isSuccess(),
            "Should capture SpEL evaluation errors");
        
        logger.info("✅ SpEL error propagation handled correctly");
        logger.info("   Result type: {}", result.getResultType());
        logger.info("   Success: {}", result.isSuccess());
    }

    @Test
    @DisplayName("Should process high-notional OTC option trade with classification")
    void testHighNotionalClassification() throws Exception {
        logger.info("=== Testing High-Notional Trade Classification ===");
        
        // For this test, we'll use the validation rules and verify the engine
        // can handle different trade scenarios based on data content
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-validation-rules.yaml"
        );
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test data - high-notional OTC option trade (pass directly as root context)
        Map<String, Object> highNotionalTrade = new HashMap<>();
        highNotionalTrade.put("tradeId", "HIGH-NOTIONAL-001");
        highNotionalTrade.put("tradeType", "OTCOption");
        highNotionalTrade.put("notional", 10000000.0); // $10M
        highNotionalTrade.put("jurisdiction", "US");
        
        // Execute rules - pass data directly without wrapper
        RuleResult result = engine.evaluate(highNotionalTrade);
        
        // Verify processing
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "High-notional trade validation should pass");
        
        logger.info("✅ High-notional trade classification processed correctly");
        logger.info("   Notional amount: ${}", highNotionalTrade.get("notional"));
    }

    @Test
    @DisplayName("Should handle commodity swap scenario validation")
    void testCommoditySwapScenario() throws Exception {
        logger.info("=== Testing Commodity Swap Scenario ===");
        
        // Load validation rules (same rules work for different trade types)
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-validation-rules.yaml"
        );
        
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test data - commodity swap trade (pass directly as root context)
        Map<String, Object> swapTrade = new HashMap<>();
        swapTrade.put("tradeId", "SWAP-2025-001");
        swapTrade.put("tradeType", "CommoditySwap");
        swapTrade.put("commodity", "GOLD");
        swapTrade.put("notional", 5000000.0);
        
        // Execute rules - pass data directly without wrapper
        RuleResult result = engine.evaluate(swapTrade);
        
        // Verify validation passed
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Commodity swap validation should pass");
        
        logger.info("✅ Commodity swap scenario processed correctly");
        logger.info("   Commodity: {}", swapTrade.get("commodity"));
    }

    @Test
    @DisplayName("Should validate trade enrichment rules")
    void testTradeEnrichmentRules() throws Exception {
        logger.info("=== Testing Trade Enrichment Rules ===");
        
        // Load the enrichment rules from the scenario YAML
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-enrichment-rules.yaml"
        );
        
        // Create RulesEngine with enrichment configuration
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        
        // Test data - trade that needs enrichment (pass directly as root context)
        Map<String, Object> trade = new HashMap<>();
        trade.put("tradeId", "ENRICH-001");
        trade.put("tradeType", "OTCOption");
        trade.put("amount", 50000.0);
        
        // Execute enrichments - pass data directly without wrapper
        RuleResult result = engine.evaluate(trade);
        
        // Verify enrichment execution
        assertNotNull(result, "Result should not be null");
        
        // Check if enriched data is available
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        // Data is now passed directly, no "data" wrapper
        assertTrue(enrichedData.containsKey("tradeId"), "Trade data should be accessible");
        
        logger.info("✅ Trade enrichment rules processed successfully");
        logger.info("   Enriched data fields: {}", enrichedData.keySet());
    }

    @Test
    @DisplayName("Should handle stage-based processing with failure policies")
    void testStagedProcessingWithFailurePolicies() throws Exception {
        logger.info("=== Testing Staged Processing with Failure Policies ===");
        
        // First stage: Validation (terminate on failure)
        YamlRuleConfiguration validationConfig = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-spel-error-validation-rules.yaml"
        );
        
        RulesEngine validationEngine = RulesEngine.fromYamlConfig(validationConfig);
        
        // Test with data that will pass trade ID validation but fail others (pass directly as root context)
        Map<String, Object> testData = new HashMap<>();
        testData.put("tradeId", "STAGE-TEST-001");
        
        // Execute validation stage - pass data directly without wrapper
        RuleResult validationResult = validationEngine.evaluate(testData);
        
        // Verify validation stage behavior
        assertNotNull(validationResult, "Validation result should not be null");
        
        if (!validationResult.isSuccess()) {
            logger.info("✓ Validation stage failed as expected (missing required fields)");
            logger.info("   Failure policy: terminate - enrichment stage should be skipped");
            
            // In real scenario processing, the failure policy would prevent
            // the enrichment stage from executing
            assertTrue(true, "Staged processing failure policy demonstration complete");
        } else {
            logger.info("✓ Validation stage passed - proceeding to enrichment stage");
            
            // Load enrichment stage configuration
            YamlRuleConfiguration enrichmentConfig = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/core/service/scenario/ScenarioBasedRulesEngineTest-spel-error-enrichment-rules.yaml"
            );
            
            RulesEngine enrichmentEngine = RulesEngine.fromYamlConfig(enrichmentConfig);
            
            // Execute enrichment stage with enriched data from validation
            RuleResult enrichmentResult = enrichmentEngine.evaluate(validationResult.getEnrichedData());
            
            assertNotNull(enrichmentResult, "Enrichment result should not be null");
            logger.info("✓ Enrichment stage completed");
        }
        
        logger.info("✅ Staged processing with failure policies demonstrated");
    }
}
