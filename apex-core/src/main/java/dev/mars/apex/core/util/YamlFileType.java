package dev.mars.apex.core.util;

/**
 * Enumeration of YAML file types in the APEX system.
 */
public enum YamlFileType {
    SCENARIO("Scenario Configuration"),
    RULE_CONFIG("Rule Configuration"),
    RULE_CHAIN("Rule Chain"),
    ENRICHMENT("Enrichment Configuration"),
    DATASET("Dataset Configuration"),
    BOOTSTRAP("Bootstrap Configuration"),
    UNKNOWN("Unknown Type");
    
    private final String description;
    
    YamlFileType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
