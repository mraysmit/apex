package dev.mars.apex.core.service.transform;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.TransformerRule;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
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
 * Service for generic transformation operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class GenericTransformerService {
    private static final Logger LOGGER = Logger.getLogger(GenericTransformerService.class.getName());
    private final LookupServiceRegistry registry;
    private final RulesEngine rulesEngine;
    /**
     * Create a new GenericTransformerService with the specified registry and rules engine.
     * 
     * @param registry The lookup service registry
     * @param rulesEngine The rules engine to use for transformation
     */
    public GenericTransformerService(LookupServiceRegistry registry, RulesEngine rulesEngine) {
        this.registry = registry;
        this.rulesEngine = rulesEngine;
        LOGGER.info("GenericTransformerService initialized with custom RulesEngine");
    }

    /**
     * Create and register a generic transformer with the specified parameters.
     * 
     * @param <T> The type of objects this transformer can transform
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param transformerRules The transformation rules to apply
     * @return The created transformer
     */
    public <T> GenericTransformer<T> createTransformer(String name, Class<T> type, List<TransformerRule<T>> transformerRules) {
        LOGGER.fine("Creating transformer: " + name);
        GenericTransformer<T> transformer = new GenericTransformer<>(name, type, rulesEngine, transformerRules);
        registry.registerService(transformer);
        return transformer;
    }

    /**
     * Create and register a generic transformer with a single rule.
     * 
     * @param <T> The type of objects this transformer can transform
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param rule The rule to apply
     * @param positiveActions The actions to apply if the rule evaluates to true
     * @param negativeActions The actions to apply if the rule evaluates to false
     * @return The created transformer
     */
    public <T> GenericTransformer<T> createTransformer(
            String name, 
            Class<T> type, 
            Rule rule, 
            List<FieldTransformerAction<T>> positiveActions, 
            List<FieldTransformerAction<T>> negativeActions) {

        LOGGER.fine("Creating transformer with single rule: " + name);

        List<TransformerRule<T>> transformerRules = new ArrayList<>();
        transformerRules.add(new TransformerRule<>(rule, positiveActions, negativeActions));

        return createTransformer(name, type, transformerRules);
    }

    /**
     * Create and register a generic transformer with a single rule and additional facts.
     * 
     * @param <T> The type of objects this transformer can transform
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param rule The rule to apply
     * @param positiveActions The actions to apply if the rule evaluates to true
     * @param negativeActions The actions to apply if the rule evaluates to false
     * @param additionalFacts Additional facts to use during rule evaluation
     * @return The created transformer
     */
    public <T> GenericTransformer<T> createTransformer(
            String name, 
            Class<T> type, 
            Rule rule, 
            List<FieldTransformerAction<T>> positiveActions, 
            List<FieldTransformerAction<T>> negativeActions,
            Map<String, Object> additionalFacts) {

        LOGGER.fine("Creating transformer with single rule and additional facts: " + name);

        List<TransformerRule<T>> transformerRules = new ArrayList<>();
        transformerRules.add(new TransformerRule<>(rule, positiveActions, negativeActions, additionalFacts));

        return createTransformer(name, type, transformerRules);
    }

    /**
     * Transform a value using a dynamically created transformer.
     * 
     * @param <T> The type of the value to transform
     * @param value The value to transform
     * @param transformerRules The transformation rules to apply
     * @return The transformed value
     */
    public <T> T transform(T value, List<TransformerRule<T>> transformerRules) {
        if (value == null) {
            return null;
        }

        LOGGER.fine("Transforming value using dynamic transformer");

        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) value.getClass();
        String name = "DynamicTransformer-" + System.currentTimeMillis();

        GenericTransformer<T> transformer = new GenericTransformer<>(name, type, rulesEngine, transformerRules);
        return transformer.transform(value);
    }

    /**
     * Transform a value using a dynamically created transformer and return a RuleResult.
     * 
     * @param <T> The type of the value to transform
     * @param value The value to transform
     * @param transformerRules The transformation rules to apply
     * @return A RuleResult containing the transformation outcome
     */
    public <T> RuleResult transformWithResult(T value, List<TransformerRule<T>> transformerRules) {
        if (value == null) {
            return RuleResult.error("DynamicTransformer", "Value is null");
        }

        LOGGER.fine("Transforming value using dynamic transformer with result");

        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) value.getClass();
        String name = "DynamicTransformer-" + System.currentTimeMillis();

        GenericTransformer<T> transformer = new GenericTransformer<>(name, type, rulesEngine, transformerRules);
        return transformer.transformWithResult(value);
    }

    /**
     * Transform a value using a registered transformer.
     * 
     * @param <T> The type of the value to transform
     * @param transformerName The name of the transformer to use
     * @param value The value to transform
     * @return The transformed value
     */
    @SuppressWarnings("unchecked")
    public <T> T transform(String transformerName, T value) {
        LOGGER.fine("Transforming value using transformer: " + transformerName);

        // Get the transformer from the registry
        GenericTransformer<?> transformer = registry.getService(transformerName, GenericTransformer.class);
        if (transformer == null) {
            LOGGER.warning("Transformer not found: " + transformerName);
            return value;
        }

        // Check if the transformer can handle this type
        if (value != null && !transformer.getType().isInstance(value)) {
            LOGGER.warning("Transformer " + transformerName + " cannot handle type: " + value.getClass().getName());
            return value;
        }

        // Call the transformer with the appropriate type
        GenericTransformer<T> typedTransformer = (GenericTransformer<T>) transformer;
        return typedTransformer.transform(value);
    }

    /**
     * Transform a value using a registered transformer and return a RuleResult.
     * 
     * @param <T> The type of the value to transform
     * @param transformerName The name of the transformer to use
     * @param value The value to transform
     * @return A RuleResult containing the transformation outcome
     */
    public <T> RuleResult transformWithResult(String transformerName, T value) {
        LOGGER.fine("Transforming value using transformer with result: " + transformerName);

        // Get the transformer from the registry
        GenericTransformer<?> transformer = registry.getService(transformerName, GenericTransformer.class);
        if (transformer == null) {
            LOGGER.warning("Transformer not found: " + transformerName);
            return RuleResult.error(transformerName, "Transformer not found");
        }

        // Check if the transformer can handle this type
        if (value != null && !transformer.getType().isInstance(value)) {
            LOGGER.warning("Transformer " + transformerName + " cannot handle type: " + value.getClass().getName());
            return RuleResult.error(transformerName, "Transformer cannot handle type: " + value.getClass().getName());
        }

        try {
            // Cast to the appropriate type and use transformWithResult method
            @SuppressWarnings("unchecked")
            GenericTransformer<T> typedTransformer = (GenericTransformer<T>) transformer;
            return typedTransformer.transformWithResult(value);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error transforming value: " + e.getMessage(), e);
            return RuleResult.error(transformerName, "Error transforming value: " + e.getMessage());
        }
    }

    /**
     * Apply a rule to a value using a registered transformer.
     * If the rule evaluates to true, the value is transformed using the specified transformer.
     * 
     * @param <T> The type of the value
     * @param rule The rule to apply
     * @param value The value to transform
     * @param additionalFacts Additional facts to use during rule evaluation
     * @param transformerName The name of the transformer to use
     * @return The transformed value if the rule evaluates to true, otherwise the original value
     */
    public <T> T applyRule(Rule rule, T value, Map<String, Object> additionalFacts, String transformerName) {
        LOGGER.fine("Applying rule to value: " + rule.getName());

        // Get the transformer from the registry
        GenericTransformer<?> transformer = registry.getService(transformerName, GenericTransformer.class);
        if (transformer == null) {
            LOGGER.warning("Transformer not found: " + transformerName);
            return value;
        }

        // Check if the transformer can handle this type
        if (value != null && !transformer.getType().isInstance(value)) {
            LOGGER.warning("Transformer " + transformerName + " cannot handle type: " + value.getClass().getName());
            return value;
        }

        // Create facts for the rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("value", value);

        // Add any additional facts
        if (additionalFacts != null) {
            facts.putAll(additionalFacts);
        }

        // Create a list of rules
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Execute the rule
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // If the rule was triggered, transform the value
        if (result.isTriggered()) {
            LOGGER.fine("Rule triggered, transforming value");
            @SuppressWarnings("unchecked")
            GenericTransformer<T> typedTransformer = (GenericTransformer<T>) transformer;
            return typedTransformer.transform(value);
        } else {
            LOGGER.fine("Rule not triggered, returning original value");
            return value;
        }
    }

    /**
     * Apply a rule condition to a value using a registered transformer.
     * If the condition evaluates to true, the value is transformed using the specified transformer.
     * 
     * @param <T> The type of the value
     * @param ruleCondition The rule condition to apply
     * @param value The value to transform
     * @param additionalFacts Additional facts to use during rule evaluation
     * @param transformerName The name of the transformer to use
     * @return The transformed value if the rule condition evaluates to true, otherwise the original value
     */
    public <T> T applyRuleCondition(String ruleCondition, T value, Map<String, Object> additionalFacts, String transformerName) {
        LOGGER.fine("Applying rule condition to value: " + ruleCondition);

        // Create a rule from the condition
        Rule rule = new Rule(
            "Transformation Rule",
            ruleCondition,
            "Transformation rule with condition: " + ruleCondition
        );

        // Apply the rule
        return applyRule(rule, value, additionalFacts, transformerName);
    }

    /**
     * Apply a rule condition to a value using a registered transformer.
     * If the condition evaluates to true, the value is transformed using the specified transformer.
     * This overload is for compatibility with TransformationEnrichmentService.
     * 
     * @param <T> The type of the value
     * @param ruleCondition The rule condition to apply
     * @param value The value to transform
     * @param lookupData The lookup data to use for transformation
     * @param transformerName The name of the transformer to use
     * @return The transformed value if the rule condition evaluates to true, otherwise the original value
     */
    public <T> T applyRuleCondition(String ruleCondition, T value, Object lookupData, String transformerName) {
        LOGGER.fine("Applying rule condition to value with lookup data: " + ruleCondition);

        // Create a map of additional facts with the lookup data
        Map<String, Object> additionalFacts = new HashMap<>();
        additionalFacts.put("lookupData", lookupData);

        // Create a rule from the condition that uses lookupData instead of additionalFacts
        Rule rule = new Rule(
            "Transformation Rule",
            ruleCondition.replace("#coreData", "#value"),
            "Transformation rule with condition: " + ruleCondition
        );

        // Apply the rule
        return applyRule(rule, value, additionalFacts, transformerName);
    }

    /**
     * Create a field transformer action for a specific field.
     * 
     * @param <T> The type of objects this action can transform
     * @param fieldName The name of the field
     * @param fieldValueExtractor A function to extract the field value
     * @param fieldValueTransformer A function to transform the field value
     * @param fieldValueSetter A function to set the field value
     * @return The created field transformer action
     */
    public <T> FieldTransformerAction<T> createFieldTransformerAction(
            String fieldName,
            Function<T, Object> fieldValueExtractor,
            BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer,
            BiConsumer<T, Object> fieldValueSetter) {

        return new FieldTransformerActionBuilder<T>()
            .withFieldName(fieldName)
            .withFieldValueExtractor(fieldValueExtractor)
            .withFieldValueTransformer(fieldValueTransformer)
            .withFieldValueSetter(fieldValueSetter)
            .build();
    }

    /**
     * Create a copy of an object using proper object-oriented design patterns.
     * This method avoids reflection and uses proper copying strategies.
     *
     * @param <T> The type of the object
     * @param value The object to copy
     * @return A copy of the object
     * @throws Exception If an error occurs during copying
     */
    @SuppressWarnings("unchecked")
    public <T> T createCopy(T value) throws Exception {
        if (value == null) {
            return null;
        }

        // Strategy 1: Check if object implements Cloneable
        if (value instanceof Cloneable) {
            try {
                Method cloneMethod = value.getClass().getMethod("clone");
                return (T) cloneMethod.invoke(value);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // Fall through to next strategy
            }
        }

        // Strategy 2: Check for copy constructor
        try {
            return (T) value.getClass().getConstructor(value.getClass()).newInstance(value);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Fall through to next strategy
        }

        // Strategy 3: Use serialization for deep copying (safer than reflection)
        return createCopyViaSerialization(value);
    }

    /**
     * Create a copy using serialization/deserialization.
     * This is safer than reflection and respects object encapsulation.
     */
    @SuppressWarnings("unchecked")
    private <T> T createCopyViaSerialization(T value) throws Exception {
        // Use Java serialization for deep copying
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bos)) {
            oos.writeObject(value);
        }

        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bos.toByteArray());
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bis)) {
            return (T) ois.readObject();
        }
    }

    /**
     * Get the names of all registered transformers.
     *
     * @return Array of registered transformer names
     */
    public String[] getRegisteredTransformers() {
        return registry.getServiceNames(GenericTransformer.class);
    }
}
