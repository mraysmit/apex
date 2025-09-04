package dev.mars.apex.core.config.datasink;

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
 * Configuration for batch processing in data sinks.
 * 
 * This class defines how batch operations should be handled,
 * including batch sizes, timeouts, and transaction management.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class BatchConfig {
    
    /**
     * Enumeration of batch processing modes.
     */
    public enum BatchMode {
        SIZE_BASED("size-based", "Batch based on number of records"),
        TIME_BASED("time-based", "Batch based on time intervals"),
        HYBRID("hybrid", "Batch based on size or time, whichever comes first"),
        MANUAL("manual", "Manual batch control"),
        DISABLED("disabled", "No batching - process records individually");
        
        private final String code;
        private final String description;
        
        BatchMode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static BatchMode fromCode(String code) {
            if (code == null) {
                return SIZE_BASED; // Default
            }
            
            for (BatchMode mode : values()) {
                if (mode.code.equalsIgnoreCase(code)) {
                    return mode;
                }
            }
            
            return SIZE_BASED;
        }
    }
    
    /**
     * Enumeration of transaction modes for batch operations.
     */
    public enum TransactionMode {
        NONE("none", "No transaction management"),
        PER_BATCH("per-batch", "One transaction per batch"),
        PER_RECORD("per-record", "One transaction per record"),
        GLOBAL("global", "Single transaction for all batches");
        
        private final String code;
        private final String description;
        
        TransactionMode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static TransactionMode fromCode(String code) {
            if (code == null) {
                return PER_BATCH; // Default
            }
            
            for (TransactionMode mode : values()) {
                if (mode.code.equalsIgnoreCase(code)) {
                    return mode;
                }
            }
            
            return PER_BATCH;
        }
    }
    
    private Boolean enabled = true;
    private String mode = "size-based";
    private Integer batchSize = 100;
    private Integer maxBatchSize = 1000;
    private Integer minBatchSize = 1;
    
    // Time-based batching
    private Long batchTimeoutMs = 5000L; // 5 seconds
    private Long maxBatchTimeoutMs = 30000L; // 30 seconds
    private Long flushIntervalMs = 1000L; // 1 second
    
    // Transaction configuration
    private String transactionMode = "per-batch";
    private Long transactionTimeoutMs = 30000L; // 30 seconds
    private String isolationLevel = "READ_COMMITTED";
    
    // Memory management
    private Long maxMemoryUsageMB = 100L; // 100 MB
    private Boolean enableMemoryMonitoring = true;
    private Double memoryThresholdPercent = 0.8; // 80%
    
    // Performance tuning
    private Integer parallelBatches = 1;
    private Boolean enableCompression = false;
    private String compressionAlgorithm = "gzip";
    
    // Buffer management
    private Integer bufferSize = 1000;
    private Boolean enableBuffering = true;
    private Long bufferFlushIntervalMs = 2000L; // 2 seconds
    
    // Batch ordering
    private Boolean maintainOrder = true;
    private String orderingField;
    private String orderingDirection = "ASC";
    
    // Monitoring and metrics
    private Boolean enableMetrics = true;
    private Boolean logBatchStatistics = false;
    private Integer metricsReportingIntervalMs = 10000; // 10 seconds
    
    /**
     * Default constructor.
     */
    public BatchConfig() {
        // Defaults are set in field declarations
    }
    
    /**
     * Constructor with basic batch configuration.
     * 
     * @param enabled Whether batching is enabled
     * @param batchSize The batch size
     * @param batchTimeoutMs The batch timeout in milliseconds
     */
    public BatchConfig(Boolean enabled, Integer batchSize, Long batchTimeoutMs) {
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.batchTimeoutMs = batchTimeoutMs;
    }
    
    // Getters and setters
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public BatchMode getBatchMode() {
        return BatchMode.fromCode(mode);
    }
    
    public Integer getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
    
    public Integer getMaxBatchSize() {
        return maxBatchSize;
    }
    
    public void setMaxBatchSize(Integer maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }
    
    public Integer getMinBatchSize() {
        return minBatchSize;
    }
    
    public void setMinBatchSize(Integer minBatchSize) {
        this.minBatchSize = minBatchSize;
    }
    
    public Long getBatchTimeoutMs() {
        return batchTimeoutMs;
    }
    
    public void setBatchTimeoutMs(Long batchTimeoutMs) {
        this.batchTimeoutMs = batchTimeoutMs;
    }
    
    public Long getMaxBatchTimeoutMs() {
        return maxBatchTimeoutMs;
    }
    
    public void setMaxBatchTimeoutMs(Long maxBatchTimeoutMs) {
        this.maxBatchTimeoutMs = maxBatchTimeoutMs;
    }
    
    public Long getFlushIntervalMs() {
        return flushIntervalMs;
    }
    
    public void setFlushIntervalMs(Long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }
    
    public String getTransactionMode() {
        return transactionMode;
    }
    
    public void setTransactionMode(String transactionMode) {
        this.transactionMode = transactionMode;
    }
    
    public TransactionMode getTransactionModeEnum() {
        return TransactionMode.fromCode(transactionMode);
    }
    
    public Long getTransactionTimeoutMs() {
        return transactionTimeoutMs;
    }
    
    public void setTransactionTimeoutMs(Long transactionTimeoutMs) {
        this.transactionTimeoutMs = transactionTimeoutMs;
    }
    
    public String getIsolationLevel() {
        return isolationLevel;
    }
    
    public void setIsolationLevel(String isolationLevel) {
        this.isolationLevel = isolationLevel;
    }
    
    public Long getMaxMemoryUsageMB() {
        return maxMemoryUsageMB;
    }
    
    public void setMaxMemoryUsageMB(Long maxMemoryUsageMB) {
        this.maxMemoryUsageMB = maxMemoryUsageMB;
    }
    
    public Boolean getEnableMemoryMonitoring() {
        return enableMemoryMonitoring;
    }
    
    public void setEnableMemoryMonitoring(Boolean enableMemoryMonitoring) {
        this.enableMemoryMonitoring = enableMemoryMonitoring;
    }
    
    public Double getMemoryThresholdPercent() {
        return memoryThresholdPercent;
    }
    
    public void setMemoryThresholdPercent(Double memoryThresholdPercent) {
        this.memoryThresholdPercent = memoryThresholdPercent;
    }
    
    public Integer getParallelBatches() {
        return parallelBatches;
    }
    
    public void setParallelBatches(Integer parallelBatches) {
        this.parallelBatches = parallelBatches;
    }
    
    public Boolean getEnableCompression() {
        return enableCompression;
    }
    
    public void setEnableCompression(Boolean enableCompression) {
        this.enableCompression = enableCompression;
    }
    
    public String getCompressionAlgorithm() {
        return compressionAlgorithm;
    }
    
    public void setCompressionAlgorithm(String compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }
    
    public Integer getBufferSize() {
        return bufferSize;
    }
    
    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }
    
    public Boolean getEnableBuffering() {
        return enableBuffering;
    }
    
    public void setEnableBuffering(Boolean enableBuffering) {
        this.enableBuffering = enableBuffering;
    }
    
    public Long getBufferFlushIntervalMs() {
        return bufferFlushIntervalMs;
    }
    
    public void setBufferFlushIntervalMs(Long bufferFlushIntervalMs) {
        this.bufferFlushIntervalMs = bufferFlushIntervalMs;
    }
    
    public Boolean getMaintainOrder() {
        return maintainOrder;
    }
    
    public void setMaintainOrder(Boolean maintainOrder) {
        this.maintainOrder = maintainOrder;
    }
    
    public String getOrderingField() {
        return orderingField;
    }
    
    public void setOrderingField(String orderingField) {
        this.orderingField = orderingField;
    }
    
    public String getOrderingDirection() {
        return orderingDirection;
    }
    
    public void setOrderingDirection(String orderingDirection) {
        this.orderingDirection = orderingDirection;
    }
    
    public Boolean getEnableMetrics() {
        return enableMetrics;
    }
    
    public void setEnableMetrics(Boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public Boolean getLogBatchStatistics() {
        return logBatchStatistics;
    }
    
    public void setLogBatchStatistics(Boolean logBatchStatistics) {
        this.logBatchStatistics = logBatchStatistics;
    }
    
    public Integer getMetricsReportingIntervalMs() {
        return metricsReportingIntervalMs;
    }
    
    public void setMetricsReportingIntervalMs(Integer metricsReportingIntervalMs) {
        this.metricsReportingIntervalMs = metricsReportingIntervalMs;
    }
    
    /**
     * Calculate the effective batch size based on configuration and current conditions.
     * 
     * @param currentMemoryUsage Current memory usage in MB
     * @param pendingRecords Number of pending records
     * @return The effective batch size to use
     */
    public int calculateEffectiveBatchSize(long currentMemoryUsage, int pendingRecords) {
        int effectiveSize = batchSize;
        
        // Adjust for memory constraints
        if (enableMemoryMonitoring && maxMemoryUsageMB != null) {
            double memoryUsagePercent = (double) currentMemoryUsage / maxMemoryUsageMB;
            if (memoryUsagePercent > memoryThresholdPercent) {
                effectiveSize = Math.max(minBatchSize, effectiveSize / 2);
            }
        }
        
        // Ensure we don't exceed limits
        effectiveSize = Math.min(effectiveSize, maxBatchSize);
        effectiveSize = Math.max(effectiveSize, minBatchSize);
        
        // Don't exceed available records
        effectiveSize = Math.min(effectiveSize, pendingRecords);
        
        return effectiveSize;
    }
    
    /**
     * Validate the batch configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (batchSize != null && batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        
        if (maxBatchSize != null && maxBatchSize <= 0) {
            throw new IllegalArgumentException("Max batch size must be positive");
        }
        
        if (minBatchSize != null && minBatchSize <= 0) {
            throw new IllegalArgumentException("Min batch size must be positive");
        }
        
        if (batchSize != null && maxBatchSize != null && batchSize > maxBatchSize) {
            throw new IllegalArgumentException("Batch size cannot exceed max batch size");
        }
        
        if (batchSize != null && minBatchSize != null && batchSize < minBatchSize) {
            throw new IllegalArgumentException("Batch size cannot be less than min batch size");
        }
        
        if (batchTimeoutMs != null && batchTimeoutMs <= 0) {
            throw new IllegalArgumentException("Batch timeout must be positive");
        }
        
        if (parallelBatches != null && parallelBatches <= 0) {
            throw new IllegalArgumentException("Parallel batches must be positive");
        }
        
        if (memoryThresholdPercent != null && (memoryThresholdPercent <= 0 || memoryThresholdPercent > 1)) {
            throw new IllegalArgumentException("Memory threshold percent must be between 0 and 1");
        }
    }
    
    /**
     * Create a copy of this configuration.
     * 
     * @return A new BatchConfig with the same settings
     */
    public BatchConfig copy() {
        BatchConfig copy = new BatchConfig();
        copy.enabled = this.enabled;
        copy.mode = this.mode;
        copy.batchSize = this.batchSize;
        copy.maxBatchSize = this.maxBatchSize;
        copy.minBatchSize = this.minBatchSize;
        copy.batchTimeoutMs = this.batchTimeoutMs;
        copy.maxBatchTimeoutMs = this.maxBatchTimeoutMs;
        copy.flushIntervalMs = this.flushIntervalMs;
        copy.transactionMode = this.transactionMode;
        copy.transactionTimeoutMs = this.transactionTimeoutMs;
        copy.isolationLevel = this.isolationLevel;
        copy.maxMemoryUsageMB = this.maxMemoryUsageMB;
        copy.enableMemoryMonitoring = this.enableMemoryMonitoring;
        copy.memoryThresholdPercent = this.memoryThresholdPercent;
        copy.parallelBatches = this.parallelBatches;
        copy.enableCompression = this.enableCompression;
        copy.compressionAlgorithm = this.compressionAlgorithm;
        copy.bufferSize = this.bufferSize;
        copy.enableBuffering = this.enableBuffering;
        copy.bufferFlushIntervalMs = this.bufferFlushIntervalMs;
        copy.maintainOrder = this.maintainOrder;
        copy.orderingField = this.orderingField;
        copy.orderingDirection = this.orderingDirection;
        copy.enableMetrics = this.enableMetrics;
        copy.logBatchStatistics = this.logBatchStatistics;
        copy.metricsReportingIntervalMs = this.metricsReportingIntervalMs;
        
        return copy;
    }
}
