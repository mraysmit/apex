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

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test for basic YAML rule group processing.
 * Demonstrates rule groups with AND/OR logic using hardcoded true/false conditions.
 *
 * @author Mark Andrew Ray-Smith
 */
class SimpleBasicYamlRuleGroupProcessingTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleBasicYamlRuleGroupProcessingTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for simple rule group processing tests...");

        this.yamlLoader = new YamlConfigurationLoader();
        this.rulesEngineService = new YamlRulesEngineService();

        logger.info("✓ APEX services initialized successfully");
    }

    /**
     * Test basic rule group processing with combined configuration.
     * Uses a single YAML file containing both rules and rule groups.
     */
    @Test
    @DisplayName("Test simple rule group processing")
    void testSimpleRuleGroupProcessing() {
        logger.info("=== Testing Simple Rule Group Processing ===");
        
        try {
            // Load combined configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/combined-config.yaml"
            );
            
            assertNotNull(config, "Configuration should be loaded");
            logger.info("✓ Configuration loaded: {} rules, {} rule groups", 
                config.getRules().size(), config.getRuleGroups().size());
            
            // Create RulesEngine
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            
            // Test data (empty for hardcoded rule conditions)
            Map<String, Object> testData = Map.of();
            
            // Test 1: AND group with true rules (should pass)
            logger.info("Testing AND group with true rules...");
            var andGroup = engine.getConfiguration().getRuleGroupById("separate-and-group");
            assertNotNull(andGroup, "AND group should be found");
            
            RuleResult andResult = engine.executeRuleGroupsList(java.util.List.of(andGroup), testData);
            assertNotNull(andResult, "Result should not be null");
            assertTrue(andResult.isTriggered(), "AND group with all true rules should pass");
            logger.info("✓ AND group passed");
            
            // Test 2: OR group with mixed rules (should pass)
            logger.info("Testing OR group with mixed rules...");
            var orGroup = engine.getConfiguration().getRuleGroupById("separate-or-group");
            assertNotNull(orGroup, "OR group should be found");
            
            RuleResult orResult = engine.executeRuleGroupsList(java.util.List.of(orGroup), testData);
            assertNotNull(orResult, "Result should not be null");
            assertTrue(orResult.isTriggered(), "OR group with at least one true rule should pass");
            logger.info("✓ OR group passed");
            
            // Test 3: AND group with mixed rules (should fail)
            logger.info("Testing AND group with mixed rules...");
            var andMixedGroup = engine.getConfiguration().getRuleGroupById("separate-and-mixed-group");
            assertNotNull(andMixedGroup, "AND mixed group should be found");
            
            RuleResult andMixedResult = engine.executeRuleGroupsList(java.util.List.of(andMixedGroup), testData);
            assertNotNull(andMixedResult, "Result should not be null");
            assertFalse(andMixedResult.isTriggered(), "AND group with mixed rules should fail");
            logger.info("✓ AND group failed as expected");
            
            logger.info("✓ All simple rule group tests passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process configuration: {}", e.getMessage());
            fail("Failed to load or process configuration: " + e.getMessage());
        }
    }

    /**
     * Test rule group processing with separate files.
     * Demonstrates automatic rule reference resolution.
     */
    @Test
    @DisplayName("Test rule group processing with separate files")
    void testRuleGroupProcessingWithSeparateFiles() {
        logger.info("=== Testing Rule Group Processing with Separate Files ===");
        
        try {
            // Load rule groups configuration (references external rules file)
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/rule-groups.yaml"
            );
            
            assertNotNull(config, "Configuration should be loaded");
            logger.info("✓ Configuration with automatic rule references loaded: {} rules, {} rule groups",
                config.getRules().size(), config.getRuleGroups().size());
            
            // Create RulesEngine
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");
            logger.info("✓ Automatic rule reference resolution successful");
            
            // Test data (empty for hardcoded rule conditions)
            Map<String, Object> testData = Map.of();
            
            // Test AND group (should pass)
            var andGroup = engine.getConfiguration().getRuleGroupById("separate-and-group");
            assertNotNull(andGroup, "AND group should be found");
            
            RuleResult result = engine.executeRuleGroupsList(java.util.List.of(andGroup), testData);
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isTriggered(), "AND group should pass");
            
            logger.info("✓ Separate files test passed");
            
        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load or process configuration: {}", e.getMessage());
            fail("Failed to load or process configuration: " + e.getMessage());
        }
    }
}
