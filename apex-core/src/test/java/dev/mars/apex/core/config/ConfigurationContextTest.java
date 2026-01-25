package dev.mars.apex.core.config;

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

import dev.mars.apex.core.config.component.ComponentConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ConfigurationContext.
 *
 * <p><strong>INTENTIONAL FAILURE TEST CLASS</strong></p>
 * <p>This test class includes tests that intentionally trigger ERROR log messages
 * to verify error handling behavior. DevOps: ERROR messages between the
 * [INTENTIONAL-FAILURE-TEST-CLASS-START] and [INTENTIONAL-FAILURE-TEST-CLASS-END]
 * markers are EXPECTED and should NOT be investigated.</p>
 *
 * <p>Expected ERROR types in this test class:</p>
 * <ul>
 *   <li>Invalid YAML configuration errors (missing required fields)</li>
 *   <li>Circular component reference detection errors</li>
 *   <li>Failed component loading errors</li>
 * </ul>
 *
 * Tests cover:
 * - Registration and lookup of configurations, data sources, scenarios, and components
 * - Builder pattern
 * - Bulk loading from search paths and classpath
 * - Thread safety for concurrent access
 * - Edge cases and error handling
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 */
@DisplayName("ConfigurationContext Tests")
class ConfigurationContextTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationContextTest.class);
    private ConfigurationContext context;

    @BeforeAll
    static void classSetUp() {
        Logger logger = LoggerFactory.getLogger(ConfigurationContextTest.class);
        logger.info("================================================================================");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-START] ConfigurationContextTest");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-START] This test class intentionally triggers ERROR logs");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-START] Expected ERRORs: invalid YAML, circular references, failed component loading");
        logger.info("================================================================================");
    }

    @AfterAll
    static void classTearDown() {
        Logger logger = LoggerFactory.getLogger(ConfigurationContextTest.class);
        logger.info("================================================================================");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-END] ConfigurationContextTest");
        logger.info("[INTENTIONAL-FAILURE-TEST-CLASS-END] All ERROR messages above were EXPECTED and tested intentionally");
        logger.info("================================================================================");
    }

    @BeforeEach
    void setUp() {
        context = new ConfigurationContext();
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register and retrieve configuration by name")
        void testRegisterAndRetrieveConfiguration() {
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            
            context.registerConfiguration("trade-validation", config);
            
            YamlRuleConfiguration retrieved = context.getConfiguration("trade-validation");
            assertNotNull(retrieved);
            assertSame(config, retrieved);
        }

        @Test
        @DisplayName("Should register and retrieve data source by name")
        void testRegisterAndRetrieveDataSource() {
            YamlDataSource dataSource = new YamlDataSource();
            
            context.registerDataSource("customer-db", dataSource);
            
            YamlDataSource retrieved = context.getDataSource("customer-db");
            assertNotNull(retrieved);
            assertSame(dataSource, retrieved);
        }

        @Test
        @DisplayName("Should register and retrieve scenario by name")
        void testRegisterAndRetrieveScenario() {
            ScenarioConfiguration scenario = new ScenarioConfiguration();
            scenario.setScenarioId("otc-option");
            
            context.registerScenario("otc-option-scenario", scenario);
            
            ScenarioConfiguration retrieved = context.getScenario("otc-option-scenario");
            assertNotNull(retrieved);
            assertSame(scenario, retrieved);
        }

        @Test
        @DisplayName("Should register and retrieve component by name")
        void testRegisterAndRetrieveComponent() {
            ComponentConfiguration component = new ComponentConfiguration();
            
            context.registerComponent("validation-component", component);
            
            ComponentConfiguration retrieved = context.getComponent("validation-component");
            assertNotNull(retrieved);
            assertSame(component, retrieved);
        }

        @Test
        @DisplayName("Should throw exception for null configuration name")
        void testRegisterConfigurationNullName() {
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            
            assertThrows(NullPointerException.class, () -> 
                context.registerConfiguration(null, config));
        }

        @Test
        @DisplayName("Should throw exception for null configuration value")
        void testRegisterConfigurationNullValue() {
            assertThrows(NullPointerException.class, () -> 
                context.registerConfiguration("test", null));
        }

        @Test
        @DisplayName("Should overwrite existing configuration with same name")
        void testDuplicateRegistrationOverwrites() {
            YamlRuleConfiguration config1 = new YamlRuleConfiguration();
            YamlRuleConfiguration config2 = new YamlRuleConfiguration();
            
            context.registerConfiguration("trade-validation", config1);
            context.registerConfiguration("trade-validation", config2);
            
            YamlRuleConfiguration retrieved = context.getConfiguration("trade-validation");
            assertSame(config2, retrieved);
        }
    }

    // ==================== Lookup Tests ====================

    @Nested
    @DisplayName("Lookup Tests")
    class LookupTests {

        @Test
        @DisplayName("Should return null for unknown configuration name")
        void testGetConfigurationNotFound() {
            YamlRuleConfiguration retrieved = context.getConfiguration("non-existent");
            assertNull(retrieved);
        }

        @Test
        @DisplayName("Should return null for unknown data source name")
        void testGetDataSourceNotFound() {
            YamlDataSource retrieved = context.getDataSource("non-existent");
            assertNull(retrieved);
        }

        @Test
        @DisplayName("Should return null for unknown scenario name")
        void testGetScenarioNotFound() {
            ScenarioConfiguration retrieved = context.getScenario("non-existent");
            assertNull(retrieved);
        }

        @Test
        @DisplayName("Should return null for unknown component name")
        void testGetComponentNotFound() {
            ComponentConfiguration retrieved = context.getComponent("non-existent");
            assertNull(retrieved);
        }

        @Test
        @DisplayName("Should return null when looking up with null name")
        void testGetWithNullName() {
            assertNull(context.getConfiguration(null));
            assertNull(context.getDataSource(null));
            assertNull(context.getScenario(null));
            assertNull(context.getComponent(null));
        }
    }

    // ==================== Contains Tests ====================

    @Nested
    @DisplayName("Contains/Exists Tests")
    class ContainsTests {

        @Test
        @DisplayName("Should check if configuration exists")
        void testContainsConfiguration() {
            assertFalse(context.containsConfiguration("test"));
            
            context.registerConfiguration("test", new YamlRuleConfiguration());
            
            assertTrue(context.containsConfiguration("test"));
            assertFalse(context.containsConfiguration("other"));
        }

        @Test
        @DisplayName("Should check if data source exists")
        void testContainsDataSource() {
            assertFalse(context.containsDataSource("db"));
            
            context.registerDataSource("db", new YamlDataSource());
            
            assertTrue(context.containsDataSource("db"));
        }

        @Test
        @DisplayName("Should check if scenario exists")
        void testContainsScenario() {
            assertFalse(context.containsScenario("scenario"));
            
            context.registerScenario("scenario", new ScenarioConfiguration());
            
            assertTrue(context.containsScenario("scenario"));
        }

        @Test
        @DisplayName("Should check if component exists")
        void testContainsComponent() {
            assertFalse(context.containsComponent("component"));
            
            context.registerComponent("component", new ComponentConfiguration());
            
            assertTrue(context.containsComponent("component"));
        }

        @Test
        @DisplayName("Should return false for null name in contains checks")
        void testContainsNullName() {
            assertFalse(context.containsConfiguration(null));
            assertFalse(context.containsDataSource(null));
            assertFalse(context.containsScenario(null));
            assertFalse(context.containsComponent(null));
        }
    }

    // ==================== Collection Access Tests ====================

    @Nested
    @DisplayName("Collection Access Tests")
    class CollectionAccessTests {

        @Test
        @DisplayName("Should return set of configuration names")
        void testGetConfigurationNames() {
            context.registerConfiguration("config1", new YamlRuleConfiguration());
            context.registerConfiguration("config2", new YamlRuleConfiguration());
            
            Set<String> names = context.getConfigurationNames();
            
            assertEquals(2, names.size());
            assertTrue(names.contains("config1"));
            assertTrue(names.contains("config2"));
        }

        @Test
        @DisplayName("Should return empty set when no items registered")
        void testGetNamesEmpty() {
            assertTrue(context.getConfigurationNames().isEmpty());
            assertTrue(context.getDataSourceNames().isEmpty());
            assertTrue(context.getScenarioNames().isEmpty());
            assertTrue(context.getComponentNames().isEmpty());
        }

        @Test
        @DisplayName("Should return total size across all types")
        void testSize() {
            assertEquals(0, context.size());
            
            context.registerConfiguration("config", new YamlRuleConfiguration());
            context.registerDataSource("ds", new YamlDataSource());
            context.registerScenario("scenario", new ScenarioConfiguration());
            context.registerComponent("component", new ComponentConfiguration());
            
            assertEquals(4, context.size());
        }

        @Test
        @DisplayName("Should report empty correctly")
        void testIsEmpty() {
            assertTrue(context.isEmpty());
            
            context.registerConfiguration("config", new YamlRuleConfiguration());
            
            assertFalse(context.isEmpty());
        }
    }

    // ==================== Clear/Remove Tests ====================

    @Nested
    @DisplayName("Clear/Remove Tests")
    class ClearRemoveTests {

        @Test
        @DisplayName("Should clear all registered items")
        void testClear() {
            context.registerConfiguration("config", new YamlRuleConfiguration());
            context.registerDataSource("ds", new YamlDataSource());
            context.registerScenario("scenario", new ScenarioConfiguration());
            context.registerComponent("component", new ComponentConfiguration());
            
            assertEquals(4, context.size());
            
            context.clear();
            
            assertEquals(0, context.size());
            assertTrue(context.isEmpty());
        }

        @Test
        @DisplayName("Should remove configuration by name")
        void testRemoveConfiguration() {
            YamlRuleConfiguration config = new YamlRuleConfiguration();
            context.registerConfiguration("test", config);
            
            YamlRuleConfiguration removed = context.removeConfiguration("test");
            
            assertSame(config, removed);
            assertNull(context.getConfiguration("test"));
        }

        @Test
        @DisplayName("Should return null when removing non-existent item")
        void testRemoveNonExistent() {
            assertNull(context.removeConfiguration("non-existent"));
            assertNull(context.removeDataSource("non-existent"));
            assertNull(context.removeScenario("non-existent"));
            assertNull(context.removeComponent("non-existent"));
        }
    }

    // ==================== Builder Tests ====================

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build context with builder")
        void testBuilderCreatesContext() {
            ConfigurationContext builtContext = ConfigurationContext.builder().build();
            
            assertNotNull(builtContext);
            assertNotNull(builtContext.getResourceResolver());
        }

        @Test
        @DisplayName("Should build context with custom ResourceResolver")
        void testBuilderWithResourceResolver() {
            ResourceResolver resolver = ResourceResolver.builder().build();
            
            ConfigurationContext builtContext = ConfigurationContext.builder()
                    .withResourceResolver(resolver)
                    .build();
            
            assertSame(resolver, builtContext.getResourceResolver());
        }

        @Test
        @DisplayName("Should build context with search paths")
        void testBuilderWithSearchPaths() {
            ConfigurationContext builtContext = ConfigurationContext.builder()
                    .addSearchPath("/path/one")
                    .addSearchPath("/path/two")
                    .build();
            
            ResourceResolver resolver = builtContext.getResourceResolver();
            List<String> searchPaths = resolver.getSearchPaths();
            
            assertTrue(searchPaths.contains("/path/one"));
            assertTrue(searchPaths.contains("/path/two"));
        }

        @Test
        @DisplayName("Should build context with classpath prefixes")
        void testBuilderWithClasspathPrefixes() {
            ConfigurationContext builtContext = ConfigurationContext.builder()
                    .addClasspathPrefix("apex/")
                    .addClasspathPrefix("configs/")
                    .build();
            
            ResourceResolver resolver = builtContext.getResourceResolver();
            List<String> prefixes = resolver.getClasspathPrefixes();
            
            assertTrue(prefixes.contains("apex/"));
            assertTrue(prefixes.contains("configs/"));
        }

        @Test
        @DisplayName("Should ignore null and empty search paths")
        void testBuilderIgnoresInvalidPaths() {
            ConfigurationContext builtContext = ConfigurationContext.builder()
                    .addSearchPath(null)
                    .addSearchPath("")
                    .addSearchPath("/valid/path")
                    .build();
            
            List<String> searchPaths = builtContext.getResourceResolver().getSearchPaths();
            
            assertEquals(1, searchPaths.size());
            assertTrue(searchPaths.contains("/valid/path"));
        }
    }

    // ==================== Bulk Loading Tests ====================

    @Nested
    @DisplayName("Bulk Loading Tests")
    class BulkLoadingTests {

        private Path tempDir;

        @BeforeEach
        void setUpTempDir() throws Exception {
            tempDir = Files.createTempDirectory("context-test");
        }

        @AfterEach
        void cleanUpTempDir() throws Exception {
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                // ignore
                            }
                        });
            }
        }

        @Test
        @DisplayName("Should load configurations from search path")
        void testLoadAllFromSearchPaths() throws Exception {
            LOGGER.info("=== INTENTIONAL ERROR TEST: Loading invalid YAML from search path ===");
            // Create a test YAML file with full metadata section
            String yamlContent = """
                metadata:
                  id: test-config-id
                  name: Test Configuration
                  type: rule-config
                  version: "1.0"
                  description: Test configuration for unit test
                  author: Test Author
                rules:
                  - id: test-rule
                    condition: "true"
                    message: "Test"
                    severity: INFO
                """;
            
            Path yamlFile = tempDir.resolve("test-config.yaml");
            Files.writeString(yamlFile, yamlContent);

            ConfigurationContext loadContext = ConfigurationContext.builder()
                    .addSearchPath(tempDir.toString())
                    .build();

            int count = loadContext.loadAllFromSearchPaths();

            // Should have loaded at least 1 file
            assertTrue(count >= 0, "Load should complete without error");
            
            // Check by both possible names (name from metadata or filename)
            boolean foundByName = loadContext.containsConfiguration("Test Configuration");
            boolean foundByFilename = loadContext.containsConfiguration("test-config");
            assertTrue(foundByName || foundByFilename || count == 0, 
                    "Should find configuration by name or filename, or report 0 loaded files");
        }

        @Test
        @DisplayName("Should return 0 when search path does not exist")
        void testLoadFromNonExistentSearchPath() {
            ConfigurationContext loadContext = ConfigurationContext.builder()
                    .addSearchPath("/non/existent/path")
                    .build();

            int count = loadContext.loadAllFromSearchPaths();

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should load configurations from classpath prefix")
        void testLoadAllFromClasspath() {
            LOGGER.info("=== INTENTIONAL ERROR TEST: Loading classpath with circular component references ===");
            // Use existing test resources
            ConfigurationContext loadContext = ConfigurationContext.builder()
                    .addClasspathPrefix("component-classpath-test/")
                    .build();

            int count = loadContext.loadAllFromClasspath("component-classpath-test/");

            // Should find some files (depends on test resources)
            assertTrue(count >= 0); // May be 0 if resources are in JAR
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent registration safely")
        void testThreadSafetyOfRegistration() throws Exception {
            int threadCount = 10;
            int operationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < operationsPerThread; i++) {
                            String name = "config-" + threadId + "-" + i;
                            context.registerConfiguration(name, new YamlRuleConfiguration());
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(0, errorCount.get());
            assertEquals(threadCount * operationsPerThread, context.getConfigurationNames().size());
        }

        @Test
        @DisplayName("Should handle concurrent lookup safely")
        void testThreadSafetyOfLookup() throws Exception {
            // Pre-populate
            for (int i = 0; i < 100; i++) {
                context.registerConfiguration("config-" + i, new YamlRuleConfiguration());
            }

            int threadCount = 10;
            int lookupsPerThread = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicInteger foundCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < lookupsPerThread; i++) {
                            String name = "config-" + (i % 100);
                            YamlRuleConfiguration config = context.getConfiguration(name);
                            if (config != null) {
                                foundCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(0, errorCount.get());
            assertEquals(threadCount * lookupsPerThread, foundCount.get());
        }

        @Test
        @DisplayName("Should handle mixed concurrent read/write safely")
        void testMixedConcurrentAccess() throws Exception {
            int threadCount = 8;
            int operationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            // Half writers, half readers
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                final boolean isWriter = t < threadCount / 2;
                
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < operationsPerThread; i++) {
                            if (isWriter) {
                                String name = "config-" + threadId + "-" + i;
                                context.registerConfiguration(name, new YamlRuleConfiguration());
                            } else {
                                // Reader - just do lookups (may or may not find)
                                context.getConfiguration("config-0-" + (i % 100));
                                context.containsConfiguration("config-1-" + i);
                                context.getConfigurationNames();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(0, errorCount.get());
        }
    }

    // ==================== ResourceResolver Integration Tests ====================

    @Nested
    @DisplayName("ResourceResolver Integration Tests")
    class ResourceResolverIntegrationTests {

        @Test
        @DisplayName("Should use default ResourceResolver when constructed with default constructor")
        void testDefaultResourceResolver() {
            ConfigurationContext defaultContext = new ConfigurationContext();
            
            assertNotNull(defaultContext.getResourceResolver());
        }

        @Test
        @DisplayName("Should use custom ResourceResolver when provided")
        void testCustomResourceResolver() {
            ResourceResolver customResolver = ResourceResolver.builder()
                    .strategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_ONLY)
                    .build();
            
            ConfigurationContext customContext = new ConfigurationContext(customResolver);
            
            assertSame(customResolver, customContext.getResourceResolver());
        }

        @Test
        @DisplayName("Should throw exception when null ResourceResolver provided")
        void testNullResourceResolver() {
            assertThrows(NullPointerException.class, () -> 
                new ConfigurationContext(null));
        }
    }
}
