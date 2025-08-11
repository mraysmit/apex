package dev.mars.apex.core.config.datasource;

import java.util.Objects;

/**
 * Configuration class for health check settings.
 * 
 * This class contains health check-related configuration including
 * check intervals, timeout settings, and health check queries or endpoints.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class HealthCheckConfig {
    
    private Boolean enabled = true;
    private Long intervalSeconds = 60L; // 1 minute default
    private Long timeoutSeconds = 10L; // 10 seconds default
    private Integer retryAttempts = 3;
    private Long retryDelay = 1000L; // 1 second default
    private String query; // For database health checks
    private String endpoint; // For REST API health checks
    private String expectedResponse; // Expected response for validation
    private Integer failureThreshold = 3; // Number of consecutive failures before marking unhealthy
    private Integer successThreshold = 1; // Number of consecutive successes to mark healthy again
    private Boolean logFailures = true;
    private Boolean alertOnFailure = false;
    private String alertEndpoint; // Endpoint to send alerts to
    
    // Circuit breaker integration
    private Boolean circuitBreakerIntegration = false;
    private Integer circuitBreakerFailureThreshold = 5;
    private Long circuitBreakerTimeoutSeconds = 60L;
    
    /**
     * Default constructor with sensible defaults.
     */
    public HealthCheckConfig() {
        // Defaults are set in field declarations
    }
    
    /**
     * Constructor with basic health check configuration.
     * 
     * @param enabled Whether health checks are enabled
     * @param intervalSeconds Interval between health checks in seconds
     * @param timeoutSeconds Timeout for health checks in seconds
     */
    public HealthCheckConfig(Boolean enabled, Long intervalSeconds, Long timeoutSeconds) {
        this.enabled = enabled;
        this.intervalSeconds = intervalSeconds;
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
    
    public Long getIntervalSeconds() {
        return intervalSeconds;
    }
    
    public void setIntervalSeconds(Long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }
    
    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public Integer getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(Integer retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    public Long getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(Long retryDelay) {
        this.retryDelay = retryDelay;
    }
    
    // Health check specifics
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getExpectedResponse() {
        return expectedResponse;
    }
    
    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }
    
    // Failure handling
    
    public Integer getFailureThreshold() {
        return failureThreshold;
    }
    
    public void setFailureThreshold(Integer failureThreshold) {
        this.failureThreshold = failureThreshold;
    }
    
    public Integer getSuccessThreshold() {
        return successThreshold;
    }
    
    public void setSuccessThreshold(Integer successThreshold) {
        this.successThreshold = successThreshold;
    }
    
    public Boolean getLogFailures() {
        return logFailures;
    }
    
    public void setLogFailures(Boolean logFailures) {
        this.logFailures = logFailures;
    }
    
    public boolean shouldLogFailures() {
        return logFailures != null && logFailures;
    }
    
    public Boolean getAlertOnFailure() {
        return alertOnFailure;
    }
    
    public void setAlertOnFailure(Boolean alertOnFailure) {
        this.alertOnFailure = alertOnFailure;
    }
    
    public boolean shouldAlertOnFailure() {
        return alertOnFailure != null && alertOnFailure;
    }
    
    public String getAlertEndpoint() {
        return alertEndpoint;
    }
    
    public void setAlertEndpoint(String alertEndpoint) {
        this.alertEndpoint = alertEndpoint;
    }
    
    // Circuit breaker integration
    
    public Boolean getCircuitBreakerIntegration() {
        return circuitBreakerIntegration;
    }
    
    public void setCircuitBreakerIntegration(Boolean circuitBreakerIntegration) {
        this.circuitBreakerIntegration = circuitBreakerIntegration;
    }
    
    public boolean isCircuitBreakerIntegrationEnabled() {
        return circuitBreakerIntegration != null && circuitBreakerIntegration;
    }
    
    public Integer getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }
    
    public void setCircuitBreakerFailureThreshold(Integer circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
    }
    
    public Long getCircuitBreakerTimeoutSeconds() {
        return circuitBreakerTimeoutSeconds;
    }
    
    public void setCircuitBreakerTimeoutSeconds(Long circuitBreakerTimeoutSeconds) {
        this.circuitBreakerTimeoutSeconds = circuitBreakerTimeoutSeconds;
    }
    
    // Utility methods
    
    /**
     * Get interval in milliseconds.
     * 
     * @return Interval in milliseconds
     */
    public long getIntervalMilliseconds() {
        return intervalSeconds != null ? intervalSeconds * 1000L : 0L;
    }
    
    /**
     * Get timeout in milliseconds.
     * 
     * @return Timeout in milliseconds
     */
    public long getTimeoutMilliseconds() {
        return timeoutSeconds != null ? timeoutSeconds * 1000L : 0L;
    }
    
    /**
     * Get circuit breaker timeout in milliseconds.
     * 
     * @return Circuit breaker timeout in milliseconds
     */
    public long getCircuitBreakerTimeoutMilliseconds() {
        return circuitBreakerTimeoutSeconds != null ? circuitBreakerTimeoutSeconds * 1000L : 0L;
    }
    
    // Validation
    
    /**
     * Validate the health check configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (intervalSeconds != null && intervalSeconds <= 0) {
            throw new IllegalArgumentException("Health check interval must be positive");
        }
        
        if (timeoutSeconds != null && timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Health check timeout must be positive");
        }
        
        if (retryAttempts != null && retryAttempts < 0) {
            throw new IllegalArgumentException("Retry attempts cannot be negative");
        }
        
        if (retryDelay != null && retryDelay < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
        
        if (failureThreshold != null && failureThreshold <= 0) {
            throw new IllegalArgumentException("Failure threshold must be positive");
        }
        
        if (successThreshold != null && successThreshold <= 0) {
            throw new IllegalArgumentException("Success threshold must be positive");
        }
        
        if (circuitBreakerFailureThreshold != null && circuitBreakerFailureThreshold <= 0) {
            throw new IllegalArgumentException("Circuit breaker failure threshold must be positive");
        }
        
        if (circuitBreakerTimeoutSeconds != null && circuitBreakerTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Circuit breaker timeout must be positive");
        }
        
        // Validate that alert endpoint is provided if alerting is enabled
        if (shouldAlertOnFailure() && (alertEndpoint == null || alertEndpoint.trim().isEmpty())) {
            throw new IllegalArgumentException("Alert endpoint is required when alert on failure is enabled");
        }
    }
    
    /**
     * Create a copy of this health check configuration.
     * 
     * @return A new HealthCheckConfig with the same settings
     */
    public HealthCheckConfig copy() {
        HealthCheckConfig copy = new HealthCheckConfig();
        copy.enabled = this.enabled;
        copy.intervalSeconds = this.intervalSeconds;
        copy.timeoutSeconds = this.timeoutSeconds;
        copy.retryAttempts = this.retryAttempts;
        copy.retryDelay = this.retryDelay;
        copy.query = this.query;
        copy.endpoint = this.endpoint;
        copy.expectedResponse = this.expectedResponse;
        copy.failureThreshold = this.failureThreshold;
        copy.successThreshold = this.successThreshold;
        copy.logFailures = this.logFailures;
        copy.alertOnFailure = this.alertOnFailure;
        copy.alertEndpoint = this.alertEndpoint;
        copy.circuitBreakerIntegration = this.circuitBreakerIntegration;
        copy.circuitBreakerFailureThreshold = this.circuitBreakerFailureThreshold;
        copy.circuitBreakerTimeoutSeconds = this.circuitBreakerTimeoutSeconds;
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthCheckConfig that = (HealthCheckConfig) o;
        return Objects.equals(enabled, that.enabled) &&
               Objects.equals(intervalSeconds, that.intervalSeconds) &&
               Objects.equals(timeoutSeconds, that.timeoutSeconds) &&
               Objects.equals(query, that.query) &&
               Objects.equals(endpoint, that.endpoint);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enabled, intervalSeconds, timeoutSeconds, query, endpoint);
    }
    
    @Override
    public String toString() {
        return "HealthCheckConfig{" +
               "enabled=" + enabled +
               ", intervalSeconds=" + intervalSeconds +
               ", timeoutSeconds=" + timeoutSeconds +
               ", query='" + query + '\'' +
               ", endpoint='" + endpoint + '\'' +
               ", failureThreshold=" + failureThreshold +
               ", successThreshold=" + successThreshold +
               '}';
    }
}
