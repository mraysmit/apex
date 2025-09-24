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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
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
 * Test for single rule that checks if an attribute value is greater than 100.
 * Demonstrates basic numeric comparison with incoming dataset containing 3 attributes.
 */
public class ValueThresholdRuleTest {

    private static final Logger logger = LoggerFactory.getLogger(ValueThresholdRuleTest.class);

    private YamlConfigurationLoader yamlLoader;
    private YamlRulesEngineService rulesEngineService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up APEX services for value threshold rule test...");

        // Initialize APEX services
        yamlLoader = new YamlConfigurationLoader();
        rulesEngineService = new YamlRulesEngineService();

        logger.info("✓ APEX services initialized successfully");
    }

    @Test
    @DisplayName("Test single rule checking value greater than 100")
    void testValueThresholdRule() throws Exception {
        logger.info("=== Testing Value Threshold Rule ===");
        
        // Load YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/ValueThresholdRuleTest.yaml");
        assertNotNull(config, "Configuration should be loaded");
        logger.info("✓ Configuration loaded: {} rules", config.getRules().size());
        
        // Create RulesEngine
        RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(engine, "RulesEngine should be created");
        
        // Get the rule from the configuration
        var rule = engine.getConfiguration().getRuleById("value-threshold-check");
        assertNotNull(rule, "Rule should be found in configuration");

        // Test Case 1: Value greater than 100 (should pass)
        logger.info("--- Test Case 1: Amount = 150 (should pass) ---");
        Map<String, Object> testData1 = Map.of(
            "amount", 150.0,
            "currency", "USD",
            "customerId", "CUST001"
        );

        RuleResult result1 = engine.executeRule(rule, testData1);
        assertNotNull(result1, "Result should not be null");
        assertTrue(result1.isTriggered(), "Rule should be triggered for amount > 100");
        logger.info("✓ Test Case 1 passed: {}", result1.getMessage());
        
        // Test Case 2: Value less than 100 (should not pass)
        logger.info("--- Test Case 2: Amount = 50 (should not pass) ---");
        Map<String, Object> testData2 = Map.of(
            "amount", 50.0,
            "currency", "EUR",
            "customerId", "CUST002"
        );

        RuleResult result2 = engine.executeRule(rule, testData2);
        assertNotNull(result2, "Result should not be null");
        assertFalse(result2.isTriggered(), "Rule should not be triggered for amount <= 100");
        logger.info("✓ Test Case 2 passed: Rule correctly rejected amount <= 100");
        
        // Test Case 3: Value exactly 100 (should not pass)
        logger.info("--- Test Case 3: Amount = 100 (should not pass) ---");
        Map<String, Object> testData3 = Map.of(
            "amount", 100.0,
            "currency", "GBP",
            "customerId", "CUST003"
        );

        RuleResult result3 = engine.executeRule(rule, testData3);
        assertNotNull(result3, "Result should not be null");
        assertFalse(result3.isTriggered(), "Rule should not be triggered for amount = 100");
        logger.info("✓ Test Case 3 passed: Rule correctly rejected amount = 100");
        
        // Test Case 4: Missing amount attribute (should not pass)
        logger.info("--- Test Case 4: Missing amount (should not pass) ---");
        Map<String, Object> testData4 = Map.of(
            "currency", "CAD",
            "customerId", "CUST004"
        );

        RuleResult result4 = engine.executeRule(rule, testData4);
        assertNotNull(result4, "Result should not be null");
        assertFalse(result4.isTriggered(), "Rule should not be triggered when amount is missing");
        logger.info("✓ Test Case 4 passed: Rule correctly handled missing amount");
        
        logger.info("✓ All value threshold rule tests passed");
    }
}
