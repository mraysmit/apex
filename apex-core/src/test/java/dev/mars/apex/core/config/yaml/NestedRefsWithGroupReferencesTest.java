package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that NESTED enrichment-refs work correctly with groups-only logic.
 * 
 * This test verifies that when:
 * 1. Main file has enrichment-refs that loads Level 1 file
 * 2. Level 1 file ITSELF has enrichment-refs that loads Level 2 file
 * 3. Level 2 file contains enrichment-groups
 * 4. Level 1 file has enrichment-groups that reference Level 2 enrichment-groups
 * 5. Main file has enrichment-groups that reference Level 1 enrichment-groups
 * 
 * Then ALL referenced enrichment-groups (Level 1 and Level 2) should be filtered from itemOrder.
 * 
 * This is an edge case test to ensure that groups-only logic works for:
 * - Multi-level nested file references (file A -> file B -> file C)
 * - Groups-only logic applies across ALL levels of nesting
 */
@DisplayName("Nested Enrichment-Refs with Group-References Tests")
class NestedRefsWithGroupReferencesTest {

    private static final Logger logger = LoggerFactory.getLogger(NestedRefsWithGroupReferencesTest.class);
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("EDGE CASE: nested enrichment-refs should work with groups-only logic across all levels")
    void testNestedEnrichmentRefsWithGroupReferences() throws Exception {
        // Load the main file from classpath
        // This file references level1 which references level2 (nested refs)
        YamlRuleConfiguration config = loader.loadFromClasspath("config/nested-main.yaml");

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("=== NESTED ENRICHMENT-REFS WITH GROUP-REFERENCES TEST ===");
        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // CRITICAL ASSERTION 1: level2_group should NOT be in itemOrder
        // level2_group is defined in nested-level2.yaml (loaded via nested enrichment-refs)
        // level2_group is referenced by level1_group in nested-level1.yaml
        // Therefore, level2_group should be filtered from itemOrder (groups-only logic)
        boolean level2GroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "level2_group".equals(item.getItemId()));

        assertFalse(level2GroupInItemOrder,
                "EDGE CASE BUG: enrichment-group 'level2_group' from nested-level2.yaml should be filtered from itemOrder " +
                "because it's referenced by 'level1_group' in nested-level1.yaml via enrichment-group-references");

        // CRITICAL ASSERTION 2: level1_group should NOT be in itemOrder
        // level1_group is defined in nested-level1.yaml (loaded via enrichment-refs)
        // level1_group is referenced by main_group in nested-main.yaml
        // Therefore, level1_group should be filtered from itemOrder (groups-only logic)
        boolean level1GroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "level1_group".equals(item.getItemId()));

        assertFalse(level1GroupInItemOrder,
                "EDGE CASE BUG: enrichment-group 'level1_group' from nested-level1.yaml should be filtered from itemOrder " +
                "because it's referenced by 'main_group' in nested-main.yaml via enrichment-group-references");

        // Verify that main_group IS in itemOrder (it's not referenced by other groups)
        boolean mainGroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "main_group".equals(item.getItemId()));

        assertTrue(mainGroupInItemOrder, 
                "main_group should be in itemOrder (not referenced by other groups)");

        // Verify that ALL enrichments are NOT in itemOrder (all referenced by groups)
        boolean level2EnrichmentInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && 
                         "level2_enrichment".equals(item.getItemId()));
        boolean level1EnrichmentInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && 
                         "level1_enrichment".equals(item.getItemId()));
        boolean mainEnrichmentInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && 
                         "main_enrichment".equals(item.getItemId()));

        assertFalse(level2EnrichmentInItemOrder, 
                "level2_enrichment should be filtered from itemOrder (referenced by level2_group)");
        assertFalse(level1EnrichmentInItemOrder, 
                "level1_enrichment should be filtered from itemOrder (referenced by level1_group)");
        assertFalse(mainEnrichmentInItemOrder, 
                "main_enrichment should be filtered from itemOrder (referenced by main_group)");

        // Verify final itemOrder size: should be 1 (main_group only)
        assertEquals(1, itemOrder.size(), 
                "itemOrder should contain exactly 1 item: main_group");

        logger.info("=== TEST RESULTS ===");
        logger.info("main_group in itemOrder: {}", mainGroupInItemOrder);
        logger.info("level1_group filtered from itemOrder: {}", !level1GroupInItemOrder);
        logger.info("level2_group filtered from itemOrder: {}", !level2GroupInItemOrder);
        logger.info("level2_enrichment filtered from itemOrder: {}", !level2EnrichmentInItemOrder);
        logger.info("level1_enrichment filtered from itemOrder: {}", !level1EnrichmentInItemOrder);
        logger.info("main_enrichment filtered from itemOrder: {}", !mainEnrichmentInItemOrder);
        logger.info("Final itemOrder size: {}", itemOrder.size());
    }
}

