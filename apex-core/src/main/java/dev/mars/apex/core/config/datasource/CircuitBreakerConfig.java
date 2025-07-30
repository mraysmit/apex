package dev.mars.apex.core.config.datasource;

import java.util.Objects;

/**
 * Configuration class for circuit breaker settings.
 * 
 * This class contains circuit breaker-related configuration for implementing
 * the circuit breaker pattern to handle external service failures gracefully.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class CircuitBreakerConfig {
    
    /**
     * Enumeration of circuit breaker states.
     */
    public enum State {
        CLOSED("closed", "Circuit is closed, requests are allowed"),
        OPEN("open", "Circuit is open, requests are blocked"),
        HALF_OPEN("half-open", "Circuit is half-open, testing if service is recovered");
        
        private final String code;
        private final String description;
        
        State(String code, String description) {
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
    
    private Boolean enabled = true;
    private Integer failureThreshold = 5; // Number of failures before opening circuit
    private Long timeoutSeconds = 60L; // Time to wait before trying half-open
    private Integer successThreshold = 3; // Number of successes needed to close circuit
    private Integer requestVolumeThreshold = 10; // Minimum requests before evaluating failure rate
    private Double failureRateThreshold = 50.0; // Failure rate percentage to open circuit
    private Long slidingWindowSize = 100L; // Size of sliding window for failure rate calculation
    private String fallbackResponse; // Default response when circuit is open
    private Boolean logStateChanges = true;
    private Boolean metricsEnabled = true;
    
    // Advanced configuration
    private Long slowCallDurationThreshold = 5000L; // Calls slower than this are considered failures
    private Double slowCallRateThreshold = 50.0; // Slow call rate percentage to open circuit
    private Boolean automaticTransitionFromOpenToHalfOpen = true;
    private Integer maxWaitDurationInHalfOpen = 30; // Max wait time in half-open state (seconds)
    
    /**
     * Default constructor with sensible defaults.
     */
    public CircuitBreakerConfig() {
        // Defaults are set in field declarations
    }
    
    /**
     * Constructor with basic circuit breaker configuration.
     * 
     * @param enabled Whether circuit breaker is enabled
     * @param failureThreshold Number of failures before opening circuit
     * @param timeoutSeconds Timeout before trying half-open
     */
    public CircuitBreakerConfig(Boolean enabled, Integer failureThreshold, Long timeoutSeconds) {
        this.enabled = enabled;
        this.failureThreshold = failureThreshold;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    // Basic configuration
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    
    public Integer getFailureThreshold() {
        return failureThreshold;
    }
    
    public void setFailureThreshold(Integer failureThreshold) {
        this.failureThreshold = failureThreshold;
    }
    
    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public Integer getSuccessThreshold() {
        return successThreshold;
    }
    
    public void setSuccessThreshold(Integer successThreshold) {
        this.successThreshold = successThreshold;
    }
    
    public Integer getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }
    
    public void setRequestVolumeThreshold(Integer requestVolumeThreshold) {
        this.requestVolumeThreshold = requestVolumeThreshold;
    }
    
    public Double getFailureRateThreshold() {
        return failureRateThreshold;
    }
    
    public void setFailureRateThreshold(Double failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }
    
    public Long getSlidingWindowSize() {
        return slidingWindowSize;
    }
    
    public void setSlidingWindowSize(Long slidingWindowSize) {
        this.slidingWindowSize = slidingWindowSize;
    }
    
    public String getFallbackResponse() {
        return fallbackResponse;
    }
    
    public void setFallbackResponse(String fallbackResponse) {
        this.fallbackResponse = fallbackResponse;
    }
    
    public Boolean getLogStateChanges() {
        return logStateChanges;
    }
    
    public void setLogStateChanges(Boolean logStateChanges) {
        this.logStateChanges = logStateChanges;
    }
    
    public boolean shouldLogStateChanges() {
        return logStateChanges != null && logStateChanges;
    }
    
    public Boolean getMetricsEnabled() {
        return metricsEnabled;
    }
    
    public void setMetricsEnabled(Boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
    
    public boolean isMetricsEnabled() {
        return metricsEnabled != null && metricsEnabled;
    }
    
    // Advanced configuration
    
    public Long getSlowCallDurationThreshold() {
        return slowCallDurationThreshold;
    }
    
    public void setSlowCallDurationThreshold(Long slowCallDurationThreshold) {
        this.slowCallDurationThreshold = slowCallDurationThreshold;
    }
    
    public Double getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }
    
    public void setSlowCallRateThreshold(Double slowCallRateThreshold) {
        this.slowCallRateThreshold = slowCallRateThreshold;
    }
    
    public Boolean getAutomaticTransitionFromOpenToHalfOpen() {
        return automaticTransitionFromOpenToHalfOpen;
    }
    
    public void setAutomaticTransitionFromOpenToHalfOpen(Boolean automaticTransitionFromOpenToHalfOpen) {
        this.automaticTransitionFromOpenToHalfOpen = automaticTransitionFromOpenToHalfOpen;
    }
    
    public boolean isAutomaticTransitionEnabled() {
        return automaticTransitionFromOpenToHalfOpen != null && automaticTransitionFromOpenToHalfOpen;
    }
    
    public Integer getMaxWaitDurationInHalfOpen() {
        return maxWaitDurationInHalfOpen;
    }
    
    public void setMaxWaitDurationInHalfOpen(Integer maxWaitDurationInHalfOpen) {
        this.maxWaitDurationInHalfOpen = maxWaitDurationInHalfOpen;
    }
    
    // Utility methods
    
    /**
     * Get timeout in milliseconds.
     * 
     * @return Timeout in milliseconds
     */
    public long getTimeoutMilliseconds() {
        return timeoutSeconds != null ? timeoutSeconds * 1000L : 0L;
    }
    
    /**
     * Get max wait duration in half-open state in milliseconds.
     * 
     * @return Max wait duration in milliseconds
     */
    public long getMaxWaitDurationInHalfOpenMilliseconds() {
        return maxWaitDurationInHalfOpen != null ? maxWaitDurationInHalfOpen * 1000L : 0L;
    }
    
    /**
     * Check if slow call detection is enabled.
     * 
     * @return true if slow call detection is enabled
     */
    public boolean isSlowCallDetectionEnabled() {
        return slowCallDurationThreshold != null && slowCallDurationThreshold > 0 &&
               slowCallRateThreshold != null && slowCallRateThreshold > 0;
    }
    
    // Validation
    
    /**
     * Validate the circuit breaker configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (failureThreshold != null && failureThreshold <= 0) {
            throw new IllegalArgumentException("Failure threshold must be positive");
        }
        
        if (timeoutSeconds != null && timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        
        if (successThreshold != null && successThreshold <= 0) {
            throw new IllegalArgumentException("Success threshold must be positive");
        }
        
        if (requestVolumeThreshold != null && requestVolumeThreshold <= 0) {
            throw new IllegalArgumentException("Request volume threshold must be positive");
        }
        
        if (failureRateThreshold != null && (failureRateThreshold <= 0 || failureRateThreshold > 100)) {
            throw new IllegalArgumentException("Failure rate threshold must be between 0 and 100");
        }
        
        if (slidingWindowSize != null && slidingWindowSize <= 0) {
            throw new IllegalArgumentException("Sliding window size must be positive");
        }
        
        if (slowCallDurationThreshold != null && slowCallDurationThreshold <= 0) {
            throw new IllegalArgumentException("Slow call duration threshold must be positive");
        }
        
        if (slowCallRateThreshold != null && (slowCallRateThreshold <= 0 || slowCallRateThreshold > 100)) {
            throw new IllegalArgumentException("Slow call rate threshold must be between 0 and 100");
        }
        
        if (maxWaitDurationInHalfOpen != null && maxWaitDurationInHalfOpen <= 0) {
            throw new IllegalArgumentException("Max wait duration in half-open must be positive");
        }
    }
    
    /**
     * Create a copy of this circuit breaker configuration.
     * 
     * @return A new CircuitBreakerConfig with the same settings
     */
    public CircuitBreakerConfig copy() {
        CircuitBreakerConfig copy = new CircuitBreakerConfig();
        copy.enabled = this.enabled;
        copy.failureThreshold = this.failureThreshold;
        copy.timeoutSeconds = this.timeoutSeconds;
        copy.successThreshold = this.successThreshold;
        copy.requestVolumeThreshold = this.requestVolumeThreshold;
        copy.failureRateThreshold = this.failureRateThreshold;
        copy.slidingWindowSize = this.slidingWindowSize;
        copy.fallbackResponse = this.fallbackResponse;
        copy.logStateChanges = this.logStateChanges;
        copy.metricsEnabled = this.metricsEnabled;
        copy.slowCallDurationThreshold = this.slowCallDurationThreshold;
        copy.slowCallRateThreshold = this.slowCallRateThreshold;
        copy.automaticTransitionFromOpenToHalfOpen = this.automaticTransitionFromOpenToHalfOpen;
        copy.maxWaitDurationInHalfOpen = this.maxWaitDurationInHalfOpen;
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircuitBreakerConfig that = (CircuitBreakerConfig) o;
        return Objects.equals(enabled, that.enabled) &&
               Objects.equals(failureThreshold, that.failureThreshold) &&
               Objects.equals(timeoutSeconds, that.timeoutSeconds) &&
               Objects.equals(successThreshold, that.successThreshold);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enabled, failureThreshold, timeoutSeconds, successThreshold);
    }
    
    @Override
    public String toString() {
        return "CircuitBreakerConfig{" +
               "enabled=" + enabled +
               ", failureThreshold=" + failureThreshold +
               ", timeoutSeconds=" + timeoutSeconds +
               ", successThreshold=" + successThreshold +
               ", failureRateThreshold=" + failureRateThreshold +
               '}';
    }
}
