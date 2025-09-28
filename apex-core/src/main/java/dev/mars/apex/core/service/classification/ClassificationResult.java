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

import dev.mars.apex.core.service.scenario.ScenarioConfiguration;

/**
 * Result of input data classification process.
 * 
 * This class encapsulates the complete result of classifying input data through
 * the multi-layer classification system, including file format detection,
 * content classification, business rule evaluation, and scenario routing.
 * 
 * DESIGN PRINCIPLES:
 * - Immutable result object for thread safety
 * - Rich information for debugging and monitoring
 * - Clear success/failure indication
 * - Support for caching decisions
 * 
 * CLASSIFICATION LAYERS:
 * 1. File Format Detection (JSON, XML, CSV, etc.)
 * 2. Content Classification (message types, data patterns)
 * 3. Business Classification (SpEL rule evaluation)
 * 4. Scenario Routing (final scenario selection)
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ClassificationResult {
    
    private final boolean successful;
    private final String fileFormat;
    private final String contentType;
    private final String businessClassification;
    private final String scenarioId;
    private final ScenarioConfiguration scenario;
    private final Object parsedData;
    private final double confidence;
    private final String errorMessage;
    private final Exception error;
    private final long classificationTimeMs;
    private final boolean cacheable;
    
    private ClassificationResult(Builder builder) {
        this.successful = builder.successful;
        this.fileFormat = builder.fileFormat;
        this.contentType = builder.contentType;
        this.businessClassification = builder.businessClassification;
        this.scenarioId = builder.scenarioId;
        this.scenario = builder.scenario;
        this.parsedData = builder.parsedData;
        this.confidence = builder.confidence;
        this.errorMessage = builder.errorMessage;
        this.error = builder.error;
        this.classificationTimeMs = builder.classificationTimeMs;
        this.cacheable = builder.cacheable;
    }
    
    /**
     * Creates a successful classification result.
     */
    public static ClassificationResult successful(String fileFormat, String contentType, 
                                                String businessClassification, String scenarioId,
                                                ScenarioConfiguration scenario, Object parsedData) {
        return builder()
            .successful(true)
            .fileFormat(fileFormat)
            .contentType(contentType)
            .businessClassification(businessClassification)
            .scenarioId(scenarioId)
            .scenario(scenario)
            .parsedData(parsedData)
            .confidence(0.9)
            .cacheable(true)
            .build();
    }
    
    /**
     * Creates a failed classification result.
     */
    public static ClassificationResult failed(String errorMessage) {
        return builder()
            .successful(false)
            .errorMessage(errorMessage)
            .confidence(0.0)
            .cacheable(false)
            .build();
    }
    
    /**
     * Creates a failed classification result with exception.
     */
    public static ClassificationResult failed(Exception error) {
        return builder()
            .successful(false)
            .errorMessage(error.getMessage())
            .error(error)
            .confidence(0.0)
            .cacheable(false)
            .build();
    }
    
    /**
     * Creates a new builder for constructing classification results.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public boolean isSuccessful() {
        return successful;
    }
    
    public boolean failed() {
        return !successful;
    }
    
    public String getFileFormat() {
        return fileFormat;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public String getBusinessClassification() {
        return businessClassification;
    }
    
    public String getScenarioId() {
        return scenarioId;
    }
    
    public ScenarioConfiguration getScenario() {
        return scenario;
    }
    
    public Object getParsedData() {
        return parsedData;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Exception getError() {
        return error;
    }
    
    public long getClassificationTimeMs() {
        return classificationTimeMs;
    }
    
    public boolean isCacheable() {
        return cacheable;
    }
    
    public boolean isExpired() {
        // For now, results don't expire - this can be enhanced later
        return false;
    }
    
    /**
     * Builder class for constructing ClassificationResult instances.
     */
    public static class Builder {
        private boolean successful = false;
        private String fileFormat;
        private String contentType;
        private String businessClassification;
        private String scenarioId;
        private ScenarioConfiguration scenario;
        private Object parsedData;
        private double confidence = 0.0;
        private String errorMessage;
        private Exception error;
        private long classificationTimeMs = 0;
        private boolean cacheable = false;
        
        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }
        
        public Builder fileFormat(String fileFormat) {
            this.fileFormat = fileFormat;
            return this;
        }
        
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder businessClassification(String businessClassification) {
            this.businessClassification = businessClassification;
            return this;
        }
        
        public Builder scenarioId(String scenarioId) {
            this.scenarioId = scenarioId;
            return this;
        }
        
        public Builder scenario(ScenarioConfiguration scenario) {
            this.scenario = scenario;
            return this;
        }
        
        public Builder parsedData(Object parsedData) {
            this.parsedData = parsedData;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder error(Exception error) {
            this.error = error;
            return this;
        }
        
        public Builder classificationTimeMs(long classificationTimeMs) {
            this.classificationTimeMs = classificationTimeMs;
            return this;
        }
        
        public Builder cacheable(boolean cacheable) {
            this.cacheable = cacheable;
            return this;
        }
        
        public ClassificationResult build() {
            return new ClassificationResult(this);
        }
    }
    
    @Override
    public String toString() {
        return "ClassificationResult{" +
                "successful=" + successful +
                ", fileFormat='" + fileFormat + '\'' +
                ", contentType='" + contentType + '\'' +
                ", businessClassification='" + businessClassification + '\'' +
                ", scenarioId='" + scenarioId + '\'' +
                ", confidence=" + confidence +
                ", classificationTimeMs=" + classificationTimeMs +
                (errorMessage != null ? ", errorMessage='" + errorMessage + '\'' : "") +
                '}';
    }
}
