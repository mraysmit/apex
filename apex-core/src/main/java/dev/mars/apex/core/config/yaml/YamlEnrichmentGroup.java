package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * YAML mapping for enrichment groups (Phase 1).
 * Mirrors YamlRuleGroup structure but for enrichments.
 */
public class YamlEnrichmentGroup {

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

    // operator: "AND" or "OR" (default AND)
    @JsonProperty("operator")
    private String operator;

    @JsonProperty("stop-on-first-failure")
    private Boolean stopOnFirstFailure;

    @JsonProperty("parallel-execution")
    private Boolean parallelExecution;

    @JsonProperty("debug-mode")
    private Boolean debugMode;

    // Simple list of enrichment ids
    @JsonProperty("enrichment-ids")
    private List<String> enrichmentIds;

    // Structured references with sequence and overrides
    @JsonProperty("enrichment-references")
    private List<EnrichmentReference> enrichmentReferences;

    // Ability to compose groups hierarchically
    @JsonProperty("enrichment-group-references")
    private List<String> enrichmentGroupReferences;

    // Reserved for future dependency DAGs
    @JsonProperty("depends-on")
    private List<String> dependsOn;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Enterprise metadata fields for category inheritance
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

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getPriority() { return priority; }
    public Boolean getEnabled() { return enabled; }
    public String getOperator() { return operator; }
    public Boolean getStopOnFirstFailure() { return stopOnFirstFailure; }
    public Boolean getParallelExecution() { return parallelExecution; }
    public Boolean getDebugMode() { return debugMode; }
    public List<String> getEnrichmentIds() { return enrichmentIds; }
    public List<EnrichmentReference> getEnrichmentReferences() { return enrichmentReferences; }
    public List<String> getEnrichmentGroupReferences() { return enrichmentGroupReferences; }
    public List<String> getDependsOn() { return dependsOn; }
    public List<String> getTags() { return tags; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setStopOnFirstFailure(Boolean stopOnFirstFailure) { this.stopOnFirstFailure = stopOnFirstFailure; }
    public void setParallelExecution(Boolean parallelExecution) { this.parallelExecution = parallelExecution; }
    public void setDebugMode(Boolean debugMode) { this.debugMode = debugMode; }
    public void setEnrichmentIds(List<String> enrichmentIds) { this.enrichmentIds = enrichmentIds; }
    public void setEnrichmentReferences(List<EnrichmentReference> enrichmentReferences) { this.enrichmentReferences = enrichmentReferences; }
    public void setEnrichmentGroupReferences(List<String> enrichmentGroupReferences) { this.enrichmentGroupReferences = enrichmentGroupReferences; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getBusinessDomain() { return businessDomain; }
    public void setBusinessDomain(String businessDomain) { this.businessDomain = businessDomain; }

    public String getBusinessOwner() { return businessOwner; }
    public void setBusinessOwner(String businessOwner) { this.businessOwner = businessOwner; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    /**
     * Reference to an enrichment with sequence and overrides.
     */
    public static class EnrichmentReference {
        @JsonProperty("enrichment-id")
        private String enrichmentId;

        @JsonProperty("sequence")
        private Integer sequence;

        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("override-priority")
        private Integer overridePriority;

        public String getEnrichmentId() { return enrichmentId; }
        public Integer getSequence() { return sequence; }
        public Boolean getEnabled() { return enabled; }
        public Integer getOverridePriority() { return overridePriority; }

        public void setEnrichmentId(String enrichmentId) { this.enrichmentId = enrichmentId; }
        public void setSequence(Integer sequence) { this.sequence = sequence; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public void setOverridePriority(Integer overridePriority) { this.overridePriority = overridePriority; }
    }
}

