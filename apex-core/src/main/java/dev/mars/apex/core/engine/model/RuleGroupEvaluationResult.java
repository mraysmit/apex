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

package dev.mars.apex.core.engine.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates complete rule group evaluation results including:
 * - Overall group result (boolean)
 * - Individual rule results with severity
 * - Aggregated group severity
 * - Performance metrics
 * - Evaluation statistics
 * 
 * This class follows the Open/Closed Principle by providing a comprehensive
 * result model that can be extended without modifying existing code.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-23
 * @version 1.0
 */
public class RuleGroupEvaluationResult {
    
    private final String groupId;
    private final String groupName;
    private final boolean groupResult;
    private final List<RuleResult> individualResults;
    private final String aggregatedSeverity;
    private final boolean isAndOperator;
    private final Instant evaluationTimestamp;
    private final long evaluationDurationMs;
    
    // Evaluation statistics
    private final int totalRulesEvaluated;
    private final int rulesTriggered;
    private final int rulesFailed;
    
    /**
     * Create a new rule group evaluation result.
     * 
     * @param groupId The unique identifier of the rule group
     * @param groupName The name of the rule group
     * @param groupResult The overall result of the group evaluation
     * @param individualResults List of individual rule results
     * @param aggregatedSeverity The aggregated severity for the group
     * @param isAndOperator Whether this was an AND (true) or OR (false) group
     * @param evaluationDurationMs Duration of evaluation in milliseconds
     */
    public RuleGroupEvaluationResult(String groupId, String groupName, boolean groupResult,
                                   List<RuleResult> individualResults, String aggregatedSeverity,
                                   boolean isAndOperator, long evaluationDurationMs) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupResult = groupResult;
        this.individualResults = individualResults != null ? 
            new ArrayList<>(individualResults) : new ArrayList<>();
        this.aggregatedSeverity = aggregatedSeverity != null ? aggregatedSeverity : "INFO";
        this.isAndOperator = isAndOperator;
        this.evaluationTimestamp = Instant.now();
        this.evaluationDurationMs = evaluationDurationMs;
        
        // Calculate statistics
        this.totalRulesEvaluated = this.individualResults.size();
        this.rulesTriggered = (int) this.individualResults.stream()
            .mapToLong(r -> r.isTriggered() ? 1 : 0)
            .sum();
        this.rulesFailed = this.totalRulesEvaluated - this.rulesTriggered;
    }
    
    /**
     * Get the unique identifier of the rule group.
     * 
     * @return The group ID
     */
    public String getGroupId() {
        return groupId;
    }
    
    /**
     * Get the name of the rule group.
     * 
     * @return The group name
     */
    public String getGroupName() {
        return groupName;
    }
    
    /**
     * Get the overall result of the group evaluation.
     * 
     * @return true if the group condition was satisfied
     */
    public boolean isGroupResult() {
        return groupResult;
    }
    
    /**
     * Get the individual rule results from the group evaluation.
     * Returns an immutable view to prevent external modification.
     * 
     * @return List of individual rule results
     */
    public List<RuleResult> getIndividualResults() {
        return Collections.unmodifiableList(individualResults);
    }
    
    /**
     * Get the aggregated severity for the group.
     * 
     * @return The aggregated severity level (ERROR, WARNING, INFO)
     */
    public String getAggregatedSeverity() {
        return aggregatedSeverity;
    }
    
    /**
     * Check if this was an AND group.
     * 
     * @return true for AND groups, false for OR groups
     */
    public boolean isAndOperator() {
        return isAndOperator;
    }
    
    /**
     * Get the evaluation timestamp.
     * 
     * @return When the evaluation was completed
     */
    public Instant getEvaluationTimestamp() {
        return evaluationTimestamp;
    }
    
    /**
     * Get the evaluation duration in milliseconds.
     * 
     * @return Duration of the evaluation
     */
    public long getEvaluationDurationMs() {
        return evaluationDurationMs;
    }
    
    /**
     * Get the total number of rules evaluated.
     * 
     * @return Total rules evaluated
     */
    public int getTotalRulesEvaluated() {
        return totalRulesEvaluated;
    }
    
    /**
     * Get the number of rules that were triggered (passed).
     * 
     * @return Number of triggered rules
     */
    public int getRulesTriggered() {
        return rulesTriggered;
    }
    
    /**
     * Get the number of rules that failed (not triggered).
     * 
     * @return Number of failed rules
     */
    public int getRulesFailed() {
        return rulesFailed;
    }
    
    /**
     * Get the success rate as a percentage.
     * 
     * @return Success rate (0.0 to 100.0)
     */
    public double getSuccessRate() {
        if (totalRulesEvaluated == 0) {
            return 0.0;
        }
        return (double) rulesTriggered / totalRulesEvaluated * 100.0;
    }
    
    /**
     * Check if the group evaluation was successful based on the operator.
     * For AND groups: all rules must pass
     * For OR groups: at least one rule must pass
     * 
     * @return true if the evaluation was successful according to group logic
     */
    public boolean isSuccessful() {
        if (isAndOperator) {
            return rulesFailed == 0; // All rules must pass for AND
        } else {
            return rulesTriggered > 0; // At least one rule must pass for OR
        }
    }
    
    @Override
    public String toString() {
        return "RuleGroupEvaluationResult{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", groupResult=" + groupResult +
                ", aggregatedSeverity='" + aggregatedSeverity + '\'' +
                ", operator=" + (isAndOperator ? "AND" : "OR") +
                ", totalRules=" + totalRulesEvaluated +
                ", triggered=" + rulesTriggered +
                ", failed=" + rulesFailed +
                ", successRate=" + String.format("%.1f%%", getSuccessRate()) +
                ", duration=" + evaluationDurationMs + "ms" +
                '}';
    }
}
