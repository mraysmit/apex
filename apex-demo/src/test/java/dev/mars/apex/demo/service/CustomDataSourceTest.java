package dev.mars.apex.demo.service;

import dev.mars.apex.core.service.data.CustomDataSource;
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
 * Test class for CustomDataSource.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for CustomDataSource.
 */
public class CustomDataSourceTest {
    private CustomDataSource customProductsSource;
    private CustomDataSource customCustomerSource;
    private CustomDataSource customTradesSource;
    private CustomDataSource emptyDataSource;

    @BeforeEach
    public void setUp() {
        customProductsSource = new CustomDataSource("CustomProductsSource", "customProducts");
        customCustomerSource = new CustomDataSource("CustomCustomerSource", "customCustomer");
        customTradesSource = new CustomDataSource("CustomTradesSource", "customTrades");
        emptyDataSource = new CustomDataSource("EmptyDataSource", "emptyDataType");
    }

    @Test
    public void testConstructor() {
        assertEquals("CustomProductsSource", customProductsSource.getName());
        assertEquals("customProducts", customProductsSource.getDataType());
    }

    @Test
    public void testGetName() {
        assertEquals("CustomProductsSource", customProductsSource.getName());
        assertEquals("CustomCustomerSource", customCustomerSource.getName());
        assertEquals("CustomTradesSource", customTradesSource.getName());
        assertEquals("EmptyDataSource", emptyDataSource.getName());
    }

    @Test
    public void testGetDataType() {
        assertEquals("customProducts", customProductsSource.getDataType());
        assertEquals("customCustomer", customCustomerSource.getDataType());
        assertEquals("customTrades", customTradesSource.getDataType());
        assertEquals("emptyDataType", emptyDataSource.getDataType());
    }

    @Test
    public void testSupportsDataType() {
        assertTrue(customProductsSource.supportsDataType("customProducts"));
        assertFalse(customProductsSource.supportsDataType("customCustomer"));
        assertFalse(customProductsSource.supportsDataType(null));
        assertFalse(customProductsSource.supportsDataType(""));
    }

    @Test
    public void testGetDataCustomProducts() {
        // Initialize data
        List<Product> initialProducts = new ArrayList<>();
        initialProducts.add(new Product("Custom Product 1", 100.0, "Custom"));
        customProductsSource.addData("customProducts", initialProducts);
        
        List<Product> products = customProductsSource.getData("customProducts");
        assertNotNull(products);
        assertFalse(products.isEmpty());

        // Verify some product data
        boolean foundCustomProduct1 = false;
        for (Product product : products) {
            if ("Custom Product 1".equals(product.getName())) {
                foundCustomProduct1 = true;
                assertEquals(100.0, product.getPrice());
                assertEquals("Custom", product.getCategory());
                break;
            }
        }
        assertTrue(foundCustomProduct1, "Custom Product 1 should be in the products list");
    }

    @Test
    public void testGetDataCustomCustomer() {
        // Initialize data
        Customer initialCustomer = new Customer("Custom Customer", 30, "Platinum", new ArrayList<>());
        customCustomerSource.addData("customCustomer", initialCustomer);
        
        Customer customer = customCustomerSource.getData("customCustomer");
        assertNotNull(customer);
        assertEquals("Custom Customer", customer.getName());
        assertEquals(30, customer.getAge());
        assertEquals("Platinum", customer.getMembershipLevel());
    }

    @Test
    public void testGetDataCustomTrades() {
        // Initialize data
        List<Trade> initialTrades = new ArrayList<>();
        initialTrades.add(new Trade("CT001", "Custom Value 1", "Custom Category"));
        customTradesSource.addData("customTrades", initialTrades);
        
        List<Trade> trades = customTradesSource.getData("customTrades");
        assertNotNull(trades);
        assertFalse(trades.isEmpty());

        // Verify some trade data
        boolean foundCustomTrade1 = false;
        for (Trade trade : trades) {
            if ("CT001".equals(trade.getId())) {
                foundCustomTrade1 = true;
                assertEquals("Custom Value 1", trade.getValue());
                assertEquals("Custom Category", trade.getCategory());
                break;
            }
        }
        assertTrue(foundCustomTrade1, "Custom Trade 1 should be in the trades list");
    }

    @Test
    public void testGetDataWithUnsupportedDataType() {
        // Test with unsupported data type
        assertNull(customProductsSource.getData("unsupportedType"));
    }

