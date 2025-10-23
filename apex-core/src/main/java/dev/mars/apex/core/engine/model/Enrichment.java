package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.constants.SeverityConstants;

import java.util.*;

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

/**
 * Runtime model representing an enrichment with category support and enterprise metadata.
 * 
 * This class provides a runtime representation of enrichments that supports:
 * - Category-based organization and metadata inheritance
 * - Enterprise metadata for audit trails and governance
 * - Immutable design for thread safety
 * - Comprehensive enrichment configuration
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-23
 * @version 1.0
 */
public class Enrichment {
    private final UUID uuid;
    private final String id;
    private final Set<Category> categories;
    private final String name;
    private final String description;
    private final String type; // e.g., "field-enrichment", "lookup-enrichment", "calculation-enrichment"
    private final String targetType;
    private final Boolean enabled;
    private final Integer priority;
    private final String condition;
    private final String severity;
    private final String targetField;
    private final String successCode;
    private final String errorCode;
    private final Object mapToField;  // String or List<String>

    // Enterprise metadata fields
    private String createdBy;
    private String businessDomain;
    private String businessOwner;
    private String sourceSystem;
    private String effectiveDate;
    private String expirationDate;

    /**
     * Create a new enrichment with minimal information.
     *
     * @param name The name of the enrichment
     * @param type The type of enrichment
     */
    public Enrichment(String name, String type) {
        this.uuid = UUID.randomUUID();
        this.id = "E" + UUID.randomUUID().toString().substring(0, 8);
        this.categories = new HashSet<>();
        this.categories.add(new Category("default", 100));
        this.name = name;
        this.description = name; // Use name as description for backward compatibility
        this.type = type;
        this.targetType = null;
        this.enabled = true;
        this.priority = 100; // Default priority
        this.condition = null;
        this.severity = SeverityConstants.INFO;
        this.targetField = null;
        this.successCode = null;
        this.errorCode = null;
        this.mapToField = null;
    }

    /**
     * Create a new enrichment with a single category.
     *
     * @param id The unique identifier of the enrichment
     * @param category The category of the enrichment
     * @param name The name of the enrichment
     * @param description The description of what the enrichment does
     * @param type The type of enrichment
     * @param priority The priority of the enrichment (lower numbers = higher priority)
     */
    public Enrichment(String id, String category, String name, String description,
                     String type, int priority) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>();
        this.categories.add(new Category(category, priority));
        this.name = name;
        this.description = description;
        this.type = type;
        this.targetType = null;
        this.enabled = true;
        this.priority = priority;
        this.condition = null;
        this.severity = SeverityConstants.INFO;
        this.targetField = null;
        this.successCode = null;
        this.errorCode = null;
        this.mapToField = null;
    }

    /**
     * Create a new enrichment with multiple categories.
     *
     * @param id The unique identifier of the enrichment
     * @param categories The set of category objects this enrichment belongs to
     * @param name The name of the enrichment
     * @param description The description of what the enrichment does
     * @param type The type of enrichment
     * @param priority The priority of the enrichment (lower numbers = higher priority)
     */
    public Enrichment(String id, Set<Category> categories, String name, String description,
                     String type, int priority) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>(categories != null ? categories : Collections.emptySet());
        this.name = name;
        this.description = description;
        this.type = type;
        this.targetType = null;
        this.enabled = true;
        this.priority = priority;
        this.condition = null;
        this.severity = SeverityConstants.INFO;
        this.targetField = null;
        this.successCode = null;
        this.errorCode = null;
        this.mapToField = null;
    }

    // Getters
    public UUID getUuid() {
        return uuid;
    }

    public String getId() {
        return id;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getTargetType() {
        return targetType;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getCondition() {
        return condition;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTargetField() {
        return targetField;
    }

    public String getSuccessCode() {
        return successCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getMapToField() {
        return mapToField;
    }

    /**
     * Add a category to this enrichment.
     *
     * @param category The category to add
     */
    public void addCategory(Category category) {
        if (category != null) {
            this.categories.add(category);
        }
    }

    // Enterprise metadata getters and setters
    public String getCreatedBy() {
        return createdBy;
    }

    public Enrichment setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public Enrichment setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
        return this;
    }

    public String getBusinessOwner() {
        return businessOwner;
    }

    public Enrichment setBusinessOwner(String businessOwner) {
        this.businessOwner = businessOwner;
        return this;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public Enrichment setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
        return this;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public Enrichment setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
        return this;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public Enrichment setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    @Override
    public String toString() {
        return "Enrichment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                ", categories=" + categories.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enrichment that = (Enrichment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
