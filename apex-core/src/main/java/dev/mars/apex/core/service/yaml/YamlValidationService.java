package dev.mars.apex.core.service.yaml;

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


import dev.mars.apex.core.util.YamlMetadataValidator;
import dev.mars.apex.core.util.YamlValidationResult;
import dev.mars.apex.core.util.YamlValidationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for comprehensive YAML validation across APEX configurations.
 * 
 * This service provides a high-level API for validating YAML files used in APEX,
 * including metadata validation, structure validation, and dependency checking.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-25
 * @version 1.0
 */
public class YamlValidationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlValidationService.class);
    
    private final YamlMetadataValidator validator;
    private final String baseResourcePath;
    
    /**
     * Create a validation service with default resource path.
     */
    public YamlValidationService() {
        this("src/main/resources");
    }
    
    /**
     * Create a validation service with custom resource path.
     * 
     * @param baseResourcePath The base path to search for YAML files
     */
    public YamlValidationService(String baseResourcePath) {
        this.baseResourcePath = baseResourcePath;
        this.validator = new YamlMetadataValidator(baseResourcePath);
    }
    
    /**
     * Validate all YAML files in the configured resource path.
     * 
     * @return Validation summary with results for all files
     */
    public YamlValidationSummary validateAllYamlFiles() {
        LOGGER.info("Starting comprehensive YAML validation in: {}", baseResourcePath);
        
        List<String> yamlFiles = discoverYamlFiles(baseResourcePath);
        LOGGER.info("Discovered {} YAML files for validation", yamlFiles.size());
        
        return validator.validateFiles(yamlFiles);
    }
    
    /**
     * Validate specific YAML files.
     * 
     * @param filePaths List of file paths to validate
     * @return Validation summary with results for specified files
     */
    public YamlValidationSummary validateFiles(List<String> filePaths) {
        LOGGER.info("Validating {} specified YAML files", filePaths.size());
        return validator.validateFiles(filePaths);
    }
    
    /**
     * Validate a single YAML file.
     * 
     * @param filePath Path to the YAML file
     * @return Validation result for the file
     */
    public YamlValidationResult validateFile(String filePath) {
        LOGGER.debug("Validating single YAML file: {}", filePath);
        return validator.validateFile(filePath);
    }
    
    /**
     * Check if all YAML files in the resource path are valid.
     * 
     * @return true if all files are valid, false otherwise
     */
    public boolean areAllFilesValid() {
        YamlValidationSummary summary = validateAllYamlFiles();
        return summary.isAllValid();
    }
    
    /**
     * Get validation errors for all YAML files.
     * 
     * @return List of validation errors across all files
     */
    public List<String> getAllValidationErrors() {
        YamlValidationSummary summary = validateAllYamlFiles();
        List<String> allErrors = new ArrayList<>();
        
        for (YamlValidationResult result : summary.getResults()) {
            if (!result.getErrors().isEmpty()) {
                allErrors.add("File: " + result.getFilePath());
                allErrors.addAll(result.getErrors());
            }
        }
        
        return allErrors;
    }
    
    /**
     * Get validation warnings for all YAML files.
     * 
     * @return List of validation warnings across all files
     */
    public List<String> getAllValidationWarnings() {
        YamlValidationSummary summary = validateAllYamlFiles();
        List<String> allWarnings = new ArrayList<>();
        
        for (YamlValidationResult result : summary.getResults()) {
            if (!result.getWarnings().isEmpty()) {
                allWarnings.add("File: " + result.getFilePath());
                allWarnings.addAll(result.getWarnings());
            }
        }
        
        return allWarnings;
    }
    
    /**
     * Validate YAML files and throw exception if any are invalid.
     * 
     * @throws YamlValidationException if any files are invalid
     */
    public void validateOrThrow() throws YamlValidationException {
        YamlValidationSummary summary = validateAllYamlFiles();
        
        if (!summary.isAllValid()) {
            List<String> errors = getAllValidationErrors();
            String errorMessage = String.format(
                "YAML validation failed: %d invalid files out of %d total files. Errors: %s",
                summary.getInvalidCount(),
                summary.getTotalCount(),
                String.join("; ", errors)
            );
            throw new YamlValidationException(errorMessage, summary);
        }
    }
    
    /**
     * Discover all YAML files in a directory recursively.
     * 
     * @param basePath The base directory to search
     * @return List of YAML file paths
     */
    public List<String> discoverYamlFiles(String basePath) {
        List<String> yamlFiles = new ArrayList<>();
        Path baseDir = Paths.get(basePath);
        
        if (!Files.exists(baseDir)) {
            LOGGER.warn("Base directory does not exist: {}", basePath);
            return yamlFiles;
        }
        
        try (Stream<Path> paths = Files.walk(baseDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String fileName = path.getFileName().toString().toLowerCase();
                     return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                 })
                 .forEach(path -> {
                     String relativePath = baseDir.relativize(path).toString().replace('\\', '/');
                     yamlFiles.add(relativePath);
                 });
        } catch (IOException e) {
            LOGGER.error("Error discovering YAML files in {}: {}", basePath, e.getMessage(), e);
        }
        
        LOGGER.debug("Discovered {} YAML files in {}", yamlFiles.size(), basePath);
        return yamlFiles;
    }
    
    /**
     * Get validation statistics for all YAML files.
     * 
     * @return Validation statistics
     */
    public ValidationStatistics getValidationStatistics() {
        YamlValidationSummary summary = validateAllYamlFiles();
        return new ValidationStatistics(
            summary.getTotalCount(),
            summary.getValidCount(),
            summary.getInvalidCount(),
            summary.getWarningCount(),
            summary.isAllValid()
        );
    }
    
    /**
     * Statistics about YAML validation results.
     */
    public static class ValidationStatistics {
        private final int totalFiles;
        private final int validFiles;
        private final int invalidFiles;
        private final int filesWithWarnings;
        private final boolean allValid;
        
        public ValidationStatistics(int totalFiles, int validFiles, int invalidFiles, 
                                  int filesWithWarnings, boolean allValid) {
            this.totalFiles = totalFiles;
            this.validFiles = validFiles;
            this.invalidFiles = invalidFiles;
            this.filesWithWarnings = filesWithWarnings;
            this.allValid = allValid;
        }
        
        public int getTotalFiles() { return totalFiles; }
        public int getValidFiles() { return validFiles; }
        public int getInvalidFiles() { return invalidFiles; }
        public int getFilesWithWarnings() { return filesWithWarnings; }
        public boolean isAllValid() { return allValid; }
        
        @Override
        public String toString() {
            return String.format("ValidationStatistics{total=%d, valid=%d, invalid=%d, warnings=%d, allValid=%s}",
                totalFiles, validFiles, invalidFiles, filesWithWarnings, allValid);
        }
    }
    
    /**
     * Exception thrown when YAML validation fails.
     */
    public static class YamlValidationException extends Exception {
        private final YamlValidationSummary validationSummary;
        
        public YamlValidationException(String message, YamlValidationSummary summary) {
            super(message);
            this.validationSummary = summary;
        }
        
        public YamlValidationSummary getValidationSummary() {
            return validationSummary;
        }
    }
}
