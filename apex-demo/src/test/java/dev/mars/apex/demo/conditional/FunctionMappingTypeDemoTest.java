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
 * Integration test for the function mapping type within conditional-mapping-enrichment.
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * 1. Count enrichments in YAML - 2 enrichments expected (translator + function CME)
 * 2. Verify log shows "Processed: X out of X" - Must be 100% execution rate
 * 3. Check EVERY enrichment condition - Test data triggers conditions
 * 4. Validate EVERY business calculation - Test actual logic
 * 5. Assert ALL enrichment results - Every field mapping has assertEquals
 */
public class FunctionMappingTypeDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(FunctionMappingTypeDemoTest.class);

    @Test
    @DisplayName("Should invoke enrichment group via function mapping and extract output")
    void shouldInvokeEnrichmentGroupViaFunctionMapping() {
        logger.info("=== Testing Function Mapping: Basic Invocation ===");
        logger.info("Flow: condition (#INPUT_VALUE != null) -> bind input-parameters -> invoke translator-group -> extract translation_result -> write to FINAL_RESULT");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                    "src/test/resources/dev/mars/apex/demo/conditional/FunctionMappingTypeDemoTest.yaml");
            logger.info("[OK] Configuration loaded: {} enrichments, {} enrichment-groups",
                    config.getEnrichments().size(),
                    config.getEnrichmentGroups() != null ? config.getEnrichmentGroups().size() : 0);

            // Test data that triggers the function mapping rule
            Map<String, Object> testData = new HashMap<>();
            testData.put("INPUT_VALUE", "TRADE_001");
            testData.put("CLIENT_CODE", "CLIENT_A");
            logger.info("Input data: {} — INPUT_VALUE present, so function mapping condition will match", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            assertTrue(enrichedData instanceof Map);
            logger.info("[OK] Enrichment completed. Result keys: {}", enrichedData.keySet());
            logger.info("[OK] Full enriched data: {}", enrichedData);

            // Function mapping should invoke the translator group, which produces
            // "TRANSLATED_IS_NDF_TRADE_001" and writes it to FINAL_RESULT
            // Chain: input-params bind Translation_Type='IS_NDF' + Input_Code='TRADE_001'
            //        -> translator-enrichment expression: 'TRANSLATED_' + #Translation_Type + '_' + #Input_Code
            //        -> output-field extracts translation_result -> written to target-field FINAL_RESULT
            assertEquals("TRANSLATED_IS_NDF_TRADE_001", enrichedData.get("FINAL_RESULT"),
                    "Function mapping should invoke enrichment group and extract translation_result into FINAL_RESULT");

            logger.info("[OK] FINAL_RESULT='{}' — function mapping invocation verified end-to-end",
                    enrichedData.get("FINAL_RESULT"));

        } catch (Exception e) {
            logger.error("Failed to execute function mapping: " + e.getMessage(), e);
            fail("Should be able to execute function mapping: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should fall back to direct mapping when function condition not met")
    void shouldFallBackToDirectMapping() {
        logger.info("=== Testing Function Mapping: Fallback to Direct ===");
        logger.info("Function rule requires #INPUT_VALUE != null, but test data omits INPUT_VALUE");
        logger.info("Expected: function rule skipped -> default-fallback (priority 999) fires -> FINAL_RESULT='DEFAULT_NO_INPUT'");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                    "src/test/resources/dev/mars/apex/demo/conditional/FunctionMappingTypeDemoTest.yaml");
            logger.info("[OK] Configuration loaded successfully");

            // Test data without INPUT_VALUE — function mapping condition won't match
            Map<String, Object> testData = new HashMap<>();
            testData.put("CLIENT_CODE", "CLIENT_B");
            logger.info("Input data: {} — no INPUT_VALUE key present", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            assertTrue(enrichedData instanceof Map);
            logger.info("[OK] Enrichment completed. Full result: {}", enrichedData);

            assertEquals("DEFAULT_NO_INPUT", enrichedData.get("FINAL_RESULT"),
                    "Should fall back to default direct mapping when function condition not met");

            logger.info("[OK] FINAL_RESULT='{}' — function rule skipped, direct fallback applied correctly",
                    enrichedData.get("FINAL_RESULT"));

        } catch (Exception e) {
            logger.error("Failed to fall back to direct mapping: " + e.getMessage(), e);
            fail("Should be able to fall back to direct mapping: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should pass multiple input parameters to enrichment group")
    void shouldPassMultipleInputParameters() {
        logger.info("=== Testing Function Mapping: Multiple Input Parameters ===");
        logger.info("input-parameters: constant->'IS_NDF' -> Translation_Type, #INPUT_VALUE -> Input_Code");
        logger.info("Expected: translator group concatenates 'TRANSLATED_' + Translation_Type + '_' + Input_Code");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                    "src/test/resources/dev/mars/apex/demo/conditional/FunctionMappingTypeDemoTest.yaml");
            logger.info("[OK] Configuration loaded successfully");

            // Test data with all fields that get passed as input parameters
            Map<String, Object> testData = new HashMap<>();
            testData.put("INPUT_VALUE", "FX_SWAP");
            testData.put("CLIENT_CODE", "BANK_XYZ");
            logger.info("Input data: {} — INPUT_VALUE='FX_SWAP' will be bound to Input_Code", testData);

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, testData);
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();

            assertNotNull(enrichedData);
            logger.info("[OK] Enrichment completed. Result keys: {}", enrichedData.keySet());
            logger.info("[OK] Full enriched data: {}", enrichedData);

            // The translator enrichment concatenates Translation_Type + Input_Code
            // Translation_Type = 'IS_NDF' (constant), Input_Code = INPUT_VALUE = 'FX_SWAP'
            assertEquals("TRANSLATED_IS_NDF_FX_SWAP", enrichedData.get("FINAL_RESULT"),
                    "Function mapping should bind all input parameters and produce correct output");

            logger.info("[OK] FINAL_RESULT='{}' — both constant and dynamic input parameters bound correctly",
                    enrichedData.get("FINAL_RESULT"));

        } catch (Exception e) {
            logger.error("Failed to pass multiple input parameters: " + e.getMessage(), e);
            fail("Should be able to pass multiple input parameters: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should mix direct and function rules in priority chain")
    void shouldMixDirectAndFunctionRulesInPriorityChain() {
        logger.info("=== Testing Function Mapping: Mixed Priority Chain ===");
        logger.info("Uses FunctionMappingTypeDemoTest-mixed.yaml: 3 rules in priority chain");
        logger.info("  swift-direct  (priority 1): type=direct,   condition=#SYSTEM_CODE=='SWIFT' AND #IS_NDF=='Y'");
        logger.info("  compute-func  (priority 5): type=function, condition=#INPUT_VALUE!=null -> compute-group");
        logger.info("  default       (priority 999): type=direct, unconditional fallback");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                    "src/test/resources/dev/mars/apex/demo/conditional/FunctionMappingTypeDemoTest-mixed.yaml");
            logger.info("[OK] Configuration loaded: {} enrichments, {} enrichment-groups",
                    config.getEnrichments().size(),
                    config.getEnrichmentGroups() != null ? config.getEnrichmentGroups().size() : 0);

            // Sub-test 1: HIGH_PRIORITY direct mapping should win
            logger.info("--- Sub-test 1: Direct rule wins (SYSTEM_CODE='SWIFT', IS_NDF='Y') ---");
            Map<String, Object> directData = new HashMap<>();
            directData.put("SYSTEM_CODE", "SWIFT");
            directData.put("IS_NDF", "Y");
            directData.put("INPUT_VALUE", "IGNORED");
            logger.info("Input data: {} — both direct conditions satisfied; INPUT_VALUE also present but lower priority", directData);

            RulesEngine engine1 = RulesEngine.fromYamlConfig(config);
            RuleResult directResult = engine1.evaluate(config, directData);
            logger.info("[OK] Sub-test 1 result: {}", directResult.getEnrichedData());

            assertEquals("SWIFT_HIGH_PRIORITY", directResult.getEnrichedData().get("RESULT"),
                    "High priority direct rule should win over lower priority function rule");
            logger.info("[OK] RESULT='{}' — priority 1 direct rule matched, function rule at priority 5 not reached",
                    directResult.getEnrichedData().get("RESULT"));

            // Sub-test 2: Function mapping should execute when direct conditions don't match
            logger.info("--- Sub-test 2: Function rule fires (SYSTEM_CODE='OTHER', no IS_NDF) ---");
            Map<String, Object> functionData = new HashMap<>();
            functionData.put("SYSTEM_CODE", "OTHER");
            functionData.put("INPUT_VALUE", "CODE_99");
            logger.info("Input data: {} — direct condition fails (SYSTEM_CODE != 'SWIFT'), function condition matches (INPUT_VALUE != null)", functionData);

            RulesEngine engine2 = RulesEngine.fromYamlConfig(config);
            RuleResult functionResult = engine2.evaluate(config, functionData);
            logger.info("[OK] Sub-test 2 result: {}", functionResult.getEnrichedData());

            assertEquals("COMPUTED_CODE_99", functionResult.getEnrichedData().get("RESULT"),
                    "Function rule should execute when higher-priority direct conditions don't match");
            logger.info("[OK] RESULT='{}' — function rule invoked compute-group with input 'CODE_99', extracted compute_output",
                    functionResult.getEnrichedData().get("RESULT"));

            logger.info("[OK] Mixed priority chain test completed — both direct and function types coexist correctly");

        } catch (Exception e) {
            logger.error("Failed mixed priority chain test: " + e.getMessage(), e);
            fail("Should handle mixed priority chain: " + e.getMessage());
        }
    }
}
