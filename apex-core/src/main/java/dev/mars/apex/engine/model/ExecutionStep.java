package dev.mars.apex.engine.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a single step in the execution flow of the rules engine.
 * Used to provide a trace of what happened during evaluation.
 *
 * <p>For pipeline steps, this class can optionally store the actual data
 * produced by the step and metrics about records processed/failed.</p>
 */
public class ExecutionStep implements Serializable {
    private static final long serialVersionUID = 2L;  // Incremented for new fields

    private String name;
    private String type; // RULE, ENRICHMENT, GROUP, CHAIN, SECTION, PIPELINE_STEP
    private String status; // SUCCESS, FAILURE, SKIPPED, ERROR
    private String message;
    private long durationMs;
    private Instant timestamp;

    // Pipeline-specific fields (optional, null for non-pipeline steps)
    private Object stepData;              // The actual data from the step
    private Integer recordsProcessed;     // Number of records processed
    private Integer recordsFailed;        // Number of records failed

    public ExecutionStep() {
        this.timestamp = Instant.now();
    }

    public ExecutionStep(String name, String type, String status, String message, long durationMs) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.message = message;
        this.durationMs = durationMs;
        this.timestamp = Instant.now();
    }

    /**
     * Constructor for pipeline steps with data and metrics.
     *
     * @param name The name of the step
     * @param type The type of step (typically "PIPELINE_STEP")
     * @param status The status (SUCCESS, FAILURE, SKIPPED, ERROR)
     * @param message The message associated with the step
     * @param durationMs The duration in milliseconds
     * @param stepData The actual data produced by the step (can be null)
     * @param recordsProcessed Number of records successfully processed
     * @param recordsFailed Number of records that failed processing
     */
    public ExecutionStep(String name, String type, String status, String message,
                        long durationMs, Object stepData, int recordsProcessed, int recordsFailed) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.message = message;
        this.durationMs = durationMs;
        this.timestamp = Instant.now();
        this.stepData = stepData;
        this.recordsProcessed = recordsProcessed;
        this.recordsFailed = recordsFailed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the data produced by this step (for pipeline steps).
     *
     * @return The step data, or null if not a pipeline step or no data available
     */
    public Object getStepData() {
        return stepData;
    }

    /**
     * Set the data produced by this step.
     *
     * @param stepData The step data
     */
    public void setStepData(Object stepData) {
        this.stepData = stepData;
    }

    /**
     * Get the number of records successfully processed by this step.
     *
     * @return The number of records processed, or null if not applicable
     */
    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    /**
     * Set the number of records successfully processed.
     *
     * @param recordsProcessed The number of records processed
     */
    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    /**
     * Get the number of records that failed processing in this step.
     *
     * @return The number of records failed, or null if not applicable
     */
    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    /**
     * Set the number of records that failed processing.
     *
     * @param recordsFailed The number of records failed
     */
    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    /**
     * Check if this step has data available.
     *
     * @return true if step data is available, false otherwise
     */
    public boolean hasStepData() {
        return stepData != null;
    }

    /**
     * Calculate the success rate for this step based on records processed/failed.
     *
     * @return The success rate as a percentage (0-100), or 100.0/0.0 based on status if metrics not available
     */
    public double getSuccessRate() {
        if (recordsProcessed == null || recordsFailed == null) {
            return "SUCCESS".equals(status) ? 100.0 : 0.0;
        }
        int total = recordsProcessed + recordsFailed;
        if (total == 0) {
            return "SUCCESS".equals(status) ? 100.0 : 0.0;
        }
        return (double) recordsProcessed / total * 100.0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ExecutionStep{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", durationMs=").append(durationMs);

        if (recordsProcessed != null) {
            sb.append(", recordsProcessed=").append(recordsProcessed);
        }
        if (recordsFailed != null) {
            sb.append(", recordsFailed=").append(recordsFailed);
        }
        if (recordsProcessed != null && recordsFailed != null) {
            sb.append(", successRate=").append(String.format("%.1f%%", getSuccessRate()));
        }

        sb.append('}');
        return sb.toString();
    }
}
