package dev.mars.apex.core.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YAML configuration class for external enrichment file references.
 * 
 * This class represents a reference to an external enrichment configuration file,
 * enabling separation of enrichment definitions across multiple files.
 * 
 * Example YAML structure:
 * <pre>
 * enrichment-refs:
 *   - name: "customer-enrichments"
 *     source: "enrichments/customer-enrichments.yaml"
 *     enabled: true
 *     description: "Customer data enrichment rules"
 * </pre>
 * 
 * @author Mark A Ray-Smith Cityline Ltd
 * @since 2025-10-27
 * @version 1.0.0
 */
public class YamlEnrichmentRef {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("description")
    private String description;
    
    // Constructors
    public YamlEnrichmentRef() {}
    
    public YamlEnrichmentRef(String name, String source) {
        this.name = name;
        this.source = source;
        this.enabled = true;
    }
    
    public YamlEnrichmentRef(String name, String source, Boolean enabled, String description) {
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
     * Check if this enrichment reference is enabled.
     * Defaults to true if not explicitly set.
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }
    
    @Override
    public String toString() {
        return "YamlEnrichmentRef{" +
                "name='" + name + '\'' +
                ", source='" + source + '\'' +
                ", enabled=" + enabled +
                ", description='" + description + '\'' +
                '}';
    }
}

