package dev.mars.apex.demo.enrichmentgroups;

import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Basic YAML Enrichment Group Processing Tests.
 *
 * Tests that RulesEngine.evaluate() correctly processes ALL enrichment groups defined in the YAML file.
 * The YAML contains 4 enrichment groups: base_and, base_or, composite, composite_par_and.
 * All groups are processed automatically when RulesEngine.evaluate() is called.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Basic YAML Enrichment Group Processing Tests")
public class BasicYamlEnrichmentGroupProcessingTest extends DemoTestBase {

    private static final String CONFIG_PATH = "src/test/java/dev/mars/apex/demo/enrichmentgroups/BasicYamlEnrichmentGroupProcessingTest-combined-config.yaml";

    @Test
    @DisplayName("RulesEngine processes all enrichment groups with all fields present")
    void testAllEnrichmentGroupsWithAllFields() throws YamlConfigurationException {
        logger.info("Testing RulesEngine.evaluate() with all enrichment groups and all required fields");

        YamlRuleConfiguration config;
        try {
            config = mergeYamlConfigsForEnrichment(CONFIG_PATH);
        } catch (YamlConfigurationException e) {
            logger.error("Failed to load YAML: " + e.getMessage());
            fail("Failed to load YAML: " + e.getMessage());
            return;
        }
        assertNotNull(config, "Configuration should load successfully");

        // Create RulesEngine from configuration using static factory method
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Test data with all required fields
        Map<String, Object> data = new HashMap<>();
        data.put("a", "A");
        data.put("b", "B");
        data.put("c", "C");

        // Execute - this processes ALL 4 enrichment groups
        RuleResult result = engine.evaluate(data);

        // Verify overall success
        assertTrue(result.isSuccess(), "RulesEngine should succeed when all enrichment groups succeed");

        // Get enriched data from result
        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify all enrichments were applied
        assertEquals("A", enrichedData.get("a_copy"), "Enrichment e1 should have copied field 'a'");
        assertEquals("B", enrichedData.get("b_copy"), "Enrichment e2 should have copied field 'b'");
        assertEquals("C", enrichedData.get("c_copy"), "Enrichment e3 should have copied field 'c'");

        logger.info("All enrichment groups processed successfully");
    }

    @Test
    @DisplayName("RulesEngine processes all enrichment groups with missing field 'c'")
    void testAllEnrichmentGroupsWithMissingC() throws YamlConfigurationException {
        logger.info("Testing RulesEngine.evaluate() with missing field 'c'");

        YamlRuleConfiguration config;
        try {
            config = mergeYamlConfigsForEnrichment(CONFIG_PATH);
        } catch (YamlConfigurationException e) {
            fail("YAML load failed: " + e.getMessage());
            return;
        }

        // DEBUG: Print groups
        dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory.buildEnrichmentGroups(config).forEach(g -> {
            System.out.println("DEBUG: Group " + g.getId() + " has " + g.getEnrichmentsInOrder().size() + " enrichments");
            g.getEnrichmentsInOrder().forEach(e -> System.out.println("  - " + e.getId()));
        });

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Test data missing field 'c' - this will cause e3 to fail
        Map<String, Object> data = new HashMap<>();
        data.put("a", "A");
        data.put("b", "B");
        // Missing 'c'

        // Execute - this processes ALL 4 enrichment groups
        RuleResult result = engine.evaluate(data);

        // DEBUG: Print result
        System.out.println("DEBUG: Result success: " + result.isSuccess());
        System.out.println("DEBUG: Enriched Data: " + result.getEnrichedData());

        // Verify overall failure (because composite and composite_par_and groups will fail)
        assertFalse(result.isSuccess(), "RulesEngine should fail when enrichment groups fail");

        // Get enriched data from result
        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify partial enrichments were applied
        assertEquals("A", enrichedData.get("a_copy"), "Enrichment e1 should have copied field 'a'");
        assertEquals("B", enrichedData.get("b_copy"), "Enrichment e2 should have copied field 'b'");
        assertNull(enrichedData.get("c_copy"), "Enrichment e3 should not have copied field 'c' (missing)");

        logger.info("Enrichment groups correctly failed with missing field");
    }

    @Test
    @DisplayName("RulesEngine processes all enrichment groups with only field 'a'")
    void testAllEnrichmentGroupsWithOnlyA() throws YamlConfigurationException {
        logger.info("Testing RulesEngine.evaluate() with only field 'a'");

        YamlRuleConfiguration config;
        try {
            config = mergeYamlConfigsForEnrichment(CONFIG_PATH);
        } catch (YamlConfigurationException e) {
            fail("YAML load failed: " + e.getMessage());
            return;
        }

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Test data with only field 'a'
        Map<String, Object> data = new HashMap<>();
        data.put("a", "A");

        // Execute - this processes ALL 4 enrichment groups
        RuleResult result = engine.evaluate(data);

        // Verify overall failure (AND groups will fail, OR group might succeed)
        assertFalse(result.isSuccess(), "RulesEngine should fail when most enrichment groups fail");

        // Get enriched data from result
        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify only e1 enrichment was applied
        assertEquals("A", enrichedData.get("a_copy"), "Enrichment e1 should have copied field 'a'");
        // Note: b_copy might or might not be present depending on OR group short-circuit behavior
        assertNull(enrichedData.get("c_copy"), "Enrichment e3 should not have copied field 'c' (missing)");

        logger.info("Enrichment groups correctly processed with partial data");
    }

    @Test
    @DisplayName("RulesEngine processes all enrichment groups with no fields")
    void testAllEnrichmentGroupsWithNoFields() throws YamlConfigurationException {
        logger.info("Testing RulesEngine.evaluate() with no fields");

        YamlRuleConfiguration config;
        try {
            config = mergeYamlConfigsForEnrichment(CONFIG_PATH);
        } catch (YamlConfigurationException e) {
            fail("YAML load failed: " + e.getMessage());
            return;
        }

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        // Test data with no fields
        Map<String, Object> data = new HashMap<>();

        // Execute - this processes ALL 4 enrichment groups
        RuleResult result = engine.evaluate(data);

        // Verify overall failure (all enrichment groups should fail)
        assertFalse(result.isSuccess(), "RulesEngine should fail when all enrichment groups fail");

        // Get enriched data from result
        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify no enrichments were applied
        assertNull(enrichedData.get("a_copy"), "No enrichments should be applied");
        assertNull(enrichedData.get("b_copy"), "No enrichments should be applied");
        assertNull(enrichedData.get("c_copy"), "No enrichments should be applied");

        logger.info("Enrichment groups correctly failed with no data");
    }

}


