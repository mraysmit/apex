package dev.mars.rulesengine.demo.rulesets;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleGroup;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.util.RuleParameterExtractor;
import dev.mars.rulesengine.demo.model.Customer;

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
 * Comprehensive demonstration of customer validation using the rules engine.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of customer validation using the rules engine.
 *
 * This integrated class combines the functionality of:
 * - DynamicCustomerValidatorDemoRuleConfig (rule configuration)
 * - CustomerValidator (validation logic)
 * - DynamicCustomerValidatorDemo (demonstration)
 *
 * It demonstrates how to create, configure, and use a customer validation
 * with various validation scenarios in a clear, sequential manner.
 */
public class IntegratedCustomerValidatorDemo {
    private static final Logger LOGGER = Logger.getLogger(IntegratedCustomerValidatorDemo.class.getName());

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
        runCustomerValidationDemo();
    }

    /**
     * Run the customer validation demonstration.
     * This method shows the complete process of creating and using a customer validation.
     */
    private static void runCustomerValidationDemo() {
        LOGGER.info("Starting integrated customer validation demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create parameters for senior gold members
        LOGGER.info("Step 2: Creating parameters for senior gold members");
        Map<String, Object> seniorGoldParams = new HashMap<>();
        seniorGoldParams.put("minAge", 60);
        seniorGoldParams.put("maxAge", 100);
        seniorGoldParams.put("allowedMembershipLevels", Collections.singletonList("Gold"));

        // Step 3: Create an IntegratedCustomerValidatorDemo instance
        LOGGER.info("Step 3: Creating an IntegratedCustomerValidatorDemo instance");
        IntegratedCustomerValidatorDemo validator = new IntegratedCustomerValidatorDemo(
                "seniorGoldValidator",
                seniorGoldParams,
                rulesEngine
        );

        // Step 4: Create sample customers
        LOGGER.info("Step 4: Creating sample customers");
        List<Customer> customers = createSampleCustomers();

        // Step 5: Validate customers using standard validation
        LOGGER.info("\nStep 5: Validating customers using standard validation");
        for (Customer customer : customers) {
            LOGGER.info(customer.getName() + " (Age: " + customer.getAge() +
                    ", Membership: " + customer.getMembershipLevel() + "): " +
                    validator.validate(customer));
        }

        // Step 6: Get detailed validation results
        LOGGER.info("\nStep 6: Getting detailed validation results");
        Customer validCustomer = customers.get(0); // John Doe
        Customer invalidCustomer = customers.get(1); // Jane Smith

        RuleResult validResult = validator.validateWithResult(validCustomer);
        LOGGER.info("Valid customer result: " + validResult);
        LOGGER.info("Valid customer triggered: " + validResult.isTriggered());
        LOGGER.info("Valid customer rule name: " + validResult.getRuleName());

        RuleResult invalidResult = validator.validateWithResult(invalidCustomer);
        LOGGER.info("Invalid customer result: " + invalidResult);
        LOGGER.info("Invalid customer triggered: " + invalidResult.isTriggered());

        // Step 7: Validate customers using dynamic expressions
        LOGGER.info("\nStep 7: Validating customers using dynamic expressions");
        String customExpression = "#customer != null && #customer.age >= 60 && #customer.membershipLevel == 'Gold'";
        LOGGER.info("Expression: " + customExpression);

        for (Customer customer : customers) {
            LOGGER.info(customer.getName() + ": " +
                    validator.validateWithExpression(customer, customExpression));
        }

        // Step 8: Use more complex dynamic expression
        LOGGER.info("\nStep 8: Using more complex dynamic expression");
        String complexExpression = "#customer != null && #customer.age >= 60 && " +
                "(#customer.membershipLevel == 'Gold' || #customer.membershipLevel == 'Platinum')";
        LOGGER.info("Expression: " + complexExpression);

        for (Customer customer : customers) {
            LOGGER.info(customer.getName() + ": " +
                    validator.validateWithExpression(customer, complexExpression));
        }

        // Step 9: Use expression with the isEligibleForDiscount method
        LOGGER.info("\nStep 9: Using expression with the isEligibleForDiscount method");
        String discountExpression = "#customer != null && #customer.isEligibleForDiscount()";
        LOGGER.info("Expression: " + discountExpression);

        for (Customer customer : customers) {
            LOGGER.info(customer.getName() + " eligible for discount: " +
                    validator.validateWithExpression(customer, discountExpression));
        }

        // Step 10: Use expression with preferred categories
        LOGGER.info("\nStep 10: Using expression with preferred categories");
        String categoriesExpression = "#customer != null && #customer.preferredCategories.contains('Equity')";
        LOGGER.info("Expression: " + categoriesExpression);

        for (Customer customer : customers) {
            LOGGER.info(customer.getName() + " has Equity in preferred categories: " +
                    validator.validateWithExpression(customer, categoriesExpression));
        }

        // Step 11: Create a different validation for young silver members
        LOGGER.info("\nStep 11: Creating a validation for young silver members");
        Map<String, Object> youngSilverParams = new HashMap<>();
        youngSilverParams.put("minAge", 18);
        youngSilverParams.put("maxAge", 30);
        youngSilverParams.put("allowedMembershipLevels", Collections.singletonList("Silver"));

        IntegratedCustomerValidatorDemo youngSilverValidator = new IntegratedCustomerValidatorDemo(
                "youngSilverValidator",
                youngSilverParams,
                rulesEngine
        );

        // Step 12: Validate with the young silver validation
        LOGGER.info("\nStep 12: Validating with the young silver validation");
        for (Customer customer : customers) {
            LOGGER.info(customer.getName() + ": " +
                    youngSilverValidator.validate(customer));
        }

        LOGGER.info("\nIntegrated customer validation demonstration completed");
    }

    /**
     * Create sample customers for demonstration.
     *
     * @return List of sample customers
     */
    private static List<Customer> createSampleCustomers() {
        List<Customer> customers = new ArrayList<>();

        // Valid senior gold customer
        List<String> preferredCategories1 = new ArrayList<>();
        preferredCategories1.add("Equity");
        preferredCategories1.add("FixedIncome");
        customers.add(new Customer("John Doe", 65, "Gold", preferredCategories1));

        // Invalid age customer (too young)
        List<String> preferredCategories2 = new ArrayList<>();
        preferredCategories2.add("ETF");
        customers.add(new Customer("Jane Smith", 45, "Gold", preferredCategories2));

        // Invalid membership customer
        List<String> preferredCategories3 = new ArrayList<>();
        preferredCategories3.add("Commodity");
        customers.add(new Customer("Bob Johnson", 70, "Silver", preferredCategories3));

        // Platinum customer
        List<String> preferredCategories4 = new ArrayList<>();
        preferredCategories4.add("FixedIncome");
        customers.add(new Customer("Alice Brown", 75, "Platinum", preferredCategories4));

        // Young customer
        List<String> preferredCategories5 = new ArrayList<>();
        preferredCategories5.add("ETF");
        customers.add(new Customer("Young Silver", 25, "Silver", preferredCategories5));

        return customers;
    }

    /**
     * Create a new IntegratedCustomerValidatorDemo with the specified parameters.
     *
     * @param name The name of the validation
     * @param parameters Map of validation parameters (minAge, maxAge, allowedMembershipLevels, etc.)
     * @param rulesEngine The rules engine to use
     */
    public IntegratedCustomerValidatorDemo(String name, Map<String, Object> parameters, RulesEngine rulesEngine) {
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
     * Create a validation rule group for customers.
     *
     * @param name The name of the validation
     * @param parameters Map of validation parameters
     * @return The validation rule group
     */
    private RuleGroup createValidationRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
                "CustomerValidationRuleGroup",
                "CustomerValidation",
                name,
                "Validates customer against defined criteria",
                1,
                true // AND operator
        );

        // Create Rule for null check
        Rule nullCheckRule = new Rule(
                "NullCheckRule",
                "#customer != null",
                "Customer must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Create Rule for age validation
        int minAge = parameters.containsKey("minAge") ? (int) parameters.get("minAge") : 0;
        int maxAge = parameters.containsKey("maxAge") ? (int) parameters.get("maxAge") : Integer.MAX_VALUE;

        Rule ageValidationRule = new Rule(
                "AgeValidationRule",
                "#customer != null && #customer.age >= #minAge && #customer.age <= #maxAge",
                "Customer age must be between " + minAge + " and " + maxAge
        );
        ruleGroup.addRule(ageValidationRule, 2);

        // Rule for membership level validation
        @SuppressWarnings("unchecked")
        List<String> allowedMembershipLevels = parameters.containsKey("allowedMembershipLevels") ?
                (List<String>) parameters.get("allowedMembershipLevels") : Collections.emptyList();

        if (!allowedMembershipLevels.isEmpty()) {
            Rule membershipLevelValidationRule = new Rule(
                    "MembershipLevelValidationRule",
                    "#customer != null && (#allowedMembershipLevels.isEmpty() || #allowedMembershipLevels.contains(#customer.membershipLevel))",
                    "Customer membership level must be in the allowed levels list"
            );
            ruleGroup.addRule(membershipLevelValidationRule, 3);
        }

        // Validate that all required parameters exist (except 'customer' which will be provided at validation time)
        Set<String> allParams = RuleParameterExtractor.extractParameters(ruleGroup);
        allParams.remove("customer"); // Remove customer as it will be provided at validation time

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
     * Validate a customer using dynamic evaluation.
     *
     * @param customer The customer to validate
     * @return True if the customer is valid, false otherwise
     */
    public boolean validate(Customer customer) {
        RuleResult result = validateWithResult(customer);
        return result.isTriggered();
    }

    /**
     * Validate a customer and return a detailed result.
     *
     * @param customer The customer to validate
     * @return The validation result
     */
    public RuleResult validateWithResult(Customer customer) {
        // Set the customer in the context
        context.setVariable("customer", customer);

        // Create initial facts map with customer data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("customer", customer);

        // Use RuleParameterExtractor to ensure all required parameters exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(validationRuleGroup, initialFacts);

        // Execute the rule group using the rules engine
        return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), facts);
    }

    /**
     * Validate a customer using a dynamic expression.
     *
     * @param customer The customer to validate
     * @param expression The expression to evaluate
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpression(Customer customer, String expression) {
        // Set the customer in the context
        context.setVariable("customer", customer);

        // Create a rule with the dynamic expression
        Rule dynamicRule = createDynamicValidationRule(expression);

        // Create initial facts map with customer data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("customer", customer);

        // Use RuleParameterExtractor to ensure all required parameters for the dynamic rule exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(dynamicRule, initialFacts);

        // Execute the rule using the rules engine
        RuleResult result = rulesEngine.executeRule(dynamicRule, facts);
        return result.isTriggered();
    }
}