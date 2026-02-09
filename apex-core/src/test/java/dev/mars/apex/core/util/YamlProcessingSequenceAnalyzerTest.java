package dev.mars.apex.core.util;

import dev.mars.apex.core.config.sequential.ProcessingItem;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for YamlProcessingSequenceAnalyzer.
 *
 * <p>These tests validate that the analyzer correctly identifies:
 * <ul>
 *   <li>Original sequence from YAML document order</li>
 *   <li>Planned sequence after groups-only logic filtering</li>
 *   <li>Filtered items (items that execute via groups only)</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> Validation tests that prove analyzer accuracy against actual execution
 * are in apex-demo module: {@code YamlProcessingSequenceAnalyzerValidationTest}
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("YamlProcessingSequenceAnalyzer Tests")
class YamlProcessingSequenceAnalyzerTest {

    private YamlProcessingSequenceAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new YamlProcessingSequenceAnalyzer();
    }
    
    @Test
    @DisplayName("Test 1: Simple enrichments with no groups - all execute at definition position")
    void testSimpleEnrichmentsNoGroups() {
        String yaml = """
            metadata:
              name: "Simple Enrichments"
            
            enrichments:
              - id: "enrich-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value1'"
                  result-field: "field1"
                field-mappings:
                  - source-field: "field1"
                    target-field: "field1"
                    
              - id: "enrich-2"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value2'"
                  result-field: "field2"
                field-mappings:
                  - source-field: "field2"
                    target-field: "field2"
                    
              - id: "enrich-3"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value3'"
                  result-field: "field3"
                field-mappings:
                  - source-field: "field3"
                    target-field: "field3"
            """;
        
        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test1");
        
        // Original sequence: 3 enrichments
        assertEquals(3, report.getOriginalSequence().size());
        assertEquals("enrichments:enrich-1", report.getOriginalSequence().get(0).toString());
        assertEquals("enrichments:enrich-2", report.getOriginalSequence().get(1).toString());
        assertEquals("enrichments:enrich-3", report.getOriginalSequence().get(2).toString());
        
        // No filtering - all execute at definition position
        assertEquals(0, report.getFilteredItems().size());
        
        // Planned sequence: same as original
        assertEquals(3, report.getPlannedSequence().size());
        assertEquals("enrichments:enrich-1", report.getPlannedSequence().get(0).toString());
        assertEquals("enrichments:enrich-2", report.getPlannedSequence().get(1).toString());
        assertEquals("enrichments:enrich-3", report.getPlannedSequence().get(2).toString());
    }
    
    @Test
    @DisplayName("Test 2: Enrichments with groups - grouped items filtered out")
    void testEnrichmentsWithGroups() {
        String yaml = """
            metadata:
              name: "Enrichments with Groups"
            
            enrichments:
              - id: "standalone-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value1'"
                  result-field: "field1"
                field-mappings:
                  - source-field: "field1"
                    target-field: "field1"
                    
              - id: "grouped-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value2'"
                  result-field: "field2"
                field-mappings:
                  - source-field: "field2"
                    target-field: "field2"
                    
              - id: "standalone-2"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value3'"
                  result-field: "field3"
                field-mappings:
                  - source-field: "field3"
                    target-field: "field3"
                    
              - id: "grouped-2"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value4'"
                  result-field: "field4"
                field-mappings:
                  - source-field: "field4"
                    target-field: "field4"
                    
            enrichment-groups:
              - id: "group-A"
                name: "Group A"
                enrichment-ids:
                  - "grouped-1"
                  - "grouped-2"
            """;
        
        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test2");
        
        // Original sequence: 4 enrichments + 1 group = 5 items
        assertEquals(5, report.getOriginalSequence().size());
        assertEquals("enrichments:standalone-1", report.getOriginalSequence().get(0).toString());
        assertEquals("enrichments:grouped-1", report.getOriginalSequence().get(1).toString());
        assertEquals("enrichments:standalone-2", report.getOriginalSequence().get(2).toString());
        assertEquals("enrichments:grouped-2", report.getOriginalSequence().get(3).toString());
        assertEquals("enrichment-groups:group-A", report.getOriginalSequence().get(4).toString());
        
        // Filtered items: grouped-1 and grouped-2
        assertEquals(2, report.getFilteredItems().size());
        assertTrue(report.getFilteredItems().stream().anyMatch(i -> i.toString().equals("enrichments:grouped-1")));
        assertTrue(report.getFilteredItems().stream().anyMatch(i -> i.toString().equals("enrichments:grouped-2")));
        
        // Planned sequence: standalone-1, standalone-2, group-A
        assertEquals(3, report.getPlannedSequence().size());
        assertEquals("enrichments:standalone-1", report.getPlannedSequence().get(0).toString());
        assertEquals("enrichments:standalone-2", report.getPlannedSequence().get(1).toString());
        assertEquals("enrichment-groups:group-A", report.getPlannedSequence().get(2).toString());
    }
    
    @Test
    @DisplayName("Test 3: Rules with rule-groups - grouped rules filtered out")
    void testRulesWithRuleGroups() {
        String yaml = """
            metadata:
              name: "Rules with Rule Groups"
            
            rules:
              - id: "standalone-rule-1"
                name: "Standalone Rule 1"
                condition: "true"
                message: "Standalone 1"
                    
              - id: "grouped-rule-1"
                name: "Grouped Rule 1"
                condition: "true"
                message: "Grouped 1"
                    
              - id: "standalone-rule-2"
                name: "Standalone Rule 2"
                condition: "true"
                message: "Standalone 2"
                    
              - id: "grouped-rule-2"
                name: "Grouped Rule 2"
                condition: "true"
                message: "Grouped 2"
                    
            rule-groups:
              - id: "rule-group-A"
                name: "Rule Group A"
                rule-ids:
                  - "grouped-rule-1"
                  - "grouped-rule-2"
            """;
        
        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test3");
        
        // Original sequence: 4 rules + 1 rule-group = 5 items
        assertEquals(5, report.getOriginalSequence().size());
        
        // Filtered items: grouped-rule-1 and grouped-rule-2
        assertEquals(2, report.getFilteredItems().size());
        assertTrue(report.getFilteredItems().stream().anyMatch(i -> i.toString().equals("rules:grouped-rule-1")));
        assertTrue(report.getFilteredItems().stream().anyMatch(i -> i.toString().equals("rules:grouped-rule-2")));
        
        // Planned sequence: standalone-rule-1, standalone-rule-2, rule-group-A
        assertEquals(3, report.getPlannedSequence().size());
        assertEquals("rules:standalone-rule-1", report.getPlannedSequence().get(0).toString());
        assertEquals("rules:standalone-rule-2", report.getPlannedSequence().get(1).toString());
        assertEquals("rule-groups:rule-group-A", report.getPlannedSequence().get(2).toString());
    }
    
    @Test
    @DisplayName("Test 4: Complex interleaving - enrichments, rules, groups")
    void testComplexInterleaving() {
        String yaml = """
            metadata:
              name: "Complex Interleaving"
            
            enrichments:
              - id: "enrich-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value1'"
                  result-field: "field1"
                field-mappings:
                  - source-field: "field1"
                    target-field: "field1"
                    
            rules:
              - id: "rule-1"
                name: "Rule 1"
                condition: "true"
                message: "Rule 1"
                    
            enrichments-1:
              - id: "enrich-2"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value2'"
                  result-field: "field2"
                field-mappings:
                  - source-field: "field2"
                    target-field: "field2"
                    
            rules-1:
              - id: "rule-2"
                name: "Rule 2"
                condition: "true"
                message: "Rule 2"
            """;
        
        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test4");
        
        // Original sequence: enrich-1, rule-1, enrich-2, rule-2
        assertEquals(4, report.getOriginalSequence().size());
        assertEquals("enrichments:enrich-1", report.getOriginalSequence().get(0).toString());
        assertEquals("rules:rule-1", report.getOriginalSequence().get(1).toString());
        assertEquals("enrichments:enrich-2", report.getOriginalSequence().get(2).toString());
        assertEquals("rules:rule-2", report.getOriginalSequence().get(3).toString());
        
        // No groups - no filtering
        assertEquals(0, report.getFilteredItems().size());
        
        // Planned sequence: same as original
        assertEquals(4, report.getPlannedSequence().size());
    }
    
    @Test
    @DisplayName("Test 5: Report formatting")
    void testReportFormatting() {
        String yaml = """
            metadata:
              name: "Test Report"

            enrichments:
              - id: "enrich-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value1'"
                  result-field: "field1"
                field-mappings:
                  - source-field: "field1"
                    target-field: "field1"
            """;

        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test5");
        String formatted = report.getFormattedReport();

        // Verify report contains key sections
        assertTrue(formatted.contains("YAML PROCESSING SEQUENCE ANALYSIS"));
        assertTrue(formatted.contains("EXECUTION SEQUENCE"));
        assertTrue(formatted.contains("PLANNED EXECUTION SEQUENCE"));
        assertTrue(formatted.contains("SUMMARY"));
        assertTrue(formatted.contains("test5"));
    }

    @Test
    @DisplayName("Test 6: Empty YAML - no items")
    void testEmptyYaml() {
        String yaml = """
            metadata:
              name: "Empty YAML"
              description: "No processing items"
            """;

        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test6");

        assertEquals(0, report.getOriginalSequence().size());
        assertEquals(0, report.getPlannedSequence().size());
        assertEquals(0, report.getFilteredItems().size());
    }

    @Test
    @DisplayName("Test 7: All items grouped - nothing executes at definition position")
    void testAllItemsGrouped() {
        String yaml = """
            metadata:
              name: "All Items Grouped"

            enrichments:
              - id: "grouped-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value1'"
                  result-field: "field1"
                field-mappings:
                  - source-field: "field1"
                    target-field: "field1"

              - id: "grouped-2"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value2'"
                  result-field: "field2"
                field-mappings:
                  - source-field: "field2"
                    target-field: "field2"

            enrichment-groups:
              - id: "group-A"
                name: "Group A"
                enrichment-ids:
                  - "grouped-1"
                  - "grouped-2"
            """;

        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test7");

        // Original: 2 enrichments + 1 group = 3 items
        assertEquals(3, report.getOriginalSequence().size());

        // Filtered: both enrichments
        assertEquals(2, report.getFilteredItems().size());

        // Planned: only the group
        assertEquals(1, report.getPlannedSequence().size());
        assertEquals("enrichment-groups:group-A", report.getPlannedSequence().get(0).toString());
    }

    @Test
    @DisplayName("Test 8: Numbered suffixes preserved in document order")
    void testNumberedSuffixes() {
        String yaml = """
            metadata:
              name: "Numbered Suffixes"

            enrichments:
              - id: "enrich-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value1'"
                  result-field: "field1"
                field-mappings:
                  - source-field: "field1"
                    target-field: "field1"

            rules:
              - id: "rule-1"
                name: "Rule 1"
                condition: "true"
                message: "Rule 1"

            enrichments-1:
              - id: "enrich-2"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value2'"
                  result-field: "field2"
                field-mappings:
                  - source-field: "field2"
                    target-field: "field2"

            rules-1:
              - id: "rule-2"
                name: "Rule 2"
                condition: "true"
                message: "Rule 2"

            enrichments-2:
              - id: "enrich-3"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value3'"
                  result-field: "field3"
                field-mappings:
                  - source-field: "field3"
                    target-field: "field3"
            """;

        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test8");

        // Verify document order is preserved: E1, R1, E2, R2, E3
        assertEquals(5, report.getOriginalSequence().size());
        assertEquals("enrichments:enrich-1", report.getOriginalSequence().get(0).toString());
        assertEquals("rules:rule-1", report.getOriginalSequence().get(1).toString());
        assertEquals("enrichments:enrich-2", report.getOriginalSequence().get(2).toString());
        assertEquals("rules:rule-2", report.getOriginalSequence().get(3).toString());
        assertEquals("enrichments:enrich-3", report.getOriginalSequence().get(4).toString());
    }

    @Test
    @DisplayName("Test 9: File not found - throws exception")
    void testFileNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.analyze("nonexistent-file.yaml");
        });
    }

    @Test
    @DisplayName("Test 10: toString returns formatted report")
    void testToString() {
        String yaml = """
            metadata:
              name: "Test ToString"

            enrichments:
              - id: "enrich-1"
                type: "calculation-enrichment"
                calculation-config:
                  expression: "'value1'"
                  result-field: "field1"
                field-mappings:
                  - source-field: "field1"
                    target-field: "field1"
            """;

        ProcessingSequenceReport report = analyzer.analyzeYamlString(yaml, "test10");
        String toString = report.toString();
        String formatted = report.getFormattedReport();

        assertEquals(formatted, toString);
    }
}

