package dev.mars.apex.core.engine.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a single step in the execution flow of the rules engine.
 * Used to provide a trace of what happened during evaluation.
 */
public class ExecutionStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String type; // RULE, ENRICHMENT, GROUP, CHAIN, SECTION
    private String status; // SUCCESS, FAILURE, SKIPPED, ERROR
    private String message;
    private long durationMs;
    private Instant timestamp;

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

    @Override
    public String toString() {
        return "ExecutionStep{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", durationMs=" + durationMs +
                '}';
    }
}
