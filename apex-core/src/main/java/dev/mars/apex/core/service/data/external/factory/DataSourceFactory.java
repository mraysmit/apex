package dev.mars.apex.core.service.data.external.factory;

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
import dev.mars.apex.core.service.data.external.*;
import dev.mars.apex.core.service.data.external.cache.CacheDataSource;
import dev.mars.apex.core.service.data.external.database.DatabaseDataSource;
import dev.mars.apex.core.service.data.external.database.JdbcTemplateFactory;
import dev.mars.apex.core.service.data.external.file.FileSystemDataSource;
import dev.mars.apex.core.service.data.external.rest.RestApiDataSource;
import dev.mars.apex.core.service.data.external.rest.RestTemplateFactory;
import dev.mars.apex.core.service.data.external.messagequeue.MessageQueueDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating external data source instances.
 * 
 * This factory creates and configures data source instances based on
 * configuration objects, handling the complexity of different data source
 * types and their specific initialization requirements.
 * 
 * Features:
 * - Type-based data source creation
 * - Configuration validation
 * - Resource management and caching
 * - Error handling and fallback mechanisms
 * - Extensible architecture for custom data sources
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);
    
    // Singleton instance
    private static volatile DataSourceFactory instance;
    private static final Object LOCK = new Object();
    
    // Cache for created resources to avoid recreation
    private final Map<String, DataSource> jdbcDataSourceCache = new ConcurrentHashMap<>();
    private final Map<String, HttpClient> httpClientCache = new ConcurrentHashMap<>();

    // Custom data source providers
    private final Map<String, DataSourceProvider> customProviders = new ConcurrentHashMap<>();

    // Concurrent creation deduplication
    private final Map<String, CompletableFuture<ExternalDataSource>> pendingCreations = new ConcurrentHashMap<>();
    
    /**
     * Private constructor for singleton pattern.
     */
    private DataSourceFactory() {
        // Initialize with default providers
        registerDefaultProviders();
    }
    
    /**
     * Get the singleton instance of the factory.
     * 
     * @return The factory instance
     */
    public static DataSourceFactory getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DataSourceFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * Create a data source from configuration.
     *
     * @param configuration The data source configuration
     * @return Configured data source instance
     * @throws DataSourceException if creation fails
     */
    public ExternalDataSource createDataSource(DataSourceConfiguration configuration) throws DataSourceException {
        if (configuration == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Configuration cannot be null");
        }

        // Validate configuration
        try {
            configuration.validate();
        } catch (IllegalArgumentException e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Invalid configuration: " + e.getMessage(), e);
        }

        // Generate unique key for deduplication
        String creationKey = generateCreationKey(configuration);

        // Use computeIfAbsent to ensure only one creation per unique configuration
        CompletableFuture<ExternalDataSource> future = pendingCreations.computeIfAbsent(creationKey,
            k -> CompletableFuture.supplyAsync(() -> {
                try {
                    return createDataSourceInternal(configuration);
                } catch (DataSourceException e) {
                    throw new RuntimeException(e);
                } finally {
                    // Remove from pending creations when done
                    pendingCreations.remove(k);
                }
            }));

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Data source creation was interrupted", e, configuration.getName(), "createDataSource", false);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof DataSourceException) {
                throw (DataSourceException) cause.getCause();
            } else if (cause instanceof DataSourceException) {
                throw (DataSourceException) cause;
            } else {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Failed to create data source '" + configuration.getName() + "'", cause,
                    configuration.getName(), "createDataSource", false);
            }
        }
    }

    /**
     * Internal method to create a data source without deduplication.
     * This is the actual implementation that was previously in createDataSource.
     */
    private ExternalDataSource createDataSourceInternal(DataSourceConfiguration configuration) throws DataSourceException {
        DataSourceType type = configuration.getDataSourceType();
        String name = configuration.getName();

        LOGGER.info("Creating data source '{}' of type {}", name, type);

        try {
            ExternalDataSource dataSource = createDataSourceByType(type, configuration);

            // Initialize the data source
            dataSource.initialize(configuration);

            LOGGER.info("Successfully created and initialized data source '{}'", name);
            return dataSource;

        } catch (DataSourceException e) {
            LOGGER.error("Failed to create data source '{}': {}", name, e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error creating data source '{}'", name, e);
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create data source '" + name + "'", e, name, "createDataSource", false);
        }
    }
    
    /**
     * Create multiple data sources from configurations.
     * 
     * @param configurations List of data source configurations
     * @return Map of data source name to instance
     * @throws DataSourceException if any creation fails
     */
    public Map<String, ExternalDataSource> createDataSources(
            java.util.List<DataSourceConfiguration> configurations) throws DataSourceException {
        
        Map<String, ExternalDataSource> dataSources = new ConcurrentHashMap<>();
        
        for (DataSourceConfiguration config : configurations) {
            try {
                ExternalDataSource dataSource = createDataSource(config);
                dataSources.put(config.getName(), dataSource);
            } catch (DataSourceException e) {
                // Clean up any successfully created data sources
                for (ExternalDataSource ds : dataSources.values()) {
                    try {
                        ds.shutdown();
                    } catch (Exception shutdownException) {
                        LOGGER.warn("Error shutting down data source during cleanup: {}", 
                            shutdownException.getMessage());
                    }
                }
                throw e;
            }
        }
        
        return dataSources;
    }
    
    /**
     * Register a custom data source provider.
     * 
     * @param type The data source type identifier
     * @param provider The provider implementation
     */
    public void registerProvider(String type, DataSourceProvider provider) {
        if (type != null && provider != null) {
            customProviders.put(type.toLowerCase(), provider);
            LOGGER.info("Registered custom data source provider for type: {}", type);
        }
    }
    
    /**
     * Unregister a custom data source provider.
     * 
     * @param type The data source type identifier
     */
    public void unregisterProvider(String type) {
        if (type != null) {
            DataSourceProvider removed = customProviders.remove(type.toLowerCase());
            if (removed != null) {
                LOGGER.info("Unregistered custom data source provider for type: {}", type);
            }
        }
    }
    
    /**
     * Check if a data source type is supported.
     * 
     * @param type The data source type
     * @return true if the type is supported
     */
    public boolean isTypeSupported(DataSourceType type) {
        return type != null && (isBuiltInType(type) || customProviders.containsKey(type.name().toLowerCase()));
    }
    
    /**
     * Check if a custom type is supported.
     * 
     * @param type The custom type identifier
     * @return true if the type is supported
     */
    public boolean isCustomTypeSupported(String type) {
        return type != null && customProviders.containsKey(type.toLowerCase());
    }
    
    /**
     * Get supported data source types.
     * 
     * @return Set of supported types
     */
    public java.util.Set<String> getSupportedTypes() {
        java.util.Set<String> types = new java.util.HashSet<>();
        
        // Add built-in types
        for (DataSourceType type : DataSourceType.values()) {
            types.add(type.name().toLowerCase());
        }
        
        // Add custom types
        types.addAll(customProviders.keySet());
        
        return types;
    }
    
    /**
     * Clear all cached resources.
     */
    public void clearCache() {
        jdbcDataSourceCache.clear();
        httpClientCache.clear();
        // Wait for any pending creations to complete before clearing
        pendingCreations.values().forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                LOGGER.warn("Error waiting for pending data source creation during cache clear", e);
            }
        });
        pendingCreations.clear();
        LOGGER.info("Cleared data source factory cache");
    }

    /**
     * Shutdown the factory and clean up resources.
     */
    public void shutdown() {
        clearCache();
        customProviders.clear();
        LOGGER.info("Data source factory shut down");
    }
    
    /**
     * Create a data source based on its type.
     */
    private ExternalDataSource createDataSourceByType(DataSourceType type, DataSourceConfiguration configuration) 
            throws DataSourceException {
        
        switch (type) {
            case DATABASE:
                return createDatabaseDataSource(configuration);
                
            case REST_API:
                return createRestApiDataSource(configuration);
                
            case FILE_SYSTEM:
                return createFileSystemDataSource(configuration);
                
            case CACHE:
                return createCacheDataSource(configuration);
                
            case MESSAGE_QUEUE:
                return createMessageQueueDataSource(configuration);
                
            case CUSTOM:
                return createCustomDataSource(configuration);
                
            default:
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Unsupported data source type: " + type);
        }
    }
    
    /**
     * Create a database data source.
     */
    private ExternalDataSource createDatabaseDataSource(DataSourceConfiguration configuration) 
            throws DataSourceException {
        
        try {
            // Create or get cached JDBC DataSource
            String cacheKey = generateJdbcCacheKey(configuration);
            DataSource jdbcDataSource = jdbcDataSourceCache.computeIfAbsent(cacheKey, 
                k -> {
                    try {
                        return JdbcTemplateFactory.createDataSource(configuration);
                    } catch (DataSourceException e) {
                        throw new RuntimeException(e);
                    }
                });
            
            return new DatabaseDataSource(jdbcDataSource, configuration);
            
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create database data source", e, configuration.getName(), "createDatabaseDataSource", false);
        }
    }
    
    /**
     * Create a REST API data source.
     */
    private ExternalDataSource createRestApiDataSource(DataSourceConfiguration configuration) 
            throws DataSourceException {
        
        try {
            // Create or get cached HttpClient
            String cacheKey = generateHttpCacheKey(configuration);
            HttpClient httpClient = httpClientCache.computeIfAbsent(cacheKey,
                k -> {
                    try {
                        return RestTemplateFactory.createHttpClient(configuration);
                    } catch (DataSourceException e) {
                        throw new RuntimeException(e);
                    }
                });
            
            return new RestApiDataSource(httpClient, configuration);
            
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create REST API data source", e, configuration.getName(), "createRestApiDataSource", false);
        }
    }
    
    /**
     * Create a file system data source.
     */
    private ExternalDataSource createFileSystemDataSource(DataSourceConfiguration configuration) 
            throws DataSourceException {
        
        try {
            return new FileSystemDataSource(configuration);
            
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create file system data source", e, configuration.getName(), "createFileSystemDataSource", false);
        }
    }
    
    /**
     * Create a cache data source.
     */
    private ExternalDataSource createCacheDataSource(DataSourceConfiguration configuration) 
            throws DataSourceException {
        
        try {
            return new CacheDataSource(configuration);
            
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create cache data source", e, configuration.getName(), "createCacheDataSource", false);
        }
    }
    
    /**
     * Create a message queue data source.
     */
    private ExternalDataSource createMessageQueueDataSource(DataSourceConfiguration configuration)
            throws DataSourceException {

        try {
            MessageQueueDataSource dataSource = new MessageQueueDataSource();
            dataSource.initialize(configuration);

            LOGGER.info("Created message queue data source: {}", configuration.getName());
            return dataSource;

        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create message queue data source", e, configuration.getName(), "create", false);
        }
    }
    
    /**
     * Create a custom data source.
     */
    private ExternalDataSource createCustomDataSource(DataSourceConfiguration configuration) 
            throws DataSourceException {
        
        String implementation = configuration.getImplementation();
        if (implementation == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Implementation class is required for custom data sources");
        }
        
        DataSourceProvider provider = customProviders.get(implementation.toLowerCase());
        if (provider == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "No provider registered for custom type: " + implementation);
        }
        
        return provider.createDataSource(configuration);
    }

    /**
     * Register default built-in providers.
     */
    private void registerDefaultProviders() {
        // Built-in types are handled directly in createDataSourceByType
        // This method is for future extensibility
        LOGGER.debug("Registered default data source providers");
    }

    /**
     * Check if a type is a built-in type.
     */
    private boolean isBuiltInType(DataSourceType type) {
        return type == DataSourceType.DATABASE ||
               type == DataSourceType.REST_API ||
               type == DataSourceType.FILE_SYSTEM ||
               type == DataSourceType.CACHE ||
               type == DataSourceType.MESSAGE_QUEUE;
    }

    /**
     * Generate cache key for JDBC DataSource.
     */
    private String generateJdbcCacheKey(DataSourceConfiguration configuration) {
        StringBuilder key = new StringBuilder();
        key.append(configuration.getSourceType()).append(":");

        if (configuration.getConnection() != null) {
            key.append(configuration.getConnection().getHost()).append(":");
            key.append(configuration.getConnection().getPort()).append(":");
            key.append(configuration.getConnection().getDatabase()).append(":");
            key.append(configuration.getConnection().getUsername());
        }

        return key.toString();
    }

    /**
     * Generate unique key for data source creation deduplication.
     * This key should uniquely identify configurations that would result in identical data sources.
     */
    private String generateCreationKey(DataSourceConfiguration configuration) {
        StringBuilder key = new StringBuilder();
        key.append(configuration.getName()).append(":");
        key.append(configuration.getType()).append(":");
        key.append(configuration.getSourceType()).append(":");

        if (configuration.getConnection() != null) {
            key.append(configuration.getConnection().getHost()).append(":");
            key.append(configuration.getConnection().getPort()).append(":");
            key.append(configuration.getConnection().getDatabase()).append(":");
            key.append(configuration.getConnection().getUsername()).append(":");
            key.append(configuration.getConnection().getBaseUrl()).append(":");
            key.append(configuration.getConnection().getBasePath());
        }

        if (configuration.getCache() != null) {
            key.append(":cache:").append(configuration.getCache().getMaxSize())
               .append(":").append(configuration.getCache().getTtlSeconds());
        }

        return key.toString();
    }

    /**
     * Generate cache key for HttpClient.
     */
    private String generateHttpCacheKey(DataSourceConfiguration configuration) {
        StringBuilder key = new StringBuilder();
        key.append("http:");

        if (configuration.getConnection() != null) {
            key.append(configuration.getConnection().getBaseUrl()).append(":");
            key.append(configuration.getConnection().getTimeout()).append(":");
            key.append(configuration.getConnection().isSslEnabled());
        }

        return key.toString();
    }
}
