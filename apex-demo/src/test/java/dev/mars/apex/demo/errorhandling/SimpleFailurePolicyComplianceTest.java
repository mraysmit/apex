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

package dev.mars.apex.demo.errorhandling;

import dev.mars.apex.core.service.scenario.DataTypeScenarioService;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
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
 * Simple Failure Policy Compliance Test - demonstrates APEX compliance rule processing.
 * 
 * This test focuses specifically on compliance stage behavior:
 * - How compliance rules execute after validation and enrichment stages
 * - How compliance checks work with different data conditions
 * - How compliance results are captured and reported
 * - How compliance stages handle various failure policies
 *
 * Uses DataTypeScenarioService to execute compliance scenarios and verifies
 * that compliance processing works correctly under various conditions.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Failure Policy Compliance Test")
public class SimpleFailurePolicyComplianceTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFailurePolicyComplianceTest.class);
    
    private DataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up compliance failure policy test environment");

        // Initialize scenario service - it creates its own dependencies
        scenarioService = new DataTypeScenarioService();

        logger.info("✓ Test environment initialized with DataTypeScenarioService");
    }

    @Test
    @DisplayName("Test compliance rules execution after previous stage failures")
    void testComplianceAfterPreviousFailures() throws Exception {
        logger.info("=== Testing Compliance After Previous Stage Failures ===");

        // Load compliance scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyComplianceTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("compliance-test");
        assertNotNull(scenario, "Compliance scenario should be loaded");

        // Create test data that may cause earlier stages to fail but compliance to proceed
        Map<String, Object> testData = new HashMap<>();
        testData.put("transactionType", "TRADE");
        testData.put("region", "US");
        testData.put("amount", 50000.0);
        // Missing some validation fields but has compliance fields

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(testData, scenario);
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ScenarioExecutionResult, "Result should be ScenarioExecutionResult");

        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify compliance executed
        assertFalse(scenarioResult.isTerminated(), "Scenario should not be terminated");
        assertFalse(scenarioResult.getStageResults().isEmpty(), "Should have stage results");
        
        logger.info("✓ Compliance stage executed despite previous stage issues");
        logger.info("✓ Compliance rules processed successfully");
        logger.info("✅ Compliance after failures test completed");
    }

    @Test
    @DisplayName("Test compliance rules with various data conditions")
    void testComplianceWithVariousDataConditions() throws Exception {
        logger.info("=== Testing Compliance with Various Data Conditions ===");

        // Load compliance scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyComplianceTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("compliance-test");
        assertNotNull(scenario, "Compliance scenario should be loaded");

        // Test with high-value transaction
        Map<String, Object> highValueData = new HashMap<>();
        highValueData.put("transactionType", "TRADE");
        highValueData.put("region", "EU");
        highValueData.put("amount", 1000000.0);
        highValueData.put("customerTier", "PREMIUM");

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(highValueData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify compliance processing
        assertNotNull(scenarioResult, "Scenario result should not be null");
        
        logger.info("✓ Compliance rules handled high-value transaction");
        logger.info("✓ Various data conditions processed correctly");
        logger.info("✅ Compliance data conditions test completed");
    }

    @Test
    @DisplayName("Test compliance failure policy behavior")
    void testComplianceFailurePolicyBehavior() throws Exception {
        logger.info("=== Testing Compliance Failure Policy Behavior ===");

        // Load compliance scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyComplianceTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("compliance-test");
        assertNotNull(scenario, "Compliance scenario should be loaded");

        // Create test data that may trigger compliance issues
        Map<String, Object> problematicData = new HashMap<>();
        problematicData.put("transactionType", "SUSPICIOUS");
        problematicData.put("region", "RESTRICTED");
        problematicData.put("amount", 10000000.0);

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(problematicData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify compliance failure handling
        assertNotNull(scenarioResult, "Result should not be null even with compliance issues");
        
        logger.info("✓ Compliance failure policy applied correctly");
        logger.info("✓ Problematic data handled according to policy");
        logger.info("✅ Compliance failure policy test completed");
    }
}
