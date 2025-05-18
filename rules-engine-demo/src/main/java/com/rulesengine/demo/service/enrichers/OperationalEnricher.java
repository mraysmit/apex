package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Enricher for operational data in financial services.
 * This enricher adds settlement instruction sequences, failure prediction scores, STP eligibility flags, exception handling instructions, and other operational data to trades.
 */
public class OperationalEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Random random = new Random();
    private final Map<String, OperationalMetrics> instrumentMetrics = new HashMap<>();
    
    /**
     * Create a new OperationalEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public OperationalEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
        initializeOperationalMetrics();
    }
    
    /**
     * Initialize operational metrics for demonstration purposes.
     */
    private void initializeOperationalMetrics() {
        // Equity metrics
        instrumentMetrics.put("Equity", new OperationalMetrics(
            1,      // Settlement instruction sequence
            0.05,   // Failure prediction score (5% chance of failure)
            true,   // STP eligible
            "Standard",  // Exception handling procedure
            "Reconciled"  // Reconciliation status
        ));
        
        // Bond metrics
        instrumentMetrics.put("Bond", new OperationalMetrics(
            2,      // Settlement instruction sequence
            0.08,   // Failure prediction score (8% chance of failure)
            true,   // STP eligible
            "Enhanced",  // Exception handling procedure
            "Pending"  // Reconciliation status
        ));
        
        // Option metrics
        instrumentMetrics.put("Option", new OperationalMetrics(
            3,      // Settlement instruction sequence
            0.12,   // Failure prediction score (12% chance of failure)
            false,  // Not STP eligible
            "Manual",  // Exception handling procedure
            "Failed"  // Reconciliation status
        ));
        
        // ETF metrics
        instrumentMetrics.put("ETF", new OperationalMetrics(
            1,      // Settlement instruction sequence
            0.03,   // Failure prediction score (3% chance of failure)
            true,   // STP eligible
            "Standard",  // Exception handling procedure
            "Reconciled"  // Reconciliation status
        ));
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Check if we have operational metrics for this instrument type
        OperationalMetrics metrics = instrumentMetrics.get(trade.getValue());
        if (metrics != null) {
            // Add settlement instruction sequence
            enrichedTrade.setValue(trade.getValue() + " (Sequence: " + metrics.getSettlementSequence() + ")");
            
            // Add failure prediction score
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Failure Risk: " + String.format("%.1f", metrics.getFailurePredictionScore() * 100) + "%)");
            
            // Add STP eligibility
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (STP: " + (metrics.isStpEligible() ? "Eligible" : "Not Eligible") + ")");
            
            // Add exception handling procedure
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Exception: " + metrics.getExceptionHandlingProcedure() + ")");
            
            // Add reconciliation status
            enrichedTrade.setValue(enrichedTrade.getValue() + 
                    " (Reconciliation: " + metrics.getReconciliationStatus() + ")");
        } else {
            // Generate random operational metrics for demonstration
            int sequence = random.nextInt(5) + 1;
            double failureRisk = 0.05 + (random.nextDouble() * 0.15);
            boolean stpEligible = random.nextBoolean();
            
            enrichedTrade.setValue(trade.getValue() + 
                    " (Sequence: " + sequence + 
                    ", Failure Risk: " + String.format("%.1f", failureRisk * 100) + "%" +
                    ", STP: " + (stpEligible ? "Eligible" : "Not Eligible") + ")");
        }
        
        return enrichedTrade;
    }
    
    /**
     * Inner class to represent operational metrics.
     */
    private static class OperationalMetrics {
        private final int settlementSequence;
        private final double failurePredictionScore;
        private final boolean stpEligible;
        private final String exceptionHandlingProcedure;
        private final String reconciliationStatus;
        
        /**
         * Create new operational metrics.
         * 
         * @param settlementSequence The settlement instruction sequence
         * @param failurePredictionScore The failure prediction score
         * @param stpEligible Whether the trade is eligible for straight-through processing
         * @param exceptionHandlingProcedure The exception handling procedure
         * @param reconciliationStatus The reconciliation status
         */
        public OperationalMetrics(int settlementSequence, double failurePredictionScore, 
                boolean stpEligible, String exceptionHandlingProcedure, String reconciliationStatus) {
            this.settlementSequence = settlementSequence;
            this.failurePredictionScore = failurePredictionScore;
            this.stpEligible = stpEligible;
            this.exceptionHandlingProcedure = exceptionHandlingProcedure;
            this.reconciliationStatus = reconciliationStatus;
        }
        
        public int getSettlementSequence() {
            return settlementSequence;
        }
        
        public double getFailurePredictionScore() {
            return failurePredictionScore;
        }
        
        public boolean isStpEligible() {
            return stpEligible;
        }
        
        public String getExceptionHandlingProcedure() {
            return exceptionHandlingProcedure;
        }
        
        public String getReconciliationStatus() {
            return reconciliationStatus;
        }
    }
}