package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.List;
import java.util.Map;

/**
 * Enricher for reference data in financial services.
 * This enricher adds LEI codes, ISIN codes, MIC codes, and other reference data to trades.
 */
public class ReferenceDataEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    
    /**
     * Create a new ReferenceDataEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public ReferenceDataEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Enrich with LEI code if category matches a counterparty
        Map<String, String> leiCodes = (Map<String, String>) referenceData.get("leiCodes");
        if (leiCodes != null && leiCodes.containsKey(trade.getCategory())) {
            String lei = leiCodes.get(trade.getCategory());
            enrichedTrade.setValue(trade.getValue() + " (LEI: " + lei + ")");
        }
        
        // Enrich with ISIN code if value matches a security
        Map<String, String> isinCodes = (Map<String, String>) referenceData.get("isinCodes");
        if (isinCodes != null && isinCodes.containsKey(trade.getValue())) {
            String isin = isinCodes.get(trade.getValue());
            enrichedTrade.setValue(trade.getValue() + " (ISIN: " + isin + ")");
        }
        
        // Enrich with MIC code if category matches an exchange
        Map<String, String> micCodes = (Map<String, String>) referenceData.get("micCodes");
        if (micCodes != null && micCodes.containsKey(trade.getCategory())) {
            String mic = micCodes.get(trade.getCategory());
            enrichedTrade.setCategory(trade.getCategory() + " (MIC: " + mic + ")");
        }
        
        // Enrich with BIC/SWIFT code if category matches a counterparty
        Map<String, String> bicCodes = (Map<String, String>) referenceData.get("bicCodes");
        if (bicCodes != null && bicCodes.containsKey(trade.getCategory())) {
            String bic = bicCodes.get(trade.getCategory());
            enrichedTrade.setCategory(trade.getCategory() + " (BIC: " + bic + ")");
        }
        
        return enrichedTrade;
    }
}