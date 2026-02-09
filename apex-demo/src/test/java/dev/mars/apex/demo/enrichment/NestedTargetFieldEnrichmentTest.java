package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Integration tests for nested target-field paths in APEX enrichments.
 *
 * This test class demonstrates real-world scenarios where enrichment results
 * need to be written to nested structures like OTC trade legs, portfolio positions,
 * and complex financial instrument hierarchies.
 *
 * KNOWN LIMITATIONS DOCUMENTED:
 * 1. Dot-paths without SpEL prefix are treated as literal keys
 * 2. SpEL prefix requires pre-existing structure - cannot auto-create
 * 3. Array indices require pre-existing list with sufficient elements
 *
 * WORKAROUND PATTERNS:
 * 1. Pre-create nested structures before enrichment
 * 2. Use flat fields and post-process into nested structure
 * 3. Use multiple enrichment passes
 *
 * YAML Configuration Files:
 * - NestedTargetFieldEnrichmentTest.yaml - OTC leg enrichment
 * - NestedTargetFieldEnrichmentTest-multi-leg.yaml - Multiple leg enrichment
 * - NestedTargetFieldEnrichmentTest-position-pricing.yaml - Position pricing
 * - NestedTargetFieldEnrichmentTest-limitations.yaml - Literal key limitation
 * - NestedTargetFieldEnrichmentTest-missing-structure.yaml - Missing structure limitation
 * - NestedTargetFieldEnrichmentTest-workaround.yaml - Pre-create structure workaround
 * - NestedTargetFieldEnrichmentTest-literal-to-nested.yaml - Literal constant to nested field
 */
@DisplayName("Nested Target-Field Integration Tests")
class NestedTargetFieldEnrichmentTest extends DemoTestBase {

    private static final String TEST_YAML_PATH = "src/test/java/dev/mars/apex/demo/enrichment/";

    @Nested
    @DisplayName("OTC Trade Leg Enrichment Scenarios")
    class OtcTradeLegTests {

        @Test
        @DisplayName("Enrich OTC leg interest rate type with pre-existing structure")
        void testOtcLegEnrichmentWithExistingStructure() throws Exception {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                TEST_YAML_PATH + "NestedTargetFieldEnrichmentTest.yaml");

            // Pre-create the full OTC trade structure
            Map<String, Object> leg0 = new HashMap<>();
            leg0.put("legId", "LEG001");
            leg0.put("notional", 1000000);

            List<Map<String, Object>> otcLeg = new ArrayList<>();
            otcLeg.add(leg0);

            Map<String, Object> otcTrade = new HashMap<>();
            otcTrade.put("otcLeg", otcLeg);
            otcTrade.put("tradeType", "IRS");

            Map<String, Object> trade = new HashMap<>();
            trade.put("otcTrade", otcTrade);
            trade.put("tradeId", "TRD-001");

            Map<String, Object> input = new HashMap<>();
            input.put("rateTypeCode", "FIXED");
            input.put("legIndex", 0);
            input.put("trade", trade);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(input);
            Map<String, Object> enriched = result.getEnrichedData();

            // Verify the nested field was set
            @SuppressWarnings("unchecked")
            Map<String, Object> resultTrade = (Map<String, Object>) enriched.get("trade");
            @SuppressWarnings("unchecked")
            Map<String, Object> resultOtcTrade = (Map<String, Object>) resultTrade.get("otcTrade");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultLegs = (List<Map<String, Object>>) resultOtcTrade.get("otcLeg");

            assertEquals("FIXED", resultLegs.get(0).get("legInterestRateType"),
                "Leg interest rate type should be enriched");
            assertEquals("LEG001", resultLegs.get(0).get("legId"),
                "Existing leg fields should be preserved");
            assertEquals(1000000, resultLegs.get(0).get("notional"),
                "Existing leg notional should be preserved");
        }

        @Test
        @DisplayName("Multiple leg enrichments in single pass")
        void testMultipleLegEnrichments() throws Exception {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                TEST_YAML_PATH + "NestedTargetFieldEnrichmentTest-multi-leg.yaml");

            // Pre-create trade with two legs
            Map<String, Object> leg0 = new HashMap<>();
            leg0.put("legId", "PAY");
            Map<String, Object> leg1 = new HashMap<>();
            leg1.put("legId", "RCV");

            List<Map<String, Object>> legs = new ArrayList<>();
            legs.add(leg0);
            legs.add(leg1);

            Map<String, Object> trade = new HashMap<>();
            trade.put("legs", legs);

            Map<String, Object> input = new HashMap<>();
            input.put("payLegRate", 0.025);
            input.put("payLegType", "FIXED");
            input.put("receiveLegRate", 0.0);
            input.put("receiveLegType", "FLOAT");
            input.put("trade", trade);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> enriched = engine.evaluate(input).getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> resultTrade = (Map<String, Object>) enriched.get("trade");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultLegs = (List<Map<String, Object>>) resultTrade.get("legs");

            // Verify leg 0 (pay leg)
            assertEquals(0.025, resultLegs.get(0).get("rate"));
            assertEquals("FIXED", resultLegs.get(0).get("legType"));
            assertEquals("PAY", resultLegs.get(0).get("legId"));

