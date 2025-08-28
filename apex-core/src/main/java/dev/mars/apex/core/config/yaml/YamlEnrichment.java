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
 * YAML representation of an enrichment configuration.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * YAML representation of an enrichment configuration.
 * This class maps to the YAML structure for defining data enrichments.
 */
public class YamlEnrichment {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("type")
    private String type; // e.g., "field-enrichment", "lookup-enrichment", "calculation-enrichment"
    
    @JsonProperty("target-type")
    private String targetType; // The class/type this enrichment applies to
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("priority")
    private Integer priority;
    
    @JsonProperty("condition")
    private String condition; // SpEL condition for when to apply this enrichment
    
    @JsonProperty("field-mappings")
    private List<FieldMapping> fieldMappings;
    
    @JsonProperty("lookup-config")
    private LookupConfig lookupConfig;
    
    @JsonProperty("calculation-config")
    private CalculationConfig calculationConfig;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Default constructor
    public YamlEnrichment() {
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
    
    public List<FieldMapping> getFieldMappings() {
        return fieldMappings;
    }
    
    public void setFieldMappings(List<FieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }
    
    public LookupConfig getLookupConfig() {
        return lookupConfig;
    }
    
    public void setLookupConfig(LookupConfig lookupConfig) {
        this.lookupConfig = lookupConfig;
    }
    
    public CalculationConfig getCalculationConfig() {
        return calculationConfig;
    }
    
    public void setCalculationConfig(CalculationConfig calculationConfig) {
        this.calculationConfig = calculationConfig;
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
     * Field mapping configuration for enrichments.
     */
    public static class FieldMapping {
        @JsonProperty("source-field")
        private String sourceField;
        
        @JsonProperty("target-field")
        private String targetField;
        
        @JsonProperty("transformation")
        private String transformation; // SpEL expression for field transformation
        
        @JsonProperty("default-value")
        private Object defaultValue;
        
        @JsonProperty("required")
        private Boolean required;
        
        // Default constructor
        public FieldMapping() {
            this.required = false;
        }
        
        // Getters and setters
        public String getSourceField() {
            return sourceField;
        }
        
        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }
        
        public String getTargetField() {
            return targetField;
        }
        
        public void setTargetField(String targetField) {
            this.targetField = targetField;
        }
        
        public String getTransformation() {
            return transformation;
        }
        
        public void setTransformation(String transformation) {
            this.transformation = transformation;
        }
        
        public Object getDefaultValue() {
            return defaultValue;
        }
        
        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public Boolean getRequired() {
            return required;
        }
        
        public void setRequired(Boolean required) {
            this.required = required;
        }
    }
    
    /**
     * Lookup configuration for enrichments.
     */
    public static class LookupConfig {
        @JsonProperty("lookup-service")
        private String lookupService;

        @JsonProperty("lookup-dataset")
        private LookupDataset lookupDataset;

        @JsonProperty("lookup-key")
        private String lookupKey; // SpEL expression to extract lookup key

        @JsonProperty("cache-enabled")
        private Boolean cacheEnabled;

        @JsonProperty("cache-ttl-seconds")
        private Integer cacheTtlSeconds;
        
        // Default constructor
        public LookupConfig() {
            this.cacheEnabled = true;
            this.cacheTtlSeconds = 300; // 5 minutes default
        }
        
        // Getters and setters
        public String getLookupService() {
            return lookupService;
        }
        
        public void setLookupService(String lookupService) {
            this.lookupService = lookupService;
        }
        
        public String getLookupKey() {
            return lookupKey;
        }
        
        public void setLookupKey(String lookupKey) {
            this.lookupKey = lookupKey;
        }
        
        public Boolean getCacheEnabled() {
            return cacheEnabled;
        }
        
        public void setCacheEnabled(Boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }
        
        public Integer getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }
        
        public void setCacheTtlSeconds(Integer cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }

        public LookupDataset getLookupDataset() {
            return lookupDataset;
        }

        public void setLookupDataset(LookupDataset lookupDataset) {
            this.lookupDataset = lookupDataset;
        }
    }
    
    /**
     * Calculation configuration for enrichments.
     */
    public static class CalculationConfig {
        @JsonProperty("expression")
        private String expression; // SpEL expression for calculation
        
        @JsonProperty("result-field")
        private String resultField;
        
        @JsonProperty("dependencies")
        private List<String> dependencies; // Fields this calculation depends on
        
        // Default constructor
        public CalculationConfig() {}
        
        // Getters and setters
        public String getExpression() {
            return expression;
        }
        
        public void setExpression(String expression) {
            this.expression = expression;
        }
        
        public String getResultField() {
            return resultField;
        }
        
        public void setResultField(String resultField) {
            this.resultField = resultField;
        }
        
        public List<String> getDependencies() {
            return dependencies;
        }
        
        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }
    }

    /**
     * Dataset configuration for lookup enrichments.
     * Supports inline datasets, YAML files, CSV files, and database lookups.
     */
    public static class LookupDataset {
        @JsonProperty("type")
        private String type; // "inline", "yaml-file", "csv-file", "database"

        @JsonProperty("file-path")
        private String filePath; // For file-based datasets

        @JsonProperty("key-field")
        private String keyField; // Field to use as lookup key

        @JsonProperty("data")
        private List<Map<String, Object>> data; // For inline datasets

        @JsonProperty("default-values")
        private Map<String, Object> defaultValues; // Default values for missing fields

        @JsonProperty("cache-enabled")
        private Boolean cacheEnabled;

        @JsonProperty("cache-ttl-seconds")
        private Integer cacheTtlSeconds;

        // Database-specific fields
        @JsonProperty("connection-name")
        private String connectionName; // Reference to data source in dataSources section

        @JsonProperty("query")
        private String query; // SQL query to execute

        @JsonProperty("parameters")
        private List<ParameterMapping> parameters; // Parameter mappings for the query

        /**
         * Parameter mapping for database queries.
         */
        public static class ParameterMapping {
            @JsonProperty("field")
            private String field; // Field name from input data

            @JsonProperty("type")
            private String type; // Parameter type (string, integer, etc.)

            @JsonProperty("name")
            private String name; // Parameter name in query (optional, defaults to field)

            public ParameterMapping() {}

            public String getField() { return field; }
            public void setField(String field) { this.field = field; }

            public String getType() { return type; }
            public void setType(String type) { this.type = type; }

            public String getName() { return name != null ? name : field; }
            public void setName(String name) { this.name = name; }
        }

        // Default constructor
        public LookupDataset() {
            this.cacheEnabled = true;
            this.cacheTtlSeconds = 3600; // 1 hour default for datasets
        }

        // Getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getKeyField() {
            return keyField;
        }

        public void setKeyField(String keyField) {
            this.keyField = keyField;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }

        public void setData(List<Map<String, Object>> data) {
            this.data = data;
        }

        public Map<String, Object> getDefaultValues() {
            return defaultValues;
        }

        public void setDefaultValues(Map<String, Object> defaultValues) {
            this.defaultValues = defaultValues;
        }

        public Boolean getCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(Boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public Integer getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(Integer cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }

        // Database-specific getters and setters
        public String getConnectionName() {
            return connectionName;
        }

        public void setConnectionName(String connectionName) {
            this.connectionName = connectionName;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public List<ParameterMapping> getParameters() {
            return parameters;
        }

        public void setParameters(List<ParameterMapping> parameters) {
            this.parameters = parameters;
        }
    }
}
