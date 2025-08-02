package dev.mars.apex.core.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete dependency graph of YAML files.
 * 
 * This class maintains the complete structure of YAML file dependencies,
 * including nodes for each file, edges representing dependencies, and
 * various analysis results like missing files and circular dependencies.
 * 
 * @author APEX Rules Engine Team
 * @since 1.0.0
 */
public class YamlDependencyGraph {
    
    private final String rootFile;
    private final Map<String, YamlNode> nodes;
    private final List<YamlDependency> dependencies;
    private int maxDepth;
    
    public YamlDependencyGraph(String rootFile) {
        this.rootFile = rootFile;
        this.nodes = new HashMap<>();
        this.dependencies = new ArrayList<>();
        this.maxDepth = 0;
    }
    
    /**
     * Adds a node to the dependency graph.
     */
    public void addNode(YamlNode node) {
        nodes.put(node.getFilePath(), node);
    }
    
    /**
     * Adds a dependency edge to the graph.
     */
    public void addDependency(YamlDependency dependency) {
        dependencies.add(dependency);
        
        // Update reverse references
        YamlNode targetNode = nodes.get(dependency.getTargetFile());
        if (targetNode != null) {
            targetNode.addReferencedBy(dependency.getSourceFile());
        }
    }
    
    /**
     * Gets a node by file path.
     */
    public YamlNode getNode(String filePath) {
        return nodes.get(filePath);
    }
    
    /**
     * Gets all nodes in the graph.
     */
    public Collection<YamlNode> getAllNodes() {
        return nodes.values();
    }
    
    /**
     * Gets all dependencies in the graph.
     */
    public List<YamlDependency> getAllDependencies() {
        return new ArrayList<>(dependencies);
    }
    
    /**
     * Gets all referenced files (including the root file).
     */
    public List<String> getAllReferencedFiles() {
        return new ArrayList<>(nodes.keySet());
    }
    
    /**
     * Gets files that are missing (referenced but don't exist).
     */
    public List<String> getMissingFiles() {
        return nodes.values().stream()
            .filter(node -> !node.exists())
            .map(YamlNode::getFilePath)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets files that exist but have invalid YAML syntax.
     */
    public List<String> getInvalidYamlFiles() {
        return nodes.values().stream()
            .filter(node -> node.exists() && !node.isYamlValid())
            .map(YamlNode::getFilePath)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds circular dependencies in the graph.
     */
    public List<List<String>> findCircularDependencies() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String filePath : nodes.keySet()) {
            if (!visited.contains(filePath)) {
                List<String> currentPath = new ArrayList<>();
                if (hasCycleDFS(filePath, visited, recursionStack, currentPath, cycles)) {
                    // Cycle found and added to cycles list
                }
            }
        }
        
        return cycles;
    }
    
    /**
     * DFS helper method to detect cycles.
     */
    private boolean hasCycleDFS(String filePath, Set<String> visited, Set<String> recursionStack, 
                               List<String> currentPath, List<List<String>> cycles) {
        
        visited.add(filePath);
        recursionStack.add(filePath);
        currentPath.add(filePath);
        
        YamlNode node = nodes.get(filePath);
        if (node != null && node.getReferencedFiles() != null) {
            for (String referencedFile : node.getReferencedFiles()) {
                String resolvedPath = referencedFile; // Assume already resolved
                
                if (!visited.contains(resolvedPath)) {
                    if (hasCycleDFS(resolvedPath, visited, recursionStack, currentPath, cycles)) {
                        return true;
                    }
                } else if (recursionStack.contains(resolvedPath)) {
                    // Found a cycle
                    int cycleStart = currentPath.indexOf(resolvedPath);
                    if (cycleStart >= 0) {
                        List<String> cycle = new ArrayList<>(currentPath.subList(cycleStart, currentPath.size()));
                        cycle.add(resolvedPath); // Complete the cycle
                        cycles.add(cycle);
                        return true;
                    }
                }
            }
        }
        
        recursionStack.remove(filePath);
        currentPath.remove(currentPath.size() - 1);
        return false;
    }
    
