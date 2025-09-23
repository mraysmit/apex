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

package dev.mars.apex.demo.basic.rules;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RulesEngine with Rule Group Severity Aggregation.
 * 
 * This test validates that the RulesEngine correctly uses the new severity aggregation
 * functionality when evaluating rule groups.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-23
 * @version 1.0
 */
@DisplayName("RulesEngine Integration Tests with Severity Aggregation")
public class RulesEngineIntegrationTest {

    /**
     * Test that RulesEngine returns rule group results with aggregated severity.
     */
    @Test
    @DisplayName("RulesEngine should return rule group result with aggregated severity")
    void testRulesEngineWithSeverityAggregation() {
        // Create rules with different severities
        Rule errorRule = new Rule("error-rule", "false", "Error rule failed", "ERROR");
        Rule warningRule = new Rule("warning-rule", "true", "Warning rule passed", "WARNING");
        Rule infoRule = new Rule("info-rule", "true", "Info rule passed", "INFO");
        
        // Create AND rule group (should fail due to error rule)
        RuleGroup andGroup = new RuleGroup("test-and-group", "default", "Test AND Group", 
                                          "Test AND Group Description", 1, true, false, false, false);
        andGroup.addRule(errorRule, 1);
        andGroup.addRule(warningRule, 2);
        andGroup.addRule(infoRule, 3);
        
        // Create RulesEngine with configuration and add the rule group
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        configuration.registerRuleGroup(andGroup);
        RulesEngine rulesEngine = new RulesEngine(configuration);
        
        // Evaluate with context using executeRulesForCategory
        Map<String, Object> context = new HashMap<>();
        RuleResult result = rulesEngine.executeRulesForCategory("default", context);

        // Verify that no match was returned (since AND group failed)
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "AND group should fail when one rule fails");
        assertEquals("no-match", result.getRuleName(), "Should return no-match result");
    }

    /**
     * Test that RulesEngine returns matching rule group with aggregated severity.
     */
    @Test
    @DisplayName("RulesEngine should return matching rule group with aggregated severity")
    void testRulesEngineWithMatchingRuleGroup() {
        // Create rules where all pass
        Rule errorRule = new Rule("error-rule", "true", "Error rule passed", "ERROR");
        Rule warningRule = new Rule("warning-rule", "true", "Warning rule passed", "WARNING");
        Rule infoRule = new Rule("info-rule", "true", "Info rule passed", "INFO");
        
        // Create AND rule group (should pass since all rules pass)
        RuleGroup andGroup = new RuleGroup("test-and-group", "default", "Test AND Group", 
                                          "Test AND Group Description", 1, true, false, false, false);
        andGroup.addRule(errorRule, 1);
        andGroup.addRule(warningRule, 2);
        andGroup.addRule(infoRule, 3);
        
        // Create RulesEngine with configuration and add the rule group
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        configuration.registerRuleGroup(andGroup);
        RulesEngine rulesEngine = new RulesEngine(configuration);
        
        // Evaluate with context using executeRulesForCategory
        Map<String, Object> context = new HashMap<>();
        RuleResult result = rulesEngine.executeRulesForCategory("default", context);

        // Verify that match was returned with aggregated severity
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "AND group should pass when all rules pass");
        assertEquals("Test AND Group", result.getRuleName(), "Should return group name");
        assertEquals("ERROR", result.getSeverity(), "Should aggregate to highest severity (ERROR)");
    }

    /**
     * Test that RulesEngine works with OR groups and severity aggregation.
     */
    @Test
    @DisplayName("RulesEngine should handle OR groups with first match severity")
    void testRulesEngineWithOrGroupSeverity() {
        // Create rules where first matching rule has WARNING severity
        Rule infoRule = new Rule("info-rule", "false", "Info rule failed", "INFO");
        Rule warningRule = new Rule("warning-rule", "true", "Warning rule passed", "WARNING");
        Rule errorRule = new Rule("error-rule", "true", "Error rule passed", "ERROR");
        
        // Create OR rule group (should pass with WARNING severity from first match)
        RuleGroup orGroup = new RuleGroup("test-or-group", "default", "Test OR Group", 
                                         "Test OR Group Description", 1, false, false, false, false);
        orGroup.addRule(infoRule, 1);
        orGroup.addRule(warningRule, 2);
        orGroup.addRule(errorRule, 3);
        
        // Create RulesEngine with configuration and add the rule group
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        configuration.registerRuleGroup(orGroup);
        RulesEngine rulesEngine = new RulesEngine(configuration);
        
        // Evaluate with context using executeRulesForCategory
        Map<String, Object> context = new HashMap<>();
        RuleResult result = rulesEngine.executeRulesForCategory("default", context);

        // Verify that match was returned with first matching rule's severity
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "OR group should pass when any rule passes");
        assertEquals("Test OR Group", result.getRuleName(), "Should return group name");
        assertEquals("WARNING", result.getSeverity(), "Should use first matching rule's severity (WARNING)");
    }

    /**
     * Test that RulesEngine handles multiple rule groups correctly.
     */
    @Test
    @DisplayName("RulesEngine should evaluate multiple rule groups in priority order")
    void testRulesEngineWithMultipleRuleGroups() {
        // Create first rule group (higher priority, should be evaluated first)
        Rule highPriorityRule = new Rule("high-priority-rule", "true", "High priority rule", "WARNING");
        RuleGroup highPriorityGroup = new RuleGroup("high-priority-group", "default", "High Priority Group", 
                                                   "High Priority Group Description", 1, true);
        highPriorityGroup.addRule(highPriorityRule, 1);
        
        // Create second rule group (lower priority)
        Rule lowPriorityRule = new Rule("low-priority-rule", "true", "Low priority rule", "ERROR");
        RuleGroup lowPriorityGroup = new RuleGroup("low-priority-group", "default", "Low Priority Group", 
                                                  "Low Priority Group Description", 2, true);
        lowPriorityGroup.addRule(lowPriorityRule, 1);
        
        // Create RulesEngine with configuration and add rule groups
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        configuration.registerRuleGroup(highPriorityGroup);
        configuration.registerRuleGroup(lowPriorityGroup);
        RulesEngine rulesEngine = new RulesEngine(configuration);
        
        // Evaluate with context using executeRulesForCategory
        Map<String, Object> context = new HashMap<>();
        RuleResult result = rulesEngine.executeRulesForCategory("default", context);

        // Verify that first matching group was returned (high priority group)
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "High priority group should match");
        assertEquals("High Priority Group", result.getRuleName(), "Should return high priority group name");
        assertEquals("WARNING", result.getSeverity(), "Should use high priority group's severity");
    }
}
