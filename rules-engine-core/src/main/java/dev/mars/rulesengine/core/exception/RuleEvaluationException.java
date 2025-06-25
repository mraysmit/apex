package dev.mars.rulesengine.core.exception;

/**
 * Exception thrown when a rule evaluation fails.
 * Provides specific context about the rule and evaluation failure.
 */
public class RuleEvaluationException extends RuleEngineException {
    private static final long serialVersionUID = 1L;
    
    private final String ruleName;
    private final String expression;
    private final String suggestion;
    
    public RuleEvaluationException(String ruleName, String expression, String message) {
        super("RULE_EVALUATION_ERROR", message, "Rule: " + ruleName + ", Expression: " + expression);
        this.ruleName = ruleName;
        this.expression = expression;
        this.suggestion = generateSuggestion(message);
    }
    
    public RuleEvaluationException(String ruleName, String expression, String message, Throwable cause) {
        super("RULE_EVALUATION_ERROR", message, "Rule: " + ruleName + ", Expression: " + expression, cause);
        this.ruleName = ruleName;
        this.expression = expression;
        this.suggestion = generateSuggestion(message);
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public String getExpression() {
        return expression;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    /**
     * Generate helpful suggestions based on the error message.
     */
    private String generateSuggestion(String errorMessage) {
        if (errorMessage == null) {
            return "Check rule syntax and available variables";
        }
        
        String lowerMessage = errorMessage.toLowerCase();
        
        if (lowerMessage.contains("property or field") && lowerMessage.contains("cannot be found")) {
            return "Check if the property exists in your facts map or object. Available properties can be verified using the rule debugger.";
        }
        
        if (lowerMessage.contains("method") && lowerMessage.contains("cannot be found")) {
            return "Verify the method name and parameters. Ensure the method is public and accessible.";
        }
        
        if (lowerMessage.contains("cannot be cast") || lowerMessage.contains("type mismatch")) {
            return "Check data types in your expression. Consider using type conversion methods or verify input data types.";
        }
        
        if (lowerMessage.contains("null")) {
            return "Add null checks in your expression using the safe navigation operator (?.) or conditional logic.";
        }
        
        if (lowerMessage.contains("syntax") || lowerMessage.contains("parse")) {
            return "Review SpEL syntax. Common issues: missing quotes for strings, incorrect operator usage, unbalanced parentheses.";
        }
        
        return "Review the rule expression syntax and ensure all referenced variables are available in the evaluation context.";
    }
    
    @Override
    public String getDetailedMessage() {
        return super.getDetailedMessage() + "\nSuggestion: " + suggestion;
    }
}
