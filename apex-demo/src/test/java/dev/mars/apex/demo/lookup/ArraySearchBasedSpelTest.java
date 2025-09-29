package dev.mars.apex.demo.lookup;

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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for Search-Based SpEL functionality.
 *
 * DEMONSTRATES SPEL SEARCH PATTERNS:
 * ✅ .^[condition] - Find first matching element
 * ✅ .$[condition] - Find last matching element  
 * ✅ .?[condition] - Find all matching elements
 * ✅ Business criteria searches (legType, payReceive, currency)
 *
 * BUSINESS LOGIC VALIDATION:
 * - Find floating leg in swap trade
 * - Find pay leg by payReceive criteria
 * - Find all USD currency legs
 * - Count high-value legs by notional amount
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArraySearchBasedSpelTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ArraySearchBasedSpelTest.class);

    @Test
    @Order(1)
    @DisplayName("Should find first floating leg using search pattern")
    void shouldFindFirstFloatingLeg() {
        logger.info("=== Testing Search Pattern: Find First Floating Leg ===");
        
        try {
            // Load YAML configuration
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ArraySearchBasedSpelTest.yaml");
            assertNotNull(config, "YAML configuration should load successfully");

            // Create test data with multiple legs
            Map<String, Object> testData = createSwapTradeData();

            // Execute the evaluation
            var result = testEvaluation(config, testData);
            
            // Validate floating leg was found
            assertNotNull(result.getEnrichedData().get("floatingLegData"), 
                         "Should find floating leg using .^[legType == 'FLOATING'] pattern");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> floatingLeg = (Map<String, Object>) result.getEnrichedData().get("floatingLegData");
            assertEquals("FLOATING", floatingLeg.get("legType"), "Found leg should be FLOATING type");
            assertEquals("RECEIVE", floatingLeg.get("payReceive"), "Found leg should be RECEIVE leg");
            
            logger.info("SUCCESS: Found floating leg - legType: {}, payReceive: {}", 
                       floatingLeg.get("legType"), floatingLeg.get("payReceive"));
            
        } catch (Exception e) {
            logger.error("Failed to find floating leg: {}", e.getMessage());
            fail("Search pattern test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should find pay leg using search pattern")
    void shouldFindPayLeg() {
        logger.info("=== Testing Search Pattern: Find Pay Leg ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ArraySearchBasedSpelTest.yaml");
            Map<String, Object> testData = createSwapTradeData();
            
            var result = testEvaluation(config, testData);
            
            // Validate pay leg was found
            assertNotNull(result.getEnrichedData().get("payLegData"), 
                         "Should find pay leg using .^[payReceive == 'PAY'] pattern");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> payLeg = (Map<String, Object>) result.getEnrichedData().get("payLegData");
            assertEquals("PAY", payLeg.get("payReceive"), "Found leg should be PAY leg");
            assertEquals("FIXED", payLeg.get("legType"), "Found leg should be FIXED type");
            
            logger.info("SUCCESS: Found pay leg - legType: {}, payReceive: {}", 
                       payLeg.get("legType"), payLeg.get("payReceive"));
            
        } catch (Exception e) {
            logger.error("Failed to find pay leg: {}", e.getMessage());
            fail("Search pattern test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should find all USD legs using search pattern")
    void shouldFindAllUsdLegs() {
        logger.info("=== Testing Search Pattern: Find All USD Legs ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ArraySearchBasedSpelTest.yaml");
            Map<String, Object> testData = createSwapTradeData();
            
            var result = testEvaluation(config, testData);
            
            // Validate USD legs were found
            assertNotNull(result.getEnrichedData().get("usdLegsData"), 
                         "Should find USD legs using .?[currency == 'USD'] pattern");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> usdLegs = (List<Map<String, Object>>) result.getEnrichedData().get("usdLegsData");
            assertTrue(usdLegs.size() >= 2, "Should find at least 2 USD legs");
            
            // Verify all found legs are USD
            for (Map<String, Object> leg : usdLegs) {
                assertEquals("USD", leg.get("currency"), "All found legs should be USD currency");
            }
            
            logger.info("SUCCESS: Found {} USD legs", usdLegs.size());
            
        } catch (Exception e) {
            logger.error("Failed to find USD legs: {}", e.getMessage());
            fail("Search pattern test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should count high value legs using search pattern")
    void shouldCountHighValueLegs() {
        logger.info("=== Testing Search Pattern: Count High Value Legs ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ArraySearchBasedSpelTest.yaml");
            Map<String, Object> testData = createSwapTradeData();
            
            var result = testEvaluation(config, testData);
            
            // Validate high value count
            assertNotNull(result.getEnrichedData().get("highValueCount"), 
                         "Should count high value legs using .?[notionalAmount > 1000000].size() pattern");
            
            Integer highValueCount = (Integer) result.getEnrichedData().get("highValueCount");
            assertTrue(highValueCount >= 1, "Should find at least 1 high value leg");
            
            logger.info("SUCCESS: Found {} high value legs (> 1,000,000)", highValueCount);
            
        } catch (Exception e) {
            logger.error("Failed to count high value legs: {}", e.getMessage());
            fail("Search pattern test failed: " + e.getMessage());
        }
    }

    @Test
    public void shouldGetFloatingLegNotionalAmount() {
        logger.info("=== Testing Search Pattern: Get Floating Leg Notional Amount ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ArraySearchBasedSpelTest.yaml");
            Map<String, Object> testData = createSwapTradeData();

            var result = testEvaluation(config, testData);

            // Verify the floating leg notional amount was extracted
            Object floatingNotional = result.getEnrichedData().get("floatingNotionalAmount");
            assertNotNull(floatingNotional, "Should extract floating leg notional amount using .^[legType == 'FLOATING']?.notionalAmount pattern");
            assertEquals(5000000, floatingNotional, "Should extract correct notional amount from floating leg");

            logger.info("SUCCESS: Extracted floating leg notional amount: {}", floatingNotional);

        } catch (Exception e) {
            logger.error("Failed to extract floating leg notional: {}", e.getMessage());
            fail("Search pattern test failed: " + e.getMessage());
        }
    }

    @Test
    public void shouldGetPayLegCurrency() {
        logger.info("=== Testing Search Pattern: Get Pay Leg Currency ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/ArraySearchBasedSpelTest.yaml");
            Map<String, Object> testData = createSwapTradeData();

            var result = testEvaluation(config, testData);

            // Verify the pay leg currency was extracted
            Object payLegCurrency = result.getEnrichedData().get("payLegCurrencyCode");
            assertNotNull(payLegCurrency, "Should extract pay leg currency using .^[payReceive == 'PAY']?.currency pattern");
            assertEquals("USD", payLegCurrency, "Should extract correct currency from pay leg");

            logger.info("SUCCESS: Extracted pay leg currency: {}", payLegCurrency);

        } catch (Exception e) {
            logger.error("Failed to extract pay leg currency: {}", e.getMessage());
            fail("Search pattern test failed: " + e.getMessage());
        }
    }

    /**
     * Create test data representing a swap trade with multiple legs
     * demonstrating different search criteria
     */
    private Map<String, Object> createSwapTradeData() {
        Map<String, Object> testData = new HashMap<>();
        
        // Create trade with multiple legs for search testing
        Map<String, Object> trade = new HashMap<>();
        List<Map<String, Object>> legs = new ArrayList<>();
        
        // Leg 1: Fixed, Pay, USD, High Value
        Map<String, Object> leg1 = new HashMap<>();
        leg1.put("legType", "FIXED");
        leg1.put("payReceive", "PAY");
        leg1.put("currency", "USD");
        leg1.put("notionalAmount", 5000000);
        leg1.put("rate", 0.025);
        legs.add(leg1);
        
        // Leg 2: Floating, Receive, USD, High Value  
        Map<String, Object> leg2 = new HashMap<>();
        leg2.put("legType", "FLOATING");
        leg2.put("payReceive", "RECEIVE");
        leg2.put("currency", "USD");
        leg2.put("notionalAmount", 5000000);
        leg2.put("rate", 0.0);
        legs.add(leg2);
        
        // Leg 3: Fixed, Pay, EUR, Low Value
        Map<String, Object> leg3 = new HashMap<>();
        leg3.put("legType", "FIXED");
        leg3.put("payReceive", "PAY");
        leg3.put("currency", "EUR");
        leg3.put("notionalAmount", 500000);
        leg3.put("rate", 0.02);
        legs.add(leg3);
        
        trade.put("legs", legs);
        trade.put("tradeType", "INTEREST_RATE_SWAP");
        testData.put("trade", trade);
        
        logger.info("Created test data with {} legs for search pattern testing", legs.size());
        return testData;
    }
}
