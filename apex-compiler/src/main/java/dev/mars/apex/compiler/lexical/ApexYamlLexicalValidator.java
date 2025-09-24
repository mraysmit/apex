package dev.mars.apex.compiler.lexical;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.mars.apex.core.config.yaml.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * APEX YAML Lexical Grammar Checker.
 * 
 * Validates APEX YAML files against the formal grammar without generating Java code.
 * The purpose is to catch syntax, structure, and semantic errors at compile-time.
 * 
 * Validation Rules:
 * 1. YAML Syntax Validation
 * 2. APEX Metadata Structure Validation  
 * 3. Document Type-Specific Validation
 * 4. SpEL Expression Syntax Validation
 * 5. Field Reference Validation
 * 6. Cross-Reference Validation
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-01-06
 * @version 1.0
 */
public class ApexYamlLexicalValidator {

    // APEX YAML Grammar Constants - Metadata validation
    private static final Set<String> REQUIRED_METADATA_FIELDS = Set.of(
        "id", "name", "version", "description", "type"
    );

    // Valid document types (kept for metadata validation)
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of(
        "rule-config", "enrichment", "dataset", "scenario",
        "scenario-registry", "bootstrap", "rule-chain", "external-data-config",
        "pipeline-config"
    );

    // SpEL Expression Pattern (basic validation)
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?");

    // Cache for reflection-based validation
    private static final Map<Class<?>, Set<String>> VALID_SECTIONS_CACHE = new HashMap<>();

    private final Yaml yamlParser;

    public ApexYamlLexicalValidator() {
        this.yamlParser = new Yaml();
    }

    /**
     * Get valid YAML sections for a given APEX configuration class using reflection.
     */
    private Set<String> getValidSections(Class<?> configClass) {
        return VALID_SECTIONS_CACHE.computeIfAbsent(configClass, this::extractJsonPropertyFields);
    }

    /**
     * Extract @JsonProperty field names from a class using reflection.
     */
    private Set<String> extractJsonPropertyFields(Class<?> clazz) {
        Set<String> fields = new HashSet<>();

        // Get all declared fields from the class
        for (Field field : clazz.getDeclaredFields()) {
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            if (jsonProperty != null) {
                String propertyName = jsonProperty.value();
                if (!propertyName.isEmpty()) {
                    fields.add(propertyName);
                } else {
                    // If no explicit value, use field name
                    fields.add(field.getName());
                }
            }
        }

        return fields;
    }
    
