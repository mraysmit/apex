package dev.mars.apex.core.service.data.external.rest;

import dev.mars.apex.core.config.datasource.CircuitBreakerConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker implementation for external data sources.
 * 
 * This class implements the circuit breaker pattern to provide resilience
 * when calling external services. It prevents cascading failures by
 * temporarily blocking calls to failing services.
 * 
 * States:
 * - CLOSED: Normal operation, calls are allowed
 * - OPEN: Service is failing, calls are blocked
 * - HALF_OPEN: Testing if service has recovered
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class CircuitBreaker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreaker.class);
    
    /**
     * Circuit breaker states.
     */
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Blocking calls due to failures
        HALF_OPEN  // Testing recovery
    }
    
    private final CircuitBreakerConfig config;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong lastSuccessTime = new AtomicLong(0);
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicInteger slowCallCount = new AtomicInteger(0);
    
    // Sliding window for failure rate calculation
    private final SlidingWindow slidingWindow;
    
    /**
     * Constructor with circuit breaker configuration.
     * 
     * @param config The circuit breaker configuration
     */
    public CircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
        this.slidingWindow = new SlidingWindow(config.getSlidingWindowSize().intValue());
        
        LOGGER.info("Circuit breaker initialized with failure threshold: {}, timeout: {}s", 
            config.getFailureThreshold(), config.getTimeoutSeconds());
    }
    
    /**
     * Execute a callable with circuit breaker protection.
     * 
     * @param callable The operation to execute
     * @param <T> The return type
     * @return The result of the callable
     * @throws Exception if the operation fails or circuit is open
     */
    public <T> T execute(Callable<T> callable) throws Exception {
        // Check if circuit is open
        if (state.get() == State.OPEN) {
            if (shouldAttemptReset()) {
                transitionToHalfOpen();
            } else {
                throw new DataSourceException(DataSourceException.ErrorType.CIRCUIT_BREAKER_ERROR,
                    "Circuit breaker is OPEN", null, null, "execute", true);
            }
        }
        
        long startTime = System.currentTimeMillis();

        try {
            T result = callable.call();
            onSuccess(System.currentTimeMillis() - startTime);
            return result;
            
        } catch (Exception e) {
            onFailure(System.currentTimeMillis() - startTime);
            throw e;
        }
    }
    
    /**
     * Handle successful operation.
     */
    private void onSuccess(long responseTime) {
        successCount.incrementAndGet();
        lastSuccessTime.set(System.currentTimeMillis());
        requestCount.incrementAndGet();
        totalResponseTime.addAndGet(responseTime);
        
        // Check for slow calls
        if (config.isSlowCallDetectionEnabled() && 
            responseTime > config.getSlowCallDurationThreshold()) {
            slowCallCount.incrementAndGet();
        }
        
        slidingWindow.recordSuccess();
        
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            // Check if we have enough successes to close the circuit
            if (successCount.get() >= config.getSuccessThreshold()) {
                transitionToClosed();
            }
        } else if (currentState == State.CLOSED) {
            // Reset failure count on success
            failureCount.set(0);
        }
        
        if (config.shouldLogStateChanges()) {
            LOGGER.debug("Circuit breaker success - State: {}, Successes: {}, Failures: {}", 
                currentState, successCount.get(), failureCount.get());
        }
    }
    
    /**
     * Handle failed operation.
     */
    private void onFailure(long responseTime) {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        requestCount.incrementAndGet();
        totalResponseTime.addAndGet(responseTime);
        
        slidingWindow.recordFailure();
        
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            // Any failure in half-open state should open the circuit
            transitionToOpen();
        } else if (currentState == State.CLOSED) {
            // Check if we should open the circuit
            if (shouldOpenCircuit()) {
                transitionToOpen();
            }
        }
        
        if (config.shouldLogStateChanges()) {
            LOGGER.debug("Circuit breaker failure - State: {}, Successes: {}, Failures: {}", 
                currentState, successCount.get(), failureCount.get());
        }
    }
    
    /**
     * Check if the circuit should be opened.
     */
    private boolean shouldOpenCircuit() {
        int failures = failureCount.get();
        int requests = requestCount.get();
        
        // Check failure threshold
        if (failures >= config.getFailureThreshold()) {
            return true;
        }
        
        // Check request volume threshold
        if (requests < config.getRequestVolumeThreshold()) {
            return false;
        }
        
        // Check failure rate
        double failureRate = slidingWindow.getFailureRate();
        if (failureRate >= config.getFailureRateThreshold()) {
            return true;
        }
        
        // Check slow call rate
        if (config.isSlowCallDetectionEnabled()) {
            double slowCallRate = (slowCallCount.get() * 100.0) / requests;
            if (slowCallRate >= config.getSlowCallRateThreshold()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if we should attempt to reset the circuit from OPEN to HALF_OPEN.
     */
    private boolean shouldAttemptReset() {
        if (!config.isAutomaticTransitionEnabled()) {
            return false;
        }
        
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        return timeSinceLastFailure >= config.getTimeoutMilliseconds();
    }
    
    /**
     * Transition to CLOSED state.
     */
    private void transitionToClosed() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        requestCount.set(0);
        totalResponseTime.set(0);
        slowCallCount.set(0);
        slidingWindow.reset();
        
        if (config.shouldLogStateChanges()) {
            LOGGER.info("Circuit breaker transitioned to CLOSED state");
        }
    }
    
    /**
     * Transition to OPEN state.
     */
    private void transitionToOpen() {
        state.set(State.OPEN);
        successCount.set(0);
        
        if (config.shouldLogStateChanges()) {
            LOGGER.warn("Circuit breaker transitioned to OPEN state - Failures: {}, Failure rate: {:.2f}%", 
                failureCount.get(), slidingWindow.getFailureRate());
        }
    }
    
    /**
     * Transition to HALF_OPEN state.
     */
    private void transitionToHalfOpen() {
        state.set(State.HALF_OPEN);
        successCount.set(0);
        
        if (config.shouldLogStateChanges()) {
            LOGGER.info("Circuit breaker transitioned to HALF_OPEN state");
        }
    }
    
    /**
     * Get current circuit breaker state.
     * 
     * @return Current state
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Get circuit breaker metrics.
     * 
     * @return Metrics information
     */
    public CircuitBreakerMetrics getMetrics() {
        return new CircuitBreakerMetrics(
            state.get(),
            failureCount.get(),
            successCount.get(),
            requestCount.get(),
            slidingWindow.getFailureRate(),
            getAverageResponseTime(),
            lastFailureTime.get(),
            lastSuccessTime.get()
        );
    }
    
    /**
     * Reset the circuit breaker to CLOSED state.
     */
    public void reset() {
        transitionToClosed();
        LOGGER.info("Circuit breaker manually reset to CLOSED state");
    }
    
    /**
     * Shutdown the circuit breaker.
     */
    public void shutdown() {
        // Nothing specific to shutdown for now
        LOGGER.info("Circuit breaker shut down");
    }
    
    /**
     * Get average response time.
     */
    private double getAverageResponseTime() {
        int requests = requestCount.get();
        if (requests == 0) {
            return 0.0;
        }
        return (double) totalResponseTime.get() / requests;
    }
    
    /**
     * Simple sliding window implementation for failure rate calculation.
     */
    private static class SlidingWindow {
        private final boolean[] window;
        private int index = 0;
        private int totalRequests = 0;
        private int failures = 0;
        
        public SlidingWindow(int size) {
            this.window = new boolean[size];
        }
        
        public synchronized void recordSuccess() {
            record(true);
        }
        
        public synchronized void recordFailure() {
            record(false);
        }
        
        private void record(boolean success) {
            // Remove old value if window is full
            if (totalRequests >= window.length) {
                if (!window[index]) {
                    failures--;
                }
            } else {
                totalRequests++;
            }
            
            // Add new value
            window[index] = success;
            if (!success) {
                failures++;
            }
            
            // Move to next position
            index = (index + 1) % window.length;
        }
        
        public synchronized double getFailureRate() {
            if (totalRequests == 0) {
                return 0.0;
            }
            return (failures * 100.0) / Math.min(totalRequests, window.length);
        }
        
        public synchronized void reset() {
            index = 0;
            totalRequests = 0;
            failures = 0;
            for (int i = 0; i < window.length; i++) {
                window[i] = false;
            }
        }
    }
    
    /**
     * Circuit breaker metrics holder.
     */
    public static class CircuitBreakerMetrics {
        private final State state;
        private final int failureCount;
        private final int successCount;
        private final int requestCount;
        private final double failureRate;
        private final double averageResponseTime;
        private final long lastFailureTime;
        private final long lastSuccessTime;
        
        public CircuitBreakerMetrics(State state, int failureCount, int successCount, 
                                   int requestCount, double failureRate, double averageResponseTime,
                                   long lastFailureTime, long lastSuccessTime) {
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.requestCount = requestCount;
            this.failureRate = failureRate;
            this.averageResponseTime = averageResponseTime;
            this.lastFailureTime = lastFailureTime;
            this.lastSuccessTime = lastSuccessTime;
        }
        
        // Getters
        public State getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public int getRequestCount() { return requestCount; }
        public double getFailureRate() { return failureRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public long getLastFailureTime() { return lastFailureTime; }
        public long getLastSuccessTime() { return lastSuccessTime; }
        
        @Override
        public String toString() {
            return "CircuitBreakerMetrics{" +
                   "state=" + state +
                   ", failureCount=" + failureCount +
                   ", successCount=" + successCount +
                   ", requestCount=" + requestCount +
                   ", failureRate=" + String.format("%.2f%%", failureRate) +
                   ", averageResponseTime=" + String.format("%.2fms", averageResponseTime) +
                   '}';
        }
    }
}
