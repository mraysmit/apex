package dev.mars.apex.core.config.datasource;

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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for response mapping settings.
 * 
 * This class contains configuration for mapping and transforming responses
 * from external data sources, particularly useful for REST APIs and
 * structured data formats.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class ResponseMappingConfig {
    
    /**
     * Enumeration of response formats.
     */
    public enum ResponseFormat {
        JSON("json", "JavaScript Object Notation"),
        XML("xml", "Extensible Markup Language"),
        TEXT("text", "Plain text"),
        CSV("csv", "Comma-separated values"),
        CUSTOM("custom", "Custom format");
        
        private final String code;
        private final String description;
        
        ResponseFormat(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static ResponseFormat fromCode(String code) {
            if (code == null) {
                return JSON; // Default
            }
            
            for (ResponseFormat format : values()) {
                if (format.code.equalsIgnoreCase(code)) {
                    return format;
                }
            }
            
            return JSON;
        }
    }
    
    private String format = "json";
    private String rootPath = "$"; // JSONPath or XPath expression
    private String errorPath = "$.error"; // Path to error information
    private String dataPath = "$.data"; // Path to actual data
    private String statusPath = "$.status"; // Path to status information
    private String messagePath = "$.message"; // Path to message information
    
    // Field mappings
    private Map<String, String> fieldMappings; // source field -> target field
    private Map<String, String> fieldTypes; // field -> type (string, number, date, etc.)
    private Map<String, String> fieldFormats; // field -> format (for dates, numbers, etc.)
    private Map<String, Object> defaultValues; // field -> default value
    
    // Transformation settings
    private Boolean flattenNestedObjects = false;
    private String arrayHandling = "first"; // first, last, all, concat
    private String nullHandling = "keep"; // keep, remove, default
    private Boolean trimStrings = true;
    private Boolean convertEmptyToNull = true;
    
    // Filtering and validation
    private List<String> includeFields; // Only include these fields
    private List<String> excludeFields; // Exclude these fields
    private Map<String, String> fieldValidations; // field -> validation expression
    
    // Custom transformation
    private String customTransformer; // Custom transformer class
    private Map<String, Object> transformerProperties;
    
    /**
     * Default constructor.
     */
    public ResponseMappingConfig() {
        this.fieldMappings = new HashMap<>();
        this.fieldTypes = new HashMap<>();
        this.fieldFormats = new HashMap<>();
        this.defaultValues = new HashMap<>();
        this.fieldValidations = new HashMap<>();
        this.transformerProperties = new HashMap<>();
    }
    
    /**
     * Constructor with response format.
     * 
     * @param format The response format
     */
    public ResponseMappingConfig(String format) {
        this();
        this.format = format;
    }
    
    // Basic configuration
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public ResponseFormat getResponseFormat() {
        return ResponseFormat.fromCode(format);
    }
    
    public String getRootPath() {
        return rootPath;
    }
    
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
    
    public String getErrorPath() {
        return errorPath;
    }
    
    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }
    
    public String getDataPath() {
        return dataPath;
    }
    
    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }
    
    public String getStatusPath() {
        return statusPath;
    }
    
    public void setStatusPath(String statusPath) {
        this.statusPath = statusPath;
    }
    
    public String getMessagePath() {
        return messagePath;
    }
    
    public void setMessagePath(String messagePath) {
        this.messagePath = messagePath;
    }
    
    // Field mappings
    
    public Map<String, String> getFieldMappings() {
        return fieldMappings;
    }
    
    public void setFieldMappings(Map<String, String> fieldMappings) {
        this.fieldMappings = fieldMappings != null ? fieldMappings : new HashMap<>();
    }
    
    public void addFieldMapping(String sourceField, String targetField) {
        this.fieldMappings.put(sourceField, targetField);
    }
    
    public Map<String, String> getFieldTypes() {
        return fieldTypes;
    }
    
    public void setFieldTypes(Map<String, String> fieldTypes) {
        this.fieldTypes = fieldTypes != null ? fieldTypes : new HashMap<>();
    }
    
    public void addFieldType(String field, String type) {
        this.fieldTypes.put(field, type);
    }
    
    public Map<String, String> getFieldFormats() {
        return fieldFormats;
    }
    
    public void setFieldFormats(Map<String, String> fieldFormats) {
        this.fieldFormats = fieldFormats != null ? fieldFormats : new HashMap<>();
    }
    
    public void addFieldFormat(String field, String format) {
        this.fieldFormats.put(field, format);
    }
    
    public Map<String, Object> getDefaultValues() {
        return defaultValues;
    }
    
    public void setDefaultValues(Map<String, Object> defaultValues) {
        this.defaultValues = defaultValues != null ? defaultValues : new HashMap<>();
    }
    
    public void addDefaultValue(String field, Object value) {
        this.defaultValues.put(field, value);
    }
    
    // Transformation settings
    
    public Boolean getFlattenNestedObjects() {
        return flattenNestedObjects;
    }
    
    public void setFlattenNestedObjects(Boolean flattenNestedObjects) {
        this.flattenNestedObjects = flattenNestedObjects;
    }
    
    public boolean shouldFlattenNestedObjects() {
        return flattenNestedObjects != null && flattenNestedObjects;
    }
    
    public String getArrayHandling() {
        return arrayHandling;
    }
    
    public void setArrayHandling(String arrayHandling) {
        this.arrayHandling = arrayHandling;
    }
    
    public String getNullHandling() {
        return nullHandling;
    }
    
    public void setNullHandling(String nullHandling) {
        this.nullHandling = nullHandling;
    }
    
    public Boolean getTrimStrings() {
        return trimStrings;
    }
    
    public void setTrimStrings(Boolean trimStrings) {
        this.trimStrings = trimStrings;
    }
    
    public boolean shouldTrimStrings() {
        return trimStrings != null && trimStrings;
    }
    
    public Boolean getConvertEmptyToNull() {
        return convertEmptyToNull;
    }
    
    public void setConvertEmptyToNull(Boolean convertEmptyToNull) {
        this.convertEmptyToNull = convertEmptyToNull;
    }
    
    public boolean shouldConvertEmptyToNull() {
        return convertEmptyToNull != null && convertEmptyToNull;
    }
    
    // Filtering and validation
    
    public List<String> getIncludeFields() {
        return includeFields;
    }
    
    public void setIncludeFields(List<String> includeFields) {
        this.includeFields = includeFields;
    }
    
    public List<String> getExcludeFields() {
        return excludeFields;
    }
    
    public void setExcludeFields(List<String> excludeFields) {
        this.excludeFields = excludeFields;
    }
    
    public Map<String, String> getFieldValidations() {
        return fieldValidations;
    }
    
    public void setFieldValidations(Map<String, String> fieldValidations) {
        this.fieldValidations = fieldValidations != null ? fieldValidations : new HashMap<>();
    }
    
    public void addFieldValidation(String field, String validation) {
        this.fieldValidations.put(field, validation);
    }
    
    // Custom transformation
    
    public String getCustomTransformer() {
        return customTransformer;
    }
    
    public void setCustomTransformer(String customTransformer) {
        this.customTransformer = customTransformer;
    }
    
    public Map<String, Object> getTransformerProperties() {
        return transformerProperties;
    }
    
    public void setTransformerProperties(Map<String, Object> transformerProperties) {
        this.transformerProperties = transformerProperties != null ? transformerProperties : new HashMap<>();
    }
    
    public void addTransformerProperty(String key, Object value) {
        this.transformerProperties.put(key, value);
    }
    
    // Utility methods
    
    /**
     * Check if field filtering is enabled.
     * 
     * @return true if include or exclude fields are specified
     */
    public boolean isFieldFilteringEnabled() {
        return (includeFields != null && !includeFields.isEmpty()) ||
               (excludeFields != null && !excludeFields.isEmpty());
    }
    
    /**
     * Check if field validation is enabled.
     * 
     * @return true if field validations are specified
     */
    public boolean isFieldValidationEnabled() {
        return fieldValidations != null && !fieldValidations.isEmpty();
    }
    
    /**
     * Check if custom transformation is enabled.
     * 
     * @return true if custom transformer is specified
     */
    public boolean isCustomTransformationEnabled() {
        return customTransformer != null && !customTransformer.trim().isEmpty();
    }
    
    // Validation
    
    /**
     * Validate the response mapping configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Response format is required");
        }
        
        if (arrayHandling != null && 
            !arrayHandling.matches("first|last|all|concat")) {
            throw new IllegalArgumentException("Array handling must be one of: first, last, all, concat");
        }
        
        if (nullHandling != null && 
            !nullHandling.matches("keep|remove|default")) {
            throw new IllegalArgumentException("Null handling must be one of: keep, remove, default");
        }
        
        if (isCustomTransformationEnabled()) {
            try {
                Class.forName(customTransformer);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Custom transformer class not found: " + customTransformer);
            }
        }
    }
    
    /**
     * Create a copy of this response mapping configuration.
     * 
     * @return A new ResponseMappingConfig with the same settings
     */
    public ResponseMappingConfig copy() {
        ResponseMappingConfig copy = new ResponseMappingConfig();
        copy.format = this.format;
        copy.rootPath = this.rootPath;
        copy.errorPath = this.errorPath;
        copy.dataPath = this.dataPath;
        copy.statusPath = this.statusPath;
        copy.messagePath = this.messagePath;
        copy.fieldMappings = new HashMap<>(this.fieldMappings);
        copy.fieldTypes = new HashMap<>(this.fieldTypes);
        copy.fieldFormats = new HashMap<>(this.fieldFormats);
        copy.defaultValues = new HashMap<>(this.defaultValues);
        copy.flattenNestedObjects = this.flattenNestedObjects;
        copy.arrayHandling = this.arrayHandling;
        copy.nullHandling = this.nullHandling;
        copy.trimStrings = this.trimStrings;
        copy.convertEmptyToNull = this.convertEmptyToNull;
        copy.includeFields = this.includeFields; // Shallow copy
        copy.excludeFields = this.excludeFields; // Shallow copy
        copy.fieldValidations = new HashMap<>(this.fieldValidations);
        copy.customTransformer = this.customTransformer;
        copy.transformerProperties = new HashMap<>(this.transformerProperties);
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseMappingConfig that = (ResponseMappingConfig) o;
        return Objects.equals(format, that.format) &&
               Objects.equals(rootPath, that.rootPath) &&
               Objects.equals(dataPath, that.dataPath) &&
               Objects.equals(fieldMappings, that.fieldMappings);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(format, rootPath, dataPath, fieldMappings);
    }
    
    @Override
    public String toString() {
        return "ResponseMappingConfig{" +
               "format='" + format + '\'' +
               ", rootPath='" + rootPath + '\'' +
               ", dataPath='" + dataPath + '\'' +
               ", fieldMappings=" + fieldMappings.size() +
               ", flattenNestedObjects=" + flattenNestedObjects +
               '}';
    }
}
