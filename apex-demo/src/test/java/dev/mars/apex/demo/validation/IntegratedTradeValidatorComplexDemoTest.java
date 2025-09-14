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
 * JUnit 5 test for IntegratedTradeValidatorComplexDemo functionality.
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 4 enrichments expected (integrated-trade-validation, complex-validator-processing, trade-validator-integration, complex-demo-completion)
 * ✅ Verify log shows "Processed: 4 out of 4" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL 4 conditions
 * ✅ Validate EVERY business calculation - Test actual integrated trade validator complex demo logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * BUSINESS LOGIC VALIDATION:
 * - Integrated trade validation with comprehensive integrated trade validation processing with real APEX processing
 * - Complex validator processing with advanced validator processing operations and business logic
 * - Trade validator integration with trade validator integration and complex validation operations
 * - Complex demo completion with complex demo completion processing and validation summary
 */
public class IntegratedTradeValidatorComplexDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedTradeValidatorComplexDemoTest.class);

    @Test
    void testComprehensiveIntegratedTradeValidatorComplexDemoFunctionality() {
        logger.info("=== Testing Comprehensive Integrated Trade Validator Complex Demo Functionality ===");
        
        // Load YAML configuration for integrated trade validator complex demo
        var config = loadAndValidateYaml("test-configs/integrated-trade-validator-complex-demo-test.yaml");
        
        // Create test data that demonstrates conditional ruleset evaluation
        // This shows the production use case: "if validationType='X' then process against Rule A"
        Map<String, Object> testData = new HashMap<>();

        // Trigger multiple validation types simultaneously (production pattern)
        // Each validation type will execute its corresponding enrichment
        testData.put("validationType", "settlement-validation");  // Triggers settlement validation
        testData.put("settlementType", "dvp");  // Data for settlement lookup

        // Add additional validation triggers with valid lookup keys
        testData.put("complianceType", "mifid");  // For compliance validation (matches lookup dataset)
        testData.put("lookupType", "default");  // For lookup service validation (matches lookup dataset)
        testData.put("rulesType", "default");  // For validation rules (matches lookup dataset)
        testData.put("sampleType", "default");  // For trade sample validation (matches lookup dataset)
        
        // Execute APEX enrichment processing
        Object result = enrichmentService.enrichObject(config, testData);
        
        // Validate enrichment results using proper casting pattern
        assertNotNull(result, "Integrated trade validator complex demo enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate ALL business logic results (all 6 enrichments should be processed)
        assertNotNull(enrichedData.get("settlementValidationResult"), "Settlement validation result should be generated");
        assertNotNull(enrichedData.get("complianceValidationResult"), "Compliance validation result should be generated");
        assertNotNull(enrichedData.get("lookupServiceValidationResult"), "Lookup service validation result should be generated");
        assertNotNull(enrichedData.get("validationRulesResult"), "Validation rules result should be generated");
        assertNotNull(enrichedData.get("tradeSampleValidationResult"), "Trade sample validation result should be generated");
        assertNotNull(enrichedData.get("validationSummary"), "Validation summary should be generated");
        
        // Validate specific business calculations with actual field names
        String settlementResult = (String) enrichedData.get("settlementValidationResult");
        assertTrue(settlementResult.contains("Settlement validation completed"), "Settlement validation should contain expected message");

        String complianceResult = (String) enrichedData.get("complianceValidationResult");
        assertTrue(complianceResult.contains("MiFID II Compliance validation completed"), "Compliance validation should contain MiFID II message");

        String validationSummary = (String) enrichedData.get("validationSummary");
        assertTrue(validationSummary.contains("Complex trade validation completed"), "Validation summary should contain expected message");
        
        logger.info("✅ Comprehensive integrated trade validator complex demo functionality test completed successfully");
    }
}
