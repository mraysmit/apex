package dev.mars.apex.yaml.manager.service;

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

import dev.mars.apex.yaml.manager.model.YamlConfigMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CatalogScanService classpath scanning functionality.
 *
 * <p>Phase 5 of the unified resource loading implementation.
 * Tests the new classpath scanning methods:</p>
 * <ul>
 *   <li>{@code scanClasspath(String classpathPrefix)}</li>
 *   <li>{@code scanClasspath(String classpathPrefix, ClassLoader classLoader)}</li>
 *   <li>{@code scanAll(List filesystemPaths, List classpathPrefixes)}</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see CatalogScanService
 */
@DisplayName("CatalogScanService Classpath Scanning Tests")
class CatalogScanServiceClasspathTest {

    private CatalogScanService catalogScanService;
    private YamlContentAnalyzer contentAnalyzer;
    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogScanService = new CatalogScanService();
        contentAnalyzer = new YamlContentAnalyzer();
        catalogService = new CatalogService();

        // Inject dependencies using reflection
        try {
            java.lang.reflect.Field contentAnalyzerField = CatalogScanService.class.getDeclaredField("contentAnalyzer");
            contentAnalyzerField.setAccessible(true);
            contentAnalyzerField.set(catalogScanService, contentAnalyzer);

            java.lang.reflect.Field catalogServiceField = CatalogScanService.class.getDeclaredField("catalogService");
            catalogServiceField.setAccessible(true);
            catalogServiceField.set(catalogScanService, catalogService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject dependencies", e);
        }
    }

    @Nested
    @DisplayName("scanClasspath() method tests")
    class ScanClasspathTests {

        @Test
        @DisplayName("Should handle null classpath prefix")
        void shouldHandleNullClasspathPrefix() {
            Map<String, Object> result = catalogScanService.scanClasspath(null);

            assertFalse((Boolean) result.get("success"));
            assertNotNull(result.get("error"));
            assertTrue(result.get("error").toString().contains("null or empty"));
        }

        @Test
        @DisplayName("Should handle empty classpath prefix")
        void shouldHandleEmptyClasspathPrefix() {
            Map<String, Object> result = catalogScanService.scanClasspath("");

            assertFalse((Boolean) result.get("success"));
            assertNotNull(result.get("error"));
            assertTrue(result.get("error").toString().contains("null or empty"));
        }

        @Test
        @DisplayName("Should handle whitespace-only classpath prefix")
        void shouldHandleWhitespaceClasspathPrefix() {
            Map<String, Object> result = catalogScanService.scanClasspath("   ");

            assertFalse((Boolean) result.get("success"));
            assertNotNull(result.get("error"));
            assertTrue(result.get("error").toString().contains("null or empty"));
        }

        @Test
        @DisplayName("Should scan classpath prefix that exists")
        void shouldScanExistingClasspathPrefix() {
            // Scan for test resources (might not find any YAML but should succeed)
            Map<String, Object> result = catalogScanService.scanClasspath("dev/mars/apex/");

            assertTrue((Boolean) result.get("success"));
            assertEquals("dev/mars/apex/", result.get("classpathPrefix"));
            assertNotNull(result.get("resourcesScanned"));
            assertNotNull(result.get("resourcesIndexed"));
            assertNotNull(result.get("durationMs"));
        }

        @Test
        @DisplayName("Should scan classpath prefix with leading slash normalization")
        void shouldNormalizeLeadingSlash() {
            // Prefix with leading slash should be normalized
            Map<String, Object> result = catalogScanService.scanClasspath("/test/prefix/");

            assertTrue((Boolean) result.get("success"));
            assertEquals("/test/prefix/", result.get("classpathPrefix")); // Original preserved in result
        }

        @Test
        @DisplayName("Should add trailing slash to prefix without one")
        void shouldAddTrailingSlash() {
            Map<String, Object> result = catalogScanService.scanClasspath("test/prefix");

            assertTrue((Boolean) result.get("success"));
            assertEquals("test/prefix", result.get("classpathPrefix")); // Original preserved in result
        }

