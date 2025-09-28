package dev.mars.apex.core.service.classification;

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
 * Complete result of APEX input data classification and processing.
 * 
 * This class encapsulates the entire result of the classify-and-process operation,
 * including both the classification decision and the processing outcome.
 * 
 * DESIGN PRINCIPLES:
 * - Immutable result object for thread safety
 * - Complete audit trail of processing decisions
 * - Rich information for monitoring and debugging
 * - Clear success/failure indication
 * 
 * USAGE EXAMPLE:
 * ```java
 * ApexProcessingResult result = apexEngine.classifyAndProcessData(inputData, context);
 * 
 * if (result.isSuccess()) {
 *     System.out.println("Classified as: " + result.getClassification().getBusinessClassification());
 *     System.out.println("Processed by: " + result.getClassification().getScenarioId());
 *     System.out.println("Execution time: " + result.getExecutionTime() + "ms");
 * } else {
 *     System.err.println("Processing failed: " + result.getError());
 * }
 * ```
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ApexProcessingResult {
    
    private final boolean success;
    private final ClassificationResult classification;
    private final Object processingResult;
    private final long executionTime;
    private final String error;
    private final Exception exception;
    private final long timestamp;
    
    private ApexProcessingResult(Builder builder) {
        this.success = builder.success;
        this.classification = builder.classification;
        this.processingResult = builder.processingResult;
        this.executionTime = builder.executionTime;
        this.error = builder.error;
        this.exception = builder.exception;
        this.timestamp = builder.timestamp;
    }
    
    /**
     * Creates a successful processing result.
     */
    public static ApexProcessingResult successful(ClassificationResult classification, 
                                                Object processingResult, long executionTime) {
        return builder()
            .success(true)
            .classification(classification)
            .processingResult(processingResult)
            .executionTime(executionTime)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Creates a failed processing result.
     */
    public static ApexProcessingResult failed(String error) {
        return builder()
            .success(false)
            .error(error)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Creates a failed processing result with exception.
     */
    public static ApexProcessingResult failed(Exception exception) {
        return builder()
            .success(false)
            .error(exception.getMessage())
            .exception(exception)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Creates a failed processing result with classification but processing failure.
     */
    public static ApexProcessingResult failed(ClassificationResult classification, String error) {
        return builder()
            .success(false)
            .classification(classification)
            .error(error)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Creates a new builder for constructing processing results.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailed() {
        return !success;
    }
    
    public ClassificationResult getClassification() {
        return classification;
    }
    
    public Object getProcessingResult() {
        return processingResult;
    }
    
    public long getExecutionTime() {
        return executionTime;
    }
    
    public String getError() {
        return error;
    }
    
    public Exception getException() {
        return exception;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Builder class for constructing ApexProcessingResult instances.
     */
    public static class Builder {
        private boolean success = false;
        private ClassificationResult classification;
        private Object processingResult;
        private long executionTime = 0;
        private String error;
        private Exception exception;
        private long timestamp = System.currentTimeMillis();
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder classification(ClassificationResult classification) {
            this.classification = classification;
            return this;
        }
        
        public Builder processingResult(Object processingResult) {
            this.processingResult = processingResult;
            return this;
        }
        
        public Builder executionTime(long executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder error(String error) {
            this.error = error;
            return this;
        }
        
        public Builder exception(Exception exception) {
            this.exception = exception;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public ApexProcessingResult build() {
            return new ApexProcessingResult(this);
        }
    }
    
    @Override
    public String toString() {
        return "ApexProcessingResult{" +
                "success=" + success +
                ", executionTime=" + executionTime +
                ", timestamp=" + timestamp +
                (classification != null ? ", classification=" + classification : "") +
                (error != null ? ", error='" + error + '\'' : "") +
                '}';
    }
}
