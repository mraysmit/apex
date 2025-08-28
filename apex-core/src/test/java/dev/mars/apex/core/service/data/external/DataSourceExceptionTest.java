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


import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DataSourceException.
 * 
 * Tests cover:
 * - Exception hierarchy and inheritance
 * - Constructor variations and parameter handling
 * - Error type enumeration and categorization
 * - Factory method creation patterns
 * - Message formatting and error details
 * - Cause chain handling and stack traces
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataSourceExceptionTest {

    // ========================================
    // Error Type Enum Tests
    // ========================================

    @Test
    @DisplayName("Should have correct error type values")
    void testErrorTypeValues() {
        DataSourceException.ErrorType[] types = DataSourceException.ErrorType.values();
        assertEquals(9, types.length);
        
        assertEquals(DataSourceException.ErrorType.CONNECTION_ERROR, types[0]);
        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, types[1]);
        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, types[2]);
        assertEquals(DataSourceException.ErrorType.DATA_FORMAT_ERROR, types[3]);
        assertEquals(DataSourceException.ErrorType.TIMEOUT_ERROR, types[4]);
        assertEquals(DataSourceException.ErrorType.AUTHENTICATION_ERROR, types[5]);
        assertEquals(DataSourceException.ErrorType.NOT_FOUND_ERROR, types[6]);
        assertEquals(DataSourceException.ErrorType.CIRCUIT_BREAKER_ERROR, types[7]);
        assertEquals(DataSourceException.ErrorType.GENERAL_ERROR, types[8]);
    }

    @Test
    @DisplayName("Should have correct error type codes")
    void testErrorTypeCodes() {
        assertEquals("CONNECTION_ERROR", DataSourceException.ErrorType.CONNECTION_ERROR.getCode());
        assertEquals("CONFIGURATION_ERROR", DataSourceException.ErrorType.CONFIGURATION_ERROR.getCode());
        assertEquals("EXECUTION_ERROR", DataSourceException.ErrorType.EXECUTION_ERROR.getCode());
        assertEquals("DATA_FORMAT_ERROR", DataSourceException.ErrorType.DATA_FORMAT_ERROR.getCode());
        assertEquals("TIMEOUT_ERROR", DataSourceException.ErrorType.TIMEOUT_ERROR.getCode());
        assertEquals("AUTHENTICATION_ERROR", DataSourceException.ErrorType.AUTHENTICATION_ERROR.getCode());
        assertEquals("NOT_FOUND_ERROR", DataSourceException.ErrorType.NOT_FOUND_ERROR.getCode());
        assertEquals("CIRCUIT_BREAKER_ERROR", DataSourceException.ErrorType.CIRCUIT_BREAKER_ERROR.getCode());
        assertEquals("GENERAL_ERROR", DataSourceException.ErrorType.GENERAL_ERROR.getCode());
    }

    @Test
    @DisplayName("Should have correct error type descriptions")
    void testErrorTypeDescriptions() {
        assertEquals("Connection failed", DataSourceException.ErrorType.CONNECTION_ERROR.getDescription());
        assertEquals("Configuration error", DataSourceException.ErrorType.CONFIGURATION_ERROR.getDescription());
        assertEquals("Execution failed", DataSourceException.ErrorType.EXECUTION_ERROR.getDescription());
        assertEquals("Data format error", DataSourceException.ErrorType.DATA_FORMAT_ERROR.getDescription());
        assertEquals("Operation timed out", DataSourceException.ErrorType.TIMEOUT_ERROR.getDescription());
        assertEquals("Authentication failed", DataSourceException.ErrorType.AUTHENTICATION_ERROR.getDescription());
        assertEquals("Resource not found", DataSourceException.ErrorType.NOT_FOUND_ERROR.getDescription());
        assertEquals("Circuit breaker activated", DataSourceException.ErrorType.CIRCUIT_BREAKER_ERROR.getDescription());
        assertEquals("General error", DataSourceException.ErrorType.GENERAL_ERROR.getDescription());
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create exception with error type and message")
    void testConstructorWithErrorTypeAndMessage() {
        String message = "Test error message";
        DataSourceException.ErrorType errorType = DataSourceException.ErrorType.CONNECTION_ERROR;
        
        DataSourceException exception = new DataSourceException(errorType, message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorType, exception.getErrorType());
        assertNull(exception.getCause());
        assertNull(exception.getDataSourceName());
        assertNull(exception.getOperation());
        assertFalse(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create exception with error type, message, and cause")
    void testConstructorWithErrorTypeMessageAndCause() {
        String message = "Test error message";
        DataSourceException.ErrorType errorType = DataSourceException.ErrorType.EXECUTION_ERROR;
        RuntimeException cause = new RuntimeException("Root cause");
        
        DataSourceException exception = new DataSourceException(errorType, message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorType, exception.getErrorType());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getDataSourceName());
        assertNull(exception.getOperation());
        assertFalse(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create exception with all parameters")
    void testConstructorWithAllParameters() {
        String message = "Authentication failed";
        DataSourceException.ErrorType errorType = DataSourceException.ErrorType.AUTHENTICATION_ERROR;
        RuntimeException cause = new RuntimeException("Invalid credentials");
        String dataSourceName = "test-database";
        String operation = "connect";
        boolean retryable = true;
        
        DataSourceException exception = new DataSourceException(
            errorType, message, cause, dataSourceName, operation, retryable);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorType, exception.getErrorType());
        assertEquals(cause, exception.getCause());
        assertEquals(dataSourceName, exception.getDataSourceName());
        assertEquals(operation, exception.getOperation());
        assertEquals(retryable, exception.isRetryable());
    }

    // ========================================
    // Factory Method Tests
    // ========================================

    @Test
    @DisplayName("Should create connection error exception")
    void testConnectionError() {
        String message = "Failed to connect to database";
        RuntimeException cause = new RuntimeException("Network error");
        
        DataSourceException exception = DataSourceException.connectionError(message, cause);
        
        assertEquals(DataSourceException.ErrorType.CONNECTION_ERROR, exception.getErrorType());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getDataSourceName());
        assertNull(exception.getOperation());
        assertTrue(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create configuration error exception")
    void testConfigurationError() {
        String message = "Invalid connection URL";
        
        DataSourceException exception = DataSourceException.configurationError(message);
        
        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getDataSourceName());
        assertNull(exception.getOperation());
        assertFalse(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create execution error exception")
    void testExecutionError() {
        String message = "Query execution failed";
        RuntimeException cause = new RuntimeException("Syntax error");
        String operation = "SELECT * FROM users";
        
        DataSourceException exception = DataSourceException.executionError(message, cause, operation);
        
        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, exception.getErrorType());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getDataSourceName());
        assertEquals(operation, exception.getOperation());
        assertTrue(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create timeout error exception")
    void testTimeoutError() {
        String message = "Operation timed out after 30 seconds";
        String operation = "data-fetch";
        
        DataSourceException exception = DataSourceException.timeoutError(message, operation);
        
        assertEquals(DataSourceException.ErrorType.TIMEOUT_ERROR, exception.getErrorType());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getDataSourceName());
        assertEquals(operation, exception.getOperation());
        assertTrue(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create authentication error exception")
    void testAuthenticationError() {
        String message = "Invalid credentials";
        String dataSourceName = "secure-db";
        
        DataSourceException exception = DataSourceException.authenticationError(message, dataSourceName);
        
        assertEquals(DataSourceException.ErrorType.AUTHENTICATION_ERROR, exception.getErrorType());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(dataSourceName, exception.getDataSourceName());
        assertNull(exception.getOperation());
        assertFalse(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create not found error exception")
    void testNotFoundError() {
        String message = "Resource not found";
        String operation = "find-user";
        
        DataSourceException exception = DataSourceException.notFoundError(message, operation);
        
        assertEquals(DataSourceException.ErrorType.NOT_FOUND_ERROR, exception.getErrorType());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertNull(exception.getDataSourceName());
        assertEquals(operation, exception.getOperation());
        assertFalse(exception.isRetryable());
    }

    @Test
    @DisplayName("Should create circuit breaker error exception")
    void testCircuitBreakerError() {
        String message = "Circuit breaker is open";
        String dataSourceName = "external-api";
        
        DataSourceException exception = DataSourceException.circuitBreakerError(message, dataSourceName);
        
        assertEquals(DataSourceException.ErrorType.CIRCUIT_BREAKER_ERROR, exception.getErrorType());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(dataSourceName, exception.getDataSourceName());
        assertNull(exception.getOperation());
        assertTrue(exception.isRetryable());
    }

    // ========================================
    // Utility Method Tests
    // ========================================

    @Test
    @DisplayName("Should get error code from error type")
    void testGetErrorCode() {
        DataSourceException exception = new DataSourceException(
            DataSourceException.ErrorType.CONNECTION_ERROR, "Test message");
        
        assertEquals("CONNECTION_ERROR", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should generate detailed error message")
    void testGetDetailedMessage() {
        String message = "Connection failed";
        String dataSourceName = "test-db";
        String operation = "connect";
        RuntimeException cause = new RuntimeException("Socket timeout");
        
        DataSourceException exception = new DataSourceException(
            DataSourceException.ErrorType.CONNECTION_ERROR, message, cause, dataSourceName, operation, true);
        
        String detailedMessage = exception.getDetailedMessage();
        
        assertTrue(detailedMessage.contains("[CONNECTION_ERROR]"));
        assertTrue(detailedMessage.contains(message));
        assertTrue(detailedMessage.contains("Data Source: " + dataSourceName));
        assertTrue(detailedMessage.contains("Operation: " + operation));
        assertTrue(detailedMessage.contains("[Retryable]"));
    }

    @Test
    @DisplayName("Should generate detailed message with minimal information")
    void testGetDetailedMessageMinimal() {
        String message = "Simple error";
        DataSourceException exception = new DataSourceException(
            DataSourceException.ErrorType.GENERAL_ERROR, message);
        
        String detailedMessage = exception.getDetailedMessage();
        
        assertTrue(detailedMessage.contains("[GENERAL_ERROR]"));
        assertTrue(detailedMessage.contains(message));
        assertFalse(detailedMessage.contains("Data Source:"));
        assertFalse(detailedMessage.contains("Operation:"));
        assertFalse(detailedMessage.contains("[Retryable]"));
    }

    // ========================================
    // Edge Cases and Null Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle null message gracefully")
    void testNullMessage() {
        DataSourceException exception = new DataSourceException(
            DataSourceException.ErrorType.GENERAL_ERROR, null);

        assertNull(exception.getMessage());
        assertEquals(DataSourceException.ErrorType.GENERAL_ERROR, exception.getErrorType());
    }

    @Test
    @DisplayName("Should handle null error type gracefully")
    void testNullErrorType() {
        String message = "Test message";
        DataSourceException exception = new DataSourceException(null, message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorType());
    }

    @Test
    @DisplayName("Should handle null parameters in factory methods")
    void testNullParametersInFactoryMethods() {
        DataSourceException exception1 = DataSourceException.connectionError(null, null);
        assertEquals(DataSourceException.ErrorType.CONNECTION_ERROR, exception1.getErrorType());
        assertNull(exception1.getMessage());
        assertNull(exception1.getCause());

        DataSourceException exception2 = DataSourceException.authenticationError(null, null);
        assertEquals(DataSourceException.ErrorType.AUTHENTICATION_ERROR, exception2.getErrorType());
        assertNull(exception2.getMessage());
        assertNull(exception2.getDataSourceName());
    }

    // ========================================
    // Inheritance and Exception Hierarchy Tests
    // ========================================

    @Test
    @DisplayName("Should extend Exception")
    void testInheritance() {
        DataSourceException exception = new DataSourceException(
            DataSourceException.ErrorType.GENERAL_ERROR, "Test");

        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
        // Note: DataSourceException extends Exception, not RuntimeException
        // We can't use instanceof with RuntimeException since it's not in the hierarchy
        assertFalse(RuntimeException.class.isAssignableFrom(exception.getClass()));
    }

    @Test
    @DisplayName("Should be throwable and catchable")
    void testThrowable() {
        String message = "Test exception";

        assertThrows(DataSourceException.class, () -> {
            throw new DataSourceException(DataSourceException.ErrorType.GENERAL_ERROR, message);
        });

        try {
            throw new DataSourceException(DataSourceException.ErrorType.GENERAL_ERROR, message);
        } catch (DataSourceException e) {
            assertEquals(message, e.getMessage());
        } catch (Exception e) {
            fail("Should have caught DataSourceException, not " + e.getClass().getSimpleName());
        }
    }

    @Test
    @DisplayName("Should have meaningful toString representation")
    void testToString() {
        DataSourceException exception = new DataSourceException(
            DataSourceException.ErrorType.CONNECTION_ERROR, "Test connection", null, "test-db", "connect", true);

        String toString = exception.toString();

        assertTrue(toString.contains("DataSourceException"));
        assertTrue(toString.contains("CONNECTION_ERROR"));
        assertTrue(toString.contains("test-db"));
        assertTrue(toString.contains("connect"));
        assertTrue(toString.contains("retryable=true"));
        assertTrue(toString.contains("Test connection"));
    }

    @Test
    @DisplayName("Should handle toString with null values")
    void testToStringWithNulls() {
        DataSourceException exception = new DataSourceException(
            DataSourceException.ErrorType.GENERAL_ERROR, null);

        String toString = exception.toString();

        assertTrue(toString.contains("DataSourceException"));
        assertTrue(toString.contains("GENERAL_ERROR"));
        assertTrue(toString.contains("retryable=false"));
        // Should handle null message gracefully
        assertNotNull(toString);
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Test
    @DisplayName("Should work correctly in exception chaining")
    void testExceptionChaining() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        DataSourceException middleCause = DataSourceException.connectionError("Connection failed", rootCause);
        DataSourceException topException = DataSourceException.executionError("Execution failed", middleCause, "query");

        assertEquals("Execution failed", topException.getMessage());
        assertEquals(middleCause, topException.getCause());
        assertEquals(rootCause, topException.getCause().getCause());
        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, topException.getErrorType());
    }

    @Test
    @DisplayName("Should maintain error context through chaining")
    void testErrorContextThroughChaining() {
        DataSourceException originalException = new DataSourceException(
            DataSourceException.ErrorType.TIMEOUT_ERROR, "Timeout occurred", null, "db1", "select", true);

        DataSourceException wrappedException = DataSourceException.executionError(
            "Failed to execute due to timeout", originalException, "complex-query");

        assertEquals(DataSourceException.ErrorType.EXECUTION_ERROR, wrappedException.getErrorType());
        assertEquals("complex-query", wrappedException.getOperation());
        assertTrue(wrappedException.isRetryable());

        // Original exception details should be preserved in the cause
        DataSourceException cause = (DataSourceException) wrappedException.getCause();
        assertEquals(DataSourceException.ErrorType.TIMEOUT_ERROR, cause.getErrorType());
        assertEquals("db1", cause.getDataSourceName());
        assertEquals("select", cause.getOperation());
    }
}
