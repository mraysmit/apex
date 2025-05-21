package com.rulesengine.core.service.data;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manager for data sources.
 * This class maintains an internal configuration of data sources and provides
 * methods to load, register, and request data from them.
 */
public class DataServiceManager {
    private static final Logger LOGGER = Logger.getLogger(DataServiceManager.class.getName());

    // Map of data sources by name
    private final Map<String, DataSource> dataSourcesByName = new HashMap<>();

    // Map of data sources by data type
    private final Map<String, DataSource> dataSourcesByType = new HashMap<>();

    /**
     * Create a new DataServiceManager.
     */
    public DataServiceManager() {
        // Initialize with default configuration if needed
    }

    /**
     * Load a data source.
     * 
     * @param dataSource The data source to load
     * @return This manager for method chaining
     */
    public DataServiceManager loadDataSource(DataSource dataSource) {
        if (dataSource == null) {
            LOGGER.warning("Attempted to load null data source");
            return this;
        }

        String name = dataSource.getName();
        String dataType = dataSource.getDataType();

        if (name == null || name.isEmpty()) {
            LOGGER.warning("Data source has no name, ignoring");
            return this;
        }

        if (dataType == null || dataType.isEmpty()) {
            LOGGER.warning("Data source has no data type, ignoring");
            return this;
        }

        // Register the data source by name and type
        dataSourcesByName.put(name, dataSource);
        dataSourcesByType.put(dataType, dataSource);

        LOGGER.info("Loaded data source: " + name + " (type: " + dataType + ")");
        return this;
    }

    /**
     * Load multiple data sources.
     * 
     * @param dataSources The data sources to load
     * @return This manager for method chaining
     */
    public DataServiceManager loadDataSources(DataSource... dataSources) {
        if (dataSources == null) {
            LOGGER.warning("Attempted to load null data sources");
            return this;
        }

        for (DataSource dataSource : dataSources) {
            loadDataSource(dataSource);
        }

        return this;
    }

    /**
     * Get a data source by name.
     * 
     * @param name The name of the data source
     * @return The data source, or null if not found
     */
    public DataSource getDataSourceByName(String name) {
        return dataSourcesByName.get(name);
    }

    /**
     * Get a data source by data type.
     * 
     * @param dataType The type of data
     * @return The data source, or null if not found
     */
    public DataSource getDataSourceByType(String dataType) {
        return dataSourcesByType.get(dataType);
    }

    /**
     * Request data from a data source by name.
     * 
     * @param <T> The type of data to return
     * @param sourceName The name of the data source
     * @param dataType The type of data to request
     * @param parameters Optional parameters to filter or customize the data
     * @return The requested data, or null if the data source is not found or does not support the data type
     */
    public <T> T requestDataByName(String sourceName, String dataType, Object... parameters) {
        DataSource dataSource = getDataSourceByName(sourceName);
        if (dataSource == null) {
            LOGGER.warning("Data source not found: " + sourceName);
            return null;
        }

        if (!dataSource.supportsDataType(dataType)) {
            LOGGER.warning("Data source " + sourceName + " does not support data type: " + dataType);
            return null;
        }

        return dataSource.getData(dataType, parameters);
    }

    /**
     * Request data from a data source by data type.
     * 
     * @param <T> The type of data to return
     * @param dataType The type of data to request
     * @param parameters Optional parameters to filter or customize the data
     * @return The requested data, or null if no data source supports the data type
     */
    public <T> T requestData(String dataType, Object... parameters) {
        DataSource dataSource = getDataSourceByType(dataType);
        if (dataSource == null) {
            LOGGER.warning("No data source found for data type: " + dataType);
            return null;
        }

        return dataSource.getData(dataType, parameters);
    }

    /**
     * Initialize with default mock data sources.
     * This is a convenience method for quick setup with test data.
     * 
     * @return This manager for method chaining
     */
    public DataServiceManager initializeWithMockData() {
        // This method should be overridden by subclasses to provide specific mock data
        // The default implementation does nothing
        return this;
    }
}
