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
 * Metrics collection class for external data sources.
 * 
 * This class tracks performance and usage statistics for data sources,
 * including response times, error rates, cache statistics, and throughput metrics.
 * 
 * All metrics are thread-safe and can be updated concurrently.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceMetrics {
    
    // Request metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    // Timing metrics
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastRequestTime = new AtomicReference<>();
    
    // Cache metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);
    
    // Connection metrics
    private final AtomicLong connectionAttempts = new AtomicLong(0);
    private final AtomicLong successfulConnections = new AtomicLong(0);
    private final AtomicLong connectionFailures = new AtomicLong(0);
    
    // Data volume metrics
    private final AtomicLong bytesRead = new AtomicLong(0);
    private final AtomicLong bytesWritten = new AtomicLong(0);
    private final AtomicLong recordsProcessed = new AtomicLong(0);
    
    // Lifecycle
    private final LocalDateTime createdAt;
    private final AtomicReference<LocalDateTime> lastResetTime = new AtomicReference<>();
    
    /**
     * Create a new DataSourceMetrics instance.
     */
    public DataSourceMetrics() {
        this.createdAt = LocalDateTime.now();
        this.lastResetTime.set(createdAt);
    }
    
    // Request tracking methods
    
    /**
     * Record a successful request with response time.
     * 
     * @param responseTimeMs Response time in milliseconds
     */
    public void recordSuccessfulRequest(long responseTimeMs) {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
        recordResponseTime(responseTimeMs);
        lastRequestTime.set(LocalDateTime.now());
    }
    
    /**
     * Record a failed request with response time.
     * 
     * @param responseTimeMs Response time in milliseconds
     */
    public void recordFailedRequest(long responseTimeMs) {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
        recordResponseTime(responseTimeMs);
        lastRequestTime.set(LocalDateTime.now());
    }
    
    /**
     * Record response time and update min/max values.
     * 
     * @param responseTimeMs Response time in milliseconds
     */
    private void recordResponseTime(long responseTimeMs) {
        totalResponseTime.addAndGet(responseTimeMs);
        
        // Update min response time
        long currentMin = minResponseTime.get();
        while (responseTimeMs < currentMin && 
               !minResponseTime.compareAndSet(currentMin, responseTimeMs)) {
            currentMin = minResponseTime.get();
        }
        
        // Update max response time
        long currentMax = maxResponseTime.get();
        while (responseTimeMs > currentMax && 
               !maxResponseTime.compareAndSet(currentMax, responseTimeMs)) {
            currentMax = maxResponseTime.get();
        }
    }
    
    // Cache tracking methods
    
    /**
     * Record a cache hit.
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    /**
     * Record a cache miss.
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    /**
     * Record a cache eviction.
     */
    public void recordCacheEviction() {
        cacheEvictions.incrementAndGet();
    }
    
    // Connection tracking methods
    
    /**
     * Record a connection attempt.
     */
    public void recordConnectionAttempt() {
        connectionAttempts.incrementAndGet();
    }
    
    /**
     * Record a successful connection.
     */
    public void recordSuccessfulConnection() {
        successfulConnections.incrementAndGet();
    }
    
    /**
     * Record a connection failure.
     */
    public void recordConnectionFailure() {
        connectionFailures.incrementAndGet();
    }
    
    // Data volume tracking methods
    
    /**
     * Record bytes read from the data source.
     * 
     * @param bytes Number of bytes read
     */
    public void recordBytesRead(long bytes) {
        bytesRead.addAndGet(bytes);
    }
    
    /**
     * Record bytes written to the data source.
     * 
     * @param bytes Number of bytes written
     */
    public void recordBytesWritten(long bytes) {
        bytesWritten.addAndGet(bytes);
    }
    
    /**
     * Record the number of records processed.
     * 
     * @param records Number of records processed
     */
    public void recordRecordsProcessed(long records) {
        recordsProcessed.addAndGet(records);
    }
    
    // Calculated metrics
    
    /**
     * Get the success rate as a percentage.
     * 
     * @return Success rate (0.0 to 100.0)
     */
    public double getSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (successfulRequests.get() * 100.0) / total;
    }
    
    /**
     * Get the error rate as a percentage.
     * 
     * @return Error rate (0.0 to 100.0)
     */
    public double getErrorRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (failedRequests.get() * 100.0) / total;
    }
    
    /**
     * Get the average response time in milliseconds.
     * 
     * @return Average response time, or 0 if no requests
     */
    public double getAverageResponseTime() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalResponseTime.get() / total;
    }
    
    /**
     * Get the cache hit rate as a percentage.
     * 
     * @return Cache hit rate (0.0 to 100.0)
     */
    public double getCacheHitRate() {
        long totalCacheRequests = cacheHits.get() + cacheMisses.get();
        if (totalCacheRequests == 0) {
            return 0.0;
        }
        return (cacheHits.get() * 100.0) / totalCacheRequests;
    }
    
    /**
     * Get the connection success rate as a percentage.
     * 
     * @return Connection success rate (0.0 to 100.0)
     */
    public double getConnectionSuccessRate() {
        long totalAttempts = connectionAttempts.get();
        if (totalAttempts == 0) {
            return 0.0;
        }
        return (successfulConnections.get() * 100.0) / totalAttempts;
    }
    
    /**
     * Reset all metrics to zero.
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.set(0);
        minResponseTime.set(Long.MAX_VALUE);
        maxResponseTime.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        cacheEvictions.set(0);
        connectionAttempts.set(0);
        successfulConnections.set(0);
        connectionFailures.set(0);
        bytesRead.set(0);
        bytesWritten.set(0);
        recordsProcessed.set(0);
        lastResetTime.set(LocalDateTime.now());
    }
    
    // Getters for all metrics
    
    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public long getFailedRequests() { return failedRequests.get(); }
    public long getTotalResponseTime() { return totalResponseTime.get(); }
    public long getMinResponseTime() { 
        long min = minResponseTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    public long getMaxResponseTime() { return maxResponseTime.get(); }
    public LocalDateTime getLastRequestTime() { return lastRequestTime.get(); }
    public long getCacheHits() { return cacheHits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }
    public long getCacheEvictions() { return cacheEvictions.get(); }
    public long getConnectionAttempts() { return connectionAttempts.get(); }
    public long getSuccessfulConnections() { return successfulConnections.get(); }
    public long getConnectionFailures() { return connectionFailures.get(); }
    public long getBytesRead() { return bytesRead.get(); }
    public long getBytesWritten() { return bytesWritten.get(); }
    public long getRecordsProcessed() { return recordsProcessed.get(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastResetTime() { return lastResetTime.get(); }
    
    @Override
    public String toString() {
        return "DataSourceMetrics{" +
               "totalRequests=" + getTotalRequests() +
               ", successRate=" + String.format("%.2f%%", getSuccessRate()) +
               ", avgResponseTime=" + String.format("%.2fms", getAverageResponseTime()) +
               ", cacheHitRate=" + String.format("%.2f%%", getCacheHitRate()) +
               ", recordsProcessed=" + getRecordsProcessed() +
               '}';
    }
}
