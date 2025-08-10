package dev.mars.apex.demo.examples;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.demo.examples.ApexRulesEngineDemo.RuleChain;
import dev.mars.apex.demo.examples.ApexRulesEngineDemo.RuleChainBuilder;
import dev.mars.apex.demo.examples.ApexRulesEngineDemo.RuleChainResult;

/**
 * Test class demonstrating nested rules and rule chaining scenarios.
 * This shows common patterns where rules depend on results of previous rules.
 */
public class NestedRulesChainingTest {

    private dev.mars.apex.core.service.engine.RuleEngineService ruleEngineService;
    private dev.mars.apex.core.service.engine.ExpressionEvaluatorService evaluatorService;

    @BeforeEach
    void setUp() {
        evaluatorService = new dev.mars.apex.core.service.engine.ExpressionEvaluatorService();
        ruleEngineService = new dev.mars.apex.core.service.engine.RuleEngineService(evaluatorService);
    }

    /**
     * Test Pattern 1: Conditional Chaining - Execute rule B only if rule A triggers.
     */
    @Test
    void testConditionalChaining() {
        System.out.println("=== Testing Conditional Chaining Pattern ===");
        
        // Test scenario: Premium customer with high-value transaction
        Map<String, Object> context = Map.of(
            "customerType", "PREMIUM",
            "transactionAmount", new BigDecimal("150000"),
            "accountAge", 5
        );

        // Rule A: Check if customer qualifies for high-value processing
        Rule ruleA = new Rule(
            "HighValueCustomerCheck",
            "#customerType == 'PREMIUM' && #transactionAmount > 100000",
            "Customer qualifies for high-value processing"
        );

        // Execute Rule A
        List<RuleResult> resultsA = ruleEngineService.evaluateRules(
            Arrays.asList(ruleA), createEvaluationContext(context));
        RuleResult resultA = resultsA.get(0);

        assertTrue(resultA.isTriggered(), "Rule A should trigger for premium customer with high transaction");
        System.out.println("✓ Rule A triggered: " + resultA.getMessage());

        // Conditional execution of Rule B (only if Rule A triggers)
        if (resultA.isTriggered()) {
            Rule ruleB = new Rule(
                "EnhancedDueDiligenceCheck",
                "#accountAge >= 3",
                "Enhanced due diligence check passed"
            );

            List<RuleResult> resultsB = ruleEngineService.evaluateRules(
                Arrays.asList(ruleB), createEvaluationContext(context));
            RuleResult resultB = resultsB.get(0);

            assertTrue(resultB.isTriggered(), "Rule B should pass for account age >= 3");
            System.out.println("✓ Rule B triggered: " + resultB.getMessage());
            System.out.println("✓ APPROVED: High-value transaction approved with enhanced checks");
        }
    }

    /**
     * Test Pattern 2: Sequential Dependency - Each rule uses results from the previous rule.
     */
    @Test
    void testSequentialDependency() {
        System.out.println("\n=== Testing Sequential Dependency Pattern ===");
        
        Map<String, Object> context = new HashMap<>();
        context.put("baseAmount", new BigDecimal("100000"));
        context.put("customerTier", "GOLD");
        context.put("region", "US");

        // Rule 1: Calculate base discount
        Rule rule1 = new Rule(
            "BaseDiscountCalculation",
            "#customerTier == 'GOLD' ? 0.15 : (#customerTier == 'SILVER' ? 0.10 : 0.05)",
            "Calculate base discount based on customer tier"
        );

        StandardEvaluationContext evalContext = createEvaluationContext(context);
        Double baseDiscount = evaluatorService.evaluate(rule1.getCondition(), evalContext, Double.class);
        
        assertNotNull(baseDiscount, "Base discount should be calculated");
        assertEquals(0.15, baseDiscount, 0.001, "Gold tier should get 15% discount");
        
        // Add result to context for next rule
        context.put("baseDiscount", baseDiscount);
        System.out.println("✓ Rule 1 Result: Base discount = " + (baseDiscount * 100) + "%");

        // Rule 2: Apply regional multiplier (depends on Rule 1 result)
        Rule rule2 = new Rule(
            "RegionalMultiplierCalculation",
            "#region == 'US' ? #baseDiscount * 1.2 : (#region == 'EU' ? #baseDiscount * 1.1 : #baseDiscount)",
            "Apply regional multiplier to base discount"
        );

        evalContext = createEvaluationContext(context);
        Double finalDiscount = evaluatorService.evaluate(rule2.getCondition(), evalContext, Double.class);
        
        assertNotNull(finalDiscount, "Final discount should be calculated");
        assertEquals(0.18, finalDiscount, 0.001, "US region should get 1.2x multiplier");
        
        context.put("finalDiscount", finalDiscount);
        System.out.println("✓ Rule 2 Result: Final discount = " + (finalDiscount * 100) + "%");

        // Rule 3: Calculate final amount (depends on Rule 2 result)
        Rule rule3 = new Rule(
            "FinalAmountCalculation",
            "#baseAmount * (1 - #finalDiscount)",
            "Calculate final amount after all discounts"
        );

        evalContext = createEvaluationContext(context);
        BigDecimal finalAmount = evaluatorService.evaluate(rule3.getCondition(), evalContext, BigDecimal.class);
        
        assertNotNull(finalAmount, "Final amount should be calculated");
        assertEquals(new BigDecimal("82000.00"), finalAmount.setScale(2, RoundingMode.HALF_UP), "Final amount should reflect all discounts");
        
        System.out.println("✓ Rule 3 Result: Final amount = $" + finalAmount);
        System.out.println("✓ Sequential processing chain completed successfully");
    }

