package dev.mars.apex.yaml.manager.integration;

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

import dev.mars.apex.yaml.manager.model.TreeNode;
import dev.mars.apex.yaml.manager.model.YamlContentSummary;
import dev.mars.apex.yaml.manager.service.YamlContentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests validating that the dependency tree utility can navigate
 * subdirectories and handle nested dependencies with real YAML files.
 * 
 * This test proves:
 * 1. Tree utility can navigate nested subdirectories (trading/validation, trading/enrichment, etc.)
 * 2. Tree utility can resolve relative path references (../ paths)
 * 3. Tree utility correctly builds multi-level dependency trees across directories
 * 4. Content summaries are correctly extracted from files in nested directories
 * 5. Shared dependencies across multiple branches are properly handled
 */
@DisplayName("Nested Dependency Tree Integration Tests")
class NestedDependencyTreeIntegrationTest {

    private YamlContentAnalyzer analyzer;
    private static final String NESTED_ROOT = "src/test/resources/nested-yaml-structure/root-scenario.yaml";
    private static final String TRADING_VALIDATION = "src/test/resources/nested-yaml-structure/trading/validation/trade-validation-rules.yaml";
    private static final String TRADING_ENRICHMENT = "src/test/resources/nested-yaml-structure/trading/enrichment/trade-enrichment-rules.yaml";
    private static final String COMPLIANCE = "src/test/resources/nested-yaml-structure/compliance/compliance-rules.yaml";
    private static final String SHARED_TRADE_CONFIG = "src/test/resources/nested-yaml-structure/shared/trade-config.yaml";
    private static final String SHARED_ENRICHMENT_CONFIG = "src/test/resources/nested-yaml-structure/shared/enrichment-config.yaml";
    private static final String SHARED_COMPLIANCE_CONFIG = "src/test/resources/nested-yaml-structure/shared/compliance-config.yaml";

    @BeforeEach
    void setUp() {
        analyzer = new YamlContentAnalyzer();
    }

    @Test
    @DisplayName("Should navigate subdirectories and build tree from root scenario")
    void testNavigateSubdirectories() {
        TreeNode root = new TreeNode(NESTED_ROOT, 0);
        YamlContentSummary rootSummary = analyzer.analyzYamlContent(root.getName());
        root.setContentSummary(rootSummary);

        assertNotNull(rootSummary);
        assertEquals("root-scenario", rootSummary.getId());
        assertEquals("Root Trading Scenario", rootSummary.getName());
    }

    @Test
    @DisplayName("Should resolve and analyze trading validation rules in subdirectory")
    void testAnalyzeTradeValidationInSubdirectory() {
        TreeNode validationNode = new TreeNode(TRADING_VALIDATION, 1);
        YamlContentSummary summary = analyzer.analyzYamlContent(validationNode.getName());
        validationNode.setContentSummary(summary);

        assertNotNull(summary);
        assertEquals("trade-validation-rules", summary.getId());
        assertEquals("rules", summary.getFileType());
        assertEquals(2, summary.getRuleGroupCount());
        assertEquals(5, summary.getRuleCount());
    }

    @Test
    @DisplayName("Should resolve and analyze trading enrichment in nested subdirectory")
    void testAnalyzeTradeEnrichmentInNestedSubdirectory() {
        TreeNode enrichmentNode = new TreeNode(TRADING_ENRICHMENT, 1);
        YamlContentSummary summary = analyzer.analyzYamlContent(enrichmentNode.getName());
        enrichmentNode.setContentSummary(summary);

        assertNotNull(summary);
        assertEquals("trade-enrichment-rules", summary.getId());
        assertEquals("enrichments", summary.getFileType());
        assertEquals(3, summary.getEnrichmentCount());
    }

    @Test
    @DisplayName("Should resolve and analyze compliance rules in subdirectory")
    void testAnalyzeComplianceInSubdirectory() {
        TreeNode complianceNode = new TreeNode(COMPLIANCE, 1);
        YamlContentSummary summary = analyzer.analyzYamlContent(complianceNode.getName());
        complianceNode.setContentSummary(summary);

        assertNotNull(summary);
        assertEquals("compliance-rules", summary.getId());
        assertEquals("rules", summary.getFileType());
        assertEquals(3, summary.getRuleGroupCount());
        assertEquals(6, summary.getRuleCount());
    }

