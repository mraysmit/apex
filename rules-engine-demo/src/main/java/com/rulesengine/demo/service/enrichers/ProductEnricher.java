/**
 * An enricher for Product objects.
 * This enricher adds additional information to products based on their category.
 */
package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Product;

import java.util.HashMap;
import java.util.Map;

public class ProductEnricher extends AbstractEnricher<Product> {
    private final Map<String, Double> categoryDiscounts;
    private final Map<String, String> categoryDescriptions;

    /**
     * Create a new ProductEnricher with the specified name.
     *
     * @param name The name of the enricher
     */
    public ProductEnricher(String name) {
        super(name, Product.class);
        this.categoryDiscounts = new HashMap<>();
        this.categoryDescriptions = new HashMap<>();

        // Initialize with default values
        initializeDefaultValues();
    }

    /**
     * Initialize the enricher with default values.
     */
    private void initializeDefaultValues() {
        // Set up category discounts
        categoryDiscounts.put("Equity", 0.05);  // 5% discount
        categoryDiscounts.put("FixedIncome", 0.03);  // 3% discount
        categoryDiscounts.put("ETF", 0.02);  // 2% discount

        // Set up category descriptions
        categoryDescriptions.put("Equity", "Stocks representing ownership in a company");
        categoryDescriptions.put("FixedIncome", "Debt securities with fixed interest payments");
        categoryDescriptions.put("ETF", "Exchange-traded funds tracking an index or sector");
    }

    /**
     * Add a category discount.
     *
     * @param category The category
     * @param discount The discount as a decimal (e.g., 0.05 for 5%)
     */
    public void addCategoryDiscount(String category, double discount) {
        categoryDiscounts.put(category, discount);
    }

    /**
     * Add a category description.
     *
     * @param category The category
     * @param description The description
     */
    public void addCategoryDescription(String category, String description) {
        categoryDescriptions.put(category, description);
    }

    @Override
    public Product enrich(Product product) {
        if (product == null) {
            return null;
        }

        String category = product.getCategory();

        // Create a new product with the same properties
        Product enrichedProduct = new Product(
            product.getName(),
            product.getPrice(),
            product.getCategory()
        );

        // Apply discount if available for this category
        if (categoryDiscounts.containsKey(category)) {
            double discount = categoryDiscounts.get(category);
            double discountedPrice = product.getPrice() * (1 - discount);
            enrichedProduct.setPrice(discountedPrice);
        }

        // Add description to the product name if available
        if (categoryDescriptions.containsKey(category)) {
            String description = categoryDescriptions.get(category);
            enrichedProduct.setName(product.getName() + " - " + description);
        }

        return enrichedProduct;
    }
}
