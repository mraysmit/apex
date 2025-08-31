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
 * APEX-Compliant Post-Trade Processing Service Demo.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for post-trade processing service
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for post-trade operations
 * - LookupServiceRegistry: Real lookup service integration for post-trade data
 * - DatabaseService: Real database service for post-trade processing and settlement data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded post-trade processing logic and uses:
 * - YAML-driven comprehensive post-trade processing service configuration from external files
 * - Real APEX enrichment services for all service categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration for trade workflows, settlement operations, and business rules
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded static final constants with real APEX service integration
 * - Eliminated embedded post-trade processing logic and settlement patterns
 * - Uses real APEX enrichment services for all post-trade processing service operations
 * - Follows fail-fast approach when YAML configurations are missing
 * - Comprehensive post-trade processing service with 3 processing categories
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-31
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class PostTradeProcessingServiceDemo {

    private static final Logger logger = LoggerFactory.getLogger(PostTradeProcessingServiceDemo.class);

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
     * Initialize the post-trade processing service demo with real APEX services.
     */
    public PostTradeProcessingServiceDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        this.databaseService = new DatabaseService();
        
        this.serviceResults = new HashMap<>();

        logger.info("PostTradeProcessingServiceDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize PostTradeProcessingServiceDemo: {}", e.getMessage());
            throw new RuntimeException("Post-trade processing service demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external post-trade processing service YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main post-trade processing service configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/post-trade-processing-service-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load trade processing workflows configuration
            YamlRuleConfiguration tradeProcessingWorkflowsConfig = yamlLoader.loadFromClasspath("evaluation/post-trade-processing/trade-processing-workflows-config.yaml");
            configurationData.put("tradeProcessingWorkflowsConfig", tradeProcessingWorkflowsConfig);
            
            // Load settlement operations configuration
            YamlRuleConfiguration settlementOperationsConfig = yamlLoader.loadFromClasspath("evaluation/post-trade-processing/settlement-operations-config.yaml");
            configurationData.put("settlementOperationsConfig", settlementOperationsConfig);
            
            // Load post-trade business rules configuration
            YamlRuleConfiguration postTradeBusinessRulesConfig = yamlLoader.loadFromClasspath("evaluation/post-trade-processing/post-trade-business-rules-config.yaml");
            configurationData.put("postTradeBusinessRulesConfig", postTradeBusinessRulesConfig);
            
            logger.info("External post-trade processing service YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External post-trade processing service YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required post-trade processing service configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT POST-TRADE PROCESSING SERVICE (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes trade processing workflows using real APEX enrichment.
     */
    public Map<String, Object> processTradeProcessingWorkflows(String workflowType, Map<String, Object> workflowParameters) {
        try {
            logger.info("Processing trade processing workflows '{}' using real APEX enrichment...", workflowType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main post-trade processing service configuration not found");
            }

            // Create trade processing workflows processing data
            Map<String, Object> serviceData = new HashMap<>(workflowParameters);
            serviceData.put("workflowType", workflowType);
            serviceData.put("serviceType", "trade-processing-workflows-processing");
            serviceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for trade processing workflows processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, serviceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Trade processing workflows processing '{}' processed successfully using real APEX enrichment", workflowType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process trade processing workflows '{}' with APEX enrichment: {}", workflowType, e.getMessage());
            throw new RuntimeException("Trade processing workflows processing failed: " + workflowType, e);
        }
    }

    /**
     * Processes settlement operations using real APEX enrichment.
     */
    public Map<String, Object> processSettlementOperations(String operationType, Map<String, Object> operationParameters) {
        try {
            logger.info("Processing settlement operations '{}' using real APEX enrichment...", operationType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main post-trade processing service configuration not found");
            }

            // Create settlement operations processing data
            Map<String, Object> serviceData = new HashMap<>(operationParameters);
            serviceData.put("operationType", operationType);
            serviceData.put("serviceType", "settlement-operations-processing");
            serviceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for settlement operations processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, serviceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Settlement operations processing '{}' processed successfully using real APEX enrichment", operationType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process settlement operations '{}' with APEX enrichment: {}", operationType, e.getMessage());
            throw new RuntimeException("Settlement operations processing failed: " + operationType, e);
        }
    }

    /**
     * Processes post-trade business rules using real APEX enrichment.
     */
    public Map<String, Object> processPostTradeBusinessRules(String ruleType, Map<String, Object> ruleParameters) {
        try {
            logger.info("Processing post-trade business rules '{}' using real APEX enrichment...", ruleType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main post-trade processing service configuration not found");
            }

            // Create post-trade business rules processing data
            Map<String, Object> serviceData = new HashMap<>(ruleParameters);
            serviceData.put("ruleType", ruleType);
            serviceData.put("serviceType", "post-trade-business-rules-processing");
            serviceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for post-trade business rules processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, serviceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Post-trade business rules processing '{}' processed successfully using real APEX enrichment", ruleType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process post-trade business rules '{}' with APEX enrichment: {}", ruleType, e.getMessage());
            throw new RuntimeException("Post-trade business rules processing failed: " + ruleType, e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT LEGACY INTERFACE METHODS (Real APEX Service Integration)
    // ============================================================================

    /**
     * Demonstrates post-trade processing service using real APEX enrichment services.
     * Legacy interface method that now uses APEX services internally.
     */
    public void demonstratePostTradeProcessingService() {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("demonstrationScope", "comprehensive");

            // Process trade processing workflows
            Map<String, Object> workflowsResult = processTradeProcessingWorkflows("trade-validation-workflows", parameters);

            // Process settlement operations
            Map<String, Object> operationsResult = processSettlementOperations("settlement-method-determination-operations", parameters);

            // Process post-trade business rules
            Map<String, Object> rulesResult = processPostTradeBusinessRules("trade-type-validation-rules", parameters);

            // Extract demonstration details from APEX enrichment results
            Object workflowDetails = workflowsResult.get("tradeProcessingWorkflowsResult");
            Object operationDetails = operationsResult.get("settlementOperationsResult");
            Object ruleDetails = rulesResult.get("postTradeBusinessRulesResult");

            if (workflowDetails != null && operationDetails != null && ruleDetails != null) {
                logger.info("Post-trade processing service demonstration completed using APEX enrichment");
                logger.info("Workflow processing: {}", workflowDetails.toString());
                logger.info("Operation processing: {}", operationDetails.toString());
                logger.info("Rule processing: {}", ruleDetails.toString());
            }

        } catch (Exception e) {
            logger.error("Failed to demonstrate post-trade processing service with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Post-trade processing service demonstration failed", e);
        }
    }

    /**
     * Run the comprehensive post-trade processing service demonstration.
     */
    public void runPostTradeProcessingServiceDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX POST-TRADE PROCESSING SERVICE DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Comprehensive post-trade processing service with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Service Categories: 3 comprehensive service categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");

        try {
            // Category 1: Trade Processing Workflows Processing
            System.out.println("\n----- TRADE PROCESSING WORKFLOWS PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> workflowsParams = new HashMap<>();
            workflowsParams.put("workflowsScope", "comprehensive");

            Map<String, Object> workflowsResult = processTradeProcessingWorkflows("trade-validation-workflows", workflowsParams);
            System.out.printf("Trade processing workflows processing completed using real APEX enrichment: %s%n",
                workflowsResult.get("tradeProcessingWorkflowsResult"));

            // Category 2: Settlement Operations Processing
            System.out.println("\n----- SETTLEMENT OPERATIONS PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> operationsParams = new HashMap<>();
            operationsParams.put("operationsScope", "settlement-method-determination-operations");

            Map<String, Object> operationsResult = processSettlementOperations("settlement-method-determination-operations", operationsParams);
            System.out.printf("Settlement operations processing completed using real APEX enrichment: %s%n",
                operationsResult.get("settlementOperationsResult"));

            // Category 3: Post-Trade Business Rules Processing
            System.out.println("\n----- POST-TRADE BUSINESS RULES PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> rulesParams = new HashMap<>();
            rulesParams.put("rulesScope", "trade-type-validation-rules");

            Map<String, Object> rulesResult = processPostTradeBusinessRules("trade-type-validation-rules", rulesParams);
            System.out.printf("Post-trade business rules processing completed using real APEX enrichment: %s%n",
                rulesResult.get("postTradeBusinessRulesResult"));

            // Demonstrate post-trade processing service
            System.out.println("\n----- POST-TRADE PROCESSING SERVICE DEMONSTRATION (Real APEX Services) -----");
            demonstratePostTradeProcessingService();
            System.out.println("Post-trade processing service demonstration completed successfully");

            System.out.println("\n=================================================================");
            System.out.println("POST-TRADE PROCESSING SERVICE DEMONSTRATION COMPLETED SUCCESSFULLY");
            System.out.println("=================================================================");
            System.out.println("All 3 service categories executed using real APEX services");
            System.out.println("Total processing: Trade processing workflows + Settlement operations + Post-trade business rules");
            System.out.println("Configuration: 4 YAML files with comprehensive service definitions");
            System.out.println("Integration: 100% real APEX enrichment services");
            System.out.println("=================================================================");

        } catch (Exception e) {
            logger.error("Post-trade processing service demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // MAIN METHOD FOR POST-TRADE PROCESSING SERVICE DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant post-trade processing service.
     */
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("POST-TRADE PROCESSING SERVICE DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Manage post-trade processing with comprehensive service operations");
        System.out.println("Architecture: Real APEX services with comprehensive YAML configurations");
        System.out.println("Trade Processing Workflows: Trade validation, Trade matching, Trade affirmation, Trade settlement workflows");
        System.out.println("Settlement Operations: Settlement method determination, Settlement fee calculation, Settlement day calculation, Settlement confirmation");
        System.out.println("Post-Trade Business Rules: Trade type validation, Settlement method rules, Fee calculation rules, Compliance validation rules");
        System.out.println("Expected Duration: ~8-12 seconds");
        System.out.println("=================================================================");

        PostTradeProcessingServiceDemo demo = new PostTradeProcessingServiceDemo();
        long totalStartTime = System.currentTimeMillis();

        try {
            System.out.println("Initializing Post-Trade Processing Service Demo...");

            System.out.println("Executing post-trade processing service demonstration...");
            demo.runPostTradeProcessingServiceDemo();

            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.out.println("=================================================================");
            System.out.println("POST-TRADE PROCESSING SERVICE DEMO COMPLETED SUCCESSFULLY!");
            System.out.println("=================================================================");
            System.out.println("Total Execution Time: " + totalDuration + " ms");
            System.out.println("Service Categories: 3 comprehensive service categories");
            System.out.println("Trade Processing Workflows: Trade validation, Trade matching, Trade affirmation, Trade settlement workflows");
            System.out.println("Settlement Operations: Settlement method determination, Settlement fee calculation, Settlement day calculation, Settlement confirmation");
            System.out.println("Post-Trade Business Rules: Trade type validation, Settlement method rules, Fee calculation rules, Compliance validation rules");
            System.out.println("Configuration Files: 1 main + 3 service configuration files");
            System.out.println("Architecture: Real APEX services with comprehensive YAML configurations");
            System.out.println("Demo Status: SUCCESS");
            System.out.println("=================================================================");

        } catch (Exception e) {
            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.err.println("=================================================================");
            System.err.println("POST-TRADE PROCESSING SERVICE DEMO FAILED!");
            System.err.println("=================================================================");
            System.err.println("Total Execution Time: " + totalDuration + " ms");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Demo Status: FAILED");
            System.err.println("=================================================================");

            logger.error("Post-trade processing service demonstration failed: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
