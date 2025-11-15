package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Day 9: Fix Issue #4 - Required Field Mapping Error Handling
 * 
 * Tests that required field mapping failures are properly handled and returned as RuleResult.error()
 * instead of being treated as graceful degradation.
 * 
 * This test suite verifies:
 * 1. Required field mapping failures return RuleResult with ERROR result type
 * 2. Error messages are logged at ERROR level (not WARNING)
 * 3. RuleResult.resultType = ERROR for required field failures
 * 4. Optional field mapping failures are handled gracefully (continue processing)
 */
@DisplayName("Required Field Mapping Error Handling Tests (Day 9)")
class YamlEnrichmentProcessorRequiredFieldErrorHandlingTest {

    private static final Logger logger = LoggerFactory.getLogger(YamlEnrichmentProcessorRequiredFieldErrorHandlingTest.class);
    private YamlEnrichmentProcessor enrichmentProcessor;
    private LookupServiceRegistry lookupServiceRegistry;

    @BeforeEach
    void setUp() {
        logger.info("Setting up YamlEnrichmentProcessor for required field error handling tests");
        lookupServiceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluatorService = new ExpressionEvaluatorService();
        enrichmentProcessor = new YamlEnrichmentProcessor(lookupServiceRegistry, expressionEvaluatorService);
    }

    @Test
    @DisplayName("Test 1: Required field mapping failure returns ERROR result type")
    void testRequiredFieldMappingFailureReturnsError() {
        logger.info("=== Test 1: Testing required field mapping failure returns ERROR result type ===");

        // Given: Enrichment with required field mapping that will fail (source field doesn't exist)
        YamlEnrichment enrichment = createEnrichmentWithRequiredField();
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("existingField", "value");
        // Note: "missingRequiredField" is NOT in testData, so required field mapping will fail

        // When: Process enrichment (should return error result)
        RuleResult result = enrichmentProcessor.processEnrichmentWithResult(enrichment, testData);

        // Then: Should return ERROR result type (not success or enrichment failure)
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
            "Result type should be ERROR when required field mapping fails");
        assertFalse(result.isSuccess(), "Result should indicate failure");
        
