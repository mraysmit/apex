package com.rulesengine.core.service.engine;

import com.rulesengine.core.engine.model.RuleResult;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for evaluating SpEL expressions.
 * This class centralizes expression parsing and evaluation.
 */
public class ExpressionEvaluatorService {
    private static final Logger LOGGER = Logger.getLogger(ExpressionEvaluatorService.class.getName());
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
        LOGGER.info("Initializing ExpressionEvaluatorService");
        this.parser = parser;
        LOGGER.fine("Using parser: " + this.parser.getClass().getSimpleName());
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
        LOGGER.info("Evaluating expression: " + expression);
        LOGGER.fine("Expected result type: " + resultType.getSimpleName());

        try {
            LOGGER.fine("Parsing expression");
            Expression exp = parser.parseExpression(expression);

            LOGGER.fine("Evaluating expression against context");
            T result = exp.getValue(context, resultType);

            LOGGER.info("Expression: " + expression + " => " + result);
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating expression '" + expression + "': " + e.getMessage(), e);
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
        LOGGER.info("Evaluating expression with result tracking: " + expression);
        LOGGER.fine("Expected result type: " + resultType.getSimpleName());

        try {
            LOGGER.fine("Parsing expression");
            Expression exp = parser.parseExpression(expression);

            LOGGER.fine("Evaluating expression against context");
            T result = exp.getValue(context, resultType);

            LOGGER.info("Expression: " + expression + " => " + result);

            // Create a RuleResult based on the evaluation outcome
            if (result == null) {
                LOGGER.fine("Result is null, returning noMatch");
                return RuleResult.noMatch();
            } else if (result instanceof Boolean) {
                if ((Boolean) result) {
                    LOGGER.fine("Boolean result is true, returning match");
                    return RuleResult.match("Expression", "Expression evaluated to true: " + expression);
                } else {
                    LOGGER.fine("Boolean result is false, returning noMatch");
                    return RuleResult.noMatch();
                }
            } else {
                LOGGER.fine("Non-boolean result, returning match");
                return RuleResult.match("Expression", "Expression evaluated successfully: " + expression);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating expression '" + expression + "': " + e.getMessage(), e);
            return RuleResult.error("Expression", "Error evaluating expression: " + e.getMessage());
        }
    }

    /**
     * Evaluates a SpEL expression and returns the result without printing.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The evaluation context
     * @param resultType The expected result type
     * @param <T> The type of the result
     * @return The result of the evaluation
     */
    public <T> T evaluateQuietly(String expression, EvaluationContext context, Class<T> resultType) {
        LOGGER.fine("Quietly evaluating expression: " + expression);
        LOGGER.finest("Expected result type: " + resultType.getSimpleName());

        try {
            Expression exp = parser.parseExpression(expression);
            T result = exp.getValue(context, resultType);
            LOGGER.finest("Expression evaluated successfully");
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating expression '" + expression + "': " + e.getMessage(), e);
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
}