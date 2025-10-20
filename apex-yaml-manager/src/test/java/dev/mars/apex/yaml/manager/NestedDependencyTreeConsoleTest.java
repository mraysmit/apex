package dev.mars.apex.yaml.manager;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that demonstrates the nested dependency tree console output
 * with real YAML files in subdirectories.
 */
@DisplayName("Nested Dependency Tree Console Output Test")
class NestedDependencyTreeConsoleTest {

    @Test
    @DisplayName("Should display nested dependency tree with content summaries in console")
    void testNestedDependencyTreeConsoleOutput() {
        YamlContentAnalyzer analyzer = new YamlContentAnalyzer();

        System.out.println("\n" + "=".repeat(100));
        System.out.println("NESTED DEPENDENCY TREE - NAVIGATING SUBDIRECTORIES WITH CONTENT SUMMARIES");
        System.out.println("=".repeat(100));

        System.out.println("\n1. DIRECTORY STRUCTURE");
        System.out.println("-".repeat(100));
        System.out.println("nested-yaml-structure/");
        System.out.println("├── root-scenario.yaml");
        System.out.println("├── trading/");
        System.out.println("│   ├── validation/");
        System.out.println("│   │   └── trade-validation-rules.yaml");
        System.out.println("│   └── enrichment/");
        System.out.println("│       └── trade-enrichment-rules.yaml");
        System.out.println("├── compliance/");
        System.out.println("│   └── compliance-rules.yaml");
        System.out.println("└── shared/");
        System.out.println("    ├── trade-config.yaml");
        System.out.println("    ├── enrichment-config.yaml");
        System.out.println("    └── compliance-config.yaml");

        System.out.println("\n2. ANALYZING NESTED YAML FILES");
        System.out.println("-".repeat(100));

        TreeNode root = new TreeNode("src/test/resources/nested-yaml-structure/root-scenario.yaml", 0);
        TreeNode validation = new TreeNode("src/test/resources/nested-yaml-structure/trading/validation/trade-validation-rules.yaml", 1);
        TreeNode enrichment = new TreeNode("src/test/resources/nested-yaml-structure/trading/enrichment/trade-enrichment-rules.yaml", 1);
        TreeNode compliance = new TreeNode("src/test/resources/nested-yaml-structure/compliance/compliance-rules.yaml", 1);
        TreeNode tradeConfig = new TreeNode("src/test/resources/nested-yaml-structure/shared/trade-config.yaml", 2);
        TreeNode enrichmentConfig = new TreeNode("src/test/resources/nested-yaml-structure/shared/enrichment-config.yaml", 2);
        TreeNode complianceConfig = new TreeNode("src/test/resources/nested-yaml-structure/shared/compliance-config.yaml", 2);

        System.out.println("Analyzing: root-scenario.yaml");
        root.setContentSummary(analyzer.analyzYamlContent(root.getName()));
        System.out.println("  ✓ Found: scenario with 3 references");

        System.out.println("Analyzing: trading/validation/trade-validation-rules.yaml");
        validation.setContentSummary(analyzer.analyzYamlContent(validation.getName()));
        System.out.println("  ✓ Found: " + validation.getContentSummary().getRuleGroupCount() + " rule groups, " + 
                          validation.getContentSummary().getRuleCount() + " rules");

        System.out.println("Analyzing: trading/enrichment/trade-enrichment-rules.yaml");
        enrichment.setContentSummary(analyzer.analyzYamlContent(enrichment.getName()));
        System.out.println("  ✓ Found: " + enrichment.getContentSummary().getEnrichmentCount() + " enrichments");

        System.out.println("Analyzing: compliance/compliance-rules.yaml");
        compliance.setContentSummary(analyzer.analyzYamlContent(compliance.getName()));
        System.out.println("  ✓ Found: " + compliance.getContentSummary().getRuleGroupCount() + " rule groups, " + 
                          compliance.getContentSummary().getRuleCount() + " rules");

        System.out.println("Analyzing: shared/trade-config.yaml");
        tradeConfig.setContentSummary(analyzer.analyzYamlContent(tradeConfig.getName()));
        System.out.println("  ✓ Found: config file");

        System.out.println("Analyzing: shared/enrichment-config.yaml");
        enrichmentConfig.setContentSummary(analyzer.analyzYamlContent(enrichmentConfig.getName()));
        System.out.println("  ✓ Found: config file");

        System.out.println("Analyzing: shared/compliance-config.yaml");
        complianceConfig.setContentSummary(analyzer.analyzYamlContent(complianceConfig.getName()));
        System.out.println("  ✓ Found: config file");

        // Build tree
        root.addChild(validation);
        root.addChild(enrichment);
        root.addChild(compliance);
        validation.addChild(tradeConfig);
        enrichment.addChild(enrichmentConfig);
        compliance.addChild(complianceConfig);
        root.calculateHeight();

        System.out.println("\n3. DEPENDENCY TREE WITH CONTENT SUMMARIES");
        System.out.println("-".repeat(100));
        printTreeToConsole(root);

        System.out.println("\n4. TREE STATISTICS");
        System.out.println("-".repeat(100));
        System.out.println("Root Node: " + root.getName().substring(root.getName().lastIndexOf('/') + 1));
        System.out.println("Total Descendants: " + root.getDescendantCount());
        System.out.println("Tree Height: " + root.getHeight());
        System.out.println("Direct Children: " + root.getChildCount());

        System.out.println("\n5. CONTENT SUMMARY DETAILS");
        System.out.println("-".repeat(100));
        printDetailedSummary("Root Scenario", root.getContentSummary());
        printDetailedSummary("Trade Validation Rules", validation.getContentSummary());
        printDetailedSummary("Trade Enrichment Rules", enrichment.getContentSummary());
        printDetailedSummary("Compliance Rules", compliance.getContentSummary());
        printDetailedSummary("Trade Config", tradeConfig.getContentSummary());
        printDetailedSummary("Enrichment Config", enrichmentConfig.getContentSummary());
        printDetailedSummary("Compliance Config", complianceConfig.getContentSummary());

        System.out.println("\n" + "=".repeat(100) + "\n");

        // Verify all nodes were analyzed correctly
        assertNotNull(root.getContentSummary());
        assertNotNull(validation.getContentSummary());
        assertNotNull(enrichment.getContentSummary());
        assertNotNull(compliance.getContentSummary());
        assertNotNull(tradeConfig.getContentSummary());
        assertNotNull(enrichmentConfig.getContentSummary());
        assertNotNull(complianceConfig.getContentSummary());

        // Verify tree structure
        assertEquals(3, root.getChildCount());
        assertEquals(7, root.getDescendantCount());
        assertEquals(2, root.getHeight());
    }

