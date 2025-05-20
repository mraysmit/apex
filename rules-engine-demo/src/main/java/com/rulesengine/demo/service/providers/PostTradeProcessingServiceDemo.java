package com.rulesengine.demo.service.providers;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Trade;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Demonstration of how to use the PostTradeProcessingServiceDemoConfig class with RulesEngine.
 * This class shows the step-by-step process of creating and using a post-trade processing service
 * for trade settlement and related operations.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
public class PostTradeProcessingServiceDemo {
    private static final Logger LOGGER = Logger.getLogger(PostTradeProcessingServiceDemo.class.getName());

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private PostTradeProcessingServiceDemo() {
        // Private constructor to prevent instantiation
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

        // Step 2: Create a PostTradeProcessingServiceDemoConfig
        LOGGER.info("Step 2: Creating a PostTradeProcessingServiceDemoConfig");
        PostTradeProcessingServiceDemoConfig config = new PostTradeProcessingServiceDemoConfig(rulesEngine);

        // Step 3: Create test trades
        LOGGER.info("Step 3: Creating test trades");
        List<Trade> trades = createTestTrades();

        // Step 4: Process each trade
        LOGGER.info("Step 4: Processing trades");
        for (Trade trade : trades) {
            LOGGER.info("Processing trade: " + trade);

            // Step 4.1: Validate trade for settlement
            validateTradeForSettlement(trade, config);

            // Step 4.2: Match trade with counterparty
            matchTradeWithCounterparty(trade, config);

            // Step 4.3: Affirm trade
            affirmTrade(trade, config);

            // Step 4.4: Confirm trade
            confirmTrade(trade, config);

            // Step 4.5: Settle trade
            settleTrade(trade, config);

            // Step 4.6: Determine settlement method
            determineSettlementMethod(trade, config);

            // Step 4.7: Calculate fees
            calculateFees(trade, config);

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("Post-trade processing demonstration completed");
    }

    /**
     * Validate a trade for settlement.
     *
     * @param trade The trade to validate
     * @param config The post-trade processing config
     */
    private static void validateTradeForSettlement(Trade trade, PostTradeProcessingServiceDemoConfig config) {
        RuleResult result = config.validateTradeForSettlementWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade is valid for settlement");
        } else {
            LOGGER.info("Trade is not valid for settlement");
        }
    }

    /**
     * Match a trade with counterparty.
     *
     * @param trade The trade to match
     * @param config The post-trade processing config
     */
    private static void matchTradeWithCounterparty(Trade trade, PostTradeProcessingServiceDemoConfig config) {
        RuleResult result = config.matchTradeWithCounterpartyWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade is matched with counterparty");
        } else {
            LOGGER.info("Trade is not matched with counterparty");
        }
    }

    /**
     * Affirm a trade.
     *
     * @param trade The trade to affirm
     * @param config The post-trade processing config
     */
    private static void affirmTrade(Trade trade, PostTradeProcessingServiceDemoConfig config) {
        RuleResult result = config.affirmTradeWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade is affirmed");
        } else {
            LOGGER.info("Trade is not affirmed");
        }
    }

    /**
     * Confirm a trade.
     *
     * @param trade The trade to confirm
     * @param config The post-trade processing config
     */
    private static void confirmTrade(Trade trade, PostTradeProcessingServiceDemoConfig config) {
        RuleResult result = config.confirmTradeWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade is confirmed");
        } else {
            LOGGER.info("Trade is not confirmed");
        }
    }

    /**
     * Settle a trade.
     *
     * @param trade The trade to settle
     * @param config The post-trade processing config
     */
    private static void settleTrade(Trade trade, PostTradeProcessingServiceDemoConfig config) {
        RuleResult result = config.settleTradeWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade is settled");
        } else {
            LOGGER.info("Trade is not settled");
        }
    }

    /**
     * Determine settlement method for a trade.
     *
     * @param trade The trade
     * @param config The post-trade processing config
     */
    private static void determineSettlementMethod(Trade trade, PostTradeProcessingServiceDemoConfig config) {
        String method = config.determineSettlementMethod(trade);
        LOGGER.info("Settlement method: " + method);
    }

    /**
     * Calculate fees for a trade.
     *
     * @param trade The trade
     * @param config The post-trade processing config
     */
    private static void calculateFees(Trade trade, PostTradeProcessingServiceDemoConfig config) {
        double settlementFee = config.calculateSettlementFee(trade);
        double clearingFee = config.calculateClearingFee(trade);
        double custodyFee = config.calculateCustodyFee(trade);
        int settlementDays = config.calculateSettlementDays(trade);

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
                new Trade("T1001", PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, "Stock"),
                new Trade("T1002", PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME, "Bond"),
                new Trade("T1003", PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE, "Option"),
                new Trade("T1004", PostTradeProcessingServiceDemoConfig.TYPE_FOREX, "Spot"),
                new Trade("T1005", PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY, "Future"),
                new Trade("", PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, "Stock") // Invalid trade
        );
    }
}