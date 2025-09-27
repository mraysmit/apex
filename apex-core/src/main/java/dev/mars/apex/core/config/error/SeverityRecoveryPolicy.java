/*
 * Copyright (c) 2024 APEX Rules Engine Contributors
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

package dev.mars.apex.core.config.error;

import dev.mars.apex.core.service.error.ErrorRecoveryService;

/**
 * Configuration class for error recovery policy specific to a severity level.
 * 
 * This class defines how errors should be handled for a specific severity level,
 * including whether recovery should be attempted, which strategy to use, and
 * retry parameters.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Enable/disable recovery per severity</li>
 *   <li>Configurable recovery strategies</li>
 *   <li>Retry count and delay configuration</li>
 *   <li>Validation of configuration parameters</li>
 * </ul>
 * 
 * <p>Example Usage:
 * <pre>{@code
 * // Create policy for WARNING severity
 * SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy();
 * warningPolicy.setRecoveryEnabled(true);
 * warningPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
 * warningPolicy.setMaxRetries(1);
 * warningPolicy.setRetryDelay(100L);
 * 
 * // Create policy for ERROR severity (no recovery)
 * SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy();
 * errorPolicy.setRecoveryEnabled(false);
 * errorPolicy.setStrategy("FAIL_FAST");
 * }</pre>
 * 
 * @author APEX Rules Engine
 * @since Phase 3 - Configurable Error Recovery
 * @version 1.0
 */
public class SeverityRecoveryPolicy {

    /**
     * Whether error recovery is enabled for this severity level.
     */
    private boolean recoveryEnabled = true;

    /**
     * The recovery strategy to use for this severity level.
     * Must be a valid ErrorRecoveryStrategy enum value.
     */
    private String strategy = "CONTINUE_WITH_DEFAULT";

    /**
     * Maximum number of recovery attempts for this severity level.
     */
    private int maxRetries = 0;

    /**
     * Delay between retry attempts in milliseconds.
     */
    private long retryDelay = 100L;

    /**
     * Maximum delay between retry attempts in milliseconds.
     */
    private long maxRetryDelay = 5000L;

    /**
     * Backoff multiplier for exponential retry delays.
     */
    private double retryBackoffMultiplier = 2.0;

    /**
     * Whether to log recovery attempts for this severity level.
     */
    private boolean logRecoveryAttempts = true;

    /**
     * Custom recovery message template for this severity level.
     */
    private String recoveryMessageTemplate;

    /**
     * Default constructor.
     */
    public SeverityRecoveryPolicy() {
        // Use defaults
    }

    /**
     * Constructor with basic parameters.
     * 
     * @param recoveryEnabled Whether recovery is enabled
     * @param strategy The recovery strategy to use
     */
    public SeverityRecoveryPolicy(boolean recoveryEnabled, String strategy) {
        this.recoveryEnabled = recoveryEnabled;
        this.strategy = strategy;
    }

