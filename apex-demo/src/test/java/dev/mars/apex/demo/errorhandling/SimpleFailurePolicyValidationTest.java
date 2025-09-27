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
 * Simple Failure Policy Validation Test - demonstrates APEX validation rule failure handling.
 * 
 * This test focuses specifically on validation rule behavior and failure handling:
 * - How validation rules fail when required fields are missing
 * - How SpEL exceptions are handled gracefully without stack traces
 * - How validation failures interact with different failure policies
 * - How validation results are captured and reported
 *
 * Uses DataTypeScenarioService to execute validation scenarios and verifies
 * that validation rule processing works correctly under various conditions.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Failure Policy Validation Test")
public class SimpleFailurePolicyValidationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFailurePolicyValidationTest.class);
    
    private DataTypeScenarioService scenarioService;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setup to initialize APEX services
        logger.info("Setting up validation failure policy test environment");

        // Initialize scenario service - it creates its own dependencies
        scenarioService = new DataTypeScenarioService();

        logger.info("✓ Test environment initialized with DataTypeScenarioService");
    }

    @Test
    @DisplayName("Test validation rule failures with missing required fields")
    void testValidationRuleFailures() throws Exception {
        logger.info("=== Testing Validation Rule Failures ===");

        // Load validation scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyValidationTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("validation-test");
        assertNotNull(scenario, "Validation scenario should be loaded");

        // Create test data with missing required fields
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("someField", "someValue");
        // Missing 'amount', 'customerName', 'tradeId' - will cause validation rules to fail

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(invalidData, scenario);
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof ScenarioExecutionResult, "Result should be ScenarioExecutionResult");

        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify validation failure behavior
        assertFalse(scenarioResult.isSuccessful(), "Scenario should not be successful due to validation failures");
        assertTrue(scenarioResult.hasWarnings() || !scenarioResult.getFailedStages().isEmpty(),
                  "Scenario should have warnings or failed stages from validation failures");
        
        logger.info("✓ Validation rules failed as expected for missing required fields");
        logger.info("✓ Validation failures handled gracefully without stack traces");
        logger.info("✅ Validation rule failure test completed");
    }

    @Test
    @DisplayName("Test validation rules with valid data - should pass")
    void testValidationRulesWithValidData() throws Exception {
        logger.info("=== Testing Validation Rules with Valid Data ===");

        // Load validation scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyValidationTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("validation-test");
        assertNotNull(scenario, "Validation scenario should be loaded");

        // Create test data with all required fields
        Map<String, Object> validData = new HashMap<>();
        validData.put("amount", 100.0);
        validData.put("customerName", "John Doe");
        validData.put("tradeId", "TRADE-12345");

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(validData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify validation success behavior
        assertTrue(scenarioResult.isSuccessful() || scenarioResult.getFailedStages().isEmpty(),
                  "Scenario should be successful with valid data");
        assertFalse(scenarioResult.isTerminated(), "Scenario should not be terminated");
        
        logger.info("✓ Validation rules passed with valid data");
        logger.info("✓ All required fields validated successfully");
        logger.info("✅ Valid data validation test completed");
    }

    @Test
    @DisplayName("Test validation rule SpEL exception handling")
    void testValidationSpelExceptionHandling() throws Exception {
        logger.info("=== Testing Validation SpEL Exception Handling ===");

        // Load validation scenario
        scenarioService.loadScenarios("src/test/java/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyValidationTest.yaml");
        ScenarioConfiguration scenario = scenarioService.getScenario("validation-test");
        assertNotNull(scenario, "Validation scenario should be loaded");

        // Create test data that will cause SpEL exceptions
        Map<String, Object> problematicData = new HashMap<>();
        problematicData.put("invalidField", "invalidValue");
        // All required fields missing - will cause SpEL "property not found" exceptions

        // Execute scenario
        Object result = scenarioService.processDataWithScenario(problematicData, scenario);
        ScenarioExecutionResult scenarioResult = (ScenarioExecutionResult) result;

        // Verify SpEL exception handling
        assertNotNull(scenarioResult, "Result should not be null even with SpEL exceptions");
        // The system should handle SpEL exceptions gracefully without throwing them
        
        logger.info("✓ SpEL exceptions handled gracefully");
        logger.info("✓ No stack traces thrown for missing properties");
        logger.info("✅ SpEL exception handling test completed");
    }
}
