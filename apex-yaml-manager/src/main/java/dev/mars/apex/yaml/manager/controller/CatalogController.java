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

import dev.mars.apex.yaml.manager.model.YamlConfigMetadata;
import dev.mars.apex.yaml.manager.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for catalog operations.
 *
 * Provides endpoints for:
 * - Searching and discovering configurations
 * - Querying by tags, categories, types, authors
 * - Finding unused and critical configurations
 * - Accessing catalog statistics
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    /**
     * Get all configurations in the catalog.
     */
    @GetMapping("/configurations")
    public ResponseEntity<Map<String, Object>> getAllConfigurations() {
        Collection<YamlConfigMetadata> configurations = catalogService.getAllConfigurations();
        Map<String, Object> response = new HashMap<>();
        response.put("total", configurations.size());
        response.put("configurations", configurations);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific configuration by ID.
     */
    @GetMapping("/configurations/{id}")
    public ResponseEntity<Map<String, Object>> getConfiguration(@PathVariable String id) {
        YamlConfigMetadata metadata = catalogService.getConfiguration(id);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("configuration", metadata);
        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by tag.
     */
    @GetMapping("/search/tag/{tag}")
    public ResponseEntity<Map<String, Object>> searchByTag(@PathVariable String tag) {
        List<YamlConfigMetadata> results = catalogService.findByTag(tag);
        Map<String, Object> response = new HashMap<>();
        response.put("tag", tag);
        response.put("count", results.size());
        response.put("configurations", results);
        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by category.
     */
    @GetMapping("/search/category/{category}")
    public ResponseEntity<Map<String, Object>> searchByCategory(@PathVariable String category) {
        List<YamlConfigMetadata> results = catalogService.findByCategory(category);
        Map<String, Object> response = new HashMap<>();
        response.put("category", category);
        response.put("count", results.size());
        response.put("configurations", results);
        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by type.
     */
    @GetMapping("/search/type/{type}")
    public ResponseEntity<Map<String, Object>> searchByType(@PathVariable String type) {
        List<YamlConfigMetadata> results = catalogService.findByType(type);
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("count", results.size());
        response.put("configurations", results);
        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by author.
     */
    @GetMapping("/search/author/{author}")
    public ResponseEntity<Map<String, Object>> searchByAuthor(@PathVariable String author) {
        List<YamlConfigMetadata> results = catalogService.findByAuthor(author);
        Map<String, Object> response = new HashMap<>();
        response.put("author", author);
        response.put("count", results.size());
        response.put("configurations", results);
        return ResponseEntity.ok(response);
    }

    /**
     * Find unused configurations.
     */
    @GetMapping("/discovery/unused")
    public ResponseEntity<Map<String, Object>> findUnused() {
        List<YamlConfigMetadata> unused = catalogService.findUnused();
        Map<String, Object> response = new HashMap<>();
        response.put("count", unused.size());
        response.put("configurations", unused);
        return ResponseEntity.ok(response);
    }

    /**
     * Find critical configurations.
     */
    @GetMapping("/discovery/critical")
    public ResponseEntity<Map<String, Object>> findCritical() {
        List<YamlConfigMetadata> critical = catalogService.findCritical();
        Map<String, Object> response = new HashMap<>();
        response.put("count", critical.size());
        response.put("configurations", critical);
        return ResponseEntity.ok(response);
    }

    /**
     * Find configurations by health score range.
     */
    @GetMapping("/discovery/health")
    public ResponseEntity<Map<String, Object>> findByHealthScore(
            @RequestParam(defaultValue = "0") int minScore,
            @RequestParam(defaultValue = "100") int maxScore) {
        List<YamlConfigMetadata> results = catalogService.findByHealthScore(minScore, maxScore);
        Map<String, Object> response = new HashMap<>();
        response.put("minScore", minScore);
        response.put("maxScore", maxScore);
        response.put("count", results.size());
        response.put("configurations", results);
        return ResponseEntity.ok(response);
    }

    /**
     * Get catalog statistics.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalConfigurations", catalogService.getTotalConfigurations());
        response.put("orphanedCount", catalogService.getOrphanedCount());
        response.put("criticalCount", catalogService.getCriticalCount());
        response.put("averageHealthScore", catalogService.getAverageHealthScore());
        response.put("allTags", catalogService.getCatalog().getAllTags());
        response.put("allCategories", catalogService.getCatalog().getAllCategories());
        response.put("allTypes", catalogService.getCatalog().getAllTypes());
        response.put("allAuthors", catalogService.getCatalog().getAllAuthors());
        return ResponseEntity.ok(response);
    }
}

