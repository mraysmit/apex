package dev.mars.apex.demo.enrichment;

import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
 * ConstantValueFieldEnrichmentTest - Demonstrates correct patterns for assigning constant values
 *
 * This test suite demonstrates the CORRECT pattern for assigning constant values in field-enrichment:
 * 
 * CORRECT PATTERN:
 * - Use source-field: "constant" (special keyword)
 * - Use expression: "'value'" (SpEL string literal with single quotes inside double quotes)
 * OR
 * - Use transformation: "'value'" (alternative to expression)
 *
 * INCORRECT PATTERNS (DO NOT USE):
 * - source-field: '''value''' (triple quotes - WRONG!)
 * - source-field: 'value' (direct value - WRONG!)
 * 
 * TEST SCENARIOS:
 * 1. Constant string assignment (e.g., status codes, categories)
 * 2. Constant numeric assignment (integers, decimals, booleans)
 * 3. Conditional constant assignment (based on conditions)
 * 4. Nested field constant assignment (using #field notation)
 * 5. Multiple constant assignments in single enrichment
 * 6. OTC Options trade processing example (domain-specific)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-12
 * @version 1.0 - Initial implementation demonstrating constant value patterns
 */
class ConstantValueFieldEnrichmentTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ConstantValueFieldEnrichmentTest.class);

    @Test
    @DisplayName("Test 1: Constant String Assignment - Basic Pattern")
    void testConstantStringAssignment() {
        logger.info("=== Test 1: Constant String Assignment ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/ConstantValueFieldEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("tradeId", "OTC-12345");
            tradeData.put("notionalAmount", 5000000.0);

            logger.info("Input: {}", tradeData);

            RuleResult result = engine.evaluate(config, tradeData);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify constant string values were assigned
            assertEquals("ACTIVE", enrichedData.get("status"), "Status should be 'ACTIVE'");
            assertEquals("e1", enrichedData.get("enrichmentId"), "Enrichment ID should be 'e1'");
            assertEquals("OTC_OPTION", enrichedData.get("productType"), "Product type should be 'OTC_OPTION'");

            logger.info("Constant string assignment successful");
            logger.info("   status: {}", enrichedData.get("status"));
            logger.info("   enrichmentId: {}", enrichedData.get("enrichmentId"));
            logger.info("   productType: {}", enrichedData.get("productType"));

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 2: Constant Numeric Assignment - Numbers and Booleans")
    void testConstantNumericAssignment() {
        logger.info("=== Test 2: Constant Numeric Assignment ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/ConstantValueFieldEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("tradeId", "OTC-67890");

            logger.info("Input: {}", tradeData);

            RuleResult result = engine.evaluate(config, tradeData);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify constant numeric values were assigned
            assertEquals(100, enrichedData.get("defaultQuantity"), "Default quantity should be 100");
            assertEquals(99.99, enrichedData.get("defaultPrice"), "Default price should be 99.99");
            assertEquals(true, enrichedData.get("requiresValidation"), "Requires validation should be true");

            logger.info("Constant numeric assignment successful");
            logger.info("   defaultQuantity: {}", enrichedData.get("defaultQuantity"));
            logger.info("   defaultPrice: {}", enrichedData.get("defaultPrice"));
            logger.info("   requiresValidation: {}", enrichedData.get("requiresValidation"));

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 3: Conditional Constant Assignment - High Value Trade")
    void testConditionalConstantAssignmentHighValue() {
        logger.info("=== Test 3: Conditional Constant Assignment - High Value ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/ConstantValueFieldEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            // High value trade
            Map<String, Object> highValueTrade = new HashMap<>();
            highValueTrade.put("tradeId", "OTC-HIGH-001");
            highValueTrade.put("notionalAmount", 15000000.0);

            logger.info("Input (High Value): {}", highValueTrade);

            RuleResult result = engine.evaluate(config, highValueTrade);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify high value constants were assigned
            assertEquals("HIGH_VALUE", enrichedData.get("valueCategory"), "Value category should be 'HIGH_VALUE'");
            assertEquals(true, enrichedData.get("requiresApproval"), "Requires approval should be true");
            assertEquals("SENIOR_TRADER", enrichedData.get("approvalLevel"), "Approval level should be 'SENIOR_TRADER'");

            logger.info("High value conditional assignment successful");
            logger.info("   valueCategory: {}", enrichedData.get("valueCategory"));
            logger.info("   requiresApproval: {}", enrichedData.get("requiresApproval"));
            logger.info("   approvalLevel: {}", enrichedData.get("approvalLevel"));

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 4: Conditional Constant Assignment - Standard Value Trade")
    void testConditionalConstantAssignmentStandardValue() {
        logger.info("=== Test 4: Conditional Constant Assignment - Standard Value ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/ConstantValueFieldEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            // Standard value trade
            Map<String, Object> standardTrade = new HashMap<>();
            standardTrade.put("tradeId", "OTC-STD-001");
            standardTrade.put("notionalAmount", 500000.0);

            logger.info("Input (Standard Value): {}", standardTrade);

            RuleResult result = engine.evaluate(config, standardTrade);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify standard value constants were assigned
            assertEquals("STANDARD", enrichedData.get("valueCategory"), "Value category should be 'STANDARD'");
            assertEquals(false, enrichedData.get("requiresApproval"), "Requires approval should be false");
            assertEquals("AUTO_APPROVED", enrichedData.get("approvalLevel"), "Approval level should be 'AUTO_APPROVED'");

            logger.info("Standard value conditional assignment successful");
            logger.info("   valueCategory: {}", enrichedData.get("valueCategory"));
            logger.info("   requiresApproval: {}", enrichedData.get("requiresApproval"));
            logger.info("   approvalLevel: {}", enrichedData.get("approvalLevel"));

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 5: Nested Field Constant Assignment")
    void testNestedFieldConstantAssignment() {
        logger.info("=== Test 5: Nested Field Constant Assignment ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/ConstantValueFieldEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            Map<String, Object> tradeData = new HashMap<>();
            Map<String, Object> metadata = new HashMap<>();
            tradeData.put("tradeId", "OTC-NESTED-001");
            tradeData.put("metadata", metadata);

            logger.info("Input: {}", tradeData);

            RuleResult result = engine.evaluate(config, tradeData);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedMetadata = (Map<String, Object>) enrichedData.get("metadata");
            assertNotNull(enrichedMetadata, "Metadata should not be null");

            // Verify nested field constants were assigned
            assertEquals("VALIDATED", enrichedMetadata.get("validationStatus"), "Validation status should be 'VALIDATED'");
            assertEquals("SYSTEM", enrichedMetadata.get("source"), "Source should be 'SYSTEM'");
            assertEquals("v1.0", enrichedMetadata.get("version"), "Version should be 'v1.0'");

            logger.info("Nested field constant assignment successful");
            logger.info("   metadata.validationStatus: {}", enrichedMetadata.get("validationStatus"));
            logger.info("   metadata.source: {}", enrichedMetadata.get("source"));
            logger.info("   metadata.version: {}", enrichedMetadata.get("version"));

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    // ==================== NEGATIVE TESTS ====================

    @Test
    @DisplayName("NEGATIVE: Invalid Constant Pattern - Triple Quotes (WRONG!)")
    void testInvalidConstantPatternTripleQuotes() {
        logger.info("=== NEGATIVE TEST: Invalid Triple Quotes Pattern ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/ConstantValueFieldEnrichmentTest-negative.yaml");
            assertNotNull(config, "Configuration should load");

            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            Map<String, Object> tradeData = new HashMap<>();
            tradeData.put("tradeId", "OTC-12345");
            tradeData.put("notionalAmount", 5000000.0);

            logger.info("Input: {}", tradeData);
            logger.info("⚠️  Testing INCORRECT pattern: source-field: '''e1''' (triple quotes)");

            RuleResult result = engine.evaluate(config, tradeData);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // VERIFY: The triple-quote pattern does NOT work as intended
            // It will try to use '''e1''' as a field name, not as a constant value
            // The field 'a_field' should either be missing or have wrong value

            if (enrichedData.containsKey("a_field")) {
                Object value = enrichedData.get("a_field");
                logger.warn("INCORRECT PATTERN RESULT: a_field = {}", value);
                logger.warn("   Expected: 'e1' (constant string)");
                logger.warn("   Actual: {} (field lookup or error)", value);

                // The value should NOT be the constant string 'e1'
                // It will be null or some other unexpected value
                assertNotEquals("e1", value,
                    "Triple quotes pattern should NOT assign constant 'e1' correctly");
            } else {
                logger.warn("INCORRECT PATTERN: Field 'a_field' was not set at all");
            }

            logger.info("Negative test confirmed: Triple quotes pattern does NOT work");
            logger.info("   CORRECT PATTERN: source-field: \"constant\" + expression: \"'e1'\"");

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

    // ==================== POSITIVE TESTS ====================

    @Test
    @DisplayName("Test 6: OTC Options Trade Processing - Domain Example")
    void testOtcOptionsTradeProcessing() {
        logger.info("=== Test 6: OTC Options Trade Processing ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/enrichment/ConstantValueFieldEnrichmentTest.yaml");
            assertNotNull(config, "Configuration should load");

            RulesEngine engine = new RulesEngine(rulesEngineConfiguration);

            Map<String, Object> optionTrade = new HashMap<>();
            optionTrade.put("tradeId", "OPT-EUR-USD-001");
            optionTrade.put("underlying", "EUR/USD");
            optionTrade.put("optionType", "CALL");
            optionTrade.put("notionalAmount", 10000000.0);

            logger.info("Input (OTC Option): {}", optionTrade);

            RuleResult result = engine.evaluate(config, optionTrade);
            assertNotNull(result, "Result should not be null");

            Map<String, Object> enrichedData = result.getEnrichedData();

            // Verify OTC option specific constants
            assertEquals("OTC_OPTION", enrichedData.get("assetClass"), "Asset class should be 'OTC_OPTION'");
            assertEquals("FX_DERIVATIVES", enrichedData.get("tradingDesk"), "Trading desk should be 'FX_DERIVATIVES'");
            assertEquals("ISDA", enrichedData.get("documentationType"), "Documentation type should be 'ISDA'");
            assertEquals("T+2", enrichedData.get("settlementCycle"), "Settlement cycle should be 'T+2'");

            logger.info("OTC Options trade processing successful");
            logger.info("   assetClass: {}", enrichedData.get("assetClass"));
            logger.info("   tradingDesk: {}", enrichedData.get("tradingDesk"));
            logger.info("   documentationType: {}", enrichedData.get("documentationType"));
            logger.info("   settlementCycle: {}", enrichedData.get("settlementCycle"));

        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
            fail("Test should not throw exception: " + e.getMessage());
        }
    }
}

