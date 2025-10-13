package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test SpEL expression support in field mappings.
 * Tests the enhancement that allows source-field and target-field to use SpEL expressions
 * prefixed with # for nested field access and complex expressions.
 */
public class SpelFieldMappingTest {

    private YamlEnrichmentProcessor enrichmentProcessor;
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        enrichmentProcessor = new YamlEnrichmentProcessor(null, expressionEvaluator);
        loader = new YamlConfigurationLoader();
    }

    @Test
    void testSpelNestedFieldAccess() throws Exception {
        System.out.println("=== Testing SpEL Nested Field Access ===");
        
        String yamlConfig = """
            metadata:
              name: "SpEL Nested Field Test"
              version: "1.0.0"
            
            enrichments:
              - id: "nested-field-enrichment"
                type: "field-enrichment"
                condition: "#data != null && #data['currency'] != null"
                field-mappings:
                  - source-field: "#data.currency"
                    target-field: "buy_currency"
                  - source-field: "#data.amount"
                    target-field: "trade_amount"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with nested structure
        Map<String, Object> data = new HashMap<>();
        data.put("currency", "USD");
        data.put("amount", 1000);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("data", data);
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        // Verify enriched data
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData instanceof Map, "Enriched data should be a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify SpEL expressions extracted nested values
        assertEquals("USD", enrichedMap.get("buy_currency"), "Should extract nested currency field");
        assertEquals(1000, enrichedMap.get("trade_amount"), "Should extract nested amount field");
        
        System.out.println("✓ SpEL nested field access test passed!");
    }

    @Test
    void testSpelMultiLevelNesting() throws Exception {
        System.out.println("=== Testing SpEL Multi-Level Nesting ===");
        
        String yamlConfig = """
            metadata:
              name: "SpEL Multi-Level Nesting Test"
              version: "1.0.0"
            
            enrichments:
              - id: "multi-level-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#data.trade.counterparty"
                    target-field: "counterparty_name"
                  - source-field: "#data.trade.amount"
                    target-field: "trade_amount"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with multi-level nesting
        Map<String, Object> trade = new HashMap<>();
        trade.put("counterparty", "Goldman Sachs");
        trade.put("amount", 5000000);
        
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
        
        // Verify multi-level nested access
        assertEquals("Goldman Sachs", enrichedMap.get("counterparty_name"), 
                    "Should extract multi-level nested counterparty");
        assertEquals(5000000, enrichedMap.get("trade_amount"), 
                    "Should extract multi-level nested amount");
        
        System.out.println("✓ SpEL multi-level nesting test passed!");
    }

    @Test
    void testSpelSafeNavigation() throws Exception {
        System.out.println("=== Testing SpEL Safe Navigation ===");
        
        String yamlConfig = """
            metadata:
              name: "SpEL Safe Navigation Test"
              version: "1.0.0"
            
            enrichments:
              - id: "safe-navigation-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#data?.currency"
                    target-field: "currency_code"
                  - source-field: "#data?.trade?.amount"
                    target-field: "trade_amount"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with missing nested fields
        Map<String, Object> data = new HashMap<>();
        // Note: currency and trade are NOT present
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("data", data);
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments - should not throw exception
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify safe navigation returns null without error
        assertNull(enrichedMap.get("currency_code"), "Safe navigation should return null for missing field");
        assertNull(enrichedMap.get("trade_amount"), "Safe navigation should return null for missing nested field");
        
        System.out.println("✓ SpEL safe navigation test passed!");
    }

    @Test
    void testSpelArrayIndexing() throws Exception {
        System.out.println("=== Testing SpEL Array Indexing ===");
        
        String yamlConfig = """
            metadata:
              name: "SpEL Array Indexing Test"
              version: "1.0.0"
            
            enrichments:
              - id: "array-indexing-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#items[0].price"
                    target-field: "first_item_price"
                  - source-field: "#items[1].price"
                    target-field: "second_item_price"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with array
        Map<String, Object> item1 = new HashMap<>();
        item1.put("price", 100);
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("price", 200);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("items", List.of(item1, item2));
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify array indexing works
        assertEquals(100, enrichedMap.get("first_item_price"), "Should extract first item price");
        assertEquals(200, enrichedMap.get("second_item_price"), "Should extract second item price");
        
        System.out.println("✓ SpEL array indexing test passed!");
    }

    @Test
    void testBackwardCompatibilitySimpleFields() throws Exception {
        System.out.println("=== Testing Backward Compatibility with Simple Fields ===");
        
        String yamlConfig = """
            metadata:
              name: "Backward Compatibility Test"
              version: "1.0.0"
            
            enrichments:
              - id: "simple-field-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "currency"
                    target-field: "currency_code"
                  - source-field: "amount"
                    target-field: "trade_amount"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with simple fields (no nesting)
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", "EUR");
        inputData.put("amount", 2500);
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify simple field names still work (backward compatible)
        assertEquals("EUR", enrichedMap.get("currency_code"), "Simple field names should still work");
        assertEquals(2500, enrichedMap.get("trade_amount"), "Simple field names should still work");
        
        System.out.println("✓ Backward compatibility test passed!");
    }

    @Test
    void testMixedSimpleAndSpelFields() throws Exception {
        System.out.println("=== Testing Mixed Simple and SpEL Fields ===");
        
        String yamlConfig = """
            metadata:
              name: "Mixed Fields Test"
              version: "1.0.0"
            
            enrichments:
              - id: "mixed-field-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "status"
                    target-field: "trade_status"
                  - source-field: "#data.currency"
                    target-field: "buy_currency"
                  - source-field: "type"
                    target-field: "trade_type"
                  - source-field: "#data.amount"
                    target-field: "trade_amount"
            """;
        
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        
        // Create input data with both simple and nested fields
        Map<String, Object> data = new HashMap<>();
        data.put("currency", "GBP");
        data.put("amount", 7500);
        
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("status", "ACTIVE");
        inputData.put("type", "SPOT");
        inputData.put("data", data);
        
        System.out.println("Input data: " + inputData);
        
        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);
        
        System.out.println("Enriched data: " + enrichedData);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;
        
        // Verify both simple and SpEL fields work together
        assertEquals("ACTIVE", enrichedMap.get("trade_status"), "Simple field should work");
        assertEquals("SPOT", enrichedMap.get("trade_type"), "Simple field should work");
        assertEquals("GBP", enrichedMap.get("buy_currency"), "SpEL field should work");
        assertEquals(7500, enrichedMap.get("trade_amount"), "SpEL field should work");
        
        System.out.println("✓ Mixed simple and SpEL fields test passed!");
    }

    @Test
    void testSpelComplexExpression() throws Exception {
        System.out.println("=== Testing SpEL Complex Expression ===");

        String yamlConfig = """
            metadata:
              name: "SpEL Complex Expression Test"
              version: "1.0.0"

            enrichments:
              - id: "complex-expression-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#status == 'ACTIVE' ? #activePrice : #inactivePrice"
                    target-field: "current_price"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("status", "ACTIVE");
        inputData.put("activePrice", 100);
        inputData.put("inactivePrice", 50);

        System.out.println("Input data: " + inputData);

        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);

        System.out.println("Enriched data: " + enrichedData);

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;

        // Verify complex expression evaluation
        assertEquals(100, enrichedMap.get("current_price"),
                    "Complex expression should evaluate to activePrice");

        System.out.println("✓ SpEL complex expression test passed!");
    }

    @Test
    void testSpelMethodCall() throws Exception {
        System.out.println("=== Testing SpEL Method Call ===");

        String yamlConfig = """
            metadata:
              name: "SpEL Method Call Test"
              version: "1.0.0"

            enrichments:
              - id: "method-call-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#currency.toUpperCase()"
                    target-field: "currency_code"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", "usd");

        System.out.println("Input data: " + inputData);

        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);

        System.out.println("Enriched data: " + enrichedData);

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;

        // Verify method call works
        assertEquals("USD", enrichedMap.get("currency_code"),
                    "Method call should convert to uppercase");

        System.out.println("✓ SpEL method call test passed!");
    }

    @Test
    void testSpelWithTransformation() throws Exception {
        System.out.println("=== Testing SpEL with Transformation ===");

        String yamlConfig = """
            metadata:
              name: "SpEL with Transformation Test"
              version: "1.0.0"

            enrichments:
              - id: "spel-transformation-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#data.amount"
                    target-field: "adjusted_amount"
                    transformation: "#value * 1.1"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1000);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("data", data);

        System.out.println("Input data: " + inputData);

        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);

        System.out.println("Enriched data: " + enrichedData);

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;

        // Verify SpEL source-field combined with transformation
        assertEquals(1100.0, enrichedMap.get("adjusted_amount"),
                    "Should extract nested field and apply transformation");

        System.out.println("✓ SpEL with transformation test passed!");
    }

    @Test
    void testSpelInvalidExpression() throws Exception {
        System.out.println("=== Testing SpEL Invalid Expression (Error Handling) ===");

        String yamlConfig = """
            metadata:
              name: "SpEL Invalid Expression Test"
              version: "1.0.0"

            enrichments:
              - id: "invalid-expression-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#invalid..syntax"
                    target-field: "result"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", "USD");

        System.out.println("Input data: " + inputData);

        // Process enrichments - should not throw exception, should handle gracefully
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);

        System.out.println("Enriched data: " + enrichedData);

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;

        // Verify invalid expression returns null (graceful error handling)
        assertNull(enrichedMap.get("result"),
                  "Invalid SpEL expression should return null and log warning");

        System.out.println("✓ SpEL invalid expression error handling test passed!");
    }

    @Test
    void testSpelNullHandling() throws Exception {
        System.out.println("=== Testing SpEL Null Handling ===");

        String yamlConfig = """
            metadata:
              name: "SpEL Null Handling Test"
              version: "1.0.0"

            enrichments:
              - id: "null-handling-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#data.currency"
                    target-field: "currency_code"
                  - source-field: "#data.amount"
                    target-field: "trade_amount"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data with null nested object
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("data", null);

        System.out.println("Input data: " + inputData);

        // Process enrichments - should handle null gracefully
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);

        System.out.println("Enriched data: " + enrichedData);

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;

        // Verify null handling
        assertNull(enrichedMap.get("currency_code"), "Should handle null gracefully");
        assertNull(enrichedMap.get("trade_amount"), "Should handle null gracefully");

        System.out.println("✓ SpEL null handling test passed!");
    }
}

