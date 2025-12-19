package dev.mars.apex.playground.ui;

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

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium UI tests for YAML import validation in the APEX Blockly visual editor.
 * Tests the 6 validation scenarios documented in APEX_BLOCKS_PROTOTYPE_GUIDE.md.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-12-19
 * @version 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class YamlImportValidationUITest extends BaseYamlImportSeleniumTest {

    /**
     * Test 1: Basic Rules Configuration
     * File: examples/validation/basic-rules-test.yaml
     * Expected: 3 Rule blocks with different severities (ERROR, WARNING, INFO)
     */
    @Test
    @Order(1)
    @DisplayName("Test 1: Import Basic Rules Configuration - Should create 3 Rule blocks")
    void testImportBasicRulesConfiguration() throws IOException {
        // Given
        String yamlContent = loadYamlFile("examples/validation/basic-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(yamlContent);

        // Then
        int blockCount = getBlockCount();
        assertTrue(blockCount >= 4, "Should have at least 4 blocks (1 Configuration + 3 Rules), found: " + blockCount);
        
        // Verify rule blocks exist
        verifyBlockExists("apex_rule", 3, "Should have 3 Rule blocks");
        
        // Verify configuration block exists
        verifyBlockExists("apex_configuration", 1, "Should have 1 Configuration block");
        
        // Verify rule severities
        verifySeverityValues(List.of("ERROR", "WARNING", "INFO"));
    }

    /**
     * Test 2: Lookup Enrichment
     * File: examples/lookup/lookup-enrichment-test.yaml
     * Expected: Lookup Enrichment block with nested dataset and 2 result mappings
     */
    @Test
    @Order(2)
    @DisplayName("Test 2: Import Lookup Enrichment - Should create Lookup Enrichment with mappings")
    void testImportLookupEnrichment() throws IOException {
        // Given
        String yamlContent = loadYamlFile("examples/lookup/lookup-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(yamlContent);

        // Then
        verifyBlockExists("apex_lookup_enrichment", 1, "Should have 1 Lookup Enrichment block");
        verifyBlockExists("apex_lookup_dataset", 1, "Should have 1 Lookup Dataset block");
        verifyBlockExists("apex_result_mapping", 2, "Should have 2 Result Mapping blocks");
        
        // Verify lookup ID
        String lookupId = getBlockFieldValue("apex_lookup_enrichment", "ID");
        assertEquals("currency-lookup", lookupId, "Lookup enrichment ID should be 'currency-lookup'");
    }

    /**
     * Test 3: Calculation Enrichment
     * File: examples/enrichment/calculation-enrichment-test.yaml
     * Expected: Calculation Enrichment block with SpEL expression
     */
    @Test
    @Order(3)
    @DisplayName("Test 3: Import Calculation Enrichment - Should create Calculation with expression")
    void testImportCalculationEnrichment() throws IOException {
        // Given
        String yamlContent = loadYamlFile("examples/enrichment/calculation-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(yamlContent);

        // Then
        verifyBlockExists("apex_calculation_enrichment", 1, "Should have 1 Calculation Enrichment block");
        
        // Verify calculation ID
        String calcId = getBlockFieldValue("apex_calculation_enrichment", "ID");
        assertEquals("total-value-calc", calcId, "Calculation enrichment ID should be 'total-value-calc'");
        
        // Verify source fields
        int sourceFieldCount = countNestedBlocks("apex_calculation_enrichment", "SOURCE_FIELDS");
        assertEquals(2, sourceFieldCount, "Should have 2 source fields (#quantity, #price)");
    }

    /**
     * Test 4: Scenario with Classification
     * File: examples/conditional/scenario-classification-test.yaml
     * Expected: Scenario block with 3 classification entries (EQUITY, BOND, DERIVATIVE)
     */
    @Test
    @Order(4)
    @DisplayName("Test 4: Import Scenario with Classification - Should create Scenario with 3 entries")
    void testImportScenarioWithClassification() throws IOException {
        // Given
        String yamlContent = loadYamlFile("examples/conditional/scenario-classification-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(yamlContent);

        // Then
        verifyBlockExists("apex_scenario", 1, "Should have 1 Scenario block");
        verifyBlockExists("apex_classification", 1, "Should have 1 Classification block");
        verifyBlockExists("apex_classification_entry", 3, "Should have 3 Classification Entry blocks");
        
        // Verify scenario name
        String scenarioName = getBlockFieldValue("apex_scenario", "NAME");
        assertEquals("Trade Classification", scenarioName, "Scenario name should be 'Trade Classification'");
    }

    /**
     * Test 5: Error Recovery with Severity Policies
     * File: examples/validation/error-recovery-test.yaml
     * Expected: Error Recovery block with 2 severity policies (ERROR, WARNING)
     */
    @Test
    @Order(5)
    @DisplayName("Test 5: Import Error Recovery - Should create Error Recovery with 2 policies")
    void testImportErrorRecovery() throws IOException {
        // Given
        String yamlContent = loadYamlFile("examples/validation/error-recovery-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(yamlContent);

        // Then
        verifyBlockExists("apex_error_recovery", 1, "Should have 1 Error Recovery block");
        verifyBlockExists("apex_severity_policy", 2, "Should have 2 Severity Policy blocks");
        
        // Verify default action
        String defaultAction = getBlockFieldValue("apex_error_recovery", "DEFAULT_ACTION");
        assertEquals("CONTINUE", defaultAction, "Default action should be 'CONTINUE'");
    }

    /**
     * Test 6: Component with External Data Source Reference
     * File: examples/basic/component-datasource-test.yaml
     * Expected: Data Source Reference + Component with nested rule
     */
    @Test
    @Order(6)
    @DisplayName("Test 6: Import Component with Data Source - Should create Component and Data Source Ref")
    void testImportComponentWithDataSource() throws IOException {
        // Given
        String yamlContent = loadYamlFile("examples/basic/component-datasource-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(yamlContent);

        // Then
        verifyBlockExists("apex_data_source_ref", 1, "Should have 1 Data Source Reference block");
        verifyBlockExists("apex_component", 1, "Should have 1 Component block");
        
        // Verify data source ref name
        String dataSourceName = getBlockFieldValue("apex_data_source_ref", "NAME");
        assertEquals("trade-database", dataSourceName, "Data source name should be 'trade-database'");
        
        // Verify component has nested rule
        int nestedRules = countNestedBlocks("apex_component", "RULES");
        assertTrue(nestedRules >= 1, "Component should have at least 1 nested rule");
    }

    /**
     * Round-trip Test 1: Basic Rules Configuration
     */
    @Test
    @Order(7)
    @DisplayName("Round-trip Test 1: Basic Rules - Import → Export → Verify")
    void testRoundTripBasicRules() throws IOException {
        // Given
        String originalYaml = loadYamlFile("examples/validation/basic-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        // Then - Verify semantic equivalence
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        
        // Verify metadata
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
        assertTrue(exportedYaml.contains("name:") && exportedYaml.contains("Basic Rules Test"), "Should preserve metadata name");
        assertTrue(exportedYaml.contains("version:") && exportedYaml.contains("1.0"), "Should preserve version");
        
        // Verify rules structure
        assertTrue(exportedYaml.contains("rules:"), "Should contain rules section");
        assertTrue(exportedYaml.contains("rule-001"), "Should contain rule-001");
        assertTrue(exportedYaml.contains("rule-002"), "Should contain rule-002");
        assertTrue(exportedYaml.contains("rule-003"), "Should contain rule-003");
        
        // Verify rule conditions
        assertTrue(exportedYaml.contains("#cusip != null"), "Should preserve condition for rule-001");
        assertTrue(exportedYaml.contains("#quantity > 0"), "Should preserve condition for rule-002");
        assertTrue(exportedYaml.contains("#price >= 0.01"), "Should preserve condition for rule-003");
        
        // Verify severities
        assertTrue(exportedYaml.contains("severity:") && exportedYaml.contains("ERROR"), "Should contain ERROR severity");
        assertTrue(exportedYaml.contains("WARNING"), "Should contain WARNING severity");
        assertTrue(exportedYaml.contains("INFO"), "Should contain INFO severity");
    }

    /**
     * Round-trip Test 2: Lookup Enrichment
     */
    @Test
    @Order(8)
    @DisplayName("Round-trip Test 2: Lookup Enrichment - Import → Export → Verify")
    void testRoundTripLookupEnrichment() throws IOException {
        // Given
        String originalYaml = loadYamlFile("examples/lookup/lookup-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        // Then
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        
        // Verify enrichments structure
        assertTrue(exportedYaml.contains("enrichments:"), "Should contain enrichments section");
        assertTrue(exportedYaml.contains("id:") && exportedYaml.contains("currency-lookup"), "Should preserve enrichment ID");
        assertTrue(exportedYaml.contains("type:") && exportedYaml.contains("lookup-enrichment"), "Should preserve enrichment type");
        
        // Verify source field
        assertTrue(exportedYaml.contains("source-field:") && exportedYaml.contains("#currencyCode"), "Should preserve source field");
        
        // Verify lookup configuration
        assertTrue(exportedYaml.contains("lookup-config:"), "Should contain lookup-config");
        assertTrue(exportedYaml.contains("lookup-dataset:"), "Should contain lookup-dataset");
        assertTrue(exportedYaml.contains("yaml-data"), "Should preserve dataset type");
        assertTrue(exportedYaml.contains("lookup-key-field:") && exportedYaml.contains("code"), "Should preserve lookup key field");
        
        // Verify result mappings
        assertTrue(exportedYaml.contains("result-mapping:"), "Should contain result-mapping");
        assertTrue(exportedYaml.contains("currencyName"), "Should preserve currencyName target field");
        assertTrue(exportedYaml.contains("currencySymbol"), "Should preserve currencySymbol target field");
    }

    /**
     * Round-trip Test 3: Calculation Enrichment
     */
    @Test
    @Order(9)
    @DisplayName("Round-trip Test 3: Calculation Enrichment - Import → Export → Verify")
    void testRoundTripCalculationEnrichment() throws IOException {
        // Given
        String originalYaml = loadYamlFile("examples/enrichment/calculation-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        // Then
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        
        // Verify enrichment structure
        assertTrue(exportedYaml.contains("enrichments:"), "Should contain enrichments section");
        assertTrue(exportedYaml.contains("id:") && exportedYaml.contains("total-value-calc"), "Should preserve enrichment ID");
        assertTrue(exportedYaml.contains("type:") && exportedYaml.contains("calculation-enrichment"), "Should preserve enrichment type");
        
        // Verify source fields
        assertTrue(exportedYaml.contains("source-fields:"), "Should contain source-fields");
        assertTrue(exportedYaml.contains("#quantity"), "Should preserve #quantity source field");
        assertTrue(exportedYaml.contains("#price"), "Should preserve #price source field");
        
        // Verify calculation
        assertTrue(exportedYaml.contains("calculation:"), "Should contain calculation section");
        assertTrue(exportedYaml.contains("expression:") && exportedYaml.contains("#quantity * #price"), "Should preserve calculation expression");
        assertTrue(exportedYaml.contains("result-field:") && exportedYaml.contains("totalValue"), "Should preserve result field");
    }

    /**
     * Round-trip Test 4: Scenario with Classification
     */
    @Test
    @Order(10)
    @DisplayName("Round-trip Test 4: Scenario Classification - Import → Export → Verify")
    void testRoundTripScenarioClassification() throws IOException {
        // Given
        String originalYaml = loadYamlFile("examples/conditional/scenario-classification-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        // Then
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        
        // Verify scenario structure
        assertTrue(exportedYaml.contains("scenario:"), "Should contain scenario section");
        assertTrue(exportedYaml.contains("name:") && exportedYaml.contains("Trade Classification"), "Should preserve scenario name");
        
        // Verify classification
        assertTrue(exportedYaml.contains("classification:"), "Should contain classification section");
        assertTrue(exportedYaml.contains("field-name:") && exportedYaml.contains("tradeType"), "Should preserve field-name");
        
        // Verify classifications
        assertTrue(exportedYaml.contains("classifications:"), "Should contain classifications list");
        assertTrue(exportedYaml.contains("EQUITY"), "Should preserve EQUITY classification");
        assertTrue(exportedYaml.contains("BOND"), "Should preserve BOND classification");
        assertTrue(exportedYaml.contains("DERIVATIVE"), "Should preserve DERIVATIVE classification");
        
        // Verify rules references
        assertTrue(exportedYaml.contains("rules-ref:") && exportedYaml.contains("equity-rules.yaml"), "Should preserve equity-rules.yaml reference");
        assertTrue(exportedYaml.contains("bond-rules.yaml"), "Should preserve bond-rules.yaml reference");
        assertTrue(exportedYaml.contains("derivative-rules.yaml"), "Should preserve derivative-rules.yaml reference");
    }

    /**
     * Round-trip Test 5: Error Recovery with Severity Policies
     */
    @Test
    @Order(11)
    @DisplayName("Round-trip Test 5: Error Recovery - Import → Export → Verify")
    void testRoundTripErrorRecovery() throws IOException {
        // Given
        String originalYaml = loadYamlFile("examples/validation/error-recovery-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        // Then
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        
        // Verify error recovery structure
        assertTrue(exportedYaml.contains("error-recovery:"), "Should contain error-recovery section");
        assertTrue(exportedYaml.contains("default-action:") && exportedYaml.contains("CONTINUE"), "Should preserve default-action");
        
        // Verify severity policies
        assertTrue(exportedYaml.contains("severity-policies:"), "Should contain severity-policies");
        
        // Verify ERROR policy
        assertTrue(exportedYaml.contains("severity:") && exportedYaml.contains("ERROR"), "Should contain ERROR severity policy");
        assertTrue(exportedYaml.contains("action:") && exportedYaml.contains("FAIL"), "Should preserve FAIL action for ERROR");
        assertTrue(exportedYaml.contains("max-errors:") && exportedYaml.contains("0"), "Should preserve max-errors: 0");
        
        // Verify WARNING policy
        assertTrue(exportedYaml.contains("WARNING"), "Should contain WARNING severity policy");
        assertTrue(exportedYaml.contains("max-errors:") && exportedYaml.contains("10"), "Should preserve max-errors: 10");
        assertTrue(exportedYaml.contains("log-level:") && exportedYaml.contains("WARN"), "Should preserve log-level: WARN");
    }

    /**
     * Round-trip Test 6: Component with External Data Source Reference
     */
    @Test
    @Order(12)
    @DisplayName("Round-trip Test 6: Component with Data Source - Import → Export → Verify")
    void testRoundTripComponentWithDataSource() throws IOException {
        // Given
        String originalYaml = loadYamlFile("examples/basic/component-datasource-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        // When
        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        // Then
        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        
        // Verify data source references
        assertTrue(exportedYaml.contains("data-source-refs:"), "Should contain data-source-refs section");
        assertTrue(exportedYaml.contains("name:") && exportedYaml.contains("trade-database"), "Should preserve data source name");
        assertTrue(exportedYaml.contains("source:") && exportedYaml.contains("data-sources/trade-db.yaml"), "Should preserve source path");
        assertTrue(exportedYaml.contains("enabled:") && exportedYaml.contains("true"), "Should preserve enabled: true");
        
        // Verify components
        assertTrue(exportedYaml.contains("components:"), "Should contain components section");
        assertTrue(exportedYaml.contains("id:") && exportedYaml.contains("trade-validator"), "Should preserve component ID");
        assertTrue(exportedYaml.contains("Trade Validation Component"), "Should preserve component name");
        assertTrue(exportedYaml.contains("Validates trade data"), "Should preserve component description");
        
        // Verify nested rule
        assertTrue(exportedYaml.contains("rules:"), "Component should contain rules");
        assertTrue(exportedYaml.contains("trade-001"), "Should preserve nested rule ID");
        assertTrue(exportedYaml.contains("#tradeId != null"), "Should preserve nested rule condition");
        assertTrue(exportedYaml.contains("Trade ID required"), "Should preserve nested rule message");
    }

    // Note: All helper methods have been moved to BaseYamlImportSeleniumTest
}
