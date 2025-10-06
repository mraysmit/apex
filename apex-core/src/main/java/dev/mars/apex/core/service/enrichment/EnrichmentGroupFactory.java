package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlEnrichmentGroup;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.model.EnrichmentGroup;

import java.util.*;
import java.util.logging.Logger;

/**
 * Factory to build runtime EnrichmentGroup models from YAML configuration.
 * Phase 3: mapping only (no execution). Group references/depends-on handled later.
 */
public class EnrichmentGroupFactory {

    private static final Logger LOGGER = Logger.getLogger(EnrichmentGroupFactory.class.getName());

    /**
     * Build enrichment groups from a rule configuration.
     * - Resolves enrichments by id
     * - Maps operator (AND/OR) into boolean andOperator
     * - Applies stop-on-first-failure and parallel-execution flags
     * - Populates enrichments by sequence based on enrichment-references, or order of enrichment-ids
     */
    public static List<EnrichmentGroup> buildEnrichmentGroups(YamlRuleConfiguration config) {
        if (config == null || config.getEnrichmentGroups() == null || config.getEnrichmentGroups().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, YamlEnrichment> enrichmentById = indexEnrichments(config.getEnrichments());
        List<EnrichmentGroup> groups = new ArrayList<>();
        Map<String, EnrichmentGroup> groupsById = new HashMap<>();

        for (YamlEnrichmentGroup yg : config.getEnrichmentGroups()) {
            if (yg == null || yg.getId() == null) continue;

            EnrichmentGroup g = new EnrichmentGroup(yg.getId())
                    .setName(yg.getName())
                    .setDescription(yg.getDescription())
                    .setAndOperator(mapOperatorToAnd(yg.getOperator()))
                    .setStopOnFirstFailure(Boolean.TRUE.equals(yg.getStopOnFirstFailure()))
                    .setParallelExecution(Boolean.TRUE.equals(yg.getParallelExecution()))
                    .setDebugMode(yg.getDebugMode() != null ? yg.getDebugMode() : Boolean.parseBoolean(System.getProperty("apex.enrichmentgroup.debug", "false")))
                    .setPriority(yg.getPriority());

            // 1) Add enrichment-ids first with auto-incrementing sequence starting at 1
            if (yg.getEnrichmentIds() != null && !yg.getEnrichmentIds().isEmpty()) {
                int sequence = 1;
                for (String id : yg.getEnrichmentIds()) {
                    if (id == null) continue;
                    YamlEnrichment e = enrichmentById.get(id);
                    if (e == null) {
                        LOGGER.warning("Enrichment id not found: " + id + " in group " + yg.getId());
                        continue;
                    }
                    g.addEnrichment(sequence++, e);
                }
            }

            // 2) Then apply enrichment-references with explicit sequence (default 1), overriding positions if needed
            if (yg.getEnrichmentReferences() != null && !yg.getEnrichmentReferences().isEmpty()) {
                for (YamlEnrichmentGroup.EnrichmentReference ref : yg.getEnrichmentReferences()) {
                    if (ref == null || ref.getEnrichmentId() == null) continue;
                    if (Boolean.FALSE.equals(ref.getEnabled())) continue; // skip disabled refs
                    int sequence = ref.getSequence() != null ? ref.getSequence() : 1;
                    YamlEnrichment e = enrichmentById.get(ref.getEnrichmentId());
                    if (e == null) {
                        LOGGER.warning("Enrichment reference not found: " + ref.getEnrichmentId() + " in group " + yg.getId());
                        continue;
                    }
                    // Note: override-priority is not applied here; priority is part of enrichment itself
                    g.addEnrichment(sequence, e);
                }
            }

            groups.add(g);
            groupsById.put(yg.getId(), g);
        }

        // Second phase: process enrichment-group-references after all groups are created
        for (YamlEnrichmentGroup yg : config.getEnrichmentGroups()) {
            if (yg == null || yg.getEnrichmentGroupReferences() == null || yg.getEnrichmentGroupReferences().isEmpty()) continue;

            EnrichmentGroup targetGroup = groupsById.get(yg.getId());
            if (targetGroup == null) {
                LOGGER.warning("Target enrichment group not found: " + yg.getId());
                continue;
            }

            int nextSequence = targetGroup.getEnrichmentsBySequence().size() + 1;
            for (String referencedGroupId : yg.getEnrichmentGroupReferences()) {
                EnrichmentGroup referencedGroup = groupsById.get(referencedGroupId);
                if (referencedGroup != null) {
                    for (YamlEnrichment e : referencedGroup.getEnrichmentsInOrder()) {
                        targetGroup.addEnrichment(nextSequence++, e);
                    }
                } else {
                    String errorMsg = "Referenced enrichment group not found: " + referencedGroupId + " in group: " + yg.getId();
                    LOGGER.severe(errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            }
        }

        return groups;
    }

    private static boolean mapOperatorToAnd(String operator) {
        if (operator == null) return true; // default AND
        String op = operator.trim().toUpperCase();
        if ("AND".equals(op)) return true;
        if ("OR".equals(op)) return false;
        LOGGER.warning("Invalid operator '" + operator + "' for enrichment group. Using AND as default.");
        return true;
    }

    private static Map<String, YamlEnrichment> indexEnrichments(List<YamlEnrichment> enrichments) {
        Map<String, YamlEnrichment> map = new HashMap<>();
        if (enrichments != null) {
            for (YamlEnrichment e : enrichments) {
                if (e != null && e.getId() != null) {
                    map.put(e.getId(), e);
                }
            }
        }
        return map;
    }
}

