package com.rulesengine.demo.service.providers;

import com.rulesengine.core.engine.model.Rule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for defining and creating test business rules.
 * This class centralizes test rule creation and definition, separating it from rule processing.
 */
public class RuleDefinitionServiceDemo {

    /**
     * Creates a rule for investment recommendations.
     * 
     * @return A rule for investment recommendations
     */
    public static Rule createInvestmentRecommendationsRule() {
        return new Rule(
            "Investment Recommendations",
            "#inventory.?[#customer.preferredCategories.contains(category)]",
            "Recommended financial instruments based on investor preferences"
        );
    }

    /**
     * Creates a rule for gold tier investor offers.
     * 
     * @return A rule for gold tier investor offers
     */
    public static Rule createGoldTierInvestorOffersRule() {
        return new Rule(
            "Gold Tier Investor Offers",
            "#customer.membershipLevel == 'Gold' ? " +
            "#inventory.?[price > 500].![name + ' - ' + (price * 0.9) + ' (10% discount)'] : " +
            "#inventory.?[price > 500].![name]",
            "Special investment opportunities for Gold tier investors"
        );
    }

    /**
     * Creates a rule for low-cost investment options.
     * 
     * @return A rule for low-cost investment options
     */
    public static Rule createLowCostInvestmentOptionsRule() {
        return new Rule(
            "Low-Cost Investment Options",
            "#inventory.?[price < 200].![name + ' - $' + price]",
            "Low-cost investment options under $200"
        );
    }

    /**
     * Creates a set of rules for free shipping eligibility, premium discounts, and express processing.
     * 
     * @return A list of rules for order processing
     */
    public static List<Rule> createOrderProcessingRules() {
        return Arrays.asList(
            new Rule(
                "Free shipping eligibility",
                "order.calculateTotal() > 100",
                "Customer is eligible for free shipping"
            ),
            new Rule(
                "Premium discount",
                "customer.membershipLevel == 'Gold' and customer.age > 25",
                "Customer is eligible for premium discount"
            ),
            new Rule(
                "Express processing",
                "order.status == 'PENDING' and order.quantity < 5 and customer.isEligibleForDiscount()",
                "Order is eligible for express processing"
            )
        );
    }

    /**
     * Creates a map of discount rules with dynamic discount percentages based on customer membership level.
     * 
     * @return A map of discount rules
     */
    public static Map<String, String> createDiscountRules() {
        Map<String, String> discountRules = new HashMap<>();
        discountRules.put("Basic", "#{ customer.age > 60 ? 10 : 5 }");
        discountRules.put("Silver", "#{ customer.age > 60 ? 15 : (order.calculateTotal() > 200 ? 12 : 8) }");
        discountRules.put("Gold", "#{ customer.age > 60 ? 20 : (order.calculateTotal() > 200 ? 18 : 15) }");
        return discountRules;
    }
}