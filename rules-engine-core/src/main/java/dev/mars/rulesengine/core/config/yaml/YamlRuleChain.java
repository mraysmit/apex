package dev.mars.rulesengine.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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

/**
 * YAML configuration class for rule chains that support nested rules and rule chaining patterns.
 * 
 * Supports 6 different patterns:
 * 1. conditional-chaining - Execute Rule B only if Rule A triggers
 * 2. sequential-dependency - Each rule builds upon results from the previous rule
 * 3. result-based-routing - Route to different rule sets based on previous results
 * 4. accumulative-chaining - Build up a score/result across multiple rules
 * 5. complex-workflow - Real-world nested rule scenario with multi-stage processing
 * 6. fluent-builder - Compose rules with conditional execution paths using fluent API
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class YamlRuleChain {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("pattern")
    private String pattern;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("priority")
    private Integer priority;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("categories")
    private List<String> categories;
    
    @JsonProperty("configuration")
    private Map<String, Object> configuration;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Enterprise metadata fields
    @JsonProperty("created-by")
    private String createdBy;
    
    @JsonProperty("business-domain")
    private String businessDomain;
    
    @JsonProperty("business-owner")
    private String businessOwner;
    
    @JsonProperty("source-system")
    private String sourceSystem;
    
    @JsonProperty("effective-date")
    private String effectiveDate;
    
    @JsonProperty("expiration-date")
    private String expirationDate;
    
    @JsonProperty("custom-properties")
    private Map<String, Object> customProperties;
    
    // Default constructor
    public YamlRuleChain() {}
    
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public List<String> getCategories() {
        return categories;
    }
    
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
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
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getBusinessDomain() {
        return businessDomain;
    }
    
    public void setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
    }
    
    public String getBusinessOwner() {
        return businessOwner;
    }
    
    public void setBusinessOwner(String businessOwner) {
        this.businessOwner = businessOwner;
    }
    
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
    
    public String getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public String getExpirationDate() {
        return expirationDate;
    }
    
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }
    
    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }
    
    /**
     * Check if this rule chain is enabled.
     * 
     * @return true if enabled (default), false if explicitly disabled
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }
    
    /**
     * Get the priority with a default value.
     * 
     * @return the priority, or 100 if not specified
     */
    public int getPriorityValue() {
        return priority != null ? priority : 100;
    }
    
    @Override
    public String toString() {
        return "YamlRuleChain{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", pattern='" + pattern + '\'' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
}
