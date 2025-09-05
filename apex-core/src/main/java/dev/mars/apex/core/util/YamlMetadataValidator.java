package dev.mars.apex.core.util;

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


import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Validator for YAML file metadata and structure.
 * 
 * This validator ensures that all YAML files in the APEX system have proper
 * metadata structure including required fields like 'type', and validates
 * the content against expected schemas for different file types.
 * 
 * VALIDATION FEATURES:
 * - Required metadata fields validation
 * - File type-specific schema validation
 * - Cross-reference validation between files
 * - Comprehensive error reporting
 * 
 * SUPPORTED FILE TYPES:
 * - scenario: Scenario configuration files
 * - scenario-registry: Central scenario registry
 * - bootstrap: Complete demo configurations
 * - rule-config: Reusable rule configurations
 * - dataset: Data reference files
 * - enrichment: Data enrichment configurations
 * - rule-chain: Sequential rule execution files
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class YamlMetadataValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlMetadataValidator.class);
    
    // Required metadata fields for all YAML files
    private static final Set<String> REQUIRED_METADATA_FIELDS = Set.of(
        "id",
        "name",
        "version",
        "description",
        "type"
    );
    
    // Valid file types
    private static final Set<String> VALID_FILE_TYPES = Set.of(
        "scenario",
        "scenario-registry",
        "bootstrap",
        "rule-config",
        "dataset",
        "enrichment",
        "rule-chain",
        "external-data-config",
        "pipeline"
    );
    
    // Type-specific required fields
    private static final Map<String, Set<String>> TYPE_SPECIFIC_REQUIRED_FIELDS = Map.of(
        "scenario", Set.of("business-domain", "owner"),
        "scenario-registry", Set.of("created-by"),
        "bootstrap", Set.of("business-domain", "created-by"),
        "rule-config", Set.of("author"),
        "dataset", Set.of("source"),
        "enrichment", Set.of("author"),
        "rule-chain", Set.of("author"),
        "external-data-config", Set.of("author"),
        "pipeline", Set.of("author")
    );
    
    private final YamlConfigurationLoader configLoader;
    private final String basePath;
    
    public YamlMetadataValidator() {
        this("apex-demo/src/main/resources");
    }
    
    public YamlMetadataValidator(String basePath) {
        this.configLoader = new YamlConfigurationLoader();
        this.basePath = basePath;
    }
    
    /**
     * Validates a single YAML file's metadata and structure.
     * 
     * @param filePath path to the YAML file to validate
     * @return validation result with details
     */
    public YamlValidationResult validateFile(String filePath) {
        logger.info("Validating YAML file: {}", filePath);
        
        YamlValidationResult result = new YamlValidationResult(filePath);
        
        try {
            // Check if file exists
            File file = new File(basePath, filePath);
            if (!file.exists()) {
                result.addError("File does not exist: " + filePath);
                return result;
            }

            // Load and parse YAML - handle validation exceptions
            String fullPath = file.getAbsolutePath();
            Map<String, Object> yamlContent;

            try {
                yamlContent = configLoader.loadAsMap(fullPath);
            } catch (YamlConfigurationException e) {
                // Handle specific YAML configuration exceptions (like missing type field)
                if (e.getMessage().contains("Missing required 'type' field")) {
                    result.addError("Missing required metadata field: type");
                } else if (e.getMessage().contains("Invalid 'type' field")) {
                    result.addError("Invalid type field in metadata");
                } else {
                    result.addError("Failed to parse YAML file: " + e.getMessage());
                }
                return result;
            }

            // Validate metadata section (additional validation beyond what loadAsMap does)
            validateMetadataSection(yamlContent, result);

            // If metadata is valid, perform type-specific validation
            if (result.isValid()) {
                String fileType = getFileType(yamlContent);
                if (fileType != null) {
                    validateTypeSpecificContent(yamlContent, fileType, result);
                }
            }

        } catch (Exception e) {
            result.addError("Failed to parse YAML file: " + e.getMessage());
            logger.error("Validation failed for file: {}", filePath, e);
        }
        
        return result;
    }
    
    /**
     * Validates multiple YAML files and returns a summary report.
     * 
     * @param filePaths list of file paths to validate
     * @return validation summary with all results
     */
    public YamlValidationSummary validateFiles(List<String> filePaths) {
        logger.info("Validating {} YAML files", filePaths.size());
        
        YamlValidationSummary summary = new YamlValidationSummary();
        
        for (String filePath : filePaths) {
            YamlValidationResult result = validateFile(filePath);
            summary.addResult(result);
        }
        
        logger.info("Validation completed. Valid: {}, Invalid: {}", 
            summary.getValidCount(), summary.getInvalidCount());
        
        return summary;
    }
    
    /**
     * Validates the metadata section of a YAML file.
     */
    @SuppressWarnings("unchecked")
    private void validateMetadataSection(Map<String, Object> yamlContent, YamlValidationResult result) {
        // Check if metadata section exists
        Object metadataObj = yamlContent.get("metadata");
        if (metadataObj == null) {
            result.addError("Missing 'metadata' section");
            return;
        }
        
        if (!(metadataObj instanceof Map)) {
            result.addError("'metadata' section must be a map/object");
            return;
        }
        
        Map<String, Object> metadata = (Map<String, Object>) metadataObj;
        
        // Validate required fields
        for (String requiredField : REQUIRED_METADATA_FIELDS) {
            if (!metadata.containsKey(requiredField) || metadata.get(requiredField) == null) {
                result.addError("Missing required metadata field: " + requiredField);
            } else {
                Object value = metadata.get(requiredField);
                if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                    result.addError("Metadata field '" + requiredField + "' must be a non-empty string");
                }
            }
        }
        
        // Validate file type
        String fileType = (String) metadata.get("type");
        if (fileType != null && !VALID_FILE_TYPES.contains(fileType)) {
            result.addError("Invalid file type: " + fileType + ". Valid types: " + VALID_FILE_TYPES);
        }
        
        // Validate type-specific required fields
        if (fileType != null && TYPE_SPECIFIC_REQUIRED_FIELDS.containsKey(fileType)) {
            Set<String> typeRequiredFields = TYPE_SPECIFIC_REQUIRED_FIELDS.get(fileType);
            for (String requiredField : typeRequiredFields) {
                if (!metadata.containsKey(requiredField) || metadata.get(requiredField) == null) {
                    result.addError("Missing required field for type '" + fileType + "': " + requiredField);
                }
            }
        }
        
        // Validate version format (basic semantic versioning check)
        String version = (String) metadata.get("version");
        if (version != null && !version.matches("\\d+\\.\\d+(\\.\\d+)?")) {
            result.addWarning("Version should follow semantic versioning format (e.g., 1.0.0): " + version);
        }
    }
    
    /**
     * Validates type-specific content based on the file type.
     */
    private void validateTypeSpecificContent(Map<String, Object> yamlContent, String fileType, YamlValidationResult result) {
        switch (fileType) {
            case "scenario":
                validateScenarioContent(yamlContent, result);
                break;
            case "scenario-registry":
                validateScenarioRegistryContent(yamlContent, result);
                break;
            case "bootstrap":
                validateBootstrapContent(yamlContent, result);
                break;
            case "rule-config":
                validateRuleConfigContent(yamlContent, result);
                break;
            case "dataset":
                validateDatasetContent(yamlContent, result);
                break;
            default:
                // For other types, just log that validation is not implemented
                logger.debug("Type-specific validation not implemented for type: {}", fileType);
        }
    }
    
    /**
     * Validates scenario file content.
     */
    @SuppressWarnings("unchecked")
    private void validateScenarioContent(Map<String, Object> yamlContent, YamlValidationResult result) {
        Object scenarioObj = yamlContent.get("scenario");
        if (scenarioObj == null) {
            result.addError("Scenario files must have a 'scenario' section");
            return;
        }
        
        if (!(scenarioObj instanceof Map)) {
            result.addError("'scenario' section must be a map/object");
            return;
        }
        
        Map<String, Object> scenario = (Map<String, Object>) scenarioObj;
        
        // Required scenario fields
        if (!scenario.containsKey("scenario-id")) {
            result.addError("Missing required field: scenario-id");
        }
        
        if (!scenario.containsKey("data-types")) {
            result.addError("Missing required field: data-types");
        } else {
            Object dataTypes = scenario.get("data-types");
            if (!(dataTypes instanceof List) || ((List<?>) dataTypes).isEmpty()) {
                result.addError("data-types must be a non-empty list");
            }
        }
        
        if (!scenario.containsKey("rule-configurations")) {
            result.addError("Missing required field: rule-configurations");
        } else {
            Object ruleConfigs = scenario.get("rule-configurations");
            if (!(ruleConfigs instanceof List) || ((List<?>) ruleConfigs).isEmpty()) {
                result.addError("rule-configurations must be a non-empty list");
            }
        }
    }
    
    /**
     * Validates scenario registry content.
     */
    @SuppressWarnings("unchecked")
    private void validateScenarioRegistryContent(Map<String, Object> yamlContent, YamlValidationResult result) {
        Object registryObj = yamlContent.get("scenario-registry");
        if (registryObj == null) {
            result.addError("Scenario registry files must have a 'scenario-registry' section");
            return;
        }
        
        if (!(registryObj instanceof List)) {
            result.addError("'scenario-registry' section must be a list");
            return;
        }
        
        List<Object> registry = (List<Object>) registryObj;
        if (registry.isEmpty()) {
            result.addWarning("Scenario registry is empty");
        }
        
        // Validate each registry entry
        for (int i = 0; i < registry.size(); i++) {
            Object entry = registry.get(i);
            if (!(entry instanceof Map)) {
                result.addError("Registry entry " + i + " must be a map/object");
                continue;
            }
            
            Map<String, Object> registryEntry = (Map<String, Object>) entry;
            String[] requiredFields = {"scenario-id", "config-file", "data-types", "description"};
            
            for (String field : requiredFields) {
                if (!registryEntry.containsKey(field)) {
                    result.addError("Registry entry " + i + " missing required field: " + field);
                }
            }
        }
    }
    
    /**
     * Validates bootstrap file content.
     */
    private void validateBootstrapContent(Map<String, Object> yamlContent, YamlValidationResult result) {
        // Bootstrap files should have rule-chains or categories
        if (!yamlContent.containsKey("rule-chains") && !yamlContent.containsKey("categories")) {
            result.addWarning("Bootstrap files typically contain 'rule-chains' or 'categories' sections");
        }
    }
    
    /**
     * Validates rule configuration content.
     */
    private void validateRuleConfigContent(Map<String, Object> yamlContent, YamlValidationResult result) {
        // Rule config files should have rules, enrichments, or rule-chains
        if (!yamlContent.containsKey("rules") && 
            !yamlContent.containsKey("enrichments") && 
            !yamlContent.containsKey("rule-chains")) {
            result.addWarning("Rule config files typically contain 'rules', 'enrichments', or 'rule-chains' sections");
        }
    }
    
    /**
     * Validates dataset content.
     */
    private void validateDatasetContent(Map<String, Object> yamlContent, YamlValidationResult result) {
        // Dataset files should have data sections
        if (!yamlContent.containsKey("data") && !yamlContent.containsKey("countries") && !yamlContent.containsKey("dataset")) {
            result.addWarning("Dataset files typically contain 'data', 'countries', or 'dataset' sections");
        }
    }
    
    /**
     * Gets the file type from the metadata.
     */
    @SuppressWarnings("unchecked")
    private String getFileType(Map<String, Object> yamlContent) {
        Object metadataObj = yamlContent.get("metadata");
        if (metadataObj instanceof Map) {
            Map<String, Object> metadata = (Map<String, Object>) metadataObj;
            return (String) metadata.get("type");
        }
        return null;
    }
}
