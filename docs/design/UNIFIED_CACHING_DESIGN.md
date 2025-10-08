# Unified Caching Architecture for APEX

## Executive Summary

### The Problem Discovered

The `DuplicateInlineDataSourceTest` revealed that **two enrichments using the same inline dataset create TWO separate `DatasetLookupService` instances**, duplicating data in memory:

```
Creating DatasetLookupService 'dataset-currency-name-lookup-inline' with 4 records
Creating DatasetLookupService 'dataset-currency-symbol-lookup-inline' with 4 records
```

**Impact**: 2x memory usage, 2x loading time, no sharing between enrichments.

### Root Cause

Investigation revealed APEX has **THREE fragmented caching mechanisms**:

1. **Production-Ready CacheManager** ✅ - Full-featured (TTL, LRU, statistics, Redis support)
2. **YamlEnrichmentProcessor Ad-hoc Caches** ⚠️ - Limited (no eviction, no max size, no statistics)
3. **LookupServiceRegistry** ❌ - Basic HashMap (not thread-safe, no cache features)

### The Solution

**Unified Caching Architecture**: Use the existing production-ready `CacheManager` infrastructure for ALL caching in APEX via a new `ApexCacheManager` facade.

**Benefits**:
- ✅ Solves dataset duplication (50-90% memory savings)
- ✅ Unified management (single API, consistent behavior)
- ✅ Production features (eviction, max size, statistics, distributed cache)
- ✅ Zero breaking changes (all internal, backward compatible)
- ✅ 11 hours implementation effort

---

## Current State Analysis

### Existing Caching Infrastructure

APEX currently has **THREE separate caching mechanisms**:

#### 1. **CacheManager Infrastructure** (Production-Ready)
**Location**: `apex-core/src/main/java/dev/mars/apex/core/service/data/external/cache/`

**Components**:
- `CacheManager` interface - Standard cache operations
- `InMemoryCacheManager` - Thread-safe in-memory implementation
- `EnhancedCacheManager` - Advanced features (TTL, LRU, statistics)
- `CacheStatistics` - Comprehensive metrics tracking
- `CacheConfig` - Rich configuration (eviction policies, TTL, max size, distributed cache)

**Features**:
- ✅ TTL-based expiration
- ✅ LRU/LFU/FIFO eviction policies
- ✅ Max size limits
- ✅ Thread-safe (ConcurrentHashMap)
- ✅ Statistics tracking (hits, misses, evictions)
- ✅ Pattern-based key matching
- ✅ Distributed cache support (Redis, Hazelcast)
- ✅ Comprehensive testing

**Current Usage**: External data sources (database, REST API, file systems)

#### 2. **YamlEnrichmentProcessor Lookup Cache** (Ad-hoc)
**Location**: `YamlEnrichmentProcessor.java` lines 68-72

```java
private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
private final Map<String, CachedLookupResult> lookupCache = new ConcurrentHashMap<>();
```

**Features**:
- ✅ TTL support (via CachedLookupResult inner class)
- ✅ Thread-safe
- ❌ No eviction policy
- ❌ No max size limit
- ❌ No statistics
- ❌ No configuration

**Current Usage**: Caching lookup results (e.g., "USD" → currency data)

#### 3. **LookupServiceRegistry** (Service Registry, not a cache)
**Location**: `LookupServiceRegistry.java`

```java
private final Map<String, NamedService> services = new HashMap<>();
```

**Features**:
- ❌ Not thread-safe
- ❌ No TTL
- ❌ No eviction
- ❌ No statistics

**Current Usage**: Registering and retrieving lookup services

### The Problem

1. **Fragmentation**: Three different caching approaches with different capabilities
2. **Duplication**: YamlEnrichmentProcessor reimplements caching logic
3. **Inconsistency**: Different cache behaviors across APEX
4. **Missing Features**: Lookup cache lacks eviction, size limits, statistics
5. **Dataset Duplication**: No caching of DatasetLookupService instances

