package dev.mars.apex.core.util;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for YamlMetadataValidator.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Test class for YamlMetadataValidator.
 */
class YamlMetadataValidatorTest {
    
    @TempDir
    Path tempDir;
    
    private YamlMetadataValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new YamlMetadataValidator(tempDir.toString());
    }
    
    @Test
    void testValidScenarioFile() throws IOException {
        String validScenario = """
            metadata:
              id: "test-scenario-001"
              name: "Test Scenario"
              version: "1.0.0"
              description: "A test scenario"
              type: "scenario"
              business-domain: "Testing"
              owner: "test.team@company.com"
            
            scenario:
              scenario-id: "test-scenario"
              data-types:
                - "TestDataType"
              rule-configurations:
                - "config/test-rules.yaml"
            """;
        
        writeFile("scenarios/test-scenario.yaml", validScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/test-scenario.yaml");
        
        assertTrue(result.isValid(), "Valid scenario should pass validation");
        assertEquals(0, result.getErrorCount());
        assertEquals("VALID", result.getStatus());
    }
    
    @Test
    void testMissingTypeField() throws IOException {
        String invalidScenario = """
            metadata:
              name: "Test Scenario"
              version: "1.0.0"
              description: "A test scenario"
              # Missing type field
            
            scenario:
              scenario-id: "test-scenario"
              data-types:
                - "TestDataType"
              rule-configurations:
                - "config/test-rules.yaml"
            """;
        
        writeFile("scenarios/invalid-scenario.yaml", invalidScenario);
        
        YamlValidationResult result = validator.validateFile("scenarios/invalid-scenario.yaml");
        
        assertFalse(result.isValid(), "Scenario missing type should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("Missing required metadata field: type")));
        assertEquals("INVALID", result.getStatus());
    }
    
    @Test
    void testInvalidFileType() throws IOException {
        String invalidType = """
            metadata:
              name: "Test File"
              version: "1.0.0"
              description: "A test file"
              type: "invalid-type"
            """;
        
        writeFile("test-file.yaml", invalidType);
        
        YamlValidationResult result = validator.validateFile("test-file.yaml");
        
        assertFalse(result.isValid(), "Invalid file type should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("Invalid file type: invalid-type")));
    }
    
    @Test
    void testMissingMetadataSection() throws IOException {
        System.out.println("TEST: Triggering intentional error - testing YAML file missing metadata section validation");

        String noMetadata = """
            scenario:
              scenario-id: "test-scenario"
              data-types:
                - "TestDataType"
              rule-configurations:
                - "config/test-rules.yaml"
            """;

        writeFile("scenarios/no-metadata.yaml", noMetadata);

        YamlValidationResult result = validator.validateFile("scenarios/no-metadata.yaml");

        assertFalse(result.isValid(), "File without metadata should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("Missing 'metadata' section")));
    }
    
    @Test
    void testValidBootstrapFile() throws IOException {
        String validBootstrap = """
            metadata:
              id: "test-bootstrap-001"
              name: "Test Bootstrap"
              version: "1.0.0"
              description: "A test bootstrap configuration"
              type: "bootstrap"
              business-domain: "Testing"
              created-by: "test.admin@company.com"
            
            rule-chains:
              - id: "test-chain"
                name: "Test Chain"
            """;
        
        writeFile("bootstrap/test-bootstrap.yaml", validBootstrap);
        
        YamlValidationResult result = validator.validateFile("bootstrap/test-bootstrap.yaml");
        
        assertTrue(result.isValid(), "Valid bootstrap should pass validation");
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testValidDatasetFile() throws IOException {
        String validDataset = """
            metadata:
              id: "test-dataset-001"
              name: "Test Dataset"
              version: "1.0.0"
              description: "A test dataset"
              type: "dataset"
              source: "Test data source"
            
            data:
              - id: "test1"
                value: "value1"
              - id: "test2"
                value: "value2"
            """;
        
        writeFile("datasets/test-dataset.yaml", validDataset);
        
        YamlValidationResult result = validator.validateFile("datasets/test-dataset.yaml");
        
        assertTrue(result.isValid(), "Valid dataset should pass validation");
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testScenarioRegistryValidation() throws IOException {
        String validRegistry = """
            metadata:
              id: "test-registry-001"
              name: "Test Registry"
              version: "1.0.0"
              description: "A test scenario registry"
              type: "scenario-registry"
              created-by: "test.admin@company.com"
            
            scenario-registry:
              - scenario-id: "test-scenario"
                config-file: "scenarios/test-scenario.yaml"
                data-types: ["TestDataType"]
                description: "Test scenario"
            """;
        
        writeFile("config/test-registry.yaml", validRegistry);
        
        YamlValidationResult result = validator.validateFile("config/test-registry.yaml");
        
        assertTrue(result.isValid(), "Valid registry should pass validation");
        assertEquals(0, result.getErrorCount());
    }
    
    @Test
    void testMultipleFileValidation() throws IOException {
        // Create multiple files
        String validFile = """
            metadata:
              id: "valid-file-001"
              name: "Valid File"
              version: "1.0.0"
              description: "A valid file"
              type: "rule-config"
              author: "test.author@company.com"
            
            rules:
              - id: "test-rule"
            """;
        
        String invalidFile = """
            metadata:
              name: "Invalid File"
              version: "1.0.0"
              description: "An invalid file"
              # Missing type field
            """;
        
        writeFile("config/valid.yaml", validFile);
        writeFile("config/invalid.yaml", invalidFile);
        
        List<String> filePaths = List.of("config/valid.yaml", "config/invalid.yaml");
        YamlValidationSummary summary = validator.validateFiles(filePaths);
        
        assertEquals(2, summary.getTotalCount());
        assertEquals(1, summary.getValidCount());
        assertEquals(1, summary.getInvalidCount());
        assertFalse(summary.isAllValid());
        
        // Check that we can get the invalid results
        List<YamlValidationResult> invalidResults = summary.getInvalidResults();
        assertEquals(1, invalidResults.size());
        assertEquals("config/invalid.yaml", invalidResults.get(0).getFilePath());
    }
    
    @Test
    void testVersionFormatWarning() throws IOException {
        String invalidVersion = """
            metadata:
              id: "test-version-001"
              name: "Test File"
              version: "invalid-version"
              description: "A test file"
              type: "rule-config"
              author: "test.author@company.com"
            """;
        
        writeFile("config/version-test.yaml", invalidVersion);
        
        YamlValidationResult result = validator.validateFile("config/version-test.yaml");
        
        assertTrue(result.isValid(), "File should be valid despite version warning");
        assertTrue(result.hasWarnings(), "Should have warnings about version format");
        assertTrue(result.getWarnings().stream().anyMatch(warning -> warning.contains("Version should follow semantic versioning")));
        assertEquals("VALID_WITH_WARNINGS", result.getStatus());
    }
    
    @Test
    void testMissingFile() {
        YamlValidationResult result = validator.validateFile("non-existent-file.yaml");
        
        assertFalse(result.isValid(), "Non-existent file should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("File does not exist")));
    }
    
    @Test
    void testInvalidYamlSyntax() throws IOException {
        String invalidYaml = """
            metadata:
              name: "Test File"
              version: "1.0.0"
              description: "A test file"
              type: "rule-config"
            invalid: yaml: syntax:
              - missing
                - bracket
            unclosed: [
            """;
        
        writeFile("config/invalid-syntax.yaml", invalidYaml);
        
        YamlValidationResult result = validator.validateFile("config/invalid-syntax.yaml");
        
        assertFalse(result.isValid(), "Invalid YAML syntax should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("Failed to parse YAML file")));
    }

    @Test
    void testValidPipelineType() throws IOException {
        String validPipeline = """
            metadata:
              id: "test-pipeline-001"
              name: "Test Pipeline"
              version: "1.0.0"
              description: "A test pipeline configuration"
              type: "pipeline"
              author: "pipeline.team@company.com"
            """;

        writeFile("config/pipeline-test.yaml", validPipeline);

        YamlValidationResult result = validator.validateFile("config/pipeline-test.yaml");
        assertTrue(result.isValid(), "Pipeline type should be valid. Errors: " + result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("VALID", result.getStatus());
    }

    /**
     * Helper method to write content to a file in the temp directory.
     */
    private void writeFile(String relativePath, String content) throws IOException {
        Path filePath = tempDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }
}
