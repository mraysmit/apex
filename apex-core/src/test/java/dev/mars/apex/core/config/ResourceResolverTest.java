package dev.mars.apex.core.config;
import dev.mars.apex.core.config.exception.*;

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

import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResourceResolver.
 * 
 * <p>Tests the unified resource resolution abstraction that supports loading
 * resources from classpath and filesystem with configurable search paths.</p>
 * 
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Classpath resource resolution</li>
 *   <li>Filesystem resource resolution</li>
 *   <li>Relative path resolution with base paths</li>
 *   <li>Search path configuration</li>
 *   <li>Classpath prefix configuration</li>
 *   <li>Resolution strategies (classpath-first, filesystem-first, etc.)</li>
 *   <li>Error handling for missing resources</li>
 *   <li>Builder pattern</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see ResourceResolver
 */
@DisplayName("ResourceResolver Tests")
class ResourceResolverTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceResolverTest.class);

    // Classpath resource path (uses existing test resources)
    private static final String CLASSPATH_RESOURCE = "scenario/test-registry.yaml";
    private static final String CLASSPATH_RESOURCE_NESTED = "scenario/basic-validation-scenario.yaml";

    private ResourceResolver resolver;

    @BeforeEach
    void setUp() {
        logger.info("Setting up ResourceResolverTest");
        resolver = new ResourceResolver();
    }

    // ========================================================================
    // Classpath Resolution Tests
    // ========================================================================

    @Nested
    @DisplayName("Classpath Resolution Tests")
    class ClasspathResolutionTests {

        @Test
        @DisplayName("Should resolve resource from classpath root")
        void testResolveFromClasspathRootResource() throws Exception {
            logger.info("=== Testing resolve() from classpath root ===");

            try (InputStream is = resolver.resolve(CLASSPATH_RESOURCE)) {
                assertNotNull(is, "InputStream should not be null");
                
                String content = readStream(is);
                assertNotNull(content, "Content should not be null");
                assertTrue(content.contains("scenario-registry"), 
                          "Content should contain 'scenario-registry'");
                
                logger.info("[OK] Successfully resolved classpath resource: {}", CLASSPATH_RESOURCE);
            }
        }

        @Test
        @DisplayName("Should resolve resource from classpath with prefix")
        void testResolveFromClasspathWithPrefix() throws Exception {
            logger.info("=== Testing resolve() with classpath prefix ===");

            resolver.addClasspathPrefix("scenario/");

            try (InputStream is = resolver.resolve("test-registry.yaml")) {
                assertNotNull(is, "InputStream should not be null");
                
                String content = readStream(is);
                assertTrue(content.contains("scenario-registry"), 
                          "Content should contain 'scenario-registry'");
                
                logger.info("[OK] Successfully resolved with classpath prefix");
            }
        }

        @Test
        @DisplayName("Should resolve nested classpath resource")
        void testResolveNestedClasspathResource() throws Exception {
            logger.info("=== Testing nested classpath resource resolution ===");

            try (InputStream is = resolver.resolve(CLASSPATH_RESOURCE_NESTED)) {
                assertNotNull(is, "InputStream should not be null");
                
                String content = readStream(is);
                assertNotNull(content, "Content should not be null");
                
                logger.info("[OK] Successfully resolved nested classpath resource");
            }
        }

        @Test
        @DisplayName("Should use resolveFromClasspath for explicit classpath resolution")
        void testResolveFromClasspathExplicit() throws Exception {
            logger.info("=== Testing resolveFromClasspath() explicit method ===");

            try (InputStream is = resolver.resolveFromClasspath(CLASSPATH_RESOURCE)) {
                assertNotNull(is, "InputStream should not be null");
                logger.info("[OK] Successfully used explicit classpath resolution");
            }
        }

        @Test
        @DisplayName("Should throw exception for non-existent classpath resource")
        void testResolveFromClasspathNotFound() {
            logger.info("=== Testing resolveFromClasspath() with non-existent resource ===");

            ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> resolver.resolveFromClasspath("non-existent/resource.yaml")
            );

            assertTrue(exception.getMessage().contains("not found"),
                      "Exception message should indicate resource not found");
            logger.info("[OK] Correctly threw ResourceNotFoundException");
        }
    }

    // ========================================================================
    // Filesystem Resolution Tests
    // ========================================================================

    @Nested
    @DisplayName("Filesystem Resolution Tests")
    class FilesystemResolutionTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should resolve file from absolute filesystem path")
        void testResolveFromFilesystemAbsolutePath() throws Exception {
            logger.info("=== Testing resolve() from absolute filesystem path ===");

            // Create a test file
            Path testFile = tempDir.resolve("test-config.yaml");
            Files.writeString(testFile, "metadata:\n  id: test-file\n");

            // Use FILESYSTEM_ONLY to ensure we're testing filesystem
            resolver.setResolutionStrategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_ONLY);

            try (InputStream is = resolver.resolve(testFile.toString())) {
                assertNotNull(is, "InputStream should not be null");
                
                String content = readStream(is);
                assertTrue(content.contains("test-file"), 
                          "Content should contain 'test-file'");
                
                logger.info("[OK] Successfully resolved from absolute path: {}", testFile);
            }
        }

        @Test
        @DisplayName("Should resolve file from search path")
        void testResolveFromFilesystemWithSearchPaths() throws Exception {
            logger.info("=== Testing resolve() with filesystem search paths ===");

            // Create a test file in a subdirectory
            Path subDir = tempDir.resolve("configs");
            Files.createDirectories(subDir);
            Path testFile = subDir.resolve("app-config.yaml");
            Files.writeString(testFile, "metadata:\n  id: search-path-test\n");

            // Add the subdirectory as a search path
            resolver.addSearchPath(subDir.toString());
            resolver.setResolutionStrategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_ONLY);

            try (InputStream is = resolver.resolve("app-config.yaml")) {
                assertNotNull(is, "InputStream should not be null");
                
                String content = readStream(is);
                assertTrue(content.contains("search-path-test"), 
                          "Content should contain 'search-path-test'");
                
                logger.info("[OK] Successfully resolved from search path");
            }
        }

        @Test
        @DisplayName("Should use resolveFromFilesystem for explicit filesystem resolution")
        void testResolveFromFilesystemExplicit() throws Exception {
            logger.info("=== Testing resolveFromFilesystem() explicit method ===");

            // Create a test file
            Path testFile = tempDir.resolve("explicit-test.yaml");
            Files.writeString(testFile, "test: explicit");

            try (InputStream is = resolver.resolveFromFilesystem(testFile.toString())) {
                assertNotNull(is, "InputStream should not be null");
                logger.info("[OK] Successfully used explicit filesystem resolution");
            }
        }

        @Test
        @DisplayName("Should throw exception for non-existent filesystem path")
        void testResolveFromFilesystemNotFound() {
            logger.info("=== Testing resolveFromFilesystem() with non-existent file ===");

            ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> resolver.resolveFromFilesystem("/non/existent/path/file.yaml")
            );

            assertTrue(exception.getMessage().contains("not found"),
                      "Exception message should indicate file not found");
            logger.info("[OK] Correctly threw ResourceNotFoundException");
        }
    }

    // ========================================================================
    // Relative Path Resolution Tests
    // ========================================================================

    @Nested
    @DisplayName("Relative Path Resolution Tests")
    class RelativePathResolutionTests {

        @Test
        @DisplayName("Should resolve relative path with base path")
        void testResolveRelativePathWithBasePath() throws Exception {
            logger.info("=== Testing resolve() with relative path and base path ===");

            // The test-registry.yaml is in scenario/
            // basic-validation-scenario.yaml is also in scenario/
            try (InputStream is = resolver.resolve("basic-validation-scenario.yaml", "scenario/")) {
                assertNotNull(is, "InputStream should not be null");
                logger.info("[OK] Successfully resolved relative path with base");
            }
        }

        @Test
        @DisplayName("Should handle ./ prefix in reference")
        void testResolveRelativePathWithDotSlash() throws Exception {
            logger.info("=== Testing resolve() with ./ prefix ===");

            try (InputStream is = resolver.resolve("./basic-validation-scenario.yaml", "scenario/")) {
                assertNotNull(is, "InputStream should not be null");
                logger.info("[OK] Successfully handled ./ prefix");
            }
        }

        @Test
        @DisplayName("Should use resolveRelativePath utility method")
        void testResolveRelativePathUtility() {
            logger.info("=== Testing resolveRelativePath() utility ===");

            String resolved = resolver.resolveRelativePath("rules.yaml", "config/scenarios/");
            assertEquals("config/scenarios/rules.yaml", resolved);

            String resolvedWithDot = resolver.resolveRelativePath("./rules.yaml", "config/");
            assertEquals("config/rules.yaml", resolvedWithDot);

            String nullBase = resolver.resolveRelativePath("rules.yaml", null);
            assertEquals("rules.yaml", nullBase);

            logger.info("[OK] resolveRelativePath utility works correctly");
        }

        @Test
        @DisplayName("Should extract classpath base from resource path")
        void testGetClasspathBaseFromResourcePath() {
            logger.info("=== Testing getClasspathBase() ===");

            assertEquals("config/scenarios/", resolver.getClasspathBase("config/scenarios/registry.yaml"));
            assertEquals("", resolver.getClasspathBase("registry.yaml"));
            assertEquals("", resolver.getClasspathBase(""));
            assertEquals("", resolver.getClasspathBase(null));

            logger.info("[OK] getClasspathBase works correctly");
        }
    }

    // ========================================================================
    // Resolution Strategy Tests
    // ========================================================================

    @Nested
    @DisplayName("Resolution Strategy Tests")
    class ResolutionStrategyTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should use CLASSPATH_FIRST strategy by default")
        void testDefaultStrategyIsClasspathFirst() {
            logger.info("=== Testing default resolution strategy ===");

            assertEquals(ResourceResolver.ResolutionStrategy.CLASSPATH_FIRST, 
                        resolver.getResolutionStrategy());
            logger.info("[OK] Default strategy is CLASSPATH_FIRST");
        }

        @Test
        @DisplayName("Should resolve from classpath first with CLASSPATH_FIRST strategy")
        void testClasspathFirstStrategy() throws Exception {
            logger.info("=== Testing CLASSPATH_FIRST strategy ===");

            // Create a file on filesystem with same name as classpath resource
            Path testFile = tempDir.resolve("test-registry.yaml");
            Files.writeString(testFile, "metadata:\n  id: filesystem-version\n");
            resolver.addSearchPath(tempDir.toString());

            // With CLASSPATH_FIRST, should get the classpath version
            try (InputStream is = resolver.resolve("scenario/test-registry.yaml")) {
                String content = readStream(is);
                // Should be the classpath version which contains "scenario-registry"
                assertTrue(content.contains("scenario-registry"), 
                          "Should resolve from classpath first");
                logger.info("[OK] CLASSPATH_FIRST resolved from classpath");
            }
        }

        @Test
        @DisplayName("Should resolve from filesystem first with FILESYSTEM_FIRST strategy")
        void testFilesystemFirstStrategy() throws Exception {
            logger.info("=== Testing FILESYSTEM_FIRST strategy ===");

            // Create a file on filesystem
            Path configDir = tempDir.resolve("scenario");
            Files.createDirectories(configDir);
            Path testFile = configDir.resolve("test-registry.yaml");
            Files.writeString(testFile, "metadata:\n  id: filesystem-version\n");

            resolver.setResolutionStrategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_FIRST);
            resolver.addSearchPath(tempDir.toString());

            // With FILESYSTEM_FIRST, should get the filesystem version
            try (InputStream is = resolver.resolve("scenario/test-registry.yaml")) {
                String content = readStream(is);
                assertTrue(content.contains("filesystem-version"), 
                          "Should resolve from filesystem first");
                logger.info("[OK] FILESYSTEM_FIRST resolved from filesystem");
            }
        }

        @Test
        @DisplayName("Should only use classpath with CLASSPATH_ONLY strategy")
        void testClasspathOnlyStrategy() throws Exception {
            logger.info("=== Testing CLASSPATH_ONLY strategy ===");

            resolver.setResolutionStrategy(ResourceResolver.ResolutionStrategy.CLASSPATH_ONLY);

            // Should find classpath resource
            try (InputStream is = resolver.resolve(CLASSPATH_RESOURCE)) {
                assertNotNull(is);
                logger.info("[OK] CLASSPATH_ONLY found classpath resource");
            }

            // Should NOT find filesystem-only resource
            Path testFile = tempDir.resolve("filesystem-only.yaml");
            Files.writeString(testFile, "test: true");

            assertThrows(ResourceNotFoundException.class,
                () -> resolver.resolve(testFile.toString()));
            logger.info("[OK] CLASSPATH_ONLY correctly ignores filesystem");
        }

        @Test
        @DisplayName("Should only use filesystem with FILESYSTEM_ONLY strategy")
        void testFilesystemOnlyStrategy() throws Exception {
            logger.info("=== Testing FILESYSTEM_ONLY strategy ===");

            resolver.setResolutionStrategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_ONLY);

            // Should find filesystem resource
            Path testFile = tempDir.resolve("filesystem-only.yaml");
            Files.writeString(testFile, "test: true");

            try (InputStream is = resolver.resolve(testFile.toString())) {
                assertNotNull(is);
                logger.info("[OK] FILESYSTEM_ONLY found filesystem resource");
            }

            // Should NOT find classpath-only resource (that doesn't exist on filesystem)
            assertThrows(ResourceNotFoundException.class,
                () -> resolver.resolve(CLASSPATH_RESOURCE));
            logger.info("[OK] FILESYSTEM_ONLY correctly ignores classpath");
        }
    }

    // ========================================================================
    // Search Path and Prefix Configuration Tests
    // ========================================================================

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should add search path dynamically")
        void testAddSearchPathDynamically() {
            logger.info("=== Testing addSearchPath() ===");

            assertTrue(resolver.getSearchPaths().isEmpty(), "Should start with no search paths");

            resolver.addSearchPath("/etc/apex/configs");
            resolver.addSearchPath("/opt/apex");

            List<String> paths = resolver.getSearchPaths();
            assertEquals(2, paths.size());
            assertTrue(paths.contains("/etc/apex/configs"));
            assertTrue(paths.contains("/opt/apex"));

            logger.info("[OK] Search paths added correctly");
        }

        @Test
        @DisplayName("Should add classpath prefix dynamically")
        void testAddClasspathPrefixDynamically() {
            logger.info("=== Testing addClasspathPrefix() ===");

            assertTrue(resolver.getClasspathPrefixes().isEmpty(), "Should start with no prefixes");

            resolver.addClasspathPrefix("apex/");
            resolver.addClasspathPrefix("META-INF/apex");

            List<String> prefixes = resolver.getClasspathPrefixes();
            assertEquals(2, prefixes.size());
            assertTrue(prefixes.contains("apex/"));
            assertTrue(prefixes.contains("META-INF/apex/"));  // Should add trailing slash

            logger.info("[OK] Classpath prefixes added correctly");
        }

        @Test
        @DisplayName("Should set search paths replacing existing")
        void testSetSearchPaths() {
            logger.info("=== Testing setSearchPaths() ===");

            resolver.addSearchPath("/old/path");
            resolver.setSearchPaths(Arrays.asList("/new/path1", "/new/path2"));

            List<String> paths = resolver.getSearchPaths();
            assertEquals(2, paths.size());
            assertFalse(paths.contains("/old/path"));
            assertTrue(paths.contains("/new/path1"));

            logger.info("[OK] setSearchPaths replaced existing paths");
        }

        @Test
        @DisplayName("Should set classpath prefixes replacing existing")
        void testSetClasspathPrefixes() {
            logger.info("=== Testing setClasspathPrefixes() ===");

            resolver.addClasspathPrefix("old/");
            resolver.setClasspathPrefixes(Arrays.asList("new1/", "new2/"));

            List<String> prefixes = resolver.getClasspathPrefixes();
            assertEquals(2, prefixes.size());
            assertFalse(prefixes.contains("old/"));
            assertTrue(prefixes.contains("new1/"));

            logger.info("[OK] setClasspathPrefixes replaced existing prefixes");
        }

        @Test
        @DisplayName("Should search paths in order")
        void testMultipleSearchPathsOrder(@TempDir Path tempDir) throws Exception {
            logger.info("=== Testing search path order ===");

            // Create two directories with same-named file
            Path dir1 = tempDir.resolve("dir1");
            Path dir2 = tempDir.resolve("dir2");
            Files.createDirectories(dir1);
            Files.createDirectories(dir2);

            Files.writeString(dir1.resolve("config.yaml"), "source: dir1");
            Files.writeString(dir2.resolve("config.yaml"), "source: dir2");

            // Add dir1 first, then dir2
            resolver.addSearchPath(dir1.toString());
            resolver.addSearchPath(dir2.toString());
            resolver.setResolutionStrategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_ONLY);

            try (InputStream is = resolver.resolve("config.yaml")) {
                String content = readStream(is);
                assertTrue(content.contains("dir1"), "Should find file from first search path");
            }

            logger.info("[OK] Search paths checked in order");
        }
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw exception for null path")
        void testResolveNullPathThrowsException() {
            logger.info("=== Testing resolve() with null path ===");

            assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null));
            assertThrows(IllegalArgumentException.class, () -> resolver.resolve(""));
            assertThrows(IllegalArgumentException.class, () -> resolver.resolve("  "));

            logger.info("[OK] Correctly throws IllegalArgumentException for null/empty path");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for not found resource")
        void testResolveNotFoundThrowsException() {
            logger.info("=== Testing resolve() with non-existent resource ===");

            ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> resolver.resolve("completely/non/existent/resource.yaml")
            );

            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("not found"));

            logger.info("[OK] Correctly throws ResourceNotFoundException");
        }

        @Test
        @DisplayName("Should throw exception for null search path")
        void testAddNullSearchPathThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> resolver.addSearchPath(null));
            assertThrows(IllegalArgumentException.class, () -> resolver.addSearchPath(""));
        }

        @Test
        @DisplayName("Should throw exception for null classpath prefix")
        void testAddNullClasspathPrefixThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> resolver.addClasspathPrefix(null));
            assertThrows(IllegalArgumentException.class, () -> resolver.addClasspathPrefix(""));
        }
    }

    // ========================================================================
    // Exists Method Tests
    // ========================================================================

    @Nested
    @DisplayName("Exists Method Tests")
    class ExistsMethodTests {

        @Test
        @DisplayName("Should return true for existing classpath resource")
        void testExistsForClasspathResource() {
            logger.info("=== Testing exists() for classpath resource ===");

            assertTrue(resolver.exists(CLASSPATH_RESOURCE));
            assertFalse(resolver.exists("non/existent/resource.yaml"));

            logger.info("[OK] exists() works correctly for classpath");
        }

        @Test
        @DisplayName("Should return true for existing filesystem resource")
        void testExistsForFilesystemResource(@TempDir Path tempDir) throws Exception {
            logger.info("=== Testing exists() for filesystem resource ===");

            Path testFile = tempDir.resolve("exists-test.yaml");
            Files.writeString(testFile, "test: true");

            assertTrue(resolver.exists(testFile.toString()));
            assertFalse(resolver.exists(tempDir.resolve("not-exists.yaml").toString()));

            logger.info("[OK] exists() works correctly for filesystem");
        }

        @Test
        @DisplayName("Should return false for null or empty reference")
        void testExistsForNullOrEmpty() {
            assertFalse(resolver.exists(null));
            assertFalse(resolver.exists(""));
            assertFalse(resolver.exists("  "));
        }
    }

    // ========================================================================
    // Builder Pattern Tests
    // ========================================================================

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create resolver using builder")
        void testBuilderCreatesResolver() throws Exception {
            logger.info("=== Testing builder pattern ===");

            ResourceResolver builtResolver = ResourceResolver.builder()
                .strategy(ResourceResolver.ResolutionStrategy.CLASSPATH_FIRST)
                .addClasspathPrefix("scenario/")
                .build();

            assertNotNull(builtResolver);
            assertEquals(ResourceResolver.ResolutionStrategy.CLASSPATH_FIRST, 
                        builtResolver.getResolutionStrategy());
            assertTrue(builtResolver.getClasspathPrefixes().contains("scenario/"));

            // Should be able to resolve with the prefix
            try (InputStream is = builtResolver.resolve("test-registry.yaml")) {
                assertNotNull(is);
            }

            logger.info("[OK] Builder pattern works correctly");
        }

        @Test
        @DisplayName("Should configure all builder options")
        void testBuilderAllOptions(@TempDir Path tempDir) {
            logger.info("=== Testing builder with all options ===");

            ResourceResolver builtResolver = ResourceResolver.builder()
                .classLoader(getClass().getClassLoader())
                .strategy(ResourceResolver.ResolutionStrategy.FILESYSTEM_FIRST)
                .addSearchPath(tempDir.toString())
                .addSearchPath("/opt/apex")
                .addClasspathPrefix("config/")
                .addClasspathPrefix("META-INF/")
                .build();

            assertEquals(ResourceResolver.ResolutionStrategy.FILESYSTEM_FIRST, 
                        builtResolver.getResolutionStrategy());
            assertEquals(2, builtResolver.getSearchPaths().size());
            assertEquals(2, builtResolver.getClasspathPrefixes().size());

            logger.info("[OK] Builder configured all options correctly");
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String readStream(InputStream is) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
