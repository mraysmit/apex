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
 * APEX-Compliant Advanced Features Demo.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for advanced features processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for advanced operations
 * - LookupServiceRegistry: Real lookup service integration for dynamic data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded advanced features logic and uses:
 * - YAML-driven comprehensive advanced features configuration from external files
 * - Real APEX enrichment services for all advanced processing categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration for collections, rules, templates, and lookups
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded data creation methods with real APEX service integration
 * - Eliminated embedded business logic and static data patterns
 * - Uses real APEX enrichment services for all advanced feature processing
 * - Follows fail-fast approach when YAML configurations are missing
 * - Comprehensive advanced processing with 6 feature categories
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class ApexAdvancedFeaturesDemo {

    private static final Logger logger = LoggerFactory.getLogger(ApexAdvancedFeaturesDemo.class);

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
     * Initialize the advanced features demo with real APEX services.
     */
    public ApexAdvancedFeaturesDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.processingResults = new HashMap<>();

        logger.info("ApexAdvancedFeaturesDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize ApexAdvancedFeaturesDemo: {}", e.getMessage());
            throw new RuntimeException("Advanced features demo initialization failed", e);
        }
    }

    /**
     * Constructor for creating a demo instance with external services (for testing).
     */
    public ApexAdvancedFeaturesDemo(ExpressionEvaluatorService evaluatorService) {
        // Initialize with provided evaluator service and create other real APEX services
        this.expressionEvaluator = evaluatorService;
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        this.processingResults = new HashMap<>();

        logger.info("ApexAdvancedFeaturesDemo initialized with external evaluator service");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize ApexAdvancedFeaturesDemo: {}", e.getMessage());
            throw new RuntimeException("Advanced features demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external advanced features YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main advanced features configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/apex-advanced-features-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load collection operations configuration
            YamlRuleConfiguration collectionConfig = yamlLoader.loadFromClasspath("evaluation/advanced-features/collection-operations-config.yaml");
            configurationData.put("collectionConfig", collectionConfig);
            
            // Load rule engine configuration
            YamlRuleConfiguration ruleEngineConfig = yamlLoader.loadFromClasspath("evaluation/advanced-features/rule-engine-config.yaml");
            configurationData.put("ruleEngineConfig", ruleEngineConfig);
            
            // Load template processing configuration
            YamlRuleConfiguration templateConfig = yamlLoader.loadFromClasspath("evaluation/advanced-features/template-processing-config.yaml");
            configurationData.put("templateConfig", templateConfig);
            
            // Load dynamic lookup configuration
            YamlRuleConfiguration lookupConfig = yamlLoader.loadFromClasspath("evaluation/advanced-features/dynamic-lookup-config.yaml");
            configurationData.put("lookupConfig", lookupConfig);
            
            // Load rule result features configuration
            YamlRuleConfiguration resultFeaturesConfig = yamlLoader.loadFromClasspath("evaluation/advanced-features/rule-result-features-config.yaml");
            configurationData.put("resultFeaturesConfig", resultFeaturesConfig);
            
            // Load advanced features test data configuration
            YamlRuleConfiguration testDataConfig = yamlLoader.loadFromClasspath("evaluation/advanced-features/advanced-features-test-data.yaml");
            configurationData.put("testDataConfig", testDataConfig);
            
            logger.info("External advanced features YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External advanced features YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required advanced features configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT ADVANCED FEATURES PROCESSING (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes collection operations using real APEX enrichment.
     */
    public Map<String, Object> processCollectionOperations(String operationType, Map<String, Object> operationData) {
        try {
            logger.info("Processing collection operations '{}' using real APEX enrichment...", operationType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main advanced features configuration not found");
            }

            // Create collection operations processing data
            Map<String, Object> collectionData = new HashMap<>(operationData);
            collectionData.put("operationType", operationType);
            collectionData.put("processingType", "collection-operations");
            collectionData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for collection operations processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, collectionData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Collection operations '{}' processed successfully using real APEX enrichment", operationType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process collection operations '{}' with APEX enrichment: {}", operationType, e.getMessage());
            throw new RuntimeException("Collection operations processing failed: " + operationType, e);
        }
    }

    /**
     * Processes rule engine operations using real APEX enrichment.
     */
    public Map<String, Object> processRuleEngine(String ruleType, Map<String, Object> ruleData) {
        try {
            logger.info("Processing rule engine '{}' using real APEX enrichment...", ruleType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main advanced features configuration not found");
            }

            // Create rule engine processing data
            Map<String, Object> engineData = new HashMap<>(ruleData);
            engineData.put("ruleType", ruleType);
            engineData.put("processingType", "rule-engine");
            engineData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for rule engine processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, engineData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Rule engine '{}' processed successfully using real APEX enrichment", ruleType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process rule engine '{}' with APEX enrichment: {}", ruleType, e.getMessage());
            throw new RuntimeException("Rule engine processing failed: " + ruleType, e);
        }
    }

    /**
     * Processes template operations using real APEX enrichment.
     */
    public Map<String, Object> processTemplateOperations(String templateType, Map<String, Object> templateData) {
        try {
            logger.info("Processing template operations '{}' using real APEX enrichment...", templateType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main advanced features configuration not found");
            }

            // Create template processing data
            Map<String, Object> templateProcessingData = new HashMap<>(templateData);
            templateProcessingData.put("templateType", templateType);
            templateProcessingData.put("processingType", "template-processing");
            templateProcessingData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for template processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, templateProcessingData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Template operations '{}' processed successfully using real APEX enrichment", templateType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process template operations '{}' with APEX enrichment: {}", templateType, e.getMessage());
            throw new RuntimeException("Template operations processing failed: " + templateType, e);
        }
    }

    /**
     * Processes dynamic lookup operations using real APEX enrichment.
     */
    public Map<String, Object> processDynamicLookup(String lookupType, Map<String, Object> lookupData) {
        try {
            logger.info("Processing dynamic lookup '{}' using real APEX enrichment...", lookupType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main advanced features configuration not found");
            }

            // Create dynamic lookup processing data
            Map<String, Object> dynamicLookupData = new HashMap<>(lookupData);
            dynamicLookupData.put("lookupType", lookupType);
            dynamicLookupData.put("processingType", "dynamic-lookup");
            dynamicLookupData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for dynamic lookup processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, dynamicLookupData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Dynamic lookup '{}' processed successfully using real APEX enrichment", lookupType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process dynamic lookup '{}' with APEX enrichment: {}", lookupType, e.getMessage());
            throw new RuntimeException("Dynamic lookup processing failed: " + lookupType, e);
        }
    }

    /**
     * Processes rule result features using real APEX enrichment.
     */
    public Map<String, Object> processRuleResultFeatures(String featureType, Map<String, Object> featureData) {
        try {
            logger.info("Processing rule result features '{}' using real APEX enrichment...", featureType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main advanced features configuration not found");
            }

            // Create rule result features processing data
            Map<String, Object> resultFeaturesData = new HashMap<>(featureData);
            resultFeaturesData.put("featureType", featureType);
            resultFeaturesData.put("processingType", "rule-result-features");
            resultFeaturesData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for rule result features processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, resultFeaturesData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Rule result features '{}' processed successfully using real APEX enrichment", featureType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process rule result features '{}' with APEX enrichment: {}", featureType, e.getMessage());
            throw new RuntimeException("Rule result features processing failed: " + featureType, e);
        }
    }

    /**
     * Processes dynamic method execution using real APEX enrichment.
     */
    public Map<String, Object> processDynamicMethodExecution(String methodName, Map<String, Object> methodData) {
        try {
            logger.info("Processing dynamic method execution '{}' using real APEX enrichment...", methodName);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main advanced features configuration not found");
            }

            // Create dynamic method execution processing data
            Map<String, Object> dynamicMethodData = new HashMap<>(methodData);
            dynamicMethodData.put("methodName", methodName);
            dynamicMethodData.put("processingType", "dynamic-method-execution");
            dynamicMethodData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for dynamic method execution processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, dynamicMethodData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Dynamic method execution '{}' processed successfully using real APEX enrichment", methodName);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process dynamic method execution '{}' with APEX enrichment: {}", methodName, e.getMessage());
            throw new RuntimeException("Dynamic method execution processing failed: " + methodName, e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT DEMONSTRATION METHODS
    // ============================================================================

    /**
     * Demonstrates collection operations using real APEX enrichment.
     */
    public void demonstrateCollectionOperations() {
        System.out.println("\n----- CATEGORY 1: COLLECTION OPERATIONS (Real APEX Enrichment) -----");

        String[] operationTypes = {"list-filtering", "array-operations", "complex-queries", "aggregation-operations"};

        for (String operationType : operationTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", operationType);

            Map<String, Object> operationData = new HashMap<>();
            operationData.put("sampleData", "products-and-customers");

            Map<String, Object> result = processCollectionOperations(operationType, operationData);

            System.out.printf("  Collection Result: %s%n", result.get("collectionResult"));
            System.out.printf("  Summary: %s%n", result.get("advancedFeaturesSummary"));
        }
    }

    /**
     * Demonstrates rule engine operations using real APEX enrichment.
     */
    public void demonstrateRuleEngine() {
        System.out.println("\n----- CATEGORY 2: RULE ENGINE (Real APEX Enrichment) -----");

        String[] ruleTypes = {"investment-rules", "conditional-rules", "result-based-rules", "dynamic-selection-rules"};

        for (String ruleType : ruleTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", ruleType);

            Map<String, Object> ruleData = new HashMap<>();
            ruleData.put("sampleRules", "financial-investment-rules");

            Map<String, Object> result = processRuleEngine(ruleType, ruleData);

            System.out.printf("  Rule Engine Result: %s%n", result.get("ruleEngineResult"));
            System.out.printf("  Summary: %s%n", result.get("advancedFeaturesSummary"));
        }
    }

    /**
     * Demonstrates template processing using real APEX enrichment.
     */
    public void demonstrateTemplateProcessing() {
        System.out.println("\n----- CATEGORY 3: TEMPLATE PROCESSING (Real APEX Enrichment) -----");

        String[] templateTypes = {"xml-template", "json-template", "text-template", "html-template"};

        for (String templateType : templateTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", templateType);

            Map<String, Object> templateData = new HashMap<>();
            templateData.put("templateVariables", "customer-and-portfolio-data");

            Map<String, Object> result = processTemplateOperations(templateType, templateData);

            System.out.printf("  Template Result: %s%n", result.get("templateResult"));
            System.out.printf("  Summary: %s%n", result.get("advancedFeaturesSummary"));
        }
    }

    /**
     * Demonstrates dynamic lookup operations using real APEX enrichment.
     */
    public void demonstrateDynamicLookup() {
        System.out.println("\n----- CATEGORY 4: DYNAMIC LOOKUP (Real APEX Enrichment) -----");

        String[] lookupTypes = {"instrument-types-lookup", "risk-levels-lookup", "membership-levels-lookup"};

        for (String lookupType : lookupTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", lookupType);

            Map<String, Object> lookupData = new HashMap<>();
            lookupData.put("lookupKey", "sample-key");

            Map<String, Object> result = processDynamicLookup(lookupType, lookupData);

            System.out.printf("  Lookup Result: %s%n", result.get("lookupResult"));
            System.out.printf("  Summary: %s%n", result.get("advancedFeaturesSummary"));
        }
    }

    /**
     * Demonstrates rule result features using real APEX enrichment.
     */
    public void demonstrateRuleResultFeatures() {
        System.out.println("\n----- CATEGORY 5: RULE RESULT FEATURES (Real APEX Enrichment) -----");

        String[] featureTypes = {"result-analysis", "result-routing", "conditional-followup", "result-aggregation"};

        for (String featureType : featureTypes) {
            System.out.printf("Processing %s using real APEX enrichment...%n", featureType);

            Map<String, Object> featureData = new HashMap<>();
            featureData.put("ruleResults", "sample-rule-execution-results");

            Map<String, Object> result = processRuleResultFeatures(featureType, featureData);

            System.out.printf("  Rule Result Feature: %s%n", result.get("ruleResultFeature"));
            System.out.printf("  Summary: %s%n", result.get("advancedFeaturesSummary"));
        }
    }

    /**
     * Demonstrates dynamic method execution using real APEX enrichment.
     */
    public void demonstrateDynamicMethodExecution() {
        System.out.println("\n----- CATEGORY 6: DYNAMIC METHOD EXECUTION (Real APEX Enrichment) -----");

        String[] methodNames = {"calculatePortfolioValue", "assessInvestmentRisk", "generateCustomerReport", "processTradeOrder"};

        for (String methodName : methodNames) {
            System.out.printf("Processing dynamic method %s using real APEX enrichment...%n", methodName);

            Map<String, Object> methodData = new HashMap<>();
            methodData.put("methodParameters", "sample-parameters");

            Map<String, Object> result = processDynamicMethodExecution(methodName, methodData);

            System.out.printf("  Dynamic Method Result: %s%n", result.get("dynamicMethodResult"));
            System.out.printf("  Summary: %s%n", result.get("advancedFeaturesSummary"));
        }
    }

    /**
     * Run the comprehensive advanced features demonstration.
     */
    public void runAdvancedFeaturesDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX ADVANCED FEATURES DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Comprehensive advanced features with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Feature Categories: 6 comprehensive categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");

        try {
            // Category 1: Collection Operations
            demonstrateCollectionOperations();

            // Category 2: Rule Engine
            demonstrateRuleEngine();

            // Category 3: Template Processing
            demonstrateTemplateProcessing();

            // Category 4: Dynamic Lookup
            demonstrateDynamicLookup();

            // Category 5: Rule Result Features
            demonstrateRuleResultFeatures();

            // Category 6: Dynamic Method Execution
            demonstrateDynamicMethodExecution();

            System.out.println("\n=================================================================");
            System.out.println("ADVANCED FEATURES DEMONSTRATION COMPLETED SUCCESSFULLY");
            System.out.println("=================================================================");
            System.out.println("All 6 categories executed using real APEX services");
            System.out.println("Total processing: 22+ advanced feature operations");
            System.out.println("Configuration: 6 YAML files with comprehensive feature definitions");
            System.out.println("Integration: 100% real APEX enrichment services");
            System.out.println("=================================================================");

        } catch (Exception e) {
            logger.error("Advanced features demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // MAIN METHOD FOR ADVANCED FEATURES DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant advanced features.
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting APEX-compliant advanced features demonstration...");

            // Initialize with real APEX services
            ApexAdvancedFeaturesDemo demo = new ApexAdvancedFeaturesDemo();

            // Run comprehensive demonstration
            demo.runAdvancedFeaturesDemo();

            logger.info("APEX-compliant advanced features demonstration completed successfully");

        } catch (Exception e) {
            logger.error("Advanced features demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
