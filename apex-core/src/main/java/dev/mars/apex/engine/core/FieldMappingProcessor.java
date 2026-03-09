package dev.mars.apex.engine.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;

/**
 * Processes field mappings and code evaluations for rule results.
 *
 * <p>Handles two related concerns:</p>
 * <ul>
 *   <li><strong>Code evaluation</strong> — evaluates success/error code expressions
 *       (constant strings or SpEL expressions) to produce result codes</li>
 *   <li><strong>Field mapping</strong> — applies {@code map-to-field} expressions to populate
 *       enriched data from the evaluation context</li>
 * </ul>
 *
 * <p>Extracted from {@link UnifiedRuleEvaluator} to isolate mapping/code logic
 * from core evaluation flow.</p>
 *
 * @author Mark A Ray-Smith
 * @since 2026-02-28
 * @version 1.0
 */
public class FieldMappingProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FieldMappingProcessor.class);

    private final ExpressionParser parser;

    /**
     * Create a new FieldMappingProcessor.
     *
     * @param parser The SpEL expression parser used to evaluate mapping expressions
     */
    public FieldMappingProcessor(ExpressionParser parser) {
        this.parser = parser;
    }

    /**
     * Evaluate a code expression (either constant or SpEL).
     * Supports both constant strings and SpEL expressions for codes.
     *
     * @param codeExpression The code expression to evaluate (e.g., "SUCCESS_CODE" or "#amount > 100 ? 'HIGH' : 'LOW'")
     * @param context The evaluation context for SpEL expressions
     * @return The evaluated code string, or null if evaluation fails
     */
    public String evaluateCode(String codeExpression, EvaluationContext context) {
        if (codeExpression == null || codeExpression.trim().isEmpty()) {
            return null;
        }

        try {
            // Check if it's a SpEL expression (starts with #)
            if (codeExpression.trim().startsWith("#")) {
                Expression exp = parser.parseExpression(codeExpression);
                Object result = exp.getValue(context);
                return result != null ? result.toString() : null;
            } else {
                // It's a constant string
                return codeExpression;
            }
        } catch (Exception e) {
            final String errorCode = classifyCodeEvaluationError(e);
            logger.error("[{}] Error evaluating code expression '{}': {}", errorCode, codeExpression, e.getMessage());
            logger.debug("Full exception details for code expression '{}':", codeExpression, e);
            return null;
        }
    }

    /**
     * Apply field mappings to the enriched data.
     * Supports generic field mapping using SpEL expressions.
     *
     * @param mapToField The field mapping configuration
     * @param context The evaluation context for SpEL expressions (must be StandardEvaluationContext)
     * @param enrichedData The enriched data map to update with mapped values
     * @param successCode The evaluated success code (available as #success_code in expressions)
     * @param errorCode The evaluated error code (available as #error_code in expressions)
     */
    public void applyFieldMappings(List<String> mapToField, StandardEvaluationContext context, Map<String, Object> enrichedData, String successCode, String errorCode) {
        if (mapToField == null) {
            return;
        }

        try {
            // Create a context that includes the codes
            StandardEvaluationContext mappingContext = context;
            if (successCode != null) {
                // Use underscore instead of hyphen for SpEL compatibility
                mappingContext.setVariable("success_code", successCode);
            }
            if (errorCode != null) {
                // Use underscore instead of hyphen for SpEL compatibility
                mappingContext.setVariable("error_code", errorCode);
            }

            // Apply each mapping
            for (String mapping : mapToField) {
                applyFieldMapping(mapping, mappingContext, enrichedData);
            }
        } catch (Exception e) {
            logger.error("[APEX-RULE-004] Error applying field mappings: {}", e.getMessage());
            logger.debug("Full exception details for field mappings:", e);
        }
    }

    /**
     * Apply a single field mapping expression.
     * Parses expressions like "fieldName = #success_code" or "status = #amount > 100 ? 'HIGH' : 'LOW'"
     *
     * @param mapping The mapping expression
     * @param context The evaluation context
     * @param enrichedData The enriched data map to update
     */
    private void applyFieldMapping(String mapping, StandardEvaluationContext context, Map<String, Object> enrichedData) {
        try {
            // Parse the mapping: "fieldName = expression"
            String[] parts = mapping.split("=", 2);
            if (parts.length != 2) {
                logger.warn("[APEX-RULE-004] Invalid field mapping format: {}. Expected 'fieldName = expression'", mapping);
                return;
            }

            String fieldName = parts[0].trim();
            String expression = parts[1].trim();

            // Evaluate the expression
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);

            // Store the mapped value in enriched data
            enrichedData.put(fieldName, value);
            logger.debug("Applied field mapping: {} = {}", fieldName, value);
        } catch (Exception e) {
            logger.error("[APEX-RULE-004] Error applying field mapping '{}': {}", mapping, e.getMessage());
            logger.debug("Full exception details for field mapping '{}':", mapping, e);
        }
    }

    /**
     * Classify the error code for a code evaluation failure.
     * Maps exception types to APEX error codes for accurate diagnostics.
     *
     * @param exception The exception that occurred
     * @return The APEX error code
     */
    private static String classifyCodeEvaluationError(Exception exception) {
        if (exception instanceof SpelParseException) {
            return "APEX-RULE-001";
        }
        String message = exception.getMessage();
        if (message != null && (message.contains("EL1001E") || message.contains("Type conversion"))) {
            return "APEX-RULE-003";
        }
        if (exception instanceof SpelEvaluationException) {
            return "APEX-RULE-001";
        }
        return "APEX-RULE-999";
    }
}
