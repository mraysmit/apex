package dev.mars.apex.engine.model;

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
 * This class contains information about the rule that was evaluated,
 * whether it was triggered, and any message associated with the result.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class RuleResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String ruleId;  // The rule's ID from YAML configuration
    private final String ruleName;
    private final String message;
    private final String severity;
    private final boolean triggered;
    private final Instant timestamp;
    private final ResultType resultType;
    private final RulePerformanceMetrics performanceMetrics;

    // New fields for comprehensive evaluation results
    private final Map<String, Object> enrichedData;
    private final List<String> failureMessages;
    private final boolean success;

    // Error and Success Code Support
    private final String successCode;
    private final String errorCode;
    private final List<String> mapToField;

    // Child results for composite evaluations (e.g., evaluateSequential)
    private final List<RuleResult> childResults;

    // New field for execution trace
    private List<ExecutionStep> executionPath = new ArrayList<>();


    /**
     * Private all-fields constructor used exclusively by the Builder.
     * All public constructors remain for backward compatibility.
     */
    private RuleResult(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.ruleId = builder.ruleId;
        this.ruleName = builder.ruleName;
        this.message = builder.message;
        this.severity = builder.severity != null ? builder.severity : SeverityConstants.INFO;
        this.triggered = builder.triggered;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.resultType = builder.resultType;
        this.performanceMetrics = builder.performanceMetrics;
        this.enrichedData = builder.enrichedData != null ? new HashMap<>(builder.enrichedData) : new HashMap<>();
        this.failureMessages = builder.failureMessages != null ? new ArrayList<>(builder.failureMessages) : new ArrayList<>();
        this.success = builder.success;
        this.successCode = builder.successCode;
        this.errorCode = builder.errorCode;
        this.mapToField = builder.mapToField;
        this.childResults = builder.childResults != null ? new ArrayList<>(builder.childResults) : new ArrayList<>();
        this.executionPath = builder.executionPath != null ? new ArrayList<>(builder.executionPath) : new ArrayList<>();
    }

    /**
     * Create a new Builder for constructing RuleResult instances.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a Builder pre-populated with values from this RuleResult.
     * Useful for creating modified copies of an existing result.
     * Note: generates a new UUID and timestamp for the copy.
     *
     * @return a Builder pre-populated with this result's values
     */
    public Builder toBuilder() {
        Builder b = new Builder();
        // Note: id and timestamp are NOT copied — each result gets fresh values
        b.ruleId = this.ruleId;
        b.ruleName = this.ruleName;
        b.message = this.message;
        b.severity = this.severity;
        b.triggered = this.triggered;
        b.resultType = this.resultType;
        b.performanceMetrics = this.performanceMetrics;
        b.enrichedData = this.enrichedData != null ? new HashMap<>(this.enrichedData) : new HashMap<>();
        b.failureMessages = this.failureMessages != null ? new ArrayList<>(this.failureMessages) : new ArrayList<>();
        b.success = this.success;
        b.successCode = this.successCode;
        b.errorCode = this.errorCode;
        b.mapToField = this.mapToField;
        b.childResults = this.childResults != null ? new ArrayList<>(this.childResults) : new ArrayList<>();
        b.executionPath = this.executionPath != null ? new ArrayList<>(this.executionPath) : new ArrayList<>();
        return b;
    }

    /**
     * Builder for constructing RuleResult instances with a fluent API.
     * Replaces the need for 12+ parameter constructors.
     *
     * <p>Usage example:</p>
     * <pre>{@code
     * RuleResult result = RuleResult.builder()
     *     .ruleName("trade-validation")
     *     .message("Trade passed validation")
     *     .severity(SeverityConstants.INFO)
     *     .triggered(true)
     *     .resultType(ResultType.MATCH)
     *     .enrichedData(enrichments)
     *     .success(true)
     *     .build();
     * }</pre>
     *
     * <p>To create a modified copy:</p>
     * <pre>{@code
     * RuleResult updated = existingResult.toBuilder()
     *     .message("Updated message")
     *     .enrichedData(mergedData)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private UUID id;
        private String ruleId;
        private String ruleName;
        private String message;
        private String severity;
        private boolean triggered;
        private Instant timestamp;
        private ResultType resultType;
        private RulePerformanceMetrics performanceMetrics;
        private Map<String, Object> enrichedData;
        private List<String> failureMessages;
        private boolean success;
        private String successCode;
        private String errorCode;
        private List<String> mapToField;
        private List<RuleResult> childResults;
        private List<ExecutionStep> executionPath;

        private Builder() {
            // defaults
            this.severity = SeverityConstants.INFO;
            this.success = true;
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder triggered(boolean triggered) {
            this.triggered = triggered;
            return this;
        }

        public Builder resultType(ResultType resultType) {
            this.resultType = resultType;
            return this;
        }

        public Builder performanceMetrics(RulePerformanceMetrics performanceMetrics) {
            this.performanceMetrics = performanceMetrics;
            return this;
        }

        public Builder enrichedData(Map<String, Object> enrichedData) {
            this.enrichedData = enrichedData;
            return this;
        }

        public Builder failureMessages(List<String> failureMessages) {
            this.failureMessages = failureMessages;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder successCode(String successCode) {
            this.successCode = successCode;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder mapToField(List<String> mapToField) {
            this.mapToField = mapToField;
            return this;
        }

        public Builder childResults(List<RuleResult> childResults) {
            this.childResults = childResults;
            return this;
        }

        public Builder executionPath(List<ExecutionStep> executionPath) {
            this.executionPath = executionPath;
            return this;
        }

        /**
         * Build the RuleResult instance.
         *
         * @return a new RuleResult
         * @throws IllegalStateException if resultType is not set
         */
        public RuleResult build() {
            if (resultType == null) {
                throw new IllegalStateException("resultType must be set");
            }
            return new RuleResult(this);
        }
    }

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
        ERROR,
        /** An enrichment operation failed */
        ENRICHMENT_FAILURE
    }

    /**
     * Create a new rule result for a rule that was triggered.
     *
     * @param ruleName The name of the rule that was triggered
     * @param message The message associated with the rule
     * @return A new RuleResult instance
     */
    public static RuleResult match(String ruleName, String message) {
        return builder().ruleName(ruleName).message(message).triggered(true).resultType(ResultType.MATCH).build();
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
        return builder().ruleName(ruleName).message(message).triggered(true).resultType(ResultType.MATCH).performanceMetrics(performanceMetrics).build();
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
        return builder().ruleName(ruleName).message(message).severity(severity).triggered(true).resultType(ResultType.MATCH).build();
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
        return builder().ruleName(ruleName).message(message).severity(severity).triggered(true).resultType(ResultType.MATCH).performanceMetrics(performanceMetrics).build();
    }

    /**
     * Create a new rule result for when no rule was matched.
     *
     * @return A new RuleResult instance
     */
    public static RuleResult noMatch() {
        return builder().ruleName("no-match").message("No matching rules found").triggered(false).resultType(ResultType.NO_MATCH).build();
    }

    /**
     * Create a new rule result for when no rule was matched, with performance metrics.
     *
     * @param performanceMetrics The performance metrics for this rule evaluation
     * @return A new RuleResult instance
     */
    public static RuleResult noMatch(RulePerformanceMetrics performanceMetrics) {
        return builder().ruleName("no-match").message("No matching rules found").triggered(false).resultType(ResultType.NO_MATCH).performanceMetrics(performanceMetrics).build();
    }

    /**
     * Create a new rule result for when no rule was matched, with specific rule name, message, and severity.
     *
     * @param ruleName The name of the rule that was not matched
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @return A new RuleResult instance
     */
    public static RuleResult noMatch(String ruleName, String message, String severity) {
        return builder().ruleName(ruleName).message(message).severity(severity).triggered(false).resultType(ResultType.NO_MATCH).build();
    }

    /**
     * Create a new rule result for when no rules were provided.
     * 
     * @return A new RuleResult instance
     */
    public static RuleResult noRules() {
        return builder().ruleName("no-rule").message("No rules provided").triggered(false).resultType(ResultType.NO_RULES).build();
    }

    /**
     * Create a new rule result for when an error occurred during rule evaluation.
     *
     * @param ruleName The name of the rule that caused the error
     * @param errorMessage The error message
     * @return A new RuleResult instance
     */
    public static RuleResult error(String ruleName, String errorMessage) {
        return builder().ruleName(ruleName).message(errorMessage).triggered(false).resultType(ResultType.ERROR).success(false).build();
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
        return builder().ruleName(ruleName).message(errorMessage).severity(severity).triggered(false).resultType(ResultType.ERROR).success(false).build();
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
        return builder().ruleName(ruleName).message(errorMessage).triggered(false).resultType(ResultType.ERROR).performanceMetrics(performanceMetrics).success(false).build();
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
        return builder().ruleName(ruleName).message(errorMessage).severity(severity).triggered(false).resultType(ResultType.ERROR).performanceMetrics(performanceMetrics).success(false).build();
    }

    /**
     * Create a new rule result for when an error occurred, with severity and error code.
     * Phase 2 Enhancement: Supports structured error codes for programmatic error identification.
     *
     * @param ruleName The name of the rule/component that caused the error
     * @param errorMessage The error message
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param errorCode The APEX error code (e.g., APEX-RULE-001)
     * @return A new RuleResult instance
     */
    public static RuleResult errorWithCode(String ruleName, String errorMessage, String severity, String errorCode) {
        return builder().ruleName(ruleName).message(errorMessage).severity(severity).errorCode(errorCode).triggered(false).resultType(ResultType.ERROR).success(false).build();
    }

    /**
     * Create a new rule result for when an error occurred, with severity, error code, and performance metrics.
     * Phase 2 Enhancement: Supports structured error codes for programmatic error identification.
     *
     * @param ruleName The name of the rule/component that caused the error
     * @param errorMessage The error message
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param errorCode The APEX error code (e.g., APEX-RULE-001)
     * @param performanceMetrics The performance metrics for this evaluation
     * @return A new RuleResult instance
     */
    public static RuleResult errorWithCode(String ruleName, String errorMessage, String severity, String errorCode,
                                           RulePerformanceMetrics performanceMetrics) {
        return builder().ruleName(ruleName).message(errorMessage).severity(severity).errorCode(errorCode).triggered(false).resultType(ResultType.ERROR).performanceMetrics(performanceMetrics).success(false).build();
    }

    // Factory methods for error and success codes

    /**
     * Create a new rule result for a rule that was triggered with success code.
     * Phase 4 Enhancement: Supports success codes for rule matches.
     *
     * @param ruleName The name of the rule that was triggered
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param successCode The code evaluated when rule succeeded
     * @return A new RuleResult instance
     */
    public static RuleResult matchWithCode(String ruleName, String message, String severity, String successCode) {
        return builder().ruleName(ruleName).message(message).severity(severity).triggered(true).resultType(ResultType.MATCH).success(true).successCode(successCode).build();
    }

    /**
     * Create a new rule result for when no rule was matched with error code.
     * Phase 4 Enhancement: Supports error codes for rule non-matches.
     *
     * @param ruleName The name of the rule that was not matched
     * @param message The message associated with the rule
     * @param severity The severity level (ERROR, WARNING, INFO)
     * @param errorCode The code evaluated when rule failed
     * @return A new RuleResult instance
     */
    public static RuleResult noMatchWithCode(String ruleName, String message, String severity, String errorCode) {
        return builder().ruleName(ruleName).message(message).severity(severity).triggered(false).resultType(ResultType.NO_MATCH).success(true).errorCode(errorCode).build();
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
        return builder().ruleName("enrichment").message("Enrichment completed successfully").severity(severity).triggered(true).resultType(ResultType.MATCH).enrichedData(enrichedData).success(true).build();
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
        return builder().ruleName("enrichment").message("Required field enrichment failed").severity(severity).triggered(false).resultType(ResultType.ENRICHMENT_FAILURE).enrichedData(enrichedData).failureMessages(failureMessages).success(false).build();
    }

    /**
     * Create a new rule result for failed enrichment evaluation with specified severity and error code.
     * Phase 2 Enhancement: Supports structured error codes for programmatic error identification.
     *
     * @param failureMessages List of failure messages from enrichments
     * @param enrichedData The enriched data map (may be partial if some enrichments failed)
     * @param severity The severity level of the enrichment failure
     * @param errorCode The APEX error code (e.g., APEX-ENRICH-001)
     * @return A new RuleResult instance representing failed enrichment
     */
    public static RuleResult enrichmentFailure(List<String> failureMessages, Map<String, Object> enrichedData, String severity, String errorCode) {
        return builder().ruleName("enrichment").message("Required field enrichment failed").severity(severity).errorCode(errorCode).triggered(false).resultType(ResultType.ENRICHMENT_FAILURE).enrichedData(enrichedData).failureMessages(failureMessages).success(false).build();
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
        return builder().ruleName(ruleName).message(ruleMessage).triggered(true).resultType(ResultType.MATCH).enrichedData(enrichedData).success(true).build();
    }

    /**
     * Create a new rule result for successful complete evaluation with child results.
     * This method is used when both enrichments and rules complete successfully and we want to
     * preserve individual rule results.
     *
     * @param enrichedData The enriched data map containing all enrichment results
     * @param ruleName The name of the final rule that was evaluated
     * @param ruleMessage The message from the final rule evaluation
     * @param childResults List of individual rule results
     * @return A new RuleResult instance representing successful complete evaluation with child results
     */
    public static RuleResult evaluationSuccess(Map<String, Object> enrichedData, String ruleName, String ruleMessage,
                                              List<RuleResult> childResults) {
        return builder().ruleName(ruleName).message(ruleMessage).triggered(true).resultType(ResultType.MATCH).enrichedData(enrichedData).success(true).childResults(childResults).build();
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
        return builder().ruleName(ruleName).message(errorMessage).triggered(false).resultType(ResultType.ERROR).enrichedData(enrichedData).failureMessages(failureMessages).success(false).build();
    }

    /**
     * Create a new rule result for failed complete evaluation (enrichments + rules) with severity.
     * This method is used when either enrichments or rules fail during evaluation and we want to preserve
     * the original rule's severity.
     *
     * @param failureMessages List of failure messages from enrichments and rules
     * @param enrichedData The enriched data map (may be partial if enrichments failed)
     * @param ruleName The name of the rule or enrichment that failed
     * @param errorMessage The primary error message
     * @param severity The severity level to preserve (ERROR, WARNING, INFO)
     * @return A new RuleResult instance representing failed complete evaluation with preserved severity
     */
    public static RuleResult evaluationFailure(List<String> failureMessages, Map<String, Object> enrichedData,
                                              String ruleName, String errorMessage, String severity) {
        return builder().ruleName(ruleName).message(errorMessage).severity(severity).triggered(false).resultType(ResultType.ERROR).enrichedData(enrichedData).failureMessages(failureMessages).success(false).build();
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
     * Get the rule ID from YAML configuration.
     *
     * @return The rule ID, or null if not set
     */
    public String getRuleId() {
        return ruleId;
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
     * Get the child results from composite evaluations.
     *
     * @return List of child RuleResult objects, or empty list if none
     */
    public List<RuleResult> getChildResults() {
        return childResults != null ? new ArrayList<>(childResults) : new ArrayList<>();
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

    /**
     * Get the success code for this rule result.
     * Phase 4 Enhancement: Returns the code evaluated when rule condition was true.
     *
     * @return The success code, or null if no code was specified or rule did not match
     */
    public String getSuccessCode() {
        return successCode;
    }

    /**
     * Get the error code for this rule result.
     * Phase 4 Enhancement: Returns the code evaluated when rule condition was false.
     *
     * @return The error code, or null if no code was specified or rule matched
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Get the field mapping expressions for this rule result.
     * Phase 4 Enhancement: Returns field mapping expressions that were applied.
     *
     * @return The field mapping expressions, or null if no mapping was specified
     */
    public List<String> getMapToField() {
        return mapToField;
    }

    // New API methods for comprehensive evaluation results

    /**
     * Get the execution path (trace) of the evaluation.
     *
     * @return List of execution steps
     */
    public List<ExecutionStep> getExecutionPath() {
        return executionPath != null ? new ArrayList<>(executionPath) : new ArrayList<>();
    }

    /**
     * Set the execution path (trace) of the evaluation.
     *
     * @param executionPath List of execution steps
     */
    public void setExecutionPath(List<ExecutionStep> executionPath) {
        this.executionPath = executionPath != null ? new ArrayList<>(executionPath) : new ArrayList<>();
    }

    /**
     * New API: Returns the matched rule/group name when a rule/group was triggered.
     * Returns null when no rule matched or evaluation failed.
     */
    public String getRuleMatchedName() {
        return isTriggered() ? this.ruleName : null;
    }

    /**
     * New API: Returns the last failed group name when evaluation failed.
     * For backward compatibility, this maps to ruleName when not triggered and a name is present.
     */
    public String getLastFailedGroupName() {
        return !isTriggered() ? this.ruleName : null;
    }

    /**
     * New API: Returns the last failed group message when evaluation failed.
     * For backward compatibility, this maps to message when not triggered.
     */
    public String getLastFailedGroupMessage() {
        return !isTriggered() ? this.message : null;
    }

    /**
     * New API: Returns the highest failed severity when evaluation failed.
     * For now, this returns the severity field when not triggered, otherwise null.
     */
    public String getHighestFailedSeverity() {
        return !isTriggered() ? this.severity : null;
    }

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
     * Check if the result type indicates an error condition.
     * Returns true for both ERROR and ENRICHMENT_FAILURE result types.
     * This method provides a convenient way to check for any error without
     * explicitly comparing against all error-related ResultType values.
     *
     * @return true if resultType is ERROR or ENRICHMENT_FAILURE, false otherwise
     */
    public boolean isError() {
        return resultType == ResultType.ERROR || resultType == ResultType.ENRICHMENT_FAILURE;
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
