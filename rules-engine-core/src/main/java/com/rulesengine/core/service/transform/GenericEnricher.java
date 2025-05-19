package com.rulesengine.core.service.transform;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.EnrichmentRule;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic enricher that can enrich any type of object based on rules and field mappings.
 * This enricher contains no business logic but receives as parameters a set of rules and
 * a set of fields and values for both positive and negative rule results.
 *
 * @param <T> The type of object this enricher can enrich
 */
public class GenericEnricher<T> extends AbstractEnricher<T> {
    private static final Logger LOGGER = Logger.getLogger(GenericEnricher.class.getName());

    private final RulesEngine rulesEngine;
    private final List<EnrichmentRule<T>> enrichmentRules;

    /**
     * Create a new GenericEnricher with the specified parameters.
     *
     * @param name The name of the enricher
     * @param type The class of objects this enricher can enrich
     * @param rulesEngine The rules engine to use for enrichment
     * @param enrichmentRules The enrichment rules to apply
     */
    public GenericEnricher(String name, Class<T> type, RulesEngine rulesEngine, List<EnrichmentRule<T>> enrichmentRules) {
        super(name, type);
        this.rulesEngine = rulesEngine != null ? rulesEngine : new RulesEngine(new RulesEngineConfiguration());
        this.enrichmentRules = enrichmentRules != null ? new ArrayList<>(enrichmentRules) : new ArrayList<>();
    }

    /**
     * Create a new GenericEnricher with the specified parameters.
     *
     * @param name The name of the enricher
     * @param type The class of objects this enricher can enrich
     * @param enrichmentRules The enrichment rules to apply
     */
    public GenericEnricher(String name, Class<T> type, List<EnrichmentRule<T>> enrichmentRules) {
        this(name, type, null, enrichmentRules);
    }

    /**
     * Add an enrichment rule to this enricher.
     *
     * @param enrichmentRule The enrichment rule to add
     */
    public void addEnrichmentRule(EnrichmentRule<T> enrichmentRule) {
        if (enrichmentRule != null) {
            this.enrichmentRules.add(enrichmentRule);
        }
    }

    @Override
    public T enrich(T value) {
        if (value == null) {
            return null;
        }

        try {
            // Create a copy of the object to enrich
            T enrichedValue = createCopy(value);

            // Apply each enrichment rule
            for (EnrichmentRule<T> enrichmentRule : enrichmentRules) {
                applyEnrichmentRule(enrichmentRule, value, enrichedValue);
            }

            return enrichedValue;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error enriching value: " + e.getMessage(), e);
            return value; // Return the original value if enrichment fails
        }
    }

    /**
     * Enrich a value and return a RuleResult.
     *
     * @param value The value to enrich
     * @return A RuleResult containing the enrichment outcome
     */
    public RuleResult enrichWithResult(T value) {
        if (value == null) {
            return RuleResult.error(getName(), "Value is null");
        }

        try {
            T enrichedValue = enrich(value);

            // Check if any enrichment was applied
            boolean enriched = !value.equals(enrichedValue);

            if (enriched) {
                return RuleResult.match(getName(), "Value enriched successfully");
            } else {
                return RuleResult.noMatch();
            }
        } catch (Exception e) {
            return RuleResult.error(getName(), "Error enriching value: " + e.getMessage());
        }
    }

    /**
     * Apply an enrichment rule to a value.
     *
     * @param enrichmentRule The enrichment rule to apply
     * @param originalValue The original value
     * @param enrichedValue The value to enrich
     */
    private void applyEnrichmentRule(EnrichmentRule<T> enrichmentRule, T originalValue, T enrichedValue) {
        // Set up facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("value", originalValue);

        // Add any additional facts from the enrichment rule
        if (enrichmentRule.getAdditionalFacts() != null) {
            facts.putAll(enrichmentRule.getAdditionalFacts());
        }

        // Evaluate the rule
        RuleResult ruleResult = rulesEngine.executeRulesList(List.of(enrichmentRule.getRule()), facts);

        // Apply the appropriate field enrichment action based on the rule result
        if (ruleResult.isTriggered()) {
            // Rule was triggered, apply positive action
            for (FieldEnrichmentAction<T> action : enrichmentRule.getPositiveActions()) {
                applyFieldEnrichmentAction(action, originalValue, enrichedValue, facts);
            }
        } else {
            // Rule was not triggered, apply negative action
            for (FieldEnrichmentAction<T> action : enrichmentRule.getNegativeActions()) {
                applyFieldEnrichmentAction(action, originalValue, enrichedValue, facts);
            }
        }
    }

    /**
     * Apply a field enrichment action to a value.
     *
     * @param action The field enrichment action to apply
     * @param originalValue The original value
     * @param enrichedValue The value to enrich
     * @param facts The facts for rule evaluation
     */
    private void applyFieldEnrichmentAction(FieldEnrichmentAction<T> action, T originalValue, T enrichedValue, Map<String, Object> facts) {
        try {
            // Get the field value from the original object
            Object fieldValue = action.getFieldValueExtractor().apply(originalValue);

            // Calculate the new field value
            Object newFieldValue = action.getFieldValueTransformer().apply(fieldValue, facts);

            // Set the new field value on the enriched object
            action.getFieldValueSetter().accept(enrichedValue, newFieldValue);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying field enrichment action: " + e.getMessage(), e);
        }
    }

    /**
     * Create a copy of an object.
     *
     * @param value The object to copy
     * @return A copy of the object
     * @throws Exception If an error occurs during copying
     */
    @SuppressWarnings("unchecked")
    private T createCopy(T value) throws Exception {
        // Try to use a copy constructor if available
        try {
            return (T) value.getClass().getConstructor(value.getClass()).newInstance(value);
        } catch (NoSuchMethodException e) {
            // No copy constructor, try to use the default constructor and copy fields
            T copy = (T) value.getClass().getConstructor().newInstance();

            // Copy all fields
            for (Field field : value.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                field.set(copy, field.get(value));
            }

            return copy;
        }
    }

}
