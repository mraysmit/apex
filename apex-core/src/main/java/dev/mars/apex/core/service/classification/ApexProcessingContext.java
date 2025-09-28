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

import java.util.HashMap;
import java.util.Map;

/**
 * Processing context for APEX input data classification and processing.
 * 
 * This class provides contextual information about the input data being processed,
 * including metadata about the source, timing, and processing requirements.
 * 
 * DESIGN PRINCIPLES:
 * - Immutable after construction for thread safety
 * - Builder pattern for flexible construction
 * - Rich metadata support for classification decisions
 * - Transport-agnostic design (works with any input source)
 * 
 * USAGE EXAMPLE:
 * ```java
 * ApexProcessingContext context = ApexProcessingContext.builder()
 *     .source("rabbitmq")
 *     .fileName("trade_data.json")
 *     .metadata(Map.of("region", "US", "priority", "HIGH"))
 *     .startTime(System.currentTimeMillis())
 *     .build();
 * ```
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ApexProcessingContext {
    
    private final String source;
    private final String fileName;
    private final Long fileSize;
    private final Map<String, Object> metadata;
    private final long startTime;
    private final String correlationId;
    
    private ApexProcessingContext(Builder builder) {
        this.source = builder.source;
        this.fileName = builder.fileName;
        this.fileSize = builder.fileSize;
        this.metadata = builder.metadata != null ? new HashMap<>(builder.metadata) : new HashMap<>();
        this.startTime = builder.startTime;
        this.correlationId = builder.correlationId;
    }
    
    /**
     * Creates a default processing context for simple use cases.
     */
    public static ApexProcessingContext defaultContext() {
        return builder().build();
    }
    
    /**
     * Creates a new builder for constructing processing contexts.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getSource() {
        return source;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Builder class for constructing ApexProcessingContext instances.
     */
    public static class Builder {
        private String source = "unknown";
        private String fileName;
        private Long fileSize;
        private Map<String, Object> metadata = new HashMap<>();
        private long startTime = System.currentTimeMillis();
        private String correlationId;
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }
        
        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata = new HashMap<>(metadata);
            }
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public ApexProcessingContext build() {
            return new ApexProcessingContext(this);
        }
    }
    
    @Override
    public String toString() {
        return "ApexProcessingContext{" +
                "source='" + source + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", startTime=" + startTime +
                ", correlationId='" + correlationId + '\'' +
                ", metadataKeys=" + metadata.keySet() +
                '}';
    }
}
