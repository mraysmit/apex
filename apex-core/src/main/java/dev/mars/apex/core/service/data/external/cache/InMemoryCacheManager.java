package dev.mars.apex.core.service.data.external.cache;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
 * @author SpEL Rules Engine Team
 * @since 1.0.0
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
        LOGGER.info("In-memory cache manager initialized with maxSize={}, defaultTTL={}s", 
            maxSize, defaultTtlSeconds);
    }
    
    @Override
    public void put(String key, Object value) {
        put(key, value, defaultTtlSeconds);
    }
    
    @Override
    public void put(String key, Object value, long ttlSeconds) {
        if (key == null) {
            return;
        }
        
        long startTime = System.nanoTime();
        
        try {
            // Check if we need to evict entries to make room
            if (cache.size() >= maxSize) {
                evictLRU();
            }
            
            long expiryTime = ttlSeconds > 0 ? 
                System.currentTimeMillis() + (ttlSeconds * 1000) : 
                Long.MAX_VALUE;
            
            CacheEntry entry = new CacheEntry(value, expiryTime, System.currentTimeMillis());
            cache.put(key, entry);
            
            statistics.recordPut();
            statistics.recordLoadTime(System.nanoTime() - startTime);
            
        } catch (Exception e) {
            LOGGER.error("Failed to put value in cache for key: {}", key, e);
        }
    }
    
    @Override
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        
        long startTime = System.nanoTime();
        
        try {
            CacheEntry entry = cache.get(key);
            
            if (entry == null) {
                statistics.recordMiss();
                return null;
            }
            
            // Check if expired
            if (entry.isExpired()) {
                cache.remove(key);
                statistics.recordMiss();
                statistics.recordEviction();
                return null;
            }
            
            // Update access time for LRU
            entry.updateAccessTime();
            
            statistics.recordHit();
            statistics.recordLoadTime(System.nanoTime() - startTime);
            
            return entry.getValue();
            
        } catch (Exception e) {
            LOGGER.error("Failed to get value from cache for key: {}", key, e);
            statistics.recordMiss();
            return null;
        }
    }
    
    @Override
    public boolean remove(String key) {
        if (key == null) {
            return false;
        }
        
        try {
            CacheEntry removed = cache.remove(key);
            if (removed != null) {
                statistics.recordRemoval();
                return true;
            }
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to remove value from cache for key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return false;
            }
            
            // Check if expired
            if (entry.isExpired()) {
                cache.remove(key);
                statistics.recordEviction();
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to check key existence in cache: {}", key, e);
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
            
            return cache.keySet().stream()
                .filter(key -> compiledPattern.matcher(key).matches())
                .filter(key -> {
                    CacheEntry entry = cache.get(key);
                    return entry != null && !entry.isExpired();
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.error("Failed to get keys by pattern: {}", pattern, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<String> getAllKeys() {
        try {
            return cache.keySet().stream()
                .filter(key -> {
                    CacheEntry entry = cache.get(key);
                    return entry != null && !entry.isExpired();
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.error("Failed to get all keys from cache", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public int size() {
        return cache.size();
    }
    
    @Override
    public void clear() {
        try {
            cache.clear();
            LOGGER.info("Cache cleared for '{}'", configuration.getName());
            
        } catch (Exception e) {
            LOGGER.error("Failed to clear cache", e);
        }
    }
    
    @Override
    public void evictExpired() {
        try {
            long currentTime = System.currentTimeMillis();
            int evictedCount = 0;
            
            Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                if (entry.getValue().isExpired(currentTime)) {
                    iterator.remove();
                    evictedCount++;
                    statistics.recordEviction();
                }
            }
            
            if (evictedCount > 0) {
                LOGGER.debug("Evicted {} expired entries from cache '{}'", 
                    evictedCount, configuration.getName());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to evict expired entries", e);
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
        
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        cache.clear();
        LOGGER.info("In-memory cache manager shut down for '{}'", configuration.getName());
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

        LOGGER.debug("Started background cleanup for cache '{}'", configuration.getName());
    }

    /**
     * Evict least recently used entries to make room for new entries.
     */
    private void evictLRU() {
        if (cache.size() < maxSize) {
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
                cache.remove(lruKey);
                statistics.recordEviction();
                LOGGER.debug("Evicted LRU entry with key: {}", lruKey);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to evict LRU entry", e);
        }
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
