package dev.mars.apex.core.service.data.external.registry;

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


import com.zaxxer.hikari.HikariDataSource;
import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import dev.mars.apex.core.service.data.external.database.DatabaseDataSource;
import dev.mars.apex.core.service.data.external.database.JdbcTemplateFactory;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.rest.RestApiDataSource;
import dev.mars.apex.core.service.data.external.rest.RestTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Central registry for managing external data sources - the SINGLE SOURCE OF TRUTH.
 * 
 * <h2>Architecture Overview</h2>
 * <p>
 * This class is the unified caching layer for all external data sources in APEX.
 * It consolidates what was previously 4 separate caching mechanisms into a single,
 * consistent design:
 * </p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │              DataSourceRegistry (Singleton)                     │
 * │         SINGLE SOURCE OF TRUTH for ExternalDataSource           │
 * │                                                                 │
 * │   Caches:                                                       │
 * │   - dataSources: ExternalDataSource instances by name           │
 * │   - jdbcPoolCache: HikariCP pools by connection key             │
 * │   - httpClientCache: HttpClient instances by base URL           │
 * │   - pendingCreations: Deduplication for concurrent requests     │
 * └─────────────────────────────────────────────────────────────────┘
 *                                │
 *                     calls when cache miss
 *                                ↓
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                      DataSourceFactory                          │
 * │              Low-level factory (creates instances)              │
 * │   - createDataSource() → always creates fresh instance          │
 * │   - Maintains underlying resource caches (JDBC, HTTP)           │
 * └─────────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Key Design Principles</h2>
 * <ul>
 *   <li><b>Single Source of Truth:</b> All components (RulesEngine, PipelineExecutionManager,
 *       DatasetLookupServiceFactory) use this registry for data source access</li>
 *   <li><b>Thread-Safe Deduplication:</b> Concurrent requests for the same data source
 *       are automatically deduplicated via CompletableFuture pattern</li>
 *   <li><b>Resource Sharing:</b> JDBC connection pools and HTTP clients are shared
 *       across data sources with the same connection details</li>
 *   <li><b>Lazy Creation:</b> Data sources are created on first access via getOrCreate()</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <pre>
 * // Primary API - get existing or create new (RECOMMENDED)
 * ExternalDataSource ds = DataSourceRegistry.getInstance().getOrCreate("my-db", config);
 * 
 * // Lookup only (no creation)
 * Optional&lt;ExternalDataSource&gt; ds = DataSourceRegistry.getInstance().get("my-db");
 * 
 * // Check existence
 * if (DataSourceRegistry.getInstance().contains("my-db")) { ... }
 * </pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * All operations are thread-safe. The getOrCreate() method uses CompletableFuture-based
 * deduplication to ensure that only one creation operation occurs for concurrent
 * requests with the same data source name.
 * </p>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 2.0
 * @see DataSourceFactory
 * @see ExternalDataSource
 */
