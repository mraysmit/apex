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
 * Root configuration class for YAML-based rules configuration.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Root configuration class for YAML-based rules configuration.
 * This class represents the top-level structure of a YAML rules configuration file.
 */
public class YamlRuleConfiguration {
    
    @JsonProperty("metadata")
    private ConfigurationMetadata metadata;
    
    @JsonProperty("categories")
    private List<YamlCategory> categories;
    
    @JsonProperty("rules")
    private List<YamlRule> rules;
    
    @JsonProperty("rule-groups")
    private List<YamlRuleGroup> ruleGroups;
    
    @JsonProperty("enrichments")
    private List<YamlEnrichment> enrichments;
    
    @JsonProperty("transformations")
    private List<YamlTransformation> transformations;
    
    // Default constructor
    public YamlRuleConfiguration() {}
    
    // Getters and setters
    public ConfigurationMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ConfigurationMetadata metadata) {
        this.metadata = metadata;
    }
    
    public List<YamlCategory> getCategories() {
        return categories;
    }
    
    public void setCategories(List<YamlCategory> categories) {
        this.categories = categories;
    }
    
    public List<YamlRule> getRules() {
        return rules;
    }
    
    public void setRules(List<YamlRule> rules) {
        this.rules = rules;
    }
    
    public List<YamlRuleGroup> getRuleGroups() {
        return ruleGroups;
    }
    
    public void setRuleGroups(List<YamlRuleGroup> ruleGroups) {
        this.ruleGroups = ruleGroups;
    }
    
    public List<YamlEnrichment> getEnrichments() {
        return enrichments;
    }
    
    public void setEnrichments(List<YamlEnrichment> enrichments) {
        this.enrichments = enrichments;
    }
    
    public List<YamlTransformation> getTransformations() {
        return transformations;
    }
    
    public void setTransformations(List<YamlTransformation> transformations) {
        this.transformations = transformations;
    }
    
    /**
     * Metadata about the configuration file.
     */
    public static class ConfigurationMetadata {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("description")
        private String description;
        
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
    }
}
