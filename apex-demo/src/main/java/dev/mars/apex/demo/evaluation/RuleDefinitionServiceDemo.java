package dev.mars.apex.demo.evaluation;

import dev.mars.apex.core.engine.model.Rule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * Service for defining and creating test business rules.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
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

    /**
     * Main method to run the rule definition service demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("=== RULE DEFINITION SERVICE DEMO ===");
        System.out.println("Demonstrates comprehensive rule definition and creation patterns");
        System.out.println();

        runRuleDefinitionDemo();

        System.out.println("\n=== RULE DEFINITION SERVICE DEMO COMPLETED ===");
    }

    /**
     * Run the rule definition service demonstration.
     */
    private static void runRuleDefinitionDemo() {
        System.out.println("Creating and demonstrating various business rule definitions...");
        System.out.println("=" .repeat(70));

        // Demonstrate individual rule creation
        demonstrateIndividualRules();

        // Demonstrate rule collections
        demonstrateRuleCollections();

        // Demonstrate rule metadata and descriptions
        demonstrateRuleMetadata();

        // Demonstrate rule complexity analysis
        demonstrateRuleComplexityAnalysis();
    }

    /**
     * Demonstrate creation and properties of individual rules.
     */
    private static void demonstrateIndividualRules() {
        System.out.println("\n1. Individual Rule Creation:");
        System.out.println("-".repeat(40));

        // Investment recommendations rule
        Rule investmentRule = createInvestmentRecommendationsRule();
        System.out.println("Investment Recommendations Rule:");
        System.out.println("  Name: " + investmentRule.getName());
        System.out.println("  Condition: " + investmentRule.getCondition());
        System.out.println("  Description: " + investmentRule.getDescription());

        // High-value transaction rule
        Rule highValueRule = createHighValueTransactionRule();
        System.out.println("\nHigh-Value Transaction Rule:");
        System.out.println("  Name: " + highValueRule.getName());
        System.out.println("  Condition: " + highValueRule.getCondition());
        System.out.println("  Description: " + highValueRule.getDescription());

        // Customer eligibility rule
        Rule eligibilityRule = createCustomerEligibilityRule();
        System.out.println("\nCustomer Eligibility Rule:");
        System.out.println("  Name: " + eligibilityRule.getName());
        System.out.println("  Condition: " + eligibilityRule.getCondition());
        System.out.println("  Description: " + eligibilityRule.getDescription());

        // Product availability rule
        Rule availabilityRule = createProductAvailabilityRule();
        System.out.println("\nProduct Availability Rule:");
        System.out.println("  Name: " + availabilityRule.getName());
        System.out.println("  Condition: " + availabilityRule.getCondition());
        System.out.println("  Description: " + availabilityRule.getDescription());
    }

    /**
     * Demonstrate rule collections and categorization.
     */
    private static void demonstrateRuleCollections() {
        System.out.println("\n2. Rule Collections:");
        System.out.println("-".repeat(40));

        // Financial rules collection
        List<Rule> financialRules = createFinancialRules();
        System.out.println("Financial Rules Collection (" + financialRules.size() + " rules):");
        for (int i = 0; i < financialRules.size(); i++) {
            Rule rule = financialRules.get(i);
            System.out.println("  " + (i + 1) + ". " + rule.getName());
            System.out.println("     Condition: " + rule.getCondition());
        }

        // Customer rules collection
        List<Rule> customerRules = createCustomerRules();
        System.out.println("\nCustomer Rules Collection (" + customerRules.size() + " rules):");
        for (int i = 0; i < customerRules.size(); i++) {
            Rule rule = customerRules.get(i);
            System.out.println("  " + (i + 1) + ". " + rule.getName());
            System.out.println("     Condition: " + rule.getCondition());
        }

        // Product rules collection
        List<Rule> productRules = createProductRules();
        System.out.println("\nProduct Rules Collection (" + productRules.size() + " rules):");
        for (int i = 0; i < productRules.size(); i++) {
            Rule rule = productRules.get(i);
            System.out.println("  " + (i + 1) + ". " + rule.getName());
            System.out.println("     Condition: " + rule.getCondition());
        }
    }

    /**
     * Demonstrate rule metadata and descriptions.
     */
    private static void demonstrateRuleMetadata() {
        System.out.println("\n3. Rule Metadata Analysis:");
        System.out.println("-".repeat(40));

        List<Rule> allRules = Arrays.asList(
            createInvestmentRecommendationsRule(),
            createHighValueTransactionRule(),
            createCustomerEligibilityRule(),
            createProductAvailabilityRule()
        );

        System.out.println("Rule Metadata Summary:");
        for (Rule rule : allRules) {
            System.out.println("\nRule: " + rule.getName());
            System.out.println("  ID: " + rule.getId());
            System.out.println("  Description Length: " + rule.getDescription().length() + " characters");
            System.out.println("  Condition Length: " + rule.getCondition().length() + " characters");
            System.out.println("  Contains SpEL operators: " + containsSpelOperators(rule.getCondition()));
            System.out.println("  Estimated complexity: " + estimateComplexity(rule.getCondition()));
        }
    }

    /**
     * Demonstrate rule complexity analysis.
     */
    private static void demonstrateRuleComplexityAnalysis() {
        System.out.println("\n4. Rule Complexity Analysis:");
        System.out.println("-".repeat(40));

        // Analyze all rule collections
        Map<String, List<Rule>> ruleCollections = new HashMap<>();
        ruleCollections.put("Financial", createFinancialRules());
        ruleCollections.put("Customer", createCustomerRules());
        ruleCollections.put("Product", createProductRules());

        System.out.println("Complexity Analysis by Category:");
        for (Map.Entry<String, List<Rule>> entry : ruleCollections.entrySet()) {
            String category = entry.getKey();
            List<Rule> rules = entry.getValue();

            int totalRules = rules.size();
            int simpleRules = 0;
            int complexRules = 0;
            int totalConditionLength = 0;

            for (Rule rule : rules) {
                String complexity = estimateComplexity(rule.getCondition());
                if ("Simple".equals(complexity)) {
                    simpleRules++;
                } else {
                    complexRules++;
                }
                totalConditionLength += rule.getCondition().length();
            }

            double avgConditionLength = totalRules > 0 ? (double) totalConditionLength / totalRules : 0;

            System.out.println("\n" + category + " Rules:");
            System.out.println("  Total Rules: " + totalRules);
            System.out.println("  Simple Rules: " + simpleRules + " (" + String.format("%.1f", (double) simpleRules / totalRules * 100) + "%)");
            System.out.println("  Complex Rules: " + complexRules + " (" + String.format("%.1f", (double) complexRules / totalRules * 100) + "%)");
            System.out.println("  Average Condition Length: " + String.format("%.1f", avgConditionLength) + " characters");
        }
    }

    /**
     * Create a high-value transaction rule.
     */
    public static Rule createHighValueTransactionRule() {
        return new Rule(
            "High Value Transaction",
            "#transaction.amount >= 10000 && #transaction.currency == 'USD'",
            "Identifies high-value USD transactions requiring special approval"
        );
    }

    /**
     * Create a customer eligibility rule.
     */
    public static Rule createCustomerEligibilityRule() {
        return new Rule(
            "Customer Eligibility",
            "#customer.creditScore >= 700 && #customer.accountAge >= 12 && !#customer.hasDefaultHistory",
            "Determines customer eligibility for premium services"
        );
    }

    /**
     * Create a product availability rule.
     */
    public static Rule createProductAvailabilityRule() {
        return new Rule(
            "Product Availability",
            "#product.stockLevel > 0 && #product.isActive && #product.region == #customer.region",
            "Checks if product is available for customer's region"
        );
    }

    /**
     * Check if a condition contains SpEL operators.
     */
    private static boolean containsSpelOperators(String condition) {
        return condition.contains("&&") || condition.contains("||") ||
               condition.contains("?[") || condition.contains("![") ||
               condition.contains(".contains(") || condition.contains(".size()");
    }

    /**
     * Estimate the complexity of a rule condition.
     */
    private static String estimateComplexity(String condition) {
        int complexity = 0;

        // Count logical operators
        complexity += countOccurrences(condition, "&&");
        complexity += countOccurrences(condition, "||");

        // Count collection operations
        complexity += countOccurrences(condition, "?[") * 2;
        complexity += countOccurrences(condition, "![") * 2;

        // Count method calls
        complexity += countOccurrences(condition, ".contains(");
        complexity += countOccurrences(condition, ".size()");
        complexity += countOccurrences(condition, ".isEmpty()");

        // Count comparisons
        complexity += countOccurrences(condition, ">=");
        complexity += countOccurrences(condition, "<=");
        complexity += countOccurrences(condition, ">");
        complexity += countOccurrences(condition, "<");
        complexity += countOccurrences(condition, "==");
        complexity += countOccurrences(condition, "!=");

        if (complexity <= 2) {
            return "Simple";
        } else if (complexity <= 5) {
            return "Moderate";
        } else {
            return "Complex";
        }
    }

    /**
     * Count occurrences of a substring in a string.
     */
    private static int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * Create financial rules collection.
     */
    public static List<Rule> createFinancialRules() {
        return Arrays.asList(
            createHighValueTransactionRule(),
            new Rule("Credit Limit Check", "#transaction.amount <= #customer.creditLimit", "Verify transaction within credit limit"),
            new Rule("Currency Validation", "#supportedCurrencies.contains(#transaction.currency)", "Validate supported currency"),
            new Rule("Daily Limit Check", "#customer.dailySpent + #transaction.amount <= #customer.dailyLimit", "Check daily spending limit")
        );
    }

    /**
     * Create customer rules collection.
     */
    public static List<Rule> createCustomerRules() {
        return Arrays.asList(
            createCustomerEligibilityRule(),
            new Rule("Age Verification", "#customer.age >= 18", "Verify customer is of legal age"),
            new Rule("KYC Compliance", "#customer.kycStatus == 'VERIFIED'", "Ensure KYC compliance"),
            new Rule("Account Status", "#customer.accountStatus == 'ACTIVE'", "Check active account status")
        );
    }

    /**
     * Create product rules collection.
     */
    public static List<Rule> createProductRules() {
        return Arrays.asList(
            createProductAvailabilityRule(),
            new Rule("Price Range", "#product.price >= 0 && #product.price <= 10000", "Validate product price range"),
            new Rule("Category Active", "#product.category.isActive", "Check if product category is active"),
            new Rule("Seasonal Availability", "#product.isSeasonallyAvailable(#currentDate)", "Check seasonal availability")
        );
    }
}
