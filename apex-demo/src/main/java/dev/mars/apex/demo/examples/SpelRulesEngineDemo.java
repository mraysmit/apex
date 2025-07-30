package dev.mars.apex.demo.examples;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.RuleConfigurationService;
import dev.mars.apex.core.service.engine.RuleEngineService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.util.*;
import java.util.List;

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
 * This class demonstrates the SpEL Rules Engine functionality,
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * This class demonstrates the SpEL Rules Engine functionality,
 * particularly focusing on rule groups and nested rule chaining.
 */
public class SpelRulesEngineDemo {

    /**
     * Main demonstration method that shows all rule engine capabilities.
     */
    public static void main(String[] args) {
        System.out.println("=== SpEL Rules Engine Comprehensive Demo ===");
        System.out.println("Demonstrating rule groups, chaining, and nested dependencies\n");

        // Run all demonstrations
        demoRuleGroupsDemo();
        demoNestedRulesAndChaining();
    }

    /**
     * Demonstrates rule groups functionality.
     * This method is called by the SpelRuleGroupsTest.
     */
    public static void demoRuleGroupsDemo() {
        System.out.println("\n=== Demonstrating Rule Groups ===");

        // Create configuration and rules engine
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);

        // Create rule configuration service
        RuleConfigurationService ruleConfigService = new RuleConfigurationService(config);

        // Register rule group demo rules directly
        registerRuleGroupDemoRules(ruleConfigService);

        // Create test scenarios for rule groups
        Map<String, Map<String, Object>> scenarios = createTestScenarios();

