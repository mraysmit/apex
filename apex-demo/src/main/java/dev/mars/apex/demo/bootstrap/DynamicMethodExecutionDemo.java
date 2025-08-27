package dev.mars.apex.demo.bootstrap;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.demo.model.Trade;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.logging.Logger;

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
 * Comprehensive Dynamic Method Execution Demo.
 *
 * This merged class combines the functionality of:
 * - DynamicMethodExecutionDemo (main demo logic)
 * - DynamicMethodExecutionDemoConfig (configuration, rules, and services)
 *
 * It demonstrates dynamic method execution using SpEL expressions,
 * focusing on financial trade processing, risk management, compliance,
 * and rule-based processing capabilities.
 *
 * This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DynamicMethodExecutionDemo {
    private static final Logger LOGGER = Logger.getLogger(DynamicMethodExecutionDemo.class.getName());

    // Core APEX services
    private final ExpressionEvaluatorService evaluatorService;
    private final Map<String, Rule> expressionRules = new HashMap<>();

    // Trade types constants (from DynamicMethodExecutionDemoConfig)
    private static final String TYPE_EQUITY = "Equity";
    private static final String TYPE_FIXED_INCOME = "FixedIncome";
    private static final String TYPE_DERIVATIVE = "Derivative";
    private static final String TYPE_FOREX = "Forex";
    private static final String TYPE_COMMODITY = "Commodity";

    // Settlement methods constants (from DynamicMethodExecutionDemoConfig)
    private static final String METHOD_DTC = "DTC";
    private static final String METHOD_FEDWIRE = "Fedwire";
    private static final String METHOD_EUROCLEAR = "Euroclear";
    private static final String METHOD_CLEARSTREAM = "Clearstream";
    private static final String METHOD_MANUAL = "Manual";

    /**
     * Constructor for creating a demo instance with services.
     *
     * @param evaluatorService The expression evaluator service to use
     */
    public DynamicMethodExecutionDemo(ExpressionEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
        initializeRules();
    }

    /**
     * Default constructor that creates all services.
     */
    public DynamicMethodExecutionDemo() {
        this.evaluatorService = new ExpressionEvaluatorService();
        initializeRules();
    }

    // ========================================
    // CONFIGURATION FUNCTIONALITY (from DynamicMethodExecutionDemoConfig)
    // ========================================

    /**
     * Initialize rules for dynamic method execution.
     */
    private void initializeRules() {
        LOGGER.info("Initializing rules for dynamic method execution");

        // Rule for settlement days calculation
        expressionRules.put("SettlementDays", new Rule(
                "SettlementDaysRule",
                "#settlementService.calculateSettlementDays(#trade)",
                "Calculate settlement days for trade"
        ));

        // Rule for settlement method determination
        expressionRules.put("SettlementMethod", new Rule(
                "SettlementMethodRule",
                "#settlementService.determineSettlementMethod(#trade)",
                "Determine settlement method for trade"
        ));

        // Rule for market risk calculation
        expressionRules.put("MarketRisk", new Rule(
                "MarketRiskRule",
                "#riskService.calculateMarketRisk(#trade)",
                "Calculate market risk for trade"
        ));

        // Rule for pricing calculation
        expressionRules.put("StandardPrice", new Rule(
                "StandardPriceRule",
                "#pricingService.calculateStandardPrice(#basePrice)",
                "Calculate standard price"
        ));

        // Additional rules for comprehensive demonstration
        expressionRules.put("CreditRisk", new Rule(
                "CreditRiskRule",
                "#riskService.calculateCreditRisk(#trade)",
                "Calculate credit risk for trade"
        ));

        expressionRules.put("ComplianceCheck", new Rule(
                "ComplianceCheckRule",
                "#complianceService.getApplicableRegulations(#trade)",
                "Get applicable regulations for trade"
        ));

        expressionRules.put("PremiumPrice", new Rule(
                "PremiumPriceRule",
                "#pricingService.calculatePremiumPrice(#basePrice)",
                "Calculate premium price"
        ));

        expressionRules.put("SalePrice", new Rule(
                "SalePriceRule",
                "#pricingService.calculateSalePrice(#basePrice)",
                "Calculate sale price"
        ));

        expressionRules.put("ClearancePrice", new Rule(
                "ClearancePriceRule",
                "#pricingService.calculateClearancePrice(#basePrice)",
                "Calculate clearance price"
        ));
    }

    /**
     * Create an evaluation context with self-contained services.
     *
     * @return The evaluation context
     */
    public StandardEvaluationContext createContext() {
        LOGGER.info("Creating evaluation context");

        StandardEvaluationContext context = new StandardEvaluationContext();

        // Create and add sample trades
        Map<String, Trade> sampleTrades = createSampleTrades();
        context.setVariable("trades", sampleTrades);

        // Create and add services
        context.setVariable("settlementService", new SettlementService());
        context.setVariable("riskService", new RiskService());
        context.setVariable("pricingService", new PricingService());
        context.setVariable("complianceService", new ComplianceService());
        context.setVariable("notionalValue", 1000000.0);

        return context;
    }

    /**
     * Create sample trades for demonstration.
     *
     * @return A map of sample trades
     */
    private Map<String, Trade> createSampleTrades() {
        Map<String, Trade> sampleTrades = new HashMap<>();

        // Create sample trades of different types
        sampleTrades.put("equity", new Trade("T001", TYPE_EQUITY, "InstrumentType"));
        sampleTrades.put("fixedIncome", new Trade("T002", TYPE_FIXED_INCOME, "InstrumentType"));
        sampleTrades.put("derivative", new Trade("T003", TYPE_DERIVATIVE, "InstrumentType"));
        sampleTrades.put("forex", new Trade("T004", TYPE_FOREX, "InstrumentType"));
        sampleTrades.put("commodity", new Trade("T005", TYPE_COMMODITY, "InstrumentType"));

        // Create a trade with no ID for validation testing
        Trade invalidTrade = new Trade();
        invalidTrade.setValue(TYPE_EQUITY);
        invalidTrade.setCategory("InstrumentType");
        sampleTrades.put("invalid", invalidTrade);

        return sampleTrades;
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Create demo instance with default constructor
        DynamicMethodExecutionDemo demo = new DynamicMethodExecutionDemo();
        demo.runDynamicMethodExecutionDemo();
    }

    /**
     * Run the dynamic method execution demonstration.
     * This method shows the step-by-step process of creating and using dynamic method execution.
     */
    public void runDynamicMethodExecutionDemo() {
        System.out.println("Starting dynamic method execution demonstration");

        // Step 1: Create an evaluation context with self-contained services
        System.out.println("Step 1: Creating an evaluation context");
        StandardEvaluationContext context = createContext();

        // Step 2: Demonstrate settlement processing
        System.out.println("Step 2: Demonstrating settlement processing");
        demonstrateSettlementProcessing(context);

        // Step 3: Demonstrate risk management
        System.out.println("Step 3: Demonstrating risk management");
        demonstrateRiskManagement(context);

        // Step 4: Demonstrate compliance and regulatory reporting
        System.out.println("Step 4: Demonstrating compliance and regulatory reporting");
        demonstrateComplianceAndReporting(context);

        // Step 5: Demonstrate fee calculations
        System.out.println("Step 5: Demonstrating fee calculations");
        demonstrateFeeCalculations(context);

        // Step 6: Demonstrate conditional processing
        System.out.println("Step 6: Demonstrating conditional processing");
        demonstrateConditionalProcessing(context);

        // Step 7: Demonstrate rule-based processing
        System.out.println("Step 7: Demonstrating rule-based processing");
        demonstrateRuleBasedProcessing(context);

        // Step 8: Demonstrate pricing variations
        System.out.println("Step 8: Demonstrating pricing variations");
        demonstratePricingVariations(context);

        // Step 9: Demonstrate trade validation
        System.out.println("Step 9: Demonstrating trade validation");
        demonstrateTradeValidation(context);

        System.out.println("Dynamic method execution demonstration completed");
    }

    // ========================================
    // DEMO METHODS
    // ========================================

    /**
     * Demonstrate settlement processing.
     *
     * @param context The evaluation context
     */
    private void demonstrateSettlementProcessing(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 1: SETTLEMENT PROCESSING -----");

        // Example 1: Calculate settlement days for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#settlementService.calculateSettlementDays(#trades['" + tradeType + "'])";
            Integer days = evaluateSettlementProcessing(expression, context, Integer.class);
            System.out.println("Example 1." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Settlement days for " + tradeType + " trade: " + days);
        }

        // Example 2: Determine settlement method for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#settlementService.determineSettlementMethod(#trades['" + tradeType + "'])";
            String method = evaluateSettlementProcessing(expression, context, String.class);
            System.out.println("Example 2." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Settlement method for " + tradeType + " trade: " + method);
        }
    }

    /**
     * Demonstrate risk management.
     *
     * @param context The evaluation context
     */
    private void demonstrateRiskManagement(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 2: RISK MANAGEMENT -----");

        // Example 3: Calculate market risk for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateMarketRisk(#trades['" + tradeType + "'])";
            Double risk = evaluateRiskManagement(expression, context, Double.class);
            System.out.println("Example 3." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Market risk for " + tradeType + " trade: " + risk);
        }

        // Example 4: Calculate credit risk for different trade types
        for (String tradeType : tradeTypes) {
            String expression = "#riskService.calculateCreditRisk(#trades['" + tradeType + "'])";
            Double risk = evaluateRiskManagement(expression, context, Double.class);
            System.out.println("Example 4." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Credit risk for " + tradeType + " trade: " + risk);
        }
    }

    /**
     * Demonstrate compliance and regulatory reporting.
     *
     * @param context The evaluation context
     */
    private void demonstrateComplianceAndReporting(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 3: COMPLIANCE AND REGULATORY REPORTING -----");

        // Example 5: Get applicable regulations for different trade types
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#complianceService.getApplicableRegulations(#trades['" + tradeType + "'])";
            @SuppressWarnings("unchecked")
            List<String> regulations = (List<String>) evaluateComplianceAndReporting(expression, context, List.class);
            System.out.println("Example 5." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Applicable regulations for " + tradeType + " trade: " + regulations);
        }
    }

    /**
     * Demonstrate fee calculations.
     *
     * @param context The evaluation context
     */
    private void demonstrateFeeCalculations(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 4: FEE CALCULATIONS -----");

        // Example 6: Calculate standard price for different base prices
        double[] basePrices = {100.0, 500.0, 1000.0, 5000.0};
        for (int i = 0; i < basePrices.length; i++) {
            String expression = "#pricingService.calculateStandardPrice(" + basePrices[i] + ")";
            Double price = evaluateFeeCalculation(expression, context, Double.class);
            System.out.println("Example 6." + (i + 1) +
                    ": Standard price for base price $" + basePrices[i] + ": $" + price);
        }

        // Example 7: Calculate premium price for different base prices
        for (int i = 0; i < basePrices.length; i++) {
            String expression = "#pricingService.calculatePremiumPrice(" + basePrices[i] + ")";
            Double price = evaluateFeeCalculation(expression, context, Double.class);
            System.out.println("Example 7." + (i + 1) +
                    ": Premium price for base price $" + basePrices[i] + ": $" + price);
        }
    }

    /**
     * Demonstrate conditional processing.
     *
     * @param context The evaluation context
     */
    private void demonstrateConditionalProcessing(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 5: CONDITIONAL PROCESSING -----");

        // Example 8: Conditional processing based on trade type
        String[] tradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};
        for (String tradeType : tradeTypes) {
            String expression = "#trades['" + tradeType + "'].value == 'Equity' ? 'Apply equity rules' : " +
                    "#trades['" + tradeType + "'].value == 'FixedIncome' ? 'Apply fixed income rules' : " +
                    "#trades['" + tradeType + "'].value == 'Derivative' ? 'Apply derivative rules' : " +
                    "#trades['" + tradeType + "'].value == 'Forex' ? 'Apply forex rules' : " +
                    "#trades['" + tradeType + "'].value == 'Commodity' ? 'Apply commodity rules' : " +
                    "'Apply default rules'";
            String result = evaluateConditionalProcessing(expression, context, String.class);
            System.out.println("Example 8." + (tradeType.charAt(0) - 'a' + 1) +
                    ": Conditional processing for " + tradeType + " trade: " + result);
        }
    }

    /**
     * Demonstrate rule-based processing using the rules engine.
     *
     * @param context The evaluation context
     */
    private void demonstrateRuleBasedProcessing(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 6: RULE-BASED PROCESSING -----");

        // Example 9: Show available rules
        Map<String, String> availableRules = getAvailableRules();
        System.out.println("Example 9.1: Available rules:");
        for (Map.Entry<String, String> entry : availableRules.entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }

        // Example 10: Execute individual rules
        System.out.println("\nExample 9.2: Executing individual rules for equity trade:");
        context.setVariable("trade", context.lookupVariable("trades"));
        @SuppressWarnings("unchecked")
        Map<String, Object> trades = (Map<String, Object>) context.lookupVariable("trades");
        context.setVariable("trade", trades.get("equity"));
        context.setVariable("basePrice", 100.0);

        try {
            Integer settlementDays = executeRule("SettlementDays", context, Integer.class);
            System.out.println("  - Settlement Days: " + settlementDays);

            String settlementMethod = executeRule("SettlementMethod", context, String.class);
            System.out.println("  - Settlement Method: " + settlementMethod);

            Double marketRisk = executeRule("MarketRisk", context, Double.class);
            System.out.println("  - Market Risk: " + marketRisk);

            Double standardPrice = executeRule("StandardPrice", context, Double.class);
            System.out.println("  - Standard Price: " + standardPrice);
        } catch (Exception e) {
            System.out.println("  - Error executing rules: " + e.getMessage());
        }

        // Example 11: Execute all rules at once
        System.out.println("\nExample 9.3: Executing all rules for derivative trade:");
        context.setVariable("trade", trades.get("derivative"));
        context.setVariable("basePrice", 500.0);

        Map<String, Object> allResults = executeAllRules(context);
        for (Map.Entry<String, Object> entry : allResults.entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * Demonstrate pricing variations using all pricing methods.
     *
     * @param context The evaluation context
     */
    private void demonstratePricingVariations(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 7: PRICING VARIATIONS -----");

        // Example 12: Demonstrate all pricing methods for different base prices
        double[] basePrices = {100.0, 500.0, 1000.0, 2500.0};

        for (int i = 0; i < basePrices.length; i++) {
            double basePrice = basePrices[i];
            System.out.println("\nExample 10." + (i + 1) + ": Pricing variations for base price $" + basePrice + ":");

            Map<String, Double> pricingResults = demonstratePricingVariations(basePrice, context);

            System.out.println("  - Standard Price: $" + pricingResults.get("standard"));
            System.out.println("  - Premium Price (+20%): $" + pricingResults.get("premium"));
            System.out.println("  - Sale Price (-20%): $" + pricingResults.get("sale"));
            System.out.println("  - Clearance Price (-50%): $" + pricingResults.get("clearance"));

            // Calculate price spread
            double spread = pricingResults.get("premium") - pricingResults.get("clearance");
            System.out.println("  - Price Spread (Premium to Clearance): $" + spread);
        }
    }

    /**
     * Demonstrate trade validation including invalid trades.
     *
     * @param context The evaluation context
     */
    private void demonstrateTradeValidation(StandardEvaluationContext context) {
        System.out.println("\n----- CATEGORY 8: TRADE VALIDATION -----");

        @SuppressWarnings("unchecked")
        Map<String, Object> trades = (Map<String, Object>) context.lookupVariable("trades");

        // Example 13: Validate valid trades
        System.out.println("Example 11.1: Validating valid trades:");
        String[] validTradeTypes = {"equity", "fixedIncome", "derivative", "forex", "commodity"};

        for (String tradeType : validTradeTypes) {
            Object tradeObj = trades.get(tradeType);
            if (tradeObj instanceof Trade) {
                Trade trade = (Trade) tradeObj;
                Map<String, Object> validationResults = validateTrade(trade, context);

                System.out.println("  - " + tradeType.toUpperCase() + " TradeB (" + trade.getId() + "):");
                System.out.println("    * Has ID: " + validationResults.get("hasId"));
                System.out.println("    * Has Value: " + validationResults.get("hasValue"));
                System.out.println("    * Has Category: " + validationResults.get("hasCategory"));
                System.out.println("    * Settlement Days: " + validationResults.get("settlementDays"));
                System.out.println("    * Valid Settlement: " + validationResults.get("validSettlementDays"));
            }
        }

        // Example 14: Validate invalid trade
        System.out.println("\nExample 11.2: Validating invalid trade:");
        Object invalidTradeObj = trades.get("invalid");
        if (invalidTradeObj instanceof Trade) {
            Trade invalidTrade = (Trade) invalidTradeObj;
            Map<String, Object> validationResults = validateTrade(invalidTrade, context);

            System.out.println("  - INVALID TradeB:");
            System.out.println("    * Has ID: " + validationResults.get("hasId"));
            System.out.println("    * Has Value: " + validationResults.get("hasValue"));
            System.out.println("    * Has Category: " + validationResults.get("hasCategory"));
            System.out.println("    * Settlement Days: " + validationResults.get("settlementDays"));
            System.out.println("    * Valid Settlement: " + validationResults.get("validSettlementDays"));
        }

        // Example 15: Validate null trade
        System.out.println("\nExample 11.3: Validating null trade:");
        Map<String, Object> nullValidationResults = validateTrade(null, context);
        System.out.println("  - NULL TradeB:");
        System.out.println("    * Has ID: " + nullValidationResults.get("hasId"));
        System.out.println("    * Has Value: " + nullValidationResults.get("hasValue"));
        System.out.println("    * Has Category: " + nullValidationResults.get("hasCategory"));
        System.out.println("    * Settlement Days: " + nullValidationResults.get("settlementDays"));
        System.out.println("    * Valid Settlement: " + nullValidationResults.get("validSettlementDays"));
    }

    // ========================================
    // EVALUATION METHODS (from DynamicMethodExecutionDemoConfig)
    // ========================================

    /**
     * Evaluate a settlement processing expression.
     *
     * @param expression The expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateSettlementProcessing(String expression, EvaluationContext context, Class<T> resultType) {
        LOGGER.info("Evaluating settlement processing expression: " + expression);
        return evaluatorService.evaluate(expression, context, resultType);
    }

    /**
     * Evaluate a risk management expression.
     *
     * @param expression The expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateRiskManagement(String expression, EvaluationContext context, Class<T> resultType) {
        LOGGER.info("Evaluating risk management expression: " + expression);
        return evaluatorService.evaluate(expression, context, resultType);
    }

    /**
     * Evaluate a compliance and reporting expression.
     *
     * @param expression The expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateComplianceAndReporting(String expression, EvaluationContext context, Class<T> resultType) {
        LOGGER.info("Evaluating compliance and reporting expression: " + expression);
        return evaluatorService.evaluate(expression, context, resultType);
    }

    /**
     * Evaluate a fee calculation expression.
     *
     * @param expression The expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateFeeCalculation(String expression, EvaluationContext context, Class<T> resultType) {
        LOGGER.info("Evaluating fee calculation expression: " + expression);
        return evaluatorService.evaluate(expression, context, resultType);
    }

    /**
     * Evaluate a conditional processing expression.
     *
     * @param expression The expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateConditionalProcessing(String expression, EvaluationContext context, Class<T> resultType) {
        LOGGER.info("Evaluating conditional processing expression: " + expression);
        return evaluatorService.evaluate(expression, context, resultType);
    }

    /**
     * Execute a rule by name using the rules engine.
     *
     * @param ruleName The name of the rule to execute
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the rule execution
     */
    public <T> T executeRule(String ruleName, EvaluationContext context, Class<T> resultType) {
        LOGGER.info("Executing rule: " + ruleName);
        Rule rule = expressionRules.get(ruleName);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found: " + ruleName);
        }
        return evaluatorService.evaluate(rule.getCondition(), context, resultType);
    }

    /**
     * Execute all rules and return their results.
     *
     * @param context The evaluation context
     * @return A map of rule names to their execution results
     */
    public Map<String, Object> executeAllRules(EvaluationContext context) {
        LOGGER.info("Executing all rules");
        Map<String, Object> results = new HashMap<>();

        for (Map.Entry<String, Rule> entry : expressionRules.entrySet()) {
            String ruleName = entry.getKey();
            Rule rule = entry.getValue();
            try {
                Object result = evaluatorService.evaluate(rule.getCondition(), context, Object.class);
                results.put(ruleName, result);
                LOGGER.info("Rule " + ruleName + " executed successfully: " + result);
            } catch (Exception e) {
                LOGGER.warning("Failed to execute rule " + ruleName + ": " + e.getMessage());
                results.put(ruleName, "ERROR: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Validate a trade using rule-based processing.
     *
     * @param trade The trade to validate
     * @param context The evaluation context
     * @return Validation results
     */
    public Map<String, Object> validateTrade(Trade trade, EvaluationContext context) {
        LOGGER.info("Validating trade: " + (trade != null ? trade.getId() : "null"));

        Map<String, Object> validationResults = new HashMap<>();
        context.setVariable("trade", trade);

        // Basic validation rules
        validationResults.put("hasId", trade != null && trade.getId() != null && !trade.getId().trim().isEmpty());
        validationResults.put("hasValue", trade != null && trade.getValue() != null && !trade.getValue().trim().isEmpty());
        validationResults.put("hasCategory", trade != null && trade.getCategory() != null && !trade.getCategory().trim().isEmpty());

        // Execute settlement days rule for validation
        if (expressionRules.containsKey("SettlementDays")) {
            try {
                Integer settlementDays = executeRule("SettlementDays", context, Integer.class);
                validationResults.put("settlementDays", settlementDays);
                validationResults.put("validSettlementDays", settlementDays > 0);
            } catch (Exception e) {
                validationResults.put("settlementDays", "ERROR: " + e.getMessage());
                validationResults.put("validSettlementDays", false);
            }
        }

        return validationResults;
    }

    /**
     * Demonstrate pricing variations using all pricing methods.
     *
     * @param basePrice The base price to calculate variations for
     * @param context The evaluation context
     * @return A map of pricing method names to calculated prices
     */
    public Map<String, Double> demonstratePricingVariations(double basePrice, EvaluationContext context) {
        LOGGER.info("Demonstrating pricing variations for base price: " + basePrice);

        Map<String, Double> pricingResults = new HashMap<>();

        // Standard price
        String standardExpression = "#pricingService.calculateStandardPrice(" + basePrice + ")";
        Double standardPrice = evaluatorService.evaluate(standardExpression, context, Double.class);
        pricingResults.put("standard", standardPrice);

        // Premium price
        String premiumExpression = "#pricingService.calculatePremiumPrice(" + basePrice + ")";
        Double premiumPrice = evaluatorService.evaluate(premiumExpression, context, Double.class);
        pricingResults.put("premium", premiumPrice);

        // Sale price
        String saleExpression = "#pricingService.calculateSalePrice(" + basePrice + ")";
        Double salePrice = evaluatorService.evaluate(saleExpression, context, Double.class);
        pricingResults.put("sale", salePrice);

        // Clearance price
        String clearanceExpression = "#pricingService.calculateClearancePrice(" + basePrice + ")";
        Double clearancePrice = evaluatorService.evaluate(clearanceExpression, context, Double.class);
        pricingResults.put("clearance", clearancePrice);

        return pricingResults;
    }

    /**
     * Get available rules.
     *
     * @return A map of rule names to their descriptions
     */
    public Map<String, String> getAvailableRules() {
        Map<String, String> ruleDescriptions = new HashMap<>();
        for (Map.Entry<String, Rule> entry : expressionRules.entrySet()) {
            ruleDescriptions.put(entry.getKey(), entry.getValue().getDescription());
        }
        return ruleDescriptions;
    }

    /**
     * Execute rules using the formal rules engine (alternative to direct evaluation).
     * This demonstrates the difference between direct expression evaluation and
     * formal rule engine execution with full rule lifecycle management.
     *
     * @param ruleNames The names of the rules to execute
     * @param context The evaluation context
     * @return A map of rule names to their execution results
     */
    public Map<String, Object> executeRulesWithEngine(List<String> ruleNames, EvaluationContext context) {
        LOGGER.info("Executing rules using formal rules engine: " + ruleNames);
        Map<String, Object> results = new HashMap<>();

        List<Rule> rulesToExecute = new ArrayList<>();
        for (String ruleName : ruleNames) {
            Rule rule = expressionRules.get(ruleName);
            if (rule != null) {
                rulesToExecute.add(rule);
            } else {
                results.put(ruleName, "ERROR: Rule not found");
            }
        }

        if (!rulesToExecute.isEmpty()) {
            try {
                // Use the rules engine for formal rule execution
                // Note: This would typically involve the RuleEngineService, but for this demo
                // we'll show the pattern of how the RulesEngine would be used
                for (Rule rule : rulesToExecute) {
                    try {
                        Object result = evaluatorService.evaluate(rule.getCondition(), context, Object.class);
                        results.put(rule.getName(), result);
                        LOGGER.info("Rule " + rule.getName() + " executed via engine: " + result);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to execute rule " + rule.getName() + " via engine: " + e.getMessage());
                        results.put(rule.getName(), "ERROR: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Rules engine execution failed: " + e.getMessage());
                for (String ruleName : ruleNames) {
                    results.put(ruleName, "ERROR: Engine execution failed - " + e.getMessage());
                }
            }
        }

        return results;
    }

    // ========================================
    // SERVICE CLASSES (from DynamicMethodExecutionDemoConfig)
    // ========================================

    /**
     * Self-contained settlement service for demonstration.
     * Methods in this class are called dynamically via SpEL expressions.
     */
    @SuppressWarnings("unused") // Methods called dynamically via SpEL expressions
    private class SettlementService {
        private final Map<String, Integer> settlementDays = new HashMap<>();

        public SettlementService() {
            // Initialize settlement days by type
            settlementDays.put(TYPE_EQUITY, 2);
            settlementDays.put(TYPE_FIXED_INCOME, 1);
            settlementDays.put(TYPE_DERIVATIVE, 1);
            settlementDays.put(TYPE_FOREX, 2);
            settlementDays.put(TYPE_COMMODITY, 3);
        }

        public int calculateSettlementDays(Trade trade) {
            if (trade == null || trade.getValue() == null || trade.getValue().trim().isEmpty()) return 0;
            return settlementDays.getOrDefault(trade.getValue(), 2);
        }

        public String determineSettlementMethod(Trade trade) {
            if (trade == null) return METHOD_MANUAL;

            switch (trade.getValue()) {
                case TYPE_EQUITY:
                    return METHOD_DTC;
                case TYPE_FIXED_INCOME:
                    return METHOD_FEDWIRE;
                case TYPE_DERIVATIVE:
                    return METHOD_CLEARSTREAM;
                case TYPE_FOREX:
                    return METHOD_EUROCLEAR;
                case TYPE_COMMODITY:
                    return METHOD_MANUAL;
                default:
                    return METHOD_MANUAL;
            }
        }
    }

    /**
     * Self-contained risk service for demonstration.
     * Methods in this class are called dynamically via SpEL expressions.
     */
    @SuppressWarnings("unused") // Methods called dynamically via SpEL expressions
    private class RiskService {
        private final Map<String, Double> marketRiskFactors = new HashMap<>();
        private final Map<String, Double> creditRiskFactors = new HashMap<>();

        public RiskService() {
            // Initialize risk factors by type
            marketRiskFactors.put(TYPE_EQUITY, 0.15);
            marketRiskFactors.put(TYPE_FIXED_INCOME, 0.05);
            marketRiskFactors.put(TYPE_DERIVATIVE, 0.25);
            marketRiskFactors.put(TYPE_FOREX, 0.10);
            marketRiskFactors.put(TYPE_COMMODITY, 0.20);

            creditRiskFactors.put(TYPE_EQUITY, 0.10);
            creditRiskFactors.put(TYPE_FIXED_INCOME, 0.15);
            creditRiskFactors.put(TYPE_DERIVATIVE, 0.20);
            creditRiskFactors.put(TYPE_FOREX, 0.05);
            creditRiskFactors.put(TYPE_COMMODITY, 0.10);
        }

        public double calculateMarketRisk(Trade trade) {
            if (trade == null) return 0.0;
            return marketRiskFactors.getOrDefault(trade.getValue(), 0.10);
        }

        public double calculateCreditRisk(Trade trade) {
            if (trade == null) return 0.0;
            return creditRiskFactors.getOrDefault(trade.getValue(), 0.10);
        }
    }

    /**
     * Self-contained pricing service for demonstration.
     * Methods in this class are called dynamically via SpEL expressions.
     */
    @SuppressWarnings("unused") // Methods called dynamically via SpEL expressions
    private class PricingService {
        public double calculateStandardPrice(double basePrice) {
            return basePrice;
        }

        public double calculatePremiumPrice(double basePrice) {
            return basePrice * 1.20; // 20% premium
        }

        public double calculateSalePrice(double basePrice) {
            return basePrice * 0.80; // 20% discount
        }

        public double calculateClearancePrice(double basePrice) {
            return basePrice * 0.50; // 50% discount
        }
    }

    /**
     * Self-contained compliance service for demonstration.
     * Methods in this class are called dynamically via SpEL expressions.
     */
    @SuppressWarnings("unused") // Methods called dynamically via SpEL expressions
    private class ComplianceService {
        private final Map<String, List<String>> applicableRegulations = new HashMap<>();

        public ComplianceService() {
            // Initialize regulations by type
            applicableRegulations.put(TYPE_EQUITY, Arrays.asList("SEC Rule 10b-5", "Regulation SHO", "Market Access Rule"));
            applicableRegulations.put(TYPE_FIXED_INCOME, Arrays.asList("MSRB Rule G-15", "TRACE Reporting", "Best Execution"));
            applicableRegulations.put(TYPE_DERIVATIVE, Arrays.asList("Dodd-Frank Act", "EMIR", "Basel III"));
            applicableRegulations.put(TYPE_FOREX, Arrays.asList("CFTC Part 23", "MiFID II", "Basel III"));
            applicableRegulations.put(TYPE_COMMODITY, Arrays.asList("CEA Section 4s", "REMIT", "Position Limits"));
        }

        public List<String> getApplicableRegulations(Trade trade) {
            if (trade == null) return Collections.emptyList();
            return applicableRegulations.getOrDefault(trade.getValue(), Collections.emptyList());
        }
    }
}
