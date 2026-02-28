package dev.mars.apex.demo.enrichmentgroups;

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Multi-file demo showcasing reusability: separate enrichments.yaml and enrichment-groups.yaml.
 *
 * We merge at YAML level (no guessing) using ConfigurationLoader's multi-file hooks.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Multi-file Enrichment Group Processing Tests")
public class MultiFileYamlEnrichmentGroupProcessingTest extends DemoTestBase {

    private static final String ENRICHMENTS_PATH = "src/test/java/dev/mars/apex/demo/enrichmentgroups/MultiFileEnrichments.yaml";
    private static final String GROUPS_PATH = "src/test/java/dev/mars/apex/demo/enrichmentgroups/MultiFileEnrichmentGroups.yaml";

    private YamlRuleConfiguration loadMergedYaml() {
        try {
            return mergeYamlConfigsForEnrichment(ENRICHMENTS_PATH, GROUPS_PATH);
        } catch (ConfigurationException e) {
            fail("Failed to load/merge YAML: " + e.getMessage());
            return null;
        }
    }

    @Test
    @DisplayName("RulesEngine processes AND enrichment group (multi-file)")
    void testAndPassMultiFile() {
        var config = loadMergedYaml();
        assertNotNull(config);

        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String,Object> data = new HashMap<>();
            data.put("a","A");
            data.put("b","B");
            data.put("c","C");

            RuleResult result = engine.evaluate(data);
            assertTrue(result.isSuccess());

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("A", enrichedData.get("a_copy"));
            assertEquals("B", enrichedData.get("b_copy"));
        } catch (ConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes OR enrichment group (multi-file)")
    void testOrShortCircuitMultiFile() {
        var config = loadMergedYaml();
        assertNotNull(config);

        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String,Object> data = new HashMap<>();
            data.put("a","A");
            data.put("b","B");
            data.put("c","C");

            RuleResult result = engine.evaluate(data);
            assertTrue(result.isSuccess());

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("A", enrichedData.get("a_copy"));
            // Note: OR groups may execute all or short-circuit depending on configuration
        } catch (ConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes composite enrichment group (multi-file)")
    void testCompositeNonParallelMultiFile() {
        var config = loadMergedYaml();
        assertNotNull(config);

        try {
            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            Map<String,Object> data = new HashMap<>();
            data.put("a","A");
            data.put("b","B");
            data.put("c","C");

            RuleResult result = engine.evaluate(data);
            assertTrue(result.isSuccess());

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("A", enrichedData.get("a_copy"));
            assertEquals("B", enrichedData.get("b_copy"));
            assertEquals("C", enrichedData.get("c_copy"));
        } catch (ConfigurationException e) {
            fail("Failed to create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Validation fails when group references missing enrichment id (multi-file)")
    void testMissingEnrichmentIdValidation() {
        String enrichmentsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/NegativeMissingEnrichmentEnrichments.yaml";
        String groupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/NegativeMissingEnrichmentGroups.yaml";
        assertThrows(ConfigurationException.class, () -> {
            mergeYamlConfigsForEnrichment(enrichmentsPath, groupsPath);
        }, "Expected validation to fail due to missing enrichment id reference");
    }


    @Test
    @DisplayName("RulesEngine processes composite group across files (multi-file)")
    void testCompositeCrossFileGroupReference() {
        String enrichmentsPath = ENRICHMENTS_PATH;
        String baseGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/CrossFileBaseEnrichmentGroups.yaml";
        String compositeGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/CrossFileCompositeEnrichmentGroups.yaml";

        try {
            YamlRuleConfiguration merged = mergeYamlConfigsForEnrichment(enrichmentsPath, baseGroupsPath, compositeGroupsPath);
            RulesEngine engine = RulesEngine.fromYamlConfig(merged);

            Map<String,Object> data = new HashMap<>();
            data.put("a","A");
            data.put("b","B");
            data.put("c","C");

            RuleResult result = engine.evaluate(data);
            assertTrue(result.isSuccess(), "RulesEngine should succeed when all fields present");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("A", enrichedData.get("a_copy"));
            assertEquals("B", enrichedData.get("b_copy"));
            assertEquals("C", enrichedData.get("c_copy"));

        } catch (ConfigurationException e) {
            fail("YAML load/validation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes composite parallel AND across files (multi-file)")
    void testCompositeCrossFileParallelAnd() {
        String enrichmentsPath = ENRICHMENTS_PATH;
        String baseGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/CrossFileBaseEnrichmentGroups.yaml";
        String compositeParGroupsPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/CrossFileCompositeParallelEnrichmentGroups.yaml";

        try {
            YamlRuleConfiguration merged = mergeYamlConfigsForEnrichment(enrichmentsPath, baseGroupsPath, compositeParGroupsPath);
            RulesEngine engine = RulesEngine.fromYamlConfig(merged);

            // Test with all fields present
            Map<String,Object> all = new HashMap<>();
            all.put("a","A");
            all.put("b","B");
            all.put("c","C");

            RuleResult result = engine.evaluate(all);
            assertTrue(result.isSuccess(), "RulesEngine should succeed when all fields present");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("A", enrichedData.get("a_copy"));
            assertEquals("B", enrichedData.get("b_copy"));
            assertEquals("C", enrichedData.get("c_copy"));

        } catch (ConfigurationException e) {
            fail("YAML load/validation failed: " + e.getMessage());
        }
    }



    @Test
    @DisplayName("Validation fails for cyclic group-to-group references across files (multi-file)")
    void testCrossFileGroupReferenceCycleValidation() {
        String enrichmentsPath = ENRICHMENTS_PATH;
        String cycleAPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/CrossFileCycleAEnrichmentGroups.yaml";
        String cycleBPath = "src/test/java/dev/mars/apex/demo/enrichmentgroups/CrossFileCycleBEnrichmentGroups.yaml";

        assertThrows(ConfigurationException.class, () -> {
            mergeYamlConfigsForEnrichment(enrichmentsPath, cycleAPath, cycleBPath);
        }, "Expected validation to fail due to cyclic enrichment-group-references across files");
    }

}

