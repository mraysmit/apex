package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for RulesEngine RuleResult integration.
 * Migrated from deprecated YamlEnrichmentProcessor to RulesEngine API.
 */
class EnrichmentServiceRuleResultTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentServiceRuleResultTest.class);
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ApexCacheManager.resetInstance();
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Should return success RuleResult when no enrichments provided")
    void testEnrichObjectWithResult_NoEnrichments() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Empty Config"
              description: "Config with no enrichments"
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);
        inputData.put("name", "Test");

        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(result.hasFailures());
        assertTrue(result.getFailureMessages().isEmpty());
        assertNotNull(result.getEnrichedData());
    }

    @Test
    @DisplayName("Should return success RuleResult for successful enrichment")
    void testEnrichObjectWithResult_Success() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Success Test"
              description: "Test successful enrichment"

            enrichments:
              - id: test-enrichment
                type: field-enrichment
                field-mappings:
                  - source-field: id
                    target-field: idCopy
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);

        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(result.hasFailures());
        assertTrue(result.getFailureMessages().isEmpty());
        assertNotNull(result.getEnrichedData());
        assertEquals(1, result.getEnrichedData().get("idCopy"));
    }

    /**
     * Intentional error test: Verifies that when a required field mapping fails (source field
     * does not exist in input data), the enrichment properly reports the failure through the
     * RuleResult API with isSuccess() returning false and failure messages populated.
     */
    @Test
    @DisplayName("Should return failure RuleResult for required field mapping failure")
    void testEnrichObjectWithResult_RequiredFieldFailureIntentionalError() throws Exception {
        LOGGER.info("=== INTENTIONAL ERROR TEST: Required field mapping failure ===");
        String yamlConfig = """
            metadata:
              name: "Required Field Test"
              description: "Test required field failure"

            enrichments:
              - id: test-enrichment
                type: field-enrichment
                field-mappings:
                  - source-field: missingField
                    target-field: targetField
                    required: true
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("id", 1);

        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.hasFailures());
        assertFalse(result.getFailureMessages().isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple enrichments correctly")
    void testEnrichObjectWithResult_MultipleEnrichments() throws Exception {
        String yamlConfig = """
            metadata:
              name: "Multiple Enrichments Test"
              description: "Test multiple enrichments"

            enrichments:
              - id: enrichment1
                type: field-enrichment
                field-mappings:
                  - source-field: field1
                    target-field: target1
              - id: enrichment2
                type: field-enrichment
                field-mappings:
                  - source-field: field2
                    target-field: target2
            """;

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("field1", "value1");
        inputData.put("field2", "value2");

        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("value1", result.getEnrichedData().get("target1"));
        assertEquals("value2", result.getEnrichedData().get("target2"));
    }
}
