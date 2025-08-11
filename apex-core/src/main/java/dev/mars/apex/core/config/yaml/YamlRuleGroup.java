package dev.mars.apex.core.config.yaml;

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
 * YAML representation of a rule group configuration.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * YAML representation of a rule group configuration.
 * This class maps to the YAML structure for defining rule groups.
 */
public class YamlRuleGroup {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("categories")
    private List<String> categories;
    
    @JsonProperty("priority")
    private Integer priority;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("stop-on-first-failure")
    private Boolean stopOnFirstFailure;
    
    @JsonProperty("parallel-execution")
    private Boolean parallelExecution;
    
    @JsonProperty("rule-ids")
    private List<String> ruleIds;
    
    @JsonProperty("rule-references")
    private List<RuleReference> ruleReferences;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("execution-config")
    private ExecutionConfig executionConfig;
    
    // Default constructor
    public YamlRuleGroup() {
        this.enabled = true; // Default to enabled
        this.priority = 100; // Default priority
        this.stopOnFirstFailure = false; // Default to continue on failure
        this.parallelExecution = false; // Default to sequential execution
    }
    
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
    
    public List<String> getRuleIds() {
        return ruleIds;
    }
    
    public void setRuleIds(List<String> ruleIds) {
        this.ruleIds = ruleIds;
    }
    
    public List<RuleReference> getRuleReferences() {
        return ruleReferences;
    }
    
    public void setRuleReferences(List<RuleReference> ruleReferences) {
        this.ruleReferences = ruleReferences;
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
    
    public ExecutionConfig getExecutionConfig() {
        return executionConfig;
    }
    
    public void setExecutionConfig(ExecutionConfig executionConfig) {
        this.executionConfig = executionConfig;
    }
    
    /**
     * Reference to a rule within a rule group.
     */
    public static class RuleReference {
        @JsonProperty("rule-id")
        private String ruleId;
        
        @JsonProperty("sequence")
        private Integer sequence;
        
        @JsonProperty("enabled")
        private Boolean enabled;
        
        @JsonProperty("override-priority")
        private Integer overridePriority;
        
        // Default constructor
        public RuleReference() {
            this.enabled = true;
        }
        
        // Getters and setters
        public String getRuleId() {
            return ruleId;
        }
        
        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }
        
        public Integer getSequence() {
            return sequence;
        }
        
        public void setSequence(Integer sequence) {
            this.sequence = sequence;
        }
        
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
        
        public Integer getOverridePriority() {
            return overridePriority;
        }
        
        public void setOverridePriority(Integer overridePriority) {
            this.overridePriority = overridePriority;
        }
    }
    
    /**
     * Execution configuration for the rule group.
     */
    public static class ExecutionConfig {
        @JsonProperty("timeout-ms")
        private Long timeoutMs;
        
        @JsonProperty("retry-count")
        private Integer retryCount;
        
        @JsonProperty("circuit-breaker")
        private Boolean circuitBreaker;
        
        // Default constructor
        public ExecutionConfig() {}
        
        // Getters and setters
        public Long getTimeoutMs() {
            return timeoutMs;
        }
        
        public void setTimeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
        
        public Integer getRetryCount() {
            return retryCount;
        }
        
        public void setRetryCount(Integer retryCount) {
            this.retryCount = retryCount;
        }
        
        public Boolean getCircuitBreaker() {
            return circuitBreaker;
        }
        
        public void setCircuitBreaker(Boolean circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }
    }
}
