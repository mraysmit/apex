package dev.mars.apex.core.config.yaml;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.EnrichmentGroup;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Enrichment Group error-handling configuration.
 * 
 * Tests that the error-handling field is properly parsed from YAML and applied to EnrichmentGroup instances.
 *
 * @author Mark A Ray-Smith
 * @since 1.0
 * @version 1.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class EnrichmentGroupErrorHandlingConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentGroupErrorHandlingConfigTest.class);
    private ConfigurationLoader yamlLoader;

    @BeforeEach
    void setUp() {
        yamlLoader = new ConfigurationLoader();
    }

    @Test
    @DisplayName("Test 1: Enrichment group with fail-fast error handling")
    void testEnrichmentGroupWithFailFastErrorHandling() throws Exception {
        logger.info("=== Test 1: Testing enrichment group with fail-fast error handling ===");

        String yaml = """
            metadata:
              name: "Error Handling Test"
              type: "test-config"

            enrichments:
              - id: "enrich1"
                name: "Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "data.value"
                    target-field: "enriched.value"

            enrichment-groups:
              - id: "fail-fast-group"
                name: "Fail Fast Group"
                operator: "AND"
                error-handling: "fail-fast"
                enrichment-ids:
                  - "enrich1"
            """;

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        EnrichmentGroup group = engine.getConfiguration().getEnrichmentGroupById("fail-fast-group");
        assertNotNull(group, "Enrichment group should be created");
        assertEquals("fail-fast", group.getErrorHandling(), "Error handling should be fail-fast");
    }

    @Test
    @DisplayName("Test 2: Enrichment group with continue-on-error error handling")
    void testEnrichmentGroupWithContinueOnErrorHandling() throws Exception {
        logger.info("=== Test 2: Testing enrichment group with continue-on-error error handling ===");

        String yaml = """
            metadata:
              name: "Error Handling Test"
              type: "test-config"

            enrichments:
              - id: "enrich1"
                name: "Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "data.value"
                    target-field: "enriched.value"

            enrichment-groups:
              - id: "continue-group"
                name: "Continue On Error Group"
                operator: "OR"
                error-handling: "continue-on-error"
                enrichment-ids:
                  - "enrich1"
            """;

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        EnrichmentGroup group = engine.getConfiguration().getEnrichmentGroupById("continue-group");
        assertNotNull(group, "Enrichment group should be created");
        assertEquals("continue-on-error", group.getErrorHandling(), "Error handling should be continue-on-error");
    }

    @Test
    @DisplayName("Test 3: Enrichment group with skip-on-error error handling")
    void testEnrichmentGroupWithSkipOnErrorHandling() throws Exception {
        logger.info("=== Test 3: Testing enrichment group with skip-on-error error handling ===");

        String yaml = """
            metadata:
              name: "Error Handling Test"
              type: "test-config"

            enrichments:
              - id: "enrich1"
                name: "Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "data.value"
                    target-field: "enriched.value"

            enrichment-groups:
              - id: "skip-group"
                name: "Skip On Error Group"
                operator: "AND"
                error-handling: "skip-on-error"
                enrichment-ids:
                  - "enrich1"
            """;

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        EnrichmentGroup group = engine.getConfiguration().getEnrichmentGroupById("skip-group");
        assertNotNull(group, "Enrichment group should be created");
        assertEquals("skip-on-error", group.getErrorHandling(), "Error handling should be skip-on-error");
    }

    @Test
    @DisplayName("Test 4: Enrichment group with default error handling (no error-handling specified)")
    void testEnrichmentGroupWithDefaultErrorHandling() throws Exception {
        logger.info("=== Test 4: Testing enrichment group with default error handling ===");

        String yaml = """
            metadata:
              name: "Error Handling Test"
              type: "test-config"

            enrichments:
              - id: "enrich1"
                name: "Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "data.value"
                    target-field: "enriched.value"
            """;

        YamlRuleConfiguration config = yamlLoader.fromYamlString(yaml);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
    }
}

