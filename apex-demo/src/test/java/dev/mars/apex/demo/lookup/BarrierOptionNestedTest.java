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

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BarrierOptionNestedTest - Validates APEX Engine Nested Data Processing Capabilities
 * 
 * PURPOSE: Prove that APEX can handle complex nested XML structures through:
 * - APEX enrichment engine processing 2+ level nested fields
 * - APEX mathematical calculations on nested data values
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 2 enrichments expected (apex-level2-navigation, apex-cross-nested-calculation)
 * ✅ Verify log shows "Processed: 2 out of 2" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL 2 conditions
 * ✅ Validate EVERY business calculation - Test mathematical formulas on nested data
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * ALL PROCESSING LOGIC IS IN APEX YAML FILES - NO CUSTOM JAVA LOGIC
 * Tests validate APEX functionality, not custom implementations
 */
public class BarrierOptionNestedTest extends DemoTestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(BarrierOptionNestedTest.class);

    @Test
    @DisplayName("Validate APEX Nested Enrichment Processing Capabilities - Initial 2 Enrichments")
    void testApexNestedEnrichmentCapabilities() {
        logger.info("=== Testing APEX Nested Enrichment Processing Capabilities ===");
        
        // Load APEX enrichment configuration
        var config = loadAndValidateYaml("lookup/barrier-option-nested-enrichment.yaml");
        
        // Create barrier option test data that triggers ALL 2 enrichments
        Map<String, Object> barrierOptionData = createBarrierOptionTestData();

        // Execute APEX enrichment processing - ALL logic in YAML
        Object result = enrichmentService.enrichObject(config, barrierOptionData);
        
        // Validate APEX successfully processed nested structures
        assertNotNull(result, "APEX nested enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // VALIDATE: APEX processed ALL 2 enrichments (must be 100% execution rate)
        validateApexNestedEnrichmentResults(enrichedData);
        
        logger.info("✓ APEX successfully processed all nested data structures");
    }
    
    /**
     * Creates barrier option test data that triggers ALL nested processing conditions
     */
    private Map<String, Object> createBarrierOptionTestData() {
        Map<String, Object> testData = new HashMap<>();
        
        // Root level data
        testData.put("tradeDate", "2025-09-19");
        testData.put("buyerParty", "GOLDMAN_SACHS");
        testData.put("sellerParty", "JP_MORGAN");
        testData.put("optionType", "Call");
        testData.put("expiryDate", "2025-12-19");
        testData.put("settlementType", "Cash");
        
        // Level 1: Underlying asset with Level 2 market data
        Map<String, Object> underlyingAsset = new HashMap<>();
        underlyingAsset.put("commodity", "Gold");
        underlyingAsset.put("unit", "Troy Ounce");
        underlyingAsset.put("exchange", "COMEX");
        
        Map<String, Object> marketData = new HashMap<>();
        marketData.put("currentPrice", "2100.00");
        marketData.put("volatility", "0.18");
        underlyingAsset.put("marketData", marketData);
        testData.put("underlyingAsset", underlyingAsset);
        
        // Level 1: Pricing terms
        Map<String, Object> pricingTerms = new HashMap<>();
        pricingTerms.put("strikePrice", "2150.00");
        pricingTerms.put("notionalQuantity", "100");
        pricingTerms.put("premium", "15000.00");
        testData.put("pricingTerms", pricingTerms);
        
        // Level 1: Barrier terms
        Map<String, Object> barrierTerms = new HashMap<>();
        barrierTerms.put("barrierType", "Knock-Out");
        barrierTerms.put("barrierLevel", "2300.00");
        barrierTerms.put("barrierDirection", "Up-and-Out");
        barrierTerms.put("monitoringFrequency", "Continuous");
        testData.put("barrierTerms", barrierTerms);
        
        return testData;
    }
    
    /**
     * Validates APEX nested enrichment processing results
     * CRITICAL: Must validate ALL 2 enrichments were processed successfully
     */
    private void validateApexNestedEnrichmentResults(Map<String, Object> enrichedData) {
        // VALIDATE: APEX Level 2 nested field navigation
        assertNotNull(enrichedData.get("apexExtractedMarketPrice"), "APEX should extract nested price: underlyingAsset.marketData.currentPrice");
        assertEquals("2100.00", enrichedData.get("apexExtractedMarketPrice"),
                    "APEX should extract nested price: underlyingAsset.marketData.currentPrice");
        
        // VALIDATE: APEX cross-nested-field business calculation
        assertNotNull(enrichedData.get("apexBarrierSpread"), "APEX should calculate barrier spread");
        assertEquals(150.0, Double.parseDouble(enrichedData.get("apexBarrierSpread").toString()),
                    "APEX should calculate: barrierLevel (2300) - strikePrice (2150) = 150");
        
        logger.info("✓ All 2 APEX nested enrichments validated successfully:");
        logger.info("  - Level 2 Navigation: {}", enrichedData.get("apexExtractedMarketPrice"));
        logger.info("  - Cross-Nested Calculation: {}", enrichedData.get("apexBarrierSpread"));
    }
}
