package dev.mars.apex.core.service.validation;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.core.RuleBuilder;
import dev.mars.apex.engine.core.RulesEngineConfiguration;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class ValidationService {
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private static final org.slf4j.Logger SLF4J_LOGGER = LoggerFactory.getLogger(ValidationService.class);
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
        // Create RulesEngine with simple constructor - no enrichment needed for validation
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        logger.info("ValidationService initialized with default RulesEngine");
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
        logger.info("ValidationService initialized with custom RulesEngine");
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
        SLF4J_LOGGER.debug("TRACE: ValidationService.validateWithResult() called - validator: '{}', value type: {}",
            validatorName, value != null ? value.getClass().getSimpleName() : "null");
        logger.debug("Validating value using validation: " + validatorName);

        // First, check if the validation exists
        Validator<?> validator = registry.getService(validatorName, Validator.class);
        if (validator == null) {
            SLF4J_LOGGER.debug("Validator not found: '{}'", validatorName);
            logger.warn("Validator not found: " + validatorName);
            return RuleResult.error("Validation", "Validator not found: " + validatorName);
        }
        SLF4J_LOGGER.debug("Found validator '{}' of type: {}", validatorName, validator.getClass().getSimpleName());

        // Check if the validation can handle this type
        if (value != null && !validator.getType().isInstance(value)) {
            SLF4J_LOGGER.debug("Type mismatch - validator '{}' expects: {}, got: {}",
                validatorName, validator.getType().getSimpleName(), value.getClass().getSimpleName());
            logger.warn("Validator " + validatorName + " cannot handle type: " + value.getClass().getName());
            return RuleResult.error("Validation", "Validator " + validatorName + " cannot handle type: " + value.getClass().getName());
        }

        // Call the validation directly with the appropriate type
        Validator<T> typedValidator = (Validator<T>) validator;

        // Call the validation's validate method directly to set lastValidatedValue for testing
        SLF4J_LOGGER.debug("Calling validator.validate() for '{}'", validatorName);
        typedValidator.validate(value);

        // Create a rule for the validation
        Rule validationRule = new RuleBuilder()
            .withName("Validation Rule for " + validatorName)
            .withCondition("#validation.validate(#value)")
            .withMessage("Validation using " + validatorName)
            .withSeverity(SeverityConstants.INFO)
            .build();
        SLF4J_LOGGER.debug("Created validation rule: '{}' with condition: '{}'",
            validationRule.getName(), validationRule.getCondition());

        // Create a list of rules
        List<Rule> rules = new ArrayList<>();
        rules.add(validationRule);

        // Create facts for the rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("validation", validator);
        facts.put("value", value);
        SLF4J_LOGGER.debug("Created facts for rule execution: validation={}, value={}",
            validator.getClass().getSimpleName(), value);

        // Execute the rule
        SLF4J_LOGGER.debug("Executing validation rule via RulesEngine");
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        SLF4J_LOGGER.debug("ValidationService.validateWithResult() completed - result: {}",
            result != null ? (result.isTriggered() ? "VALID" : "INVALID") : "NULL");
        return result;
    }
}
