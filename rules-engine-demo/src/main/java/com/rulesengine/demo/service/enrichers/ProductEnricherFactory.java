package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.EnrichmentRule;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.service.transform.FieldEnrichmentAction;
import com.rulesengine.core.service.transform.FieldEnrichmentActionBuilder;
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
    private final RulesEngine rulesEngine;
    
    /**
     * Create a new ProductEnricherFactory with the specified RulesEngine.
     *
     * @param rulesEngine The rules engine to use for enrichment
     */
    public ProductEnricherFactory(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }
    
    /**
     * Create a product enricher with default rules and field mappings.
     *
     * @param name The name of the enricher
     * @return A new GenericEnricher for products
     */
    public GenericEnricher<Product> createProductEnricher(String name) {
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
        FieldEnrichmentAction<Product> priceDiscountAction = new FieldEnrichmentActionBuilder<Product>()
            .withFieldName("price")
            .withFieldValueExtractor(Product::getPrice)
            .withFieldValueTransformer((price, facts) -> {
                Map<String, Double> discounts = (Map<String, Double>) facts.get("categoryDiscounts");
                String category = ((Product) facts.get("value")).getCategory();
                double discount = discounts.getOrDefault(category, 0.0);
                double priceValue = ((Number) price).doubleValue();
                return priceValue * (1 - discount);
            })
            .withFieldValueSetter((product, value) -> product.setPrice(((Number) value).doubleValue()))
            .build();
        
        // Create name enrichment action for when description is available
        FieldEnrichmentAction<Product> nameDescriptionAction = new FieldEnrichmentActionBuilder<Product>()
            .withFieldName("name")
            .withFieldValueExtractor(Product::getName)
            .withFieldValueTransformer((productName, facts) -> {
                Map<String, String> descriptions = (Map<String, String>) facts.get("categoryDescriptions");
                String category = ((Product) facts.get("value")).getCategory();
                String description = descriptions.get(category);
                String nameValue = String.valueOf(productName);
                return nameValue + " - " + description;
            })
            .withFieldValueSetter((product, value) -> product.setName(String.valueOf(value)))
            .build();
        
        // Create enrichment rules
        List<EnrichmentRule<Product>> enrichmentRules = new ArrayList<>();
        
        // Add category discount rule
        enrichmentRules.add(new EnrichmentRule<>(
            categoryDiscountRule,
            List.of(priceDiscountAction),  // Apply discount when rule is triggered
            List.of(),  // Do nothing when rule is not triggered
            additionalFacts
        ));
        
        // Add category description rule
        enrichmentRules.add(new EnrichmentRule<>(
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
    public GenericEnricher<Product> createProductEnricher(
            String name, 
            List<Rule> rules, 
            Map<String, Object> fieldMappings) {
        
        // Create enrichment rules
        List<EnrichmentRule<Product>> enrichmentRules = new ArrayList<>();
        
        // For each rule, create an enrichment rule with appropriate actions
        for (Rule rule : rules) {
            // Get the field mappings for this rule
            Map<String, Object> ruleMappings = (Map<String, Object>) fieldMappings.get(rule.getName());
            if (ruleMappings == null) {
                continue;
            }
            
            // Get the positive and negative actions for this rule
            List<FieldEnrichmentAction<Product>> positiveActions = 
                (List<FieldEnrichmentAction<Product>>) ruleMappings.get("positiveActions");
            List<FieldEnrichmentAction<Product>> negativeActions = 
                (List<FieldEnrichmentAction<Product>>) ruleMappings.get("negativeActions");
            
            // Get additional facts for this rule
            Map<String, Object> additionalFacts = (Map<String, Object>) ruleMappings.get("additionalFacts");
            
            // Create and add the enrichment rule
            enrichmentRules.add(new EnrichmentRule<>(
                rule,
                positiveActions != null ? positiveActions : List.of(),
                negativeActions != null ? negativeActions : List.of(),
                additionalFacts
            ));
        }
        
        // Create and return the generic enricher
        return new GenericEnricher<>(name, Product.class, rulesEngine, enrichmentRules);
    }
    
    /**
     * Create a product enricher that can apply a custom discount.
     * This method is provided for backward compatibility with code that uses
     * ProductEnricher.enrich(product, customDiscount).
     *
     * @param name The name of the enricher
     * @param customDiscount The custom discount to apply
     * @return A new GenericEnricher for products that applies the custom discount
     */
    public GenericEnricher<Product> createProductEnricherWithCustomDiscount(String name, Double customDiscount) {
        // Create custom discount rule
        Rule customDiscountRule = new Rule(
            "CustomDiscountRule",
            "true", // Always apply the custom discount
            "Apply custom discount to product"
        );
        
        // Create additional facts
        Map<String, Object> additionalFacts = new HashMap<>();
        additionalFacts.put("customDiscount", customDiscount);
        
        // Create price enrichment action for custom discount
        FieldEnrichmentAction<Product> priceDiscountAction = new FieldEnrichmentActionBuilder<Product>()
            .withFieldName("price")
            .withFieldValueExtractor(Product::getPrice)
            .withFieldValueTransformer((price, facts) -> {
                Double discount = (Double) facts.get("customDiscount");
                double priceValue = ((Number) price).doubleValue();
                return priceValue * (1 - discount);
            })
            .withFieldValueSetter((product, value) -> product.setPrice(((Number) value).doubleValue()))
            .build();
        
        // Create enrichment rules
        List<EnrichmentRule<Product>> enrichmentRules = new ArrayList<>();
        
        // Add custom discount rule
        enrichmentRules.add(new EnrichmentRule<>(
            customDiscountRule,
            List.of(priceDiscountAction),  // Apply discount when rule is triggered
            List.of(),  // Do nothing when rule is not triggered
            additionalFacts
        ));
        
        // Create and return the generic enricher
        return new GenericEnricher<>(name, Product.class, rulesEngine, enrichmentRules);
    }
}