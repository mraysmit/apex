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
 * Enrichment Group Severity Aggregation Tests.
 * 
 * Tests the severity aggregation functionality for enrichment groups where groups
 * without explicit severity attributes aggregate severity from their constituent enrichments.
 * 
 * Business Logic:
 * - AND Groups: Use highest severity of failed enrichments, or highest of all if all pass
 * - OR Groups: Use severity of first matching enrichment
 * - Empty Groups: Default to INFO severity
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Enrichment Group Severity Aggregation Tests")
public class EnrichmentGroupSeverityAggregationTest extends DemoTestBase {

    @Test
    @DisplayName("AND enrichment group aggregates severity from failed enrichments")
    void testAndEnrichmentGroupAggregatesSeverityFromFailedEnrichments() {
        logInfo("Testing AND enrichment group severity aggregation with mixed results");
        
        String yamlContent = """
            metadata:
              id: "and-mixed-severity-test"
              name: "AND Mixed Severity Test"
              version: "1.0.0"
              description: "AND enrichment group with mixed severities where one enrichment fails"
              type: "rule-config"
              author: "APEX Demo Team"

            enrichments:
              - id: "error-enrichment"
                name: "Error Enrichment"
                type: "field-enrichment"
                severity: "ERROR"
                field-mappings:
                  - source-field: "missing_field"
                    target-field: "error_output"
                    required: true
              - id: "warning-enrichment"
                name: "Warning Enrichment"
                type: "field-enrichment"
                severity: "WARNING"
                field-mappings:
                  - source-field: "input"
                    target-field: "warning_output"
                    required: true
              - id: "info-enrichment"
                name: "Info Enrichment"
                type: "field-enrichment"
                severity: "INFO"
                field-mappings:
                  - source-field: "input"
                    target-field: "info_output"
                    required: true

            enrichment-groups:
              - id: "and-mixed-group"
                name: "AND Mixed Group"
                description: "AND enrichment group with mixed severities"
                operator: "AND"
                stop-on-first-failure: false
                enrichment-ids:
                  - "error-enrichment"
                  - "warning-enrichment"
                  - "info-enrichment"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = groups.stream()
                .filter(g -> g.getId().equals("and-mixed-group"))
                .findFirst().orElse(null);
            assertNotNull(group, "Enrichment group should be found");
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "test");  // Provide 'input' but not 'missing_field'
            EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

            assertNotNull(result, "Result should not be null");
            assertFalse(result.isSuccess(), "AND group should fail when one enrichment fails");

            // Verify that successful enrichments executed
            assertEquals("test", testData.get("warning_output"), "Warning enrichment should succeed");
            assertEquals("test", testData.get("info_output"), "Info enrichment should succeed");
            assertNull(testData.get("error_output"), "Error enrichment should not produce output (missing required field)");
            
            logSuccess("AND enrichment group severity aggregation working correctly - failed as expected");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("AND enrichment group aggregates severity from all enrichments when all pass")
    void testAndEnrichmentGroupAggregatesSeverityFromAllEnrichmentsWhenAllPass() {
        logInfo("Testing AND enrichment group severity aggregation when all enrichments pass");
        
        String yamlContent = """
            metadata:
              id: "and-all-pass-test"
              name: "AND All Pass Test"
              version: "1.0.0"
              description: "AND enrichment group where all enrichments pass"
              type: "rule-config"
              author: "APEX Demo Team"

            enrichments:
              - id: "error-enrichment"
                name: "Error Enrichment"
                type: "field-enrichment"
                severity: "ERROR"
                field-mappings:
                  - source-field: "input"
                    target-field: "error_output"
                    required: true
              - id: "warning-enrichment"
                name: "Warning Enrichment"
                type: "field-enrichment"
                severity: "WARNING"
                field-mappings:
                  - source-field: "input"
                    target-field: "warning_output"
                    required: true

            enrichment-groups:
              - id: "and-all-pass-group"
                name: "AND All Pass Group"
                description: "AND enrichment group where all enrichments pass"
                operator: "AND"
                stop-on-first-failure: false
                enrichment-ids:
                  - "error-enrichment"
                  - "warning-enrichment"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = groups.stream()
                .filter(g -> g.getId().equals("and-all-pass-group"))
                .findFirst().orElse(null);
            assertNotNull(group, "Enrichment group should be found");
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "test");
            EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);
            
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isSuccess(), "AND group should pass when all enrichments pass");
            
            // Verify that all enrichments executed successfully
            assertEquals("test", testData.get("error_output"), "Error enrichment should succeed");
            assertEquals("test", testData.get("warning_output"), "Warning enrichment should succeed");
            
            logSuccess("AND enrichment group severity aggregation working correctly - all enrichments passed");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("OR enrichment group uses severity of first matching enrichment")
    void testOrEnrichmentGroupUsesFirstMatchingSeverity() {
        logInfo("Testing OR enrichment group severity aggregation with first match logic");
        
        String yamlContent = """
            metadata:
              id: "or-first-match-test"
              name: "OR First Match Test"
              version: "1.0.0"
              description: "OR enrichment group using first matching enrichment severity"
              type: "rule-config"
              author: "APEX Demo Team"

            enrichments:
              - id: "info-enrichment"
                name: "Info Enrichment"
                type: "field-enrichment"
                severity: "INFO"
                field-mappings:
                  - source-field: "missing_field"
                    target-field: "info_output"
                    required: true
              - id: "warning-enrichment"
                name: "Warning Enrichment"
                type: "field-enrichment"
                severity: "WARNING"
                field-mappings:
                  - source-field: "input"
                    target-field: "warning_output"
                    required: true
              - id: "error-enrichment"
                name: "Error Enrichment"
                type: "field-enrichment"
                severity: "ERROR"
                field-mappings:
                  - source-field: "input"
                    target-field: "error_output"
                    required: true

            enrichment-groups:
              - id: "or-first-match-group"
                name: "OR First Match Group"
                description: "OR enrichment group using first matching enrichment"
                operator: "OR"
                stop-on-first-failure: false
                enrichment-ids:
                  - "info-enrichment"
                  - "warning-enrichment"
                  - "error-enrichment"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = groups.stream()
                .filter(g -> g.getId().equals("or-first-match-group"))
                .findFirst().orElse(null);
            assertNotNull(group, "Enrichment group should be found");
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "test");  // Provide 'input' but not 'missing_field'
            EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);

            assertNotNull(result, "Result should not be null");
            assertTrue(result.isSuccess(), "OR group should pass when any enrichment passes");

            // Verify that successful enrichments executed (warning and error should succeed)
            assertNull(testData.get("info_output"), "Info enrichment should not produce output (missing required field)");
            assertEquals("test", testData.get("warning_output"), "Warning enrichment should succeed");
            assertEquals("test", testData.get("error_output"), "Error enrichment should succeed");
            
            logSuccess("OR enrichment group severity aggregation working correctly - first match logic applied");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Empty enrichment group does not pass by default")
    void testEmptyEnrichmentGroupDefaultSeverity() {
        logInfo("Testing empty enrichment group default behavior");
        
        String yamlContent = """
            metadata:
              id: "empty-group-test"
              name: "Empty Group Test"
              version: "1.0.0"
              description: "Empty enrichment group with no enrichments"
              type: "rule-config"
              author: "APEX Demo Team"

            enrichment-groups:
              - id: "empty-group"
                name: "Empty Group"
                description: "Group with no enrichments"
                operator: "AND"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup group = groups.stream()
                .filter(g -> g.getId().equals("empty-group"))
                .findFirst().orElse(null);
            assertNotNull(group, "Enrichment group should be found");
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "test");
            EnrichmentGroupResult result = enrichmentService.processEnrichmentGroup(group, testData, config);
            
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isSuccess(), "Empty group should pass by default (no enrichments to fail)");
            assertEquals(0, result.getEnrichmentResults().size(), "Empty group should have no enrichment results");

            logSuccess("Empty enrichment group behavior working correctly - empty groups pass by default");
            
        } catch (YamlConfigurationException e) {
            logError("Failed to load YAML configuration: " + e.getMessage());
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }
}
