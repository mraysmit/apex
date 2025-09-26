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

import dev.mars.apex.core.engine.model.RuleResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of executing a single stage within a scenario.
 * 
 * Contains execution status, timing information, rule results, and any outputs
 * produced by the stage. Used for tracking individual stage performance and
 * providing detailed feedback on stage execution.
 * 
 * RESULT TYPES:
 * - SUCCESS: Stage completed successfully
 * - FAILURE: Stage failed during execution
 * - CONFIGURATION_ERROR: Stage configuration was invalid
 * - CRITICAL_FAILURE: Required stage failed, scenario cannot continue
 * - NON_CRITICAL_FAILURE: Optional stage failed, scenario can continue
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class StageExecutionResult {
    
    public enum ResultType {
        SUCCESS,
        FAILURE,
        CONFIGURATION_ERROR,
        CRITICAL_FAILURE,
        NON_CRITICAL_FAILURE
    }
    
    private final String stageName;
    private final ResultType resultType;
    private final boolean successful;
    private String errorMessage;
    private RuleResult ruleResult;
    private long executionTimeMs;
    private Map<String, Object> stageOutputs;
    private long startTime;
    private long endTime;
    
    // Private constructor - use factory methods
    private StageExecutionResult(String stageName, ResultType resultType, boolean successful) {
        this.stageName = stageName;
        this.resultType = resultType;
        this.successful = successful;
        this.stageOutputs = new HashMap<>();
        this.startTime = System.currentTimeMillis();
    }
    
    // Factory methods for creating different result types
    
    /**
     * Creates a successful stage execution result.
     * 
     * @param stageName the name of the stage
     * @param ruleResult the rule execution result
     * @return successful stage result
     */
    public static StageExecutionResult success(String stageName, RuleResult ruleResult) {
        StageExecutionResult result = new StageExecutionResult(stageName, ResultType.SUCCESS, true);
        result.ruleResult = ruleResult;
        return result;
    }
    
    /**
     * Creates a failed stage execution result.
     * 
     * @param stageName the name of the stage
     * @param errorMessage the error message
     * @return failed stage result
     */
    public static StageExecutionResult failure(String stageName, String errorMessage) {
        StageExecutionResult result = new StageExecutionResult(stageName, ResultType.FAILURE, false);
        result.errorMessage = errorMessage;
        return result;
    }
    
    /**
     * Creates a configuration error result.
     * 
     * @param stageName the name of the stage
     * @param errorMessage the configuration error message
     * @return configuration error result
     */
    public static StageExecutionResult configurationError(String stageName, String errorMessage) {
        StageExecutionResult result = new StageExecutionResult(stageName, ResultType.CONFIGURATION_ERROR, false);
        result.errorMessage = errorMessage;
        return result;
    }
    
    /**
     * Creates a critical failure result (required stage failed).
     * 
     * @param stageName the name of the stage
     * @param errorMessage the error message
     * @return critical failure result
     */
    public static StageExecutionResult criticalFailure(String stageName, String errorMessage) {
        StageExecutionResult result = new StageExecutionResult(stageName, ResultType.CRITICAL_FAILURE, false);
        result.errorMessage = errorMessage;
        return result;
    }
    
    /**
     * Creates a non-critical failure result (optional stage failed).
     * 
     * @param stageName the name of the stage
     * @param errorMessage the error message
     * @return non-critical failure result
     */
    public static StageExecutionResult nonCriticalFailure(String stageName, String errorMessage) {
        StageExecutionResult result = new StageExecutionResult(stageName, ResultType.NON_CRITICAL_FAILURE, false);
        result.errorMessage = errorMessage;
        return result;
    }
    
    // Getters
    
    public String getStageName() {
        return stageName;
    }
    
    public ResultType getResultType() {
        return resultType;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public RuleResult getRuleResult() {
        return ruleResult;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public Map<String, Object> getStageOutputs() {
        return new HashMap<>(stageOutputs);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    // Setters and utility methods
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        this.endTime = startTime + executionTimeMs;
    }
    
    public void addStageOutput(String key, Object value) {
        stageOutputs.put(key, value);
    }
    
    public void setStageOutputs(Map<String, Object> outputs) {
        this.stageOutputs = outputs != null ? new HashMap<>(outputs) : new HashMap<>();
    }
    
    /**
     * Gets a specific output value from the stage.
     * 
     * @param key the output key
     * @return the output value or null if not found
     */
    public Object getStageOutput(String key) {
        return stageOutputs.get(key);
    }
    
    /**
     * Checks if this is a critical failure that should terminate scenario processing.
     * 
     * @return true if this is a critical failure
     */
    public boolean isCriticalFailure() {
        return resultType == ResultType.CRITICAL_FAILURE;
    }
    
    /**
     * Checks if this is a configuration error.
     * 
     * @return true if this is a configuration error
     */
    public boolean isConfigurationError() {
        return resultType == ResultType.CONFIGURATION_ERROR;
    }
    
    /**
     * Gets a summary of the stage execution for logging.
     * 
     * @return execution summary string
     */
    public String getExecutionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Stage '").append(stageName).append("': ");
        summary.append(resultType.name());
        
        if (executionTimeMs > 0) {
            summary.append(" (").append(executionTimeMs).append("ms)");
        }
        
        if (!successful && errorMessage != null) {
            summary.append(" - ").append(errorMessage);
        }
        
        if (ruleResult != null) {
            summary.append(" - Rules: ").append(ruleResult.isTriggered() ? "PASSED" : "FAILED");
        }
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return "StageExecutionResult{" +
                "stageName='" + stageName + '\'' +
                ", resultType=" + resultType +
                ", successful=" + successful +
                ", executionTimeMs=" + executionTimeMs +
                ", errorMessage='" + errorMessage + '\'' +
                ", hasRuleResult=" + (ruleResult != null) +
                ", outputCount=" + stageOutputs.size() +
                '}';
    }
}
