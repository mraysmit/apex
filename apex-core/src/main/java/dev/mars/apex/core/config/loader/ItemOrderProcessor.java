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
package dev.mars.apex.core.config.loader;

import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlEnrichmentGroup;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.YamlRuleGroup;
import dev.mars.apex.core.config.sequential.ProcessingItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages execution ordering of rules, enrichments, and groups in a loaded configuration.
 *
 * <p>Extracted from {@link ConfigurationLoader} (Phase 13e decomposition) to isolate
 * item-order management from configuration loading orchestration.</p>
 *
 * <p>Provides two operations:</p>
 * <ul>
 *   <li>{@link #expandReferencePlaceholders} — replaces {@code *-refs} wildcard
 *       placeholders with actual referenced item IDs</li>
 *   <li>{@link #applyGroupsOnlyLogic} — filters items that are referenced by groups
 *       so they only execute via the group, not at their definition position</li>
 * </ul>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.4
 */
class ItemOrderProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ItemOrderProcessor.class);

    /**
     * Expand reference placeholders in item order.
     * This replaces "*-refs" placeholders with actual items from referenced files.
     * Must be called AFTER processRuleReferences() and processEnrichmentReferences().
     *
     * @param config Configuration with item order and tracked referenced IDs
     */
    void expandReferencePlaceholders(YamlRuleConfiguration config) {
        if (config.getItemOrder() == null || config.getItemOrder().isEmpty()) {
            logger.debug("No item order to expand");
            return;
        }

        List<ProcessingItem> expandedOrder = new ArrayList<>();
        int originalSize = config.getItemOrder().size();

        for (ProcessingItem item : config.getItemOrder()) {
            String sectionType = item.getSectionType();
            String itemId = item.getItemId();

            if (sectionType.equals("enrichment-refs") && itemId.equals("*")) {
                // Expand enrichment references
                logger.debug("Expanding enrichment-refs placeholder");

                if (config.getReferencedEnrichmentIds() != null) {
                    for (String enrichmentId : config.getReferencedEnrichmentIds()) {
                        expandedOrder.add(new ProcessingItem("enrichments", enrichmentId));
                        logger.debug("  Added enrichment: " + enrichmentId);
                    }
                }

                if (config.getReferencedEnrichmentGroupIds() != null) {
                    for (String groupId : config.getReferencedEnrichmentGroupIds()) {
                        expandedOrder.add(new ProcessingItem("enrichment-groups", groupId));
                        logger.debug("  Added enrichment-group: " + groupId);
                    }
                }
            } else if (sectionType.equals("rule-refs") && itemId.equals("*")) {
                // Expand rule references
                logger.debug("Expanding rule-refs placeholder");

                if (config.getReferencedRuleIds() != null) {
                    for (String ruleId : config.getReferencedRuleIds()) {
                        expandedOrder.add(new ProcessingItem("rules", ruleId));
                        logger.debug("  Added rule: " + ruleId);
                    }
                }

                if (config.getReferencedRuleGroupIds() != null) {
                    for (String groupId : config.getReferencedRuleGroupIds()) {
                        expandedOrder.add(new ProcessingItem("rule-groups", groupId));
                        logger.debug("  Added rule-group: " + groupId);
                    }
                }
            } else {
                // Keep non-placeholder items as-is
                expandedOrder.add(item);
            }
        }

        config.setItemOrder(expandedOrder);
        logger.info("Expanded item order from " + originalSize + " to " + expandedOrder.size() + " items");
    }

    /**
     * Apply groups-only logic to filter itemOrder.
     * <p>
     * When enrichments/rules/groups are referenced by enrichment-groups/rule-groups,
     * they should only execute via the group (via flattening), not at their definition position.
     * <p>
     * This method must be called AFTER expandReferencePlaceholders() to ensure that:
     * - Enrichment-groups/rule-groups loaded from external files are in itemOrder
     * - Groups in the main file can reference and filter groups from external files
     * - All referenced items (enrichments, rules, enrichment-groups, rule-groups) are properly filtered
     * <p>
     * This method filters the itemOrder to remove enrichments/rules/groups that are referenced by groups,
     * so they only execute via the group (not at their definition position).
     *
     * @param config The configuration with itemOrder to filter
     */
    void applyGroupsOnlyLogic(YamlRuleConfiguration config) {
        logger.info("=== APPLYING GROUPS-ONLY LOGIC ===");

        if (config.getItemOrder() == null || config.getItemOrder().isEmpty()) {
            logger.info("No item order to filter - skipping groups-only logic");
            return;
        }

        logger.info("Item order size BEFORE filtering: " + config.getItemOrder().size());

        // Collect enrichment IDs referenced by enrichment-groups (use LinkedHashSet to preserve order)
        Set<String> referencedEnrichmentIds = new LinkedHashSet<>();
        if (config.getEnrichmentGroups() != null && !config.getEnrichmentGroups().isEmpty()) {
            for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
                // Collect from enrichment-ids (simple string list)
                if (group.getEnrichmentIds() != null) {
                    referencedEnrichmentIds.addAll(group.getEnrichmentIds());
                }
                // Collect from enrichment-references (structured objects with enrichment-id field)
                if (group.getEnrichmentReferences() != null) {
                    for (YamlEnrichmentGroup.EnrichmentReference ref : group.getEnrichmentReferences()) {
                        if (ref.getEnrichmentId() != null) {
                            referencedEnrichmentIds.add(ref.getEnrichmentId());
                        }
                    }
                }
            }
            logger.info("Found " + referencedEnrichmentIds.size() + " enrichment IDs referenced by groups: " + referencedEnrichmentIds);
        }

        // Collect rule IDs referenced by rule-groups (use LinkedHashSet to preserve order)
        Set<String> referencedRuleIds = new LinkedHashSet<>();
        if (config.getRuleGroups() != null && !config.getRuleGroups().isEmpty()) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                // Collect from rule-ids (simple string list)
                if (group.getRuleIds() != null) {
                    referencedRuleIds.addAll(group.getRuleIds());
                }
                // Collect from rule-references (structured objects with rule-id field)
                if (group.getRuleReferences() != null) {
                    for (YamlRuleGroup.RuleReference ref : group.getRuleReferences()) {
                        if (ref.getRuleId() != null) {
                            referencedRuleIds.add(ref.getRuleId());
                        }
                    }
                }
            }
            logger.info("Found " + referencedRuleIds.size() + " rule IDs referenced by groups: " + referencedRuleIds);
        }

        // Collect enrichment-group IDs referenced by other enrichment-groups (use LinkedHashSet to preserve order)
        Set<String> referencedEnrichmentGroupIds = new LinkedHashSet<>();
        if (config.getEnrichmentGroups() != null && !config.getEnrichmentGroups().isEmpty()) {
            for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
                if (group.getEnrichmentGroupReferences() != null) {
                    referencedEnrichmentGroupIds.addAll(group.getEnrichmentGroupReferences());
                }
            }
            logger.info("Found " + referencedEnrichmentGroupIds.size() + " enrichment-group IDs referenced by other groups: " + referencedEnrichmentGroupIds);
        }

        // Collect enrichment-group IDs referenced by function mappings and function conditions.
        // These groups are invoked at runtime, so they must not auto-execute at their definition position.
        if (config.getEnrichments() != null && !config.getEnrichments().isEmpty()) {
            for (YamlEnrichment enrichment : config.getEnrichments()) {
                // From mapping-rules: mapping-config enrichment-group-ref (function mapping action)
                if (enrichment.getMappingRules() != null) {
                    for (YamlEnrichment.MappingRule rule : enrichment.getMappingRules()) {
                        if (rule.getMapping() != null && rule.getMapping().getEnrichmentGroupRef() != null) {
                            referencedEnrichmentGroupIds.add(rule.getMapping().getEnrichmentGroupRef());
                        }
                        // From mapping-rules: condition-rules with type "function"
                        collectFunctionConditionGroupRefs(rule.getConditions(), referencedEnrichmentGroupIds);
                    }
                }
                // From conditional-mappings: condition-rules with type "function"
                if (enrichment.getConditionalMappings() != null) {
                    for (YamlEnrichment.ConditionalMapping cm : enrichment.getConditionalMappings()) {
                        collectFunctionConditionGroupRefs(cm.getConditions(), referencedEnrichmentGroupIds);
                    }
                }
            }
            if (!referencedEnrichmentGroupIds.isEmpty()) {
                logger.info("Total enrichment-group IDs referenced (including function mappings/conditions): " + referencedEnrichmentGroupIds);
            }
        }

        // Collect rule-group IDs referenced by other rule-groups (use LinkedHashSet to preserve order)
        Set<String> referencedRuleGroupIds = new LinkedHashSet<>();
        if (config.getRuleGroups() != null && !config.getRuleGroups().isEmpty()) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                if (group.getRuleGroupReferences() != null) {
                    referencedRuleGroupIds.addAll(group.getRuleGroupReferences());
                }
            }
            logger.info("Found " + referencedRuleGroupIds.size() + " rule-group IDs referenced by other groups: " + referencedRuleGroupIds);
        }

        // If no groups exist, no filtering needed
        if (referencedEnrichmentIds.isEmpty() && referencedRuleIds.isEmpty() &&
            referencedEnrichmentGroupIds.isEmpty() && referencedRuleGroupIds.isEmpty()) {
            logger.info("No groups found - skipping groups-only logic (all items execute at definition position)");
            return;
        }

        // Filter itemOrder: Remove enrichments/rules/groups referenced by groups
        List<ProcessingItem> originalOrder = new ArrayList<>(config.getItemOrder());
        List<ProcessingItem> filteredOrder = new ArrayList<>();
        int enrichmentsFiltered = 0;
        int rulesFiltered = 0;
        int enrichmentGroupsFiltered = 0;
        int ruleGroupsFiltered = 0;

        for (ProcessingItem item : originalOrder) {
            boolean shouldRemove = false;

            if ("enrichments".equals(item.getSectionType()) &&
                referencedEnrichmentIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via enrichment-group
                enrichmentsFiltered++;
                logger.debug("Filtering enrichment '" + item.getItemId() + "' from itemOrder (referenced by group - definition only)");
            } else if ("rules".equals(item.getSectionType()) &&
                       referencedRuleIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via rule-group
                rulesFiltered++;
                logger.debug("Filtering rule '" + item.getItemId() + "' from itemOrder (referenced by group - definition only)");
            } else if ("enrichment-groups".equals(item.getSectionType()) &&
                       referencedEnrichmentGroupIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via parent enrichment-group
                enrichmentGroupsFiltered++;
                logger.debug("Filtering enrichment-group '" + item.getItemId() + "' from itemOrder (referenced by another group - definition only)");
            } else if ("rule-groups".equals(item.getSectionType()) &&
                       referencedRuleGroupIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via parent rule-group
                ruleGroupsFiltered++;
                logger.debug("Filtering rule-group '" + item.getItemId() + "' from itemOrder (referenced by another group - definition only)");
            }

            if (!shouldRemove) {
                filteredOrder.add(item);
            }
        }

        // Update configuration with filtered order
        config.setItemOrder(filteredOrder);

        logger.info("Applied groups-only logic: filtered " + enrichmentsFiltered + " enrichments, " +
                   rulesFiltered + " rules, " + enrichmentGroupsFiltered + " enrichment-groups, and " +
                   ruleGroupsFiltered + " rule-groups from itemOrder (original: " + originalOrder.size() +
                   " items, filtered: " + filteredOrder.size() + " items)");
    }

    /**
     * Collect enrichment-group IDs from condition rules with type "function" in a ConditionGroup.
     */
    private void collectFunctionConditionGroupRefs(YamlEnrichment.ConditionGroup conditionGroup,
                                                   Set<String> target) {
        if (conditionGroup == null || conditionGroup.getRules() == null) {
            return;
        }
        for (YamlEnrichment.ConditionRule cr : conditionGroup.getRules()) {
            if ("function".equalsIgnoreCase(cr.getType()) && cr.getEnrichmentGroupRef() != null) {
                target.add(cr.getEnrichmentGroupRef());
            }
        }
    }
}
