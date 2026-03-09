package dev.mars.apex.core.service.data.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * External data-source configuration model.
 * 
 * Represents the structure of external data-source YAML files that can be
 * referenced from enrichment configurations for infrastructure separation.
 * 
 * Uses standard APEX YAML format with metadata and data-sources sections.
 * 
 * Example YAML structure:
 * <pre>
 * metadata:
 *   id: "customer-database-config"
 *   name: "customer-database"
 *   type: "external-data-config"
 *   version: "1.0.0"
 *   description: "Customer database configuration"
 * 
 * data-sources:
 *   - name: "customer-database"
 *     type: "database"
 *     source-type: "h2"
 *     enabled: true
 *     connection:
 *       database: "apex_demo_shared"
 *       username: "sa"
 *       password: ""
 *     queries:
 *       getCustomerById: "SELECT * FROM customers WHERE customer_id = :customerId"
 *     cache:
 *       enabled: true
 *       ttlSeconds: 300
 * </pre>
 * 
 * @author Mark A Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 2.0.0
 */
public class ExternalDataSourceConfig {
    
    @JsonProperty("metadata")
    private DataSourceMetadata metadata;
    
    @JsonProperty("data-sources")
    private java.util.List<DataSourceSpec> dataSources;
    
    // Constructors
    public ExternalDataSourceConfig() {}
    
    public ExternalDataSourceConfig(DataSourceMetadata metadata, java.util.List<DataSourceSpec> dataSources) {
        this.metadata = metadata;
        this.dataSources = dataSources;
    }
    
    // Getters and Setters
    public DataSourceMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(DataSourceMetadata metadata) {
        this.metadata = metadata;
    }
    
    public java.util.List<DataSourceSpec> getDataSources() {
        return dataSources;
    }
    
    public void setDataSources(java.util.List<DataSourceSpec> dataSources) {
        this.dataSources = dataSources;
    }
    
    /**
     * Get the first (primary) data source spec.
     * Convenience method for configurations with a single data source.
     */
    public DataSourceSpec getSpec() {
        if (dataSources != null && !dataSources.isEmpty()) {
            return dataSources.get(0);
        }
        return null;
    }
    
    /**
     * Metadata section of external data-source configuration.
     * Uses standard APEX metadata format.
     */
    public static class DataSourceMetadata {
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("author")
        private String author;
        
        @JsonProperty("created-date")
        private String createdDate;
        
        @JsonProperty("tags")
        private java.util.List<String> tags;
        
        // Constructors
        public DataSourceMetadata() {}
        
        public DataSourceMetadata(String name, String version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.type = "external-data-config";
        }
        
        // Getters and Setters
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
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
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
        
        public String getAuthor() {
            return author;
        }
        
        public void setAuthor(String author) {
            this.author = author;
        }
        
        public String getCreatedDate() {
            return createdDate;
        }
        
        public void setCreatedDate(String createdDate) {
            this.createdDate = createdDate;
        }
        
        public java.util.List<String> getTags() {
            return tags;
        }
        
        public void setTags(java.util.List<String> tags) {
            this.tags = tags;
        }
    }
    
    /**
     * Data source specification within external data-source configuration.
     * Matches standard APEX data-sources array element format.
     */
    public static class DataSourceSpec {
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("source-type")
        private String sourceType;
        
        @JsonProperty("enabled")
        private Boolean enabled;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("connection")
        private Map<String, Object> connection;
        
        @JsonProperty("queries")
        private Map<String, String> queries;
        
        @JsonProperty("cache")
        private Map<String, Object> cache;
        
        @JsonProperty("parameters")
        private Map<String, Object> parameters;
        
        @JsonProperty("connection-pool")
        private Map<String, Object> connectionPool;
        
        // Constructors
        public DataSourceSpec() {}
        
        // Getters and Setters
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
        
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
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
        
        public Map<String, Object> getConnectionPool() {
            return connectionPool;
        }
        
        public void setConnectionPool(Map<String, Object> connectionPool) {
            this.connectionPool = connectionPool;
        }
    }
}
