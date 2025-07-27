package dev.mars.rulesengine.demo.service.providers;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
 * Service for calculating different pricing strategies for financial instruments.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
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
