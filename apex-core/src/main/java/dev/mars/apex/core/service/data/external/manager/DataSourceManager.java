package dev.mars.apex.core.service.data.external.manager;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistryEvent;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistryListener;
import dev.mars.apex.core.service.data.external.registry.RegistryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Central manager for coordinating multiple external data sources.
 * 
 * This class provides high-level management capabilities for data sources,
 * including lifecycle management, health monitoring, load balancing,
 * and resource coordination.
 * 
 * Features:
 * - Centralized data source lifecycle management
 * - Health monitoring and automatic recovery
 * - Load balancing and failover capabilities
 * - Resource pooling and optimization
 * - Event-driven notifications
 * - Performance monitoring and metrics
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceManager implements DataSourceRegistryListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceManager.class);
    
    // Core components
    private final DataSourceRegistry registry;
    private final DataSourceFactory factory;
    
    // Configuration and state
    private final Map<String, DataSourceConfiguration> configurations = new ConcurrentHashMap<>();
    private final Map<String, DataSourceMetrics> metricsCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    private volatile boolean running = false;
    
    // Background tasks
    private ScheduledExecutorService managementExecutor;
    private final ExecutorService queryExecutor;
    
    // Load balancing and failover
    private final Map<DataSourceType, List<String>> typeGroups = new ConcurrentHashMap<>();
    private final Map<String, Integer> loadBalancingCounters = new ConcurrentHashMap<>();
    
    // Event listeners
    private final List<DataSourceManagerListener> listeners = new ArrayList<>();
    
    /**
     * Constructor with default registry and factory.
     */
    public DataSourceManager() {
        this(DataSourceRegistry.getInstance(), DataSourceFactory.getInstance());
    }
    
    /**
     * Constructor with custom registry and factory.
     * 
     * @param registry The data source registry
     * @param factory The data source factory
     */
    public DataSourceManager(DataSourceRegistry registry, DataSourceFactory factory) {
        this.registry = registry;
        this.factory = factory;
        this.queryExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "DataSourceManager-Query");
            thread.setDaemon(true);
            return thread;
        });
        
        // Register as a registry listener
        registry.addListener(this);
    }
    
    /**
     * Initialize the manager with configurations.
     * 
     * @param configurations List of data source configurations
     * @throws DataSourceException if initialization fails
     */
    public void initialize(List<DataSourceConfiguration> configurations) throws DataSourceException {
        if (initialized) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "DataSourceManager is already initialized");
        }
        
        LOGGER.info("Initializing DataSourceManager with {} configurations", configurations.size());
        
        try {
            // Create and register all data sources
            for (DataSourceConfiguration config : configurations) {
                createAndRegisterDataSource(config);
            }
            
            // Start background management tasks
            startManagementTasks();
            
            initialized = true;
            running = true;
            
            LOGGER.info("DataSourceManager initialized successfully with {} data sources", 
                registry.size());
            
            // Notify listeners
            notifyListeners(DataSourceManagerEvent.initialized(registry.size()));
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize DataSourceManager", e);
            
            // Clean up any partially created data sources
            shutdown();
            
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to initialize DataSourceManager", e);
        }
    }
    
    /**
     * Add a new data source configuration.
     * 
     * @param configuration The data source configuration
     * @throws DataSourceException if creation fails
     */
    public void addDataSource(DataSourceConfiguration configuration) throws DataSourceException {
        if (!running) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "DataSourceManager is not running");
        }
        
        createAndRegisterDataSource(configuration);
        LOGGER.info("Added data source '{}'", configuration.getName());
    }
    
    /**
     * Remove a data source by name.
     * 
     * @param name The name of the data source to remove
     * @return true if the data source was removed
     */
    public boolean removeDataSource(String name) {
        if (!running) {
            return false;
        }
        
        boolean removed = registry.unregister(name);
        if (removed) {
            configurations.remove(name);
            metricsCache.remove(name);
            
            // Update type groups
            updateTypeGroups();
            
            LOGGER.info("Removed data source '{}'", name);
            
            // Notify listeners
            notifyListeners(DataSourceManagerEvent.dataSourceRemoved(name));
        }
        
        return removed;
    }
    
    /**
     * Get a data source by name.
     * 
     * @param name The name of the data source
     * @return The data source, or null if not found
     */
    public ExternalDataSource getDataSource(String name) {
        return registry.getDataSource(name);
    }
    
    /**
     * Get all registered data source names.
     * 
     * @return Set of data source names
     */
    public Set<String> getDataSourceNames() {
        return registry.getDataSourceNames();
    }
    
    /**
     * Get data sources by type.
     * 
     * @param type The data source type
     * @return List of data sources of the specified type
     */
    public List<ExternalDataSource> getDataSourcesByType(DataSourceType type) {
        return registry.getDataSourcesByType(type);
    }
    
    /**
     * Get a data source with load balancing for the specified type.
     * 
     * @param type The data source type
     * @return A data source instance, or null if none available
     */
    public ExternalDataSource getDataSourceWithLoadBalancing(DataSourceType type) {
        List<String> typeGroup = typeGroups.get(type);
        if (typeGroup == null || typeGroup.isEmpty()) {
            return null;
        }
        
        // Simple round-robin load balancing
        String typeKey = type.name();
        int counter = loadBalancingCounters.compute(typeKey, (k, v) -> (v == null ? 0 : v + 1));
        int index = counter % typeGroup.size();
        
        String selectedName = typeGroup.get(index);
        ExternalDataSource dataSource = registry.getDataSource(selectedName);
        
        // If the selected data source is unhealthy, try to find a healthy one
        if (dataSource == null || !dataSource.isHealthy()) {
            for (String name : typeGroup) {
                ExternalDataSource candidate = registry.getDataSource(name);
                if (candidate != null && candidate.isHealthy()) {
                    return candidate;
                }
            }
        }
        
        return dataSource;
    }
    
    /**
     * Execute a query with automatic failover.
     * 
     * @param type The data source type
     * @param query The query to execute
     * @param parameters Query parameters
     * @param <T> The result type
     * @return Query results
     * @throws DataSourceException if all data sources fail
     */
    public <T> List<T> queryWithFailover(DataSourceType type, String query, 
                                        Map<String, Object> parameters) throws DataSourceException {
        
        List<ExternalDataSource> dataSources = getHealthyDataSourcesByType(type);
        if (dataSources.isEmpty()) {
            throw new DataSourceException(DataSourceException.ErrorType.CONNECTION_ERROR,
                "No healthy data sources available for type: " + type);
        }
        
        DataSourceException lastException = null;
        
        for (ExternalDataSource dataSource : dataSources) {
            try {
                return dataSource.query(query, parameters);
            } catch (DataSourceException e) {
                lastException = e;
                LOGGER.warn("Query failed on data source '{}', trying next: {}", 
                    dataSource.getName(), e.getMessage());
            }
        }
        
        throw new DataSourceException(DataSourceException.ErrorType.EXECUTION_ERROR,
            "All data sources failed for type: " + type, lastException);
    }
    
    /**
     * Execute a query asynchronously.
     * 
     * @param dataSourceName The name of the data source
     * @param query The query to execute
     * @param parameters Query parameters
     * @param <T> The result type
     * @return Future containing the query results
     */
    public <T> CompletableFuture<List<T>> queryAsync(String dataSourceName, String query, 
                                                    Map<String, Object> parameters) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                ExternalDataSource dataSource = registry.getDataSource(dataSourceName);
                if (dataSource == null) {
                    throw new RuntimeException("Data source not found: " + dataSourceName);
                }
                
                return dataSource.query(query, parameters);
            } catch (Exception e) {
                throw new RuntimeException("Async query failed", e);
            }
        }, queryExecutor);
    }
    
    /**
     * Get healthy data sources by type.
     * 
     * @param type The data source type
     * @return List of healthy data sources
     */
    public List<ExternalDataSource> getHealthyDataSourcesByType(DataSourceType type) {
        return registry.getDataSourcesByType(type).stream()
            .filter(ExternalDataSource::isHealthy)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all healthy data sources.
     * 
     * @return List of healthy data sources
     */
    public List<ExternalDataSource> getHealthyDataSources() {
        return registry.getHealthyDataSources();
    }
    
    /**
     * Get all unhealthy data sources.
     * 
     * @return List of unhealthy data sources
     */
    public List<ExternalDataSource> getUnhealthyDataSources() {
        return registry.getUnhealthyDataSources();
    }
    
    /**
     * Refresh all data sources.
     */
    public void refreshAll() {
        LOGGER.info("Refreshing all data sources");
        registry.refreshAll();
        
        // Clear metrics cache to force refresh
        metricsCache.clear();
        
        // Notify listeners
        notifyListeners(DataSourceManagerEvent.refreshCompleted());
    }
    
    /**
     * Get manager statistics.
     * 
     * @return Manager statistics
     */
    public DataSourceManagerStatistics getStatistics() {
        RegistryStatistics registryStats = registry.getStatistics();
        
        // Collect metrics from all data sources
        Map<String, DataSourceMetrics> currentMetrics = new HashMap<>();
        for (String name : registry.getDataSourceNames()) {
            ExternalDataSource dataSource = registry.getDataSource(name);
            if (dataSource != null) {
                currentMetrics.put(name, dataSource.getMetrics());
            }
        }
        
        return new DataSourceManagerStatistics(
            registryStats,
            currentMetrics,
            typeGroups.size(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Add a manager event listener.
     * 
     * @param listener The listener to add
     */
    public void addListener(DataSourceManagerListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
    }
    
    /**
     * Remove a manager event listener.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(DataSourceManagerListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }
    
    /**
     * Check if the manager is running.
     * 
     * @return true if the manager is running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Check if the manager is initialized.
     * 
     * @return true if the manager is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Shutdown the manager and all managed data sources.
     */
    public void shutdown() {
        LOGGER.info("Shutting down DataSourceManager");
        
        running = false;
        
        // Stop management tasks
        stopManagementTasks();
        
        // Shutdown query executor
        queryExecutor.shutdown();
        try {
            if (!queryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                queryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            queryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Shutdown registry (which will shutdown all data sources)
        registry.shutdown();
        
        // Clear state
        configurations.clear();
        metricsCache.clear();
        typeGroups.clear();
        loadBalancingCounters.clear();
        
        synchronized (listeners) {
            listeners.clear();
        }
        
        initialized = false;
        
        LOGGER.info("DataSourceManager shut down");
    }

    // DataSourceRegistryListener implementation

    @Override
    public void onDataSourceEvent(DataSourceRegistryEvent event) {
        switch (event.getEventType()) {
            case REGISTERED:
                updateTypeGroups();
                notifyListeners(DataSourceManagerEvent.dataSourceAdded(event.getDataSourceName()));
                break;

            case UNREGISTERED:
                updateTypeGroups();
                notifyListeners(DataSourceManagerEvent.dataSourceRemoved(event.getDataSourceName()));
                break;

            case HEALTH_RESTORED:
                notifyListeners(DataSourceManagerEvent.healthRestored(event.getDataSourceName()));
                break;

            case HEALTH_LOST:
                notifyListeners(DataSourceManagerEvent.healthLost(event.getDataSourceName()));
                break;
        }
    }

    // Private helper methods

    /**
     * Create and register a data source from configuration.
     */
    private void createAndRegisterDataSource(DataSourceConfiguration configuration) throws DataSourceException {
        String name = configuration.getName();

        // Store configuration
        configurations.put(name, configuration);

        try {
            // Create data source
            ExternalDataSource dataSource = factory.createDataSource(configuration);

            // Register with registry
            registry.register(dataSource);

            // Update type groups
            updateTypeGroups();

        } catch (DataSourceException e) {
            // Clean up on failure
            configurations.remove(name);
            throw e;
        }
    }

    /**
     * Update type groups for load balancing.
     */
    private void updateTypeGroups() {
        typeGroups.clear();

        for (String name : registry.getDataSourceNames()) {
            ExternalDataSource dataSource = registry.getDataSource(name);
            if (dataSource != null) {
                DataSourceType type = dataSource.getSourceType();
                typeGroups.computeIfAbsent(type, k -> new ArrayList<>()).add(name);
            }
        }
    }

    /**
     * Start background management tasks.
     */
    private void startManagementTasks() {
        managementExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "DataSourceManager-Management");
            thread.setDaemon(true);
            return thread;
        });

        // Schedule metrics collection every 60 seconds
        managementExecutor.scheduleAtFixedRate(
            this::collectMetrics,
            60, // Initial delay
            60, // Period
            TimeUnit.SECONDS
        );

        LOGGER.debug("Started background management tasks");
    }

    /**
     * Stop background management tasks.
     */
    private void stopManagementTasks() {
        if (managementExecutor != null) {
            managementExecutor.shutdown();
            try {
                if (!managementExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    managementExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                managementExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Collect metrics from all data sources.
     */
    private void collectMetrics() {
        try {
            for (String name : registry.getDataSourceNames()) {
                ExternalDataSource dataSource = registry.getDataSource(name);
                if (dataSource != null) {
                    DataSourceMetrics metrics = dataSource.getMetrics();
                    metricsCache.put(name, metrics);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error collecting metrics", e);
        }
    }

    /**
     * Notify all listeners of a manager event.
     */
    private void notifyListeners(DataSourceManagerEvent event) {
        List<DataSourceManagerListener> currentListeners;
        synchronized (listeners) {
            currentListeners = new ArrayList<>(listeners);
        }

        for (DataSourceManagerListener listener : currentListeners) {
            try {
                listener.onManagerEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error notifying manager listener", e);
            }
        }
    }
}
