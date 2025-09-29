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

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.service.error.ErrorRecoveryService;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for severity-based error recovery policies.
 * 
 * This class provides fine-grained control over error recovery behavior based on
 * rule severity levels. Different severity levels can have different recovery
 * strategies, retry counts, and delays.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Severity-specific recovery policies</li>
 *   <li>Configurable recovery strategies per severity</li>
 *   <li>Backward compatibility with existing behavior</li>
 *   <li>Runtime configuration updates</li>
 *   <li>Comprehensive validation</li>
 * </ul>
 * 
 * <p>Example Configuration:
 * <pre>{@code
 * ErrorRecoveryConfig config = new ErrorRecoveryConfig();
 * config.setEnabled(true);
 * 
 * // ERROR severity - no recovery (backward compatible)
 * SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy();
 * errorPolicy.setRecoveryEnabled(false);
 * errorPolicy.setStrategy("FAIL_FAST");
 * config.setSeverityPolicy("ERROR", errorPolicy);
 * 
 * // WARNING severity - recovery enabled
 * SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy();
 * warningPolicy.setRecoveryEnabled(true);
 * warningPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
 * config.setSeverityPolicy("WARNING", warningPolicy);
 * }</pre>
 * 
 * @author APEX Rules Engine
 * @since Phase 3 - Configurable Error Recovery
 * @version 1.0
 */
public class ErrorRecoveryConfig {

    /**
     * Whether error recovery is globally enabled.
     */
    private boolean enabled = true;

    /**
     * Severity-specific recovery policies.
     * Maps severity level (ERROR, WARNING, INFO) to recovery policy.
     */
    private Map<String, SeverityRecoveryPolicy> severityPolicies = new HashMap<>();

    /**
     * Default recovery strategy when no specific policy is configured.
     */
    private String defaultStrategy = "CONTINUE_WITH_DEFAULT";

    /**
     * Whether to log recovery attempts for debugging and monitoring.
     */
    private boolean logRecoveryAttempts = true;

    /**
     * Whether to collect metrics on recovery success/failure rates.
     */
    private boolean metricsEnabled = true;

    /**
     * Default constructor that sets up backward-compatible defaults.
     */
    public ErrorRecoveryConfig() {
        initializeDefaults();
    }

    /**
     * Initialize backward-compatible default policies.
     * This ensures existing tests continue to pass.
     */
    private void initializeDefaults() {
        // CRITICAL severity - no recovery (highest severity)
        SeverityRecoveryPolicy criticalPolicy = new SeverityRecoveryPolicy();
        criticalPolicy.setRecoveryEnabled(false);
        criticalPolicy.setStrategy("FAIL_FAST");
        severityPolicies.put("CRITICAL", criticalPolicy);

        // ERROR severity - no recovery (matches current test expectations)
        SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy();
        errorPolicy.setRecoveryEnabled(false);
        errorPolicy.setStrategy("FAIL_FAST");
        severityPolicies.put(SeverityConstants.ERROR, errorPolicy);

        // WARNING severity - recovery enabled
        SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy();
        warningPolicy.setRecoveryEnabled(true);
        warningPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
        warningPolicy.setMaxRetries(0);
        severityPolicies.put(SeverityConstants.WARNING, warningPolicy);

        // INFO severity - Default backward compatible behavior
        SeverityRecoveryPolicy infoPolicy = new SeverityRecoveryPolicy();
        infoPolicy.setRecoveryEnabled(true);
        infoPolicy.setStrategy("CONTINUE_WITH_DEFAULT");
        infoPolicy.setMaxRetries(0);
        severityPolicies.put(SeverityConstants.INFO, infoPolicy);
    }

    /**
     * Get the recovery policy for a specific severity level.
     * 
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @return The recovery policy, or null if not configured
     */
    public SeverityRecoveryPolicy getSeverityPolicy(String severity) {
        if (severity == null) {
            return null;
        }
        return severityPolicies.get(severity.toUpperCase());
    }

    /**
     * Set the recovery policy for a specific severity level.
     * 
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param policy The recovery policy to set
     */
    public void setSeverityPolicy(String severity, SeverityRecoveryPolicy policy) {
        if (severity != null && policy != null) {
            severityPolicies.put(severity.toUpperCase(), policy);
        }
    }

    /**
     * Check if recovery is enabled for a specific severity level.
     * 
     * @param severity The severity level to check
     * @return true if recovery is enabled for this severity, false otherwise
     */
    public boolean isRecoveryEnabledForSeverity(String severity) {
        if (!enabled) {
            return false;
        }
        
        SeverityRecoveryPolicy policy = getSeverityPolicy(severity);
        return policy != null && policy.isRecoveryEnabled();
    }

    /**
     * Get the recovery strategy for a specific severity level.
     * 
     * @param severity The severity level
     * @return The recovery strategy, or default strategy if not configured
     */
    public String getRecoveryStrategy(String severity) {
        SeverityRecoveryPolicy policy = getSeverityPolicy(severity);
        if (policy != null && policy.getStrategy() != null) {
            return policy.getStrategy();
        }
        return defaultStrategy;
    }

    /**
     * Validate the configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (defaultStrategy == null || defaultStrategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Default recovery strategy cannot be null or empty");
        }

        // Validate that default strategy is a valid ErrorRecoveryStrategy
        try {
            ErrorRecoveryService.ErrorRecoveryStrategy.valueOf(defaultStrategy);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid default recovery strategy: " + defaultStrategy);
        }

        // Validate all severity policies
        for (Map.Entry<String, SeverityRecoveryPolicy> entry : severityPolicies.entrySet()) {
            String severity = entry.getKey();
            SeverityRecoveryPolicy policy = entry.getValue();
            
            if (policy == null) {
                throw new IllegalArgumentException("Recovery policy cannot be null for severity: " + severity);
            }
            
            try {
                policy.validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid recovery policy for severity " + severity + ": " + e.getMessage());
            }
        }
    }

    // Getters and setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, SeverityRecoveryPolicy> getSeverityPolicies() {
        return new HashMap<>(severityPolicies);
    }

    public void setSeverityPolicies(Map<String, SeverityRecoveryPolicy> severityPolicies) {
        this.severityPolicies = severityPolicies != null ? new HashMap<>(severityPolicies) : new HashMap<>();
    }

    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(String defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public boolean isLogRecoveryAttempts() {
        return logRecoveryAttempts;
    }

    public void setLogRecoveryAttempts(boolean logRecoveryAttempts) {
        this.logRecoveryAttempts = logRecoveryAttempts;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    @Override
    public String toString() {
        return "ErrorRecoveryConfig{" +
                "enabled=" + enabled +
                ", severityPolicies=" + severityPolicies.size() + " policies" +
                ", defaultStrategy='" + defaultStrategy + '\'' +
                ", logRecoveryAttempts=" + logRecoveryAttempts +
                ", metricsEnabled=" + metricsEnabled +
                '}';
    }
}
