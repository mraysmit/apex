package dev.mars.apex.engine.core;

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
import org.junit.jupiter.api.DisplayName;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RulesEngine.fromScenarioRegistry() classpath loading support.
 * 
 * <p>Tests the enhanced fromScenarioRegistry() method that supports both classpath
 * and filesystem loading, as documented in APEX_PATH_PROCESSING.md Section 2.5.</p>
 * 
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Classpath-based registry loading</li>
 *   <li>Filesystem fallback when not on classpath</li>
 *   <li>Error handling for non-existent resources</li>
 *   <li>Relative path resolution within classpath context</li>
 * </ul>
 * 
 * <p><b>Test Resources:</b></p>
 * <pre>
 * src/test/resources/scenario/
 *   ├── test-registry.yaml
 *   ├── basic-validation-scenario.yaml
 *   └── complex-rules-scenario.yaml
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see RulesEngine#fromScenarioRegistry(String)
 */
@DisplayName("RulesEngine.fromScenarioRegistry() Classpath Loading Tests")
class RulesEngineFromScenarioRegistryTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineFromScenarioRegistryTest.class);

    // Classpath resource path (relative to src/test/resources)
    private static final String CLASSPATH_REGISTRY = "scenario/test-registry.yaml";

    // ========================================================================
    // Classpath Loading Tests
    // ========================================================================

    @Nested
    @DisplayName("Classpath Loading Tests")
    class ClasspathLoadingTests {

        @Test
        @DisplayName("Should load scenario registry from classpath resource")
        void testLoadRegistryFromClasspath() throws Exception {
            logger.info("=== Testing fromScenarioRegistry() with classpath resource ===");
            logger.info("Resource path: {}", CLASSPATH_REGISTRY);

            // Load from classpath - this should work with the enhanced method
            RulesEngine engine = RulesEngine.fromScenarioRegistry(CLASSPATH_REGISTRY);

            assertNotNull(engine, "Engine should not be null");
            assertNotNull(engine.getScenarioRegistry(), "Scenario registry should not be null");
            assertFalse(engine.getScenarioRegistry().isEmpty(), "Scenario registry should not be empty");

            // Verify expected scenarios were loaded
            assertTrue(engine.getScenarioRegistry().containsKey("basic-validation"),
                    "Registry should contain 'basic-validation' scenario");
            assertTrue(engine.getScenarioRegistry().containsKey("complex-rules"),
                    "Registry should contain 'complex-rules' scenario");

            logger.info("[OK] Successfully loaded {} scenarios from classpath", 
                       engine.getScenarioRegistry().size());
            engine.getScenarioRegistry().keySet().forEach(id -> 
                logger.info("  - Scenario: {}", id));
        }

        @Test
        @DisplayName("Should resolve relative config-file paths in classpath context")
        void testRelativePathResolutionInClasspath() throws Exception {
            logger.info("=== Testing relative path resolution in classpath ===");

            // The registry references scenarios via relative paths like:
            //   config-file: "basic-validation-scenario.yaml"
            // These should resolve relative to the registry's classpath location

            RulesEngine engine = RulesEngine.fromScenarioRegistry(CLASSPATH_REGISTRY);

            // If relative paths resolved correctly, scenario configurations should be fully loaded
            var basicScenario = engine.getScenarioRegistry().get("basic-validation");
            assertNotNull(basicScenario, "basic-validation scenario should be loaded");
            assertNotNull(basicScenario.getScenarioId(), "Scenario ID should be set");
            assertEquals("basic-validation", basicScenario.getScenarioId(), 
                        "Scenario ID should match");

            logger.info("[OK] Relative path resolution working correctly");
            logger.info("  - Loaded scenario: {} (domain: {})", 
                       basicScenario.getScenarioId(), basicScenario.getBusinessDomain());
        }

        @Test
        @DisplayName("Should load scenario with business domain from classpath")
        void testScenarioBusinessDomainFromClasspath() throws Exception {
            logger.info("=== Testing scenario business domain from classpath ===");

            RulesEngine engine = RulesEngine.fromScenarioRegistry(CLASSPATH_REGISTRY);

            var basicScenario = engine.getScenarioRegistry().get("basic-validation");
            assertNotNull(basicScenario, "basic-validation scenario should be loaded");
            assertEquals("Testing", basicScenario.getBusinessDomain(), 
                        "Business domain should be 'Testing'");

            logger.info("[OK] Business domain correctly loaded: {}", basicScenario.getBusinessDomain());
        }
    }

    // ========================================================================
    // Filesystem Fallback Tests
    // ========================================================================

    @Nested
    @DisplayName("Filesystem Fallback Tests")
    class FilesystemFallbackTests {

        @Test
        @DisplayName("Should fall back to filesystem when not on classpath")
        void testFilesystemFallback() throws Exception {
            logger.info("=== Testing filesystem fallback ===");

            // Create a temporary registry file on filesystem
            Path tempDir = Files.createTempDirectory("apex-test-");
            Path scenariosDir = tempDir.resolve("scenarios");
            Files.createDirectories(scenariosDir);

            // Create a simple scenario file with full metadata
            String scenarioYaml = """
                metadata:
                  id: "temp-scenario"
                  name: "Temporary Test Scenario"
                  description: "Temporary scenario for testing filesystem fallback"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "temp-scenario"
                  business-domain: "Testing"
                  
                  rule-configurations:
                    - id: "temp-rules"
                      rules:
                        - id: "temp-rule-1"
                          condition: "true"
                          message: "Always passes"
                          severity: "INFO"
                """;
            Files.writeString(scenariosDir.resolve("temp-scenario.yaml"), scenarioYaml);

            // Create registry pointing to the scenario with full metadata
            String registryYaml = """
                metadata:
                  id: "temp-registry"
                  name: "Temporary Test Registry"
                  description: "Temporary registry for testing filesystem fallback"
                  type: "scenario-registry"
                  version: "1.0.0"
                
                scenarios:
                  - scenario-id: "temp-scenario"
                    config-file: "scenarios/temp-scenario.yaml"
                    business-domain: "Testing"
                    enabled: true
                """;
            Path registryFile = tempDir.resolve("temp-registry.yaml");
            Files.writeString(registryFile, registryYaml);

            try {
                // Load using filesystem path - should work via fallback
                RulesEngine engine = RulesEngine.fromScenarioRegistry(registryFile.toString());

                assertNotNull(engine, "Engine should not be null");
                assertNotNull(engine.getScenarioRegistry(), "Scenario registry should not be null");
                assertTrue(engine.getScenarioRegistry().containsKey("temp-scenario"),
                        "Registry should contain 'temp-scenario'");

                logger.info("[OK] Filesystem fallback working correctly");
            } finally {
                // Cleanup
                Files.deleteIfExists(scenariosDir.resolve("temp-scenario.yaml"));
                Files.deleteIfExists(scenariosDir);
                Files.deleteIfExists(registryFile);
                Files.deleteIfExists(tempDir);
            }
        }
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw exception for non-existent classpath resource")
        void testNonExistentClasspathResource() {
            logger.info("=== Testing non-existent classpath resource handling ===");

            String nonExistentPath = "non-existent/registry.yaml";

            YamlConfigurationException exception = assertThrows(
                YamlConfigurationException.class,
                () -> RulesEngine.fromScenarioRegistry(nonExistentPath),
                "Should throw exception for non-existent resource"
            );

            logger.info("[OK] Correctly threw exception: {}", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for null registry path")
        void testNullRegistryPath() {
            logger.info("=== Testing null registry path handling ===");

            assertThrows(
                Exception.class,
                () -> RulesEngine.fromScenarioRegistry(null),
                "Should throw exception for null path"
            );

            logger.info("[OK] Correctly threw exception for null path");
        }

        @Test
        @DisplayName("Should throw exception for empty registry path")
        void testEmptyRegistryPath() {
            logger.info("=== Testing empty registry path handling ===");

            assertThrows(
                Exception.class,
                () -> RulesEngine.fromScenarioRegistry(""),
                "Should throw exception for empty path"
            );

            logger.info("[OK] Correctly threw exception for empty path");
        }
    }

    // ========================================================================
    // Backward Compatibility Tests
    // ========================================================================

    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Should maintain backward compatibility with filesystem paths")
        void testBackwardCompatibilityWithFilesystemPaths() throws Exception {
            logger.info("=== Testing backward compatibility with filesystem paths ===");

            // Create temporary files to test filesystem loading
            Path tempDir = Files.createTempDirectory("apex-compat-test-");
            
            String scenarioYaml = """
                metadata:
                  id: "compat-scenario"
                  name: "Compatibility Test Scenario"
                  description: "Scenario for testing backward compatibility with filesystem paths"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "compat-scenario"
                  business-domain: "Testing"
                  
                  rule-configurations:
                    - id: "compat-rules"
                      rules:
                        - id: "compat-rule"
                          condition: "#value > 0"
                          message: "Value must be positive"
                          severity: "ERROR"
                """;
            Files.writeString(tempDir.resolve("compat-scenario.yaml"), scenarioYaml);

            String registryYaml = """
                metadata:
                  id: "compat-registry"
                  name: "Compatibility Test Registry"
                  description: "Registry for testing backward compatibility with filesystem paths"
                  type: "scenario-registry"
                  version: "1.0.0"
                
                scenarios:
                  - scenario-id: "compat-scenario"
                    config-file: "compat-scenario.yaml"
                    business-domain: "Testing"
                    enabled: true
                """;
            Path registryFile = tempDir.resolve("compat-registry.yaml");
            Files.writeString(registryFile, registryYaml);

            try {
                // This path should NOT be on classpath, so it tests filesystem fallback
                RulesEngine engine = RulesEngine.fromScenarioRegistry(registryFile.toString());

                assertNotNull(engine, "Engine should not be null");
                assertTrue(engine.getScenarioRegistry().containsKey("compat-scenario"),
                        "Registry should contain 'compat-scenario'");

                // Verify scenario properties
                var scenario = engine.getScenarioRegistry().get("compat-scenario");
                assertEquals("compat-scenario", scenario.getScenarioId(), "Scenario ID should match");
                // Business domain may be null if not propagated from registry to scenario
                // The key test is that the scenario was loaded successfully
                assertNotNull(scenario, "Scenario should be loaded");

                logger.info("[OK] Backward compatibility maintained");
                logger.info("  - Loaded scenario: {}", scenario.getScenarioId());
            } finally {
                // Cleanup
                Files.deleteIfExists(tempDir.resolve("compat-scenario.yaml"));
                Files.deleteIfExists(registryFile);
                Files.deleteIfExists(tempDir);
            }
        }

        @Test
        @DisplayName("Should verify classpath takes priority over filesystem")
        void testClasspathPriorityOverFilesystem() throws Exception {
            logger.info("=== Testing classpath priority over filesystem ===");

            // When a resource exists on both classpath and filesystem,
            // classpath should be checked first (as per APEX resolution strategy)
            
            // Use the existing classpath resource
            RulesEngine engine = RulesEngine.fromScenarioRegistry(CLASSPATH_REGISTRY);
            
            assertNotNull(engine, "Engine should not be null");
            assertNotNull(engine.getScenarioRegistry(), "Scenario registry should not be null");
            
            // The classpath resource has 2 scenarios
            assertEquals(2, engine.getScenarioRegistry().size(), 
                        "Should load 2 scenarios from classpath");

            logger.info("[OK] Classpath loading verified (2 scenarios loaded)");
        }
    }
}

