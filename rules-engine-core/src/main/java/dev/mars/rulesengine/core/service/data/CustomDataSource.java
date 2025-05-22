package dev.mars.rulesengine.core.service.data;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom data source implementation that allows dynamic data modification.
 * This class is useful for demonstration and testing purposes.
 */
public class CustomDataSource implements DataSource {
    private final String name;
    private final String dataType;
    private final Map<String, Object> dataStore = new HashMap<>();

    /**
     * Create a new CustomDataSource with the specified name and data type.
     *
     * @param name The name of the data source
     * @param dataType The type of data this source provides
     */
    public CustomDataSource(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public boolean supportsDataType(String dataType) {
        // Handle null dataType gracefully
        if (dataType == null) {
            return false;
        }
        return dataType.equals(this.dataType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getData(String dataType, Object... parameters) {
        if (!supportsDataType(dataType)) {
            return null;
        }

        return (T) dataStore.get(dataType);
    }

    /**
     * Add or update data in this data source.
     *
     * @param dataType The type of data to add or update
     * @param data The data to store
     */
    public void addData(String dataType, Object data) {
        if (supportsDataType(dataType)) {
            dataStore.put(dataType, data);
        }
    }

    /**
     * Remove data from this data source.
     *
     * @param dataType The type of data to remove
     */
    public void removeData(String dataType) {
        if (supportsDataType(dataType)) {
            dataStore.remove(dataType);
        }
    }

    /**
     * Clear all data from this data source.
     */
    public void clearData() {
        dataStore.clear();
    }
}
