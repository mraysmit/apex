package dev.mars.apex.core.service.data.external;

/**
 * Specialized exception for external data source operations.
 * 
 * This exception is thrown when data source operations fail, providing
 * detailed information about the failure including error codes and
 * recovery suggestions.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceException extends Exception {
    
    /**
     * Enumeration of data source error types.
     */
    public enum ErrorType {
        /**
         * Connection-related errors (network, authentication, etc.).
         */
        CONNECTION_ERROR("CONNECTION_ERROR", "Connection failed"),
        
        /**
         * Configuration-related errors (invalid settings, missing parameters, etc.).
         */
        CONFIGURATION_ERROR("CONFIGURATION_ERROR", "Configuration error"),
        
        /**
         * Query or operation execution errors.
         */
        EXECUTION_ERROR("EXECUTION_ERROR", "Execution failed"),
        
        /**
         * Data format or parsing errors.
         */
        DATA_FORMAT_ERROR("DATA_FORMAT_ERROR", "Data format error"),
        
        /**
         * Timeout errors.
         */
        TIMEOUT_ERROR("TIMEOUT_ERROR", "Operation timed out"),
        
        /**
         * Authentication or authorization errors.
         */
        AUTHENTICATION_ERROR("AUTHENTICATION_ERROR", "Authentication failed"),
        
        /**
         * Resource not found errors.
         */
        NOT_FOUND_ERROR("NOT_FOUND_ERROR", "Resource not found"),
        
        /**
         * Circuit breaker or rate limiting errors.
         */
        CIRCUIT_BREAKER_ERROR("CIRCUIT_BREAKER_ERROR", "Circuit breaker activated"),
        
        /**
         * General or unknown errors.
         */
        GENERAL_ERROR("GENERAL_ERROR", "General error");
        
        private final String code;
        private final String description;
        
        ErrorType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ErrorType errorType;
    private final String dataSourceName;
    private final String operation;
    private final boolean retryable;
    
    /**
     * Create a DataSourceException with an error type and message.
     * 
     * @param errorType The type of error
     * @param message The error message
     */
    public DataSourceException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.dataSourceName = null;
        this.operation = null;
        this.retryable = false;
    }
    
    /**
     * Create a DataSourceException with an error type, message, and cause.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     */
    public DataSourceException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.dataSourceName = null;
        this.operation = null;
        this.retryable = false;
    }
    
    /**
     * Create a DataSourceException with full details.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     * @param dataSourceName The name of the data source
     * @param operation The operation that failed
     * @param retryable Whether the operation can be retried
     */
    public DataSourceException(ErrorType errorType, String message, Throwable cause,
                             String dataSourceName, String operation, boolean retryable) {
        super(message, cause);
        this.errorType = errorType;
        this.dataSourceName = dataSourceName;
        this.operation = operation;
        this.retryable = retryable;
    }
    
    /**
     * Get the error type.
     * 
     * @return The error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Get the data source name.
     * 
     * @return The data source name, or null if not specified
     */
    public String getDataSourceName() {
        return dataSourceName;
    }
    
    /**
     * Get the operation that failed.
     * 
     * @return The operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Check if the operation can be retried.
     * 
     * @return true if the operation is retryable
     */
    public boolean isRetryable() {
        return retryable;
    }
    
    /**
     * Get the error code.
     * 
     * @return The error code
     */
    public String getErrorCode() {
        return errorType.getCode();
    }
    
    /**
     * Get a detailed error message including all available information.
     * 
     * @return Detailed error message
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorType.getCode()).append("] ");
        sb.append(getMessage());
        
        if (dataSourceName != null) {
            sb.append(" (Data Source: ").append(dataSourceName).append(")");
        }
        
        if (operation != null) {
            sb.append(" (Operation: ").append(operation).append(")");
        }
        
        if (retryable) {
            sb.append(" [Retryable]");
        }
        
        return sb.toString();
    }
    
    // Static factory methods for common error types
    
    /**
     * Create a connection error.
     * 
     * @param message Error message
     * @param cause Underlying cause
     * @return DataSourceException with CONNECTION_ERROR type
     */
    public static DataSourceException connectionError(String message, Throwable cause) {
        return new DataSourceException(ErrorType.CONNECTION_ERROR, message, cause, null, null, true);
    }
    
    /**
     * Create a configuration error.
     * 
     * @param message Error message
     * @return DataSourceException with CONFIGURATION_ERROR type
     */
    public static DataSourceException configurationError(String message) {
        return new DataSourceException(ErrorType.CONFIGURATION_ERROR, message, null, null, null, false);
    }
    
    /**
     * Create an execution error.
     * 
     * @param message Error message
     * @param cause Underlying cause
     * @param operation The operation that failed
     * @return DataSourceException with EXECUTION_ERROR type
     */
    public static DataSourceException executionError(String message, Throwable cause, String operation) {
        return new DataSourceException(ErrorType.EXECUTION_ERROR, message, cause, null, operation, true);
    }
    
    /**
     * Create a timeout error.
     * 
     * @param message Error message
     * @param operation The operation that timed out
     * @return DataSourceException with TIMEOUT_ERROR type
     */
    public static DataSourceException timeoutError(String message, String operation) {
        return new DataSourceException(ErrorType.TIMEOUT_ERROR, message, null, null, operation, true);
    }
    
    /**
     * Create an authentication error.
     * 
     * @param message Error message
     * @param dataSourceName The data source name
     * @return DataSourceException with AUTHENTICATION_ERROR type
     */
    public static DataSourceException authenticationError(String message, String dataSourceName) {
        return new DataSourceException(ErrorType.AUTHENTICATION_ERROR, message, null, dataSourceName, null, false);
    }
    
    /**
     * Create a not found error.
     * 
     * @param message Error message
     * @param operation The operation that failed
     * @return DataSourceException with NOT_FOUND_ERROR type
     */
    public static DataSourceException notFoundError(String message, String operation) {
        return new DataSourceException(ErrorType.NOT_FOUND_ERROR, message, null, null, operation, false);
    }
    
    /**
     * Create a circuit breaker error.
     * 
     * @param message Error message
     * @param dataSourceName The data source name
     * @return DataSourceException with CIRCUIT_BREAKER_ERROR type
     */
    public static DataSourceException circuitBreakerError(String message, String dataSourceName) {
        return new DataSourceException(ErrorType.CIRCUIT_BREAKER_ERROR, message, null, dataSourceName, null, true);
    }
    
    @Override
    public String toString() {
        return "DataSourceException{" +
               "errorType=" + errorType +
               ", dataSourceName='" + dataSourceName + '\'' +
               ", operation='" + operation + '\'' +
               ", retryable=" + retryable +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
