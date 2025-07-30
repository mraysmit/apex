package dev.mars.apex.core.config.datasource;

import java.util.Objects;

/**
 * Configuration class for connection pooling settings.
 * 
 * This class contains settings for managing connection pools,
 * including pool size limits, timeouts, and validation parameters.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class ConnectionPoolConfig {
    
    private Integer minSize = 5;
    private Integer maxSize = 20;
    private Integer initialSize = 5;
    private Long connectionTimeout = 30000L; // 30 seconds
    private Long idleTimeout = 600000L; // 10 minutes
    private Long maxLifetime = 1800000L; // 30 minutes
    private Long leakDetectionThreshold = 0L; // Disabled by default
    private String connectionTestQuery = "SELECT 1";
    private Boolean testOnBorrow = true;
    private Boolean testOnReturn = false;
    private Boolean testWhileIdle = true;
    private Long validationInterval = 30000L; // 30 seconds
    private Integer maxRetries = 3;
    private Long retryDelay = 1000L; // 1 second
    
    /**
     * Default constructor with sensible defaults.
     */
    public ConnectionPoolConfig() {
        // Defaults are set in field declarations
    }
    
    /**
     * Constructor with basic pool size configuration.
     * 
     * @param minSize Minimum pool size
     * @param maxSize Maximum pool size
     */
    public ConnectionPoolConfig(Integer minSize, Integer maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.initialSize = minSize;
    }
    
    // Pool size configuration
    
    public Integer getMinSize() {
        return minSize;
    }
    
    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }
    
    public Integer getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }
    
    public Integer getInitialSize() {
        return initialSize;
    }
    
    public void setInitialSize(Integer initialSize) {
        this.initialSize = initialSize;
    }
    
    // Timeout configuration
    
    public Long getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public Long getIdleTimeout() {
        return idleTimeout;
    }
    
    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }
    
    public Long getMaxLifetime() {
        return maxLifetime;
    }
    
    public void setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }
    
    public Long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }
    
    public void setLeakDetectionThreshold(Long leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }
    
    // Validation configuration
    
    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }
    
    public void setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
    }
    
    public Boolean getTestOnBorrow() {
        return testOnBorrow;
    }
    
    public void setTestOnBorrow(Boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }
    
    public Boolean getTestOnReturn() {
        return testOnReturn;
    }
    
    public void setTestOnReturn(Boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }
    
    public Boolean getTestWhileIdle() {
        return testWhileIdle;
    }
    
    public void setTestWhileIdle(Boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }
    
    public Long getValidationInterval() {
        return validationInterval;
    }
    
    public void setValidationInterval(Long validationInterval) {
        this.validationInterval = validationInterval;
    }
    
    // Retry configuration
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Long getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(Long retryDelay) {
        this.retryDelay = retryDelay;
    }
    
    // Validation methods
    
    /**
     * Validate the connection pool configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (minSize != null && minSize < 0) {
            throw new IllegalArgumentException("Minimum pool size cannot be negative");
        }
        
        if (maxSize != null && maxSize <= 0) {
            throw new IllegalArgumentException("Maximum pool size must be positive");
        }
        
        if (minSize != null && maxSize != null && minSize > maxSize) {
            throw new IllegalArgumentException("Minimum pool size cannot be greater than maximum pool size");
        }
        
        if (initialSize != null && initialSize < 0) {
            throw new IllegalArgumentException("Initial pool size cannot be negative");
        }
        
        if (initialSize != null && maxSize != null && initialSize > maxSize) {
            throw new IllegalArgumentException("Initial pool size cannot be greater than maximum pool size");
        }
        
        if (connectionTimeout != null && connectionTimeout <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        
        if (idleTimeout != null && idleTimeout <= 0) {
            throw new IllegalArgumentException("Idle timeout must be positive");
        }
        
        if (maxLifetime != null && maxLifetime <= 0) {
            throw new IllegalArgumentException("Max lifetime must be positive");
        }
        
        if (validationInterval != null && validationInterval <= 0) {
            throw new IllegalArgumentException("Validation interval must be positive");
        }
        
        if (maxRetries != null && maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        
        if (retryDelay != null && retryDelay < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
    }
    
    /**
     * Check if connection validation is enabled.
     * 
     * @return true if any validation is enabled
     */
    public boolean isValidationEnabled() {
        return (testOnBorrow != null && testOnBorrow) ||
               (testOnReturn != null && testOnReturn) ||
               (testWhileIdle != null && testWhileIdle);
    }
    
    /**
     * Check if leak detection is enabled.
     * 
     * @return true if leak detection is enabled
     */
    public boolean isLeakDetectionEnabled() {
        return leakDetectionThreshold != null && leakDetectionThreshold > 0;
    }
    
    /**
     * Create a copy of this connection pool configuration.
     * 
     * @return A new ConnectionPoolConfig with the same settings
     */
    public ConnectionPoolConfig copy() {
        ConnectionPoolConfig copy = new ConnectionPoolConfig();
        copy.minSize = this.minSize;
        copy.maxSize = this.maxSize;
        copy.initialSize = this.initialSize;
        copy.connectionTimeout = this.connectionTimeout;
        copy.idleTimeout = this.idleTimeout;
        copy.maxLifetime = this.maxLifetime;
        copy.leakDetectionThreshold = this.leakDetectionThreshold;
        copy.connectionTestQuery = this.connectionTestQuery;
        copy.testOnBorrow = this.testOnBorrow;
        copy.testOnReturn = this.testOnReturn;
        copy.testWhileIdle = this.testWhileIdle;
        copy.validationInterval = this.validationInterval;
        copy.maxRetries = this.maxRetries;
        copy.retryDelay = this.retryDelay;
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionPoolConfig that = (ConnectionPoolConfig) o;
        return Objects.equals(minSize, that.minSize) &&
               Objects.equals(maxSize, that.maxSize) &&
               Objects.equals(initialSize, that.initialSize) &&
               Objects.equals(connectionTimeout, that.connectionTimeout) &&
               Objects.equals(idleTimeout, that.idleTimeout) &&
               Objects.equals(maxLifetime, that.maxLifetime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(minSize, maxSize, initialSize, connectionTimeout, idleTimeout, maxLifetime);
    }
    
    @Override
    public String toString() {
        return "ConnectionPoolConfig{" +
               "minSize=" + minSize +
               ", maxSize=" + maxSize +
               ", initialSize=" + initialSize +
               ", connectionTimeout=" + connectionTimeout +
               ", idleTimeout=" + idleTimeout +
               ", maxLifetime=" + maxLifetime +
               ", validationEnabled=" + isValidationEnabled() +
               ", leakDetectionEnabled=" + isLeakDetectionEnabled() +
               '}';
    }
}
