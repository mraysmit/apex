package dev.mars.apex.rest.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for YAML dependency analysis operations.
 * 
 * Provides business logic for analyzing YAML file dependencies,
 * generating dependency trees, and performing validation operations.
 * 
 * This service acts as a bridge between the REST API layer and the
 * apex-core YamlDependencyAnalyzer.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-22
 * @version 1.0
 */
@Service
public class DependencyAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalysisService.class);

    // Cache for dependency graphs to avoid re-analysis
    private YamlDependencyGraph currentGraph;
    private String currentBasePath;

    /**
     * Generate dependency tree for a YAML file.
     */
    public Map<String, Object> generateDependencyTree(String rootFile) {
        logger.info("Generating dependency tree for: {}", rootFile);
        
        if (rootFile == null || rootFile.trim().isEmpty()) {
            throw new IllegalArgumentException("rootFile parameter is required");
        }

        try {
            // Analyze dependencies using apex-core
            YamlDependencyGraph graph = analyzeDependenciesInternal(rootFile);
            
            // Convert to tree structure
            Map<String, Object> treeNode = convertToTreeNode(graph, rootFile);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rootFile", rootFile);
            response.put("totalFiles", graph.getTotalFiles());
            response.put("maxDepth", graph.getMaxDepth());
            response.put("tree", treeNode);
            response.put("timestamp", Instant.now().toEpochMilli());
            
            logger.info("Successfully generated dependency tree with {} files and max depth {}",
                graph.getTotalFiles(), graph.getMaxDepth());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to generate dependency tree for: " + rootFile, e);
            throw new RuntimeException("Failed to generate dependency tree: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze dependencies for a YAML file.
     */
    public Map<String, Object> analyzeDependencies(String filePath) {
        logger.info("Analyzing dependencies for: {}", filePath);
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath parameter is required");
        }

        try {
            // Analyze dependencies using apex-core
            YamlDependencyGraph graph = analyzeDependenciesInternal(filePath);
            
            // Calculate metrics
            Map<String, Object> metrics = calculateMetrics(graph);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rootFile", graph.getRootFile());
            response.put("totalFiles", graph.getTotalFiles());
            response.put("maxDepth", graph.getMaxDepth());
            response.put("metrics", metrics);
            response.put("timestamp", Instant.now().toEpochMilli());
            
            logger.info("Successfully analyzed dependencies for {} with {} total files",
                filePath, graph.getTotalFiles());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to analyze dependencies for: " + filePath, e);
            throw new RuntimeException("Failed to analyze dependencies: " + e.getMessage(), e);
        }
    }

    /**
     * Validate dependency tree structure.
     */
    public Map<String, Object> validateDependencyTree(String rootFile) {
        logger.info("Validating dependency tree for: {}", rootFile);
        
        if (rootFile == null || rootFile.trim().isEmpty()) {
            throw new IllegalArgumentException("rootFile parameter is required");
        }

        try {
            // Analyze dependencies
            YamlDependencyGraph graph = analyzeDependenciesInternal(rootFile);
            
            // Perform validation
            Map<String, Object> validation = performValidation(graph);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rootFile", rootFile);
            response.put("validation", validation);
            response.put("timestamp", Instant.now().toEpochMilli());
            
            logger.info("Successfully validated dependency tree for: {}", rootFile);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to validate dependency tree for: " + rootFile, e);
            throw new RuntimeException("Failed to validate dependency tree: " + e.getMessage(), e);
        }
    }

    /**
     * Get detailed information for a specific node.
     */
    public Map<String, Object> getNodeDetails(String filePath) {
        logger.debug("Getting node details for: {}", filePath);
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath parameter is required");
        }

        if (currentGraph == null) {
            throw new RuntimeException("No dependency graph available. Please analyze dependencies first.");
        }

        YamlNode node = currentGraph.getNode(filePath);
        if (node == null) {
            throw new RuntimeException("File not found in dependency graph: " + filePath);
        }

        try {
            // Build detailed node information
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("path", node.getFilePath());
            nodeData.put("exists", node.exists());
            nodeData.put("yamlValid", node.isYamlValid());
            nodeData.put("referencedFiles", node.getReferencedFiles() != null ? node.getReferencedFiles() : List.of());
            nodeData.put("referencedBy", node.getReferencedBy() != null ? node.getReferencedBy() : List.of());
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", nodeData);
            response.put("timestamp", Instant.now().toEpochMilli());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to get node details for: " + filePath, e);
            throw new RuntimeException("Failed to get node details: " + e.getMessage(), e);
        }
    }

    /**
     * Scan a folder for YAML files.
     */
    public Map<String, Object> scanFolder(String folderPath) {
        logger.info("Scanning folder: {}", folderPath);
        
        if (folderPath == null || folderPath.trim().isEmpty()) {
            throw new IllegalArgumentException("folderPath parameter is required");
        }

        try {
            Path folder = Paths.get(folderPath);
            if (!Files.exists(folder) || !Files.isDirectory(folder)) {
                throw new IllegalArgumentException("Folder does not exist or is not a directory: " + folderPath);
            }

            // Scan for YAML files
            List<String> yamlFiles = Files.walk(folder)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                })
                .map(path -> folder.relativize(path).toString())
                .sorted()
                .collect(Collectors.toList());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("folderPath", folderPath);
            response.put("filesFound", yamlFiles.size());
            response.put("files", yamlFiles);
            response.put("timestamp", Instant.now().toEpochMilli());
            
            logger.info("Successfully scanned folder {} and found {} YAML files",
                folderPath, yamlFiles.size());
            
            return response;
            
        } catch (IOException e) {
            logger.error("Failed to scan folder: " + folderPath, e);
            throw new RuntimeException("Failed to scan folder: " + e.getMessage(), e);
        }
    }

    /**
     * Internal method to analyze dependencies using apex-core.
     */
    private YamlDependencyGraph analyzeDependenciesInternal(String rootFile) {
        try {
            Path rootPath = Paths.get(rootFile);
            boolean isAbsolute = rootPath.isAbsolute();
            
            YamlDependencyAnalyzer analyzer;
            String analyzerRootArg;
            
            if (isAbsolute) {
                this.currentBasePath = rootPath.getParent().toString();
                analyzer = new YamlDependencyAnalyzer(this.currentBasePath);
                analyzerRootArg = rootPath.getFileName().toString();
                logger.debug("Using absolute path analysis with basePath: {} and root: {}", 
                    this.currentBasePath, analyzerRootArg);
            } else {
                // For relative paths, use current working directory
                this.currentBasePath = System.getProperty("user.dir");
                analyzer = new YamlDependencyAnalyzer(this.currentBasePath);
                analyzerRootArg = rootFile;
                logger.debug("Using relative path analysis with basePath: {} and root: {}", 
                    this.currentBasePath, analyzerRootArg);
            }

            // Use apex-core to analyze dependencies
            this.currentGraph = analyzer.analyzeYamlDependencies(analyzerRootArg);
            
            logger.debug("Analysis complete: {} files, max depth: {}", 
                this.currentGraph.getTotalFiles(), this.currentGraph.getMaxDepth());
            
            return this.currentGraph;
            
        } catch (Exception e) {
            logger.error("Failed to analyze dependencies internally for: " + rootFile, e);
            throw new RuntimeException("Dependency analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convert YamlDependencyGraph to tree node structure.
     */
    private Map<String, Object> convertToTreeNode(YamlDependencyGraph graph, String rootFile) {
        // First, try to find the root node using the exact path
        YamlNode rootNode = graph.getNode(rootFile);

        // If not found, try to find it using the graph's root file
        if (rootNode == null) {
            String graphRootFile = graph.getRootFile();
            rootNode = graph.getNode(graphRootFile);
            logger.debug("Root node not found for '{}', trying graph root file: '{}'", rootFile, graphRootFile);
        }

        // If still not found, try to find any node that matches the filename
        if (rootNode == null) {
            String fileName = rootFile.substring(rootFile.lastIndexOf('/') + 1);
            for (YamlNode node : graph.getAllNodes()) {
                if (node.getFilePath().endsWith(fileName)) {
                    rootNode = node;
                    logger.debug("Found root node by filename match: '{}'", node.getFilePath());
                    break;
                }
            }
        }

        if (rootNode == null) {
            logger.warn("No root node found for '{}' in graph with {} nodes", rootFile, graph.getTotalFiles());
            // Create a minimal root node if not found
            Map<String, Object> node = new HashMap<>();
            node.put("name", rootFile);
            node.put("children", List.of());
            return node;
        }

        logger.debug("Converting tree starting from root node: '{}'", rootNode.getFilePath());
        return convertNodeToTree(rootNode, graph, new HashSet<>());
    }

    /**
     * Recursively convert YamlNode to tree structure.
     */
    private Map<String, Object> convertNodeToTree(YamlNode node, YamlDependencyGraph graph, Set<String> visited) {
        Map<String, Object> treeNode = new HashMap<>();
        treeNode.put("name", node.getFilePath());
        treeNode.put("path", node.getFilePath());
        treeNode.put("exists", node.exists());
        treeNode.put("yamlValid", node.isYamlValid());

        // Avoid infinite recursion
        if (visited.contains(node.getFilePath())) {
            logger.debug("Circular reference detected for: {}", node.getFilePath());
            treeNode.put("children", List.of());
            treeNode.put("circular", true);
            return treeNode;
        }

        visited.add(node.getFilePath());

        // Add children
        List<Map<String, Object>> children = new ArrayList<>();
        if (node.getReferencedFiles() != null && !node.getReferencedFiles().isEmpty()) {
            logger.debug("Node '{}' has {} referenced files: {}",
                node.getFilePath(), node.getReferencedFiles().size(), node.getReferencedFiles());

            for (String referencedFile : node.getReferencedFiles()) {
                YamlNode childNode = graph.getNode(referencedFile);
                if (childNode != null) {
                    logger.debug("Converting child node: '{}'", referencedFile);
                    children.add(convertNodeToTree(childNode, graph, new HashSet<>(visited)));
                } else {
                    logger.warn("Referenced file not found in graph: '{}'", referencedFile);
                    // Add a placeholder for missing files
                    Map<String, Object> missingNode = new HashMap<>();
                    missingNode.put("name", referencedFile);
                    missingNode.put("path", referencedFile);
                    missingNode.put("exists", false);
                    missingNode.put("yamlValid", false);
                    missingNode.put("children", List.of());
                    missingNode.put("missing", true);
                    children.add(missingNode);
                }
            }
        } else {
            logger.debug("Node '{}' has no referenced files", node.getFilePath());
        }

        treeNode.put("children", children);
        visited.remove(node.getFilePath());

        logger.debug("Converted node '{}' with {} children", node.getFilePath(), children.size());
        return treeNode;
    }

    /**
     * Calculate metrics for the dependency graph.
     */
    private Map<String, Object> calculateMetrics(YamlDependencyGraph graph) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("totalFiles", graph.getTotalFiles());
        metrics.put("maxDepth", graph.getMaxDepth());
        metrics.put("missingFiles", graph.getMissingFiles().size());
        metrics.put("invalidFiles", graph.getInvalidYamlFiles().size());
        metrics.put("circularDependencies", graph.findCircularDependencies().size());
        metrics.put("healthy", graph.getStatistics().isHealthy());
        
        return metrics;
    }

    /**
     * Perform validation on the dependency graph.
     */
    private Map<String, Object> performValidation(YamlDependencyGraph graph) {
        Map<String, Object> validation = new HashMap<>();
        
        validation.put("isValid", graph.getStatistics().isHealthy());
        validation.put("totalFiles", graph.getTotalFiles());
        validation.put("maxDepth", graph.getMaxDepth());
        validation.put("missingFiles", graph.getMissingFiles());
        validation.put("invalidFiles", graph.getInvalidYamlFiles());
        validation.put("circularDependencies", graph.findCircularDependencies());
        validation.put("issues", new ArrayList<>());
        
        // Add validation issues
        List<String> issues = new ArrayList<>();
        if (!graph.getMissingFiles().isEmpty()) {
            issues.add("Missing files detected: " + graph.getMissingFiles().size());
        }
        if (!graph.getInvalidYamlFiles().isEmpty()) {
            issues.add("Invalid YAML files detected: " + graph.getInvalidYamlFiles().size());
        }
        if (!graph.findCircularDependencies().isEmpty()) {
            issues.add("Circular dependencies detected: " + graph.findCircularDependencies().size());
        }
        
        validation.put("issues", issues);
        validation.put("summary", issues.isEmpty() ? "No issues found" : issues.size() + " issues detected");
        
        return validation;
    }
}
