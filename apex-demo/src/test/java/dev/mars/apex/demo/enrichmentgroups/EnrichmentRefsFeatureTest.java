package dev.mars.apex.demo.enrichmentgroups;

import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test demonstrating the new enrichment-refs feature.
 * 
 * This feature allows enrichment groups to reference enrichments and enrichment groups
 * defined in external YAML files, similar to how rule-refs works for rules.
 * 
 * Use Case: LeifMultiFileEnrichmentGroups.yaml uses enrichment-refs to load
 * enrichments and enrichment groups from LeifMultiFileEnrichments.yaml, then
 * references the external enrichment group using enrichment-group-references.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Enrichment-Refs Feature Tests")
public class EnrichmentRefsFeatureTest extends DemoTestBase {

    private static final String MAIN_CONFIG_PATH = 
        "src/test/java/dev/mars/apex/demo/enrichmentgroups/LeifMultiFileEnrichmentGroups.yaml";

    /**
     * Load configuration using the new enrichment-refs feature.
     * The YamlConfigurationLoader will automatically process enrichment-refs
     * and merge enrichments and enrichment groups from external files.
     */
    private YamlRuleConfiguration loadConfigWithEnrichmentRefs() {
        try {
            YamlConfigurationLoader loader = new YamlConfigurationLoader();
            return loader.loadFromFile(MAIN_CONFIG_PATH);
        } catch (YamlConfigurationException e) {
            fail("Failed to load YAML with enrichment-refs: " + e.getMessage());
            return null;
        }
    }

    @Test
    @DisplayName("enrichment-refs: loads enrichments from external file")
    void testEnrichmentRefsLoadsEnrichments() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");
        
        // Verify enrichments from external file (r1, r2) are merged
        assertNotNull(config.getEnrichments(), "Enrichments should not be null");
        assertTrue(config.getEnrichments().size() >= 4, 
            "Should have at least 4 enrichments (r1, r2 from external + r3, r4 from main)");
        
        // Verify specific enrichments exist
        assertTrue(config.getEnrichments().stream().anyMatch(e -> "r1".equals(e.getId())),
            "Enrichment r1 from external file should be loaded");
        assertTrue(config.getEnrichments().stream().anyMatch(e -> "r2".equals(e.getId())),
            "Enrichment r2 from external file should be loaded");
        assertTrue(config.getEnrichments().stream().anyMatch(e -> "r3".equals(e.getId())),
            "Enrichment r3 from main file should be loaded");
        assertTrue(config.getEnrichments().stream().anyMatch(e -> "r4".equals(e.getId())),
            "Enrichment r4 from main file should be loaded");
    }

    @Test
    @DisplayName("enrichment-refs: loads enrichment groups from external file")
    void testEnrichmentRefsLoadsEnrichmentGroups() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");
        
        // Verify enrichment groups from external file are merged
        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
        assertTrue(config.getEnrichmentGroups().size() >= 5,
            "Should have at least 5 groups (rule_builder_group from external + g1, g2_rule_builder, g3, g4_rule_builder from main)");
        
        // Verify specific enrichment groups exist
        assertTrue(config.getEnrichmentGroups().stream().anyMatch(g -> "rule_builder_group".equals(g.getId())),
            "Enrichment group rule_builder_group from external file should be loaded");
        assertTrue(config.getEnrichmentGroups().stream().anyMatch(g -> "g1".equals(g.getId())),
            "Enrichment group g1 from main file should be loaded");
        assertTrue(config.getEnrichmentGroups().stream().anyMatch(g -> "g2_rule_builder".equals(g.getId())),
            "Enrichment group g2_rule_builder from main file should be loaded");
    }

    @Test
    @DisplayName("RulesEngine processes enrichment-group-references across files")
    void testEnrichmentGroupReferencesAcrossFiles() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");

        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Execute with all required fields - RulesEngine will process ALL enrichment groups
            Map<String, Object> data = new HashMap<>();
            data.put("input_x", "X_VALUE");
            data.put("input_y", "Y_VALUE");
            data.put("input_z", "Z_VALUE");
            data.put("input_w", "W_VALUE");

            RuleResult result = engine.evaluate(data);
            assertTrue(result.isSuccess(), "RulesEngine should succeed when all required fields present");

            // Verify enrichments from external file were applied
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("X_VALUE", enrichedData.get("output_x"), "r1 should have set output_x");
            assertEquals("Y_VALUE", enrichedData.get("output_y"), "r2 should have set output_y");
            assertEquals("Z_VALUE", enrichedData.get("output_z"), "r3 should have set output_z");
            assertEquals("W_VALUE", enrichedData.get("output_w"), "r4 should have set output_w");
        } catch (YamlConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes combined groups with local and external references")
    void testCombinedGroupWithExternalReference() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");

        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Execute with all required fields
            Map<String, Object> data = new HashMap<>();
            data.put("input_x", "X_VALUE");
            data.put("input_y", "Y_VALUE");
            data.put("input_z", "Z_VALUE");
            data.put("input_w", "W_VALUE");

            RuleResult result = engine.evaluate(data);
            assertTrue(result.isSuccess(), "RulesEngine should succeed");

            // Verify all enrichments were applied
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("X_VALUE", enrichedData.get("output_x"), "r1 should have set output_x");
            assertEquals("Y_VALUE", enrichedData.get("output_y"), "r2 should have set output_y");
            assertEquals("Z_VALUE", enrichedData.get("output_z"), "r3 should have set output_z");
            assertEquals("W_VALUE", enrichedData.get("output_w"), "r4 should have set output_w");
        } catch (YamlConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes all enrichment groups from external files")
    void testAllEnrichmentGroupsProcessed() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");

        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Execute with all required fields - RulesEngine processes ALL groups
            Map<String, Object> data = new HashMap<>();
            data.put("input_x", "X_VALUE");
            data.put("input_y", "Y_VALUE");
            data.put("input_z", "Z_VALUE");
            data.put("input_w", "W_VALUE");

            RuleResult result = engine.evaluate(data);
            assertTrue(result.isSuccess(), "RulesEngine should succeed when all fields present");

            // Verify all enrichments from all groups were applied
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertNotNull(enrichedData.get("output_x"), "output_x should be set");
            assertNotNull(enrichedData.get("output_y"), "output_y should be set");
            assertNotNull(enrichedData.get("output_z"), "output_z should be set");
            assertNotNull(enrichedData.get("output_w"), "output_w should be set");
        } catch (YamlConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine validates enrichment-refs configuration")
    void testEnrichmentRefsValidation() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");

        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            // Execute with partial data to test validation
            Map<String, Object> data = new HashMap<>();
            data.put("input_x", "X_VALUE");

            RuleResult result = engine.evaluate(data);
            // Result may fail due to missing required fields, but engine should be created successfully
            assertNotNull(result, "Result should not be null");
        } catch (YamlConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("enrichment-refs: validates configuration metadata")
    void testConfigurationMetadata() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");
        assertNotNull(config.getMetadata(), "Metadata should not be null");
        assertEquals("leif-multifile-enrichment-groups", config.getMetadata().getId(),
            "Should use main file's metadata");
    }
}


