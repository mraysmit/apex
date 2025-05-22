package dev.mars.rulesengine.core.engine.config;

import dev.mars.rulesengine.core.engine.model.Category;
import dev.mars.rulesengine.core.engine.model.Rule;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builder class for creating Rule instances.
 * This provides a fluent API for constructing rules with various properties.
 */
public class
RuleBuilder {
    private String id;
    private Set<Category> categories = new HashSet<>();
    private String name;
    private String condition;
    private String message;
    private String description;
    private int priority = 100; // Default priority

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
     * Build a Rule instance with the current builder state.
     * If no categories have been added, a default category will be used.
     * If no description has been set, the message will be used as the description.
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

        return new Rule(id, categories, name, condition, message, description, priority);
    }
}