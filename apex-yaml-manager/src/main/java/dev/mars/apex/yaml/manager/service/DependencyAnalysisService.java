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

import dev.mars.apex.core.util.YamlDependencyAnalyzer;
import dev.mars.apex.core.util.YamlDependencyGraph;
import dev.mars.apex.core.util.YamlNode;
import dev.mars.apex.yaml.manager.model.CircularDependencyInfo;
import dev.mars.apex.yaml.manager.model.DependencyMetrics;
import dev.mars.apex.yaml.manager.model.EnhancedYamlDependencyGraph;
import dev.mars.apex.yaml.manager.model.ImpactAnalysisResult;
import dev.mars.apex.yaml.manager.model.TreeNode;
import dev.mars.apex.yaml.manager.model.YamlContentSummary;
import dev.mars.apex.yaml.manager.util.CircularDependencyDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for analyzing YAML file dependencies.
 *
 * Provides operations for:
 * - Analyzing dependency graphs
 * - Performing impact analysis
 * - Detecting circular dependencies
 * - Calculating complexity metrics
 * - Identifying orphaned and critical files
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@Service
public class DependencyAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalysisService.class);

    private final YamlDependencyAnalyzer yamlAnalyzer;

    @Autowired(required = false)
    private YamlContentAnalyzer contentAnalyzer;

    public DependencyAnalysisService() {
        this.yamlAnalyzer = new YamlDependencyAnalyzer();
        logger.info("DependencyAnalysisService initialized");
    }

    /**
     * Analyze dependencies starting from a root YAML file.
     */
    public EnhancedYamlDependencyGraph analyzeDependencies(String rootFilePath) {
        logger.info("Analyzing dependencies for: {}", rootFilePath);

        try {
            // Determine base path and root argument for analyzer
            java.nio.file.Path rootPath = java.nio.file.Paths.get(rootFilePath);
            boolean isAbsolute = rootPath.isAbsolute();
            YamlDependencyAnalyzer analyzerToUse = this.yamlAnalyzer;
            String analyzerRootArg = rootFilePath;

            if (isAbsolute) {
                String basePath = rootPath.getParent().toString();
                analyzerToUse = new YamlDependencyAnalyzer(basePath);
                analyzerRootArg = rootPath.getFileName().toString();
                logger.debug("Using dynamic basePath for analysis: {} with root {}", basePath, analyzerRootArg);
            }

            // Use core analyzer to build basic graph
            YamlDependencyGraph basicGraph = analyzerToUse.analyzeYamlDependencies(analyzerRootArg);

            // Enhance with bidirectional edges
            // Use the analyzer's root key for graph root so keys match edge maps
            EnhancedYamlDependencyGraph enhancedGraph = new EnhancedYamlDependencyGraph(analyzerRootArg);

            // Copy nodes from basic graph
            for (YamlNode node : basicGraph.getAllNodes()) {
                enhancedGraph.addNode(node);
            }

            // Copy dependencies and build bidirectional edges
            for (var dependency : basicGraph.getAllDependencies()) {
                enhancedGraph.addDependency(dependency);
                enhancedGraph.addBidirectionalEdge(dependency.getSourceFile(), dependency.getTargetFile());
            }

            // Preserve computed maxDepth from the basic graph
            enhancedGraph.updateMaxDepth(basicGraph.getMaxDepth());

            logger.info("Dependency analysis complete: {} files, max depth: {}",
                    enhancedGraph.getTotalFiles(), enhancedGraph.getMaxDepth());

            return enhancedGraph;
        } catch (Exception e) {
            logger.error("Failed to analyze dependencies for: {}", rootFilePath, e);
            throw new RuntimeException("Dependency analysis failed", e);
        }
    }

    /**
     * Perform impact analysis for a specific file.
     */
    public ImpactAnalysisResult analyzeImpact(EnhancedYamlDependencyGraph graph, String filePath) {
        logger.debug("Analyzing impact for: {}", filePath);

        if (!graph.getAllReferencedFiles().contains(filePath)) {
            logger.warn("File not found in graph: {}", filePath);
            throw new IllegalArgumentException("File not found in dependency graph: " + filePath);
        }

        return graph.analyzeImpact(filePath);
    }

    /**
     * Get all circular dependencies in the graph.
     */
    public List<List<String>> findCircularDependencies(EnhancedYamlDependencyGraph graph) {
        logger.debug("Detecting circular dependencies");
        List<List<String>> cycles = graph.findCircularDependencies();
        logger.info("Found {} circular dependency cycles", cycles.size());
        return cycles;
    }

    /**
     * Get all orphaned files (not referenced and don't reference anything).
     */
    public Set<String> findOrphanedFiles(EnhancedYamlDependencyGraph graph) {
        logger.debug("Finding orphaned files");
        Set<String> orphaned = graph.getOrphanedFiles();
        logger.info("Found {} orphaned files", orphaned.size());
        return orphaned;
    }

    /**
     * Get all critical files (referenced by many other files).
     */
    public Set<String> findCriticalFiles(EnhancedYamlDependencyGraph graph) {
        logger.debug("Finding critical files");
        Set<String> critical = graph.getCriticalFiles();
        logger.info("Found {} critical files", critical.size());
        return critical;
    }

    /**
     * Calculate comprehensive metrics for the graph.
     */
    public DependencyMetrics calculateMetrics(EnhancedYamlDependencyGraph graph) {
        logger.debug("Calculating dependency metrics");
        DependencyMetrics metrics = graph.calculateMetrics();
        logger.info("Metrics calculated: complexity={}, circular={}, orphaned={}",
                metrics.getComplexityScore(), metrics.getCircularDependencies().size(),
                metrics.getOrphanedFiles().size());
        return metrics;
    }

    /**
     * Get direct dependencies for a file.
     */
    public Set<String> getDirectDependencies(EnhancedYamlDependencyGraph graph, String filePath) {
        logger.debug("Getting direct dependencies for: {}", filePath);
        return graph.getDirectDependencies(filePath);
    }

    /**
     * Get direct dependents for a file.
     */
    public Set<String> getDirectDependents(EnhancedYamlDependencyGraph graph, String filePath) {
        logger.debug("Getting direct dependents for: {}", filePath);
        return graph.getDirectDependents(filePath);
    }

    /**
     * Get transitive dependencies for a file.
     */
    public Set<String> getTransitiveDependencies(EnhancedYamlDependencyGraph graph, String filePath) {
        logger.debug("Getting transitive dependencies for: {}", filePath);
        return graph.getTransitiveDependencies(filePath);
    }

    /**
     * Get transitive dependents for a file.
     */
    public Set<String> getTransitiveDependents(EnhancedYamlDependencyGraph graph, String filePath) {
        logger.debug("Getting transitive dependents for: {}", filePath);
        return graph.getTransitiveDependents(filePath);
    }

    /**
     * Generate a text report of the dependency analysis.
     */
    public String generateReport(EnhancedYamlDependencyGraph graph) {
        logger.debug("Generating dependency analysis report");

        StringBuilder report = new StringBuilder();
        report.append("YAML Dependency Analysis Report\n");
        report.append("================================\n\n");

        // Summary
        DependencyMetrics metrics = graph.getMetrics();
        report.append("Summary:\n");
        report.append("├── Total Files: ").append(graph.getTotalFiles()).append("\n");
        report.append("├── Max Depth: ").append(graph.getMaxDepth()).append("\n");
        report.append("├── Complexity Score: ").append(metrics.getComplexityScore()).append("/100\n");
        report.append("├── Circular Dependencies: ").append(metrics.getCircularDependencies().size()).append("\n");
        report.append("├── Orphaned Files: ").append(metrics.getOrphanedFiles().size()).append("\n");
        report.append("└── Critical Files: ").append(metrics.getCriticalFiles().size()).append("\n\n");

        // Issues
        if (!metrics.getCircularDependencies().isEmpty()) {
            report.append("Circular Dependencies:\n");
            for (String file : metrics.getCircularDependencies()) {
                report.append("  ⚠ ").append(file).append("\n");
            }
            report.append("\n");
        }

        if (!metrics.getOrphanedFiles().isEmpty()) {
            report.append("Orphaned Files:\n");
            for (String file : metrics.getOrphanedFiles()) {
                report.append("  ✗ ").append(file).append("\n");
            }
            report.append("\n");
        }

        if (!metrics.getCriticalFiles().isEmpty()) {
            report.append("Critical Files (high impact):\n");
            for (String file : metrics.getCriticalFiles()) {
                report.append("  ★ ").append(file).append("\n");
            }
        }

        return report.toString();
    }

    /**
     * Get detailed circular dependency information.
     */
    public List<CircularDependencyInfo> getDetailedCircularDependencies(EnhancedYamlDependencyGraph graph) {
        logger.debug("Getting detailed circular dependencies");
        CircularDependencyDetector detector = new CircularDependencyDetector(graph.getForwardEdges());
        return detector.detectAllCycles();
    }

    /**
     * Get circular dependencies filtered by severity.
     */
    public List<CircularDependencyInfo> getCircularDependenciesBySeverity(EnhancedYamlDependencyGraph graph,
                                                                          CircularDependencyInfo.Severity severity) {
        logger.debug("Getting circular dependencies with severity: {}", severity);
        CircularDependencyDetector detector = new CircularDependencyDetector(graph.getForwardEdges());
        detector.detectAllCycles();
        return detector.getCyclesBySeverity(severity);
    }

    /**
     * Get circular dependencies involving a specific file.
     */
    public List<CircularDependencyInfo> getCircularDependenciesForFile(EnhancedYamlDependencyGraph graph,
                                                                       String filePath) {
        logger.debug("Getting circular dependencies for file: {}", filePath);
        CircularDependencyDetector detector = new CircularDependencyDetector(graph.getForwardEdges());
        detector.detectAllCycles();
        return detector.getCyclesForFile(filePath);
    }

    /**
     * Generate a detailed circular dependency report.
     */
    public String generateCircularDependencyReport(EnhancedYamlDependencyGraph graph) {
        logger.debug("Generating circular dependency report");
        CircularDependencyDetector detector = new CircularDependencyDetector(graph.getForwardEdges());
        detector.detectAllCycles();
        return detector.generateReport();
    }

    /**
     * Generate dependency tree for a file using nested children format (D3 Hierarchy standard).
     */
    public TreeNode generateDependencyTree(EnhancedYamlDependencyGraph graph, String filePath) {
        logger.debug("Generating dependency tree for: {}", filePath);
        Set<String> visited = new HashSet<>();
        TreeNode root = buildDependencyTreeNode(graph, filePath, 0, visited);
        if (root != null) {
            root.calculateHeight();
        }
        return root;
    }

    /**
     * Recursively build dependency tree node structure with content summaries.
     */
    private TreeNode buildDependencyTreeNode(EnhancedYamlDependencyGraph graph, String filePath,
                                             int depth, Set<String> visited) {
        TreeNode node = new TreeNode(filePath, depth);
        // Ensure path/name are available to UI and details endpoint lookups
        node.setPath(filePath);

        if (visited.contains(filePath)) {
            node.setCircular(true);
            node.setCircularReference("Circular reference detected");
            return node;
        }

        visited.add(filePath);

        // Analyze YAML content if analyzer is available
        if (contentAnalyzer != null) {
            try {
                YamlContentSummary summary = contentAnalyzer.analyzYamlContent(filePath);
                node.setContentSummary(summary);
            } catch (Exception e) {
                logger.debug("Could not analyze YAML content for {}: {}", filePath, e.getMessage());
            }
        }

        // Get forward edges (dependencies) for this file
        Map<String, Set<String>> forwardEdges = graph.getForwardEdges();
        Set<String> dependencies = forwardEdges.getOrDefault(filePath, new HashSet<>());

        for (String dependency : dependencies) {
            Set<String> childVisited = new HashSet<>(visited);
            TreeNode childNode = buildDependencyTreeNode(graph, dependency, depth + 1, childVisited);
            if (childNode != null) {
                node.addChild(childNode);
            }
        }

        return node;
    }

    /**
     * Get detailed information for a specific node in the dependency tree.
     */
    public TreeNode getNodeDetails(EnhancedYamlDependencyGraph graph, String filePath) {
        logger.debug("Getting node details for: {}", filePath);

        TreeNode node = new TreeNode(filePath, 0);

        // Get forward edges (dependencies) for this file
        Map<String, Set<String>> forwardEdges = graph.getForwardEdges();
        Set<String> dependencies = forwardEdges.getOrDefault(filePath, new HashSet<>());
        node.setDependencies(new ArrayList<>(dependencies));

        // Get backward edges (dependents) for this file
        Map<String, Set<String>> backwardEdges = graph.getBackwardEdges();
        Set<String> dependents = backwardEdges.getOrDefault(filePath, new HashSet<>());
        node.setDependents(new ArrayList<>(dependents));

        // Calculate all transitive dependencies
        Set<String> allDeps = new HashSet<>(dependencies);
        Queue<String> queue = new LinkedList<>(dependencies);
        Set<String> visited = new HashSet<>();
        visited.add(filePath);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.contains(current)) {
                visited.add(current);
                Set<String> transitiveDeps = forwardEdges.getOrDefault(current, new HashSet<>());
                for (String dep : transitiveDeps) {
                    if (!visited.contains(dep)) {
                        allDeps.add(dep);
                        queue.add(dep);
                    }
                }
            }
        }
        node.setAllDependencies(new ArrayList<>(allDeps));

        // Set health score (default to 85 if not set)
        if (node.getHealthScore() == 0) {
            node.setHealthScore(85);
        }

        return node;
    }
}

