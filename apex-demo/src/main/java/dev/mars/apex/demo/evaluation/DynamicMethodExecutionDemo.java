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
 * APEX-Compliant Dynamic Method Execution Demo.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for dynamic method execution
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for dynamic rules
 * - LookupServiceRegistry: Real lookup service integration for dynamic data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded dynamic method execution logic and uses:
 * - YAML-driven comprehensive dynamic method configuration from external files
 * - Real APEX enrichment services for all dynamic processing categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration for settlement, risk, pricing, and compliance
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded service classes with real APEX service integration
 * - Eliminated embedded business logic and calculation patterns
 * - Uses real APEX enrichment services for all dynamic method execution
 * - Follows fail-fast approach when YAML configurations are missing
 * - Comprehensive dynamic processing with 7 execution categories
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class DynamicMethodExecutionDemo {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMethodExecutionDemo.class);

    // Real APEX services for authentic integration
    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;

    // Configuration data (populated via real APEX processing)
    private Map<String, Object> configurationData;
    
    // Processing results (populated via real APEX processing)
    private Map<String, Object> processingResults;

    /**
     * Initialize the dynamic method execution demo with real APEX services.
     */
    public DynamicMethodExecutionDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.processingResults = new HashMap<>();

        logger.info("DynamicMethodExecutionDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize DynamicMethodExecutionDemo: {}", e.getMessage());
            throw new RuntimeException("Dynamic method execution demo initialization failed", e);
        }
    }

    /**
     * Constructor for creating a demo instance with external services (for testing).
     */
    public DynamicMethodExecutionDemo(ExpressionEvaluatorService evaluatorService) {
        // Initialize with provided evaluator service and create other real APEX services
        this.expressionEvaluator = evaluatorService;
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.processingResults = new HashMap<>();

        logger.info("DynamicMethodExecutionDemo initialized with external evaluator service");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize DynamicMethodExecutionDemo: {}", e.getMessage());
            throw new RuntimeException("Dynamic method execution demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external dynamic method execution YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main dynamic method execution configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/dynamic-method-execution-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load settlement processing configuration
            YamlRuleConfiguration settlementConfig = yamlLoader.loadFromClasspath("evaluation/dynamic-execution/settlement-processing-config.yaml");
            configurationData.put("settlementConfig", settlementConfig);
            
            // Load risk management configuration
            YamlRuleConfiguration riskConfig = yamlLoader.loadFromClasspath("evaluation/dynamic-execution/risk-management-config.yaml");
            configurationData.put("riskConfig", riskConfig);
            
            // Load pricing service configuration
            YamlRuleConfiguration pricingConfig = yamlLoader.loadFromClasspath("evaluation/dynamic-execution/pricing-service-config.yaml");
            configurationData.put("pricingConfig", pricingConfig);
            
            // Load compliance service configuration
            YamlRuleConfiguration complianceConfig = yamlLoader.loadFromClasspath("evaluation/dynamic-execution/compliance-service-config.yaml");
            configurationData.put("complianceConfig", complianceConfig);
            
            // Load dynamic rules configuration
            YamlRuleConfiguration rulesConfig = yamlLoader.loadFromClasspath("evaluation/dynamic-execution/dynamic-rules-config.yaml");
            configurationData.put("rulesConfig", rulesConfig);
            
            // Load dynamic test data configuration
            YamlRuleConfiguration testDataConfig = yamlLoader.loadFromClasspath("evaluation/dynamic-execution/dynamic-test-data.yaml");
            configurationData.put("testDataConfig", testDataConfig);
            
            logger.info("External dynamic method execution YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External dynamic execution YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required dynamic method execution configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT DYNAMIC METHOD PROCESSING (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes settlement operations using real APEX enrichment.
     */
    public Map<String, Object> processSettlementOperation(String tradeType) {
        try {
            logger.info("Processing settlement operation for trade type '{}' using real APEX enrichment...", tradeType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main dynamic method execution configuration not found");
            }

            // Create settlement processing data
            Map<String, Object> settlementData = new HashMap<>();
            settlementData.put("tradeType", tradeType);
            settlementData.put("processingType", "settlement-processing");
            settlementData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for settlement processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, settlementData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Settlement operation for trade type '{}' processed successfully using real APEX enrichment", tradeType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process settlement operation for trade type '{}' with APEX enrichment: {}", tradeType, e.getMessage());
            throw new RuntimeException("Settlement operation processing failed: " + tradeType, e);
        }
    }

    /**
     * Processes risk management operations using real APEX enrichment.
     */
    public Map<String, Object> processRiskManagement(String tradeType) {
        try {
            logger.info("Processing risk management for trade type '{}' using real APEX enrichment...", tradeType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main dynamic method execution configuration not found");
            }

            // Create risk management processing data
            Map<String, Object> riskData = new HashMap<>();
            riskData.put("tradeType", tradeType);
            riskData.put("processingType", "risk-management");
            riskData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for risk management processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, riskData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Risk management for trade type '{}' processed successfully using real APEX enrichment", tradeType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process risk management for trade type '{}' with APEX enrichment: {}", tradeType, e.getMessage());
            throw new RuntimeException("Risk management processing failed: " + tradeType, e);
        }
    }

    /**
     * Processes pricing operations using real APEX enrichment.
     */
    public Map<String, Object> processPricingOperation(double basePrice, String pricingType) {
        try {
            logger.info("Processing pricing operation for base price {} with type '{}' using real APEX enrichment...", basePrice, pricingType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main dynamic method execution configuration not found");
            }

            // Create pricing processing data
            Map<String, Object> pricingData = new HashMap<>();
            pricingData.put("basePrice", basePrice);
            pricingData.put("pricingType", pricingType);
            pricingData.put("processingType", "pricing-service");
            pricingData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for pricing processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, pricingData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Pricing operation for base price {} with type '{}' processed successfully using real APEX enrichment", basePrice, pricingType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process pricing operation for base price {} with type '{}' with APEX enrichment: {}", basePrice, pricingType, e.getMessage());
            throw new RuntimeException("Pricing operation processing failed: " + pricingType, e);
        }
    }

    /**
     * Processes compliance operations using real APEX enrichment.
     */
    public Map<String, Object> processComplianceOperation(String tradeType) {
        try {
            logger.info("Processing compliance operation for trade type '{}' using real APEX enrichment...", tradeType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main dynamic method execution configuration not found");
            }

            // Create compliance processing data
            Map<String, Object> complianceData = new HashMap<>();
            complianceData.put("tradeType", tradeType);
            complianceData.put("processingType", "compliance-service");
            complianceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for compliance processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, complianceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Compliance operation for trade type '{}' processed successfully using real APEX enrichment", tradeType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process compliance operation for trade type '{}' with APEX enrichment: {}", tradeType, e.getMessage());
            throw new RuntimeException("Compliance operation processing failed: " + tradeType, e);
        }
    }

    /**
     * Processes dynamic rule execution using real APEX enrichment.
     */
    public Map<String, Object> processDynamicRule(String ruleName, Map<String, Object> inputData) {
        try {
            logger.info("Processing dynamic rule '{}' using real APEX enrichment...", ruleName);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main dynamic method execution configuration not found");
            }

            // Create dynamic rule processing data
            Map<String, Object> ruleData = new HashMap<>(inputData);
            ruleData.put("ruleName", ruleName);
            ruleData.put("processingType", "dynamic-rules");
            ruleData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for dynamic rule processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, ruleData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Dynamic rule '{}' processed successfully using real APEX enrichment", ruleName);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process dynamic rule '{}' with APEX enrichment: {}", ruleName, e.getMessage());
            throw new RuntimeException("Dynamic rule processing failed: " + ruleName, e);
        }
    }

    /**
     * Processes conditional operations using real APEX enrichment.
     */
    public Map<String, Object> processConditionalOperation(boolean condition, String trueValue, String falseValue) {
        try {
            logger.info("Processing conditional operation using real APEX enrichment...");

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main dynamic method execution configuration not found");
            }

            // Create conditional processing data
            Map<String, Object> conditionalData = new HashMap<>();
            conditionalData.put("condition", condition);
            conditionalData.put("trueValue", trueValue);
            conditionalData.put("falseValue", falseValue);
            conditionalData.put("processingType", "conditional-processing");
            conditionalData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for conditional processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, conditionalData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Conditional operation processed successfully using real APEX enrichment");
            return result;

        } catch (Exception e) {
            logger.error("Failed to process conditional operation with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Conditional operation processing failed", e);
        }
    }

    /**
     * Processes fee calculation using real APEX enrichment.
     */
    public Map<String, Object> processFeeCalculation(double notionalValue, Double feeRate) {
        try {
            logger.info("Processing fee calculation for notional value {} using real APEX enrichment...", notionalValue);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main dynamic method execution configuration not found");
            }

            // Create fee calculation processing data
            Map<String, Object> feeData = new HashMap<>();
            feeData.put("notionalValue", notionalValue);
            if (feeRate != null) {
                feeData.put("feeRate", feeRate);
            }
            feeData.put("processingType", "fee-calculation");
            feeData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for fee calculation processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, feeData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Fee calculation for notional value {} processed successfully using real APEX enrichment", notionalValue);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process fee calculation for notional value {} with APEX enrichment: {}", notionalValue, e.getMessage());
            throw new RuntimeException("Fee calculation processing failed", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT DEMONSTRATION METHODS
    // ============================================================================

    /**
     * Demonstrates settlement processing using real APEX enrichment.
     */
    public void demonstrateSettlementProcessing() {
        System.out.println("\n----- CATEGORY 1: SETTLEMENT PROCESSING (Real APEX Enrichment) -----");

        String[] tradeTypes = {"Equity", "FixedIncome", "Derivative", "Forex", "Commodity"};

        for (String tradeType : tradeTypes) {
            System.out.printf("Processing settlement for %s trade using real APEX enrichment...%n", tradeType);

            Map<String, Object> result = processSettlementOperation(tradeType);

            System.out.printf("  Settlement Days: %s%n", result.get("settlementDays"));
            System.out.printf("  Settlement Method: %s%n", result.get("settlementMethod"));
            System.out.printf("  Execution Summary: %s%n", result.get("executionSummary"));
        }
    }

    /**
     * Demonstrates risk management using real APEX enrichment.
     */
    public void demonstrateRiskManagement() {
        System.out.println("\n----- CATEGORY 2: RISK MANAGEMENT (Real APEX Enrichment) -----");

        String[] tradeTypes = {"Equity", "FixedIncome", "Derivative", "Forex", "Commodity"};

        for (String tradeType : tradeTypes) {
            System.out.printf("Processing risk management for %s trade using real APEX enrichment...%n", tradeType);

            Map<String, Object> result = processRiskManagement(tradeType);

            System.out.printf("  Market Risk: %s%n", result.get("marketRisk"));
            System.out.printf("  Credit Risk: %s%n", result.get("creditRisk"));
            System.out.printf("  Execution Summary: %s%n", result.get("executionSummary"));
        }
    }

    /**
     * Demonstrates pricing service using real APEX enrichment.
     */
    public void demonstratePricingService() {
        System.out.println("\n----- CATEGORY 3: PRICING SERVICE (Real APEX Enrichment) -----");

        String[] pricingTypes = {"standard", "premium", "sale", "clearance"};
        double basePrice = 100.00;

        for (String pricingType : pricingTypes) {
            System.out.printf("Processing %s pricing for base price $%.2f using real APEX enrichment...%n", pricingType, basePrice);

            Map<String, Object> result = processPricingOperation(basePrice, pricingType);

            System.out.printf("  Calculated Price: %s%n", result.get("calculatedPrice"));
            System.out.printf("  Execution Summary: %s%n", result.get("executionSummary"));
        }
    }

    /**
     * Demonstrates compliance service using real APEX enrichment.
     */
    public void demonstrateComplianceService() {
        System.out.println("\n----- CATEGORY 4: COMPLIANCE SERVICE (Real APEX Enrichment) -----");

        String[] tradeTypes = {"Equity", "FixedIncome", "Derivative", "Forex", "Commodity"};

        for (String tradeType : tradeTypes) {
            System.out.printf("Processing compliance for %s trade using real APEX enrichment...%n", tradeType);

            Map<String, Object> result = processComplianceOperation(tradeType);

            System.out.printf("  Applicable Regulations: %s%n", result.get("applicableRegulations"));
            System.out.printf("  Execution Summary: %s%n", result.get("executionSummary"));
        }
    }

    /**
     * Demonstrates dynamic rule execution using real APEX enrichment.
     */
    public void demonstrateDynamicRules() {
        System.out.println("\n----- CATEGORY 5: DYNAMIC RULES (Real APEX Enrichment) -----");

        // Settlement days rule
        Map<String, Object> settlementInput = new HashMap<>();
        settlementInput.put("tradeType", "Equity");
        Map<String, Object> settlementResult = processDynamicRule("SettlementDays", settlementInput);
        System.out.printf("Settlement Days Rule Result: %s%n", settlementResult.get("ruleResult"));

        // Market risk rule
        Map<String, Object> riskInput = new HashMap<>();
        riskInput.put("tradeType", "Derivative");
        Map<String, Object> riskResult = processDynamicRule("MarketRisk", riskInput);
        System.out.printf("Market Risk Rule Result: %s%n", riskResult.get("ruleResult"));

        // Premium pricing rule
        Map<String, Object> pricingInput = new HashMap<>();
        pricingInput.put("basePrice", 100.00);
        Map<String, Object> pricingResult = processDynamicRule("PremiumPrice", pricingInput);
        System.out.printf("Premium Price Rule Result: %s%n", pricingResult.get("ruleResult"));
    }

    /**
     * Demonstrates conditional processing using real APEX enrichment.
     */
    public void demonstrateConditionalProcessing() {
        System.out.println("\n----- CATEGORY 6: CONDITIONAL PROCESSING (Real APEX Enrichment) -----");

        // True condition
        Map<String, Object> trueResult = processConditionalOperation(true, "Condition is true", "Condition is false");
        System.out.printf("True Condition Result: %s%n", trueResult.get("conditionalResult"));

        // False condition
        Map<String, Object> falseResult = processConditionalOperation(false, "Condition is true", "Condition is false");
        System.out.printf("False Condition Result: %s%n", falseResult.get("conditionalResult"));
    }

    /**
     * Demonstrates fee calculation using real APEX enrichment.
     */
    public void demonstrateFeeCalculation() {
        System.out.println("\n----- CATEGORY 7: FEE CALCULATION (Real APEX Enrichment) -----");

        // Standard fee calculation
        Map<String, Object> standardFeeResult = processFeeCalculation(1000000.00, null);
        System.out.printf("Standard Fee (1M notional): %s%n", standardFeeResult.get("calculatedFee"));

        // Premium fee calculation
        Map<String, Object> premiumFeeResult = processFeeCalculation(5000000.00, 0.0015);
        System.out.printf("Premium Fee (5M notional, 0.15%% rate): %s%n", premiumFeeResult.get("calculatedFee"));
    }

    /**
     * Run the comprehensive dynamic method execution demonstration.
     */
    public void runDynamicMethodExecutionDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX DYNAMIC METHOD EXECUTION DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Comprehensive dynamic method execution with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Execution Categories: 7 comprehensive categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");

        try {
            // Category 1: Settlement Processing
            demonstrateSettlementProcessing();

            // Category 2: Risk Management
            demonstrateRiskManagement();

            // Category 3: Pricing Service
            demonstratePricingService();

            // Category 4: Compliance Service
            demonstrateComplianceService();

            // Category 5: Dynamic Rules
            demonstrateDynamicRules();

            // Category 6: Conditional Processing
            demonstrateConditionalProcessing();

            // Category 7: Fee Calculation
            demonstrateFeeCalculation();

            System.out.println("\n=================================================================");
            System.out.println("DYNAMIC METHOD EXECUTION DEMONSTRATION COMPLETED SUCCESSFULLY");
            System.out.println("=================================================================");
            System.out.println("All 7 categories executed using real APEX services");
            System.out.println("Total processing: 25+ dynamic method executions");
            System.out.println("Configuration: 6 YAML files with comprehensive method definitions");
            System.out.println("Integration: 100% real APEX enrichment services");
            System.out.println("=================================================================");

        } catch (Exception e) {
            logger.error("Dynamic method execution demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // MAIN METHOD FOR DYNAMIC METHOD EXECUTION DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant dynamic method execution.
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting APEX-compliant dynamic method execution demonstration...");

            // Initialize with real APEX services
            DynamicMethodExecutionDemo demo = new DynamicMethodExecutionDemo();

            // Run comprehensive demonstration
            demo.runDynamicMethodExecutionDemo();

            logger.info("APEX-compliant dynamic method execution demonstration completed successfully");

        } catch (Exception e) {
            logger.error("Dynamic method execution demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
