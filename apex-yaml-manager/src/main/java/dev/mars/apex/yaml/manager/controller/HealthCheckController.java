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

import dev.mars.apex.yaml.manager.model.HealthReport;
import dev.mars.apex.yaml.manager.model.HealthScore;
import dev.mars.apex.yaml.manager.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for health check operations.
 *
 * Provides endpoints for:
 * - Performing health checks on YAML files
 * - Generating health reports
 * - Getting health scores
 * - Retrieving recommendations
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@RestController
@RequestMapping("/api/health-checks")
@Tag(name = "Health Checks", description = "YAML configuration health check operations")
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);

    @Autowired(required = false)
    private HealthCheckService healthCheckService;

    /**
     * Perform a health check on a YAML file.
     */
    @PostMapping("/check")
    @Operation(
        summary = "Perform health check",
        description = "Perform a comprehensive health check on a YAML configuration file"
    )
    @ApiResponse(responseCode = "200", description = "Health check completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Health check service not available")
    public ResponseEntity<?> performHealthCheck(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath,
            @Parameter(description = "Base directory for resolving references")
            @RequestParam(required = false, defaultValue = ".") String baseDir) {
        
        logger.debug("Health check requested for: {}", filePath);
        
        if (healthCheckService == null) {
            return ResponseEntity.status(500).body("Health check service not available");
        }
        
        try {
            HealthScore score = healthCheckService.performHealthCheck(filePath, baseDir);
            return ResponseEntity.ok(score);
        } catch (Exception e) {
            logger.error("Error performing health check: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error performing health check: " + e.getMessage());
        }
    }

    /**
     * Generate a comprehensive health report.
     */
    @PostMapping("/report")
    @Operation(
        summary = "Generate health report",
        description = "Generate a comprehensive health report with issues and recommendations"
    )
    @ApiResponse(responseCode = "200", description = "Health report generated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Health check service not available")
    public ResponseEntity<?> generateHealthReport(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath,
            @Parameter(description = "Base directory for resolving references")
            @RequestParam(required = false, defaultValue = ".") String baseDir) {
        
        logger.debug("Health report requested for: {}", filePath);
        
        if (healthCheckService == null) {
            return ResponseEntity.status(500).body("Health check service not available");
        }
        
        try {
            HealthReport report = healthCheckService.generateHealthReport(filePath, baseDir);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error generating health report: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error generating health report: " + e.getMessage());
        }
    }

    /**
     * Get health score for a file.
     */
    @GetMapping("/score")
    @Operation(
        summary = "Get health score",
        description = "Get the health score for a YAML configuration file"
    )
    @ApiResponse(responseCode = "200", description = "Health score retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Health check service not available")
    public ResponseEntity<?> getHealthScore(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath,
            @Parameter(description = "Base directory for resolving references")
            @RequestParam(required = false, defaultValue = ".") String baseDir) {
        
        logger.debug("Health score requested for: {}", filePath);
        
        if (healthCheckService == null) {
            return ResponseEntity.status(500).body("Health check service not available");
        }
        
        try {
            HealthScore score = healthCheckService.performHealthCheck(filePath, baseDir);
            return ResponseEntity.ok(score);
        } catch (Exception e) {
            logger.error("Error getting health score: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error getting health score: " + e.getMessage());
        }
    }

    /**
     * Check if a file is healthy.
     */
    @GetMapping("/is-healthy")
    @Operation(
        summary = "Check if file is healthy",
        description = "Check if a YAML configuration file is healthy (score >= 75)"
    )
    @ApiResponse(responseCode = "200", description = "Health status retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Health check service not available")
    public ResponseEntity<?> isHealthy(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath,
            @Parameter(description = "Base directory for resolving references")
            @RequestParam(required = false, defaultValue = ".") String baseDir) {
        
        logger.debug("Health status check requested for: {}", filePath);
        
        if (healthCheckService == null) {
            return ResponseEntity.status(500).body("Health check service not available");
        }
        
        try {
            HealthScore score = healthCheckService.performHealthCheck(filePath, baseDir);
            return ResponseEntity.ok(score.isHealthy());
        } catch (Exception e) {
            logger.error("Error checking health status: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error checking health status: " + e.getMessage());
        }
    }
}

