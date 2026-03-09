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

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.core.service.scenario.StageExecutionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RulesEngine classification-based routing.
 * 
 * <p>Replaces the deleted DataTypeScenarioServiceClassificationTest, using the new RulesEngine API.</p>
 * 
 * <p>Tests the complete end-to-end flow:</p>
 * <ol>
 *   <li>Load scenarios with embedded classification rules from YAML via RulesEngine.fromScenarioRegistry()</li>
 *   <li>Route Map data to scenarios based on SpEL classification rule evaluation via evaluateWithClassification()</li>
 *   <li>Execute processing stages for matched scenarios</li>
 *   <li>Verify results and error handling</li>
 * </ol>
 * 
 * <p><b>PROGRESSIVE COMPLEXITY:</b></p>
 * <ul>
 *   <li>Level 1: Simple single-field classification</li>
 *   <li>Level 2: Multiple field AND conditions</li>
 *   <li>Level 3: Numeric comparisons</li>
 *   <li>Level 4: No match scenarios and error handling</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see RulesEngine#fromScenarioRegistry(String)
 * @see RulesEngine#evaluateWithClassification(Map)
 */
@DisplayName("RulesEngine Classification-Based Routing Integration Tests")
class RulesEngineClassificationRoutingTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineClassificationRoutingTest.class);
    
    private static final String REGISTRY_PATH = 
        "dev/mars/apex/core/service/scenario/RulesEngineClassificationRoutingTest-registry.yaml";
    
    private RulesEngine engine;

    @BeforeAll
    static void classSetUp() {
        MDC.put("testContext", "[EXPECTED] ");
        LoggerFactory.getLogger(RulesEngineClassificationRoutingTest.class)
            .info("[INTENTIONAL-FAILURE-TEST-CLASS-START] RulesEngineClassificationRoutingTest intentionally triggers ERROR/WARN logs");
        LoggerFactory.getLogger(RulesEngineClassificationRoutingTest.class)
            .info("[INTENTIONAL-FAILURE-TEST-CLASS-START] Expected: no-match IllegalStateException, null NullPointerException");
    }

    @AfterAll
    static void classTearDown() {
        LoggerFactory.getLogger(RulesEngineClassificationRoutingTest.class)
            .info("[INTENTIONAL-FAILURE-TEST-CLASS-END] RulesEngineClassificationRoutingTest intentional error tests completed");
        MDC.remove("testContext");
    }
    
    @BeforeEach
    void setUp() throws Exception {
        engine = RulesEngine.fromScenarioRegistry(REGISTRY_PATH);
        logger.info("Initialized RulesEngine from scenario registry for classification routing tests");
    }
    
    // ========================================
    // Level 1: Simple Single-Field Classification
    // ========================================
    
    @Test
    @DisplayName("Level 1: Should route OTC Option trade to correct scenario via simple classification")
    void testLevel1_SimpleOtcOptionClassification() throws Exception {
        logger.info("=== Level 1: Simple OTC Option Classification ===");

        // 1. Verify engine is loaded with scenarios
        assertNotNull(engine.getScenarioRegistry(), "Scenario registry should be loaded");
        assertFalse(engine.getScenarioRegistry().isEmpty(), "Scenario registry should not be empty");
        logger.info("[OK] STEP 1: Loaded {} scenarios from registry", engine.getScenarioRegistry().size());

        // 2. Create simple OTC Option trade data
        Map<String, Object> otcOptionData = new HashMap<>();
        otcOptionData.put("tradeType", "OTCOption");
        otcOptionData.put("tradeId", "OTC-001");
        otcOptionData.put("notional", 1000000.0);

        logger.info("[OK] STEP 2: Created OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Trade ID: OTC-001");
        
        // 3. Process data using classification-based routing
        logger.info("[OK] STEP 3: Processing data through classification-based routing");
        long startTime = System.currentTimeMillis();
        ScenarioExecutionResult result = engine.evaluateWithClassification(otcOptionData);
        long executionTime = System.currentTimeMillis() - startTime;
        
        logger.info("  - Execution completed in {}ms", executionTime);
        logger.info("  - Matched scenario: {}", result.getScenarioId());
        logger.info("  - Execution status: {}", result.getExecutionStatus());
        
        // 4. Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-scenario", result.getScenarioId(), 
            "Should match otc-option-scenario via classification rule");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        // 5. Verify stages executed
        List<StageExecutionResult> stageResults = result.getStageResults();
        assertFalse(stageResults.isEmpty(), "Should have executed at least one stage");
        
        logger.info("[OK] VERIFICATION: OTC Option correctly routed to otc-option-scenario");
        logger.info("  - Stages executed: {}", stageResults.size());
    }
    
    @Test
    @DisplayName("Level 1: Should route Commodity Swap trade to correct scenario via simple classification")
    void testLevel1_SimpleCommoditySwapClassification() throws Exception {
        logger.info("=== Level 1: Simple Commodity Swap Classification ===");
        
        // Create Commodity Swap trade data
        Map<String, Object> commoditySwapData = new HashMap<>();
        commoditySwapData.put("tradeType", "CommoditySwap");
        commoditySwapData.put("tradeId", "SWAP-001");
        commoditySwapData.put("commodity", "GOLD");
        
        logger.info("[OK] Created Commodity Swap trade data");
        logger.info("  - Trade Type: CommoditySwap");
        
        // Process data using classification-based routing
        ScenarioExecutionResult result = engine.evaluateWithClassification(commoditySwapData);
        
        // Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("commodity-swap-scenario", result.getScenarioId(),
            "Should match commodity-swap-scenario via classification rule");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("[OK] VERIFICATION: Commodity Swap correctly routed to commodity-swap-scenario");
    }
    
    // ========================================
    // Level 2: Multiple Field AND Conditions
    // ========================================
    
    @Test
    @DisplayName("Level 2: Should route US OTC Option to region-specific scenario via AND conditions")
    void testLevel2_UsOtcOptionAndConditions() throws Exception {
        logger.info("=== Level 2: US OTC Option AND Conditions ===");
        
        // Create US OTC Option trade data
        Map<String, Object> usOtcOptionData = new HashMap<>();
        usOtcOptionData.put("tradeType", "OTCOption");
        usOtcOptionData.put("region", "US");
        usOtcOptionData.put("tradeId", "US-OTC-001");
        usOtcOptionData.put("notional", 5000000.0);
        
        logger.info("[OK] Created US OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Region: US");
        
        // Process data using classification-based routing
        ScenarioExecutionResult result = engine.evaluateWithClassification(usOtcOptionData);
        
        // Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-us-scenario", result.getScenarioId(),
            "Should match otc-option-us-scenario via AND condition (tradeType && region)");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("[OK] VERIFICATION: US OTC Option correctly routed to region-specific scenario");
    }
    
    @Test
    @DisplayName("Level 2: Should route non-US OTC Option to generic scenario when region doesn't match")
    void testLevel2_NonUsOtcOptionFallback() throws Exception {
        logger.info("=== Level 2: Non-US OTC Option Fallback ===");
        
        // Create EMEA OTC Option trade data
        Map<String, Object> emeaOtcOptionData = new HashMap<>();
        emeaOtcOptionData.put("tradeType", "OTCOption");
        emeaOtcOptionData.put("region", "EMEA");
        emeaOtcOptionData.put("tradeId", "EMEA-OTC-001");
        
        logger.info("[OK] Created EMEA OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Region: EMEA (should NOT match US-specific scenario)");
        
        // Process data using classification-based routing
        ScenarioExecutionResult result = engine.evaluateWithClassification(emeaOtcOptionData);
        
        // Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-scenario", result.getScenarioId(),
            "Should match generic otc-option-scenario when region is not US");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("[OK] VERIFICATION: EMEA OTC Option correctly routed to generic scenario");
    }
    
    // ========================================
    // Level 3: Numeric Comparisons
    // ========================================
    
    @Test
    @DisplayName("Level 3: Should route high-notional OTC Option to special scenario via numeric comparison")
    void testLevel3_HighNotionalOtcOption() throws Exception {
        logger.info("=== Level 3: High-Notional OTC Option Numeric Comparison ===");
        
        // Create high-notional OTC Option trade data (> $100M)
        Map<String, Object> highNotionalData = new HashMap<>();
        highNotionalData.put("tradeType", "OTCOption");
        highNotionalData.put("notional", 150000000.0);  // $150M
        highNotionalData.put("tradeId", "HIGH-OTC-001");
        
        logger.info("[OK] Created high-notional OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Notional: $150M (> $100M threshold)");
        
        // Process data using classification-based routing
        ScenarioExecutionResult result = engine.evaluateWithClassification(highNotionalData);
        
        // Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("high-notional-otc-scenario", result.getScenarioId(),
            "Should match high-notional-otc-scenario via numeric comparison (notional > 100000000)");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("[OK] VERIFICATION: High-notional OTC Option correctly routed to special scenario");
    }
    
    @Test
    @DisplayName("Level 3: Should route low-notional OTC Option to generic scenario when threshold not met")
    void testLevel3_LowNotionalOtcOptionFallback() throws Exception {
        logger.info("=== Level 3: Low-Notional OTC Option Fallback ===");
        
        // Create low-notional OTC Option trade data (< $100M)
        Map<String, Object> lowNotionalData = new HashMap<>();
        lowNotionalData.put("tradeType", "OTCOption");
        lowNotionalData.put("notional", 50000000.0);  // $50M
        lowNotionalData.put("tradeId", "LOW-OTC-001");
        
        logger.info("[OK] Created low-notional OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Notional: $50M (< $100M threshold)");
        
        // Process data using classification-based routing
        ScenarioExecutionResult result = engine.evaluateWithClassification(lowNotionalData);
        
        // Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-scenario", result.getScenarioId(),
            "Should match generic otc-option-scenario when notional < 100000000");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("[OK] VERIFICATION: Low-notional OTC Option correctly routed to generic scenario");
    }
    
    // ========================================
    // Level 4: No Match Scenarios and Error Handling
    // ========================================
    
    @Test
    @DisplayName("Level 4: Should throw exception when no matching scenario found")
    void testLevel4_NoMatchingScenario() throws Exception {
        logger.info("=== Level 4: No Matching Scenario ===");
        
        // Create data that doesn't match any classification rule
        Map<String, Object> unknownTradeData = new HashMap<>();
        unknownTradeData.put("tradeType", "UnknownInstrument");
        unknownTradeData.put("tradeId", "UNKNOWN-001");
        
        logger.info("[OK] Created unknown trade data");
        logger.info("  - Trade Type: UnknownInstrument (no matching scenario)");
        
        // RulesEngine.evaluateWithClassification() throws IllegalStateException when no scenario matches
        assertThrows(IllegalStateException.class, () -> {
            engine.evaluateWithClassification(unknownTradeData);
        }, "Should throw IllegalStateException when no scenario matches");
        
        logger.info("[OK] VERIFICATION: No matching scenario throws IllegalStateException as expected");
    }
    
    @Test
    @DisplayName("Level 4: Should throw exception for null data")
    void testLevel4_NullDataHandling() {
        logger.info("=== Level 4: Null Data Handling ===");
        
        // evaluateWithClassification() now validates null input with IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            engine.evaluateWithClassification(null);
        }, "Should throw IllegalArgumentException for null data");
        
        assertTrue(exception.getMessage().contains("null"), "Exception message should mention null");
        
        logger.info("[OK] VERIFICATION: Null data throws IllegalArgumentException as expected");
    }
    
    // ========================================
    // Additional RulesEngine-specific Tests
    // ========================================
    
    @Test
    @DisplayName("Should provide access to scenario registry")
    void testScenarioRegistryAccess() {
        logger.info("=== Testing scenario registry access ===");
        
        Map<String, dev.mars.apex.core.service.scenario.ScenarioConfiguration> registry = engine.getScenarioRegistry();
        
        assertNotNull(registry, "Scenario registry should not be null");
        assertFalse(registry.isEmpty(), "Scenario registry should not be empty");
        
        // Verify expected scenarios are in registry
        assertTrue(registry.containsKey("otc-option-scenario"), "Should contain otc-option-scenario");
        assertTrue(registry.containsKey("commodity-swap-scenario"), "Should contain commodity-swap-scenario");
        assertTrue(registry.containsKey("otc-option-us-scenario"), "Should contain otc-option-us-scenario");
        assertTrue(registry.containsKey("high-notional-otc-scenario"), "Should contain high-notional-otc-scenario");
        
        logger.info("[OK] Scenario registry contains {} scenarios", registry.size());
        registry.keySet().forEach(id -> logger.info("  - {}", id));
    }
    
    @Test
    @DisplayName("Should evaluate specific scenario by ID using evaluateScenario()")
    void testEvaluateScenarioById() throws Exception {
        logger.info("=== Testing evaluateScenario() by ID ===");
        
        // Create trade data
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeType", "OTCOption");
        tradeData.put("tradeId", "DIRECT-001");
        
        // Directly evaluate a specific scenario by ID
        ScenarioExecutionResult result = engine.evaluateScenario("otc-option-scenario", tradeData);
        
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-scenario", result.getScenarioId(), "Should execute the specified scenario");
        
        logger.info("[OK] Successfully evaluated scenario by ID: {}", result.getScenarioId());
    }
    
    @Test
    @DisplayName("Should throw exception for non-existent scenario ID")
    void testEvaluateNonExistentScenario() {
        logger.info("=== Testing evaluateScenario() with non-existent ID ===");
        
        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeType", "OTCOption");
        
        // Should throw IllegalArgumentException for non-existent scenario
        assertThrows(IllegalArgumentException.class, () -> {
            engine.evaluateScenario("non-existent-scenario", tradeData);
        }, "Should throw IllegalArgumentException for non-existent scenario ID");
        
        logger.info("[OK] Non-existent scenario ID throws IllegalArgumentException as expected");
    }
}
