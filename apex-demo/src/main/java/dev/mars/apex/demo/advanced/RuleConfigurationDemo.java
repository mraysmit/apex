package dev.mars.apex.demo.advanced;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;

import java.util.*;

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
 * Service for defining and creating business rules configurations.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service for defining and creating business rules configurations.
 * This class centralizes business rule configuration, separating it from rule processing.
 */
public class RuleConfigurationDemo {

    /**
     * Register loan approval rules with the provided configuration.
     *
     * @param config The rules engine configuration to register rules with
     */
    public static void registerLoanApprovalRules(RulesEngineConfiguration config) {
        try {
            // Rule 1: Approve loans with good credit and low debt-to-income ratio
            config.rule("LA001")
                .withCategory("loan-approval")
                .withName("approve-good-credit")
                .withCondition("#creditScore >= 700 and #debtToIncomeRatio <= 0.36")
                .withMessage("Loan approved based on good credit")
                .withDescription("Approves loans for applicants with credit score >= 700 and debt-to-income ratio <= 36%")
                .withPriority(10)
                .build();

            // Rule 2: Approve loans with excellent credit regardless of debt-to-income ratio
            config.rule("LA002")
                .withCategory("loan-approval")
                .withName("approve-excellent-credit")
                .withCondition("#creditScore >= 750")
                .withMessage("Loan approved based on excellent credit")
                .withDescription("Approves loans for applicants with credit score >= 750")
                .withPriority(5)
                .build();

            // Rule 3: Reject loans with poor credit
            config.rule("LA003")
                .withCategory("loan-approval")
                .withName("reject-poor-credit")
                .withCondition("#creditScore < 620")
                .withMessage("Loan rejected due to poor credit")
                .withDescription("Rejects loans for applicants with credit score < 620")
                .withPriority(20)
                .build();

            // Rule 4: Reject loans with high debt-to-income ratio
            config.rule("LA004")
                .withCategory("loan-approval")
                .withName("reject-high-dti")
                .withCondition("#debtToIncomeRatio > 0.43")
                .withMessage("Loan rejected due to high debt-to-income ratio")
                .withDescription("Rejects loans for applicants with debt-to-income ratio > 43%")
                .withPriority(30)
                .build();

            // Rule 5: Refer loans with moderate credit and acceptable debt-to-income ratio
            config.rule("LA005")
                .withCategory("loan-approval")
                .withName("refer-moderate-credit")
                .withCondition("#creditScore >= 620 and #creditScore < 700 and #debtToIncomeRatio <= 0.43")
                .withMessage("Loan referred for manual review")
                .withDescription("Refers loans for manual review for applicants with credit score between 620 and 700 and debt-to-income ratio <= 43%")
                .withPriority(40)
                .build();

            // Rule 6: Default rule - reject all other loans
            config.rule("LA006")
                .withCategory("loan-approval")
                .withName("reject-default")
                .withCondition("true")
                .withMessage("Loan rejected based on default criteria")
                .withDescription("Default rule that rejects all loans that don't meet other criteria")
                .withPriority(100)
                .build();
        } catch (Exception e) {
            System.err.println("Error registering loan approval rules: " + e.getMessage());
        }
    }

