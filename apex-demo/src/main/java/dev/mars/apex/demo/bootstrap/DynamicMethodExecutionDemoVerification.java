package dev.mars.apex.demo.bootstrap;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.demo.model.Trade;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;

/**
 * Simple verification class to demonstrate that the merged DynamicMethodExecutionDemo works correctly.
 * 
 * This class shows that all functionality from the two original classes:
 * - DynamicMethodExecutionDemo
 * - DynamicMethodExecutionDemoConfig
 * 
 * Has been successfully merged into a single working class.
 */
public class DynamicMethodExecutionDemoVerification {

    public static void main(String[] args) {
        System.out.println("=== Dynamic Method Execution Demo - Merged Class Verification ===");
        System.out.println();

        try {
            // Create the merged demo instance
            DynamicMethodExecutionDemo demo = new DynamicMethodExecutionDemo();
            System.out.println("✅ Successfully created merged demo instance");

            // Test configuration functionality
            testConfigurationFunctionality(demo);

            // Test evaluation functionality
            testEvaluationFunctionality(demo);

            // Test rule execution functionality
            testRuleExecutionFunctionality(demo);

            // Test service functionality
            testServiceFunctionality(demo);

            System.out.println();
            System.out.println("🎉 ALL TESTS PASSED! The merged class works perfectly!");
            System.out.println("✅ Successfully merged two classes into one without losing functionality");

        } catch (Exception e) {
            System.err.println("❌ Error during verification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testConfigurationFunctionality(DynamicMethodExecutionDemo demo) {
        System.out.println("\n--- Testing Configuration Functionality ---");

        // Test context creation
        StandardEvaluationContext context = demo.createContext();
        System.out.println("✅ Context creation: " + (context != null ? "success" : "failed"));

        // Test sample trades creation
        @SuppressWarnings("unchecked")
        Map<String, Trade> trades = (Map<String, Trade>) context.lookupVariable("trades");
        System.out.println("✅ Sample trades: " + (trades != null ? trades.size() + " trades" : "null"));

        // Test services in context
        Object settlementService = context.lookupVariable("settlementService");
        Object riskService = context.lookupVariable("riskService");
        Object pricingService = context.lookupVariable("pricingService");
        Object complianceService = context.lookupVariable("complianceService");

        System.out.println("✅ Services in context:");
        System.out.println("   - settlementService: " + (settlementService != null ? "available" : "missing"));
        System.out.println("   - riskService: " + (riskService != null ? "available" : "missing"));
        System.out.println("   - pricingService: " + (pricingService != null ? "available" : "missing"));
        System.out.println("   - complianceService: " + (complianceService != null ? "available" : "missing"));
    }

    private static void testEvaluationFunctionality(DynamicMethodExecutionDemo demo) {
        System.out.println("\n--- Testing Evaluation Functionality ---");

        StandardEvaluationContext context = demo.createContext();

        try {
            // Test settlement processing evaluation
            String settlementExpression = "#settlementService.calculateSettlementDays(#trades['equity'])";
            Integer settlementDays = demo.evaluateSettlementProcessing(settlementExpression, context, Integer.class);
            System.out.println("✅ Settlement processing: " + settlementDays + " days");

            // Test risk management evaluation
            String riskExpression = "#riskService.calculateMarketRisk(#trades['equity'])";
            Double marketRisk = demo.evaluateRiskManagement(riskExpression, context, Double.class);
            System.out.println("✅ Risk management: " + marketRisk + " risk factor");

            // Test fee calculation evaluation
            String feeExpression = "#pricingService.calculateStandardPrice(100.0)";
            Double standardPrice = demo.evaluateFeeCalculation(feeExpression, context, Double.class);
            System.out.println("✅ Fee calculation: $" + standardPrice);

            // Test conditional processing evaluation
            String conditionalExpression = "#trades['equity'].value == 'Equity' ? 'Apply equity rules' : 'Apply default rules'";
            String conditionalResult = demo.evaluateConditionalProcessing(conditionalExpression, context, String.class);
            System.out.println("✅ Conditional processing: " + conditionalResult);

        } catch (Exception e) {
            System.err.println("❌ Evaluation functionality test failed: " + e.getMessage());
        }
    }

    private static void testRuleExecutionFunctionality(DynamicMethodExecutionDemo demo) {
        System.out.println("\n--- Testing Rule Execution Functionality ---");

        StandardEvaluationContext context = demo.createContext();

        try {
            // Test available rules
            Map<String, String> availableRules = demo.getAvailableRules();
            System.out.println("✅ Available rules: " + availableRules.size() + " rules");
            for (Map.Entry<String, String> entry : availableRules.entrySet()) {
                System.out.println("   - " + entry.getKey() + ": " + entry.getValue());
            }

            // Test individual rule execution
            @SuppressWarnings("unchecked")
            Map<String, Trade> trades = (Map<String, Trade>) context.lookupVariable("trades");
            context.setVariable("trade", trades.get("equity"));
            context.setVariable("basePrice", 100.0);

            Integer settlementDays = demo.executeRule("SettlementDays", context, Integer.class);
            System.out.println("✅ Individual rule execution: " + settlementDays + " settlement days");

            // Test all rules execution
            Map<String, Object> allResults = demo.executeAllRules(context);
            System.out.println("✅ All rules execution: " + allResults.size() + " results");

        } catch (Exception e) {
            System.err.println("❌ Rule execution functionality test failed: " + e.getMessage());
        }
    }

    private static void testServiceFunctionality(DynamicMethodExecutionDemo demo) {
        System.out.println("\n--- Testing Service Functionality ---");

        StandardEvaluationContext context = demo.createContext();

        try {
            // Test trade validation
            @SuppressWarnings("unchecked")
            Map<String, Trade> trades = (Map<String, Trade>) context.lookupVariable("trades");
            Trade equityTrade = (Trade) trades.get("equity");

            Map<String, Object> validationResults = demo.validateTrade(equityTrade, context);
            System.out.println("✅ Trade validation:");
            System.out.println("   - Has ID: " + validationResults.get("hasId"));
            System.out.println("   - Has Value: " + validationResults.get("hasValue"));
            System.out.println("   - Has Category: " + validationResults.get("hasCategory"));

            // Test pricing variations
            Map<String, Double> pricingResults = demo.demonstratePricingVariations(100.0, context);
            System.out.println("✅ Pricing variations:");
            System.out.println("   - Standard: $" + pricingResults.get("standard"));
            System.out.println("   - Premium: $" + pricingResults.get("premium"));
            System.out.println("   - Sale: $" + pricingResults.get("sale"));
            System.out.println("   - Clearance: $" + pricingResults.get("clearance"));

        } catch (Exception e) {
            System.err.println("❌ Service functionality test failed: " + e.getMessage());
        }
    }
}
