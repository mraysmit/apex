package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.Map;
import java.util.Random;

/**
 * Enricher for risk data in financial services.
 * This enricher adds VaR metrics, exposure calculations, margin requirements, collateral eligibility, and other risk data to trades.
 */
public class RiskEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Random random = new Random();
    
    /**
     * Create a new RiskEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public RiskEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Enrich with VaR metrics if value matches an instrument type
        Map<String, Double> varMetrics = (Map<String, Double>) referenceData.get("varMetrics");
        if (varMetrics != null && varMetrics.containsKey(trade.getValue())) {
            Double var = varMetrics.get(trade.getValue());
            enrichedTrade.setValue(trade.getValue() + " (VaR: " + var + ")");
        }
        
        // Add exposure calculations (simulated)
        double exposure = 0.0;
        if ("Equity".equals(trade.getValue())) {
            exposure = 100000.0 * random.nextDouble();
        } else if ("Bond".equals(trade.getValue())) {
            exposure = 500000.0 * random.nextDouble();
        } else if ("Option".equals(trade.getValue())) {
            exposure = 250000.0 * random.nextDouble();
        } else {
            exposure = 50000.0 * random.nextDouble();
        }
        enrichedTrade.setValue(enrichedTrade.getValue() + " (Exposure: $" + String.format("%.2f", exposure) + ")");
        
        // Add margin requirements for derivatives
        if ("Option".equals(trade.getValue()) || "Future".equals(trade.getValue())) {
            double initialMargin = exposure * 0.1; // 10% initial margin
            double variationMargin = exposure * 0.05; // 5% variation margin
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (IM: $" + String.format("%.2f", initialMargin) + 
                    ", VM: $" + String.format("%.2f", variationMargin) + ")");
        }
        
        // Add collateral eligibility (simulated)
        if ("Future".equals(trade.getValue())) {
            enrichedTrade.setCategory(trade.getCategory() + " (Collateral: Cash, Treasuries)");
        } else if ("Option".equals(trade.getValue())) {
            enrichedTrade.setCategory(trade.getCategory() + " (Collateral: Cash only)");
        }
        
        // Add stress test results (simulated)
        if ("Currency".equals(trade.getValue())) {
            double stressLoss = exposure * 0.2; // 20% loss under stress
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Stress Loss: $" + String.format("%.2f", stressLoss) + ")");
        }
        
        return enrichedTrade;
    }
}