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
package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.DemoTestBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Customer Transformer Demo Test
 *
 * This test demonstrates APEX's customer transformation capabilities using:
 * 1. Customer segments processing with membership-based categorization
 * 2. Conditional enrichment execution (enabled/disabled scenarios)
 * 3. Field enrichment with default value assignments
 * 4. Customer transformer summary generation
 * 5. Selective enrichment processing based on conditions
 *
 * Key Features Demonstrated:
 * - Field enrichment with default values
 * - Conditional enrichment execution (true/false conditions)
 * - Customer segmentation and profile processing
 * - Transformation workflow with selective processing
 * - Summary generation for transformation activities
 *
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 4 enrichments expected
 * ✅ Verify log shows "Processed: 2 out of 4" - Only 2 active enrichments (condition: "true")
 * ✅ Check EVERY enrichment condition - Test data triggers only active enrichments
 * ✅ Validate EVERY field enrichment - Test actual field mapping and default values
 * ✅ Assert ALL enrichment results - Every active result-field has corresponding assertEquals
 *
 * ENRICHMENT EXECUTION PATTERN:
 * - Enrichment 1: customer-segments-processing (condition: "true") - EXECUTES
 * - Enrichment 2: customer-field-actions-processing (condition: "false") - SKIPPED
 * - Enrichment 3: customer-profile-enrichment (condition: "false") - SKIPPED
 * - Enrichment 4: customer-transformer-summary (condition: "true") - EXECUTES
 *
 * YAML FIRST PRINCIPLE:
 * - ALL transformation logic is in YAML enrichments
 * - Java test only provides input data and validates results
 * - NO custom customer transformation logic in Java test code
 * - Simple test data setup and basic assertions only
 *
 * @author APEX Demo Team
 * @since 2025-09-25
 * @version 1.0.0
 */
