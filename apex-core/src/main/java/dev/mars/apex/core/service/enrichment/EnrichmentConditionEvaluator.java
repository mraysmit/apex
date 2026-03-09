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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.function.Function;

/**
 * Evaluates condition groups (AND/OR logic), individual condition rules,
 * and mapping-rule conditions using SpEL expressions.
 *
 * <p>Extracted from {@link EnrichmentProcessor} (Phase 13 decomposition) to isolate
 * condition evaluation logic from enrichment orchestration.</p>
 *
 * @since 2.4
 */
public class EnrichmentConditionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentConditionEvaluator.class);

    private final ExpressionParser parser;
    private final Function<Object, StandardEvaluationContext> contextFactory;

    /**
     * @param parser         SpEL expression parser
     * @param contextFactory Creates a {@link StandardEvaluationContext} from a root object.
     *                       Typically provided by {@link EnrichmentProcessor#createEvaluationContext}.
     */
    public EnrichmentConditionEvaluator(ExpressionParser parser,
                                        Function<Object, StandardEvaluationContext> contextFactory) {
        this.parser = parser;
        this.contextFactory = contextFactory;
    }

    // ─── Condition Group Evaluation ──────────────────────────────────────

    /**
     * Evaluate a condition group (AND/OR) against a target object.
     *
     * @param conditionGroup The condition group configuration
     * @param targetObject   The target object for context
     * @return true if conditions are met, false otherwise
     */
    public boolean evaluateConditionGroup(YamlEnrichment.ConditionGroup conditionGroup, Object targetObject) {
        if (conditionGroup == null || conditionGroup.getRules() == null || conditionGroup.getRules().isEmpty()) {
            logger.debug("No conditions to evaluate, returning true");
            return true;
        }

        String operator = conditionGroup.getOperator();
        if (operator == null) {
            operator = "AND"; // Default to AND if not specified
        }

        logger.trace("Evaluating condition group with operator: " + operator);

        StandardEvaluationContext context = contextFactory.apply(targetObject);

        boolean result;
        if ("OR".equalsIgnoreCase(operator)) {
            result = evaluateOrConditions(conditionGroup.getRules(), context);
        } else if ("AND".equalsIgnoreCase(operator)) {
            result = evaluateAndConditions(conditionGroup.getRules(), context);
        } else {
            logger.warn("Unknown condition operator: " + operator + ", defaulting to AND");
            result = evaluateAndConditions(conditionGroup.getRules(), context);
        }

        logger.debug("Condition group evaluation result: " + result);
        return result;
    }

    // ─── OR / AND Logic ──────────────────────────────────────────────────

    /**
     * Evaluate conditions with OR logic (short-circuit on first true).
     */
    boolean evaluateOrConditions(List<YamlEnrichment.ConditionRule> rules, StandardEvaluationContext context) {
        for (YamlEnrichment.ConditionRule rule : rules) {
            try {
                if (evaluateConditionRule(rule, context)) {
                    logger.trace("OR condition met: " + rule.getCondition());
                    return true;
                }
            } catch (Exception e) {
                logger.error("[APEX-ENRICH-005] Failed to evaluate OR condition: '{}' - Error: {}",
                          rule.getCondition(), e.getMessage());
                logger.debug("Full stack trace for OR condition evaluation failure:", e);
                throw new EnrichmentException(
                    "OR condition evaluation failed: '" + rule.getCondition() + "': " + e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Evaluate conditions with AND logic (short-circuit on first false).
     */
    boolean evaluateAndConditions(List<YamlEnrichment.ConditionRule> rules, StandardEvaluationContext context) {
        for (YamlEnrichment.ConditionRule rule : rules) {
            try {
                if (!evaluateConditionRule(rule, context)) {
                    logger.trace("AND condition failed: " + rule.getCondition());
                    return false;
                }
            } catch (Exception e) {
                logger.error("[APEX-ENRICH-005] Failed to evaluate AND condition: '{}' - Error: {}",
                          rule.getCondition(), e.getMessage());
                logger.debug("Full stack trace for AND condition evaluation failure:", e);
                throw new EnrichmentException(
                    "AND condition evaluation failed: '" + rule.getCondition() + "': " + e.getMessage(), e);
            }
        }
        return true;
    }

    // ─── Single Condition Rule ───────────────────────────────────────────

    /**
     * Evaluate a single condition rule.
     */
    boolean evaluateConditionRule(YamlEnrichment.ConditionRule rule, StandardEvaluationContext context) {
        if (rule.getCondition() == null || rule.getCondition().trim().isEmpty()) {
            return true;
        }

        try {
            Expression expression = parser.parseExpression(rule.getCondition());
            Object result = expression.getValue(context);

            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result != null) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("[APEX-ENRICH-006] Failed to evaluate condition rule: '{}' - Error: {}",
                      rule.getCondition(), e.getMessage());
            logger.debug("Full exception details for condition rule evaluation failure:", e);
            throw new EnrichmentException(
                "[APEX-ENRICH-006] Condition rule evaluation failed for '" + rule.getCondition() + "': " + e.getMessage(), e);
        }
    }

    // ─── Mapping Rule Conditions ─────────────────────────────────────────

    /**
     * Evaluate mapping rule conditions (delegates to condition group evaluation).
     *
     * @param rule         The mapping rule
     * @param targetObject The target object for context
     * @return true if conditions are met (or no conditions specified), false otherwise
     */
    public boolean evaluateMappingRuleConditions(YamlEnrichment.MappingRule rule, Object targetObject) {
        YamlEnrichment.ConditionGroup conditions = rule.getConditions();

        if (conditions == null) {
            logger.trace("No conditions specified for rule '" + rule.getId() + "', treating as default rule");
            return true;
        }

        return evaluateConditionGroup(conditions, targetObject);
    }
}
