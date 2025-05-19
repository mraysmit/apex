/**
 * A validator for Customer objects.
 * This validator checks if a customer meets certain criteria.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.demo.model.Customer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerValidator implements Validator<Customer> {
    private final String name;
    private final int minAge;
    private final int maxAge;
    private final List<String> allowedMembershipLevels;
    private final RulesEngine rulesEngine;
    private final List<Rule> validationRules;

    /**
     * Create a new CustomerValidator with the specified criteria.
     *
     * @param name The name of the validator
     * @param minAge The minimum age for a valid customer
     * @param maxAge The maximum age for a valid customer
     * @param allowedMembershipLevels The allowed membership levels for a valid customer
     */
    public CustomerValidator(String name, int minAge, int maxAge, String... allowedMembershipLevels) {
        this.name = name;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.allowedMembershipLevels = Arrays.asList(allowedMembershipLevels);
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.validationRules = createValidationRules();
    }

    private List<Rule> createValidationRules() {
        List<Rule> rules = new ArrayList<>();

        // Rule for null check
        rules.add(new Rule(
            "NullCheckRule",
            "#customer != null",
            "Customer must not be null"
        ));

        // Rule for age validation
        rules.add(new Rule(
            "AgeValidationRule",
            "#customer != null && #customer.age >= #minAge && #customer.age <= #maxAge",
            "Customer age must be between " + minAge + " and " + maxAge
        ));

        // Rule for membership level validation
        if (!allowedMembershipLevels.isEmpty()) {
            rules.add(new Rule(
                "MembershipLevelValidationRule",
                "#customer != null && (#allowedMembershipLevels.isEmpty() || #allowedMembershipLevels.contains(#customer.membershipLevel))",
                "Customer membership level must be in the allowed levels list"
            ));
        }

        return rules;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Customer customer) {
        RuleResult result = validateWithResult(customer);
        return result.isTriggered();
    }

    @Override
    public Class<Customer> getType() {
        return Customer.class;
    }

    @Override
    public RuleResult validateWithResult(Customer customer) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("customer", customer);
        facts.put("minAge", minAge);
        facts.put("maxAge", maxAge);
        facts.put("allowedMembershipLevels", allowedMembershipLevels);

        // Execute all rules and return the first failure or success if all pass
        for (Rule rule : validationRules) {
            RuleResult result = rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
            if (!result.isTriggered()) {
                return RuleResult.noMatch();
            }
        }

        return RuleResult.match(getName(), "Customer validation successful");
    }
}
