package dev.mars.apex.demo.validation;

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

import dev.mars.apex.core.api.ValidationBuilder;
import dev.mars.apex.core.api.ValidationResult;
import dev.mars.apex.core.api.RulesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify comprehensive debug logging in validation processing.
 */
public class ValidationDebugLoggingTest {

    private static final Logger logger = LoggerFactory.getLogger(ValidationDebugLoggingTest.class);

    @Test
    @DisplayName("Should demonstrate comprehensive debug logging for validation operations")
    void testComprehensiveValidationDebugLogging() {
        logger.info("=".repeat(80));
        logger.info("COMPREHENSIVE VALIDATION DEBUG LOGGING TEST");
        logger.info("=".repeat(80));
        logger.info("Testing all validation operations with debug logging enabled");
        logger.info("");

        // Test ValidationBuilder with multiple rules
        testValidationBuilder();
        
        // Test RulesService check operations
        testRulesServiceCheck();
        
        logger.info("\n" + "=".repeat(80));
        logger.info("COMPREHENSIVE VALIDATION DEBUG LOGGING TEST COMPLETED");
        logger.info("=".repeat(80));
    }

    private void testValidationBuilder() {
        logger.info("Testing ValidationBuilder with debug logging...");
        
        try {
            // Create test data
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("age", 25);
            customerData.put("email", "test@example.com");
            customerData.put("name", "John Doe");
            customerData.put("balance", 1000.0);
            
            logger.info("Test data: {}", customerData);
            
            // Create ValidationBuilder and add multiple rules
            ValidationBuilder builder = new ValidationBuilder(customerData);
            
            // Add validation rules
            builder.that("#age >= 18", "Age must be at least 18")
                   .that("#email != null && #email.length() > 0", "Email is required")
                   .that("#name != null && #name.length() >= 2", "Name must be at least 2 characters")
                   .that("#balance >= 0", "Balance must be non-negative");
            
            logger.info("Added 4 validation rules to ValidationBuilder");
            
            // Test passes() method
            logger.info("Testing ValidationBuilder.passes()...");
            boolean passes = builder.passes();
            logger.info("✓ ValidationBuilder.passes() result: {}", passes);
            
            // Test validate() method for detailed results
            logger.info("Testing ValidationBuilder.validate()...");
            ValidationResult result = builder.validate();
            logger.info("✓ ValidationBuilder.validate() completed: valid={}, errors={}", 
                result.isValid(), result.getErrors().size());
            
            if (!result.getErrors().isEmpty()) {
                logger.info("Validation errors:");
                result.getErrors().forEach(error -> logger.info("  - {}", error));
            }
            
            // Test with invalid data
            logger.info("Testing ValidationBuilder with invalid data...");
            Map<String, Object> invalidData = new HashMap<>();
            invalidData.put("age", 16);  // Too young
            invalidData.put("email", ""); // Empty email
            invalidData.put("name", "X"); // Too short
            invalidData.put("balance", -100.0); // Negative balance
            
            ValidationBuilder invalidBuilder = new ValidationBuilder(invalidData);
            invalidBuilder.that("#age >= 18", "Age must be at least 18")
                         .that("#email != null && #email.length() > 0", "Email is required")
                         .that("#name != null && #name.length() >= 2", "Name must be at least 2 characters")
                         .that("#balance >= 0", "Balance must be non-negative");
            
            ValidationResult invalidResult = invalidBuilder.validate();
            logger.info("✓ Invalid data validation completed: valid={}, errors={}", 
                invalidResult.isValid(), invalidResult.getErrors().size());
            
        } catch (Exception e) {
            logger.error("ValidationBuilder test failed: {}", e.getMessage(), e);
            fail("ValidationBuilder test failed: " + e.getMessage());
        }
    }

    private void testRulesServiceCheck() {
        logger.info("Testing RulesService.check() with debug logging...");
        
        try {
            RulesService rulesService = new RulesService();
            
            // Test with Map facts
            Map<String, Object> facts = new HashMap<>();
            facts.put("temperature", 25);
            facts.put("humidity", 60);
            facts.put("pressure", 1013);
            
            logger.info("Testing RulesService.check() with facts: {}", facts);
            
            // Test multiple conditions
            String[] conditions = {
                "#temperature > 20",
                "#humidity < 80",
                "#pressure > 1000",
                "#temperature > 30", // This should fail
                "#humidity > 90"     // This should fail
            };
            
            for (String condition : conditions) {
                logger.info("Testing condition: '{}'", condition);
                boolean result = rulesService.check(condition, facts);
                logger.info("✓ Condition '{}' result: {}", condition, result);
            }
            
            // Test with object data
            logger.info("Testing RulesService.check() with object data...");
            TestObject testObj = new TestObject("Alice", 30, true);
            
            boolean objectResult = rulesService.check("#name.length() > 3", testObj);
            logger.info("✓ Object condition result: {}", objectResult);
            
        } catch (Exception e) {
            logger.error("RulesService test failed: {}", e.getMessage(), e);
            fail("RulesService test failed: " + e.getMessage());
        }
    }

    // Simple test object for validation
    public static class TestObject {
        private String name;
        private int age;
        private boolean active;
        
        public TestObject(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        
        @Override
        public String toString() {
            return String.format("TestObject{name='%s', age=%d, active=%s}", name, age, active);
        }
    }
}
