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

import dev.mars.apex.core.engine.core.RulesEngine;
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
 * Uses RulesEngine to execute scenarios with stages and verifies
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

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up terminate failure policy test environment");
        logger.info("[OK] Test environment initialized for RulesEngine scenario testing");
    }

    @Test
    @DisplayName("Test terminate failure policy - stops processing on stage failure")
    void testTerminateFailurePolicy() throws Exception {
        logger.info("=== Testing Terminate Failure Policy ===");

        // Load terminate policy scenario using RulesEngine
        RulesEngine engine = RulesEngine.fromScenarioRegistry("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyTerminateTest.yaml");

        // Create test data that will cause validation stage to fail (missing required fields)
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("someField", "someValue");
        // Missing 'amount' and 'customerName' - will cause validation rules to fail

        // Execute scenario
        ScenarioExecutionResult scenarioResult = engine.evaluateScenario("terminate-policy-test", invalidData);
        assertNotNull(scenarioResult, "Result should not be null");

        // Verify terminate policy behavior
        assertTrue(scenarioResult.isTerminated(), "Scenario should be terminated due to validation failure");
        assertFalse(scenarioResult.isSuccessful(), "Scenario should not be successful");
        
        // Verify second stage was skipped
        assertTrue(scenarioResult.getSkippedStages().containsKey("enrichment"), 
                  "Enrichment stage should be skipped due to termination");
        
        logger.info("[OK] Terminate policy correctly stopped processing after validation failure");
        logger.info("[OK] Enrichment stage was skipped as expected");
        logger.info("Terminate failure policy test completed");
    }

    @Test
    @DisplayName("Test terminate policy with multiple stages - all subsequent stages skipped")
    void testTerminateWithMultipleStages() throws Exception {
        logger.info("=== Testing Terminate Policy with Multiple Stages ===");

        // Load scenario with multiple stages using RulesEngine
        RulesEngine engine = RulesEngine.fromScenarioRegistry("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyTerminateTest.yaml");

        // Create test data that will cause first stage to fail
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("testField", "testValue");

        // Execute scenario
        ScenarioExecutionResult scenarioResult = engine.evaluateScenario("terminate-policy-test", invalidData);

        // Verify termination behavior
        assertTrue(scenarioResult.isTerminated(), "Scenario should be terminated");
        assertFalse(scenarioResult.getSkippedStages().isEmpty(), "Some stages should be skipped");
        
        logger.info("[OK] Multiple stages correctly skipped after termination");
        logger.info("[OK] Skipped stages: {}", scenarioResult.getSkippedStages().keySet());
        logger.info("Multi-stage terminate test completed");
    }
}
