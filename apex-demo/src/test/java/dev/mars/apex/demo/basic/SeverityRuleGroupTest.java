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
 * Severity Rule Group Test - Tests rule groups containing rules with different severity levels
 * to verify severity aggregation and inheritance behavior.
 * 
 * This test validates:
 * - Rule groups with single severity types (ERROR-only, WARNING-only, INFO-only)
 * - Rule groups with mixed severity types and proper aggregation
 * - AND group severity aggregation (highest severity of all triggered rules)
 * - OR group severity aggregation (severity of first matching rule)
 * - Complex nested severity scenarios with multiple rule combinations
 * - Edge cases like single-rule groups and large mixed groups
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
@DisplayName("APEX Severity Rule Group Test")
public class SeverityRuleGroupTest {

    private static final Logger logger = LoggerFactory.getLogger(SeverityRuleGroupTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for severity rule group testing...");
        
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();
        
        logger.info("✅ APEX services initialized successfully");
    }

    @Test
    @DisplayName("Test rule groups with single severity types")
    void testSingleSeverityRuleGroups() throws Exception {
        logger.info("=== Testing Single Severity Rule Groups ===");
        
        // Load rule group mixed severity configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityRuleGroupTest.yaml"
        );
        
        assertNotNull(config, "Configuration should be loaded");
        assertEquals("Rule Groups Mixed Severity Test", config.getMetadata().getName());
        assertEquals(9, config.getRules().size(), "Should have exactly 9 rules");
        assertEquals(10, config.getRuleGroups().size(), "Should have exactly 10 rule groups");
        
        logger.info("✅ Configuration loaded: {} rules, {} rule groups", 
            config.getRules().size(), config.getRuleGroups().size());
        
        // Create RulesEngine
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(engine, "RulesEngine should be created");
        
        // Test ERROR-only rule group
        testErrorOnlyRuleGroup(engine);
        
        // Test WARNING-only rule group
        testWarningOnlyRuleGroup(engine);
        
        // Test INFO-only rule group
        testInfoOnlyRuleGroup(engine);
        
