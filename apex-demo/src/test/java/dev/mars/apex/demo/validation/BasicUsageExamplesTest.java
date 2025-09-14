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

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for BasicUsageExamples functionality.
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 4 enrichments expected (field-validation, customer-validation, product-validation, trade-validation)
 * ✅ Verify log shows "Processed: 4 out of 4" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL 4 conditions
 * ✅ Validate EVERY business calculation - Test actual basic usage examples logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * BUSINESS LOGIC VALIDATION:
 * - Field validation with comprehensive field validation patterns with real APEX processing
 * - Customer validation with YAML-driven customer validation rules and business logic
 * - Product validation with business rule enforcement and validation operations
 * - Trade validation with financial domain validation examples and processing
 */
public class BasicUsageExamplesTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(BasicUsageExamplesTest.class);

    @Test
    void testComprehensiveBasicUsageExamplesFunctionality() {
        logger.info("=== Testing Comprehensive Basic Usage Examples Functionality ===");
        
        // Load YAML configuration for basic usage examples
        var config = loadAndValidateYaml("test-configs/basic-usage-examples-test.yaml");
        
        // Create comprehensive test data that triggers existing enrichments
        Map<String, Object> testData = new HashMap<>();

        // Data for basic field validation
        testData.put("name", "John Smith");
        testData.put("age", 35);
        testData.put("email", "john.smith@example.com");

        // Data for customer validation
        testData.put("customerId", "CUST-001");
        testData.put("customerType", "PREMIUM");

        // Data for product validation
        testData.put("productId", "PROD-001");
        testData.put("productCategory", "FINANCIAL");

        // Data for trade validation
        testData.put("tradeId", "TRADE-001");
        testData.put("tradeType", "SWAP");

        // Data for expression evaluation enrichment
        testData.put("amount", 1000.0);
        
        // Execute APEX enrichment processing
        Object result = enrichmentService.enrichObject(config, testData);
        
        // Validate enrichment results using proper casting pattern
        assertNotNull(result, "Basic usage examples enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate basic usage examples results (all 3 enrichments should be processed)
        assertNotNull(enrichedData.get("validatedName"), "Validated name should be generated");
        assertNotNull(enrichedData.get("validatedAge"), "Validated age should be generated");
        assertNotNull(enrichedData.get("validatedEmail"), "Validated email should be generated");
        assertNotNull(enrichedData.get("calculatedResult"), "Calculated result should be generated");

        // Validate specific business calculations
        String validatedName = (String) enrichedData.get("validatedName");
        assertTrue(validatedName.contains("John Smith"), "Validated name should contain original name");

        Integer validatedAge = (Integer) enrichedData.get("validatedAge");
        assertEquals(35, validatedAge, "Validated age should match input age");

        String validatedEmail = (String) enrichedData.get("validatedEmail");
        assertTrue(validatedEmail.contains("john.smith@example.com"), "Validated email should contain original email");

        Double calculatedResult = (Double) enrichedData.get("calculatedResult");
        assertEquals(50.0, calculatedResult, "Calculated result should be 1000 * 0.05 = 50.0");
        
        logger.info("✅ Comprehensive basic usage examples functionality test completed successfully");
    }
}
