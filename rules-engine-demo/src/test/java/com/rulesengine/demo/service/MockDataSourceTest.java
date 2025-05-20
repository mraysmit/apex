package com.rulesengine.demo.service;

import com.rulesengine.core.service.lookup.LookupService;
import com.rulesengine.demo.model.Customer;
import com.rulesengine.demo.model.Product;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.demo.service.providers.MockDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MockDataSource.
 */
public class MockDataSourceTest {
    private MockDataSource productsDataSource;
    private MockDataSource inventoryDataSource;
    private MockDataSource customerDataSource;
    private MockDataSource templateCustomerDataSource;
    private MockDataSource lookupServicesDataSource;
    private MockDataSource sourceRecordsDataSource;
    private MockDataSource matchingRecordsDataSource;
    private MockDataSource nonMatchingRecordsDataSource;

    @BeforeEach
    public void setUp() {
        productsDataSource = new MockDataSource("ProductsDataSource", "products");
        inventoryDataSource = new MockDataSource("InventoryDataSource", "inventory");
        customerDataSource = new MockDataSource("CustomerDataSource", "customer");
        templateCustomerDataSource = new MockDataSource("TemplateCustomerDataSource", "templateCustomer");
        lookupServicesDataSource = new MockDataSource("LookupServicesDataSource", "lookupServices");
        sourceRecordsDataSource = new MockDataSource("SourceRecordsDataSource", "sourceRecords");
        matchingRecordsDataSource = new MockDataSource("MatchingRecordsDataSource", "matchingRecords");
        nonMatchingRecordsDataSource = new MockDataSource("NonMatchingRecordsDataSource", "nonMatchingRecords");
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
        assertEquals(42, templateCustomer.getAge());
        assertEquals("Silver", templateCustomer.getMembershipLevel());
    }

    @Test
    public void testGetDataLookupServices() {
        List<LookupService> lookupServices = lookupServicesDataSource.getData("lookupServices");
        assertNotNull(lookupServices);
        assertFalse(lookupServices.isEmpty());
        
        // Verify some lookup service data
        boolean foundInstrumentTypes = false;
        for (LookupService lookupService : lookupServices) {
            if ("InstrumentTypes".equals(lookupService.getName())) {
                foundInstrumentTypes = true;
                List<String> values = lookupService.getLookupValues();
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
        List<LookupService> lookupServices = lookupServicesDataSource.getData("lookupServices");
        
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
        List<LookupService> lookupServices = lookupServicesDataSource.getData("lookupServices");
        
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
}