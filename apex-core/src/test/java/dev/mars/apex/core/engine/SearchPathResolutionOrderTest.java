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

import dev.mars.apex.core.config.yaml.ScenarioRegistryLoader;
import org.junit.jupiter.api.*;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for search path resolution order and precedence.
 * 
 * <p>The resolution order from highest to lowest priority is:
 * <ol>
 *   <li>Registry-level search paths (specified in registry YAML)</li>
 *   <li>Programmatic search paths (added via API)</li>
 *   <li>System property search paths</li>
 *   <li>Environment variable search paths</li>
 *   <li>Default resolution (relative to source file)</li>
 * </ol>
 * 
 * <p>This test class validates that files are found using the correct
 * precedence when the same file exists in multiple locations.
 * 
 * @author Mars Development Team
 * @since 2.1
 */
@DisplayName("Search Path Resolution Order Tests")
class SearchPathResolutionOrderTest {

    private static final String PROP_SEARCH_PATHS = "apex.config.searchPaths";
    
    @TempDir
    Path tempDir;
    
    private String originalSearchPaths;
    
    @BeforeEach
    void setUp() {
        originalSearchPaths = System.getProperty(PROP_SEARCH_PATHS);
        System.clearProperty(PROP_SEARCH_PATHS);
    }
    
    @AfterEach
    void tearDown() {
        if (originalSearchPaths != null) {
            System.setProperty(PROP_SEARCH_PATHS, originalSearchPaths);
        } else {
            System.clearProperty(PROP_SEARCH_PATHS);
        }
    }

    // ==================== Resolution Precedence Tests ====================
    
    @Nested
    @DisplayName("Resolution Precedence Tests")
    class ResolutionPrecedenceTests {
        
        @Test
        @DisplayName("Should find file in first search path before subsequent paths")
        void testFirstPathTakesPrecedence() throws IOException {
            // Create two directories with same-named file
            Path path1 = tempDir.resolve("path1");
            Path path2 = tempDir.resolve("path2");
            Files.createDirectories(path1);
            Files.createDirectories(path2);
            
            String content1 = createScenarioYaml("scenario-from-path1", "From Path 1");
            String content2 = createScenarioYaml("scenario-from-path2", "From Path 2");
            
            Files.writeString(path1.resolve("test-scenario.yaml"), content1);
            Files.writeString(path2.resolve("test-scenario.yaml"), content2);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(path1.toString(), path2.toString()));
            
            // The first path should take precedence
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths("test-scenario.yaml", List.of(), List.of());
            
            assertNotNull(resolved, "Should resolve the file");
            assertEquals(ScenarioRegistryLoader.ResolvedPath.Type.FILESYSTEM, resolved.type(),
                "Should be filesystem resolution");
            assertTrue(resolved.path().contains("path1"), 
                "Should resolve from first path, not second");
        }
        
        @Test
        @DisplayName("Should fall back to second path if file not in first")
        void testFallbackToSecondPath() throws IOException {
            Path path1 = tempDir.resolve("path1");
            Path path2 = tempDir.resolve("path2");
            Files.createDirectories(path1);
            Files.createDirectories(path2);
            
            // Only create file in second path
            String content = createScenarioYaml("scenario-from-path2", "From Path 2");
            Files.writeString(path2.resolve("unique-scenario.yaml"), content);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(path1.toString(), path2.toString()));
            
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths("unique-scenario.yaml", List.of(), List.of());
            
