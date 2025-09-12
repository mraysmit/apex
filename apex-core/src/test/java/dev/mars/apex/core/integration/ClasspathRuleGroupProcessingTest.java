package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify if APEX automatically handles cross-file rule references
 * when loading from classpath, without using createRulesEngineFromMultipleFiles.
 * 
 * This test loads ONLY the rule groups file and checks if APEX can automatically
 * resolve rule references to rules defined in separate files.
 */
@DisplayName("Classpath Rule Group Processing Test")
class ClasspathRuleGroupProcessingTest {

    private YamlConfigurationLoader yamlLoader;
    private YamlRuleFactory ruleFactory;

    @BeforeEach
    void setUp() {
        yamlLoader = new YamlConfigurationLoader();
        ruleFactory = new YamlRuleFactory();
    }

    @Test
    @DisplayName("Should automatically resolve cross-file rule references from classpath")
    void testCrossFileRuleReferencesFromClasspath() {
        try {
            // Load ONLY the rule groups file using standard APEX loading
            // This should automatically resolve rule references if APEX supports it
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("rulegroups/customer-validation-groups.yaml");
            
            // Convert to RulesEngineConfiguration
            RulesEngineConfiguration engineConfig = ruleFactory.createRulesEngineConfiguration(config);
            
            // Create rules engine
            RulesEngine engine = new RulesEngine(engineConfig);
            
            // Test data - should pass validation
            Map<String, Object> validCustomer = new HashMap<>();
            validCustomer.put("age", 25);
            validCustomer.put("email", "test@example.com");
            
            // Get the rule group and execute it
            RuleGroup ruleGroup = engine.getConfiguration().getRuleGroupById("customer-validation");
            assertNotNull(ruleGroup, "Rule group should be found");

            // Execute rule group by creating a list and using executeRuleGroupsList
            RuleResult result = engine.executeRuleGroupsList(List.of(ruleGroup), validCustomer);

            // Verify result
            assertNotNull(result, "Rule result should not be null");
            System.out.println("Rule execution result: " + result);

            // Test data - should fail validation
            Map<String, Object> invalidCustomer = new HashMap<>();
            invalidCustomer.put("age", 16);  // Under 18
            invalidCustomer.put("email", "invalid-email");  // No @ symbol

            // Execute rule group with invalid data
            RuleResult invalidResult = engine.executeRuleGroupsList(List.of(ruleGroup), invalidCustomer);
            
            // Verify result
            assertNotNull(invalidResult, "Rule result should not be null for invalid data");
            System.out.println("Invalid data result: " + invalidResult);
            
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            fail("Cross-file rule reference resolution failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should load rule groups configuration successfully")
    void testRuleGroupsConfigurationLoading() {
        try {
            // Load the rule groups file
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath("rulegroups/customer-validation-groups.yaml");
            
            // Verify configuration loaded
            assertNotNull(config, "Configuration should not be null");
            assertNotNull(config.getMetadata(), "Metadata should not be null");
            assertEquals("Customer Validation Groups", config.getMetadata().getName());
            
            // Verify rule groups exist
            assertNotNull(config.getRuleGroups(), "Rule groups should not be null");
            assertFalse(config.getRuleGroups().isEmpty(), "Rule groups should not be empty");
            
            // Verify rule group has rule references
            assertEquals(1, config.getRuleGroups().size(), "Should have one rule group");
            assertEquals("customer-validation", config.getRuleGroups().get(0).getId());
            assertNotNull(config.getRuleGroups().get(0).getRuleIds(), "Rule IDs should not be null");
            assertEquals(2, config.getRuleGroups().get(0).getRuleIds().size(), "Should have 2 rule references");

            System.out.println("Rule groups configuration loaded successfully");
            System.out.println("Rule group: " + config.getRuleGroups().get(0).getId());
            System.out.println("Rule IDs: " + config.getRuleGroups().get(0).getRuleIds());
            
        } catch (Exception e) {
            System.err.println("Configuration loading failed: " + e.getMessage());
            e.printStackTrace();
            fail("Failed to load rule groups configuration: " + e.getMessage());
        }
    }
}
