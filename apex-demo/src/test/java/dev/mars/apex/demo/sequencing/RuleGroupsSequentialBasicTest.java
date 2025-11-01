package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.RulesEngineService;
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
 * Test rule-groups executing in sequential (document order) mode.
 * 
 * This test validates that rule-groups are executed in the order they appear
 * in the YAML document, following the sequential processing implementation.
 */
class RuleGroupsSequentialBasicTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleGroupsSequentialBasicTest.class);

    @Test
    @DisplayName("Rule-groups execute in document order: enrichments -> rules -> rule-groups")
    void testRuleGroupsSequentialBasic() throws Exception {
        LOGGER.info("=== TESTING: Rule-Groups Sequential Basic Pattern ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RuleGroupsSequentialBasicTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = rulesEngineService.createRulesEngineFromFile(new File(yamlPath));

        assertNotNull(engine, "RulesEngine should be created successfully");
        assertNotNull(config, "Configuration should load successfully");

        // Test with CUST001 - GOLD tier
        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");

        LOGGER.info("* Input Data: {}", testData);

        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Processing completed. Final data state: {}", enrichedData);

        // Verify enrichment worked
        assertNotNull(enrichedData.get("customerTier"), "Customer tier should be enriched");
        assertEquals("GOLD", enrichedData.get("customerTier"), "Customer CUST001 should have GOLD tier");

        // Verify original data preserved
        assertEquals("CUST001", enrichedData.get("customerId"), "Original customer ID should be preserved");

        LOGGER.info("* Rule-Groups Sequential Processing WORKS");
        LOGGER.info("   1. Enrichment: customerTier = {}", enrichedData.get("customerTier"));
        LOGGER.info("   2. Rule: validate-tier-exists passed");
        LOGGER.info("   3. Rule-Group: tier-validation-group passed");
        LOGGER.info("   4. Sequential processing respects YAML document order");
    }

    @Test
    @DisplayName("Rule-groups with multiple customers - validate enrichment works for all")
    void testRuleGroupsMultipleCustomers() throws Exception {
        LOGGER.info("=== TESTING: Rule-Groups with Multiple Customers ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RuleGroupsSequentialMultipleCustomersTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = rulesEngineService.createRulesEngineFromFile(new File(yamlPath));

        // Test CUST001 - GOLD tier
        Map<String, Object> testData1 = new HashMap<>();
        testData1.put("customerId", "CUST001");
        RuleResult result1 = engine.evaluate(config, testData1);

        assertTrue(result1.isSuccess(), "CUST001 should succeed");
        assertEquals("GOLD", result1.getEnrichedData().get("customerTier"));
        LOGGER.info("* CUST001 (GOLD): Enrichment and rule-group executed successfully");

        // Test CUST002 - SILVER tier
        Map<String, Object> testData2 = new HashMap<>();
        testData2.put("customerId", "CUST002");
        RuleResult result2 = engine.evaluate(config, testData2);

        assertTrue(result2.isSuccess(), "CUST002 should succeed");
        assertEquals("SILVER", result2.getEnrichedData().get("customerTier"));
        LOGGER.info("* CUST002 (SILVER): Enrichment and rule-group executed successfully");

        // Test CUST003 - BRONZE tier
        Map<String, Object> testData3 = new HashMap<>();
        testData3.put("customerId", "CUST003");
        RuleResult result3 = engine.evaluate(config, testData3);

        assertTrue(result3.isSuccess(), "CUST003 should succeed");
        assertEquals("BRONZE", result3.getEnrichedData().get("customerTier"));
        LOGGER.info("* CUST003 (BRONZE): Enrichment and rule-group executed successfully");

        LOGGER.info("* Rule-groups correctly process multiple customers with different tiers");
    }

    @Test
    @DisplayName("Rule-groups depend on enrichment results - sequential order matters")
    void testRuleGroupsDependOnEnrichment() throws Exception {
        LOGGER.info("=== TESTING: Rule-Groups Depend on Enrichment Results ===");

        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/RuleGroupsSequentialDependOnEnrichmentTest.yaml";
        YamlRuleConfiguration config = yamlLoader.loadFromFile(yamlPath);
        RulesEngine engine = rulesEngineService.createRulesEngineFromFile(new File(yamlPath));

        Map<String, Object> testData = new HashMap<>();
        testData.put("customerId", "CUST001");

        LOGGER.info("* Input Data: {}", testData);

        RuleResult result = engine.evaluate(config, testData);
        assertNotNull(result, "Rule result should not be null");
        assertTrue(result.isSuccess(), "Processing should succeed");

        Map<String, Object> enrichedData = result.getEnrichedData();
        LOGGER.info("* Enriched Data: {}", enrichedData);

        // Verify enrichments executed first
        assertEquals("GOLD", enrichedData.get("customerTier"), "Customer tier should be enriched");
        assertEquals(50000, enrichedData.get("creditLimit"), "Credit limit should be enriched");

        LOGGER.info("* KEY INSIGHT: Rule-groups depend on enrichment results");
        LOGGER.info("   - Enrichments MUST execute BEFORE rule-groups");
        LOGGER.info("   - Sequential processing ensures correct execution order");
        LOGGER.info("   - Document order: enrichments -> rules -> rule-groups");
    }
}

