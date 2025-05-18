package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Enricher for pricing and valuation data in financial services.
 * This enricher adds mark-to-market values, yield calculations, accrued interest, volatility metrics, and other pricing data to trades.
 */
public class PricingEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Map<String, SecurityPrice> securityPrices = new HashMap<>();
    private final Random random = new Random();
    
    /**
     * Create a new PricingEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public PricingEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
        initializeSecurityPrices();
    }
    
    /**
     * Initialize security prices for demonstration purposes.
     */
    private void initializeSecurityPrices() {
        // Apple stock
        securityPrices.put("APPLE", new SecurityPrice(
            175.25,  // Price
            0.0,     // Yield (not applicable for stocks)
            0.0,     // Accrued interest (not applicable for stocks)
            0.25,    // Volatility
            "Bloomberg"  // Price source
        ));
        
        // Microsoft stock
        securityPrices.put("MICROSOFT", new SecurityPrice(
            325.50,  // Price
            0.0,     // Yield (not applicable for stocks)
            0.0,     // Accrued interest (not applicable for stocks)
            0.22,    // Volatility
            "Reuters"  // Price source
        ));
        
        // US Treasury Bond
        securityPrices.put("Bond", new SecurityPrice(
            98.75,   // Price
            3.5,     // Yield
            0.75,    // Accrued interest
            0.08,    // Volatility
            "ICE Data Services"  // Price source
        ));
        
        // S&P 500 ETF
        securityPrices.put("ETF", new SecurityPrice(
            450.25,  // Price
            1.8,     // Yield (dividend yield)
            0.0,     // Accrued interest (not applicable for ETFs)
            0.18,    // Volatility
            "Bloomberg"  // Price source
        ));
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Check if we have pricing data for this security
        SecurityPrice price = securityPrices.get(trade.getValue());
        if (price != null) {
            // Add mark-to-market value
            enrichedTrade.setValue(trade.getValue() + " (Price: $" + price.getPrice() + ")");
            
            // Add price source information
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Source: " + price.getPriceSource() + ")");
            
            // Add volatility metrics
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Vol: " + (price.getVolatility() * 100) + "%)");
            
            // Add yield for fixed income and ETFs
            if (price.getYield() > 0) {
                enrichedTrade.setValue(enrichedTrade.getValue() + " (Yield: " + price.getYield() + "%)");
            }
            
            // Add accrued interest for bonds
            if (price.getAccruedInterest() > 0) {
                enrichedTrade.setValue(enrichedTrade.getValue() + " (AI: $" + price.getAccruedInterest() + ")");
            }
        } else {
            // Generate random pricing data for demonstration
            double randomPrice = 100.0 + (random.nextDouble() * 900.0);
            double randomVolatility = 0.05 + (random.nextDouble() * 0.3);
            
            enrichedTrade.setValue(trade.getValue() + " (Est. Price: $" + String.format("%.2f", randomPrice) + 
                    ", Vol: " + String.format("%.2f", randomVolatility * 100) + "%)");
        }
        
        return enrichedTrade;
    }
    
    /**
     * Inner class to represent security pricing data.
     */
    private static class SecurityPrice {
        private final double price;
        private final double yield;
        private final double accruedInterest;
        private final double volatility;
        private final String priceSource;
        
        /**
         * Create a new security price.
         * 
         * @param price The current market price
         * @param yield The yield (for fixed income securities)
         * @param accruedInterest The accrued interest (for fixed income securities)
         * @param volatility The volatility metric
         * @param priceSource The source of the price data
         */
        public SecurityPrice(double price, double yield, double accruedInterest, double volatility, String priceSource) {
            this.price = price;
            this.yield = yield;
            this.accruedInterest = accruedInterest;
            this.volatility = volatility;
            this.priceSource = priceSource;
        }
        
        public double getPrice() {
            return price;
        }
        
        public double getYield() {
            return yield;
        }
        
        public double getAccruedInterest() {
            return accruedInterest;
        }
        
        public double getVolatility() {
            return volatility;
        }
        
        public String getPriceSource() {
            return priceSource;
        }
    }
}