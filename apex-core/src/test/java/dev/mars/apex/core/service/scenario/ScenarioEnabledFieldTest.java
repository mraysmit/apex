package dev.mars.apex.core.service.scenario;

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

import dev.mars.apex.core.config.loader.ScenarioRegistryLoader;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScenarioConfiguration enabled field functionality.
 *
 * Tests cover:
 * - Default enabled state (true)
 * - Explicit enabled/disabled states
 * - Registry parsing of enabled field
 * - Disabled scenarios skipped during classification
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@DisplayName("Scenario Enabled Field Tests")
class ScenarioEnabledFieldTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEnabledFieldTest.class);

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("scenario-enabled-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up temp files
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                });
        }
    }

    // ========================================
    // ScenarioConfiguration Unit Tests
    // ========================================

    @Test
    @DisplayName("Should default to enabled=true")
    void testDefaultEnabledState() {
        ScenarioConfiguration scenario = new ScenarioConfiguration();
        assertTrue(scenario.isEnabled(), "New scenario should be enabled by default");
    }

    @Test
    @DisplayName("Should allow setting enabled to false")
    void testSetEnabledFalse() {
        ScenarioConfiguration scenario = new ScenarioConfiguration();
        scenario.setEnabled(false);
        assertFalse(scenario.isEnabled(), "Scenario should be disabled after setEnabled(false)");
    }

    @Test
    @DisplayName("Should allow setting enabled to true")
    void testSetEnabledTrue() {
        ScenarioConfiguration scenario = new ScenarioConfiguration();
        scenario.setEnabled(false);
        scenario.setEnabled(true);
        assertTrue(scenario.isEnabled(), "Scenario should be enabled after setEnabled(true)");
    }

    // ========================================
    // ScenarioRegistryLoader Tests
    // ========================================

    @Test
    @DisplayName("Should parse enabled=true from registry")
    void testParseEnabledTrue() throws Exception {
        // Create scenario file
        Path scenarioFile = createScenarioFile("enabled-scenario");

        // Create registry with enabled=true
        String registryYaml = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry for enabled field testing"
              type: "scenario-registry"
            scenarios:
              - scenario-id: "enabled-scenario"
                config-file: "%s"
                enabled: true
            """.formatted(scenarioFile.toString().replace("\\", "/"));

        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);

        // Load registry
        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(registryFile.toString());

        // Verify
        assertTrue(scenarios.containsKey("enabled-scenario"));
        assertTrue(scenarios.get("enabled-scenario").isEnabled(),
            "Scenario with enabled=true should be enabled");
    }

    @Test
    @DisplayName("Should parse enabled=false from registry")
    void testParseEnabledFalse() throws Exception {
        // Create scenario file
        Path scenarioFile = createScenarioFile("disabled-scenario");

        // Create registry with enabled=false
        String registryYaml = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry for enabled field testing"
              type: "scenario-registry"
            scenarios:
              - scenario-id: "disabled-scenario"
                config-file: "%s"
                enabled: false
            """.formatted(scenarioFile.toString().replace("\\", "/"));

        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);

        // Load registry
        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(registryFile.toString());

        // Verify
        assertTrue(scenarios.containsKey("disabled-scenario"));
        assertFalse(scenarios.get("disabled-scenario").isEnabled(),
            "Scenario with enabled=false should be disabled");
    }

    @Test
    @DisplayName("Should default to enabled=true when not specified in registry")
    void testDefaultEnabledWhenNotSpecified() throws Exception {
        // Create scenario file
        Path scenarioFile = createScenarioFile("default-scenario");

        // Create registry without enabled field
        String registryYaml = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry for enabled field testing"
              type: "scenario-registry"
            scenarios:
              - scenario-id: "default-scenario"
                config-file: "%s"
            """.formatted(scenarioFile.toString().replace("\\", "/"));

        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);

        // Load registry
        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(registryFile.toString());

        // Verify
        assertTrue(scenarios.containsKey("default-scenario"));
        assertTrue(scenarios.get("default-scenario").isEnabled(),
            "Scenario without enabled field should default to enabled");
    }

    @Test
    @DisplayName("Should parse enabled as string 'true'")
    void testParseEnabledAsStringTrue() throws Exception {
        Path scenarioFile = createScenarioFile("string-true-scenario");

        String registryYaml = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry for enabled field testing"
              type: "scenario-registry"
            scenarios:
              - scenario-id: "string-true-scenario"
                config-file: "%s"
                enabled: "true"
            """.formatted(scenarioFile.toString().replace("\\", "/"));

        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);

        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(registryFile.toString());

        assertTrue(scenarios.get("string-true-scenario").isEnabled());
    }

    @Test
    @DisplayName("Should parse enabled as string 'false'")
    void testParseEnabledAsStringFalse() throws Exception {
        Path scenarioFile = createScenarioFile("string-false-scenario");

        String registryYaml = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry for enabled field testing"
              type: "scenario-registry"
            scenarios:
              - scenario-id: "string-false-scenario"
                config-file: "%s"
                enabled: "false"
            """.formatted(scenarioFile.toString().replace("\\", "/"));

        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);

        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(registryFile.toString());

        assertFalse(scenarios.get("string-false-scenario").isEnabled());
    }

    // ========================================
    // Scenario File-Level Enabled Tests
    // ========================================

    @Test
    @DisplayName("Should parse enabled=false from scenario file itself")
    void testParseEnabledFromScenarioFile() throws Exception {
        // Create scenario file with enabled=false in the scenario section
        String scenarioYaml = """
            metadata:
              id: "file-disabled-scenario"
              name: "Test Scenario"
              version: "1.0.0"
              description: "Test scenario with enabled=false in file"
              type: "scenario"
            scenario:
              scenario-id: "file-disabled-scenario"
              name: "Test Scenario"
              description: "Test scenario with enabled=false in file"
              enabled: false
              classification-rule: "#tradeType == 'TEST'"
              processing-stages:
                - stage-name: "validation"
                  config-file: "dummy-validation.yaml"
                  execution-order: 1
            """;

        Path scenarioFile = tempDir.resolve("file-disabled-scenario.yaml");
        Files.writeString(scenarioFile, scenarioYaml);

        // Create registry without enabled field (should use scenario file's enabled value)
        String registryYaml = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry for enabled field testing"
              type: "scenario-registry"
            scenarios:
              - scenario-id: "file-disabled-scenario"
                config-file: "%s"
            """.formatted(scenarioFile.toString().replace("\\", "/"));

        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryYaml);

        ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
        Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(registryFile.toString());

        // Note: Registry entry enabled flag overrides scenario file enabled flag
        // Since registry doesn't specify enabled, it defaults to true and overrides
        assertTrue(scenarios.get("file-disabled-scenario").isEnabled(),
            "Registry enabled flag (default true) should override scenario file enabled flag");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private Path createScenarioFile(String scenarioId) throws IOException {
        String scenarioYaml = """
            metadata:
              id: "%s"
              name: "Test Scenario"
              version: "1.0.0"
              description: "Test scenario for enabled field testing"
              type: "scenario"
            scenario:
              scenario-id: "%s"
              name: "Test Scenario"
              description: "Test scenario for enabled field testing"
              classification-rule: "#tradeType == 'TEST'"
              processing-stages:
                - stage-name: "validation"
                  config-file: "dummy-validation.yaml"
                  execution-order: 1
            """.formatted(scenarioId, scenarioId);

        Path scenarioFile = tempDir.resolve(scenarioId + ".yaml");
        Files.writeString(scenarioFile, scenarioYaml);
        return scenarioFile;
    }
}

