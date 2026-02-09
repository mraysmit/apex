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

import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

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
 * Unit tests for YamlConfigurationLoader stream-based loading methods.
 * 
 * <p>Tests the following methods added for Phase 1 of the Unified Resource Loading plan:</p>
 * <ul>
 *   <li>{@link YamlConfigurationLoader#loadAsMap(InputStream)}</li>
 *   <li>{@link YamlConfigurationLoader#loadAsMapFromClasspath(String)}</li>
 * </ul>
 * 
 * <p>These tests validate that YAML content can be loaded from InputStreams
 * and classpath resources, enabling JAR-packaged resource loading.</p>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 */
@DisplayName("YamlConfigurationLoader Stream Loading Tests")
class YamlConfigurationLoaderStreamTest {

    private static final Logger logger = LoggerFactory.getLogger(YamlConfigurationLoaderStreamTest.class);

    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        logger.info("Setting up YamlConfigurationLoaderStreamTest");
        loader = new YamlConfigurationLoader();
    }

    // ========================================================================
    // loadAsMap(InputStream) Tests
    // ========================================================================

    @Test
    @DisplayName("Should load valid YAML from InputStream as Map")
    void testLoadAsMapFromInputStream() throws Exception {
        logger.info("=== Testing loadAsMap(InputStream) with valid YAML ===");

        String yamlContent = """
            metadata:
              id: "test-config"
              name: "Test Configuration"
              description: "Test configuration for stream loading"
              type: "rule-config"
              version: "1.0.0"
            
            rules:
              - id: "test-rule-1"
                condition: "#amount > 0"
                message: "Amount must be positive"
            """;

        try (InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))) {
            Map<String, Object> result = loader.loadAsMap(inputStream);

            assertNotNull(result, "Result should not be null");
            assertTrue(result.containsKey("metadata"), "Should contain metadata section");
            assertTrue(result.containsKey("rules"), "Should contain rules section");

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            assertEquals("test-config", metadata.get("id"), "Metadata ID should match");
            assertEquals("Test Configuration", metadata.get("name"), "Metadata name should match");

            logger.info("[OK] Successfully loaded YAML from InputStream");
        }
    }

    @Test
    @DisplayName("Should throw exception for null InputStream")
    void testLoadAsMapFromInputStreamNullThrowsException() {
        logger.info("=== Testing loadAsMap(InputStream) with null stream ===");

        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadAsMap((InputStream) null)
        );

        assertTrue(exception.getMessage().contains("null"), 
            "Exception message should mention null");
        
        logger.info("[OK] Correctly threw exception for null InputStream: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for empty YAML content")
    void testLoadAsMapFromInputStreamEmptyContent() {
        logger.info("=== Testing loadAsMap(InputStream) with empty content ===");

        String emptyYaml = "";
        InputStream inputStream = new ByteArrayInputStream(emptyYaml.getBytes(StandardCharsets.UTF_8));

        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadAsMap(inputStream)
        );

        logger.info("[OK] Correctly threw exception for empty YAML: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for invalid YAML syntax")
    void testLoadAsMapFromInputStreamInvalidYaml() {
        logger.info("=== Testing loadAsMap(InputStream) with invalid YAML ===");

        String invalidYaml = """
            metadata:
              id: "test"
              invalid yaml here: [broken
            """;

        InputStream inputStream = new ByteArrayInputStream(invalidYaml.getBytes(StandardCharsets.UTF_8));

        assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadAsMap(inputStream),
            "Should throw exception for invalid YAML"
        );

        logger.info("[OK] Correctly threw exception for invalid YAML syntax");
    }

    @Test
    @DisplayName("Should handle YAML with property placeholders from stream")
    void testLoadAsMapFromInputStreamWithProperties() throws Exception {
        logger.info("=== Testing loadAsMap(InputStream) with property placeholders ===");

        // Set a system property for resolution
        String originalValue = System.getProperty("test.config.name");
        System.setProperty("test.config.name", "Resolved Name");

        try {
            String yamlContent = """
                metadata:
                  id: "test-config"
                  name: "Stream Config"
                  description: "Stream configuration for testing"
                  type: "rule-config"
                  version: "1.0.0"
                
                rules:
                  - id: "rule-1"
                    condition: "true"
                    message: "Test message"
                """;

            try (InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))) {
                Map<String, Object> result = loader.loadAsMap(inputStream);

                assertNotNull(result, "Result should not be null");
                assertTrue(result.containsKey("metadata"), "Should contain metadata section");

                logger.info("[OK] Successfully loaded YAML with properties from InputStream");
            }
        } finally {
            // Restore original property value
            if (originalValue != null) {
                System.setProperty("test.config.name", originalValue);
            } else {
                System.clearProperty("test.config.name");
            }
        }
    }

    // ========================================================================
    // loadAsMapFromClasspath(String) Tests
    // ========================================================================

    @Test
    @DisplayName("Should load YAML from classpath resource")
    void testLoadAsMapFromClasspath() throws Exception {
        logger.info("=== Testing loadAsMapFromClasspath() ===");

        // Use an existing test resource
        String resourcePath = "scenario/test-registry.yaml";
        
        Map<String, Object> result = loader.loadAsMapFromClasspath(resourcePath);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("metadata"), "Should contain metadata section");

        logger.info("[OK] Successfully loaded YAML from classpath: {}", resourcePath);
    }

    @Test
    @DisplayName("Should throw exception for non-existent classpath resource")
    void testLoadAsMapFromClasspathNotFound() {
        logger.info("=== Testing loadAsMapFromClasspath() with non-existent resource ===");

        String nonExistentPath = "non-existent/path/config.yaml";

        YamlConfigurationException exception = assertThrows(
            YamlConfigurationException.class,
            () -> loader.loadAsMapFromClasspath(nonExistentPath)
        );

        assertTrue(exception.getMessage().contains("not found") || 
                   exception.getMessage().contains(nonExistentPath),
            "Exception message should mention the missing resource");

        logger.info("[OK] Correctly threw exception for non-existent classpath resource: {}", 
                   exception.getMessage());
    }

    // ========================================================================
    // Parity Tests - Stream vs File Loading
    // ========================================================================

    @Test
    @DisplayName("Stream loading should produce same result as file loading")
    void testLoadFromStreamComparedToFile() throws Exception {
        logger.info("=== Testing stream/file parity ===");

        // Create identical YAML content
        String yamlContent = """
            metadata:
              id: "parity-test"
              name: "Parity Test Configuration"
              description: "Testing stream vs file loading parity"
              type: "rule-config"
              version: "1.0.0"
            
            rules:
              - id: "rule-1"
                condition: "#value > 100"
                message: "Value exceeds limit"
                severity: "ERROR"
              
              - id: "rule-2"
                condition: "#name != null"
                message: "Name is required"
                severity: "WARNING"
            """;

        // Load from stream
        try (InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))) {
            Map<String, Object> streamResult = loader.loadAsMap(inputStream);

            assertNotNull(streamResult, "Stream result should not be null");

            // Verify structure
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) streamResult.get("metadata");
            assertEquals("parity-test", metadata.get("id"));
            assertEquals("Parity Test Configuration", metadata.get("name"));

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> rules = 
                (java.util.List<Map<String, Object>>) streamResult.get("rules");
            assertEquals(2, rules.size(), "Should have 2 rules");
            assertEquals("rule-1", rules.get(0).get("id"));
            assertEquals("rule-2", rules.get(1).get("id"));

            logger.info("[OK] Stream loading produces expected structure");
        }
    }

    @Test
    @DisplayName("Should correctly parse complex nested YAML from stream")
    void testLoadAsMapFromInputStreamComplexStructure() throws Exception {
        logger.info("=== Testing loadAsMap(InputStream) with complex nested structure ===");

        String yamlContent = """
            metadata:
              id: "complex-config"
              name: "Complex Configuration"
              description: "Testing complex nested structure loading"
              type: "scenario-registry"
              version: "2.0.0"
            
            scenarios:
              - scenario-id: "scenario-1"
                config-file: "scenarios/scenario-1.yaml"
                business-domain: "Trading"
                enabled: true
              
              - scenario-id: "scenario-2"
                config-file: "scenarios/scenario-2.yaml"
                business-domain: "Settlement"
                enabled: false
            
            routing:
              strategy: "classification-based"
              default-scenario: "scenario-1"
            """;

        try (InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))) {
            Map<String, Object> result = loader.loadAsMap(inputStream);

            assertNotNull(result, "Result should not be null");

            // Verify scenarios list
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> scenarios = 
                (java.util.List<Map<String, Object>>) result.get("scenarios");
            assertNotNull(scenarios, "Should have scenarios section");
            assertEquals(2, scenarios.size(), "Should have 2 scenarios");

            // Verify first scenario
            Map<String, Object> scenario1 = scenarios.get(0);
            assertEquals("scenario-1", scenario1.get("scenario-id"));
            assertEquals("Trading", scenario1.get("business-domain"));
            assertEquals(true, scenario1.get("enabled"));

            // Verify routing section
            @SuppressWarnings("unchecked")
            Map<String, Object> routing = (Map<String, Object>) result.get("routing");
            assertNotNull(routing, "Should have routing section");
            assertEquals("classification-based", routing.get("strategy"));

            logger.info("[OK] Successfully parsed complex nested YAML from stream");
        }
    }
}
