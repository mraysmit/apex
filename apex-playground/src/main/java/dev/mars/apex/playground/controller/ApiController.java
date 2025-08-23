package dev.mars.apex.playground.controller;

import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import dev.mars.apex.playground.model.YamlValidationResponse;
import dev.mars.apex.playground.service.PlaygroundService;
import dev.mars.apex.playground.service.YamlValidationService;
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

    @Autowired
    public ApiController(PlaygroundService playgroundService, YamlValidationService yamlValidationService) {
        this.playgroundService = playgroundService;
        this.yamlValidationService = yamlValidationService;
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
     * Get available example templates.
     * This is a placeholder implementation that will be expanded in Phase 2.
     */
    @GetMapping("/examples")
    @Operation(
        summary = "Get example templates",
        description = "Retrieve available example data and YAML rules templates."
    )
    @ApiResponse(responseCode = "200", description = "Examples retrieved successfully")
    public ResponseEntity<Map<String, Object>> getExamples() {
        logger.info("Examples request received");
        
        // Placeholder response - will be implemented in Phase 2
        Map<String, Object> response = new HashMap<>();
        response.put("examples", new String[]{"customer-validation", "financial-rules", "data-enrichment"});
        response.put("message", "Examples endpoint ready - implementation coming in Phase 2");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
