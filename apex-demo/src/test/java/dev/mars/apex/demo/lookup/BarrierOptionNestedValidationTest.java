package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BarrierOptionNestedValidationTest - Validates APEX Nested Validation Rules
 *
 * PURPOSE: Prove that APEX can validate complex nested structures through:
 * - APEX rules engine processing 2+ level nested fields
 * - APEX business rule validation on nested data values
 * - APEX validation rule condition evaluation using RuleResult pattern
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 *  Count validation rules in YAML - 3 validation rules expected
 *  Verify RuleResult for each rule individually
 *  Check EVERY validation condition - Test data triggers ALL 3 conditions
 *  Validate EVERY business rule - Test validation logic on nested data
 *  Assert ALL RuleResult properties - Every rule has corresponding RuleResult validation
 *
 * ALL VALIDATION LOGIC IS IN APEX YAML FILES - NO CUSTOM JAVA LOGIC
 * Tests validate APEX functionality using established RuleResult patterns
 *
 * Following established patterns from SimpleAgeValidationTest and RestApiIntegrationTest
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Barrier Option Nested Validation Rules Tests")
public class BarrierOptionNestedValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(BarrierOptionNestedValidationTest.class);

    @Test
    @DisplayName("Test Barrier vs Strike Price Validation Rule - Valid Case")
    void testBarrierStrikeValidationRuleValid() {
        logger.info("=== Testing Barrier vs Strike Price Validation Rule - Valid Case ===");

        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/BarrierOptionNestedValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");
            assertEquals("APEX Nested Validation Rules", config.getMetadata().getName());
            assertEquals(3, config.getRules().size(), "Should have exactly 3 validation rules");

            // Create RulesEngine
            YamlRulesEngineService rulesEngineService = new YamlRulesEngineService();
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");

            // Get the barrier vs strike validation rule
            Rule barrierStrikeRule = engine.getConfiguration().getRuleById("nested-barrier-strike-validation");
            assertNotNull(barrierStrikeRule, "Barrier strike validation rule should be found");

            // Test data: Valid case - barrier level (2300) > strike price (2150)
            Map<String, Object> validData = createValidBarrierStrikeData();
            logger.info("Testing with barrier level 2300 > strike price 2150 (valid)");

            // Execute rule and validate results
            RuleResult result = engine.executeRule(barrierStrikeRule, validData);
            assertNotNull(result, "RuleResult should not be null");

            // Demonstrate RuleResult API methods following established patterns
            logger.info("=== RuleResult API Methods - Valid Case ===");
            logger.info("result.isTriggered(): {}", result.isTriggered());
            logger.info("result.isSuccess(): {}", result.isSuccess());
            logger.info("result.getMessage(): {}", result.getMessage());
            logger.info("result.getRuleName(): {}", result.getRuleName());
            logger.info("result.hasFailures(): {}", result.hasFailures());

            // Business logic validation: Valid barrier vs strike should pass
            assertTrue(result.isTriggered(), "Barrier strike rule should be triggered for valid data");
            assertTrue(result.isSuccess(), "Rule execution should succeed for valid barrier vs strike");
            assertNotNull(result.getMessage(), "Result should have a message");
            assertEquals("nested-barrier-strike-validation", barrierStrikeRule.getId());

            logger.info("✓ Barrier vs Strike validation rule passed for valid data");

        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Date Consistency Validation Rule - Valid Case")
    void testDateConsistencyValidationRuleValid() {
        logger.info("=== Testing Date Consistency Validation Rule - Valid Case ===");

        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/BarrierOptionNestedValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");

            // Create RulesEngine
            YamlRulesEngineService rulesEngineService = new YamlRulesEngineService();
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");

            // Get the date consistency validation rule
            Rule dateConsistencyRule = engine.getConfiguration().getRuleById("nested-date-consistency-validation");
            assertNotNull(dateConsistencyRule, "Date consistency validation rule should be found");

            // Test data: Valid case - observation end date matches expiry date
            Map<String, Object> validData = createValidDateConsistencyData();
            logger.info("Testing with matching observation end date and expiry date (valid)");

            // Execute rule and validate results
            RuleResult result = engine.executeRule(dateConsistencyRule, validData);
            assertNotNull(result, "RuleResult should not be null");

            // Demonstrate RuleResult API methods
            logger.info("=== RuleResult API Methods - Date Consistency Valid ===");
            logger.info("result.isTriggered(): {}", result.isTriggered());
            logger.info("result.isSuccess(): {}", result.isSuccess());
            logger.info("result.getMessage(): {}", result.getMessage());
            logger.info("result.getRuleName(): {}", result.getRuleName());

            // Business logic validation: Valid date consistency should pass
            assertTrue(result.isTriggered(), "Date consistency rule should be triggered for valid data");
            assertTrue(result.isSuccess(), "Rule execution should succeed for valid date consistency");
            assertNotNull(result.getMessage(), "Result should have a message");

            logger.info("✓ Date consistency validation rule passed for valid data");

        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test Rebate Amount Validation Rule - Valid Case")
    void testRebateAmountValidationRuleValid() {
        logger.info("=== Testing Rebate Amount Validation Rule - Valid Case ===");

        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/BarrierOptionNestedValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");

            // Create RulesEngine
            YamlRulesEngineService rulesEngineService = new YamlRulesEngineService();
            RulesEngine engine = rulesEngineService.createRulesEngineFromYamlConfig(config);
            assertNotNull(engine, "RulesEngine should be created");

            // Get the rebate amount validation rule
            Rule rebateAmountRule = engine.getConfiguration().getRuleById("nested-rebate-validation");
            assertNotNull(rebateAmountRule, "Rebate amount validation rule should be found");

            // Test data: Valid case - rebate amount (5000) is 33% of premium (15000) - under 50%
            Map<String, Object> validData = createValidRebateAmountData();
            logger.info("Testing with rebate amount 33% of premium (valid - under 50%)");

            // Execute rule and validate results
            RuleResult result = engine.executeRule(rebateAmountRule, validData);
            assertNotNull(result, "RuleResult should not be null");

            // Demonstrate RuleResult API methods
            logger.info("=== RuleResult API Methods - Rebate Amount Valid ===");
            logger.info("result.isTriggered(): {}", result.isTriggered());
            logger.info("result.isSuccess(): {}", result.isSuccess());
            logger.info("result.getMessage(): {}", result.getMessage());
            logger.info("result.getRuleName(): {}", result.getRuleName());

            // Business logic validation: Valid rebate amount should pass
            assertTrue(result.isTriggered(), "Rebate amount rule should be triggered for valid data");
            assertTrue(result.isSuccess(), "Rule execution should succeed for valid rebate amount");
            assertNotNull(result.getMessage(), "Result should have a message");

            logger.info("✓ Rebate amount validation rule passed for valid data");

        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Validate APEX Validation Rules Configuration")
    void testApexValidationRulesConfiguration() {
        logger.info("=== Testing APEX Validation Rules Configuration ===");

        try {
            // Load YAML configuration
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = loader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/BarrierOptionNestedValidationTest.yaml");
            assertNotNull(config, "Configuration should be loaded");

            // VALIDATE: APEX validation configuration loaded successfully
            assertNotNull(config.getRules(), "APEX validation rules should be loaded");
            assertEquals(3, config.getRules().size(), "Should have exactly 3 validation rules");

            // VALIDATE: All validation rules are properly configured
            var rules = config.getRules();
            
            // Rule 1: Barrier vs Strike validation
            assertNotNull(rules.get(0).getCondition(), "First validation rule should have condition");
            assertTrue(rules.get(0).getCondition().contains("barrierLevel"), "First rule should check barrier level");
            assertTrue(rules.get(0).getCondition().contains("strikePrice"), "First rule should check strike price");
            assertEquals("ERROR", rules.get(0).getSeverity(), "First rule should be ERROR severity");

            // Rule 2: Date consistency validation  
            assertNotNull(rules.get(1).getCondition(), "Second validation rule should have condition");
            assertTrue(rules.get(1).getCondition().contains("observationPeriod"), "Second rule should check observation period");
            assertTrue(rules.get(1).getCondition().contains("endDate"), "Second rule should check end date");
            assertEquals("ERROR", rules.get(1).getSeverity(), "Second rule should be ERROR severity");

            // Rule 3: Rebate amount validation
            assertNotNull(rules.get(2).getCondition(), "Third validation rule should have condition");
            assertTrue(rules.get(2).getCondition().contains("rebateAmount"), "Third rule should check rebate amount");
            assertTrue(rules.get(2).getCondition().contains("premium"), "Third rule should check premium");
            assertEquals("WARNING", rules.get(2).getSeverity(), "Third rule should be WARNING severity");

            logger.info("✓ All 3 APEX nested validation rules configured successfully:");
            logger.info("  - Barrier vs Strike Validation: {} severity", rules.get(0).getSeverity());
            logger.info("  - Date Consistency Validation: {} severity", rules.get(1).getSeverity());
            logger.info("  - Rebate Amount Validation: {} severity", rules.get(2).getSeverity());

        } catch (YamlConfigurationException e) {
            logger.error("❌ Failed to load configuration: {}", e.getMessage());
            fail("Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Creates valid barrier vs strike test data - barrier level > strike price
     */
    private Map<String, Object> createValidBarrierStrikeData() {
        Map<String, Object> testData = new HashMap<>();

        // Level 1: Pricing terms
        Map<String, Object> pricingTerms = new HashMap<>();
        pricingTerms.put("strikePrice", "2150.00");  // Strike price
        testData.put("pricingTerms", pricingTerms);

        // Level 1: Barrier terms
        Map<String, Object> barrierTerms = new HashMap<>();
        barrierTerms.put("barrierLevel", "2300.00");      // Higher than strike (valid)
        barrierTerms.put("barrierDirection", "Up-and-Out");
        testData.put("barrierTerms", barrierTerms);

        return testData;
    }

    /**
     * Creates invalid barrier vs strike test data - barrier level < strike price
     */
    private Map<String, Object> createInvalidBarrierStrikeData() {
        Map<String, Object> testData = new HashMap<>();

        // Level 1: Pricing terms
        Map<String, Object> pricingTerms = new HashMap<>();
        pricingTerms.put("strikePrice", "2150.00");  // Strike price
        testData.put("pricingTerms", pricingTerms);

        // Level 1: Barrier terms
        Map<String, Object> barrierTerms = new HashMap<>();
        barrierTerms.put("barrierLevel", "2100.00");      // Lower than strike (invalid)
        barrierTerms.put("barrierDirection", "Up-and-Out");
        testData.put("barrierTerms", barrierTerms);

        return testData;
    }

    /**
     * Creates valid date consistency test data - observation end date matches expiry date
     */
    private Map<String, Object> createValidDateConsistencyData() {
        Map<String, Object> testData = new HashMap<>();

        // Root level data
        testData.put("expiryDate", "2025-12-19");

        // Level 1: Barrier terms with nested structures
        Map<String, Object> barrierTerms = new HashMap<>();

        // Level 2: Knockout conditions
        Map<String, Object> knockoutConditions = new HashMap<>();

        // Level 3: Observation period with matching expiry date
        Map<String, Object> observationPeriod = new HashMap<>();
        observationPeriod.put("endDate", "2025-12-19");  // Matches expiry date (valid)
        knockoutConditions.put("observationPeriod", observationPeriod);

        barrierTerms.put("knockoutConditions", knockoutConditions);
        testData.put("barrierTerms", barrierTerms);

        return testData;
    }

    /**
     * Creates valid rebate amount test data - rebate amount <= 50% of premium
     */
    private Map<String, Object> createValidRebateAmountData() {
        Map<String, Object> testData = new HashMap<>();

        // Level 1: Pricing terms
        Map<String, Object> pricingTerms = new HashMap<>();
        pricingTerms.put("premium", "15000.00");     // Premium for rebate calculation
        testData.put("pricingTerms", pricingTerms);

        // Level 1: Barrier terms with nested structures
        Map<String, Object> barrierTerms = new HashMap<>();

        // Level 2: Knockout conditions
        Map<String, Object> knockoutConditions = new HashMap<>();

        // Level 3: Rebate terms with reasonable amount
        Map<String, Object> rebateTerms = new HashMap<>();
        rebateTerms.put("rebateAmount", "5000.00");  // 33% of premium (valid - under 50%)
        knockoutConditions.put("rebateTerms", rebateTerms);

        barrierTerms.put("knockoutConditions", knockoutConditions);
        testData.put("barrierTerms", barrierTerms);

        return testData;
    }
}
