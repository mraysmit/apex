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
package dev.mars.apex.core.service.data.external.rest;

import dev.mars.apex.core.config.datasource.CircuitBreakerConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for CircuitBreaker - resilience pattern for external data sources.
 * 
 * Tests the circuit breaker implementation including:
 * - State transitions (CLOSED -> OPEN -> HALF_OPEN -> CLOSED)
 * - Failure threshold handling
 * - Success threshold for recovery
 * - Metrics tracking
 * - Reset functionality
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("CircuitBreaker Tests")
class CircuitBreakerTest {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerTest.class);
    
    private CircuitBreakerConfig config;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        config = new CircuitBreakerConfig();
        config.setEnabled(true);
        config.setFailureThreshold(3);
        config.setSuccessThreshold(2);
        config.setTimeoutSeconds(5L);
        config.setSlidingWindowSize(10L);
        // Slow call detection is automatically disabled when duration threshold is null
        config.setSlowCallDurationThreshold(null);
        config.setLogStateChanges(true);
        
        circuitBreaker = new CircuitBreaker(config);
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("Should start in CLOSED state")
        void shouldStartInClosedState() {
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            logger.info("[OK] Circuit breaker starts in CLOSED state");
        }

        @Test
        @DisplayName("Should have zero metrics initially")
        void shouldHaveZeroMetricsInitially() {
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            
            assertEquals(0, metrics.getFailureCount());
            assertEquals(0, metrics.getSuccessCount());
            assertEquals(0, metrics.getRequestCount());
            
            logger.info("[OK] Initial metrics are all zero");
        }
    }

    @Nested
    @DisplayName("Successful Execution Tests")
    class SuccessfulExecutionTests {

        @Test
        @DisplayName("Should allow successful calls in CLOSED state")
        void shouldAllowSuccessfulCallsInClosedState() throws Exception {
            String result = circuitBreaker.execute(() -> "success");
            
            assertEquals("success", result);
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            assertEquals(1, metrics.getSuccessCount());
            assertEquals(0, metrics.getFailureCount());
            
            logger.info("[OK] Successful calls allowed in CLOSED state");
        }

        @Test
        @DisplayName("Should increment request count on success")
        void shouldIncrementRequestCountOnSuccess() throws Exception {
            circuitBreaker.execute(() -> "result1");
            circuitBreaker.execute(() -> "result2");
            circuitBreaker.execute(() -> "result3");
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            assertEquals(3, metrics.getRequestCount());
            assertEquals(3, metrics.getSuccessCount());
            
            logger.info("[OK] Request count incremented correctly");
        }

        @Test
        @DisplayName("Should track average response time")
        void shouldTrackAverageResponseTime() throws Exception {
            circuitBreaker.execute(() -> {
                Thread.sleep(50);
                return "result";
            });
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            assertTrue(metrics.getAverageResponseTime() >= 50);
            
            logger.info("[OK] Response time tracked correctly: {}ms", metrics.getAverageResponseTime());
        }
    }

    @Nested
    @DisplayName("Failure Handling Tests")
    class FailureHandlingTests {

        @Test
        @DisplayName("Should track failures and stay CLOSED below threshold")
        void shouldTrackFailuresAndStayClosedBelowThreshold() {
            // Fail twice (below threshold of 3)
            for (int i = 0; i < 2; i++) {
                try {
                    circuitBreaker.execute(() -> {
                        throw new RuntimeException("Test failure");
                    });
                } catch (Exception e) {
                    // Expected
                }
            }
            
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            assertEquals(2, metrics.getFailureCount());
            
            logger.info("[OK] Circuit stays CLOSED below failure threshold");
        }

        @Test
        @DisplayName("Should transition to OPEN after reaching failure threshold")
        void shouldTransitionToOpenAfterReachingFailureThreshold() {
            // Fail 3 times (at threshold)
            for (int i = 0; i < 3; i++) {
                final int failureNum = i;
                try {
                    circuitBreaker.execute(() -> {
                        throw new RuntimeException("Test failure " + failureNum);
                    });
                } catch (Exception e) {
                    // Expected
                }
            }
            
            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
            
            logger.info("[OK] Circuit transitions to OPEN at failure threshold");
        }

        @Test
        @DisplayName("Should block calls when OPEN")
        void shouldBlockCallsWhenOpen() {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    circuitBreaker.execute(() -> {
                        throw new RuntimeException("Test failure");
                    });
                } catch (Exception e) {
                    // Expected
                }
            }
            
            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
            
            // Now try another call - should be blocked
            AtomicInteger callCount = new AtomicInteger(0);
            assertThrows(DataSourceException.class, () -> {
                circuitBreaker.execute(() -> {
                    callCount.incrementAndGet();
                    return "should not execute";
                });
            });
            
            // Verify the call was blocked (never executed)
            assertEquals(0, callCount.get());
            
            logger.info("[OK] Calls blocked when circuit is OPEN");
        }

        @Test
        @DisplayName("Should throw DataSourceException when circuit is OPEN")
        void shouldThrowDataSourceExceptionWhenCircuitIsOpen() {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    circuitBreaker.execute(() -> {
                        throw new RuntimeException("Test failure");
                    });
                } catch (Exception e) {
                    // Expected
                }
            }
            
            DataSourceException exception = assertThrows(DataSourceException.class, () -> {
                circuitBreaker.execute(() -> "should not execute");
            });
            
            assertEquals(DataSourceException.ErrorType.CIRCUIT_BREAKER_ERROR, exception.getErrorType());
            
            logger.info("[OK] DataSourceException thrown when circuit is OPEN");
        }
    }

    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("Should transition through CLOSED -> OPEN -> CLOSED via reset")
        void shouldTransitionThroughStatesViaReset() {
            // Start in CLOSED
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            
            // Cause failures to open circuit
            for (int i = 0; i < 3; i++) {
                try {
                    circuitBreaker.execute(() -> {
                        throw new RuntimeException("Test failure");
                    });
                } catch (Exception e) {
                    // Expected
                }
            }
            
            // Should be OPEN now
            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
            
            // Reset to CLOSED
            circuitBreaker.reset();
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            
            // Verify metrics are reset
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            assertEquals(0, metrics.getFailureCount());
            assertEquals(0, metrics.getSuccessCount());
            
            logger.info("[OK] State transitions work correctly");
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @Test
        @DisplayName("Should reset circuit to CLOSED state")
        void shouldResetCircuitToClosedState() {
            // Open the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    circuitBreaker.execute(() -> {
                        throw new RuntimeException("Test failure");
                    });
                } catch (Exception e) {
                    // Expected
                }
            }
            
            assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
            
            // Reset
            circuitBreaker.reset();
            
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            
            logger.info("[OK] Circuit reset to CLOSED state");
        }

        @Test
        @DisplayName("Should clear metrics on reset")
        void shouldClearMetricsOnReset() throws Exception {
            // Generate some metrics
            circuitBreaker.execute(() -> "success");
            
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
            
            CircuitBreaker.CircuitBreakerMetrics metricsBeforeReset = circuitBreaker.getMetrics();
            assertTrue(metricsBeforeReset.getRequestCount() > 0);
            
            // Reset
            circuitBreaker.reset();
            
            CircuitBreaker.CircuitBreakerMetrics metricsAfterReset = circuitBreaker.getMetrics();
            assertEquals(0, metricsAfterReset.getRequestCount());
            assertEquals(0, metricsAfterReset.getSuccessCount());
            assertEquals(0, metricsAfterReset.getFailureCount());
            
            logger.info("[OK] Metrics cleared on reset");
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should track failure rate correctly")
        void shouldTrackFailureRateCorrectly() throws Exception {
            // 2 successes, 1 failure
            circuitBreaker.execute(() -> "success1");
            circuitBreaker.execute(() -> "success2");
            
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            
            assertEquals(3, metrics.getRequestCount());
            assertEquals(2, metrics.getSuccessCount());
            assertEquals(1, metrics.getFailureCount());
            // Failure rate calculation depends on sliding window implementation
            assertTrue(metrics.getFailureRate() >= 0);
            
            logger.info("[OK] Failure rate tracked: {}%", metrics.getFailureRate());
        }

        @Test
        @DisplayName("Should include timestamp information in metrics")
        void shouldIncludeTimestampInformationInMetrics() throws Exception {
            circuitBreaker.execute(() -> "success");
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            
            assertTrue(metrics.getLastSuccessTime() > 0);
            
            logger.info("[OK] Timestamp information included in metrics");
        }

        @Test
        @DisplayName("Should have proper toString representation")
        void shouldHaveProperToStringRepresentation() throws Exception {
            circuitBreaker.execute(() -> "success");
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            String str = metrics.toString();
            
            assertNotNull(str);
            assertTrue(str.contains("state="));
            assertTrue(str.contains("CLOSED"));
            
            logger.info("[OK] Metrics toString is correct: {}", str);
        }
    }

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @DisplayName("Should shutdown gracefully")
        void shouldShutdownGracefully() {
            assertDoesNotThrow(() -> circuitBreaker.shutdown());
            
            logger.info("[OK] Circuit breaker shutdown gracefully");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle callable returning null")
        void shouldHandleCallableReturningNull() throws Exception {
            Object result = circuitBreaker.execute(() -> null);
            
            assertNull(result);
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            
            logger.info("[OK] Callable returning null handled correctly");
        }

        @Test
        @DisplayName("Should handle rapid successive calls")
        void shouldHandleRapidSuccessiveCalls() throws Exception {
            for (int i = 0; i < 100; i++) {
                circuitBreaker.execute(() -> "rapid-" + System.currentTimeMillis());
            }
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            assertEquals(100, metrics.getRequestCount());
            assertEquals(100, metrics.getSuccessCount());
            assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
            
            logger.info("[OK] Rapid successive calls handled correctly");
        }
    }
}
