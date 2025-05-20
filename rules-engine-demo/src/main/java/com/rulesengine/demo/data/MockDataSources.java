package com.rulesengine.demo.data;

import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.core.service.lookup.LookupService;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.lookup.RecordMatcher;
import com.rulesengine.demo.service.providers.TradeRecordMatcherDemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for providing product, customer, and lookup data.
 * This class centralizes data loading and management.
 */
public class MockDataSources {

    /**
     * Get a list of financial products for demonstration.
     * @return List of Product objects
     */
    public static List<Product> getProducts() {
        return Arrays.asList(
            new Product("US Treasury Bond", 1200.0, "FixedIncome"),
            new Product("Apple Stock", 800.0, "Equity"),
            new Product("S&P 500 ETF", 150.0, "ETF"),
            new Product("Gold Futures", 200.0, "Derivative"),
            new Product("Corporate Bond", 100.0, "FixedIncome")
        );
    }

    /**
     * Get a list of financial products for inventory demonstration.
     * @return List of Product objects representing inventory
     */
    public static List<Product> getInventory() {
        return Arrays.asList(
            new Product("US Treasury Bond", 1200.0, "FixedIncome"),
            new Product("Apple Stock", 800.0, "Equity"),
            new Product("S&P 500 ETF", 150.0, "ETF"),
            new Product("Gold Futures", 200.0, "Derivative"),
            new Product("Corporate Bond", 100.0, "FixedIncome"),
            new Product("Microsoft Stock", 350.0, "Equity"),
            new Product("Bitcoin ETF", 450.0, "ETF")
        );
    }

    /**
     * Get a customer for demonstration.
     * @return Customer object
     */
    public static Customer getCustomer() {
        return new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity", "ETF"));
    }

    /**
     * Get a customer for template demonstration.
     * @return Customer object
     */
    public static Customer getTemplateCustomer() {
        return new Customer("Bob Johnson", 65, "Silver", Arrays.asList("Equity", "ETF"));
    }

    /**
     * Creates a list of lookup services for demonstration.
     * 
     * @return List of LookupService objects
     */
    public static List<LookupService> createLookupServices() {
        return Arrays.asList(
            new LookupService("InstrumentTypes", Arrays.asList("Equity", "Bond", "Option", "Future", "Swap", "ETF")),
            new LookupService("Markets", Arrays.asList("NYSE", "NASDAQ", "LSE", "TSE", "HKEX", "SGX")),
            new LookupService("TradeStatuses", Arrays.asList("Executed", "Settled", "Failed", "Pending", "Cancelled"))
        );
    }

    /**
     * Creates a list of source records for demonstration.
     * 
     * @return List of Trade objects
     */
    public static List<Trade> createSourceRecords() {
        return Arrays.asList(
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
    }

    /**
     * Finds records that match any lookup service.
     * 
     * @param sourceTrades The source records to check
     * @param lookupServices The lookup services to check against
     * @return List of matching records
     */
    public static List<Trade> findMatchingRecords(List<Trade> sourceTrades, List<LookupService> lookupServices) {
        // Create a registry and register the lookup services as validators
        LookupServiceRegistry registry = new LookupServiceRegistry();
        List<String> validatorNames = new ArrayList<>();

        for (LookupService lookupService : lookupServices) {
            registry.registerService(lookupService);
            validatorNames.add(lookupService.getName());
        }

        // Use the generic TradeRecordMatcherDemo to find matching records
        RecordMatcher<Trade> matcher = new TradeRecordMatcherDemo(registry);
        return matcher.findMatchingRecords(sourceTrades, validatorNames);
    }

    /**
     * Finds records that don't match any lookup service.
     * 
     * @param sourceTrades The source records to check
     * @param lookupServices The lookup services to check against
     * @return List of non-matching records
     */
    public static List<Trade> findNonMatchingRecords(List<Trade> sourceTrades, List<LookupService> lookupServices) {
        // Create a registry and register the lookup services as validators
        LookupServiceRegistry registry = new LookupServiceRegistry();
        List<String> validatorNames = new ArrayList<>();

        for (LookupService lookupService : lookupServices) {
            registry.registerService(lookupService);
            validatorNames.add(lookupService.getName());
        }

        // Use the generic TradeRecordMatcherDemo to find non-matching records
        RecordMatcher<Trade> matcher = new TradeRecordMatcherDemo(registry);
        return matcher.findNonMatchingRecords(sourceTrades, validatorNames);
    }
}
