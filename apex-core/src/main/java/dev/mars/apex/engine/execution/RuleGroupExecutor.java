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
package dev.mars.apex.engine.execution;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleBase;
import dev.mars.apex.engine.model.RuleGroup;
import dev.mars.apex.engine.model.RuleGroupEvaluationResult;
import dev.mars.apex.engine.model.RuleResult;
import dev.mars.apex.engine.model.RuleResult.ResultType;
import dev.mars.apex.engine.core.UnifiedRuleEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executor for rule group evaluation logic.
 * Handles execution of rule groups with proper error handling and severity aggregation.
 * 
 * <p>This class extracts rule group execution logic from RulesEngine to maintain
 * focused responsibilities. It processes rule groups and handles mixed type lists.</p>
 * 
 * <p>Phase 2 refactoring: uses {@link RuleGroupEvaluationService} to route
 * individual rule evaluation through {@link UnifiedRuleEvaluator}.</p>
 * 
 * @since 2026-01-22
 */
public class RuleGroupExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RuleGroupExecutor.class);
    
    private final UnifiedRuleEvaluator unifiedEvaluator;
    private final RuleGroupEvaluationService groupEvaluationService;
    
    public RuleGroupExecutor(UnifiedRuleEvaluator unifiedEvaluator) {
        this.unifiedEvaluator = unifiedEvaluator;
        this.groupEvaluationService = new RuleGroupEvaluationService(unifiedEvaluator);
    }

    /**
     * Returns the {@link RuleGroupEvaluationService} used by this executor.
     * This allows other components (e.g., {@code EnrichmentProcessor}) to route
     * rule group evaluation through the canonical path.
     *
     * @return the rule group evaluation service
     */
    public RuleGroupEvaluationService getGroupEvaluationService() {
        return groupEvaluationService;
    }
    
    /**
     * Execute a list of RuleGroup objects against the provided facts.
     *
     * @param ruleGroups The list of RuleGroup objects to execute
     * @param facts The facts to evaluate the rule groups against
     * @param context The evaluation context
     * @return The result of the first rule group that matches, or a default result if no rule groups match
     */
    public RuleResult executeRuleGroupsList(List<RuleGroup> ruleGroups, 
                                           Map<String, Object> facts,
                                           StandardEvaluationContext context) {
        if (ruleGroups == null || ruleGroups.isEmpty()) {
            logger.info("No rule groups provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} rule groups", ruleGroups.size());
        logger.debug("executeRuleGroupsList() - rule group ids: {}", 
                    ruleGroups.stream().map(RuleGroup::getId).toList());
        logger.debug("executeRuleGroupsList() - facts keys: {}, facts size: {}", 
                    facts != null ? facts.keySet() : "none", facts != null ? facts.size() : 0);

        // Track the highest severity from failed rule groups
        String highestFailedSeverity = SeverityConstants.INFO;
        String lastFailedGroupName = null;
        String lastFailedGroupMessage = null;

        // Evaluate rule groups in priority order
        for (RuleGroup group : ruleGroups) {
            logger.debug("Evaluating rule group: '{}' with {} rules", 
                        group.getName(), group.getRules() != null ? group.getRules().size() : 0);
            try {
                // Use detailed evaluation via service to get severity aggregation
                // delegates individual rule evaluation to UnifiedRuleEvaluator
                long startTime = System.currentTimeMillis();
                RuleGroupEvaluationResult evaluationResult = groupEvaluationService.evaluateWithDetails(group, context);
                long duration = System.currentTimeMillis() - startTime;
                boolean result = evaluationResult.isGroupResult();
                String aggregatedSeverity = evaluationResult.getAggregatedSeverity();

                logger.debug("Rule group '{}' evaluated in {}ms - result: {}, severity: {}",
                           group.getName(), duration, result, aggregatedSeverity);

                // Debug: Log individual results
                logger.debug("Individual results count: {}", evaluationResult.getIndividualResults().size());
                for (RuleResult individualResult : evaluationResult.getIndividualResults()) {
                    logger.debug("  Rule result: name={}, type={}, success={}, triggered={}", 
                               individualResult.getRuleName(), individualResult.getResultType(), 
                               individualResult.isSuccess(), individualResult.isTriggered());
                }

                // Note: Individual ResultType.ERROR results are NOT treated as group failures.
                // A rule that didn't match with ERROR severity is a normal non-match outcome,
                // not a system failure. Actual exceptions are caught in the catch block below.
                // The AND/OR aggregation in RuleGroupEvaluationService.computeGroupResult()
                // correctly handles triggered/not-triggered semantics.

                if (result) {
                    logger.info("Rule group matched: {}", group.getName());
                    // Include individual rule results in enrichedData for downstream use
                    Map<String, Object> enrichedWithRuleResults = new HashMap<>();
                    enrichedWithRuleResults.put("_individualRuleResults", evaluationResult.getRuleResultsMap());
                    return RuleResult.builder()
                            .ruleName(group.getName())
                            .message(group.getMessage())
                            .severity(aggregatedSeverity)
                            .triggered(true)
                            .resultType(ResultType.MATCH)
                            .success(true)
                            .enrichedData(enrichedWithRuleResults)
                            .build();
                } else {
                    // Track failed group with highest severity
                    if (SeverityConstants.getSeverityPriority(aggregatedSeverity) > SeverityConstants.getSeverityPriority(highestFailedSeverity)) {
                        highestFailedSeverity = aggregatedSeverity;
                        lastFailedGroupName = group.getName();
                        lastFailedGroupMessage = group.getMessage();
                    }
                }
            } catch (Exception e) {
                // CRITICAL: Rule group evaluation exception is a business logic failure
                // This is NOT a "rule didn't match" scenario - it's a system failure
                logger.error("Rule group evaluation failed for '{}': {}", group.getName(), e.getMessage());
                logger.debug("Full exception details:", e);
                return RuleResult.error(
                    group.getName(),
                    "Rule group evaluation failed: " + e.getMessage(),
                    SeverityConstants.ERROR
                );
            }
        }

        logger.info("No rule groups matched");

        // Return result with highest severity from failed groups
        if (lastFailedGroupName != null) {
            return RuleResult.noMatch(lastFailedGroupName, lastFailedGroupMessage, highestFailedSeverity);
        } else {
            return RuleResult.noMatch();
        }
    }
    
    /**
     * Execute a list of rules against the provided facts.
     * This method determines the type of objects in the list and delegates to the appropriate method.
     *
     * @param rules The list of rules to execute (can be a mix of Rule and RuleGroup objects)
     * @param facts The facts to evaluate the rules against
     * @param context The evaluation context
     * @return The result of the first rule that matches, or a default result if no rules match
     */
    public RuleResult executeRules(List<RuleBase> rules, 
                                  Map<String, Object> facts, 
                                  StandardEvaluationContext context) {
        if (rules == null || rules.isEmpty()) {
            logger.info("No rules provided for execution");
            return RuleResult.noRules();
        }

        logger.info("Executing {} rules/rule groups", rules.size());
        logger.debug("Facts provided: {}", facts != null ? facts.keySet() : "none");

        // Check if all rules are of the same type and delegate to the appropriate method
        boolean allRules = true;
        boolean allRuleGroups = true;

        for (RuleBase ruleObj : rules) {
            if (!(ruleObj instanceof Rule)) {
                allRules = false;
            }
            if (!(ruleObj instanceof RuleGroup)) {
                allRuleGroups = false;
            }
        }

        if (allRules) {
            // All objects are Rule instances, delegate to unified evaluator
            logger.debug("All objects are Rule instances, delegating to unified evaluator");
            @SuppressWarnings("unchecked")
            List<Rule> rulesList = (List<Rule>) (List<?>) rules;
            return unifiedEvaluator.evaluateRules(rulesList, facts);
        } else if (allRuleGroups) {
            // All objects are RuleGroup instances, delegate to rule group executor
            logger.debug("All objects are RuleGroup instances, delegating to executeRuleGroupsList");
            @SuppressWarnings("unchecked")
            List<RuleGroup> ruleGroupsList = (List<RuleGroup>) (List<?>) rules;
            return executeRuleGroupsList(ruleGroupsList, facts, context);
        }

        logger.debug("Mixed list of rules and rule groups, processing manually");
        // Mixed list or unknown types, process manually
        return processMixedRules(rules, facts, context);
    }
    
    /**
     * Process a mixed list of rules and rule groups.
     * 
     * @param rules The mixed list of rules and rule groups
     * @param facts The facts to evaluate against
     * @param context The evaluation context
     * @return The result of the first matching rule or rule group
     */
    private RuleResult processMixedRules(List<RuleBase> rules, 
                                        Map<String, Object> facts, 
                                        StandardEvaluationContext context) {
        // Evaluate rules in priority order
        for (RuleBase ruleObj : rules) {
            logger.debug("Evaluating rule/rule group: {}", ruleObj.getName());
            try {
                if (ruleObj instanceof Rule) {
                    Rule rule = (Rule) ruleObj;
                    RuleResult result = unifiedEvaluator.evaluateRule(rule, facts);
                    logger.debug("Rule '{}' evaluated to: {}", rule.getName(), result.isTriggered());

                    if (result.isTriggered()) {
                        logger.info("Rule matched: {}", rule.getName());
                        return result;
                    }
                } else if (ruleObj instanceof RuleGroup) {
                    RuleGroup group = (RuleGroup) ruleObj;
                    // delegate through service for canonical evaluation path
                    boolean result = groupEvaluationService.evaluate(group, context);
                    logger.debug("Rule group '{}' evaluated to: {}", group.getName(), result);

                    if (result) {
                        logger.info("Rule group matched: {}", group.getName());
                        return RuleResult.match(group.getName(), group.getMessage());
                    }
                }
            } catch (Exception e) {
                String ruleName = ruleObj.getName();
                String errorMessage = String.format("Rule evaluation failed: %s", e.getMessage());

                // Get severity from rule configuration
                String severity = SeverityConstants.ERROR; // Default severity for evaluation errors
                if (ruleObj instanceof Rule) {
                    Rule rule = (Rule) ruleObj;
                    severity = rule.getSeverity() != null ? rule.getSeverity() : SeverityConstants.ERROR;
                }

                // Log error details at appropriate level based on severity
                if (SeverityConstants.CRITICAL.equalsIgnoreCase(severity)) {
                    logger.error("CRITICAL rule evaluation error for '{}': {}", ruleName, e.getMessage());
                } else if (SeverityConstants.WARNING.equalsIgnoreCase(severity)) {
                    logger.info("Rule evaluation warning for '{}': {}", ruleName, e.getMessage());
                } else {
                    logger.info("Rule evaluation error for '{}': {}", ruleName, e.getMessage());
                }

                // Always log full exception details at DEBUG level for troubleshooting
                logger.debug("Full exception details for rule/rule group '{}':", ruleName, e);

                return RuleResult.error(ruleName, errorMessage, severity);
            }
        }

        logger.info("No rules or rule groups matched");
        return RuleResult.noMatch();
    }
}
