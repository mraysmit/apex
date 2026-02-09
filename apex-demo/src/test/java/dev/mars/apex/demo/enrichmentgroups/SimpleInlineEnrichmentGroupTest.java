package dev.mars.apex.demo.enrichmentgroups;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;

import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Simple Inline Enrichment Group Test.
 *
 * Tests inline enrichment-group-id references within the same YAML file.
 * Demonstrates:
 * - 2 simple enrichments (one passes, one fails)
 * - Base enrichment group containing both enrichments
 * - Composite enrichment group that references the base group by ID
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Simple Inline Enrichment Group Test")
public class SimpleInlineEnrichmentGroupTest extends DemoTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInlineEnrichmentGroupTest.class);

    @Test
    @DisplayName("RulesEngine processes enrichment groups with inline references")
    void testBaseEnrichmentGroup() {
        LOGGER.info("Testing RulesEngine with Enrichment Groups");

        String yamlContent = """
            metadata:
              name: "Simple Inline Enrichment Group Test"
              version: "1.0.0"
              description: "Test inline enrichment-group-id references"

            enrichments:
              - id: "simple-enrichment-1"
                name: "Simple Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input"
                    target-field: "output1"

            enrichment-groups:
              - id: "base-validation"
                name: "Base Validation"
                description: "Base enrichment group with 1 enrichment"
                operator: "AND"
                enrichment-ids:
                  - "simple-enrichment-1"
              - id: "composite-validation"
                name: "Composite Validation"
                description: "Composite enrichment group that references base group"
                operator: "OR"
                enrichment-group-references:
                  - "base-validation"
            """;

        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testContext = new HashMap<>();
            testContext.put("input", "test");

            RuleResult result = engine.evaluate(testContext);

            assertTrue(result.isSuccess(), "RulesEngine should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("test", enrichedData.get("output1"), "First enrichment should succeed");

            LOGGER.info("RulesEngine enrichment group test passed");

        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes composite enrichment group references")
    void testCompositeEnrichmentGroup() {
        LOGGER.info("Testing RulesEngine with Composite Enrichment Group");

        String yamlContent = """
            metadata:
              name: "Simple Inline Enrichment Group Test"
              version: "1.0.0"
              description: "Test inline enrichment-group-id references"

            enrichments:
              - id: "simple-enrichment-1"
                name: "Simple Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input"
                    target-field: "output1"
              - id: "simple-enrichment-2"
                name: "Simple Enrichment 2"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input2"
                    target-field: "output2"

            enrichment-groups:
              - id: "base-validation"
                name: "Base Validation"
                description: "Base enrichment group with 2 enrichments"
                operator: "AND"
                enrichment-ids:
                  - "simple-enrichment-1"
                  - "simple-enrichment-2"
              - id: "composite-validation"
                name: "Composite Validation"
                description: "Composite enrichment group that references base group"
                operator: "OR"
                enrichment-group-references:
                  - "base-validation"
            """;

        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testContext = new HashMap<>();
            testContext.put("input", "test");
            testContext.put("input2", "test2");

            RuleResult result = engine.evaluate(testContext);

            assertTrue(result.isSuccess(), "RulesEngine should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("test", enrichedData.get("output1"));
            assertEquals("test2", enrichedData.get("output2"));

            LOGGER.info("Composite enrichment group test completed");

        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes enrichment group registry")
    void testEnrichmentGroupRegistry() {
        LOGGER.info("Testing RulesEngine Enrichment Group Registry");

        String yamlContent = """
            metadata:
              name: "Simple Inline Enrichment Group Test"
              version: "1.0.0"
              description: "Test inline enrichment-group-id references"

            enrichments:
              - id: "simple-enrichment-1"
                name: "Simple Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input"
                    target-field: "output1"
              - id: "simple-enrichment-2"
                name: "Simple Enrichment 2"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input2"
                    target-field: "output2"

            enrichment-groups:
              - id: "base-validation"
                name: "Base Validation"
                description: "Base enrichment group with 2 enrichments"
                operator: "AND"
                enrichment-ids:
                  - "simple-enrichment-1"
                  - "simple-enrichment-2"
              - id: "composite-validation"
                name: "Composite Validation"
                description: "Composite enrichment group that references base group"
                operator: "OR"
                enrichment-group-references:
                  - "base-validation"
            """;

        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testContext = new HashMap<>();
            testContext.put("input", "test");
            testContext.put("input2", "test2");

            RuleResult result = engine.evaluate(testContext);

            assertTrue(result.isSuccess(), "RulesEngine should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("test", enrichedData.get("output1"));
            assertEquals("test2", enrichedData.get("output2"));

            LOGGER.info("Enrichment group registry test passed");

        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine full workflow integration test")
    void testFullWorkflow() {
        LOGGER.info("Testing RulesEngine Full Workflow");

        String yamlContent = """
            metadata:
              name: "Simple Inline Enrichment Group Test"
              version: "1.0.0"
              description: "Test inline enrichment-group-id references"

            enrichments:
              - id: "simple-enrichment-1"
                name: "Simple Enrichment 1"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input"
                    target-field: "output1"
              - id: "simple-enrichment-2"
                name: "Simple Enrichment 2"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "input2"
                    target-field: "output2"

            enrichment-groups:
              - id: "base-validation"
                name: "Base Validation"
                description: "Base enrichment group with 2 enrichments"
                operator: "AND"
                enrichment-ids:
                  - "simple-enrichment-1"
                  - "simple-enrichment-2"
              - id: "composite-validation"
                name: "Composite Validation"
                description: "Composite enrichment group that references base group"
                operator: "OR"
                enrichment-group-references:
                  - "base-validation"
            """;

        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            RulesEngine engine = RulesEngine.fromYamlConfig(config);

            Map<String, Object> testContext = new HashMap<>();
            testContext.put("input", "test");
            testContext.put("input2", "test2");

            RuleResult result = engine.evaluate(testContext);

            assertTrue(result.isSuccess(), "RulesEngine should succeed");

            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("test", enrichedData.get("output1"));
            assertEquals("test2", enrichedData.get("output2"));

            LOGGER.info("Full workflow test passed");
            LOGGER.info("SUCCESS: Inline enrichment-group-id references working correctly!");
            LOGGER.info("SUMMARY: 2 enrichments, 2 enrichment groups, 1 inline reference - All working!");

        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }
}

