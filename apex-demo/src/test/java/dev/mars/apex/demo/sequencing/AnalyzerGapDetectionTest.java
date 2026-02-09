package dev.mars.apex.demo.sequencing;

import dev.mars.apex.core.config.yaml.sequential.ProcessingItem;
import dev.mars.apex.core.util.ProcessingSequenceReport;
import dev.mars.apex.core.util.YamlProcessingSequenceAnalyzer;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRITICAL GAP DETECTION TEST
 * 
 * <p>This test exposes gaps in the YamlProcessingSequenceAnalyzer by testing
 * YAML sections that are NOT currently handled by the analyzer.
 * 
 * <p>CRITICAL GAPS:
 * <ul>
 *   <li>transformations - IS in itemOrder but analyzer doesn't handle it</li>
 *   <li>data-sources - Section-level only (not in itemOrder)</li>
 *   <li>data-sinks - Section-level only (not in itemOrder)</li>
 *   <li>pipeline - Section-level only (not in itemOrder)</li>
 *   <li>categories - Section-level only (not in itemOrder)</li>
 *   <li>error-recovery - Section-level only (not in itemOrder)</li>
 * </ul>
 */
@DisplayName("Analyzer Gap Detection Test")
public class AnalyzerGapDetectionTest extends DemoTestBase {

    private final YamlProcessingSequenceAnalyzer analyzer = new YamlProcessingSequenceAnalyzer();

    @Test
    @DisplayName("GAP 1: Analyzer handles transformations in itemOrder")
    void testAnalyzerHandlesTransformations() {
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test8_TransformationsBasicTest.yaml";

        ProcessingSequenceReport report = analyzer.analyze(yamlPath);

        // Print the formatted report to show individual items
        System.out.println("\n" + report.getFormattedReport());

        // Verify transformations are in original sequence
        List<String> originalIds = report.getOriginalSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertTrue(originalIds.contains("transform-1"),
            "Analyzer MUST include transform-1 in original sequence");
        assertTrue(originalIds.contains("transform-2"),
            "Analyzer MUST include transform-2 in original sequence");
        assertTrue(originalIds.contains("transform-3"),
            "Analyzer MUST include transform-3 in original sequence");

        // Verify transformations are in planned sequence (no groups-only filtering)
        List<String> plannedIds = report.getPlannedSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of("transform-1", "transform-2", "transform-3"), plannedIds,
            "Analyzer MUST preserve transformation order in planned sequence");

