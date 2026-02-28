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
public class Catalog {

    private Map<String, ConfigMetadata> configurations;
    private Map<String, Set<String>> tagIndex;
    private Map<String, Set<String>> typeIndex;
    private Map<String, Set<String>> authorIndex;
    private Map<String, Set<String>> businessDomainIndex;
    private Map<String, Set<String>> ownerIndex;
    private LocalDateTime lastUpdated;
    private int totalConfigurations;
    private int orphanedCount;
    private int criticalCount;
    private double averageHealthScore;

    public Catalog() {
        this.configurations = new HashMap<>();
        this.tagIndex = new HashMap<>();
        this.typeIndex = new HashMap<>();
        this.authorIndex = new HashMap<>();
        this.businessDomainIndex = new HashMap<>();
        this.ownerIndex = new HashMap<>();
        this.lastUpdated = LocalDateTime.now();
    }

    // Core operations

    public void addConfiguration(ConfigMetadata metadata) {
        configurations.put(metadata.getId(), metadata);
        indexConfiguration(metadata);
        updateStatistics();
    }

    public void removeConfiguration(String id) {
        ConfigMetadata metadata = configurations.remove(id);
        if (metadata != null) {
            deindexConfiguration(metadata);
            updateStatistics();
        }
    }

    public ConfigMetadata getConfiguration(String id) {
        return configurations.get(id);
    }

    public Collection<ConfigMetadata> getAllConfigurations() {
        return configurations.values();
    }

    // Indexing operations

    private void indexConfiguration(ConfigMetadata metadata) {
        // Index by tags
        for (String tag : metadata.getTags()) {
            tagIndex.computeIfAbsent(tag, k -> new HashSet<>()).add(metadata.getId());
        }

        // Index by type
        typeIndex.computeIfAbsent(metadata.getType(), k -> new HashSet<>()).add(metadata.getId());

        // Index by author
        if (metadata.getAuthor() != null) {
            authorIndex.computeIfAbsent(metadata.getAuthor(), k -> new HashSet<>()).add(metadata.getId());
        }

        // Index by business domain
        if (metadata.getBusinessDomain() != null) {
            businessDomainIndex.computeIfAbsent(metadata.getBusinessDomain(), k -> new HashSet<>()).add(metadata.getId());
        }

        // Index by owner
        if (metadata.getOwner() != null) {
            ownerIndex.computeIfAbsent(metadata.getOwner(), k -> new HashSet<>()).add(metadata.getId());
        }
    }

