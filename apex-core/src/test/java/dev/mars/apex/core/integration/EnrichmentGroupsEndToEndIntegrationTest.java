package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.EnrichmentGroupResult;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for Enrichment Groups:
 * - AND/OR semantics with short-circuiting
 * - Parallel execution
 * - Group references (second-pass flattening)
 */
class EnrichmentGroupsEndToEndIntegrationTest {

    private EnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        enrichmentService = new EnrichmentService(new LookupServiceRegistry(), new ExpressionEvaluatorService());
    }

    private String yamlConfig() {
        return """
            metadata:
              name: "EG EndToEnd"
              type: "rule-configuration"
              version: "1.0.0"

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
        EnrichmentGroup gAnd = groups.stream().filter(g -> g.getId().equals("base_and")).findFirst().orElseThrow();
        EnrichmentGroup gOr = groups.stream().filter(g -> g.getId().equals("base_or")).findFirst().orElseThrow();
        EnrichmentGroup gParOr = groups.stream().filter(g -> g.getId().equals("par_or")).findFirst().orElseThrow();
        EnrichmentGroup gComposite = groups.stream().filter(g -> g.getId().equals("composite")).findFirst().orElseThrow();

        // OR with only 'a' present: succeeds on e1, short-circuits before e2
        Map<String, Object> mOr = new HashMap<>();
        mOr.put("a", "A");
        EnrichmentGroupResult rOr = enrichmentService.processEnrichmentGroup(gOr, mOr, config);
        assertTrue(rOr.isSuccess(), "OR should succeed when any enrichment succeeds");
        assertEquals(1, rOr.getEnrichmentResults().size(), "OR+short-circuit should evaluate only first success");
        assertEquals("A", rOr.getEnrichmentResults().get(0).getEnrichedData().get("a"));
        assertEquals("A", rOr.getEnrichmentResults().get(0).getEnrichedData().get("a"));

        // AND with only 'a': e1 succeeds, e2 fails; should stop on first failure (second enrichment)
        Map<String, Object> mAnd = new HashMap<>();
        mAnd.put("a", "A");
        EnrichmentGroupResult rAnd = enrichmentService.processEnrichmentGroup(gAnd, mAnd, config);
        assertFalse(rAnd.isSuccess(), "AND should fail when a required enrichment fails");
        assertEquals(2, rAnd.getEnrichmentResults().size(), "AND+stop-on-first-failure should stop at failing enrichment");
        assertEquals("ERROR", rAnd.getAggregatedSeverity());

        // Parallel OR: runs all regardless of short-circuit, still succeeds if any succeed
        Map<String, Object> mParOr = new HashMap<>();
        mParOr.put("a", "A");
        EnrichmentGroupResult rParOr = enrichmentService.processEnrichmentGroup(gParOr, mParOr, config);
        assertTrue(rParOr.isSuccess(), "Parallel OR should succeed when any enrichment succeeds");
        assertEquals(2, rParOr.getEnrichmentResults().size(), "Parallel execution should evaluate all enrichments");

        // Composite (e3 + base_and) with all fields → all three succeed
        Map<String, Object> mComposite = new HashMap<>();
        mComposite.put("a", "A");
        mComposite.put("b", "B");
        mComposite.put("c", "C");
        EnrichmentGroupResult rComposite = enrichmentService.processEnrichmentGroup(gComposite, mComposite, config);
        assertTrue(rComposite.isSuccess(), "Composite group should succeed when referenced base AND also succeeds");
        assertEquals(3, rComposite.getEnrichmentResults().size(), "Composite should include e3 plus base_and's two enrichments");
        // Verify that enriched data across evaluations contains expected copies (order-independent check)
        boolean hasACopy = rComposite.getEnrichmentResults().stream().anyMatch(rr -> "A".equals(rr.getEnrichedData().get("a")) || "A".equals(rr.getEnrichedData().get("a_copy")));
        boolean hasBCopy = rComposite.getEnrichmentResults().stream().anyMatch(rr -> "B".equals(rr.getEnrichedData().get("b")) || "B".equals(rr.getEnrichedData().get("b_copy")));
        boolean hasCCopy = rComposite.getEnrichmentResults().stream().anyMatch(rr -> "C".equals(rr.getEnrichedData().get("c")) || "C".equals(rr.getEnrichedData().get("c_copy")));
        assertTrue(hasACopy && hasBCopy && hasCCopy, "Composite enrichment results should reflect a,b,c copies in data");
        assertEquals("INFO", rComposite.getAggregatedSeverity());
    }

    @Test
    @DisplayName("Composite Parallel AND runs all enrichments and aggregates correctly")
    void compositeParallelAnd_runsAll_andAggregates() throws Exception {
        YamlRuleConfiguration config = new YamlConfigurationLoader().fromYamlString(yamlConfig());
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gCompositeParAnd = groups.stream().filter(g -> g.getId().equals("composite_par_and")).findFirst().orElseThrow();

        // Missing 'c' -> e3 fails, AND overall fails, but all run due to parallel-execution
        Map<String, Object> mMissingC = new HashMap<>();
        mMissingC.put("a", "A");
        mMissingC.put("b", "B");
        EnrichmentGroupResult rMissingC = enrichmentService.processEnrichmentGroup(gCompositeParAnd, mMissingC, config);
        assertFalse(rMissingC.isSuccess(), "Composite Parallel AND should fail when any required enrichment fails");
        assertEquals(3, rMissingC.getEnrichmentResults().size(), "Parallel execution should evaluate all enrichments in composite");

        // All present -> success and all run
        Map<String, Object> mAll = new HashMap<>();
        mAll.put("a", "A");
        mAll.put("b", "B");
        mAll.put("c", "C");
        EnrichmentGroupResult rAll = enrichmentService.processEnrichmentGroup(gCompositeParAnd, mAll, config);
        assertTrue(rAll.isSuccess(), "Composite Parallel AND should succeed when all required enrichments succeed");
        assertEquals(3, rAll.getEnrichmentResults().size(), "Parallel execution should evaluate all enrichments in composite");
    }
}

