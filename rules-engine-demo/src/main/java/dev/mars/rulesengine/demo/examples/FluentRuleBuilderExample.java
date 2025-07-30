package dev.mars.rulesengine.demo.examples;

import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.engine.RuleEngineService;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.util.*;

import static dev.mars.rulesengine.demo.examples.SpelRulesEngineDemo.*;

/**
 * Standalone example demonstrating the Fluent Rule Builder pattern.
 * This shows how to compose rules with conditional execution paths using a fluent API.
 */
public class FluentRuleBuilderExample {

    public static void main(String[] args) {
        System.out.println("=== FLUENT RULE BUILDER PATTERN EXAMPLE ===");
        System.out.println("Demonstrating rule composition with conditional execution paths\n");

        // Create services (simplified initialization)
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        RuleEngineService ruleEngineService = new RuleEngineService(evaluatorService);

        // Example 1: VIP Customer Processing
        demonstrateVIPProcessing(ruleEngineService, evaluatorService);

        // Example 2: Standard Customer Processing  
        demonstrateStandardProcessing(ruleEngineService, evaluatorService);

        // Example 3: Complex Multi-Branch Processing
        demonstrateComplexProcessing(ruleEngineService, evaluatorService);
    }

    /**
     * Example 1: VIP Customer Processing - Success Path
     */
    private static void demonstrateVIPProcessing(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("1. VIP CUSTOMER PROCESSING EXAMPLE");
        System.out.println("   " + "-".repeat(40));

        Map<String, Object> context = Map.of(
            "customerType", "VIP",
            "transactionAmount", new BigDecimal("250000"),
            "region", "US",
            "accountAge", 8
        );

        System.out.println("   Context: " + context);

        // Build rule chain using fluent API
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
        System.out.println("   ✓ VIP customer successfully processed through high-value path\n");
    }

    /**
     * Example 2: Standard Customer Processing - Failure Path
     */
    private static void demonstrateStandardProcessing(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("2. STANDARD CUSTOMER PROCESSING EXAMPLE");
        System.out.println("   " + "-".repeat(40));

        Map<String, Object> context = Map.of(
            "customerType", "STANDARD",
            "transactionAmount", new BigDecimal("75000"),
            "region", "EU",
            "accountAge", 3
        );

        System.out.println("   Context: " + context);

        // Same rule chain as above - will follow different path
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
        
        System.out.println("   → Execution Path: " + String.join(" → ", result.getExecutionPath()));
        System.out.println("   → Final Result: " + result.getFinalOutcome());
        System.out.println("   ✓ Standard customer routed to basic validation path\n");
    }

    /**
     * Example 3: Complex Multi-Branch Processing
     */
    private static void demonstrateComplexProcessing(RuleEngineService ruleEngineService, ExpressionEvaluatorService evaluatorService) {
        System.out.println("3. COMPLEX MULTI-BRANCH PROCESSING EXAMPLE");
        System.out.println("   " + "-".repeat(40));

        Map<String, Object> context = Map.of(
            "customerType", "PREMIUM",
            "transactionAmount", new BigDecimal("150000"),
            "region", "ASIA",
            "accountAge", 1,
            "riskScore", 85
        );

        System.out.println("   Context: " + context);

        // Complex rule chain with multiple branches
        RuleChain ruleChain = RuleChainBuilder
            .start()
            .withRule("InitialRiskCheck", "#riskScore < 90", "Initial risk assessment")
                .onSuccess()
                    .executeRule("CustomerTierCheck", "#customerType == 'VIP' || #customerType == 'PREMIUM'", "Customer tier validation")
                        .onSuccess()
                            .executeRule("TransactionLimitCheck", "#transactionAmount <= 200000", "Transaction limit check")
                                .onSuccess()
                                    .executeRule("AccountMaturityCheck", "#accountAge >= 2", "Account maturity check")
                                        .onSuccess()
                                            .executeRule("FinalApproval", "true", "Final approval granted")
                                        .onFailure()
                                            .executeRule("ManualReview", "true", "Manual review required")
                                .onFailure()
                                    .executeRule("SeniorApproval", "true", "Senior approval required")
                        .onFailure()
                            .executeRule("StandardLimits", "#transactionAmount <= 50000", "Standard customer limits")
                .onFailure()
                    .executeRule("HighRiskReview", "true", "High risk review required")
            .build();

        // Execute the rule chain
        RuleChainResult result = ruleChain.execute(createEvaluationContext(context), ruleEngineService, evaluatorService);
        
        System.out.println("   → Execution Path: " + String.join(" → ", result.getExecutionPath()));
        System.out.println("   → Final Result: " + result.getFinalOutcome());
        System.out.println("   ✓ Complex processing completed with " + result.getExecutedRules().size() + " rules executed");
    }

    /**
     * Helper method to create evaluation context from a map.
     */
    private static StandardEvaluationContext createEvaluationContext(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        variables.forEach(context::setVariable);
        return context;
    }
}
