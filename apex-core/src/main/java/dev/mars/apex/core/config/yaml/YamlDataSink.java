package dev.mars.apex.core.config.yaml;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.mars.apex.core.config.datasink.DataSinkConfiguration;
import dev.mars.apex.core.config.datasink.OutputFormatConfig;
import dev.mars.apex.core.config.datasink.ErrorHandlingConfig;
import dev.mars.apex.core.config.datasink.BatchConfig;
import dev.mars.apex.core.config.datasink.SchemaConfig;
import dev.mars.apex.core.config.datasink.RetryConfig;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.HealthCheckConfig;
import dev.mars.apex.core.config.datasource.AuthenticationConfig;
import dev.mars.apex.core.config.datasource.CircuitBreakerConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML configuration class for data sinks.
 * 
 * This class represents the YAML configuration structure for data sinks,
 * following APEX YAML conventions and patterns. It mirrors the structure
 * of YamlDataSource but for output destinations.
 * 
 * APEX YAML Syntax:
 * - Uses kebab-case for property names (data-sinks, source-type, etc.)
 * - Follows the same patterns as existing data-sources configuration
 * - Supports all standard APEX configuration sections
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class YamlDataSink {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("source-type")
    private String sourceType;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("implementation")
    private String implementation;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    // Connection configuration (reusing existing pattern)
    @JsonProperty("connection")
    private Map<String, Object> connection;
    
    // Cache configuration (reusing existing pattern)
    @JsonProperty("cache")
    private Map<String, Object> cache;
    
    // Health check configuration (reusing existing pattern)
    @JsonProperty("health-check")
    private Map<String, Object> healthCheck;
    
    // Authentication configuration (reusing existing pattern)
    @JsonProperty("authentication")
    private Map<String, Object> authentication;
    
    // Circuit breaker configuration (reusing existing pattern)
    @JsonProperty("circuit-breaker")
    private Map<String, Object> circuitBreaker;
    
    // Data sink specific configurations
    @JsonProperty("operations")
    private Map<String, String> operations;
    
    @JsonProperty("output-format")
    private Map<String, Object> outputFormat;
    
    @JsonProperty("error-handling")
    private Map<String, Object> errorHandling;
    
    @JsonProperty("batch")
    private Map<String, Object> batch;
    
    @JsonProperty("schema")
    private Map<String, Object> schema;
    
    @JsonProperty("retry")
    private Map<String, Object> retry;
    
    // Custom properties for extensibility
    @JsonProperty("custom-properties")
    private Map<String, Object> customProperties;
    
    // Parameter configuration
    @JsonProperty("parameter-names")
    private String[] parameterNames;
    
    /**
     * Default constructor.
     */
    public YamlDataSink() {
        this.connection = new HashMap<>();
        this.cache = new HashMap<>();
        this.healthCheck = new HashMap<>();
        this.authentication = new HashMap<>();
        this.circuitBreaker = new HashMap<>();
        this.operations = new HashMap<>();
        this.outputFormat = new HashMap<>();
        this.errorHandling = new HashMap<>();
        this.batch = new HashMap<>();
        this.schema = new HashMap<>();
        this.retry = new HashMap<>();
        this.customProperties = new HashMap<>();
    }
    
    // Getters and setters following APEX patterns
    
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
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getImplementation() {
        return implementation;
    }
    
    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getConnection() {
        return connection;
    }
    
    public void setConnection(Map<String, Object> connection) {
        this.connection = connection != null ? connection : new HashMap<>();
    }
    
    public Map<String, Object> getCache() {
        return cache;
    }
    
    public void setCache(Map<String, Object> cache) {
        this.cache = cache != null ? cache : new HashMap<>();
    }
    
    public Map<String, Object> getHealthCheck() {
        return healthCheck;
    }
    
    public void setHealthCheck(Map<String, Object> healthCheck) {
        this.healthCheck = healthCheck != null ? healthCheck : new HashMap<>();
    }
    
    public Map<String, Object> getAuthentication() {
        return authentication;
    }
    
    public void setAuthentication(Map<String, Object> authentication) {
        this.authentication = authentication != null ? authentication : new HashMap<>();
    }
    
    public Map<String, Object> getCircuitBreaker() {
        return circuitBreaker;
    }
    
    public void setCircuitBreaker(Map<String, Object> circuitBreaker) {
        this.circuitBreaker = circuitBreaker != null ? circuitBreaker : new HashMap<>();
    }
    
    public Map<String, String> getOperations() {
        return operations;
    }
    
    public void setOperations(Map<String, String> operations) {
        this.operations = operations != null ? operations : new HashMap<>();
    }
    
    public Map<String, Object> getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(Map<String, Object> outputFormat) {
        this.outputFormat = outputFormat != null ? outputFormat : new HashMap<>();
    }
    
    public Map<String, Object> getErrorHandling() {
        return errorHandling;
    }
    
    public void setErrorHandling(Map<String, Object> errorHandling) {
        this.errorHandling = errorHandling != null ? errorHandling : new HashMap<>();
    }
    
    public Map<String, Object> getBatch() {
        return batch;
    }
    
    public void setBatch(Map<String, Object> batch) {
        this.batch = batch != null ? batch : new HashMap<>();
    }
    
    public Map<String, Object> getSchema() {
        return schema;
    }
    
    public void setSchema(Map<String, Object> schema) {
        this.schema = schema != null ? schema : new HashMap<>();
    }
    
    public Map<String, Object> getRetry() {
        return retry;
    }
    
    public void setRetry(Map<String, Object> retry) {
        this.retry = retry != null ? retry : new HashMap<>();
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
    
    /**
     * Convert this YAML data sink configuration to a DataSinkConfiguration object.
     * This method follows the same pattern as YamlDataSource.toDataSourceConfiguration().
     * 
     * @return DataSinkConfiguration object
     */
    public DataSinkConfiguration toDataSinkConfiguration() {
        DataSinkConfiguration config = new DataSinkConfiguration(name, type);
        config.setSourceType(sourceType);
        config.setDescription(description);
        config.setEnabled(enabled != null ? enabled : true);
        config.setImplementation(implementation);
        
        // Convert connection configuration (reusing existing conversion logic)
        if (connection != null && !connection.isEmpty()) {
            config.setConnection(convertToConnectionConfig(connection));
        }
        
        // Convert cache configuration (reusing existing conversion logic)
        if (cache != null && !cache.isEmpty()) {
            config.setCache(convertToCacheConfig(cache));
        }
        
        // Convert health check configuration (reusing existing conversion logic)
        if (healthCheck != null && !healthCheck.isEmpty()) {
            config.setHealthCheck(convertToHealthCheckConfig(healthCheck));
        }
        
        // Convert authentication configuration (reusing existing conversion logic)
        if (authentication != null && !authentication.isEmpty()) {
            config.setAuthentication(convertToAuthenticationConfig(authentication));
        }
        
        // Convert circuit breaker configuration (reusing existing conversion logic)
        if (circuitBreaker != null && !circuitBreaker.isEmpty()) {
            config.setCircuitBreaker(convertToCircuitBreakerConfig(circuitBreaker));
        }
        
        // Convert data sink specific configurations
        if (operations != null && !operations.isEmpty()) {
            config.setOperations(new HashMap<>(operations));
        }
        
        if (outputFormat != null && !outputFormat.isEmpty()) {
            config.setOutputFormat(convertToOutputFormatConfig(outputFormat));
        }
        
        if (errorHandling != null && !errorHandling.isEmpty()) {
            config.setErrorHandling(convertToErrorHandlingConfig(errorHandling));
        }
        
        if (batch != null && !batch.isEmpty()) {
            config.setBatch(convertToBatchConfig(batch));
        }
        
        if (schema != null && !schema.isEmpty()) {
            config.setSchema(convertToSchemaConfig(schema));
        }
        
        if (retry != null && !retry.isEmpty()) {
            config.setRetry(convertToRetryConfig(retry));
        }
        
        // Convert custom properties
        if (customProperties != null && !customProperties.isEmpty()) {
            config.setCustomProperties(new HashMap<>(customProperties));
        }
        
        // Set parameter names
        if (parameterNames != null) {
            config.setParameterNames(parameterNames.clone());
        }
        
        // Set tags
        if (tags != null) {
            config.setTags(List.copyOf(tags));
        }
        
        return config;
    }
    
    // Private conversion methods (following YamlDataSource patterns)
    // These methods will be implemented to convert Map configurations to typed config objects
    // For now, we'll create placeholder methods that will be implemented in the next iteration
    
    private ConnectionConfig convertToConnectionConfig(Map<String, Object> map) {
        ConnectionConfig config = new ConnectionConfig();

        // Basic connection properties
        if (map.containsKey("host")) {
            config.setHost((String) map.get("host"));
        }
        if (map.containsKey("port")) {
            Object port = map.get("port");
            if (port instanceof Integer) {
                config.setPort((Integer) port);
            }
        }
        if (map.containsKey("database")) {
            config.setDatabase((String) map.get("database"));
        }
        if (map.containsKey("username")) {
            config.setUsername((String) map.get("username"));
        }
        if (map.containsKey("password")) {
            config.setPassword((String) map.get("password"));
        }
        if (map.containsKey("mode")) {
            config.setCustomProperty("mode", map.get("mode"));
        }
        if (map.containsKey("base-path")) {
            config.setBasePath((String) map.get("base-path"));
        }
        if (map.containsKey("file-pattern")) {
            config.setFilePattern((String) map.get("file-pattern"));
        }
        if (map.containsKey("encoding")) {
            config.setEncoding((String) map.get("encoding"));
        }

        return config;
    }
    
    private CacheConfig convertToCacheConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic similar to YamlDataSource
        return new CacheConfig();
    }
    
    private HealthCheckConfig convertToHealthCheckConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic similar to YamlDataSource
        return new HealthCheckConfig();
    }
    
    private AuthenticationConfig convertToAuthenticationConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic similar to YamlDataSource
        return new AuthenticationConfig();
    }
    
    private CircuitBreakerConfig convertToCircuitBreakerConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic similar to YamlDataSource
        return new CircuitBreakerConfig();
    }
    
    private OutputFormatConfig convertToOutputFormatConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic for OutputFormatConfig
        return new OutputFormatConfig();
    }
    
    private ErrorHandlingConfig convertToErrorHandlingConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic for ErrorHandlingConfig
        return new ErrorHandlingConfig();
    }
    
    private BatchConfig convertToBatchConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic for BatchConfig
        return new BatchConfig();
    }
    
    private SchemaConfig convertToSchemaConfig(Map<String, Object> map) {
        SchemaConfig config = new SchemaConfig();

        // Basic settings
        config.setEnabled(getBooleanValue(map, "enabled", true));
        config.setStrategy(getStringValue(map, "strategy", "validate-only"));
        config.setSchemaName(getStringValue(map, "schema-name"));
        config.setTableName(getStringValue(map, "table-name"));
        config.setCatalogName(getStringValue(map, "catalog-name"));

        // Schema creation settings
        config.setAutoCreate(getBooleanValue(map, "auto-create", false));
        config.setAutoUpdate(getBooleanValue(map, "auto-update", false));
        config.setDropIfExists(getBooleanValue(map, "drop-if-exists", false));
        config.setInitScript(getStringValue(map, "init-script"));

        // Handle init-scripts as a list
        @SuppressWarnings("unchecked")
        List<String> initScripts = (List<String>) map.get("init-scripts");
        if (initScripts != null) {
            config.setInitScripts(initScripts);
        }

        // Data validation settings
        config.setValidateData(getBooleanValue(map, "validate-data", true));
        config.setStrictMode(getBooleanValue(map, "strict-mode", false));
        config.setAllowNulls(getBooleanValue(map, "allow-nulls", true));
        config.setTruncateStrings(getBooleanValue(map, "truncate-strings", false));
        config.setMaxStringLength(getIntegerValue(map, "max-string-length", 255));

        return config;
    }
    
    private RetryConfig convertToRetryConfig(Map<String, Object> map) {
        // TODO: Implement conversion logic for RetryConfig
        return new RetryConfig();
    }

    // Helper methods for type conversion

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        String value = getStringValue(map, key);
        return value != null ? value : defaultValue;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key, Integer defaultValue) {
        Integer value = getIntegerValue(map, key);
        return value != null ? value : defaultValue;
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}
