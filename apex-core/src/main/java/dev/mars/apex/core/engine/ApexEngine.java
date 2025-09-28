package dev.mars.apex.core.engine;

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

import dev.mars.apex.core.service.classification.*;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced APEX Engine with integrated input data classification.
 * 
 * Provides a simple, unified API for classifying and processing any input data
 * from any transport mechanism (message queues, REST APIs, file systems, etc.).
 * 
 * CORE FUNCTIONALITY:
 * - Single-method API for classify-and-process operations
 * - Multi-layer input data classification
 * - Transport-agnostic design
 * - Comprehensive error handling and fallback mechanisms
 * - Performance monitoring and audit trails
 * 
 * DESIGN PRINCIPLES:
 * - Simple API hiding complex classification logic
 * - Conservative approach respecting existing APEX patterns
 * - Backward compatibility with existing scenarios
 * - Comprehensive error handling with meaningful messages
 * 
 * USAGE EXAMPLES:
 * 
 * Basic usage:
 * ```java
 * ApexEngine engine = new ApexEngine();
 * engine.loadScenarios("config/data-type-scenarios.yaml");
 * ApexProcessingResult result = engine.classifyAndProcessData(jsonString);
 * ```
 * 
 * Advanced usage with context:
 * ```java
 * ApexProcessingContext context = ApexProcessingContext.builder()
 *     .source("rabbitmq")
 *     .fileName("trade_data.json")
 *     .metadata(Map.of("region", "US", "priority", "HIGH"))
 *     .build();
 * 
 * ApexProcessingResult result = engine.classifyAndProcessData(inputData, context);
 * ```
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ApexEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ApexEngine.class);
    
    private final EnhancedDataTypeScenarioService scenarioService;
    
    /**
     * Creates a new APEX Engine with default configuration.
     */
    public ApexEngine() {
        this.scenarioService = new EnhancedDataTypeScenarioService();
        logger.info("APEX Engine initialized with enhanced classification capabilities");
    }
    
    /**
     * Creates a new APEX Engine with custom scenario service.
     * Useful for testing and custom configurations.
     */
    public ApexEngine(EnhancedDataTypeScenarioService scenarioService) {
        this.scenarioService = scenarioService != null ? scenarioService : new EnhancedDataTypeScenarioService();
        logger.info("APEX Engine initialized with custom scenario service");
    }
    
    /**
     * Loads scenario configurations from a registry file.
     * 
     * @param registryPath path to the scenario registry YAML file
     * @throws RuntimeException if scenarios cannot be loaded
     */
    public void loadScenarios(String registryPath) {
        try {
            logger.info("Loading scenarios from registry: {}", registryPath);
            scenarioService.loadScenarios(registryPath);
            logger.info("Successfully loaded scenarios from: {}", registryPath);
        } catch (Exception e) {
            logger.error("Failed to load scenarios from: {}", registryPath, e);
            throw new RuntimeException("Failed to load scenarios: " + e.getMessage(), e);
        }
    }
    
    /**
     * Main entry point: Classify input data and process through appropriate scenario.
     * 
     * This method provides the complete classify-and-process workflow:
     * 1. Classifies the input data using multi-layer classification
     * 2. Routes to the appropriate scenario based on classification
     * 3. Processes the data through the scenario's stages
     * 4. Returns comprehensive results with audit trail
     * 
     * @param inputData Raw input data (String, byte[], InputStream, File, etc.)
     * @param context Processing context with metadata
     * @return Complete processing result with classification details
     */
    public ApexProcessingResult classifyAndProcessData(Object inputData, ApexProcessingContext context) {
        if (inputData == null) {
            return ApexProcessingResult.failed("Input data cannot be null");
        }
        
        if (context == null) {
            context = ApexProcessingContext.defaultContext();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Starting classify-and-process for data from source: {}", context.getSource());
            
            // Step 1: Classify the input data
            ClassificationResult classification = scenarioService.classifyInputData(inputData, context);
            
            // Step 2: Handle classification failures with fallback
            if (classification.failed()) {
                return handleClassificationFailure(inputData, context, classification, startTime);
            }
            
            logger.debug("Classification successful: scenario={}, confidence={}", 
                        classification.getScenarioId(), classification.getConfidence());
            
            // Step 3: Process through identified scenario
            Object processingResult = scenarioService.processDataWithScenario(
                classification.getParsedData(), 
                classification.getScenario()
            );
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Step 4: Return comprehensive result
            ApexProcessingResult result = ApexProcessingResult.successful(
                classification, 
                processingResult, 
                executionTime
            );
            
            logger.info("Successfully processed data from {} with scenario {} in {}ms", 
                       context.getSource(), classification.getScenarioId(), executionTime);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Processing failed for data from source: {} ({}ms)", 
                        context.getSource(), executionTime, e);
            return ApexProcessingResult.failed(e);
        }
    }
    
    /**
     * Simplified entry point for basic use cases.
     * Uses default processing context.
     * 
     * @param inputData Raw input data to classify and process
     * @return Complete processing result
     */
    public ApexProcessingResult classifyAndProcessData(Object inputData) {
        return classifyAndProcessData(inputData, ApexProcessingContext.defaultContext());
    }
    
    /**
     * Handles classification failures with fallback mechanisms.
     * 
     * This method implements the fallback strategy when classification fails:
     * 1. Try to use existing data type matching as fallback
     * 2. Log detailed information for debugging
     * 3. Return meaningful error messages
     */
    private ApexProcessingResult handleClassificationFailure(Object inputData, 
                                                           ApexProcessingContext context,
                                                           ClassificationResult classification,
                                                           long startTime) {
        
        logger.warn("Classification failed for data from {}: {}", 
                   context.getSource(), classification.getErrorMessage());
        
        // Fallback: Try existing data type matching
        try {
            ScenarioConfiguration fallbackScenario = scenarioService.getScenarioForData(inputData);
            
            if (fallbackScenario != null) {
                logger.info("Using fallback scenario: {}", fallbackScenario.getScenarioId());
                
                Object processingResult = scenarioService.processDataWithScenario(inputData, fallbackScenario);
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Create a basic classification result for the fallback
                ClassificationResult fallbackClassification = ClassificationResult.builder()
                    .successful(true)
                    .fileFormat("unknown")
                    .contentType("fallback")
                    .businessClassification("fallback")
                    .scenarioId(fallbackScenario.getScenarioId())
                    .scenario(fallbackScenario)
                    .parsedData(inputData)
                    .confidence(0.5) // Lower confidence for fallback
                    .cacheable(false) // Don't cache fallback results
                    .build();
                
                return ApexProcessingResult.successful(fallbackClassification, processingResult, executionTime);
            }
            
        } catch (Exception fallbackError) {
            logger.error("Fallback processing also failed", fallbackError);
        }
        
        // No fallback available
        long executionTime = System.currentTimeMillis() - startTime;
        return ApexProcessingResult.failed(classification, 
            "Classification failed and no fallback scenario available: " + classification.getErrorMessage());
    }
    
    /**
     * Gets the underlying scenario service for advanced operations.
     * Useful for testing and monitoring.
     */
    public EnhancedDataTypeScenarioService getScenarioService() {
        return scenarioService;
    }
}
