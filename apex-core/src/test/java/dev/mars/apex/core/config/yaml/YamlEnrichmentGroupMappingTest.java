package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlEnrichmentGroupMappingTest {

    @Test
    @DisplayName("YamlEnrichmentGroup maps from YAML (embedded string) with core fields and references")
    void testYamlMapping() throws Exception {
        String yaml = """
            id: eg-cust
            name: Customer Enrichments
            description: Demo enrichment group
            priority: 10
            enabled: true
            operator: OR
            stop-on-first-failure: true
            parallel-execution: false
            debug-mode: false
            enrichment-ids: [E1, E2]
            enrichment-references:
              - enrichment-id: E3
                sequence: 1
                enabled: true
                override-priority: 5
            enrichment-group-references: [eg-base]
            depends-on: [eg-prereq]
            tags: [customer, demo]
            metadata:
              owner: team-a
            """;

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        YamlEnrichmentGroup group = mapper.readValue(yaml, YamlEnrichmentGroup.class);

        assertEquals("eg-cust", group.getId());
        assertEquals("Customer Enrichments", group.getName());
        assertEquals(10, group.getPriority());
        assertEquals(Boolean.TRUE, group.getEnabled());
        assertEquals("OR", group.getOperator());
        assertEquals(Boolean.TRUE, group.getStopOnFirstFailure());
        assertEquals(Boolean.FALSE, group.getParallelExecution());
        assertEquals(Boolean.FALSE, group.getDebugMode());

        assertNotNull(group.getEnrichmentIds());
        assertEquals(2, group.getEnrichmentIds().size());
        assertTrue(group.getEnrichmentIds().containsAll(List.of("E1", "E2")));

        assertNotNull(group.getEnrichmentReferences());
        assertEquals(1, group.getEnrichmentReferences().size());
        YamlEnrichmentGroup.EnrichmentReference ref = group.getEnrichmentReferences().get(0);
        assertEquals("E3", ref.getEnrichmentId());
        assertEquals(1, ref.getSequence());
        assertEquals(Boolean.TRUE, ref.getEnabled());
        assertEquals(5, ref.getOverridePriority());

        assertEquals(List.of("eg-base"), group.getEnrichmentGroupReferences());
        assertEquals(List.of("eg-prereq"), group.getDependsOn());

        Map<String, Object> metadata = group.getMetadata();
        assertNotNull(metadata);
        assertEquals("team-a", metadata.get("owner"));
    }
}

