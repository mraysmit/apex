package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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

/**
 * YAML representation of a rule configuration.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * YAML representation of a rule configuration.
 * This class maps to the YAML structure for defining individual rules.
 */
public class YamlRule {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("categories")
    private List<String> categories;
    
    @JsonProperty("condition")
    private String condition;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("priority")
    private Integer priority;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("validation")
    private ValidationConfig validation;

    // Enterprise metadata fields for audit trails and governance
    @JsonProperty("created-by")
    private String createdBy;

    @JsonProperty("business-domain")
    private String businessDomain;

    @JsonProperty("business-owner")
    private String businessOwner;

    @JsonProperty("source-system")
    private String sourceSystem;

    @JsonProperty("effective-date")
    private String effectiveDate;  // ISO 8601 format string

    @JsonProperty("expiration-date")
    private String expirationDate;  // ISO 8601 format string

    @JsonProperty("custom-properties")
    private Map<String, Object> customProperties;

    // Phase 3A Enhancement: Default value for error recovery
    @JsonProperty("default-value")
    private Object defaultValue;

    // Phase 4: Error and Success Code Support
    @JsonProperty("success-code")
    private String successCode;

    @JsonProperty("error-code")
    private String errorCode;

    @JsonProperty("map-to-field")
    private Object mapToField;  // String or List<String>

    // Default constructor
    public YamlRule() {
        this.enabled = true; // Default to enabled
        this.priority = 100; // Default priority
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public List<String> getCategories() {
        return categories;
    }
    
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public ValidationConfig getValidation() {
        return validation;
    }
    
    public void setValidation(ValidationConfig validation) {
        this.validation = validation;
    }

    // Enterprise metadata getters and setters

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public void setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
    }

    public String getBusinessOwner() {
        return businessOwner;
    }

    public void setBusinessOwner(String businessOwner) {
        this.businessOwner = businessOwner;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getSuccessCode() {
        return successCode;
    }

    public void setSuccessCode(String successCode) {
        this.successCode = successCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Object getMapToField() {
        return mapToField;
    }

    public void setMapToField(Object mapToField) {
        this.mapToField = mapToField;
    }

    /**
     * Validation configuration for the rule.
     */
    public static class ValidationConfig {
        @JsonProperty("required-fields")
        private List<String> requiredFields;
        
        @JsonProperty("field-types")
        private Map<String, String> fieldTypes;
        
        @JsonProperty("custom-validators")
        private List<String> customValidators;
        
        // Default constructor
        public ValidationConfig() {}
        
        // Getters and setters
        public List<String> getRequiredFields() {
            return requiredFields;
        }
        
        public void setRequiredFields(List<String> requiredFields) {
            this.requiredFields = requiredFields;
        }
        
        public Map<String, String> getFieldTypes() {
            return fieldTypes;
        }
        
        public void setFieldTypes(Map<String, String> fieldTypes) {
            this.fieldTypes = fieldTypes;
        }
        
        public List<String> getCustomValidators() {
            return customValidators;
        }
        
        public void setCustomValidators(List<String> customValidators) {
            this.customValidators = customValidators;
        }
    }
}
