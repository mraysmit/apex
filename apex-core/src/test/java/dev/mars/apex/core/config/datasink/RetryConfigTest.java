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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for RetryConfig - configuration for retry mechanisms in data sinks.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("RetryConfig Tests")
class RetryConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(RetryConfigTest.class);

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should create with sensible defaults")
        void shouldCreateWithSensibleDefaults() {
            RetryConfig config = new RetryConfig();
            
            assertTrue(config.getEnabled(), "Retry should be enabled by default");
            assertEquals("exponential-backoff", config.getStrategy());
            assertEquals(3, config.getMaxAttempts());
            assertEquals(1000L, config.getInitialDelay());
            assertEquals(30000L, config.getMaxDelay());
            assertEquals(2.0, config.getBackoffMultiplier());
            assertEquals(0.1, config.getJitterFactor());
            
            logger.info("[OK] Default values are sensible");
        }

        @Test
        @DisplayName("Should return correct RetryStrategy enum from code")
        void shouldReturnCorrectRetryStrategyFromCode() {
            assertEquals(RetryConfig.RetryStrategy.EXPONENTIAL_BACKOFF, 
                        RetryConfig.RetryStrategy.fromCode("exponential-backoff"));
            assertEquals(RetryConfig.RetryStrategy.FIXED_DELAY, 
                        RetryConfig.RetryStrategy.fromCode("fixed-delay"));
            assertEquals(RetryConfig.RetryStrategy.LINEAR_BACKOFF, 
                        RetryConfig.RetryStrategy.fromCode("linear-backoff"));
            assertEquals(RetryConfig.RetryStrategy.NONE, 
                        RetryConfig.RetryStrategy.fromCode("none"));
            assertEquals(RetryConfig.RetryStrategy.CUSTOM, 
                        RetryConfig.RetryStrategy.fromCode("custom"));
            
            logger.info("[OK] RetryStrategy enum resolves correctly from codes");
        }

        @Test
        @DisplayName("Should default to exponential backoff for unknown strategies")
        void shouldDefaultToExponentialBackoffForUnknown() {
            assertEquals(RetryConfig.RetryStrategy.EXPONENTIAL_BACKOFF, 
                        RetryConfig.RetryStrategy.fromCode("unknown-strategy"));
            assertEquals(RetryConfig.RetryStrategy.EXPONENTIAL_BACKOFF, 
                        RetryConfig.RetryStrategy.fromCode(null));
            
            logger.info("[OK] Unknown strategies default to EXPONENTIAL_BACKOFF");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create with basic parameters")
        void shouldCreateWithBasicParameters() {
            RetryConfig config = new RetryConfig(true, 5, 2000L);
            
            assertTrue(config.getEnabled());
            assertEquals(5, config.getMaxAttempts());
            assertEquals(2000L, config.getInitialDelay());
            
            logger.info("[OK] Basic parameter constructor works");
        }

        @Test
        @DisplayName("Should create disabled retry config")
        void shouldCreateDisabledRetryConfig() {
            RetryConfig config = new RetryConfig(false, 0, 0L);
            
            assertFalse(config.getEnabled());
            assertEquals(0, config.getMaxAttempts());
            
            logger.info("[OK] Disabled retry config created");
        }
    }

    @Nested
    @DisplayName("Setters and Getters Tests")
    class SettersAndGettersTests {

        @Test
        @DisplayName("Should set and get all basic properties")
        void shouldSetAndGetAllBasicProperties() {
            RetryConfig config = new RetryConfig();
            
            config.setEnabled(false);
            assertFalse(config.getEnabled());
            
            config.setStrategy("fixed-delay");
            assertEquals("fixed-delay", config.getStrategy());
            assertEquals(RetryConfig.RetryStrategy.FIXED_DELAY, config.getRetryStrategy());
            
            config.setMaxAttempts(10);
            assertEquals(10, config.getMaxAttempts());
            
            config.setInitialDelay(500L);
            assertEquals(500L, config.getInitialDelay());
            
            config.setMaxDelay(60000L);
            assertEquals(60000L, config.getMaxDelay());
            
            config.setBackoffMultiplier(3.0);
            assertEquals(3.0, config.getBackoffMultiplier());
            
            config.setJitterFactor(0.2);
            assertEquals(0.2, config.getJitterFactor());
            
            logger.info("[OK] All basic properties set and retrieved correctly");
        }

        @Test
        @DisplayName("Should set and get retryable exceptions")
        void shouldSetAndGetRetryableExceptions() {
            RetryConfig config = new RetryConfig();
            List<String> exceptions = Arrays.asList(
                "java.io.IOException", 
                "java.net.SocketTimeoutException"
            );
            
            config.setRetryableExceptions(exceptions);
            assertEquals(exceptions, config.getRetryableExceptions());
            
            logger.info("[OK] Retryable exceptions configured correctly");
        }

        @Test
        @DisplayName("Should set and get non-retryable exceptions")
        void shouldSetAndGetNonRetryableExceptions() {
            RetryConfig config = new RetryConfig();
            List<String> exceptions = Arrays.asList(
                "java.lang.NullPointerException",
                "java.lang.IllegalArgumentException"
            );
            
            config.setNonRetryableExceptions(exceptions);
            assertEquals(exceptions, config.getNonRetryableExceptions());
            
            logger.info("[OK] Non-retryable exceptions configured correctly");
        }

        @Test
        @DisplayName("Should set and get HTTP codes")
        void shouldSetAndGetHttpCodes() {
            RetryConfig config = new RetryConfig();
            
            List<Integer> retryableCodes = Arrays.asList(500, 502, 503, 504);
            config.setRetryableHttpCodes(retryableCodes);
            assertEquals(retryableCodes, config.getRetryableHttpCodes());
            
            List<Integer> nonRetryableCodes = Arrays.asList(400, 401, 403, 404);
            config.setNonRetryableHttpCodes(nonRetryableCodes);
            assertEquals(nonRetryableCodes, config.getNonRetryableHttpCodes());
            
            logger.info("[OK] HTTP codes configured correctly");
        }

        @Test
        @DisplayName("Should set and get retry conditions map")
        void shouldSetAndGetRetryConditions() {
            RetryConfig config = new RetryConfig();
            Map<String, String> conditions = new HashMap<>();
            conditions.put("timeout", "retry");
            conditions.put("connection_refused", "retry_with_backoff");
            
            config.setRetryConditions(conditions);
            assertEquals(conditions, config.getRetryConditions());
            
            logger.info("[OK] Retry conditions map configured correctly");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Integration Tests")
    class CircuitBreakerIntegrationTests {

        @Test
        @DisplayName("Should configure circuit breaker settings")
        void shouldConfigureCircuitBreakerSettings() {
            RetryConfig config = new RetryConfig();
            
            config.setCircuitBreakerEnabled(true);
            assertTrue(config.getCircuitBreakerEnabled());
            
            config.setCircuitBreakerThreshold(10);
            assertEquals(10, config.getCircuitBreakerThreshold());
            
            config.setCircuitBreakerTimeout(120000L);
            assertEquals(120000L, config.getCircuitBreakerTimeout());
            
            config.setCircuitBreakerSuccessThreshold(5);
            assertEquals(5, config.getCircuitBreakerSuccessThreshold());
            
            logger.info("[OK] Circuit breaker settings configured correctly");
        }

        @Test
        @DisplayName("Should have circuit breaker disabled by default")
        void shouldHaveCircuitBreakerDisabledByDefault() {
            RetryConfig config = new RetryConfig();
            assertFalse(config.getCircuitBreakerEnabled());
            
            logger.info("[OK] Circuit breaker disabled by default");
        }
    }

    @Nested
    @DisplayName("Retry Limits Tests")
    class RetryLimitsTests {

        @Test
        @DisplayName("Should configure retry limits")
        void shouldConfigureRetryLimits() {
            RetryConfig config = new RetryConfig();
            
            config.setTotalRetryTimeout(600000L);
            assertEquals(600000L, config.getTotalRetryTimeout());
            
            config.setMaxRetriesPerMinute(20);
            assertEquals(20, config.getMaxRetriesPerMinute());
            
            config.setMaxRetriesPerHour(200);
            assertEquals(200, config.getMaxRetriesPerHour());
            
            logger.info("[OK] Retry limits configured correctly");
        }

        @Test
        @DisplayName("Should have sensible default retry limits")
        void shouldHaveSensibleDefaultRetryLimits() {
            RetryConfig config = new RetryConfig();
            
            assertEquals(300000L, config.getTotalRetryTimeout()); // 5 minutes
            assertEquals(10, config.getMaxRetriesPerMinute());
            assertEquals(100, config.getMaxRetriesPerHour());
            
            logger.info("[OK] Default retry limits are sensible");
        }
    }

    @Nested
    @DisplayName("Monitoring and Logging Tests")
    class MonitoringAndLoggingTests {

        @Test
        @DisplayName("Should configure monitoring options")
        void shouldConfigureMonitoringOptions() {
            RetryConfig config = new RetryConfig();
            
            config.setLogRetries(false);
            assertFalse(config.getLogRetries());
            
            config.setLogLevel("ERROR");
            assertEquals("ERROR", config.getLogLevel());
            
            config.setIncludeStackTrace(true);
            assertTrue(config.getIncludeStackTrace());
            
            config.setEnableMetrics(false);
            assertFalse(config.getEnableMetrics());
            
            logger.info("[OK] Monitoring options configured correctly");
        }

        @Test
        @DisplayName("Should have logging enabled by default")
        void shouldHaveLoggingEnabledByDefault() {
            RetryConfig config = new RetryConfig();
            
            assertTrue(config.getLogRetries());
            assertEquals("WARN", config.getLogLevel());
            assertFalse(config.getIncludeStackTrace());
            assertTrue(config.getEnableMetrics());
            
            logger.info("[OK] Default logging settings are correct");
        }
    }

    @Nested
    @DisplayName("Custom Handler Tests")
    class CustomHandlerTests {

        @Test
        @DisplayName("Should configure custom retry handler")
        void shouldConfigureCustomRetryHandler() {
            RetryConfig config = new RetryConfig();
            
            config.setCustomRetryHandler("com.example.CustomRetryHandler");
            assertEquals("com.example.CustomRetryHandler", config.getCustomRetryHandler());
            
            Map<String, Object> props = new HashMap<>();
            props.put("maxBackoff", 5000);
            props.put("useJitter", true);
            config.setCustomHandlerProperties(props);
            assertEquals(props, config.getCustomHandlerProperties());
            
            logger.info("[OK] Custom handler configured correctly");
        }
    }

    @Nested
    @DisplayName("Retry Queue Tests")
    class RetryQueueTests {

        @Test
        @DisplayName("Should configure retry queue settings")
        void shouldConfigureRetryQueueSettings() {
            RetryConfig config = new RetryConfig();
            
            config.setEnableRetryQueue(true);
            assertTrue(config.getEnableRetryQueue());
            
            config.setRetryQueueName("retry-queue-1");
            assertEquals("retry-queue-1", config.getRetryQueueName());
            
            config.setRetryQueueSize(5000);
            assertEquals(5000, config.getRetryQueueSize());
            
            config.setRetryQueueTimeout(120000L);
            assertEquals(120000L, config.getRetryQueueTimeout());
            
            logger.info("[OK] Retry queue configured correctly");
        }

        @Test
        @DisplayName("Should have retry queue disabled by default")
        void shouldHaveRetryQueueDisabledByDefault() {
            RetryConfig config = new RetryConfig();
            
            assertFalse(config.getEnableRetryQueue());
            assertEquals(1000, config.getRetryQueueSize());
            assertEquals(60000L, config.getRetryQueueTimeout());
            
            logger.info("[OK] Retry queue defaults are correct");
        }
    }

    @Nested
    @DisplayName("RetryStrategy Enum Tests")
    class RetryStrategyEnumTests {

        @Test
        @DisplayName("Should have correct codes and descriptions")
        void shouldHaveCorrectCodesAndDescriptions() {
            assertEquals("none", RetryConfig.RetryStrategy.NONE.getCode());
            assertEquals("No retry attempts", RetryConfig.RetryStrategy.NONE.getDescription());
            
            assertEquals("fixed-delay", RetryConfig.RetryStrategy.FIXED_DELAY.getCode());
            assertEquals("Fixed delay between retries", RetryConfig.RetryStrategy.FIXED_DELAY.getDescription());
            
            assertEquals("exponential-backoff", RetryConfig.RetryStrategy.EXPONENTIAL_BACKOFF.getCode());
            assertEquals("Exponential backoff with jitter", RetryConfig.RetryStrategy.EXPONENTIAL_BACKOFF.getDescription());
            
            assertEquals("linear-backoff", RetryConfig.RetryStrategy.LINEAR_BACKOFF.getCode());
            assertEquals("Linear increase in delay", RetryConfig.RetryStrategy.LINEAR_BACKOFF.getDescription());
            
            assertEquals("custom", RetryConfig.RetryStrategy.CUSTOM.getCode());
            assertEquals("Custom retry strategy", RetryConfig.RetryStrategy.CUSTOM.getDescription());
            
            logger.info("[OK] All strategy codes and descriptions are correct");
        }

        @Test
        @DisplayName("Should be case insensitive for code lookup")
        void shouldBeCaseInsensitiveForCodeLookup() {
            assertEquals(RetryConfig.RetryStrategy.EXPONENTIAL_BACKOFF, 
                        RetryConfig.RetryStrategy.fromCode("EXPONENTIAL-BACKOFF"));
            assertEquals(RetryConfig.RetryStrategy.EXPONENTIAL_BACKOFF, 
                        RetryConfig.RetryStrategy.fromCode("Exponential-Backoff"));
            assertEquals(RetryConfig.RetryStrategy.FIXED_DELAY, 
                        RetryConfig.RetryStrategy.fromCode("FIXED-DELAY"));
            
            logger.info("[OK] Strategy code lookup is case insensitive");
        }
    }
}
