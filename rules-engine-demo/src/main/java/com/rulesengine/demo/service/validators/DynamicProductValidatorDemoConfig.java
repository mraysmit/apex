package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.util.RuleParameterExtractor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration class for DynamicProductValidatorDemo.
 * This class creates configurations for the functionality of the DynamicProductValidatorDemo class.
 */
public class DynamicProductValidatorDemoConfig {
    private final RulesEngine rulesEngine;

    /**
     * Create a new DynamicProductValidatorDemoConfig with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for validation
     */
    public DynamicProductValidatorDemoConfig(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    /**
     * Create a validation rule group for products.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters (minPrice, maxPrice, requiredCategory, etc.)
     * @return The validation rule group
     * @throws IllegalArgumentException if required parameters are missing
     */
    public RuleGroup createValidationRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
            "ProductValidationRuleGroup",
            "ProductValidation",
            name,
            "Validates product against defined criteria",
            1,
            true // AND operator
        );

        // Rule for null check
        Rule nullCheckRule = new Rule(
            "NullCheckRule",
            "#product != null",
            "Product must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Rule for price validation
        double minPrice = parameters.containsKey("minPrice") ? (double)parameters.get("minPrice") : 0.0;
        double maxPrice = parameters.containsKey("maxPrice") ? (double)parameters.get("maxPrice") : Double.MAX_VALUE;

        Rule priceValidationRule = new Rule(
            "PriceValidationRule",
            "#product != null && #product.price >= #minPrice && #product.price <= #maxPrice",
            "Product price must be between " + minPrice + " and " + maxPrice
        );
        ruleGroup.addRule(priceValidationRule, 2);

        // Rule for category validation
        String requiredCategory = (String)parameters.get("requiredCategory");
        if (requiredCategory != null) {
            Rule categoryValidationRule = new Rule(
                "CategoryValidationRule",
                "#product != null && (#requiredCategory == null || #requiredCategory.equals(#product.category))",
                "Product category must be " + requiredCategory
            );
            ruleGroup.addRule(categoryValidationRule, 3);
        }

        // Validate that all required parameters exist (except 'product' which will be provided at validation time)
        Set<String> allParams = RuleParameterExtractor.extractParameters(ruleGroup);
        allParams.remove("product"); // Remove product as it will be provided at validation time

        Set<String> missingParams = new HashSet<>();
        for (String param : allParams) {
            if (!parameters.containsKey(param)) {
                missingParams.add(param);
            }
        }

        if (!missingParams.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + missingParams);
        }

        return ruleGroup;
    }

    /**
     * Create a dynamic validation rule based on a custom expression.
     *
     * @param expression The expression to evaluate
     * @return The validation rule
     */
    public Rule createDynamicValidationRule(String expression) {
        return new Rule(
            "DynamicValidationRule",
            expression,
            "Dynamic validation rule"
        );
    }

    /**
     * Get the rules engine used by this configuration.
     *
     * @return The rules engine
     */
    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }
}