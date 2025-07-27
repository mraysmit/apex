package dev.mars.rulesengine.core.engine.model;

import dev.mars.rulesengine.core.engine.model.metadata.RuleMetadata;
import dev.mars.rulesengine.core.engine.model.metadata.RuleStatus;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * Represents a business rule with a condition, message, and extensible metadata.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Represents a business rule with a condition, message, and extensible metadata.
 *
 * This class now supports comprehensive metadata including:
 * - Audit trail (creation/modification dates and users)
 * - Version information
 * - Business context (owner, domain, purpose)
 * - Technical attributes (complexity, tags, source system)
 * - Extensible custom properties
 */
public class Rule implements RuleBase {
    private final UUID uuid;
    private final String id;
    private final Set<Category> categories;
    private final String name;
    private final String condition;
    private final String message;
    private final String description;
    private final int priority;
    private final RuleMetadata metadata;

    /**
     * Create a new business rule with minimal information.
     * This constructor is provided for compatibility with the legacy Rule class.
     * It automatically generates an ID, uses a default category, and sets a default priority.
     *
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies
     * @param message The message to display when the rule applies
     */
    public Rule(String name, String condition, String message) {
        this.uuid = UUID.randomUUID();
        this.id = "R" + UUID.randomUUID().toString().substring(0, 8);
        this.categories = new HashSet<>();
        this.categories.add(new Category("default", 100));
        this.name = name;
        this.condition = condition;
        this.message = message;
        this.description = message; // Use message as description for backward compatibility
        this.priority = 100; // Default priority
        this.metadata = RuleMetadata.builder()
            .createdByUser("system")
            .build();
    }

