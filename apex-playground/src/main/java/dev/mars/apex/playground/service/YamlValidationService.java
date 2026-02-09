package dev.mars.apex.playground.service;

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


import dev.mars.apex.core.config.loader.YamlConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.exception.YamlConfigurationException;
import dev.mars.apex.core.config.validation.YamlMetadataValidator;
import dev.mars.apex.playground.model.YamlValidationResponse;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for YAML configuration validation.
 *
 * Provides real-time YAML syntax validation and structure checking
 * for APEX rules configurations in the playground interface.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Service
public class YamlValidationService {

    private static final Logger logger = LoggerFactory.getLogger(YamlValidationService.class);

    private final YamlConfigurationLoader configLoader;
    public YamlValidationService() {
        this.configLoader = new YamlConfigurationLoader();
        new YamlMetadataValidator();
    }

    /**
     * Validate YAML syntax and structure.
     *
     * Performs comprehensive validation including:
     * - YAML syntax validation
     * - APEX-specific structure validation
     * - Metadata validation
     * - Rules and enrichments validation
     *
     * @param yamlContent The YAML content to validate
     * @return Detailed validation result with errors, warnings, and metadata
     */
    public YamlValidationResponse validateYaml(String yamlContent) {
        logger.debug("Validating YAML content, length: {}", yamlContent != null ? yamlContent.length() : 0);

        YamlValidationResponse response = new YamlValidationResponse();

        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            response.setValid(false);
            response.setMessage("YAML content is empty");
            response.addError("YAML content cannot be empty", 1, 1);
            return response;
        }

        try {
            // Step 1: Basic YAML syntax validation
            validateYamlSyntax(yamlContent, response);

            // Step 2: APEX-specific structure validation (if syntax is valid)
            if (response.isValid()) {
                validateApexStructure(yamlContent, response);
            }

            // Step 3: Extract and validate metadata
            if (response.isValid()) {
                extractAndValidateMetadata(yamlContent, response);
            }

            // Step 4: Update statistics
            if (response.isValid()) {
                updateStatistics(yamlContent, response);
            }

            // Set final status
            if (response.isValid()) {
                response.setMessage("YAML configuration is valid");
                logger.debug("YAML validation successful");
            } else {
                if (!response.getErrors().isEmpty()) {
                    response.setMessage("Validation failed: " + response.getErrors().get(0).getMessage());
                } else {
                    response.setMessage("YAML configuration has validation errors");
                }
                logger.debug("YAML validation failed with {} errors and {} warnings",
                           response.getErrors().size(), response.getWarnings().size());
            }

        } catch (Exception e) {
            logger.error("Unexpected error during YAML validation: {}", e.getMessage());
            logger.debug("Full exception details:", e);
            response.setValid(false);
            response.setMessage("Validation failed: " + e.getMessage());
            response.addError("Unexpected validation error: " + e.getMessage(), 0, 0);
        }

