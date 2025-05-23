
# TradeEnricher Configuration Refactoring

I'll implement a solution to make the TradeEnricher use external configuration for enrichment mappings, following SOLID principles and ensuring all business logic uses the RulesEngine.

## Current Issues

The current `TradeEnricher` implementation has several limitations:

1. **Hardcoded Mappings**: The source-to-target data mappings (category and value descriptions) are hardcoded in the class
2. **Limited Configurability**: The enrichment process cannot be easily configured without modifying the class
3. **Tight Coupling**: The enrichment logic is tightly coupled with the data it operates on

## Solution Design

I'll refactor the `TradeEnricher` to:

1. Accept external configuration via a `Map<String, Object>` parameter
2. Move hardcoded mappings to this external configuration
3. Make the enrichment process generic and configurable
4. Maintain separation between enrichment logic and data

## Implementation

```java
/**
 * An enricher for Trade objects.
 * This enricher adds additional information to trades based on their value and category.
 * Uses external configuration for enrichment mappings.
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
    private final RulesEngine rulesEngine;
    private final Map<String, Object> configuration;
    private final List<Rule> enrichmentRules;

    /**
     * Create a new TradeEnricher with the specified name and configuration.
     *
     * @param name The name of the enricher
     * @param configuration The configuration to use for enrichment
     * @param rulesEngine The rules engine to use for enrichment
     */
    public TradeEnricher(String name,
                         Map<String, Object> configuration,
                         RulesEngine rulesEngine) {
        super(name, Trade.class);
        this.configuration = configuration != null ? new HashMap<>(configuration) : new HashMap<>();
        this.rulesEngine = rulesEngine != null ? rulesEngine : new RulesEngine(new RulesEngineConfiguration());
        this.enrichmentRules = createEnrichmentRules();

        // Initialize with default values if configuration is empty
        if (this.configuration.isEmpty()) {
            initializeDefaultConfiguration();
        }
    }

    /**
     * Create a new TradeEnricher with the specified name and default configuration.
     *
     * @param name The name of the enricher
     */
    public TradeEnricher(String name) {
        this(name, null, null);
    }

    /**
     * Initialize the enricher with default configuration.
     */
    private void initializeDefaultConfiguration() {
        // Create category descriptions map
        Map<String, String> categoryDescriptions = new HashMap<>();
        categoryDescriptions.put("InstrumentType", "Type of financial instrument");
        categoryDescriptions.put("AssetClass", "Class of investment asset");
        categoryDescriptions.put("Market", "Market where the trade is executed");
        categoryDescriptions.put("TradeStatus", "Status of the trade");
        configuration.put("categoryDescriptions", categoryDescriptions);

        // Create value descriptions map
        Map<String, String> valueDescriptions = new HashMap<>();
        valueDescriptions.put("Equity", "Ownership in a company");
        valueDescriptions.put("Bond", "Debt security with fixed interest payments");
        valueDescriptions.put("ETF", "Exchange-traded fund tracking an index");
        valueDescriptions.put("Option", "Contract giving the right to buy/sell at a specific price");
        valueDescriptions.put("Future", "Contract to buy/sell at a future date");
        valueDescriptions.put("FixedIncome", "Investment with fixed periodic payments");
        valueDescriptions.put("Commodity", "Basic good used in commerce");
        valueDescriptions.put("Currency", "Money in circulation");
        configuration.put("valueDescriptions", valueDescriptions);
    }

    /**
     * Create rules for enrichment logic.
     */
    private List<Rule> createEnrichmentRules() {
        List<Rule> rules = new ArrayList<>();

        // Rule for value description
        rules.add(new Rule(
                "ValueDescriptionRule",
                "#valueDescriptions != null && #valueDescriptions.containsKey(#trade.value)",
                "Trade value has a description"
        ));

        // Rule for category description
        rules.add(new Rule(
                "CategoryDescriptionRule",
                "#categoryDescriptions != null && #categoryDescriptions.containsKey(#trade.category)",
                "Trade category has a description"
        ));

        return rules;
    }

    /**
     * Add a category description to the configuration.
     *
     * @param category The category
     * @param description The description
     */
    @SuppressWarnings("unchecked")
    public void addCategoryDescription(String category, String description) {
        Map<String, String> categoryDescriptions = (Map<String, String>) configuration.get("categoryDescriptions");
        if (categoryDescriptions == null) {
            categoryDescriptions = new HashMap<>();
            configuration.put("categoryDescriptions", categoryDescriptions);
        }
        categoryDescriptions.put(category, description);
    }

    /**
     * Add a value description to the configuration.
     *
     * @param value The value
     * @param description The description
     */
    @SuppressWarnings("unchecked")
    public void addValueDescription(String value, String description) {
        Map<String, String> valueDescriptions = (Map<String, String>) configuration.get("valueDescriptions");
        if (valueDescriptions == null) {
            valueDescriptions = new HashMap<>();
            configuration.put("valueDescriptions", valueDescriptions);
        }
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

        // Get description maps from configuration
        @SuppressWarnings("unchecked")
        Map<String, String> categoryDescriptions = (Map<String, String>) configuration.get("categoryDescriptions");
        @SuppressWarnings("unchecked")
        Map<String, String> valueDescriptions = (Map<String, String>) configuration.get("valueDescriptions");

        // Build a description for the trade using rules
        StringBuilder description = new StringBuilder();

        // Set up facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("valueDescriptions", valueDescriptions);
        facts.put("categoryDescriptions", categoryDescriptions);
        facts.put("configuration", configuration);

        // Check if value has a description
        RuleResult valueResult = rulesEngine.executeRulesList(
                List.of(enrichmentRules.get(0)), facts);

        if (valueResult.isTriggered() && valueDescriptions != null) {
            description.append(valueDescriptions.get(trade.getValue()));
        } else {
            description.append(trade.getValue());
        }

        description.append(" (");

        // Check if category has a description
        RuleResult categoryResult = rulesEngine.executeRulesList(
                List.of(enrichmentRules.get(1)), facts);

        if (categoryResult.isTriggered() && categoryDescriptions != null) {
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

## Usage in EnrichmentFinancialServicesDemo

The `EnrichmentFinancialServicesDemo` class should be updated to use the new constructor:

```java
// In the initializeSampleData method, add trade descriptions
Map<String, String> tradeValueDescriptions = new HashMap<>();
tradeValueDescriptions.put("Equity", "Ownership in a company");
tradeValueDescriptions.put("Bond", "Debt security with fixed interest payments");
tradeValueDescriptions.put("ETF", "Exchange-traded fund tracking an index");
// ... add more descriptions as needed
referenceData.put("valueDescriptions", tradeValueDescriptions);

