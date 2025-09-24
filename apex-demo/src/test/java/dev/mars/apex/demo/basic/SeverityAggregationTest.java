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

package dev.mars.apex.demo.basic;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleGroupEvaluationResult;
import dev.mars.apex.core.engine.model.RuleGroupSeverityAggregator;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Rule Group Severity Aggregation functionality.
 * 
 * This test class validates the core severity aggregation logic for rule groups,
 * ensuring that AND and OR groups properly aggregate severity from individual rules.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-23
 * @version 1.0
 */
@DisplayName("Rule Group Severity Aggregation Tests")
public class SeverityAggregationTest {

    /**
     * Test that the RuleGroupSeverityAggregator correctly aggregates severity for AND groups.
     * Business Logic: AND groups should use the highest severity of failed rules,
     * or highest of all rules if all pass.
     */
    @Test
    @DisplayName("AND group with mixed severities should use highest severity of failed rules")
    void testAndGroupMixedSeveritiesFailedRules() {
        RuleGroupSeverityAggregator aggregator = new RuleGroupSeverityAggregator();
        
        // Create rule results: ERROR fails, WARNING passes, INFO passes
        List<RuleResult> results = Arrays.asList(
            RuleResult.noMatch("error-rule", "Error rule failed", "ERROR"),
            RuleResult.match("warning-rule", "Warning rule passed", "WARNING"),
            RuleResult.match("info-rule", "Info rule passed", "INFO")
        );
        
        // For AND group: Should use highest severity of failed rules (ERROR)
        String aggregatedSeverity = aggregator.aggregateSeverity(results, true);
        
        assertEquals("ERROR", aggregatedSeverity, 
            "AND group should use highest severity of failed rules");
    }

    /**
     * Test that AND groups use highest severity when all rules pass.
     */
    @Test
    @DisplayName("AND group with all rules passing should use highest severity")
    void testAndGroupAllRulesPassing() {
        RuleGroupSeverityAggregator aggregator = new RuleGroupSeverityAggregator();
        
        // Create rule results: All rules pass with different severities
        List<RuleResult> results = Arrays.asList(
            RuleResult.match("error-rule", "Error rule passed", "ERROR"),
            RuleResult.match("warning-rule", "Warning rule passed", "WARNING"),
            RuleResult.match("info-rule", "Info rule passed", "INFO")
        );
        
        // For AND group: Should use highest severity of all rules (ERROR)
        String aggregatedSeverity = aggregator.aggregateSeverity(results, true);
        
        assertEquals("ERROR", aggregatedSeverity, 
            "AND group should use highest severity when all rules pass");
    }

    /**
     * Test that OR groups use severity of first matching rule.
     */
    @Test
    @DisplayName("OR group should use severity of first matching rule")
    void testOrGroupFirstMatchSeverity() {
        RuleGroupSeverityAggregator aggregator = new RuleGroupSeverityAggregator();
        
        // Create rule results: WARNING matches first, then ERROR matches
        List<RuleResult> results = Arrays.asList(
            RuleResult.noMatch("info-rule", "Info rule failed", "INFO"),
            RuleResult.match("warning-rule", "Warning rule matched", "WARNING"),
            RuleResult.match("error-rule", "Error rule matched", "ERROR")
        );
        
        // For OR group: Should use severity of first matching rule (WARNING)
        String aggregatedSeverity = aggregator.aggregateSeverity(results, false);
        
        assertEquals("WARNING", aggregatedSeverity, 
            "OR group should use severity of first matching rule");
    }

    /**
     * Test that empty rule groups default to INFO severity.
     */
    @Test
    @DisplayName("Empty rule group should default to INFO severity")
    void testEmptyRuleGroupDefaultSeverity() {
        RuleGroupSeverityAggregator aggregator = new RuleGroupSeverityAggregator();
        
        // Empty results list
        List<RuleResult> results = Arrays.asList();
        
        // Should default to INFO for both AND and OR groups
        String andSeverity = aggregator.aggregateSeverity(results, true);
        String orSeverity = aggregator.aggregateSeverity(results, false);
        
        assertEquals("INFO", andSeverity, "Empty AND group should default to INFO");
        assertEquals("INFO", orSeverity, "Empty OR group should default to INFO");
    }

