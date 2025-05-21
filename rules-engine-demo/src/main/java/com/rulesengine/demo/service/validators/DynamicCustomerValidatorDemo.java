/**
 * Demonstration of how to use the DynamicCustomerValidatorDemoConfig class with CustomerValidator.
 * This class shows the step-by-step process of creating and using a CustomerValidator
 * for validating Customer objects using the DynamicCustomerValidatorDemoConfig to define validation rules.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Customer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstration of dynamic customer validation using the rules engine.
 */
public class DynamicCustomerValidatorDemo {
    
    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private DynamicCustomerValidatorDemo() {
        // Private constructor to prevent instantiation
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Starting dynamic customer validation demonstration");

        // Step 1: Create a RulesEngine
        System.out.println("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a DynamicCustomerValidatorDemoConfig
        System.out.println("Step 2: Creating a DynamicCustomerValidatorDemoConfig");
        DynamicCustomerValidatorDemoConfig config = new DynamicCustomerValidatorDemoConfig(rulesEngine);

        // Step 3: Create parameters for senior gold members
        System.out.println("Step 3: Creating parameters for senior gold members");
        Map<String, Object> seniorGoldParams = new HashMap<>();
        seniorGoldParams.put("minAge", 60);
        seniorGoldParams.put("maxAge", 100);
        seniorGoldParams.put("allowedMembershipLevels", Collections.singletonList("Gold"));

        // Step 4: Create a CustomerValidator for senior gold members
        System.out.println("Step 4: Creating a CustomerValidator for senior gold members");
        CustomerValidator seniorGoldValidator = new CustomerValidator(
                "seniorGoldValidator", 
                seniorGoldParams,
                config
        );

        // Step 5: Create sample customers
        System.out.println("Step 5: Creating sample customers");
        List<String> preferredCategories1 = new ArrayList<>();
        preferredCategories1.add("Equity");
        preferredCategories1.add("FixedIncome");
        Customer validSeniorGoldCustomer = new Customer("John Doe", 65, "Gold", preferredCategories1);

        List<String> preferredCategories2 = new ArrayList<>();
        preferredCategories2.add("ETF");
        Customer invalidAgeCustomer = new Customer("Jane Smith", 45, "Gold", preferredCategories2);

        List<String> preferredCategories3 = new ArrayList<>();
        preferredCategories3.add("Commodity");
        Customer invalidMembershipCustomer = new Customer("Bob Johnson", 70, "Silver", preferredCategories3);

        List<String> preferredCategories4 = new ArrayList<>();
        preferredCategories4.add("FixedIncome");
        Customer platinumCustomer = new Customer("Alice Brown", 75, "Platinum", preferredCategories4);

        // Step 6: Validate customers using standard validation
        System.out.println("\nStep 6: Validating customers using standard validation");
        System.out.println("Valid senior gold customer: " + seniorGoldValidator.validate(validSeniorGoldCustomer));
        System.out.println("Invalid age customer: " + seniorGoldValidator.validate(invalidAgeCustomer));
        System.out.println("Invalid membership customer: " + seniorGoldValidator.validate(invalidMembershipCustomer));
        System.out.println("Platinum customer: " + seniorGoldValidator.validate(platinumCustomer));

        // Step 7: Get detailed validation results
        System.out.println("\nStep 7: Getting detailed validation results");
        RuleResult validResult = seniorGoldValidator.validateWithResult(validSeniorGoldCustomer);
        System.out.println("Valid customer result: " + validResult);
        System.out.println("Valid customer triggered: " + validResult.isTriggered());
        System.out.println("Valid customer rule name: " + validResult.getRuleName());

        RuleResult invalidResult = seniorGoldValidator.validateWithResult(invalidAgeCustomer);
        System.out.println("Invalid customer result: " + invalidResult);
        System.out.println("Invalid customer triggered: " + invalidResult.isTriggered());

        // Step 8: Validate customers using dynamic expressions
        System.out.println("\nStep 8: Validating customers using dynamic expressions");
        String customExpression = "#customer != null && #customer.age >= 60 && #customer.membershipLevel == 'Gold'";
        System.out.println("Valid senior gold customer with custom expression: " + 
                seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, customExpression, config));
        System.out.println("Invalid age customer with custom expression: " + 
                seniorGoldValidator.validateWithExpression(invalidAgeCustomer, customExpression, config));

        // Step 9: Use more complex dynamic expression
        System.out.println("\nStep 9: Using more complex dynamic expression");
        String complexExpression = "#customer != null && #customer.age >= 60 && (#customer.membershipLevel == 'Gold' || #customer.membershipLevel == 'Platinum')";
        System.out.println("Valid senior gold customer with complex expression: " + 
                seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, complexExpression, config));
        System.out.println("Platinum customer with complex expression: " + 
                seniorGoldValidator.validateWithExpression(platinumCustomer, complexExpression, config));

        // Step 10: Use expression with the isEligibleForDiscount method
        System.out.println("\nStep 10: Using expression with the isEligibleForDiscount method");
        String discountExpression = "#customer != null && #customer.isEligibleForDiscount()";
        System.out.println("Valid senior gold customer eligible for discount: " + 
                seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, discountExpression, config));
        System.out.println("Invalid age customer eligible for discount: " + 
                seniorGoldValidator.validateWithExpression(invalidAgeCustomer, discountExpression, config));
        System.out.println("Invalid membership customer eligible for discount: " + 
                seniorGoldValidator.validateWithExpression(invalidMembershipCustomer, discountExpression, config));

        // Step 11: Use expression with preferred categories
        System.out.println("\nStep 11: Using expression with preferred categories");
        String categoriesExpression = "#customer != null && #customer.preferredCategories.contains('Equity')";
        System.out.println("Customer with Equity in preferred categories: " + 
                seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, categoriesExpression, config));
        System.out.println("Customer without Equity in preferred categories: " + 
                seniorGoldValidator.validateWithExpression(invalidAgeCustomer, categoriesExpression, config));

        System.out.println("\nDynamic customer validation demonstration completed");
    }
}