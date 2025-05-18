package com.rulesengine.demo.service;

import com.rulesengine.demo.model.Trade;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for post-trade processing operations.
 * This service provides methods for various post-trade processing tasks.
 */
public class PostTradeProcessingService {
    
    // Settlement status constants
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_MATCHED = "Matched";
    public static final String STATUS_AFFIRMED = "Affirmed";
    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_SETTLED = "Settled";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_CANCELLED = "Cancelled";
    
    // Trade types
    public static final String TYPE_EQUITY = "Equity";
    public static final String TYPE_FIXED_INCOME = "FixedIncome";
    public static final String TYPE_DERIVATIVE = "Derivative";
    public static final String TYPE_FOREX = "Forex";
    public static final String TYPE_COMMODITY = "Commodity";
    
    // Settlement methods
    public static final String METHOD_DTC = "DTC";
    public static final String METHOD_FEDWIRE = "Fedwire";
    public static final String METHOD_EUROCLEAR = "Euroclear";
    public static final String METHOD_CLEARSTREAM = "Clearstream";
    public static final String METHOD_MANUAL = "Manual";
    
    private final Map<String, Double> settlementFees = new HashMap<>();
    private final Map<String, Integer> settlementDays = new HashMap<>();
    
    /**
     * Create a new PostTradeProcessingService with default values.
     */
    public PostTradeProcessingService() {
        initializeDefaultValues();
    }
    
    /**
     * Initialize default values for settlement fees and days.
     */
    private void initializeDefaultValues() {
        // Initialize settlement fees by type
        settlementFees.put(TYPE_EQUITY, 1.50);
        settlementFees.put(TYPE_FIXED_INCOME, 2.25);
        settlementFees.put(TYPE_DERIVATIVE, 3.00);
        settlementFees.put(TYPE_FOREX, 1.00);
        settlementFees.put(TYPE_COMMODITY, 2.50);
        
        // Initialize settlement days by type
        settlementDays.put(TYPE_EQUITY, 2);
        settlementDays.put(TYPE_FIXED_INCOME, 1);
        settlementDays.put(TYPE_DERIVATIVE, 1);
        settlementDays.put(TYPE_FOREX, 2);
        settlementDays.put(TYPE_COMMODITY, 3);
    }
    
    /**
     * Calculate settlement fee for a trade.
     * 
     * @param trade The trade
     * @return The settlement fee
     */
    public double calculateSettlementFee(Trade trade) {
        String type = trade.getValue();
        return settlementFees.getOrDefault(type, 2.0);
    }
    
    /**
     * Calculate settlement days for a trade.
     * 
     * @param trade The trade
     * @return The number of days until settlement
     */
    public int calculateSettlementDays(Trade trade) {
        String type = trade.getValue();
        return settlementDays.getOrDefault(type, 2);
    }
    
    /**
     * Validate a trade for settlement.
     * 
     * @param trade The trade to validate
     * @return True if the trade is valid for settlement, false otherwise
     */
    public boolean validateTradeForSettlement(Trade trade) {
        return trade != null && trade.getId() != null && !trade.getId().isEmpty();
    }
    
    /**
     * Match a trade with counterparty.
     * 
     * @param trade The trade to match
     * @return True if the trade was matched, false otherwise
     */
    public boolean matchTradeWithCounterparty(Trade trade) {
        // In a real system, this would involve complex matching logic
        return trade != null && !trade.getId().equals("Unknown");
    }
    
    /**
     * Affirm a trade.
     * 
     * @param trade The trade to affirm
     * @return True if the trade was affirmed, false otherwise
     */
    public boolean affirmTrade(Trade trade) {
        // In a real system, this would involve confirmation from both parties
        return trade != null && matchTradeWithCounterparty(trade);
    }
    
    /**
     * Confirm a trade.
     * 
     * @param trade The trade to confirm
     * @return True if the trade was confirmed, false otherwise
     */
    public boolean confirmTrade(Trade trade) {
        // In a real system, this would involve legal confirmation
        return trade != null && affirmTrade(trade);
    }
    
    /**
     * Settle a trade.
     * 
     * @param trade The trade to settle
     * @return True if the trade was settled, false otherwise
     */
    public boolean settleTrade(Trade trade) {
        // In a real system, this would involve actual money/asset transfer
        return trade != null && confirmTrade(trade);
    }
    
    /**
     * Get the appropriate settlement method for a trade.
     * 
     * @param trade The trade
     * @return The settlement method
     */
    public String determineSettlementMethod(Trade trade) {
        if (trade == null) return METHOD_MANUAL;
        
        String type = trade.getValue();
        switch (type) {
            case TYPE_EQUITY:
                return METHOD_DTC;
            case TYPE_FIXED_INCOME:
                return METHOD_FEDWIRE;
            case TYPE_DERIVATIVE:
                return METHOD_CLEARSTREAM;
            case TYPE_FOREX:
                return METHOD_EUROCLEAR;
            default:
                return METHOD_MANUAL;
        }
    }
    
    /**
     * Calculate clearing fee for a trade.
     * 
     * @param trade The trade
     * @return The clearing fee
     */
    public double calculateClearingFee(Trade trade) {
        if (trade == null) return 0.0;
        
        String type = trade.getValue();
        double baseFee = settlementFees.getOrDefault(type, 2.0);
        return baseFee * 0.5; // Clearing fee is typically half of settlement fee
    }
    
    /**
     * Calculate custody fee for a trade.
     * 
     * @param trade The trade
     * @return The custody fee
     */
    public double calculateCustodyFee(Trade trade) {
        if (trade == null) return 0.0;
        
        String type = trade.getValue();
        double baseFee = settlementFees.getOrDefault(type, 2.0);
        return baseFee * 0.25; // Custody fee is typically quarter of settlement fee
    }
}