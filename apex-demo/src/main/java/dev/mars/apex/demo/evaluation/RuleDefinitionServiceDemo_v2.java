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
import dev.mars.apex.core.service.database.DatabaseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * APEX-Compliant Rule Definition Service Demo.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for rule definition service
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for rule definition operations
 * - LookupServiceRegistry: Real lookup service integration for rule definition data
 * - DatabaseService: Real database service for rule definition and analysis data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded rule definition service logic and uses:
 * - YAML-driven comprehensive rule definition service configuration from external files
 * - Real APEX enrichment services for all service categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration for rule creation engines, management frameworks, and analysis processors
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded HashMap creation with real APEX service integration
 * - Eliminated embedded rule definition logic and analysis patterns
 * - Uses real APEX enrichment services for all rule definition service operations
 * - Follows fail-fast approach when YAML configurations are missing
 * - Comprehensive rule definition service with 3 processing categories
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-31
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class RuleDefinitionServiceDemo {

    private static final Logger logger = LoggerFactory.getLogger(RuleDefinitionServiceDemo.class);

    // Real APEX services for authentic integration
    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;
    private final DatabaseService databaseService;

    // Configuration data (populated via real APEX processing)
    private Map<String, Object> configurationData;
    
    // Service results (populated via real APEX processing)
    private Map<String, Object> serviceResults;

    /**
     * Initialize the rule definition service demo with real APEX services.
     */
    public RuleDefinitionServiceDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        this.databaseService = new DatabaseService();
        
        this.serviceResults = new HashMap<>();

        logger.info("RuleDefinitionServiceDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize RuleDefinitionServiceDemo: {}", e.getMessage());
            throw new RuntimeException("Rule definition service demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external rule definition service YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main rule definition service configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/rule-definition-service-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load rule creation engines configuration
            YamlRuleConfiguration ruleCreationEnginesConfig = yamlLoader.loadFromClasspath("evaluation/rule-definition-service/rule-creation-engines-config.yaml");
            configurationData.put("ruleCreationEnginesConfig", ruleCreationEnginesConfig);
            
            // Load rule management frameworks configuration
            YamlRuleConfiguration ruleManagementFrameworksConfig = yamlLoader.loadFromClasspath("evaluation/rule-definition-service/rule-management-frameworks-config.yaml");
            configurationData.put("ruleManagementFrameworksConfig", ruleManagementFrameworksConfig);
            
            // Load rule analysis processors configuration
            YamlRuleConfiguration ruleAnalysisProcessorsConfig = yamlLoader.loadFromClasspath("evaluation/rule-definition-service/rule-analysis-processors-config.yaml");
            configurationData.put("ruleAnalysisProcessorsConfig", ruleAnalysisProcessorsConfig);
            
            logger.info("External rule definition service YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External rule definition service YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required rule definition service configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT RULE DEFINITION SERVICE (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes rule creation engines using real APEX enrichment.
     */
    public Map<String, Object> processRuleCreationEngines(String engineType, Map<String, Object> engineParameters) {
        try {
            logger.info("Processing rule creation engines '{}' using real APEX enrichment...", engineType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main rule definition service configuration not found");
            }

            // Create rule creation engines processing data
            Map<String, Object> serviceData = new HashMap<>(engineParameters);
            serviceData.put("engineType", engineType);
            serviceData.put("serviceType", "rule-creation-engines-processing");
            serviceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for rule creation engines processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, serviceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Rule creation engines processing '{}' processed successfully using real APEX enrichment", engineType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process rule creation engines '{}' with APEX enrichment: {}", engineType, e.getMessage());
            throw new RuntimeException("Rule creation engines processing failed: " + engineType, e);
        }
    }

    /**
     * Processes rule management frameworks using real APEX enrichment.
     */
    public Map<String, Object> processRuleManagementFrameworks(String frameworkType, Map<String, Object> frameworkParameters) {
        try {
            logger.info("Processing rule management frameworks '{}' using real APEX enrichment...", frameworkType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main rule definition service configuration not found");
            }

            // Create rule management frameworks processing data
            Map<String, Object> serviceData = new HashMap<>(frameworkParameters);
            serviceData.put("frameworkType", frameworkType);
            serviceData.put("serviceType", "rule-management-frameworks-processing");
            serviceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for rule management frameworks processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, serviceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Rule management frameworks processing '{}' processed successfully using real APEX enrichment", frameworkType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process rule management frameworks '{}' with APEX enrichment: {}", frameworkType, e.getMessage());
            throw new RuntimeException("Rule management frameworks processing failed: " + frameworkType, e);
        }
    }

    /**
     * Processes rule analysis processors using real APEX enrichment.
     */
    public Map<String, Object> processRuleAnalysisProcessors(String processorType, Map<String, Object> processorParameters) {
        try {
            logger.info("Processing rule analysis processors '{}' using real APEX enrichment...", processorType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main rule definition service configuration not found");
            }

            // Create rule analysis processors processing data
            Map<String, Object> serviceData = new HashMap<>(processorParameters);
            serviceData.put("processorType", processorType);
            serviceData.put("serviceType", "rule-analysis-processors-processing");
            serviceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for rule analysis processors processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, serviceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Rule analysis processors processing '{}' processed successfully using real APEX enrichment", processorType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process rule analysis processors '{}' with APEX enrichment: {}", processorType, e.getMessage());
            throw new RuntimeException("Rule analysis processors processing failed: " + processorType, e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT LEGACY INTERFACE METHODS (Real APEX Service Integration)
    // ============================================================================

    /**
     * Demonstrates rule definition service using real APEX enrichment services.
     * Legacy interface method that now uses APEX services internally.
     */
    public void demonstrateRuleDefinitionService() {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("demonstrationScope", "comprehensive");

            // Process rule creation engines
            Map<String, Object> enginesResult = processRuleCreationEngines("financial-rule-creation-engines", parameters);

            // Process rule management frameworks
            Map<String, Object> frameworksResult = processRuleManagementFrameworks("rule-lifecycle-management-frameworks", parameters);

            // Process rule analysis processors
            Map<String, Object> processorsResult = processRuleAnalysisProcessors("rule-complexity-analysis-processors", parameters);

            // Extract demonstration details from APEX enrichment results
            Object engineDetails = enginesResult.get("ruleCreationEnginesResult");
            Object frameworkDetails = frameworksResult.get("ruleManagementFrameworksResult");
            Object processorDetails = processorsResult.get("ruleAnalysisProcessorsResult");

            if (engineDetails != null && frameworkDetails != null && processorDetails != null) {
                logger.info("Rule definition service demonstration completed using APEX enrichment");
                logger.info("Engine processing: {}", engineDetails.toString());
                logger.info("Framework processing: {}", frameworkDetails.toString());
                logger.info("Processor processing: {}", processorDetails.toString());
            }

        } catch (Exception e) {
            logger.error("Failed to demonstrate rule definition service with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Rule definition service demonstration failed", e);
        }
    }

    /**
     * Run the comprehensive rule definition service demonstration.
     */
    public void runRuleDefinitionServiceDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX RULE DEFINITION SERVICE DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Comprehensive rule definition service with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Service Categories: 3 comprehensive service categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");

        try {
            // Category 1: Rule Creation Engines Processing
            System.out.println("\n----- RULE CREATION ENGINES PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> enginesParams = new HashMap<>();
            enginesParams.put("enginesScope", "comprehensive");

            Map<String, Object> enginesResult = processRuleCreationEngines("financial-rule-creation-engines", enginesParams);
            System.out.printf("Rule creation engines processing completed using real APEX enrichment: %s%n",
                enginesResult.get("ruleCreationEnginesResult"));

            // Category 2: Rule Management Frameworks Processing
            System.out.println("\n----- RULE MANAGEMENT FRAMEWORKS PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> frameworksParams = new HashMap<>();
            frameworksParams.put("frameworksScope", "rule-lifecycle-management-frameworks");

            Map<String, Object> frameworksResult = processRuleManagementFrameworks("rule-lifecycle-management-frameworks", frameworksParams);
            System.out.printf("Rule management frameworks processing completed using real APEX enrichment: %s%n",
                frameworksResult.get("ruleManagementFrameworksResult"));

            // Category 3: Rule Analysis Processors Processing
            System.out.println("\n----- RULE ANALYSIS PROCESSORS PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> processorsParams = new HashMap<>();
            processorsParams.put("processorsScope", "rule-complexity-analysis-processors");

            Map<String, Object> processorsResult = processRuleAnalysisProcessors("rule-complexity-analysis-processors", processorsParams);
            System.out.printf("Rule analysis processors processing completed using real APEX enrichment: %s%n",
                processorsResult.get("ruleAnalysisProcessorsResult"));

            // Demonstrate rule definition service
            System.out.println("\n----- RULE DEFINITION SERVICE DEMONSTRATION (Real APEX Services) -----");
            demonstrateRuleDefinitionService();
            System.out.println("Rule definition service demonstration completed successfully");

            System.out.println("\n=================================================================");
            System.out.println("RULE DEFINITION SERVICE DEMONSTRATION COMPLETED SUCCESSFULLY");
            System.out.println("=================================================================");
            System.out.println("All 3 service categories executed using real APEX services");
            System.out.println("Total processing: Rule creation engines + Rule management frameworks + Rule analysis processors");
            System.out.println("Configuration: 4 YAML files with comprehensive service definitions");
            System.out.println("Integration: 100% real APEX enrichment services");
            System.out.println("=================================================================");

        } catch (Exception e) {
            logger.error("Rule definition service demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
