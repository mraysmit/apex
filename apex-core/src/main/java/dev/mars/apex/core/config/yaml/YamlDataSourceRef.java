package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YAML configuration class for external data-source references.
 * 
 * This class represents a reference to an external data-source configuration file,
 * enabling separation of infrastructure configuration from business logic configuration.
 * 
 * Example YAML structure:
 * <pre>
 * data-source-refs:
 *   - name: "customer-database"
 *     source: "data-sources/customer-database.yaml"
 *     enabled: true
 * </pre>
 * 
 * @author APEX Core Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class YamlDataSourceRef {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("description")
    private String description;
    
    // Constructors
    public YamlDataSourceRef() {}
    
    public YamlDataSourceRef(String name, String source) {
        this.name = name;
        this.source = source;
        this.enabled = true;
    }
    
    public YamlDataSourceRef(String name, String source, Boolean enabled, String description) {
        this.name = name;
        this.source = source;
        this.enabled = enabled;
        this.description = description;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
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
    
    /**
     * Check if this data-source reference is enabled.
     * Defaults to true if not explicitly set.
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }
    
    @Override
    public String toString() {
        return "YamlDataSourceRef{" +
                "name='" + name + '\'' +
                ", source='" + source + '\'' +
                ", enabled=" + enabled +
                ", description='" + description + '\'' +
                '}';
    }
}
