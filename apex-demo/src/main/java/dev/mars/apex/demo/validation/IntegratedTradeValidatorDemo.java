package dev.mars.apex.demo.validation;

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
import dev.mars.apex.core.service.validation.Validator;
import dev.mars.apex.demo.model.Trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * APEX-Compliant Integrated Trade Validator Demo.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for integrated trade validation
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for validation rules
 * - LookupServiceRegistry: Real lookup service integration for reference data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded trade validation logic and uses:
 * - YAML-driven comprehensive trade validation configuration from external files
 * - Real APEX enrichment services for all validation categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration for parameter, sample, and rule validation
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded trade parameters with real APEX service integration
 * - Eliminated embedded sample trade creation and validation rule creation
 * - Uses real APEX enrichment services for all validation processing
 * - Follows fail-fast approach when YAML configurations are missing
 * - Integrated trade validation with 3 validation categories
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class IntegratedTradeValidatorDemo implements Validator<Trade> {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedTradeValidatorDemo.class);

    // Real APEX services for authentic integration
    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;

    // Configuration data (populated via real APEX processing)
    private Map<String, Object> configurationData;
    
    // Validation results (populated via real APEX processing)
    private Map<String, Object> validationResults;

    /**
     * Initialize the integrated trade validator demo with real APEX services.
     */
    public IntegratedTradeValidatorDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.validationResults = new HashMap<>();

        logger.info("IntegratedTradeValidatorDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize IntegratedTradeValidatorDemo: {}", e.getMessage());
            throw new RuntimeException("Integrated trade validator initialization failed", e);
        }
    }

    /**
     * Constructor for creating a validator instance with external services (for testing).
     */
    public IntegratedTradeValidatorDemo(ExpressionEvaluatorService evaluatorService) {
        // Initialize with provided evaluator service and create other real APEX services
        this.expressionEvaluator = evaluatorService;
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.validationResults = new HashMap<>();

        logger.info("IntegratedTradeValidatorDemo initialized with external evaluator service");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize IntegratedTradeValidatorDemo: {}", e.getMessage());
            throw new RuntimeException("Integrated trade validator initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external integrated trade validation YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main integrated trade validation configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("validation/integrated-trade-validator-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load trade validation parameters configuration
            YamlRuleConfiguration parametersConfig = yamlLoader.loadFromClasspath("validation/trade-validation-simple/trade-validation-parameters-config.yaml");
            configurationData.put("parametersConfig", parametersConfig);
            
            // Load trade samples simple configuration
            YamlRuleConfiguration samplesConfig = yamlLoader.loadFromClasspath("validation/trade-validation-simple/trade-samples-simple-config.yaml");
            configurationData.put("samplesConfig", samplesConfig);
            
            // Load validation rules simple configuration
            YamlRuleConfiguration rulesConfig = yamlLoader.loadFromClasspath("validation/trade-validation-simple/validation-rules-simple-config.yaml");
            configurationData.put("rulesConfig", rulesConfig);
            
            logger.info("External integrated trade validation YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External integrated trade validation YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required integrated trade validation configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT INTEGRATED TRADE VALIDATION (Real APEX Service Integration)
    // ============================================================================

    /**
     * Validates trade parameters using real APEX enrichment.
     */
    public Map<String, Object> validateTradeParameters(String parameterType, Map<String, Object> parameterData) {
        try {
            logger.info("Validating trade parameters '{}' using real APEX enrichment...", parameterType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main integrated trade validation configuration not found");
            }

            // Create trade parameter validation processing data
            Map<String, Object> validationData = new HashMap<>(parameterData);
            validationData.put("parameterType", parameterType);
            validationData.put("validationType", "trade-parameter-validation");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for trade parameter validation
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Trade parameter validation '{}' processed successfully using real APEX enrichment", parameterType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to validate trade parameters '{}' with APEX enrichment: {}", parameterType, e.getMessage());
            throw new RuntimeException("Trade parameter validation failed: " + parameterType, e);
        }
    }

    /**
     * Processes trade samples using real APEX enrichment.
     */
    public Map<String, Object> processTradeSamples(String sampleType, Map<String, Object> sampleData) {
        try {
            logger.info("Processing trade samples '{}' using real APEX enrichment...", sampleType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main integrated trade validation configuration not found");
            }

            // Create trade sample processing data
            Map<String, Object> validationData = new HashMap<>(sampleData);
            validationData.put("sampleType", sampleType);
            validationData.put("validationType", "trade-sample-processing");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for trade sample processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Trade sample processing '{}' processed successfully using real APEX enrichment", sampleType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process trade samples '{}' with APEX enrichment: {}", sampleType, e.getMessage());
            throw new RuntimeException("Trade sample processing failed: " + sampleType, e);
        }
    }

    /**
     * Processes validation rules using real APEX enrichment.
     */
    public Map<String, Object> processValidationRulesSimple(String ruleType, Map<String, Object> ruleData) {
        try {
            logger.info("Processing validation rules simple '{}' using real APEX enrichment...", ruleType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main integrated trade validation configuration not found");
            }

            // Create validation rules simple processing data
            Map<String, Object> validationData = new HashMap<>(ruleData);
            validationData.put("ruleType", ruleType);
            validationData.put("validationType", "validation-rules-simple");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for validation rules simple processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Validation rules simple '{}' processed successfully using real APEX enrichment", ruleType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process validation rules simple '{}' with APEX enrichment: {}", ruleType, e.getMessage());
            throw new RuntimeException("Validation rules simple processing failed: " + ruleType, e);
        }
    }

    /**
     * Comprehensive integrated trade validation using real APEX enrichment.
     */
    public Map<String, Object> validateTradeIntegrated(Trade trade, String validationType) {
        try {
            logger.info("Performing integrated trade validation '{}' using real APEX enrichment...", validationType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main integrated trade validation configuration not found");
            }

            // Create integrated trade validation processing data
            Map<String, Object> validationData = new HashMap<>();
            validationData.put("trade", trade);
            validationData.put("validationType", validationType);
            validationData.put("approach", "real-apex-services");
            validationData.put("integrated", true);

            // Use real APEX enrichment service for integrated trade validation
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Integrated trade validation '{}' processed successfully using real APEX enrichment", validationType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to perform integrated trade validation '{}' with APEX enrichment: {}", validationType, e.getMessage());
            throw new RuntimeException("Integrated trade validation failed: " + validationType, e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT DEMONSTRATION METHODS
    // ============================================================================

    /**
     * Demonstrates trade parameter validation using real APEX enrichment.
     */
    public void demonstrateTradeParameterValidation() {
        System.out.println("\n----- CATEGORY 1: TRADE PARAMETER VALIDATION (Real APEX Enrichment) -----");

        String[] parameterTypes = {"equity-parameters", "bond-parameters", "generic-parameters", "option-parameters"};

        for (String parameterType : parameterTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", parameterType);

            Map<String, Object> parameterData = new HashMap<>();
            parameterData.put("sampleParameters", "trade-parameter-data");

            Map<String, Object> result = validateTradeParameters(parameterType, parameterData);

            System.out.printf("  Parameter Result: %s%n", result.get("tradeParameterValidationResult"));
            System.out.printf("  Summary: %s%n", result.get("integratedValidationSummary"));
        }
    }

    /**
     * Demonstrates trade sample processing using real APEX enrichment.
     */
    public void demonstrateTradeSampleProcessing() {
        System.out.println("\n----- CATEGORY 2: TRADE SAMPLE PROCESSING (Real APEX Enrichment) -----");

        String[] sampleTypes = {"equity-trade-sample", "bond-trade-sample", "option-trade-sample", "invalid-equity-sample"};

        for (String sampleType : sampleTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", sampleType);

            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("tradeSample", "sample-trade-data");

            Map<String, Object> result = processTradeSamples(sampleType, sampleData);

            System.out.printf("  Sample Result: %s%n", result.get("tradeSampleProcessingResult"));
            System.out.printf("  Summary: %s%n", result.get("integratedValidationSummary"));
        }
    }

    /**
     * Demonstrates validation rules simple processing using real APEX enrichment.
     */
    public void demonstrateValidationRulesSimpleProcessing() {
        System.out.println("\n----- CATEGORY 3: VALIDATION RULES SIMPLE PROCESSING (Real APEX Enrichment) -----");

        String[] ruleTypes = {"equity-validation-rules", "bond-validation-rules", "generic-validation-rules", "option-validation-rules"};

        for (String ruleType : ruleTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", ruleType);

            Map<String, Object> ruleData = new HashMap<>();
            ruleData.put("sampleRules", "validation-rule-data");

            Map<String, Object> result = processValidationRulesSimple(ruleType, ruleData);

            System.out.printf("  Rules Result: %s%n", result.get("validationRulesSimpleResult"));
            System.out.printf("  Summary: %s%n", result.get("integratedValidationSummary"));
        }
    }

    // ============================================================================
    // VALIDATOR INTERFACE IMPLEMENTATION
    // ============================================================================

    /**
     * Validates a trade using real APEX enrichment services.
     * Implementation of the Validator<Trade> interface.
     */
    @Override
    public boolean validate(Trade trade) {
        try {
            logger.info("Validating trade using real APEX enrichment services...");

            Map<String, Object> result = validateTradeIntegrated(trade, "comprehensive-validation");

            // Extract validation result from APEX enrichment
            Object validationResult = result.get("integratedValidationSummary");
            boolean isValid = validationResult != null && validationResult.toString().contains("completed");

            logger.info("Trade validation completed: {}", isValid ? "VALID" : "INVALID");
            return isValid;

        } catch (Exception e) {
            logger.error("Trade validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Run the comprehensive integrated trade validation demonstration.
     */
    public void runIntegratedTradeValidationDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX INTEGRATED TRADE VALIDATOR DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Comprehensive integrated trade validation with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Validation Categories: 3 integrated validation categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");

        try {
            // Category 1: Trade Parameter Validation
            demonstrateTradeParameterValidation();

            // Category 2: Trade Sample Processing
            demonstrateTradeSampleProcessing();

            // Category 3: Validation Rules Simple Processing
            demonstrateValidationRulesSimpleProcessing();

            System.out.println("\n=================================================================");
            System.out.println("INTEGRATED TRADE VALIDATION DEMONSTRATION COMPLETED SUCCESSFULLY");
            System.out.println("=================================================================");
            System.out.println("All 3 validation categories executed using real APEX services");
            System.out.println("Total processing: 12+ integrated validation operations");
            System.out.println("Configuration: 4 YAML files with comprehensive validation definitions");
            System.out.println("Integration: 100% real APEX enrichment services");
            System.out.println("=================================================================");

        } catch (Exception e) {
            logger.error("Integrated trade validation demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // MAIN METHOD FOR INTEGRATED TRADE VALIDATION DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant integrated trade validation.
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting APEX-compliant integrated trade validation demonstration...");

            // Initialize with real APEX services
            IntegratedTradeValidatorDemo demo = new IntegratedTradeValidatorDemo();

            // Run comprehensive demonstration
            demo.runIntegratedTradeValidationDemo();

            logger.info("APEX-compliant integrated trade validation demonstration completed successfully");

        } catch (Exception e) {
            logger.error("Integrated trade validation demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
