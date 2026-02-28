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

import dev.mars.apex.yaml.manager.model.ConfigMetadata;
import dev.mars.apex.yaml.manager.service.CatalogScanService;
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

    @Autowired
    private CatalogScanService catalogScanService;

    /**
     * Scan a directory and index all YAML files into the catalog.
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanDirectory(@RequestParam String directory) {
        Map<String, Object> result = catalogScanService.scanDirectory(directory);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all configurations in the catalog.
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields (id, name, type, author, etc.)
     *               without analysis results (dependencies, health scores, etc.)
     */
    @GetMapping("/configurations")
    public ResponseEntity<Map<String, Object>> getAllConfigurations(
            @RequestParam(required = false) String fields) {
        Collection<ConfigMetadata> configurations = catalogService.getAllConfigurations();
        Map<String, Object> response = new HashMap<>();
        response.put("total", configurations.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(configurations));
        } else {
            response.put("configurations", configurations);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific configuration by ID.
     */
    @GetMapping("/configurations/{id}")
    public ResponseEntity<Map<String, Object>> getConfiguration(@PathVariable String id) {
        ConfigMetadata metadata = catalogService.getConfiguration(id);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("configuration", metadata);
        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by tag.
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields.
     */
    @GetMapping("/search/tag/{tag}")
    public ResponseEntity<Map<String, Object>> searchByTag(
            @PathVariable String tag,
            @RequestParam(required = false) String fields) {
        List<ConfigMetadata> results = catalogService.findByTag(tag);
        Map<String, Object> response = new HashMap<>();
        response.put("tag", tag);
        response.put("count", results.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(results));
        } else {
            response.put("configurations", results);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by metadata attribute (tag, type, author, business-domain, owner).
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields.
     */
    @GetMapping("/search/attribute/{attributeName}/{value}")
    public ResponseEntity<Map<String, Object>> searchByMetadataAttribute(
            @PathVariable String attributeName,
            @PathVariable String value,
            @RequestParam(required = false) String fields) {
        List<ConfigMetadata> results = catalogService.findByMetadataAttribute(attributeName, value);
        Map<String, Object> response = new HashMap<>();
        response.put("attributeName", attributeName);
        response.put("value", value);
        response.put("count", results.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(results));
        } else {
            response.put("configurations", results);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by type.
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields.
     */
    @GetMapping("/search/type/{type}")
    public ResponseEntity<Map<String, Object>> searchByType(
            @PathVariable String type,
            @RequestParam(required = false) String fields) {
        List<ConfigMetadata> results = catalogService.findByType(type);
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("count", results.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(results));
        } else {
            response.put("configurations", results);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Search configurations by author.
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields.
     */
    @GetMapping("/search/author/{author}")
    public ResponseEntity<Map<String, Object>> searchByAuthor(
            @PathVariable String author,
            @RequestParam(required = false) String fields) {
        List<ConfigMetadata> results = catalogService.findByAuthor(author);
        Map<String, Object> response = new HashMap<>();
        response.put("author", author);
        response.put("count", results.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(results));
        } else {
            response.put("configurations", results);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Find unused configurations.
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields.
     */
    @GetMapping("/discovery/unused")
    public ResponseEntity<Map<String, Object>> findUnused(
            @RequestParam(required = false) String fields) {
        List<ConfigMetadata> unused = catalogService.findUnused();
        Map<String, Object> response = new HashMap<>();
        response.put("count", unused.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(unused));
        } else {
            response.put("configurations", unused);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Find critical configurations.
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields.
     */
    @GetMapping("/discovery/critical")
    public ResponseEntity<Map<String, Object>> findCritical(
            @RequestParam(required = false) String fields) {
        List<ConfigMetadata> critical = catalogService.findCritical();
        Map<String, Object> response = new HashMap<>();
        response.put("count", critical.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(critical));
        } else {
            response.put("configurations", critical);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Find configurations by health score range.
     *
     * @param fields Optional parameter to control which fields to return.
     *               Use "metadata" to return only core metadata fields.
     */
    @GetMapping("/discovery/health")
    public ResponseEntity<Map<String, Object>> findByHealthScore(
            @RequestParam(defaultValue = "0") int minScore,
            @RequestParam(defaultValue = "100") int maxScore,
            @RequestParam(required = false) String fields) {
        List<ConfigMetadata> results = catalogService.findByHealthScore(minScore, maxScore);
        Map<String, Object> response = new HashMap<>();
        response.put("minScore", minScore);
        response.put("maxScore", maxScore);
        response.put("count", results.size());

        if ("metadata".equalsIgnoreCase(fields)) {
            response.put("configurations", extractMetadataOnly(results));
        } else {
            response.put("configurations", results);
        }

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
        response.put("allBusinessDomains", catalogService.getCatalog().getAllBusinessDomains());
        response.put("allOwners", catalogService.getCatalog().getAllOwners());
        response.put("allTypes", catalogService.getCatalog().getAllTypes());
        response.put("allAuthors", catalogService.getCatalog().getAllAuthors());
        return ResponseEntity.ok(response);
    }

    /**
     * Extract only core metadata fields from configurations.
     * Returns a simplified view with just the metadata fields, excluding analysis results.
     *
     * @param configurations Collection of full ConfigMetadata objects
     * @return List of maps containing only metadata fields
     */
    private List<Map<String, Object>> extractMetadataOnly(Collection<ConfigMetadata> configurations) {
        List<Map<String, Object>> metadataList = new ArrayList<>();

        for (ConfigMetadata config : configurations) {
            Map<String, Object> metadata = new HashMap<>();

            // Core identification
            metadata.put("id", config.getId());
            metadata.put("name", config.getName());
            metadata.put("description", config.getDescription());
            metadata.put("type", config.getType());
            metadata.put("version", config.getVersion());
            metadata.put("path", config.getPath());

            // Classification metadata
            metadata.put("author", config.getAuthor());
            metadata.put("businessDomain", config.getBusinessDomain());
            metadata.put("owner", config.getOwner());
            metadata.put("tags", config.getTags());

            // Timestamps
            metadata.put("created", config.getCreated());
            metadata.put("lastModified", config.getLastModified());

            metadataList.add(metadata);
        }

        return metadataList;
    }
}

