package dev.mars.apex.core.service.data.external.cache;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache statistics holder.
 * 
 * This class tracks various cache performance metrics including
 * hit/miss ratios, eviction counts, and timing information.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class CacheStatistics {
    
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong puts = new AtomicLong(0);
    private final AtomicLong removals = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong totalLoadTime = new AtomicLong(0);
    private final LocalDateTime createdAt;
    private volatile LocalDateTime lastResetTime;
    
    /**
     * Default constructor.
     */
    public CacheStatistics() {
        this.createdAt = LocalDateTime.now();
        this.lastResetTime = createdAt;
    }
    
    /**
     * Record a cache hit.
     */
    public void recordHit() {
        hits.incrementAndGet();
    }
    
    /**
     * Record a cache miss.
     */
    public void recordMiss() {
        misses.incrementAndGet();
    }
    
    /**
     * Record a cache put operation.
     */
    public void recordPut() {
        puts.incrementAndGet();
    }
    
    /**
     * Record a cache removal.
     */
    public void recordRemoval() {
        removals.incrementAndGet();
    }
    
    /**
     * Record a cache eviction.
     */
    public void recordEviction() {
        evictions.incrementAndGet();
    }
    
    /**
     * Record load time for cache operations.
     * 
     * @param loadTimeNanos Load time in nanoseconds
     */
    public void recordLoadTime(long loadTimeNanos) {
        totalLoadTime.addAndGet(loadTimeNanos);
    }
    
    /**
     * Get the number of cache hits.
     * 
     * @return Number of hits
     */
    public long getHits() {
        return hits.get();
    }
    
    /**
     * Get the number of cache misses.
     * 
     * @return Number of misses
     */
    public long getMisses() {
        return misses.get();
    }
    
    /**
     * Get the total number of cache requests.
     * 
     * @return Total requests (hits + misses)
     */
    public long getRequestCount() {
        return hits.get() + misses.get();
    }
    
    /**
     * Get the cache hit rate as a percentage.
     * 
     * @return Hit rate (0.0 to 100.0)
     */
    public double getHitRate() {
        long totalRequests = getRequestCount();
        if (totalRequests == 0) {
            return 0.0;
        }
        return (hits.get() * 100.0) / totalRequests;
    }
    
    /**
     * Get the cache miss rate as a percentage.
     * 
     * @return Miss rate (0.0 to 100.0)
     */
    public double getMissRate() {
        long totalRequests = getRequestCount();
        if (totalRequests == 0) {
            return 0.0;
        }
        return (misses.get() * 100.0) / totalRequests;
    }
    
    /**
     * Get the number of put operations.
     * 
     * @return Number of puts
     */
    public long getPuts() {
        return puts.get();
    }
    
    /**
     * Get the number of removal operations.
     * 
     * @return Number of removals
     */
    public long getRemovals() {
        return removals.get();
    }
    
    /**
     * Get the number of evictions.
     * 
     * @return Number of evictions
     */
    public long getEvictions() {
        return evictions.get();
    }
    
    /**
     * Get the total load time in nanoseconds.
     * 
     * @return Total load time
     */
    public long getTotalLoadTime() {
        return totalLoadTime.get();
    }
    
    /**
     * Get the average load time in nanoseconds.
     * 
     * @return Average load time
     */
    public double getAverageLoadTime() {
        long requests = getRequestCount();
        if (requests == 0) {
            return 0.0;
        }
        return (double) totalLoadTime.get() / requests;
    }
    
    /**
     * Get the average load time in milliseconds.
     * 
     * @return Average load time in milliseconds
     */
    public double getAverageLoadTimeMillis() {
        return getAverageLoadTime() / 1_000_000.0;
    }
    
    /**
     * Get the creation time of these statistics.
     * 
     * @return Creation time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Get the last reset time.
     * 
     * @return Last reset time
     */
    public LocalDateTime getLastResetTime() {
        return lastResetTime;
    }
    
    /**
     * Reset all statistics to zero.
     */
    public void reset() {
        hits.set(0);
        misses.set(0);
        puts.set(0);
        removals.set(0);
        evictions.set(0);
        totalLoadTime.set(0);
        lastResetTime = LocalDateTime.now();
    }
    
    /**
     * Create a snapshot of current statistics.
     * 
     * @return A new CacheStatistics instance with current values
     */
    public CacheStatistics snapshot() {
        CacheStatistics snapshot = new CacheStatistics();
        snapshot.hits.set(this.hits.get());
        snapshot.misses.set(this.misses.get());
        snapshot.puts.set(this.puts.get());
        snapshot.removals.set(this.removals.get());
        snapshot.evictions.set(this.evictions.get());
        snapshot.totalLoadTime.set(this.totalLoadTime.get());
        return snapshot;
    }
    
    @Override
    public String toString() {
        return "CacheStatistics{" +
               "hits=" + getHits() +
               ", misses=" + getMisses() +
               ", hitRate=" + String.format("%.2f%%", getHitRate()) +
               ", puts=" + getPuts() +
               ", removals=" + getRemovals() +
               ", evictions=" + getEvictions() +
               ", avgLoadTime=" + String.format("%.2fms", getAverageLoadTimeMillis()) +
               '}';
    }
}
