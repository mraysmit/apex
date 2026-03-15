package dev.mars.apex.core.config.model;

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

import java.util.List;

/**
 * YAML configuration for runtime script execution settings.
 *
 * This class represents the optional "runtime-scripts" section in YAML configurations.
 * When present, it enables dynamic Groovy script loading and invocation from SpEL expressions.
 *
 * Example YAML:
 * <pre>
 * runtime-scripts:
 *   enabled: true
 *   locations:
 *     - "./config/scripts"
 *   engine: "groovy"
 *   polling-interval-ms: 5000
 *   execution-timeout-ms: 200
 *   allowlist:
 *     - "risk-score"
 *     - "eligibility-check"
 *   fail-mode: "use-last-good"
 * </pre>
 */
public class YamlRuntimeScriptConfig {

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("locations")
    private List<String> locations;

    @JsonProperty("engine")
    private String engine = "groovy";

    @JsonProperty("polling-interval-ms")
    private long pollingIntervalMs = 5000;

    @JsonProperty("execution-timeout-ms")
    private long executionTimeoutMs = 200;

    @JsonProperty("allowlist")
    private List<String> allowlist;

    @JsonProperty("fail-mode")
    private String failMode = "use-last-good";

    public YamlRuntimeScriptConfig() {}

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public long getExecutionTimeoutMs() {
        return executionTimeoutMs;
    }

    public void setExecutionTimeoutMs(long executionTimeoutMs) {
        this.executionTimeoutMs = executionTimeoutMs;
    }

    public List<String> getAllowlist() {
        return allowlist;
    }

    public void setAllowlist(List<String> allowlist) {
        this.allowlist = allowlist;
    }

    public String getFailMode() {
        return failMode;
    }

    public void setFailMode(String failMode) {
        this.failMode = failMode;
    }
}
