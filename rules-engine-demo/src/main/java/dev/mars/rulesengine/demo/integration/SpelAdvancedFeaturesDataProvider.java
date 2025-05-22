package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.core.service.data.DataSource;
import dev.mars.rulesengine.demo.service.providers.MockDataSource;

import java.util.*;
import java.util.logging.Logger;

/**
 * Data provider for the SpelAdvancedFeaturesDemo.
 * This class uses MockDataSource to provide example data for the demo.
 */
class SpelAdvancedFeaturesDataProvider {
    private static final Logger LOGGER = Logger.getLogger(SpelAdvancedFeaturesDataProvider.class.getName());

    private final Map<String, Object> dataStore = new HashMap<>();
    private final List<DataSource> dataSources = new ArrayList<>();

    /**
     * Create a new SpelAdvancedFeaturesDataProvider.
     */
    public SpelAdvancedFeaturesDataProvider() {
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