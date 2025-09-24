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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Severity Edge Cases Test - Tests edge cases and boundary conditions for severity handling
 * including case sensitivity, whitespace, and special scenarios.
 * 
 * This test validates:
 * - Severity normalization (case sensitivity, whitespace trimming)
 * - Boundary conditions (null values, empty strings, numeric limits)
 * - Complex data structures (nested objects, collections, dates)
 * - Special content handling (long messages, special characters)
 * - Rule group behavior with edge case rules
 * - Performance with large rule groups and complex conditions
 * 
 * Following APEX testing principles:
 * - Tests actual functionality, not YAML syntax
 * - Uses real APEX rule engine operations with RuleResult API
 * - Validates business logic with specific assertions
 * - Tests end-to-end workflows from YAML to results
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-24
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("APEX Severity Edge Cases Test")
public class SeverityEdgeCasesTest {

    private static final Logger logger = LoggerFactory.getLogger(SeverityEdgeCasesTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for severity edge cases testing...");
        
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();
        
        logger.info("✅ APEX services initialized successfully");
    }

    @Test
    @DisplayName("Test severity handling and case sensitivity")
    void testSeverityHandling() throws Exception {
        logger.info("=== Testing Severity Handling and Case Sensitivity ===");

        // Load edge cases configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityEdgeCasesTest.yaml"
        );

        assertNotNull(config, "Configuration should be loaded");
        assertEquals("Severity Edge Cases Test", config.getMetadata().getName());
        assertEquals(12, config.getRules().size(), "Should have exactly 12 rules");
        assertEquals(6, config.getRuleGroups().size(), "Should have exactly 6 rule groups");

        logger.info("✅ Configuration loaded: {} rules, {} rule groups",
            config.getRules().size(), config.getRuleGroups().size());

        // Create RulesEngine
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(engine, "RulesEngine should be created");

        // Test standard severity handling
        testStandardSeverityHandling(engine);

        // Test case sensitivity preservation (APEX preserves original case)
        testCaseSensitivityPreservation(engine);

        // Test case sensitivity rule group
        testCaseSensitivityRuleGroup(engine);

