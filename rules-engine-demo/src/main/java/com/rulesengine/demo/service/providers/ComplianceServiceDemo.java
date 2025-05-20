package com.rulesengine.demo.service.providers;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.demo.model.Trade;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Demonstration of how to use the ComplianceServiceDemoConfig class with RulesEngine.
 * This class shows the step-by-step process of creating and using a compliance service
 * for regulatory reporting and compliance checks.
 *
 * This is a demo class with no public constructors or methods except for the main method.
 */
public class ComplianceServiceDemo {
    private static final Logger LOGGER = Logger.getLogger(ComplianceServiceDemo.class.getName());

    /**
     * Private constructor to prevent instantiation.
     * This is a demo class that should only be run via the main method.
     */
    private ComplianceServiceDemo() {
        // Private constructor to prevent instantiation
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

        // Step 2: Create a ComplianceServiceDemoConfig
        LOGGER.info("Step 2: Creating a ComplianceServiceDemoConfig");
        ComplianceServiceDemoConfig config = new ComplianceServiceDemoConfig(rulesEngine);

        // Step 3: Create test trades
        LOGGER.info("Step 3: Creating test trades");
        List<Trade> trades = createTestTrades();

        // Step 4: Process each trade for compliance
        LOGGER.info("Step 4: Processing trades for compliance");
        for (Trade trade : trades) {
            LOGGER.info("Processing trade: " + trade);

            // Step 4.1: Check applicable regulations
            List<String> regulations = config.getApplicableRegulations(trade);
            LOGGER.info("Applicable regulations: " + regulations);

            // Step 4.2: Check MiFID II reporting requirement
            checkMiFIDReporting(trade, config);

            // Step 4.3: Check EMIR reporting requirement
            checkEMIRReporting(trade, config);

            // Step 4.4: Check Dodd-Frank reporting requirement
            checkDoddFrankReporting(trade, config);

            // Step 4.5: Check Basel III reporting requirement
            checkBaselReporting(trade, config);

            // Step 4.6: Check SFTR reporting requirement
            checkSFTRReporting(trade, config);

            // Step 4.7: Check for compliance issues
            checkComplianceIssues(trade, config);

            // Add a separator
            LOGGER.info("----------------------------------------");
        }

        LOGGER.info("Compliance service demonstration completed");
    }

    /**
     * Check if a trade requires MiFID II reporting.
     *
     * @param trade The trade to check
     * @param config The compliance service config
     */
    private static void checkMiFIDReporting(Trade trade, ComplianceServiceDemoConfig config) {
        RuleResult result = config.requiresMiFIDReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade requires MiFID II reporting");
            LOGGER.info("Report: " + config.generateMiFIDReport(trade));
        } else {
            LOGGER.info("Trade does not require MiFID II reporting");
        }
    }

    /**
     * Check if a trade requires EMIR reporting.
     *
     * @param trade The trade to check
     * @param config The compliance service config
     */
    private static void checkEMIRReporting(Trade trade, ComplianceServiceDemoConfig config) {
        RuleResult result = config.requiresEMIRReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade requires EMIR reporting");
            LOGGER.info("Report: " + config.generateEMIRReport(trade));
        } else {
            LOGGER.info("Trade does not require EMIR reporting");
        }
    }

    /**
     * Check if a trade requires Dodd-Frank reporting.
     *
     * @param trade The trade to check
     * @param config The compliance service config
     */
    private static void checkDoddFrankReporting(Trade trade, ComplianceServiceDemoConfig config) {
        RuleResult result = config.requiresDoddFrankReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade requires Dodd-Frank reporting");
            LOGGER.info("Report: " + config.generateDoddFrankReport(trade));
        } else {
            LOGGER.info("Trade does not require Dodd-Frank reporting");
        }
    }

    /**
     * Check if a trade requires Basel III reporting.
     *
     * @param trade The trade to check
     * @param config The compliance service config
     */
    private static void checkBaselReporting(Trade trade, ComplianceServiceDemoConfig config) {
        RuleResult result = config.requiresBaselReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade requires Basel III reporting");
            LOGGER.info("Report: " + config.generateBaselReport(trade));
        } else {
            LOGGER.info("Trade does not require Basel III reporting");
        }
    }

    /**
     * Check if a trade requires SFTR reporting.
     *
     * @param trade The trade to check
     * @param config The compliance service config
     */
    private static void checkSFTRReporting(Trade trade, ComplianceServiceDemoConfig config) {
        RuleResult result = config.requiresSFTRReportingWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade requires SFTR reporting");
            LOGGER.info("Report: " + config.generateSFTRReport(trade));
        } else {
            LOGGER.info("Trade does not require SFTR reporting");
        }
    }

    /**
     * Check if a trade has compliance issues.
     *
     * @param trade The trade to check
     * @param config The compliance service config
     */
    private static void checkComplianceIssues(Trade trade, ComplianceServiceDemoConfig config) {
        RuleResult result = config.hasComplianceIssuesWithResult(trade);

        if (result.isTriggered()) {
            LOGGER.info("Trade has compliance issues");
        } else {
            LOGGER.info("Trade has no compliance issues");
        }
    }

    /**
     * Create a list of test trades.
     *
     * @return A list of test trades
     */
    private static List<Trade> createTestTrades() {
        return Arrays.asList(
                new Trade("T1001", PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, "Stock"),
                new Trade("T1002", PostTradeProcessingServiceDemoConfig.TYPE_FIXED_INCOME, "Bond"),
                new Trade("T1003", PostTradeProcessingServiceDemoConfig.TYPE_DERIVATIVE, "Option"),
                new Trade("T1004", PostTradeProcessingServiceDemoConfig.TYPE_FOREX, "Spot"),
                new Trade("T1005", PostTradeProcessingServiceDemoConfig.TYPE_COMMODITY, "Future"),
                new Trade("", PostTradeProcessingServiceDemoConfig.TYPE_EQUITY, "Stock") // Invalid trade
        );
    }
}
