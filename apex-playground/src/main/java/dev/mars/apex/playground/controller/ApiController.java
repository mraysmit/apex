package dev.mars.apex.playground.controller;

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


import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import dev.mars.apex.playground.model.YamlValidationResponse;
import dev.mars.apex.playground.service.PlaygroundService;
import dev.mars.apex.playground.service.YamlValidationService;
import dev.mars.apex.playground.service.ExampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for APEX Playground operations.
 * 
 * Provides REST endpoints for processing data with YAML rules,
 * validating configurations, and managing playground operations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@RestController
@RequestMapping("/playground/api")
@Tag(name = "Playground API", description = "APEX Playground REST API operations")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final PlaygroundService playgroundService;
    private final YamlValidationService yamlValidationService;
    private final ExampleService exampleService;

    @Autowired
    public ApiController(PlaygroundService playgroundService, YamlValidationService yamlValidationService, ExampleService exampleService) {
        this.playgroundService = playgroundService;
        this.yamlValidationService = yamlValidationService;
        this.exampleService = exampleService;
    }

    /**
     * Health check endpoint for the playground API.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the playground API is running and healthy."
    )
    @ApiResponse(responseCode = "200", description = "API is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("Health check requested");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "apex-playground");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Process data with YAML rules configuration.
     */
    @PostMapping("/process")
    @Operation(
        summary = "Process data with YAML rules",
        description = "Process source data using YAML rules configuration and return validation and enrichment results."
    )
    @ApiResponse(responseCode = "200", description = "Processing completed successfully")
    public ResponseEntity<PlaygroundResponse> processData(
            @RequestBody PlaygroundRequest request) {

        logger.info("Processing data request received: {}", request);

        try {
            PlaygroundResponse response = playgroundService.processData(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing data", e);
            PlaygroundResponse errorResponse = new PlaygroundResponse(false, "Processing failed: " + e.getMessage());
            errorResponse.addError(e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Validate YAML configuration syntax.
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate YAML configuration",
        description = "Validate YAML rules configuration syntax and structure."
    )
    @ApiResponse(responseCode = "200", description = "Validation completed")
    public ResponseEntity<YamlValidationResponse> validateYaml(
            @RequestBody Map<String, Object> request) {

        logger.info("YAML validation request received");

        try {
            String yamlContent = (String) request.get("yamlContent");
            if (yamlContent == null) {
                YamlValidationResponse errorResponse = new YamlValidationResponse(false, "YAML content is required");
                errorResponse.addError("Missing 'yamlContent' field in request", 0, 0);
                return ResponseEntity.ok(errorResponse);
            }

            YamlValidationResponse response = yamlValidationService.validateYaml(yamlContent);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating YAML", e);
            YamlValidationResponse errorResponse = new YamlValidationResponse(false, "Validation failed: " + e.getMessage());
            errorResponse.addError("Validation error: " + e.getMessage(), 0, 0);
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get available example templates from apex-demo module.
     */
    @GetMapping("/examples")
    @Operation(
        summary = "Get example templates",
        description = "Retrieve available example data and YAML rules templates from apex-demo module."
    )
    @ApiResponse(responseCode = "200", description = "Examples retrieved successfully")
    public ResponseEntity<Map<String, Object>> getExamples() {
        logger.info("Loading examples from apex-demo module");

        try {
            Map<String, Object> examples = exampleService.getAllExamples();
            examples.put("timestamp", System.currentTimeMillis());
            examples.put("message", "Examples loaded from apex-demo module");

            return ResponseEntity.ok(examples);

        } catch (Exception e) {
            logger.error("Error loading examples", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load examples: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get a specific example by category and name.
     */
    @GetMapping("/examples/{category}/{name}")
    @Operation(
        summary = "Get specific example",
        description = "Retrieve a specific example configuration by category and name."
    )
    @ApiResponse(responseCode = "200", description = "Example retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Example not found")
    public ResponseEntity<Map<String, Object>> getExample(
            @PathVariable String category,
            @PathVariable String name) {
        logger.info("Loading example: {}/{}", category, name);

        try {
            Map<String, Object> example = exampleService.getExample(category, name);

            if (example.containsKey("error")) {
                return ResponseEntity.notFound().build();
            }

            example.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(example);

        } catch (Exception e) {
            logger.error("Error loading example {}/{}", category, name, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load example: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Upload data file.
     */
    @PostMapping("/upload/data")
    @Operation(
        summary = "Upload data file",
        description = "Upload a data file (JSON, XML, CSV, or TXT) to be used as source data."
    )
    @ApiResponse(responseCode = "200", description = "Data file uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file or file format")
    @ApiResponse(responseCode = "413", description = "File size exceeds limit")
    public ResponseEntity<Map<String, Object>> uploadDataFile(
            @Parameter(description = "Data file to upload")
            @RequestParam("file") MultipartFile file) {

        logger.info("Uploading data file: {}", file.getOriginalFilename());

        try {
            // Validate file
            validateDataFile(file);

            // Read file content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Auto-detect format
            String format = detectDataFormat(file.getOriginalFilename());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Data file uploaded successfully");
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("content", content);
            response.put("detectedFormat", format);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid data file upload: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Error uploading data file", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Upload YAML configuration file.
     */
    @PostMapping("/upload/yaml")
    @Operation(
        summary = "Upload YAML file",
        description = "Upload a YAML configuration file to be used as rules configuration."
    )
    @ApiResponse(responseCode = "200", description = "YAML file uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file or YAML format")
    @ApiResponse(responseCode = "413", description = "File size exceeds limit")
    public ResponseEntity<Map<String, Object>> uploadYamlFile(
            @Parameter(description = "YAML file to upload")
            @RequestParam("file") MultipartFile file) {

        logger.info("Uploading YAML file: {}", file.getOriginalFilename());

        try {
            // Validate file
            validateYamlFile(file);

            // Read file content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Validate YAML syntax
            YamlValidationResponse validationResult = yamlValidationService.validateYaml(content);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException("Invalid YAML syntax: " + validationResult.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "YAML file uploaded successfully");
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("content", content);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid YAML file upload: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Error uploading YAML file", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Private helper methods for file validation

    /**
     * Validate data file.
     */
    private void validateDataFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size (10MB limit)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (10MB)");
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name is required");
        }

        String extension = fileName.toLowerCase();
        if (!extension.endsWith(".json") && !extension.endsWith(".xml") &&
            !extension.endsWith(".csv") && !extension.endsWith(".txt")) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: .json, .xml, .csv, .txt");
        }
    }

    /**
     * Validate YAML file.
     */
    private void validateYamlFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size (10MB limit)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (10MB)");
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name is required");
        }

        String extension = fileName.toLowerCase();
        if (!extension.endsWith(".yaml") && !extension.endsWith(".yml")) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: .yaml, .yml");
        }
    }

    /**
     * Detect data format based on file extension.
     */
    private String detectDataFormat(String fileName) {
        if (fileName == null) {
            return "JSON";
        }

        String extension = fileName.toLowerCase();
        if (extension.endsWith(".xml")) {
            return "XML";
        } else if (extension.endsWith(".csv")) {
            return "CSV";
        } else {
            return "JSON"; // Default for .json and .txt files
        }
    }
}
