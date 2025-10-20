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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnhancedYamlDependencyGraph.
 *
 * Tests cover:
 * - Graph construction
 * - Bidirectional edge management
 * - Dependency calculations
 * - Impact analysis
 * - Metrics calculation
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@DisplayName("EnhancedYamlDependencyGraph Tests")
class EnhancedYamlDependencyGraphTest {

    private EnhancedYamlDependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new EnhancedYamlDependencyGraph("root.yaml");
    }

    @Test
    @DisplayName("Should create graph with root file")
    void testGraphCreation() {
        assertNotNull(graph);
        assertEquals("root.yaml", graph.getRootFile());
    }

    @Test
    @DisplayName("Should initialize with zero files")
    void testInitialState() {
        assertEquals(0, graph.getTotalFiles());
        assertEquals(0, graph.getMaxDepth());
    }

    @Test
    @DisplayName("Should add single bidirectional edge")
    void testAddSingleEdge() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");

        Set<String> aDeps = graph.getDirectDependencies("a.yaml");
        Set<String> bDependents = graph.getDirectDependents("b.yaml");

        assertTrue(aDeps.contains("b.yaml"));
        assertTrue(bDependents.contains("a.yaml"));
    }

    @Test
    @DisplayName("Should add multiple edges")
    void testAddMultipleEdges() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("a.yaml", "c.yaml");
        graph.addBidirectionalEdge("b.yaml", "d.yaml");

        Set<String> aDeps = graph.getDirectDependencies("a.yaml");
        assertEquals(2, aDeps.size());
    }

    @Test
    @DisplayName("Should handle self-referencing edges")
    void testSelfReferencingEdge() {
        graph.addBidirectionalEdge("a.yaml", "a.yaml");
        Set<String> deps = graph.getDirectDependencies("a.yaml");
        assertTrue(deps.contains("a.yaml"));
    }

    @Test
    @DisplayName("Should calculate empty dependencies for isolated node")
    void testIsolatedNodeDependencies() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");

        Set<String> cDeps = graph.getDirectDependencies("c.yaml");
        assertTrue(cDeps.isEmpty());
    }

    @Test
    @DisplayName("Should calculate transitive dependencies correctly")
    void testTransitiveDependenciesChain() {
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
    @DisplayName("Should calculate transitive dependents correctly")
    void testTransitiveDependentsChain() {
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
    @DisplayName("Should handle diamond dependency pattern")
    void testDiamondDependencyPattern() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("a.yaml", "c.yaml");
        graph.addBidirectionalEdge("b.yaml", "d.yaml");
        graph.addBidirectionalEdge("c.yaml", "d.yaml");

        Set<String> aDeps = graph.getDirectDependencies("a.yaml");
        assertEquals(2, aDeps.size());

        Set<String> dDependents = graph.getDirectDependents("d.yaml");
        assertEquals(2, dDependents.size());
    }

    @Test
    @DisplayName("Should perform impact analysis")
    void testImpactAnalysis() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");

        ImpactAnalysisResult impact = graph.analyzeImpact("b.yaml");

        assertNotNull(impact);
        assertEquals("b.yaml", impact.getAnalyzedFile());
        assertEquals(1, impact.getDirectDependents().size());
        assertEquals(1, impact.getDirectDependencies().size());
    }

    @Test
    @DisplayName("Should calculate complexity score between 0 and 100")
    void testComplexityScoreBounds() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");

        int score = graph.calculateComplexityScore();
        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("Should calculate metrics")
    void testMetricsCalculation() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        graph.addBidirectionalEdge("b.yaml", "c.yaml");

        DependencyMetrics metrics = graph.calculateMetrics();

        assertNotNull(metrics);
        assertNotNull(metrics.getComplexityLevel());
        assertTrue(metrics.getComplexityScore() >= 0);
        assertTrue(metrics.getComplexityScore() <= 100);
    }

    @Test
    @DisplayName("Should identify orphaned files")
    void testOrphanedFileIdentification() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");
        // c.yaml is not added to any edge

        Set<String> orphaned = graph.getOrphanedFiles();
        // Orphaned files should not include files with edges
        assertFalse(orphaned.contains("a.yaml"));
        assertFalse(orphaned.contains("b.yaml"));
    }

    @Test
    @DisplayName("Should identify critical files")
    void testCriticalFileIdentification() {
        // Create a hub file
        graph.addBidirectionalEdge("a.yaml", "hub.yaml");
        graph.addBidirectionalEdge("b.yaml", "hub.yaml");
        graph.addBidirectionalEdge("c.yaml", "hub.yaml");
        graph.addBidirectionalEdge("d.yaml", "hub.yaml");

        Set<String> critical = graph.getCriticalFiles();
        assertTrue(critical.contains("hub.yaml"));
    }

    @Test
    @DisplayName("Should handle empty graph metrics")
    void testEmptyGraphMetrics() {
        DependencyMetrics metrics = graph.calculateMetrics();

        assertNotNull(metrics);
        assertEquals(0, metrics.getTotalFiles());
        assertEquals(0, metrics.getMaxDepth());
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
    @DisplayName("Should handle complex graph with multiple levels")
    void testComplexMultiLevelGraph() {
        // Create a 4-level dependency chain
        graph.addBidirectionalEdge("level1.yaml", "level2a.yaml");
        graph.addBidirectionalEdge("level1.yaml", "level2b.yaml");
        graph.addBidirectionalEdge("level2a.yaml", "level3a.yaml");
        graph.addBidirectionalEdge("level2b.yaml", "level3b.yaml");
        graph.addBidirectionalEdge("level3a.yaml", "level4.yaml");
        graph.addBidirectionalEdge("level3b.yaml", "level4.yaml");

        Set<String> transitive = graph.getTransitiveDependencies("level1.yaml");
        assertTrue(transitive.size() >= 3);

        DependencyMetrics metrics = graph.calculateMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getComplexityScore() >= 0);
    }

    @Test
    @DisplayName("Should return non-null complexity level")
    void testComplexityLevel() {
        graph.addBidirectionalEdge("a.yaml", "b.yaml");

        DependencyMetrics metrics = graph.calculateMetrics();
        assertNotNull(metrics.getComplexityLevel());
        assertFalse(metrics.getComplexityLevel().isEmpty());
    }
}

