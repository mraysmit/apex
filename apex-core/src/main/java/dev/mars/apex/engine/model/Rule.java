package dev.mars.apex.engine.model;

import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.model.metadata.RuleMetadata;
import dev.mars.apex.engine.model.metadata.RuleStatus;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
 * <p>This class is part of the APEX rules engine for Java applications.
 *
 * <p>Supports comprehensive metadata including:
 * <ul>
 *   <li>Audit trail (creation/modification dates and users)</li>
 *   <li>Version information</li>
 *   <li>Business context (owner, domain, purpose)</li>
 *   <li>Technical attributes (complexity, tags, source system)</li>
 *   <li>Extensible custom properties</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class Rule implements RuleBase {
    private final UUID uuid;
    private final String id;
    private final Set<Category> categories;
    private final String name;
    private final String condition;
    private final String message;
    private final String noMatchMessage;
    private final String description;
    private final String severity;
    private final int priority;
    private final RuleMetadata metadata;

    // Default value for error recovery
    private final Object defaultValue;

    private final String successCode;
    private final String errorCode;
    private final List<String> mapToField;
    private final String resultField;
    private final boolean enabled;  

    // Structured condition group (mutually exclusive with string condition)
    private final SharedConditionGroup conditions;

    /**
     * Create a new business rule with full metadata support including enabled flag.
     * This is the canonical constructor that all other constructors ultimately delegate to.
     *
     * @param id The unique identifier of the rule
     * @param categories The category objects of the rule
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies
     * @param message The message to display when the rule matches (condition=true)
     * @param description The description of what the rule does
     * @param priority The priority of the rule (lower numbers = higher priority)
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param metadata The extensible metadata for the rule
     * @param defaultValue The default value to use for error recovery (null if no default)
     * @param successCode The code to use when rule succeeds (null if no code)
     * @param errorCode The code to use when rule fails (null if no code)
     * @param mapToField Field mapping expressions for enriching data (null if no mapping)
     * @param resultField Field name where the boolean condition result will be stored (null if no storage)
     * @param noMatchMessage The message to display when the rule does not match (condition=false), null to use message
     * @param enabled Whether the rule is enabled for evaluation (false = skip evaluation)
     */
    public Rule(String id, Set<Category> categories, String name, String condition,
                String message, String description, int priority, String severity,
                RuleMetadata metadata, Object defaultValue, String successCode, String errorCode,
                List<String> mapToField, String resultField, String noMatchMessage, boolean enabled) {
        this(id, categories, name, condition, message, description, priority, severity,
             metadata, defaultValue, successCode, errorCode, mapToField, resultField,
             noMatchMessage, enabled, null);
    }

    /**
     * Create a new business rule with full metadata support including structured conditions.
     * Extended constructor that accepts a {@link SharedConditionGroup} for structured condition evaluation.
     *
     * @param id The unique identifier of the rule
     * @param categories The category objects of the rule
     * @param name The name of the rule
     * @param condition The SpEL condition that determines if the rule applies (may be null if conditions is set)
     * @param message The message to display when the rule matches (condition=true)
     * @param description The description of what the rule does
     * @param priority The priority of the rule (lower numbers = higher priority)
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param metadata The extensible metadata for the rule
     * @param defaultValue The default value to use for error recovery (null if no default)
     * @param successCode The code to use when rule succeeds (null if no code)
     * @param errorCode The code to use when rule fails (null if no code)
     * @param mapToField Field mapping expressions for enriching data (null if no mapping)
     * @param resultField Field name where the boolean condition result will be stored (null if no storage)
     * @param noMatchMessage The message to display when the rule does not match (condition=false), null to use message
     * @param enabled Whether the rule is enabled for evaluation (false = skip evaluation)
     * @param conditions The structured condition group (null if using string condition)
     */
    public Rule(String id, Set<Category> categories, String name, String condition,
                String message, String description, int priority, String severity,
                RuleMetadata metadata, Object defaultValue, String successCode, String errorCode,
                List<String> mapToField, String resultField, String noMatchMessage, boolean enabled,
                SharedConditionGroup conditions) {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.categories = new HashSet<>(categories);
        this.name = name;
        this.condition = condition;
        this.message = message;
        this.description = description;
        this.severity = severity != null ? severity : SeverityConstants.INFO; // Default to INFO if null
        this.priority = priority;
        this.defaultValue = defaultValue;
        this.successCode = successCode;
        this.errorCode = errorCode;
        this.mapToField = mapToField;
        this.resultField = resultField;
        this.noMatchMessage = noMatchMessage;
        this.enabled = enabled;
        this.conditions = conditions;
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
     * Get the structured condition group for this rule.
     * Mutually exclusive with {@link #getCondition()} — only one will be non-null.
     *
     * @return The structured condition group, or null if using a simple string condition
     */
    public SharedConditionGroup getConditions() {
        return conditions;
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
     * Get the no-match message of the rule.
     * This is the message displayed when the rule condition evaluates to false (no match).
     * If null, the standard message is used for both match and no-match outcomes.
     * Supports the same {{#expression}} and #{expression} placeholder formats as message.
     *
     * @return The no-match message, or null if the standard message should be used
     */
    public String getNoMatchMessage() {
        return noMatchMessage;
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
     * Get the severity of the rule.
     *
     * @return The rule severity (ERROR, WARNING, INFO)
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Get the default value for error recovery.
     * Phase 3A Enhancement: Returns the default value to use when rule evaluation fails.
     *
     * @return The default value for error recovery, or null if no default is specified
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get the success code for this rule.
     * Phase 4 Enhancement: Returns the code to use when rule condition evaluates to true.
     *
     * @return The success code (constant or SpEL expression), or null if no code is specified
     */
    public String getSuccessCode() {
        return successCode;
    }

    /**
     * Get the error code for this rule.
     * Phase 4 Enhancement: Returns the code to use when rule condition evaluates to false.
     *
     * @return The error code (constant or SpEL expression), or null if no code is specified
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Get the field mapping expressions for this rule.
     * Phase 4 Enhancement: Returns field mapping expressions to enrich the dataset.
     *
     * @return The field mapping expressions, or null if no mapping is specified
     */
    public List<String> getMapToField() {
        return mapToField;
    }

    /**
     * Get the result field name for this rule.
     * Phase 5 Enhancement: Returns the field name where the boolean condition result will be stored.
     *
     * @return The result field name, or null if no result storage is configured
     */
    public String getResultField() {
        return resultField;
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
     * Check if the rule is enabled for evaluation.
     * Disabled rules are skipped during evaluation without producing errors.
     *
     * @return true if the rule is enabled (default), false if disabled
     */
    public boolean isEnabled() {
        return enabled;
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
        return new Rule(id, categories, name, condition, message, description, priority, severity,
                       newMetadata, defaultValue, successCode, errorCode, mapToField, resultField, noMatchMessage, this.enabled,
                       this.conditions);
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
