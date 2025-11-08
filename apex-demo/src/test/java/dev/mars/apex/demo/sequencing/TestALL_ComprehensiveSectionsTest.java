package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.util.ProcessingSequenceReport;
import dev.mars.apex.core.util.YamlProcessingSequenceAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for comprehensive YAML file containing ALL APEX section keywords.
 * 
 * This test validates that the analyzer correctly handles a YAML file with:
 * - ALL 15 APEX section keywords
 * - Multiple items in each list section
 * - Both itemOrder sections and non-itemOrder sections
 * 
 * Expected sections in itemOrder:
 * - enrichments (3 items)
 * - enrichment-groups (3 items)
 * - enrichment-refs (1 placeholder)
 * - rules (3 items)
 * - rule-groups (3 items)
 * - rule-refs (1 placeholder)
 * - transformations (3 items)
 * - rule-chains (3 items)
 * 
 * Total expected: 23 items in itemOrder
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-07
 */
public class TestALL_ComprehensiveSectionsTest {

    private static final Logger LOGGER = Logger.getLogger(TestALL_ComprehensiveSectionsTest.class.getName());

    private YamlProcessingSequenceAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new YamlProcessingSequenceAnalyzer();
    }

    @Test
    void testAnalyzerComprehensiveAllSections() throws Exception {
        LOGGER.info("TEST: Analyzer - Comprehensive All Sections Test");

        // Get the YAML file path (relative to project root)
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/TestALL_ComprehensiveSectionsTest.yaml";

        // Analyze the YAML file
        ProcessingSequenceReport report = analyzer.analyze(yamlPath);
        
        // Print the formatted report
        System.out.println(report.getFormattedReport());
        
        // Verify the report
        assertNotNull(report, "Report should not be null");

        // Expected: 20 items total in itemOrder
        // - enrichments: 3
        // - enrichment-groups: 3
        // - enrichment-refs: 1 (single placeholder for all 3 refs)
        // - rules: 3
        // - rule-groups: 3
        // - rule-refs: 1 (single placeholder for all 3 refs)
        // - transformations: 3
        // - rule-chains: 3
        // Note: data-source-refs does NOT appear in itemOrder (not a LIST_SECTION or REFERENCE_SECTION in itemOrder)
        assertEquals(20, report.getOriginalSequence().size(),
            "Should have 20 items in original sequence");
        
        // Verify enrichments
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("enrichments") && item.getItemId().equals("enrich-1")),
            "Should contain enrich-1");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("enrichments") && item.getItemId().equals("enrich-2")),
            "Should contain enrich-2");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("enrichments") && item.getItemId().equals("enrich-3")),
            "Should contain enrich-3");
        
        // Verify enrichment-groups
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("enrichment-groups") && item.getItemId().equals("enrichment-group-1")),
            "Should contain enrichment-group-1");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("enrichment-groups") && item.getItemId().equals("enrichment-group-2")),
            "Should contain enrichment-group-2");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("enrichment-groups") && item.getItemId().equals("enrichment-group-3")),
            "Should contain enrichment-group-3");
        
        // Verify enrichment-refs (placeholder with itemId = "*")
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("enrichment-refs") && item.getItemId().equals("*")),
            "Should contain enrichment-refs placeholder");

        // Verify rules
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rules") && item.getItemId().equals("rule-1")),
            "Should contain rule-1");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rules") && item.getItemId().equals("rule-2")),
            "Should contain rule-2");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rules") && item.getItemId().equals("rule-3")),
            "Should contain rule-3");

        // Verify rule-groups
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rule-groups") && item.getItemId().equals("rule-group-1")),
            "Should contain rule-group-1");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rule-groups") && item.getItemId().equals("rule-group-2")),
            "Should contain rule-group-2");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rule-groups") && item.getItemId().equals("rule-group-3")),
            "Should contain rule-group-3");

        // Verify rule-refs (placeholder with itemId = "*")
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rule-refs") && item.getItemId().equals("*")),
            "Should contain rule-refs placeholder");
        
        // Verify transformations
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("transformations") && item.getItemId().equals("transform-1")),
            "Should contain transform-1");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("transformations") && item.getItemId().equals("transform-2")),
            "Should contain transform-2");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("transformations") && item.getItemId().equals("transform-3")),
            "Should contain transform-3");
        
        // Verify rule-chains
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rule-chains") && item.getItemId().equals("chain-1")),
            "Should contain chain-1");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rule-chains") && item.getItemId().equals("chain-2")),
            "Should contain chain-2");
        assertTrue(report.getOriginalSequence().stream()
            .anyMatch(item -> item.getSectionType().equals("rule-chains") && item.getItemId().equals("chain-3")),
            "Should contain chain-3");
        
        LOGGER.info("TEST PASSED: Comprehensive all-sections test validated successfully");
    }
}

