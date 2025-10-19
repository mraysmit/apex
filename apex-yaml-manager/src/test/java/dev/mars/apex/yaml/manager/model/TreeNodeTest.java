package dev.mars.apex.yaml.manager.model;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintStream;
import dev.mars.apex.yaml.manager.model.YamlContentSummary;

/**
 * Unit tests for TreeNode model.
 */
@DisplayName("TreeNode Model Tests")
class TreeNodeTest {

    private TreeNode root;

    @BeforeEach
    void setUp() {
        root = new TreeNode("root.yaml", 0);
    }

    @Test
    @DisplayName("Should create TreeNode with name and depth")
    void testTreeNodeCreation() {
        assertEquals("root.yaml", root.getName());
        assertEquals(0, root.getDepth());
        assertEquals("root.yaml", root.getId());
        assertTrue(root.getChildren().isEmpty());
    }

    @Test
    @DisplayName("Should add child nodes")
    void testAddChild() {
        TreeNode child1 = new TreeNode("child1.yaml", 1);
        TreeNode child2 = new TreeNode("child2.yaml", 1);

        root.addChild(child1);
        root.addChild(child2);

        assertEquals(2, root.getChildCount());
        assertEquals(2, root.getChildren().size());
        assertTrue(root.getChildren().contains(child1));
        assertTrue(root.getChildren().contains(child2));
    }

    @Test
    @DisplayName("Should calculate height correctly for leaf node")
    void testCalculateHeightLeaf() {
        root.calculateHeight();
        assertEquals(0, root.getHeight());
    }

    @Test
    @DisplayName("Should calculate height correctly for parent node")
    void testCalculateHeightParent() {
        TreeNode child1 = new TreeNode("child1.yaml", 1);
        TreeNode child2 = new TreeNode("child2.yaml", 1);
        TreeNode grandchild = new TreeNode("grandchild.yaml", 2);

        root.addChild(child1);
        root.addChild(child2);
        child1.addChild(grandchild);

        root.calculateHeight();

        assertEquals(2, root.getHeight());
        assertEquals(1, child1.getHeight());
        assertEquals(0, child2.getHeight());
        assertEquals(0, grandchild.getHeight());
    }

    @Test
    @DisplayName("Should get descendant count")
    void testGetDescendantCount() {
        TreeNode child1 = new TreeNode("child1.yaml", 1);
        TreeNode child2 = new TreeNode("child2.yaml", 1);
        TreeNode grandchild = new TreeNode("grandchild.yaml", 2);

        root.addChild(child1);
        root.addChild(child2);
        child1.addChild(grandchild);

        assertEquals(4, root.getDescendantCount()); // root + 2 children + 1 grandchild
        assertEquals(2, child1.getDescendantCount()); // child1 + 1 grandchild
        assertEquals(1, child2.getDescendantCount()); // child2 only
    }

    @Test
    @DisplayName("Should get max depth of subtree")
    void testGetMaxDepth() {
        TreeNode child1 = new TreeNode("child1.yaml", 1);
        TreeNode child2 = new TreeNode("child2.yaml", 1);
        TreeNode grandchild = new TreeNode("grandchild.yaml", 2);
        TreeNode greatgrandchild = new TreeNode("greatgrandchild.yaml", 3);

        root.addChild(child1);
        root.addChild(child2);
        child1.addChild(grandchild);
        grandchild.addChild(greatgrandchild);

        assertEquals(3, root.getMaxDepth());
        assertEquals(3, child1.getMaxDepth());
        assertEquals(1, child2.getMaxDepth());
    }

    @Test
    @DisplayName("Should mark circular reference")
    void testCircularReference() {
        root.setCircular(true);
        root.setCircularReference("Circular reference detected");

        assertTrue(root.isCircular());
        assertEquals("Circular reference detected", root.getCircularReference());
    }

    @Test
    @DisplayName("Should set children list and update child count")
    void testSetChildren() {
        TreeNode child1 = new TreeNode("child1.yaml", 1);
        TreeNode child2 = new TreeNode("child2.yaml", 1);

        root.setChildren(java.util.Arrays.asList(child1, child2));

        assertEquals(2, root.getChildCount());
        assertEquals(2, root.getChildren().size());
    }

    @Test
    @DisplayName("Should handle null children")
    void testNullChildren() {
        root.setChildren(null);
        assertEquals(0, root.getChildCount());
    }

