package dev.mars.rulesengine.core.engine.model;

import java.time.Instant;
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
 * Result of executing a rule chain, containing execution path, results, and performance metrics.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class RuleChainResult {
    
    private final String ruleChainId;
    private final String ruleChainName;
    private final String pattern;
    private final List<String> executionPath;
    private final List<RuleResult> ruleResults;
    private final Map<String, Object> stageResults;
    private final String finalOutcome;
    private final boolean successful;
    private final Instant startTime;
    private final Instant endTime;
    private final long executionTimeMillis;
    private final String errorMessage;
    private final Map<String, Object> metadata;
    
    // Private constructor for builder pattern
    private RuleChainResult(Builder builder) {
        this.ruleChainId = builder.ruleChainId;
        this.ruleChainName = builder.ruleChainName;
        this.pattern = builder.pattern;
        this.executionPath = new ArrayList<>(builder.executionPath);
        this.ruleResults = new ArrayList<>(builder.ruleResults);
        this.stageResults = new HashMap<>(builder.stageResults);
        this.finalOutcome = builder.finalOutcome;
        this.successful = builder.successful;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.executionTimeMillis = builder.executionTimeMillis;
        this.errorMessage = builder.errorMessage;
        this.metadata = new HashMap<>(builder.metadata);
    }
    
    // Getters
    public String getRuleChainId() {
        return ruleChainId;
    }
    
    public String getRuleChainName() {
        return ruleChainName;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public List<String> getExecutionPath() {
        return new ArrayList<>(executionPath);
    }
    
    public List<RuleResult> getRuleResults() {
        return new ArrayList<>(ruleResults);
    }
    
    public Map<String, Object> getStageResults() {
        return new HashMap<>(stageResults);
    }
    
    public String getFinalOutcome() {
        return finalOutcome;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Get the number of rules executed in this chain.
     */
    public int getExecutedRulesCount() {
        return ruleResults.size();
    }
    
    /**
     * Get the number of rules that were triggered (matched).
     */
    public int getTriggeredRulesCount() {
        return (int) ruleResults.stream().filter(RuleResult::isTriggered).count();
    }
    
    /**
     * Check if any rules were triggered in this chain.
     */
    public boolean hasTriggeredRules() {
        return ruleResults.stream().anyMatch(RuleResult::isTriggered);
    }
    
    /**
     * Get a specific stage result by name.
     */
    public Object getStageResult(String stageName) {
        return stageResults.get(stageName);
    }
    
    /**
     * Check if a specific stage result exists.
     */
    public boolean hasStageResult(String stageName) {
        return stageResults.containsKey(stageName);
    }
    
    @Override
    public String toString() {
        return "RuleChainResult{" +
                "ruleChainId='" + ruleChainId + '\'' +
                ", pattern='" + pattern + '\'' +
                ", successful=" + successful +
                ", finalOutcome='" + finalOutcome + '\'' +
                ", executionTimeMillis=" + executionTimeMillis +
                ", executedRules=" + getExecutedRulesCount() +
                ", triggeredRules=" + getTriggeredRulesCount() +
                '}';
    }
    
    // Builder class
    public static class Builder {
        private String ruleChainId;
        private String ruleChainName;
        private String pattern;
        private List<String> executionPath = new ArrayList<>();
        private List<RuleResult> ruleResults = new ArrayList<>();
        private Map<String, Object> stageResults = new HashMap<>();
        private String finalOutcome;
        private boolean successful = true;
        private Instant startTime;
        private Instant endTime;
        private long executionTimeMillis;
        private String errorMessage;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder(String ruleChainId, String pattern) {
            this.ruleChainId = ruleChainId;
            this.pattern = pattern;
            this.startTime = Instant.now();
        }
        
        public Builder ruleChainName(String ruleChainName) {
            this.ruleChainName = ruleChainName;
            return this;
        }
        
        public Builder addToExecutionPath(String ruleName) {
            this.executionPath.add(ruleName);
            return this;
        }
        
        public Builder addRuleResult(RuleResult ruleResult) {
            this.ruleResults.add(ruleResult);
            return this;
        }
        
        public Builder addStageResult(String stageName, Object result) {
            this.stageResults.put(stageName, result);
            return this;
        }
        
        public Builder finalOutcome(String finalOutcome) {
            this.finalOutcome = finalOutcome;
            return this;
        }
        
        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            this.successful = false;
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public RuleChainResult build() {
            this.endTime = Instant.now();
            this.executionTimeMillis = endTime.toEpochMilli() - startTime.toEpochMilli();
            return new RuleChainResult(this);
        }
    }
    
    // Static factory methods
    public static Builder builder(String ruleChainId, String pattern) {
        return new Builder(ruleChainId, pattern);
    }
    
    public static RuleChainResult success(String ruleChainId, String pattern, String finalOutcome) {
        return new Builder(ruleChainId, pattern)
                .finalOutcome(finalOutcome)
                .successful(true)
                .build();
    }
    
    public static RuleChainResult failure(String ruleChainId, String pattern, String errorMessage) {
        return new Builder(ruleChainId, pattern)
                .errorMessage(errorMessage)
                .finalOutcome("FAILED")
                .build();
    }
}
