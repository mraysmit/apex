/*
 * Copyright (c) 2025 Mars Development Team
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
package dev.mars.apex.core.engine;

import dev.mars.apex.core.config.ScenarioRegistryLoader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;


import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for environment-based configuration of search paths.
 * 
 * <p>This test class validates:
 * <ul>
 *   <li>System property configuration for search paths</li>
 *   <li>Environment variable configuration for search paths</li>
 *   <li>Multiple path parsing with proper delimiters</li>
 *   <li>Whitespace handling in path strings</li>
 *   <li>Empty and null value handling</li>
 *   <li>Path normalization across operating systems</li>
 * </ul>
 * 
 * @author Mars Development Team
 * @since 2.1
 */
@DisplayName("Environment Configuration Tests")
class EnvironmentConfigurationTest {

    // System property names
    private static final String PROP_SEARCH_PATHS = "apex.config.searchPaths";
    private static final String PROP_CLASSPATH_PREFIXES = "apex.config.classpathPrefixes";
    
    // Store original values to restore after tests
    private String originalSearchPaths;
    private String originalClasspathPrefixes;
    
    @BeforeEach
    void setUp() {
        // Save original system property values
        originalSearchPaths = System.getProperty(PROP_SEARCH_PATHS);
        originalClasspathPrefixes = System.getProperty(PROP_CLASSPATH_PREFIXES);
        
        // Clear properties for clean test state
        System.clearProperty(PROP_SEARCH_PATHS);
        System.clearProperty(PROP_CLASSPATH_PREFIXES);
    }
    
    @AfterEach
    void tearDown() {
        // Restore original values
        if (originalSearchPaths != null) {
            System.setProperty(PROP_SEARCH_PATHS, originalSearchPaths);
        } else {
            System.clearProperty(PROP_SEARCH_PATHS);
        }
        
        if (originalClasspathPrefixes != null) {
            System.setProperty(PROP_CLASSPATH_PREFIXES, originalClasspathPrefixes);
        } else {
            System.clearProperty(PROP_CLASSPATH_PREFIXES);
        }
    }

    // ==================== System Property Tests ====================
    
    @Nested
    @DisplayName("System Property Configuration Tests")
    class SystemPropertyTests {
        
        @Test
        @DisplayName("Should read single filesystem search path from system property")
        void testSingleSearchPathFromSystemProperty() {
            System.setProperty(PROP_SEARCH_PATHS, "/etc/apex/configs");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertTrue(paths.contains("/etc/apex/configs"),
                "Should contain the configured search path");
        }
        
        @Test
        @DisplayName("Should read multiple filesystem search paths separated by semicolon")
        void testMultipleSearchPathsWithSemicolon() {
            System.setProperty(PROP_SEARCH_PATHS, "/path/one;/path/two;/path/three");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertTrue(paths.contains("/path/one"), "Should contain first path");
            assertTrue(paths.contains("/path/two"), "Should contain second path");
            assertTrue(paths.contains("/path/three"), "Should contain third path");
        }
        
        @Test
        @DisplayName("Should read single classpath prefix from system property")
        void testSingleClasspathPrefixFromSystemProperty() {
            System.setProperty(PROP_CLASSPATH_PREFIXES, "apex/scenarios/");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> prefixes = loader.getClasspathPrefixes();
            
            assertTrue(prefixes.contains("apex/scenarios/"),
                "Should contain the configured classpath prefix");
        }
        
        @Test
        @DisplayName("Should read multiple classpath prefixes separated by semicolon")
        void testMultipleClasspathPrefixesWithSemicolon() {
            System.setProperty(PROP_CLASSPATH_PREFIXES, "config/;rules/;scenarios/");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> prefixes = loader.getClasspathPrefixes();
            
            assertTrue(prefixes.contains("config/"), "Should contain first prefix");
            assertTrue(prefixes.contains("rules/"), "Should contain second prefix");
            assertTrue(prefixes.contains("scenarios/"), "Should contain third prefix");
        }
        