    /**
     * Gets files that depend on the specified file.
     */
    public List<String> getFilesDependingOn(String filePath) {
        YamlNode node = nodes.get(filePath);
        if (node != null && node.getReferencedBy() != null) {
            return new ArrayList<>(node.getReferencedBy());
        }
        return new ArrayList<>();
    }
    
    /**
     * Gets files that the specified file depends on.
     */
    public List<String> getFilesDependedOnBy(String filePath) {
        YamlNode node = nodes.get(filePath);
        if (node != null && node.getReferencedFiles() != null) {
            return new ArrayList<>(node.getReferencedFiles());
        }
        return new ArrayList<>();
    }
    
    /**
     * Checks if the graph has any missing files.
     */
    public boolean hasMissingFiles() {
        return !getMissingFiles().isEmpty();
    }
    
    /**
     * Checks if the graph has any invalid YAML files.
     */
    public boolean hasInvalidYamlFiles() {
        return !getInvalidYamlFiles().isEmpty();
    }
    
    /**
     * Checks if the graph has any circular dependencies.
     */
    public boolean hasCircularDependencies() {
        return !findCircularDependencies().isEmpty();
    }
    
    /**
     * Updates the maximum depth reached during analysis.
     */
    public void updateMaxDepth(int depth) {
        this.maxDepth = Math.max(this.maxDepth, depth);
    }
    
    // Getters
    public String getRootFile() {
        return rootFile;
    }
    
    public int getTotalFiles() {
        return nodes.size();
    }
    
    public int getMaxDepth() {
        return maxDepth;
    }
    
    /**
     * Gets statistics about the dependency graph.
     */
    public DependencyStatistics getStatistics() {
        return new DependencyStatistics(
            getTotalFiles(),
            getMaxDepth(),
            getMissingFiles().size(),
            getInvalidYamlFiles().size(),
            findCircularDependencies().size(),
            dependencies.size()
        );
    }
    
    @Override
    public String toString() {
        return "YamlDependencyGraph{" +
                "rootFile='" + rootFile + '\'' +
                ", totalFiles=" + getTotalFiles() +
                ", maxDepth=" + maxDepth +
                ", missingFiles=" + getMissingFiles().size() +
                ", invalidFiles=" + getInvalidYamlFiles().size() +
                '}';
    }
}

/**
 * Statistics about a dependency graph.
 */
class DependencyStatistics {
    private final int totalFiles;
    private final int maxDepth;
    private final int missingFiles;
    private final int invalidFiles;
    private final int circularDependencies;
    private final int totalDependencies;
    
    public DependencyStatistics(int totalFiles, int maxDepth, int missingFiles, 
                               int invalidFiles, int circularDependencies, int totalDependencies) {
        this.totalFiles = totalFiles;
        this.maxDepth = maxDepth;
        this.missingFiles = missingFiles;
        this.invalidFiles = invalidFiles;
        this.circularDependencies = circularDependencies;
        this.totalDependencies = totalDependencies;
    }
    
    // Getters
    public int getTotalFiles() { return totalFiles; }
    public int getMaxDepth() { return maxDepth; }
    public int getMissingFiles() { return missingFiles; }
    public int getInvalidFiles() { return invalidFiles; }
    public int getCircularDependencies() { return circularDependencies; }
    public int getTotalDependencies() { return totalDependencies; }
    
    public boolean isHealthy() {
        return missingFiles == 0 && invalidFiles == 0 && circularDependencies == 0;
    }
    
    @Override
    public String toString() {
        return "DependencyStatistics{" +
                "totalFiles=" + totalFiles +
                ", maxDepth=" + maxDepth +
                ", missingFiles=" + missingFiles +
                ", invalidFiles=" + invalidFiles +
                ", circularDependencies=" + circularDependencies +
                ", totalDependencies=" + totalDependencies +
                ", healthy=" + isHealthy() +
                '}';
    }
}
