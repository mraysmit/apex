package com.rulesengine.core.engine.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a business rule with a condition, message, and other properties.
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
}