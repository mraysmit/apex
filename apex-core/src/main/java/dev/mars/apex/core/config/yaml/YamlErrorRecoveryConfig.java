package dev.mars.apex.core.config.yaml;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;

import java.util.Map;

/**
 * YAML configuration for error recovery settings.
 * 
 * This class represents the optional "error-recovery" section in YAML configurations.
 * When not present, the system uses backward-compatible defaults.
 * 
 * Example YAML:
 * <pre>
 * error-recovery:
 *   enabled: true
 *   log-recovery-attempts: true
 *   metrics-enabled: true
 *   severity-policies:
 *     ERROR:
 *       recovery-enabled: false
 *       strategy: "FAIL_FAST"
 *     WARNING:
 *       recovery-enabled: true
 *       strategy: "CONTINUE_WITH_DEFAULT"
 *       max-retries: 1
 *       retry-delay: 100
 * </pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
public class YamlErrorRecoveryConfig {

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("log-recovery-attempts")
    private Boolean logRecoveryAttempts;

    @JsonProperty("metrics-enabled")
    private Boolean metricsEnabled;

    @JsonProperty("default-strategy")
    private String defaultStrategy;

    @JsonProperty("severity-policies")
    private Map<String, YamlSeverityRecoveryPolicy> severityPolicies;

    // Default constructor
    public YamlErrorRecoveryConfig() {}

    /**
     * Convert this YAML configuration to the internal ErrorRecoveryConfig.
     * Provides backward-compatible defaults when values are not specified.
     * 
     * @return ErrorRecoveryConfig with appropriate defaults
     */
    public ErrorRecoveryConfig toErrorRecoveryConfig() {
        ErrorRecoveryConfig config = new ErrorRecoveryConfig();
        
        // Apply YAML values or use defaults
        if (enabled != null) {
            config.setEnabled(enabled);
        }
        
        if (logRecoveryAttempts != null) {
            config.setLogRecoveryAttempts(logRecoveryAttempts);
        }
        
        if (metricsEnabled != null) {
            config.setMetricsEnabled(metricsEnabled);
        }
        
        if (defaultStrategy != null) {
            config.setDefaultStrategy(defaultStrategy);
        }
        
        // Convert severity policies if present
        if (severityPolicies != null) {
            for (Map.Entry<String, YamlSeverityRecoveryPolicy> entry : severityPolicies.entrySet()) {
                String severity = entry.getKey();
                YamlSeverityRecoveryPolicy yamlPolicy = entry.getValue();
                SeverityRecoveryPolicy policy = yamlPolicy.toSeverityRecoveryPolicy();
                config.setSeverityPolicy(severity, policy);
            }
        }
        
        return config;
    }

    // Getters and setters
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getLogRecoveryAttempts() {
        return logRecoveryAttempts;
    }

    public void setLogRecoveryAttempts(Boolean logRecoveryAttempts) {
        this.logRecoveryAttempts = logRecoveryAttempts;
    }

    public Boolean getMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(Boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(String defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public Map<String, YamlSeverityRecoveryPolicy> getSeverityPolicies() {
        return severityPolicies;
    }

    public void setSeverityPolicies(Map<String, YamlSeverityRecoveryPolicy> severityPolicies) {
        this.severityPolicies = severityPolicies;
    }

    /**
     * YAML configuration for individual severity recovery policies.
     */
    public static class YamlSeverityRecoveryPolicy {
        
        @JsonProperty("recovery-enabled")
        private Boolean recoveryEnabled;

        @JsonProperty("strategy")
        private String strategy;

        @JsonProperty("max-retries")
        private Integer maxRetries;

        @JsonProperty("retry-delay")
        private Long retryDelay;

        // Default constructor
        public YamlSeverityRecoveryPolicy() {}

        /**
         * Convert to internal SeverityRecoveryPolicy.
         * 
         * @return SeverityRecoveryPolicy with appropriate defaults
         */
        public SeverityRecoveryPolicy toSeverityRecoveryPolicy() {
            SeverityRecoveryPolicy policy = new SeverityRecoveryPolicy();
            
            if (recoveryEnabled != null) {
                policy.setRecoveryEnabled(recoveryEnabled);
            }
            
            if (strategy != null) {
                policy.setStrategy(strategy);
            }
            
            if (maxRetries != null) {
                policy.setMaxRetries(maxRetries);
            }
            
            if (retryDelay != null) {
                policy.setRetryDelay(retryDelay);
            }
            
            return policy;
        }

        // Getters and setters
        public Boolean getRecoveryEnabled() {
            return recoveryEnabled;
        }

        public void setRecoveryEnabled(Boolean recoveryEnabled) {
            this.recoveryEnabled = recoveryEnabled;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
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
    }
}
