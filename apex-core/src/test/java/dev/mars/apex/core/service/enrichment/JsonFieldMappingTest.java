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
 * Test JSON field mapping functionality to understand how APEX handles JSON data.
 */
public class JsonFieldMappingTest {

    private YamlEnrichmentProcessor enrichmentProcessor;

    @BeforeEach
    void setUp() {
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        enrichmentProcessor = new YamlEnrichmentProcessor(null, expressionEvaluator);
    }

    @Test
    void testJsonFieldMappingWithComplexData() throws Exception {
        System.out.println("=== Testing JSON Field Mapping with Complex Data ===");
        
        // Create YAML configuration that simulates REST API response structure
        String yamlConfig = """
            metadata:
              name: "JSON Field Mapping Test"
              version: "1.0.0"
            
            enrichments:
              - id: "json-lookup"
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
                        metadata:
                          source: "market-data-api"
                          timestamp: "2025-09-21T05:30:00Z"
                          quality: "high"
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
        
        System.out.println("Enriched data keys: " + enrichedMap.keySet());
        
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
        
        System.out.println("✓ JSON field mapping test passed!");
    }

    @Test
    void testDirectMapFieldMapping() throws Exception {
        System.out.println("=== Testing Direct Map Field Mapping ===");
        
        // Create a source map that simulates a REST API JSON response
        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("symbol", "EURUSD");
        sourceMap.put("name", "Euro/US Dollar");
        sourceMap.put("bid", 1.0850);
        sourceMap.put("ask", 1.0852);
        sourceMap.put("volume", 1250000);
        sourceMap.put("change_percent", 0.14);
        
        // Create target map
        Map<String, Object> targetMap = new HashMap<>();
        targetMap.put("symbol", "EURUSD");
        targetMap.put("lookupType", "market_data");
        
        System.out.println("Source map: " + sourceMap);
        System.out.println("Target map before: " + targetMap);
        
        // Create field mappings
        String yamlConfig = """
            metadata:
              name: "Direct Map Test"
              version: "1.0.0"
            
            enrichments:
              - id: "direct-mapping"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "symbol"
                    target-field: "marketSymbol"
                    default-value: "UNKNOWN"
                  - source-field: "name"
                    target-field: "marketName"
                    default-value: "Unknown Currency"
                  - source-field: "bid"
                    target-field: "bidPrice"
                    default-value: 0.0
                  - source-field: "ask"
                    target-field: "askPrice"
                    default-value: 0.0
            """;
        
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), targetMap);
        
        System.out.println("Target map after: " + enrichedData);
        
        // Verify the field enrichment worked
        assertNotNull(enrichedData);
        assertTrue(enrichedData instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Check if default values were applied (since we're not doing a lookup)
        assertEquals("UNKNOWN", enrichedMap.get("marketSymbol"));
        assertEquals("Unknown Currency", enrichedMap.get("marketName"));
        assertEquals(0.0, enrichedMap.get("bidPrice"));
        assertEquals(0.0, enrichedMap.get("askPrice"));
        
        System.out.println("✓ Direct map field mapping test passed!");
    }
}
