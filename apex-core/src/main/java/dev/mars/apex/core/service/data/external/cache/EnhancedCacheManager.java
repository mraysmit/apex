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

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Enhanced cache manager that provides unified caching functionality across all APEX data sources.
 * 
 * Features:
 * - TTL-based expiration
 * - Max idle time expiration  
 * - LRU eviction policy
 * - Key prefix support
 * - Size-based eviction
 * - Cache statistics
 * - Thread-safe operations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class EnhancedCacheManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCacheManager.class);
    
    private final ConcurrentHashMap<String, EnhancedCacheEntry> cache;
    private final ReadWriteLock evictionLock;
    private final AtomicInteger currentSize;
    
    // Configuration
    private final int maxSize;
    private final long defaultTtlSeconds;
    private final long maxIdleSeconds;
    private final String keyPrefix;
    private final boolean enabled;
    
    // Statistics
    private final CacheStatistics statistics;
    
    /**
     * Constructor with data source configuration.
     * 
     * @param configuration The data source configuration containing cache settings
     */
    public EnhancedCacheManager(DataSourceConfiguration configuration) {
        this.cache = new ConcurrentHashMap<>();
        this.evictionLock = new ReentrantReadWriteLock();
        this.currentSize = new AtomicInteger(0);
        this.statistics = new CacheStatistics();
        
        // Extract cache configuration
        CacheConfig cacheConfig = configuration.getCache();
        if (cacheConfig != null) {
            this.enabled = cacheConfig.isEnabled();
            this.maxSize = cacheConfig.getMaxSize() != null ? cacheConfig.getMaxSize() : 10000;
            this.defaultTtlSeconds = cacheConfig.getTtlSeconds() != null ? cacheConfig.getTtlSeconds() : 3600L;
            this.maxIdleSeconds = cacheConfig.getMaxIdleSeconds() != null ? cacheConfig.getMaxIdleSeconds() : 0L;
            this.keyPrefix = cacheConfig.getKeyPrefix() != null ? cacheConfig.getKeyPrefix() : "";
        } else {
            this.enabled = false;
            this.maxSize = 10000;
            this.defaultTtlSeconds = 3600L;
            this.maxIdleSeconds = 0L;
            this.keyPrefix = "";
        }
        
        LOGGER.info("Enhanced cache manager initialized: enabled={}, maxSize={}, defaultTTL={}s, maxIdle={}s, keyPrefix='{}'", 
            enabled, maxSize, defaultTtlSeconds, maxIdleSeconds, keyPrefix);
    }
    
    /**
     * Check if caching is enabled.
     * 
     * @return true if caching is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Put an entry in the cache with default TTL.
     * 
     * @param key The cache key (without prefix)
     * @param data The data to cache
     */
    public void put(String key, Object data) {
        put(key, data, defaultTtlSeconds);
    }
    
    /**
     * Put an entry in the cache with specified TTL.
     * 
     * @param key The cache key (without prefix)
     * @param data The data to cache
     * @param ttlSeconds Time-to-live in seconds
     */
    public void put(String key, Object data, long ttlSeconds) {
        if (!enabled || key == null || data == null) {
            return;
        }
        
        String prefixedKey = buildPrefixedKey(key);
        EnhancedCacheEntry entry = new EnhancedCacheEntry(prefixedKey, data, ttlSeconds, maxIdleSeconds);
        
        evictionLock.readLock().lock();
        try {
            // Check if we need to evict entries to make room
            while (currentSize.get() >= maxSize) {
                // Upgrade to write lock for eviction
                evictionLock.readLock().unlock();
                evictionLock.writeLock().lock();
                try {
                    if (currentSize.get() >= maxSize) {
                        evictLruEntry();
                    }
                } finally {
                    evictionLock.writeLock().unlock();
                    evictionLock.readLock().lock();
                }
            }
            
            // Add the new entry
            EnhancedCacheEntry previous = cache.put(prefixedKey, entry);
            if (previous == null) {
                currentSize.incrementAndGet();
            }
            
            statistics.recordPut();
            LOGGER.debug("Cached entry with key: {} (TTL: {}s, maxIdle: {}s)", prefixedKey, ttlSeconds, maxIdleSeconds);
            
        } finally {
            evictionLock.readLock().unlock();
        }
    }
    
    /**
     * Get an entry from the cache.
     * 
     * @param key The cache key (without prefix)
     * @return The cached data, or null if not found or expired
     */
    public Object get(String key) {
        if (!enabled || key == null) {
            return null;
        }
        
        String prefixedKey = buildPrefixedKey(key);
        EnhancedCacheEntry entry = cache.get(prefixedKey);
        
        if (entry == null) {
            statistics.recordMiss();
            return null;
        }
        
        // Check if expired
        if (entry.isExpired()) {
            // Remove expired entry
            if (cache.remove(prefixedKey, entry)) {
                currentSize.decrementAndGet();
                statistics.recordEviction();
            }
            statistics.recordMiss();
            return null;
        }
        
        statistics.recordHit();
        return entry.getData(); // This updates access tracking
    }
    
    /**
     * Remove an entry from the cache.
     * 
     * @param key The cache key (without prefix)
     * @return The removed data, or null if not found
     */
    public Object remove(String key) {
        if (!enabled || key == null) {
            return null;
        }
        
        String prefixedKey = buildPrefixedKey(key);
        EnhancedCacheEntry removed = cache.remove(prefixedKey);
        
        if (removed != null) {
            currentSize.decrementAndGet();
            statistics.recordRemoval();
            return removed.getData();
        }
        
        return null;
    }
    
    /**
     * Clear all entries from the cache.
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        currentSize.set(0);
        // Record as multiple removals
        for (int i = 0; i < size; i++) {
            statistics.recordRemoval();
        }
        LOGGER.info("Cache cleared: {} entries removed", size);
    }
    
    /**
     * Get the current cache size.
     * 
     * @return Number of entries in the cache
     */
    public int size() {
        return currentSize.get();
    }
    
    /**
     * Check if the cache is empty.
     * 
     * @return true if the cache is empty
     */
    public boolean isEmpty() {
        return currentSize.get() == 0;
    }
    
    /**
     * Get cache statistics.
     * 
     * @return Cache statistics
     */
    public CacheStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Generate a cache key with parameters.
     * 
     * @param dataType The data type
     * @param parameters The parameters
     * @return Generated cache key (without prefix)
     */
    public String generateCacheKey(String dataType, Object... parameters) {
        StringBuilder key = new StringBuilder(dataType);
        for (Object param : parameters) {
            key.append(":").append(param != null ? param.toString() : "null");
        }
        return key.toString();
    }
    
    /**
     * Clean up expired entries.
     * 
     * @return Number of entries removed
     */
    public int cleanupExpiredEntries() {
        int removedCount = 0;
        
        evictionLock.writeLock().lock();
        try {
            Iterator<Map.Entry<String, EnhancedCacheEntry>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, EnhancedCacheEntry> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    currentSize.decrementAndGet();
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                // Record as evictions
                for (int i = 0; i < removedCount; i++) {
                    statistics.recordEviction();
                }
                LOGGER.debug("Cleaned up {} expired cache entries", removedCount);
            }
            
        } finally {
            evictionLock.writeLock().unlock();
        }
        
        return removedCount;
    }
    
    /**
     * Build a prefixed cache key.
     * 
     * @param key The original key
     * @return Prefixed key
     */
    private String buildPrefixedKey(String key) {
        if (keyPrefix == null || keyPrefix.isEmpty()) {
            return key;
        }
        return keyPrefix + ":" + key;
    }
    
    /**
     * Evict the least recently used entry.
     */
    private void evictLruEntry() {
        if (cache.isEmpty()) {
            return;
        }
        
        // Find the LRU entry
        EnhancedCacheEntry lruEntry = null;
        String lruKey = null;
        long oldestAccessTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, EnhancedCacheEntry> entry : cache.entrySet()) {
            EnhancedCacheEntry cacheEntry = entry.getValue();
            if (cacheEntry.getLastAccessTime() < oldestAccessTime) {
                oldestAccessTime = cacheEntry.getLastAccessTime();
                lruEntry = cacheEntry;
                lruKey = entry.getKey();
            }
        }
        
        // Remove the LRU entry
        if (lruKey != null && cache.remove(lruKey, lruEntry)) {
            currentSize.decrementAndGet();
            statistics.recordEviction();
            LOGGER.debug("Evicted LRU cache entry: {} (last accessed: {}ms ago)", 
                lruKey, System.currentTimeMillis() - oldestAccessTime);
        }
    }
}
