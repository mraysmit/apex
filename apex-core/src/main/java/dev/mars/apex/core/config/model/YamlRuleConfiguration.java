package dev.mars.apex.core.config.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.mars.apex.core.config.pipeline.PipelineConfiguration;
import dev.mars.apex.core.config.sequential.ProcessingItem;

import java.util.List;
import java.util.Set;

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
* This class is part of the APEX A powerful expression processor for Java applications.
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

    @JsonProperty("data-sources")
    private List<YamlDataSource> dataSources;

    @JsonProperty("data-source-refs")
    private List<YamlDataSourceRef> dataSourceRefs;

    @JsonProperty("rule-refs")
    private List<YamlRuleRef> ruleRefs;

    @JsonProperty("enrichment-refs")
    private List<YamlEnrichmentRef> enrichmentRefs;

    @JsonProperty("data-sinks")
    private List<YamlDataSink> dataSinks;

    @JsonProperty("categories")
    private List<YamlCategory> categories;
    
    @JsonProperty("rules")
    private List<YamlRule> rules;
    
    @JsonProperty("rule-groups")
    private List<YamlRuleGroup> ruleGroups;

    @JsonProperty("enrichments")
    private List<YamlEnrichment> enrichments;

    @JsonProperty("enrichment-groups")
    private List<YamlEnrichmentGroup> enrichmentGroups;

    @JsonProperty("transformations")
    private List<YamlTransformation> transformations;

    @JsonProperty("rule-chains")
    private List<YamlRuleChain> ruleChains;

    @JsonProperty("pipeline")
    private PipelineConfiguration pipeline;

    @JsonProperty("error-recovery")
    private YamlErrorRecoveryConfig errorRecovery;

    @JsonProperty("scenario")
    private Object scenarioData;

    /**
     * Section order as it appears in the YAML document.
     * This field is populated by OrderedYamlParser to enable sequential processing.
     * Not serialized to YAML - only used internally for execution order.
     */
    private List<String> sectionOrder;

    /**
     * Item-level order as items appear in the YAML document.
     * This field is populated by OrderedYamlParser to enable item-level sequential processing
     * where items from different sections can be interleaved in document order.
     * Not serialized to YAML - only used internally for execution order.
     */
    private List<ProcessingItem> itemOrder;

    /**
     * Track which enrichment IDs came from external references.
     * Used to expand reference placeholders in item order.
     * Not serialized to YAML - only used internally for reference tracking.
     */
    private Set<String> referencedEnrichmentIds;

    /**
     * Track which rule IDs came from external references.
     * Used to expand reference placeholders in item order.
     * Not serialized to YAML - only used internally for reference tracking.
     */
    private Set<String> referencedRuleIds;

    /**
     * Track which enrichment group IDs came from external references.
     * Used to expand reference placeholders in item order.
     * Not serialized to YAML - only used internally for reference tracking.
     */
    private Set<String> referencedEnrichmentGroupIds;

    /**
     * Track which rule group IDs came from external references.
     * Used to expand reference placeholders in item order.
     * Not serialized to YAML - only used internally for reference tracking.
     */
    private Set<String> referencedRuleGroupIds;

    // Default constructor
    public YamlRuleConfiguration() {}
    
    // Getters and setters
    public ConfigurationMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ConfigurationMetadata metadata) {
        this.metadata = metadata;
    }

    public List<YamlDataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<YamlDataSource> dataSources) {
        this.dataSources = dataSources;
    }

    public List<YamlDataSourceRef> getDataSourceRefs() {
        return dataSourceRefs;
    }

    public void setDataSourceRefs(List<YamlDataSourceRef> dataSourceRefs) {
        this.dataSourceRefs = dataSourceRefs;
    }

    public List<YamlRuleRef> getRuleRefs() {
        return ruleRefs;
    }

    public void setRuleRefs(List<YamlRuleRef> ruleRefs) {
        this.ruleRefs = ruleRefs;
    }

    public List<YamlEnrichmentRef> getEnrichmentRefs() {
        return enrichmentRefs;
    }

    public void setEnrichmentRefs(List<YamlEnrichmentRef> enrichmentRefs) {
        this.enrichmentRefs = enrichmentRefs;
    }

    public List<YamlDataSink> getDataSinks() {
        return dataSinks;
    }

    public void setDataSinks(List<YamlDataSink> dataSinks) {
        this.dataSinks = dataSinks;
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

    public List<YamlEnrichmentGroup> getEnrichmentGroups() {
        return enrichmentGroups;
    }

    public void setEnrichmentGroups(List<YamlEnrichmentGroup> enrichmentGroups) {
        this.enrichmentGroups = enrichmentGroups;
    }

    public List<YamlTransformation> getTransformations() {
        return transformations;
    }
    
    public void setTransformations(List<YamlTransformation> transformations) {
        this.transformations = transformations;
    }

    public List<YamlRuleChain> getRuleChains() {
        return ruleChains;
    }

    public void setRuleChains(List<YamlRuleChain> ruleChains) {
        this.ruleChains = ruleChains;
    }

    public PipelineConfiguration getPipeline() {
        return pipeline;
    }

    public void setPipeline(PipelineConfiguration pipeline) {
        this.pipeline = pipeline;
    }

    public YamlErrorRecoveryConfig getErrorRecovery() {
        return errorRecovery;
    }

    public void setErrorRecovery(YamlErrorRecoveryConfig errorRecovery) {
        this.errorRecovery = errorRecovery;
    }

    public Object getScenarioData() {
        return scenarioData;
    }

    public void setScenarioData(Object scenarioData) {
        this.scenarioData = scenarioData;
    }

    /**
     * Checks if this configuration contains a scenario section.
     *
     * @return true if scenario data is present
     */
    public boolean hasScenario() {
        return scenarioData != null;
    }

    /**
     * Checks if this configuration contains processing stages within the scenario.
     *
     * @return true if scenario has processing-stages defined
     */
    @SuppressWarnings("unchecked")
    public boolean hasProcessingStages() {
        if (!hasScenario() || !(scenarioData instanceof java.util.Map)) {
            return false;
        }

        java.util.Map<String, Object> scenarioMap = (java.util.Map<String, Object>) scenarioData;
        Object stages = scenarioMap.get("processing-stages");
        return stages instanceof java.util.List && !((java.util.List<?>) stages).isEmpty();
    }

    /**
     * Metadata about the configuration file.
     */
    public static class ConfigurationMetadata {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;
        
        @JsonProperty("description")
        private String description;

        @JsonProperty("type")
        private String type;

        @JsonProperty("author")
        private String author;
        
        @JsonProperty("created")
        @JsonAlias("created-date")
        private String created;
        
        @JsonProperty("last-modified")
        private String lastModified;
        
        @JsonProperty("tags")
        private List<String> tags;

        @JsonProperty("processing-mode")
        private String processingMode;

        // Default constructor
        public ConfigurationMetadata() {}

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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

        public String getProcessingMode() {
            return processingMode;
        }

        public void setProcessingMode(String processingMode) {
            this.processingMode = processingMode;
        }
    }

    /**
     * Get the section order as it appears in the YAML document.
     *
     * @return List of section names in document order, or null if not available
     */
    public List<String> getSectionOrder() {
        return sectionOrder;
    }

    /**
     * Set the section order from the YAML document.
     * This is called by YamlConfigurationLoader after parsing with OrderedYamlParser.
     *
     * @param sectionOrder List of section names in document order
     */
    public void setSectionOrder(List<String> sectionOrder) {
        this.sectionOrder = sectionOrder;
    }

    /**
     * Get the item-level order as items appear in the YAML document.
     * This enables item-level sequential processing where items from different
     * sections can be interleaved in document order (e.g., E1 → R1 → E2 → R2).
     *
     * @return List of processing items in document order, or null if not available
     */
    public List<ProcessingItem> getItemOrder() {
        return itemOrder;
    }

    /**
     * Set the item-level order from the YAML document.
     * This is called by YamlConfigurationLoader after parsing with OrderedYamlParser.
     *
     * @param itemOrder List of processing items in document order
     */
    public void setItemOrder(List<ProcessingItem> itemOrder) {
        this.itemOrder = itemOrder;
    }

    /**
     * Get the set of enrichment IDs that came from external references.
     * Used to expand reference placeholders in item order.
     *
     * @return Set of referenced enrichment IDs, or null if not tracked
     */
    public Set<String> getReferencedEnrichmentIds() {
        return referencedEnrichmentIds;
    }

    /**
     * Set the enrichment IDs that came from external references.
     * This is called by YamlConfigurationLoader during reference processing.
     *
     * @param referencedEnrichmentIds Set of referenced enrichment IDs
     */
    public void setReferencedEnrichmentIds(Set<String> referencedEnrichmentIds) {
        this.referencedEnrichmentIds = referencedEnrichmentIds;
    }

    /**
     * Get the set of rule IDs that came from external references.
     * Used to expand reference placeholders in item order.
     *
     * @return Set of referenced rule IDs, or null if not tracked
     */
    public Set<String> getReferencedRuleIds() {
        return referencedRuleIds;
    }

    /**
     * Set the rule IDs that came from external references.
     * This is called by YamlConfigurationLoader during reference processing.
     *
     * @param referencedRuleIds Set of referenced rule IDs
     */
    public void setReferencedRuleIds(Set<String> referencedRuleIds) {
        this.referencedRuleIds = referencedRuleIds;
    }

    /**
     * Get the set of enrichment group IDs that came from external references.
     * Used to expand reference placeholders in item order.
     *
     * @return Set of referenced enrichment group IDs, or null if not tracked
     */
    public Set<String> getReferencedEnrichmentGroupIds() {
        return referencedEnrichmentGroupIds;
    }

    /**
     * Set the enrichment group IDs that came from external references.
     * This is called by YamlConfigurationLoader during reference processing.
     *
     * @param referencedEnrichmentGroupIds Set of referenced enrichment group IDs
     */
    public void setReferencedEnrichmentGroupIds(Set<String> referencedEnrichmentGroupIds) {
        this.referencedEnrichmentGroupIds = referencedEnrichmentGroupIds;
    }

    /**
     * Get the set of rule group IDs that came from external references.
     * Used to expand reference placeholders in item order.
     *
     * @return Set of referenced rule group IDs, or null if not tracked
     */
    public Set<String> getReferencedRuleGroupIds() {
        return referencedRuleGroupIds;
    }

    /**
     * Set the rule group IDs that came from external references.
     * This is called by YamlConfigurationLoader during reference processing.
     *
     * @param referencedRuleGroupIds Set of referenced rule group IDs
     */
    public void setReferencedRuleGroupIds(Set<String> referencedRuleGroupIds) {
        this.referencedRuleGroupIds = referencedRuleGroupIds;
    }
}
