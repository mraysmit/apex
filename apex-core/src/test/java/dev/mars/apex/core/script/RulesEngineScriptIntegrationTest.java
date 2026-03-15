package dev.mars.apex.core.script;

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

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test: RulesEngine.fromFile() with runtime-scripts configured.
 * Validates the full lifecycle: YAML load → script registry → bridge activation →
 * SpEL evaluation with #script() → result extraction → engine shutdown.
 */
class RulesEngineScriptIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void endToEndScriptEvaluationThroughRulesEngine() throws Exception {
        // Create script directory and script file
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);

        Files.writeString(scriptsDir.resolve("risk-score.groovy"), """
            class RiskScore {
                def run(Map payload) {
                    def notional = payload.get('notional') ?: 0
                    if (notional > 1000000) return 'HIGH'
                    if (notional > 100000) return 'MEDIUM'
                    return 'LOW'
                }
            }
            """);

        // Create YAML config with runtime-scripts and a rule that uses #script()
        String yaml = """
            metadata:
              name: "Script Integration Test"
              type: "rule-config"
              version: "1.0"

            runtime-scripts:
              enabled: true
              locations:
                - "%s"
              engine: "groovy"
              polling-interval-ms: 0
              execution-timeout-ms: 5000

            enrichments:
              - id: "compute-risk"
                name: "Compute Risk Level"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "#script('risk-score', #root)"
                  result-field: "riskLevel"
                field-mappings:
                  - source-field: "riskLevel"
                    target-field: "riskLevel"

            rules:
              - id: "high-risk-check"
                name: "High Risk Check"
                condition: "riskLevel == 'HIGH'"
                message: "Trade is high risk"
                severity: "WARNING"
            """.formatted(scriptsDir.toString().replace("\\", "/"));

        Path yamlFile = tempDir.resolve("config.yaml");
        Files.writeString(yamlFile, yaml);

        // Create engine and evaluate
        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 5000000);
            data.put("currency", "USD");

            RuleResult result = engine.evaluate(data);
            assertNotNull(result);

            // The enrichment should have set riskLevel to "HIGH"
            Map<String, Object> enriched = result.getEnrichedData();
            assertEquals("HIGH", enriched.get("riskLevel"),
                    "Script should classify notional > 1M as HIGH risk");

        } finally {
            engine.shutdown();
        }
    }

    @Test
    void engineWithoutScriptsStillWorksNormally() throws Exception {
        // Verify backward compatibility: engine without runtime-scripts config
        String yaml = """
            metadata:
              name: "No Scripts Test"
              type: "rule-config"
              version: "1.0"

            rules:
              - id: "simple-rule"
                name: "Simple Rule"
                condition: "amount > 100"
                message: "Amount exceeds threshold"
                severity: "INFO"
            """;

        Path yamlFile = tempDir.resolve("no-scripts.yaml");
        Files.writeString(yamlFile, yaml);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("amount", 200);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result);
            assertTrue(result.isSuccess(), "Simple rule should match");
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void scriptWithMediumRiskClassification() throws Exception {
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);

        Files.writeString(scriptsDir.resolve("risk-score.groovy"), """
            class RiskScore {
                def run(Map payload) {
                    def notional = payload.get('notional') ?: 0
                    if (notional > 1000000) return 'HIGH'
                    if (notional > 100000) return 'MEDIUM'
                    return 'LOW'
                }
            }
            """);

        String yaml = """
            metadata:
              name: "Medium Risk Test"
              type: "rule-config"
              version: "1.0"

            runtime-scripts:
              enabled: true
              locations:
                - "%s"
              polling-interval-ms: 0
              execution-timeout-ms: 5000

            enrichments:
              - id: "compute-risk"
                name: "Compute Risk Level"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "#script('risk-score', #root)"
                  result-field: "riskLevel"
                field-mappings:
                  - source-field: "riskLevel"
                    target-field: "riskLevel"

            rules:
              - id: "medium-risk-check"
                name: "Medium Risk Check"
                condition: "riskLevel == 'MEDIUM'"
                message: "Trade is medium risk"
                severity: "INFO"
            """.formatted(scriptsDir.toString().replace("\\", "/"));

        Path yamlFile = tempDir.resolve("medium-risk.yaml");
        Files.writeString(yamlFile, yaml);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("notional", 500000);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result);

            Map<String, Object> enriched = result.getEnrichedData();
            assertEquals("MEDIUM", enriched.get("riskLevel"));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void invalidScriptLocationDoesNotBreakValidLocation() throws Exception {
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);

        Files.writeString(scriptsDir.resolve("risk-score.groovy"), """
            class RiskScore {
                def run(Map payload) {
                    return 'HIGH'
                }
            }
            """);

        String yaml = """
            metadata:
              name: "Invalid Location Tolerance Test"
              type: "rule-config"
              version: "1.0"

            runtime-scripts:
              enabled: true
              locations:
                - "classpath:missing/scripts"
                - "%s"
              polling-interval-ms: 0
              execution-timeout-ms: 5000

            enrichments:
              - id: "compute-risk"
                name: "Compute Risk Level"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "#script('risk-score', #root)"
                  result-field: "riskLevel"
                field-mappings:
                  - source-field: "riskLevel"
                    target-field: "riskLevel"

            rules:
              - id: "high-risk-check"
                name: "High Risk Check"
                condition: "riskLevel == 'HIGH'"
                message: "Trade is high risk"
                severity: "WARNING"
            """.formatted(scriptsDir.toString().replace("\\", "/"));

        Path yamlFile = tempDir.resolve("invalid-location-tolerance.yaml");
        Files.writeString(yamlFile, yaml);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        try {
            RuleResult result = engine.evaluate(new HashMap<>());
            assertNotNull(result);
            assertEquals("HIGH", result.getEnrichedData().get("riskLevel"));
        } finally {
            engine.shutdown();
        }
    }
}
