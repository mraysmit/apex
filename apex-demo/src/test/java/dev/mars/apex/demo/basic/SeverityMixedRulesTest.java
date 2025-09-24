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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mixed Severity Rules Test - Tests mixed severity levels within single configuration
 * to verify proper severity handling and isolation between different rule types.
 * 
 * This test validates:
 * - Individual rules with different severity levels work correctly
 * - Rule groups with single severity type aggregate properly
 * - Rule groups with mixed severity types handle aggregation correctly
 * - Severity isolation between different rule categories
 * - Complex mixed severity scenarios work as expected
 * 
 * Following APEX testing principles:
 * - Tests actual functionality, not YAML syntax
 * - Uses real APEX rule engine operations
 * - Validates business logic with specific assertions
 * - Tests end-to-end workflows from YAML to results
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-24
 * @version 1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("APEX Mixed Severity Rules Test")
public class SeverityMixedRulesTest {

    private static final Logger logger = LoggerFactory.getLogger(SeverityMixedRulesTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for mixed severity rules testing...");
        
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();
        
        logger.info("✅ APEX services initialized successfully");
    }

    @Test
    @DisplayName("Test individual rules with different severity levels")
    void testIndividualMixedSeverityRules() throws Exception {
        logger.info("=== Testing Individual Mixed Severity Rules ===");
        
        // Load mixed severity rules configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityMixedRulesTest.yaml"
        );
        
        assertNotNull(config, "Configuration should be loaded");
        assertEquals("Mixed Severity Rules Test", config.getMetadata().getName());
        assertEquals(12, config.getRules().size(), "Should have exactly 12 rules");
        assertEquals(6, config.getRuleGroups().size(), "Should have exactly 6 rule groups");
        
        logger.info("✅ Configuration loaded: {} rules, {} rule groups", 
            config.getRules().size(), config.getRuleGroups().size());
        
        // Create RulesEngine
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(engine, "RulesEngine should be created");
        
        // Test ERROR severity rules
        testErrorSeverityRules(engine);
        
        // Test WARNING severity rules
        testWarningSeverityRules(engine);
        
        // Test INFO severity rules
        testInfoSeverityRules(engine);
        
