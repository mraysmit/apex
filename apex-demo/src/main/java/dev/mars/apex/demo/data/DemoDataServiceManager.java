package dev.mars.apex.demo.data;

import dev.mars.apex.core.service.data.DataServiceManager;
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
 * This class is used for testing and demonstration purposes.
 *
 * ⚠️ LEGACY DEMO CLASS - DEPRECATED ⚠️
 * This class uses the deprecated MockDataSource and should be replaced with
 * ProductionDemoDataServiceManager for realistic demonstrations.
 *
 * @deprecated Use ProductionDemoDataServiceManager with ExternalDataSource implementations instead
 * @see ProductionDemoDataServiceManager
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@Deprecated
public class DemoDataServiceManager extends DataServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataServiceManager.class);

    /**
     * Initialize with default mock data sources.
     * This method creates and loads MockDataSource instances for various data types.
     *
     * ⚠️ DEPRECATED - Use ProductionDemoDataServiceManager instead ⚠️
     * This method uses deprecated MockDataSource for backward compatibility only.
     *
     * @deprecated Use ProductionDemoDataServiceManager.initializeWithMockData() instead
     * @return This manager for method chaining
     */
    @Override
    @Deprecated
    public DataServiceManager initializeWithMockData() {
        logger.warn("DemoDataServiceManager is deprecated. Use ProductionDemoDataServiceManager instead.");
        logger.info("Initializing DemoDataServiceManager with deprecated mock data sources");

        // Create and load mock data sources for various data types
        // Note: Using deprecated MockDataSource for backward compatibility only
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
        logger.warn("Consider migrating to ProductionDemoDataServiceManager for production-ready demonstrations");
        return this;
    }
}
