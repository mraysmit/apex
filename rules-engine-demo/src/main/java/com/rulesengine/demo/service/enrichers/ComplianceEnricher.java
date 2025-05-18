package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.demo.model.Trade;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Enricher for compliance data in financial services.
 * This enricher adds AML/KYC status, restricted security flags, sanctions screening results, beneficial owner information, and other compliance data to trades.
 */
public class ComplianceEnricher extends AbstractEnricher<Trade> {
    private final Map<String, Object> referenceData;
    private final Map<String, ComplianceStatus> counterpartyComplianceStatus = new HashMap<>();
    private final Map<String, SecurityRestriction> securityRestrictions = new HashMap<>();
    private final Random random = new Random();
    
    /**
     * Create a new ComplianceEnricher with the specified name and reference data.
     * 
     * @param name The name of the enricher
     * @param referenceData The reference data to use for enrichment
     */
    public ComplianceEnricher(String name, Map<String, Object> referenceData) {
        super(name, Trade.class);
        this.referenceData = referenceData;
        initializeComplianceData();
    }
    
    /**
     * Initialize compliance data for demonstration purposes.
     */
    private void initializeComplianceData() {
        // Counterparty compliance status
        counterpartyComplianceStatus.put("BANK123", new ComplianceStatus(
            true,   // KYC verified
            "2023-06-15",  // Last KYC date
            "Low",  // AML risk
            "John Smith",  // Beneficial owner
            false   // Sanctions hit
        ));
        
        counterpartyComplianceStatus.put("BROKER456", new ComplianceStatus(
            true,   // KYC verified
            "2023-03-22",  // Last KYC date
            "Medium",  // AML risk
            "Jane Doe",  // Beneficial owner
            false   // Sanctions hit
        ));
        
        counterpartyComplianceStatus.put("FUND789", new ComplianceStatus(
            false,  // KYC not verified
            "2022-11-30",  // Last KYC date
            "High",  // AML risk
            "Global Investments LLC",  // Beneficial owner
            true    // Sanctions hit
        ));
        
        // Security restrictions
        securityRestrictions.put("RESTRICTED_SECURITY", new SecurityRestriction(
            true,   // Is restricted
            "Insider trading blackout",  // Restriction reason
            "2023-07-01",  // Restriction start date
            "2023-07-15"   // Restriction end date
        ));
    }
    
    @Override
    public Trade enrich(Trade trade) {
        if (trade == null) {
            return null;
        }
        
        Trade enrichedTrade = new Trade(trade.getId(), trade.getValue(), trade.getCategory());
        
        // Check for counterparty compliance status
        ComplianceStatus status = counterpartyComplianceStatus.get(trade.getCategory());
        if (status != null) {
            // Add KYC status
            enrichedTrade.setValue(trade.getValue() + " (KYC: " + 
                    (status.isKycVerified() ? "Verified" : "Not Verified") + ", " + 
                    status.getLastKycDate() + ")");
            
            // Add AML risk
            enrichedTrade.setValue(enrichedTrade.getValue() + " (AML Risk: " + status.getAmlRisk() + ")");
            
            // Add beneficial owner information
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Owner: " + status.getBeneficialOwner() + ")");
            
            // Add sanctions screening result
            if (status.isSanctionsHit()) {
                enrichedTrade.setValue(enrichedTrade.getValue() + " (SANCTIONS HIT)");
            }
        }
        
        // Check for security restrictions
        SecurityRestriction restriction = securityRestrictions.get(trade.getValue());
        if (restriction != null && restriction.isRestricted()) {
            enrichedTrade.setValue(enrichedTrade.getValue() + " (RESTRICTED: " + 
                    restriction.getRestrictionReason() + ", " + 
                    restriction.getStartDate() + " to " + 
                    restriction.getEndDate() + ")");
        }
        
        // Add insider trading surveillance (simulated)
        if (random.nextDouble() < 0.1) { // 10% chance of surveillance flag
            String surveillanceId = "SUR-" + UUID.randomUUID().toString().substring(0, 8);
            enrichedTrade.setValue(enrichedTrade.getValue() + " (Surveillance ID: " + surveillanceId + ")");
        }
        
        return enrichedTrade;
    }
    
    /**
     * Inner class to represent counterparty compliance status.
     */
    private static class ComplianceStatus {
        private final boolean kycVerified;
        private final String lastKycDate;
        private final String amlRisk;
        private final String beneficialOwner;
        private final boolean sanctionsHit;
        
        /**
         * Create a new compliance status.
         * 
         * @param kycVerified Whether KYC is verified
         * @param lastKycDate The date of the last KYC check
         * @param amlRisk The AML risk level
         * @param beneficialOwner The beneficial owner
         * @param sanctionsHit Whether there's a sanctions hit
         */
        public ComplianceStatus(boolean kycVerified, String lastKycDate, String amlRisk, 
                String beneficialOwner, boolean sanctionsHit) {
            this.kycVerified = kycVerified;
            this.lastKycDate = lastKycDate;
            this.amlRisk = amlRisk;
            this.beneficialOwner = beneficialOwner;
            this.sanctionsHit = sanctionsHit;
        }
        
        public boolean isKycVerified() {
            return kycVerified;
        }
        
        public String getLastKycDate() {
            return lastKycDate;
        }
        
        public String getAmlRisk() {
            return amlRisk;
        }
        
        public String getBeneficialOwner() {
            return beneficialOwner;
        }
        
        public boolean isSanctionsHit() {
            return sanctionsHit;
        }
    }
    
    /**
     * Inner class to represent security restrictions.
     */
    private static class SecurityRestriction {
        private final boolean restricted;
        private final String restrictionReason;
        private final String startDate;
        private final String endDate;
        
        /**
         * Create a new security restriction.
         * 
         * @param restricted Whether the security is restricted
         * @param restrictionReason The reason for the restriction
         * @param startDate The start date of the restriction
         * @param endDate The end date of the restriction
         */
        public SecurityRestriction(boolean restricted, String restrictionReason, 
                String startDate, String endDate) {
            this.restricted = restricted;
            this.restrictionReason = restrictionReason;
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        public boolean isRestricted() {
            return restricted;
        }
        
        public String getRestrictionReason() {
            return restrictionReason;
        }
        
        public String getStartDate() {
            return startDate;
        }
        
        public String getEndDate() {
            return endDate;
        }
    }
}