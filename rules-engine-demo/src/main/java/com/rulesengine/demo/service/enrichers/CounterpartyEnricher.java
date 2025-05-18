package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.Map;

/**
 * Enricher for counterparty data in financial services.
 * This enricher adds credit ratings, counterparty types, relationship tiers, and other counterparty data to trades.
 */
public class CounterpartyEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    
    /**
     * Create a new CounterpartyEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public CounterpartyEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Enrich with credit rating if category matches a counterparty
        Map<String, String> creditRatings = (Map<String, String>) referenceData.get("creditRatings");
        if (creditRatings != null && creditRatings.containsKey(trade.getCategory())) {
            String rating = creditRatings.get(trade.getCategory());
            enrichedTrade.setValue(trade.getValue() + " (Rating: " + rating + ")");
        }
        
        // Enrich with counterparty type if category matches a counterparty
        Map<String, String> counterpartyTypes = (Map<String, String>) referenceData.get("counterpartyTypes");
        if (counterpartyTypes != null && counterpartyTypes.containsKey(trade.getCategory())) {
            String type = counterpartyTypes.get(trade.getCategory());
            enrichedTrade.setCategory(trade.getCategory() + " (" + type + ")");
        }
        
        // Enrich with relationship tier if category matches a counterparty
        Map<String, String> relationshipTiers = (Map<String, String>) referenceData.get("relationshipTiers");
        if (relationshipTiers != null && relationshipTiers.containsKey(trade.getCategory())) {
            String tier = relationshipTiers.get(trade.getCategory());
            enrichedTrade.setId(trade.getId() + " (Tier: " + tier + ")");
        }
        
        // Add netting agreement status (simulated)
        if ("BANK123".equals(trade.getCategory())) {
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Netting: Yes)");
        } else if ("BROKER456".equals(trade.getCategory())) {
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Netting: Partial)");
        }
        
        // Add default fund contribution (simulated)
        if ("BROKER456".equals(trade.getCategory())) {
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Default Fund: $5M)");
        }
        
        return enrichedTrade;
    }
}