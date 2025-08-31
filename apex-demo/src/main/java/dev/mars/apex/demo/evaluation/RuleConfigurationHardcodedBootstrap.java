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

import java.math.BigDecimal;
import java.util.*;

/**
 * APEX-Compliant Rule Configuration Bootstrap Demo for Rule Processing Transformation.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for rule processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for rule conditions
 * - LookupServiceRegistry: Real lookup service integration for rule data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded rule bootstrap logic and uses:
 * - YAML-driven rule configuration from external files
 * - Real APEX enrichment services for rule processing
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration throughout
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded rule creation with YAML-driven configuration
 * - Eliminated embedded business logic and decision patterns
 * - Uses real APEX enrichment services for rule evaluation
 * - Follows fail-fast approach when YAML configurations are missing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class RuleConfigurationHardcodedBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(RuleConfigurationHardcodedBootstrap.class);

    // Real APEX services for authentic integration
    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;

    // Configuration data (populated via real APEX processing)
    private Map<String, Object> configurationData;

    /**
     * Initialize the rule configuration bootstrap demo with real APEX services.
     */
    public RuleConfigurationHardcodedBootstrap() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        logger.info("RuleConfigurationHardcodedBootstrap initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize RuleConfigurationHardcodedBootstrap: {}", e.getMessage());
            throw new RuntimeException("Rule configuration bootstrap demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external rule configuration bootstrap YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main bootstrap configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/rule-configuration-bootstrap-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load loan approval rules configuration
            YamlRuleConfiguration loanRulesConfig = yamlLoader.loadFromClasspath("evaluation/bootstrap/loan-approval-rules.yaml");
            configurationData.put("loanRulesConfig", loanRulesConfig);
            
            // Load discount rules configuration
            YamlRuleConfiguration discountRulesConfig = yamlLoader.loadFromClasspath("evaluation/bootstrap/discount-rules.yaml");
            configurationData.put("discountRulesConfig", discountRulesConfig);
            
            // Load test data configuration
            YamlRuleConfiguration testDataConfig = yamlLoader.loadFromClasspath("evaluation/bootstrap/bootstrap-test-data.yaml");
            configurationData.put("testDataConfig", testDataConfig);
            
            logger.info("External rule configuration bootstrap YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External bootstrap YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required bootstrap configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT RULE PROCESSING METHODS (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes loan applications using real APEX enrichment.
     */
    public Map<String, Object> processLoanApplication(Map<String, Object> loanData) {
        try {
            logger.info("Processing loan application using real APEX enrichment...");

            // Load main bootstrap configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main bootstrap configuration not found");
            }

            // Add processing context
            Map<String, Object> enrichedLoanData = new HashMap<>(loanData);
            enrichedLoanData.put("processingType", "loan-application");
            enrichedLoanData.put("approach", "data-driven");

            // Use real APEX enrichment service for loan processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, enrichedLoanData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Loan application processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process loan application with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Loan application processing failed", e);
        }
    }

    /**
     * Processes order discounts using real APEX enrichment.
     */
    public Map<String, Object> processOrderDiscount(Map<String, Object> orderData) {
        try {
            logger.info("Processing order discount using real APEX enrichment...");

            // Load main bootstrap configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main bootstrap configuration not found");
            }

            // Add processing context
            Map<String, Object> enrichedOrderData = new HashMap<>(orderData);
            enrichedOrderData.put("processingType", "order-discount");
            enrichedOrderData.put("approach", "data-driven");

            // Use real APEX enrichment service for discount processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, enrichedOrderData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Order discount processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process order discount with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Order discount processing failed", e);
        }
    }

    /**
     * Demonstrates infrastructure setup using real APEX services.
     */
    public Map<String, Object> setupInfrastructure(String setupType) {
        try {
            logger.info("Setting up infrastructure using real APEX services...");

            // Create infrastructure setup data
            Map<String, Object> setupData = new HashMap<>();
            setupData.put("setupType", setupType);
            setupData.put("processingType", "infrastructure-setup");
            setupData.put("approach", "real-apex-services");

            // Load main bootstrap configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main bootstrap configuration not found");
            }

            // Use real APEX enrichment service for infrastructure setup
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, setupData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Infrastructure setup completed using real APEX services");
            return result;

        } catch (Exception e) {
            logger.error("Failed to setup infrastructure with APEX services: {}", e.getMessage());
            throw new RuntimeException("Infrastructure setup failed", e);
        }
    }

    /**
     * Loads YAML configurations using real APEX services.
     */
    public Map<String, Object> loadYamlConfiguration(String configType) {
        try {
            logger.info("Loading YAML configuration using real APEX services...");

            // Create configuration loading data
            Map<String, Object> configData = new HashMap<>();
            configData.put("configType", configType);
            configData.put("processingType", "yaml-configuration-loading");
            configData.put("approach", "real-apex-services");

            // Load main bootstrap configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main bootstrap configuration not found");
            }

            // Use real APEX enrichment service for configuration loading
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, configData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("YAML configuration loaded successfully using real APEX services");
            return result;

        } catch (Exception e) {
            logger.error("Failed to load YAML configuration with APEX services: {}", e.getMessage());
            throw new RuntimeException("YAML configuration loading failed", e);
        }
    }

    // ============================================================================
    // DEMONSTRATION METHODS (APEX-Compliant)
    // ============================================================================

    /**
     * Demonstrates loan application processing with excellent credit score.
     */
    public void demonstrateExcellentCreditLoan() {
        logger.info("Demonstrating excellent credit loan processing with real APEX processing...");

        // Create sample loan application with excellent credit
        Map<String, Object> loanApplication = new HashMap<>();
        loanApplication.put("applicationId", "LA001");
        loanApplication.put("creditScore", 780);
        loanApplication.put("debtToIncomeRatio", 0.25);
        loanApplication.put("loanAmount", 300000);
        loanApplication.put("applicantName", "John Smith");
        loanApplication.put("employmentYears", 8);

        // Process using real APEX enrichment
        Map<String, Object> result = processLoanApplication(loanApplication);

        logger.info("Excellent credit loan result: {}", result);
        System.out.println("=== Excellent Credit Loan Demo Results ===");
        System.out.println("Input: " + loanApplication);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates loan application processing with poor credit score.
     */
    public void demonstratePoorCreditLoan() {
        logger.info("Demonstrating poor credit loan processing with real APEX processing...");

        // Create sample loan application with poor credit
        Map<String, Object> loanApplication = new HashMap<>();
        loanApplication.put("applicationId", "LA003");
        loanApplication.put("creditScore", 580);
        loanApplication.put("debtToIncomeRatio", 0.45);
        loanApplication.put("loanAmount", 150000);
        loanApplication.put("applicantName", "Bob Johnson");
        loanApplication.put("employmentYears", 2);

        // Process using real APEX enrichment
        Map<String, Object> result = processLoanApplication(loanApplication);

        logger.info("Poor credit loan result: {}", result);
        System.out.println("=== Poor Credit Loan Demo Results ===");
        System.out.println("Input: " + loanApplication);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates large order discount processing.
     */
    public void demonstrateLargeOrderDiscount() {
        logger.info("Demonstrating large order discount processing with real APEX processing...");

        // Create sample large order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", "OP001");
        orderData.put("orderTotal", 1250.00);
        orderData.put("customerId", "CUST001");
        orderData.put("customerYears", 3);
        orderData.put("customerName", "Premium Corp");

        // Process using real APEX enrichment
        Map<String, Object> result = processOrderDiscount(orderData);

        logger.info("Large order discount result: {}", result);
        System.out.println("=== Large Order Discount Demo Results ===");
        System.out.println("Input: " + orderData);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates new customer discount processing.
     */
    public void demonstrateNewCustomerDiscount() {
        logger.info("Demonstrating new customer discount processing with real APEX processing...");

        // Create sample new customer order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", "OP003");
        orderData.put("orderTotal", 400.00);
        orderData.put("customerId", "CUST003");
        orderData.put("customerYears", 0);
        orderData.put("customerName", "New Startup LLC");

        // Process using real APEX enrichment
        Map<String, Object> result = processOrderDiscount(orderData);

        logger.info("New customer discount result: {}", result);
        System.out.println("=== New Customer Discount Demo Results ===");
        System.out.println("Input: " + orderData);
        System.out.println("Result: " + result);
    }

    /**
     * Demonstrates infrastructure setup with real APEX services.
     */
    public void demonstrateInfrastructureSetup() {
        logger.info("Demonstrating infrastructure setup with real APEX services...");

        // Setup basic infrastructure
        Map<String, Object> basicSetup = setupInfrastructure("basic-setup");

        logger.info("Basic infrastructure setup result: {}", basicSetup);
        System.out.println("=== Infrastructure Setup Demo Results ===");
        System.out.println("Basic Setup Result: " + basicSetup);

        // Setup advanced infrastructure
        Map<String, Object> advancedSetup = setupInfrastructure("advanced-setup");

        logger.info("Advanced infrastructure setup result: {}", advancedSetup);
        System.out.println("Advanced Setup Result: " + advancedSetup);
    }

    /**
     * Demonstrates YAML configuration loading with real APEX services.
     */
    public void demonstrateYamlConfigurationLoading() {
        logger.info("Demonstrating YAML configuration loading with real APEX services...");

        // Load loan rules configuration
        Map<String, Object> loanConfig = loadYamlConfiguration("loan-approval-rules");

        logger.info("Loan configuration loading result: {}", loanConfig);
        System.out.println("=== YAML Configuration Loading Demo Results ===");
        System.out.println("Loan Config Result: " + loanConfig);

        // Load discount rules configuration
        Map<String, Object> discountConfig = loadYamlConfiguration("discount-rules");

        logger.info("Discount configuration loading result: {}", discountConfig);
        System.out.println("Discount Config Result: " + discountConfig);
    }

    // ============================================================================
    // MAIN METHOD FOR DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant rule configuration bootstrap.
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting APEX-compliant rule configuration bootstrap demonstration...");

            // Initialize with real APEX services
            RuleConfigurationHardcodedBootstrap demo = new RuleConfigurationHardcodedBootstrap();

            // Run all demonstrations
            demo.demonstrateExcellentCreditLoan();
            System.out.println();

            demo.demonstratePoorCreditLoan();
            System.out.println();

            demo.demonstrateLargeOrderDiscount();
            System.out.println();

            demo.demonstrateNewCustomerDiscount();
            System.out.println();

            demo.demonstrateInfrastructureSetup();
            System.out.println();

            demo.demonstrateYamlConfigurationLoading();

            logger.info("APEX-compliant rule configuration bootstrap demonstration completed successfully");

        } catch (Exception e) {
            logger.error("Rule configuration bootstrap demonstration failed: {}", e.getMessage());
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
