package dev.mars.apex.core.engine.config;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RulesEngine.Builder fluent API.
 * 
 * <p>Tests for Phase 6 of the Unified Resource Loading plan, validating:</p>
 * <ul>
 *   <li>Builder creation and method chaining</li>
 *   <li>Search path configuration</li>
 *   <li>Classpath prefix configuration</li>
 *   <li>Context variable substitution</li>
 *   <li>Building from various sources</li>
 *   <li>Error handling for invalid configurations</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@DisplayName("RulesEngine.Builder Tests")
class RulesEngineBuilderTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineBuilderTest.class);

    @BeforeEach
    void setUp() {
        logger.info("Setting up RulesEngineBuilderTest");
    }

    // ========================================================================
    // POSITIVE TESTS: Builder Creation and Method Chaining
    // ========================================================================

    @Nested
    @DisplayName("Builder Creation and Method Chaining (Positive)")
    class BuilderCreationPositiveTests {

        @Test
        @DisplayName("Should create builder via static factory method")
        void testBuilderCreation() {
            logger.info("=== Testing builder creation ===");

            RulesEngine.Builder builder = RulesEngine.builder();

            assertNotNull(builder, "Builder should not be null");
        }

        @Test
        @DisplayName("Should support fluent method chaining")
        void testMethodChaining() {
            logger.info("=== Testing fluent method chaining ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPath("/path1")
                .addSearchPath("/path2")
                .addClasspathPrefix("prefix1/")
                .withContext("key", "value")
                .fromScenarioRegistry("test-registry.yaml");

            assertNotNull(builder, "Builder should not be null after chaining");
            assertEquals(2, builder.getSearchPaths().size(), "Should have 2 search paths");
            assertEquals(1, builder.getClasspathPrefixes().size(), "Should have 1 classpath prefix");
            assertEquals(1, builder.getContextVariables().size(), "Should have 1 context variable");
        }

        @Test
        @DisplayName("Should add multiple search paths via varargs")
        void testAddSearchPathsVarargs() {
            logger.info("=== Testing addSearchPaths varargs ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPaths("/path1", "/path2", "/path3");

            assertEquals(3, builder.getSearchPaths().size());
            assertTrue(builder.getSearchPaths().containsAll(Arrays.asList("/path1", "/path2", "/path3")));
        }

        @Test
        @DisplayName("Should add multiple search paths from collection")
        void testAddSearchPathsCollection() {
            logger.info("=== Testing addSearchPaths from collection ===");

            List<String> paths = Arrays.asList("/path1", "/path2");
            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPaths(paths);

            assertEquals(2, builder.getSearchPaths().size());
        }

        @Test
        @DisplayName("Should add multiple classpath prefixes via varargs")
        void testAddClasspathPrefixesVarargs() {
            logger.info("=== Testing addClasspathPrefixes varargs ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addClasspathPrefixes("prefix1/", "prefix2/", "prefix3/");

            assertEquals(3, builder.getClasspathPrefixes().size());
        }
    }

    // ========================================================================
    // POSITIVE TESTS: Search Path Configuration
    // ========================================================================

    @Nested
    @DisplayName("Search Path Configuration (Positive)")
    class SearchPathConfigurationPositiveTests {

        @Test
        @DisplayName("Should normalize classpath prefix to end with slash")
        void testClasspathPrefixNormalization() {
            logger.info("=== Testing classpath prefix normalization ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addClasspathPrefix("prefix-no-slash")
                .addClasspathPrefix("prefix-with-slash/");

            List<String> prefixes = builder.getClasspathPrefixes();
            assertEquals(2, prefixes.size());
            assertTrue(prefixes.get(0).endsWith("/"), "Should normalize to end with /");
            assertTrue(prefixes.get(1).endsWith("/"), "Should keep existing /");
        }

        @Test
        @DisplayName("Should expand environment variables in search paths")
        void testEnvVarExpansionInSearchPaths() {
            logger.info("=== Testing env var expansion in search paths ===");

            // Set a test property
            String originalValue = System.getProperty("apex.test.builder.path");
            System.setProperty("apex.test.builder.path", "/expanded/path");

            try {
                RulesEngine.Builder builder = RulesEngine.builder()
                    .addSearchPath("${apex.test.builder.path}/configs");

                List<String> paths = builder.getSearchPaths();
                assertEquals(1, paths.size());
                assertEquals("/expanded/path/configs", paths.get(0), "Should expand env var");

            } finally {
                if (originalValue != null) {
                    System.setProperty("apex.test.builder.path", originalValue);
                } else {
                    System.clearProperty("apex.test.builder.path");
                }
            }
        }

        @Test
        @DisplayName("Should return unmodifiable search paths list")
        void testUnmodifiableSearchPaths() {
            logger.info("=== Testing unmodifiable search paths ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPath("/path1");

            List<String> paths = builder.getSearchPaths();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                paths.add("/path2");
            }, "Should throw when trying to modify returned list");
        }

        @Test
        @DisplayName("Should return unmodifiable context variables map")
        void testUnmodifiableContextVariables() {
            logger.info("=== Testing unmodifiable context variables ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .withContext("key", "value");

            Map<String, Object> context = builder.getContextVariables();
            
            assertThrows(UnsupportedOperationException.class, () -> {
                context.put("key2", "value2");
            }, "Should throw when trying to modify returned map");
        }
    }

    // ========================================================================
    // POSITIVE TESTS: Context Variables
    // ========================================================================

    @Nested
    @DisplayName("Context Variables (Positive)")
    class ContextVariablesPositiveTests {

        @Test
        @DisplayName("Should add context variable")
        void testAddContextVariable() {
            logger.info("=== Testing withContext() ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .withContext("environment", "production")
                .withContext("region", "us-east-1");

            Map<String, Object> context = builder.getContextVariables();
            assertEquals(2, context.size());
            assertEquals("production", context.get("environment"));
            assertEquals("us-east-1", context.get("region"));
        }

        @Test
        @DisplayName("Should add context variables from map")
        void testAddContextVariablesFromMap() {
            logger.info("=== Testing withContext(Map) ===");

            Map<String, Object> variables = Map.of(
                "key1", "value1",
                "key2", 123,
                "key3", true
            );

            RulesEngine.Builder builder = RulesEngine.builder()
                .withContext(variables);

            Map<String, Object> context = builder.getContextVariables();
            assertEquals(3, context.size());
        }

        @Test
        @DisplayName("Should expand context variables in search paths")
        void testContextVariableExpansionInPaths() {
            logger.info("=== Testing context var expansion in paths ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .withContext("config.root", "/app/configs")
                .addSearchPath("${config.root}/rules");

            List<String> paths = builder.getSearchPaths();
            assertEquals("/app/configs/rules", paths.get(0));
        }
    }

    // ========================================================================
    // POSITIVE TESTS: Building from Sources
    // ========================================================================

    @Nested
    @DisplayName("Building from Sources (Positive)")
    class BuildingFromSourcesPositiveTests {

        @Test
        @DisplayName("Should configure fromScenarioRegistry source")
        void testFromScenarioRegistry() {
            logger.info("=== Testing fromScenarioRegistry() ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addClasspathPrefix("builder-test/")
                .fromScenarioRegistry("builder-test/builder-registry.yaml");

            // Should not throw - configuration is deferred to build()
            assertNotNull(builder);
        }

        @Test
        @DisplayName("Should configure fromFile source")
        void testFromFile() {
            logger.info("=== Testing fromFile() ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .fromFile("test-config.yaml");

            // Should not throw - configuration is deferred to build()
            assertNotNull(builder);
        }

        @Test
        @DisplayName("Should build engine from classpath scenario registry")
        void testBuildFromClasspathRegistry() throws Exception {
            logger.info("=== Testing build from classpath registry ===");

            RulesEngine engine = RulesEngine.builder()
                .addClasspathPrefix("builder-test/")
                .fromScenarioRegistry("builder-test/builder-registry.yaml")
                .build();

            assertNotNull(engine, "Engine should not be null");
            
            Map<String, ScenarioConfiguration> scenarios = engine.getScenarioRegistry();
            assertNotNull(scenarios, "Scenario registry should not be null");
            logger.info("Built engine with {} scenarios", scenarios.size());
        }

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should build engine from filesystem file with search paths")
        void testBuildFromFilesystemWithSearchPaths() throws Exception {
            logger.info("=== Testing build from filesystem with search paths ===");

            // Create test config in temp directory
            Path configDir = tempDir.resolve("configs");
            Files.createDirectories(configDir);
            
            String configYaml = """
                metadata:
                  id: "builder-test-config"
                  name: "Builder Test Config"
                  type: "rule-config"
                  description: "Configuration for RulesEngine.Builder tests"
                  version: "1.0"
                
                rules:
                  - id: "test-rule"
                    name: "Test Rule"
                    condition: "true"
                    message: "Test rule passed"
                    severity: "INFO"
                """;
            Files.writeString(configDir.resolve("test-rules.yaml"), configYaml);

            RulesEngine engine = RulesEngine.builder()
                .addSearchPath(configDir.toString())
                .fromFile("test-rules.yaml")
                .build();

            assertNotNull(engine, "Engine should not be null");
        }
    }

    // ========================================================================
    // NEGATIVE TESTS: Invalid Configurations
    // ========================================================================

    @Nested
    @DisplayName("Invalid Configurations (Negative)")
    class InvalidConfigurationsNegativeTests {

        @Test
        @DisplayName("Should throw when building without source")
        void testBuildWithoutSource() {
            logger.info("=== Testing build without source ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPath("/some/path");

            assertThrows(IllegalStateException.class, () -> {
                builder.build();
            }, "Should throw when no source is configured");
        }

        @Test
        @DisplayName("Should throw when source file not found")
        void testBuildWithNonExistentFile() {
            logger.info("=== Testing build with non-existent file ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .fromFile("/definitely/does/not/exist/config.yaml");

            assertThrows(YamlConfigurationException.class, () -> {
                builder.build();
            }, "Should throw when file not found");
        }

        @Test
        @DisplayName("Should handle null search path gracefully")
        void testNullSearchPath() {
            logger.info("=== Testing null search path ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPath(null);

            assertEquals(0, builder.getSearchPaths().size(), "Null path should be ignored");
        }

        @Test
        @DisplayName("Should handle empty search path gracefully")
        void testEmptySearchPath() {
            logger.info("=== Testing empty search path ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPath("")
                .addSearchPath("   ");

            assertEquals(0, builder.getSearchPaths().size(), "Empty paths should be ignored");
        }

        @Test
        @DisplayName("Should handle null classpath prefix gracefully")
        void testNullClasspathPrefix() {
            logger.info("=== Testing null classpath prefix ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addClasspathPrefix(null);

            assertEquals(0, builder.getClasspathPrefixes().size(), "Null prefix should be ignored");
        }

        @Test
        @DisplayName("Should handle null context key gracefully")
        void testNullContextKey() {
            logger.info("=== Testing null context key ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .withContext(null, "value");

            assertEquals(0, builder.getContextVariables().size(), "Null key should be ignored");
        }

        @Test
        @DisplayName("Should handle null context map gracefully")
        void testNullContextMap() {
            logger.info("=== Testing null context map ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .withContext((Map<String, Object>) null);

            assertEquals(0, builder.getContextVariables().size(), "Null map should be ignored");
        }

        @Test
        @DisplayName("Should handle null varargs in addSearchPaths")
        void testNullVarargsSearchPaths() {
            logger.info("=== Testing null varargs in addSearchPaths ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPaths((String[]) null);

            assertEquals(0, builder.getSearchPaths().size(), "Null varargs should be handled");
        }

        @Test
        @DisplayName("Should handle null collection in addSearchPaths")
        void testNullCollectionSearchPaths() {
            logger.info("=== Testing null collection in addSearchPaths ===");

            RulesEngine.Builder builder = RulesEngine.builder()
                .addSearchPaths((java.util.Collection<String>) null);

            assertEquals(0, builder.getSearchPaths().size(), "Null collection should be handled");
        }

        @Test
        @DisplayName("Should throw for invalid scenario registry format")
        void testInvalidRegistryFormat() {
            logger.info("=== Testing invalid registry format ===");

            // This uses a file that exists but is not a valid registry format
            RulesEngine.Builder builder = RulesEngine.builder()
                .fromScenarioRegistry("search-path-test/scenarios/scenario-a.yaml"); // This is a config, not registry

            // Should throw when building since file is not a valid registry
            assertThrows(YamlConfigurationException.class, () -> {
                builder.build();
            }, "Should throw for invalid registry format");
        }
    }

    // ========================================================================
    // POSITIVE TESTS: Environment Variable Precedence
    // ========================================================================

    @Nested
    @DisplayName("Environment Variable Precedence (Positive)")
    class EnvVarPrecedenceTests {

        @Test
        @DisplayName("Context variables should take precedence over env vars")
        void testContextPrecedenceOverEnv() {
            logger.info("=== Testing context precedence over env ===");

            // System property should be overridden by context
            String propertyName = "apex.test.precedence.var";
            String originalValue = System.getProperty(propertyName);
            System.setProperty(propertyName, "from-system");

            try {
                RulesEngine.Builder builder = RulesEngine.builder()
                    .withContext(propertyName, "from-context")
                    .addSearchPath("${" + propertyName + "}/configs");

                List<String> paths = builder.getSearchPaths();
                assertEquals("from-context/configs", paths.get(0), 
                    "Context should take precedence over system property");

            } finally {
                if (originalValue != null) {
                    System.setProperty(propertyName, originalValue);
                } else {
                    System.clearProperty(propertyName);
                }
            }
        }
    }
}
