package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.config.yaml.YamlEnrichment;

import java.util.*;

/**
 * Core model representing a group of enrichments to be executed with
 * AND/OR semantics, optional short-circuiting, and optional parallel execution.
 *
 * Phase 1: data model + simple helpers. Execution wiring comes in later phases.
 */
public class EnrichmentGroup {
    private final String id;
    private String name;
    private String description;
    private boolean andOperator = true; // true = AND, false = OR
    private boolean stopOnFirstFailure = true;
    private boolean parallelExecution = false;
    private boolean debugMode = false;
    private Integer priority; // lower is higher priority

    // Sequence -> Enrichment
    private final Map<Integer, YamlEnrichment> enrichmentsBySequence = new HashMap<>();

    public EnrichmentGroup(String id) {
        this.id = Objects.requireNonNull(id, "id");
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
}

