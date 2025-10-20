package dev.mars.apex.yaml.manager.model;

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

import java.time.LocalDateTime;
import java.util.*;

/**
 * Central catalog of all YAML configurations.
 *
 * Maintains a comprehensive index of all YAML configurations with:
 * - Metadata for each configuration
 * - Multiple indices for efficient searching
 * - Discovery and query capabilities
 * - Catalog statistics and health metrics
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class YamlCatalog {

    private Map<String, YamlConfigMetadata> configurations;
    private Map<String, Set<String>> tagIndex;
    private Map<String, Set<String>> categoryIndex;
    private Map<String, Set<String>> typeIndex;
    private Map<String, Set<String>> authorIndex;
    private LocalDateTime lastUpdated;
    private int totalConfigurations;
    private int orphanedCount;
    private int criticalCount;
    private double averageHealthScore;

    public YamlCatalog() {
        this.configurations = new HashMap<>();
        this.tagIndex = new HashMap<>();
        this.categoryIndex = new HashMap<>();
        this.typeIndex = new HashMap<>();
        this.authorIndex = new HashMap<>();
        this.lastUpdated = LocalDateTime.now();
    }

    // Core operations

    public void addConfiguration(YamlConfigMetadata metadata) {
        configurations.put(metadata.getId(), metadata);
        indexConfiguration(metadata);
        updateStatistics();
    }

    public void removeConfiguration(String id) {
        YamlConfigMetadata metadata = configurations.remove(id);
        if (metadata != null) {
            deindexConfiguration(metadata);
            updateStatistics();
        }
    }

    public YamlConfigMetadata getConfiguration(String id) {
        return configurations.get(id);
    }

    public Collection<YamlConfigMetadata> getAllConfigurations() {
        return configurations.values();
    }

    // Indexing operations

    private void indexConfiguration(YamlConfigMetadata metadata) {
        // Index by tags
        for (String tag : metadata.getTags()) {
            tagIndex.computeIfAbsent(tag, k -> new HashSet<>()).add(metadata.getId());
        }

        // Index by categories
        for (String category : metadata.getCategories()) {
            categoryIndex.computeIfAbsent(category, k -> new HashSet<>()).add(metadata.getId());
        }

        // Index by type
        typeIndex.computeIfAbsent(metadata.getType(), k -> new HashSet<>()).add(metadata.getId());

        // Index by author
        if (metadata.getAuthor() != null) {
            authorIndex.computeIfAbsent(metadata.getAuthor(), k -> new HashSet<>()).add(metadata.getId());
        }
    }

    private void deindexConfiguration(YamlConfigMetadata metadata) {
        // Remove from tag index
        for (String tag : metadata.getTags()) {
            Set<String> ids = tagIndex.get(tag);
            if (ids != null) {
                ids.remove(metadata.getId());
            }
        }

        // Remove from category index
        for (String category : metadata.getCategories()) {
            Set<String> ids = categoryIndex.get(category);
            if (ids != null) {
                ids.remove(metadata.getId());
            }
        }

        // Remove from type index
        Set<String> typeIds = typeIndex.get(metadata.getType());
        if (typeIds != null) {
            typeIds.remove(metadata.getId());
        }

        // Remove from author index
        if (metadata.getAuthor() != null) {
            Set<String> authorIds = authorIndex.get(metadata.getAuthor());
            if (authorIds != null) {
                authorIds.remove(metadata.getId());
            }
        }
    }

    // Query operations

    public List<YamlConfigMetadata> findByTag(String tag) {
        Set<String> ids = tagIndex.getOrDefault(tag, new HashSet<>());
        return ids.stream()
                .map(configurations::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<YamlConfigMetadata> findByCategory(String category) {
        Set<String> ids = categoryIndex.getOrDefault(category, new HashSet<>());
        return ids.stream()
                .map(configurations::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<YamlConfigMetadata> findByType(String type) {
        Set<String> ids = typeIndex.getOrDefault(type, new HashSet<>());
        return ids.stream()
                .map(configurations::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<YamlConfigMetadata> findByAuthor(String author) {
        Set<String> ids = authorIndex.getOrDefault(author, new HashSet<>());
        return ids.stream()
                .map(configurations::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<YamlConfigMetadata> findUnused() {
        return configurations.values().stream()
                .filter(YamlConfigMetadata::isOrphaned)
                .toList();
    }

    public List<YamlConfigMetadata> findCritical() {
        return configurations.values().stream()
                .filter(YamlConfigMetadata::isCritical)
                .toList();
    }

    public List<YamlConfigMetadata> findByHealthScore(int minScore, int maxScore) {
        return configurations.values().stream()
                .filter(m -> m.getHealthScore() >= minScore && m.getHealthScore() <= maxScore)
                .toList();
    }

    // Statistics

    private void updateStatistics() {
        this.totalConfigurations = configurations.size();
        this.orphanedCount = (int) configurations.values().stream()
                .filter(YamlConfigMetadata::isOrphaned)
                .count();
        this.criticalCount = (int) configurations.values().stream()
                .filter(YamlConfigMetadata::isCritical)
                .count();
        this.averageHealthScore = configurations.values().stream()
                .mapToInt(YamlConfigMetadata::getHealthScore)
                .average()
                .orElse(0.0);
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters

    public int getTotalConfigurations() {
        return totalConfigurations;
    }

    public int getOrphanedCount() {
        return orphanedCount;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public double getAverageHealthScore() {
        return averageHealthScore;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public Set<String> getAllTags() {
        return new HashSet<>(tagIndex.keySet());
    }

    public Set<String> getAllCategories() {
        return new HashSet<>(categoryIndex.keySet());
    }

    public Set<String> getAllTypes() {
        return new HashSet<>(typeIndex.keySet());
    }

    public Set<String> getAllAuthors() {
        return new HashSet<>(authorIndex.keySet());
    }
}

