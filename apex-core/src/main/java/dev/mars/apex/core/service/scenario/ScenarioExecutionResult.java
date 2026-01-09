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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicBoolean successful;
    private final AtomicBoolean terminated;
    private final AtomicBoolean requiresReview;
    private final List<StageExecutionResult> stageResults;
    private final List<String> warnings;
    private final List<String> reviewFlags;
    private final Map<String, String> skippedStages; // stageName -> reason (ConcurrentHashMap for thread-safety)
    private final AtomicLong totalExecutionTimeMs;
    private final Map<String, Object> scenarioOutputs;
    private final List<dev.mars.apex.core.engine.model.ExecutionStep> executionPath; // Trace execution
    
    public ScenarioExecutionResult(String scenarioId) {
        this.scenarioId = scenarioId;
        this.executionStartTime = System.currentTimeMillis();
        this.successful = new AtomicBoolean(true); // Assume success until proven otherwise
        this.terminated = new AtomicBoolean(false);
        this.requiresReview = new AtomicBoolean(false);
        this.stageResults = new CopyOnWriteArrayList<>();
        this.warnings = new CopyOnWriteArrayList<>();
        this.reviewFlags = new CopyOnWriteArrayList<>();
        this.skippedStages = new java.util.concurrent.ConcurrentHashMap<>();
        this.totalExecutionTimeMs = new AtomicLong(0);
        this.scenarioOutputs = new java.util.concurrent.ConcurrentHashMap<>();
        this.executionPath = new CopyOnWriteArrayList<>();
    }
    
    // Getters
    
    public String getScenarioId() {
        return scenarioId;
    }
    
    public boolean isSuccessful() {
        return successful.get() && !terminated.get() && stageResults.stream().allMatch(StageExecutionResult::isSuccessful);
    }

    public boolean isTerminated() {
        return terminated.get();
    }

    public boolean requiresReview() {
        return requiresReview.get();
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
        return totalExecutionTimeMs.get();
    }
    
    public long getExecutionStartTime() {
        return executionStartTime;
    }
    
    public Map<String, Object> getScenarioOutputs() {
        return new HashMap<>(scenarioOutputs);
    }

    public List<dev.mars.apex.core.engine.model.ExecutionStep> getExecutionPath() {
        return new ArrayList<>(executionPath);
    }

    public void addExecutionStep(dev.mars.apex.core.engine.model.ExecutionStep step) {
        this.executionPath.add(step);
    }

    public void addExecutionSteps(List<dev.mars.apex.core.engine.model.ExecutionStep> steps) {
        if (steps != null) {
            this.executionPath.addAll(steps);
        }
    }
    
    // Stage management methods
    
    /**
     * Adds a stage execution result to the scenario result.
     * Thread-safe: Uses CopyOnWriteArrayList and atomic operations.
     *
     * @param stageResult the stage execution result
     */
    public void addStageResult(StageExecutionResult stageResult) {
        stageResults.add(stageResult);

        // Update overall success status (thread-safe)
        if (!stageResult.isSuccessful()) {
            successful.set(false);
        }

        // Update total execution time (thread-safe atomic operation)
        totalExecutionTimeMs.addAndGet(stageResult.getExecutionTimeMs());
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
        this.terminated.set(terminated);
        if (terminated) {
            this.successful.set(false);
        }
    }

    public void setRequiresReview(boolean requiresReview) {
        this.requiresReview.set(requiresReview);
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
        this.scenarioOutputs.clear();
        if (outputs != null) {
            this.scenarioOutputs.putAll(outputs);
        }
    }
    
    public Object getScenarioOutput(String key) {
        return scenarioOutputs.get(key);
    }
    
    // Utility methods
    
    /**
     * Finalizes the execution result by calculating total time.
     * Thread-safe: Uses atomic compareAndSet operation.
     */
    public void finalizeExecution() {
        if (totalExecutionTimeMs.get() == 0) {
            totalExecutionTimeMs.set(System.currentTimeMillis() - executionStartTime);
        }
    }
    
    /**
     * Gets the overall execution status as a string.
     * 
     * @return execution status description
     */
    public String getExecutionStatus() {
        if (terminated.get()) {
            return "TERMINATED";
        } else if (isSuccessful()) {
            return "SUCCESSFUL";
        } else if (requiresReview.get()) {
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
        
        summary.append(" - Stages: ").append(getSuccessfulStages().size());
        summary.append(" successful, ").append(getFailedStages().size()).append(" failed");
        
        if (!skippedStages.isEmpty()) {
            summary.append(", ").append(skippedStages.size()).append(" skipped");
        }
        
        if (hasWarnings()) {
            summary.append(" - Warnings: ").append(warnings.size());
        }

        if (requiresReview.get()) {
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
