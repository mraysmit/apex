package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

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
 * Represents the result of evaluating a rule or rule group.
 *
 * This enum is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Represents the result of evaluating a rule or rule group.
 * This class contains information about the rule that was evaluated,
 * whether it was triggered, and any message associated with the result.
 */
public class RuleResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String ruleMatchedName;  // Name of the rule/group that matched (only meaningful when triggered=true)
    private final String message;
    private final String severity;
    private final boolean triggered;
    private final Instant timestamp;
    private final ResultType resultType;

    // Failure diagnostic information (only meaningful when triggered=false)
    private final String lastFailedGroupName;     // Which group failed with highest severity
    private final String lastFailedGroupMessage;  // Message from the failed group
    private final String highestFailedSeverity;   // Highest severity from failed groups
    private final RulePerformanceMetrics performanceMetrics;

    // New fields for comprehensive evaluation results
    private final Map<String, Object> enrichedData;
    private final List<String> failureMessages;
    private final boolean success;

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
        this(ruleName, message, SeverityConstants.INFO, triggered, resultType); // Default severity for backward compatibility
    }

    /**
     * Create a new rule result with the specified parameters including severity.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param triggered Whether the rule was triggered (true) or not (false)
     * @param resultType The type of result
     */
    public RuleResult(String ruleName, String message, String severity, boolean triggered, ResultType resultType) {
        this.id = UUID.randomUUID();
        this.ruleMatchedName = triggered ? ruleName : null;  // Only set if rule actually matched
        this.message = message;
        this.severity = severity != null ? severity : SeverityConstants.INFO; // Default to INFO if null
        this.triggered = triggered;
        this.timestamp = Instant.now();
        this.resultType = resultType;
        this.performanceMetrics = null; // No performance metrics for basic constructor

        // Initialize failure diagnostic fields
        this.lastFailedGroupName = !triggered ? ruleName : null;  // Use ruleName as failed group for backward compatibility
        this.lastFailedGroupMessage = !triggered ? message : null;
        this.highestFailedSeverity = !triggered ? severity : null;

        // Initialize new fields with defaults for backward compatibility
        this.enrichedData = new HashMap<>();
        this.failureMessages = new ArrayList<>();
        this.success = (resultType == ResultType.MATCH || resultType == ResultType.NO_MATCH);
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
        this(ruleName, message, SeverityConstants.INFO, triggered, resultType, performanceMetrics); // Default severity
    }

    /**
     * Create a new rule result with the specified parameters including performance metrics and severity.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param triggered Whether the rule was triggered (true) or not (false)
     * @param resultType The type of result
     * @param performanceMetrics The performance metrics for this rule evaluation
     */
    public RuleResult(String ruleName, String message, String severity, boolean triggered, ResultType resultType, RulePerformanceMetrics performanceMetrics) {
        this.id = UUID.randomUUID();
        this.ruleMatchedName = triggered ? ruleName : null;  // Only set if rule actually matched
        this.message = message;
        this.severity = severity != null ? severity : SeverityConstants.INFO; // Default to INFO if null
        this.triggered = triggered;
        this.timestamp = Instant.now();
        this.resultType = resultType;
        this.performanceMetrics = performanceMetrics;

        // Initialize failure diagnostic fields
        this.lastFailedGroupName = !triggered ? ruleName : null;  // Use ruleName as failed group for backward compatibility
        this.lastFailedGroupMessage = !triggered ? message : null;
        this.highestFailedSeverity = !triggered ? severity : null;

        // Initialize new fields with defaults for backward compatibility
        this.enrichedData = new HashMap<>();
        this.failureMessages = new ArrayList<>();
        this.success = (resultType == ResultType.MATCH || resultType == ResultType.NO_MATCH);
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
     * Create a new rule result with comprehensive evaluation information.
     * This constructor supports the complete APEX evaluation workflow including enrichments.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param triggered Whether the rule was triggered (true) or not (false)
     * @param resultType The type of result
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @param enrichedData The enriched data map containing all enrichment results
     * @param failureMessages List of failure messages from enrichments and rules
     * @param success Overall success status of the evaluation
     */
    public RuleResult(String ruleName, String message, boolean triggered, ResultType resultType,
                     RulePerformanceMetrics performanceMetrics, Map<String, Object> enrichedData,
                     List<String> failureMessages, boolean success) {
        this(ruleName, message, SeverityConstants.INFO, triggered, resultType, performanceMetrics, enrichedData, failureMessages, success); // Default severity
    }

    /**
     * Create a new rule result with comprehensive evaluation information including severity.
     * This constructor supports the complete APEX evaluation workflow including enrichments and severity.
     *
     * @param ruleName The name of the rule that was evaluated
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param triggered Whether the rule was triggered (true) or not (false)
     * @param resultType The type of result
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @param enrichedData The enriched data map containing all enrichment results
     * @param failureMessages List of failure messages from enrichments and rules
     * @param success Overall success status of the evaluation
     */
    public RuleResult(String ruleName, String message, String severity, boolean triggered, ResultType resultType,
                     RulePerformanceMetrics performanceMetrics, Map<String, Object> enrichedData,
                     List<String> failureMessages, boolean success) {
        this.id = UUID.randomUUID();
        this.ruleMatchedName = triggered ? ruleName : null;  // Only set if rule actually matched
        this.message = message;
        this.severity = severity != null ? severity : SeverityConstants.INFO; // Default to INFO if null
        this.triggered = triggered;
        this.timestamp = Instant.now();
        this.resultType = resultType;
        this.performanceMetrics = performanceMetrics;

        // Initialize failure diagnostic fields
        this.lastFailedGroupName = !triggered ? ruleName : null;  // Use ruleName as failed group for backward compatibility
        this.lastFailedGroupMessage = !triggered ? message : null;
        this.highestFailedSeverity = !triggered ? severity : null;

        // Initialize new fields with provided values
        this.enrichedData = enrichedData != null ? new HashMap<>(enrichedData) : new HashMap<>();
        this.failureMessages = failureMessages != null ? new ArrayList<>(failureMessages) : new ArrayList<>();
        this.success = success;
    }

    /**
     * Create a new rule result with failure diagnostic information.
     * This constructor is used for no-match results that need to provide failure diagnostics.
     *
     * @param ruleMatchedName The name of the matched rule (null for no-match cases)
     * @param message The message associated with the result
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param triggered Whether the rule was triggered (should be false for failure diagnostics)
     * @param resultType The type of result
     * @param lastFailedGroupName The name of the group that failed with highest severity
     * @param lastFailedGroupMessage The message from the failed group
     * @param highestFailedSeverity The highest severity from failed groups
     */
    public RuleResult(String ruleMatchedName, String message, String severity, boolean triggered, ResultType resultType,
                     String lastFailedGroupName, String lastFailedGroupMessage, String highestFailedSeverity) {
        this.id = UUID.randomUUID();
        this.ruleMatchedName = ruleMatchedName;
        this.message = message;
        this.severity = severity != null ? severity : SeverityConstants.INFO;
        this.triggered = triggered;
        this.timestamp = Instant.now();
        this.resultType = resultType;
        this.performanceMetrics = null;

        // Set failure diagnostic fields
        this.lastFailedGroupName = lastFailedGroupName;
        this.lastFailedGroupMessage = lastFailedGroupMessage;
        this.highestFailedSeverity = highestFailedSeverity;

        // Initialize other fields with defaults
        this.enrichedData = new HashMap<>();
        this.failureMessages = new ArrayList<>();
        this.success = (resultType == ResultType.MATCH || resultType == ResultType.NO_MATCH);
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
     * Create a new rule result for a rule that was triggered with severity.
     *
     * @param ruleName The name of the rule that was triggered
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @return A new RuleResult instance
     */
    public static RuleResult match(String ruleName, String message, String severity) {
        return new RuleResult(ruleName, message, severity, true, ResultType.MATCH);
    }

    /**
     * Create a new rule result for a rule that was triggered with severity and performance metrics.
     *
     * @param ruleName The name of the rule that was triggered
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @return A new RuleResult instance
     */
    public static RuleResult match(String ruleName, String message, String severity, RulePerformanceMetrics performanceMetrics) {
        return new RuleResult(ruleName, message, severity, true, ResultType.MATCH, performanceMetrics);
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
     * Create a new rule result for when no rule was matched, with performance metrics.
     *
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @return A new RuleResult instance
     */
    public static RuleResult noMatch(RulePerformanceMetrics performanceMetrics) {
        return new RuleResult("no-match", "No matching rules found", false, ResultType.NO_MATCH, performanceMetrics);
    }

    /**
     * Create a new rule result for when no rule was matched, with specific rule name, message, and severity.
     *
     * @deprecated Use noMatchWithFailureInfo() for clearer semantics when providing failure diagnostic information.
     * This method creates semantic confusion by using ruleName for failure information.
     * @param ruleName The name of the rule that was not matched
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @return A new RuleResult instance
     */
    @Deprecated
    public static RuleResult noMatch(String ruleName, String message, String severity) {
        return new RuleResult(ruleName, message, severity, false, ResultType.NO_MATCH);
    }

    /**
     * Create a new rule result for when no rule was matched, with failure diagnostic information.
     * This method provides clear semantics for failure cases with diagnostic details.
     *
     * @param failedGroupName The name of the group that failed with highest severity
     * @param failedGroupMessage The message from the failed group
     * @param highestFailedSeverity The highest severity from failed groups
     * @return A new RuleResult instance with failure diagnostics
     */
    public static RuleResult noMatchWithFailureInfo(String failedGroupName, String failedGroupMessage, String highestFailedSeverity) {
        // Use the new constructor that properly handles failure diagnostic information
        // Use the highest failed severity as the overall result severity
        return new RuleResult(null, "No matching rules found", highestFailedSeverity, false, ResultType.NO_MATCH,
                             failedGroupName, failedGroupMessage, highestFailedSeverity);
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
     * Create a new rule result for when an error occurred during rule evaluation, with severity.
     *
     * @param ruleName The name of the rule that caused the error
     * @param errorMessage The error message
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @return A new RuleResult instance
     */
    public static RuleResult error(String ruleName, String errorMessage, String severity) {
        return new RuleResult(ruleName, errorMessage, severity, false, ResultType.ERROR);
    }

    /**
     * Create a new rule result for when an error occurred during rule evaluation, with performance metrics.
     *
     * @param ruleName The name of the rule that caused the error
     * @param errorMessage The error message
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @return A new RuleResult instance
     */
    public static RuleResult error(String ruleName, String errorMessage, RulePerformanceMetrics performanceMetrics) {
        return new RuleResult(ruleName, errorMessage, false, ResultType.ERROR, performanceMetrics);
    }

    /**
     * Create a new rule result for when an error occurred during rule evaluation, with severity and performance metrics.
     *
     * @param ruleName The name of the rule that caused the error
     * @param errorMessage The error message
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @return A new RuleResult instance
     */
    public static RuleResult error(String ruleName, String errorMessage, String severity, RulePerformanceMetrics performanceMetrics) {
        return new RuleResult(ruleName, errorMessage, severity, false, ResultType.ERROR, performanceMetrics);
    }

    // New factory methods for enrichment results

    /**
     * Create a new rule result for successful enrichment evaluation.
     * This method is used when enrichments complete successfully.
     *
     * @param enrichedData The enriched data map containing all enrichment results
     * @return A new RuleResult instance representing successful enrichment
     */
    public static RuleResult enrichmentSuccess(Map<String, Object> enrichedData) {
        return enrichmentSuccess(enrichedData, SeverityConstants.INFO);
    }

    /**
     * Create a new rule result for successful enrichment evaluation with specified severity.
     * This method is used when all enrichments complete successfully with a specific severity level.
     *
     * @param enrichedData The enriched data map containing all enrichment results
     * @param severity The severity level of the enrichment result
     * @return A new RuleResult instance representing successful enrichment
     */
    public static RuleResult enrichmentSuccess(Map<String, Object> enrichedData, String severity) {
        return new RuleResult("enrichment", "Enrichment completed successfully",
                             severity, true, ResultType.MATCH, null, enrichedData, new ArrayList<>(), true);
    }

    /**
     * Create a new rule result for failed enrichment evaluation.
     * This method is used when enrichments fail due to required field mapping failures or other errors.
     *
     * @param failureMessages List of failure messages describing what went wrong
     * @param enrichedData The enriched data map (may be partial if some enrichments failed)
     * @return A new RuleResult instance representing failed enrichment
     */
    public static RuleResult enrichmentFailure(List<String> failureMessages, Map<String, Object> enrichedData) {
        return enrichmentFailure(failureMessages, enrichedData, SeverityConstants.ERROR);
    }

    /**
     * Create a new rule result for failed enrichment evaluation with specified severity.
     * This method is used when enrichments fail due to required field mapping failures or other errors.
     *
     * @param failureMessages List of failure messages from enrichments
     * @param enrichedData The enriched data map (may be partial if some enrichments failed)
     * @param severity The severity level of the enrichment failure
     * @return A new RuleResult instance representing failed enrichment
     */
    public static RuleResult enrichmentFailure(List<String> failureMessages, Map<String, Object> enrichedData, String severity) {
        return new RuleResult("enrichment", "Enrichment failed",
                             severity, false, ResultType.ERROR, null, enrichedData, failureMessages, false);
    }

    /**
     * Create a new rule result for successful complete evaluation (enrichments + rules).
     * This method is used when both enrichments and rules complete successfully.
     *
     * @param enrichedData The enriched data map containing all enrichment results
     * @param ruleName The name of the final rule that was evaluated
     * @param ruleMessage The message from the final rule evaluation
     * @return A new RuleResult instance representing successful complete evaluation
     */
    public static RuleResult evaluationSuccess(Map<String, Object> enrichedData, String ruleName, String ruleMessage) {
        return new RuleResult(ruleName, ruleMessage,
                             true, ResultType.MATCH, null, enrichedData, new ArrayList<>(), true);
    }

    /**
     * Create a new rule result for failed complete evaluation (enrichments + rules).
     * This method is used when either enrichments or rules fail during evaluation.
     *
     * @param failureMessages List of failure messages from enrichments and rules
     * @param enrichedData The enriched data map (may be partial if enrichments failed)
     * @param ruleName The name of the rule or enrichment that failed
     * @param errorMessage The primary error message
     * @return A new RuleResult instance representing failed complete evaluation
     */
    public static RuleResult evaluationFailure(List<String> failureMessages, Map<String, Object> enrichedData,
                                              String ruleName, String errorMessage) {
        return new RuleResult(ruleName, errorMessage,
                             false, ResultType.ERROR, null, enrichedData, failureMessages, false);
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
        this.message = message;
        this.severity = SeverityConstants.INFO; // Default severity for backward compatibility
        this.timestamp = Instant.now();
        this.performanceMetrics = null; // No performance metrics for backward compatibility

        // Try to determine the result type based on the ruleName
        if ("no-rule".equals(ruleName)) {
            this.resultType = ResultType.NO_RULES;
            this.triggered = false;
            this.ruleMatchedName = null;
            this.lastFailedGroupName = ruleName;
            this.lastFailedGroupMessage = message;
            this.highestFailedSeverity = SeverityConstants.INFO;
        } else if ("no-match".equals(ruleName)) {
            this.resultType = ResultType.NO_MATCH;
            this.triggered = false;
            this.ruleMatchedName = null;
            this.lastFailedGroupName = null;  // no-match means no specific group failed
            this.lastFailedGroupMessage = null;
            this.highestFailedSeverity = null;
        } else {
            this.resultType = ResultType.MATCH;
            this.triggered = true;
            this.ruleMatchedName = ruleName;
            this.lastFailedGroupName = null;
            this.lastFailedGroupMessage = null;
            this.highestFailedSeverity = null;
        }

        // Initialize new fields with defaults for backward compatibility
        this.enrichedData = new HashMap<>();
        this.failureMessages = new ArrayList<>();
        this.success = (this.resultType == ResultType.MATCH || this.resultType == ResultType.NO_MATCH);
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
     * @deprecated Use getRuleMatchedName() for clearer semantics. This method maintains backward compatibility
     * by returning the matched rule name when triggered=true, or failure diagnostic info when triggered=false.
     * @return The rule name (matched rule name or failure diagnostic info)
     */
    @Deprecated
    public String getRuleName() {
        // Backward compatibility: return matched name if triggered, otherwise return failure info
        if (triggered) {
            return ruleMatchedName;
        } else {
            // For backward compatibility, return failure diagnostic info
            return lastFailedGroupName != null ? lastFailedGroupName : "no-match";
        }
    }

    /**
     * Get the name of the rule or rule group that matched.
     * This method is only meaningful when isTriggered() returns true.
     *
     * @return The name of the matched rule/group, or null if no rule matched
     */
    public String getRuleMatchedName() {
        return ruleMatchedName;
    }

    /**
     * Get the name of the last failed group (highest severity failure).
     * This method is only meaningful when isTriggered() returns false.
     *
     * @return The name of the failed group, or null if no specific group failed
     */
    public String getLastFailedGroupName() {
        return lastFailedGroupName;
    }

    /**
     * Get the message from the last failed group (highest severity failure).
     * This method is only meaningful when isTriggered() returns false.
     *
     * @return The message from the failed group, or null if no specific group failed
     */
    public String getLastFailedGroupMessage() {
        return lastFailedGroupMessage;
    }

    /**
     * Get the highest severity from failed groups.
     * This method is only meaningful when isTriggered() returns false.
     *
     * @return The highest severity from failed groups, or null if no failures
     */
    public String getHighestFailedSeverity() {
        return highestFailedSeverity;
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
     * Get the severity level of the rule.
     *
     * @return The rule severity (ERROR, WARNING, INFO)
     */
    public String getSeverity() {
        return severity;
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

    // New API methods for comprehensive evaluation results

    /**
     * Check if all enrichments and rules succeeded.
     * This method provides programmatic access to the overall evaluation status.
     *
     * @return true if all operations succeeded, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if there were any failures during evaluation.
     * This method allows applications to detect failures without parsing logs.
     *
     * @return true if there were failures, false otherwise
     */
    public boolean hasFailures() {
        return !success || (failureMessages != null && !failureMessages.isEmpty());
    }

    /**
     * Get list of failure messages from enrichments and rules.
     * This method provides detailed error information for programmatic handling.
     *
     * @return List of failure messages, empty if no failures
     */
    public List<String> getFailureMessages() {
        return failureMessages != null ? new ArrayList<>(failureMessages) : new ArrayList<>();
    }

    /**
     * Get the enriched data map containing all enrichment results.
     * This method provides access to the data that was enriched during evaluation.
     *
     * @return Map of enriched data, empty if no enrichments
     */
    public Map<String, Object> getEnrichedData() {
        return enrichedData != null ? new HashMap<>(enrichedData) : new HashMap<>();
    }

    @Override
    public String toString() {
        return "RuleResult{" +
                "id=" + id +
                ", ruleMatchedName='" + ruleMatchedName + '\'' +
                ", message='" + message + '\'' +
                ", triggered=" + triggered +
                ", resultType=" + resultType +
                ", timestamp=" + timestamp +
                ", lastFailedGroupName='" + lastFailedGroupName + '\'' +
                ", highestFailedSeverity='" + highestFailedSeverity + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleResult that = (RuleResult) o;
        return triggered == that.triggered &&
                Objects.equals(id, that.id) &&
                Objects.equals(ruleMatchedName, that.ruleMatchedName) &&
                Objects.equals(message, that.message) &&
                Objects.equals(lastFailedGroupName, that.lastFailedGroupName) &&
                resultType == that.resultType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ruleMatchedName, message, triggered, resultType, lastFailedGroupName);
    }
}