## Unified Caching Solution

### Design Principles

1. **Single Source of Truth**: Use existing `CacheManager` infrastructure for ALL caching
2. **Layered Caching**: Different cache scopes for different use cases
3. **Configuration-Driven**: All caches configured via `CacheConfig`
4. **Statistics-Enabled**: Unified metrics across all caches
5. **Backward Compatible**: Existing code continues to work

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    APEX Unified Caching                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐    │
│  │ ApexCacheManager (NEW - Facade)                       │    │
│  │ - Manages multiple cache scopes                       │    │
│  │ - Provides unified API                                │    │
│  │ - Aggregates statistics                               │    │
│  └───────────────────────────────────────────────────────┘    │
│                          │                                      │
│         ┌────────────────┼────────────────┬──────────────┐    │
│         │                │                │              │    │
│         ▼                ▼                ▼              ▼    │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐   ┌──────────┐│
│  │ Dataset  │    │ Lookup   │    │Expression│   │ Service  ││
│  │ Cache    │    │ Result   │    │ Cache    │   │ Registry ││
│  │          │    │ Cache    │    │          │   │ Cache    ││
│  └──────────┘    └──────────┘    └──────────┘   └──────────┘│
│       │               │                │              │       │
│       └───────────────┴────────────────┴──────────────┘       │
│                          │                                      │
│                          ▼                                      │
│              ┌────────────────────────┐                        │
│              │ CacheManager (Existing)│                        │
│              │ - InMemoryCacheManager │                        │
│              │ - Redis/Hazelcast      │                        │
│              └────────────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### Cache Scopes

#### 1. **Dataset Cache** (NEW - Solves duplicate dataset problem)
**Purpose**: Cache DatasetLookupService instances by content signature

**Key**: `DatasetSignature` (type + contentHash + keyField)
**Value**: `DatasetLookupService` instance
**TTL**: Long (1 hour default) - datasets rarely change
**Max Size**: 1000 datasets
**Eviction**: LRU

**Example**:
```java
// Key: "inline:md5(data):code"
// Value: DatasetLookupService with 4 currency records
```

#### 2. **Lookup Result Cache** (MIGRATE from YamlEnrichmentProcessor)
**Purpose**: Cache lookup results to avoid repeated queries

**Key**: `serviceName:lookupKey` (e.g., "currency-service:USD")
**Value**: Lookup result (Map<String, Object>)
**TTL**: Medium (5 minutes default) - results may change
**Max Size**: 10000 results
**Eviction**: LRU

#### 3. **Expression Cache** (MIGRATE from YamlEnrichmentProcessor)
**Purpose**: Cache compiled SpEL expressions

