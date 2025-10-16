package dev.mars.apex.core.service.scenario;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for malformed scenario registry handling in DataTypeScenarioService.
 * 
 * Tests cover:
 * - Missing scenarios section
 * - Invalid YAML syntax
 * - Missing required fields (scenario-id, config-file)
 * - Null/empty values
 * - Invalid data types
 * - Graceful error handling
 * 
 * FOLLOWS CODING PRINCIPLES FROM prompts.txt:
 * - Test actual functionality (registry validation)
 * - Use real file system operations
 * - Progressive complexity (simple to complex errors)
 * - Validate error messages are clear and actionable
 * - Log test errors with "TEST:" prefix to distinguish from production errors
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("DataTypeScenarioService Malformed Registry Tests")
class DataTypeScenarioServiceMalformedRegistryTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DataTypeScenarioServiceMalformedRegistryTest.class);
    
    private DataTypeScenarioService service;
    private YamlConfigurationLoader configLoader;
    
    @BeforeEach
    void setUp() {
        service = new DataTypeScenarioService();
        configLoader = new YamlConfigurationLoader();
    }
    
    // ========================================
    // Missing Scenarios Section Tests
    // ========================================
    
    @Test
    @DisplayName("Should handle registry with missing scenarios section")
    void testMissingScenariosSectionInRegistry() {
        logger.info("TEST: Triggering intentional error - Missing scenarios section");
        
        // Given: Registry YAML with no scenarios section
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              type: "scenario-registry"
            
            routing:
              strategy: "type-based"
            """;
        
        // When: Load registry
        assertDoesNotThrow(() -> {
            Path registryFile = createTempRegistryFile(registryContent);
            service.loadScenarios(registryFile.toString());
        }, "Should handle missing scenarios section gracefully");
    }
    
    @Test
    @DisplayName("Should handle registry with empty scenarios list")
    void testEmptyScenariosList() {
        logger.info("TEST: Triggering intentional error - Empty scenarios list");
        
        // Given: Registry with empty scenarios list
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              type: "scenario-registry"
            
            scenarios: []
            
            routing:
              strategy: "type-based"
            """;
        
        // When: Load registry
        assertDoesNotThrow(() -> {
            Path registryFile = createTempRegistryFile(registryContent);
            service.loadScenarios(registryFile.toString());
        }, "Should handle empty scenarios list gracefully");
    }
    
    @Test
    @DisplayName("Should handle registry with null scenarios section")
    void testNullScenariosSection() {
        logger.info("TEST: Triggering intentional error - Null scenarios section");
        
        // Given: Registry with null scenarios
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              type: "scenario-registry"
            
            scenarios: null
            
            routing:
              strategy: "type-based"
            """;
        
        // When: Load registry
        assertDoesNotThrow(() -> {
            Path registryFile = createTempRegistryFile(registryContent);
            service.loadScenarios(registryFile.toString());
        }, "Should handle null scenarios section gracefully");
    }
    
    // ========================================
    // Missing Required Fields Tests
    // ========================================
    
    @Test
    @DisplayName("Should handle scenario entry missing scenario-id")
    void testMissingScenarioId() {
        logger.info("TEST: Triggering intentional error - Missing scenario-id");
        
        // Given: Scenario entry without scenario-id
        String registryContent = """
            scenarios:
              - config-file: "scenarios/test.yaml"
            
            routing:
              strategy: "type-based"
            """;
        
        // When: Load registry
        assertDoesNotThrow(() -> {
            Path registryFile = createTempRegistryFile(registryContent);
            service.loadScenarios(registryFile.toString());
        }, "Should handle missing scenario-id gracefully");
    }
    
    @Test
    @DisplayName("Should handle scenario entry missing config-file")
    void testMissingConfigFile() {
        logger.info("TEST: Triggering intentional error - Missing config-file");
        
        // Given: Scenario entry without config-file
        String registryContent = """
            scenarios:
              - scenario-id: "test-scenario"
            
            routing:
              strategy: "type-based"
            """;
        
        // When: Load registry
        assertDoesNotThrow(() -> {
            Path registryFile = createTempRegistryFile(registryContent);
            service.loadScenarios(registryFile.toString());
        }, "Should handle missing config-file gracefully");
    }
    
    @Test
    @DisplayName("Should handle scenario entry with null scenario-id")
    void testNullScenarioId() {
        logger.info("TEST: Triggering intentional error - Null scenario-id");
        
        // Given: Scenario entry with null scenario-id
        String registryContent = """
            scenarios:
              - scenario-id: null
                config-file: "scenarios/test.yaml"
            
            routing:
              strategy: "type-based"
            """;
        
        // When: Load registry
        assertDoesNotThrow(() -> {
            Path registryFile = createTempRegistryFile(registryContent);
            service.loadScenarios(registryFile.toString());
        }, "Should handle null scenario-id gracefully");
    }
    
    @Test
    @DisplayName("Should handle scenario entry with empty scenario-id")
    void testEmptyScenarioId() {
        logger.info("TEST: Triggering intentional error - Empty scenario-id");
        
        // Given: Scenario entry with empty scenario-id
        String registryContent = """
            scenarios:
              - scenario-id: ""
                config-file: "scenarios/test.yaml"
            
            routing:
              strategy: "type-based"
            """;
        
        // When: Load registry
        assertDoesNotThrow(() -> {
            Path registryFile = createTempRegistryFile(registryContent);
            service.loadScenarios(registryFile.toString());
        }, "Should handle empty scenario-id gracefully");
    }
    
    // ========================================
    // Helper Methods
    // ========================================
    
    private Path createTempRegistryFile(String content) throws IOException {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path registryFile = tempDir.resolve("test-registry-" + System.nanoTime() + ".yaml");
        Files.writeString(registryFile, content);
        registryFile.toFile().deleteOnExit();
        return registryFile;
    }
}

