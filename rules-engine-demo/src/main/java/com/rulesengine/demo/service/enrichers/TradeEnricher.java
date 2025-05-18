/**
 * An enricher for Trade objects.
 * This enricher adds additional information to trades based on their value and category.
 */
package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.HashMap;
import java.util.Map;

public class TradeEnricher extends AbstractEnricher<Trade> {
    private final Map<String, String> categoryDescriptions;
    private final Map<String, String> valueDescriptions;

    /**
     * Create a new TradeEnricher with the specified name.
     *
     * @param name The name of the enricher
     */
    public TradeEnricher(String name) {
        super(name, Trade.class);
        this.categoryDescriptions = new HashMap<>();
        this.valueDescriptions = new HashMap<>();

        // Initialize with default values
        initializeDefaultValues();
    }

    /**
     * Initialize the enricher with default values.
     */
    private void initializeDefaultValues() {
        // Set up category descriptions
        categoryDescriptions.put("InstrumentType", "Type of financial instrument");
        categoryDescriptions.put("AssetClass", "Class of investment asset");
        categoryDescriptions.put("Market", "Market where the trade is executed");
        categoryDescriptions.put("TradeStatus", "Status of the trade");

        // Set up value descriptions
        valueDescriptions.put("Equity", "Ownership in a company");
        valueDescriptions.put("Bond", "Debt security with fixed interest payments");
        valueDescriptions.put("ETF", "Exchange-traded fund tracking an index");
        valueDescriptions.put("Option", "Contract giving the right to buy/sell at a specific price");
        valueDescriptions.put("Future", "Contract to buy/sell at a future date");
        valueDescriptions.put("FixedIncome", "Investment with fixed periodic payments");
        valueDescriptions.put("Commodity", "Basic good used in commerce");
        valueDescriptions.put("Currency", "Money in circulation");
    }

    /**
     * Add a category description.
     *
     * @param category The category
     * @param description The description
     */
    public void addCategoryDescription(String category, String description) {
        categoryDescriptions.put(category, description);
    }

    /**
     * Add a value description.
     *
     * @param value The value
     * @param description The description
     */
    public void addValueDescription(String value, String description) {
        valueDescriptions.put(value, description);
    }

    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }

        String tradeValue = trade.getValue();
        String category = trade.getCategory();

        // Create a new trade with the same properties
        Trade enrichedTrade = new Trade(
            trade.getId(),
            trade.getValue(),
            trade.getCategory()
        );

        // Build a description for the trade
        StringBuilder description = new StringBuilder();

        // Add value description if available
        if (valueDescriptions.containsKey(tradeValue)) {
            description.append(valueDescriptions.get(tradeValue));
        } else {
            description.append(tradeValue);
        }

        description.append(" (");

        // Add category description if available
        if (categoryDescriptions.containsKey(category)) {
            description.append(categoryDescriptions.get(category));
        } else {
            description.append(category);
        }

        description.append(")");

        // Set the enriched value with the description
        enrichedTrade.setValue(description.toString());

        return enrichedTrade;
    }
}
