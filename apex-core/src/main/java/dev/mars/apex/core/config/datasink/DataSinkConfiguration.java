package dev.mars.apex.core.config.datasink;

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

import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.config.datasource.AuthenticationConfig;
import dev.mars.apex.core.config.datasource.CircuitBreakerConfig;
import dev.mars.apex.core.service.data.external.DataSinkType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Main configuration class for external data sinks.
 * 
 * This class contains all the configuration settings needed to initialize
 * and manage an external data sink, including connection settings,
 * operation definitions, error handling, and performance tuning.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSinkConfiguration {
    
    private String name;
    private String type;
    private String sourceType;
    private String description;
    private boolean enabled = true;
    private String implementation;
    
    // Reuse existing configuration classes from data sources
    private ConnectionConfig connection;
    private CacheConfig cache;
    private HealthCheckConfig healthCheck;
    private AuthenticationConfig authentication;
    private CircuitBreakerConfig circuitBreaker;
    
    // Data sink specific configurations
    private Map<String, String> operations;
    private OutputFormatConfig outputFormat;
    private ErrorHandlingConfig errorHandling;
    private BatchConfig batch;
    private SchemaConfig schema;
    private RetryConfig retry;
    
    // Custom properties for extensibility
    private Map<String, Object> customProperties;
    
    // Parameter configuration
    private String[] parameterNames;

    // Tags for categorization and discovery
    private List<String> tags;
    
    /**
     * Default constructor.
     */
    public DataSinkConfiguration() {
        this.operations = new HashMap<>();
        this.customProperties = new HashMap<>();
    }
    
    /**
     * Constructor with basic configuration.
     * 
     * @param name The name of the data sink
     * @param type The type of data sink
     */
    public DataSinkConfiguration(String name, String type) {
        this();
        this.name = name;
        this.type = type;
    }
    
    // Basic properties
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public DataSinkType getSinkType() {
        return DataSinkType.fromCode(type);
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getImplementation() {
        return implementation;
    }
    
    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }
    
    // Configuration objects (reusing existing classes)
    
    public ConnectionConfig getConnection() {
        return connection;
    }
    
    public void setConnection(ConnectionConfig connection) {
        this.connection = connection;
    }
    
    public CacheConfig getCache() {
        return cache;
    }
    
    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }
    
    public HealthCheckConfig getHealthCheck() {
        return healthCheck;
    }
    
    public void setHealthCheck(HealthCheckConfig healthCheck) {
        this.healthCheck = healthCheck;
    }
    
    public AuthenticationConfig getAuthentication() {
        return authentication;
    }
    
    public void setAuthentication(AuthenticationConfig authentication) {
        this.authentication = authentication;
    }
    
    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }
    
    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    // Data sink specific configurations
    
    public Map<String, String> getOperations() {
        return operations;
    }
    
    public void setOperations(Map<String, String> operations) {
        this.operations = operations != null ? operations : new HashMap<>();
    }
    
    public OutputFormatConfig getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(OutputFormatConfig outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    public ErrorHandlingConfig getErrorHandling() {
        return errorHandling;
    }
    
    public void setErrorHandling(ErrorHandlingConfig errorHandling) {
        this.errorHandling = errorHandling;
    }
    
    public BatchConfig getBatch() {
        return batch;
    }
    
    public void setBatch(BatchConfig batch) {
        this.batch = batch;
    }
    
    public SchemaConfig getSchema() {
        return schema;
    }
    
    public void setSchema(SchemaConfig schema) {
        this.schema = schema;
    }
    
    public RetryConfig getRetry() {
        return retry;
    }
    
    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }
    
    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }
    
    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties != null ? customProperties : new HashMap<>();
    }
    
    public String[] getParameterNames() {
        return parameterNames;
    }
    
    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    // Utility methods
    
    /**
     * Add an operation to the configuration.
     * 
     * @param name The operation name
     * @param definition The operation definition (SQL, template, etc.)
     */
    public void addOperation(String name, String definition) {
        if (operations == null) {
            operations = new HashMap<>();
        }
        operations.put(name, definition);
    }
    
    /**
     * Get an operation definition by name.
     * 
     * @param name The operation name
     * @return The operation definition, or null if not found
     */
    public String getOperation(String name) {
        return operations != null ? operations.get(name) : null;
    }
    
    /**
     * Check if an operation is defined.
     * 
     * @param name The operation name
     * @return true if the operation is defined, false otherwise
     */
    public boolean hasOperation(String name) {
        return operations != null && operations.containsKey(name);
    }
    
    /**
     * Add a custom property.
     * 
     * @param key The property key
     * @param value The property value
     */
    public void addCustomProperty(String key, Object value) {
        if (customProperties == null) {
            customProperties = new HashMap<>();
        }
        customProperties.put(key, value);
    }
    
    /**
     * Get a custom property value.
     * 
     * @param key The property key
     * @return The property value, or null if not found
     */
    public Object getCustomProperty(String key) {
        return customProperties != null ? customProperties.get(key) : null;
    }
    
    /**
     * Validate the configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Data sink name is required");
        }
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Data sink type is required");
        }
        
        DataSinkType sinkType = getSinkType();
        if (sinkType == null) {
            throw new IllegalArgumentException("Invalid data sink type: " + type);
        }
        
        if (connection != null) {
            connection.validate();
        }
        
        if (healthCheck != null) {
            healthCheck.validate();
        }
        
        if (errorHandling != null) {
            errorHandling.validate();
        }
        
        if (batch != null) {
            batch.validate();
        }
        
        if (schema != null) {
            schema.validate();
        }
        
        if (retry != null) {
            retry.validate();
        }
    }
    
    /**
     * Create a copy of this configuration.
     * 
     * @return A new DataSinkConfiguration with the same settings
     */
    public DataSinkConfiguration copy() {
        DataSinkConfiguration copy = new DataSinkConfiguration();
        copy.name = this.name;
        copy.type = this.type;
        copy.sourceType = this.sourceType;
        copy.description = this.description;
        copy.enabled = this.enabled;
        copy.implementation = this.implementation;
        
        copy.connection = this.connection != null ? this.connection.copy() : null;
        copy.cache = this.cache != null ? this.cache.copy() : null;
        copy.healthCheck = this.healthCheck != null ? this.healthCheck.copy() : null;
        copy.authentication = this.authentication != null ? this.authentication.copy() : null;
        copy.circuitBreaker = this.circuitBreaker != null ? this.circuitBreaker.copy() : null;
        
        copy.operations = new HashMap<>(this.operations);
        copy.outputFormat = this.outputFormat != null ? this.outputFormat.copy() : null;
        copy.errorHandling = this.errorHandling != null ? this.errorHandling.copy() : null;
        copy.batch = this.batch != null ? this.batch.copy() : null;
        copy.schema = this.schema != null ? this.schema.copy() : null;
        copy.retry = this.retry != null ? this.retry.copy() : null;
        
        copy.customProperties = new HashMap<>(this.customProperties);
        copy.parameterNames = this.parameterNames != null ? this.parameterNames.clone() : null;
        copy.tags = this.tags != null ? List.copyOf(this.tags) : null;
        
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSinkConfiguration that = (DataSinkConfiguration) o;
        return enabled == that.enabled &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type) &&
               Objects.equals(sourceType, that.sourceType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type, sourceType, enabled);
    }
    
    @Override
    public String toString() {
        return String.format("DataSinkConfiguration[name=%s, type=%s, sourceType=%s, enabled=%s]",
                           name, type, sourceType, enabled);
    }
}