        logger.info("✅ All individual mixed severity tests passed");
    }

    private void testErrorSeverityRules(RulesEngine engine) {
        logger.info("--- Testing ERROR Severity Rules ---");
        
        // Test account balance critical (ERROR)
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", -500.0); // Negative balance - should trigger ERROR
        
        Rule rule = engine.getConfiguration().getRuleById("account-balance-critical");
        assertNotNull(rule, "Account balance critical rule should be found");
        
        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Account balance critical rule should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Account balance critical rule should have ERROR severity");
        
        logger.info("✅ ERROR severity rule test passed: {}", result.getSeverity());
    }

    private void testWarningSeverityRules(RulesEngine engine) {
        logger.info("--- Testing WARNING Severity Rules ---");
        
        // Test account balance warning (WARNING)
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", 500.0); // Low balance - should trigger WARNING
        
        Rule rule = engine.getConfiguration().getRuleById("account-balance-warning");
        assertNotNull(rule, "Account balance warning rule should be found");
        
        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Account balance warning rule should be triggered");
        assertEquals("WARNING", result.getSeverity(), "Account balance warning rule should have WARNING severity");
        
        logger.info("✅ WARNING severity rule test passed: {}", result.getSeverity());
    }

    private void testInfoSeverityRules(RulesEngine engine) {
        logger.info("--- Testing INFO Severity Rules ---");
        
        // Test account balance info (INFO)
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", 5000.0); // Healthy balance - should trigger INFO
        
        Rule rule = engine.getConfiguration().getRuleById("account-balance-info");
        assertNotNull(rule, "Account balance info rule should be found");
        
        RuleResult result = engine.executeRule(rule, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isTriggered(), "Account balance info rule should be triggered");
        assertEquals("INFO", result.getSeverity(), "Account balance info rule should have INFO severity");
        
        logger.info("✅ INFO severity rule test passed: {}", result.getSeverity());
    }

    @Test
    @DisplayName("Test rule groups with single severity types")
    void testSingleSeverityRuleGroups() throws Exception {
        logger.info("=== Testing Single Severity Rule Groups ===");

        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityMixedRulesTest.yaml"
        );
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);

        // Test ERROR-only rule group
        testErrorOnlyGroup(engine);

        // Test WARNING-only rule group
        testWarningOnlyGroup(engine);

        // Test INFO-only rule group
        testInfoOnlyGroup(engine);

        logger.info("✅ All single severity rule group tests passed");
    }

    private void testErrorOnlyGroup(RulesEngine engine) {
        logger.info("--- Testing ERROR-Only Rule Group ---");

        // Test data that triggers all ERROR rules in the group
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", -100.0); // < 0 - triggers account-balance-critical
        testData.put("age", 16); // < 18 - triggers customer-age-error
        testData.put("amount", 150000.0); // > 100000 - triggers transaction-amount-critical
        testData.put("status", "BLOCKED"); // == 'BLOCKED' - triggers status-error

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("error-only-group");
        assertNotNull(ruleGroup, "ERROR-only rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "ERROR-only AND group should be triggered when all rules pass");
        assertEquals("ERROR", result.getSeverity(), "ERROR-only group should have ERROR severity");

        logger.info("✅ ERROR-only rule group test passed with severity: {}", result.getSeverity());
    }

    private void testWarningOnlyGroup(RulesEngine engine) {
        logger.info("--- Testing WARNING-Only Rule Group ---");

        // Test data that triggers at least one WARNING rule in the OR group
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", 500.0); // < 1000 && >= 0 - triggers account-balance-warning

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("warning-only-group");
        assertNotNull(ruleGroup, "WARNING-only rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "WARNING-only OR group should be triggered");
        assertEquals("WARNING", result.getSeverity(), "WARNING-only group should have WARNING severity");

        logger.info("✅ WARNING-only rule group test passed with severity: {}", result.getSeverity());
    }

    private void testInfoOnlyGroup(RulesEngine engine) {
        logger.info("--- Testing INFO-Only Rule Group ---");

        // Test data that triggers all INFO rules in the group
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", 5000.0); // >= 1000 - triggers account-balance-info
        testData.put("age", 30); // >= 18 && < 65 - triggers customer-age-info
        testData.put("amount", 25000.0); // <= 50000 - triggers transaction-amount-info
        testData.put("status", "ACTIVE"); // == 'ACTIVE' - triggers status-info

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("info-only-group");
        assertNotNull(ruleGroup, "INFO-only rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "INFO-only AND group should be triggered when all rules pass");
        assertEquals("INFO", result.getSeverity(), "INFO-only group should have INFO severity");

        logger.info("✅ INFO-only rule group test passed with severity: {}", result.getSeverity());
    }

    @Test
    @DisplayName("Test rule groups with mixed severity types")
    void testMixedSeverityRuleGroups() throws Exception {
        logger.info("=== Testing Mixed Severity Rule Groups ===");

        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityMixedRulesTest.yaml"
        );
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);

        // Test ERROR and WARNING mixed group
        testErrorWarningMixedGroup(engine);

        // Test WARNING and INFO mixed group
        testWarningInfoMixedGroup(engine);

        // Test all severities mixed group
        testAllSeveritiesMixedGroup(engine);

        logger.info("✅ All mixed severity rule group tests passed");
    }

    private void testErrorWarningMixedGroup(RulesEngine engine) {
        logger.info("--- Testing ERROR-WARNING Mixed Rule Group ---");

        // Test data that triggers ERROR rule first in OR group
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", -200.0); // < 0 - triggers account-balance-critical (ERROR)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("error-warning-mixed-group");
        assertNotNull(ruleGroup, "ERROR-WARNING mixed rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "ERROR-WARNING mixed OR group should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Mixed group should use severity from first matching rule (ERROR)");

        logger.info("✅ ERROR-WARNING mixed rule group test passed with severity: {}", result.getSeverity());
    }

    private void testWarningInfoMixedGroup(RulesEngine engine) {
        logger.info("--- Testing WARNING-INFO Mixed Rule Group ---");

        // Test data that triggers all rules in the AND group
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", 500.0); // < 1000 && >= 0 - triggers account-balance-warning (WARNING)
        testData.put("age", 30); // >= 18 && < 65 - triggers customer-age-info (INFO)
        testData.put("amount", 25000.0); // <= 50000 - triggers transaction-amount-info (INFO)
        testData.put("status", "SUSPENDED"); // == 'SUSPENDED' - triggers status-warning (WARNING)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("warning-info-mixed-group");
        assertNotNull(ruleGroup, "WARNING-INFO mixed rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "WARNING-INFO mixed AND group should be triggered when all rules pass");
        assertEquals("WARNING", result.getSeverity(), "Mixed AND group should use highest severity (WARNING)");

        logger.info("✅ WARNING-INFO mixed rule group test passed with severity: {}", result.getSeverity());
    }

    private void testAllSeveritiesMixedGroup(RulesEngine engine) {
        logger.info("--- Testing All Severities Mixed Rule Group ---");

        // Test data that triggers ERROR rule first in OR group
        Map<String, Object> testData = new HashMap<>();
        testData.put("balance", -100.0); // < 0 - triggers account-balance-critical (ERROR)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("all-severities-mixed-group");
        assertNotNull(ruleGroup, "All severities mixed rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "All severities mixed OR group should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Mixed group should use severity from first matching rule (ERROR)");

        logger.info("✅ All severities mixed rule group test passed with severity: {}", result.getSeverity());
    }
}
