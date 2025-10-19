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
 * Integration tests proving that TreeNode correctly displays YAML content summaries
 * from actual APEX YAML files.
 * 
 * This test proves:
 * 1. YamlContentAnalyzer correctly reads and parses real YAML files
 * 2. TreeNode can hold and display content summaries
 * 3. The complete flow from YAML file → analysis → tree node works end-to-end
 */
@DisplayName("TreeNode with Content Integration Tests")
class TreeNodeWithContentIntegrationTest {

    private YamlContentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new YamlContentAnalyzer();
    }

    @Test
    @DisplayName("Should build tree with real YAML file content summaries")
    void testTreeWithRealYamlContent() {
        // Create tree nodes with actual YAML file paths
        TreeNode root = new TreeNode("src/test/resources/apex-yaml-samples/scenario-registry.yaml", 0);
        TreeNode tradeValidation = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 1);
        TreeNode tradeEnrichment = new TreeNode("src/test/resources/apex-yaml-samples/trade-enrichment.yaml", 1);

        // Analyze actual YAML files and attach summaries to nodes
        root.setContentSummary(analyzer.analyzYamlContent(root.getName()));
        tradeValidation.setContentSummary(analyzer.analyzYamlContent(tradeValidation.getName()));
        tradeEnrichment.setContentSummary(analyzer.analyzYamlContent(tradeEnrichment.getName()));

        // Build tree structure
        root.addChild(tradeValidation);
        root.addChild(tradeEnrichment);
        root.calculateHeight();

        // Verify tree structure
        assertEquals(2, root.getChildCount());
        assertEquals(1, root.getHeight());

        // Verify content summaries are populated from real YAML files
        assertNotNull(root.getContentSummary());
        assertNotNull(tradeValidation.getContentSummary());
        assertNotNull(tradeEnrichment.getContentSummary());

        // Verify trade validation rules content
        assertEquals("rules", tradeValidation.getContentSummary().getFileType());
        assertEquals("trade-validation-rules", tradeValidation.getContentSummary().getId());
        assertEquals(2, tradeValidation.getContentSummary().getRuleGroupCount());
        assertEquals(5, tradeValidation.getContentSummary().getRuleCount());

        // Verify trade enrichment content
        assertEquals("enrichments", tradeEnrichment.getContentSummary().getFileType());
        assertEquals("trade-enrichment", tradeEnrichment.getContentSummary().getId());
        assertEquals(3, tradeEnrichment.getContentSummary().getEnrichmentCount());
    }

    @Test
    @DisplayName("Should display rule count in tree node summary")
    void testRuleCountDisplayInNode() {
        TreeNode node = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 0);
        YamlContentSummary summary = analyzer.analyzYamlContent(node.getName());
        node.setContentSummary(summary);

        // Verify the node has the correct rule count from the YAML file
        assertEquals(5, node.getContentSummary().getRuleCount());
        assertEquals(2, node.getContentSummary().getRuleGroupCount());
    }

    @Test
    @DisplayName("Should display enrichment count in tree node summary")
    void testEnrichmentCountDisplayInNode() {
        TreeNode node = new TreeNode("src/test/resources/apex-yaml-samples/trade-enrichment.yaml", 0);
        YamlContentSummary summary = analyzer.analyzYamlContent(node.getName());
        node.setContentSummary(summary);

        // Verify the node has the correct enrichment count from the YAML file
        assertEquals(3, node.getContentSummary().getEnrichmentCount());
    }

    @Test
    @DisplayName("Should display file type in tree node summary")
    void testFileTypeDisplayInNode() {
        TreeNode rulesNode = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 0);
        TreeNode enrichmentNode = new TreeNode("src/test/resources/apex-yaml-samples/trade-enrichment.yaml", 0);

        rulesNode.setContentSummary(analyzer.analyzYamlContent(rulesNode.getName()));
        enrichmentNode.setContentSummary(analyzer.analyzYamlContent(enrichmentNode.getName()));

        // Verify file types are correctly identified from YAML content
        assertEquals("rules", rulesNode.getContentSummary().getFileType());
        assertEquals("enrichments", enrichmentNode.getContentSummary().getFileType());
    }

    @Test
    @DisplayName("Should display metadata in tree node summary")
    void testMetadataDisplayInNode() {
        TreeNode node = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 0);
        YamlContentSummary summary = analyzer.analyzYamlContent(node.getName());
        node.setContentSummary(summary);

        // Verify metadata is extracted from YAML file
        assertEquals("trade-validation-rules", node.getContentSummary().getId());
        assertEquals("Trade Validation Rules", node.getContentSummary().getName());
        assertNotNull(node.getContentSummary().getDescription());
        assertNotNull(node.getContentSummary().getVersion());
    }

    @Test
    @DisplayName("Should build multi-level tree with content summaries at each level")
    void testMultiLevelTreeWithContent() {
        // Create a 3-level tree
        TreeNode level0 = new TreeNode("src/test/resources/apex-yaml-samples/scenario-registry.yaml", 0);
        TreeNode level1a = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 1);
        TreeNode level1b = new TreeNode("src/test/resources/apex-yaml-samples/trade-enrichment.yaml", 1);
        TreeNode level2 = new TreeNode("src/test/resources/apex-yaml-samples/compliance-rules.yaml", 2);

        // Analyze and attach content summaries
        level0.setContentSummary(analyzer.analyzYamlContent(level0.getName()));
        level1a.setContentSummary(analyzer.analyzYamlContent(level1a.getName()));
        level1b.setContentSummary(analyzer.analyzYamlContent(level1b.getName()));
        level2.setContentSummary(analyzer.analyzYamlContent(level2.getName()));

        // Build tree
        level0.addChild(level1a);
        level0.addChild(level1b);
        level1a.addChild(level2);
        level0.calculateHeight();

        // Verify all levels have content summaries
        assertNotNull(level0.getContentSummary());
        assertNotNull(level1a.getContentSummary());
        assertNotNull(level1b.getContentSummary());
        assertNotNull(level2.getContentSummary());

        // Verify each level has correct content
        assertEquals("rules", level1a.getContentSummary().getFileType());
        assertEquals(5, level1a.getContentSummary().getRuleCount());

        assertEquals("enrichments", level1b.getContentSummary().getFileType());
        assertEquals(3, level1b.getContentSummary().getEnrichmentCount());

        // Verify tree structure
        assertEquals(2, level0.getChildCount());
        assertEquals(1, level1a.getChildCount());
        assertEquals(2, level0.getHeight());
    }

    @Test
    @DisplayName("Should prove console output can display rule counts from real YAML")
    void testConsoleOutputWithRealContent() {
        TreeNode node = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 0);
        YamlContentSummary summary = analyzer.analyzYamlContent(node.getName());
        node.setContentSummary(summary);

        // This proves the data is available for console output
        String output = formatNodeForConsole(node);
        
        // Verify the output contains the actual counts from the YAML file
        assertTrue(output.contains("trade-validation-rules"));
        assertTrue(output.contains("Type: rules"));
        assertTrue(output.contains("RuleGroups: 2"));
        assertTrue(output.contains("Rules: 5"));
    }

    /**
     * Helper method to format node for console output (simulates what console demo does).
     */
    private String formatNodeForConsole(TreeNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getName()).append(" [depth=").append(node.getDepth()).append("]");
        
        if (node.getContentSummary() != null) {
            YamlContentSummary summary = node.getContentSummary();
            sb.append(" | [SUMMARY] ");
            
            if (summary.getFileType() != null) {
                sb.append("Type: ").append(summary.getFileType()).append(" | ");
            }
            if (summary.getRuleGroupCount() > 0) {
                sb.append("RuleGroups: ").append(summary.getRuleGroupCount()).append(" | ");
            }
            if (summary.getRuleCount() > 0) {
                sb.append("Rules: ").append(summary.getRuleCount()).append(" | ");
            }
            if (summary.getEnrichmentCount() > 0) {
                sb.append("Enrichments: ").append(summary.getEnrichmentCount()).append(" | ");
            }
        }
        
        return sb.toString();
    }
}

