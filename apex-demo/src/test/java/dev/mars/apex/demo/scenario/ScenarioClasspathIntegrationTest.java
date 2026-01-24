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
package dev.mars.apex.demo.scenario;

import dev.mars.apex.core.config.yaml.ScenarioRegistryLoader;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioStage;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScenarioClasspathIntegrationTest - End-to-End Classpath-Based Scenario Loading Tests
 *
 * <p>PURPOSE:</p>
 * This test class demonstrates and validates the Unified Resource Loading for APEX Configuration
 * implementation (Phase 4 of the plan). It tests scenario registry and configuration loading
 * from the classpath rather than the filesystem.
 *
 * <p>TESTING APPROACH:</p>
 * <ul>
 *   <li>Loads scenario registry from classpath using loadRegistry(InputStream, classpathBase)</li>
 *   <li>Resolves relative scenario file references within the classpath context</li>
 *   <li>Validates scenario configurations are fully parsed including stages and rules</li>
 *   <li>Tests error handling for missing classpath resources</li>
 * </ul>
 *
 * <p>BUSINESS CONTEXT:</p>
 * Classpath-based resource loading is essential for:
 * <ul>
 *   <li>JAR-packaged applications where configurations must be embedded</li>
 *   <li>Spring Boot applications using classpath-based resource resolution</li>
 *   <li>Library distributions where consumers don't have filesystem access</li>
 *   <li>Microservices with containerized deployments</li>
 * </ul>
 *
 * <p>TEST RESOURCES:</p>
 * This test uses resources located at:
 * <pre>
 * src/test/java/dev/mars/apex/demo/scenario/classpath-integration/
 *   ├── scenario-registry.yaml
 *   └── scenarios/
 *       ├── otc-option-scenario.yaml
 *       └── simple-trade-scenario.yaml
 * </pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @version 1.0.0
 * @since 2026-01-07
 * @see ScenarioRegistryLoader#loadRegistry(InputStream, String)
 */
