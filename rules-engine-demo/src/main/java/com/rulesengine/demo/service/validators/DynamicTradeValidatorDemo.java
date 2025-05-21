/**
 * Demonstration of how to use the DynamicTradeValidatorDemoConfig class with TradeValidator.
 * This class shows the step-by-step process of creating and using a TradeValidator
 * for validating Trade objects using the DynamicTradeValidatorDemoConfig to define validation rules.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Trade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstration of dynamic trade validation using the rules engine.
 */
public class DynamicTradeValidatorDemo {

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private DynamicTradeValidatorDemo() {
        // Private constructor to prevent instantiation
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Starting dynamic trade validation demonstration");

        // Step 1: Create a RulesEngine
        System.out.println("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a DynamicTradeValidatorDemoConfig
        System.out.println("Step 2: Creating a DynamicTradeValidatorDemoConfig");
        DynamicTradeValidatorDemoConfig config = new DynamicTradeValidatorDemoConfig(rulesEngine);

        // Step 3: Create parameters for equity trades
        System.out.println("Step 3: Creating parameters for equity trades");
        Map<String, Object> equityParams = new HashMap<>();
        equityParams.put("allowedValues", Arrays.asList("Equity"));
        equityParams.put("allowedCategories", Arrays.asList("InstrumentType"));

        // Step 4: Create a TradeValidator for equity trades
        System.out.println("Step 4: Creating a TradeValidator for equity trades");
        TradeValidator equityValidator = new TradeValidator(
                "equityValidator", 
                equityParams,
                config
        );

        // Step 5: Create sample trades
        System.out.println("Step 5: Creating sample trades");
        Trade validEquityTrade = new Trade("T001", "Equity", "InstrumentType");
        Trade invalidEquityTrade = new Trade("T002", "Bond", "InstrumentType");
        Trade etfTrade = new Trade("T003", "ETF", "InstrumentType");

        // Step 6: Validate trades using standard validation
        System.out.println("\nStep 6: Validating trades using standard validation");
        System.out.println("Valid equity trade: " + equityValidator.validate(validEquityTrade));
        System.out.println("Invalid equity trade: " + equityValidator.validate(invalidEquityTrade));
        System.out.println("ETF trade: " + equityValidator.validate(etfTrade));

        // Step 7: Get detailed validation results
        System.out.println("\nStep 7: Getting detailed validation results");
        RuleResult validResult = equityValidator.validateWithResult(validEquityTrade);
        System.out.println("Valid trade result: " + validResult);
        System.out.println("Valid trade triggered: " + validResult.isTriggered());
        System.out.println("Valid trade rule name: " + validResult.getRuleName());

        RuleResult invalidResult = equityValidator.validateWithResult(invalidEquityTrade);
        System.out.println("Invalid trade result: " + invalidResult);
        System.out.println("Invalid trade triggered: " + invalidResult.isTriggered());

        // Step 8: Validate trades using dynamic expressions
        System.out.println("\nStep 8: Validating trades using dynamic expressions");
        String customExpression = "#trade != null && #trade.value == 'Equity' && #trade.id.startsWith('T')";
        System.out.println("Valid equity trade with custom expression: " + 
                equityValidator.validateWithExpression(validEquityTrade, customExpression, config));
        System.out.println("Invalid equity trade with custom expression: " + 
                equityValidator.validateWithExpression(invalidEquityTrade, customExpression, config));

        // Step 9: Use more complex dynamic expression
        System.out.println("\nStep 9: Using more complex dynamic expression");
        String complexExpression = "#trade != null && (#trade.value == 'Equity' || #trade.value == 'ETF') && #trade.category == 'InstrumentType'";
        System.out.println("Valid equity trade with complex expression: " + 
                equityValidator.validateWithExpression(validEquityTrade, complexExpression, config));
        System.out.println("ETF trade with complex expression: " + 
                equityValidator.validateWithExpression(etfTrade, complexExpression, config));

        // Step 10: Use expression with trade ID
        System.out.println("\nStep 10: Using expression with trade ID");
        String idExpression = "#trade != null && #trade.id.startsWith('T')";
        System.out.println("Valid equity trade with ID expression: " + 
                equityValidator.validateWithExpression(validEquityTrade, idExpression, config));
        System.out.println("Invalid equity trade with ID expression: " + 
                equityValidator.validateWithExpression(invalidEquityTrade, idExpression, config));

        System.out.println("\nDynamic trade validation demonstration completed");
    }
}
