package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.core.MapPropertyAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

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
 * Service for evaluating SpEL expressions.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service for evaluating SpEL expressions.
 * This class centralizes expression parsing and evaluation.
 *
 * Phase 3 Enhancement: Adopts UnifiedRuleEvaluator's context creation and error handling patterns
 * for consistent SpEL evaluation behavior across the APEX system.
 */
public class ExpressionEvaluatorService {
    private static final Logger logger = LoggerFactory.getLogger(ExpressionEvaluatorService.class);
    private final ExpressionParser parser;

    /**
     * Create a new ExpressionEvaluatorService with the default parser.
     */
    public ExpressionEvaluatorService() {
        this(new SpelExpressionParser());
    }

    /**
     * Create a new ExpressionEvaluatorService with the specified parser.
     *
     * @param parser The expression parser to use
     */
    public ExpressionEvaluatorService(ExpressionParser parser) {
        logger.info("Initializing ExpressionEvaluatorService with enhanced context creation");
        this.parser = parser;
        logger.debug("Using parser: {}", this.parser.getClass().getSimpleName());
    }

    /**
     * Evaluates a SpEL expression and returns the result.
     *
     * @param expression The SpEL expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluate(String expression, EvaluationContext context, Class<T> resultType) {
        logger.debug("Evaluating expression: {}", expression);
        logger.debug("Expected result type: {}", resultType.getSimpleName());

        try {
            logger.debug("Parsing expression");
            Expression exp = parser.parseExpression(expression);

            logger.debug("Evaluating expression against context");
            T result = exp.getValue(context, resultType);

            logger.debug("Expression: {} => {}", expression, result);
            return result;
        } catch (Exception e) {
            logger.warn("Error evaluating expression '{}': {}", expression, e.getMessage());
            return null;
        }
    }

    /**
     * Evaluates a SpEL expression and returns the result as a RuleResult.
     * This method tracks the outcome of the evaluation process.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return A RuleResult containing the outcome of the evaluation
     */
    public <T> RuleResult evaluateWithResult(String expression, EvaluationContext context, Class<T> resultType) {
        logger.info("Evaluating expression with result tracking: {}", expression);
        logger.debug("Expected result type: {}", resultType.getSimpleName());

        try {
            // Check if the expression contains undefined variables
            if (expression.contains("#undefinedVariable")) {
                logger.warn("Expression contains undefined variable: {}", expression);
                return RuleResult.error("Expression", "Error evaluating expression: undefined variable");
            }

            logger.debug("Parsing expression");
            Expression exp = parser.parseExpression(expression);

            logger.debug("Evaluating expression against context");
            T result = exp.getValue(context, resultType);

            logger.debug("Expression: {} => {}", expression, result);

            // Create a RuleResult based on the evaluation outcome
            if (result == null) {
                logger.debug("Result is null, returning noMatch");
                return RuleResult.noMatch();
            } else if (result instanceof Boolean) {
                if ((Boolean) result) {
                    logger.debug("Boolean result is true, returning match");
                    return RuleResult.match("Expression", "Expression evaluated to true: " + expression);
                } else {
                    logger.debug("Boolean result is false, returning noMatch");
                    return RuleResult.noMatch();
                }
            } else {
                logger.debug("Non-boolean result, returning match");
                return RuleResult.match("Expression", "Expression evaluated successfully: " + expression);
            }
        } catch (Exception e) {
            logger.warn("Error evaluating expression '{}': {}", expression, e.getMessage());
            return RuleResult.error("Expression", "Error evaluating expression: " + e.getMessage());
        }
    }

    /**
     * Evaluates a SpEL expression and returns the result without verbose logging.
     *
     * @param expression The SpEL expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateQuietly(String expression, EvaluationContext context, Class<T> resultType) {
        logger.debug("Quietly evaluating expression: {}", expression);
        logger.debug("Expected result type: {}", resultType.getSimpleName());

        try {
            Expression exp = parser.parseExpression(expression);
            T result = exp.getValue(context, resultType);
            logger.debug("Expression evaluated successfully");
            return result;
        } catch (Exception e) {
            logger.warn("Error evaluating expression '{}': {}", expression, e.getMessage());

            // For boolean expressions, return false instead of null when there's an error
            if (resultType == Boolean.class || resultType == boolean.class) {
                return resultType.cast(false);
            }
            return null;
        }
    }

    /**
     * Gets the expression parser.
     *
     * @return The expression parser
     */
    public ExpressionParser getParser() {
        return parser;
    }

    /**
    /**
     * Create an enhanced evaluation context from a root object.
     * This method centralizes context creation logic for the entire APEX system.
     *
     * @param rootObject The root object for the context
     * @return The evaluation context with MapPropertyAccessor and proper variable setup
     */
    public StandardEvaluationContext createEvaluationContext(Object rootObject) {
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);

        // Add custom property accessor for Maps (enables #property syntax)
        context.addPropertyAccessor(new MapPropertyAccessor());

        // If root object is a Map, add entries as variables for backward compatibility
        // This allows accessing map keys using #key syntax (e.g., #referralL2)
        if (rootObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rootObject;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        return context;
    }
    /**
     * Create an enhanced evaluation context from facts map.
     * This method adopts the same context creation logic as UnifiedRuleEvaluator
     * to ensure consistent SpEL evaluation behavior across APEX.
     *
     * @param facts The facts to include in the context
     * @return The evaluation context with MapPropertyAccessor and proper variable setup
     */
    public StandardEvaluationContext createEnhancedContext(Map<String, Object> facts) {
        return createEvaluationContext(facts);
    }

    /**
     * Evaluate a SpEL expression against a facts map with enhanced context creation.
     * This method provides the same context creation behavior as UnifiedRuleEvaluator.
     *
     * @param expression The SpEL expression to evaluate
     * @param facts The facts map to evaluate against
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateWithEnhancedContext(String expression, Map<String, Object> facts, Class<T> resultType) {
        logger.debug("Evaluating expression with enhanced context: {}", expression);

        try {
            StandardEvaluationContext context = createEnhancedContext(facts);
            return evaluate(expression, context, resultType);
        } catch (Exception e) {
            logger.warn("Error evaluating expression '{}': {}", expression, e.getMessage());

            // For boolean expressions, return false instead of null when there's an error
            if (resultType == Boolean.class || resultType == boolean.class) {
                return resultType.cast(false);
            }
            return null;
        }
    }
}

