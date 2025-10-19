package dev.mars.apex.yaml.manager.util;

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

import dev.mars.apex.yaml.manager.model.CircularDependencyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CircularDependencyDetector.
 *
 * Tests cover:
 * - Simple circular dependencies (2-file cycles)
 * - Complex circular dependencies (3+ file cycles)
 * - Self-referencing cycles
 * - Multiple independent cycles
 * - Cycles with diamond patterns
 * - Severity classification
 * - Resolution suggestions
 * - Performance with large graphs
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@DisplayName("CircularDependencyDetector Tests")
class CircularDependencyDetectorTest {

    private Map<String, Set<String>> graph;
    private CircularDependencyDetector detector;

    @BeforeEach
    void setUp() {
        graph = new HashMap<>();
        detector = null;
    }

    @Test
    @DisplayName("Should detect no cycles in acyclic graph")
    void testNoCyclesInAcyclicGraph() {
        graph.put("a.yaml", new HashSet<>(Arrays.asList("b.yaml", "c.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("d.yaml")));
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("d.yaml")));
        graph.put("d.yaml", new HashSet<>());

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(0, cycles.size());
    }

    @Test
    @DisplayName("Should detect self-referencing cycle")
    void testSelfReferencingCycle() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(1, cycles.size());
        assertEquals(1, cycles.get(0).getCycleLength());
        assertEquals(CircularDependencyInfo.Severity.CRITICAL, cycles.get(0).getSeverity());
    }

    @Test
    @DisplayName("Should detect simple 2-file circular dependency")
    void testSimpleTwoFileCycle() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(1, cycles.size());
        assertEquals(2, cycles.get(0).getCycleLength());
        assertEquals(CircularDependencyInfo.Severity.CRITICAL, cycles.get(0).getSeverity());
    }

    @Test
    @DisplayName("Should detect 3-file circular dependency")
    void testThreeFileCycle() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("c.yaml")));
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(1, cycles.size());
        assertEquals(3, cycles.get(0).getCycleLength());
        assertEquals(CircularDependencyInfo.Severity.HIGH, cycles.get(0).getSeverity());
    }

    @Test
    @DisplayName("Should detect 4-file circular dependency")
    void testFourFileCycle() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("c.yaml")));
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("d.yaml")));
        graph.put("d.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(1, cycles.size());
        assertEquals(4, cycles.get(0).getCycleLength());
        assertEquals(CircularDependencyInfo.Severity.HIGH, cycles.get(0).getSeverity());
    }

    @Test
    @DisplayName("Should detect multiple independent cycles")
    void testMultipleIndependentCycles() {
        // Cycle 1: a -> b -> a
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        // Cycle 2: c -> d -> c
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("d.yaml")));
        graph.put("d.yaml", new HashSet<>(Collections.singletonList("c.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(2, cycles.size());
    }

    @Test
    @DisplayName("Should identify files in cycles")
    void testIdentifyFilesInCycles() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("a.yaml")));
        graph.put("c.yaml", new HashSet<>());

        detector = new CircularDependencyDetector(graph);
        detector.detectAllCycles();

        Set<String> filesInCycles = detector.getFilesInCycles();
        assertTrue(filesInCycles.contains("a.yaml"));
        assertTrue(filesInCycles.contains("b.yaml"));
        assertFalse(filesInCycles.contains("c.yaml"));
    }

    @Test
    @DisplayName("Should get cycles for specific file")
    void testGetCyclesForFile() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("c.yaml")));
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        detector.detectAllCycles();

        List<CircularDependencyInfo> cyclesForA = detector.getCyclesForFile("a.yaml");
        assertEquals(1, cyclesForA.size());
        assertTrue(cyclesForA.get(0).containsFile("a.yaml"));
    }

    @Test
    @DisplayName("Should classify severity correctly")
    void testSeverityClassification() {
        // Self-reference: CRITICAL
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("a.yaml")));
        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();
        assertEquals(CircularDependencyInfo.Severity.CRITICAL, cycles.get(0).getSeverity());

        // 3-file cycle: HIGH
        graph.clear();
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("c.yaml")));
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("a.yaml")));
        detector = new CircularDependencyDetector(graph);
        cycles = detector.detectAllCycles();
        assertEquals(CircularDependencyInfo.Severity.HIGH, cycles.get(0).getSeverity());
    }

    @Test
    @DisplayName("Should suggest resolution strategy")
    void testResolutionSuggestion() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        String resolution = cycles.get(0).suggestResolution();
        assertNotNull(resolution);
        assertFalse(resolution.isEmpty());
        assertTrue(resolution.contains("a.yaml") || resolution.contains("b.yaml"));
    }

    @Test
    @DisplayName("Should format cycle path correctly")
    void testCyclePathFormatting() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("c.yaml")));
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        String pathString = cycles.get(0).getCyclePathAsString();
        assertTrue(pathString.contains("->"));
        // Path should contain the cycle files
        assertTrue(pathString.contains("a.yaml"));
        assertTrue(pathString.contains("b.yaml"));
        assertTrue(pathString.contains("c.yaml"));
    }

    @Test
    @DisplayName("Should get next file in cycle")
    void testGetNextFileInCycle() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("c.yaml")));
        graph.put("c.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();
        CircularDependencyInfo cycle = cycles.get(0);

        String nextFile = cycle.getNextFileInCycle("a.yaml");
        assertNotNull(nextFile);
        assertTrue(cycle.containsFile(nextFile));
    }

    @Test
    @DisplayName("Should generate detailed report")
    void testReportGeneration() {
        graph.put("a.yaml", new HashSet<>(Collections.singletonList("b.yaml")));
        graph.put("b.yaml", new HashSet<>(Collections.singletonList("a.yaml")));

        detector = new CircularDependencyDetector(graph);
        detector.detectAllCycles();

        String report = detector.generateReport();
        assertNotNull(report);
        assertTrue(report.contains("Circular Dependency Analysis Report"));
        assertTrue(report.contains("Total Cycles"));
        assertTrue(report.contains("a.yaml"));
    }

    @Test
    @DisplayName("Should handle empty graph")
    void testEmptyGraph() {
        detector = new CircularDependencyDetector(graph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(0, cycles.size());
        assertEquals(0, detector.getTotalCycleCount());
    }

    @Test
    @DisplayName("Should handle large graph efficiently")
    void testLargeGraphPerformance() {
        // Create a large acyclic graph
        for (int i = 0; i < 100; i++) {
            Set<String> deps = new HashSet<>();
            if (i < 99) {
                deps.add((i + 1) + ".yaml");
            }
            graph.put(i + ".yaml", deps);
        }

        detector = new CircularDependencyDetector(graph);
        long startTime = System.currentTimeMillis();
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(0, cycles.size());
        assertTrue(duration < 1000, "Detection should complete in less than 1 second");
    }
}

