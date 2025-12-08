package dev.mars.apex.core.config.component;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/**
 * Configuration class representing an APEX component.
 *
 * A component is a grouping container that allows multiple YAML configuration files
 * to be organized together and referenced as a single unit in scenario processing stages.
 *
 * COMPONENT STRUCTURE:
 * - Component identification and metadata
 * - File references with optional execution order and failure policies
 * - Support for nested components (max depth 5 levels)
 * - Rich metadata for governance and monitoring
 *
 * EXECUTION ORDER:
 * - If execution-order is specified: Files execute in numerical order
 * - If execution-order is NOT specified: Files execute in document order (APEX default)
 * - Mixed mode supported: Some files with explicit order, others using document order
 *
 * FAILURE POLICIES:
 * - File-level failure-policy overrides stage-level policy
 * - If not specified, inherits from stage-level failure-policy
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.2.0
 */
public class ComponentConfiguration {

    // Metadata section
    @JsonProperty("metadata")
    private Metadata metadata;

    // File references with execution order and failure policy
    @JsonProperty("rule-configurations")
    private List<FileReference> ruleConfigurations;

    @JsonProperty("enrichment-refs")
    private List<FileReference> enrichmentRefs;

    @JsonProperty("component-refs")
    private List<FileReference> componentRefs;

    @JsonProperty("config-files")
    private List<FileReference> configFiles;

    // Constructors
    public ComponentConfiguration() {
        this.ruleConfigurations = new ArrayList<>();
        this.enrichmentRefs = new ArrayList<>();
        this.componentRefs = new ArrayList<>();
        this.configFiles = new ArrayList<>();
        this.metadata = new Metadata();
    }

    /**
     * Nested class representing component metadata.
     */
    public static class Metadata {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;  // Must be "component"

        @JsonProperty("version")
        private String version;

        @JsonProperty("description")
        private String description;

        @JsonProperty("business-domain")
        private String businessDomain;

        @JsonProperty("owner")
        private String owner;

        @JsonProperty("criticality")
        private String criticality;

        @JsonProperty("sla-ms")
        private Integer slaMs;

        @JsonProperty("tags")
        private List<String> tags;

        @JsonProperty("documentation-url")
        private String documentationUrl;

        @JsonProperty("author")
        private String author;

        @JsonProperty("created")
        private String created;

        @JsonProperty("enabled")
        private Boolean enabled;  // Default: true if not specified

