package dev.mars.apex.core.config;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import dev.mars.apex.core.config.sequential.ProcessingItem;
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
 * Test that enrichment-refs with enrichment-group-references work correctly.
 * 
 * This test verifies that when:
 * 1. Main file has enrichment-refs that loads an external file with enrichment-groups
 * 2. Main file has enrichment-groups that reference those external enrichment-groups via enrichment-group-references
 * 
 * Then the external enrichment-groups should be filtered from itemOrder (groups-only logic).
 */
@DisplayName("Enrichment-Refs with Enrichment-Group-References Tests")
class EnrichmentRefsWithGroupReferencesTest {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentRefsWithGroupReferencesTest.class);
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("CRITICAL BUG: enrichment-group from enrichment-refs should be filtered when referenced by main file")
    void testEnrichmentRefsWithGroupReferences() throws Exception {
        // Load the main file that has enrichment-refs and enrichment-group-references
        File yamlFile = new File("src/test/resources/config/composite-rulegroup-enrichmentgroup.yaml");
        YamlRuleConfiguration config = loader.loadFromFile(yamlFile);

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("=== ENRICHMENT-REFS WITH GROUP-REFERENCES TEST ===");
        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // CRITICAL ASSERTION: rbg1 should NOT be in itemOrder
        // rbg1 is defined in test.yaml (loaded via enrichment-refs)
        // rbg1 is referenced by e2_eg in the main file via enrichment-group-references
        // Therefore, rbg1 should be filtered from itemOrder (groups-only logic)
        boolean rbg1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "rbg1".equals(item.getItemId()));

        assertFalse(rbg1InItemOrder,
                "CRITICAL BUG: enrichment-group 'rbg1' from test.yaml should be filtered from itemOrder " +
                "because it's referenced by 'e2_eg' in the main file via enrichment-group-references");

        // Verify that e1_eg, e2_eg, e3_eg ARE in itemOrder (they are not referenced by other groups)
        boolean e1_egInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e1_eg".equals(item.getItemId()));
        boolean e2_egInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e2_eg".equals(item.getItemId()));
        boolean e3_egInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && "e3_eg".equals(item.getItemId()));

        assertTrue(e1_egInItemOrder, "e1_eg should be in itemOrder (not referenced by other groups)");
        assertTrue(e2_egInItemOrder, "e2_eg should be in itemOrder (not referenced by other groups)");
        assertTrue(e3_egInItemOrder, "e3_eg should be in itemOrder (not referenced by other groups)");

        // Verify that enrichments e1, e2, e3 are NOT in itemOrder (referenced by groups)
        boolean e1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e1".equals(item.getItemId()));
        boolean e2InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e2".equals(item.getItemId()));
        boolean e3InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && "e3".equals(item.getItemId()));

        assertFalse(e1InItemOrder, "e1 should be filtered from itemOrder (referenced by e1_eg)");
        assertFalse(e2InItemOrder, "e2 should be filtered from itemOrder (referenced by e3_eg)");
        assertFalse(e3InItemOrder, "e3 should be filtered from itemOrder (referenced by rbg1 in test.yaml)");

        // Verify that the rule-group is in itemOrder
        boolean ruleGroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "rule-groups".equals(item.getSectionType()) && 
                         "identify-source-master-message-control-rule-group".equals(item.getItemId()));
        assertTrue(ruleGroupInItemOrder, "Rule group should be in itemOrder");

        // Verify that rules are NOT in itemOrder (referenced by rule-group)
        boolean rule1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "rules".equals(item.getSectionType()) && "is-source-identifier-populated".equals(item.getItemId()));
        boolean rule2InItemOrder = itemOrder.stream()
                .anyMatch(item -> "rules".equals(item.getSectionType()) && "is-source-identifier-valid".equals(item.getItemId()));

        assertFalse(rule1InItemOrder, "is-source-identifier-populated should be filtered from itemOrder (referenced by rule-group)");
        assertFalse(rule2InItemOrder, "is-source-identifier-valid should be filtered from itemOrder (referenced by rule-group)");

        logger.info("=== TEST RESULTS ===");
        logger.info("e1_eg, e2_eg, e3_eg in itemOrder: {}, {}, {}", e1_egInItemOrder, e2_egInItemOrder, e3_egInItemOrder);
        logger.info("rbg1 filtered from itemOrder: {}", !rbg1InItemOrder);
        logger.info("e1, e2, e3 filtered from itemOrder: {}, {}, {}", !e1InItemOrder, !e2InItemOrder, !e3InItemOrder);
        logger.info("Rule group in itemOrder: {}", ruleGroupInItemOrder);
        logger.info("Rules filtered from itemOrder: {}, {}", !rule1InItemOrder, !rule2InItemOrder);
    }
}

