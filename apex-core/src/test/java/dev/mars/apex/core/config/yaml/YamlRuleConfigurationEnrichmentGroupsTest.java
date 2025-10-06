package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YamlRuleConfigurationEnrichmentGroupsTest {

    @Test
    void parsesEnrichmentGroupsInRuleConfig() throws Exception {
        String yaml = """
            metadata:
              name: "Enrichment Groups Mapping"
              type: "rule-configuration"
              version: "1.0.0"

            enrichments:
              - id: "e1"
                name: "Copy name"
                type: "field-enrichment"
                enabled: true
                field-mappings:
                  - source-field: "name"
                    target-field: "name_copy"

            enrichment-groups:
              - id: "eg1"
                name: "Group 1"
                operator: "AND"
                enrichment-ids:
                  - "e1"
            """;

        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yaml);

        assertNotNull(config.getEnrichmentGroups(), "enrichment-groups should be deserialized");
        assertEquals(1, config.getEnrichmentGroups().size(), "one enrichment group expected");
        YamlEnrichmentGroup group = config.getEnrichmentGroups().get(0);
        assertEquals("eg1", group.getId());
        assertEquals("Group 1", group.getName());

        assertNotNull(config.getEnrichments(), "enrichments should be deserialized");
        assertEquals(1, config.getEnrichments().size());
        assertEquals("e1", config.getEnrichments().get(0).getId());
    }
}

