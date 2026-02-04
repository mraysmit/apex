# Unified DataSource Registry Design

## Current State (Problematic)

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                          CURRENT: Multiple Caching Layers                       │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  RulesEngine                                                                    │
│  └── dataSources: ConcurrentHashMap<String, ExternalDataSource>                │
│      └── Per-instance cache of ExternalDataSource wrappers                     │
│      └── Passed to DatasetLookupServiceFactory as "dataSourceRegistry"         │
│                                                                                 │
│  DatasetLookupServiceFactory                                                    │
│  └── Uses dataSourceRegistry parameter (if provided)                           │
│  └── Falls back to creating new data sources via DataSourceFactory             │
│  └── NOW: Registers newly created sources back into registry                   │
│                                                                                 │
│  DataSourceFactory (Singleton)                                                  │
│  └── jdbcDataSourceCache: Map<String, javax.sql.DataSource>  ← HikariCP pools  │
│  └── httpClientCache: Map<String, HttpClient>                                  │
│  └── pendingCreations: Map<String, CompletableFuture<ExternalDataSource>>      │
│  └── customProviders: Map<String, DataSourceProvider>                          │
│                                                                                 │
│  DataSourceRegistry (Singleton) ← ALREADY EXISTS BUT UNDERUTILIZED!            │
│  └── dataSources: Map<String, DataSourceRegistration>                          │
│  └── typeIndex, tagIndex, listeners, health monitoring                         │
│  └── Full-featured but not integrated with the other components                │
│                                                                                 │
├────────────────────────────────────────────────────────────────────────────────┤
│  PROBLEMS:                                                                      │
│  1. Four different caching mechanisms for related concerns                      │
│  2. Registry parameter passing is fragile (easy to forget)                      │
│  3. DataSourceRegistry exists but isn't used by RulesEngine/Factory            │
│  4. No single source of truth for "does this data source exist?"               │
│  5. Lifecycle management scattered across classes                               │
└────────────────────────────────────────────────────────────────────────────────┘
```

## Proposed Design: Unified DataSourceRegistry

### Key Insight
You already have `DataSourceRegistry` with health monitoring, type indexing, and event listeners. 
The fix is to **USE IT** as the single source of truth, not create yet another cache.

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                     PROPOSED: Single Source of Truth                            │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  DataSourceRegistry (Enhanced Singleton)                                        │
│  ├── getOrCreate(name, config) → ExternalDataSource   ← NEW: Primary API       │
│  │   └── Checks if registered, creates if not                                  │
│  │   └── Automatically registers on creation                                   │
│  │   └── Thread-safe, deduplicated                                             │
│  │                                                                             │
│  ├── Internal Caches (encapsulated)                                            │
│  │   ├── jdbcPoolCache: Map<String, javax.sql.DataSource>  ← HikariCP         │
│  │   └── httpClientCache: Map<String, HttpClient>                              │
│  │                                                                             │
│  ├── Existing Features (retained)                                               │
│  │   ├── Health monitoring                                                     │
│  │   ├── Type/tag indexing                                                     │
│  │   ├── Event listeners                                                       │
│  │   └── Statistics                                                            │
│  │                                                                             │
│  └── Lifecycle                                                                  │
│      ├── shutdown(name) → Close one data source                                │
│      └── shutdownAll() → Close all, clear pools                                │
│                                                                                 │
│  RulesEngine                                                                    │
│  └── Uses: DataSourceRegistry.getInstance().getOrCreate(name, config)          │
│  └── NO local dataSources map needed                                           │
│  └── shutdown() calls: DataSourceRegistry.getInstance().shutdown(...)          │
│                                                                                 │
│  DatasetLookupServiceFactory                                                    │
│  └── Uses: DataSourceRegistry.getInstance().getOrCreate(name, config)          │
│  └── NO dataSourceRegistry parameter needed                                    │
│                                                                                 │
│  DataSourceFactory                                                              │
│  └── INTERNAL USE ONLY by DataSourceRegistry                                   │
│  └── No public caches - just creates instances                                 │
│                                                                                 │
└────────────────────────────────────────────────────────────────────────────────┘
```

## Implementation Sketch

