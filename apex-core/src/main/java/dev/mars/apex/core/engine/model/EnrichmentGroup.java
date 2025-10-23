package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.config.yaml.YamlEnrichment;

import java.util.*;
import java.util.UUID;

/**
 * Core model representing a group of enrichments to be executed with
 * AND/OR semantics, optional short-circuiting, and optional parallel execution.
 *
 * Phase 1: data model + simple helpers. Execution wiring comes in later phases.
 */
public class EnrichmentGroup {
    private final UUID uuid;
    private final String id;
    private final Set<Category> categories;
    private String name;
    private String description;
    private boolean andOperator = true; // true = AND, false = OR
    private boolean stopOnFirstFailure = true;
    private boolean parallelExecution = false;
    private boolean debugMode = false;
    private Integer priority; // lower is higher priority

    // Enterprise metadata fields
    private String createdBy;
    private String businessDomain;
    private String businessOwner;
    private String sourceSystem;
    private String effectiveDate;
    private String expirationDate;

    // Sequence -> Enrichment
    private final Map<Integer, YamlEnrichment> enrichmentsBySequence = new HashMap<>();

    public EnrichmentGroup(String id) {
        this.uuid = UUID.randomUUID();
        this.id = Objects.requireNonNull(id, "id");
        this.categories = new HashSet<>();
    }

    /**
     * Create a new enrichment group with a category.
     *
     * @param id The unique identifier of the enrichment group
     * @param category The initial category of the enrichment group
     */
    public EnrichmentGroup(String id, String category) {
        this.uuid = UUID.randomUUID();
        this.id = Objects.requireNonNull(id, "id");
        this.categories = new HashSet<>();
        if (category != null) {
            this.categories.add(new Category(category, 100)); // Default priority
        }
    }

    /**
     * Create a new enrichment group with multiple categories.
     *
     * @param id The unique identifier of the enrichment group
     * @param categories The set of category objects this enrichment group belongs to
     */
    public EnrichmentGroup(String id, Set<Category> categories) {
        this.uuid = UUID.randomUUID();
        this.id = Objects.requireNonNull(id, "id");
        this.categories = new HashSet<>(categories != null ? categories : Collections.emptySet());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isAndOperator() { return andOperator; }
    public boolean isStopOnFirstFailure() { return stopOnFirstFailure; }
    public boolean isParallelExecution() { return parallelExecution; }
    public boolean isDebugMode() { return debugMode; }
    public Integer getPriority() { return priority; }

    public EnrichmentGroup setName(String name) { this.name = name; return this; }
    public EnrichmentGroup setDescription(String description) { this.description = description; return this; }
    public EnrichmentGroup setAndOperator(boolean andOperator) { this.andOperator = andOperator; return this; }
    public EnrichmentGroup setStopOnFirstFailure(boolean stopOnFirstFailure) { this.stopOnFirstFailure = stopOnFirstFailure; return this; }
    public EnrichmentGroup setParallelExecution(boolean parallelExecution) { this.parallelExecution = parallelExecution; return this; }
    public EnrichmentGroup setDebugMode(boolean debugMode) { this.debugMode = debugMode; return this; }
    public EnrichmentGroup setPriority(Integer priority) { this.priority = priority; return this; }

    public void addEnrichment(int sequence, YamlEnrichment enrichment) {
        if (enrichment == null) return;
        enrichmentsBySequence.put(sequence, enrichment);
    }

    public List<YamlEnrichment> getEnrichmentsInOrder() {
        return enrichmentsBySequence.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    public Map<Integer, YamlEnrichment> getEnrichmentsBySequence() {
        return Collections.unmodifiableMap(new TreeMap<>(enrichmentsBySequence));
    }

    public UUID getUuid() {
        return uuid;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    /**
     * Add a category to this enrichment group.
     *
     * @param category The category to add
     */
    public void addCategory(Category category) {
        if (category != null) {
            this.categories.add(category);
        }
    }

    // Enterprise metadata getters and setters
    public String getCreatedBy() {
        return createdBy;
    }

    public EnrichmentGroup setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public EnrichmentGroup setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
        return this;
    }

    public String getBusinessOwner() {
        return businessOwner;
    }

    public EnrichmentGroup setBusinessOwner(String businessOwner) {
        this.businessOwner = businessOwner;
        return this;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public EnrichmentGroup setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
        return this;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public EnrichmentGroup setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
        return this;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public EnrichmentGroup setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }
}

