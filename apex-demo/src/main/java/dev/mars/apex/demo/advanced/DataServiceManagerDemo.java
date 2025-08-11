package dev.mars.apex.demo.advanced;

import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.demo.model.Trade;
import dev.mars.apex.core.service.data.CustomDataSource;
import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.lookup.RecordMatcher;
import dev.mars.apex.demo.rulesets.TradeRecordMatcherDemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * Demonstrates how to use the DataServiceManager to access data.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Demonstrates how to use the DataServiceManager to access data.
 * This class shows how to replace direct calls to MockDataSources with
 * calls to DataServiceManager, which provides a more flexible and
 * configurable approach to data access.
 */
public class DataServiceManagerDemo {

    /**
     * Demonstrates basic usage of the DataServiceManager.
     */
    public static void demonstrateBasicUsage() {
        System.out.println("\n=== Demonstrating DataServiceManager Basic Usage ===");

        // Create and initialize the DataServiceManager
        DataServiceManager dataServiceManager = new DataServiceManager();
        dataServiceManager.initializeWithMockData();

        // Get products
        List<Product> products = dataServiceManager.requestData("products");
        System.out.println("Products:");
        for (Product product : products) {
            System.out.println("  " + product.getName() + " - $" + product.getPrice() + " (" + product.getCategory() + ")");
        }

        // Get customer
        Customer customer = dataServiceManager.requestData("customer");
        System.out.println("\nCustomer:");
        System.out.println("  Name: " + customer.getName());
        System.out.println("  Age: " + customer.getAge());
        System.out.println("  Membership Level: " + customer.getMembershipLevel());
        System.out.println("  Preferred Categories: " + customer.getPreferredCategories());
    }

    /**
     * Demonstrates advanced usage of the DataServiceManager.
     */
    public static void demonstrateAdvancedUsage() {
        System.out.println("\n=== Demonstrating DataServiceManager Advanced Usage ===");

        // Create and initialize the DataServiceManager
        DataServiceManager dataServiceManager = new DataServiceManager();
        dataServiceManager.initializeWithMockData();

        // Get source records and lookup services
        List<Trade> sourceRecords = dataServiceManager.requestData("sourceRecords");
        List<LookupService> lookupServices = dataServiceManager.requestData("lookupServices");

        // Find matching records
        List<Trade> matchingRecords = dataServiceManager.requestData("matchingRecords", sourceRecords, lookupServices);
        System.out.println("Matching Records:");
        for (Trade trade : matchingRecords) {
            System.out.println("  " + trade.getId() + " - " + trade.getValue() + " (" + trade.getCategory() + ")");
        }

        // Find non-matching records
        List<Trade> nonMatchingRecords = dataServiceManager.requestData("nonMatchingRecords", sourceRecords, lookupServices);
        System.out.println("\nNon-Matching Records:");
        for (Trade trade : nonMatchingRecords) {
            System.out.println("  " + trade.getId() + " - " + trade.getValue() + " (" + trade.getCategory() + ")");
        }
    }

