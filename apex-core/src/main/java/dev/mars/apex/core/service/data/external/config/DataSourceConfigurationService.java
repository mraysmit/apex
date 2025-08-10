package dev.mars.apex.core.service.data.external.config;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlDataSourceLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import dev.mars.apex.core.service.data.external.manager.DataSourceManagerEvent;
import dev.mars.apex.core.service.data.external.manager.DataSourceManagerListener;
import dev.mars.apex.core.service.data.external.registry.DataSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for managing data source configurations and their lifecycle.
 * 
 * This service provides a high-level API for managing data source configurations,
 * integrating YAML configuration loading with the data source management layer.
 * 
 * Features:
 * - Configuration lifecycle management
 * - YAML integration
 * - Event-driven notifications
 * - Configuration validation
 * - Hot reloading support
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class DataSourceConfigurationService implements DataSourceManagerListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfigurationService.class);
    
    // Singleton instance
    private static volatile DataSourceConfigurationService instance;
    private static final Object LOCK = new Object();
    
    // Core components
    private final YamlDataSourceLoader yamlLoader;
    private final DataSourceManager dataSourceManager;
    private final DataSourceRegistry dataSourceRegistry;
    
    // Configuration tracking
    private final ConcurrentMap<String, DataSourceConfiguration> configurations = new ConcurrentHashMap<>();
    private volatile YamlRuleConfiguration currentYamlConfig;
    
    // Event listeners
    private final List<DataSourceConfigurationListener> listeners = new java.util.ArrayList<>();
    
    /**
     * Private constructor for singleton pattern.
     */
    private DataSourceConfigurationService() {
        this.dataSourceManager = new DataSourceManager();
        this.dataSourceRegistry = DataSourceRegistry.getInstance();
        this.yamlLoader = new YamlDataSourceLoader(dataSourceManager);
        
        // Register as a manager listener
        dataSourceManager.addListener(this);
    }
    
    /**
     * Get the singleton instance of the service.
     * 
     * @return The service instance
     */
    public static DataSourceConfigurationService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DataSourceConfigurationService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize the service with YAML configuration.
     * 
     * @param yamlConfig The YAML rule configuration
     * @throws DataSourceException if initialization fails
     */
    public void initialize(YamlRuleConfiguration yamlConfig) throws DataSourceException {
        LOGGER.info("Initializing DataSourceConfigurationService");
        
        try {
            // Store current configuration
            this.currentYamlConfig = yamlConfig;
            
            // Load data sources from YAML
            yamlLoader.loadDataSources(yamlConfig);
            
            // Update configuration tracking
            updateConfigurationTracking();
            
            LOGGER.info("DataSourceConfigurationService initialized successfully");
            
            // Notify listeners
            notifyListeners(DataSourceConfigurationEvent.initialized(configurations.size()));
            
        } catch (DataSourceException e) {
            LOGGER.error("Failed to initialize DataSourceConfigurationService", e);
            throw e;
        }
    }
    
    /**
     * Add a data source configuration.
     * 
     * @param configuration The data source configuration
     * @throws DataSourceException if adding fails
     */
    public void addConfiguration(DataSourceConfiguration configuration) throws DataSourceException {
        if (configuration == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Configuration cannot be null");
        }
        
        String name = configuration.getName();
        
        try {
            // Add to manager
            dataSourceManager.addDataSource(configuration);
            
            // Track configuration
            configurations.put(name, configuration);
            
            LOGGER.info("Added data source configuration '{}'", name);
            
            // Notify listeners
            notifyListeners(DataSourceConfigurationEvent.configurationAdded(name, configuration));
            
        } catch (DataSourceException e) {
            LOGGER.error("Failed to add data source configuration '{}'", name, e);
            throw e;
        }
    }
    
    /**
     * Remove a data source configuration.
     * 
     * @param name The name of the configuration to remove
     * @return true if the configuration was removed
     */
    public boolean removeConfiguration(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Remove from manager
            boolean removed = dataSourceManager.removeDataSource(name);
            
            if (removed) {
                // Remove from tracking
                DataSourceConfiguration removedConfig = configurations.remove(name);
                
                LOGGER.info("Removed data source configuration '{}'", name);
                
                // Notify listeners
                if (removedConfig != null) {
                    notifyListeners(DataSourceConfigurationEvent.configurationRemoved(name, removedConfig));
                }
            }
            
            return removed;
            
        } catch (Exception e) {
            LOGGER.error("Error removing data source configuration '{}'", name, e);
            return false;
        }
    }
    
    /**
     * Get a data source configuration by name.
     * 
     * @param name The name of the configuration
     * @return The configuration, or null if not found
     */
    public DataSourceConfiguration getConfiguration(String name) {
        return configurations.get(name);
    }
    
    /**
     * Get all configuration names.
     * 
     * @return Set of configuration names
     */
    public Set<String> getConfigurationNames() {
        return configurations.keySet();
    }
    
    /**
     * Get a data source instance by name.
     * 
     * @param name The name of the data source
     * @return The data source instance, or null if not found
     */
    public ExternalDataSource getDataSource(String name) {
        return dataSourceManager.getDataSource(name);
    }
    
    /**
     * Get the data source manager.
     * 
     * @return The data source manager
     */
    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
    }
    
    /**
     * Reload configurations from YAML.
     * 
     * @param yamlConfig The new YAML configuration
     * @throws DataSourceException if reloading fails
     */
    public void reloadFromYaml(YamlRuleConfiguration yamlConfig) throws DataSourceException {
        LOGGER.info("Reloading data source configurations from YAML");
        
        try {
            // Store new configuration
            this.currentYamlConfig = yamlConfig;
            
            // Reload data sources
            yamlLoader.reloadDataSources(yamlConfig);
            
            // Update configuration tracking
            updateConfigurationTracking();
            
            LOGGER.info("Successfully reloaded data source configurations from YAML");
            
            // Notify listeners
            notifyListeners(DataSourceConfigurationEvent.reloaded(configurations.size()));
            
        } catch (DataSourceException e) {
            LOGGER.error("Failed to reload data source configurations from YAML", e);
            throw e;
        }
    }
    
    /**
     * Validate a YAML data source configuration.
     * 
     * @param yamlDataSource The YAML data source to validate
     * @throws DataSourceException if validation fails
     */
    public void validateYamlConfiguration(YamlDataSource yamlDataSource) throws DataSourceException {
        yamlLoader.validateConfiguration(yamlDataSource);
    }
    
    /**
     * Validate all YAML data source configurations.
     * 
     * @param yamlDataSources List of YAML data sources to validate
     * @throws DataSourceException if any validation fails
     */
    public void validateYamlConfigurations(List<YamlDataSource> yamlDataSources) throws DataSourceException {
        yamlLoader.validateConfigurations(yamlDataSources);
    }
    
    /**
     * Get the current YAML configuration.
     * 
     * @return The current YAML configuration
     */
    public YamlRuleConfiguration getCurrentYamlConfiguration() {
        return currentYamlConfig;
    }
    
    /**
     * Check if the service is initialized.
     * 
     * @return true if the service is initialized
     */
    public boolean isInitialized() {
        return yamlLoader.isInitialized();
    }
    
    /**
     * Check if the service is running.
     * 
     * @return true if the service is running
     */
    public boolean isRunning() {
        return yamlLoader.isRunning();
    }
    
    /**
     * Add a configuration event listener.
     * 
     * @param listener The listener to add
     */
    public void addListener(DataSourceConfigurationListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
    }
    
    /**
     * Remove a configuration event listener.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(DataSourceConfigurationListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }
    
    /**
     * Shutdown the service and all managed resources.
     */
    public void shutdown() {
        LOGGER.info("Shutting down DataSourceConfigurationService");
        
        // Shutdown YAML loader
        yamlLoader.shutdown();
        
        // Clear state
        configurations.clear();
        currentYamlConfig = null;
        
        synchronized (listeners) {
            listeners.clear();
        }
        
        LOGGER.info("DataSourceConfigurationService shut down");
    }
    
    // DataSourceManagerListener implementation
    
    @Override
    public void onManagerEvent(DataSourceManagerEvent event) {
        // Forward manager events as configuration events
        switch (event.getEventType()) {
            case INITIALIZED:
                // Manager initialization - no specific action needed
                break;

            case SHUTDOWN:
                // Manager shutdown - no specific action needed
                break;

            case DATA_SOURCE_ADDED:
                // Configuration tracking is updated elsewhere
                break;

            case DATA_SOURCE_REMOVED:
                // Configuration tracking is updated elsewhere
                break;

            case HEALTH_RESTORED:
            case HEALTH_LOST:
                // Forward health events
                DataSourceConfigurationEvent configEvent = event.isHealthImprovement() ?
                    DataSourceConfigurationEvent.healthRestored(event.getDataSourceName()) :
                    DataSourceConfigurationEvent.healthLost(event.getDataSourceName());

                notifyListeners(configEvent);
                break;

            case REFRESH_COMPLETED:
                // Refresh operation completed - no specific action needed
                break;
        }
    }
    
    // Private helper methods
    
    /**
     * Update configuration tracking from current data sources.
     */
    private void updateConfigurationTracking() {
        configurations.clear();
        
        for (String name : dataSourceRegistry.getDataSourceNames()) {
            ExternalDataSource dataSource = dataSourceRegistry.getDataSource(name);
            if (dataSource != null && dataSource.getConfiguration() != null) {
                configurations.put(name, dataSource.getConfiguration());
            }
        }
    }
    
    /**
     * Notify all listeners of a configuration event.
     */
    private void notifyListeners(DataSourceConfigurationEvent event) {
        List<DataSourceConfigurationListener> currentListeners;
        synchronized (listeners) {
            currentListeners = new java.util.ArrayList<>(listeners);
        }
        
        for (DataSourceConfigurationListener listener : currentListeners) {
            try {
                listener.onConfigurationEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error notifying configuration listener", e);
            }
        }
    }
}