### 1. Enhanced DataSourceRegistry

```java
package dev.mars.apex.core.service.data.external.registry;

/**
 * Unified registry for all data sources in APEX.
 * 
 * This is THE single source of truth for data source management:
 * - Creation (with automatic caching and deduplication)
 * - Lookup
 * - Lifecycle management
 * - Health monitoring
 * 
 * All components (RulesEngine, DatasetLookupServiceFactory, etc.) should
 * use this registry instead of maintaining their own caches.
 */
public class DataSourceRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRegistry.class);
    
    // Singleton
    private static volatile DataSourceRegistry instance;
    private static final Object LOCK = new Object();
    
    // Primary registry: name → ExternalDataSource
    private final ConcurrentHashMap<String, DataSourceRegistration> dataSources = new ConcurrentHashMap<>();
    
    // Connection pool caches (internal implementation detail)
    private final ConcurrentHashMap<String, javax.sql.DataSource> jdbcPoolCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HttpClient> httpClientCache = new ConcurrentHashMap<>();
    
    // Deduplication for concurrent creation requests
    private final ConcurrentHashMap<String, CompletableFuture<ExternalDataSource>> pendingCreations = new ConcurrentHashMap<>();
    
    // Existing features (retain)
    private final Map<DataSourceType, Set<String>> typeIndex = new ConcurrentHashMap<>();
    private final List<DataSourceRegistryListener> listeners = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService healthMonitor;
    
    // Factory for actual creation (delegate)
    private final DataSourceFactory factory = DataSourceFactory.getInstance();
    
    /**
     * PRIMARY API: Get existing data source or create new one.
     * 
     * Thread-safe, deduplicated, automatically registered.
     * 
     * @param name Unique name for the data source (e.g., "postgres-database")
     * @param config Configuration for creation (only used if not already registered)
     * @return The data source (cached or newly created)
     */
    public ExternalDataSource getOrCreate(String name, DataSourceConfiguration config) {
        Objects.requireNonNull(name, "Data source name cannot be null");
        
        // Fast path: already registered
        DataSourceRegistration existing = dataSources.get(name);
        if (existing != null) {
            LOGGER.debug("Returning cached data source: {}", name);
            return existing.getDataSource();
        }
        
        // Slow path: need to create (with deduplication)
        return createWithDeduplication(name, config);
    }
    
    /**
     * Get data source by name (lookup only, no creation).
     */
    public Optional<ExternalDataSource> get(String name) {
        DataSourceRegistration reg = dataSources.get(name);
        return Optional.ofNullable(reg).map(DataSourceRegistration::getDataSource);
    }
    
    /**
     * Check if a data source is registered.
     */
    public boolean contains(String name) {
        return dataSources.containsKey(name);
    }
    
    private ExternalDataSource createWithDeduplication(String name, DataSourceConfiguration config) {
        // Use computeIfAbsent to ensure only one creation per name
        CompletableFuture<ExternalDataSource> future = pendingCreations.computeIfAbsent(name,
            k -> CompletableFuture.supplyAsync(() -> {
                try {
                    return createAndRegister(name, config);
                } finally {
                    pendingCreations.remove(k);
                }
            }));
        
        try {
            return future.get();
        } catch (Exception e) {
            throw new DataSourceException(ErrorType.CONFIGURATION_ERROR,
                "Failed to create data source: " + name, e);
        }
    }
    
    private ExternalDataSource createAndRegister(String name, DataSourceConfiguration config) {
        LOGGER.info("Creating new data source: {} (type: {})", name, config.getDataSourceType());
        
        // Create the data source (using shared pools where applicable)
        ExternalDataSource dataSource = createDataSourceWithSharedPools(config);
        
        // Register it
        DataSourceRegistration registration = new DataSourceRegistration(dataSource);
        dataSources.put(name, registration);
        
        // Update indexes
        updateTypeIndex(name, dataSource.getSourceType());
        
        // Notify listeners
        notifyListeners(DataSourceRegistryEvent.registered(name, dataSource));
        
        LOGGER.info("Registered new data source: {}", name);
        return dataSource;
    }
    
    private ExternalDataSource createDataSourceWithSharedPools(DataSourceConfiguration config) {
        DataSourceType type = config.getDataSourceType();
        
        switch (type) {
            case DATABASE:
                return createDatabaseDataSourceWithSharedPool(config);
            case REST_API:
                return createRestApiDataSourceWithSharedClient(config);
            default:
                // Other types don't need shared resources
                return factory.createDataSource(config);
        }
    }
    
    private ExternalDataSource createDatabaseDataSourceWithSharedPool(DataSourceConfiguration config) {
        // Cache key based on connection details (not name)
        String poolKey = buildJdbcPoolKey(config);
        
        // Get or create the HikariCP pool
        javax.sql.DataSource pool = jdbcPoolCache.computeIfAbsent(poolKey, k -> {
            LOGGER.info("Creating new JDBC connection pool for: {}", poolKey);
            return JdbcTemplateFactory.createDataSource(config);
        });
        
        // Create wrapper using shared pool
        return new DatabaseDataSource(pool, config);
    }
    
    private ExternalDataSource createRestApiDataSourceWithSharedClient(DataSourceConfiguration config) {
        String clientKey = buildHttpClientKey(config);
        
        HttpClient client = httpClientCache.computeIfAbsent(clientKey, k -> {
            LOGGER.info("Creating new HTTP client for: {}", clientKey);
            return RestTemplateFactory.createHttpClient(config);
        });
        
        return new RestApiDataSource(client, config);
    }
    
    private String buildJdbcPoolKey(DataSourceConfiguration config) {
        // Pool key is based on connection details, not the data source name
        // This allows multiple named data sources to share the same pool
        return config.getConnection().getJdbcUrl() + "|" + config.getConnection().getUsername();
    }
    
    /**
     * Shutdown a specific data source.
     */
    public boolean shutdown(String name) {
        DataSourceRegistration reg = dataSources.remove(name);
        if (reg == null) return false;
        
        try {
            reg.getDataSource().shutdown();
            LOGGER.info("Shut down data source: {}", name);
            notifyListeners(DataSourceRegistryEvent.unregistered(name, reg.getDataSource()));
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error shutting down data source {}: {}", name, e.getMessage());
            return false;
        }
    }
    
    /**
     * Shutdown all data sources and clear pools.
     */
    public void shutdownAll() {
        LOGGER.info("Shutting down all data sources...");
        
        // Shutdown all registered data sources
        for (String name : new ArrayList<>(dataSources.keySet())) {
            shutdown(name);
        }
        
        // Close JDBC pools
        for (javax.sql.DataSource pool : jdbcPoolCache.values()) {
            if (pool instanceof HikariDataSource) {
                ((HikariDataSource) pool).close();
            }
        }
        jdbcPoolCache.clear();
        
        // Clear HTTP clients
        httpClientCache.clear();
        
        LOGGER.info("All data sources shut down");
    }
    
    /**
     * Reset for testing purposes.
     */
    public void reset() {
        shutdownAll();
        typeIndex.clear();
        listeners.clear();
    }
    
    // ... retain existing methods: getDataSourcesByType, getHealthyDataSources, etc.
}
```

