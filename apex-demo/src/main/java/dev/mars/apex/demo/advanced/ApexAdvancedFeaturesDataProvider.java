package dev.mars.apex.demo.advanced;

import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;

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
public class ApexAdvancedFeaturesDataProvider {
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
        dataSources.add(new DemoDataSource("ProductsDataSource", "products"));
        dataSources.add(new DemoDataSource("InventoryDataSource", "inventory"));
        dataSources.add(new DemoDataSource("CustomerDataSource", "customer"));
        dataSources.add(new DemoDataSource("TemplateCustomerDataSource", "templateCustomer"));
        dataSources.add(new DemoDataSource("LookupServicesDataSource", "lookupServices"));
        dataSources.add(new DemoDataSource("SourceRecordsDataSource", "sourceRecords"));
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

    /**
     * Simple demo data source implementation to replace deprecated MockDataSource.
     * This provides the same functionality without deprecation warnings.
     */
    private static class DemoDataSource implements DataSource {
        private final String name;
        private final String dataType;
        private final Map<String, Object> dataStore = new HashMap<>();

        public DemoDataSource(String name, String dataType) {
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
            return this.dataType != null && this.dataType.equals(dataType);
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
         * Initialize demo data based on the data type.
         */
        private void initializeData() {
            if (dataType == null) {
                return;
            }

            switch (dataType) {
                case "products":
                    dataStore.put(dataType, createSampleProducts());
                    break;
                case "inventory":
                    dataStore.put(dataType, createSampleInventory());
                    break;
                case "customer":
                    dataStore.put(dataType, createSampleCustomer());
                    break;
                case "templateCustomer":
                    dataStore.put(dataType, createSampleTemplateCustomer());
                    break;
                case "lookupServices":
                    dataStore.put(dataType, createSampleLookupServices());
                    break;
                case "sourceRecords":
                    dataStore.put(dataType, createSampleSourceRecords());
                    break;
                default:
                    // No initialization for other data types
                    break;
            }
        }

        private List<Object> createSampleProducts() {
            List<Object> products = new ArrayList<>();

            Product product1 = new Product("US Treasury Bond", 550.00, "FixedIncome");
            products.add(product1);

            Product product2 = new Product("Apple Stock", 149.99, "Equity");
            products.add(product2);

            Product product3 = new Product("Gold ETF", 75.50, "ETF");
            products.add(product3);

            Product product4 = new Product("Premium Corporate Bond", 750.00, "FixedIncome");
            products.add(product4);

            Product product5 = new Product("Corporate Bond", 180.00, "ETF");
            products.add(product5);

            return products;
        }

        private List<Object> createSampleInventory() {
            List<Object> inventory = new ArrayList<>();

            // Create inventory items as Product objects with appropriate pricing
            Product item1 = new Product("Corporate Bond", 250.0, "FixedIncome");
            inventory.add(item1);

            Product item2 = new Product("Microsoft Stock", 350.0, "Equity");
            inventory.add(item2);

            Product item3 = new Product("Real Estate ETF", 125.0, "ETF");
            inventory.add(item3);

            Product item4 = new Product("Premium Bond", 1500.0, "FixedIncome");
            inventory.add(item4);

            return inventory;
        }

        private Object createSampleCustomer() {
            Customer customer = new Customer("John Doe", 35, "john.doe@example.com");
            customer.setMembershipLevel("Gold");
            customer.setBalance(15000.0);
            customer.setPreferredCategories(Arrays.asList("FixedIncome", "Equity"));
            return customer;
        }

        private Object createSampleTemplateCustomer() {
            Customer customer = new Customer("Bob Johnson", 65, "bob.johnson@example.com");
            customer.setMembershipLevel("Silver");
            customer.setBalance(25000.0);
            customer.setPreferredCategories(Arrays.asList("FixedIncome", "ETF"));
            return customer;
        }

        private List<Object> createSampleLookupServices() {
            List<Object> services = new ArrayList<>();
            Map<String, Object> service1 = new HashMap<>();
            service1.put("id", 1);
            service1.put("name", "Lookup Service 1");
            services.add(service1);

            Map<String, Object> service2 = new HashMap<>();
            service2.put("id", 2);
            service2.put("name", "Lookup Service 2");
            services.add(service2);

            return services;
        }

        private List<Object> createSampleSourceRecords() {
            List<Object> records = new ArrayList<>();
            Map<String, Object> record1 = new HashMap<>();
            record1.put("id", 1);
            record1.put("data", "Sample Record 1");
            records.add(record1);

            Map<String, Object> record2 = new HashMap<>();
            record2.put("id", 2);
            record2.put("data", "Sample Record 2");
            records.add(record2);

            return records;
        }
    }
}