    /**
     * Validate APEX YAML file.
     */
    public ValidationResult validateFile(Path yamlFile) {
        ValidationResult result = new ValidationResult(yamlFile.toString());
        
        try {
            // Step 1: Basic file validation
            if (!Files.exists(yamlFile)) {
                result.addError("File does not exist: " + yamlFile);
                return result;
            }
            
            if (!yamlFile.toString().toLowerCase().endsWith(".yaml") && 
                !yamlFile.toString().toLowerCase().endsWith(".yml")) {
                result.addWarning("File should have .yaml or .yml extension");
            }
            
            // Step 2: Read and parse YAML syntax
            String yamlContent = Files.readString(yamlFile);
            Map<String, Object> yamlData = parseYamlSyntax(yamlContent, result);
            
            if (yamlData == null) {
                return result; // Syntax errors already recorded
            }
            
            // Step 3: Validate APEX structure
            validateApexStructure(yamlData, result);
            
            // Step 4: Validate document type-specific rules
            String documentType = extractDocumentType(yamlData);
            if (documentType != null) {
                validateTypeSpecificRules(yamlData, documentType, result);
            }
            
            // Step 5: Validate SpEL expressions
            validateSpelExpressions(yamlData, result);
            
        } catch (IOException e) {
            result.addError("Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            result.addError("Unexpected validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse YAML syntax and return structured data.
     */
    private Map<String, Object> parseYamlSyntax(String yamlContent, ValidationResult result) {
        try {
            Object parsed = yamlParser.load(yamlContent);
            
            if (!(parsed instanceof Map)) {
                result.addError("YAML root must be a map/object, not " + 
                              (parsed != null ? parsed.getClass().getSimpleName() : "null"));
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlData = (Map<String, Object>) parsed;
            return yamlData;
            
        } catch (YAMLException e) {
            result.addError("YAML syntax error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate APEX document structure.
     */
    private void validateApexStructure(Map<String, Object> yamlData, ValidationResult result) {
        // Validate metadata section exists
        if (!yamlData.containsKey("metadata")) {
            result.addError("Missing required 'metadata' section");
            return;
        }
        
        Object metadataObj = yamlData.get("metadata");
        if (!(metadataObj instanceof Map)) {
            result.addError("'metadata' section must be a map/object");
            return;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) metadataObj;
        
        // Validate required metadata fields
        for (String requiredField : REQUIRED_METADATA_FIELDS) {
            if (!metadata.containsKey(requiredField)) {
                result.addError("Missing required metadata field: " + requiredField);
            } else {
                Object value = metadata.get(requiredField);
                if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                    result.addError("Metadata field '" + requiredField + "' must be a non-empty string");
                }
            }
        }
        
        // Validate document type
        String documentType = (String) metadata.get("type");
        if (documentType != null && !VALID_DOCUMENT_TYPES.contains(documentType)) {
            result.addError("Invalid document type: " + documentType + 
                          ". Valid types: " + VALID_DOCUMENT_TYPES);
        }
        
        // Validate version format
        String version = (String) metadata.get("version");
        if (version != null && !VERSION_PATTERN.matcher(version).matches()) {
            result.addWarning("Version should follow semantic versioning format (e.g., 1.0.0): " + version);
        }
    }
    
    /**
     * Validate YAML sections against actual APEX configuration classes using reflection.
     */
    private void validateTypeSpecificRules(Map<String, Object> yamlData, String documentType,
                                         ValidationResult result) {
        // Get the appropriate configuration class based on document type
        Class<?> configClass = getConfigurationClass(documentType);

        // Get valid sections from the appropriate configuration class
        Set<String> validSections = getValidSections(configClass);

        // Validate all top-level sections (except metadata which is handled separately)
        for (String section : yamlData.keySet()) {
            if (!"metadata".equals(section) && !validSections.contains(section)) {
                result.addError("Invalid YAML section: '" + section + "'. Valid sections for " +
                              documentType + " are: " + validSections);
            }
        }

        // Validate that we have at least one content section for certain document types
        if ("rule-config".equals(documentType) || "external-data-config".equals(documentType)) {
            boolean hasContentSection = yamlData.keySet().stream()
                .anyMatch(section -> validSections.contains(section) && !"metadata".equals(section));

            if (!hasContentSection) {
                result.addError("Document type '" + documentType + "' requires at least one content section from: " + validSections);
            }
        }

        // Validate specific section contents if present
        validateSectionContents(yamlData, result);
    }

    /**
     * Get the appropriate configuration class based on document type.
     */
    private Class<?> getConfigurationClass(String documentType) {
        if (documentType == null) {
            return YamlRuleConfiguration.class; // Default fallback
        }

        switch (documentType) {
            case "external-data-config":
                return ExternalDataConfiguration.class;
            case "rule-config":
            case "enrichment":
            case "dataset":
            case "scenario":
            case "scenario-registry":
            case "bootstrap":
            case "rule-chain":
            case "pipeline-config":
            default:
                return YamlRuleConfiguration.class;
        }
    }

    /**
     * Validate the contents of specific YAML sections.
     */
    private void validateSectionContents(Map<String, Object> yamlData, ValidationResult result) {
        // Validate enrichments section if present
        if (yamlData.containsKey("enrichments")) {
            validateEnrichmentsSection(yamlData.get("enrichments"), result);
        }

        // Validate data-sources section if present (rule-config format)
        if (yamlData.containsKey("data-sources")) {
            validateDataSourcesSection(yamlData.get("data-sources"), result);
        }

        // Validate dataSources section if present (external-data-config format)
        if (yamlData.containsKey("dataSources")) {
            validateDataSourcesSection(yamlData.get("dataSources"), result);
        }

        // Validate rules section if present
        if (yamlData.containsKey("rules")) {
            validateRulesSection(yamlData.get("rules"), result);
        }
    }

    /**
     * Validate enrichments section structure.
     */
    private void validateEnrichmentsSection(Object enrichments, ValidationResult result) {
        if (!(enrichments instanceof List)) {
            result.addError("'enrichments' section must be a list");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object> enrichmentList = (List<Object>) enrichments;

        for (int i = 0; i < enrichmentList.size(); i++) {
            Object enrichment = enrichmentList.get(i);
            if (!(enrichment instanceof Map)) {
                result.addError("enrichments[" + i + "] must be an object");
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichmentMap = (Map<String, Object>) enrichment;

            // Validate required fields for enrichment
            if (!enrichmentMap.containsKey("id")) {
                result.addError("enrichments[" + i + "] missing required field: id");
            }
        }
    }

    /**
     * Validate data-sources section structure.
     */
    private void validateDataSourcesSection(Object dataSources, ValidationResult result) {
        if (!(dataSources instanceof List)) {
            result.addError("'data-sources' section must be a list");
        }
    }

    /**
     * Validate rules section structure.
     */
    private void validateRulesSection(Object rules, ValidationResult result) {
        if (!(rules instanceof List)) {
            result.addError("'rules' section must be a list");
        }
    }
    
    /**
     * Validate SpEL expressions in the document.
     */
    private void validateSpelExpressions(Map<String, Object> yamlData, ValidationResult result) {
        // This is a basic implementation - we'll expand it
        validateSpelInObject(yamlData, "", result);
    }
    
    /**
     * Recursively validate SpEL expressions in nested objects.
     */
    private void validateSpelInObject(Object obj, String path, ValidationResult result) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String newPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                validateSpelInObject(entry.getValue(), newPath, result);
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                String newPath = path + "[" + i + "]";
                validateSpelInObject(list.get(i), newPath, result);
            }
        } else if (obj instanceof String) {
            String str = (String) obj;
            if (str.contains("#") && isSpelField(path)) {
                validateSpelExpression(str, path, result);
            }
        }
    }

    /**
     * Determine if a field path should be validated as SpEL expression.
     * Message fields and description fields should NOT be validated as SpEL.
     */
    private boolean isSpelField(String path) {
        // Fields that should NOT be validated as SpEL (they're plain text)
        String[] nonSpelFields = {
            "message", "description", "name", "id", "author", "version",
            "business-domain", "owner", "created-by", "source"
        };

        String lowerPath = path.toLowerCase();
        for (String nonSpelField : nonSpelFields) {
            if (lowerPath.endsWith("." + nonSpelField) || lowerPath.equals(nonSpelField)) {
                return false;
            }
        }

        // Fields that SHOULD be validated as SpEL
        String[] spelFields = {
            "condition", "lookup-key", "transformation", "expression",
            "calculation", "filter", "where-clause"
        };

        for (String spelField : spelFields) {
            if (lowerPath.endsWith("." + spelField) || lowerPath.equals(spelField)) {
                return true;
            }
        }

        // Default: if it contains # and we're not sure, validate it
        // This is conservative but better than missing real SpEL errors
        return true;
    }

    /**
     * Validate individual SpEL expression.
     */
    private void validateSpelExpression(String expression, String path, ValidationResult result) {
        // Basic SpEL validation - check for common patterns
        if (expression.contains("##")) {
            result.addError("Invalid SpEL expression at " + path + ": double hash not allowed");
        }
        
        // Check for unmatched parentheses
        int openParens = 0;
        for (char c : expression.toCharArray()) {
            if (c == '(') openParens++;
            else if (c == ')') openParens--;
            if (openParens < 0) {
                result.addError("Invalid SpEL expression at " + path + ": unmatched closing parenthesis");
                return;
            }
        }
        if (openParens > 0) {
            result.addError("Invalid SpEL expression at " + path + ": unmatched opening parenthesis");
        }
    }
    
    /**
     * Extract document type from YAML data.
     */
    private String extractDocumentType(Map<String, Object> yamlData) {
        Object metadataObj = yamlData.get("metadata");
        if (metadataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) metadataObj;
            return (String) metadata.get("type");
        }
        return null;
    }
    
    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private final String filePath;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        
        public ValidationResult(String filePath) {
            this.filePath = filePath;
        }
        
        public void addError(String message) {
            errors.add(message);
        }
        
        public void addWarning(String message) {
            warnings.add(message);
        }
        
        public void addInfo(String message) {
            info.add(message);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }
        
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }
        
        public List<String> getInfo() {
            return Collections.unmodifiableList(info);
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Validation Result for: ").append(filePath).append("\n");
            sb.append("Status: ").append(isValid() ? "VALID" : "INVALID").append("\n");
            
            if (!errors.isEmpty()) {
                sb.append("Errors (").append(errors.size()).append("):\n");
                for (String error : errors) {
                    sb.append("  ❌ ").append(error).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append("Warnings (").append(warnings.size()).append("):\n");
                for (String warning : warnings) {
                    sb.append("  ⚠️  ").append(warning).append("\n");
                }
            }
            
            if (!info.isEmpty()) {
                sb.append("Info (").append(info.size()).append("):\n");
                for (String infoMsg : info) {
                    sb.append("  ℹ️  ").append(infoMsg).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
}
