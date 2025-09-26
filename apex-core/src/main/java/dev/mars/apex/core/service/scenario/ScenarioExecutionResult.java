package dev.mars.apex.core.service.scenario;

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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Result of executing a complete scenario with multiple stages.
 * 
 * Aggregates results from all stages, tracks overall execution status,
 * and provides detailed information about scenario processing including
 * warnings, review flags, and performance metrics.
 * 
 * EXECUTION STATES:
 * - SUCCESSFUL: All stages completed successfully
 * - FAILED: One or more stages failed
 * - TERMINATED: Processing terminated due to critical failure
 * - REQUIRES_REVIEW: Flagged for manual review due to stage failures
 * - PARTIAL_SUCCESS: Some stages succeeded, others failed with warnings
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ScenarioExecutionResult {
    
    private final String scenarioId;
    private final long executionStartTime;
    private boolean successful;
    private boolean terminated;
    private boolean requiresReview;
    private List<StageExecutionResult> stageResults;
    private List<String> warnings;
    private List<String> reviewFlags;
    private Map<String, String> skippedStages; // stageName -> reason
    private long totalExecutionTimeMs;
    private Map<String, Object> scenarioOutputs;
    
    public ScenarioExecutionResult(String scenarioId) {
        this.scenarioId = scenarioId;
        this.executionStartTime = System.currentTimeMillis();
        this.successful = true; // Assume success until proven otherwise
        this.terminated = false;
        this.requiresReview = false;
        this.stageResults = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.reviewFlags = new ArrayList<>();
        this.skippedStages = new HashMap<>();
        this.scenarioOutputs = new HashMap<>();
    }
    
    // Getters
    
    public String getScenarioId() {
        return scenarioId;
    }
    
    public boolean isSuccessful() {
        return successful && !terminated && stageResults.stream().allMatch(StageExecutionResult::isSuccessful);
    }
    
    public boolean isTerminated() {
        return terminated;
    }
    
    public boolean requiresReview() {
        return requiresReview;
    }
    
    public List<StageExecutionResult> getStageResults() {
        return new ArrayList<>(stageResults);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public List<String> getReviewFlags() {
        return new ArrayList<>(reviewFlags);
    }
    
    public Map<String, String> getSkippedStages() {
        return new HashMap<>(skippedStages);
    }
    
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public long getExecutionStartTime() {
        return executionStartTime;
    }
    
    public Map<String, Object> getScenarioOutputs() {
        return new HashMap<>(scenarioOutputs);
    }
    
    // Stage management methods
    
    /**
     * Adds a stage execution result to the scenario result.
     * 
     * @param stageResult the stage execution result
     */
    public void addStageResult(StageExecutionResult stageResult) {
        stageResults.add(stageResult);
        
        // Update overall success status
        if (!stageResult.isSuccessful()) {
            successful = false;
        }
        
        // Update total execution time
        totalExecutionTimeMs += stageResult.getExecutionTimeMs();
    }
    
    /**
     * Adds a skipped stage with reason.
     * 
     * @param stageName the name of the skipped stage
     * @param reason the reason for skipping
     */
    public void addSkippedStage(String stageName, String reason) {
        skippedStages.put(stageName, reason);
    }
    
    /**
     * Checks if a specific stage was successful.
     * 
     * @param stageName the name of the stage to check
     * @return true if the stage was successful
     */
    public boolean isStageSuccessful(String stageName) {
        return stageResults.stream()
            .filter(result -> stageName.equals(result.getStageName()))
            .findFirst()
            .map(StageExecutionResult::isSuccessful)
            .orElse(false);
    }
    
    /**
     * Gets the result for a specific stage.
     * 
     * @param stageName the name of the stage
     * @return the stage result or null if not found
     */
    public StageExecutionResult getStageResult(String stageName) {
        return stageResults.stream()
            .filter(result -> stageName.equals(result.getStageName()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets all successful stage results.
     * 
     * @return list of successful stage results
     */
    public List<StageExecutionResult> getSuccessfulStages() {
        return stageResults.stream()
            .filter(StageExecutionResult::isSuccessful)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all failed stage results.
     * 
     * @return list of failed stage results
     */
    public List<StageExecutionResult> getFailedStages() {
        return stageResults.stream()
            .filter(result -> !result.isSuccessful())
            .collect(Collectors.toList());
    }
    
    // Status management methods
    
    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
        if (terminated) {
            this.successful = false;
        }
    }
    
    public void setRequiresReview(boolean requiresReview) {
        this.requiresReview = requiresReview;
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public void addReviewFlag(String reviewFlag) {
        reviewFlags.add(reviewFlag);
        setRequiresReview(true);
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public boolean hasReviewFlags() {
        return !reviewFlags.isEmpty();
    }
    
    // Output management methods
    
    public void addScenarioOutput(String key, Object value) {
        scenarioOutputs.put(key, value);
    }
    
    public void setScenarioOutputs(Map<String, Object> outputs) {
        this.scenarioOutputs = outputs != null ? new HashMap<>(outputs) : new HashMap<>();
    }
    
    public Object getScenarioOutput(String key) {
        return scenarioOutputs.get(key);
    }
    
    // Utility methods
    
    /**
     * Finalizes the execution result by calculating total time.
     */
    public void finalizeExecution() {
        if (totalExecutionTimeMs == 0) {
            totalExecutionTimeMs = System.currentTimeMillis() - executionStartTime;
        }
    }
    
    /**
     * Gets the overall execution status as a string.
     * 
     * @return execution status description
     */
    public String getExecutionStatus() {
        if (terminated) {
            return "TERMINATED";
        } else if (isSuccessful()) {
            return "SUCCESSFUL";
        } else if (requiresReview) {
            return "REQUIRES_REVIEW";
        } else if (hasWarnings()) {
            return "PARTIAL_SUCCESS";
        } else {
            return "FAILED";
        }
    }
    
    /**
     * Gets a summary of the scenario execution for logging.
     * 
     * @return execution summary string
     */
    public String getExecutionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Scenario '").append(scenarioId).append("': ");
        summary.append(getExecutionStatus());
        summary.append(" (").append(totalExecutionTimeMs).append("ms)");
        
        summary.append(" - Stages: ").append(stageResults.size());
        summary.append(" successful, ").append(getFailedStages().size()).append(" failed");
        
        if (!skippedStages.isEmpty()) {
            summary.append(", ").append(skippedStages.size()).append(" skipped");
        }
        
        if (hasWarnings()) {
            summary.append(" - Warnings: ").append(warnings.size());
        }
        
        if (requiresReview) {
            summary.append(" - REQUIRES REVIEW");
        }
        
        return summary.toString();
    }
    
    /**
     * Gets detailed execution report including all stage results.
     * 
     * @return detailed execution report
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Scenario Execution Report ===\n");
        report.append("Scenario ID: ").append(scenarioId).append("\n");
        report.append("Status: ").append(getExecutionStatus()).append("\n");
        report.append("Total Time: ").append(totalExecutionTimeMs).append("ms\n");
        report.append("Stages Executed: ").append(stageResults.size()).append("\n");
        
        if (!stageResults.isEmpty()) {
            report.append("\n--- Stage Results ---\n");
            for (StageExecutionResult stageResult : stageResults) {
                report.append(stageResult.getExecutionSummary()).append("\n");
            }
        }
        
        if (!skippedStages.isEmpty()) {
            report.append("\n--- Skipped Stages ---\n");
            skippedStages.forEach((stage, reason) -> 
                report.append("- ").append(stage).append(": ").append(reason).append("\n"));
        }
        
        if (hasWarnings()) {
            report.append("\n--- Warnings ---\n");
            warnings.forEach(warning -> report.append("- ").append(warning).append("\n"));
        }
        
        if (hasReviewFlags()) {
            report.append("\n--- Review Flags ---\n");
            reviewFlags.forEach(flag -> report.append("- ").append(flag).append("\n"));
        }
        
        return report.toString();
    }
    
    @Override
    public String toString() {
        return "ScenarioExecutionResult{" +
                "scenarioId='" + scenarioId + '\'' +
                ", status='" + getExecutionStatus() + '\'' +
                ", totalTimeMs=" + totalExecutionTimeMs +
                ", stagesExecuted=" + stageResults.size() +
                ", stagesSkipped=" + skippedStages.size() +
                ", warnings=" + warnings.size() +
                ", requiresReview=" + requiresReview +
                '}';
    }
}
