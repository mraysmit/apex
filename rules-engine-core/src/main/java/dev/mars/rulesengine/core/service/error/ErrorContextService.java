package dev.mars.rulesengine.core.service.error;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.*;
import java.util.logging.Logger;

/**
 * Service for providing detailed error context and diagnostics for rule evaluation failures.
 * Helps developers understand what went wrong and how to fix it.
 */
public class ErrorContextService {
    private static final Logger LOGGER = Logger.getLogger(ErrorContextService.class.getName());
    
    /**
     * Generate detailed error context for a rule evaluation failure.
     * 
     * @param ruleName The name of the rule that failed
     * @param expression The expression that caused the error
     * @param context The evaluation context
     * @param exception The original exception
     * @return Detailed error context information
     */
    public ErrorContext generateErrorContext(String ruleName, String expression, 
                                           EvaluationContext context, Exception exception) {
        LOGGER.fine("Generating error context for rule: " + ruleName);
        
        ErrorContext.Builder builder = new ErrorContext.Builder(ruleName, expression, exception);
        
        // Add available variables
        if (context instanceof StandardEvaluationContext) {
            StandardEvaluationContext stdContext = (StandardEvaluationContext) context;
            builder.withAvailableVariables(extractAvailableVariables(stdContext));
        }
        
        // Analyze the expression for common issues
        builder.withExpressionAnalysis(analyzeExpression(expression));
        
        // Generate suggestions based on the error
        builder.withSuggestions(generateSuggestions(expression, exception));
        
        // Add error classification
        builder.withErrorType(classifyError(exception));
        
        return builder.build();
    }
    
    /**
     * Extract available variables from the evaluation context.
     */
    private Map<String, String> extractAvailableVariables(StandardEvaluationContext context) {
        Map<String, String> variables = new HashMap<>();
        
        try {
            // Get root object information
            Object rootObject = context.getRootObject();
            if (rootObject != null) {
                variables.put("rootObject", rootObject.getClass().getSimpleName());
                
                // If it's a Map, show the keys
                if (rootObject instanceof Map) {
                    Map<?, ?> rootMap = (Map<?, ?>) rootObject;
                    variables.put("availableKeys", String.join(", ", 
                        rootMap.keySet().stream()
                            .map(Object::toString)
                            .toArray(String[]::new)));
                }
            }
            
            // Get variable information (this is limited by SpEL's API)
            // We can only get what we can access through reflection
            
        } catch (Exception e) {
            LOGGER.fine("Could not extract all variable information: " + e.getMessage());
        }
        
        return variables;
    }
    
    /**
     * Analyze the expression for common syntax issues.
     */
    private ExpressionAnalysis analyzeExpression(String expression) {
        ExpressionAnalysis.Builder builder = new ExpressionAnalysis.Builder(expression);
        
        // Check for common syntax issues
        if (expression.contains("..")) {
            builder.addIssue("Double dots (..) detected - possible typo in property access");
        }
        
        if (expression.contains("?.?.")) {
            builder.addIssue("Multiple safe navigation operators - consider simplifying");
        }
        
        // Check for unbalanced parentheses
        long openParens = expression.chars().filter(ch -> ch == '(').count();
        long closeParens = expression.chars().filter(ch -> ch == ')').count();
        if (openParens != closeParens) {
            builder.addIssue("Unbalanced parentheses: " + openParens + " open, " + closeParens + " close");
        }
        
        // Check for unbalanced brackets
        long openBrackets = expression.chars().filter(ch -> ch == '[').count();
        long closeBrackets = expression.chars().filter(ch -> ch == ']').count();
        if (openBrackets != closeBrackets) {
            builder.addIssue("Unbalanced brackets: " + openBrackets + " open, " + closeBrackets + " close");
        }
        
        // Check for string literal issues
        long singleQuotes = expression.chars().filter(ch -> ch == '\'').count();
        if (singleQuotes % 2 != 0) {
            builder.addIssue("Unmatched single quotes - string literals may be incomplete");
        }
        
        long doubleQuotes = expression.chars().filter(ch -> ch == '"').count();
        if (doubleQuotes % 2 != 0) {
            builder.addIssue("Unmatched double quotes - string literals may be incomplete");
        }
        
        return builder.build();
    }
    
