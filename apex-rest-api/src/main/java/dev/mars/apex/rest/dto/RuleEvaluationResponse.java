package dev.mars.apex.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

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

/**
 * Response DTO for rule evaluation operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Response DTO for rule evaluation operations.
 * 
 * This class represents the response from rule evaluation,
 * containing the result, performance metrics, and additional metadata.
 */
@Schema(description = "Response from rule evaluation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleEvaluationResponse {
    
    @Schema(description = "Whether the rule evaluation was successful", example = "true")
    @JsonProperty("success")
    private boolean success;
    
    @Schema(description = "Whether the rule condition matched/triggered", example = "true")
    @JsonProperty("matched")
    private boolean matched;
    
    @Schema(description = "Rule name or identifier", example = "customer-validation")
    @JsonProperty("ruleName")
    private String ruleName;
    
    @Schema(description = "Message associated with the rule result", 
            example = "Customer validation passed")
    @JsonProperty("message")
    private String message;
    
    @Schema(description = "Timestamp when the evaluation was performed")
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @Schema(description = "Unique identifier for this evaluation")
    @JsonProperty("evaluationId")
    private String evaluationId;
    
    @Schema(description = "Performance metrics for the evaluation")
    @JsonProperty("metrics")
    private PerformanceMetricsDto metrics;
    
    @Schema(description = "Any enriched data added during evaluation")
    @JsonProperty("enrichedData")
    private Map<String, Object> enrichedData;
    
    @Schema(description = "Error message if evaluation failed")
    @JsonProperty("error")
    private String error;
    
    @Schema(description = "Additional details about the error")
    @JsonProperty("errorDetails")
    private String errorDetails;
    
    // Default constructor
    public RuleEvaluationResponse() {
        this.timestamp = Instant.now();
    }
    
    // Success constructor
    public RuleEvaluationResponse(boolean matched, String ruleName, String message) {
        this();
        this.success = true;
        this.matched = matched;
        this.ruleName = ruleName;
        this.message = message;
    }
    
    // Error constructor
    public RuleEvaluationResponse(String error, String errorDetails) {
        this();
        this.success = false;
        this.matched = false;
        this.error = error;
        this.errorDetails = errorDetails;
    }
    
    // Static factory methods
    public static RuleEvaluationResponse success(boolean matched, String ruleName, String message) {
        return new RuleEvaluationResponse(matched, ruleName, message);
    }
    
    public static RuleEvaluationResponse error(String error) {
        return new RuleEvaluationResponse(error, null);
    }
    
    public static RuleEvaluationResponse error(String error, String details) {
        return new RuleEvaluationResponse(error, details);
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isMatched() {
        return matched;
    }
    
    public void setMatched(boolean matched) {
        this.matched = matched;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEvaluationId() {
        return evaluationId;
    }
    
    public void setEvaluationId(String evaluationId) {
        this.evaluationId = evaluationId;
    }
    
    public PerformanceMetricsDto getMetrics() {
        return metrics;
    }
    
    public void setMetrics(PerformanceMetricsDto metrics) {
        this.metrics = metrics;
    }
    
    public Map<String, Object> getEnrichedData() {
        return enrichedData;
    }
    
    public void setEnrichedData(Map<String, Object> enrichedData) {
        this.enrichedData = enrichedData;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
