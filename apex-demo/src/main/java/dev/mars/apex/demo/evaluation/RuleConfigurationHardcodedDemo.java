package dev.mars.apex.demo.evaluation;

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


import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * APEX-Compliant Rule Configuration Demo for Rules Engine Demonstrations.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for rule processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for rule conditions
 * - LookupServiceRegistry: Real lookup service integration
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded rule creation and uses:
 * - YAML-driven rule loading from external configuration files
 * - Real APEX enrichment services for rule processing
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration throughout
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded rule creation with YAML-driven configuration
 * - Eliminated embedded business logic parameters
 * - Uses real APEX enrichment services for rule evaluation
 * - Follows fail-fast approach when YAML configurations are missing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class RuleConfigurationHardcodedDemo {

    private static final Logger logger = LoggerFactory.getLogger(RuleConfigurationHardcodedDemo.class);

    // Real APEX services for authentic integration
    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;

    // Configuration data (populated via real APEX processing)
    private Map<String, Object> configurationData;

    /**
     * Initialize the rule configuration demo with real APEX services.
     */
    public RuleConfigurationHardcodedDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        logger.info("RuleConfigurationHardcodedDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize RuleConfigurationHardcodedDemo: {}", e.getMessage());
            throw new RuntimeException("Rule configuration demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external rule configuration YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main rule configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/rule-configuration-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load specific rule configurations
            YamlRuleConfiguration loanApprovalConfig = yamlLoader.loadFromClasspath("evaluation/rules/loan-approval-rules.yaml");
            configurationData.put("loanApprovalConfig", loanApprovalConfig);
            
            YamlRuleConfiguration discountConfig = yamlLoader.loadFromClasspath("evaluation/rules/discount-rules.yaml");
            configurationData.put("discountConfig", discountConfig);
            
            YamlRuleConfiguration combinedConfig = yamlLoader.loadFromClasspath("evaluation/rules/combined-rules.yaml");
            configurationData.put("combinedConfig", combinedConfig);
            
            logger.info("External rule configuration YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External rule YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required rule configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT RULE PROCESSING METHODS (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes loan approval rules using real APEX enrichment.
     */
    public Map<String, Object> processLoanApprovalRules(Map<String, Object> inputData) {
        try {
            logger.info("Processing loan approval rules using real APEX enrichment...");

            // Load loan approval rule configuration
            YamlRuleConfiguration loanApprovalConfig = (YamlRuleConfiguration) configurationData.get("loanApprovalConfig");
            if (loanApprovalConfig == null) {
                throw new RuntimeException("Loan approval rule configuration not found");
            }

            // Use real APEX enrichment service for rule processing
            Object enrichedResult = enrichmentService.enrichObject(loanApprovalConfig, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Loan approval rules processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process loan approval rules with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Loan approval rule processing failed", e);
        }
    }

    /**
     * Processes discount rules using real APEX enrichment.
     */
    public Map<String, Object> processDiscountRules(Map<String, Object> inputData) {
        try {
            logger.info("Processing discount rules using real APEX enrichment...");

            // Load discount rule configuration
            YamlRuleConfiguration discountConfig = (YamlRuleConfiguration) configurationData.get("discountConfig");
            if (discountConfig == null) {
                throw new RuntimeException("Discount rule configuration not found");
            }

            // Use real APEX enrichment service for rule processing
            Object enrichedResult = enrichmentService.enrichObject(discountConfig, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Discount rules processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process discount rules with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Discount rule processing failed", e);
        }
    }

    /**
     * Processes combined rules using real APEX enrichment.
     */
    public Map<String, Object> processCombinedRules(Map<String, Object> inputData) {
        try {
            logger.info("Processing combined rules using real APEX enrichment...");

            // Load combined rule configuration
            YamlRuleConfiguration combinedConfig = (YamlRuleConfiguration) configurationData.get("combinedConfig");
            if (combinedConfig == null) {
                throw new RuntimeException("Combined rule configuration not found");
            }

            // Use real APEX enrichment service for rule processing
            Object enrichedResult = enrichmentService.enrichObject(combinedConfig, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Combined rules processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process combined rules with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Combined rule processing failed", e);
        }
    }

    /**
     * Processes all rule types using real APEX enrichment.
     */
    public Map<String, Object> processAllRules(Map<String, Object> inputData) {
        try {
            logger.info("Processing all rules using real APEX enrichment...");

            // Load main rule configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main rule configuration not found");
            }

            // Use real APEX enrichment service for comprehensive rule processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, inputData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("All rules processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process all rules with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("All rules processing failed", e);
        }
    }

    // ============================================================================
    // DEMONSTRATION METHODS (APEX-Compliant)
    // ============================================================================

    /**
     * Demonstrates loan approval rule processing with sample data.
     */
    public void demonstrateLoanApprovalRules() {
        logger.info("Demonstrating loan approval rules with real APEX processing...");

        // Create sample loan application data
        Map<String, Object> loanApplication = new HashMap<>();
        loanApplication.put("applicantId", "APP001");
        loanApplication.put("creditScore", 720);
        loanApplication.put("debtToIncomeRatio", 0.35);
        loanApplication.put("loanAmount", 250000);
        loanApplication.put("applicationType", "MORTGAGE");

        // Process using real APEX enrichment
        Map<String, Object> result = processLoanApprovalRules(loanApplication);

        logger.info("Loan approval result: {}", result);
        System.out.println("=== Loan Approval Demo Results ===");
        System.out.println("Input: " + loanApplication);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates discount rule processing with sample data.
     */
    public void demonstrateDiscountRules() {
        logger.info("Demonstrating discount rules with real APEX processing...");

        // Create sample order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", "ORD001");
        orderData.put("orderTotal", 1250.00);
        orderData.put("customerYears", 6);
        orderData.put("customerType", "LOYAL");
        orderData.put("orderCategory", "ELECTRONICS");

        // Process using real APEX enrichment
        Map<String, Object> result = processDiscountRules(orderData);

        logger.info("Discount rule result: {}", result);
        System.out.println("=== Discount Rules Demo Results ===");
        System.out.println("Input: " + orderData);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates combined rule processing with sample data.
     */
    public void demonstrateCombinedRules() {
        logger.info("Demonstrating combined rules with real APEX processing...");

        // Create sample comprehensive data
        Map<String, Object> comprehensiveData = new HashMap<>();
        comprehensiveData.put("customerId", "CUST001");
        comprehensiveData.put("creditScore", 680);
        comprehensiveData.put("orderTotal", 800.00);
        comprehensiveData.put("customerYears", 3);
        comprehensiveData.put("riskProfile", "MEDIUM");
        comprehensiveData.put("transactionType", "PURCHASE");

        // Process using real APEX enrichment
        Map<String, Object> result = processCombinedRules(comprehensiveData);

        logger.info("Combined rules result: {}", result);
        System.out.println("=== Combined Rules Demo Results ===");
        System.out.println("Input: " + comprehensiveData);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates comprehensive rule processing with sample data.
     */
    public void demonstrateAllRules() {
        logger.info("Demonstrating all rules with real APEX processing...");

        // Create sample comprehensive data
        Map<String, Object> allRulesData = new HashMap<>();
        allRulesData.put("sessionId", "SESSION001");
        allRulesData.put("creditScore", 750);
        allRulesData.put("debtToIncomeRatio", 0.28);
        allRulesData.put("orderTotal", 1500.00);
        allRulesData.put("customerYears", 8);
        allRulesData.put("loanAmount", 300000);
        allRulesData.put("riskProfile", "LOW");
        allRulesData.put("customerType", "PREMIUM");

        // Process using real APEX enrichment
        Map<String, Object> result = processAllRules(allRulesData);

        logger.info("All rules result: {}", result);
        System.out.println("=== All Rules Demo Results ===");
        System.out.println("Input: " + allRulesData);
        System.out.println("Result: " + result);
    }

    // ============================================================================
    // MAIN METHOD FOR DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant rule processing.
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting APEX-compliant rule configuration demonstration...");

            // Initialize with real APEX services
            RuleConfigurationHardcodedDemo demo = new RuleConfigurationHardcodedDemo();

            // Run all demonstrations
            demo.demonstrateLoanApprovalRules();
            System.out.println();

            demo.demonstrateDiscountRules();
            System.out.println();

            demo.demonstrateCombinedRules();
            System.out.println();

            demo.demonstrateAllRules();

            logger.info("APEX-compliant rule configuration demonstration completed successfully");

        } catch (Exception e) {
            logger.error("Rule configuration demonstration failed: {}", e.getMessage());
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
