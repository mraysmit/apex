package dev.mars.apex.core.config.datasource;

import dev.mars.apex.core.service.data.external.DataSourceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Main configuration class for external data sources.
 * 
 * This class contains all the configuration settings needed to initialize
 * and manage an external data source, including connection settings,
 * caching configuration, health checks, and authentication.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceConfiguration {
    
    private String name;
    private String type;
    private String sourceType;
    private String description;
    private boolean enabled = true;
    private String implementation;
    
    private ConnectionConfig connection;
    private CacheConfig cache;
    private HealthCheckConfig healthCheck;
    private AuthenticationConfig authentication;
    
    // Type-specific configurations
    private Map<String, String> queries;
    private Map<String, String> endpoints;
    private Map<String, String> topics;
    private Map<String, String> keyPatterns;
    private FileFormatConfig fileFormat;
    private CircuitBreakerConfig circuitBreaker;
    private ResponseMappingConfig responseMapping;
    
    // Custom properties for extensibility
    private Map<String, Object> customProperties;
    
    // Parameter configuration
    private String[] parameterNames;

    // Tags for categorization and discovery
    private List<String> tags;
    
    /**
     * Default constructor.
     */
    public DataSourceConfiguration() {
        this.queries = new HashMap<>();
        this.endpoints = new HashMap<>();
        this.topics = new HashMap<>();
        this.keyPatterns = new HashMap<>();
        this.customProperties = new HashMap<>();
    }
    
    /**
     * Constructor with basic configuration.
     * 
     * @param name The name of the data source
     * @param type The type of data source
     */
    public DataSourceConfiguration(String name, String type) {
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
    
    public DataSourceType getDataSourceType() {
        return DataSourceType.fromConfigValue(type);
    }

    public void setDataSourceType(DataSourceType dataSourceType) {
        this.type = dataSourceType.getConfigValue();
    }
    
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDataType() {
        // Return sourceType as the data type, or fall back to type
        return sourceType != null ? sourceType : type;
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
    
    // Configuration objects
    
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
    
    // Type-specific configurations
    
    public Map<String, String> getQueries() {
        return queries;
    }
    
    public void setQueries(Map<String, String> queries) {
        this.queries = queries != null ? queries : new HashMap<>();
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
    
    public FileFormatConfig getFileFormat() {
        return fileFormat;
    }
    
    public void setFileFormat(FileFormatConfig fileFormat) {
        this.fileFormat = fileFormat;
    }
    
    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }
    
    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    public ResponseMappingConfig getResponseMapping() {
        return responseMapping;
    }
    
    public void setResponseMapping(ResponseMappingConfig responseMapping) {
        this.responseMapping = responseMapping;
    }
    
    // Custom properties
    
    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }
    
    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties != null ? customProperties : new HashMap<>();
    }
    
    public Object getCustomProperty(String key) {
        return customProperties.get(key);
    }
    
    public void setCustomProperty(String key, Object value) {
        customProperties.put(key, value);
    }
    
    // Parameter configuration
    
    public String[] getParameterNames() {
        return parameterNames;
    }
    
    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    // Tags configuration

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    // Validation methods
    
    /**
     * Validate the configuration for completeness and correctness.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source name is required");
        }
        
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source type is required");
        }
        
        DataSourceType dataSourceType = getDataSourceType();
        if (dataSourceType == null) {
            throw new IllegalArgumentException("Invalid data source type: " + type);
        }
        
        // Validate type-specific requirements
        validateTypeSpecificConfiguration(dataSourceType);
        
        // Validate sub-configurations
        if (connection != null) {
            connection.validate();
        }
        
        if (cache != null) {
            cache.validate();
        }
        
        if (healthCheck != null) {
            healthCheck.validate();
        }
        
        if (authentication != null) {
            authentication.validate();
        }
    }
    
    /**
     * Validate type-specific configuration requirements.
     * 
     * @param dataSourceType The data source type
     */
    private void validateTypeSpecificConfiguration(DataSourceType dataSourceType) {
        switch (dataSourceType) {
            case DATABASE:
                if (connection == null) {
                    throw new IllegalArgumentException("Connection configuration is required for database data sources");
                }
                break;
            case REST_API:
                if (connection == null || connection.getBaseUrl() == null) {
                    throw new IllegalArgumentException("Base URL is required for REST API data sources");
                }
                break;
            case CUSTOM:
                if (implementation == null || implementation.trim().isEmpty()) {
                    throw new IllegalArgumentException("Implementation class is required for custom data sources");
                }
                break;
            case MESSAGE_QUEUE:
            case CACHE:
            case FILE_SYSTEM:
                // These types have optional configurations
                break;
        }
    }
    
    /**
     * Create a copy of this configuration.
     * 
     * @return A new DataSourceConfiguration with the same settings
     */
    public DataSourceConfiguration copy() {
        DataSourceConfiguration copy = new DataSourceConfiguration();
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
        
        copy.queries = new HashMap<>(this.queries);
        copy.endpoints = new HashMap<>(this.endpoints);
        copy.topics = new HashMap<>(this.topics);
        copy.keyPatterns = new HashMap<>(this.keyPatterns);
        copy.customProperties = new HashMap<>(this.customProperties);
        
        copy.fileFormat = this.fileFormat != null ? this.fileFormat.copy() : null;
        copy.circuitBreaker = this.circuitBreaker != null ? this.circuitBreaker.copy() : null;
        copy.responseMapping = this.responseMapping != null ? this.responseMapping.copy() : null;
        
        copy.parameterNames = this.parameterNames != null ? this.parameterNames.clone() : null;
        
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceConfiguration that = (DataSourceConfiguration) o;
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
        return "DataSourceConfiguration{" +
               "name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", sourceType='" + sourceType + '\'' +
               ", enabled=" + enabled +
               ", description='" + description + '\'' +
               '}';
    }
}