### 2. Simplified RulesEngine

```java
public class RulesEngine {
    
    // REMOVE: private final Map<String, ExternalDataSource> dataSources;
    
    // No longer need to maintain local cache or pass registry around
    
    private ExternalDataSource getDataSource(String name, DataSourceConfiguration config) {
        // Single line - registry handles everything
        return DataSourceRegistry.getInstance().getOrCreate(name, config);
    }
    
    public void shutdown() {
        // ... other cleanup ...
        
        // Option A: Shutdown data sources we created (if tracking names)
        // for (String name : myDataSourceNames) {
        //     DataSourceRegistry.getInstance().shutdown(name);
        // }
        
        // Option B: Let registry manage lifecycle globally
        // (appropriate if one registry instance per application)
    }
}
```

### 3. Simplified DatasetLookupServiceFactory

```java
public class DatasetLookupServiceFactory {
    
    // REMOVE: dataSourceRegistry parameter from all methods
    
    public static DatasetLookupService createDatasetLookupService(
            String serviceName,
            YamlEnrichment.LookupDataset dataset,
            YamlRuleConfiguration configuration) {
        
        // ... existing logic ...
        
        case "database":
            return createDatabaseDatasetService(serviceName, dataset, configuration);
            // NO dataSourceRegistry parameter needed
    }
    
    private static DatasetLookupService createDatabaseDatasetService(
            String serviceName,
            YamlEnrichment.LookupDataset dataset,
            YamlRuleConfiguration configuration) {
        
        String connectionName = dataset.getDataSourceRef();
        DataSourceConfiguration config = resolveConfig(connectionName, configuration);
        
        // Single source of truth - no parameter passing
        ExternalDataSource dataSource = DataSourceRegistry.getInstance()
            .getOrCreate(connectionName, config);
        
        return new DatabaseDatasetLookupService(serviceName, dataSource, dataset);
    }
}
```

