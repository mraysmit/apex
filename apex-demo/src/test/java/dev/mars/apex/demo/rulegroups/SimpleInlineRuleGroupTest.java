package dev.mars.apex.demo.rulegroups;

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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Simple Inline Rule Group Test.
 *
 * Tests inline rule-group-id references within the same YAML file.
 * Demonstrates:
 * - 2 simple rules (one passes, one fails)
 * - Base rule group containing both rules
 * - Composite rule group that references the base group by ID
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Inline Rule Group Test")
public class SimpleInlineRuleGroupTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInlineRuleGroupTest.class);
    
    private RulesEngine rulesEngine;
    private Map<String, Object> testContext;

    @BeforeEach
    void setUp() throws YamlConfigurationException {
        LOGGER.info("Setting up Simple Inline Rule Group Test...");

        // Create the rules engine using the correct API
        YamlRulesEngineService service = new YamlRulesEngineService();
        rulesEngine = service.createRulesEngineFromFile(
            "src/test/java/dev/mars/apex/demo/rulegroups/SimpleInlineRuleGroupTest-rules.yaml"
        );

        // Create test context (empty for this simple test)
        testContext = new HashMap<>();

        LOGGER.info("Setup complete - Rules engine created successfully");
    }

    @Test
    @DisplayName("Test Base Rule Group (2 rules: 1 pass, 1 fail)")
    void testBaseRuleGroup() {
        LOGGER.info("Testing Base Rule Group");

        // Get the base rule group
        RuleGroup baseGroup = rulesEngine.getConfiguration().getRuleGroupById("base-validation");
        assertNotNull(baseGroup, "Base rule group should exist");

        // Execute the base rule group
        RuleResult result = rulesEngine.executeRuleGroupsList(List.of(baseGroup), testContext);

        // Verify results
        assertNotNull(result, "Result should not be null");
        // AND group with one false rule should fail
        assertFalse(result.isTriggered(), "Base group should fail (AND with one false rule)");

        LOGGER.info("✅ Base rule group test passed - group failed as expected (AND logic)");
    }

    @Test
    @DisplayName("Test Composite Rule Group (references base group by ID)")
    void testCompositeRuleGroup() {
        LOGGER.info("Testing Composite Rule Group with Inline Reference");

        // Get the composite rule group
        RuleGroup compositeGroup = rulesEngine.getConfiguration().getRuleGroupById("composite-validation");
        assertNotNull(compositeGroup, "Composite rule group should exist");

        // Execute the composite rule group
        RuleResult result = rulesEngine.executeRuleGroupsList(List.of(compositeGroup), testContext);

        // Verify results
        assertNotNull(result, "Result should not be null");
        // OR group with one true rule should pass
        assertTrue(result.isTriggered(), "Composite group should pass (OR with one true rule)");

        LOGGER.info("✅ Composite rule group test passed - group passed as expected (OR logic)");
        LOGGER.info("Composite rule group successfully referenced base group by ID");
    }

    @Test
    @DisplayName("Test Rule Group Registry")
    void testRuleGroupRegistry() {
        LOGGER.info("Testing Rule Group Registry");

        // Verify both rule groups are registered
        RuleGroup baseGroup = rulesEngine.getConfiguration().getRuleGroupById("base-validation");
        assertNotNull(baseGroup, "Base rule group should be registered");
        assertEquals("base-validation", baseGroup.getId(), "Base group ID should match");

        RuleGroup compositeGroup = rulesEngine.getConfiguration().getRuleGroupById("composite-validation");
        assertNotNull(compositeGroup, "Composite rule group should be registered");
        assertEquals("composite-validation", compositeGroup.getId(), "Composite group ID should match");

        // Verify rule counts
        assertEquals(2, baseGroup.getRules().size(), "Base group should have 2 rules");
        assertEquals(2, compositeGroup.getRules().size(), "Composite group should have 2 rules (from reference)");

        LOGGER.info("✅ Rule group registry test passed");
        LOGGER.info("Both rule groups properly registered: base={} rules, composite={} rules",
            baseGroup.getRules().size(), compositeGroup.getRules().size());
    }

    @Test
    @DisplayName("Test Inline Rule Group Reference Resolution")
    void testInlineReferenceResolution() {
        LOGGER.info("Testing Inline Rule Group Reference Resolution");

        // Get the composite group
        RuleGroup compositeGroup = rulesEngine.getConfiguration().getRuleGroupById("composite-validation");
        assertNotNull(compositeGroup, "Composite group should exist");

        // Verify that the composite group contains the rules from the referenced base group
        assertEquals(2, compositeGroup.getRules().size(), "Composite group should have 2 rules from base group");

        // Verify the specific rules are present
        boolean hasRule1 = compositeGroup.getRules().stream()
            .anyMatch(rule -> "simple-rule-1".equals(rule.getId()));
        boolean hasRule2 = compositeGroup.getRules().stream()
            .anyMatch(rule -> "simple-rule-2".equals(rule.getId()));

        assertTrue(hasRule1, "Composite group should contain simple-rule-1 from base group");
        assertTrue(hasRule2, "Composite group should contain simple-rule-2 from base group");

        LOGGER.info("✅ Inline reference resolution test passed");
        LOGGER.info("Inline rule-group-id reference successfully resolved: base-validation → composite-validation");
    }

    @Test
    @DisplayName("Integration Test: Full Workflow")
    void testFullWorkflow() {
        LOGGER.info("Testing Full Workflow");

        // Get both rule groups
        RuleGroup baseGroup = rulesEngine.getConfiguration().getRuleGroupById("base-validation");
        RuleGroup compositeGroup = rulesEngine.getConfiguration().getRuleGroupById("composite-validation");

        assertNotNull(baseGroup, "Base group should exist");
        assertNotNull(compositeGroup, "Composite group should exist");

        // Test that we can execute both groups independently
        RuleResult baseResult = rulesEngine.executeRuleGroupsList(List.of(baseGroup), testContext);
        RuleResult compositeResult = rulesEngine.executeRuleGroupsList(List.of(compositeGroup), testContext);

        assertNotNull(baseResult, "Base result should not be null");
        assertNotNull(compositeResult, "Composite result should not be null");

        // Base group (AND) should fail, composite group (OR) should pass
        assertFalse(baseResult.isTriggered(), "Base group (AND) should fail");
        assertTrue(compositeResult.isTriggered(), "Composite group (OR) should pass");

        // Both groups should have the same rules (since composite references base)
        assertEquals(baseGroup.getRules().size(), compositeGroup.getRules().size(),
            "Both groups should have same number of rules");

        LOGGER.info("✅ Full workflow test passed");
        LOGGER.info("✅ SUCCESS: Inline rule-group-id references working correctly!");
        LOGGER.info("📋 SUMMARY: 2 rules, 2 rule groups, 1 inline reference - All working!");
    }
}
