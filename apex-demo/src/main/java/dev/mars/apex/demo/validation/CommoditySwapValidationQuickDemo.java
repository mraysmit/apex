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
 * APEX Commodity Swap Validation Quick Demo.
 *
 * Demonstrates layered API approach with performance monitoring using pure YAML-driven configuration.
 * Unique features: Ultra-Simple API, Template-Based Rules, Advanced Configuration, Performance Monitoring.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class CommoditySwapValidationQuickDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommoditySwapValidationQuickDemo.class);

    /**
     * Run the commodity swap validation quick demo.
     */
    public void runDemo() throws Exception {
        LOGGER.info("=== APEX Commodity Swap Validation Quick Demo ===");
        LOGGER.info("Demonstrating layered API approach with performance monitoring");

        // Step 1: Load YAML configuration
        YamlRuleConfiguration config = loadConfiguration();

        // Step 2: Call APEX core enrichment processor
        EnrichmentService enrichmentService = createEnrichmentService();

        // Step 3: Process sample data for layered API demonstration
        demonstrateLayeredAPIs(enrichmentService, config);

        LOGGER.info("=== Demo completed successfully ===");
    }
    /**
     * Load YAML configuration (APEX way).
     */
    private YamlRuleConfiguration loadConfiguration() throws Exception {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        return loader.loadFromClasspath("validation/commodity-swap-validation-quick-demo.yaml");
    }

    /**
     * Create APEX enrichment service.
     */
    private EnrichmentService createEnrichmentService() {
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        return new EnrichmentService(serviceRegistry, expressionEvaluator);
    }

    /**
     * Demonstrate layered APIs using APEX enrichment.
     */
    private void demonstrateLayeredAPIs(EnrichmentService enrichmentService, YamlRuleConfiguration config) throws Exception {
        LOGGER.info("=== LAYERED API DEMONSTRATION ===");

        // Layer 1: Ultra-Simple API
        Map<String, Object> layer1Data = createSampleData("ultra-simple-api");
        Object layer1Result = enrichmentService.enrichObject(config, layer1Data);
        LOGGER.info("Layer 1 (Ultra-Simple API) result: {}", layer1Result);

        // Layer 2: Template-Based Rules
        Map<String, Object> layer2Data = createSampleData("template-based-rules");
        Object layer2Result = enrichmentService.enrichObject(config, layer2Data);
        LOGGER.info("Layer 2 (Template-Based Rules) result: {}", layer2Result);

        // Layer 3: Advanced Configuration with Performance Monitoring
        Map<String, Object> layer3Data = createSampleData("advanced-configuration");
        Object layer3Result = enrichmentService.enrichObject(config, layer3Data);
        LOGGER.info("Layer 3 (Advanced Configuration) result: {}", layer3Result);

        // Static Data Validation
        Map<String, Object> staticData = createSampleData("static-data-validation");
        Object staticResult = enrichmentService.enrichObject(config, staticData);
        LOGGER.info("Static Data Validation result: {}", staticResult);
    }

    /**
     * Create sample data for different API layers.
     */
    private Map<String, Object> createSampleData(String apiLayer) {
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", "TRS001");
        data.put("counterpartyId", "CP001");
        data.put("clientId", "CLI001");
        data.put("commodityType", "ENERGY");
        data.put("referenceIndex", "WTI");
        data.put("notionalAmount", "10000000");
        data.put("notionalCurrency", "USD");
        data.put("apiLayer", apiLayer);
        data.put("validationType", "commodity-swap-validation");
        return data;
    }

    /**
     * Main method to run the demo.
     */
    public static void main(String[] args) {
        try {
            CommoditySwapValidationQuickDemo demo = new CommoditySwapValidationQuickDemo();
            demo.runDemo();
        } catch (Exception e) {
            LOGGER.error("Demo failed: {}", e.getMessage(), e);
        }
    }
}
