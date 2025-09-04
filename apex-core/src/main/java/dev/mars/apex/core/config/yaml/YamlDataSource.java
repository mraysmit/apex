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
import dev.mars.apex.core.config.datasource.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * YAML configuration class for external data sources.
 * 
 * This class represents the YAML structure for configuring external data sources
 * and provides conversion methods to create DataSourceConfiguration objects.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class YamlDataSource {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("source-type")
    private String sourceType;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("enabled")
    private Boolean enabled = true;
    
    @JsonProperty("implementation")
    private String implementation;
    
    @JsonProperty("connection")
    private Map<String, Object> connection;
    
    @JsonProperty("cache")
    private Map<String, Object> cache;
    
    @JsonProperty("health-check")
    private Map<String, Object> healthCheck;
    
    @JsonProperty("authentication")
    private Map<String, Object> authentication;
    
    // Type-specific configurations
    @JsonProperty("queries")
    private Map<String, String> queries;

    @JsonProperty("operations")
    private Map<String, String> operations;

    @JsonProperty("endpoints")
    private Map<String, String> endpoints;
    
    @JsonProperty("topics")
    private Map<String, String> topics;
    
    @JsonProperty("key-patterns")
    private Map<String, String> keyPatterns;
    
    @JsonProperty("file-format")
    private Map<String, Object> fileFormat;
    
    @JsonProperty("circuit-breaker")
    private Map<String, Object> circuitBreaker;
    
    @JsonProperty("response-mapping")
    private Map<String, Object> responseMapping;
    
    @JsonProperty("custom-properties")
    private Map<String, Object> customProperties;
    
    @JsonProperty("parameter-names")
    private String[] parameterNames;

    @JsonProperty("tags")
    private List<String> tags;
    
    /**
     * Default constructor.
     */
    public YamlDataSource() {
        this.connection = new HashMap<>();
        this.cache = new HashMap<>();
        this.healthCheck = new HashMap<>();
        this.authentication = new HashMap<>();
        this.queries = new HashMap<>();
        this.endpoints = new HashMap<>();
        this.topics = new HashMap<>();
        this.keyPatterns = new HashMap<>();
        this.fileFormat = new HashMap<>();
        this.circuitBreaker = new HashMap<>();
        this.responseMapping = new HashMap<>();
        this.customProperties = new HashMap<>();
    }
    
    // Getters and setters
    
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
    
    public Map<String, String> getQueries() {
        return queries;
    }

    public void setQueries(Map<String, String> queries) {
        this.queries = queries != null ? queries : new HashMap<>();
    }

    public Map<String, String> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String> operations) {
        this.operations = operations != null ? operations : new HashMap<>();
    }

    public Map<String, String> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(Map<String, String> endpoints) {
        this.endpoints = endpoints != null ? endpoints : new HashMap<>();
    }
    
    public Map<String, String> getTopics() {
        return topics;
    }
    
    public void setTopics(Map<String, String> topics) {
        this.topics = topics != null ? topics : new HashMap<>();
    }
    
    public Map<String, String> getKeyPatterns() {
        return keyPatterns;
    }
    
    public void setKeyPatterns(Map<String, String> keyPatterns) {
        this.keyPatterns = keyPatterns != null ? keyPatterns : new HashMap<>();
    }
    
    public Map<String, Object> getFileFormat() {
        return fileFormat;
    }
    
    public void setFileFormat(Map<String, Object> fileFormat) {
        this.fileFormat = fileFormat != null ? fileFormat : new HashMap<>();
    }
    
    public Map<String, Object> getCircuitBreaker() {
        return circuitBreaker;
    }
    
    public void setCircuitBreaker(Map<String, Object> circuitBreaker) {
        this.circuitBreaker = circuitBreaker != null ? circuitBreaker : new HashMap<>();
    }
    
    public Map<String, Object> getResponseMapping() {
        return responseMapping;
    }
    
    public void setResponseMapping(Map<String, Object> responseMapping) {
        this.responseMapping = responseMapping != null ? responseMapping : new HashMap<>();
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

    // Convenience methods for connection properties
    public void setBasePath(String basePath) {
        if (connection == null) {
            connection = new HashMap<>();
        }
        connection.put("base-path", basePath);
    }

    public void setFilePattern(String filePattern) {
        if (connection == null) {
            connection = new HashMap<>();
        }
        connection.put("file-pattern", filePattern);
    }
    
    /**
     * Convert this YAML data source configuration to a DataSourceConfiguration object.
     * 
     * @return DataSourceConfiguration object
     */
    public DataSourceConfiguration toDataSourceConfiguration() {
        DataSourceConfiguration config = new DataSourceConfiguration(name, type);
        config.setSourceType(sourceType);
        config.setDescription(description);
        config.setEnabled(enabled != null ? enabled : true);
        config.setImplementation(implementation);
        
        // Convert connection configuration
        if (connection != null && !connection.isEmpty()) {
            config.setConnection(convertToConnectionConfig(connection));
        }
        
        // Convert cache configuration
        if (cache != null && !cache.isEmpty()) {
            config.setCache(convertToCacheConfig(cache));
        }
        
        // Convert health check configuration
        if (healthCheck != null && !healthCheck.isEmpty()) {
            config.setHealthCheck(convertToHealthCheckConfig(healthCheck));
        }
        
        // Convert authentication configuration
        if (authentication != null && !authentication.isEmpty()) {
            config.setAuthentication(convertToAuthenticationConfig(authentication));
        }
        
        // Set type-specific configurations
        // Merge queries and operations into the queries map
        Map<String, String> allQueries = new HashMap<>();
        if (queries != null) {
            allQueries.putAll(queries);
        }
        if (operations != null) {
            allQueries.putAll(operations); // Operations take precedence over queries if there are conflicts
        }
        config.setQueries(allQueries);

        config.setEndpoints(endpoints);
        config.setTopics(topics);
        config.setKeyPatterns(keyPatterns);
        
        // Convert complex configurations
        if (fileFormat != null && !fileFormat.isEmpty()) {
            config.setFileFormat(convertToFileFormatConfig(fileFormat));
        }
        
        if (circuitBreaker != null && !circuitBreaker.isEmpty()) {
            config.setCircuitBreaker(convertToCircuitBreakerConfig(circuitBreaker));
        }
        
        if (responseMapping != null && !responseMapping.isEmpty()) {
            config.setResponseMapping(convertToResponseMappingConfig(responseMapping));
        }
        
        config.setCustomProperties(customProperties);
        config.setParameterNames(parameterNames);
        
        return config;
    }
    
    /**
     * Convert map to ConnectionConfig.
     */
    private ConnectionConfig convertToConnectionConfig(Map<String, Object> map) {
        ConnectionConfig config = new ConnectionConfig();
        
        // Basic properties
        config.setHost(getStringValue(map, "host"));
        config.setPort(getIntegerValue(map, "port"));
        config.setDatabase(getStringValue(map, "database"));
        config.setSchema(getStringValue(map, "schema"));
        config.setUsername(getStringValue(map, "username"));
        config.setPassword(getStringValue(map, "password"));
        config.setSslEnabled(getBooleanValue(map, "ssl-enabled", false));
        config.setTrustStore(getStringValue(map, "trust-store"));
        config.setTrustStorePassword(getStringValue(map, "trust-store-password"));
        
        // HTTP properties
        config.setBaseUrl(getStringValue(map, "base-url"));
        config.setTimeout(getIntegerValue(map, "timeout"));
        config.setRetryAttempts(getIntegerValue(map, "retry-attempts"));
        config.setRetryDelay(getIntegerValue(map, "retry-delay"));
        
        // Headers
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) map.get("headers");
        if (headers != null) {
            config.setHeaders(headers);
        }
        
        // Message queue properties
        config.setBootstrapServers(getStringValue(map, "bootstrap-servers"));
        config.setSecurityProtocol(getStringValue(map, "security-protocol"));
        config.setSaslMechanism(getStringValue(map, "sasl-mechanism"));
        
        // File system properties
        config.setBasePath(getStringValue(map, "base-path"));
        config.setFilePattern(getStringValue(map, "file-pattern"));
        config.setPollingInterval(getIntegerValue(map, "polling-interval"));
        config.setEncoding(getStringValue(map, "encoding"));
        
        // Connection pool
        @SuppressWarnings("unchecked")
        Map<String, Object> poolMap = (Map<String, Object>) map.get("connection-pool");
        if (poolMap != null) {
            config.setConnectionPool(convertToConnectionPoolConfig(poolMap));
        }
        
        return config;
    }
    
    /**
     * Convert map to ConnectionPoolConfig.
     */
    private ConnectionPoolConfig convertToConnectionPoolConfig(Map<String, Object> map) {
        ConnectionPoolConfig config = new ConnectionPoolConfig();
        config.setMinSize(getIntegerValue(map, "min-size"));
        config.setMaxSize(getIntegerValue(map, "max-size"));
        config.setInitialSize(getIntegerValue(map, "initial-size"));
        config.setConnectionTimeout(getLongValue(map, "connection-timeout"));
        config.setIdleTimeout(getLongValue(map, "idle-timeout"));
        config.setMaxLifetime(getLongValue(map, "max-lifetime"));
        config.setLeakDetectionThreshold(getLongValue(map, "leak-detection-threshold"));
        config.setConnectionTestQuery(getStringValue(map, "connection-test-query"));
        config.setTestOnBorrow(getBooleanValue(map, "test-on-borrow", true));
        config.setTestOnReturn(getBooleanValue(map, "test-on-return", false));
        config.setTestWhileIdle(getBooleanValue(map, "test-while-idle", true));
        config.setValidationInterval(getLongValue(map, "validation-interval"));
        config.setMaxRetries(getIntegerValue(map, "max-retries"));
        config.setRetryDelay(getLongValue(map, "retry-delay"));
        return config;
    }
    
    // Helper methods for type conversion
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
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
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
    
    /**
     * Convert map to CacheConfig.
     */
    private CacheConfig convertToCacheConfig(Map<String, Object> map) {
        CacheConfig config = new CacheConfig();
        config.setEnabled(getBooleanValue(map, "enabled", true));
        config.setTtlSeconds(getLongValue(map, "ttl-seconds"));
        config.setMaxIdleSeconds(getLongValue(map, "max-idle-seconds"));
        config.setMaxSize(getIntegerValue(map, "max-size"));

        String evictionPolicy = getStringValue(map, "eviction-policy");
        if (evictionPolicy != null) {
            try {
                config.setEvictionPolicy(CacheConfig.EvictionPolicy.valueOf(evictionPolicy.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Use default
            }
        }

        config.setPreloadEnabled(getBooleanValue(map, "preload-enabled", false));
        config.setRefreshAhead(getBooleanValue(map, "refresh-ahead", false));
        config.setRefreshAheadFactor(getLongValue(map, "refresh-ahead-factor"));
        config.setStatisticsEnabled(getBooleanValue(map, "statistics-enabled", true));
        config.setKeyPrefix(getStringValue(map, "key-prefix"));
        config.setCompressionEnabled(getBooleanValue(map, "compression-enabled", false));
        config.setSerializationFormat(getStringValue(map, "serialization-format"));
        config.setWarmupEnabled(getBooleanValue(map, "warmup-enabled", false));
        config.setWarmupBatchSize(getIntegerValue(map, "warmup-batch-size"));
        config.setWarmupDelay(getLongValue(map, "warmup-delay"));
        config.setDistributedCache(getBooleanValue(map, "distributed-cache", false));
        config.setCacheCluster(getStringValue(map, "cache-cluster"));
        config.setReplicationFactor(getIntegerValue(map, "replication-factor"));

        return config;
    }

    /**
     * Convert map to HealthCheckConfig.
     */
    private HealthCheckConfig convertToHealthCheckConfig(Map<String, Object> map) {
        HealthCheckConfig config = new HealthCheckConfig();
        config.setEnabled(getBooleanValue(map, "enabled", true));
        config.setIntervalSeconds(getLongValue(map, "interval-seconds"));
        config.setTimeoutSeconds(getLongValue(map, "timeout-seconds"));
        config.setRetryAttempts(getIntegerValue(map, "retry-attempts"));
        config.setRetryDelay(getLongValue(map, "retry-delay"));
        config.setQuery(getStringValue(map, "query"));
        config.setEndpoint(getStringValue(map, "endpoint"));
        config.setExpectedResponse(getStringValue(map, "expected-response"));
        config.setFailureThreshold(getIntegerValue(map, "failure-threshold"));
        config.setSuccessThreshold(getIntegerValue(map, "success-threshold"));
        config.setLogFailures(getBooleanValue(map, "log-failures", true));
        config.setAlertOnFailure(getBooleanValue(map, "alert-on-failure", false));
        config.setAlertEndpoint(getStringValue(map, "alert-endpoint"));
        config.setCircuitBreakerIntegration(getBooleanValue(map, "circuit-breaker-integration", false));
        config.setCircuitBreakerFailureThreshold(getIntegerValue(map, "circuit-breaker-failure-threshold"));
        config.setCircuitBreakerTimeoutSeconds(getLongValue(map, "circuit-breaker-timeout-seconds"));

        return config;
    }

    /**
     * Convert map to AuthenticationConfig.
     */
    private AuthenticationConfig convertToAuthenticationConfig(Map<String, Object> map) {
        AuthenticationConfig config = new AuthenticationConfig();
        config.setType(getStringValue(map, "type"));
        config.setUsername(getStringValue(map, "username"));
        config.setPassword(getStringValue(map, "password"));
        config.setToken(getStringValue(map, "token"));
        config.setApiKey(getStringValue(map, "api-key"));
        config.setApiKeyHeader(getStringValue(map, "api-key-header"));
        config.setTokenHeader(getStringValue(map, "token-header"));
        config.setTokenPrefix(getStringValue(map, "token-prefix"));
        config.setClientId(getStringValue(map, "client-id"));
        config.setClientSecret(getStringValue(map, "client-secret"));
        config.setTokenUrl(getStringValue(map, "token-url"));
        config.setScope(getStringValue(map, "scope"));
        config.setGrantType(getStringValue(map, "grant-type"));
        config.setCertificatePath(getStringValue(map, "certificate-path"));
        config.setCertificatePassword(getStringValue(map, "certificate-password"));
        config.setKeyStorePath(getStringValue(map, "key-store-path"));
        config.setKeyStorePassword(getStringValue(map, "key-store-password"));
        config.setKeyStoreType(getStringValue(map, "key-store-type"));
        config.setTrustStorePath(getStringValue(map, "trust-store-path"));
        config.setTrustStorePassword(getStringValue(map, "trust-store-password"));
        config.setTrustStoreType(getStringValue(map, "trust-store-type"));
        config.setCustomImplementation(getStringValue(map, "custom-implementation"));

        @SuppressWarnings("unchecked")
        Map<String, Object> customProps = (Map<String, Object>) map.get("custom-properties");
        if (customProps != null) {
            config.setCustomProperties(customProps);
        }

        config.setAutoRefresh(getBooleanValue(map, "auto-refresh", true));
        config.setRefreshThresholdSeconds(getLongValue(map, "refresh-threshold-seconds"));
        config.setMaxRefreshAttempts(getIntegerValue(map, "max-refresh-attempts"));

        return config;
    }

    /**
     * Convert map to FileFormatConfig.
     */
    private FileFormatConfig convertToFileFormatConfig(Map<String, Object> map) {
        FileFormatConfig config = new FileFormatConfig();
        config.setType(getStringValue(map, "type"));
        config.setDelimiter(getStringValue(map, "delimiter"));
        config.setQuoteCharacter(getStringValue(map, "quote-character"));
        config.setEscapeCharacter(getStringValue(map, "escape-character"));
        config.setHeaderRow(getBooleanValue(map, "header-row", true));
        config.setSkipLines(getIntegerValue(map, "skip-lines"));
        config.setEncoding(getStringValue(map, "encoding"));
        config.setDateFormat(getStringValue(map, "date-format"));
        config.setTimestampFormat(getStringValue(map, "timestamp-format"));
        config.setNullValue(getStringValue(map, "null-value"));
        config.setRootPath(getStringValue(map, "root-path"));
        config.setFlattenArrays(getBooleanValue(map, "flatten-arrays", false));
        config.setRootElement(getStringValue(map, "root-element"));
        config.setRecordElement(getStringValue(map, "record-element"));

        @SuppressWarnings("unchecked")
        Map<String, String> namespaces = (Map<String, String>) map.get("namespaces");
        if (namespaces != null) {
            config.setNamespaces(namespaces);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> columnMappings = (Map<String, String>) map.get("column-mappings");
        if (columnMappings != null) {
            config.setColumnMappings(columnMappings);
        }

        config.setKeyColumn(getStringValue(map, "key-column"));
        config.setCustomParser(getStringValue(map, "custom-parser"));

        @SuppressWarnings("unchecked")
        Map<String, Object> customProps = (Map<String, Object>) map.get("custom-properties");
        if (customProps != null) {
            config.setCustomProperties(customProps);
        }

        return config;
    }

    /**
     * Convert map to CircuitBreakerConfig.
     */
    private CircuitBreakerConfig convertToCircuitBreakerConfig(Map<String, Object> map) {
        CircuitBreakerConfig config = new CircuitBreakerConfig();
        config.setEnabled(getBooleanValue(map, "enabled", true));
        config.setFailureThreshold(getIntegerValue(map, "failure-threshold"));
        config.setTimeoutSeconds(getLongValue(map, "timeout-seconds"));
        config.setSuccessThreshold(getIntegerValue(map, "success-threshold"));
        config.setRequestVolumeThreshold(getIntegerValue(map, "request-volume-threshold"));
        config.setFailureRateThreshold(getDoubleValue(map, "failure-rate-threshold"));
        config.setSlidingWindowSize(getLongValue(map, "sliding-window-size"));
        config.setFallbackResponse(getStringValue(map, "fallback-response"));
        config.setLogStateChanges(getBooleanValue(map, "log-state-changes", true));
        config.setMetricsEnabled(getBooleanValue(map, "metrics-enabled", true));
        config.setSlowCallDurationThreshold(getLongValue(map, "slow-call-duration-threshold"));
        config.setSlowCallRateThreshold(getDoubleValue(map, "slow-call-rate-threshold"));
        config.setAutomaticTransitionFromOpenToHalfOpen(getBooleanValue(map, "automatic-transition-from-open-to-half-open", true));
        config.setMaxWaitDurationInHalfOpen(getIntegerValue(map, "max-wait-duration-in-half-open"));

        return config;
    }

    /**
     * Convert map to ResponseMappingConfig.
     */
    private ResponseMappingConfig convertToResponseMappingConfig(Map<String, Object> map) {
        ResponseMappingConfig config = new ResponseMappingConfig();
        config.setFormat(getStringValue(map, "format"));
        config.setRootPath(getStringValue(map, "root-path"));
        config.setErrorPath(getStringValue(map, "error-path"));
        config.setDataPath(getStringValue(map, "data-path"));
        config.setStatusPath(getStringValue(map, "status-path"));
        config.setMessagePath(getStringValue(map, "message-path"));

        @SuppressWarnings("unchecked")
        Map<String, String> fieldMappings = (Map<String, String>) map.get("field-mappings");
        if (fieldMappings != null) {
            config.setFieldMappings(fieldMappings);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> fieldTypes = (Map<String, String>) map.get("field-types");
        if (fieldTypes != null) {
            config.setFieldTypes(fieldTypes);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> fieldFormats = (Map<String, String>) map.get("field-formats");
        if (fieldFormats != null) {
            config.setFieldFormats(fieldFormats);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> defaultValues = (Map<String, Object>) map.get("default-values");
        if (defaultValues != null) {
            config.setDefaultValues(defaultValues);
        }

        config.setFlattenNestedObjects(getBooleanValue(map, "flatten-nested-objects", false));
        config.setArrayHandling(getStringValue(map, "array-handling"));
        config.setNullHandling(getStringValue(map, "null-handling"));
        config.setTrimStrings(getBooleanValue(map, "trim-strings", true));
        config.setConvertEmptyToNull(getBooleanValue(map, "convert-empty-to-null", true));

        @SuppressWarnings("unchecked")
        List<String> includeFields = (List<String>) map.get("include-fields");
        if (includeFields != null) {
            config.setIncludeFields(includeFields);
        }

        @SuppressWarnings("unchecked")
        List<String> excludeFields = (List<String>) map.get("exclude-fields");
        if (excludeFields != null) {
            config.setExcludeFields(excludeFields);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> fieldValidations = (Map<String, String>) map.get("field-validations");
        if (fieldValidations != null) {
            config.setFieldValidations(fieldValidations);
        }

        config.setCustomTransformer(getStringValue(map, "custom-transformer"));

        @SuppressWarnings("unchecked")
        Map<String, Object> transformerProps = (Map<String, Object>) map.get("transformer-properties");
        if (transformerProps != null) {
            config.setTransformerProperties(transformerProps);
        }

        return config;
    }

    /**
     * Helper method to get Double value from map.
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YamlDataSource that = (YamlDataSource) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(type, that.type) &&
               Objects.equals(sourceType, that.sourceType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type, sourceType);
    }
    
    @Override
    public String toString() {
        return "YamlDataSource{" +
               "name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", sourceType='" + sourceType + '\'' +
               ", enabled=" + enabled +
               '}';
    }
}
