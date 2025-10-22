package dev.mars.apex.rest.controller;

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

import dev.mars.apex.rest.dto.ApiResponse;
import dev.mars.apex.rest.service.DependencyAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * REST Controller for YAML dependency analysis operations.
 * 
 * Provides centralized REST API endpoints for analyzing YAML file dependencies,
 * generating dependency trees, and performing impact analysis.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-22
 * @version 1.0
 */
@RestController
@RequestMapping("/api/dependencies")
@Tag(name = "Dependency Analysis", description = "YAML dependency analysis and tree generation operations")
public class DependencyController {

    private static final Logger logger = LoggerFactory.getLogger(DependencyController.class);

    @Autowired
    private DependencyAnalysisService dependencyAnalysisService;

    /**
     * Generate dependency tree for a YAML file using D3 Hierarchy standard.
     */
    @GetMapping("/tree")
    @Operation(
        summary = "Generate dependency tree", 
        description = "Generate a hierarchical tree view of dependencies for a YAML file using nested children format (D3 Hierarchy standard)"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dependency tree generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or analysis failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDependencyTree(
            @Parameter(description = "Root YAML file path to analyze", required = true)
            @RequestParam @NotBlank String rootFile) {
        
        logger.info("Generating dependency tree for: {}", rootFile);
        
        try {
            Map<String, Object> treeData = dependencyAnalysisService.generateDependencyTree(rootFile);
            
            logger.info("Successfully generated dependency tree with {} files and max depth {}",
                treeData.get("totalFiles"), treeData.get("maxDepth"));
            
            return ResponseEntity.ok(ApiResponse.success(treeData));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for dependency tree: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", "Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to generate dependency tree for: " + rootFile, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("TREE_GENERATION_FAILED", "Failed to generate dependency tree: " + e.getMessage()));
        }
    }

    /**
     * Analyze dependencies for a YAML file.
     */
    @PostMapping("/analyze")
    @Operation(
        summary = "Analyze YAML dependencies",
        description = "Analyze the complete dependency graph starting from a root YAML file"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dependencies analyzed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file path"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Analysis failed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeDependencies(
            @Parameter(description = "YAML file path to analyze", required = true)
            @RequestParam @NotBlank String filePath) {
        
        logger.info("Analyzing dependencies for: {}", filePath);
        
        try {
            Map<String, Object> analysisResult = dependencyAnalysisService.analyzeDependencies(filePath);
            
            logger.info("Successfully analyzed dependencies for {} with {} total files",
                filePath, analysisResult.get("totalFiles"));
            
            return ResponseEntity.ok(ApiResponse.success(analysisResult));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file path for dependency analysis: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_FILE_PATH", "Invalid file path: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to analyze dependencies for: " + filePath, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ANALYSIS_FAILED", "Failed to analyze dependencies: " + e.getMessage()));
        }
    }

    /**
     * Validate dependency tree structure.
     */
    @PostMapping("/validate-tree")
    @Operation(
        summary = "Validate dependency tree",
        description = "Validate structure and graph integrity for a given root file before rendering the tree"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Validation completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Validation error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateTree(
            @Parameter(description = "Root YAML file path to validate", required = true)
            @RequestParam @NotBlank String rootFile) {
        
        logger.info("Validating dependency tree for: {}", rootFile);
        
        try {
            Map<String, Object> validationResult = dependencyAnalysisService.validateDependencyTree(rootFile);
            
            logger.info("Successfully validated dependency tree for: {}", rootFile);
            
            return ResponseEntity.ok(ApiResponse.success(validationResult));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for tree validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", "Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to validate dependency tree for: " + rootFile, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("VALIDATION_FAILED", "Failed to validate dependency tree: " + e.getMessage()));
        }
    }

    /**
     * Get detailed information for a specific node in the dependency tree.
     */
    @GetMapping("/{filePath:.+}/details")
    @Operation(
        summary = "Get node details",
        description = "Get detailed information for a specific file in the dependency tree"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Node details retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file path"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found in dependency graph"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to retrieve node details")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNodeDetails(
            @Parameter(description = "File path to get details for", required = true)
            @PathVariable String filePath) {
        
        logger.info("Getting node details for: {}", filePath);
        
        try {
            Map<String, Object> nodeDetails = dependencyAnalysisService.getNodeDetails(filePath);
            
            logger.debug("Successfully retrieved node details for: {}", filePath);
            
            return ResponseEntity.ok(ApiResponse.success(nodeDetails));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file path for node details: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_FILE_PATH", "Invalid file path: " + e.getMessage()));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                logger.warn("File not found in dependency graph: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            throw e;

        } catch (Exception e) {
            logger.error("Failed to get node details for: " + filePath, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("NODE_DETAILS_FAILED", "Failed to get node details: " + e.getMessage()));
        }
    }

    /**
     * Scan a folder for YAML files.
     */
    @PostMapping("/scan-folder")
    @Operation(
        summary = "Scan folder for YAML files",
        description = "Scan a folder recursively for YAML files and return file list"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Folder scanned successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid folder path"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Scan failed")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> scanFolder(
            @Parameter(description = "Path to folder to scan", required = true)
            @RequestParam @NotBlank String folderPath) {
        
        logger.info("Scanning folder: {}", folderPath);
        
        try {
            Map<String, Object> scanResult = dependencyAnalysisService.scanFolder(folderPath);
            
            logger.info("Successfully scanned folder {} and found {} files",
                folderPath, scanResult.get("filesFound"));
            
            return ResponseEntity.ok(ApiResponse.success(scanResult));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid folder path for scanning: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_FOLDER_PATH", "Invalid folder path: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to scan folder: " + folderPath, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("SCAN_FAILED", "Failed to scan folder: " + e.getMessage()));
        }
    }
}
