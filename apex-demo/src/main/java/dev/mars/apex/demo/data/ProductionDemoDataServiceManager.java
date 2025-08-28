package dev.mars.apex.demo.data;

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


import dev.mars.apex.core.service.data.CustomDataSource;
import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.core.service.data.DataSource;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.demo.bootstrap.model.Customer;
import dev.mars.apex.demo.bootstrap.model.Product;
import dev.mars.apex.demo.bootstrap.model.Trade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Production-ready demo DataServiceManager with realistic mock data sources.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Production-ready demo DataServiceManager with realistic mock data sources.
 *
 * This class demonstrates how to register multiple data sources and provide
 * data for different types used across demos and tests. It intentionally uses
 * CustomDataSource for simplicity and determinism in demos/tests.
 */
public class ProductionDemoDataServiceManager extends DataServiceManager {

    @Override
    public DataServiceManager initializeWithMockData() {
        // Products data source
        CustomDataSource productsSource = new CustomDataSource("ProductsDataSource", "products");
        productsSource.addData("products", createProducts());
        loadDataSource(productsSource);

        // Inventory data source
        CustomDataSource inventorySource = new CustomDataSource("InventoryDataSource", "inventory");
        inventorySource.addData("inventory", createInventory());
        loadDataSource(inventorySource);

        // Customer data source
        CustomDataSource customerSource = new CustomDataSource("CustomerDataSource", "customer");
        customerSource.addData("customer", createPrimaryCustomer());
        loadDataSource(customerSource);

        // Template customer data source
        CustomDataSource templateCustomerSource = new CustomDataSource("TemplateCustomerDataSource", "templateCustomer");
        templateCustomerSource.addData("templateCustomer", createTemplateCustomer());
        loadDataSource(templateCustomerSource);

        // Lookup services data source
        CustomDataSource lookupServicesSource = new CustomDataSource("LookupServicesDataSource", "lookupServices");
        lookupServicesSource.addData("lookupServices", createLookupServices());
        loadDataSource(lookupServicesSource);

        // Source records (trades) data source
        CustomDataSource sourceRecordsSource = new CustomDataSource("SourceRecordsDataSource", "sourceRecords");
        sourceRecordsSource.addData("sourceRecords", createSourceRecords());
        loadDataSource(sourceRecordsSource);

        // Matching records data source
        CustomDataSource matchingRecordsSource = new CustomDataSource("MatchingRecordsDataSource", "matchingRecords");
        matchingRecordsSource.addData("matchingRecords", createMatchingRecords());
        loadDataSource(matchingRecordsSource);

        // Non-matching records data source
        CustomDataSource nonMatchingRecordsSource = new CustomDataSource("NonMatchingRecordsDataSource", "nonMatchingRecords");
        nonMatchingRecordsSource.addData("nonMatchingRecords", createNonMatchingRecords());
        loadDataSource(nonMatchingRecordsSource);

        return this;
    }

    private List<Product> createProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("US Treasury Bond", 1200.0, "FixedIncome"));
        products.add(new Product("Apple Stock", 180.5, "Equity"));
        products.add(new Product("Gold ETF", 250.0, "ETF"));
        return products;
    }

    private List<Product> createInventory() {
        List<Product> inventory = new ArrayList<>();
        inventory.add(new Product("Bitcoin ETF", 450.0, "ETF"));
        inventory.add(new Product("Corporate Bond", 980.0, "FixedIncome"));
        inventory.add(new Product("Oil Futures", 73.25, "Derivatives"));
        return inventory;
    }

    private Customer createPrimaryCustomer() {
        Customer customer = new Customer();
        customer.setName("Alice Smith");
        customer.setAge(35);
        customer.setMembershipLevel("Gold");
        customer.setPreferredCategories(Arrays.asList("FixedIncome", "ETF"));
        return customer;
    }

    private Customer createTemplateCustomer() {
        Customer customer = new Customer();
        customer.setName("Bob Johnson");
        customer.setAge(65);
        customer.setMembershipLevel("Silver");
        customer.setPreferredCategories(Arrays.asList("Equity"));
        return customer;
    }

    private List<LookupService> createLookupServices() {
        List<LookupService> services = new ArrayList<>();
        services.add(new LookupService("InstrumentTypeService", Arrays.asList("Equity", "FixedIncome", "ETF", "Derivatives")));
        services.add(new LookupService("RegionService", Arrays.asList("NA", "EU", "APAC")));
        return services;
    }

    private List<Trade> createSourceRecords() {
        List<Trade> trades = new ArrayList<>();
        trades.add(new Trade("T001", "Equity", "InstrumentType"));
        trades.add(new Trade("T002", "FixedIncome", "InstrumentType"));
        trades.add(new Trade("T003", "ETF", "InstrumentType"));
        return trades;
    }

    private List<Trade> createMatchingRecords() {
        // Ensure it contains T001 Equity for tests
        List<Trade> matches = new ArrayList<>();
        matches.add(new Trade("T001", "Equity", "InstrumentType"));
        matches.add(new Trade("T003", "ETF", "InstrumentType"));
        return matches;
    }

    private List<Trade> createNonMatchingRecords() {
        List<Trade> nonMatches = new ArrayList<>();
        nonMatches.add(new Trade("T008", "NonMatchingValue", "InstrumentType"));
        return nonMatches;
    }
}

