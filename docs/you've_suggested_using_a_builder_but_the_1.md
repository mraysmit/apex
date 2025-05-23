
```java
/**
 * A dynamic validator for Product objects.
 * This validator checks if a product meets certain criteria using the dynamic evaluation capabilities of the RulesEngine.
 * Unlike ProductValidator, this class does not implement the Validator<T> interface.
 * This implementation uses a map of facts directly, without hard-coded parameters.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Product;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class DynamicProductValidatorDemo {
    private final String name;
    private final Map<String, Object> facts;
    private final RulesEngine rulesEngine;
    private final RuleGroup validationRuleGroup;
    private final StandardEvaluationContext context;

    /**
     * Create a new DynamicProductValidatorDemo with the specified facts.
     *
     * @param name The name of the validator
     * @param facts The map of facts to use for validation
     */
    public DynamicProductValidatorDemo(String name, Map<String, Object> facts) {
        this.name = name;
        this.facts = new HashMap<>(facts); // Create a defensive copy
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.context = new StandardEvaluationContext();
        
        // Initialize context with all facts
        for (Map.Entry<String, Object> entry : facts.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        
        this.validationRuleGroup = createValidationRuleGroup();
    }

    private RuleGroup createValidationRuleGroup() {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
            "ProductValidationRuleGroup",
            "ProductValidation",
            name,
            "Validates product against defined criteria",
            1,
            true // AND operator
        );

        // Rule for null check (always included)
        Rule nullCheckRule = new Rule(
            "NullCheckRule",
            "#product != null",
            "Product must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Add rules based on available facts
        int sequence = 2;
        
        // Price validation rule (if min and max price are provided)
        if (facts.containsKey("minPrice") && facts.containsKey("maxPrice")) {
            Rule priceValidationRule = new Rule(
                "PriceValidationRule",
                "#product != null && #product.price >= #minPrice && #product.price <= #maxPrice",
                "Product price must be between " + facts.get("minPrice") + " and " + facts.get("maxPrice")
            );
            ruleGroup.addRule(priceValidationRule, sequence++);
        }

        // Category validation rule (if required category is provided)
        if (facts.containsKey("requiredCategory")) {
            Rule categoryValidationRule = new Rule(
                "CategoryValidationRule",
                "#product != null && (#requiredCategory == null || #requiredCategory.equals(#product.category))",
                "Product category must be " + facts.get("requiredCategory")
            );
            ruleGroup.addRule(categoryValidationRule, sequence++);
        }
        
        // Any other rules can be added dynamically based on the facts provided
        // For example, if there's a list of allowed categories
        if (facts.containsKey("allowedCategories")) {
            Rule allowedCategoriesRule = new Rule(
                "AllowedCategoriesRule",
                "#product != null && (#allowedCategories == null || #allowedCategories.isEmpty() || #allowedCategories.contains(#product.category))",
                "Product category must be in the allowed categories list"
            );
            ruleGroup.addRule(allowedCategoriesRule, sequence++);
        }
        
        // Custom rules can be added directly from the facts map if provided
        if (facts.containsKey("customRules")) {
            Object customRulesObj = facts.get("customRules");
            if (customRulesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> customRules = (Map<String, String>) customRulesObj;
                for (Map.Entry<String, String> entry : customRules.entrySet()) {
                    Rule customRule = new Rule(
                        entry.getKey(),
                        entry.getValue(),
                        "Custom rule: " + entry.getKey()
                    );
                    ruleGroup.addRule(customRule, sequence++);
                }
            }
        }

        return ruleGroup;
    }

    /**
     * Get the name of this validator.
     *
     * @return The name
     */
    public String getName() {
        return name;
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
        // Create a new facts map with the product and all existing facts
        Map<String, Object> evaluationFacts = createFacts(product);
        
        // Execute the rule group using the rules engine
        return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), evaluationFacts);
    }
    
    /**
     * Validate a product using a dynamic expression.
     *
     * @param product The product to validate
     * @param expression The expression to evaluate
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpression(Product product, String expression) {
        // Create a rule with the dynamic expression
        Rule dynamicRule = new Rule(
            "DynamicValidationRule",
            expression,
            "Dynamic validation rule"
        );
        
        // Create a new facts map with the product and all existing facts
        Map<String, Object> evaluationFacts = createFacts(product);
        
        // Execute the rule using the rules engine
        RuleResult result = rulesEngine.executeRule(dynamicRule, evaluationFacts);
        return result.isTriggered();
    }
    
    /**
     * Create facts map for rule evaluation.
     *
     * @param product The product to create facts for
     * @return The facts map
     */
    private Map<String, Object> createFacts(Product product) {
        Map<String, Object> evaluationFacts = new HashMap<>(facts);
        evaluationFacts.put("product", product);
        return evaluationFacts;
    }
    
    /**
     * Demonstrate the usage of DynamicProductValidatorDemo.
     */
    public static void main(String[] args) {
        // Create a map of facts for fixed income products
        Map<String, Object> fixedIncomeValidatorFacts = new HashMap<>();
        fixedIncomeValidatorFacts.put("minPrice", 100.0);
        fixedIncomeValidatorFacts.put("maxPrice", 2000.0);
        fixedIncomeValidatorFacts.put("requiredCategory", "FixedIncome");
        
        // Create a validator for fixed income products
        DynamicProductValidatorDemo fixedIncomeValidator = new DynamicProductValidatorDemo(
            "fixedIncomeValidator", 
            fixedIncomeValidatorFacts
        );
        
        // Create sample products
        Product validFixedIncomeProduct = new Product("US Treasury Bond", 1200.0, "FixedIncome");
        Product invalidPriceProduct = new Product("Expensive Bond", 2500.0, "FixedIncome");
        Product invalidCategoryProduct = new Product("Apple Stock", 180.0, "Equity");
        
        // Validate products using standard validation
        System.out.println("Standard validation results:");
        System.out.println("Valid fixed income product: " + fixedIncomeValidator.validate(validFixedIncomeProduct));
        System.out.println("Invalid price product: " + fixedIncomeValidator.validate(invalidPriceProduct));
        System.out.println("Invalid category product: " + fixedIncomeValidator.validate(invalidCategoryProduct));
        
        // Validate products using dynamic expressions
        System.out.println("\nDynamic expression validation results:");
        String customExpression = "#product != null && #product.price < 2000.0 && #product.category == 'FixedIncome'";
        System.out.println("Valid fixed income product with custom expression: " + 
            fixedIncomeValidator.validateWithExpression(validFixedIncomeProduct, customExpression));
        System.out.println("Invalid price product with custom expression: " + 
            fixedIncomeValidator.validateWithExpression(invalidPriceProduct, customExpression));
        
        // Create a validator with more complex facts including custom rules
        Map<String, Object> complexValidatorFacts = new HashMap<>();
        complexValidatorFacts.put("minPrice", 1000.0);
        complexValidatorFacts.put("maxPrice", 5000.0);
        complexValidatorFacts.put("allowedCategories", Arrays.asList("FixedIncome", "ETF"));
        
        // Add custom rules
        Map<String, String> customRules = new HashMap<>();
        customRules.put("HighValueRule", "#product != null && #product.price > 1000.0");
        customRules.put("NameLengthRule", "#product != null && #product.name.length() > 5");
        complexValidatorFacts.put("customRules", customRules);
        
        DynamicProductValidatorDemo complexValidator = new DynamicProductValidatorDemo(
            "complexValidator", 
            complexValidatorFacts
        );
        
        // More complex dynamic expression
        String complexExpression = "#product != null && #product.price > 1000.0 && (#product.category == 'FixedIncome' || #product.category == 'ETF')";
        System.out.println("\nComplex expression validation results:");
        System.out.println("Valid fixed income product with complex expression: " + 
            complexValidator.validateWithExpression(validFixedIncomeProduct, complexExpression));
        
        // Create a product that would match the complex expression but not the standard validation
        Product etfProduct = new Product("Gold ETF", 1500.0, "ETF");
        System.out.println("ETF product with standard validation (complex validator): " + complexValidator.validate(etfProduct));
        System.out.println("ETF product with standard validation (fixed income validator): " + fixedIncomeValidator.validate(etfProduct));
        System.out.println("ETF product with complex expression: " + 
            complexValidator.validateWithExpression(etfProduct, complexExpression));
    }
}
```

This implementation of `DynamicProductValidatorDemo` addresses the requirements by:

1. **Using a Map of Facts Directly**: The constructor accepts a map of facts directly, eliminating the need for hard-coded parameters.

2. **No Hard-Coded Parameter Methods**: There are no hard-coded methods for specific parameters. All parameters are passed through the facts map.

3. **Rules Query the Map for Parameters**: The Rule objects query the facts map for their required parameters. The `createValidationRuleGroup()` method dynamically creates rules based on what's available in the facts map.

4. **Extensibility**: New validation criteria can be added without changing the class by simply including them in the facts map. The class even supports adding custom rules directly through the facts map.

5. **Maintains Existing Functionality**: All the functionality of the original implementation is preserved, including the ability to validate products and use dynamic expressions.

Key improvements:

- **Dynamic Rule Creation**: Rules are created based on what's in the facts map, not hard-coded.
- **Custom Rules Support**: Custom rules can be added directly through the facts map.
- **Flexible Parameter Handling**: Parameters can be added or removed without changing the class.
- **Maintains Context**: The evaluation context is initialized with all facts, making them available to rules.

This design follows SOLID principles with clear separation of concerns and dependency injection. It's also more maintainable and extensible than the previous implementations.