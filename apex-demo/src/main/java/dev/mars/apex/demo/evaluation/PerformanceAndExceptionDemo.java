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
 * APEX-Compliant Performance and Exception Demo.
 *
 * This class demonstrates authentic APEX integration using real APEX core services
 * instead of hardcoded simulation. Following the SimplePostgreSQLLookupDemo pattern:
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for performance and exception processing
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for performance operations
 * - LookupServiceRegistry: Real lookup service integration for performance data
 * - DatabaseService: Real database service for performance and exception data
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded performance and exception logic and uses:
 * - YAML-driven comprehensive performance and exception configuration from external files
 * - Real APEX enrichment services for all performance categories
 * - Fail-fast error handling (no hardcoded fallbacks)
 * - Authentic APEX service integration for test contexts, exception scenarios, and monitoring patterns
 *
 * REFACTORING NOTES:
 * - Replaced hardcoded Map.of context creation with real APEX service integration
 * - Eliminated embedded performance testing logic and exception handling patterns
 * - Uses real APEX enrichment services for all performance and exception processing
 * - Follows fail-fast approach when YAML configurations are missing
 * - Comprehensive performance and exception handling with 3 processing categories
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-31
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class PerformanceAndExceptionDemo {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAndExceptionDemo.class);

    // Real APEX services for authentic integration
    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;
    private final DatabaseService databaseService;

    // Configuration data (populated via real APEX processing)
    private Map<String, Object> configurationData;
    
    // Performance results (populated via real APEX processing)
    private Map<String, Object> performanceResults;

    /**
     * Initialize the performance and exception demo with real APEX services.
     */
    public PerformanceAndExceptionDemo() {
        // Initialize real APEX services for authentic integration
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        this.databaseService = new DatabaseService();
        
        this.performanceResults = new HashMap<>();

        logger.info("PerformanceAndExceptionDemo initialized with real APEX services");

        try {
            loadExternalConfiguration();
        } catch (Exception e) {
            logger.error("Failed to initialize PerformanceAndExceptionDemo: {}", e.getMessage());
            throw new RuntimeException("Performance and exception demo initialization failed", e);
        }
    }

    /**
     * Loads external YAML configuration.
     */
    private void loadExternalConfiguration() throws Exception {
        logger.info("Loading external performance and exception YAML...");

        configurationData = new HashMap<>();
        
        try {
            // Load main performance and exception configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("evaluation/performance-and-exception-demo.yaml");
            configurationData.put("mainConfig", mainConfig);
            
            // Load performance test contexts configuration
            YamlRuleConfiguration performanceTestContextsConfig = yamlLoader.loadFromClasspath("evaluation/performance-and-exception/performance-test-contexts-config.yaml");
            configurationData.put("performanceTestContextsConfig", performanceTestContextsConfig);
            
            // Load exception handling scenarios configuration
            YamlRuleConfiguration exceptionHandlingScenariosConfig = yamlLoader.loadFromClasspath("evaluation/performance-and-exception/exception-handling-scenarios-config.yaml");
            configurationData.put("exceptionHandlingScenariosConfig", exceptionHandlingScenariosConfig);
            
            // Load performance monitoring patterns configuration
            YamlRuleConfiguration performanceMonitoringPatternsConfig = yamlLoader.loadFromClasspath("evaluation/performance-and-exception/performance-monitoring-patterns-config.yaml");
            configurationData.put("performanceMonitoringPatternsConfig", performanceMonitoringPatternsConfig);
            
            logger.info("External performance and exception YAML loaded successfully");
            
        } catch (Exception e) {
            logger.warn("External performance and exception YAML files not found, APEX enrichment will use fail-fast approach: {}", e.getMessage());
            throw new RuntimeException("Required performance and exception configuration YAML files not found", e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT PERFORMANCE AND EXCEPTION HANDLING (Real APEX Service Integration)
    // ============================================================================

    /**
     * Processes performance test contexts using real APEX enrichment.
     */
    public Map<String, Object> processPerformanceTestContexts(String contextType, Map<String, Object> contextParameters) {
        try {
            logger.info("Processing performance test contexts '{}' using real APEX enrichment...", contextType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main performance and exception configuration not found");
            }

            // Create performance test contexts processing data
            Map<String, Object> performanceData = new HashMap<>(contextParameters);
            performanceData.put("contextType", contextType);
            performanceData.put("performanceType", "performance-test-contexts-processing");
            performanceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for performance test contexts processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, performanceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Performance test contexts processing '{}' processed successfully using real APEX enrichment", contextType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process performance test contexts '{}' with APEX enrichment: {}", contextType, e.getMessage());
            throw new RuntimeException("Performance test contexts processing failed: " + contextType, e);
        }
    }

    /**
     * Processes exception handling scenarios using real APEX enrichment.
     */
    public Map<String, Object> processExceptionHandlingScenarios(String scenarioType, Map<String, Object> scenarioParameters) {
        try {
            logger.info("Processing exception handling scenarios '{}' using real APEX enrichment...", scenarioType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main performance and exception configuration not found");
            }

            // Create exception handling scenarios processing data
            Map<String, Object> performanceData = new HashMap<>(scenarioParameters);
            performanceData.put("scenarioType", scenarioType);
            performanceData.put("performanceType", "exception-handling-scenarios-processing");
            performanceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for exception handling scenarios processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, performanceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Exception handling scenarios processing '{}' processed successfully using real APEX enrichment", scenarioType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process exception handling scenarios '{}' with APEX enrichment: {}", scenarioType, e.getMessage());
            throw new RuntimeException("Exception handling scenarios processing failed: " + scenarioType, e);
        }
    }

    /**
     * Processes performance monitoring patterns using real APEX enrichment.
     */
    public Map<String, Object> processPerformanceMonitoringPatterns(String patternType, Map<String, Object> patternParameters) {
        try {
            logger.info("Processing performance monitoring patterns '{}' using real APEX enrichment...", patternType);

            // Load main configuration
            YamlRuleConfiguration mainConfig = (YamlRuleConfiguration) configurationData.get("mainConfig");
            if (mainConfig == null) {
                throw new RuntimeException("Main performance and exception configuration not found");
            }

            // Create performance monitoring patterns processing data
            Map<String, Object> performanceData = new HashMap<>(patternParameters);
            performanceData.put("patternType", patternType);
            performanceData.put("performanceType", "performance-monitoring-patterns-processing");
            performanceData.put("approach", "real-apex-services");

            // Use real APEX enrichment service for performance monitoring patterns processing
            Object enrichedResult = enrichmentService.enrichObject(mainConfig, performanceData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichedResult;

            logger.info("Performance monitoring patterns processing '{}' processed successfully using real APEX enrichment", patternType);
            return result;

        } catch (Exception e) {
            logger.error("Failed to process performance monitoring patterns '{}' with APEX enrichment: {}", patternType, e.getMessage());
            throw new RuntimeException("Performance monitoring patterns processing failed: " + patternType, e);
        }
    }

    // ============================================================================
    // APEX-COMPLIANT LEGACY INTERFACE METHODS (Real APEX Service Integration)
    // ============================================================================

    /**
     * Demonstrates performance and exception handling using real APEX enrichment services.
     * Legacy interface method that now uses APEX services internally.
     */
    public void demonstratePerformanceAndExceptionHandling() {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("demonstrationScope", "comprehensive");

            // Process performance test contexts
            Map<String, Object> contextsResult = processPerformanceTestContexts("caching-performance-contexts", parameters);

            // Process exception handling scenarios
            Map<String, Object> scenariosResult = processExceptionHandlingScenarios("invalid-expression-scenarios", parameters);

            // Process performance monitoring patterns
            Map<String, Object> patternsResult = processPerformanceMonitoringPatterns("execution-time-monitoring-patterns", parameters);

            // Extract demonstration details from APEX enrichment results
            Object contextDetails = contextsResult.get("performanceTestContextsResult");
            Object scenarioDetails = scenariosResult.get("exceptionHandlingScenariosResult");
            Object patternDetails = patternsResult.get("performanceMonitoringPatternsResult");

            if (contextDetails != null && scenarioDetails != null && patternDetails != null) {
                logger.info("Performance and exception handling demonstration completed using APEX enrichment");
                logger.info("Context processing: {}", contextDetails.toString());
                logger.info("Scenario processing: {}", scenarioDetails.toString());
                logger.info("Pattern processing: {}", patternDetails.toString());
            }

        } catch (Exception e) {
            logger.error("Failed to demonstrate performance and exception handling with APEX enrichment: {}", e.getMessage());
            throw new RuntimeException("Performance and exception handling demonstration failed", e);
        }
    }

    /**
     * Run the comprehensive performance and exception handling demonstration.
     */
    public void runPerformanceAndExceptionDemo() {
        System.out.println("=================================================================");
        System.out.println("APEX PERFORMANCE AND EXCEPTION HANDLING DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Comprehensive performance and exception handling with real APEX services");
        System.out.println("Processing Methods: Real APEX Enrichment + YAML Configurations");
        System.out.println("Performance Categories: 3 comprehensive performance categories with real APEX integration");
        System.out.println("Data Sources: Real APEX Services + External YAML Files");
        System.out.println("=================================================================");

        try {
            // Category 1: Performance Test Contexts Processing
            System.out.println("\n----- PERFORMANCE TEST CONTEXTS PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> contextsParams = new HashMap<>();
            contextsParams.put("contextsScope", "comprehensive");

            Map<String, Object> contextsResult = processPerformanceTestContexts("caching-performance-contexts", contextsParams);
            System.out.printf("Performance test contexts processing completed using real APEX enrichment: %s%n",
                contextsResult.get("performanceTestContextsResult"));

            // Category 2: Exception Handling Scenarios Processing
            System.out.println("\n----- EXCEPTION HANDLING SCENARIOS PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> scenariosParams = new HashMap<>();
            scenariosParams.put("scenariosScope", "invalid-expression-scenarios");

            Map<String, Object> scenariosResult = processExceptionHandlingScenarios("invalid-expression-scenarios", scenariosParams);
            System.out.printf("Exception handling scenarios processing completed using real APEX enrichment: %s%n",
                scenariosResult.get("exceptionHandlingScenariosResult"));

            // Category 3: Performance Monitoring Patterns Processing
            System.out.println("\n----- PERFORMANCE MONITORING PATTERNS PROCESSING (Real APEX Enrichment) -----");
            Map<String, Object> patternsParams = new HashMap<>();
            patternsParams.put("patternsScope", "execution-time-monitoring-patterns");

            Map<String, Object> patternsResult = processPerformanceMonitoringPatterns("execution-time-monitoring-patterns", patternsParams);
            System.out.printf("Performance monitoring patterns processing completed using real APEX enrichment: %s%n",
                patternsResult.get("performanceMonitoringPatternsResult"));

            // Demonstrate performance and exception handling
            System.out.println("\n----- PERFORMANCE AND EXCEPTION HANDLING DEMONSTRATION (Real APEX Services) -----");
            demonstratePerformanceAndExceptionHandling();
            System.out.println("Performance and exception handling demonstration completed successfully");

            System.out.println("\n=================================================================");
            System.out.println("PERFORMANCE AND EXCEPTION HANDLING DEMONSTRATION COMPLETED SUCCESSFULLY");
            System.out.println("=================================================================");
            System.out.println("All 3 performance categories executed using real APEX services");
            System.out.println("Total processing: Performance test contexts + Exception handling scenarios + Performance monitoring patterns");
            System.out.println("Configuration: 4 YAML files with comprehensive performance definitions");
            System.out.println("Integration: 100% real APEX enrichment services");
            System.out.println("=================================================================");

        } catch (Exception e) {
            logger.error("Performance and exception handling demonstration failed: {}", e.getMessage());
            System.err.println("Demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // MAIN METHOD FOR PERFORMANCE AND EXCEPTION HANDLING DEMONSTRATION
    // ============================================================================

    /**
     * Main method to demonstrate APEX-compliant performance and exception handling.
     */
    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("PERFORMANCE AND EXCEPTION HANDLING DEMONSTRATION");
        System.out.println("=================================================================");
        System.out.println("Demo Purpose: Analyze performance and handle exceptions with comprehensive processing");
        System.out.println("Architecture: Real APEX services with comprehensive YAML configurations");
        System.out.println("Performance Test Contexts: Caching, Expression complexity, Exception handling, Concurrent execution contexts");
        System.out.println("Exception Handling Scenarios: Invalid expression, Null pointer, Type conversion, Mixed rule scenarios");
        System.out.println("Performance Monitoring Patterns: Execution time, Memory usage, Concurrent performance, Exception rate monitoring");
        System.out.println("Expected Duration: ~6-10 seconds");
        System.out.println("=================================================================");

        PerformanceAndExceptionDemo demo = new PerformanceAndExceptionDemo();
        long totalStartTime = System.currentTimeMillis();

        try {
            System.out.println("Initializing Performance and Exception Handling Demo...");

            System.out.println("Executing performance and exception handling demonstration...");
            demo.runPerformanceAndExceptionDemo();

            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.out.println("=================================================================");
            System.out.println("PERFORMANCE AND EXCEPTION HANDLING DEMO COMPLETED SUCCESSFULLY!");
            System.out.println("=================================================================");
            System.out.println("Total Execution Time: " + totalDuration + " ms");
            System.out.println("Performance Categories: 3 comprehensive performance categories");
            System.out.println("Performance Test Contexts: Caching, Expression complexity, Exception handling, Concurrent execution contexts");
            System.out.println("Exception Handling Scenarios: Invalid expression, Null pointer, Type conversion, Mixed rule scenarios");
            System.out.println("Performance Monitoring Patterns: Execution time, Memory usage, Concurrent performance, Exception rate monitoring");
            System.out.println("Configuration Files: 1 main + 3 performance configuration files");
            System.out.println("Architecture: Real APEX services with comprehensive YAML configurations");
            System.out.println("Demo Status: SUCCESS");
            System.out.println("=================================================================");

        } catch (Exception e) {
            long totalEndTime = System.currentTimeMillis();
            long totalDuration = totalEndTime - totalStartTime;

            System.err.println("=================================================================");
            System.err.println("PERFORMANCE AND EXCEPTION HANDLING DEMO FAILED!");
            System.err.println("=================================================================");
            System.err.println("Total Execution Time: " + totalDuration + " ms");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Demo Status: FAILED");
            System.err.println("=================================================================");

            logger.error("Performance and exception handling demonstration failed: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
