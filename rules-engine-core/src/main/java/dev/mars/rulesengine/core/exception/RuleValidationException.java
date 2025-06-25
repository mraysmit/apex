package dev.mars.rulesengine.core.exception;

import java.util.List;
import java.util.ArrayList;

/**
 * Exception thrown when rule validation fails.
 * Can contain multiple validation errors for comprehensive feedback.
 */
public class RuleValidationException extends RuleEngineException {
    private static final long serialVersionUID = 1L;
    
    private final String ruleName;
    private final List<ValidationError> validationErrors;
    
    public RuleValidationException(String ruleName, String message) {
        super("RULE_VALIDATION_ERROR", message, "Rule: " + ruleName);
        this.ruleName = ruleName;
        this.validationErrors = new ArrayList<>();
    }
    
    public RuleValidationException(String ruleName, List<ValidationError> validationErrors) {
        super("RULE_VALIDATION_ERROR", buildMessage(validationErrors), "Rule: " + ruleName);
        this.ruleName = ruleName;
        this.validationErrors = new ArrayList<>(validationErrors);
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public List<ValidationError> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    public void addValidationError(ValidationError error) {
        this.validationErrors.add(error);
    }
    
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
    
    private static String buildMessage(List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return "Rule validation failed";
        }
        
        StringBuilder sb = new StringBuilder("Rule validation failed with ");
        sb.append(errors.size()).append(" error(s):");
        for (ValidationError error : errors) {
            sb.append("\n- ").append(error.getMessage());
        }
        return sb.toString();
    }
    
    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(super.getDetailedMessage());
        if (!validationErrors.isEmpty()) {
            sb.append("\nValidation Errors:");
            for (ValidationError error : validationErrors) {
                sb.append("\n- ").append(error.getField()).append(": ").append(error.getMessage());
                if (error.getSuggestion() != null) {
                    sb.append(" (Suggestion: ").append(error.getSuggestion()).append(")");
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * Represents a single validation error with field context and suggestions.
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        private final String suggestion;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
            this.suggestion = null;
        }
        
        public ValidationError(String field, String message, String suggestion) {
            this.field = field;
            this.message = message;
            this.suggestion = suggestion;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
    }
}
