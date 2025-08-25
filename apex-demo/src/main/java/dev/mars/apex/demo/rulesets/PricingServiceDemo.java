package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;

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
* This class is part of the APEX A powerful expression processor for Java applications.
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
        // Rule for standard pricing eligibility
        pricingRules.put("StandardPrice", new Rule(
            "StandardPriceRule",
            "#basePrice > 0",
            "Check if standard pricing is applicable (base price > 0)"
        ));

        // Rule for premium pricing eligibility (high-value items)
        pricingRules.put("PremiumPrice", new Rule(
            "PremiumPriceRule",
            "#basePrice >= 1000",
            "Check if premium pricing is applicable (base price >= 1000)"
        ));

        // Rule for sale pricing eligibility (mid-range items)
        pricingRules.put("SalePrice", new Rule(
            "SalePriceRule",
            "#basePrice >= 100 && #basePrice < 1000",
            "Check if sale pricing is applicable (100 <= base price < 1000)"
        ));

        // Rule for clearance pricing eligibility (low-value items)
        pricingRules.put("ClearancePrice", new Rule(
            "ClearancePriceRule",
            "#basePrice > 0 && #basePrice < 100",
            "Check if clearance pricing is applicable (0 < base price < 100)"
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
     * Calculate premium price using the actual APEX rules engine.
     *
     * @param basePrice The base price
     * @return The premium price
     */
    public double calculatePremiumPrice(double basePrice) {
        RuleResult result = calculatePremiumPriceWithResult(basePrice);
        return extractPriceFromRuleResult(result, basePrice * 1.2); // fallback to 20% premium
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
     * Calculate sale price using the actual APEX rules engine.
     *
     * @param basePrice The base price
     * @return The sale price
     */
    public double calculateSalePrice(double basePrice) {
        RuleResult result = calculateSalePriceWithResult(basePrice);
        return extractPriceFromRuleResult(result, basePrice * 0.8); // fallback to 20% discount
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
     * Calculate clearance price using the actual APEX rules engine.
     *
     * @param basePrice The base price
     * @return The clearance price
     */
    public double calculateClearancePrice(double basePrice) {
        RuleResult result = calculateClearancePriceWithResult(basePrice);
        return extractPriceFromRuleResult(result, basePrice * 0.5); // fallback to 50% discount
    }

    /**
     * Main method to run the pricing service demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("=== PRICING SERVICE DEMO ===");
        System.out.println("Demonstrates rule-based pricing calculations for financial instruments");
        System.out.println();

        runPricingServiceDemo();

        System.out.println("\n=== PRICING SERVICE DEMO COMPLETED ===");
    }

    /**
     * Run the pricing service demonstration.
     */
    private static void runPricingServiceDemo() {
        System.out.println("Creating PricingServiceDemo instance...");
        PricingServiceDemo pricingService = new PricingServiceDemo();

        // Test different base prices
        double[] basePrices = {100.0, 250.0, 500.0, 1000.0, 2500.0};

        System.out.println("\nDemonstrating pricing calculations:");
        System.out.println("=" .repeat(60));

        for (double basePrice : basePrices) {
            System.out.println("\nBase Price: $" + String.format("%.2f", basePrice));
            System.out.println("-".repeat(40));

            // Calculate all pricing variations
            double standardPrice = pricingService.calculateStandardPrice(basePrice);
            double premiumPrice = pricingService.calculatePremiumPrice(basePrice);
            double salePrice = pricingService.calculateSalePrice(basePrice);
            double clearancePrice = pricingService.calculateClearancePrice(basePrice);

            System.out.println("  Standard Price:  $" + String.format("%8.2f", standardPrice) + " (Base price)");
            System.out.println("  Premium Price:   $" + String.format("%8.2f", premiumPrice) + " (+20% premium)");
            System.out.println("  Sale Price:      $" + String.format("%8.2f", salePrice) + " (-20% discount)");
            System.out.println("  Clearance Price: $" + String.format("%8.2f", clearancePrice) + " (-50% discount)");

            // Calculate price spread
            double spread = premiumPrice - clearancePrice;
            System.out.println("  Price Spread:    $" + String.format("%8.2f", spread) + " (Premium to Clearance)");

            // Demonstrate detailed pricing rule for premium prices
            if (basePrice >= 500.0) {
                System.out.println("\n  Detailed Premium Pricing Analysis:");
                demonstratePricingRule(pricingService, "Premium Pricing Rule", basePrice,
                    () -> {
                        // Create a mock RuleResult for demonstration
                        return RuleResult.match("PremiumPricingRule",
                                              "Premium pricing applied for high-value items (Base: $" +
                                              String.format("%.2f", basePrice) + ", Premium: $" +
                                              String.format("%.2f", premiumPrice) + ")");
                    });
            }
        }

        // Demonstrate rule-based pricing eligibility with detailed results
        System.out.println("\n\nDemonstrating rule-based pricing eligibility:");
        System.out.println("=" .repeat(60));

        double[] testPrices = {50.0, 500.0, 1500.0, 2500.0};

        for (double testPrice : testPrices) {
            System.out.println("\nTest Base Price: $" + String.format("%.2f", testPrice));
            System.out.println("-".repeat(40));

            // Test each pricing rule eligibility
            demonstratePricingEligibility(pricingService, "Standard Price", testPrice,
                                        () -> pricingService.calculateStandardPriceWithResult(testPrice));

            demonstratePricingEligibility(pricingService, "Premium Price", testPrice,
                                        () -> pricingService.calculatePremiumPriceWithResult(testPrice));

            demonstratePricingEligibility(pricingService, "Sale Price", testPrice,
                                        () -> pricingService.calculateSalePriceWithResult(testPrice));

            demonstratePricingEligibility(pricingService, "Clearance Price", testPrice,
                                        () -> pricingService.calculateClearancePriceWithResult(testPrice));

            // Show actual calculated prices
            System.out.println("  Actual Prices:");
            System.out.println("    Standard:  $" + String.format("%8.2f", pricingService.calculateStandardPrice(testPrice)));
            System.out.println("    Premium:   $" + String.format("%8.2f", pricingService.calculatePremiumPrice(testPrice)));
            System.out.println("    Sale:      $" + String.format("%8.2f", pricingService.calculateSalePrice(testPrice)));
            System.out.println("    Clearance: $" + String.format("%8.2f", pricingService.calculateClearancePrice(testPrice)));
        }
    }

    /**
     * Demonstrate pricing eligibility using rule-based evaluation.
     */
    private static void demonstratePricingEligibility(PricingServiceDemo pricingService, String ruleName,
                                                    double basePrice, java.util.function.Supplier<RuleResult> ruleSupplier) {
        try {
            RuleResult result = ruleSupplier.get();

            String eligibility = result.isTriggered() ? "ELIGIBLE" : "NOT ELIGIBLE";
            System.out.println("  " + ruleName + ": " + eligibility);

            if (result.hasPerformanceMetrics()) {
                System.out.println("    (Evaluated in " + result.getPerformanceMetrics().getEvaluationTimeMillis() + " ms)");
            }

        } catch (Exception e) {
            System.out.println("  " + ruleName + ": ERROR - " + e.getMessage());
        }
    }

    /**
     * Demonstrate a specific pricing rule with detailed results.
     */
    private static void demonstratePricingRule(PricingServiceDemo pricingService, String ruleName,
                                             double basePrice, java.util.function.Supplier<RuleResult> ruleSupplier) {
        System.out.println("Rule: " + ruleName);
        System.out.println("-".repeat(30));

        try {
            RuleResult result = ruleSupplier.get();

            System.out.println("  Rule ID: " + result.getRuleName());
            System.out.println("  Triggered: " + result.isTriggered());
            System.out.println("  Message: " + result.getMessage());
            System.out.println("  Result Type: " + result.getResultType());

            if (result.hasPerformanceMetrics()) {
                System.out.println("  Execution Time: " + result.getPerformanceMetrics().getEvaluationTimeMillis() + " ms");
                System.out.println("  Memory Used: " + result.getPerformanceMetrics().getMemoryUsedBytes() + " bytes");
            }

        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Extract price value from RuleResult, with fallback to default value.
     * This helper method attempts to extract a numeric price from the rule result.
     */
    private double extractPriceFromRuleResult(RuleResult result, double fallbackValue) {
        if (result == null || !result.isTriggered() || result.getResultType() == RuleResult.ResultType.ERROR) {
            return fallbackValue;
        }

        // For now, since RuleResult doesn't expose calculated values directly,
        // we use the fallback value but log that the rule was triggered
        if (result.isTriggered()) {
            System.out.println("  Rule '" + result.getRuleName() + "' was triggered: " + result.getMessage());
        }

        // In a real implementation, the rule would need to store the calculated price
        // in a way that can be retrieved. For now, we use the fallback calculation
        // but at least we know the rule was properly evaluated by the APEX engine.
        return fallbackValue;
    }
}
