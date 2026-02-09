package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.yaml.sequential.ProcessingItem;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that groups-only logic correctly filters enrichment-groups and rule-groups
 * that are referenced by other groups.
 * 
 * This test addresses a critical gap in test coverage where groups referencing other groups
 * were not being filtered from itemOrder, causing double execution.
 */
@DisplayName("Groups-Only Logic: Group References")
class GroupReferencesGroupsOnlyLogicTest {

    private static final Logger logger = LoggerFactory.getLogger(GroupReferencesGroupsOnlyLogicTest.class);
    
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Enrichment-groups referenced by other enrichment-groups should be filtered from itemOrder")
    void testEnrichmentGroupReferencesFiltering() throws Exception {
        // Load the test YAML file
        File yamlFile = new File("src/test/resources/config/enrichment-group-references.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // CRITICAL ASSERTION: rbg1 should NOT be in itemOrder because it's referenced by e2_eg
        boolean rbg1InItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "rbg1".equals(item.getItemId()));
        
        assertFalse(rbg1InItemOrder, 
            "CRITICAL BUG: enrichment-group 'rbg1' should be filtered from itemOrder because it's referenced by 'e2_eg'");

        // Verify that e1_eg, e2_eg, and e3_eg ARE in itemOrder (they are not referenced by other groups)
        boolean e1_egInItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e1_eg".equals(item.getItemId()));
        boolean e2_egInItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e2_eg".equals(item.getItemId()));
        boolean e3_egInItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e3_eg".equals(item.getItemId()));

        assertTrue(e1_egInItemOrder, "e1_eg should be in itemOrder (not referenced by other groups)");
        assertTrue(e2_egInItemOrder, "e2_eg should be in itemOrder (not referenced by other groups)");
        assertTrue(e3_egInItemOrder, "e3_eg should be in itemOrder (not referenced by other groups)");

        // Verify that enrichments e1, e2, rbg1_enrichment are NOT in itemOrder (referenced by groups)
        boolean e1InItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e1".equals(item.getItemId()));
        boolean e2InItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e2".equals(item.getItemId()));
        boolean rbg1_enrichmentInItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "rbg1_enrichment".equals(item.getItemId()));

        assertFalse(e1InItemOrder, "e1 should be filtered from itemOrder (referenced by e1_eg)");
        assertFalse(e2InItemOrder, "e2 should be filtered from itemOrder (referenced by e3_eg)");
        assertFalse(rbg1_enrichmentInItemOrder, "rbg1_enrichment should be filtered from itemOrder (referenced by rbg1)");

