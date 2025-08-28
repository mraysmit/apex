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


import dev.mars.apex.core.service.data.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Central registry for managing external data sources.
 * 
 * This class provides centralized registration, discovery, and lifecycle
 * management for all external data sources in the system.
 * 
 * Features:
 * - Thread-safe data source registration and lookup
 * - Health monitoring and status tracking
 * - Automatic cleanup of failed data sources
 * - Event-driven notifications for data source changes
 * - Query capabilities for data source discovery
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRegistry.class);
    
    // Singleton instance
    private static volatile DataSourceRegistry instance;
    private static final Object LOCK = new Object();
    
    // Registry storage
    private final Map<String, DataSourceRegistration> dataSources = new ConcurrentHashMap<>();
    private final Map<DataSourceType, Set<String>> typeIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> tagIndex = new ConcurrentHashMap<>();
    
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
     */
    public void shutdown() {
        LOGGER.info("Shutting down data source registry with {} registered data sources", dataSources.size());
        
        // Stop monitoring
        stopHealthMonitoring();
        
        // Shutdown all data sources
        for (DataSourceRegistration registration : dataSources.values()) {
            try {
                registration.getDataSource().shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down data source '{}': {}", 
                    registration.getDataSource().getName(), e.getMessage());
            }
        }
        
        // Clear registry
        synchronized (this) {
            dataSources.clear();
            typeIndex.clear();
            tagIndex.clear();
        }
        listeners.clear();
        
        LOGGER.info("Data source registry shut down");
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
            LOGGER.error("Error during health check", e);
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
                LOGGER.error("Error notifying registry listener", e);
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
