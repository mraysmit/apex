package dev.mars.rulesengine.core.engine.config;

import dev.mars.rulesengine.core.engine.model.Category;
import dev.mars.rulesengine.core.engine.model.RuleGroup;

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
 * Builder class for creating RuleGroup instances.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Builder class for creating RuleGroup instances.
 * This provides a fluent API for constructing rule groups with various properties.
 */
public class RuleGroupBuilder {
    private String id;
    private Set<Category> categories = new HashSet<>();
    private String name;
    private String description;
    private int priority = 100; // Default priority
    private boolean isAndOperator = true; // Default to AND operator

    /**
     * Create a new RuleGroupBuilder with a generated ID.
     */
    public RuleGroupBuilder() {
        this.id = "G" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Create a new RuleGroupBuilder with the specified ID.
     *
     * @param id The unique identifier for the rule group
     */
    public RuleGroupBuilder(String id) {
        this.id = id;
    }

    /**
     * Set the ID for the rule group.
     *
     * @param id The unique identifier for the rule group
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Add a category to the rule group.
     *
     * @param category The category to add
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withCategory(Category category) {
        this.categories.add(category);
        return this;
    }

    /**
     * Add a category to the rule group by name.
     *
     * @param categoryName The name of the category to add
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withCategory(String categoryName) {
        this.categories.add(new Category(categoryName, priority));
        return this;
    }

    /**
     * Set the categories for the rule group, replacing any existing categories.
     *
     * @param categories The set of categories for the rule group
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withCategories(Set<Category> categories) {
        this.categories = new HashSet<>(categories);
        return this;
    }

    /**
     * Set the categories for the rule group by name, replacing any existing categories.
     *
     * @param categoryNames The set of category names for the rule group
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withCategoryNames(Set<String> categoryNames) {
        this.categories = categoryNames.stream()
            .map(name -> new Category(name, priority))
            .collect(Collectors.toSet());
        return this;
    }

    /**
     * Set the name for the rule group.
     *
     * @param name The name of the rule group
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the description for the rule group.
     *
     * @param description The description of what the rule group does
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the priority for the rule group.
     *
     * @param priority The priority of the rule group (lower numbers = higher priority)
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Set the operator to AND for combining rules in this group.
     *
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withAndOperator() {
        this.isAndOperator = true;
        return this;
    }

    /**
     * Set the operator to OR for combining rules in this group.
     *
     * @return This builder for method chaining
     */
    public RuleGroupBuilder withOrOperator() {
        this.isAndOperator = false;
        return this;
    }

    /**
     * Build a RuleGroup instance with the current builder state.
     * If no categories have been added, a default category will be used.
     *
     * @return A new RuleGroup instance
     * @throws IllegalStateException if name or description is not set
     */
    public RuleGroup build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Rule group name must be set");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalStateException("Rule group description must be set");
        }

        // Add default category if no categories are specified
        if (categories.isEmpty()) {
            categories.add(new Category("default", priority));
        }

        return new RuleGroup(id, categories, name, description, priority, isAndOperator);
    }
}