package dev.mars.apex.demo.errorhandling;

import dev.mars.apex.engine.core.RulesEngine;
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
 * Test class to validate that APEX configuration errors are handled gracefully
 * according to the principles in prompts.txt:
 *
 * - Configuration errors should NEVER throw exceptions that break application flow
 * - Errors should be logged as warnings, not thrown as exceptions
 * - System should continue processing with reasonable defaults
 * - Failure policies should work correctly with configuration errors
 *
 * This test specifically validates the fix for ConfigurationException being
 * thrown for missing field-mappings in calculation-enrichment configurations.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("APEX Configuration Error Handling Tests")
public class SimpleFailurePolicyConfigurationErrorTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFailurePolicyConfigurationErrorTest.class);

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up configuration error handling test environment");
        logger.info("Test environment initialized for RulesEngine scenario testing");
    }

    @Test
    @DisplayName("Test missing field-mappings in calculation-enrichment - should continue with warnings, not throw exception")
    void testMissingFieldMappingsInCalculationEnrichment() throws Exception {
        logger.info("=== Testing Missing Field-Mappings Configuration Error ===");

        // Load configuration error test scenario using RulesEngine
        RulesEngine engine = RulesEngine.fromScenarioRegistry("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyConfigurationErrorTest.yaml");

        // Test data that will trigger validation failure AND configuration error
        Map<String, Object> testData = new HashMap<>();
        testData.put("tradeId", "TEST-001");
        // Deliberately missing 'amount' and 'customerName' to trigger validation failure

        // Execute scenario - this should NOT throw an exception
        ScenarioExecutionResult result = engine.evaluateScenario("config-error-continue-test", testData);
        assertNotNull(result, "Result should not be null - processing should complete gracefully");

        // Validate that the scenario was not terminated (continue-with-warnings policy)
        assertFalse(result.isTerminated(), "Scenario should not be terminated with continue-with-warnings policy");

        // Validate that warnings were collected for both validation and configuration errors
        assertTrue(result.hasWarnings(), "Result should have warnings for both validation and configuration errors");

        // Validate that both stages attempted to execute (even though they failed)
        assertEquals(2, result.getStageResults().size(), "Both stages should have execution results");

        // Validate stage results: validation should fail, enrichment should succeed with warnings
        result.getStageResults().forEach(stageResult -> {
            logger.info("Stage result: {} - {}", stageResult.getStageName(), stageResult.getResultType());
            if ("validation".equals(stageResult.getStageName())) {
                assertFalse(stageResult.isSuccessful(), "Validation stage should fail due to SpEL property not found");
            } else if ("enrichment".equals(stageResult.getStageName())) {
                assertTrue(stageResult.isSuccessful(), "Enrichment stage should succeed despite configuration warnings");
            }
        });

        logger.info("Configuration errors handled gracefully without exceptions");
        logger.info("Warnings collected: {}", result.getWarnings().size());
        logger.info("Continue-with-warnings policy working correctly");
    }

    @Test
    @DisplayName("Test configuration error with terminate policy - should terminate gracefully, not throw exception")
    void testConfigurationErrorWithTerminatePolicy() throws Exception {
        logger.info("=== Testing Configuration Error with Terminate Policy ===");

        // Load terminate policy scenario using RulesEngine
        RulesEngine engine = RulesEngine.fromScenarioRegistry("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyConfigurationErrorTest-terminate.yaml");

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("tradeId", "TEST-002");

        // Execute scenario with terminate policy
        ScenarioExecutionResult result = engine.evaluateScenario("config-error-terminate-test", testData);
        assertNotNull(result, "Result should not be null - processing should complete gracefully");

        // Validate graceful termination without exceptions
        assertTrue(result.isTerminated(), "Scenario should be terminated due to terminate policy");

        // Should have at least one stage result (the one that failed)
        assertFalse(result.getStageResults().isEmpty(), "Should have at least one stage result");

        // Validate stage results: enrichment should succeed, validation should fail and cause termination
        result.getStageResults().forEach(stageResult -> {
            logger.info("Stage result: {} - {}", stageResult.getStageName(), stageResult.getResultType());
            if ("enrichment".equals(stageResult.getStageName())) {
                assertTrue(stageResult.isSuccessful(), "Enrichment stage should succeed despite configuration warnings");
            } else if ("validation".equals(stageResult.getStageName())) {
                assertFalse(stageResult.isSuccessful(), "Validation stage should fail and cause termination");
            }
        });

        logger.info("Configuration error with terminate policy handled gracefully");
        logger.info("No exceptions thrown, proper termination behavior");
    }

    @Test
    @DisplayName("Test multiple configuration errors - should collect all errors as warnings")
    void testMultipleConfigurationErrors() throws Exception {
        logger.info("=== Testing Multiple Configuration Errors ===");

        // Load multiple errors scenario using RulesEngine
        RulesEngine engine = RulesEngine.fromScenarioRegistry("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyConfigurationErrorTest-multiple.yaml");

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("tradeId", "TEST-003");

        // Execute scenario with multiple configuration errors
        ScenarioExecutionResult result = engine.evaluateScenario("config-error-multiple-test", testData);
        assertNotNull(result, "Result should not be null");

        // Validate graceful handling of multiple errors
        assertFalse(result.isTerminated(), "Should continue with warnings despite multiple errors");
        assertTrue(result.hasWarnings(), "Should have warnings for multiple configuration errors");

        // Should attempt to execute all stages
        assertTrue(result.getStageResults().size() >= 2, "Should attempt multiple stages");

        logger.info("Multiple configuration errors handled gracefully");
        logger.info("All errors converted to warnings, no exceptions thrown");
    }
}
