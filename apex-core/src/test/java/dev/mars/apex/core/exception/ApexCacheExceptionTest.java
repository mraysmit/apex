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

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ApexCacheException}.
 * 
 * Tests verify that the exception class properly captures cache operation
 * context including cache name, operation, key, and provides appropriate
 * factory methods for common error scenarios.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-24
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("ApexCacheException Tests")
class ApexCacheExceptionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApexCacheExceptionTest.class);

    // ========================================
    // Constructor Tests
    // ========================================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with all fields populated")
        void shouldCreateExceptionWithAllFields() {
            LOGGER.info("=== Testing full constructor ===");
            
            ApexCacheException exception = new ApexCacheException(
                ApexCacheException.ErrorType.LOOKUP_FAILED,
                "customer-cache",
                "get",
                "CUST-123",
                "Failed to retrieve customer data"
            );

            assertEquals(ApexCacheException.ErrorType.LOOKUP_FAILED, exception.getErrorType());
            assertEquals("customer-cache", exception.getCacheName());
            assertEquals("get", exception.getOperation());
            assertEquals("CUST-123", exception.getKey());
            assertEquals("Failed to retrieve customer data", exception.getMessage());
            assertEquals("APEX-CACHE-002", exception.getErrorCode());
            assertTrue(exception.isRetryable());
            assertNotNull(exception.getContext());
            
            LOGGER.info("[OK] Exception created with all fields correctly");
        }

        @Test
        @DisplayName("Should create exception with cause")
        void shouldCreateExceptionWithCause() {
            LOGGER.info("=== Testing constructor with cause ===");
            
            RuntimeException cause = new RuntimeException("Connection lost");
            
            ApexCacheException exception = new ApexCacheException(
                ApexCacheException.ErrorType.LOOKUP_FAILED,
                "session-cache",
                "get",
                "SESSION-456",
                "Cache lookup failed",
                cause
            );

            assertEquals(cause, exception.getCause());
            assertEquals("Cache lookup failed", exception.getMessage());
            assertEquals("APEX-CACHE-002", exception.getErrorCode());
            
            LOGGER.info("[OK] Exception preserves cause correctly");
        }
    }

    // ========================================
    // Error Type Tests
    // ========================================

    @Nested
    @DisplayName("Error Type Tests")
    class ErrorTypeTests {

        @Test
        @DisplayName("Should have correct error codes for all types")
        void shouldHaveCorrectErrorCodes() {
            LOGGER.info("=== Testing error type codes ===");
            
            assertEquals("APEX-CACHE-001", ApexCacheException.ErrorType.NOT_INITIALIZED.getCode());
            assertEquals("APEX-CACHE-002", ApexCacheException.ErrorType.LOOKUP_FAILED.getCode());
            assertEquals("APEX-CACHE-003", ApexCacheException.ErrorType.WRITE_FAILED.getCode());
            assertEquals("APEX-CACHE-004", ApexCacheException.ErrorType.REMOVE_FAILED.getCode());
            assertEquals("APEX-CACHE-005", ApexCacheException.ErrorType.CLEAR_FAILED.getCode());
            assertEquals("APEX-CACHE-006", ApexCacheException.ErrorType.INVALID_KEY.getCode());
            assertEquals("APEX-CACHE-007", ApexCacheException.ErrorType.CONFIGURATION_ERROR.getCode());
            assertEquals("APEX-CACHE-008", ApexCacheException.ErrorType.CAPACITY_EXCEEDED.getCode());
            assertEquals("APEX-CACHE-009", ApexCacheException.ErrorType.SERIALIZATION_ERROR.getCode());
            assertEquals("APEX-CACHE-999", ApexCacheException.ErrorType.GENERAL_ERROR.getCode());
            
            LOGGER.info("[OK] All error types have correct codes");
        }

        @Test
        @DisplayName("Should have correct retryable flags")
        void shouldHaveCorrectRetryableFlags() {
            LOGGER.info("=== Testing retryable flags ===");
            
            // Non-retryable errors
            assertFalse(ApexCacheException.ErrorType.NOT_INITIALIZED.isRetryable());
            assertFalse(ApexCacheException.ErrorType.INVALID_KEY.isRetryable());
            assertFalse(ApexCacheException.ErrorType.CONFIGURATION_ERROR.isRetryable());
            assertFalse(ApexCacheException.ErrorType.SERIALIZATION_ERROR.isRetryable());
            
            // Retryable errors
            assertTrue(ApexCacheException.ErrorType.LOOKUP_FAILED.isRetryable());
            assertTrue(ApexCacheException.ErrorType.WRITE_FAILED.isRetryable());
            assertTrue(ApexCacheException.ErrorType.REMOVE_FAILED.isRetryable());
            assertTrue(ApexCacheException.ErrorType.CLEAR_FAILED.isRetryable());
            assertTrue(ApexCacheException.ErrorType.CAPACITY_EXCEEDED.isRetryable());
            assertTrue(ApexCacheException.ErrorType.GENERAL_ERROR.isRetryable());
            
            LOGGER.info("[OK] Retryable flags are correctly set");
        }

        @Test
        @DisplayName("Should have descriptions for all types")
        void shouldHaveDescriptions() {
            LOGGER.info("=== Testing error type descriptions ===");
            
            for (ApexCacheException.ErrorType type : ApexCacheException.ErrorType.values()) {
                assertNotNull(type.getDescription());
                assertFalse(type.getDescription().isEmpty());
                LOGGER.info("  {} -> {} (retryable: {})", 
                    type.getCode(), type.getDescription(), type.isRetryable());
            }
            
            LOGGER.info("[OK] All error types have descriptions");
        }
    }

    // ========================================
    // Factory Method Tests
    // ========================================

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create not initialized error")
        void shouldCreateNotInitializedError() {
            LOGGER.info("=== Testing notInitialized factory method ===");
            
            ApexCacheException exception = ApexCacheException.notInitialized("customer-cache");

            assertEquals(ApexCacheException.ErrorType.NOT_INITIALIZED, exception.getErrorType());
            assertEquals("customer-cache", exception.getCacheName());
            assertNull(exception.getOperation());
            assertFalse(exception.isRetryable());
            assertEquals("APEX-CACHE-001", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("not initialized"));
            
            LOGGER.info("[OK] Not initialized error factory method works correctly");
        }

        @Test
        @DisplayName("Should create not initialized error with operation")
        void shouldCreateNotInitializedErrorWithOperation() {
            LOGGER.info("=== Testing notInitialized factory method with operation ===");
            
            ApexCacheException exception = ApexCacheException.notInitialized(
                "session-cache",
                "get"
            );

            assertEquals("session-cache", exception.getCacheName());
            assertEquals("get", exception.getOperation());
            assertTrue(exception.getMessage().contains("get"));
            
            LOGGER.info("[OK] Not initialized with operation works correctly");
        }

        @Test
        @DisplayName("Should create lookup failed error")
        void shouldCreateLookupFailedError() {
            LOGGER.info("=== Testing lookupFailed factory method ===");
            
            RuntimeException cause = new RuntimeException("Network error");
            
            ApexCacheException exception = ApexCacheException.lookupFailed(
                "user-cache",
                "USER-001",
                cause
            );

            assertEquals(ApexCacheException.ErrorType.LOOKUP_FAILED, exception.getErrorType());
            assertEquals("user-cache", exception.getCacheName());
            assertEquals("get", exception.getOperation());
            assertEquals("USER-001", exception.getKey());
            assertEquals(cause, exception.getCause());
            assertTrue(exception.isRetryable());
            assertEquals("APEX-CACHE-002", exception.getErrorCode());
            
            LOGGER.info("[OK] Lookup failed factory method works correctly");
        }

        @Test
        @DisplayName("Should create lookup failed error with message")
        void shouldCreateLookupFailedErrorWithMessage() {
            LOGGER.info("=== Testing lookupFailed factory method with message ===");
            
            ApexCacheException exception = ApexCacheException.lookupFailed(
                "product-cache",
                "PROD-999",
                "Key not found after timeout"
            );

            assertEquals("PROD-999", exception.getKey());
            assertTrue(exception.getMessage().contains("PROD-999"));
            assertTrue(exception.getMessage().contains("Key not found after timeout"));
            assertNull(exception.getCause());
            
            LOGGER.info("[OK] Lookup failed with message works correctly");
        }

        @Test
        @DisplayName("Should create write failed error")
        void shouldCreateWriteFailedError() {
            LOGGER.info("=== Testing writeFailed factory method ===");
            
            RuntimeException cause = new RuntimeException("Disk full");
            
            ApexCacheException exception = ApexCacheException.writeFailed(
                "document-cache",
                "DOC-123",
                cause
            );

            assertEquals(ApexCacheException.ErrorType.WRITE_FAILED, exception.getErrorType());
            assertEquals("document-cache", exception.getCacheName());
            assertEquals("put", exception.getOperation());
            assertEquals("DOC-123", exception.getKey());
            assertEquals(cause, exception.getCause());
            assertEquals("APEX-CACHE-003", exception.getErrorCode());
            
            LOGGER.info("[OK] Write failed factory method works correctly");
        }

        @Test
        @DisplayName("Should create remove failed error")
        void shouldCreateRemoveFailedError() {
            LOGGER.info("=== Testing removeFailed factory method ===");
            
            RuntimeException cause = new RuntimeException("Lock timeout");
            
            ApexCacheException exception = ApexCacheException.removeFailed(
                "temp-cache",
                "TEMP-456",
                cause
            );

            assertEquals(ApexCacheException.ErrorType.REMOVE_FAILED, exception.getErrorType());
            assertEquals("remove", exception.getOperation());
            assertEquals("APEX-CACHE-004", exception.getErrorCode());
            
            LOGGER.info("[OK] Remove failed factory method works correctly");
        }

        @Test
        @DisplayName("Should create invalid key error for null key")
        void shouldCreateInvalidKeyErrorForNullKey() {
            LOGGER.info("=== Testing invalidKey factory method for null key ===");
            
            ApexCacheException exception = ApexCacheException.invalidKey(
                "my-cache",
                "get",
                null
            );

            assertEquals(ApexCacheException.ErrorType.INVALID_KEY, exception.getErrorType());
            assertFalse(exception.isRetryable());
            assertEquals("APEX-CACHE-006", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("null"));
            
            LOGGER.info("[OK] Invalid key for null key works correctly");
        }

        @Test
        @DisplayName("Should create invalid key error for malformed key")
        void shouldCreateInvalidKeyErrorForMalformedKey() {
            LOGGER.info("=== Testing invalidKey factory method for malformed key ===");
            
            ApexCacheException exception = ApexCacheException.invalidKey(
                "my-cache",
                "put",
                "key:with:colons"
            );

            assertEquals("key:with:colons", exception.getKey());
            assertTrue(exception.getMessage().contains("key:with:colons"));
            
            LOGGER.info("[OK] Invalid key for malformed key works correctly");
        }

        @Test
        @DisplayName("Should create configuration error")
        void shouldCreateConfigurationError() {
            LOGGER.info("=== Testing configurationError factory method ===");
            
            ApexCacheException exception = ApexCacheException.configurationError(
                "redis-cache",
                "Invalid Redis URL format"
            );

            assertEquals(ApexCacheException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
            assertEquals("redis-cache", exception.getCacheName());
            assertFalse(exception.isRetryable());
            assertEquals("APEX-CACHE-007", exception.getErrorCode());
            
            LOGGER.info("[OK] Configuration error factory method works correctly");
        }

        @Test
        @DisplayName("Should create capacity exceeded error")
        void shouldCreateCapacityExceededError() {
            LOGGER.info("=== Testing capacityExceeded factory method ===");
            
            ApexCacheException exception = ApexCacheException.capacityExceeded(
                "bounded-cache",
                1000L,
                1000L
            );

            assertEquals(ApexCacheException.ErrorType.CAPACITY_EXCEEDED, exception.getErrorType());
            assertTrue(exception.isRetryable());
            assertEquals("APEX-CACHE-008", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("1000"));
            
            LOGGER.info("[OK] Capacity exceeded factory method works correctly");
        }

        @Test
        @DisplayName("Should create serialization error")
        void shouldCreateSerializationError() {
            LOGGER.info("=== Testing serializationError factory method ===");
            
            RuntimeException cause = new RuntimeException("Not serializable");
            
            ApexCacheException exception = ApexCacheException.serializationError(
                "distributed-cache",
                "put",
                cause
            );

            assertEquals(ApexCacheException.ErrorType.SERIALIZATION_ERROR, exception.getErrorType());
            assertEquals("put", exception.getOperation());
            assertFalse(exception.isRetryable());
            assertEquals("APEX-CACHE-009", exception.getErrorCode());
            assertEquals(cause, exception.getCause());
            
            LOGGER.info("[OK] Serialization error factory method works correctly");
        }

        @Test
        @DisplayName("Should wrap unexpected exception")
        void shouldWrapUnexpectedException() {
            LOGGER.info("=== Testing wrap factory method ===");
            
            OutOfMemoryError oom = new OutOfMemoryError("Heap space");
            
            ApexCacheException exception = ApexCacheException.wrap(
                "large-cache",
                "put",
                oom
            );

            assertEquals(ApexCacheException.ErrorType.GENERAL_ERROR, exception.getErrorType());
            assertEquals("large-cache", exception.getCacheName());
            assertEquals("put", exception.getOperation());
            assertEquals(oom, exception.getCause());
            assertTrue(exception.isRetryable());
            assertEquals("APEX-CACHE-999", exception.getErrorCode());
            
            LOGGER.info("[OK] Wrap factory method works correctly");
        }
    }

    // ========================================
    // Message and Context Tests
    // ========================================

    @Nested
    @DisplayName("Message and Context Tests")
    class MessageAndContextTests {

        @Test
        @DisplayName("Should build context with all fields")
        void shouldBuildContextWithAllFields() {
            LOGGER.info("=== Testing context building ===");
            
            ApexCacheException exception = new ApexCacheException(
                ApexCacheException.ErrorType.LOOKUP_FAILED,
                "test-cache",
                "get",
                "KEY-123",
                "Test error"
            );

            String context = exception.getContext();
            assertTrue(context.contains("test-cache"));
            assertTrue(context.contains("get"));
            assertTrue(context.contains("KEY-123"));
            
            LOGGER.info("Context: {}", context);
            LOGGER.info("[OK] Context contains all fields");
        }

        @Test
        @DisplayName("Should handle null optional fields in context")
        void shouldHandleNullFieldsInContext() {
            LOGGER.info("=== Testing context with null fields ===");
            
            ApexCacheException exception = ApexCacheException.notInitialized("my-cache");

            String context = exception.getContext();
            assertTrue(context.contains("my-cache"));
            assertFalse(context.contains("Operation:"));
            assertFalse(context.contains("Key:"));
            
            LOGGER.info("Context: {}", context);
            LOGGER.info("[OK] Context handles null fields gracefully");
        }

        @Test
        @DisplayName("Should provide meaningful toString")
        void shouldProvideToString() {
            LOGGER.info("=== Testing toString ===");
            
            ApexCacheException exception = new ApexCacheException(
                ApexCacheException.ErrorType.LOOKUP_FAILED,
                "my-cache",
                "get",
                "MY-KEY",
                "Test message"
            );

            String str = exception.toString();
            assertTrue(str.contains("ApexCacheException"));
            assertTrue(str.contains("LOOKUP_FAILED"));
            assertTrue(str.contains("my-cache"));
            assertTrue(str.contains("get"));
            assertTrue(str.contains("MY-KEY"));
            assertTrue(str.contains("retryable=true"));
            
            LOGGER.info("toString: {}", str);
            LOGGER.info("[OK] toString provides all relevant information");
        }
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should be catchable as RuntimeException")
        void shouldBeCatchableAsRuntimeException() {
            LOGGER.info("=== Testing exception hierarchy ===");
            
            ApexCacheException exception = ApexCacheException.notInitialized("test-cache");

            // Should be assignable to parent
            assertTrue(exception instanceof RuntimeException);
            assertTrue(exception instanceof Exception);
            
            // Should be catchable as RuntimeException
            try {
                throw exception;
            } catch (RuntimeException e) {
                assertTrue(e instanceof ApexCacheException);
                ApexCacheException ace = (ApexCacheException) e;
                assertEquals("APEX-CACHE-001", ace.getErrorCode());
                assertNotNull(ace.getContext());
            }
            
            LOGGER.info("[OK] Exception hierarchy is correct");
        }

        @Test
        @DisplayName("Should use retryable flag in retry logic")
        void shouldUseRetryableFlagInRetryLogic() {
            LOGGER.info("=== Testing retryable flag usage ===");
            
            ApexCacheException notInit = ApexCacheException.notInitialized("cache");
            ApexCacheException lookupFail = ApexCacheException.lookupFailed("cache", "key", "error");
            
            // Simulating retry decision
            assertFalse(shouldRetry(notInit), "NOT_INITIALIZED should not trigger retry");
            assertTrue(shouldRetry(lookupFail), "LOOKUP_FAILED should trigger retry");
            
            LOGGER.info("[OK] Retryable flag works correctly in retry logic");
        }

        private boolean shouldRetry(ApexCacheException e) {
            return e.isRetryable();
        }
    }
}
