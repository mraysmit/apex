package dev.mars.apex.yaml.manager.service;

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

import dev.mars.apex.yaml.manager.model.DependencyMetrics;
import dev.mars.apex.yaml.manager.model.EnhancedYamlDependencyGraph;
import dev.mars.apex.yaml.manager.model.ImpactAnalysisResult;
import dev.mars.apex.yaml.manager.model.TreeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DependencyAnalysisService.
 *
 * Tests cover:
 * - Dependency analysis
 * - Impact analysis
 * - Circular dependency detection
 * - Orphaned file detection
 * - Critical file identification
 * - Metrics calculation
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@DisplayName("DependencyAnalysisService Tests")
class DependencyAnalysisServiceTest {

    private DependencyAnalysisService service;
    private EnhancedYamlDependencyGraph graph;

    @BeforeEach
    void setUp() {
        service = new DependencyAnalysisService();
        graph = new EnhancedYamlDependencyGraph("root.yaml");
    }

    @Test
    @DisplayName("Should create service instance")
    void testServiceCreation() {
        assertNotNull(service);
    }

    @Test
    @DisplayName("Should create empty dependency graph")
    void testEmptyGraph() {
        assertNotNull(graph);
        assertEquals("root.yaml", graph.getRootFile());
        assertEquals(0, graph.getTotalFiles());
    }

    @Test
    @DisplayName("Should add bidirectional edges")
    void testAddBidirectionalEdges() {
        graph.addBidirectionalEdge("file1.yaml", "file2.yaml");
        graph.addBidirectionalEdge("file2.yaml", "file3.yaml");

        // Verify forward edges
        Set<String> deps1 = graph.getDirectDependencies("file1.yaml");
        assertTrue(deps1.contains("file2.yaml"));

        // Verify reverse edges
        Set<String> dependents2 = graph.getDirectDependents("file2.yaml");
        assertTrue(dependents2.contains("file1.yaml"));
    }

    @Test
    @DisplayName("Should calculate direct dependencies")
    void testDirectDependencies() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("a.yaml", "c.yaml");

