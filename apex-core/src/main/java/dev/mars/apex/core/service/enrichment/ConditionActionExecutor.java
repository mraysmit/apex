/*
 * Copyright (c) 2025-2026 Mars Software - All Rights Reserved.
 *
 * This file is part of the APEX Rules Engine.
 * Unauthorized copying or distribution is prohibited.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.4
 * @created 2026-03-19
 */
package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.EnrichmentGroupFactory;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
import dev.mars.apex.engine.model.EnrichmentGroup;
import dev.mars.apex.engine.model.EnrichmentGroupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Executes lookup and function actions for typed condition predicates.
 *
 * <p>Injected into {@link EnrichmentConditionEvaluator} to keep the evaluator thin
 * (dispatch + SpEL) while isolating the heavy dependencies
 * ({@link LookupEnrichmentHandler}, {@link EnrichmentGroupExecutor}).</p>
 *
 * @since 2.4
 */
public class ConditionActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ConditionActionExecutor.class);

    private final LookupEnrichmentHandler lookupHandler;
    private final FieldAccessor fieldAccessor;
    private final ExpressionParser parser;
    private final Function<Object, StandardEvaluationContext> contextFactory;

    // Lazy reference — same pattern as EnrichmentProcessor to break circular dependency
    private Supplier<EnrichmentGroupExecutor> enrichmentGroupExecutorSupplier;

    public ConditionActionExecutor(LookupEnrichmentHandler lookupHandler,
                                   FieldAccessor fieldAccessor,
                                   ExpressionParser parser,
                                   Function<Object, StandardEvaluationContext> contextFactory) {
        this.lookupHandler = lookupHandler;
        this.fieldAccessor = fieldAccessor;
        this.parser = parser;
        this.contextFactory = contextFactory;
    }

    public void setEnrichmentGroupExecutorSupplier(Supplier<EnrichmentGroupExecutor> supplier) {
        this.enrichmentGroupExecutorSupplier = supplier;
    }

    // ─── Lookup Condition Execution ──────────────────────────────────────

    /**
     * Execute a lookup for a condition predicate.
     * Resolves the lookup service, extracts the key, performs the lookup,
     * and returns the result (to be stashed into {@code result-field}).
     *
     * @param rule         The condition rule with lookup-config
     * @param targetObject The shared context
     * @param config       YAML configuration for data-source-ref resolution
     * @return The lookup result, or null if lookup fails/returns no data
     */
    public Object executeLookup(SharedConditionRule rule, Object targetObject,
                                YamlRuleConfiguration config) {
        YamlEnrichment.LookupConfig lookupConfig = rule.getLookupConfig();
        if (lookupConfig == null) {
            logger.warn("Lookup condition has no lookup-config");
            return null;
        }

        try {
            LookupService lookupService = lookupHandler.resolveLookupService(
                    "condition-lookup", lookupConfig, config);

            StandardEvaluationContext context = contextFactory.apply(targetObject);
            Expression keyExpr = parser.parseExpression(lookupConfig.getLookupKey());
            Object lookupKey = keyExpr.getValue(context);

            if (lookupKey == null) {
                logger.warn("Lookup condition key expression '{}' evaluated to null",
                        lookupConfig.getLookupKey());
                return null;
            }

            Object result = lookupHandler.performLookup(lookupService, lookupKey, lookupConfig);
            logger.debug("Lookup condition result for key '{}': {}", lookupKey, result);
            return result;
        } catch (Exception e) {
            logger.warn("Lookup condition execution failed: {}", e.getMessage());
            logger.debug("Stack trace for lookup condition failure:", e);
            return null;
        }
    }

    // ─── Function Condition Execution ────────────────────────────────────

    /**
     * Execute a function (enrichment group invocation) for a condition predicate.
     * Applies input parameters, invokes the group, and returns the output value
     * (to be stashed into {@code output-field}).
     *
     * @param rule         The condition rule with enrichment-group-ref and input-parameters
     * @param targetObject The shared context
     * @param config       YAML configuration for enrichment group resolution
     * @return The extracted output value, or null if invocation fails
     */
    public Object executeFunction(SharedConditionRule rule, Object targetObject,
                                  YamlRuleConfiguration config) {
        String groupRef = rule.getEnrichmentGroupRef();
        if (groupRef == null || groupRef.trim().isEmpty()) {
            logger.warn("Function condition has no enrichment-group-ref");
            return null;
        }

        if (enrichmentGroupExecutorSupplier == null) {
            logger.warn("Function condition requires EnrichmentGroupExecutor but none is configured");
            return null;
        }

        try {
            // 1. Apply input-parameters into shared context
            List<YamlEnrichment.FieldMapping> inputParams = rule.getInputParameters();
            if (inputParams != null && !inputParams.isEmpty()) {
                applyInputParameters(inputParams, targetObject);
            }

            // 2. Resolve enrichment group
            List<EnrichmentGroup> groups = EnrichmentGroupFactory.buildEnrichmentGroups(config);
            EnrichmentGroup targetGroup = groups.stream()
                    .filter(g -> groupRef.equals(g.getId()))
                    .findFirst()
                    .orElse(null);

            if (targetGroup == null) {
                logger.warn("Function condition enrichment-group-ref '{}' not found", groupRef);
                return null;
            }

            // 3. Execute the group
            EnrichmentGroupExecutor executor = enrichmentGroupExecutorSupplier.get();
            EnrichmentGroupResult groupResult = executor.processEnrichmentGroup(
                    targetGroup, targetObject, config);

            if (!groupResult.isSuccess()) {
                logger.warn("Function condition enrichment group '{}' execution failed: {}",
                        groupRef, groupResult.getMessage());
                return null;
            }

            // 4. Extract output-field
            String outputField = rule.getOutputField();
            if (outputField == null || outputField.trim().isEmpty()) {
                logger.warn("Function condition has no output-field for enrichment-group-ref '{}'", groupRef);
                return null;
            }

            Object outputValue = fieldAccessor.getFieldValue(targetObject, outputField);
            logger.debug("Function condition output: {} = {} (from group '{}')",
                    outputField, outputValue, groupRef);
            return outputValue;
        } catch (Exception e) {
            logger.warn("Function condition execution failed for group '{}': {}", groupRef, e.getMessage());
            logger.debug("Stack trace for function condition failure:", e);
            return null;
        }
    }

    // ─── Input Parameter Binding ─────────────────────────────────────────

    private void applyInputParameters(List<YamlEnrichment.FieldMapping> params, Object targetObject) {
        for (YamlEnrichment.FieldMapping param : params) {
            try {
                Object value;
                boolean isConstant = "constant".equals(param.getSourceField())
                        || param.getSourceField() == null
                        || param.getSourceField().trim().isEmpty();

                if (isConstant) {
                    if (param.getExpression() != null && !param.getExpression().trim().isEmpty()) {
                        StandardEvaluationContext ctx = contextFactory.apply(targetObject);
                        Expression expr = parser.parseExpression(param.getExpression());
                        value = expr.getValue(ctx);
                    } else {
                        value = param.getDefaultValue();
                    }
                } else {
                    StandardEvaluationContext ctx = contextFactory.apply(targetObject);
                    String sourceExpr = param.getSourceField().startsWith("#")
                            ? param.getSourceField()
                            : "#" + param.getSourceField();
                    Expression expr = parser.parseExpression(sourceExpr);
                    value = expr.getValue(ctx);
                }

                if (param.getTargetField() != null) {
                    fieldAccessor.setFieldValue(targetObject, param.getTargetField(), value);
                    logger.debug("Function condition input: {} -> {} = {}",
                            param.getSourceField(), param.getTargetField(), value);
                }
            } catch (Exception e) {
                logger.warn("Failed to apply function condition input parameter '{}' -> '{}': {}",
                        param.getSourceField(), param.getTargetField(), e.getMessage());
            }
        }
    }
}
