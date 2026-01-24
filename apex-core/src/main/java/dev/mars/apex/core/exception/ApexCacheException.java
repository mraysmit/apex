package dev.mars.apex.core.exception;

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
 * Runtime exception thrown when cache operations fail.
 * 
 * This exception provides detailed context about cache failures including:
 * - The cache name
 * - The operation that failed (get, put, remove, etc.)
 * - The key being accessed
 * - Whether the error is retryable
 * 
 * <p>This is an unchecked exception to allow transparent error propagation
 * through cache operations without requiring explicit exception handling
 * at every level.
 * 
 * <p>Usage example:
 * <pre>{@code
 * // For initialization errors
 * throw ApexCacheException.notInitialized("customer-cache");
 * 
 * // For lookup failures
 * throw ApexCacheException.lookupFailed("customer-cache", "CUST-123", cause);
 * }</pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-24
 * @version 1.0
 */
public class ApexCacheException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Enumeration of cache error types.
     */
    public enum ErrorType {
        /**
         * Cache is not initialized or configuration is missing.
         */
        NOT_INITIALIZED("APEX-CACHE-001", "Cache not initialized", false),
        
        /**
         * Cache lookup operation failed.
         */
        LOOKUP_FAILED("APEX-CACHE-002", "Cache lookup failed", true),
        
        /**
         * Cache write (put) operation failed.
         */
        WRITE_FAILED("APEX-CACHE-003", "Cache write failed", true),
        
        /**
         * Cache remove operation failed.
         */
        REMOVE_FAILED("APEX-CACHE-004", "Cache remove failed", true),
        
        /**
         * Cache clear operation failed.
         */
        CLEAR_FAILED("APEX-CACHE-005", "Cache clear failed", true),
        
        /**
         * Cache key is invalid (null or malformed).
         */
        INVALID_KEY("APEX-CACHE-006", "Invalid cache key", false),
        
        /**
         * Cache configuration error.
         */
        CONFIGURATION_ERROR("APEX-CACHE-007", "Cache configuration error", false),
        
        /**
         * Cache capacity exceeded.
         */
        CAPACITY_EXCEEDED("APEX-CACHE-008", "Cache capacity exceeded", true),
        
        /**
         * Cache data serialization/deserialization error.
         */
        SERIALIZATION_ERROR("APEX-CACHE-009", "Cache serialization error", false),
        
        /**
         * General cache error.
         */
        GENERAL_ERROR("APEX-CACHE-999", "Cache error", true);
        
        private final String code;
        private final String description;
        private final boolean retryable;
        
        ErrorType(String code, String description, boolean retryable) {
            this.code = code;
            this.description = description;
            this.retryable = retryable;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
    
    private final ErrorType errorType;
    private final String cacheName;
    private final String operation;
    private final String key;
    private final boolean retryable;
    private final String errorCode;
    private final String context;
    
    /**
     * Create an ApexCacheException with full details.
     * 
     * @param errorType The type of cache error
     * @param cacheName The name of the cache
     * @param operation The operation that failed (get, put, remove, etc.)
     * @param key The cache key (may be null)
     * @param message The error message
     */
    public ApexCacheException(ErrorType errorType, String cacheName, String operation, 
                               String key, String message) {
        super(message);
        this.errorType = errorType;
        this.cacheName = cacheName;
        this.operation = operation;
        this.key = key;
        this.retryable = errorType.isRetryable();
        this.errorCode = errorType.getCode();
        this.context = buildContext(cacheName, operation, key);
    }
    
    /**
     * Create an ApexCacheException with full details and a cause.
     * 
     * @param errorType The type of cache error
     * @param cacheName The name of the cache
     * @param operation The operation that failed (get, put, remove, etc.)
     * @param key The cache key (may be null)
     * @param message The error message
     * @param cause The underlying cause
     */
    public ApexCacheException(ErrorType errorType, String cacheName, String operation, 
                               String key, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.cacheName = cacheName;
        this.operation = operation;
        this.key = key;
        this.retryable = errorType.isRetryable();
        this.errorCode = errorType.getCode();
        this.context = buildContext(cacheName, operation, key);
    }
    
    private static String buildContext(String cacheName, String operation, String key) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cache: ").append(cacheName != null ? cacheName : "unknown");
        if (operation != null) {
            sb.append(", Operation: ").append(operation);
        }
        if (key != null) {
            sb.append(", Key: ").append(key);
        }
        return sb.toString();
    }
    
    // ========================================
    // Getters
    // ========================================
    
    /**
     * Get the error type.
     * 
     * @return The error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Get the cache name.
     * 
     * @return The cache name, or null if not specified
     */
    public String getCacheName() {
        return cacheName;
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
     * Get the cache key involved.
     * 
     * @return The cache key, or null if not applicable
     */
    public String getKey() {
        return key;
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
     * @return The error code (e.g., APEX-CACHE-001)
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get the context string.
     * 
     * @return The context describing where the error occurred
     */
    public String getContext() {
        return context;
    }
    
    /**
     * Get a detailed error message including error code and context.
     * 
     * @return Formatted message with error code and context
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode).append("] ").append(getMessage());
        if (context != null && !context.trim().isEmpty()) {
            sb.append(" (Context: ").append(context).append(")");
        }
        return sb.toString();
    }
    
    // ========================================
    // Static Factory Methods
    // ========================================
    
    /**
     * Create a "not initialized" error.
     * Use when the cache or cache manager is null.
     * 
     * @param cacheName The name of the cache
     * @return ApexCacheException with NOT_INITIALIZED type
     */
    public static ApexCacheException notInitialized(String cacheName) {
        return new ApexCacheException(
            ErrorType.NOT_INITIALIZED, cacheName, null, null,
            "Cache '" + cacheName + "' is not initialized. " +
            "Ensure cache manager is properly configured before use.");
    }
    
    /**
     * Create a "not initialized" error with additional context.
     * 
     * @param cacheName The name of the cache
     * @param operation The operation that was attempted
     * @return ApexCacheException with NOT_INITIALIZED type
     */
    public static ApexCacheException notInitialized(String cacheName, String operation) {
        return new ApexCacheException(
            ErrorType.NOT_INITIALIZED, cacheName, operation, null,
            "Cannot perform '" + operation + "' on cache '" + cacheName + 
            "': cache is not initialized.");
    }
    
    /**
     * Create a lookup failure error.
     * 
     * @param cacheName The name of the cache
     * @param key The key that was being looked up
     * @param cause The underlying cause
     * @return ApexCacheException with LOOKUP_FAILED type
     */
    public static ApexCacheException lookupFailed(String cacheName, String key, Throwable cause) {
        return new ApexCacheException(
            ErrorType.LOOKUP_FAILED, cacheName, "get", key,
            "Cache lookup failed for key '" + key + "' in cache '" + cacheName + "': " + 
            cause.getMessage(), cause);
    }
    
    /**
     * Create a lookup failure error without cause.
     * 
     * @param cacheName The name of the cache
     * @param key The key that was being looked up
     * @param message Additional error message
     * @return ApexCacheException with LOOKUP_FAILED type
     */
    public static ApexCacheException lookupFailed(String cacheName, String key, String message) {
        return new ApexCacheException(
            ErrorType.LOOKUP_FAILED, cacheName, "get", key,
            "Cache lookup failed for key '" + key + "' in cache '" + cacheName + "': " + message);
    }
    
    /**
     * Create a write failure error.
     * 
     * @param cacheName The name of the cache
     * @param key The key being written
     * @param cause The underlying cause
     * @return ApexCacheException with WRITE_FAILED type
     */
    public static ApexCacheException writeFailed(String cacheName, String key, Throwable cause) {
        return new ApexCacheException(
            ErrorType.WRITE_FAILED, cacheName, "put", key,
            "Cache write failed for key '" + key + "' in cache '" + cacheName + "': " + 
            cause.getMessage(), cause);
    }
    
    /**
     * Create a remove failure error.
     * 
     * @param cacheName The name of the cache
     * @param key The key being removed
     * @param cause The underlying cause
     * @return ApexCacheException with REMOVE_FAILED type
     */
    public static ApexCacheException removeFailed(String cacheName, String key, Throwable cause) {
        return new ApexCacheException(
            ErrorType.REMOVE_FAILED, cacheName, "remove", key,
            "Cache remove failed for key '" + key + "' in cache '" + cacheName + "': " + 
            cause.getMessage(), cause);
    }
    
    /**
     * Create an invalid key error.
     * 
     * @param cacheName The name of the cache
     * @param operation The operation being performed
     * @param key The invalid key (may be null for null key errors)
     * @return ApexCacheException with INVALID_KEY type
     */
    public static ApexCacheException invalidKey(String cacheName, String operation, String key) {
        String message = key == null 
            ? "Cache key cannot be null for operation '" + operation + "' on cache '" + cacheName + "'"
            : "Invalid cache key '" + key + "' for operation '" + operation + "' on cache '" + cacheName + "'";
        return new ApexCacheException(
            ErrorType.INVALID_KEY, cacheName, operation, key, message);
    }
    
    /**
     * Create a configuration error.
     * 
     * @param cacheName The name of the cache
     * @param message Detailed error message
     * @return ApexCacheException with CONFIGURATION_ERROR type
     */
    public static ApexCacheException configurationError(String cacheName, String message) {
        return new ApexCacheException(
            ErrorType.CONFIGURATION_ERROR, cacheName, null, null, message);
    }
    
    /**
     * Create a capacity exceeded error.
     * 
     * @param cacheName The name of the cache
     * @param currentSize Current cache size
     * @param maxSize Maximum cache size
     * @return ApexCacheException with CAPACITY_EXCEEDED type
     */
    public static ApexCacheException capacityExceeded(String cacheName, long currentSize, long maxSize) {
        return new ApexCacheException(
            ErrorType.CAPACITY_EXCEEDED, cacheName, "put", null,
            "Cache '" + cacheName + "' capacity exceeded: current size " + currentSize + 
            ", maximum " + maxSize);
    }
    
    /**
     * Create a serialization error.
     * 
     * @param cacheName The name of the cache
     * @param operation The operation being performed
     * @param cause The underlying cause
     * @return ApexCacheException with SERIALIZATION_ERROR type
     */
    public static ApexCacheException serializationError(String cacheName, String operation, Throwable cause) {
        return new ApexCacheException(
            ErrorType.SERIALIZATION_ERROR, cacheName, operation, null,
            "Serialization error during '" + operation + "' on cache '" + cacheName + "': " + 
            cause.getMessage(), cause);
    }
    
    /**
     * Wrap an existing exception as a general cache error.
     * 
     * @param cacheName The name of the cache
     * @param operation The operation that failed
     * @param cause The underlying cause
     * @return ApexCacheException with GENERAL_ERROR type
     */
    public static ApexCacheException wrap(String cacheName, String operation, Throwable cause) {
        return new ApexCacheException(
            ErrorType.GENERAL_ERROR, cacheName, operation, null,
            "Cache operation '" + operation + "' failed on cache '" + cacheName + "': " + 
            cause.getMessage(), cause);
    }
    
    @Override
    public String toString() {
        return "ApexCacheException{" +
               "errorType=" + errorType +
               ", cacheName='" + cacheName + '\'' +
               ", operation='" + operation + '\'' +
               ", key='" + key + '\'' +
               ", retryable=" + retryable +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
