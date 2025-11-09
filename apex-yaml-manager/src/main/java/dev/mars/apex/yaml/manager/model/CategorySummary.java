package dev.mars.apex.yaml.manager.model;

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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Summary of a category extracted from YAML configuration files.
 * 
 * Represents category metadata including:
 * - Category identification and description
 * - Enterprise governance metadata (business domain, owner, creator)
 * - Lifecycle management (effective/expiration dates)
 * - Execution control (priority, parallel execution, failure handling)
 * - Usage tracking (which files define this category, which rules use it)
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-09
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategorySummary {

    // Core identification
    private String name;
    private String displayName;
    private String description;
    
    // Execution control
    private Integer priority;
    private Boolean enabled;
    private Integer executionOrder;
    private Boolean stopOnFirstFailure;
    private Boolean parallelExecution;
    
    // Enterprise governance metadata
    private String businessDomain;
    private String businessOwner;
    private String createdBy;
    private String effectiveDate;
    private String expirationDate;
    
    // Hierarchy
    private String parentCategory;
    
    // Classification
    private List<String> tags;
    private Map<String, Object> metadata;
    
    // Usage tracking
    private List<String> definedInFiles; // YAML files that define this category
    private List<String> usedByRules; // Rule IDs that reference this category
    private List<String> usedByRuleGroups; // Rule group IDs that reference this category
    private List<String> usedByEnrichments; // Enrichment IDs that reference this category
    private List<String> usedByEnrichmentGroups; // Enrichment group IDs that reference this category
    private int totalUsageCount;
    
    public CategorySummary() {
        this.tags = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.definedInFiles = new ArrayList<>();
        this.usedByRules = new ArrayList<>();
        this.usedByRuleGroups = new ArrayList<>();
        this.usedByEnrichments = new ArrayList<>();
        this.usedByEnrichmentGroups = new ArrayList<>();
        this.totalUsageCount = 0;
    }
    
    public CategorySummary(String name) {
        this();
        this.name = name;
    }

    // Getters and Setters
    
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

    public List<String> getDefinedInFiles() {
        return definedInFiles;
    }

    public void setDefinedInFiles(List<String> definedInFiles) {
        this.definedInFiles = definedInFiles;
    }

    public List<String> getUsedByRules() {
        return usedByRules;
    }

    public void setUsedByRules(List<String> usedByRules) {
        this.usedByRules = usedByRules;
    }

    public List<String> getUsedByRuleGroups() {
        return usedByRuleGroups;
    }

    public void setUsedByRuleGroups(List<String> usedByRuleGroups) {
        this.usedByRuleGroups = usedByRuleGroups;
    }

    public List<String> getUsedByEnrichments() {
        return usedByEnrichments;
    }

    public void setUsedByEnrichments(List<String> usedByEnrichments) {
        this.usedByEnrichments = usedByEnrichments;
    }

    public List<String> getUsedByEnrichmentGroups() {
        return usedByEnrichmentGroups;
    }

    public void setUsedByEnrichmentGroups(List<String> usedByEnrichmentGroups) {
        this.usedByEnrichmentGroups = usedByEnrichmentGroups;
    }

    public int getTotalUsageCount() {
        return totalUsageCount;
    }

    public void setTotalUsageCount(int totalUsageCount) {
        this.totalUsageCount = totalUsageCount;
    }
    
    // Utility methods
    
    public void addDefinedInFile(String filePath) {
        if (!this.definedInFiles.contains(filePath)) {
            this.definedInFiles.add(filePath);
        }
    }
    
    public void addUsedByRule(String ruleId) {
        if (!this.usedByRules.contains(ruleId)) {
            this.usedByRules.add(ruleId);
            this.totalUsageCount++;
        }
    }
    
    public void addUsedByRuleGroup(String ruleGroupId) {
        if (!this.usedByRuleGroups.contains(ruleGroupId)) {
            this.usedByRuleGroups.add(ruleGroupId);
            this.totalUsageCount++;
        }
    }
    
    public void addUsedByEnrichment(String enrichmentId) {
        if (!this.usedByEnrichments.contains(enrichmentId)) {
            this.usedByEnrichments.add(enrichmentId);
            this.totalUsageCount++;
        }
    }
    
    public void addUsedByEnrichmentGroup(String enrichmentGroupId) {
        if (!this.usedByEnrichmentGroups.contains(enrichmentGroupId)) {
            this.usedByEnrichmentGroups.add(enrichmentGroupId);
            this.totalUsageCount++;
        }
    }

    @Override
    public String toString() {
        return "CategorySummary{" +
                "name='" + name + '\'' +
                ", businessDomain='" + businessDomain + '\'' +
                ", businessOwner='" + businessOwner + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                ", totalUsageCount=" + totalUsageCount +
                ", definedInFiles=" + definedInFiles.size() +
                '}';
    }
}

