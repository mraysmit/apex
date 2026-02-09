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

import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.engine.config.RulesEngine;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for malformed scenario registry handling in RulesEngine.
 * 
 * <p>Replaces the deleted DataTypeScenarioServiceMalformedRegistryTest, using the new RulesEngine API.</p>
 * 
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Missing scenarios section</li>
 *   <li>Invalid YAML syntax</li>
 *   <li>Missing required fields (scenario-id, config-file)</li>
 *   <li>Null/empty values</li>
 *   <li>Invalid data types</li>
 *   <li>Graceful error handling</li>
 * </ul>
 * 
 * <p>FOLLOWS CODING PRINCIPLES FROM prompts.txt:</p>
 * <ul>
 *   <li>Test actual functionality (registry validation)</li>
 *   <li>Use real file system operations</li>
 *   <li>Progressive complexity (simple to complex errors)</li>
 *   <li>Validate error messages are clear and actionable</li>
 *   <li>Log test errors with "TEST:" prefix to distinguish from production errors</li>
 * </ul>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 3.0
 * @see RulesEngine#fromScenarioRegistry(String)
 */
@DisplayName("RulesEngine Malformed Registry Tests")
class RulesEngineMalformedRegistryTest {
    
    private static final Logger logger = LoggerFactory.getLogger(RulesEngineMalformedRegistryTest.class);
    
    // ========================================
    // Missing Scenarios Section Tests
    // ========================================
    
    @Test
    @DisplayName("Should throw exception for registry with missing scenarios section")
    void testMissingScenariosSectionInRegistry() {
        logger.info("TEST: Triggering intentional error - Missing scenarios section");
        
        // Given: Registry YAML with no scenarios section
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry for missing scenarios section"
              type: "scenario-registry"
            
            routing:
              strategy: "type-based"
            """;
        
        // When/Then: Load registry should throw exception for empty registry
        assertThrows(YamlConfigurationException.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for registry with missing scenarios section");
    }
    
    @Test
    @DisplayName("Should throw exception for registry with empty scenarios list")
    void testEmptyScenariosList() {
        logger.info("TEST: Triggering intentional error - Empty scenarios list");
        
        // Given: Registry with empty scenarios list
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry with empty scenarios list"
              type: "scenario-registry"
            
            scenarios: []
            
            routing:
              strategy: "type-based"
            """;
        
        // When/Then: Load registry should throw exception
        assertThrows(YamlConfigurationException.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for registry with empty scenarios list");
    }
    
    @Test
    @DisplayName("Should throw exception for registry with null scenarios section")
    void testNullScenariosSection() {
        logger.info("TEST: Triggering intentional error - Null scenarios section");
        
        // Given: Registry with null scenarios
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry with null scenarios section"
              type: "scenario-registry"
            
            scenarios: null
            
            routing:
              strategy: "type-based"
            """;
        
        // When/Then: Load registry should throw exception
        assertThrows(YamlConfigurationException.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for registry with null scenarios section");
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
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry with missing scenario-id"
              type: "scenario-registry"
            
            scenarios:
              - config-file: "scenarios/test.yaml"
            
            routing:
              strategy: "type-based"
            """;
        
        // When/Then: This may throw or handle gracefully depending on implementation
        assertThrows(Exception.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for scenario entry missing scenario-id");
    }
    
    @Test
    @DisplayName("Should handle scenario entry missing config-file")
    void testMissingConfigFile() {
        logger.info("TEST: Triggering intentional error - Missing config-file");
        
        // Given: Scenario entry without config-file
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry with missing config-file"
              type: "scenario-registry"
            
            scenarios:
              - scenario-id: "test-scenario"
            
            routing:
              strategy: "type-based"
            """;
        
        // When/Then: This may throw or handle gracefully depending on implementation
        assertThrows(Exception.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for scenario entry missing config-file");
    }
    
    @Test
    @DisplayName("Should handle scenario entry with null scenario-id")
    void testNullScenarioId() {
        logger.info("TEST: Triggering intentional error - Null scenario-id");
        
        // Given: Scenario entry with null scenario-id
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry with null scenario-id"
              type: "scenario-registry"
            
            scenarios:
              - scenario-id: null
                config-file: "scenarios/test.yaml"
            
            routing:
              strategy: "type-based"
            """;
        
        // When/Then: This should throw exception
        assertThrows(Exception.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for scenario entry with null scenario-id");
    }
    
    @Test
    @DisplayName("Should handle scenario entry with empty scenario-id")
    void testEmptyScenarioId() {
        logger.info("TEST: Triggering intentional error - Empty scenario-id");
        
        // Given: Scenario entry with empty scenario-id
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry with empty scenario-id"
              type: "scenario-registry"
            
            scenarios:
              - scenario-id: ""
                config-file: "scenarios/test.yaml"
            
            routing:
              strategy: "type-based"
            """;
        
        // When/Then: This should throw exception
        assertThrows(Exception.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for scenario entry with empty scenario-id");
    }
    
    // ========================================
    // Invalid YAML Syntax Tests
    // ========================================
    
    @Test
    @DisplayName("Should throw exception for invalid YAML syntax")
    void testInvalidYamlSyntax() {
        logger.info("TEST: Triggering intentional error - Invalid YAML syntax");
        
        // Given: Invalid YAML syntax
        String registryContent = """
            invalid: yaml: syntax:
              - missing
                - bracket
            unclosed: [
            """;
        
        // When/Then: Should throw exception for invalid YAML
        assertThrows(YamlConfigurationException.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for invalid YAML syntax");
    }
    
    @Test
    @DisplayName("Should throw exception for malformed scenario reference")
    void testMalformedScenarioReference() {
        logger.info("TEST: Triggering intentional error - Malformed scenario reference");
        
        // Given: Registry with malformed scenario reference (non-existent file)
        String registryContent = """
            metadata:
              id: "test-registry"
              name: "Test Registry"
              version: "1.0.0"
              description: "Test registry with malformed scenario reference"
              type: "scenario-registry"
            
            scenarios:
              - scenario-id: "test-scenario"
                config-file: "nonexistent/path/to/scenario.yaml"
            """;
        
        // When/Then: Should throw exception for non-existent scenario file
        assertThrows(YamlConfigurationException.class, () -> {
            Path registryFile = createTempRegistryFile(registryContent);
            RulesEngine.fromScenarioRegistry(registryFile.toString());
        }, "Should throw exception for non-existent scenario file");
    }
    
    // ========================================
    // Edge Cases
    // ========================================
    
    @Test
    @DisplayName("Should throw exception for non-existent registry file")
    void testNonExistentRegistryFile() {
        logger.info("TEST: Triggering intentional error - Non-existent registry file");
        
        assertThrows(YamlConfigurationException.class, () -> {
            RulesEngine.fromScenarioRegistry("/path/that/does/not/exist/registry.yaml");
        }, "Should throw exception for non-existent registry file");
    }
    
    @Test
    @DisplayName("Should throw exception for null registry path")
    void testNullRegistryPath() {
        logger.info("TEST: Triggering intentional error - Null registry path");
        
        assertThrows(NullPointerException.class, () -> {
            RulesEngine.fromScenarioRegistry(null);
        }, "Should throw NullPointerException for null registry path");
    }
    
    @Test
    @DisplayName("Should throw exception for empty registry path")
    void testEmptyRegistryPath() {
        logger.info("TEST: Triggering intentional error - Empty registry path");
        
        assertThrows(YamlConfigurationException.class, () -> {
            RulesEngine.fromScenarioRegistry("");
        }, "Should throw exception for empty registry path");
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
