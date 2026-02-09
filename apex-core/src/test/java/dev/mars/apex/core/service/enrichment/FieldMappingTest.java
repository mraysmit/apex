package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test field mapping functionality using RulesEngine.
 */
public class FieldMappingTest {

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

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

        // Verify enriched data
        assertNotNull(enrichedMap, "Enriched data should not be null");
        
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
        
        System.out.println("[OK] Field mapping test passed!");
    }
}
