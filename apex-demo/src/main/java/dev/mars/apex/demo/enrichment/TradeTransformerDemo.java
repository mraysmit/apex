
// TradeTransformerDemo.java
package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.TransformerRule;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.transform.FieldTransformerAction;
import dev.mars.apex.core.service.transform.FieldTransformerActionBuilder;
import dev.mars.apex.core.service.transform.GenericTransformer;
import dev.mars.apex.core.service.transform.GenericTransformerService;
import dev.mars.apex.demo.model.Trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
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
 * Comprehensive demonstration of trade transformation functionality with GenericTransformer.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of trade transformation functionality with GenericTransformer.
 * This class shows the step-by-step process of creating and using a GenericTransformer
 * for Trade objects. It combines both the demonstration logic and the
 * configuration functionality in a single self-contained class.
 *
 * This is a demo class with a main method for running the demonstration and instance
 * methods for trade transformation operations.
 */
public class TradeTransformerDemo {
    private static final Logger LOGGER = Logger.getLogger(TradeTransformerDemo.class.getName());

    // Instance fields for trade transformation configuration
    private final RulesEngine rulesEngine;
    private final Map<String, Double> tradeRiskRatings = new HashMap<>();

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private TradeTransformerDemo() {
        // Private constructor to prevent instantiation
        this.rulesEngine = null;
    }

    /**
     * Create a new TradeTransformerDemo with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for transformation
     */
    public TradeTransformerDemo(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
        initializeTradeRiskRatings();
    }

    /**
     * Initialize trade risk ratings.
     */
    private void initializeTradeRiskRatings() {
        // Initialize trade risk ratings
        tradeRiskRatings.put("Equity", 0.8);        // 80% risk for Equity trades
        tradeRiskRatings.put("Bond", 0.4);          // 40% risk for Bond trades
        tradeRiskRatings.put("ETF", 0.6);           // 60% risk for ETF trades
        tradeRiskRatings.put("Commodity", 0.9);     // 90% risk for Commodity trades
        tradeRiskRatings.put("HighPriority", 0.95); // 95% risk for High Priority trades
    }

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

        // Step 3: Create a TradeTransformerDemo instance
        LOGGER.info("Step 3: Creating a TradeTransformerDemo instance");
        TradeTransformerDemo demo = new TradeTransformerDemo(rulesEngine);

        // Step 4: Create a GenericTransformerService
        LOGGER.info("Step 4: Creating a GenericTransformerService");
        GenericTransformerService transformerService = new GenericTransformerService(registry, rulesEngine);

        // Step 5: Create transformer rules
        LOGGER.info("Step 5: Creating transformer rules");
        List<TransformerRule<Trade>> rules = new ArrayList<>();
        rules.add(demo.createEquityTradeRule());
        rules.add(demo.createBondTradeRule());
        rules.add(demo.createETFTradeRule());
        rules.add(demo.createHighPriorityTradeRule());

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
            double riskRating = demo.getRiskRatingForTrade(transformedTrade);
            LOGGER.info("Risk rating for " + transformedTrade.getId() + ": " + (riskRating * 100) + "%");

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("TradeB transformation demonstration completed");
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

    // ========== Configuration Methods (from TradeTransformerDemoConfig) ==========

    /**
     * Create a GenericTransformer for Trade objects using the GenericTransformerService.
     *
     * @param name The name of the transformer
     * @param transformerService The GenericTransformerService to use
     * @return A GenericTransformer for Trade objects
     */
    public GenericTransformer<Trade> createTradeTransformer(String name, GenericTransformerService transformerService) {
        List<TransformerRule<Trade>> rules = new ArrayList<>();

        // Add rules for value-based category transformations
        rules.add(createEquityTradeRule());
        rules.add(createBondTradeRule());
        rules.add(createETFTradeRule());

        // Add rules for ID-based transformations
        rules.add(createHighPriorityTradeRule());

        // Create and register the transformer
        return transformerService.createTransformer(name, Trade.class, rules);
    }

