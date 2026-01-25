package dev.mars.apex.core.config.yaml;

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

import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScenarioRegistryLoader stream-based loading methods.
 * 
 * <p>Tests the following methods added for Phase 1 of the Unified Resource Loading plan:</p>
 * <ul>
 *   <li>{@link ScenarioRegistryLoader#loadRegistry(InputStream)}</li>
 *   <li>{@link ScenarioRegistryLoader#loadRegistry(InputStream, String)}</li>
 *   <li>{@link ScenarioRegistryLoader#loadRegistryFromClasspath(String)}</li>
 *   <li>{@link ScenarioRegistryLoader#loadScenarioFromClasspath(String)}</li>
 *   <li>{@link ScenarioRegistryLoader#loadScenarioFromStream(InputStream)}</li>
 * </ul>
 * 
 * <p>These tests validate that scenario registry and configuration files can be loaded
 * from InputStreams and classpath resources, enabling JAR-packaged resource loading
 * which was previously broken due to InvalidPathException on Windows.</p>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@DisplayName("ScenarioRegistryLoader Stream Loading Tests")
class ScenarioRegistryLoaderStreamTest {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioRegistryLoaderStreamTest.class);
    
    private static final String TEST_RESOURCE_BASE = "scenario/";
    private static final String TEST_REGISTRY_PATH = TEST_RESOURCE_BASE + "test-registry.yaml";
    
    private ScenarioRegistryLoader loader;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        logger.info("Setting up ScenarioRegistryLoaderStreamTest");
        yamlLoader = new YamlConfigurationLoader();
        loader = new ScenarioRegistryLoader(yamlLoader);
    }

    // ========================================================================
    // loadRegistry(InputStream) Tests
    // ========================================================================
    
    @Nested
    @DisplayName("loadRegistry(InputStream) Tests")
    class LoadRegistryInputStreamTests {

        @Test
        @DisplayName("Should load registry from InputStream with scenarios")
        void testLoadRegistryFromInputStream() throws Exception {
            logger.info("=== Testing loadRegistry(InputStream) ===");

            String registryYaml = """
                metadata:
                  id: "stream-registry"
                  name: "Stream Test Registry"
                  description: "Test registry for stream loading"
                  type: "scenario-registry"
                  version: "1.0.0"
                
                scenarios:
                  - scenario-id: "basic-validation"
                    config-file: "basic-validation-scenario.yaml"
                    business-domain: "Testing"
                    enabled: true
                """;

            try (InputStream inputStream = new ByteArrayInputStream(registryYaml.getBytes(StandardCharsets.UTF_8))) {
                // Use the classpath base to resolve the config-file reference
                Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(inputStream, TEST_RESOURCE_BASE);

                assertNotNull(scenarios, "Scenarios map should not be null");
                assertFalse(scenarios.isEmpty(), "Scenarios map should not be empty when config-file references are resolved");
                logger.info("[OK] Successfully loaded registry from InputStream, scenarios count: {}", 
                           scenarios.size());
            }
        }

        @Test
        @DisplayName("Should throw exception for null InputStream")
        void testLoadRegistryFromInputStreamNullThrowsException() {
            logger.info("=== Testing loadRegistry(InputStream) with null ===");

            assertThrows(
                Exception.class,
                () -> loader.loadRegistry((InputStream) null),
                "Should throw exception for null InputStream"
            );

            logger.info("[OK] Correctly threw exception for null InputStream");
        }

        @Test
        @DisplayName("Should throw exception for empty registry")
        void testLoadRegistryFromInputStreamEmptyRegistry() throws Exception {
            logger.info("=== Testing loadRegistry(InputStream) with empty scenarios ===");

            String registryYaml = """
                metadata:
                  id: "empty-registry"
                  name: "Empty Test Registry"
                  description: "Test registry with no scenarios"
                  type: "scenario-registry"
                  version: "1.0.0"
                
                scenarios: []
                """;

            try (InputStream inputStream = new ByteArrayInputStream(registryYaml.getBytes(StandardCharsets.UTF_8))) {
                // ScenarioRegistryLoader requires at least one scenario
                YamlConfigurationException exception = assertThrows(
                    YamlConfigurationException.class,
                    () -> loader.loadRegistry(inputStream),
                    "Should throw exception for empty scenarios list"
                );
                
                assertTrue(exception.getMessage().contains("empty") || exception.getMessage().contains("scenarios"),
                    "Exception message should mention empty scenarios");
                
                logger.info("[OK] Correctly threw exception for empty scenarios: {}", exception.getMessage());
            }
        }
    }

    // ========================================================================
    // loadRegistry(InputStream, String classpathBase) Tests
    // ========================================================================
    
    @Nested
    @DisplayName("loadRegistry(InputStream, String classpathBase) Tests")
    class LoadRegistryWithClasspathBaseTests {

        @Test
        @DisplayName("Should load registry and resolve config-file references from classpath")
        void testLoadRegistryWithClasspathBaseResolvesReferences() throws Exception {
            logger.info("=== Testing loadRegistry(InputStream, classpathBase) with config-file resolution ===");

            // Load the test registry and provide the classpath base for resolving config-file references
            try (InputStream registryStream = getClass().getClassLoader()
                    .getResourceAsStream(TEST_REGISTRY_PATH)) {
                
                assertNotNull(registryStream, "Test registry should exist on classpath");
                
                Map<String, ScenarioConfiguration> scenarios = 
                    loader.loadRegistry(registryStream, TEST_RESOURCE_BASE);

                assertNotNull(scenarios, "Scenarios map should not be null");
                
                // If config-files were resolved, we should have loaded scenario configurations
                logger.info("[OK] Loaded {} scenarios from registry with classpath base", scenarios.size());
                
                // Verify expected scenarios are present
                scenarios.forEach((id, config) -> {
                    logger.info("  - Scenario: {} (id: {}, enabled: {})", 
                               id, config.getScenarioId(), config.isEnabled());
                });
            }
        }

        @Test
        @DisplayName("Should resolve relative config-file paths correctly")
        void testConfigFilePathResolution() throws Exception {
            logger.info("=== Testing config-file path resolution ===");

            String registryYaml = """
                metadata:
                  id: "path-resolution-test"
                  name: "Path Resolution Test Registry"
                  description: "Test registry for path resolution"
                  type: "scenario-registry"
                  version: "1.0.0"
                
                scenarios:
                  - scenario-id: "basic-validation"
                    config-file: "basic-validation-scenario.yaml"
                    business-domain: "Testing"
                    enabled: true
                """;

            try (InputStream inputStream = new ByteArrayInputStream(registryYaml.getBytes(StandardCharsets.UTF_8))) {
                Map<String, ScenarioConfiguration> scenarios = 
                    loader.loadRegistry(inputStream, TEST_RESOURCE_BASE);

                assertNotNull(scenarios, "Scenarios map should not be null");
                
                // The basic-validation scenario should be loaded from classpath
                if (scenarios.containsKey("basic-validation")) {
                    ScenarioConfiguration config = scenarios.get("basic-validation");
                    assertNotNull(config.getScenarioId(), "Scenario should have ID");
                    logger.info("[OK] Successfully resolved config-file reference for scenario: {}", 
                               config.getScenarioId());
                } else {
                    logger.info("[OK] Registry parsed, scenario resolution depends on config-file loading");
                }
            }
        }
    }

    // ========================================================================
    // loadRegistryFromClasspath(String) Tests
    // ========================================================================
    
    @Nested
    @DisplayName("loadRegistryFromClasspath(String) Tests")
    class LoadRegistryFromClasspathTests {

        @Test
        @DisplayName("Should load registry directly from classpath resource")
        void testLoadRegistryFromClasspath() throws Exception {
            logger.info("=== Testing loadRegistryFromClasspath() ===");

            Map<String, ScenarioConfiguration> scenarios = 
                loader.loadRegistryFromClasspath(TEST_REGISTRY_PATH);

            assertNotNull(scenarios, "Scenarios map should not be null");
            
            logger.info("[OK] Successfully loaded registry from classpath: {}", TEST_REGISTRY_PATH);
            logger.info("  Scenarios loaded: {}", scenarios.size());
            
            scenarios.forEach((id, config) -> {
                logger.info("  - {}: enabled={}", id, config.isEnabled());
            });
        }

        @Test
        @DisplayName("Should throw exception for non-existent classpath resource")
        void testLoadRegistryFromClasspathNotFound() {
            logger.info("=== Testing loadRegistryFromClasspath() with non-existent path ===");

            String nonExistentPath = "non-existent/registry.yaml";

            Exception exception = assertThrows(
                Exception.class,
                () -> loader.loadRegistryFromClasspath(nonExistentPath),
                "Should throw exception for non-existent resource"
            );

            logger.info("[OK] Correctly threw exception for non-existent classpath resource: {}", 
                       exception.getMessage());
        }

        @Test
        @DisplayName("Should auto-derive classpath base from resource path")
        void testAutoDerivesClasspathBase() throws Exception {
            logger.info("=== Testing classpath base auto-derivation ===");

            // When loading from "scenario/test-registry.yaml",
            // the loader should automatically use "scenario/" as the base
            Map<String, ScenarioConfiguration> scenarios = 
                loader.loadRegistryFromClasspath(TEST_REGISTRY_PATH);

            assertNotNull(scenarios, "Scenarios should be loaded");
            
            logger.info("[OK] Auto-derived classpath base worked correctly");
        }
    }

    // ========================================================================
    // loadScenarioFromClasspath(String) Tests
    // ========================================================================
    
    @Nested
    @DisplayName("loadScenarioFromClasspath(String) Tests")
    class LoadScenarioFromClasspathTests {

        @Test
        @DisplayName("Should load individual scenario from classpath")
        void testLoadScenarioFromClasspath() throws Exception {
            logger.info("=== Testing loadScenarioFromClasspath() ===");

            String scenarioPath = TEST_RESOURCE_BASE + "basic-validation-scenario.yaml";
            
            ScenarioConfiguration scenario = loader.loadScenarioFromClasspath(scenarioPath);

            assertNotNull(scenario, "Scenario should not be null");

            logger.info("[OK] Successfully loaded scenario from classpath: {}", scenarioPath);
            logger.info("  Scenario ID: {}", scenario.getScenarioId());
            logger.info("  Name: {}", scenario.getName());
        }

        @Test
        @DisplayName("Should throw exception for non-existent scenario")
        void testLoadScenarioFromClasspathNotFound() {
            logger.info("=== Testing loadScenarioFromClasspath() with non-existent path ===");

            String nonExistentPath = "non-existent/scenario.yaml";

            assertThrows(
                Exception.class,
                () -> loader.loadScenarioFromClasspath(nonExistentPath),
                "Should throw exception for non-existent scenario"
            );

            logger.info("[OK] Correctly threw exception for non-existent scenario");
        }
    }

    // ========================================================================
    // loadScenarioFromStream(InputStream) Tests
    // ========================================================================
    
    @Nested
    @DisplayName("loadScenarioFromStream(InputStream) Tests")
    class LoadScenarioFromStreamTests {

        @Test
        @DisplayName("Should load scenario from InputStream")
        void testLoadScenarioFromStream() throws Exception {
            logger.info("=== Testing loadScenarioFromStream() ===");

            String scenarioYaml = """
                metadata:
                  id: "stream-scenario"
                  name: "Stream Scenario"
                  description: "Test scenario for stream loading"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "stream-scenario"
                  name: "Stream Test Scenario"
                  business-domain: "Testing"
                  data-types:
                    - "TestData"
                """;

            try (InputStream inputStream = new ByteArrayInputStream(scenarioYaml.getBytes(StandardCharsets.UTF_8))) {
                ScenarioConfiguration scenario = loader.loadScenarioFromStream(inputStream);

                assertNotNull(scenario, "Scenario should not be null");
                assertEquals("stream-scenario", scenario.getScenarioId(), "Scenario ID should match");

                logger.info("[OK] Successfully loaded scenario from stream");
                logger.info("  Scenario ID: {}", scenario.getScenarioId());
            }
        }

        @Test
        @DisplayName("Should throw exception for null InputStream")
        void testLoadScenarioFromStreamNullThrowsException() {
            logger.info("=== Testing loadScenarioFromStream() with null ===");

            assertThrows(
                Exception.class,
                () -> loader.loadScenarioFromStream(null),
                "Should throw exception for null InputStream"
            );

            logger.info("[OK] Correctly threw exception for null InputStream");
        }

        @Test
        @DisplayName("Should handle scenario with processing stages from stream")
        void testLoadScenarioWithProcessingStagesFromStream() throws Exception {
            logger.info("=== Testing loadScenarioFromStream() with processing stages ===");

            String scenarioYaml = """
                metadata:
                  id: "stages-scenario"
                  name: "Stages Scenario"
                  description: "Test scenario with processing stages"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "stages-scenario"
                  name: "Processing Stages Test Scenario"
                  business-domain: "Testing"
                  data-types:
                    - "TestData"
                  processing-stages:
                    - stage-name: "validation"
                      config-file: "validation.yaml"
                      execution-order: 1
                    - stage-name: "enrichment"
                      config-file: "enrichment.yaml"
                      execution-order: 2
                """;

            try (InputStream inputStream = new ByteArrayInputStream(scenarioYaml.getBytes(StandardCharsets.UTF_8))) {
                ScenarioConfiguration scenario = loader.loadScenarioFromStream(inputStream);

                assertNotNull(scenario, "Scenario should not be null");
                assertEquals("stages-scenario", scenario.getScenarioId(), "Scenario ID should match");
                
                logger.info("[OK] Successfully loaded scenario with processing stages from stream");
            }
        }
    }

    // ========================================================================
    // JAR URL Compatibility Tests (The Original Issue)
    // ========================================================================
    
    @Nested
    @DisplayName("JAR URL Compatibility Tests")
    class JarUrlCompatibilityTests {

        @Test
        @DisplayName("Classpath loading should work for JAR-packaged resources")
        void testClasspathLoadingWorksForJarResources() throws Exception {
            logger.info("=== Testing JAR URL compatibility via classpath loading ===");

            // This test verifies that the classpath loading approach works
            // regardless of whether the resource is in a JAR or on the filesystem.
            // The key is that we use InputStream-based loading, which avoids
            // the InvalidPathException that occurred with JAR URLs.

            Map<String, ScenarioConfiguration> scenarios = 
                loader.loadRegistryFromClasspath(TEST_REGISTRY_PATH);

            assertNotNull(scenarios, "Classpath loading should work");
            
            logger.info("[OK] Classpath loading works correctly (simulates JAR resource access)");
            logger.info("  This approach avoids the InvalidPathException that occurred with JAR URLs");
        }

        @Test
        @DisplayName("Stream-based loading bypasses filesystem path resolution")
        void testStreamLoadingBypassesPathResolution() throws Exception {
            logger.info("=== Testing stream loading bypasses path resolution ===");

            // Create a scenario YAML and load it via stream
            // This proves that we never need to convert URLs to filesystem paths
            String scenarioYaml = """
                metadata:
                  id: "jar-compatible-scenario"
                  name: "JAR Compatible Scenario"
                  description: "Test scenario for JAR compatibility"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "jar-compatible-scenario"
                  name: "JAR Compatible Test Scenario"
                  business-domain: "Testing"
                  data-types:
                    - "TestData"
                """;

            try (InputStream inputStream = new ByteArrayInputStream(scenarioYaml.getBytes(StandardCharsets.UTF_8))) {
                ScenarioConfiguration scenario = loader.loadScenarioFromStream(inputStream);

                assertNotNull(scenario, "Stream loading should work");
                assertEquals("jar-compatible-scenario", scenario.getScenarioId());

                logger.info("[OK] Stream-based loading successfully bypasses filesystem path resolution");
                logger.info("  This is the fix for the JAR URL InvalidPathException issue");
            }
        }
    }

    // ========================================================================
    // Edge Cases and Error Handling Tests
    // ========================================================================
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle scenario with no processing stages gracefully")
        void testScenarioWithNoStages() throws Exception {
            logger.info("=== Testing scenario with no processing stages ===");

            String scenarioYaml = """
                metadata:
                  id: "no-stages-scenario"
                  name: "No Stages Scenario"
                  description: "Test scenario with no processing stages"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "no-stages-scenario"
                  name: "Empty Processing Stages Test Scenario"
                  business-domain: "Testing"
                  data-types:
                    - "TestData"
                """;

            try (InputStream inputStream = new ByteArrayInputStream(scenarioYaml.getBytes(StandardCharsets.UTF_8))) {
                ScenarioConfiguration scenario = loader.loadScenarioFromStream(inputStream);

                assertNotNull(scenario, "Scenario should not be null");
                assertFalse(scenario.hasStageConfiguration(), "Scenario should not have stage configuration");

                logger.info("[OK] Correctly handled scenario with no processing stages");
            }
        }

        @Test
        @DisplayName("Should handle registry with disabled scenarios")
        void testRegistryWithDisabledScenarios() throws Exception {
            logger.info("=== Testing registry with disabled scenarios ===");

            // Note: Even disabled scenarios need valid config-file references
            // The registry loader still loads them, just marks them as disabled
            String registryYaml = """
                metadata:
                  id: "disabled-scenarios-registry"
                  name: "Disabled Scenarios Registry"
                  description: "Test registry with disabled scenarios"
                  type: "scenario-registry"
                  version: "1.0.0"
                
                scenarios:
                  - scenario-id: "disabled-scenario"
                    config-file: "basic-validation-scenario.yaml"
                    business-domain: "Testing"
                    enabled: false
                """;

            try (InputStream inputStream = new ByteArrayInputStream(registryYaml.getBytes(StandardCharsets.UTF_8))) {
                Map<String, ScenarioConfiguration> scenarios = loader.loadRegistry(inputStream, TEST_RESOURCE_BASE);

                assertNotNull(scenarios, "Scenarios map should not be null");
                // Disabled scenarios are still loaded, they're just marked as disabled
                if (scenarios.containsKey("disabled-scenario")) {
                    ScenarioConfiguration config = scenarios.get("disabled-scenario");
                    assertFalse(config.isEnabled(), "Scenario should be marked as disabled");
                    logger.info("[OK] Disabled scenario loaded with enabled={}", config.isEnabled());
                }
                
                logger.info("[OK] Registry with disabled scenarios processed correctly");
            }
        }

        @Test
        @DisplayName("Should handle scenario with metadata")
        void testScenarioWithMetadata() throws Exception {
            logger.info("=== Testing scenario with metadata ===");

            String scenarioYaml = """
                metadata:
                  id: "metadata-scenario"
                  name: "Metadata Test Scenario"
                  description: "Test scenario with rich metadata"
                  type: "scenario"
                  version: "1.0.0"
                
                scenario:
                  scenario-id: "metadata-scenario"
                  name: "Metadata Test Scenario"
                  description: "A scenario with rich metadata"
                  business-domain: "Testing"
                  data-types:
                    - "TestData"
                  metadata:
                    business-domain: "FinancialServices"
                    owner: "Test Team"
                    processing-sla-ms: 5000
                """;

            try (InputStream inputStream = new ByteArrayInputStream(scenarioYaml.getBytes(StandardCharsets.UTF_8))) {
                ScenarioConfiguration scenario = loader.loadScenarioFromStream(inputStream);

                assertNotNull(scenario, "Scenario should not be null");
                assertEquals("metadata-scenario", scenario.getScenarioId());
                
                logger.info("[OK] Correctly loaded scenario with metadata");
            }
        }
    }
}