        @Test
        @DisplayName("Should return scan statistics")
        void shouldReturnScanStatistics() {
            Map<String, Object> result = catalogScanService.scanClasspath("nonexistent/prefix/");

            assertTrue((Boolean) result.get("success"));
            assertTrue(result.containsKey("resourcesScanned"));
            assertTrue(result.containsKey("resourcesIndexed"));
            assertTrue(result.containsKey("errorCount"));
            assertTrue(result.containsKey("errors"));
            assertTrue(result.containsKey("durationMs"));
        }

        @Test
        @DisplayName("Should handle nonexistent classpath prefix gracefully")
        void shouldHandleNonexistentPrefixGracefully() {
            Map<String, Object> result = catalogScanService.scanClasspath("this/prefix/does/not/exist/");

            assertTrue((Boolean) result.get("success"));
            assertEquals(0, ((Number) result.get("resourcesScanned")).intValue());
            assertEquals(0, ((Number) result.get("resourcesIndexed")).intValue());
        }

        @Test
        @DisplayName("Should use provided ClassLoader")
        void shouldUseProvidedClassLoader() {
            ClassLoader customLoader = Thread.currentThread().getContextClassLoader();
            Map<String, Object> result = catalogScanService.scanClasspath("test/", customLoader);

            assertTrue((Boolean) result.get("success"));
        }
    }

    @Nested
    @DisplayName("scanAll() method tests")
    class ScanAllTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should handle null filesystem paths")
        void shouldHandleNullFilesystemPaths() {
            Map<String, Object> result = catalogScanService.scanAll(null, Arrays.asList("test/"));

            assertTrue((Boolean) result.get("success"));
            assertTrue(result.containsKey("filesystemResults"));
            assertTrue(result.containsKey("classpathResults"));
        }

        @Test
        @DisplayName("Should handle null classpath prefixes")
        void shouldHandleNullClasspathPrefixes() {
            Map<String, Object> result = catalogScanService.scanAll(Arrays.asList(tempDir.toString()), null);

            assertTrue((Boolean) result.get("success"));
        }

        @Test
        @DisplayName("Should handle both null inputs")
        void shouldHandleBothNullInputs() {
            Map<String, Object> result = catalogScanService.scanAll(null, null);

            assertTrue((Boolean) result.get("success"));
            assertEquals(0, ((Number) result.get("totalIndexed")).intValue());
        }

        @Test
        @DisplayName("Should handle empty lists")
        void shouldHandleEmptyLists() {
            Map<String, Object> result = catalogScanService.scanAll(new ArrayList<>(), new ArrayList<>());

            assertTrue((Boolean) result.get("success"));
            assertEquals(0, ((Number) result.get("totalFilesIndexed")).intValue());
            assertEquals(0, ((Number) result.get("totalResourcesIndexed")).intValue());
        }

