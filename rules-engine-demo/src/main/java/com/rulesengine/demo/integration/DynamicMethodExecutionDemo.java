package com.rulesengine.demo.integration;

import com.rulesengine.core.service.engine.ExpressionEvaluatorService;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.demo.service.ComplianceService;
import com.rulesengine.demo.service.PostTradeProcessingService;
import com.rulesengine.demo.service.RiskManagementService;
import com.rulesengine.demo.service.PricingServiceDemo;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates dynamic method execution features in post-trade processing.
 * This class provides extensive examples of using SpEL for dynamic method execution
 * in financial services, particularly in post-trade processing scenarios.
 */
public class DynamicMethodExecutionDemo {
    private final ExpressionEvaluatorService evaluatorService;
    private final Map<String, Trade> sampleTrades = new HashMap<>();
    
    /**
     * Create a new DynamicMethodExecutionDemo with the specified evaluator service.
     * 
     * @param evaluatorService The expression evaluator service
     */
    public DynamicMethodExecutionDemo(ExpressionEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
        initializeSampleTrades();
    }
    
    /**
     * Initialize sample trades for demonstration.
     */
    private void initializeSampleTrades() {
        // Create sample trades of different types
        sampleTrades.put("equity", new Trade("T001", PostTradeProcessingService.TYPE_EQUITY, "InstrumentType"));
        sampleTrades.put("fixedIncome", new Trade("T002", PostTradeProcessingService.TYPE_FIXED_INCOME, "InstrumentType"));
        sampleTrades.put("derivative", new Trade("T003", PostTradeProcessingService.TYPE_DERIVATIVE, "InstrumentType"));
        sampleTrades.put("forex", new Trade("T004", PostTradeProcessingService.TYPE_FOREX, "InstrumentType"));
        sampleTrades.put("commodity", new Trade("T005", PostTradeProcessingService.TYPE_COMMODITY, "InstrumentType"));
        
        // Create a trade with no ID for validation testing
        Trade invalidTrade = new Trade();
        invalidTrade.setValue(PostTradeProcessingService.TYPE_EQUITY);
        invalidTrade.setCategory("InstrumentType");
        sampleTrades.put("invalid", invalidTrade);
    }
    
    /**
     * Demonstrates dynamic method execution in post-trade processing.
     * This method showcases 50+ examples of dynamic method execution using SpEL.
     * 
     * @param pricingService The pricing service for pricing examples
     */
    public void demonstrateDynamicMethodExecution(PricingServiceDemo pricingService) {
        System.out.println("\n=== Demonstrating Dynamic Method Execution in Post-Trade Processing ===");
        
        // Create services
        PostTradeProcessingService postTradeService = new PostTradeProcessingService();
        RiskManagementService riskService = new RiskManagementService();
        ComplianceService complianceService = new ComplianceService();
        
        // Create context with services and sample trades
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("postTradeService", postTradeService);
        context.setVariable("riskService", riskService);
        context.setVariable("complianceService", complianceService);
        context.setVariable("pricingService", pricingService);
        context.setVariable("trades", sampleTrades);
        
        // Set notional value for risk calculations
        context.setVariable("notionalValue", 1000000.0);
        
        // ===== CATEGORY 1: SETTLEMENT PROCESSING =====
        demonstrateSettlementProcessing(context);
        
        // ===== CATEGORY 2: RISK MANAGEMENT =====
        demonstrateRiskManagement(context);
        
        // ===== CATEGORY 3: COMPLIANCE AND REGULATORY REPORTING =====
        demonstrateComplianceAndReporting(context);
        
        // ===== CATEGORY 4: FEE CALCULATIONS =====
        demonstrateFeeCalculations(context);
        
        // ===== CATEGORY 5: CONDITIONAL PROCESSING BASED ON TRADE ATTRIBUTES =====
        demonstrateConditionalProcessing(context);
    }
    
    /**
     * Demonstrates settlement processing examples.
     * 
     * @param context The evaluation context
     */
    private void demonstrateSettlementProcessing(EvaluationContext context) {
        System.out.println("\n----- CATEGORY 1: SETTLEMENT PROCESSING -----");
        
        // Example 1: Calculate settlement days for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.calculateSettlementDays(#trades['" + tradeType + "'])";
            Integer days = evaluatorService.evaluate(expression, context, Integer.class);
            System.out.println("Example 1." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Settlement days for " + tradeType + " trade: " + days);
        }
        
        // Example 2: Determine settlement method for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.determineSettlementMethod(#trades['" + tradeType + "'])";
            String method = evaluatorService.evaluate(expression, context, String.class);
            System.out.println("Example 2." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Settlement method for " + tradeType + " trade: " + method);
        }
        
        // Example 3: Validate trades for settlement
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.validateTradeForSettlement(#trades['" + tradeType + "'])";
            Boolean isValid = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 3." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Is " + tradeType + " trade valid for settlement? " + isValid);
        }
        
