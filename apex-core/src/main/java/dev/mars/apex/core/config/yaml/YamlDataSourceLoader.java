package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader for creating and managing data sources from YAML configuration.
 * 
 * This class bridges the YAML configuration system with the data source
 * management layer, providing seamless integration between configuration
 * and runtime data source instances.
 * 
 * Features:
 * - YAML to DataSourceConfiguration conversion
 * - Automatic data source creation and registration
 * - Configuration validation and error handling
 * - Integration with DataSourceManager
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class YamlDataSourceLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlDataSourceLoader.class);
    
    private final DataSourceManager dataSourceManager;
    
    /**
     * Constructor with default data source manager.
     */
    public YamlDataSourceLoader() {
        this(new DataSourceManager());
    }
    
    /**
     * Constructor with custom data source manager.
     * 
     * @param dataSourceManager The data source manager to use
     */
    public YamlDataSourceLoader(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }
    
    /**
     * Load data sources from YAML rule configuration.
     * 
     * @param yamlConfig The YAML rule configuration
     * @throws DataSourceException if loading fails
     */
    public void loadDataSources(YamlRuleConfiguration yamlConfig) throws DataSourceException {
        if (yamlConfig == null) {
            LOGGER.warn("No YAML configuration provided for data source loading");
            return;
        }
        
        List<YamlDataSource> yamlDataSources = yamlConfig.getDataSources();
        if (yamlDataSources == null || yamlDataSources.isEmpty()) {
            LOGGER.info("No data sources defined in YAML configuration");
            return;
        }
        
        LOGGER.info("Loading {} data sources from YAML configuration", yamlDataSources.size());
        
        try {
            // Convert YAML data sources to configurations
            List<DataSourceConfiguration> configurations = convertToConfigurations(yamlDataSources);
            
            // Initialize the data source manager
            dataSourceManager.initialize(configurations);
            
            LOGGER.info("Successfully loaded {} data sources from YAML configuration", 
                configurations.size());
            
        } catch (DataSourceException e) {
            LOGGER.error("Failed to load data sources from YAML configuration", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error loading data sources from YAML configuration", e);
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to load data sources from YAML configuration", e);
        }
    }
    
    /**
     * Add a single data source from YAML configuration.
     * 
     * @param yamlDataSource The YAML data source configuration
     * @throws DataSourceException if adding fails
     */
    public void addDataSource(YamlDataSource yamlDataSource) throws DataSourceException {
        if (yamlDataSource == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "YAML data source configuration cannot be null");
        }
        
        try {
            DataSourceConfiguration configuration = convertToConfiguration(yamlDataSource);
            dataSourceManager.addDataSource(configuration);
            
            LOGGER.info("Successfully added data source '{}' from YAML configuration", 
                configuration.getName());
            
        } catch (DataSourceException e) {
            LOGGER.error("Failed to add data source from YAML configuration", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error adding data source from YAML configuration", e);
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to add data source from YAML configuration", e);
        }
    }
    
    /**
     * Remove a data source by name.
     * 
     * @param name The name of the data source to remove
     * @return true if the data source was removed
     */
    public boolean removeDataSource(String name) {
        boolean removed = dataSourceManager.removeDataSource(name);
        if (removed) {
            LOGGER.info("Removed data source '{}' from manager", name);
        } else {
            LOGGER.warn("Data source '{}' not found for removal", name);
        }
        return removed;
    }
    
    /**
     * Reload data sources from YAML configuration.
     * 
     * This method shuts down existing data sources and reloads them
     * from the provided configuration.
     * 
     * @param yamlConfig The YAML rule configuration
     * @throws DataSourceException if reloading fails
     */
    public void reloadDataSources(YamlRuleConfiguration yamlConfig) throws DataSourceException {
        LOGGER.info("Reloading data sources from YAML configuration");
        
        try {
            // Shutdown existing data sources
            dataSourceManager.shutdown();
            
            // Load new configuration
            loadDataSources(yamlConfig);
            
            LOGGER.info("Successfully reloaded data sources from YAML configuration");
            
        } catch (DataSourceException e) {
            LOGGER.error("Failed to reload data sources from YAML configuration", e);
            throw e;
        }
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
     * Check if the loader is initialized.
     * 
     * @return true if the data source manager is initialized
     */
    public boolean isInitialized() {
        return dataSourceManager.isInitialized();
    }
    
    /**
     * Check if the loader is running.
     * 
     * @return true if the data source manager is running
     */
    public boolean isRunning() {
        return dataSourceManager.isRunning();
    }
    
    /**
     * Shutdown the loader and all managed data sources.
     */
    public void shutdown() {
        LOGGER.info("Shutting down YAML data source loader");
        dataSourceManager.shutdown();
    }
    
    /**
     * Convert YAML data sources to data source configurations.
     * 
     * @param yamlDataSources List of YAML data sources
     * @return List of data source configurations
     * @throws DataSourceException if conversion fails
     */
    private List<DataSourceConfiguration> convertToConfigurations(List<YamlDataSource> yamlDataSources) 
            throws DataSourceException {
        
        List<DataSourceConfiguration> configurations = new ArrayList<>();
        
        for (YamlDataSource yamlDataSource : yamlDataSources) {
            try {
                DataSourceConfiguration configuration = convertToConfiguration(yamlDataSource);
                configurations.add(configuration);
            } catch (Exception e) {
                String name = yamlDataSource.getName() != null ? yamlDataSource.getName() : "unknown";
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "Failed to convert YAML data source '" + name + "' to configuration", e);
            }
        }
        
        return configurations;
    }
    
    /**
     * Convert a single YAML data source to data source configuration.
     * 
     * @param yamlDataSource The YAML data source
     * @return Data source configuration
     * @throws DataSourceException if conversion fails
     */
    private DataSourceConfiguration convertToConfiguration(YamlDataSource yamlDataSource) 
            throws DataSourceException {
        
        try {
            // Use the conversion method from YamlDataSource
            DataSourceConfiguration configuration = yamlDataSource.toDataSourceConfiguration();
            
            // Validate the configuration
            configuration.validate();
            
            return configuration;
            
        } catch (IllegalArgumentException e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Invalid YAML data source configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to convert YAML data source to configuration", e);
        }
    }
    
    /**
     * Validate YAML data source configuration.
     * 
     * @param yamlDataSource The YAML data source to validate
     * @throws DataSourceException if validation fails
     */
    public void validateConfiguration(YamlDataSource yamlDataSource) throws DataSourceException {
        if (yamlDataSource == null) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "YAML data source configuration cannot be null");
        }
        
        try {
            // Convert to configuration (this will validate)
            convertToConfiguration(yamlDataSource);
            
            LOGGER.debug("YAML data source configuration '{}' is valid", yamlDataSource.getName());
            
        } catch (DataSourceException e) {
            LOGGER.error("YAML data source configuration '{}' is invalid: {}", 
                yamlDataSource.getName(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Validate all YAML data source configurations.
     * 
     * @param yamlDataSources List of YAML data sources to validate
     * @throws DataSourceException if any validation fails
     */
    public void validateConfigurations(List<YamlDataSource> yamlDataSources) throws DataSourceException {
        if (yamlDataSources == null || yamlDataSources.isEmpty()) {
            return;
        }
        
        for (YamlDataSource yamlDataSource : yamlDataSources) {
            validateConfiguration(yamlDataSource);
        }
        
        LOGGER.info("All {} YAML data source configurations are valid", yamlDataSources.size());
    }
}
