package dev.mars.apex.demo.service;

import dev.mars.apex.core.service.data.CustomDataSource;
import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.demo.test.data.TestDataSource;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
 * Test class for DataServiceManager.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for DataServiceManager.
 */
public class DataServiceManagerTest {
    private ProductionDemoDataServiceManager dataServiceManager;

    @BeforeEach
    public void setUp() {
        dataServiceManager = new ProductionDemoDataServiceManager();
        dataServiceManager.initializeWithMockData();
    }

    @Test
    public void testGetProductsData() {
        // Test requesting data by type
        List<Product> products = dataServiceManager.requestData("products");
        assertNotNull(products);
        assertFalse(products.isEmpty());

        // Verify some product data
        boolean foundTreasuryBond = false;
        for (Product product : products) {
            if ("US Treasury Bond".equals(product.getName())) {
                foundTreasuryBond = true;
                assertEquals(1200.0, product.getPrice());
                assertEquals("FixedIncome", product.getCategory());
                break;
            }
        }
        assertTrue(foundTreasuryBond, "US Treasury Bond should be in the products list");
    }

    @Test
    public void testGetInventoryData() {
        // Test requesting data by name
        List<Product> inventory = dataServiceManager.requestDataByName("InventoryDataSource", "inventory");
        assertNotNull(inventory);
        assertFalse(inventory.isEmpty());

        // Verify some inventory data
        boolean foundBitcoinETF = false;
        for (Product product : inventory) {
            if ("Bitcoin ETF".equals(product.getName())) {
                foundBitcoinETF = true;
                assertEquals(450.0, product.getPrice());
                assertEquals("ETF", product.getCategory());
                break;
            }
        }
        assertTrue(foundBitcoinETF, "Bitcoin ETF should be in the inventory list");
    }

    @Test
    public void testGetCustomerData() {
        // Test requesting customer data
        Customer customer = dataServiceManager.requestData("customer");
        assertNotNull(customer);
        assertEquals("Alice Smith", customer.getName());
        assertEquals(35, customer.getAge());
        assertEquals("Gold", customer.getMembershipLevel());

        // Test requesting template customer data
        Customer templateCustomer = dataServiceManager.requestData("templateCustomer");
        assertNotNull(templateCustomer);
        assertEquals("Bob Johnson", templateCustomer.getName());
        assertEquals(65, templateCustomer.getAge());
        assertEquals("Silver", templateCustomer.getMembershipLevel());
    }

    @Test
    public void testGetLookupServicesData() {
        // Test requesting lookup services data
        List<Object> lookupServices = dataServiceManager.requestData("lookupServices");
        assertNotNull(lookupServices);
        assertFalse(lookupServices.isEmpty());

        // Verify some lookup service data
        assertTrue(lookupServices.size() > 0, "Should have lookup services");
    }

    @Test
    public void testGetSourceRecordsData() {
        // Test requesting source records data
        List<Trade> sourceRecords = dataServiceManager.requestData("sourceRecords");
        assertNotNull(sourceRecords);
        assertFalse(sourceRecords.isEmpty());

        // Verify some source record data
        boolean foundEquityTrade = false;
        for (Trade trade : sourceRecords) {
            if ("T001".equals(trade.getId())) {
                foundEquityTrade = true;
                assertEquals("Equity", trade.getValue());
                assertEquals("InstrumentType", trade.getCategory());
                break;
            }
        }
        assertTrue(foundEquityTrade, "Equity trade should be in the source records list");
    }

    @Test
    public void testFindMatchingRecords() {
        // Get source records and lookup services
        List<Trade> sourceRecords = dataServiceManager.requestData("sourceRecords");
        List<Object> lookupServices = dataServiceManager.requestData("lookupServices");

        // Test finding matching records
        List<Trade> matchingRecords = dataServiceManager.requestData("matchingRecords", sourceRecords, lookupServices);
        assertNotNull(matchingRecords);
        assertFalse(matchingRecords.isEmpty());

        // Verify that matching records contain expected trades
        boolean foundEquityTrade = false;
        for (Trade trade : matchingRecords) {
            if ("T001".equals(trade.getId()) && "Equity".equals(trade.getValue())) {
                foundEquityTrade = true;
                break;
            }
        }
        assertTrue(foundEquityTrade, "Equity trade should be in the matching records list");
    }

    @Test
    public void testFindNonMatchingRecords() {
        // Get source records and lookup services
        List<Trade> sourceRecords = dataServiceManager.requestData("sourceRecords");
        List<Object> lookupServices = dataServiceManager.requestData("lookupServices");

        // Test finding non-matching records
        List<Trade> nonMatchingRecords = dataServiceManager.requestData("nonMatchingRecords", sourceRecords, lookupServices);
        assertNotNull(nonMatchingRecords);

        // Verify that non-matching records contain expected trades
        boolean foundNonMatchingTrade = false;
        for (Trade trade : nonMatchingRecords) {
            if ("T008".equals(trade.getId()) && "NonMatchingValue".equals(trade.getValue())) {
                foundNonMatchingTrade = true;
                break;
            }
        }
        assertTrue(foundNonMatchingTrade, "Non-matching trade should be in the non-matching records list");
    }

