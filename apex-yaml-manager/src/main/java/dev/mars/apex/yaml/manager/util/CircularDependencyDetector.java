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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for detecting and analyzing circular dependencies in YAML dependency graphs.
 *
 * Provides comprehensive circular dependency detection with:
 * - Detection of all circular dependency cycles
 * - Cycle path extraction and analysis
 * - Severity classification
 * - Resolution strategy suggestions
 * - Performance optimization for large graphs
 *
 * Uses depth-first search (DFS) with cycle detection to identify all cycles
 * in the dependency graph.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class CircularDependencyDetector {

    private static final Logger logger = LoggerFactory.getLogger(CircularDependencyDetector.class);

    private final Map<String, Set<String>> dependencyGraph;
    private final List<CircularDependencyInfo> detectedCycles;
    private final Set<String> allFilesInCycles;

    /**
     * Create a detector for the given dependency graph.
     *
     * @param dependencyGraph Map where key is a file and value is set of files it depends on
     */
    public CircularDependencyDetector(Map<String, Set<String>> dependencyGraph) {
        this.dependencyGraph = new HashMap<>(dependencyGraph);
        this.detectedCycles = new ArrayList<>();
        this.allFilesInCycles = new HashSet<>();
    }

    /**
     * Detect all circular dependencies in the graph.
     *
     * @return List of CircularDependencyInfo objects representing each cycle
     */
    public List<CircularDependencyInfo> detectAllCycles() {
        logger.debug("Starting circular dependency detection for {} files", dependencyGraph.size());

        detectedCycles.clear();
        allFilesInCycles.clear();

        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String file : dependencyGraph.keySet()) {
            if (!visited.contains(file)) {
                detectCyclesFromNode(file, visited, recursionStack, new ArrayList<>());
            }
        }

        logger.info("Circular dependency detection complete: {} cycles found", detectedCycles.size());
        return new ArrayList<>(detectedCycles);
    }

    /**
     * Detect cycles starting from a specific node using DFS.
     */
    private void detectCyclesFromNode(String node, Set<String> visited, Set<String> recursionStack,
                                     List<String> currentPath) {
        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);

        Set<String> dependencies = dependencyGraph.getOrDefault(node, new HashSet<>());

        for (String dependency : dependencies) {
            if (!visited.contains(dependency)) {
                detectCyclesFromNode(dependency, visited, recursionStack, currentPath);
            } else if (recursionStack.contains(dependency)) {
                // Found a cycle
                int cycleStart = currentPath.indexOf(dependency);
                if (cycleStart >= 0) {
                    List<String> cycle = new ArrayList<>(currentPath.subList(cycleStart, currentPath.size()));
                    CircularDependencyInfo cycleInfo = new CircularDependencyInfo(cycle);

                    // Only add if not already detected (avoid duplicates)
                    if (!isDuplicateCycle(cycleInfo)) {
                        detectedCycles.add(cycleInfo);
                        allFilesInCycles.addAll(cycle);
                        logger.debug("Cycle detected: {}", cycleInfo.getCyclePathAsString());
                    }
                }
            }
        }

        recursionStack.remove(node);
        currentPath.remove(currentPath.size() - 1);
    }

    /**
     * Check if a cycle is already detected (avoid duplicates).
     */
    private boolean isDuplicateCycle(CircularDependencyInfo newCycle) {
        return detectedCycles.stream()
                .anyMatch(existing -> existing.getFilesInCycle().equals(newCycle.getFilesInCycle()));
    }

    /**
     * Get all files involved in any circular dependency.
     */
    public Set<String> getFilesInCycles() {
        return new HashSet<>(allFilesInCycles);
    }

    /**
     * Get cycles involving a specific file.
     */
    public List<CircularDependencyInfo> getCyclesForFile(String filePath) {
        return detectedCycles.stream()
                .filter(cycle -> cycle.containsFile(filePath))
                .collect(Collectors.toList());
    }

    /**
     * Get cycles by severity level.
     */
    public List<CircularDependencyInfo> getCyclesBySeverity(CircularDependencyInfo.Severity severity) {
        return detectedCycles.stream()
                .filter(cycle -> cycle.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    /**
     * Get the most critical cycles (sorted by severity).
     */
    public List<CircularDependencyInfo> getMostCriticalCycles(int limit) {
        return detectedCycles.stream()
                .sorted((c1, c2) -> {
                    int severityCompare = c1.getSeverity().compareTo(c2.getSeverity());
                    if (severityCompare != 0) return severityCompare;
                    return Integer.compare(c1.getCycleLength(), c2.getCycleLength());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Check if a specific file is in a circular dependency.
     */
    public boolean isFileInCycle(String filePath) {
        return allFilesInCycles.contains(filePath);
    }

    /**
     * Get total number of cycles detected.
     */
    public int getTotalCycleCount() {
        return detectedCycles.size();
    }

    /**
     * Get total number of files involved in cycles.
     */
    public int getTotalFilesInCycles() {
        return allFilesInCycles.size();
    }

    /**
     * Generate a detailed report of all circular dependencies.
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("Circular Dependency Analysis Report\n");
        report.append("====================================\n\n");

        report.append("Summary:\n");
        report.append("├── Total Cycles: ").append(detectedCycles.size()).append("\n");
        report.append("├── Files in Cycles: ").append(allFilesInCycles.size()).append("\n");

        long criticalCount = detectedCycles.stream()
                .filter(c -> c.getSeverity() == CircularDependencyInfo.Severity.CRITICAL)
                .count();
        report.append("└── Critical Cycles: ").append(criticalCount).append("\n\n");

        if (!detectedCycles.isEmpty()) {
            report.append("Detected Cycles:\n");
            for (int i = 0; i < detectedCycles.size(); i++) {
                CircularDependencyInfo cycle = detectedCycles.get(i);
                report.append(String.format("%d. [%s] %s\n", i + 1, cycle.getSeverity(), cycle.getCyclePathAsString()));
                report.append("   Resolution: ").append(cycle.suggestResolution()).append("\n\n");
            }
        }

        return report.toString();
    }

    /**
     * Get all detected cycles.
     */
    public List<CircularDependencyInfo> getAllCycles() {
        return new ArrayList<>(detectedCycles);
    }
}

