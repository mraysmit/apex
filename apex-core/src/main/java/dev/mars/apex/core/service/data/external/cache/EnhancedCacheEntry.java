package dev.mars.apex.core.service.data.external.cache;

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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced cache entry that supports advanced caching features including:
 * - TTL-based expiration
 * - Max idle time expiration
 * - LRU access tracking
 * - Creation and access timestamps
 * - Hit count tracking
 * 
 * This unified cache entry is used across all APEX data sources to provide
 * consistent caching behavior and advanced cache management features.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class EnhancedCacheEntry {
    
    private final String key;
    private final Object data;
    private final long creationTime;
    private final long ttlExpiryTime;
    private final long maxIdleSeconds;
    
    // LRU tracking
    private volatile long lastAccessTime;
    private final AtomicLong accessCount;
    
    // Additional metadata
    private final Instant lastModified;
    private final long dataSize;
    
    /**
     * Constructor for basic cache entry with TTL only.
     * 
     * @param key The cache key
     * @param data The cached data
     * @param ttlSeconds Time-to-live in seconds (0 = no TTL expiration)
     */
    public EnhancedCacheEntry(String key, Object data, long ttlSeconds) {
        this(key, data, ttlSeconds, 0L);
    }
    
    /**
     * Constructor for cache entry with TTL and max idle time.
     * 
     * @param key The cache key
     * @param data The cached data
     * @param ttlSeconds Time-to-live in seconds (0 = no TTL expiration)
     * @param maxIdleSeconds Maximum idle time in seconds (0 = no idle expiration)
     */
    public EnhancedCacheEntry(String key, Object data, long ttlSeconds, long maxIdleSeconds) {
        this.key = key;
        this.data = data;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessTime = this.creationTime;
        this.maxIdleSeconds = maxIdleSeconds;
        this.accessCount = new AtomicLong(1); // Created with 1 access
        this.lastModified = Instant.now();
        
        // Calculate TTL expiry time
        this.ttlExpiryTime = ttlSeconds > 0 ? 
            this.creationTime + (ttlSeconds * 1000L) : 
            Long.MAX_VALUE; // No TTL expiration
            
        // Estimate data size (simplified)
        this.dataSize = estimateDataSize(data);
    }
    
    /**
     * Get the cached data and update access tracking.
     * 
     * @return The cached data
     */
    public Object getData() {
        updateAccessTracking();
        return data;
    }
    
    /**
     * Get the cache key.
     * 
     * @return The cache key
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Check if this cache entry has expired based on TTL or max idle time.
     * 
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        
        // Check TTL expiration
        if (currentTime > ttlExpiryTime) {
            return true;
        }
        
        // Check max idle time expiration
        if (maxIdleSeconds > 0) {
            long idleTime = currentTime - lastAccessTime;
            if (idleTime > (maxIdleSeconds * 1000L)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if this entry has expired due to TTL only.
     * 
     * @return true if TTL expired, false otherwise
     */
    public boolean isTtlExpired() {
        return System.currentTimeMillis() > ttlExpiryTime;
    }
    
    /**
     * Check if this entry has expired due to idle time only.
     * 
     * @return true if idle time expired, false otherwise
     */
    public boolean isIdleExpired() {
        if (maxIdleSeconds <= 0) {
            return false;
        }
        long idleTime = System.currentTimeMillis() - lastAccessTime;
        return idleTime > (maxIdleSeconds * 1000L);
    }
    
    /**
     * Get the creation time of this cache entry.
     * 
     * @return Creation time in milliseconds
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Get the last access time of this cache entry.
     * 
     * @return Last access time in milliseconds
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * Get the access count for this cache entry.
     * 
     * @return Number of times this entry has been accessed
     */
    public long getAccessCount() {
        return accessCount.get();
    }
    
    /**
     * Get the age of this cache entry in milliseconds.
     * 
     * @return Age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }
    
    /**
     * Get the idle time of this cache entry in milliseconds.
     * 
     * @return Idle time in milliseconds
     */
    public long getIdleTime() {
        return System.currentTimeMillis() - lastAccessTime;
    }
    
    /**
     * Get the estimated size of the cached data.
     * 
     * @return Estimated data size in bytes
     */
    public long getDataSize() {
        return dataSize;
    }
    
    /**
     * Get the last modified time.
     * 
     * @return Last modified instant
     */
    public Instant getLastModified() {
        return lastModified;
    }
    
    /**
     * Get the TTL expiry time.
     * 
     * @return TTL expiry time in milliseconds
     */
    public long getTtlExpiryTime() {
        return ttlExpiryTime;
    }
    
    /**
     * Get the max idle seconds configuration.
     * 
     * @return Max idle seconds
     */
    public long getMaxIdleSeconds() {
        return maxIdleSeconds;
    }
    
    /**
     * Update access tracking for LRU eviction.
     */
    private void updateAccessTracking() {
        this.lastAccessTime = System.currentTimeMillis();
        this.accessCount.incrementAndGet();
    }
    
    /**
     * Estimate the size of the cached data (simplified implementation).
     * 
     * @param data The data to estimate
     * @return Estimated size in bytes
     */
    private long estimateDataSize(Object data) {
        if (data == null) {
            return 0L;
        }
        
        if (data instanceof String) {
            return ((String) data).length() * 2L; // Rough estimate for UTF-16
        } else if (data instanceof byte[]) {
            return ((byte[]) data).length;
        } else {
            // Very rough estimate for other objects
            return 100L; // Default estimate
        }
    }
    
    @Override
    public String toString() {
        return String.format("EnhancedCacheEntry{key='%s', age=%dms, idleTime=%dms, accessCount=%d, expired=%s}", 
            key, getAge(), getIdleTime(), getAccessCount(), isExpired());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancedCacheEntry that = (EnhancedCacheEntry) o;
        return key != null ? key.equals(that.key) : that.key == null;
    }
    
    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
