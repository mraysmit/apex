package dev.mars.apex.yaml.manager.controller;

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
import dev.mars.apex.yaml.manager.model.DependencyMetrics;
import dev.mars.apex.yaml.manager.model.EnhancedYamlDependencyGraph;
import dev.mars.apex.yaml.manager.model.ImpactAnalysisResult;
import dev.mars.apex.yaml.manager.model.TreeNode;
import dev.mars.apex.yaml.manager.service.DependencyAnalysisService;
import dev.mars.apex.yaml.manager.service.TreeValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * REST API controller for dependency analysis operations.
 *
 * Provides endpoints for:
 * - Analyzing YAML file dependencies
 * - Performing impact analysis
 * - Detecting circular dependencies
 * - Calculating complexity metrics
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@RestController
@RequestMapping("/api/dependencies")
@Tag(name = "Dependency Analysis", description = "YAML dependency analysis operations")
public class DependencyAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalysisController.class);

    private final DependencyAnalysisService dependencyService;
    private final TreeValidationService treeValidationService;
    private EnhancedYamlDependencyGraph currentGraph;

    @Autowired
    public DependencyAnalysisController(DependencyAnalysisService dependencyService,
                                        TreeValidationService treeValidationService) {
        this.dependencyService = dependencyService;
        this.treeValidationService = treeValidationService;
    }

    /**
     * Analyze dependencies for a root YAML file.
     */
    @PostMapping("/analyze")
    @Operation(
        summary = "Analyze YAML dependencies",
        description = "Analyze the complete dependency graph starting from a root YAML file"
    )
    @ApiResponse(responseCode = "200", description = "Analysis completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    public ResponseEntity<Map<String, Object>> analyzeDependencies(
            @Parameter(description = "Path to root YAML file")
            @RequestParam String filePath) {

        logger.info("Analyzing dependencies for: {}", filePath);

        try {
            EnhancedYamlDependencyGraph graph = dependencyService.analyzeDependencies(filePath);
            this.currentGraph = graph;

            DependencyMetrics metrics = dependencyService.calculateMetrics(graph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rootFile", graph.getRootFile());
            response.put("totalFiles", graph.getTotalFiles());
            response.put("maxDepth", graph.getMaxDepth());
            response.put("metrics", metrics);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to analyze dependencies", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Analyze dependencies from file content (for browser-uploaded files).
     */
    @PostMapping("/analyze-content")
    @Operation(
        summary = "Analyze YAML dependencies from content",
        description = "Analyze dependencies from file content provided by browser file selection"
    )
    @ApiResponse(responseCode = "200", description = "Analysis completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file content")
    public ResponseEntity<Map<String, Object>> analyzeContentDependencies(
            @RequestBody Map<String, String> request) {

        String fileName = request.get("fileName");
        String content = request.get("content");
        String folderPath = request.get("folderPath");

        logger.info("Analyzing dependencies from content for file: {}", fileName);

        try {
            // Create a temporary file with the content
            Path tempFile = Files.createTempFile("yaml-analysis-", ".yaml");
            Files.write(tempFile, content.getBytes());

            try {
                // Analyze using the temporary file
                EnhancedYamlDependencyGraph graph = dependencyService.analyzeDependencies(tempFile.toString());
                this.currentGraph = graph;

                DependencyMetrics metrics = dependencyService.calculateMetrics(graph);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("rootFile", fileName);
                response.put("totalFiles", graph.getTotalFiles());
                response.put("maxDepth", graph.getMaxDepth());
                response.put("metrics", metrics);
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            } finally {
                // Clean up temporary file
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            logger.error("Failed to analyze dependencies from content", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Perform impact analysis for a specific file.
     */
    @GetMapping("/{filePath}/impact")
    @Operation(
        summary = "Analyze impact of changes",
        description = "Determine what breaks if the specified file is modified or deleted"
    )
    @ApiResponse(responseCode = "200", description = "Impact analysis completed")
    @ApiResponse(responseCode = "400", description = "File not found in graph")
    public ResponseEntity<Map<String, Object>> analyzeImpact(
            @Parameter(description = "File path to analyze")
            @PathVariable String filePath) {

        logger.info("Analyzing impact for: {}", filePath);

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            ImpactAnalysisResult impact = dependencyService.analyzeImpact(currentGraph, filePath);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("analyzedFile", impact.getAnalyzedFile());
            response.put("directDependents", impact.getDirectDependents());
            response.put("transitiveDependents", impact.getTransitiveDependents());
            response.put("impactRadius", impact.getImpactRadius());
            response.put("impactScore", impact.getImpactScore());
            response.put("riskLevel", impact.getRiskLevel());
            response.put("recommendation", impact.getRecommendation());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to analyze impact", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get metrics for the current graph.
     */
    @GetMapping("/metrics")
    @Operation(
        summary = "Get dependency metrics",
        description = "Get comprehensive metrics for the current dependency graph"
    )
    @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        logger.info("Retrieving dependency metrics");

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            DependencyMetrics metrics = dependencyService.calculateMetrics(currentGraph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("metrics", metrics);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get metrics", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get circular dependencies.
     */
    @GetMapping("/circular-dependencies")
    @Operation(
        summary = "Find circular dependencies",
        description = "Identify all circular dependency cycles in the graph"
    )
    @ApiResponse(responseCode = "200", description = "Circular dependencies retrieved")
    public ResponseEntity<Map<String, Object>> getCircularDependencies() {
        logger.info("Retrieving circular dependencies");

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            List<List<String>> cycles = dependencyService.findCircularDependencies(currentGraph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("circularDependencies", cycles);
            response.put("count", cycles.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get circular dependencies", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get orphaned files.
     */
    @GetMapping("/orphaned-files")
    @Operation(
        summary = "Find orphaned files",
        description = "Identify files that are not referenced and don't reference anything"
    )
    @ApiResponse(responseCode = "200", description = "Orphaned files retrieved")
    public ResponseEntity<Map<String, Object>> getOrphanedFiles() {
        logger.info("Retrieving orphaned files");

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            Set<String> orphaned = dependencyService.findOrphanedFiles(currentGraph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("orphanedFiles", orphaned);
            response.put("count", orphaned.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get orphaned files", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get critical files.
     */
    @GetMapping("/critical-files")
    @Operation(
        summary = "Find critical files",
        description = "Identify files that are referenced by many other files"
    )
    @ApiResponse(responseCode = "200", description = "Critical files retrieved")
    public ResponseEntity<Map<String, Object>> getCriticalFiles() {
        logger.info("Retrieving critical files");

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            Set<String> critical = dependencyService.findCriticalFiles(currentGraph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("criticalFiles", critical);
            response.put("count", critical.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get critical files", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get analysis report.
     */
    @GetMapping("/report")
    @Operation(
        summary = "Generate analysis report",
        description = "Generate a comprehensive text report of the dependency analysis"
    )
    @ApiResponse(responseCode = "200", description = "Report generated successfully")
    public ResponseEntity<Map<String, Object>> getReport() {
        logger.info("Generating dependency analysis report");

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            String report = dependencyService.generateReport(currentGraph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("report", report);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to generate report", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get detailed circular dependency analysis.
     */
    @GetMapping("/circular-dependencies/detailed")
    @Operation(
        summary = "Get detailed circular dependency analysis",
        description = "Get comprehensive analysis of all circular dependencies with severity and resolution suggestions"
    )
    @ApiResponse(responseCode = "200", description = "Detailed analysis retrieved")
    public ResponseEntity<Map<String, Object>> getDetailedCircularDependencies() {
        logger.info("Retrieving detailed circular dependency analysis");

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            List<CircularDependencyInfo> cycles = dependencyService.getDetailedCircularDependencies(currentGraph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalCycles", cycles.size());
            response.put("cycles", cycles.stream().map(cycle -> Map.of(
                    "path", cycle.getCyclePathAsString(),
                    "length", cycle.getCycleLength(),
                    "severity", cycle.getSeverity().toString(),
                    "files", cycle.getFilesInCycle(),
                    "resolution", cycle.suggestResolution()
            )).toList());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get detailed circular dependencies", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get circular dependencies by severity.
     */
    @GetMapping("/circular-dependencies/severity/{severity}")
    @Operation(
        summary = "Get circular dependencies by severity",
        description = "Get circular dependencies filtered by severity level (CRITICAL, HIGH, MEDIUM, LOW)"
    )
    @ApiResponse(responseCode = "200", description = "Filtered cycles retrieved")
    public ResponseEntity<Map<String, Object>> getCircularDependenciesBySeverity(
            @Parameter(description = "Severity level: CRITICAL, HIGH, MEDIUM, or LOW")
            @PathVariable String severity) {
        logger.info("Retrieving circular dependencies with severity: {}", severity);

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            CircularDependencyInfo.Severity sev = CircularDependencyInfo.Severity.valueOf(severity.toUpperCase());
            List<CircularDependencyInfo> cycles = dependencyService.getCircularDependenciesBySeverity(currentGraph, sev);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("severity", severity);
            response.put("count", cycles.size());
            response.put("cycles", cycles.stream().map(cycle -> Map.of(
                    "path", cycle.getCyclePathAsString(),
                    "length", cycle.getCycleLength(),
                    "resolution", cycle.suggestResolution()
            )).toList());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid severity level: {}", severity);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid severity. Must be one of: CRITICAL, HIGH, MEDIUM, LOW"
            ));
        } catch (Exception e) {
            logger.error("Failed to get circular dependencies by severity", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get circular dependencies for a specific file.
     */
    @GetMapping("/circular-dependencies/file/{filePath}")
    @Operation(
        summary = "Get cycles involving a specific file",
        description = "Get all circular dependency cycles that involve the specified file"
    )
    @ApiResponse(responseCode = "200", description = "Cycles retrieved")
    public ResponseEntity<Map<String, Object>> getCircularDependenciesForFile(
            @Parameter(description = "File path to check")
            @PathVariable String filePath) {
        logger.info("Retrieving circular dependencies for file: {}", filePath);

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            List<CircularDependencyInfo> cycles = dependencyService.getCircularDependenciesForFile(currentGraph, filePath);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("file", filePath);
            response.put("cycleCount", cycles.size());
            response.put("isInCycle", !cycles.isEmpty());
            response.put("cycles", cycles.stream().map(cycle -> Map.of(
                    "path", cycle.getCyclePathAsString(),
                    "position", cycle.getFilePosition(filePath),
                    "nextFile", cycle.getNextFileInCycle(filePath),
                    "previousFile", cycle.getPreviousFileInCycle(filePath),
                    "severity", cycle.getSeverity().toString()
            )).toList());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get circular dependencies for file", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get circular dependency analysis report.
     */
    @GetMapping("/circular-dependencies/report")
    @Operation(
        summary = "Generate circular dependency report",
        description = "Generate a detailed text report of all circular dependencies"
    )
    @ApiResponse(responseCode = "200", description = "Report generated")
    public ResponseEntity<Map<String, Object>> getCircularDependencyReport() {
        logger.info("Generating circular dependency report");

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            String report = dependencyService.generateCircularDependencyReport(currentGraph);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("report", report);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to generate circular dependency report", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Scan a folder for YAML files and generate dependency tree.
     */
    @PostMapping("/scan-folder")
    @Operation(summary = "Scan folder for YAML files", description = "Scan a folder recursively for YAML files and generate dependency tree")
    @ApiResponse(responseCode = "200", description = "Folder scanned successfully")
    @ApiResponse(responseCode = "400", description = "Invalid folder path")
    public ResponseEntity<Map<String, Object>> scanFolder(
            @Parameter(description = "Path to folder to scan")
            @RequestParam String folderPath) {
        logger.info("Scanning folder: {}", folderPath);

        try {
            if (folderPath == null || folderPath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "folderPath parameter is required"
                ));
            }

            java.nio.file.Path folder = java.nio.file.Paths.get(folderPath);
            if (!java.nio.file.Files.exists(folder) || !java.nio.file.Files.isDirectory(folder)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Folder does not exist or is not a directory: " + folderPath
                ));
            }

            // Find all YAML files in the folder
            List<java.nio.file.Path> yamlFiles = new ArrayList<>();
            try (var stream = java.nio.file.Files.walk(folder)) {
                stream.filter(path -> java.nio.file.Files.isRegularFile(path))
                      .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                      .forEach(yamlFiles::add);
            }

            if (yamlFiles.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "folderPath", folderPath,
                        "yamlFiles", List.of(),
                        "totalFiles", 0,
                        "message", "No YAML files found in folder"
                ));
            }

            // Sort files by name
            yamlFiles.sort(java.nio.file.Path::compareTo);

            // Convert to list of file info
            List<Map<String, Object>> fileList = yamlFiles.stream()
                    .map(path -> {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("path", path.toString());
                        fileInfo.put("name", path.getFileName().toString());
                        fileInfo.put("size", getFileSize(path));
                        return fileInfo;
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("folderPath", folderPath);
            response.put("yamlFiles", fileList);
            response.put("totalFiles", yamlFiles.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to scan folder", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get file size in bytes.
     */
    private long getFileSize(java.nio.file.Path path) {
        try {
            return java.nio.file.Files.size(path);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Generate dependency tree for a file using nested children format (D3 Hierarchy standard).
     */
    @GetMapping("/tree")
    @Operation(summary = "Generate dependency tree", description = "Generate a hierarchical tree view of dependencies for a file using nested children format")
    @ApiResponse(responseCode = "200", description = "Dependency tree generated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or analysis failed")
    public ResponseEntity<Map<String, Object>> getDependencyTree(
            @RequestParam String rootFile) {
        try {
            if (rootFile == null || rootFile.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "rootFile parameter is required"
                ));
            }

            // URL decode the file path in case it was encoded by the browser
            String decodedRootFile = java.net.URLDecoder.decode(rootFile, java.nio.charset.StandardCharsets.UTF_8);

            this.currentGraph = dependencyService.analyzeDependencies(decodedRootFile);
            // IMPORTANT: Build the tree starting from the graph's root key, not the raw request path
            String treeRoot = this.currentGraph.getRootFile();
            TreeNode tree = dependencyService.generateDependencyTree(this.currentGraph, treeRoot);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rootFile", treeRoot);
            response.put("totalFiles", this.currentGraph.getTotalFiles());
            response.put("maxDepth", this.currentGraph.getMaxDepth());
            response.put("tree", tree);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to generate dependency tree", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }


    /**
     * Validate the dependency tree and graph prior to UI rendering.
     */
    @PostMapping("/validate-tree")
    @Operation(summary = "Validate dependency tree", description = "Validate structure and graph integrity for a given root file before rendering the tree")
    @ApiResponse(responseCode = "200", description = "Validation completed")
    @ApiResponse(responseCode = "400", description = "Invalid request or analysis failed")
    public ResponseEntity<Map<String, Object>> validateTree(@RequestParam String rootFile) {
        try {
            if (rootFile == null || rootFile.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "rootFile parameter is required"
                ));
            }

            // URL decode the file path in case it was encoded by the browser
            String decodedRootFile = java.net.URLDecoder.decode(rootFile, java.nio.charset.StandardCharsets.UTF_8);

            // Build fresh graph + tree for the provided root
            this.currentGraph = dependencyService.analyzeDependencies(decodedRootFile);
            String treeRoot = this.currentGraph.getRootFile();
            TreeNode tree = dependencyService.generateDependencyTree(this.currentGraph, treeRoot);

            Map<String, Object> validation = treeValidationService.validate(this.currentGraph, tree);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("rootFile", treeRoot);
            response.put("validation", validation);
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to validate dependency tree", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get detailed information for a specific node in the dependency tree.
     */
    @GetMapping("/{filePath:.+}/details")
    @Operation(summary = "Get node details", description = "Get detailed information for a specific file in the dependency tree")
    @ApiResponse(responseCode = "200", description = "Node details retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    public ResponseEntity<Map<String, Object>> getNodeDetails(
            @Parameter(description = "File path to get details for")
            @PathVariable String filePath) {
        logger.info("Getting node details for: {}", filePath);

        try {
            if (currentGraph == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No graph loaded. Call /analyze first."
                ));
            }

            TreeNode node = dependencyService.getNodeDetails(currentGraph, filePath);
            if (node == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "File not found in dependency graph: " + filePath
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");

            Map<String, Object> data = new HashMap<>();
            data.put("name", node.getName());
            data.put("path", node.getPath());
            data.put("type", node.getType());
            data.put("dependencies", node.getDependencies() != null ? node.getDependencies() : List.of());
            data.put("allDependencies", node.getAllDependencies() != null ? node.getAllDependencies() : List.of());
            data.put("dependents", node.getDependents() != null ? node.getDependents() : List.of());
            data.put("healthScore", node.getHealthScore());
            data.put("author", node.getAuthor() != null ? node.getAuthor() : "Unknown");
            data.put("created", node.getCreated() != null ? node.getCreated() : "Unknown");
            data.put("lastModified", node.getLastModified() != null ? node.getLastModified() : "Unknown");
            data.put("version", node.getVersion() != null ? node.getVersion() : "1.0");
            // Include content summary so UI can display badges
            data.put("contentSummary", node.getContentSummary());
            data.put("circularDependencies", node.getCircularDependencies() != null ? node.getCircularDependencies() : List.of());

            response.put("data", data);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get node details", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }

    }
}

