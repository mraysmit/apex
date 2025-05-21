package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.util.RuleParameterExtractor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration class for DynamicCustomerValidatorDemo.
 * This class creates configurations for the functionality of the DynamicCustomerValidatorDemo class.
 */
public class DynamicCustomerValidatorDemoConfig {
    private final RulesEngine rulesEngine;

    /**
     * Create a new DynamicCustomerValidatorDemoConfig with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for validation
     */
    public DynamicCustomerValidatorDemoConfig(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    /**
     * Create a validation rule group for customers.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters (minAge, maxAge, allowedMembershipLevels, etc.)
     * @return The validation rule group
     * @throws IllegalArgumentException if required parameters are missing
     */
    public RuleGroup createValidationRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
            "CustomerValidationRuleGroup",
            "CustomerValidation",
            name,
            "Validates customer against defined criteria",
            1,
            true // AND operator
        );

        // Rule for null check
        Rule nullCheckRule = new Rule(
            "NullCheckRule",
            "#customer != null",
            "Customer must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Rule for age validation
        int minAge = parameters.containsKey("minAge") ? (int)parameters.get("minAge") : 0;
        int maxAge = parameters.containsKey("maxAge") ? (int)parameters.get("maxAge") : Integer.MAX_VALUE;

        Rule ageValidationRule = new Rule(
            "AgeValidationRule",
            "#customer != null && #customer.age >= #minAge && #customer.age <= #maxAge",
            "Customer age must be between " + minAge + " and " + maxAge
        );
        ruleGroup.addRule(ageValidationRule, 2);

        // Rule for membership level validation
        @SuppressWarnings("unchecked")
        List<String> allowedMembershipLevels = parameters.containsKey("allowedMembershipLevels") ? 
            (List<String>)parameters.get("allowedMembershipLevels") : Collections.emptyList();

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