            assertNotNull(resolved, "Should resolve the file");
            assertTrue(resolved.path().contains("path2"), 
                "Should find file in second path when not in first");
        }
        
        @Test
        @DisplayName("Should prefer registry paths over global paths")
        void testRegistryPathsOverGlobalPaths() throws IOException {
            Path globalPath = tempDir.resolve("global");
            Path registryPath = tempDir.resolve("registry");
            Files.createDirectories(globalPath);
            Files.createDirectories(registryPath);
            
            String globalContent = createScenarioYaml("global-version", "Global Version");
            String registryContent = createScenarioYaml("registry-version", "Registry Version");
            
            Files.writeString(globalPath.resolve("config.yaml"), globalContent);
            Files.writeString(registryPath.resolve("config.yaml"), registryContent);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(globalPath.toString()));
            
            // Registry paths passed as parameter should take precedence
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "config.yaml", 
                    List.of(registryPath.toString()),  // Registry paths
                    List.of()  // Classpath prefixes
                );
            
            assertNotNull(resolved, "Should resolve the file");
            assertTrue(resolved.path().contains("registry"), 
                "Registry path should take precedence over global path");
        }
    }
    
    // ==================== Filesystem vs Classpath Tests ====================
    
    @Nested
    @DisplayName("Filesystem vs Classpath Resolution Tests")
    class FilesystemVsClasspathTests {
        
        @Test
        @DisplayName("Should prefer filesystem over classpath when both exist")
        void testFilesystemOverClasspath() throws IOException {
            Path fsPath = tempDir.resolve("filesystem");
            Files.createDirectories(fsPath);
            
            String fsContent = createScenarioYaml("fs-version", "Filesystem Version");
            Files.writeString(fsPath.resolve("shared-config.yaml"), fsContent);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(fsPath.toString()));
            
            // Also add classpath prefix - but filesystem should win
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "shared-config.yaml", 
                    List.of(),
                    List.of("search-path-test/")  // Classpath prefix
                );
            
            assertNotNull(resolved, "Should resolve the file");
            assertEquals(ScenarioRegistryLoader.ResolvedPath.Type.FILESYSTEM, resolved.type(),
                "Filesystem should take precedence over classpath");
        }
        
        @Test
        @DisplayName("Should fall back to classpath when filesystem path not found")
        void testFallbackToClasspath() {
            // No filesystem paths, only classpath
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "scenario-a.yaml",
                    List.of(),
                    List.of("search-path-test/scenarios/")
                );
            
            assertNotNull(resolved, "Should resolve from classpath");
            assertEquals(ScenarioRegistryLoader.ResolvedPath.Type.CLASSPATH, resolved.type(),
                "Should use classpath when filesystem not found");
        }
    }
    
    // ==================== Edge Case Resolution Tests ====================
    
    @Nested
    @DisplayName("Edge Case Resolution Tests")
    class EdgeCaseResolutionTests {
        
        @Test
        @DisplayName("Should return null when file not found in any path")
        void testNotFoundReturnsNull() {
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(tempDir.toString()));
            
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "nonexistent-file.yaml",
                    List.of(),
                    List.of()
                );
            
            assertNull(resolved, "Should return null when file not found");
        }
        
        @Test
        @DisplayName("Should handle absolute paths directly")
        void testAbsolutePathHandling() throws IOException {
            Path configFile = tempDir.resolve("absolute-config.yaml");
            String content = createScenarioYaml("absolute", "Absolute Path Test");
            Files.writeString(configFile, content);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            
            // Absolute path should be used directly, not searched
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    configFile.toString(),  // Absolute path
                    List.of(),
                    List.of()
                );
            
            assertNotNull(resolved, "Should resolve absolute path");
            assertEquals(ScenarioRegistryLoader.ResolvedPath.Type.FILESYSTEM, resolved.type(),
                "Absolute path should be filesystem type");
        }
        
        @Test
        @DisplayName("Should handle relative paths with subdirectories")
        void testRelativePathsWithSubdirs() throws IOException {
            Path basePath = tempDir.resolve("base");
            Path subDir = basePath.resolve("subdir").resolve("configs");
            Files.createDirectories(subDir);
            
            String content = createScenarioYaml("subdir-config", "Subdir Config");
            Files.writeString(subDir.resolve("nested.yaml"), content);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(basePath.toString()));
            
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "subdir/configs/nested.yaml",
                    List.of(),
                    List.of()
                );
            
            assertNotNull(resolved, "Should resolve nested relative path");
        }
        
        @Test
        @DisplayName("Should handle paths with special characters")
        void testPathsWithSpecialCharacters() throws IOException {
            // Create directory with space in name
            Path specialDir = tempDir.resolve("path with spaces");
            Files.createDirectories(specialDir);
            
            String content = createScenarioYaml("special", "Special Path");
            Files.writeString(specialDir.resolve("config.yaml"), content);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(specialDir.toString()));
            
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "config.yaml",
                    List.of(),
                    List.of()
                );
            
            assertNotNull(resolved, "Should handle paths with spaces");
        }
        
        @Test
        @DisplayName("Should ignore non-existent search paths gracefully")
        void testIgnoreNonExistentSearchPaths() throws IOException {
            Path existingPath = tempDir.resolve("existing");
            Files.createDirectories(existingPath);
            
            String content = createScenarioYaml("test", "Test");
            Files.writeString(existingPath.resolve("config.yaml"), content);
            
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            loader.setSearchPaths(List.of(
                "/nonexistent/path/one",
                existingPath.toString(),
                "/nonexistent/path/two"
            ));
            
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "config.yaml",
                    List.of(),
                    List.of()
                );
            
            assertNotNull(resolved, "Should skip non-existent paths and find file");
        }
    }
    
    // ==================== Multiple Classpath Prefix Tests ====================
    
    @Nested
    @DisplayName("Multiple Classpath Prefix Tests")
    class MultipleClasspathPrefixTests {
        
        @Test
        @DisplayName("Should search multiple classpath prefixes in order")
        void testMultipleClasspathPrefixOrder() {
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            
            // scenario-a.yaml exists in search-path-test/scenarios/
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "scenario-a.yaml",
                    List.of(),
                    List.of("nonexistent/", "search-path-test/scenarios/", "another/")
                );
            
            assertNotNull(resolved, "Should find in second classpath prefix");
            assertEquals(ScenarioRegistryLoader.ResolvedPath.Type.CLASSPATH, resolved.type());
        }
        
        @Test
        @DisplayName("Should return null when file not in any classpath prefix")
        void testNotFoundInAnyClasspathPrefix() {
            ScenarioRegistryLoader loader = new ScenarioRegistryLoader();
            
            ScenarioRegistryLoader.ResolvedPath resolved = 
                loader.resolveConfigFileWithSearchPaths(
                    "definitely-not-exists.yaml",
                    List.of(),
                    List.of("prefix1/", "prefix2/", "prefix3/")
                );
            
            assertNull(resolved, "Should return null when not in any prefix");
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Creates a minimal valid scenario YAML content.
     */
    private String createScenarioYaml(String scenarioId, String description) {
        return String.format("""
            metadata:
              type: "scenario"
              version: "1.0"
              description: "%s"
            
            scenario:
              id: "%s"
              name: "%s"
              description: "%s"
            
            rules:
              - id: "test-rule"
                condition: "true"
                message: "Test rule"
                severity: "INFO"
            """, description, scenarioId, scenarioId, description);
    }
}
