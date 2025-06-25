package dev.mars.rulesengine.core.api;

import dev.mars.rulesengine.core.engine.model.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the simplified rules engine API.
 */
public class SimpleRulesEngineTest {
    
    private SimpleRulesEngine simpleEngine;
    
    @BeforeEach
    public void setUp() {
        simpleEngine = new SimpleRulesEngine();
    }
    
    @Test
    public void testAgeEligibilityCheck() {
        // Test eligible customer
        assertTrue(simpleEngine.isAgeEligible(25, 18));
        assertTrue(simpleEngine.isAgeEligible(18, 18));
        
        // Test ineligible customer
        assertFalse(simpleEngine.isAgeEligible(16, 18));
        assertFalse(simpleEngine.isAgeEligible(17, 21));
    }
    
    @Test
    public void testAmountRangeCheck() {
        // Test amounts within range
        assertTrue(simpleEngine.isAmountInRange(100.0, 50.0, 200.0));
        assertTrue(simpleEngine.isAmountInRange(50.0, 50.0, 200.0));
        assertTrue(simpleEngine.isAmountInRange(200.0, 50.0, 200.0));
        
        // Test amounts outside range
        assertFalse(simpleEngine.isAmountInRange(49.0, 50.0, 200.0));
        assertFalse(simpleEngine.isAmountInRange(201.0, 50.0, 200.0));
    }
    
    @Test
    public void testRequiredFieldsValidation() {
        TestCustomer customer = new TestCustomer("John Doe", "john@example.com");
        
        // Test with all required fields present
        assertTrue(simpleEngine.validateRequiredFields(customer, "name", "email"));
        
        // Test with missing field
        TestCustomer incompleteCustomer = new TestCustomer("Jane Doe", null);
        assertFalse(simpleEngine.validateRequiredFields(incompleteCustomer, "name", "email"));
        
        // Test with empty field
        TestCustomer emptyFieldCustomer = new TestCustomer("", "jane@example.com");
        assertFalse(simpleEngine.validateRequiredFields(emptyFieldCustomer, "name", "email"));
    }
    
    @Test
    public void testSimpleConditionEvaluation() {
        Map<String, Object> data = new HashMap<>();
        data.put("age", 25);
        data.put("status", "active");
        data.put("balance", 1000.0);
        
        // Test simple conditions
        assertTrue(simpleEngine.evaluate("#age > 18", data));
        assertTrue(simpleEngine.evaluate("#status == 'active'", data));
        assertTrue(simpleEngine.evaluate("#balance >= 500", data));
        
        // Test complex condition
        assertTrue(simpleEngine.evaluate("#age > 18 && #status == 'active' && #balance >= 500", data));
        
        // Test false conditions
        assertFalse(simpleEngine.evaluate("#age < 18", data));
        assertFalse(simpleEngine.evaluate("#status == 'inactive'", data));
    }
    
    @Test
    public void testSimpleConditionWithSingleObject() {
        TestCustomer customer = new TestCustomer("John Doe", "john@example.com");
        customer.setAge(30);
        customer.setBalance(1500.0);
        
        // Test conditions against single object
        assertTrue(simpleEngine.evaluate("#data.age > 25", customer));
        assertTrue(simpleEngine.evaluate("#data.name == 'John Doe'", customer));
        assertTrue(simpleEngine.evaluate("#data.balance > 1000", customer));
        
        // Test false conditions
        assertFalse(simpleEngine.evaluate("#data.age < 25", customer));
        assertFalse(simpleEngine.evaluate("#data.balance < 1000", customer));
    }
    
    @Test
    public void testValidationRuleBuilder() {
        Map<String, Object> data = new HashMap<>();
        data.put("age", 25);
        data.put("income", 50000);
        
        // Test validation rule creation and testing
        boolean result = simpleEngine.validationRule("Age Check", "#age >= 18", "Customer is adult")
            .priority(1)
            .description("Validates customer age")
            .test(data);
        
        assertTrue(result);
        
        // Test with failing condition
        data.put("age", 16);
        boolean failResult = simpleEngine.validationRule("Age Check 2", "#age >= 18", "Customer is adult")
            .test(data);
        
        assertFalse(failResult);
    }
    
    @Test
    public void testBusinessRuleBuilder() {
        TestCustomer customer = new TestCustomer("Premium Customer", "premium@example.com");
        customer.setAge(35);
        customer.setBalance(10000.0);
        customer.setMembershipLevel("Gold");
        
        // Test business rule with object
        boolean result = simpleEngine.businessRule("Premium Eligibility", 
                "#data.balance > 5000 && #data.membershipLevel == 'Gold'", 
                "Customer eligible for premium services")
            .description("Checks premium service eligibility")
            .test(customer);
        
        assertTrue(result);
    }
    
    @Test
    public void testEligibilityRuleBuilder() {
        Map<String, Object> data = new HashMap<>();
        data.put("creditScore", 750);
        data.put("income", 75000);
        data.put("employmentStatus", "employed");
        
        // Test eligibility rule
        boolean result = simpleEngine.eligibilityRule("Loan Eligibility", 
                "#creditScore >= 700 && #income >= 50000 && #employmentStatus == 'employed'", 
                "Eligible for loan")
            .priority(2)
            .test(data);
        
        assertTrue(result);
        
        // Test with failing criteria
        data.put("creditScore", 650);
        boolean failResult = simpleEngine.eligibilityRule("Loan Eligibility 2", 
                "#creditScore >= 700 && #income >= 50000 && #employmentStatus == 'employed'", 
                "Eligible for loan")
            .test(data);
        
        assertFalse(failResult);
    }
    
    @Test
    public void testAccessToUnderlyingEngine() {
        // Verify we can access the underlying engine for advanced operations
        assertNotNull(simpleEngine.getEngine());
        assertNotNull(simpleEngine.getConfiguration());

        // Verify the underlying engine works by checking if we can get a rule by ID
        assertNull(simpleEngine.getConfiguration().getRuleById("non-existent"));

        // Create a rule and verify it has an ID
        Rule testRule = simpleEngine.validationRule("Test Rule", "#value > 0", "Positive value").build();
        assertNotNull(testRule.getId());
        assertNotNull(testRule.getName());
        assertEquals("Test Rule", testRule.getName());
    }
    
    /**
     * Test customer class for testing purposes.
     */
    public static class TestCustomer {
        private String name;
        private String email;
        private int age;
        private double balance;
        private String membershipLevel;
        
        public TestCustomer(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
        
        public String getMembershipLevel() { return membershipLevel; }
        public void setMembershipLevel(String membershipLevel) { this.membershipLevel = membershipLevel; }
    }
}
