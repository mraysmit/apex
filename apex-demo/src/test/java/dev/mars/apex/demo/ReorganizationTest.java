package dev.mars.apex.demo;

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


import dev.mars.apex.demo.bootstrap.model.Customer;
import dev.mars.apex.demo.bootstrap.model.Product;
import dev.mars.apex.demo.util.DemoDataProvider;
import dev.mars.apex.demo.util.MockDataSources;
import dev.mars.apex.demo.util.TestUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test class for verifying demo reorganization and data consistency.
 * Ensures that demo data providers and utilities work correctly after
 * package reorganization.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@DisplayName("Demo Reorganization Tests")
public class ReorganizationTest {

    @Test
    @DisplayName("Demo data providers should create valid customers")
    void testCustomerDataProviders() {
        // Test main data provider
        List<Customer> customers = dev.mars.apex.demo.data.DemoDataProvider.createDemoCustomers();
        assertNotNull(customers, "Customer list should not be null");
        assertFalse(customers.isEmpty(), "Customer list should not be empty");

        // Verify customer data integrity
        for (Customer customer : customers) {
            assertNotNull(customer.getName(), "Customer name should not be null");
            assertNotNull(customer.getEmail(), "Customer email should not be null");
            assertTrue(customer.getAge() > 0, "Customer age should be positive");
            assertNotNull(customer.getMembershipLevel(), "Membership level should not be null");
        }

        // Test support data provider
        List<Customer> edgeCaseCustomers = DemoDataProvider.createEdgeCaseCustomers();
        assertNotNull(edgeCaseCustomers, "Edge case customer list should not be null");
        assertFalse(edgeCaseCustomers.isEmpty(), "Edge case customer list should not be empty");
    }

    @Test
    @DisplayName("Demo data providers should create valid products")
    void testProductDataProviders() {
        // Test main data provider
        List<Product> products = dev.mars.apex.demo.data.DemoDataProvider.createDemoProducts();
        assertNotNull(products, "Product list should not be null");
        assertFalse(products.isEmpty(), "Product list should not be empty");

        // Verify product data integrity
        for (Product product : products) {
            assertNotNull(product.getName(), "Product name should not be null");
            assertNotNull(product.getCategory(), "Product category should not be null");
            assertTrue(product.getPrice() >= 0, "Product price should not be negative");
        }

        // Test category-specific products
        List<Product> investmentProducts = DemoDataProvider.createCategorySpecificProducts("investment");
        assertNotNull(investmentProducts, "Investment products should not be null");
        assertFalse(investmentProducts.isEmpty(), "Investment products should not be empty");
    }

    @Test
    @DisplayName("Mock data sources should provide consistent data")
    void testMockDataSources() {
        // Test customer lookup
        var customerData = MockDataSources.lookupCustomer("CUST001");
        assertNotNull(customerData, "Customer lookup should return data");
        assertEquals("CUST001", customerData.get("id"), "Customer ID should match");

        // Test product lookup
        var productData = MockDataSources.lookupProduct("PROD001");
        assertNotNull(productData, "Product lookup should return data");
        assertEquals("PROD001", productData.get("id"), "Product ID should match");

        // Test reference data lookup
        var currencyData = MockDataSources.lookupReferenceData("CURRENCY", "USD");
        assertNotNull(currencyData, "Currency lookup should return data");
        assertEquals("USD", currencyData.get("code"), "Currency code should match");
    }

    @Test
    @DisplayName("Test utilities should provide helper functions")
    void testUtilityFunctions() {
        // Test customer creation
        List<Customer> customers = TestUtilities.createSampleCustomers();
        assertNotNull(customers, "Sample customers should not be null");
        assertFalse(customers.isEmpty(), "Sample customers should not be empty");

        // Test object to map conversion
        Customer testCustomer = customers.get(0);
        var customerMap = TestUtilities.objectToMap(testCustomer);
        assertNotNull(customerMap, "Customer map should not be null");
        assertTrue(customerMap.containsKey("name"), "Customer map should contain name");
        assertTrue(customerMap.containsKey("age"), "Customer map should contain age");
        assertTrue(customerMap.containsKey("email"), "Customer map should contain email");
    }

    @Test
    @DisplayName("Performance test data should be generated correctly")
    void testPerformanceDataGeneration() {
        int testCount = 100;
        List<Customer> performanceCustomers = dev.mars.apex.demo.data.DemoDataProvider.createPerformanceTestCustomers(testCount);

        assertNotNull(performanceCustomers, "Performance customers should not be null");
        assertEquals(testCount, performanceCustomers.size(), "Should create exact number of customers requested");

        // Verify data variety
        boolean hasBasic = false, hasSilver = false, hasGold = false, hasPremium = false;
        for (Customer customer : performanceCustomers) {
            String level = customer.getMembershipLevel();
            switch (level) {
                case "Basic" -> hasBasic = true;
                case "Silver" -> hasSilver = true;
                case "Gold" -> hasGold = true;
                case "Premium" -> hasPremium = true;
            }
        }

        assertTrue(hasBasic && hasSilver && hasGold && hasPremium,
                  "Performance data should include all membership levels");
    }

    @Test
    @DisplayName("Invalid demo data should be properly structured")
    void testInvalidDataGeneration() {
        List<Customer> invalidCustomers = dev.mars.apex.demo.data.DemoDataProvider.createInvalidDemoCustomers();

        assertNotNull(invalidCustomers, "Invalid customers should not be null");
        assertFalse(invalidCustomers.isEmpty(), "Invalid customers should not be empty");

        // Verify that invalid data is actually invalid for testing purposes
        boolean hasInvalidEmail = false, hasUnderAge = false, hasNegativeBalance = false;
        for (Customer customer : invalidCustomers) {
            if (customer.getEmail() != null && !customer.getEmail().contains("@")) {
                hasInvalidEmail = true;
            }
            if (customer.getAge() < 18) {
                hasUnderAge = true;
            }
            if (customer.getBalance() < 0) {
                hasNegativeBalance = true;
            }
        }

        assertTrue(hasInvalidEmail || hasUnderAge || hasNegativeBalance,
                  "Invalid data should contain at least one type of invalid condition");
    }

    @Test
    @DisplayName("Data source statistics should be accurate")
    void testDataSourceStatistics() {
        var stats = MockDataSources.getDataSourceStats();

        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.containsKey("customers"), "Statistics should include customer count");
        assertTrue(stats.containsKey("products"), "Statistics should include product count");
        assertTrue(stats.containsKey("referenceData"), "Statistics should include reference data count");

        assertTrue(stats.get("customers") > 0, "Should have customer data");
        assertTrue(stats.get("products") > 0, "Should have product data");
        assertTrue(stats.get("referenceData") > 0, "Should have reference data");
    }
}