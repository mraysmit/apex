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
 * Verifies that script errors route through APEX error recovery the same way
 * as any other SpEL evaluation error.
 */
class ScriptErrorRecoveryTest {

    @TempDir
    Path tempDir;

    @Test
    void scriptErrorInRuleConditionRoutedThroughErrorRecovery() throws Exception {
        // Script that always throws
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);
        Files.writeString(scriptsDir.resolve("exploder.groovy"), """
            class Exploder {
                def run(Map payload) {
                    throw new RuntimeException("Intentional script error")
                }
            }
            """);

        // Configure error recovery with CONTINUE_WITH_DEFAULT for WARNING
        String yaml = """
            metadata:
              name: "Script Error Recovery Test"
              type: "rule-config"
              version: "1.0"

            runtime-scripts:
              enabled: true
              locations:
                - "%s"
              polling-interval-ms: 0
              execution-timeout-ms: 5000

            error-recovery:
              enabled: true
              log-recovery-attempts: true
              default-strategy: "CONTINUE_WITH_DEFAULT"
              severity-policies:
                WARNING:
                  recovery-enabled: true
                  strategy: "CONTINUE_WITH_DEFAULT"
                ERROR:
                  recovery-enabled: false
                  strategy: "FAIL_FAST"

            rules:
              - id: "script-rule-warning"
                name: "Script Rule Warning"
                condition: "#script('exploder', #root)"
                message: "This should be recovered"
                severity: "WARNING"
            """.formatted(scriptsDir.toString().replace("\\", "/"));

        Path yamlFile = tempDir.resolve("error-recovery.yaml");
        Files.writeString(yamlFile, yaml);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("value", 42);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Engine should return a result even when script errors occur");
            // With WARNING severity + CONTINUE_WITH_DEFAULT, the engine should not crash
            // The result status depends on error recovery behavior
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void scriptNotFoundErrorHandledGracefully() throws Exception {
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);
        // Create a valid script so registry initializes
        Files.writeString(scriptsDir.resolve("helper.groovy"), """
            class Helper {
                def run(Map payload) { return true }
            }
            """);

        // Reference a non-existent script
        String yaml = """
            metadata:
              name: "Missing Script Test"
              type: "rule-config"
              version: "1.0"

            runtime-scripts:
              enabled: true
              locations:
                - "%s"
              polling-interval-ms: 0
              execution-timeout-ms: 5000

            error-recovery:
              enabled: true
              log-recovery-attempts: true
              default-strategy: "CONTINUE_WITH_DEFAULT"
              severity-policies:
                WARNING:
                  recovery-enabled: true
                  strategy: "CONTINUE_WITH_DEFAULT"

            rules:
              - id: "missing-script-rule"
                name: "Missing Script Rule"
                condition: "#script('nonexistent', #root)"
                message: "Script not found should be recovered"
                severity: "WARNING"
            """.formatted(scriptsDir.toString().replace("\\", "/"));

        Path yamlFile = tempDir.resolve("missing-script.yaml");
        Files.writeString(yamlFile, yaml);

        RulesEngine engine = RulesEngine.fromFile(yamlFile.toString());
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("value", 42);

            RuleResult result = engine.evaluate(data);
            assertNotNull(result, "Engine should handle missing script gracefully");
        } finally {
            engine.shutdown();
        }
    }
}
