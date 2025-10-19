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

import dev.mars.apex.core.util.YamlDependencyGraph;
import dev.mars.apex.core.util.YamlNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced YAML dependency graph with bidirectional edges and impact analysis.
 *
 * Extends the core YamlDependencyGraph with:
 * - Bidirectional edges (forward: depends-on, reverse: used-by)
 * - Impact analysis (what breaks if I change this file?)
 * - Complexity metrics (depth, breadth, criticality)
 * - Orphaned file detection
 * - Critical path identification
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
public class EnhancedYamlDependencyGraph extends YamlDependencyGraph {

    private final Map<String, Set<String>> forwardEdges;  // file -> files it depends on
    private final Map<String, Set<String>> reverseEdges;  // file -> files that depend on it
    private final Map<String, Integer> depthMap;          // file -> depth in graph
    private final Map<String, Integer> breadthMap;        // file -> number of dependents
    private DependencyMetrics metrics;
    private Map<String, ImpactAnalysisResult> impactCache;

    public EnhancedYamlDependencyGraph(String rootFile) {
        super(rootFile);
        this.forwardEdges = new HashMap<>();
        this.reverseEdges = new HashMap<>();
        this.depthMap = new HashMap<>();
        this.breadthMap = new HashMap<>();
        this.impactCache = new HashMap<>();
    }

    /**
     * Add a bidirectional edge to the graph.
     */
    public void addBidirectionalEdge(String sourceFile, String targetFile) {
        // Forward edge: source depends on target
        forwardEdges.computeIfAbsent(sourceFile, k -> new HashSet<>()).add(targetFile);

        // Reverse edge: target is used by source
        reverseEdges.computeIfAbsent(targetFile, k -> new HashSet<>()).add(sourceFile);
    }

    /**
     * Get all files that the specified file depends on (direct dependencies).
     */
    public Set<String> getDirectDependencies(String filePath) {
        return forwardEdges.getOrDefault(filePath, new HashSet<>());
    }

    /**
     * Get all files that depend on the specified file (direct dependents).
     */
    public Set<String> getDirectDependents(String filePath) {
        return reverseEdges.getOrDefault(filePath, new HashSet<>());
    }

    /**
     * Get all files that the specified file transitively depends on.
     */
    public Set<String> getTransitiveDependencies(String filePath) {
        Set<String> result = new HashSet<>();
        Set<String> visited = new HashSet<>();
        collectTransitiveDependencies(filePath, result, visited);
        return result;
    }

    /**
     * Get all files that transitively depend on the specified file.
     */
    public Set<String> getTransitiveDependents(String filePath) {
        Set<String> result = new HashSet<>();
        Set<String> visited = new HashSet<>();
        collectTransitiveDependents(filePath, result, visited);
        return result;
    }

    /**
     * Perform impact analysis for the specified file.
     */
    public ImpactAnalysisResult analyzeImpact(String filePath) {
        if (impactCache.containsKey(filePath)) {
            return impactCache.get(filePath);
        }

        ImpactAnalysisResult result = new ImpactAnalysisResult(filePath);

        // Direct dependents
        Set<String> directDependents = getDirectDependents(filePath);
        result.setDirectDependents(directDependents);

        // Transitive dependents
        Set<String> transitiveDependents = getTransitiveDependents(filePath);
        result.setTransitiveDependents(transitiveDependents);

        // Direct dependencies
        Set<String> directDependencies = getDirectDependencies(filePath);
        result.setDirectDependencies(directDependencies);

        // Transitive dependencies
        Set<String> transitiveDependencies = getTransitiveDependencies(filePath);
        result.setTransitiveDependencies(transitiveDependencies);

        // Calculate impact metrics
        int impactRadius = transitiveDependents.size();
        result.setImpactRadius(impactRadius);

        // Impact score: 0-100 based on number of affected files
        int totalFiles = getAllNodes().size();
        int impactScore = totalFiles > 0 ? (impactRadius * 100) / totalFiles : 0;
        result.setImpactScore(Math.min(100, impactScore));

        // Calculate risk level based on impact score
        result.calculateRiskLevel();

        impactCache.put(filePath, result);
        return result;
    }

