package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.TransformerRule;
import dev.mars.apex.core.service.transform.FieldTransformerAction;
import dev.mars.apex.core.service.transform.FieldTransformerActionBuilder;
import dev.mars.apex.demo.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

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
 * Factory for creating product transformers.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class ProductTransformerDemoConfig {
    private final RulesEngine rulesEngine;

    /**
     * Create a new ProductTransformerDemoConfig with the specified rules engine.
     * 
     * @param rulesEngine The rules engine to use for transformation
     */
    public ProductTransformerDemoConfig(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    /**
     * Create a transformer rule for applying a discount to a product.
     * 
     * @param discount The discount to apply (as a decimal, e.g., 0.15 for 15%)
     * @return The transformer rule
     */
    public TransformerRule<Product> createDiscountRule(double discount) {
        // Create a rule that always matches
        Rule rule = new Rule(
            "DiscountRule",
            "true",
            "Apply discount to product"
        );

        // Create a field transformer action for the price field
        FieldTransformerAction<Product> priceAction = createPriceDiscountAction(discount);

        // Create a list of positive actions (actions to apply when the rule matches)
        List<FieldTransformerAction<Product>> positiveActions = new ArrayList<>();
        positiveActions.add(priceAction);

        // Create a list of negative actions (actions to apply when the rule doesn't match)
        List<FieldTransformerAction<Product>> negativeActions = new ArrayList<>();

        // Create additional facts for the rule evaluation
        Map<String, Object> additionalFacts = new HashMap<>();
        additionalFacts.put("discount", discount);

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions, additionalFacts);
    }

    /**
     * Create a field transformer action for applying a discount to the price field.
     * 
     * @param discount The discount to apply (as a decimal, e.g., 0.15 for 15%)
     * @return The field transformer action
     */
    private FieldTransformerAction<Product> createPriceDiscountAction(double discount) {
        // Create a function to extract the price field
        Function<Product, Object> extractor = Product::getPrice;

        // Create a function to transform the price field
        BiFunction<Object, Map<String, Object>, Object> transformer = (price, facts) -> {
            double originalPrice = (double) price;
            double discountAmount = originalPrice * discount;
            return originalPrice - discountAmount;
        };

        // Create a function to set the price field
        BiConsumer<Product, Object> setter = (product, price) -> product.setPrice((double) price);

        // Create and return the field transformer action
        return new FieldTransformerActionBuilder<Product>()
            .withFieldName("price")
            .withFieldValueExtractor(extractor)
            .withFieldValueTransformer(transformer)
            .withFieldValueSetter(setter)
            .build();
    }

    /**
     * Create a transformer rule for applying a discount to a product based on a condition.
     * 
     * @param condition The condition to evaluate
     * @param discount The discount to apply (as a decimal, e.g., 0.15 for 15%)
     * @return The transformer rule
     */
    public TransformerRule<Product> createConditionalDiscountRule(String condition, double discount) {
        // Create a rule with the specified condition
        Rule rule = new Rule(
            "ConditionalDiscountRule",
            condition,
            "Apply discount to product if condition is met"
        );

        // Create a field transformer action for the price field
        FieldTransformerAction<Product> priceAction = createPriceDiscountAction(discount);

        // Create a list of positive actions (actions to apply when the rule matches)
        List<FieldTransformerAction<Product>> positiveActions = new ArrayList<>();
        positiveActions.add(priceAction);

        // Create a list of negative actions (actions to apply when the rule doesn't match)
        List<FieldTransformerAction<Product>> negativeActions = new ArrayList<>();

        // Create additional facts for the rule evaluation
        Map<String, Object> additionalFacts = new HashMap<>();
        additionalFacts.put("discount", discount);

        // Create and return the transformer rule
        return new TransformerRule<>(rule, positiveActions, negativeActions, additionalFacts);
    }

    /**
     * Get the rules engine used by this factory.
     * 
     * @return The rules engine
     */
    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }
}
