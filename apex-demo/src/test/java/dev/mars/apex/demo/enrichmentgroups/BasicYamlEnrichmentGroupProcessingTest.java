package dev.mars.apex.demo.enrichmentgroups;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.EnrichmentGroupResult;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Basic YAML Enrichment Group Processing Tests.
 *
 * Mirrors the rule group examples using a combined config and validates:
 * - AND vs OR semantics (with short-circuit)
 * - Composite group via enrichment-group-references
 * - Parallel AND in a composite group (no short-circuit, runs all)
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Basic YAML Enrichment Group Processing Tests")
public class BasicYamlEnrichmentGroupProcessingTest extends DemoTestBase {

    private static final String CONFIG_PATH = "src/test/java/dev/mars/apex/demo/enrichmentgroups/BasicYamlEnrichmentGroupProcessingTest-combined-config.yaml";

    @Test
    @DisplayName("Composite Parallel AND: runs all and aggregates correctly")
    void testCompositeParallelAnd() {
        logger.info("Loading enrichment groups combined config for composite parallel AND test");

        YamlRuleConfiguration config;
        try {
            config = mergeYamlConfigsForEnrichment(CONFIG_PATH);
        } catch (YamlConfigurationException e) {
            logger.error("Failed to load YAML: " + e.getMessage());
            fail("Failed to load YAML: " + e.getMessage());
            return;
        }
        assertNotNull(config, "Configuration should load successfully");

        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup compositeParAnd = groups.stream().filter(g -> g.getId().equals("composite_par_and")).findFirst().orElse(null);
        assertNotNull(compositeParAnd, "Composite parallel AND group should exist");

        // Case 1: Missing 'c' -> e3 fails; all run due to parallel; overall AND fails
        Map<String, Object> dataMissingC = new HashMap<>();
        dataMissingC.put("a", "A");
        dataMissingC.put("b", "B");
        EnrichmentGroupResult rMissingC = enrichmentService.processEnrichmentGroup(compositeParAnd, dataMissingC, config);
        assertFalse(rMissingC.isSuccess(), "Composite Parallel AND should fail when a required enrichment fails");
        assertEquals(3, rMissingC.getEnrichmentResults().size(), "Parallel execution should evaluate all enrichments");

        // Case 2: All present -> success and all run
        Map<String, Object> dataAll = new HashMap<>();
        dataAll.put("a", "A");
        dataAll.put("b", "B");
        dataAll.put("c", "C");
        EnrichmentGroupResult rAll = enrichmentService.processEnrichmentGroup(compositeParAnd, dataAll, config);
        assertTrue(rAll.isSuccess(), "Composite Parallel AND should succeed when all required enrichments succeed");
        assertEquals(3, rAll.getEnrichmentResults().size(), "Parallel execution should evaluate all enrichments");
        assertEquals("A", dataAll.get("a_copy"));
        assertEquals("B", dataAll.get("b_copy"));
        assertEquals("C", dataAll.get("c_copy"));

    }

    @Test
    @DisplayName("Basic AND group: succeeds when all required fields present")
    void testBasicAndPass() {
        YamlRuleConfiguration config;
        try { config = mergeYamlConfigsForEnrichment(CONFIG_PATH); }
        catch (YamlConfigurationException e) { fail("YAML load failed: " + e.getMessage()); return; }
        var groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        var gAnd = groups.stream().filter(g -> g.getId().equals("base_and")).findFirst().orElse(null);
        assertNotNull(gAnd, "base_and group should exist");
        Map<String,Object> data = new HashMap<>(); data.put("a","A"); data.put("b","B");
        EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gAnd, data, config);
        assertTrue(r.isSuccess(), "AND should succeed when all enrichments succeed");
        assertEquals(2, r.getEnrichmentResults().size(), "Should evaluate both enrichments");
        assertEquals("A", data.get("a_copy"));
        assertEquals("B", data.get("b_copy"));

    }

    @Test
    @DisplayName("OR group short-circuits on first success")
    void testOrShortCircuit() {
        YamlRuleConfiguration config;
        try { config = mergeYamlConfigsForEnrichment(CONFIG_PATH); }
        catch (YamlConfigurationException e) { fail("YAML load failed: " + e.getMessage()); return; }
        var groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        var gOr = groups.stream().filter(g -> g.getId().equals("base_or")).findFirst().orElse(null);
        assertNotNull(gOr, "base_or group should exist");
        Map<String,Object> data = new HashMap<>(); data.put("a","A");
        EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gOr, data, config);
        assertTrue(r.isSuccess(), "OR should succeed when any enrichment succeeds");
        assertEquals(1, r.getEnrichmentResults().size(), "OR+short-circuit should evaluate only first success");
        assertEquals("A", data.get("a_copy"));
        assertNull(data.get("b_copy"), "OR short-circuit should not produce b_copy");

    }

    @Test
    @DisplayName("AND stop-on-first-failure: stops at failing enrichment")
    void testAndStopOnFirstFailure() {
        YamlRuleConfiguration config;
        try { config = mergeYamlConfigsForEnrichment(CONFIG_PATH); }
        catch (YamlConfigurationException e) { fail("YAML load failed: " + e.getMessage()); return; }
        var groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        var gAnd = groups.stream().filter(g -> g.getId().equals("base_and")).findFirst().orElse(null);
        assertNotNull(gAnd, "base_and group should exist");
        Map<String,Object> data = new HashMap<>(); data.put("a","A");
        EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gAnd, data, config);
        assertFalse(r.isSuccess(), "AND should fail when a required enrichment fails");
        assertEquals(2, r.getEnrichmentResults().size(), "AND+stop-on-first-failure should stop at failing enrichment");
    }

    @Test
    @DisplayName("Composite (non-parallel) AND: succeeds when all present")
    void testCompositeNonParallel() {
        YamlRuleConfiguration config;
        try { config = mergeYamlConfigsForEnrichment(CONFIG_PATH); }
        catch (YamlConfigurationException e) { fail("YAML load failed: " + e.getMessage()); return; }
        var groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        var gComposite = groups.stream().filter(g -> g.getId().equals("composite")).findFirst().orElse(null);
        assertNotNull(gComposite, "composite group should exist");
        Map<String,Object> data = new HashMap<>(); data.put("a","A"); data.put("b","B"); data.put("c","C");
        EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gComposite, data, config);
        assertTrue(r.isSuccess(), "Composite group should succeed when referenced base AND also succeeds");
        assertEquals(3, r.getEnrichmentResults().size(), "Composite should include e3 plus base_and's two enrichments");
        assertEquals("A", data.get("a_copy"));
        assertEquals("B", data.get("b_copy"));
        assertEquals("C", data.get("c_copy"));

    }

}