        logger.info("✅ All single severity rule group tests passed");
    }

    private void testErrorOnlyRuleGroup(RulesEngine engine) {
        logger.info("--- Testing ERROR-Only Rule Group ---");
        
        // Test data that triggers all ERROR rules in the AND group
        Map<String, Object> testData = new HashMap<>();
        testData.put("securityFlags", List.of("BREACH", "SUSPICIOUS")); // contains 'BREACH' - triggers security-breach-detected
        testData.put("fraudScore", 95); // > 90 - triggers fraud-pattern-detected
        testData.put("complianceScore", 45); // < 60 - triggers regulatory-violation
        
        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("critical-security-checks");
        assertNotNull(ruleGroup, "Critical security checks rule group should be found by ID");
        
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "ERROR-only AND group should be triggered when all rules pass");
        assertEquals("ERROR", result.getSeverity(), "ERROR-only group should have ERROR severity");
        
        logger.info("✅ ERROR-only rule group test passed with severity: {}", result.getSeverity());
    }

    private void testWarningOnlyRuleGroup(RulesEngine engine) {
        logger.info("--- Testing WARNING-Only Rule Group ---");
        
        // Test data that triggers at least one WARNING rule in the OR group
        Map<String, Object> testData = new HashMap<>();
        testData.put("activityScore", 80); // > 75 - triggers unusual-activity-pattern
        
        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("risk-assessment-warnings");
        assertNotNull(ruleGroup, "Risk assessment warnings rule group should be found by ID");
        
        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "WARNING-only OR group should be triggered");
        assertEquals("WARNING", result.getSeverity(), "WARNING-only group should have WARNING severity");
        
        logger.info("✅ WARNING-only rule group test passed with severity: {}", result.getSeverity());
    }

    private void testInfoOnlyRuleGroup(RulesEngine engine) {
        logger.info("--- Testing INFO-Only Rule Group ---");
        
        // Test data that triggers all INFO rules in the AND group
        Map<String, Object> testData = new HashMap<>();
        testData.put("transactionId", "TXN-12345"); // not null - triggers transaction-processed
        testData.put("profileUpdated", true); // == true - triggers customer-profile-updated
        testData.put("auditEnabled", true); // == true - triggers audit-trail-logged
        
        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("processing-information");
        assertNotNull(ruleGroup, "Processing information rule group should be found by ID");
        
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
            "src/test/java/dev/mars/apex/demo/basic/SeverityRuleGroupTest.yaml"
        );
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);

        // Test ERROR and WARNING mixed in AND group
        testErrorWarningMixedAndGroup(engine);

        // Test WARNING and INFO mixed in OR group
        testWarningInfoMixedOrGroup(engine);

        // Test all three severities in AND group
        testAllSeveritiesMixedAndGroup(engine);

        logger.info("✅ All mixed severity rule group tests passed");
    }

    private void testErrorWarningMixedAndGroup(RulesEngine engine) {
        logger.info("--- Testing ERROR-WARNING Mixed AND Group ---");

        // Test data that triggers both ERROR and WARNING rules in the AND group
        Map<String, Object> testData = new HashMap<>();
        testData.put("securityFlags", List.of("BREACH")); // contains 'BREACH' - triggers security-breach-detected (ERROR)
        testData.put("activityScore", 80); // > 75 - triggers unusual-activity-pattern (WARNING)
        testData.put("complianceScore", 45); // < 60 - triggers regulatory-violation (ERROR)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("security-and-risk-checks");
        assertNotNull(ruleGroup, "Security and risk checks rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "ERROR-WARNING mixed AND group should be triggered when all rules pass");
        assertEquals("ERROR", result.getSeverity(), "Mixed AND group should use highest severity (ERROR)");

        logger.info("✅ ERROR-WARNING mixed AND group test passed with severity: {}", result.getSeverity());
    }

    private void testWarningInfoMixedOrGroup(RulesEngine engine) {
        logger.info("--- Testing WARNING-INFO Mixed OR Group ---");

        // Test data that triggers WARNING rule first in the OR group
        Map<String, Object> testData = new HashMap<>();
        testData.put("riskLevel", "HIGH"); // == 'HIGH' - triggers high-risk-transaction (WARNING)
        testData.put("transactionId", "TXN-67890"); // not null - would also trigger transaction-processed (INFO)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("monitoring-and-info");
        assertNotNull(ruleGroup, "Monitoring and info rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "WARNING-INFO mixed OR group should be triggered");
        assertEquals("WARNING", result.getSeverity(), "Mixed OR group should use severity from first matching rule (WARNING)");

        logger.info("✅ WARNING-INFO mixed OR group test passed with severity: {}", result.getSeverity());
    }

    private void testAllSeveritiesMixedAndGroup(RulesEngine engine) {
        logger.info("--- Testing All Severities Mixed AND Group ---");

        // Test data that triggers ERROR, WARNING, and INFO rules in the AND group
        Map<String, Object> testData = new HashMap<>();
        testData.put("fraudScore", 95); // > 90 - triggers fraud-pattern-detected (ERROR)
        testData.put("activityScore", 80); // > 75 - triggers unusual-activity-pattern (WARNING)
        testData.put("transactionId", "TXN-MIXED"); // not null - triggers transaction-processed (INFO)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("comprehensive-validation");
        assertNotNull(ruleGroup, "Comprehensive validation rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "All severities mixed AND group should be triggered when all rules pass");
        assertEquals("ERROR", result.getSeverity(), "Mixed AND group should use highest severity (ERROR)");

        logger.info("✅ All severities mixed AND group test passed with severity: {}", result.getSeverity());
    }

    @Test
    @DisplayName("Test edge cases and complex scenarios")
    void testEdgeCasesAndComplexScenarios() throws Exception {
        logger.info("=== Testing Edge Cases and Complex Scenarios ===");

        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/basic/SeverityRuleGroupTest.yaml"
        );
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);

        // Test single rule group
        testSingleRuleGroup(engine);

        // Test large mixed group (stress test)
        testLargeMixedGroup(engine);

        // Test OR group with all severities (first match wins)
        testAllSeveritiesOrGroup(engine);

        logger.info("✅ All edge case and complex scenario tests passed");
    }

    private void testSingleRuleGroup(RulesEngine engine) {
        logger.info("--- Testing Single Rule Group ---");

        // Test data that triggers the single ERROR rule
        Map<String, Object> testData = new HashMap<>();
        testData.put("securityFlags", List.of("BREACH", "ALERT")); // contains 'BREACH' - triggers security-breach-detected

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("single-critical-rule");
        assertNotNull(ruleGroup, "Single critical rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "Single rule group should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Single ERROR rule group should have ERROR severity");

        logger.info("✅ Single rule group test passed with severity: {}", result.getSeverity());
    }

    private void testLargeMixedGroup(RulesEngine engine) {
        logger.info("--- Testing Large Mixed Group (Stress Test) ---");

        // Test data that triggers the first rule in the large OR group
        Map<String, Object> testData = new HashMap<>();
        testData.put("securityFlags", List.of("BREACH")); // contains 'BREACH' - triggers security-breach-detected (first rule, ERROR)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("full-validation-suite");
        assertNotNull(ruleGroup, "Full validation suite rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "Large mixed OR group should be triggered");
        assertEquals("ERROR", result.getSeverity(), "Large mixed OR group should use severity from first matching rule (ERROR)");

        logger.info("✅ Large mixed group test passed with severity: {}", result.getSeverity());
    }

    private void testAllSeveritiesOrGroup(RulesEngine engine) {
        logger.info("--- Testing All Severities OR Group (First Match Wins) ---");

        // Test data that triggers ERROR rule first in the OR group
        Map<String, Object> testData = new HashMap<>();
        testData.put("complianceScore", 50); // < 60 - triggers regulatory-violation (ERROR, first in list)
        testData.put("kycStatus", "INCOMPLETE"); // would also trigger incomplete-kyc-data (WARNING)
        testData.put("auditEnabled", true); // would also trigger audit-trail-logged (INFO)

        // Get the rule group by ID and execute it
        RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("any-issue-detection");
        assertNotNull(ruleGroup, "Any issue detection rule group should be found by ID");

        RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), testData);
        assertNotNull(result, "Rule group result should not be null");
        assertTrue(result.isTriggered(), "All severities OR group should be triggered");
        assertEquals("ERROR", result.getSeverity(), "All severities OR group should use severity from first matching rule (ERROR)");

        logger.info("✅ All severities OR group test passed with severity: {}", result.getSeverity());
    }
}
