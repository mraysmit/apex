package dev.mars.apex.core.service.expression;

import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for evaluating SpEL expressions with enhanced functionality.
 * This service provides a higher-level API for expression evaluation
 * with support for Map-based contexts and validation.
 */
@Service
public class ExpressionEvaluationService {
    
    private static final Logger LOGGER = Logger.getLogger(ExpressionEvaluationService.class.getName());
    
    private final ExpressionEvaluatorService expressionEvaluatorService;
    private final ExpressionParser parser;
    
    /**
     * Create a new ExpressionEvaluationService.
     */
    public ExpressionEvaluationService() {
        this.expressionEvaluatorService = new ExpressionEvaluatorService();
        this.parser = new SpelExpressionParser();
    }
    
    /**
     * Evaluate a SpEL expression with a Map-based context.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The context variables as a Map
     * @return The result of the evaluation
     */
    public Object evaluate(String expression, Map<String, Object> context) {
        LOGGER.info("Evaluating expression: " + expression);
        LOGGER.fine("Context variables: " + (context != null ? context.keySet() : "none"));
        
        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            return expressionEvaluatorService.evaluate(expression, evalContext, Object.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating expression '" + expression + "': " + e.getMessage(), e);
            throw new RuntimeException("Failed to evaluate expression: " + expression, e);
        }
    }
    
    /**
     * Evaluate a SpEL expression with detailed result information.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The context variables as a Map
     * @return A RuleResult containing detailed evaluation information
     */
    public RuleResult evaluateWithResult(String expression, Map<String, Object> context) {
        LOGGER.info("Evaluating expression with result tracking: " + expression);
        
        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            return expressionEvaluatorService.evaluateWithResult(expression, evalContext, Object.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating expression with result '" + expression + "': " + e.getMessage(), e);
            return RuleResult.error("Expression", "Error evaluating expression: " + e.getMessage());
        }
    }
    
    /**
     * Validate the syntax of a SpEL expression.
     * 
     * @param expression The SpEL expression to validate
     * @return true if the expression syntax is valid, false otherwise
     */
    public boolean validateSyntax(String expression) {
        LOGGER.fine("Validating expression syntax: " + expression);
        
        try {
            parser.parseExpression(expression);
            LOGGER.fine("Expression syntax is valid");
            return true;
        } catch (Exception e) {
            LOGGER.fine("Expression syntax is invalid: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluate a SpEL expression as a boolean.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The context variables as a Map
     * @return The boolean result of the evaluation
     */
    public boolean evaluateAsBoolean(String expression, Map<String, Object> context) {
        LOGGER.fine("Evaluating expression as boolean: " + expression);
        
        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            Boolean result = expressionEvaluatorService.evaluate(expression, evalContext, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating boolean expression '" + expression + "': " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Evaluate a SpEL expression as a string.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The context variables as a Map
     * @return The string result of the evaluation
     */
    public String evaluateAsString(String expression, Map<String, Object> context) {
        LOGGER.fine("Evaluating expression as string: " + expression);
        
        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            Object result = expressionEvaluatorService.evaluate(expression, evalContext, Object.class);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating string expression '" + expression + "': " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Evaluate a SpEL expression as a number.
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The context variables as a Map
     * @return The numeric result of the evaluation
     */
    public Number evaluateAsNumber(String expression, Map<String, Object> context) {
        LOGGER.fine("Evaluating expression as number: " + expression);
        
        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            return expressionEvaluatorService.evaluate(expression, evalContext, Number.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating numeric expression '" + expression + "': " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Evaluate a SpEL expression with a specific result type.
     * 
     * @param <T> The expected result type
     * @param expression The SpEL expression to evaluate
     * @param context The context variables as a Map
     * @param resultType The expected result type class
     * @return The typed result of the evaluation
     */
    public <T> T evaluate(String expression, Map<String, Object> context, Class<T> resultType) {
        LOGGER.fine("Evaluating expression with type " + resultType.getSimpleName() + ": " + expression);
        
        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            return expressionEvaluatorService.evaluate(expression, evalContext, resultType);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating typed expression '" + expression + "': " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Evaluate a SpEL expression quietly (with minimal logging).
     * 
     * @param expression The SpEL expression to evaluate
     * @param context The context variables as a Map
     * @return The result of the evaluation
     */
    public Object evaluateQuietly(String expression, Map<String, Object> context) {
        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            return expressionEvaluatorService.evaluateQuietly(expression, evalContext, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if an expression contains any variables that are not defined in the context.
     * 
     * @param expression The SpEL expression to check
     * @param context The context variables as a Map
     * @return true if all variables are defined, false otherwise
     */
    public boolean areAllVariablesDefined(String expression, Map<String, Object> context) {
        LOGGER.fine("Checking variable definitions for expression: " + expression);
        
        try {
            // Try to evaluate the expression - if it fails due to undefined variables, return false
            EvaluationContext evalContext = createEvaluationContext(context);
            expressionEvaluatorService.evaluateQuietly(expression, evalContext, Object.class);
            return true;
        } catch (Exception e) {
            // Check if the error is related to undefined variables
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("undefined") || errorMessage.contains("not found"))) {
                LOGGER.fine("Expression contains undefined variables: " + errorMessage);
                return false;
            }
            // Other errors don't necessarily mean undefined variables
            return true;
        }
    }
    
    /**
     * Get the underlying expression parser.
     * 
     * @return The SpEL expression parser
     */
    public ExpressionParser getParser() {
        return parser;
    }
    
    /**
     * Create an evaluation context from a Map of variables.
     * 
     * @param contextVariables The context variables as a Map
     * @return A StandardEvaluationContext with the variables set
     */
    private EvaluationContext createEvaluationContext(Map<String, Object> contextVariables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        if (contextVariables != null) {
            for (Map.Entry<String, Object> entry : contextVariables.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }
        
        return context;
    }
    
    /**
     * Parse an expression without evaluating it.
     * This can be used for syntax validation or expression analysis.
     * 
     * @param expression The SpEL expression to parse
     * @return The parsed Expression object
     * @throws org.springframework.expression.ParseException if the expression syntax is invalid
     */
    public Expression parseExpression(String expression) {
        return parser.parseExpression(expression);
    }
}
