package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Result of executing a single rule in the playground.
 * 
 * Contains detailed information about the rule execution including
 * whether it passed, the message, and any additional metadata.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@Schema(description = "Result of executing a single rule")
public class RuleExecutionResult {

    @JsonProperty("ruleId")
    @Schema(description = "The unique identifier of the rule", example = "age-check")
    private String ruleId;

    @JsonProperty("ruleName")
    @Schema(description = "The display name of the rule", example = "Age Validation")
    private String ruleName;

    @JsonProperty("passed")
    @Schema(description = "Whether the rule passed", example = "true")
    private boolean passed;

    @JsonProperty("message")
    @Schema(description = "The rule message or error description", example = "Age must be 18 or older")
    private String message;

    @JsonProperty("condition")
    @Schema(description = "The rule condition that was evaluated", example = "#age >= 18")
    private String condition;

    @JsonProperty("executionTimeMs")
    @Schema(description = "Time taken to execute this rule in milliseconds", example = "5")
    private long executionTimeMs;

    @JsonProperty("severity")
    @Schema(description = "The severity level of the rule", example = "ERROR")
    private String severity;

    @JsonProperty("category")
    @Schema(description = "The category of the rule", example = "validation")
    private String category;

    // Default constructor
    public RuleExecutionResult() {
    }

    // Constructor with basic fields
    public RuleExecutionResult(String ruleId, String ruleName, boolean passed, String message) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.passed = passed;
        this.message = message;
    }

    // Constructor with all fields
    public RuleExecutionResult(String ruleId, String ruleName, boolean passed, String message, 
                              String condition, long executionTimeMs, String severity, String category) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.passed = passed;
        this.message = message;
        this.condition = condition;
        this.executionTimeMs = executionTimeMs;
        this.severity = severity;
        this.category = category;
    }

    // Getters and setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "RuleExecutionResult{" +
                "ruleId='" + ruleId + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", passed=" + passed +
                ", message='" + message + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}
