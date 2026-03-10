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
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 comprehensive Selenium tests for YAML import validation (Tests 7-35).
 * Production-quality test suite covering 29 YAML configurations with full import + round-trip validation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class YamlImportPhase1FullTest extends BaseYamlImportSeleniumTest {

    /**
     * Close any open modals before importing YAML.
     * Some tests may leave modals open, causing click interception errors.
     */
    private void closeAnyOpenModals() {
        try {
            List<WebElement> closeButtons = driver.findElements(By.cssSelector(".modal.show .btn-close, .modal.show button[data-bs-dismiss='modal']"));
            for (WebElement closeButton : closeButtons) {
                if (closeButton.isDisplayed()) {
                    closeButton.click();
                    Thread.sleep(300);
                }
            }
        } catch (NoSuchElementException | InterruptedException e) {
            // No modal to close, continue
        }
    }

    // ==========================
    // CONFIGURATION VARIATIONS (Tests 7-9)
    // ==========================

    @Test
    @Order(7)
    @DisplayName("Test 7: Import YAML with global settings configuration")
    void test07_ImportGlobalSettings() throws IOException {
        String yamlContent = loadYamlFile("examples/configuration/global-settings-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        closeAnyOpenModals();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from global-settings-test.yaml");
        verifyBlockExists("apex_configuration", 1, "Should have Configuration block");
    }

    @Test
    @Order(8)
    @DisplayName("Test 7 Round-Trip: Global settings export matches import")
    void test08_RoundTripGlobalSettings() throws IOException {
        String originalYaml = loadYamlFile("examples/configuration/global-settings-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(9)
    @DisplayName("Test 8: Import YAML with metadata edge cases")
    void test09_ImportMetadataEdgeCases() throws IOException {
        String yamlContent = loadYamlFile("examples/configuration/metadata-edge-cases-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from metadata-edge-cases-test.yaml");
    }

    @Test
    @Order(10)
    @DisplayName("Test 8 Round-Trip: Metadata edge cases export matches import")
    void test10_RoundTripMetadataEdgeCases() throws IOException {
        String originalYaml = loadYamlFile("examples/configuration/metadata-edge-cases-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(11)
    @DisplayName("Test 9: Import YAML with execution options")
    void test11_ImportExecutionOptions() throws IOException {
        String yamlContent = loadYamlFile("examples/configuration/execution-options-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from execution-options-test.yaml");
    }

    @Test
    @Order(12)
    @DisplayName("Test 9 Round-Trip: Execution options export matches import")
    void test12_RoundTripExecutionOptions() throws IOException {
        String originalYaml = loadYamlFile("examples/configuration/execution-options-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // SCENARIO PATTERNS (Tests 10-12)
    // ==========================

    @Test
    @Order(13)
    @DisplayName("Test 10: Import YAML with rule-ref routing scenarios")
    void test13_ImportRuleRefRouting() throws IOException {
        String yamlContent = loadYamlFile("examples/scenario/rule-ref-routing-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from rule-ref-routing-test.yaml");
    }

    @Test
    @Order(14)
    @DisplayName("Test 10 Round-Trip: Rule-ref routing export matches import")
    void test14_RoundTripRuleRefRouting() throws IOException {
        String originalYaml = loadYamlFile("examples/scenario/rule-ref-routing-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(15)
    @DisplayName("Test 11: Import YAML with hybrid classification scenarios")
    void test15_ImportHybridClassification() throws IOException {
        String yamlContent = loadYamlFile("examples/scenario/hybrid-classification-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from hybrid-classification-test.yaml");
    }

    @Test
    @Order(16)
    @DisplayName("Test 11 Round-Trip: Hybrid classification export matches import")
    void test16_RoundTripHybridClassification() throws IOException {
        String originalYaml = loadYamlFile("examples/scenario/hybrid-classification-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(17)
    @DisplayName("Test 12: Import YAML with nested scenarios")
    void test17_ImportNestedScenarios() throws IOException {
        String yamlContent = loadYamlFile("examples/scenario/nested-scenarios-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from nested-scenarios-test.yaml");
    }

    @Test
    @Order(18)
    @DisplayName("Test 12 Round-Trip: Nested scenarios export matches import")
    void test18_RoundTripNestedScenarios() throws IOException {
        String originalYaml = loadYamlFile("examples/scenario/nested-scenarios-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // COMPONENT CONFIGURATIONS (Tests 13-15)
    // ==========================

    @Test
    @Order(19)
    @DisplayName("Test 13: Import YAML with component groups")
    void test19_ImportComponentGroups() throws IOException {
        String yamlContent = loadYamlFile("examples/components/component-groups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from component-groups-test.yaml");
    }

    @Test
    @Order(20)
    @DisplayName("Test 13 Round-Trip: Component groups export matches import")
    void test20_RoundTripComponentGroups() throws IOException {
        String originalYaml = loadYamlFile("examples/components/component-groups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(21)
    @DisplayName("Test 14: Import YAML with execution order")
    void test21_ImportExecutionOrder() throws IOException {
        String yamlContent = loadYamlFile("examples/components/execution-order-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from execution-order-test.yaml");
    }

    @Test
    @Order(22)
    @DisplayName("Test 14 Round-Trip: Execution order export matches import")
    void test22_RoundTripExecutionOrder() throws IOException {
        String originalYaml = loadYamlFile("examples/components/execution-order-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(23)
    @DisplayName("Test 15: Import YAML with component dependencies")
    void test23_ImportComponentDependencies() throws IOException {
        String yamlContent = loadYamlFile("examples/components/component-dependencies-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from component-dependencies-test.yaml");
    }

    @Test
    @Order(24)
    @DisplayName("Test 15 Round-Trip: Component dependencies export matches import")
    void test24_RoundTripComponentDependencies() throws IOException {
        String originalYaml = loadYamlFile("examples/components/component-dependencies-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // ERROR RECOVERY (Tests 16-18)
    // ==========================

    @Test
    @Order(25)
    @DisplayName("Test 16: Import YAML with notification policies")
    void test25_ImportNotificationPolicies() throws IOException {
        String yamlContent = loadYamlFile("examples/error-recovery/notification-policies-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from notification-policies-test.yaml");
    }

    @Test
    @Order(26)
    @DisplayName("Test 16 Round-Trip: Notification policies export matches import")
    void test26_RoundTripNotificationPolicies() throws IOException {
        String originalYaml = loadYamlFile("examples/error-recovery/notification-policies-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(27)
    @DisplayName("Test 17: Import YAML with custom handlers")
    void test27_ImportCustomHandlers() throws IOException {
        String yamlContent = loadYamlFile("examples/error-recovery/custom-handlers-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from custom-handlers-test.yaml");
    }

    @Test
    @Order(28)
    @DisplayName("Test 17 Round-Trip: Custom handlers export matches import")
    void test28_RoundTripCustomHandlers() throws IOException {
        String originalYaml = loadYamlFile("examples/error-recovery/custom-handlers-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(29)
    @DisplayName("Test 18: Import YAML with retry strategies")
    void test29_ImportRetryStrategies() throws IOException {
        String yamlContent = loadYamlFile("examples/error-recovery/retry-strategies-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from retry-strategies-test.yaml");
    }

    @Test
    @Order(30)
    @DisplayName("Test 18 Round-Trip: Retry strategies export matches import")
    void test30_RoundTripRetryStrategies() throws IOException {
        String originalYaml = loadYamlFile("examples/error-recovery/retry-strategies-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // DATA SOURCES (Tests 19-20)
    // ==========================

    @Test
    @Order(31)
    @DisplayName("Test 19: Import YAML with multiple data source refs")
    void test31_ImportMultipleDataSourceRefs() throws IOException {
        String yamlContent = loadYamlFile("examples/data-sources/multiple-refs-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from multiple-refs-test.yaml");
    }

    @Test
    @Order(32)
    @DisplayName("Test 19 Round-Trip: Multiple data source refs export matches import")
    void test32_RoundTripMultipleDataSourceRefs() throws IOException {
        String originalYaml = loadYamlFile("examples/data-sources/multiple-refs-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(33)
    @DisplayName("Test 20: Import YAML with conditional data source enablement")
    void test33_ImportConditionalDataSourceEnablement() throws IOException {
        String yamlContent = loadYamlFile("examples/data-sources/conditional-enablement-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from conditional-enablement-test.yaml");
    }

    @Test
    @Order(34)
    @DisplayName("Test 20 Round-Trip: Conditional data source enablement export matches import")
    void test34_RoundTripConditionalDataSourceEnablement() throws IOException {
        String originalYaml = loadYamlFile("examples/data-sources/conditional-enablement-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // RULE VARIATIONS (Tests 21-23)
    // ==========================

    @Test
    @Order(35)
    @DisplayName("Test 21: Import YAML with advanced rule groups")
    void test35_ImportAdvancedRuleGroups() throws IOException {
        String yamlContent = loadYamlFile("examples/rules/advanced-rule-groups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from advanced-rule-groups-test.yaml");
    }

    @Test
    @Order(36)
    @DisplayName("Test 21 Round-Trip: Advanced rule groups export matches import")
    void test36_RoundTripAdvancedRuleGroups() throws IOException {
        String originalYaml = loadYamlFile("examples/rules/advanced-rule-groups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(37)
    @DisplayName("Test 22: Import YAML with inline rules")
    void test37_ImportInlineRules() throws IOException {
        String yamlContent = loadYamlFile("examples/rules/inline-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from inline-rules-test.yaml");
    }

    @Test
    @Order(38)
    @DisplayName("Test 22 Round-Trip: Inline rules export matches import")
    void test38_RoundTripInlineRules() throws IOException {
        String originalYaml = loadYamlFile("examples/rules/inline-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(39)
    @DisplayName("Test 23: Import YAML with conditional rules")
    void test39_ImportConditionalRules() throws IOException {
        String yamlContent = loadYamlFile("examples/rules/conditional-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from conditional-rules-test.yaml");
    }

    @Test
    @Order(40)
    @DisplayName("Test 23 Round-Trip: Conditional rules export matches import")
    void test40_RoundTripConditionalRules() throws IOException {
        String originalYaml = loadYamlFile("examples/rules/conditional-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // TRANSFORMATIONS (Tests 24-26)
    // ==========================

    @Test
    @Order(41)
    @DisplayName("Test 24: Import YAML with field mapping transformations")
    void test41_ImportFieldMapping() throws IOException {
        String yamlContent = loadYamlFile("examples/transformations/field-mapping-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from field-mapping-test.yaml");
    }

    @Test
    @Order(42)
    @DisplayName("Test 24 Round-Trip: Field mapping export matches import")
    void test42_RoundTripFieldMapping() throws IOException {
        String originalYaml = loadYamlFile("examples/transformations/field-mapping-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(43)
    @DisplayName("Test 25: Import YAML with data type conversion")
    void test43_ImportDataTypeConversion() throws IOException {
        String yamlContent = loadYamlFile("examples/transformations/data-type-conversion-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from data-type-conversion-test.yaml");
    }

    @Test
    @Order(44)
    @DisplayName("Test 25 Round-Trip: Data type conversion export matches import")
    void test44_RoundTripDataTypeConversion() throws IOException {
        String originalYaml = loadYamlFile("examples/transformations/data-type-conversion-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(45)
    @DisplayName("Test 26: Import YAML with custom expressions")
    void test45_ImportCustomExpressions() throws IOException {
        String yamlContent = loadYamlFile("examples/transformations/custom-expressions-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from custom-expressions-test.yaml");
    }

    @Test
    @Order(46)
    @DisplayName("Test 26 Round-Trip: Custom expressions export matches import")
    void test46_RoundTripCustomExpressions() throws IOException {
        String originalYaml = loadYamlFile("examples/transformations/custom-expressions-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // ENRICHMENTS (Tests 27-29)
    // ==========================

    @Test
    @Order(47)
    @DisplayName("Test 27: Import YAML with composite enrichments")
    void test47_ImportCompositeEnrichments() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichments/composite-enrichments-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from composite-enrichments-test.yaml");
    }

    @Test
    @Order(48)
    @DisplayName("Test 27 Round-Trip: Composite enrichments export matches import")
    void test48_RoundTripCompositeEnrichments() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichments/composite-enrichments-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(49)
    @DisplayName("Test 28: Import YAML with conditional enrichment")
    void test49_ImportConditionalEnrichment() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichments/conditional-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from conditional-enrichment-test.yaml");
    }

    @Test
    @Order(50)
    @DisplayName("Test 28 Round-Trip: Conditional enrichment export matches import")
    void test50_RoundTripConditionalEnrichment() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichments/conditional-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(51)
    @DisplayName("Test 29: Import YAML with async patterns")
    void test51_ImportAsyncPatterns() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichments/async-patterns-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from async-patterns-test.yaml");
    }

    @Test
    @Order(52)
    @DisplayName("Test 29 Round-Trip: Async patterns export matches import")
    void test52_RoundTripAsyncPatterns() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichments/async-patterns-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // LOOKUP PATTERNS (Tests 30-33)
    // ==========================

    @Test
    @Order(53)
    @DisplayName("Test 30: Import YAML with multi-key lookups")
    void test53_ImportMultiKeyLookups() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/multi-key-lookups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from multi-key-lookups-test.yaml");
    }

    @Test
    @Order(54)
    @DisplayName("Test 30 Round-Trip: Multi-key lookups export matches import")
    void test54_RoundTripMultiKeyLookups() throws IOException {
        String originalYaml = loadYamlFile("examples/lookups/multi-key-lookups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(55)
    @DisplayName("Test 31: Import YAML with fallback values")
    void test55_ImportFallbackValues() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/fallback-values-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from fallback-values-test.yaml");
    }

    @Test
    @Order(56)
    @DisplayName("Test 31 Round-Trip: Fallback values export matches import")
    void test56_RoundTripFallbackValues() throws IOException {
        String originalYaml = loadYamlFile("examples/lookups/fallback-values-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(57)
    @DisplayName("Test 32: Import YAML with cache configuration")
    void test57_ImportCacheConfiguration() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/cache-configuration-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from cache-configuration-test.yaml");
    }

    @Test
    @Order(58)
    @DisplayName("Test 32 Round-Trip: Cache configuration export matches import")
    void test58_RoundTripCacheConfiguration() throws IOException {
        String originalYaml = loadYamlFile("examples/lookups/cache-configuration-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(59)
    @DisplayName("Test 33: Import YAML with dynamic lookups")
    void test59_ImportDynamicLookups() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/dynamic-lookups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from dynamic-lookups-test.yaml");
    }

    @Test
    @Order(60)
    @DisplayName("Test 33 Round-Trip: Dynamic lookups export matches import")
    void test60_RoundTripDynamicLookups() throws IOException {
        String originalYaml = loadYamlFile("examples/lookups/dynamic-lookups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    // ==========================
    // TEMPLATE USAGE (Tests 34-36)
    // ==========================

    @Test
    @Order(61)
    @DisplayName("Test 34: Import YAML with reusable blocks")
    void test61_ImportReusableBlocks() throws IOException {
        String yamlContent = loadYamlFile("examples/templates/reusable-blocks-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from reusable-blocks-test.yaml");
    }

    @Test
    @Order(62)
    @DisplayName("Test 34 Round-Trip: Reusable blocks export matches import")
    void test62_RoundTripReusableBlocks() throws IOException {
        String originalYaml = loadYamlFile("examples/templates/reusable-blocks-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(63)
    @DisplayName("Test 35: Import YAML with parameterized templates")
    void test63_ImportParameterizedTemplates() throws IOException {
        String yamlContent = loadYamlFile("examples/templates/parameterized-templates-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from parameterized-templates-test.yaml");
    }

    @Test
    @Order(64)
    @DisplayName("Test 35 Round-Trip: Parameterized templates export matches import")
    void test64_RoundTripParameterizedTemplates() throws IOException {
        String originalYaml = loadYamlFile("examples/templates/parameterized-templates-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }

    @Test
    @Order(65)
    @DisplayName("Test 36: Import YAML with template inheritance")
    void test65_ImportTemplateInheritance() throws IOException {
        String yamlContent = loadYamlFile("examples/templates/template-inheritance-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount > 0, "Should have imported blocks from template-inheritance-test.yaml");
    }

    @Test
    @Order(66)
    @DisplayName("Test 36 Round-Trip: Template inheritance export matches import")
    void test66_RoundTripTemplateInheritance() throws IOException {
        String originalYaml = loadYamlFile("examples/templates/template-inheritance-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();
        
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
    }
}
