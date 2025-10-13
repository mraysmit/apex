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
 * Integration test demonstrating real-world use case for SpEL field mapping.
 * This test shows how the original issue (nested field access in field mappings) is now solved.
 */
public class SpelFieldMappingIntegrationTest {

    private YamlEnrichmentProcessor enrichmentProcessor;
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        enrichmentProcessor = new YamlEnrichmentProcessor(null, expressionEvaluator);
        loader = new YamlConfigurationLoader();
    }

    @Test
    void testOriginalIssueScenario() throws Exception {
        System.out.println("=== Testing Original Issue Scenario (SOLVED!) ===");
        System.out.println("This test demonstrates the exact scenario from the issue document:");
        System.out.println("- Nested data structure: { data: { currency: 'USD', amount: 1000 } }");
        System.out.println("- Condition uses SpEL: #data.currency != null");
        System.out.println("- Field mapping NOW uses SpEL: source-field: '#data.currency'");
        System.out.println();
        
        String yamlConfig = """
            metadata:
              name: "Field Enrichment Demo (Original Issue)"
              version: "1.0.0"
            
            enrichments:
              - id: "field-enrichment-demo"
                name: "field-enrichment-demo"
                description: "field-enrichment-demo"
                enabled: true
                type: "field-enrichment"
                condition: "#data.currency != null"
                field-mappings:
                  # ✅ NOW WORKS! Access nested field with SpEL
                  - source-field: "#data.currency"
                    target-field: "buy_currency"
                  - source-field: "#data.amount"
                    target-field: "trade_amount"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with nested structure (exactly as in the issue)
        Map<String, Object> data = new HashMap<>();
        data.put("currency", "USD");
        data.put("amount", 1000);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("data", data);
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        // Verify the issue is solved
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData instanceof Map, "Enriched data should be a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // ✅ ISSUE SOLVED: Nested fields are now accessible via SpEL in field mappings
        assertEquals("USD", enrichedMap.get("buy_currency"), 
                    "Should extract nested currency field using SpEL");
        assertEquals(1000, enrichedMap.get("trade_amount"), 
                    "Should extract nested amount field using SpEL");
        
        System.out.println();
        System.out.println("✓ Original issue SOLVED!");
        System.out.println("✓ Field mappings now support SpEL expressions for nested field access");
        System.out.println("✓ Consistent with conditions, transformations, and lookup-keys");
    }

    @Test
    void testLookupEnrichmentWithNestedResults() throws Exception {
        System.out.println("=== Testing Lookup Enrichment with Nested Results ===");
        
        String yamlConfig = """
            metadata:
              name: "Lookup with Nested Results"
              version: "1.0.0"
            
            enrichments:
              - id: "instrument-lookup"
                type: "lookup-enrichment"
                condition: "#symbol != null"
                lookup-config:
                  lookup-key: "#symbol"
                  lookup-dataset:
                    type: "inline"
                    key-field: "symbol"
                    data:
                      - symbol: "AAPL"
                        data:
                          instrument:
                            name: "Apple Inc."
                            type: "EQUITY"
                          pricing:
                            bid: 150.25
                            ask: 150.30
                field-mappings:
                  # ✅ Access nested fields in lookup result with SpEL
                  - source-field: "#data.instrument.name"
                    target-field: "instrument_name"
                  - source-field: "#data.instrument.type"
                    target-field: "instrument_type"
                  - source-field: "#data.pricing.bid"
                    target-field: "bid_price"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("symbol", "AAPL");
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify nested lookup results are extracted correctly
        assertEquals("Apple Inc.", enrichedMap.get("instrument_name"), 
                    "Should extract nested instrument name from lookup result");
        assertEquals("EQUITY", enrichedMap.get("instrument_type"), 
                    "Should extract nested instrument type from lookup result");
        assertEquals(150.25, enrichedMap.get("bid_price"), 
                    "Should extract nested bid price from lookup result");
        
        System.out.println("✓ Lookup enrichment with nested results test passed!");
    }

    @Test
    void testConsistencyAcrossAllApexFeatures() throws Exception {
        System.out.println("=== Testing Consistency Across All APEX Features ===");
        System.out.println("Demonstrating that SpEL is now used consistently across:");
        System.out.println("- Conditions");
        System.out.println("- Transformations");
        System.out.println("- Field mappings (NEW!)");
        System.out.println();
        
        String yamlConfig = """
            metadata:
              name: "Consistency Demo"
              version: "1.0.0"
            
            enrichments:
              - id: "consistency-demo"
                type: "field-enrichment"
                # ✅ SpEL in condition
                condition: "#data.trade.status == 'ACTIVE'"
                field-mappings:
                  # ✅ SpEL in source-field (NEW!)
                  - source-field: "#data.trade.counterparty"
                    target-field: "counterparty_name"
                  
                  # ✅ SpEL in source-field + transformation
                  - source-field: "#data.trade.amount"
                    target-field: "adjusted_amount"
                    transformation: "#value * 1.1"  # ✅ SpEL in transformation
                  
                  # ✅ Complex SpEL expression in source-field
                  - source-field: "#data.trade.currency.toUpperCase()"
                    target-field: "currency_code"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create nested input data
        Map<String, Object> trade = new HashMap<>();
        trade.put("status", "ACTIVE");
        trade.put("counterparty", "JP Morgan");
        trade.put("amount", 1000000);
        trade.put("currency", "usd");
        
        Map<String, Object> data = new HashMap<>();
        data.put("trade", trade);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("data", data);
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify all SpEL features work together
        assertEquals("JP Morgan", enrichedMap.get("counterparty_name"), 
                    "SpEL in source-field should work");
        assertEquals(1100000.0, enrichedMap.get("adjusted_amount"), 
                    "SpEL in source-field + transformation should work");
        assertEquals("USD", enrichedMap.get("currency_code"), 
                    "Complex SpEL expression in source-field should work");
        
        System.out.println();
        System.out.println("✓ Consistency test passed!");
        System.out.println("✓ SpEL now works consistently across all APEX features");
    }

    @Test
    void testBackwardCompatibilityPreserved() throws Exception {
        System.out.println("=== Testing Backward Compatibility ===");
        System.out.println("Verifying that existing configurations without # prefix still work");
        System.out.println();
        
        String yamlConfig = """
            metadata:
              name: "Backward Compatibility Test"
              version: "1.0.0"
            
            enrichments:
              - id: "backward-compat"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  # Old style (no # prefix) - should still work
                  - source-field: "currency"
                    target-field: "currency_code"
                  - source-field: "amount"
                    target-field: "trade_amount"
                  
                  # New style (with # prefix) - also works
                  - source-field: "#data.nested_field"
                    target-field: "nested_value"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with both simple and nested fields
        Map<String, Object> data = new HashMap<>();
        data.put("nested_field", "nested_value");
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", "EUR");
        inputData.put("amount", 5000);
        inputData.put("data", data);
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify backward compatibility
        assertEquals("EUR", enrichedMap.get("currency_code"), 
                    "Old style (no #) should still work");
        assertEquals(5000, enrichedMap.get("trade_amount"), 
                    "Old style (no #) should still work");
        assertEquals("nested_value", enrichedMap.get("nested_value"), 
                    "New style (with #) should work");
        
        System.out.println();
        System.out.println("✓ Backward compatibility preserved!");
        System.out.println("✓ Existing configurations continue to work unchanged");
    }
}

