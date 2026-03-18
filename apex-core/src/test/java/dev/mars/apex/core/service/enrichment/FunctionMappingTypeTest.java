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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the function mapping type within conditional-mapping-enrichment.
 * Tests the ability to invoke an enrichment group with bound input parameters
 * and extract a specific output field.
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class FunctionMappingTypeTest {

    private static final Logger logger = LoggerFactory.getLogger(FunctionMappingTypeTest.class);

    @Test
    @DisplayName("Should create function mapping config structure with new fields")
    void shouldCreateFunctionMappingConfigStructure() {
        logger.info("=== Testing Function Mapping: Model Structure (Programmatic Construction) ===");
        logger.info("Verifying that MappingConfig supports 'function' type with enrichment-group-ref, input-parameters, and output-field");

        // Create a conditional-mapping-enrichment with function mapping type
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-function-mapping");
        enrichment.setType("conditional-mapping-enrichment");
        enrichment.setTargetField("IS_NDF");

        // Create mapping rules with function type
        List<YamlEnrichment.MappingRule> mappingRules = new ArrayList<>();

        YamlEnrichment.MappingRule rule = new YamlEnrichment.MappingRule();
        rule.setId("translate-via-function");
        rule.setPriority(1);

        // Create conditions
        YamlEnrichment.ConditionGroup conditions = new YamlEnrichment.ConditionGroup();
        conditions.setOperator("AND");
        List<YamlEnrichment.ConditionRule> conditionRules = new ArrayList<>();
        YamlEnrichment.ConditionRule condRule = new YamlEnrichment.ConditionRule();
        condRule.setCondition("#IS_NDF != null");
        conditionRules.add(condRule);
        conditions.setRules(conditionRules);
        rule.setConditions(conditions);

        // Create function mapping
        YamlEnrichment.MappingConfig mapping = new YamlEnrichment.MappingConfig();
        mapping.setType("function");
        mapping.setEnrichmentGroupRef("translation-group");

        // Create input parameters
        List<YamlEnrichment.FieldMapping> inputParams = new ArrayList<>();
        YamlEnrichment.FieldMapping param1 = new YamlEnrichment.FieldMapping();
        param1.setSourceField("constant");
        param1.setTargetField("#translation.Translation_Type");
        param1.setExpression("'IS_NDF'");
        inputParams.add(param1);

        YamlEnrichment.FieldMapping param2 = new YamlEnrichment.FieldMapping();
        param2.setSourceField("#client_code");
        param2.setTargetField("#translation.Client_Code");
        inputParams.add(param2);

        mapping.setInputParameters(inputParams);
        mapping.setOutputField("translation_result");

        rule.setMapping(mapping);
        mappingRules.add(rule);
        enrichment.setMappingRules(mappingRules);

        // Verify structure
        logger.info("Verifying enrichment structure...");
        assertNotNull(enrichment);
        assertEquals("conditional-mapping-enrichment", enrichment.getType());
        assertEquals("IS_NDF", enrichment.getTargetField());
        assertEquals(1, enrichment.getMappingRules().size());
        logger.info("[OK] Enrichment id='{}', type='{}', target-field='{}', mapping-rules count={}",
                enrichment.getId(), enrichment.getType(), enrichment.getTargetField(),
                enrichment.getMappingRules().size());

        YamlEnrichment.MappingConfig resultMapping = enrichment.getMappingRules().get(0).getMapping();
        assertEquals("function", resultMapping.getType());
        assertEquals("translation-group", resultMapping.getEnrichmentGroupRef());
        assertEquals("translation_result", resultMapping.getOutputField());
        assertEquals(2, resultMapping.getInputParameters().size());
        assertEquals("constant", resultMapping.getInputParameters().get(0).getSourceField());
        assertEquals("#translation.Translation_Type", resultMapping.getInputParameters().get(0).getTargetField());
        assertEquals("'IS_NDF'", resultMapping.getInputParameters().get(0).getExpression());
        logger.info("[OK] MappingConfig: type='{}', enrichment-group-ref='{}', output-field='{}'",
                resultMapping.getType(), resultMapping.getEnrichmentGroupRef(), resultMapping.getOutputField());
        logger.info("[OK] Input parameters: {} entries — [0] source='{}' target='{}' expr='{}', [1] source='{}' target='{}'",
                resultMapping.getInputParameters().size(),
                resultMapping.getInputParameters().get(0).getSourceField(),
                resultMapping.getInputParameters().get(0).getTargetField(),
                resultMapping.getInputParameters().get(0).getExpression(),
                resultMapping.getInputParameters().get(1).getSourceField(),
                resultMapping.getInputParameters().get(1).getTargetField());
        logger.info("[OK] Programmatic model construction verified — all 3 new MappingConfig fields populated correctly");
    }

    @Test
    @DisplayName("Should deserialize function mapping from YAML")
    void shouldDeserializeFunctionMappingFromYaml() throws Exception {
        logger.info("=== Testing Function Mapping: YAML Deserialization ===");
        logger.info("Verifying Jackson @JsonProperty deserialization of function mapping type fields");

        String yamlConfig = """
            metadata:
              id: "function-mapping-deserialize-test"
              name: "Function Mapping Deserialization Test"
              version: "1.0.0"
              description: "Test YAML deserialization of function mapping type"
              type: "rule-config"

            enrichments:
              - id: "translation-enrichment"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "constant"
                    target-field: "translation_result"
                    expression: "'TRANSLATED_' + #Translation_Type"

              - id: "function-mapping-test"
                type: "conditional-mapping-enrichment"
                target-field: "RESULT_FIELD"
                mapping-rules:
                  - id: "function-rule"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#INPUT_VALUE != null"
                    mapping:
                      type: "function"
                      enrichment-group-ref: "translation-group"
                      input-parameters:
                        - source-field: "constant"
                          target-field: "Translation_Type"
                          expression: "'TEST_TYPE'"
                        - source-field: "#INPUT_VALUE"
                          target-field: "Input_Code"
                      output-field: "translation_result"
                  - id: "direct-fallback"
                    priority: 999
                    mapping:
                      type: "direct"
                      expression: "'FALLBACK'"

            enrichment-groups:
              - id: "translation-group"
                name: "Translation Group"
                enrichment-ids:
                  - "translation-enrichment"
            """;

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
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
        String yamlConfig = """
            metadata:
              id: "function-mapping-e2e-test"
              name: "Function Mapping E2E Test"
              version: "1.0.0"
              description: "End-to-end test of function mapping type"
              type: "rule-config"

            enrichments:
              - id: "translator"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "constant"
                    target-field: "translation_result"
                    expression: "'TRANSLATED_' + #Translation_Type + '_' + #Input_Code"

              - id: "function-cme"
                type: "conditional-mapping-enrichment"
                target-field: "FINAL_RESULT"
                mapping-rules:
                  - id: "translate-rule"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#INPUT_VALUE != null"
                    mapping:
                      type: "function"
                      enrichment-group-ref: "translator-group"
                      input-parameters:
                        - source-field: "constant"
                          target-field: "Translation_Type"
                          expression: "'NDF_TYPE'"
                        - source-field: "#INPUT_VALUE"
                          target-field: "Input_Code"
                      output-field: "translation_result"

            enrichment-groups:
              - id: "translator-group"
                name: "Translator Group"
                enrichment-ids:
                  - "translator"
            """;

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

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

        String yamlConfig = """
            metadata:
              id: "function-mapping-fallback-test"
              name: "Function Mapping Fallback Test"
              version: "1.0.0"
              description: "Test fallback to direct rule when function condition not met"
              type: "rule-config"

            enrichments:
              - id: "translator"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "constant"
                    target-field: "translation_result"
                    expression: "'TRANSLATED'"

              - id: "function-with-fallback"
                type: "conditional-mapping-enrichment"
                target-field: "FINAL_RESULT"
                mapping-rules:
                  - id: "function-rule"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#TRIGGER == 'YES'"
                    mapping:
                      type: "function"
                      enrichment-group-ref: "translator-group"
                      input-parameters:
                        - source-field: "constant"
                          target-field: "Translation_Type"
                          expression: "'TYPE_A'"
                      output-field: "translation_result"
                  - id: "fallback-rule"
                    priority: 999
                    mapping:
                      type: "direct"
                      expression: "'DEFAULT_FALLBACK'"

            enrichment-groups:
              - id: "translator-group"
                name: "Translator Group"
                enrichment-ids:
                  - "translator"
            """;

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

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

        String yamlConfig = """
            metadata:
              id: "missing-group-ref-test"
              name: "Missing Group Ref Test"
              version: "1.0.0"
              description: "Test graceful handling of missing enrichment group reference"
              type: "rule-config"

            enrichments:
              - id: "function-with-bad-ref"
                type: "conditional-mapping-enrichment"
                target-field: "RESULT"
                mapping-rules:
                  - id: "function-rule"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#INPUT != null"
                    mapping:
                      type: "function"
                      enrichment-group-ref: "nonexistent-group"
                      output-field: "some_field"
                  - id: "fallback"
                    priority: 999
                    mapping:
                      type: "direct"
                      expression: "'SAFE_DEFAULT'"

                execution-settings:
                  stop-on-first-match: false
            """;

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

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

        String yamlConfig = """
            metadata:
              id: "mixed-mapping-types-test"
              name: "Mixed Mapping Types Test"
              version: "1.0.0"
              description: "Test mixing direct and function mapping types"
              type: "rule-config"

            enrichments:
              - id: "calculator"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "constant"
                    target-field: "calc_result"
                    expression: "'CALCULATED_' + #calc_input"

              - id: "mixed-mappings"
                type: "conditional-mapping-enrichment"
                target-field: "OUTPUT"
                mapping-rules:
                  - id: "direct-high-priority"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#MODE == 'DIRECT'"
                    mapping:
                      type: "direct"
                      expression: "'DIRECT_RESULT'"
                  - id: "function-medium-priority"
                    priority: 2
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#MODE == 'FUNCTION'"
                    mapping:
                      type: "function"
                      enrichment-group-ref: "calc-group"
                      input-parameters:
                        - source-field: "#INPUT_DATA"
                          target-field: "calc_input"
                      output-field: "calc_result"
                  - id: "default-lowest"
                    priority: 999
                    mapping:
                      type: "direct"
                      expression: "'NONE'"

            enrichment-groups:
              - id: "calc-group"
                name: "Calculation Group"
                enrichment-ids:
                  - "calculator"

                execution-settings:
                  stop-on-first-match: true
            """;

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

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

        String yamlConfig = """
            metadata:
              id: "multi-param-test"
              name: "Multi Parameter Test"
              version: "1.0.0"
              description: "Test multiple input parameters binding"
              type: "rule-config"

            enrichments:
              - id: "multi-field-enrichment"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "constant"
                    target-field: "combined_result"
                    expression: "#param_a + ':' + #param_b + ':' + #param_c"

              - id: "multi-param-function"
                type: "conditional-mapping-enrichment"
                target-field: "COMBINED"
                mapping-rules:
                  - id: "multi-param-rule"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#FIELD_A != null"
                    mapping:
                      type: "function"
                      enrichment-group-ref: "multi-group"
                      input-parameters:
                        - source-field: "#FIELD_A"
                          target-field: "param_a"
                        - source-field: "#FIELD_B"
                          target-field: "param_b"
                        - source-field: "constant"
                          target-field: "param_c"
                          expression: "'CONST_VAL'"
                      output-field: "combined_result"

            enrichment-groups:
              - id: "multi-group"
                name: "Multi Parameter Group"
                enrichment-ids:
                  - "multi-field-enrichment"
            """;

        ConfigurationLoader loader = new ConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

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
}
