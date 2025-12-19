package dev.mars.apex.playground.ui;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive YAML Import Tests
 * 
 * Tests all YAML files in the examples directory that contain supported features:
 * - Rules (basic, conditional, inline, groups)
 * - Enrichments (calculation, lookup, field, constant)
 * - Lookups (multi-key, fallback, cache, dynamic)
 * - Transformations (field mapping, data type conversion, custom expressions)
 * - Rule Groups (inline, advanced)
 * - Enrichment Groups (composite, conditional, async)
 * 
 * Each test follows the pattern:
 * 1. Load YAML file
 * 2. Import to Blockly workspace
 * 3. Verify blocks created
 * 4. Export back to YAML
 * 5. Verify basic structure preserved
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class YamlImportComprehensiveTest extends BaseYamlImportSeleniumTest {

    // ========== BASIC RULES TESTS (Tests 1-5) ==========

    @Test
    @Order(1)
    @DisplayName("Test 1: Minimal Rule")
    void testMinimalRule() throws IOException {
        String yamlContent = loadYamlFile("examples/basic/minimal-rule.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        int blockCount = getBlockCount();
        assertTrue(blockCount >= 2, "Should have at least 2 blocks (1 Configuration + 1 Rule), found: " + blockCount);
        verifyBlockExists("apex_rule", 1, "Should have 1 Rule block");
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Simple Age Validation")
    void testSimpleAgeValidation() throws IOException {
        String yamlContent = loadYamlFile("examples/basic/simple-age-validation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule", 3, "Should have 3 Rule blocks");
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Nested Field Navigation")
    void testNestedFieldNavigation() throws IOException {
        String yamlContent = loadYamlFile("examples/basic/nested-field-navigation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        int blockCount = getBlockCount();
        assertTrue(blockCount >= 2, "Should have at least 2 blocks, found: " + blockCount);
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Quick Start")
    void testQuickStart() throws IOException {
        String yamlContent = loadYamlFile("examples/basic/quick-start.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        int blockCount = getBlockCount();
        assertTrue(blockCount >= 2, "Should have at least 2 blocks, found: " + blockCount);
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Value Threshold")
    void testValueThreshold() throws IOException {
        String yamlContent = loadYamlFile("examples/validation/value-threshold.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        int blockCount = getBlockCount();
        assertTrue(blockCount >= 2, "Should have at least 2 blocks, found: " + blockCount);
    }

    // ========== ENRICHMENT TESTS (Tests 6-15) ==========

    @Test
    @Order(6)
    @DisplayName("Test 6: Constant Value Enrichment")
    void testConstantValueEnrichment() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichment/constant-value-enrichment.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();
        
        importYamlContent(yamlContent);
        
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        int blockCount = getBlockCount();
        assertTrue(blockCount >= 2, "Should have at least 2 blocks, found: " + blockCount);
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Constant Values")
    void testConstantValues() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichment/constant-values.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Financial Validation")
    void testFinancialValidation() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichment/financial-validation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(9)
    @DisplayName("Test 9: Conditional Enrichment")
    void testConditionalEnrichment() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichments/conditional-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(10)
    @DisplayName("Test 10: Composite Enrichments")
    void testCompositeEnrichments() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichments/composite-enrichments-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    // ========== LOOKUP TESTS (Tests 11-15) ==========

    @Test
    @Order(11)
    @DisplayName("Test 11: Dynamic Pricing")
    void testDynamicPricing() throws IOException {
        String yamlContent = loadYamlFile("examples/lookup/dynamic-pricing.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(12)
    @DisplayName("Test 12: Math Calculations")
    void testMathCalculations() throws IOException {
        String yamlContent = loadYamlFile("examples/lookup/math-calculations.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(13)
    @DisplayName("Test 13: Cache Configuration")
    void testCacheConfiguration() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/cache-configuration-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(14)
    @DisplayName("Test 14: Fallback Values")
    void testFallbackValues() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/fallback-values-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(15)
    @DisplayName("Test 15: Multi-Key Lookups")
    void testMultiKeyLookups() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/multi-key-lookups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    // ========== TRANSFORMATION TESTS (Tests 16-20) ==========

    @Test
    @Order(16)
    @DisplayName("Test 16: Field Mapping")
    void testFieldMapping() throws IOException {
        String yamlContent = loadYamlFile("examples/transformations/field-mapping-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(17)
    @DisplayName("Test 17: Data Type Conversion")
    void testDataTypeConversion() throws IOException {
        String yamlContent = loadYamlFile("examples/transformations/data-type-conversion-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(18)
    @DisplayName("Test 18: Custom Expressions")
    void testCustomExpressions() throws IOException {
        String yamlContent = loadYamlFile("examples/transformations/custom-expressions-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(19)
    @DisplayName("Test 19: Payment Routing")
    void testPaymentRouting() throws IOException {
        String yamlContent = loadYamlFile("examples/transformation/payment-routing.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(20)
    @DisplayName("Test 20: JSON Transformation")
    void testJsonTransformation() throws IOException {
        String yamlContent = loadYamlFile("examples/etl/json-transformation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    // ========== RULE GROUP TESTS (Tests 21-25) ==========

    @Test
    @Order(21)
    @DisplayName("Test 21: Inline Rules")
    void testInlineRules() throws IOException {
        String yamlContent = loadYamlFile("examples/rules/inline-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(22)
    @DisplayName("Test 22: Conditional Rules")
    void testConditionalRules() throws IOException {
        String yamlContent = loadYamlFile("examples/rules/conditional-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(23)
    @DisplayName("Test 23: Advanced Rule Groups")
    void testAdvancedRuleGroups() throws IOException {
        String yamlContent = loadYamlFile("examples/rules/advanced-rule-groups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(24)
    @DisplayName("Test 24: Inline Groups")
    void testInlineGroups() throws IOException {
        String yamlContent = loadYamlFile("examples/rulegroups/inline-groups.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(25)
    @DisplayName("Test 25: Customer Pipeline")
    void testCustomerPipeline() throws IOException {
        String yamlContent = loadYamlFile("examples/etl/customer-pipeline.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    // ========== CONDITIONAL LOGIC TESTS (Tests 26-30) ==========

    @Test
    @Order(26)
    @DisplayName("Test 26: Advanced Routing")
    void testAdvancedRouting() throws IOException {
        String yamlContent = loadYamlFile("examples/conditional/advanced-routing.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(27)
    @DisplayName("Test 27: FX Transaction Processing")
    void testFxTransactionProcessing() throws IOException {
        String yamlContent = loadYamlFile("examples/conditional/fx-transaction-processing.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(28)
    @DisplayName("Test 28: Nested Discount Logic")
    void testNestedDiscountLogic() throws IOException {
        String yamlContent = loadYamlFile("examples/conditional/nested-discount-logic.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(29)
    @DisplayName("Test 29: Waterfall Approval")
    void testWaterfallApproval() throws IOException {
        String yamlContent = loadYamlFile("examples/conditional/waterfall-approval.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(30)
    @DisplayName("Test 30: Comprehensive Financial Settlement")
    void testComprehensiveFinancialSettlement() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichment/comprehensive-financial-settlement.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    // ========== CONFIGURATION TESTS (Tests 31-35) ==========

    @Test
    @Order(31)
    @DisplayName("Test 31: Global Settings")
    void testGlobalSettings() throws IOException {
        String yamlContent = loadYamlFile("examples/configuration/global-settings-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(32)
    @DisplayName("Test 32: Execution Options")
    void testExecutionOptions() throws IOException {
        String yamlContent = loadYamlFile("examples/configuration/execution-options-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(33)
    @DisplayName("Test 33: Metadata Edge Cases")
    void testMetadataEdgeCases() throws IOException {
        String yamlContent = loadYamlFile("examples/configuration/metadata-edge-cases-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(34)
    @DisplayName("Test 34: Conditional Enablement")
    void testConditionalEnablement() throws IOException {
        String yamlContent = loadYamlFile("examples/data-sources/conditional-enablement-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(35)
    @DisplayName("Test 35: Multiple Data Source Refs")
    void testMultipleDataSourceRefs() throws IOException {
        String yamlContent = loadYamlFile("examples/data-sources/multiple-refs-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    // ========== ENRICHMENT SAMPLES TESTS (Tests 36-40) ==========

    @Test
    @Order(36)
    @DisplayName("Test 36: Constant Enrichment Samples")
    void testConstantEnrichmentSamples() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichment/constant-enrichment-samples.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(37)
    @DisplayName("Test 37: Enrichment Service Requirement")
    void testEnrichmentServiceRequirement() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichment/enrichment-service-requirement.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(38)
    @DisplayName("Test 38: Async Patterns")
    void testAsyncPatterns() throws IOException {
        String yamlContent = loadYamlFile("examples/enrichments/async-patterns-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(39)
    @DisplayName("Test 39: Dynamic Lookups")
    void testDynamicLookups() throws IOException {
        String yamlContent = loadYamlFile("examples/lookups/dynamic-lookups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    @Test
    @Order(40)
    @DisplayName("Test 40: Basic Rules Test (from validation)")
    void testBasicRulesFromValidation() throws IOException {
        String yamlContent = loadYamlFile("examples/validation/basic-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yamlContent);

        verifyBlockExists("apex_rule", 3, "Should have 3 Rule blocks");
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }

    // ========== ROUND-TRIP TESTS (Tests 41-64) ==========
    // These tests verify that YAML can be imported and exported back with basic structure preserved

    @Test
    @Order(41)
    @DisplayName("Round-trip Test 1: Minimal Rule")
    void testRoundTripMinimalRule() throws IOException {
        String originalYaml = loadYamlFile("examples/basic/minimal-rule.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
        assertTrue(exportedYaml.contains("rules:"), "Should contain rules section");
    }

    @Test
    @Order(42)
    @DisplayName("Round-trip Test 2: Simple Age Validation")
    void testRoundTripSimpleAgeValidation() throws IOException {
        String originalYaml = loadYamlFile("examples/basic/simple-age-validation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(43)
    @DisplayName("Round-trip Test 3: Nested Field Navigation")
    void testRoundTripNestedFieldNavigation() throws IOException {
        String originalYaml = loadYamlFile("examples/basic/nested-field-navigation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(44)
    @DisplayName("Round-trip Test 4: Quick Start")
    void testRoundTripQuickStart() throws IOException {
        String originalYaml = loadYamlFile("examples/basic/quick-start.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(45)
    @DisplayName("Round-trip Test 5: Value Threshold")
    void testRoundTripValueThreshold() throws IOException {
        String originalYaml = loadYamlFile("examples/validation/value-threshold.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(46)
    @DisplayName("Round-trip Test 6: Constant Value Enrichment")
    void testRoundTripConstantValueEnrichment() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichment/constant-value-enrichment.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(47)
    @DisplayName("Round-trip Test 7: Constant Values")
    void testRoundTripConstantValues() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichment/constant-values.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(48)
    @DisplayName("Round-trip Test 8: Financial Validation")
    void testRoundTripFinancialValidation() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichment/financial-validation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(49)
    @DisplayName("Round-trip Test 9: Conditional Enrichment")
    void testRoundTripConditionalEnrichment() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichments/conditional-enrichment-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(50)
    @DisplayName("Round-trip Test 10: Composite Enrichments")
    void testRoundTripCompositeEnrichments() throws IOException {
        String originalYaml = loadYamlFile("examples/enrichments/composite-enrichments-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(51)
    @DisplayName("Round-trip Test 11: Dynamic Pricing")
    void testRoundTripDynamicPricing() throws IOException {
        String originalYaml = loadYamlFile("examples/lookup/dynamic-pricing.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(52)
    @DisplayName("Round-trip Test 12: Math Calculations")
    void testRoundTripMathCalculations() throws IOException {
        String originalYaml = loadYamlFile("examples/lookup/math-calculations.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(53)
    @DisplayName("Round-trip Test 13: Cache Configuration")
    void testRoundTripCacheConfiguration() throws IOException {
        String originalYaml = loadYamlFile("examples/lookups/cache-configuration-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(54)
    @DisplayName("Round-trip Test 14: Fallback Values")
    void testRoundTripFallbackValues() throws IOException {
        String originalYaml = loadYamlFile("examples/lookups/fallback-values-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(55)
    @DisplayName("Round-trip Test 15: Multi-Key Lookups")
    void testRoundTripMultiKeyLookups() throws IOException {
        String originalYaml = loadYamlFile("examples/lookups/multi-key-lookups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(56)
    @DisplayName("Round-trip Test 16: Field Mapping")
    void testRoundTripFieldMapping() throws IOException {
        String originalYaml = loadYamlFile("examples/transformations/field-mapping-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(57)
    @DisplayName("Round-trip Test 17: Data Type Conversion")
    void testRoundTripDataTypeConversion() throws IOException {
        String originalYaml = loadYamlFile("examples/transformations/data-type-conversion-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(58)
    @DisplayName("Round-trip Test 18: Custom Expressions")
    void testRoundTripCustomExpressions() throws IOException {
        String originalYaml = loadYamlFile("examples/transformations/custom-expressions-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(59)
    @DisplayName("Round-trip Test 19: Payment Routing")
    void testRoundTripPaymentRouting() throws IOException {
        String originalYaml = loadYamlFile("examples/transformation/payment-routing.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(60)
    @DisplayName("Round-trip Test 20: JSON Transformation")
    void testRoundTripJsonTransformation() throws IOException {
        String originalYaml = loadYamlFile("examples/etl/json-transformation.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(61)
    @DisplayName("Round-trip Test 21: Inline Rules")
    void testRoundTripInlineRules() throws IOException {
        String originalYaml = loadYamlFile("examples/rules/inline-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(62)
    @DisplayName("Round-trip Test 22: Conditional Rules")
    void testRoundTripConditionalRules() throws IOException {
        String originalYaml = loadYamlFile("examples/rules/conditional-rules-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(63)
    @DisplayName("Round-trip Test 23: Advanced Rule Groups")
    void testRoundTripAdvancedRuleGroups() throws IOException {
        String originalYaml = loadYamlFile("examples/rules/advanced-rule-groups-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }

    @Test
    @Order(64)
    @DisplayName("Round-trip Test 24: Inline Groups")
    void testRoundTripInlineGroups() throws IOException {
        String originalYaml = loadYamlFile("examples/rulegroups/inline-groups.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(originalYaml);
        waitForBlocksToRender();
        String exportedYaml = exportYamlContent();

        assertNotNull(exportedYaml, "Exported YAML should not be null");
        assertFalse(exportedYaml.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exportedYaml.contains("metadata:"), "Should contain metadata section");
    }
}

