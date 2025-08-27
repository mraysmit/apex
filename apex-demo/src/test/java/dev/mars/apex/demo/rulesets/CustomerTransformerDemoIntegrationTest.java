package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.TransformerRule;
import dev.mars.apex.demo.bootstrap.model.Customer;

import java.util.ArrayList;
import java.util.Map;

/**
 * Integration test for the merged CustomerTransformerDemo.
 * 
 * This test verifies that the core functionality works correctly
 * by testing the merged class with actual APEX components.
 */
public class CustomerTransformerDemoIntegrationTest {

    public static void main(String[] args) {
        CustomerTransformerDemoIntegrationTest test = new CustomerTransformerDemoIntegrationTest();
        
        System.out.println("Running CustomerTransformerDemo integration test...");
        
        try {
            test.testMergedClassFunctionality();
            System.out.println("✓ Merged class functionality test passed");
            
            test.testTransformerRuleCreation();
            System.out.println("✓ Transformer rule creation test passed");
            
            test.testDiscountCalculation();
            System.out.println("✓ Discount calculation test passed");
            
            test.testMembershipDiscountsConfiguration();
            System.out.println("✓ Membership discounts configuration test passed");
            
            System.out.println("\nAll integration tests passed! ✓");
            System.out.println("CustomerTransformerDemo merge is fully functional!");
            
        } catch (Exception e) {
            System.err.println("Integration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void testMergedClassFunctionality() {
        // Test that we can create an instance with a rules engine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        
        // Use reflection to create an instance since the constructor is private
        try {
            java.lang.reflect.Constructor<CustomerTransformerDemo> constructor = 
                CustomerTransformerDemo.class.getDeclaredConstructor(RulesEngine.class);
            constructor.setAccessible(true);
            CustomerTransformerDemo demo = constructor.newInstance(rulesEngine);
            
            assertNotNull("Demo instance should be created", demo);
            
            // Test that we can access the rules engine
            RulesEngine retrievedEngine = demo.getRulesEngine();
            assertNotNull("Rules engine should be accessible", retrievedEngine);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create demo instance: " + e.getMessage(), e);
        }
    }

    public void testTransformerRuleCreation() {
        // Create a demo instance
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        CustomerTransformerDemo demo = createDemoInstance(rulesEngine);
        
        // Test creating different types of transformer rules
        TransformerRule<Customer> goldRule = demo.createGoldMemberRule();
        assertNotNull("Gold member rule should be created", goldRule);
        assertEquals("GoldMemberRule", goldRule.getRule().getName());
        
        TransformerRule<Customer> silverRule = demo.createSilverMemberRule();
        assertNotNull("Silver member rule should be created", silverRule);
        assertEquals("SilverMemberRule", silverRule.getRule().getName());
        
        TransformerRule<Customer> basicRule = demo.createBasicMemberRule();
        assertNotNull("Basic member rule should be created", basicRule);
        assertEquals("BasicMemberRule", basicRule.getRule().getName());
        
        TransformerRule<Customer> youngRule = demo.createYoungCustomerRule();
        assertNotNull("Young customer rule should be created", youngRule);
        assertEquals("YoungCustomerRule", youngRule.getRule().getName());
        
        TransformerRule<Customer> seniorRule = demo.createSeniorCustomerRule();
        assertNotNull("Senior customer rule should be created", seniorRule);
        assertEquals("SeniorCustomerRule", seniorRule.getRule().getName());
    }

    public void testDiscountCalculation() {
        // Create a demo instance
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        CustomerTransformerDemo demo = createDemoInstance(rulesEngine);
        
        // Create test customers
        Customer goldCustomer = new Customer("Alice", 25, "Gold", new ArrayList<>());
        Customer silverCustomer = new Customer("Bob", 35, "Silver", new ArrayList<>());
        Customer bronzeCustomer = new Customer("Charlie", 45, "Bronze", new ArrayList<>());
        Customer basicCustomer = new Customer("Diana", 65, "Basic", new ArrayList<>());
        Customer unknownCustomer = new Customer("Eve", 30, "Unknown", new ArrayList<>());
        
        // Test discount calculations
        double goldDiscount = demo.getDiscountForCustomer(goldCustomer);
        assertEquals(0.15, goldDiscount, 0.001); // 15% discount
        
        double silverDiscount = demo.getDiscountForCustomer(silverCustomer);
        assertEquals(0.10, silverDiscount, 0.001); // 10% discount
        
        double bronzeDiscount = demo.getDiscountForCustomer(bronzeCustomer);
        assertEquals(0.05, bronzeDiscount, 0.001); // 5% discount
        
        double basicDiscount = demo.getDiscountForCustomer(basicCustomer);
        assertEquals(0.02, basicDiscount, 0.001); // 2% discount
        
        double unknownDiscount = demo.getDiscountForCustomer(unknownCustomer);
        assertEquals(0.0, unknownDiscount, 0.001); // No discount for unknown level
        
        // Test null customer
        double nullDiscount = demo.getDiscountForCustomer(null);
        assertEquals(0.0, nullDiscount, 0.001); // No discount for null customer
    }

    public void testMembershipDiscountsConfiguration() {
        // Create a demo instance
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        CustomerTransformerDemo demo = createDemoInstance(rulesEngine);
        
        // Get the membership discounts map
        Map<String, Double> discounts = demo.getMembershipDiscounts();
        assertNotNull("Membership discounts should not be null", discounts);
        
        // Verify all expected membership levels are configured
        assertTrue("Should contain Gold discount", discounts.containsKey("Gold"));
        assertTrue("Should contain Silver discount", discounts.containsKey("Silver"));
        assertTrue("Should contain Bronze discount", discounts.containsKey("Bronze"));
        assertTrue("Should contain Basic discount", discounts.containsKey("Basic"));
        
        // Verify discount values
        assertEquals(0.15, discounts.get("Gold"), 0.001);
        assertEquals(0.10, discounts.get("Silver"), 0.001);
        assertEquals(0.05, discounts.get("Bronze"), 0.001);
        assertEquals(0.02, discounts.get("Basic"), 0.001);
    }

    private CustomerTransformerDemo createDemoInstance(RulesEngine rulesEngine) {
        try {
            java.lang.reflect.Constructor<CustomerTransformerDemo> constructor = 
                CustomerTransformerDemo.class.getDeclaredConstructor(RulesEngine.class);
            constructor.setAccessible(true);
            return constructor.newInstance(rulesEngine);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create demo instance: " + e.getMessage(), e);
        }
    }
    
    private void assertNotNull(String message, Object value) {
        if (value == null) {
            throw new RuntimeException(message + " - Expected not null, but was null");
        }
    }
    
    private void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new RuntimeException("Expected '" + expected + "' but was '" + actual + "'");
        }
    }
    
    private void assertEquals(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new RuntimeException("Expected " + expected + " but was " + actual + " (delta: " + delta + ")");
        }
    }
    
    private void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
