package dev.mars.apex.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

/**
 * API Version information controller.
 * 
 * Provides information about API versions, deprecation status, and migration paths.
 * This controller helps clients understand version compatibility and plan migrations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-31
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
@Tag(name = "API Version", description = "API version information and compatibility")
public class ApiVersionController {
    
    /**
     * Get current API version information.
     */
    @GetMapping(value = "/version", produces = {org.springframework.http.MediaType.APPLICATION_JSON_VALUE, "application/vnd.apex.v1+json"})
    @Operation(
        summary = "Get API version information",
        description = "Returns information about the current API version, supported versions, and deprecation status."
    )
    @ApiResponse(responseCode = "200", description = "Version information retrieved successfully")
    public ResponseEntity<Map<String, Object>> getVersionInfo() {
        Map<String, Object> versionInfo = new java.util.HashMap<>();
        versionInfo.put("currentVersion", "v1");
        versionInfo.put("apiVersion", "1.0.0");
        versionInfo.put("status", "stable");
        versionInfo.put("releaseDate", "2025-07-31");
        versionInfo.put("supportedVersions", List.of("v1"));
        versionInfo.put("deprecatedVersions", List.of());
        versionInfo.put("endOfLifeVersions", List.of());
        versionInfo.put("latestVersion", "v1");
        versionInfo.put("timestamp", Instant.now());
        versionInfo.put("documentation", Map.of(
            "v1", "https://docs.apex.dev/api/v1/",
            "swagger", "https://api.apex.dev/swagger-ui.html"
        ));
        versionInfo.put("migration", Map.of(
            "guides", "https://docs.apex.dev/migration/",
            "support", "migration-help@apex.dev"
        ));

        return ResponseEntity.ok(versionInfo);
    }
    
    /**
     * Get version compatibility matrix.
     */
    @GetMapping(value = "/version/compatibility", produces = {org.springframework.http.MediaType.APPLICATION_JSON_VALUE, "application/vnd.apex.v1+json"})
    @Operation(
        summary = "Get version compatibility information",
        description = "Returns compatibility information between different API versions and client requirements."
    )
    @ApiResponse(responseCode = "200", description = "Compatibility information retrieved successfully")
    public ResponseEntity<Map<String, Object>> getCompatibilityInfo() {
        Map<String, Object> compatibilityInfo = new java.util.HashMap<>();

        compatibilityInfo.put("compatibilityMatrix", Map.of(
            "v1", Map.of(
                "status", "current",
                "backwardCompatible", List.of(),
                "forwardCompatible", List.of("v2-beta"),
                "breakingChanges", List.of(),
                "supportUntil", "TBD"
            )
        ));

        compatibilityInfo.put("clientRequirements", Map.of(
            "minimumVersion", "v1",
            "recommendedVersion", "v1",
            "headers", Map.of(
                "required", List.of("Content-Type"),
                "optional", List.of("API-Version", "Accept")
            )
        ));

        compatibilityInfo.put("featureSupport", Map.of(
            "v1", List.of(
                "rule-evaluation",
                "validation",
                "configuration-management",
                "monitoring",
                "error-handling"
            )
        ));

        compatibilityInfo.put("timestamp", Instant.now());

        return ResponseEntity.ok(compatibilityInfo);
    }
    
    /**
     * Get deprecation information.
     */
    @GetMapping(value = "/version/deprecation", produces = {org.springframework.http.MediaType.APPLICATION_JSON_VALUE, "application/vnd.apex.v1+json"})
    @Operation(
        summary = "Get deprecation information",
        description = "Returns information about deprecated API versions and migration timelines."
    )
    @ApiResponse(responseCode = "200", description = "Deprecation information retrieved successfully")
    public ResponseEntity<Map<String, Object>> getDeprecationInfo() {
        Map<String, Object> deprecationInfo = new java.util.HashMap<>();

        deprecationInfo.put("currentDeprecations", List.of());
        deprecationInfo.put("plannedDeprecations", List.of());
        deprecationInfo.put("endOfLifeSchedule", List.of());

        deprecationInfo.put("migrationTimeline", Map.of(
            "notice", "Deprecation notices will be provided at least 12 months in advance",
            "support", "Deprecated versions receive security fixes only",
            "migration", "Migration guides and tools will be provided"
        ));

        deprecationInfo.put("policy", Map.of(
            "minimumSupportPeriod", "12 months",
            "deprecationWarnings", "Provided via HTTP headers and response metadata",
            "breakingChangePolicy", "Only in major version updates"
        ));

        deprecationInfo.put("contact", Map.of(
            "support", "api-team@apex.dev",
            "migration", "migration-help@apex.dev",
            "documentation", "https://docs.apex.dev/versioning/"
        ));

        deprecationInfo.put("timestamp", Instant.now());

        return ResponseEntity.ok(deprecationInfo);
    }
    
    /**
     * Health check with version information.
     */
    @GetMapping(value = "/version/health", produces = {org.springframework.http.MediaType.APPLICATION_JSON_VALUE, "application/vnd.apex.v1+json"})
    @Operation(
        summary = "Version-aware health check",
        description = "Returns health status with version-specific information."
    )
    @ApiResponse(responseCode = "200", description = "Health check completed successfully")
    public ResponseEntity<Map<String, Object>> getVersionHealth() {
        Map<String, Object> healthInfo = new java.util.HashMap<>();

        healthInfo.put("status", "healthy");
        healthInfo.put("version", "v1");
        healthInfo.put("apiVersion", "1.0.0");

        healthInfo.put("compatibility", Map.of(
            "backward", "stable",
            "forward", "planned"
        ));

        healthInfo.put("features", Map.of(
            "ruleEvaluation", "available",
            "validation", "available",
            "configuration", "available",
            "monitoring", "available"
        ));

        healthInfo.put("performance", Map.of(
            "responseTime", "optimal",
            "throughput", "normal",
            "errorRate", "low"
        ));

        healthInfo.put("timestamp", Instant.now());

        return ResponseEntity.ok(healthInfo);
    }
}
