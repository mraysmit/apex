/*
 * Copyright (c) 2024 Mark Andrew Ray-Smith Cityline Ltd
 * All rights reserved.
 */
package dev.mars.apex.demo.scenario;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConditionalStageExecutionTest - Integration Tests for Conditional Processing-Stage Execution
 *
 * PURPOSE:
 * This test class demonstrates and validates APEX's conditional stage execution feature.
 * It tests real-world scenarios where processing stages execute conditionally based on
 * data attributes using SpEL expressions.
 *
 * FEATURE OVERVIEW:
 * Conditional stage execution allows stages to have optional SpEL conditions that control
 * whether the stage executes. This enables dynamic, data-driven workflow logic such as:
 * - Region-specific compliance checks (US vs EMEA regulations)
 * - Value-based processing (high-value trades require additional validation)
 * - Product-type routing (different processing for different instruments)
 *
 * BUSINESS CONTEXT:
 * In OTC options trading, different regions have different regulatory requirements:
 * - US trades must comply with Dodd-Frank and CFTC regulations
 * - EMEA trades must comply with MiFID II and EMIR regulations
 * - High-value trades (>$10M) require additional approval and risk assessment
 * - All trades undergo common base validation regardless of region or value
 *
 * TESTING APPROACH:
 * - Uses RulesEngine with real YAML configuration files
 * - Creates realistic OTC options trade data with varying attributes
 * - Tests conditional execution based on region (US, EMEA, APAC)
 * - Tests conditional execution based on notional amount (high-value threshold)
 * - Validates that only appropriate stages execute for each trade
 * - Verifies skipped stages are properly logged and tracked
 *
 * YAML CONFIGURATION FILES:
 * - ConditionalStageExecutionTest.yaml (scenario registry)
 * - ConditionalStageExecutionTest-scenario.yaml (main scenario with conditional stages)
 * - ConditionalStageExecutionTest-base-validation.yaml (always executes - no condition)
 * - ConditionalStageExecutionTest-us-compliance.yaml (condition: region == 'US')
 * - ConditionalStageExecutionTest-emea-compliance.yaml (condition: region == 'EMEA')
 * - ConditionalStageExecutionTest-high-value.yaml (condition: notionalAmount > 10000000)
 *
 * TEST SCENARIOS:
 * 1. US Standard Trade - Base + US compliance stages execute
 * 2. EMEA Standard Trade - Base + EMEA compliance stages execute
 * 3. US High-Value Trade - Base + US compliance + high-value stages execute
 * 4. EMEA High-Value Trade - Base + EMEA compliance + high-value stages execute
 * 5. APAC Trade - Only base validation executes (no region-specific compliance)
 *
 * EXPECTED OUTCOMES:
 * - Conditions are evaluated before stage execution
 * - Stages with false conditions are skipped (not executed)
 * - Skipped stages are tracked in execution results
 * - Dependencies are only checked if condition is true
 * - Execution results show which stages executed and which were skipped
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @version 1.0.0
 * @since 2024-11-13
 */
