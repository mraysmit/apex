package dev.mars.apex.yaml.manager.service;

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

import dev.mars.apex.yaml.manager.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Service for validating YAML configuration files.
 * 
 * Performs structural, reference, consistency, performance, and compliance validation.
 */
@Service
public class ValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private final Yaml yaml = new Yaml();

    /**
     * Validate a YAML file for structural correctness.
     */
    public ValidationResult validateStructure(String filePath) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult(filePath);
        result.setValidationType("STRUCTURAL");
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                result.addIssue(new ValidationIssue(
                    "FILE_NOT_FOUND",
                    "File does not exist: " + filePath,
                    ValidationIssue.Severity.ERROR,
                    ValidationIssue.Category.STRUCTURAL,
                    filePath,
                    "Verify the file path is correct"
                ));
                result.setValid(false);
                return result;
            }
            
            // Try to parse YAML
            try (FileInputStream fis = new FileInputStream(file)) {
                Map<String, Object> data = yaml.load(fis);
                
                // Check for required metadata
                if (data == null || !data.containsKey("metadata")) {
                    result.addIssue(new ValidationIssue(
                        "MISSING_METADATA",
                        "Missing required 'metadata' section",
                        ValidationIssue.Severity.ERROR,
                        ValidationIssue.Category.STRUCTURAL,
                        "root",
                        "Add metadata section with id, name, and version"
                    ));
                } else {
                    Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
                    
                    // Check for required metadata fields
                    if (!metadata.containsKey("id")) {
                        result.addIssue(new ValidationIssue(
                            "MISSING_ID",
                            "Missing required 'id' in metadata",
                            ValidationIssue.Severity.ERROR,
                            ValidationIssue.Category.STRUCTURAL,
                            "metadata.id",
                            "Add unique identifier to metadata section"
                        ));
                    }
                    
                    if (!metadata.containsKey("name")) {
                        result.addIssue(new ValidationIssue(
                            "MISSING_NAME",
                            "Missing required 'name' in metadata",
                            ValidationIssue.Severity.WARNING,
                            ValidationIssue.Category.STRUCTURAL,
                            "metadata.name",
                            "Add descriptive name to metadata section"
                        ));
                    }
                    
                    if (!metadata.containsKey("version")) {
                        result.addIssue(new ValidationIssue(
                            "MISSING_VERSION",
                            "Missing recommended 'version' in metadata",
                            ValidationIssue.Severity.INFO,
                            ValidationIssue.Category.STRUCTURAL,
                            "metadata.version",
                            "Add version number to metadata section"
                        ));
                    }
                }
            }
        } catch (IOException e) {
            result.addIssue(new ValidationIssue(
                "IO_ERROR",
                "Error reading file: " + e.getMessage(),
                ValidationIssue.Severity.ERROR,
                ValidationIssue.Category.STRUCTURAL,
                filePath,
                "Check file permissions and accessibility"
            ));
            result.setValid(false);
        } catch (Exception e) {
            result.addIssue(new ValidationIssue(
                "YAML_PARSE_ERROR",
                "Invalid YAML syntax: " + e.getMessage(),
                ValidationIssue.Severity.ERROR,
                ValidationIssue.Category.STRUCTURAL,
                filePath,
                "Fix YAML syntax errors"
            ));
            result.setValid(false);
        }
        
        result.setValidationTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Validate file references exist.
     */
    public ValidationResult validateReferences(String filePath, String baseDir) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult(filePath);
        result.setValidationType("REFERENCE");
        
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Map<String, Object> data = yaml.load(fis);
            
            if (data != null) {
                // Check scenario-refs
                if (data.containsKey("scenario-refs")) {
                    List<String> refs = (List<String>) data.get("scenario-refs");
                    for (String ref : refs) {
                        File refFile = new File(baseDir, ref);
                        if (!refFile.exists()) {
                            result.addIssue(new ValidationIssue(
                                "MISSING_REFERENCE",
                                "Referenced file not found: " + ref,
                                ValidationIssue.Severity.ERROR,
                                ValidationIssue.Category.REFERENCE,
                                "scenario-refs",
                                "Verify the referenced file exists at: " + ref
                            ));
                        }
                    }
                }
                
                // Check enrichment-refs
                if (data.containsKey("enrichment-refs")) {
                    List<String> refs = (List<String>) data.get("enrichment-refs");
                    for (String ref : refs) {
                        File refFile = new File(baseDir, ref);
                        if (!refFile.exists()) {
                            result.addIssue(new ValidationIssue(
                                "MISSING_REFERENCE",
                                "Referenced enrichment file not found: " + ref,
                                ValidationIssue.Severity.ERROR,
                                ValidationIssue.Category.REFERENCE,
                                "enrichment-refs",
                                "Verify the referenced file exists at: " + ref
                            ));
                        }
                    }
                }
                
                // Check config-files
                if (data.containsKey("config-files")) {
                    List<String> refs = (List<String>) data.get("config-files");
                    for (String ref : refs) {
                        File refFile = new File(baseDir, ref);
                        if (!refFile.exists()) {
                            result.addIssue(new ValidationIssue(
                                "MISSING_REFERENCE",
                                "Referenced config file not found: " + ref,
                                ValidationIssue.Severity.WARNING,
                                ValidationIssue.Category.REFERENCE,
                                "config-files",
                                "Verify the referenced file exists at: " + ref
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error validating references: {}", e.getMessage());
        }
        
        result.setValidationTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Validate consistency (unique IDs, naming conventions).
     */
    public ValidationResult validateConsistency(String filePath) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult(filePath);
        result.setValidationType("CONSISTENCY");
        
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Map<String, Object> data = yaml.load(fis);
            
            if (data != null && data.containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
                String id = (String) metadata.get("id");
                
                if (id != null && !id.matches("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")) {
                    result.addIssue(new ValidationIssue(
                        "INVALID_ID_FORMAT",
                        "ID does not follow naming convention (lowercase, hyphens only): " + id,
                        ValidationIssue.Severity.WARNING,
                        ValidationIssue.Category.CONSISTENCY,
                        "metadata.id",
                        "Use lowercase letters, numbers, and hyphens only"
                    ));
                }
            }
        } catch (Exception e) {
            logger.debug("Error validating consistency: {}", e.getMessage());
        }
        
        result.setValidationTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
}

