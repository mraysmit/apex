package dev.mars.apex.core.service.transformation;

import dev.mars.apex.core.config.yaml.YamlTransformation;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.model.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;

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

/**
 * Processor for executing YAML-defined transformation configurations.
 * 
 * This class handles the execution of transformations defined in YAML files,
 * supporting field transformations with SpEL expressions.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-14
 * @version 1.0
 */
public class YamlTransformationProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlTransformationProcessor.class);
    
    private final SpelExpressionParser parser;
    private final Map<String, Expression> expressionCache;
    
    /**
     * Create a new transformation processor.
     */
    public YamlTransformationProcessor() {
        this.parser = new SpelExpressionParser();
        this.expressionCache = new HashMap<>();
        logger.info("YamlTransformationProcessor initialized");
    }
    
    /**
     * Process a list of transformations on a target object.
     *
     * @param transformations List of YAML transformation configurations
     * @param targetObject The object to transform
     * @return The transformed object
     * @deprecated since 1.1, for removal in 2.0. This method returns Object and cannot propagate errors properly.
     *             Use {@link #processTransformationsWithResult(List, Object)} instead, which returns RuleResult
     *             with proper error tracking and failure messages.
     *             <p><strong>CRITICAL FLAW:</strong> This method catches and logs exceptions but continues processing,
     *             making it impossible for callers to detect failures. Errors are lost and only appear in logs.</p>
     *             <p><strong>Migration:</strong> Replace {@code Object result = processor.processTransformations(transformations, data)}
     *             with {@code RuleResult result = processor.processTransformationsWithResult(transformations, data)}
     *             and check {@code result.getResultType() == ResultType.ERROR} to detect failures.</p>
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public Object processTransformations(List<YamlTransformation> transformations, Object targetObject) {
        // Runtime deprecation warning
        logger.warn("DEPRECATED: processTransformations() is deprecated since 1.1 and will be removed in 2.0. " +
                    "Use processTransformationsWithResult() instead for proper error propagation. " +
                    "This method cannot propagate errors to callers - failures are only logged.");

        if (transformations == null || transformations.isEmpty()) {
            logger.debug("No transformations to process");
            return targetObject;
        }

        logger.info("Processing {} transformations", transformations.size());

        Object transformedObject = targetObject;
        int processedCount = 0;

        for (YamlTransformation transformation : transformations) {
            try {
                if (shouldProcessTransformation(transformation, transformedObject)) {
                    transformedObject = processTransformation(transformation, transformedObject);
                    processedCount++;
                    logger.debug("Successfully processed transformation: {}", transformation.getId());
                } else {
                    logger.debug("Skipping transformation (condition not met or disabled): {}", transformation.getId());
                }
            } catch (Exception e) {
                logger.error("CRITICAL: Transformation failure in deprecated method cannot be propagated to caller: {} - {}",
                            transformation.getId(), e.getMessage(), e);
                // Continue processing other transformations
            }
        }

        logger.info("Processed {} out of {} transformations", processedCount, transformations.size());
        return transformedObject;
    }

    /**
     * Process a list of transformations on a target object and return detailed results.
     * This method provides programmatic access to transformation success/failure status and detailed error information.
     *
     * @param transformations List of YAML transformation configurations
     * @param targetObject The object to transform
     * @return A RuleResult containing success status, transformed data, and failure messages
     */
    public RuleResult processTransformationsWithResult(List<YamlTransformation> transformations, Object targetObject) {
        if (transformations == null || transformations.isEmpty()) {
            logger.debug("No transformations to process");
            // Return success with original data
            if (targetObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) targetObject;
                return RuleResult.enrichmentSuccess(dataMap, SeverityConstants.INFO);
            } else {
                return RuleResult.match("transformations", "No transformations to process", SeverityConstants.INFO);
            }
        }

        logger.info("Processing {} transformations with result tracking", transformations.size());

        Object transformedObject = targetObject;
        int processedCount = 0;

        for (YamlTransformation transformation : transformations) {
            try {
                if (shouldProcessTransformation(transformation, transformedObject)) {
                    transformedObject = processTransformation(transformation, transformedObject);
                    processedCount++;
                    logger.debug("Successfully processed transformation: {}", transformation.getId());
                } else {
                    logger.debug("Skipping transformation (condition not met or disabled): {}", transformation.getId());
                }
            } catch (Exception e) {
                logger.error("CRITICAL: Transformation failed: {} - {}", transformation.getId(), e.getMessage(), e);

                // Return error result immediately (fail-fast behavior)
                return RuleResult.error(
                    "transformation:" + transformation.getId(),
                    "Transformation processing failed: " + e.getMessage(),
                    SeverityConstants.ERROR
                );
            }
        }

        logger.info("Processed {} out of {} transformations", processedCount, transformations.size());

        // Return success result with transformed data
        if (transformedObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) transformedObject;
            return RuleResult.enrichmentSuccess(dataMap, SeverityConstants.INFO);
        } else {
            return RuleResult.match(
                "transformations",
                "All transformations completed successfully",
                SeverityConstants.INFO
            );
        }
    }

    /**
     * Process a single transformation on a target object and return detailed results.
     * This method provides programmatic access to transformation success/failure status and detailed error information.
     *
     * @param transformation The transformation configuration
     * @param targetObject The object to transform
     * @return A RuleResult containing success status, transformed data, and failure messages
     */
    public RuleResult processTransformationWithResult(YamlTransformation transformation, Object targetObject) {
        if (transformation == null) {
            logger.error("CRITICAL: Null transformation provided");
            return RuleResult.error("transformation:null", "Null transformation provided", SeverityConstants.ERROR);
        }

        logger.debug("Processing single transformation with result tracking: {}", transformation.getId());

        try {
            if (!shouldProcessTransformation(transformation, targetObject)) {
                logger.debug("Skipping transformation (condition not met or disabled): {}", transformation.getId());
                // Return success with original data (transformation was skipped, not failed)
                if (targetObject instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) targetObject;
                    return RuleResult.enrichmentSuccess(dataMap, SeverityConstants.INFO);
                } else {
                    return RuleResult.match(
                        "transformation:" + transformation.getId(),
                        "Transformation skipped (condition not met)",
                        SeverityConstants.INFO
                    );
                }
            }

            Object transformedObject = processTransformation(transformation, targetObject);
            logger.debug("Successfully processed transformation: {}", transformation.getId());

            // Return success result with transformed data
            if (transformedObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) transformedObject;
                return RuleResult.enrichmentSuccess(dataMap, SeverityConstants.INFO);
            } else {
                return RuleResult.match(
                    "transformation:" + transformation.getId(),
                    "Transformation completed successfully",
                    SeverityConstants.INFO
                );
            }
        } catch (Exception e) {
            logger.error("CRITICAL: Transformation failed: {} - {}", transformation.getId(), e.getMessage(), e);
            return RuleResult.error(
                "transformation:" + transformation.getId(),
                "Transformation processing failed: " + e.getMessage(),
                SeverityConstants.ERROR
            );
        }
    }

    /**
     * Check if a transformation should be processed.
     *
     * @param transformation The transformation configuration
     * @param targetObject The target object
     * @return true if transformation should be processed
     */
    private boolean shouldProcessTransformation(YamlTransformation transformation, Object targetObject) {
        // Check if transformation is enabled
        if (transformation.getEnabled() != null && !transformation.getEnabled()) {
            return false;
        }
        
        // Check condition if present
        if (transformation.getCondition() != null && !transformation.getCondition().trim().isEmpty()) {
            try {
                StandardEvaluationContext context = createEvaluationContext(targetObject);
                Expression conditionExpr = getOrCompileExpression(transformation.getCondition());
                Boolean result = conditionExpr.getValue(context, Boolean.class);
                return result != null && result;
            } catch (Exception e) {
                logger.warn("Failed to evaluate transformation condition for {}: {}", 
                    transformation.getId(), e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Process a single transformation on a target object.
     * 
     * @param transformation The transformation configuration
     * @param targetObject The object to transform
     * @return The transformed object
     */
    private Object processTransformation(YamlTransformation transformation, Object targetObject) {
        String type = transformation.getType();

        if (type == null) {
            String errorMsg = "Transformation " + transformation.getId() + " has no type specified";
            logger.error("CRITICAL: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        return switch (type) {
            case "field-transformation" -> processFieldTransformation(transformation, targetObject);
            case "object-transformation" -> processObjectTransformation(transformation, targetObject);
            case "conditional-transformation" -> processConditionalTransformation(transformation, targetObject);
            default -> {
                String errorMsg = "Unknown transformation type: " + type + " for transformation " + transformation.getId();
                logger.error("CRITICAL: {}", errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        };
    }

    /**
     * Process a field transformation.
     * Supports simple field transformations with source-field, target-field, and expression.
     *
     * @param transformation The transformation configuration
     * @param targetObject The object to transform
     * @return The transformed object
     */
    private Object processFieldTransformation(YamlTransformation transformation, Object targetObject) {
        try {
            // Get source field value
            Object sourceValue = null;
            if (transformation.getSourceField() != null) {
                sourceValue = getFieldValue(targetObject, transformation.getSourceField());
            }

            // Evaluate expression if present
            Object transformedValue = sourceValue;
            if (transformation.getExpression() != null && !transformation.getExpression().trim().isEmpty()) {
                StandardEvaluationContext context = createEvaluationContext(targetObject);
                // Make source value available in expression context
                if (sourceValue != null) {
                    context.setVariable("value", sourceValue);
                }
                Expression expr = getOrCompileExpression(transformation.getExpression());
                transformedValue = expr.getValue(context);
            }

            // Set target field value
            if (transformation.getTargetField() != null) {
                setFieldValue(targetObject, transformation.getTargetField(), transformedValue);
                logger.debug("Field transformation completed: {} -> {} = {}",
                    transformation.getSourceField(), transformation.getTargetField(), transformedValue);
            }

            return targetObject;
        } catch (Exception e) {
            logger.error("CRITICAL: Field transformation failed for {}: {}",
                transformation.getId(), e.getMessage(), e);
            throw new RuntimeException("Field transformation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process an object transformation.
     *
     * @param transformation The transformation configuration
     * @param targetObject The object to transform
     * @return The transformed object
     */
    private Object processObjectTransformation(YamlTransformation transformation, Object targetObject) {
        logger.warn("Object transformation not yet implemented for transformation: {}", transformation.getId());
        return targetObject;
    }

    /**
     * Process a conditional transformation.
     *
     * @param transformation The transformation configuration
     * @param targetObject The object to transform
     * @return The transformed object
     */
    private Object processConditionalTransformation(YamlTransformation transformation, Object targetObject) {
        logger.warn("Conditional transformation not yet implemented for transformation: {}", transformation.getId());
        return targetObject;
    }

    /**
     * Get field value from an object.
     * Supports both Map and POJO access.
     *
     * @param object The object
     * @param fieldName The field name
     * @return The field value
     */
    @SuppressWarnings("unchecked")
    private Object getFieldValue(Object object, String fieldName) {
        if (object instanceof Map) {
            return ((Map<String, Object>) object).get(fieldName);
        }

        // For POJOs, use SpEL to access the field
        try {
            StandardEvaluationContext context = createEvaluationContext(object);
            Expression expr = getOrCompileExpression("#" + fieldName);
            return expr.getValue(context);
        } catch (Exception e) {
            logger.warn("Failed to get field value for {}: {}", fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * Set field value on an object.
     * Supports both Map and POJO access.
     *
     * @param object The object
     * @param fieldName The field name
     * @param value The value to set
     */
    @SuppressWarnings("unchecked")
    private void setFieldValue(Object object, String fieldName, Object value) {
        if (object instanceof Map) {
            ((Map<String, Object>) object).put(fieldName, value);
            return;
        }

        // For POJOs, use SpEL to set the field
        try {
            StandardEvaluationContext context = createEvaluationContext(object);
            Expression expr = getOrCompileExpression("#" + fieldName);
            expr.setValue(context, value);
        } catch (Exception e) {
            logger.error("Failed to set field value for {}: {}", fieldName, e.getMessage());
        }
    }

    /**
     * Create evaluation context for SpEL expressions.
     *
     * @param rootObject The root object for the context
     * @return The evaluation context
     */
    private StandardEvaluationContext createEvaluationContext(Object rootObject) {
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);

        // Add custom property accessor for Maps to enable nested field access
        context.addPropertyAccessor(new dev.mars.apex.core.engine.config.MapPropertyAccessor());

        return context;
    }

    /**
     * Get or compile SpEL expression with caching.
     *
     * @param expressionString The expression string
     * @return The compiled expression
     */
    private Expression getOrCompileExpression(String expressionString) {
        return expressionCache.computeIfAbsent(expressionString, parser::parseExpression);
    }
}

