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

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.model.YamlEnrichment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads and writes field values on target objects using Map access, reflection-based
 * getters/setters, or SpEL expressions. Also orchestrates field-mapping application
 * (source extraction → expression transformation → target write).
 *
 * <p>Extracted from {@link EnrichmentProcessor} (Phase 13 decomposition) to isolate
 * field access and mapping logic from enrichment orchestration.</p>
 *
 * @since 2.4
 */
public class FieldAccessor {

    private static final Logger logger = LoggerFactory.getLogger(FieldAccessor.class);

    private final ExpressionParser parser;
    private final ApexCacheManager cacheManager;
    private final Function<Object, StandardEvaluationContext> contextFactory;

    /**
     * @param parser         SpEL expression parser
     * @param cacheManager   Unified cache manager for compiled expressions
     * @param contextFactory Creates a {@link StandardEvaluationContext} from a root object.
     *                       Typically provided by {@link EnrichmentProcessor#createEvaluationContext}.
     */
    public FieldAccessor(ExpressionParser parser,
                         ApexCacheManager cacheManager,
                         Function<Object, StandardEvaluationContext> contextFactory) {
        this.parser = parser;
        this.cacheManager = cacheManager;
        this.contextFactory = contextFactory;
    }

    // ─── Field Read ──────────────────────────────────────────────────────

    /**
     * Get field value from an object using reflection or map access.
     *
     * @param object    The object to get the field from
     * @param fieldName The field name
     * @return The field value
     */
    public Object getFieldValue(Object object, String fieldName) {
        if (object == null || fieldName == null) {
            logger.debug("getFieldValue called with null object or fieldName");
            return null;
        }

        // If fieldName starts with #, treat it as a SpEL expression
        if (fieldName.startsWith("#")) {
            try {
                logger.trace("Evaluating SpEL expression for field: " + fieldName);
                StandardEvaluationContext context = contextFactory.apply(object);
                Expression expr = getOrCompileExpression(fieldName);
                Object value = expr.getValue(context);
                logger.trace("SpEL expression '" + fieldName + "' evaluated to: " + value);
                return value;
            } catch (Exception e) {
                logger.error("SPEL EXPRESSION EVALUATION FAILED: Failed to evaluate SpEL expression '" + fieldName +
                           "' for field lookup. Error: " + e.getMessage() + ". " +
                           "Check: (1) expression syntax is correct, (2) referenced fields/methods exist, " +
                           "(3) object context is valid. Returning NULL.");
                return null;
            }
        }

        // Simple field lookup for non-SpEL field names
        logger.trace("Getting field '" + fieldName + "' from object of type: " + object.getClass().getSimpleName());

        // Handle Map objects
        if (object instanceof Map) {
            Object value = ((Map<?, ?>) object).get(fieldName);
            logger.trace("Map lookup for '" + fieldName + "' returned: " + value);
            return value;
        }

        // Handle regular objects using proper getter methods
        try {
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method getter = object.getClass().getMethod(getterName);
            Object value = getter.invoke(object);
            logger.trace("Getter method lookup for '" + fieldName + "' returned: " + value);
            return value;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Try boolean getter (isXxx)
            try {
                String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method booleanGetter = object.getClass().getMethod(booleanGetterName);
                Object value = booleanGetter.invoke(object);
                logger.trace("Boolean getter method lookup for '" + fieldName + "' returned: " + value);
                return value;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                logger.debug("No getter method found for field '" + fieldName + "' on object of type " +
                           object.getClass().getSimpleName());
                return null;
            }
        }
    }

    // ─── Field Write ─────────────────────────────────────────────────────

    /**
     * Set field value on an object using reflection or map access.
     *
     * @param object    The object to set the field on
     * @param fieldName The field name
     * @param value     The value to set
     * @return true if the field was set successfully, false if it failed
     */
    public boolean setFieldValue(Object object, String fieldName, Object value) {
        if (object == null || fieldName == null) {
            logger.error("FIELD SET FAILED: setFieldValue called with null object or fieldName. " +
                        "object=" + (object == null ? "null" : object.getClass().getSimpleName()) +
                        ", fieldName=" + fieldName);
            return false;
        }

        // If fieldName starts with #, treat it as a SpEL expression for setting
        if (fieldName.startsWith("#")) {
            try {
                logger.trace("Setting value via SpEL expression: " + fieldName);
                StandardEvaluationContext context = contextFactory.apply(object);
                Expression expr = getOrCompileExpression(fieldName);
                expr.setValue(context, value);
                logger.trace("Successfully set field via SpEL '" + fieldName + "' to: " + value);
                return true;
            } catch (Exception e) {
                logger.error("SPEL EXPRESSION SET FAILED: Failed to set field via SpEL expression '" + fieldName +
                           "' to value '" + value + "'. Error: " + e.getMessage() + ". " +
                           "Check: (1) expression syntax is correct, (2) target field/property exists and is writable, " +
                           "(3) value type is compatible with target field type, (4) intermediate structures exist. " +
                           "Field was NOT set. This is a CRITICAL error that will be reported in the RuleResult.");
                return false;
            }
        }

        // Simple field setting for non-SpEL field names
        logger.trace("Setting field '" + fieldName + "' to value: " + value +
                     " on object of type: " + object.getClass().getSimpleName());

        // Handle Map objects
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            map.put(fieldName, value);
            logger.trace("Successfully set map key '" + fieldName + "' to: " + value);
            return true;
        }