    /**
     * Test Pattern 3: Result-Based Routing - Route to different rule sets based on previous results.
     */
    @Test
    void testResultBasedRouting() {
        System.out.println("\n=== Testing Result-Based Routing Pattern ===");
        
        Map<String, Object> context = Map.of(
            "riskScore", 85,
            "transactionAmount", new BigDecimal("500000"),
            "customerHistory", "EXCELLENT"
        );

        // Router Rule: Determine processing path
        Rule routerRule = new Rule(
            "ProcessingPathRouter",
            "#riskScore > 70 ? 'HIGH_RISK' : (#riskScore > 30 ? 'MEDIUM_RISK' : 'LOW_RISK')",
            "Determine processing path based on risk score"
        );

        StandardEvaluationContext evalContext = createEvaluationContext(context);
        String processingPath = evaluatorService.evaluate(routerRule.getCondition(), evalContext, String.class);
        
        assertEquals("HIGH_RISK", processingPath, "Risk score 85 should route to HIGH_RISK");
        System.out.println("✓ Router Decision: " + processingPath + " processing path");

        // Execute high risk rules based on routing result
        if ("HIGH_RISK".equals(processingPath)) {
            Rule rule1 = new Rule("ManagerApprovalRequired", "#transactionAmount > 100000", "Manager approval required");
            Rule rule2 = new Rule("ComplianceReviewRequired", "#riskScore > 80", "Compliance review required");
            Rule rule3 = new Rule("CustomerHistoryCheck", "#customerHistory == 'EXCELLENT'", "Customer history must be excellent");

            List<Rule> highRiskRules = Arrays.asList(rule1, rule2, rule3);
            List<RuleResult> results = ruleEngineService.evaluateRules(highRiskRules, createEvaluationContext(context));
            
            assertTrue(results.stream().allMatch(RuleResult::isTriggered), "All high risk rules should pass");
            System.out.println("✓ HIGH RISK RESULT: APPROVED with multiple approvals");
        }
    }

    /**
     * Test Pattern 4: Accumulative Chaining - Build up a score/result across multiple rules.
     */
    @Test
    void testAccumulativeChaining() {
        System.out.println("\n=== Testing Accumulative Chaining Pattern ===");
        
        Map<String, Object> context = new HashMap<>();
        context.put("creditScore", 750);
        context.put("annualIncome", new BigDecimal("85000"));
        context.put("employmentYears", 8);
        context.put("existingDebt", new BigDecimal("25000"));
        context.put("totalScore", 0);

        // Rule 1: Credit Score Component
        Rule rule1 = new Rule(
            "CreditScoreEvaluation",
            "#creditScore >= 700 ? 25 : (#creditScore >= 650 ? 15 : (#creditScore >= 600 ? 10 : 0))",
            "Credit score component"
        );

        StandardEvaluationContext evalContext = createEvaluationContext(context);
        Integer creditScorePoints = evaluatorService.evaluate(rule1.getCondition(), evalContext, Integer.class);
        context.put("totalScore", (Integer)context.get("totalScore") + creditScorePoints);
        
        assertEquals(25, creditScorePoints, "Credit score 750 should get 25 points");
        System.out.println("✓ Rule 1 (Credit Score): +" + creditScorePoints + " points (Total: " + context.get("totalScore") + ")");

        // Rule 2: Income Component
        Rule rule2 = new Rule(
            "IncomeEvaluation", 
            "#annualIncome >= 80000 ? 20 : (#annualIncome >= 60000 ? 15 : (#annualIncome >= 40000 ? 10 : 0))",
            "Income component"
        );

        evalContext = createEvaluationContext(context);
        Integer incomePoints = evaluatorService.evaluate(rule2.getCondition(), evalContext, Integer.class);
        context.put("totalScore", (Integer)context.get("totalScore") + incomePoints);
        
        assertEquals(20, incomePoints, "Income $85,000 should get 20 points");
        System.out.println("✓ Rule 2 (Income): +" + incomePoints + " points (Total: " + context.get("totalScore") + ")");

        // Rule 3: Employment Component
        Rule rule3 = new Rule(
            "EmploymentEvaluation",
            "#employmentYears >= 5 ? 15 : (#employmentYears >= 3 ? 10 : (#employmentYears >= 1 ? 5 : 0))",
            "Employment stability component"
        );

        evalContext = createEvaluationContext(context);
        Integer employmentPoints = evaluatorService.evaluate(rule3.getCondition(), evalContext, Integer.class);
        context.put("totalScore", (Integer)context.get("totalScore") + employmentPoints);

        assertEquals(15, employmentPoints, "Employment 8 years should get 15 points");
        System.out.println("✓ Rule 3 (Employment): +" + employmentPoints + " points (Total: " + context.get("totalScore") + ")");

        // Final Decision Rule (depends on accumulated score)
        Rule finalRule = new Rule(
            "LoanDecision",
            "#totalScore >= 60 ? 'APPROVED' : (#totalScore >= 40 ? 'CONDITIONAL' : 'DENIED')",
            "Final loan decision based on total score"
        );

        evalContext = createEvaluationContext(context);
        String decision = evaluatorService.evaluate(finalRule.getCondition(), evalContext, String.class);
        
        assertEquals("APPROVED", decision, "Total score should result in approval");
        System.out.println("✓ Final Decision: " + decision + " (Score: " + context.get("totalScore") + "/100)");
    }

