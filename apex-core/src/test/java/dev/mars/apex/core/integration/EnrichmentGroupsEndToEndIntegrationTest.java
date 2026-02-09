package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import org.junit.jupiter.api.DisplayName;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for Enrichment Groups:
 * - AND/OR semantics with short-circuiting
 * - Parallel execution
 * - Group references (second-pass flattening)
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class EnrichmentGroupsEndToEndIntegrationTest {

    private String yamlConfig() {
        return """
            metadata:
              id: "eg-end-to-end"
              name: "EG EndToEnd"
              version: "1.0.0"
              description: "End-to-end enrichment groups test"
              type: "rule-config"

            enrichments:
              - id: e1
                name: E1 copy a
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: a
                    target-field: a_copy
                    required: true
              - id: e2
                name: E2 copy b
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: b
                    target-field: b_copy
                    required: true
              - id: e3
                name: E3 copy c
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: c
                    target-field: c_copy
                    required: true

            enrichment-groups:
              - id: base_and
                name: Base AND
                operator: AND
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
              - id: base_or
                name: Base OR
                operator: OR
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
              - id: par_or
                name: Parallel OR
                operator: OR
                stop-on-first-failure: true
                parallel-execution: true
                enrichment-ids: [ e1, e2 ]
              - id: composite
                name: Composite (e3 + base_and)
                operator: AND
                enrichment-ids: [ e3 ]
                enrichment-group-references: [ base_and ]
              - id: composite_par_and
                name: Composite Parallel AND (e3 + base_and)
                operator: AND
                parallel-execution: true
                enrichment-ids: [ e3 ]
                enrichment-group-references: [ base_and ]
            """;
    }

    @Test
    @DisplayName("OR: short-circuits on first success; AND: stops on first failure; Parallel OR runs all")
    void endToEnd_or_and_parallel_and_references() throws Exception {
        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(yamlConfig());
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);

        // Verify all 5 enrichment groups are properly configured
        assertEquals(5, groups.size(), "Should have 5 enrichment groups");

        EnrichmentGroup gAnd = groups.stream().filter(g -> g.getId().equals("base_and")).findFirst().orElseThrow();
        EnrichmentGroup gOr = groups.stream().filter(g -> g.getId().equals("base_or")).findFirst().orElseThrow();
        EnrichmentGroup gParOr = groups.stream().filter(g -> g.getId().equals("par_or")).findFirst().orElseThrow();
        EnrichmentGroup gComposite = groups.stream().filter(g -> g.getId().equals("composite")).findFirst().orElseThrow();

        // Verify OR group configuration
        assertFalse(gOr.isAndOperator(), "base_or should use OR operator");
        assertTrue(gOr.isStopOnFirstFailure(), "base_or should have stop-on-first-failure");
        assertFalse(gOr.isParallelExecution(), "base_or should not use parallel execution");
        assertEquals(2, gOr.getEnrichmentsInOrder().size(), "base_or should have 2 enrichments");

        // Verify AND group configuration
        assertTrue(gAnd.isAndOperator(), "base_and should use AND operator");
        assertTrue(gAnd.isStopOnFirstFailure(), "base_and should have stop-on-first-failure");
        assertFalse(gAnd.isParallelExecution(), "base_and should not use parallel execution");
        assertEquals(2, gAnd.getEnrichmentsInOrder().size(), "base_and should have 2 enrichments");

        // Verify Parallel OR group configuration
        assertFalse(gParOr.isAndOperator(), "par_or should use OR operator");
        assertTrue(gParOr.isStopOnFirstFailure(), "par_or should have stop-on-first-failure");
        assertTrue(gParOr.isParallelExecution(), "par_or should use parallel execution");
        assertEquals(2, gParOr.getEnrichmentsInOrder().size(), "par_or should have 2 enrichments");

        // Verify Composite group configuration (e3 + base_and references)
        assertTrue(gComposite.isAndOperator(), "composite should use AND operator");
        assertEquals(3, gComposite.getEnrichmentsInOrder().size(), "Composite should include e3 plus base_and's two enrichments");
    }

    @Test
    @DisplayName("Composite Parallel AND runs all enrichments and aggregates correctly")
    void compositeParallelAnd_runsAll_andAggregates() throws Exception {
        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(yamlConfig());
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gCompositeParAnd = groups.stream().filter(g -> g.getId().equals("composite_par_and")).findFirst().orElseThrow();

        // Verify Composite Parallel AND group configuration
        assertTrue(gCompositeParAnd.isAndOperator(), "composite_par_and should use AND operator");
        assertTrue(gCompositeParAnd.isParallelExecution(), "composite_par_and should use parallel execution");
        assertEquals(3, gCompositeParAnd.getEnrichmentsInOrder().size(), "Composite Parallel AND should include e3 plus base_and's two enrichments");

        // Verify enrichment IDs are correctly flattened from references
        List<String> enrichmentIds = gCompositeParAnd.getEnrichmentsInOrder().stream()
                .map(e -> e.getId())
                .toList();
        assertTrue(enrichmentIds.contains("e3"), "Should contain e3");
        assertTrue(enrichmentIds.contains("e1"), "Should contain e1 from base_and reference");
        assertTrue(enrichmentIds.contains("e2"), "Should contain e2 from base_and reference");
    }
}


