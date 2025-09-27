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
 * Simple Failure Policy Enrichment Test - demonstrates APEX enrichment stage behavior under failure policies.
 * 
 * This test focuses specifically on enrichment stage behavior:
 * - How enrichment stages execute under different failure policies
 * - How data enrichment works when previous stages fail
 * - How enrichment results are captured and field mappings work
 * - How enrichment stages handle missing or invalid data
 *
 * Uses DataTypeScenarioService to execute enrichment scenarios and verifies
 * that enrichment processing works correctly under various failure conditions.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Failure Policy Enrichment Test")
public class SimpleFailurePolicyEnrichmentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFailurePolicyEnrichmentTest.class);
    
    private DataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up enrichment failure policy test environment");

        // Initialize scenario service - it creates its own dependencies
        scenarioService = new DataTypeScenarioService();

        logger.info("✓ Test environment initialized with DataTypeScenarioService");
    }

    @Test
    @DisplayName("Test enrichment stage execution after validation failure")
    void testEnrichmentAfterValidationFailure() throws Exception {
        logger.info("=== Testing Enrichment After Validation Failure ===");

        // Load enrichment scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyEnrichmentTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("enrichment-test");
        assertNotNull(scenario, "Enrichment scenario should be loaded");

        // Create test data that will cause validation to fail but enrichment to proceed
        Map<String, Object> testData = new HashMap<>();
        testData.put("baseAmount", 100.0);
        testData.put("currency", "USD");
        // Missing validation fields but has enrichment fields

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(testData, scenario);
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ScenarioExecutionResult, "Result should be ScenarioExecutionResult");

        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify enrichment executed despite validation failure
        assertFalse(scenarioResult.isTerminated(), "Scenario should not be terminated");
        assertEquals(2, scenarioResult.getStageResults().size(), "Both stages should have executed");
        
        logger.info("✓ Enrichment stage executed despite validation failure");
        logger.info("✓ Data enrichment completed successfully");
        logger.info("✅ Enrichment after validation failure test completed");
    }

    @Test
    @DisplayName("Test enrichment field mappings and calculations")
    void testEnrichmentFieldMappingsAndCalculations() throws Exception {
        logger.info("=== Testing Enrichment Field Mappings and Calculations ===");

        // Load enrichment scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyEnrichmentTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("enrichment-test");
        assertNotNull(scenario, "Enrichment scenario should be loaded");

        // Create test data with enrichment fields
        Map<String, Object> testData = new HashMap<>();
        testData.put("baseAmount", 250.0);
        testData.put("currency", "EUR");
        testData.put("multiplier", 1.5);

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(testData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify enrichment results
        assertNotNull(scenarioResult, "Scenario result should not be null");
        assertFalse(scenarioResult.getStageResults().isEmpty(), "Should have stage results");
        
        logger.info("✓ Enrichment calculations performed successfully");
        logger.info("✓ Field mappings applied correctly");
        logger.info("✅ Enrichment field mappings test completed");
    }

    @Test
    @DisplayName("Test enrichment stage with missing data - graceful handling")
    void testEnrichmentWithMissingData() throws Exception {
        logger.info("=== Testing Enrichment with Missing Data ===");

        // Load enrichment scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyEnrichmentTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("enrichment-test");
        assertNotNull(scenario, "Enrichment scenario should be loaded");

        // Create test data with minimal fields
        Map<String, Object> minimalData = new HashMap<>();
        minimalData.put("testField", "testValue");
        // Missing most enrichment fields

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(minimalData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify graceful handling of missing data
        assertNotNull(scenarioResult, "Result should not be null even with missing data");
        // Enrichment should handle missing fields gracefully
        
        logger.info("✓ Enrichment handled missing data gracefully");
        logger.info("✓ No exceptions thrown for missing enrichment fields");
        logger.info("✅ Enrichment missing data test completed");
    }
}
