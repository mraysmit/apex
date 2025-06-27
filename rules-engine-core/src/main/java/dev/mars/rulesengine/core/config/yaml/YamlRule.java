package dev.mars.rulesengine.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("validation")
    private ValidationConfig validation;
    
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
