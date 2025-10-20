package dev.mars.apex.yaml.manager.controller;

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

import dev.mars.apex.yaml.manager.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ValidationController.
 * 
 * Tests the REST API endpoints with real service instances and actual HTTP calls.
 * No mocking - uses real ValidationService and real YAML files.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ValidationController Integration Tests")
class ValidationControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/yaml-manager/api/validation";
    }

    // ========================================
    // POST /api/validation/structure Tests
    // ========================================

    @Test
    @DisplayName("Should validate YAML structure with valid file")
    void testValidateStructureWithValidFile(@TempDir Path tempDir) throws IOException {
        // Create valid YAML file
        File yamlFile = tempDir.resolve("valid.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-config\n");
            writer.write("  name: Test Configuration\n");
            writer.write("  version: 1.0.0\n");
        }

        // Call actual REST endpoint
        ResponseEntity<ValidationResult> response = restTemplate.postForEntity(
            baseUrl + "/structure?filePath=" + yamlFile.getAbsolutePath(),
            null,
            ValidationResult.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isValid(), "Valid YAML should pass validation");
        assertEquals(0, response.getBody().getErrorCount(), "Valid YAML should have no errors");
    }

    @Test
    @DisplayName("Should detect missing metadata in YAML structure")
    void testValidateStructureWithMissingMetadata(@TempDir Path tempDir) throws IOException {
        // Create YAML file without metadata
        File yamlFile = tempDir.resolve("no-metadata.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("rules:\n");
            writer.write("  - id: rule1\n");
        }

        // Call actual REST endpoint
        ResponseEntity<ValidationResult> response = restTemplate.postForEntity(
            baseUrl + "/structure?filePath=" + yamlFile.getAbsolutePath(),
            null,
            ValidationResult.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isValid(), "YAML without metadata should fail");
        assertTrue(response.getBody().getErrorCount() > 0, "Should have errors");
    }

    @Test
    @DisplayName("Should detect missing required metadata fields")
    void testValidateStructureWithMissingId(@TempDir Path tempDir) throws IOException {
        // Create YAML with metadata but missing id
        File yamlFile = tempDir.resolve("no-id.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  name: Test\n");
            writer.write("  version: 1.0.0\n");
        }

        // Call actual REST endpoint
        ResponseEntity<ValidationResult> response = restTemplate.postForEntity(
            baseUrl + "/structure?filePath=" + yamlFile.getAbsolutePath(),
            null,
            ValidationResult.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isValid(), "YAML without id should fail");
        assertTrue(response.getBody().getErrorCount() > 0, "Should have errors");
    }

    @Test
    @DisplayName("Should handle missing file gracefully")
    void testValidateStructureWithMissingFile() {
        // Call with non-existent file
        ResponseEntity<ValidationResult> response = restTemplate.postForEntity(
            baseUrl + "/structure?filePath=/nonexistent/file.yaml",
            null,
            ValidationResult.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isValid(), "Non-existent file should fail validation");
        assertTrue(response.getBody().getErrorCount() > 0, "Should have errors");
    }

    @Test
    @DisplayName("Should return 400 when filePath parameter is missing")
    void testValidateStructureMissingFilePath() {
        // Call without filePath parameter
        ResponseEntity<?> response = restTemplate.postForEntity(
            baseUrl + "/structure",
            null,
            Map.class
        );

        // Validate response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========================================
    // POST /api/validation/references Tests
    // ========================================

    @Test
    @DisplayName("Should validate file references")
    void testValidateReferences(@TempDir Path tempDir) throws IOException {
        // Create YAML file with valid structure
        File yamlFile = tempDir.resolve("refs.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-refs\n");
            writer.write("  name: Test References\n");
        }

        // Call actual REST endpoint
        ResponseEntity<ValidationResult> response = restTemplate.postForEntity(
            baseUrl + "/references?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            null,
            ValidationResult.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilePath());
    }

    // ========================================
    // POST /api/validation/consistency Tests
    // ========================================

    @Test
    @DisplayName("Should validate consistency")
    void testValidateConsistency(@TempDir Path tempDir) throws IOException {
        // Create YAML file
        File yamlFile = tempDir.resolve("consistency.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-consistency\n");
            writer.write("  name: Test Consistency\n");
        }

        // Call actual REST endpoint
        ResponseEntity<ValidationResult> response = restTemplate.postForEntity(
            baseUrl + "/consistency?filePath=" + yamlFile.getAbsolutePath(),
            null,
            ValidationResult.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilePath());
    }

    // ========================================
    // POST /api/validation/all Tests
    // ========================================

    @Test
    @DisplayName("Should perform all validations")
    void testValidateAll(@TempDir Path tempDir) throws IOException {
        // Create valid YAML file
        File yamlFile = tempDir.resolve("all.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-all\n");
            writer.write("  name: Test All\n");
            writer.write("  version: 1.0.0\n");
        }

        // Call actual REST endpoint
        ResponseEntity<ValidationResult> response = restTemplate.postForEntity(
            baseUrl + "/all?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            null,
            ValidationResult.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ALL", response.getBody().getValidationType());
    }

    // ========================================
    // GET /api/validation/is-valid Tests
    // ========================================

    @Test
    @DisplayName("Should return true for valid file")
    void testIsValidWithValidFile(@TempDir Path tempDir) throws IOException {
        // Create valid YAML file
        File yamlFile = tempDir.resolve("valid-check.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-valid\n");
            writer.write("  name: Test Valid\n");
        }

        // Call actual REST endpoint
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
            baseUrl + "/is-valid?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            Boolean.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody(), "Valid file should return true");
    }

    @Test
    @DisplayName("Should return false for invalid file")
    void testIsValidWithInvalidFile(@TempDir Path tempDir) throws IOException {
        // Create invalid YAML file (missing metadata)
        File yamlFile = tempDir.resolve("invalid-check.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("rules:\n");
            writer.write("  - id: rule1\n");
        }

        // Call actual REST endpoint
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
            baseUrl + "/is-valid?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            Boolean.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody(), "Invalid file should return false");
    }
}

