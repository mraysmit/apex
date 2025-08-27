package dev.mars.apex.demo.bootstrap;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.model.Product;
import dev.mars.apex.core.service.lookup.LookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the merged ApexAdvancedFeaturesDemo.
 * 
 * This test verifies that the merged functionality from:
 * - ApexAdvancedFeaturesDemo
 * - ApexAdvancedFeaturesDemoConfig  
 * - ApexAdvancedFeaturesDataProvider
 * 
 * All works correctly in the single merged class.
 */
class ApexAdvancedFeaturesDemoTest {

    private ApexAdvancedFeaturesDemo demo;

    @BeforeEach
    void setUp() {
        demo = new ApexAdvancedFeaturesDemo();
    }

    @Test
    @DisplayName("Should create demo instance successfully")
    void testDemoCreation() {
        assertNotNull(demo, "Demo instance should be created");
    }

    @Test
    @DisplayName("Should provide products data")
    void testProductsData() {
        List<Product> products = demo.getData("products");
        
        assertNotNull(products, "Products should not be null");
        assertFalse(products.isEmpty(), "Products list should not be empty");
        assertTrue(products.size() >= 5, "Should have at least 5 products");
        
        // Verify product structure
        Product firstProduct = products.get(0);
        assertNotNull(firstProduct.getName(), "Product name should not be null");
        assertTrue(firstProduct.getPrice() > 0, "Product price should be positive");
        assertNotNull(firstProduct.getCategory(), "Product category should not be null");
    }

    @Test
    @DisplayName("Should provide customer data")
    void testCustomerData() {
        Customer customer = demo.getData("customer");
        
        assertNotNull(customer, "Customer should not be null");
        assertNotNull(customer.getName(), "Customer name should not be null");
        assertTrue(customer.getAge() > 0, "Customer age should be positive");
        assertNotNull(customer.getMembershipLevel(), "Membership level should not be null");
        assertNotNull(customer.getPreferredCategories(), "Preferred categories should not be null");
    }

    @Test
    @DisplayName("Should provide lookup services data")
    void testLookupServicesData() {
        List<LookupService> services = demo.getData("lookupServices");
        
        assertNotNull(services, "Lookup services should not be null");
        assertFalse(services.isEmpty(), "Lookup services list should not be empty");
        
        // Verify lookup service structure
        LookupService firstService = services.get(0);
        assertNotNull(firstService.getName(), "Service name should not be null");
        assertNotNull(firstService.getLookupValues(), "Lookup values should not be null");
        assertFalse(firstService.getLookupValues().isEmpty(), "Lookup values should not be empty");
    }

    @Test
    @DisplayName("Should create evaluation context with data")
    void testCreateContext() {
        StandardEvaluationContext context = demo.createContext();
        
        assertNotNull(context, "Context should not be null");
        
        // Verify context has required variables
        assertTrue(context.lookupVariable("products") != null, "Context should have products variable");
        assertTrue(context.lookupVariable("customer") != null, "Context should have customer variable");
        assertTrue(context.lookupVariable("inventory") != null, "Context should have inventory variable");
    }

    @Test
    @DisplayName("Should create template context with data")
    void testCreateTemplateContext() {
        StandardEvaluationContext context = demo.createTemplateContext();
        
        assertNotNull(context, "Template context should not be null");
        
        // Verify context has required variables
        assertTrue(context.lookupVariable("customer") != null, "Template context should have customer variable");
        assertTrue(context.lookupVariable("products") != null, "Template context should have products variable");
    }

    @Test
    @DisplayName("Should create investment rules")
    void testCreateInvestmentRules() {
        List<Rule> rules = demo.createInvestmentRules();
        
        assertNotNull(rules, "Rules should not be null");
        assertFalse(rules.isEmpty(), "Rules list should not be empty");
        assertTrue(rules.size() >= 3, "Should have at least 3 investment rules");
        
        // Verify rule structure
        Rule firstRule = rules.get(0);
        assertNotNull(firstRule.getName(), "Rule name should not be null");
        assertNotNull(firstRule.getCondition(), "Rule condition should not be null");
        assertNotNull(firstRule.getDescription(), "Rule description should not be null");
    }

    @Test
    @DisplayName("Should create rule result rules")
    void testCreateRuleResultRules() {
        List<Rule> rules = demo.createRuleResultRules();
        
        assertNotNull(rules, "Rule result rules should not be null");
        assertFalse(rules.isEmpty(), "Rule result rules list should not be empty");
        assertTrue(rules.size() >= 4, "Should have at least 4 rule result rules");
        
        // Verify rule structure
        Rule firstRule = rules.get(0);
        assertNotNull(firstRule.getName(), "Rule name should not be null");
        assertNotNull(firstRule.getCondition(), "Rule condition should not be null");
        assertNotNull(firstRule.getDescription(), "Rule description should not be null");
    }

    @Test
    @DisplayName("Should provide source records data")
    void testSourceRecordsData() {
        List<Map<String, Object>> records = demo.getData("sourceRecords");
        
        assertNotNull(records, "Source records should not be null");
        assertFalse(records.isEmpty(), "Source records list should not be empty");
        
        // Verify record structure
        Map<String, Object> firstRecord = records.get(0);
        assertNotNull(firstRecord, "First record should not be null");
        assertFalse(firstRecord.isEmpty(), "First record should not be empty");
        assertTrue(firstRecord.containsKey("id"), "Record should have id field");
        assertTrue(firstRecord.containsKey("type"), "Record should have type field");
    }

    @Test
    @DisplayName("Should handle unknown data types gracefully")
    void testUnknownDataType() {
        Object result = demo.getData("unknownType");
        assertNull(result, "Unknown data type should return null");
    }
}
