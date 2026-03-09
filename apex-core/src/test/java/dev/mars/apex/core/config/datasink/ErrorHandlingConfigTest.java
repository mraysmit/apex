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
package dev.mars.apex.core.config.datasink;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for ErrorHandlingConfig - data sink error handling configuration.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("ErrorHandlingConfig Tests")
class ErrorHandlingConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingConfigTest.class);

    private ErrorHandlingConfig config;

    @BeforeEach
    void setUp() {
        config = new ErrorHandlingConfig();
    }

    @Nested
    @DisplayName("ErrorStrategy Enum Tests")
    class ErrorStrategyEnumTests {

        @Test
        @DisplayName("Should have all expected error strategies")
        void shouldHaveAllExpectedErrorStrategies() {
            ErrorHandlingConfig.ErrorStrategy[] strategies = ErrorHandlingConfig.ErrorStrategy.values();
            
            assertEquals(6, strategies.length);
            assertNotNull(ErrorHandlingConfig.ErrorStrategy.FAIL_FAST);
            assertNotNull(ErrorHandlingConfig.ErrorStrategy.LOG_AND_CONTINUE);
            assertNotNull(ErrorHandlingConfig.ErrorStrategy.DEAD_LETTER);
            assertNotNull(ErrorHandlingConfig.ErrorStrategy.RETRY_AND_FAIL);
            assertNotNull(ErrorHandlingConfig.ErrorStrategy.RETRY_AND_CONTINUE);
            assertNotNull(ErrorHandlingConfig.ErrorStrategy.CUSTOM);
            
            logger.info("[OK] All expected error strategies present");
        }

        @Test
        @DisplayName("Should have correct codes for error strategies")
        void shouldHaveCorrectCodesForErrorStrategies() {
            assertEquals("fail-fast", ErrorHandlingConfig.ErrorStrategy.FAIL_FAST.getCode());
            assertEquals("log-and-continue", ErrorHandlingConfig.ErrorStrategy.LOG_AND_CONTINUE.getCode());
            assertEquals("dead-letter", ErrorHandlingConfig.ErrorStrategy.DEAD_LETTER.getCode());
            assertEquals("retry-and-fail", ErrorHandlingConfig.ErrorStrategy.RETRY_AND_FAIL.getCode());
            assertEquals("retry-and-continue", ErrorHandlingConfig.ErrorStrategy.RETRY_AND_CONTINUE.getCode());
            assertEquals("custom", ErrorHandlingConfig.ErrorStrategy.CUSTOM.getCode());
            
            logger.info("[OK] All error strategy codes verified");
        }

        @Test
        @DisplayName("Should have descriptions for error strategies")
        void shouldHaveDescriptionsForErrorStrategies() {
            for (ErrorHandlingConfig.ErrorStrategy strategy : ErrorHandlingConfig.ErrorStrategy.values()) {
                assertNotNull(strategy.getDescription());
                assertFalse(strategy.getDescription().isEmpty());
            }
            
            logger.info("[OK] All error strategies have descriptions");
        }

        @Test
        @DisplayName("Should convert code to strategy (case insensitive)")
        void shouldConvertCodeToStrategyCaseInsensitive() {
            assertEquals(ErrorHandlingConfig.ErrorStrategy.FAIL_FAST, 
                        ErrorHandlingConfig.ErrorStrategy.fromCode("fail-fast"));
            assertEquals(ErrorHandlingConfig.ErrorStrategy.FAIL_FAST, 
                        ErrorHandlingConfig.ErrorStrategy.fromCode("FAIL-FAST"));
            assertEquals(ErrorHandlingConfig.ErrorStrategy.LOG_AND_CONTINUE, 
                        ErrorHandlingConfig.ErrorStrategy.fromCode("Log-And-Continue"));
            
            logger.info("[OK] Code to strategy conversion case insensitive");
        }

        @Test
        @DisplayName("Should return FAIL_FAST as default for null code")
        void shouldReturnFailFastAsDefaultForNullCode() {
            assertEquals(ErrorHandlingConfig.ErrorStrategy.FAIL_FAST, 
                        ErrorHandlingConfig.ErrorStrategy.fromCode(null));
            
            logger.info("[OK] FAIL_FAST returned as default for null code");
        }

        @Test
        @DisplayName("Should return FAIL_FAST for unknown code")
        void shouldReturnFailFastForUnknownCode() {
            assertEquals(ErrorHandlingConfig.ErrorStrategy.FAIL_FAST, 
                        ErrorHandlingConfig.ErrorStrategy.fromCode("unknown"));
            
            logger.info("[OK] FAIL_FAST returned for unknown code");
        }
    }

    @Nested
    @DisplayName("Constructor and Default Tests")
    class ConstructorAndDefaultTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            ErrorHandlingConfig config = new ErrorHandlingConfig();
            
            assertEquals("fail-fast", config.getStrategy());
            assertEquals(3, config.getMaxRetries());
            assertEquals(1000L, config.getRetryDelay());
            assertEquals(2.0, config.getRetryBackoffMultiplier());
            assertEquals(30000L, config.getMaxRetryDelay());
            assertFalse(config.getDeadLetterEnabled());
            assertTrue(config.getLogErrors());
            assertEquals("ERROR", config.getLogLevel());
            assertTrue(config.getIncludeStackTrace());
            assertFalse(config.getIncludeData());
            assertEquals(100, config.getMaxLoggedErrors());
            assertFalse(config.getReportErrors());
            assertFalse(config.getContinueOnBatchError());
            assertEquals(0.1, config.getMaxBatchErrorRate());
            assertEquals(1, config.getMinBatchSuccessCount());
            
            logger.info("[OK] All default values verified");
        }

        @Test
        @DisplayName("Should initialize empty collections")
        void shouldInitializeEmptyCollections() {
            ErrorHandlingConfig config = new ErrorHandlingConfig();
            
            assertNotNull(config.getDeadLetterProperties());
            assertTrue(config.getDeadLetterProperties().isEmpty());
            
            assertNotNull(config.getReportingHeaders());
            assertTrue(config.getReportingHeaders().isEmpty());
            
            assertNotNull(config.getCustomHandlerProperties());
            assertTrue(config.getCustomHandlerProperties().isEmpty());
            
            logger.info("[OK] Empty collections initialized");
        }
    }

    @Nested
    @DisplayName("Retry Configuration Tests")
    class RetryConfigurationTests {

        @Test
        @DisplayName("Should set and get max retries")
        void shouldSetAndGetMaxRetries() {
            config.setMaxRetries(5);
            assertEquals(5, config.getMaxRetries());
            
            logger.info("[OK] Max retries getter/setter works");
        }

        @Test
        @DisplayName("Should set and get retry delay")
        void shouldSetAndGetRetryDelay() {
            config.setRetryDelay(2000L);
            assertEquals(2000L, config.getRetryDelay());
            
            logger.info("[OK] Retry delay getter/setter works");
        }

        @Test
        @DisplayName("Should set and get retry backoff multiplier")
        void shouldSetAndGetRetryBackoffMultiplier() {
            config.setRetryBackoffMultiplier(1.5);
            assertEquals(1.5, config.getRetryBackoffMultiplier());
            
            logger.info("[OK] Retry backoff multiplier getter/setter works");
        }

        @Test
        @DisplayName("Should set and get max retry delay")
        void shouldSetAndGetMaxRetryDelay() {
            config.setMaxRetryDelay(60000L);
            assertEquals(60000L, config.getMaxRetryDelay());
            
            logger.info("[OK] Max retry delay getter/setter works");
        }
    }

    @Nested
    @DisplayName("Dead Letter Configuration Tests")
    class DeadLetterConfigurationTests {

        @Test
        @DisplayName("Should set and get dead letter enabled")
        void shouldSetAndGetDeadLetterEnabled() {
            config.setDeadLetterEnabled(true);
            assertTrue(config.getDeadLetterEnabled());
            
            logger.info("[OK] Dead letter enabled getter/setter works");
        }

        @Test
        @DisplayName("Should set and get dead letter table")
        void shouldSetAndGetDeadLetterTable() {
            config.setDeadLetterTable("error_queue");
            assertEquals("error_queue", config.getDeadLetterTable());
            
            logger.info("[OK] Dead letter table getter/setter works");
        }

        @Test
        @DisplayName("Should set and get dead letter topic")
        void shouldSetAndGetDeadLetterTopic() {
            config.setDeadLetterTopic("errors.dlq");
            assertEquals("errors.dlq", config.getDeadLetterTopic());
            
            logger.info("[OK] Dead letter topic getter/setter works");
        }

        @Test
        @DisplayName("Should set and get dead letter file")
        void shouldSetAndGetDeadLetterFile() {
            config.setDeadLetterFile("/var/log/dead_letters.json");
            assertEquals("/var/log/dead_letters.json", config.getDeadLetterFile());
            
            logger.info("[OK] Dead letter file getter/setter works");
        }

        @Test
        @DisplayName("Should set and get dead letter properties")
        void shouldSetAndGetDeadLetterProperties() {
            Map<String, Object> props = new HashMap<>();
            props.put("retention", "7d");
            
            config.setDeadLetterProperties(props);
            
            assertEquals("7d", config.getDeadLetterProperties().get("retention"));
            
            logger.info("[OK] Dead letter properties getter/setter works");
        }

        @Test
        @DisplayName("Should handle null dead letter properties")
        void shouldHandleNullDeadLetterProperties() {
            config.setDeadLetterProperties(null);
            
            assertNotNull(config.getDeadLetterProperties());
            assertTrue(config.getDeadLetterProperties().isEmpty());
            
            logger.info("[OK] Null dead letter properties handled");
        }
    }

    @Nested
    @DisplayName("Logging Configuration Tests")
    class LoggingConfigurationTests {

        @Test
        @DisplayName("Should set and get log errors")
        void shouldSetAndGetLogErrors() {
            config.setLogErrors(false);
            assertFalse(config.getLogErrors());
            
            logger.info("[OK] Log errors getter/setter works");
        }

        @Test
        @DisplayName("Should set and get log level")
        void shouldSetAndGetLogLevel() {
            config.setLogLevel("WARN");
            assertEquals("WARN", config.getLogLevel());
            
            logger.info("[OK] Log level getter/setter works");
        }

        @Test
        @DisplayName("Should set and get include stack trace")
        void shouldSetAndGetIncludeStackTrace() {
            config.setIncludeStackTrace(false);
            assertFalse(config.getIncludeStackTrace());
            
            logger.info("[OK] Include stack trace getter/setter works");
        }

        @Test
        @DisplayName("Should set and get include data")
        void shouldSetAndGetIncludeData() {
            config.setIncludeData(true);
            assertTrue(config.getIncludeData());
            
            logger.info("[OK] Include data getter/setter works");
        }

        @Test
        @DisplayName("Should set and get max logged errors")
        void shouldSetAndGetMaxLoggedErrors() {
            config.setMaxLoggedErrors(50);
            assertEquals(50, config.getMaxLoggedErrors());
            
            logger.info("[OK] Max logged errors getter/setter works");
        }
    }

    @Nested
    @DisplayName("Reporting Configuration Tests")
    class ReportingConfigurationTests {

        @Test
        @DisplayName("Should set and get report errors")
        void shouldSetAndGetReportErrors() {
            config.setReportErrors(true);
            assertTrue(config.getReportErrors());
            
            logger.info("[OK] Report errors getter/setter works");
        }

        @Test
        @DisplayName("Should set and get reporting endpoint")
        void shouldSetAndGetReportingEndpoint() {
            config.setReportingEndpoint("https://errors.example.com/api/v1");
            assertEquals("https://errors.example.com/api/v1", config.getReportingEndpoint());
            
            logger.info("[OK] Reporting endpoint getter/setter works");
        }

        @Test
        @DisplayName("Should set and get reporting topic")
        void shouldSetAndGetReportingTopic() {
            config.setReportingTopic("error-reports");
            assertEquals("error-reports", config.getReportingTopic());
            
            logger.info("[OK] Reporting topic getter/setter works");
        }

        @Test
        @DisplayName("Should set and get reporting headers")
        void shouldSetAndGetReportingHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer token");
            
            config.setReportingHeaders(headers);
            
            assertEquals("Bearer token", config.getReportingHeaders().get("Authorization"));
            
            logger.info("[OK] Reporting headers getter/setter works");
        }

        @Test
        @DisplayName("Should handle null reporting headers")
        void shouldHandleNullReportingHeaders() {
            config.setReportingHeaders(null);
            
            assertNotNull(config.getReportingHeaders());
            assertTrue(config.getReportingHeaders().isEmpty());
            
            logger.info("[OK] Null reporting headers handled");
        }
    }

    @Nested
    @DisplayName("Batch Error Handling Tests")
    class BatchErrorHandlingTests {

        @Test
        @DisplayName("Should set and get continue on batch error")
        void shouldSetAndGetContinueOnBatchError() {
            config.setContinueOnBatchError(true);
            assertTrue(config.getContinueOnBatchError());
            
            logger.info("[OK] Continue on batch error getter/setter works");
        }

        @Test
        @DisplayName("Should set and get max batch error rate")
        void shouldSetAndGetMaxBatchErrorRate() {
            config.setMaxBatchErrorRate(0.25);
            assertEquals(0.25, config.getMaxBatchErrorRate());
            
            logger.info("[OK] Max batch error rate getter/setter works");
        }

        @Test
        @DisplayName("Should set and get min batch success count")
        void shouldSetAndGetMinBatchSuccessCount() {
            config.setMinBatchSuccessCount(10);
            assertEquals(10, config.getMinBatchSuccessCount());
            
            logger.info("[OK] Min batch success count getter/setter works");
        }
    }

    @Nested
    @DisplayName("Calculate Retry Delay Tests")
    class CalculateRetryDelayTests {

        @Test
        @DisplayName("Should calculate first retry delay")
        void shouldCalculateFirstRetryDelay() {
            config.setRetryDelay(1000L);
            config.setRetryBackoffMultiplier(2.0);
            
            long delay = config.calculateRetryDelay(1);
            
            assertEquals(1000L, delay);
            
            logger.info("[OK] First retry delay calculated correctly");
        }

        @Test
        @DisplayName("Should calculate retry delay with backoff")
        void shouldCalculateRetryDelayWithBackoff() {
            config.setRetryDelay(1000L);
            config.setRetryBackoffMultiplier(2.0);
            config.setMaxRetryDelay(60000L);
            
            assertEquals(1000L, config.calculateRetryDelay(1));
            assertEquals(2000L, config.calculateRetryDelay(2));
            assertEquals(4000L, config.calculateRetryDelay(3));
            assertEquals(8000L, config.calculateRetryDelay(4));
            
            logger.info("[OK] Retry delay with backoff calculated correctly");
        }

        @Test
        @DisplayName("Should cap retry delay at max")
        void shouldCapRetryDelayAtMax() {
            config.setRetryDelay(1000L);
            config.setRetryBackoffMultiplier(2.0);
            config.setMaxRetryDelay(5000L);
            
            long delay = config.calculateRetryDelay(5); // Would be 16000 without cap
            
            assertEquals(5000L, delay);
            
            logger.info("[OK] Retry delay capped at maximum");
        }

        @Test
        @DisplayName("Should return zero for invalid attempt")
        void shouldReturnZeroForInvalidAttempt() {
            assertEquals(0, config.calculateRetryDelay(0));
            assertEquals(0, config.calculateRetryDelay(-1));
            
            logger.info("[OK] Zero returned for invalid attempt");
        }
    }

    @Nested
    @DisplayName("Should Retry Tests")
    class ShouldRetryTests {

        @Test
        @DisplayName("Should retry when under max retries")
        void shouldRetryWhenUnderMaxRetries() {
            config.setMaxRetries(3);
            
            assertTrue(config.shouldRetry("ERROR", 0));
            assertTrue(config.shouldRetry("ERROR", 1));
            assertTrue(config.shouldRetry("ERROR", 2));
            
            logger.info("[OK] Retry allowed when under max retries");
        }

        @Test
        @DisplayName("Should not retry when at max retries")
        void shouldNotRetryWhenAtMaxRetries() {
            config.setMaxRetries(3);
            
            assertFalse(config.shouldRetry("ERROR", 3));
            assertFalse(config.shouldRetry("ERROR", 4));
            
            logger.info("[OK] Retry not allowed when at max retries");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with default config")
        void shouldPassValidationWithDefaultConfig() {
            assertDoesNotThrow(() -> config.validate());
            
            logger.info("[OK] Default config passes validation");
        }

        @Test
        @DisplayName("Should fail validation with null strategy")
        void shouldFailValidationWithNullStrategy() {
            config.setStrategy(null);
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Null strategy fails validation");
        }

        @Test
        @DisplayName("Should fail validation with negative max retries")
        void shouldFailValidationWithNegativeMaxRetries() {
            config.setMaxRetries(-1);
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Negative max retries fails validation");
        }

        @Test
        @DisplayName("Should fail validation with negative retry delay")
        void shouldFailValidationWithNegativeRetryDelay() {
            config.setRetryDelay(-1L);
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Negative retry delay fails validation");
        }

        @Test
        @DisplayName("Should fail validation with non-positive backoff multiplier")
        void shouldFailValidationWithNonPositiveBackoffMultiplier() {
            config.setRetryBackoffMultiplier(0.0);
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Non-positive backoff multiplier fails validation");
        }

        @Test
        @DisplayName("Should fail validation with invalid batch error rate")
        void shouldFailValidationWithInvalidBatchErrorRate() {
            config.setMaxBatchErrorRate(1.5); // Greater than 1
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            config.setMaxBatchErrorRate(-0.1); // Less than 0
            
            assertThrows(IllegalArgumentException.class, () -> config.validate());
            
            logger.info("[OK] Invalid batch error rate fails validation");
        }
    }

    @Nested
    @DisplayName("Copy Tests")
    class CopyTests {

        @Test
        @DisplayName("Should create deep copy")
        void shouldCreateDeepCopy() {
            // Set up original
            config.setStrategy("log-and-continue");
            config.setMaxRetries(5);
            config.setRetryDelay(2000L);
            config.setDeadLetterEnabled(true);
            config.setDeadLetterTable("errors");
            
            Map<String, Object> props = new HashMap<>();
            props.put("retention", "7d");
            config.setDeadLetterProperties(props);
            
            // Create copy
            ErrorHandlingConfig copy = config.copy();
            
            // Verify copy has same values
            assertEquals("log-and-continue", copy.getStrategy());
            assertEquals(5, copy.getMaxRetries());
            assertEquals(2000L, copy.getRetryDelay());
            assertTrue(copy.getDeadLetterEnabled());
            assertEquals("errors", copy.getDeadLetterTable());
            assertEquals("7d", copy.getDeadLetterProperties().get("retention"));
            
            // Verify modifications to original don't affect copy
            config.setStrategy("fail-fast");
            assertEquals("log-and-continue", copy.getStrategy());
            
            config.getDeadLetterProperties().put("retention", "30d");
            assertEquals("7d", copy.getDeadLetterProperties().get("retention"));
            
            logger.info("[OK] Deep copy created correctly");
        }
    }

    @Nested
    @DisplayName("Custom Error Handler Tests")
    class CustomErrorHandlerTests {

        @Test
        @DisplayName("Should set and get custom error handler")
        void shouldSetAndGetCustomErrorHandler() {
            config.setCustomErrorHandler("com.example.MyErrorHandler");
            assertEquals("com.example.MyErrorHandler", config.getCustomErrorHandler());
            
            logger.info("[OK] Custom error handler getter/setter works");
        }

        @Test
        @DisplayName("Should set and get custom handler properties")
        void shouldSetAndGetCustomHandlerProperties() {
            Map<String, Object> props = new HashMap<>();
            props.put("timeout", 5000);
            
            config.setCustomHandlerProperties(props);
            
            assertEquals(5000, config.getCustomHandlerProperties().get("timeout"));
            
            logger.info("[OK] Custom handler properties getter/setter works");
        }

        @Test
        @DisplayName("Should handle null custom handler properties")
        void shouldHandleNullCustomHandlerProperties() {
            config.setCustomHandlerProperties(null);
            
            assertNotNull(config.getCustomHandlerProperties());
            assertTrue(config.getCustomHandlerProperties().isEmpty());
            
            logger.info("[OK] Null custom handler properties handled");
        }
    }
}