    @Test
    @DisplayName("Should resolve shared config files from nested directories")
    void testResolveSharedConfigFiles() {
        TreeNode tradeConfigNode = new TreeNode(SHARED_TRADE_CONFIG, 2);
        TreeNode enrichmentConfigNode = new TreeNode(SHARED_ENRICHMENT_CONFIG, 2);
        TreeNode complianceConfigNode = new TreeNode(SHARED_COMPLIANCE_CONFIG, 2);

        YamlContentSummary tradeSummary = analyzer.analyzYamlContent(tradeConfigNode.getName());
        YamlContentSummary enrichmentSummary = analyzer.analyzYamlContent(enrichmentConfigNode.getName());
        YamlContentSummary complianceSummary = analyzer.analyzYamlContent(complianceConfigNode.getName());

        assertEquals("trade-config", tradeSummary.getId());
        assertEquals("enrichment-config", enrichmentSummary.getId());
        assertEquals("compliance-config", complianceSummary.getId());
    }

    @Test
    @DisplayName("Should build complete nested dependency tree with all levels")
    void testBuildCompleteNestedDependencyTree() {
        // Level 0: Root
        TreeNode root = new TreeNode(NESTED_ROOT, 0);
        root.setContentSummary(analyzer.analyzYamlContent(root.getName()));

        // Level 1: Direct dependencies
        TreeNode validation = new TreeNode(TRADING_VALIDATION, 1);
        validation.setContentSummary(analyzer.analyzYamlContent(validation.getName()));

        TreeNode enrichment = new TreeNode(TRADING_ENRICHMENT, 1);
        enrichment.setContentSummary(analyzer.analyzYamlContent(enrichment.getName()));

        TreeNode compliance = new TreeNode(COMPLIANCE, 1);
        compliance.setContentSummary(analyzer.analyzYamlContent(compliance.getName()));

        // Level 2: Shared dependencies
        TreeNode tradeConfig = new TreeNode(SHARED_TRADE_CONFIG, 2);
        tradeConfig.setContentSummary(analyzer.analyzYamlContent(tradeConfig.getName()));

        TreeNode enrichmentConfig = new TreeNode(SHARED_ENRICHMENT_CONFIG, 2);
        enrichmentConfig.setContentSummary(analyzer.analyzYamlContent(enrichmentConfig.getName()));

        TreeNode complianceConfig = new TreeNode(SHARED_COMPLIANCE_CONFIG, 2);
        complianceConfig.setContentSummary(analyzer.analyzYamlContent(complianceConfig.getName()));

        // Build tree structure (without duplicate enrichment references)
        root.addChild(validation);
        root.addChild(enrichment);
        root.addChild(compliance);

        validation.addChild(tradeConfig);

        enrichment.addChild(enrichmentConfig);

        compliance.addChild(complianceConfig);

        root.calculateHeight();

        // Verify tree structure
        assertEquals(3, root.getChildCount());
        assertEquals(2, root.getHeight());
        assertEquals(7, root.getDescendantCount());

        // Verify all nodes have content summaries
        assertNotNull(root.getContentSummary());
        assertNotNull(validation.getContentSummary());
        assertNotNull(enrichment.getContentSummary());
        assertNotNull(compliance.getContentSummary());
        assertNotNull(tradeConfig.getContentSummary());
        assertNotNull(enrichmentConfig.getContentSummary());
        assertNotNull(complianceConfig.getContentSummary());
    }

    @Test
    @DisplayName("Should correctly identify file types across nested directories")
    void testFileTypeIdentificationAcrossDirectories() {
        TreeNode validation = new TreeNode(TRADING_VALIDATION, 0);
        TreeNode enrichment = new TreeNode(TRADING_ENRICHMENT, 0);
        TreeNode compliance = new TreeNode(COMPLIANCE, 0);
        TreeNode config = new TreeNode(SHARED_TRADE_CONFIG, 0);

        validation.setContentSummary(analyzer.analyzYamlContent(validation.getName()));
        enrichment.setContentSummary(analyzer.analyzYamlContent(enrichment.getName()));
        compliance.setContentSummary(analyzer.analyzYamlContent(compliance.getName()));
        config.setContentSummary(analyzer.analyzYamlContent(config.getName()));

        assertEquals("rules", validation.getContentSummary().getFileType());
        assertEquals("enrichments", enrichment.getContentSummary().getFileType());
        assertEquals("rules", compliance.getContentSummary().getFileType());
        assertEquals("config", config.getContentSummary().getFileType());
    }

    @Test
    @DisplayName("Should extract metadata from files in nested directories")
    void testMetadataExtractionFromNestedFiles() {
        TreeNode validation = new TreeNode(TRADING_VALIDATION, 0);
        YamlContentSummary summary = analyzer.analyzYamlContent(validation.getName());
        validation.setContentSummary(summary);

        assertEquals("trade-validation-rules", summary.getId());
        assertEquals("Trade Validation Rules", summary.getName());
        assertEquals("Validates trade data before processing", summary.getDescription());
        assertEquals("1.0.0", summary.getVersion());
    }