        // Verify final itemOrder size: should be 3 (e1_eg, e2_eg, e3_eg)
        assertEquals(3, itemOrder.size(), 
            "itemOrder should contain exactly 3 items: e1_eg, e2_eg, e3_eg");
    }

    @Test
    @DisplayName("Rule-groups referenced by other rule-groups should be filtered from itemOrder")
    void testRuleGroupReferencesFiltering() throws Exception {
        // Load the test YAML file
        File yamlFile = new File("src/test/resources/config/rule-group-references.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // CRITICAL ASSERTION: base_rule_group should NOT be in itemOrder because it's referenced by composite_rule_group
        boolean baseRuleGroupInItemOrder = itemOrder.stream()
            .anyMatch(item -> "rule-groups".equals(item.getSectionType()) && "base_rule_group".equals(item.getItemId()));
        
        assertFalse(baseRuleGroupInItemOrder, 
            "CRITICAL BUG: rule-group 'base_rule_group' should be filtered from itemOrder because it's referenced by 'composite_rule_group'");

        // Verify that composite_rule_group IS in itemOrder (not referenced by other groups)
        boolean compositeRuleGroupInItemOrder = itemOrder.stream()
            .anyMatch(item -> "rule-groups".equals(item.getSectionType()) && "composite_rule_group".equals(item.getItemId()));

        assertTrue(compositeRuleGroupInItemOrder, "composite_rule_group should be in itemOrder (not referenced by other groups)");

        // Verify that rules are NOT in itemOrder (referenced by groups)
        boolean rule1InItemOrder = itemOrder.stream()
            .anyMatch(item -> "rules".equals(item.getSectionType()) && "rule1".equals(item.getItemId()));
        boolean rule2InItemOrder = itemOrder.stream()
            .anyMatch(item -> "rules".equals(item.getSectionType()) && "rule2".equals(item.getItemId()));

        assertFalse(rule1InItemOrder, "rule1 should be filtered from itemOrder (referenced by base_rule_group)");
        assertFalse(rule2InItemOrder, "rule2 should be filtered from itemOrder (referenced by base_rule_group)");

        // Verify final itemOrder size: should be 1 (composite_rule_group)
        assertEquals(1, itemOrder.size(), 
            "itemOrder should contain exactly 1 item: composite_rule_group");
    }

    @Test
    @DisplayName("Multiple levels of group references should all be filtered correctly")
    void testMultipleLevelsOfGroupReferences() throws Exception {
        // Load the test YAML file
        File yamlFile = new File("src/test/resources/config/nested-group-references.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // level1_group references level2_group
        // level2_group references level3_group
        // Only level1_group should be in itemOrder

        boolean level1InItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "level1_group".equals(item.getItemId()));
        boolean level2InItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "level2_group".equals(item.getItemId()));
        boolean level3InItemOrder = itemOrder.stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "level3_group".equals(item.getItemId()));

        assertTrue(level1InItemOrder, "level1_group should be in itemOrder (not referenced by other groups)");
        assertFalse(level2InItemOrder, "level2_group should be filtered from itemOrder (referenced by level1_group)");
        assertFalse(level3InItemOrder, "level3_group should be filtered from itemOrder (referenced by level2_group)");

        // Verify final itemOrder size: should be 1 (level1_group)
        assertEquals(1, itemOrder.size(), 
            "itemOrder should contain exactly 1 item: level1_group");
    }

    @Test
    @DisplayName("Groups not referenced by other groups should remain in itemOrder")
    void testUnreferencedGroupsRemainInItemOrder() throws Exception {
        // Load the test YAML file
        File yamlFile = new File("src/test/resources/config/enrichment-group-references.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        // Count enrichment-groups in itemOrder
        long enrichmentGroupCount = itemOrder.stream()
            .filter(item -> "enrichment-groups".equals(item.getSectionType()))
            .count();

        // Should have 3 enrichment-groups: e1_eg, e2_eg, e3_eg (rbg1 is filtered)
        assertEquals(3, enrichmentGroupCount, 
            "Should have exactly 3 enrichment-groups in itemOrder (e1_eg, e2_eg, e3_eg)");
    }

    @Test
    @DisplayName("Verify log messages confirm enrichment-group filtering")
    void testLogMessagesConfirmFiltering() throws Exception {
        // This test verifies that the log messages are correct
        // The actual log output is checked manually, but we can verify the configuration is loaded correctly
        
        File yamlFile = new File("src/test/resources/config/enrichment-group-references.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Verify configuration has the expected structure
        assertNotNull(config.getEnrichmentGroups(), "Configuration should have enrichment groups");
        assertEquals(4, config.getEnrichmentGroups().size(), "Should have 4 enrichment groups defined");

        // Verify rbg1 is in the configuration (defined) but not in itemOrder (filtered)
        boolean rbg1Defined = config.getEnrichmentGroups().stream()
            .anyMatch(group -> "rbg1".equals(group.getId()));
        assertTrue(rbg1Defined, "rbg1 should be defined in configuration");

        boolean rbg1InItemOrder = config.getItemOrder().stream()
            .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "rbg1".equals(item.getItemId()));
        assertFalse(rbg1InItemOrder, "rbg1 should NOT be in itemOrder (filtered by groups-only logic)");
    }

    @Test
    @DisplayName("Enrichments referenced via enrichment-references (structured objects) should be filtered from itemOrder")
    void testEnrichmentReferencesFiltering() throws Exception {
        // Load the test YAML file
        File yamlFile = new File("src/test/resources/config/enrichment-references.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // Verify that e2 and e3 (referenced via enrichment-references) are filtered from itemOrder
        boolean e2InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e2".equals(item.getItemId()));
        boolean e3InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e3".equals(item.getItemId()));

        assertFalse(e2InItemOrder,
                "CRITICAL BUG: enrichment 'e2' should be filtered from itemOrder because it's referenced via enrichment-references by 'eg1'");
        assertFalse(e3InItemOrder,
                "CRITICAL BUG: enrichment 'e3' should be filtered from itemOrder because it's referenced via enrichment-references by 'eg1'");

        // Verify that e1 (referenced via enrichment-ids) is also filtered
        boolean e1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e1".equals(item.getItemId()));
        assertFalse(e1InItemOrder,
                "CRITICAL BUG: enrichment 'e1' should be filtered from itemOrder because it's referenced via enrichment-ids by 'eg1'");

        // Verify that only the enrichment-group remains in itemOrder
        assertEquals(1, itemOrder.size(), "Should have exactly 1 item in itemOrder (the enrichment-group)");
        assertEquals("enrichment-groups", itemOrder.get(0).getSectionType(), "Item should be an enrichment-group");
        assertEquals("eg1", itemOrder.get(0).getItemId(), "Item should be 'eg1'");
    }

    @Test
    @DisplayName("Rules referenced via rule-references (structured objects) should be filtered from itemOrder")
    void testRuleReferencesFiltering() throws Exception {
        // Load the test YAML file
        File yamlFile = new File("src/test/resources/config/rule-references.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // Verify that r2 and r3 (referenced via rule-references) are filtered from itemOrder
        boolean r2InItemOrder = itemOrder.stream()
                .anyMatch(item -> "rules".equals(item.getSectionType()) && "r2".equals(item.getItemId()));
        boolean r3InItemOrder = itemOrder.stream()
                .anyMatch(item -> "rules".equals(item.getSectionType()) && "r3".equals(item.getItemId()));

        assertFalse(r2InItemOrder,
                "CRITICAL BUG: rule 'r2' should be filtered from itemOrder because it's referenced via rule-references by 'rg1'");
        assertFalse(r3InItemOrder,
                "CRITICAL BUG: rule 'r3' should be filtered from itemOrder because it's referenced via rule-references by 'rg1'");

        // Verify that r1 (referenced via rule-ids) is also filtered
        boolean r1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "rules".equals(item.getSectionType()) && "r1".equals(item.getItemId()));
        assertFalse(r1InItemOrder,
                "CRITICAL BUG: rule 'r1' should be filtered from itemOrder because it's referenced via rule-ids by 'rg1'");

        // Verify that only the rule-group remains in itemOrder
        assertEquals(1, itemOrder.size(), "Should have exactly 1 item in itemOrder (the rule-group)");
        assertEquals("rule-groups", itemOrder.get(0).getSectionType(), "Item should be a rule-group");
        assertEquals("rg1", itemOrder.get(0).getItemId(), "Item should be 'rg1'");
    }

    @Test
    @DisplayName("User Example: enrichment-group-references should filter rbg1 from itemOrder")
    void testUserExampleEnrichmentGroupReferences() throws Exception {
        // Load the test YAML file that precisely replicates the user's example
        File yamlFile = new File("src/test/resources/config/user-example-enrichment-group-refs.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("=== USER EXAMPLE TEST ===");
        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // CRITICAL ASSERTION: rbg1 should NOT be in itemOrder because it's referenced by e2_eg
        boolean rbg1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "rbg1".equals(item.getItemId()));

        assertFalse(rbg1InItemOrder,
                "CRITICAL BUG: enrichment-group 'rbg1' should be filtered from itemOrder because it's referenced by 'e2_eg' via enrichment-group-references");

        // Verify that e1_eg, e2_eg, and e3_eg ARE in itemOrder (they are not referenced by other groups)
        boolean e1_egInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e1_eg".equals(item.getItemId()));
        boolean e2_egInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e2_eg".equals(item.getItemId()));
        boolean e3_egInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e3_eg".equals(item.getItemId()));

        assertTrue(e1_egInItemOrder, "e1_eg should be in itemOrder (not referenced by other groups)");
        assertTrue(e2_egInItemOrder, "e2_eg should be in itemOrder (not referenced by other groups)");
        assertTrue(e3_egInItemOrder, "e3_eg should be in itemOrder (not referenced by other groups)");

        // Verify that enrichments e1, e2, rbg1_enrichment are NOT in itemOrder (referenced by groups)
        boolean e1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e1".equals(item.getItemId()));
        boolean e2InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e2".equals(item.getItemId()));
        boolean rbg1_enrichmentInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "rbg1_enrichment".equals(item.getItemId()));

        assertFalse(e1InItemOrder, "e1 should be filtered from itemOrder (referenced by e1_eg)");
        assertFalse(e2InItemOrder, "e2 should be filtered from itemOrder (referenced by e3_eg)");
        assertFalse(rbg1_enrichmentInItemOrder, "rbg1_enrichment should be filtered from itemOrder (referenced by rbg1)");

        // Verify final itemOrder size: should be 3 (e1_eg, e2_eg, e3_eg)
        assertEquals(3, itemOrder.size(),
                "itemOrder should contain exactly 3 items: e1_eg, e2_eg, e3_eg");

        // Verify the exact order
        assertEquals("e1_eg", itemOrder.get(0).getItemId(), "First item should be e1_eg");
        assertEquals("e2_eg", itemOrder.get(1).getItemId(), "Second item should be e2_eg");
        assertEquals("e3_eg", itemOrder.get(2).getItemId(), "Third item should be e3_eg");

        logger.info("=== USER EXAMPLE TEST PASSED ===");
        logger.info("rbg1 correctly filtered from itemOrder");
        logger.info("e1_eg, e2_eg, e3_eg remain in itemOrder");
        logger.info("All enrichments (e1, e2, rbg1_enrichment) filtered from itemOrder");
    }
}