        public Metadata() {
            this.tags = new ArrayList<>();
            this.enabled = true;  // Default to enabled
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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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

        public String getBusinessDomain() {
            return businessDomain;
        }

        public void setBusinessDomain(String businessDomain) {
            this.businessDomain = businessDomain;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getCriticality() {
            return criticality;
        }

        public void setCriticality(String criticality) {
            this.criticality = criticality;
        }

        public Integer getSlaMs() {
            return slaMs;
        }

        public void setSlaMs(Integer slaMs) {
            this.slaMs = slaMs;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags != null ? tags : new ArrayList<>();
        }

        public String getDocumentationUrl() {
            return documentationUrl;
        }

        public void setDocumentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
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

        /**
         * Check if this component is enabled.
         * Components are enabled by default if not explicitly specified.
         *
         * @return true if enabled (default), false if explicitly disabled
         */
        public boolean isEnabled() {
            return enabled == null || enabled;
        }

        /**
         * Set whether this component is enabled.
         *
         * @param enabled true to enable, false to disable
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Nested class representing a file reference with optional execution order and failure policy.
     */
    public static class FileReference {
        @JsonProperty("file")
        private String file;

        @JsonProperty("execution-order")
        private Integer executionOrder;  // Optional: null means use document order

        @JsonProperty("failure-policy")
        private String failurePolicy;    // Optional: null means inherit from stage

        @JsonProperty("document-position")
        private int documentPosition;    // Internal: position in YAML document

        public FileReference() {}

        public FileReference(String file) {
            this.file = file;
        }

        public FileReference(String file, Integer executionOrder) {
            this.file = file;
            this.executionOrder = executionOrder;
        }

        public FileReference(String file, Integer executionOrder, String failurePolicy) {
            this.file = file;
            this.executionOrder = executionOrder;
            this.failurePolicy = failurePolicy;
        }

        // Getters and setters
        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public Integer getExecutionOrder() {
            return executionOrder;
        }

        public void setExecutionOrder(Integer executionOrder) {
            this.executionOrder = executionOrder;
        }

        public String getFailurePolicy() {
            return failurePolicy;
        }

        public void setFailurePolicy(String failurePolicy) {
            this.failurePolicy = failurePolicy;
        }

        public int getDocumentPosition() {
            return documentPosition;
        }

        public void setDocumentPosition(int documentPosition) {
            this.documentPosition = documentPosition;
        }

        public boolean hasExplicitExecutionOrder() {
            return executionOrder != null;
        }

        public boolean hasFailurePolicy() {
            return failurePolicy != null && !failurePolicy.trim().isEmpty();
        }

        @Override
        public String toString() {
            return "FileReference{" +
                    "file='" + file + '\'' +
                    ", executionOrder=" + executionOrder +
                    ", failurePolicy='" + failurePolicy + '\'' +
                    ", documentPosition=" + documentPosition +
                    '}';
        }
    }

    /**
     * Gets all file references from all sections, sorted by execution order or document order.
     *
     * Files with explicit execution-order are sorted numerically.
     * Files without execution-order maintain their document sequence.
     *
     * @return list of all file references sorted appropriately
     */
    public List<FileReference> getAllReferences() {
        List<FileReference> allRefs = new ArrayList<>();
        
        // Collect all references with their document positions
        int position = 0;
        for (FileReference ref : ruleConfigurations) {
            ref.setDocumentPosition(position++);
            allRefs.add(ref);
        }
        for (FileReference ref : enrichmentRefs) {
            ref.setDocumentPosition(position++);
            allRefs.add(ref);
        }
        for (FileReference ref : componentRefs) {
            ref.setDocumentPosition(position++);
            allRefs.add(ref);
        }
        for (FileReference ref : configFiles) {
            ref.setDocumentPosition(position++);
            allRefs.add(ref);
        }

        // Sort: explicit execution-order first (numerically), then document order
        allRefs.sort((ref1, ref2) -> {
            boolean hasOrder1 = ref1.hasExplicitExecutionOrder();
            boolean hasOrder2 = ref2.hasExplicitExecutionOrder();

            if (hasOrder1 && hasOrder2) {
                // Both have explicit order: sort numerically
                return Integer.compare(ref1.getExecutionOrder(), ref2.getExecutionOrder());
            } else if (hasOrder1) {
                // Only ref1 has explicit order: it comes first
                return -1;
            } else if (hasOrder2) {
                // Only ref2 has explicit order: it comes first
                return 1;
            } else {
                // Neither has explicit order: use document position
                return Integer.compare(ref1.getDocumentPosition(), ref2.getDocumentPosition());
            }
        });

        return allRefs;
    }

    /**
     * Validates the component configuration.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        // Validate metadata exists
        if (metadata == null) {
            throw new IllegalStateException("Component 'metadata' section is required");
        }

        // Validate required fields
        if (metadata.getId() == null || metadata.getId().trim().isEmpty()) {
            throw new IllegalStateException("Component 'id' is required");
        }
        if (metadata.getType() == null || !metadata.getType().equals("component")) {
            throw new IllegalStateException("Component 'type' must be 'component', found: " + metadata.getType());
        }

        // Validate that component has at least one file reference
        if (getAllReferences().isEmpty()) {
            throw new IllegalStateException("Component '" + metadata.getId() + "' must have at least one file reference");
        }

        // Validate file references
        for (FileReference ref : getAllReferences()) {
            if (ref.getFile() == null || ref.getFile().trim().isEmpty()) {
                throw new IllegalStateException("Component '" + metadata.getId() + "' has file reference with missing 'file' field");
            }

            // Validate failure policy if specified
            if (ref.hasFailurePolicy()) {
                String policy = ref.getFailurePolicy();
                if (!isValidFailurePolicy(policy)) {
                    throw new IllegalStateException(
                        "Component '" + metadata.getId() + "' has invalid failure-policy: " + policy +
                        ". Valid values: terminate, continue-with-warnings, flag-for-review");
                }
            }
        }
    }

    private boolean isValidFailurePolicy(String policy) {
        return "terminate".equals(policy) || 
               "continue-with-warnings".equals(policy) || 
               "flag-for-review".equals(policy);
    }

    // Getters and Setters
    public String getId() {
        return metadata != null ? metadata.getId() : null;
    }

    public void setId(String id) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setId(id);
    }

    public String getName() {
        return metadata != null ? metadata.getName() : null;
    }

    public void setName(String name) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setName(name);
    }

    public String getType() {
        return metadata != null ? metadata.getType() : null;
    }

    public void setType(String type) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setType(type);
    }

    public String getVersion() {
        return metadata != null ? metadata.getVersion() : null;
    }

    public void setVersion(String version) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setVersion(version);
    }

    public String getDescription() {
        return metadata != null ? metadata.getDescription() : null;
    }

    public void setDescription(String description) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setDescription(description);
    }

    public String getBusinessDomain() {
        return metadata != null ? metadata.getBusinessDomain() : null;
    }

    public void setBusinessDomain(String businessDomain) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setBusinessDomain(businessDomain);
    }

    public String getOwner() {
        return metadata != null ? metadata.getOwner() : null;
    }

    public void setOwner(String owner) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setOwner(owner);
    }

    public String getCriticality() {
        return metadata != null ? metadata.getCriticality() : null;
    }

    public void setCriticality(String criticality) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setCriticality(criticality);
    }

    public Integer getSlaMs() {
        return metadata != null ? metadata.getSlaMs() : null;
    }

    public void setSlaMs(Integer slaMs) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setSlaMs(slaMs);
    }

    public List<String> getTags() {
        return metadata != null ? metadata.getTags() : new ArrayList<>();
    }

    public void setTags(List<String> tags) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setTags(tags);
    }

    public String getDocumentationUrl() {
        return metadata != null ? metadata.getDocumentationUrl() : null;
    }

    public void setDocumentationUrl(String documentationUrl) {
        if (metadata == null) {
            metadata = new Metadata();
        }
        metadata.setDocumentationUrl(documentationUrl);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public List<FileReference> getRuleConfigurations() {
        return ruleConfigurations;
    }

    public void setRuleConfigurations(List<FileReference> ruleConfigurations) {
        this.ruleConfigurations = ruleConfigurations != null ? ruleConfigurations : new ArrayList<>();
    }

    public List<FileReference> getEnrichmentRefs() {
        return enrichmentRefs;
    }

    public void setEnrichmentRefs(List<FileReference> enrichmentRefs) {
        this.enrichmentRefs = enrichmentRefs != null ? enrichmentRefs : new ArrayList<>();
    }

    public List<FileReference> getComponentRefs() {
        return componentRefs;
    }

    public void setComponentRefs(List<FileReference> componentRefs) {
        this.componentRefs = componentRefs != null ? componentRefs : new ArrayList<>();
    }

    public List<FileReference> getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(List<FileReference> configFiles) {
        this.configFiles = configFiles != null ? configFiles : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ComponentConfiguration{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", type='" + getType() + '\'' +
                ", version='" + getVersion() + '\'' +
                ", businessDomain='" + getBusinessDomain() + '\'' +
                ", totalReferences=" + getAllReferences().size() +
                ", ruleConfigurations=" + ruleConfigurations.size() +
                ", enrichmentRefs=" + enrichmentRefs.size() +
                ", componentRefs=" + componentRefs.size() +
                ", configFiles=" + configFiles.size() +
                '}';
    }
}

