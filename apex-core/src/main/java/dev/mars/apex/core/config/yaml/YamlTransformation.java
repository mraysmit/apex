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
 * YAML representation of a transformation configuration.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * YAML representation of a transformation configuration.
 * This class maps to the YAML structure for defining data transformations.
 */
public class YamlTransformation {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("type")
    private String type; // e.g., "field-transformation", "object-transformation", "conditional-transformation"
    
    @JsonProperty("target-type")
    private String targetType; // The class/type this transformation applies to
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("priority")
    private Integer priority;
    
    @JsonProperty("condition")
    private String condition; // SpEL condition for when to apply this transformation
    
    @JsonProperty("transformation-rules")
    private List<TransformationRule> transformationRules;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Default constructor
    public YamlTransformation() {
        this.enabled = true;
        this.priority = 100;
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
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getTargetType() {
        return targetType;
    }
    
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public List<TransformationRule> getTransformationRules() {
        return transformationRules;
    }
    
    public void setTransformationRules(List<TransformationRule> transformationRules) {
        this.transformationRules = transformationRules;
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
    
    /**
     * Individual transformation rule within a transformation.
     */
    public static class TransformationRule {
        @JsonProperty("condition")
        private String condition; // When to apply this specific transformation rule
        
        @JsonProperty("actions")
        private List<TransformationAction> actions;
        
        @JsonProperty("else-actions")
        private List<TransformationAction> elseActions;
        
        // Default constructor
        public TransformationRule() {}
        
        // Getters and setters
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
        }
        
        public List<TransformationAction> getActions() {
            return actions;
        }
        
        public void setActions(List<TransformationAction> actions) {
            this.actions = actions;
        }
        
        public List<TransformationAction> getElseActions() {
            return elseActions;
        }
        
        public void setElseActions(List<TransformationAction> elseActions) {
            this.elseActions = elseActions;
        }
    }
    
    /**
     * Individual transformation action.
     */
    public static class TransformationAction {
        @JsonProperty("type")
        private String type; // e.g., "set-field", "calculate-field", "copy-field", "remove-field"
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("source-field")
        private String sourceField;
        
        @JsonProperty("expression")
        private String expression; // SpEL expression for the transformation
        
        @JsonProperty("value")
        private Object value; // Static value to set
        
        @JsonProperty("parameters")
        private Map<String, Object> parameters;
        
        // Default constructor
        public TransformationAction() {}
        
        // Getters and setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public String getSourceField() {
            return sourceField;
        }
        
        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }
        
        public String getExpression() {
            return expression;
        }
        
        public void setExpression(String expression) {
            this.expression = expression;
        }
        
        public Object getValue() {
            return value;
        }
        
        public void setValue(Object value) {
            this.value = value;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}
