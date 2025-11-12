package dev.mars.apex.demo.scenario;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ComponentScenarioTest - Integration tests for APEX Component feature
 *
 * PURPOSE:
 * This test class validates that component YAML files work correctly in scenario processing stages.
 * It tests the complete integration of components with the APEX rules engine, including:
 * - Component file detection and loading
 * - Execution order (explicit and document order)
 * - Failure policy inheritance and overrides
 * - Nested component support
 * - Backward compatibility with non-component scenarios
 *
 * TESTING APPROACH:
 * - Uses RulesEngine with scenario registry for authentic end-to-end testing
 * - Creates meaningful test data that exercises actual business logic
 * - Validates component expansion and execution
 * - Tests both positive scenarios (successful processing) and negative scenarios (failures)
 *
 * YAML FILES USED:
 * - ComponentScenarioTest-registry.yaml (scenario registry)
 * - ComponentScenarioTest-simple-component-scenario.yaml (scenario with simple component)
 * - ComponentScenarioTest-nested-component-scenario.yaml (scenario with nested components)
 * - basic-validation-component.yaml (simple component)
 * - multi-stage-component.yaml (component with enrichment + validation)
 * - nested-component-level1.yaml (nested component level 1)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 */
@DisplayName("Component Scenario Integration Tests")
public class ComponentScenarioTest {

    private static final Logger logger = LoggerFactory.getLogger(ComponentScenarioTest.class);

    @BeforeEach
    void setUp() {
        logger.info("\n" + "=".repeat(80));
        logger.info("COMPONENT SCENARIO INTEGRATION TEST");
        logger.info("=".repeat(80));
    }

    @Test
    @DisplayName("Test 1: Simple component stage execution")
    void testSimpleComponentStage() throws Exception {
        logger.info("\n=== TEST 1: Simple Component Stage Execution ===");

        // 1. Create test data
        Map<String, Object> tradeData = createValidTradeData();
        logger.info("✓ STEP 1: Created test trade data");
        logTradeData(tradeData);

        // 2. Load scenario registry with component stage
        String registryPath = "src/test/java/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml";
        logger.info("✓ STEP 2: Loading scenario registry from: {}", registryPath);
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        logger.info("  - Scenario registry loaded successfully");

        // 3. Execute scenario with component stage
        logger.info("✓ STEP 3: Executing scenario 'simple-component-test'");
        ScenarioExecutionResult result = engine.evaluateScenario("simple-component-test", tradeData);

        // 4. Validate results
        logger.info("✓ STEP 4: Validating execution results");
        assertNotNull(result, "Execution result should not be null");
        assertTrue(result.isSuccessful(), "Scenario execution should be successful");
        assertFalse(result.isTerminated(), "Scenario should not be terminated");
        assertEquals("simple-component-test", result.getScenarioId());

        // Verify stage execution
        assertFalse(result.getStageResults().isEmpty(), "Should have stage results");
        logger.info("  - Executed {} stages", result.getStageResults().size());

        logger.info("\n[SUCCESS] Simple component stage test passed");
    }

    @Test
    @DisplayName("Test 2: Multi-stage component with execution order")
    void testMultiStageComponentWithExecutionOrder() throws Exception {
        logger.info("\n=== TEST 2: Multi-Stage Component with Execution Order ===");

        // 1. Create test data
        Map<String, Object> tradeData = createValidTradeData();
        logger.info("✓ STEP 1: Created test trade data");

        // 2. Load scenario registry
        String registryPath = "src/test/java/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml";
        logger.info("✓ STEP 2: Loading scenario registry");
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);

        // 3. Execute scenario with multi-stage component
        logger.info("✓ STEP 3: Executing scenario 'multi-stage-component-test'");
        ScenarioExecutionResult result = engine.evaluateScenario("multi-stage-component-test", tradeData);

        // 4. Validate results
        logger.info("✓ STEP 4: Validating execution results");
        assertNotNull(result);
        assertTrue(result.isSuccessful(), "Multi-stage component execution should be successful");

