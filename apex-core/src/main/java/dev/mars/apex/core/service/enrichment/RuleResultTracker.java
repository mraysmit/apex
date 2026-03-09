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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mutable state for rule and rule-group evaluation results used by conditional mapping.
 *
 * <p>In document-order processing mode, {@link dev.mars.apex.engine.execution.SequentialProcessor}
 * stores rule results here as rules are evaluated. Enrichments evaluated later in the same pass can
 * then reference {@code #ruleResults['rule-id']} or {@code #ruleGroupResults['group-id']['passed']}
 * in SpEL conditions.</p>
 *
 * <p>This class is extracted from {@link EnrichmentProcessor} (Phase 13 decomposition) to isolate
 * the mutable state container from enrichment orchestration logic.</p>
 *
 * <p>Thread safety: uses {@link ConcurrentHashMap} for both maps, matching the original implementation.
 * Note that individual operations are atomic but compound read-then-write sequences are not.</p>
 *
 * @since 2.4
 */
public class RuleResultTracker {

    private static final Logger logger = LoggerFactory.getLogger(RuleResultTracker.class);

    /** Rule group results keyed by group ID. Each value contains "passed", "passedRules", "failedRules", plus individual rule entries. */
    private final Map<String, Map<String, Object>> ruleGroupResults = new ConcurrentHashMap<>();

    /** Individual rule results keyed by rule ID. */
    private final Map<String, Boolean> individualRuleResults = new ConcurrentHashMap<>();

    /**
     * Clear rule results before starting a new evaluation pass.
     * This must be called at the start of each document-order evaluation to prevent
     * stale results from previous evaluations affecting conditional mappings.
     */
    public void clearRuleResults() {
        ruleGroupResults.clear();
        individualRuleResults.clear();
        logger.debug("Cleared rule results for new evaluation pass");
    }

    /**
     * Store a rule group result for use in conditional mapping expressions.
     * This method is called by RulesEngine when processing rule groups in document order mode.
     *
     * @param ruleGroupId The ID of the rule group
     * @param passed Whether the rule group passed
     * @param ruleResults Map of individual rule results within the group
     */
    public void storeRuleGroupResult(String ruleGroupId, boolean passed, Map<String, Boolean> ruleResults) {
        Map<String, Object> groupRuleResults = new HashMap<>();
        groupRuleResults.put("passed", passed);

        if (ruleResults != null) {
            groupRuleResults.putAll(ruleResults);

            // Add passedRules and failedRules lists
            List<String> passedRules = new ArrayList<>();
            List<String> failedRules = new ArrayList<>();
            for (Map.Entry<String, Boolean> entry : ruleResults.entrySet()) {
                if (entry.getValue()) {
                    passedRules.add(entry.getKey());
                } else {
                    failedRules.add(entry.getKey());
                }
            }
            groupRuleResults.put("passedRules", passedRules);
            groupRuleResults.put("failedRules", failedRules);

            // ALSO store individual rule results in the individualRuleResults map
            // This allows enrichments to reference #ruleResults['rule-id'] in document order mode
            for (Map.Entry<String, Boolean> entry : ruleResults.entrySet()) {
                individualRuleResults.put(entry.getKey(), entry.getValue());
                logger.debug("Stored individual rule result from group: " + entry.getKey() + " -> passed=" + entry.getValue());
            }
        }

        ruleGroupResults.put(ruleGroupId, groupRuleResults);
        logger.debug("Stored rule group result: " + ruleGroupId + " -> passed=" + passed);
    }

    /**
     * Store individual rule result for conditional mapping in enrichments.
     * This allows enrichments to reference #ruleResults in document order mode.
     *
     * @param ruleId The ID of the rule
     * @param passed Whether the rule passed
     */
    public void storeIndividualRuleResult(String ruleId, boolean passed) {
        individualRuleResults.put(ruleId, passed);
        logger.debug("Stored individual rule result: " + ruleId + " -> passed=" + passed);
    }

    /**
     * Get rule group results map (read-only view for SpEL context).
     *
     * @return unmodifiable view of rule group results
     */
    public Map<String, Map<String, Object>> getRuleGroupResults() {
        return ruleGroupResults;
    }

    /**
     * Get individual rule results map (read-only view for SpEL context).
     *
     * @return unmodifiable view of individual rule results
     */
    public Map<String, Boolean> getIndividualRuleResults() {
        return individualRuleResults;
    }
}
