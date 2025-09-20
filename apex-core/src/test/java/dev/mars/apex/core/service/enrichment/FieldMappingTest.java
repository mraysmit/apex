package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test field mapping functionality in enrichment processor.
 */
public class FieldMappingTest {

    private YamlEnrichmentProcessor enrichmentProcessor;

    @BeforeEach
    void setUp() {
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        enrichmentProcessor = new YamlEnrichmentProcessor(null, expressionEvaluator);
    }

    @Test
    void testSimpleFieldMapping() throws Exception {
        System.out.println("=== Testing Simple Field Mapping ===");
        
        // Create YAML configuration with inline data and field mappings
        String yamlConfig = """
            metadata:
              name: "Field Mapping Test"
              version: "1.0.0"
            
            enrichments:
              - id: "test-lookup"
                type: "lookup-enrichment"
                condition: "#symbol != null"
                lookup-config:
                  lookup-key: "#symbol"
                  lookup-dataset:
                    type: "inline"
                    key-field: "symbol"
                    data:
                      - symbol: "EURUSD"
                        name: "Euro/US Dollar"
                        bid: 1.0850
                        ask: 1.0852
                        volume: 1250000
                        change_percent: 0.14
                field-mappings:
                  - source-field: "symbol"
                    target-field: "marketSymbol"
                  - source-field: "name"
                    target-field: "marketName"
                  - source-field: "bid"
                    target-field: "bidPrice"
                  - source-field: "ask"
                    target-field: "askPrice"
                  - source-field: "volume"
                    target-field: "tradingVolume"
                  - source-field: "change_percent"
                    target-field: "changePercent"
            """;
        
        // Load configuration
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("symbol", "EURUSD");
        inputData.put("lookupType", "market_data");
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        // Verify enriched data
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData instanceof Map, "Enriched data should be a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify original fields are preserved
        assertEquals("EURUSD", enrichedMap.get("symbol"));
        assertEquals("market_data", enrichedMap.get("lookupType"));
        
        // Verify field mappings were applied
        assertEquals("EURUSD", enrichedMap.get("marketSymbol"));
        assertEquals("Euro/US Dollar", enrichedMap.get("marketName"));
        assertEquals(1.0850, enrichedMap.get("bidPrice"));
        assertEquals(1.0852, enrichedMap.get("askPrice"));
        assertEquals(1250000, enrichedMap.get("tradingVolume"));
        assertEquals(0.14, enrichedMap.get("changePercent"));
        
        System.out.println("✓ Field mapping test passed!");
    }
}