    private void deindexConfiguration(ConfigMetadata metadata) {
        // Remove from tag index
        for (String tag : metadata.getTags()) {
            Set<String> ids = tagIndex.get(tag);
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

        // Remove from business domain index
        if (metadata.getBusinessDomain() != null) {
            Set<String> domainIds = businessDomainIndex.get(metadata.getBusinessDomain());
            if (domainIds != null) {
                domainIds.remove(metadata.getId());
            }
        }

        // Remove from owner index
        if (metadata.getOwner() != null) {
            Set<String> ownerIds = ownerIndex.get(metadata.getOwner());
            if (ownerIds != null) {
                ownerIds.remove(metadata.getId());
            }
        }
    }

    // Query operations

    /**
     * Find configurations by metadata attribute value.
     * Supports: tag, type, author, business-domain, owner
     */
    public List<ConfigMetadata> findByMetadataAttribute(String attributeName, String value) {
        if (attributeName == null || attributeName.trim().isEmpty() || value == null) {
            return new ArrayList<>();
        }

        String attr = attributeName.toLowerCase().trim();
        Map<String, Set<String>> index = switch (attr) {
            case "tag", "tags" -> tagIndex;
            case "type", "types" -> typeIndex;
            case "author", "authors" -> authorIndex;
            case "business-domain", "businessdomain", "domain" -> businessDomainIndex;
            case "owner", "owners" -> ownerIndex;
            default -> null;
        };

        if (index == null) {
            return new ArrayList<>();
        }

        Set<String> ids = index.getOrDefault(value, new HashSet<>());
        return ids.stream()
                .map(configurations::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ConfigMetadata> findByTag(String tag) {
        return findByMetadataAttribute("tag", tag);
    }

    public List<ConfigMetadata> findByType(String type) {
        return findByMetadataAttribute("type", type);
    }

    public List<ConfigMetadata> findByAuthor(String author) {
        return findByMetadataAttribute("author", author);
    }

    public List<ConfigMetadata> findUnused() {
        return configurations.values().stream()
                .filter(ConfigMetadata::isOrphaned)
                .toList();
    }

    public List<ConfigMetadata> findCritical() {
        return configurations.values().stream()
                .filter(ConfigMetadata::isCritical)
                .toList();
    }

    public List<ConfigMetadata> findByHealthScore(int minScore, int maxScore) {
        return configurations.values().stream()
                .filter(m -> m.getHealthScore() >= minScore && m.getHealthScore() <= maxScore)
                .toList();
    }

    /**
     * Search configurations across all metadata fields.
     * Searches in: id, name, description, type, author, tags, categories, path, dependencies.
     *
     * @param query The search query (case-insensitive)
     * @return List of configurations matching the query
     */
    public List<ConfigMetadata> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String lowerQuery = query.toLowerCase().trim();

        return configurations.values().stream()
                .filter(m -> matchesQuery(m, lowerQuery))
                .toList();
    }

    private boolean matchesQuery(ConfigMetadata metadata, String lowerQuery) {
        // Search in ID
        if (metadata.getId() != null && metadata.getId().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in name
        if (metadata.getName() != null && metadata.getName().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in description
        if (metadata.getDescription() != null && metadata.getDescription().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in type
        if (metadata.getType() != null && metadata.getType().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in author
        if (metadata.getAuthor() != null && metadata.getAuthor().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in version
        if (metadata.getVersion() != null && metadata.getVersion().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in path
        if (metadata.getPath() != null && metadata.getPath().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in tags
        if (metadata.getTags() != null && metadata.getTags().stream()
                .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))) {
            return true;
        }

        // Search in business domain
        if (metadata.getBusinessDomain() != null && metadata.getBusinessDomain().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in owner
        if (metadata.getOwner() != null && metadata.getOwner().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in dependencies
        if (metadata.getDependencies() != null && metadata.getDependencies().stream()
                .anyMatch(dep -> dep.toLowerCase().contains(lowerQuery))) {
            return true;
        }

        // Search in referenced IDs
        if (metadata.getReferencedIds() != null && metadata.getReferencedIds().stream()
                .anyMatch(ref -> ref.toLowerCase().contains(lowerQuery))) {
            return true;
        }

        return false;
    }

    // Statistics

    private void updateStatistics() {
        this.totalConfigurations = configurations.size();
        this.orphanedCount = (int) configurations.values().stream()
                .filter(ConfigMetadata::isOrphaned)
                .count();
        this.criticalCount = (int) configurations.values().stream()
                .filter(ConfigMetadata::isCritical)
                .count();
        this.averageHealthScore = configurations.values().stream()
                .mapToInt(ConfigMetadata::getHealthScore)
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

    public Set<String> getAllTypes() {
        return new HashSet<>(typeIndex.keySet());
    }

    public Set<String> getAllAuthors() {
        return new HashSet<>(authorIndex.keySet());
    }

    public Set<String> getAllBusinessDomains() {
        return new HashSet<>(businessDomainIndex.keySet());
    }

    public Set<String> getAllOwners() {
        return new HashSet<>(ownerIndex.keySet());
    }

    /**
     * Get all distinct values for a specific metadata attribute.
     *
     * Supported attributes:
     * - "tags" - All distinct tags
     * - "types" - All distinct types
     * - "authors" - All distinct authors
     * - "business-domain" - All distinct business domains
     * - "owner" - All distinct owners
     * - "versions" - All distinct versions
     * - "ids" - All distinct IDs
     * - "names" - All distinct names
     * - "descriptions" - All distinct descriptions
     * - "paths" - All distinct paths
     *
     * @param attributeName The name of the metadata attribute
     * @return Set of distinct values for the attribute, or empty set if attribute not found
     */
    public Set<String> getDistinctValues(String attributeName) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            return new HashSet<>();
        }

        String attr = attributeName.toLowerCase().trim();

        return switch (attr) {
            case "tags", "tag" -> getAllTags();
            case "types", "type" -> getAllTypes();
            case "authors", "author" -> getAllAuthors();
            case "business-domain", "businessdomain", "domain" -> getAllBusinessDomains();
            case "owner", "owners" -> getAllOwners();
            case "versions", "version" -> getDistinctVersions();
            case "ids", "id" -> getDistinctIds();
            case "names", "name" -> getDistinctNames();
            case "descriptions", "description" -> getDistinctDescriptions();
            case "paths", "path" -> getDistinctPaths();
            default -> new HashSet<>();
        };
    }

    private Set<String> getDistinctVersions() {
        return configurations.values().stream()
                .map(ConfigMetadata::getVersion)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> getDistinctIds() {
        return new HashSet<>(configurations.keySet());
    }

    private Set<String> getDistinctNames() {
        return configurations.values().stream()
                .map(ConfigMetadata::getName)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> getDistinctDescriptions() {
        return configurations.values().stream()
                .map(ConfigMetadata::getDescription)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> getDistinctPaths() {
        return configurations.values().stream()
                .map(ConfigMetadata::getPath)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }
}

