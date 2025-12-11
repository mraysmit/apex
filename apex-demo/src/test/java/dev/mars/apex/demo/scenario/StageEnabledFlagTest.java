package dev.mars.apex.demo.scenario;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Stage Enabled Flag feature.
 * 
 * Tests the use case from APEX_SCENARIO_GUIDE.md:
 * Feature Toggle - Disable legacy system, enable new system
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Stage Enabled Flag Test")
class StageEnabledFlagTest {

    private static final Logger logger = LoggerFactory.getLogger(StageEnabledFlagTest.class);

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("stage-enabled-test");
    }

    @Test
    @DisplayName("Should skip disabled stage and execute enabled stage")
    void testFeatureToggle() throws Exception {
        logger.info("=".repeat(80));
        logger.info("TEST: FEATURE TOGGLE - LEGACY DISABLED, NEW SYSTEM ENABLED");
        logger.info("=".repeat(80));

        // Create registry
        String registryYaml = createRegistryYaml();
        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);
        logger.info("[OK] Registry created: {}", registryFile);

        // Create scenario with legacy stage DISABLED and new stage ENABLED
        String scenarioYaml = createScenarioYaml();
        Path scenarioFile = tempDir.resolve("feature-toggle-scenario.yaml");
        Files.writeString(scenarioFile, scenarioYaml);
        logger.info("[OK] Scenario created: {}", scenarioFile);

        // Create legacy compliance rules (will be skipped)
        String legacyRulesYaml = createLegacyRulesYaml();
        Path legacyFile = tempDir.resolve("legacy-compliance.yaml");
        Files.writeString(legacyFile, legacyRulesYaml);
        logger.info("[OK] Legacy rules created: {}", legacyFile);

        // Create new compliance rules (will execute)
        String newRulesYaml = createNewRulesYaml();
        Path newFile = tempDir.resolve("new-compliance.yaml");
        Files.writeString(newFile, newRulesYaml);
        logger.info("[OK] New rules created: {}", newFile);
        
        // DEBUG: Log scenario YAML content to verify enabled flag
        logger.info("\n[DEBUG] Scenario YAML content:\n{}", scenarioYaml);

        // Load registry and create test data
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryFile.toString());
        logger.info("[OK] Registry loaded successfully");

        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("tradeType", "EquityTrade");
        tradeData.put("region", "US");
        tradeData.put("notional", 1000000);

        logger.info("\n[DATA] Input trade data:");
        logger.info("  - Trade Type: {}", tradeData.get("tradeType"));
        logger.info("  - Region: {}", tradeData.get("region"));
        logger.info("  - Notional: ${}", tradeData.get("notional"));

        // DEBUG: Print scenario configuration to verify stages are parsed correctly
        logger.info("\n[DEBUG] Inspecting loaded scenario configuration...");
        // Note: RulesEngine doesn't expose scenario config directly, so we'll check results
        
        // Execute scenario
        ScenarioExecutionResult result = engine.evaluateWithClassification(tradeData);

        // Assertions
        assertNotNull(result, "Scenario execution result should not be null");
        assertEquals("feature-toggle", result.getScenarioId(), "Should execute feature-toggle scenario");
        assertTrue(result.isSuccessful(), "Scenario execution should be successful");

        List<String> executedStages = result.getStageResults().stream()
            .map(StageExecutionResult::getStageName)
            .toList();

        logger.info("\n[RESULTS] Executed stages: {}", executedStages);
        logger.info("[RESULTS] Skipped stages: {}", result.getSkippedStages());

        // Verify legacy stage was SKIPPED (enabled=false)
        assertTrue(result.getSkippedStages().containsKey("legacy-compliance"), 
            "Legacy compliance stage should be SKIPPED (enabled=false)");
        logger.info("[OK] Legacy stage was skipped as expected");

        // Verify new stage was EXECUTED (enabled=true)
        assertTrue(executedStages.contains("new-compliance"), 
            "New compliance stage should be EXECUTED (enabled=true)");
        logger.info("[OK] New compliance stage was executed");

        logger.info("\n" + "=".repeat(80));
        logger.info("[SUCCESS] FEATURE TOGGLE TEST PASSED");
        logger.info("=".repeat(80));
    }

    private String createRegistryYaml() {
        String path = tempDir.toString().replace("\\", "/");
        return """
            metadata:
              id: "feature-toggle-registry"
              name: "Feature Toggle Registry"
              version: "1.0.0"
              description: "Registry for feature toggle testing"
              type: "scenario-registry"

            scenarios:
              - scenario-id: "feature-toggle"
                config-file: "%s/feature-toggle-scenario.yaml"
                business-domain: "Trading"
            """.formatted(path);
    }

    private String createScenarioYaml() {
        String path = tempDir.toString().replace("\\", "/");
        return """
            metadata:
              id: "feature-toggle"
              name: "Feature Toggle Scenario"
              version: "1.0.0"
              description: "Testing enabled/disabled stages"
              type: "scenario"

            scenario:
              scenario-id: "feature-toggle"
              name: "Feature Toggle Test"
              
              classification-rule:
                condition: "#root['tradeType'] == 'EquityTrade'"
                description: "Equity trades"

              processing-stages:
                - stage-name: "legacy-compliance"
                  config-file: "%s/legacy-compliance.yaml"
                  execution-order: 1
                  enabled: false
                  required: false
                  stage-metadata:
                    description: "Legacy compliance - being phased out"

                - stage-name: "new-compliance"
                  config-file: "%s/new-compliance.yaml"
                  execution-order: 2
                  enabled: true
                  required: true
                  stage-metadata:
                    description: "New compliance system"
            """.formatted(path, path);
    }

    private String createLegacyRulesYaml() {
        return """
            metadata:
              id: "legacy-compliance"
              name: "Legacy Compliance Rules"
              version: "1.0.0"
              description: "Old compliance system"
              type: "rule-config"
            
            rules:
              - id: "legacy-check"
                name: "Legacy Compliance Check"
                condition: "true"
                message: "Legacy compliance checked"
                enabled: true
            """;
    }

    private String createNewRulesYaml() {
        return """
            metadata:
              id: "new-compliance"
              name: "New Compliance Rules"
              version: "1.0.0"
              description: "New compliance system"
              type: "rule-config"
            
            rules:
              - id: "new-check"
                name: "New Compliance Check"
                condition: "#root['notional'] > 0"
                message: "New compliance checked"
                enabled: true
            """;
    }
}
