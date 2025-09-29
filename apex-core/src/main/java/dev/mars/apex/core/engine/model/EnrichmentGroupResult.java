package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.constants.SeverityConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of evaluating an EnrichmentGroup.
 * Reuses RuleResult for per-enrichment outcomes and severity aggregation.
 */
public class EnrichmentGroupResult {
    private final String groupId;
    private final boolean success;
    private final String message;
    private final List<RuleResult> enrichmentResults;
    private final long executionTimeMs;
    private final String aggregatedSeverity;

    public static EnrichmentGroupResult of(String groupId,
                                           boolean success,
                                           String message,
                                           List<RuleResult> enrichmentResults,
                                           long executionTimeMs) {
        String severity = aggregateSeverity(enrichmentResults);
        return new EnrichmentGroupResult(groupId, success, message, enrichmentResults, executionTimeMs, severity);
    }

    public EnrichmentGroupResult(String groupId,
                                 boolean success,
                                 String message,
                                 List<RuleResult> enrichmentResults,
                                 long executionTimeMs,
                                 String aggregatedSeverity) {
        this.groupId = groupId;
        this.success = success;
        this.message = message;
        this.enrichmentResults = enrichmentResults != null ? new ArrayList<>(enrichmentResults) : new ArrayList<>();
        this.executionTimeMs = executionTimeMs;
        this.aggregatedSeverity = aggregatedSeverity != null ? aggregatedSeverity : SeverityConstants.INFO;
    }

    public String getGroupId() { return groupId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public String getAggregatedSeverity() { return aggregatedSeverity; }

    public List<RuleResult> getEnrichmentResults() {
        return Collections.unmodifiableList(enrichmentResults);
    }

    private static String aggregateSeverity(List<RuleResult> results) {
        if (results == null || results.isEmpty()) {
            return SeverityConstants.INFO;
        }
        boolean hasError = results.stream().filter(Objects::nonNull)
                .anyMatch(r -> SeverityConstants.ERROR.equalsIgnoreCase(r.getSeverity()));
        if (hasError) return SeverityConstants.ERROR;
        boolean hasWarn = results.stream().filter(Objects::nonNull)
                .anyMatch(r -> SeverityConstants.WARNING.equalsIgnoreCase(r.getSeverity()));
        if (hasWarn) return SeverityConstants.WARNING;
        boolean hasInfo = results.stream().filter(Objects::nonNull)
                .anyMatch(r -> SeverityConstants.INFO.equalsIgnoreCase(r.getSeverity()));
        return hasInfo ? SeverityConstants.INFO : SeverityConstants.INFO;
    }

    @Override
    public String toString() {
        return "EnrichmentGroupResult{" +
                "groupId='" + groupId + '\'' +
                ", success=" + success +
                ", aggregatedSeverity='" + aggregatedSeverity + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}

