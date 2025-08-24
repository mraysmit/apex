package dev.mars.apex.playground.controller;

import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import dev.mars.apex.playground.model.YamlValidationResponse;
import dev.mars.apex.playground.service.PlaygroundService;
import dev.mars.apex.playground.service.YamlValidationService;
import dev.mars.apex.playground.service.ExampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
