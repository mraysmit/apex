package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Enricher for market data in financial services.
 * This enricher adds benchmark information, market liquidity metrics, trading volume statistics, bid-ask spread information, and other market data to trades.
 */
public class MarketDataEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Map<String, MarketData> instrumentMarketData = new HashMap<>();
    private final Random random = new Random();
    
    /**
     * Create a new MarketDataEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public MarketDataEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
        initializeMarketData();
    }
    
    /**
     * Initialize market data for demonstration purposes.
     */
    private void initializeMarketData() {
        // Equity market data
        instrumentMarketData.put("Equity", new MarketData(
            "S&P 500",     // Benchmark
            "High",        // Liquidity
            5250000,       // Trading volume
            0.02,          // Bid-ask spread
            "Bullish"      // Market sentiment
        ));
        
        // Bond market data
        instrumentMarketData.put("Bond", new MarketData(
            "US 10Y Treasury",  // Benchmark
            "Medium",      // Liquidity
            1250000,       // Trading volume
            0.05,          // Bid-ask spread
            "Neutral"      // Market sentiment
        ));
        
        // Option market data
        instrumentMarketData.put("Option", new MarketData(
            "VIX",         // Benchmark
            "Low",         // Liquidity
            750000,        // Trading volume
            0.10,          // Bid-ask spread
            "Volatile"     // Market sentiment
        ));
        
        // ETF market data
        instrumentMarketData.put("ETF", new MarketData(
            "S&P 500",     // Benchmark
            "High",        // Liquidity
            3750000,       // Trading volume
            0.01,          // Bid-ask spread
            "Bullish"      // Market sentiment
        ));
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Check if we have market data for this instrument type
        MarketData marketData = instrumentMarketData.get(trade.getValue());
        if (marketData != null) {
            // Add benchmark information
            enrichedTrade.setValue(trade.getValue() + " (Benchmark: " + marketData.getBenchmark() + ")");
            
            // Add liquidity metrics
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Liquidity: " + marketData.getLiquidity() + ")");
            
            // Add trading volume
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Volume: " + formatVolume(marketData.getTradingVolume()) + ")");
            
            // Add bid-ask spread
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Spread: " + (marketData.getBidAskSpread() * 100) + "%)");
            
            // Add market sentiment
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Sentiment: " + marketData.getMarketSentiment() + ")");
        } else {
            // Generate random market data for demonstration
            String[] benchmarks = {"S&P 500", "Russell 2000", "NASDAQ", "DJIA", "FTSE 100"};
            String benchmark = benchmarks[random.nextInt(benchmarks.length)];
            
            String[] liquidityLevels = {"Low", "Medium", "High"};
            String liquidity = liquidityLevels[random.nextInt(liquidityLevels.length)];
            
            int volume = 100000 + random.nextInt(10000000);
            double spread = 0.01 + (random.nextDouble() * 0.1);
            
            String[] sentiments = {"Bullish", "Bearish", "Neutral", "Volatile"};
            String sentiment = sentiments[random.nextInt(sentiments.length)];
            
            enrichedTrade.setValue(trade.getValue() + 
                    " (Benchmark: " + benchmark + 
                    ", Liquidity: " + liquidity + 
                    ", Volume: " + formatVolume(volume) + 
                    ", Spread: " + String.format("%.2f", spread * 100) + "%" +
                    ", Sentiment: " + sentiment + ")");
        }
        
        return enrichedTrade;
    }
    
    /**
     * Format trading volume for display.
     * 
     * @param volume The trading volume
     * @return Formatted volume string
     */
    private String formatVolume(int volume) {
        if (volume >= 1000000) {
            return String.format("%.1fM", volume / 1000000.0);
        } else if (volume >= 1000) {
            return String.format("%.1fK", volume / 1000.0);
        } else {
            return String.valueOf(volume);
        }
    }
    
    /**
     * Inner class to represent market data.
     */
    private static class MarketData {
        private final String benchmark;
        private final String liquidity;
        private final int tradingVolume;
        private final double bidAskSpread;
        private final String marketSentiment;
        
        /**
         * Create new market data.
         * 
         * @param benchmark The benchmark index
         * @param liquidity The liquidity level
         * @param tradingVolume The trading volume
         * @param bidAskSpread The bid-ask spread
         * @param marketSentiment The market sentiment
         */
        public MarketData(String benchmark, String liquidity, int tradingVolume, 
                double bidAskSpread, String marketSentiment) {
            this.benchmark = benchmark;
            this.liquidity = liquidity;
            this.tradingVolume = tradingVolume;
            this.bidAskSpread = bidAskSpread;
            this.marketSentiment = marketSentiment;
        }
        
        public String getBenchmark() {
            return benchmark;
        }
        
        public String getLiquidity() {
            return liquidity;
        }
        
        public int getTradingVolume() {
            return tradingVolume;
        }
        
        public double getBidAskSpread() {
            return bidAskSpread;
        }
        
        public String getMarketSentiment() {
            return marketSentiment;
        }
    }
}