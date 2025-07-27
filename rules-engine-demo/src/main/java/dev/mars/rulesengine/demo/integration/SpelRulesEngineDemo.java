package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleGroup;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.engine.RuleConfigurationService;

import java.util.HashMap;
import java.util.Map;

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
 * This class demonstrates the SpEL Rules Engine functionality,
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * This class demonstrates the SpEL Rules Engine functionality,
 * particularly focusing on rule groups.
 */
public class SpelRulesEngineDemo {

    /**
     * Demonstrates rule groups functionality.
     * This method is called by the SpelRuleGroupsTest.
     */
    public static void demoRuleGroupsDemo() {
        System.out.println("\n=== Demonstrating Rule Groups ===");

        // Create configuration and rules engine
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);

        // Create rule configuration service
        RuleConfigurationService ruleConfigService = new RuleConfigurationService(config);

        // Register rule group demo rules directly
        registerRuleGroupDemoRules(ruleConfigService);

        // Create test scenarios for rule groups
        Map<String, Map<String, Object>> scenarios = createTestScenarios();

        // Execute each scenario
        for (Map.Entry<String, Map<String, Object>> scenario : scenarios.entrySet()) {
            String scenarioName = scenario.getKey();
            Map<String, Object> facts = scenario.getValue();

            System.out.println("\nProcessing scenario for: " + scenarioName);

            // Execute rules for each category
            executeRulesForCategory(engine, "investment-rules", facts);
            executeRulesForCategory(engine, "risk-assessment", facts);
            executeRulesForCategory(engine, "compliance", facts);
        }
    }

    /**
     * Register rule group demonstration rules.
     *
     * @param ruleConfigService The rule configuration service to register rules with
     */
    private static void registerRuleGroupDemoRules(RuleConfigurationService ruleConfigService) {
        try {
            // Create categories for demonstration
            String investmentCategory = "investment-rules";
            String riskCategory = "risk-assessment";
            String complianceCategory = "compliance";

            // Register some basic rules in different categories and store them in variables
            Rule ir001 = ruleConfigService.registerRule(
                    "IR001", // Rule ID
                    investmentCategory, // Rule category
                    "high-value-investment", // Rule name
                    "#investmentAmount > 100000", // Condition
                    "High-value investment detected", // Message
                    "Identifies investments with a value exceeding $100,000", // Description
                    10 // Priority
            );

            Rule ir002 = ruleConfigService.registerRule(
                    "IR002", // Rule ID
                    investmentCategory, // Rule category
                    "retirement-account", // Rule name
                    "#accountType == 'retirement'", // Condition
                    "Retirement account detected", // Message
                    "Identifies retirement accounts", // Description
                    20 // Priority
            );

            // Register a rule in multiple categories (for simplicity, we'll just use one category here)
            Rule ra001 = ruleConfigService.registerRule(
                    "RA001", // Rule ID
                    riskCategory, // Rule category
                    "high-risk-client", // Rule name
                    "#clientRiskScore > 7", // Condition
                    "High-risk client detected", // Message
                    "Identifies clients with high risk scores", // Description
                    5 // Priority
            );

            Rule ra002 = ruleConfigService.registerRule(
                    "RA002", // Rule ID
                    riskCategory, // Rule category
                    "volatile-market", // Rule name
                    "#marketVolatility > 0.2", // Condition
                    "Volatile market conditions detected", // Message
                    "Identifies periods of high market volatility", // Description
                    15 // Priority
            );

            Rule co001 = ruleConfigService.registerRule(
                    "CO001", // Rule ID
                    complianceCategory, // Rule category
                    "kyc-verified", // Rule name
                    "!#kycVerified", // Condition
                    "KYC verification required", // Message
                    "Identifies clients who need KYC verification", // Description
                    25 // Priority
            );

            // Create a rule group with AND operator
            RuleGroup ruleGroup1 = ruleConfigService.registerRuleGroupWithAnd(
                    "RG001", // Group ID
                    investmentCategory, // Group category
                    "retirement-investment-checks", // Group name
                    "Checks for retirement investment criteria", // Description
                    50 // Priority
            );

            // Add rules to the group with sequence numbers
            if (ir001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup1, ir001, 1);
            } else {
                System.err.println("Cannot add rule IR001 to group 'retirement-investment-checks', rule not found");
            }

            if (ir002 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup1, ir002, 2);
            } else {
                System.err.println("Cannot add rule IR002 to group 'retirement-investment-checks', rule not found");
            }

            // Create a rule group with OR operator
            RuleGroup ruleGroup2 = ruleConfigService.registerRuleGroupWithOr(
                    "RG002", // Group ID
                    riskCategory, // Group category
                    "risk-assessment-checks", // Group name
                    "Checks for various risk factors", // Description
                    60 // Priority
            );

            // Add rules to the group with sequence numbers
            if (ra001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup2, ra001, 1);
            } else {
                System.err.println("Cannot add rule RA001 to group 'risk-assessment-checks', rule not found");
            }

            if (ra002 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup2, ra002, 2);
            } else {
                System.err.println("Cannot add rule RA002 to group 'risk-assessment-checks', rule not found");
            }

            // Create a multi-category rule group
            RuleGroup ruleGroup3 = ruleConfigService.registerRuleGroupWithAnd(
                    "RG003", // Group ID
                    complianceCategory, // Group category
                    "compliance-checks", // Group name
                    "Checks for compliance requirements", // Description
                    70 // Priority
            );

            // Add rules to the group with sequence numbers
            if (co001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup3, co001, 1);
            } else {
                System.err.println("Cannot add rule CO001 to group 'compliance-checks', rule not found");
            }

            // Also add the high-risk client rule to the compliance checks
            if (ra001 != null) {
                ruleConfigService.addRuleToGroup(ruleGroup3, ra001, 2);
            } else {
                System.err.println("Cannot add rule RA001 to group 'compliance-checks', rule not found");
            }
        } catch (Exception e) {
            System.err.println("Error registering rule group demo rules: " + e.getMessage());
        }
    }

    /**
     * Execute rules for a specific category and print the results.
     */
    private static void executeRulesForCategory(RulesEngine engine, String category, Map<String, Object> facts) {
        RuleResult result = engine.executeRulesForCategory(category, facts);
        if (result.isTriggered()) {
            System.out.println("Category: " + category + ", rule triggered: " + result.getRuleName());
            System.out.println("Result: " + result.getMessage());
        }
    }

    /**
     * Create test scenarios for rule groups.
     */
    private static Map<String, Map<String, Object>> createTestScenarios() {
        Map<String, Map<String, Object>> scenarios = new HashMap<>();

        // Scenario 1: High-value retirement investment
        Map<String, Object> scenario1 = new HashMap<>();
        scenario1.put("investmentAmount", 150000);
        scenario1.put("accountType", "retirement");
        scenario1.put("clientRiskScore", 5);
        scenario1.put("marketVolatility", 0.15);
        scenario1.put("kycVerified", true);
        scenarios.put("High-value retirement investment", scenario1);

        // Scenario 2: High-risk client with volatile market
        Map<String, Object> scenario2 = new HashMap<>();
        scenario2.put("investmentAmount", 75000);
        scenario2.put("accountType", "standard");
        scenario2.put("clientRiskScore", 8);
        scenario2.put("marketVolatility", 0.25);
        scenario2.put("kycVerified", true);
        scenarios.put("High-risk client with volatile market", scenario2);

        // Scenario 3: KYC verification required
        Map<String, Object> scenario3 = new HashMap<>();
        scenario3.put("investmentAmount", 50000);
        scenario3.put("accountType", "standard");
        scenario3.put("clientRiskScore", 4);
        scenario3.put("marketVolatility", 0.1);
        scenario3.put("kycVerified", false);
        scenarios.put("KYC verification required", scenario3);

        return scenarios;
    }
}