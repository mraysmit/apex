package dev.mars.apex.demo.data;

import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * ⚠️ DEMO/TESTING DATA SOURCE ONLY ⚠️
 * 
 * A mock data source implementation for demonstration and testing purposes.
 * 
 * 🚨 IMPORTANT NOTICE:
 * This class is intended for DEMO and TESTING purposes only and should NOT be used in production.
 * 
 * For production use cases, use the appropriate ExternalDataSource implementations:
 * - DatabaseDataSource for database connectivity
 * - FileSystemDataSource for file-based data
 * - RestApiDataSource for REST API integration
 * - CacheDataSource for caching scenarios
 * 
 * This MockDataSource provides hardcoded sample data for demonstrations and has significant
 * limitations compared to production data sources:
 * - No health monitoring or metrics
 * - No connection management
 * - No configuration-driven setup
 * - No error handling and recovery
 * - No scalability features
 * - Limited to hardcoded data types
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 * @deprecated Consider using production ExternalDataSource implementations for realistic scenarios
 */
@Deprecated
public class MockDataSource implements DataSource {
    private final String name;
    private final String dataType;
    private final Map<String, Object> dataStore = new HashMap<>();

    /**
     * Create a new MockDataSource with the specified name and data type.
     * 
     * ⚠️ FOR DEMO/TESTING PURPOSES ONLY ⚠️
     *
     * @param name The name of the data source
     * @param dataType The type of data this source provides
     */
    public MockDataSource(String name, String dataType) {
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
            List<LookupService> lookupServices = (List<LookupService>) parameters[1];

            if ("matchingRecords".equals(dataType)) {
                return (T) getMatchingRecords(sourceRecords, lookupServices);
            } else {
                return (T) getNonMatchingRecords(sourceRecords, lookupServices);
            }
        }

        return (T) dataStore.get(dataType);
    }

    /**
     * Initialize the mock data for this data source.
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
        List<Product> products = new ArrayList<>();
        products.add(new Product("US Treasury Bond", 1200.0, "FixedIncome"));
        products.add(new Product("Apple Stock", 180.0, "Equity"));
        products.add(new Product("Gold ETF", 320.0, "ETF"));
        products.add(new Product("Silver ETF", 150.0, "ETF"));
        products.add(new Product("Corporate Bond", 180.0, "FixedIncome"));
        dataStore.put("products", products);
    }

    private void initializeInventory() {
        List<Product> inventory = new ArrayList<>();
        inventory.add(new Product("Bitcoin ETF", 450.0, "ETF"));
        inventory.add(new Product("Corporate Bond", 1050.0, "FixedIncome"));
        inventory.add(new Product("Microsoft Corp", 350.0, "Equity"));
        dataStore.put("inventory", inventory);
    }

    private void initializeCustomer() {
        List<String> preferredCategories = new ArrayList<>();
        preferredCategories.add("Equity");
        preferredCategories.add("FixedIncome");
        Customer customer = new Customer("Alice Smith", 35, "Gold", preferredCategories);
        dataStore.put("customer", customer);
    }

    private void initializeTemplateCustomer() {
        List<String> preferredCategories = new ArrayList<>();
        preferredCategories.add("Equity");
        preferredCategories.add("FixedIncome");
        Customer templateCustomer = new Customer("Bob Johnson", 65, "Silver", preferredCategories);
        dataStore.put("templateCustomer", templateCustomer);
    }

    private void initializeLookupServices() {
        List<LookupService> lookupServices = new ArrayList<>();

        // Create InstrumentTypes lookup service
        List<String> instrumentTypeValues = Arrays.asList("Equity", "Bond", "ETF", "Option", "Future");
        LookupService instrumentTypes = new LookupService("InstrumentTypes", instrumentTypeValues);

        // Create AssetClasses lookup service
        List<String> assetClassValues = Arrays.asList("Equity", "FixedIncome", "Currency");
        LookupService assetClasses = new LookupService("AssetClasses", assetClassValues);

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

    private List<Trade> getMatchingRecords(List<Trade> sourceRecords, List<LookupService> lookupServices) {
        List<Trade> matchingRecords = new ArrayList<>();

        for (Trade trade : sourceRecords) {
            for (LookupService lookupService : lookupServices) {
                if (lookupService.getLookupValues().contains(trade.getValue())) {
                    matchingRecords.add(trade);
                    break;
                }
            }
        }

        return matchingRecords;
    }

    private List<Trade> getNonMatchingRecords(List<Trade> sourceRecords, List<LookupService> lookupServices) {
        List<Trade> nonMatchingRecords = new ArrayList<>();

        for (Trade trade : sourceRecords) {
            boolean isMatching = false;
            for (LookupService lookupService : lookupServices) {
                if (lookupService.getLookupValues().contains(trade.getValue())) {
                    isMatching = true;
                    break;
                }
            }
            if (!isMatching) {
                nonMatchingRecords.add(trade);
            }
        }

        return nonMatchingRecords;
    }
}
