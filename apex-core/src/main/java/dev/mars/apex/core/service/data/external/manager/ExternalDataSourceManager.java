package dev.mars.apex.core.service.data.external.manager;

import dev.mars.apex.core.service.data.external.ExternalDataSource;

/**
 * Manager interface for external data sources.
 * Provides access to configured data sources by name.
 * 
 * @author APEX Team
 * @since 1.0.0
 */
public interface ExternalDataSourceManager {
    
    /**
     * Get a data source by name.
     * 
     * @param name The name of the data source
     * @return The data source, or null if not found
     */
    ExternalDataSource getDataSource(String name);
    
    /**
     * Add a data source.
     * 
     * @param name The name of the data source
     * @param dataSource The data source instance
     */
    void addDataSource(String name, ExternalDataSource dataSource);
    
    /**
     * Remove a data source.
     * 
     * @param name The name of the data source to remove
     */
    void removeDataSource(String name);
    
    /**
     * Check if a data source exists.
     * 
     * @param name The name of the data source
     * @return true if the data source exists, false otherwise
     */
    boolean hasDataSource(String name);
}
