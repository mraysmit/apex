package com.rulesengine.demo.service.providers;

import com.rulesengine.core.engine.config.RulesEngine;
import com.rulesengine.core.engine.config.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for calculating different pricing strategies for financial instruments.
 * Uses the RulesEngine to evaluate pricing rules.
 */
public class PricingServiceDemo {
    private final RulesEngine rulesEngine;
    private final Map<String, Rule> pricingRules = new HashMap<>();

    /**
     * Create a new PricingServiceDemo with default rules.
     */
    public PricingServiceDemo() {
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        initializeRules();
    }

    /**
     * Initialize rules for pricing calculations.
     */
    private void initializeRules() {
        // Rule for standard price
        pricingRules.put("StandardPrice", new Rule(
            "StandardPriceRule",
            "#basePrice",
            "Standard price calculation"
        ));

        // Rule for premium price
        pricingRules.put("PremiumPrice", new Rule(
            "PremiumPriceRule",
            "#basePrice * 1.2",
            "Premium price calculation (20% premium)"
        ));

        // Rule for sale price
        pricingRules.put("SalePrice", new Rule(
            "SalePriceRule",
            "#basePrice * 0.8",
            "Sale price calculation (20% discount)"
        ));

        // Rule for clearance price
        pricingRules.put("ClearancePrice", new Rule(
            "ClearancePriceRule",
            "#basePrice * 0.5",
            "Clearance price calculation (50% discount)"
        ));
    }

    /**
     * Calculate standard price with detailed result.
     * 
     * @param basePrice The base price
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateStandardPriceWithResult(double basePrice) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("basePrice", basePrice);

        Rule rule = pricingRules.get("StandardPrice");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate standard price.
     * 
     * @param basePrice The base price
     * @return The standard price
     */
    public double calculateStandardPrice(double basePrice) {
        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        return basePrice;
    }

    /**
     * Calculate premium price with detailed result.
     * 
     * @param basePrice The base price
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculatePremiumPriceWithResult(double basePrice) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("basePrice", basePrice);

        Rule rule = pricingRules.get("PremiumPrice");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate premium price.
     * 
     * @param basePrice The base price
     * @return The premium price
     */
    public double calculatePremiumPrice(double basePrice) {
        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        return basePrice * 1.2; // 20% premium
    }

    /**
     * Calculate sale price with detailed result.
     * 
     * @param basePrice The base price
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateSalePriceWithResult(double basePrice) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("basePrice", basePrice);

        Rule rule = pricingRules.get("SalePrice");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate sale price.
     * 
     * @param basePrice The base price
     * @return The sale price
     */
    public double calculateSalePrice(double basePrice) {
        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        return basePrice * 0.8; // 20% discount
    }

    /**
     * Calculate clearance price with detailed result.
     * 
     * @param basePrice The base price
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateClearancePriceWithResult(double basePrice) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("basePrice", basePrice);

        Rule rule = pricingRules.get("ClearancePrice");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate clearance price.
     * 
     * @param basePrice The base price
     * @return The clearance price
     */
    public double calculateClearancePrice(double basePrice) {
        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        return basePrice * 0.5; // 50% discount
    }
}
