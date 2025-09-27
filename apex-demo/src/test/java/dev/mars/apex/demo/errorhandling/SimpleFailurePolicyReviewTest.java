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
 * Simple Failure Policy Review Test - demonstrates APEX "flag-for-review" failure policy.
 * 
 * This test focuses specifically on the "flag-for-review" failure policy behavior:
 * - When a stage fails with "flag-for-review" policy, the scenario is flagged for manual review
 * - Processing continues to the next stage despite the failure
 * - The ScenarioExecutionResult.requiresReview() returns true
 * - Review flags are set for manual intervention
 *
 * Uses DataTypeScenarioService to execute scenarios with stages and verifies
 * that flag-for-review behavior works correctly.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Failure Policy Review Test")
public class SimpleFailurePolicyReviewTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFailurePolicyReviewTest.class);
    
    private DataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up flag-for-review failure policy test environment");

        // Initialize scenario service - it creates its own dependencies
        scenarioService = new DataTypeScenarioService();

        logger.info("✓ Test environment initialized with DataTypeScenarioService");
    }

    @Test
    @DisplayName("Test flag-for-review failure policy - flags for review but continues processing")
    void testFlagForReviewFailurePolicy() throws Exception {
        logger.info("=== Testing Flag For Review Failure Policy ===");

        // Load flag-for-review policy scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyReviewTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("flag-for-review-test");
        assertNotNull(scenario, "Flag for review scenario should be loaded");

        // Create test data that will cause validation stage to fail
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("someField", "someValue");
        // Missing 'amount' and 'customerName' - will cause validation rules to fail

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(invalidData, scenario);
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ScenarioExecutionResult, "Result should be ScenarioExecutionResult");

        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify flag-for-review policy behavior
        assertFalse(scenarioResult.isTerminated(), "Scenario should not be terminated");
        assertTrue(scenarioResult.requiresReview(), "Scenario should be flagged for review");
        assertTrue(scenarioResult.hasReviewFlags(), "Scenario should have review flags");
        
        // Verify both stages executed (even though first failed)
        assertEquals(2, scenarioResult.getStageResults().size(), "Both stages should have executed");
        assertTrue(scenarioResult.getSkippedStages().isEmpty(), "No stages should be skipped");
        
        logger.info("✓ Flag-for-review policy flagged scenario for manual review");
        logger.info("✓ Review flags were set: {}", scenarioResult.getReviewFlags().size());
        logger.info("✅ Flag-for-review failure policy test completed");
    }

    @Test
    @DisplayName("Test review policy with successful subsequent stages")
    void testReviewPolicyWithSuccessfulStages() throws Exception {
        logger.info("=== Testing Review Policy with Successful Subsequent Stages ===");

        // Load scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyReviewTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("flag-for-review-test");
        assertNotNull(scenario, "Flag for review scenario should be loaded");

        // Create test data that will cause first stage to fail but others to succeed
        Map<String, Object> mixedData = new HashMap<>();
        mixedData.put("testField", "testValue");

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(mixedData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify review behavior with mixed results
        assertTrue(scenarioResult.requiresReview(), "Scenario should require review");
        assertFalse(scenarioResult.isTerminated(), "Scenario should not be terminated");
        assertTrue(scenarioResult.getSkippedStages().isEmpty(), "No stages should be skipped");
        
        logger.info("✓ Review flags set despite successful subsequent stages");
        logger.info("✓ All stages executed: {}", scenarioResult.getStageResults().size());
        logger.info("✅ Mixed results review test completed");
    }
}
