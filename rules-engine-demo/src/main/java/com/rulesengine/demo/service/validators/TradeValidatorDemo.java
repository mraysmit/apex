/**
 * A validator for Trade objects.
 * This validator checks if a trade meets certain criteria.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.demo.model.Trade;

import java.util.ArrayList;
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
    private final List<Rule> validationRules;

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
        this.validationRules = createValidationRules();
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

    private List<Rule> createValidationRules() {
        List<Rule> rules = new ArrayList<>();

        // Rule for null check
        rules.add(new Rule(
            "NullCheckRule",
            "#trade != null",
            "Trade must not be null"
        ));

        // Rule for value validation
        if (!allowedValues.isEmpty()) {
            StringBuilder condition = new StringBuilder("#allowedValues.isEmpty() || #allowedValues.contains(#trade.value)");
            rules.add(new Rule(
                "ValueValidationRule",
                condition.toString(),
                "Trade value must be in the allowed values list"
            ));
        }

        // Rule for category validation
        if (!allowedCategories.isEmpty()) {
            StringBuilder condition = new StringBuilder("#allowedCategories.isEmpty() || #allowedCategories.contains(#trade.category)");
            rules.add(new Rule(
                "CategoryValidationRule",
                condition.toString(),
                "Trade category must be in the allowed categories list"
            ));
        }

        return rules;
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

        // Execute all rules and return the first failure or success if all pass
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
            if (!result.isTriggered()) {
                return RuleResult.noMatch();
            }
        }

        return RuleResult.match(getName(), "Trade validation successful");
    }
}
