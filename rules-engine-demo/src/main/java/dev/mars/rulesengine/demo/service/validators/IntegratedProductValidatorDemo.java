package dev.mars.rulesengine.demo.service.validators;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleGroup;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.util.RuleParameterExtractor;
import dev.mars.rulesengine.demo.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.springframework.expression.spel.support.StandardEvaluationContext;

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
 * Comprehensive demonstration of product validation using the rules engine.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of product validation using the rules engine.
 *
 * This integrated class combines the functionality of:
 * - DynamicProductValidatorDemoRuleConfig (rule configuration)
 * - ProductValidator (validation logic)
 * - DynamicProductValidatorDemo (demonstration)
 *
 * It demonstrates how to create, configure, and use a product validation
 * with various validation scenarios in a clear, sequential manner.
 */
public class IntegratedProductValidatorDemo {
    private static final Logger LOGGER = Logger.getLogger(IntegratedProductValidatorDemo.class.getName());

    // Core components
    private final RulesEngine rulesEngine;
    private final String validatorName;
    private final Map<String, Object> parameters;
    private final StandardEvaluationContext context;
    private final RuleGroup validationRuleGroup;

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        runProductValidationDemo();
    }

    /**
     * Run the product validation demonstration.
     * This method shows the complete process of creating and using a product validation.
     */
    private static void runProductValidationDemo() {
        LOGGER.info("Starting integrated product validation demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create parameters for fixed income products
        LOGGER.info("Step 2: Creating parameters for fixed income products");
        Map<String, Object> fixedIncomeParams = new HashMap<>();
        fixedIncomeParams.put("minPrice", 100.0);
        fixedIncomeParams.put("maxPrice", 2000.0);
        fixedIncomeParams.put("requiredCategory", "FixedIncome");

        // Step 3: Create an IntegratedProductValidatorDemo instance
        LOGGER.info("Step 3: Creating an IntegratedProductValidatorDemo instance");
        IntegratedProductValidatorDemo validator = new IntegratedProductValidatorDemo(
                "fixedIncomeValidator",
                fixedIncomeParams,
                rulesEngine
        );

        // Step 4: Create sample products
        LOGGER.info("Step 4: Creating sample products");
        List<Product> products = createSampleProducts();

        // Step 5: Validate products using standard validation
        LOGGER.info("\nStep 5: Validating products using standard validation");
        for (Product product : products) {
            LOGGER.info(product.getName() + " (Price: $" + product.getPrice() +
                    ", Category: " + product.getCategory() + "): " +
                    validator.validate(product));
        }

        // Step 6: Get detailed validation results
        LOGGER.info("\nStep 6: Getting detailed validation results");
        Product validProduct = products.get(0); // US Treasury Bond
        Product invalidPriceProduct = products.get(1); // Expensive Bond

        RuleResult validResult = validator.validateWithResult(validProduct);
        LOGGER.info("Valid product result: " + validResult);
        LOGGER.info("Valid product triggered: " + validResult.isTriggered());
        LOGGER.info("Valid product rule name: " + validResult.getRuleName());

        RuleResult invalidResult = validator.validateWithResult(invalidPriceProduct);
        LOGGER.info("Invalid product result: " + invalidResult);
        LOGGER.info("Invalid product triggered: " + invalidResult.isTriggered());

        // Step 7: Validate products using dynamic expressions
        LOGGER.info("\nStep 7: Validating products using dynamic expressions");
        String customExpression = "#product != null && #product.price < 2000.0 && #product.category == 'FixedIncome'";
        LOGGER.info("Expression: " + customExpression);

        for (Product product : products) {
            LOGGER.info(product.getName() + ": " +
                    validator.validateWithExpression(product, customExpression));
        }

        // Step 8: Use more complex dynamic expression
        LOGGER.info("\nStep 8: Using more complex dynamic expression");
        String complexExpression = "#product != null && #product.price > 1000.0 && " +
                "(#product.category == 'FixedIncome' || #product.category == 'ETF')";
        LOGGER.info("Expression: " + complexExpression);

        for (Product product : products) {
            LOGGER.info(product.getName() + ": " +
                    validator.validateWithExpression(product, complexExpression));
        }

        // Step 9: Use expression with the getDiscountedPrice method
        LOGGER.info("\nStep 9: Using expression with the getDiscountedPrice method");
        String discountExpression = "#product != null && #product.getDiscountedPrice(10) < 1500.0";
        LOGGER.info("Expression: " + discountExpression);

        for (Product product : products) {
            LOGGER.info(product.getName() + " with 10% discount: " +
                    validator.validateWithExpression(product, discountExpression));
        }

        // Step 10: Create a different validation for equity products
        LOGGER.info("\nStep 10: Creating a validation for equity products");
        Map<String, Object> equityParams = new HashMap<>();
        equityParams.put("minPrice", 50.0);
        equityParams.put("maxPrice", 1000.0);
        equityParams.put("requiredCategory", "Equity");

        IntegratedProductValidatorDemo equityValidator = new IntegratedProductValidatorDemo(
                "equityValidator",
                equityParams,
                rulesEngine
        );

        // Step 11: Validate with the equity validation
        LOGGER.info("\nStep 11: Validating with the equity validation");
        for (Product product : products) {
            LOGGER.info(product.getName() + ": " +
                    equityValidator.validate(product));
        }

        LOGGER.info("\nIntegrated product validation demonstration completed");
    }

    /**
     * Create sample products for demonstration.
     *
     * @return List of sample products
     */
    private static List<Product> createSampleProducts() {
        List<Product> products = new ArrayList<>();

        // Valid fixed income product
        products.add(new Product("US Treasury Bond", 1200.0, "FixedIncome"));

        // Invalid price product (too expensive)
        products.add(new Product("Expensive Bond", 2500.0, "FixedIncome"));

        // Invalid category product
        products.add(new Product("Apple Stock", 180.0, "Equity"));

        // ETF product
        products.add(new Product("Gold ETF", 1500.0, "ETF"));

        return products;
    }

    /**
     * Create a new IntegratedProductValidatorDemo with the specified parameters.
     *
     * @param name The name of the validation
     * @param parameters Map of validation parameters (minPrice, maxPrice, requiredCategory, etc.)
     * @param rulesEngine The rules engine to use
     */
    public IntegratedProductValidatorDemo(String name, Map<String, Object> parameters, RulesEngine rulesEngine) {
        this.validatorName = name;
        this.parameters = new HashMap<>(parameters);
        this.rulesEngine = rulesEngine;
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group
        this.validationRuleGroup = createValidationRuleGroup(name, parameters);
    }

    /**
     * Create a validation rule group for products.
     *
     * @param name The name of the validation
     * @param parameters Map of validation parameters
     * @return The validation rule group
     */
    private RuleGroup createValidationRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
                "ProductValidationRuleGroup",
                "ProductValidation",
                name,
                "Validates product against defined criteria",
                1,
                true // AND operator
        );

        // Create Rule for null check
        Rule nullCheckRule = new Rule(
                "NullCheckRule",
                "#product != null",
                "Product must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Create Rule for price validation
        double minPrice = parameters.containsKey("minPrice") ? (double) parameters.get("minPrice") : 0.0;
        double maxPrice = parameters.containsKey("maxPrice") ? (double) parameters.get("maxPrice") : Double.MAX_VALUE;

        Rule priceValidationRule = new Rule(
                "PriceValidationRule",
                "#product != null && #product.price >= #minPrice && #product.price <= #maxPrice",
                "Product price must be between " + minPrice + " and " + maxPrice
        );
        ruleGroup.addRule(priceValidationRule, 2);

        // Rule for category validation
        String requiredCategory = (String) parameters.get("requiredCategory");
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
    private Rule createDynamicValidationRule(String expression) {
        return new Rule(
                "DynamicValidationRule",
                expression,
                "Dynamic validation rule"
        );
    }

    /**
     * Get the name of this validation.
     *
     * @return The name
     */
    public String getName() {
        return validatorName;
    }

    /**
     * Validate a product using dynamic evaluation.
     *
     * @param product The product to validate
     * @return True if the product is valid, false otherwise
     */
    public boolean validate(Product product) {
        RuleResult result = validateWithResult(product);
        return result.isTriggered();
    }

    /**
     * Validate a product and return a detailed result.
     *
     * @param product The product to validate
     * @return The validation result
     */
    public RuleResult validateWithResult(Product product) {
        // Set the product in the context
        context.setVariable("product", product);

        // Create initial facts map with product data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("product", product);

        // Use RuleParameterExtractor to ensure all required parameters exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(validationRuleGroup, initialFacts);

        // Execute the rule group using the rules engine
        return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), facts);
    }

    /**
     * Validate a product using a dynamic expression.
     *
     * @param product The product to validate
     * @param expression The expression to evaluate
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpression(Product product, String expression) {
        // Set the product in the context
        context.setVariable("product", product);

        // Create a rule with the dynamic expression
        Rule dynamicRule = createDynamicValidationRule(expression);

        // Create initial facts map with product data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("product", product);

        // Use RuleParameterExtractor to ensure all required parameters for the dynamic rule exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(dynamicRule, initialFacts);

        // Execute the rule using the rules engine
        RuleResult result = rulesEngine.executeRule(dynamicRule, facts);
        return result.isTriggered();
    }
}