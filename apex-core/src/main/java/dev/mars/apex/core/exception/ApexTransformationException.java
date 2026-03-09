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
 * Runtime exception thrown when transformation processing fails.
 * 
 * This exception provides detailed context about transformation failures including:
 * - The transformation ID that failed
 * - The expression that caused the failure
 * - The original value being transformed
 * - Error codes for categorization
 * 
 * <p>This is an unchecked exception to allow transparent error propagation
 * through transformation pipelines without requiring explicit exception handling
 * at every level.
 * 
 * <p>Usage example:
 * <pre>{@code
 * throw ApexTransformationException.expressionError(
 *     "calculate-premium",
 *     "#value * rate",
 *     "Expression evaluation failed: variable 'rate' not found"
 * );
 * }</pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-24
 * @version 1.0
 */
public class ApexTransformationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Enumeration of transformation error types.
     */
    public enum ErrorType {
        /**
         * Expression evaluation failed (SpEL parsing or evaluation error).
         */
        EXPRESSION_ERROR("APEX-TRANS-001", "Expression evaluation failed"),
        
        /**
         * Type conversion or coercion failed.
         */
        TYPE_CONVERSION_ERROR("APEX-TRANS-002", "Type conversion failed"),
        
        /**
         * Source field not found or inaccessible.
         */
        SOURCE_FIELD_ERROR("APEX-TRANS-003", "Source field error"),
        
        /**
         * Target field not found or not writable.
         */
        TARGET_FIELD_ERROR("APEX-TRANS-004", "Target field error"),
        
        /**
         * Transformation configuration is invalid.
         */
        CONFIGURATION_ERROR("APEX-TRANS-005", "Transformation configuration error"),
        
        /**
         * Condition evaluation failed.
         */
        CONDITION_ERROR("APEX-TRANS-006", "Condition evaluation failed"),
        
        /**
         * Null value encountered where not allowed.
         */
        NULL_VALUE_ERROR("APEX-TRANS-007", "Null value encountered"),
        
        /**
         * General transformation error.
         */
        GENERAL_ERROR("APEX-TRANS-999", "Transformation error");
        
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
    private final String transformationId;
    private final String expression;
    private final Object originalValue;
    private final String errorCode;
    private final String context;
    
    /**
     * Create an ApexTransformationException with full details.
     * 
     * @param errorType The type of transformation error
     * @param transformationId The ID of the transformation that failed
     * @param expression The expression that caused the failure (may be null)
     * @param message The error message
     * @param originalValue The original value being transformed (may be null)
     */
    public ApexTransformationException(ErrorType errorType, String transformationId, 
                                        String expression, String message, Object originalValue) {
        super(message);
        this.errorType = errorType;
        this.transformationId = transformationId;
        this.expression = expression;
        this.originalValue = originalValue;
        this.errorCode = errorType.getCode();
        this.context = buildContext(transformationId, expression);
    }
    
    /**
     * Create an ApexTransformationException with full details and a cause.
     * 
     * @param errorType The type of transformation error
     * @param transformationId The ID of the transformation that failed
     * @param expression The expression that caused the failure (may be null)
     * @param message The error message
     * @param originalValue The original value being transformed (may be null)
     * @param cause The underlying cause
     */
    public ApexTransformationException(ErrorType errorType, String transformationId,
                                        String expression, String message, Object originalValue, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.transformationId = transformationId;
        this.expression = expression;
        this.originalValue = originalValue;
        this.errorCode = errorType.getCode();
        this.context = buildContext(transformationId, expression);
    }
    
    private static String buildContext(String transformationId, String expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transformation: ").append(transformationId != null ? transformationId : "unknown");
        if (expression != null && !expression.isEmpty()) {
            sb.append(", Expression: ").append(expression);
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
     * Get the transformation ID.
     * 
     * @return The transformation ID, or null if not specified
     */
    public String getTransformationId() {
        return transformationId;
    }
    
    /**
     * Get the expression that failed.
     * 
     * @return The expression, or null if not applicable
     */
    public String getExpression() {
        return expression;
    }
    
    /**
     * Get the original value being transformed.
     * 
     * @return The original value, or null if not available
     */
    public Object getOriginalValue() {
        return originalValue;
    }
    
    /**
     * Get the error code.
     * 
     * @return The error code (e.g., APEX-TRANS-001)
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
     * Create an expression evaluation error.
     * 
     * @param transformationId The transformation ID
     * @param expression The expression that failed
     * @param message Detailed error message
     * @return ApexTransformationException with EXPRESSION_ERROR type
     */
    public static ApexTransformationException expressionError(String transformationId, 
                                                               String expression, String message) {
        return new ApexTransformationException(
            ErrorType.EXPRESSION_ERROR, transformationId, expression, message, null);
    }
    
    /**
     * Create an expression evaluation error with cause.
     * 
     * @param transformationId The transformation ID
     * @param expression The expression that failed
     * @param message Detailed error message
     * @param cause The underlying cause
     * @return ApexTransformationException with EXPRESSION_ERROR type
     */
    public static ApexTransformationException expressionError(String transformationId, 
                                                               String expression, String message, Throwable cause) {
        return new ApexTransformationException(
            ErrorType.EXPRESSION_ERROR, transformationId, expression, message, null, cause);
    }
    
    /**
     * Create a type conversion error.
     * 
     * @param transformationId The transformation ID
     * @param message Detailed error message
     * @param originalValue The value that couldn't be converted
     * @return ApexTransformationException with TYPE_CONVERSION_ERROR type
     */
    public static ApexTransformationException typeConversionError(String transformationId, 
                                                                   String message, Object originalValue) {
        return new ApexTransformationException(
            ErrorType.TYPE_CONVERSION_ERROR, transformationId, null, message, originalValue);
    }
    
    /**
     * Create a type conversion error with cause.
     * 
     * @param transformationId The transformation ID
     * @param message Detailed error message
     * @param originalValue The value that couldn't be converted
     * @param cause The underlying cause
     * @return ApexTransformationException with TYPE_CONVERSION_ERROR type
     */
    public static ApexTransformationException typeConversionError(String transformationId, 
                                                                   String message, Object originalValue, Throwable cause) {
        return new ApexTransformationException(
            ErrorType.TYPE_CONVERSION_ERROR, transformationId, null, message, originalValue, cause);
    }
    
    /**
     * Create a source field error.
     * 
     * @param transformationId The transformation ID
     * @param fieldName The source field name
     * @param message Detailed error message
     * @return ApexTransformationException with SOURCE_FIELD_ERROR type
     */
    public static ApexTransformationException sourceFieldError(String transformationId, 
                                                                String fieldName, String message) {
        return new ApexTransformationException(
            ErrorType.SOURCE_FIELD_ERROR, transformationId, fieldName, message, null);
    }
    
    /**
     * Create a target field error.
     * 
     * @param transformationId The transformation ID
     * @param fieldName The target field name
     * @param message Detailed error message
     * @return ApexTransformationException with TARGET_FIELD_ERROR type
     */
    public static ApexTransformationException targetFieldError(String transformationId, 
                                                                String fieldName, String message) {
        return new ApexTransformationException(
            ErrorType.TARGET_FIELD_ERROR, transformationId, fieldName, message, null);
    }
    
    /**
     * Create a configuration error.
     * 
     * @param transformationId The transformation ID
     * @param message Detailed error message
     * @return ApexTransformationException with CONFIGURATION_ERROR type
     */
    public static ApexTransformationException configurationError(String transformationId, String message) {
        return new ApexTransformationException(
            ErrorType.CONFIGURATION_ERROR, transformationId, null, message, null);
    }
    
    /**
     * Create a condition evaluation error.
     * 
     * @param transformationId The transformation ID
     * @param condition The condition that failed
     * @param message Detailed error message
     * @param cause The underlying cause
     * @return ApexTransformationException with CONDITION_ERROR type
     */
    public static ApexTransformationException conditionError(String transformationId, 
                                                              String condition, String message, Throwable cause) {
        return new ApexTransformationException(
            ErrorType.CONDITION_ERROR, transformationId, condition, message, null, cause);
    }
    
    /**
     * Create a null value error.
     * 
     * @param transformationId The transformation ID
     * @param fieldName The field that was null
     * @param message Detailed error message
     * @return ApexTransformationException with NULL_VALUE_ERROR type
     */
    public static ApexTransformationException nullValueError(String transformationId, 
                                                              String fieldName, String message) {
        return new ApexTransformationException(
            ErrorType.NULL_VALUE_ERROR, transformationId, fieldName, message, null);
    }
    
    /**
     * Create a general transformation error from an existing exception.
     * Useful for wrapping unexpected exceptions.
     * 
     * @param transformationId The transformation ID
     * @param cause The underlying cause
     * @return ApexTransformationException with GENERAL_ERROR type
     */
    public static ApexTransformationException wrap(String transformationId, Throwable cause) {
        return new ApexTransformationException(
            ErrorType.GENERAL_ERROR, transformationId, null,
            "Transformation failed: " + cause.getMessage(), null, cause);
    }
    
    @Override
    public String toString() {
        return "ApexTransformationException{" +
               "errorType=" + errorType +
               ", transformationId='" + transformationId + '\'' +
               ", expression='" + expression + '\'' +
               ", originalValue=" + originalValue +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
