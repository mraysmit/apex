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
 * Configuration for error handling in data sinks.
 * 
 * This class defines how errors should be handled during write operations,
 * including retry strategies, dead letter handling, and error reporting.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class ErrorHandlingConfig {
    
    /**
     * Enumeration of error handling strategies.
     */
    public enum ErrorStrategy {
        FAIL_FAST("fail-fast", "Stop processing on first error"),
        LOG_AND_CONTINUE("log-and-continue", "Log error and continue processing"),
        DEAD_LETTER("dead-letter", "Send failed records to dead letter queue/table"),
        RETRY_AND_FAIL("retry-and-fail", "Retry failed operations, then fail"),
        RETRY_AND_CONTINUE("retry-and-continue", "Retry failed operations, then continue"),
        CUSTOM("custom", "Use custom error handler");
        
        private final String code;
        private final String description;
        
        ErrorStrategy(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static ErrorStrategy fromCode(String code) {
            if (code == null) {
                return FAIL_FAST; // Default
            }
            
            for (ErrorStrategy strategy : values()) {
                if (strategy.code.equalsIgnoreCase(code)) {
                    return strategy;
                }
            }
            
            return FAIL_FAST;
        }
    }
    
    private String strategy = "fail-fast";
    private Integer maxRetries = 3;
    private Long retryDelay = 1000L; // milliseconds
    private Double retryBackoffMultiplier = 2.0;
    private Long maxRetryDelay = 30000L; // 30 seconds
    
    // Dead letter configuration
    private Boolean deadLetterEnabled = false;
    private String deadLetterTable;
    private String deadLetterTopic;
    private String deadLetterFile;
    private Map<String, Object> deadLetterProperties;
    
    // Error logging configuration
    private Boolean logErrors = true;
    private String logLevel = "ERROR";
    private Boolean includeStackTrace = true;
    private Boolean includeData = false; // For security reasons
    private Integer maxLoggedErrors = 100;
    
    // Error reporting configuration
    private Boolean reportErrors = false;
    private String reportingEndpoint;
    private String reportingTopic;
    private Map<String, String> reportingHeaders;
    
    // Batch error handling
    private Boolean continueOnBatchError = false;
    private Double maxBatchErrorRate = 0.1; // 10% error rate threshold
    private Integer minBatchSuccessCount = 1;
    
    // Circuit breaker integration
    private Boolean circuitBreakerEnabled = false;
    private Integer circuitBreakerThreshold = 5;
    private Long circuitBreakerTimeout = 60000L; // 1 minute
    
    // Custom error handler
    private String customErrorHandler;
    private Map<String, Object> customHandlerProperties;
    
    // Error classification
    private List<String> retryableErrors;
    private List<String> nonRetryableErrors;
    private Map<String, String> errorMappings; // error type -> handling strategy
    
    /**
     * Default constructor.
     */
    public ErrorHandlingConfig() {
        this.deadLetterProperties = new HashMap<>();
        this.reportingHeaders = new HashMap<>();
        this.customHandlerProperties = new HashMap<>();
        this.errorMappings = new HashMap<>();
    }
    
    // Getters and setters
    
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    public ErrorStrategy getErrorStrategy() {
        return ErrorStrategy.fromCode(strategy);
    }
    
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
    
    public Double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }
    
    public void setRetryBackoffMultiplier(Double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
    }
    
    public Long getMaxRetryDelay() {
        return maxRetryDelay;
    }
    
    public void setMaxRetryDelay(Long maxRetryDelay) {
        this.maxRetryDelay = maxRetryDelay;
    }
    
    public Boolean getDeadLetterEnabled() {
        return deadLetterEnabled;
    }
    
    public void setDeadLetterEnabled(Boolean deadLetterEnabled) {
        this.deadLetterEnabled = deadLetterEnabled;
    }
    
    public String getDeadLetterTable() {
        return deadLetterTable;
    }
    
    public void setDeadLetterTable(String deadLetterTable) {
        this.deadLetterTable = deadLetterTable;
    }
    
    public String getDeadLetterTopic() {
        return deadLetterTopic;
    }
    
    public void setDeadLetterTopic(String deadLetterTopic) {
        this.deadLetterTopic = deadLetterTopic;
    }
    
    public String getDeadLetterFile() {
        return deadLetterFile;
    }
    
    public void setDeadLetterFile(String deadLetterFile) {
        this.deadLetterFile = deadLetterFile;
    }
    
    public Map<String, Object> getDeadLetterProperties() {
        return deadLetterProperties;
    }
    
    public void setDeadLetterProperties(Map<String, Object> deadLetterProperties) {
        this.deadLetterProperties = deadLetterProperties != null ? deadLetterProperties : new HashMap<>();
    }
    
    public Boolean getLogErrors() {
        return logErrors;
    }
    
    public void setLogErrors(Boolean logErrors) {
        this.logErrors = logErrors;
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
    
    public Boolean getIncludeData() {
        return includeData;
    }
    
    public void setIncludeData(Boolean includeData) {
        this.includeData = includeData;
    }
    
    public Integer getMaxLoggedErrors() {
        return maxLoggedErrors;
    }
    
    public void setMaxLoggedErrors(Integer maxLoggedErrors) {
        this.maxLoggedErrors = maxLoggedErrors;
    }
    
    public Boolean getReportErrors() {
        return reportErrors;
    }
    
    public void setReportErrors(Boolean reportErrors) {
        this.reportErrors = reportErrors;
    }
    
    public String getReportingEndpoint() {
        return reportingEndpoint;
    }
    
    public void setReportingEndpoint(String reportingEndpoint) {
        this.reportingEndpoint = reportingEndpoint;
    }
    
    public String getReportingTopic() {
        return reportingTopic;
    }
    
    public void setReportingTopic(String reportingTopic) {
        this.reportingTopic = reportingTopic;
    }
    
    public Map<String, String> getReportingHeaders() {
        return reportingHeaders;
    }
    
    public void setReportingHeaders(Map<String, String> reportingHeaders) {
        this.reportingHeaders = reportingHeaders != null ? reportingHeaders : new HashMap<>();
    }
    
    public Boolean getContinueOnBatchError() {
        return continueOnBatchError;
    }
    
    public void setContinueOnBatchError(Boolean continueOnBatchError) {
        this.continueOnBatchError = continueOnBatchError;
    }
    
    public Double getMaxBatchErrorRate() {
        return maxBatchErrorRate;
    }
    
    public void setMaxBatchErrorRate(Double maxBatchErrorRate) {
        this.maxBatchErrorRate = maxBatchErrorRate;
    }
    
    public Integer getMinBatchSuccessCount() {
        return minBatchSuccessCount;
    }
    
    public void setMinBatchSuccessCount(Integer minBatchSuccessCount) {
        this.minBatchSuccessCount = minBatchSuccessCount;
    }
    
    public String getCustomErrorHandler() {
        return customErrorHandler;
    }
    
    public void setCustomErrorHandler(String customErrorHandler) {
        this.customErrorHandler = customErrorHandler;
    }
    
    public Map<String, Object> getCustomHandlerProperties() {
        return customHandlerProperties;
    }
    
    public void setCustomHandlerProperties(Map<String, Object> customHandlerProperties) {
        this.customHandlerProperties = customHandlerProperties != null ? customHandlerProperties : new HashMap<>();
    }
    
    /**
     * Calculate the retry delay for a given attempt using exponential backoff.
     * 
     * @param attempt The retry attempt number (1-based)
     * @return The delay in milliseconds
     */
    public long calculateRetryDelay(int attempt) {
        if (attempt <= 0) {
            return 0;
        }
        
        long delay = (long) (retryDelay * Math.pow(retryBackoffMultiplier, attempt - 1));
        return Math.min(delay, maxRetryDelay);
    }
    
    /**
     * Check if an error should be retried based on configuration.
     * 
     * @param errorType The type of error
     * @param attemptCount The current attempt count
     * @return true if the error should be retried
     */
    public boolean shouldRetry(String errorType, int attemptCount) {
        if (attemptCount >= maxRetries) {
            return false;
        }
        
        if (nonRetryableErrors != null && nonRetryableErrors.contains(errorType)) {
            return false;
        }
        
        if (retryableErrors != null && !retryableErrors.contains(errorType)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate the error handling configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (strategy == null || strategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Error handling strategy is required");
        }
        
        ErrorStrategy errorStrategy = getErrorStrategy();
        if (errorStrategy == null) {
            throw new IllegalArgumentException("Invalid error handling strategy: " + strategy);
        }
        
        if (maxRetries != null && maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        
        if (retryDelay != null && retryDelay < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
        
        if (retryBackoffMultiplier != null && retryBackoffMultiplier <= 0) {
            throw new IllegalArgumentException("Retry backoff multiplier must be positive");
        }
        
        if (maxBatchErrorRate != null && (maxBatchErrorRate < 0 || maxBatchErrorRate > 1)) {
            throw new IllegalArgumentException("Max batch error rate must be between 0 and 1");
        }
    }
    
    /**
     * Create a copy of this configuration.
     * 
     * @return A new ErrorHandlingConfig with the same settings
     */
    public ErrorHandlingConfig copy() {
        ErrorHandlingConfig copy = new ErrorHandlingConfig();
        copy.strategy = this.strategy;
        copy.maxRetries = this.maxRetries;
        copy.retryDelay = this.retryDelay;
        copy.retryBackoffMultiplier = this.retryBackoffMultiplier;
        copy.maxRetryDelay = this.maxRetryDelay;
        
        copy.deadLetterEnabled = this.deadLetterEnabled;
        copy.deadLetterTable = this.deadLetterTable;
        copy.deadLetterTopic = this.deadLetterTopic;
        copy.deadLetterFile = this.deadLetterFile;
        copy.deadLetterProperties = new HashMap<>(this.deadLetterProperties);
        
        copy.logErrors = this.logErrors;
        copy.logLevel = this.logLevel;
        copy.includeStackTrace = this.includeStackTrace;
        copy.includeData = this.includeData;
        copy.maxLoggedErrors = this.maxLoggedErrors;
        
        copy.reportErrors = this.reportErrors;
        copy.reportingEndpoint = this.reportingEndpoint;
        copy.reportingTopic = this.reportingTopic;
        copy.reportingHeaders = new HashMap<>(this.reportingHeaders);
        
        copy.continueOnBatchError = this.continueOnBatchError;
        copy.maxBatchErrorRate = this.maxBatchErrorRate;
        copy.minBatchSuccessCount = this.minBatchSuccessCount;
        
        copy.circuitBreakerEnabled = this.circuitBreakerEnabled;
        copy.circuitBreakerThreshold = this.circuitBreakerThreshold;
        copy.circuitBreakerTimeout = this.circuitBreakerTimeout;
        
        copy.customErrorHandler = this.customErrorHandler;
        copy.customHandlerProperties = new HashMap<>(this.customHandlerProperties);
        
        copy.retryableErrors = this.retryableErrors != null ? List.copyOf(this.retryableErrors) : null;
        copy.nonRetryableErrors = this.nonRetryableErrors != null ? List.copyOf(this.nonRetryableErrors) : null;
        copy.errorMappings = new HashMap<>(this.errorMappings);
        
        return copy;
    }
}
