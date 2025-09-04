package dev.mars.apex.core.config.datasink;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for retry mechanisms in data sinks.
 * 
 * This class defines how retry operations should be handled,
 * including retry strategies, backoff algorithms, and retry conditions.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class RetryConfig {
    
    /**
     * Enumeration of retry strategies.
     */
    public enum RetryStrategy {
        NONE("none", "No retry attempts"),
        FIXED_DELAY("fixed-delay", "Fixed delay between retries"),
        EXPONENTIAL_BACKOFF("exponential-backoff", "Exponential backoff with jitter"),
        LINEAR_BACKOFF("linear-backoff", "Linear increase in delay"),
        CUSTOM("custom", "Custom retry strategy");
        
        private final String code;
        private final String description;
        
        RetryStrategy(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static RetryStrategy fromCode(String code) {
            if (code == null) {
                return EXPONENTIAL_BACKOFF; // Default
            }
            
            for (RetryStrategy strategy : values()) {
                if (strategy.code.equalsIgnoreCase(code)) {
                    return strategy;
                }
            }
            
            return EXPONENTIAL_BACKOFF;
        }
    }
    
    private Boolean enabled = true;
    private String strategy = "exponential-backoff";
    private Integer maxAttempts = 3;
    private Long initialDelay = 1000L; // 1 second
    private Long maxDelay = 30000L; // 30 seconds
    private Double backoffMultiplier = 2.0;
    private Double jitterFactor = 0.1; // 10% jitter
    
    // Retry conditions
    private List<String> retryableExceptions;
    private List<String> nonRetryableExceptions;
    private List<Integer> retryableHttpCodes;
    private List<Integer> nonRetryableHttpCodes;
    private Map<String, String> retryConditions; // condition -> action
    
    // Circuit breaker integration
    private Boolean circuitBreakerEnabled = false;
    private Integer circuitBreakerThreshold = 5;
    private Long circuitBreakerTimeout = 60000L; // 1 minute
    private Integer circuitBreakerSuccessThreshold = 3;
    
    // Retry limits
    private Long totalRetryTimeout = 300000L; // 5 minutes total
    private Integer maxRetriesPerMinute = 10;
    private Integer maxRetriesPerHour = 100;
    
    // Monitoring and logging
    private Boolean logRetries = true;
    private String logLevel = "WARN";
    private Boolean includeStackTrace = false;
    private Boolean enableMetrics = true;
    
    // Custom retry handler
    private String customRetryHandler;
    private Map<String, Object> customHandlerProperties;
    
    // Retry queue configuration
    private Boolean enableRetryQueue = false;
    private String retryQueueName;
    private Integer retryQueueSize = 1000;
    private Long retryQueueTimeout = 60000L; // 1 minute
    
    /**
     * Default constructor.
     */
    public RetryConfig() {
        this.retryConditions = new HashMap<>();
        this.customHandlerProperties = new HashMap<>();
    }
    
    /**
     * Constructor with basic retry configuration.
     * 
     * @param enabled Whether retries are enabled
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelay Initial delay between retries in milliseconds
     */
    public RetryConfig(Boolean enabled, Integer maxAttempts, Long initialDelay) {
        this();
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
    }
    
    // Getters and setters
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    public RetryStrategy getRetryStrategy() {
        return RetryStrategy.fromCode(strategy);
    }
    
    public Integer getMaxAttempts() {
        return maxAttempts;
    }
    
    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public Long getInitialDelay() {
        return initialDelay;
    }
    
    public void setInitialDelay(Long initialDelay) {
        this.initialDelay = initialDelay;
    }
    
    public Long getMaxDelay() {
        return maxDelay;
    }
    
    public void setMaxDelay(Long maxDelay) {
        this.maxDelay = maxDelay;
    }
    
    public Double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public void setBackoffMultiplier(Double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }
    
    public Double getJitterFactor() {
        return jitterFactor;
    }
    
    public void setJitterFactor(Double jitterFactor) {
        this.jitterFactor = jitterFactor;
    }
    
    public List<String> getRetryableExceptions() {
        return retryableExceptions;
    }
    
    public void setRetryableExceptions(List<String> retryableExceptions) {
        this.retryableExceptions = retryableExceptions;
    }
    
    public List<String> getNonRetryableExceptions() {
        return nonRetryableExceptions;
    }
    
    public void setNonRetryableExceptions(List<String> nonRetryableExceptions) {
        this.nonRetryableExceptions = nonRetryableExceptions;
    }
    
    public List<Integer> getRetryableHttpCodes() {
        return retryableHttpCodes;
    }
    
    public void setRetryableHttpCodes(List<Integer> retryableHttpCodes) {
        this.retryableHttpCodes = retryableHttpCodes;
    }
    
    public List<Integer> getNonRetryableHttpCodes() {
        return nonRetryableHttpCodes;
    }
    
    public void setNonRetryableHttpCodes(List<Integer> nonRetryableHttpCodes) {
        this.nonRetryableHttpCodes = nonRetryableHttpCodes;
    }
    
    public Map<String, String> getRetryConditions() {
        return retryConditions;
    }
    
    public void setRetryConditions(Map<String, String> retryConditions) {
        this.retryConditions = retryConditions != null ? retryConditions : new HashMap<>();
    }
    
    public Boolean getCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }
    
    public void setCircuitBreakerEnabled(Boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }
    
    public Integer getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }
    
    public void setCircuitBreakerThreshold(Integer circuitBreakerThreshold) {
        this.circuitBreakerThreshold = circuitBreakerThreshold;
    }
    
    public Long getCircuitBreakerTimeout() {
        return circuitBreakerTimeout;
    }
    
    public void setCircuitBreakerTimeout(Long circuitBreakerTimeout) {
        this.circuitBreakerTimeout = circuitBreakerTimeout;
    }
    
    public Integer getCircuitBreakerSuccessThreshold() {
        return circuitBreakerSuccessThreshold;
    }
    
    public void setCircuitBreakerSuccessThreshold(Integer circuitBreakerSuccessThreshold) {
        this.circuitBreakerSuccessThreshold = circuitBreakerSuccessThreshold;
    }
    
    public Long getTotalRetryTimeout() {
        return totalRetryTimeout;
    }
    
    public void setTotalRetryTimeout(Long totalRetryTimeout) {
        this.totalRetryTimeout = totalRetryTimeout;
    }
    
    public Integer getMaxRetriesPerMinute() {
        return maxRetriesPerMinute;
    }
    
    public void setMaxRetriesPerMinute(Integer maxRetriesPerMinute) {
        this.maxRetriesPerMinute = maxRetriesPerMinute;
    }
    
    public Integer getMaxRetriesPerHour() {
        return maxRetriesPerHour;
    }
    
    public void setMaxRetriesPerHour(Integer maxRetriesPerHour) {
        this.maxRetriesPerHour = maxRetriesPerHour;
    }
    
    public Boolean getLogRetries() {
        return logRetries;
    }
    
    public void setLogRetries(Boolean logRetries) {
        this.logRetries = logRetries;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
    
    public Boolean getIncludeStackTrace() {
        return includeStackTrace;
    }
    
    public void setIncludeStackTrace(Boolean includeStackTrace) {
        this.includeStackTrace = includeStackTrace;
    }
    
    public Boolean getEnableMetrics() {
        return enableMetrics;
    }
    
    public void setEnableMetrics(Boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public String getCustomRetryHandler() {
        return customRetryHandler;
    }
    
    public void setCustomRetryHandler(String customRetryHandler) {
        this.customRetryHandler = customRetryHandler;
    }
    
    public Map<String, Object> getCustomHandlerProperties() {
        return customHandlerProperties;
    }
    
    public void setCustomHandlerProperties(Map<String, Object> customHandlerProperties) {
        this.customHandlerProperties = customHandlerProperties != null ? customHandlerProperties : new HashMap<>();
    }
    
    public Boolean getEnableRetryQueue() {
        return enableRetryQueue;
    }
    
    public void setEnableRetryQueue(Boolean enableRetryQueue) {
        this.enableRetryQueue = enableRetryQueue;
    }
    
    public String getRetryQueueName() {
        return retryQueueName;
    }
    
    public void setRetryQueueName(String retryQueueName) {
        this.retryQueueName = retryQueueName;
    }
    
    public Integer getRetryQueueSize() {
        return retryQueueSize;
    }
    
    public void setRetryQueueSize(Integer retryQueueSize) {
        this.retryQueueSize = retryQueueSize;
    }
    
    public Long getRetryQueueTimeout() {
        return retryQueueTimeout;
    }
    
    public void setRetryQueueTimeout(Long retryQueueTimeout) {
        this.retryQueueTimeout = retryQueueTimeout;
    }
    
    /**
     * Calculate the retry delay for a given attempt.
     * 
     * @param attempt The retry attempt number (1-based)
     * @return The delay in milliseconds
     */
    public long calculateRetryDelay(int attempt) {
        if (attempt <= 0) {
            return 0;
        }
        
        long delay;
        RetryStrategy retryStrategy = getRetryStrategy();
        
        switch (retryStrategy) {
            case FIXED_DELAY:
                delay = initialDelay;
                break;
                
            case EXPONENTIAL_BACKOFF:
                delay = (long) (initialDelay * Math.pow(backoffMultiplier, attempt - 1));
                break;
                
            case LINEAR_BACKOFF:
                delay = initialDelay * attempt;
                break;
                
            case NONE:
                return 0;
                
            case CUSTOM:
            default:
                delay = initialDelay;
                break;
        }
        
        // Apply jitter
        if (jitterFactor > 0) {
            double jitter = 1.0 + (Math.random() - 0.5) * 2 * jitterFactor;
            delay = (long) (delay * jitter);
        }
        
        // Ensure delay doesn't exceed maximum
        return Math.min(delay, maxDelay);
    }
    
    /**
     * Check if an exception should be retried.
     * 
     * @param exception The exception that occurred
     * @param attempt The current attempt number
     * @return true if the exception should be retried
     */
    public boolean shouldRetryException(Throwable exception, int attempt) {
        if (!enabled || attempt >= maxAttempts) {
            return false;
        }
        
        String exceptionType = exception.getClass().getSimpleName();
        
        // Check non-retryable exceptions first
        if (nonRetryableExceptions != null && nonRetryableExceptions.contains(exceptionType)) {
            return false;
        }
        
        // Check retryable exceptions
        if (retryableExceptions != null && !retryableExceptions.contains(exceptionType)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if an HTTP status code should be retried.
     * 
     * @param statusCode The HTTP status code
     * @param attempt The current attempt number
     * @return true if the status code should be retried
     */
    public boolean shouldRetryHttpCode(int statusCode, int attempt) {
        if (!enabled || attempt >= maxAttempts) {
            return false;
        }
        
        // Check non-retryable codes first
        if (nonRetryableHttpCodes != null && nonRetryableHttpCodes.contains(statusCode)) {
            return false;
        }
        
        // Check retryable codes
        if (retryableHttpCodes != null && !retryableHttpCodes.contains(statusCode)) {
            return false;
        }
        
        // Default retryable HTTP codes (5xx server errors and some 4xx)
        return statusCode >= 500 || statusCode == 408 || statusCode == 429;
    }
    
    /**
     * Validate the retry configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (maxAttempts != null && maxAttempts < 0) {
            throw new IllegalArgumentException("Max attempts cannot be negative");
        }
        
        if (initialDelay != null && initialDelay < 0) {
            throw new IllegalArgumentException("Initial delay cannot be negative");
        }
        
        if (maxDelay != null && maxDelay < 0) {
            throw new IllegalArgumentException("Max delay cannot be negative");
        }
        
        if (backoffMultiplier != null && backoffMultiplier <= 0) {
            throw new IllegalArgumentException("Backoff multiplier must be positive");
        }
        
        if (jitterFactor != null && (jitterFactor < 0 || jitterFactor > 1)) {
            throw new IllegalArgumentException("Jitter factor must be between 0 and 1");
        }
        
        if (totalRetryTimeout != null && totalRetryTimeout < 0) {
            throw new IllegalArgumentException("Total retry timeout cannot be negative");
        }
    }
    
    /**
     * Create a copy of this configuration.
     * 
     * @return A new RetryConfig with the same settings
     */
    public RetryConfig copy() {
        RetryConfig copy = new RetryConfig();
        copy.enabled = this.enabled;
        copy.strategy = this.strategy;
        copy.maxAttempts = this.maxAttempts;
        copy.initialDelay = this.initialDelay;
        copy.maxDelay = this.maxDelay;
        copy.backoffMultiplier = this.backoffMultiplier;
        copy.jitterFactor = this.jitterFactor;
        
        copy.retryableExceptions = this.retryableExceptions != null ? List.copyOf(this.retryableExceptions) : null;
        copy.nonRetryableExceptions = this.nonRetryableExceptions != null ? List.copyOf(this.nonRetryableExceptions) : null;
        copy.retryableHttpCodes = this.retryableHttpCodes != null ? List.copyOf(this.retryableHttpCodes) : null;
        copy.nonRetryableHttpCodes = this.nonRetryableHttpCodes != null ? List.copyOf(this.nonRetryableHttpCodes) : null;
        copy.retryConditions = new HashMap<>(this.retryConditions);
        
        copy.circuitBreakerEnabled = this.circuitBreakerEnabled;
        copy.circuitBreakerThreshold = this.circuitBreakerThreshold;
        copy.circuitBreakerTimeout = this.circuitBreakerTimeout;
        copy.circuitBreakerSuccessThreshold = this.circuitBreakerSuccessThreshold;
        
        copy.totalRetryTimeout = this.totalRetryTimeout;
        copy.maxRetriesPerMinute = this.maxRetriesPerMinute;
        copy.maxRetriesPerHour = this.maxRetriesPerHour;
        
        copy.logRetries = this.logRetries;
        copy.logLevel = this.logLevel;
        copy.includeStackTrace = this.includeStackTrace;
        copy.enableMetrics = this.enableMetrics;
        
        copy.customRetryHandler = this.customRetryHandler;
        copy.customHandlerProperties = new HashMap<>(this.customHandlerProperties);
        
        copy.enableRetryQueue = this.enableRetryQueue;
        copy.retryQueueName = this.retryQueueName;
        copy.retryQueueSize = this.retryQueueSize;
        copy.retryQueueTimeout = this.retryQueueTimeout;
        
        return copy;
    }
}
