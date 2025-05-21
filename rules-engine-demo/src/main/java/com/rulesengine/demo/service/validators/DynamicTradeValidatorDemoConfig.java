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
 * Configuration class for DynamicTradeValidatorDemo.
 * This class creates configurations for the functionality of the DynamicTradeValidatorDemo class.
 */
public class DynamicTradeValidatorDemoConfig {
    private final RulesEngine rulesEngine;

    /**
     * Create a new DynamicTradeValidatorDemoConfig with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for validation
     */
    public DynamicTradeValidatorDemoConfig(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    /**
     * Create a validation rule group for trades.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters (allowedValues, allowedCategories, etc.)
     * @return The validation rule group
     * @throws IllegalArgumentException if required parameters are missing
     */
    public RuleGroup createValidationRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
            "TradeValidationRuleGroup",
            "TradeValidation",
            name,
            "Validates trade against defined criteria",
            1,
            true // AND operator
        );

        // Rule for null check
        Rule nullCheckRule = new Rule(
            "NullCheckRule",
            "#trade != null",
            "Trade must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Rule for value validation
        @SuppressWarnings("unchecked")
        List<String> allowedValues = parameters.containsKey("allowedValues") ? 
            (List<String>)parameters.get("allowedValues") : Collections.emptyList();

        if (!allowedValues.isEmpty()) {
            String condition = "#allowedValues.isEmpty() || #allowedValues.contains(#trade.value)";
            Rule valueValidationRule = new Rule(
                "ValueValidationRule",
                condition,
                "Trade value must be in the allowed values list"
            );
            ruleGroup.addRule(valueValidationRule, 2);
        }

        // Rule for category validation
        @SuppressWarnings("unchecked")
        List<String> allowedCategories = parameters.containsKey("allowedCategories") ? 
            (List<String>)parameters.get("allowedCategories") : Collections.emptyList();

        if (!allowedCategories.isEmpty()) {
            String condition = "#allowedCategories.isEmpty() || #allowedCategories.contains(#trade.category)";
            Rule categoryValidationRule = new Rule(
                "CategoryValidationRule",
                condition,
                "Trade category must be in the allowed categories list"
            );
            ruleGroup.addRule(categoryValidationRule, 3);
        }

        // Validate that all required parameters exist (except 'trade' which will be provided at validation time)
        Set<String> allParams = RuleParameterExtractor.extractParameters(ruleGroup);
        allParams.remove("trade"); // Remove trade as it will be provided at validation time

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