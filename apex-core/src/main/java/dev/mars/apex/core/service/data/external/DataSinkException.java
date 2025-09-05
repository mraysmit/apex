package dev.mars.apex.core.service.data.external;

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
 * Exception thrown when data sink operations fail.
 * 
 * This exception provides detailed error information including error types,
 * context information, and support for error recovery scenarios.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSinkException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Enumeration of data sink error types.
     */
    public enum ErrorType {
        /**
         * Configuration-related errors (invalid settings, missing required fields, etc.).
         */
        CONFIGURATION_ERROR("Configuration Error", "Invalid or missing configuration"),
        
        /**
         * Connection-related errors (network issues, authentication failures, etc.).
         */
        CONNECTION_ERROR("Connection Error", "Failed to connect to data sink"),
        
        /**
         * Write operation errors (data validation failures, constraint violations, etc.).
         */
        WRITE_ERROR("Write Error", "Failed to write data to sink"),

        /**
         * Data integrity violations (primary key conflicts, constraint violations, etc.).
         * These are typically data quality issues that should be handled gracefully.
         */
        DATA_INTEGRITY_ERROR("Data Integrity Error", "Data violates database constraints"),
        
        /**
         * Batch operation errors (partial failures, transaction rollbacks, etc.).
         */
        BATCH_ERROR("Batch Error", "Failed to process batch operation"),
        
        /**
         * Schema-related errors (table not found, column mismatch, etc.).
         */
        SCHEMA_ERROR("Schema Error", "Schema validation or creation failed"),
        
        /**
         * Authentication and authorization errors.
         */
        SECURITY_ERROR("Security Error", "Authentication or authorization failed"),
        
        /**
         * Resource-related errors (disk space, memory, connection pool exhaustion, etc.).
         */
        RESOURCE_ERROR("Resource Error", "Insufficient resources or resource exhaustion"),
        
        /**
         * Timeout errors (operation took too long to complete).
         */
        TIMEOUT_ERROR("Timeout Error", "Operation timed out"),
        
        /**
         * Data format or validation errors.
         */
        DATA_ERROR("Data Error", "Invalid data format or validation failure"),
        
        /**
         * Unknown or unexpected errors.
         */
        UNKNOWN_ERROR("Unknown Error", "An unexpected error occurred");
        
        private final String displayName;
        private final String description;
        
        ErrorType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ErrorType errorType;
    private final String context;
    private final boolean retryable;
    
    /**
     * Create a new DataSinkException with an error type and message.
     * 
     * @param errorType The type of error
     * @param message The error message
     */
    public DataSinkException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.context = null;
        this.retryable = isRetryableByDefault(errorType);
    }
    
    /**
     * Create a new DataSinkException with an error type, message, and cause.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     */
    public DataSinkException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.context = null;
        this.retryable = isRetryableByDefault(errorType);
    }
    
    /**
     * Create a new DataSinkException with full details.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     * @param context Additional context information
     * @param retryable Whether this error is retryable
     */
    public DataSinkException(ErrorType errorType, String message, Throwable cause, String context, boolean retryable) {
        super(message, cause);
        this.errorType = errorType;
        this.context = context;
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
     * Get the context information.
     * 
     * @return The context information, or null if not provided
     */
    public String getContext() {
        return context;
    }
    
    /**
     * Check if this error is retryable.
     * 
     * @return true if the operation can be retried, false otherwise
     */
    public boolean isRetryable() {
        return retryable;
    }
    
    /**
     * Determine if an error type is retryable by default.
     */
    private static boolean isRetryableByDefault(ErrorType errorType) {
        switch (errorType) {
            case CONNECTION_ERROR:
            case TIMEOUT_ERROR:
            case RESOURCE_ERROR:
                return true;
            case CONFIGURATION_ERROR:
            case WRITE_ERROR:
            case SCHEMA_ERROR:
            case SECURITY_ERROR:
            case DATA_ERROR:
            case DATA_INTEGRITY_ERROR:
                return false;
            case BATCH_ERROR:
            case UNKNOWN_ERROR:
                return false; // Conservative approach
            default:
                return false;
        }
    }
    
    // Static factory methods for common error scenarios
    
    /**
     * Create a configuration error.
     */
    public static DataSinkException configurationError(String message) {
        return new DataSinkException(ErrorType.CONFIGURATION_ERROR, message);
    }
    
    /**
     * Create a configuration error with cause.
     */
    public static DataSinkException configurationError(String message, Throwable cause) {
        return new DataSinkException(ErrorType.CONFIGURATION_ERROR, message, cause);
    }
    
    /**
     * Create a connection error.
     */
    public static DataSinkException connectionError(String message) {
        return new DataSinkException(ErrorType.CONNECTION_ERROR, message);
    }
    
    /**
     * Create a connection error with cause.
     */
    public static DataSinkException connectionError(String message, Throwable cause) {
        return new DataSinkException(ErrorType.CONNECTION_ERROR, message, cause);
    }
    
    /**
     * Create a write error.
     */
    public static DataSinkException writeError(String message) {
        return new DataSinkException(ErrorType.WRITE_ERROR, message);
    }
    
    /**
     * Create a write error with cause and context.
     */
    public static DataSinkException writeError(String message, Throwable cause, String context) {
        return new DataSinkException(ErrorType.WRITE_ERROR, message, cause, context, false);
    }
    
    /**
     * Create a batch error.
     */
    public static DataSinkException batchError(String message, int processedCount, int totalCount) {
        String context = String.format("Processed %d of %d records", processedCount, totalCount);
        return new DataSinkException(ErrorType.BATCH_ERROR, message, null, context, true);
    }
    
    /**
     * Create a schema error.
     */
    public static DataSinkException schemaError(String message) {
        return new DataSinkException(ErrorType.SCHEMA_ERROR, message);
    }
    
    /**
     * Create a schema error with cause.
     */
    public static DataSinkException schemaError(String message, Throwable cause) {
        return new DataSinkException(ErrorType.SCHEMA_ERROR, message, cause);
    }
    
    /**
     * Create a timeout error.
     */
    public static DataSinkException timeoutError(String message, long timeoutMs) {
        String context = String.format("Timeout: %d ms", timeoutMs);
        return new DataSinkException(ErrorType.TIMEOUT_ERROR, message, null, context, true);
    }

    /**
     * Create a data integrity error.
     */
    public static DataSinkException dataIntegrityError(String message) {
        return new DataSinkException(ErrorType.DATA_INTEGRITY_ERROR, message);
    }

    /**
     * Create a data integrity error with cause and context.
     */
    public static DataSinkException dataIntegrityError(String message, Throwable cause, String context) {
        return new DataSinkException(ErrorType.DATA_INTEGRITY_ERROR, message, cause, context, false);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataSinkException[")
          .append("type=").append(errorType.getDisplayName())
          .append(", message=").append(getMessage());
        
        if (context != null) {
            sb.append(", context=").append(context);
        }
        
        sb.append(", retryable=").append(retryable)
          .append("]");
        
        return sb.toString();
    }
}
