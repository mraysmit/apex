package dev.mars.apex.playground.config;

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


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for APEX Playground.
 * 
 * Manages playground-specific configuration settings including
 * file size limits, processing timeouts, and feature toggles.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "apex.playground")
public class PlaygroundConfig {

    /**
     * Maximum file size for uploads (in bytes).
     */
    private long maxFileSize = 10485760L; // 10MB

    /**
     * Maximum number of rules per configuration.
     */
    private int maxRulesPerConfig = 100;

    /**
     * Maximum processing timeout (in milliseconds).
     */
    private long processingTimeout = 30000L; // 30 seconds

    /**
     * Enable/disable example templates.
     */
    private boolean examplesEnabled = true;

    /**
     * Directory for storing temporary files.
     */
    private String tempDirectory = System.getProperty("java.io.tmpdir") + "/apex-playground";

    /**
     * Enable/disable performance metrics collection.
     */
    private boolean metricsEnabled = true;

    // Getters and setters

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxRulesPerConfig() {
        return maxRulesPerConfig;
    }

    public void setMaxRulesPerConfig(int maxRulesPerConfig) {
        this.maxRulesPerConfig = maxRulesPerConfig;
    }

    public long getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(long processingTimeout) {
        this.processingTimeout = processingTimeout;
    }

    public boolean isExamplesEnabled() {
        return examplesEnabled;
    }

    public void setExamplesEnabled(boolean examplesEnabled) {
        this.examplesEnabled = examplesEnabled;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
}
