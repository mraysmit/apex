package dev.mars.apex.core.config.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a single step in an APEX pipeline.
 * Each step represents a unit of work (extract, transform, load, etc.).
 * 
 * @author APEX Team
 * @since 1.0.0
 */
public class PipelineStep {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private String type; // extract, load, transform, audit, etc.
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("source")
    private String source; // data source name (for extract steps)
    
    @JsonProperty("sink")
    private String sink; // data sink name (for load steps)
    
    @JsonProperty("operation")
    private String operation; // operation name to execute
    
    @JsonProperty("depends-on")
    private List<String> dependsOn; // step dependencies
    
    @JsonProperty("optional")
    private boolean optional = false; // if true, failure doesn't stop pipeline
    
    @JsonProperty("parameters")
    private Map<String, Object> parameters; // step-specific parameters
    
    @JsonProperty("condition")
    private String condition; // conditional execution expression
    
    @JsonProperty("retry")
    private RetryConfiguration retry;
    
    // Constructors
    public PipelineStep() {}
    
    public PipelineStep(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    // Getters and Setters
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSink() {
        return sink;
    }
    
    public void setSink(String sink) {
        this.sink = sink;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public List<String> getDependsOn() {
        return dependsOn;
    }
    
    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }
    
    public boolean isOptional() {
        return optional;
    }
    
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public RetryConfiguration getRetry() {
        return retry;
    }
    
    public void setRetry(RetryConfiguration retry) {
        this.retry = retry;
    }
    
    /**
     * Step-level retry configuration.
     */
    public static class RetryConfiguration {
        @JsonProperty("max-attempts")
        private int maxAttempts = 3;
        
        @JsonProperty("delay-ms")
        private long delayMs = 1000;
        
        @JsonProperty("backoff-multiplier")
        private double backoffMultiplier = 2.0;
        
        @JsonProperty("max-delay-ms")
        private long maxDelayMs = 30000;
        
        // Getters and Setters
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        
        public long getDelayMs() { return delayMs; }
        public void setDelayMs(long delayMs) { this.delayMs = delayMs; }
        
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        
        public long getMaxDelayMs() { return maxDelayMs; }
        public void setMaxDelayMs(long maxDelayMs) { this.maxDelayMs = maxDelayMs; }
    }
    
    /**
     * Check if this step has dependencies.
     */
    public boolean hasDependencies() {
        return dependsOn != null && !dependsOn.isEmpty();
    }
    
    /**
     * Check if this step is an extract step.
     */
    public boolean isExtractStep() {
        return "extract".equalsIgnoreCase(type);
    }
    
    /**
     * Check if this step is a load step.
     */
    public boolean isLoadStep() {
        return "load".equalsIgnoreCase(type);
    }
    
    /**
     * Check if this step is a transform step.
     */
    public boolean isTransformStep() {
        return "transform".equalsIgnoreCase(type);
    }
    
    /**
     * Check if this step is an audit step.
     */
    public boolean isAuditStep() {
        return "audit".equalsIgnoreCase(type);
    }
}
