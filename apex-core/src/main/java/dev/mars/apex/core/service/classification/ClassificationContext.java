package dev.mars.apex.core.service.classification;

import java.util.Map;
import java.util.Objects;

/**
 * Context information for data classification operations.
 * Contains metadata and configuration needed for classification decisions.
 * 
 * <p>This class provides context for the classification process including:
 * <ul>
 *   <li>Source information (transport type, file name, etc.)</li>
 *   <li>Content metadata (size, format hints, etc.)</li>
 *   <li>Processing configuration (cache settings, confidence thresholds)</li>
 * </ul>
 * 
 * @since 1.0
 */
public final class ClassificationContext {

    private final ApexProcessingContext processingContext;
    private final Map<String, Object> metadata;
    private final boolean enableCaching;
    private final double confidenceThreshold;
    private final long timestamp;

    private ClassificationContext(Builder builder) {
        this.processingContext = builder.processingContext;
        this.metadata = Map.copyOf(builder.metadata);
        this.enableCaching = builder.enableCaching;
        this.confidenceThreshold = builder.confidenceThreshold;
        this.timestamp = builder.timestamp;
    }
    
    /**
     * Creates a new builder for ClassificationContext.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the source identifier (e.g., "rabbitmq", "rest-api", "file-system").
     *
     * @return source identifier
     */
    public String getSource() {
        return processingContext != null ? processingContext.getSource() : "unknown";
    }

    /**
     * Gets the file name or identifier.
     *
     * @return file name
     */
    public String getFileName() {
        return processingContext != null ? processingContext.getFileName() : null;
    }

    /**
     * Gets the content type hint.
     *
     * @return content type
     */
    public String getContentType() {
        // Content type can be stored in metadata if needed
        Object contentType = metadata.get("contentType");
        return contentType instanceof String ? (String) contentType : null;
    }

    /**
     * Gets the content size in bytes.
     *
     * @return content size
     */
    public long getContentSize() {
        return processingContext != null ? processingContext.getFileSize() : 0L;
    }
    
    /**
     * Gets additional metadata.
     * 
     * @return metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Checks if caching is enabled.
     * 
     * @return true if caching enabled
     */
    public boolean isCachingEnabled() {
        return enableCaching;
    }
    
    /**
     * Gets the confidence threshold for classification decisions.
     *
     * @return confidence threshold (0.0 to 1.0)
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    /**
     * Gets input data as string for content analysis.
     * This method is used by content-based detectors.
     *
     * @return input data as string, or null if not available
     */
    public String getInputDataAsString() {
        // This method would be implemented to extract string content
        // from the processing context or metadata
        Object inputData = metadata.get("inputData");
        if (inputData == null) {
            return null;
        }

        if (inputData instanceof String) {
            return (String) inputData;
        }

        if (inputData instanceof byte[]) {
            return new String((byte[]) inputData);
        }

        return inputData.toString();
    }

    /**
     * Gets the file size from processing context.
     *
     * @return file size in bytes, or null if not available
     */
    public Long getFileSize() {
        return processingContext != null ? processingContext.getFileSize() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassificationContext that = (ClassificationContext) o;
        return enableCaching == that.enableCaching &&
               Double.compare(that.confidenceThreshold, confidenceThreshold) == 0 &&
               timestamp == that.timestamp &&
               Objects.equals(processingContext, that.processingContext) &&
               Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processingContext, metadata, enableCaching, confidenceThreshold, timestamp);
    }
    
    @Override
    public String toString() {
        return "ClassificationContext{" +
               "processingContext=" + processingContext +
               ", metadata=" + metadata +
               ", enableCaching=" + enableCaching +
               ", confidenceThreshold=" + confidenceThreshold +
               ", timestamp=" + timestamp +
               '}';
    }
    
    /**
     * Builder for ClassificationContext.
     */
    public static final class Builder {
        private ApexProcessingContext processingContext;
        private Map<String, Object> metadata = Map.of();
        private boolean enableCaching = true;
        private double confidenceThreshold = 0.7;
        private long timestamp = System.currentTimeMillis();

        private Builder() {}
        
        /**
         * Sets the processing context.
         *
         * @param processingContext APEX processing context
         * @return this builder
         */
        public Builder processingContext(ApexProcessingContext processingContext) {
            this.processingContext = processingContext;
            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param timestamp timestamp in milliseconds
         * @return this builder
         */
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        /**
         * Sets the metadata.
         * 
         * @param metadata metadata map
         * @return this builder
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
            return this;
        }
        
        /**
         * Sets caching enabled flag.
         * 
         * @param enableCaching true to enable caching
         * @return this builder
         */
        public Builder enableCaching(boolean enableCaching) {
            this.enableCaching = enableCaching;
            return this;
        }
        
        /**
         * Sets the confidence threshold.
         *
         * @param confidenceThreshold confidence threshold (0.0 to 1.0)
         * @return this builder
         */
        public Builder confidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = Math.max(0.0, Math.min(1.0, confidenceThreshold));
            return this;
        }

        /**
         * Sets input data in metadata.
         *
         * @param inputData input data object
         * @return this builder
         */
        public Builder inputData(Object inputData) {
            if (this.metadata.isEmpty()) {
                this.metadata = new java.util.HashMap<>();
            } else if (!(this.metadata instanceof java.util.HashMap)) {
                this.metadata = new java.util.HashMap<>(this.metadata);
            }
            ((java.util.HashMap<String, Object>) this.metadata).put("inputData", inputData);
            return this;
        }
        
        /**
         * Builds the ClassificationContext.
         * 
         * @return new ClassificationContext instance
         */
        public ClassificationContext build() {
            return new ClassificationContext(this);
        }
    }
}
