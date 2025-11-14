package dev.mars.apex.core.service.transformation;

import dev.mars.apex.core.config.yaml.YamlTransformation;
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
     */
    public Object processTransformations(List<YamlTransformation> transformations, Object targetObject) {
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
                logger.error("Failed to process transformation: {} - {}", transformation.getId(), e.getMessage(), e);
                // Continue processing other transformations
            }
        }
        
        logger.info("Processed {} out of {} transformations", processedCount, transformations.size());
        return transformedObject;
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
            logger.warn("Transformation {} has no type specified", transformation.getId());
            return targetObject;
        }
        
        return switch (type) {
            case "field-transformation" -> processFieldTransformation(transformation, targetObject);
            case "object-transformation" -> processObjectTransformation(transformation, targetObject);
            case "conditional-transformation" -> processConditionalTransformation(transformation, targetObject);
            default -> {
                logger.warn("Unknown transformation type: {} for transformation {}", type, transformation.getId());
                yield targetObject;
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
            logger.error("Failed to process field transformation {}: {}",
                transformation.getId(), e.getMessage(), e);
            return targetObject;
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

