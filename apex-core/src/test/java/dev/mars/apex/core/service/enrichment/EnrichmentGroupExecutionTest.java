package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.EnrichmentGroupResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichmentGroupExecutionTest {

    private EnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        enrichmentService = new EnrichmentService(new LookupServiceRegistry(), new ExpressionEvaluatorService());
    }

    private String buildYaml() {
        return """
            metadata:
              name: "EG Exec"
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
                    required: true
              - id: e2
                name: E2
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: b
                    target-field: b_copy
                    required: true

            enrichment-groups:
              - id: g_or
                name: G OR
                operator: OR
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
              - id: g_and
                name: G AND
                operator: AND
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
              - id: g_or_parallel
                name: G OR PAR
                operator: OR
                stop-on-first-failure: true
                parallel-execution: true
                enrichment-ids: [ e1, e2 ]
              - id: g_and_parallel
                name: G AND PAR
                operator: AND
                stop-on-first-failure: true
                parallel-execution: true
                enrichment-ids: [ e1, e2 ]
            """;
    }

    @Test
    @DisplayName("OR group short-circuits on first success")
    void testOrGroupShortCircuitOnSuccess() throws Exception {
        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(buildYaml());
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gOr = groups.stream().filter(g -> g.getId().equals("g_or")).findFirst().orElseThrow();

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 will succeed; e2 would fail but should not run due to OR+stop-on-first

        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(gOr, input, config);
        assertTrue(result.isSuccess(), "OR group should succeed when first enrichment succeeds");
        assertEquals(1, result.getEnrichmentResults().size(), "Should short-circuit after first success");
    }

    @Test
    @DisplayName("AND group stops on first failure")
    void testAndGroupStopsOnFirstFailure() throws Exception {
        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(buildYaml());
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gAnd = groups.stream().filter(g -> g.getId().equals("g_and")).findFirst().orElseThrow();

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 succeeds
        // 'b' missing -> e2 fails; AND + stop-on-first should stop at failure

        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(gAnd, input, config);
        assertFalse(result.isSuccess(), "AND group should fail when a required enrichment fails");
        assertEquals(2, result.getEnrichmentResults().size(), "Should stop at first failure (second enrichment)");
    }

    @Test
    @DisplayName("OR group with parallel-execution runs all and succeeds if any succeed")
    void testOrGroupParallelRunsAll() throws Exception {
        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(buildYaml());
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gOrPar = groups.stream().filter(g -> g.getId().equals("g_or_parallel")).findFirst().orElseThrow();

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 succeeds; e2 fails

        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(gOrPar, input, config);
        assertTrue(result.isSuccess(), "OR parallel group should succeed when any enrichment succeeds");
        assertEquals(2, result.getEnrichmentResults().size(), "Parallel execution should run all enrichments");
    }

    @Test
    @DisplayName("AND group with parallel-execution runs all and fails if any fail")
    void testAndGroupParallelRunsAll() throws Exception {
        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(buildYaml());
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gAndPar = groups.stream().filter(g -> g.getId().equals("g_and_parallel")).findFirst().orElseThrow();

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 succeeds; 'b' missing -> e2 fails

        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(gAndPar, input, config);
        assertFalse(result.isSuccess(), "AND parallel group should fail when any enrichment fails");
        assertEquals(2, result.getEnrichmentResults().size(), "Parallel execution should run all enrichments");
    }
}

