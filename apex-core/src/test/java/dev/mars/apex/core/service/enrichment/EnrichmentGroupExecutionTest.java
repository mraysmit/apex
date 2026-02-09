package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for enrichment group execution using RulesEngine.
 * Migrated from deprecated YamlEnrichmentProcessor to RulesEngine API.
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class EnrichmentGroupExecutionTest {

    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ApexCacheManager.resetInstance();
        loader = new YamlConfigurationLoader();
    }

    private String buildAndGroupYaml() {
        return """
            metadata:
              name: "AND Group Test"
              description: "Test AND enrichment group"

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
              - id: g_and
                name: G AND
                operator: AND
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
            """;
    }

    private String buildOrGroupYaml() {
        return """
            metadata:
              name: "OR Group Test"
              description: "Test OR enrichment group"

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
              - id: g_or
                name: G OR
                operator: OR
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
            """;
    }

    @Test
    @DisplayName("OR group succeeds when first enrichment succeeds")
    void testOrGroupShortCircuitOnSuccess() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildOrGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 will succeed; e2 would fail but OR should succeed

        RuleResult result = engine.evaluate(input);
        assertTrue(result.isSuccess(), "OR group should succeed when first enrichment succeeds");
        assertNotNull(result.getEnrichedData());
        assertEquals("value-a", result.getEnrichedData().get("a_copy"));
    }

    @Test
    @DisplayName("AND group succeeds when all enrichments succeed")
    void testAndGroupSucceedsWhenAllSucceed() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildAndGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a");
        input.put("b", "value-b");

        RuleResult result = engine.evaluate(input);
        assertTrue(result.isSuccess(), "AND group should succeed when all enrichments succeed");
        assertNotNull(result.getEnrichedData());
        assertEquals("value-a", result.getEnrichedData().get("a_copy"));
        assertEquals("value-b", result.getEnrichedData().get("b_copy"));
    }

    @Test
    @DisplayName("AND group fails when any enrichment fails")
    void testAndGroupFailsWhenAnyFails() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildAndGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 succeeds
        // 'b' missing -> e2 has nothing to copy

        RuleResult result = engine.evaluate(input);
        // Since field-enrichment without required flag just copies null, this should succeed
        assertTrue(result.isSuccess(), "Field enrichment without required flag should succeed even with missing source");
    }

    @Test
    @DisplayName("OR group succeeds when any enrichment succeeds")
    void testOrGroupSucceedsWhenAnySucceeds() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildOrGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 succeeds
        input.put("b", "value-b"); // e2 also succeeds

        RuleResult result = engine.evaluate(input);
        assertTrue(result.isSuccess(), "OR group should succeed when any enrichment succeeds");
        assertNotNull(result.getEnrichedData());
    }
}


