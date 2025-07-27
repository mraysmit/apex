
// TradeTransformerDemo.java
package dev.mars.rulesengine.demo.service.transformers;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.TransformerRule;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import dev.mars.rulesengine.core.service.transform.GenericTransformer;
import dev.mars.rulesengine.core.service.transform.GenericTransformerService;
import dev.mars.rulesengine.demo.model.Trade;

import java.util.ArrayList;
import java.util.List;
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
 * Demonstration of how to use the TradeTransformerDemoConfig class with GenericTransformer.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Demonstration of how to use the TradeTransformerDemoConfig class with GenericTransformer.
 * This class shows the step-by-step process of creating and using a GenericTransformer
 * for Trade objects using the TradeTransformerDemoConfig to define transformation rules.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
public class TradeTransformerDemo {
    private static final Logger LOGGER = Logger.getLogger(TradeTransformerDemo.class.getName());

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private TradeTransformerDemo() { }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Run the demonstration
        runTradeTransformationDemo();
    }

    /**
     * Run the trade transformation demonstration.
     * This method shows the step-by-step process of creating and using a GenericTransformer.
     */
    private static void runTradeTransformationDemo() {
        LOGGER.info("Starting trade transformation demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a LookupServiceRegistry
        LOGGER.info("Step 2: Creating a LookupServiceRegistry");
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Step 3: Create a TradeTransformerDemoConfig
        LOGGER.info("Step 3: Creating a TradeTransformerDemoConfig");
        TradeTransformerDemoConfig config = new TradeTransformerDemoConfig(rulesEngine);

        // Step 4: Create a GenericTransformerService
        LOGGER.info("Step 4: Creating a GenericTransformerService");
        GenericTransformerService transformerService = new GenericTransformerService(registry, rulesEngine);

        // Step 5: Create transformer rules
        LOGGER.info("Step 5: Creating transformer rules");
        List<TransformerRule<Trade>> rules = new ArrayList<>();
        rules.add(config.createEquityTradeRule());
        rules.add(config.createBondTradeRule());
        rules.add(config.createETFTradeRule());
        rules.add(config.createHighPriorityTradeRule());

        // Step 6: Create a GenericTransformer
        LOGGER.info("Step 6: Creating a GenericTransformer");
        GenericTransformer<Trade> transformer = transformerService.createTransformer("TradeTransformer", Trade.class, rules);

        // Step 7: Create test trades
        LOGGER.info("Step 7: Creating test trades");
        List<Trade> trades = createTestTrades();

        // Step 8: Transform each trade and display the results
        LOGGER.info("Step 8: Transforming trades and displaying results");
        for (Trade trade : trades) {
            LOGGER.info("Original trade: " + trade);
            LOGGER.info("Original ID: " + trade.getId());
            LOGGER.info("Original value: " + trade.getValue());
            LOGGER.info("Original category: " + trade.getCategory());

            // Transform the trade
            Trade transformedTrade = transformer.transform(trade);

            // Display the transformed trade
            LOGGER.info("Transformed trade: " + transformedTrade);
            LOGGER.info("Transformed ID: " + transformedTrade.getId());
            LOGGER.info("Transformed value: " + transformedTrade.getValue());
            LOGGER.info("Transformed category: " + transformedTrade.getCategory());

            // Get transformation result
            RuleResult result = transformer.transformWithResult(transformedTrade);
            LOGGER.info("Transformation result: " + result);

            // Get the risk rating for the transformed trade
            double riskRating = config.getRiskRatingForTrade(transformedTrade);
            LOGGER.info("Risk rating for " + transformedTrade.getId() + ": " + (riskRating * 100) + "%");

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("Trade transformation demonstration completed");
    }

    /**
     * Create a list of test trades.
     *
     * @return A list of test trades
     */
    private static List<Trade> createTestTrades() {
        List<Trade> trades = new ArrayList<>();

        // Create trades with different values and categories
        trades.add(new Trade("T001", "Equity", ""));
        trades.add(new Trade("T002", "Bond", "Uncategorized"));
        trades.add(new Trade("T003", "ETF", "AssetClass"));
        trades.add(new Trade("HP001", "Equity", "")); // High priority trade

        return trades;
    }
}