public class ConditionalStageExecutionTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalStageExecutionTest.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up conditional stage execution test");

        try {
            // Load scenario registry with conditional stages
            String registryPath = "src/test/java/dev/mars/apex/demo/scenario/ConditionalStageExecutionTest.yaml";
            rulesEngine = RulesEngine.fromScenarioRegistry(registryPath);

            logger.info("[OK] Loaded scenario registry with conditional processing stages");
        } catch (Exception e) {
            logger.error("Failed to load scenario registry", e);
            throw new RuntimeException("Failed to load scenario registry", e);
        }
    }

    @Test
    @DisplayName("Should execute only US compliance stage for US region trade")
    void testUSRegionConditionalExecution() throws Exception {
        logger.info("=== Testing US Region Conditional Stage Execution ===");
        
        // Create US trade data
        Map<String, Object> tradeData = createTradeData(
            "US",                    // region
            "OTC_OPTION",           // productType
            5000000.0,              // notionalAmount ($5M - below high-value threshold)
            "Goldman Sachs",        // counterparty
            LocalDate.now()         // tradeDate
        );
        
        // Execute scenario
        ScenarioExecutionResult result = rulesEngine.evaluateScenario("conditional-stage-execution-test", tradeData);
        
        // Assertions
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "Scenario should execute successfully");
        
        // Verify base-validation executed
        assertTrue(result.isStageSuccessful("base-validation"), 
            "Base validation stage should execute");
        
        // Verify us-compliance executed (condition: region == 'US')
        assertTrue(result.isStageSuccessful("us-compliance"), 
            "US compliance stage should execute for US region");
        
        // Verify emea-compliance was skipped (condition: region == 'EMEA')
        assertFalse(result.isStageSuccessful("emea-compliance"),
            "EMEA compliance stage should be skipped for US region");
        assertEquals(2, result.getSkippedStages().size(),
            "Should have 2 skipped stages (EMEA + high-value)");
        
        // Verify high-value-validation was skipped (condition: notionalAmount > 10000000)
        assertFalse(result.isStageSuccessful("high-value-validation"), 
            "High-value stage should be skipped for $5M trade");
        
        logger.info("[OK] US region conditional execution validated successfully");
        logger.info("  - Executed stages: base-validation, us-compliance");
        logger.info("  - Skipped stages: emea-compliance, high-value-validation");
    }

    @Test
    @DisplayName("Should execute only EMEA compliance stage for EMEA region trade")
    void testEMEARegionConditionalExecution() throws Exception {
        logger.info("=== Testing EMEA Region Conditional Stage Execution ===");
        
        // Create EMEA trade data
        Map<String, Object> tradeData = createTradeData(
            "EMEA",                 // region
            "OTC_OPTION",          // productType
            3000000.0,             // notionalAmount ($3M - below high-value threshold)
            "Deutsche Bank",       // counterparty
            LocalDate.now()        // tradeDate
        );

        // Execute scenario
        ScenarioExecutionResult result = rulesEngine.evaluateScenario("conditional-stage-execution-test", tradeData);

        // Assertions
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "Scenario should execute successfully");

        // Verify base-validation executed
        assertTrue(result.isStageSuccessful("base-validation"),
            "Base validation stage should execute");

        // Verify emea-compliance executed (condition: region == 'EMEA')
        assertTrue(result.isStageSuccessful("emea-compliance"),
            "EMEA compliance stage should execute for EMEA region");

        // Verify us-compliance was skipped (condition: region == 'US')
        assertFalse(result.isStageSuccessful("us-compliance"),
            "US compliance stage should be skipped for EMEA region");

        // Verify high-value-validation was skipped (condition: notionalAmount > 10000000)
        assertFalse(result.isStageSuccessful("high-value-validation"),
            "High-value stage should be skipped for $3M trade");

        logger.info("[OK] EMEA region conditional execution validated successfully");
        logger.info("  - Executed stages: base-validation, emea-compliance");
        logger.info("  - Skipped stages: us-compliance, high-value-validation");
    }

    @Test
    @DisplayName("Should execute high-value stage for US high-value trade")
    void testUSHighValueConditionalExecution() throws Exception {
        logger.info("=== Testing US High-Value Conditional Stage Execution ===");

        // Create US high-value trade data
        Map<String, Object> tradeData = createTradeData(
            "US",                    // region
            "OTC_OPTION",           // productType
            15000000.0,             // notionalAmount ($15M - above high-value threshold)
            "JP Morgan",            // counterparty
            LocalDate.now()         // tradeDate
        );

        // Add high-value specific fields
        tradeData.put("approvedBy", "John Smith");
        tradeData.put("creditLimitChecked", true);
        tradeData.put("riskAssessment", "APPROVED");

        // Execute scenario
        ScenarioExecutionResult result = rulesEngine.evaluateScenario("conditional-stage-execution-test", tradeData);

        // Assertions
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "Scenario should execute successfully");

        // Verify base-validation executed
        assertTrue(result.isStageSuccessful("base-validation"),
            "Base validation stage should execute");

        // Verify us-compliance executed (condition: region == 'US')
        assertTrue(result.isStageSuccessful("us-compliance"),
            "US compliance stage should execute for US region");

        // Verify high-value-validation executed (condition: notionalAmount > 10000000)
        assertTrue(result.isStageSuccessful("high-value-validation"),
            "High-value stage should execute for $15M trade");

        // Verify emea-compliance was skipped (condition: region == 'EMEA')
        assertFalse(result.isStageSuccessful("emea-compliance"),
            "EMEA compliance stage should be skipped for US region");

        logger.info("[OK] US high-value conditional execution validated successfully");
        logger.info("  - Executed stages: base-validation, us-compliance, high-value-validation");
        logger.info("  - Skipped stages: emea-compliance");
    }

    @Test
    @DisplayName("Should execute high-value stage for EMEA high-value trade")
    void testEMEAHighValueConditionalExecution() throws Exception {
        logger.info("=== Testing EMEA High-Value Conditional Stage Execution ===");

        // Create EMEA high-value trade data
        Map<String, Object> tradeData = createTradeData(
            "EMEA",                 // region
            "OTC_OPTION",          // productType
            20000000.0,            // notionalAmount ($20M - above high-value threshold)
            "Barclays",            // counterparty
            LocalDate.now()        // tradeDate
        );

        // Add high-value specific fields
        tradeData.put("approvedBy", "Jane Doe");
        tradeData.put("creditLimitChecked", true);
        tradeData.put("riskAssessment", "APPROVED");

        // Execute scenario
        ScenarioExecutionResult result = rulesEngine.evaluateScenario("conditional-stage-execution-test", tradeData);

        // Assertions
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "Scenario should execute successfully");

        // Verify base-validation executed
        assertTrue(result.isStageSuccessful("base-validation"),
            "Base validation stage should execute");

        // Verify emea-compliance executed (condition: region == 'EMEA')
        assertTrue(result.isStageSuccessful("emea-compliance"),
            "EMEA compliance stage should execute for EMEA region");

        // Verify high-value-validation executed (condition: notionalAmount > 10000000)
        assertTrue(result.isStageSuccessful("high-value-validation"),
            "High-value stage should execute for $20M trade");

        // Verify us-compliance was skipped (condition: region == 'US')
        assertFalse(result.isStageSuccessful("us-compliance"),
            "US compliance stage should be skipped for EMEA region");

        logger.info("[OK] EMEA high-value conditional execution validated successfully");
        logger.info("  - Executed stages: base-validation, emea-compliance, high-value-validation");
        logger.info("  - Skipped stages: us-compliance");
    }

    @Test
    @DisplayName("Should execute only base validation for APAC region trade")
    void testAPACRegionConditionalExecution() throws Exception {
        logger.info("=== Testing APAC Region Conditional Stage Execution ===");

        // Create APAC trade data (no region-specific compliance configured)
        Map<String, Object> tradeData = createTradeData(
            "APAC",                 // region
            "OTC_OPTION",          // productType
            2000000.0,             // notionalAmount ($2M - below high-value threshold)
            "HSBC",                // counterparty
            LocalDate.now()        // tradeDate
        );

        // Execute scenario
        ScenarioExecutionResult result = rulesEngine.evaluateScenario("conditional-stage-execution-test", tradeData);

        // Assertions
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "Scenario should execute successfully");

        // Verify base-validation executed
        assertTrue(result.isStageSuccessful("base-validation"),
            "Base validation stage should execute");

        // Verify all conditional stages were skipped
        assertFalse(result.isStageSuccessful("us-compliance"),
            "US compliance stage should be skipped for APAC region");
        assertFalse(result.isStageSuccessful("emea-compliance"),
            "EMEA compliance stage should be skipped for APAC region");
        assertFalse(result.isStageSuccessful("high-value-validation"),
            "High-value stage should be skipped for $2M trade");

        assertEquals(3, result.getSkippedStages().size(),
            "Should have 3 skipped stages (US, EMEA, high-value)");

        logger.info("[OK] APAC region conditional execution validated successfully");
        logger.info("  - Executed stages: base-validation");
        logger.info("  - Skipped stages: us-compliance, emea-compliance, high-value-validation");
    }

    /**
     * Helper method to create trade data for testing
     */
    private Map<String, Object> createTradeData(String region, String productType,
                                                 Double notionalAmount, String counterparty,
                                                 LocalDate tradeDate) {
        Map<String, Object> data = new HashMap<>();
        data.put("region", region);
        data.put("productType", productType);
        data.put("notionalAmount", notionalAmount);
        data.put("counterparty", counterparty);
        data.put("tradeDate", tradeDate);
        return data;
    }
}

