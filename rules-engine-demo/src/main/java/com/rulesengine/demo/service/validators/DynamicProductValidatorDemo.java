/**
 * Demonstration of how to use the DynamicProductValidatorDemoConfig class with ProductValidator.
 * This class shows the step-by-step process of creating and using a ProductValidator
 * for validating Product objects using the DynamicProductValidatorDemoConfig to define validation rules.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Product;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstration of dynamic product validation using the rules engine.
 */
public class DynamicProductValidatorDemo {

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private DynamicProductValidatorDemo() {
        // Private constructor to prevent instantiation
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Starting dynamic product validation demonstration");

        // Step 1: Create a RulesEngine
        System.out.println("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a DynamicProductValidatorDemoConfig
        System.out.println("Step 2: Creating a DynamicProductValidatorDemoConfig");
        DynamicProductValidatorDemoConfig config = new DynamicProductValidatorDemoConfig(rulesEngine);

        // Step 3: Create parameters for fixed income products
        System.out.println("Step 3: Creating parameters for fixed income products");
        Map<String, Object> fixedIncomeParams = new HashMap<>();
        fixedIncomeParams.put("minPrice", 100.0);
        fixedIncomeParams.put("maxPrice", 2000.0);
        fixedIncomeParams.put("requiredCategory", "FixedIncome");

        // Step 4: Create a ProductValidator for fixed income products
        System.out.println("Step 4: Creating a ProductValidator for fixed income products");
        ProductValidator fixedIncomeValidator = new ProductValidator(
                "fixedIncomeValidator", 
                fixedIncomeParams,
                config
        );

        // Step 5: Create sample products
        System.out.println("Step 5: Creating sample products");
        Product validFixedIncomeProduct = new Product("US Treasury Bond", 1200.0, "FixedIncome");
        Product invalidPriceProduct = new Product("Expensive Bond", 2500.0, "FixedIncome");
        Product invalidCategoryProduct = new Product("Apple Stock", 180.0, "Equity");
        Product etfProduct = new Product("Gold ETF", 1500.0, "ETF");

        // Step 6: Validate products using standard validation
        System.out.println("\nStep 6: Validating products using standard validation");
        System.out.println("Valid fixed income product: " + fixedIncomeValidator.validate(validFixedIncomeProduct));
        System.out.println("Invalid price product: " + fixedIncomeValidator.validate(invalidPriceProduct));
        System.out.println("Invalid category product: " + fixedIncomeValidator.validate(invalidCategoryProduct));
        System.out.println("ETF product: " + fixedIncomeValidator.validate(etfProduct));

        // Step 7: Get detailed validation results
        System.out.println("\nStep 7: Getting detailed validation results");
        RuleResult validResult = fixedIncomeValidator.validateWithResult(validFixedIncomeProduct);
        System.out.println("Valid product result: " + validResult);
        System.out.println("Valid product triggered: " + validResult.isTriggered());
        System.out.println("Valid product rule name: " + validResult.getRuleName());

        RuleResult invalidResult = fixedIncomeValidator.validateWithResult(invalidPriceProduct);
        System.out.println("Invalid product result: " + invalidResult);
        System.out.println("Invalid product triggered: " + invalidResult.isTriggered());

        // Step 8: Validate products using dynamic expressions
        System.out.println("\nStep 8: Validating products using dynamic expressions");
        String customExpression = "#product != null && #product.price < 2000.0 && #product.category == 'FixedIncome'";
        System.out.println("Valid fixed income product with custom expression: " + 
                fixedIncomeValidator.validateWithExpression(validFixedIncomeProduct, customExpression, config));
        System.out.println("Invalid price product with custom expression: " + 
                fixedIncomeValidator.validateWithExpression(invalidPriceProduct, customExpression, config));

        // Step 9: Use more complex dynamic expression
        System.out.println("\nStep 9: Using more complex dynamic expression");
        String complexExpression = "#product != null && #product.price > 1000.0 && (#product.category == 'FixedIncome' || #product.category == 'ETF')";
        System.out.println("Valid fixed income product with complex expression: " + 
                fixedIncomeValidator.validateWithExpression(validFixedIncomeProduct, complexExpression, config));
        System.out.println("ETF product with complex expression: " + 
                fixedIncomeValidator.validateWithExpression(etfProduct, complexExpression, config));

        // Step 10: Use expression with the getDiscountedPrice method
        System.out.println("\nStep 10: Using expression with the getDiscountedPrice method");
        String discountExpression = "#product != null && #product.getDiscountedPrice(10) < 1500.0";
        System.out.println("Valid fixed income product with discount expression: " + 
                fixedIncomeValidator.validateWithExpression(validFixedIncomeProduct, discountExpression, config));
        System.out.println("Invalid price product with discount expression: " + 
                fixedIncomeValidator.validateWithExpression(invalidPriceProduct, discountExpression, config));

        System.out.println("\nDynamic product validation demonstration completed");
    }
}
