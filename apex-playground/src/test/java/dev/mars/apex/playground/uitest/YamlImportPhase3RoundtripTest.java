package dev.mars.apex.playground.uitest;

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

import dev.mars.apex.playground.ui.BaseYamlImportSeleniumTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3 Roundtrip Integrity Tests.
 *
 * <p>Validates the import of 4 new top-level config types:</p>
 * <ol>
 *   <li><b>Scenario</b> — {@code type: "scenario"} with classification rule and processing stages</li>
 *   <li><b>Scenario Registry</b> — {@code type: "scenario-registry"} with scenario refs and routing</li>
 *   <li><b>Component</b> — {@code type: "component"} with grouped file references</li>
 *   <li><b>Pipeline</b> — pipeline config (no metadata) with stages</li>
 * </ol>
 *
 * @author APEX Test Suite
 * @since 2025-12-20
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class YamlImportPhase3RoundtripTest extends BaseYamlImportSeleniumTest {

    // ========================================================================
    // SCENARIO CONFIG (Gap #9)
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Phase3-RT-1: Scenario config import creates blocks")
    void testScenarioImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_scenario_config", 1, "Should have 1 Scenario Configuration block");
        verifyBlockExists("apex_section_scenario", 1, "Should have 1 Scenario section block");
        verifyBlockExists("apex_classification_rule", 1, "Should have 1 Classification Rule block");
        verifyBlockExists("apex_processing_stage", 3, "Should have 3 Processing Stage blocks");
    }

    @Test
    @Order(2)
    @DisplayName("Phase3-RT-2: Scenario config metadata fields imported correctly")
    void testScenarioMetadataFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockFieldValues("apex_scenario_config", Map.of(
                "ID", "fx-forward-scenario",
                "NAME", "FX Forward Processing",
                "VERSION", "2.1.0",
                "BUSINESS_DOMAIN", "FX Trading",
                "DESCRIPTION", "FX forward trade validation and settlement",
                "AUTHOR", "trading-team@example.com",
                "CREATED_BY", "john.doe",
                "OWNER", "fx-desk@example.com"
        ));
    }

    @Test
    @Order(3)
    @DisplayName("Phase3-RT-3: Scenario section fields imported correctly")
    void testScenarioSectionFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockFieldValues("apex_section_scenario", Map.of(
                "SCENARIO_ID", "fx-forward-validation",
                "DESCRIPTION", "Validates and processes FX forward trades"
        ));
    }

    @Test
    @Order(4)
    @DisplayName("Phase3-RT-4: Scenario config roundtrip preserves structure")
    void testScenarioRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        String exported = exportYamlContent();

        System.out.println("=== SCENARIO ROUNDTRIP EXPORTED YAML ===");
        System.out.println(exported);
        System.out.println("=== END SCENARIO ROUNDTRIP ===");

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.trim().isEmpty(), "Exported YAML should not be empty");

        verifyYamlStructure(exported, List.of("metadata", "scenario"));
        assertTrue(exported.contains("type: scenario") || exported.contains("type: \"scenario\""),
                "Should contain type: scenario");
        assertTrue(exported.contains("fx-forward-scenario"), "Should contain scenario ID");
        assertTrue(exported.contains("scenario-id:"), "Should contain scenario-id key");
        assertTrue(exported.contains("classification-rule:"), "Should contain classification-rule");
        assertTrue(exported.contains("processing-stages:"), "Should contain processing-stages");
    }

    @Test
    @Order(5)
    @DisplayName("Phase3-RT-5: Processing stage fields imported correctly")
    void testProcessingStageFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // Verify first processing stage (validation) — getBlockFieldValue gets first block of type
        verifyBlockFieldValues("apex_processing_stage", Map.of(
                "STAGE_NAME", "validation",
                "CONFIG_FILE", "config/fx-validation-rules.yaml",
                "FAILURE_POLICY", "terminate"
        ));
    }

    // ========================================================================
    // SCENARIO REGISTRY (Gap #10)
    // ========================================================================

    @Test
    @Order(6)
    @DisplayName("Phase3-RT-6: Scenario registry import creates blocks")
    void testScenarioRegistryImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-registry-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_scenario_registry", 1, "Should have 1 Scenario Registry block");
        verifyBlockExists("apex_scenario_ref", 3, "Should have 3 Scenario Reference blocks");
    }

    @Test
    @Order(7)
    @DisplayName("Phase3-RT-7: Scenario registry metadata and routing fields")
    void testScenarioRegistryFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-registry-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockFieldValues("apex_scenario_registry", Map.of(
                "ID", "trade-processing-registry",
                "NAME", "Trade Processing Registry",
                "VERSION", "1.5.0",
                "DESCRIPTION", "Routes trades to appropriate processing scenarios",
                "CREATED_BY", "admin",
                "ROUTING_STRATEGY", "classification-based",
                "DEFAULT_SCENARIO", "fx-forward-scenario"
        ));
    }

    @Test
    @Order(8)
    @DisplayName("Phase3-RT-8: Scenario ref fields imported correctly")
    void testScenarioRefFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-registry-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // First scenario ref
        verifyBlockFieldValues("apex_scenario_ref", Map.of(
                "SCENARIO_ID", "fx-forward-scenario",
                "CONFIG_FILE", "scenarios/fx-forward.yaml",
                "BUSINESS_DOMAIN", "FX Trading"
        ));
    }

    @Test
    @Order(9)
    @DisplayName("Phase3-RT-9: Scenario registry roundtrip preserves structure")
    void testScenarioRegistryRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-registry-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.trim().isEmpty(), "Exported YAML should not be empty");

        verifyYamlStructure(exported, List.of("metadata", "scenarios", "routing"));
        assertTrue(exported.contains("type: scenario-registry") || exported.contains("type: \"scenario-registry\""),
                "Should contain type: scenario-registry");
        assertTrue(exported.contains("trade-processing-registry"), "Should contain registry ID");
        assertTrue(exported.contains("fx-forward-scenario"), "Should contain scenario references");
        assertTrue(exported.contains("classification-based"), "Should contain routing strategy");
    }

    // ========================================================================
    // COMPONENT CONFIG (Gap #11)
    // ========================================================================

    @Test
    @Order(10)
    @DisplayName("Phase3-RT-10: Component config import creates blocks")
    void testComponentImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/component-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_component_config", 1, "Should have 1 Component Configuration block");
        // 2 rule-configurations + 1 enrichment-ref + 1 component-ref = 4 file references
        verifyBlockExists("apex_file_reference", 4, "Should have 4 File Reference blocks");
    }

    @Test
    @Order(11)
    @DisplayName("Phase3-RT-11: Component metadata with criticality and SLA")
    void testComponentMetadataFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/component-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockFieldValues("apex_component_config", Map.of(
                "ID", "comprehensive-validation",
                "NAME", "Comprehensive Validation Component",
                "VERSION", "3.0.0",
                "DESCRIPTION", "Groups validation, enrichment, and compliance rules",
                "BUSINESS_DOMAIN", "Trade Processing",
                "OWNER", "compliance-team@example.com",
                "CREATED_BY", "jane.smith",
                "CRITICALITY", "high"
        ));

        // Check SLA separately since it's a number field
        String slaValue = getBlockFieldValue("apex_component_config", "SLA_MS");
        assertEquals("500", slaValue, "SLA should be 500ms");
    }

    @Test
    @Order(12)
    @DisplayName("Phase3-RT-12: Component file references have correct types")
    void testComponentFileRefTypes() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/component-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // First file reference should be rule-configurations type
        verifyBlockFieldValues("apex_file_reference", Map.of(
                "REF_TYPE", "rule-configurations",
                "FILE", "rules/basic-validation.yaml",
                "FAILURE_POLICY", "terminate"
        ));
    }

    @Test
    @Order(13)
    @DisplayName("Phase3-RT-13: Component roundtrip preserves grouped file refs")
    void testComponentRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/component-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.trim().isEmpty(), "Exported YAML should not be empty");

        verifyYamlStructure(exported, List.of("metadata"));
        assertTrue(exported.contains("type: component") || exported.contains("type: \"component\""),
                "Should contain type: component");
        assertTrue(exported.contains("comprehensive-validation"), "Should contain component ID");
        assertTrue(exported.contains("rule-configurations:"), "Should contain rule-configurations section");
        assertTrue(exported.contains("enrichment-refs:"), "Should contain enrichment-refs section");
        assertTrue(exported.contains("component-refs:"), "Should contain component-refs section");
    }

    // ========================================================================
    // PIPELINE CONFIG (Gap #12)
    // ========================================================================

    @Test
    @Order(14)
    @DisplayName("Phase3-RT-14: Pipeline config import creates blocks (no metadata)")
    void testPipelineImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/pipeline-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_pipeline_config", 1, "Should have 1 Pipeline Configuration block");
        verifyBlockExists("apex_pipeline_stage", 4, "Should have 4 Pipeline Stage blocks");
    }

    @Test
    @Order(15)
    @DisplayName("Phase3-RT-15: Pipeline config fields imported correctly")
    void testPipelineConfigFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/pipeline-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockFieldValues("apex_pipeline_config", Map.of(
                "ID", "data-processing-pipeline",
                "MODE", "sequential",
                "ERROR_HANDLING", "continue-on-error",
                "COLLECT_METRICS", "TRUE"
        ));

        // Check numeric fields
        String maxRetries = getBlockFieldValue("apex_pipeline_config", "MAX_RETRIES");
        assertEquals("5", maxRetries, "Max retries should be 5");

        String retryDelay = getBlockFieldValue("apex_pipeline_config", "RETRY_DELAY_MS");
        assertEquals("2000", retryDelay, "Retry delay should be 2000ms");
    }

    @Test
    @Order(16)
    @DisplayName("Phase3-RT-16: Pipeline stage fields imported correctly")
    void testPipelineStageFields() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/pipeline-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // First stage (extract)
        verifyBlockFieldValues("apex_pipeline_stage", Map.of(
                "NAME", "extract",
                "ENABLED", "TRUE"
        ));

        String order = getBlockFieldValue("apex_pipeline_stage", "ORDER");
        assertEquals("1", order, "First stage order should be 1");
    }

    @Test
    @Order(17)
    @DisplayName("Phase3-RT-17: Pipeline roundtrip preserves structure")
    void testPipelineRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/pipeline-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.trim().isEmpty(), "Exported YAML should not be empty");

        verifyYamlStructure(exported, List.of("pipeline"));
        assertTrue(exported.contains("data-processing-pipeline"), "Should contain pipeline ID");
        assertTrue(exported.contains("stages:"), "Should contain stages section");
        assertTrue(exported.contains("extract"), "Should contain extract stage");
        assertTrue(exported.contains("transform"), "Should contain transform stage");
        assertTrue(exported.contains("validate"), "Should contain validate stage");
        assertTrue(exported.contains("load"), "Should contain load stage");
    }

    // ========================================================================
    // COMBINED / CROSS-TYPE TESTS
    // ========================================================================

    @Test
    @Order(18)
    @DisplayName("Phase3-RT-18: Component with all 4 ref types imports correctly")
    void testComponentAllRefTypes() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/phase3-combined-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_component_config", 1, "Should have 1 Component Configuration block");
        // 1 rule-config + 1 enrichment-ref + 1 component-ref + 1 config-file = 4
        verifyBlockExists("apex_file_reference", 4, "Should have 4 File Reference blocks (one per ref type)");

        // Verify the component metadata
        verifyBlockFieldValues("apex_component_config", Map.of(
                "ID", "combined-phase3-component",
                "CRITICALITY", "medium"
        ));

        // Roundtrip should have all 4 sections
        String exported = exportYamlContent();
        assertTrue(exported.contains("rule-configurations:"), "Roundtrip should have rule-configurations");
        assertTrue(exported.contains("enrichment-refs:"), "Roundtrip should have enrichment-refs");
        assertTrue(exported.contains("component-refs:"), "Roundtrip should have component-refs");
        assertTrue(exported.contains("config-files:"), "Roundtrip should have config-files");
    }

    @Test
    @Order(19)
    @DisplayName("Phase3-RT-19: Scenario with tags roundtrips correctly")
    void testScenarioTagsRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // Verify tags field
        String tags = getBlockFieldValue("apex_scenario_config", "TAGS");
        assertNotNull(tags, "Tags field should not be null");
        assertTrue(tags.contains("fx"), "Tags should contain 'fx'");
        assertTrue(tags.contains("forward"), "Tags should contain 'forward'");
        assertTrue(tags.contains("scenario"), "Tags should contain 'scenario'");

        // Roundtrip should preserve tags
        String exported = exportYamlContent();
        assertTrue(exported.contains("tags:"), "Exported should have tags section");
    }

    @Test
    @Order(20)
    @DisplayName("Phase3-RT-20: Scenario registry tags roundtrip")
    void testScenarioRegistryTagsRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/scenario-registry-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String tags = getBlockFieldValue("apex_scenario_registry", "TAGS");
        assertNotNull(tags, "Tags field should not be null");
        assertTrue(tags.contains("registry"), "Tags should contain 'registry'");
        assertTrue(tags.contains("trading"), "Tags should contain 'trading'");
    }

    // ========================================================================
    // REGRESSION
    // ========================================================================

    @Test
    @Order(21)
    @DisplayName("Phase3-RT-21: Regression — rule-config still imports correctly")
    void testRegressionRuleConfigStillWorks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/combined-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_rule_config", 1, "Rule config should still import");
        String exported = exportYamlContent();
        assertNotNull(exported, "Exported YAML should not be null");
        assertTrue(exported.contains("metadata:"), "Should still have metadata section");
    }
}
