package dev.mars.apex.core.service.data.external.factory;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.ExternalDataSource;

/**
 * Interface for custom data source providers.
 * 
 * This interface allows for extensibility by enabling custom data source
 * implementations to be registered with the DataSourceFactory.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public interface DataSourceProvider {
    
    /**
     * Create a data source instance from configuration.
     * 
     * @param configuration The data source configuration
     * @return Configured data source instance
     * @throws DataSourceException if creation fails
     */
    ExternalDataSource createDataSource(DataSourceConfiguration configuration) throws DataSourceException;
    
    /**
     * Get the type identifier for this provider.
     * 
     * @return The type identifier
     */
    String getType();
    
    /**
     * Check if this provider supports the given configuration.
     * 
     * @param configuration The configuration to check
     * @return true if this provider can handle the configuration
     */
    default boolean supports(DataSourceConfiguration configuration) {
        return configuration != null && 
               getType().equalsIgnoreCase(configuration.getType()) ||
               getType().equalsIgnoreCase(configuration.getImplementation());
    }
    
    /**
     * Get a description of this provider.
     * 
     * @return Provider description
     */
    default String getDescription() {
        return "Custom data source provider for type: " + getType();
    }
    
    /**
     * Get the version of this provider.
     * 
     * @return Provider version
     */
    default String getVersion() {
        return "1.0.0";
    }
}
