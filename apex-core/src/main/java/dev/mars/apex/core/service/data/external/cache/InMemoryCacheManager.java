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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In-memory cache manager implementation.
 * 
 * This class provides a thread-safe in-memory cache with support for TTL,
 * eviction policies, and pattern-based key matching.
 * 
 * Features:
 * - Thread-safe operations using ConcurrentHashMap
 * - TTL support with background cleanup
 * - LRU eviction policy
 * - Pattern-based key matching
 * - Cache statistics tracking
 * - Configurable maximum size
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 1.0
 */
public class InMemoryCacheManager implements CacheManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryCacheManager.class);
    
    private final DataSourceConfiguration configuration;
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final CacheStatistics statistics;

    // Background cleanup
    private ScheduledExecutorService cleanupExecutor;
    private volatile boolean running = false;

    // Thread safety for eviction operations
    private final ReadWriteLock evictionLock = new ReentrantReadWriteLock();
    private final AtomicInteger currentSize = new AtomicInteger(0);

    // Configuration
    private final int maxSize;
    private final long defaultTtlSeconds;
    private final boolean enableCleanup;
    
    /**
     * Constructor with configuration.
     * 
     * @param configuration The data source configuration
     */
    public InMemoryCacheManager(DataSourceConfiguration configuration) {
        this.configuration = configuration;
        this.cache = new ConcurrentHashMap<>();
        this.statistics = new CacheStatistics();
        
        // Extract configuration
        this.maxSize = configuration.getCache() != null && configuration.getCache().getMaxSize() != null ?
            configuration.getCache().getMaxSize() : 10000;
        this.defaultTtlSeconds = configuration.getCache() != null && configuration.getCache().getTtlSeconds() != null ?
            configuration.getCache().getTtlSeconds() : 3600; // 1 hour default
        this.enableCleanup = configuration.getCache() != null && configuration.getCache().isEnabled();
        
        // Start background cleanup if enabled
        if (enableCleanup) {
            startBackgroundCleanup();
        }
        
        this.running = true;
        LOGGER.info("Initialized in-memory cache '{}': maxSize={}, defaultTtl={}s, cleanupEnabled={}",
            cacheName(), maxSize, defaultTtlSeconds, enableCleanup);
    }
    
    @Override
    public void put(String key, Object value) {
        put(key, value, defaultTtlSeconds);
    }
    
    @Override
    public void put(String key, Object value, long ttlSeconds) {
        if (key == null) {
            LOGGER.debug("Ignoring cache put for '{}' because key is null", cacheName());
            return;
        }

        long startTime = System.nanoTime();

        try {
            // Use read lock for normal operations, upgrade to write lock for eviction
            evictionLock.readLock().lock();
            try {
                // Check if we need to evict entries to make room
                while (currentSize.get() >= maxSize) {
                    // Upgrade to write lock for eviction
                    evictionLock.readLock().unlock();
                    evictionLock.writeLock().lock();
                    try {
                        // Double-check condition after acquiring write lock
                        if (currentSize.get() >= maxSize) {
                            evictLRUInternal();
                        }
                        // Downgrade to read lock
                        evictionLock.readLock().lock();
                    } finally {
                        evictionLock.writeLock().unlock();
                    }
                }

                long expiryTime = ttlSeconds > 0 ?
                    System.currentTimeMillis() + (ttlSeconds * 1000) :
                    Long.MAX_VALUE;

                CacheEntry entry = new CacheEntry(value, expiryTime, System.currentTimeMillis());
                CacheEntry previous = cache.put(key, entry);

                // Update size counter atomically
                if (previous == null) {
                    currentSize.incrementAndGet();
                }

                statistics.recordPut();
                statistics.recordLoadTime(System.nanoTime() - startTime);
                LOGGER.debug("Stored cache entry in '{}': key='{}', ttl={}s, replacedExisting={}, size={}",
                    cacheName(), key, ttlSeconds, previous != null, currentSize.get());

            } finally {
                evictionLock.readLock().unlock();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to put value in cache '{}': key='{}', ttl={}s, size={}, error={}",
                cacheName(), key, ttlSeconds, currentSize.get(), e.getMessage());
            LOGGER.debug("Put failure stack trace for cache '{}' and key '{}':", cacheName(), key, e);
        }
    }
    
    @Override
    public Object get(String key) {
        if (key == null) {
            LOGGER.debug("Ignoring cache get for '{}' because key is null", cacheName());
            return null;
        }
        
        long startTime = System.nanoTime();
        
        try {
            CacheEntry entry = cache.get(key);
            
            if (entry == null) {
                statistics.recordMiss();
                LOGGER.debug("Cache miss in '{}': key='{}', size={}", cacheName(), key, currentSize.get());
                return null;
            }
            
            // Check if expired
            if (entry.isExpired()) {
                // Only decrement if we actually removed the entry
                if (cache.remove(key, entry)) {
                    currentSize.decrementAndGet();
                }
                statistics.recordMiss();
                statistics.recordEviction();
                LOGGER.debug("Expired cache entry removed from '{}': key='{}', size={}",
                    cacheName(), key, currentSize.get());
                return null;
            }
            
            // Update access time for LRU
            entry.updateAccessTime();
            
            statistics.recordHit();
            statistics.recordLoadTime(System.nanoTime() - startTime);
            LOGGER.debug("Cache hit in '{}': key='{}', size={}", cacheName(), key, currentSize.get());
            
            return entry.getValue();
            
        } catch (Exception e) {
            LOGGER.error("Failed to get value from cache '{}': key='{}', size={}, error={}",
                cacheName(), key, currentSize.get(), e.getMessage());
            LOGGER.debug("Get failure stack trace for cache '{}' and key '{}':", cacheName(), key, e);
            statistics.recordMiss();
            return null;
        }
    }
    
    @Override
    public boolean remove(String key) {
        if (key == null) {
            LOGGER.debug("Ignoring cache remove for '{}' because key is null", cacheName());
            return false;
        }

        try {
            CacheEntry removed = cache.remove(key);
            if (removed != null) {
                currentSize.decrementAndGet();
                statistics.recordRemoval();
                LOGGER.debug("Removed cache entry from '{}': key='{}', size={}",
                    cacheName(), key, currentSize.get());
                return true;
            }
            LOGGER.debug("Cache remove found no entry in '{}': key='{}', size={}",
                cacheName(), key, currentSize.get());
            return false;

        } catch (Exception e) {
            LOGGER.error("Failed to remove value from cache '{}': key='{}', size={}, error={}",
                cacheName(), key, currentSize.get(), e.getMessage());
            LOGGER.debug("Remove failure stack trace for cache '{}' and key '{}':", cacheName(), key, e);
            return false;
        }
    }
    
    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            LOGGER.debug("Ignoring containsKey for '{}' because key is null", cacheName());
            return false;
        }
        
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                LOGGER.debug("containsKey miss in '{}': key='{}', size={}", cacheName(), key, currentSize.get());
                return false;
            }
            
            // Check if expired
            if (entry.isExpired()) {
                // Only decrement if we actually removed the entry
                if (cache.remove(key, entry)) {
                    currentSize.decrementAndGet();
                }
                statistics.recordEviction();
                LOGGER.debug("containsKey removed expired entry from '{}': key='{}', size={}",
                    cacheName(), key, currentSize.get());
                return false;
            }

            LOGGER.debug("containsKey hit in '{}': key='{}', size={}", cacheName(), key, currentSize.get());
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to check key existence in cache '{}': key='{}', size={}, error={}",
                cacheName(), key, currentSize.get(), e.getMessage());
            LOGGER.debug("containsKey failure stack trace for cache '{}' and key '{}':", cacheName(), key, e);
            return false;
        }
    }
    
    @Override
    public List<String> getKeysByPattern(String pattern) {
        if (pattern == null) {
            return Collections.emptyList();
        }
        
        try {
            // Convert wildcard pattern to regex
            String regexPattern = pattern
                .replace("*", ".*")
                .replace("?", ".");
            
            Pattern compiledPattern = Pattern.compile(regexPattern);
            
            List<String> matchingKeys = cache.keySet().stream()
                .filter(key -> compiledPattern.matcher(key).matches())
                .filter(key -> {
                    CacheEntry entry = cache.get(key);
                    return entry != null && !entry.isExpired();
                })
                .collect(Collectors.toList());

            LOGGER.debug("Pattern lookup in '{}': pattern='{}', matches={}, size={}",
                cacheName(), pattern, matchingKeys.size(), currentSize.get());
            return matchingKeys;
                
        } catch (Exception e) {
            LOGGER.error("Failed to get keys by pattern in cache '{}': pattern='{}', size={}, error={}",
                cacheName(), pattern, currentSize.get(), e.getMessage());
            LOGGER.debug("Pattern lookup failure stack trace for cache '{}' and pattern '{}':", cacheName(), pattern, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<String> getAllKeys() {
        try {
            List<String> keys = cache.keySet().stream()
                .filter(key -> {
                    CacheEntry entry = cache.get(key);
                    return entry != null && !entry.isExpired();
                })
                .collect(Collectors.toList());

            LOGGER.debug("Collected all live keys from '{}': count={}, size={}",
                cacheName(), keys.size(), currentSize.get());
            return keys;
                
        } catch (Exception e) {
            LOGGER.error("Failed to get all keys from cache '{}': size={}, error={}",
                cacheName(), currentSize.get(), e.getMessage());
            LOGGER.debug("Key retrieval failure stack trace for cache '{}':", cacheName(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public int size() {
        return currentSize.get();
    }
    
    @Override
    public void clear() {
        evictionLock.writeLock().lock();
        try {
            int previousSize = currentSize.get();
            cache.clear();
            currentSize.set(0);
            LOGGER.info("Cleared cache '{}': removedEntries={}", cacheName(), previousSize);

        } catch (Exception e) {
            LOGGER.error("Failed to clear cache '{}': size={}, error={}",
                cacheName(), currentSize.get(), e.getMessage());
            LOGGER.debug("Clear failure stack trace for cache '{}':", cacheName(), e);
        } finally {
            evictionLock.writeLock().unlock();
        }
    }
    
    @Override
    public void evictExpired() {
        evictionLock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            int evictedCount = 0;

            for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
                if (entry.getValue().isExpired(currentTime) && cache.remove(entry.getKey(), entry.getValue())) {
                    currentSize.decrementAndGet();
                    evictedCount++;
                    statistics.recordEviction();
                }
            }

            if (evictedCount > 0) {
                LOGGER.info("Evicted expired entries from cache '{}': evictedCount={}, size={}",
                    cacheName(), evictedCount, currentSize.get());
            } else {
                LOGGER.debug("Expired-entry eviction completed for '{}': evictedCount=0, size={}",
                    cacheName(), currentSize.get());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to evict expired entries from cache '{}': size={}, error={}",
                cacheName(), currentSize.get(), e.getMessage());
            LOGGER.debug("Expired-entry eviction failure stack trace for cache '{}':", cacheName(), e);
        } finally {
            evictionLock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean isHealthy() {
        return running && cache != null;
    }
    
    @Override
    public CacheStatistics getStatistics() {
        return statistics.snapshot();
    }
    
    @Override
    public void shutdown() {
        running = false;
        LOGGER.info("Shutting down cache '{}': size={}, cleanupEnabled={}",
            cacheName(), currentSize.get(), enableCleanup);
        
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.info("Cleanup executor for cache '{}' did not stop gracefully; forcing shutdown", cacheName());
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while shutting down cleanup executor for cache '{}': {}",
                    cacheName(), e.getMessage());
                LOGGER.debug("Cleanup executor shutdown interruption for cache '{}':", cacheName(), e);
            }
        }
        
        evictionLock.writeLock().lock();
        try {
            cache.clear();
            currentSize.set(0);
        } finally {
            evictionLock.writeLock().unlock();
        }
        LOGGER.info("Cache '{}' shut down: size={}", cacheName(), currentSize.get());
    }

    /**
     * Start background cleanup task.
     */
    private void startBackgroundCleanup() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "CacheCleanup-" + configuration.getName());
            thread.setDaemon(true);
            return thread;
        });

        // Run cleanup every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(
            this::evictExpired,
            5, // Initial delay
            5, // Period
            TimeUnit.MINUTES
        );

        LOGGER.info("Started background cleanup for cache '{}': initialDelay={}m, interval={}m",
            cacheName(), 5, 5);
    }

    /**
     * Evict least recently used entries to make room for new entries.
     * This method should only be called when holding the write lock.
     */
    private void evictLRUInternal() {
        if (currentSize.get() < maxSize) {
            return;
        }

        try {
            // Find the least recently used entry
            String lruKey = null;
            long oldestAccessTime = Long.MAX_VALUE;

            for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
                long accessTime = entry.getValue().getLastAccessTime();
                if (accessTime < oldestAccessTime) {
                    oldestAccessTime = accessTime;
                    lruKey = entry.getKey();
                }
            }

            if (lruKey != null) {
                CacheEntry removed = cache.remove(lruKey);
                if (removed != null) {
                    currentSize.decrementAndGet();
                    statistics.recordEviction();
                    LOGGER.debug("Evicted LRU entry from '{}': key='{}', size={}",
                        cacheName(), lruKey, currentSize.get());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to evict LRU entry from cache '{}': size={}, error={}",
                cacheName(), currentSize.get(), e.getMessage());
            LOGGER.debug("LRU eviction failure stack trace for cache '{}':", cacheName(), e);
        }
    }

    private String cacheName() {
        return configuration != null && configuration.getName() != null
            ? configuration.getName()
            : "unnamed-cache";
    }

    /**
     * Cache entry holder with TTL and access time tracking.
     */
    private static class CacheEntry {
        private final Object value;
        private final long expiryTime;
        private volatile long lastAccessTime;

        public CacheEntry(Object value, long expiryTime, long creationTime) {
            this.value = value;
            this.expiryTime = expiryTime;
            this.lastAccessTime = creationTime;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long currentTime) {
            return expiryTime != Long.MAX_VALUE && currentTime > expiryTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
