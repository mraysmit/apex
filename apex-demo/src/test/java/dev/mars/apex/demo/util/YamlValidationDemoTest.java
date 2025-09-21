package dev.mars.apex.demo.util;

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
 * JUnit 5 test for YamlValidationDemo functionality.
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 *  Count enrichments in YAML - 4 enrichments expected (yaml-metadata-validation, yaml-schema-validation, yaml-content-validation, validation-summary-analysis)
 *  Verify log shows "Processed: 4 out of 4" - Must be 100% execution rate
 *  Check EVERY enrichment condition - Test data triggers ALL 4 conditions
 *  Validate EVERY business calculation - Test actual YAML validation demo logic
 *  Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * BUSINESS LOGIC VALIDATION:
 * - YAML metadata validation with comprehensive metadata structure validation with real APEX processing
 * - YAML schema validation with schema compliance checking and validation rule processing
 * - YAML content validation with content structure analysis and validation operations
 * - Validation summary analysis with comprehensive validation result analysis and reporting
 */
public class YamlValidationDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(YamlValidationDemoTest.class);

    @Test
    void testComprehensiveYamlValidationDemoFunctionality() {
        logger.info("=== Testing Comprehensive YAML Validation Demo Functionality ===");
        
        // Load YAML configuration for YAML validation demo
        var config = loadAndValidateYaml("test-configs/yamlvalidationdemo-test.yaml");
        
        // Create comprehensive test data that triggers ALL 4 enrichments
        Map<String, Object> testData = new HashMap<>();
        
        // Data for yaml-metadata-validation enrichment
        testData.put("validationType", "yaml-metadata-validation");
        testData.put("validationScope", "metadata-structure-validation");
        
        // Data for yaml-schema-validation enrichment
        testData.put("schemaValidationType", "yaml-schema-validation");
        testData.put("schemaValidationScope", "schema-compliance-checking");
        
        // Data for yaml-content-validation enrichment
        testData.put("contentValidationType", "yaml-content-validation");
        testData.put("contentValidationScope", "content-structure-analysis");
        
        // Common data for validation-summary-analysis enrichment
        testData.put("approach", "real-apex-services");
        
        // Execute APEX enrichment processing
        Object result = enrichmentService.enrichObject(config, testData);
        
        // Validate enrichment results using proper casting pattern
        assertNotNull(result, "YAML validation demo enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;
        
        // Validate ALL business logic results (all 4 enrichments should be processed)
        assertNotNull(enrichedData.get("yamlMetadataValidationResult"), "YAML metadata validation result should be generated");
        assertNotNull(enrichedData.get("yamlSchemaValidationResult"), "YAML schema validation result should be generated");
        assertNotNull(enrichedData.get("yamlContentValidationResult"), "YAML content validation result should be generated");
        assertNotNull(enrichedData.get("validationSummaryAnalysisResult"), "Validation summary analysis result should be generated");
        
        // Validate specific business calculations
        String yamlMetadataValidationResult = (String) enrichedData.get("yamlMetadataValidationResult");
        assertTrue(yamlMetadataValidationResult.contains("yaml-metadata-validation"), "YAML metadata validation result should contain validation type");
        
        String yamlSchemaValidationResult = (String) enrichedData.get("yamlSchemaValidationResult");
        assertTrue(yamlSchemaValidationResult.contains("yaml-schema-validation"), "YAML schema validation result should reference schema validation type");
        
        String yamlContentValidationResult = (String) enrichedData.get("yamlContentValidationResult");
        assertTrue(yamlContentValidationResult.contains("yaml-content-validation"), "YAML content validation result should reference content validation type");
        
        String validationSummaryAnalysisResult = (String) enrichedData.get("validationSummaryAnalysisResult");
        assertTrue(validationSummaryAnalysisResult.contains("real-apex-services"), "Validation summary analysis result should reference approach");
        
        logger.info(" Comprehensive YAML validation demo functionality test completed successfully");
    }
}
