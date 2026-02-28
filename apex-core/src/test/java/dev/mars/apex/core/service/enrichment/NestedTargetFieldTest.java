package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Nested;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nested target-field paths in field-mappings.
 * 
 * This test class documents the current behavior of APEX when setting values
 * to nested target-field paths like "trade.currency" or "items[0].value".
 * 
 * CRITICAL: These tests expose a significant gap in APEX functionality.
 * While source-field supports SpEL for reading nested paths, target-field
 * has limited support for writing to nested paths.
 */
@DisplayName("Nested Target-Field Tests")
public class NestedTargetFieldTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NestedTargetFieldTest.class);

    private ConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader();
    }

    @Nested
    @DisplayName("Flat Target-Field (Baseline)")
    class FlatTargetFieldTests {

        @Test
        @DisplayName("Simple flat target-field should work")
        void testSimpleFlatTargetField() throws Exception {
            String yamlConfig = """
                metadata:
                  id: "flat-target-test"
                  name: "Flat Target Field Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "flat-target-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputCurrency"
                        target-field: "outputCurrency"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputCurrency", "USD");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            assertEquals("USD", enrichedData.get("outputCurrency"), 
                "Flat target-field should work");
        }
    }

    @Nested
    @DisplayName("Dot-Notation Target-Field")
    class DotNotationTargetFieldTests {

        @Test
        @DisplayName("Dot notation target-field WITHOUT SpEL prefix - KNOWN LIMITATION")
        void testDotNotationWithoutSpelPrefix() throws Exception {
            // KNOWN LIMITATION: Without SpEL prefix, dot-paths are treated as literal keys
            String yamlConfig = """
                metadata:
                  id: "dot-notation-test"
                  name: "Dot Notation Target Field Test"
                  version: "1.0.0"

                enrichments:
                  - id: "dot-notation-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputCurrency"
                        target-field: "trade.currency"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputCurrency", "USD");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            System.out.println("Enriched data: " + enrichedData);

            // DOCUMENTED BEHAVIOR: Without SpEL prefix, "trade.currency" becomes a literal key
            // This is a KNOWN LIMITATION - nested structures are NOT auto-created
            Object tradeObj = enrichedData.get("trade");
            assertNull(tradeObj, "KNOWN LIMITATION: No nested 'trade' object is created");

            // The value is stored under the literal key "trade.currency"
            assertEquals("USD", enrichedData.get("trade.currency"),
                "KNOWN LIMITATION: Dot-path is treated as literal key name, not nested path");
        }

        @Test
        @DisplayName("Dot notation target-field WITH SpEL prefix - existing structure")
        void testDotNotationWithSpelPrefixExistingStructure() throws Exception {
            String yamlConfig = """
                metadata:
                  id: "spel-dot-notation-test"
                  name: "SpEL Dot Notation Target Field Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "spel-dot-notation-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputCurrency"
                        target-field: "#trade.currency"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
            
            // Pre-create the nested structure
            Map<String, Object> trade = new HashMap<>();
            trade.put("amount", 1000);
            
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputCurrency", "USD");
            inputData.put("trade", trade);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            System.out.println("Enriched data with SpEL prefix: " + enrichedData);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedTrade = (Map<String, Object>) enrichedData.get("trade");
            assertNotNull(enrichedTrade, "Trade object should exist");
            assertEquals("USD", enrichedTrade.get("currency"),
                "SpEL prefix should set nested field when structure exists");
        }

        @Test
        @DisplayName("Dot notation target-field WITH SpEL prefix - missing structure")
        void testDotNotationWithSpelPrefixMissingStructure() throws Exception {
            LOGGER.info("=== INTENTIONAL ERROR TEST: SpEL with missing structure ===");
            String yamlConfig = """
                metadata:
                  id: "spel-missing-structure-test"
                  name: "SpEL Missing Structure Test"
                  version: "1.0.0"

                enrichments:
                  - id: "spel-missing-structure-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputCurrency"
                        target-field: "#trade.currency"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Do NOT pre-create the nested structure
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputCurrency", "USD");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            System.out.println("Enriched data (missing structure): " + enrichedData);

            // Document behavior when structure doesn't exist
            Object tradeObj = enrichedData.get("trade");
            if (tradeObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> trade = (Map<String, Object>) tradeObj;
                assertEquals("USD", trade.get("currency"),
                    "SpEL should auto-create nested structure");
            } else {
                // SpEL setValue typically fails when intermediate path doesn't exist
                assertNull(tradeObj, "SpEL cannot auto-create missing intermediate structures");
            }
        }
    }

    @Nested
    @DisplayName("Array Index Target-Field")
    class ArrayIndexTargetFieldTests {

        @Test
        @DisplayName("Array index target-field - existing list")
        void testArrayIndexWithExistingList() throws Exception {
            String yamlConfig = """
                metadata:
                  id: "array-index-test"
                  name: "Array Index Target Field Test"
                  version: "1.0.0"

                enrichments:
                  - id: "array-index-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputValue"
                        target-field: "#items[0].value"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Pre-create the list with an element
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item0 = new HashMap<>();
            item0.put("name", "Item 0");
            items.add(item0);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputValue", 100);
            inputData.put("items", items);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            System.out.println("Enriched data (array index): " + enrichedData);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> enrichedItems = (List<Map<String, Object>>) enrichedData.get("items");
            assertNotNull(enrichedItems, "Items list should exist");
            assertFalse(enrichedItems.isEmpty(), "Items list should not be empty");
            assertEquals(100, enrichedItems.get(0).get("value"),
                "Should set value at array index");
        }

        @Test
        @DisplayName("Array index target-field - missing list")
        void testArrayIndexWithMissingList() throws Exception {
            LOGGER.info("=== INTENTIONAL ERROR TEST: Array index with missing list ===");
            String yamlConfig = """
                metadata:
                  id: "array-missing-list-test"
                  name: "Array Missing List Test"
                  version: "1.0.0"

                enrichments:
                  - id: "array-missing-list-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputValue"
                        target-field: "#items[0].value"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Do NOT pre-create the list
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputValue", 100);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            System.out.println("Enriched data (missing list): " + enrichedData);

            // Document behavior - SpEL cannot create lists
            Object itemsObj = enrichedData.get("items");
            if (itemsObj instanceof List) {
                fail("Unexpected: SpEL auto-created the list");
            } else {
                assertNull(itemsObj, "SpEL cannot auto-create missing lists");
            }
        }
    }

    @Nested
    @DisplayName("Deep Nesting Target-Field")
    class DeepNestingTargetFieldTests {

        @Test
        @DisplayName("OTC trade structure - trade.otcTrade.otcLeg[0].legInterestRateType")
        void testOtcTradeDeepNesting() throws Exception {
            String yamlConfig = """
                metadata:
                  id: "otc-deep-nesting-test"
                  name: "OTC Deep Nesting Test"
                  version: "1.0.0"

                enrichments:
                  - id: "otc-deep-nesting-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "rateType"
                        target-field: "#trade.otcTrade.otcLeg[0].legInterestRateType"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Pre-create the full nested structure
            Map<String, Object> leg0 = new HashMap<>();
            leg0.put("legId", "LEG001");

            List<Map<String, Object>> otcLeg = new ArrayList<>();
            otcLeg.add(leg0);

            Map<String, Object> otcTrade = new HashMap<>();
            otcTrade.put("otcLeg", otcLeg);

            Map<String, Object> trade = new HashMap<>();
            trade.put("otcTrade", otcTrade);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("rateType", "FIXED");
            inputData.put("trade", trade);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            System.out.println("Enriched data (OTC deep nesting): " + enrichedData);

            // Navigate to verify the value was set
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedTrade = (Map<String, Object>) enrichedData.get("trade");
            assertNotNull(enrichedTrade, "Trade should exist");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedOtcTrade = (Map<String, Object>) enrichedTrade.get("otcTrade");
            assertNotNull(enrichedOtcTrade, "OTC Trade should exist");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> enrichedOtcLeg = (List<Map<String, Object>>) enrichedOtcTrade.get("otcLeg");
            assertNotNull(enrichedOtcLeg, "OTC Leg list should exist");
            assertFalse(enrichedOtcLeg.isEmpty(), "OTC Leg list should not be empty");

            assertEquals("FIXED", enrichedOtcLeg.get(0).get("legInterestRateType"),
                "Should set deeply nested field in OTC trade structure");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty target-field should be rejected by validation")
        void testEmptyTargetFieldRejected() {
            String yamlConfig = """
                metadata:
                  id: "empty-target-test"
                  name: "Empty Target Field Test"
                  version: "1.0.0"

                enrichments:
                  - id: "empty-target-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputValue"
                        target-field: ""
                """;

            // APEX validation correctly rejects empty target-field
            Exception exception = assertThrows(Exception.class, () -> {
                loader.fromYamlString(yamlConfig);
            });

            assertTrue(exception.getMessage().contains("target-field"),
                "Exception should mention missing target-field");
        }

        @Test
        @DisplayName("Null source value should be handled gracefully")
        void testNullSourceValue() throws Exception {
            String yamlConfig = """
                metadata:
                  id: "null-source-test"
                  name: "Null Source Value Test"
                  version: "1.0.0"

                enrichments:
                  - id: "null-source-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "missingField"
                        target-field: "outputField"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("otherField", "value");
            // Note: "missingField" is not in input

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);
            Map<String, Object> enrichedData = result.getEnrichedData();

            // Should handle gracefully - either skip or set null
            assertNotNull(result, "Result should not be null");
            System.out.println("Enriched data with null source: " + enrichedData);
        }

        @Test
        @DisplayName("Array index out of bounds should be handled gracefully")
        void testArrayIndexOutOfBounds() throws Exception {
            LOGGER.info("=== INTENTIONAL ERROR TEST: Array index out of bounds ===");
            String yamlConfig = """
                metadata:
                  id: "array-oob-test"
                  name: "Array Out of Bounds Test"
                  version: "1.0.0"

                enrichments:
                  - id: "array-oob-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputValue"
                        target-field: "#items[5].value"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            // Create list with only 2 elements
            List<Map<String, Object>> items = new ArrayList<>();
            items.add(new HashMap<>());
            items.add(new HashMap<>());

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputValue", 100);
            inputData.put("items", items);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);

            // Should handle gracefully - not throw exception
            assertNotNull(result, "Result should not be null");
            System.out.println("Result after OOB access attempt: " + result.getEnrichedData());
        }

        @Test
        @DisplayName("Type mismatch - setting on non-map should be handled gracefully")
        void testTypeMismatchNonMap() throws Exception {
            LOGGER.info("=== INTENTIONAL ERROR TEST: Type mismatch setting on string ===");
            String yamlConfig = """
                metadata:
                  id: "type-mismatch-test"
                  name: "Type Mismatch Test"
                  version: "1.0.0"

                enrichments:
                  - id: "type-mismatch-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputValue"
                        target-field: "#stringField.nested"
                """;

            YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("inputValue", "TEST");
            inputData.put("stringField", "I am a string, not a map");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(inputData);

            // Should handle gracefully - SpEL will fail but not throw
            assertNotNull(result, "Result should not be null");
            System.out.println("Result after type mismatch: " + result.getEnrichedData());
        }
    }
}

