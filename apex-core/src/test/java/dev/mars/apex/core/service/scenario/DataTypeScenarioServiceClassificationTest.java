package dev.mars.apex.core.service.scenario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DataTypeScenarioService classification-based routing (Phase 1).
 * 
 * Tests the complete end-to-end flow:
 * 1. Load scenarios with embedded classification rules from YAML
 * 2. Route Map data to scenarios based on SpEL classification rule evaluation
 * 3. Execute processing stages for matched scenarios
 * 4. Verify results and error handling
 * 
 * PROGRESSIVE COMPLEXITY:
 * - Level 1: Simple single-field classification
 * - Level 2: Multiple field AND conditions
 * - Level 3: Numeric comparisons
 * - Level 4: No match scenarios and error handling
 * - Level 5: Backward compatibility with data-types
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("DataTypeScenarioService Classification-Based Routing Integration Tests")
class DataTypeScenarioServiceClassificationTest {

    private static final Logger logger = LoggerFactory.getLogger(DataTypeScenarioServiceClassificationTest.class);
    
    private DataTypeScenarioService scenarioService;
    
    @BeforeEach
    void setUp() {
        scenarioService = new DataTypeScenarioService();
        logger.info("Initialized DataTypeScenarioService for classification routing tests");
    }
    
    // ========================================
    // Level 1: Simple Single-Field Classification
    // ========================================
    
    @Test
    @DisplayName("Level 1: Should route OTC Option trade to correct scenario via simple classification")
    void testLevel1_SimpleOtcOptionClassification() throws Exception {
        logger.info("=== Level 1: Simple OTC Option Classification ===");

        // 1. Load scenarios with embedded classification rules
        String registryPath = "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml";
        logger.info("✓ STEP 1: Loading scenario registry from: {}", registryPath);
        scenarioService.loadScenarios(registryPath);
        logger.info("  - Scenario registry loaded successfully");

        // 2. Create simple OTC Option trade data
        Map<String, Object> otcOptionData = new HashMap<>();
        otcOptionData.put("tradeType", "OTCOption");
        otcOptionData.put("tradeId", "OTC-001");
        otcOptionData.put("notional", 1000000.0);

        logger.info("✓ STEP 2: Created OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Trade ID: OTC-001");
        
        // 3. Process data - should match otc-option-scenario
        logger.info("✓ STEP 3: Processing data through classification-based routing");
        long startTime = System.currentTimeMillis();
        ScenarioExecutionResult result = scenarioService.processMapData(otcOptionData);
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
        
        logger.info("✓ VERIFICATION: OTC Option correctly routed to otc-option-scenario");
        logger.info("  - Stages executed: {}", stageResults.size());
    }
    
