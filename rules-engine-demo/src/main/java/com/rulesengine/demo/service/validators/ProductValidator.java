/**
 * A validator for Product objects.
 * This validator checks if a product meets certain criteria.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.demo.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductValidator implements Validator<Product> {
    private final String name;
    private final double minPrice;
    private final double maxPrice;
    private final String requiredCategory;
    private final RulesEngine rulesEngine;
    private final List<Rule> validationRules;

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
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.validationRules = createValidationRules();
    }

    private List<Rule> createValidationRules() {
        List<Rule> rules = new ArrayList<>();

        // Rule for null check
        rules.add(new Rule(
            "NullCheckRule",
            "#product != null",
            "Product must not be null"
        ));

        // Rule for price validation
        rules.add(new Rule(
            "PriceValidationRule",
            "#product != null && #product.price >= #minPrice && #product.price <= #maxPrice",
            "Product price must be between " + minPrice + " and " + maxPrice
        ));

        // Rule for category validation
        if (requiredCategory != null) {
            rules.add(new Rule(
                "CategoryValidationRule",
                "#product != null && (#requiredCategory == null || #requiredCategory.equals(#product.category))",
                "Product category must be " + requiredCategory
            ));
        }

        return rules;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Product product) {
        RuleResult result = validateWithResult(product);
        return result.isTriggered();
    }

    @Override
    public Class<Product> getType() {
        return Product.class;
    }

    @Override
    public RuleResult validateWithResult(Product product) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("product", product);
        facts.put("minPrice", minPrice);
        facts.put("maxPrice", maxPrice);
        facts.put("requiredCategory", requiredCategory);

        // Execute all rules and return the first failure or success if all pass
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
            if (!result.isTriggered()) {
                return RuleResult.noMatch();
            }
        }

        return RuleResult.match(getName(), "Product validation successful");
    }
}
