/*
 * Copyright (c) 2025-2026 Mars Software - All Rights Reserved.
 *
 * This file is part of the APEX Rules Engine.
 * Unauthorized copying or distribution is prohibited.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.4
 * @created 2026-03-04
 */
package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.constants.SeverityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Detects enrichment failures (missing required fields) and aggregates severity
 * levels across a list of processed enrichments.
 *
 * <p>Extracted from {@link EnrichmentProcessor} (Phase 13 decomposition) to isolate
 * result analysis from enrichment orchestration.</p>
 *
 * @since 2.4
 */
public class EnrichmentResultBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentResultBuilder.class);

    // ─── Failure Detection ───────────────────────────────────────────────

    /**
     * Detect enrichment failures by checking if required fields were successfully enriched.
     *
     * @param enrichments  The list of enrichments that were processed
     * @param enrichedData The enriched data map
     * @return true if enrichment failures were detected, false otherwise
     */
    public boolean detectEnrichmentFailures(List<YamlEnrichment> enrichments, Map<String, Object> enrichedData) {
        if (enrichments == null || enrichments.isEmpty()) {
            return false;
        }

        boolean hasFailures = false;

        for (YamlEnrichment enrichment : enrichments) {
            if (enrichment.getFieldMappings() != null) {
                for (YamlEnrichment.FieldMapping mapping : enrichment.getFieldMappings()) {
                    if (mapping.getRequired() != null && mapping.getRequired()) {
                        String targetField = mapping.getTargetField();

                        if (!enrichedData.containsKey(targetField) || enrichedData.get(targetField) == null) {
                            logger.debug("Required field '" + targetField + "' is missing from enriched data");
                            hasFailures = true;
                        }
                    }
                }
            }
        }

        return hasFailures;
    }

    // ─── Severity Aggregation ────────────────────────────────────────────

    /**
     * Aggregate severity from a list of enrichments.
     *
     * @param enrichments    The list of enrichments that were processed
     * @param overallSuccess Whether the enrichment processing was successful
     * @return The aggregated severity level
     */
    public String aggregateEnrichmentSeverity(List<YamlEnrichment> enrichments, boolean overallSuccess) {
        if (enrichments == null || enrichments.isEmpty()) {
            return SeverityConstants.INFO;
        }

        if (!overallSuccess) {
            return SeverityConstants.ERROR;
        }

        String highestSeverity = SeverityConstants.INFO;
        int highestPriority = SeverityConstants.SEVERITY_PRIORITY.get(SeverityConstants.INFO);

        for (YamlEnrichment enrichment : enrichments) {
            String enrichmentSeverity = enrichment.getSeverity();
            if (enrichmentSeverity == null) {
                enrichmentSeverity = SeverityConstants.INFO;
            }

            Integer priority = SeverityConstants.SEVERITY_PRIORITY.get(enrichmentSeverity);
            if (priority != null && priority > highestPriority) {
                highestSeverity = enrichmentSeverity;
                highestPriority = priority;
            }
        }

        logger.debug("Aggregated enrichment severity: " + highestSeverity + " from " + enrichments.size() + " enrichments");
        return highestSeverity;
    }
}
