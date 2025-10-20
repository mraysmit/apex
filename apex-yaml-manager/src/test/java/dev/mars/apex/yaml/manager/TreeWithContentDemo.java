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

/**
 * Demo class to display tree view with YAML content summaries in console.
 * Run this as a main method to see the tree output with content analysis.
 */
public class TreeWithContentDemo {

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("APEX YAML DEPENDENCY TREE WITH CONTENT SUMMARIES - CONSOLE VIEW DEMO");
        System.out.println("=".repeat(80));

        YamlContentAnalyzer analyzer = new YamlContentAnalyzer();

        // Build a tree structure with actual YAML files
        TreeNode root = new TreeNode("src/test/resources/apex-yaml-samples/scenario-registry.yaml", 0);
        TreeNode tradeValidation = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 1);
        TreeNode tradeEnrichment = new TreeNode("src/test/resources/apex-yaml-samples/trade-enrichment.yaml", 1);
        TreeNode complianceRules = new TreeNode("src/test/resources/apex-yaml-samples/compliance-rules.yaml", 1);

        // Analyze content for each node
        root.setContentSummary(analyzer.analyzYamlContent(root.getName()));
        tradeValidation.setContentSummary(analyzer.analyzYamlContent(tradeValidation.getName()));
        tradeEnrichment.setContentSummary(analyzer.analyzYamlContent(tradeEnrichment.getName()));
        complianceRules.setContentSummary(analyzer.analyzYamlContent(complianceRules.getName()));

        // Build tree structure
        root.addChild(tradeValidation);
        root.addChild(tradeEnrichment);
        root.addChild(complianceRules);

        root.calculateHeight();

        // Output tree to console
        System.out.println("\nDependency Tree with Content Summaries:");
        System.out.println("-".repeat(80));
        printTreeToConsole(root);
        System.out.println("-".repeat(80));

        // Print statistics
        System.out.println("\nTree Statistics:");
        System.out.println("  Root Node: " + root.getName());
        System.out.println("  Total Descendants: " + root.getDescendantCount());
        System.out.println("  Tree Height: " + root.getHeight());
        System.out.println("  Max Depth: " + root.getMaxDepth());
        System.out.println("  Direct Children: " + root.getChildCount());

        System.out.println("\n" + "=".repeat(80) + "\n");
    }

    /**
     * Recursively print tree structure to console with proper formatting.
     */
    private static void printTreeToConsole(TreeNode node) {
        printTreeToConsole(node, "", true);
    }

    /**
     * Recursively print tree structure with ASCII art formatting and content summaries.
     */
    private static void printTreeToConsole(TreeNode node, String prefix, boolean isLast) {
        // Print current node
        String connector = isLast ? "└── " : "├── ";
        String fileName = node.getName().substring(node.getName().lastIndexOf('/') + 1);
        String nodeInfo = String.format("%s [depth=%d, height=%d, children=%d]",
                fileName, node.getDepth(), node.getHeight(), node.getChildCount());

        if (node.isCircular()) {
            nodeInfo += " ⚠ CIRCULAR";
        }

        System.out.println(prefix + connector + nodeInfo);

        // Print content summary if available
        if (node.getContentSummary() != null) {
            printContentSummary(node.getContentSummary(), prefix, isLast);
        }

        // Print children
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                TreeNode child = node.getChildren().get(i);
                boolean isLastChild = (i == node.getChildren().size() - 1);
                String newPrefix = prefix + (isLast ? "    " : "│   ");
                printTreeToConsole(child, newPrefix, isLastChild);
            }
        }
    }

    /**
     * Print YAML content summary for a node with detailed formatting.
     */
    private static void printContentSummary(YamlContentSummary summary, String prefix, boolean isLast) {
        String summaryPrefix = prefix + (isLast ? "    " : "│   ");

        // Line 1: File type and ID
        StringBuilder line1 = new StringBuilder();
        line1.append("├─ [SUMMARY] ");

        if (summary.getFileType() != null) {
            line1.append("Type: ").append(String.format("%-12s", summary.getFileType()));
        }

        if (summary.getId() != null) {
            line1.append(" | ID: ").append(summary.getId());
        }

        System.out.println(summaryPrefix + line1.toString());

        // Line 2: Counts
        StringBuilder line2 = new StringBuilder();
        line2.append("│  ");

        boolean hasContent = false;
        if (summary.getRuleGroupCount() > 0) {
            line2.append("RuleGroups: ").append(summary.getRuleGroupCount());
            hasContent = true;
        }

        if (summary.getRuleCount() > 0) {
            if (hasContent) line2.append(" | ");
            line2.append("Rules: ").append(summary.getRuleCount());
            hasContent = true;
        }

        if (summary.getEnrichmentCount() > 0) {
            if (hasContent) line2.append(" | ");
            line2.append("Enrichments: ").append(summary.getEnrichmentCount());
            hasContent = true;
        }

        if (summary.getConfigFileCount() > 0) {
            if (hasContent) line2.append(" | ");
            line2.append("Configs: ").append(summary.getConfigFileCount());
            hasContent = true;
        }

        if (summary.getReferenceCount() > 0) {
            if (hasContent) line2.append(" | ");
            line2.append("References: ").append(summary.getReferenceCount());
        }

        if (hasContent) {
            System.out.println(summaryPrefix + line2.toString());
        }
    }
}

