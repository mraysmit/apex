package dev.mars.apex.core.config.model;

import dev.mars.apex.core.config.deserializer.FlexibleOperationsDeserializer;

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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
 * @since 2025-09-04
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
    @JsonDeserialize(using = FlexibleOperationsDeserializer.class)
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
    
    @SuppressWarnings("unchecked")
    private OutputFormatConfig convertToOutputFormatConfig(Map<String, Object> map) {
        OutputFormatConfig config = new OutputFormatConfig();
        config.setFormat(getStringValue(map, "format", "json"));
        config.setEncoding(getStringValue(map, "encoding", "UTF-8"));
        config.setPrettyPrint(getBooleanValue(map, "pretty-print", false));
        config.setDateFormat(getStringValue(map, "date-format"));
        config.setNumberFormat(getStringValue(map, "number-format"));
        config.setBooleanFormat(getStringValue(map, "boolean-format"));
        config.setDelimiter(getStringValue(map, "delimiter"));
        config.setQuoteCharacter(getStringValue(map, "quote-character"));
        config.setEscapeCharacter(getStringValue(map, "escape-character"));
        config.setIncludeHeader(getBooleanValue(map, "include-header", true));
        config.setLineEnding(getStringValue(map, "line-ending"));

        Map<String, String> fieldMappings = (Map<String, String>) map.get("field-mappings");
        if (fieldMappings != null) {
            config.setFieldMappings(fieldMappings);
        }

        Map<String, String> fieldTypes = (Map<String, String>) map.get("field-types");
        if (fieldTypes != null) {
            config.setFieldTypes(fieldTypes);
        }

        Map<String, String> fieldFormats = (Map<String, String>) map.get("field-formats");
        if (fieldFormats != null) {
            config.setFieldFormats(fieldFormats);
        }

        Map<String, Object> defaultValues = (Map<String, Object>) map.get("default-values");
        if (defaultValues != null) {
            config.setDefaultValues(defaultValues);
        }

        return config;
    }
    
    @SuppressWarnings("unchecked")
    private ErrorHandlingConfig convertToErrorHandlingConfig(Map<String, Object> map) {
        ErrorHandlingConfig config = new ErrorHandlingConfig();
        config.setStrategy(getStringValue(map, "strategy", "fail-fast"));
        config.setMaxRetries(getIntegerValue(map, "max-retries", 3));
        config.setRetryDelay(getLongValue(map, "retry-delay", 1000L));
        config.setRetryBackoffMultiplier(getDoubleValue(map, "retry-backoff-multiplier", 2.0));
        config.setMaxRetryDelay(getLongValue(map, "max-retry-delay", 30000L));

        // Dead letter configuration
        config.setDeadLetterEnabled(getBooleanValue(map, "dead-letter-enabled", false));
        config.setDeadLetterTable(getStringValue(map, "dead-letter-table"));
        config.setDeadLetterTopic(getStringValue(map, "dead-letter-topic"));
        config.setDeadLetterFile(getStringValue(map, "dead-letter-file"));

        Map<String, Object> deadLetterProps = (Map<String, Object>) map.get("dead-letter-properties");
        if (deadLetterProps != null) {
            config.setDeadLetterProperties(deadLetterProps);
        }

        // Error logging
        config.setLogErrors(getBooleanValue(map, "log-errors", true));
        config.setLogLevel(getStringValue(map, "log-level", "ERROR"));
        config.setIncludeStackTrace(getBooleanValue(map, "include-stack-trace", true));
        config.setIncludeData(getBooleanValue(map, "include-data", false));
        config.setMaxLoggedErrors(getIntegerValue(map, "max-logged-errors", 100));

        // Error reporting
        config.setReportErrors(getBooleanValue(map, "report-errors", false));
        config.setReportingEndpoint(getStringValue(map, "reporting-endpoint"));
        config.setReportingTopic(getStringValue(map, "reporting-topic"));

        Map<String, String> reportingHeaders = (Map<String, String>) map.get("reporting-headers");
        if (reportingHeaders != null) {
            config.setReportingHeaders(reportingHeaders);
        }

        // Batch error handling
        config.setContinueOnBatchError(getBooleanValue(map, "continue-on-batch-error", false));
        config.setMaxBatchErrorRate(getDoubleValue(map, "max-batch-error-rate", 0.1));
        config.setMinBatchSuccessCount(getIntegerValue(map, "min-batch-success-count", 1));

        // Custom error handler
        config.setCustomErrorHandler(getStringValue(map, "custom-error-handler"));

        Map<String, Object> customHandlerProps = (Map<String, Object>) map.get("custom-handler-properties");
        if (customHandlerProps != null) {
            config.setCustomHandlerProperties(customHandlerProps);
        }

        return config;
    }
    
    private BatchConfig convertToBatchConfig(Map<String, Object> map) {
        BatchConfig config = new BatchConfig();
        config.setEnabled(getBooleanValue(map, "enabled", true));
        config.setMode(getStringValue(map, "mode", "size-based"));
        config.setBatchSize(getIntegerValue(map, "batch-size", 100));
        config.setMaxBatchSize(getIntegerValue(map, "max-batch-size", 1000));
        config.setMinBatchSize(getIntegerValue(map, "min-batch-size", 1));

        // Time-based batching
        config.setBatchTimeoutMs(getLongValue(map, "batch-timeout-ms", 5000L));
        config.setMaxBatchTimeoutMs(getLongValue(map, "max-batch-timeout-ms", 30000L));
        config.setFlushIntervalMs(getLongValue(map, "flush-interval-ms", 1000L));

        // Transaction configuration
        config.setTransactionMode(getStringValue(map, "transaction-mode", "per-batch"));
        config.setTransactionTimeoutMs(getLongValue(map, "transaction-timeout-ms", 30000L));
        config.setIsolationLevel(getStringValue(map, "isolation-level", "READ_COMMITTED"));

        // Memory management
        config.setMaxMemoryUsageMB(getLongValue(map, "max-memory-usage-mb", 100L));
        config.setEnableMemoryMonitoring(getBooleanValue(map, "enable-memory-monitoring", true));
        config.setMemoryThresholdPercent(getDoubleValue(map, "memory-threshold-percent", 0.8));

        // Performance tuning
        config.setParallelBatches(getIntegerValue(map, "parallel-batches", 1));
        config.setEnableCompression(getBooleanValue(map, "enable-compression", false));
        config.setCompressionAlgorithm(getStringValue(map, "compression-algorithm", "gzip"));

        // Buffer management
        config.setBufferSize(getIntegerValue(map, "buffer-size", 1000));
        config.setEnableBuffering(getBooleanValue(map, "enable-buffering", true));
        config.setBufferFlushIntervalMs(getLongValue(map, "buffer-flush-interval-ms", 2000L));

        // Batch ordering
        config.setMaintainOrder(getBooleanValue(map, "maintain-order", true));
        config.setOrderingField(getStringValue(map, "ordering-field"));
        config.setOrderingDirection(getStringValue(map, "ordering-direction", "ASC"));

        // Monitoring and metrics
        config.setEnableMetrics(getBooleanValue(map, "enable-metrics", true));
        config.setLogBatchStatistics(getBooleanValue(map, "log-batch-statistics", false));
        config.setMetricsReportingIntervalMs(getIntegerValue(map, "metrics-reporting-interval-ms", 10000));

        return config;
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
    
    @SuppressWarnings("unchecked")
    private RetryConfig convertToRetryConfig(Map<String, Object> map) {
        RetryConfig config = new RetryConfig();
        config.setEnabled(getBooleanValue(map, "enabled", true));
        config.setStrategy(getStringValue(map, "strategy", "exponential-backoff"));
        config.setMaxAttempts(getIntegerValue(map, "max-attempts", 3));
        config.setInitialDelay(getLongValue(map, "initial-delay", 1000L));
        config.setMaxDelay(getLongValue(map, "max-delay", 30000L));
        config.setBackoffMultiplier(getDoubleValue(map, "backoff-multiplier", 2.0));
        config.setJitterFactor(getDoubleValue(map, "jitter-factor", 0.1));

        // Retry conditions
        List<String> retryableExceptions = (List<String>) map.get("retryable-exceptions");
        if (retryableExceptions != null) {
            config.setRetryableExceptions(retryableExceptions);
        }

        List<String> nonRetryableExceptions = (List<String>) map.get("non-retryable-exceptions");
        if (nonRetryableExceptions != null) {
            config.setNonRetryableExceptions(nonRetryableExceptions);
        }

        List<Integer> retryableHttpCodes = (List<Integer>) map.get("retryable-http-codes");
        if (retryableHttpCodes != null) {
            config.setRetryableHttpCodes(retryableHttpCodes);
        }

        List<Integer> nonRetryableHttpCodes = (List<Integer>) map.get("non-retryable-http-codes");
        if (nonRetryableHttpCodes != null) {
            config.setNonRetryableHttpCodes(nonRetryableHttpCodes);
        }

        Map<String, String> retryConditions = (Map<String, String>) map.get("retry-conditions");
        if (retryConditions != null) {
            config.setRetryConditions(retryConditions);
        }

        // Circuit breaker integration
        config.setCircuitBreakerEnabled(getBooleanValue(map, "circuit-breaker-enabled", false));
        config.setCircuitBreakerThreshold(getIntegerValue(map, "circuit-breaker-threshold", 5));
        config.setCircuitBreakerTimeout(getLongValue(map, "circuit-breaker-timeout", 60000L));
        config.setCircuitBreakerSuccessThreshold(getIntegerValue(map, "circuit-breaker-success-threshold", 3));

        // Retry limits
        config.setTotalRetryTimeout(getLongValue(map, "total-retry-timeout", 300000L));
        config.setMaxRetriesPerMinute(getIntegerValue(map, "max-retries-per-minute", 10));
        config.setMaxRetriesPerHour(getIntegerValue(map, "max-retries-per-hour", 100));

        return config;
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

    private Long getLongValue(Map<String, Object> map, String key, Long defaultValue) {
        Long value = getLongValue(map, key);
        return value != null ? value : defaultValue;
    }

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

    private Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        Double value = getDoubleValue(map, key);
        return value != null ? value : defaultValue;
    }
}
