package dev.mars.apex.core.config;

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
 * Test that mixed references (file system + classpath) work correctly with groups-only logic.
 * 
 * This test verifies that when:
 * 1. Main file has enrichment-refs that loads files from BOTH file system AND classpath
 * 2. Main file has enrichment-groups that reference those external enrichment-groups
 * 
 * Then BOTH external enrichment-groups (from file system and classpath) should be filtered from itemOrder.
 * 
 * This is an edge case test to ensure that APEX's groups-only logic works correctly
 * regardless of whether external files are loaded from file system or classpath.
 */
@DisplayName("Mixed References (File System + Classpath) with Groups-Only Logic Tests")
class MixedRefsWithGroupReferencesTest {

    private static final Logger logger = LoggerFactory.getLogger(MixedRefsWithGroupReferencesTest.class);
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("EDGE CASE: enrichment-groups from mixed references (file system + classpath) should be filtered")
    void testMixedReferencesWithGroupsOnlyLogic() throws Exception {
        // Load the main file that references both file system and classpath files
        YamlRuleConfiguration config = loader.loadFromClasspath("config/mixed-main.yaml");

        logger.info("=== MIXED REFERENCES WITH GROUPS-ONLY LOGIC TEST ===");

        // Verify that both external groups are present in the configuration
        assertNotNull(config.getEnrichmentGroups(), "Enrichment groups should not be null");
        
        boolean hasFilesystemGroup = config.getEnrichmentGroups().stream()
                .anyMatch(g -> "filesystem_group".equals(g.getId()));
        boolean hasClasspathGroup = config.getEnrichmentGroups().stream()
                .anyMatch(g -> "classpath_group".equals(g.getId()));
        boolean hasCompositeGroup = config.getEnrichmentGroups().stream()
                .anyMatch(g -> "composite_mixed_group".equals(g.getId()));

        assertTrue(hasFilesystemGroup, "Should have filesystem_group from mixed-filesystem.yaml");
        assertTrue(hasClasspathGroup, "Should have classpath_group from mixed-classpath.yaml");
        assertTrue(hasCompositeGroup, "Should have composite_mixed_group from main file");

        // Get the filtered itemOrder
        List<ProcessingItem> itemOrder = config.getItemOrder();
        logger.info("Item order after groups-only logic: {} items", itemOrder.size());
        for (ProcessingItem item : itemOrder) {
            logger.info("  - {} : {}", item.getSectionType(), item.getItemId());
        }

        // CRITICAL ASSERTION 1: filesystem_group should NOT be in itemOrder
        boolean filesystemGroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "filesystem_group".equals(item.getItemId()));

        assertFalse(filesystemGroupInItemOrder,
                "EDGE CASE BUG: enrichment-group 'filesystem_group' from file system should be filtered from itemOrder " +
                "because it's referenced by 'composite_mixed_group' via enrichment-group-references");

        // CRITICAL ASSERTION 2: classpath_group should NOT be in itemOrder
        boolean classpathGroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "classpath_group".equals(item.getItemId()));

        assertFalse(classpathGroupInItemOrder,
                "EDGE CASE BUG: enrichment-group 'classpath_group' from classpath should be filtered from itemOrder " +
                "because it's referenced by 'composite_mixed_group' via enrichment-group-references");

        // ASSERTION 3: composite_mixed_group SHOULD be in itemOrder (it's not referenced by anyone)
        boolean compositeGroupInItemOrder = itemOrder.stream()
                .anyMatch(item -> "enrichment-groups".equals(item.getSectionType()) && 
                         "composite_mixed_group".equals(item.getItemId()));

        assertTrue(compositeGroupInItemOrder,
                "composite_mixed_group should be in itemOrder because it's not referenced by any other group");

        logger.info("=== TEST RESULTS ===");
        logger.info("filesystem_group filtered: {}", !filesystemGroupInItemOrder);
        logger.info("classpath_group filtered: {}", !classpathGroupInItemOrder);
        logger.info("composite_mixed_group in itemOrder: {}", compositeGroupInItemOrder);
        logger.info("Groups-only logic works correctly with mixed references (file system + classpath)");
    }
}