    /**
     * Generate helpful suggestions based on the error and expression.
     */
    private List<String> generateSuggestions(String expression, Exception exception) {
        List<String> suggestions = new ArrayList<>();
        String errorMessage = exception.getMessage();
        
        if (errorMessage == null) {
            suggestions.add("Check the rule expression syntax and ensure all variables are properly defined");
            return suggestions;
        }
        
        String lowerError = errorMessage.toLowerCase();
        
        // Property/field not found
        if (lowerError.contains("property or field") && lowerError.contains("cannot be found")) {
            suggestions.add("Verify that the property exists in your data object");
            suggestions.add("Check for typos in property names");
            suggestions.add("Ensure the property is public or has a getter method");
            suggestions.add("Use the safe navigation operator (?.) if the property might not exist");
        }
        
        // Method not found
        if (lowerError.contains("method") && lowerError.contains("cannot be found")) {
            suggestions.add("Check the method name and parameter types");
            suggestions.add("Ensure the method is public");
            suggestions.add("Verify that the correct number of parameters are provided");
        }
        
        // Type conversion issues
        if (lowerError.contains("cannot be cast") || lowerError.contains("type")) {
            suggestions.add("Check data types in your expression");
            suggestions.add("Use explicit type conversion if needed (e.g., T(Integer).valueOf())");
            suggestions.add("Verify that input data matches expected types");
        }
        
        // Null pointer issues
        if (lowerError.contains("null")) {
            suggestions.add("Add null checks using the safe navigation operator (?.)");
            suggestions.add("Use conditional expressions to handle null values");
            suggestions.add("Ensure all required data is provided in the facts map");
        }
        
        // Syntax errors
        if (lowerError.contains("syntax") || lowerError.contains("parse")) {
            suggestions.add("Review SpEL expression syntax documentation");
            suggestions.add("Check for missing quotes around string literals");
            suggestions.add("Verify operator usage and precedence");
            suggestions.add("Ensure parentheses and brackets are balanced");
        }
        
        // Collection access issues
        if (expression.contains("[") && expression.contains("]")) {
            suggestions.add("Verify collection indices are within bounds");
            suggestions.add("Check that the object is actually a collection or array");
            suggestions.add("Use safe collection access patterns");
        }
        
        // If no specific suggestions, provide general ones
        if (suggestions.isEmpty()) {
            suggestions.add("Review the rule expression for syntax errors");
            suggestions.add("Ensure all referenced variables are available in the evaluation context");
            suggestions.add("Check the SpEL documentation for correct syntax");
        }
        
        return suggestions;
    }
    
