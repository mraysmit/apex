package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.bootstrap.model.Trade;

import java.util.Arrays;
import java.util.Collections;
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
 * Comprehensive demonstration of post-trade processing functionality with RulesEngine.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of post-trade processing functionality with RulesEngine.
 * This class shows the step-by-step process of creating and using a post-trade processing service
 * for trade settlement and related operations. It combines both the demonstration logic and the
 * configuration functionality in a single self-contained class.
 *
 * This is a demo class with a main method for running the demonstration and instance
 * methods for post-trade processing operations.
 */
public class PostTradeProcessingServiceDemo {
    private static final Logger LOGGER = Logger.getLogger(PostTradeProcessingServiceDemo.class.getName());

    // Trade types
    public static final String TYPE_EQUITY = "Equity";
    public static final String TYPE_FIXED_INCOME = "FixedIncome";
    public static final String TYPE_DERIVATIVE = "Derivative";
    public static final String TYPE_FOREX = "Forex";
    public static final String TYPE_COMMODITY = "Commodity";

    // Settlement methods
    public static final String METHOD_DTC = "DTC";
    public static final String METHOD_FEDWIRE = "Fedwire";
    public static final String METHOD_EUROCLEAR = "Euroclear";
    public static final String METHOD_CLEARSTREAM = "Clearstream";
    public static final String METHOD_MANUAL = "Manual";

    // Instance fields for post-trade processing configuration
    private final Map<String, Double> settlementFees = new HashMap<>();
    private final Map<String, Integer> settlementDays = new HashMap<>();
    private final RulesEngine rulesEngine;
    private final Map<String, Rule> settlementRules = new HashMap<>();

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private PostTradeProcessingServiceDemo() {
        // Private constructor to prevent instantiation
        this.rulesEngine = null;
    }

    /**
     * Create a new PostTradeProcessingServiceDemo with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for post-trade processing
     */
    public PostTradeProcessingServiceDemo(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
        initializeDefaultValues();
        initializeRules();
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Run the demonstration
        runPostTradeProcessingDemo();
    }

    /**
     * Run the post-trade processing demonstration.
     * This method shows the step-by-step process of creating and using a post-trade processing service.
     */
    private static void runPostTradeProcessingDemo() {
        LOGGER.info("Starting post-trade processing demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a PostTradeProcessingServiceDemo instance
        LOGGER.info("Step 2: Creating a PostTradeProcessingServiceDemo instance");
        PostTradeProcessingServiceDemo demo = new PostTradeProcessingServiceDemo(rulesEngine);

        // Step 3: Create test trades
        LOGGER.info("Step 3: Creating test trades");
        List<Trade> trades = createTestTrades();

        // Step 4: Process each trade
        LOGGER.info("Step 4: Processing trades");
        for (Trade trade : trades) {
            LOGGER.info("Processing trade: " + trade);

            // Step 4.1: Validate trade for settlement
            validateTradeForSettlement(trade, demo);

            // Step 4.2: Match trade with counterparty
            matchTradeWithCounterparty(trade, demo);

            // Step 4.3: Affirm trade
            affirmTrade(trade, demo);

            // Step 4.4: Confirm trade
            confirmTrade(trade, demo);

            // Step 4.5: Settle trade
            settleTrade(trade, demo);

            // Step 4.6: Determine settlement method
            determineSettlementMethod(trade, demo);

            // Step 4.7: Calculate fees
            calculateFees(trade, demo);

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("Post-trade processing demonstration completed");
    }

    /**
     * Validate a trade for settlement.
     *
     * @param trade The trade to validate
     * @param demo The post-trade processing demo instance
     */
    private static void validateTradeForSettlement(Trade trade, PostTradeProcessingServiceDemo demo) {
        RuleResult result = demo.validateTradeForSettlementWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB is valid for settlement");
        } else {
            LOGGER.info("TradeB is not valid for settlement");
        }
    }

    /**
     * Match a trade with counterparty.
     *
     * @param trade The trade to match
     * @param demo The post-trade processing demo instance
     */
    private static void matchTradeWithCounterparty(Trade trade, PostTradeProcessingServiceDemo demo) {
        RuleResult result = demo.matchTradeWithCounterpartyWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB is matched with counterparty");
        } else {
            LOGGER.info("TradeB is not matched with counterparty");
        }
    }

