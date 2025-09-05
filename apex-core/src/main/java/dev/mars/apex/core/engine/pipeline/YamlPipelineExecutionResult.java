package dev.mars.apex.core.engine.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of YAML-defined pipeline execution containing overall status and individual step results.
 * 
 * @author APEX Team
 * @since 1.0.0
 */
public class YamlPipelineExecutionResult {
    
    private final String pipelineName;
    private boolean success;
    private String error;
    private long durationMs;
    private final List<PipelineStepResult> stepResults;
    private int totalSteps;
    private int successfulSteps;
    private int failedSteps;
    private int skippedSteps;
    
    public YamlPipelineExecutionResult(String pipelineName) {
        this.pipelineName = pipelineName;
        this.stepResults = new ArrayList<>();
        this.success = false;
    }
    
    /**
     * Add a step result and update counters.
     */
    public void addStepResult(PipelineStepResult stepResult) {
        stepResults.add(stepResult);
        totalSteps++;
        
        if (stepResult.isSuccess()) {
            successfulSteps++;
        } else if (stepResult.isSkipped()) {
            skippedSteps++;
        } else {
            failedSteps++;
        }
    }
    
    // Getters and Setters
    public String getPipelineName() {
        return pipelineName;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public List<PipelineStepResult> getStepResults() {
        return stepResults;
    }
    
    public int getTotalSteps() {
        return totalSteps;
    }
    
    public int getSuccessfulSteps() {
        return successfulSteps;
    }
    
    public int getFailedSteps() {
        return failedSteps;
    }
    
    public int getSkippedSteps() {
        return skippedSteps;
    }
    
    /**
     * Get success rate as percentage.
     */
    public double getSuccessRate() {
        if (totalSteps == 0) return 0.0;
        return (double) successfulSteps / totalSteps * 100.0;
    }
    
    /**
     * Check if pipeline completed (regardless of success/failure).
     */
    public boolean isCompleted() {
        return totalSteps > 0 && (successfulSteps + failedSteps + skippedSteps) == totalSteps;
    }
    
    @Override
    public String toString() {
        return String.format("YamlPipelineExecutionResult{pipeline='%s', success=%s, duration=%dms, " +
                "steps=%d, successful=%d, failed=%d, skipped=%d, successRate=%.1f%%}",
                pipelineName, success, durationMs, totalSteps, successfulSteps, 
                failedSteps, skippedSteps, getSuccessRate());
    }
}