    @Test
    @DisplayName("Level 1: Should route Commodity Swap trade to correct scenario via simple classification")
    void testLevel1_SimpleCommoditySwapClassification() throws Exception {
        logger.info("=== Level 1: Simple Commodity Swap Classification ===");
        
        // 1. Load scenarios
        String registryPath = "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml";
        scenarioService.loadScenarios(registryPath);
        
        // 2. Create Commodity Swap trade data
        Map<String, Object> commoditySwapData = new HashMap<>();
        commoditySwapData.put("tradeType", "CommoditySwap");
        commoditySwapData.put("tradeId", "SWAP-001");
        commoditySwapData.put("commodity", "GOLD");
        
        logger.info("✓ Created Commodity Swap trade data");
        logger.info("  - Trade Type: CommoditySwap");
        
        // 3. Process data - should match commodity-swap-scenario
        ScenarioExecutionResult result = scenarioService.processMapData(commoditySwapData);
        
        // 4. Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("commodity-swap-scenario", result.getScenarioId(),
            "Should match commodity-swap-scenario via classification rule");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("✓ VERIFICATION: Commodity Swap correctly routed to commodity-swap-scenario");
    }
    
    // ========================================
    // Level 2: Multiple Field AND Conditions
    // ========================================
    
    @Test
    @DisplayName("Level 2: Should route US OTC Option to region-specific scenario via AND conditions")
    void testLevel2_UsOtcOptionAndConditions() throws Exception {
        logger.info("=== Level 2: US OTC Option AND Conditions ===");
        
        // 1. Load scenarios
        String registryPath = "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml";
        scenarioService.loadScenarios(registryPath);
        
        // 2. Create US OTC Option trade data
        Map<String, Object> usOtcOptionData = new HashMap<>();
        usOtcOptionData.put("tradeType", "OTCOption");
        usOtcOptionData.put("region", "US");
        usOtcOptionData.put("tradeId", "US-OTC-001");
        usOtcOptionData.put("notional", 5000000.0);
        
        logger.info("✓ Created US OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Region: US");
        
        // 3. Process data - should match otc-option-us-scenario (more specific)
        ScenarioExecutionResult result = scenarioService.processMapData(usOtcOptionData);
        
        // 4. Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-us-scenario", result.getScenarioId(),
            "Should match otc-option-us-scenario via AND condition (tradeType && region)");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("✓ VERIFICATION: US OTC Option correctly routed to region-specific scenario");
    }
    
    @Test
    @DisplayName("Level 2: Should route non-US OTC Option to generic scenario when region doesn't match")
    void testLevel2_NonUsOtcOptionFallback() throws Exception {
        logger.info("=== Level 2: Non-US OTC Option Fallback ===");
        
        // 1. Load scenarios
        String registryPath = "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml";
        scenarioService.loadScenarios(registryPath);
        
        // 2. Create EMEA OTC Option trade data
        Map<String, Object> emeaOtcOptionData = new HashMap<>();
        emeaOtcOptionData.put("tradeType", "OTCOption");
        emeaOtcOptionData.put("region", "EMEA");
        emeaOtcOptionData.put("tradeId", "EMEA-OTC-001");
        
        logger.info("✓ Created EMEA OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Region: EMEA (should NOT match US-specific scenario)");
        
        // 3. Process data - should match generic otc-option-scenario (not US-specific)
        ScenarioExecutionResult result = scenarioService.processMapData(emeaOtcOptionData);
        
        // 4. Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-scenario", result.getScenarioId(),
            "Should match generic otc-option-scenario when region is not US");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("✓ VERIFICATION: EMEA OTC Option correctly routed to generic scenario");
    }
    
    // ========================================
    // Level 3: Numeric Comparisons
    // ========================================
    
    @Test
    @DisplayName("Level 3: Should route high-notional OTC Option to special scenario via numeric comparison")
    void testLevel3_HighNotionalOtcOption() throws Exception {
        logger.info("=== Level 3: High-Notional OTC Option Numeric Comparison ===");
        
        // 1. Load scenarios
        String registryPath = "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml";
        scenarioService.loadScenarios(registryPath);
        
        // 2. Create high-notional OTC Option trade data (> $100M)
        Map<String, Object> highNotionalData = new HashMap<>();
        highNotionalData.put("tradeType", "OTCOption");
        highNotionalData.put("notional", 150000000.0);  // $150M
        highNotionalData.put("tradeId", "HIGH-OTC-001");
        
        logger.info("✓ Created high-notional OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Notional: $150M (> $100M threshold)");
        
        // 3. Process data - should match high-notional-otc-scenario
        ScenarioExecutionResult result = scenarioService.processMapData(highNotionalData);
        
        // 4. Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("high-notional-otc-scenario", result.getScenarioId(),
            "Should match high-notional-otc-scenario via numeric comparison (notional > 100000000)");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("✓ VERIFICATION: High-notional OTC Option correctly routed to special scenario");
    }
    
    @Test
    @DisplayName("Level 3: Should route low-notional OTC Option to generic scenario when threshold not met")
    void testLevel3_LowNotionalOtcOptionFallback() throws Exception {
        logger.info("=== Level 3: Low-Notional OTC Option Fallback ===");
        
        // 1. Load scenarios
        String registryPath = "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml";
        scenarioService.loadScenarios(registryPath);
        
        // 2. Create low-notional OTC Option trade data (< $100M)
        Map<String, Object> lowNotionalData = new HashMap<>();
        lowNotionalData.put("tradeType", "OTCOption");
        lowNotionalData.put("notional", 50000000.0);  // $50M
        lowNotionalData.put("tradeId", "LOW-OTC-001");
        
        logger.info("✓ Created low-notional OTC Option trade data");
        logger.info("  - Trade Type: OTCOption");
        logger.info("  - Notional: $50M (< $100M threshold)");
        
        // 3. Process data - should match generic otc-option-scenario
        ScenarioExecutionResult result = scenarioService.processMapData(lowNotionalData);
        
        // 4. Verify correct scenario was selected
        assertNotNull(result, "Result should not be null");
        assertEquals("otc-option-scenario", result.getScenarioId(),
            "Should match generic otc-option-scenario when notional < 100000000");
        assertFalse(result.isTerminated(), "Scenario should complete successfully");
        
        logger.info("✓ VERIFICATION: Low-notional OTC Option correctly routed to generic scenario");
    }
    
    // ========================================
    // Level 4: No Match Scenarios and Error Handling
    // ========================================
    
    @Test
    @DisplayName("Level 4: Should handle no matching scenario gracefully")
    void testLevel4_NoMatchingScenario() throws Exception {
        logger.info("=== Level 4: No Matching Scenario ===");
        
        // 1. Load scenarios
        String registryPath = "src/test/java/dev/mars/apex/core/service/scenario/DataTypeScenarioServiceClassificationTest-registry.yaml";
        scenarioService.loadScenarios(registryPath);
        
        // 2. Create data that doesn't match any classification rule
        Map<String, Object> unknownTradeData = new HashMap<>();
        unknownTradeData.put("tradeType", "UnknownInstrument");
        unknownTradeData.put("tradeId", "UNKNOWN-001");
        
        logger.info("✓ Created unknown trade data");
        logger.info("  - Trade Type: UnknownInstrument (no matching scenario)");
        
        // 3. Process data - should return result with warning
        ScenarioExecutionResult result = scenarioService.processMapData(unknownTradeData);
        
        // 4. Verify graceful handling
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTerminated(), "Should be terminated when no scenario matches");
        assertFalse(result.getWarnings().isEmpty(), "Should have warning about no matching scenario");
        
        logger.info("✓ VERIFICATION: No matching scenario handled gracefully");
        logger.info("  - Warnings: {}", result.getWarnings());
    }
    
    @Test
    @DisplayName("Level 4: Should handle null data gracefully")
    void testLevel4_NullDataHandling() {
        logger.info("=== Level 4: Null Data Handling ===");
        
        // Process null data - should return result with warning
        ScenarioExecutionResult result = scenarioService.processMapData(null);
        
        // Verify graceful handling
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTerminated(), "Should be terminated for null data");
        assertFalse(result.getWarnings().isEmpty(), "Should have warning about null data");
        
        logger.info("✓ VERIFICATION: Null data handled gracefully");
        logger.info("  - Warnings: {}", result.getWarnings());
    }
}

