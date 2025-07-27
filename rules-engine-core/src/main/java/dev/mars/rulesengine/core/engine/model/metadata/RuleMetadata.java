package dev.mars.rulesengine.core.engine.model.metadata;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Extensible metadata container for rules that supports both standard and custom metadata.
 * 
 * Standard metadata includes:
 * - Audit trail (created/modified dates and users)
 * - Version information
 * - Business context
 * - Technical attributes
 * 
 * Custom metadata can be added through the extensible properties map.
 */
public class RuleMetadata {
    
    // === STANDARD AUDIT METADATA ===
    private final Instant createdDate;
    private final String createdByUser;
    private Instant lastModifiedDate;
    private String lastModifiedByUser;
    
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
     */
    private RuleMetadata(Builder builder) {
        // Audit metadata
        this.createdDate = builder.createdDate != null ? builder.createdDate : Instant.now();
        this.createdByUser = builder.createdByUser;
        this.lastModifiedDate = builder.lastModifiedDate != null ? builder.lastModifiedDate : this.createdDate;
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
    
    // === GETTERS ===
    
    public Instant getCreatedDate() { return createdDate; }
    public String getCreatedByUser() { return createdByUser; }
    public Instant getLastModifiedDate() { return lastModifiedDate; }
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
     */
    public RuleMetadata withModification(String modifiedByUser, String changeReason) {
        return new Builder(this)
            .lastModifiedDate(Instant.now())
            .lastModifiedByUser(modifiedByUser)
            .changeReason(changeReason)
            .build();
    }
    
    /**
     * Create a new metadata instance with a status change.
     */
    public RuleMetadata withStatus(RuleStatus newStatus, String modifiedByUser) {
        return new Builder(this)
            .status(newStatus)
            .lastModifiedDate(Instant.now())
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
        // Audit metadata
        private Instant createdDate;
        private String createdByUser;
        private Instant lastModifiedDate;
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
         */
        public Builder(RuleMetadata existing) {
            this.createdDate = existing.createdDate;
            this.createdByUser = existing.createdByUser;
            this.lastModifiedDate = existing.lastModifiedDate;
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
        
        // === AUDIT METADATA BUILDERS ===
        public Builder createdDate(Instant createdDate) { this.createdDate = createdDate; return this; }
        public Builder createdByUser(String createdByUser) { this.createdByUser = createdByUser; return this; }
        public Builder lastModifiedDate(Instant lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; return this; }
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
                "version='" + version + '\'' +
                ", status=" + status +
                ", createdBy='" + createdByUser + '\'' +
                ", createdDate=" + createdDate +
                ", lastModifiedBy='" + lastModifiedByUser + '\'' +
                ", lastModifiedDate=" + lastModifiedDate +
                ", businessOwner='" + businessOwner + '\'' +
                ", customProperties=" + customProperties.size() + " entries" +
                '}';
    }
}
