package dev.mars.rulesengine.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.util.List;
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
 * Request DTO for validation operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Request DTO for validation operations.
 * 
 * This class represents a request for validating data against multiple rules
 * or validation conditions, providing detailed validation results.
 */
@Schema(description = "Request for data validation against multiple rules")
public class ValidationRequest {
    
    @Schema(description = "Data object to validate",
            example = "{\"age\": 16, \"email\": null, \"balance\": -100}")
    @JsonProperty("data")
    @NotNull(message = "Data cannot be null")
    private Map<String, Object> data;
    
    @Schema(description = "List of validation rules to apply")
    @JsonProperty("validationRules")
    private List<ValidationRuleDto> validationRules;
    
    @Schema(description = "Whether to stop validation on first failure", example = "false")
    @JsonProperty("stopOnFirstFailure")
    private boolean stopOnFirstFailure = false;
    
    @Schema(description = "Whether to include detailed error information", example = "true")
    @JsonProperty("includeDetails")
    private boolean includeDetails = true;
    
    @Schema(description = "Whether to include performance metrics", example = "false")
    @JsonProperty("includeMetrics")
    private boolean includeMetrics = false;
    
    // Default constructor
    public ValidationRequest() {}
    
    // Constructor with required fields
    public ValidationRequest(Map<String, Object> data, List<ValidationRuleDto> validationRules) {
        this.data = data;
        this.validationRules = validationRules;
    }
    
    // Getters and setters
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public List<ValidationRuleDto> getValidationRules() {
        return validationRules;
    }
    
    public void setValidationRules(List<ValidationRuleDto> validationRules) {
        this.validationRules = validationRules;
    }
    
    public boolean isStopOnFirstFailure() {
        return stopOnFirstFailure;
    }
    
    public void setStopOnFirstFailure(boolean stopOnFirstFailure) {
        this.stopOnFirstFailure = stopOnFirstFailure;
    }
    
    public boolean isIncludeDetails() {
        return includeDetails;
    }
    
    public void setIncludeDetails(boolean includeDetails) {
        this.includeDetails = includeDetails;
    }
    
    public boolean isIncludeMetrics() {
        return includeMetrics;
    }
    
    public void setIncludeMetrics(boolean includeMetrics) {
        this.includeMetrics = includeMetrics;
    }
    
    /**
     * DTO for individual validation rules.
     */
    @Schema(description = "Individual validation rule")
    public static class ValidationRuleDto {
        
        @Schema(description = "Rule name or identifier", example = "age-check")
        @JsonProperty("name")
        private String name;
        
        @Schema(description = "SpEL condition to evaluate", example = "#data.age >= 18")
        @JsonProperty("condition")
        private String condition;
        
        @Schema(description = "Error message if validation fails", 
                example = "Age must be at least 18")
        @JsonProperty("message")
        private String message;
        
        @Schema(description = "Severity level of the validation", example = "ERROR")
        @JsonProperty("severity")
        private String severity = "ERROR";
        
        // Default constructor
        public ValidationRuleDto() {}
        
        // Constructor
        public ValidationRuleDto(String name, String condition, String message) {
            this.name = name;
            this.condition = condition;
            this.message = message;
        }
        
        // Constructor with severity
        public ValidationRuleDto(String name, String condition, String message, String severity) {
            this.name = name;
            this.condition = condition;
            this.message = message;
            this.severity = severity;
        }
        
        // Getters and setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
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
        
        @Override
        public String toString() {
            return "ValidationRuleDto{" +
                    "name='" + name + '\'' +
                    ", condition='" + condition + '\'' +
                    ", message='" + message + '\'' +
                    ", severity='" + severity + '\'' +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "ValidationRequest{" +
                "data=" + data +
                ", validationRules=" + validationRules +
                ", stopOnFirstFailure=" + stopOnFirstFailure +
                ", includeDetails=" + includeDetails +
                ", includeMetrics=" + includeMetrics +
                '}';
    }
}
