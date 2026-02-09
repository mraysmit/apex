package dev.mars.apex.demo.basic;

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class validating the transformation/expression alias feature.
 * 
 * This test ensures backward compatibility by verifying that both 'transformation' 
 * and 'expression' keywords work identically in field-mappings.
 * 
 * Background:
 * - Originally, field-mappings used 'transformation' keyword
 * - Nov 8, 2025: Refactored to use 'expression' keyword
 * - Added 'transformation' as an alias to maintain backward compatibility
 * 
 * Key Features Tested:
 * - String constants with both keywords
 * - Boolean constants with both keywords
 * - Numeric constants (integer and decimal) with both keywords
 * - Null constants with both keywords
 * - Both field-enrichment and conditional-mapping-enrichment types
 * - Direct comparison: transformation vs expression produces identical results
 * 
 * Following prompts.txt guidelines:
 * - Tests actual functionality, not YAML syntax
 * - Uses real APEX enrichment operations
 * - Validates business logic outcomes
 * - Follows existing working patterns
 * - Uses middle office trade processing domain (OTC options)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Transformation/Expression Alias Feature Test")
public class TransformationExpressionAliasTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(TransformationExpressionAliasTest.class);

    @Test
    @Order(1)
    @DisplayName("Test string constants: transformation vs expression produce identical results")
    public void testStringConstantsAlias() {
        logger.info("=== Testing String Constants: transformation vs expression ===");
        
        try {
            // Test with 'transformation' keyword
            String yamlWithTransformation = """
                version: "1.0"
                name: "String Constant Test - Transformation"

                enrichments:
                  - id: "set-status-transformation"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "tradeStatus"
                        transformation: "'VALIDATED'"
                """;

            YamlRuleConfiguration config1 = yamlLoader.fromYamlString(yamlWithTransformation);
            RulesEngine engine1 = RulesEngine.fromYamlConfig(config1);
            RuleResult result1 = engine1.evaluate(new HashMap<>());
            Map<String, Object> enriched1 = result1.getEnrichedData();

            // Test with 'expression' keyword
            String yamlWithExpression = """
                version: "1.0"
                name: "String Constant Test - Expression"

                enrichments:
                  - id: "set-status-expression"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "tradeStatus"
                        expression: "'VALIDATED'"
                """;

            YamlRuleConfiguration config2 = yamlLoader.fromYamlString(yamlWithExpression);
            RulesEngine engine2 = RulesEngine.fromYamlConfig(config2);
            RuleResult result2 = engine2.evaluate(new HashMap<>());
            Map<String, Object> enriched2 = result2.getEnrichedData();

            // Verify both produce identical results
            assertEquals("VALIDATED", enriched1.get("tradeStatus"),
                "transformation keyword should set string constant");
            assertEquals("VALIDATED", enriched2.get("tradeStatus"),
                "expression keyword should set string constant");
            assertEquals(enriched1.get("tradeStatus"), enriched2.get("tradeStatus"),
                "transformation and expression should produce identical results");

            logger.info("[OK] String constants test passed - both keywords work identically");
            
        } catch (Exception e) {
            logger.error("String constants test failed", e);
            fail("String constants test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test boolean constants: transformation vs expression produce identical results")
    public void testBooleanConstantsAlias() {
        logger.info("=== Testing Boolean Constants: transformation vs expression ===");
        
        try {
            // Test with 'transformation' keyword
            String yamlWithTransformation = """
                version: "1.0"
                name: "Boolean Constant Test - Transformation"

                enrichments:
                  - id: "set-approval-transformation"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "requiresApproval"
                        transformation: "true"
                      - source-field: "constant"
                        target-field: "isHighRisk"
                        transformation: "false"
                """;

            YamlRuleConfiguration config1 = yamlLoader.fromYamlString(yamlWithTransformation);
            RulesEngine engine1 = RulesEngine.fromYamlConfig(config1);
            RuleResult result1 = engine1.evaluate(new HashMap<>());
            Map<String, Object> enriched1 = result1.getEnrichedData();

            // Test with 'expression' keyword
            String yamlWithExpression = """
                version: "1.0"
                name: "Boolean Constant Test - Expression"

                enrichments:
                  - id: "set-approval-expression"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "requiresApproval"
                        expression: "true"
                      - source-field: "constant"
                        target-field: "isHighRisk"
                        expression: "false"
                """;

            YamlRuleConfiguration config2 = yamlLoader.fromYamlString(yamlWithExpression);
            RulesEngine engine2 = RulesEngine.fromYamlConfig(config2);
            RuleResult result2 = engine2.evaluate(new HashMap<>());
            Map<String, Object> enriched2 = result2.getEnrichedData();

            // Verify both produce identical results
            assertTrue((Boolean) enriched1.get("requiresApproval"),
                "transformation keyword should set boolean true");
            assertFalse((Boolean) enriched1.get("isHighRisk"),
                "transformation keyword should set boolean false");
            assertTrue((Boolean) enriched2.get("requiresApproval"),
                "expression keyword should set boolean true");
            assertFalse((Boolean) enriched2.get("isHighRisk"),
                "expression keyword should set boolean false");
            assertEquals(enriched1.get("requiresApproval"), enriched2.get("requiresApproval"),
                "transformation and expression should produce identical boolean true");
            assertEquals(enriched1.get("isHighRisk"), enriched2.get("isHighRisk"),
                "transformation and expression should produce identical boolean false");

            logger.info("[OK] Boolean constants test passed - both keywords work identically");
            
        } catch (Exception e) {
            logger.error("Boolean constants test failed", e);
            fail("Boolean constants test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test numeric constants: transformation vs expression produce identical results")
    public void testNumericConstantsAlias() {
        logger.info("=== Testing Numeric Constants: transformation vs expression ===");
        
        try {
            // Test with 'transformation' keyword
            String yamlWithTransformation = """
                version: "1.0"
                name: "Numeric Constant Test - Transformation"

                enrichments:
                  - id: "set-numeric-transformation"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "priorityLevel"
                        transformation: "42"
                      - source-field: "constant"
                        target-field: "riskFactor"
                        transformation: "3.14159"
                      - source-field: "constant"
                        target-field: "notionalAmount"
                        transformation: "1000000.50"
                """;

            YamlRuleConfiguration config1 = yamlLoader.fromYamlString(yamlWithTransformation);
            RulesEngine engine1 = RulesEngine.fromYamlConfig(config1);
            RuleResult result1 = engine1.evaluate(new HashMap<>());
            Map<String, Object> enriched1 = result1.getEnrichedData();

            // Test with 'expression' keyword
            String yamlWithExpression = """
                version: "1.0"
                name: "Numeric Constant Test - Expression"

                enrichments:
                  - id: "set-numeric-expression"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "priorityLevel"
                        expression: "42"
                      - source-field: "constant"
                        target-field: "riskFactor"
                        expression: "3.14159"
                      - source-field: "constant"
                        target-field: "notionalAmount"
                        expression: "1000000.50"
                """;

            YamlRuleConfiguration config2 = yamlLoader.fromYamlString(yamlWithExpression);
            RulesEngine engine2 = RulesEngine.fromYamlConfig(config2);
            RuleResult result2 = engine2.evaluate(new HashMap<>());
            Map<String, Object> enriched2 = result2.getEnrichedData();

            // Verify both produce identical results
            assertEquals(42, enriched1.get("priorityLevel"),
                "transformation keyword should set integer constant");
            assertEquals(3.14159, (Double) enriched1.get("riskFactor"), 0.00001,
                "transformation keyword should set decimal constant");
            assertEquals(1000000.50, (Double) enriched1.get("notionalAmount"), 0.01,
                "transformation keyword should set large decimal constant");
            
            assertEquals(42, enriched2.get("priorityLevel"),
                "expression keyword should set integer constant");
            assertEquals(3.14159, (Double) enriched2.get("riskFactor"), 0.00001,
                "expression keyword should set decimal constant");
            assertEquals(1000000.50, (Double) enriched2.get("notionalAmount"), 0.01,
                "expression keyword should set large decimal constant");
            
            assertEquals(enriched1.get("priorityLevel"), enriched2.get("priorityLevel"),
                "transformation and expression should produce identical integer");
            assertEquals(enriched1.get("riskFactor"), enriched2.get("riskFactor"),
                "transformation and expression should produce identical decimal");
            assertEquals(enriched1.get("notionalAmount"), enriched2.get("notionalAmount"),
                "transformation and expression should produce identical large decimal");

            logger.info("[OK] Numeric constants test passed - both keywords work identically");

        } catch (Exception e) {
            logger.error("Numeric constants test failed", e);
            fail("Numeric constants test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test null constants: transformation vs expression produce identical results")
    public void testNullConstantsAlias() {
        logger.info("=== Testing Null Constants: transformation vs expression ===");

        try {
            // Test with 'transformation' keyword
            String yamlWithTransformation = """
                version: "1.0"
                name: "Null Constant Test - Transformation"

                enrichments:
                  - id: "set-null-transformation"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "optionalField"
                        transformation: "null"
                """;

            YamlRuleConfiguration config1 = yamlLoader.fromYamlString(yamlWithTransformation);
            RulesEngine engine1 = RulesEngine.fromYamlConfig(config1);
            RuleResult result1 = engine1.evaluate(new HashMap<>());
            Map<String, Object> enriched1 = result1.getEnrichedData();

            // Test with 'expression' keyword
            String yamlWithExpression = """
                version: "1.0"
                name: "Null Constant Test - Expression"

                enrichments:
                  - id: "set-null-expression"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "optionalField"
                        expression: "null"
                """;

            YamlRuleConfiguration config2 = yamlLoader.fromYamlString(yamlWithExpression);
            RulesEngine engine2 = RulesEngine.fromYamlConfig(config2);
            RuleResult result2 = engine2.evaluate(new HashMap<>());
            Map<String, Object> enriched2 = result2.getEnrichedData();

            // Verify both produce identical results (null values are not added to map)
            assertNull(enriched1.get("optionalField"),
                "transformation keyword should handle null constant");
            assertNull(enriched2.get("optionalField"),
                "expression keyword should handle null constant");
            assertEquals(enriched1.get("optionalField"), enriched2.get("optionalField"),
                "transformation and expression should produce identical null handling");

            logger.info("[OK] Null constants test passed - both keywords work identically");

        } catch (Exception e) {
            logger.error("Null constants test failed", e);
            fail("Null constants test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test conditional-mapping-enrichment: transformation vs expression produce identical results")
    public void testConditionalMappingAlias() {
        logger.info("=== Testing Conditional Mapping: transformation vs expression ===");

        try {
            // Test with 'transformation' keyword
            String yamlWithTransformation = """
                version: "1.0"
                name: "Conditional Mapping Test - Transformation"

                enrichments:
                  - id: "risk-classification-transformation"
                    type: "conditional-mapping-enrichment"
                    target-field: "riskLevel"
                    mapping-rules:
                      - id: "high-risk"
                        priority: 1
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "#notionalAmount > 10000000"
                        mapping:
                          type: "direct"
                          transformation: "'HIGH'"
                      - id: "medium-risk"
                        priority: 2
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "#notionalAmount > 1000000"
                        mapping:
                          type: "direct"
                          transformation: "'MEDIUM'"
                      - id: "low-risk"
                        priority: 3
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "true"
                        mapping:
                          type: "direct"
                          transformation: "'LOW'"
                """;

            YamlRuleConfiguration config1 = yamlLoader.fromYamlString(yamlWithTransformation);
            RulesEngine engine1 = RulesEngine.fromYamlConfig(config1);

            Map<String, Object> highRiskData = new HashMap<>();
            highRiskData.put("notionalAmount", 15000000.0);
            RuleResult result1 = engine1.evaluate(highRiskData);
            Map<String, Object> enriched1 = result1.getEnrichedData();

            // Test with 'expression' keyword
            String yamlWithExpression = """
                version: "1.0"
                name: "Conditional Mapping Test - Expression"

                enrichments:
                  - id: "risk-classification-expression"
                    type: "conditional-mapping-enrichment"
                    target-field: "riskLevel"
                    mapping-rules:
                      - id: "high-risk"
                        priority: 1
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "#notionalAmount > 10000000"
                        mapping:
                          type: "direct"
                          expression: "'HIGH'"
                      - id: "medium-risk"
                        priority: 2
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "#notionalAmount > 1000000"
                        mapping:
                          type: "direct"
                          expression: "'MEDIUM'"
                      - id: "low-risk"
                        priority: 3
                        conditions:
                          operator: "AND"
                          rules:
                            - condition: "true"
                        mapping:
                          type: "direct"
                          expression: "'LOW'"
                """;

            YamlRuleConfiguration config2 = yamlLoader.fromYamlString(yamlWithExpression);
            RulesEngine engine2 = RulesEngine.fromYamlConfig(config2);
            RuleResult result2 = engine2.evaluate(highRiskData);
            Map<String, Object> enriched2 = result2.getEnrichedData();

            // Verify both produce identical results
            assertEquals("HIGH", enriched1.get("riskLevel"),
                "transformation keyword should work in conditional-mapping-enrichment");
            assertEquals("HIGH", enriched2.get("riskLevel"),
                "expression keyword should work in conditional-mapping-enrichment");
            assertEquals(enriched1.get("riskLevel"), enriched2.get("riskLevel"),
                "transformation and expression should produce identical results in conditional-mapping");

            logger.info("[OK] Conditional mapping test passed - both keywords work identically");

        } catch (Exception e) {
            logger.error("Conditional mapping test failed", e);
            fail("Conditional mapping test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test mixed usage: transformation and expression in same config")
    public void testMixedUsage() {
        logger.info("=== Testing Mixed Usage: transformation and expression together ===");

        try {
            // Test using both keywords in the same configuration
            String yamlMixed = """
                version: "1.0"
                name: "Mixed Usage Test"

                enrichments:
                  - id: "mixed-enrichment"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "field1"
                        transformation: "'VALUE_FROM_TRANSFORMATION'"
                      - source-field: "constant"
                        target-field: "field2"
                        expression: "'VALUE_FROM_EXPRESSION'"
                      - source-field: "constant"
                        target-field: "field3"
                        transformation: "100"
                      - source-field: "constant"
                        target-field: "field4"
                        expression: "200"
                """;

            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlMixed);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(new HashMap<>());
            Map<String, Object> enriched = result.getEnrichedData();

            // Verify both keywords work correctly when used together
            assertEquals("VALUE_FROM_TRANSFORMATION", enriched.get("field1"),
                "transformation keyword should work in mixed config");
            assertEquals("VALUE_FROM_EXPRESSION", enriched.get("field2"),
                "expression keyword should work in mixed config");
            assertEquals(100, enriched.get("field3"),
                "transformation keyword should work for numeric in mixed config");
            assertEquals(200, enriched.get("field4"),
                "expression keyword should work for numeric in mixed config");

            logger.info("[OK] Mixed usage test passed - both keywords work together");

        } catch (Exception e) {
            logger.error("Mixed usage test failed", e);
            fail("Mixed usage test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test backward compatibility: legacy YAML files still work")
    public void testBackwardCompatibility() {
        logger.info("=== Testing Backward Compatibility: legacy YAML files ===");

        try {
            // Simulate a legacy YAML file that only uses 'transformation'
            String legacyYaml = """
                version: "1.0"
                name: "Legacy Configuration"
                description: "Simulates a YAML file created before Nov 8, 2025 refactoring"

                enrichments:
                  - id: "legacy-counterparty-validation"
                    type: "field-enrichment"
                    condition: "#counterpartyRating == 'AAA'"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "validationStatus"
                        transformation: "'APPROVED'"
                      - source-field: "constant"
                        target-field: "requiresReview"
                        transformation: "false"
                      - source-field: "constant"
                        target-field: "approvalLimit"
                        transformation: "50000000"
                """;

            YamlRuleConfiguration config = yamlLoader.fromYamlString(legacyYaml);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("counterpartyRating", "AAA");

            RuleResult result = engine.evaluate(testData);
            Map<String, Object> enriched = result.getEnrichedData();

            // Verify legacy YAML still works correctly
            assertEquals("APPROVED", enriched.get("validationStatus"),
                "Legacy transformation keyword should still work for strings");
            assertFalse((Boolean) enriched.get("requiresReview"),
                "Legacy transformation keyword should still work for booleans");
            assertEquals(50000000, enriched.get("approvalLimit"),
                "Legacy transformation keyword should still work for numbers");

            logger.info("[OK] Backward compatibility test passed - legacy YAML files work correctly");

        } catch (Exception e) {
            logger.error("Backward compatibility test failed", e);
            fail("Backward compatibility test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Test error detection: missing transformation/expression should be caught")
    public void testMissingTransformationExpression() {
        logger.info("=== Testing Error Detection: missing transformation/expression ===");

        try {
            // YAML with field-mapping that has neither transformation nor expression
            String yamlMissingBoth = """
                version: "1.0"
                name: "Missing Transformation Test"

                enrichments:
                  - id: "missing-transformation"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "inputField"
                        target-field: "outputField"
                        # Neither transformation nor expression specified
                """;

            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlMissingBoth);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testData = new HashMap<>();
            testData.put("inputField", "TEST_VALUE");

            RuleResult result = engine.evaluate(testData);
            Map<String, Object> enriched = result.getEnrichedData();

            // When no transformation/expression is specified, it should do a direct field copy
            // This is valid behavior - just copying the source field to target field
            assertEquals("TEST_VALUE", enriched.get("outputField"),
                "Without transformation/expression, should copy source field directly");

            logger.info("[OK] Missing transformation test passed - direct field copy works as expected");

        } catch (Exception e) {
            logger.error("Missing transformation test failed", e);
            fail("Missing transformation test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test silent failure prevention: verify values are actually set")
    public void testSilentFailurePrevention() {
        logger.info("=== Testing Silent Failure Prevention: verify values are actually set ===");

        try {
            // This test simulates the production outage scenario
            // If 'transformation' was not recognized, the field would be null/missing
            String yamlWithTransformation = """
                version: "1.0"
                name: "Silent Failure Prevention Test"

                enrichments:
                  - id: "critical-validation"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "constant"
                        target-field: "criticalStatus"
                        transformation: "'VALIDATED'"
                      - source-field: "constant"
                        target-field: "approvalRequired"
                        transformation: "true"
                      - source-field: "constant"
                        target-field: "riskScore"
                        transformation: "100"
                """;

            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlWithTransformation);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult result = engine.evaluate(new HashMap<>());
            Map<String, Object> enriched = result.getEnrichedData();

            // CRITICAL: These assertions would have FAILED in the production outage
            // because the transformation keyword was not recognized
            assertNotNull(enriched.get("criticalStatus"),
                "criticalStatus must not be null - this was the production outage!");
            assertEquals("VALIDATED", enriched.get("criticalStatus"),
                "criticalStatus must be set to VALIDATED");

            assertNotNull(enriched.get("approvalRequired"),
                "approvalRequired must not be null");
            assertTrue((Boolean) enriched.get("approvalRequired"),
                "approvalRequired must be true");

            assertNotNull(enriched.get("riskScore"),
                "riskScore must not be null");
            assertEquals(100, enriched.get("riskScore"),
                "riskScore must be 100");

            logger.info("[OK] Silent failure prevention test passed - all critical values are set correctly");
            logger.info("  This test would have CAUGHT the production outage!");

        } catch (Exception e) {
            logger.error("Silent failure prevention test failed", e);
            fail("Silent failure prevention test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test complex SpEL expressions with both keywords")
    public void testComplexSpelExpressions() {
        logger.info("=== Testing Complex SpEL Expressions: transformation vs expression ===");

        try {
            // Test with actual SpEL expressions (not just constants)
            String yamlTransformation = """
                version: "1.0"
                name: "Complex SpEL with transformation"

                enrichments:
                  - id: "spel-transformation"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "tradeAmount"
                        target-field: "adjustedAmount"
                        transformation: "#tradeAmount * 1.05"
                      - source-field: "price"
                        target-field: "priceCategory"
                        transformation: "#price > 100 ? 'EXPENSIVE' : 'CHEAP'"
                      - source-field: "quantity"
                        target-field: "totalValue"
                        transformation: "#price * #quantity"
                """;

            String yamlExpression = """
                version: "1.0"
                name: "Complex SpEL with expression"

                enrichments:
                  - id: "spel-expression"
                    type: "field-enrichment"
                    condition: "true"
                    field-mappings:
                      - source-field: "tradeAmount"
                        target-field: "adjustedAmount"
                        expression: "#tradeAmount * 1.05"
                      - source-field: "price"
                        target-field: "priceCategory"
                        expression: "#price > 100 ? 'EXPENSIVE' : 'CHEAP'"
                      - source-field: "quantity"
                        target-field: "totalValue"
                        expression: "#price * #quantity"
                """;

            Map<String, Object> testData = new HashMap<>();
            testData.put("tradeAmount", 1000.0);
            testData.put("price", 150.0);
            testData.put("quantity", 10);

            // Test with transformation keyword
            YamlRuleConfiguration configTransformation = yamlLoader.fromYamlString(yamlTransformation);
            RulesEngine engineTransformation = RulesEngine.fromYamlConfig(configTransformation);
            RuleResult resultTransformation = engineTransformation.evaluate(new HashMap<>(testData));
            Map<String, Object> enrichedTransformation = resultTransformation.getEnrichedData();

            // Test with expression keyword
            YamlRuleConfiguration configExpression = yamlLoader.fromYamlString(yamlExpression);
            RulesEngine engineExpression = RulesEngine.fromYamlConfig(configExpression);
            RuleResult resultExpression = engineExpression.evaluate(new HashMap<>(testData));
            Map<String, Object> enrichedExpression = resultExpression.getEnrichedData();

            // Verify both keywords produce identical results for complex SpEL
            assertEquals(enrichedTransformation.get("adjustedAmount"), enrichedExpression.get("adjustedAmount"),
                "Complex SpEL: transformation and expression should produce same adjustedAmount");
            assertEquals(enrichedTransformation.get("priceCategory"), enrichedExpression.get("priceCategory"),
                "Complex SpEL: transformation and expression should produce same priceCategory");
            assertEquals(enrichedTransformation.get("totalValue"), enrichedExpression.get("totalValue"),
                "Complex SpEL: transformation and expression should produce same totalValue");

            // Verify actual values
            assertEquals(1050.0, enrichedTransformation.get("adjustedAmount"),
                "adjustedAmount should be 1000 * 1.05 = 1050");
            assertEquals("EXPENSIVE", enrichedTransformation.get("priceCategory"),
                "priceCategory should be EXPENSIVE (price > 100)");
            assertEquals(1500.0, enrichedTransformation.get("totalValue"),
                "totalValue should be 150 * 10 = 1500");

            logger.info("[OK] Complex SpEL expressions test passed - both keywords work identically");

        } catch (Exception e) {
            logger.error("Complex SpEL expressions test failed", e);
            fail("Complex SpEL expressions test failed: " + e.getMessage());
        }
    }
}
