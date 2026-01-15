package dev.mars.apex.core.config.component;

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

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.core.service.scenario.StageExecutionResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for component failure-policy handling.
 * 
 * Tests verify that failure policies work correctly when components are used in scenarios:
 * - terminate: Stop processing when component file fails
 * - continue-with-warnings: Continue processing with warnings
 * - flag-for-review: Flag scenario for review but continue
 * 
 * Uses the partial-sections-component.yaml which has:
 * - component-ref with "continue-with-warnings" policy
 * - config-file with "terminate" policy
 * - config-file with "flag-for-review" policy
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 */
@DisplayName("Component Failure Policy Integration Tests")
class ComponentFailurePolicyTest {

    private static final Logger logger = LoggerFactory.getLogger(ComponentFailurePolicyTest.class);

    @BeforeEach
    void setUp() {
        logger.info("=".repeat(80));
    }

    @AfterEach
    void tearDown() {
        logger.info("=".repeat(80));
        logger.info("");
    }

    @Test
    @DisplayName("Should execute component successfully when all files pass")
    void testComponentSuccessfulExecution() throws Exception {
        logger.info("=== Testing Component Successful Execution ===");

        // Load scenario that uses partial-sections-component
        RulesEngine engine = RulesEngine.fromScenarioRegistry(
            "src/test/resources/scenario/component-failure-policy-test-registry.yaml"
        );

        // Create valid test data that matches the validation rules
        Map<String, Object> validData = new HashMap<>();
        validData.put("tradeId", "TRADE-001");
        validData.put("instrumentType", "OPTION");
        validData.put("quantity", 100.0);
        validData.put("price", 50.0);
        validData.put("currency", "USD");

        // Execute scenario
        ScenarioExecutionResult result = engine.evaluateScenario("component-success-test", validData);

        // Log detailed results
        logger.info("\n=== DETAILED REPORT ===");
        logger.info(result.getDetailedReport());
        logger.info("\n=== EXECUTION SUMMARY ===");
        logger.info(result.getExecutionSummary());
        logger.info("\n=== STAGE RESULTS ===");
        result.getStageResults().forEach(stageResult -> {
            logger.info("Stage: {} - ResultType: {} - Successful: {} - Error: {}",
                stageResult.getStageName(),
                stageResult.getResultType(),
                stageResult.isSuccessful(),
                stageResult.getErrorMessage());
        });

        // Verify successful execution
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccessful(), "Scenario should be successful. Summary: " + result.getExecutionSummary());
        assertFalse(result.isTerminated(), "Scenario should not be terminated");
        assertFalse(result.requiresReview(), "Scenario should not require review");
        assertFalse(result.hasWarnings(), "Scenario should not have warnings");

        logger.info("✓ Component executed successfully with all files passing");
    }

    @Test
    @DisplayName("Should continue with warnings when component-ref fails with continue-with-warnings policy")
    void testComponentRefContinueWithWarnings() throws Exception {
        logger.info("=== Testing Component-Ref Continue-With-Warnings Policy ===");

        // Load scenario
        RulesEngine engine = RulesEngine.fromScenarioRegistry(
            "src/test/resources/scenario/component-failure-policy-test-registry.yaml"
        );

        // Create data that will fail validation in the nested component
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("tradeId", "TRADE-002");
        // Missing required fields to trigger validation failure

        // Execute scenario
        ScenarioExecutionResult result = engine.evaluateScenario("component-continue-test", invalidData);

        // Verify continue-with-warnings behavior
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTerminated(), "Scenario should not be terminated");
        assertTrue(result.hasWarnings(), "Scenario should have warnings");

        logger.info("✓ Component-ref failed but processing continued with warnings");
    }

    @Test
    @DisplayName("Should terminate when config-file fails with terminate policy")
    void testConfigFileTerminatePolicy() throws Exception {
        logger.info("=== Testing Config-File Terminate Policy ===");

        // Load scenario
        RulesEngine engine = RulesEngine.fromScenarioRegistry(
            "src/test/resources/scenario/component-failure-policy-test-registry.yaml"
        );

        // Create data that will fail the config-file with terminate policy
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("tradeId", "TRADE-003");
        // Data designed to fail enrichment rules

        // Execute scenario
        ScenarioExecutionResult result = engine.evaluateScenario("component-terminate-test", invalidData);

        // Verify terminate behavior
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTerminated(), "Scenario should be terminated");
        assertFalse(result.isSuccessful(), "Scenario should not be successful");

        logger.info("✓ Config-file failed with terminate policy - scenario terminated");
    }
}

