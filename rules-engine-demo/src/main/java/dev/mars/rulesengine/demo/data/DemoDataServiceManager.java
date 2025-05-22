package dev.mars.rulesengine.demo.data;

import dev.mars.rulesengine.core.service.data.DataServiceManager;
import dev.mars.rulesengine.demo.service.providers.MockDataSource;

/**
 * A demo implementation of DataServiceManager that initializes with mock data.
 * This class is used for testing and demonstration purposes.
 */
public class DemoDataServiceManager extends DataServiceManager {

    /**
     * Initialize with default mock data sources.
     * This method creates and loads MockDataSource instances for various data types.
     * 
     * @return This manager for method chaining
     */
    @Override
    public DataServiceManager initializeWithMockData() {
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

        return this;
    }
}