    @Test
    @DisplayName("Should set and get id")
    void testSetId() {
        root.setId("custom-id");
        assertEquals("custom-id", root.getId());
    }

    @Test
    @DisplayName("Should build complex tree structure")
    void testComplexTreeStructure() {
        // Build a more complex tree
        TreeNode scenario = new TreeNode("scenario.yaml", 0);
        TreeNode ruleConfig = new TreeNode("rule-config.yaml", 1);
        TreeNode ruleGroup = new TreeNode("rule-group.yaml", 2);
        TreeNode rule = new TreeNode("rule.yaml", 3);
        TreeNode enrichment = new TreeNode("enrichment.yaml", 1);

        scenario.addChild(ruleConfig);
        scenario.addChild(enrichment);
        ruleConfig.addChild(ruleGroup);
        ruleGroup.addChild(rule);

        scenario.calculateHeight();

        assertEquals(3, scenario.getHeight());
        assertEquals(5, scenario.getDescendantCount());
        assertEquals(3, scenario.getMaxDepth());
        assertEquals(2, scenario.getChildCount());
    }

    @Test
    @DisplayName("Should detect circular reference in tree")
    void testCircularReferenceDetection() {
        TreeNode child = new TreeNode("child.yaml", 1);
        child.setCircular(true);
        child.setCircularReference("Points back to root");

        root.addChild(child);

        assertTrue(child.isCircular());
        assertEquals("Points back to root", child.getCircularReference());
    }

    @Test
    @DisplayName("Should set and get content summary")
    void testContentSummary() {
        YamlContentSummary summary = new YamlContentSummary("test.yaml");
        summary.setFileType("rules");
        summary.setRuleCount(5);
        summary.setRuleGroupCount(2);

        root.setContentSummary(summary);

        assertNotNull(root.getContentSummary());
        assertEquals("rules", root.getContentSummary().getFileType());
        assertEquals(5, root.getContentSummary().getRuleCount());
        assertEquals(2, root.getContentSummary().getRuleGroupCount());
    }

    @Test
    @DisplayName("Should include content summary in tree structure")
    void testContentSummaryInTree() {
        TreeNode child1 = new TreeNode("child1.yaml", 1);
        TreeNode child2 = new TreeNode("child2.yaml", 1);

        YamlContentSummary summary1 = new YamlContentSummary("child1.yaml");
        summary1.setFileType("enrichments");
        summary1.setEnrichmentCount(3);

        YamlContentSummary summary2 = new YamlContentSummary("child2.yaml");
        summary2.setFileType("rules");
        summary2.setRuleCount(4);

        child1.setContentSummary(summary1);
        child2.setContentSummary(summary2);

        root.addChild(child1);
        root.addChild(child2);

        assertEquals(2, root.getChildCount());
        assertEquals("enrichments", root.getChildren().get(0).getContentSummary().getFileType());
        assertEquals("rules", root.getChildren().get(1).getContentSummary().getFileType());
    }

    @Test
    @DisplayName("Should serialize to string")
    void testToString() {
        String str = root.toString();
        assertTrue(str.contains("root.yaml"));
        assertTrue(str.contains("depth=0"));
    }

    @Test
    @DisplayName("Should output tree view to console")
    void testTreeConsoleOutput() {
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
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DEPENDENCY TREE VIEW");
        System.out.println("=".repeat(60));
        printTreeToConsole(scenario);
        System.out.println("=".repeat(60) + "\n");

        // Verify tree structure
        assertEquals(3, scenario.getChildCount());
        assertEquals(3, scenario.getHeight());
    }

    /**
     * Recursively print tree structure to console with proper formatting.
     */
    private void printTreeToConsole(TreeNode node) {
        printTreeToConsole(node, "", true);
    }

    /**
     * Recursively print tree structure with ASCII art formatting.
     */
    private void printTreeToConsole(TreeNode node, String prefix, boolean isLast) {
        // Print current node
        String connector = isLast ? "└── " : "├── ";
        String nodeInfo = String.format("%s[depth=%d, height=%d, children=%d]",
                node.getName(), node.getDepth(), node.getHeight(), node.getChildCount());

        if (node.isCircular()) {
            nodeInfo += " ⚠ CIRCULAR";
        }

        System.out.println(prefix + connector + nodeInfo);

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
}