**Key**: Expression string
**Value**: Compiled Expression object
**TTL**: None (expressions don't change)
**Max Size**: 1000 expressions
**Eviction**: LRU

#### 4. **Service Registry Cache** (ENHANCE LookupServiceRegistry)
**Purpose**: Cache service instances for reuse

**Key**: Service name
**Value**: NamedService instance
**TTL**: None (services don't expire)
**Max Size**: 500 services
**Eviction**: None (or LRU if size exceeded)

## Implementation Plan

### Phase 1: Create ApexCacheManager Facade

**File**: `apex-core/src/main/java/dev/mars/apex/core/service/cache/ApexCacheManager.java` (NEW)

```java
public class ApexCacheManager {
    private final CacheManager datasetCache;
    private final CacheManager lookupResultCache;
    private final CacheManager expressionCache;
    private final CacheManager serviceRegistryCache;
    
    public ApexCacheManager(CacheConfig globalConfig) {
        // Create specialized caches with different configs
        this.datasetCache = createDatasetCache(globalConfig);
        this.lookupResultCache = createLookupResultCache(globalConfig);
        this.expressionCache = createExpressionCache(globalConfig);
        this.serviceRegistryCache = createServiceRegistryCache(globalConfig);
    }
    
    // Dataset cache operations
    public DatasetLookupService getDataset(DatasetSignature signature) {
        return (DatasetLookupService) datasetCache.get(signature.toString());
    }
    
    public void putDataset(DatasetSignature signature, DatasetLookupService service) {
        datasetCache.put(signature.toString(), service);
    }
    
    // Lookup result cache operations
    public Object getLookupResult(String serviceName, Object key) {
        return lookupResultCache.get(serviceName + ":" + key);
    }
    
    public void putLookupResult(String serviceName, Object key, Object result, int ttlSeconds) {
        lookupResultCache.put(serviceName + ":" + key, result, ttlSeconds);
    }
    
    // Expression cache operations
    public Expression getExpression(String expressionString) {
        return (Expression) expressionCache.get(expressionString);
    }
    
    public void putExpression(String expressionString, Expression expression) {
        expressionCache.put(expressionString, expression);
    }
    
    // Service registry cache operations
    public NamedService getService(String name) {
        return (NamedService) serviceRegistryCache.get(name);
    }
    
    public void putService(String name, NamedService service) {
        serviceRegistryCache.put(name, service);
    }
    
    // Unified statistics
    public Map<String, CacheStatistics> getAllStatistics() {
        Map<String, CacheStatistics> stats = new HashMap<>();
        stats.put("dataset", datasetCache.getStatistics());
        stats.put("lookupResult", lookupResultCache.getStatistics());
        stats.put("expression", expressionCache.getStatistics());
        stats.put("serviceRegistry", serviceRegistryCache.getStatistics());
        return stats;
    }
    
    // Clear all caches
    public void clearAll() {
        datasetCache.clear();
        lookupResultCache.clear();
        expressionCache.clear();
        serviceRegistryCache.clear();
    }
}
```

### Phase 2: Migrate YamlEnrichmentProcessor

**Changes to YamlEnrichmentProcessor**:

```java
public class YamlEnrichmentProcessor {
    private final LookupServiceRegistry serviceRegistry;
    private final ExpressionEvaluatorService evaluatorService;
    private final ApexCacheManager cacheManager; // NEW - replaces individual caches
    
    // REMOVE these:
    // private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    // private final Map<String, CachedLookupResult> lookupCache = new ConcurrentHashMap<>();
    
    public YamlEnrichmentProcessor(LookupServiceRegistry serviceRegistry,
                                   ExpressionEvaluatorService evaluatorService,
                                   ApexCacheManager cacheManager) {
        this.serviceRegistry = serviceRegistry;
        this.evaluatorService = evaluatorService;
        this.cacheManager = cacheManager;
    }
    
    // Update resolveLookupService() to use dataset cache
    private LookupService resolveLookupService(String enrichmentId, LookupConfig lookupConfig) {
        if (lookupConfig.getLookupDataset() != null) {
            DatasetSignature signature = DatasetSignature.from(lookupConfig.getLookupDataset());
            
            // Check dataset cache
            DatasetLookupService cached = cacheManager.getDataset(signature);
            if (cached != null) {
                LOGGER.info("Dataset cache HIT: " + signature);
                return cached;
            }
            
            // Create and cache
            DatasetLookupService newService = DatasetLookupServiceFactory
                .createDatasetLookupService("dataset-" + signature.toShortString(), 
                                           lookupConfig.getLookupDataset(), 
                                           currentConfiguration);
            
            cacheManager.putDataset(signature, newService);
            LOGGER.info("Dataset cache MISS: " + signature);
            
            return newService;
        }
        // ... existing code ...
    }
    
    // Update performLookup() to use lookup result cache
    private Object performLookup(LookupService lookupService, Object lookupKey, LookupConfig lookupConfig) {
        // Check cache if enabled
        if (lookupConfig.getCacheEnabled()) {
            Object cached = cacheManager.getLookupResult(lookupService.getName(), lookupKey);
            if (cached != null) {
                return cached;
            }
        }
        
        // Perform lookup
        Object result = lookupService.transform(lookupKey);
        
        // Cache result
        if (lookupConfig.getCacheEnabled()) {
            int ttl = lookupConfig.getCacheTtlSeconds() != null ? lookupConfig.getCacheTtlSeconds() : 300;
            cacheManager.putLookupResult(lookupService.getName(), lookupKey, result, ttl);
        }
        
        return result;
    }
    
    // Update getOrCompileExpression() to use expression cache
    private Expression getOrCompileExpression(String expressionString) {
        Expression cached = cacheManager.getExpression(expressionString);
        if (cached != null) {
            return cached;
        }
        
        Expression compiled = parser.parseExpression(expressionString);
        cacheManager.putExpression(expressionString, compiled);
        return compiled;
    }
}
```

### Phase 3: Enhance LookupServiceRegistry

```java
public class LookupServiceRegistry {
    private final ApexCacheManager cacheManager;
    
    public LookupServiceRegistry(ApexCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    public void registerService(NamedService service) {
        cacheManager.putService(service.getName(), service);
    }
    
    public <T extends NamedService> T getService(String name, Class<T> type) {
        NamedService service = cacheManager.getService(name);
        if (service != null && type.isInstance(service)) {
            return type.cast(service);
        }
        return null;
    }
}
```

## DatasetSignature Class

### Purpose
Generate unique signatures for datasets based on **content**, not enrichment ID, enabling dataset deduplication.

### Implementation

**File**: `apex-core/src/main/java/dev/mars/apex/core/service/lookup/DatasetSignature.java`

```java
package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.config.yaml.YamlEnrichment.LookupDataset;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Generates unique signatures for datasets to enable caching and deduplication.
 */
public class DatasetSignature {
    private final String type;           // inline, file, database, rest-api
    private final String contentHash;    // Hash of dataset content
    private final String keyField;       // Lookup key field

    private DatasetSignature(String type, String contentHash, String keyField) {
        this.type = type;
        this.contentHash = contentHash;
        this.keyField = keyField;
    }

    /**
     * Create signature from dataset configuration.
     */
    public static DatasetSignature from(LookupDataset dataset) {
        String type = dataset.getType();
        String keyField = dataset.getKeyField();
        String contentHash = generateContentHash(dataset);

        return new DatasetSignature(type, contentHash, keyField);
    }

    private static String generateContentHash(LookupDataset dataset) {
        switch (dataset.getType()) {
            case "inline":
                return hashInlineData(dataset.getData());
            case "file":
                return dataset.getPath(); // File path is the signature
            case "database":
                return hashDatabaseConfig(dataset.getDatabase());
            case "rest-api":
                return hashRestApiConfig(dataset.getRestApi());
            default:
                return "unknown";
        }
    }

    private static String hashInlineData(List<Map<String, Object>> data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String dataString = data.toString(); // Simple approach
            byte[] hash = md.digest(dataString.getBytes());
            return bytesToHex(hash).substring(0, 8); // First 8 chars
        } catch (Exception e) {
            return "hash-error";
        }
    }

    public String toShortString() {
        return type + "-" + contentHash;
    }

    @Override
    public String toString() {
        return type + ":" + contentHash + ":" + keyField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetSignature)) return false;
        DatasetSignature that = (DatasetSignature) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(contentHash, that.contentHash) &&
               Objects.equals(keyField, that.keyField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, contentHash, keyField);
    }
}
```

## Configuration

### YAML Configuration Example

```yaml
metadata:
  id: "unified-cache-demo"

# Global cache configuration (optional - defaults provided)
cache:
  enabled: true
  ttl-seconds: 3600
  max-size: 10000
  eviction-policy: LRU
  statistics-enabled: true

  # Scope-specific overrides (optional)
  scopes:
    dataset:
      ttl-seconds: 7200  # 2 hours for datasets
      max-size: 1000
    lookup-result:
      ttl-seconds: 300   # 5 minutes for results
      max-size: 10000
    expression:
      ttl-seconds: -1    # No expiration
      max-size: 1000
```

## Benefits

### 1. **Solves Dataset Duplication**
- ✅ DatasetLookupService instances shared across enrichments
- ✅ 50% memory reduction for 2 duplicate enrichments
- ✅ 90% memory reduction for 10 duplicate enrichments

### 2. **Unified Management**
- ✅ Single API for all caching operations
- ✅ Consistent configuration across APEX
- ✅ Centralized statistics and monitoring

### 3. **Production-Ready Features**
- ✅ LRU/LFU eviction policies prevent memory leaks
- ✅ Max size limits protect against unbounded growth
- ✅ TTL support for all caches
- ✅ Comprehensive statistics (hits, misses, evictions)

### 4. **Scalability**
- ✅ Can swap to Redis/Hazelcast for distributed caching
- ✅ Thread-safe for concurrent access
- ✅ Configurable per environment
- ✅ No code changes needed

### 5. **Maintainability**
- ✅ Less code duplication
- ✅ Easier to test
- ✅ Consistent behavior across APEX

## Expected Results

### Before Fix
```
Creating DatasetLookupService 'dataset-currency-name-lookup-inline' with 4 records
Creating DatasetLookupService 'dataset-currency-symbol-lookup-inline' with 4 records
```

### After Fix
```
Dataset cache MISS - created service: inline-abc123 with 4 records
Dataset cache HIT - reusing service: inline-abc123

APEX Cache Statistics:
  Dataset Cache: hits=1, misses=1, hitRate=50.00%, size=1
  Lookup Result Cache: hits=0, misses=0, hitRate=0.00%, size=0
  Expression Cache: hits=5, misses=3, hitRate=62.50%, size=3
  Service Registry Cache: hits=0, misses=0, hitRate=0.00%, size=0
```

## Migration Path

### ✅ Zero Breaking Changes
All changes are internal to APEX:
- Existing YAML configurations work unchanged
- Existing tests pass without modification
- Existing APIs remain the same
- Backward compatible

### Step 1: Create ApexCacheManager (No Breaking Changes)
- Add new class
- Add tests
- No impact on existing code

### Step 2: Update YamlEnrichmentProcessor (Internal Change)
- Replace internal caches with ApexCacheManager
- Maintain same external API
- Backward compatible

### Step 3: Update LookupServiceRegistry (Internal Change)
- Use ApexCacheManager for service storage
- Maintain same external API
- Backward compatible

### Step 4: Add Configuration Support (Optional)
- Add cache configuration to YAML
- Default values maintain current behavior
- Opt-in for advanced features

## Success Criteria

1. ✅ DuplicateInlineDataSourceTest shows only 1 dataset creation
2. ✅ All caches use CacheManager infrastructure
3. ✅ Unified statistics available via ApexCacheManager
4. ✅ All existing tests pass
5. ✅ Memory usage reduced for duplicate datasets
6. ✅ Cache hit rates visible in logs/metrics
7. ✅ No breaking changes to public APIs
8. ✅ Thread-safe concurrent access
9. ✅ Configurable cache behavior
10. ✅ Comprehensive test coverage

---

## FAQ

**Q: Will this break existing YAML configurations?**
A: No, all changes are internal. Existing YAML works unchanged.

**Q: Do I need to configure caching?**
A: No, sensible defaults are provided. Configuration is optional.

**Q: Can I disable caching?**
A: Yes, each cache scope can be enabled/disabled independently.

**Q: Will this work with distributed caching (Redis)?**
A: Yes, `CacheManager` already supports Redis and Hazelcast.

**Q: What about thread safety?**
A: All caches use `ConcurrentHashMap` and are thread-safe.

**Q: How do I monitor cache performance?**
A: Use `ApexCacheManager.getAllStatistics()` for comprehensive metrics.

