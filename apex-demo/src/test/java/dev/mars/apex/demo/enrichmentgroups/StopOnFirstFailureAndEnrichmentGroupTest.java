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

package dev.mars.apex.demo.enrichmentgroups;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
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
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Stop-On-First-Failure AND Enrichment Group Tests.
 * 
 * Tests AND enrichment groups with stop-on-first-failure behavior enabled and disabled.
 * Demonstrates short-circuit evaluation where AND groups stop on the first false result
 * when stop-on-first-failure is enabled.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Stop-On-First-Failure AND Enrichment Group Tests")
public class StopOnFirstFailureAndEnrichmentGroupTest extends DemoTestBase {

    @Test
    @DisplayName("AND enrichment group with stop-on-first-failure: all conditions true")
    void testAndEnrichmentGroupStopOnFirstFailure_AllTrue() {
        logInfo("Testing AND enrichment group stop-on-first-failure with all true conditions");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - All True"
              version: "1.0.0"
              description: "AND enrichment group with all true conditions should execute all enrichments"

            enrichments:
              - id: "enrich1"
                name: "Copy Field A"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "a"
                    target-field: "a_copy"
                    required: true
              - id: "enrich2"
                name: "Copy Field B"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "b"
                    target-field: "b_copy"
                    required: true
              - id: "enrich3"
                name: "Copy Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true

            enrichment-groups:
              - id: "and-stop-all-true"
                name: "AND Stop All True"
                description: "AND enrichment group with stop-on-first-failure and all true conditions"
                operator: "AND"
                stop-on-first-failure: true
                enrichment-ids:
                  - "enrich1"
                  - "enrich2"
                  - "enrich3"
            """;
        
        YamlRuleConfiguration config = loadEnrichmentConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup group = groups.stream()
            .filter(g -> g.getId().equals("and-stop-all-true"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - all required fields present, so group should pass
        Map<String, Object> testData = new HashMap<>();
        testData.put("a", "A");
        testData.put("b", "B");
        testData.put("c", "C");
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Enrichment group should pass (all required fields present)");
        assertEquals(3, result.getEnrichmentResults().size(), "Should execute all 3 enrichments");
        assertEquals("A", testData.get("a_copy"));
        assertEquals("B", testData.get("b_copy"));
        assertEquals("C", testData.get("c_copy"));

        logSuccess("All true conditions executed successfully - all enrichments ran, group passed");
    }

    @Test
    @DisplayName("AND enrichment group with stop-on-first-failure: first condition false")
    void testAndEnrichmentGroupStopOnFirstFailure_FirstFalse() {
        logInfo("Testing AND enrichment group stop-on-first-failure with first condition false");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - First False"
              version: "1.0.0"
              description: "AND enrichment group should stop immediately on first false condition"

            enrichments:
              - id: "enrich1"
                name: "Copy Missing Field A"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "a"
                    target-field: "a_copy"
                    required: true
              - id: "enrich2"
                name: "Copy Field B"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "b"
                    target-field: "b_copy"
                    required: true
              - id: "enrich3"
                name: "Copy Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true

            enrichment-groups:
              - id: "and-stop-first-false"
                name: "AND Stop First False"
                description: "AND enrichment group should stop on first false condition"
                operator: "AND"
                stop-on-first-failure: true
                enrichment-ids:
                  - "enrich1"
                  - "enrich2"
                  - "enrich3"
            """;
        
        YamlRuleConfiguration config = loadEnrichmentConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup group = groups.stream()
            .filter(g -> g.getId().equals("and-stop-first-false"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - missing field 'a', first enrichment should fail
        Map<String, Object> testData = new HashMap<>();
        testData.put("b", "B");  // Provide b and c, but not a
        testData.put("c", "C");
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results - AND group with first enrichment failing should fail
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Enrichment group should fail (first enrichment fails, stop immediately)");

        // With stop-on-first-failure, should stop at first failed enrichment
        assertEquals(1, result.getEnrichmentResults().size(), "Should stop at first failed enrichment");
        assertNull(testData.get("a_copy"), "First enrichment should not produce output (missing required field)");
        assertNull(testData.get("b_copy"), "Second enrichment should not run (stopped)");
        assertNull(testData.get("c_copy"), "Third enrichment should not run (stopped)");

        logSuccess("First false condition executed - stopped immediately as expected");
    }

    @Test
    @DisplayName("AND enrichment group with stop-on-first-failure: middle condition false")
    void testAndEnrichmentGroupStopOnFirstFailure_MiddleFalse() {
        logInfo("Testing AND enrichment group stop-on-first-failure with middle condition false");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Middle False"
              version: "1.0.0"
              description: "AND enrichment group should stop at middle false condition"

            enrichments:
              - id: "enrich1"
                name: "Copy Field A"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "a"
                    target-field: "a_copy"
                    required: true
              - id: "enrich2"
                name: "Copy Missing Field B"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "b"
                    target-field: "b_copy"
                    required: true
              - id: "enrich3"
                name: "Copy Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true

            enrichment-groups:
              - id: "and-stop-middle-false"
                name: "AND Stop Middle False"
                description: "AND enrichment group should stop at middle false condition"
                operator: "AND"
                stop-on-first-failure: true
                enrichment-ids:
                  - "enrich1"
                  - "enrich2"
                  - "enrich3"
            """;

        YamlRuleConfiguration config = loadEnrichmentConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup group = groups.stream()
            .filter(g -> g.getId().equals("and-stop-middle-false"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - missing field 'b', second enrichment should fail
        Map<String, Object> testData = new HashMap<>();
        testData.put("a", "A");  // Provide a and c, but not b
        testData.put("c", "C");
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results - AND group with middle enrichment failing should fail
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Enrichment group should fail (middle enrichment fails, stopped at enrichment 2)");

        // Should execute first enrichment, fail on second, not execute third
        assertEquals(2, result.getEnrichmentResults().size(), "Should stop at second failed enrichment");
        assertEquals("A", testData.get("a_copy"), "First enrichment should succeed");
        assertNull(testData.get("b_copy"), "Second enrichment should not produce output (missing required field)");
        assertNull(testData.get("c_copy"), "Third enrichment should not run (stopped)");

        logSuccess("Middle false condition executed - stopped at enrichment 2 as expected");
    }

    @Test
    @DisplayName("AND enrichment group with stop-on-first-failure disabled")
    void testAndEnrichmentGroupStopOnFirstFailure_Disabled() {
        logInfo("Testing AND enrichment group with stop-on-first-failure disabled");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Disabled"
              version: "1.0.0"
              description: "AND enrichment group should execute all enrichments when stop-on-first-failure is disabled"

            enrichments:
              - id: "enrich1"
                name: "Copy Field A"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "a"
                    target-field: "a_copy"
                    required: true
              - id: "enrich2"
                name: "Copy Missing Field B"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "b"
                    target-field: "b_copy"
                    required: true
              - id: "enrich3"
                name: "Copy Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true
              - id: "enrich4"
                name: "Copy Missing Field D"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "d"
                    target-field: "d_copy"
                    required: true

            enrichment-groups:
              - id: "and-stop-disabled"
                name: "AND Stop Disabled"
                description: "AND enrichment group with stop-on-first-failure disabled"
                operator: "AND"
                stop-on-first-failure: false
                enrichment-ids:
                  - "enrich1"
                  - "enrich2"
                  - "enrich3"
                  - "enrich4"
            """;

        YamlRuleConfiguration config = loadEnrichmentConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup group = groups.stream()
            .filter(g -> g.getId().equals("and-stop-disabled"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - stop-on-first-failure disabled, should execute all enrichments
        Map<String, Object> testData = new HashMap<>();
        testData.put("a", "A");  // Provide a and c, but not b and d
        testData.put("c", "C");
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results - AND group should fail but all enrichments executed
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Enrichment group should fail (has missing required fields) but all enrichments executed");

        // Should execute all 4 enrichments despite failures
        assertEquals(4, result.getEnrichmentResults().size(), "Should execute all 4 enrichments");
        assertEquals("A", testData.get("a_copy"), "First enrichment should succeed");
        assertNull(testData.get("b_copy"), "Second enrichment should not produce output (missing required field)");
        assertEquals("C", testData.get("c_copy"), "Third enrichment should succeed");
        assertNull(testData.get("d_copy"), "Fourth enrichment should not produce output (missing required field)");

        logSuccess("Stop-on-first-failure disabled - all 4 enrichments executed despite failures");
    }

    // Helper method for consistent error handling
    private YamlRuleConfiguration loadEnrichmentConfiguration(String yamlContent) {
        try {
            return yamlLoader.fromYamlString(yamlContent);
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
            return null;
        }
    }
}
