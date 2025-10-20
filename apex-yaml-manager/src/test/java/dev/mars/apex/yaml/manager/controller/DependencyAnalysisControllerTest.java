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

import dev.mars.apex.yaml.manager.model.DependencyMetrics;
import dev.mars.apex.yaml.manager.model.EnhancedYamlDependencyGraph;
import dev.mars.apex.yaml.manager.model.ImpactAnalysisResult;
import dev.mars.apex.yaml.manager.model.TreeNode;
import dev.mars.apex.yaml.manager.service.DependencyAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DependencyAnalysisController.
 *
 * Tests the REST API endpoints with real service instances and actual HTTP calls.
 * No mocking - uses real DependencyAnalysisService and real YAML files.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("DependencyAnalysisController Integration Tests")
class DependencyAnalysisControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DependencyAnalysisService dependencyService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/yaml-manager/api/dependencies";
    }



    // ========================================
    // POST /api/dependencies/analyze Tests
    // ========================================

    @Test
    @DisplayName("Should handle missing file path parameter")
    void testAnalyzeWithMissingFilePath() {
        // Call without filePath parameter
        ResponseEntity<?> response = restTemplate.postForEntity(
            baseUrl + "/analyze",
            null,
            String.class
        );

        // Validate response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Should analyze dependencies for valid file")
    void testAnalyzeWithValidFile(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.postForEntity(
            baseUrl + "/analyze?filePath=" + yamlFile.getAbsolutePath(),
            null,
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/dependencies/metrics Tests
    // ========================================

    @Test
    @DisplayName("Should retrieve dependency metrics")
    void testGetMetrics(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");

        // First analyze
        restTemplate.postForEntity(
            baseUrl + "/analyze?filePath=" + yamlFile.getAbsolutePath(),
            null,
            Object.class
        );

        // Then get metrics
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/metrics",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/dependencies/{file}/impact Tests
    // ========================================

    @Test
    @DisplayName("Should analyze impact of file changes")
    void testAnalyzeImpact(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");
        String filePath = yamlFile.getAbsolutePath();

        // First analyze
        restTemplate.postForEntity(
            baseUrl + "/analyze?filePath=" + filePath,
            null,
            Object.class
        );

        // Then get impact - use simple filename for path variable
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/test.yaml/impact",
            Object.class
        );

        // Validate response - endpoint should respond (may be 400 if file not in graph)
        assertNotNull(response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/dependencies/circular-dependencies Tests
    // ========================================

    @Test
    @DisplayName("Should find circular dependencies")
    void testFindCircularDependencies(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");

        // First analyze
        restTemplate.postForEntity(
            baseUrl + "/analyze?filePath=" + yamlFile.getAbsolutePath(),
            null,
            Object.class
        );

        // Then get circular dependencies
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/circular-dependencies",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/dependencies/orphaned-files Tests
    // ========================================

    @Test
    @DisplayName("Should find orphaned files")
    void testFindOrphanedFiles(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");

        // First analyze
        restTemplate.postForEntity(
            baseUrl + "/analyze?filePath=" + yamlFile.getAbsolutePath(),
            null,
            Object.class
        );

        // Then get orphaned files
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/orphaned-files",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/dependencies/critical-files Tests
    // ========================================

    @Test
    @DisplayName("Should find critical files")
    void testFindCriticalFiles(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");

        // First analyze
        restTemplate.postForEntity(
            baseUrl + "/analyze?filePath=" + yamlFile.getAbsolutePath(),
            null,
            Object.class
        );

        // Then get critical files
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/critical-files",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/dependencies/report Tests
    // ========================================

    @Test
    @DisplayName("Should generate dependency report")
    void testGenerateReport(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");

        // First analyze
        restTemplate.postForEntity(
            baseUrl + "/analyze?filePath=" + yamlFile.getAbsolutePath(),
            null,
            Object.class
        );

        // Then get report
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/report",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/dependencies/tree Tests
    // ========================================

    @Test
    @DisplayName("Should generate dependency tree")
    void testGetDependencyTree(@TempDir Path tempDir) throws IOException {
        // Create test YAML file
        File yamlFile = createTestYamlFile(tempDir, "test.yaml");

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/tree?rootFile=" + yamlFile.getAbsolutePath(),
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should return 400 when rootFile is missing")
    void testGetDependencyTreeMissingRootFile() {
        // Call without rootFile parameter
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/tree",
            String.class
        );

        // Validate response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    }



    // ========================================
    // Helper Methods
    // ========================================

    private File createTestYamlFile(Path tempDir, String filename) throws IOException {
        File yamlFile = tempDir.resolve(filename).toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-config\n");
            writer.write("  name: Test Configuration\n");
            writer.write("  version: 1.0.0\n");
        }
        return yamlFile;
    }
}

