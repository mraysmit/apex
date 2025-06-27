package dev.mars.rulesengine.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * YAML representation of a category configuration.
 * This class maps to the YAML structure for defining rule categories.
 */
public class YamlCategory {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("display-name")
    private String displayName;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("priority")
    private Integer priority;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("parent-category")
    private String parentCategory;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("execution-order")
    private Integer executionOrder;
    
    @JsonProperty("stop-on-first-failure")
    private Boolean stopOnFirstFailure;
    
    @JsonProperty("parallel-execution")
    private Boolean parallelExecution;
    
    // Default constructor
    public YamlCategory() {
        this.enabled = true; // Default to enabled
        this.priority = 100; // Default priority
        this.stopOnFirstFailure = false; // Default to continue on failure
        this.parallelExecution = false; // Default to sequential execution
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getParentCategory() {
        return parentCategory;
    }
    
    public void setParentCategory(String parentCategory) {
        this.parentCategory = parentCategory;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Integer getExecutionOrder() {
        return executionOrder;
    }
    
    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }
    
    public Boolean getStopOnFirstFailure() {
        return stopOnFirstFailure;
    }
    
    public void setStopOnFirstFailure(Boolean stopOnFirstFailure) {
        this.stopOnFirstFailure = stopOnFirstFailure;
    }
    
    public Boolean getParallelExecution() {
        return parallelExecution;
    }
    
    public void setParallelExecution(Boolean parallelExecution) {
        this.parallelExecution = parallelExecution;
    }
}
