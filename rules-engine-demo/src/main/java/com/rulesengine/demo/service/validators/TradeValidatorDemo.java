/**
 * A validator for Trade objects.
 * This validator checks if a trade meets certain criteria using the RulesEngine.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.demo.model.Trade;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeValidatorDemo implements Validator<Trade> {
    private final String name;
    private final List<String> allowedValues;
    private final List<String> allowedCategories;
    private final RulesEngine rulesEngine;
    private final RuleGroup validationRuleGroup;

    /**
     * Create a new TradeValidatorDemo with the specified criteria.
     *
     * @param name The name of the validator
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public TradeValidatorDemo(String name, List<String> allowedValues, List<String> allowedCategories) {
        this.name = name;
        this.allowedValues = allowedValues;
        this.allowedCategories = allowedCategories;
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.validationRuleGroup = createValidationRuleGroup();
    }

    /**
     * Create a new TradeValidatorDemo with the specified criteria.
     *
     * @param name The name of the validator
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public TradeValidatorDemo(String name, String[] allowedValues, String[] allowedCategories) {
        this(name, Arrays.asList(allowedValues), Arrays.asList(allowedCategories));
    }

    private RuleGroup createValidationRuleGroup() {
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
        if (!allowedCategories.isEmpty()) {
            String condition = "#allowedCategories.isEmpty() || #allowedCategories.contains(#trade.category)";
            Rule categoryValidationRule = new Rule(
                "CategoryValidationRule",
                condition,
                "Trade category must be in the allowed categories list"
            );
            ruleGroup.addRule(categoryValidationRule, 3);
        }

        return ruleGroup;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Trade trade) {
        RuleResult result = validateWithResult(trade);
        return result.isTriggered();
    }

    @Override
    public Class<Trade> getType() {
        return Trade.class;
    }

    @Override
    public RuleResult validateWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("allowedValues", allowedValues);
        facts.put("allowedCategories", allowedCategories);

        // Execute the rule group using the rules engine
        return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), facts);
    }
}