    /**
     * Create a new business rule with a single category.
     *
     * @param id The unique identifier of the rule
     * @param category The category of the rule
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies
     * @param message The message to display when the rule applies
     * @param description The description of what the rule does
     * @param priority The priority of the rule (lower numbers = higher priority)
     */
    public Rule(String id, String category, String name, String condition,
                String message, String description, int priority) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>();
        this.categories.add(new Category(category, priority));
        this.name = name;
        this.condition = condition;
        this.message = message;
        this.description = description;
        this.priority = priority;
        this.metadata = RuleMetadata.builder()
            .createdByUser("system")
            .build();
    }

    /**
     * Create a rule from a set of category names.
     * 
     * @param id The unique identifier of the rule
     * @param categoryNames The names of the categories of the rule
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies
     * @param message The message to display when the rule applies
     * @param description The description of what the rule does
     * @param priority The priority of the rule (lower numbers = higher priority)
     * @return A new Rule instance
     */
    public static Rule fromCategoryNames(String id, Set<String> categoryNames, String name, String condition,
                                        String message, String description, int priority) {
        Set<Category> categoryObjects = categoryNames.stream()
            .map(c -> new Category(c, priority))
            .collect(Collectors.toSet());
        return new Rule(id, categoryObjects, name, condition, message, description, priority);
    }

    /**
     * Create a new business rule with a single category object.
     *
     * @param id The unique identifier of the rule
     * @param category The category object of the rule
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies
     * @param message The message to display when the rule applies
     * @param description The description of what the rule does
     * @param priority The priority of the rule (lower numbers = higher priority)
     */
    public Rule(String id, Category category, String name, String condition,
                String message, String description, int priority) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>();
        this.categories.add(category);
        this.name = name;
        this.condition = condition;
        this.message = message;
        this.description = description;
        this.priority = priority;
        this.metadata = RuleMetadata.builder()
            .createdByUser("system")
            .build();
    }

    /**
     * Create a new business rule with multiple category objects.
     *
     * @param id The unique identifier of the rule
     * @param categories The category objects of the rule
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies
     * @param message The message to display when the rule applies
     * @param description The description of what the rule does
     * @param priority The priority of the rule (lower numbers = higher priority)
     */
    public Rule(String id, Set<Category> categories, String name, String condition,
                String message, String description, int priority) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>(categories);
        this.name = name;
        this.condition = condition;
        this.message = message;
        this.description = description;
        this.priority = priority;
        this.metadata = RuleMetadata.builder()
            .createdByUser("system")
            .build();
    }

    /**
     * Create a new business rule with full metadata support.
     * This is the primary constructor for creating rules with comprehensive metadata.
     *
     * @param id The unique identifier of the rule
     * @param categories The category objects of the rule
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies
     * @param message The message to display when the rule applies
     * @param description The description of what the rule does
     * @param priority The priority of the rule (lower numbers = higher priority)
     * @param metadata The extensible metadata for the rule
     */
    public Rule(String id, Set<Category> categories, String name, String condition,
                String message, String description, int priority, RuleMetadata metadata) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>(categories);
        this.name = name;
        this.condition = condition;
        this.message = message;
        this.description = description;
        this.priority = priority;
        this.metadata = metadata != null ? metadata : RuleMetadata.builder().createdByUser("system").build();
    }

    /**
     * Get the unique identifier of the rule.
     * 
     * @return The rule ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the categories of the rule.
     * 
     * @return The rule categories
     */
    public Set<Category> getCategories() {
        return categories;
    }

    /**
     * Add a category to the rule.
     * 
     * @param category The category to add
     */
    public void addCategory(Category category) {
        this.categories.add(category);
    }

    /**
     * Add a category to the rule by name.
     * 
     * @param categoryName The name of the category to add
     * @param sequenceNumber The sequence number of the category
     */
    public void addCategory(String categoryName, int sequenceNumber) {
        this.categories.add(new Category(categoryName, sequenceNumber));
    }

    /**
     * Check if the rule has a specific category.
     * 
     * @param category The category to check
     * @return True if the rule has the category, false otherwise
     */
    public boolean hasCategory(Category category) {
        return this.categories.contains(category);
    }

    /**
     * Check if the rule has a category with the specified name.
     * 
     * @param categoryName The name of the category to check
     * @return True if the rule has a category with the specified name, false otherwise
     */
    public boolean hasCategory(String categoryName) {
        return this.categories.stream().anyMatch(c -> c.getName().equals(categoryName));
    }

    /**
     * Get the name of the rule.
     * 
     * @return The rule name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the condition of the rule.
     * 
     * @return The rule condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Get the message of the rule.
     * 
     * @return The rule message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the description of the rule.
     * 
     * @return The rule description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the priority of the rule.
     * 
     * @return The rule priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get the UUID of the rule.
     *
     * @return The rule UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Get the metadata of the rule.
     *
     * @return The rule metadata
     */
    public RuleMetadata getMetadata() {
        return metadata;
    }

    // === CRITICAL AUDIT CONVENIENCE METHODS ===

    /**
     * Get the creation date - CRITICAL audit attribute.
     * This is ALWAYS available and never null.
     */
    public Instant getCreatedDate() {
        return metadata.getCreatedDate();
    }

    /**
     * Get the modification date - CRITICAL audit attribute.
     * This is ALWAYS available and never null.
     */
    public Instant getModifiedDate() {
        return metadata.getModifiedDate();
    }

    // === OTHER METADATA CONVENIENCE METHODS ===

    /**
     * Check if the rule is currently active and executable.
     */
    public boolean isActive() {
        return metadata.getStatus().isExecutable();
    }

    /**
     * Check if the rule can be modified.
     */
    public boolean isModifiable() {
        return metadata.getStatus().isModifiable();
    }

    /**
     * Get the rule's business owner if specified.
     */
    public Optional<String> getBusinessOwner() {
        return metadata.getBusinessOwner();
    }

    /**
     * Get the rule's tags.
     */
    public String[] getTags() {
        return metadata.getTags();
    }

    /**
     * Get a custom metadata property.
     */
    public <T> Optional<T> getCustomProperty(String key, Class<T> type) {
        return metadata.getCustomProperty(key, type);
    }

    /**
     * Create a new rule instance with updated metadata.
     * This preserves immutability while allowing metadata updates.
     */
    public Rule withMetadata(RuleMetadata newMetadata) {
        return new Rule(id, categories, name, condition, message, description, priority, newMetadata);
    }

    /**
     * Create a new rule instance with a status change.
     */
    public Rule withStatus(RuleStatus newStatus, String modifiedByUser) {
        RuleMetadata updatedMetadata = metadata.withStatus(newStatus, modifiedByUser);
        return withMetadata(updatedMetadata);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", createdDate=" + metadata.getCreatedDate() +      // CRITICAL: Show creation date
                ", modifiedDate=" + metadata.getModifiedDate() +    // CRITICAL: Show modification date
                ", status=" + metadata.getStatus() +
                ", version='" + metadata.getVersion() + '\'' +
                ", priority=" + priority +
                ", condition='" + condition + '\'' +
                ", categories=" + categories.stream().map(Category::getName).collect(Collectors.toList()) +
                '}';
    }
}