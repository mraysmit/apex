package dev.mars.apex.demo.rulesets;

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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PricingServiceDemo to verify the pricing calculations work correctly.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Test class for PricingServiceDemo to verify the pricing calculations work correctly.
 */
public class PricingServiceDemoTest {

    private PricingServiceDemo pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceDemo();
    }

    @Test
    void testCalculateStandardPrice() {
        double basePrice = 100.0;
        double result = pricingService.calculateStandardPrice(basePrice);
        assertEquals(100.0, result, 0.01, "Standard price should equal base price");
    }

    @Test
    void testCalculatePremiumPrice() {
        double basePrice = 100.0;
        double result = pricingService.calculatePremiumPrice(basePrice);
        assertEquals(120.0, result, 0.01, "Premium price should be 20% higher than base price");
    }

    @Test
    void testCalculateSalePrice() {
        double basePrice = 100.0;
        double result = pricingService.calculateSalePrice(basePrice);
        assertEquals(80.0, result, 0.01, "Sale price should be 20% lower than base price");
    }

    @Test
    void testCalculateClearancePrice() {
        double basePrice = 100.0;
        double result = pricingService.calculateClearancePrice(basePrice);
        assertEquals(50.0, result, 0.01, "Clearance price should be 50% lower than base price");
    }

    @Test
    void testPricingVariationsWithDifferentBasePrices() {
        double[] basePrices = {50.0, 250.0, 1000.0, 2500.0};
        
        for (double basePrice : basePrices) {
            double standard = pricingService.calculateStandardPrice(basePrice);
            double premium = pricingService.calculatePremiumPrice(basePrice);
            double sale = pricingService.calculateSalePrice(basePrice);
            double clearance = pricingService.calculateClearancePrice(basePrice);
            
            // Verify relationships
            assertEquals(basePrice, standard, 0.01);
            assertEquals(basePrice * 1.2, premium, 0.01);
            assertEquals(basePrice * 0.8, sale, 0.01);
            assertEquals(basePrice * 0.5, clearance, 0.01);
            
            // Verify ordering: clearance < sale < standard < premium
            assertTrue(clearance < sale, "Clearance should be less than sale");
            assertTrue(sale < standard, "Sale should be less than standard");
            assertTrue(standard < premium, "Standard should be less than premium");
        }
    }

    @Test
    void testRuleBasedCalculationsWithResults() {
        double basePrice = 500.0;
        
        // Test that rule-based methods return valid RuleResult objects
        assertNotNull(pricingService.calculateStandardPriceWithResult(basePrice));
        assertNotNull(pricingService.calculatePremiumPriceWithResult(basePrice));
        assertNotNull(pricingService.calculateSalePriceWithResult(basePrice));
        assertNotNull(pricingService.calculateClearancePriceWithResult(basePrice));
    }

    @Test
    void testZeroBasePrice() {
        double basePrice = 0.0;
        
        assertEquals(0.0, pricingService.calculateStandardPrice(basePrice), 0.01);
        assertEquals(0.0, pricingService.calculatePremiumPrice(basePrice), 0.01);
        assertEquals(0.0, pricingService.calculateSalePrice(basePrice), 0.01);
        assertEquals(0.0, pricingService.calculateClearancePrice(basePrice), 0.01);
    }

    @Test
    void testNegativeBasePrice() {
        double basePrice = -100.0;
        
        // Negative prices should still follow the same percentage rules
        assertEquals(-100.0, pricingService.calculateStandardPrice(basePrice), 0.01);
        assertEquals(-120.0, pricingService.calculatePremiumPrice(basePrice), 0.01);
        assertEquals(-80.0, pricingService.calculateSalePrice(basePrice), 0.01);
        assertEquals(-50.0, pricingService.calculateClearancePrice(basePrice), 0.01);
    }
}
