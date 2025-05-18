package com.rulesengine.core.engine.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a category for rules and rule groups.
 * A category has a GUID, a name, a description, and a sequence number.
 */
public class Category implements Comparable<Category> {
    private final UUID uuid;
    private final String name;
    private final String description;
    private final int sequenceNumber;

    /**
     * Create a new category with the specified name, description, and sequence number.
     * A UUID will be automatically generated.
     *
     * @param name The name of the category
     * @param description The description of the category
     * @param sequenceNumber The sequence number of the category (lower numbers = higher priority)
     */
    public Category(String name, String description, int sequenceNumber) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Create a new category with the specified name and sequence number.
     * A UUID will be automatically generated, and the description will be the same as the name.
     *
     * @param name The name of the category
     * @param sequenceNumber The sequence number of the category (lower numbers = higher priority)
     */
    public Category(String name, int sequenceNumber) {
        this(name, name, sequenceNumber);
    }

    /**
     * Get the UUID of the category.
     *
     * @return The UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Get the name of the category.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description of the category.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the sequence number of the category.
     *
     * @return The sequence number
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Category other) {
        // Compare by sequence number (lower numbers = higher priority)
        return Integer.compare(this.sequenceNumber, other.sequenceNumber);
    }
}