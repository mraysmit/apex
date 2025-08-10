package dev.mars.apex.rest.controller;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dev.mars.apex.rest.util.TestAwareLogger;

import java.io.ByteArrayInputStream;
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
 * REST Controller for configuration management operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuration", description = "Configuration management operations")
public class ConfigurationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    @Autowired
    private YamlConfigurationLoader yamlConfigurationLoader;

    @Autowired
    private TestAwareLogger testAwareLogger;
    
    // Store the current configuration for inspection
    private YamlRuleConfiguration currentConfiguration;
    private Instant configurationLoadTime;
    
    /**
     * Get current configuration information.
     */
    @GetMapping("/info")
    @Operation(
        summary = "Get configuration information",
        description = "Returns information about the currently loaded configuration including metadata and statistics."
    )
    @ApiResponse(responseCode = "200", description = "Configuration information retrieved successfully")
    public ResponseEntity<Map<String, Object>> getConfigurationInfo() {
        Map<String, Object> info = new HashMap<>();

        if (currentConfiguration != null) {
            info.put("hasConfiguration", true);
            info.put("loadTime", configurationLoadTime);

            // Add metadata if available
            if (currentConfiguration.getMetadata() != null) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", currentConfiguration.getMetadata().getName());
                metadata.put("version", currentConfiguration.getMetadata().getVersion());
                metadata.put("description", currentConfiguration.getMetadata().getDescription());
                metadata.put("author", currentConfiguration.getMetadata().getAuthor());
                metadata.put("created", currentConfiguration.getMetadata().getCreated());
                metadata.put("lastModified", currentConfiguration.getMetadata().getLastModified());
                metadata.put("tags", currentConfiguration.getMetadata().getTags());
                info.put("metadata", metadata);
            }

            // Add statistics
            Map<String, Object> stats = new HashMap<>();
            stats.put("rulesCount", currentConfiguration.getRules() != null ?
                     currentConfiguration.getRules().size() : 0);
            stats.put("ruleGroupsCount", currentConfiguration.getRuleGroups() != null ?
                     currentConfiguration.getRuleGroups().size() : 0);
            stats.put("enrichmentsCount", currentConfiguration.getEnrichments() != null ?
                     currentConfiguration.getEnrichments().size() : 0);
            stats.put("categoriesCount", currentConfiguration.getCategories() != null ?
                     currentConfiguration.getCategories().size() : 0);
            info.put("statistics", stats);

        } else {
            info.put("hasConfiguration", false);
            info.put("message", "No configuration currently loaded");
        }

        info.put("timestamp", Instant.now());

        return ResponseEntity.ok(info);
    }
    
    /**
     * Load configuration from YAML content.
     */
    @PostMapping("/load")
    @Operation(
        summary = "Load YAML configuration",
        description = "Loads a new YAML configuration from the provided content. This replaces any existing configuration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration loaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid YAML configuration"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> loadConfiguration(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "YAML configuration content",
                content = @Content(
                    mediaType = "application/x-yaml",
                    examples = @ExampleObject(
                        name = "Sample configuration",
                        value = """
                        metadata:
                          name: "Sample Rules"
                          version: "1.0.0"
                          description: "Sample rules configuration"

                        rules:
                          - id: "sample-rule"
                            name: "Sample Rule"
                            condition: "#value > 0"
                            message: "Value is positive"
                        """
                    )
                )
            )
            String yamlContent) throws dev.mars.apex.core.config.yaml.YamlConfigurationException {

        logger.info("Loading new YAML configuration");
        logger.debug("YAML content length: {} characters", yamlContent.length());

        // Use test-aware logger for enhanced testing capabilities
        testAwareLogger.warn(logger, "Configuration loading initiated via REST API - this may cause temporary service disruption");
        testAwareLogger.error(logger, "YAML configuration size: {} bytes", yamlContent.getBytes().length);

        // Load configuration from string content
        YamlRuleConfiguration config = yamlConfigurationLoader.loadFromStream(
            new ByteArrayInputStream(yamlContent.getBytes())
        );

        // Store the configuration
        this.currentConfiguration = config;
        this.configurationLoadTime = Instant.now();

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Configuration loaded successfully");
        response.put("loadTime", configurationLoadTime);

        if (config.getMetadata() != null) {
            response.put("configurationName", config.getMetadata().getName());
            response.put("configurationVersion", config.getMetadata().getVersion());
        }

        // Add statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("rulesCount", config.getRules() != null ? config.getRules().size() : 0);
        stats.put("ruleGroupsCount", config.getRuleGroups() != null ? config.getRuleGroups().size() : 0);
        stats.put("enrichmentsCount", config.getEnrichments() != null ? config.getEnrichments().size() : 0);
        response.put("statistics", stats);

        logger.info("Configuration loaded successfully: {} rules, {} enrichments",
                   stats.get("rulesCount"), stats.get("enrichmentsCount"));

        return ResponseEntity.ok(response);
    }
    
    /**
     * Load configuration from uploaded file.
     */
    @PostMapping("/upload")
    @Operation(
        summary = "Upload YAML configuration file",
        description = "Uploads and loads a YAML configuration file. This replaces any existing configuration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration file uploaded and loaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or YAML configuration"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> uploadConfiguration(
            @Parameter(description = "YAML configuration file to upload")
            @RequestParam("file") MultipartFile file) throws dev.mars.apex.core.config.yaml.YamlConfigurationException, java.io.IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "No file provided"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".yaml") &&
            !filename.toLowerCase().endsWith(".yml"))) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "File must be a YAML file (.yaml or .yml)"));
        }

        logger.info("Uploading configuration file: {}", filename);

        // Load configuration from uploaded file
        YamlRuleConfiguration config = yamlConfigurationLoader.loadFromStream(
            file.getInputStream()
        );

        // Store the configuration
        this.currentConfiguration = config;
        this.configurationLoadTime = Instant.now();

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Configuration file uploaded and loaded successfully");
        response.put("fileName", file.getOriginalFilename());
        response.put("fileSize", file.getSize());
        response.put("loadTime", configurationLoadTime);

        if (config.getMetadata() != null) {
            response.put("configurationName", config.getMetadata().getName());
            response.put("configurationVersion", config.getMetadata().getVersion());
        }

        // Add statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("rulesCount", config.getRules() != null ? config.getRules().size() : 0);
        stats.put("ruleGroupsCount", config.getRuleGroups() != null ? config.getRuleGroups().size() : 0);
        stats.put("enrichmentsCount", config.getEnrichments() != null ? config.getEnrichments().size() : 0);
        response.put("statistics", stats);

        logger.info("Configuration file loaded successfully: {} ({} bytes)",
                   file.getOriginalFilename(), file.getSize());

        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate YAML configuration without loading it.
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate YAML configuration",
        description = "Validates the provided YAML configuration without loading it into the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration is valid"),
        @ApiResponse(responseCode = "400", description = "Configuration is invalid")
    })
    public ResponseEntity<Map<String, Object>> validateConfiguration(
            @RequestBody String yamlContent) throws dev.mars.apex.core.config.yaml.YamlConfigurationException {

        logger.info("Validating YAML configuration");

        // Attempt to parse the configuration
        YamlRuleConfiguration config = yamlConfigurationLoader.loadFromStream(
            new ByteArrayInputStream(yamlContent.getBytes())
        );

        // Prepare validation response
        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("message", "Configuration is valid");
        response.put("timestamp", Instant.now());

        if (config.getMetadata() != null) {
            response.put("configurationName", config.getMetadata().getName());
            response.put("configurationVersion", config.getMetadata().getVersion());
        }

        // Add statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("rulesCount", config.getRules() != null ? config.getRules().size() : 0);
        stats.put("ruleGroupsCount", config.getRuleGroups() != null ? config.getRuleGroups().size() : 0);
        stats.put("enrichmentsCount", config.getEnrichments() != null ? config.getEnrichments().size() : 0);
        response.put("statistics", stats);

        logger.info("Configuration validation successful");
        return ResponseEntity.ok(response);
    }
}