        // Example 4: Match trades with counterparty
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.matchTradeWithCounterparty(#trades['" + tradeType + "'])";
            Boolean isMatched = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 4." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Is " + tradeType + " trade matched with counterparty? " + isMatched);
        }
        
        // Example 5: Affirm trades
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.affirmTrade(#trades['" + tradeType + "'])";
            Boolean isAffirmed = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 5." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Is " + tradeType + " trade affirmed? " + isAffirmed);
        }
        
        // Example 6: Confirm trades
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.confirmTrade(#trades['" + tradeType + "'])";
            Boolean isConfirmed = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 6." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Is " + tradeType + " trade confirmed? " + isConfirmed);
        }
        
        // Example 7: Settle trades
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.settleTrade(#trades['" + tradeType + "'])";
            Boolean isSettled = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 7." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Is " + tradeType + " trade settled? " + isSettled);
        }
    }
    
    /**
     * Demonstrates risk management examples.
     * 
     * @param context The evaluation context
     */
    private void demonstrateRiskManagement(EvaluationContext context) {
        System.out.println("\n----- CATEGORY 2: RISK MANAGEMENT -----");
        
        // Example 8: Calculate market risk for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateMarketRisk(#trades['" + tradeType + "'])";
            Double risk = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 8." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Market risk for " + tradeType + " trade: " + risk);
        }
        
        // Example 9: Calculate credit risk for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateCreditRisk(#trades['" + tradeType + "'])";
            Double risk = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 9." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Credit risk for " + tradeType + " trade: " + risk);
        }
        
        // Example 10: Calculate liquidity risk for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateLiquidityRisk(#trades['" + tradeType + "'])";
            Double risk = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 10." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Liquidity risk for " + tradeType + " trade: " + risk);
        }
        
        // Example 11: Calculate operational risk for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateOperationalRisk(#trades['" + tradeType + "'])";
            Double risk = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 11." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Operational risk for " + tradeType + " trade: " + risk);
        }
        
        // Example 12: Calculate settlement risk for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateSettlementRisk(#trades['" + tradeType + "'])";
            Double risk = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 12." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Settlement risk for " + tradeType + " trade: " + risk);
        }
        
        // Example 13: Calculate total risk for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateTotalRisk(#trades['" + tradeType + "'])";
            Double risk = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 13." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Total risk for " + tradeType + " trade: " + risk);
        }
        
        // Example 14: Determine risk level for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.determineRiskLevel(#trades['" + tradeType + "'])";
            String riskLevel = evaluatorService.evaluate(expression, context, String.class);
            System.out.println("Example 14." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Risk level for " + tradeType + " trade: " + riskLevel);
        }
        
        // Example 15: Check if additional risk review is required
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.requiresAdditionalRiskReview(#trades['" + tradeType + "'])";
            Boolean requiresReview = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 15." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Does " + tradeType + " trade require additional risk review? " + requiresReview);
        }
        
        // Example 16: Calculate risk-weighted value
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateRiskWeightedValue(#trades['" + tradeType + "'], #notionalValue)";
            Double riskWeightedValue = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 16." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Risk-weighted value for " + tradeType + " trade: $" + riskWeightedValue);
        }
    }
    
    /**
     * Demonstrates compliance and regulatory reporting examples.
     * 
     * @param context The evaluation context
     */
    private void demonstrateComplianceAndReporting(EvaluationContext context) {
        System.out.println("\n----- CATEGORY 3: COMPLIANCE AND REGULATORY REPORTING -----");
        
        // Example 17: Get applicable regulations for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.getApplicableRegulations(#trades['" + tradeType + "'])";
            List<String> regulations = evaluatorService.evaluate(expression, context, List.class);
            System.out.println("Example 17." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Applicable regulations for " + tradeType + " trade: " + regulations);
        }
        
        // Example 18: Check if MiFID II reporting is required
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.requiresMiFIDReporting(#trades['" + tradeType + "'])";
            Boolean isRequired = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 18." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Does " + tradeType + " trade require MiFID II reporting? " + isRequired);
        }
        
        // Example 19: Check if EMIR reporting is required
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.requiresEMIRReporting(#trades['" + tradeType + "'])";
            Boolean isRequired = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 19." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Does " + tradeType + " trade require EMIR reporting? " + isRequired);
        }
        
        // Example 20: Check if Dodd-Frank reporting is required
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.requiresDoddFrankReporting(#trades['" + tradeType + "'])";
            Boolean isRequired = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 20." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Does " + tradeType + " trade require Dodd-Frank reporting? " + isRequired);
        }
        
        // Example 21: Check if Basel III reporting is required
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.requiresBaselReporting(#trades['" + tradeType + "'])";
            Boolean isRequired = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 21." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Does " + tradeType + " trade require Basel III reporting? " + isRequired);
        }
        
        // Example 22: Check if SFTR reporting is required
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.requiresSFTRReporting(#trades['" + tradeType + "'])";
            Boolean isRequired = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 22." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Does " + tradeType + " trade require SFTR reporting? " + isRequired);
        }
        
        // Example 23: Generate MiFID II report for equity trade
        String expression = "#complianceService.generateMiFIDReport(#trades['equity'])";
        String report = evaluatorService.evaluate(expression, context, String.class);
        System.out.println("Example 23: MiFID II report for equity trade:\n" + report);
        
        // Example 24: Generate EMIR report for derivative trade
        expression = "#complianceService.generateEMIRReport(#trades['derivative'])";
        report = evaluatorService.evaluate(expression, context, String.class);
        System.out.println("\nExample 24: EMIR report for derivative trade:\n" + report);
        
        // Example 25: Generate Dodd-Frank report for forex trade
        expression = "#complianceService.generateDoddFrankReport(#trades['forex'])";
        report = evaluatorService.evaluate(expression, context, String.class);
        System.out.println("\nExample 25: Dodd-Frank report for forex trade:\n" + report);
        
        // Example 26: Generate Basel III report for fixed income trade
        expression = "#complianceService.generateBaselReport(#trades['fixedIncome'])";
        report = evaluatorService.evaluate(expression, context, String.class);
        System.out.println("\nExample 26: Basel III report for fixed income trade:\n" + report);
        
        // Example 27: Check for compliance issues
        for (String tradeType : tradeTypes) {
            expression = "#complianceService.hasComplianceIssues(#trades['" + tradeType + "'])";
            Boolean hasIssues = evaluatorService.evaluate(expression, context, Boolean.class);
            System.out.println("Example 27." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Does " + tradeType + " trade have compliance issues? " + hasIssues);
        }
        
        // Example 28: Check for compliance issues with invalid trade
        expression = "#complianceService.hasComplianceIssues(#trades['invalid'])";
        Boolean hasIssues = evaluatorService.evaluate(expression, context, Boolean.class);
        System.out.println("Example 28: Does invalid trade have compliance issues? " + hasIssues);
    }
    
    /**
     * Demonstrates fee calculation examples.
     * 
     * @param context The evaluation context
     */
    private void demonstrateFeeCalculations(EvaluationContext context) {
        System.out.println("\n----- CATEGORY 4: FEE CALCULATIONS -----");
        
        // Example 29: Calculate settlement fee for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.calculateSettlementFee(#trades['" + tradeType + "'])";
            Double fee = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 29." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Settlement fee for " + tradeType + " trade: $" + fee);
        }
        
        // Example 30: Calculate clearing fee for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.calculateClearingFee(#trades['" + tradeType + "'])";
            Double fee = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 30." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Clearing fee for " + tradeType + " trade: $" + fee);
        }
        
        // Example 31: Calculate custody fee for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.calculateCustodyFee(#trades['" + tradeType + "'])";
            Double fee = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 31." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Custody fee for " + tradeType + " trade: $" + fee);
        }
        
        // Example 32: Calculate total post-trade processing fee
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.calculateSettlementFee(#trades['" + tradeType + "']) + " +
                    "#postTradeService.calculateClearingFee(#trades['" + tradeType + "']) + " +
                    "#postTradeService.calculateCustodyFee(#trades['" + tradeType + "'])";
            Double totalFee = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 32." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Total post-trade processing fee for " + tradeType + " trade: $" + totalFee);
        }
        
        // Example 33: Calculate price using different pricing strategies
        String[] pricingStrategies = {"Standard", "Premium", "Sale", "Clearance"};
        for (String strategy : pricingStrategies) {
            String methodName = "calculate" + strategy + "Price";
            String expression = "#pricingService." + methodName + "(100.0)";
            Double price = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 33." + (strategy.charAt(0) - 'A' + 1) + 
                    ": " + strategy + " price for $100 base price: $" + price);
        }
    }
    
    /**
     * Demonstrates conditional processing examples based on trade attributes.
     * 
     * @param context The evaluation context
     */
    private void demonstrateConditionalProcessing(EvaluationContext context) {
        System.out.println("\n----- CATEGORY 5: CONDITIONAL PROCESSING BASED ON TRADE ATTRIBUTES -----");
        
        // Example 34: Determine settlement method based on trade type
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#trades['" + tradeType + "'].getValue() == '" + 
                    PostTradeProcessingService.TYPE_EQUITY + "' ? 'DTC' : " +
                    "#trades['" + tradeType + "'].getValue() == '" + 
                    PostTradeProcessingService.TYPE_FIXED_INCOME + "' ? 'Fedwire' : " +
                    "#trades['" + tradeType + "'].getValue() == '" + 
                    PostTradeProcessingService.TYPE_DERIVATIVE + "' ? 'Clearstream' : " +
                    "#trades['" + tradeType + "'].getValue() == '" + 
                    PostTradeProcessingService.TYPE_FOREX + "' ? 'Euroclear' : 'Manual'";
            String method = evaluatorService.evaluate(expression, context, String.class);
            System.out.println("Example 34." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Settlement method for " + tradeType + " trade: " + method);
        }
        
        // Example 35: Apply different pricing strategies based on risk level
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.determineRiskLevel(#trades['" + tradeType + "']) == '" + 
                    RiskManagementService.RISK_LOW + "' ? #pricingService.calculateStandardPrice(100.0) : " +
                    "#riskService.determineRiskLevel(#trades['" + tradeType + "']) == '" + 
                    RiskManagementService.RISK_MEDIUM + "' ? #pricingService.calculatePremiumPrice(100.0) : " +
                    "#riskService.determineRiskLevel(#trades['" + tradeType + "']) == '" + 
                    RiskManagementService.RISK_HIGH + "' ? #pricingService.calculateSalePrice(100.0) : " +
                    "#pricingService.calculateClearancePrice(100.0)";
            Double price = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 35." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Risk-based price for " + tradeType + " trade: $" + price);
        }
        
        // Example 36: Determine reporting requirements based on trade type
        for (String tradeType : tradeTypes) {
            String expression = "T(java.util.Arrays).asList(" +
                    "#complianceService.requiresMiFIDReporting(#trades['" + tradeType + "']) ? 'MiFID II' : '', " +
                    "#complianceService.requiresEMIRReporting(#trades['" + tradeType + "']) ? 'EMIR' : '', " +
                    "#complianceService.requiresDoddFrankReporting(#trades['" + tradeType + "']) ? 'Dodd-Frank' : '', " +
                    "#complianceService.requiresBaselReporting(#trades['" + tradeType + "']) ? 'Basel III' : '', " +
                    "#complianceService.requiresSFTRReporting(#trades['" + tradeType + "']) ? 'SFTR' : ''" +
                    ").?[length() > 0]";
            List<String> requirements = evaluatorService.evaluate(expression, context, List.class);
            System.out.println("Example 36." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Reporting requirements for " + tradeType + " trade: " + requirements);
        }
        
        // Example 37: Calculate total cost based on trade type and risk
        for (String tradeType : tradeTypes) {
            String expression = "#postTradeService.calculateSettlementFee(#trades['" + tradeType + "']) + " +
                    "#postTradeService.calculateClearingFee(#trades['" + tradeType + "']) + " +
                    "#postTradeService.calculateCustodyFee(#trades['" + tradeType + "']) + " +
                    "(#riskService.calculateTotalRisk(#trades['" + tradeType + "']) * 10)";
            Double totalCost = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println("Example 37." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Total cost for " + tradeType + " trade: $" + totalCost);
        }
        
        // Example 38: Determine processing priority based on trade attributes
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.hasComplianceIssues(#trades['" + tradeType + "']) ? 'High' : " +
                    "#riskService.requiresAdditionalRiskReview(#trades['" + tradeType + "']) ? 'Medium' : 'Low'";
            String priority = evaluatorService.evaluate(expression, context, String.class);
            System.out.println("Example 38." + (tradeType.charAt(0) - 'a' + 1) + 
                    ": Processing priority for " + tradeType + " trade: " + priority);
        }
    }
}