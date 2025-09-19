package dev.mars.apex.demo.basic.rules;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic YAML Rule Group Processing Tests - Alternative Implementation.
 * 
 * This test class demonstrates APEX rule group processing using the YAML files
 * in the basic-rules directory. It tests both combined configuration and 
 * separate file loading approaches.
 * 
 * YAML Files Used:
 * - combined-config.yaml: Complete configuration with rules and rule groups
 * - rules.yaml: Rules-only configuration
 * - rule-groups.yaml: Rule groups-only configuration
 * 
 * Test Coverage:
 * - Combined YAML configuration loading and processing
 * - Separate file loading with rules and rule groups
 * - AND/OR rule group logic validation
 * - Rule execution and result validation
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Basic YAML Rule Group Processing Tests - Alternative")
public class BasicYamlRuleGroupProcessingATest {

    private static final Logger logger = LoggerFactory.getLogger(BasicYamlRuleGroupProcessingATest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for basic rule group processing tests...");
        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();
        logger.info("✅ APEX services initialized successfully");
    }

    /**
     * Test basic rule group processing using combined configuration.
     * Uses: combined-config.yaml
     * 
     * This test validates:
     * - Loading combined YAML configuration with rules and rule groups
     * - AND group logic with true rules (should pass)
     * - OR group logic with mixed rules (should pass)
     * - AND group logic with mixed rules (should fail)
     */
    @Test
    @DisplayName("Test basic rule group processing with combined configuration")
    void testBasicRuleGroupProcessing() {
        logger.info("=== Testing Basic Rule Group Processing with Combined Configuration ===");
        
        try {
            // Load combined configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic-rules/combined-config.yaml"
            );
            
            assertNotNull(config, "Configuration should be loaded");
            assertEquals("Separate Rules Test - Combined Configuration", config.getMetadata().getName());
            assertEquals(3, config.getRules().size(), "Should have 3 rules");
            assertEquals(3, config.getRuleGroups().size(), "Should have 3 rule groups");
            
            logger.info("✅ Configuration loaded: {} rules, {} rule groups", 
                config.getRules().size(), config.getRuleGroups().size());
            
            // Create RulesEngine
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            
            // Test AND group with true rules (should pass)
            testAndGroupWithTrueRules(engine);
            
            // Test OR group with mixed rules (should pass)
            testOrGroupWithMixedRules(engine);
            
            // Test AND group with mixed rules (should fail)
            testAndGroupWithMixedRules(engine);
            
            logger.info("✅ All combined configuration tests passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process configuration: {}", e.getMessage());
            fail("Failed to load or process configuration: " + e.getMessage());
        }
    }

    /**
     * Test basic rule group processing using separate files.
     * Uses: rules.yaml + rule-groups.yaml
     * 
     * This test validates:
     * - Loading separate YAML files for rules and rule groups
     * - Merging configurations from multiple files
     * - Rule group processing with separated configuration
     */
    @Test
    @DisplayName("Test basic rule group processing with separate files")
    void testBasicRuleGroupProcessingWithSeparateFiles() {
        logger.info("=== Testing Basic Rule Group Processing with Separate Files ===");
        
        try {
            // Use APEX's automatic rule reference resolution
            // Load only the rule-groups.yaml file which contains rule-refs to rules.yaml
            // APEX will automatically resolve and load the referenced rules
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic-rules/rule-groups.yaml"
            );

            assertNotNull(config, "Configuration should be loaded");
            assertEquals("Separate Rules Test - Rule Groups Only", config.getMetadata().getName());

            // Verify that APEX automatically loaded the referenced rules
            assertNotNull(config.getRules(), "Rules should be automatically loaded via rule-refs");
            assertEquals(3, config.getRules().size(), "Should have 3 rules from referenced file");
            assertEquals(3, config.getRuleGroups().size(), "Should have 3 rule groups");

            logger.info("✅ Configuration with automatic rule references loaded: {} rules, {} rule groups",
                config.getRules().size(), config.getRuleGroups().size());

            // Create RulesEngine from the configuration with resolved references
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created from configuration with resolved references");

            // Verify the engine has the expected rules and rule groups
            assertNotNull(engine.getConfiguration().getRuleGroupById("separate-and-group"),
                "AND group should be found");
            assertNotNull(engine.getConfiguration().getRuleGroupById("separate-or-group"),
                "OR group should be found");
            assertNotNull(engine.getConfiguration().getRuleGroupById("separate-and-mixed-group"),
                "AND mixed group should be found");

            logger.info("✅ Automatic rule reference resolution successful");
            
            // Test rule group processing with merged configuration
            testAndGroupWithTrueRules(engine);
            testOrGroupWithMixedRules(engine);
            
            logger.info("✅ All separate files tests passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process separate configurations: {}", e.getMessage());
            fail("Failed to load or process separate configurations: " + e.getMessage());
        }
    }

    /**
     * Test AND group with true rules (should pass).
     */
    private void testAndGroupWithTrueRules(RulesEngine engine) {
        logger.info("Testing AND group with true rules...");
        
        RuleGroup andGroup = engine.getConfiguration().getRuleGroupById("separate-and-group");
        assertNotNull(andGroup, "AND group should be found");
        assertTrue(andGroup.isAndOperator(), "Should be AND operator");
        
        // Execute rule group - uses separate-rule-1 (true) and separate-rule-3 (true)
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(andGroup), testData);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "AND group with all true rules should pass");
        
        logger.info("✅ AND group with true rules passed: {}", 
            result.getMessage() != null ? result.getMessage() : "No message");
    }

    /**
     * Test OR group with mixed rules (should pass).
     */
    private void testOrGroupWithMixedRules(RulesEngine engine) {
        logger.info("Testing OR group with mixed rules...");
        
        RuleGroup orGroup = engine.getConfiguration().getRuleGroupById("separate-or-group");
        assertNotNull(orGroup, "OR group should be found");
        assertFalse(orGroup.isAndOperator(), "Should be OR operator");
        
        // Execute rule group - uses separate-rule-1 (true), separate-rule-2 (false), separate-rule-3 (true)
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(orGroup), testData);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "OR group with at least one true rule should pass");
        
        logger.info("✅ OR group with mixed rules passed: {}", 
            result.getMessage() != null ? result.getMessage() : "No message");
    }

    /**
     * Test AND group with mixed rules (should fail).
     */
    private void testAndGroupWithMixedRules(RulesEngine engine) {
        logger.info("Testing AND group with mixed rules...");
        
        RuleGroup andMixedGroup = engine.getConfiguration().getRuleGroupById("separate-and-mixed-group");
        assertNotNull(andMixedGroup, "AND mixed group should be found");
        assertTrue(andMixedGroup.isAndOperator(), "Should be AND operator");
        
        // Execute rule group - uses separate-rule-1 (true), separate-rule-2 (false), separate-rule-3 (true)
        Map<String, Object> testData = Map.of();
        RuleResult result = engine.executeRuleGroupsList(List.of(andMixedGroup), testData);
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "AND group with mixed rules should fail");
        
        logger.info("✅ AND group with mixed rules failed as expected: {}", 
            result.getMessage() != null ? result.getMessage() : "No message");
    }


}
