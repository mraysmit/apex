package dev.mars.rulesengine.demo.service.providers;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.demo.model.Trade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration class for post-trade processing rules and requirements.
 * This class creates and manages rules for post-trade processing operations.
 */
class PostTradeProcessingServiceDemoConfig {
    private static final Logger LOGGER = Logger.getLogger(PostTradeProcessingServiceDemoConfig.class.getName());

    // Settlement status constants
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_MATCHED = "Matched";
    public static final String STATUS_AFFIRMED = "Affirmed";
    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_SETTLED = "Settled";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_CANCELLED = "Cancelled";

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

    private final Map<String, Double> settlementFees = new HashMap<>();
    private final Map<String, Integer> settlementDays = new HashMap<>();
    private final RulesEngine rulesEngine;
    private final Map<String, Rule> settlementRules = new HashMap<>();

    /**
     * Create a new PostTradeProcessingServiceDemoConfig with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for post-trade processing
     */
    public PostTradeProcessingServiceDemoConfig(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
        initializeDefaultValues();
        initializeRules();
    }

    /**
     * Initialize default values for settlement fees and days.
     */
    private void initializeDefaultValues() {
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
    }

    /**
     * Initialize rules for settlement logic.
     */
    private void initializeRules() {
        // Rule for trade validation
        settlementRules.put("TradeValidation", new Rule(
                "TradeValidationRule",
                "#trade != null && #trade.id != null && !#trade.id.isEmpty()",
                "Trade is valid for settlement"
        ));

        // Rule for trade matching
        settlementRules.put("TradeMatching", new Rule(
                "TradeMatchingRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "Trade is matched with counterparty"
        ));

        // Rule for trade affirmation
        settlementRules.put("TradeAffirmation", new Rule(
                "TradeAffirmationRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "Trade is affirmed"
        ));

        // Rule for trade confirmation
        settlementRules.put("TradeConfirmation", new Rule(
                "TradeConfirmationRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "Trade is confirmed"
        ));

        // Rule for trade settlement
        settlementRules.put("TradeSettlement", new Rule(
                "TradeSettlementRule",
                "#trade != null && !#trade.id.equals('Unknown')",
                "Trade is settled"
        ));

        // Rules for settlement method determination
        settlementRules.put(METHOD_DTC, new Rule(
                "DTCSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_EQUITY + "'",
                "Trade should use DTC settlement method"
        ));

        settlementRules.put(METHOD_FEDWIRE, new Rule(
                "FedwireSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_FIXED_INCOME + "'",
                "Trade should use Fedwire settlement method"
        ));

        settlementRules.put(METHOD_CLEARSTREAM, new Rule(
                "ClearstreamSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_DERIVATIVE + "'",
                "Trade should use Clearstream settlement method"
        ));

        settlementRules.put(METHOD_EUROCLEAR, new Rule(
                "EuroclearSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_FOREX + "'",
                "Trade should use Euroclear settlement method"
        ));

        settlementRules.put(METHOD_MANUAL, new Rule(
                "ManualSettlementRule",
                "#trade != null && #trade.value == '" + TYPE_COMMODITY + "'",
                "Trade should use Manual settlement method"
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
     * Get the appropriate settlement method for a trade with detailed result.
     *
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult determineSettlementMethodWithResult(Trade trade) {
        if (trade == null) {
            return RuleResult.match("SettlementMethodRule", "Trade should use Manual settlement method");
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
        return RuleResult.match("DefaultSettlementRule", "Trade should use Manual settlement method");
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
     * Get the rules engine used by this config.
     *
     * @return The rules engine
     */
    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }
}