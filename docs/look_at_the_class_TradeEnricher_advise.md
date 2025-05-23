# Refactoring TradeEnricher to Use RulesEngine for Business Logic

## Current Implementation Issues

The current `TradeEnricher` class has several issues that don't align with the project's requirements:

1. **Hard-coded business logic**: The enrichment logic is directly implemented in the `enrich` method rather than using the RulesEngine.
2. **No use of RuleResult**: The class doesn't return or use RuleResult objects.
3. **No separation of concerns**: The class combines data storage (maps of descriptions) with business logic.
4. **No dependency injection**: The class creates its own data structures rather than receiving them through dependency injection.

## Recommended Implementation

Here's how the `TradeEnricher` should be refactored to use the RulesEngine for business logic:

```java
/**
 * An enricher for Trade objects.
 * This enricher adds additional information to trades based on their value and category.
 */
package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
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
        this.categoryDescriptions = categoryDescriptions != null ?
                new HashMap<>(categoryDescriptions) : new HashMap<>();
        this.valueDescriptions = valueDescriptions != null ?
                new HashMap<>(valueDescriptions) : new HashMap<>();
        this.rulesEngine = rulesEngine != null ?
                rulesEngine : new RulesEngine(new RulesEngineConfiguration());
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
        RuleResult valueResult = rulesEngine.executeRulesList(
                List.of(enrichmentRules.get(0)), facts);

        if (valueResult.isTriggered()) {
            description.append(valueDescriptions.get(trade.getValue()));
        } else {
            description.append(trade.getValue());
        }

        description.append(" (");

        // Check if category has a description
        RuleResult categoryResult = rulesEngine.executeRulesList(
                List.of(enrichmentRules.get(1)), facts);

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
```

## Key Improvements

1. **RulesEngine Integration**:
   - Added RulesEngine as a dependency
   - Created rules for the enrichment logic
   - Used the RulesEngine to evaluate conditions

2. **Proper Dependency Injection**:
   - Added constructor parameters for dependencies
   - Provided default implementations when dependencies are not provided

3. **Separation of Concerns**:
   - Moved business logic to rules
   - Separated data storage from rule evaluation

4. **RuleResult Support**:
   - Added an `enrichWithResult` method that returns a RuleResult
   - Maintained backward compatibility with the original `enrich` method

5. **SOLID Principles**:
   - Single Responsibility: Each rule has a single responsibility
   - Open/Closed: New rules can be added without modifying existing code
   - Dependency Inversion: Dependencies are injected rather than created

## Integration with EnrichmentService

To use this refactored TradeEnricher with the EnrichmentService, you would:

1. Register the TradeEnricher with the LookupServiceRegistry
2. Use the EnrichmentService's `applyRuleCondition` method to apply rules to trades

Example:
```java
// Create and register the enricher
Map<String, String> categoryDescriptions = new HashMap<>();
Map<String, String> valueDescriptions = new HashMap<>();
// ... populate maps ...
TradeEnricher tradeEnricher = new TradeEnricher("tradeEnricher", 
    categoryDescriptions, valueDescriptions, rulesEngine);
registry.registerService(tradeEnricher);

// Use the enrichment service
Trade enrichedTrade = (Trade) enrichmentService.applyRuleCondition(
    "#trade.value == 'Equity'", 
    trade, 
    null, 
    "tradeEnricher"
);
```

This approach ensures that all business logic is implemented using the RulesEngine, following the project's requirements.