    /**
     * Register discount rules with the provided configuration.
     *
     * @param config The rules engine configuration to register rules with
     */
    public static void registerDiscountRules(RulesEngineConfiguration config) {
        try {
            // Define rule parameters
            double largeOrderThreshold = 1000.0;
            int loyalCustomerYears = 5;
            int newCustomerYears = 0;

            // Rule 1: 15% discount for orders over $1000
            createRule(
                    config,
                    "OD001", // Rule ID
                    "order-discount", // Rule category
                    "large-order-discount", // Rule name
                    "#orderTotal > " + largeOrderThreshold, // Parameterized condition
                    "15% discount applied for large order", // Message
                    "Provides a 15% discount for orders exceeding $1000", // Description
                    10 // Priority
            );

            // Rule 2: 10% discount for loyal customers (over 5 years)
            createRule(
                    config,
                    "OD002", // Rule ID
                    "order-discount", // Rule category
                    "loyalty-discount", // Rule name
                    "#customerYears > " + loyalCustomerYears, // Parameterized condition
                    "10% discount applied for customer loyalty", // Message
                    "Rewards loyal customers who have been with us for more than 5 years", // Description
                    20 // Priority
            );

            // Rule 3: 5% discount for first-time customers
            createRule(
                    config,
                    "OD003", // Rule ID
                    "order-discount", // Rule category
                    "new-customer-discount", // Rule name
                    "#customerYears == " + newCustomerYears, // Parameterized condition
                    "5% discount applied for new customers", // Message
                    "Welcomes new customers with a 5% discount on their first order", // Description
                    30 // Priority
            );

            // Rule 4: No discount for standard orders
            createRule(
                    config,
                    "OD004", // Rule ID
                    "order-discount", // Rule category
                    "no-discount", // Rule name
                    "true", // Condition
                    "No discount applied", // Message
                    "Default rule when no other discount rules apply", // Description
                    100 // Priority
            );
        } catch (Exception e) {
            System.err.println("Error registering discount rules: " + e.getMessage());
        }
    }

    /**
     * Register combined rules demonstration rules with the provided configuration.
     *
     * @param config The rules engine configuration to register rules with
     */
    public static void registerCombinedRulesDemoRules(RulesEngineConfiguration config) {
        try {
            // Create a new category for combined rules
            String combinedCategory = "combined-rules";

            // Register some simple rules for demonstration
            createRule(
                    config,
                    "CR001", // Rule ID
                    combinedCategory, // Rule category
                    "high-value-order", // Rule name
                    "#orderTotal > 500", // Condition
                    "High-value order detected", // Message
                    "Identifies orders with a total value exceeding $500", // Description
                    10 // Priority
            );

            createRule(
                    config,
                    "CR002", // Rule ID
                    combinedCategory, // Rule category
                    "loyal-customer", // Rule name
                    "#customerYears > 3", // Condition
                    "Loyal customer detected", // Message
                    "Identifies customers who have been with us for more than 3 years", // Description
                    20 // Priority
            );

            createRule(
                    config,
                    "CR003", // Rule ID
                    combinedCategory, // Rule category
                    "large-quantity", // Rule name
                    "#quantity > 10", // Condition
                    "Large quantity order detected", // Message
                    "Identifies orders with more than 10 items", // Description
                    30 // Priority
            );

            // Combine rules with AND - high value AND loyal customer
            combineRulesWithAnd(
                    config,
                    "CR004", // Rule ID
                    combinedCategory, // Rule category
                    "premium-loyal-customer", // Rule name
                    Arrays.asList("CR001", "CR002"), // Rules to combine
                    "Premium loyal customer detected", // Message
                    "Identifies high-value orders from loyal customers", // Description
                    5 // Priority (higher priority than individual rules)
            );

            // Combine rules with OR - high value OR large quantity
            combineRulesWithOr(
                    config,
                    "CR005", // Rule ID
                    combinedCategory, // Rule category
                    "special-handling-required", // Rule name
                    Arrays.asList("CR001", "CR003"), // Rules to combine
                    "Special handling required", // Message
                    "Identifies orders that require special handling due to high value or large quantity", // Description
                    15 // Priority
            );
        } catch (Exception e) {
            System.err.println("Error registering combined rules demo rules: " + e.getMessage());
        }
    }

