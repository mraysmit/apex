package dev.mars.rulesengine.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request DTO for rule evaluation operations.
 * 
 * This class represents the request payload for evaluating rules,
 * containing the rule condition/expression and the data to evaluate against.
 */
@Schema(description = "Request for rule evaluation")
public class RuleEvaluationRequest {
    
    @Schema(description = "SpEL expression or rule condition to evaluate", 
            example = "#age >= 18 && #email != null")
    @JsonProperty("condition")
    @NotBlank(message = "Condition cannot be blank")
    private String condition;
    
    @Schema(description = "Data object or facts to evaluate the rule against",
            example = "{\"age\": 25, \"email\": \"john@example.com\", \"balance\": 1500}")
    @JsonProperty("data")
    @NotNull(message = "Data cannot be null")
    private Map<String, Object> data;
    
    @Schema(description = "Optional rule name for identification",
            example = "customer-validation")
    @JsonProperty("ruleName")
    private String ruleName;
    
    @Schema(description = "Optional message to return when rule matches",
            example = "Customer validation passed")
    @JsonProperty("message")
    private String message;
    
    @Schema(description = "Whether to include performance metrics in the response",
            example = "true")
    @JsonProperty("includeMetrics")
    private boolean includeMetrics = false;
    
    // Default constructor
    public RuleEvaluationRequest() {}
    
    // Constructor with required fields
    public RuleEvaluationRequest(String condition, Map<String, Object> data) {
        this.condition = condition;
        this.data = data;
    }
    
    // Full constructor
    public RuleEvaluationRequest(String condition, Map<String, Object> data, 
                                String ruleName, String message, boolean includeMetrics) {
        this.condition = condition;
        this.data = data;
        this.ruleName = ruleName;
        this.message = message;
        this.includeMetrics = includeMetrics;
    }
    
    // Getters and setters
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
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
    
    public boolean isIncludeMetrics() {
        return includeMetrics;
    }
    
    public void setIncludeMetrics(boolean includeMetrics) {
        this.includeMetrics = includeMetrics;
    }
    
    @Override
    public String toString() {
        return "RuleEvaluationRequest{" +
                "condition='" + condition + '\'' +
                ", data=" + data +
                ", ruleName='" + ruleName + '\'' +
                ", message='" + message + '\'' +
                ", includeMetrics=" + includeMetrics +
                '}';
    }
}
