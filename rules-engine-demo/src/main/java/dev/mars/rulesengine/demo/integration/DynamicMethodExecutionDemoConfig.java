package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.demo.model.Trade;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.logging.Logger;

/**
 * Configuration class for dynamic method execution demonstrations.
 * This class contains all the business logic and rules for the demo.
 */
class DynamicMethodExecutionDemoConfig {
    private static final Logger LOGGER = Logger.getLogger(DynamicMethodExecutionDemoConfig.class.getName());

    private final RulesEngine rulesEngine;
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
     * Create a new DynamicMethodExecutionDemoConfig with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use
     * @param evaluatorService The expression evaluator service to use
     */
    public DynamicMethodExecutionDemoConfig(RulesEngine rulesEngine, ExpressionEvaluatorService evaluatorService) {
        this.rulesEngine = rulesEngine;
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
     * Self-contained settlement service for demonstration.
     */
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
            if (trade == null) return 0;
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
     */
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
     */
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
     */
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
