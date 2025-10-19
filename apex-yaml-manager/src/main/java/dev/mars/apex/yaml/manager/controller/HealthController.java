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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for YAML Manager health and status operations.
 *
 * Provides endpoints for checking system health and status.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "YAML Manager health and status operations")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    /**
     * Health check endpoint for the YAML Manager API.
     */
    @GetMapping
    @Operation(
        summary = "Health check",
        description = "Check if the YAML Manager API is running and healthy."
    )
    @ApiResponse(responseCode = "200", description = "API is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("Health check requested");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "apex-yaml-manager");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Get system status and capabilities.
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get system status",
        description = "Get detailed system status and capabilities."
    )
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    public ResponseEntity<Map<String, Object>> status() {
        logger.debug("Status check requested");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OPERATIONAL");
        response.put("service", "apex-yaml-manager");
        response.put("version", "1.0.0");
        response.put("capabilities", new String[]{
            "dependency-analysis",
            "catalog-discovery",
            "health-checks",
            "refactoring-tools",
            "visualization"
        });
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}