    /**
     * Affirm a trade.
     *
     * @param trade The trade to affirm
     * @param demo The post-trade processing demo instance
     */
    private static void affirmTrade(Trade trade, PostTradeProcessingServiceDemo demo) {
        RuleResult result = demo.affirmTradeWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB is affirmed");
        } else {
            LOGGER.info("TradeB is not affirmed");
        }
    }

    /**
     * Confirm a trade.
     *
     * @param trade The trade to confirm
     * @param demo The post-trade processing demo instance
     */
    private static void confirmTrade(Trade trade, PostTradeProcessingServiceDemo demo) {
        RuleResult result = demo.confirmTradeWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB is confirmed");
        } else {
            LOGGER.info("TradeB is not confirmed");
        }
    }

    /**
     * Settle a trade.
     *
     * @param trade The trade to settle
     * @param demo The post-trade processing demo instance
     */
    private static void settleTrade(Trade trade, PostTradeProcessingServiceDemo demo) {
        RuleResult result = demo.settleTradeWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB is settled");
        } else {
            LOGGER.info("TradeB is not settled");
        }
    }

    /**
     * Determine settlement method for a trade.
     *
     * @param trade The trade
     * @param demo The post-trade processing demo instance
     */
    private static void determineSettlementMethod(Trade trade, PostTradeProcessingServiceDemo demo) {
        String method = demo.determineSettlementMethod(trade);
        LOGGER.info("Settlement method: " + method);
    }

    /**
     * Calculate fees for a trade.
     *
     * @param trade The trade
     * @param demo The post-trade processing demo instance
     */
    private static void calculateFees(Trade trade, PostTradeProcessingServiceDemo demo) {
        double settlementFee = demo.calculateSettlementFee(trade);
        double clearingFee = demo.calculateClearingFee(trade);
        double custodyFee = demo.calculateCustodyFee(trade);
        int settlementDays = demo.calculateSettlementDays(trade);

        LOGGER.info("Settlement fee: $" + String.format("%.2f", settlementFee));
        LOGGER.info("Clearing fee: $" + String.format("%.2f", clearingFee));
        LOGGER.info("Custody fee: $" + String.format("%.2f", custodyFee));
        LOGGER.info("Settlement days: " + settlementDays);
    }

    /**
     * Create a list of test trades.
     *
     * @return A list of test trades
     */
    private static List<Trade> createTestTrades() {
        return Arrays.asList(
                new Trade("T1001", TYPE_EQUITY, "Stock"),
                new Trade("T1002", TYPE_FIXED_INCOME, "Bond"),
                new Trade("T1003", TYPE_DERIVATIVE, "Option"),
                new Trade("T1004", TYPE_FOREX, "Spot"),
                new Trade("T1005", TYPE_COMMODITY, "Future"),
                new Trade("", TYPE_EQUITY, "Stock") // Invalid trade
        );
    }

    // ========== Configuration Methods (from PostTradeProcessingServiceDemoConfig) ==========

    /**
     * Initialize default values for settlement fees and days.
     */
    private void initializeDefaultValues() {
        LOGGER.info("Initializing post-trade processing configuration");

        // Initialize settlement fees by type
        settlementFees.put(TYPE_EQUITY, 1.50);
        settlementFees.put(TYPE_FIXED_INCOME, 2.25);
        settlementFees.put(TYPE_DERIVATIVE, 3.00);
        settlementFees.put(TYPE_FOREX, 1.00);
        settlementFees.put(TYPE_COMMODITY, 2.50);

        // Initialize settlement days by type
        settlementDays.put(TYPE_EQUITY, 2);
        settlementDays.put(TYPE_FIXED_INCOME, 1);
        settlementDays.put(TYPE_DERIVATIVE, 1);
        settlementDays.put(TYPE_FOREX, 2);
        settlementDays.put(TYPE_COMMODITY, 3);

        LOGGER.info("Configured settlement parameters for " + settlementFees.size() + " trade types");
    }

    /**
     * Initialize rules for post-trade processing.
     */
    private void initializeRules() {
        // Rule for trade validation
        settlementRules.put("TradeValidation", new Rule(
                "TradeValidationRule",
                "#trade != null && #trade.id != null && !#trade.id.isEmpty()",
                "TradeB is valid for settlement"
        ));

        // Rule for trade matching
        settlementRules.put("TradeMatching", new Rule(
                "TradeMatchingRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "TradeB is matched with counterparty"
        ));

        // Rule for trade affirmation
        settlementRules.put("TradeAffirmation", new Rule(
                "TradeAffirmationRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "TradeB is affirmed"
        ));

        // Rule for trade confirmation
        settlementRules.put("TradeConfirmation", new Rule(
                "TradeConfirmationRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "TradeB is confirmed"
        ));

        // Rule for trade settlement
        settlementRules.put("TradeSettlement", new Rule(
                "TradeSettlementRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "TradeB is settled"
        ));

        // Rules for settlement method determination
        settlementRules.put(METHOD_DTC, new Rule(
                "DTCSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_EQUITY + "'",
                "TradeB should use DTC settlement method"
        ));

        settlementRules.put(METHOD_FEDWIRE, new Rule(
                "FedwireSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_FIXED_INCOME + "'",
                "TradeB should use Fedwire settlement method"
        ));

        settlementRules.put(METHOD_CLEARSTREAM, new Rule(
                "ClearstreamSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_DERIVATIVE + "'",
                "TradeB should use Clearstream settlement method"
        ));

        settlementRules.put(METHOD_EUROCLEAR, new Rule(
                "EuroclearSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_FOREX + "'",
                "TradeB should use Euroclear settlement method"
        ));

        settlementRules.put(METHOD_MANUAL, new Rule(
                "ManualSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_COMMODITY + "'",
                "TradeB should use Manual settlement method"
        ));

        // Rules for fee calculations
        settlementRules.put("SettlementFee", new Rule(
                "SettlementFeeRule",
                "#trade != null ? #settlementFees.getOrDefault(#trade.value, 2.0) : 0.0",
                "Settlement fee calculation"
        ));

        settlementRules.put("SettlementDays", new Rule(
                "SettlementDaysRule",
                "#trade != null ? #settlementDays.getOrDefault(#trade.value, 2) : 0",
                "Settlement days calculation"
        ));

        settlementRules.put("ClearingFee", new Rule(
                "ClearingFeeRule",
                "#trade != null ? #settlementFees.getOrDefault(#trade.value, 2.0) * 0.5 : 0.0",
                "Clearing fee calculation"
        ));

        settlementRules.put("CustodyFee", new Rule(
                "CustodyFeeRule",
                "#trade != null ? #settlementFees.getOrDefault(#trade.value, 2.0) * 0.25 : 0.0",
                "Custody fee calculation"
        ));
    }

    /**
     * Calculate settlement fee for a trade.
     *
     * @param trade The trade
     * @return The settlement fee
     */
    public double calculateSettlementFee(Trade trade) {
        if (trade == null) return 0.0;

        String type = trade.getValue();
        return settlementFees.getOrDefault(type, 2.0);
    }

    /**
     * Calculate settlement days for a trade.
     *
     * @param trade The trade
     * @return The number of days until settlement
     */
    public int calculateSettlementDays(Trade trade) {
        if (trade == null) return 0;

        String type = trade.getValue();
        return settlementDays.getOrDefault(type, 2);
    }

    /**
     * Calculate clearing fee for a trade.
     *
     * @param trade The trade
     * @return The clearing fee
     */
    public double calculateClearingFee(Trade trade) {
        if (trade == null) return 0.0;

        String type = trade.getValue();
        double baseFee = settlementFees.getOrDefault(type, 2.0);
        return baseFee * 0.5; // Clearing fee is typically half of settlement fee
    }

    /**
     * Calculate custody fee for a trade.
     *
     * @param trade The trade
     * @return The custody fee
     */
    public double calculateCustodyFee(Trade trade) {
        if (trade == null) return 0.0;

        String type = trade.getValue();
        double baseFee = settlementFees.getOrDefault(type, 2.0);
        return baseFee * 0.25; // Custody fee is typically quarter of settlement fee
    }

    /**
     * Validate a trade for settlement with detailed result.
     *
     * @param trade The trade to validate
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult validateTradeForSettlementWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        Rule rule = settlementRules.get("TradeValidation");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Match a trade with counterparty with detailed result.
     *
     * @param trade The trade to match
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult matchTradeWithCounterpartyWithResult(Trade trade) {
        // First check if the trade is valid for settlement
        RuleResult validationResult = validateTradeForSettlementWithResult(trade);
        if (!validationResult.isTriggered()) {
            return RuleResult.noMatch();
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        Rule rule = settlementRules.get("TradeMatching");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Affirm a trade with detailed result.
     *
     * @param trade The trade to affirm
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult affirmTradeWithResult(Trade trade) {
        // First check if the trade can be matched
        RuleResult matchResult = matchTradeWithCounterpartyWithResult(trade);
        if (!matchResult.isTriggered()) {
            return RuleResult.noMatch();
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        Rule rule = settlementRules.get("TradeAffirmation");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Confirm a trade with detailed result.
     *
     * @param trade The trade to confirm
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult confirmTradeWithResult(Trade trade) {
        // First check if the trade can be affirmed
        RuleResult affirmResult = affirmTradeWithResult(trade);
        if (!affirmResult.isTriggered()) {
            return RuleResult.noMatch();
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        Rule rule = settlementRules.get("TradeConfirmation");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Settle a trade with detailed result.
     *
     * @param trade The trade to settle
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult settleTradeWithResult(Trade trade) {
        // First check if the trade can be confirmed
        RuleResult confirmResult = confirmTradeWithResult(trade);
        if (!confirmResult.isTriggered()) {
            return RuleResult.noMatch();
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        Rule rule = settlementRules.get("TradeSettlement");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Determine settlement method for a trade with detailed result.
     *
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult determineSettlementMethodWithResult(Trade trade) {
        if (trade == null) {
            return RuleResult.noMatch();
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        // Try each settlement method rule in order
        String[] methods = {METHOD_DTC, METHOD_FEDWIRE, METHOD_CLEARSTREAM, METHOD_EUROCLEAR, METHOD_MANUAL};
        for (String method : methods) {
            Rule rule = settlementRules.get(method);
            if (rule != null) {
                RuleResult result = rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
                if (result.isTriggered()) {
                    return result;
                }
            }
        }

        // Default to manual if no rule matches
        return RuleResult.match("DefaultSettlementRule", "TradeB should use Manual settlement method");
    }

    /**
     * Get the appropriate settlement method for a trade.
     *
     * @param trade The trade
     * @return The settlement method
     */
    public String determineSettlementMethod(Trade trade) {
        RuleResult result = determineSettlementMethodWithResult(trade);

        // Extract the method from the rule message
        String message = result.getMessage();
        if (message.contains(METHOD_DTC)) {
            return METHOD_DTC;
        } else if (message.contains(METHOD_FEDWIRE)) {
            return METHOD_FEDWIRE;
        } else if (message.contains(METHOD_CLEARSTREAM)) {
            return METHOD_CLEARSTREAM;
        } else if (message.contains(METHOD_EUROCLEAR)) {
            return METHOD_EUROCLEAR;
        } else {
            return METHOD_MANUAL;
        }
    }

    /**
     * Get the rules engine used by this demo.
     *
     * @return The rules engine
     */
    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }

    /**
     * Get the settlement fees map.
     *
     * @return The settlement fees map
     */
    public Map<String, Double> getSettlementFees() {
        return new HashMap<>(settlementFees);
    }

    /**
     * Get the settlement days map.
     *
     * @return The settlement days map
     */
    public Map<String, Integer> getSettlementDays() {
        return new HashMap<>(settlementDays);
    }
}