    @Test
    public void testUnsupportedDataType() {
        System.out.println("TEST: Triggering intentional error - testing unsupported data type handling");

        // Test requesting an unsupported data type
        Object result = dataServiceManager.requestData("unsupportedType");
        assertNull(result);
    }

    @Test
    public void testNonExistentDataSource() {
        System.out.println("TEST: Triggering intentional error - testing non-existent data source handling");

        // Test requesting data from a non-existent data source
        Object result = dataServiceManager.requestDataByName("NonExistentDataSource", "products");
        assertNull(result);
    }

    @Test
    public void testLoadDataSource() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source
        TestDataSource dataSource = new TestDataSource("TestDataSource", "testData");

        // Load the data source
        manager.loadDataSource(dataSource);

        // Verify the data source was loaded
        DataSource loadedDataSource = manager.getDataSourceByName("TestDataSource");
        assertNotNull(loadedDataSource);
        assertEquals("TestDataSource", loadedDataSource.getName());
        assertEquals("testData", loadedDataSource.getDataType());
    }

    @Test
    public void testLoadDataSourceWithNullDataSource() {
        // INTENTIONAL ERROR TEST: This test validates fail-fast behavior for null data sources
        // Expected: IllegalArgumentException with WARNING log message (not SEVERE)

        System.out.println("TEST: Triggering intentional error - testing null data source validation");

        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Loading a null data source should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.loadDataSource(null);
        });

        assertEquals("Data source cannot be null", exception.getMessage());
    }

    @Test
    public void testLoadDataSourceWithNullName() {
        System.out.println("TEST: Triggering intentional error - testing null data source name validation");

        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source with null name
        TestDataSource dataSource = new TestDataSource(null, "testData") {
            @Override
            public String getName() {
                return null;
            }
        };

        // Loading a data source with null name should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.loadDataSource(dataSource);
        });

        assertEquals("Data source name cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testLoadDataSourceWithEmptyName() {
        System.out.println("TEST: Triggering intentional error - testing empty data source name validation");

        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source with empty name
        TestDataSource dataSource = new TestDataSource("", "testData") {
            @Override
            public String getName() {
                return "";
            }
        };

        // Loading a data source with empty name should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.loadDataSource(dataSource);
        });

        assertEquals("Data source name cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testLoadDataSourceWithNullDataType() {
        System.out.println("TEST: Triggering intentional error - testing null data source type validation");

        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source with null data type
        TestDataSource dataSource = new TestDataSource("TestDataSource", null) {
            @Override
            public String getDataType() {
                return null;
            }
        };

        // Loading a data source with null data type should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.loadDataSource(dataSource);
        });

        assertEquals("Data source data type cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testLoadDataSourceWithEmptyDataType() {
        System.out.println("TEST: Triggering intentional error - testing empty data source type validation");

        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source with empty data type
        TestDataSource dataSource = new TestDataSource("TestDataSource", "") {
            @Override
            public String getDataType() {
                return "";
            }
        };

        // Loading a data source with empty data type should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.loadDataSource(dataSource);
        });

        assertEquals("Data source data type cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testLoadDataSources() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create test data sources
        TestDataSource dataSource1 = new TestDataSource("TestDataSource1", "testData1");
        TestDataSource dataSource2 = new TestDataSource("TestDataSource2", "testData2");

        // Load the data sources
        manager.loadDataSources(dataSource1, dataSource2);

        // Verify the data sources were loaded
        DataSource loadedDataSource1 = manager.getDataSourceByName("TestDataSource1");
        assertNotNull(loadedDataSource1);
        assertEquals("TestDataSource1", loadedDataSource1.getName());
        assertEquals("testData1", loadedDataSource1.getDataType());

        DataSource loadedDataSource2 = manager.getDataSourceByName("TestDataSource2");
        assertNotNull(loadedDataSource2);
        assertEquals("TestDataSource2", loadedDataSource2.getName());
        assertEquals("testData2", loadedDataSource2.getDataType());
    }

    @Test
    public void testLoadDataSourcesWithNullDataSources() {
        System.out.println("TEST: Triggering intentional error - testing null data sources array validation");

        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Loading null data sources array should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.loadDataSources((DataSource[]) null);
        });

        assertEquals("Data sources array cannot be null", exception.getMessage());
    }

    @Test
    public void testLoadDataSourcesWithNullDataSource() {
        System.out.println("TEST: Triggering intentional error - testing null data source in array validation");

        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source
        TestDataSource dataSource = new TestDataSource("TestDataSource", "testData");

        // Loading data sources with a null data source should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            manager.loadDataSources(dataSource, null);
        });

        assertEquals("Data source cannot be null", exception.getMessage());
    }

    @Test
    public void testGetDataSourceByName() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source
        TestDataSource dataSource = new TestDataSource("TestDataSource", "testData");

        // Load the data source
        manager.loadDataSource(dataSource);

        // Get the data source by name
        DataSource loadedDataSource = manager.getDataSourceByName("TestDataSource");
        assertNotNull(loadedDataSource);
        assertEquals("TestDataSource", loadedDataSource.getName());
        assertEquals("testData", loadedDataSource.getDataType());
    }

    @Test
    public void testGetDataSourceByNameWithNonExistentName() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Get a data source with a non-existent name
        DataSource loadedDataSource = manager.getDataSourceByName("NonExistentDataSource");
        assertNull(loadedDataSource);
    }

    @Test
    public void testGetDataSourceByType() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a test data source
        TestDataSource dataSource = new TestDataSource("TestDataSource", "testData");

        // Load the data source
        manager.loadDataSource(dataSource);

        // Get the data source by type
        DataSource loadedDataSource = manager.getDataSourceByType("testData");
        assertNotNull(loadedDataSource);
        assertEquals("TestDataSource", loadedDataSource.getName());
        assertEquals("testData", loadedDataSource.getDataType());
    }

    @Test
    public void testGetDataSourceByTypeWithNonExistentType() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Get a data source with a non-existent type
        DataSource loadedDataSource = manager.getDataSourceByType("NonExistentDataType");
        assertNull(loadedDataSource);
    }

    @Test
    public void testIntegrationWithCustomDataSource() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a custom data source
        CustomDataSource dataSource = new CustomDataSource("CustomProductsSource", "customProducts");

        // Add some initial data to the custom data source
        List<Product> initialProducts = new ArrayList<>();
        initialProducts.add(new Product("Custom Product 1", 100.0, "Custom Category"));
        initialProducts.add(new Product("Custom Product 2", 200.0, "Custom Category"));
        dataSource.addData("customProducts", initialProducts);

        // Load the data source
        manager.loadDataSource(dataSource);

        // Get the data source by name
        DataSource loadedDataSource = manager.getDataSourceByName("CustomProductsSource");
        assertNotNull(loadedDataSource);
        assertEquals("CustomProductsSource", loadedDataSource.getName());
        assertEquals("customProducts", loadedDataSource.getDataType());

        // Verify we can get data from the custom data source
        List<Product> products = manager.requestData("customProducts");
        assertNotNull(products);
        assertFalse(products.isEmpty());
    }

    @Test
    public void testDynamicDataModification() {
        // Create a new DataServiceManager
        DataServiceManager manager = new DataServiceManager();

        // Create a custom data source
        CustomDataSource dataSource = new CustomDataSource("CustomTradesSource", "customTrades");

        // Add some initial data to the custom data source
        List<Trade> initialTrades = new ArrayList<>();
        initialTrades.add(new Trade("CT001", "Custom Value 1", "Custom Category 1"));
        initialTrades.add(new Trade("CT002", "Custom Value 2", "Custom Category 2"));
        initialTrades.add(new Trade("CT003", "Custom Value 3", "Custom Category 3"));
        dataSource.addData("customTrades", initialTrades);

        // Load the data source
        manager.loadDataSource(dataSource);

        // Get the initial data
        List<Trade> loadedTrades = manager.requestData("customTrades");
        assertNotNull(loadedTrades);
        int initialSize = loadedTrades.size();

        // Get the custom data source by name
        CustomDataSource customTradesSource = (CustomDataSource) manager.getDataSourceByName("CustomTradesSource");
        assertNotNull(customTradesSource);

        // Add a new trade
        List<Trade> updatedTrades = new ArrayList<>(initialTrades);
        updatedTrades.add(new Trade("CT004", "New Custom Value", "Custom Category"));
        customTradesSource.addData("customTrades", updatedTrades);

        // Get the updated data
        List<Trade> newTrades = manager.requestData("customTrades");
        assertNotNull(newTrades);
        assertEquals(initialSize + 1, newTrades.size());

        // Verify the new trade was added
        boolean foundNewTrade = false;
        for (Trade trade : newTrades) {
            if ("CT004".equals(trade.getId())) {
                foundNewTrade = true;
                assertEquals("New Custom Value", trade.getValue());
                assertEquals("Custom Category", trade.getCategory());
                break;
            }
        }
        assertTrue(foundNewTrade, "New trade should be in the trades list");
    }
}
