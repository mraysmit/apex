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

@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Singular Enrichment Group Reference Test")
public class SingularEnrichmentGroupReferenceTest extends DemoTestBase {

    private static final String CONFIG_PATH = "src/test/java/dev/mars/apex/demo/enrichmentgroups/SingularEnrichmentGroupReferenceTest.yaml";

    @Test
    @DisplayName("RulesEngine processes singular enrichment-group reference")
    void testSingularEnrichmentGroupReference() throws ConfigurationException {
        logger.info("Testing RulesEngine.evaluate() with singular enrichment-group reference");

        YamlRuleConfiguration config;
        try {
            config = yamlLoader.loadFromFile(CONFIG_PATH);
        } catch (ConfigurationException e) {
            logger.error("Failed to load YAML: " + e.getMessage());
            fail("Failed to load YAML: " + e.getMessage());
            return;
        }
        assertNotNull(config, "Configuration should load successfully");

        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> data = new HashMap<>();
        data.put("a", "A");
        data.put("b", "B");

        RuleResult result = engine.evaluate(data);

        assertTrue(result.isSuccess(), "RulesEngine should succeed");

        Map<String, Object> enrichedData = result.getEnrichedData();

        // Verify e1 (from base_group) was applied via the singular reference
        assertEquals("A", enrichedData.get("a_copy"), "Enrichment e1 should have copied field 'a'");
        // Verify e2 (from composite_group) was applied
        assertEquals("B", enrichedData.get("b_copy"), "Enrichment e2 should have copied field 'b'");

        logger.info("Singular enrichment-group reference processed successfully");
    }
}