        @Test
        @DisplayName("Should aggregate filesystem and classpath results")
        void shouldAggregateResults() throws IOException {
            // Create a test YAML file in the temp directory
            Path yamlFile = tempDir.resolve("test-config.yaml");
            Files.writeString(yamlFile, """
                metadata:
                  id: test-aggregate
                  name: Test Config
                  type: test
                rules:
                  - id: rule-1
                    condition: "true"
                    message: "Test rule"
                """);

            List<String> filesystemPaths = Arrays.asList(tempDir.toString());
            List<String> classpathPrefixes = Arrays.asList("nonexistent/");

            Map<String, Object> result = catalogScanService.scanAll(filesystemPaths, classpathPrefixes);

            assertTrue((Boolean) result.get("success"));
            assertTrue(result.containsKey("filesystemResults"));
            assertTrue(result.containsKey("classpathResults"));
            assertTrue(result.containsKey("totalFilesIndexed"));
            assertTrue(result.containsKey("totalResourcesIndexed"));
            assertTrue(result.containsKey("totalIndexed"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> filesystemResults = (List<Map<String, Object>>) result.get("filesystemResults");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> classpathResults = (List<Map<String, Object>>) result.get("classpathResults");
            
            assertEquals(1, filesystemResults.size());
            assertEquals(1, classpathResults.size());
        }

        @Test
        @DisplayName("Should aggregate errors from all scans")
        void shouldAggregateErrors() {
            // Include a non-existent filesystem path to generate errors
            List<String> filesystemPaths = Arrays.asList("/nonexistent/path/12345/");
            List<String> classpathPrefixes = Arrays.asList("test/");

            Map<String, Object> result = catalogScanService.scanAll(filesystemPaths, classpathPrefixes);

            // Should still succeed overall even with individual errors
            assertTrue((Boolean) result.get("success"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> filesystemResults = (List<Map<String, Object>>) result.get("filesystemResults");
            assertFalse((Boolean) filesystemResults.get(0).get("success"));
        }

        @Test
        @DisplayName("Should return combined totals")
        void shouldReturnCombinedTotals() throws IOException {
            // Create test YAML files
            Path yamlFile1 = tempDir.resolve("config1.yaml");
            Path yamlFile2 = tempDir.resolve("config2.yaml");
            
            Files.writeString(yamlFile1, """
                metadata:
                  id: config-1
                  name: Config 1
                rules:
                  - id: r1
                    condition: "true"
                """);
            
            Files.writeString(yamlFile2, """
                metadata:
                  id: config-2
                  name: Config 2
                enrichments:
                  - id: e1
                    type: lookup-enrichment
                """);

            Map<String, Object> result = catalogScanService.scanAll(
                Arrays.asList(tempDir.toString()),
                new ArrayList<>()
            );

            assertTrue((Boolean) result.get("success"));
            int totalFilesIndexed = ((Number) result.get("totalFilesIndexed")).intValue();
            int totalResourcesIndexed = ((Number) result.get("totalResourcesIndexed")).intValue();
            int totalIndexed = ((Number) result.get("totalIndexed")).intValue();
            
            assertEquals(totalFilesIndexed + totalResourcesIndexed, totalIndexed);
            assertTrue(totalFilesIndexed >= 2); // At least our 2 test files
        }
    }

    @Nested
    @DisplayName("YamlContentAnalyzer InputStream method tests")
    class YamlContentAnalyzerStreamTests {

        @Test
        @DisplayName("Should analyze YAML from InputStream")
        void shouldAnalyzeFromInputStream() throws IOException {
            String yamlContent = """
                metadata:
                  id: stream-test
                  name: Stream Test Config
                  type: rule-config
                  description: Testing stream-based analysis
                rules:
                  - id: rule-1
                    condition: "data.value > 100"
                    message: "Value exceeds threshold"
                """;

            try (java.io.InputStream is = new java.io.ByteArrayInputStream(yamlContent.getBytes())) {
                var summary = contentAnalyzer.analyzeYamlContent(is, "classpath:test/stream-test.yaml");

                assertEquals("stream-test", summary.getId());
                assertEquals("Stream Test Config", summary.getName());
                assertTrue(summary.getRuleCount() > 0);
            }
        }

        @Test
        @DisplayName("Should handle empty YAML stream")
        void shouldHandleEmptyStream() throws IOException {
            String yamlContent = "# Empty YAML file\n";

            try (java.io.InputStream is = new java.io.ByteArrayInputStream(yamlContent.getBytes())) {
                var summary = contentAnalyzer.analyzeYamlContent(is, "classpath:test/empty.yaml");

                // Should not throw, but summary should be minimal
                assertNotNull(summary);
            }
        }

        @Test
        @DisplayName("Should include resource path in summary")
        void shouldIncludeResourcePath() throws IOException {
            String yamlContent = """
                metadata:
                  id: path-test
                rules:
                  - id: r1
                """;

            String resourcePath = "classpath:config/scenarios/test.yaml";
            
            try (java.io.InputStream is = new java.io.ByteArrayInputStream(yamlContent.getBytes())) {
                var summary = contentAnalyzer.analyzeYamlContent(is, resourcePath);

                assertEquals(resourcePath, summary.getFilePath());
            }
        }
    }

    @Nested
    @DisplayName("YamlConfigMetadata classpath fields tests")
    class MetadataClasspathFieldsTests {

        @Test
        @DisplayName("Should have default false for isClasspathResource")
        void shouldDefaultToFalseForClasspathResource() {
            YamlConfigMetadata metadata = new YamlConfigMetadata();
            
            assertFalse(metadata.isClasspathResource());
            assertNull(metadata.getClasspathPrefix());
        }

        @Test
        @DisplayName("Should set and get isClasspathResource")
        void shouldSetAndGetClasspathResource() {
            YamlConfigMetadata metadata = new YamlConfigMetadata();
            
            metadata.setClasspathResource(true);
            assertTrue(metadata.isClasspathResource());
            
            metadata.setClasspathResource(false);
            assertFalse(metadata.isClasspathResource());
        }

        @Test
        @DisplayName("Should set and get classpathPrefix")
        void shouldSetAndGetClasspathPrefix() {
            YamlConfigMetadata metadata = new YamlConfigMetadata();
            
            String prefix = "META-INF/apex/";
            metadata.setClasspathPrefix(prefix);
            
            assertEquals(prefix, metadata.getClasspathPrefix());
        }
    }

    @Nested
    @DisplayName("Integration tests with filesystem")
    class FilesystemIntegrationTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should scan directory and find YAML files")
        void shouldScanDirectoryAndFindYamlFiles() throws IOException {
            // Create test YAML files
            Path yamlFile = tempDir.resolve("test-rules.yaml");
            Files.writeString(yamlFile, """
                metadata:
                  id: test-rules-scan
                  name: Test Rules
                  type: rule-config
                rules:
                  - id: validation-rule-1
                    condition: "data.amount > 0"
                    message: "Amount must be positive"
                    severity: ERROR
                """);

            Map<String, Object> result = catalogScanService.scanDirectory(tempDir.toString());

            assertTrue((Boolean) result.get("success"));
            assertEquals(tempDir.toString(), result.get("directoryPath"));
            assertTrue(((Number) result.get("filesIndexed")).intValue() >= 1);
        }

        @Test
        @DisplayName("Should scan nested directories")
        void shouldScanNestedDirectories() throws IOException {
            // Create nested directory structure
            Path nestedDir = tempDir.resolve("config/rules");
            Files.createDirectories(nestedDir);
            
            Path yamlFile1 = tempDir.resolve("root-config.yaml");
            Path yamlFile2 = nestedDir.resolve("nested-rules.yaml");
            
            Files.writeString(yamlFile1, """
                metadata:
                  id: root-config
                rules:
                  - id: r1
                """);
            
            Files.writeString(yamlFile2, """
                metadata:
                  id: nested-rules
                rules:
                  - id: r2
                """);

            Map<String, Object> result = catalogScanService.scanDirectory(tempDir.toString());

            assertTrue((Boolean) result.get("success"));
            assertTrue(((Number) result.get("filesIndexed")).intValue() >= 2);
        }

        @Test
        @DisplayName("Should handle both .yaml and .yml extensions")
        void shouldHandleBothExtensions() throws IOException {
            Path yamlFile = tempDir.resolve("config.yaml");
            Path ymlFile = tempDir.resolve("rules.yml");
            
            Files.writeString(yamlFile, """
                metadata:
                  id: yaml-ext
                """);
            
            Files.writeString(ymlFile, """
                metadata:
                  id: yml-ext
                """);

            Map<String, Object> result = catalogScanService.scanDirectory(tempDir.toString());

            assertTrue((Boolean) result.get("success"));
            assertTrue(((Number) result.get("filesIndexed")).intValue() >= 2);
        }
    }
}
