/*
 * Copyright (c) 2024 Mark Andrew Ray-Smith Cityline Ltd
 * All rights reserved.
 */
package dev.mars.apex.core.config.loader;

import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.core.service.scenario.StageExecutionResult;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import dev.mars.apex.engine.core.RulesEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for scenario stage config-file path resolution.
 *
 * A scenario YAML may reference its stages with short relative paths
 * (e.g. "stages/validation.yaml"). {@link ScenarioRegistryLoader} resolves
 * those paths relative to the scenario file's own directory before the stage
 * executor attempts to load them, so they work regardless of the JVM working
 * directory.
 *
 * YAML tree under stage-path-resolution/:
 *   registry.yaml
 *   scenarios/
 *     fx-validation-scenario.yaml  (stage config-file: "stages/validation.yaml")
 *     stages/
 *       validation.yaml
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class StageConfigFilePathResolutionTest {

    private static final Logger logger = LoggerFactory.getLogger(StageConfigFilePathResolutionTest.class);

    private static final String REGISTRY_PATH =
        "src/test/java/dev/mars/apex/core/config/loader/stage-path-resolution/registry.yaml";

    @Test
    @DisplayName("Relative stage path resolves and scenario succeeds")
    void testRelativeStagePathResolves() throws Exception {
        logger.info("=== Stage Config-File Path Resolution ===");

        RulesEngine engine = RulesEngine.fromScenarioRegistry(REGISTRY_PATH);
        Map<String, Object> fxTradeData = createFxTradeData();

        ScenarioExecutionResult result = engine.evaluateScenario("fx-new", fxTradeData);

        logger.info("  Execution status : {}", result.getExecutionStatus());
        logger.info("  Successful       : {}", result.isSuccessful());
        logStageDetails(result.getStageResults());

        assertTrue(result.isSuccessful(),
            "Scenario should succeed. Stage errors: " + collectStageErrors(result.getStageResults()));

        boolean validationStageRan = result.getStageResults().stream()
            .anyMatch(s -> "fx-validation".equals(s.getStageName()));
        assertTrue(validationStageRan, "Stage 'fx-validation' must appear in results");

        logger.info("=== TEST PASSED: stage path resolved correctly ===");
    }

    // -------------------------------------------------------------------------

    private Map<String, Object> createFxTradeData() {
        Map<String, Object> data = new HashMap<>();
        data.put("currencyPair", "EUR/USD");
        data.put("notional", 1_000_000.0);
        data.put("direction", "BUY");
        data.put("tradeId", "FX-20260521-001");
        return data;
    }

    private void logStageDetails(List<StageExecutionResult> stages) {
        for (StageExecutionResult stage : stages) {
            logger.info("  Stage '{}': resultType={}, successful={}, error={}",
                stage.getStageName(), stage.getResultType(),
                stage.isSuccessful(), stage.getErrorMessage());
        }
    }

    private String collectStageErrors(List<StageExecutionResult> stages) {
        StringBuilder sb = new StringBuilder();
        for (StageExecutionResult stage : stages) {
            if (!stage.isSuccessful() && stage.getErrorMessage() != null) {
                sb.append("[").append(stage.getStageName()).append(": ")
                  .append(stage.getErrorMessage()).append("] ");
            }
        }
        return sb.isEmpty() ? "(none)" : sb.toString();
    }
}
