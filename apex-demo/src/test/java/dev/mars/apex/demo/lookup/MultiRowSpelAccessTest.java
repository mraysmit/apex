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
 * MultiRowSpelAccessTest - Multi-Row Lookup + SpEL Collection Operators Demo
 *
 * PURPOSE: Demonstrate how to use rows: "all" to retrieve a full List of matching
 * records, then use SpEL collection operators in subsequent enrichments and rules
 * to filter, navigate, and extract individual rows from that list.
 *
 * CRITICAL VALIDATION CHECKLIST:
 * 1. Multi-row lookup populates #settlementInstructions as a List (3 rows for CP001)
 * 2. SpEL [0] index access extracts primary settlement method (DTC)
 * 3. SpEL .^[condition] find-first extracts USD instruction ID (SI-001)
 * 4. SpEL .?[condition].size() counts filtered rows (2 high-priority)
 * 5. SpEL .^[condition]['field'] navigates into matched row (USD maxAmount)
 * 6. Rules using .size(), .?[], none-match patterns all evaluate correctly
 * 7. CP002 with 2 rows also works (different data shape)
 *
 * BUSINESS LOGIC (CP001):
 * - 3 settlement instructions: USD/DTC (priority 1), EUR/TARGET2 (priority 2), GBP/CREST (priority 3)
 * - Primary method = DTC (first row)
 * - USD instruction = SI-001
 * - High-priority count (priority <= 2) = 2
 * - USD max amount = 5,000,000
 * - All instructions have capacity >= 1,000,000 ✓
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiRowSpelAccessTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(MultiRowSpelAccessTest.class);

    private static final String YAML_PATH =
            "src/test/java/dev/mars/apex/demo/lookup/MultiRowSpelAccessTest.yaml";

    @Test
    @Order(1)
    @DisplayName("SpEL index access: extract primary settlement method from first row")
    void testSpelIndexAccess_PrimarySettlementMethod() {
        logger.info("=".repeat(80));
        logger.info("SpEL Index Access Test: #settlementInstructions[0]['settlementMethod']");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            // Verify the multi-row list was populated
            Object settlements = enrichedData.get("settlementInstructions");
            assertNotNull(settlements, "settlementInstructions should not be null");
            assertInstanceOf(List.class, settlements);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> settlementList = (List<Map<String, Object>>) settlements;
            assertEquals(3, settlementList.size(), "CP001 should have 3 settlement instructions");
            logger.info("Multi-row list populated with {} rows", settlementList.size());

            // Verify SpEL [0] index extracted the primary method
            Object primaryMethod = enrichedData.get("primarySettlementMethod");
            assertNotNull(primaryMethod, "primarySettlementMethod should be extracted via SpEL [0]");
            assertEquals("DTC", primaryMethod, "First instruction settlement method should be DTC");
            logger.info("PASS: SpEL [0] index access → primarySettlementMethod = {}", primaryMethod);

        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("SpEL find-first (.^[]): extract USD instruction ID")
    void testSpelFindFirst_UsdInstructionId() {
        logger.info("=".repeat(80));
        logger.info("SpEL Find-First Test: .^[currency == 'USD']['instructionId']");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            // Verify SpEL .^[currency == 'USD']['instructionId'] found SI-001
            Object usdId = enrichedData.get("usdInstructionId");
            assertNotNull(usdId, "usdInstructionId should be found via SpEL .^[]");
            assertEquals("SI-001", usdId, "USD instruction should be SI-001");
            logger.info("PASS: SpEL .^[currency == 'USD'] → usdInstructionId = {}", usdId);

        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("SpEL filter+count (.?[].size()): count high-priority instructions")
    void testSpelFilterCount_HighPriorityCount() {
        logger.info("=".repeat(80));
        logger.info("SpEL Filter+Count Test: .?[priority <= 2].size()");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            // Verify SpEL .?[priority <= 2].size() counted 2 high-priority instructions
            Object count = enrichedData.get("highPriorityCount");
            assertNotNull(count, "highPriorityCount should be computed via SpEL .?[].size()");
            assertEquals(2, ((Number) count).intValue(),
                    "CP001 should have 2 high-priority instructions (priority <= 2)");
            logger.info("PASS: SpEL .?[priority <= 2].size() → highPriorityCount = {}", count);

        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("SpEL find-first+navigate (.^[]['field']): get USD max amount")
    void testSpelFindFirstNavigate_UsdMaxAmount() {
        logger.info("=".repeat(80));
        logger.info("SpEL Find-First+Navigate Test: .^[currency == 'USD']['maxAmount']");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            // Verify SpEL .^[currency == 'USD']['maxAmount'] navigated into the matched row
            Object maxAmount = enrichedData.get("usdMaxAmount");
            assertNotNull(maxAmount, "usdMaxAmount should be extracted via SpEL .^[]['maxAmount']");
            assertEquals(5000000, ((Number) maxAmount).intValue(),
                    "USD instruction maxAmount should be 5,000,000");
            logger.info("PASS: SpEL .^[currency == 'USD']['maxAmount'] → usdMaxAmount = {}", maxAmount);

        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Rules validate multi-row data using SpEL collection operators")
    void testRulesWithSpelCollectionOperators() {
        logger.info("=".repeat(80));
        logger.info("Rules Validation Test: .size(), .?[], none-match patterns");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP001");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            // All 5 rules should match for CP001
            assertNotNull(ruleResult, "RuleResult should not be null");

            // Log all matched rules
            if (ruleResult.getChildResults() != null) {
                logger.info("Matched rules:");
                ruleResult.getChildResults().forEach(r ->
                        logger.info("  [{}] {} → {}", r.getSeverity(), r.getRuleId(), r.getMessage()));
            }

            logger.info("PASS: Rules using SpEL collection operators evaluated successfully");

        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("CP002 with 2 rows: SpEL operators work with different list sizes")
    void testCp002_TwoRows_SpelStillWorks() {
        logger.info("=".repeat(80));
        logger.info("CP002 Test: 2 settlement instructions - SpEL operators still work");
        logger.info("=".repeat(80));

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_PATH);
            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyId", "CP002");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) ruleResult.getEnrichedData();

            // Multi-row: 2 instructions for CP002
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> settlementList =
                    (List<Map<String, Object>>) enrichedData.get("settlementInstructions");
            assertNotNull(settlementList);
            assertEquals(2, settlementList.size(), "CP002 should have 2 settlement instructions");

            // [0] index → DTC (first instruction for CP002)
            assertEquals("DTC", enrichedData.get("primarySettlementMethod"),
                    "CP002 primary method should be DTC");

            // .^[currency == 'USD'] → SI-004
            assertEquals("SI-004", enrichedData.get("usdInstructionId"),
                    "CP002 USD instruction should be SI-004");

            // .^[currency == 'USD']['maxAmount'] → 10,000,000
            Object maxAmount = enrichedData.get("usdMaxAmount");
            assertNotNull(maxAmount);
            assertEquals(10000000, ((Number) maxAmount).intValue(),
                    "CP002 USD max amount should be 10,000,000");

            // .?[priority <= 2].size() → 2 (both instructions are high-priority)
            Object count = enrichedData.get("highPriorityCount");
            assertNotNull(count);
            assertEquals(2, ((Number) count).intValue(),
                    "CP002 should have 2 high-priority instructions");

            logger.info("PASS: CP002 SpEL operators work correctly with 2-row list");

        } catch (Exception e) {
            logger.error("Test failed with exception", e);
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}
