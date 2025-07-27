package dev.mars.rulesengine.core.engine.model.metadata;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
 * Extensible metadata container for rules that supports both standard and custom metadata.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class RuleMetadata {
    
    // === CORE AUDIT METADATA (REQUIRED) ===
    private final Instant createdDate;      // CRITICAL: When was this rule created
    private final Instant modifiedDate;     // CRITICAL: When was this rule last modified

    // === ADDITIONAL AUDIT METADATA (OPTIONAL) ===
    private final String createdByUser;
    private final String lastModifiedByUser;
    
    // === VERSION METADATA ===
    private final String version;
    private final String previousVersion;
    private final String changeReason;
    
    // === BUSINESS METADATA ===
    private final String businessOwner;
    private final String businessDomain;
    private final String businessPurpose;
    private final RuleStatus status;
    private final Instant effectiveDate;
    private final Instant expirationDate;
    
    // === TECHNICAL METADATA ===
    private final String sourceSystem;
    private final String environment;
    private final RuleComplexity complexity;
    private final String[] tags;
    
    // === EXTENSIBLE PROPERTIES ===
    private final Map<String, Object> customProperties;
    
    /**
     * Private constructor - use Builder to create instances.
     * Ensures createdDate and modifiedDate are ALWAYS set.
     */
    private RuleMetadata(Builder builder) {
        // CORE AUDIT METADATA - Always required
        this.createdDate = builder.createdDate != null ? builder.createdDate : Instant.now();
        this.modifiedDate = builder.modifiedDate != null ? builder.modifiedDate : this.createdDate;

        // ADDITIONAL AUDIT METADATA - Optional
        this.createdByUser = builder.createdByUser;
        this.lastModifiedByUser = builder.lastModifiedByUser != null ? builder.lastModifiedByUser : this.createdByUser;
        
        // Version metadata
        this.version = builder.version != null ? builder.version : "1.0";
        this.previousVersion = builder.previousVersion;
        this.changeReason = builder.changeReason;
        
        // Business metadata
        this.businessOwner = builder.businessOwner;
        this.businessDomain = builder.businessDomain;
        this.businessPurpose = builder.businessPurpose;
        this.status = builder.status != null ? builder.status : RuleStatus.ACTIVE;
        this.effectiveDate = builder.effectiveDate;
        this.expirationDate = builder.expirationDate;
        
        // Technical metadata
        this.sourceSystem = builder.sourceSystem;
        this.environment = builder.environment;
        this.complexity = builder.complexity != null ? builder.complexity : RuleComplexity.MEDIUM;
        this.tags = builder.tags != null ? builder.tags.clone() : new String[0];
        
        // Custom properties
        this.customProperties = new HashMap<>(builder.customProperties);
    }
    
    // === CORE GETTERS (ALWAYS AVAILABLE) ===

    /**
     * Get the creation date - ALWAYS available, never null.
     * This is the most critical audit attribute.
     */
    public Instant getCreatedDate() { return createdDate; }

    /**
     * Get the last modification date - ALWAYS available, never null.
     * This is the second most critical audit attribute.
     */
    public Instant getModifiedDate() { return modifiedDate; }

    // === ADDITIONAL GETTERS (MAY BE NULL) ===

    public String getCreatedByUser() { return createdByUser; }
    public String getLastModifiedByUser() { return lastModifiedByUser; }
    
    public String getVersion() { return version; }
    public Optional<String> getPreviousVersion() { return Optional.ofNullable(previousVersion); }
    public Optional<String> getChangeReason() { return Optional.ofNullable(changeReason); }
    
    public Optional<String> getBusinessOwner() { return Optional.ofNullable(businessOwner); }
    public Optional<String> getBusinessDomain() { return Optional.ofNullable(businessDomain); }
    public Optional<String> getBusinessPurpose() { return Optional.ofNullable(businessPurpose); }
    public RuleStatus getStatus() { return status; }
    public Optional<Instant> getEffectiveDate() { return Optional.ofNullable(effectiveDate); }
    public Optional<Instant> getExpirationDate() { return Optional.ofNullable(expirationDate); }
    
    public Optional<String> getSourceSystem() { return Optional.ofNullable(sourceSystem); }
    public Optional<String> getEnvironment() { return Optional.ofNullable(environment); }
    public RuleComplexity getComplexity() { return complexity; }
    public String[] getTags() { return tags.clone(); }
    
    // === CUSTOM PROPERTIES ===
    
    /**
     * Get a custom property value.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getCustomProperty(String key, Class<T> type) {
        Object value = customProperties.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Get all custom properties.
     */
    public Map<String, Object> getCustomProperties() {
        return new HashMap<>(customProperties);
    }
    
    /**
     * Check if a custom property exists.
     */
    public boolean hasCustomProperty(String key) {
        return customProperties.containsKey(key);
    }
    
    // === MODIFICATION METHODS ===
    
    /**
     * Create a new metadata instance with updated modification info.
     * This preserves immutability while allowing updates.
     * CRITICAL: Always updates modifiedDate to current time.
     */
    public RuleMetadata withModification(String modifiedByUser, String changeReason) {
        return new Builder(this)
            .modifiedDate(Instant.now())  // CRITICAL: Always update modification time
            .lastModifiedByUser(modifiedByUser)
            .changeReason(changeReason)
            .build();
    }
    
    /**
     * Create a new metadata instance with a status change.
     * CRITICAL: Always updates modifiedDate to current time.
     */
    public RuleMetadata withStatus(RuleStatus newStatus, String modifiedByUser) {
        return new Builder(this)
            .status(newStatus)
            .modifiedDate(Instant.now())  // CRITICAL: Always update modification time
            .lastModifiedByUser(modifiedByUser)
            .build();
    }
    
    // === BUILDER PATTERN ===
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(RuleMetadata existing) {
        return new Builder(existing);
    }
    
    public static class Builder {
        // CORE AUDIT METADATA (CRITICAL)
        private Instant createdDate;
        private Instant modifiedDate;

        // ADDITIONAL AUDIT METADATA
        private String createdByUser;
        private String lastModifiedByUser;
        
        // Version metadata
        private String version;
        private String previousVersion;
        private String changeReason;
        
        // Business metadata
        private String businessOwner;
        private String businessDomain;
        private String businessPurpose;
        private RuleStatus status;
        private Instant effectiveDate;
        private Instant expirationDate;
        
        // Technical metadata
        private String sourceSystem;
        private String environment;
        private RuleComplexity complexity;
        private String[] tags;
        
        // Custom properties
        private Map<String, Object> customProperties = new HashMap<>();
        
        public Builder() {}
        
        /**
         * Copy constructor for creating modified versions.
         * Preserves the critical createdDate and modifiedDate.
         */
        public Builder(RuleMetadata existing) {
            this.createdDate = existing.createdDate;
            this.modifiedDate = existing.modifiedDate;
            this.createdByUser = existing.createdByUser;
            this.lastModifiedByUser = existing.lastModifiedByUser;
            
            this.version = existing.version;
            this.previousVersion = existing.previousVersion;
            this.changeReason = existing.changeReason;
            
            this.businessOwner = existing.businessOwner;
            this.businessDomain = existing.businessDomain;
            this.businessPurpose = existing.businessPurpose;
            this.status = existing.status;
            this.effectiveDate = existing.effectiveDate;
            this.expirationDate = existing.expirationDate;
            
            this.sourceSystem = existing.sourceSystem;
            this.environment = existing.environment;
            this.complexity = existing.complexity;
            this.tags = existing.tags.clone();
            
            this.customProperties = new HashMap<>(existing.customProperties);
        }
        
        // === CORE AUDIT METADATA BUILDERS (CRITICAL) ===

        /**
         * Set the creation date - CRITICAL audit attribute.
         * If not set, defaults to current time.
         */
        public Builder createdDate(Instant createdDate) { this.createdDate = createdDate; return this; }

        /**
         * Set the modification date - CRITICAL audit attribute.
         * If not set, defaults to creation date.
         */
        public Builder modifiedDate(Instant modifiedDate) { this.modifiedDate = modifiedDate; return this; }

        // === ADDITIONAL AUDIT METADATA BUILDERS ===
        public Builder createdByUser(String createdByUser) { this.createdByUser = createdByUser; return this; }
        public Builder lastModifiedByUser(String lastModifiedByUser) { this.lastModifiedByUser = lastModifiedByUser; return this; }
        
        // === VERSION METADATA BUILDERS ===
        public Builder version(String version) { this.version = version; return this; }
        public Builder previousVersion(String previousVersion) { this.previousVersion = previousVersion; return this; }
        public Builder changeReason(String changeReason) { this.changeReason = changeReason; return this; }
        
        // === BUSINESS METADATA BUILDERS ===
        public Builder businessOwner(String businessOwner) { this.businessOwner = businessOwner; return this; }
        public Builder businessDomain(String businessDomain) { this.businessDomain = businessDomain; return this; }
        public Builder businessPurpose(String businessPurpose) { this.businessPurpose = businessPurpose; return this; }
        public Builder status(RuleStatus status) { this.status = status; return this; }
        public Builder effectiveDate(Instant effectiveDate) { this.effectiveDate = effectiveDate; return this; }
        public Builder expirationDate(Instant expirationDate) { this.expirationDate = expirationDate; return this; }
        
        // === TECHNICAL METADATA BUILDERS ===
        public Builder sourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; return this; }
        public Builder environment(String environment) { this.environment = environment; return this; }
        public Builder complexity(RuleComplexity complexity) { this.complexity = complexity; return this; }
        public Builder tags(String... tags) { this.tags = tags; return this; }
        
        // === CUSTOM PROPERTIES BUILDERS ===
        public Builder customProperty(String key, Object value) { 
            this.customProperties.put(key, value); 
            return this; 
        }
        
        public Builder customProperties(Map<String, Object> properties) { 
            this.customProperties.putAll(properties); 
            return this; 
        }
        
        public RuleMetadata build() {
            return new RuleMetadata(this);
        }
    }
    
    @Override
    public String toString() {
        return "RuleMetadata{" +
                "createdDate=" + createdDate +           // CRITICAL: Show first
                ", modifiedDate=" + modifiedDate +       // CRITICAL: Show second
                ", version='" + version + '\'' +
                ", status=" + status +
                ", createdBy='" + createdByUser + '\'' +
                ", lastModifiedBy='" + lastModifiedByUser + '\'' +
                ", businessOwner='" + businessOwner + '\'' +
                ", customProperties=" + customProperties.size() + " entries" +
                '}';
    }
}