        @Test
        @DisplayName("Should handle Windows-style paths with semicolon separator")
        void testWindowsStylePathsWithSemicolon() {
            System.setProperty(PROP_SEARCH_PATHS, "C:\\apex\\configs;D:\\shared\\rules");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertTrue(paths.contains("C:\\apex\\configs"), "Should contain Windows C: path");
            assertTrue(paths.contains("D:\\shared\\rules"), "Should contain Windows D: path");
        }
    }
    
    // ==================== Whitespace Handling Tests ====================
    
    @Nested
    @DisplayName("Whitespace Handling Tests")
    class WhitespaceHandlingTests {
        
        @Test
        @DisplayName("Should trim whitespace from paths")
        void testTrimWhitespaceFromPaths() {
            System.setProperty(PROP_SEARCH_PATHS, "  /path/one  ;  /path/two  ");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertTrue(paths.contains("/path/one"), 
                "Should contain trimmed first path");
            assertTrue(paths.contains("/path/two"), 
                "Should contain trimmed second path");
            assertFalse(paths.stream().anyMatch(p -> p.startsWith(" ") || p.endsWith(" ")),
                "No paths should have leading or trailing whitespace");
        }
        
        @Test
        @DisplayName("Should ignore empty paths between separators")
        void testIgnoreEmptyPathsBetweenSeparators() {
            System.setProperty(PROP_SEARCH_PATHS, "/path/one;;/path/two;;;/path/three");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertEquals(3, paths.size(), "Should only have 3 valid paths");
            assertFalse(paths.contains(""), "Should not contain empty paths");
        }
        
        @Test
        @DisplayName("Should ignore whitespace-only paths")
        void testIgnoreWhitespaceOnlyPaths() {
            System.setProperty(PROP_SEARCH_PATHS, "/path/one;   ;/path/two");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertEquals(2, paths.size(), "Should only have 2 valid paths");
            assertTrue(paths.contains("/path/one"), "Should contain first path");
            assertTrue(paths.contains("/path/two"), "Should contain second path");
        }
    }
    
    // ==================== Empty/Null Value Tests ====================
    
    @Nested
    @DisplayName("Empty and Null Value Tests")
    class EmptyNullValueTests {
        
        @Test
        @DisplayName("Should return empty list when no system property is set")
        void testEmptyListWhenNoPropertySet() {
            // Ensure property is not set
            System.clearProperty(PROP_SEARCH_PATHS);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertNotNull(paths, "Paths list should not be null");
            assertTrue(paths.isEmpty(), "Paths list should be empty when no property set");
        }
        
        @Test
        @DisplayName("Should return empty list when property is empty string")
        void testEmptyListWhenPropertyIsEmpty() {
            System.setProperty(PROP_SEARCH_PATHS, "");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertNotNull(paths, "Paths list should not be null");
            assertTrue(paths.isEmpty(), "Paths list should be empty when property is empty");
        }
        
        @Test
        @DisplayName("Should return empty list when property is only separators")
        void testEmptyListWhenPropertyIsOnlySeparators() {
            System.setProperty(PROP_SEARCH_PATHS, ";;;");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertNotNull(paths, "Paths list should not be null");
            assertTrue(paths.isEmpty(), "Paths list should be empty when property is only separators");
        }
    }
    
    // ==================== Path Normalization Tests ====================
    
    @Nested
    @DisplayName("Path Normalization Tests")
    class PathNormalizationTests {
        
        @Test
        @DisplayName("Should handle mixed forward and backward slashes")
        void testMixedSlashHandling() {
            System.setProperty(PROP_SEARCH_PATHS, "/path/to\\config;C:\\users/shared/rules");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertEquals(2, paths.size(), "Should parse both paths");
        }
        
