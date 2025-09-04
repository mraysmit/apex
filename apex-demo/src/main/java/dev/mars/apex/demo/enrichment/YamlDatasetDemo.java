package dev.mars.apex.demo.enrichment;

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
 * YAML Dataset Demo - Real APEX Service Integration Template
 * 
 * This demo demonstrates authentic APEX YAML dataset enrichment functionality using real APEX services:
 * - Inline datasets embedded directly in YAML files
 * - Field mappings for data transformation  
 * - Conditional processing based on data content
 * - Performance benefits of in-memory lookups
 * - Business-editable reference data management
 * 
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation
 * - LookupServiceRegistry: Real lookup service management
 *
 * NO HARDCODED SIMULATION: All processing uses authentic APEX core services with YAML-driven configuration.
 * NO FALLBACK SCENARIOS: Fail fast approach - no hardcoded fallback logic allowed.
 *
 * YAML FILES REQUIRED:
 * - yaml-dataset-demo-config.yaml: Dataset configurations and enrichment definitions
 * - yaml-dataset-demo-data.yaml: Test data scenarios for demonstration
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 - Real APEX services integration (Reference Template)
 */
public class YamlDatasetDemo {

    private static final Logger logger = LoggerFactory.getLogger(YamlDatasetDemo.class);

    private final YamlConfigurationLoader yamlLoader;
    private final EnrichmentService enrichmentService;
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService expressionEvaluator;

    /**
     * Constructor initializes real APEX services - NO HARDCODED SIMULATION
     */
    public YamlDatasetDemo() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.serviceRegistry = new LookupServiceRegistry();
        this.enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        logger.info("YamlDatasetDemo initialized with real APEX services");
    }

    /**
     * Main demonstration method using real APEX services - NO HARDCODED SIMULATION
     */
    public static void main(String[] args) {
        logger.info("=== YAML Dataset Demo - Real APEX Services Integration ===");
        logger.info("Demonstrating authentic APEX YAML dataset functionality with real services");

        YamlDatasetDemo demo = new YamlDatasetDemo();
        demo.runDemo();
    }

    /**
     * Standard run method for demo runner compatibility
     */
    public void run() {
        runDemo();
    }

    /**
     * Main demo execution method using real APEX services - NO HARDCODED SIMULATION
     */
    public void runDemo() {
        try {
            logger.info("\n=== YAML Dataset Demo - Real Service Integration ===");
            
            // Load YAML configuration using real APEX services
            YamlRuleConfiguration config = loadConfiguration();
            
            // Demonstrate dataset-based enrichment scenarios
            demonstrateDatasetEnrichment(config);
            
            // Demonstrate field mapping functionality
            demonstrateFieldMappingProcessing(config);
            
            // Demonstrate conditional dataset processing
            demonstrateConditionalDatasetProcessing(config);
            
            logger.info("✅ Demo completed successfully using real APEX services");
            
        } catch (Exception e) {
            logger.error("❌ Demo failed: " + e.getMessage(), e);
            throw new RuntimeException("Demo execution failed", e);
        }
    }

    /**
     * Load YAML configuration using real APEX services - NO HARDCODED DATA
     */
    private YamlRuleConfiguration loadConfiguration() {
        try {
            logger.info("Loading YAML configuration from yaml-dataset-demo-config.yaml");
            
            // Load configuration using real APEX YamlConfigurationLoader
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("enrichment/yaml-dataset-demo-config.yaml");
            
            if (config == null) {
                throw new IllegalStateException("Failed to load YAML configuration - file not found or invalid");
            }
            
            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            return config;
            
        } catch (Exception e) {
            logger.error("❌ Failed to load YAML configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    /**
     * Demonstrate dataset-based enrichment using real APEX services - NO HARDCODED SIMULATION
     */
    private void demonstrateDatasetEnrichment(YamlRuleConfiguration config) {
        try {
            logger.info("\n=== Dataset-Based Enrichment Demo ===");
            
            // Create minimal input data for enrichment
            Map<String, Object> datasetData = new HashMap<>();
            datasetData.put("currencyCode", "USD");
            datasetData.put("countryCode", "US");
            datasetData.put("productType", "EQUITY");
            
            // Use real APEX EnrichmentService to process the data
            Object enrichedResult = enrichmentService.enrichObject(config, datasetData);
            
            logger.info("✅ Dataset enrichment completed using real APEX services");
            logger.info("Input data: " + datasetData);
            logger.info("Enriched result: " + enrichedResult);
            
        } catch (Exception e) {
            logger.error("❌ Dataset enrichment failed", e);
            throw new RuntimeException("Dataset enrichment failed", e);
        }
    }

    /**
     * Demonstrate field mapping processing using real APEX services - NO HARDCODED SIMULATION
     */
    private void demonstrateFieldMappingProcessing(YamlRuleConfiguration config) {
        try {
            logger.info("\n=== Field Mapping Processing Demo ===");
            
            // Create minimal input data for field mapping
            Map<String, Object> mappingData = new HashMap<>();
            mappingData.put("sourceField1", "originalValue1");
            mappingData.put("sourceField2", "originalValue2");
            mappingData.put("transformField", "TRANSFORM_ME");
            
            // Use real APEX EnrichmentService to process field mappings
            Object mappedResult = enrichmentService.enrichObject(config, mappingData);
            
            logger.info("✅ Field mapping completed using real APEX services");
            logger.info("Input data: " + mappingData);
            logger.info("Mapped result: " + mappedResult);
            
        } catch (Exception e) {
            logger.error("❌ Field mapping failed", e);
            throw new RuntimeException("Field mapping failed", e);
        }
    }

    /**
     * Demonstrate conditional dataset processing using real APEX services - NO HARDCODED SIMULATION
     */
    private void demonstrateConditionalDatasetProcessing(YamlRuleConfiguration config) {
        try {
            logger.info("\n=== Conditional Dataset Processing Demo ===");
            
            // Create minimal input data for conditional processing
            Map<String, Object> conditionalData = new HashMap<>();
            conditionalData.put("processingType", "CONDITIONAL");
            conditionalData.put("riskLevel", "HIGH");
            conditionalData.put("customerTier", "PREMIUM");
            
            // Use real APEX EnrichmentService to process conditional logic
            Object conditionalResult = enrichmentService.enrichObject(config, conditionalData);
            
            logger.info("✅ Conditional processing completed using real APEX services");
            logger.info("Input data: " + conditionalData);
            logger.info("Conditional result: " + conditionalResult);
            
        } catch (Exception e) {
            logger.error("❌ Conditional processing failed", e);
            throw new RuntimeException("Conditional processing failed", e);
        }
    }
}
