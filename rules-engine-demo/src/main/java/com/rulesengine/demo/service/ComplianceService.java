package com.rulesengine.demo.service;

import com.rulesengine.demo.model.Trade;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * Service for compliance and regulatory reporting in post-trade processing.
 * This service provides methods for compliance checks and regulatory reporting.
 */
public class ComplianceService {
    
    // Regulatory frameworks
    public static final String REG_MIFID_II = "MiFID II";
    public static final String REG_EMIR = "EMIR";
    public static final String REG_DODD_FRANK = "Dodd-Frank";
    public static final String REG_BASEL_III = "Basel III";
    public static final String REG_SFTR = "SFTR";
    
    // Reporting statuses
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_SUBMITTED = "Submitted";
    public static final String STATUS_ACCEPTED = "Accepted";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_AMENDED = "Amended";
    
    private final Map<String, List<String>> regulatoryRequirements = new HashMap<>();
    private final Map<String, Integer> reportingDeadlines = new HashMap<>();
    
    /**
     * Create a new ComplianceService with default values.
     */
    public ComplianceService() {
        initializeDefaultValues();
    }
    
    /**
     * Initialize default values for regulatory requirements and deadlines.
     */
    private void initializeDefaultValues() {
        // Initialize regulatory requirements by trade type
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_EQUITY, 
                Arrays.asList(REG_MIFID_II, REG_BASEL_III));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_FIXED_INCOME, 
                Arrays.asList(REG_MIFID_II, REG_BASEL_III, REG_SFTR));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_DERIVATIVE, 
                Arrays.asList(REG_MIFID_II, REG_EMIR, REG_DODD_FRANK));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_FOREX, 
                Arrays.asList(REG_MIFID_II, REG_DODD_FRANK));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_COMMODITY, 
                Arrays.asList(REG_MIFID_II, REG_EMIR));
        
        // Initialize reporting deadlines (in hours) by regulatory framework
        reportingDeadlines.put(REG_MIFID_II, 24);
        reportingDeadlines.put(REG_EMIR, 48);
        reportingDeadlines.put(REG_DODD_FRANK, 24);
        reportingDeadlines.put(REG_BASEL_III, 72);
        reportingDeadlines.put(REG_SFTR, 24);
    }
    
    /**
     * Get applicable regulatory frameworks for a trade.
     * 
     * @param trade The trade
     * @return List of applicable regulatory frameworks
     */
    public List<String> getApplicableRegulations(Trade trade) {
        if (trade == null) return Arrays.asList();
        
        String type = trade.getValue();
        return regulatoryRequirements.getOrDefault(type, Arrays.asList());
    }
    
    /**
     * Get reporting deadline for a specific regulatory framework.
     * 
     * @param regulation The regulatory framework
     * @return Reporting deadline in hours
     */
    public int getReportingDeadline(String regulation) {
        return reportingDeadlines.getOrDefault(regulation, 48);
    }
    
    /**
     * Check if a trade requires MiFID II reporting.
     * 
     * @param trade The trade
     * @return True if MiFID II reporting is required, false otherwise
     */
    public boolean requiresMiFIDReporting(Trade trade) {
        if (trade == null) return false;
        
        List<String> regulations = getApplicableRegulations(trade);
        return regulations.contains(REG_MIFID_II);
    }
    
    /**
     * Check if a trade requires EMIR reporting.
     * 
     * @param trade The trade
     * @return True if EMIR reporting is required, false otherwise
     */
    public boolean requiresEMIRReporting(Trade trade) {
        if (trade == null) return false;
        
        List<String> regulations = getApplicableRegulations(trade);
        return regulations.contains(REG_EMIR);
    }
    
    /**
     * Check if a trade requires Dodd-Frank reporting.
     * 
     * @param trade The trade
     * @return True if Dodd-Frank reporting is required, false otherwise
     */
    public boolean requiresDoddFrankReporting(Trade trade) {
        if (trade == null) return false;
        
        List<String> regulations = getApplicableRegulations(trade);
        return regulations.contains(REG_DODD_FRANK);
    }
    
    /**
     * Check if a trade requires Basel III reporting.
     * 
     * @param trade The trade
     * @return True if Basel III reporting is required, false otherwise
     */
    public boolean requiresBaselReporting(Trade trade) {
        if (trade == null) return false;
        
        List<String> regulations = getApplicableRegulations(trade);
        return regulations.contains(REG_BASEL_III);
    }
    
    /**
     * Check if a trade requires SFTR reporting.
     * 
     * @param trade The trade
     * @return True if SFTR reporting is required, false otherwise
     */
    public boolean requiresSFTRReporting(Trade trade) {
        if (trade == null) return false;
        
        List<String> regulations = getApplicableRegulations(trade);
        return regulations.contains(REG_SFTR);
    }
    
    /**
     * Generate MiFID II transaction report for a trade.
     * 
     * @param trade The trade
     * @return Report content as a string
     */
    public String generateMiFIDReport(Trade trade) {
        if (trade == null) return "Error: No trade provided";
        if (!requiresMiFIDReporting(trade)) return "Error: MiFID II reporting not required";
        
        return "MiFID II Transaction Report for Trade ID: " + trade.getId() + 
               "\nInstrument: " + trade.getValue() + 
               "\nCategory: " + trade.getCategory() + 
               "\nReporting Deadline: " + getReportingDeadline(REG_MIFID_II) + " hours";
    }
    
    /**
     * Generate EMIR transaction report for a trade.
     * 
     * @param trade The trade
     * @return Report content as a string
     */
    public String generateEMIRReport(Trade trade) {
        if (trade == null) return "Error: No trade provided";
        if (!requiresEMIRReporting(trade)) return "Error: EMIR reporting not required";
        
        return "EMIR Transaction Report for Trade ID: " + trade.getId() + 
               "\nInstrument: " + trade.getValue() + 
               "\nCategory: " + trade.getCategory() + 
               "\nReporting Deadline: " + getReportingDeadline(REG_EMIR) + " hours";
    }
    
    /**
     * Generate Dodd-Frank transaction report for a trade.
     * 
     * @param trade The trade
     * @return Report content as a string
     */
    public String generateDoddFrankReport(Trade trade) {
        if (trade == null) return "Error: No trade provided";
        if (!requiresDoddFrankReporting(trade)) return "Error: Dodd-Frank reporting not required";
        
        return "Dodd-Frank Transaction Report for Trade ID: " + trade.getId() + 
               "\nInstrument: " + trade.getValue() + 
               "\nCategory: " + trade.getCategory() + 
               "\nReporting Deadline: " + getReportingDeadline(REG_DODD_FRANK) + " hours";
    }
    
    /**
     * Generate Basel III transaction report for a trade.
     * 
     * @param trade The trade
     * @return Report content as a string
     */
    public String generateBaselReport(Trade trade) {
        if (trade == null) return "Error: No trade provided";
        if (!requiresBaselReporting(trade)) return "Error: Basel III reporting not required";
        
        return "Basel III Transaction Report for Trade ID: " + trade.getId() + 
               "\nInstrument: " + trade.getValue() + 
               "\nCategory: " + trade.getCategory() + 
               "\nReporting Deadline: " + getReportingDeadline(REG_BASEL_III) + " hours";
    }
    
    /**
     * Generate SFTR transaction report for a trade.
     * 
     * @param trade The trade
     * @return Report content as a string
     */
    public String generateSFTRReport(Trade trade) {
        if (trade == null) return "Error: No trade provided";
        if (!requiresSFTRReporting(trade)) return "Error: SFTR reporting not required";
        
        return "SFTR Transaction Report for Trade ID: " + trade.getId() + 
               "\nInstrument: " + trade.getValue() + 
               "\nCategory: " + trade.getCategory() + 
               "\nReporting Deadline: " + getReportingDeadline(REG_SFTR) + " hours";
    }
    
    /**
     * Check if a trade has any compliance issues.
     * 
     * @param trade The trade
     * @return True if there are compliance issues, false otherwise
     */
    public boolean hasComplianceIssues(Trade trade) {
        if (trade == null) return true;
        
        // Check for basic compliance issues
        if (trade.getId() == null || trade.getId().isEmpty()) return true;
        if (trade.getValue() == null || trade.getValue().isEmpty()) return true;
        if (trade.getCategory() == null || trade.getCategory().isEmpty()) return true;
        
        // Check for high-risk trades
        RiskManagementService riskService = new RiskManagementService();
        String riskLevel = riskService.determineRiskLevel(trade);
        if (RiskManagementService.RISK_HIGH.equals(riskLevel) || 
            RiskManagementService.RISK_EXTREME.equals(riskLevel)) {
            return true;
        }
        
        return false;
    }
}