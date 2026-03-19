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
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.function.Function;

/**
 * Evaluates condition groups (AND/OR logic), individual condition predicates,
 * and mapping-rule conditions using SpEL expressions.
 *
 * <p>Supports three condition predicate types via the {@code type} field on
 * {@link YamlEnrichment.ConditionRule}:</p>
 * <ul>
 *   <li>{@code "expression"} (default) — pure SpEL evaluation</li>
 *   <li>{@code "lookup"} — execute a lookup, stash result, then evaluate SpEL</li>
 *   <li>{@code "function"} — invoke an enrichment group, stash output, then evaluate SpEL</li>
 * </ul>
 *
 * <p>Lookup and function execution is delegated to {@link ConditionActionExecutor}
 * to keep this class focused on boolean evaluation.</p>
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

    // Optional delegate for lookup/function condition execution.
    // Null when only SpEL conditions are needed (backward-compatible).
    private ConditionActionExecutor actionExecutor;

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

    /**
     * Set the action executor for lookup/function condition predicates.
     * Must be called after construction to support typed conditions.
     */
    public void setActionExecutor(ConditionActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
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
        return evaluateConditionGroup(conditionGroup, targetObject, null);
    }

    /**
     * Evaluate a condition group (AND/OR) against a target object with YAML configuration
     * for lookup/function condition resolution.
     *
     * @param conditionGroup The condition group configuration
     * @param targetObject   The target object for context
     * @param config         YAML configuration (needed for lookup/function conditions, may be null)
     * @return true if conditions are met, false otherwise
     */
    public boolean evaluateConditionGroup(YamlEnrichment.ConditionGroup conditionGroup,
                                           Object targetObject,
                                           YamlRuleConfiguration config) {
        if (conditionGroup == null || conditionGroup.getRules() == null || conditionGroup.getRules().isEmpty()) {
            logger.debug("No conditions to evaluate, returning true");
            return true;
        }

        String operator = conditionGroup.getOperator();
        if (operator == null) {
            operator = "AND"; // Default to AND if not specified
        }

        logger.trace("Evaluating condition group with operator: " + operator);

        boolean result;
        if ("OR".equalsIgnoreCase(operator)) {
            result = evaluateOrConditions(conditionGroup.getRules(), targetObject, config);
        } else if ("AND".equalsIgnoreCase(operator)) {
            result = evaluateAndConditions(conditionGroup.getRules(), targetObject, config);
        } else {
            logger.warn("Unknown condition operator: " + operator + ", defaulting to AND");
            result = evaluateAndConditions(conditionGroup.getRules(), targetObject, config);
        }

        logger.debug("Condition group evaluation result: " + result);
        return result;
    }

    // ─── OR / AND Logic ──────────────────────────────────────────────────

    /**
     * Evaluate conditions with OR logic (short-circuit on first true).
     */
    boolean evaluateOrConditions(List<YamlEnrichment.ConditionRule> rules,
                                 Object targetObject,
                                 YamlRuleConfiguration config) {
        for (YamlEnrichment.ConditionRule rule : rules) {
            try {
                if (evaluateConditionRule(rule, targetObject, config)) {
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
    boolean evaluateAndConditions(List<YamlEnrichment.ConditionRule> rules,
                                  Object targetObject,
                                  YamlRuleConfiguration config) {
        for (YamlEnrichment.ConditionRule rule : rules) {
            try {
                if (!evaluateConditionRule(rule, targetObject, config)) {
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

    // ─── Single Condition Predicate ──────────────────────────────────────

    /**
     * Evaluate a single condition predicate.
     * <p>
     * For {@code type: "lookup"}, executes the lookup and stashes the result
     * into {@code result-field} on the targetObject before SpEL evaluation.
     * For {@code type: "function"}, invokes the enrichment group and stashes
     * the output into {@code output-field} before SpEL evaluation.
     * For {@code type: "expression"} (or absent), pure SpEL evaluation.
     *
     * @param rule         The condition predicate
     * @param targetObject The shared context (may be mutated for lookup/function stashing)
     * @param config       YAML configuration for lookup/function resolution (may be null)
     * @return true if condition is met, false otherwise
     */
    boolean evaluateConditionRule(YamlEnrichment.ConditionRule rule,
                                  Object targetObject,
                                  YamlRuleConfiguration config) {
        String type = rule.getType();

        // Type dispatch for lookup/function conditions
        if ("lookup".equalsIgnoreCase(type)) {
            return evaluateLookupCondition(rule, targetObject, config);
        } else if ("function".equalsIgnoreCase(type)) {
            return evaluateFunctionCondition(rule, targetObject, config);
        }

        // Default: expression type — pure SpEL evaluation
        return evaluateSpEL(rule.getCondition(), targetObject);
    }

    /**
     * Execute a lookup, stash the result, then evaluate the SpEL condition.
     */
    private boolean evaluateLookupCondition(YamlEnrichment.ConditionRule rule,
                                             Object targetObject,
                                             YamlRuleConfiguration config) {
        if (actionExecutor == null) {
            logger.warn("Lookup condition requires ConditionActionExecutor but none is configured");
            return false;
        }

        // Execute lookup and stash result
        Object result = actionExecutor.executeLookup(rule, targetObject, config);
        if (rule.getResultField() != null && !rule.getResultField().trim().isEmpty()) {
            setFieldValue(targetObject, rule.getResultField(), result);
            logger.debug("Lookup condition stashed result into '{}': {}", rule.getResultField(), result);
        }

        // Evaluate the SpEL condition on the updated context
        return evaluateSpEL(rule.getCondition(), targetObject);
    }

    /**
     * Execute a function (enrichment group), stash the output, then evaluate the SpEL condition.
     */
    private boolean evaluateFunctionCondition(YamlEnrichment.ConditionRule rule,
                                               Object targetObject,
                                               YamlRuleConfiguration config) {
        if (actionExecutor == null) {
            logger.warn("Function condition requires ConditionActionExecutor but none is configured");
            return false;
        }

        // Execute function and stash output
        Object result = actionExecutor.executeFunction(rule, targetObject, config);
        if (rule.getOutputField() != null && !rule.getOutputField().trim().isEmpty()) {
            setFieldValue(targetObject, rule.getOutputField(), result);
            logger.debug("Function condition stashed output into '{}': {}", rule.getOutputField(), result);
        }

        // Evaluate the SpEL condition on the updated context
        return evaluateSpEL(rule.getCondition(), targetObject);
    }

    // ─── SpEL Evaluation ─────────────────────────────────────────────────

    /**
     * Evaluate a SpEL expression as a boolean.
     */
    private boolean evaluateSpEL(String condition, Object targetObject) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        try {
            StandardEvaluationContext context = contextFactory.apply(targetObject);
            Expression expression = parser.parseExpression(condition);
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
                      condition, e.getMessage());
            logger.debug("Full exception details for condition rule evaluation failure:", e);
            throw new EnrichmentException(
                "[APEX-ENRICH-006] Condition rule evaluation failed for '" + condition + "': " + e.getMessage(), e);
        }
    }

    /**
     * Set a field value on the target object (for stashing lookup/function results).
     */
    @SuppressWarnings("unchecked")
    private void setFieldValue(Object targetObject, String fieldName, Object value) {
        if (targetObject instanceof java.util.Map) {
            ((java.util.Map<String, Object>) targetObject).put(fieldName, value);
        } else {
            // For non-map objects, use SpEL to set the value
            try {
                StandardEvaluationContext context = contextFactory.apply(targetObject);
                context.setVariable(fieldName, value);
            } catch (Exception e) {
                logger.warn("Failed to set condition result field '{}': {}", fieldName, e.getMessage());
            }
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
        return evaluateMappingRuleConditions(rule, targetObject, null);
    }

    /**
     * Evaluate mapping rule conditions with YAML configuration for typed condition resolution.
     *
     * @param rule         The mapping rule
     * @param targetObject The target object for context
     * @param config       YAML configuration (needed for lookup/function conditions, may be null)
     * @return true if conditions are met (or no conditions specified), false otherwise
     */
    public boolean evaluateMappingRuleConditions(YamlEnrichment.MappingRule rule,
                                                  Object targetObject,
                                                  YamlRuleConfiguration config) {
        YamlEnrichment.ConditionGroup conditions = rule.getConditions();

        if (conditions == null) {
            logger.trace("No conditions specified for rule '" + rule.getId() + "', treating as default rule");
            return true;
        }

        return evaluateConditionGroup(conditions, targetObject, config);
    }
}
