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
 * Stop-On-First-Failure OR Enrichment Group Tests.
 * 
 * Tests OR enrichment groups with stop-on-first-failure behavior enabled and disabled.
 * Demonstrates short-circuit evaluation where OR groups stop on the first true result
 * when stop-on-first-failure is enabled.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Stop-On-First-Failure OR Enrichment Group Tests")
public class StopOnFirstFailureOrEnrichmentGroupTest extends DemoTestBase {

    @Test
    @DisplayName("OR enrichment group with stop-on-first-failure: all conditions false")
    void testOrEnrichmentGroupStopOnFirstFailure_AllFalse() {
        logInfo("Testing OR enrichment group stop-on-first-failure with all false conditions");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - All False"
              version: "1.0.0"
              description: "OR enrichment group with all false conditions should execute all enrichments"

            enrichments:
              - id: "enrich1"
                name: "Copy Missing Field A"
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
                name: "Copy Missing Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true

            enrichment-groups:
              - id: "or-stop-all-false"
                name: "OR Stop All False"
                description: "OR enrichment group with stop-on-first-failure and all false conditions"
                operator: "OR"
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
            .filter(g -> g.getId().equals("or-stop-all-false"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - all required fields missing, so group should fail (all enrichments executed)
        Map<String, Object> testData = new HashMap<>();
        // Don't provide any data - all enrichments should fail due to missing required fields
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Enrichment group should fail (all required fields missing)");
        assertEquals(3, result.getEnrichmentResults().size(), "Should execute all 3 enrichments when all fail");
        assertNull(testData.get("a_copy"), "First enrichment should not produce output (missing required field)");
        assertNull(testData.get("b_copy"), "Second enrichment should not produce output (missing required field)");
        assertNull(testData.get("c_copy"), "Third enrichment should not produce output (missing required field)");

        logSuccess("All false conditions executed - all enrichments ran, group failed as expected");
    }

    @Test
    @DisplayName("OR enrichment group with stop-on-first-failure: first condition true")
    void testOrEnrichmentGroupStopOnFirstFailure_FirstTrue() {
        logInfo("Testing OR enrichment group stop-on-first-failure with first condition true");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - First True"
              version: "1.0.0"
              description: "OR enrichment group should stop immediately on first true condition"

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
                name: "Copy Missing Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true

            enrichment-groups:
              - id: "or-stop-first-true"
                name: "OR Stop First True"
                description: "OR enrichment group should stop on first true condition"
                operator: "OR"
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
            .filter(g -> g.getId().equals("or-stop-first-true"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - first required field present, should stop immediately
        Map<String, Object> testData = new HashMap<>();
        testData.put("a", "A");  // Provide only field 'a', not b or c
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results - OR group with first enrichment succeeding should pass
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Enrichment group should pass (first enrichment succeeds, stop immediately)");
        assertEquals(1, result.getEnrichmentResults().size(), "Should stop after first successful enrichment");
        assertEquals("A", testData.get("a_copy"), "First enrichment should succeed");
        assertNull(testData.get("b_copy"), "Second enrichment should not run (stopped)");
        assertNull(testData.get("c_copy"), "Third enrichment should not run (stopped)");

        logSuccess("First true condition executed - stopped immediately as expected");
    }

    @Test
    @DisplayName("OR enrichment group with stop-on-first-failure: middle condition true")
    void testOrEnrichmentGroupStopOnFirstFailure_MiddleTrue() {
        logInfo("Testing OR enrichment group stop-on-first-failure with middle condition true");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Middle True"
              version: "1.0.0"
              description: "OR enrichment group should stop at middle true condition"

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
                name: "Copy Missing Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true

            enrichment-groups:
              - id: "or-stop-middle-true"
                name: "OR Stop Middle True"
                description: "OR enrichment group should stop at middle true condition"
                operator: "OR"
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
            .filter(g -> g.getId().equals("or-stop-middle-true"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - middle enrichment succeeds, should stop at enrichment 2
        Map<String, Object> testData = new HashMap<>();
        testData.put("b", "B");  // Provide only field 'b', not a or c
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results - OR group with middle enrichment succeeding should pass
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Enrichment group should pass (middle enrichment succeeds, stopped at enrichment 2)");
        assertEquals(2, result.getEnrichmentResults().size(), "Should execute first two enrichments");
        assertNull(testData.get("a_copy"), "First enrichment should not produce output (missing required field)");
        assertEquals("B", testData.get("b_copy"), "Second enrichment should succeed");
        assertNull(testData.get("c_copy"), "Third enrichment should not run (stopped)");

        logSuccess("Middle true condition executed - stopped at enrichment 2 as expected");
    }

    @Test
    @DisplayName("OR enrichment group with stop-on-first-failure disabled")
    void testOrEnrichmentGroupStopOnFirstFailure_Disabled() {
        logInfo("Testing OR enrichment group with stop-on-first-failure disabled");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Disabled"
              version: "1.0.0"
              description: "OR enrichment group should execute all enrichments when stop-on-first-failure is disabled"

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
                name: "Copy Missing Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"
                    required: true
              - id: "enrich4"
                name: "Copy Field D"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "d"
                    target-field: "d_copy"
                    required: true

            enrichment-groups:
              - id: "or-stop-disabled"
                name: "OR Stop Disabled"
                description: "OR enrichment group with stop-on-first-failure disabled"
                operator: "OR"
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
            .filter(g -> g.getId().equals("or-stop-disabled"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - stop-on-first-failure disabled, should execute all enrichments
        Map<String, Object> testData = new HashMap<>();
        testData.put("b", "B");  // Provide fields 'b' and 'd', not a or c
        testData.put("d", "D");
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results - OR group should pass (has successful enrichments) and all enrichments executed
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Enrichment group should pass (has successful enrichments) and all enrichments executed");
        assertEquals(4, result.getEnrichmentResults().size(), "Should execute all 4 enrichments");
        assertNull(testData.get("a_copy"), "First enrichment should not produce output (missing required field)");
        assertEquals("B", testData.get("b_copy"), "Second enrichment should succeed");
        assertNull(testData.get("c_copy"), "Third enrichment should not produce output (missing required field)");
        assertEquals("D", testData.get("d_copy"), "Fourth enrichment should succeed");

        logSuccess("Stop-on-first-failure disabled - all 4 enrichments executed despite early successes");
    }

    @Test
    @DisplayName("OR enrichment group with stop-on-first-failure: multiple enrichments with middle success")
    void testOrEnrichmentGroupStopOnFirstFailure_MultipleEnrichments() {
        logInfo("Testing OR enrichment group stop-on-first-failure with 5 enrichments (F,F,T,F,F)");

        String yamlContent = """
            metadata:
              name: "Stop On First Failure - Multiple Enrichments"
              version: "1.0.0"
              description: "OR enrichment group with 5 enrichments should stop at 3rd enrichment success"

            enrichments:
              - id: "enrich1"
                name: "Copy Missing Field A"
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
              - id: "enrich5"
                name: "Copy Missing Field E"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "e"
                    target-field: "e_copy"
                    required: true

            enrichment-groups:
              - id: "or-stop-multiple-enrichments"
                name: "OR Stop Multiple Enrichments"
                description: "OR enrichment group with 5 enrichments should stop at 3rd enrichment"
                operator: "OR"
                stop-on-first-failure: true
                enrichment-ids:
                  - "enrich1"
                  - "enrich2"
                  - "enrich3"
                  - "enrich4"
                  - "enrich5"
            """;

        YamlRuleConfiguration config = loadEnrichmentConfiguration(yamlContent);
        assertNotNull(config, "Configuration should load successfully");

        List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
        EnrichmentGroup group = groups.stream()
            .filter(g -> g.getId().equals("or-stop-multiple-enrichments"))
            .findFirst().orElse(null);
        assertNotNull(group, "Enrichment group should be found");

        // Execute enrichment group - 5 enrichments (F,F,T,F,F), should stop at enrichment 3
        Map<String, Object> testData = new HashMap<>();
        testData.put("c", "C");  // Provide only field 'c', not a, b, d, or e
        EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

        // Validate results - OR group should pass at enrichment 3, enrichments 4 and 5 not executed
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Enrichment group should pass (stopped at enrichment 3)");
        assertEquals(3, result.getEnrichmentResults().size(), "Should execute first 3 enrichments");
        assertNull(testData.get("a_copy"), "First enrichment should not produce output (missing required field)");
        assertNull(testData.get("b_copy"), "Second enrichment should not produce output (missing required field)");
        assertEquals("C", testData.get("c_copy"), "Third enrichment should succeed");
        assertNull(testData.get("d_copy"), "Fourth enrichment should not run (stopped)");
        assertNull(testData.get("e_copy"), "Fifth enrichment should not run (stopped)");

        logSuccess("Multiple enrichments executed - stopped at enrichment 3 as expected, enrichments 4 and 5 not executed");
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
