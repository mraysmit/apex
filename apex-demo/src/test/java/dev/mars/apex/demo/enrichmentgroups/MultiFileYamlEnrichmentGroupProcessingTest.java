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
 * Multi-file demo showcasing reusability: separate enrichments.yaml and enrichment-groups.yaml.
 *
 * We merge at YAML level (no guessing) using YamlConfigurationLoader's multi-file hooks.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Multi-file Enrichment Group Processing Tests")
public class MultiFileYamlEnrichmentGroupProcessingTest extends DemoTestBase {

    private static final String ENRICHMENTS_PATH = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/MultiFileEnrichments.yaml";
    private static final String GROUPS_PATH = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/MultiFileEnrichmentGroups.yaml";

    private YamlRuleConfiguration loadMergedYaml() {
        try {
            return mergeYamlConfigsForEnrichment(ENRICHMENTS_PATH, GROUPS_PATH);
        } catch (YamlConfigurationException e) {
            fail("Failed to load/merge YAML: " + e.getMessage());
            return null;
        }
    }

    @Test
    @DisplayName("AND: succeeds when all required fields present (multi-file)")
    void testAndPassMultiFile() {
        var config = loadMergedYaml();
        assertNotNull(config);
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gAnd = groups.stream().filter(g -> g.getId().equals("base_and")).findFirst().orElse(null);
        assertNotNull(gAnd);
        Map<String,Object> data = new HashMap<>(); data.put("a","A"); data.put("b","B");
        EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gAnd, data, config);
        assertTrue(r.isSuccess());
        assertEquals(2, r.getEnrichmentResults().size());
        assertEquals("A", data.get("a_copy"));
        assertEquals("B", data.get("b_copy"));

    }

    @Test
    @DisplayName("OR: short-circuits on first success (multi-file)")
    void testOrShortCircuitMultiFile() {
        var config = loadMergedYaml();
        assertNotNull(config);
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gOr = groups.stream().filter(g -> g.getId().equals("base_or")).findFirst().orElse(null);
        assertNotNull(gOr);
        Map<String,Object> data = new HashMap<>(); data.put("a","A");
        EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gOr, data, config);
        assertTrue(r.isSuccess());
        assertEquals(1, r.getEnrichmentResults().size());
        assertEquals("A", data.get("a_copy"));
        assertNull(data.get("b_copy"), "OR short-circuit should not produce b_copy");

    }

    @Test
    @DisplayName("Composite (non-parallel) AND: succeeds when all present (multi-file)")
    void testCompositeNonParallelMultiFile() {
        var config = loadMergedYaml();
        assertNotNull(config);
        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup gComposite = groups.stream().filter(g -> g.getId().equals("composite")).findFirst().orElse(null);
        assertNotNull(gComposite);
        Map<String,Object> data = new HashMap<>(); data.put("a","A"); data.put("b","B"); data.put("c","C");
        EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gComposite, data, config);
        assertTrue(r.isSuccess());
        assertEquals(3, r.getEnrichmentResults().size());
        assertEquals("A", data.get("a_copy"));
        assertEquals("B", data.get("b_copy"));
        assertEquals("C", data.get("c_copy"));

    }

    @Test
    @DisplayName("Validation fails when group references missing enrichment id (multi-file)")
    void testMissingEnrichmentIdValidation() {
        String enrichmentsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/NegativeMissingEnrichmentEnrichments.yaml";
        String groupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/NegativeMissingEnrichmentGroups.yaml";
        assertThrows(YamlConfigurationException.class, () -> {
            mergeYamlConfigsForEnrichment(enrichmentsPath, groupsPath);
        }, "Expected validation to fail due to missing enrichment id reference");
    }


    @Test
    @DisplayName("Composite group references base group across files (multi-file group-to-group)")
    void testCompositeCrossFileGroupReference() {
        String enrichmentsPath = ENRICHMENTS_PATH;
        String baseGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/CrossFileBaseEnrichmentGroups.yaml";
        String compositeGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/CrossFileCompositeEnrichmentGroups.yaml";

        try {
            YamlRuleConfiguration merged = mergeYamlConfigsForEnrichment(enrichmentsPath, baseGroupsPath, compositeGroupsPath);

            List<EnrichmentGroup> runtimeGroups = EnrichmentGroupFactory.buildEnrichmentGroups(merged);
            EnrichmentGroup gComposite = runtimeGroups.stream().filter(g -> g.getId().equals("cf_composite")).findFirst().orElse(null);
            assertNotNull(gComposite, "cf_composite group should exist after cross-file merge");

            Map<String,Object> data = new HashMap<>(); data.put("a","A"); data.put("b","B"); data.put("c","C");
            EnrichmentGroupResult r = enrichmentService.processEnrichmentGroup(gComposite, data, merged);
            assertTrue(r.isSuccess(), "Composite should succeed when e3 and base_and succeed");
            assertEquals(3, r.getEnrichmentResults().size(), "Composite should include e3 + base_and's two enrichments");
            assertEquals("A", data.get("a_copy"));
            assertEquals("B", data.get("b_copy"));
            assertEquals("C", data.get("c_copy"));

        } catch (YamlConfigurationException e) {
            fail("YAML load/validation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Composite Parallel AND references base group across files (multi-file)")
    void testCompositeCrossFileParallelAnd() {
        String enrichmentsPath = ENRICHMENTS_PATH;
        String baseGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/CrossFileBaseEnrichmentGroups.yaml";
        String compositeParGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/CrossFileCompositeParallelEnrichmentGroups.yaml";

        try {
            YamlRuleConfiguration merged = mergeYamlConfigsForEnrichment(enrichmentsPath, baseGroupsPath, compositeParGroupsPath);

            List<EnrichmentGroup> runtimeGroups = EnrichmentGroupFactory.buildEnrichmentGroups(merged);
            EnrichmentGroup gCompositePar = runtimeGroups.stream().filter(g -> g.getId().equals("cf_composite_par_and")).findFirst().orElse(null);
            assertNotNull(gCompositePar, "cf_composite_par_and should exist after cross-file merge");

            // Missing 'c' -> e3 fails; AND overall fails, but all still run (no short-circuit in parallel)
            Map<String,Object> missingC = new HashMap<>(); missingC.put("a","A"); missingC.put("b","B");
            EnrichmentGroupResult rFail = enrichmentService.processEnrichmentGroup(gCompositePar, missingC, merged);
            assertFalse(rFail.isSuccess(), "Parallel AND should fail when any required enrichment fails");
            assertEquals(3, rFail.getEnrichmentResults().size(), "Parallel execution should evaluate all enrichments in composite");

            // All present -> success and all run
            Map<String,Object> all = new HashMap<>(); all.put("a","A"); all.put("b","B"); all.put("c","C");
            EnrichmentGroupResult rOk = enrichmentService.processEnrichmentGroup(gCompositePar, all, merged);
            assertTrue(rOk.isSuccess(), "Parallel AND should succeed when all required enrichments succeed");
            assertEquals(3, rOk.getEnrichmentResults().size(), "Parallel execution should evaluate all enrichments in composite");
            assertEquals("A", all.get("a_copy"));
            assertEquals("B", all.get("b_copy"));
            assertEquals("C", all.get("c_copy"));

        } catch (YamlConfigurationException e) {
            fail("YAML load/validation failed: " + e.getMessage());
        }
    }



    @Test
    @DisplayName("Validation fails for cyclic group-to-group references across files (multi-file)")
    void testCrossFileGroupReferenceCycleValidation() {
        String enrichmentsPath = ENRICHMENTS_PATH;
        String cycleAPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/CrossFileCycleAEnrichmentGroups.yaml";
        String cycleBPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/multifile/CrossFileCycleBEnrichmentGroups.yaml";

        assertThrows(YamlConfigurationException.class, () -> {
            mergeYamlConfigsForEnrichment(enrichmentsPath, cycleAPath, cycleBPath);
        }, "Expected validation to fail due to cyclic enrichment-group-references across files");
    }

}
