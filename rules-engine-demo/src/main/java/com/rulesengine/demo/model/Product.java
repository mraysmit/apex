package com.rulesengine.demo.model;

/**
 * Represents a product with basic information.
 */
public class Product {
    private String name;
    private double price;
    private String category;

    /**
     * Create a new product with the specified attributes.
     *
     * @param name The name of the product
     * @param price The price of the product
     * @param category The category of the product
     */
    public Product(String name, double price, String category) {
        this.name = name;
        this.price = price;
        this.category = category;
    }

    /**
     * Create a new product with default values.
     */
    public Product() {
        this.name = "Unknown";
        this.price = 0.0;
        this.category = "Uncategorized";
    }

    /**
     * Get the name of the product.
     *
     * @return The product's name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the product.
     *
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the price of the product.
     *
     * @return The product's price
     */
    public double getPrice() {
        return price;
    }

    /**
     * Set the price of the product.
     *
     * @param price The new price
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Get the category of the product.
     *
     * @return The product's category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Set the category of the product.
     *
     * @param category The new category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Calculate the discounted price of the product.
     *
     * @param discountPercent The discount percentage (0-100)
     * @return The discounted price
     */
    public double getDiscountedPrice(double discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        return price * (1 - discountPercent / 100);
    }

    @Override
    public String toString() {
        return "Product{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                '}';
    }
}