    /**
     * Calculate comprehensive metrics for the graph.
     */
    public DependencyMetrics calculateMetrics() {
        if (metrics != null) {
            return metrics;
        }

        metrics = new DependencyMetrics();

        // Basic metrics
        metrics.setTotalFiles(getAllNodes().size());
        metrics.setMaxDepth(getMaxDepth());

        // Calculate average depth
        if (!depthMap.isEmpty()) {
            double avgDepth = depthMap.values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            metrics.setAverageDepth(avgDepth);
        }

        // Count dependencies
        int totalDeps = forwardEdges.values().stream()
                .mapToInt(Set::size)
                .sum();
        metrics.setTotalDependencies(totalDeps);

        int totalDependents = reverseEdges.values().stream()
                .mapToInt(Set::size)
                .sum();
        metrics.setTotalDependents(totalDependents);

        // Detect circular dependencies
        Set<String> circularDeps = new HashSet<>();
        for (List<String> cycle : findCircularDependencies()) {
            circularDeps.addAll(cycle);
        }
        metrics.setCircularDependencies(circularDeps);

        // Find orphaned files
        Set<String> orphaned = new HashSet<>();
        for (YamlNode node : getAllNodes()) {
            if (!node.hasDependencies() && !node.isReferenced()) {
                orphaned.add(node.getFilePath());
            }
        }
        metrics.setOrphanedFiles(orphaned);

        // Find critical files (high breadth)
        Set<String> critical = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : reverseEdges.entrySet()) {
            if (entry.getValue().size() > 3) { // Files depended on by 3+ files
                critical.add(entry.getKey());
            }
        }
        metrics.setCriticalFiles(critical);

        // Calculate complexity score
        int complexityScore = calculateComplexityScore();
        metrics.setComplexityScore(complexityScore);

        return metrics;
    }

    /**
     * Calculate complexity score (0-100).
     */
    public int calculateComplexityScore() {
        int score = 0;

        // Factor 1: Depth (max 30 points)
        int depthScore = Math.min(30, getMaxDepth() * 5);
        score += depthScore;

        // Factor 2: Total dependencies (max 30 points)
        int totalDeps = forwardEdges.values().stream()
                .mapToInt(Set::size)
                .sum();
        int depScore = Math.min(30, (totalDeps / 2));
        score += depScore;

        // Factor 3: Circular dependencies (max 20 points)
        int circularCount = findCircularDependencies().size();
        int circScore = Math.min(20, circularCount * 5);
        score += circScore;

        // Factor 4: Orphaned files (max 10 points)
        long orphanedCount = getAllNodes().stream()
                .filter(n -> !n.hasDependencies() && !n.isReferenced())
                .count();
        int orphanScore = Math.min(10, (int) (orphanedCount * 2));
        score += orphanScore;

        return Math.min(100, score);
    }

    /**
     * Recursively collect transitive dependencies.
     */
    private void collectTransitiveDependencies(String filePath, Set<String> result, Set<String> visited) {
        if (visited.contains(filePath)) {
            return;
        }
        visited.add(filePath);

        Set<String> directDeps = getDirectDependencies(filePath);
        for (String dep : directDeps) {
            result.add(dep);
            collectTransitiveDependencies(dep, result, visited);
        }
    }

    /**
     * Recursively collect transitive dependents.
     */
    private void collectTransitiveDependents(String filePath, Set<String> result, Set<String> visited) {
        if (visited.contains(filePath)) {
            return;
        }
        visited.add(filePath);

        Set<String> directDependents = getDirectDependents(filePath);
        for (String dependent : directDependents) {
            result.add(dependent);
            collectTransitiveDependents(dependent, result, visited);
        }
    }

    /**
     * Get all orphaned files (not referenced and don't reference anything).
     */
    public Set<String> getOrphanedFiles() {
        return getAllNodes().stream()
                .filter(n -> !n.hasDependencies() && !n.isReferenced())
                .map(YamlNode::getFilePath)
                .collect(Collectors.toSet());
    }

    /**
     * Get all critical files (referenced by many other files).
     */
    public Set<String> getCriticalFiles() {
        return reverseEdges.entrySet().stream()
                .filter(e -> e.getValue().size() > 3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Get metrics for this graph.
     */
    public DependencyMetrics getMetrics() {
        if (metrics == null) {
            calculateMetrics();
        }
        return metrics;
    }

    /**
     * Get forward edges (for circular dependency detection).
     */
    public Map<String, Set<String>> getForwardEdges() {
        return new HashMap<>(forwardEdges);
    }

    /**
     * Get backward edges (reverse dependencies).
     */
    public Map<String, Set<String>> getBackwardEdges() {
        return new HashMap<>(reverseEdges);
    }
}

