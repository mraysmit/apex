package dev.mars.apex.demo.data;

import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.demo.data.MockDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * A demo implementation of DataServiceManager that initializes with mock data.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * A demo implementation of DataServiceManager that initializes with mock data.
 * This class is used for testing and demonstration purposes.
 *
 * NOTE: This class uses MockDataSource which is now located in the test package.
 * For production scenarios, consider using actual ExternalDataSource implementations.
 */
public class DemoDataServiceManager extends DataServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataServiceManager.class);

    /**
     * Initialize with default mock data sources.
     * This method creates and loads MockDataSource instances for various data types.
     *
     * @return This manager for method chaining
     */
    @Override
    public DataServiceManager initializeWithMockData() {
        logger.info("Initializing DemoDataServiceManager with mock data sources");

        // Create and load mock data sources for various data types
        loadDataSource(new MockDataSource("ProductsDataSource", "products"));
        loadDataSource(new MockDataSource("InventoryDataSource", "inventory"));
        loadDataSource(new MockDataSource("CustomerDataSource", "customer"));
        loadDataSource(new MockDataSource("TemplateCustomerDataSource", "templateCustomer"));
        loadDataSource(new MockDataSource("LookupServicesDataSource", "lookupServices"));
        loadDataSource(new MockDataSource("SourceRecordsDataSource", "sourceRecords"));

        // Add data sources for matchingRecords and nonMatchingRecords
        // These are handled dynamically in MockDataSource.getData()
        loadDataSource(new MockDataSource("MatchingRecordsDataSource", "matchingRecords"));
        loadDataSource(new MockDataSource("NonMatchingRecordsDataSource", "nonMatchingRecords"));

        logger.info("Mock data sources initialized successfully");
        return this;
    }
}
