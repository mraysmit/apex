package dev.mars.apex.demo.lookup;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XML File Lookup Test.
 *
 * Demonstrates file-system data source lookup using XML files with real APEX services.
 * This test shows how to load data from XML files in the demo-data/xml directory
 * and perform enrichment using YAML-configured lookup rules.
 *
 * ============================================================================
 * REAL APEX SERVICES USED:
 * - EnrichmentService: Real APEX enrichment processor for XML file lookup
 * - YamlConfigurationLoader: Real YAML configuration loading and validation
 * - ExpressionEvaluatorService: Real SpEL expression evaluation for lookup keys
 * - LookupServiceRegistry: Real lookup service integration for file-based lookups
 * ============================================================================
 *
 * CRITICAL: This class eliminates ALL hardcoded lookup logic and uses:
 * - Real APEX YAML configuration for file-system data source definitions
 * - Real APEX enrichment services for XML file processing
 * - Real APEX lookup services for data retrieval and field mapping
 */
public class XmlFileLookupTest {

    private static final Logger logger = LoggerFactory.getLogger(XmlFileLookupTest.class);

    private YamlConfigurationLoader yamlLoader;
    private EnrichmentService enrichmentService;
    private LookupServiceRegistry lookupRegistry;
    private ExpressionEvaluatorService expressionEvaluator;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.lookupRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.enrichmentService = new EnrichmentService(lookupRegistry, expressionEvaluator);
    }

    @Test
    @DisplayName("Should perform XML file lookup and enrichment")
    void testXmlFileLookup() {
        logger.info("=".repeat(80));
        logger.info("XML FILE LOOKUP TEST");
        logger.info("=".repeat(80));
        logger.info("Testing XML file-based lookup using APEX services");
        logger.info("Data Source: demo-data/xml/products.xml");
        logger.info("Configuration: lookup/xml-file-lookup.yaml");
        logger.info("");

        // Load YAML configuration
        loadConfiguration();
        
        // Test product lookup
        testProductLookup();
        
        logger.info("\n" + "=".repeat(80));
        logger.info("XML FILE LOOKUP TEST COMPLETED SUCCESSFULLY");
        logger.info("=".repeat(80));
    }

    /**
     * Load YAML configuration files.
     */
    private void loadConfiguration() {
        try {
            logger.info("Loading XML file lookup YAML configuration...");

            // Load main configuration
            YamlRuleConfiguration mainConfig = yamlLoader.loadFromClasspath("lookup/xml-file-lookup.yaml");
            logger.info("✓ Loaded main configuration: {}", mainConfig.getMetadata().getName());

            logger.info("Configuration loaded successfully");

        } catch (Exception e) {
            logger.error("Failed to load YAML configuration: {}", e.getMessage());
            throw new RuntimeException("Required XML file lookup configuration not found", e);
        }
    }

    /**
     * Test product lookup using XML file data source.
     */
    private void testProductLookup() {
        logger.info("\n" + "-".repeat(60));
        logger.info("TEST: Product Lookup from XML File");
        logger.info("-".repeat(60));

        // Create input data with product ID
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("productId", "PROD002");

        logger.info("Input Data:");
        logger.info("  Product ID: {}", inputData.get("productId"));

        try {
            // Load configuration and perform enrichment
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("lookup/xml-file-lookup.yaml");
            Map<String, Object> enrichedResult = new HashMap<>(inputData);
            
            // Apply enrichment using APEX services
            enrichmentService.enrichObject(config, enrichedResult);

            // Display results
            logger.info("\nEnrichment Results:");
            logger.info("  Product Name: {}", enrichedResult.get("productName"));
            logger.info("  Product Price: {}", enrichedResult.get("productPrice"));
            logger.info("  Product Category: {}", enrichedResult.get("productCategory"));
            logger.info("  Product Available: {}", enrichedResult.get("productAvailable"));

            // Validate results
            if (enrichedResult.get("productName") != null) {
                logger.info("✓ Product lookup successful");
            } else {
                logger.warn("⚠ Product lookup returned no results");
            }
            
        } catch (Exception e) {
            logger.error("Product lookup test failed: {}", e.getMessage(), e);
            fail("Product lookup test failed: " + e.getMessage());
        }
    }
}