    /**
     * Constructor with all parameters.
     * 
     * @param recoveryEnabled Whether recovery is enabled
     * @param strategy The recovery strategy to use
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelay Delay between retries in milliseconds
     */
    public SeverityRecoveryPolicy(boolean recoveryEnabled, String strategy, int maxRetries, long retryDelay) {
        this.recoveryEnabled = recoveryEnabled;
        this.strategy = strategy;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    /**
     * Validate the recovery policy configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (strategy == null || strategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Recovery strategy cannot be null or empty");
        }

        // Validate that strategy is a valid ErrorRecoveryStrategy
        try {
            ErrorRecoveryService.ErrorRecoveryStrategy.valueOf(strategy);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid recovery strategy: " + strategy + 
                ". Valid strategies are: " + java.util.Arrays.toString(ErrorRecoveryService.ErrorRecoveryStrategy.values()));
        }

        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative: " + maxRetries);
        }

        if (retryDelay < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative: " + retryDelay);
        }

        if (maxRetryDelay < retryDelay) {
            throw new IllegalArgumentException("Max retry delay (" + maxRetryDelay + 
                ") cannot be less than retry delay (" + retryDelay + ")");
        }

        if (retryBackoffMultiplier <= 0) {
            throw new IllegalArgumentException("Retry backoff multiplier must be positive: " + retryBackoffMultiplier);
        }
    }

    /**
     * Create a copy of this policy.
     * 
     * @return A new SeverityRecoveryPolicy with the same configuration
     */
    public SeverityRecoveryPolicy copy() {
        SeverityRecoveryPolicy copy = new SeverityRecoveryPolicy();
        copy.recoveryEnabled = this.recoveryEnabled;
        copy.strategy = this.strategy;
        copy.maxRetries = this.maxRetries;
        copy.retryDelay = this.retryDelay;
        copy.maxRetryDelay = this.maxRetryDelay;
        copy.retryBackoffMultiplier = this.retryBackoffMultiplier;
        copy.logRecoveryAttempts = this.logRecoveryAttempts;
        copy.recoveryMessageTemplate = this.recoveryMessageTemplate;
        return copy;
    }

    // Getters and setters

    public boolean isRecoveryEnabled() {
        return recoveryEnabled;
    }

    public void setRecoveryEnabled(boolean recoveryEnabled) {
        this.recoveryEnabled = recoveryEnabled;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(long retryDelay) {
        this.retryDelay = retryDelay;
    }

    public long getMaxRetryDelay() {
        return maxRetryDelay;
    }

    public void setMaxRetryDelay(long maxRetryDelay) {
        this.maxRetryDelay = maxRetryDelay;
    }

    public double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public void setRetryBackoffMultiplier(double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
    }

    public boolean isLogRecoveryAttempts() {
        return logRecoveryAttempts;
    }

    public void setLogRecoveryAttempts(boolean logRecoveryAttempts) {
        this.logRecoveryAttempts = logRecoveryAttempts;
    }

    public String getRecoveryMessageTemplate() {
        return recoveryMessageTemplate;
    }

    public void setRecoveryMessageTemplate(String recoveryMessageTemplate) {
        this.recoveryMessageTemplate = recoveryMessageTemplate;
    }

    @Override
    public String toString() {
        return "SeverityRecoveryPolicy{" +
                "recoveryEnabled=" + recoveryEnabled +
                ", strategy='" + strategy + '\'' +
                ", maxRetries=" + maxRetries +
                ", retryDelay=" + retryDelay +
                ", maxRetryDelay=" + maxRetryDelay +
                ", retryBackoffMultiplier=" + retryBackoffMultiplier +
                ", logRecoveryAttempts=" + logRecoveryAttempts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SeverityRecoveryPolicy that = (SeverityRecoveryPolicy) o;

        if (recoveryEnabled != that.recoveryEnabled) return false;
        if (maxRetries != that.maxRetries) return false;
        if (retryDelay != that.retryDelay) return false;
        if (maxRetryDelay != that.maxRetryDelay) return false;
        if (Double.compare(that.retryBackoffMultiplier, retryBackoffMultiplier) != 0) return false;
        if (logRecoveryAttempts != that.logRecoveryAttempts) return false;
        if (!strategy.equals(that.strategy)) return false;
        return recoveryMessageTemplate != null ? recoveryMessageTemplate.equals(that.recoveryMessageTemplate) : that.recoveryMessageTemplate == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (recoveryEnabled ? 1 : 0);
        result = 31 * result + strategy.hashCode();
        result = 31 * result + maxRetries;
        result = 31 * result + (int) (retryDelay ^ (retryDelay >>> 32));
        result = 31 * result + (int) (maxRetryDelay ^ (maxRetryDelay >>> 32));
        temp = Double.doubleToLongBits(retryBackoffMultiplier);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (logRecoveryAttempts ? 1 : 0);
        result = 31 * result + (recoveryMessageTemplate != null ? recoveryMessageTemplate.hashCode() : 0);
        return result;
    }
}
