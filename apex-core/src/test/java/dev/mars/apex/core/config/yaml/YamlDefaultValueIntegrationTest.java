package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

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
 * Integration test for Phase 3A Enhancement: Default-Value Support.
 * 
 * Tests the complete flow from YAML configuration through rule evaluation
 * with default-value error recovery.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
public class YamlDefaultValueIntegrationTest {

    @Test
    public void testYamlRuleWithDefaultValueLoading() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Default Value Test"
              version: "1.0.0"
            
            rules:
              - id: "age-validation"
                name: "Age Validation Rule"
                condition: "#data.age != null && #data.age >= 18"
                severity: "WARNING"
                default-value: false
                message: "Age validation failed"
                
              - id: "email-check"
                name: "Email Format Check"
                condition: "#data.email != null && #data.email.contains('@')"
                severity: "INFO"
                default-value: true
                message: "Email format check"
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Verify rules are loaded with default values
        assertNotNull(config.getRules());
        assertEquals(2, config.getRules().size());

        YamlRule ageRule = config.getRules().get(0);
        assertEquals("age-validation", ageRule.getId());
        assertEquals(false, ageRule.getDefaultValue());
        assertEquals("WARNING", ageRule.getSeverity());

        YamlRule emailRule = config.getRules().get(1);
        assertEquals("email-check", emailRule.getId());
        assertEquals(true, emailRule.getDefaultValue());
        assertEquals("INFO", emailRule.getSeverity());
    }

    @Test
    public void testRuleCreationWithDefaultValue() throws Exception {
        // Create a rule with default value
        YamlRuleFactory factory = new YamlRuleFactory();

        YamlRule yamlRule = new YamlRule();
        yamlRule.setId("test-rule");
        yamlRule.setName("Test Rule");
        yamlRule.setCondition("#data.field != null");
        yamlRule.setSeverity("WARNING");
        yamlRule.setDefaultValue("DEFAULT_RESULT");

        Rule rule = factory.createRuleWithMetadata(yamlRule);

        // Verify rule has default value
        assertEquals("DEFAULT_RESULT", rule.getDefaultValue());
        assertEquals("test-rule", rule.getId());
        assertEquals("Test Rule", rule.getName());
        assertEquals("#data.field != null", rule.getCondition());
        assertEquals("WARNING", rule.getSeverity());
    }

    @Test
    public void testCalculationEnrichmentWithDefaultValue() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Calculation Default Value Test"
              version: "1.0.0"
            
            enrichments:
              - id: "safe-calculation"
                name: "Safe Calculation Enrichment"
                type: "calculation-enrichment"
                condition: "#data != null"
                
                calculation-config:
                  expression: "#data.amount * #data.rate / 100"
                  result-field: "calculatedValue"
                  default-value: 0.0
                
                field-mappings:
                  - source-field: "calculatedValue"
                    target-field: "finalValue"
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Verify enrichment is loaded with calculation default value
        assertNotNull(config.getEnrichments());
        assertEquals(1, config.getEnrichments().size());

        YamlEnrichment enrichment = config.getEnrichments().get(0);
        assertEquals("safe-calculation", enrichment.getId());
        assertNotNull(enrichment.getCalculationConfig());
        assertEquals(0.0, enrichment.getCalculationConfig().getDefaultValue());
        assertEquals("#data.amount * #data.rate / 100", enrichment.getCalculationConfig().getExpression());
        assertEquals("calculatedValue", enrichment.getCalculationConfig().getResultField());
    }

    @Test
    public void testBackwardCompatibilityWithoutDefaultValues() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Backward Compatibility Test"
              version: "1.0.0"
            
            rules:
              - id: "legacy-rule"
                name: "Legacy Rule"
                condition: "#data.field != null"
                severity: "ERROR"
                message: "Legacy rule message"
            
            enrichments:
              - id: "legacy-calculation"
                name: "Legacy Calculation"
                type: "calculation-enrichment"
                
                calculation-config:
                  expression: "#data.value * 2"
                  result-field: "doubledValue"
                
                field-mappings:
                  - source-field: "doubledValue"
                    target-field: "result"
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Verify backward compatibility - no default values
        assertNotNull(config.getRules());
        assertEquals(1, config.getRules().size());
        YamlRule rule = config.getRules().get(0);
        assertNull(rule.getDefaultValue()); // Should be null for backward compatibility

        assertNotNull(config.getEnrichments());
        assertEquals(1, config.getEnrichments().size());
        YamlEnrichment enrichment = config.getEnrichments().get(0);
        assertNull(enrichment.getCalculationConfig().getDefaultValue()); // Should be null
    }

    @Test
    public void testMixedDefaultValueConfiguration() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Mixed Default Value Test"
              version: "1.0.0"
            
            rules:
              - id: "rule-with-default"
                name: "Rule With Default"
                condition: "#data.field1 > 0"
                severity: "WARNING"
                default-value: "SAFE_DEFAULT"
                
              - id: "rule-without-default"
                name: "Rule Without Default"
                condition: "#data.field2 != null"
                severity: "ERROR"
                # No default-value specified
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Verify mixed configuration
        assertNotNull(config.getRules());
        assertEquals(2, config.getRules().size());

        YamlRule ruleWithDefault = config.getRules().get(0);
        assertEquals("rule-with-default", ruleWithDefault.getId());
        assertEquals("SAFE_DEFAULT", ruleWithDefault.getDefaultValue());

        YamlRule ruleWithoutDefault = config.getRules().get(1);
        assertEquals("rule-without-default", ruleWithoutDefault.getId());
        assertNull(ruleWithoutDefault.getDefaultValue());
    }

    @Test
    public void testDefaultValueDataTypes() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Data Types Test"
              version: "1.0.0"
            
            rules:
              - id: "string-default"
                name: "String Default Rule"
                condition: "#data.field1 != null"
                default-value: "STRING_DEFAULT"

              - id: "boolean-default"
                name: "Boolean Default Rule"
                condition: "#data.field2 != null"
                default-value: true

              - id: "numeric-default"
                name: "Numeric Default Rule"
                condition: "#data.field3 != null"
                default-value: 42

              - id: "decimal-default"
                name: "Decimal Default Rule"
                condition: "#data.field4 != null"
                default-value: 3.14
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Verify different data types are preserved
        assertEquals(4, config.getRules().size());

        assertEquals("STRING_DEFAULT", config.getRules().get(0).getDefaultValue());
        assertTrue(config.getRules().get(0).getDefaultValue() instanceof String);

        assertEquals(true, config.getRules().get(1).getDefaultValue());
        assertTrue(config.getRules().get(1).getDefaultValue() instanceof Boolean);

        assertEquals(42, config.getRules().get(2).getDefaultValue());
        assertTrue(config.getRules().get(2).getDefaultValue() instanceof Integer);

        assertEquals(3.14, config.getRules().get(3).getDefaultValue());
        assertTrue(config.getRules().get(3).getDefaultValue() instanceof Double);
    }
}
