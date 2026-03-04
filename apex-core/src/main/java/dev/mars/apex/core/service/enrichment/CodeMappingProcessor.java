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

import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;

/**
 * Evaluates success/error code expressions and applies code-to-field mappings.
 *
 * <p>Extracted from {@link EnrichmentProcessor} (Phase 13 decomposition) to isolate
 * code evaluation and mapping logic from enrichment orchestration.</p>
 *
 * @since 2.4
 */
public class CodeMappingProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CodeMappingProcessor.class);

    private final ExpressionParser parser;
    private final ExpressionEvaluatorService evaluatorService;
    private final FieldAccessor fieldAccessor;

    /**
     * @param parser            SpEL expression parser
     * @param evaluatorService  Expression evaluator for creating evaluation contexts
     * @param fieldAccessor     Field accessor for setting values on target objects
     */
    public CodeMappingProcessor(ExpressionParser parser,
                                ExpressionEvaluatorService evaluatorService,
                                FieldAccessor fieldAccessor) {
        this.parser = parser;
        this.evaluatorService = evaluatorService;
        this.fieldAccessor = fieldAccessor;
    }

    // ─── Code Evaluation ─────────────────────────────────────────────────

    /**
     * Evaluate a success or error code expression.
     * Phase 4 Enhancement: Supports both constant strings and SpEL expressions.
     *
     * @param codeExpression The code expression (constant or SpEL starting with #)
     * @param context        The evaluation context for SpEL expressions
     * @return The evaluated code string, or null if expression is null or evaluation fails
     */
    public String evaluateCode(String codeExpression, StandardEvaluationContext context) {
        if (codeExpression == null || codeExpression.trim().isEmpty()) {
            return null;
        }

        try {
            if (codeExpression.trim().startsWith("#")) {
                Expression exp = parser.parseExpression(codeExpression);
                Object result = exp.getValue(context);
                return result != null ? result.toString() : null;
            } else {
                return codeExpression;
            }
        } catch (Exception e) {
            logger.warn("Error evaluating code expression '{}': {}", codeExpression, e.getMessage());
            logger.debug("Stack trace for code expression evaluation error:", e);
            return null;
        }
    }

    // ─── Code Field Mappings ─────────────────────────────────────────────

    /**
     * Apply field mappings for success/error codes to the target object.
     * Phase 4 Enhancement: Supports generic field mapping using SpEL expressions.
     *
     * @param mapToField   The field mapping configuration
     * @param context      The evaluation context for SpEL expressions
     * @param targetObject The target object to update with mapped values
     * @param successCode  The evaluated success code (available as #success_code in expressions)
     * @param errorCode    The evaluated error code (available as #error_code in expressions)
     */
    public void applyCodeFieldMappings(List<String> mapToField, StandardEvaluationContext context, Object targetObject,
                                       String successCode, String errorCode) {
        if (mapToField == null) {
            return;
        }

        try {
            // Create a new context with success_code and error_code variables
            StandardEvaluationContext mappingContext = evaluatorService.createEvaluationContext(context.getRootObject().getValue());

            // Copy ALL variables from the original context using reflection
            try {
                java.lang.reflect.Field variablesField = StandardEvaluationContext.class.getDeclaredField("variables");
                variablesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> originalVariables = (Map<String, Object>) variablesField.get(context);
                if (originalVariables != null) {
                    for (Map.Entry<String, Object> entry : originalVariables.entrySet()) {
                        mappingContext.setVariable(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to copy variables from original context: " + e.getMessage());
                if (context.lookupVariable("this") != null) {
                    mappingContext.setVariable("this", context.lookupVariable("this"));
                }
            }

            if (successCode != null) {
                mappingContext.setVariable("success_code", successCode);
            }
            if (errorCode != null) {
                mappingContext.setVariable("error_code", errorCode);
            }

            for (String mapping : mapToField) {
                applyCodeFieldMapping(mapping, mappingContext, targetObject);
            }
        } catch (Exception e) {
            logger.warn("Error applying field mappings: {}", e.getMessage());
            logger.debug("Stack trace for field mappings application error:", e);
        }
    }

    /**
     * Apply a single field mapping expression for success/error codes.
     * Parses expressions like "fieldName = #success_code" or "status = #amount &gt; 100 ? 'HIGH' : 'LOW'"
     *
     * @param mapping      The mapping expression
     * @param context      The evaluation context
     * @param targetObject The target object to update
     */
    void applyCodeFieldMapping(String mapping, StandardEvaluationContext context, Object targetObject) {
        try {
            String[] parts = mapping.split("=", 2);
            if (parts.length != 2) {
                logger.warn("Invalid field mapping format: " + mapping + ". Expected 'fieldName = expression'");
                return;
            }

            String fieldName = parts[0].trim();
            String expression = parts[1].trim();

            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);

            fieldAccessor.setFieldValue(targetObject, fieldName, value);
            logger.info("Applied field mapping: " + fieldName + " = " + value);
        } catch (Exception e) {
            logger.warn("Error applying field mapping '{}': {}", mapping, e.getMessage());
            logger.debug("Stack trace for field mapping application error:", e);
        }
    }
}
