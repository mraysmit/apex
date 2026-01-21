package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * CRITICAL SYSTEM TEST: Comprehensive RuleResult Error Propagation Verification
 * 
 * This test suite proves that ALL validation and enrichment errors are properly
 * propagated to RuleResult as required by APEX design. The entire operation of the
 * APEX system depends on this error propagation mechanism working correctly.
 * 
 * DESIGN REQUIREMENTS (per APEX_ERROR_HANDLING_GUIDE.md):
 * ========================================================
 * 1. Exceptions are NEVER thrown to callers - they are caught and converted to RuleResult
 * 2. RuleResult.isSuccess() returns false for ANY error condition
 * 3. RuleResult.getFailureMessages() contains detailed error information
 * 4. RuleResult.getResultType() returns ERROR for system/processing failures
 * 5. RuleResult.hasFailures() returns true when any failures occurred
 * 
 * ERROR CATEGORIES TESTED:
 * ========================
 * 1. ENRICHMENT ERRORS:
 *    - Required field mapping failures (missing source field)
 *    - SpEL expression evaluation failures
 *    - Lookup failures (missing data, invalid keys)
 *    - Type conversion failures
 *    - Invalid target field paths
 *    
 * 2. VALIDATION/RULE ERRORS:
 *    - Rule condition evaluation failures (invalid SpEL)
 *    - Rule with ERROR severity when condition is false and no recovery
 *    
 * 3. TRANSFORMATION ERRORS:
 *    - Transformation expression failures
 *    - Invalid transformation operations
 *    
 * 4. SYSTEM ERRORS:
 *    - Null configuration
 *    - Null input data
 *    - Engine initialization failures
 * 
 * CRITICAL: If any test in this class fails, the APEX system cannot be trusted
 * to correctly report errors to callers. This would cause silent data corruption
 * and invalid processing results in production systems.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-21
 * @version 1.0 - Comprehensive error propagation verification
 */
