package dev.mars.apex.rest.controller;

import dev.mars.apex.rest.service.RuleEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
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
 * REST Controller for monitoring and health check operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@RestController
@RequestMapping("/api/monitoring")
@Tag(name = "Monitoring", description = "System monitoring and health check operations")
public class MonitoringController {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);
    
    @Autowired
    private RuleEvaluationService ruleEvaluationService;
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Returns the health status of the Rules Engine API service."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy"),
        @ApiResponse(responseCode = "503", description = "Service is unhealthy")
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        // Basic health indicators
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "Rules Engine REST API");
        health.put("version", "1.0.0");

        // Test basic functionality
        boolean basicFunctionality = testBasicFunctionality();
        health.put("basicFunctionality", basicFunctionality ? "UP" : "DOWN");

        // Memory information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("max", runtime.maxMemory());

        double memoryUsagePercent = ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100;
        memory.put("usagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0);

        health.put("memory", memory);

        // System information
        Map<String, Object> system = new HashMap<>();
        system.put("availableProcessors", runtime.availableProcessors());
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        health.put("system", system);

        // Determine overall health status
        boolean isHealthy = basicFunctionality && memoryUsagePercent < 90;

        if (isHealthy) {
            logger.debug("Health check passed");
            return ResponseEntity.ok(health);
        } else {
            health.put("status", "DOWN");
            logger.warn("Health check failed: basicFunctionality={}, memoryUsage={}%",
                       basicFunctionality, memoryUsagePercent);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    /**
     * System statistics endpoint.
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Get system statistics",
        description = "Returns detailed system statistics including memory usage, performance metrics, and rule engine information."
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        logger.debug("Retrieving system statistics");

        Map<String, Object> stats = ruleEvaluationService.getSystemInfo();

        // Add additional statistics
        stats.put("service", "Rules Engine REST API");
        stats.put("version", "1.0.0");
        stats.put("uptime", getUptime());

        // Add JVM statistics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("totalMemory", runtime.totalMemory());
        jvm.put("freeMemory", runtime.freeMemory());
        jvm.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        jvm.put("maxMemory", runtime.maxMemory());
        jvm.put("availableProcessors", runtime.availableProcessors());

        // Calculate memory usage percentage
        double memoryUsagePercent = ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100;
        jvm.put("memoryUsagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0);

        stats.put("jvm", jvm);

        // Add garbage collection information
        Map<String, Object> gc = new HashMap<>();
        gc.put("totalGCTime", getTotalGCTime());
        gc.put("gcCount", getGCCount());
        stats.put("garbageCollection", gc);

        logger.debug("System statistics retrieved successfully");
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Performance metrics endpoint.
     */
    @GetMapping("/performance")
    @Operation(
        summary = "Get performance metrics",
        description = "Returns performance metrics for the Rules Engine including evaluation times and throughput."
    )
    @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        logger.debug("Retrieving performance metrics");

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", Instant.now());
        metrics.put("service", "Rules Engine REST API");

        // Add basic performance information
        // Note: In a real implementation, you would collect actual performance metrics
        // from the Rules Engine's performance monitoring system

        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("totalEvaluations", 0); // Would be tracked in real implementation
        evaluation.put("averageEvaluationTime", 0); // Would be calculated from actual metrics
        evaluation.put("successRate", 100.0); // Would be calculated from actual metrics
        metrics.put("ruleEvaluation", evaluation);

        Map<String, Object> validation = new HashMap<>();
        validation.put("totalValidations", 0); // Would be tracked in real implementation
        validation.put("averageValidationTime", 0); // Would be calculated from actual metrics
        validation.put("averageRulesPerValidation", 0); // Would be calculated from actual metrics
        metrics.put("validation", validation);

        // Add system performance
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> system = new HashMap<>();
        system.put("memoryUsage", runtime.totalMemory() - runtime.freeMemory());
        system.put("memoryUsagePercent",
                  ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100);
        system.put("availableProcessors", runtime.availableProcessors());
        metrics.put("system", system);

        logger.debug("Performance metrics retrieved successfully");
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Ready check endpoint for Kubernetes readiness probes.
     */
    @GetMapping("/ready")
    @Operation(
        summary = "Readiness check",
        description = "Returns whether the service is ready to accept requests."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is ready"),
        @ApiResponse(responseCode = "503", description = "Service is not ready")
    })
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        // Check if the service is ready to handle requests
        boolean isReady = testBasicFunctionality();

        Map<String, Object> readiness = new HashMap<>();
        readiness.put("ready", isReady);
        readiness.put("timestamp", Instant.now());
        readiness.put("service", "Rules Engine REST API");

        if (isReady) {
            logger.debug("Readiness check passed");
            return ResponseEntity.ok(readiness);
        } else {
            logger.warn("Readiness check failed");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(readiness);
        }
    }
    
    // Helper methods
    
    private boolean testBasicFunctionality() {
        try {
            // Test basic rule evaluation functionality
            Map<String, Object> testData = Map.of("test", true);
            return ruleEvaluationService.quickCheck("#test == true", testData);
        } catch (Exception e) {
            logger.warn("Basic functionality test failed: {}", e.getMessage());
            return false;
        }
    }
    
    private long getUptime() {
        // Simple uptime calculation based on JVM uptime
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
    }
    
    private long getTotalGCTime() {
        // Get total garbage collection time
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .mapToLong(gcBean -> gcBean.getCollectionTime())
                .sum();
    }
    
    private long getGCCount() {
        // Get total garbage collection count
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .mapToLong(gcBean -> gcBean.getCollectionCount())
                .sum();
    }
}