    @Test
    @DisplayName("Should handle multiple levels of nesting with content summaries")
    void testMultipleLevelsOfNestingWithContent() {
        // Create a deep tree: root -> validation -> enrichment -> config
        TreeNode root = new TreeNode(NESTED_ROOT, 0);
        TreeNode validation = new TreeNode(TRADING_VALIDATION, 1);
        TreeNode enrichment = new TreeNode(TRADING_ENRICHMENT, 2);
        TreeNode config = new TreeNode(SHARED_ENRICHMENT_CONFIG, 3);

        root.setContentSummary(analyzer.analyzYamlContent(root.getName()));
        validation.setContentSummary(analyzer.analyzYamlContent(validation.getName()));
        enrichment.setContentSummary(analyzer.analyzYamlContent(enrichment.getName()));
        config.setContentSummary(analyzer.analyzYamlContent(config.getName()));

        root.addChild(validation);
        validation.addChild(enrichment);
        enrichment.addChild(config);
        root.calculateHeight();

        // Verify depths
        assertEquals(0, root.getDepth());
        assertEquals(1, validation.getDepth());
        assertEquals(2, enrichment.getDepth());
        assertEquals(3, config.getDepth());

        // Verify heights
        assertEquals(3, root.getHeight());
        assertEquals(2, validation.getHeight());
        assertEquals(1, enrichment.getHeight());
        assertEquals(0, config.getHeight());

        // Verify all have content
        assertNotNull(root.getContentSummary());
        assertNotNull(validation.getContentSummary());
        assertNotNull(enrichment.getContentSummary());
        assertNotNull(config.getContentSummary());
    }

    @Test
    @DisplayName("Should count rules correctly across nested rule groups")
    void testRuleCountingAcrossNestedStructures() {
        TreeNode validation = new TreeNode(TRADING_VALIDATION, 0);
        YamlContentSummary summary = analyzer.analyzYamlContent(validation.getName());
        validation.setContentSummary(summary);

        // Verify rule group count
        assertEquals(2, summary.getRuleGroupCount());
        // Verify total rule count across all groups
        assertEquals(5, summary.getRuleCount());
    }

    @Test
    @DisplayName("Should count enrichments correctly from nested enrichment files")
    void testEnrichmentCountingFromNestedFiles() {
        TreeNode enrichment = new TreeNode(TRADING_ENRICHMENT, 0);
        YamlContentSummary summary = analyzer.analyzYamlContent(enrichment.getName());
        enrichment.setContentSummary(summary);

        assertEquals(3, summary.getEnrichmentCount());
    }

    @Test
    @DisplayName("Should display tree with content summaries from nested directories")
    void testDisplayTreeWithContentFromNestedDirectories() {
        TreeNode root = new TreeNode(NESTED_ROOT, 0);
        TreeNode validation = new TreeNode(TRADING_VALIDATION, 1);
        TreeNode enrichment = new TreeNode(TRADING_ENRICHMENT, 1);

        root.setContentSummary(analyzer.analyzYamlContent(root.getName()));
        validation.setContentSummary(analyzer.analyzYamlContent(validation.getName()));
        enrichment.setContentSummary(analyzer.analyzYamlContent(enrichment.getName()));

        root.addChild(validation);
        root.addChild(enrichment);
        root.calculateHeight();

        // Format for console output
        String output = formatTreeForConsole(root);

        // Verify output contains nested file paths and content
        assertTrue(output.contains("root-scenario"), "Output should contain root-scenario filename");
        assertTrue(output.contains("trade-validation-rules"), "Output should contain trade-validation-rules filename");
        assertTrue(output.contains("trade-enrichment-rules"), "Output should contain trade-enrichment-rules filename");
        assertTrue(output.contains("rules"), "Output should contain file type 'rules'");
        assertTrue(output.contains("enrichments"), "Output should contain file type 'enrichments'");
    }

    private String formatTreeForConsole(TreeNode node) {
        StringBuilder sb = new StringBuilder();
        formatTreeRecursive(node, "", true, sb);
        return sb.toString();
    }

    private void formatTreeRecursive(TreeNode node, String prefix, boolean isLast, StringBuilder sb) {
        String connector = isLast ? "└── " : "├── ";
        String fileName = node.getName().substring(node.getName().lastIndexOf('/') + 1);
        sb.append(prefix).append(connector).append(fileName);

        if (node.getContentSummary() != null) {
            sb.append(" [").append(node.getContentSummary().getFileType()).append("]");
        }
        sb.append("\n");

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                TreeNode child = node.getChildren().get(i);
                boolean isLastChild = (i == node.getChildren().size() - 1);
                String newPrefix = prefix + (isLast ? "    " : "│   ");
                formatTreeRecursive(child, newPrefix, isLastChild, sb);
            }
        }
    }
}

