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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircularDependencyInfo model.
 *
 * Tests cover:
 * - Cycle creation and properties
 * - Severity classification
 * - File position tracking
 * - Cycle path navigation
 * - Resolution suggestions
 * - Equality and hashing
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@DisplayName("CircularDependencyInfo Tests")
class CircularDependencyInfoTest {

    private CircularDependencyInfo cycleInfo;

    @Test
    @DisplayName("Should create cycle info from path")
    void testCycleCreation() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertNotNull(cycleInfo);
        assertEquals(3, cycleInfo.getCycleLength());
        assertEquals(3, cycleInfo.getFilesInCycle().size());
    }

    @Test
    @DisplayName("Should classify self-reference as CRITICAL")
    void testSelfReferenceSeverity() {
        List<String> path = Arrays.asList("a.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals(CircularDependencyInfo.Severity.CRITICAL, cycleInfo.getSeverity());
    }

    @Test
    @DisplayName("Should classify 2-file cycle as CRITICAL")
    void testTwoFileCycleSeverity() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals(CircularDependencyInfo.Severity.CRITICAL, cycleInfo.getSeverity());
    }

    @Test
    @DisplayName("Should classify 3-4 file cycle as HIGH")
    void testThreeToFourFileCycleSeverity() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals(CircularDependencyInfo.Severity.HIGH, cycleInfo.getSeverity());
    }

    @Test
    @DisplayName("Should classify 5-10 file cycle as MEDIUM")
    void testMediumCycleSeverity() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml", "d.yaml", "e.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals(CircularDependencyInfo.Severity.MEDIUM, cycleInfo.getSeverity());
    }

    @Test
    @DisplayName("Should classify 11+ file cycle as LOW")
    void testLargeCycleSeverity() {
        List<String> path = Arrays.asList(
            "a.yaml", "b.yaml", "c.yaml", "d.yaml", "e.yaml",
            "f.yaml", "g.yaml", "h.yaml", "i.yaml", "j.yaml", "k.yaml", "l.yaml"
        );
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals(CircularDependencyInfo.Severity.LOW, cycleInfo.getSeverity());
    }

    @Test
    @DisplayName("Should check if file is in cycle")
    void testContainsFile() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertTrue(cycleInfo.containsFile("a.yaml"));
        assertTrue(cycleInfo.containsFile("b.yaml"));
        assertFalse(cycleInfo.containsFile("d.yaml"));
    }

    @Test
    @DisplayName("Should get file position in cycle")
    void testGetFilePosition() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals(0, cycleInfo.getFilePosition("a.yaml"));
        assertEquals(1, cycleInfo.getFilePosition("b.yaml"));
        assertEquals(2, cycleInfo.getFilePosition("c.yaml"));
        assertEquals(-1, cycleInfo.getFilePosition("d.yaml"));
    }

    @Test
    @DisplayName("Should get next file in cycle")
    void testGetNextFileInCycle() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals("b.yaml", cycleInfo.getNextFileInCycle("a.yaml"));
        assertEquals("c.yaml", cycleInfo.getNextFileInCycle("b.yaml"));
        assertEquals("a.yaml", cycleInfo.getNextFileInCycle("c.yaml")); // Wraps around
    }

    @Test
    @DisplayName("Should get previous file in cycle")
    void testGetPreviousFileInCycle() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        assertEquals("c.yaml", cycleInfo.getPreviousFileInCycle("a.yaml")); // Wraps around
        assertEquals("a.yaml", cycleInfo.getPreviousFileInCycle("b.yaml"));
        assertEquals("b.yaml", cycleInfo.getPreviousFileInCycle("c.yaml"));
    }

    @Test
    @DisplayName("Should format cycle path as string")
    void testCyclePathFormatting() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        String formatted = cycleInfo.getCyclePathAsString();
        assertTrue(formatted.contains("->"));
        assertTrue(formatted.startsWith("a.yaml"));
        assertTrue(formatted.endsWith("a.yaml")); // Should complete the cycle
    }

    @Test
    @DisplayName("Should suggest resolution for self-reference")
    void testResolutionForSelfReference() {
        List<String> path = Arrays.asList("a.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        String resolution = cycleInfo.suggestResolution();
        assertNotNull(resolution);
        assertTrue(resolution.contains("Self-referencing"));
        assertTrue(resolution.contains("a.yaml"));
    }

    @Test
    @DisplayName("Should suggest resolution for 2-file cycle")
    void testResolutionForTwoFileCycle() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        String resolution = cycleInfo.suggestResolution();
        assertNotNull(resolution);
        assertTrue(resolution.contains("Bidirectional"));
        assertTrue(resolution.contains("a.yaml"));
        assertTrue(resolution.contains("b.yaml"));
    }

    @Test
    @DisplayName("Should suggest resolution for complex cycle")
    void testResolutionForComplexCycle() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        String resolution = cycleInfo.suggestResolution();
        assertNotNull(resolution);
        assertTrue(resolution.contains("3"));
        assertTrue(resolution.contains("->"));
    }

    @Test
    @DisplayName("Should cache resolution suggestion")
    void testResolutionCaching() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        String resolution1 = cycleInfo.suggestResolution();
        String resolution2 = cycleInfo.suggestResolution();

        assertSame(resolution1, resolution2);
    }

    @Test
    @DisplayName("Should allow custom resolution strategy")
    void testCustomResolutionStrategy() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        String customStrategy = "Extract common dependencies";
        cycleInfo.setResolutionStrategy(customStrategy);

        assertEquals(customStrategy, cycleInfo.suggestResolution());
    }

    @Test
    @DisplayName("Should track detection timestamp")
    void testDetectionTimestamp() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml");
        long beforeCreation = System.currentTimeMillis();
        cycleInfo = new CircularDependencyInfo(path);
        long afterCreation = System.currentTimeMillis();

        long detectedAt = cycleInfo.getDetectedAt();
        assertTrue(detectedAt >= beforeCreation);
        assertTrue(detectedAt <= afterCreation);
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void testEquality() {
        List<String> path1 = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        List<String> path2 = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        List<String> path3 = Arrays.asList("a.yaml", "b.yaml");

        CircularDependencyInfo cycle1 = new CircularDependencyInfo(path1);
        CircularDependencyInfo cycle2 = new CircularDependencyInfo(path2);
        CircularDependencyInfo cycle3 = new CircularDependencyInfo(path3);

        assertEquals(cycle1, cycle2);
        assertNotEquals(cycle1, cycle3);
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void testHashCode() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        CircularDependencyInfo cycle1 = new CircularDependencyInfo(path);
        CircularDependencyInfo cycle2 = new CircularDependencyInfo(path);

        assertEquals(cycle1.hashCode(), cycle2.hashCode());
    }

    @Test
    @DisplayName("Should provide meaningful toString")
    void testToString() {
        List<String> path = Arrays.asList("a.yaml", "b.yaml", "c.yaml");
        cycleInfo = new CircularDependencyInfo(path);

        String str = cycleInfo.toString();
        assertNotNull(str);
        assertTrue(str.contains("CircularDependencyInfo"));
        assertTrue(str.contains("HIGH"));
    }
}