    /**
     * Test Pattern 6: Fluent Rule Builder - Compose rules with conditional execution paths.
     */
    @Test
    void testFluentRuleBuilder() {
        System.out.println("\n=== Testing Fluent Rule Builder Pattern ===");

        // Test scenario: VIP customer with high-value transaction
        Map<String, Object> context = Map.of(
            "customerType", "VIP",
            "transactionAmount", new BigDecimal("250000"),
            "region", "US",
            "accountAge", 8
        );

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

        assertNotNull(result, "Rule chain result should not be null");
        assertFalse(result.getExecutionPath().isEmpty(), "Execution path should not be empty");
        assertEquals("SUCCESS", result.getFinalOutcome(), "VIP customer with valid criteria should succeed");

        // Verify execution path
        List<String> expectedPath = Arrays.asList("CustomerTypeCheck", "HighValueCheck", "RegionalComplianceCheck");
        assertEquals(expectedPath, result.getExecutionPath(), "Execution path should follow success branch");

        System.out.println("✓ Execution Path: " + String.join(" → ", result.getExecutionPath()));
        System.out.println("✓ Final Result: " + result.getFinalOutcome());
        System.out.println("✓ Rules Executed: " + result.getExecutedRules().size());
    }

    /**
     * Test fluent builder with failure path.
     */
    @Test
    void testFluentRuleBuilderFailurePath() {
        System.out.println("\n=== Testing Fluent Rule Builder Failure Path ===");

        // Test scenario: Standard customer (should trigger failure path)
        Map<String, Object> context = Map.of(
            "customerType", "STANDARD",
            "transactionAmount", new BigDecimal("75000"),
            "region", "EU",
            "accountAge", 3
        );

        // Create fluent rule composition
        RuleChain ruleChain = RuleChainBuilder
            .start()
            .withRule("CustomerTypeCheck", "#customerType == 'VIP' || #customerType == 'PREMIUM'", "High-tier customer detected")
                .onSuccess()
                    .executeRule("HighValueCheck", "#transactionAmount > 100000", "High-value transaction detected")
                .onFailure()
                    .executeRule("BasicValidation", "#transactionAmount > 0", "Basic validation check")
            .build();

        // Execute the rule chain
        RuleChainResult result = ruleChain.execute(createEvaluationContext(context), ruleEngineService, evaluatorService);

        assertNotNull(result, "Rule chain result should not be null");
        assertEquals("FAILURE", result.getFinalOutcome(), "Standard customer should follow failure path");

        // Verify execution path follows failure branch
        List<String> expectedPath = Arrays.asList("CustomerTypeCheck");
        assertEquals(expectedPath, result.getExecutionPath(), "Execution path should include CustomerTypeCheck");

        System.out.println("✓ Execution Path: " + String.join(" → ", result.getExecutionPath()));
        System.out.println("✓ Final Result: " + result.getFinalOutcome());
        System.out.println("✓ Fluent builder correctly routed to failure path");
    }

    /**
     * Helper method to create evaluation context from a map.
     */
    private StandardEvaluationContext createEvaluationContext(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        variables.forEach(context::setVariable);
        return context;
    }
}
