/**
 * An enricher for Trade objects.
 * This enricher adds additional information to trades based on their value and category.
 */
package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeEnricher extends AbstractEnricher<Trade> {
    private final Map<String, String> categoryDescriptions;
    private final Map<String, String> valueDescriptions;
    private final RulesEngine rulesEngine;
    private final List<Rule> enrichmentRules;

    /**
     * Create a new TradeEnricher with the specified name.
     *
     * @param name The name of the enricher
     * @param categoryDescriptions Map of category descriptions
     * @param valueDescriptions Map of value descriptions
     * @param rulesEngine The rules engine to use for enrichment
     */
    public TradeEnricher(String name, 
                         Map<String, String> categoryDescriptions, 
                         Map<String, String> valueDescriptions,
                         RulesEngine rulesEngine) {

        super(name, Trade.class);

        this.categoryDescriptions = categoryDescriptions != null ? new HashMap<>(categoryDescriptions) : new HashMap<>();
        this.valueDescriptions = valueDescriptions != null ? new HashMap<>(valueDescriptions) : new HashMap<>();
        this.rulesEngine = rulesEngine != null ? rulesEngine : new RulesEngine(new RulesEngineConfiguration());
        this.enrichmentRules = createEnrichmentRules();
    }

    /**
     * Create a new TradeEnricher with the specified name and default values.
     *
     * @param name The name of the enricher
     */
    public TradeEnricher(String name) {
        this(name, null, null, null);
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
     * Create rules for enrichment logic.
     */
    private List<Rule> createEnrichmentRules() {
        List<Rule> rules = new ArrayList<>();

        // Rule for value description
        rules.add(new Rule(
            "ValueDescriptionRule",
            "#valueDescriptions.containsKey(#trade.value)",
            "Trade value has a description"
        ));

        // Rule for category description
        rules.add(new Rule(
            "CategoryDescriptionRule",
            "#categoryDescriptions.containsKey(#trade.category)",
            "Trade category has a description"
        ));

        return rules;
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

        // Create a new trade with the same properties
        Trade enrichedTrade = new Trade(
            trade.getId(),
            trade.getValue(),
            trade.getCategory()
        );

        // Build a description for the trade using rules
        StringBuilder description = new StringBuilder();

        // Set up facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("valueDescriptions", valueDescriptions);
        facts.put("categoryDescriptions", categoryDescriptions);

        // Check if value has a description
        RuleResult valueResult = rulesEngine.executeRulesList(List.of(enrichmentRules.get(0)), facts);

        if (valueResult.isTriggered()) {
            description.append(valueDescriptions.get(trade.getValue()));
        } else {
            description.append(trade.getValue());
        }

        description.append(" (");

        // Check if category has a description
        RuleResult categoryResult = rulesEngine.executeRulesList(List.of(enrichmentRules.get(1)), facts);

        if (categoryResult.isTriggered()) {
            description.append(categoryDescriptions.get(trade.getCategory()));
        } else {
            description.append(trade.getCategory());
        }

        description.append(")");

        // Set the enriched value with the description
        enrichedTrade.setValue(description.toString());

        return enrichedTrade;
    }

    /**
     * Enrich a trade and return a RuleResult.
     * 
     * @param trade The trade to enrich
     * @return A RuleResult containing the enrichment outcome
     */
    public RuleResult enrichWithResult(Trade trade) {
        if (trade == null) {
            return RuleResult.error(getName(), "Trade is null");
        }

        Trade enrichedTrade = enrich(trade);

        if (enrichedTrade != null && !enrichedTrade.getValue().equals(trade.getValue())) {
            return RuleResult.match(getName(), "Trade enriched successfully");
        } else {
            return RuleResult.noMatch();
        }
    }
}