        @Test
        @DisplayName("Should preserve trailing slashes")
        void testPreserveTrailingSlashes() {
            System.setProperty(PROP_CLASSPATH_PREFIXES, "config/;rules/");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> prefixes = loader.getClasspathPrefixes();
            
            assertTrue(prefixes.stream().allMatch(p -> p.endsWith("/")),
                "Should preserve trailing slashes on classpath prefixes");
        }
        
        @Test
        @DisplayName("Should handle UNC paths on Windows")
        void testUncPathHandling() {
            System.setProperty(PROP_SEARCH_PATHS, "\\\\server\\share\\configs;/local/path");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            List<String> paths = loader.getSearchPaths();
            
            assertEquals(2, paths.size(), "Should parse UNC and local paths");
            assertTrue(paths.contains("\\\\server\\share\\configs"), 
                "Should preserve UNC path");
        }
    }
    
    // ==================== Programmatic Override Tests ====================
    
    @Nested
    @DisplayName("Programmatic Override Tests")
    class ProgrammaticOverrideTests {
        
        @Test
        @DisplayName("Should allow programmatic paths to override system properties")
        void testProgrammaticOverride() {
            System.setProperty(PROP_SEARCH_PATHS, "/system/path");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.addSearchPath("/programmatic/path");
            
            List<String> paths = loader.getSearchPaths();
            
            assertTrue(paths.contains("/system/path"), 
                "Should still contain system property path");
            assertTrue(paths.contains("/programmatic/path"), 
                "Should also contain programmatically added path");
        }
        
        @Test
        @DisplayName("Should allow setting paths that replace system property paths")
        void testSetPathsReplacesAll() {
            System.setProperty(PROP_SEARCH_PATHS, "/system/path");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of("/new/path/one", "/new/path/two"));
            
            List<String> paths = loader.getSearchPaths();
            
            assertFalse(paths.contains("/system/path"), 
                "Should not contain system property path after setSearchPaths");
            assertTrue(paths.contains("/new/path/one"), 
                "Should contain first new path");
            assertTrue(paths.contains("/new/path/two"), 
                "Should contain second new path");
        }
        
        @Test
        @DisplayName("Should clear all paths when setting empty list")
        void testClearPathsWithEmptyList() {
            System.setProperty(PROP_SEARCH_PATHS, "/system/path");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.addSearchPath("/extra/path");
            loader.setSearchPaths(List.of());
            
            List<String> paths = loader.getSearchPaths();
            
            assertTrue(paths.isEmpty(), "Paths should be empty after setting empty list");
        }
    }
    
    // ==================== Combined Configuration Tests ====================
    
    @Nested
    @DisplayName("Combined Configuration Tests")
    class CombinedConfigurationTests {
        
        @Test
        @DisplayName("Should combine filesystem and classpath configurations")
        void testCombinedFilesystemAndClasspath() {
            System.setProperty(PROP_SEARCH_PATHS, "/etc/apex;/opt/apex");
            System.setProperty(PROP_CLASSPATH_PREFIXES, "apex/;config/");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            
            List<String> fsPaths = loader.getSearchPaths();
            List<String> cpPrefixes = loader.getClasspathPrefixes();
            
            assertEquals(2, fsPaths.size(), "Should have 2 filesystem paths");
            assertEquals(2, cpPrefixes.size(), "Should have 2 classpath prefixes");
        }
        
        @Test
        @DisplayName("Should independently manage filesystem and classpath paths")
        void testIndependentFilesystemAndClasspath() {
            System.setProperty(PROP_SEARCH_PATHS, "/filesystem/path");
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.addClasspathPrefix("classpath/prefix/");
            
            List<String> fsPaths = loader.getSearchPaths();
            List<String> cpPrefixes = loader.getClasspathPrefixes();
            
            assertTrue(fsPaths.contains("/filesystem/path"), 
                "Should have filesystem path from property");
            assertTrue(cpPrefixes.contains("classpath/prefix/"), 
                "Should have programmatically added classpath prefix");
        }
    }
}
