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

import dev.mars.apex.core.config.datasource.CacheConfig;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.cache.CacheManager;
import dev.mars.apex.core.service.data.external.cache.InMemoryCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Classification cache for storing and retrieving classification results.
 * 
 * This cache uses the existing APEX caching infrastructure to store classification
 * results, improving performance for repeated classification of similar content.
 * 
 * CACHING STRATEGY:
 * - Content-based cache keys using SHA-256 hashing
 * - TTL-based expiration for cache freshness
 * - Size-limited cache with LRU eviction
 * - Configurable cache settings
 * 
 * CACHE KEY GENERATION:
 * - Based on content hash for consistency
 * - Includes file name and size for context
 * - Prefixed for namespace isolation
 * - Collision-resistant using SHA-256
 * 
 * DESIGN PRINCIPLES:
 * - Uses existing APEX CacheManager interface
 * - Conservative caching of successful results only
 * - Configurable TTL and size limits
 * - Thread-safe operations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ClassificationCache {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassificationCache.class);
    
    private static final String CACHE_KEY_PREFIX = "apex:classification:";
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes
    private static final int DEFAULT_MAX_SIZE = 1000;
    
    private final CacheManager cacheManager;
    private final boolean enabled;
    private final long ttlSeconds;
    
    /**
     * Create classification cache with default settings.
     */
    public ClassificationCache() {
        this(createDefaultConfiguration());
    }
    
    /**
     * Create classification cache with custom configuration.
     */
    public ClassificationCache(ClassificationCacheConfig config) {
        this.enabled = config.isEnabled();
        this.ttlSeconds = config.getTtlSeconds();
        
        if (enabled) {
            this.cacheManager = createCacheManager(config);
            logger.info("Classification cache initialized: enabled={}, ttl={}s, maxSize={}", 
                       enabled, ttlSeconds, config.getMaxSize());
        } else {
            this.cacheManager = null;
            logger.info("Classification cache disabled");
        }
    }
    
    /**
     * Get cached classification result.
     * 
     * @param context the classification context
     * @return cached result or null if not found/expired
     */
    public ClassificationResult get(ClassificationContext context) {
        if (!enabled || cacheManager == null) {
            return null;
        }
        
        try {
            String cacheKey = generateCacheKey(context);
            Object cached = cacheManager.get(cacheKey);
            
            if (cached instanceof ClassificationResult) {
                ClassificationResult result = (ClassificationResult) cached;
                if (!result.isExpired()) {
                    logger.debug("Cache hit for key: {}", cacheKey);
                    return result;
                } else {
                    logger.debug("Cache entry expired for key: {}", cacheKey);
                    cacheManager.remove(cacheKey);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to get from classification cache: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Put classification result in cache.
     * 
     * @param context the classification context
     * @param result the classification result to cache
     */
    public void put(ClassificationContext context, ClassificationResult result) {
        if (!enabled || cacheManager == null || result == null || !result.isCacheable()) {
            return;
        }
        
        try {
            String cacheKey = generateCacheKey(context);
            cacheManager.put(cacheKey, result, ttlSeconds);
            logger.debug("Cached classification result for key: {}", cacheKey);
            
        } catch (Exception e) {
            logger.warn("Failed to put in classification cache: {}", e.getMessage());
        }
    }
    
    /**
     * Check if cache contains a result for the given context.
     * 
     * @param context the classification context
     * @return true if cache contains non-expired result
     */
    public boolean contains(ClassificationContext context) {
        if (!enabled || cacheManager == null) {
            return false;
        }
        
        try {
            String cacheKey = generateCacheKey(context);
            return cacheManager.containsKey(cacheKey);
        } catch (Exception e) {
            logger.warn("Failed to check classification cache: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear all cached classification results.
     */
    public void clear() {
        if (enabled && cacheManager != null) {
            try {
                cacheManager.clear();
                logger.info("Classification cache cleared");
            } catch (Exception e) {
                logger.warn("Failed to clear classification cache: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Get cache statistics.
     * 
     * @return cache statistics or null if cache disabled
     */
    public Object getStatistics() {
        if (enabled && cacheManager != null) {
            return cacheManager.getStatistics();
        }
        return null;
    }
    
    /**
     * Get current cache size.
     * 
     * @return number of cached entries
     */
    public int size() {
        if (enabled && cacheManager != null) {
            return cacheManager.size();
        }
        return 0;
    }
    
    /**
     * Generate cache key for classification context.
     */
    private String generateCacheKey(ClassificationContext context) {
        StringBuilder keyBuilder = new StringBuilder(CACHE_KEY_PREFIX);
        
        // Add content hash for consistency
        String contentHash = hashContent(context.getInputDataAsString());
        keyBuilder.append(contentHash);
        
        // Add context information for uniqueness
        if (context.getFileName() != null) {
            keyBuilder.append(":").append(context.getFileName());
        }
        
        if (context.getFileSize() != null) {
            keyBuilder.append(":").append(context.getFileSize());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Generate SHA-256 hash of content for cache key.
     */
    private String hashContent(String content) {
        if (content == null || content.isEmpty()) {
            return "empty";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 16); // Use first 16 chars
        } catch (NoSuchAlgorithmException e) {
            logger.warn("SHA-256 not available, using hashCode: {}", e.getMessage());
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * Create cache manager using existing APEX infrastructure.
     */
    private CacheManager createCacheManager(ClassificationCacheConfig config) {
        try {
            // Create data source configuration for cache manager
            DataSourceConfiguration dsConfig = new DataSourceConfiguration();
            dsConfig.setName("classification-cache");
            dsConfig.setSourceType("memory");
            
            // Configure cache settings
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.setEnabled(true);
            cacheConfig.setTtlSeconds(config.getTtlSeconds());
            cacheConfig.setMaxSize(config.getMaxSize());
            cacheConfig.setKeyPrefix(CACHE_KEY_PREFIX);
            dsConfig.setCache(cacheConfig);
            
            return new InMemoryCacheManager(dsConfig);
            
        } catch (Exception e) {
            logger.error("Failed to create classification cache manager", e);
            throw new RuntimeException("Failed to initialize classification cache", e);
        }
    }
    
    /**
     * Create default cache configuration.
     */
    private static ClassificationCacheConfig createDefaultConfiguration() {
        ClassificationCacheConfig config = new ClassificationCacheConfig();
        config.setEnabled(true);
        config.setTtlSeconds(DEFAULT_TTL_SECONDS);
        config.setMaxSize(DEFAULT_MAX_SIZE);
        return config;
    }
    
    /**
     * Configuration for classification cache.
     */
    public static class ClassificationCacheConfig {
        private boolean enabled = true;
        private long ttlSeconds = DEFAULT_TTL_SECONDS;
        private int maxSize = DEFAULT_MAX_SIZE;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public long getTtlSeconds() {
            return ttlSeconds;
        }
        
        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
        
        public int getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
        
        @Override
        public String toString() {
            return "ClassificationCacheConfig{" +
                    "enabled=" + enabled +
                    ", ttlSeconds=" + ttlSeconds +
                    ", maxSize=" + maxSize +
                    '}';
        }
    }
}
