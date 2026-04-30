package dev.mars.apex.core.config.validation;

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

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlEnrichmentGroup;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.util.EnabledFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Validates enrichment configurations including basic structure, type-specific requirements,
 * lookup configurations, SpEL expressions, field mappings, conditional mappings, enrichment groups,
 * and complex lookup key patterns.
 *
 * <p>Extracted from {@code ConfigurationLoader} as part of the validation layer refactoring.
 * This is the largest validator, covering ~35 validation methods.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 */
public class EnrichmentValidator {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentValidator.class);

    /**
     * Validate all enrichments in the configuration.
     *
     * @param config the YAML rule configuration to validate
     * @throws ConfigurationException if any enrichment is invalid
     */
    public void validate(YamlRuleConfiguration config) throws ConfigurationException {
        validateEnrichments(config);
        validateEnrichmentGroups(config);
    }

    /**
     * Validate enrichments in the configuration.
     * Validates all enrichment attributes according to patterns documented in lookups.md.
     */
    private void validateEnrichments(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getEnrichments() == null || config.getEnrichments().isEmpty()) {
            logger.debug("No enrichments to validate");
            return;
        }

        logger.debug("Validating " + config.getEnrichments().size() + " enrichments");

        for (YamlEnrichment enrichment : config.getEnrichments()) {
            validateEnrichment(enrichment);
        }

        logger.debug("Enrichment validation completed successfully");
    }

    /**
     * Validate a single enrichment configuration.
     */
    private void validateEnrichment(YamlEnrichment enrichment) throws ConfigurationException {
        // Validate basic enrichment structure
        validateEnrichmentBasicStructure(enrichment);

        // Validate enrichment type and type-specific requirements
        validateEnrichmentType(enrichment);

        // Validate condition expression if present
        validateEnrichmentCondition(enrichment);

        // Validate severity if present
        validateEnrichmentSeverity(enrichment);

        // Validate lookup configuration for lookup enrichments
        if ("lookup-enrichment".equals(enrichment.getType())) {
            validateLookupConfiguration(enrichment);
        }

        // Validate field mappings
        validateFieldMappings(enrichment.getFieldMappings(), enrichment.getId());

        logger.debug("Validated enrichment: " + enrichment.getId());
    }

    /**
     * Validate basic enrichment structure (required fields).
     */
    private void validateEnrichmentBasicStructure(YamlEnrichment enrichment) throws ConfigurationException {
        if (enrichment.getId() == null || enrichment.getId().trim().isEmpty()) {
            throw new ConfigurationException("Enrichment ID is required");
        }

        String enrichmentId = enrichment.getId();

        if (enrichment.getType() == null || enrichment.getType().trim().isEmpty()) {
            throw new ConfigurationException("Enrichment type is required for enrichment: " + enrichmentId);
        }

        // Validate ID format (alphanumeric, hyphens, underscores only)
        if (!enrichmentId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ConfigurationException("Enrichment ID '" + enrichmentId + "' contains invalid characters. Use only letters, numbers, hyphens, and underscores");
        }
    }

    /**
     * Validate enrichment type and type-specific requirements.
     * Special handling: calculation-enrichment field-mappings validation is graceful,
     * all other validations throw exceptions for strict configuration validation.
     */
    private void validateEnrichmentType(YamlEnrichment enrichment) throws ConfigurationException {
        String type = enrichment.getType();
        String enrichmentId = enrichment.getId();

        Set<String> validTypes = Set.of("lookup-enrichment", "field-enrichment", "calculation-enrichment", "conditional-mapping-enrichment");
        if (!validTypes.contains(type)) {
            throw new ConfigurationException("Invalid enrichment type '" + type + "' for enrichment: " + enrichmentId + ". Valid types: " + validTypes);
        }

        // Type-specific validation
        switch (type) {
            case "lookup-enrichment":
                if (enrichment.getLookupConfig() == null) {
                    throw new ConfigurationException("lookup-enrichment type requires 'lookup-config' for enrichment: " + enrichmentId);
                }
                break;
            case "field-enrichment":
                // field-enrichment requires either field-mappings OR conditional-mappings (or both)
                boolean hasFieldMappings = enrichment.getFieldMappings() != null && !enrichment.getFieldMappings().isEmpty();
                boolean hasConditionalMappings = enrichment.getConditionalMappings() != null && !enrichment.getConditionalMappings().isEmpty();

                if (!hasFieldMappings && !hasConditionalMappings) {
                    throw new ConfigurationException("field-enrichment type requires either 'field-mappings' or 'conditional-mappings' for enrichment: " + enrichmentId);
                }

                // Validate conditional mappings if present
                if (hasConditionalMappings) {
                    validateConditionalMappings(enrichment.getConditionalMappings(), enrichmentId);
                }
                break;
            case "calculation-enrichment":
                // SPECIAL CASE: Graceful handling for calculation-enrichment field-mappings
                // This allows processing to continue with warnings instead of failing configuration loading
                if (enrichment.getFieldMappings() == null || enrichment.getFieldMappings().isEmpty()) {
                    String errorMsg = "calculation-enrichment type requires 'field-mappings' for enrichment: " + enrichmentId;
                    logger.warn("Configuration validation warning: " + errorMsg);
                    // Don't throw exception - this is handled gracefully during processing
                }
                break;
            case "conditional-mapping-enrichment":
                // conditional-mapping-enrichment requires target-field and mapping-rules
                if (enrichment.getTargetField() == null || enrichment.getTargetField().trim().isEmpty()) {
                    throw new ConfigurationException("conditional-mapping-enrichment type requires 'target-field' for enrichment: " + enrichmentId);
                }
                if (enrichment.getMappingRules() == null || enrichment.getMappingRules().isEmpty()) {
                    throw new ConfigurationException("conditional-mapping-enrichment type requires 'mapping-rules' for enrichment: " + enrichmentId);
                }
                // Validate mapping rules
                if (enrichment.getMappingRules() != null && !enrichment.getMappingRules().isEmpty()) {
                    validateMappingRules(enrichment.getMappingRules(), enrichmentId);
                }
                break;
        }
    }

    /**
     * Validate enrichment condition expression.
     */
    private void validateEnrichmentCondition(YamlEnrichment enrichment) throws ConfigurationException {
        String condition = enrichment.getCondition();
        String enrichmentId = enrichment.getId();

        if (condition != null && !condition.trim().isEmpty()) {
            // Validate SpEL syntax
            if (!isValidSpELExpression(condition)) {
                throw new ConfigurationException("Invalid SpEL expression in condition '" + condition + "' for enrichment: " + enrichmentId);
            }

            // Validate common condition patterns
            validateConditionPatterns(condition, enrichmentId);
        }
    }

    /**
     * Validate enrichment severity.
     */
    private void validateEnrichmentSeverity(YamlEnrichment enrichment) throws ConfigurationException {
        if (enrichment.getSeverity() != null) {
            String severity = enrichment.getSeverity().trim().toUpperCase();
            if (!SeverityConstants.VALID_SEVERITIES.contains(severity)) {
                throw new ConfigurationException("Enrichment '" + enrichment.getId() + "' has invalid severity '" +
                    enrichment.getSeverity() + "'. Must be one of: " + String.join(", ", SeverityConstants.VALID_SEVERITIES));
            }
        }
    }

    /**
     * Validate lookup configuration for lookup enrichments.
     */
    private void validateLookupConfiguration(YamlEnrichment enrichment) throws ConfigurationException {
        YamlEnrichment.LookupConfig lookupConfig = enrichment.getLookupConfig();
        String enrichmentId = enrichment.getId();

        if (lookupConfig == null) {
            throw new ConfigurationException("lookup-config is required for lookup enrichment: " + enrichmentId);
        }

        // Validate lookup configuration structure
        validateLookupConfigStructure(lookupConfig, enrichmentId);

        // Validate lookup key expression
        validateLookupKeyExpression(lookupConfig.getLookupKey(), enrichmentId);

        // Validate dataset configuration if present
        if (lookupConfig.getLookupDataset() != null) {
            validateLookupDataset(lookupConfig.getLookupDataset(), enrichmentId);
        }

        // Validate caching configuration
        validateCachingConfiguration(lookupConfig, enrichmentId);
    }

    /**
     * Validate lookup configuration structure.
     */
    private void validateLookupConfigStructure(YamlEnrichment.LookupConfig lookupConfig, String enrichmentId) throws ConfigurationException {
        // Must have either lookup-service OR lookup-dataset
        boolean hasService = lookupConfig.getLookupService() != null && !lookupConfig.getLookupService().trim().isEmpty();
        boolean hasDataset = lookupConfig.getLookupDataset() != null;

        if (!hasService && !hasDataset) {
            throw new ConfigurationException("lookup-config must specify either 'lookup-service' or 'lookup-dataset' for enrichment: " + enrichmentId);
        }

        // Cannot have both (this is a design decision - could be relaxed if needed)
        if (hasService && hasDataset) {
            throw new ConfigurationException("lookup-config cannot specify both 'lookup-service' and 'lookup-dataset' for enrichment: " + enrichmentId + ". Choose one approach");
        }

        // Validate lookup-key is present
        if (lookupConfig.getLookupKey() == null || lookupConfig.getLookupKey().trim().isEmpty()) {
            throw new ConfigurationException("lookup-key is required in lookup-config for enrichment: " + enrichmentId);
        }
    }

    /**
     * Validate lookup key expression according to patterns from lookups.md.
     */
    private void validateLookupKeyExpression(String lookupKey, String enrichmentId) throws ConfigurationException {
        if (lookupKey == null || lookupKey.trim().isEmpty()) {
            throw new ConfigurationException("lookup-key is required for lookup enrichment: " + enrichmentId);
        }

        // Validate SpEL syntax
        if (!isValidSpELExpression(lookupKey)) {
            throw new ConfigurationException("Invalid SpEL expression in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId);
        }

        // Validate lookup key patterns from lookups.md
        validateLookupKeyPatterns(lookupKey, enrichmentId);
    }

    /**
     * Validate complex lookup key patterns documented in lookups.md.
     */
    private void validateLookupKeyPatterns(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Pattern 1: String concatenation (compound keys)
        if (lookupKey.contains("+") && lookupKey.contains("'")) {
            validateStringConcatenationPattern(lookupKey, enrichmentId);
        }

        // Pattern 2: Conditional expressions (ternary operators)
        if (lookupKey.contains("?") && lookupKey.contains(":")) {
            validateConditionalExpressionPattern(lookupKey, enrichmentId);
        }

        // Pattern 3: String manipulation methods
        if (lookupKey.contains(".substring(") || lookupKey.contains(".toUpperCase(") || lookupKey.contains(".toLowerCase(")) {
            validateStringManipulationPattern(lookupKey, enrichmentId);
        }

        // Pattern 4: Hash-based compound keys
        if (lookupKey.contains("T(java.lang.String).valueOf") || lookupKey.contains(".hashCode()")) {
            validateHashBasedKeyPattern(lookupKey, enrichmentId);
        }

        // Pattern 5: Hierarchical field access
        if (lookupKey.contains(".") && !lookupKey.contains("(")) {
            validateHierarchicalFieldAccess(lookupKey, enrichmentId);
        }

        // Pattern 6: Safe navigation operator
        if (lookupKey.contains("?.")) {
            validateSafeNavigationPattern(lookupKey, enrichmentId);
        }
    }

    /**
     * Validate string concatenation patterns in lookup keys.
     */
    private void validateStringConcatenationPattern(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Check for balanced quotes in concatenation
        long singleQuoteCount = lookupKey.chars().filter(ch -> ch == '\'').count();
        if (singleQuoteCount % 2 != 0) {
            throw new ConfigurationException("Unbalanced single quotes in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId);
        }

        // Check for proper concatenation syntax
        if (lookupKey.contains("++") || lookupKey.contains("+ +")) {
            throw new ConfigurationException("Invalid concatenation syntax in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use single '+' for concatenation");
        }
    }

    /**
     * Validate conditional expression patterns in lookup keys.
     */
    private void validateConditionalExpressionPattern(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Handle Elvis operator (?:) - safe navigation with null coalescing
        if (lookupKey.contains("?:")) {
            // Elvis operator is valid - just ensure basic syntax
            if (lookupKey.indexOf("?:") == lookupKey.length() - 2) {
                throw new ConfigurationException("Elvis operator (?:) missing right operand in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId);
            }
            return; // Elvis operator is valid, skip ternary validation
        }

        // Count ternary operators (excluding safe navigation ?. and string literals)
        String withoutSafeNav = lookupKey.replace("?.", "X."); // Replace ?. with X. to avoid counting

        // Remove string literals to avoid counting colons inside strings
        String withoutStrings = withoutSafeNav.replaceAll("'[^']*'", "''"); // Replace 'text' with ''
        withoutStrings = withoutStrings.replaceAll("\"[^\"]*\"", "\"\""); // Replace "text" with ""

        long questionMarkCount = withoutStrings.chars().filter(ch -> ch == '?').count();
        long colonCount = withoutStrings.chars().filter(ch -> ch == ':').count();

        // Basic ternary validation - should have matching ? and :
        if (questionMarkCount != colonCount) {
            throw new ConfigurationException("Unbalanced ternary operators in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Each '?' must have a matching ':'");
        }

        // Check for nested ternary complexity (warn if too complex)
        if (questionMarkCount > 2) {
            logger.warn("Complex nested ternary expression in lookup-key for enrichment: " + enrichmentId + ". Consider simplifying for maintainability");
        }
    }

    /**
     * Validate string manipulation patterns in lookup keys.
     */
    private void validateStringManipulationPattern(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Validate substring calls
        if (lookupKey.contains(".substring(")) {
            validateSubstringCalls(lookupKey, enrichmentId);
        }

        // Validate case conversion calls
        if (lookupKey.contains(".toUpperCase(") || lookupKey.contains(".toLowerCase(")) {
            validateCaseConversionCalls(lookupKey, enrichmentId);
        }
    }

    /**
     * Validate substring method calls in lookup keys.
     */
    private void validateSubstringCalls(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Check for proper substring syntax
        if (lookupKey.contains(".substring()")) {
            throw new ConfigurationException("Invalid substring call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". substring() requires parameters");
        }

        // For now, just validate that substring calls have basic structure - detailed validation can be done at runtime
        // This is a simple check to ensure substring calls are not malformed
        if (lookupKey.contains(".substring(") && !lookupKey.contains(")")) {
            throw new ConfigurationException("Malformed substring call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Missing closing parenthesis");
        }
    }

    /**
     * Validate case conversion method calls in lookup keys.
     */
    private void validateCaseConversionCalls(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Check for proper method call syntax
        if (lookupKey.contains(".toUpperCase") && !lookupKey.contains(".toUpperCase()")) {
            throw new ConfigurationException("Invalid toUpperCase call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use toUpperCase()");
        }

        if (lookupKey.contains(".toLowerCase") && !lookupKey.contains(".toLowerCase()")) {
            throw new ConfigurationException("Invalid toLowerCase call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use toLowerCase()");
        }
    }

    /**
     * Validate hash-based compound key patterns.
     */
    private void validateHashBasedKeyPattern(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Validate T(java.lang.String).valueOf usage
        if (lookupKey.contains("T(java.lang.String).valueOf") && !lookupKey.contains("T(java.lang.String).valueOf(")) {
            throw new ConfigurationException("Invalid T(java.lang.String).valueOf usage in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Must include opening parenthesis");
        }

        // Validate hashCode usage
        if (lookupKey.contains(".hashCode") && !lookupKey.contains(".hashCode()")) {
            throw new ConfigurationException("Invalid hashCode call in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use hashCode()");
        }

        // Warn about hash collision potential
        if (lookupKey.contains(".hashCode()")) {
            logger.warn("Hash-based lookup key in enrichment: " + enrichmentId + ". Be aware of potential hash collisions in production data");
        }
    }

    /**
     * Validate hierarchical field access patterns.
     */
    private void validateHierarchicalFieldAccess(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Check for excessive nesting depth
        long dotCount = lookupKey.chars().filter(ch -> ch == '.').count();
        if (dotCount > 5) {
            logger.warn("Deep hierarchical field access in lookup-key for enrichment: " + enrichmentId + ". Consider flattening data structure for better performance");
        }

        // Check for field access on potentially null objects without safe navigation
        if (lookupKey.contains(".") && !lookupKey.contains("?.") && !lookupKey.contains("!= null")) {
            logger.info("Consider using safe navigation operator (?.) in lookup-key for enrichment: " + enrichmentId + " to handle null values gracefully");
        }
    }

    /**
     * Validate safe navigation operator patterns.
     */
    private void validateSafeNavigationPattern(String lookupKey, String enrichmentId) throws ConfigurationException {
        // Check for proper safe navigation syntax
        if (lookupKey.contains("? .")) {
            throw new ConfigurationException("Invalid safe navigation syntax in lookup-key '" + lookupKey + "' for enrichment: " + enrichmentId + ". Use '?.' without space");
        }

        // Validate that safe navigation is used consistently
        if (lookupKey.contains("?.") && lookupKey.contains(".") && !lookupKey.contains("?:")) {
            logger.info("Mixed safe and unsafe navigation in lookup-key for enrichment: " + enrichmentId + ". Consider using consistent safe navigation or null checks");
        }
    }

    /**
     * Validate condition patterns in enrichment conditions.
     */
    private void validateConditionPatterns(String condition, String enrichmentId) throws ConfigurationException {
        // Check for common condition patterns
        if (condition.contains("!= null") || condition.contains("== null")) {
            // Good - explicit null checks
        } else if (condition.contains(".") && !condition.contains("?.")) {
            logger.info("Consider adding null checks in condition for enrichment: " + enrichmentId + " to prevent NullPointerException");
        }

        // Check for boolean logic complexity
        long andCount = condition.split("&&").length - 1;
        long orCount = condition.split("\\|\\|").length - 1;
        if (andCount + orCount > 3) {
            logger.warn("Complex boolean logic in condition for enrichment: " + enrichmentId + ". Consider simplifying for maintainability");
        }
    }

    /**
     * Validate lookup dataset configuration.
     */
    private void validateLookupDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws ConfigurationException {
        if (dataset.getType() == null || dataset.getType().trim().isEmpty()) {
            throw new ConfigurationException("Dataset type is required for enrichment: " + enrichmentId);
        }

        String type = dataset.getType().toLowerCase();
        Set<String> validTypes = Set.of("inline", "yaml-file", "csv-file", "file-system", "database", "rest-api");

        if (!validTypes.contains(type)) {
            throw new ConfigurationException("Invalid dataset type '" + type + "' for enrichment: " + enrichmentId + ". Valid types: " + validTypes);
        }

        // Type-specific validation
        switch (type) {
            case "inline":
                validateInlineDataset(dataset, enrichmentId);
                break;
            case "yaml-file":
            case "csv-file":
            case "file-system":
                validateFileDataset(dataset, enrichmentId, type);
                break;
            case "database":
                validateDatabaseDataset(dataset, enrichmentId);
                break;
            case "rest-api":
                validateRestApiDataset(dataset, enrichmentId);
                break;
        }
    }

    /**
     * Validate inline dataset configuration.
     */
    private void validateInlineDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws ConfigurationException {
        if (dataset.getData() == null || dataset.getData().isEmpty()) {
            throw new ConfigurationException("Inline dataset must have 'data' array for enrichment: " + enrichmentId);
        }

        if (dataset.getKeyField() == null || dataset.getKeyField().trim().isEmpty()) {
            throw new ConfigurationException("Inline dataset must specify 'key-field' for enrichment: " + enrichmentId);
        }

        // Validate that all data records have the key field
        String keyField = dataset.getKeyField();
        for (int i = 0; i < dataset.getData().size(); i++) {
            Map<String, Object> record = dataset.getData().get(i);
            if (!record.containsKey(keyField)) {
                throw new ConfigurationException("Data record at index " + i + " missing key field '" + keyField + "' for enrichment: " + enrichmentId);
            }

            Object keyValue = record.get(keyField);
            if (keyValue == null) {
                throw new ConfigurationException("Data record at index " + i + " has null value for key field '" + keyField + "' for enrichment: " + enrichmentId);
            }
        }

        // Check for duplicate keys (skip check when rows: "all" allows multi-row lookups)
        if (!dataset.isMultiRow()) {
            Set<Object> keyValues = new HashSet<>();
            for (int i = 0; i < dataset.getData().size(); i++) {
                Object keyValue = dataset.getData().get(i).get(keyField);
                if (!keyValues.add(keyValue)) {
                    throw new ConfigurationException("Duplicate key value '" + keyValue + "' found in inline dataset for enrichment: " + enrichmentId);
                }
            }
        }
    }

    /**
     * Validate file-based dataset configuration.
     */
    private void validateFileDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId, String type) throws ConfigurationException {
        if (dataset.getFilePath() == null || dataset.getFilePath().trim().isEmpty()) {
            throw new ConfigurationException(type + " dataset must specify 'file-path' for enrichment: " + enrichmentId);
        }

        if (dataset.getKeyField() == null || dataset.getKeyField().trim().isEmpty()) {
            throw new ConfigurationException(type + " dataset must specify 'key-field' for enrichment: " + enrichmentId);
        }

        // Validate file extension matches type
        String filePath = dataset.getFilePath().toLowerCase();
        if ("yaml-file".equals(type) && !filePath.endsWith(".yaml") && !filePath.endsWith(".yml")) {
            logger.warn("YAML dataset file path should end with .yaml or .yml for enrichment: " + enrichmentId);
        }

        if ("csv-file".equals(type) && !filePath.endsWith(".csv")) {
            logger.warn("CSV dataset file path should end with .csv for enrichment: " + enrichmentId);
        }
    }

    /**
     * Validate database dataset configuration.
     */
    private void validateDatabaseDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws ConfigurationException {
        // Database datasets typically don't use key-field (they use SQL queries)
        // This is a placeholder for future database-specific validation
        logger.debug("Database dataset validation for enrichment: " + enrichmentId);
    }

    /**
     * Validate REST API dataset configuration.
     */
    private void validateRestApiDataset(YamlEnrichment.LookupDataset dataset, String enrichmentId) throws ConfigurationException {
        // REST API datasets typically don't use key-field (they use URL patterns)
        // This is a placeholder for future REST API-specific validation
        logger.debug("REST API dataset validation for enrichment: " + enrichmentId);
    }

    /**
     * Validate caching configuration.
     */
    private void validateCachingConfiguration(YamlEnrichment.LookupConfig lookupConfig, String enrichmentId) throws ConfigurationException {
        if (lookupConfig.getCacheTtlSeconds() != null) {
            Integer ttl = lookupConfig.getCacheTtlSeconds();
            if (ttl < 0) {
                throw new ConfigurationException("Cache TTL cannot be negative for enrichment: " + enrichmentId);
            }

            if (ttl > 86400) { // 24 hours
                logger.warn("Cache TTL is very long (" + ttl + " seconds) for enrichment: " + enrichmentId + ". Consider if this is appropriate for your use case");
            }
        }
    }

    /**
     * Validate field mappings configuration.
     */
    private void validateFieldMappings(List<YamlEnrichment.FieldMapping> fieldMappings, String enrichmentId) throws ConfigurationException {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            // Field mappings are optional for some enrichment types
            return;
        }

        Set<String> targetFields = new HashSet<>();

        for (int i = 0; i < fieldMappings.size(); i++) {
            YamlEnrichment.FieldMapping mapping = fieldMappings.get(i);

            // Validate required fields
            boolean hasSourceField = mapping.getSourceField() != null && !mapping.getSourceField().trim().isEmpty();
            boolean hasExpression = mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty();

            if (!hasSourceField && !hasExpression) {
                throw new ConfigurationException("Field mapping at index " + i + " missing 'source-field' for enrichment: " + enrichmentId + ". Must provide at least one of: source-field, expression (or transformation).");
            }

            if (mapping.getTargetField() == null || mapping.getTargetField().trim().isEmpty()) {
                throw new ConfigurationException("Field mapping at index " + i + " missing 'target-field' for enrichment: " + enrichmentId);
            }

            // Check for duplicate target fields
            String targetField = mapping.getTargetField();
            if (!targetFields.add(targetField)) {
                throw new ConfigurationException("Duplicate target field '" + targetField + "' in field mappings for enrichment: " + enrichmentId);
            }

            // Validate expression if present
            if (mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty()) {
                String expression = mapping.getExpression();
                if (!isValidSpELExpression(expression)) {
                    throw new ConfigurationException("Invalid expression '" + expression + "' in field mapping for enrichment: " + enrichmentId);
                }

                // Validate expression patterns
                validateExpressionPatterns(expression, enrichmentId, i);
            }

            // Validate conditional mappings if present
            validateConditionalMappings(mapping, enrichmentId, i);
        }
    }

    /**
     * Validate expression patterns in field mappings.
     */
    private void validateExpressionPatterns(String expression, String enrichmentId, int mappingIndex) throws ConfigurationException {
        // Check for common expression patterns
        if (expression.contains("T(java.") && !expression.contains("T(java.lang.") && !expression.contains("T(java.time.") && !expression.contains("T(java.math.")) {
            logger.warn("Expression uses Java type reference in field mapping " + mappingIndex + " for enrichment: " + enrichmentId + ". Ensure the class is available at runtime");
        }

        // Check for potentially unsafe operations
        if (expression.contains(".getClass()") || expression.contains("T(java.lang.Class)")) {
            logger.warn("Expression uses reflection in field mapping " + mappingIndex + " for enrichment: " + enrichmentId + ". This may have security implications");
        }

        // Check for null safety
        if (expression.contains(".") && !expression.contains("?.") && !expression.contains("!= null")) {
            logger.info("Consider adding null safety to expression in field mapping " + mappingIndex + " for enrichment: " + enrichmentId);
        }
    }

    /**
     * Validate conditional mappings in field mappings.
     */
    private void validateConditionalMappings(YamlEnrichment.FieldMapping mapping, String enrichmentId, int mappingIndex) throws ConfigurationException {
        // This is a placeholder for future conditional mapping validation
        // The current YamlEnrichment.FieldMapping class doesn't have conditional mapping support
        // but this method is here for future extensibility
        logger.trace("Conditional mapping validation for field mapping " + mappingIndex + " in enrichment: " + enrichmentId);
    }

    /**
     * Validate conditional mappings for field-enrichment.
     */
    private void validateConditionalMappings(List<YamlEnrichment.ConditionalMapping> conditionalMappings, String enrichmentId) throws ConfigurationException {
        if (conditionalMappings == null || conditionalMappings.isEmpty()) {
            return;
        }

        logger.debug("Validating " + conditionalMappings.size() + " conditional mappings for enrichment: " + enrichmentId);

        for (int i = 0; i < conditionalMappings.size(); i++) {
            YamlEnrichment.ConditionalMapping conditionalMapping = conditionalMappings.get(i);

            // Validate condition group
            validateConditionGroup(conditionalMapping.getConditions(), enrichmentId, i);

            // Validate field mappings within conditional mapping
            if (conditionalMapping.getFieldMappings() == null || conditionalMapping.getFieldMappings().isEmpty()) {
                throw new ConfigurationException("Conditional mapping at index " + i + " missing 'field-mappings' for enrichment: " + enrichmentId);
            }

            // Validate each field mapping
            validateFieldMappings(conditionalMapping.getFieldMappings(), enrichmentId + ".conditional-mapping[" + i + "]");
        }
    }

    /**
     * Validate condition group for conditional mappings.
     */
    private void validateConditionGroup(SharedConditionGroup conditionGroup, String enrichmentId, int mappingIndex) throws ConfigurationException {
        if (conditionGroup == null) {
            throw new ConfigurationException("Conditional mapping at index " + mappingIndex + " missing 'conditions' for enrichment: " + enrichmentId);
        }

        // Validate operator
        String operator = conditionGroup.getOperator();
        if (operator != null && !"OR".equalsIgnoreCase(operator) && !"AND".equalsIgnoreCase(operator)) {
            throw new ConfigurationException("Invalid condition operator '" + operator + "' at conditional mapping " + mappingIndex + " for enrichment: " + enrichmentId + ". Valid operators: OR, AND");
        }

        // Validate rules
        if (conditionGroup.getRules() == null || conditionGroup.getRules().isEmpty()) {
            throw new ConfigurationException("Conditional mapping at index " + mappingIndex + " missing condition 'rules' for enrichment: " + enrichmentId);
        }

        // Validate each condition rule
        for (int j = 0; j < conditionGroup.getRules().size(); j++) {
            SharedConditionRule rule = conditionGroup.getRules().get(j);
            if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
                throw new ConfigurationException("Condition rule at index " + j + " in conditional mapping " + mappingIndex + " missing 'condition' for enrichment: " + enrichmentId);
            }

            // Validate condition expression syntax
            if (!isValidSpELExpression(rule.getCondition())) {
                throw new ConfigurationException("Invalid SpEL expression in condition '" + rule.getCondition() + "' for enrichment: " + enrichmentId + ".conditional-mapping[" + mappingIndex + "].rule[" + j + "]");
            }

            // Validate condition patterns
            validateConditionPatterns(rule.getCondition(), enrichmentId + ".conditional-mapping[" + mappingIndex + "].rule[" + j + "]");
        }
    }

    /**
     * Validate mapping rules for conditional-mapping-enrichment.
     */
    private void validateMappingRules(List<YamlEnrichment.MappingRule> mappingRules, String enrichmentId) throws ConfigurationException {
        if (mappingRules == null || mappingRules.isEmpty()) {
            return;
        }

        logger.debug("Validating " + mappingRules.size() + " mapping rules for enrichment: " + enrichmentId);

        for (int i = 0; i < mappingRules.size(); i++) {
            YamlEnrichment.MappingRule rule = mappingRules.get(i);

            // Validate rule ID
            if (rule.getId() == null || rule.getId().trim().isEmpty()) {
                throw new ConfigurationException("Mapping rule at index " + i + " missing 'id' for enrichment: " + enrichmentId);
            }

            // Validate priority
            if (rule.getPriority() == null) {
                throw new ConfigurationException("Mapping rule '" + rule.getId() + "' missing 'priority' for enrichment: " + enrichmentId);
            }

            // Validate conditions (unless it's a default rule)
            if (rule.getConditions() != null) {
                validateConditionGroup(rule.getConditions(), enrichmentId, i);
            }

            // Validate mapping configuration
            if (rule.getMapping() == null) {
                throw new ConfigurationException("Mapping rule '" + rule.getId() + "' missing 'mapping' configuration for enrichment: " + enrichmentId);
            }

            validateMappingConfig(rule.getMapping(), enrichmentId, rule.getId());
        }
    }

    /**
     * Validate mapping configuration for conditional mapping rules.
     */
    private void validateMappingConfig(YamlEnrichment.MappingConfig mappingConfig, String enrichmentId, String ruleId) throws ConfigurationException {
        // Validate mapping type
        String type = mappingConfig.getType();
        if (type == null || type.trim().isEmpty()) {
            throw new ConfigurationException("Mapping configuration missing 'type' for rule '" + ruleId + "' in enrichment: " + enrichmentId);
        }

        if (!"direct".equalsIgnoreCase(type) && !"lookup".equalsIgnoreCase(type) && !"function".equalsIgnoreCase(type)) {
            throw new ConfigurationException("Invalid mapping type '" + type + "' for rule '" + ruleId + "' in enrichment: " + enrichmentId + ". Valid types: direct, lookup, function");
        }

        // Type-specific validation
        if ("direct".equalsIgnoreCase(type)) {
            // Direct mapping should have source-field or expression
            if ((mappingConfig.getSourceField() == null || mappingConfig.getSourceField().trim().isEmpty()) &&
                (mappingConfig.getExpression() == null || mappingConfig.getExpression().trim().isEmpty())) {
                throw new ConfigurationException("Direct mapping requires either 'source-field' or 'expression' for rule '" + ruleId + "' in enrichment: " + enrichmentId);
            }
        } else if ("lookup".equalsIgnoreCase(type)) {
            // Lookup mapping should have lookup-config
            if (mappingConfig.getLookupConfig() == null) {
                throw new ConfigurationException("Lookup mapping requires 'lookup-config' for rule '" + ruleId + "' in enrichment: " + enrichmentId);
            }
        } else if ("function".equalsIgnoreCase(type)) {
            // Function mapping requires enrichment-group-ref and output-field
            if (mappingConfig.getEnrichmentGroupRef() == null || mappingConfig.getEnrichmentGroupRef().trim().isEmpty()) {
                throw new ConfigurationException("Function mapping requires 'enrichment-group-ref' for rule '" + ruleId + "' in enrichment: " + enrichmentId);
            }
            if (mappingConfig.getOutputField() == null || mappingConfig.getOutputField().trim().isEmpty()) {
                throw new ConfigurationException("Function mapping requires 'output-field' for rule '" + ruleId + "' in enrichment: " + enrichmentId);
            }
        }

        // Validate expression if present
        if (mappingConfig.getExpression() != null && !mappingConfig.getExpression().trim().isEmpty()) {
            if (!isValidSpELExpression(mappingConfig.getExpression())) {
                throw new ConfigurationException("Invalid SpEL expression '" + mappingConfig.getExpression() + "' for rule '" + ruleId + "' in enrichment: " + enrichmentId);
            }
        }
    }

    /**
     * Validate SpEL expression syntax.
     */
    private boolean isValidSpELExpression(String expression) {
        try {
            // Handle template expressions (#{...}) by extracting and validating individual expressions
            if (expression.contains("#{") && expression.contains("}")) {
                return validateTemplateExpression(expression);
            }

            // Use a simple SpEL parser to validate syntax for regular expressions
            org.springframework.expression.ExpressionParser parser = dev.mars.apex.engine.core.SpelParserHolder.INSTANCE;
            parser.parseExpression(expression);
            return true;
        } catch (Exception e) {
            logger.debug("Invalid SpEL expression: " + expression + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate template expressions containing #{...} syntax.
     */
    private boolean validateTemplateExpression(String template) {
        try {
            // Extract expressions from #{...} blocks and validate each one
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\{([^}]+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(template);

            org.springframework.expression.ExpressionParser parser = dev.mars.apex.engine.core.SpelParserHolder.INSTANCE;

            while (matcher.find()) {
                String expression = matcher.group(1);
                parser.parseExpression(expression); // This will throw if invalid
            }

            return true;
        } catch (Exception e) {
            logger.debug("Invalid template expression: " + template + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate enrichment groups in the configuration.
     */
    private void validateEnrichmentGroups(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getEnrichmentGroups() == null || config.getEnrichmentGroups().isEmpty()) {
            logger.debug("No enrichment groups to validate");
            return;
        }

        // Build set of available enrichment IDs for reference checks
        Set<String> enrichmentIds = new HashSet<>();
        if (config.getEnrichments() != null) {
            for (YamlEnrichment e : config.getEnrichments()) {
                if (e != null && e.getId() != null) {
                    enrichmentIds.add(e.getId());
                }
            }
        }

        // Build set of enrichment group IDs for group-reference validation
        Set<String> groupIds = new HashSet<>();
        for (YamlEnrichmentGroup g : config.getEnrichmentGroups()) {
            if (g != null && g.getId() != null) {
                groupIds.add(g.getId());
            }
        }

        // Adjacency list and indegree map for cycle detection (Kahn's algorithm)
        Map<String, List<String>> groupRefAdj = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (String gid : groupIds) {
            groupRefAdj.put(gid, new ArrayList<>());
            indegree.put(gid, 0);
        }

        for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
            if (group == null) continue;

            // Required fields
            if (group.getId() == null || group.getId().trim().isEmpty()) {
                throw new ConfigurationException("Enrichment group ID is required");
            }
            if (group.getName() == null || group.getName().trim().isEmpty()) {
                throw new ConfigurationException("Enrichment group name is required for group: " + group.getId());
            }

            // Operator validation when present
            if (group.getOperator() != null) {
                String op = group.getOperator().trim().toUpperCase();
                if (!"AND".equals(op) && !"OR".equals(op)) {
                    throw new ConfigurationException("Enrichment group '" + group.getId() + "' has invalid operator '" + group.getOperator() + "'. Must be AND or OR");
                }
            }

            // Sequence uniqueness for enrichment-references
            if (group.getEnrichmentReferences() != null && !group.getEnrichmentReferences().isEmpty()) {
                Set<Integer> sequences = new HashSet<>();
                for (YamlEnrichmentGroup.EnrichmentReference ref : group.getEnrichmentReferences()) {
                    if (!EnabledFilter.isEnabled(ref)) continue;
                    int seq = ref.getSequence() != null ? ref.getSequence() : 1;
                    if (!sequences.add(seq)) {
                        throw new ConfigurationException("Duplicate enrichment reference sequence " + seq + " in group: " + group.getId());
                    }

                    // Reference existence check
                    if (ref.getEnrichmentId() == null || !enrichmentIds.contains(ref.getEnrichmentId())) {
                        throw new ConfigurationException("Enrichment reference not found: " + ref.getEnrichmentId() + " in group: " + group.getId());
                    }
                }
            }

            // enrichment-ids existence check
            if (group.getEnrichmentIds() != null) {
                for (String id : group.getEnrichmentIds()) {
                    if (id == null || !enrichmentIds.contains(id)) {
                        throw new ConfigurationException("Enrichment id not found: " + id + " in group: " + group.getId());
                    }
                }
            }

            // enrichment-group-references: existence and self-reference checks, build graph
            if (group.getEnrichmentGroupReferences() != null && !group.getEnrichmentGroupReferences().isEmpty()) {
                for (String refGroupId : group.getEnrichmentGroupReferences()) {
                    if (refGroupId == null || refGroupId.trim().isEmpty()) {
                        throw new ConfigurationException("Enrichment group '" + group.getId() + "' has an empty enrichment-group-reference");
                    }
                    if (refGroupId.equals(group.getId())) {
                        throw new ConfigurationException("Enrichment group '" + group.getId() + "' cannot reference itself");
                    }
                    if (!groupIds.contains(refGroupId)) {
                        throw new ConfigurationException("Referenced enrichment group not found: " + refGroupId + " in group: " + group.getId());
                    }
                    // Build adjacency and indegree for cycle detection
                    groupRefAdj.get(group.getId()).add(refGroupId);
                    indegree.put(refGroupId, indegree.get(refGroupId) + 1);
                }
            }
        }

        // Cycle detection using Kahn's algorithm (topological sort)
        int processed = 0;
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }
        while (!queue.isEmpty()) {
            String node = queue.poll();
            processed++;
            for (String neighbor : groupRefAdj.getOrDefault(node, Collections.emptyList())) {
                int newIn = indegree.get(neighbor) - 1;
                indegree.put(neighbor, newIn);
                if (newIn == 0) {
                    queue.add(neighbor);
                }
            }
        }
        if (processed < groupIds.size()) {
            throw new ConfigurationException("Cyclic enrichment-group-references detected");
        }

        logger.debug("Enrichment group validation completed successfully");
    }
}