### 4. DataSourceFactory Becomes Internal

```java
/**
 * Internal factory for creating data source instances.
 * 
 * NOTE: This class should not be used directly by application code.
 * Use DataSourceRegistry.getInstance().getOrCreate() instead.
 * 
 * This factory handles the low-level creation of ExternalDataSource
 * instances. Caching and lifecycle management is handled by DataSourceRegistry.
 */
class DataSourceFactory {  // Package-private, not public
    
    // REMOVE: jdbcDataSourceCache (moved to DataSourceRegistry)
    // REMOVE: httpClientCache (moved to DataSourceRegistry)
    // REMOVE: pendingCreations (moved to DataSourceRegistry)
    
    // KEEP: customProviders, registration logic
    // KEEP: createDataSourceByType, createFileSystemDataSource, etc.
    
    ExternalDataSource createDataSource(DataSourceConfiguration config) {
        // Pure creation, no caching
        return createDataSourceByType(config.getDataSourceType(), config);
    }
}
```

## Migration Path

### Phase 1: Enhance DataSourceRegistry (Low Risk)
1. Add `getOrCreate()` method to existing `DataSourceRegistry`
2. Move JDBC pool cache from `DataSourceFactory` to `DataSourceRegistry`
3. Move HTTP client cache from `DataSourceFactory` to `DataSourceRegistry`
4. Tests still pass (backward compatible)

### Phase 2: Update Consumers (Medium Risk)
1. Update `RulesEngine` to use `DataSourceRegistry.getInstance().getOrCreate()`
2. Remove local `dataSources` map from `RulesEngine`
3. Update tests

### Phase 3: Simplify DatasetLookupServiceFactory (Medium Risk)
1. Remove `dataSourceRegistry` parameter from all method signatures
2. Use `DataSourceRegistry.getInstance().getOrCreate()` internally
3. Update all callers (remove registry passing)

### Phase 4: Internalize DataSourceFactory (Low Risk)
1. Make `DataSourceFactory` package-private
2. Remove public caching methods
3. Document that `DataSourceRegistry` is the public API

## Benefits

| Before | After |
|--------|-------|
| 4 caching layers | 1 unified registry |
| Parameter passing (fragile) | Singleton access (reliable) |
| Scattered lifecycle management | Centralized shutdown |
| Multiple "sources of truth" | Single source of truth |
| Hard to debug caching issues | Clear cache behavior |
| Health monitoring unused | Health monitoring integrated |

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Singleton makes testing harder | Add `reset()` method for tests |
| Global state | Already have global state (just scattered) |
| Breaking changes | Phased migration, keep old methods temporarily |
| Thread-safety | Use existing ConcurrentHashMap patterns |

## Questions to Consider

1. **Scope of registry**: Should registry be per-RulesEngine or truly global?
   - Global simplifies sharing but complicates multi-tenant scenarios
   - Per-engine is safer but requires passing reference

2. **Pool sharing**: Should different named data sources share JDBC pools?
   - Yes (current design): Efficient, but lifecycle is complex
   - No: Each data source gets own pool, simpler but more connections

3. **Cleanup responsibility**: Who calls `shutdown()`?
   - RulesEngine (explicit)
   - Application shutdown hook (implicit)
   - Both (belt and suspenders)
