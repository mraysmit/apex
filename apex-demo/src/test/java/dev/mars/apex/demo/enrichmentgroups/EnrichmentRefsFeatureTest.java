package dev.mars.apex.demo.enrichmentgroups;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.EnrichmentGroupResult;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
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
    @DisplayName("enrichment-refs: enrichment-group-references resolves across files")
    void testEnrichmentGroupReferencesAcrossFiles() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");
        
        // Build runtime enrichment groups
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        assertNotNull(groups, "Enrichment groups should be built successfully");
        
        // Find g2_rule_builder which references rule_builder_group from external file
        EnrichmentGroup g2 = groups.stream()
            .filter(g -> "g2_rule_builder".equals(g.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(g2, "g2_rule_builder should exist");
        
        // Execute g2_rule_builder - it should execute r1 and r2 from the referenced group
        Map<String, Object> data = new HashMap<>();
        data.put("input_x", "X_VALUE");
        data.put("input_y", "Y_VALUE");
        
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(g2, data, config);
        assertTrue(result.isSuccess(), "g2_rule_builder should succeed when all required fields present");
        assertEquals(2, result.getEnrichmentResults().size(), 
            "Should execute 2 enrichments from referenced rule_builder_group");
        
        // Verify enrichments were applied
        assertEquals("X_VALUE", data.get("output_x"), "r1 should have set output_x");
        assertEquals("Y_VALUE", data.get("output_y"), "r2 should have set output_y");
    }

    @Test
    @DisplayName("enrichment-refs: combined group with local enrichments and external group reference")
    void testCombinedGroupWithExternalReference() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");
        
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        
        // Find g3 which has both local enrichment (r3) and external group reference (rule_builder_group)
        EnrichmentGroup g3 = groups.stream()
            .filter(g -> "g3".equals(g.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(g3, "g3 should exist");
        
        // Execute g3 - it should execute r3 + r1 + r2
        Map<String, Object> data = new HashMap<>();
        data.put("input_x", "X_VALUE");
        data.put("input_y", "Y_VALUE");
        data.put("input_z", "Z_VALUE");
        
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(g3, data, config);
        assertTrue(result.isSuccess(), "g3 should succeed when all required fields present");
        assertEquals(3, result.getEnrichmentResults().size(),
            "Should execute 3 enrichments (r3 + rule_builder_group's r1 and r2)");
        
        // Verify all enrichments were applied
        assertEquals("X_VALUE", data.get("output_x"), "r1 should have set output_x");
        assertEquals("Y_VALUE", data.get("output_y"), "r2 should have set output_y");
        assertEquals("Z_VALUE", data.get("output_z"), "r3 should have set output_z");
    }

    @Test
    @DisplayName("enrichment-refs: OR group with external reference short-circuits correctly")
    void testOrGroupWithExternalReferenceShortCircuits() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");
        
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        
        // Find g4_rule_builder which is an OR group referencing rule_builder_group
        EnrichmentGroup g4 = groups.stream()
            .filter(g -> "g4_rule_builder".equals(g.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(g4, "g4_rule_builder should exist");
        
        // Execute g4 with only input_x - should short-circuit after r1 succeeds
        Map<String, Object> data = new HashMap<>();
        data.put("input_x", "X_VALUE");
        
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(g4, data, config);
        assertTrue(result.isSuccess(), "OR group should succeed when first enrichment succeeds");
        assertEquals(1, result.getEnrichmentResults().size(),
            "OR group should short-circuit after first success");
        
        // Verify only r1 was applied
        assertEquals("X_VALUE", data.get("output_x"), "r1 should have set output_x");
        assertNull(data.get("output_y"), "r2 should not execute due to OR short-circuit");
    }

    @Test
    @DisplayName("enrichment-refs: local group works independently")
    void testLocalGroupWorksIndependently() {
        YamlRuleConfiguration config = loadConfigWithEnrichmentRefs();
        assertNotNull(config, "Configuration should load successfully");
        
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        
        // Find g1 which only uses local enrichments (r3, r4)
        EnrichmentGroup g1 = groups.stream()
            .filter(g -> "g1".equals(g.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(g1, "g1 should exist");
        
        // Execute g1
        Map<String, Object> data = new HashMap<>();
        data.put("input_z", "Z_VALUE");
        data.put("input_w", "W_VALUE");
        
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(g1, data, config);
        assertTrue(result.isSuccess(), "g1 should succeed when all required fields present");
        assertEquals(2, result.getEnrichmentResults().size(),
            "Should execute 2 local enrichments");
        
        // Verify local enrichments were applied
        assertEquals("Z_VALUE", data.get("output_z"), "r3 should have set output_z");
        assertEquals("W_VALUE", data.get("output_w"), "r4 should have set output_w");
        
        // Verify external enrichments were NOT applied
        assertNull(data.get("output_x"), "r1 should not execute in g1");
        assertNull(data.get("output_y"), "r2 should not execute in g1");
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

