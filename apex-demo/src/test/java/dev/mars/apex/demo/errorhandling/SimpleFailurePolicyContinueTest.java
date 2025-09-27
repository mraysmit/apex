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
 * Simple Failure Policy Continue Test - demonstrates APEX "continue-with-warnings" failure policy.
 * 
 * This test focuses specifically on the "continue-with-warnings" failure policy behavior:
 * - When a stage fails with "continue-with-warnings" policy, warnings are logged
 * - Processing continues to the next stage despite the failure
 * - The ScenarioExecutionResult.hasWarnings() returns true
 * - All stages execute even if some fail
 *
 * Uses DataTypeScenarioService to execute scenarios with stages and verifies
 * that continue-with-warnings behavior works correctly.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Failure Policy Continue Test")
public class SimpleFailurePolicyContinueTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFailurePolicyContinueTest.class);
    
    private DataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up continue-with-warnings failure policy test environment");

        // Initialize scenario service - it creates its own dependencies
        scenarioService = new DataTypeScenarioService();

        logger.info("✓ Test environment initialized with DataTypeScenarioService");
    }

    @Test
    @DisplayName("Test continue-with-warnings failure policy - continues processing with warnings")
    void testContinueWithWarningsFailurePolicy() throws Exception {
        logger.info("=== Testing Continue With Warnings Failure Policy ===");

        // Load continue-with-warnings policy scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyContinueTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("continue-with-warnings-test");
        assertNotNull(scenario, "Continue with warnings scenario should be loaded");

        // Create test data that will cause validation stage to fail
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("someField", "someValue");
        // Missing 'amount' and 'customerName' - will cause validation rules to fail

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(invalidData, scenario);
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ScenarioExecutionResult, "Result should be ScenarioExecutionResult");

        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify continue-with-warnings policy behavior
        assertFalse(scenarioResult.isTerminated(), "Scenario should not be terminated");
        assertTrue(scenarioResult.hasWarnings(), "Scenario should have warnings from validation failure");
        
        // Verify both stages executed (even though first failed)
        assertEquals(2, scenarioResult.getStageResults().size(), "Both stages should have executed");
        assertTrue(scenarioResult.getSkippedStages().isEmpty(), "No stages should be skipped");
        
        logger.info("✓ Continue-with-warnings policy allowed processing to continue");
        logger.info("✓ Warnings were collected: {}", scenarioResult.getWarnings().size());
        logger.info("✅ Continue-with-warnings failure policy test completed");
    }

    @Test
    @DisplayName("Test continue policy with multiple failing stages - all stages execute")
    void testContinueWithMultipleFailures() throws Exception {
        logger.info("=== Testing Continue Policy with Multiple Failures ===");

        // Load scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyContinueTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("continue-with-warnings-test");
        assertNotNull(scenario, "Continue with warnings scenario should be loaded");

        // Create test data that will cause multiple stages to have issues
        Map<String, Object> problematicData = new HashMap<>();
        problematicData.put("testField", "testValue");

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(problematicData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify continue behavior with multiple issues
        assertFalse(scenarioResult.isTerminated(), "Scenario should not be terminated");
        assertTrue(scenarioResult.getSkippedStages().isEmpty(), "No stages should be skipped");
        assertFalse(scenarioResult.getStageResults().isEmpty(), "All stages should have executed");
        
        logger.info("✓ All stages executed despite failures");
        logger.info("✓ Stage results count: {}", scenarioResult.getStageResults().size());
        logger.info("✅ Multi-failure continue test completed");
    }
}
