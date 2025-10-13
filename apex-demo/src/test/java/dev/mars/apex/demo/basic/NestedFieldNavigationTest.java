/*
 * Copyright 2024 APEX Demo Team
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

package dev.mars.apex.demo.basic;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.enrichment.YamlEnrichmentProcessor;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates SpEL field navigation in field mappings (NEW in v2.3).
 *
 * Shows how to access nested fields using the # prefix:
 * - Before: Could only access top-level fields
 * - After: Can access nested fields like #trade.currency
 */
@ExtendWith(ColoredTestOutputExtension.class)
class NestedFieldNavigationTest {

    private YamlEnrichmentProcessor enrichmentProcessor;
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        enrichmentProcessor = new YamlEnrichmentProcessor(null, expressionEvaluator);
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Access nested fields with SpEL field mappings")
    void testNestedFieldNavigation() throws Exception {
        System.out.println("=== Testing Nested Field Navigation ===");

        // Use inline YAML exactly like the working test
        String yamlConfig = """
            metadata:
              name: "Nested Field Navigation Demo"
              version: "1.0.0"

            enrichments:
              - id: "extract-nested-fields"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#trade.currency"
                    target-field: "trade_currency"
                  - source-field: "#trade.amount"
                    target-field: "trade_amount"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Test data with nested structure
        Map<String, Object> trade = new HashMap<>();
        trade.put("currency", "USD");
        trade.put("amount", 1000.0);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("trade", trade);

        System.out.println("Input data: " + inputData);

        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);

        System.out.println("Enriched data: " + enrichedData);

        // Verify enriched data
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData instanceof Map, "Enriched data should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;

        // Verify nested fields were extracted
        assertEquals("USD", enrichedMap.get("trade_currency"),
            "Should extract nested currency field");
        assertEquals(1000.0, enrichedMap.get("trade_amount"),
            "Should extract nested amount field");

        System.out.println("\n✅ Successfully extracted nested fields:");
        System.out.println("   trade.currency → trade_currency: " + enrichedMap.get("trade_currency"));
        System.out.println("   trade.amount → trade_amount: " + enrichedMap.get("trade_amount"));
    }

    @Test
    @DisplayName("Access deeply nested fields with multiple levels")
    void testDeeplyNestedFieldNavigation() throws Exception {
        System.out.println("\n=== Testing Deeply Nested Field Navigation ===");

        // Use inline YAML with deeply nested field access
        String yamlConfig = """
            metadata:
              name: "Deeply Nested Field Navigation Demo"
              version: "1.0.0"

            enrichments:
              - id: "extract-deeply-nested-fields"
                type: "field-enrichment"
                condition: "true"
                field-mappings:
                  - source-field: "#transaction.trade.counterparty.name"
                    target-field: "counterparty_name"
                  - source-field: "#transaction.trade.counterparty.country"
                    target-field: "counterparty_country"
                  - source-field: "#transaction.trade.details.currency"
                    target-field: "trade_currency"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Test data with multiple levels of nesting
        Map<String, Object> counterparty = new HashMap<>();
        counterparty.put("name", "Goldman Sachs");
        counterparty.put("country", "USA");

        Map<String, Object> details = new HashMap<>();
        details.put("currency", "EUR");
        details.put("notional", 5000000.0);

        Map<String, Object> trade = new HashMap<>();
        trade.put("counterparty", counterparty);
        trade.put("details", details);

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("trade", trade);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("transaction", transaction);

        System.out.println("Input data: " + inputData);

        // Process enrichments
        Object enrichedData = enrichmentProcessor.processEnrichments(config.getEnrichments(), inputData);

        System.out.println("Enriched data: " + enrichedData);

        // Verify enriched data
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData instanceof Map, "Enriched data should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedMap = (Map<String, Object>) enrichedData;

        // Verify deeply nested fields were extracted
        assertEquals("Goldman Sachs", enrichedMap.get("counterparty_name"),
            "Should extract transaction.trade.counterparty.name");
        assertEquals("USA", enrichedMap.get("counterparty_country"),
            "Should extract transaction.trade.counterparty.country");
        assertEquals("EUR", enrichedMap.get("trade_currency"),
            "Should extract transaction.trade.details.currency");

        System.out.println("\n✅ Successfully extracted deeply nested fields:");
        System.out.println("   transaction.trade.counterparty.name → counterparty_name: " + enrichedMap.get("counterparty_name"));
        System.out.println("   transaction.trade.counterparty.country → counterparty_country: " + enrichedMap.get("counterparty_country"));
        System.out.println("   transaction.trade.details.currency → trade_currency: " + enrichedMap.get("trade_currency"));
    }
}

