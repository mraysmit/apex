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
package dev.mars.apex.engine.core;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for RulesEngineBuilder - fluent builder for RulesEngine configuration.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("RulesEngineBuilder Tests")
class RulesEngineBuilderTest {

    private static final Logger logger = LoggerFactory.getLogger(RulesEngineBuilderTest.class);

    private RulesEngineBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new RulesEngineBuilder();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create new builder instance")
        void shouldCreateNewBuilderInstance() {
            RulesEngineBuilder builder = new RulesEngineBuilder();
            
            assertNotNull(builder);
            assertTrue(builder.getSearchPaths().isEmpty());
            assertTrue(builder.getClasspathPrefixes().isEmpty());
            assertTrue(builder.getContextVariables().isEmpty());
            
            logger.info("[OK] Builder created with empty configuration");
        }
    }

    @Nested
    @DisplayName("Search Path Tests")
    class SearchPathTests {

        @Test
        @DisplayName("Should add single search path")
        void shouldAddSingleSearchPath() {
            builder.addSearchPath("/config/apex");
            
            List<String> paths = builder.getSearchPaths();
            assertEquals(1, paths.size());
            assertEquals("/config/apex", paths.get(0));
            
            logger.info("[OK] Single search path added");
        }

        @Test
        @DisplayName("Should add multiple search paths via varargs")
        void shouldAddMultipleSearchPathsViaVarargs() {
            builder.addSearchPaths("/config/apex", "/opt/apex", "/home/user/config");
            
            List<String> paths = builder.getSearchPaths();
            assertEquals(3, paths.size());
            assertTrue(paths.contains("/config/apex"));
            assertTrue(paths.contains("/opt/apex"));
            assertTrue(paths.contains("/home/user/config"));
            
            logger.info("[OK] Multiple search paths added via varargs");
        }

        @Test
        @DisplayName("Should add search paths from collection")
        void shouldAddSearchPathsFromCollection() {
            List<String> pathsList = Arrays.asList("/dir1", "/dir2", "/dir3");
            builder.addSearchPaths(pathsList);
            
            List<String> paths = builder.getSearchPaths();
            assertEquals(3, paths.size());
            
            logger.info("[OK] Search paths added from collection");
        }

        @Test
        @DisplayName("Should ignore null search path")
        void shouldIgnoreNullSearchPath() {
            builder.addSearchPath(null);
            
            assertTrue(builder.getSearchPaths().isEmpty());
            
            logger.info("[OK] Null search path ignored");
        }

        @Test
        @DisplayName("Should ignore empty search path")
        void shouldIgnoreEmptySearchPath() {
            builder.addSearchPath("");
            builder.addSearchPath("   ");
            
            assertTrue(builder.getSearchPaths().isEmpty());
            
            logger.info("[OK] Empty search paths ignored");
        }

        @Test
        @DisplayName("Should trim whitespace from search path")
        void shouldTrimWhitespaceFromSearchPath() {
            builder.addSearchPath("  /config/apex  ");
            
            List<String> paths = builder.getSearchPaths();
            assertEquals(1, paths.size());
            assertEquals("/config/apex", paths.get(0));
            
            logger.info("[OK] Whitespace trimmed from search path");
        }

        @Test
        @DisplayName("Should return unmodifiable list from getSearchPaths")
        void shouldReturnUnmodifiableListFromGetSearchPaths() {
            builder.addSearchPath("/config");
            
            List<String> paths = builder.getSearchPaths();
            
            assertThrows(UnsupportedOperationException.class, () -> paths.add("/new"));
            
            logger.info("[OK] Search paths list is unmodifiable");
        }

        @Test
        @DisplayName("Should support method chaining")
        void shouldSupportMethodChaining() {
            RulesEngineBuilder result = builder
                .addSearchPath("/path1")
                .addSearchPath("/path2")
                .addSearchPaths("/path3", "/path4");
            
            assertSame(builder, result);
            assertEquals(4, builder.getSearchPaths().size());
            
            logger.info("[OK] Method chaining supported for search paths");
        }

        @Test
        @DisplayName("Should handle null varargs array")
        void shouldHandleNullVarargsArray() {
            builder.addSearchPaths((String[]) null);
            
            assertTrue(builder.getSearchPaths().isEmpty());
            
            logger.info("[OK] Null varargs array handled");
        }

        @Test
        @DisplayName("Should handle null collection")
        void shouldHandleNullCollection() {
            builder.addSearchPaths((Collection<String>) null);
            
            assertTrue(builder.getSearchPaths().isEmpty());
            
            logger.info("[OK] Null collection handled");
        }
    }

    @Nested
    @DisplayName("Classpath Prefix Tests")
    class ClasspathPrefixTests {

        @Test
        @DisplayName("Should add classpath prefix")
        void shouldAddClasspathPrefix() {
            builder.addClasspathPrefix("apex/");
            
            List<String> prefixes = builder.getClasspathPrefixes();
            assertEquals(1, prefixes.size());
            assertEquals("apex/", prefixes.get(0));
            
            logger.info("[OK] Classpath prefix added");
        }

        @Test
        @DisplayName("Should add trailing slash to prefix if missing")
        void shouldAddTrailingSlashToPrefixIfMissing() {
            builder.addClasspathPrefix("apex");
            
            List<String> prefixes = builder.getClasspathPrefixes();
            assertEquals(1, prefixes.size());
            assertEquals("apex/", prefixes.get(0));
            
            logger.info("[OK] Trailing slash added to prefix");
        }

        @Test
        @DisplayName("Should not add extra slash if already present")
        void shouldNotAddExtraSlashIfAlreadyPresent() {
            builder.addClasspathPrefix("apex/");
            
            List<String> prefixes = builder.getClasspathPrefixes();
            assertEquals("apex/", prefixes.get(0));
            
            logger.info("[OK] No extra slash added when already present");
        }

        @Test
        @DisplayName("Should add multiple classpath prefixes")
        void shouldAddMultipleClasspathPrefixes() {
            builder.addClasspathPrefixes("apex/", "META-INF/apex/", "config/");
            
            List<String> prefixes = builder.getClasspathPrefixes();
            assertEquals(3, prefixes.size());
            
            logger.info("[OK] Multiple classpath prefixes added");
        }

        @Test
        @DisplayName("Should ignore null classpath prefix")
        void shouldIgnoreNullClasspathPrefix() {
            builder.addClasspathPrefix(null);
            
            assertTrue(builder.getClasspathPrefixes().isEmpty());
            
            logger.info("[OK] Null classpath prefix ignored");
        }

        @Test
        @DisplayName("Should ignore empty classpath prefix")
        void shouldIgnoreEmptyClasspathPrefix() {
            builder.addClasspathPrefix("");
            builder.addClasspathPrefix("   ");
            
            assertTrue(builder.getClasspathPrefixes().isEmpty());
            
            logger.info("[OK] Empty classpath prefixes ignored");
        }

        @Test
        @DisplayName("Should return unmodifiable list from getClasspathPrefixes")
        void shouldReturnUnmodifiableListFromGetClasspathPrefixes() {
            builder.addClasspathPrefix("apex/");
            
            List<String> prefixes = builder.getClasspathPrefixes();
            
            assertThrows(UnsupportedOperationException.class, () -> prefixes.add("new/"));
            
            logger.info("[OK] Classpath prefixes list is unmodifiable");
        }
    }

    @Nested
    @DisplayName("Context Variable Tests")
    class ContextVariableTests {

        @Test
        @DisplayName("Should add context variable")
        void shouldAddContextVariable() {
            builder.withContext("dbHost", "localhost");
            
            Map<String, Object> context = builder.getContextVariables();
            assertEquals(1, context.size());
            assertEquals("localhost", context.get("dbHost"));
            
            logger.info("[OK] Context variable added");
        }

        @Test
        @DisplayName("Should add multiple context variables individually")
        void shouldAddMultipleContextVariablesIndividually() {
            builder.withContext("host", "localhost")
                   .withContext("port", 5432)
                   .withContext("enabled", true);
            
            Map<String, Object> context = builder.getContextVariables();
            assertEquals(3, context.size());
            assertEquals("localhost", context.get("host"));
            assertEquals(5432, context.get("port"));
            assertEquals(true, context.get("enabled"));
            
            logger.info("[OK] Multiple context variables added individually");
        }

        @Test
        @DisplayName("Should add context variables from map")
        void shouldAddContextVariablesFromMap() {
            Map<String, Object> vars = new HashMap<>();
            vars.put("key1", "value1");
            vars.put("key2", 123);
            vars.put("key3", true);
            
            builder.withContext(vars);
            
            Map<String, Object> context = builder.getContextVariables();
            assertEquals(3, context.size());
            
            logger.info("[OK] Context variables added from map");
        }

        @Test
        @DisplayName("Should ignore null variable name")
        void shouldIgnoreNullVariableName() {
            builder.withContext(null, "value");
            
            assertTrue(builder.getContextVariables().isEmpty());
            
            logger.info("[OK] Null variable name ignored");
        }

        @Test
        @DisplayName("Should ignore empty variable name")
        void shouldIgnoreEmptyVariableName() {
            builder.withContext("", "value");
            builder.withContext("   ", "value");
            
            assertTrue(builder.getContextVariables().isEmpty());
            
            logger.info("[OK] Empty variable names ignored");
        }

        @Test
        @DisplayName("Should allow null value")
        void shouldAllowNullValue() {
            builder.withContext("nullableKey", null);
            
            Map<String, Object> context = builder.getContextVariables();
            assertEquals(1, context.size());
            assertTrue(context.containsKey("nullableKey"));
            assertNull(context.get("nullableKey"));
            
            logger.info("[OK] Null value allowed for context variable");
        }

        @Test
        @DisplayName("Should trim whitespace from variable name")
        void shouldTrimWhitespaceFromVariableName() {
            builder.withContext("  key  ", "value");
            
            Map<String, Object> context = builder.getContextVariables();
            assertTrue(context.containsKey("key"));
            assertEquals("value", context.get("key"));
            
            logger.info("[OK] Whitespace trimmed from variable name");
        }

        @Test
        @DisplayName("Should return unmodifiable map from getContextVariables")
        void shouldReturnUnmodifiableMapFromGetContextVariables() {
            builder.withContext("key", "value");
            
            Map<String, Object> context = builder.getContextVariables();
            
            assertThrows(UnsupportedOperationException.class, 
                        () -> context.put("newKey", "newValue"));
            
            logger.info("[OK] Context variables map is unmodifiable");
        }

        @Test
        @DisplayName("Should handle null map")
        void shouldHandleNullMap() {
            builder.withContext((Map<String, Object>) null);
            
            assertTrue(builder.getContextVariables().isEmpty());
            
            logger.info("[OK] Null map handled");
        }
    }

    @Nested
    @DisplayName("Source Configuration Tests")
    class SourceConfigurationTests {

        @Test
        @DisplayName("Should configure from scenario registry")
        void shouldConfigureFromScenarioRegistry() {
            RulesEngineBuilder result = builder.fromScenarioRegistry("path/to/registry.yaml");
            
            assertSame(builder, result);
            
            logger.info("[OK] Scenario registry source configured");
        }

        @Test
        @DisplayName("Should configure from file")
        void shouldConfigureFromFile() {
            RulesEngineBuilder result = builder.fromFile("path/to/config.yaml");
            
            assertSame(builder, result);
            
            logger.info("[OK] File source configured");
        }
    }

    @Nested
    @DisplayName("Build Validation Tests")
    class BuildValidationTests {

        @Test
        @DisplayName("Should throw if no source configured")
        void shouldThrowIfNoSourceConfigured() {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
            );
            
            assertTrue(exception.getMessage().contains("No source configured"));
            
            logger.info("[OK] Exception thrown when no source configured");
        }
    }

    @Nested
    @DisplayName("Fluent API Tests")
    class FluentApiTests {

        @Test
        @DisplayName("Should support complete fluent chain")
        void shouldSupportCompleteFluentChain() {
            RulesEngineBuilder result = builder
                .addSearchPath("/config")
                .addSearchPaths("/opt/apex", "/home/user")
                .addClasspathPrefix("apex/")
                .addClasspathPrefixes("META-INF/", "config/")
                .withContext("env", "production")
                .withContext(Collections.singletonMap("debug", false))
                .fromFile("rules.yaml");
            
            assertSame(builder, result);
            assertEquals(3, builder.getSearchPaths().size());
            assertEquals(3, builder.getClasspathPrefixes().size());
            assertEquals(2, builder.getContextVariables().size());
            
            logger.info("[OK] Complete fluent chain supported");
        }
    }

    @Nested
    @DisplayName("Environment Variable Expansion Tests")
    class EnvironmentVariableExpansionTests {

        @Test
        @DisplayName("Should expand context variables in path")
        void shouldExpandContextVariablesInPath() {
            builder.withContext("CONFIG_DIR", "/opt/apex")
                   .addSearchPath("${CONFIG_DIR}/rules");
            
            List<String> paths = builder.getSearchPaths();
            assertEquals(1, paths.size());
            assertEquals("/opt/apex/rules", paths.get(0));
            
            logger.info("[OK] Context variables expanded in path");
        }

        @Test
        @DisplayName("Should preserve path without variables")
        void shouldPreservePathWithoutVariables() {
            builder.addSearchPath("/config/apex");
            
            List<String> paths = builder.getSearchPaths();
            assertEquals("/config/apex", paths.get(0));
            
            logger.info("[OK] Path without variables preserved");
        }

        @Test
        @DisplayName("Should handle undefined variable")
        void shouldHandleUndefinedVariable() {
            builder.addSearchPath("${UNDEFINED_VAR}/rules");
            
            // Should not throw and should handle gracefully
            List<String> paths = builder.getSearchPaths();
            assertFalse(paths.isEmpty());
            
            logger.info("[OK] Undefined variable handled gracefully");
        }
    }
}
