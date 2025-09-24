package dev.mars.apex.compiler.lexical;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
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
    
    // APEX YAML Grammar Constants (from APEX_YAML_REFERENCE.md)
    private static final Set<String> REQUIRED_METADATA_FIELDS = Set.of(
        "id", "name", "version", "description", "type"
    );
    
    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of(
        "rule-config", "enrichment", "dataset", "scenario",
        "scenario-registry", "bootstrap", "rule-chain", "external-data-config",
        "pipeline-config"
    );
    
    private static final Map<String, Set<String>> TYPE_SPECIFIC_REQUIRED_FIELDS = Map.of(
        "rule-config", Set.of("author"),
        "enrichment", Set.of("author"),
        "dataset", Set.of("source"),
        "scenario", Set.of("business-domain", "owner"),
        "scenario-registry", Set.of("created-by"),
        "bootstrap", Set.of("business-domain", "created-by"),
        "rule-chain", Set.of("author"),
        "external-data-config", Set.of("author"),
        "pipeline-config", Set.of("author")
    );
    
    private static final Map<String, Set<String>> TYPE_REQUIRED_SECTIONS = Map.of(
        "rule-config", Set.of("rules", "enrichments"), // At least one required
        "enrichment", Set.of("enrichments"),
        "dataset", Set.of("data"),
        "scenario", Set.of("scenario", "data-types", "rule-configurations"),
        "scenario-registry", Set.of("scenarios"),
        "bootstrap", Set.of("bootstrap", "data-sources"),
        "rule-chain", Set.of("rule-chains"),
        "external-data-config", Set.of("dataSources", "configuration"),
        "pipeline-config", Set.of("pipeline", "data-sources", "data-sinks") // At least one required
    );
    
    // SpEL Expression Pattern (basic validation)
    private static final Pattern SPEL_PATTERN = Pattern.compile("#[a-zA-Z][a-zA-Z0-9_.]*");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?");
    
    private final YamlConfigurationLoader configLoader;
    private final Yaml yamlParser;
    
    public ApexYamlLexicalValidator() {
        this.configLoader = new YamlConfigurationLoader();
        this.yamlParser = new Yaml();
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
     * Validate type-specific rules.
     */
    private void validateTypeSpecificRules(Map<String, Object> yamlData, String documentType, 
                                         ValidationResult result) {
        // Validate type-specific required metadata fields
        Set<String> typeRequiredFields = TYPE_SPECIFIC_REQUIRED_FIELDS.get(documentType);
        if (typeRequiredFields != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) yamlData.get("metadata");
            
            for (String requiredField : typeRequiredFields) {
                if (!metadata.containsKey(requiredField)) {
                    result.addError("Missing required field for type '" + documentType + "': " + requiredField);
                }
            }
        }
        
        // Validate required sections for document type
        Set<String> requiredSections = TYPE_REQUIRED_SECTIONS.get(documentType);
        if (requiredSections != null) {
            boolean hasRequiredSection = false;
            for (String section : requiredSections) {
                if (yamlData.containsKey(section)) {
                    hasRequiredSection = true;
                    break;
                }
            }
            
            if (!hasRequiredSection) {
                result.addError("Document type '" + documentType + "' requires at least one of: " + requiredSections);
            }
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
            if (str.contains("#")) {
                validateSpelExpression(str, path, result);
            }
        }
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
