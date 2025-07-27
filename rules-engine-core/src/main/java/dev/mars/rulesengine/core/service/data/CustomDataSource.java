package dev.mars.rulesengine.core.service.data;

import java.util.HashMap;
import java.util.Map;

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

/**
 * A custom data source implementation that allows dynamic data modification.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
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
