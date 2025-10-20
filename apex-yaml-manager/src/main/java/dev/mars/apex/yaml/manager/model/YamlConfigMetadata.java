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
import java.util.HashSet;
import java.util.Set;

/**
 * Metadata for a YAML configuration file.
 *
 * Captures comprehensive information about a YAML configuration including:
 * - File identification and location
 * - Configuration type and purpose
 * - Dependencies and dependents
 * - Health and usage metrics
 * - Audit information
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class YamlConfigMetadata {

    private String id;
    private String path;
    private String type; // scenario, rule-config, rule-group, enrichment, dataset, etc.
    private String name;
    private String description;
    private String author;
    private String version;
    private LocalDateTime created;
    private LocalDateTime lastModified;
    private Set<String> tags;
    private Set<String> categories;
    private Set<String> dependencies; // Files this config depends on
    private Set<String> dependents; // Files that depend on this config
    private Set<String> referencedIds; // Rule IDs, enrichment IDs referenced
    private Set<String> referencingIds; // Rule IDs, enrichment IDs that reference this
    private int usageCount;
    private int healthScore; // 0-100
    private Set<String> healthIssues;
    private LocalDateTime lastValidated;
    private boolean isOrphaned;
    private boolean isCritical;

    public YamlConfigMetadata() {
        this.tags = new HashSet<>();
        this.categories = new HashSet<>();
        this.dependencies = new HashSet<>();
        this.dependents = new HashSet<>();
        this.referencedIds = new HashSet<>();
        this.referencingIds = new HashSet<>();
        this.healthIssues = new HashSet<>();
        this.usageCount = 0;
        this.healthScore = 50;
        this.isOrphaned = false;
        this.isCritical = false;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<String> getDependents() {
        return dependents;
    }

    public void setDependents(Set<String> dependents) {
        this.dependents = dependents;
    }

    public Set<String> getReferencedIds() {
        return referencedIds;
    }

    public void setReferencedIds(Set<String> referencedIds) {
        this.referencedIds = referencedIds;
    }

    public Set<String> getReferencingIds() {
        return referencingIds;
    }

    public void setReferencingIds(Set<String> referencingIds) {
        this.referencingIds = referencingIds;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = Math.max(0, Math.min(100, healthScore));
    }

    public Set<String> getHealthIssues() {
        return healthIssues;
    }

    public void setHealthIssues(Set<String> healthIssues) {
        this.healthIssues = healthIssues;
    }

    public LocalDateTime getLastValidated() {
        return lastValidated;
    }

    public void setLastValidated(LocalDateTime lastValidated) {
        this.lastValidated = lastValidated;
    }

    public boolean isOrphaned() {
        return isOrphaned;
    }

    public void setOrphaned(boolean orphaned) {
        isOrphaned = orphaned;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    // Utility methods

    public String getHealthLevel() {
        if (healthScore >= 80) {
            return "EXCELLENT";
        } else if (healthScore >= 60) {
            return "GOOD";
        } else if (healthScore >= 40) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    // Utility methods for adding items to collections

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void addCategory(String category) {
        this.categories.add(category);
    }

    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }

    public void addDependent(String dependent) {
        this.dependents.add(dependent);
    }

    public void addReferencedId(String id) {
        this.referencedIds.add(id);
    }

    public void addReferencingId(String id) {
        this.referencingIds.add(id);
    }

    public void addHealthIssue(String issue) {
        this.healthIssues.add(issue);
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    @Override
    public String toString() {
        return "YamlConfigMetadata{" +
                "id='" + id + '\'' +
                ", path='" + path + '\'' +
                ", type='" + type + '\'' +
                ", healthScore=" + healthScore +
                ", healthLevel='" + getHealthLevel() + '\'' +
                ", isOrphaned=" + isOrphaned +
                ", isCritical=" + isCritical +
                '}';
    }
}

