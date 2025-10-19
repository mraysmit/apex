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

import dev.mars.apex.yaml.manager.model.YamlCatalog;
import dev.mars.apex.yaml.manager.model.YamlConfigMetadata;
import dev.mars.apex.yaml.manager.service.CatalogService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CatalogController.
 *
 * Tests the REST API endpoints with real service instances and actual HTTP calls.
 * No mocking - uses real CatalogService and real YAML files.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("CatalogController Integration Tests")
public class CatalogControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CatalogService catalogService;

    private String baseUrl;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/yaml-manager/api/catalog";
    }

    // ========================================
    // GET /api/catalog/configurations Tests
    // ========================================

    @Test
    @DisplayName("Should retrieve all configurations")
    public void testGetAllConfigurations(@TempDir Path tempDir) throws IOException {
        // Create and add test configurations to catalog
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/configurations",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should retrieve specific configuration by id")
    public void testGetConfiguration(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration to catalog
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/configurations/rule-1",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should return 404 for non-existent configuration")
    public void testGetConfigurationNotFound() {
        // Call with non-existent id
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/configurations/nonexistent",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ========================================
    // Search Endpoints Tests
    // ========================================

    @Test
    @DisplayName("Should search configurations by tag")
    public void testSearchByTag(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration with tags
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/search/tag/compliance",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should search configurations by category")
    public void testSearchByCategory(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/search/category/validation",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should search configurations by type")
    public void testSearchByType(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/search/type/rule-config",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should search configurations by author")
    public void testSearchByAuthor(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/search/author/alice",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // Discovery Endpoints Tests
    // ========================================

    @Test
    @DisplayName("Should find unused configurations")
    public void testFindUnused(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/discovery/unused",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should find critical configurations")
    public void testFindCritical(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/discovery/critical",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Should find configurations by health score range")
    public void testFindByHealthScore(@TempDir Path tempDir) throws IOException {
        // Create and add test configuration
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/discovery/health?minScore=80&maxScore=100",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // Statistics Endpoint Tests
    // ========================================

    @Test
    @DisplayName("Should retrieve catalog statistics")
    public void testGetStatistics(@TempDir Path tempDir) throws IOException {
        // Create and add test configurations
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        // Call actual REST endpoint
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/statistics",
            Object.class
        );

        // Validate response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========================================
    // Helper Methods
    // ========================================

    private YamlConfigMetadata createMetadata(String id, String type, String name) {
        YamlConfigMetadata metadata = new YamlConfigMetadata();
        metadata.setId(id);
        metadata.setType(type);
        metadata.setName(name);
        metadata.setPath("/configs/" + id + ".yaml");
        metadata.setDescription("Test configuration for " + name);
        metadata.setAuthor("alice");
        metadata.setVersion("1.0.0");
        metadata.addCategory("validation");
        metadata.addTag("compliance");
        return metadata;
    }
}

