package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CustomerValidator using DynamicCustomerValidatorDemoConfig.
 */
public class DynamicCustomerValidatorDemoTest {
    private CustomerValidator seniorGoldValidator;
    private DynamicCustomerValidatorDemoConfig config;
    private Customer validSeniorGoldCustomer;
    private Customer invalidAgeCustomer;
    private Customer invalidMembershipCustomer;
    private Customer platinumCustomer;

    @BeforeEach
    public void setUp() {
        // Create a RulesEngine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create a DynamicCustomerValidatorDemoConfig
        config = new DynamicCustomerValidatorDemoConfig(rulesEngine);

        // Create parameters map for senior gold members
        Map<String, Object> seniorGoldParams = new HashMap<>();
        seniorGoldParams.put("minAge", 60);
        seniorGoldParams.put("maxAge", 100);
        seniorGoldParams.put("allowedMembershipLevels", Collections.singletonList("Gold"));

        // Create a validator for senior gold members
        seniorGoldValidator = new CustomerValidator(
            "seniorGoldValidator", 
            seniorGoldParams,
            config
        );

        // Create sample customers
        List<String> preferredCategories1 = new ArrayList<>();
        preferredCategories1.add("Equity");
        preferredCategories1.add("FixedIncome");
        validSeniorGoldCustomer = new Customer("John Doe", 65, "Gold", preferredCategories1);

        List<String> preferredCategories2 = new ArrayList<>();
        preferredCategories2.add("ETF");
        invalidAgeCustomer = new Customer("Jane Smith", 45, "Gold", preferredCategories2);

        List<String> preferredCategories3 = new ArrayList<>();
        preferredCategories3.add("Commodity");
        invalidMembershipCustomer = new Customer("Bob Johnson", 70, "Silver", preferredCategories3);

        List<String> preferredCategories4 = new ArrayList<>();
        preferredCategories4.add("FixedIncome");
        platinumCustomer = new Customer("Alice Brown", 75, "Platinum", preferredCategories4);
    }

    @Test
    public void testValidateWithValidCustomer() {
        // Test standard validation with valid customer
        boolean result = seniorGoldValidator.validate(validSeniorGoldCustomer);
        assertTrue(result, "Valid senior gold customer should be valid");

        // Test detailed result
        RuleResult detailedResult = seniorGoldValidator.validateWithResult(validSeniorGoldCustomer);
        assertTrue(detailedResult.isTriggered(), "Valid senior gold customer should trigger rule");
        assertEquals("seniorGoldValidator", detailedResult.getRuleName(), "Rule name should match");
    }

    @Test
    public void testValidateWithInvalidAgeCustomer() {
        // Test standard validation with invalid age customer
        boolean result = seniorGoldValidator.validate(invalidAgeCustomer);
        assertFalse(result, "Invalid age customer should be invalid");

        // Test detailed result
        RuleResult detailedResult = seniorGoldValidator.validateWithResult(invalidAgeCustomer);
        assertFalse(detailedResult.isTriggered(), "Invalid age customer should not trigger rule");
    }

    @Test
    public void testValidateWithInvalidMembershipCustomer() {
        // Test standard validation with invalid membership customer
        boolean result = seniorGoldValidator.validate(invalidMembershipCustomer);
        assertFalse(result, "Invalid membership customer should be invalid");

        // Test detailed result
        RuleResult detailedResult = seniorGoldValidator.validateWithResult(invalidMembershipCustomer);
        assertFalse(detailedResult.isTriggered(), "Invalid membership customer should not trigger rule");
    }

    @Test
    public void testValidateWithExpression() {
        // Test dynamic expression validation
        String customExpression = "#customer != null && #customer.age >= 60 && #customer.membershipLevel == 'Gold'";

        // Valid customer with custom expression
        boolean validResult = seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, customExpression, config);
        assertTrue(validResult, "Valid senior gold customer should be valid with custom expression");

