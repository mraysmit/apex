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
 * Regression test for nested rule-ref path resolution inside stage YAMLs.
 *
 * A stage YAML may declare rule-refs with short relative paths such as
 * "rules/extra-rules.yaml". Those paths must resolve relative to the stage
 * YAML file's own directory, not the JVM working directory.
 *
 * YAML tree under nested-ref-path-resolution/:
 *   registry.yaml
 *   scenarios/
 *     fx-nested-scenario.yaml  (stage config-file: "stages/fx-stage.yaml")
 *     stages/
 *       fx-stage.yaml          (rule-refs source: "rules/extra-rules.yaml")
 *       rules/
 *         extra-rules.yaml
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class NestedRefPathResolutionTest {

    private static final Logger logger = LoggerFactory.getLogger(NestedRefPathResolutionTest.class);

    private static final String REGISTRY_PATH =
        "src/test/java/dev/mars/apex/core/config/loader/nested-ref-path-resolution/registry.yaml";

    @Test
    @DisplayName("Relative rule-ref inside a stage YAML resolves relative to the stage file's directory")
    void testRelativeRuleRefInsideStageResolves() throws Exception {
        logger.info("=== Nested Ref Path Resolution ===");

        RulesEngine engine = RulesEngine.fromScenarioRegistry(REGISTRY_PATH);
        Map<String, Object> tradeData = createTradeData();

        ScenarioExecutionResult result = engine.evaluateScenario("fx-nested", tradeData);

        logger.info("  Execution status : {}", result.getExecutionStatus());
        logger.info("  Successful       : {}", result.isSuccessful());
        logStageDetails(result.getStageResults());

        assertTrue(result.isSuccessful(),
            "Scenario should succeed — relative rule-ref must resolve. Stage errors: "
                + collectStageErrors(result.getStageResults()));

        boolean validationStageRan = result.getStageResults().stream()
            .anyMatch(s -> "fx-validation".equals(s.getStageName()));
        assertTrue(validationStageRan, "Stage 'fx-validation' must appear in results");

        logger.info("=== TEST PASSED: relative rule-ref inside stage resolved correctly ===");
    }

    // -------------------------------------------------------------------------

    private Map<String, Object> createTradeData() {
        Map<String, Object> data = new HashMap<>();
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
