package com.rulesengine.demo.model;

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