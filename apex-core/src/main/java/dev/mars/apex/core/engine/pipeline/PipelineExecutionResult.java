package dev.mars.apex.core.engine.pipeline;

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
 * Result of pipeline execution.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class PipelineExecutionResult {
    
    private final String pipelineId;
    private final boolean successful;
    private final long startTime;
    private final long executionTimeMs;
    private final int recordsProcessed;
    private final int recordsFailed;
    private final int batchesProcessed;
    private final String errorMessage;
    
    private PipelineExecutionResult(Builder builder) {
        this.pipelineId = builder.pipelineId;
        this.successful = builder.successful;
        this.startTime = builder.startTime;
        this.executionTimeMs = builder.executionTimeMs;
        this.recordsProcessed = builder.recordsProcessed;
        this.recordsFailed = builder.recordsFailed;
        this.batchesProcessed = builder.batchesProcessed;
        this.errorMessage = builder.errorMessage;
    }
    
    // Getters
    public String getPipelineId() { return pipelineId; }
    public boolean isSuccessful() { return successful; }
    public long getStartTime() { return startTime; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public int getRecordsProcessed() { return recordsProcessed; }
    public int getRecordsFailed() { return recordsFailed; }
    public int getBatchesProcessed() { return batchesProcessed; }
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for PipelineExecutionResult.
     */
    public static class Builder {
        private String pipelineId;
        private boolean successful;
        private long startTime;
        private long executionTimeMs;
        private int recordsProcessed;
        private int recordsFailed;
        private int batchesProcessed;
        private String errorMessage;
        
        public Builder pipelineId(String pipelineId) {
            this.pipelineId = pipelineId;
            return this;
        }
        
        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }
        
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder recordsProcessed(int recordsProcessed) {
            this.recordsProcessed = recordsProcessed;
            return this;
        }
        
        public Builder recordsFailed(int recordsFailed) {
            this.recordsFailed = recordsFailed;
            return this;
        }
        
        public Builder batchesProcessed(int batchesProcessed) {
            this.batchesProcessed = batchesProcessed;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public PipelineExecutionResult build() {
            return new PipelineExecutionResult(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("PipelineExecutionResult{id='%s', successful=%s, processed=%d, failed=%d, time=%dms}", 
                           pipelineId, successful, recordsProcessed, recordsFailed, executionTimeMs);
    }
}
