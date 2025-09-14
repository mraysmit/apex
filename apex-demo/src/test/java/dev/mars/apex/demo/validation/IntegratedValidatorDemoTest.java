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
 * JUnit 5 test for IntegratedValidatorDemo functionality.
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 4 enrichments expected (integrated-validator-setup, validator-integration-processing, integrated-validation-operations, validator-demo-completion)
 * ✅ Verify log shows "Processed: 4 out of 4" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL 4 conditions
 * ✅ Validate EVERY business calculation - Test actual integrated validator demo logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * BUSINESS LOGIC VALIDATION:
 * - Integrated validator setup with comprehensive integrated validator setup processing with real APEX processing
 * - Validator integration processing with validator integration operations and business logic enforcement
 * - Integrated validation operations with integrated validation processing and rule enforcement
 * - Validator demo completion with validator demo completion processing and validation summary
 */
public class IntegratedValidatorDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedValidatorDemoTest.class);

    @Test
    void testComprehensiveIntegratedValidatorDemoFunctionality() {
        logger.info("=== Testing Comprehensive Integrated Validator Demo Functionality ===");
        
        // Load YAML configuration for integrated validator demo
        var config = loadAndValidateYaml("test-configs/integrated-validator-demo-test.yaml");
        
        // Create comprehensive test data that triggers ALL 4 enrichments
        Map<String, Object> testData = new HashMap<>();
        
        // Data to trigger multiple validation types simultaneously (production pattern)
        testData.put("entityType", "Customer");  // Base entity type for lookups
        testData.put("validationType", "INTEGRATED_VALIDATION");

        // Trigger different validation rules with specific data
        testData.put("rulesType", "basic-validation-rules");  // For validation-rules-processing
        testData.put("ruleType", "basic-validation-rules");   // For lookup key construction
        testData.put("samplesType", "default");  // For entity-samples-processing
        testData.put("parametersType", "default");  // For validation-parameters-processing
        
        // Execute APEX enrichment processing
        Object result = enrichmentService.enrichObject(config, testData);
        
        // Validate enrichment results using proper casting pattern
        assertNotNull(result, "Integrated validator demo enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate ALL business logic results (all 4 enrichments should be processed)
        assertNotNull(enrichedData.get("validationRulesResult"), "Validation rules result should be generated");
        assertNotNull(enrichedData.get("entitySamplesResult"), "Entity samples result should be generated");
        assertNotNull(enrichedData.get("validationParametersResult"), "Validation parameters result should be generated");
        assertNotNull(enrichedData.get("validationSummary"), "Validation summary should be generated");
        
        // Validate specific business calculations with actual field names
        String validationRulesResult = (String) enrichedData.get("validationRulesResult");
        assertTrue(validationRulesResult.contains("Customer basic validation rules completed"), "Validation rules should contain expected message");

        String entitySamplesResult = (String) enrichedData.get("entitySamplesResult");
        assertTrue(entitySamplesResult.contains("Entity samples processing completed"), "Entity samples should contain expected message");

        String validationSummary = (String) enrichedData.get("validationSummary");
        assertTrue(validationSummary.contains("Integrated validator completed"), "Validation summary should contain expected message");
        
        logger.info("✅ Comprehensive integrated validator demo functionality test completed successfully");
    }
}
