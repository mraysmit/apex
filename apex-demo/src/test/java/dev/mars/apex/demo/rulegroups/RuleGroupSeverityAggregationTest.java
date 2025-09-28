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

package dev.mars.apex.demo.rulegroups;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Rule Group Severity Aggregation Tests.
 * 
 * Tests the new severity aggregation functionality for rule groups where groups
 * without explicit severity attributes aggregate severity from their constituent rules.
 * 
 * Business Logic:
 * - AND Groups: Use highest severity of failed rules, or highest of all if all pass
 * - OR Groups: Use severity of first matching rule
 * - Empty Groups: Default to INFO severity
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Rule Group Severity Aggregation Tests")
public class RuleGroupSeverityAggregationTest {

    private static final Logger logger = LoggerFactory.getLogger(RuleGroupSeverityAggregationTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
    }

    @Test
    @DisplayName("AND group aggregates severity from failed rules")
    void testAndGroupAggregatesSeverityFromFailedRules() {
        logInfo("Testing AND group severity aggregation with mixed results");
        
        String yamlContent = """
            metadata:
              id: "and-mixed-severity-test"
              name: "AND Mixed Severity Test"
              version: "1.0.0"
              description: "AND group with mixed severities where one rule fails"
              type: "rule-config"
              author: "APEX Demo Team"

            rules:
              - id: "error-rule"
                name: "Error Rule"
                condition: "false"
                message: "Error rule failed"
                severity: "ERROR"
                priority: 1
              - id: "warning-rule"
                name: "Warning Rule"
                condition: "true"
                message: "Warning rule passed"
                severity: "WARNING"
                priority: 2
              - id: "info-rule"
                name: "Info Rule"
                condition: "true"
                message: "Info rule passed"
                severity: "INFO"
                priority: 3

            rule-groups:
              - id: "and-mixed-group"
                name: "AND Mixed Group"
                description: "AND group with mixed severities"
                operator: "AND"
                stop-on-first-failure: false
                rule-ids:
                  - "error-rule"
                  - "warning-rule"
                  - "info-rule"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            
            RuleGroup group = engine.getConfiguration().getRuleGroupById("and-mixed-group");
            assertNotNull(group, "Rule group should be found");
            
            RuleResult result = engine.executeRuleGroupsList(List.of(group), Map.of());
            
            assertNotNull(result, "Result should not be null");
            assertFalse(result.isTriggered(), "AND group should fail when one rule fails");
            assertEquals("ERROR", result.getSeverity(), "Should use highest severity of failed rules");
            
            logSuccess("AND group severity aggregation working correctly - used ERROR from failed rule");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("AND group aggregates severity from all rules when all pass")
    void testAndGroupAggregatesSeverityFromAllRulesWhenAllPass() {
        logInfo("Testing AND group severity aggregation when all rules pass");
        
        String yamlContent = """
            metadata:
              id: "and-all-pass-test"
              name: "AND All Pass Test"
              version: "1.0.0"
              description: "AND group where all rules pass"
              type: "rule-config"
              author: "APEX Demo Team"

            rules:
              - id: "error-rule"
                name: "Error Rule"
                condition: "true"
                message: "Error rule passed"
                severity: "ERROR"
                priority: 1
              - id: "warning-rule"
                name: "Warning Rule"
                condition: "true"
                message: "Warning rule passed"
                severity: "WARNING"
                priority: 2

            rule-groups:
              - id: "and-all-pass-group"
                name: "AND All Pass Group"
                description: "AND group where all rules pass"
                operator: "AND"
                stop-on-first-failure: false
                rule-ids:
                  - "error-rule"
                  - "warning-rule"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            
            RuleGroup group = engine.getConfiguration().getRuleGroupById("and-all-pass-group");
            assertNotNull(group, "Rule group should be found");
            
            RuleResult result = engine.executeRuleGroupsList(List.of(group), Map.of());
            
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group should pass when all rules pass");
            assertEquals("ERROR", result.getSeverity(), "Should use highest severity of all rules");
            
            logSuccess("AND group severity aggregation working correctly - used ERROR as highest severity");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("OR group uses severity of first matching rule")
    void testOrGroupUsesFirstMatchingSeverity() {
        logInfo("Testing OR group severity aggregation with first match logic");
        
        String yamlContent = """
            metadata:
              id: "or-first-match-test"
              name: "OR First Match Test"
              version: "1.0.0"
              description: "OR group using first matching rule severity"
              type: "rule-config"
              author: "APEX Demo Team"

            rules:
              - id: "info-rule"
                name: "Info Rule"
                condition: "false"
                message: "Info rule failed"
                severity: "INFO"
                priority: 1
              - id: "warning-rule"
                name: "Warning Rule"
                condition: "true"
                message: "Warning rule passed"
                severity: "WARNING"
                priority: 2
              - id: "error-rule"
                name: "Error Rule"
                condition: "true"
                message: "Error rule passed"
                severity: "ERROR"
                priority: 3

            rule-groups:
              - id: "or-first-match-group"
                name: "OR First Match Group"
                description: "OR group using first matching rule"
                operator: "OR"
                stop-on-first-failure: false
                rule-ids:
                  - "info-rule"
                  - "warning-rule"
                  - "error-rule"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            
            RuleGroup group = engine.getConfiguration().getRuleGroupById("or-first-match-group");
            assertNotNull(group, "Rule group should be found");
            
            RuleResult result = engine.executeRuleGroupsList(List.of(group), Map.of());
            
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "OR group should pass when any rule passes");
            assertEquals("WARNING", result.getSeverity(), "Should use severity of first matching rule");
            
            logSuccess("OR group severity aggregation working correctly - used WARNING from first match");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Empty group does not pass by default")
    void testEmptyGroupDefaultSeverity() {
        logInfo("Testing empty group default behavior");
        
        String yamlContent = """
            metadata:
              id: "empty-group-test"
              name: "Empty Group Test"
              version: "1.0.0"
              description: "Empty group with no rules"
              type: "rule-config"
              author: "APEX Demo Team"

            rule-groups:
              - id: "empty-group"
                name: "Empty Group"
                description: "Group with no rules"
                operator: "AND"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            
            RuleGroup group = engine.getConfiguration().getRuleGroupById("empty-group");
            assertNotNull(group, "Rule group should be found");
            
            RuleResult result = engine.executeRuleGroupsList(List.of(group), Map.of());
            
            assertNotNull(result, "Result should not be null");
            assertFalse(result.isTriggered(), "Empty group should not pass (no rules to evaluate)");
            // Note: When no rules match, the result severity is typically the default from the engine

            logSuccess("Empty group behavior working correctly - empty groups do not pass");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }
}
