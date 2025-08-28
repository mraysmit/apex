package dev.mars.apex.core.service.data.external;

/**
 * Exception thrown when external data-source configuration resolution fails.
 * 
 * This exception is thrown by the DataSourceResolver when it cannot resolve
 * an external data-source reference due to various reasons such as:
 * - Configuration file not found
 * - Invalid YAML format
 * - Missing required configuration fields
 * - Network or I/O errors
 * 
 * @author APEX Core Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class DataSourceResolutionException extends RuntimeException {
    
    /**
     * Constructs a new DataSourceResolutionException with the specified detail message.
     * 
     * @param message the detail message explaining the resolution failure
     */
    public DataSourceResolutionException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new DataSourceResolutionException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the resolution failure
     * @param cause the cause of the resolution failure
     */
    public DataSourceResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new DataSourceResolutionException with the specified cause.
     * 
     * @param cause the cause of the resolution failure
     */
    public DataSourceResolutionException(Throwable cause) {
        super(cause);
    }
}