    /**
     * Register rule group demonstration rules with the provided configuration.
     *
     * @param config The rules engine configuration to register rules with
     */
    public static void registerRuleGroupDemoRules(RulesEngineConfiguration config) {
        try {
            // Create categories for demonstration
            String investmentCategory = "investment-rules";
            String riskCategory = "risk-assessment";
            String complianceCategory = "compliance";

            // Register some basic rules in different categories and store them in variables
            Rule ir001 = createRule(
                    config,
                    "IR001", // Rule ID
                    investmentCategory, // Rule category
                    "high-value-investment", // Rule name
                    "#investmentAmount > 100000", // Condition
                    "High-value investment detected", // Message
                    "Identifies investments with a value exceeding $100,000", // Description
                    10 // Priority
            );

            Rule ir002 = createRule(
                    config,
                    "IR002", // Rule ID
                    investmentCategory, // Rule category
                    "retirement-account", // Rule name
                    "#accountType == 'retirement'", // Condition
                    "Retirement account detected", // Message
                    "Identifies retirement accounts", // Description
                    20 // Priority
            );

            // Register a rule in multiple categories
            Set<String> multiCategories = new HashSet<>(Arrays.asList(riskCategory, complianceCategory));
            Rule ra001 = createRuleWithCategories(
                    config,
                    "RA001", // Rule ID
                    multiCategories, // Multiple categories
                    "high-risk-client", // Rule name
                    "#clientRiskScore > 7", // Condition
                    "High-risk client detected", // Message
                    "Identifies clients with high risk scores", // Description
                    5 // Priority
            );

            Rule ra002 = createRule(
                    config,
                    "RA002", // Rule ID
                    riskCategory, // Rule category
                    "volatile-market", // Rule name
                    "#marketVolatility > 0.2", // Condition
                    "Volatile market conditions detected", // Message
                    "Identifies periods of high market volatility", // Description
                    15 // Priority
            );

            Rule co001 = createRule(
                    config,
                    "CO001", // Rule ID
                    complianceCategory, // Rule category
                    "kyc-verified", // Rule name
                    "!#kycVerified", // Condition
                    "KYC verification required", // Message
                    "Identifies clients who need KYC verification", // Description
                    25 // Priority
            );

            // Create a rule group with AND operator
            RuleGroup ruleGroup1 = createRuleGroupWithAnd(
                    config,
                    "RG001", // Group ID
                    investmentCategory, // Group category
                    "retirement-investment-checks", // Group name
                    "Checks for retirement investment criteria", // Description
                    50 // Priority
            );

            // Add rules to the group with sequence numbers, using the stored rule variables
            if (ir001 != null) {
                ruleGroup1.addRule(ir001, 1);
            } else {
                System.err.println("Cannot add rule IR001 to group 'retirement-investment-checks', rule not found");
            }

            if (ir002 != null) {
                ruleGroup1.addRule(ir002, 2);
            } else {
                System.err.println("Cannot add rule IR002 to group 'retirement-investment-checks', rule not found");
            }

            // Create a rule group with OR operator
            RuleGroup ruleGroup2 = createRuleGroupWithOr(
                    config,
                    "RG002", // Group ID
                    riskCategory, // Group category
                    "risk-assessment-checks", // Group name
                    "Checks for various risk factors", // Description
                    60 // Priority
            );

            // Add rules to the group with sequence numbers, using the stored rule variables
            if (ra001 != null) {
                ruleGroup2.addRule(ra001, 1);
            } else {
                System.err.println("Cannot add rule RA001 to group 'risk-assessment-checks', rule not found");
            }

            if (ra002 != null) {
                ruleGroup2.addRule(ra002, 2);
            } else {
                System.err.println("Cannot add rule RA002 to group 'risk-assessment-checks', rule not found");
            }

            // Create a multi-category rule group
            Set<String> groupCategories = new HashSet<>(Arrays.asList(riskCategory, complianceCategory));
            RuleGroup ruleGroup3 = createRuleGroupWithAnd(
                    config,
                    "RG003", // Group ID
                    groupCategories, // Multiple categories
                    "compliance-checks", // Group name
                    "Checks for compliance requirements", // Description
                    70 // Priority
            );

            // Add rules to the group with sequence numbers, using the stored rule variables
            if (co001 != null) {
                ruleGroup3.addRule(co001, 1);
            } else {
                System.err.println("Cannot add rule CO001 to group 'compliance-checks', rule not found");
            }

            if (ra001 != null) {
                ruleGroup3.addRule(ra001, 2);
            } else {
                System.err.println("Cannot add rule RA001 to group 'compliance-checks', rule not found");
            }
        } catch (Exception e) {
            System.err.println("Error registering rule group demo rules: " + e.getMessage());
        }
    }

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
     * Helper method to create and register a rule.
     */
    private static Rule createRule(RulesEngineConfiguration config, String id, String category, String name,
                                  String condition, String message, String description, int priority) {
        return config.rule(id)
            .withCategory(category)
            .withName(name)
            .withCondition(condition)
            .withMessage(message)
            .withDescription(description)
            .withPriority(priority)
            .build();
    }

