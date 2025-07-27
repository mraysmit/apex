package dev.mars.rulesengine.core.engine.model;

import java.util.Objects;
import java.util.UUID;

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
 * Represents a category for rules and rule groups.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
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