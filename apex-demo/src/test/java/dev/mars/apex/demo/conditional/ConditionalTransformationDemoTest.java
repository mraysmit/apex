package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive demo test for Conditional Transformations.
 * Covers:
 * - Basic True/False conditions
 * - Else actions
 * - Multiple actions (Set, Calculate, Copy, Remove)
 * - Complex conditions
 */
class ConditionalTransformationDemoTest extends DemoTestBase {

    private static final String YAML_FILE = "c:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-demo/src/test/java/dev/mars/apex/demo/conditional/ConditionalTransformationDemoTest.yaml";

    @Test
    @DisplayName("Test Conditional Transformations - High Value USD Scenario")
    void testHighValueUSD() throws Exception {
        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_FILE);
        assertNotNull(config, "YAML configuration should not be null");

        // Prepare test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 1500.0); // > 1000 (High Value)
        testData.put("currency", "USD");
        testData.put("temporaryFlag", "should-be-removed");

        // Execute evaluation
        RuleResult result = testEvaluation(config, testData);

        // Validate results
        assertTrue(result.isSuccess());
        Map<String, Object> enrichedData = result.getEnrichedData();

        // 1. Basic True Condition (trans-basic-true)
        assertEquals("HIGH_VALUE", enrichedData.get("status"), "Status should be HIGH_VALUE");

        // 2. Basic False Condition (trans-basic-false) - amount is positive, so else-action runs
        assertEquals("VALID", enrichedData.get("validity"), "Validity should be VALID");

        // 3. Multiple Actions (trans-complex-actions) - Currency is USD
        assertEquals(150.0, enrichedData.get("taxAmount"), "Tax should be 10% of amount");
        assertEquals("USD", enrichedData.get("originalCurrency"), "Original currency should be copied");
        assertFalse(enrichedData.containsKey("temporaryFlag"), "Temporary flag should be removed");
        assertEquals(true, enrichedData.get("processed"), "Processed flag should be true");

        // 4. Complex Condition (trans-complex-condition) - Status is HIGH_VALUE and Validity is VALID
        assertEquals(true, enrichedData.get("priority_processing"), "Priority processing should be enabled");
    }

    @Test
    @DisplayName("Test Conditional Transformations - Low Value EUR Scenario")
    void testLowValueEUR() throws Exception {
        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_FILE);
        assertNotNull(config, "YAML configuration should not be null");

        // Prepare test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", 500.0); // < 1000 (Not High Value)
        testData.put("currency", "EUR"); // Not USD

        // Execute evaluation
        RuleResult result = testEvaluation(config, testData);

        // Validate results
        assertTrue(result.isSuccess());
        Map<String, Object> enrichedData = result.getEnrichedData();

        // 1. Basic True Condition (trans-basic-true) - Condition fails, no action
        assertNull(enrichedData.get("status"), "Status should be null (condition failed)");

        // 2. Basic False Condition (trans-basic-false) - amount is positive, so else-action runs
        assertEquals("VALID", enrichedData.get("validity"), "Validity should be VALID");

        // 3. Multiple Actions (trans-complex-actions) - Currency is EUR (not USD)
        assertNull(enrichedData.get("taxAmount"), "Tax should not be calculated");
        assertNull(enrichedData.get("processed"), "Processed flag should not be set");

        // 4. Complex Condition (trans-complex-condition) - Status is null, so condition fails
        assertNull(enrichedData.get("priority_processing"), "Priority processing should not be enabled");
    }

    @Test
    @DisplayName("Test Conditional Transformations - Invalid Amount Scenario")
    void testInvalidAmount() throws Exception {
        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(YAML_FILE);
        assertNotNull(config, "YAML configuration should not be null");

        // Prepare test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("amount", -100.0); // Negative amount
        testData.put("currency", "GBP");

        // Execute evaluation
        RuleResult result = testEvaluation(config, testData);

        // Validate results
        assertTrue(result.isSuccess());
        Map<String, Object> enrichedData = result.getEnrichedData();

        // 2. Basic False Condition (trans-basic-false) - amount is negative, so main action runs
        assertEquals("INVALID", enrichedData.get("validity"), "Validity should be INVALID");
    }
}
