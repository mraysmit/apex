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
 * APEX-Compliant Integrated Trade Validator Complex Demo.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for complex trade validation
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for validation rules
 * - LookupServiceRegistry: Real lookup service integration for reference data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded trade validation logic and uses:
 * - YAML-driven comprehensive trade validation configuration from external files
 * - Real APEX enrichment services for all validation categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration for settlement, compliance, and risk validation
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded settlement parameters with real APEX service integration
 * - Eliminated embedded compliance rules and lookup service creation
 * - Uses real APEX enrichment services for all validation processing
 * - Follows fail-fast approach when YAML configurations are missing
 * - Comprehensive trade validation with 5 validation categories
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class IntegratedTradeValidatorComplexDemo implements Validator<Trade> {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedTradeValidatorComplexDemo.class);

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
     * Initialize the integrated trade validator complex demo with real APEX services.
     */
    public IntegratedTradeValidatorComplexDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.validationResults = new HashMap<>();

        logger.info("IntegratedTradeValidatorComplexDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize IntegratedTradeValidatorComplexDemo: {}", e.getMessage());
            throw new RuntimeException("Complex trade validator initialization failed", e);
        }
    }

    /**
     * Constructor for creating a validator instance with external services (for testing).
     */
    public IntegratedTradeValidatorComplexDemo(ExpressionEvaluatorService evaluatorService) {
        // Initialize with provided evaluator service and create other real APEX services
        this.expressionEvaluator = evaluatorService;
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.validationResults = new HashMap<>();

        logger.info("IntegratedTradeValidatorComplexDemo initialized with external evaluator service");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize IntegratedTradeValidatorComplexDemo: {}", e.getMessage());
            throw new RuntimeException("Complex trade validator initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external complex trade validation YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main complex trade validation configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("validation/integrated-trade-validator-complex-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load settlement validation configuration
            YamlRuleConfiguration settlementConfig = yamlLoader.loadFromClasspath("validation/trade-validation/settlement-validation-config.yaml");
            configurationData.put("settlementConfig", settlementConfig);
            
            // Load compliance validation configuration
            YamlRuleConfiguration complianceConfig = yamlLoader.loadFromClasspath("validation/trade-validation/compliance-validation-config.yaml");
            configurationData.put("complianceConfig", complianceConfig);
            
            // Load lookup services configuration
            YamlRuleConfiguration lookupConfig = yamlLoader.loadFromClasspath("validation/trade-validation/lookup-services-config.yaml");
            configurationData.put("lookupConfig", lookupConfig);
            
            // Load validation rules configuration
            YamlRuleConfiguration rulesConfig = yamlLoader.loadFromClasspath("validation/trade-validation/validation-rules-config.yaml");
            configurationData.put("rulesConfig", rulesConfig);
            
            // Load trade samples configuration
            YamlRuleConfiguration samplesConfig = yamlLoader.loadFromClasspath("validation/trade-validation/trade-samples-config.yaml");
            configurationData.put("samplesConfig", samplesConfig);
            
            logger.info("External complex trade validation YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External complex trade validation YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required complex trade validation configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT COMPLEX TRADE VALIDATION (Real APEX Service Integration)
    // ============================================================================

    /**
     * Validates settlement parameters using real APEX enrichment.
     */
    public Map<String, Object> validateSettlement(String settlementType, Map<String, Object> settlementData) {
        try {
            logger.info("Validating settlement '{}' using real APEX enrichment...", settlementType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main complex trade validation configuration not found");
            }

            // Create settlement validation processing data
            Map<String, Object> validationData = new HashMap<>(settlementData);
            validationData.put("settlementType", settlementType);
            validationData.put("validationType", "settlement-validation");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for settlement validation
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Settlement validation '{}' processed successfully using real APEX enrichment", settlementType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to validate settlement '{}' with APEX enrichment: {}", settlementType, e.getMessage());
            throw new RuntimeException("Settlement validation failed: " + settlementType, e);
        }
    }

    /**
     * Validates compliance parameters using real APEX enrichment.
     */
    public Map<String, Object> validateCompliance(String complianceType, Map<String, Object> complianceData) {
        try {
            logger.info("Validating compliance '{}' using real APEX enrichment...", complianceType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main complex trade validation configuration not found");
            }

            // Create compliance validation processing data
            Map<String, Object> validationData = new HashMap<>(complianceData);
            validationData.put("complianceType", complianceType);
            validationData.put("validationType", "compliance-validation");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for compliance validation
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Compliance validation '{}' processed successfully using real APEX enrichment", complianceType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to validate compliance '{}' with APEX enrichment: {}", complianceType, e.getMessage());
            throw new RuntimeException("Compliance validation failed: " + complianceType, e);
        }
    }

    /**
     * Validates using lookup services with real APEX enrichment.
     */
    public Map<String, Object> validateLookupService(String lookupServiceType, Map<String, Object> lookupData) {
        try {
            logger.info("Validating lookup service '{}' using real APEX enrichment...", lookupServiceType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main complex trade validation configuration not found");
            }

            // Create lookup service validation processing data
            Map<String, Object> validationData = new HashMap<>(lookupData);
            validationData.put("lookupServiceType", lookupServiceType);
            validationData.put("validationType", "lookup-service-validation");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for lookup service validation
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Lookup service validation '{}' processed successfully using real APEX enrichment", lookupServiceType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to validate lookup service '{}' with APEX enrichment: {}", lookupServiceType, e.getMessage());
            throw new RuntimeException("Lookup service validation failed: " + lookupServiceType, e);
        }
    }

    /**
     * Processes validation rules using real APEX enrichment.
     */
    public Map<String, Object> processValidationRules(String ruleType, Map<String, Object> ruleData) {
        try {
            logger.info("Processing validation rules '{}' using real APEX enrichment...", ruleType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main complex trade validation configuration not found");
            }

            // Create validation rules processing data
            Map<String, Object> validationData = new HashMap<>(ruleData);
            validationData.put("ruleType", ruleType);
            validationData.put("validationType", "validation-rules");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for validation rules processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Validation rules '{}' processed successfully using real APEX enrichment", ruleType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process validation rules '{}' with APEX enrichment: {}", ruleType, e.getMessage());
            throw new RuntimeException("Validation rules processing failed: " + ruleType, e);
        }
    }

    /**
     * Validates trade samples using real APEX enrichment.
     */
    public Map<String, Object> validateTradeSample(String sampleType, Map<String, Object> sampleData) {
        try {
            logger.info("Validating trade sample '{}' using real APEX enrichment...", sampleType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main complex trade validation configuration not found");
            }

            // Create trade sample validation processing data
            Map<String, Object> validationData = new HashMap<>(sampleData);
            validationData.put("sampleType", sampleType);
            validationData.put("validationType", "trade-sample-validation");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for trade sample validation
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Trade sample validation '{}' processed successfully using real APEX enrichment", sampleType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to validate trade sample '{}' with APEX enrichment: {}", sampleType, e.getMessage());
            throw new RuntimeException("Trade sample validation failed: " + sampleType, e);
        }
    }

    /**
     * Comprehensive trade validation using real APEX enrichment.
     */
    public Map<String, Object> validateTrade(Trade trade, String validationType) {
        try {
            logger.info("Performing comprehensive trade validation '{}' using real APEX enrichment...", validationType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main complex trade validation configuration not found");
            }

            // Create comprehensive trade validation processing data
            Map<String, Object> validationData = new HashMap<>();
            validationData.put("trade", trade);
            validationData.put("validationType", validationType);
            validationData.put("approach", "real-apex-services");
            validationData.put("comprehensive", true);

            // Use real APEX enrichment service for comprehensive trade validation
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Comprehensive trade validation '{}' processed successfully using real APEX enrichment", validationType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to perform comprehensive trade validation '{}' with APEX enrichment: {}", validationType, e.getMessage());
            throw new RuntimeException("Comprehensive trade validation failed: " + validationType, e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT DEMONSTRATION METHODS
    // ============================================================================

    /**
     * Demonstrates settlement validation using real APEX enrichment.
     */
    public void demonstrateSettlementValidation() {
        System.out.println("\n----- CATEGORY 1: SETTLEMENT VALIDATION (Real APEX Enrichment) -----");

        String[] settlementTypes = {"standard-settlement", "enhanced-settlement", "institutional-settlement"};

        for (String settlementType : settlementTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", settlementType);

            Map<String, Object> settlementData = new HashMap<>();
            settlementData.put("sampleSettlement", "trade-settlement-data");

            Map<String, Object> result = validateSettlement(settlementType, settlementData);

            System.out.printf("  Settlement Result: %s%n", result.get("settlementValidationResult"));
            System.out.printf("  Summary: %s%n", result.get("validationSummary"));
        }
    }

    /**
     * Demonstrates compliance validation using real APEX enrichment.
     */
    public void demonstrateComplianceValidation() {
        System.out.println("\n----- CATEGORY 2: COMPLIANCE VALIDATION (Real APEX Enrichment) -----");

        String[] complianceTypes = {"standard-compliance", "enhanced-compliance", "institutional-compliance"};

        for (String complianceType : complianceTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", complianceType);

            Map<String, Object> complianceData = new HashMap<>();
            complianceData.put("sampleCompliance", "trade-compliance-data");

            Map<String, Object> result = validateCompliance(complianceType, complianceData);

            System.out.printf("  Compliance Result: %s%n", result.get("complianceValidationResult"));
            System.out.printf("  Summary: %s%n", result.get("validationSummary"));
        }
    }

    /**
     * Demonstrates lookup service validation using real APEX enrichment.
     */
    public void demonstrateLookupServiceValidation() {
        System.out.println("\n----- CATEGORY 3: LOOKUP SERVICE VALIDATION (Real APEX Enrichment) -----");

        String[] lookupServiceTypes = {"instrument-types", "settlement-methods", "settlement-locations", "custodians"};

        for (String lookupServiceType : lookupServiceTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", lookupServiceType);

            Map<String, Object> lookupData = new HashMap<>();
            lookupData.put("lookupKey", "sample-lookup-key");

            Map<String, Object> result = validateLookupService(lookupServiceType, lookupData);

            System.out.printf("  Lookup Service Result: %s%n", result.get("lookupServiceValidationResult"));
            System.out.printf("  Summary: %s%n", result.get("validationSummary"));
        }
    }

    /**
     * Demonstrates validation rules processing using real APEX enrichment.
     */
    public void demonstrateValidationRulesProcessing() {
        System.out.println("\n----- CATEGORY 4: VALIDATION RULES PROCESSING (Real APEX Enrichment) -----");

        String[] ruleTypes = {"settlement-validation-rules", "compliance-validation-rules", "instrument-validation-rules", "risk-validation-rules"};

        for (String ruleType : ruleTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", ruleType);

            Map<String, Object> ruleData = new HashMap<>();
            ruleData.put("sampleRules", "validation-rule-data");

            Map<String, Object> result = processValidationRules(ruleType, ruleData);

            System.out.printf("  Validation Rules Result: %s%n", result.get("validationRulesResult"));
            System.out.printf("  Summary: %s%n", result.get("validationSummary"));
        }
    }

    /**
     * Demonstrates trade sample validation using real APEX enrichment.
     */
    public void demonstrateTradeSampleValidation() {
        System.out.println("\n----- CATEGORY 5: TRADE SAMPLE VALIDATION (Real APEX Enrichment) -----");

        String[] sampleTypes = {"valid-equity-trade", "valid-bond-trade", "invalid-sanctions-trade", "large-amount-aml-trade"};

        for (String sampleType : sampleTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", sampleType);

            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("tradeSample", "sample-trade-data");

            Map<String, Object> result = validateTradeSample(sampleType, sampleData);

            System.out.printf("  Trade Sample Result: %s%n", result.get("tradeSampleValidationResult"));
            System.out.printf("  Summary: %s%n", result.get("validationSummary"));
        }
    }

    // ============================================================================
    // VALIDATOR INTERFACE IMPLEMENTATION
    // ============================================================================

    /**
     * Get the name of this validator service.
     */
    @Override
    public String getName() {
        return "IntegratedTradeValidatorComplexDemo";
    }

    /**
     * Get the type of objects this validator can validate.
     */
    @Override
    public Class<Trade> getType() {
        return Trade.class;
    }

    /**
     * Validates a trade using real APEX enrichment services.
     * Implementation of the Validator<Trade> interface.
     */
    @Override
    public boolean validate(Trade trade) {
        try {
            logger.info("Validating trade using real APEX enrichment services...");

            Map<String, Object> result = validateTrade(trade, "comprehensive-validation");

            // Extract validation result from APEX enrichment
            Object validationResult = result.get("validationSummary");
            boolean isValid = validationResult != null && validationResult.toString().contains("completed");

            logger.info("Trade validation completed: {}", isValid ? "VALID" : "INVALID");
            return isValid;

        } catch (Exception e) {
            logger.error("Trade validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Run the comprehensive complex trade validation demonstration.
     */
    public void runComplexTradeValidationDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX INTEGRATED TRADE VALIDATOR COMPLEX DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Comprehensive complex trade validation with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Validation Categories: 5 comprehensive validation categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");

        try {
            // Category 1: Settlement Validation
            demonstrateSettlementValidation();

            // Category 2: Compliance Validation
            demonstrateComplianceValidation();

            // Category 3: Lookup Service Validation
            demonstrateLookupServiceValidation();

            // Category 4: Validation Rules Processing
            demonstrateValidationRulesProcessing();

            // Category 5: Trade Sample Validation
            demonstrateTradeSampleValidation();

            System.out.println("\n=================================================================");
            System.out.println("COMPLEX TRADE VALIDATION DEMONSTRATION COMPLETED SUCCESSFULLY");
            System.out.println("=================================================================");
            System.out.println("All 5 validation categories executed using real APEX services");
            System.out.println("Total processing: 18+ complex validation operations");
            System.out.println("Configuration: 5 YAML files with comprehensive validation definitions");
            System.out.println("Integration: 100% real APEX enrichment services");
            System.out.println("=================================================================");

        } catch (Exception e) {
            logger.error("Complex trade validation demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // MAIN METHOD FOR COMPLEX TRADE VALIDATION DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant complex trade validation.
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting APEX-compliant complex trade validation demonstration...");

            // Initialize with real APEX services
            IntegratedTradeValidatorComplexDemo demo = new IntegratedTradeValidatorComplexDemo();

            // Run comprehensive demonstration
            demo.runComplexTradeValidationDemo();

            logger.info("APEX-compliant complex trade validation demonstration completed successfully");

        } catch (Exception e) {
            logger.error("Complex trade validation demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