        // Execute each scenario
        for (Map.Entry<String, Map<String, Object>> scenario : scenarios.entrySet()) {
            String scenarioName = scenario.getKey();
            Map<String, Object> facts = scenario.getValue();

            System.out.println("\nProcessing scenario for: " + scenarioName);

            // Execute rules for each category
            executeRulesForCategory(engine, "investment-rules", facts);
            executeRulesForCategory(engine, "risk-assessment", facts);
            executeRulesForCategory(engine, "compliance", facts);
        }
    }

    /**
     * Register rule group demonstration rules.
     *
     * @param ruleConfigService The rule configuration service to register rules with
     */
    private static void registerRuleGroupDemoRules(RuleConfigurationService ruleConfigService) {
        try {
            // Create categories for demonstration
            String investmentCategory = "investment-rules";
            String riskCategory = "risk-assessment";
            String complianceCategory = "compliance";

            // Register some basic rules in different categories and store them in variables
            Rule ir001 = ruleConfigService.registerRule(
                    "IR001", // Rule ID
                    investmentCategory, // Rule category
                    "high-value-investment", // Rule name
                    "#investmentAmount > 100000", // Condition
                    "High-value investment detected", // Message
                    "Identifies investments with a value exceeding $100,000", // Description
                    10 // Priority
            );

            Rule ir002 = ruleConfigService.registerRule(
                    "IR002", // Rule ID
                    investmentCategory, // Rule category
                    "retirement-account", // Rule name
                    "#accountType == 'retirement'", // Condition
                    "Retirement account detected", // Message
                    "Identifies retirement accounts", // Description
                    20 // Priority
            );

            // Register a rule in multiple categories (for simplicity, we'll just use one category here)
            Rule ra001 = ruleConfigService.registerRule(
                    "RA001", // Rule ID
                    riskCategory, // Rule category
                    "high-risk-client", // Rule name
                    "#clientRiskScore > 7", // Condition
                    "High-risk client detected", // Message
                    "Identifies clients with high risk scores", // Description
                    5 // Priority
            );

            Rule ra002 = ruleConfigService.registerRule(
                    "RA002", // Rule ID
                    riskCategory, // Rule category
                    "volatile-market", // Rule name
                    "#marketVolatility > 0.2", // Condition
                    "Volatile market conditions detected", // Message
                    "Identifies periods of high market volatility", // Description
                    15 // Priority
            );

            Rule co001 = ruleConfigService.registerRule(
                    "CO001", // Rule ID
                    complianceCategory, // Rule category
                    "kyc-verified", // Rule name
                    "!#kycVerified", // Condition
                    "KYC verification required", // Message
                    "Identifies clients who need KYC verification", // Description
                    25 // Priority
            );

            // Create a rule group with AND operator
            RuleGroup ruleGroup1 = ruleConfigService.registerRuleGroupWithAnd(
                    "RG001", // Group ID
                    investmentCategory, // Group category
                    "retirement-investment-checks", // Group name
                    "Checks for retirement investment criteria", // Description
                    50 // Priority
            );

            // Add rules to the group with sequence numbers
            if (ir001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup1, ir001, 1);
            } else {
                System.err.println("Cannot add rule IR001 to group 'retirement-investment-checks', rule not found");
            }

            if (ir002 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup1, ir002, 2);
            } else {
                System.err.println("Cannot add rule IR002 to group 'retirement-investment-checks', rule not found");
            }

            // Create a rule group with OR operator
            RuleGroup ruleGroup2 = ruleConfigService.registerRuleGroupWithOr(
                    "RG002", // Group ID
                    riskCategory, // Group category
                    "risk-assessment-checks", // Group name
                    "Checks for various risk factors", // Description
                    60 // Priority
            );

            // Add rules to the group with sequence numbers
            if (ra001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup2, ra001, 1);
            } else {
                System.err.println("Cannot add rule RA001 to group 'risk-assessment-checks', rule not found");
            }

            if (ra002 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup2, ra002, 2);
            } else {
                System.err.println("Cannot add rule RA002 to group 'risk-assessment-checks', rule not found");
            }

            // Create a multi-category rule group
            RuleGroup ruleGroup3 = ruleConfigService.registerRuleGroupWithAnd(
                    "RG003", // Group ID
                    complianceCategory, // Group category
                    "compliance-checks", // Group name
                    "Checks for compliance requirements", // Description
                    70 // Priority
            );

            // Add rules to the group with sequence numbers
            if (co001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup3, co001, 1);
            } else {
                System.err.println("Cannot add rule CO001 to group 'compliance-checks', rule not found");
            }

            // Also add the high-risk client rule to the compliance checks
            if (ra001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup3, ra001, 2);
            } else {
                System.err.println("Cannot add rule RA001 to group 'compliance-checks', rule not found");
            }
        } catch (Exception e) {
            System.err.println("Error registering rule group demo rules: " + e.getMessage());
        }
    }

    /**
     * Execute rules for a specific category and print the results.
     */
    private static void executeRulesForCategory(RulesEngine engine, String category, Map<String, Object> facts) {
        RuleResult result = engine.executeRulesForCategory(category, facts);
        if (result.isTriggered()) {
            System.out.println("Category: " + category + ", rule triggered: " + result.getRuleName());
            System.out.println("Result: " + result.getMessage());
        }
    }

    /**
     * Create test scenarios for rule groups.
     */
    private static Map<String, Map<String, Object>> createTestScenarios() {
        Map<String, Map<String, Object>> scenarios = new HashMap<>();

        // Scenario 1: High-value retirement investment
        Map<String, Object> scenario1 = new HashMap<>();
        scenario1.put("investmentAmount", 150000);
        scenario1.put("accountType", "retirement");
        scenario1.put("clientRiskScore", 5);
        scenario1.put("marketVolatility", 0.15);
        scenario1.put("kycVerified", true);
        scenarios.put("High-value retirement investment", scenario1);

        // Scenario 2: High-risk client with volatile market
        Map<String, Object> scenario2 = new HashMap<>();
        scenario2.put("investmentAmount", 75000);
        scenario2.put("accountType", "standard");
        scenario2.put("clientRiskScore", 8);
        scenario2.put("marketVolatility", 0.25);
        scenario2.put("kycVerified", true);
        scenarios.put("High-risk client with volatile market", scenario2);

        // Scenario 3: KYC verification required
        Map<String, Object> scenario3 = new HashMap<>();
        scenario3.put("investmentAmount", 50000);
        scenario3.put("accountType", "standard");
        scenario3.put("clientRiskScore", 4);
        scenario3.put("marketVolatility", 0.1);
        scenario3.put("kycVerified", false);
        scenarios.put("KYC verification required", scenario3);

        return scenarios;
    }

    /**
     * Demonstrates nested rules and rule chaining scenarios.
     * This shows common patterns where rules depend on results of previous rules.
     */
    public static void demoNestedRulesAndChaining() {
        System.out.println("\n=== NESTED RULES AND RULE CHAINING DEMO ===");
        System.out.println("Demonstrating rule dependencies and cascading execution patterns\n");

        // Create services
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        RuleEngineService ruleEngineService = new RuleEngineService(evaluatorService);

        // Demonstrate different chaining patterns
        demonstrateConditionalChaining(ruleEngineService, evaluatorService);
        demonstrateSequentialDependency(ruleEngineService, evaluatorService);
        demonstrateResultBasedRouting(ruleEngineService, evaluatorService);
        demonstrateAccumulativeChaining(ruleEngineService, evaluatorService);
        demonstrateComplexFinancialWorkflow(ruleEngineService, evaluatorService);
        demonstrateFluentRuleBuilder(ruleEngineService, evaluatorService);
    }

    /**
     * Pattern 1: Conditional Chaining - Execute rule B only if rule A triggers.
     */
    private static void demonstrateConditionalChaining(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("1. CONDITIONAL CHAINING PATTERN");
        System.out.println("   Execute Rule B only if Rule A triggers");
        System.out.println("   " + "=".repeat(50));

        // Create test scenarios
        List<Map<String, Object>> scenarios = Arrays.asList(
            Map.of("customerType", "PREMIUM", "transactionAmount", new BigDecimal("150000"), "accountAge", 5),
            Map.of("customerType", "STANDARD", "transactionAmount", new BigDecimal("50000"), "accountAge", 2)
        );

        for (int i = 0; i < scenarios.size(); i++) {
            Map<String, Object> context = scenarios.get(i);
            System.out.println("\n   Scenario " + (i + 1) + ": " + context);

            // Rule A: Check if customer qualifies for high-value processing
            Rule ruleA = new Rule(
                "HighValueCustomerCheck",
                "#customerType == 'PREMIUM' && #transactionAmount > 100000",
                "Customer qualifies for high-value processing"
            );

            // Execute Rule A
            List<RuleResult> resultsA = ruleEngineService.evaluateRules(Arrays.asList(ruleA),
                createEvaluationContext(context));
            RuleResult resultA = resultsA.get(0);

            System.out.println("   → Rule A (" + ruleA.getName() + "): " +
                (resultA.isTriggered() ? "TRIGGERED" : "NOT TRIGGERED"));

            // Conditional execution of Rule B
            if (resultA.isTriggered()) {
                System.out.println("   → Rule A triggered, executing Rule B...");

                // Rule B: Enhanced due diligence check (only executed if Rule A triggers)
                Rule ruleB = new Rule(
                    "EnhancedDueDiligenceCheck",
                    "#accountAge >= 3",
                    "Enhanced due diligence check passed"
                );

                List<RuleResult> resultsB = ruleEngineService.evaluateRules(Arrays.asList(ruleB),
                    createEvaluationContext(context));
                RuleResult resultB = resultsB.get(0);

                System.out.println("   → Rule B (" + ruleB.getName() + "): " +
                    (resultB.isTriggered() ? "PASSED" : "FAILED"));

                if (resultB.isTriggered()) {
                    System.out.println("   ✓ APPROVED: High-value transaction approved with enhanced checks");
                } else {
                    System.out.println("   ✗ REJECTED: Enhanced due diligence failed");
                }
            } else {
                System.out.println("   → Rule A not triggered, skipping Rule B");
                System.out.println("   ✓ APPROVED: Standard processing applied");
            }
        }
    }

    /**
     * Pattern 2: Sequential Dependency - Each rule uses results from the previous rule.
     */
    private static void demonstrateSequentialDependency(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("\n\n2. SEQUENTIAL DEPENDENCY PATTERN");
        System.out.println("   Each rule builds upon results from the previous rule");
        System.out.println("   " + "=".repeat(50));

        Map<String, Object> context = new HashMap<>();
        context.put("baseAmount", new BigDecimal("100000"));
        context.put("customerTier", "GOLD");
        context.put("region", "US");

        System.out.println("\n   Initial Context: " + context);

        // Rule 1: Calculate base discount
        Rule rule1 = new Rule(
            "BaseDiscountCalculation",
            "#customerTier == 'GOLD' ? 0.15 : (#customerTier == 'SILVER' ? 0.10 : 0.05)",
            "Calculate base discount based on customer tier"
        );

        StandardEvaluationContext evalContext = createEvaluationContext(context);
        Double baseDiscount = evaluatorService.evaluate(rule1.getCondition(), evalContext, Double.class);

        // Add result to context for next rule
        context.put("baseDiscount", baseDiscount);
        System.out.println("   → Rule 1 Result: Base discount = " + (baseDiscount * 100) + "%");

        // Rule 2: Apply regional multiplier (depends on Rule 1 result)
        Rule rule2 = new Rule(
            "RegionalMultiplierCalculation",
            "#region == 'US' ? #baseDiscount * 1.2 : (#region == 'EU' ? #baseDiscount * 1.1 : #baseDiscount)",
            "Apply regional multiplier to base discount"
        );

        evalContext = createEvaluationContext(context);
        Double finalDiscount = evaluatorService.evaluate(rule2.getCondition(), evalContext, Double.class);

        // Add result to context for next rule
        context.put("finalDiscount", finalDiscount);
        System.out.println("   → Rule 2 Result: Final discount = " + (finalDiscount * 100) + "%");

        // Rule 3: Calculate final amount (depends on Rule 2 result)
        Rule rule3 = new Rule(
            "FinalAmountCalculation",
            "#baseAmount * (1 - #finalDiscount)",
            "Calculate final amount after all discounts"
        );

        evalContext = createEvaluationContext(context);
        BigDecimal finalAmount = evaluatorService.evaluate(rule3.getCondition(), evalContext, BigDecimal.class);

        System.out.println("   → Rule 3 Result: Final amount = $" + finalAmount);
        System.out.println("   ✓ COMPLETE: Sequential processing chain completed");
        System.out.println("     Original: $" + context.get("baseAmount") +
                         " → Final: $" + finalAmount +
                         " (Saved: $" + ((BigDecimal)context.get("baseAmount")).subtract(finalAmount) + ")");
    }

    /**
     * Pattern 3: Result-Based Routing - Route to different rule sets based on previous results.
     */
    private static void demonstrateResultBasedRouting(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("\n\n3. RESULT-BASED ROUTING PATTERN");
        System.out.println("   Route to different rule sets based on previous results");
        System.out.println("   " + "=".repeat(50));

        List<Map<String, Object>> testCases = Arrays.asList(
            Map.of("riskScore", 85, "transactionAmount", new BigDecimal("500000"), "customerHistory", "EXCELLENT"),
            Map.of("riskScore", 45, "transactionAmount", new BigDecimal("25000"), "customerHistory", "GOOD"),
            Map.of("riskScore", 15, "transactionAmount", new BigDecimal("75000"), "customerHistory", "AVERAGE")
        );

        for (int i = 0; i < testCases.size(); i++) {
            Map<String, Object> context = new HashMap<>(testCases.get(i));
            System.out.println("\n   Test Case " + (i + 1) + ": " + context);

            // Router Rule: Determine processing path
            Rule routerRule = new Rule(
                "ProcessingPathRouter",
                "#riskScore > 70 ? 'HIGH_RISK' : (#riskScore > 30 ? 'MEDIUM_RISK' : 'LOW_RISK')",
                "Determine processing path based on risk score"
            );

            StandardEvaluationContext evalContext = createEvaluationContext(context);
            String processingPath = evaluatorService.evaluate(routerRule.getCondition(), evalContext, String.class);

            System.out.println("   → Router Decision: " + processingPath + " processing path");

            // Route to appropriate rule set based on result
            switch (processingPath) {
                case "HIGH_RISK":
                    executeHighRiskRules(context, ruleEngineService, evaluatorService);
                    break;
                case "MEDIUM_RISK":
                    executeMediumRiskRules(context, ruleEngineService, evaluatorService);
                    break;
                case "LOW_RISK":
                    executeLowRiskRules(context, ruleEngineService, evaluatorService);
                    break;
            }
        }
    }

    private static void executeHighRiskRules(Map<String, Object> context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("   → Executing HIGH RISK rule set:");

        // High risk requires multiple approvals
        Rule rule1 = new Rule("ManagerApprovalRequired", "#transactionAmount > 100000", "Manager approval required");
        Rule rule2 = new Rule("ComplianceReviewRequired", "#riskScore > 80", "Compliance review required");
        Rule rule3 = new Rule("CustomerHistoryCheck", "#customerHistory == 'EXCELLENT'", "Customer history must be excellent");

        List<Rule> highRiskRules = Arrays.asList(rule1, rule2, rule3);
        List<RuleResult> results = ruleEngineService.evaluateRules(highRiskRules, createEvaluationContext(context));

        boolean allPassed = results.stream().allMatch(RuleResult::isTriggered);
        for (RuleResult result : results) {
            System.out.println("     • " + result.getRuleName() + ": " + (result.isTriggered() ? "REQUIRED" : "NOT REQUIRED"));
        }
        System.out.println("   ✓ HIGH RISK RESULT: " + (allPassed ? "APPROVED with multiple approvals" : "REQUIRES MANUAL REVIEW"));
    }

    private static void executeMediumRiskRules(Map<String, Object> context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("   → Executing MEDIUM RISK rule set:");

        Rule rule1 = new Rule("AutoApprovalLimit", "#transactionAmount <= 50000", "Within auto-approval limit");
        Rule rule2 = new Rule("CustomerHistoryGood", "#customerHistory != 'POOR'", "Customer history acceptable");

        List<Rule> mediumRiskRules = Arrays.asList(rule1, rule2);
        List<RuleResult> results = ruleEngineService.evaluateRules(mediumRiskRules, createEvaluationContext(context));

        boolean allPassed = results.stream().allMatch(RuleResult::isTriggered);
        for (RuleResult result : results) {
            System.out.println("     • " + result.getRuleName() + ": " + (result.isTriggered() ? "PASSED" : "FAILED"));
        }
        System.out.println("   ✓ MEDIUM RISK RESULT: " + (allPassed ? "AUTO-APPROVED" : "REQUIRES SUPERVISOR APPROVAL"));
    }

    private static void executeLowRiskRules(Map<String, Object> context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("   → Executing LOW RISK rule set:");

        Rule rule1 = new Rule("BasicValidation", "#transactionAmount > 0", "Basic amount validation");

        List<RuleResult> results = ruleEngineService.evaluateRules(Arrays.asList(rule1), createEvaluationContext(context));
        RuleResult result = results.get(0);

        System.out.println("     • " + result.getRuleName() + ": " + (result.isTriggered() ? "PASSED" : "FAILED"));
        System.out.println("   ✓ LOW RISK RESULT: FAST-TRACK APPROVED");
    }

    /**
     * Pattern 4: Accumulative Chaining - Build up a score/result across multiple rules.
     */
    private static void demonstrateAccumulativeChaining(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("\n\n4. ACCUMULATIVE CHAINING PATTERN");
        System.out.println("   Build up a score/result across multiple rules");
        System.out.println("   " + "=".repeat(50));

        Map<String, Object> context = new HashMap<>();
        context.put("creditScore", 750);
        context.put("annualIncome", new BigDecimal("85000"));
        context.put("employmentYears", 8);
        context.put("existingDebt", new BigDecimal("25000"));
        context.put("loanAmount", new BigDecimal("300000"));
        context.put("totalScore", 0); // Initialize accumulator

        System.out.println("\n   Loan Application: " + context);
        System.out.println("   → Building creditworthiness score through rule chain...");

        // Rule 1: Credit Score Component
        Rule rule1 = new Rule(
            "CreditScoreEvaluation",
            "#creditScore >= 700 ? 25 : (#creditScore >= 650 ? 15 : (#creditScore >= 600 ? 10 : 0))",
            "Credit score component"
        );

        StandardEvaluationContext evalContext = createEvaluationContext(context);
        Integer creditScorePoints = evaluatorService.evaluate(rule1.getCondition(), evalContext, Integer.class);
        context.put("totalScore", (Integer)context.get("totalScore") + creditScorePoints);

        System.out.println("   → Rule 1 (Credit Score): +" + creditScorePoints + " points (Total: " + context.get("totalScore") + ")");

        // Rule 2: Income Component (depends on accumulated score)
        Rule rule2 = new Rule(
            "IncomeEvaluation",
            "#annualIncome >= 80000 ? 20 : (#annualIncome >= 60000 ? 15 : (#annualIncome >= 40000 ? 10 : 0))",
            "Income component"
        );

        evalContext = createEvaluationContext(context);
        Integer incomePoints = evaluatorService.evaluate(rule2.getCondition(), evalContext, Integer.class);
        context.put("totalScore", (Integer)context.get("totalScore") + incomePoints);

        System.out.println("   → Rule 2 (Income): +" + incomePoints + " points (Total: " + context.get("totalScore") + ")");

        // Rule 3: Employment Stability Component
        Rule rule3 = new Rule(
            "EmploymentStabilityEvaluation",
            "#employmentYears >= 5 ? 15 : (#employmentYears >= 2 ? 10 : 5)",
            "Employment stability component"
        );

        evalContext = createEvaluationContext(context);
        Integer employmentPoints = evaluatorService.evaluate(rule3.getCondition(), evalContext, Integer.class);
        context.put("totalScore", (Integer)context.get("totalScore") + employmentPoints);

        System.out.println("   → Rule 3 (Employment): +" + employmentPoints + " points (Total: " + context.get("totalScore") + ")");

        // Rule 4: Debt-to-Income Ratio (negative points possible)
        Rule rule4 = new Rule(
            "DebtToIncomeEvaluation",
            "(#existingDebt.doubleValue() / #annualIncome.doubleValue()) < 0.2 ? 10 : " +
            "((#existingDebt.doubleValue() / #annualIncome.doubleValue()) < 0.4 ? 0 : -10)",
            "Debt-to-income ratio component"
        );

        evalContext = createEvaluationContext(context);
        Integer debtRatioPoints = evaluatorService.evaluate(rule4.getCondition(), evalContext, Integer.class);
        context.put("totalScore", (Integer)context.get("totalScore") + debtRatioPoints);

        System.out.println("   → Rule 4 (Debt Ratio): " + (debtRatioPoints >= 0 ? "+" : "") + debtRatioPoints + " points (Total: " + context.get("totalScore") + ")");

        // Final Decision Rule (depends on accumulated score)
        Rule finalRule = new Rule(
            "LoanDecision",
            "#totalScore >= 60 ? 'APPROVED' : (#totalScore >= 40 ? 'CONDITIONAL' : 'DENIED')",
            "Final loan decision based on total score"
        );

        evalContext = createEvaluationContext(context);
        String decision = evaluatorService.evaluate(finalRule.getCondition(), evalContext, String.class);

        System.out.println("   → Final Decision Rule: " + decision + " (Score: " + context.get("totalScore") + "/100)");

        String outcome;
        switch (decision) {
            case "APPROVED":
                outcome = "✓ LOAN APPROVED - Excellent creditworthiness";
                break;
            case "CONDITIONAL":
                outcome = "⚠ CONDITIONAL APPROVAL - Additional documentation required";
                break;
            default:
                outcome = "✗ LOAN DENIED - Insufficient creditworthiness";
        }
        System.out.println("   " + outcome);
    }

    /**
     * Pattern 5: Complex Financial Workflow - Real-world nested rule scenario.
     */
    private static void demonstrateComplexFinancialWorkflow(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("\n\n5. COMPLEX FINANCIAL WORKFLOW PATTERN");
        System.out.println("   Real-world nested rule scenario for trade processing");
        System.out.println("   " + "=".repeat(50));

        Map<String, Object> context = new HashMap<>();
        context.put("tradeType", "DERIVATIVE");
        context.put("notionalAmount", new BigDecimal("5000000"));
        context.put("counterparty", "BANK_A");
        context.put("maturityDays", 180);
        context.put("clientTier", "INSTITUTIONAL");
        context.put("marketVolatility", 0.25);

        System.out.println("\n   Trade Details: " + context);
        System.out.println("   → Processing through nested validation workflow...");

        // Stage 1: Pre-validation
        System.out.println("\n   STAGE 1: PRE-VALIDATION");
        Rule preValidationRule = new Rule(
            "PreValidation",
            "#tradeType != null && #notionalAmount != null && #counterparty != null",
            "Basic trade data validation"
        );

        List<RuleResult> preResults = ruleEngineService.evaluateRules(Arrays.asList(preValidationRule), createEvaluationContext(context));
        if (!preResults.get(0).isTriggered()) {
            System.out.println("   ✗ PRE-VALIDATION FAILED - Missing required data");
            return;
        }
        System.out.println("   ✓ Pre-validation passed");

        // Stage 2: Risk Assessment (depends on Stage 1)
        System.out.println("\n   STAGE 2: RISK ASSESSMENT");
        Rule riskRule = new Rule(
            "RiskAssessment",
            "#notionalAmount.doubleValue() > 1000000 && #marketVolatility > 0.2 ? 'HIGH' : " +
            "(#notionalAmount.doubleValue() > 500000 || #marketVolatility > 0.15 ? 'MEDIUM' : 'LOW')",
            "Assess trade risk level"
        );

        StandardEvaluationContext evalContext = createEvaluationContext(context);
        String riskLevel = evaluatorService.evaluate(riskRule.getCondition(), evalContext, String.class);
        context.put("riskLevel", riskLevel);
        System.out.println("   → Risk Level: " + riskLevel);

        // Stage 3: Approval Workflow (depends on Stage 2 result)
        System.out.println("\n   STAGE 3: APPROVAL WORKFLOW");
        switch (riskLevel) {
            case "HIGH":
                executeHighRiskApprovalChain(context, ruleEngineService, evaluatorService);
                break;
            case "MEDIUM":
                executeMediumRiskApprovalChain(context, ruleEngineService, evaluatorService);
                break;
            case "LOW":
                executeLowRiskApprovalChain(context, ruleEngineService, evaluatorService);
                break;
        }

        // Stage 4: Final Settlement (depends on all previous stages)
        if (context.containsKey("approvalStatus") && "APPROVED".equals(context.get("approvalStatus"))) {
            System.out.println("\n   STAGE 4: SETTLEMENT PROCESSING");
            executeSettlementRules(context, ruleEngineService, evaluatorService);
        }
    }

    private static void executeHighRiskApprovalChain(Map<String, Object> context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("   → HIGH RISK approval chain:");

        // Step 1: Check counterparty limits
        Rule limitRule = new Rule("CounterpartyLimitCheck", "#counterparty == 'BANK_A'", "Counterparty within limits");
        List<RuleResult> limitResults = ruleEngineService.evaluateRules(Arrays.asList(limitRule), createEvaluationContext(context));

        if (!limitResults.get(0).isTriggered()) {
            context.put("approvalStatus", "REJECTED");
            System.out.println("     ✗ REJECTED: Counterparty limit exceeded");
            return;
        }

        // Step 2: Require senior approval (depends on Step 1)
        Rule seniorApprovalRule = new Rule("SeniorApprovalRequired", "#clientTier == 'INSTITUTIONAL'", "Senior approval granted");
        List<RuleResult> approvalResults = ruleEngineService.evaluateRules(Arrays.asList(seniorApprovalRule), createEvaluationContext(context));

        if (approvalResults.get(0).isTriggered()) {
            context.put("approvalStatus", "APPROVED");
            context.put("approvalLevel", "SENIOR");
            System.out.println("     ✓ APPROVED: Senior approval granted");
        } else {
            context.put("approvalStatus", "PENDING");
            System.out.println("     ⚠ PENDING: Awaiting senior approval");
        }
    }

    private static void executeMediumRiskApprovalChain(Map<String, Object> context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("   → MEDIUM RISK approval chain:");

        Rule managerApprovalRule = new Rule("ManagerApprovalCheck", "#maturityDays <= 365", "Manager approval sufficient");
        List<RuleResult> results = ruleEngineService.evaluateRules(Arrays.asList(managerApprovalRule), createEvaluationContext(context));

        if (results.get(0).isTriggered()) {
            context.put("approvalStatus", "APPROVED");
            context.put("approvalLevel", "MANAGER");
            System.out.println("     ✓ APPROVED: Manager approval granted");
        } else {
            context.put("approvalStatus", "ESCALATED");
            System.out.println("     ⚠ ESCALATED: Requires senior approval");
        }
    }

    private static void executeLowRiskApprovalChain(Map<String, Object> context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("   → LOW RISK approval chain:");
        context.put("approvalStatus", "APPROVED");
        context.put("approvalLevel", "AUTO");
        System.out.println("     ✓ AUTO-APPROVED: Low risk trade");
    }

    private static void executeSettlementRules(Map<String, Object> context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        // Settlement days calculation (depends on trade type and risk level)
        Rule settlementRule = new Rule(
            "SettlementDaysCalculation",
            "#tradeType == 'DERIVATIVE' ? (#riskLevel == 'HIGH' ? 5 : 3) : 2",
            "Calculate settlement days"
        );

        StandardEvaluationContext evalContext = createEvaluationContext(context);
        Integer settlementDays = evaluatorService.evaluate(settlementRule.getCondition(), evalContext, Integer.class);

        System.out.println("   → Settlement Days: " + settlementDays);
        System.out.println("   ✓ WORKFLOW COMPLETE: Trade approved and scheduled for settlement");
        System.out.println("     Final Status: " + context.get("approvalStatus") +
                         " (" + context.get("approvalLevel") + " level)");
    }

    /**
     * Pattern 6: Fluent Rule Builder - Compose rules with conditional execution paths.
     */
    private static void demonstrateFluentRuleBuilder(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("\n\n6. FLUENT RULE BUILDER PATTERN");
        System.out.println("   Compose rules with conditional execution paths using fluent API");
        System.out.println("   " + "=".repeat(50));

        // Test scenarios for fluent rule composition
        List<Map<String, Object>> testScenarios = Arrays.asList(
            Map.of("customerType", "VIP", "transactionAmount", new BigDecimal("250000"), "region", "US", "accountAge", 8),
            Map.of("customerType", "STANDARD", "transactionAmount", new BigDecimal("75000"), "region", "EU", "accountAge", 3),
            Map.of("customerType", "PREMIUM", "transactionAmount", new BigDecimal("150000"), "region", "ASIA", "accountAge", 1)
        );

        for (int i = 0; i < testScenarios.size(); i++) {
            Map<String, Object> context = testScenarios.get(i);
            System.out.println("\n   Scenario " + (i + 1) + ": " + context);

            // Create fluent rule composition
            RuleChain ruleChain = RuleChainBuilder
                .start()
                .withRule("CustomerTypeCheck", "#customerType == 'VIP' || #customerType == 'PREMIUM'", "High-tier customer detected")
                    .onSuccess()
                        .executeRule("HighValueCheck", "#transactionAmount > 100000", "High-value transaction detected")
                            .onSuccess()
                                .executeRule("RegionalComplianceCheck", "#region == 'US' ? #accountAge >= 5 : #accountAge >= 3", "Regional compliance check")
                            .onFailure()
                                .executeRule("StandardProcessing", "true", "Standard processing applied")
                    .onFailure()
                        .executeRule("BasicValidation", "#transactionAmount > 0", "Basic validation check")
                .build();

            // Execute the rule chain
            RuleChainResult result = ruleChain.execute(createEvaluationContext(context), ruleEngineService, evaluatorService);

            System.out.println("   → Execution Path: " + String.join(" → ", result.getExecutionPath()));
            System.out.println("   → Final Result: " + result.getFinalOutcome());
            System.out.println("   → Rules Executed: " + result.getExecutedRules().size());
        }
    }

    /**
     * Helper method to create evaluation context from a map.
     */
    private static StandardEvaluationContext createEvaluationContext(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        variables.forEach(context::setVariable);
        return context;
    }

    // ===== FLUENT RULE BUILDER CLASSES =====

    /**
     * Fluent builder for creating rule chains with conditional execution paths.
     */
    public static class RuleChainBuilder {
        private RuleNode rootNode;
        private RuleNode currentNode;

        private RuleChainBuilder() {}

        public static RuleChainBuilder start() {
            return new RuleChainBuilder();
        }

        public RuleChainBuilder withRule(String name, String condition, String message) {
            RuleNode node = new RuleNode(new Rule(name, condition, message));
            if (rootNode == null) {
                rootNode = node;
            }
            currentNode = node;
            return this;
        }

        public SuccessPathBuilder onSuccess() {
            return new SuccessPathBuilder(this, currentNode);
        }

        public FailurePathBuilder onFailure() {
            return new FailurePathBuilder(this, currentNode);
        }

        public RuleChain build() {
            return new RuleChain(rootNode);
        }
    }

    /**
     * Builder for success execution paths.
     */
    public static class SuccessPathBuilder {
        private final RuleChainBuilder parent;
        private final RuleNode parentNode;

        public SuccessPathBuilder(RuleChainBuilder parent, RuleNode parentNode) {
            this.parent = parent;
            this.parentNode = parentNode;
        }

        public SuccessPathBuilder executeRule(String name, String condition, String message) {
            RuleNode successNode = new RuleNode(new Rule(name, condition, message));
            parentNode.setSuccessNode(successNode);
            parent.currentNode = successNode;
            return this;
        }

        public SuccessPathBuilder onSuccess() {
            return new SuccessPathBuilder(parent, parent.currentNode);
        }

        public FailurePathBuilder onFailure() {
            return new FailurePathBuilder(parent, parent.currentNode);
        }

        public RuleChain build() {
            return parent.build();
        }
    }

    /**
     * Builder for failure execution paths.
     */
    public static class FailurePathBuilder {
        private final RuleChainBuilder parent;
        private final RuleNode parentNode;

        public FailurePathBuilder(RuleChainBuilder parent, RuleNode parentNode) {
            this.parent = parent;
            this.parentNode = parentNode;
        }

        public FailurePathBuilder executeRule(String name, String condition, String message) {
            RuleNode failureNode = new RuleNode(new Rule(name, condition, message));
            parentNode.setFailureNode(failureNode);
            parent.currentNode = failureNode;
            return this;
        }

        public SuccessPathBuilder onSuccess() {
            return new SuccessPathBuilder(parent, parent.currentNode);
        }

        public FailurePathBuilder onFailure() {
            return new FailurePathBuilder(parent, parent.currentNode);
        }

        public RuleChain build() {
            return parent.build();
        }
    }

    /**
     * Represents a node in the rule execution tree.
     */
    public static class RuleNode {
        private final Rule rule;
        private RuleNode successNode;
        private RuleNode failureNode;

        public RuleNode(Rule rule) {
            this.rule = rule;
        }

        public Rule getRule() { return rule; }
        public RuleNode getSuccessNode() { return successNode; }
        public RuleNode getFailureNode() { return failureNode; }

        public void setSuccessNode(RuleNode successNode) { this.successNode = successNode; }
        public void setFailureNode(RuleNode failureNode) { this.failureNode = failureNode; }
    }

    /**
     * Represents a chain of rules with conditional execution paths.
     */
    public static class RuleChain {
        private final RuleNode rootNode;

        public RuleChain(RuleNode rootNode) {
            this.rootNode = rootNode;
        }

        public RuleChainResult execute(StandardEvaluationContext context, RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
            List<String> executionPath = new ArrayList<>();
            List<Rule> executedRules = new ArrayList<>();
            String finalOutcome = executeNode(rootNode, context, ruleEngineService, evaluatorService, executionPath, executedRules);

            return new RuleChainResult(executionPath, executedRules, finalOutcome);
        }

        private String executeNode(RuleNode node, StandardEvaluationContext context, RuleEngineService ruleEngineService,
                                 ExpressionEvaluatorService evaluatorService, List<String> executionPath, List<Rule> executedRules) {
            if (node == null) {
                return "COMPLETED";
            }

            Rule rule = node.getRule();
            executedRules.add(rule);
            executionPath.add(rule.getName());

            // Execute the rule
            List<RuleResult> results = ruleEngineService.evaluateRules(Arrays.asList(rule), context);
            RuleResult result = results.get(0);

            if (result.isTriggered()) {
                System.out.println("     ✓ " + rule.getName() + ": PASSED - " + rule.getMessage());
                // Execute success path
                if (node.getSuccessNode() != null) {
                    return executeNode(node.getSuccessNode(), context, ruleEngineService, evaluatorService, executionPath, executedRules);
                } else {
                    return "SUCCESS";
                }
            } else {
                System.out.println("     ✗ " + rule.getName() + ": FAILED - " + rule.getMessage());
                // Execute failure path
                if (node.getFailureNode() != null) {
                    return executeNode(node.getFailureNode(), context, ruleEngineService, evaluatorService, executionPath, executedRules);
                } else {
                    return "FAILURE";
                }
            }
        }
    }

    /**
     * Result of executing a rule chain.
     */
    public static class RuleChainResult {
        private final List<String> executionPath;
        private final List<Rule> executedRules;
        private final String finalOutcome;

        public RuleChainResult(List<String> executionPath, List<Rule> executedRules, String finalOutcome) {
            this.executionPath = new ArrayList<>(executionPath);
            this.executedRules = new ArrayList<>(executedRules);
            this.finalOutcome = finalOutcome;
        }

        public List<String> getExecutionPath() { return executionPath; }
        public List<Rule> getExecutedRules() { return executedRules; }
        public String getFinalOutcome() { return finalOutcome; }
    }
}
