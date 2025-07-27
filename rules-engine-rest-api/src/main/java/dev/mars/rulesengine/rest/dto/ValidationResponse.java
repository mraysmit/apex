package dev.mars.rulesengine.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
 * Response DTO for validation operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Response DTO for validation operations.
 * 
 * This class represents the response from validation operations,
 * containing the overall validation result and detailed error information.
 */
@Schema(description = "Response from data validation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResponse {
    
    @Schema(description = "Whether the validation was successful overall", example = "false")
    @JsonProperty("valid")
    private boolean valid;
    
    @Schema(description = "Total number of validation rules evaluated", example = "3")
    @JsonProperty("totalRules")
    private int totalRules;
    
    @Schema(description = "Number of rules that passed", example = "1")
    @JsonProperty("passedRules")
    private int passedRules;
    
    @Schema(description = "Number of rules that failed", example = "2")
    @JsonProperty("failedRules")
    private int failedRules;
    
    @Schema(description = "List of validation errors")
    @JsonProperty("errors")
    private List<ValidationErrorDto> errors;
    
    @Schema(description = "List of validation warnings")
    @JsonProperty("warnings")
    private List<ValidationErrorDto> warnings;
    
    @Schema(description = "Timestamp when validation was performed")
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @Schema(description = "Unique identifier for this validation")
    @JsonProperty("validationId")
    private String validationId;
    
    @Schema(description = "Performance metrics for the validation")
    @JsonProperty("metrics")
    private PerformanceMetricsDto metrics;
    
    // Default constructor
    public ValidationResponse() {
        this.timestamp = Instant.now();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    // Constructor
    public ValidationResponse(boolean valid, int totalRules, int passedRules, int failedRules) {
        this();
        this.valid = valid;
        this.totalRules = totalRules;
        this.passedRules = passedRules;
        this.failedRules = failedRules;
    }
    
    // Static factory methods
    public static ValidationResponse success(int totalRules) {
        return new ValidationResponse(true, totalRules, totalRules, 0);
    }
    
    public static ValidationResponse failure(int totalRules, int passedRules, int failedRules) {
        return new ValidationResponse(false, totalRules, passedRules, failedRules);
    }
    
    // Helper methods
    public void addError(String ruleName, String message, String severity) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(new ValidationErrorDto(ruleName, message, severity));
    }
    
    public void addWarning(String ruleName, String message) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(new ValidationErrorDto(ruleName, message, "WARNING"));
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
    
    public int getWarningCount() {
        return warnings != null ? warnings.size() : 0;
    }
    
    // Getters and setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public int getTotalRules() {
        return totalRules;
    }
    
    public void setTotalRules(int totalRules) {
        this.totalRules = totalRules;
    }
    
    public int getPassedRules() {
        return passedRules;
    }
    
    public void setPassedRules(int passedRules) {
        this.passedRules = passedRules;
    }
    
    public int getFailedRules() {
        return failedRules;
    }
    
    public void setFailedRules(int failedRules) {
        this.failedRules = failedRules;
    }
    
    public List<ValidationErrorDto> getErrors() {
        return errors;
    }
    
    public void setErrors(List<ValidationErrorDto> errors) {
        this.errors = errors;
    }
    
    public List<ValidationErrorDto> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<ValidationErrorDto> warnings) {
        this.warnings = warnings;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getValidationId() {
        return validationId;
    }
    
    public void setValidationId(String validationId) {
        this.validationId = validationId;
    }
    
    public PerformanceMetricsDto getMetrics() {
        return metrics;
    }
    
    public void setMetrics(PerformanceMetricsDto metrics) {
        this.metrics = metrics;
    }
    
    /**
     * DTO for validation errors and warnings.
     */
    @Schema(description = "Validation error or warning")
    public static class ValidationErrorDto {
        
        @Schema(description = "Name of the rule that failed", example = "age-check")
        @JsonProperty("ruleName")
        private String ruleName;
        
        @Schema(description = "Error or warning message", example = "Age must be at least 18")
        @JsonProperty("message")
        private String message;
        
        @Schema(description = "Severity level", example = "ERROR")
        @JsonProperty("severity")
        private String severity;
        
        @Schema(description = "Timestamp when the error occurred")
        @JsonProperty("timestamp")
        private Instant timestamp;
        
        // Default constructor
        public ValidationErrorDto() {
            this.timestamp = Instant.now();
        }
        
        // Constructor
        public ValidationErrorDto(String ruleName, String message, String severity) {
            this();
            this.ruleName = ruleName;
            this.message = message;
            this.severity = severity;
        }
        
        // Getters and setters
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
        
        public String getSeverity() {
            return severity;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return "ValidationErrorDto{" +
                    "ruleName='" + ruleName + '\'' +
                    ", message='" + message + '\'' +
                    ", severity='" + severity + '\'' +
                    '}';
        }
    }
}
