package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlEnrichmentGroup;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnrichmentGroupFactoryTest {

    @Test
    void buildsGroupFromEnrichmentIdsInOrder() throws Exception {
        String yaml = """
            metadata:
              name: "EG Factory"
              type: "rule-configuration"
              version: "1.0.0"

            enrichments:
              - id: e1
                name: E1
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: a
                    target-field: a_copy
              - id: e2
                name: E2
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: b
                    target-field: b_copy

            enrichment-groups:
              - id: g1
                name: G1
                operator: AND
                enrichment-ids: [e1, e2]
            """;

        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(yaml);
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        assertEquals(1, groups.size());
        EnrichmentGroup g = groups.get(0);
        assertTrue(g.isAndOperator());
        assertEquals("g1", g.getId());
        assertEquals(List.of("e1", "e2"), g.getEnrichmentsInOrder().stream().map(e -> e.getId()).toList());
    }

    @Test
    void buildsGroupFromEnrichmentReferencesWithExplicitSequence() throws Exception {
        String yaml = """
            metadata:
              name: "EG Factory"
              type: "rule-configuration"
              version: "1.0.0"

            enrichments:
              - id: eA
                name: EA
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: x, target-field: xa } ]
              - id: eB
                name: EB
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: y, target-field: yb } ]

            enrichment-groups:
              - id: g2
                name: G2
                operator: OR
                stop-on-first-failure: true
                parallel-execution: false
                enrichment-references:
                  - enrichment-id: eB
                    sequence: 2
                  - enrichment-id: eA
                    sequence: 1
            """;

        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(yaml);
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        assertEquals(1, groups.size());
        EnrichmentGroup g = groups.get(0);
        assertFalse(g.isAndOperator(), "OR should map to andOperator=false");
        assertTrue(g.isStopOnFirstFailure());
        assertFalse(g.isParallelExecution());
        assertEquals(List.of("eA", "eB"), g.getEnrichmentsInOrder().stream().map(e -> e.getId()).toList());
    }

    @Test
    void mixesReferencesThenIdsAppendingAfterMaxSequence() throws Exception {
        String yaml = """
            metadata:
              name: "EG Factory"
              type: "rule-configuration"
              version: "1.0.0"

            enrichments:
              - id: e1
                name: E1
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: a, target-field: a_copy } ]
              - id: e2
                name: E2
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: b, target-field: b_copy } ]
              - id: e3
                name: E3
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: c, target-field: c_copy } ]

            enrichment-groups:
              - id: g3
                name: G3
                enrichment-references:
                  - enrichment-id: e2
                    sequence: 3
                enrichment-ids: [ e1, e3 ]
            """;

        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(yaml);
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup g = groups.get(0);
        // ids first => e1@1, e3@2; then reference e2@3 overrides/sets explicit position; order: [e1, e3, e2]
        assertEquals(List.of("e1", "e3", "e2"), g.getEnrichmentsInOrder().stream().map(e -> e.getId()).toList());
    }

    @Test
    void buildsGroupWithEnrichmentGroupReferencesAppended() throws Exception {
        String yaml = """
            metadata:
              name: "EG Factory"
              type: "rule-configuration"
              version: "1.0.0"

            enrichments:
              - id: e1
                name: E1
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: a, target-field: a_copy } ]
              - id: e2
                name: E2
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: b, target-field: b_copy } ]
              - id: e3
                name: E3
                type: field-enrichment
                enabled: true
                field-mappings: [ { source-field: c, target-field: c_copy } ]

            enrichment-groups:
              - id: base
                name: Base
                enrichment-ids: [ e1, e2 ]
              - id: composite
                name: Composite
                enrichment-ids: [ e3 ]
                enrichment-group-references: [ base ]
            """;

        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(yaml);
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup base = groups.stream().filter(g -> g.getId().equals("base")).findFirst().orElseThrow();
        EnrichmentGroup composite = groups.stream().filter(g -> g.getId().equals("composite")).findFirst().orElseThrow();

        assertEquals(List.of("e1", "e2"), base.getEnrichmentsInOrder().stream().map(e -> e.getId()).toList());
        assertEquals(List.of("e3", "e1", "e2"), composite.getEnrichmentsInOrder().stream().map(e -> e.getId()).toList());
    }
}

