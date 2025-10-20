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

import dev.mars.apex.yaml.manager.model.HealthReport;
import dev.mars.apex.yaml.manager.model.HealthScore;
import dev.mars.apex.yaml.manager.model.Recommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HealthCheckService.
 */
@DisplayName("HealthCheckService Tests")
class HealthCheckServiceTest {

    private HealthCheckService healthCheckService;
    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
        healthCheckService = new HealthCheckService(validationService);
    }

    @Test
    @DisplayName("Should create health score with default values")
    void testHealthScoreDefaults() {
        HealthScore score = new HealthScore("test.yaml");
        
        assertEquals("test.yaml", score.getFilePath());
        assertEquals(100, score.getOverallScore());
        assertEquals(100, score.getStructuralScore());
        assertEquals(100, score.getReferenceScore());
        assertEquals(100, score.getConsistencyScore());
        assertEquals(100, score.getPerformanceScore());
        assertEquals(100, score.getComplianceScore());
        assertEquals(100, score.getMetadataScore());
    }

    @Test
    @DisplayName("Should calculate overall score from component scores")
    void testCalculateOverallScore() {
        HealthScore score = new HealthScore("test.yaml");
        score.setStructuralScore(80);
        score.setReferenceScore(90);
        score.setConsistencyScore(70);
        score.setPerformanceScore(85);
        score.setComplianceScore(75);
        score.setMetadataScore(80);
        
        score.calculateOverallScore();
        
        assertTrue(score.getOverallScore() > 0);
        assertTrue(score.getOverallScore() <= 100);
    }

    @Test
    @DisplayName("Should determine health grade from score")
    void testHealthGrade() {
        HealthScore score = new HealthScore("test.yaml");
        score.setOverallScore(95);
        score.calculateOverallScore();
        
        assertEquals(HealthScore.Grade.EXCELLENT, score.getGrade());
    }

    @Test
    @DisplayName("Should identify healthy configuration")
    void testIsHealthy() {
        HealthScore score = new HealthScore("test.yaml");
        score.setOverallScore(80);
        
        assertTrue(score.isHealthy());
    }

    @Test
    @DisplayName("Should identify unhealthy configuration")
    void testIsUnhealthy() {
        HealthScore score = new HealthScore("test.yaml");
        score.setOverallScore(50);
        
        assertFalse(score.isHealthy());
    }

    @Test
    @DisplayName("Should detect critical issues")
    void testHasCriticalIssues() {
        HealthScore score = new HealthScore("test.yaml");
        score.setOverallScore(30);
        
        assertTrue(score.hasCriticalIssues());
    }

    @Test
    @DisplayName("Should perform health check on valid file")
    void testPerformHealthCheckValid(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n  name: Test\n  version: 1.0.0\n");
        }
        
        HealthScore score = healthCheckService.performHealthCheck(
            yamlFile.getAbsolutePath(), 
            tempDir.toFile().getAbsolutePath()
        );
        
        assertNotNull(score);
        assertEquals(yamlFile.getAbsolutePath(), score.getFilePath());
        assertTrue(score.getOverallScore() > 0);
    }

    @Test
    @DisplayName("Should perform health check on invalid file")
    void testPerformHealthCheckInvalid(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("invalid yaml: [unclosed\n");
        }
        
        HealthScore score = healthCheckService.performHealthCheck(
            yamlFile.getAbsolutePath(), 
            tempDir.toFile().getAbsolutePath()
        );
        
        assertNotNull(score);
        assertTrue(score.getStructuralScore() < 100);
    }

    @Test
    @DisplayName("Should generate recommendations for unhealthy file")
    void testGetRecommendations() {
        HealthScore score = new HealthScore("test.yaml");
        score.setStructuralScore(50);
        score.setReferenceScore(60);
        score.setConsistencyScore(70);
        
        List<Recommendation> recommendations = healthCheckService.getRecommendations(score);
        
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream()
            .anyMatch(r -> r.getId().equals("REC_STRUCTURAL")));
    }

    @Test
    @DisplayName("Should generate health report")
    void testGenerateHealthReport(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("metadata:\n  id: test-id\n  name: Test\n  version: 1.0.0\n");
        }

        HealthReport report = healthCheckService.generateHealthReport(
            yamlFile.getAbsolutePath(),
            tempDir.toFile().getAbsolutePath()
        );

        assertNotNull(report);
        assertTrue(report.getOverallScore() >= 0);
    }

    @Test
    @DisplayName("Should include validation issues in health report")
    void testHealthReportIncludesIssues(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("rule-groups:\n  - id: test\n");
        }
        
        HealthReport report = healthCheckService.generateHealthReport(
            yamlFile.getAbsolutePath(), 
            tempDir.toFile().getAbsolutePath()
        );
        
        assertNotNull(report);
        assertTrue(report.getIssues().size() > 0);
    }

    @Test
    @DisplayName("Should track health score trend")
    void testHealthScoreTrend() {
        HealthScore score = new HealthScore("test.yaml");
        score.setTrend("IMPROVING");
        
        assertEquals("IMPROVING", score.getTrend());
    }

    @Test
    @DisplayName("Should track last checked time")
    void testLastCheckedTime() {
        HealthScore score = new HealthScore("test.yaml");
        long time = System.currentTimeMillis();
        score.setLastCheckedTime(time);
        
        assertEquals(time, score.getLastCheckedTime());
    }

    @Test
    @DisplayName("Should handle missing file in health check")
    void testHealthCheckMissingFile() {
        HealthScore score = healthCheckService.performHealthCheck(
            "/nonexistent/file.yaml", 
            "."
        );
        
        assertNotNull(score);
        assertTrue(score.getStructuralScore() < 100);
    }

    @Test
    @DisplayName("Should calculate score from validation with errors")
    void testScoreCalculationWithErrors(@TempDir Path tempDir) throws IOException {
        File yamlFile = tempDir.resolve("test.yaml").toFile();
        try (FileWriter writer = new FileWriter(yamlFile)) {
            writer.write("invalid: [unclosed\n");
        }
        
        HealthScore score = healthCheckService.performHealthCheck(
            yamlFile.getAbsolutePath(), 
            tempDir.toFile().getAbsolutePath()
        );
        
        assertTrue(score.getStructuralScore() < 100);
    }

    @Test
    @DisplayName("Should provide recommendations with priority levels")
    void testRecommendationPriorities() {
        HealthScore score = new HealthScore("test.yaml");
        score.setStructuralScore(0);
        score.setReferenceScore(0);
        
        List<Recommendation> recommendations = healthCheckService.getRecommendations(score);
        
        assertTrue(recommendations.stream()
            .anyMatch(r -> r.getPriority() == Recommendation.Priority.CRITICAL));
    }
}

