package dev.mars.rulesengine.core.engine.model;

import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMetrics;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the result of evaluating a rule or rule group.
 * This class contains information about the rule that was evaluated,
 * whether it was triggered, and any message associated with the result.
 */
public class RuleResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String ruleName;
    private final String message;
    private final boolean triggered;
    private final Instant timestamp;
    private final ResultType resultType;
    private final RulePerformanceMetrics performanceMetrics;

    /**
     * Enum representing the type of result.
     */
    public enum ResultType {
        /** A rule was matched/triggered */
        MATCH,
        /** No rule was matched/triggered */
        NO_MATCH,
        /** No rules were provided for evaluation */
        NO_RULES,
        /** An error occurred during rule evaluation */
        ERROR
    }

    /**
     * Create a new rule result with the specified parameters.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param triggered Whether the rule was triggered (true) or not (false)
     * @param resultType The type of result
     */
    public RuleResult(String ruleName, String message, boolean triggered, ResultType resultType) {
        this.id = UUID.randomUUID();
        this.ruleName = ruleName;
        this.message = message;
        this.triggered = triggered;
        this.timestamp = Instant.now();
        this.resultType = resultType;
        this.performanceMetrics = null; // No performance metrics for basic constructor
    }

    /**
     * Create a new rule result with the specified parameters including performance metrics.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param triggered Whether the rule was triggered (true) or not (false)
     * @param resultType The type of result
     * @param performanceMetrics The performance metrics for this rule evaluation
     */
    public RuleResult(String ruleName, String message, boolean triggered, ResultType resultType, RulePerformanceMetrics performanceMetrics) {
        this.id = UUID.randomUUID();
        this.ruleName = ruleName;
        this.message = message;
        this.triggered = triggered;
        this.timestamp = Instant.now();
        this.resultType = resultType;
        this.performanceMetrics = performanceMetrics;
    }

    /**
     * Create a new rule result with the specified parameters.
     * The rule is considered triggered if resultType is MATCH.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param resultType The type of result
     */
    public RuleResult(String ruleName, String message, ResultType resultType) {
        this(ruleName, message, resultType == ResultType.MATCH, resultType);
    }

    /**
     * Create a new rule result with the specified parameters including performance metrics.
     * The rule is considered triggered if resultType is MATCH.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param resultType The type of result
     * @param performanceMetrics The performance metrics for this rule evaluation
     */
    public RuleResult(String ruleName, String message, ResultType resultType, RulePerformanceMetrics performanceMetrics) {
        this(ruleName, message, resultType == ResultType.MATCH, resultType, performanceMetrics);
    }

    /**
     * Create a new rule result for a rule that was triggered.
     *
     * @param ruleName The name of the rule that was triggered
     * @param message The message associated with the rule
     * @return A new RuleResult instance
     */
    public static RuleResult match(String ruleName, String message) {
        return new RuleResult(ruleName, message, true, ResultType.MATCH);
    }

    /**
     * Create a new rule result for a rule that was triggered with performance metrics.
     *
     * @param ruleName The name of the rule that was triggered
     * @param message The message associated with the rule
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @return A new RuleResult instance
     */
    public static RuleResult match(String ruleName, String message, RulePerformanceMetrics performanceMetrics) {
        return new RuleResult(ruleName, message, true, ResultType.MATCH, performanceMetrics);
    }

    /**
     * Create a new rule result for when no rule was matched.
     * 
     * @return A new RuleResult instance
     */
    public static RuleResult noMatch() {
        return new RuleResult("no-match", "No matching rules found", false, ResultType.NO_MATCH);
    }

    /**
     * Create a new rule result for when no rules were provided.
     * 
     * @return A new RuleResult instance
     */
    public static RuleResult noRules() {
        return new RuleResult("no-rule", "No rules provided", false, ResultType.NO_RULES);
    }

    /**
     * Create a new rule result for when an error occurred during rule evaluation.
     * 
     * @param ruleName The name of the rule that caused the error
     * @param errorMessage The error message
     * @return A new RuleResult instance
     */
    public static RuleResult error(String ruleName, String errorMessage) {
        return new RuleResult(ruleName, errorMessage, false, ResultType.ERROR);
    }

    /**
     * Constructor for backward compatibility.
     * This constructor tries to determine the result type based on the ruleName.
     *
     * @param ruleName The name of the rule
     * @param message The message associated with the rule
     */
    public RuleResult(String ruleName, String message) {
        this.id = UUID.randomUUID();
        this.ruleName = ruleName;
        this.message = message;
        this.timestamp = Instant.now();
        this.performanceMetrics = null; // No performance metrics for backward compatibility

        // Try to determine the result type based on the ruleName
        if ("no-rule".equals(ruleName)) {
            this.resultType = ResultType.NO_RULES;
            this.triggered = false;
        } else if ("no-match".equals(ruleName)) {
            this.resultType = ResultType.NO_MATCH;
            this.triggered = false;
        } else {
            this.resultType = ResultType.MATCH;
            this.triggered = true;
        }
    }

    /**
     * Get the unique identifier of this result.
     * 
     * @return The UUID of this result
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the name of the rule that was evaluated.
     * 
     * @return The rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Get the message associated with the rule.
     * 
     * @return The rule message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Check if the rule was triggered.
     * 
     * @return true if the rule was triggered, false otherwise
     */
    public boolean isTriggered() {
        return triggered;
    }

    /**
     * Get the timestamp when this result was created.
     * 
     * @return The timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Get the type of this result.
     *
     * @return The result type
     */
    public ResultType getResultType() {
        return resultType;
    }

    /**
     * Get the performance metrics for this rule evaluation.
     *
     * @return The performance metrics, or null if not available
     */
    public RulePerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }

    /**
     * Check if performance metrics are available for this result.
     *
     * @return true if performance metrics are available, false otherwise
     */
    public boolean hasPerformanceMetrics() {
        return performanceMetrics != null;
    }

    @Override
    public String toString() {
        return "RuleResult{" +
                "id=" + id +
                ", ruleName='" + ruleName + '\'' +
                ", message='" + message + '\'' +
                ", triggered=" + triggered +
                ", resultType=" + resultType +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleResult that = (RuleResult) o;
        return triggered == that.triggered &&
                Objects.equals(id, that.id) &&
                Objects.equals(ruleName, that.ruleName) &&
                Objects.equals(message, that.message) &&
                resultType == that.resultType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ruleName, message, triggered, resultType);
    }
}