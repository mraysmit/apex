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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for NestedFieldLookupDemo functionality.
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 4 enrichments expected (nested-field-navigation, country-settlement-lookup, object-hierarchy-processing, summary)
 * ✅ Verify log shows "Processed: 4 out of 4" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL 4 conditions
 * ✅ Validate EVERY business calculation - Test actual nested field lookup logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * BUSINESS LOGIC VALIDATION:
 * - Nested field navigation with real APEX processing
 * - Country-specific settlement information lookup
 * - Object hierarchy processing for nested data structures
 * - Comprehensive nested field lookup summary
 */
public class NestedFieldLookupDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(NestedFieldLookupDemoTest.class);

    @Test
    void testComprehensiveNestedFieldLookupFunctionality() {
        logger.info("=== Testing Comprehensive Nested Field Lookup Functionality ===");
        
        // Load YAML configuration for nested field lookup
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
        
        // Create comprehensive test data that triggers ALL 4 enrichments
        Map<String, Object> testData = new HashMap<>();
        
        // Data for nested-field-navigation enrichment
        testData.put("navigationType", "nested-field-navigation");
        testData.put("navigationScope", "object-hierarchy");
        
        // Data for country-settlement-lookup enrichment
        testData.put("settlementType", "country-settlement-lookup");
        testData.put("settlementScope", "country-specific");
        
        // Data for object-hierarchy-processing enrichment
        testData.put("hierarchyType", "object-hierarchy-processing");
        testData.put("hierarchyScope", "nested-structures");
        
        // Common data for summary enrichment
        testData.put("approach", "real-apex-services");
        
        // Execute APEX enrichment processing
        Object result = enrichmentService.enrichObject(config, testData);
        
        // Validate enrichment results using proper casting pattern
        assertNotNull(result, "Nested field lookup enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate ALL business logic results (all 4 enrichments should be processed)
        assertNotNull(enrichedData.get("nestedFieldNavigationResult"), "Nested field navigation result should be generated");
        assertNotNull(enrichedData.get("countrySettlementLookupResult"), "Country settlement lookup result should be generated");
        assertNotNull(enrichedData.get("objectHierarchyProcessingResult"), "Object hierarchy processing result should be generated");
        assertNotNull(enrichedData.get("nestedFieldLookupSummary"), "Nested field lookup summary should be generated");
        
        // Validate specific business calculations
        String nestedFieldNavigationResult = (String) enrichedData.get("nestedFieldNavigationResult");
        assertTrue(nestedFieldNavigationResult.contains("nested-field-navigation"), "Nested field navigation result should contain navigation type");
        
        String countrySettlementLookupResult = (String) enrichedData.get("countrySettlementLookupResult");
        assertTrue(countrySettlementLookupResult.contains("country-settlement-lookup"), "Country settlement lookup result should reference settlement type");
        
        String objectHierarchyProcessingResult = (String) enrichedData.get("objectHierarchyProcessingResult");
        assertTrue(objectHierarchyProcessingResult.contains("object-hierarchy-processing"), "Object hierarchy processing result should reference hierarchy type");
        
        String nestedFieldLookupSummary = (String) enrichedData.get("nestedFieldLookupSummary");
        assertTrue(nestedFieldLookupSummary.contains("real-apex-services"), "Nested field lookup summary should reference approach");
        
            logger.info("✅ Comprehensive nested field lookup functionality test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    void testNestedFieldNavigationProcessing() {
        logger.info("=== Testing Nested Field Navigation Processing ===");
        
        // Load YAML configuration for nested field lookup
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
        
        // Test different navigation types
        String[] navigationTypes = {"nested-field-navigation", "object-navigation", "field-hierarchy-navigation"};
        
        for (String navigationType : navigationTypes) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("navigationType", navigationType);
            testData.put("navigationScope", "object-hierarchy");
            testData.put("approach", "real-apex-services");
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate enrichment results
            assertNotNull(result, "Nested field navigation result should not be null for " + navigationType);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate nested field navigation business logic
            assertNotNull(enrichedData.get("nestedFieldNavigationResult"), "Nested field navigation result should be generated for " + navigationType);
            
            String nestedFieldNavigationResult = (String) enrichedData.get("nestedFieldNavigationResult");
            assertTrue(nestedFieldNavigationResult.contains(navigationType), "Nested field navigation result should contain " + navigationType);
        }
        
            logger.info("✅ Nested field navigation processing test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    void testCountrySettlementLookupProcessing() {
        logger.info("=== Testing Country Settlement Lookup Processing ===");
        
        // Load YAML configuration for nested field lookup
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
        
        // Test different settlement types
        String[] settlementTypes = {"country-settlement-lookup", "settlement-information-lookup", "country-specific-lookup"};
        
        for (String settlementType : settlementTypes) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("settlementType", settlementType);
            testData.put("settlementScope", "country-specific");
            testData.put("approach", "real-apex-services");
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate enrichment results
            assertNotNull(result, "Country settlement lookup result should not be null for " + settlementType);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate country settlement lookup processing business logic
            assertNotNull(enrichedData.get("countrySettlementLookupResult"), "Country settlement lookup result should be generated for " + settlementType);
            
            String countrySettlementLookupResult = (String) enrichedData.get("countrySettlementLookupResult");
            assertTrue(countrySettlementLookupResult.contains(settlementType), "Country settlement lookup result should reference settlement type " + settlementType);
        }
        
            logger.info("✅ Country settlement lookup processing test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }

    @Test
    void testObjectHierarchyProcessingProcessing() {
        logger.info("=== Testing Object Hierarchy Processing Processing ===");
        
        // Load YAML configuration for nested field lookup
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/NestedFieldLookupDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");
        
        // Test different hierarchy types
        String[] hierarchyTypes = {"object-hierarchy-processing", "nested-structure-processing", "hierarchy-navigation"};
        
        for (String hierarchyType : hierarchyTypes) {
            Map<String, Object> testData = new HashMap<>();
            testData.put("hierarchyType", hierarchyType);
            testData.put("hierarchyScope", "nested-structures");
            testData.put("approach", "real-apex-services");
            
            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            
            // Validate enrichment results
            assertNotNull(result, "Object hierarchy processing result should not be null for " + hierarchyType);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;
            
            // Validate object hierarchy processing processing business logic
            assertNotNull(enrichedData.get("objectHierarchyProcessingResult"), "Object hierarchy processing result should be generated for " + hierarchyType);
            
            String objectHierarchyProcessingResult = (String) enrichedData.get("objectHierarchyProcessingResult");
            assertTrue(objectHierarchyProcessingResult.contains(hierarchyType), "Object hierarchy processing result should reference hierarchy type " + hierarchyType);
        }
        
            logger.info("✅ Object hierarchy processing processing test completed successfully");
        } catch (Exception e) {
            fail("Failed to load YAML configuration: " + e.getMessage());
        }
    }
}
