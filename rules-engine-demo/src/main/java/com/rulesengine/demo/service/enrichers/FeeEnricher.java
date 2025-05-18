package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.Map;
import java.util.Random;

/**
 * Enricher for fee and commission data in financial services.
 * This enricher adds broker commissions, clearing fees, custody fees, transaction taxes, and other fee data to trades.
 */
public class FeeEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Random random = new Random();
    
    /**
     * Create a new FeeEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public FeeEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Calculate broker commission (0.1% for equities, 0.05% for bonds, 0.2% for options)
        double commission = 0.0;
        if ("Equity".equals(trade.getValue())) {
            commission = 0.001;
        } else if ("Bond".equals(trade.getValue())) {
            commission = 0.0005;
        } else if ("Option".equals(trade.getValue())) {
            commission = 0.002;
        } else if ("Future".equals(trade.getValue())) {
            commission = 0.0015;
        } else {
            commission = 0.001;
        }
        
        // Add UK stamp duty for LSE trades
        if ("LSE".equals(trade.getCategory())) {
            commission += 0.005; // 0.5% stamp duty
        }
        
        enrichedTrade.setValue(trade.getValue() + " (Commission: " + (commission * 100) + "%)");
        
        // Add clearing fee for derivatives
        if ("Option".equals(trade.getValue()) || "Future".equals(trade.getValue())) {
            double clearingFee = 0.0002; // 0.02% clearing fee
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Clearing: " + (clearingFee * 100) + "%)");
        }
        
        // Add custody fee for securities
        if ("Equity".equals(trade.getValue()) || "Bond".equals(trade.getValue()) || "ETF".equals(trade.getValue())) {
            double custodyFee = 0.0001; // 0.01% custody fee
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Custody: " + (custodyFee * 100) + "%)");
        }
        
        // Add exchange fee for exchange-traded instruments
        if ("Equity".equals(trade.getValue()) || "ETF".equals(trade.getValue()) || "Future".equals(trade.getValue())) {
            double exchangeFee = 0.00005; // 0.005% exchange fee
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Exchange: " + (exchangeFee * 100) + "%)");
        }
        
        // Add transaction tax for specific venues
        if ("NYSE".equals(trade.getCategory())) {
            double secFee = 0.0000231; // SEC fee
            enrichedTrade.setValue(enrichedTrade.getValue() + " (SEC Fee: " + (secFee * 100) + "%)");
        } else if ("LSE".equals(trade.getCategory())) {
            double ptrFee = 0.001; // PTM levy
            enrichedTrade.setValue(enrichedTrade.getValue() + " (PTM Levy: " + (ptrFee * 100) + "%)");
        }
        
        return enrichedTrade;
    }
}