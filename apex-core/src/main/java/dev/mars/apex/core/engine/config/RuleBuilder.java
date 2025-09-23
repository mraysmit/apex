package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.metadata.RuleMetadata;
import dev.mars.apex.core.engine.model.metadata.RuleStatus;
import dev.mars.apex.core.engine.model.metadata.RuleComplexity;

import java.time.Instant;
import java.util.HashSet;
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
 * Builder class for creating Rule instances with comprehensive metadata support.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Builder class for creating Rule instances with comprehensive metadata support.
 * This provides a fluent API for constructing rules with various properties and extensible metadata.
 */
public class RuleBuilder {
    private String id;
    private Set<Category> categories = new HashSet<>();
    private String name;
    private String condition;
    private String message;
    private String description;
    private int priority = 100; // Default priority
    private String severity = SeverityConstants.INFO; // Default severity

    // Metadata builder
    private RuleMetadata.Builder metadataBuilder = RuleMetadata.builder();

    // Reference to the configuration for automatic registration
    private RulesEngineConfiguration configuration;

    /**
     * Create a new RuleBuilder with a generated ID.
     */
    public RuleBuilder() {
        this.id = "R" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Create a new RuleBuilder with the specified ID.
     *
     * @param id The unique identifier for the rule
     */
    public RuleBuilder(String id) {
        this.id = id;
    }

    /**
     * Create a new RuleBuilder with the specified ID and configuration for automatic registration.
     *
     * @param id The unique identifier for the rule
     * @param configuration The configuration to register the rule with when built
     */
    public RuleBuilder(String id, RulesEngineConfiguration configuration) {
        this.id = id;
        this.configuration = configuration;
    }

    /**
     * Set the ID for the rule.
     *
     * @param id The unique identifier for the rule
     * @return This builder for method chaining
     */
    public RuleBuilder withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Add a category to the rule.
     *
     * @param category The category to add
     * @return This builder for method chaining
     */
    public RuleBuilder withCategory(Category category) {
        this.categories.add(category);
        return this;
    }

    /**
     * Add a category to the rule by name.
     *
     * @param categoryName The name of the category to add
     * @return This builder for method chaining
     */
    public RuleBuilder withCategory(String categoryName) {
        this.categories.add(new Category(categoryName, priority));
        return this;
    }

    /**
     * Set the categories for the rule, replacing any existing categories.
     *
     * @param categories The set of categories for the rule
     * @return This builder for method chaining
     */
    public RuleBuilder withCategories(Set<Category> categories) {
        this.categories = new HashSet<>(categories);
        return this;
    }

    /**
     * Set the categories for the rule by name, replacing any existing categories.
     *
     * @param categoryNames The set of category names for the rule
     * @return This builder for method chaining
     */
    public RuleBuilder withCategoryNames(Set<String> categoryNames) {
        this.categories = categoryNames.stream()
            .map(name -> new Category(name, priority))
            .collect(Collectors.toSet());
        return this;
    }

    /**
     * Set the name for the rule.
     *
     * @param name The name of the rule
     * @return This builder for method chaining
     */
    public RuleBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the condition for the rule.
     *
     * @param condition The SpEL condition that determines if the rule applies
     * @return This builder for method chaining
     */
    public RuleBuilder withCondition(String condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Set the message for the rule.
     *
     * @param message The message to display when the rule applies
     * @return This builder for method chaining
     */
    public RuleBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Set the description for the rule.
     *
     * @param description The description of what the rule does
     * @return This builder for method chaining
     */
    public RuleBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the priority for the rule.
     *
     * @param priority The priority of the rule (lower numbers = higher priority)
     * @return This builder for method chaining
     */
    public RuleBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Set the severity for the rule.
     *
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @return This builder for method chaining
     */
    public RuleBuilder withSeverity(String severity) {
        this.severity = severity != null ? severity : SeverityConstants.INFO;
        return this;
    }

    // === CORE METADATA BUILDER METHODS (CRITICAL AUDIT ATTRIBUTES) ===

    /**
     * Set the creation date for the rule - CRITICAL audit attribute.
     * If not set, defaults to current time when rule is built.
     */
    public RuleBuilder withCreatedDate(Instant createdDate) {
        this.metadataBuilder.createdDate(createdDate);
        return this;
    }

    /**
     * Set the modification date for the rule - CRITICAL audit attribute.
     * If not set, defaults to creation date when rule is built.
     */
    public RuleBuilder withModifiedDate(Instant modifiedDate) {
        this.metadataBuilder.modifiedDate(modifiedDate);
        return this;
    }

    // === ADDITIONAL METADATA BUILDER METHODS ===

    /**
     * Set the user who created the rule.
     */
    public RuleBuilder withCreatedByUser(String createdByUser) {
        this.metadataBuilder.createdByUser(createdByUser);
        return this;
    }

    /**
     * Set the business owner of the rule.
     */
    public RuleBuilder withBusinessOwner(String businessOwner) {
        this.metadataBuilder.businessOwner(businessOwner);
        return this;
    }

    /**
     * Set the business domain for the rule.
     */
    public RuleBuilder withBusinessDomain(String businessDomain) {
        this.metadataBuilder.businessDomain(businessDomain);
        return this;
    }

    /**
     * Set the business purpose for the rule.
     */
    public RuleBuilder withBusinessPurpose(String businessPurpose) {
        this.metadataBuilder.businessPurpose(businessPurpose);
        return this;
    }

    /**
     * Set the status of the rule.
     */
    public RuleBuilder withStatus(RuleStatus status) {
        this.metadataBuilder.status(status);
        return this;
    }

    /**
     * Set the version of the rule.
     */
    public RuleBuilder withVersion(String version) {
        this.metadataBuilder.version(version);
        return this;
    }

    /**
     * Set the source system for the rule.
     */
    public RuleBuilder withSourceSystem(String sourceSystem) {
        this.metadataBuilder.sourceSystem(sourceSystem);
        return this;
    }

    /**
     * Set the environment for the rule.
     */
    public RuleBuilder withEnvironment(String environment) {
        this.metadataBuilder.environment(environment);
        return this;
    }

    /**
     * Set the complexity of the rule.
     */
    public RuleBuilder withComplexity(RuleComplexity complexity) {
        this.metadataBuilder.complexity(complexity);
        return this;
    }

    /**
     * Set tags for the rule.
     */
    public RuleBuilder withTags(String... tags) {
        this.metadataBuilder.tags(tags);
        return this;
    }

    /**
     * Add a custom metadata property.
     */
    public RuleBuilder withCustomProperty(String key, Object value) {
        this.metadataBuilder.customProperty(key, value);
        return this;
    }

    /**
     * Set the effective date for the rule.
     */
    public RuleBuilder withEffectiveDate(Instant effectiveDate) {
        this.metadataBuilder.effectiveDate(effectiveDate);
        return this;
    }

    /**
     * Set the expiration date for the rule.
     */
    public RuleBuilder withExpirationDate(Instant expirationDate) {
        this.metadataBuilder.expirationDate(expirationDate);
        return this;
    }

    /**
     * Build a Rule instance with the current builder state.
     * If no categories have been added, a default category will be used.
     * If no description has been set, the message will be used as the description.
     * Metadata will be automatically enhanced based on rule characteristics.
     *
     * @return A new Rule instance
     * @throws IllegalStateException if name, condition, or message is not set
     */
    public Rule build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Rule name must be set");
        }
        if (condition == null || condition.isEmpty()) {
            throw new IllegalStateException("Rule condition must be set");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalStateException("Rule message must be set");
        }

        // Use message as description if description is not set
        if (description == null || description.isEmpty()) {
            description = message;
        }

        // Add default category if no categories are specified
        if (categories.isEmpty()) {
            categories.add(new Category("default", priority));
        }

        // Auto-detect complexity if not set
        if (metadataBuilder.build().getComplexity() == RuleComplexity.MEDIUM) {
            metadataBuilder.complexity(RuleComplexity.fromCondition(condition));
        }

        // Build metadata
        RuleMetadata metadata = metadataBuilder.build();

        Rule rule = new Rule(id, categories, name, condition, message, description, priority, severity, metadata);

        // Auto-register the rule if configuration is available
        if (configuration != null) {
            configuration.registerRule(rule);
        }

        return rule;
    }
}
