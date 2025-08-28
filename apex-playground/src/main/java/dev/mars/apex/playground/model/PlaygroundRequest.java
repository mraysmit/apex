package dev.mars.apex.playground.model;

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
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for playground data processing operations.
 * 
 * Contains the source data and YAML rules configuration needed
 * for processing in the APEX playground.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Schema(description = "Request for processing data with YAML rules in the playground")
public class PlaygroundRequest {

    @JsonProperty("sourceData")
    @Schema(description = "The source data to process (JSON, XML, or CSV format)",
            example = "{\"name\": \"John Doe\", \"age\": 30, \"email\": \"john@example.com\"}"
    )
    private String sourceData;

    @JsonProperty("yamlRules")
    @Schema(description = "The YAML rules configuration",
            example = "metadata:\n  name: \"Sample Rules\"\n  version: \"1.0.0\"\nrules:\n  - id: \"age-check\"\n    condition: \"#age >= 18\"")
    private String yamlRules;

    @JsonProperty("dataFormat")
    @Schema(description = "The format of the source data",
            allowableValues = {"JSON", "XML", "CSV"},
            example = "JSON")
    private String dataFormat;

    @JsonProperty("processingOptions")
    @Schema(description = "Optional processing configuration")
    private ProcessingOptions processingOptions;

    // Default constructor
    public PlaygroundRequest() {
        this.processingOptions = new ProcessingOptions();
    }

    // Constructor with required fields
    public PlaygroundRequest(String sourceData, String yamlRules, String dataFormat) {
        this.sourceData = sourceData;
        this.yamlRules = yamlRules;
        this.dataFormat = dataFormat;
        this.processingOptions = new ProcessingOptions();
    }

    // Getters and setters
    public String getSourceData() {
        return sourceData;
    }

    public void setSourceData(String sourceData) {
        this.sourceData = sourceData;
    }

    public String getYamlRules() {
        return yamlRules;
    }

    public void setYamlRules(String yamlRules) {
        this.yamlRules = yamlRules;
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public ProcessingOptions getProcessingOptions() {
        return processingOptions;
    }

    public void setProcessingOptions(ProcessingOptions processingOptions) {
        this.processingOptions = processingOptions;
    }

    /**
     * Processing options for customizing the playground behavior.
     */
    @Schema(description = "Processing options for customizing playground behavior")
    public static class ProcessingOptions {
        
        @JsonProperty("enableEnrichment")
        @Schema(description = "Whether to enable data enrichment", defaultValue = "true")
        private boolean enableEnrichment = true;

        @JsonProperty("enableValidation")
        @Schema(description = "Whether to enable data validation", defaultValue = "true")
        private boolean enableValidation = true;

        @JsonProperty("collectMetrics")
        @Schema(description = "Whether to collect performance metrics", defaultValue = "true")
        private boolean collectMetrics = true;

        @JsonProperty("timeoutMs")
        @Schema(description = "Processing timeout in milliseconds", defaultValue = "30000")
        private long timeoutMs = 30000L;

        // Getters and setters
        public boolean isEnableEnrichment() {
            return enableEnrichment;
        }

        public void setEnableEnrichment(boolean enableEnrichment) {
            this.enableEnrichment = enableEnrichment;
        }

        public boolean isEnableValidation() {
            return enableValidation;
        }

        public void setEnableValidation(boolean enableValidation) {
            this.enableValidation = enableValidation;
        }

        public boolean isCollectMetrics() {
            return collectMetrics;
        }

        public void setCollectMetrics(boolean collectMetrics) {
            this.collectMetrics = collectMetrics;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    @Override
    public String toString() {
        return "PlaygroundRequest{" +
                "dataFormat='" + dataFormat + '\'' +
                ", sourceDataLength=" + (sourceData != null ? sourceData.length() : 0) +
                ", yamlRulesLength=" + (yamlRules != null ? yamlRules.length() : 0) +
                ", processingOptions=" + processingOptions +
                '}';
    }
}