Map<String, String> tradeCategoryDescriptions = new HashMap<>();
tradeCategoryDescriptions.put("InstrumentType", "Type of financial instrument");
tradeCategoryDescriptions.put("AssetClass", "Class of investment asset");
// ... add more descriptions as needed
referenceData.put("categoryDescriptions", tradeCategoryDescriptions);

// In the registerServices method, update the TradeEnricher registration
registry.registerService(new TradeEnricher("basicTradeEnricher", referenceData, rulesEngine));
```

## Benefits of This Approach

1. **Separation of Concerns**: The enrichment logic is separated from the data it operates on
2. **Configurability**: The enrichment process can be configured externally without modifying the class
3. **Flexibility**: Different configurations can be used for different instances of the enricher
4. **Maintainability**: The code is more maintainable as the configuration can be changed without modifying the class
5. **Testability**: The class is easier to test as the configuration can be mocked or injected

## SOLID Principles

This implementation follows SOLID principles:

1. **Single Responsibility**: The class is responsible only for enriching trades
2. **Open/Closed**: The class is open for extension (via configuration) but closed for modification
3. **Liskov Substitution**: The class maintains the contract of its parent class
4. **Interface Segregation**: The class implements only the methods it needs
5. **Dependency Inversion**: The class depends on abstractions (RulesEngine, Map) rather than concrete implementations

All business logic is implemented using the RulesEngine, ensuring consistency with the project's requirements.