    @Test
    public void testAddData() {
        // Create new data
        List<Product> newProducts = new ArrayList<>();
        newProducts.add(new Product("New Product 1", 150.0, "New Category"));
        newProducts.add(new Product("New Product 2", 250.0, "New Category"));

        // Add the new data
        customProductsSource.addData("customProducts", newProducts);

        // Verify the data was added
        List<Product> products = customProductsSource.getData("customProducts");
        assertNotNull(products);
        assertEquals(2, products.size());

        boolean foundNewProduct1 = false;
        for (Product product : products) {
            if ("New Product 1".equals(product.getName())) {
                foundNewProduct1 = true;
                assertEquals(150.0, product.getPrice());
                assertEquals("New Category", product.getCategory());
                break;
            }
        }
        assertTrue(foundNewProduct1, "New Product 1 should be in the products list");
    }

    @Test
    public void testAddDataWithDifferentDataType() {
        // Create new data
        List<Product> newProducts = new ArrayList<>();
        newProducts.add(new Product("New Product 1", 150.0, "New Category"));

        // Add the new data with a different data type
        customProductsSource.addData("differentDataType", newProducts);

        // Verify the original data is still there
        List<Product> products = customProductsSource.getData("customProducts");
        assertNull(products); // Initially empty

        // The CustomDataSource implementation doesn't support getting data with a different data type
        // than the one specified in the constructor, so we can't verify the new data directly.
        // Instead, we can verify that the data was added to the data store by checking if it's returned
        // when we override the supportsDataType method.

        // Create a new CustomDataSource that supports the different data type
        CustomDataSource differentDataSource = new CustomDataSource("DifferentDataSource", "differentDataType");

        // Add the same data to the new data source
        differentDataSource.addData("differentDataType", newProducts);

        // Verify the data is there
        List<Product> differentProducts = differentDataSource.getData("differentDataType");
        assertNotNull(differentProducts);
        assertEquals(1, differentProducts.size());
        assertEquals("New Product 1", differentProducts.get(0).getName());
    }

    @Test
    public void testRemoveData() {
        // Add data first
        List<Product> initialProducts = new ArrayList<>();
        initialProducts.add(new Product("Custom Product 1", 100.0, "Custom"));
        customProductsSource.addData("customProducts", initialProducts);
        
        // Verify the data exists
        List<Product> products = customProductsSource.getData("customProducts");
        assertNotNull(products);
        assertFalse(products.isEmpty());

        // Remove the data
        customProductsSource.removeData("customProducts");

        // Verify the data was removed
        List<Product> productsAfterRemoval = customProductsSource.getData("customProducts");
        assertNull(productsAfterRemoval);
    }

    @Test
    public void testClearData() {
        // Add data first
        List<Product> initialProducts = new ArrayList<>();
        initialProducts.add(new Product("Custom Product 1", 100.0, "Custom"));
        customProductsSource.addData("customProducts", initialProducts);
        
        // Verify the data exists
        List<Product> products = customProductsSource.getData("customProducts");
        assertNotNull(products);
        assertFalse(products.isEmpty());

        // Clear the data
        customProductsSource.clearData();

        // Verify all data was removed
        List<Product> productsAfterClear = customProductsSource.getData("customProducts");
        assertNull(productsAfterClear);
    }

    @Test
    public void testAddAndRemoveData() {
        // Create a new CustomDataSource that supports the data type we want to add
        CustomDataSource newDataSource = new CustomDataSource("NewDataSource", "newDataType");

        // Create new data
        List<Product> newProducts = new ArrayList<>();
        newProducts.add(new Product("New Product 1", 150.0, "New Category"));

        // Add the new data
        newDataSource.addData("newDataType", newProducts);

        // Verify the data was added
        List<Product> products = newDataSource.getData("newDataType");
        assertNotNull(products);
        assertEquals(1, products.size());

        // Remove the data
        newDataSource.removeData("newDataType");

        // Verify the data was removed
        List<Product> productsAfterRemoval = newDataSource.getData("newDataType");
        assertNull(productsAfterRemoval);
    }

    @Test
    public void testAddAndClearData() {
        // Create a new CustomDataSource that supports the data type we want to add
        CustomDataSource newDataSource = new CustomDataSource("NewDataSource", "newDataType");

        // Create new data
        List<Product> newProducts = new ArrayList<>();
        newProducts.add(new Product("New Product 1", 150.0, "New Category"));

        // Add the new data
        newDataSource.addData("newDataType", newProducts);

        // Verify the data was added
        List<Product> products = newDataSource.getData("newDataType");
        assertNotNull(products);
        assertEquals(1, products.size());

        // Clear the data
        newDataSource.clearData();

        // Verify the data was removed
        List<Product> productsAfterClear = newDataSource.getData("newDataType");
        assertNull(productsAfterClear);
    }
}