    /**
     * Create a rule for Equity trades.
     * Sets category to "InstrumentType" if not already set.
     *
     * @return The transformer rule
     */
    public TransformerRule<Trade> createEquityTradeRule() {
        // Create a rule that matches Equity trades
        Rule rule = new Rule(
                "EquityTradeRule",
                "#value.value == 'Equity'",
                "Set category for Equity trades"
        );

        // Create field transformer actions for setting category
        List<FieldTransformerAction<Trade>> positiveActions = new ArrayList<>();
        positiveActions.add(createSetCategoryAction("InstrumentType"));

        // No negative actions
        List<FieldTransformerAction<Trade>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a rule for Bond trades.
     * Sets category to "InstrumentType" if not already set.
     *
     * @return The transformer rule
     */
    public TransformerRule<Trade> createBondTradeRule() {
        // Create a rule that matches Bond trades
        Rule rule = new Rule(
                "BondTradeRule",
                "#value.value == 'Bond'",
                "Set category for Bond trades"
        );

        // Create field transformer actions for setting category
        List<FieldTransformerAction<Trade>> positiveActions = new ArrayList<>();
        positiveActions.add(createSetCategoryAction("InstrumentType"));

        // No negative actions
        List<FieldTransformerAction<Trade>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a rule for ETF trades.
     * Sets category to "InstrumentType" if not already set.
     *
     * @return The transformer rule
     */
    public TransformerRule<Trade> createETFTradeRule() {
        // Create a rule that matches ETF trades
        Rule rule = new Rule(
                "ETFTradeRule",
                "#value.value == 'ETF'",
                "Set category for ETF trades"
        );

        // Create field transformer actions for setting category
        List<FieldTransformerAction<Trade>> positiveActions = new ArrayList<>();
        positiveActions.add(createSetCategoryAction("InstrumentType"));

        // No negative actions
        List<FieldTransformerAction<Trade>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a rule for high priority trades (IDs starting with 'HP').
     * Sets value to "HighPriority" if ID starts with "HP".
     *
     * @return The transformer rule
     */
    public TransformerRule<Trade> createHighPriorityTradeRule() {
        // Create a rule that matches high priority trades
        Rule rule = new Rule(
                "HighPriorityTradeRule",
                "#value.id.startsWith('HP')",
                "Set value for high priority trades"
        );

        // Create field transformer actions for setting value
        List<FieldTransformerAction<Trade>> positiveActions = new ArrayList<>();
        positiveActions.add(createSetValueAction("HighPriority"));

        // No negative actions
        List<FieldTransformerAction<Trade>> negativeActions = new ArrayList<>();

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions);
    }

    /**
     * Create a field transformer action for setting the category of a trade.
     *
     * @param category The category to set
     * @return The field transformer action
     */
    private FieldTransformerAction<Trade> createSetCategoryAction(String category) {
        LOGGER.info("Creating field transformer action for category: " + category);

        // Create a function to extract the category
        Function<Trade, Object> extractor = trade -> {
            String currentCategory = trade.getCategory();
            LOGGER.info("Extracted category from trade: " + currentCategory);
            return currentCategory;
        };

        // Create a function to transform the category
        BiFunction<Object, Map<String, Object>, Object> transformer = (currentCategory, facts) -> {
            String newCategory = (String) currentCategory;
            LOGGER.info("Transforming category: " + newCategory);

            // Set the category if it's not already set or is "Uncategorized"
            if (newCategory == null || newCategory.isEmpty() || "Uncategorized".equals(newCategory)) {
                newCategory = category;
                LOGGER.info("Set category to " + category);
            } else {
                LOGGER.info("Category already set to " + newCategory + ", not changing");
            }

            return newCategory;
        };

        // Create a function to set the category
        BiConsumer<Trade, Object> setter = (trade, newCategory) -> {
            LOGGER.info("Setting category on trade: " + newCategory);
            trade.setCategory((String) newCategory);
        };

        // Create and return the field transformer action
        return new FieldTransformerActionBuilder<Trade>()
                .withFieldName("category")
                .withFieldValueExtractor(extractor)
                .withFieldValueTransformer(transformer)
                .withFieldValueSetter(setter)
                .build();
    }

    /**
     * Create a field transformer action for setting the value of a trade.
     *
     * @param value The value to set
     * @return The field transformer action
     */
    private FieldTransformerAction<Trade> createSetValueAction(String value) {
        LOGGER.info("Creating field transformer action for value: " + value);

        // Create a function to extract the value
        Function<Trade, Object> extractor = trade -> {
            String currentValue = trade.getValue();
            LOGGER.info("Extracted value from trade: " + currentValue);
            return currentValue;
        };

        // Create a function to transform the value
        BiFunction<Object, Map<String, Object>, Object> transformer = (currentValue, facts) -> {
            LOGGER.info("Transforming value: " + currentValue);
            return value;
        };

        // Create a function to set the value
        BiConsumer<Trade, Object> setter = (trade, newValue) -> {
            LOGGER.info("Setting value on trade: " + newValue);
            trade.setValue((String) newValue);
        };

        // Create and return the field transformer action
        return new FieldTransformerActionBuilder<Trade>()
                .withFieldName("value")
                .withFieldValueExtractor(extractor)
                .withFieldValueTransformer(transformer)
                .withFieldValueSetter(setter)
                .build();
    }

    /**
     * Get the risk rating for a trade based on its value.
     *
     * @param trade The trade
     * @return The risk rating as a decimal (e.g., 0.8 for 80%)
     */
    public double getRiskRatingForTrade(Trade trade) {
        if (trade == null || trade.getValue() == null) {
            return 0.0;
        }

        return tradeRiskRatings.getOrDefault(trade.getValue(), 0.5); // Default to 50% risk
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
     * Get the trade risk ratings map.
     *
     * @return The trade risk ratings map
     */
    public Map<String, Double> getTradeRiskRatings() {
        return new HashMap<>(tradeRiskRatings);
    }
}
