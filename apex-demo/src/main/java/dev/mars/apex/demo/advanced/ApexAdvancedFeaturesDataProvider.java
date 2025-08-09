package dev.mars.apex.demo.advanced;

import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.demo.data.MockDataSource;

import java.util.*;
import java.util.logging.Logger;

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
 * Data provider for the ApexAdvancedFeaturesDemo.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Data provider for the ApexAdvancedFeaturesDemo.
 * This class uses MockDataSource to provide example data for the demo.
 */
class ApexAdvancedFeaturesDataProvider {
    private static final Logger LOGGER = Logger.getLogger(ApexAdvancedFeaturesDataProvider.class.getName());

    private final Map<String, Object> dataStore = new HashMap<>();
    private final List<DataSource> dataSources = new ArrayList<>();

    /**
     * Create a new ApexAdvancedFeaturesDataProvider.
     */
    public ApexAdvancedFeaturesDataProvider() {
        initializeDataSources();
        initializeData();
    }

    /**
     * Initialize data sources.
     */
    private void initializeDataSources() {
        LOGGER.info("Initializing data sources");
        dataSources.add(new MockDataSource("ProductsDataSource", "products"));
        dataSources.add(new MockDataSource("InventoryDataSource", "inventory"));
        dataSources.add(new MockDataSource("CustomerDataSource", "customer"));
        dataSources.add(new MockDataSource("TemplateCustomerDataSource", "templateCustomer"));
        dataSources.add(new MockDataSource("LookupServicesDataSource", "lookupServices"));
        dataSources.add(new MockDataSource("SourceRecordsDataSource", "sourceRecords"));
    }

    /**
     * Initialize data from data sources.
     */
    private void initializeData() {
        LOGGER.info("Initializing data from data sources");
        for (DataSource dataSource : dataSources) {
            String dataType = dataSource.getDataType();
            Object data = dataSource.getData(dataType);
            if (data != null) {
                dataStore.put(dataType, data);
            }
        }
    }

    /**
     * Get data of the specified type.
     *
     * @param dataType The type of data to get
     * @param <T> The type of the data
     * @return The data, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType) {
        return (T) dataStore.get(dataType);
    }
}
