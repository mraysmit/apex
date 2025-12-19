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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Selenium tests for YAML import validation covering tests 7-35.
 * <p>
 * Simplified version focusing on successful import and basic block validation.
 * Tests 29 YAML samples across configuration, scenarios, components, error recovery,
 * data sources, rules, transformations, enrichments, lookups, and templates.
 * </p>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-12-19
 * @version 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class YamlImportPhase1SimpleTest extends BaseYamlImportSeleniumTest {

    private static final String EXAMPLES_BASE_PATH = "examples/";

    // ==========================
    // CONFIGURATION VARIATIONS (Tests 7-9)
    // ==========================

    @Test
    @Order(7)
    @DisplayName("Test 7: Import YAML with global settings configuration")
    void test07_ImportGlobalSettingsConfiguration() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "configuration/global-settings-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from global-settings-test.yaml");
        
        // Verify metadata block
        verifyBlockExists("metadata", 1, "Should have metadata block");
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Import YAML with metadata edge cases")
    void test08_ImportMetadataEdgeCases() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "configuration/metadata-edge-cases-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from metadata-edge-cases-test.yaml");
    }

    @Test
    @Order(9)
    @DisplayName("Test 9: Import YAML with execution options")
    void test09_ImportExecutionOptions() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "configuration/execution-options-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from execution-options-test.yaml");
    }

    // ==========================
    // SCENARIO PATTERNS (Tests 10-12)
    // ==========================

    @Test
    @Order(10)
    @DisplayName("Test 10: Import YAML with rule-ref routing scenarios")
    void test10_ImportRuleRefRouting() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "scenario/rule-ref-routing-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from rule-ref-routing-test.yaml");
    }

    @Test
    @Order(11)
    @DisplayName("Test 11: Import YAML with hybrid classification scenarios")
    void test11_ImportHybridClassification() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "scenario/hybrid-classification-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from hybrid-classification-test.yaml");
    }

    @Test
    @Order(12)
    @DisplayName("Test 12: Import YAML with nested scenarios")
    void test12_ImportNestedScenarios() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "scenario/nested-scenarios-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from nested-scenarios-test.yaml");
    }

    // ==========================
    // COMPONENT CONFIGURATIONS (Tests 13-15)
    // ==========================

    @Test
    @Order(13)
    @DisplayName("Test 13: Import YAML with component groups")
    void test13_ImportComponentGroups() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "components/component-groups-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from component-groups-test.yaml");
    }

    @Test
    @Order(14)
    @DisplayName("Test 14: Import YAML with execution order")
    void test14_ImportExecutionOrder() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "components/execution-order-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from execution-order-test.yaml");
    }

    @Test
    @Order(15)
    @DisplayName("Test 15: Import YAML with component dependencies")
    void test15_ImportComponentDependencies() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "components/component-dependencies-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from component-dependencies-test.yaml");
    }

    // ==========================
    // ERROR RECOVERY (Tests 16-18)
    // ==========================

    @Test
    @Order(16)
    @DisplayName("Test 16: Import YAML with notification policies")
    void test16_ImportNotificationPolicies() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "error-recovery/notification-policies-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from notification-policies-test.yaml");
    }

    @Test
    @Order(17)
    @DisplayName("Test 17: Import YAML with custom handlers")
    void test17_ImportCustomHandlers() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "error-recovery/custom-handlers-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from custom-handlers-test.yaml");
    }

    @Test
    @Order(18)
    @DisplayName("Test 18: Import YAML with retry strategies")
    void test18_ImportRetryStrategies() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "error-recovery/retry-strategies-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from retry-strategies-test.yaml");
    }

    // ==========================
    // DATA SOURCES (Tests 19-20)
    // ==========================

    @Test
    @Order(19)
    @DisplayName("Test 19: Import YAML with multiple data source refs")
    void test19_ImportMultipleDataSourceRefs() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "data-sources/multiple-refs-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from multiple-refs-test.yaml");
    }

    @Test
    @Order(20)
    @DisplayName("Test 20: Import YAML with conditional data source enablement")
    void test20_ImportConditionalDataSourceEnablement() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "data-sources/conditional-enablement-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from conditional-enablement-test.yaml");
    }

    // ==========================
    // RULE VARIATIONS (Tests 21-23)
    // ==========================

    @Test
    @Order(21)
    @DisplayName("Test 21: Import YAML with advanced rule groups")
    void test21_ImportAdvancedRuleGroups() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "rules/advanced-rule-groups-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from advanced-rule-groups-test.yaml");
        
        // Verify rule blocks exist
        List<String> ruleBlocks = getBlocksByType("apex_rule");
        assertTrue(ruleBlocks.size() > 0, "Should have rule blocks");
    }

    @Test
    @Order(22)
    @DisplayName("Test 22: Import YAML with inline rules")
    void test22_ImportInlineRules() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "rules/inline-rules-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from inline-rules-test.yaml");
    }

    @Test
    @Order(23)
    @DisplayName("Test 23: Import YAML with conditional rules")
    void test23_ImportConditionalRules() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "rules/conditional-rules-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from conditional-rules-test.yaml");
    }

    // ==========================
    // TRANSFORMATIONS (Tests 24-26)
    // ==========================

    @Test
    @Order(24)
    @DisplayName("Test 24: Import YAML with field mapping transformations")
    void test24_ImportFieldMapping() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "transformations/field-mapping-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from field-mapping-test.yaml");
    }

    @Test
    @Order(25)
    @DisplayName("Test 25: Import YAML with data type conversion")
    void test25_ImportDataTypeConversion() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "transformations/data-type-conversion-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from data-type-conversion-test.yaml");
    }

    @Test
    @Order(26)
    @DisplayName("Test 26: Import YAML with custom expressions")
    void test26_ImportCustomExpressions() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "transformations/custom-expressions-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from custom-expressions-test.yaml");
    }

    // ==========================
    // ENRICHMENTS (Tests 27-29)
    // ==========================

    @Test
    @Order(27)
    @DisplayName("Test 27: Import YAML with composite enrichments")
    void test27_ImportCompositeEnrichments() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "enrichments/composite-enrichments-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from composite-enrichments-test.yaml");
    }

    @Test
    @Order(28)
    @DisplayName("Test 28: Import YAML with conditional enrichment")
    void test28_ImportConditionalEnrichment() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "enrichments/conditional-enrichment-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from conditional-enrichment-test.yaml");
    }

    @Test
    @Order(29)
    @DisplayName("Test 29: Import YAML with async patterns")
    void test29_ImportAsyncPatterns() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "enrichments/async-patterns-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from async-patterns-test.yaml");
    }

    // ==========================
    // LOOKUP PATTERNS (Tests 30-33)
    // ==========================

    @Test
    @Order(30)
    @DisplayName("Test 30: Import YAML with multi-key lookups")
    void test30_ImportMultiKeyLookups() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "lookups/multi-key-lookups-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from multi-key-lookups-test.yaml");
    }

    @Test
    @Order(31)
    @DisplayName("Test 31: Import YAML with fallback values")
    void test31_ImportFallbackValues() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "lookups/fallback-values-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from fallback-values-test.yaml");
    }

    @Test
    @Order(32)
    @DisplayName("Test 32: Import YAML with cache configuration")
    void test32_ImportCacheConfiguration() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "lookups/cache-configuration-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from cache-configuration-test.yaml");
    }

    @Test
    @Order(33)
    @DisplayName("Test 33: Import YAML with dynamic lookups")
    void test33_ImportDynamicLookups() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "lookups/dynamic-lookups-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from dynamic-lookups-test.yaml");
    }

    // ==========================
    // TEMPLATE USAGE (Tests 34-36)
    // ==========================

    @Test
    @Order(34)
    @DisplayName("Test 34: Import YAML with reusable blocks")
    void test34_ImportReusableBlocks() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "templates/reusable-blocks-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from reusable-blocks-test.yaml");
    }

    @Test
    @Order(35)
    @DisplayName("Test 35: Import YAML with parameterized templates")
    void test35_ImportParameterizedTemplates() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "templates/parameterized-templates-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from parameterized-templates-test.yaml");
    }

    @Test
    @Order(36)
    @DisplayName("Test 36: Import YAML with template inheritance")
    void test36_ImportTemplateInheritance() throws IOException {
        String yamlPath = EXAMPLES_BASE_PATH + "templates/template-inheritance-test.yaml";
        String yamlContent = loadYamlFile(yamlPath);
        
        driver.get(baseUrl + "/playground");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        waitForBlocksToRender();
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from template-inheritance-test.yaml");
    }
}