            // Verify leg 1 (receive leg)
            assertEquals(0.0, resultLegs.get(1).get("rate"));
            assertEquals("FLOAT", resultLegs.get(1).get("legType"));
            assertEquals("RCV", resultLegs.get(1).get("legId"));
        }

        @Test
        @DisplayName("Set literal constant value to deeply nested OTC leg field")
        void testLiteralValueToNestedOtcLegField() throws Exception {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                TEST_YAML_PATH + "NestedTargetFieldEnrichmentTest-literal-to-nested.yaml");

            // Pre-create the full OTC trade structure
            Map<String, Object> leg0 = new HashMap<>();
            leg0.put("legId", "LEG001");
            leg0.put("notional", 5000000);

            List<Map<String, Object>> otcLeg = new ArrayList<>();
            otcLeg.add(leg0);

            Map<String, Object> otcTrade = new HashMap<>();
            otcTrade.put("otcLeg", otcLeg);
            otcTrade.put("tradeType", "IRS");

            Map<String, Object> trade = new HashMap<>();
            trade.put("otcTrade", otcTrade);
            trade.put("tradeId", "TRD-002");

            Map<String, Object> input = new HashMap<>();
            input.put("trade", trade);
            // Note: No source field needed - using literal constant expression

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(input);
            Map<String, Object> enriched = result.getEnrichedData();

            // Verify the nested field was set with the literal value
            @SuppressWarnings("unchecked")
            Map<String, Object> resultTrade = (Map<String, Object>) enriched.get("trade");
            @SuppressWarnings("unchecked")
            Map<String, Object> resultOtcTrade = (Map<String, Object>) resultTrade.get("otcTrade");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultLegs = (List<Map<String, Object>>) resultOtcTrade.get("otcLeg");

            assertEquals("FIXED", resultLegs.get(0).get("legInterestRateType"),
                "Literal value 'FIXED' should be set in deeply nested leg field");
            assertEquals("LEG001", resultLegs.get(0).get("legId"),
                "Existing leg fields should be preserved");
            assertEquals(5000000, resultLegs.get(0).get("notional"),
                "Existing leg notional should be preserved");
            assertEquals("IRS", resultOtcTrade.get("tradeType"),
                "Existing otcTrade fields should be preserved");
        }
    }

    @Nested
    @DisplayName("Portfolio Position Enrichment Scenarios")
    class PortfolioPositionTests {

        @Test
        @DisplayName("Enrich position pricing with calculation result")
        void testPositionPricingEnrichment() throws Exception {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                TEST_YAML_PATH + "NestedTargetFieldEnrichmentTest-position-pricing.yaml");

            // Pre-create position with pricing structure
            Map<String, Object> pricing = new HashMap<>();
            pricing.put("currency", "EUR");

            Map<String, Object> position = new HashMap<>();
            position.put("pricing", pricing);
            position.put("securityId", "DE0001234567");

            Map<String, Object> input = new HashMap<>();
            input.put("quantity", 1000);
            input.put("price", 105.50);
            input.put("fxRate", 1.08);
            input.put("position", position);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> enriched = engine.evaluate(input).getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> resultPosition = (Map<String, Object>) enriched.get("position");
            @SuppressWarnings("unchecked")
            Map<String, Object> resultPricing = (Map<String, Object>) resultPosition.get("pricing");

            assertEquals(105500.0, (Double) resultPricing.get("marketValue"), 0.01);
            assertEquals(113940.0, (Double) resultPricing.get("marketValueBase"), 0.01);
            assertEquals("EUR", resultPricing.get("currency"), "Existing field preserved");
        }
    }

    @Nested
    @DisplayName("Known Limitation Demonstrations")
    class KnownLimitationTests {

        @Test
        @DisplayName("LIMITATION: Dot-path without SpEL creates literal key")
        void testDotPathWithoutSpelCreatesLiteralKey() throws Exception {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                TEST_YAML_PATH + "NestedTargetFieldEnrichmentTest-limitations.yaml");

            Map<String, Object> input = new HashMap<>();
            input.put("value", "TEST");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> enriched = engine.evaluate(input).getEnrichedData();

            // LIMITATION: No nested structure created
            assertNull(enriched.get("nested"), "No nested object created");
            assertEquals("TEST", enriched.get("nested.field"), "Stored as literal key");
        }

        @Test
        @DisplayName("SpEL failure when structure missing is properly reported")
        void testSpelFailureWhenStructureMissingIsReported() throws Exception {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                TEST_YAML_PATH + "NestedTargetFieldEnrichmentTest-missing-structure.yaml");

            Map<String, Object> input = new HashMap<>();
            input.put("value", "TEST");
            // Note: "missing" structure is NOT pre-created

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(input);

            // Verify the failure is properly reported (not silent)
            assertFalse(result.isSuccess(),
                "Result should indicate failure when SpEL cannot set field due to missing structure");

            // Verify the structure was not auto-created
            Map<String, Object> enriched = result.getEnrichedData();
            assertNull(enriched.get("missing"), "Structure not auto-created");
            assertFalse(enriched.containsKey("field"), "Field not set at root");
        }

        @Test
        @DisplayName("WORKAROUND: Pre-create structure before enrichment")
        void testWorkaroundPreCreateStructure() throws Exception {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                TEST_YAML_PATH + "NestedTargetFieldEnrichmentTest-workaround.yaml");

            // WORKAROUND: Pre-create the full nested structure
            Map<String, Object> nested = new HashMap<>();
            Map<String, Object> result = new HashMap<>();
            result.put("nested", nested);

            Map<String, Object> input = new HashMap<>();
            input.put("value", "SUCCESS");
            input.put("result", result);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String, Object> enriched = engine.evaluate(input).getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> resultObj = (Map<String, Object>) enriched.get("result");
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedObj = (Map<String, Object>) resultObj.get("nested");

            assertEquals("SUCCESS", nestedObj.get("field"), "Field set in pre-created structure");
        }
    }
}