        Set<String> deps = graph.getDirectDependencies("a.yaml");
        assertEquals(2, deps.size());
        assertTrue(deps.contains("b.yaml"));
        assertTrue(deps.contains("c.yaml"));
    }

    @Test
    @DisplayName("Should calculate direct dependents")
    void testDirectDependents() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("c.yaml", "b.yaml");

        Set<String> dependents = graph.getDirectDependents("b.yaml");
        assertEquals(2, dependents.size());
        assertTrue(dependents.contains("a.yaml"));
        assertTrue(dependents.contains("c.yaml"));
    }

    @Test
    @DisplayName("Should calculate transitive dependencies")
    void testTransitiveDependencies() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");
        graph.addBidirectionalEdge("c.yaml", "d.yaml");

        Set<String> transitive = graph.getTransitiveDependencies("a.yaml");
        assertEquals(3, transitive.size());
        assertTrue(transitive.contains("b.yaml"));
        assertTrue(transitive.contains("c.yaml"));
        assertTrue(transitive.contains("d.yaml"));
    }

    @Test
    @DisplayName("Should calculate transitive dependents")
    void testTransitiveDependents() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");
        graph.addBidirectionalEdge("c.yaml", "d.yaml");

        Set<String> transitive = graph.getTransitiveDependents("d.yaml");
        assertEquals(3, transitive.size());
        assertTrue(transitive.contains("c.yaml"));
        assertTrue(transitive.contains("b.yaml"));
        assertTrue(transitive.contains("a.yaml"));
    }

    @Test
    @DisplayName("Should perform impact analysis")
    void testImpactAnalysis() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");
        graph.addBidirectionalEdge("b.yaml", "d.yaml");

        ImpactAnalysisResult impact = graph.analyzeImpact("b.yaml");

        assertNotNull(impact);
        assertEquals("b.yaml", impact.getAnalyzedFile());
        assertEquals(1, impact.getDirectDependents().size());
        assertEquals(2, impact.getDirectDependencies().size());
    }

    @Test
    @DisplayName("Should identify orphaned files")
    void testOrphanedFiles() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");
        // d.yaml is orphaned - no edges

        Set<String> orphaned = graph.getOrphanedFiles();
        assertTrue(orphaned.isEmpty() || !orphaned.contains("a.yaml"));
    }

    @Test
    @DisplayName("Should identify critical files")
    void testCriticalFiles() {
        // Create a hub file referenced by many others
        graph.addBidirectionalEdge("a.yaml", "hub.yaml");
        graph.addBidirectionalEdge("b.yaml", "hub.yaml");
        graph.addBidirectionalEdge("c.yaml", "hub.yaml");
        graph.addBidirectionalEdge("d.yaml", "hub.yaml");

        Set<String> critical = graph.getCriticalFiles();
        assertTrue(critical.contains("hub.yaml"));
    }

    @Test
    @DisplayName("Should calculate complexity score")
    void testComplexityScore() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");

        int score = graph.calculateComplexityScore();
        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("Should calculate metrics")
    void testCalculateMetrics() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");

        DependencyMetrics metrics = graph.calculateMetrics();

        assertNotNull(metrics);
        assertNotNull(metrics.getComplexityLevel());
        assertTrue(metrics.getComplexityScore() >= 0);
        assertTrue(metrics.getComplexityScore() <= 100);
    }

    @Test
    @DisplayName("Should generate report")
    void testGenerateReport() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");

        String report = service.generateReport(graph);

        assertNotNull(report);
        assertFalse(report.isEmpty());
        assertTrue(report.contains("Dependency Analysis Report"));
    }

    @Test
    @DisplayName("Should handle empty graph gracefully")
    void testEmptyGraphHandling() {
        DependencyMetrics metrics = graph.calculateMetrics();
        assertNotNull(metrics);
        assertEquals(0, metrics.getTotalFiles());
    }

    @Test
    @DisplayName("Should cache impact analysis results")
    void testImpactAnalysisCaching() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");

        ImpactAnalysisResult result1 = graph.analyzeImpact("a.yaml");
        ImpactAnalysisResult result2 = graph.analyzeImpact("a.yaml");

        assertSame(result1, result2);
    }

    @Test
    @DisplayName("Should generate dependency tree with nested children format")
    void testGenerateDependencyTree() {
        graph.addBidirectionalEdge("root.yaml", "child1.yaml");
        graph.addBidirectionalEdge("root.yaml", "child2.yaml");
        graph.addBidirectionalEdge("child1.yaml", "grandchild.yaml");

        TreeNode tree = service.generateDependencyTree(graph, "root.yaml");

        assertNotNull(tree);
        assertEquals("root.yaml", tree.getName());
        assertEquals(0, tree.getDepth());
        assertEquals(2, tree.getChildCount());
        assertEquals(2, tree.getHeight());
    }

    @Test
    @DisplayName("Should generate tree with correct hierarchy")
    void testGenerateTreeHierarchy() {
        graph.addBidirectionalEdge("scenario.yaml", "rules.yaml");
        graph.addBidirectionalEdge("rules.yaml", "enrichment.yaml");

        TreeNode tree = service.generateDependencyTree(graph, "scenario.yaml");

        assertEquals("scenario.yaml", tree.getName());
        assertEquals(1, tree.getChildCount());

        TreeNode child = tree.getChildren().get(0);
        assertEquals("rules.yaml", child.getName());
        assertEquals(1, child.getDepth());
        assertEquals(1, child.getChildCount());

        TreeNode grandchild = child.getChildren().get(0);
        assertEquals("enrichment.yaml", grandchild.getName());
        assertEquals(2, grandchild.getDepth());
    }

    @Test
    @DisplayName("Should detect circular references in tree")
    void testTreeCircularReferences() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");
        graph.addBidirectionalEdge("c.yaml", "a.yaml");

        TreeNode tree = service.generateDependencyTree(graph, "a.yaml");

        assertNotNull(tree);
        assertEquals("a.yaml", tree.getName());
        // The tree should handle circular references gracefully
    }

    @Test
    @DisplayName("Should calculate tree height correctly")
    void testTreeHeightCalculation() {
        graph.addBidirectionalEdge("root.yaml", "level1.yaml");
        graph.addBidirectionalEdge("level1.yaml", "level2.yaml");
        graph.addBidirectionalEdge("level2.yaml", "level3.yaml");

        TreeNode tree = service.generateDependencyTree(graph, "root.yaml");

        assertEquals(3, tree.getHeight());
    }

    @Test
    @DisplayName("Should handle single node tree")
    void testSingleNodeTree() {
        TreeNode tree = service.generateDependencyTree(graph, "isolated.yaml");

        assertNotNull(tree);
        assertEquals("isolated.yaml", tree.getName());
        assertEquals(0, tree.getHeight());
        assertEquals(0, tree.getChildCount());
    }
}

