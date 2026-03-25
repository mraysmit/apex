package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlRule;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;

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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for the structured conditions feature (Phase 1).
 *
 * Tests cover:
 * - Mutual exclusivity of 'condition' vs 'conditions'
 * - Structured condition group operator validation (AND/OR)
 * - Condition predicate type validation (expression/lookup/function)
 * - Type-specific field requirements (lookup-config, enrichment-group-ref)
 * - YAML deserialization of 'conditions' into SharedConditionGroup
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.5
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class StructuredConditionValidationTest {

    private ConfigurationLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader();
    }

    // ========================================
    // Mutual Exclusivity Tests
    // ========================================

    @Test
    @DisplayName("Should reject rule with both 'condition' and 'conditions'")
    void testRejectBothConditionAndConditions() throws Exception {
        Path yamlFile = tempDir.resolve("both-condition-and-conditions.yaml");
        String yaml = """
            metadata:
              name: "Both Condition Test"
              type: "rule-config"
            rules:
              - id: "dual-rule"
                name: "Dual Condition Rule"
                condition: "#amount > 100"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "expression"
                      condition: "#amount > 200"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("defines both 'condition' and 'conditions'"),
                "Exception should indicate mutual exclusivity violation");
    }

    @Test
    @DisplayName("Should reject rule with neither 'condition' nor 'conditions'")
    void testRejectNeitherConditionNorConditions() throws Exception {
        Path yamlFile = tempDir.resolve("no-condition.yaml");
        String yaml = """
            metadata:
              name: "No Condition Test"
              type: "rule-config"
            rules:
              - id: "empty-rule"
                name: "No Condition Rule"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("must define either 'condition' or 'conditions'"),
                "Exception should indicate missing condition");
    }

    @Test
    @DisplayName("Should accept rule with only 'condition' (backward compatible)")
    void testAcceptTraditionalCondition() throws Exception {
        Path yamlFile = tempDir.resolve("traditional-condition.yaml");
        String yaml = """
            metadata:
              name: "Traditional Condition Test"
              type: "rule-config"
            rules:
              - id: "trad-rule"
                name: "Traditional Rule"
                condition: "#amount > 100"
                message: "Amount exceeds threshold"
            """;
        Files.writeString(yamlFile, yaml);

        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());

        assertNotNull(config);
        assertEquals(1, config.getRules().size());
        YamlRule rule = config.getRules().get(0);
        assertEquals("#amount > 100", rule.getCondition());
        assertNull(rule.getConditions(), "conditions should be null for traditional rule");
    }

    @Test
    @DisplayName("Should accept rule with only 'conditions' (structured)")
    void testAcceptStructuredConditions() throws Exception {
        Path yamlFile = tempDir.resolve("structured-conditions.yaml");
        String yaml = """
            metadata:
              name: "Structured Conditions Test"
              type: "rule-config"
            rules:
              - id: "struct-rule"
                name: "Structured Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "expression"
                      condition: "#amount > 100"
                      description: "Amount threshold"
                    - type: "expression"
                      condition: "#currency == 'USD'"
                      description: "USD currency check"
                message: "High USD amount"
            """;
        Files.writeString(yamlFile, yaml);

        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());

        assertNotNull(config);
        assertEquals(1, config.getRules().size());
        YamlRule rule = config.getRules().get(0);
        assertNull(rule.getCondition(), "condition should be null for structured rule");
        assertNotNull(rule.getConditions());
        assertEquals("AND", rule.getConditions().getOperator());
        assertEquals(2, rule.getConditions().getRules().size());
    }

    // ========================================
    // Operator Validation Tests
    // ========================================

    @Test
    @DisplayName("Should reject conditions with missing operator")
    void testRejectMissingOperator() throws Exception {
        Path yamlFile = tempDir.resolve("missing-operator.yaml");
        String yaml = """
            metadata:
              name: "Missing Operator Test"
              type: "rule-config"
            rules:
              - id: "no-op-rule"
                name: "No Operator Rule"
                conditions:
                  rules:
                    - type: "expression"
                      condition: "#amount > 100"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("must specify an 'operator'"),
                "Exception should indicate missing operator");
    }

    @Test
    @DisplayName("Should reject conditions with invalid operator")
    void testRejectInvalidOperator() throws Exception {
        Path yamlFile = tempDir.resolve("invalid-operator.yaml");
        String yaml = """
            metadata:
              name: "Invalid Operator Test"
              type: "rule-config"
            rules:
              - id: "bad-op-rule"
                name: "Bad Operator Rule"
                conditions:
                  operator: "XOR"
                  rules:
                    - type: "expression"
                      condition: "#amount > 100"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("invalid operator 'XOR'"),
                "Exception should indicate invalid operator");
    }

    @Test
    @DisplayName("Should accept AND operator")
    void testAcceptAndOperator() throws Exception {
        Path yamlFile = tempDir.resolve("and-operator.yaml");
        String yaml = """
            metadata:
              name: "AND Operator Test"
              type: "rule-config"
            rules:
              - id: "and-rule"
                name: "AND Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "expression"
                      condition: "#a > 1"
                message: "AND works"
            """;
        Files.writeString(yamlFile, yaml);

        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());
        assertEquals("AND", config.getRules().get(0).getConditions().getOperator());
    }

    @Test
    @DisplayName("Should accept OR operator")
    void testAcceptOrOperator() throws Exception {
        Path yamlFile = tempDir.resolve("or-operator.yaml");
        String yaml = """
            metadata:
              name: "OR Operator Test"
              type: "rule-config"
            rules:
              - id: "or-rule"
                name: "OR Rule"
                conditions:
                  operator: "OR"
                  rules:
                    - type: "expression"
                      condition: "#a > 1"
                message: "OR works"
            """;
        Files.writeString(yamlFile, yaml);

        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());
        assertEquals("OR", config.getRules().get(0).getConditions().getOperator());
    }

    // ========================================
    // Predicate Rules Validation Tests
    // ========================================

    @Test
    @DisplayName("Should reject conditions with empty rules list")
    void testRejectEmptyRulesList() throws Exception {
        Path yamlFile = tempDir.resolve("empty-rules.yaml");
        String yaml = """
            metadata:
              name: "Empty Rules Test"
              type: "rule-config"
            rules:
              - id: "empty-rules-rule"
                name: "Empty Rules Rule"
                conditions:
                  operator: "AND"
                  rules: []
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("must contain at least one rule predicate"),
                "Exception should indicate empty rules");
    }

    @Test
    @DisplayName("Should reject predicate with invalid type")
    void testRejectInvalidPredicateType() throws Exception {
        Path yamlFile = tempDir.resolve("invalid-type.yaml");
        String yaml = """
            metadata:
              name: "Invalid Type Test"
              type: "rule-config"
            rules:
              - id: "bad-type-rule"
                name: "Bad Type Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "custom"
                      condition: "#a > 1"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("invalid type 'custom'"),
                "Exception should indicate invalid type");
    }

    @Test
    @DisplayName("Should reject predicate with missing condition expression")
    void testRejectMissingConditionExpression() throws Exception {
        Path yamlFile = tempDir.resolve("missing-condition-expr.yaml");
        String yaml = """
            metadata:
              name: "Missing Condition Expression Test"
              type: "rule-config"
            rules:
              - id: "no-expr-rule"
                name: "No Expression Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "expression"
                      description: "Missing the condition field"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("must have a 'condition' SpEL expression"),
                "Exception should indicate missing condition expression");
    }

    @Test
    @DisplayName("Should reject lookup type without lookup-config")
    void testRejectLookupWithoutConfig() throws Exception {
        Path yamlFile = tempDir.resolve("lookup-no-config.yaml");
        String yaml = """
            metadata:
              name: "Lookup No Config Test"
              type: "rule-config"
            rules:
              - id: "lookup-rule"
                name: "Lookup Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "lookup"
                      condition: "#lookupResult != null"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("type 'lookup' requires 'lookup-config'"),
                "Exception should indicate missing lookup-config");
    }

    @Test
    @DisplayName("Should reject function type without enrichment-group-ref")
    void testRejectFunctionWithoutGroupRef() throws Exception {
        Path yamlFile = tempDir.resolve("function-no-ref.yaml");
        String yaml = """
            metadata:
              name: "Function No Ref Test"
              type: "rule-config"
            rules:
              - id: "function-rule"
                name: "Function Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "function"
                      condition: "#functionResult == true"
                message: "Should not load"
            """;
        Files.writeString(yamlFile, yaml);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            loader.loadFromFile(yamlFile.toString());
        });

        assertTrue(exception.getMessage().contains("type 'function' requires 'enrichment-group-ref'"),
                "Exception should indicate missing enrichment-group-ref");
    }

    @Test
    @DisplayName("Should default predicate type to expression when not specified")
    void testDefaultTypeIsExpression() throws Exception {
        Path yamlFile = tempDir.resolve("default-type.yaml");
        String yaml = """
            metadata:
              name: "Default Type Test"
              type: "rule-config"
            rules:
              - id: "default-type-rule"
                name: "Default Type Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - condition: "#amount > 100"
                      description: "No type specified, defaults to expression"
                message: "Defaults to expression type"
            """;
        Files.writeString(yamlFile, yaml);

        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());

        assertNotNull(config);
        SharedConditionRule predicate = config.getRules().get(0).getConditions().getRules().get(0);
        // Type should be null (validator treats null as "expression")
        assertNull(predicate.getType(), "Type should be null when not specified (defaults to expression)");
    }

    // ========================================
    // Deserialization Tests
    // ========================================

    @Test
    @DisplayName("Should deserialize full structured conditions with all fields")
    void testFullDeserialization() throws Exception {
        Path yamlFile = tempDir.resolve("full-deserialization.yaml");
        String yaml = """
            metadata:
              name: "Full Deserialization Test"
              type: "rule-config"
            rules:
              - id: "full-rule"
                name: "Full Structured Rule"
                conditions:
                  operator: "OR"
                  rules:
                    - type: "expression"
                      condition: "#amount > 10000"
                      description: "High value transaction"
                    - type: "expression"
                      condition: "#priority == 'URGENT'"
                      description: "Urgent priority"
                    - type: "expression"
                      condition: "#region == 'RESTRICTED'"
                      description: "Restricted region"
                message: "Requires elevated approval"
                severity: "WARNING"
            """;
        Files.writeString(yamlFile, yaml);

        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());

        YamlRule rule = config.getRules().get(0);
        assertEquals("full-rule", rule.getId());
        assertNull(rule.getCondition());

        SharedConditionGroup group = rule.getConditions();
        assertNotNull(group);
        assertEquals("OR", group.getOperator());

        List<SharedConditionRule> predicates = group.getRules();
        assertEquals(3, predicates.size());

        assertEquals("expression", predicates.get(0).getType());
        assertEquals("#amount > 10000", predicates.get(0).getCondition());
        assertEquals("High value transaction", predicates.get(0).getDescription());

        assertEquals("expression", predicates.get(1).getType());
        assertEquals("#priority == 'URGENT'", predicates.get(1).getCondition());

        assertEquals("expression", predicates.get(2).getType());
        assertEquals("#region == 'RESTRICTED'", predicates.get(2).getCondition());
    }

    @Test
    @DisplayName("Should mix structured-condition rules with traditional rules in same config")
    void testMixedRuleTypes() throws Exception {
        Path yamlFile = tempDir.resolve("mixed-rules.yaml");
        String yaml = """
            metadata:
              name: "Mixed Rules Test"
              type: "rule-config"
            rules:
              - id: "traditional-rule"
                name: "Traditional Rule"
                condition: "#age >= 18"
                message: "Age verified"

              - id: "structured-rule"
                name: "Structured Rule"
                conditions:
                  operator: "AND"
                  rules:
                    - type: "expression"
                      condition: "#amount > 500"
                    - type: "expression"
                      condition: "#currency == 'EUR'"
                message: "High EUR amount"
            """;
        Files.writeString(yamlFile, yaml);

        YamlRuleConfiguration config = loader.loadFromFile(yamlFile.toString());

        assertEquals(2, config.getRules().size());

        YamlRule trad = config.getRules().get(0);
        assertEquals("#age >= 18", trad.getCondition());
        assertNull(trad.getConditions());

        YamlRule structured = config.getRules().get(1);
        assertNull(structured.getCondition());
        assertNotNull(structured.getConditions());
        assertEquals("AND", structured.getConditions().getOperator());
        assertEquals(2, structured.getConditions().getRules().size());
    }
}
