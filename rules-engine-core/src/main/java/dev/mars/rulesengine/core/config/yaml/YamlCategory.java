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
 * YAML representation of a category configuration.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
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

    // Enterprise metadata fields for category governance
    @JsonProperty("business-domain")
    private String businessDomain;

    @JsonProperty("business-owner")
    private String businessOwner;

    @JsonProperty("created-by")
    private String createdBy;

    @JsonProperty("effective-date")
    private String effectiveDate;

    @JsonProperty("expiration-date")
    private String expirationDate;
    
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

    // Enterprise metadata getters and setters

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
}
