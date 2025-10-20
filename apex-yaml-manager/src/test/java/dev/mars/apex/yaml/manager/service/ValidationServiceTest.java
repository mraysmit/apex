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

import dev.mars.apex.yaml.manager.model.ValidationIssue;
import dev.mars.apex.yaml.manager.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationService.
 */
@DisplayName("ValidationService Tests")
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    @DisplayName("Should detect missing file")
    void testMissingFile() {
        ValidationResult result = validationService.validateStructure("/nonexistent/file.yaml");
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> "FILE_NOT_FOUND".equals(i.getCode())));
    }

    @Test
    @DisplayName("Should detect missing metadata section")
    void testMissingMetadata(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("rule-groups:\n  - id: test\n");
        }
        
        ValidationResult result = validationService.validateStructure(yamlFile.getAbsolutePath());
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> "MISSING_METADATA".equals(i.getCode())));
    }

    @Test
    @DisplayName("Should detect missing id in metadata")
    void testMissingId(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  name: Test\n  version: 1.0.0\n");
        }
        
        ValidationResult result = validationService.validateStructure(yamlFile.getAbsolutePath());
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> "MISSING_ID".equals(i.getCode())));
    }

    @Test
    @DisplayName("Should detect missing name in metadata")
    void testMissingName(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n  version: 1.0.0\n");
        }
        
        ValidationResult result = validationService.validateStructure(yamlFile.getAbsolutePath());
        
        assertTrue(result.hasWarnings());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> "MISSING_NAME".equals(i.getCode())));
    }

    @Test
    @DisplayName("Should detect missing version in metadata")
    void testMissingVersion(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n  name: Test\n");
        }
        
        ValidationResult result = validationService.validateStructure(yamlFile.getAbsolutePath());
        
        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> "MISSING_VERSION".equals(i.getCode())));
    }

    @Test
    @DisplayName("Should pass valid YAML structure")
    void testValidStructure(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n  name: Test\n  version: 1.0.0\n");
        }
        
        ValidationResult result = validationService.validateStructure(yamlFile.getAbsolutePath());
        
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("Should detect missing referenced file")
    void testMissingReference(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n  name: Test\n");
            writer.write("scenario-refs:\n  - missing-file.yaml\n");
        }
        
        ValidationResult result = validationService.validateReferences(
            yamlFile.getAbsolutePath(), 
            tempDir.toFile().getAbsolutePath()
        );
        
        assertTrue(result.hasErrors());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> "MISSING_REFERENCE".equals(i.getCode())));
    }

    @Test
    @DisplayName("Should pass when all references exist")
    void testValidReferences(@TempDir Path tempDir) throws IOException {
        File refFile = tempDir.resolve("ref.yaml").toFile();
        refFile.createNewFile();
        
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n");
            writer.write("scenario-refs:\n  - ref.yaml\n");
        }
        
        ValidationResult result = validationService.validateReferences(
            yamlFile.getAbsolutePath(), 
            tempDir.toFile().getAbsolutePath()
        );
        
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("Should detect invalid ID format")
    void testInvalidIdFormat(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: Test_ID_123\n  name: Test\n");
        }
        
        ValidationResult result = validationService.validateConsistency(yamlFile.getAbsolutePath());
        
        assertTrue(result.hasWarnings());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> "INVALID_ID_FORMAT".equals(i.getCode())));
    }

    @Test
    @DisplayName("Should pass valid ID format")
    void testValidIdFormat(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id-123\n  name: Test\n");
        }
        
        ValidationResult result = validationService.validateConsistency(yamlFile.getAbsolutePath());
        
        assertFalse(result.hasWarnings());
    }

    @Test
    @DisplayName("Should handle invalid YAML syntax")
    void testInvalidYamlSyntax(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test\n  invalid: [unclosed\n");
        }
        
        ValidationResult result = validationService.validateStructure(yamlFile.getAbsolutePath());
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("Should track validation time")
    void testValidationTime(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n  name: Test\n  version: 1.0.0\n");
        }
        
        ValidationResult result = validationService.validateStructure(yamlFile.getAbsolutePath());
        
        assertTrue(result.getValidationTimeMs() >= 0);
    }
}

