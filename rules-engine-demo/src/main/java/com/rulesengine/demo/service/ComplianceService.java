package com.rulesengine.demo.service;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Trade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service for compliance and regulatory reporting in post-trade processing.
 * This service provides methods for compliance checks and regulatory reporting.
 * Uses the RulesEngine to evaluate compliance rules.
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
    private final RulesEngine rulesEngine;
    private final Map<String, Rule> complianceRules = new HashMap<>();

    /**
     * Create a new ComplianceService with default values.
     */
    public ComplianceService() {
        initializeDefaultValues();
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        initializeRules();
    }

    /**
     * Initialize default values for regulatory requirements and deadlines.
     */
    private void initializeDefaultValues() {
        // Initialize regulatory requirements by trade type
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_EQUITY, Arrays.asList(REG_MIFID_II, REG_BASEL_III));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_FIXED_INCOME, Arrays.asList(REG_MIFID_II, REG_BASEL_III, REG_SFTR));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_DERIVATIVE, Arrays.asList(REG_MIFID_II, REG_EMIR, REG_DODD_FRANK));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_FOREX, Arrays.asList(REG_MIFID_II, REG_DODD_FRANK));
        regulatoryRequirements.put(PostTradeProcessingService.TYPE_COMMODITY, Arrays.asList(REG_MIFID_II, REG_EMIR));

        // Initialize reporting deadlines (in hours) by regulatory framework
        reportingDeadlines.put(REG_MIFID_II, 24);
        reportingDeadlines.put(REG_EMIR, 48);
        reportingDeadlines.put(REG_DODD_FRANK, 24);
        reportingDeadlines.put(REG_BASEL_III, 72);
        reportingDeadlines.put(REG_SFTR, 24);
    }

    /**
     * Initialize rules for compliance checks.
     */
    private void initializeRules() {
        // Rule for MiFID II reporting
        complianceRules.put(REG_MIFID_II, new Rule(
            "MiFIDReportingRule",
            "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
            "#regulatoryRequirements.get(#trade.value).contains('" + REG_MIFID_II + "')",
            "Trade requires MiFID II reporting"
        ));

        // Rule for EMIR reporting
        complianceRules.put(REG_EMIR, new Rule(
            "EMIRReportingRule",
            "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
            "#regulatoryRequirements.get(#trade.value).contains('" + REG_EMIR + "')",
            "Trade requires EMIR reporting"
        ));

        // Rule for Dodd-Frank reporting
        complianceRules.put(REG_DODD_FRANK, new Rule(
            "DoddFrankReportingRule",
            "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
            "#regulatoryRequirements.get(#trade.value).contains('" + REG_DODD_FRANK + "')",
            "Trade requires Dodd-Frank reporting"
        ));

        // Rule for Basel III reporting
        complianceRules.put(REG_BASEL_III, new Rule(
            "BaselReportingRule",
            "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
            "#regulatoryRequirements.get(#trade.value).contains('" + REG_BASEL_III + "')",
            "Trade requires Basel III reporting"
        ));

        // Rule for SFTR reporting
        complianceRules.put(REG_SFTR, new Rule(
            "SFTRReportingRule",
            "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
            "#regulatoryRequirements.get(#trade.value).contains('" + REG_SFTR + "')",
            "Trade requires SFTR reporting"
        ));

        // Rule for compliance issues
        complianceRules.put("ComplianceIssues", new Rule(
            "ComplianceIssuesRule",
            "#trade == null || #trade.id == null || #trade.id.isEmpty() || " +
            "#trade.value == null || #trade.value.isEmpty() || " +
            "#trade.category == null || #trade.category.isEmpty() || " +
            "(#riskService != null && (#riskLevel == 'High' || #riskLevel == 'Extreme'))",
            "Trade has compliance issues"
        ));
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
     * Check if a trade requires MiFID II reporting with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult requiresMiFIDReportingWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("regulatoryRequirements", regulatoryRequirements);

        Rule rule = complianceRules.get(REG_MIFID_II);
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Check if a trade requires MiFID II reporting.
     * 
     * @param trade The trade
     * @return True if MiFID II reporting is required, false otherwise
     */
    public boolean requiresMiFIDReporting(Trade trade) {
        RuleResult result = requiresMiFIDReportingWithResult(trade);
        return result.isTriggered();
    }

    /**
     * Check if a trade requires EMIR reporting with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult requiresEMIRReportingWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("regulatoryRequirements", regulatoryRequirements);

        Rule rule = complianceRules.get(REG_EMIR);
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Check if a trade requires EMIR reporting.
     * 
     * @param trade The trade
     * @return True if EMIR reporting is required, false otherwise
     */
    public boolean requiresEMIRReporting(Trade trade) {
        RuleResult result = requiresEMIRReportingWithResult(trade);
        return result.isTriggered();
    }

    /**
     * Check if a trade requires Dodd-Frank reporting with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult requiresDoddFrankReportingWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("regulatoryRequirements", regulatoryRequirements);

        Rule rule = complianceRules.get(REG_DODD_FRANK);
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Check if a trade requires Dodd-Frank reporting.
     * 
     * @param trade The trade
     * @return True if Dodd-Frank reporting is required, false otherwise
     */
    public boolean requiresDoddFrankReporting(Trade trade) {
        RuleResult result = requiresDoddFrankReportingWithResult(trade);
        return result.isTriggered();
    }

    /**
     * Check if a trade requires Basel III reporting with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult requiresBaselReportingWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("regulatoryRequirements", regulatoryRequirements);

        Rule rule = complianceRules.get(REG_BASEL_III);
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Check if a trade requires Basel III reporting.
     * 
     * @param trade The trade
     * @return True if Basel III reporting is required, false otherwise
     */
    public boolean requiresBaselReporting(Trade trade) {
        RuleResult result = requiresBaselReportingWithResult(trade);
        return result.isTriggered();
    }

    /**
     * Check if a trade requires SFTR reporting with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult requiresSFTRReportingWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);
        facts.put("regulatoryRequirements", regulatoryRequirements);

        Rule rule = complianceRules.get(REG_SFTR);
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Check if a trade requires SFTR reporting.
     * 
     * @param trade The trade
     * @return True if SFTR reporting is required, false otherwise
     */
    public boolean requiresSFTRReporting(Trade trade) {
        RuleResult result = requiresSFTRReportingWithResult(trade);
        return result.isTriggered();
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
     * Check if a trade has any compliance issues with detailed result.
     * 
     * @param trade The trade
     * @return RuleResult containing the evaluation outcome
     */
    public RuleResult hasComplianceIssuesWithResult(Trade trade) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("trade", trade);

        // Add risk service and risk level to facts
        if (trade != null) {
            RiskManagementService riskService = new RiskManagementService();
            String riskLevel = riskService.determineRiskLevel(trade);
            facts.put("riskService", riskService);
            facts.put("riskLevel", riskLevel);
        }

        Rule rule = complianceRules.get("ComplianceIssues");
        return rulesEngine.executeRulesList(Collections.singletonList(rule), facts);
    }

    /**
     * Check if a trade has any compliance issues.
     * 
     * @param trade The trade
     * @return True if there are compliance issues, false otherwise
     */
    public boolean hasComplianceIssues(Trade trade) {
        RuleResult result = hasComplianceIssuesWithResult(trade);
        return result.isTriggered();
    }
}
