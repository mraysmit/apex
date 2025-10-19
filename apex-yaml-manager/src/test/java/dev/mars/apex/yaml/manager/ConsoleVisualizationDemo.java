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
 * Console visualization demo showing how YAML content summaries appear in the dependency tree.
 * This demonstrates the actual test data and how it's displayed.
 */
public class ConsoleVisualizationDemo {

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("APEX YAML DEPENDENCY TREE - CONSOLE VISUALIZATION WITH CONTENT SUMMARIES");
        System.out.println("=".repeat(100));

        YamlContentAnalyzer analyzer = new YamlContentAnalyzer();

        // Build a realistic dependency tree from actual test YAML files
        System.out.println("\n1. ANALYZING YAML FILES");
        System.out.println("-".repeat(100));

        TreeNode root = new TreeNode("src/test/resources/apex-yaml-samples/scenario-registry.yaml", 0);
        TreeNode tradeValidation = new TreeNode("src/test/resources/apex-yaml-samples/trade-validation-rules.yaml", 1);
        TreeNode tradeEnrichment = new TreeNode("src/test/resources/apex-yaml-samples/trade-enrichment.yaml", 1);
        TreeNode complianceRules = new TreeNode("src/test/resources/apex-yaml-samples/compliance-rules.yaml", 1);

        // Analyze each file
        System.out.println("Analyzing: trade-validation-rules.yaml");
        YamlContentSummary validationSummary = analyzer.analyzYamlContent(tradeValidation.getName());
        System.out.println("  ✓ Found: " + validationSummary.getRuleGroupCount() + " rule groups, " + 
                          validationSummary.getRuleCount() + " rules");

        System.out.println("Analyzing: trade-enrichment.yaml");
        YamlContentSummary enrichmentSummary = analyzer.analyzYamlContent(tradeEnrichment.getName());
        System.out.println("  ✓ Found: " + enrichmentSummary.getEnrichmentCount() + " enrichments");

        System.out.println("Analyzing: compliance-rules.yaml");
        YamlContentSummary complianceSummary = analyzer.analyzYamlContent(complianceRules.getName());
        System.out.println("  ✓ Found: " + complianceSummary.getRuleGroupCount() + " rule groups, " + 
                          complianceSummary.getRuleCount() + " rules");

        // Attach summaries to nodes
        root.setContentSummary(analyzer.analyzYamlContent(root.getName()));
        tradeValidation.setContentSummary(validationSummary);
        tradeEnrichment.setContentSummary(enrichmentSummary);
        complianceRules.setContentSummary(complianceSummary);

        // Build tree structure
        root.addChild(tradeValidation);
        root.addChild(tradeEnrichment);
        root.addChild(complianceRules);
        root.calculateHeight();

        // Display tree
        System.out.println("\n2. DEPENDENCY TREE WITH CONTENT SUMMARIES");
        System.out.println("-".repeat(100));
        printTreeToConsole(root);

        // Display statistics
        System.out.println("\n3. TREE STATISTICS");
        System.out.println("-".repeat(100));
        System.out.println("Root Node: " + root.getName().substring(root.getName().lastIndexOf('/') + 1));
        System.out.println("Total Descendants: " + root.getDescendantCount());
        System.out.println("Tree Height: " + root.getHeight());
        System.out.println("Direct Children: " + root.getChildCount());

        // Display content summary
        System.out.println("\n4. CONTENT SUMMARY DETAILS");
        System.out.println("-".repeat(100));
        printDetailedSummary("Trade Validation Rules", validationSummary);
        printDetailedSummary("Trade Enrichment", enrichmentSummary);
        printDetailedSummary("Compliance Rules", complianceSummary);

        System.out.println("\n" + "=".repeat(100) + "\n");
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

