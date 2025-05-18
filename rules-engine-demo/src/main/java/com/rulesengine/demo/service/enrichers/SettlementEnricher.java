package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.time.LocalDate;
import java.util.Map;
import java.util.Random;

/**
 * Enricher for settlement data in financial services.
 * This enricher adds settlement dates, settlement methods, priority flags, custodian information, and other settlement data to trades.
 */
public class SettlementEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Random random = new Random();
    
    /**
     * Create a new SettlementEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public SettlementEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Enrich with settlement days if value matches an instrument type
        Map<String, Integer> settlementDays = (Map<String, Integer>) referenceData.get("settlementDays");
        if (settlementDays != null && settlementDays.containsKey(trade.getValue())) {
            Integer days = settlementDays.get(trade.getValue());
            LocalDate settlementDate = LocalDate.now().plusDays(days);
            enrichedTrade.setValue(trade.getValue() + " (Settles: T+" + days + ", " + settlementDate + ")");
        }
        
        // Enrich with settlement method if value matches an instrument type
        Map<String, String> settlementMethods = (Map<String, String>) referenceData.get("settlementMethods");
        if (settlementMethods != null && settlementMethods.containsKey(trade.getValue())) {
            String method = settlementMethods.get(trade.getValue());
            enrichedTrade.setCategory(trade.getCategory() + " (Method: " + method + ")");
        }
        
        // Add settlement priority flags (simulated)
        String priority = "Normal";
        if ("Equity".equals(trade.getValue()) && random.nextDouble() < 0.2) {
            priority = "High";
        } else if ("Bond".equals(trade.getValue()) && random.nextDouble() < 0.1) {
            priority = "Urgent";
        }
        enrichedTrade.setValue(enrichedTrade.getValue() + " (Priority: " + priority + ")");
        
        // Add custodian information (simulated)
        String custodian = null;
        if ("ETF".equals(trade.getValue())) {
            custodian = "State Street";
        } else if ("Equity".equals(trade.getValue())) {
            custodian = "BNY Mellon";
        } else if ("Bond".equals(trade.getValue())) {
            custodian = "JP Morgan";
        }
        
        if (custodian != null) {
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Custodian: " + custodian + ")");
        }
        
        // Add depository information (simulated)
        String depository = null;
        if ("Future".equals(trade.getValue())) {
            depository = "CME";
        } else if ("Equity".equals(trade.getValue())) {
            depository = "DTC";
        } else if ("Bond".equals(trade.getValue())) {
            depository = "Euroclear";
        }
        
        if (depository != null) {
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Depository: " + depository + ")");
        }
        
        return enrichedTrade;
    }
}