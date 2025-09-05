package dev.mars.apex.core.config.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Configuration for APEX pipeline orchestration.
 * Defines the complete workflow including steps, execution settings, and transformations.
 * 
 * @author APEX Team
 * @since 1.0.0
 */
public class PipelineConfiguration {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("steps")
    private List<PipelineStep> steps;
    
    @JsonProperty("execution")
    private ExecutionConfiguration execution;
    
    @JsonProperty("transformations")
    private List<TransformationConfiguration> transformations;
    
    @JsonProperty("monitoring")
    private MonitoringConfiguration monitoring;
    
    // Constructors
    public PipelineConfiguration() {}
    
    public PipelineConfiguration(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<PipelineStep> getSteps() {
        return steps;
    }
    
    public void setSteps(List<PipelineStep> steps) {
        this.steps = steps;
    }
    
    public ExecutionConfiguration getExecution() {
        return execution;
    }
    
    public void setExecution(ExecutionConfiguration execution) {
        this.execution = execution;
    }
    
    public List<TransformationConfiguration> getTransformations() {
        return transformations;
    }
    
    public void setTransformations(List<TransformationConfiguration> transformations) {
        this.transformations = transformations;
    }
    
    public MonitoringConfiguration getMonitoring() {
        return monitoring;
    }
    
    public void setMonitoring(MonitoringConfiguration monitoring) {
        this.monitoring = monitoring;
    }
    
    /**
     * Execution configuration for pipeline.
     */
    public static class ExecutionConfiguration {
        @JsonProperty("mode")
        private String mode = "sequential"; // sequential or parallel
        
        @JsonProperty("error-handling")
        private String errorHandling = "stop-on-error"; // stop-on-error or continue-on-error
        
        @JsonProperty("max-retries")
        private int maxRetries = 3;
        
        @JsonProperty("retry-delay-ms")
        private long retryDelayMs = 1000;
        
        // Getters and Setters
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        
        public String getErrorHandling() { return errorHandling; }
        public void setErrorHandling(String errorHandling) { this.errorHandling = errorHandling; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    }
    
    /**
     * Transformation configuration for pipeline-level data transformations.
     */
    public static class TransformationConfiguration {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("value")
        private String value;
        
        @JsonProperty("rule")
        private String rule;
        
        @JsonProperty("parameters")
        private Map<String, Object> parameters;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public String getRule() { return rule; }
        public void setRule(String rule) { this.rule = rule; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
    /**
     * Monitoring configuration for pipeline observability.
     */
    public static class MonitoringConfiguration {
        @JsonProperty("enabled")
        private boolean enabled = true;
        
        @JsonProperty("log-progress")
        private boolean logProgress = true;
        
        @JsonProperty("collect-metrics")
        private boolean collectMetrics = true;
        
        @JsonProperty("alert-on-failure")
        private boolean alertOnFailure = true;
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean isLogProgress() { return logProgress; }
        public void setLogProgress(boolean logProgress) { this.logProgress = logProgress; }
        
        public boolean isCollectMetrics() { return collectMetrics; }
        public void setCollectMetrics(boolean collectMetrics) { this.collectMetrics = collectMetrics; }
        
        public boolean isAlertOnFailure() { return alertOnFailure; }
        public void setAlertOnFailure(boolean alertOnFailure) { this.alertOnFailure = alertOnFailure; }
    }
}
