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

import dev.mars.apex.yaml.manager.model.CircularDependencyInfo;
import dev.mars.apex.yaml.manager.util.CircularDependencyDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for circular dependency detection with real APEX YAML scenarios.
 *
 * Tests cover:
 * - Circular dependencies in real APEX configuration hierarchies
 * - Compliance and risk rule circular references
 * - Enrichment cross-references
 * - Scenario registry dependencies
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@DisplayName("Circular Dependency Integration Tests")
class CircularDependencyIntegrationTest {

    private Map<String, Set<String>> apexDependencyGraph;
    private CircularDependencyDetector detector;

    @BeforeEach
    void setUp() {
        apexDependencyGraph = new HashMap<>();
    }

    @Test
    @DisplayName("Should detect circular dependency in compliance and risk rules")
    void testComplianceRiskCircularDependency() {
        // Simulate APEX configuration hierarchy:
        // scenario-registry -> compliance-rules -> risk-rules -> compliance-rules (CYCLE)
        apexDependencyGraph.put("scenario-registry.yaml", 
            new HashSet<>(Arrays.asList("compliance-rules.yaml", "risk-rules.yaml")));
        apexDependencyGraph.put("compliance-rules.yaml", 
            new HashSet<>(Collections.singletonList("risk-rules.yaml")));
        apexDependencyGraph.put("risk-rules.yaml", 
            new HashSet<>(Collections.singletonList("compliance-rules.yaml")));
        apexDependencyGraph.put("trade-validation-rules.yaml", 
            new HashSet<>(Collections.singletonList("trade-enrichment.yaml")));
        apexDependencyGraph.put("trade-enrichment.yaml", new HashSet<>());

        detector = new CircularDependencyDetector(apexDependencyGraph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(1, cycles.size());
        CircularDependencyInfo cycle = cycles.get(0);
        assertEquals(2, cycle.getCycleLength());
        assertEquals(CircularDependencyInfo.Severity.CRITICAL, cycle.getSeverity());
        assertTrue(cycle.containsFile("compliance-rules.yaml"));
        assertTrue(cycle.containsFile("risk-rules.yaml"));
    }

    @Test
    @DisplayName("Should identify files in circular dependency")
    void testIdentifyFilesInCircularDependency() {
        apexDependencyGraph.put("compliance-rules.yaml", 
            new HashSet<>(Collections.singletonList("risk-rules.yaml")));
        apexDependencyGraph.put("risk-rules.yaml", 
            new HashSet<>(Collections.singletonList("compliance-rules.yaml")));
        apexDependencyGraph.put("trade-validation-rules.yaml", 
            new HashSet<>(Collections.singletonList("trade-enrichment.yaml")));
        apexDependencyGraph.put("trade-enrichment.yaml", new HashSet<>());

        detector = new CircularDependencyDetector(apexDependencyGraph);
        detector.detectAllCycles();

        Set<String> filesInCycles = detector.getFilesInCycles();
        assertEquals(2, filesInCycles.size());
        assertTrue(filesInCycles.contains("compliance-rules.yaml"));
        assertTrue(filesInCycles.contains("risk-rules.yaml"));
        assertFalse(filesInCycles.contains("trade-validation-rules.yaml"));
    }

    @Test
    @DisplayName("Should detect cycles for specific file")
    void testDetectCyclesForSpecificFile() {
        apexDependencyGraph.put("compliance-rules.yaml", 
            new HashSet<>(Collections.singletonList("risk-rules.yaml")));
        apexDependencyGraph.put("risk-rules.yaml", 
            new HashSet<>(Collections.singletonList("compliance-rules.yaml")));

        detector = new CircularDependencyDetector(apexDependencyGraph);
        detector.detectAllCycles();

        List<CircularDependencyInfo> cyclesForCompliance = detector.getCyclesForFile("compliance-rules.yaml");
        assertEquals(1, cyclesForCompliance.size());

        List<CircularDependencyInfo> cyclesForRisk = detector.getCyclesForFile("risk-rules.yaml");
        assertEquals(1, cyclesForRisk.size());

        List<CircularDependencyInfo> cyclesForTrade = detector.getCyclesForFile("trade-validation-rules.yaml");
        assertEquals(0, cyclesForTrade.size());
    }

    @Test
    @DisplayName("Should suggest resolution for compliance-risk cycle")
    void testResolutionSuggestionForApexCycle() {
        apexDependencyGraph.put("compliance-rules.yaml", 
            new HashSet<>(Collections.singletonList("risk-rules.yaml")));
        apexDependencyGraph.put("risk-rules.yaml", 
            new HashSet<>(Collections.singletonList("compliance-rules.yaml")));

        detector = new CircularDependencyDetector(apexDependencyGraph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        String resolution = cycles.get(0).suggestResolution();
        assertNotNull(resolution);
        assertFalse(resolution.isEmpty());
        assertTrue(resolution.contains("compliance-rules.yaml") || resolution.contains("risk-rules.yaml"));
    }

    @Test
    @DisplayName("Should handle complex APEX scenario with multiple rule configurations")
    void testComplexApexScenario() {
        // Scenario registry references multiple rule configurations
        apexDependencyGraph.put("scenario-registry.yaml", 
            new HashSet<>(Arrays.asList(
                "trade-validation-rules.yaml",
                "compliance-rules.yaml",
                "risk-rules.yaml"
            )));

        // Trade validation references enrichment
        apexDependencyGraph.put("trade-validation-rules.yaml", 
            new HashSet<>(Collections.singletonList("trade-enrichment.yaml")));

        // Enrichments reference datasets
        apexDependencyGraph.put("trade-enrichment.yaml", 
            new HashSet<>(Arrays.asList(
                "counterparty-dataset.yaml",
                "market-dataset.yaml"
            )));

        // Compliance and risk have circular reference
        apexDependencyGraph.put("compliance-rules.yaml", 
            new HashSet<>(Collections.singletonList("risk-rules.yaml")));
        apexDependencyGraph.put("risk-rules.yaml", 
            new HashSet<>(Collections.singletonList("compliance-rules.yaml")));

        // Datasets
        apexDependencyGraph.put("counterparty-dataset.yaml", new HashSet<>());
        apexDependencyGraph.put("market-dataset.yaml", new HashSet<>());

        detector = new CircularDependencyDetector(apexDependencyGraph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(1, cycles.size());
        assertEquals(2, cycles.get(0).getCycleLength());
    }

    @Test
    @DisplayName("Should generate detailed report for APEX circular dependencies")
    void testGenerateApexCircularDependencyReport() {
        apexDependencyGraph.put("compliance-rules.yaml", 
            new HashSet<>(Collections.singletonList("risk-rules.yaml")));
        apexDependencyGraph.put("risk-rules.yaml", 
            new HashSet<>(Collections.singletonList("compliance-rules.yaml")));

        detector = new CircularDependencyDetector(apexDependencyGraph);
        detector.detectAllCycles();

        String report = detector.generateReport();
        assertNotNull(report);
        assertTrue(report.contains("Circular Dependency Analysis Report"));
        assertTrue(report.contains("compliance-rules.yaml"));
        assertTrue(report.contains("risk-rules.yaml"));
        assertTrue(report.contains("CRITICAL"));
    }

    @Test
    @DisplayName("Should handle acyclic APEX configuration")
    void testAcyclicApexConfiguration() {
        // Acyclic hierarchy: registry -> rules -> enrichments -> datasets
        apexDependencyGraph.put("scenario-registry.yaml", 
            new HashSet<>(Collections.singletonList("trade-validation-rules.yaml")));
        apexDependencyGraph.put("trade-validation-rules.yaml", 
            new HashSet<>(Collections.singletonList("trade-enrichment.yaml")));
        apexDependencyGraph.put("trade-enrichment.yaml", 
            new HashSet<>(Collections.singletonList("counterparty-dataset.yaml")));
        apexDependencyGraph.put("counterparty-dataset.yaml", new HashSet<>());

        detector = new CircularDependencyDetector(apexDependencyGraph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();

        assertEquals(0, cycles.size());
        assertEquals(0, detector.getTotalCycleCount());
    }

    @Test
    @DisplayName("Should classify severity for different cycle lengths")
    void testSeverityClassificationForApexCycles() {
        // 2-file cycle: CRITICAL
        apexDependencyGraph.put("file1.yaml", 
            new HashSet<>(Collections.singletonList("file2.yaml")));
        apexDependencyGraph.put("file2.yaml", 
            new HashSet<>(Collections.singletonList("file1.yaml")));

        detector = new CircularDependencyDetector(apexDependencyGraph);
        List<CircularDependencyInfo> cycles = detector.detectAllCycles();
        assertEquals(CircularDependencyInfo.Severity.CRITICAL, cycles.get(0).getSeverity());

        // 3-file cycle: HIGH
        apexDependencyGraph.clear();
        apexDependencyGraph.put("compliance-rules.yaml", 
            new HashSet<>(Collections.singletonList("risk-rules.yaml")));
        apexDependencyGraph.put("risk-rules.yaml", 
            new HashSet<>(Collections.singletonList("trade-validation-rules.yaml")));
        apexDependencyGraph.put("trade-validation-rules.yaml", 
            new HashSet<>(Collections.singletonList("compliance-rules.yaml")));

        detector = new CircularDependencyDetector(apexDependencyGraph);
        cycles = detector.detectAllCycles();
        assertEquals(CircularDependencyInfo.Severity.HIGH, cycles.get(0).getSeverity());
    }
}