public class DataSourceRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRegistry.class);
    
    // Singleton instance
    private static volatile DataSourceRegistry instance;
    private static final Object LOCK = new Object();
    
    // Registry storage - primary data source registry
    private final Map<String, DataSourceRegistration> dataSources = new ConcurrentHashMap<>();
    private final Map<DataSourceType, Set<String>> typeIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> tagIndex = new ConcurrentHashMap<>();
    
    // Connection pool caches - shared across data sources for efficiency
    // These are keyed by connection details, not data source name, allowing pool reuse
    private final Map<String, DataSource> jdbcPoolCache = new ConcurrentHashMap<>();
    private final Map<String, HttpClient> httpClientCache = new ConcurrentHashMap<>();
    
    // Deduplication for concurrent creation requests
    private final Map<String, CompletableFuture<ExternalDataSource>> pendingCreations = new ConcurrentHashMap<>();
    
    // Event listeners - use thread-safe collection
    private final List<DataSourceRegistryListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    // Background monitoring
    private ScheduledExecutorService monitoringExecutor;
    private volatile boolean monitoring = false;
    
    /**
     * Private constructor for singleton pattern.
     */
    private DataSourceRegistry() {
        startHealthMonitoring();
    }
    
    /**
     * Get the singleton instance of the registry.
     * 
     * @return The registry instance
     */
    public static DataSourceRegistry getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DataSourceRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a data source in the registry.
     * 
     * @param dataSource The data source to register
     * @throws DataSourceException if registration fails
     */
    public void register(ExternalDataSource dataSource) throws DataSourceException {
        if (dataSource == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Cannot register null data source");
        }
        
        String name = dataSource.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Data source name cannot be null or empty");
        }
        
        synchronized (this) {
            if (dataSources.containsKey(name)) {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Data source with name '" + name + "' is already registered");
            }
            
            DataSourceRegistration registration = new DataSourceRegistration(dataSource);
            dataSources.put(name, registration);
            
            // Update type index
            DataSourceType type = dataSource.getSourceType();
            typeIndex.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(name);
            
            // Update tag index if tags are available
            Set<String> tags = getDataSourceTags(dataSource);
            for (String tag : tags) {
                tagIndex.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(name);
            }
            
            LOGGER.info("Registered data source '{}' of type {}", name, type);
            
            // Notify listeners
            notifyListeners(DataSourceRegistryEvent.registered(name, dataSource));
        }
    }
    
    // ========================================================================
    // PRIMARY API: getOrCreate - Unified data source access
    // ========================================================================
    
    /**
     * Get existing data source or create and register a new one.
     * 
     * This is the PRIMARY API for obtaining data sources. It handles:
     * - Checking if data source already exists (fast path)
     * - Creating new data source if needed
     * - Deduplicating concurrent creation requests
     * - Automatic registration
     * - Reusing shared connection pools (JDBC, HTTP)
     * 
     * Thread-safe and idempotent - calling multiple times with the same name
     * returns the same data source instance.
     * 
     * @param name Unique name for the data source (e.g., "postgres-database")
     * @param config Configuration for creation (only used if not already registered)
     * @return The data source (existing or newly created)
     * @throws DataSourceException if creation fails
     */
    public ExternalDataSource getOrCreate(String name, DataSourceConfiguration config) throws DataSourceException {
        Objects.requireNonNull(name, "Data source name cannot be null");
        Objects.requireNonNull(config, "Data source configuration cannot be null");
        
        // Fast path: already registered
        DataSourceRegistration existing = dataSources.get(name);
        if (existing != null) {
            LOGGER.debug("Returning cached data source from registry: {}", name);
            return existing.getDataSource();
        }
        
        // Slow path: need to create (with deduplication)
        return createWithDeduplication(name, config);
    }
    
    /**
     * Get data source by name (lookup only, no creation).
     * 
     * Use this when you want to check if a data source exists without
     * triggering creation.
     * 
     * @param name The name of the data source
     * @return Optional containing the data source, or empty if not found
     */
    public Optional<ExternalDataSource> get(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        DataSourceRegistration registration = dataSources.get(name);
        return Optional.ofNullable(registration).map(DataSourceRegistration::getDataSource);
    }
    
    /**
     * Check if a data source exists in the registry.
     * 
     * @param name The name to check
     * @return true if a data source with this name is registered
     */
    public boolean contains(String name) {
        return name != null && dataSources.containsKey(name);
    }
    
    /**
     * Create data source with deduplication for concurrent requests.
     */
    private ExternalDataSource createWithDeduplication(String name, DataSourceConfiguration config) 
            throws DataSourceException {
        
        // Use computeIfAbsent to ensure only one creation per name
        CompletableFuture<ExternalDataSource> future = pendingCreations.computeIfAbsent(name,
            k -> CompletableFuture.supplyAsync(() -> {
                try {
                    return createAndRegister(name, config);
                } catch (DataSourceException e) {
                    throw new DataSourceResolutionException("Failed to create data source: " + name, e);
                } finally {
                    pendingCreations.remove(k);
                }
            }));
        
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Data source creation was interrupted: " + name, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DataSourceResolutionException && cause.getCause() instanceof DataSourceException) {
                throw (DataSourceException) cause.getCause();
            } else if (cause instanceof DataSourceException) {
                throw (DataSourceException) cause;
            } else {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Failed to create data source: " + name, cause);
            }
        }
    }
    
    /**
     * Create a new data source and register it.
     */
    private ExternalDataSource createAndRegister(String name, DataSourceConfiguration config) 
            throws DataSourceException {
        
        // Double-check in case another thread registered while we were waiting
        DataSourceRegistration existing = dataSources.get(name);
        if (existing != null) {
            LOGGER.debug("Data source '{}' was registered by another thread, returning existing", name);
            return existing.getDataSource();
        }
        
        LOGGER.info("Creating new data source: {} (type: {})", name, config.getDataSourceType());
        
        // Create the data source using shared pools where applicable
        ExternalDataSource dataSource = createDataSourceWithSharedPools(name, config);
        
        // Register it (using synchronized block for consistency with register())
        synchronized (this) {
            // Final check inside synchronized block
            existing = dataSources.get(name);
            if (existing != null) {
                LOGGER.debug("Data source '{}' was registered while acquiring lock, returning existing", name);
                // Shutdown the one we just created since we won't use it
                try {
                    dataSource.shutdown();
                } catch (Exception e) {
                    LOGGER.debug("Error shutting down duplicate data source: {}", e.getMessage());
                }
                return existing.getDataSource();
            }
            
            DataSourceRegistration registration = new DataSourceRegistration(dataSource);
            dataSources.put(name, registration);
            
            // Update type index
            DataSourceType type = dataSource.getSourceType();
            typeIndex.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(name);
            
            // Update tag index
            Set<String> tags = getDataSourceTags(dataSource);
            for (String tag : tags) {
                tagIndex.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(name);
            }
            
            LOGGER.info("Registered new data source: {} (type: {})", name, type);
            notifyListeners(DataSourceRegistryEvent.registered(name, dataSource));
        }
        
        return dataSource;
    }
    
    /**
     * Create data source, reusing shared connection pools where applicable.
     */
    private ExternalDataSource createDataSourceWithSharedPools(String name, DataSourceConfiguration config) 
            throws DataSourceException {
        
        DataSourceType type = config.getDataSourceType();
        
        switch (type) {
            case DATABASE:
                return createDatabaseDataSourceWithSharedPool(config);
            case REST_API:
                return createRestApiDataSourceWithSharedClient(config);
            default:
                // Other types don't need shared resources - use factory
                return DataSourceFactory.getInstance().createDataSource(config);
        }
    }
    
    /**
     * Create database data source using a shared JDBC connection pool.
     * Multiple data sources with the same connection details share one HikariCP pool.
     */
    private ExternalDataSource createDatabaseDataSourceWithSharedPool(DataSourceConfiguration config) 
            throws DataSourceException {
        
        // Pool key based on connection details (not data source name)
        String poolKey = buildJdbcPoolKey(config);
        
        // Check if we already have this pool
        boolean poolExisted = jdbcPoolCache.containsKey(poolKey);
        
        // Get or create the HikariCP pool
        DataSource jdbcPool = jdbcPoolCache.computeIfAbsent(poolKey, k -> {
            try {
                LOGGER.info("Creating new JDBC connection pool: {}", poolKey);
                return JdbcTemplateFactory.createDataSource(config);
            } catch (DataSourceException e) {
                throw new DataSourceResolutionException("Failed to create JDBC connection pool: " + poolKey, e);
            }
        });
        
        if (poolExisted) {
            LOGGER.info("Reusing existing JDBC connection pool: {}", poolKey);
        }
        
        // Create wrapper using shared pool
        return new DatabaseDataSource(jdbcPool, config);
    }
    
    /**
     * Create REST API data source using a shared HTTP client.
     */
    private ExternalDataSource createRestApiDataSourceWithSharedClient(DataSourceConfiguration config) 
            throws DataSourceException {
        
        String clientKey = buildHttpClientKey(config);
        
        boolean clientExisted = httpClientCache.containsKey(clientKey);
        
        HttpClient client = httpClientCache.computeIfAbsent(clientKey, k -> {
            try {
                LOGGER.info("Creating new HTTP client: {}", clientKey);
                return RestTemplateFactory.createHttpClient(config);
            } catch (DataSourceException e) {
                throw new DataSourceResolutionException("Failed to create HTTP client: " + clientKey, e);
            }
        });
        
        if (clientExisted) {
            LOGGER.info("Reusing existing HTTP client: {}", clientKey);
        }
        
        return new RestApiDataSource(client, config);
    }
    
    /**
     * Build cache key for JDBC connection pool.
     * Key is based on connection details, not data source name.
     * 
     * IMPORTANT: Schema IS included because the schema is baked into the JDBC URL
     * (e.g., PostgreSQL uses currentSchema parameter). Different schemas need
     * separate connection pools to ensure correct query routing.
     */
    private String buildJdbcPoolKey(DataSourceConfiguration config) {
        StringBuilder key = new StringBuilder("jdbc:");
        if (config.getConnection() != null) {
            key.append(config.getConnection().getHost()).append(":");
            key.append(config.getConnection().getPort()).append(":");
            key.append(config.getConnection().getDatabase()).append(":");
            key.append(config.getConnection().getUsername()).append(":");
            // Schema IS included - JDBC URL contains schema (e.g., PostgreSQL currentSchema)
            String schema = config.getConnection().getSchema();
            key.append(schema != null ? schema : "default");
        }
        return key.toString();
    }
    
    /**
     * Build cache key for HTTP client.
     */
    private String buildHttpClientKey(DataSourceConfiguration config) {
        StringBuilder key = new StringBuilder("http:");
        if (config.getConnection() != null) {
            key.append(config.getConnection().getBaseUrl()).append(":");
            key.append(config.getConnection().getTimeout()).append(":");
            key.append(config.getConnection().isSslEnabled());
        }
        return key.toString();
    }
    
    // ========================================================================
    // EXISTING API: Registration and lookup methods
    // ========================================================================
    
    /**
     * Unregister a data source from the registry.
     * 
     * @param name The name of the data source to unregister
     * @return true if the data source was unregistered, false if it wasn't found
     */
    public boolean unregister(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        synchronized (this) {
            DataSourceRegistration registration = dataSources.remove(name);
            if (registration == null) {
                return false;
            }
            
            ExternalDataSource dataSource = registration.getDataSource();
            
            // Remove from type index
            DataSourceType type = dataSource.getSourceType();
            Set<String> typeSet = typeIndex.get(type);
            if (typeSet != null) {
                typeSet.remove(name);
                if (typeSet.isEmpty()) {
                    typeIndex.remove(type);
                }
            }
            
            // Remove from tag index
            Set<String> tags = getDataSourceTags(dataSource);
            for (String tag : tags) {
                Set<String> tagSet = tagIndex.get(tag);
                if (tagSet != null) {
                    tagSet.remove(name);
                    if (tagSet.isEmpty()) {
                        tagIndex.remove(tag);
                    }
                }
            }
            
            // Shutdown the data source
            try {
                dataSource.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Error shutting down data source '{}': {}", name, e.getMessage());
                LOGGER.debug("Full exception details:", e);
            }
            
            LOGGER.info("Unregistered data source '{}'", name);
            
            // Notify listeners
            notifyListeners(DataSourceRegistryEvent.unregistered(name, dataSource));
            
            return true;
        }
    }
    
    /**
     * Get a data source by name.
     * 
     * @param name The name of the data source
     * @return The data source, or null if not found
     */
    public ExternalDataSource getDataSource(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        DataSourceRegistration registration = dataSources.get(name);
        return registration != null ? registration.getDataSource() : null;
    }
    
    /**
     * Get all registered data source names.
     * 
     * @return Set of data source names
     */
    public Set<String> getDataSourceNames() {
        return new HashSet<>(dataSources.keySet());
    }
    
    /**
     * Get data sources by type.
     * 
     * @param type The data source type
     * @return List of data sources of the specified type
     */
    public List<ExternalDataSource> getDataSourcesByType(DataSourceType type) {
        Set<String> names = typeIndex.get(type);
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        
        return names.stream()
            .map(this::getDataSource)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get data sources by tag.
     * 
     * @param tag The tag to search for
     * @return List of data sources with the specified tag
     */
    public List<ExternalDataSource> getDataSourcesByTag(String tag) {
        Set<String> names = tagIndex.get(tag);
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        
        return names.stream()
            .map(this::getDataSource)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all healthy data sources.
     * 
     * @return List of healthy data sources
     */
    public List<ExternalDataSource> getHealthyDataSources() {
        return dataSources.values().stream()
            .map(DataSourceRegistration::getDataSource)
            .filter(ExternalDataSource::isHealthy)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all unhealthy data sources.
     * 
     * @return List of unhealthy data sources
     */
    public List<ExternalDataSource> getUnhealthyDataSources() {
        return dataSources.values().stream()
            .map(DataSourceRegistration::getDataSource)
            .filter(ds -> !ds.isHealthy())
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a data source is registered.
     * 
     * @param name The name of the data source
     * @return true if the data source is registered
     */
    public boolean isRegistered(String name) {
        return name != null && dataSources.containsKey(name);
    }
    
    /**
     * Get the number of registered data sources.
     * 
     * @return Number of registered data sources
     */
    public int size() {
        return dataSources.size();
    }
    
    /**
     * Get registry statistics.
     * 
     * @return Registry statistics
     */
    public RegistryStatistics getStatistics() {
        Map<DataSourceType, Integer> typeCounts = new HashMap<>();
        int healthyCount = 0;
        int unhealthyCount = 0;
        
        for (DataSourceRegistration registration : dataSources.values()) {
            ExternalDataSource dataSource = registration.getDataSource();
            DataSourceType type = dataSource.getSourceType();
            
            typeCounts.merge(type, 1, Integer::sum);
            
            if (dataSource.isHealthy()) {
                healthyCount++;
            } else {
                unhealthyCount++;
            }
        }
        
        return new RegistryStatistics(
            dataSources.size(),
            healthyCount,
            unhealthyCount,
            typeCounts,
            LocalDateTime.now()
        );
    }
    
    /**
     * Add a registry event listener.
     * 
     * @param listener The listener to add
     */
    public void addListener(DataSourceRegistryListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a registry event listener.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(DataSourceRegistryListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Refresh all data sources.
     */
    public void refreshAll() {
        LOGGER.info("Refreshing all {} registered data sources", dataSources.size());
        
        for (DataSourceRegistration registration : dataSources.values()) {
            try {
                registration.getDataSource().refresh();
            } catch (Exception e) {
                LOGGER.error("Failed to refresh data source '{}': {}", 
                    registration.getDataSource().getName(), e.getMessage());
            }
        }
    }
    
    /**
     * Shutdown the registry and all registered data sources.
     * Also closes all shared connection pools.
     */
    public void shutdown() {
        LOGGER.info("Shutting down data source registry with {} registered data sources", dataSources.size());
        
        // Stop monitoring
        stopHealthMonitoring();
        
        // Wait for any pending creations to complete
        for (CompletableFuture<ExternalDataSource> future : pendingCreations.values()) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.debug("Error waiting for pending creation during shutdown: {}", e.getMessage());
            }
        }
        pendingCreations.clear();
        
        // Shutdown all data sources
        for (DataSourceRegistration registration : dataSources.values()) {
            try {
                registration.getDataSource().shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down data source '{}': {}", 
                    registration.getDataSource().getName(), e.getMessage());
            }
        }
        
        // Close JDBC connection pools
        LOGGER.info("Closing {} JDBC connection pools", jdbcPoolCache.size());
        for (Map.Entry<String, DataSource> entry : jdbcPoolCache.entrySet()) {
            try {
                DataSource pool = entry.getValue();
                if (pool instanceof HikariDataSource) {
                    ((HikariDataSource) pool).close();
                    LOGGER.debug("Closed JDBC pool: {}", entry.getKey());
                }
            } catch (Exception e) {
                LOGGER.warn("Error closing JDBC pool '{}': {}", entry.getKey(), e.getMessage());
                LOGGER.debug("Full exception details:", e);
            }
        }
        jdbcPoolCache.clear();
        
        // Clear HTTP clients (no explicit close needed)
        httpClientCache.clear();
        
        // Clear registry
        synchronized (this) {
            dataSources.clear();
            typeIndex.clear();
            tagIndex.clear();
        }
        listeners.clear();
        
        LOGGER.info("Data source registry shut down completely");
    }
    
    /**
     * Clear all caches and registered data sources.
     * This method CLOSES connection pools to ensure proper isolation.
     */
    public void clear() {
        LOGGER.info("Clearing registry for testing - {} data sources, {} pools", 
            dataSources.size(), jdbcPoolCache.size());
        
        // Wait for pending creations
        pendingCreations.values().forEach(f -> {
            try {
                f.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.debug("Error waiting for pending creation: {}", e.getMessage());
            }
        });
        pendingCreations.clear();
        
        // Clear data sources (don't shut them down - they might be reused)
        synchronized (this) {
            dataSources.clear();
            typeIndex.clear();
            tagIndex.clear();
        }
        
        // Close JDBC connection pools (critical for Testcontainers - ports change between test classes)
        LOGGER.info("Closing {} JDBC connection pools for test isolation", jdbcPoolCache.size());
        for (Map.Entry<String, DataSource> entry : jdbcPoolCache.entrySet()) {
            try {
                DataSource pool = entry.getValue();
                if (pool instanceof HikariDataSource) {
                    ((HikariDataSource) pool).close();
                    LOGGER.debug("Closed JDBC pool: {}", entry.getKey());
                }
            } catch (Exception e) {
                LOGGER.warn("Error closing JDBC pool '{}': {}", entry.getKey(), e.getMessage());
                LOGGER.debug("Full exception details:", e);
            }
        }
        jdbcPoolCache.clear();
        httpClientCache.clear();
        
        LOGGER.info("Registry cleared for testing");
    }
    
    /**
     * Get pool cache statistics for monitoring/debugging.
     * 
     * @return Map of cache name to size
     */
    public Map<String, Integer> getPoolCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("jdbcPools", jdbcPoolCache.size());
        stats.put("httpClients", httpClientCache.size());
        stats.put("dataSources", dataSources.size());
        stats.put("pendingCreations", pendingCreations.size());
        return stats;
    }

    /**
     * Start health monitoring for all registered data sources.
     */
    private void startHealthMonitoring() {
        if (monitoring) {
            return;
        }

        monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "DataSourceRegistryMonitor");
            thread.setDaemon(true);
            return thread;
        });

        // Monitor health every 30 seconds
        monitoringExecutor.scheduleAtFixedRate(
            this::performHealthCheck,
            30, // Initial delay
            30, // Period
            TimeUnit.SECONDS
        );

        monitoring = true;
        LOGGER.debug("Started health monitoring for data source registry");
    }

    /**
     * Stop health monitoring.
     */
    private void stopHealthMonitoring() {
        if (!monitoring) {
            return;
        }

        if (monitoringExecutor != null) {
            monitoringExecutor.shutdown();
            try {
                if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitoringExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitoringExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        monitoring = false;
        LOGGER.debug("Stopped health monitoring for data source registry");
    }

    /**
     * Perform health check on all registered data sources.
     */
    private void performHealthCheck() {
        try {
            for (DataSourceRegistration registration : dataSources.values()) {
                ExternalDataSource dataSource = registration.getDataSource();
                boolean wasHealthy = registration.isHealthy();
                boolean isHealthy = dataSource.isHealthy();

                registration.updateHealthStatus(isHealthy);

                // Notify listeners of health status changes
                if (wasHealthy != isHealthy) {
                    DataSourceRegistryEvent event = isHealthy ?
                        DataSourceRegistryEvent.healthRestored(dataSource.getName(), dataSource) :
                        DataSourceRegistryEvent.healthLost(dataSource.getName(), dataSource);

                    notifyListeners(event);

                    LOGGER.info("Data source '{}' health status changed: {} -> {}",
                        dataSource.getName(), wasHealthy ? "HEALTHY" : "UNHEALTHY",
                        isHealthy ? "HEALTHY" : "UNHEALTHY");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error during health check: {}", e.getMessage());
            LOGGER.debug("Stack trace for registry health check error:", e);
        }
    }

    /**
     * Get tags for a data source.
     */
    private Set<String> getDataSourceTags(ExternalDataSource dataSource) {
        Set<String> tags = new HashSet<>();

        // Add type as a tag
        tags.add(dataSource.getSourceType().name().toLowerCase());

        // Add data type as a tag
        String dataType = dataSource.getDataType();
        if (dataType != null) {
            tags.add(dataType);
        }

        // Add configuration-based tags if available
        if (dataSource.getConfiguration() != null &&
            dataSource.getConfiguration().getTags() != null) {
            tags.addAll(dataSource.getConfiguration().getTags());
        }

        return tags;
    }

    /**
     * Notify all listeners of a registry event.
     */
    private void notifyListeners(DataSourceRegistryEvent event) {
        // CopyOnWriteArrayList is already thread-safe for iteration
        for (DataSourceRegistryListener listener : listeners) {
            try {
                listener.onDataSourceEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error notifying registry listener: {}", e.getMessage());
                LOGGER.debug("Stack trace for registry listener notification error:", e);
            }
        }
    }

    /**
     * Data source registration holder.
     */
    private static class DataSourceRegistration {
        private final ExternalDataSource dataSource;
        private volatile boolean healthy;

        public DataSourceRegistration(ExternalDataSource dataSource) {
            this.dataSource = dataSource;
            this.healthy = dataSource.isHealthy();
        }

        public ExternalDataSource getDataSource() {
            return dataSource;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void updateHealthStatus(boolean healthy) {
            this.healthy = healthy;
        }
    }
}
