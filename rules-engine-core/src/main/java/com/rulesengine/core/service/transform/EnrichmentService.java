package com.rulesengine.core.service.transform;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for enrichment operations.
 * Uses the rules engine internally to perform enrichment based on rule evaluation.
 */
public class EnrichmentService {
    private static final Logger LOGGER = Logger.getLogger(EnrichmentService.class.getName());
    private final LookupServiceRegistry registry;
    private final RulesEngine rulesEngine;

    /**
     * Create a new EnrichmentService with the specified registry.
     * This constructor creates a new RulesEngine with a default configuration.
     * 
     * @param registry The lookup service registry
     */
    public EnrichmentService(LookupServiceRegistry registry) {
        this.registry = registry;
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        LOGGER.info("EnrichmentService initialized with default RulesEngine");
    }

    /**
     * Create a new EnrichmentService with the specified registry and rules engine.
     * 
     * @param registry The lookup service registry
     * @param rulesEngine The rules engine to use for enrichment
     */
    public EnrichmentService(LookupServiceRegistry registry, RulesEngine rulesEngine) {
        this.registry = registry;
        this.rulesEngine = rulesEngine;
        LOGGER.info("EnrichmentService initialized with custom RulesEngine");
    }

    /**
     * Enrich a value using the specified enricher with type safety.
     * 
     * @param <T> The type of the value to enrich
     * @param enricherName The name of the enricher to use
     * @param value The value to enrich
     * @return The enriched value, or the original value if the enricher is not found
     */
    @SuppressWarnings("unchecked")
    public <T> T enrich(String enricherName, T value) {
        LOGGER.fine("Enriching value using enricher: " + enricherName);

        // Get the enricher from the registry
        Enricher<?> enricher = registry.getService(enricherName, Enricher.class);
        if (enricher == null) {
            LOGGER.warning("Enricher not found: " + enricherName);
            return value;
        }

        // Check if the enricher can handle this type
        if (value != null && !enricher.getType().isInstance(value)) {
            LOGGER.warning("Enricher " + enricherName + " cannot handle type: " + value.getClass().getName());
            return value;
        }

        // Call the enricher with the appropriate type
        Enricher<T> typedEnricher = (Enricher<T>) enricher;
        return typedEnricher.enrich(value);
    }

    /**
     * Apply a rule to a core data record using lookup data.
     * If the rule evaluates to true, the core data is enriched using the specified enricher.
     * 
     * @param <T> The type of the core data
     * @param rule The rule to apply
     * @param coreData The core data to operate on
     * @param lookupData The lookup data to use for enrichment
     * @param enricherName The name of the enricher to use
     * @return The enriched data if the rule evaluates to true, otherwise the original data
     */
    @SuppressWarnings("unchecked")
    public <T> T applyRule(Rule rule, T coreData, Object lookupData, String enricherName) {
        LOGGER.fine("Applying rule to core data: " + rule.getName());

        // Get the enricher from the registry
        Enricher<?> enricher = registry.getService(enricherName, Enricher.class);
        if (enricher == null) {
            LOGGER.warning("Enricher not found: " + enricherName);
            return coreData;
        }

        // Check if the enricher can handle this type
        if (coreData != null && !enricher.getType().isInstance(coreData)) {
            LOGGER.warning("Enricher " + enricherName + " cannot handle type: " + coreData.getClass().getName());
            return coreData;
        }

        // Create facts for the rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("coreData", coreData);
        facts.put("lookupData", lookupData);

        // Create a list of rules
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Execute the rule
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // If the rule was triggered, enrich the core data
        if (result.isTriggered()) {
            LOGGER.fine("Rule triggered, enriching core data");
            Enricher<T> typedEnricher = (Enricher<T>) enricher;
            return typedEnricher.enrich(coreData);
        } else {
            LOGGER.fine("Rule not triggered, returning original core data");
            return coreData;
        }
    }

    /**
     * Apply a rule condition to a core data record using lookup data.
     * If the condition evaluates to true, the core data is enriched using the specified enricher.
     * 
     * @param <T> The type of the core data
     * @param ruleCondition The rule condition to apply
     * @param coreData The core data to operate on
     * @param lookupData The lookup data to use for enrichment
     * @param enricherName The name of the enricher to use
     * @return The enriched data if the rule condition evaluates to true, otherwise the original data
     */
    public <T> T applyRuleCondition(String ruleCondition, T coreData, Object lookupData, String enricherName) {
        LOGGER.fine("Applying rule condition to core data: " + ruleCondition);

        // Create a rule from the condition
        Rule rule = new Rule(
            "Enrichment Rule",
            ruleCondition,
            "Enrichment rule with condition: " + ruleCondition
        );

        // Apply the rule
        return applyRule(rule, coreData, lookupData, enricherName);
    }
}