        return response;
    }

    /**
     * Validate YAML content in real-time (for editor integration).
     *
     * Performs lightweight validation suitable for real-time feedback.
     *
     * @param yamlContent The YAML content to validate
     * @return Quick validation result for real-time feedback
     */
    public boolean isValidYaml(String yamlContent) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            return false;
        }

        try {
            // Quick syntax check using YAML parser directly (not full APEX validation)
            parseYamlAsMap(yamlContent);
            return true;
        } catch (Exception e) {
            logger.debug("Real-time YAML validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get YAML validation errors with line numbers and descriptions.
     *
     * @param yamlContent The YAML content to validate
     * @return Detailed validation response with error locations
     */
    public YamlValidationResponse getValidationErrors(String yamlContent) {
        return validateYaml(yamlContent);
    }

    /**
     * Validate basic YAML syntax.
     */
    private void validateYamlSyntax(String yamlContent, YamlValidationResponse response) {
        try {
            // Try to parse as YAML using the configuration loader
            configLoader.fromYamlString(yamlContent);
            logger.debug("YAML syntax validation passed");
        } catch (YamlConfigurationException e) {
            logger.debug("YAML syntax validation failed: {}", e.getMessage());
            response.setValid(false);

            // Try to extract line information from the error message
            String errorMessage = e.getMessage();
            
            // Provide user-friendly messages for common errors
            if (errorMessage.contains("Cannot deserialize") && errorMessage.contains("LinkedHashMap") && errorMessage.contains("Array value")) {
                String friendlyMessage = extractFieldNameFromDeserializationError(errorMessage);
                errorMessage = friendlyMessage;
            }
            
            int line = extractLineNumber(errorMessage);
            response.addError("YAML syntax error: " + errorMessage, line, 0);
        }
    }
    
    /**
     * Extract field name from deserialization error message and provide helpful examples.
     */
    private String extractFieldNameFromDeserializationError(String errorMessage) {
        // Provide specific examples based on the field
        if (errorMessage.contains("queries")) {
            return "Field 'queries' has incorrect format.\n\n" +
                   "[OK] MAP FORMAT (simple queries):\n" +
                   "queries:\n" +
                   "  customerProfile: \"SELECT * FROM customers WHERE id = :id\"\n" +
                   "  getAllActive: \"SELECT * FROM customers WHERE status = 'ACTIVE'\"\n\n" +
                   "[OK] ARRAY FORMAT (queries with metadata):\n" +
                   "queries:\n" +
                   "  - name: \"customerProfile\"\n" +
                   "    query: \"SELECT * FROM customers WHERE id = :id\"\n" +
                   "    description: \"Get customer by ID\"\n" +
                   "    parameters: [\"id\"]\n\n" +
                   "✗ INCORRECT (plain array of strings):\n" +
                   "queries:\n" +
                   "  - SELECT * FROM customers";
        }
        if (errorMessage.contains("operations")) {
            return "Field 'operations' has incorrect format.\n\n" +
                   "[OK] MAP FORMAT (simple operations):\n" +
                   "operations:\n" +
                   "  getAllCustomers: \"SELECT * FROM csv\"\n" +
                   "  getActiveCustomers: \"SELECT * FROM csv WHERE status = 'ACTIVE'\"\n\n" +
                   "[OK] ARRAY FORMAT (operations with metadata):\n" +
                   "operations:\n" +
                   "  - name: \"getAllCustomers\"\n" +
                   "    query: \"SELECT * FROM csv\"\n" +
                   "    description: \"Retrieve all customers\"\n\n" +
                   "✗ INCORRECT (plain array of strings):\n" +
                   "operations:\n" +
                   "  - SELECT * FROM csv";
        }
        if (errorMessage.contains("endpoints")) {
            return "Field 'endpoints' has incorrect format.\n\n" +
                   "[OK] MAP FORMAT (simple endpoints):\n" +
                   "endpoints:\n" +
                   "  currency-lookup: \"/api/currency/{key}\"\n" +
                   "  country-lookup: \"/api/country/{code}\"\n\n" +
                   "[OK] ARRAY FORMAT (endpoints with metadata):\n" +
                   "endpoints:\n" +
                   "  - name: \"currency-lookup\"\n" +
                   "    endpoint: \"/api/currency/{key}\"\n" +
                   "    description: \"Lookup currency by code\"\n" +
                   "    method: \"GET\"\n\n" +
                   "✗ INCORRECT (plain array of strings):\n" +
                   "endpoints:\n" +
                   "  - /api/currency/{key}";
        }
        if (errorMessage.contains("topics")) {
            return "Field 'topics' has incorrect format.\n\n" +
                   "[OK] MAP FORMAT (simple topics):\n" +
                   "topics:\n" +
                   "  customerEvents: \"customer-events\"\n" +
                   "  orderEvents: \"order-events\"\n\n" +
                   "[OK] ARRAY FORMAT (topics with metadata):\n" +
                   "topics:\n" +
                   "  - name: \"customerEvents\"\n" +
                   "    topic: \"customer-events\"\n" +
                   "    description: \"Customer lifecycle events\"\n\n" +
                   "✗ INCORRECT (plain array of strings):\n" +
                   "topics:\n" +
                   "  - customer-events";
        }
        if (errorMessage.contains("key-patterns") || errorMessage.contains("keyPatterns")) {
            return "Field 'key-patterns' has incorrect format.\n\n" +
                   "[OK] MAP FORMAT (simple patterns):\n" +
                   "key-patterns:\n" +
                   "  customerPattern: \"customer:*\"\n" +
                   "  orderPattern: \"order:*\"\n\n" +
                   "[OK] ARRAY FORMAT (patterns with metadata):\n" +
                   "key-patterns:\n" +
                   "  - name: \"customerPattern\"\n" +
                   "    pattern: \"customer:*\"\n" +
                   "    description: \"Match all customer keys\"\n\n" +
                   "✗ INCORRECT (plain array of strings):\n" +
                   "key-patterns:\n" +
                   "  - customer:*";
        }
        return "YAML structure error: Expected either a map (key-value pairs) or an array of objects with 'name' field. " +
               "Plain arrays of strings are not supported for queries, operations, endpoints, topics, and key-patterns.";
    }

    /**
     * Validate APEX-specific YAML structure.
     */
    private void validateApexStructure(String yamlContent, YamlValidationResponse response) {
        try {
            // Parse the YAML configuration
            YamlRuleConfiguration config = configLoader.fromYamlString(yamlContent);

            // Count rules and enrichments for statistics
            if (config.getRules() != null) {
                response.getStatistics().setRulesCount(config.getRules().size());
            }
            if (config.getEnrichments() != null) {
                response.getStatistics().setEnrichmentsCount(config.getEnrichments().size());
            }

            // Validate that we have either rules or enrichments
            boolean hasRules = config.getRules() != null && !config.getRules().isEmpty();
            boolean hasEnrichments = config.getEnrichments() != null && !config.getEnrichments().isEmpty();

            if (!hasRules && !hasEnrichments) {
                response.addWarning("Configuration contains no rules or enrichments", 0, 0);
            }

            logger.debug("APEX structure validation passed");

        } catch (YamlConfigurationException e) {
            logger.debug("APEX structure validation failed: {}", e.getMessage());
            response.setValid(false);
            response.addError("APEX structure error: " + e.getMessage(), 0, 0);
        }
    }

    /**
     * Extract and validate metadata from YAML content.
     */
    private void extractAndValidateMetadata(String yamlContent, YamlValidationResponse response) {
        try {
            // Parse YAML as Map to extract metadata
            Map<String, Object> yamlMap = parseYamlAsMap(yamlContent);

            // Use Core Validator for validation
            try {
                YamlMetadataValidator.validateMetadataAndThrow(yamlMap, "playground-editor");
            } catch (YamlConfigurationException e) {
                response.addError(e.getMessage(), 0, 0);
            }

            // Extract metadata if present (for UI display)
            if (yamlMap.containsKey("metadata")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = (Map<String, Object>) yamlMap.get("metadata");

                YamlValidationResponse.YamlMetadata metadata = new YamlValidationResponse.YamlMetadata();
                metadata.setId((String) metadataMap.get("id"));
                metadata.setName((String) metadataMap.get("name"));
                metadata.setVersion((String) metadataMap.get("version"));
                metadata.setDescription((String) metadataMap.get("description"));
                metadata.setType((String) metadataMap.get("type"));
                metadata.setAuthor((String) metadataMap.get("author"));

                response.setMetadata(metadata);
            }

            logger.debug("Metadata validation completed");

        } catch (Exception e) {
            logger.debug("Metadata validation failed: {}", e.getMessage());
            response.addWarning("Could not validate metadata: " + e.getMessage(), 0, 0);
        }
    }

    /**
     * Parse YAML content as a Map without full validation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYamlAsMap(String yamlContent) throws Exception {
        YAMLMapper yamlMapper = new YAMLMapper();
        return yamlMapper.readValue(yamlContent, Map.class);
    }

    /**
     * Update statistics based on YAML content.
     */
    @SuppressWarnings("unchecked")
    private void updateStatistics(String yamlContent, YamlValidationResponse response) {
        try {
            Map<String, Object> yamlMap = parseYamlAsMap(yamlContent);

            // Count rules
            Object rulesObj = yamlMap.get("rules");
            if (rulesObj instanceof List) {
                List<Object> rules = (List<Object>) rulesObj;
                response.getStatistics().setRulesCount(rules.size());
            }

            // Count enrichments
            Object enrichmentsObj = yamlMap.get("enrichments");
            if (enrichmentsObj instanceof List) {
                List<Object> enrichments = (List<Object>) enrichmentsObj;
                response.getStatistics().setEnrichmentsCount(enrichments.size());
            }

            // Update error and warning counts
            response.getStatistics().setErrorCount(response.getErrors().size());
            response.getStatistics().setWarningCount(response.getWarnings().size());

        } catch (Exception e) {
            logger.debug("Failed to update statistics: {}", e.getMessage());
        }
    }

    /**
     * Extract line number from error message.
     */
    private int extractLineNumber(String errorMessage) {
        if (errorMessage == null) {
            return 0;
        }

        // Try to find line number patterns in the error message
        // Common patterns: "line 5", "at line 5", "line: 5"
        String[] patterns = {"line\\s+(\\d+)", "at\\s+line\\s+(\\d+)", "line:\\s*(\\d+)"};

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(errorMessage);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException e) {
                    // Continue to next pattern
                }
            }
        }

        return 0; // Default to 0 if no line number found
    }
}
