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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * APEX-Compliant Integrated Validator Demo.
 *
 * Consolidated demo that replaces IntegratedCustomerValidatorDemo, IntegratedProductValidatorDemo,
 * and IntegratedTradeValidatorDemo with a single configurable validator that supports all entity types.
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for validation processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for validation operations
 * - LookupServiceRegistry: Real lookup service integration for entity data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded validation logic and uses:
 * - YAML-driven comprehensive validation configuration from external files
 * - Real APEX enrichment services for all validation categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Configurable entity types (Customer, Product, Trade)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class IntegratedValidatorDemo {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedValidatorDemo.class);

    private final EnrichmentService enrichmentService;
    private final Map<String, Object> configurationData;

    /**
     * Constructor initializes real APEX services.
     */
    public IntegratedValidatorDemo() {
        logger.info("Starting APEX-compliant integrated validator demonstration...");

        // Initialize real APEX services
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);

        logger.info("IntegratedValidatorDemo initialized with real APEX services");

        // Load external YAML configurations
        this.configurationData = loadExternalConfiguration();
        logger.info("External integrated validator YAML loaded successfully");
    }

    /**
     * Load external YAML configuration files.
     */
    private Map<String, Object> loadExternalConfiguration() {
        try {
            logger.info("Loading external integrated validator YAML...");

            Map<String, Object> configs = new HashMap<>();
            YamlConfigurationLoader loader = new YamlConfigurationLoader();

            // Load main configuration
            YamlRuleConfiguration mainConfig = loader.loadFromClasspath("validation/integrated-validator-demo.yaml");
            configs.put("mainConfig", mainConfig);

            return configs;

        } catch (Exception e) {
            logger.warn("External integrated validator YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required integrated validator configuration YAML files not found", e);
        }
    }

    /**
     * Processes validation rules using real APEX enrichment.
     */
    public Map<String, Object> processValidationRules(String entityType, String ruleType, Map<String, Object> ruleParameters) {
        try {
            logger.info("Processing validation rules '{}' for entity type '{}' using real APEX enrichment...", ruleType, entityType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main integrated validator configuration not found");
            }

            // Create validation rules processing data
            Map<String, Object> validationData = new HashMap<>(ruleParameters);
            validationData.put("entityType", entityType);
            validationData.put("ruleType", ruleType);
            validationData.put("validationType", "validation-rules");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for validation rules processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Validation rules '{}' for entity type '{}' processed successfully using real APEX enrichment", ruleType, entityType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process validation rules '{}' for entity type '{}' with APEX enrichment: {}", ruleType, entityType, e.getMessage());
            throw new RuntimeException("Validation rules processing failed: " + ruleType, e);
        }
    }

    /**
     * Processes entity samples using real APEX enrichment.
     */
    public Map<String, Object> processEntitySamples(String entityType, String sampleType, Map<String, Object> sampleParameters) {
        try {
            logger.info("Processing {} samples '{}' using real APEX enrichment...", entityType.toLowerCase(), sampleType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main integrated validator configuration not found");
            }

            // Create entity samples processing data
            Map<String, Object> validationData = new HashMap<>(sampleParameters);
            validationData.put("entityType", entityType);
            validationData.put("sampleType", sampleType);
            validationData.put("validationType", "entity-samples");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for entity samples processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("{} samples '{}' processed successfully using real APEX enrichment", entityType, sampleType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process {} samples '{}' with APEX enrichment: {}", entityType.toLowerCase(), sampleType, e.getMessage());
            throw new RuntimeException("Entity samples processing failed: " + sampleType, e);
        }
    }

    /**
     * Processes validation parameters using real APEX enrichment.
     */
    public Map<String, Object> processValidationParameters(String entityType, String parameterType, Map<String, Object> parameterParameters) {
        try {
            logger.info("Processing validation parameters '{}' for entity type '{}' using real APEX enrichment...", parameterType, entityType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main integrated validator configuration not found");
            }

            // Create validation parameters processing data
            Map<String, Object> validationData = new HashMap<>(parameterParameters);
            validationData.put("entityType", entityType);
            validationData.put("parameterType", parameterType);
            validationData.put("validationType", "validation-parameters");
            validationData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for validation parameters processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, validationData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Validation parameters '{}' for entity type '{}' processed successfully using real APEX enrichment", parameterType, entityType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process validation parameters '{}' for entity type '{}' with APEX enrichment: {}", parameterType, entityType, e.getMessage());
            throw new RuntimeException("Validation parameters processing failed: " + parameterType, e);
        }
    }

    /**
     * Demonstrates validation rules processing using real APEX enrichment.
     */
    private void demonstrateValidationRulesProcessing(String entityType) {
        System.out.printf("----- CATEGORY 1: VALIDATION RULES PROCESSING (Real APEX Enrichment) - %s -----\n", entityType.toUpperCase());

        String[] ruleTypes = {"basic-validation-rules", "business-validation-rules", "compliance-validation-rules"};

        for (String ruleType : ruleTypes) {
            System.out.printf("Processing %s using real APEX enrichment...\n", ruleType);

            Map<String, Object> ruleParameters = new HashMap<>();
            ruleParameters.put("sampleRule", "rule-processing-data");

            Map<String, Object> result = processValidationRules(entityType, ruleType, ruleParameters);

            System.out.printf("  Validation Rules Result: %s\n", result.get("validationRulesResult"));
            System.out.printf("  Summary: %s\n", result.get("validationSummary"));
        }
    }

    /**
     * Demonstrates entity samples processing using real APEX enrichment.
     */
    private void demonstrateEntitySamplesProcessing(String entityType) {
        System.out.printf("----- CATEGORY 2: %s SAMPLES PROCESSING (Real APEX Enrichment) -----\n", entityType.toUpperCase());

        String[] sampleTypes = {"valid-" + entityType.toLowerCase() + "-sample", "invalid-" + entityType.toLowerCase() + "-sample", "complex-" + entityType.toLowerCase() + "-sample"};

        for (String sampleType : sampleTypes) {
            System.out.printf("Processing %s using real APEX enrichment...\n", sampleType);

            Map<String, Object> sampleParameters = new HashMap<>();
            sampleParameters.put("sampleData", "sample-processing-data");

            Map<String, Object> result = processEntitySamples(entityType, sampleType, sampleParameters);

            System.out.printf("  %s Samples Result: %s\n", entityType, result.get("entitySamplesResult"));
            System.out.printf("  Summary: %s\n", result.get("validationSummary"));
        }
    }

    /**
     * Demonstrates validation parameters processing using real APEX enrichment.
     */
    private void demonstrateValidationParametersProcessing(String entityType) {
        System.out.printf("----- CATEGORY 3: VALIDATION PARAMETERS PROCESSING (Real APEX Enrichment) - %s -----\n", entityType.toUpperCase());

        String[] parameterTypes = {"standard-parameters", "advanced-parameters", "custom-parameters"};

        for (String parameterType : parameterTypes) {
            System.out.printf("Processing %s using real APEX enrichment...\n", parameterType);

            Map<String, Object> parameterParameters = new HashMap<>();
            parameterParameters.put("sampleParameter", "parameter-processing-data");

            Map<String, Object> result = processValidationParameters(entityType, parameterType, parameterParameters);

            System.out.printf("  Validation Parameters Result: %s\n", result.get("validationParametersResult"));
            System.out.printf("  Summary: %s\n", result.get("validationSummary"));
        }
    }

    /**
     * Run the comprehensive integrated validator demonstration for a specific entity type.
     */
    public void runIntegratedValidatorDemo(String entityType) {
        System.out.println("=================================================================");
        System.out.printf("APEX INTEGRATED %s VALIDATOR DEMONSTRATION\n", entityType.toUpperCase());
        System.out.println("=================================================================");
        System.out.printf("Demo Purpose: Comprehensive %s validation with real APEX services\n", entityType.toLowerCase());
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Validation Categories: 3 comprehensive validation categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");
        System.out.println();

        // Category 1: Validation Rules Processing
        demonstrateValidationRulesProcessing(entityType);
        System.out.println();

        // Category 2: Entity Samples Processing
        demonstrateEntitySamplesProcessing(entityType);
        System.out.println();

        // Category 3: Validation Parameters Processing
        demonstrateValidationParametersProcessing(entityType);
        System.out.println();

        System.out.println("=================================================================");
        System.out.printf("%s VALIDATION DEMONSTRATION COMPLETED SUCCESSFULLY\n", entityType.toUpperCase());
        System.out.println("=================================================================");
        System.out.println("All 3 validation categories executed using real APEX services");
        System.out.println("Total processing: 9+ comprehensive validation operations");
        System.out.println("Configuration: YAML files with comprehensive validation definitions");
        System.out.println("Integration: 100% real APEX enrichment services");
        System.out.println("=================================================================");

        logger.info("APEX-compliant {} validator demonstration completed successfully", entityType.toLowerCase());
    }

    /**
     * Main method to run the demo.
     */
    public static void main(String[] args) {
        try {
            IntegratedValidatorDemo demo = new IntegratedValidatorDemo();
            
            // Default to Customer validation if no entity type specified
            String entityType = args.length > 0 ? args[0] : "Customer";
            
            demo.runIntegratedValidatorDemo(entityType);
        } catch (Exception e) {
            logger.error("Demo failed: {}", e.getMessage(), e);
            System.err.println("Demonstration failed: " + e.getMessage());
        }
    }
}