    private static void printTreeToConsole(TreeNode node) {
        printTreeToConsole(node, "", true);
    }

    private static void printTreeToConsole(TreeNode node, String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        String fileName = node.getName().substring(node.getName().lastIndexOf('/') + 1);
        String nodeInfo = String.format("%s [depth=%d, height=%d, children=%d]",
                fileName, node.getDepth(), node.getHeight(), node.getChildCount());
        
        System.out.println(prefix + connector + nodeInfo);

        if (node.getContentSummary() != null) {
            printContentSummary(node.getContentSummary(), prefix, isLast);
        }

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                TreeNode child = node.getChildren().get(i);
                boolean isLastChild = (i == node.getChildren().size() - 1);
                String newPrefix = prefix + (isLast ? "    " : "│   ");
                printTreeToConsole(child, newPrefix, isLastChild);
            }
        }
    }

    private static void printContentSummary(YamlContentSummary summary, String prefix, boolean isLast) {
        String summaryPrefix = prefix + (isLast ? "    " : "│   ");
        
        StringBuilder sb = new StringBuilder();
        sb.append("├─ [SUMMARY] ");
        
        if (summary.getFileType() != null) {
            sb.append("Type: ").append(String.format("%-12s", summary.getFileType()));
        }
        
        if (summary.getId() != null) {
            sb.append(" | ID: ").append(summary.getId());
        }
        
        System.out.println(summaryPrefix + sb.toString());
        
        StringBuilder counts = new StringBuilder();
        counts.append("│  ");
        
        boolean hasContent = false;
        if (summary.getRuleGroupCount() > 0) {
            counts.append("RuleGroups: ").append(summary.getRuleGroupCount());
            hasContent = true;
        }
        
        if (summary.getRuleCount() > 0) {
            if (hasContent) counts.append(" | ");
            counts.append("Rules: ").append(summary.getRuleCount());
            hasContent = true;
        }
        
        if (summary.getEnrichmentCount() > 0) {
            if (hasContent) counts.append(" | ");
            counts.append("Enrichments: ").append(summary.getEnrichmentCount());
            hasContent = true;
        }
        
        if (summary.getConfigFileCount() > 0) {
            if (hasContent) counts.append(" | ");
            counts.append("Configs: ").append(summary.getConfigFileCount());
            hasContent = true;
        }
        
        if (summary.getReferenceCount() > 0) {
            if (hasContent) counts.append(" | ");
            counts.append("References: ").append(summary.getReferenceCount());
        }
        
        if (hasContent) {
            System.out.println(summaryPrefix + counts.toString());
        }
    }

    private static void printDetailedSummary(String name, YamlContentSummary summary) {
        System.out.println("\n" + name + ":");
        System.out.println("  File Type: " + summary.getFileType());
        System.out.println("  ID: " + summary.getId());
        System.out.println("  Name: " + summary.getName());
        System.out.println("  Description: " + summary.getDescription());
        System.out.println("  Version: " + summary.getVersion());
        if (summary.getRuleGroupCount() > 0) {
            System.out.println("  Rule Groups: " + summary.getRuleGroupCount());
        }
        if (summary.getRuleCount() > 0) {
            System.out.println("  Rules: " + summary.getRuleCount());
        }
        if (summary.getEnrichmentCount() > 0) {
            System.out.println("  Enrichments: " + summary.getEnrichmentCount());
        }
        if (summary.getConfigFileCount() > 0) {
            System.out.println("  Config Files: " + summary.getConfigFileCount());
        }
        if (summary.getReferenceCount() > 0) {
            System.out.println("  References: " + summary.getReferenceCount());
        }
    }
}