    /**
     * Helper method to create and register a rule with multiple categories.
     */
    private static Rule createRuleWithCategories(RulesEngineConfiguration config, String id, Set<String> categoryNames, String name,
                                               String condition, String message, String description, int priority) {
        return config.rule(id)
            .withCategoryNames(categoryNames)
            .withName(name)
            .withCondition(condition)
            .withMessage(message)
            .withDescription(description)
            .withPriority(priority)
            .build();
    }

    /**
     * Helper method to create a rule group with AND operator.
     */
    private static RuleGroup createRuleGroupWithAnd(RulesEngineConfiguration config, String id, String category, String name,
                                                  String description, int priority) {
        return config.createRuleGroupWithAnd(id, category, name, description, priority);
    }

    /**
     * Helper method to create a rule group with AND operator and multiple categories.
     */
    private static RuleGroup createRuleGroupWithAnd(RulesEngineConfiguration config, String id, Set<String> categoryNames, String name,
                                                  String description, int priority) {
        return config.createRuleGroupWithAnd(id, categoryNames, name, description, priority);
    }

    /**
     * Helper method to create a rule group with OR operator.
     */
    private static RuleGroup createRuleGroupWithOr(RulesEngineConfiguration config, String id, String category, String name,
                                                 String description, int priority) {
        return config.createRuleGroupWithOr(id, category, name, description, priority);
    }



    /**
     * Helper method to combine rules with an AND operator.
     * 
     * @param config The rules engine configuration
     * @param id The ID of the new combined rule
     * @param category The category of the new combined rule
     * @param name The name of the new combined rule
     * @param ruleIds The IDs of the rules to combine
     * @param message The message for the new combined rule
     * @param description The description of the new combined rule
     * @param priority The priority of the new combined rule
     * @return The ID of the new combined rule, or null if any of the rules don't exist
     */
    private static String combineRulesWithAnd(RulesEngineConfiguration config, String id, String category, String name,
                                            List<String> ruleIds, String message, String description, int priority) {
        // Check if any rule IDs are null or empty
        if (ruleIds == null || ruleIds.isEmpty()) {
            System.err.println("Cannot combine rules with AND: no rule IDs provided");
            return null;
        }

        // Check if all rules exist before combining
        for (String ruleId : ruleIds) {
            Rule rule = config.getRuleById(ruleId);
            if (rule == null) {
                System.err.println("Cannot combine rules with AND: rule with ID '" + ruleId + "' not found");
                return null;
            }
        }

        String combinedRuleId = config.combineRulesWithAnd(id, category, name, ruleIds, message, description, priority);
        if (combinedRuleId == null) {
            System.err.println("Failed to combine rules with AND: " + ruleIds);
        }
        return combinedRuleId;
    }

    /**
     * Helper method to combine rules with an OR operator.
     * 
     * @param config The rules engine configuration
     * @param id The ID of the new combined rule
     * @param category The category of the new combined rule
     * @param name The name of the new combined rule
     * @param ruleIds The IDs of the rules to combine
     * @param message The message for the new combined rule
     * @param description The description of the new combined rule
     * @param priority The priority of the new combined rule
     * @return The ID of the new combined rule, or null if any of the rules don't exist
     */
    private static String combineRulesWithOr(RulesEngineConfiguration config, String id, String category, String name,
                                           List<String> ruleIds, String message, String description, int priority) {
        // Check if any rule IDs are null or empty
        if (ruleIds == null || ruleIds.isEmpty()) {
            System.err.println("Cannot combine rules with OR: no rule IDs provided");
            return null;
        }

        // Check if all rules exist before combining
        for (String ruleId : ruleIds) {
            Rule rule = config.getRuleById(ruleId);
            if (rule == null) {
                System.err.println("Cannot combine rules with OR: rule with ID '" + ruleId + "' not found");
                return null;
            }
        }

        String combinedRuleId = config.combineRulesWithOr(id, category, name, ruleIds, message, description, priority);
        if (combinedRuleId == null) {
            System.err.println("Failed to combine rules with OR: " + ruleIds);
        }
        return combinedRuleId;
    }
}