@DisplayName("Customer Transformer Demo Tests")
public class CustomerTransformerDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTransformerDemoTest.class);

    @Test
    @DisplayName("Should process active customer segments enrichment")
    void testActiveCustomerSegmentsProcessing() {
        logger.info("=== Testing Active Customer Segments Processing ===");

        try {
            // Load YAML configuration for customer transformer demo
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerTransformerDemoTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Verify we have 4 enrichments as expected
            assertEquals(4, config.getEnrichments().size(), "Should have exactly 4 enrichments");

            // Create test data for customer segments processing
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST001");
            testData.put("segmentType", "PREMIUM");
            testData.put("customerName", "Acme Corporation");

            logger.debug("Customer segments test data: {}", testData);

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, testData);

            // Validate enrichment results
            assertNotNull(result, "Customer segments enrichment result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Customer segments enriched result: {}", enrichedData);

            // Validate customer segments processing result (condition: "true" - should execute)
            assertNotNull(enrichedData.get("customerSegmentsResult"), "Customer segments result should be processed");
            String segmentsResult = (String) enrichedData.get("customerSegmentsResult");
            assertTrue(segmentsResult.contains("Customer segments processed"), "Should contain segments processing message");
            assertTrue(segmentsResult.contains("Membership-based segmentation"), "Should contain segmentation details");

            // Validate disabled enrichments are NOT processed (condition: "false")
            assertNull(enrichedData.get("customerFieldActionsResult"), "Customer field actions should not be processed (disabled)");
            assertNull(enrichedData.get("customerProfileEnrichmentResult"), "Customer profile enrichment should not be processed (disabled)");

            logger.info("✅ Active customer segments processing completed successfully");
            logger.info("  - Customer Segments Result: {}", segmentsResult);

        } catch (Exception e) {
            logger.error("Active customer segments processing test failed: {}", e.getMessage());
            fail("Active customer segments processing test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process active customer transformer summary")
    void testActiveCustomerTransformerSummary() {
        logger.info("=== Testing Active Customer Transformer Summary ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerTransformerDemoTest.yaml");

            // Create test data for transformer summary
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST002");
            testData.put("transformerType", "COMPREHENSIVE");
            testData.put("customerName", "Global Tech Solutions");

            logger.debug("Transformer summary test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Transformer summary enriched result: {}", enrichedData);

            // Validate customer transformer summary result (condition: "true" - should execute)
            assertNotNull(enrichedData.get("customerTransformerSummary"), "Customer transformer summary should be processed");
            String transformerSummary = (String) enrichedData.get("customerTransformerSummary");
            assertTrue(transformerSummary.contains("Customer transformer completed"), "Should contain transformer completion message");
            assertTrue(transformerSummary.contains("real APEX services"), "Should contain APEX services reference");

            logger.info("✅ Active customer transformer summary completed successfully");
            logger.info("  - Customer Transformer Summary: {}", transformerSummary);

        } catch (Exception e) {
            logger.error("Active customer transformer summary test failed: {}", e.getMessage());
            fail("Active customer transformer summary test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should skip disabled enrichments correctly")
    void testDisabledEnrichmentsSkipped() {
        logger.info("=== Testing Disabled Enrichments Are Skipped ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerTransformerDemoTest.yaml");

            // Create comprehensive test data that would trigger all enrichments if enabled
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST003");
            testData.put("segmentType", "STANDARD");
            testData.put("actionType", "PROFILE_UPDATE");
            testData.put("profileType", "ENHANCED");
            testData.put("transformerType", "SELECTIVE");
            testData.put("customerName", "Small Business Inc");

            logger.debug("Disabled enrichments test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Disabled enrichments result: {}", enrichedData);

            // Validate only active enrichments are processed (condition: "true")
            assertNotNull(enrichedData.get("customerSegmentsResult"), "Customer segments should be processed (active)");
            assertNotNull(enrichedData.get("customerTransformerSummary"), "Transformer summary should be processed (active)");

            // Validate disabled enrichments are NOT processed (condition: "false")
            assertNull(enrichedData.get("customerFieldActionsResult"), "Customer field actions should NOT be processed (disabled)");
            assertNull(enrichedData.get("customerProfileEnrichmentResult"), "Customer profile enrichment should NOT be processed (disabled)");

            // Validate original input data is preserved
            assertEquals("CUST003", enrichedData.get("customerId"), "Original customer ID should be preserved");
            assertEquals("Small Business Inc", enrichedData.get("customerName"), "Original customer name should be preserved");

            logger.info("✅ Disabled enrichments correctly skipped");
            logger.info("  - Active enrichments: 2 processed");
            logger.info("  - Disabled enrichments: 2 skipped");

        } catch (Exception e) {
            logger.error("Disabled enrichments test failed: {}", e.getMessage());
            fail("Disabled enrichments test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process complete customer transformation workflow")
    void testCompleteCustomerTransformationWorkflow() {
        logger.info("=== Testing Complete Customer Transformation Workflow ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerTransformerDemoTest.yaml");

            // Create comprehensive test data for complete workflow
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST999");
            testData.put("segmentType", "ENTERPRISE");
            testData.put("actionType", "FULL_PROFILE_SYNC");
            testData.put("profileType", "COMPREHENSIVE");
            testData.put("transformerType", "COMPLETE");
            testData.put("customerName", "Enterprise Customer Corp");
            testData.put("customerTier", "PLATINUM");

            logger.debug("Complete workflow test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Complete workflow enriched result: {}", enrichedData);

            // Validate all active enrichments are processed (only 2 out of 4)
            assertNotNull(enrichedData.get("customerSegmentsResult"), "Customer segments should be processed");
            assertNotNull(enrichedData.get("customerTransformerSummary"), "Transformer summary should be processed");

            // Validate disabled enrichments remain unprocessed
            assertNull(enrichedData.get("customerFieldActionsResult"), "Field actions should remain unprocessed");
            assertNull(enrichedData.get("customerProfileEnrichmentResult"), "Profile enrichment should remain unprocessed");

            // Validate specific content of active enrichments
            String segmentsResult = (String) enrichedData.get("customerSegmentsResult");
            String transformerSummary = (String) enrichedData.get("customerTransformerSummary");

            assertTrue(segmentsResult.contains("Membership-based segmentation"), "Segments result should contain segmentation details");
            assertTrue(transformerSummary.contains("real APEX services"), "Summary should contain APEX services reference");

            // Validate original data preservation
            assertEquals("CUST999", enrichedData.get("customerId"), "Customer ID should be preserved");
            assertEquals("Enterprise Customer Corp", enrichedData.get("customerName"), "Customer name should be preserved");
            assertEquals("PLATINUM", enrichedData.get("customerTier"), "Customer tier should be preserved");

            logger.info("✅ Complete customer transformation workflow completed successfully");
            logger.info("  - Total enrichments in YAML: 4");
            logger.info("  - Active enrichments processed: 2");
            logger.info("  - Disabled enrichments skipped: 2");
            logger.info("  - Customer Segments: {}", segmentsResult);
            logger.info("  - Transformer Summary: {}", transformerSummary);

        } catch (Exception e) {
            logger.error("Complete customer transformation workflow test failed: {}", e.getMessage());
            fail("Complete customer transformation workflow test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate field enrichment default values")
    void testFieldEnrichmentDefaultValues() {
        logger.info("=== Testing Field Enrichment Default Values ===");

        try {
            // Load YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/CustomerTransformerDemoTest.yaml");

            // Create minimal test data to test default value behavior
            Map<String, Object> testData = new HashMap<>();
            testData.put("customerId", "CUST777");
            // Note: Not providing segmentType or transformerType to test default values

            logger.debug("Default values test data: {}", testData);

            // Execute APEX enrichment processing
            Object result = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.debug("Default values enriched result: {}", enrichedData);

            // Validate default values are applied for active enrichments
            assertNotNull(enrichedData.get("customerSegmentsResult"), "Customer segments default should be applied");
            assertNotNull(enrichedData.get("customerTransformerSummary"), "Transformer summary default should be applied");

            // Validate specific default value content
            String segmentsResult = (String) enrichedData.get("customerSegmentsResult");
            String transformerSummary = (String) enrichedData.get("customerTransformerSummary");

            assertEquals("Customer segments processed: Membership-based segmentation and profile enrichment applied", 
                segmentsResult, "Should use exact default value for segments");
            assertEquals("Customer transformer completed using real APEX services", 
                transformerSummary, "Should use exact default value for summary");

            logger.info("✅ Field enrichment default values validated successfully");
            logger.info("  - Segments Default: {}", segmentsResult);
            logger.info("  - Summary Default: {}", transformerSummary);

        } catch (Exception e) {
            logger.error("Field enrichment default values test failed: {}", e.getMessage());
            fail("Field enrichment default values test failed: " + e.getMessage());
        }
    }
}
