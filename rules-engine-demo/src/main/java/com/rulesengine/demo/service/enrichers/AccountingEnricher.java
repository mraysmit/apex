package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Enricher for accounting data in financial services.
 * This enricher adds general ledger codes, cost basis information, P&L attribution, tax lot identification, and other accounting data to trades.
 */
public class AccountingEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Map<String, AccountingData> instrumentAccountingData = new HashMap<>();
    private final Random random = new Random();
    
    /**
     * Create a new AccountingEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public AccountingEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
        initializeAccountingData();
    }
    
    /**
     * Initialize accounting data for demonstration purposes.
     */
    private void initializeAccountingData() {
        // Equity accounting data
        instrumentAccountingData.put("Equity", new AccountingData(
            "GL-1001-EQ",  // General ledger code
            "FIFO",        // Tax lot method
            "Trading",     // Accounting treatment
            95.25,         // Cost basis
            2.75           // P&L
        ));
        
        // Bond accounting data
        instrumentAccountingData.put("Bond", new AccountingData(
            "GL-2001-FI",  // General ledger code
            "LIFO",        // Tax lot method
            "Available for Sale",  // Accounting treatment
            99.50,         // Cost basis
            0.75           // P&L
        ));
        
        // Option accounting data
        instrumentAccountingData.put("Option", new AccountingData(
            "GL-3001-DV",  // General ledger code
            "Specific ID",  // Tax lot method
            "Trading",     // Accounting treatment
            2.50,          // Cost basis
            -0.25          // P&L
        ));
        
        // ETF accounting data
        instrumentAccountingData.put("ETF", new AccountingData(
            "GL-1002-EQ",  // General ledger code
            "FIFO",        // Tax lot method
            "Available for Sale",  // Accounting treatment
            105.75,        // Cost basis
            3.25           // P&L
        ));
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Check if we have accounting data for this instrument type
        AccountingData accountingData = instrumentAccountingData.get(trade.getValue());
        if (accountingData != null) {
            // Add general ledger code
            enrichedTrade.setValue(trade.getValue() + " (GL: " + accountingData.getGeneralLedgerCode() + ")");
            
            // Add tax lot method
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Tax Lot: " + accountingData.getTaxLotMethod() + ")");
            
            // Add accounting treatment
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Treatment: " + accountingData.getAccountingTreatment() + ")");
            
            // Add cost basis
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Cost: $" + accountingData.getCostBasis() + ")");
            
            // Add P&L
            String plSign = accountingData.getProfitLoss() >= 0 ? "+" : "";
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (P&L: " + plSign + "$" + accountingData.getProfitLoss() + ")");
        } else {
            // Generate random accounting data for demonstration
            String glCode = "GL-" + (1000 + random.nextInt(9000)) + "-XX";
            String[] taxLotMethods = {"FIFO", "LIFO", "Specific ID", "Average Cost"};
            String taxLotMethod = taxLotMethods[random.nextInt(taxLotMethods.length)];
            double costBasis = 50.0 + (random.nextDouble() * 150.0);
            double pl = -5.0 + (random.nextDouble() * 10.0);
            String plSign = pl >= 0 ? "+" : "";
            
            enrichedTrade.setValue(trade.getValue() + 
                    " (GL: " + glCode + 
                    ", Tax Lot: " + taxLotMethod + 
                    ", Cost: $" + String.format("%.2f", costBasis) + 
                    ", P&L: " + plSign + "$" + String.format("%.2f", pl) + ")");
        }
        
        return enrichedTrade;
    }
    
    /**
     * Inner class to represent accounting data.
     */
    private static class AccountingData {
        private final String generalLedgerCode;
        private final String taxLotMethod;
        private final String accountingTreatment;
        private final double costBasis;
        private final double profitLoss;
        
        /**
         * Create new accounting data.
         * 
         * @param generalLedgerCode The general ledger code
         * @param taxLotMethod The tax lot method (FIFO, LIFO, etc.)
         * @param accountingTreatment The accounting treatment
         * @param costBasis The cost basis
         * @param profitLoss The profit/loss
         */
        public AccountingData(String generalLedgerCode, String taxLotMethod, 
                String accountingTreatment, double costBasis, double profitLoss) {
            this.generalLedgerCode = generalLedgerCode;
            this.taxLotMethod = taxLotMethod;
            this.accountingTreatment = accountingTreatment;
            this.costBasis = costBasis;
            this.profitLoss = profitLoss;
        }
        
        public String getGeneralLedgerCode() {
            return generalLedgerCode;
        }
        
        public String getTaxLotMethod() {
            return taxLotMethod;
        }
        
        public String getAccountingTreatment() {
            return accountingTreatment;
        }
        
        public double getCostBasis() {
            return costBasis;
        }
        
        public double getProfitLoss() {
            return profitLoss;
        }
    }
}