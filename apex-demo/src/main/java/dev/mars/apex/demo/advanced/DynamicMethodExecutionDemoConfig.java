package dev.mars.apex.demo.advanced;

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
 * Configuration class for dynamic method execution demonstrations.
 * This class contains all the business logic and rules for the demo,
 * providing comprehensive examples of rule-based processing for financial trades.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class DynamicMethodExecutionDemoConfig {
    private static final Logger LOGGER = Logger.getLogger(DynamicMethodExecutionDemoConfig.class.getName());

    private final ExpressionEvaluatorService evaluatorService;
    private final Map<String, Rule> expressionRules = new HashMap<>();

    // Trade types constants
    private static final String TYPE_EQUITY = "Equity";
    private static final String TYPE_FIXED_INCOME = "FixedIncome";
    private static final String TYPE_DERIVATIVE = "Derivative";
    private static final String TYPE_FOREX = "Forex";
    private static final String TYPE_COMMODITY = "Commodity";

    // Settlement methods constants
    private static final String METHOD_DTC = "DTC";
    private static final String METHOD_FEDWIRE = "Fedwire";
    private static final String METHOD_EUROCLEAR = "Euroclear";
    private static final String METHOD_CLEARSTREAM = "Clearstream";
    private static final String METHOD_MANUAL = "Manual";

    /**
     * Create a new DynamicMethodExecutionDemoConfig with the specified evaluator service.
     *
     * @param evaluatorService The expression evaluator service to use
     */
    public DynamicMethodExecutionDemoConfig(ExpressionEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
        initializeRules();
    }

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
     * Self-contained compliance service for demonstration.
     * Methods in this class are called dynamically via SpEL expressions.
     */
    @SuppressWarnings("unused") // Methods called dynamically via SpEL expressions
    private class ComplianceService {
        private final Map<String, List<String>> regulatoryRequirements = new HashMap<>();

        public ComplianceService() {
            // Initialize regulatory requirements by trade type
            regulatoryRequirements.put(TYPE_EQUITY, Arrays.asList("MiFID II", "Basel III"));
            regulatoryRequirements.put(TYPE_FIXED_INCOME, Arrays.asList("MiFID II", "Basel III", "SFTR"));
            regulatoryRequirements.put(TYPE_DERIVATIVE, Arrays.asList("MiFID II", "EMIR", "Dodd-Frank"));
            regulatoryRequirements.put(TYPE_FOREX, Arrays.asList("MiFID II", "Dodd-Frank"));
            regulatoryRequirements.put(TYPE_COMMODITY, Arrays.asList("MiFID II", "EMIR"));
        }

        public List<String> getApplicableRegulations(Trade trade) {
            if (trade == null) return Collections.emptyList();
            return regulatoryRequirements.getOrDefault(trade.getValue(), Collections.emptyList());
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
            return basePrice * 1.2;
        }

        public double calculateSalePrice(double basePrice) {
            return basePrice * 0.8;
        }

        public double calculateClearancePrice(double basePrice) {
            return basePrice * 0.5;
        }
    }
}
