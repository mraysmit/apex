package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Enricher for corporate action data in financial services.
 * This enricher adds ex-dates, record dates, payment dates, corporate action types, and other corporate action data to trades.
 */
public class CorporateActionEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Map<String, CorporateAction> corporateActions = new HashMap<>();
    
    /**
     * Create a new CorporateActionEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public CorporateActionEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
        initializeCorporateActions();
    }
    
    /**
     * Initialize corporate actions for demonstration purposes.
     */
    private void initializeCorporateActions() {
        // Apple dividend
        corporateActions.put("APPLE", new CorporateAction(
            "Dividend",
            LocalDate.now().plusDays(10),  // Ex-date
            LocalDate.now().plusDays(12),  // Record date
            LocalDate.now().plusDays(25),  // Payment date
            0.23  // $0.23 per share
        ));
        
        // Microsoft stock split
        corporateActions.put("MICROSOFT", new CorporateAction(
            "Stock Split",
            LocalDate.now().plusDays(15),  // Ex-date
            LocalDate.now().plusDays(17),  // Record date
            LocalDate.now().plusDays(20),  // Effective date
            2.0  // 2-for-1 split
        ));
        
        // Amazon special dividend
        corporateActions.put("AMAZON", new CorporateAction(
            "Special Dividend",
            LocalDate.now().plusDays(5),  // Ex-date
            LocalDate.now().plusDays(7),  // Record date
            LocalDate.now().plusDays(30),  // Payment date
            1.0  // $1.00 per share
        ));
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Check if there's a corporate action for this security
        CorporateAction action = corporateActions.get(trade.getValue());
        if (action != null) {
            // Add corporate action information
            enrichedTrade.setValue(trade.getValue() + " (" + action.getType() + ")");
            
            // Add ex-date information
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Ex-date: " + action.getExDate() + ")");
            
            // Add record date information
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Record: " + action.getRecordDate() + ")");
            
            // Add payment/effective date information
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Payment: " + action.getPaymentDate() + ")");
            
            // Add entitlement information
            if ("Dividend".equals(action.getType()) || "Special Dividend".equals(action.getType())) {
                enrichedTrade.setValue(enrichedTrade.getValue() + 
                        " (Amount: $" + action.getValue() + " per share)");
            } else if ("Stock Split".equals(action.getType())) {
                enrichedTrade.setValue(enrichedTrade.getValue() + 
                        " (Ratio: " + action.getValue() + "-for-1)");
            }
        }
        
        return enrichedTrade;
    }
    
    /**
     * Inner class to represent a corporate action.
     */
    private static class CorporateAction {
        private final String type;
        private final LocalDate exDate;
        private final LocalDate recordDate;
        private final LocalDate paymentDate;
        private final double value;
        
        /**
         * Create a new corporate action.
         * 
         * @param type The type of corporate action (e.g., "Dividend", "Stock Split")
         * @param exDate The ex-date
         * @param recordDate The record date
         * @param paymentDate The payment date
         * @param value The value (e.g., dividend amount, split ratio)
         */
        public CorporateAction(String type, LocalDate exDate, LocalDate recordDate, LocalDate paymentDate, double value) {
            this.type = type;
            this.exDate = exDate;
            this.recordDate = recordDate;
            this.paymentDate = paymentDate;
            this.value = value;
        }
        
        public String getType() {
            return type;
        }
        
        public LocalDate getExDate() {
            return exDate;
        }
        
        public LocalDate getRecordDate() {
            return recordDate;
        }
        
        public LocalDate getPaymentDate() {
            return paymentDate;
        }
        
        public double getValue() {
            return value;
        }
    }
}