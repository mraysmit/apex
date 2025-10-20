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

import dev.mars.apex.yaml.manager.model.HealthReport;
import dev.mars.apex.yaml.manager.model.HealthScore;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HealthCheckController.
 * 
 * Tests the REST API endpoints with real service instances and actual HTTP calls.
 * No mocking - uses real HealthCheckService and real YAML files.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("HealthCheckController Integration Tests")
class HealthCheckControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/yaml-manager/api/health-checks";
    }

    // ========================================
    // POST /api/health-checks/check Tests
    // ========================================

    @Test
    @DisplayName("Should perform health check on valid file")
    void testPerformHealthCheckOnValidFile(@TempDir Path tempDir) throws IOException {
        // Create valid YAML file
        File yamlFile = tempDir.resolve("valid.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-health\n");
            writer.write("  name: Test Health\n");
            writer.write("  version: 1.0.0\n");
        }

        // Call actual REST endpoint
        ResponseEntity<HealthScore> response = restTemplate.postForEntity(
            baseUrl + "/check?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            null,
            HealthScore.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getFilePath());
        assertTrue(response.getBody().getOverallScore() >= 0, "Score should be non-negative");
        assertTrue(response.getBody().getOverallScore() <= 100, "Score should be at most 100");
        assertNotNull(response.getBody().getGrade(), "Grade should not be null");
    }

    @Test
    @DisplayName("Should perform health check on file with issues")
    void testPerformHealthCheckOnInvalidFile(@TempDir Path tempDir) throws IOException {
        // Create YAML file without metadata
        File yamlFile = tempDir.resolve("invalid.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("rules:\n");
            writer.write("  - id: rule1\n");
        }

        // Call actual REST endpoint
        ResponseEntity<HealthScore> response = restTemplate.postForEntity(
            baseUrl + "/check?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            null,
            HealthScore.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getOverallScore() >= 0, "Score should be non-negative");
    }

    @Test
    @DisplayName("Should handle missing file in health check")
    void testPerformHealthCheckOnMissingFile() {
        // Call with non-existent file
        ResponseEntity<HealthScore> response = restTemplate.postForEntity(
            baseUrl + "/check?filePath=/nonexistent/file.yaml&baseDir=/tmp",
            null,
            HealthScore.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should return 400 when filePath parameter is missing in health check")
    void testPerformHealthCheckMissingFilePath() {
        // Call without filePath parameter
        ResponseEntity<?> response = restTemplate.postForEntity(
            baseUrl + "/check",
            null,
            String.class
        );

        // Validate response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========================================
    // POST /api/health-checks/report Tests
    // ========================================

    @Test
    @DisplayName("Should generate health report for valid file")
    void testGenerateHealthReportForValidFile(@TempDir Path tempDir) throws IOException {
        // Create valid YAML file
        File yamlFile = tempDir.resolve("report-valid.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-report\n");
            writer.write("  name: Test Report\n");
            writer.write("  version: 1.0.0\n");
        }

        // Call actual REST endpoint
        ResponseEntity<HealthReport> response = restTemplate.postForEntity(
            baseUrl + "/report?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            null,
            HealthReport.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getOverallScore() >= 0, "Overall score should be non-negative");
        assertNotNull(response.getBody().getHealthLevel(), "Health level should not be null");
    }

    @Test
    @DisplayName("Should generate health report with issues")
    void testGenerateHealthReportWithIssues(@TempDir Path tempDir) throws IOException {
        // Create YAML file without metadata
        File yamlFile = tempDir.resolve("report-invalid.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("rules:\n");
            writer.write("  - id: rule1\n");
        }

        // Call actual REST endpoint
        ResponseEntity<HealthReport> response = restTemplate.postForEntity(
            baseUrl + "/report?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            null,
            HealthReport.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // GET /api/health-checks/score Tests
    // ========================================

    @Test
    @DisplayName("Should retrieve health score for valid file")
    void testGetHealthScoreForValidFile(@TempDir Path tempDir) throws IOException {
        // Create valid YAML file
        File yamlFile = tempDir.resolve("score-valid.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-score\n");
            writer.write("  name: Test Score\n");
        }

        // Call actual REST endpoint
        ResponseEntity<HealthScore> response = restTemplate.getForEntity(
            baseUrl + "/score?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            HealthScore.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getOverallScore() >= 0, "Score should be non-negative");
        assertTrue(response.getBody().getOverallScore() <= 100, "Score should be at most 100");
    }

    @Test
    @DisplayName("Should return 400 when filePath parameter is missing in score endpoint")
    void testGetHealthScoreMissingFilePath() {
        // Call without filePath parameter
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/score",
            String.class
        );

        // Validate response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ========================================
    // GET /api/health-checks/is-healthy Tests
    // ========================================

    @Test
    @DisplayName("Should return true for healthy file")
    void testIsHealthyWithValidFile(@TempDir Path tempDir) throws IOException {
        // Create valid YAML file
        File yamlFile = tempDir.resolve("healthy.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n");
            writer.write("  id: test-healthy\n");
            writer.write("  name: Test Healthy\n");
            writer.write("  version: 1.0.0\n");
        }

        // Call actual REST endpoint
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
            baseUrl + "/is-healthy?filePath=" + yamlFile.getAbsolutePath() + 
            "&baseDir=" + tempDir.toAbsolutePath(),
            Boolean.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should handle missing file in is-healthy endpoint")
    void testIsHealthyWithMissingFile() {
        // Call with non-existent file
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
            baseUrl + "/is-healthy?filePath=/nonexistent/file.yaml&baseDir=/tmp",
            Boolean.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should return 400 when filePath parameter is missing in is-healthy endpoint")
    void testIsHealthyMissingFilePath() {
        // Call without filePath parameter
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/is-healthy",
            String.class
        );

        // Validate response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}

