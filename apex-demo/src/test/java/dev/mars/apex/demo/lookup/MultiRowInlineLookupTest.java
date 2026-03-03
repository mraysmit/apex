package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for multi-row lookup functionality (rows: "all").
 *
 * CRITICAL VALIDATION CHECKLIST:
 * 1. Multi-row lookup returns List of all matching records (not just first)
 * 2. Single-row lookup (default, no rows: config) still returns first row only
 * 3. Multi-row lookup with no matches returns empty list
 * 4. Both enrichments in same YAML execute correctly
 *
 * BUSINESS LOGIC:
 * - CP001 has 3 settlement instructions → multi-row returns all 3
 * - CP002 has 2 settlement instructions → multi-row returns both
 * - CP003 has 1 settlement instruction → multi-row returns list of 1
 * - Single-row counterparty lookup always returns one record (default behavior)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiRowInlineLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(MultiRowInlineLookupTest.class);

    private static final String YAML_PATH =
            "src/test/java/dev/mars/apex/demo/lookup/MultiRowInlineLookupTest.yaml";

    @Test
    @Order(1)
    @DisplayName("Multi-row lookup should return all 3 settlement instructions for CP001")
    void testMultiRowLookup_CP001_Returns3Rows() {
        logger.info("=".repeat(80));
        logger.info("Multi-Row Lookup Test: CP001 - 3 settlement instructions expected");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            assertNotNull(config, "YAML configuration should not be null");

            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Object result = ruleResult.getEnrichedData();

            assertNotNull(result, "Enriched data should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate multi-row result: settlementInstructions should be a List
            Object settlements = enrichedData.get("settlementInstructions");
            assertNotNull(settlements, "settlementInstructions should not be null");
            assertInstanceOf(List.class, settlements, "settlementInstructions should be a List");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> settlementList = (List<Map<String, Object>>) settlements;
            assertEquals(3, settlementList.size(), "CP001 should have 3 settlement instructions");

            // Validate each instruction
            logger.info("Settlement instructions for CP001:");
            for (Map<String, Object> instruction : settlementList) {
                logger.info("  {} - {} via {}", instruction.get("instructionId"),
                        instruction.get("currency"), instruction.get("settlementMethod"));
            }

            // Verify specific instructions exist
            assertTrue(settlementList.stream().anyMatch(i -> "SI-001".equals(i.get("instructionId"))));
            assertTrue(settlementList.stream().anyMatch(i -> "SI-002".equals(i.get("instructionId"))));
            assertTrue(settlementList.stream().anyMatch(i -> "SI-003".equals(i.get("instructionId"))));

            // Validate single-row enrichment also executed (counterparty details)
            assertEquals("Goldman Sachs", enrichedData.get("counterpartyName"),
                    "Single-row lookup should return counterparty name");
            assertEquals("North America", enrichedData.get("counterpartyRegion"));
            assertEquals("ACTIVE", enrichedData.get("counterpartyStatus"));

            logger.info("PASS: CP001 multi-row + single-row lookups both successful");
        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Multi-row lookup should return 2 settlement instructions for CP002")
    void testMultiRowLookup_CP002_Returns2Rows() {
        logger.info("=".repeat(80));
        logger.info("Multi-Row Lookup Test: CP002 - 2 settlement instructions expected");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP002");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> settlementList =
                    (List<Map<String, Object>>) enrichedData.get("settlementInstructions");

            assertNotNull(settlementList, "settlementInstructions should not be null for CP002");
            assertEquals(2, settlementList.size(), "CP002 should have 2 settlement instructions");

            assertTrue(settlementList.stream().anyMatch(i -> "SI-004".equals(i.get("instructionId"))));
            assertTrue(settlementList.stream().anyMatch(i -> "SI-005".equals(i.get("instructionId"))));

            // Validate single-row enrichment
            assertEquals("Deutsche Bank", enrichedData.get("counterpartyName"));

            logger.info("PASS: CP002 multi-row returned {} rows", settlementList.size());
        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Multi-row lookup should return 1 settlement instruction for CP003")
    void testMultiRowLookup_CP003_Returns1Row() {
        logger.info("=".repeat(80));
        logger.info("Multi-Row Lookup Test: CP003 - 1 settlement instruction expected");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP003");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> settlementList =
                    (List<Map<String, Object>>) enrichedData.get("settlementInstructions");

            assertNotNull(settlementList, "settlementInstructions should not be null for CP003");
            assertEquals(1, settlementList.size(), "CP003 should have 1 settlement instruction");
            assertEquals("SI-006", settlementList.get(0).get("instructionId"));
            assertEquals("CHF", settlementList.get(0).get("currency"));

            // Validate single-row enrichment
            assertEquals("Nomura", enrichedData.get("counterpartyName"));

            logger.info("PASS: CP003 multi-row returned {} row", settlementList.size());
        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Multi-row lookup should return empty list for unknown counterparty")
    void testMultiRowLookup_UnknownCounterparty_ReturnsEmptyList() {
        logger.info("=".repeat(80));
        logger.info("Multi-Row Lookup Test: UNKNOWN - empty list expected");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP999");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            // The multi-row lookup should set an empty list
            // The single-row counterparty lookup will fail (no match) - that's OK
            Object result = ruleResult.getEnrichedData();
            assertNotNull(result, "Enriched data should not be null even with no matches");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            Object settlements = enrichedData.get("settlementInstructions");
            assertNotNull(settlements, "settlementInstructions should be set (empty list, not null)");
            assertInstanceOf(List.class, settlements, "settlementInstructions should be a List");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> settlementList = (List<Map<String, Object>>) settlements;
            assertEquals(0, settlementList.size(), "Unknown counterparty should return empty settlement list");

            logger.info("PASS: Unknown counterparty returned empty list as expected");
        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Default single-row behavior is preserved when rows is not specified")
    void testDefaultSingleRowBehaviorPreserved() {
        logger.info("=".repeat(80));
        logger.info("Default Single-Row Behavior Preservation Test");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            // The single-row enrichment (no rows: config) should return a single String value,
            // NOT a List - this proves default behavior is preserved
            Object counterpartyName = enrichedData.get("counterpartyName");
            assertNotNull(counterpartyName, "counterpartyName should be set by single-row lookup");
            assertInstanceOf(String.class, counterpartyName,
                    "Single-row lookup result should be a String, not a List");
            assertEquals("Goldman Sachs", counterpartyName);

            logger.info("PASS: Default single-row behavior preserved - counterpartyName is String: {}", counterpartyName);
        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}
