package dev.mars.apex.demo.test.data;

import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.demo.bootstrap.model.Customer;
import dev.mars.apex.demo.bootstrap.model.Product;
import dev.mars.apex.demo.bootstrap.model.Trade;

import java.util.*;

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
 * Test data source implementation for unit testing purposes.
 * This replaces the deprecated MockDataSource with a clean, non-deprecated implementation.
 * 
 * This class provides the same functionality as MockDataSource but without deprecation warnings.
 * It should only be used in test code.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-10
 * @version 1.0
 */
public class TestDataSource implements DataSource {
    private final String name;
    private final String dataType;
    private final Map<String, Object> dataStore = new HashMap<>();

    /**
     * Create a new TestDataSource with the specified name and data type.
     *
     * @param name The name of the data source
     * @param dataType The type of data this source provides
     */
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType, Object... parameters) {
        // Handle special cases like dynamic matching
        if ("matchingRecords".equals(dataType) || "nonMatchingRecords".equals(dataType)) {
            return (T) processMatchingLogic(parameters);
        }

        return (T) dataStore.get(dataType);
    }

    /**
     * Initialize the data store with sample data based on the data type.
     */
    private void initializeData() {
        if (dataType == null) {
            return; // No initialization for null data type
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
            case "matchingRecords":
            case "nonMatchingRecords":
                // These are handled dynamically in getData()
                break;
            default:
                // For unknown data types, create a simple map
                dataStore.put(dataType, createDefaultData());
                break;
        }
    }

    private void initializeProducts() {
        List<Product> products = Arrays.asList(
            new Product("Laptop", 999.99, "Electronics"),
            new Product("Mouse", 29.99, "Electronics"),
            new Product("Keyboard", 79.99, "Electronics"),
            new Product("Monitor", 299.99, "Electronics"),
            new Product("Headphones", 149.99, "Electronics")
        );
        dataStore.put("products", products);
    }

    private void initializeInventory() {
        Map<String, Integer> inventory = new HashMap<>();
        inventory.put("P001", 50);
        inventory.put("P002", 100);
        inventory.put("P003", 75);
        inventory.put("P004", 25);
        inventory.put("P005", 60);
        dataStore.put("inventory", inventory);
    }

    private void initializeCustomer() {
        Customer customer = new Customer("John Doe", 35, "john.doe@example.com");
        customer.setPreferredCategories(Arrays.asList("Electronics", "Books"));
        dataStore.put("customer", customer);
    }

    private void initializeTemplateCustomer() {
        Customer templateCustomer = new Customer("Template Customer", 30, "template@example.com");
        templateCustomer.setPreferredCategories(Arrays.asList("General"));
        dataStore.put("templateCustomer", templateCustomer);
    }

    private void initializeLookupServices() {
        Map<String, String> lookupServices = new HashMap<>();
        lookupServices.put("currencyService", "USD");
        lookupServices.put("exchangeRateService", "1.0");
        lookupServices.put("taxService", "0.08");
        dataStore.put("lookupServices", lookupServices);
    }

    private void initializeSourceRecords() {
        List<Trade> sourceRecords = Arrays.asList(
            new Trade("T001", "Equity", "Stock"),
            new Trade("T002", "Bond", "FixedIncome"),
            new Trade("T003", "Option", "Derivative"),
            new Trade("T004", "Future", "Derivative"),
            new Trade("T005", "Swap", "Derivative")
        );
        dataStore.put("sourceRecords", sourceRecords);
    }

    private Object createDefaultData() {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("type", dataType);
        defaultData.put("data", "Sample data for " + dataType);
        defaultData.put("timestamp", System.currentTimeMillis());
        return defaultData;
    }

    /**
     * Process matching logic for dynamic record matching.
     */
    @SuppressWarnings("unchecked")
    private List<Trade> processMatchingLogic(Object... parameters) {
        if (parameters.length < 2) {
            return new ArrayList<>();
        }

        List<Trade> sourceTrades = (List<Trade>) parameters[0];
        List<String> validatorNames = (List<String>) parameters[1];
        boolean findMatching = "matchingRecords".equals(dataType);

        List<Trade> result = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            boolean matches = hasMatch(trade, validatorNames);
            if (matches == findMatching) {
                result.add(trade);
            }
        }
        return result;
    }

    /**
     * Check if a trade matches any of the validators.
     */
    private boolean hasMatch(Trade trade, List<String> validatorNames) {
        // Simple matching logic for testing
        for (String validatorName : validatorNames) {
            if (validatorName.toLowerCase().contains(trade.getValue().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
