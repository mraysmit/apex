package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleGroup;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.common.NamedService;
import com.rulesengine.core.util.RuleParameterExtractor;
import com.rulesengine.demo.model.Customer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * A validator for Customer objects using the RulesEngine.
 * This class provides methods to validate customers against defined criteria.
 */
public class CustomerValidator implements NamedService {
    private final String name;
    private final Map<String, Object> parameters;
    private final RulesEngine rulesEngine;
    private final RuleGroup validationRuleGroup;
    private final StandardEvaluationContext context;

    /**
     * Create a new CustomerValidator with the specified parameters.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters (minAge, maxAge, allowedMembershipLevels, etc.)
     * @param config The configuration to use for creating validation rules
     */
    public CustomerValidator(String name, Map<String, Object> parameters, DynamicCustomerValidatorDemoConfig config) {
        this.name = name;
        this.parameters = new HashMap<>(parameters);
        this.rulesEngine = config.getRulesEngine();
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group using the config
        this.validationRuleGroup = config.createValidationRuleGroup(name, parameters);
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
     * @param config The configuration to use for creating the dynamic rule
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpression(Customer customer, String expression, DynamicCustomerValidatorDemoConfig config) {
        // Set the customer in the context
        context.setVariable("customer", customer);

        // Create a rule with the dynamic expression
        Rule dynamicRule = config.createDynamicValidationRule(expression);

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
