package com.rulesengine.demo.service.providers;

import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.lookup.RecordMatcher;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Demonstration of how to use RecordMatcher for Trade objects.
 * This class shows the step-by-step process of creating and using a RecordMatcher
 * for finding trades that match specific validation criteria.
 *
 * This class implements RecordMatcher<Trade> to provide matching functionality.
 */
public class TradeRecordMatcherDemo implements RecordMatcher<Trade> {
    private static final Logger LOGGER = Logger.getLogger(TradeRecordMatcherDemo.class.getName());
    private final LookupServiceRegistry registry;

    /**
     * Create a new TradeRecordMatcherDemo with the specified registry.
     *
     * @param registry The lookup service registry
     */
    public TradeRecordMatcherDemo(LookupServiceRegistry registry) {
        this.registry = registry;
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Run the demonstration
        runTradeRecordMatcherDemo();
    }

    /**
     * Run the trade record matcher demonstration.
     * This method shows the step-by-step process of creating and using a RecordMatcher.
     */
    private static void runTradeRecordMatcherDemo() {
        LOGGER.info("Starting trade record matcher demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a LookupServiceRegistry
        LOGGER.info("Step 2: Creating a LookupServiceRegistry");
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Step 3: Create and register validators
        LOGGER.info("Step 3: Creating and registering validators");
        registerValidators(registry, rulesEngine);

        // Step 4: Create a TradeRecordMatcherDemo
        LOGGER.info("Step 4: Creating a TradeRecordMatcherDemo");
        RecordMatcher<Trade> recordMatcher = new TradeRecordMatcherDemo(registry);

        // Step 5: Create test trades
        LOGGER.info("Step 5: Creating test trades");
        List<Trade> trades = createTestTrades();

        // Step 6: Define validator names to match against
        LOGGER.info("Step 6: Defining validator names to match against");
        List<String> validatorNames = Arrays.asList("equityValidator", "fixedIncomeValidator");

        // Step 7: Find matching trades
        LOGGER.info("Step 7: Finding matching trades");
        List<Trade> matchingTrades = recordMatcher.findMatchingRecords(trades, validatorNames);

        // Display matching trades
        LOGGER.info("Matching trades:");
        for (Trade trade : matchingTrades) {
            LOGGER.info("- " + trade);
        }

        // Step 8: Find non-matching trades
        LOGGER.info("Step 8: Finding non-matching trades");
        List<Trade> nonMatchingTrades = recordMatcher.findNonMatchingRecords(trades, validatorNames);

        // Display non-matching trades
        LOGGER.info("Non-matching trades:");
        for (Trade trade : nonMatchingTrades) {
            LOGGER.info("- " + trade);
        }

        LOGGER.info("Trade record matcher demonstration completed");
    }

    /**
     * Register validators with the registry.
     *
     * @param registry The lookup service registry
     * @param rulesEngine The rules engine
     */
    private static void registerValidators(LookupServiceRegistry registry, RulesEngine rulesEngine) {
        // Create and register validators for different trade types
        registry.registerService(new TradeTypeValidator("equityValidator", PostTradeProcessingServiceDemoConfig.TYPE_EQUITY));
        registry.registerService(new TradeTypeValidator("fixedIncomeValidator", PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME));
        registry.registerService(new TradeTypeValidator("derivativeValidator", PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE));
        registry.registerService(new TradeTypeValidator("forexValidator", PostTradeProcessingServiceDemoConfig.TYPE_FOREX));
        registry.registerService(new TradeTypeValidator("commodityValidator", PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY));
    }

    /**
     * Create a list of test trades.
     *
     * @return A list of test trades
     */
    private static List<Trade> createTestTrades() {
        return Arrays.asList(
            new Trade("T1001", PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, "Stock"),
            new Trade("T1002", PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME, "Bond"),
            new Trade("T1003", PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE, "Option"),
            new Trade("T1004", PostTradeProcessingServiceDemoConfig.TYPE_FOREX, "Spot"),
            new Trade("T1005", PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY, "Future")
        );
    }

    @Override
    public List<Trade> findMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> matchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (hasMatch(trade, validatorNames)) {
                matchingTrades.add(trade);
            }
        }
        return matchingTrades;
    }

    @Override
    public List<Trade> findNonMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> nonMatchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (!hasMatch(trade, validatorNames)) {
                nonMatchingTrades.add(trade);
            }
        }
        return nonMatchingTrades;
    }

    /**
     * Check if a trade matches any of the specified validators.
     *
     * @param trade The trade to check
     * @param validatorNames The names of the validators to check against
     * @return true if the trade matches any validator, false otherwise
     */
    private boolean hasMatch(Trade trade, List<String> validatorNames) {
        for (String validatorName : validatorNames) {
            Validator validator = registry.getService(validatorName, Validator.class);
            if (validator != null && validator.validate(trade.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A validator for trade types.
     * This validator checks if a trade's value matches a specific type using the RulesEngine.
     */
    private static class TradeTypeValidator implements Validator<String> {
        private final String name;
        private final String tradeType;
        private final RulesEngine rulesEngine;
        private final Rule validationRule;

        /**
         * Create a new TradeTypeValidator with the specified name and trade type.
         *
         * @param name The name of the validator
         * @param tradeType The trade type to validate against
         */
        public TradeTypeValidator(String name, String tradeType) {
            this.name = name;
            this.tradeType = tradeType;
            this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
            this.validationRule = createValidationRule();
        }

        /**
         * Create a rule for validating trade types.
         *
         * @return The validation rule
         */
        private Rule createValidationRule() {
            return new Rule(
                "TradeTypeValidationRule",
                "#value != null && #value.equals(#tradeType)",
                "Trade type matches " + tradeType
            );
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(String value) {
            RuleResult result = validateWithResult(value);
            return result.isTriggered();
        }

        @Override
        public RuleResult validateWithResult(String value) {
            Map<String, Object> facts = new HashMap<>();
            facts.put("value", value);
            facts.put("tradeType", tradeType);

            // Execute the rule using the rules engine
            return rulesEngine.executeRulesList(Collections.singletonList(validationRule), facts);
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }
}
