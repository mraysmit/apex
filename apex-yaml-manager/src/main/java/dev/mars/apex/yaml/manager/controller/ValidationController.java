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

import dev.mars.apex.yaml.manager.model.ValidationResult;
import dev.mars.apex.yaml.manager.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for validation operations.
 *
 * Provides endpoints for:
 * - Structural validation (YAML syntax, required fields)
 * - Reference validation (file references)
 * - Consistency validation (naming conventions, unique IDs)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@RestController
@RequestMapping("/api/validation")
@Tag(name = "Validation", description = "YAML configuration validation operations")
public class ValidationController {

    private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);

    @Autowired(required = false)
    private ValidationService validationService;

    /**
     * Validate YAML file structure.
     */
    @PostMapping("/structure")
    @Operation(
        summary = "Validate structure",
        description = "Validate YAML syntax and required fields"
    )
    @ApiResponse(responseCode = "200", description = "Validation completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Validation service not available")
    public ResponseEntity<?> validateStructure(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath) {
        
        logger.debug("Structure validation requested for: {}", filePath);
        
        if (validationService == null) {
            return ResponseEntity.status(500).body("Validation service not available");
        }
        
        try {
            ValidationResult result = validationService.validateStructure(filePath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error validating structure: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error validating structure: " + e.getMessage());
        }
    }

    /**
     * Validate file references.
     */
    @PostMapping("/references")
    @Operation(
        summary = "Validate references",
        description = "Validate that all referenced files exist"
    )
    @ApiResponse(responseCode = "200", description = "Validation completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Validation service not available")
    public ResponseEntity<?> validateReferences(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath,
            @Parameter(description = "Base directory for resolving references")
            @RequestParam(required = false, defaultValue = ".") String baseDir) {
        
        logger.debug("Reference validation requested for: {}", filePath);
        
        if (validationService == null) {
            return ResponseEntity.status(500).body("Validation service not available");
        }
        
        try {
            ValidationResult result = validationService.validateReferences(filePath, baseDir);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error validating references: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error validating references: " + e.getMessage());
        }
    }

    /**
     * Validate consistency.
     */
    @PostMapping("/consistency")
    @Operation(
        summary = "Validate consistency",
        description = "Validate naming conventions and consistency rules"
    )
    @ApiResponse(responseCode = "200", description = "Validation completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Validation service not available")
    public ResponseEntity<?> validateConsistency(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath) {
        
        logger.debug("Consistency validation requested for: {}", filePath);
        
        if (validationService == null) {
            return ResponseEntity.status(500).body("Validation service not available");
        }
        
        try {
            ValidationResult result = validationService.validateConsistency(filePath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error validating consistency: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error validating consistency: " + e.getMessage());
        }
    }

    /**
     * Validate all aspects of a file.
     */
    @PostMapping("/all")
    @Operation(
        summary = "Validate all",
        description = "Perform all validation checks on a YAML file"
    )
    @ApiResponse(responseCode = "200", description = "Validation completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Validation service not available")
    public ResponseEntity<?> validateAll(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath,
            @Parameter(description = "Base directory for resolving references")
            @RequestParam(required = false, defaultValue = ".") String baseDir) {
        
        logger.debug("Full validation requested for: {}", filePath);
        
        if (validationService == null) {
            return ResponseEntity.status(500).body("Validation service not available");
        }
        
        try {
            ValidationResult structural = validationService.validateStructure(filePath);
            ValidationResult references = validationService.validateReferences(filePath, baseDir);
            ValidationResult consistency = validationService.validateConsistency(filePath);
            
            // Combine results
            ValidationResult combined = new ValidationResult(filePath);
            combined.setValidationType("ALL");
            combined.addIssues(structural.getIssues());
            combined.addIssues(references.getIssues());
            combined.addIssues(consistency.getIssues());
            combined.setValid(structural.isValid() && references.isValid() && consistency.isValid());
            
            return ResponseEntity.ok(combined);
        } catch (Exception e) {
            logger.error("Error performing full validation: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error performing full validation: " + e.getMessage());
        }
    }

    /**
     * Check if a file is valid.
     */
    @GetMapping("/is-valid")
    @Operation(
        summary = "Check if valid",
        description = "Check if a YAML file is valid"
    )
    @ApiResponse(responseCode = "200", description = "Validation status retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file path")
    @ApiResponse(responseCode = "500", description = "Validation service not available")
    public ResponseEntity<?> isValid(
            @Parameter(description = "Path to YAML file", required = true)
            @RequestParam String filePath,
            @Parameter(description = "Base directory for resolving references")
            @RequestParam(required = false, defaultValue = ".") String baseDir) {
        
        logger.debug("Validity check requested for: {}", filePath);
        
        if (validationService == null) {
            return ResponseEntity.status(500).body("Validation service not available");
        }
        
        try {
            ValidationResult structural = validationService.validateStructure(filePath);
            ValidationResult references = validationService.validateReferences(filePath, baseDir);
            ValidationResult consistency = validationService.validateConsistency(filePath);
            
            boolean isValid = structural.isValid() && references.isValid() && consistency.isValid();
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            logger.error("Error checking validity: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error checking validity: " + e.getMessage());
        }
    }
}

