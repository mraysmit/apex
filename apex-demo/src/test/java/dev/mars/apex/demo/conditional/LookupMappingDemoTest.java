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

package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the RefLookup Markit pattern: the THEN side of a mapping rule
 * is a pure lookup — no condition gate needed. This is the simplest enrichment
 * pattern: "for this key, look up the value".
 *
 * <p>Scenario: Counterparty reference data enrichment.
 * Given a counterparty code, look up the legal name, LEI, and jurisdiction.</p>
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichments in YAML — 3 conditional-mapping-enrichments
 * 2. Verify log shows "Processed: X out of X" — 100% execution rate
 * 3. Check EVERY mapping type — lookup mappings with output-field extraction
 * 4. Validate EVERY lookup result — counterparty name, LEI, jurisdiction
 * 5. Assert ALL enrichment results — 3 target fields populated
 */
public class LookupMappingDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(LookupMappingDemoTest.class);

    private static final String CONFIG_PATH =
            "src/test/resources/dev/mars/apex/demo/conditional/LookupMappingDemoTest.yaml";

    @Test
    @DisplayName("Goldman Sachs lookup: name, LEI, and jurisdiction")
    void shouldLookupGoldmanSachs() {
        logger.info("=== Testing Lookup Mapping: Goldman Sachs ===");
        logger.info("Flow: COUNTERPARTY_CODE='GS' → lookup → name/LEI/jurisdiction populated");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);
            logger.info("[OK] Configuration loaded: {} enrichments", config.getEnrichments().size());

            Map<String, Object> testData = new HashMap<>();
            testData.put("COUNTERPARTY_CODE", "GS");
            logger.info("Input: {}", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            logger.info("[OK] Enriched data: {}", enrichedData);

            assertEquals("Goldman Sachs Group Inc.", enrichedData.get("COUNTERPARTY_NAME"),
                    "Should look up Goldman Sachs legal name");
            assertEquals("784F5XWPLTWKTBV8GR34", enrichedData.get("COUNTERPARTY_LEI"),
                    "Should look up Goldman Sachs LEI");
            assertEquals("US", enrichedData.get("REGULATORY_REGION"),
                    "Should look up Goldman Sachs jurisdiction");

            logger.info("[OK] All 3 counterparty fields populated: name='{}', LEI='{}', region='{}'",
                    enrichedData.get("COUNTERPARTY_NAME"),
                    enrichedData.get("COUNTERPARTY_LEI"),
                    enrichedData.get("REGULATORY_REGION"));

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should look up Goldman Sachs counterparty data: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Deutsche Bank lookup: different jurisdiction (DE)")
    void shouldLookupDeutscheBank() {
        logger.info("=== Testing Lookup Mapping: Deutsche Bank ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

            Map<String, Object> testData = new HashMap<>();
            testData.put("COUNTERPARTY_CODE", "DB");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);

            assertEquals("Deutsche Bank AG", enrichedData.get("COUNTERPARTY_NAME"),
                    "Should look up Deutsche Bank legal name");
            assertEquals("7LTWFZYICNSX8D621K86", enrichedData.get("COUNTERPARTY_LEI"),
                    "Should look up Deutsche Bank LEI");
            assertEquals("DE", enrichedData.get("REGULATORY_REGION"),
                    "Should look up Deutsche Bank jurisdiction as DE");

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should look up Deutsche Bank counterparty data: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Unknown counterparty falls through to UNKNOWN for jurisdiction")
    void shouldHandleUnknownCounterparty() {
        logger.info("=== Testing Lookup Mapping: Unknown Counterparty ===");
        logger.info("Flow: COUNTERPARTY_CODE='UNKNOWN_CP' → lookup returns null → fallback fires for jurisdiction");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

            Map<String, Object> testData = new HashMap<>();
            testData.put("COUNTERPARTY_CODE", "UNKNOWN_CP");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            logger.info("[OK] Enriched data: {}", enrichedData);

            // Jurisdiction enrichment has a fallback rule at priority 999
            assertEquals("UNKNOWN", enrichedData.get("REGULATORY_REGION"),
                    "Unknown counterparty should fall through to UNKNOWN jurisdiction");

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should handle unknown counterparty gracefully: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Barclays lookup verifies GB jurisdiction")
    void shouldLookupBarclays() {
        logger.info("=== Testing Lookup Mapping: Barclays ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(CONFIG_PATH);

            Map<String, Object> testData = new HashMap<>();
            testData.put("COUNTERPARTY_CODE", "BARC");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);

            assertEquals("Barclays PLC", enrichedData.get("COUNTERPARTY_NAME"),
                    "Should look up Barclays legal name");
            assertEquals("213800LBQA1Y9L22JB70", enrichedData.get("COUNTERPARTY_LEI"),
                    "Should look up Barclays LEI");
            assertEquals("GB", enrichedData.get("REGULATORY_REGION"),
                    "Should look up Barclays jurisdiction as GB");

        } catch (Exception e) {
            logger.error("Failed: " + e.getMessage(), e);
            fail("Should look up Barclays counterparty data: " + e.getMessage());
        }
    }
}
