/**
 * A validator for Product objects.
 * This validator checks if a product meets certain criteria.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.demo.model.Product;

public class ProductValidator implements Validator<Product> {
    private final String name;
    private final double minPrice;
    private final double maxPrice;
    private final String requiredCategory;

    /**
     * Create a new ProductValidator with the specified criteria.
     *
     * @param name The name of the validator
     * @param minPrice The minimum price for a valid product
     * @param maxPrice The maximum price for a valid product
     * @param requiredCategory The required category for a valid product, or null if any category is valid
     */
    public ProductValidator(String name, double minPrice, double maxPrice, String requiredCategory) {
        this.name = name;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.requiredCategory = requiredCategory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Product product) {
        if (product == null) {
            return false;
        }

        // Check price range
        if (product.getPrice() < minPrice || product.getPrice() > maxPrice) {
            return false;
        }

        // Check category if required
        if (requiredCategory != null && !requiredCategory.equals(product.getCategory())) {
            return false;
        }

        return true;
    }

    @Override
    public Class<Product> getType() {
        return Product.class;
    }
}
