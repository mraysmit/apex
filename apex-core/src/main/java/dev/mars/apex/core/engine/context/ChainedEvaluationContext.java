package dev.mars.apex.core.engine.context;

import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/**
 * Enhanced evaluation context for rule chains that maintains stage results and execution path.
 * This context allows rules to access results from previous stages and tracks the execution flow.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class ChainedEvaluationContext extends StandardEvaluationContext {
    
    private final Map<String, Object> stageResults = new HashMap<>();
    private final List<String> executionPath = new ArrayList<>();
    private final Map<String, Object> chainMetadata = new HashMap<>();
    private String currentStage;
    private String currentRule;
    
    /**
     * Create a new chained evaluation context.
     */
    public ChainedEvaluationContext() {
        super();
    }
    
    /**
     * Create a new chained evaluation context with initial variables.
     * 
     * @param variables Initial variables to set in the context
     */
    public ChainedEvaluationContext(Map<String, Object> variables) {
        super();
        if (variables != null) {
            variables.forEach(this::setVariable);
        }
    }
    
    /**
     * Add a result from a stage execution.
     * This result becomes available to subsequent stages as a variable.
     * 
     * @param stageName The name of the stage
     * @param result The result value
     */
    public void addStageResult(String stageName, Object result) {
        stageResults.put(stageName, result);
        // Also make it available as a variable for SpEL expressions
        setVariable(stageName, result);
    }
    
    /**
     * Get a result from a previous stage.
     * 
     * @param stageName The name of the stage
     * @return The result value, or null if not found
     */
    public Object getStageResult(String stageName) {
        return stageResults.get(stageName);
    }
    
    /**
     * Check if a stage result exists.
     * 
     * @param stageName The name of the stage
     * @return true if the result exists, false otherwise
     */
    public boolean hasStageResult(String stageName) {
        return stageResults.containsKey(stageName);
    }
    
    /**
     * Get all stage results.
     * 
     * @return A copy of the stage results map
     */
    public Map<String, Object> getAllStageResults() {
        return new HashMap<>(stageResults);
    }
    
    /**
     * Add a rule or stage name to the execution path.
     * 
     * @param name The name to add to the execution path
     */
    public void addToExecutionPath(String name) {
        executionPath.add(name);
    }
    
    /**
     * Get the current execution path.
     * 
     * @return A copy of the execution path
     */
    public List<String> getExecutionPath() {
        return new ArrayList<>(executionPath);
    }
    
    /**
     * Get the execution path as a formatted string.
     * 
     * @return The execution path joined with " → "
     */
    public String getExecutionPathString() {
        return String.join(" → ", executionPath);
    }
    
    /**
     * Set the current stage being executed.
     * 
     * @param stageName The name of the current stage
     */
    public void setCurrentStage(String stageName) {
        this.currentStage = stageName;
    }
    
    /**
     * Get the current stage being executed.
     * 
     * @return The current stage name
     */
    public String getCurrentStage() {
        return currentStage;
    }
    
    /**
     * Set the current rule being executed.
     * 
     * @param ruleName The name of the current rule
     */
    public void setCurrentRule(String ruleName) {
        this.currentRule = ruleName;
    }
    
    /**
     * Get the current rule being executed.
     * 
     * @return The current rule name
     */
    public String getCurrentRule() {
        return currentRule;
    }
    
    /**
     * Add metadata about the chain execution.
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addChainMetadata(String key, Object value) {
        chainMetadata.put(key, value);
    }
    
    /**
     * Get chain metadata.
     * 
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public Object getChainMetadata(String key) {
        return chainMetadata.get(key);
    }
    
    /**
     * Get all chain metadata.
     * 
     * @return A copy of the chain metadata map
     */
    public Map<String, Object> getAllChainMetadata() {
        return new HashMap<>(chainMetadata);
    }
    
    /**
     * Create a copy of this context for use in a sub-chain or parallel execution.
     * The copy includes all current variables, stage results, and metadata.
     * 
     * @return A new ChainedEvaluationContext with copied state
     */
    public ChainedEvaluationContext createCopy() {
        ChainedEvaluationContext copy = new ChainedEvaluationContext();

        // Copy all variables from stage results
        // Note: StandardEvaluationContext doesn't provide direct access to variables,
        // so we copy from our stage results and any additional variables would need
        // to be tracked separately if needed
        stageResults.forEach(copy::setVariable);
        
        // Copy stage results
        copy.stageResults.putAll(this.stageResults);
        
        // Copy execution path
        copy.executionPath.addAll(this.executionPath);
        
        // Copy metadata
        copy.chainMetadata.putAll(this.chainMetadata);
        
        // Copy current state
        copy.currentStage = this.currentStage;
        copy.currentRule = this.currentRule;
        
        return copy;
    }
    
    /**
     * Clear all stage results and execution path.
     * This can be useful when reusing a context for a new chain execution.
     */
    public void clearChainState() {
        stageResults.clear();
        executionPath.clear();
        chainMetadata.clear();
        currentStage = null;
        currentRule = null;
    }
    
    /**
     * Get a summary of the current context state.
     * 
     * @return A string representation of the context state
     */
    public String getContextSummary() {
        return String.format(
            "ChainedEvaluationContext{stages=%d, executionPath=%s, currentStage='%s', currentRule='%s'}",
            stageResults.size(),
            getExecutionPathString(),
            currentStage,
            currentRule
        );
    }
    
    @Override
    public String toString() {
        return getContextSummary();
    }
}
