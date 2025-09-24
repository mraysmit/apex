package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Configuration class for standard APEX external-data-config files.
 * 
 * This class handles the standard APEX external-data-config format that uses
 * the dataSources section, as opposed to the Kubernetes-style format handled
 * by ExternalDataSourceConfig.
 * 
 * Example YAML structure:
 * <pre>
 * metadata:
 *   id: "data-sources-example"
 *   name: "Data Sources Example"
 *   version: "1.0.0"
 *   type: "external-data-config"
 *   author: "apex.team@company.com"
 * 
 * dataSources:
 *   - name: "customer-database"
 *     type: "database"
 *     enabled: true
 *     connection:
 *       host: "localhost"
 *       database: "customers"
 * 
 * configuration:
 *   monitoring:
 *     enabled: true
 * 
 * environments:
 *   development:
 *     dataSources:
 *       - name: "customer-database"
 *         connection:
 *           host: "dev-db"
 * </pre>
 * 
 * @author APEX Core Team
 * @since 2025-09-24
 * @version 1.0.0
 */
public class ExternalDataConfiguration {
    
    @JsonProperty("metadata")
    private ConfigurationMetadata metadata;
    
    @JsonProperty("dataSources")
    private List<YamlDataSource> dataSources;
    
    @JsonProperty("configuration")
    private Map<String, Object> configuration;
    
    @JsonProperty("environments")
    private Map<String, Object> environments;
    
    @JsonProperty("rules")
    private List<Map<String, Object>> rules;
    
    // Default constructor
    public ExternalDataConfiguration() {}
    
    // Getters and setters
    public ConfigurationMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ConfigurationMetadata metadata) {
        this.metadata = metadata;
    }
    
    public List<YamlDataSource> getDataSources() {
        return dataSources;
    }
    
    public void setDataSources(List<YamlDataSource> dataSources) {
        this.dataSources = dataSources;
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
    
    public Map<String, Object> getEnvironments() {
        return environments;
    }
    
    public void setEnvironments(Map<String, Object> environments) {
        this.environments = environments;
    }
    
    public List<Map<String, Object>> getRules() {
        return rules;
    }
    
    public void setRules(List<Map<String, Object>> rules) {
        this.rules = rules;
    }
    
    /**
     * Metadata about the external data configuration file.
     * Reuses the same structure as YamlRuleConfiguration.ConfigurationMetadata.
     */
    public static class ConfigurationMetadata {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;
        
        @JsonProperty("description")
        private String description;

        @JsonProperty("type")
        private String type;

        @JsonProperty("author")
        private String author;
        
        @JsonProperty("created")
        private String created;
        
        @JsonProperty("last-modified")
        private String lastModified;
        
        @JsonProperty("tags")
        private List<String> tags;
        
        // Default constructor
        public ConfigurationMetadata() {}

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        @Override
        public String toString() {
            return "ConfigurationMetadata{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", type='" + type + '\'' +
                    ", author='" + author + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "ExternalDataConfiguration{" +
                "metadata=" + metadata +
                ", dataSources=" + (dataSources != null ? dataSources.size() + " sources" : "null") +
                ", configuration=" + (configuration != null ? "present" : "null") +
                ", environments=" + (environments != null ? environments.keySet() : "null") +
                ", rules=" + (rules != null ? rules.size() + " rules" : "null") +
                '}';
    }
}
