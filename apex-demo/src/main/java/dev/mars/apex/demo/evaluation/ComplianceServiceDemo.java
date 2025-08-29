package dev.mars.apex.demo.evaluation;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.model.Trade;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Comprehensive demonstration of compliance service functionality with RulesEngine.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of compliance service functionality with RulesEngine.
 * This class shows the step-by-step process of creating and using a compliance service
 * for regulatory reporting and compliance checks. It combines both the demonstration
 * logic and the configuration functionality in a single self-contained class.
 *
 * This is a demo class with a main method for running the demonstration and instance
 * methods for compliance operations.
 */
public class ComplianceServiceDemo {
    private static final Logger LOGGER = Logger.getLogger(ComplianceServiceDemo.class.getName());

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

    // Trade types (from PostTradeProcessingServiceDemoConfig)
    public static final String TYPE_EQUITY = "Equity";
    public static final String TYPE_FIXED_INCOME = "FixedIncome";
    public static final String TYPE_DERIVATIVE = "Derivative";
    public static final String TYPE_FOREX = "Forex";
    public static final String TYPE_COMMODITY = "Commodity";

    // Instance fields for compliance configuration
    private final Map<String, List<String>> regulatoryRequirements = new HashMap<>();
    private final Map<String, Integer> reportingDeadlines = new HashMap<>();
    private final RulesEngine rulesEngine;
    private final Map<String, Rule> complianceRules = new HashMap<>();

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private ComplianceServiceDemo() {
        // Private constructor to prevent instantiation
        this.rulesEngine = null;
    }

