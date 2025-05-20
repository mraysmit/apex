package com.rulesengine.demo.service.transformers;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.TransformerRule;
import com.rulesengine.core.service.transform.FieldTransformerAction;
import com.rulesengine.core.service.transform.FieldTransformerActionBuilder;
import com.rulesengine.demo.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Factory for creating product transformers.
 * This factory provides methods to create transformer rules for products.
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
