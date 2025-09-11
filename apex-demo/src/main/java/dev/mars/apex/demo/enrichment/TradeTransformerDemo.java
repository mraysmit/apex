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
 * APEX Trade Transformer Demo.
 *
 * Simple demo that loads YAML configuration and calls APEX core enrichment processor.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class TradeTransformerDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeTransformerDemo.class);

    /**
     * Run the trade transformer demo.
     */
    public void runDemo() throws Exception {
        LOGGER.info("=== APEX Trade Transformer Demo ===");

        // Step 1: Load YAML configuration
        YamlRuleConfiguration config = loadConfiguration();

        // Step 2: Call APEX core enrichment processor
        EnrichmentService enrichmentService = createEnrichmentService();

        // Step 3: Process sample data
        Map<String, Object> sampleData = createSampleData();
        Object result = enrichmentService.enrichObject(config, sampleData);

        LOGGER.info("Enrichment result: {}", result);
        LOGGER.info("=== Demo completed successfully ===");
    }

    /**
     * Load YAML configuration (APEX way).
     */
    private YamlRuleConfiguration loadConfiguration() throws Exception {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        return loader.loadFromClasspath("enrichment/trade-transformer-demo.yaml");
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
     * Create sample data for processing.
     */
    private Map<String, Object> createSampleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", "HP001");
        data.put("instrumentType", "Equity");
        data.put("category", "InstrumentType");
        data.put("value", "1000000");
        data.put("currency", "USD");
        data.put("transformationType", "trade-transformation");
        return data;
    }

    /**
     * Main method to run the demo.
     */
    public static void main(String[] args) {
        try {
            TradeTransformerDemo demo = new TradeTransformerDemo();
            demo.runDemo();
        } catch (Exception e) {
            LOGGER.error("Demo failed: {}", e.getMessage(), e);
        }
    }
}
