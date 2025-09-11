package dev.mars.apex.core.engine.pipeline;

/**
 * Result of a single pipeline step execution.
 * 
 * @author APEX Team
 * @since 1.0.0
 */
public class PipelineStepResult {
    
    private final String stepName;
    private boolean success;
    private boolean skipped;
    private String error;
    private long durationMs;
    private Object data;
    private int recordsProcessed;
    private int recordsFailed;
    
    public PipelineStepResult(String stepName) {
        this.stepName = stepName;
        this.success = false;
        this.skipped = false;
    }
    
    // Getters and Setters
    public String getStepName() {
        return stepName;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isSkipped() {
        return skipped;
    }
    
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
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
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public int getRecordsProcessed() {
        return recordsProcessed;
    }
    
    public void setRecordsProcessed(int recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }
    
    public int getRecordsFailed() {
        return recordsFailed;
    }
    
    public void setRecordsFailed(int recordsFailed) {
        this.recordsFailed = recordsFailed;
    }
    
    /**
     * Get success rate for this step.
     */
    public double getSuccessRate() {
        int total = recordsProcessed + recordsFailed;
        if (total == 0) return success ? 100.0 : 0.0;
        return (double) recordsProcessed / total * 100.0;
    }
    
    @Override
    public String toString() {
        return String.format("PipelineStepResult{step='%s', success=%s, duration=%dms, " +
                "processed=%d, failed=%d, successRate=%.1f%%}",
                stepName, success, durationMs, recordsProcessed, recordsFailed, getSuccessRate());
    }
}
