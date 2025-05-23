```java
// GenericEnricher.java
package com.rulesengine.core.service.transform;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Represents an enrichment rule with associated field enrichment actions.
     *
     * @param <T> The type of object this rule can enrich
     */
    public static class EnrichmentRule<T> {
        private final Rule rule;
        private final List<FieldEnrichmentAction<T>> positiveActions;
        private final List<FieldEnrichmentAction<T>> negativeActions;
        private final Map<String, Object> additionalFacts;

        /**
         * Create a new EnrichmentRule with the specified parameters.
         *
         * @param rule The rule to evaluate
         * @param positiveActions The actions to apply when the rule is triggered
         * @param negativeActions The actions to apply when the rule is not triggered
         * @param additionalFacts Additional facts to add to the rule evaluation context
         */
        public EnrichmentRule(Rule rule, List<FieldEnrichmentAction<T>> positiveActions,
                              List<FieldEnrichmentAction<T>> negativeActions, Map<String, Object> additionalFacts) {
            this.rule = rule;
            this.positiveActions = positiveActions != null ? new ArrayList<>(positiveActions) : new ArrayList<>();
            this.negativeActions = negativeActions != null ? new ArrayList<>(negativeActions) : new ArrayList<>();
            this.additionalFacts = additionalFacts != null ? new HashMap<>(additionalFacts) : new HashMap<>();
        }

        /**
         * Create a new EnrichmentRule with the specified parameters.
         *
         * @param rule The rule to evaluate
         * @param positiveActions The actions to apply when the rule is triggered
         * @param negativeActions The actions to apply when the rule is not triggered
         */
        public EnrichmentRule(Rule rule, List<FieldEnrichmentAction<T>> positiveActions,
                              List<FieldEnrichmentAction<T>> negativeActions) {
            this(rule, positiveActions, negativeActions, null);
        }

        /**
         * Get the rule to evaluate.
         *
         * @return The rule
         */
        public Rule getRule() {
            return rule;
        }

        /**
         * Get the actions to apply when the rule is triggered.
         *
         * @return The positive actions
         */
        public List<FieldEnrichmentAction<T>> getPositiveActions() {
            return positiveActions;
        }

        /**
         * Get the actions to apply when the rule is not triggered.
         *
         * @return The negative actions
         */
        public List<FieldEnrichmentAction<T>> getNegativeActions() {
            return negativeActions;
        }

        /**
         * Get additional facts to add to the rule evaluation context.
         *
         * @return The additional facts
         */
        public Map<String, Object> getAdditionalFacts() {
            return additionalFacts;
        }
    }

    /**
     * Represents an action to enrich a field of an object.
     *
     * @param <T> The type of object this action can enrich
     */
    public static class FieldEnrichmentAction<T> {
        private final String fieldName;
        private final Function<T, Object> fieldValueExtractor;
        private final BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer;
        private final java.util.function.BiConsumer<T, Object> fieldValueSetter;

        /**
         * Create a new FieldEnrichmentAction with the specified parameters.
         *
         * @param fieldName The name of the field to enrich
         * @param fieldValueExtractor A function to extract the field value from the object
         * @param fieldValueTransformer A function to transform the field value
         * @param fieldValueSetter A function to set the field value on the object
         */
        public FieldEnrichmentAction(String fieldName, Function<T, Object> fieldValueExtractor,
                                     BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer,
                                     java.util.function.BiConsumer<T, Object> fieldValueSetter) {
            this.fieldName = fieldName;
            this.fieldValueExtractor = fieldValueExtractor;
            this.fieldValueTransformer = fieldValueTransformer;
            this.fieldValueSetter = fieldValueSetter;
        }

        /**
         * Get the name of the field to enrich.
         *
         * @return The field name
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * Get the function to extract the field value from the object.
         *
         * @return The field value extractor
         */
        public Function<T, Object> getFieldValueExtractor() {
            return fieldValueExtractor;
        }

        /**
         * Get the function to transform the field value.
         *
         * @return The field value transformer
         */
        public BiFunction<Object, Map<String, Object>, Object> getFieldValueTransformer() {
            return fieldValueTransformer;
        }

        /**
         * Get the function to set the field value on the object.
         *
         * @return The field value setter
         */
        public java.util.function.BiConsumer<T, Object> getFieldValueSetter() {
            return fieldValueSetter;
        }
    }

    /**
     * Builder for creating FieldEnrichmentAction instances.
     *
     * @param <T> The type of object this action can enrich
     */
    public static class FieldEnrichmentActionBuilder<T> {
        private String fieldName;
        private Function<T, Object> fieldValueExtractor;
        private BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer;
        private java.util.function.BiConsumer<T, Object> fieldValueSetter;

        /**
         * Set the name of the field to enrich.
         *
         * @param fieldName The field name
         * @return This builder for method chaining
         */
        public FieldEnrichmentActionBuilder<T> withFieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Set the function to extract the field value from the object.
         *
         * @param fieldValueExtractor The field value extractor
         * @return This builder for method chaining
         */
        public FieldEnrichmentActionBuilder<T> withFieldValueExtractor(Function<T, Object> fieldValueExtractor) {
            this.fieldValueExtractor = fieldValueExtractor;
            return this;
        }

        /**
         * Set the function to transform the field value.
         *
         * @param fieldValueTransformer The field value transformer
         * @return This builder for method chaining
         */
        public FieldEnrichmentActionBuilder<T> withFieldValueTransformer(BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer) {
            this.fieldValueTransformer = fieldValueTransformer;
            return this;
        }

        /**
         * Set the function to set the field value on the object.
         *
         * @param fieldValueSetter The field value setter
         * @return This builder for method chaining
         */
        public FieldEnrichmentActionBuilder<T> withFieldValueSetter(java.util.function.BiConsumer<T, Object> fieldValueSetter) {
            this.fieldValueSetter = fieldValueSetter;
            return this;
        }

        /**
         * Build a FieldEnrichmentAction instance.
         *
         * @return A new FieldEnrichmentAction instance
         */
        public FieldEnrichmentAction<T> build() {
            return new FieldEnrichmentAction<>(fieldName, fieldValueExtractor, fieldValueTransformer, fieldValueSetter);
        }
    }
}

// ProductEnricherFactory.java
package com.rulesengine.demo.service.enrichers;

import RulesEngine;
import RulesEngineConfiguration;
import Rule;
import com.rulesengine.core.service.transform.GenericEnricher;
import com.rulesengine.demo.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating product enrichers using the GenericEnricher.
 */
public class ProductEnricherFactory {

    /**
     * Create a product enricher with default rules and field mappings.
     *
     * @param name The name of the enricher
     * @return A new GenericEnricher for products
     */
    public static GenericEnricher<Product> createProductEnricher(String name) {
        // Create rules engine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create category discount rule
        Rule categoryDiscountRule = new Rule(
                "CategoryDiscountRule",
                "#categoryDiscounts.containsKey(#value.category)",
                "Product category has a discount"
        );

        // Create category description rule
        Rule categoryDescriptionRule = new Rule(
                "CategoryDescriptionRule",
                "#categoryDescriptions.containsKey(#value.category)",
                "Product category has a description"
        );

        // Create additional facts
        Map<String, Object> additionalFacts = new HashMap<>();

        // Set up category discounts
        Map<String, Double> categoryDiscounts = new HashMap<>();
        categoryDiscounts.put("Equity", 0.05);  // 5% discount
        categoryDiscounts.put("FixedIncome", 0.03);  // 3% discount
        categoryDiscounts.put("ETF", 0.02);  // 2% discount
        additionalFacts.put("categoryDiscounts", categoryDiscounts);

        // Set up category descriptions
        Map<String, String> categoryDescriptions = new HashMap<>();
        categoryDescriptions.put("Equity", "Stocks representing ownership in a company");
        categoryDescriptions.put("FixedIncome", "Debt securities with fixed interest payments");
        categoryDescriptions.put("ETF", "Exchange-traded funds tracking an index or sector");
        additionalFacts.put("categoryDescriptions", categoryDescriptions);

        // Create price enrichment action for when discount is available
        GenericEnricher.FieldEnrichmentAction<Product> priceDiscountAction = new GenericEnricher.FieldEnrichmentActionBuilder<Product>()
                .withFieldName("price")
                .withFieldValueExtractor(Product::getPrice)
                .withFieldValueTransformer((price, facts) -> {
                    Map<String, Double> discounts = (Map<String, Double>) facts.get("categoryDiscounts");
                    String category = ((Product) facts.get("value")).getCategory();
                    double discount = discounts.getOrDefault(category, 0.0);
                    return (double) price * (1 - discount);
                })
                .withFieldValueSetter(Product::setPrice)
                .build();

        // Create name enrichment action for when description is available
        GenericEnricher.FieldEnrichmentAction<Product> nameDescriptionAction = new GenericEnricher.FieldEnrichmentActionBuilder<Product>()
                .withFieldName("name")
                .withFieldValueExtractor(Product::getName)
                .withFieldValueTransformer((name, facts) -> {
                    Map<String, String> descriptions = (Map<String, String>) facts.get("categoryDescriptions");
                    String category = ((Product) facts.get("value")).getCategory();
                    String description = descriptions.get(category);
                    return name + " - " + description;
                })
                .withFieldValueSetter(Product::setName)
                .build();

        // Create enrichment rules
        List<GenericEnricher.EnrichmentRule<Product>> enrichmentRules = new ArrayList<>();

        // Add category discount rule
        enrichmentRules.add(new GenericEnricher.EnrichmentRule<>(
                categoryDiscountRule,
                List.of(priceDiscountAction),  // Apply discount when rule is triggered
                List.of(),  // Do nothing when rule is not triggered
                additionalFacts
        ));

        // Add category description rule
        enrichmentRules.add(new GenericEnricher.EnrichmentRule<>(
                categoryDescriptionRule,
                List.of(nameDescriptionAction),  // Add description when rule is triggered
                List.of(),  // Do nothing when rule is not triggered
                additionalFacts
        ));

        // Create and return the generic enricher
        return new GenericEnricher<>(name, Product.class, rulesEngine, enrichmentRules);
    }

    /**
     * Create a product enricher with custom rules and field mappings.
     *
     * @param name The name of the enricher
     * @param rules The rules to use for enrichment
     * @param fieldMappings The field mappings to use for enrichment
     * @return A new GenericEnricher for products
     */
    public static GenericEnricher<Product> createProductEnricher(
            String name,
            List<Rule> rules,
            Map<String, Object> fieldMappings) {

        // Create rules engine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create enrichment rules
        List<GenericEnricher.EnrichmentRule<Product>> enrichmentRules = new ArrayList<>();

        // For each rule, create an enrichment rule with appropriate actions
        for (Rule rule : rules) {
            // Get the field mappings for this rule
            Map<String, Object> ruleMappings = (Map<String, Object>) fieldMappings.get(rule.getName());
            if (ruleMappings == null) {
                continue;
            }

            // Get the positive and negative actions for this rule
            List<GenericEnricher.FieldEnrichmentAction<Product>> positiveActions =
                    (List<GenericEnricher.FieldEnrichmentAction<Product>>) ruleMappings.get("positiveActions");
            List<GenericEnricher.FieldEnrichmentAction<Product>> negativeActions =
                    (List<GenericEnricher.FieldEnrichmentAction<Product>>) ruleMappings.get("negativeActions");

            // Get additional facts for this rule
            Map<String, Object> additionalFacts = (Map<String, Object>) ruleMappings.get("additionalFacts");

            // Create and add the enrichment rule
            enrichmentRules.add(new GenericEnricher.EnrichmentRule<>(
                    rule,
                    positiveActions != null ? positiveActions : List.of(),
                    negativeActions != null ? negativeActions : List.of(),
                    additionalFacts
            ));
        }

        // Create and return the generic enricher
        return new GenericEnricher<>(name, Product.class, rulesEngine, enrichmentRules);
    }
}
```

