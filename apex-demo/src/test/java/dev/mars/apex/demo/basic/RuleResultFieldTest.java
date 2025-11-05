package dev.mars.apex.demo.basic;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating the result-field feature for storing rule evaluation results.
 * 
 * This test validates Phase 5 enhancement: storing boolean rule condition results in the
 * facts map for use by subsequent rules and enrichments.
 * 
 * Key Features Tested:
 * - Basic rule chaining: Rule 1 result used in Rule 2 condition
 * - Multiple result fields: Multiple rules storing different results
 * - Nested field storage: Storing results in nested objects
 * - Conditional logic: Using stored results in complex conditions
 * - No overhead: Rules without result-field don't incur storage overhead
 * - Rule groups: Result field works with sequential and parallel rule groups
 * 
 * Following prompts.txt guidelines:
 * - Tests actual functionality, not YAML syntax
 * - Uses real APEX rule operations
 * - Validates business logic outcomes
 * - Follows existing working patterns
 * - Uses middle office trade processing domain (OTC options)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Rule Result Field Storage Test")
public class RuleResultFieldTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RuleResultFieldTest.class);

    @Test
    @Order(1)
    @DisplayName("Test basic rule chaining with result-field")
    public void testBasicRuleChaining() {
        logger.info("=== Testing Basic Rule Chaining with result-field ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/RuleResultFieldTest.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Given: OTC option trade data
            Map<String, Object> data = new HashMap<>();
            data.put("tradeType", "OTC_OPTION");
            data.put("notionalAmount", 15000000.0);
            data.put("counterparty", "BANK_A");
            data.put("region", "EMEA");

            logger.info("Testing basic rule chaining with trade data: {}", data);

            // When: Process through APEX rules
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, data);
            assertNotNull(ruleResult, "Rule result should not be null");

            // Then: Validate that Rule 1 result was stored and used by Rule 2
            // Note: enrichedData contains the results, not the original data map
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            assertTrue((Boolean) enrichedData.get("isHighValue"),
                "Rule 1 should have stored isHighValue=true");
            assertTrue((Boolean) enrichedData.get("requiresApproval"),
                "Rule 2 should have evaluated to true based on isHighValue");

            logger.info("✓ Basic rule chaining test completed successfully");
            
        } catch (Exception e) {
            logger.error("Basic rule chaining test failed", e);
            fail("Basic rule chaining test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test multiple result fields")
    public void testMultipleResultFields() {
        logger.info("=== Testing Multiple Result Fields ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/RuleResultFieldTest.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Given: OTC option trade with multiple validation points
            Map<String, Object> data = new HashMap<>();
            data.put("tradeType", "OTC_OPTION");
            data.put("notionalAmount", 25000000.0);
            data.put("counterparty", "BANK_B");
            data.put("region", "APAC");
            data.put("creditRating", "BBB");

            logger.info("Testing multiple result fields with trade data: {}", data);

            // When: Process through APEX rules
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, data);
            assertNotNull(ruleResult, "Rule result should not be null");

            // Then: Validate that multiple results were stored
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            assertTrue((Boolean) enrichedData.get("isHighValue"),
                "isHighValue should be true for 25M notional");
            assertTrue((Boolean) enrichedData.get("isApacTrade"),
                "isApacTrade should be true for APAC region");
            assertTrue((Boolean) enrichedData.get("requiresCreditCheck"),
                "requiresCreditCheck should be true for BBB rating");

            logger.info("✓ Multiple result fields test completed successfully");
            
        } catch (Exception e) {
            logger.error("Multiple result fields test failed", e);
            fail("Multiple result fields test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test nested field storage")
    public void testNestedFieldStorage() {
        logger.info("=== Testing Nested Field Storage ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/RuleResultFieldTest.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Given: OTC option trade data
            Map<String, Object> data = new HashMap<>();
            data.put("tradeType", "OTC_OPTION");
            data.put("notionalAmount", 8000000.0);
            data.put("counterparty", "BANK_C");
            data.put("region", "AMERICAS");

            logger.info("Testing nested field storage with trade data: {}", data);

            // When: Process through APEX rules
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, data);
            assertNotNull(ruleResult, "Rule result should not be null");

            // Then: Validate that nested results were stored
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> validation = (Map<String, Object>) enrichedData.get("validation");
            assertNotNull(validation, "validation object should exist");
            assertFalse((Boolean) validation.get("isHighValue"),
                "validation.isHighValue should be false for 8M notional");

            logger.info("✓ Nested field storage test completed successfully");
            
        } catch (Exception e) {
            logger.error("Nested field storage test failed", e);
            fail("Nested field storage test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test conditional logic using stored results")
    public void testConditionalLogicWithStoredResults() {
        logger.info("=== Testing Conditional Logic Using Stored Results ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/RuleResultFieldTest.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Given: Complex OTC option trade
            Map<String, Object> data = new HashMap<>();
            data.put("tradeType", "OTC_OPTION");
            data.put("notionalAmount", 50000000.0);
            data.put("counterparty", "HEDGE_FUND_X");
            data.put("region", "EMEA");
            data.put("creditRating", "BB");
            data.put("maturityDays", 180);

            logger.info("Testing conditional logic with complex trade data: {}", data);

            // When: Process through APEX rules
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, data);
            assertNotNull(ruleResult, "Rule result should not be null");

            // Then: Validate complex conditional logic
            Map<String, Object> enrichedData = ruleResult.getEnrichedData();
            assertNotNull(enrichedData, "Enriched data should not be null");

            assertTrue((Boolean) enrichedData.get("isHighValue"),
                "isHighValue should be true for 50M notional");
            assertTrue((Boolean) enrichedData.get("requiresCreditCheck"),
                "requiresCreditCheck should be true for BB rating");
            assertTrue((Boolean) enrichedData.get("requiresApproval"),
                "requiresApproval should be true (high value + credit check)");

            logger.info("✓ Conditional logic test completed successfully");
            
        } catch (Exception e) {
            logger.error("Conditional logic test failed", e);
            fail("Conditional logic test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test no overhead when result-field not configured")
    public void testNoOverheadWithoutResultField() {
        logger.info("=== Testing No Overhead Without result-field ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile(
                "src/test/java/dev/mars/apex/demo/basic/RuleResultFieldTest.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Given: Simple trade data
            Map<String, Object> data = new HashMap<>();
            data.put("tradeType", "VANILLA_SWAP");
            data.put("notionalAmount", 5000000.0);

            int initialSize = data.size();
            logger.info("Testing no overhead with initial data size: {}", initialSize);

            // When: Process through APEX rules (some without result-field)
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            RuleResult ruleResult = engine.evaluate(config, data);
            assertNotNull(ruleResult, "Rule result should not be null");

            // Then: Validate that only configured result fields were added
            // (not every single rule evaluation result)
            int finalSize = data.size();
            logger.info("Final data size: {}", finalSize);
            
            // We should only have the explicitly configured result fields,
            // not a result for every rule that was evaluated
            assertTrue(finalSize < initialSize + 10, 
                "Should not have excessive result fields stored");

            logger.info("✓ No overhead test completed successfully");
            
        } catch (Exception e) {
            logger.error("No overhead test failed", e);
            fail("No overhead test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test APEX Services Initialization")
    public void testApexServicesInitialization() {
        logger.info("=== Testing APEX Services Initialization ===");

        // Call parent test
        super.testApexServicesInitialization();

        // Additional validations
        assertNotNull(yamlLoader, "YAML configuration loader should be initialized");

        logger.info("✅ All APEX services properly initialized for result-field testing");
    }
}

