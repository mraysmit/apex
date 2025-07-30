package dev.mars.apex.core.service.validation;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
 * Service for validation operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
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
     * Validate a value using the specified validation with type safety.
     * This method uses the rules engine internally to perform validation.
     * 
     * @param <T> The type of the value to validate
     * @param validatorName The name of the validation to use
     * @param value The value to validate
     * @return True if the value is valid, false otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> boolean validate(String validatorName, T value) {
        RuleResult result = validateWithResult(validatorName, value);
        return result != null && result.isTriggered();
    }

    /**
     * Validate a value using the specified validation with type safety and return the full RuleResult.
     * This method uses the rules engine internally to perform validation.
     * 
     * @param <T> The type of the value to validate
     * @param validatorName The name of the validation to use
     * @param value The value to validate
     * @return The RuleResult containing the validation outcome, or null if validation could not be performed
     */
    @SuppressWarnings("unchecked")
    public <T> RuleResult validateWithResult(String validatorName, T value) {
        LOGGER.fine("Validating value using validation: " + validatorName);

        // First, check if the validation exists
        Validator<?> validator = registry.getService(validatorName, Validator.class);
        if (validator == null) {
            LOGGER.warning("Validator not found: " + validatorName);
            return RuleResult.error("Validation", "Validator not found: " + validatorName);
        }

        // Check if the validation can handle this type
        if (value != null && !validator.getType().isInstance(value)) {
            LOGGER.warning("Validator " + validatorName + " cannot handle type: " + value.getClass().getName());
            return RuleResult.error("Validation", "Validator " + validatorName + " cannot handle type: " + value.getClass().getName());
        }

        // Call the validation directly with the appropriate type
        Validator<T> typedValidator = (Validator<T>) validator;

        // Call the validation's validate method directly to set lastValidatedValue for testing
        typedValidator.validate(value);

        // Create a rule for the validation
        Rule validationRule = new Rule(
            "Validation Rule for " + validatorName,
            "#validation.validate(#value)",
            "Validation using " + validatorName
        );

        // Create a list of rules
        List<Rule> rules = new ArrayList<>();
        rules.add(validationRule);

        // Create facts for the rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("validation", validator);
        facts.put("value", value);

        // Execute the rule
        return rulesEngine.executeRulesList(rules, facts);
    }
}
