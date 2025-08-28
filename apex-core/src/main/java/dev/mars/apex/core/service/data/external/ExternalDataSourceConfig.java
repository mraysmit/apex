package dev.mars.apex.core.service.data.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * External data-source configuration model.
 * 
 * Represents the structure of external data-source YAML files that can be
 * referenced from enrichment configurations for infrastructure separation.
 * 
 * Example YAML structure:
 * <pre>
 * apiVersion: "apex.dev/v1"
 * kind: "DataSource"
 * metadata:
 *   name: "customer-database"
 *   version: "1.0.0"
 *   description: "Customer database configuration"
 * spec:
 *   type: "database"
 *   source-type: "h2"
 *   connection:
 *     database: "apex_demo_shared"
 *     username: "sa"
 *     password: ""
 *   queries:
 *     getCustomerById: "SELECT * FROM customers WHERE customer_id = :customerId"
 *   cache:
 *     enabled: true
 *     ttlSeconds: 300
 * </pre>
 * 
 * @author APEX Core Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class ExternalDataSourceConfig {
    
    @JsonProperty("apiVersion")
    private String apiVersion;
    
    @JsonProperty("kind")
    private String kind;
    
    @JsonProperty("metadata")
    private DataSourceMetadata metadata;
    
    @JsonProperty("spec")
    private DataSourceSpec spec;
    
    // Constructors
    public ExternalDataSourceConfig() {}
    
    public ExternalDataSourceConfig(String apiVersion, String kind, DataSourceMetadata metadata, DataSourceSpec spec) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.metadata = metadata;
        this.spec = spec;
    }
    
    // Getters and Setters
    public String getApiVersion() {
        return apiVersion;
    }
    
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
    
    public String getKind() {
        return kind;
    }
    
    public void setKind(String kind) {
        this.kind = kind;
    }
    
    public DataSourceMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(DataSourceMetadata metadata) {
        this.metadata = metadata;
    }
    
    public DataSourceSpec getSpec() {
        return spec;
    }
    
    public void setSpec(DataSourceSpec spec) {
        this.spec = spec;
    }
    
    /**
     * Metadata section of external data-source configuration.
     */
    public static class DataSourceMetadata {
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("environment")
        private String environment;
        
        @JsonProperty("labels")
        private Map<String, String> labels;
        
        // Constructors
        public DataSourceMetadata() {}
        
        public DataSourceMetadata(String name, String version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
        }
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getEnvironment() {
            return environment;
        }
        
        public void setEnvironment(String environment) {
            this.environment = environment;
        }
        
        public Map<String, String> getLabels() {
            return labels;
        }
        
        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }
    }
    
    /**
     * Specification section of external data-source configuration.
     */
    public static class DataSourceSpec {
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("source-type")
        private String sourceType;
        
        @JsonProperty("enabled")
        private Boolean enabled;
        
        @JsonProperty("connection")
        private Map<String, Object> connection;
        
        @JsonProperty("queries")
        private Map<String, String> queries;
        
        @JsonProperty("cache")
        private Map<String, Object> cache;
        
        @JsonProperty("parameters")
        private Map<String, Object> parameters;
        
        // Constructors
        public DataSourceSpec() {}
        
        // Getters and Setters
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
        
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
        
        public Map<String, Object> getConnection() {
            return connection;
        }
        
        public void setConnection(Map<String, Object> connection) {
            this.connection = connection;
        }
        
        public Map<String, String> getQueries() {
            return queries;
        }
        
        public void setQueries(Map<String, String> queries) {
            this.queries = queries;
        }
        
        public Map<String, Object> getCache() {
            return cache;
        }
        
        public void setCache(Map<String, Object> cache) {
            this.cache = cache;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}
