package dev.mars.rulesengine.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;

/**
 * DTO for performance metrics information.
 * 
 * This class represents performance metrics for rule evaluations,
 * including timing, memory usage, and complexity information.
 */
@Schema(description = "Performance metrics for rule evaluation")
public class PerformanceMetricsDto {
    
    @Schema(description = "Evaluation time in milliseconds", example = "15")
    @JsonProperty("evaluationTimeMs")
    private long evaluationTimeMs;
    
    @Schema(description = "Memory used during evaluation in bytes", example = "2048")
    @JsonProperty("memoryUsedBytes")
    private long memoryUsedBytes;
    
    @Schema(description = "Rule complexity score", example = "3")
    @JsonProperty("complexityScore")
    private int complexityScore;
    
    @Schema(description = "Start time of the evaluation")
    @JsonProperty("startTime")
    private Instant startTime;
    
    @Schema(description = "End time of the evaluation")
    @JsonProperty("endTime")
    private Instant endTime;
    
    @Schema(description = "Whether the evaluation completed successfully", example = "true")
    @JsonProperty("successful")
    private boolean successful;
    
    @Schema(description = "Exception message if evaluation failed")
    @JsonProperty("exceptionMessage")
    private String exceptionMessage;
    
    @Schema(description = "Cache hit indicator", example = "false")
    @JsonProperty("cacheHit")
    private boolean cacheHit;
    
    // Default constructor
    public PerformanceMetricsDto() {}
    
    // Constructor with basic metrics
    public PerformanceMetricsDto(long evaluationTimeMs, long memoryUsedBytes, 
                                int complexityScore, boolean successful) {
        this.evaluationTimeMs = evaluationTimeMs;
        this.memoryUsedBytes = memoryUsedBytes;
        this.complexityScore = complexityScore;
        this.successful = successful;
        this.endTime = Instant.now();
        this.startTime = endTime.minus(Duration.ofMillis(evaluationTimeMs));
    }
    
    // Getters and setters
    public long getEvaluationTimeMs() {
        return evaluationTimeMs;
    }
    
    public void setEvaluationTimeMs(long evaluationTimeMs) {
        this.evaluationTimeMs = evaluationTimeMs;
    }
    
    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }
    
    public void setMemoryUsedBytes(long memoryUsedBytes) {
        this.memoryUsedBytes = memoryUsedBytes;
    }
    
    public int getComplexityScore() {
        return complexityScore;
    }
    
    public void setComplexityScore(int complexityScore) {
        this.complexityScore = complexityScore;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getExceptionMessage() {
        return exceptionMessage;
    }
    
    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }
    
    public boolean isCacheHit() {
        return cacheHit;
    }
    
    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
    
    @Override
    public String toString() {
        return "PerformanceMetricsDto{" +
                "evaluationTimeMs=" + evaluationTimeMs +
                ", memoryUsedBytes=" + memoryUsedBytes +
                ", complexityScore=" + complexityScore +
                ", successful=" + successful +
                ", cacheHit=" + cacheHit +
                '}';
    }
}
