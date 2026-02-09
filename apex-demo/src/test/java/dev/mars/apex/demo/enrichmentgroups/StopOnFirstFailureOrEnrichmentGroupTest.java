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

import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
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
import static dev.mars.apex.demo.ColoredTestOutputExtension.*;

/**
 * Stop-On-First-Failure OR Enrichment Group Tests.
 * 
 * Tests OR enrichment groups with stop-on-first-failure behavior using RulesEngine.evaluate().
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("Stop-On-First-Failure OR Enrichment Group Tests")
public class StopOnFirstFailureOrEnrichmentGroupTest extends DemoTestBase {

    @Test
    @DisplayName("RulesEngine processes OR enrichment group with stop-on-first-failure: first condition true")
    void testOrEnrichmentGroupStopOnFirstFailure_FirstTrue() {
        logInfo("Testing RulesEngine with OR enrichment group stop-on-first-failure (first true)");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure - First True"
              version: "1.0.0"
              description: "OR enrichment group with first condition true"

            enrichments:
              - id: "enrich1"
                name: "Copy Field A"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "a"
                    target-field: "a_copy"
              - id: "enrich2"
                name: "Copy Field B"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "b"
                    target-field: "b_copy"
              - id: "enrich3"
                name: "Copy Field C"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "c"
                    target-field: "c_copy"

            enrichment-groups:
              - id: "or-stop-first-true"
                name: "OR Stop First True"
                description: "OR enrichment group with stop-on-first-failure"
                operator: "OR"
                stop-on-first-failure: true
                enrichment-ids:
                  - "enrich1"
                  - "enrich2"
                  - "enrich3"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            assertNotNull(config, "Configuration should load successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("a", "A");
            testData.put("b", "B");
            testData.put("c", "C");
            
            RuleResult result = engine.evaluate(testData);

            assertTrue(result.isSuccess(), "RulesEngine should succeed");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("A", enrichedData.get("a_copy"));
            assertEquals("B", enrichedData.get("b_copy"));
            assertEquals("C", enrichedData.get("c_copy"));

            logSuccess("RulesEngine OR enrichment group with stop-on-first-failure succeeded");
        } catch (YamlConfigurationException e) {
            fail("Failed to load YAML or create RulesEngine: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RulesEngine processes OR enrichment group with stop-on-first-failure disabled")
    void testOrEnrichmentGroupStopOnFirstFailure_Disabled() {
        logInfo("Testing RulesEngine with OR enrichment group (stop-on-first-failure disabled)");
        
        String yamlContent = """
            metadata:
              name: "Stop On First Failure Disabled"
              version: "1.0.0"
              description: "OR enrichment group with stop-on-first-failure disabled"

            enrichments:
              - id: "enrich1"
                name: "Copy Field A"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "a"
                    target-field: "a_copy"
              - id: "enrich2"
                name: "Copy Field B"
                type: "field-enrichment"
                field-mappings:
                  - source-field: "b"
                    target-field: "b_copy"

            enrichment-groups:
              - id: "or-no-stop"
                name: "OR No Stop"
                description: "OR enrichment group without stop-on-first-failure"
                operator: "OR"
                stop-on-first-failure: false
                enrichment-ids:
                  - "enrich1"
                  - "enrich2"
            """;
        
        try {
            YamlRuleConfiguration config = yamlLoader.fromYamlString(yamlContent);
            assertNotNull(config, "Configuration should load successfully");

            RulesEngine engine = RulesEngine.fromYamlConfig(config);
            
            Map<String, Object> testData = new HashMap<>();
            testData.put("a", "A");
            testData.put("b", "B");
            
            RuleResult result = engine.evaluate(testData);

            assertTrue(result.isSuccess(), "RulesEngine should succeed");
            
            Map<String, Object> enrichedData = result.getEnrichedData();
            assertEquals("A", enrichedData.get("a_copy"));
            assertEquals("B", enrichedData.get("b_copy"));

            logSuccess("RulesEngine OR enrichment group (no stop) succeeded");
        } catch (YamlConfigurationException e) {
            fail("Failed to load YAML or create RulesEngine: " + e.getMessage());
        }
    }
}

