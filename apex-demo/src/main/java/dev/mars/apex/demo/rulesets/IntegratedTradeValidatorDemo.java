package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;

import dev.mars.apex.core.service.validation.Validator;
import dev.mars.apex.core.util.RuleParameterExtractor;
import dev.mars.apex.demo.bootstrap.model.Trade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.springframework.expression.spel.support.StandardEvaluationContext;

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
 * Comprehensive demonstration of trade validation using the rules engine.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of trade validation using the rules engine.
 *
 * This integrated class combines the functionality of:
 * - DynamicTradeValidatorDemoRuleConfig (rule configuration)
 * - TradeValidator (validation logic)
 * - DynamicTradeValidatorDemo (demonstration)
 *
 * It demonstrates how to create, configure, and use a trade validation
 * with various validation scenarios in a clear, sequential manner.
 */
public class IntegratedTradeValidatorDemo implements Validator<Trade> {
    private static final Logger LOGGER = Logger.getLogger(IntegratedTradeValidatorDemo.class.getName());

    // Core components
    private final RulesEngine rulesEngine;
    private final String validatorName;
    private final Map<String, Object> parameters;
    private final StandardEvaluationContext context;
    private final RuleGroup validationRuleGroup;

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        runTradeValidationDemo();
    }

    /**
     * Run the trade validation demonstration.
     * This method shows the complete process of creating and using a trade validation.
     */
    private static void runTradeValidationDemo() {
        LOGGER.info("Starting integrated trade validation demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create parameters for equity trades
        LOGGER.info("Step 2: Creating parameters for equity trades");
        Map<String, Object> equityParams = new HashMap<>();
        equityParams.put("allowedValues", Arrays.asList("Equity"));
        equityParams.put("allowedCategories", Arrays.asList("InstrumentType"));

        // Step 3: Create an IntegratedTradeValidatorDemo instance
        LOGGER.info("Step 3: Creating an IntegratedTradeValidatorDemo instance");
        IntegratedTradeValidatorDemo equityValidator = new IntegratedTradeValidatorDemo(
                "equityValidator",
                equityParams,
                rulesEngine
        );

        // Step 4: Create sample trades
        LOGGER.info("Step 4: Creating sample trades");
        List<Trade> trades = createSampleTrades();

        // Step 5: Validate trades using standard validation
        LOGGER.info("\nStep 5: Validating trades using standard validation");
        for (Trade trade : trades) {
            LOGGER.info(trade.getId() + " (Value: " + trade.getValue() +
                    ", Category: " + trade.getCategory() + "): " +
                    equityValidator.validate(trade));
        }

        // Step 6: Get detailed validation results
        LOGGER.info("\nStep 6: Getting detailed validation results");
        Trade validEquityTrade = trades.get(0); // T001 Equity
        Trade invalidEquityTrade = trades.get(1); // T002 Bond

        RuleResult validResult = equityValidator.validateWithResult(validEquityTrade);
        LOGGER.info("Valid trade result: " + validResult);
        LOGGER.info("Valid trade triggered: " + validResult.isTriggered());
        LOGGER.info("Valid trade rule name: " + validResult.getRuleName());

        RuleResult invalidResult = equityValidator.validateWithResult(invalidEquityTrade);
        LOGGER.info("Invalid trade result: " + invalidResult);
        LOGGER.info("Invalid trade triggered: " + invalidResult.isTriggered());

        // Step 7: Validate trades using dynamic expressions
        LOGGER.info("\nStep 7: Validating trades using dynamic expressions");
        String customExpression = "#trade != null && #trade.value == 'Equity' && #trade.id.startsWith('T')";
        LOGGER.info("Expression: " + customExpression);

        for (Trade trade : trades) {
            LOGGER.info(trade.getId() + ": " +
                    equityValidator.validateWithExpression(trade, customExpression));
        }

        // Step 8: Use more complex dynamic expression
        LOGGER.info("\nStep 8: Using more complex dynamic expression");
        String complexExpression = "#trade != null && (#trade.value == 'Equity' || #trade.value == 'ETF') && #trade.category == 'InstrumentType'";
        LOGGER.info("Expression: " + complexExpression);

        for (Trade trade : trades) {
            LOGGER.info(trade.getId() + ": " +
                    equityValidator.validateWithExpression(trade, complexExpression));
        }

        // Step 9: Use expression with trade ID
        LOGGER.info("\nStep 9: Using expression with trade ID");
        String idExpression = "#trade != null && #trade.id.startsWith('T')";
        LOGGER.info("Expression: " + idExpression);

        for (Trade trade : trades) {
            LOGGER.info(trade.getId() + ": " +
                    equityValidator.validateWithExpression(trade, idExpression));
        }

        // Step 10: Create a different validation for bond trades
        LOGGER.info("\nStep 10: Creating a validation for bond trades");
        Map<String, Object> bondParams = new HashMap<>();
        bondParams.put("allowedValues", Arrays.asList("Bond"));
        bondParams.put("allowedCategories", Arrays.asList("InstrumentType"));

        IntegratedTradeValidatorDemo bondValidator = new IntegratedTradeValidatorDemo(
                "bondValidator",
                bondParams,
                rulesEngine
        );

        // Step 11: Validate with the bond validation
        LOGGER.info("\nStep 11: Validating with the bond validation");
        for (Trade trade : trades) {
            LOGGER.info(trade.getId() + ": " +
                    bondValidator.validate(trade));
        }

        LOGGER.info("\nIntegrated trade validation demonstration completed");
    }

    /**
     * Create sample trades for demonstration.
     *
     * @return List of sample trades
     */
    private static List<Trade> createSampleTrades() {
        List<Trade> trades = new ArrayList<>();

        // Valid equity trade
        trades.add(new Trade("T001", "Equity", "InstrumentType"));

        // Bond trade
        trades.add(new Trade("T002", "Bond", "InstrumentType"));

        // ETF trade
        trades.add(new Trade("T003", "ETF", "InstrumentType"));

        // Different category trade
        trades.add(new Trade("T004", "Equity", "AssetClass"));

        return trades;
    }

    /**
     * Create a new IntegratedTradeValidatorDemo with the specified parameters.
     *
     * @param name The name of the validation
     * @param parameters Map of validation parameters (allowedValues, allowedCategories, etc.)
     * @param rulesEngine The rules engine to use
     */
    public IntegratedTradeValidatorDemo(String name, Map<String, Object> parameters, RulesEngine rulesEngine) {
        this.validatorName = name;
        this.parameters = new HashMap<>(parameters);
        this.rulesEngine = rulesEngine;
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group
        this.validationRuleGroup = createValidationRuleGroup(name, parameters);
    }

    /**
     * Create a new IntegratedTradeValidatorDemo with the specified criteria.
     * This constructor is provided for backward compatibility.
     *
     * @param name The name of the validation
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public IntegratedTradeValidatorDemo(String name, String[] allowedValues, String[] allowedCategories) {
        this.validatorName = name;
        this.parameters = new HashMap<>();
        parameters.put("allowedValues", Arrays.asList(allowedValues));
        parameters.put("allowedCategories", Arrays.asList(allowedCategories));

        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group
        this.validationRuleGroup = createValidationRuleGroup(name, parameters);
    }

    /**
     * Create a new IntegratedTradeValidatorDemo with the specified criteria.
     * This constructor is provided for backward compatibility.
     *
     * @param name The name of the validation
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public IntegratedTradeValidatorDemo(String name, List<String> allowedValues, List<String> allowedCategories) {
        this.validatorName = name;
        this.parameters = new HashMap<>();
        parameters.put("allowedValues", allowedValues);
        parameters.put("allowedCategories", allowedCategories);

        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Create validation rule group
        this.validationRuleGroup = createValidationRuleGroup(name, parameters);
    }

    /**
     * Create a validation rule group for trades.
     *
     * @param name The name of the validation
     * @param parameters Map of validation parameters
     * @return The validation rule group
     */
    private RuleGroup createValidationRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
                "TradeValidationRuleGroup",
                "TradeValidation",
                name,
                "Validates trade against defined criteria",
                1,
                true // AND operator
        );

        // Create Rule for null check
        Rule nullCheckRule = new Rule(
                "NullCheckRule",
                "#trade != null",
                "TradeB must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Rule for value validation
        @SuppressWarnings("unchecked")
        List<String> allowedValues = parameters.containsKey("allowedValues") ?
                (List<String>) parameters.get("allowedValues") : Collections.emptyList();

        if (!allowedValues.isEmpty()) {
            String condition = "#allowedValues.isEmpty() || #allowedValues.contains(#trade.value)";
            Rule valueValidationRule = new Rule(
                    "ValueValidationRule",
                    condition,
                    "TradeB value must be in the allowed values list"
            );
            ruleGroup.addRule(valueValidationRule, 2);
        }

        // Rule for category validation
        @SuppressWarnings("unchecked")
        List<String> allowedCategories = parameters.containsKey("allowedCategories") ?
                (List<String>) parameters.get("allowedCategories") : Collections.emptyList();

        if (!allowedCategories.isEmpty()) {
            String condition = "#allowedCategories.isEmpty() || #allowedCategories.contains(#trade.category)";
            Rule categoryValidationRule = new Rule(
                    "CategoryValidationRule",
                    condition,
                    "TradeB category must be in the allowed categories list"
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
    private Rule createDynamicValidationRule(String expression) {
        return new Rule(
                "DynamicValidationRule",
                expression,
                "Dynamic validation rule"
        );
    }

    @Override
    public String getName() {
        return validatorName;
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
        // Set the trade in the context
        context.setVariable("trade", trade);

        // Create initial facts map with trade data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("trade", trade);

        // Use RuleParameterExtractor to ensure all required parameters exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(validationRuleGroup, initialFacts);

        // Execute the rule group using the rules engine
        return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), facts);
    }

    /**
     * Validate a trade using a dynamic expression.
     *
     * @param trade The trade to validate
     * @param expression The expression to evaluate
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpression(Trade trade, String expression) {
        // Set the trade in the context
        context.setVariable("trade", trade);

        // Create a rule with the dynamic expression
        Rule dynamicRule = createDynamicValidationRule(expression);

        // Create initial facts map with trade data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("trade", trade);

        // Use RuleParameterExtractor to ensure all required parameters for the dynamic rule exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(dynamicRule, initialFacts);

        // Execute the rule using the rules engine
        RuleResult result = rulesEngine.executeRule(dynamicRule, facts);
        return result.isTriggered();
    }
}