    /**
     * Create a new ComplianceServiceDemo with the specified rules engine.
     *
     * @param rulesEngine The rules engine to use for compliance checks
     */
    private ComplianceServiceDemo(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
        initializeDefaultValues();
        initializeRules();
    }

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Run the demonstration
        runComplianceServiceDemo();
    }

    /**
     * Run the compliance service demonstration.
     * This method shows the step-by-step process of creating and using a compliance service.
     */
    private static void runComplianceServiceDemo() {
        LOGGER.info("Starting compliance service demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a ComplianceServiceDemo instance
        LOGGER.info("Step 2: Creating a ComplianceServiceDemo instance");
        ComplianceServiceDemo demo = new ComplianceServiceDemo(rulesEngine);

        // Step 3: Create test trades
        LOGGER.info("Step 3: Creating test trades");
        List<Trade> trades = createTestTrades();

        // Step 4: Process each trade for compliance
        LOGGER.info("Step 4: Processing trades for compliance");
        for (Trade trade : trades) {
            LOGGER.info("Processing trade: " + trade);

            // Step 4.1: Check applicable regulations
            List<String> regulations = demo.getApplicableRegulations(trade);
            LOGGER.info("Applicable regulations: " + regulations);

            // Step 4.2: Check MiFID II reporting requirement
            checkMiFIDReporting(trade, demo);

            // Step 4.3: Check EMIR reporting requirement
            checkEMIRReporting(trade, demo);

            // Step 4.4: Check Dodd-Frank reporting requirement
            checkDoddFrankReporting(trade, demo);

            // Step 4.5: Check Basel III reporting requirement
            checkBaselReporting(trade, demo);

            // Step 4.6: Check SFTR reporting requirement
            checkSFTRReporting(trade, demo);

            // Step 4.7: Check for compliance issues
            checkComplianceIssues(trade, demo);

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("Compliance service demonstration completed");
    }

    /**
     * Check if a trade requires MiFID II reporting.
     *
     * @param trade The trade to check
     * @param demo The compliance service demo instance
     */
    private static void checkMiFIDReporting(Trade trade, ComplianceServiceDemo demo) {
        RuleResult result = demo.requiresMiFIDReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB requires MiFID II reporting");
            LOGGER.info("Report: " + demo.generateMiFIDReport(trade));
        } else {
            LOGGER.info("TradeB does not require MiFID II reporting");
        }
    }

    /**
     * Check if a trade requires EMIR reporting.
     *
     * @param trade The trade to check
     * @param demo The compliance service demo instance
     */
    private static void checkEMIRReporting(Trade trade, ComplianceServiceDemo demo) {
        RuleResult result = demo.requiresEMIRReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB requires EMIR reporting");
            LOGGER.info("Report: " + demo.generateEMIRReport(trade));
        } else {
            LOGGER.info("TradeB does not require EMIR reporting");
        }
    }

    /**
     * Check if a trade requires Dodd-Frank reporting.
     *
     * @param trade The trade to check
     * @param demo The compliance service demo instance
     */
    private static void checkDoddFrankReporting(Trade trade, ComplianceServiceDemo demo) {
        RuleResult result = demo.requiresDoddFrankReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB requires Dodd-Frank reporting");
            LOGGER.info("Report: " + demo.generateDoddFrankReport(trade));
        } else {
            LOGGER.info("TradeB does not require Dodd-Frank reporting");
        }
    }

    /**
     * Check if a trade requires Basel III reporting.
     *
     * @param trade The trade to check
     * @param demo The compliance service demo instance
     */
    private static void checkBaselReporting(Trade trade, ComplianceServiceDemo demo) {
        RuleResult result = demo.requiresBaselReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB requires Basel III reporting");
            LOGGER.info("Report: " + demo.generateBaselReport(trade));
        } else {
            LOGGER.info("TradeB does not require Basel III reporting");
        }
    }

    /**
     * Check if a trade requires SFTR reporting.
     *
     * @param trade The trade to check
     * @param demo The compliance service demo instance
     */
    private static void checkSFTRReporting(Trade trade, ComplianceServiceDemo demo) {
        RuleResult result = demo.requiresSFTRReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB requires SFTR reporting");
            LOGGER.info("Report: " + demo.generateSFTRReport(trade));
        } else {
            LOGGER.info("TradeB does not require SFTR reporting");
        }
    }

    /**
     * Check if a trade has compliance issues.
     *
     * @param trade The trade to check
     * @param demo The compliance service demo instance
     */
    private static void checkComplianceIssues(Trade trade, ComplianceServiceDemo demo) {
        RuleResult result = demo.hasComplianceIssuesWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("TradeB has compliance issues");
        } else {
            LOGGER.info("TradeB has no compliance issues");
        }
    }

    /**
     * Create a list of test trades.
     *
     * @return A list of test trades
     */
    private static List<Trade> createTestTrades() {
        return Arrays.asList(
                new Trade("T1001", TYPE_EQUITY, "Stock"),
                new Trade("T1002", TYPE_FIXED_INCOME, "Bond"),
                new Trade("T1003", TYPE_DERIVATIVE, "Option"),
                new Trade("T1004", TYPE_FOREX, "Spot"),
                new Trade("T1005", TYPE_COMMODITY, "Future"),
                new Trade("", TYPE_EQUITY, "Stock") // Invalid trade
        );
    }

    // ========== Configuration Methods (from ComplianceServiceDemoConfig) ==========

    /**
     * Initialize default values for regulatory requirements and deadlines.
     */
    private void initializeDefaultValues() {
        LOGGER.info("Initializing compliance service configuration with regulatory requirements");

        // Initialize regulatory requirements by trade type
        regulatoryRequirements.put(TYPE_EQUITY, Arrays.asList(REG_MIFID_II, REG_BASEL_III));
        regulatoryRequirements.put(TYPE_FIXED_INCOME, Arrays.asList(REG_MIFID_II, REG_BASEL_III, REG_SFTR));
        regulatoryRequirements.put(TYPE_DERIVATIVE, Arrays.asList(REG_MIFID_II, REG_EMIR, REG_DODD_FRANK));
        regulatoryRequirements.put(TYPE_FOREX, Arrays.asList(REG_MIFID_II, REG_DODD_FRANK));
        regulatoryRequirements.put(TYPE_COMMODITY, Arrays.asList(REG_MIFID_II, REG_EMIR));

        LOGGER.info("Configured regulatory requirements for " + regulatoryRequirements.size() + " trade types");

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
                "TradeB requires MiFID II reporting"
        ));

        // Rule for EMIR reporting
        complianceRules.put(REG_EMIR, new Rule(
                "EMIRReportingRule",
                "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
                        "#regulatoryRequirements.get(#trade.value).contains('" + REG_EMIR + "')",
                "TradeB requires EMIR reporting"
        ));

        // Rule for Dodd-Frank reporting
        complianceRules.put(REG_DODD_FRANK, new Rule(
                "DoddFrankReportingRule",
                "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
                        "#regulatoryRequirements.get(#trade.value).contains('" + REG_DODD_FRANK + "')",
                "TradeB requires Dodd-Frank reporting"
        ));

        // Rule for Basel III reporting
        complianceRules.put(REG_BASEL_III, new Rule(
                "BaselReportingRule",
                "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
                        "#regulatoryRequirements.get(#trade.value).contains('" + REG_BASEL_III + "')",
                "TradeB requires Basel III reporting"
        ));

        // Rule for SFTR reporting
        complianceRules.put(REG_SFTR, new Rule(
                "SFTRReportingRule",
                "#trade != null && #regulatoryRequirements.containsKey(#trade.value) && " +
                        "#regulatoryRequirements.get(#trade.value).contains('" + REG_SFTR + "')",
                "TradeB requires SFTR reporting"
        ));

        // Rule for compliance issues
        complianceRules.put("ComplianceIssues", new Rule(
                "ComplianceIssuesRule",
                "#trade == null || #trade.id == null || #trade.id.isEmpty() || " +
                        "#trade.value == null || #trade.value.isEmpty() || " +
                        "#trade.category == null || #trade.category.isEmpty() || " +
                        "(#riskService != null && (#riskLevel == 'High' || #riskLevel == 'Extreme'))",
                "TradeB has compliance issues"
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

    // ========== Report Generation Methods ==========

    /**
     * Generate MiFID II transaction report for a trade.
     *
     * @param trade The trade
     * @return Report content as a string
     */
    public String generateMiFIDReport(Trade trade) {
        if (trade == null) return "Error: No trade provided";

        RuleResult result = requiresMiFIDReportingWithResult(trade);
        if (!result.isTriggered()) return "Error: MiFID II reporting not required";

        return "MiFID II Transaction Report for TradeB ID: " + trade.getId() +
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

        RuleResult result = requiresEMIRReportingWithResult(trade);
        if (!result.isTriggered()) return "Error: EMIR reporting not required";

        return "EMIR Transaction Report for TradeB ID: " + trade.getId() +
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

        RuleResult result = requiresDoddFrankReportingWithResult(trade);
        if (!result.isTriggered()) return "Error: Dodd-Frank reporting not required";

        return "Dodd-Frank Transaction Report for TradeB ID: " + trade.getId() +
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

        RuleResult result = requiresBaselReportingWithResult(trade);
        if (!result.isTriggered()) return "Error: Basel III reporting not required";

        return "Basel III Transaction Report for TradeB ID: " + trade.getId() +
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

        RuleResult result = requiresSFTRReportingWithResult(trade);
        if (!result.isTriggered()) return "Error: SFTR reporting not required";

        return "SFTR Transaction Report for TradeB ID: " + trade.getId() +
                "\nInstrument: " + trade.getValue() +
                "\nCategory: " + trade.getCategory() +
                "\nReporting Deadline: " + getReportingDeadline(REG_SFTR) + " hours";
    }

    /**
     * Get the rules engine used by this demo.
     *
     * @return The rules engine
     */
    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }
}