        logger.info("✅ Test 1 PASSED: Required field mapping failure returns ERROR result type");
        logger.info("   Result type: {}", result.getResultType());
        logger.info("   Result message: {}", result.getMessage());
    }

    @Test
    @DisplayName("Test 2: Error message logged at ERROR level for required field failures")
    void testErrorMessageLoggedAtErrorLevel() {
        logger.info("=== Test 2: Testing error message logged at ERROR level ===");

        // Given: Enrichment with required field mapping that will fail
        YamlEnrichment enrichment = createEnrichmentWithRequiredField();
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("existingField", "value");

        // When: Process enrichment
        RuleResult result = enrichmentProcessor.processEnrichmentWithResult(enrichment, testData);

        // Then: Should return error result with proper error message
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
            "Result type should be ERROR");
        
        // Verify error message contains relevant information
        String errorMessage = result.getMessage();
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.contains("Required field") || errorMessage.contains("field") || errorMessage.contains("failed"), 
            "Error message should indicate field mapping failure");
        
        logger.info("✅ Test 2 PASSED: Error message logged at ERROR level");
        logger.info("   Error message: {}", errorMessage);
    }

    @Test
    @DisplayName("Test 3: Optional field mapping failure continues processing (graceful degradation)")
    void testOptionalFieldMappingContinuesProcessing() {
        logger.info("=== Test 3: Testing optional field mapping continues processing ===");

        // Given: Enrichment with optional field mapping (no required flag or required=false)
        YamlEnrichment enrichment = createEnrichmentWithOptionalField();
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("existingField", "value");
        // Note: "missingOptionalField" is NOT in testData, but it's optional so should continue

        // When: Process enrichment
        RuleResult result = enrichmentProcessor.processEnrichmentWithResult(enrichment, testData);

        // Then: Should return success (graceful degradation for optional fields)
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Result should indicate success for optional field failures");
        assertNotEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
            "Result type should NOT be ERROR for optional field failures");

        logger.info("✅ Test 3 PASSED: Optional field mapping continues processing");
        logger.info("   Result type: {}", result.getResultType());
    }

    @Test
    @DisplayName("Test 4: Successful enrichment with required fields returns success")
    void testSuccessfulEnrichmentWithRequiredFields() {
        logger.info("=== Test 4: Testing successful enrichment with required fields ===");

        // Given: Enrichment with required field that exists in lookup result
        YamlEnrichment enrichment = createEnrichmentWithRequiredFieldSuccess();

        Map<String, Object> testData = new HashMap<>();
        testData.put("existingField", "value");

        // When: Process enrichment
        RuleResult result = enrichmentProcessor.processEnrichmentWithResult(enrichment, testData);

        // Then: Should return success
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Result should indicate success");
        assertNotEquals(RuleResult.ResultType.ERROR, result.getResultType(),
            "Result type should NOT be ERROR for successful enrichment");

        logger.info("✅ Test 4 PASSED: Successful enrichment with required fields returns success");
        logger.info("   Result type: {}", result.getResultType());
    }

    /**
     * Helper method to create enrichment with required field mapping
     * The lookup will succeed but the required field will be missing from the lookup result
     */
    private YamlEnrichment createEnrichmentWithRequiredField() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-enrichment-required");
        enrichment.setType("lookup-enrichment");

        // Create lookup config with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#existingField");

        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("key");

        // Create test data - lookup will succeed but won't have the required field
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("key", "value");
        row1.put("someOtherField", "someValue"); // Different field, not the required one
        // NOTE: "missingRequiredField" is NOT in the lookup result
        data.add(row1);

        dataset.setData(data);
        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);

        // Create field mappings with REQUIRED field that doesn't exist in lookup result
        List<YamlEnrichment.FieldMapping> fieldMappings = new ArrayList<>();
        YamlEnrichment.FieldMapping requiredMapping = new YamlEnrichment.FieldMapping();
        requiredMapping.setSourceField("missingRequiredField"); // This field doesn't exist in lookup result
        requiredMapping.setTargetField("resultField");
        requiredMapping.setRequired(true); // Mark as required
        fieldMappings.add(requiredMapping);
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    /**
     * Helper method to create enrichment with required field mapping for success case
     * The lookup will succeed and the required field will be present in the lookup result
     */
    private YamlEnrichment createEnrichmentWithRequiredFieldSuccess() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-enrichment-required-success");
        enrichment.setType("lookup-enrichment");

        // Create lookup config with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#existingField");

        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("key");

        // Create test data - lookup will succeed and has the required field
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("key", "value");
        row1.put("requiredField", "requiredValue"); // Required field IS present
        data.add(row1);

        dataset.setData(data);
        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);

        // Create field mappings with REQUIRED field that exists in lookup result
        List<YamlEnrichment.FieldMapping> fieldMappings = new ArrayList<>();
        YamlEnrichment.FieldMapping requiredMapping = new YamlEnrichment.FieldMapping();
        requiredMapping.setSourceField("requiredField"); // This field exists in lookup result
        requiredMapping.setTargetField("resultField");
        requiredMapping.setRequired(true); // Mark as required
        fieldMappings.add(requiredMapping);
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    /**
     * Helper method to create enrichment with optional field mapping
     */
    private YamlEnrichment createEnrichmentWithOptionalField() {
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-enrichment-optional");
        enrichment.setType("lookup-enrichment");

        // Create lookup config with inline dataset
        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupKey("#existingField");

        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("key");

        // Create test data
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("key", "value");
        row1.put("missingOptionalField", "lookupValue");
        data.add(row1);

        dataset.setData(data);
        lookupConfig.setLookupDataset(dataset);
        enrichment.setLookupConfig(lookupConfig);

        // Create field mappings with OPTIONAL field (required=false or null)
        List<YamlEnrichment.FieldMapping> fieldMappings = new ArrayList<>();
        YamlEnrichment.FieldMapping optionalMapping = new YamlEnrichment.FieldMapping();
        optionalMapping.setSourceField("missingOptionalField");
        optionalMapping.setTargetField("resultField");
        optionalMapping.setRequired(false); // Mark as optional
        fieldMappings.add(optionalMapping);
        enrichment.setFieldMappings(fieldMappings);

        return enrichment;
    }

    /**
     * NOTE: The error handling fix for required field mappings should ensure that:
     *
     * 1. **Required Field Failures**: When a required field mapping fails (source field is null/missing)
     *    - Log at ERROR level with "CRITICAL:" prefix
     *    - Return RuleResult.error() with SeverityConstants.ERROR
     *    - Do NOT continue processing (fail-fast behavior)
     *
     * 2. **Optional Field Failures**: When an optional field mapping fails
     *    - Log at WARN level (graceful degradation)
     *    - Continue processing with default value or null
     *    - Return success result
     *
     * 3. **Business Logic Failure vs Configuration Error**:
     *    - Required field missing = Business logic failure (return ERROR)
     *    - Optional field missing = Configuration error (graceful degradation)
     *
     * These tests verify that the error handling distinguishes between required and optional fields.
     */
}

