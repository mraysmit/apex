package dev.mars.apex.demo.model;

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
 * Represents a trade with basic information.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Represents a trade with basic information.
 * This class is used for demonstration purposes.
 */
public class Trade {
    private String id;
    private String value;
    private String category;

    /**
     * Create a new trade with the specified attributes.
     *
     * @param id The ID of the trade
     * @param value The value of the trade
     * @param category The category of the trade
     */
    public Trade(String id, String value, String category) {
        this.id = id;
        this.value = value;
        this.category = category;
    }

    /**
     * Create a new trade with default values.
     */
    public Trade() {
        this.id = "Unknown";
        this.value = "";
        this.category = "Uncategorized";
    }

    /**
     * Get the ID of the trade.
     *
     * @return The trade's ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the ID of the trade.
     *
     * @param id The new ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the value of the trade.
     *
     * @return The trade's value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value of the trade.
     *
     * @param value The new value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the category of the trade.
     *
     * @return The trade's category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Set the category of the trade.
     *
     * @param category The new category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "id='" + id + '\'' +
                ", value='" + value + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