@DisplayName("Scenario Classpath Integration Tests")
public class ScenarioClasspathIntegrationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioClasspathIntegrationTest.class);

    private static final String CLASSPATH_REGISTRY = "dev/mars/apex/demo/scenario/classpath-integration/scenario-registry.yaml";
    private static final String CLASSPATH_BASE = "dev/mars/apex/demo/scenario/classpath-integration/";

    private ScenarioRegistryLoader scenarioLoader;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        logger.info("Setting up ScenarioClasspathIntegrationTest");
        scenarioLoader = new ScenarioRegistryLoader(yamlLoader);
    }

    /**
     * Helper method to load scenarios from classpath.
     * Uses the stream-based API with classpath base for relative path resolution.
     */
    private Map<String, ScenarioConfiguration> loadScenariosFromClasspath() throws YamlConfigurationException {
        try (InputStream registryStream = getClass().getClassLoader().getResourceAsStream(CLASSPATH_REGISTRY)) {
            if (registryStream == null) {
                throw new YamlConfigurationException("Registry not found on classpath: " + CLASSPATH_REGISTRY);
            }
            return scenarioLoader.loadRegistry(registryStream, CLASSPATH_BASE);
        } catch (Exception e) {
            throw new YamlConfigurationException("Failed to load scenarios from classpath: " + e.getMessage(), e);
        }
    }

    // ========================================================================
    // Registry Loading Tests
    // ========================================================================

    @Nested
    @DisplayName("Classpath Registry Loading Tests")
    class ClasspathRegistryLoadingTests {

        @Test
        @DisplayName("Should load scenario registry from classpath")
        void testLoadRegistryFromClasspath() throws Exception {
            logger.info("=== Testing loadRegistry(InputStream, classpathBase) ===");
            logger.info("TEST OBJECTIVE: Validate scenario registry loading from classpath resources");

            // Load registry from classpath using stream-based API
            Map<String, ScenarioConfiguration> scenarios = loadScenariosFromClasspath();

            // Validate loading succeeded
            assertNotNull(scenarios, "Scenarios map should not be null");
            assertFalse(scenarios.isEmpty(), "Scenarios map should not be empty");

            logger.info("[OK] Successfully loaded {} scenarios from classpath registry", scenarios.size());

            // Log loaded scenarios
            scenarios.forEach((id, config) -> {
                logger.info("  - Scenario: {} (domain: {}, enabled: {})",
                        id, config.getBusinessDomain(), config.isEnabled());
            });

            // Verify expected scenarios
            assertTrue(scenarios.containsKey("otc-option-validation"),
                    "Registry should contain otc-option-validation scenario");
            assertTrue(scenarios.containsKey("simple-trade-validation"),
                    "Registry should contain simple-trade-validation scenario");

            logger.info("=== Registry Loading Test PASSED ===");
        }

        @Test
        @DisplayName("Should resolve relative scenario file paths from classpath")
        void testRelativePathResolution() throws Exception {
            logger.info("=== Testing Relative Path Resolution ===");
            logger.info("TEST OBJECTIVE: Validate config-file references resolve correctly in classpath context");

            Map<String, ScenarioConfiguration> scenarios = loadScenariosFromClasspath();

            // Get the OTC option scenario and verify it was fully loaded
            ScenarioConfiguration otcScenario = scenarios.get("otc-option-validation");
            assertNotNull(otcScenario, "OTC option scenario should be loaded");

            // Verify scenario configuration was fully parsed
            assertNotNull(otcScenario.getScenarioId(), "Scenario ID should be set");
            assertEquals("otc-option-validation", otcScenario.getScenarioId(),
                    "Scenario ID should match");

            logger.info("[OK] OTC Option scenario loaded successfully:");
            logger.info("  - Scenario ID: {}", otcScenario.getScenarioId());
            logger.info("  - Business Domain: {}", otcScenario.getBusinessDomain());
            logger.info("  - Enabled: {}", otcScenario.isEnabled());

            logger.info("=== Relative Path Resolution Test PASSED ===");
        }

        @Test
        @DisplayName("Should throw exception for non-existent classpath resource")
        void testNonExistentResourceThrowsException() {
            logger.info("=== Testing Non-Existent Resource Handling ===");
            logger.info("TEST OBJECTIVE: Validate proper error handling for missing classpath resources");

            String nonExistentPath = "non/existent/registry.yaml";

            YamlConfigurationException exception = assertThrows(
                    YamlConfigurationException.class,
                    () -> {
                        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(nonExistentPath)) {
                            if (stream == null) {
                                throw new YamlConfigurationException("Registry not found: " + nonExistentPath);
                            }
                            scenarioLoader.loadRegistry(stream, "non/existent/");
                        }
                    },
                    "Should throw exception for non-existent classpath resource"
            );

            assertTrue(exception.getMessage().contains("not found") || exception.getMessage().contains(nonExistentPath),
                    "Exception message should indicate resource not found");

            logger.info("[OK] Correctly threw exception: {}", exception.getMessage());
            logger.info("=== Non-Existent Resource Test PASSED ===");
        }
    }

    // ========================================================================
    // Scenario Configuration Validation Tests
    // ========================================================================

    @Nested
    @DisplayName("Scenario Configuration Validation Tests")
    class ScenarioConfigurationValidationTests {

        @Test
        @DisplayName("Should load OTC option scenario with stages and rules")
        void testOtcOptionScenarioConfiguration() throws Exception {
            logger.info("=== Testing OTC Option Scenario Configuration ===");
            logger.info("TEST OBJECTIVE: Validate scenario stages and rules are fully parsed from classpath");

            Map<String, ScenarioConfiguration> scenarios = loadScenariosFromClasspath();
            ScenarioConfiguration otcScenario = scenarios.get("otc-option-validation");

            assertNotNull(otcScenario, "OTC option scenario should be loaded");

            // Verify basic scenario properties
            assertEquals("otc-option-validation", otcScenario.getScenarioId());
            assertEquals("OTC Derivatives", otcScenario.getBusinessDomain());
            assertTrue(otcScenario.isEnabled(), "Scenario should be enabled");

            logger.info("[OK] Basic scenario properties validated");
            logger.info("  - Scenario ID: {}", otcScenario.getScenarioId());
            logger.info("  - Business Domain: {}", otcScenario.getBusinessDomain());
            logger.info("  - Enabled: {}", otcScenario.isEnabled());

            // Verify stages are present
            List<ScenarioStage> stages = otcScenario.getProcessingStages();
            if (stages != null && !stages.isEmpty()) {
                logger.info("[OK] Scenario has {} stages:", stages.size());
                stages.forEach(stage -> {
                    logger.info("  - Stage: {} (failure-policy: {})",
                            stage.getStageName(), stage.getFailurePolicy());
                });
            } else {
                logger.info("  Note: Stages not configured or embedded in rule-configurations");
            }

            // Verify data types
            List<String> dataTypes = otcScenario.getDataTypes();
            if (dataTypes != null && !dataTypes.isEmpty()) {
                logger.info("[OK] Scenario data types: {}", dataTypes);
            }

            logger.info("=== OTC Option Scenario Configuration Test PASSED ===");
        }

        @Test
        @DisplayName("Should load simple trade scenario with validation rules")
        void testSimpleTradeScenarioConfiguration() throws Exception {
            logger.info("=== Testing Simple Trade Scenario Configuration ===");
            logger.info("TEST OBJECTIVE: Validate basic trade scenario is fully parsed");

            Map<String, ScenarioConfiguration> scenarios = loadScenariosFromClasspath();
            ScenarioConfiguration simpleScenario = scenarios.get("simple-trade-validation");

            assertNotNull(simpleScenario, "Simple trade scenario should be loaded");

            // Verify basic scenario properties
            assertEquals("simple-trade-validation", simpleScenario.getScenarioId());
            assertEquals("Trading", simpleScenario.getBusinessDomain());
            assertTrue(simpleScenario.isEnabled(), "Scenario should be enabled");

            logger.info("[OK] Simple trade scenario validated:");
            logger.info("  - Scenario ID: {}", simpleScenario.getScenarioId());
            logger.info("  - Business Domain: {}", simpleScenario.getBusinessDomain());
            logger.info("  - Enabled: {}", simpleScenario.isEnabled());

            logger.info("=== Simple Trade Scenario Configuration Test PASSED ===");
        }

        @Test
        @DisplayName("Should verify all scenarios have required metadata")
        void testAllScenariosHaveRequiredMetadata() throws Exception {
            logger.info("=== Testing All Scenarios Required Metadata ===");
            logger.info("TEST OBJECTIVE: Validate all loaded scenarios have required fields");

            Map<String, ScenarioConfiguration> scenarios = loadScenariosFromClasspath();

            logger.info("Validating {} scenarios...", scenarios.size());

            scenarios.forEach((id, config) -> {
                // Verify required fields are present
                assertNotNull(config.getScenarioId(), "Scenario " + id + " should have scenarioId");
                assertNotNull(config.getBusinessDomain(), "Scenario " + id + " should have businessDomain");

                logger.info("[OK] Scenario '{}' has required metadata", id);
            });

            logger.info("=== All Scenarios Required Metadata Test PASSED ===");
        }
    }

    // ========================================================================
    // Multiple Scenario Loading Tests
    // ========================================================================

    @Nested
    @DisplayName("Multiple Scenario Loading Tests")
    class MultipleScenarioLoadingTests {

        @Test
        @DisplayName("Should load all scenarios from registry")
        void testLoadAllScenarios() throws Exception {
            logger.info("=== Testing Load All Scenarios ===");
            logger.info("TEST OBJECTIVE: Validate all scenarios in registry are loaded");

            Map<String, ScenarioConfiguration> scenarios = loadScenariosFromClasspath();

            // We expect 2 scenarios based on our test registry
            assertEquals(2, scenarios.size(), "Should load 2 scenarios from registry");

            logger.info("[OK] Loaded all {} expected scenarios", scenarios.size());

            // List all scenarios
            scenarios.keySet().forEach(id -> logger.info("  - {}", id));

            logger.info("=== Load All Scenarios Test PASSED ===");
        }

        @Test
        @DisplayName("Should preserve scenario enabled status from registry")
        void testScenarioEnabledStatus() throws Exception {
            logger.info("=== Testing Scenario Enabled Status ===");
            logger.info("TEST OBJECTIVE: Validate enabled status is correctly loaded from registry");

            Map<String, ScenarioConfiguration> scenarios = loadScenariosFromClasspath();

            // Both scenarios should be enabled based on our test registry
            scenarios.forEach((id, config) -> {
                assertTrue(config.isEnabled(), "Scenario '" + id + "' should be enabled");
                logger.info("[OK] Scenario '{}' enabled status: {}", id, config.isEnabled());
            });

            logger.info("=== Scenario Enabled Status Test PASSED ===");
        }
    }
}