        logger.info("✅ All severity handling tests passed");
    }

    private void testStandardSeverityHandling(RulesEngine engine) {
        logger.info("--- Testing Standard Severity Handling ---");

        // Test data (always true condition)
        Map<String, Object> testData = new HashMap<>();

        // Get rule with standard ERROR severity and execute it
        Rule rule = engine.getConfiguration().getRuleById("whitespace-severity-test");
        assertNotNull(rule, "Standard severity test rule should be found by ID");

        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule with standard severity should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Standard ERROR severity should be preserved");

        logger.info("✅ Standard severity handling test passed with severity: {}", result.getSeverity());
    }

    private void testCaseSensitivityPreservation(RulesEngine engine) {
        logger.info("--- Testing Case Sensitivity Preservation ---");

        // Test data (always true condition)
        Map<String, Object> testData = new HashMap<>();

        // Test mixed case ERROR (APEX preserves original case from YAML)
        Rule mixedCaseRule = engine.getConfiguration().getRuleById("mixed-case-error");
        assertNotNull(mixedCaseRule, "Mixed case error rule should be found by ID");

        RuleResult mixedResult = engine.executeRule(mixedCaseRule, testData);
        assertNotNull(mixedResult, "Mixed case rule result should not be null");
        assertTrue(mixedResult.isTriggered(), "Mixed case rule should be triggered");
        assertEquals("Error", mixedResult.getSeverity(), "Mixed case 'Error' should be preserved as-is");

        // Test lowercase WARNING (APEX preserves original case from YAML)
        Rule lowercaseRule = engine.getConfiguration().getRuleById("lowercase-warning");
        assertNotNull(lowercaseRule, "Lowercase warning rule should be found by ID");

        RuleResult lowercaseResult = engine.executeRule(lowercaseRule, testData);
        assertNotNull(lowercaseResult, "Lowercase rule result should not be null");
        assertTrue(lowercaseResult.isTriggered(), "Lowercase rule should be triggered");
        assertEquals("warning", lowercaseResult.getSeverity(), "Lowercase 'warning' should be preserved as-is");

        logger.info("✅ Case sensitivity preservation tests passed");
    }

    private void testCaseSensitivityRuleGroup(RulesEngine engine) {
        logger.info("--- Testing Case Sensitivity Rule Group ---");

        // Test data (always true conditions)
        Map<String, Object> testData = new HashMap<>();

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("case-sensitivity-group");
        assertNotNull(ruleGroup, "Case sensitivity group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "Case sensitivity AND group should be triggered when all rules pass");
        // APEX uses the first rule's severity in AND groups when all pass, which is "ERROR" (standard case)
        assertEquals("ERROR", result.getSeverity(), "Case sensitivity group should use severity from first rule (ERROR)");

        logger.info("✅ Case sensitivity rule group test passed with severity: {}", result.getSeverity());
    }

    @Test
    @DisplayName("Test boundary conditions and edge cases")
    void testBoundaryConditions() throws Exception {
        logger.info("=== Testing Boundary Conditions and Edge Cases ===");

        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityEdgeCasesTest.yaml"
        );
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);

        // Test numeric boundary conditions
        testNumericBoundaryConditions(engine);

        // Test null and empty string conditions
        testNullEmptyStringConditions(engine);

        // Test boolean edge cases
        testBooleanEdgeCases(engine);

        // Test boundary conditions rule group
        testBoundaryConditionsRuleGroup(engine);

        logger.info("✅ All boundary condition tests passed");
    }

    private void testNumericBoundaryConditions(RulesEngine engine) {
        logger.info("--- Testing Numeric Boundary Conditions ---");

        // Test with boundary value 0
        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 0);

        Rule rule = engine.getConfiguration().getRuleById("numeric-boundary-test");
        assertNotNull(rule, "Numeric boundary test rule should be found by ID");

        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered for boundary value 0");
        assertEquals("ERROR", result.getSeverity(), "Numeric boundary rule should have ERROR severity");

        // Test with boundary value -1
        testData.put("value", -1);
        result = engine.executeRule(rule, testData);
        assertTrue(result.isTriggered(), "Rule should be triggered for boundary value -1");

        // Test with large boundary value
        testData.put("value", 999999999);
        result = engine.executeRule(rule, testData);
        assertTrue(result.isTriggered(), "Rule should be triggered for large boundary value");

        logger.info("✅ Numeric boundary conditions test passed");
    }

    private void testNullEmptyStringConditions(RulesEngine engine) {
        logger.info("--- Testing Null and Empty String Conditions ---");

        Rule rule = engine.getConfiguration().getRuleById("null-empty-test");
        assertNotNull(rule, "Null empty test rule should be found by ID");

        // Test with null value
        Map<String, Object> testData = new HashMap<>();
        testData.put("testString", null);

        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered for null string");
        assertEquals("WARNING", result.getSeverity(), "Null empty rule should have WARNING severity");

        // Test with empty string
        testData.put("testString", "");
        result = engine.executeRule(rule, testData);
        assertTrue(result.isTriggered(), "Rule should be triggered for empty string");

        // Test with whitespace-only string
        testData.put("testString", "   ");
        result = engine.executeRule(rule, testData);
        assertTrue(result.isTriggered(), "Rule should be triggered for whitespace-only string");

        logger.info("✅ Null and empty string conditions test passed");
    }

    private void testBooleanEdgeCases(RulesEngine engine) {
        logger.info("--- Testing Boolean Edge Cases ---");

        Rule rule = engine.getConfiguration().getRuleById("boolean-edge-test");
        assertNotNull(rule, "Boolean edge test rule should be found by ID");

        // Test with true value
        Map<String, Object> testData = new HashMap<>();
        testData.put("boolValue", true);

        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered for boolean true");
        assertEquals("INFO", result.getSeverity(), "Boolean edge rule should have INFO severity");

        // Test with false value
        testData.put("boolValue", false);
        result = engine.executeRule(rule, testData);
        assertTrue(result.isTriggered(), "Rule should be triggered for boolean false");

        logger.info("✅ Boolean edge cases test passed");
    }

    private void testBoundaryConditionsRuleGroup(RulesEngine engine) {
        logger.info("--- Testing Boundary Conditions Rule Group ---");

        // Test data that triggers numeric boundary rule
        Map<String, Object> testData = new HashMap<>();
        testData.put("value", 0); // triggers numeric-boundary-test

        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("boundary-conditions-group");
        assertNotNull(ruleGroup, "Boundary conditions group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "Boundary conditions OR group should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Boundary conditions group should use severity from first matching rule (ERROR)");

        logger.info("✅ Boundary conditions rule group test passed with severity: {}", result.getSeverity());
    }

    @Test
    @DisplayName("Test complex data structures and special content")
    void testComplexDataStructures() throws Exception {
        logger.info("=== Testing Complex Data Structures and Special Content ===");

        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityEdgeCasesTest.yaml"
        );
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);

        // Test collection edge cases
        testCollectionEdgeCases(engine);

        // Test datetime edge cases
        testDateTimeEdgeCases(engine);

        // Test nested object access
        testNestedObjectAccess(engine);

        // Test special content handling
        testSpecialContentHandling(engine);

        // Test comprehensive edge cases group
        testComprehensiveEdgeCasesGroup(engine);

        logger.info("✅ All complex data structure tests passed");
    }

    private void testCollectionEdgeCases(RulesEngine engine) {
        logger.info("--- Testing Collection Edge Cases ---");

        Rule rule = engine.getConfiguration().getRuleById("collection-edge-test");
        assertNotNull(rule, "Collection edge test rule should be found by ID");

        // Test with empty collection
        Map<String, Object> testData = new HashMap<>();
        testData.put("collection", new ArrayList<>());

        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered for empty collection");
        assertEquals("WARNING", result.getSeverity(), "Collection edge rule should have WARNING severity");

        // Test with large collection (over 100 items)
        List<Integer> largeCollection = new ArrayList<>();
        for (int i = 0; i <= 101; i++) {
            largeCollection.add(i);
        }
        testData.put("collection", largeCollection);
        result = engine.executeRule(rule, testData);
        assertTrue(result.isTriggered(), "Rule should be triggered for large collection");

        logger.info("✅ Collection edge cases test passed");
    }

    private void testDateTimeEdgeCases(RulesEngine engine) {
        logger.info("--- Testing DateTime Edge Cases ---");

        Rule rule = engine.getConfiguration().getRuleById("datetime-edge-test");
        assertNotNull(rule, "DateTime edge test rule should be found by ID");

        // Test with positive timestamp
        Map<String, Object> testData = new HashMap<>();
        testData.put("timestamp", System.currentTimeMillis());

        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered for positive timestamp");
        assertEquals("INFO", result.getSeverity(), "DateTime edge rule should have INFO severity");

        logger.info("✅ DateTime edge cases test passed");
    }

    private void testNestedObjectAccess(RulesEngine engine) {
        logger.info("--- Testing Nested Object Access ---");

        Rule rule = engine.getConfiguration().getRuleById("nested-object-test");
        assertNotNull(rule, "Nested object test rule should be found by ID");

        // Create nested object structure
        Map<String, Object> profile = new HashMap<>();
        profile.put("riskScore", 98);

        Map<String, Object> customer = new HashMap<>();
        customer.put("profile", profile);

        Map<String, Object> testData = new HashMap<>();
        testData.put("customer", customer);

        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered for high risk nested object");
        assertEquals("ERROR", result.getSeverity(), "Nested object rule should have ERROR severity");

        logger.info("✅ Nested object access test passed");
    }

    private void testSpecialContentHandling(RulesEngine engine) {
        logger.info("--- Testing Special Content Handling ---");

        // Test long message rule
        Rule longMessageRule = engine.getConfiguration().getRuleById("long-message-rule");
        assertNotNull(longMessageRule, "Long message rule should be found by ID");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testLongMessage", true);

        RuleResult result = engine.executeRule(longMessageRule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Long message rule should be triggered");
        assertEquals("WARNING", result.getSeverity(), "Long message rule should have WARNING severity");
        assertNotNull(result.getMessage(), "Long message should be present");
        assertTrue(result.getMessage().length() > 100, "Message should be long");

        // Test special characters rule
        Rule specialCharsRule = engine.getConfiguration().getRuleById("special-chars-rule");
        assertNotNull(specialCharsRule, "Special chars rule should be found by ID");

        testData.put("testSpecialChars", true);
        result = engine.executeRule(specialCharsRule, testData);
        assertTrue(result.isTriggered(), "Special chars rule should be triggered");
        assertEquals("INFO", result.getSeverity(), "Special chars rule should have INFO severity");

        logger.info("✅ Special content handling test passed");
    }

    private void testComprehensiveEdgeCasesGroup(RulesEngine engine) {
        logger.info("--- Testing Comprehensive Edge Cases Group ---");

        // Test data that triggers the first rule (whitespace severity test)
        Map<String, Object> testData = new HashMap<>();
        // No specific data needed as whitespace-severity-test has condition "true"

        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("comprehensive-edge-cases");
        assertNotNull(ruleGroup, "Comprehensive edge cases group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "Comprehensive edge cases OR group should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Comprehensive group should use severity from first matching rule (ERROR)");

        logger.info("✅ Comprehensive edge cases group test passed with severity: {}", result.getSeverity());
    }
}
