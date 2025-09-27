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
 * Simple Failure Policy Terminate Test - demonstrates APEX "terminate" failure policy.
 * 
 * This test focuses specifically on the "terminate" failure policy behavior:
 * - When a stage fails with "terminate" policy, processing stops immediately
 * - All subsequent stages are marked as SKIPPED
 * - The ScenarioExecutionResult.isTerminated() returns true
 * - No further processing occurs after the failure
 *
 * Uses DataTypeScenarioService to execute scenarios with stages and verifies
 * that termination behavior works correctly.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Failure Policy Terminate Test")
public class SimpleFailurePolicyTerminateTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFailurePolicyTerminateTest.class);
    
    private DataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up terminate failure policy test environment");

        // Initialize scenario service - it creates its own dependencies
        scenarioService = new DataTypeScenarioService();

        logger.info("✓ Test environment initialized with DataTypeScenarioService");
    }

    @Test
    @DisplayName("Test terminate failure policy - stops processing on stage failure")
    void testTerminateFailurePolicy() throws Exception {
        logger.info("=== Testing Terminate Failure Policy ===");

        // Load terminate policy scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyTerminateTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("terminate-policy-test");
        assertNotNull(scenario, "Terminate policy scenario should be loaded");

        // Create test data that will cause validation stage to fail (missing required fields)
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("someField", "someValue");
        // Missing 'amount' and 'customerName' - will cause validation rules to fail

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(invalidData, scenario);
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ScenarioExecutionResult, "Result should be ScenarioExecutionResult");

        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify terminate policy behavior
        assertTrue(scenarioResult.isTerminated(), "Scenario should be terminated due to validation failure");
        assertFalse(scenarioResult.isSuccessful(), "Scenario should not be successful");
        
        // Verify second stage was skipped
        assertTrue(scenarioResult.getSkippedStages().containsKey("enrichment"), 
                  "Enrichment stage should be skipped due to termination");
        
        logger.info("✓ Terminate policy correctly stopped processing after validation failure");
        logger.info("✓ Enrichment stage was skipped as expected");
        logger.info("✅ Terminate failure policy test completed");
    }

    @Test
    @DisplayName("Test terminate policy with multiple stages - all subsequent stages skipped")
    void testTerminateWithMultipleStages() throws Exception {
        logger.info("=== Testing Terminate Policy with Multiple Stages ===");

        // Load scenario with multiple stages
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyTerminateTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("terminate-policy-test");
        assertNotNull(scenario, "Terminate policy scenario should be loaded");

        // Create test data that will cause first stage to fail
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("testField", "testValue");

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(invalidData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify termination behavior
        assertTrue(scenarioResult.isTerminated(), "Scenario should be terminated");
        assertFalse(scenarioResult.getSkippedStages().isEmpty(), "Some stages should be skipped");
        
        logger.info("✓ Multiple stages correctly skipped after termination");
        logger.info("✓ Skipped stages: {}", scenarioResult.getSkippedStages().keySet());
        logger.info("✅ Multi-stage terminate test completed");
    }
}