        // Invalid age customer with custom expression
        boolean invalidAgeResult = seniorGoldValidator.validateWithExpression(invalidAgeCustomer, customExpression, config);
        assertFalse(invalidAgeResult, "Invalid age customer should be invalid with custom expression");

        // Invalid membership customer with custom expression
        boolean invalidMembershipResult = seniorGoldValidator.validateWithExpression(invalidMembershipCustomer, customExpression, config);
        assertFalse(invalidMembershipResult, "Invalid membership customer should be invalid with custom expression");
    }

    @Test
    public void testComplexExpression() {
        // Test complex expression that allows both Gold and Platinum customers with age >= 60
        String complexExpression = "#customer != null && #customer.age >= 60 && (#customer.membershipLevel == 'Gold' || #customer.membershipLevel == 'Platinum')";

        // Platinum customer with standard validation (should fail)
        boolean platinumStandardResult = seniorGoldValidator.validate(platinumCustomer);
        assertFalse(platinumStandardResult, "Platinum customer should be invalid with standard validation");

        // Platinum customer with complex expression (should pass)
        boolean platinumComplexResult = seniorGoldValidator.validateWithExpression(platinumCustomer, complexExpression, config);
        assertTrue(platinumComplexResult, "Platinum customer should be valid with complex expression");

        // Gold customer with complex expression (should pass)
        boolean goldComplexResult = seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, complexExpression, config);
        assertTrue(goldComplexResult, "Gold customer should be valid with complex expression");
    }

    @Test
    public void testNullCustomer() {
        // Test with null customer
        boolean result = seniorGoldValidator.validate(null);
        assertFalse(result, "Null customer should be invalid");

        // Test with null customer and custom expression
        String customExpression = "#customer != null";
        boolean expressionResult = seniorGoldValidator.validateWithExpression(null, customExpression, config);
        assertFalse(expressionResult, "Null customer should be invalid with custom expression");
    }

    @Test
    public void testIsEligibleForDiscountExpression() {
        // Test expression using the isEligibleForDiscount method
        String discountExpression = "#customer != null && #customer.isEligibleForDiscount()";

        // Valid senior gold customer with discount expression (should pass - both age > 60 and Gold membership)
        boolean validResult = seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, discountExpression, config);
        assertTrue(validResult, "Valid senior gold customer should be eligible for discount");

        // Invalid age customer with discount expression (should pass - Gold membership)
        boolean invalidAgeResult = seniorGoldValidator.validateWithExpression(invalidAgeCustomer, discountExpression, config);
        assertTrue(invalidAgeResult, "Invalid age customer should be eligible for discount (Gold membership)");

        // Invalid membership customer with discount expression (should pass - age > 60)
        boolean invalidMembershipResult = seniorGoldValidator.validateWithExpression(invalidMembershipCustomer, discountExpression, config);
        assertTrue(invalidMembershipResult, "Invalid membership customer should be eligible for discount (age > 60)");

        // Create a customer that is not eligible for discount
        List<String> preferredCategories = new ArrayList<>();
        preferredCategories.add("ETF");
        Customer nonEligibleCustomer = new Customer("Young Silver", 30, "Silver", preferredCategories);

        // Non-eligible customer with discount expression (should fail)
        boolean nonEligibleResult = seniorGoldValidator.validateWithExpression(nonEligibleCustomer, discountExpression, config);
        assertFalse(nonEligibleResult, "Non-eligible customer should not be eligible for discount");
    }

    @Test
    public void testPreferredCategoriesExpression() {
        // Test expression using the preferred categories
        String categoriesExpression = "#customer != null && #customer.preferredCategories.contains('Equity')";

        // Customer with Equity in preferred categories
        boolean validResult = seniorGoldValidator.validateWithExpression(validSeniorGoldCustomer, categoriesExpression, config);
        assertTrue(validResult, "Customer with Equity in preferred categories should be valid");

        // Customer without Equity in preferred categories
        boolean invalidResult = seniorGoldValidator.validateWithExpression(invalidAgeCustomer, categoriesExpression, config);
        assertFalse(invalidResult, "Customer without Equity in preferred categories should be invalid");
    }
}
