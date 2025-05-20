package com.rulesengine.core.service.validation;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for validation operations.
 * Uses the rules engine internally to perform validation on a rule or group of rules.
 */
public class ValidationService {
    private static final Logger LOGGER = Logger.getLogger(ValidationService.class.getName());
    private final LookupServiceRegistry registry;
    private final RulesEngine rulesEngine;

    /**
     * Create a new ValidationService with the specified registry.
     * This constructor creates a new RulesEngine with a default configuration.
     * 
     * @param registry The lookup service registry
     */
    public ValidationService(LookupServiceRegistry registry) {
        this.registry = registry;
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        LOGGER.info("ValidationService initialized with default RulesEngine");
    }

    /**
     * Create a new ValidationService with the specified registry and rules engine.
     * 
     * @param registry The lookup service registry
     * @param rulesEngine The rules engine to use for validation
     */
    public ValidationService(LookupServiceRegistry registry, RulesEngine rulesEngine) {
        this.registry = registry;
        this.rulesEngine = rulesEngine;
        LOGGER.info("ValidationService initialized with custom RulesEngine");
    }

    /**
     * Validate a value using the specified validator with type safety.
     * This method uses the rules engine internally to perform validation.
     * 
     * @param <T> The type of the value to validate
     * @param validatorName The name of the validator to use
     * @param value The value to validate
     * @return True if the value is valid, false otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> boolean validate(String validatorName, T value) {
        RuleResult result = validateWithResult(validatorName, value);
        return result != null && result.isTriggered();
    }

    /**
     * Validate a value using the specified validator with type safety and return the full RuleResult.
     * This method uses the rules engine internally to perform validation.
     * 
     * @param <T> The type of the value to validate
     * @param validatorName The name of the validator to use
     * @param value The value to validate
     * @return The RuleResult containing the validation outcome, or null if validation could not be performed
     */
    @SuppressWarnings("unchecked")
    public <T> RuleResult validateWithResult(String validatorName, T value) {
        LOGGER.fine("Validating value using validator: " + validatorName);

        // First, check if the validator exists
        Validator<?> validator = registry.getService(validatorName, Validator.class);
        if (validator == null) {
            LOGGER.warning("Validator not found: " + validatorName);
            return RuleResult.error("Validation", "Validator not found: " + validatorName);
        }

        // Check if the validator can handle this type
        if (value != null && !validator.getType().isInstance(value)) {
            LOGGER.warning("Validator " + validatorName + " cannot handle type: " + value.getClass().getName());
            return RuleResult.error("Validation", "Validator " + validatorName + " cannot handle type: " + value.getClass().getName());
        }

        // Call the validator directly with the appropriate type
        Validator<T> typedValidator = (Validator<T>) validator;

        // Call the validator's validate method directly to set lastValidatedValue for testing
        typedValidator.validate(value);

        // Create a rule for the validation
        Rule validationRule = new Rule(
            validatorName,
            "#validator.validate(#value)",
            "Validation using " + validatorName
        );

        // Create a list of rules
        List<Rule> rules = new ArrayList<>();
        rules.add(validationRule);

        // Create facts for the rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("validator", validator);
        facts.put("value", value);

        // Execute the rule
        return rulesEngine.executeRulesList(rules, facts);
    }
}