I've implemented the GenericEnricher class and ProductEnricherFactory as specified in the issue description. Here's a summary of what each class does:

## GenericEnricher

The GenericEnricher is a flexible, configurable enricher that can work with any type of object. It follows SOLID principles with clear separation of concerns:

1. **Single Responsibility**: It only handles enrichment based on rules and field mappings.
2. **Open/Closed**: It's open for extension (via configuration) but closed for modification.
3. **Dependency Injection**: Dependencies are injected rather than created internally.

Key features:
- Takes rules and field mappings as parameters, containing no hard-coded business logic
- Supports both positive actions (when a rule is triggered) and negative actions (when not triggered)
- Uses reflection to create copies of objects when needed
- Provides detailed RuleResult objects with enrichment outcomes
- Includes a builder pattern for creating field enrichment actions

## ProductEnricherFactory

The ProductEnricherFactory demonstrates how to use the GenericEnricher to replace the hard-coded ProductEnricher:

1. Creates rules for determining when to apply enrichments
2. Defines field mappings that specify which fields to enrich and how
3. Configures additional facts needed for rule evaluation
4. Provides both a default configuration and a custom configuration option

This implementation addresses the issue by:
- Removing hard-coded field enrichment logic from ProductEnricher
- Making the enrichment process fully configurable
- Ensuring all business logic is implemented using the RulesEngine
- Following SOLID principles with clear separation of concerns

The factory pattern makes it easy to create properly configured GenericEnricher instances for Product objects while hiding the complexity of the configuration.