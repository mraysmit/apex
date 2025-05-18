package com.rulesengine.core.service.data;

/**
 * Interface for data sources that can provide various types of data.
 * This interface abstracts away the concrete implementation of data sources,
 * allowing for different data sources to be used interchangeably.
 */
public interface DataSource {
    /**
     * Get the name of this data source.
     * 
     * @return The name of the data source
     */
    String getName();

    /**
     * Get the type of data this source provides.
     * 
     * @return The type of data (e.g., "products", "customers", "trades")
     */
    String getDataType();

    /**
     * Check if this data source can provide the specified data type.
     * 
     * @param dataType The type of data to check for
     * @return True if this data source can provide the specified data type, false otherwise
     */
    boolean supportsDataType(String dataType);

    /**
     * Get data from this source.
     * 
     * @param <T> The type of data to return
     * @param dataType The type of data to get
     * @param parameters Optional parameters to filter or customize the data
     * @return The requested data, or null if the data type is not supported
     */
    <T> T getData(String dataType, Object... parameters);
}