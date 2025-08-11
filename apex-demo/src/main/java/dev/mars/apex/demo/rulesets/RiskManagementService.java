package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.model.Trade;
import java.util.Collections;
import java.util.HashMap;
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
 * Service for risk management operations in post-trade processing.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class RiskManagementService {

    // Risk level constants
    public static final String RISK_LOW = "Low";
    public static final String RISK_MEDIUM = "Medium";
    public static final String RISK_HIGH = "High";
    public static final String RISK_EXTREME = "Extreme";

    // Risk types
    public static final String RISK_TYPE_MARKET = "Market";
    public static final String RISK_TYPE_CREDIT = "Credit";
    public static final String RISK_TYPE_LIQUIDITY = "Liquidity";
    public static final String RISK_TYPE_OPERATIONAL = "Operational";
    public static final String RISK_TYPE_SETTLEMENT = "Settlement";

    private final Map<String, Double> marketRiskFactors = new HashMap<>();
    private final Map<String, Double> creditRiskFactors = new HashMap<>();
    private final Map<String, Double> liquidityRiskFactors = new HashMap<>();
    private final RulesEngine rulesEngine;
    private final Map<String, Rule> riskRules = new HashMap<>();

    /**
     * Create a new RiskManagementService with default values.
     */
    public RiskManagementService() {
        initializeDefaultValues();
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        initializeRules();
    }

    /**
     * Initialize default values for risk factors.
     */
    private void initializeDefaultValues() {
        // Initialize market risk factors by trade type
        marketRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, 0.15);
        marketRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME, 0.08);
        marketRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE, 0.25);
        marketRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_FOREX, 0.12);
        marketRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY, 0.18);

        // Initialize credit risk factors by trade type
        creditRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, 0.10);
        creditRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME, 0.15);
        creditRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE, 0.20);
        creditRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_FOREX, 0.12);
        creditRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY, 0.08);

        // Initialize liquidity risk factors by trade type
        liquidityRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, 0.05);
        liquidityRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME, 0.12);
        liquidityRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE, 0.18);
        liquidityRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_FOREX, 0.03);
        liquidityRiskFactors.put(PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY, 0.15);
    }

    /**
     * Initialize rules for risk assessment.
     */
    private void initializeRules() {
        // Rules for risk factor calculations
        riskRules.put("MarketRisk", new Rule(
            "MarketRiskRule",
            "#trade != null ? #marketRiskFactors.getOrDefault(#trade.value, 0.1) : 0.0",
            "Market risk calculation"
        ));

        riskRules.put("CreditRisk", new Rule(
            "CreditRiskRule",
            "#trade != null ? #creditRiskFactors.getOrDefault(#trade.value, 0.1) : 0.0",
            "Credit risk calculation"
        ));

        riskRules.put("LiquidityRisk", new Rule(
            "LiquidityRiskRule",
            "#trade != null ? #liquidityRiskFactors.getOrDefault(#trade.value, 0.1) : 0.0",
            "Liquidity risk calculation"
        ));

        // Rule for operational risk
        riskRules.put("OperationalRisk", new Rule(
            "OperationalRiskRule",
            "#trade != null ? " +
            "(#trade.value == '" + PostTradeProcessingServiceDemoConfig.TYPE_EQUITY + "' ? 0.05 : " +
            "(#trade.value == '" + PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME + "' ? 0.08 : " +
            "(#trade.value == '" + PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE + "' ? 0.15 : " +
            "(#trade.value == '" + PostTradeProcessingServiceDemoConfig.TYPE_FOREX + "' ? 0.07 : " +
            "(#trade.value == '" + PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY + "' ? 0.10 : 0.08))))) : 0.0",
            "Operational risk calculation"
        ));

        // Rule for settlement risk
        riskRules.put("SettlementRisk", new Rule(
            "SettlementRiskRule",
            "#trade != null ? " +
            "(#settlementMethod == '" + PostTradeProcessingServiceDemoConfig.METHOD_DTC + "' ? 0.03 : " +
            "(#settlementMethod == '" + PostTradeProcessingServiceDemoConfig.METHOD_FEDWIRE + "' ? 0.04 : " +
            "(#settlementMethod == '" + PostTradeProcessingServiceDemoConfig.METHOD_EUROCLEAR + "' ? 0.05 : " +
            "(#settlementMethod == '" + PostTradeProcessingServiceDemoConfig.METHOD_CLEARSTREAM + "' ? 0.05 : " +
            "(#settlementMethod == '" + PostTradeProcessingServiceDemoConfig.METHOD_MANUAL + "' ? 0.10 : 0.07))))) * #settlementDays : 0.0",
            "Settlement risk calculation"
        ));

        // Rule for total risk
        riskRules.put("TotalRisk", new Rule(
            "TotalRiskRule",
            "#marketRisk + #creditRisk + #liquidityRisk + #operationalRisk + #settlementRisk",
            "Total risk calculation"
        ));

        // Rules for risk level determination
        riskRules.put("RiskLevelLow", new Rule(
            "RiskLevelLowRule",
            "#totalRisk < 0.3",
            "Risk level is Low"
        ));

        riskRules.put("RiskLevelMedium", new Rule(
            "RiskLevelMediumRule",
            "#totalRisk >= 0.3 && #totalRisk < 0.6",
            "Risk level is Medium"
        ));

        riskRules.put("RiskLevelHigh", new Rule(
            "RiskLevelHighRule",
            "#totalRisk >= 0.6 && #totalRisk < 0.9",
            "Risk level is High"
        ));

        riskRules.put("RiskLevelExtreme", new Rule(
            "RiskLevelExtremeRule",
            "#totalRisk >= 0.9",
            "Risk level is Extreme"
        ));

        // Rule for additional risk review
        riskRules.put("AdditionalRiskReview", new Rule(
            "AdditionalRiskReviewRule",
            "#riskLevel == '" + RISK_HIGH + "' || #riskLevel == '" + RISK_EXTREME + "'",
            "Trade requires additional risk review"
        ));
    }

    /**
     * Calculate market risk for a trade with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateMarketRiskWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("marketRiskFactors", marketRiskFactors);

        Rule rule = riskRules.get("MarketRisk");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate market risk for a trade.
     * 
     * @param trade The trade
     * @return The market risk factor
     */
    public double calculateMarketRisk(Trade trade) {
        if (trade == null) return 0.0;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        String type = trade.getValue();
        return marketRiskFactors.getOrDefault(type, 0.1);
    }

    /**
     * Calculate credit risk for a trade with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateCreditRiskWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("creditRiskFactors", creditRiskFactors);

        Rule rule = riskRules.get("CreditRisk");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate credit risk for a trade.
     * 
     * @param trade The trade
     * @return The credit risk factor
     */
    public double calculateCreditRisk(Trade trade) {
        if (trade == null) return 0.0;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        String type = trade.getValue();
        return creditRiskFactors.getOrDefault(type, 0.1);
    }

    /**
     * Calculate liquidity risk for a trade with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateLiquidityRiskWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("liquidityRiskFactors", liquidityRiskFactors);

        Rule rule = riskRules.get("LiquidityRisk");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate liquidity risk for a trade.
     * 
     * @param trade The trade
     * @return The liquidity risk factor
     */
    public double calculateLiquidityRisk(Trade trade) {
        if (trade == null) return 0.0;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        String type = trade.getValue();
        return liquidityRiskFactors.getOrDefault(type, 0.1);
    }

    /**
     * Calculate operational risk for a trade with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateOperationalRiskWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        Rule rule = riskRules.get("OperationalRisk");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate operational risk for a trade.
     * 
     * @param trade The trade
     * @return The operational risk factor
     */
    public double calculateOperationalRisk(Trade trade) {
        if (trade == null) return 0.0;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        // Operational risk is often based on complexity of the trade type
        String type = trade.getValue();
        switch (type) {
            case PostTradeProcessingServiceDemoConfig.TYPE_EQUITY:
                return 0.05;
            case PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME:
                return 0.08;
            case PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE:
                return 0.15;
            case PostTradeProcessingServiceDemoConfig.TYPE_FOREX:
                return 0.07;
            case PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY:
                return 0.10;
            default:
                return 0.08;
        }
    }

    /**
     * Calculate settlement risk for a trade with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateSettlementRiskWithResult(Trade trade) {
        if (trade == null) {
            return RuleResult.match("SettlementRiskRule", "0.0");
        }

        // Get settlement method and days
        PostTradeProcessingServiceDemoConfig postTradeConfig = new PostTradeProcessingServiceDemoConfig(rulesEngine);
        String settlementMethod = postTradeConfig.determineSettlementMethod(trade);
        int settlementDays = postTradeConfig.calculateSettlementDays(trade);

        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("settlementMethod", settlementMethod);
        facts.put("settlementDays", settlementDays);

        Rule rule = riskRules.get("SettlementRisk");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate settlement risk for a trade.
     * 
     * @param trade The trade
     * @return The settlement risk factor
     */
    public double calculateSettlementRisk(Trade trade) {
        if (trade == null) return 0.0;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        // Settlement risk is often based on settlement method and days
        PostTradeProcessingServiceDemoConfig postTradeConfig = new PostTradeProcessingServiceDemoConfig(rulesEngine);
        String method = postTradeConfig.determineSettlementMethod(trade);
        int days = postTradeConfig.calculateSettlementDays(trade);

        double methodFactor;
        switch (method) {
            case PostTradeProcessingServiceDemoConfig.METHOD_DTC:
                methodFactor = 0.03;
                break;
            case PostTradeProcessingServiceDemoConfig.METHOD_FEDWIRE:
                methodFactor = 0.04;
                break;
            case PostTradeProcessingServiceDemoConfig.METHOD_EUROCLEAR:
                methodFactor = 0.05;
                break;
            case PostTradeProcessingServiceDemoConfig.METHOD_CLEARSTREAM:
                methodFactor = 0.05;
                break;
            case PostTradeProcessingServiceDemoConfig.METHOD_MANUAL:
                methodFactor = 0.10;
                break;
            default:
                methodFactor = 0.07;
        }

        // More days = more risk
        return methodFactor * days;
    }

    /**
     * Calculate total risk for a trade with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateTotalRiskWithResult(Trade trade) {
        if (trade == null) {
            return RuleResult.match("TotalRiskRule", "0.0");
        }

        // Calculate individual risk factors
        double marketRisk = calculateMarketRisk(trade);
        double creditRisk = calculateCreditRisk(trade);
        double liquidityRisk = calculateLiquidityRisk(trade);
        double operationalRisk = calculateOperationalRisk(trade);
        double settlementRisk = calculateSettlementRisk(trade);

        Map<String, Object> facts = new HashMap<>();
        facts.put("marketRisk", marketRisk);
        facts.put("creditRisk", creditRisk);
        facts.put("liquidityRisk", liquidityRisk);
        facts.put("operationalRisk", operationalRisk);
        facts.put("settlementRisk", settlementRisk);

        Rule rule = riskRules.get("TotalRisk");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Calculate total risk for a trade.
     * 
     * @param trade The trade
     * @return The total risk factor
     */
    public double calculateTotalRisk(Trade trade) {
        if (trade == null) return 0.0;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        double marketRisk = calculateMarketRisk(trade);
        double creditRisk = calculateCreditRisk(trade);
        double liquidityRisk = calculateLiquidityRisk(trade);
        double operationalRisk = calculateOperationalRisk(trade);
        double settlementRisk = calculateSettlementRisk(trade);

        return marketRisk + creditRisk + liquidityRisk + operationalRisk + settlementRisk;
    }

    /**
     * Determine risk level for a trade with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult determineRiskLevelWithResult(Trade trade) {
        if (trade == null) {
            return RuleResult.match("RiskLevelLowRule", "Risk level is Low");
        }

        // Calculate total risk
        double totalRisk = calculateTotalRisk(trade);

        Map<String, Object> facts = new HashMap<>();
        facts.put("totalRisk", totalRisk);

        // Check each risk level rule in order
        if (totalRisk < 0.3) {
            Rule rule = riskRules.get("RiskLevelLow");
            return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
        } else if (totalRisk < 0.6) {
            Rule rule = riskRules.get("RiskLevelMedium");
            return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
        } else if (totalRisk < 0.9) {
            Rule rule = riskRules.get("RiskLevelHigh");
            return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
        } else {
            Rule rule = riskRules.get("RiskLevelExtreme");
            return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
        }
    }

    /**
     * Determine risk level for a trade.
     * 
     * @param trade The trade
     * @return The risk level (Low, Medium, High, Extreme)
     */
    public String determineRiskLevel(Trade trade) {
        if (trade == null) return RISK_LOW;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        double totalRisk = calculateTotalRisk(trade);

        if (totalRisk < 0.3) {
            return RISK_LOW;
        } else if (totalRisk < 0.6) {
            return RISK_MEDIUM;
        } else if (totalRisk < 0.9) {
            return RISK_HIGH;
        } else {
            return RISK_EXTREME;
        }
    }

    /**
     * Check if a trade requires additional risk review with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult requiresAdditionalRiskReviewWithResult(Trade trade) {
        if (trade == null) {
            return RuleResult.noMatch();
        }

        // Get risk level
        String riskLevel = determineRiskLevel(trade);

        Map<String, Object> facts = new HashMap<>();
        facts.put("riskLevel", riskLevel);

        Rule rule = riskRules.get("AdditionalRiskReview");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Check if a trade requires additional risk review.
     * 
     * @param trade The trade
     * @return True if additional review is required, false otherwise
     */
    public boolean requiresAdditionalRiskReview(Trade trade) {
        RuleResult result = requiresAdditionalRiskReviewWithResult(trade);
        return result.isTriggered();
    }

    /**
     * Calculate risk-weighted value for a trade with detailed result.
     * 
     * @param trade The trade
     * @param notionalValue The notional value of the trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult calculateRiskWeightedValueWithResult(Trade trade, double notionalValue) {
        if (trade == null) {
            return RuleResult.match("RiskWeightedValueRule", "0.0");
        }

        // Calculate total risk
        double totalRisk = calculateTotalRisk(trade);

        // Create a custom rule for risk-weighted value calculation
        Rule riskWeightedValueRule = new Rule(
            "RiskWeightedValueRule",
            "#notionalValue * (1 + #totalRisk)",
            "Risk-weighted value calculation"
        );

        Map<String, Object> facts = new HashMap<>();
        facts.put("notionalValue", notionalValue);
        facts.put("totalRisk", totalRisk);

        return rulesEngine.executeRulesList(Collections.singletonList(riskWeightedValueRule), facts);
    }

    /**
     * Calculate risk-weighted value for a trade.
     * 
     * @param trade The trade
     * @param notionalValue The notional value of the trade
     * @return The risk-weighted value
     */
    public double calculateRiskWeightedValue(Trade trade, double notionalValue) {
        if (trade == null) return 0.0;

        // Use direct calculation for now, as RuleResult doesn't provide a way to get the actual value
        double totalRisk = calculateTotalRisk(trade);
        return notionalValue * (1 + totalRisk);
    }
}
