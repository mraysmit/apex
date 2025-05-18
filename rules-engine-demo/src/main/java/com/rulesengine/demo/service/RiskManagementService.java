package com.rulesengine.demo.service;

import com.rulesengine.demo.model.Trade;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for risk management operations in post-trade processing.
 * This service provides methods for risk assessment and management.
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
    
    /**
     * Create a new RiskManagementService with default values.
     */
    public RiskManagementService() {
        initializeDefaultValues();
    }
    
    /**
     * Initialize default values for risk factors.
     */
    private void initializeDefaultValues() {
        // Initialize market risk factors by trade type
        marketRiskFactors.put(PostTradeProcessingService.TYPE_EQUITY, 0.15);
        marketRiskFactors.put(PostTradeProcessingService.TYPE_FIXED_INCOME, 0.08);
        marketRiskFactors.put(PostTradeProcessingService.TYPE_DERIVATIVE, 0.25);
        marketRiskFactors.put(PostTradeProcessingService.TYPE_FOREX, 0.12);
        marketRiskFactors.put(PostTradeProcessingService.TYPE_COMMODITY, 0.18);
        
        // Initialize credit risk factors by trade type
        creditRiskFactors.put(PostTradeProcessingService.TYPE_EQUITY, 0.10);
        creditRiskFactors.put(PostTradeProcessingService.TYPE_FIXED_INCOME, 0.15);
        creditRiskFactors.put(PostTradeProcessingService.TYPE_DERIVATIVE, 0.20);
        creditRiskFactors.put(PostTradeProcessingService.TYPE_FOREX, 0.12);
        creditRiskFactors.put(PostTradeProcessingService.TYPE_COMMODITY, 0.08);
        
        // Initialize liquidity risk factors by trade type
        liquidityRiskFactors.put(PostTradeProcessingService.TYPE_EQUITY, 0.05);
        liquidityRiskFactors.put(PostTradeProcessingService.TYPE_FIXED_INCOME, 0.12);
        liquidityRiskFactors.put(PostTradeProcessingService.TYPE_DERIVATIVE, 0.18);
        liquidityRiskFactors.put(PostTradeProcessingService.TYPE_FOREX, 0.03);
        liquidityRiskFactors.put(PostTradeProcessingService.TYPE_COMMODITY, 0.15);
    }
    
    /**
     * Calculate market risk for a trade.
     * 
     * @param trade The trade
     * @return The market risk factor
     */
    public double calculateMarketRisk(Trade trade) {
        if (trade == null) return 0.0;
        
        String type = trade.getValue();
        return marketRiskFactors.getOrDefault(type, 0.1);
    }
    
    /**
     * Calculate credit risk for a trade.
     * 
     * @param trade The trade
     * @return The credit risk factor
     */
    public double calculateCreditRisk(Trade trade) {
        if (trade == null) return 0.0;
        
        String type = trade.getValue();
        return creditRiskFactors.getOrDefault(type, 0.1);
    }
    
    /**
     * Calculate liquidity risk for a trade.
     * 
     * @param trade The trade
     * @return The liquidity risk factor
     */
    public double calculateLiquidityRisk(Trade trade) {
        if (trade == null) return 0.0;
        
        String type = trade.getValue();
        return liquidityRiskFactors.getOrDefault(type, 0.1);
    }
    
    /**
     * Calculate operational risk for a trade.
     * 
     * @param trade The trade
     * @return The operational risk factor
     */
    public double calculateOperationalRisk(Trade trade) {
        if (trade == null) return 0.0;
        
        // Operational risk is often based on complexity of the trade type
        String type = trade.getValue();
        switch (type) {
            case PostTradeProcessingService.TYPE_EQUITY:
                return 0.05;
            case PostTradeProcessingService.TYPE_FIXED_INCOME:
                return 0.08;
            case PostTradeProcessingService.TYPE_DERIVATIVE:
                return 0.15;
            case PostTradeProcessingService.TYPE_FOREX:
                return 0.07;
            case PostTradeProcessingService.TYPE_COMMODITY:
                return 0.10;
            default:
                return 0.08;
        }
    }
    
    /**
     * Calculate settlement risk for a trade.
     * 
     * @param trade The trade
     * @return The settlement risk factor
     */
    public double calculateSettlementRisk(Trade trade) {
        if (trade == null) return 0.0;
        
        // Settlement risk is often based on settlement method and days
        PostTradeProcessingService postTradeService = new PostTradeProcessingService();
        String method = postTradeService.determineSettlementMethod(trade);
        int days = postTradeService.calculateSettlementDays(trade);
        
        double methodFactor;
        switch (method) {
            case PostTradeProcessingService.METHOD_DTC:
                methodFactor = 0.03;
                break;
            case PostTradeProcessingService.METHOD_FEDWIRE:
                methodFactor = 0.04;
                break;
            case PostTradeProcessingService.METHOD_EUROCLEAR:
                methodFactor = 0.05;
                break;
            case PostTradeProcessingService.METHOD_CLEARSTREAM:
                methodFactor = 0.05;
                break;
            case PostTradeProcessingService.METHOD_MANUAL:
                methodFactor = 0.10;
                break;
            default:
                methodFactor = 0.07;
        }
        
        // More days = more risk
        return methodFactor * days;
    }
    
    /**
     * Calculate total risk for a trade.
     * 
     * @param trade The trade
     * @return The total risk factor
     */
    public double calculateTotalRisk(Trade trade) {
        if (trade == null) return 0.0;
        
        double marketRisk = calculateMarketRisk(trade);
        double creditRisk = calculateCreditRisk(trade);
        double liquidityRisk = calculateLiquidityRisk(trade);
        double operationalRisk = calculateOperationalRisk(trade);
        double settlementRisk = calculateSettlementRisk(trade);
        
        return marketRisk + creditRisk + liquidityRisk + operationalRisk + settlementRisk;
    }
    
    /**
     * Determine risk level for a trade.
     * 
     * @param trade The trade
     * @return The risk level (Low, Medium, High, Extreme)
     */
    public String determineRiskLevel(Trade trade) {
        if (trade == null) return RISK_LOW;
        
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
     * Check if a trade requires additional risk review.
     * 
     * @param trade The trade
     * @return True if additional review is required, false otherwise
     */
    public boolean requiresAdditionalRiskReview(Trade trade) {
        if (trade == null) return false;
        
        String riskLevel = determineRiskLevel(trade);
        return RISK_HIGH.equals(riskLevel) || RISK_EXTREME.equals(riskLevel);
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
        
        double totalRisk = calculateTotalRisk(trade);
        return notionalValue * (1 + totalRisk);
    }
}