    /**
     * Classify the type of error for better handling.
     */
    private ErrorType classifyError(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return ErrorType.UNKNOWN;
        }
        
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("property") || lowerMessage.contains("field")) {
            return ErrorType.PROPERTY_ACCESS;
        }
        
        if (lowerMessage.contains("method")) {
            return ErrorType.METHOD_INVOCATION;
        }
        
        if (lowerMessage.contains("cast") || lowerMessage.contains("type")) {
            return ErrorType.TYPE_CONVERSION;
        }
        
        if (lowerMessage.contains("null")) {
            return ErrorType.NULL_POINTER;
        }
        
        if (lowerMessage.contains("syntax") || lowerMessage.contains("parse")) {
            return ErrorType.SYNTAX_ERROR;
        }
        
        if (lowerMessage.contains("index") || lowerMessage.contains("bound")) {
            return ErrorType.INDEX_OUT_OF_BOUNDS;
        }
        
        return ErrorType.RUNTIME_ERROR;
    }
    
    /**
     * Types of errors that can occur during rule evaluation.
     */
    public enum ErrorType {
        PROPERTY_ACCESS,
        METHOD_INVOCATION,
        TYPE_CONVERSION,
        NULL_POINTER,
        SYNTAX_ERROR,
        INDEX_OUT_OF_BOUNDS,
        RUNTIME_ERROR,
        UNKNOWN
    }

    /**
     * Represents detailed error context information.
     */
    public static class ErrorContext {
        private final String ruleName;
        private final String expression;
        private final Exception originalException;
        private final Map<String, String> availableVariables;
        private final ExpressionAnalysis expressionAnalysis;
        private final List<String> suggestions;
        private final ErrorType errorType;

        private ErrorContext(Builder builder) {
            this.ruleName = builder.ruleName;
            this.expression = builder.expression;
            this.originalException = builder.originalException;
            this.availableVariables = new HashMap<>(builder.availableVariables);
            this.expressionAnalysis = builder.expressionAnalysis;
            this.suggestions = new ArrayList<>(builder.suggestions);
            this.errorType = builder.errorType;
        }

        public String getRuleName() { return ruleName; }
        public String getExpression() { return expression; }
        public Exception getOriginalException() { return originalException; }
        public Map<String, String> getAvailableVariables() { return new HashMap<>(availableVariables); }
        public ExpressionAnalysis getExpressionAnalysis() { return expressionAnalysis; }
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
        public ErrorType getErrorType() { return errorType; }

        public String getFormattedErrorReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Rule Evaluation Error Report ===\n");
            sb.append("Rule: ").append(ruleName).append("\n");
            sb.append("Expression: ").append(expression).append("\n");
            sb.append("Error Type: ").append(errorType).append("\n");
            sb.append("Original Error: ").append(originalException.getMessage()).append("\n\n");

            if (!availableVariables.isEmpty()) {
                sb.append("Available Variables:\n");
                availableVariables.forEach((key, value) ->
                    sb.append("  ").append(key).append(": ").append(value).append("\n"));
                sb.append("\n");
            }

            if (expressionAnalysis.hasIssues()) {
                sb.append("Expression Analysis Issues:\n");
                expressionAnalysis.getIssues().forEach(issue ->
                    sb.append("  - ").append(issue).append("\n"));
                sb.append("\n");
            }

            if (!suggestions.isEmpty()) {
                sb.append("Suggestions:\n");
                suggestions.forEach(suggestion ->
                    sb.append("  - ").append(suggestion).append("\n"));
            }

            return sb.toString();
        }

        public static class Builder {
            private final String ruleName;
            private final String expression;
            private final Exception originalException;
            private Map<String, String> availableVariables = new HashMap<>();
            private ExpressionAnalysis expressionAnalysis;
            private List<String> suggestions = new ArrayList<>();
            private ErrorType errorType = ErrorType.UNKNOWN;

            public Builder(String ruleName, String expression, Exception originalException) {
                this.ruleName = ruleName;
                this.expression = expression;
                this.originalException = originalException;
            }

            public Builder withAvailableVariables(Map<String, String> variables) {
                this.availableVariables = variables;
                return this;
            }

            public Builder withExpressionAnalysis(ExpressionAnalysis analysis) {
                this.expressionAnalysis = analysis;
                return this;
            }

            public Builder withSuggestions(List<String> suggestions) {
                this.suggestions = suggestions;
                return this;
            }

            public Builder withErrorType(ErrorType errorType) {
                this.errorType = errorType;
                return this;
            }

            public ErrorContext build() {
                return new ErrorContext(this);
            }
        }
    }

    /**
     * Represents analysis of an expression for potential issues.
     */
    public static class ExpressionAnalysis {
        private final String expression;
        private final List<String> issues;

        private ExpressionAnalysis(Builder builder) {
            this.expression = builder.expression;
            this.issues = new ArrayList<>(builder.issues);
        }

        public String getExpression() { return expression; }
        public List<String> getIssues() { return new ArrayList<>(issues); }
        public boolean hasIssues() { return !issues.isEmpty(); }

        public static class Builder {
            private final String expression;
            private final List<String> issues = new ArrayList<>();

            public Builder(String expression) {
                this.expression = expression;
            }

            public Builder addIssue(String issue) {
                this.issues.add(issue);
                return this;
            }

            public ExpressionAnalysis build() {
                return new ExpressionAnalysis(this);
            }
        }
    }
}
