package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Conditional Mapping Enrichment functionality.
 * Tests the new conditional-mapping-enrichment type with priority-based processing.
 */
public class ConditionalMappingEnrichmentTest {

    @Test
    @DisplayName("Should create conditional mapping enrichment structure")
    void shouldCreateConditionalMappingEnrichmentStructure() {
        // Create a conditional-mapping-enrichment
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-conditional-mapping");
        enrichment.setType("conditional-mapping-enrichment");
        enrichment.setTargetField("IS_NDF");

        // Create mapping rules
        List<YamlEnrichment.MappingRule> mappingRules = new ArrayList<>();
        
        // Rule 1: High priority direct mapping
        YamlEnrichment.MappingRule rule1 = new YamlEnrichment.MappingRule();
        rule1.setId("high-priority-rule");
        rule1.setPriority(1);
        
        // Create conditions for rule 1
        YamlEnrichment.ConditionGroup conditions1 = new YamlEnrichment.ConditionGroup();
        conditions1.setOperator("AND");
        List<YamlEnrichment.ConditionRule> conditionRules1 = new ArrayList<>();
        YamlEnrichment.ConditionRule condRule1 = new YamlEnrichment.ConditionRule();
        condRule1.setCondition("#SYSTEM_CODE == 'SWIFT'");
        conditionRules1.add(condRule1);
        conditions1.setRules(conditionRules1);
        rule1.setConditions(conditions1);
        
        // Create mapping for rule 1
        YamlEnrichment.MappingConfig mapping1 = new YamlEnrichment.MappingConfig();
        mapping1.setType("direct");
        mapping1.setExpression("'HIGH_PRIORITY'");
        rule1.setMapping(mapping1);

        mappingRules.add(rule1);

        // Rule 2: Lower priority default rule
        YamlEnrichment.MappingRule rule2 = new YamlEnrichment.MappingRule();
        rule2.setId("default-rule");
        rule2.setPriority(999);
        // No conditions = default rule

        YamlEnrichment.MappingConfig mapping2 = new YamlEnrichment.MappingConfig();
        mapping2.setType("direct");
        mapping2.setExpression("'DEFAULT_VALUE'");
        rule2.setMapping(mapping2);
        
        mappingRules.add(rule2);

        enrichment.setMappingRules(mappingRules);

        // Create execution settings
        YamlEnrichment.ExecutionSettings executionSettings = new YamlEnrichment.ExecutionSettings();
        executionSettings.setStopOnFirstMatch(true);
        executionSettings.setLogMatchedRule(true);
        enrichment.setExecutionSettings(executionSettings);

        // Verify structure
        assertNotNull(enrichment);
        assertEquals("conditional-mapping-enrichment", enrichment.getType());
        assertEquals("IS_NDF", enrichment.getTargetField());
        assertEquals(2, enrichment.getMappingRules().size());
        assertTrue(enrichment.getExecutionSettings().getStopOnFirstMatch());
        assertTrue(enrichment.getExecutionSettings().getLogMatchedRule());
    }

    @Test
    @DisplayName("Should process high priority rule first")
    void shouldProcessHighPriorityRuleFirst() throws Exception {
        // Create test data that matches high priority rule
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "SWIFT");
        testData.put("IS_NDF", "original_value");

        // Create YAML config with priority-based rules
        String yamlConfig = """
            metadata:
              id: "priority-test"
              name: "Priority Test"
              version: "1.0.0"
              description: "Test priority-based conditional mapping"
              type: "rule-config"

            enrichments:
              - id: "priority-test"
                type: "conditional-mapping-enrichment"
                target-field: "IS_NDF"
                mapping-rules:
                  - id: "high-priority"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#SYSTEM_CODE == 'SWIFT'"
                    mapping:
                      type: "direct"
                      expression: "'HIGH_PRIORITY'"
                  - id: "default"
                    priority: 999
                    mapping:
                      type: "direct"
                      expression: "'DEFAULT_VALUE'"
                execution-settings:
                  stop-on-first-match: true
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Process enrichment using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> resultMap = result.getEnrichedData();

        // Verify high priority rule was applied
        assertNotNull(resultMap);
        assertEquals("HIGH_PRIORITY", resultMap.get("IS_NDF"));
    }

    @Test
    @DisplayName("Should fall back to default rule when conditions not met")
    void shouldFallBackToDefaultRule() throws Exception {
        // Create test data that doesn't match high priority rule
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "OTHER");
        testData.put("IS_NDF", "original_value");

        // Create YAML config with priority-based rules
        String yamlConfig = """
            metadata:
              id: "priority-test"
              name: "Priority Test"
              version: "1.0.0"
              description: "Test priority-based conditional mapping"
              type: "rule-config"

            enrichments:
              - id: "priority-test"
                type: "conditional-mapping-enrichment"
                target-field: "IS_NDF"
                mapping-rules:
                  - id: "high-priority"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#SYSTEM_CODE == 'SWIFT'"
                    mapping:
                      type: "direct"
                      expression: "'HIGH_PRIORITY'"
                  - id: "default"
                    priority: 999
                    mapping:
                      type: "direct"
                      expression: "'DEFAULT_VALUE'"
                execution-settings:
                  stop-on-first-match: true
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Process enrichment using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> resultMap = result.getEnrichedData();

        // Verify default rule was applied
        assertNotNull(resultMap);
        assertEquals("DEFAULT_VALUE", resultMap.get("IS_NDF"));
    }

    @Test
    @DisplayName("Should stop on first match when configured")
    void shouldStopOnFirstMatch() throws Exception {
        // Create test data that would match multiple rules
        Map<String, Object> testData = new HashMap<>();
        testData.put("SYSTEM_CODE", "SWIFT");
        testData.put("IS_NDF", "original_value");

        // Create YAML config with multiple matching rules
        String yamlConfig = """
            metadata:
              id: "multiple-match-test"
              name: "Multiple Match Test"
              version: "1.0.0"
              description: "Test stop on first match"
              type: "rule-config"

            enrichments:
              - id: "multiple-match-test"
                type: "conditional-mapping-enrichment"
                target-field: "IS_NDF"
                mapping-rules:
                  - id: "first-rule"
                    priority: 1
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#SYSTEM_CODE == 'SWIFT'"
                    mapping:
                      type: "direct"
                      expression: "'FIRST_MATCH'"
                  - id: "second-rule"
                    priority: 2
                    conditions:
                      operator: "AND"
                      rules:
                        - condition: "#SYSTEM_CODE != null"
                    mapping:
                      type: "direct"
                      expression: "'SECOND_MATCH'"
                execution-settings:
                  stop-on-first-match: true
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);

        // Process enrichment using RulesEngine
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(testData);
        Map<String, Object> resultMap = result.getEnrichedData();

        // Verify only first (highest priority) rule was applied
        assertNotNull(resultMap);
        assertEquals("FIRST_MATCH", resultMap.get("IS_NDF"));
    }

}
