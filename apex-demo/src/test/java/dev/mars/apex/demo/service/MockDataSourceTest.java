package dev.mars.apex.demo.service;

import dev.mars.apex.core.service.data.DataSource;

import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

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
 * Test class for data source functionality.
 *
 * This class tests data source implementations using a test-specific implementation
 * that provides the same functionality as the deprecated MockDataSource.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class MockDataSourceTest {
    private TestDataSource productsDataSource;
    private TestDataSource inventoryDataSource;
    private TestDataSource customerDataSource;
    private TestDataSource templateCustomerDataSource;
    private TestDataSource lookupServicesDataSource;
    private TestDataSource sourceRecordsDataSource;
    private TestDataSource matchingRecordsDataSource;
    private TestDataSource nonMatchingRecordsDataSource;

    @BeforeEach
    public void setUp() {
        productsDataSource = new TestDataSource("ProductsDataSource", "products");
        inventoryDataSource = new TestDataSource("InventoryDataSource", "inventory");
        customerDataSource = new TestDataSource("CustomerDataSource", "customer");
        templateCustomerDataSource = new TestDataSource("TemplateCustomerDataSource", "templateCustomer");
        lookupServicesDataSource = new TestDataSource("LookupServicesDataSource", "lookupServices");
        sourceRecordsDataSource = new TestDataSource("SourceRecordsDataSource", "sourceRecords");
        matchingRecordsDataSource = new TestDataSource("MatchingRecordsDataSource", "matchingRecords");
        nonMatchingRecordsDataSource = new TestDataSource("NonMatchingRecordsDataSource", "nonMatchingRecords");
    }

    @Test
    public void testConstructor() {
        assertEquals("ProductsDataSource", productsDataSource.getName());
        assertEquals("products", productsDataSource.getDataType());
    }

    @Test
    public void testGetName() {
        assertEquals("ProductsDataSource", productsDataSource.getName());
        assertEquals("InventoryDataSource", inventoryDataSource.getName());
        assertEquals("CustomerDataSource", customerDataSource.getName());
        assertEquals("TemplateCustomerDataSource", templateCustomerDataSource.getName());
        assertEquals("LookupServicesDataSource", lookupServicesDataSource.getName());
        assertEquals("SourceRecordsDataSource", sourceRecordsDataSource.getName());
        assertEquals("MatchingRecordsDataSource", matchingRecordsDataSource.getName());
        assertEquals("NonMatchingRecordsDataSource", nonMatchingRecordsDataSource.getName());
    }

    @Test
    public void testGetDataType() {
        assertEquals("products", productsDataSource.getDataType());
        assertEquals("inventory", inventoryDataSource.getDataType());
        assertEquals("customer", customerDataSource.getDataType());
        assertEquals("templateCustomer", templateCustomerDataSource.getDataType());
        assertEquals("lookupServices", lookupServicesDataSource.getDataType());
        assertEquals("sourceRecords", sourceRecordsDataSource.getDataType());
        assertEquals("matchingRecords", matchingRecordsDataSource.getDataType());
        assertEquals("nonMatchingRecords", nonMatchingRecordsDataSource.getDataType());
    }

    @Test
    public void testSupportsDataType() {
        assertTrue(productsDataSource.supportsDataType("products"));
        assertFalse(productsDataSource.supportsDataType("inventory"));
        assertFalse(productsDataSource.supportsDataType(null));
        assertFalse(productsDataSource.supportsDataType(""));
    }

    @Test
    public void testGetDataProducts() {
        List<Product> products = productsDataSource.getData("products");
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
    public void testGetDataInventory() {
        List<Product> inventory = inventoryDataSource.getData("inventory");
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
    public void testGetDataCustomer() {
        Customer customer = customerDataSource.getData("customer");
        assertNotNull(customer);
        assertEquals("Alice Smith", customer.getName());
        assertEquals(35, customer.getAge());
        assertEquals("Gold", customer.getMembershipLevel());
    }

    @Test
    public void testGetDataTemplateCustomer() {
        Customer templateCustomer = templateCustomerDataSource.getData("templateCustomer");
        assertNotNull(templateCustomer);
        assertEquals("Bob Johnson", templateCustomer.getName());
        assertEquals(65, templateCustomer.getAge());
        assertEquals("Silver", templateCustomer.getMembershipLevel());
    }

    @Test
    public void testGetDataLookupServices() {
        List<Map<String, Object>> lookupServices = lookupServicesDataSource.getData("lookupServices");
        assertNotNull(lookupServices);
        assertFalse(lookupServices.isEmpty());

        // Verify some lookup service data
        boolean foundInstrumentTypes = false;
        for (Map<String, Object> lookupService : lookupServices) {
            if ("InstrumentTypes".equals(lookupService.get("name"))) {
                foundInstrumentTypes = true;
                List<String> values = (List<String>) lookupService.get("values");
                assertTrue(values.contains("Equity"));
                assertTrue(values.contains("Bond"));
                break;
            }
        }
        assertTrue(foundInstrumentTypes, "InstrumentTypes lookup service should be in the list");
    }

    @Test
    public void testGetDataSourceRecords() {
        List<Trade> sourceRecords = sourceRecordsDataSource.getData("sourceRecords");
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
    public void testGetDataMatchingRecords() {
        // Get source records and lookup services
        List<Trade> sourceRecords = sourceRecordsDataSource.getData("sourceRecords");
        List<Map<String, Object>> lookupServices = lookupServicesDataSource.getData("lookupServices");

        // Test finding matching records
        List<Trade> matchingRecords = matchingRecordsDataSource.getData("matchingRecords", sourceRecords, lookupServices);
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
    public void testGetDataNonMatchingRecords() {
        // Get source records and lookup services
        List<Trade> sourceRecords = sourceRecordsDataSource.getData("sourceRecords");
        List<Map<String, Object>> lookupServices = lookupServicesDataSource.getData("lookupServices");

        // Test finding non-matching records
        List<Trade> nonMatchingRecords = nonMatchingRecordsDataSource.getData("nonMatchingRecords", sourceRecords, lookupServices);
        assertNotNull(nonMatchingRecords);

        // Verify that non-matching records contain expected trades
        boolean foundCommodityTrade = false;
        for (Trade trade : nonMatchingRecords) {
            if ("T007".equals(trade.getId()) && "Commodity".equals(trade.getValue())) {
                foundCommodityTrade = true;
                break;
            }
        }
        assertTrue(foundCommodityTrade, "Commodity trade should be in the non-matching records list");
    }

    @Test
    public void testGetDataWithUnsupportedDataType() {
        // Test with unsupported data type
        assertNull(productsDataSource.getData("unsupportedType"));
    }

    @Test
    public void testGetDataWithInvalidParameters() {
        // Test with invalid parameters for matchingRecords
        assertNull(matchingRecordsDataSource.getData("matchingRecords"));
        assertNull(matchingRecordsDataSource.getData("matchingRecords", "invalid"));
        assertNull(matchingRecordsDataSource.getData("matchingRecords", null, null));
    }

    /**
     * Test implementation of DataSource to replace deprecated MockDataSource.
     * This provides the same functionality without deprecation warnings.
     */
    private static class TestDataSource implements DataSource {
        private final String name;
        private final String dataType;
        private final Map<String, Object> dataStore = new HashMap<>();

        public TestDataSource(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
            initializeData();
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
            if (dataType == null || dataType.isEmpty()) {
                return false;
            }
            return this.dataType.equals(dataType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getData(String dataType, Object... parameters) {
            if (!supportsDataType(dataType)) {
                return null;
            }

            // Special handling for matchingRecords and nonMatchingRecords
            if ("matchingRecords".equals(dataType) || "nonMatchingRecords".equals(dataType)) {
                if (parameters.length < 2 || !(parameters[0] instanceof List) || !(parameters[1] instanceof List)) {
                    return null;
                }

                List<Trade> sourceRecords = (List<Trade>) parameters[0];
                List<Map<String, Object>> lookupServices = (List<Map<String, Object>>) parameters[1];

                if ("matchingRecords".equals(dataType)) {
                    return (T) getMatchingRecords(sourceRecords, lookupServices);
                } else {
                    return (T) getNonMatchingRecords(sourceRecords, lookupServices);
                }
            }

            return (T) dataStore.get(dataType);
        }

        /**
         * Initialize the test data for this data source.
         */
        private void initializeData() {
            // Handle null dataType gracefully
            if (dataType == null) {
                return;
            }

            switch (dataType) {
                case "products":
                    initializeProducts();
                    break;
                case "inventory":
                    initializeInventory();
                    break;
                case "customer":
                    initializeCustomer();
                    break;
                case "templateCustomer":
                    initializeTemplateCustomer();
                    break;
                case "lookupServices":
                    initializeLookupServices();
                    break;
                case "sourceRecords":
                    initializeSourceRecords();
                    break;
                // matchingRecords and nonMatchingRecords are handled dynamically in getData()
                default:
                    // No initialization for other data types
                    break;
            }
        }

        private void initializeProducts() {
            List<Product> products = Arrays.asList(
                new Product("Laptop", 999.99, "Electronics"),
                new Product("Mouse", 29.99, "Electronics"),
                new Product("Keyboard", 79.99, "Electronics"),
                new Product("US Treasury Bond", 1200.0, "FixedIncome")
            );
            dataStore.put("products", products);
        }

        private void initializeInventory() {
            List<Product> inventory = Arrays.asList(
                new Product("Monitor", 299.99, "Electronics"),
                new Product("Speakers", 149.99, "Electronics"),
                new Product("Webcam", 89.99, "Electronics"),
                new Product("Bitcoin ETF", 450.0, "ETF")
            );
            dataStore.put("inventory", inventory);
        }

        private void initializeCustomer() {
            Customer customer = new Customer("Alice Smith", 35, "alice@example.com");
            customer.setMembershipLevel("Gold");
            dataStore.put("customer", customer);
        }

        private void initializeTemplateCustomer() {
            Customer templateCustomer = new Customer("Bob Johnson", 65, "bob@example.com");
            templateCustomer.setMembershipLevel("Silver");
            dataStore.put("templateCustomer", templateCustomer);
        }

        private void initializeLookupServices() {
            List<Map<String, Object>> lookupServices = new ArrayList<>();

            // Create InstrumentTypes lookup service
            Map<String, Object> instrumentTypes = new HashMap<>();
            instrumentTypes.put("name", "InstrumentTypes");
            instrumentTypes.put("values", Arrays.asList("Equity", "Bond", "ETF", "Option", "Future"));

            // Create AssetClasses lookup service
            Map<String, Object> assetClasses = new HashMap<>();
            assetClasses.put("name", "AssetClasses");
            assetClasses.put("values", Arrays.asList("Equity", "FixedIncome", "Currency"));

            lookupServices.add(instrumentTypes);
            lookupServices.add(assetClasses);

            dataStore.put("lookupServices", lookupServices);
        }

        private void initializeSourceRecords() {
            List<Trade> sourceRecords = new ArrayList<>();
            sourceRecords.add(new Trade("T001", "Equity", "InstrumentType"));
            sourceRecords.add(new Trade("T002", "Bond", "InstrumentType"));
            sourceRecords.add(new Trade("T003", "ETF", "InstrumentType"));
            sourceRecords.add(new Trade("T004", "Equity", "AssetClass"));
            sourceRecords.add(new Trade("T005", "FixedIncome", "AssetClass"));
            sourceRecords.add(new Trade("T006", "Currency", "AssetClass"));
            sourceRecords.add(new Trade("T007", "Commodity", "AssetClass"));
            // Add a non-matching trade
            sourceRecords.add(new Trade("T008", "NonMatchingValue", "NonMatchingCategory"));

            dataStore.put("sourceRecords", sourceRecords);
        }

        private List<Trade> getMatchingRecords(List<Trade> sourceRecords, List<Map<String, Object>> lookupServices) {
            List<Trade> matchingRecords = new ArrayList<>();
            Set<String> supportedValues = new HashSet<>();

            for (Map<String, Object> service : lookupServices) {
                List<String> values = (List<String>) service.get("values");
                if (values != null) {
                    supportedValues.addAll(values);
                }
            }

            for (Trade trade : sourceRecords) {
                if (supportedValues.contains(trade.getValue())) {
                    matchingRecords.add(trade);
                }
            }

            return matchingRecords;
        }

        private List<Trade> getNonMatchingRecords(List<Trade> sourceRecords, List<Map<String, Object>> lookupServices) {
            List<Trade> nonMatchingRecords = new ArrayList<>();
            Set<String> supportedValues = new HashSet<>();

            for (Map<String, Object> service : lookupServices) {
                List<String> values = (List<String>) service.get("values");
                if (values != null) {
                    supportedValues.addAll(values);
                }
            }

            for (Trade trade : sourceRecords) {
                if (!supportedValues.contains(trade.getValue())) {
                    nonMatchingRecords.add(trade);
                }
            }

            return nonMatchingRecords;
        }
    }
}