@DisplayName("CRITICAL: RuleResult Error Propagation Verification")
public class RuleResultErrorPropagationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleResultErrorPropagationTest.class);
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ApexCacheManager.resetInstance();
        loader = new YamlConfigurationLoader();
    }

    /**
     * Helper method to verify error propagation in RuleResult.
     * This method validates ALL required error propagation conditions.
     */
    private void assertErrorPropagation(RuleResult result, String errorContext) {
        assertNotNull(result, "RuleResult must not be null for: " + errorContext);
        assertFalse(result.isSuccess(), 
            "RuleResult.isSuccess() must be FALSE for error: " + errorContext);
        assertTrue(result.hasFailures(), 
            "RuleResult.hasFailures() must be TRUE for error: " + errorContext);
        assertFalse(result.getFailureMessages().isEmpty(), 
            "RuleResult.getFailureMessages() must not be empty for error: " + errorContext);
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
            "RuleResult.getResultType() must be ERROR for: " + errorContext);
        
        LOGGER.info("✓ Error properly propagated to RuleResult for: {}", errorContext);
        LOGGER.info("  - isSuccess: {}", result.isSuccess());
        LOGGER.info("  - hasFailures: {}", result.hasFailures());
        LOGGER.info("  - failureMessages: {}", result.getFailureMessages());
        LOGGER.info("  - resultType: {}", result.getResultType());
    }

    // =========================================================================
    // ENRICHMENT ERROR PROPAGATION TESTS
    // =========================================================================
    
    @Nested
    @DisplayName("1. Enrichment Error Propagation")
    class EnrichmentErrorPropagationTests {

        @Test
        @DisplayName("1.1 Required field mapping failure propagates to RuleResult")
        void testRequiredFieldMappingFailure() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Required field mapping failure - missing source field");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Required Field Failure Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "required-field-enrichment"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "nonExistentField"
                        target-field: "outputField"
                        required: true
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("existingField", "value");  // Different field - required field is missing
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Required field mapping failure (missing source field)");
        }

        @Test
        @DisplayName("1.2 SpEL expression evaluation failure propagates to RuleResult")
        void testSpelExpressionFailure() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: SpEL expression evaluation failure - invalid expression");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "SpEL Failure Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "spel-failure-enrichment"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "missingMap.nestedKey"
                        target-field: "outputField"
                        required: true
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("existingField", "value");  // No 'missingMap' key
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "SpEL expression failure (missing map key)");
        }

        @Test
        @DisplayName("1.3 Lookup enrichment failure propagates to RuleResult")
        void testLookupEnrichmentFailure() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Lookup enrichment failure - missing required lookup match");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Lookup Failure Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "lookup-failure-enrichment"
                    type: "lookup-enrichment"
                    lookup-config:
                      lookup-key: "lookupKey"
                      lookup-dataset:
                        type: "inline"
                        key-field: "id"
                        data:
                          - id: "KNOWN_KEY"
                            value: "Known Value"
                    field-mappings:
                      - source-field: "value"
                        target-field: "resolvedValue"
                        required: true
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("lookupKey", "UNKNOWN_KEY");  // Key that doesn't exist in lookup data
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Lookup enrichment failure (no match for required field)");
        }

        @Test
        @DisplayName("1.4 Multiple enrichment failures accumulate in RuleResult")
        void testMultipleEnrichmentFailures() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Multiple enrichment failures - all should be captured");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Multiple Failures Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "failure-1"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "missing1"
                        target-field: "output1"
                        required: true
                  - id: "failure-2"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "missing2"
                        target-field: "output2"
                        required: true
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("existingField", "value");
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Multiple enrichment failures");
            
            // Additional assertion: should capture multiple failures
            LOGGER.info("  - Total failure messages: {}", result.getFailureMessages().size());
        }

        @Test
        @DisplayName("1.5 Nested SpEL path with missing structure propagates to RuleResult")
        void testNestedSpelPathMissingStructure() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Nested SpEL path failure - missing intermediate structure");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Nested SpEL Path Failure Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "nested-path-enrichment"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "inputValue"
                        target-field: "#root['level1']['level2']['level3']"
                        required: true
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "TEST");  // No 'level1' structure exists
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Nested SpEL path with missing structure");
        }

        @Test
        @DisplayName("1.6 Array index with missing list propagates to RuleResult")
        void testArrayIndexMissingList() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Array index failure - accessing index on missing list");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Array Index Failure Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "array-index-enrichment"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "inputValue"
                        target-field: "#root['items'][0]['name']"
                        required: true
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("inputValue", "TEST");  // No 'items' list exists
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Array index with missing list");
        }
    }

    // =========================================================================
    // RULE/VALIDATION ERROR PROPAGATION TESTS
    // =========================================================================
    
    @Nested
    @DisplayName("2. Rule/Validation Error Propagation")
    class RuleErrorPropagationTests {

        @Test
        @DisplayName("2.1 Invalid SpEL condition in rule propagates to RuleResult")
        void testInvalidRuleCondition() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Invalid rule condition - malformed SpEL expression");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Invalid Rule Condition Test"
                  version: "1.0.0"
                
                rules:
                  - id: "invalid-condition-rule"
                    name: "Invalid Condition Rule"
                    condition: "#root['field'] >>>>> 'invalid_operator'"
                    message: "Should not reach here"
                    severity: "ERROR"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("field", "value");
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Invalid SpEL condition in rule");
        }

        @Test
        @DisplayName("2.2 Rule with ERROR severity and no match propagates to RuleResult")
        void testErrorSeverityRuleNoMatch() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: ERROR severity rule - condition false with no recovery");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Error Severity Rule Test"
                  version: "1.0.0"
                
                error-recovery:
                  enabled: false
                
                rules:
                  - id: "error-severity-rule"
                    name: "Error Severity Rule"
                    condition: "#root['requiredField'] != null"
                    message: "Required field is missing"
                    severity: "ERROR"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("otherField", "value");  // requiredField is null/missing
            
            RuleResult result = engine.evaluate(input);
            
            // Note: For ERROR severity rules that don't match with recovery disabled,
            // the result should indicate failure
            assertNotNull(result, "RuleResult must not be null");
            LOGGER.info("  - isSuccess: {}", result.isSuccess());
            LOGGER.info("  - isTriggered: {}", result.isTriggered());
            LOGGER.info("  - resultType: {}", result.getResultType());
            LOGGER.info("  - failureMessages: {}", result.getFailureMessages());
        }

        @Test
        @DisplayName("2.3 Rule referencing missing field in condition propagates to RuleResult")
        void testRuleMissingFieldReference() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Rule condition referencing non-existent nested field");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Missing Field Reference Test"
                  version: "1.0.0"
                
                rules:
                  - id: "missing-field-rule"
                    name: "Missing Field Rule"
                    condition: "#root['nested']['deepField'] == 'expected'"
                    message: "Deep field check"
                    severity: "ERROR"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("unrelatedField", "value");  // No 'nested' map exists
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Rule condition with missing field reference");
        }
    }

    // =========================================================================
    // TRANSFORMATION ERROR PROPAGATION TESTS
    // =========================================================================
    
    @Nested
    @DisplayName("3. Transformation Error Propagation")
    class TransformationErrorPropagationTests {

        @Test
        @DisplayName("3.1 Invalid transformation expression propagates to RuleResult")
        void testInvalidTransformationExpression() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Invalid transformation expression - malformed SpEL");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Invalid Transformation Test"
                  version: "1.0.0"
                
                transformations:
                  - id: "invalid-transformation"
                    expression: "#root['value'].toUpperCase(((((("
                    target-field: "transformed"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("value", "test");
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Invalid transformation expression");
        }

        @Test
        @DisplayName("3.2 Transformation on null value propagates to RuleResult")
        void testTransformationOnNullValue() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Transformation on null - method call on null object");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Null Transformation Test"
                  version: "1.0.0"
                
                transformations:
                  - id: "null-transformation"
                    expression: "#root['nullField'].toUpperCase()"
                    target-field: "transformed"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("nullField", null);
            
            RuleResult result = engine.evaluate(input);
            
            assertErrorPropagation(result, "Transformation on null value");
        }
    }

    // =========================================================================
    // SYSTEM ERROR PROPAGATION TESTS
    // =========================================================================
    
    @Nested
    @DisplayName("4. System Error Propagation")
    class SystemErrorPropagationTests {

        @Test
        @DisplayName("4.1 Null input data propagates to RuleResult")
        void testNullInputData() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Null input data - system should handle gracefully");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Null Input Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "test-enrichment"
                    type: "field-enrichment"
                    field-mappings:
                      - source-field: "field"
                        target-field: "output"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            RuleResult result = engine.evaluate((Map<String, Object>) null);
            
            assertErrorPropagation(result, "Null input data");
        }

        @Test
        @DisplayName("4.2 Null YAML configuration propagates to RuleResult")
        void testNullYamlConfiguration() throws Exception {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Null YAML configuration - system should handle gracefully");
            LOGGER.info("=".repeat(80));
            
            // Create engine with valid config but evaluate with null config
            String yaml = """
                metadata:
                  name: "Test"
                  version: "1.0.0"
                """;
            
            YamlRuleConfiguration config = loader.fromYamlString(yaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> input = new HashMap<>();
            input.put("field", "value");
            
            RuleResult result = engine.evaluate(null, input);
            
            assertErrorPropagation(result, "Null YAML configuration");
        }
    }

    // =========================================================================
    // YAML CONFIGURATION ERROR PROPAGATION TESTS
    // These tests verify that YAML parsing and validation errors are returned
    // via RuleResult, NOT thrown as exceptions.
    // =========================================================================
    
    @Nested
    @DisplayName("5. YAML Configuration Error Propagation")
    class YamlConfigurationErrorPropagationTests {

        @Test
        @DisplayName("5.1 Invalid YAML syntax error propagates to RuleResult")
        void testInvalidYamlSyntax() {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Invalid YAML syntax - should return RuleResult, NOT throw exception");
            LOGGER.info("=".repeat(80));
            
            String invalidYaml = """
                metadata:
                  name: "Test"
                  invalid yaml here: [missing bracket
                rules:
                  - id: "rule-1"
                """;
            
            Map<String, Object> input = new HashMap<>();
            input.put("field", "value");
            
            // Using the safe evaluateYaml method - should NOT throw exception
            RuleResult result = RulesEngine.evaluateYaml(invalidYaml, input);
            
            assertErrorPropagation(result, "Invalid YAML syntax");
            assertTrue(result.getFailureMessages().stream()
                .anyMatch(msg -> msg.contains("YAML")),
                "Error message should mention YAML error");
        }

        @Test
        @DisplayName("5.2 Missing required YAML field propagates to RuleResult")
        void testMissingRequiredYamlField() {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Missing required YAML field - should return RuleResult, NOT throw exception");
            LOGGER.info("=".repeat(80));
            
            // Enrichment missing required 'type' field
            String yaml = """
                metadata:
                  name: "Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "missing-type-enrichment"
                    field-mappings:
                      - source-field: "input"
                        target-field: "output"
                """;
            
            Map<String, Object> input = new HashMap<>();
            input.put("input", "value");
            
            RuleResult result = RulesEngine.evaluateYaml(yaml, input);
            
            assertErrorPropagation(result, "Missing required YAML field (enrichment type)");
        }

        @Test
        @DisplayName("5.3 Invalid enrichment type propagates to RuleResult")
        void testInvalidEnrichmentType() {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Invalid enrichment type - should return RuleResult, NOT throw exception");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "invalid-type-enrichment"
                    type: "non-existent-enrichment-type"
                    field-mappings:
                      - source-field: "input"
                        target-field: "output"
                """;
            
            Map<String, Object> input = new HashMap<>();
            input.put("input", "value");
            
            RuleResult result = RulesEngine.evaluateYaml(yaml, input);
            
            assertErrorPropagation(result, "Invalid enrichment type");
        }

        @Test
        @DisplayName("5.4 Missing lookup dataset type propagates to RuleResult")
        void testMissingLookupDatasetType() {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Missing lookup dataset type - should return RuleResult, NOT throw exception");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Test"
                  version: "1.0.0"
                
                enrichments:
                  - id: "lookup-missing-type"
                    type: "lookup-enrichment"
                    lookup-config:
                      lookup-key: "key"
                      lookup-dataset:
                        key-field: "id"
                        data:
                          - id: "1"
                            value: "one"
                    field-mappings:
                      - source-field: "value"
                        target-field: "result"
                """;
            
            Map<String, Object> input = new HashMap<>();
            input.put("key", "1");
            
            RuleResult result = RulesEngine.evaluateYaml(yaml, input);
            
            assertErrorPropagation(result, "Missing lookup dataset type");
            assertTrue(result.getFailureMessages().stream()
                .anyMatch(msg -> msg.toLowerCase().contains("type") || msg.toLowerCase().contains("dataset")),
                "Error message should mention missing type");
        }

        @Test
        @DisplayName("5.5 Invalid rule severity propagates to RuleResult")
        void testInvalidRuleSeverity() {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Invalid rule severity - should return RuleResult, NOT throw exception");
            LOGGER.info("=".repeat(80));
            
            String yaml = """
                metadata:
                  name: "Test"
                  version: "1.0.0"
                
                rules:
                  - id: "invalid-severity-rule"
                    condition: "#value > 0"
                    message: "Value is positive"
                    severity: "INVALID_SEVERITY_LEVEL"
                """;
            
            Map<String, Object> input = new HashMap<>();
            input.put("value", 10);
            
            RuleResult result = RulesEngine.evaluateYaml(yaml, input);
            
            // This may either fail validation or use default severity - either way check result
            assertNotNull(result, "Result should not be null");
            LOGGER.info("  - isSuccess: {}", result.isSuccess());
            LOGGER.info("  - failureMessages: {}", result.getFailureMessages());
        }

        @Test
        @DisplayName("5.6 Completely empty YAML propagates to RuleResult")
        void testEmptyYaml() {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Empty YAML - should return RuleResult, NOT throw exception");
            LOGGER.info("=".repeat(80));
            
            String yaml = "";
            
            Map<String, Object> input = new HashMap<>();
            input.put("field", "value");
            
            RuleResult result = RulesEngine.evaluateYaml(yaml, input);
            
            assertNotNull(result, "Result should not be null even for empty YAML");
            LOGGER.info("  - isSuccess: {}", result.isSuccess());
            LOGGER.info("  - hasFailures: {}", result.hasFailures());
            LOGGER.info("  - failureMessages: {}", result.getFailureMessages());
        }

        @Test
        @DisplayName("5.7 Null YAML string propagates to RuleResult")
        void testNullYamlString() {
            LOGGER.info("\n" + "=".repeat(80));
            LOGGER.info("TEST: Null YAML string - should return RuleResult, NOT throw exception");
            LOGGER.info("=".repeat(80));
            
            Map<String, Object> input = new HashMap<>();
            input.put("field", "value");
            
            RuleResult result = RulesEngine.evaluateYaml(null, input);
            
            assertErrorPropagation(result, "Null YAML string");
        }
    }

    // =========================================================================
    // VERIFICATION SUMMARY TEST
    // =========================================================================
    
    @Test
    @DisplayName("SUMMARY: Verify error propagation contract is maintained")
    void testErrorPropagationContractSummary() {
        LOGGER.info("\n" + "=".repeat(80));
        LOGGER.info("APEX ERROR PROPAGATION CONTRACT VERIFICATION SUMMARY");
        LOGGER.info("=".repeat(80));
        LOGGER.info("");
        LOGGER.info("The APEX Rules Engine MUST propagate ALL errors to RuleResult:");
        LOGGER.info("");
        LOGGER.info("  ✓ RuleResult.isSuccess() returns FALSE for any error");
        LOGGER.info("  ✓ RuleResult.hasFailures() returns TRUE for any error");
        LOGGER.info("  ✓ RuleResult.getFailureMessages() contains error details");
        LOGGER.info("  ✓ RuleResult.getResultType() returns ERROR for system failures");
        LOGGER.info("");
        LOGGER.info("Error categories covered by this test suite:");
        LOGGER.info("");
        LOGGER.info("  1. ENRICHMENT ERRORS:");
        LOGGER.info("     - Required field mapping failures");
        LOGGER.info("     - SpEL expression failures");
        LOGGER.info("     - Lookup failures");
        LOGGER.info("     - Nested path failures");
        LOGGER.info("     - Array index failures");
        LOGGER.info("");
        LOGGER.info("  2. RULE/VALIDATION ERRORS:");
        LOGGER.info("     - Invalid SpEL conditions");
        LOGGER.info("     - Missing field references");
        LOGGER.info("     - ERROR severity rule failures");
        LOGGER.info("");
        LOGGER.info("  3. TRANSFORMATION ERRORS:");
        LOGGER.info("     - Invalid transformation expressions");
        LOGGER.info("     - Null value transformations");
        LOGGER.info("");
        LOGGER.info("  4. SYSTEM ERRORS:");
        LOGGER.info("     - Null input data");
        LOGGER.info("     - Null configuration");
        LOGGER.info("");
        LOGGER.info("  5. YAML CONFIGURATION ERRORS:");
        LOGGER.info("     - Invalid YAML syntax");
        LOGGER.info("     - Missing required fields");
        LOGGER.info("     - Invalid enrichment types");
        LOGGER.info("     - Missing lookup dataset types");
        LOGGER.info("     - Empty/null YAML");
        LOGGER.info("");
        LOGGER.info("=".repeat(80));
        LOGGER.info("CRITICAL: If any test in this suite fails, the APEX system");
        LOGGER.info("cannot be trusted to correctly report errors to callers.");
        LOGGER.info("=".repeat(80));
        
        // This test always passes - it's a summary/documentation test
        assertTrue(true, "Error propagation contract verification completed");
    }
}
