package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test SpEL expression support in field mappings using RulesEngine.
 * Tests the enhancement that allows source-field and target-field to use SpEL expressions
 * prefixed with # for nested field access and complex expressions.
 */
public class SpelFieldMappingTest {

    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    void testSpelNestedFieldAccess() throws Exception {
        System.out.println("=== Testing SpEL Nested Field Access ===");

        String yamlConfig = """
            metadata:
              id: "spel-nested-field-test"
              name: "SpEL Nested Field Test"
              version: "1.0.0"
              description: "Test SpEL nested field access"
              type: "rule-config"

            enrichments:
              - id: "nested-field-enrichment"
                type: "field-enrichment"
                condition: "#currency != null"
                field-mappings:
                  - source-field: "#currency"
                    target-field: "buy_currency"
                  - source-field: "#amount"
                    target-field: "trade_amount"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data - pass directly as root context
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", "USD");
        inputData.put("amount", 1000);

        System.out.println("Input data: " + inputData);

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

        // Verify enriched data
        assertNotNull(enrichedMap, "Enriched data should not be null");

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
              id: "spel-multi-level-test"
              name: "SpEL Multi-Level Nesting Test"
              version: "1.0.0"
              description: "Test SpEL multi-level nesting"
              type: "rule-config"

            enrichments:
              - id: "multi-level-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#trade.counterparty"
                    target-field: "counterparty_name"
                  - source-field: "#trade.amount"
                    target-field: "trade_amount"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data with multi-level nesting
        Map<String, Object> trade = new HashMap<>();
        trade.put("counterparty", "Goldman Sachs");
        trade.put("amount", 5000000);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("trade", trade);

        System.out.println("Input data: " + inputData);

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

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
              id: "spel-safe-navigation-test"
              name: "SpEL Safe Navigation Test"
              version: "1.0.0"
              description: "Test SpEL safe navigation"
              type: "rule-config"

            enrichments:
              - id: "safe-navigation-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#?currency"
                    target-field: "currency_code"
                  - source-field: "#?trade?.amount"
                    target-field: "trade_amount"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data with missing fields (empty map)
        Map<String, Object> inputData = new HashMap<>();
        // Note: currency and trade are NOT present

        System.out.println("Input data: " + inputData);

        // Process enrichments using RulesEngine - should not throw exception
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

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
              id: "spel-array-indexing-test"
              name: "SpEL Array Indexing Test"
              version: "1.0.0"
              description: "Test SpEL array indexing"
              type: "rule-config"

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

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

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
              id: "backward-compatibility-test"
              name: "Backward Compatibility Test"
              version: "1.0.0"
              description: "Test backward compatibility with simple fields"
              type: "rule-config"

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

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

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
              id: "mixed-fields-test"
              name: "Mixed Fields Test"
              version: "1.0.0"
              description: "Test mixed simple and SpEL fields"
              type: "rule-config"

            enrichments:
              - id: "mixed-field-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "status"
                    target-field: "trade_status"
                  - source-field: "#currency"
                    target-field: "buy_currency"
                  - source-field: "type"
                    target-field: "trade_type"
                  - source-field: "#amount"
                    target-field: "trade_amount"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data with both simple and nested fields
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("status", "ACTIVE");
        inputData.put("type", "SPOT");
        inputData.put("currency", "GBP");
        inputData.put("amount", 7500);

        System.out.println("Input data: " + inputData);

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

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
              id: "spel-complex-expression-test"
              name: "SpEL Complex Expression Test"
              version: "1.0.0"
              description: "Test SpEL complex expression"
              type: "rule-config"

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

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

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
              id: "spel-method-call-test"
              name: "SpEL Method Call Test"
              version: "1.0.0"
              description: "Test SpEL method call"
              type: "rule-config"

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

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

        // Verify method call works
        assertEquals("USD", enrichedMap.get("currency_code"),
                    "Method call should convert to uppercase");

        System.out.println("✓ SpEL method call test passed!");
    }

    @Test
    void testSpelWithTransformation() throws Exception {
        System.out.println("=== Testing SpEL with Expression ===");

        String yamlConfig = """
            metadata:
              id: "spel-with-expression-test"
              name: "SpEL with Expression Test"
              version: "1.0.0"
              description: "Test SpEL with expression"
              type: "rule-config"

            enrichments:
              - id: "spel-expression-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#amount"
                    target-field: "adjusted_amount"
                    expression: "#value * 1.1"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("amount", 1000);

        System.out.println("Input data: " + inputData);

        // Process enrichments using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

        // Verify SpEL source-field combined with expression
        assertEquals(1100.0, enrichedMap.get("adjusted_amount"),
                    "Should extract nested field and apply expression");

        System.out.println("✓ SpEL with expression test passed!");
    }

    @Test
    void testSpelInvalidExpression() throws Exception {
        System.out.println("=== Testing SpEL Invalid Expression (Error Handling) ===");

        String yamlConfig = """
            metadata:
              id: "spel-invalid-expression-test"
              name: "SpEL Invalid Expression Test"
              version: "1.0.0"
              description: "Test SpEL invalid expression error handling"
              type: "rule-config"

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

        // Process enrichments using RulesEngine - should not throw exception, should handle gracefully
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

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
              id: "spel-null-handling-test"
              name: "SpEL Null Handling Test"
              version: "1.0.0"
              description: "Test SpEL null handling"
              type: "rule-config"

            enrichments:
              - id: "null-handling-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#currency"
                    target-field: "currency_code"
                  - source-field: "#amount"
                    target-field: "trade_amount"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Create input data with null fields
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", null);
        inputData.put("amount", null);

        System.out.println("Input data: " + inputData);

        // Process enrichments using RulesEngine - should handle null gracefully
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);
        Map<String, Object> enrichedMap = result.getEnrichedData();

        System.out.println("Enriched data: " + enrichedMap);

        // Verify null handling
        assertNull(enrichedMap.get("currency_code"), "Should handle null gracefully");
        assertNull(enrichedMap.get("trade_amount"), "Should handle null gracefully");

        System.out.println("✓ SpEL null handling test passed!");
    }
}

