package dev.mars.apex.core.service.yaml;

import dev.mars.apex.core.util.YamlDependencyAnalyzer;
import dev.mars.apex.core.util.YamlDependencyGraph;
import dev.mars.apex.core.util.YamlNode;
import dev.mars.apex.core.util.DependencyStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for comprehensive YAML dependency analysis in APEX configurations.
 * 
 * This service provides a high-level API for analyzing dependencies between YAML files,
 * detecting circular dependencies, missing files, and generating dependency reports.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-25
 * @version 1.0
 */
public class YamlDependencyService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlDependencyService.class);
    
    private final YamlDependencyAnalyzer analyzer;
    
    /**
     * Create a dependency service with default analyzer.
     */
    public YamlDependencyService() {
        this.analyzer = new YamlDependencyAnalyzer();
    }
    
    /**
     * Create a dependency service with custom analyzer.
     * 
     * @param analyzer The dependency analyzer to use
     */
    public YamlDependencyService(YamlDependencyAnalyzer analyzer) {
        this.analyzer = analyzer;
    }
    
    /**
     * Analyze dependencies for a single YAML file.
     * 
     * @param filePath Path to the YAML file
     * @return Dependency graph for the file
     */
    public YamlDependencyGraph analyzeDependencies(String filePath) {
        LOGGER.info("Analyzing dependencies for: {}", filePath);
        
        try {
            YamlDependencyGraph graph = analyzer.analyzeYamlDependencies(filePath);
            LOGGER.debug("Dependency analysis completed for: {}", filePath);
            return graph;
        } catch (Exception e) {
            LOGGER.error("Failed to analyze dependencies for {}: {}", filePath, e.getMessage(), e);
            throw new DependencyAnalysisException("Failed to analyze dependencies for " + filePath, e);
        }
    }
    
    /**
     * Analyze dependencies for multiple YAML files.
     * 
     * @param filePaths List of file paths to analyze
     * @return Combined dependency analysis results
     */
    public DependencyAnalysisResult analyzeMultipleDependencies(List<String> filePaths) {
        LOGGER.info("Analyzing dependencies for {} files", filePaths.size());
        
        List<YamlDependencyGraph> graphs = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        for (String filePath : filePaths) {
            try {
                YamlDependencyGraph graph = analyzeDependencies(filePath);
                graphs.add(graph);
            } catch (Exception e) {
                LOGGER.warn("Failed to analyze dependencies for {}: {}", filePath, e.getMessage());
                failedFiles.add(filePath);
            }
        }
        
        return new DependencyAnalysisResult(graphs, failedFiles);
    }
    
    /**
     * Check if a file has any dependency issues.
     * 
     * @param filePath Path to the YAML file
     * @return true if the file has dependency issues, false otherwise
     */
    public boolean hasDependencyIssues(String filePath) {
        try {
            YamlDependencyGraph graph = analyzeDependencies(filePath);
            DependencyStatistics stats = graph.getStatistics();
            return !stats.isHealthy();
        } catch (Exception e) {
            LOGGER.warn("Error checking dependency issues for {}: {}", filePath, e.getMessage());
            return true; // Assume issues if analysis fails
        }
    }
    
    /**
     * Get all missing dependencies for a file.
     * 
     * @param filePath Path to the YAML file
     * @return List of missing dependency file paths
     */
    public List<String> getMissingDependencies(String filePath) {
        try {
            YamlDependencyGraph graph = analyzeDependencies(filePath);
            return new ArrayList<>(graph.getMissingFiles());
        } catch (Exception e) {
            LOGGER.warn("Error getting missing dependencies for {}: {}", filePath, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Check for circular dependencies in a file.
     * 
     * @param filePath Path to the YAML file
     * @return List of circular dependency chains
     */
    public List<List<String>> getCircularDependencies(String filePath) {
        try {
            YamlDependencyGraph graph = analyzeDependencies(filePath);
            return graph.findCircularDependencies();
        } catch (Exception e) {
            LOGGER.warn("Error checking circular dependencies for {}: {}", filePath, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get dependency statistics for a file.
     * 
     * @param filePath Path to the YAML file
     * @return Dependency statistics
     */
    public DependencyStatistics getDependencyStatistics(String filePath) {
        try {
            YamlDependencyGraph graph = analyzeDependencies(filePath);
            return graph.getStatistics();
        } catch (Exception e) {
            LOGGER.warn("Error getting dependency statistics for {}: {}", filePath, e.getMessage());
            return new DependencyStatistics(0, 0, 0, 0, 0, 0);
        }
    }
    
    /**
     * Generate a dependency report for a file.
     * 
     * @param filePath Path to the YAML file
     * @return Formatted dependency report
     */
    public DependencyReport generateDependencyReport(String filePath) {
        LOGGER.info("Generating dependency report for: {}", filePath);
        
        try {
            YamlDependencyGraph graph = analyzeDependencies(filePath);
            DependencyStatistics stats = graph.getStatistics();
            
            List<String> missingFiles = new ArrayList<>(graph.getMissingFiles());
            List<List<String>> circularDeps = graph.findCircularDependencies();
            
            return new DependencyReport(
                filePath,
                stats,
                missingFiles,
                circularDeps,
                getDependencyTree(graph, filePath)
            );
        } catch (Exception e) {
            LOGGER.error("Failed to generate dependency report for {}: {}", filePath, e.getMessage(), e);
            throw new DependencyAnalysisException("Failed to generate dependency report for " + filePath, e);
        }
    }
    
    /**
     * Validate that all dependencies for a file are satisfied.
     * 
     * @param filePath Path to the YAML file
     * @throws DependencyValidationException if dependencies are not satisfied
     */
    public void validateDependencies(String filePath) throws DependencyValidationException {
        YamlDependencyGraph graph = analyzeDependencies(filePath);
        DependencyStatistics stats = graph.getStatistics();
        
        if (!stats.isHealthy()) {
            List<String> issues = new ArrayList<>();
            
            if (!graph.getMissingFiles().isEmpty()) {
                issues.add("Missing files: " + graph.getMissingFiles());
            }
            
            List<List<String>> circularDeps = graph.findCircularDependencies();
            if (!circularDeps.isEmpty()) {
                issues.add("Circular dependencies: " + circularDeps);
            }
            
            String errorMessage = String.format(
                "Dependency validation failed for %s: %s",
                filePath,
                String.join("; ", issues)
            );
            throw new DependencyValidationException(errorMessage, graph);
        }
    }
    
    // Helper methods
    
    private List<String> getDependencyTree(YamlDependencyGraph graph, String rootFile) {
        List<String> tree = new ArrayList<>();
        YamlNode rootNode = graph.getNode(rootFile);
        
        if (rootNode != null) {
            buildDependencyTree(graph, rootNode, tree, "", new java.util.HashSet<>());
        }
        
        return tree;
    }
    
    private void buildDependencyTree(YamlDependencyGraph graph, YamlNode node, List<String> tree, 
                                   String indent, Set<String> visited) {
        if (visited.contains(node.getFilePath())) {
            tree.add(indent + node.getFilePath() + " (circular reference)");
            return;
        }
        
        visited.add(node.getFilePath());
        tree.add(indent + node.getFilePath() + " " + node.getStatusIndicator());
        
        if (node.getReferencedFiles() != null) {
            for (String referencedFile : node.getReferencedFiles()) {
                YamlNode refNode = graph.getNode(referencedFile);
                if (refNode != null) {
                    buildDependencyTree(graph, refNode, tree, indent + "  ", new java.util.HashSet<>(visited));
                } else {
                    tree.add(indent + "  " + referencedFile + " (missing)");
                }
            }
        }
    }
    
    /**
     * Result of analyzing multiple files.
     */
    public static class DependencyAnalysisResult {
        private final List<YamlDependencyGraph> graphs;
        private final List<String> failedFiles;
        
        public DependencyAnalysisResult(List<YamlDependencyGraph> graphs, List<String> failedFiles) {
            this.graphs = graphs;
            this.failedFiles = failedFiles;
        }
        
        public List<YamlDependencyGraph> getGraphs() { return graphs; }
        public List<String> getFailedFiles() { return failedFiles; }
        public boolean hasFailures() { return !failedFiles.isEmpty(); }
        public int getSuccessCount() { return graphs.size(); }
        public int getFailureCount() { return failedFiles.size(); }
    }
    
    /**
     * Comprehensive dependency report for a file.
     */
    public static class DependencyReport {
        private final String filePath;
        private final DependencyStatistics statistics;
        private final List<String> missingFiles;
        private final List<List<String>> circularDependencies;
        private final List<String> dependencyTree;
        
        public DependencyReport(String filePath, DependencyStatistics statistics, 
                              List<String> missingFiles, List<List<String>> circularDependencies,
                              List<String> dependencyTree) {
            this.filePath = filePath;
            this.statistics = statistics;
            this.missingFiles = missingFiles;
            this.circularDependencies = circularDependencies;
            this.dependencyTree = dependencyTree;
        }
        
        public String getFilePath() { return filePath; }
        public DependencyStatistics getStatistics() { return statistics; }
        public List<String> getMissingFiles() { return missingFiles; }
        public List<List<String>> getCircularDependencies() { return circularDependencies; }
        public List<String> getDependencyTree() { return dependencyTree; }
        public boolean hasIssues() { return !missingFiles.isEmpty() || !circularDependencies.isEmpty(); }
    }
    
    /**
     * Exception thrown when dependency analysis fails.
     */
    public static class DependencyAnalysisException extends RuntimeException {
        public DependencyAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when dependency validation fails.
     */
    public static class DependencyValidationException extends Exception {
        private final YamlDependencyGraph dependencyGraph;
        
        public DependencyValidationException(String message, YamlDependencyGraph graph) {
            super(message);
            this.dependencyGraph = graph;
        }
        
        public YamlDependencyGraph getDependencyGraph() {
            return dependencyGraph;
        }
    }
}
