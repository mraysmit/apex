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

import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.junit.jupiter.api.*;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for RulesEngine scenario registry functionality.
 * 
 * <p>Replaces the deleted DataTypeScenarioServiceTest, using the new RulesEngine API.</p>
 * 
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Scenario registry loading via RulesEngine.fromScenarioRegistry()</li>
 *   <li>Scenario retrieval from registry</li>
 *   <li>Error handling for missing/invalid registry files</li>
 *   <li>Registry configuration parsing</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see RulesEngine#fromScenarioRegistry(String)
 */
@DisplayName("RulesEngine Scenario Registry Tests")
class RulesEngineScenarioRegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineScenarioRegistryTest.class);

    @TempDir
    Path tempDir;

    // ========================================
    // Factory Method Tests
    // ========================================

    @Test
    @DisplayName("Should create RulesEngine from scenario registry")
    void testFromScenarioRegistry() throws Exception {
        // Create test registry and scenario files
        String registryPath = createTestRegistryWithScenario();

        // Create RulesEngine from registry
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        assertNotNull(engine, "Engine should be created successfully");
        assertNotNull(engine.getScenarioRegistry(), "Scenario registry should not be null");
        assertFalse(engine.getScenarioRegistry().isEmpty(), "Scenario registry should not be empty");
    }

    @Test
    @DisplayName("Should throw exception for missing registry file")
    void testFromScenarioRegistryWithMissingFile() {
        logger.info("TEST: Triggering intentional error - testing scenario loading with missing file");
        
        assertThrows(YamlConfigurationException.class, () -> {
            RulesEngine.fromScenarioRegistry("nonexistent/path/registry.yaml");
        }, "Missing registry file should throw YamlConfigurationException");
    }

    @Test
    @DisplayName("Should throw exception for null registry path")
    void testFromScenarioRegistryWithNullPath() {
        logger.info("TEST: Testing scenario registry with null path");

        assertThrows(NullPointerException.class, () -> {
            RulesEngine.fromScenarioRegistry(null);
        }, "Null registry path should throw NullPointerException");
    }

    @Test
    @DisplayName("Should throw exception for empty registry path")
    void testFromScenarioRegistryWithEmptyPath() {
        logger.info("TEST: Testing scenario registry with empty path");

        assertThrows(YamlConfigurationException.class, () -> {
            RulesEngine.fromScenarioRegistry("");
        }, "Empty registry path should throw YamlConfigurationException");
    }

    // ========================================
    // Scenario Registry Access Tests
    // ========================================

    @Test
    @DisplayName("Should retrieve scenario by ID from registry")
    void testGetScenarioById() throws Exception {
        String registryPath = createTestRegistryWithScenario();
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        Map<String, ScenarioConfiguration> registry = engine.getScenarioRegistry();
        ScenarioConfiguration scenario = registry.get("test-scenario");
        
        assertNotNull(scenario, "Should retrieve scenario by ID");
        assertEquals("test-scenario", scenario.getScenarioId(), "Should return correct scenario");
    }

    @Test
    @DisplayName("Should return null for non-existent scenario ID")
    void testGetScenarioByNonExistentId() throws Exception {
        String registryPath = createTestRegistryWithScenario();
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        Map<String, ScenarioConfiguration> registry = engine.getScenarioRegistry();
        ScenarioConfiguration scenario = registry.get("non-existent");
        
        assertNull(scenario, "Should return null for non-existent scenario ID");
    }

    @Test
    @DisplayName("Should return all scenario IDs from registry")
    void testGetAvailableScenarios() throws Exception {
        String registryPath = createTestRegistryWithMultipleScenarios();
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        Set<String> scenarioIds = engine.getScenarioRegistry().keySet();
        
        assertFalse(scenarioIds.isEmpty(), "Should have available scenarios");
        assertTrue(scenarioIds.contains("test-scenario-1"), "Should contain test-scenario-1");
        assertTrue(scenarioIds.contains("test-scenario-2"), "Should contain test-scenario-2");
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle invalid YAML syntax in registry")
    void testLoadScenariosWithInvalidSyntax() throws IOException {
        logger.info("TEST: Triggering intentional error - testing scenario loading with invalid YAML syntax");
        
        String invalidRegistryPath = createInvalidRegistryFile();
        
        assertThrows(YamlConfigurationException.class, () -> {
            RulesEngine.fromScenarioRegistry(invalidRegistryPath);
        }, "Invalid YAML syntax should throw YamlConfigurationException");
    }

    @Test
    @DisplayName("Should handle registry with missing scenario files gracefully")
    void testScenarioLoadingWithMissingScenarioFile() throws Exception {
        logger.info("TEST: Triggering intentional error - testing scenario loading with missing scenario file");

        String registryPath = createRegistryWithMissingScenarioFile();

        // May throw exception or handle gracefully depending on implementation
        assertThrows(YamlConfigurationException.class, () -> {
            RulesEngine.fromScenarioRegistry(registryPath);
        }, "Registry with missing scenario files should throw exception");
    }

    // ========================================
    // Classpath Loading Tests
    // ========================================

    @Test
    @DisplayName("Should load registry from classpath")
    void testLoadRegistryFromClasspath() throws Exception {
        // Use the test registry that exists in test resources
        String classpathRegistry = "scenario/test-registry.yaml";
        
        RulesEngine engine = RulesEngine.fromScenarioRegistry(classpathRegistry);
        
        assertNotNull(engine, "Engine should be created from classpath registry");
        assertNotNull(engine.getScenarioRegistry(), "Scenario registry should not be null");
    }

    // ========================================
    // evaluateScenario() Method Tests
    // ========================================

    @Test
    @DisplayName("Should evaluate scenario by ID with input data")
    void testEvaluateScenarioById() throws Exception {
        String registryPath = createTestRegistryWithScenario();
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("testField", "testValue");
        
        var result = engine.evaluateScenario("test-scenario", inputData);
        
        assertNotNull(result, "Result should not be null");
        assertEquals("test-scenario", result.getScenarioId(), "Should execute correct scenario");
    }

    @Test
    @DisplayName("Should throw exception when evaluating non-existent scenario")
    void testEvaluateNonExistentScenario() throws Exception {
        String registryPath = createTestRegistryWithScenario();
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("testField", "testValue");
        
        assertThrows(IllegalArgumentException.class, () -> {
            engine.evaluateScenario("non-existent", inputData);
        }, "Non-existent scenario should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should throw exception when evaluating with null scenario ID")
    void testEvaluateScenarioWithNullId() throws Exception {
        String registryPath = createTestRegistryWithScenario();
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        Map<String, Object> inputData = new HashMap<>();
        
        assertThrows(NullPointerException.class, () -> {
            engine.evaluateScenario(null, inputData);
        }, "Null scenario ID should throw NullPointerException");
    }

    @Test
    @DisplayName("Should throw exception when evaluating with null input data")
    void testEvaluateScenarioWithNullData() throws Exception {
        String registryPath = createTestRegistryWithScenario();
        RulesEngine engine = RulesEngine.fromScenarioRegistry(registryPath);
        
        assertThrows(NullPointerException.class, () -> {
            engine.evaluateScenario("test-scenario", null);
        }, "Null input data should throw NullPointerException");
    }

    // ========================================
    // Test Helper Methods
    // ========================================

    /**
     * Creates a test registry configuration file with a single scenario.
     */
    private String createTestRegistryWithScenario() throws IOException {
        Path scenariosDir = tempDir.resolve("scenarios");
        Files.createDirectories(scenariosDir);
        
        // Create a validation rules file for the stage
        String validationRulesContent = """
            metadata:
              id: "test-validation-rules"
              name: "Test Validation Rules"
              type: "rule-config"
              version: "1.0.0"
              category: "VALIDATION"
            
            rules:
              - id: "test-rule-1"
                condition: "true"
                message: "Always passes"
                severity: "INFO"
            """;
        Path validationFile = scenariosDir.resolve("test-validation-rules.yaml");
        Files.writeString(validationFile, validationRulesContent);
        
        // Create the scenario file with processing stages
        String scenarioContent = """
            metadata:
              id: "test-scenario"
              name: "Test Scenario"
              description: "Test scenario for unit testing"
              type: "scenario"
              version: "1.0.0"
            
            scenario:
              scenario-id: "test-scenario"
              business-domain: "Testing"
              
              processing-stages:
                - stage-name: "validation"
                  config-file: "%s"
                  execution-order: 1
                  failure-policy: "terminate"
                  required: true
            """.formatted(validationFile.toString().replace("\\", "/"));
        
        Path scenarioFile = scenariosDir.resolve("test-scenario.yaml");
        Files.writeString(scenarioFile, scenarioContent);

        // Then create the registry file
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Scenario Registry"
              version: "1.0.0"
              description: "Test registry for scenario loading"
              type: "scenario-registry"
            
            scenarios:
              - scenario-id: "test-scenario"
                config-file: "%s"
            """.formatted(scenarioFile.toString().replace("\\", "/"));
        
        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryContent);
        return registryFile.toString();
    }

    /**
     * Creates a test registry with multiple scenarios.
     */
    private String createTestRegistryWithMultipleScenarios() throws IOException {
        Path scenariosDir = tempDir.resolve("scenarios");
        Files.createDirectories(scenariosDir);
        
        // Create shared validation rules file
        String validationRulesContent = """
            metadata:
              id: "test-validation-rules"
              name: "Test Validation Rules"
              type: "rule-config"
              version: "1.0.0"
              category: "VALIDATION"
            
            rules:
              - id: "test-rule-1"
                condition: "true"
                message: "Always passes"
                severity: "INFO"
            """;
        Path validationFile = scenariosDir.resolve("test-validation-rules.yaml");
        Files.writeString(validationFile, validationRulesContent);
        
        // Create scenario files with processing stages
        for (int i = 1; i <= 2; i++) {
            String scenarioContent = """
                metadata:
                  id: "test-scenario-%d"
                  name: "Test Scenario %d"
                  description: "Test scenario %d for unit testing"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "test-scenario-%d"
                  business-domain: "Testing"
                  
                  processing-stages:
                    - stage-name: "validation"
                      config-file: "%s"
                      execution-order: 1
                      failure-policy: "terminate"
                      required: true
                """.formatted(i, i, i, i, validationFile.toString().replace("\\", "/"));
            
            Files.writeString(scenariosDir.resolve("test-scenario-" + i + ".yaml"), scenarioContent);
        }

        // Create registry file
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Scenario Registry"
              version: "1.0.0"
              description: "Test registry with multiple scenarios"
              type: "scenario-registry"
            
            scenarios:
              - scenario-id: "test-scenario-1"
                config-file: "%s/test-scenario-1.yaml"
              - scenario-id: "test-scenario-2"
                config-file: "%s/test-scenario-2.yaml"
            """.formatted(
                scenariosDir.toString().replace("\\", "/"),
                scenariosDir.toString().replace("\\", "/")
            );
        
        Path registryFile = tempDir.resolve("registry.yaml");
        Files.writeString(registryFile, registryContent);
        return registryFile.toString();
    }

    /**
     * Creates an invalid registry configuration file.
     */
    private String createInvalidRegistryFile() throws IOException {
        String invalidContent = """
            invalid: yaml: syntax:
              - missing
                - bracket
            unclosed: [
            """;
        
        Path invalidFile = tempDir.resolve("invalid-registry.yaml");
        Files.writeString(invalidFile, invalidContent);
        return invalidFile.toString();
    }

    /**
     * Creates a registry that references a non-existent scenario file.
     */
    private String createRegistryWithMissingScenarioFile() throws IOException {
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Registry with missing scenario files"
              type: "scenario-registry"
            
            scenarios:
              - scenario-id: "missing-scenario"
                config-file: "nonexistent/scenario.yaml"
            """;
        
        Path registryFile = tempDir.resolve("registry-missing-scenario.yaml");
        Files.writeString(registryFile, registryContent);
        return registryFile.toString();
    }
}