        System.out.println("GAP 1 TEST PASSED: Analyzer correctly handles transformations");
    }

    @Test
    @DisplayName("GAP 2: Analyzer handles numbered suffix transformations")
    void testAnalyzerHandlesNumberedTransformations() {
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test9_TransformationsNumberedSuffixesTest.yaml";
        
        ProcessingSequenceReport report = analyzer.analyze(yamlPath);
        
        List<String> plannedIds = report.getPlannedSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());
        
        // Verify document order is preserved: batch1-t1, batch1-t2, batch2-t1, batch2-t2, batch3-t1, batch3-t2
        assertEquals(List.of("batch1-t1", "batch1-t2", "batch2-t1", "batch2-t2", "batch3-t1", "batch3-t2"), 
            plannedIds,
            "Analyzer MUST preserve numbered suffix transformation order");
        
        System.out.println("GAP 2 TEST PASSED: Analyzer correctly handles numbered suffix transformations");
    }

    @Test
    @DisplayName("GAP 3: Analyzer handles mixed enrichments and transformations")
    void testAnalyzerHandlesMixedEnrichmentsAndTransformations() {
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test10_MixedEnrichmentsAndTransformationsTest.yaml";

        ProcessingSequenceReport report = analyzer.analyze(yamlPath);

        // Print the formatted report to show individual items
        System.out.println("\n" + report.getFormattedReport());

        List<String> plannedIds = report.getPlannedSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        // Verify interleaved order: enrich-1, transform-1, enrich-2, transform-2, enrich-3
        assertEquals(List.of("enrich-1", "transform-1", "enrich-2", "transform-2", "enrich-3"),
            plannedIds,
            "Analyzer MUST preserve interleaved enrichment/transformation order");

        System.out.println("GAP 3 TEST PASSED: Analyzer correctly handles mixed enrichments and transformations");
    }

    @Test
    @DisplayName("GAP 4: Analyzer tracks section-level sections (data-sources, pipeline, data-sinks)")
    void testAnalyzerTracksSectionLevelSections() {
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test11_DataSourcesAndPipelineTest.yaml";
        
        ProcessingSequenceReport report = analyzer.analyze(yamlPath);
        
        // Section-level sections (data-sources, pipeline, data-sinks) should NOT be in itemOrder
        // but should be tracked in sectionOrder
        
        List<String> plannedIds = report.getPlannedSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());
        
        // Verify only items (not sections) are in planned sequence
        assertEquals(List.of("enrich-1", "rule-1"), plannedIds,
            "Only items (enrichments, rules) should be in itemOrder, not section-level sections");
        
        // TODO: Add sectionOrder validation when analyzer supports it
        // Expected sectionOrder: [data-sources, enrichments, pipeline, rules, data-sinks]
        
        System.out.println("GAP 4 TEST PASSED: Analyzer correctly excludes section-level sections from itemOrder");
        System.out.println("NOTE: sectionOrder validation not yet implemented in analyzer");
    }

    @Test
    @DisplayName("GAP 5: Analyzer report shows section types correctly")
    void testAnalyzerReportShowsSectionTypes() {
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test10_MixedEnrichmentsAndTransformationsTest.yaml";
        
        ProcessingSequenceReport report = analyzer.analyze(yamlPath);
        
        // Verify section types are correct
        List<ProcessingItem> plannedSequence = report.getPlannedSequence();
        
        assertEquals("enrichments", plannedSequence.get(0).getSectionType(), 
            "First item should be enrichments type");
        assertEquals("transformations", plannedSequence.get(1).getSectionType(), 
            "Second item should be transformations type");
        assertEquals("enrichments", plannedSequence.get(2).getSectionType(), 
            "Third item should be enrichments type");
        assertEquals("transformations", plannedSequence.get(3).getSectionType(), 
            "Fourth item should be transformations type");
        assertEquals("enrichments", plannedSequence.get(4).getSectionType(), 
            "Fifth item should be enrichments type");
        
        System.out.println("GAP 5 TEST PASSED: Analyzer correctly identifies section types");
        System.out.println("\n" + report.getFormattedReport());
    }

    @Test
    @DisplayName("Analyzer shows MULTIPLE items in SAME section")
    void testAnalyzerShowsMultipleItemsInSameSection() {
        // Test4B has 4 enrichment items in ONE enrichments section
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test4B_AllStandaloneTest.yaml";

        ProcessingSequenceReport report = analyzer.analyze(yamlPath);

        // Print the formatted report
        System.out.println("\n========== CRITICAL TEST: Multiple Items in Same Section ==========");
        System.out.println(report.getFormattedReport());

        // Verify all 4 items are shown
        List<String> plannedIds = report.getPlannedSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of("standalone-1", "standalone-2", "standalone-3", "standalone-4"), plannedIds,
            "Analyzer MUST show all 4 enrichment items in document order");

        System.out.println("CRITICAL TEST PASSED: Analyzer shows all 4 items in their YAML file order");
    }

    @Test
    @DisplayName("COMPLEX YAML - Multiple sections with multiple items AND groups-only logic")
    void testAnalyzerComplexYamlWithGroupsOnlyLogic() {
        // Test4 has:
        // - enrichments section with 4 items (standalone-1, grouped-1, standalone-2, grouped-2)
        // - enrichment-groups section with 1 item (group-A)
        // - Groups-only logic: grouped-1 and grouped-2 should be FILTERED (execute via group only)
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test4_StandaloneEnrichmentsTest.yaml";

        ProcessingSequenceReport report = analyzer.analyze(yamlPath);

        // Print the formatted report
        System.out.println("\n========== CRITICAL TEST: COMPLEX YAML with Groups-Only Logic ==========");
        System.out.println(report.getFormattedReport());

        // Verify ORIGINAL sequence shows ALL items in document order
        List<String> originalIds = report.getOriginalSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of("standalone-1", "grouped-1", "standalone-2", "grouped-2", "group-A"), originalIds,
            "ORIGINAL sequence MUST show all 5 items in YAML document order");

        // Verify FILTERED items (grouped-1 and grouped-2 are filtered by groups-only logic)
        List<String> filteredIds = report.getFilteredItems().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of("grouped-1", "grouped-2"), filteredIds,
            "FILTERED items MUST be grouped-1 and grouped-2 (execute via group only)");

        // Verify PLANNED sequence (after filtering)
        List<String> plannedIds = report.getPlannedSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of("standalone-1", "standalone-2", "group-A"), plannedIds,
            "PLANNED sequence MUST be standalone-1, standalone-2, group-A (grouped items filtered out)");

        System.out.println("CRITICAL TEST PASSED: Complex YAML with groups-only logic correctly analyzed");
    }

    @Test
    @DisplayName("MOST COMPLEX YAML - Numbered suffixes + multiple groups + interleaving")
    void testAnalyzerMostComplexYaml() {
        // Test6B has:
        // - enrichments-1 (2 items: standalone-1, grouped-A1)
        // - enrichment-groups-1 (1 item: group-A)
        // - enrichments-2 (3 items: standalone-2, grouped-A2, grouped-B1)
        // - enrichment-groups-2 (1 item: group-B)
        // - enrichments-3 (1 item: standalone-3)
        // Total: 8 items across 5 sections with complex groups-only logic
        String yamlPath = "src/test/java/dev/mars/apex/demo/sequencing/Test6B_ComplexNumberedWithGroupsTest.yaml";

        ProcessingSequenceReport report = analyzer.analyze(yamlPath);

        // Print the formatted report
        System.out.println("\n========== MOST COMPLEX YAML TEST ==========");
        System.out.println(report.getFormattedReport());

        // Verify ORIGINAL sequence shows ALL items in document order
        List<String> originalIds = report.getOriginalSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of(
            "standalone-1", "grouped-A1",  // enrichments-1
            "group-A",                      // enrichment-groups-1
            "standalone-2", "grouped-A2", "grouped-B1",  // enrichments-2
            "group-B",                      // enrichment-groups-2
            "standalone-3"                  // enrichments-3
        ), originalIds, "ORIGINAL sequence MUST show all 8 items in exact YAML document order");

        // Verify FILTERED items (grouped-A1, grouped-A2, grouped-B1 are filtered by groups-only logic)
        List<String> filteredIds = report.getFilteredItems().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of("grouped-A1", "grouped-A2", "grouped-B1"), filteredIds,
            "FILTERED items MUST be grouped-A1, grouped-A2, grouped-B1 (execute via groups only)");

        // Verify PLANNED sequence (after filtering)
        List<String> plannedIds = report.getPlannedSequence().stream()
            .map(ProcessingItem::getItemId)
            .collect(Collectors.toList());

        assertEquals(List.of("standalone-1", "group-A", "standalone-2", "group-B", "standalone-3"), plannedIds,
            "PLANNED sequence MUST preserve document order after filtering grouped items");

        System.out.println("MOST COMPLEX YAML TEST PASSED: Numbered suffixes + multiple groups + interleaving correctly analyzed");
    }
}

