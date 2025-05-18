package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enricher for regulatory data in financial services.
 * This enricher adds regulatory reporting flags, transaction reporting fields, UTIs, UPIs, and other regulatory data to trades.
 */
public class RegulatoryEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    
    /**
     * Create a new RegulatoryEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public RegulatoryEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Enrich with regulatory flags if value matches an instrument type
        Map<String, List<String>> regulatoryFlags = (Map<String, List<String>>) referenceData.get("regulatoryFlags");
        if (regulatoryFlags != null && regulatoryFlags.containsKey(trade.getValue())) {
            List<String> flags = regulatoryFlags.get(trade.getValue());
            enrichedTrade.setValue(trade.getValue() + " (Regulations: " + String.join(", ", flags) + ")");
        }
        
        // Add UTI (Unique Transaction Identifier)
        String uti = "UTI-" + UUID.randomUUID().toString().substring(0, 8);
        enrichedTrade.setId(trade.getId() + " (" + uti + ")");
        
        // Add UPI (Unique Product Identifier) for derivatives
        if ("Option".equals(trade.getValue()) || "Future".equals(trade.getValue())) {
            String upi = "UPI-" + UUID.randomUUID().toString().substring(0, 8);
            enrichedTrade.setValue(enrichedTrade.getValue() + " (UPI: " + upi + ")");
        }
        
        // Add transaction reporting fields based on instrument type
        if ("Equity".equals(trade.getValue())) {
            enrichedTrade.setCategory(trade.getCategory() + " (T+2, MiFID II)");
        } else if ("Bond".equals(trade.getValue())) {
            enrichedTrade.setCategory(trade.getCategory() + " (T+1, SFTR)");
        } else if ("Option".equals(trade.getValue()) || "Future".equals(trade.getValue())) {
            enrichedTrade.setCategory(trade.getCategory() + " (T+1, EMIR)");
        }
        
        // Add legal documentation status (simulated)
        if ("Currency".equals(trade.getValue())) {
            enrichedTrade.setValue(enrichedTrade.getValue() + " (ISDA: 2002)");
        }
        
        return enrichedTrade;
    }
}