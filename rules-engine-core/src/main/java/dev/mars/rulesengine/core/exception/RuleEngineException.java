package dev.mars.rulesengine.core.exception;

/**
 * Base exception class for all rules engine related exceptions.
 * Provides a hierarchy for different types of rule engine errors.
 */
public class RuleEngineException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final String errorCode;
    private final String context;
    
    public RuleEngineException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
        this.context = null;
    }
    
    public RuleEngineException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_ERROR";
        this.context = null;
    }
    
    public RuleEngineException(String errorCode, String message, String context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    public RuleEngineException(String errorCode, String message, String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getContext() {
        return context;
    }
    
    /**
     * Get a detailed error message including error code and context.
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode).append("] ").append(getMessage());
        if (context != null && !context.trim().isEmpty()) {
            sb.append(" (Context: ").append(context).append(")");
        }
        return sb.toString();
    }
}