    /**
     * Test end-to-end rule group evaluation with severity aggregation.
     */
    @Test
    @DisplayName("Rule group evaluation should aggregate severity correctly")
    void testRuleGroupEvaluationWithSeverity() {
        // Create rules with different severities
        Rule errorRule = new Rule("error-rule", "false", "Error rule message", "ERROR");
        Rule warningRule = new Rule("warning-rule", "true", "Warning rule message", "WARNING");
        Rule infoRule = new Rule("info-rule", "true", "Info rule message", "INFO");
        
        // Create AND rule group with short-circuiting disabled to evaluate all rules
        RuleGroup andGroup = new RuleGroup("test-and-group", "default", "Test AND Group",
                                          "Test AND Group Description", 1, true, false, false, false);
        andGroup.addRule(errorRule, 1);
        andGroup.addRule(warningRule, 2);
        andGroup.addRule(infoRule, 3);
        
        // Evaluate with details
        StandardEvaluationContext context = new StandardEvaluationContext();
        RuleGroupEvaluationResult result = andGroup.evaluateWithDetails(context);
        
        // Verify results
        assertFalse(result.isGroupResult(), "AND group should fail when one rule fails");
        assertEquals("ERROR", result.getAggregatedSeverity(), 
            "AND group should aggregate to ERROR severity");
        assertEquals(3, result.getTotalRulesEvaluated(), "Should evaluate all 3 rules");
        assertEquals(2, result.getRulesTriggered(), "Should have 2 triggered rules");
        assertEquals(1, result.getRulesFailed(), "Should have 1 failed rule");
    }

    /**
     * Test OR group evaluation with severity aggregation.
     */
    @Test
    @DisplayName("OR group evaluation should use first match severity")
    void testOrGroupEvaluationWithSeverity() {
        // Create rules with different severities
        Rule infoRule = new Rule("info-rule", "false", "Info rule message", "INFO");
        Rule warningRule = new Rule("warning-rule", "true", "Warning rule message", "WARNING");
        Rule errorRule = new Rule("error-rule", "true", "Error rule message", "ERROR");
        
        // Create OR rule group with short-circuiting disabled to evaluate all rules
        RuleGroup orGroup = new RuleGroup("test-or-group", "default", "Test OR Group",
                                         "Test OR Group Description", 1, false, false, false, false);
        orGroup.addRule(infoRule, 1);
        orGroup.addRule(warningRule, 2);
        orGroup.addRule(errorRule, 3);
        
        // Evaluate with details
        StandardEvaluationContext context = new StandardEvaluationContext();
        RuleGroupEvaluationResult result = orGroup.evaluateWithDetails(context);
        
        // Verify results
        assertTrue(result.isGroupResult(), "OR group should pass when any rule passes");
        assertEquals("WARNING", result.getAggregatedSeverity(), 
            "OR group should use first matching rule's severity (WARNING)");
        assertEquals(3, result.getTotalRulesEvaluated(), "Should evaluate all 3 rules");
        assertEquals(2, result.getRulesTriggered(), "Should have 2 triggered rules");
        assertEquals(1, result.getRulesFailed(), "Should have 1 failed rule");
    }

    /**
     * Test severity priority validation.
     */
    @Test
    @DisplayName("Severity aggregator should validate severity priorities correctly")
    void testSeverityPriorityValidation() {
        RuleGroupSeverityAggregator aggregator = new RuleGroupSeverityAggregator();
        
        // Test valid severities
        assertTrue(aggregator.isValidSeverity("ERROR"), "ERROR should be valid");
        assertTrue(aggregator.isValidSeverity("WARNING"), "WARNING should be valid");
        assertTrue(aggregator.isValidSeverity("INFO"), "INFO should be valid");
        
        // Test invalid severities
        assertFalse(aggregator.isValidSeverity("INVALID"), "INVALID should not be valid");
        assertFalse(aggregator.isValidSeverity(null), "null should not be valid");
        
        // Test priority values
        assertEquals(3, aggregator.getSeverityPriority("ERROR"), "ERROR should have priority 3");
        assertEquals(2, aggregator.getSeverityPriority("WARNING"), "WARNING should have priority 2");
        assertEquals(1, aggregator.getSeverityPriority("INFO"), "INFO should have priority 1");
        assertEquals(1, aggregator.getSeverityPriority("INVALID"), "Invalid severity should default to INFO priority");
    }
}
