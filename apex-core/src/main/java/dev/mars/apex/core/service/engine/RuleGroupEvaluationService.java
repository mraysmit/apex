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
package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleGroupEvaluationResult;
import dev.mars.apex.core.engine.model.RuleGroupSeverityAggregator;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.util.EnabledFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service that evaluates rules within a {@link RuleGroup} by delegating individual
 * rule evaluation to {@link UnifiedRuleEvaluator}.
 *
 * <p>This is the Phase 2 refactoring outcome: rule groups no longer maintain their
 * own SpEL parser or duplicate evaluation logic. Every rule—whether standalone or
 * inside a group—is evaluated through the single canonical path in
 * {@code UnifiedRuleEvaluator}, thereby gaining error recovery, performance
 * monitoring, message templating, success/error codes, and field mappings for free.</p>
 *
 * <p>The service preserves the existing AND/OR semantics, short-circuit behaviour,
 * parallel execution, and severity aggregation.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-02-08
 */
public class RuleGroupEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(RuleGroupEvaluationService.class);

    private final UnifiedRuleEvaluator unifiedRuleEvaluator;
    private final RuleGroupSeverityAggregator severityAggregator = new RuleGroupSeverityAggregator();

    public RuleGroupEvaluationService(UnifiedRuleEvaluator unifiedRuleEvaluator) {
        this.unifiedRuleEvaluator = Objects.requireNonNull(unifiedRuleEvaluator, "unifiedRuleEvaluator must not be null");
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Evaluate a rule group, returning a simple boolean result.
     *
     * @param group   the rule group to evaluate
     * @param context the SpEL evaluation context populated with facts
     * @return {@code true} if the group condition is satisfied
     */
    public boolean evaluate(RuleGroup group, StandardEvaluationContext context) {
        RuleGroupEvaluationResult result = evaluateWithDetails(group, context);
        return result.isGroupResult();
    }

    /**
     * Evaluate a rule group with detailed results including individual rule
     * outcomes and aggregated severity.
     *
     * @param group   the rule group to evaluate
     * @param context the SpEL evaluation context populated with facts
     * @return a comprehensive evaluation result
     */
    public RuleGroupEvaluationResult evaluateWithDetails(RuleGroup group, StandardEvaluationContext context) {
        long startTime = System.currentTimeMillis();

        if (group.getRules().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return new RuleGroupEvaluationResult(
                    group.getId(), group.getName(), false, new ArrayList<>(),
                    SeverityConstants.INFO, group.isAndOperator(), duration
            );
        }

        List<RuleResult> individualResults;
        boolean groupResult;

        if (group.isParallelExecution() && group.getRules().size() > 1) {
            individualResults = evaluateParallel(group, context);
        } else {
            individualResults = evaluateSequential(group, context);
        }

        groupResult = computeGroupResult(individualResults, group.isAndOperator());

        // Phase 2: propagate individual results back to the group so that
        // downstream enrichments can reference #ruleResults['rule-id']
        group.updateEvaluationResults(individualResults, groupResult);

        String aggregatedSeverity = severityAggregator.aggregateSeverity(individualResults, group.isAndOperator());
        long duration = System.currentTimeMillis() - startTime;

        return new RuleGroupEvaluationResult(
                group.getId(), group.getName(), groupResult, individualResults,
                aggregatedSeverity, group.isAndOperator(), duration
        );
    }

    // =========================================================================
    // Sequential evaluation
    // =========================================================================

    private List<RuleResult> evaluateSequential(RuleGroup group, StandardEvaluationContext context) {
        List<Rule> rules = group.getRules(); // already sorted by sequence
        boolean useShortCircuit = group.isStopOnFirstFailure() && !group.isDebugMode();
        boolean isAnd = group.isAndOperator();

        List<RuleResult> results = new ArrayList<>();
        boolean runningResult = isAnd; // true for AND, false for OR

        for (Rule rule : rules) {
            if (rule == null) {
                logger.error("Null rule in group '{}', skipping", group.getName());
                continue;
            }

            // Skip disabled rules
            if (!EnabledFilter.isEnabled(rule)) {
                logger.info("Rule '{}' in group '{}' is disabled, skipping", rule.getName(), group.getName());
                results.add(RuleResult.noMatch(rule.getName(), "Rule is disabled", SeverityConstants.INFO));
                continue;
            }

            // Delegate to the canonical evaluation path
            RuleResult ruleResult = unifiedRuleEvaluator.evaluateRule(rule, context);
            results.add(ruleResult);

            boolean triggered = ruleResult.isTriggered();

            if (group.isDebugMode()) {
                logger.debug("Rule '{}' in group '{}' evaluated to: {} (severity: {})",
                        rule.getName(), group.getName(), triggered, ruleResult.getSeverity());
            }

            // Apply AND/OR short-circuit logic
            if (isAnd) {
                runningResult = runningResult && triggered;
                if (!runningResult && useShortCircuit) {
                    if (group.isDebugMode()) {
                        logger.debug("AND group '{}' short-circuited after {} rules", group.getName(), results.size());
                    }
                    break;
                }
            } else {
                runningResult = runningResult || triggered;
                if (runningResult && useShortCircuit) {
                    if (group.isDebugMode()) {
                        logger.debug("OR group '{}' short-circuited after {} rules", group.getName(), results.size());
                    }
                    break;
                }
            }
        }

        if (group.isDebugMode()) {
            long passed = results.stream().filter(RuleResult::isTriggered).count();
            logger.debug("Group '{}' sequential evaluation complete. Evaluated: {}, Passed: {}, Failed: {}, Final: {}",
                    group.getName(), results.size(), passed, results.size() - passed, runningResult);
        }

        return results;
    }

    // =========================================================================
    // Parallel evaluation
    // =========================================================================

    private List<RuleResult> evaluateParallel(RuleGroup group, StandardEvaluationContext context) {
        List<Rule> rules = group.getRules();
        List<RuleResult> results = new ArrayList<>();

        // Build tasks (skip disabled upfront)
        List<Callable<RuleResult>> tasks = new ArrayList<>();
        List<Rule> activeRules = new ArrayList<>();

        for (Rule rule : rules) {
            if (rule == null) {
                logger.error("Null rule in group '{}', skipping", group.getName());
                continue;
            }
            if (!EnabledFilter.isEnabled(rule)) {
                logger.info("Rule '{}' in group '{}' is disabled, skipping (parallel)", rule.getName(), group.getName());
                results.add(RuleResult.noMatch(rule.getName(), "Rule is disabled", SeverityConstants.INFO));
                continue;
            }
            activeRules.add(rule);
            tasks.add(() -> unifiedRuleEvaluator.evaluateRule(rule, context));
        }

        if (tasks.isEmpty()) {
            return results;
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(tasks.size(), Runtime.getRuntime().availableProcessors())
        );

        try {
            List<Future<RuleResult>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < futures.size(); i++) {
                try {
                    results.add(futures.get(i).get());
                } catch (Exception e) {
                    Rule rule = activeRules.get(i);
                    logger.error("Error getting result for rule '{}' in group '{}': {}",
                            rule.getName(), group.getName(), e.getMessage());
                    results.add(RuleResult.error(rule.getName(),
                            "Error getting result: " + e.getMessage(), SeverityConstants.ERROR));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Parallel evaluation interrupted for group '{}': {}", group.getName(), e.getMessage());
        } finally {
            executor.shutdown();
        }

        if (group.isDebugMode()) {
            long passed = results.stream().filter(RuleResult::isTriggered).count();
            logger.debug("Group '{}' parallel evaluation complete. Total: {}, Passed: {}, Failed: {}, Final: {}",
                    group.getName(), results.size(), passed, results.size() - passed,
                    computeGroupResult(results, group.isAndOperator()));
        }

        return results;
    }

    // =========================================================================
    // AND/OR aggregation
    // =========================================================================

    /**
     * Compute the group-level boolean result from individual rule results
     * using AND or OR semantics.
     */
    private boolean computeGroupResult(List<RuleResult> results, boolean isAnd) {
        if (results.isEmpty()) {
            return false;
        }
        // Only consider enabled results (non-disabled)
        List<RuleResult> enabledResults = results.stream()
                .filter(r -> !"Rule is disabled".equals(r.getMessage()))
                .toList();

        if (enabledResults.isEmpty()) {
            return false;
        }

        if (isAnd) {
            return enabledResults.stream().allMatch(RuleResult::isTriggered);
        } else {
            return enabledResults.stream().anyMatch(RuleResult::isTriggered);
        }
    }
}
