package dev.mars.apex.core.service.data.external;

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

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Metrics collection for data sink operations.
 * 
 * This class tracks performance and operational metrics for data sinks,
 * including write times, error rates, throughput, and connection health.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSinkMetrics {
    
    // Write operation metrics
    private final AtomicLong totalWrites = new AtomicLong(0);
    private final AtomicLong successfulWrites = new AtomicLong(0);
    private final AtomicLong failedWrites = new AtomicLong(0);
    private final AtomicLong totalRecordsWritten = new AtomicLong(0);
    
    // Batch operation metrics
    private final AtomicLong totalBatches = new AtomicLong(0);
    private final AtomicLong successfulBatches = new AtomicLong(0);
    private final AtomicLong failedBatches = new AtomicLong(0);
    private final AtomicLong partialBatches = new AtomicLong(0);
    
    // Timing metrics
    private final AtomicLong totalWriteTime = new AtomicLong(0);
    private final AtomicLong minWriteTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxWriteTime = new AtomicLong(0);
    
    // Connection metrics
    private final AtomicLong connectionAttempts = new AtomicLong(0);
    private final AtomicLong connectionFailures = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastConnectionTime = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastWriteTime = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastErrorTime = new AtomicReference<>();
    
    // Health metrics
    private final AtomicLong healthCheckCount = new AtomicLong(0);
    private final AtomicLong healthCheckFailures = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastHealthCheck = new AtomicReference<>();
    
    // Retry metrics
    private final AtomicLong retryAttempts = new AtomicLong(0);
    private final AtomicLong retrySuccesses = new AtomicLong(0);
    
    /**
     * Record a successful write operation.
     * 
     * @param writeTimeMs The time taken for the write operation in milliseconds
     * @param recordCount The number of records written
     */
    public void recordSuccessfulWrite(long writeTimeMs, int recordCount) {
        totalWrites.incrementAndGet();
        successfulWrites.incrementAndGet();
        totalRecordsWritten.addAndGet(recordCount);
        
        updateWriteTime(writeTimeMs);
        lastWriteTime.set(LocalDateTime.now());
    }
    
    /**
     * Record a failed write operation.
     * 
     * @param writeTimeMs The time taken for the failed write operation in milliseconds
     */
    public void recordFailedWrite(long writeTimeMs) {
        totalWrites.incrementAndGet();
        failedWrites.incrementAndGet();
        
        updateWriteTime(writeTimeMs);
        lastErrorTime.set(LocalDateTime.now());
    }
    
    /**
     * Record a successful batch operation.
     * 
     * @param batchTimeMs The time taken for the batch operation in milliseconds
     * @param recordCount The number of records in the batch
     */
    public void recordSuccessfulBatch(long batchTimeMs, int recordCount) {
        totalBatches.incrementAndGet();
        successfulBatches.incrementAndGet();
        totalRecordsWritten.addAndGet(recordCount);
        
        updateWriteTime(batchTimeMs);
        lastWriteTime.set(LocalDateTime.now());
    }
    
    /**
     * Record a failed batch operation.
     * 
     * @param batchTimeMs The time taken for the failed batch operation in milliseconds
     */
    public void recordFailedBatch(long batchTimeMs) {
        totalBatches.incrementAndGet();
        failedBatches.incrementAndGet();
        
        updateWriteTime(batchTimeMs);
        lastErrorTime.set(LocalDateTime.now());
    }
    
    /**
     * Record a partially successful batch operation.
     * 
     * @param batchTimeMs The time taken for the batch operation in milliseconds
     * @param successfulRecords The number of successfully written records
     * @param failedRecords The number of failed records
     */
    public void recordPartialBatch(long batchTimeMs, int successfulRecords, int failedRecords) {
        totalBatches.incrementAndGet();
        partialBatches.incrementAndGet();
        totalRecordsWritten.addAndGet(successfulRecords);
        
        updateWriteTime(batchTimeMs);
        lastWriteTime.set(LocalDateTime.now());
        lastErrorTime.set(LocalDateTime.now());
    }
    
    /**
     * Record a connection attempt.
     * 
     * @param successful Whether the connection was successful
     */
    public void recordConnectionAttempt(boolean successful) {
        connectionAttempts.incrementAndGet();
        if (successful) {
            lastConnectionTime.set(LocalDateTime.now());
        } else {
            connectionFailures.incrementAndGet();
            lastErrorTime.set(LocalDateTime.now());
        }
    }
    
    /**
     * Record a health check.
     * 
     * @param successful Whether the health check was successful
     */
    public void recordHealthCheck(boolean successful) {
        healthCheckCount.incrementAndGet();
        lastHealthCheck.set(LocalDateTime.now());
        
        if (!successful) {
            healthCheckFailures.incrementAndGet();
            lastErrorTime.set(LocalDateTime.now());
        }
    }
    
    /**
     * Record a retry attempt.
     * 
     * @param successful Whether the retry was successful
     */
    public void recordRetryAttempt(boolean successful) {
        retryAttempts.incrementAndGet();
        if (successful) {
            retrySuccesses.incrementAndGet();
        }
    }
    
    /**
     * Update write time statistics.
     */
    private void updateWriteTime(long writeTimeMs) {
        totalWriteTime.addAndGet(writeTimeMs);
        
        // Update min time
        long currentMin = minWriteTime.get();
        while (writeTimeMs < currentMin && !minWriteTime.compareAndSet(currentMin, writeTimeMs)) {
            currentMin = minWriteTime.get();
        }
        
        // Update max time
        long currentMax = maxWriteTime.get();
        while (writeTimeMs > currentMax && !maxWriteTime.compareAndSet(currentMax, writeTimeMs)) {
            currentMax = maxWriteTime.get();
        }
    }
    
    // Getter methods for metrics
    
    public long getTotalWrites() {
        return totalWrites.get();
    }
    
    public long getSuccessfulWrites() {
        return successfulWrites.get();
    }
    
    public long getFailedWrites() {
        return failedWrites.get();
    }
    
    public long getTotalRecordsWritten() {
        return totalRecordsWritten.get();
    }
    
    public long getTotalBatches() {
        return totalBatches.get();
    }
    
    public long getSuccessfulBatches() {
        return successfulBatches.get();
    }
    
    public long getFailedBatches() {
        return failedBatches.get();
    }
    
    public long getPartialBatches() {
        return partialBatches.get();
    }
    
    public double getWriteSuccessRate() {
        long total = totalWrites.get();
        return total > 0 ? (double) successfulWrites.get() / total : 0.0;
    }
    
    public double getBatchSuccessRate() {
        long total = totalBatches.get();
        return total > 0 ? (double) successfulBatches.get() / total : 0.0;
    }
    
    public double getAverageWriteTime() {
        long total = totalWrites.get();
        return total > 0 ? (double) totalWriteTime.get() / total : 0.0;
    }
    
    public long getMinWriteTime() {
        long min = minWriteTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    public long getMaxWriteTime() {
        return maxWriteTime.get();
    }
    
    public double getThroughputRecordsPerSecond() {
        // Calculate throughput based on last minute of activity
        // This is a simplified calculation - in production, you might want a sliding window
        long totalTime = totalWriteTime.get();
        long totalRecords = totalRecordsWritten.get();
        
        if (totalTime > 0) {
            return (double) totalRecords / (totalTime / 1000.0);
        }
        return 0.0;
    }
    
    public long getConnectionAttempts() {
        return connectionAttempts.get();
    }
    
    public long getConnectionFailures() {
        return connectionFailures.get();
    }
    
    public double getConnectionSuccessRate() {
        long total = connectionAttempts.get();
        return total > 0 ? (double) (total - connectionFailures.get()) / total : 0.0;
    }
    
    public long getHealthCheckCount() {
        return healthCheckCount.get();
    }
    
    public long getHealthCheckFailures() {
        return healthCheckFailures.get();
    }
    
    public double getHealthCheckSuccessRate() {
        long total = healthCheckCount.get();
        return total > 0 ? (double) (total - healthCheckFailures.get()) / total : 0.0;
    }
    
    public long getRetryAttempts() {
        return retryAttempts.get();
    }
    
    public long getRetrySuccesses() {
        return retrySuccesses.get();
    }
    
    public double getRetrySuccessRate() {
        long total = retryAttempts.get();
        return total > 0 ? (double) retrySuccesses.get() / total : 0.0;
    }
    
    public LocalDateTime getLastConnectionTime() {
        return lastConnectionTime.get();
    }
    
    public LocalDateTime getLastWriteTime() {
        return lastWriteTime.get();
    }
    
    public LocalDateTime getLastErrorTime() {
        return lastErrorTime.get();
    }
    
    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck.get();
    }
    
    /**
     * Reset all metrics to zero.
     */
    public void reset() {
        totalWrites.set(0);
        successfulWrites.set(0);
        failedWrites.set(0);
        totalRecordsWritten.set(0);
        totalBatches.set(0);
        successfulBatches.set(0);
        failedBatches.set(0);
        partialBatches.set(0);
        totalWriteTime.set(0);
        minWriteTime.set(Long.MAX_VALUE);
        maxWriteTime.set(0);
        connectionAttempts.set(0);
        connectionFailures.set(0);
        healthCheckCount.set(0);
        healthCheckFailures.set(0);
        retryAttempts.set(0);
        retrySuccesses.set(0);
        lastConnectionTime.set(null);
        lastWriteTime.set(null);
        lastErrorTime.set(null);
        lastHealthCheck.set(null);
    }
    
    @Override
    public String toString() {
        return String.format(
            "DataSinkMetrics[writes=%d/%d (%.1f%%), batches=%d/%d (%.1f%%), avgTime=%.1fms, throughput=%.1f rec/s]",
            successfulWrites.get(), totalWrites.get(), getWriteSuccessRate() * 100,
            successfulBatches.get(), totalBatches.get(), getBatchSuccessRate() * 100,
            getAverageWriteTime(), getThroughputRecordsPerSecond()
        );
    }
}