        // Handle regular objects using proper setter methods
        try {
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method setter = object.getClass().getMethod(setterName, value.getClass());
            setter.invoke(object, value);
            logger.trace("Successfully set field '" + fieldName + "' to: " + value);
            return true;
        } catch (NoSuchMethodException e) {
            // Try with different parameter types if exact match fails
            try {
                String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method[] methods = object.getClass().getMethods();
                for (Method method : methods) {
                    if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        if (paramType.isAssignableFrom(value.getClass())) {
                            method.invoke(object, value);
                            logger.trace("Successfully set field '" + fieldName + "' to: " + value);
                            return true;
                        }
                    }
                }
                logger.error("SETTER METHOD NOT FOUND: No suitable setter method found for field '" + fieldName +
                           "' on object of type " + object.getClass().getSimpleName() + ". " +
                           "Check: (1) setter method exists (e.g., set" + Character.toUpperCase(fieldName.charAt(0)) +
                           fieldName.substring(1) + "), (2) field name is correct, (3) object type supports this field. " +
                           "Field was NOT set.");
                return false;
            } catch (IllegalAccessException | InvocationTargetException e2) {
                logger.error("SETTER INVOCATION FAILED: Could not invoke setter for field '" + fieldName +
                           "' on object of type " + object.getClass().getSimpleName() + ". Error: " + e2.getMessage() + ". " +
                           "Check: (1) setter method is accessible, (2) value type is compatible. Field was NOT set.", e2);
                return false;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("SETTER INVOCATION FAILED: Could not invoke setter for field '" + fieldName +
                       "' on object of type " + object.getClass().getSimpleName() + ". Error: " + e.getMessage() + ". " +
                       "Check: (1) setter method is accessible, (2) value type is compatible. Field was NOT set.", e);
            return false;
        }
    }

    // ─── Field Mapping Application ───────────────────────────────────────

    /**
     * Apply field mappings from lookup result to target object.
     *
     * <p><strong>Null Value Handling (v2.1):</strong></p>
     * <ul>
     *   <li>Required fields with null values → Enrichment fails</li>
     *   <li>Optional fields with null values → Attempts to set null; skips if setter incompatible</li>
     *   <li>Non-null values that fail to set → Enrichment fails</li>
     * </ul>
     *
     * @param fieldMappings The field mapping configurations
     * @param sourceObject  The source object (lookup result)
     * @param targetObject  The target object to enrich
     * @return The enriched target object, or null if any required field mapping failed
     */
    public Object applyFieldMappings(List<YamlEnrichment.FieldMapping> fieldMappings,
                                     Object sourceObject, Object targetObject) {
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            logger.debug("No field mappings to apply");
            return targetObject;
        }

        // Check if source object is a simple value (failed lookup)
        boolean isFailedLookup = sourceObject != null && !(sourceObject instanceof Map) &&
                                sourceObject.getClass().equals(String.class);

        // Track if any required field mapping failed
        boolean hasRequiredFieldFailure = false;

        if (isFailedLookup) {
            logger.debug("Source object is a simple value (likely failed lookup), applying only default values");
        }

        logger.debug("Applying " + fieldMappings.size() + " field mappings from " +
                   (sourceObject != null ? sourceObject.getClass().getSimpleName() : "null") +
                   " to " + targetObject.getClass().getSimpleName());

        for (YamlEnrichment.FieldMapping mapping : fieldMappings) {
            try {
                logger.debug("Processing field mapping: source-field='" + mapping.getSourceField() +
                           "' -> target-field='" + mapping.getTargetField() +
                           "', expression='" + mapping.getExpression() +
                           "', default-value='" + mapping.getDefaultValue() + "'");

                Object sourceValue = null;
                boolean isConstantMapping = "constant".equals(mapping.getSourceField());
                boolean isImplicitConstant = mapping.getSourceField() == null || mapping.getSourceField().trim().isEmpty();

                // For constant mappings, skip field lookup and directly evaluate expression
                if (isConstantMapping || isImplicitConstant) {
                    logger.debug("Constant mapping detected (explicit=" + isConstantMapping + "), will evaluate expression directly");
                    if (mapping.getExpression() == null || mapping.getExpression().trim().isEmpty()) {
                        logger.error("FIELD MAPPING FAILED: source-field 'constant' (or missing) requires 'expression' or 'transformation' to be specified for target-field '" + mapping.getTargetField() + "'");
                        continue;
                    }
                } else if (!isFailedLookup) {
                    sourceValue = getFieldValue(sourceObject, mapping.getSourceField());
                    logger.debug("Source value for '" + mapping.getSourceField() + "': " + sourceValue);
                }

                // Use default value if source value is null (or for failed lookups)
                Object valueToSet = sourceValue != null ? sourceValue : mapping.getDefaultValue();
                logger.trace("Value to set (after defaults): " + valueToSet);

                // Apply expression if specified
                if (mapping.getExpression() != null && !mapping.getExpression().trim().isEmpty()) {
                    valueToSet = applyExpression(mapping.getExpression(), valueToSet, targetObject);
                    logger.trace("Value after expression: " + valueToSet);
                }

                // Consolidated handling for setting mapped value
                String targetField = mapping.getTargetField();
                String sourceField = mapping.getSourceField();
                boolean isRequired = mapping.getRequired() != null && mapping.getRequired();

                if (valueToSet == null && isRequired) {
                    logger.error("REQUIRED FIELD MAPPING FAILED: source-field '" + sourceField +
                               "' -> target-field '" + targetField +
                               "' produced NULL value but field is marked as required. " +
                               "Check: (1) source field exists, (2) expression is valid, (3) default-value is provided.");
                    hasRequiredFieldFailure = true;
                } else {
                    boolean setSuccess = setFieldValue(targetObject, targetField, valueToSet);
                    if (setSuccess) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Successfully mapped field: " + sourceField + " -> " +
                                       targetField + " (value: " + valueToSet + ")");
                        }
                    } else {
                        if (valueToSet == null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Skipping null set for non-required target field '" + targetField + "' (setter not compatible or missing)");
                            }
                        } else {
                            logger.error("FIELD SET FAILED: source-field '" + sourceField +
                                       "' -> target-field '" + targetField +
                                       "' with value '" + valueToSet + "'. The setFieldValue operation failed. " +
                                       "Check: (1) target path exists, (2) intermediate structures are pre-created, " +
                                       "(3) SpEL expression is valid. This failure will be reported in the RuleResult.");
                            hasRequiredFieldFailure = true;
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("FIELD MAPPING EXCEPTION: Failed to apply field mapping: " +
                          mapping.getSourceField() + " -> " + mapping.getTargetField() +
                          ": " + e.getMessage() + ". This failure will be reported in the RuleResult.", e);
                hasRequiredFieldFailure = true;
            }
        }

        // Return null if any field mapping failed to signal enrichment failure
        if (hasRequiredFieldFailure) {
            return null;
        }

        return targetObject;
    }

    // ─── Expression Application ──────────────────────────────────────────

    /**
     * Apply expression to a value.
     *
     * @param expression The SpEL expression
     * @param value      The value to transform
     * @param context    The context object
     * @return The transformed value
     */
    public Object applyExpression(String expression, Object value, Object context) {
        try {
            StandardEvaluationContext evalContext = contextFactory.apply(context);
            evalContext.setVariable("value", value);

            Expression expr = getOrCompileExpression(expression);
            return expr.getValue(evalContext);

        } catch (Exception e) {
            logger.warn("Failed to apply expression '" + expression +
                      "' to value: " + value, e);
            return value; // Return original value on expression failure
        }
    }

    // ─── Utility Conversion ──────────────────────────────────────────────

    /**
     * Convert an object to a Map for consistent data handling.
     *
     * @param object The object to convert
     * @return A Map representation of the object
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToMap(Object object) {
        if (object instanceof Map) {
            return new HashMap<>((Map<String, Object>) object);
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("data", object);
            return result;
        }
    }

    // ─── Expression Caching ──────────────────────────────────────────────

    /**
     * Get or compile SpEL expression with caching.
     *
     * @param expressionString The expression string
     * @return The compiled expression
     */
    Expression getOrCompileExpression(String expressionString) {
        Object cached = cacheManager.get(ApexCacheManager.EXPRESSION_CACHE, expressionString);
        if (cached instanceof Expression) {
            return (Expression) cached;
        }

        Expression expression = parser.parseExpression(expressionString);
        cacheManager.put(ApexCacheManager.EXPRESSION_CACHE, expressionString, expression);
        return expression;
    }
}
