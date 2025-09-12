package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YAML configuration class for external rule file references.
 * 
 * This class represents a reference to an external rule configuration file,
 * enabling separation of rule definitions across multiple files.
 * 
 * Example YAML structure:
 * <pre>
 * rule-refs:
 *   - name: "customer-rules"
 *     source: "rules/customer-rules.yaml"
 *     enabled: true
 *     description: "Customer validation rules"
 * </pre>
 * 
 * @author APEX Core Team
 * @since 2025-09-13
 * @version 1.0.0
 */
public class YamlRuleRef {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("description")
    private String description;
    
    // Constructors
    public YamlRuleRef() {}
    
    public YamlRuleRef(String name, String source) {
        this.name = name;
        this.source = source;
        this.enabled = true;
    }
    
    public YamlRuleRef(String name, String source, Boolean enabled, String description) {
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
     * Check if this rule reference is enabled.
     * Defaults to true if not explicitly set.
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }
    
    @Override
    public String toString() {
        return "YamlRuleRef{" +
                "name='" + name + '\'' +
                ", source='" + source + '\'' +
                ", enabled=" + enabled +
                ", description='" + description + '\'' +
                '}';
    }
}
