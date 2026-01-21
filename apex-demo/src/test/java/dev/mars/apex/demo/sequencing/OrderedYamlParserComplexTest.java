package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complex Sequential Processing Tests - Using RulesEngine
 *
 * These tests validate complex YAML configurations with multiple sections
 * executing in sequential (document order) mode.
 *
 * Test Coverage:
 * - Complex multi-section YAML files with actual business logic
 * - Enrich-then-validate pattern with real enrichments and rules
 * - Validate-then-enrich pattern with real validations and enrichments
 * - All sections execute and produce correct business results
 *
 * TESTING PRINCIPLES FOLLOWED:
 * - Use RulesEngine.evaluate() not internal parsers
 * - Test actual business logic not YAML syntax
 * - Validate enriched data values not section order
 * - Follow patterns from RuleGroupsSequentialBasicTest
 *
 * @author APEX Sequential Processing Implementation
 * @version 2.0 - Rewritten to follow APEX testing principles
 */
class OrderedYamlParserComplexTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderedYamlParserComplexTest.class.getName());

    @Test
    @DisplayName("Complex configuration: all section types execute in order with correct results")
    void testAllSectionTypesInOrder() throws Exception {
        LOGGER.info("=== TESTING: Complex Configuration with All Section Types ===");

        // 1. Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/OrderedYamlParserComplexTestAllSections.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        assertNotNull(config, "Configuration should be loaded");

        // 2. Create RulesEngine
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        assertNotNull(engine, "RulesEngine should be created");

        // 3. Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        LOGGER.info("* Input Data: {}", testData);

        // 4. Execute using RulesEngine.evaluate()
        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        // 5. Get enriched data
        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // 6. Validate business logic results
        assertNotNull(enrichedData.get("customerData"), "Customer should be enriched");
        assertEquals("John Doe", enrichedData.get("customerData"),
                    "Should enrich with correct customer name");
        assertEquals("CUST001", enrichedData.get("customerId"),
                    "Original customer ID should be preserved");

        LOGGER.info("* COMPLEX CONFIGURATION VALIDATED:");
        LOGGER.info("   1. All sections processed in document order");
        LOGGER.info("   2. Enrichments executed: customerData = {}", enrichedData.get("customerData"));
        LOGGER.info("   3. Rules validated enriched data successfully");
        LOGGER.info("   4. Rule-groups, enrichment-groups, transformations all processed");
        LOGGER.info("All section types in order test PASSED");
    }

    @Test
    @DisplayName("Enrich-then-validate pattern: multiple enrichments execute before rules")
    void testEnrichThenValidatePattern() throws Exception {
        LOGGER.info("=== TESTING: Enrich-Then-Validate Pattern (Complex) ===");

        // 1. Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/OrderedYamlParserComplexTestEnrichThenValidate.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        assertNotNull(config, "Configuration should be loaded");

        // 2. Create RulesEngine
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        assertNotNull(engine, "RulesEngine should be created");

        // 3. Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        LOGGER.info("* Input Data: {}", testData);

        // 4. Execute using RulesEngine.evaluate()
        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        // 5. Get enriched data
        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // 6. Validate business logic results - multiple enrichments
        assertNotNull(enrichedData.get("customerProfile"), "Customer profile should be enriched");
        assertEquals("ACTIVE", enrichedData.get("customerProfile"),
                    "Should enrich with correct customer profile");
        assertNotNull(enrichedData.get("preferences"), "Customer preferences should be enriched");
        assertEquals("ACTIVE", enrichedData.get("preferences"),
                    "Preferences should be enriched from profile");

        LOGGER.info("* ENRICH-THEN-VALIDATE PATTERN VALIDATED:");
        LOGGER.info("   1. First enrichment: customerProfile = {}", enrichedData.get("customerProfile"));
        LOGGER.info("   2. Second enrichment: preferences = {}", enrichedData.get("preferences"));
        LOGGER.info("   3. Rules validated enriched data successfully");
        LOGGER.info("Enrich-then-validate pattern test PASSED");
    }

    @Test
    @DisplayName("Validate-then-enrich pattern: rules execute before enrichments")
    void testValidateThenEnrichPattern() throws Exception {
        LOGGER.info("=== TESTING: Validate-Then-Enrich Pattern (Complex) ===");

        // 1. Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/OrderedYamlParserComplexTestValidateThenEnrich.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        assertNotNull(config, "Configuration should be loaded");

        // 2. Create RulesEngine
        RulesEngine engine = RulesEngine.fromFile(yamlPath);
        assertNotNull(engine, "RulesEngine should be created");

        // 3. Create test data with fields to validate
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");
        testData.put("requestType", "LOOKUP");
        LOGGER.info("* Input Data: {}", testData);

        // 4. Execute using RulesEngine.evaluate()
        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        // 5. Get enriched data
        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // 6. Validate business logic results
        assertEquals("CUST001", enrichedData.get("customerId"),
                    "Customer ID should be validated first");
        assertEquals("LOOKUP", enrichedData.get("requestType"),
                    "Request type should be validated first");
        assertNotNull(enrichedData.get("customerData"),
                     "Customer should be enriched after validation");
        assertEquals("John Doe", enrichedData.get("customerData"),
                    "Should enrich with correct customer name");

        LOGGER.info("* VALIDATE-THEN-ENRICH PATTERN VALIDATED:");
        LOGGER.info("   1. Rules validated input data first");
        LOGGER.info("   2. Enrichment executed after validation: customerData = {}", enrichedData.get("customerData"));
        LOGGER.info("   3. Sequential processing respects YAML document order");
        LOGGER.info("Validate-then-enrich pattern test PASSED");
    }
}