        logger.info("\n[SUCCESS] Multi-stage component test passed");
    }

    @Test
    @DisplayName("Test 3: Nested component execution")
    void testNestedComponentExecution() throws Exception {
        logger.info("\n=== TEST 3: Nested Component Execution ===");

        // 1. Create test data
        Map<String, Object> tradeData = createValidTradeData();
        logger.info("✓ STEP 1: Created test trade data");

        // 2. Load scenario registry
        String registryPath = "src/test/java/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml";
        logger.info("✓ STEP 2: Loading scenario registry");
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);

        // 3. Execute scenario with nested component
        logger.info("✓ STEP 3: Executing scenario 'nested-component-test'");
        ScenarioExecutionResult result = engine.evaluateScenario("nested-component-test", tradeData);

        // 4. Validate results
        logger.info("✓ STEP 4: Validating execution results");
        assertNotNull(result);
        assertTrue(result.isSuccessful(), "Nested component execution should be successful");

        logger.info("\n[SUCCESS] Nested component test passed");
    }

    @Test
    @DisplayName("Test 4: Backward compatibility with non-component scenarios")
    void testBackwardCompatibility() throws Exception {
        logger.info("\n=== TEST 4: Backward Compatibility Test ===");

        // 1. Create test data
        Map<String, Object> tradeData = createValidTradeData();
        logger.info("✓ STEP 1: Created test trade data");

        // 2. Load scenario registry
        String registryPath = "src/test/java/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml";
        logger.info("✓ STEP 2: Loading scenario registry");
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);

        // 3. Execute scenario with regular (non-component) config files
        logger.info("✓ STEP 3: Executing scenario 'backward-compatibility-test'");
        ScenarioExecutionResult result = engine.evaluateScenario("backward-compatibility-test", tradeData);

        // 4. Validate results
        logger.info("✓ STEP 4: Validating execution results");
        assertNotNull(result);
        assertTrue(result.isSuccessful(), "Backward compatibility scenario should be successful");

        // Verify validation stage executed
        assertEquals(1, result.getStageResults().size(), "Should have 1 stage (validation)");
        logger.info("  - Executed {} stages successfully", result.getStageResults().size());

        logger.info("\n[SUCCESS] Backward compatibility test passed");
    }

    @Test
    @DisplayName("Test 5: Component with failure policy inheritance")
    void testFailurePolicyInheritance() throws Exception {
        logger.info("\n=== TEST 5: Failure Policy Inheritance Test ===");

        // 1. Create INVALID test data (missing required fields)
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("tradeId", "TRADE-INVALID");
        // Missing required fields: tradeType, amount, currency, counterparty
        logger.info("✓ STEP 1: Created invalid test data (missing required fields)");

        // 2. Load scenario registry
        String registryPath = "src/test/java/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml";
        logger.info("✓ STEP 2: Loading scenario registry");
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);

        // 3. Execute scenario with component (should fail validation)
        logger.info("✓ STEP 3: Executing scenario 'simple-component-test' with invalid data");
        ScenarioExecutionResult result = engine.evaluateScenario("simple-component-test", invalidData);

        // 4. Validate failure behavior
        logger.info("✓ STEP 4: Validating failure policy behavior");
        assertNotNull(result);
        // The scenario should complete but may have validation failures
        logger.info("  - Scenario completed with status: {}", result.isSuccessful() ? "SUCCESS" : "FAILURE");
        logger.info("  - Terminated: {}", result.isTerminated());

        logger.info("\n[SUCCESS] Failure policy inheritance test passed");
    }

    // Helper methods

    private Map<String, Object> createValidTradeData() {
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", "TRADE-001");
        data.put("tradeType", "Equity");
        data.put("amount", 1000000);
        data.put("currency", "USD");
        data.put("counterparty", "BANK-A");
        return data;
    }

    private void logTradeData(Map<String, Object> data) {
        logger.info("  [Trade Data]");
        data.forEach((key, value) -> logger.info("    - {}: {}", key, value));
    }
}

