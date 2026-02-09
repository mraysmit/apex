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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that enrichment-refs with CLASSPATH references work correctly with groups-only logic.
 * 
 * This test verifies that when:
 * 1. Main file has enrichment-refs that loads an external file from CLASSPATH (not file system)
 * 2. External file contains enrichment-groups
 * 3. Main file has enrichment-groups that reference those external enrichment-groups
 * 
 * Then the external enrichment-groups should be filtered from itemOrder (groups-only logic).
 * 
 * This is an edge case test to ensure that groups-only logic works for BOTH:
 * - File system references (tested in EnrichmentRefsWithGroupReferencesTest)
 * - Classpath references (tested here)
 */
@DisplayName("Classpath Enrichment-Refs with Group-References Tests")
class ClasspathRefsWithGroupReferencesTest {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathRefsWithGroupReferencesTest.class);
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("EDGE CASE: enrichment-group from CLASSPATH enrichment-refs should be filtered when referenced")
    void testClasspathEnrichmentRefsWithGroupReferences() throws Exception {
        // Load the main file from classpath
        // This file references "config/classpath-external.yaml" from classpath
        YamlRuleConfiguration config = loader.loadFromClasspath("config/classpath-main.yaml");

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();

        logger.info("=== CLASSPATH ENRICHMENT-REFS WITH GROUP-REFERENCES TEST ===");
        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // CRITICAL ASSERTION: external_group_1 should NOT be in itemOrder
        // external_group_1 is defined in classpath-external.yaml (loaded via enrichment-refs from CLASSPATH)
        // external_group_1 is referenced by composite_group in the main file via enrichment-group-references
        // Therefore, external_group_1 should be filtered from itemOrder (groups-only logic)
        boolean externalGroup1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "external_group_1".equals(item.getItemId()));

        assertFalse(externalGroup1InItemOrder,
                "EDGE CASE BUG: enrichment-group 'external_group_1' from classpath-external.yaml should be filtered from itemOrder " +
                "because it's referenced by 'composite_group' in the main file via enrichment-group-references");

        // Verify that composite_group IS in itemOrder (it's not referenced by other groups)
        boolean compositeGroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "composite_group".equals(item.getItemId()));

        assertTrue(compositeGroupInItemOrder, 
                "composite_group should be in itemOrder (not referenced by other groups)");

        // Verify that enrichments are NOT in itemOrder (referenced by groups)
        boolean externalEnrichment1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && 
                         "external_enrichment_1".equals(item.getItemId()));
        boolean localEnrichment1InItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichments".equals(item.getSectionType()) && 
                         "local_enrichment_1".equals(item.getItemId()));

        assertFalse(externalEnrichment1InItemOrder, 
                "external_enrichment_1 should be filtered from itemOrder (referenced by external_group_1)");
        assertFalse(localEnrichment1InItemOrder, 
                "local_enrichment_1 should be filtered from itemOrder (referenced by composite_group)");

        // Verify final itemOrder size: should be 1 (composite_group only)
        assertEquals(1, itemOrder.size(), 
                "itemOrder should contain exactly 1 item: composite_group");

        logger.info("=== TEST RESULTS ===");
        logger.info("composite_group in itemOrder: {}", compositeGroupInItemOrder);
        logger.info("external_group_1 filtered from itemOrder: {}", !externalGroup1InItemOrder);
        logger.info("external_enrichment_1 filtered from itemOrder: {}", !externalEnrichment1InItemOrder);
        logger.info("local_enrichment_1 filtered from itemOrder: {}", !localEnrichment1InItemOrder);
        logger.info("Final itemOrder size: {}", itemOrder.size());
    }
}

