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

    private String currentBasePath;

    public DependencyAnalysisService() {
        this.yamlAnalyzer = new YamlDependencyAnalyzer();
        logger.info("DependencyAnalysisService initialized");
    }

    /**
     * Analyze dependencies starting from a root YAML file.
     */
    public EnhancedYamlDependencyGraph analyzeDependencies(String rootFilePath) {
        logger.info("=== STARTING DEPENDENCY ANALYSIS ===");
        logger.info("Root file path: {}", rootFilePath);

        try {
            // Determine base path and root argument for analyzer
            java.nio.file.Path rootPath = java.nio.file.Paths.get(rootFilePath);
            boolean isAbsolute = rootPath.isAbsolute();
            YamlDependencyAnalyzer analyzerToUse = this.yamlAnalyzer;
            String analyzerRootArg = rootFilePath;

            logger.info("Path analysis: isAbsolute={}, parent={}, fileName={}",
                isAbsolute, rootPath.getParent(), rootPath.getFileName());

            if (isAbsolute) {
                this.currentBasePath = rootPath.getParent().toString();
                analyzerToUse = new YamlDependencyAnalyzer(this.currentBasePath);
                analyzerRootArg = rootPath.getFileName().toString();
                logger.info("Using dynamic basePath for analysis: {} with root {}", this.currentBasePath, analyzerRootArg);
            } else {
                this.currentBasePath = null; // Use relative paths as-is
                logger.info("Using relative path analysis");
            }

            // Use core analyzer to build basic graph
            logger.info("Building basic dependency graph...");
            YamlDependencyGraph basicGraph = analyzerToUse.analyzeYamlDependencies(analyzerRootArg);
            logger.info("Basic graph created with {} nodes and {} dependencies",
                basicGraph.getAllNodes().size(), basicGraph.getAllDependencies().size());

            // Enhance with bidirectional edges
            // Use the analyzer's root key for graph root so keys match edge maps
            EnhancedYamlDependencyGraph enhancedGraph = new EnhancedYamlDependencyGraph(analyzerRootArg);
            logger.info("Enhanced graph initialized with root: {}", analyzerRootArg);

            // Copy nodes from basic graph
            logger.info("Copying {} nodes to enhanced graph...", basicGraph.getAllNodes().size());
            for (YamlNode node : basicGraph.getAllNodes()) {
                enhancedGraph.addNode(node);
                logger.debug("Added node: {}", node.getFilePath());
            }

            // Copy dependencies and build bidirectional edges
            logger.info("Copying {} dependencies and building bidirectional edges...", basicGraph.getAllDependencies().size());
            for (var dependency : basicGraph.getAllDependencies()) {
                enhancedGraph.addDependency(dependency);
                enhancedGraph.addBidirectionalEdge(dependency.getSourceFile(), dependency.getTargetFile());
                logger.debug("Added dependency: {} -> {}", dependency.getSourceFile(), dependency.getTargetFile());
            }

            // Preserve computed maxDepth from the basic graph
            enhancedGraph.updateMaxDepth(basicGraph.getMaxDepth());

            logger.info("=== DEPENDENCY ANALYSIS COMPLETE ===");
            logger.info("Total files: {}", enhancedGraph.getTotalFiles());
            logger.info("Max depth: {}", enhancedGraph.getMaxDepth());
            logger.info("Forward edges: {}", enhancedGraph.getForwardEdges().size());
            logger.info("Backward edges: {}", enhancedGraph.getBackwardEdges().size());

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
        logger.info("=== GENERATING DEPENDENCY TREE ===");
        logger.info("Root file: {}", filePath);
        logger.info("Graph contains {} nodes", graph.getAllNodes().size());
        logger.info("Graph forward edges: {}", graph.getForwardEdges().size());

        Set<String> visited = new HashSet<>();
        TreeNode root = buildDependencyTreeNode(graph, filePath, 0, visited);
        if (root != null) {
            root.calculateHeight();
            logger.info("=== TREE GENERATION COMPLETE ===");
            logger.info("Root node: {}", root.getName());
            logger.info("Max depth: {}", root.getMaxDepth());
            logger.info("Total descendants: {}", root.getDescendantCount());
            logger.info("Child count: {}", root.getChildCount());
            logTreeStructure(root, 0);
        } else {
            logger.warn("Failed to generate tree - root node is null");
        }
        return root;
    }

    /**
     * Recursively build dependency tree node structure with content summaries.
     */
    private TreeNode buildDependencyTreeNode(EnhancedYamlDependencyGraph graph, String filePath,
                                             int depth, Set<String> visited) {
        logger.debug("Building tree node: {} at depth {}", filePath, depth);

        TreeNode node = new TreeNode(filePath, depth);
        // Ensure path/name are available to UI and details endpoint lookups
        node.setPath(filePath);

        // Get YAML content information from apex-core YamlNode (do this BEFORE circular dependency check)
        YamlNode yamlNode = graph.getNode(filePath);
        if (yamlNode != null) {
            logger.debug("Using apex-core YamlNode data for: {}", filePath);
            YamlContentSummary summary = createSummaryFromYamlNode(yamlNode);
            logger.debug("Content summary from YamlNode for {}: type={}, exists={}, valid={}",
                filePath, summary.getFileType(), yamlNode.exists(), yamlNode.isYamlValid());
            node.setContentSummary(summary);
        } else {
            logger.warn("No YamlNode available from apex-core for: {}", filePath);
        }

        if (visited.contains(filePath)) {
            logger.warn("CIRCULAR DEPENDENCY detected: {} (visited: {})", filePath, visited);
            node.setCircular(true);
            node.setCircularReference("Circular reference detected");
            return node;
        }

        visited.add(filePath);

        // Get forward edges (dependencies) for this file
        Map<String, Set<String>> forwardEdges = graph.getForwardEdges();
        Set<String> dependencies = forwardEdges.getOrDefault(filePath, new HashSet<>());
        logger.debug("Dependencies for {}: {} (count: {})", filePath, dependencies, dependencies.size());

        for (String dependency : dependencies) {
            logger.debug("Processing dependency: {} -> {}", filePath, dependency);
            Set<String> childVisited = new HashSet<>(visited);
            TreeNode childNode = buildDependencyTreeNode(graph, dependency, depth + 1, childVisited);
            if (childNode != null) {
                node.addChild(childNode);
                logger.debug("Added child node: {} to parent: {}", dependency, filePath);
            } else {
                logger.warn("Failed to create child node for dependency: {}", dependency);
            }
        }

        logger.debug("Completed node: {} with {} children", filePath, node.getChildCount());
        return node;
    }

    /**
     * Log the tree structure for debugging purposes.
     */
    private void logTreeStructure(TreeNode node, int indentLevel) {
        String indent = "  ".repeat(indentLevel);
        String nodeInfo = String.format("%s%s (depth=%d, children=%d)",
            indent, node.getName(), node.getDepth(), node.getChildCount());

        if (node.getContentSummary() != null) {
            YamlContentSummary summary = node.getContentSummary();
            nodeInfo += String.format(" [type=%s, rules=%d, enrichments=%d]",
                summary.getFileType(), summary.getRuleCount(), summary.getEnrichmentCount());
        }

        if (node.isCircular()) {
            nodeInfo += " [CIRCULAR]";
        }

        logger.info(nodeInfo);

        // Recursively log children
        if (node.getChildren() != null) {
            for (TreeNode child : node.getChildren()) {
                logTreeStructure(child, indentLevel + 1);
            }
        }
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

    /**
     * Create YamlContentSummary from apex-core YamlNode data.
     * TODO: This is a temporary adapter - apex-core should be enhanced to read YAML metadata properly.
     */
    private YamlContentSummary createSummaryFromYamlNode(YamlNode yamlNode) {
        YamlContentSummary summary = new YamlContentSummary(yamlNode.getFilePath());

        // Try to get more accurate file type by reading YAML metadata if file exists and is valid
        String fileType = "unknown";
        if (yamlNode.exists() && yamlNode.isYamlValid()) {
            try {
                // Temporarily use contentAnalyzer to get accurate metadata until apex-core is enhanced
                if (contentAnalyzer != null) {
                    String fullPath = resolveFullPath(yamlNode.getFilePath());
                    YamlContentSummary tempSummary = contentAnalyzer.analyzYamlContent(fullPath);
                    if (tempSummary != null && tempSummary.getFileType() != null) {
                        fileType = tempSummary.getFileType();
                        // Also copy other metadata
                        summary.setId(tempSummary.getId());
                        summary.setName(tempSummary.getName());
                        summary.setDescription(tempSummary.getDescription());
                        summary.setVersion(tempSummary.getVersion());
                        summary.setRuleCount(tempSummary.getRuleCount());
                        summary.setRuleGroupCount(tempSummary.getRuleGroupCount());
                        summary.setEnrichmentCount(tempSummary.getEnrichmentCount());
                        summary.setConfigFileCount(tempSummary.getConfigFileCount());
                        summary.setReferenceCount(tempSummary.getReferenceCount());
                        summary.setRawContent(tempSummary.getRawContent());
                        summary.setContentCounts(tempSummary.getContentCounts());
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not read YAML metadata for {}: {}", yamlNode.getFilePath(), e.getMessage());
                // Fall back to apex-core file type
                if (yamlNode.getFileType() != null) {
                    fileType = yamlNode.getFileType().toString().toLowerCase();
                }
            }
        } else {
            // File doesn't exist or invalid YAML - use apex-core file type
            if (yamlNode.getFileType() != null) {
                fileType = yamlNode.getFileType().toString().toLowerCase();
            }
        }

        summary.setFileType(fileType);

        // Add metadata about file existence and validity as content counts
        summary.addContentCount("file-exists", yamlNode.exists() ? 1 : 0);
        summary.addContentCount("yaml-valid", yamlNode.isYamlValid() ? 1 : 0);

        logger.debug("Created summary from YamlNode: path={}, type={}, exists={}, valid={}",
            yamlNode.getFilePath(), summary.getFileType(), yamlNode.exists(), yamlNode.isYamlValid());

        return summary;
    }

    /**
     * Resolve full path for content analysis.
     */
    private String resolveFullPath(String filePath) {
        if (currentBasePath != null) {
            // If we have a base path, resolve relative to it
            java.nio.file.Path fullPath = java.nio.file.Paths.get(currentBasePath, filePath);
            return fullPath.toString();
        } else {
            // Use the path as-is (could be relative or absolute)
            return filePath;
        }
    }
}