    /**
     * Demonstrates how to use custom data sources with the DataServiceManager.
     */
    public static void demonstrateCustomDataSources() {
        System.out.println("\n=== Demonstrating DataServiceManager with Custom Data Sources ===");

        // Create the DataServiceManager
        DataServiceManager dataServiceManager = new DataServiceManager();

        // Create and load custom data sources
        dataServiceManager.loadDataSources(
            new CustomDataSource("CustomProductsSource", "customProducts"),
            new CustomDataSource("CustomCustomerSource", "customCustomer"),
            new CustomDataSource("CustomTradesSource", "customTrades")
        );

        // Get custom products
        List<Product> customProducts = dataServiceManager.requestData("customProducts");
        System.out.println("Custom Products:");
        for (Product product : customProducts) {
            System.out.println("  " + product.getName() + " - $" + product.getPrice() + " (" + product.getCategory() + ")");
        }

        // Get custom customer
        Customer customCustomer = dataServiceManager.requestData("customCustomer");
        System.out.println("\nCustom Customer:");
        System.out.println("  Name: " + customCustomer.getName());
        System.out.println("  Age: " + customCustomer.getAge());
        System.out.println("  Membership Level: " + customCustomer.getMembershipLevel());
        System.out.println("  Preferred Categories: " + customCustomer.getPreferredCategories());

        // Get custom trades
        List<Trade> customTrades = dataServiceManager.requestData("customTrades");
        System.out.println("\nCustom Trades:");
        for (Trade trade : customTrades) {
            System.out.println("  " + trade.getId() + " - " + trade.getValue() + " (" + trade.getCategory() + ")");
        }

        // Demonstrate dynamic data modification
        System.out.println("\nDemonstrating Dynamic Data Modification:");

        // Get the custom data source by name
        CustomDataSource customTradesSource = (CustomDataSource) dataServiceManager.getDataSourceByName("CustomTradesSource");

        // Add a new trade
        List<Trade> updatedTrades = new ArrayList<>(customTrades);
        updatedTrades.add(new Trade("CT004", "New Custom Value", "Custom Category"));
        customTradesSource.addData("customTrades", updatedTrades);

        // Get the updated trades
        List<Trade> newCustomTrades = dataServiceManager.requestData("customTrades");
        System.out.println("Updated Custom Trades:");
        for (Trade trade : newCustomTrades) {
            System.out.println("  " + trade.getId() + " - " + trade.getValue() + " (" + trade.getCategory() + ")");
        }
    }

    /**
     * Demonstrates how to use the generic RecordMatcher with Trade objects.
     * This shows how to use the TradeRecordMatcherDemo implementation directly,
     * which is a more flexible and extensible approach than using hard-coded
     * Trade-specific matching logic.
     */
    public static void demonstrateGenericRecordMatcher() {
        System.out.println("\n=== Demonstrating Generic RecordMatcher with Trade Objects ===");

        // Create a registry and register some lookup services as validators
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Create some lookup services
        List<LookupService> lookupServices = Arrays.asList(
            new LookupService("InstrumentTypes", Arrays.asList("Equity", "Bond", "Option", "Future", "Swap", "ETF")),
            new LookupService("Markets", Arrays.asList("NYSE", "NASDAQ", "LSE", "TSE", "HKEX", "SGX")),
            new LookupService("TradeStatuses", Arrays.asList("Executed", "Settled", "Failed", "Pending", "Cancelled"))
        );

        // Register the lookup services with the registry
        List<String> validatorNames = new ArrayList<>();
        for (LookupService lookupService : lookupServices) {
            registry.registerService(lookupService);
            validatorNames.add(lookupService.getName());
        }

        // Create some source trades
        List<Trade> sourceTrades = Arrays.asList(
            new Trade("T001", "Equity", "InstrumentType"),
            new Trade("T002", "NASDAQ", "Market"),
            new Trade("T003", "Executed", "TradeStatus"),
            new Trade("T004", "Bond", "InstrumentType"),
            new Trade("T005", "NYSE", "Market"),
            new Trade("T006", "Pending", "TradeStatus"),
            new Trade("T007", "Commodity", "InstrumentType"),
            new Trade("T008", "OTC", "Market"),
            new Trade("T009", "Rejected", "TradeStatus")
        );

        // Create a TradeRecordMatcherDemo (implementation of RecordMatcher<Trade>)
        RecordMatcher<Trade> matcher = new TradeRecordMatcherDemo(registry);

        // Find matching records
        List<Trade> matchingTrades = matcher.findMatchingRecords(sourceTrades, validatorNames);
        System.out.println("Matching Records (using generic RecordMatcher):");
        for (Trade trade : matchingTrades) {
            System.out.println("  " + trade.getId() + " - " + trade.getValue() + " (" + trade.getCategory() + ")");
        }

        // Find non-matching records
        List<Trade> nonMatchingTrades = matcher.findNonMatchingRecords(sourceTrades, validatorNames);
        System.out.println("\nNon-Matching Records (using generic RecordMatcher):");
        for (Trade trade : nonMatchingTrades) {
            System.out.println("  " + trade.getId() + " - " + trade.getValue() + " (" + trade.getCategory() + ")");
        }
    }

    /**
     * Main method to run the demo.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        demonstrateBasicUsage();
        demonstrateAdvancedUsage();
        demonstrateCustomDataSources();
        demonstrateGenericRecordMatcher();
    }
}
