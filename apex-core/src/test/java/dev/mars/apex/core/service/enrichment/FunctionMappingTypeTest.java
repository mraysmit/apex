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

package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the function mapping type within conditional-mapping-enrichment.
 * Tests the ability to invoke an enrichment group with bound input parameters
 * and extract a specific output field.
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class FunctionMappingTypeTest {

    private static final String YAML_PATH = "src/test/resources/dev/mars/apex/core/service/enrichment/";

    private static final Logger logger = LoggerFactory.getLogger(FunctionMappingTypeTest.class);

    @Test
    @DisplayName("Should deserialize function mapping from YAML")
    void shouldDeserializeFunctionMappingFromYaml() throws Exception {
        logger.info("=== Testing Function Mapping: YAML Deserialization ===");
        logger.info("Verifying Jackson @JsonProperty deserialization of function mapping type fields");

        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-deserialize.yaml");
        logger.info("[OK] YAML parsed successfully — {} enrichments, {} enrichment-groups",
                config.getEnrichments().size(),
                config.getEnrichmentGroups() != null ? config.getEnrichmentGroups().size() : 0);

        assertNotNull(config);
        assertNotNull(config.getEnrichments());
        assertEquals(2, config.getEnrichments().size());

        // Verify the conditional-mapping-enrichment deserialized correctly
        YamlEnrichment cme = config.getEnrichments().stream()
                .filter(e -> "function-mapping-test".equals(e.getId()))
                .findFirst().orElseThrow();

        assertEquals("conditional-mapping-enrichment", cme.getType());
        assertEquals("RESULT_FIELD", cme.getTargetField());
        assertEquals(2, cme.getMappingRules().size());

        // Verify function mapping rule
        YamlEnrichment.MappingRule functionRule = cme.getMappingRules().get(0);
        assertEquals("function-rule", functionRule.getId());
        assertEquals(1, functionRule.getPriority());

        YamlEnrichment.MappingConfig mapping = functionRule.getMapping();
        assertEquals("function", mapping.getType());
        assertEquals("translation-group", mapping.getEnrichmentGroupRef());
        assertEquals("translation_result", mapping.getOutputField());

        assertNotNull(mapping.getInputParameters());
        assertEquals(2, mapping.getInputParameters().size());
        assertEquals("constant", mapping.getInputParameters().get(0).getSourceField());
        assertEquals("Translation_Type", mapping.getInputParameters().get(0).getTargetField());
        assertEquals("'TEST_TYPE'", mapping.getInputParameters().get(0).getExpression());
        assertEquals("#INPUT_VALUE", mapping.getInputParameters().get(1).getSourceField());
        assertEquals("Input_Code", mapping.getInputParameters().get(1).getTargetField());
        logger.info("[OK] Function rule deserialized: type='{}', group-ref='{}', output-field='{}', input-params={}",
                mapping.getType(), mapping.getEnrichmentGroupRef(), mapping.getOutputField(),
                mapping.getInputParameters().size());

        // Verify direct fallback rule still works
        YamlEnrichment.MappingRule directRule = cme.getMappingRules().get(1);
        assertEquals("direct", directRule.getMapping().getType());
        logger.info("[OK] Fallback rule deserialized: type='{}' — existing mapping types preserved alongside function",
                directRule.getMapping().getType());
        logger.info("[OK] All @JsonProperty fields deserialized correctly from YAML text block");
    }

    @Test
    @DisplayName("Should execute function mapping end-to-end via RulesEngine")
    void shouldExecuteFunctionMappingEndToEnd() throws Exception {
        logger.info("=== Testing Function Mapping: End-to-End Execution ===");
        logger.info("Full flow: condition evaluation -> input parameter binding -> group execution -> output extraction");

        // This test verifies the full flow:
        // 1. Conditional mapping evaluates condition
        // 2. Function mapping binds input parameters
        // 3. Enrichment group executes (field-enrichment writes translation_result)
        // 4. Output field extracted and written to target-field
        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-e2e.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("INPUT_VALUE", "ABC123");
        logger.info("Input data: {}", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("[OK] Enrichment completed. Result keys: {}", enrichedData.keySet());
        logger.info("[OK] Full enriched data: {}", enrichedData);

        assertNotNull(enrichedData);
        // Expect: input-parameters set Translation_Type='NDF_TYPE' and Input_Code='ABC123',
        // then 'translator' group produces 'TRANSLATED_NDF_TYPE_ABC123', extracted via output-field
        assertEquals("TRANSLATED_NDF_TYPE_ABC123", enrichedData.get("FINAL_RESULT"),
                "Function mapping should invoke enrichment group and extract output-field");
        logger.info("[OK] FINAL_RESULT='{}' — function mapping end-to-end chain verified",
                enrichedData.get("FINAL_RESULT"));
    }

    @Test
    @DisplayName("Should fall back to direct rule when function mapping condition not met")
    void shouldFallBackToDirectRuleWhenConditionNotMet() throws Exception {
        logger.info("=== Testing Function Mapping: Priority Fallback ===");
        logger.info("Function rule (priority 1) condition will NOT match -> fallback direct rule (priority 999) should fire");

        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-fallback.yaml");

        // Test data does NOT have TRIGGER='YES', so function rule should not match
        Map<String, Object> testData = new HashMap<>();
        testData.put("TRIGGER", "NO");
        logger.info("Input data: {} — TRIGGER='NO' does not satisfy condition #TRIGGER == 'YES'", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("[OK] Enrichment completed. Full result: {}", enrichedData);

        assertNotNull(enrichedData);
        assertEquals("DEFAULT_FALLBACK", enrichedData.get("FINAL_RESULT"),
                "Should fall back to direct mapping when function condition not met");
        logger.info("[OK] FINAL_RESULT='{}' — function rule skipped, direct fallback applied as expected",
                enrichedData.get("FINAL_RESULT"));
    }

    @Test
    @DisplayName("Should handle missing enrichment-group-ref gracefully")
    void shouldHandleMissingEnrichmentGroupRefGracefully() throws Exception {
        logger.info("=== Testing Function Mapping: Missing Group Reference (Error Handling) ===");
        logger.info("enrichment-group-ref points to 'nonexistent-group' — function should return null, fallback should apply");

        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-missing-group.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("INPUT", "something");
        logger.info("Input data: {}", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("[OK] Enrichment completed (no exception thrown). Full result: {}", enrichedData);

        // Function mapping should fail gracefully (return null) and fallback should apply
        assertNotNull(enrichedData);
        assertEquals("SAFE_DEFAULT", enrichedData.get("RESULT"),
                "Fallback rule should apply when function mapping group ref not found");
        logger.info("[OK] RESULT='{}' — missing group ref handled gracefully, fallback applied",
                enrichedData.get("RESULT"));
    }

    @Test
    @DisplayName("Should mix direct and function mapping types in same enrichment")
    void shouldMixDirectAndFunctionMappingTypes() throws Exception {
        logger.info("=== Testing Function Mapping: Mixed Mapping Types in Priority Chain ===");
        logger.info("Three rules: direct (priority 1), function (priority 2), default (priority 999)");
        logger.info("Sub-test 1: MODE='DIRECT' -> direct rule wins; Sub-test 2: MODE='FUNCTION' -> function rule invokes group");

        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-mixed.yaml");

        // Test 1: Direct mode should pick direct mapping
        Map<String, Object> directData = new HashMap<>();
        directData.put("MODE", "DIRECT");
        directData.put("INPUT_DATA", "ignored");
        logger.info("Sub-test 1 — Input data: {}", directData);

        RulesEngine engine1 = RulesEngine.fromYamlConfig(config);
        RuleResult directResult = engine1.evaluate(directData);
        logger.info("[OK] Sub-test 1 result: {}", directResult.getEnrichedData());
        assertEquals("DIRECT_RESULT", directResult.getEnrichedData().get("OUTPUT"),
                "Direct mode should use direct mapping");
        logger.info("[OK] OUTPUT='{}' — direct rule matched at priority 1, function rule not reached",
                directResult.getEnrichedData().get("OUTPUT"));

        // Test 2: Function mode should invoke enrichment group
        Map<String, Object> functionData = new HashMap<>();
        functionData.put("MODE", "FUNCTION");
        functionData.put("INPUT_DATA", "XYZ");
        logger.info("Sub-test 2 — Input data: {}", functionData);

        RulesEngine engine2 = RulesEngine.fromYamlConfig(config);
        RuleResult functionResult = engine2.evaluate(functionData);
        logger.info("[OK] Sub-test 2 result: {}", functionResult.getEnrichedData());
        assertEquals("CALCULATED_XYZ", functionResult.getEnrichedData().get("OUTPUT"),
                "Function mode should invoke enrichment group and extract result");
        logger.info("[OK] OUTPUT='{}' — direct rule skipped (MODE != 'DIRECT'), function rule matched at priority 2",
                functionResult.getEnrichedData().get("OUTPUT"));
    }

    @Test
    @DisplayName("Should pass multiple input parameters to function mapping")
    void shouldPassMultipleInputParameters() throws Exception {
        logger.info("=== Testing Function Mapping: Multiple Input Parameter Binding ===");
        logger.info("Three input-parameters: #FIELD_A -> param_a, #FIELD_B -> param_b, constant -> param_c='CONST_VAL'");

        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-multi-param.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("FIELD_A", "alpha");
        testData.put("FIELD_B", "beta");
        logger.info("Input data: {} — FIELD_A='alpha', FIELD_B='beta', no FIELD_C (constant param)", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("[OK] Enrichment completed. Full result: {}", enrichedData);

        assertNotNull(enrichedData);
        // Expected: param_a='alpha' (from #FIELD_A), param_b='beta' (from #FIELD_B),
        // param_c='CONST_VAL' (constant expression) -> concatenated as 'alpha:beta:CONST_VAL'
        assertEquals("alpha:beta:CONST_VAL", enrichedData.get("COMBINED"),
                "All input parameters should be bound and available to the enrichment group");
        logger.info("[OK] COMBINED='{}' — all 3 input parameters (2 dynamic + 1 constant) bound correctly",
                enrichedData.get("COMBINED"));
    }

    @Test
    @DisplayName("Should trigger recursion depth guard and not throw StackOverflowError")
    void shouldHandleRecursionDepthGuard() throws Exception {
        logger.info("=== Testing Function Mapping: Recursion Depth Guard ===");
        logger.info("Self-referencing CME: function-rule -> recursive-group -> recursive-cme (same CME)");
        logger.info("MAX_FUNCTION_MAPPING_DEPTH=5 — guard should fire, no StackOverflowError, fallback applies");

        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-recursion.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("RECURSE", true);
        logger.info("Input data: {} — RECURSE=true triggers the recursive function rule", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        // Key assertion: must complete without StackOverflowError
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();

        assertNotNull(enrichedData, "Engine should complete without exception");
        assertEquals("DEPTH_GUARD_TRIGGERED", enrichedData.get("RECURSION_RESULT"),
                "Depth guard should fire at depth 5, leaf-level fallback propagates back as result");
        logger.info("[OK] RECURSION_RESULT='{}' — depth guard fired, no StackOverflowError, fallback applied",
                enrichedData.get("RECURSION_RESULT"));
    }

    @Test
    @DisplayName("Should return null gracefully when output-field is absent after group execution")
    void shouldHandleMissingOutputFieldGracefully() throws Exception {
        logger.info("=== Testing Function Mapping: Missing Output Field ===");
        logger.info("Enrichment group writes to 'actual_output'; function mapping requests 'nonexistent_output_field'");
        logger.info("getFieldValue returns null -> function mapping returns null -> fallback applies");

        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(YAML_PATH + "FunctionMappingTypeTest-missing-output-field.yaml");

        Map<String, Object> testData = new HashMap<>();
        testData.put("TRIGGER", true);
        logger.info("Input data: {} — TRIGGER=true fires the function rule", testData);

        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> enrichedData = result.getEnrichedData();
        logger.info("[OK] Enrichment completed (no exception thrown). Full result: {}", enrichedData);

        assertNotNull(enrichedData);
        assertEquals("FALLBACK_APPLIED", enrichedData.get("RESULT"),
                "When output-field is absent after group execution, function returns null and fallback should apply");
        logger.info("[OK] RESULT='{}' — absent output-field handled gracefully, fallback applied",
                enrichedData.get("RESULT"));
    }
}
