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

/**
 * Demo class to display tree view in console.
 * Run this as a main method to see the tree output.
 */
public class TreeConsoleDemo {

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("APEX YAML DEPENDENCY TREE - CONSOLE VIEW DEMO");
        System.out.println("=".repeat(70));

        // Build a complex tree structure
        TreeNode scenario = new TreeNode("scenario.yaml", 0);
        TreeNode ruleConfig = new TreeNode("rule-config.yaml", 1);
        TreeNode ruleGroup = new TreeNode("rule-group.yaml", 2);
        TreeNode rule1 = new TreeNode("validation-rules.yaml", 3);
        TreeNode rule2 = new TreeNode("enrichment-rules.yaml", 3);
        TreeNode enrichment = new TreeNode("enrichment.yaml", 1);
        TreeNode compliance = new TreeNode("compliance-rules.yaml", 1);

        scenario.addChild(ruleConfig);
        scenario.addChild(enrichment);
        scenario.addChild(compliance);
        ruleConfig.addChild(ruleGroup);
        ruleGroup.addChild(rule1);
        ruleGroup.addChild(rule2);

        scenario.calculateHeight();

        // Output tree to console
        System.out.println("\nDependency Tree Structure:");
        System.out.println("-".repeat(70));
        printTreeToConsole(scenario);
        System.out.println("-".repeat(70));

        // Print statistics
        System.out.println("\nTree Statistics:");
        System.out.println("  Root Node: " + scenario.getName());
        System.out.println("  Total Descendants: " + scenario.getDescendantCount());
        System.out.println("  Tree Height: " + scenario.getHeight());
        System.out.println("  Max Depth: " + scenario.getMaxDepth());
        System.out.println("  Direct Children: " + scenario.getChildCount());

        System.out.println("\n" + "=".repeat(70) + "\n");
    }

    /**
     * Recursively print tree structure to console with proper formatting.
     */
    private static void printTreeToConsole(TreeNode node) {
        printTreeToConsole(node, "", true);
    }

    /**
     * Recursively print tree structure with ASCII art formatting.
     */
    private static void printTreeToConsole(TreeNode node, String prefix, boolean isLast) {
        // Print current node
        String connector = isLast ? "└── " : "├── ";
        String nodeInfo = String.format("%s [depth=%d, height=%d, children=%d]",
                node.getName(), node.getDepth(), node.getHeight(), node.getChildCount());

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
     * Print YAML content summary for a node.
     */
    private static void printContentSummary(YamlContentSummary summary, String prefix, boolean isLast) {
        String summaryPrefix = prefix + (isLast ? "    " : "│   ");

        StringBuilder sb = new StringBuilder();
        sb.append("├─ [SUMMARY] ");

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

        if (summary.getConfigFileCount() > 0) {
            sb.append("Configs: ").append(summary.getConfigFileCount()).append(" | ");
        }

        if (summary.getReferenceCount() > 0) {
            sb.append("References: ").append(summary.getReferenceCount());
        }

        System.out.println(summaryPrefix + sb.toString());
    }
}

