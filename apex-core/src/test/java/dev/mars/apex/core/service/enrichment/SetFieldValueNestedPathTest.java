package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for setFieldValue() behavior with nested paths.
 * 
 * Tests the internal setFieldValue() method through the public enrichment API.
 * Documents current behavior and known limitations for nested target-field paths.
 * 
 * SUMMARY OF CURRENT BEHAVIOR:
 * 1. Simple keys: Works (map.put(key, value))
 * 2. Dot-paths without SpEL: Treated as literal keys (LIMITATION)
 * 3. SpEL prefix with existing structure: Works
 * 4. SpEL prefix with missing structure: Fails silently (LIMITATION)
 * 5. Array indices with existing list: Works
 * 6. Array indices with missing list: Fails silently (LIMITATION)
 */
@DisplayName("SetFieldValue Nested Path Tests")
public class SetFieldValueNestedPathTest {

    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    private String createEnrichmentYaml(String targetField) {
        return """
            metadata:
              id: "test"
              name: "Test"
              version: "1.0.0"
            
            enrichments:
              - id: "test-enrichment"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "inputValue"
                    target-field: "%s"
            """.formatted(targetField);
    }

    @Nested
    @DisplayName("Simple Key Behavior")
    class SimpleKeyTests {

        @Test
        @DisplayName("Simple key sets value directly in map")
        void testSimpleKey() throws Exception {
            String yaml = createEnrichmentYaml("outputValue");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            
            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "TEST");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            assertEquals("TEST", result.get("outputValue"));
        }
    }

    @Nested
    @DisplayName("Dot-Path Without SpEL Prefix")
    class DotPathWithoutSpelTests {

        @Test
        @DisplayName("Single dot-path becomes literal key - KNOWN LIMITATION")
        void testSingleDotPath() throws Exception {
            String yaml = createEnrichmentYaml("level1.level2");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            
            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "TEST");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            // LIMITATION: Dot-path is treated as literal key
            assertNull(result.get("level1"), "No nested structure created");
            assertEquals("TEST", result.get("level1.level2"), "Stored as literal key");
        }

        @Test
        @DisplayName("Multi-level dot-path becomes literal key - KNOWN LIMITATION")
        void testMultiLevelDotPath() throws Exception {
            String yaml = createEnrichmentYaml("a.b.c.d");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            
            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "DEEP");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            // LIMITATION: Entire path is literal key
            assertNull(result.get("a"), "No nested structure created");
            assertEquals("DEEP", result.get("a.b.c.d"), "Stored as literal key");
        }
    }

    @Nested
    @DisplayName("SpEL Prefix With Existing Structure")
    class SpelWithExistingStructureTests {

        @Test
        @DisplayName("SpEL sets value in existing nested map")
        void testSpelExistingMap() throws Exception {
            String yaml = createEnrichmentYaml("#nested.field");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            
            Map<String, Object> nested = new HashMap<>();
            nested.put("existing", "value");
            
            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "NEW");
            input.put("nested", nested);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> resultNested = (Map<String, Object>) result.get("nested");
            assertEquals("NEW", resultNested.get("field"), "SpEL sets nested field");
            assertEquals("value", resultNested.get("existing"), "Existing field preserved");
        }

        @Test
        @DisplayName("SpEL sets value in existing list element")
        void testSpelExistingList() throws Exception {
            String yaml = createEnrichmentYaml("#items[0].value");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", "Item0");
            List<Map<String, Object>> items = new ArrayList<>();
            items.add(item);
            
            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", 999);
            input.put("items", items);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultItems = (List<Map<String, Object>>) result.get("items");
            assertEquals(999, resultItems.get(0).get("value"), "SpEL sets list element field");
            assertEquals("Item0", resultItems.get(0).get("name"), "Existing field preserved");
        }
    }

    @Nested
    @DisplayName("SpEL Prefix With Missing Structure - KNOWN LIMITATIONS")
    class SpelWithMissingStructureTests {

        @Test
        @DisplayName("SpEL fails silently when intermediate map is missing")
        void testSpelMissingMap() throws Exception {
            String yaml = createEnrichmentYaml("#missing.field");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);

            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "VALUE");
            // Note: "missing" map is NOT pre-created

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            // LIMITATION: SpEL cannot auto-create missing intermediate structures
            assertNull(result.get("missing"), "SpEL does not auto-create missing map");
            assertFalse(result.containsKey("field"), "Field not set at root level either");
        }

        @Test
        @DisplayName("SpEL fails silently when list is missing")
        void testSpelMissingList() throws Exception {
            String yaml = createEnrichmentYaml("#items[0].value");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);

            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", 100);
            // Note: "items" list is NOT pre-created

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            // LIMITATION: SpEL cannot auto-create missing lists
            assertNull(result.get("items"), "SpEL does not auto-create missing list");
        }

        @Test
        @DisplayName("SpEL fails silently when deep path has missing intermediate")
        void testSpelDeepPathMissingIntermediate() throws Exception {
            String yaml = createEnrichmentYaml("#a.b.c.d");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);

            // Create partial structure - only "a" exists
            Map<String, Object> a = new HashMap<>();
            a.put("other", "value");

            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "DEEP");
            input.put("a", a);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            // LIMITATION: SpEL fails when "b" doesn't exist in "a"
            @SuppressWarnings("unchecked")
            Map<String, Object> resultA = (Map<String, Object>) result.get("a");
            assertNull(resultA.get("b"), "SpEL does not auto-create missing intermediate 'b'");
        }
    }

    @Nested
    @DisplayName("Complex Nested Paths With Full Structure")
    class ComplexNestedPathTests {

        @Test
        @DisplayName("Deep nesting works when full structure exists")
        void testDeepNestingFullStructure() throws Exception {
            String yaml = createEnrichmentYaml("#trade.details.pricing.amount");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);

            // Create full nested structure
            Map<String, Object> pricing = new HashMap<>();
            pricing.put("currency", "USD");

            Map<String, Object> details = new HashMap<>();
            details.put("pricing", pricing);

            Map<String, Object> trade = new HashMap<>();
            trade.put("details", details);

            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", 50000);
            input.put("trade", trade);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> resultTrade = (Map<String, Object>) result.get("trade");
            @SuppressWarnings("unchecked")
            Map<String, Object> resultDetails = (Map<String, Object>) resultTrade.get("details");
            @SuppressWarnings("unchecked")
            Map<String, Object> resultPricing = (Map<String, Object>) resultDetails.get("pricing");

            assertEquals(50000, resultPricing.get("amount"), "Deep nested field set correctly");
            assertEquals("USD", resultPricing.get("currency"), "Existing field preserved");
        }

        @Test
        @DisplayName("Mixed map and list nesting works when structure exists")
        void testMixedMapListNesting() throws Exception {
            String yaml = createEnrichmentYaml("#portfolio.positions[0].trades[0].status");
            YamlRuleConfiguration config = loader.fromYamlString(yaml);

            // Create full nested structure
            Map<String, Object> trade0 = new HashMap<>();
            trade0.put("id", "T001");
            List<Map<String, Object>> trades = new ArrayList<>();
            trades.add(trade0);

            Map<String, Object> position0 = new HashMap<>();
            position0.put("trades", trades);
            List<Map<String, Object>> positions = new ArrayList<>();
            positions.add(position0);

            Map<String, Object> portfolio = new HashMap<>();
            portfolio.put("positions", positions);

            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "SETTLED");
            input.put("portfolio", portfolio);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> result = engine.evaluate(input).getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> resultPortfolio = (Map<String, Object>) result.get("portfolio");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultPositions = (List<Map<String, Object>>) resultPortfolio.get("positions");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultTrades = (List<Map<String, Object>>) resultPositions.get(0).get("trades");

            assertEquals("SETTLED", resultTrades.get(0).get("status"), "Deep mixed path set correctly");
            assertEquals("T001", resultTrades.get(0).get("id"), "Existing field preserved");
        }
    }
}

